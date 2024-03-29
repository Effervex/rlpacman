/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/test/TestTest.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package test;

import static org.junit.Assert.*;

import relationalFramework.StateSpec;

import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import jess.Fact;
import jess.JessException;
import jess.Rete;

import org.apache.commons.math.distribution.PoissonDistribution;
import org.apache.commons.math.distribution.PoissonDistributionImpl;
import org.junit.Test;

import blocksWorld.BlocksWorldEnvironment;

import util.ArgumentComparator;
import util.ProbabilityDistribution;

public class TestTest {
	@Test
	public void testStuff() throws Exception {
		PoissonDistribution p = new PoissonDistributionImpl(1);
		assertEquals(p.inverseCumulativeProbability(0) + 1, 0);
		assertEquals(p.inverseCumulativeProbability(0.5) + 1, 1);
		assertEquals(p.inverseCumulativeProbability(0.75) + 1, 2);

		System.out.println("processors: "
				+ Runtime.getRuntime().availableProcessors());
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
	public void testSortedSetArrayContains() {
		SortedSet<String[]> arraySet = new TreeSet<String[]>(
				ArgumentComparator.getInstance());
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

	@Test
	public void testAssertionSpeeds() throws Exception {
		Rete rete = new Rete();

		long start = System.currentTimeMillis();
		StringBuffer assertion = new StringBuffer();
		for (int i = 0; i < 500; i++) {
			assertion.append("(bflock d" + i + ") ");
		}
		rete.assertString(assertion.toString());
		long end = System.currentTimeMillis();
		System.out.println(end - start);
		start = end;

		assertion = new StringBuffer();
		for (int i = 0; i < 500; i++) {
			assertion.append("(belock e" + i + ") ");
		}
		rete.assertString(assertion.toString());
		end = System.currentTimeMillis();
		System.out.println(end - start);
		start = end;

		assertion = new StringBuffer();
		for (int i = 0; i < 500; i++) {
			assertion.append("(bdlock f" + i + ") ");
		}
		rete.assertString(assertion.toString());
		end = System.currentTimeMillis();
		System.out.println(end - start);
		start = end;

		System.out.println("-------");

		for (int i = 0; i < 500; i++) {
			String block = "(bclock a" + i + ")";
			rete.assertString(block);
		}
		end = System.currentTimeMillis();
		System.out.println(end - start);
		start = end;

		for (int i = 0; i < 500; i++) {
			String block = "(bblock b" + i + ")";
			rete.assertString(block);
		}
		end = System.currentTimeMillis();
		System.out.println(end - start);
		start = end;

		for (int i = 0; i < 500; i++) {
			String block = "(balock c" + i + ")";
			rete.assertString(block);
		}
		end = System.currentTimeMillis();
		System.out.println(end - start);
		start = end;
	}

	@Test
	public void testUnorderedSpeed() throws JessException {
		Rete rete = new Rete();
		rete.eval("(deftemplate on (slot arg0) (slot arg1))");
		Random rand = new Random(0);
		long fullStartTime = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			char a = (char) ('a' + rand.nextInt(26));
			char b = (char) ('a' + rand.nextInt(26));
			String fact = "(on (arg0 " + a + ") (arg1 " + b + "))";
			rete.assertString(fact);
		}
		rete.eval("(defrule findOnA (on (arg0 a) (arg1 ?X)) (on (arg0 ?X) (arg1 z)) => "
				+ "(printout t \"It has been found!\" crlf))");
		rete.eval("(defrule findNoA (on (arg0 ?X) (arg1 a)) (on (arg0 ?X) (arg1 ?Y)) (on (arg0 ?Y) (arg1 z)) => "
				+ "(printout t \"It has also been found!\" crlf))");
		long startTime = System.currentTimeMillis();
		rete.run();
		long endTime = System.currentTimeMillis();
		System.out.println("Rule: " + (endTime - startTime));
		System.out.println("Assert+Rule: " + (endTime - fullStartTime));
	}

	@Test
	public void testOrderedSpeed() throws JessException {
		Rete rete = new Rete();
		rete.eval("(deftemplate on (declare (ordered TRUE)))");
		Random rand = new Random(0);
		long fullStartTime = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			char a = (char) ('a' + rand.nextInt(26));
			char b = (char) ('a' + rand.nextInt(26));
			String fact = "(on " + a + " " + b + ")";
			rete.assertString(fact);
		}
		rete.eval("(defrule findOnA (on a ?X) (on ?X z) => "
				+ "(printout t \"It has been found!\" crlf))");
		rete.eval("(defrule findNoA (on ?X a) (on ?X ?Y) (on ?Y z) => "
				+ "(printout t \"It has also been found!\" crlf))");
		long startTime = System.currentTimeMillis();
		rete.run();
		long endTime = System.currentTimeMillis();
		System.out.println("Rule: " + (endTime - startTime));
		System.out.println("Assert+Rule: " + (endTime - fullStartTime));
	}

