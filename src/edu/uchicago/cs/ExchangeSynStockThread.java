package edu.uchicago.cs;

import org.jgroups.Address;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RpcDispatcher;

import java.util.concurrent.ConcurrentHashMap;

public class ExchangeSynStockThread extends BaseSynThread<Stock> {
	public ExchangeSynStockThread(RpcDispatcher rpcDispatcher, ConcurrentHashMap.KeySetView<Address, Boolean> sentinelAddSet, String exchangName) {
		super(rpcDispatcher, sentinelAddSet, exchangName);
		this.errorMsg = "Fail to syn a changed stock with sentinel.";
	}

	@Override
	protected MethodCall getMethodCall(Stock element) {
		return new MethodCall("updateStockMap", new Object[]{exchangName, element},
				new Class[]{String.class, Stock.class});
	}
}
