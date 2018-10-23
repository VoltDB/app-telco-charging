package com.example.db;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcedureCallback;
import org.voltdb.client.VoltBulkLoader.BulkLoaderFailureCallBack;
import org.voltdb.client.VoltBulkLoader.VoltBulkLoader;

import com.example.model.Balance;
import com.example.model.PaymentEvent;
import com.example.model.Product;
import com.example.model.RequestQuotaEvent;

public class VoltDB {

	public final Client client;
	public final Client userBulkClient;
	public final Client usageBulkClient;
	public final VoltBulkLoader userLoader;
	public final VoltBulkLoader usageLoader;
	public final Consumer<PaymentEvent> upsertUser;
	public final Consumer<RequestQuotaEvent> requestQuota;
	public final Consumer<Product> addProduct;
	public final Consumer<Balance> addBalance;

	public VoltDB(String servers, ClientConfig config, int batchSize) throws Exception {

		client = ClientFactory.createClient(config);
		userBulkClient = ClientFactory.createClient();
		usageBulkClient = ClientFactory.createClient();

		connect(servers, client);
		connect(servers, userBulkClient);
		connect(servers, usageBulkClient);

		userLoader = userBulkClient.getNewBulkLoader("user", batchSize, new BulkCallback());
		usageLoader = usageBulkClient.getNewBulkLoader("usage", batchSize, new BulkCallback());

		upsertUser = (event) -> {
			try {
				client.callProcedure(new Callback(), "UpsertUser", event.userid, event.amount, "", "", "");
			} catch (IOException e) {
				e.printStackTrace();
			}
		};

		requestQuota = (event) -> {
			try {
				client.callProcedure(new Callback(), "RequestQuota", event.userid, event.productid, event.unitsWanted);
			} catch (IOException e) {
				e.printStackTrace();
			}
		};

		addProduct = (product) -> {
			try {
				client.callProcedure(new Callback(), "PRODUCT.insert", product.id, product.cost);
			} catch (IOException e) {
				e.printStackTrace();
			}
		};

		addBalance = (balance) -> {
			try {
				client.callProcedure(new Callback(), "BALANCE.insert", balance.userid, balance.balance);
			} catch (IOException e) {
				e.printStackTrace();
			}
		};
	}

	public void drain() throws Exception {
		userLoader.drain();
		usageLoader.drain();
		userLoader.close();
		usageLoader.close();
		userBulkClient.close();
		usageBulkClient.close();
		client.drain();
	}

	private class BulkCallback implements BulkLoaderFailureCallBack {
		public void failureCallback(Object rowHandle, Object[] fieldList, ClientResponse cr) {
			if (cr.getStatus() != ClientResponse.SUCCESS) {
				System.err.println(cr.getStatusString());
			}
		}
	}

	private class Callback implements ProcedureCallback {

		@Override
		public void clientCallback(ClientResponse arg0) throws Exception {
			if(arg0.getStatus() != ClientResponse.SUCCESS) {
				System.out.println(arg0.getStatusString());
			}
		}
	}

	private void connect(String servers, Client client) throws InterruptedException {
		System.out.println("Connecting to VoltDB at ..." + servers);

		String[] serverArray = servers.split(",");
		final CountDownLatch connections = new CountDownLatch(serverArray.length);

		// use a new thread to connect to each server
		for (final String server : serverArray) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					connectToOneServerWithRetry(server, client);
					connections.countDown();
				}
			}).start();
		}
		// block until all have connected
		connections.await();
	}

	private void connectToOneServerWithRetry(String server, Client client) {
		int sleep = 1000;
		while (true) {
			try {
				client.createConnection(server);
				break;
			}
			catch (Exception e) {
				System.err.printf("Connection failed - retrying in %d second(s).\n", sleep / 1000);
				try { Thread.sleep(sleep); } catch (Exception interruted) {}
				if (sleep < 8000) sleep += sleep;
			}
		}
		System.out.printf("Connected to VoltDB node at: %s.\n", server);
	}
}
