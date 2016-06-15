package edu.uchicago.cs;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RpcDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExchangeServer extends ReceiverAdapter {
	//	private ConcurrentHashMap<String, Address> sentinelAddMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap.KeySetView<Address, Boolean> sentinelAddSet = ConcurrentHashMap.newKeySet();
	private ConcurrentHashMap<String, Stock> stockMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, UserInfo> userInfoMap = new ConcurrentHashMap<>();

	private String exchangeName = null;
	private String clusterName = null;
	private String stockFilePath = null;
	private String priceFilePath = null;
	private boolean isRestart = false;
	private int portNum = 0;

	private JChannel channel = null;
	private View oldView = null;
	private RpcDispatcher rpcDispatcher = null;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static ExchangeServer exchangeServer = null;
	private ExchangeSynStockThread exchangeSynStockThread = null;
	private ExchangeSynUserInfoThread exchangeSynUserInfoThread = null;
//	private boolean isRecovered = false;

	// singleton
	public static final ExchangeServer getExchangeServer(String exchangeName, String clusterName, String stockFilePath,
														 String priceFilePath) throws Exception {
		if (exchangeServer == null) {
			exchangeServer = new ExchangeServer(exchangeName, clusterName, stockFilePath, priceFilePath);
		}
		return exchangeServer;
	}

	public static final ExchangeServer getExchangeServer() {
		if (exchangeServer != null) {
			return exchangeServer;
		}
		throw new RuntimeException("edu.uchicago.cs.ExchangeServer has never been initialized, please initialize it with the constructor with arguments");
	}

	private ExchangeServer(String exchangeName, String clusterName, String stockFilePath, String priceFilePath) throws Exception {
		this.exchangeName = exchangeName;
		this.clusterName = clusterName;
		this.stockFilePath = stockFilePath;
		this.priceFilePath = priceFilePath;

		String configFilePath = Config.getConfigFilePath(clusterName);
		channel = new JChannel(new File(configFilePath));
		channel.setName(exchangeName);
		channel.setReceiver(this);
		channel.setDiscardOwnMessages(true);
		channel.connect(clusterName);
		rpcDispatcher = new RpcDispatcher(channel, this, this, this);
	}

	public void setIsRestart(boolean isRestart) {
		this.isRestart = isRestart;
	}

	public void setPortNum(String portNum) {
		this.portNum = Integer.parseInt(portNum);
	}

	@Override
	public void viewAccepted(View view) {
		System.out.println(view);
		if (oldView == null) {
			oldView = view;
			return;
		}

		List<Address> leftMembers = null;
		List<Address> joinedMembers = null;
		if (oldView.size() > view.size()) {
			leftMembers = View.leftMembers(oldView, view);
		} else if (oldView.size() < view.size()) {
			joinedMembers = View.leftMembers(view, oldView);
		}

		if (leftMembers != null) {
			for (Address address : leftMembers) {
				if (sentinelAddSet.contains(address)) {
					sentinelAddSet.remove(address);
				}
			}
		}

		if (joinedMembers != null) {
			for (Address address : joinedMembers) {
				if (channel.getName(address).equals(Config.getSentinelName(clusterName))) {
					sentinelAddSet.add(address);
				}
			}
		}

		oldView = view;
	}


	private void loadPriceUpdateList() {
		if (priceFilePath == null) {
			logger.error("Invalid price info file path.");
			return;
		}

		Scanner scanner = null;
		try {
			scanner = new Scanner(new File(priceFilePath));

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine().replaceAll("\\s+", "");
				// skip empty line
				if (line.equals("")) {
					logger.info("Empty line, process to next line...");
					continue;
				}

				String[] tokens = line.split(",");
				if (tokens.length == 4) {
					// get date
					String date = tokens[0] + " " + tokens[1];
					DateFormat dateFormat = Utils.getDateFormat();

					// get issued shares
					Stock stock = stockMap.get(tokens[2]);
					stock.setPrice(Double.parseDouble(tokens[3]));    // for simulation purpose
/*
					// @TODO comment back
					try {
						stock.getPriceUpdateList().add(new edu.uchicago.cs.PriceUpdate(Double.parseDouble(tokens[3]), date, dateFormat));
					} catch (ParseException e) {
						logger.error("Error occurs in parsing date in price info file. Process to next line....");
						e.printStackTrace();
						continue;
					}
*/
				} else {    // invalid price info
					logger.info("Not 4 columns, invalid data, process to next line...");
					continue;
				}
			}

			scanner.close();
		} catch (FileNotFoundException e) {
			logger.error("File not found: price info file.");
//			e.printStackTrace();
		}
	}

	private void loadStockMap() {
		if (stockFilePath == null) {
			logger.error("Invalid stock info file path.");
			return;
		}

		Scanner scanner = null;
		try {
			scanner = new Scanner(new File(stockFilePath));
			boolean isFirstLine = true;
			Map<Integer, String> colToStockNameMap = new HashMap<>();

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine().replaceAll("\\s+", "");
				// skip empty line
				if (line.equals("")) {
					logger.info("Empty line, process to next line...");
					continue;
				}

				String[] tokens = line.split(",");
				if (tokens.length > 3) {
					// first line, get stock names
					if (isFirstLine) {
						for (int i = 3; i < tokens.length; i++) {
							stockMap.put(tokens[i], new Stock(tokens[i]));
							colToStockNameMap.put(i, tokens[i]);
						}
						isFirstLine = false;
						continue;
					}

					// get date
					String date = tokens[0] + " " + tokens[1];
					DateFormat dateFormat = Utils.getDateFormat();

					// get issued shares
					for (int i = 3; i < tokens.length; i++) {
						int numIssued = Integer.parseInt(tokens[i]);
						String stockName = colToStockNameMap.get(i);
						Stock stock = stockMap.get(stockName);
						stock.setNumOfAvailableShares(numIssued);    // for simulation purpose
/*
						// @TODO comment back
						try {
							stock.getIssueList().add(new edu.uchicago.cs.Issue(numIssued, date, dateFormat));
						} catch (ParseException e) {
							logger.error("Error occurs in parsing date in stock info file.");
							e.printStackTrace();
							return;
						}
*/
					}
				} else {    // no available stock
					logger.info("Only less than 3 columns, invalid data, process to next line...");
					continue;
				}
			}

			scanner.close();
		} catch (FileNotFoundException e) {
			logger.error("File not found: stock info file.");
//			e.printStackTrace();
		}

	}


	private void registerStockMap() {
		MethodCall methodCall = new MethodCall("registerStockMap", new Object[]{exchangeName, channel.getAddress(), stockMap, userInfoMap},
				new Class[]{String.class, Address.class, ConcurrentHashMap.class, ConcurrentHashMap.class});
		String errorMsg = "Fail to rpc registerStockMap successfully in some sentinel servers.";

		// wait till there is some sentinel adds available
/*
		int i = 20;
		while ((i >= 0) || sentinelAddSet.isEmpty()) {
			try {
				Thread.sleep(100);
				i--;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
*/

		Response response = null;
		// check again before proceed
		if (!sentinelAddSet.isEmpty()) {
			response = Utils.rpcFuncsAllSuccessful(rpcDispatcher, sentinelAddSet, methodCall, errorMsg);
		} else {
			throw new RuntimeException("No sentinel address available in the exchange.");
		}

		if (response == null || !response.isSuccess()) {
			throw new RuntimeException(errorMsg);
		}
	}

