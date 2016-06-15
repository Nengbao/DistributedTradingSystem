package edu.uchicago.cs;

import org.jgroups.Address;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RpcDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Sub classes need to specify errorMsg in the constructor.
 * @param <T>
 */
public abstract class BaseSynThread<T> implements Runnable {
	protected ConcurrentLinkedDeque<T> queue = new ConcurrentLinkedDeque<>();
	protected RpcDispatcher rpcDispatcher = null;
	protected ConcurrentHashMap.KeySetView<Address, Boolean> sentinelAddSet = null;
	protected String exchangName = null;
	protected String errorMsg = null;

	protected static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public BaseSynThread(RpcDispatcher rpcDispatcher, ConcurrentHashMap.KeySetView<Address, Boolean> sentinelAddSet, String exchangName) {
		this.rpcDispatcher = rpcDispatcher;
		this.sentinelAddSet = sentinelAddSet;
		this.exchangName = exchangName;
	}

	public void addChangedElement(T element) {
		try {
			queue.add(element);	//@TODO serialization
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Fail to add a changed element to the to-be-syn queue.");
		}
	}

	private void syn() {
		int i = 0;
		while (true) {
			i++;
			while (!queue.isEmpty()) {
				i = 0;
				T element = queue.poll();

				MethodCall methodCall = getMethodCall(element);

				// syn with sentinel servers, rpc
				Response response = Utils.rpcFuncsAllSuccessful(rpcDispatcher, sentinelAddSet, methodCall, errorMsg);

				// in case of failure, re-syn
				if (!response.isSuccess()) {
					logger.error(errorMsg);
					queue.offerFirst(element);
				}
			}

			// less spinning in case queue is empty for a long time
			try {
				if (i > 10) {
					i = 11;		// avoid overflow in case that queue is empty for a long time
					Thread.sleep(1000);
				}
				else {
					Thread.sleep(100);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	protected abstract MethodCall getMethodCall(T element);

	/**
	 * When an object implementing interface <code>Runnable</code> is used
	 * to create a thread, starting the thread causes the object's
	 * <code>run</code> method to be called in that separately executing
	 * thread.
	 * <p/>
	 * The general contract of the method <code>run</code> is that it may
	 * take any action whatsoever.
	 *
	 * @see Thread#run()
	 */
	@Override

	public void run() {
		syn();
	}
}
