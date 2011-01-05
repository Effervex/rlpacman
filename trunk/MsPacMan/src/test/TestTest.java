package test;

import static org.junit.Assert.*;

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
}
