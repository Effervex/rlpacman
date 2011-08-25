package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import util.ProbabilityDistribution;

public class ProbabilityDistributionTest {
	private ProbabilityDistribution<String> sut_;

	@Before
	public void setUp() {
		resetSUT();
	}

	private void resetSUT() {
		sut_ = new ProbabilityDistribution<String>();
		sut_.add("a", 0.1);
		sut_.add("b", 0.2);
		sut_.add("c", 0.3);
		sut_.add("d", 0.35);
		sut_.add("e", 0.05);
	}

	@Test
	public void testSample() {
		int aCount = 0;
		int dCount = 0;
		for (int i = 0; i < 1000; i++) {
			String sample = sut_.sample(false);
			if (sample.equals("a"))
				aCount++;
			else if (sample.equals("d"))
				dCount++;
			assertEquals(sut_.sample(true), "d");
		}

		assertEquals(100, aCount, 25);
		assertEquals(350, dCount, 25);
	}

	@Test
	public void testSampleWithRemoval() {
		assertEquals(sut_.size(), 5);
		assertEquals(sut_.sampleWithRemoval(true), "d");
		assertEquals(sut_.size(), 4);
		assertFalse(sut_.contains("d"));
		String sample = sut_.sampleWithRemoval(false);
		assertEquals(sut_.size(), 3);
		assertFalse(sut_.contains(sample));
	}

	@Test
	public void testGetOrderedElements() {
		ArrayList<String> expected = new ArrayList<String>();
		expected.add("d");
		expected.add("c");
		expected.add("b");
		expected.add("a");
		expected.add("e");
		assertEquals(expected, sut_.getOrderedElements());
	}

	@Test
	public void testUpdateDistributionDoubleMapOfTDoubleDouble() {
		Map<String, Double> counts = new HashMap<String, Double>();
		counts.put("a", 2d);
		counts.put("b", 1d);
		counts.put("c", 3.5);
		counts.put("d", 3.5);
		counts.put("e", 0d);
		sut_.updateDistribution(10, counts, 0.6);
		assertEquals(0.16, sut_.getProb("a"), 0.0001);
		assertEquals(0.14, sut_.getProb("b"), 0.0001);
		assertEquals(0.33, sut_.getProb("c"), 0.0001);
		assertEquals(0.35, sut_.getProb("d"), 0.0001);
		assertEquals(0.02, sut_.getProb("e"), 0.0001);
		
		// Normalisation
		resetSUT();
		counts.put("c", 7d);
		sut_.updateDistribution(10, counts, 0.6);
		assertEquals(0.16 / 1.21, sut_.getProb("a"), 0.0001);
		assertEquals(0.14 / 1.21, sut_.getProb("b"), 0.0001);
		assertEquals(0.54 / 1.21, sut_.getProb("c"), 0.0001);
		assertEquals(0.35 / 1.21, sut_.getProb("d"), 0.0001);
		assertEquals(0.02 / 1.21, sut_.getProb("e"), 0.0001);
	}

	@Test
	public void testKlSize() {
		assertEquals(sut_.klSize(), 4.44429164, 0.001);
		
		sut_.set("d", 0.7);
		sut_.normaliseProbs();
		assertEquals(sut_.klSize(), 3.95338898, 0.001);
	}
}
