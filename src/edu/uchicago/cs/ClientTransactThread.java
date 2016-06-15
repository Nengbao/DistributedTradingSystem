package edu.uchicago.cs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.util.ArrayList;

public class ClientTransactThread implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private String userName = null;
	private String exchangeIp = null;
	private int exchangePortNum = 0;
	private ArrayList<String> actionList = null;

	public ClientTransactThread(String userName, MultipleClients multipleClients) {
		this.userName = userName;
		this.exchangeIp = multipleClients.getExchangeIp();
		this.exchangePortNum = multipleClients.getExchangePortNum();
		this.actionList = MultipleClients.getActionList();
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
		Socket socket = null;
		int numOfTrials = 0;
		boolean isSuccess = false;
		while (!isSuccess && numOfTrials < 5) {
			// binary exponential backoff
			int sleepTime = 100 * (1 + (int) (Math.random() * ((int)Math.pow(2, numOfTrials))));
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			try {
				socket = new Socket(exchangeIp, exchangePortNum);
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

				logger.info("Transactions begin!");
				int numOfActions = actionList.size();
//			while (true) {
//				int i = (int)(Math.random() * (numOfActions + 1));
				for (int i = 0; i < numOfActions; i++) {
					// send request
					String line = actionList.get(i).replaceFirst(",.,", "," + userName + ",");    // replace userName
					out.println(line);

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						logger.error("Error occurs in thread sleep.");
						e.printStackTrace();
					}

					// get back result
					String result = in.readLine();
					if (result != null) {
						String[] tokens = result.split("; ");
						for (String token : tokens) {
							System.out.println(token);
						}
					}

					i = (i == numOfActions - 1) ? -1 : i;
				}
				logger.info("Transactions complete!");
				isSuccess = true;
			} catch (IOException e1) {
				e1.printStackTrace();
				numOfTrials++;
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
