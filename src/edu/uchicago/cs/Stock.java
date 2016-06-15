package edu.uchicago.cs;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Stock implements Serializable {
	private String name = null;
	private double price = 0.0;
	private int numOfAvailableShares = 0;

	private List<Issue> issueList = new LinkedList<>();
	private List<PriceUpdate> priceUpdateList = new LinkedList<>();

	public Stock() {
	}

	public Stock(String name) {
		this.name = name;
	}

	public Stock(String name, double price) {
		this.name = name;
		this.price = price;
	}

	public Stock(String name, double price, int numOfAvailableShares) {
		this.name = name;
		this.price = price;
		this.numOfAvailableShares = numOfAvailableShares;
	}

	public void update(Date date) {
		// @NOTE if the list is changed during iteration, it's unsafe
		// update stock shares
		Iterator<Issue> issueIterator = issueList.iterator();
		while (issueIterator.hasNext()) {
			Issue issue = issueIterator.next();
			if (issue.getDate().compareTo(date) <= 0) {
				numOfAvailableShares += issue.getNumIssued();
				issueIterator.remove();
			} else {
				break;
			}
		}

		// update price
		Iterator<PriceUpdate> priceUpdateIterator = priceUpdateList.iterator();
		while (priceUpdateIterator.hasNext()) {
			PriceUpdate priceUpdate = priceUpdateIterator.next();
			if (priceUpdate.getDate().compareTo(date) <= 0) {
				price = priceUpdate.getNewPrice();
				priceUpdateIterator.remove();
			} else {
				break;
			}
		}
	}

	public String getName() {
		return name;
	}

	public double getPrice() {
		return price;
	}

	public int getNumOfAvailableShares() {
		return numOfAvailableShares;
	}

	public List<Issue> getIssueList() {
		return issueList;
	}

	public List<PriceUpdate> getPriceUpdateList() {
		return priceUpdateList;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public void setNumOfAvailableShares(int numOfAvailableShares) {
		this.numOfAvailableShares = numOfAvailableShares;
	}

	public void setIssueList(List<Issue> issueList) {
		this.issueList = issueList;
	}

	public void setPriceUpdateList(List<PriceUpdate> priceUpdateList) {
		this.priceUpdateList = priceUpdateList;
	}
}
