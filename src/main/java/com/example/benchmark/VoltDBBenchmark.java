package com.example.benchmark;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientStats;
import org.voltdb.client.ClientStatsContext;
import org.voltdb.client.ProcCallException;

import com.example.db.VoltDB;

public class VoltDBBenchmark extends Benchmark {
	
	private static final int RUN_DURATION = 600;
	private static final int STATS_START = 180;
	private static final int STATS_DELAY = 5;
	private static Client statsClient;
	private static final int DEFAULT_BATCH_SIZE = 10000;

	public static void main(String[] args) throws Exception {
		String server = args[0];
		int useridOffset = Integer.parseInt(args[1]);
		int users = Integer.parseInt(args[2]);
		int productCount = Integer.parseInt(args[3]);
		int reqsPerSecond = Integer.parseInt(args[4]);
		int clientId = Integer.parseInt(args[5]);
		int threadPoolCount = Integer.parseInt(args[6]);
		String run = args[7];

		statsClient = ClientFactory.createClient();
		try {
			statsClient.createConnection("nat-gateway");
		} catch (IOException e) {
			e.printStackTrace();
		}

		ClientConfig config = new ClientConfig();
		config.setMaxTransactionsPerSecond(reqsPerSecond);
		config.setTopologyChangeAware(true);
		
		VoltDB voltdb = new VoltDB(server, config, DEFAULT_BATCH_SIZE);
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(threadPoolCount);
		start(voltdb, executor, useridOffset, users, productCount, reqsPerSecond, clientId, run);
	}

	public static void start(
			VoltDB voltdb, ScheduledExecutorService executor, int useridOffset, 
			int userCount, int productCount, int countPerSecond, int clientId, String run) {

		System.out.println("Running VoltDB Benchmark");
		ClientStatsContext statsContext = voltdb.client.createStatsContext();
		ClientStatsContext fullStatsContext = voltdb.client.createStatsContext();
		
		executor.scheduleWithFixedDelay(
				() ->
				newPaymentStream(useridOffset, userCount, countPerSecond)
				.forEach((payment) -> voltdb.upsertUser.accept(payment))
				,0, 1000, TimeUnit.MILLISECONDS);
		
		executor.scheduleWithFixedDelay(
				() ->
				newRequestQuotaEvent(useridOffset, userCount, productCount, countPerSecond)
				.forEach((quotaRequest) -> voltdb.requestQuota.accept(quotaRequest))
				,500, 1000, TimeUnit.MILLISECONDS);
		
		executor.scheduleWithFixedDelay(
				() -> {
					printPeriodicStats(statsContext.fetchAndResetBaseline().getStats(), run, clientId);
				}, STATS_START, STATS_DELAY, TimeUnit.SECONDS);
		
		executor.schedule(
				() -> {
					printFullStats(fullStatsContext.fetchAndResetBaseline().getStats(), run, clientId);
					executor.shutdown();
				}, STATS_START+RUN_DURATION, TimeUnit.SECONDS);
	}

	private static void printPeriodicStats(ClientStats stats, String run, int clientId) {
		long time = Math.round((stats.getEndTimestamp() - stats.getStartTimestamp()) / 1000.0);
		
		System.out.printf("%02d:%02d:%02d ", time / 3600, (time / 60) % 60, time % 60);
		System.out.printf("Throughput %d/s, ", stats.getTxnThroughput());
		System.out.printf("Aborts/Failures %d/%d",
				stats.getInvocationAborts(), stats.getInvocationErrors());
		System.out.printf(", Avg/95%%/99%% Latency %.2f/%.2f/%.2fms ", 
				stats.getAverageLatency(),
				stats.kPercentileLatencyAsDouble(0.95),
				stats.kPercentileLatencyAsDouble(0.99));
		System.out.printf("\n");
		try {
			Date date = new Date();
			statsClient.callProcedure("STATS.insert", 
					run, 
					clientId,
					date.toString(),
					stats.getTxnThroughput(), 
					stats.getInvocationAborts(), 
					stats.getInvocationErrors(), 
					stats.kPercentileLatencyAsDouble(0.99),
					stats.kPercentileLatencyAsDouble(0.999));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ProcCallException e) {
			e.printStackTrace();
		}
	}
	
	private static void printFullStats(ClientStats stats, String run, int clientId) {
		System.out.println(" FULL --------------------------");
		System.out.println(stats.latencyHistoReport());
		try {
			Date date = new Date();
			statsClient.callProcedure("COMPLETE_STATS.insert", 
					run, 
					clientId,
					date.toString(),
					stats.getTxnThroughput(), 
					stats.getInvocationAborts(), 
					stats.getInvocationErrors(), 
					stats.kPercentileLatencyAsDouble(0.99),
					stats.kPercentileLatencyAsDouble(0.999));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ProcCallException e) {
			e.printStackTrace();
		}
	}
}
