package edu.uchicago.cs;

import org.jgroups.Address;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RpcDispatcher;

import java.util.concurrent.ConcurrentHashMap;

public class ExchangeSynUserInfoThread extends BaseSynThread<UserInfo> {
	public ExchangeSynUserInfoThread(RpcDispatcher rpcDispatcher, ConcurrentHashMap.KeySetView<Address, Boolean> sentinelAddSet, String exchangName) {
		super(rpcDispatcher, sentinelAddSet, exchangName);
		this.errorMsg = "Fail to syn a changed userinfo with sentinel.";
	}

	@Override
	protected MethodCall getMethodCall(UserInfo element) {
		return new MethodCall("updateUserInfoMap", new Object[]{exchangName, element},
				new Class[]{String.class, UserInfo.class});
	}
}
