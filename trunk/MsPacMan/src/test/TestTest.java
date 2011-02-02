package test;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
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
}
