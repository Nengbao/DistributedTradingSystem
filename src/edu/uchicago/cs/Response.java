package edu.uchicago.cs;

import org.jgroups.Address;

import java.io.Serializable;

public class Response implements Serializable {
	private boolean isSuccess = false;
	private String msg = null;
	private double stockPrice = 0.0;
	private double remainedCapital = 0.0;
	private Address associatedExchangeAdd = null;

	public Response() {
	}

	public Response(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}

	public Response(boolean isSuccess, String msg) {
		this.isSuccess = isSuccess;
		this.msg = msg;
	}

	public Response(boolean isSuccess, String msg, double stockPrice) {
		this.isSuccess = isSuccess;
		this.msg = msg;
		this.stockPrice = stockPrice;
	}

	public Response(boolean isSuccess, String msg, double stockPrice, double remainedCapital) {
		this.isSuccess = isSuccess;
		this.msg = msg;
		this.remainedCapital = remainedCapital;
		this.stockPrice = stockPrice;
	}

	public boolean isSuccess() {
		return isSuccess;
	}

	public String getMsg() {
		return msg;
	}

	public double getRemainedCapital() {
		return remainedCapital;
	}

	public double getStockPrice() {
		return stockPrice;
	}

	public Address getAssociatedExchangeAdd() {
		return associatedExchangeAdd;
	}

	public void setSuccess(boolean success) {
		isSuccess = success;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public void setRemainedCapital(double remainedCapital) {
		this.remainedCapital = remainedCapital;
	}

	public void setStockPrice(double stockPrice) {
		this.stockPrice = stockPrice;
	}

	public void setAssociatedExchangeAdd(Address associatedExchangeAdd) {
		this.associatedExchangeAdd = associatedExchangeAdd;
	}
}