/*
	public edu.uchicago.cs.Response recover(ConcurrentHashMap<String, edu.uchicago.cs.Stock> stockMap) {
		this.stockMap = stockMap;
		isRecovered = true;
		return new edu.uchicago.cs.Response(true);
	}

	private void waitForRecover() {
		// wait till exchange joins the cluster completely
		int i = 1000;
		while ((i >= 0) && !isRecovered) {
			try {
				Thread.sleep(100);
				i--;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}


		if (!isRecovered) {
			String errMsg = "Fail to recover exchange.";
			logger.error(errMsg);
			throw new RuntimeException(errMsg);
		}

		logger.info("Succeed to recover.");

	}
*/

	public void recover() {
		MethodCall methodCall = new MethodCall("getStockMap", new Object[]{exchangeName}, new Class[]{String.class});
		String errMsg = "Error in rpc calling recover method of Exchange.";
		stockMap = Utils.rpcGenericFunc(rpcDispatcher, sentinelAddSet.iterator().next(), methodCall, errMsg);

		methodCall = new MethodCall("getUserInfoMap", new Object[]{exchangeName}, new Class[]{String.class});
		userInfoMap = Utils.rpcGenericFunc(rpcDispatcher, sentinelAddSet.iterator().next(), methodCall, errMsg);

		if (stockMap == null || userInfoMap == null) {
			throw new RuntimeException(errMsg);
		}

		logger.info("Succeed to recover exchange.");
	}

	public Response addSentinelAdd(Address sentinelAdd) {
		try {
			sentinelAddSet.add(sentinelAdd);
			return new Response(true);
		} catch (Exception e) {
//			e.printStackTrace();
			logger.error("Fail to addSentinelAdd in exchange server");
			return new Response(false);
		}
	}

	public Response transact(Request request) {
		String action = request.getAction();
		String stockName = request.getStockName();
		int numOfShares = request.getNumOfShares();

		if (action != null && action.equals("buy")) {
			// stock is in local exchange
			if (stockMap.containsKey(stockName)) {
				Stock stock = stockMap.get(stockName);
				stock.update(request.getDate());    //@TODO syn stock with sentinel
				// succeed to buy, has enough shares && enough capital
				boolean hasEnoughShares = stock.getNumOfAvailableShares() >= numOfShares;
				double remainedCapital = request.getAssociatedCapital() - stock.getPrice() * numOfShares;
				boolean hasEnoughCapital = remainedCapital >= 0;
//				boolean hasEnoughCapital = request.getAssociatedCapital() >= 0; //buggy scenario implemented by Dr. Seb
				if (hasEnoughShares && hasEnoughCapital) {
					stock.setNumOfAvailableShares(stock.getNumOfAvailableShares() - numOfShares);
					exchangeSynStockThread.addChangedElement(stock);    //@TODO add two phase commit
					String msg = request.getDateFormat().format(request.getDate()) + " Filled: User " + request.getUserName() + " Buy " + stockName + " " + numOfShares + " " + stock.getPrice();
					logger.info(msg);
					return new Response(true, msg, stock.getPrice(), remainedCapital);
				}

				// fail to buy
				String msg = null;
				if (!hasEnoughShares) {
					msg = request.getDateFormat().format(request.getDate()) + " Exchange: " + stockName + " quantity requested(" + numOfShares + ") > (" + stock.getNumOfAvailableShares() + ")inventory; "
							+ request.getDateFormat().format(request.getDate()) + " Reject:  User " + request.getUserName() + " Buy " + stockName + " " + numOfShares;
				}
				if (!hasEnoughCapital) {
					msg = request.getDateFormat().format(request.getDate()) + " No cash - Reject:  User " + request.getUserName() + " Buy " + stockName + " " + numOfShares;
				}
				logger.info(msg);
				return new Response(false, msg, stock.getPrice(), request.getAssociatedCapital());
			}
		} else if (action != null && action.equals("sell")) {
			// stock is in local exchange
			if (stockMap.containsKey(stockName)) {
				Stock stock = stockMap.get(stockName);
				stock.update(request.getDate());
				stock.setNumOfAvailableShares(stock.getNumOfAvailableShares() + numOfShares);
				exchangeSynStockThread.addChangedElement(stock);
				String msg = request.getDateFormat().format(request.getDate()) + " Filled: User " + request.getUserName() + " Sell " + stockName + " " + numOfShares + " " + stock.getPrice();
				logger.info(msg);
				return new Response(true, msg, stock.getPrice());
			}
		} else {
			String msg = action + " is not supported.";
			logger.error(msg);
			return new Response(false, msg);
		}

		// stock is in remote exchange
		if (request.isRecursiveExchange()) {
			request.setRecursiveExchange(false);
			MethodCall methodCall = new MethodCall("transact", new Object[]{request},
					new Class[]{Request.class});
			String errorMsg = "Fail to rpc call transact " + numOfShares + " shares of " + stockName + " in the exchange.";
			return Utils.rpcTransact(rpcDispatcher, sentinelAddSet.iterator().next(), methodCall, errorMsg);    //@TODO round robin sentinels
		}

		return new Response(false, request.getDateFormat().format(request.getDate()) + " No such stock - Reject:  User " + request.getUserName() + " " + request.getAction() + " "
				+ request.getStockName() + " " + request.getNumOfShares(), request.getAssociatedCapital());
	}

	public void start() throws Exception {
		Utils.findSentinels(channel, Config.getSentinelName(channel.getClusterName()), sentinelAddSet);
		if (isRestart) {
//			waitForRecover();
			recover();
		} else {
			loadStockMap();
			loadPriceUpdateList();
			registerStockMap();
			logger.info("Succeed to register stockmap with sentinels.");
		}

		ExecutorService pool = Executors.newCachedThreadPool();
		Runnable exchangeAdminThread = new ExchangeAdminThread(this);
		exchangeSynStockThread = new ExchangeSynStockThread(rpcDispatcher, sentinelAddSet, exchangeName);
		exchangeSynUserInfoThread = new ExchangeSynUserInfoThread(rpcDispatcher, sentinelAddSet, exchangeName);
		pool.submit(exchangeAdminThread);
		pool.submit(exchangeSynStockThread);
		pool.submit(exchangeSynUserInfoThread);

		try {
			ServerSocket serverSocket = new ServerSocket(portNum);
			while (true) {
				Socket socket = serverSocket.accept();
				Runnable requestHandler = new RequestHandlerThread(socket, this);
				pool.submit(requestHandler);
			}
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Fail to start exchange as a server for user to use or fail to accept a user socket connection.");
		}
	}

	public ConcurrentHashMap<String, UserInfo> getUserInfoMap() {
		return userInfoMap;
	}

	public ConcurrentHashMap.KeySetView<Address, Boolean> getSentinelAddSet() {
		return sentinelAddSet;
	}

	public ConcurrentHashMap<String, Stock> getStockMap() {
		return stockMap;
	}

	public ExchangeSynUserInfoThread getExchangeSynUserInfoThread() {
		return exchangeSynUserInfoThread;
	}

	public static void main(String[] args) throws Exception {
		String[] flags = new String[]{"--exchangeName", "--clusterName", "--stockFile", "--priceFile", "--isRestart", "--portNum"};
		String argErrMsg = "Wrong argument format. " + "Example: " + flags[0] + "=new_york " + flags[1] + "=north_america "
				+ flags[2] + "=qty_stocks.csv " + flags[3] + "=price_stocks.csv " + flags[4] + "=false " + flags[5] + "=10000";

		Map<String, String> argMap = Utils.parseCmdArgs(args, argErrMsg);
		boolean areArgsCorrect = Utils.checkArgs(argMap, flags);
		if (!areArgsCorrect) {
			System.out.println(argErrMsg);
			return;
		}

		ExchangeServer exchangeServer = ExchangeServer.getExchangeServer(argMap.get(flags[0]), argMap.get(flags[1]),
				argMap.get(flags[2]), argMap.get(flags[3]));
		exchangeServer.setIsRestart(argMap.get(flags[4]).equals("true"));
		exchangeServer.setPortNum(argMap.get(flags[5]));

		exchangeServer.start();
	}
}
