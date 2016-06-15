package edu.uchicago.cs;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RpcDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SentinelServer extends ReceiverAdapter {
	private ConcurrentHashMap.KeySetView<Address, Boolean> sentinelAddSet = ConcurrentHashMap.newKeySet();
	private ConcurrentHashMap<String, String> stockToExchangeNameMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, ConcurrentHashMap<String, Stock>> stockMapBackup = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, ConcurrentHashMap<String, UserInfo>> userInfoMapBackup = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, Address> exchangeNameToAddMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Address, String> exchangeAddToNameMap = new ConcurrentHashMap<>();

	private String exchangeClusterName = null;
	private String exchangeClusterChannelName = null;
	private String sentinelClusterChannelName = null;
	private static final String SENTINEL_CLUSTER_NAME = "sentinel";
	boolean isRestart = false;

	private JChannel exchangeClusterChannel = null;
	private JChannel sentinelClusterChannel = null;
	private RpcDispatcher exchangeClusterRpcDispatcher = null;
	private RpcDispatcher sentinelClusterRpcDispatcher = null;
	private View sentinelClusterOldView = null;

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private ExecutorService pool = Executors.newCachedThreadPool();

	public SentinelServer(String exchangeClusterName, boolean isRestart) throws Exception {
		this.exchangeClusterName = exchangeClusterName;
		this.isRestart = isRestart;
		exchangeClusterChannelName = Config.getSentinelName(exchangeClusterName);
		sentinelClusterChannelName = exchangeClusterChannelName;

		String configFilePathForExchangeCluster = Config.getConfigFilePath(exchangeClusterName);
		exchangeClusterChannel = new JChannel(new File(configFilePathForExchangeCluster));
		exchangeClusterChannel.setName(exchangeClusterChannelName);
		ExchangeClusterReceiver exchangeClusterReceiver = new ExchangeClusterReceiver(this);
		exchangeClusterChannel.setReceiver(exchangeClusterReceiver);
		exchangeClusterChannel.setDiscardOwnMessages(true);
		exchangeClusterChannel.connect(exchangeClusterName);
//		exchangeClusterReceiver.setSelfSentinelAdd(exchangeClusterChannel.getAddress());
		exchangeClusterRpcDispatcher = new RpcDispatcher(exchangeClusterChannel, exchangeClusterReceiver, exchangeClusterReceiver, this);
//		exchangeClusterReceiver.setRpcDispatcher(exchangeClusterRpcDispatcher);

		String configFilePathForSentinelCluster = Config.getConfigFilePath(SENTINEL_CLUSTER_NAME);
		sentinelClusterChannel = new JChannel(new File(configFilePathForSentinelCluster));
		sentinelClusterChannel.setName(sentinelClusterChannelName);
//		SentinelClusterReceiver sentinelClusterReceiver = new SentinelClusterReceiver(sentinelAddSet, sentinelClusterChannel);
		sentinelClusterChannel.setReceiver(this);
		sentinelClusterChannel.setDiscardOwnMessages(true);
		sentinelClusterChannel.connect(SENTINEL_CLUSTER_NAME);
		sentinelClusterRpcDispatcher = new RpcDispatcher(sentinelClusterChannel, this, this, this);
//		sentinelClusterReceiver.setRpcDispatcher(sentinelClusterRpcDispatcher);
	}

	@Override
	public void viewAccepted(View view) {
		System.out.println(view);
		if (sentinelClusterOldView == null) {
			sentinelClusterOldView = view;
			return;
		}

		List<Address> leftMembers = null;
		List<Address> joinedMembers = null;
		if (sentinelClusterOldView.size() > view.size()) {
			leftMembers = View.leftMembers(sentinelClusterOldView, view);
		} else if (sentinelClusterOldView.size() < view.size()) {
			joinedMembers = View.leftMembers(view, sentinelClusterOldView);
		}

		if (leftMembers != null) {
			for (Address address : leftMembers) {
				if (sentinelAddSet.contains(address)) {
					sentinelAddSet.remove(address);
				}
			}
		}

/*
		if (joinedMembers != null) {
			for (Address address : joinedMembers) {
				if (sentinelClusterChannel.getName(address).equals(sentinelClusterChannel.getName())) {
					sentinelAddSet.add(address);
					MethodCall methodCall = new MethodCall("addSentinelAdd", new Object[]{sentinelClusterChannel.getAddress()}, new Class[]{Address.class});
					String errMsg = "Fail to rpc call addSentinelAdd to add existing sentinel addresses to newly joined sentinel.";
					edu.uchicago.cs.Utils.rpcFunc(sentinelClusterRpcDispatcher, address, methodCall, logger, errMsg);
				}
			}
		}
*/

		sentinelClusterOldView = view;
	}


	public Response registerStockMap(String exchangeName, Address exchangeAdd, ConcurrentHashMap<String, Stock> stockMap,
									 ConcurrentHashMap<String, UserInfo> userInfoMap) {
		try {
			exchangeNameToAddMap.put(exchangeName, exchangeAdd);
			exchangeAddToNameMap.put(exchangeAdd, exchangeName);
			stockMapBackup.put(exchangeName, stockMap);
			userInfoMapBackup.put(exchangeName, userInfoMap);

			for (String key : stockMap.keySet()) {
				stockToExchangeNameMap.put(key, exchangeName);
			}
			logger.info(exchangeName + " joined the " + exchangeClusterChannelName + " cluster.");
			logger.info(exchangeName + " succeeds to register stockmap with sentinels.");
			return new Response(true);
		} catch (Exception e) {
			logger.error("Fail to registerStockMap in the sentinel.");
			return new Response(false);
		}

	}

	public Response addSentinelAdd(Address sentinelAdd) {
		sentinelAddSet.add(sentinelAdd);
		return new Response(true);
	}

/*
	public ConcurrentHashMap<String, edu.uchicago.cs.Stock> recoverExchange(String exchangeName) {
		return null;
	}
*/

	public void recover() {
		MethodCall methodCall = new MethodCall("getStockToExchangeNameMap", new Object[]{}, new Class[]{});
		String errMsg = "Error in rpc calling recover method of Sentinel.";
		stockToExchangeNameMap = Utils.rpcGenericFunc(sentinelClusterRpcDispatcher, sentinelAddSet.iterator().next(), methodCall, errMsg);

		methodCall = new MethodCall("getStockMapBackup", new Object[]{}, new Class[]{});
		stockMapBackup = Utils.rpcGenericFunc(sentinelClusterRpcDispatcher, sentinelAddSet.iterator().next(), methodCall, errMsg);

		methodCall = new MethodCall("getUserInfoMapBackup", new Object[]{}, new Class[]{});
		userInfoMapBackup = Utils.rpcGenericFunc(sentinelClusterRpcDispatcher, sentinelAddSet.iterator().next(), methodCall, errMsg);

		methodCall = new MethodCall("getExchangeNameToAddMap", new Object[]{}, new Class[]{});
		exchangeNameToAddMap = Utils.rpcGenericFunc(sentinelClusterRpcDispatcher, sentinelAddSet.iterator().next(), methodCall, errMsg);

		methodCall = new MethodCall("getExchangeAddToNameMap", new Object[]{}, new Class[]{});
		exchangeAddToNameMap = Utils.rpcGenericFunc(sentinelClusterRpcDispatcher, sentinelAddSet.iterator().next(), methodCall, errMsg);

		if (stockToExchangeNameMap == null || stockMapBackup == null || exchangeNameToAddMap == null || exchangeAddToNameMap == null) {
			throw new RuntimeException(errMsg);
		}

		logger.info("Succeed to recover sentinel.");
	}

	public ConcurrentHashMap<String, String> getStockToExchangeNameMap() {
		return stockToExchangeNameMap;
	}

	public ConcurrentHashMap<String, ConcurrentHashMap<String, Stock>> getStockMapBackup() {
		return stockMapBackup;
	}

	public ConcurrentHashMap<String, ConcurrentHashMap<String, UserInfo>> getUserInfoMapBackup() {
		return userInfoMapBackup;
	}

	public ConcurrentHashMap<String, Address> getExchangeNameToAddMap() {
		return exchangeNameToAddMap;
	}

	public ConcurrentHashMap<Address, String> getExchangeAddToNameMap() {
		return exchangeAddToNameMap;
	}

	// for recover exchange
	public ConcurrentHashMap<String, Stock> getStockMap(String exchangeName) {
		if (stockMapBackup.containsKey(exchangeName)) {
			return stockMapBackup.get(exchangeName);
		}

		logger.error(exchangeName + " does not exists in stockMapBackup");
		return new ConcurrentHashMap<>();
	}

	// for recover exchange
	public ConcurrentHashMap<String, UserInfo> getUserInfoMap(String exchangeName) {
		if (userInfoMapBackup.containsKey(exchangeName)) {
			return userInfoMapBackup.get(exchangeName);
		}

		logger.error(exchangeName + " does not exists in userInfoMapBackup");
		return new ConcurrentHashMap<>();
	}

	// in the same continent, return the exchange add
/*
	public Address findStockLocally(String stockName) {
		if (stockToExchangeNameMap.containsKey(stockName)) {
			String exchangeName = stockToExchangeNameMap.get(stockName);
			if (exchangeNameToAddMap.containsKey(exchangeName)) {
				return exchangeNameToAddMap.get(exchangeName);
			}
		}

		return null;
	}
*/

	public Response findStockLocally(String stockName) {
		Response response = new Response(false, "edu.uchicago.cs.Stock is not in local continent.");

		if (stockToExchangeNameMap.containsKey(stockName)) {
			String exchangeName = stockToExchangeNameMap.get(stockName);
			if (exchangeNameToAddMap.containsKey(exchangeName)) {
				response.setSuccess(true);
				response.setMsg("edu.uchicago.cs.Stock is in local continent");
				response.setAssociatedExchangeAdd(exchangeNameToAddMap.get(exchangeName));
				return response;
			}
		}

		return response;
	}


	// in other continents, return the sentinel add (rpc call it later)
/*
	public Address findStockRemotely(String stockName) {
		MethodCall methodCall = new MethodCall("findStockLocally", new Object[]{stockName}, new Class[]{String.class});
		RequestOptions opts = new RequestOptions(ResponseMode.GET_ALL, 5 * 1000);
		RspList<Address> rsps = null;
		try {
			rsps = sentinelClusterRpcDispatcher.callRemoteMethods(null, methodCall, opts);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Fail to do RPC call findStockRemotely in the sentinel.");
			return null;
		}

		Iterator<Map.Entry<Address, Rsp<Address>>> resultIterator = rsps.entrySet().iterator();
		while (resultIterator.hasNext()) {
			Map.Entry<Address, Rsp<Address>> entry = resultIterator.next();
			Rsp<Address> rsp = entry.getValue();
			if (rsp.wasReceived() && !rsp.wasSuspected() && (rsp.getValue() != null)) {
				return entry.getKey();
			}
		}

		logger.error("Fail to receive a valid response from other sentinels.");
		return null;
	}
*/

	public Address findStockRemotely(String stockName) {

		MethodCall methodCall = new MethodCall("findStockLocally", new Object[]{stockName}, new Class[]{String.class});
		Address sentinelAdd = Utils.rpcSentinelsSingleSuccessful(sentinelClusterRpcDispatcher, null, methodCall, "Fail to do RPC call findStockRemotely in the sentinel.");

		if (sentinelAdd == null) {
			logger.error("Fail to receive a valid response from other sentinels.");
		}

		return sentinelAdd;
	}

	public Response transact(Request request) {
		String action = request.getAction();
		String stockName = request.getStockName();
		int numOfShares = request.getNumOfShares();

		MethodCall methodCall = new MethodCall("transact", new Object[]{request},
				new Class[]{Request.class});

		// in local continent
		Address exchangeAdd = findStockLocally(stockName).getAssociatedExchangeAdd();
//		Address exchangeAdd = findStockLocally(stockName);
		if (exchangeAdd != null) {
			String errMsg = "Fail to do RPC call transact in the local exchange in the sentinel.";
			return Utils.rpcTransact(exchangeClusterRpcDispatcher, exchangeAdd, methodCall, errMsg);
		}

		// in remote continent
		if (request.isRecursiveSentinel()) {
			request.setRecursiveSentinel(false);
			Address sentinelAdd = findStockRemotely(stockName);
			if (sentinelAdd != null) {
				String errMsg = "Fail to do RPC call transact in the remote exchange in the sentinel.";
				return Utils.rpcTransact(sentinelClusterRpcDispatcher, sentinelAdd, methodCall, errMsg);
			}
		}

		return new Response(false, request.getDateFormat().format(request.getDate()) + " No such stock - Reject: " + request.getAction() + " "
				+ request.getStockName() + " " + request.getNumOfShares(), request.getAssociatedCapital());
	}

	public Response updateStockMap(String exchangeName, Stock stock) {
		try {
			if (stockMapBackup.containsKey(exchangeName)) {
				ConcurrentHashMap<String, Stock> stockMap = stockMapBackup.get(exchangeName);
				stockMap.put(stock.getName(), stock);
				return new Response(true);
			}
		} catch (Exception e) {
			logger.error("Fail to updateStockMap in the sentinel.");
			return new Response(false);
		}

		return new Response(false);
	}

	public Response updateUserInfoMap(String exchangeName, UserInfo userInfo) {
		try {
			if (userInfoMapBackup.containsKey(exchangeName)) {
				ConcurrentHashMap<String, UserInfo> userInfoMap = userInfoMapBackup.get(exchangeName);
				userInfoMap.put(userInfo.getName(), userInfo);
				return new Response(true);
			}
		} catch (Exception e) {
			logger.error("Fail to updateUserInfoMap in the sentinel.");
			return new Response(false);
		}

		return new Response(false);
	}

	public void start() {
		Utils.findSentinels(sentinelClusterChannel, sentinelClusterChannel.getName(), sentinelAddSet);
		if (isRestart) {
			recover();
		}

		Runnable exchangeAdminThread = new SentinelAdminThread(this);
		pool.submit(exchangeAdminThread);
	}

	public ExecutorService getPool() {
		return pool;
	}

	public JChannel getExchangeClusterChannel() {
		return exchangeClusterChannel;
	}

	public RpcDispatcher getExchangeClusterRpcDispatcher() {
		return exchangeClusterRpcDispatcher;
	}

	public String getExchangeClusterName() {
		return exchangeClusterName;
	}

	public static void main(String[] args) throws Exception {
		String argErrMsg = "Wrong argument format. Example: --exchangeClusterName=north_america --isRestart=false";

		Map<String, String> argMap = Utils.parseCmdArgs(args, argErrMsg);

		if (!(argMap.size() == 2 && argMap.containsKey("--exchangeClusterName") && argMap.containsKey("--isRestart"))) {
			System.out.println(argErrMsg);
			return;
		}

		SentinelServer sentinelServer = new SentinelServer(argMap.get("--exchangeClusterName"), argMap.get("--isRestart").equals("true"));
		sentinelServer.start();
	}
}