package com.example.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.model.PaymentEvent;

class BenchmarkTest {

	Benchmark benchmark;
	
	@BeforeEach
	void setUp() throws Exception {
		benchmark = new Benchmark() {};
	}

	@Test
	void testNewActivityStream() {
		Stream<PaymentEvent> paymentEventStream1 = Benchmark.newPaymentStream(0, 100, 100);
		Stream<PaymentEvent> paymentEventStream2 = Benchmark.newPaymentStream(0, 100, 1000);
		Stream<PaymentEvent> paymentEventStream3 = Benchmark.newPaymentStream(0, 1000, 100);
		
		assertEquals(paymentEventStream1.collect(Collectors.toSet()).size(), 100);
		assertEquals(paymentEventStream2.collect(Collectors.toSet()).size(), 1000);
		assertEquals(paymentEventStream3.collect(Collectors.toSet()).size(), 100);
	}
	
	@Test
	void testNewReferenceStream() {
//		Stream<ReferenceEvent> referenceStream = Benchmark.newReferenceStream(0, 5, 0, 0);
//		HashSet<String> ips = new HashSet<>();
//		referenceStream.forEach((x) -> ips.add(x.origin));
//		assertEquals(5, ips.size());
	}
	
	@Test
	void testNewSourceStream() {
//		Stream<String> sourceStream = Benchmark.newSourceStream();
//		assertEquals(65534, sourceStream.count());
	}
}
