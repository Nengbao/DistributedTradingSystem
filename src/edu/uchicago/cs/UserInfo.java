package edu.uchicago.cs;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class UserInfo implements Serializable {
	private String name = null;
	private String password = null;
	private ConcurrentHashMap<String, Stock> stockMap = new ConcurrentHashMap<>();
	private double cashFlow = 0.0;
	private double availableCapital = 10000.0;

	public UserInfo() {
	}

	public UserInfo(String name) {
		this.name = name;
	}

	public UserInfo(String name, String password) {
		this.name = name;
		this.password = password;
	}

	public String getName() {
		return name;
	}

	public String getPassword() {
		return password;
	}

	public ConcurrentHashMap<String, Stock> getStockMap() {
		return stockMap;
	}

	public double getCashFlow() {
		return cashFlow;
	}

	public double getAvailableCapital() {
		return availableCapital;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setAvailableCapital(double availableCapital) {
		this.availableCapital = availableCapital;
	}
}
