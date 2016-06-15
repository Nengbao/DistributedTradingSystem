package edu.uchicago.cs;// @TODO use JGroups to implement communicate between client and exchange

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;

public class Client {
	private String exchangeIp = null;
	private int exchangePortNum = 0;
	private boolean registerOrLogin = false;
	private String userName = null;
	private String userPwd = null;
	private String actionFilePath = null;

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public Client(String exchangeIp, int exchangePortNum, String actionFilePath) {
		this.exchangeIp = exchangeIp;
		this.exchangePortNum = exchangePortNum;
		this.actionFilePath = actionFilePath;
	}

	private void start() throws IOException {
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			logger.error("Error occurs in thread sleep.");
			e.printStackTrace();
		}

		Socket socket = new Socket(exchangeIp, exchangePortNum);
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

		Scanner scanner = new Scanner(new File(actionFilePath));
		boolean isFirstLine = true;

		logger.info("Transactions begin!");
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine().replaceAll("\\s+", "");

			// skip first line
			if (isFirstLine) {
				isFirstLine = false;
				continue;
			}

			// send request
			out.println(line);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error("Error occurs in thread sleep.");
				e.printStackTrace();
			}

			// get back result
			String[] results = in.readLine().split("; ");
			for (String result : results) {
				System.out.println(result);
			}
		}

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			logger.error("Error occurs in thread sleep.");
			e.printStackTrace();
		}

		logger.info("Transactions complete!");
		socket.close();
		scanner.close();
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

		Client client = new Client(argMap.get(flags[0]), Integer.parseInt(argMap.get(flags[1])), argMap.get(flags[2]));
		client.start();
	}
}
