package edu.uchicago.cs;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

public class Issue implements Serializable {
	private Date date = null;
	private int numIssued = 0;

	public Issue(int numIssued, String date, DateFormat dateFormat) throws ParseException {
		this.date = dateFormat.parse(date);
		this.numIssued = numIssued;
	}

	public Date getDate() {
		return date;
	}

	public int getNumIssued() {
		return numIssued;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public void setNumIssued(int numIssued) {
		this.numIssued = numIssued;
	}
}
