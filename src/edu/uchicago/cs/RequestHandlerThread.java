package edu.uchicago.cs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.concurrent.ConcurrentHashMap;

public class RequestHandlerThread implements Runnable {
	private Socket socket = null;
	private ExchangeServer exchangeServer = null;
	private ConcurrentHashMap<String, UserInfo> userInfoMap = null;
	private ExchangeSynUserInfoThread exchangeSynUserInfoThread = null;

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public RequestHandlerThread(Socket socket, ExchangeServer exchangeServer) {
		this.socket = socket;
		this.exchangeServer = exchangeServer;
		this.userInfoMap = exchangeServer.getUserInfoMap();
		this.exchangeSynUserInfoThread = exchangeServer.getExchangeSynUserInfoThread();
	}

	private Request parseRequestString(String requestString) {
		if (requestString == null) {
			return new Request();
		}

		String[] tokens = requestString.replaceAll("\\s+", "").split(",");

		if (tokens.length != 6) {
			logger.error("Wrong request format: " + requestString);
			return new Request();
		}

		String action = null;
		if (tokens[3].toLowerCase().equals("b")) {
			action = "buy";
		} else if (tokens[3].toLowerCase().equals("s")) {
			action = "sell";
		} else {
			action = "";
		}

		DateFormat dateFormat = Utils.getDateFormat();

		try {
			return new Request(tokens[0] + " " + tokens[1], dateFormat, tokens[2], action, tokens[4], Integer.parseInt(tokens[5]));
		} catch (ParseException e) {
			e.printStackTrace();
			logger.error("Error occurs in parsing date in the request.");
			return new Request();
		}
	}

	/**
	 * When an object implementing interface <code>Runnable</code> is used
	 * to create a thread, starting the thread causes the object's
	 * <code>run</code> method to be called in that separately executing
	 * thread.
	 * <p/>
	 * The general contract of the method <code>run</code> is that it may
	 * take any action whatsoever.
	 *
	 * @see Thread#run()
	 */
	@Override
	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

			while (true) {
				String requestString = in.readLine();
				if (requestString != null) {
					Request request = parseRequestString(requestString);
					Response response = null;
					response = checkRequest(request, userInfoMap);

					if (response.isSuccess()) {
						response = exchangeServer.transact(request);
					}
					updateUserInfoMap(request, response, userInfoMap);
					out.println(response.getMsg());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("edu.uchicago.cs.Client socket connection to exchange server throws an IO exception.");
		} finally {
			try {
				socket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
				logger.error("edu.uchicago.cs.Client socket connection to exchange server throws an IO exception.");
			}
		}
	}

	private Response checkRequest(Request request, ConcurrentHashMap<String, UserInfo> userInfoMap) {
		String userName = request.getUserName();
		String action = request.getAction();
		String stockName = request.getStockName();
		int numOfShares = request.getNumOfShares();

		// buy, ensure you have enough cash
		if (action != null && action.equals("buy")) {
			UserInfo userInfo = null;

			// first time user, only in the case of buying
			// @TODO register user, set available capital
			if (userInfoMap.containsKey(userName)) {
				userInfo = userInfoMap.get(userName);
			} else {
				userInfo = new UserInfo(userName);
				userInfoMap.put(userName, userInfo);
				exchangeSynUserInfoThread.addChangedElement(userInfo);
			}

			// first time stock, only in the case of buying
			Stock stock = null;
			if (userInfo.getStockMap().containsKey(stockName)) {
				stock = userInfo.getStockMap().get(request.getStockName());
			} else {
				stock = new Stock(stockName);
				userInfo.getStockMap().put(stockName, stock);
			}

			// has userinfo, set capital with request
			if (userInfoMap.containsKey(userName)) {
				// could be determined by user before sending the request, here use all available capital to buy a stock
				userInfo = userInfoMap.get(userName);
				request.setAssociatedCapital(userInfo.getAvailableCapital());
				userInfo.setAvailableCapital(0.0);
			} else {
				String errMsg = request.getDateFormat().format(request.getDate()) + " No cash - Reject: Buy " + stockName + " " + numOfShares;
				logger.error(errMsg);
				return new Response(false, errMsg);
			}
		} else if (action != null && action.equals("sell")) {
			// has userinfo && has stock
			boolean hasShares = userInfoMap.containsKey(userName) && userInfoMap.get(userName).getStockMap().containsKey(stockName);
			if (!hasShares) {
				String errMsg = request.getDateFormat().format(request.getDate()) + " No Inventory: Sell " + stockName + " " + numOfShares;
				logger.error(errMsg);
				return new Response(false, errMsg);
			}

			// has enough stock shares
			int numOfAvailableShares = userInfoMap.get(userName).getStockMap().get(stockName).getNumOfAvailableShares();
			boolean hasEnoughShares = (numOfAvailableShares >= numOfShares);
			if (!hasEnoughShares) {
				String errMsg = request.getDateFormat().format(request.getDate()) + " Not enough Inventory: Sell " + stockName + " " + numOfShares + " > " + numOfAvailableShares;
				logger.error(errMsg);
				return new Response(false, errMsg);
			}
		} else {
			String errMsg = action + " is not supported.";
			logger.error(errMsg);
			return new Response(false, errMsg);
		}

		return new Response(true);
	}

	private void updateUserInfoMap(Request request, Response response, ConcurrentHashMap<String, UserInfo> userInfoMap) {
		String userName = request.getUserName();
		UserInfo userInfo = userInfoMap.get(userName);
		Stock stock = userInfo.getStockMap().get(request.getStockName());
		String action = request.getAction();
		int numOfShares = request.getNumOfShares();

		if (response.isSuccess()) {
			// update stock numOfAvailableShares, user availableCapital
			if (action != null && action.toLowerCase().equals("buy")) {
				stock.setNumOfAvailableShares(stock.getNumOfAvailableShares() + numOfShares);
				userInfo.setAvailableCapital(userInfo.getAvailableCapital() + response.getRemainedCapital());    // add back remaining capital after buying
				exchangeSynUserInfoThread.addChangedElement(userInfo);
			} else if (action != null && action.toLowerCase().equals("sell")) {
				stock.setNumOfAvailableShares(stock.getNumOfAvailableShares() - numOfShares);
				userInfo.setAvailableCapital(userInfo.getAvailableCapital() + response.getStockPrice() * numOfShares);
				exchangeSynUserInfoThread.addChangedElement(userInfo);
			} else {
				logger.error(action + " is not supported.");
			}
		} else {
			if (action != null && action.toLowerCase().equals("buy")) {
				userInfo.setAvailableCapital(userInfo.getAvailableCapital() + response.getRemainedCapital());    // add back remaining capital after buying
				exchangeSynUserInfoThread.addChangedElement(userInfo);
			}
		}
	}
}
