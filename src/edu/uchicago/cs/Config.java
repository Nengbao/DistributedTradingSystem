package edu.uchicago.cs;

import java.util.HashMap;
import java.util.Map;

public class Config {
	private static Map<String, String> clusterToConfigFilePathMap = new HashMap<>();

	static {
		clusterToConfigFilePathMap.put("africa", "../../resources/africa_exchange_cluster_config.xml");
		clusterToConfigFilePathMap.put("asia", "../../resources/asia_exchange_cluster_config.xml");
		clusterToConfigFilePathMap.put("australia", "../../resources/australia_exchange_cluster_config.xml");
		clusterToConfigFilePathMap.put("europe", "../../resources/europe_exchange_cluster_config.xml");
		clusterToConfigFilePathMap.put("north_america", "../../resources/north_america_exchange_cluster_config.xml");
		clusterToConfigFilePathMap.put("south_america", "../../resources/south_america_exchange_cluster_config.xml");
		clusterToConfigFilePathMap.put("sentinel", "../../resources/sentinel_cluster_config.xml");
	}

	private static Map<String, String> exchangeClusterToSentinelName = new HashMap<>();
	static {
		exchangeClusterToSentinelName.put("africa", "africa_sentinel");
		exchangeClusterToSentinelName.put("asia", "asia_sentinel");
		exchangeClusterToSentinelName.put("australia", "australia_sentinel");
		exchangeClusterToSentinelName.put("europe", "europe_sentinel");
		exchangeClusterToSentinelName.put("north_america", "north_america_sentinel");
		exchangeClusterToSentinelName.put("south_america", "south_america_sentinel");
	}

	public static String getConfigFilePath(String clusterName) {
		return clusterToConfigFilePathMap.get(clusterName);
	}

	public static String getSentinelName(String exchangeClusterName) {
		return exchangeClusterToSentinelName.get(exchangeClusterName);
	}
}
