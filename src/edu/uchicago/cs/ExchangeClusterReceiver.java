package edu.uchicago.cs;

import org.jgroups.Address;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class ExchangeClusterReceiver extends ReceiverAdapter {
	private View exchangeClusterOldView = null;

	private SentinelServer sentinelServer = null;
	private ConcurrentHashMap<String, Address> exchangeNameToAddMap = null;
	private ConcurrentHashMap<Address, String> exchangeAddToNameMap = new ConcurrentHashMap<>();
	private ExecutorService pool = null;

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

/*
	public edu.uchicago.cs.ExchangeClusterReceiver(ConcurrentHashMap<String, Address> exchangeNameToAddMap,
								   ConcurrentHashMap<Address, String> exchangeAddToNameMap) {
		this.exchangeNameToAddMap = exchangeNameToAddMap;
		this.exchangeAddToNameMap = exchangeAddToNameMap;
	}
*/

	public ExchangeClusterReceiver(SentinelServer sentinelServer) {
		this.sentinelServer = sentinelServer;
		this.exchangeNameToAddMap = sentinelServer.getExchangeNameToAddMap();
		this.exchangeAddToNameMap = sentinelServer.getExchangeAddToNameMap();
		this.pool = sentinelServer.getPool();
	}

	@Override
	public void viewAccepted(View view) {
		System.out.println(view);
		if (exchangeClusterOldView == null) {
			exchangeClusterOldView = view;
			return;
		}

		List<Address> leftMembers = null;
		List<Address> joinedMembers = null;
		if (exchangeClusterOldView.size() > view.size()) {
			leftMembers = View.leftMembers(exchangeClusterOldView, view);
		} else if (exchangeClusterOldView.size() < view.size()) {
			joinedMembers = View.leftMembers(view, exchangeClusterOldView);
		}

		if (leftMembers != null) {
			for (Address address : leftMembers) {
				if (exchangeAddToNameMap.containsKey(address)) {
					String exchangeName = exchangeAddToNameMap.get(address);
					exchangeAddToNameMap.remove(address);
					exchangeNameToAddMap.remove(exchangeName);
					logger.info(exchangeName + " left the " + sentinelServer.getExchangeClusterName() + "cluster.");
				}
			}
		}

		// recover exchange server
/*
		if (joinedMembers != null) {
			Runnable recoverExchangeInfoThread = new RecoverExchangeInfoThread(joinedMembers, sentinelServer);
			pool.submit(recoverExchangeInfoThread);
		}
*/

		exchangeClusterOldView = view;

	}

}
