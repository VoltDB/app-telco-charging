/**
 * 
 */
package com.example.benchmark;

import java.util.Random;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomStringUtils;
import org.voltdb.client.ClientConfig;

import com.example.db.VoltDB;
import com.example.model.Balance;
import com.example.model.Product;

import jsr166y.ThreadLocalRandom;

/**
 * @author seetasomagani
 *
 */
public class InitDataLoader {
	
	private static final int RANDOM_STR_LEN = 8000;

	public static void main(String args[]) throws Exception {
		String server = args[0];
		int useridOffset = Integer.parseInt(args[1]);
		int users = Integer.parseInt(args[2]);
		int initProdCount = Integer.parseInt(args[3]);
		int totalProdCount = Integer.parseInt(args[4]);
		int batchSize = Integer.parseInt(args[5]);
		
		System.out.println("useridOffset: " + useridOffset + " users: " + users + " batchSize: " + batchSize);
		
		ClientConfig config = new ClientConfig();
		
		VoltDB voltdb = new VoltDB(server, config, batchSize);
		start(voltdb, useridOffset, users, initProdCount, totalProdCount, batchSize);
		voltdb.drain();
	}
	
	public static void start(
			VoltDB voltdb,
			int useridOffset, 
			int userCount, int initProdCount, int totalProdCount, int batchSize) {
		int noOfBatches = userCount/batchSize;
		System.out.println("noOfBatches = " + noOfBatches);
		
		IntStream.range(0, noOfBatches).forEach(
				(i) -> {
					int startUserId = useridOffset + i*batchSize;
					int endUserId = startUserId + batchSize;
					runBatch(voltdb, startUserId, endUserId, initProdCount);
				}
				);

		Random rand = ThreadLocalRandom.current();
		for (int i=0; i<totalProdCount; i++) {
			voltdb.addProduct.accept(new Product(i, rand.nextInt(20)));
		}
		
		for(int i=useridOffset; i<useridOffset+userCount; i++) {
			voltdb.addBalance.accept(new Balance(i, 200+rand.nextInt(1000)));
		}
	}
	
	public static void runBatch(VoltDB voltDB,int startUserId, int endUserId, int initProdCount) {
		System.out.println("Running batch from " + startUserId + " to " + endUserId);
		IntStream.range(startUserId, endUserId).forEach(
				(userId) -> {
					try {
						voltDB.userLoader.insertRow(userId, userId, randomString());
						for(int productId=0; productId<initProdCount; productId++)
							voltDB.usageLoader.insertRow(userId, userId, productId, 10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				);
	}
	
	public static String randomString() {
	    return RandomStringUtils.random(RANDOM_STR_LEN, Boolean.TRUE, Boolean.TRUE);
	}
}
