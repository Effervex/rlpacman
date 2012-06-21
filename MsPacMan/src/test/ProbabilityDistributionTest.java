package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
	public void testMinimumBaggingSample() {
		sut_.clear();
		// Build a uniform set of samples
		int size = 25;
		for (int i = 0; i < size; i++) {
			char name = (char) ('a' + i);
			sut_.add(name + "");
		}
		sut_.normaliseProbs();

		// Test sampling probability
		// Only sampling N
		testRepetitions(1, size);
		// Sampling N * 2
		testRepetitions(2, size);
		// Sampling N * 3
		testRepetitions(3, size);
		// Sampling N * 4
		testRepetitions(4, size);
		// Sampling N * 5
		testRepetitions(5, size);
	}

	/**
	 * Tests repetitive sampling from the probability distribution with a given
	 * amount of reps.
	 * 
	 * @param numReps
	 *            The number of repretitions of sampling.
	 * @param size
	 *            The size of the set being sampled
	 */
	private void testRepetitions(double numReps, int size) {
		int totalSize = 0;
		int crossValidation = 20;
		for (int i = 0; i < crossValidation; i++) {
			Set<String> sampled = new HashSet<String>();
			for (int j = 0; j < numReps * size; j++)
				sampled.add(sut_.sample(false));

			int sampledSize = sampled.size();
			totalSize += sampledSize;
		}
		double totalProb = 1.0 * totalSize / (size * crossValidation);
		System.out.println(numReps + " samples: " + totalSize + "/" + size
				* crossValidation);
		System.out.println("  Total prob: " + totalProb);
		assertEquals(1 - Math.exp(-1 * numReps), totalProb, 1.0 / size);
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
	public void testUpdateDistribution() {
		for (String val : sut_)
			sut_.set(val, 0.2);
		
		Map<String, Integer> counts = new HashMap<String, Integer>();
		counts.put("a", 2);
		counts.put("b", 1);
		counts.put("c", 3);
		counts.put("d", 3);
		counts.put("e", 1);
		sut_.updateDistribution(10, counts, 0.6);
		assertEquals(0.2, sut_.getProb("a"), 0.0001);
		assertEquals(0.14, sut_.getProb("b"), 0.0001);
		assertEquals(0.26, sut_.getProb("c"), 0.0001);
		assertEquals(0.26, sut_.getProb("d"), 0.0001);
		assertEquals(0.14, sut_.getProb("e"), 0.0001);

		// Normalisation
		for (String val : sut_)
			sut_.set(val, 0.2);
		counts.put("c", 7);
		sut_.updateDistribution(14, counts, 0.6);
		assertEquals(0.1657, sut_.getProb("a"), 0.0001);
		assertEquals(0.1229, sut_.getProb("b"), 0.0001);
		assertEquals(0.38, sut_.getProb("c"), 0.0001);
		assertEquals(0.2086, sut_.getProb("d"), 0.0001);
		assertEquals(0.1229, sut_.getProb("e"), 0.0001);
	}

	@Test
	public void testKlSize() {
		assertEquals(sut_.klSize(), 4.44429164, 0.001);

		sut_.set("d", 0.7);
		sut_.normaliseProbs();
		assertEquals(sut_.klSize(), 3.95338898, 0.001);
	}
	
	@Test
	public void testNormalisation() {
		sut_.normaliseProbs();
		assertEquals(0.1, sut_.getProb("a"), 0.0001);
		assertEquals(0.2, sut_.getProb("b"), 0.0001);
		assertEquals(0.3, sut_.getProb("c"), 0.0001);
		assertEquals(0.35, sut_.getProb("d"), 0.0001);
		assertEquals(0.05, sut_.getProb("e"), 0.0001);
		
		sut_.add("f", 0.2);
		sut_.add("g", 0.2);
		sut_.add("h", 0.2);
		sut_.normaliseProbs();
		
		assertEquals(0.0625, sut_.getProb("a"), 0.0001);
		assertEquals(0.125, sut_.getProb("b"), 0.0001);
		assertEquals(0.1875, sut_.getProb("c"), 0.0001);
		assertEquals(0.21875, sut_.getProb("d"), 0.0001);
		assertEquals(0.03125, sut_.getProb("e"), 0.0001);
		assertEquals(0.125, sut_.getProb("f"), 0.0001);
		assertEquals(0.125, sut_.getProb("g"), 0.0001);
		assertEquals(0.125, sut_.getProb("h"), 0.0001);
	}
}
