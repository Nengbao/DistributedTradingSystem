package edu.uchicago.cs;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

public class Request implements Serializable {
	private Date date = null;
	private DateFormat dateFormat = null;
	private String userName = null;
	private String action = null;
	private String stockName = null;
	private int numOfShares = 0;
	private double associatedCapital = 0.0;
	private boolean isRecursiveExchange = true;
	private boolean isRecursiveSentinel = true;

	public Request() {
	}

	public Request(String date, DateFormat dateFormat, String userName, String action, String stockName, int numOfShares) throws ParseException {
		this.date = dateFormat.parse(date);
		this.dateFormat = dateFormat;
		this.userName = userName;
		this.action = action;
		this.stockName = stockName;
		this.numOfShares = numOfShares;

	}

	public Date getDate() {
		return date;
	}

	public String getUserName() {
		return userName;
	}

	public String getAction() {
		return action;
	}

	public String getStockName() {
		return stockName;
	}

	public int getNumOfShares() {
		return numOfShares;
	}

	public double getAssociatedCapital() {
		return associatedCapital;
	}

	public DateFormat getDateFormat() {
		return dateFormat;
	}

	public boolean isRecursiveExchange() {
		return isRecursiveExchange;
	}

	public boolean isRecursiveSentinel() {
		return isRecursiveSentinel;
	}

	public void setAssociatedCapital(double associatedCapital) {
		this.associatedCapital = associatedCapital;
	}

	public void setRecursiveExchange(boolean recursiveExchange) {
		isRecursiveExchange = recursiveExchange;
	}

	public void setRecursiveSentinel(boolean recursiveSentinel) {
		isRecursiveSentinel = recursiveSentinel;
	}
}
