package com.example.benchmark;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InitDataLoaderTest {

	@BeforeEach
	void setUp() throws Exception {
		
	}

	@Test
	void test() {
		assertEquals(InitDataLoader.randomString().length(), 8000);
		assertNotEquals(InitDataLoader.randomString(), InitDataLoader.randomString());
	}
}
