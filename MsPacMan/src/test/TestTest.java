package test;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import jess.Fact;
import jess.Rete;

import org.apache.commons.math.distribution.PoissonDistribution;
import org.apache.commons.math.distribution.PoissonDistributionImpl;
import org.junit.Test;

import relationalFramework.StateSpec;
import relationalFramework.util.ArgumentComparator;

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
		double delta = 0.03;
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
		delta = 0.03;
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

	public void testDistributions() throws Exception {
		double[] poisson = new double[1000];
		double[] normal05 = new double[1000];
		double[] normal025 = new double[1000];
		double[] normal001 = new double[1000];
		double[] maxVals = new double[4];

		PoissonDistribution poissonD = new PoissonDistributionImpl(1);
		Random random = new Random();
		for (int i = 0; i < 10000; i++) {
			double result = poissonD.inverseCumulativeProbability(random
					.nextDouble()) + 1;
			if (result >= 0 && result < 10) {
				Arrays.fill(poisson, (int) (result * 100),
						(int) (result * 100 + 100),
						poisson[(int) (result * 100)] + 1);
				maxVals[0] = Math
						.max(maxVals[0], poisson[(int) (result * 100)]);
			}
			result = 1 + random.nextGaussian() * 0.5;
			if (result >= 0 && result < 10) {
				normal05[(int) (result * 100)]++;
				maxVals[1] = Math.max(maxVals[1],
						normal05[(int) (result * 100)]);
			}
			result = 1 + random.nextGaussian() * 0.25;
			if (result >= 0 && result < 10) {
				normal025[(int) (result * 100)]++;
				maxVals[2] = Math.max(maxVals[2],
						normal025[(int) (result * 100)]);
			}
			result = 1 + random.nextGaussian() * 0.01;
			if (result >= 0 && result < 10) {
				normal001[(int) (result * 100)]++;
				maxVals[3] = Math.max(maxVals[3],
						normal001[(int) (result * 100)]);
			}
		}

		File out = new File("distributions.txt");
		out.createNewFile();
		BufferedWriter write = new BufferedWriter(new FileWriter(out));
		for (int i = 0; i < 1000; i++)
			write.write("" + i + '\t' + poisson[i] / maxVals[0] + '\t'
					+ normal05[i] / maxVals[1] + '\t' + normal025[i]
					/ maxVals[2] + '\t' + normal001[i] / maxVals[3] + '\n');

		write.close();
	}

	@Test
	public void testBitOperators() {
		assertTrue((2 & 1) == 0);
		assertTrue((2 & 2) != 0);
		assertTrue((3 & 1) != 0);
		assertTrue((2 & 2) != 0);
		assertTrue((4 & 4) != 0);
		assertTrue((4 & 2) == 0);
		assertTrue((4 & 1) == 0);
	}

	@Test
	public void testSortedSetArrayContains() {
		SortedSet<String[]> arraySet = new TreeSet<String[]>(ArgumentComparator
				.getInstance());
		assertTrue(arraySet.add(new String[] { "a", "b", "c" }));
		assertTrue(arraySet.contains(new String[] { "a", "b", "c" }));
		assertFalse(arraySet.contains(new String[] { "a", "b", "d" }));
		assertFalse(arraySet.contains(new String[] { "a", "c", "b" }));

		assertTrue(arraySet.add(new String[] { "a", "b", "d" }));
		assertTrue(arraySet.contains(new String[] { "a", "b", "c" }));
		assertTrue(arraySet.contains(new String[] { "a", "b", "d" }));
		assertFalse(arraySet.contains(new String[] { "a", "c", "b" }));

		assertFalse(arraySet.add(new String[] { "a", "b", "d" }));
	}

	@Test
	public void testEqualStateActions() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
		Rete state = StateSpec.getInstance().getRete();
		state.reset();
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (on a d))");
		state.eval("(assert (on d c))");
		state.eval("(assert (on b e))");
		state.eval("(assert (onFloor e))");
		state.eval("(assert (onFloor c))");
		state.eval("(assert (highest a))");
		state.run();
		StateSpec.getInstance().generateValidActions(state);
		Collection<Fact> facts = StateSpec.extractFacts(state);

		assertEquals(facts, StateSpec.extractFacts(state));
		
		state.reset();
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (on a d))");
		state.eval("(assert (on d c))");
		state.eval("(assert (on b e))");
		state.eval("(assert (onFloor e))");
		state.eval("(assert (onFloor c))");
		state.eval("(assert (highest a))");
		state.run();
		StateSpec.getInstance().generateValidActions(state);
		Collection<Fact> sameFacts = StateSpec.extractFacts(state);
		assertEquals(facts, sameFacts);
		
		ArrayList<String> abc = new ArrayList<String>();
		abc.add("a");
		abc.add("b");
		abc.add("c");
		
		ArrayList<String> sameABC = new ArrayList<String>();
		sameABC.add("a");
		sameABC.add("b");
		sameABC.add("c");
		assertEquals(abc, sameABC);
	}
}