	@Test
	public void testClass() {
		BlocksWorldEnvironment bwe = new BlocksWorldEnvironment();
		assertEquals("blocksWorldMove.BlocksWorldEnvironment", bwe.getClass()
				.getName());
	}

	@Test
	public void testEmpiricalBaggingSampling() {
		ProbabilityDistribution<String> pDist = new ProbabilityDistribution<String>(
				new Random(10));
		// Load up the distribution
		int numSamples = 10000;
		double prob = 1.0 / numSamples;
		for (int i = 0; i < numSamples; i++)
			pDist.add("El" + i, prob);

		// Initialise the empirical test sets
		int numSets = 10;
		Set<String>[] sampledSets = new Set[numSets];
		for (int i = 0; i < numSets; i++)
			sampledSets[i] = new HashSet<String>(numSamples);


		for (int numPasses = 1; numPasses < 13; numPasses++) {
			// Performing 3 passes
			System.out.println(numPasses + " PASSES");

			// Sample the distribution numSamples times
			for (int i = 0; i < numSamples; i++) {
				String sample = pDist.sample(false);
				int setNum = 0;
				boolean contains;
				do {
					contains = sampledSets[setNum].add(sample);
					setNum++;
				} while (!contains && setNum < numSets);
			}

			// Outputting the probabilities
			for (int i = 0; i < numSets; i++) {
				double duplicateProb = (1.0 * sampledSets[i].size() / numSamples);
				System.out.println("Num Duplicates = " + (i + 1) + ": "
						+ duplicateProb);
			}

			double n = 20 * numPasses * 1;
			System.out.println("Example single slot, |S|=20: N=" + n + ", N_E="
					+ Math.ceil(n * 0.05));
			n = 20 * numPasses * 6;
			System.out.println("Example multi slots, max|S|=20, |D_S|=6: N="
					+ n + ", N_E=" + Math.ceil(n * 0.05));
		}
	}

	@Test
	public void testSerialisation() throws Exception {
		SerialisationClass sc = new SerialisationClass();
		// Basic value tests
		assertEquals(sc.getFoo(), 56);
		sc.setFoo(23);
		assertEquals(sc.getFoo(), 23);

		// Serialisation and deserialisation
		File testFile = new File("testFile.ser");
		testFile.createNewFile();
		testFile.deleteOnExit();
		FileOutputStream fos = new FileOutputStream(testFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(sc);
		oos.close();
		fos.close();

		assertTrue(testFile.exists());

		FileInputStream fis = new FileInputStream(testFile);
		ObjectInputStream ois = new ObjectInputStream(fis);
		SerialisationClass serSC = (SerialisationClass) ois.readObject();
		ois.close();
		fis.close();

		assertFalse(serSC.getFoo() == 23);
		assertEquals(serSC.getFoo(), 0);
	}

	@Test
	public void testPriorityQueue() {
		PriorityQueue<Integer> pq = new PriorityQueue<Integer>();
		pq.add(4);
		pq.add(10);
		pq.add(1);
		pq.add(6);
		pq.add(3);
		System.out.println(pq);
		assertTrue(pq.poll() == 1);
		assertTrue(pq.poll() == 3);
		assertTrue(pq.poll() == 4);
		assertTrue(pq.poll() == 6);
		assertTrue(pq.poll() == 10);
	}

	@Test
	public void testDouble() {
		double prob = 0.8675432;
		double smallProb = 1E-16;
		double tinyProb = 1E-17;
		assertTrue(prob + smallProb != prob);
		assertTrue(smallProb + tinyProb != smallProb);
		// Value is too small.
		assertFalse(prob + tinyProb != prob);
	}

	@Test
	public void testGreedyPolicy() {
		char[] slots = { 'a', 'b', 'c', 'd', 'e', 'f' };
		double[] binomialProbs = { 0.9, 0.495, 0.485, 0.475, 0.3, 0.2 };
		Map<String, Integer> slotCounts = new HashMap<String, Integer>();
		Random rand = new Random(1);
		int bestCount = 0;
		String bestSample = null;
		for (int i = 0; i < 10000; i++) {
			String sample = "";
			for (int s = 0; s < binomialProbs.length; s++) {
				if (rand.nextDouble() < binomialProbs[s])
					sample += slots[s];
			}

			Integer count = slotCounts.get(sample);
			if (count == null)
				count = 0;
			slotCounts.put(sample, count + 1);

			if (count + 1 > bestCount) {
				bestCount = count + 1;
				bestSample = sample;
			}
		}

		SortedMap<Double, String> sortedCounts = new TreeMap<Double, String>();
		for (String key : slotCounts.keySet()) {
			Double count = slotCounts.get(key).doubleValue();
			while (sortedCounts.containsKey(count))
				count += 0.01;
			sortedCounts.put(count, key);
		}
		bestCount = sortedCounts.lastKey().intValue();
		bestSample = sortedCounts.get(sortedCounts.lastKey());
		System.out.println(bestSample + ": " + bestCount);
	}
}
