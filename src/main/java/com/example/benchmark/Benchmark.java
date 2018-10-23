package com.example.benchmark;

import java.util.ArrayList;
import java.util.stream.Stream;

import com.example.model.PaymentEvent;
import com.example.model.RequestQuotaEvent;

import jsr166y.ThreadLocalRandom;

public abstract class Benchmark {

	private static final int MIN_AMT = 10;
	private static final int MAX_AMT = 200;
	private static final int MIN_UNITS = 10;
	private static final int MAX_UNITS = 100;
	private static final int MIN_PRODID = 1;
	
	public static Stream<PaymentEvent> newPaymentStream(
			int offset, int userCount, int eventCount) {
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		ArrayList<PaymentEvent> events = new ArrayList<>(eventCount);
		
		int maxEventsPerUser = (eventCount < userCount ? 1 : eventCount/userCount);
		int runningEventCount = 0;

		while(runningEventCount < eventCount) {
			for (int j=0; j<maxEventsPerUser; j++) {
				events.add(new PaymentEvent(
						rand.nextInt(offset, offset+userCount), 
						rand.nextLong(MIN_AMT, MAX_AMT)));
				runningEventCount++;
			}
		}
		return events.stream();
	}
	
	public static Stream<RequestQuotaEvent> newRequestQuotaEvent(
			int offset, int userCount, int productCount, int eventCount) {
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		ArrayList<RequestQuotaEvent> events = new ArrayList<>(eventCount);
		
		int maxEventsPerUser = (eventCount < userCount ? 1 : eventCount/userCount);
		int runningEventCount = 0;
		
		while(runningEventCount < eventCount) {
			for (int j=0; j<maxEventsPerUser; j++) {
				events.add(new RequestQuotaEvent(
						rand.nextInt(offset, offset+userCount), 
						rand.nextLong(MIN_PRODID, productCount), 
						rand.nextInt(MIN_UNITS, MAX_UNITS)));
				runningEventCount++;
			}
		}
		return events.stream();
	}
}
