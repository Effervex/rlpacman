package test;

import static org.junit.Assert.*;

import java.util.Random;

import org.apache.commons.math.distribution.PoissonDistribution;
import org.apache.commons.math.distribution.PoissonDistributionImpl;
import org.junit.Test;

public class TestTest {
	@Test
	public void testStuff() throws Exception {
		PoissonDistribution p = new PoissonDistributionImpl(1);
		assertEquals(p.inverseCumulativeProbability(0) + 1, 0);
		assertEquals(p.inverseCumulativeProbability(0.5) + 1, 1);
		assertEquals(p.inverseCumulativeProbability(0.75) + 1, 2);
	}

	@Test
	public void testPoisson() throws Exception {
		// Poisson dist with t = 1
		PoissonDistribution p = new PoissonDistributionImpl(1);
		Random random = new Random();
		int[] counts = new int[500];
		for (int i = 0; i < 10000; i++) {
			counts[p.inverseCumulativeProbability(random.nextDouble()) + 1]++;
		}
		double delta = 0.01;
		assertEquals(0.367879, counts[0] / 10000d, delta);
		assertEquals(0.367879, counts[1] / 10000d, delta);
		assertEquals(0.183940, counts[2] / 10000d, delta);
		assertEquals(0.061313, counts[3] / 10000d, delta);
		assertEquals(0.015328, counts[4] / 10000d, delta);
		assertEquals(0, p.inverseCumulativeProbability(0.0) + 1, 0);
		assertEquals(0, p.inverseCumulativeProbability(0.1) + 1, 0);
		assertEquals(0, p.inverseCumulativeProbability(0.2) + 1, 0);
		assertEquals(0, p.inverseCumulativeProbability(0.3) + 1, 0);
		assertEquals(1, p.inverseCumulativeProbability(0.4) + 1, 0);
		assertEquals(1, p.inverseCumulativeProbability(0.5) + 1, 0);
		assertEquals(1, p.inverseCumulativeProbability(0.6) + 1, 0);
		assertEquals(1, p.inverseCumulativeProbability(0.7) + 1, 0);
		assertEquals(2, p.inverseCumulativeProbability(0.8) + 1, 0);
		assertEquals(2, p.inverseCumulativeProbability(0.9) + 1, 0);
		assertEquals(3, p.inverseCumulativeProbability(0.95) + 1, 0);
		assertEquals(3, p.inverseCumulativeProbability(0.975) + 1, 0);
		assertEquals(4, p.inverseCumulativeProbability(0.99) + 1, 0);
		
		// Poisson dist with t = 0.86
		p = new PoissonDistributionImpl(0.86);
		counts = new int[500];
		for (int i = 0; i < 10000; i++) {
			counts[p.inverseCumulativeProbability(random.nextDouble()) + 1]++;
		}
		delta = 0.01;
		assertEquals(0.423162, counts[0] / 10000d, delta);
		assertEquals(0.363919, counts[1] / 10000d, delta);
		assertEquals(0.156485, counts[2] / 10000d, delta);
		assertEquals(0.044819, counts[3] / 10000d, delta);
		assertEquals(0.009645, counts[4] / 10000d, delta);
		assertEquals(0, p.inverseCumulativeProbability(0.0) + 1, 0);
		assertEquals(0, p.inverseCumulativeProbability(0.1) + 1, 0);
		assertEquals(0, p.inverseCumulativeProbability(0.2) + 1, 0);
		assertEquals(0, p.inverseCumulativeProbability(0.3) + 1, 0);
		assertEquals(0, p.inverseCumulativeProbability(0.4) + 1, 0);
		assertEquals(1, p.inverseCumulativeProbability(0.5) + 1, 0);
		assertEquals(1, p.inverseCumulativeProbability(0.6) + 1, 0);
		assertEquals(1, p.inverseCumulativeProbability(0.7) + 1, 0);
		assertEquals(2, p.inverseCumulativeProbability(0.8) + 1, 0);
		assertEquals(2, p.inverseCumulativeProbability(0.9) + 1, 0);
		assertEquals(3, p.inverseCumulativeProbability(0.95) + 1, 0);
		assertEquals(3, p.inverseCumulativeProbability(0.975) + 1, 0);
		assertEquals(4, p.inverseCumulativeProbability(0.99) + 1, 0);
	}
}
