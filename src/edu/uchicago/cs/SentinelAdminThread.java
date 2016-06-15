package edu.uchicago.cs;

import org.jgroups.Address;

import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class SentinelAdminThread extends BaseAdminThread {
	private SentinelServer sentinelServer = null;

	public SentinelAdminThread(SentinelServer sentinelServer) {
		this.sentinelServer = sentinelServer;
	}

	@Override
	protected void admin() {
		Scanner scanner = new Scanner(System.in);

		while (scanner.hasNext()) {
			String cmd = scanner.nextLine().trim().toLowerCase();
			if ("getStockToExchangeNameMap".toLowerCase().equals(cmd)) {
				printstockToExchangeNameMap();
			} else if ("getStockMapBackup".toLowerCase().equals(cmd)) {
				printStockMapBackup();
			} else if ("getExchangeNameToAddMap".toLowerCase().equals(cmd)) {
				printExchangeNameToAddMap();
			} else if ("getUserInfoMapBackup".toLowerCase().equals(cmd)) {
				printUserInfoMapBackup();
			}
		}
	}

	private void printUserInfoMapBackup() {
		System.out.println("=========== EXCHANGE TO USER INFO TABLE ===========");
		ConcurrentHashMap<String, ConcurrentHashMap<String, UserInfo>> userInfoMapBackup = sentinelServer.getUserInfoMapBackup();
		for (Map.Entry<String, ConcurrentHashMap<String, UserInfo>> entry : userInfoMapBackup.entrySet()) {
			String exchangeName = entry.getKey();
			ConcurrentHashMap<String, UserInfo> userInfoMap = entry.getValue();
			System.out.println("=====" + exchangeName + "=====");
			for (UserInfo userInfo : userInfoMap.values()) {
				System.out.println("edu.uchicago.cs.Client: " + userInfo.getName());
//				System.out.println("=========== STOCK LISTED ===========");
				for (Stock stock : userInfo.getStockMap().values()) {
					System.out.println(stock.getName() + " " + stock.getNumOfAvailableShares());
				}
//				System.out.println("=========== CASH FLOW ==============");
				System.out.printf("%.2f", userInfo.getAvailableCapital());
				System.out.println("\n\n");
			}
		}
	}

	private void printstockToExchangeNameMap() {
		System.out.println("=========== STOCK TO EXCHANGE TABLE ===========");
		ConcurrentHashMap<String, String> stockToExchangeNameMap = sentinelServer.getStockToExchangeNameMap();
		for (Map.Entry<String, String> entry : stockToExchangeNameMap.entrySet()) {
			System.out.println(entry.getKey() + " ===> " + entry.getValue());
		}
	}

	private void printExchangeNameToAddMap() {
		System.out.println("=========== EXCHANGE NAME TABLE ===========");
		ConcurrentHashMap<String, Address> exchangeNameToAddMap = sentinelServer.getExchangeNameToAddMap();
		for (Map.Entry<String, Address> entry : exchangeNameToAddMap.entrySet()) {
			System.out.println("Exchange name: " + entry.getKey() + " address: " + entry.getValue());
		}
	}

	private void printStockMapBackup() {
		System.out.println("=========== EXCHANGE TO STOCK TABLE ===========");
		ConcurrentHashMap<String, ConcurrentHashMap<String, Stock>> stockMapBackup = sentinelServer.getStockMapBackup();
		for (Map.Entry<String, ConcurrentHashMap<String, Stock>> entry : stockMapBackup.entrySet()) {
			String exchangeName = entry.getKey();
			ConcurrentHashMap<String, Stock> stockMap = entry.getValue();
			System.out.println("Exchange name: " + exchangeName);
			for (Map.Entry<String, Stock> entry1 : stockMap.entrySet()) {
				Stock stock = entry1.getValue();
				System.out.println(stock.getName() + ": " + String.format("%.2f", stock.getPrice()) + ", " + stock.getNumOfAvailableShares());
			}
			System.out.println("\n");
		}
	}
}
