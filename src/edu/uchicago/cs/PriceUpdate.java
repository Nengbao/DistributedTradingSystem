package edu.uchicago.cs;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

public class PriceUpdate implements Serializable {
	private Date date = null;
	private double newPrice = 0.0;

	public PriceUpdate(double newPrice, String date, DateFormat dateFormat) throws ParseException {
		this.date = dateFormat.parse(date);
		this.newPrice = newPrice;
	}

	public Date getDate() {
		return date;
	}

	public double getNewPrice() {
		return newPrice;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public void setNewPrice(double newPrice) {
		this.newPrice = newPrice;
	}
}
