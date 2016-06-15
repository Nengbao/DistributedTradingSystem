package edu.uchicago.cs;

import org.jgroups.Address;

import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class ExchangeAdminThread extends BaseAdminThread {
	private ExchangeServer exchangeServer = null;
	private ConcurrentHashMap<String, UserInfo> userInfoMap;

	public ExchangeAdminThread(ExchangeServer exchangeServer) {
		this.exchangeServer = exchangeServer;
		this.userInfoMap = exchangeServer.getUserInfoMap();
	}

	@Override
	protected void admin() {
		Scanner scanner = new Scanner(System.in);

		while (scanner.hasNext()) {
			String cmd = scanner.nextLine().trim().toLowerCase();
			if (("getUserInfoMap").toLowerCase().equals(cmd)) {
				printUserInfoMap();
			} else if ("getNumOfUsers".toLowerCase().equals(cmd)) {
				printNumOfUsers();
			} else if (("getSentinelAddSet").toLowerCase().equals(cmd)) {
				printSentinelAddSet();
			} else if (("getStockMap").toLowerCase().equals(cmd)) {
				printStockMap();
			}
		}
	}

	private void printStockMap() {
		System.out.println("=========== EXCHANGE STOCK TABLE ===========");
		ConcurrentHashMap<String, Stock> stockMap = exchangeServer.getStockMap();
		for (Map.Entry<String, Stock> entry : stockMap.entrySet()) {
			Stock stock = entry.getValue();
			System.out.println(stock.getName() + ": " + String.format("%.2f", stock.getPrice()) + ", " + stock.getNumOfAvailableShares());
		}
		System.out.println("\n");
	}

	private void printUserInfoMap() {
		for (UserInfo userInfo : userInfoMap.values()) {
			System.out.println("edu.uchicago.cs.Client: " + userInfo.getName());
			System.out.println("=========== STOCK LISTED ===========");
			for (Stock stock : userInfo.getStockMap().values()) {
				System.out.println(stock.getName() + " " + stock.getNumOfAvailableShares());
			}
			System.out.println("=========== CASH FLOW ==============");
			System.out.printf("%.2f", userInfo.getAvailableCapital());
			System.out.println("\n\n");
		}
	}

	private void printNumOfUsers() {
		System.out.println("=========== NUM OF USERS ==============");
		System.out.println(userInfoMap.size());
	}

	private void printSentinelAddSet() {
		System.out.println("=========== SENTINEL ADDRESSES ===========");
		for (Address address : exchangeServer.getSentinelAddSet()) {
			System.out.println(address);
		}
		System.out.println("\n");
	}


}
