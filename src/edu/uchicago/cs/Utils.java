package edu.uchicago.cs;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.View;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Utils {

	private static DateFormat dateFormat = null;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static RequestOptions opts = new RequestOptions(ResponseMode.GET_ALL, 10 * 1000);

	// remote method must be: edu.uchicago.cs.Request as parameter and edu.uchicago.cs.Response as return type
	public static Response rpcTransact(RpcDispatcher rpcDispatcher, Address dest, MethodCall methodCall, String errorMsg) {
//		RequestOptions opts = new RequestOptions(ResponseMode.GET_ALL, 15 * 1000);
		Response rsp = null;
		try {
			rsp = rpcDispatcher.callRemoteMethod(dest, methodCall, opts);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(errorMsg);
		}

		if (rsp != null) {
			return rsp;
		} else {
			logger.error(errorMsg);
		}

		// error
		Response response = new Response(false, errorMsg);

		// set back remainedCapital in case of buy
		Request request = (Request) methodCall.getArgs()[0];
		response.setRemainedCapital(request.getAssociatedCapital());

		return response;
	}

	// remote method must return edu.uchicago.cs.Response
	public static Response rpcFunc(RpcDispatcher rpcDispatcher, Address dest, MethodCall methodCall, String errorMsg) {
		ArrayList<Address> dests = new ArrayList<>();
		dests.add(dest);
		return rpcFuncsAllSuccessful(rpcDispatcher, dests, methodCall, errorMsg);
	}

	// remote method must return edu.uchicago.cs.Response, successful only if all of the remote calls are successful
	public static Response rpcFuncsAllSuccessful(RpcDispatcher rpcDispatcher, Collection<Address> dests, MethodCall methodCall, String errorMsg) {
//		RequestOptions opts = new RequestOptions(ResponseMode.GET_ALL, 15 * 1000);
		RspList<Response> rsps = null;
		try {
			rsps = rpcDispatcher.callRemoteMethods(dests, methodCall, opts);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(errorMsg + " RPC call exception");
			return new Response(false, errorMsg);
		}

		// deal with case where dests are of size 0
		if (rsps != null && rsps.size() == 0) {
			logger.error(errorMsg + " Destination addresses is of size 0");
			return new Response(false, errorMsg);
		}

		// check result
		boolean isSucess = true;
		String msg = "Fail to receive the reponse or the response is suspected.";
		Iterator<Rsp<Response>> resultIterator = rsps.iterator();
		while (resultIterator.hasNext()) {
			Rsp<Response> rsp = resultIterator.next();
			if (rsp.wasReceived() && !rsp.wasSuspected()) {
				isSucess = isSucess && rsp.getValue().isSuccess();
				msg = rsp.getValue().getMsg();
			} else {
				logger.error(errorMsg + msg);
				isSucess = isSucess && false;
			}
		}

		return new Response(isSucess, msg);
	}

	//@TODO refactor those methods
	// remote method must return edu.uchicago.cs.Response
	public static <T> T rpcGenericFunc(RpcDispatcher rpcDispatcher, Address dest, MethodCall methodCall, String errorMsg) {
		T rsp = null;
		try {
			rsp = rpcDispatcher.callRemoteMethod(dest, methodCall, opts);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(errorMsg + " RPC call exception");
			return null;
		}

		return rsp;
	}

	public static <T> RspList<T> rpcGenericFuncsAllSuccessful(RpcDispatcher rpcDispatcher, Collection<Address> dests, MethodCall methodCall, String errorMsg) {
		RspList<T> rsps = null;
		try {
			rsps = rpcDispatcher.callRemoteMethods(dests, methodCall, opts);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(errorMsg + " RPC call exception");
			return null;
		}

		// deal with case where dests are of size 0
		if (rsps != null && rsps.size() == 0) {
			logger.error(errorMsg + " Destination addresses is of size 0");
			return null;
		}

		// check result
		Iterator<Rsp<T>> resultIterator = rsps.iterator();
		while (resultIterator.hasNext()) {
			Rsp<T> rsp = resultIterator.next();
			if (!(rsp.wasReceived() && !rsp.wasSuspected())) {
				return null;
			}
		}

		return rsps;
	}

	/* 	remote method must return edu.uchicago.cs.Response, successful if one of the remote calls is successful
		return the sentinel address, not the exchange address
	 */

	public static Address rpcSentinelsSingleSuccessful(RpcDispatcher rpcDispatcher, Collection<Address> dests, MethodCall methodCall, String errorMsg) {
//		RequestOptions opts = new RequestOptions(ResponseMode.GET_ALL, 15 * 1000);
		RspList<Response> rsps = null;
		try {
			rsps = rpcDispatcher.callRemoteMethods(dests, methodCall, opts);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(errorMsg);
			return null;
		}

		// check result
		Iterator<Map.Entry<Address, Rsp<Response>>> resultIterator = rsps.entrySet().iterator();
		while (resultIterator.hasNext()) {
			Map.Entry<Address, Rsp<Response>> entry = resultIterator.next();
			Rsp<Response> rsp = entry.getValue();
			if (rsp.wasReceived() && !rsp.wasSuspected() && rsp.getValue().isSuccess()) {
				return entry.getKey();
			}
		}

		logger.error(errorMsg);
		return null;
	}

	public static void findSentinels(JChannel channel, String sentinelName, ConcurrentHashMap.KeySetView<Address, Boolean> sentinelAddSet) {
		// wait till exchange joins the cluster completely
		int i = 20;
		while ((i >= 0) && channel.getView() == null) {
			try {
				Thread.sleep(100);
				i--;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		View view = channel.getView();
		if (view != null) {
			for (Address address : view.getMembers()) {
				// the name of a node is the sentinel name
				// in the case of a sentinel looking for peer sentinels, exclude itself
				if (channel.getName(address).equals(sentinelName) && !channel.getAddress().equals(address)) {
					sentinelAddSet.add(address);
				}
			}
		} else {
			String errMsg = "Sentinel has not joined the cluster completely, cannot get sentinel addresses.";
			logger.error(errMsg);
			throw new RuntimeException(errMsg);
		}
	}

	public static Map<String, String> parseCmdArgs(String[] args, String argErrMsg) {
		Map<String, String> argMap = new HashMap<>();

		for (String arg : args) {
			int equalSignInd = arg.indexOf("=");
			if (equalSignInd == -1) {
				System.out.println(argErrMsg);
				return new HashMap<>();
			}
			argMap.put(arg.substring(0, equalSignInd), arg.substring(equalSignInd + 1));
		}

		return argMap;
	}

	public static boolean checkArgs(Map<String, String> argMap, String[] flags) {
		boolean areArgsCorrect = argMap.size() == flags.length;

		for (int i = 0; i < flags.length; i++) {
			areArgsCorrect = areArgsCorrect && argMap.containsKey(flags[i]);
		}

		return areArgsCorrect;
	}

	// singleton
	public static final DateFormat getDateFormat() {
		if (dateFormat == null) {
			return new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.ENGLISH);
		}

		return dateFormat;
	}
}
