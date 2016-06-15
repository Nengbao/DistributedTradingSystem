package edu.uchicago.cs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultipleClients {
	private String exchangeIp = null;
	private int exchangePortNum = 0;
	private boolean registerOrLogin = false;
	private String userName = null;
	private String userPwd = null;
	private String actionFilePath = null;

	private ExecutorService pool = Executors.newCachedThreadPool();
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static ArrayList<String> actionList = new ArrayList<>();	//only read, no write, thus not use concurrent

	public MultipleClients(String exchangeIp, int exchangePortNum, String actionFilePath) {
		this.exchangeIp = exchangeIp;
		this.exchangePortNum = exchangePortNum;
		this.actionFilePath = actionFilePath;
	}

	private void start() throws FileNotFoundException {
		// read in actions into a list
		Scanner scanner = new Scanner(new File(actionFilePath));
		boolean isFirstLine = true;

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine().replaceAll("\\s+", "");
			// skip first line
			if (isFirstLine) {
				isFirstLine = false;
				continue;
			}
			// add to list
			actionList.add(line);
		}

		// wait for exchange to listen on a port
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			logger.error("Error occurs in thread sleep.");
			e.printStackTrace();
		}

		int numOfClients = 1000;
		for (int i = 0; i < numOfClients; i++) {
			Runnable clientTransactThread = new ClientTransactThread(String.valueOf(i), this);
			pool.submit(clientTransactThread);
		}
	}

	public String getExchangeIp() {
		return exchangeIp;
	}

	public int getExchangePortNum() {
		return exchangePortNum;
	}

	public static ArrayList<String> getActionList() {
		return actionList;
	}

	public static void main(String[] args) throws IOException {
		String[] flags = new String[]{"--exchangeIp", "--exchangePortNum", "--file"};
		String argErrMsg = "Wrong argument format. " + "Example: " + flags[0] + "=127.0.0.1 " + flags[1] + "=10000 "
				+ flags[2] + "=data.txt";

		Map<String, String> argMap = Utils.parseCmdArgs(args, argErrMsg);
		boolean areArgsCorrect = Utils.checkArgs(argMap, flags);
		if (!areArgsCorrect) {
			System.out.println(argErrMsg);
			return;
		}

		MultipleClients multipleClients = new MultipleClients(argMap.get(flags[0]), Integer.parseInt(argMap.get(flags[1])), argMap.get(flags[2]));
		multipleClients.start();
	}
}
