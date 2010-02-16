package relationalFramework;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.*;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.rlcommunity.rlglue.codec.RLGlue;

/**
 * The cross entropy algorithm implementation.
 * 
 * @author Sam Sarjant
 */
public class CrossEntropyExperiment {
	/** The number of iterations a policy is repeated to get an average score. */
	public static final int AVERAGE_ITERATIONS = 3;
	/** The number of test episodes to run for performance measures. */
	public static final int TEST_ITERATIONS = 100;
	/** The best policy found output file. */
	private File policyFile_;
	/** The generator states file. */
	private File generatorFile_;
	/** The generator states file. */
	private File humanGeneratorFile_;
	/** The performance output file. */
	private File performanceFile_;
	/** The folder to store the temp files. */
	private static final File TEMP_FOLDER = new File("temp/");

	/** The policy generator for the experiment. */
	private PolicyGenerator policyGenerator_;
	/** The population size of the experiment. */
	private int population_;
	/** The number of episodes to run. */
	private int episodes_;
	/** The ratio of samples to use as 'elite' samples. */
	private static final double SELECTION_RATIO = 0.05;
	/** The rate at which the weights change. */
	private static final double STEP_SIZE = 0.6;
	/** The time that the experiment started. */
	private long experimentStart_;

	/**
	 * A constructor for initialising the cross-entropy generators and
	 * experiment parameters from an argument file.
	 * 
	 * @param argumentFile
	 *            The file containing the arguments.
	 */
	public CrossEntropyExperiment(File argumentFile) {
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);

		// Read the arguments in from file.
		ArrayList<String> argsList = new ArrayList<String>();
		try {
			FileReader reader = new FileReader(argumentFile);
			BufferedReader bf = new BufferedReader(reader);

			String input = null;
			while (((input = bf.readLine()) != null) && (!input.equals(""))) {
				argsList.add(input);
			}

			bf.close();
			reader.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		// Choose the correct constructor to initialise the values.
		String[] args = new String[argsList.size()];
		argsList.toArray(args);
		if (args.length == 7) {
			try {
				// Random rules
				Integer.parseInt(args[3]);
				initialise(args[0], Integer.parseInt(args[1]), Integer
						.parseInt(args[2]), Integer.parseInt(args[3]), args[4],
						args[5], args[6]);
			} catch (Exception e) {
				// Rules from file
				initialise(args[0], Integer.parseInt(args[1]), Integer
						.parseInt(args[2]), args[3], args[4], args[5], args[6]);
			}
		} else if (args.length == 9) {
			initialise(args[0], Integer.parseInt(args[1]), Integer
					.parseInt(args[2]), args[3], args[5], args[6], args[7],
					args[8]);
		}
	}

	/**
	 * A bare minimal initialiser, should only be called by other initialise
	 * methods as their first execution.
	 * 
	 * @param populationSize
	 *            The size of the population used in calculations.
	 * @param episodeCount
	 *            The number of episodes to perform.
	 * @param policyFile
	 *            The output file for the best policy.
	 * @param generatorFile
	 *            The output file for the generators.
	 * @param performanceFile
	 *            The output file for the agent's performance.
	 */
	private void initialise(int populationSize, int episodeCount,
			String policyFile, String generatorFile, String performanceFile) {
		population_ = populationSize;
		episodes_ = episodeCount;

		// Create the output files if necessary
		policyFile_ = new File(policyFile);
		generatorFile_ = new File(generatorFile);
		humanGeneratorFile_ = new File("readable-" + generatorFile);
		performanceFile_ = new File(performanceFile);
		try {
			if (!policyFile_.exists())
				policyFile_.createNewFile();
			if (!generatorFile_.exists())
				generatorFile_.createNewFile();
			if (!humanGeneratorFile_.exists())
				humanGeneratorFile_.createNewFile();
			if (!performanceFile_.exists())
				performanceFile_.createNewFile();
			TEMP_FOLDER.mkdir();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * A constructor for the typical arguments and a randomly generated rule
	 * base.
	 * 
	 * @param environmentClass
	 *            The name of the environment class files.
	 * @param populationSize
	 *            The size of the population used in calculations.
	 * @param episodeCount
	 *            The number of episodes to perform.
	 * @param policySize
	 *            The size of the policy to load.
	 * @param policyFile
	 *            The output file for the best policy.
	 * @param generatorFile
	 *            The output file for the generators.
	 * @param performanceFile
	 *            The output file for the agent's performance.
	 */
	private void initialise(String environmentClass, int populationSize,
			int episodeCount, int policySize, String policyFile,
			String generatorFile, String performanceFile) {
		initialise(populationSize, episodeCount, policyFile, generatorFile,
				performanceFile);

		// Load the generators from the input file
		PolicyGenerator.initInstance(environmentClass);
		policyGenerator_ = PolicyGenerator.getInstance();
	}

	/**
	 * A constructor for the typical arguments plus an initial rule base.
	 * 
	 * @param environmentClass
	 *            The name of the environment class files.
	 * @param populationSize
	 *            The size of the population used in calculations.
	 * @param episodeCount
	 *            The number of episodes to perform.
	 * @param ruleFile
	 *            The file to load the rules from.
	 * @param policyFile
	 *            The output file for the best policy.
	 * @param generatorFile
	 *            The output file for the generators.
	 * @param performanceFile
	 *            The output file for the agent's performance.
	 */
	private void initialise(String environmentClass, int populationSize,
			int episodeCount, String ruleFile, String policyFile,
			String generatorFile, String performanceFile) {
		initialise(populationSize, episodeCount, policyFile, generatorFile,
				performanceFile);

		// Load the generators from the input file
		PolicyGenerator.initInstance(environmentClass, new File(ruleFile));
		PolicyGenerator.getInstance().normaliseDistributions();
		policyGenerator_ = PolicyGenerator.getInstance();
	}

	/**
	 * A constructor for the typical arguments plus a state of the generators
	 * file and rulebase.
	 * 
	 * @param environmentClass
	 *            The name of the environment class files.
	 * @param populationSize
	 *            The size of the population used in calculations.
	 * @param episodeCount
	 *            The number of episodes to perform.
	 * @param policySize
	 *            The maximum number of rules in the policy.
	 * @param ruleFile
	 *            The file to load the rules from.
	 * @param policyFile
	 *            The output file for the best policy.
	 * @param generatorFile
	 *            The output file for the generators.
	 * @param performanceFile
	 *            The output file for the agent's performance.
	 * @param genInputFile
	 *            The input file for generators.
	 */
	private void initialise(String environmentClass, int populationSize,
			int episodeCount, String ruleFile, String policyFile,
			String generatorFile, String performanceFile, String genInputFile) {
		initialise(environmentClass, populationSize, episodeCount, ruleFile,
				policyFile, generatorFile, performanceFile);

		// Load the generators from the input file
		try {
			policyGenerator_.loadGenerators(new File(generatorFile));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Runs the experiment through a number of iterations and saves the averaged
	 * performance.
	 * 
	 * @param runs
	 *            The number of runs to average the results over.
	 */
	public void runExperiment(int runs) {
		experimentStart_ = System.currentTimeMillis();

		// Initialise the environment/agent
		RLGlue.RL_init();
		int maxSteps = Integer.parseInt(RLGlue.RL_env_message("maxSteps"));
		RLGlue.RL_env_message("5");
		System.out.println("Goal: " + StateSpec.getInstance().getGoalState());

		PolicyValue bestPolicy = null;

		// Determine the initial run (as previous runs may have already been
		// done in a previous experiment)
		int run = checkFiles();

		// The ultra-outer loop, for averaging experiment results
		for (; run < runs; run++) {
			float[] episodePerformances = new float[episodes_ + 1];
			//episodePerformances[0] = testAgent(-1, maxSteps, run, runs);
			// The outer loop, for refinement episode by episode
			for (int t = 0; t < episodes_; t++) {
				// Forming a population of solutions
				SortedSet<PolicyValue> pvs = new TreeSet<PolicyValue>();
				// TODO Even this population value can change. Blocks World is
				// showing good results even with populations of 10. Perhaps
				// this could be equal to the policy size * 10?
				for (int i = 0; i < population_; i++) {
					Policy pol = policyGenerator_.generatePolicy();
					System.out.println(pol);
					// Send the agent a generated policy
					ObjectObservations.getInstance().objectArray = new Policy[] { pol };
					RLGlue.RL_agent_message("Policy");

					float score = 0;
					for (int j = 0; j < AVERAGE_ITERATIONS; j++) {
						RLGlue.RL_episode(maxSteps);
						score += RLGlue.RL_return();
					}
					score /= AVERAGE_ITERATIONS;
					System.out.println(score);

					PolicyValue thisPolicy = new PolicyValue(pol, score);
					pvs.add(thisPolicy);
					// Storing the best policy
					if ((bestPolicy == null)
							|| (thisPolicy.getValue() > bestPolicy.getValue()))
						bestPolicy = thisPolicy;

					// Give an ETA
					estimateETA(experimentStart_, t * population_ + i + 1, run,
							episodes_ * population_, runs, "experiment");
				}

				// Update the weights for all distributions using only the elite
				// samples
				updateWeights(pvs.iterator(), (int) Math.ceil(population_
						* SELECTION_RATIO));

				// Test the agent and record the performances
				episodePerformances[t + 1] = testAgent(t, maxSteps, run, runs);

				// Save the results at each episode
				try {
					File tempGen = new File(TEMP_FOLDER + "/"
							+ generatorFile_.getName() + run);
					tempGen.createNewFile();
					RuleFileManager.saveGenerators(tempGen);
					saveBestPolicy(bestPolicy);
					// Output the episode averages
					savePerformance(episodePerformances, run);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// Resetting experiment values
			PolicyGenerator.getInstance().resetGenerator();
		}

		RLGlue.RL_cleanup();

		try {
			combineGenerators(runs);
			compilePerformanceAverage(runs);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Tests the agent at its current state. This is achieved by 'freezing' the
	 * generators and trialing the agent several times over the environment to
	 * get an idea of the average performance at this point.
	 * 
	 * @param maxSteps
	 *            The maximum number of allowed.
	 * @param episode
	 *            The current episode
	 * @param run
	 *            The current run number.
	 * @param runs
	 *            The total number of runs to complete.
	 * @return The average performance of the agent.
	 */
	private float testAgent(int episode, int maxSteps, int run, int runs) {
		long testStart = System.currentTimeMillis();
		System.out.println();
		System.out.println("Beginning testing for episode " + episode + ".");
		System.out.println();
		float averageScore = 0;
		RLGlue.RL_env_message("freeze");

		// Run the agent through several test iterations, resampling the agent
		// at each step
		for (int i = 0; i < TEST_ITERATIONS; i++) {
			Policy pol = policyGenerator_.generatePolicy();
			System.out.println(pol);
			// Send the agent a generated policy
			ObjectObservations.getInstance().objectArray = new Policy[] { pol };
			RLGlue.RL_agent_message("Policy");

			double score = 0;
			for (int j = 0; j < AVERAGE_ITERATIONS; j++) {
				RLGlue.RL_episode(maxSteps);
				score += RLGlue.RL_return();
			}
			averageScore += score;
			System.out.println(score / AVERAGE_ITERATIONS + "\n");

			System.out.println("For episode test: " + episode);
			estimateETA(testStart, i + 1, run, TEST_ITERATIONS, runs, "test");
			System.out.println();
		}
		averageScore /= (AVERAGE_ITERATIONS * TEST_ITERATIONS);

		// Write the state of the generators out in human readable form
		try {
			File output = new File(TEMP_FOLDER + "/"
					+ humanGeneratorFile_.getName() + run);
			output.createNewFile();
			RuleFileManager.saveHumanGenerators(output);
		} catch (Exception e) {
			e.printStackTrace();
		}

		RLGlue.RL_env_message("unfreeze");
		return averageScore;
	}

	/**
	 * Checks the files for pre-existing versions so runs do not have to be
	 * re-run.
	 * 
	 * @return The run number that the files stopped at.
	 */
	private int checkFiles() {
		// Check the performance files
		int run = -1;
		File tempPerf = null;
		do {
			run++;
			tempPerf = new File(TEMP_FOLDER + "/" + performanceFile_.getName()
					+ run);
		} while (tempPerf.exists());
		return run;
	}

	/**
	 * Prints out the percentage complete, time elapsed and estimated time to
	 * completion.
	 * 
	 * @param timeStart
	 *            The start time.
	 * @param currentProg
	 *            The current progress.
	 * @param run
	 *            The current run number.
	 * @param totalProg
	 *            The total amount of progress to cover.
	 * @param runs
	 *            The total number of runs.
	 */
	private void estimateETA(long timeStart, int currentProg, int run,
			int totalProg, int runs, String stringType) {
		long elapsedTime = System.currentTimeMillis() - timeStart;
		double percent = (currentProg * 1.0) / totalProg;
		double totalPercent = (currentProg * 1.0 + run * totalProg)
				/ (totalProg * runs);

		DecimalFormat formatter = new DecimalFormat("#0.000");
		String percentStr = formatter.format(100 * percent) + "% run complete.";
		String totalPercentStr = formatter.format(100 * totalPercent) + "% "
				+ stringType + " complete.";
		String elapsed = "Elapsed: " + elapsedTime / (1000 * 60 * 60) + ":"
				+ (elapsedTime / (1000 * 60)) % 60 + ":" + (elapsedTime / 1000)
				% 60;
		long remainingTime = (long) (elapsedTime / totalPercent - elapsedTime);
		String remaining = "Remaining: " + remainingTime / (1000 * 60 * 60)
				+ ":" + (remainingTime / (1000 * 60)) % 60 + ":"
				+ (remainingTime / 1000) % 60;
		System.out.println(percentStr);
		System.out.println(totalPercentStr + " " + elapsed + ", " + remaining);
		System.out.println();
	}

	/**
	 * Saves the best policy to a file.
	 * 
	 * @param bestPolicy
	 *            The best policy, in string format.
	 */
	private void saveBestPolicy(PolicyValue bestPolicy) throws Exception {
		FileWriter wr = new FileWriter(policyFile_);
		BufferedWriter buf = new BufferedWriter(wr);

		buf.write(bestPolicy.getPolicy().toString() + "\n");
		buf.write(bestPolicy.getValue() + "\n");

		buf.close();
		wr.close();
	}

	/**
	 * Saves the performance to file and outputs them.
	 * 
	 * @param episodeAverage
	 *            The saved episode average performances.
	 */
	private void savePerformance(float[] episodeAverage, int run)
			throws Exception {
		File tempPerf = new File(TEMP_FOLDER + "/" + performanceFile_.getName()
				+ run);
		tempPerf.createNewFile();
		FileWriter wr = new FileWriter(tempPerf);
		BufferedWriter buf = new BufferedWriter(wr);

		System.out.println("Average episode elite scores:");
		for (int e = 0; e < episodeAverage.length; e++) {
			buf.write(episodeAverage[e] + "\n");
			System.out.println(episodeAverage[e]);
		}

		buf.close();
		wr.close();
	}

	/**
	 * Compiles the performance files togetrher into a single file, detailing
	 * the average, min and max performances.
	 * 
	 * @param runs
	 *            The number of runs involved in the experiment.
	 */
	private void compilePerformanceAverage(int runs) throws Exception {
		double[][] performances = new double[episodes_][runs];
		float min = Float.MAX_VALUE;
		int minIndex = -1;
		float max = -Float.MAX_VALUE;
		int maxIndex = -1;
		// For every performance file
		for (int i = 0; i < runs; i++) {
			File tempPerf = new File(TEMP_FOLDER + "/" + performanceFile_ + i);
			FileReader reader = new FileReader(tempPerf);
			BufferedReader buf = new BufferedReader(reader);

			// For every value within the performance file
			float sum = 0;
			for (int e = 0; e < episodes_; e++) {
				float val = Float.parseFloat(buf.readLine());
				performances[e][i] = val;
				sum += val;
			}

			// Find min or max runs
			if (sum < min) {
				min = sum;
				minIndex = i;
			}
			if (sum > max) {
				max = sum;
				maxIndex = i;
			}

			buf.close();
			reader.close();
		}

		// Calculate the average and print out the stats
		FileWriter writer = new FileWriter(performanceFile_);
		BufferedWriter buf = new BufferedWriter(writer);
		buf.write("Average\tSD\tMin\tMax\n");
		for (int e = 0; e < performances.length; e++) {
			Mean mean = new Mean();
			StandardDeviation sd = new StandardDeviation();
			buf.write(mean.evaluate(performances[e]) + "\t"
					+ sd.evaluate(performances[e]) + "\t"
					+ performances[e][minIndex] + "\t"
					+ performances[e][maxIndex] + "\n");
		}

		buf.close();
		writer.close();
	}

	/**
	 * Combines the generators into a single file, averaging the generator
	 * values.
	 * 
	 * @param runs
	 *            The number of runs involved in the experiment.
	 * @throws Exception
	 *             Should something go awry...
	 */
	private void combineGenerators(int runs) throws Exception {
		// TODO Combine generators in a modular fashion
	}

	/**
	 * Updates the weights in the probability distributions according to their
	 * frequency within the 'elite' samples.
	 * 
	 * @param iter
	 *            The iterator over the samples.
	 * @param numElite
	 *            The number of samples to form the 'elite' samples.
	 */
	private void updateWeights(Iterator<PolicyValue> iter, int numElite) {
		// Keep count of the rules seen (and slots used)
		Map<Slot, Integer> slotCounts = new HashMap<Slot, Integer>();
		Map<GuidedRule, Integer> ruleCounts = new HashMap<GuidedRule, Integer>();
		countRules(iter, numElite, slotCounts, ruleCounts);

		// Apply the weights to the distributions
		policyGenerator_.updateDistributions(numElite, slotCounts, ruleCounts,
				STEP_SIZE);
	}

	/**
	 * Counts the rules from the elite samples and stores their frequencies and
	 * total score.
	 * TODO Fix this up
	 * 
	 * @param iter
	 *            The iterator through the samples.
	 * @param numElite
	 *            The number of elite samples to iterate through.
	 * @param slotCounts
	 *            The counts for the slots
	 * @param ruleCounts
	 *            The counts for the individual rules.
	 * @return The average value of the elite samples.
	 */
	private void countRules(Iterator<PolicyValue> iter, int numElite,
			Map<Slot, Integer> slotCounts, Map<GuidedRule, Integer> ruleCounts) {
		// Only selecting the top elite samples
		for (int k = 0; k < numElite; k++) {
			PolicyValue pv = iter.next();
			Policy eliteSolution = pv.getPolicy();

			// Count the occurrences of rules and slots in the policy
			Collection<GuidedRule> polRules = eliteSolution.getFiringRules();
			// TODO Ensure this is performing correctly.
			for (GuidedRule rule : polRules) {
				// Slot counts
				Slot ruleSlot = rule.getSlot();
				Integer count = slotCounts.get(ruleSlot);
				if (count == null)
					count = 0;
				slotCounts.put(ruleSlot, count + 1);

				// Rule counts
				count = ruleCounts.get(ruleSlot);
				if (count == null)
					count = 0;
				ruleCounts.put(rule, count + 1);
			}
		}
	}

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments for the program.
	 */
	public static void main(String[] args) {
		CrossEntropyExperiment theExperiment = new CrossEntropyExperiment(
				new File(args[0]));

		theExperiment.runExperiment(10);
		System.exit(0);
	}

	/**
	 * A simple class for binding a policy and a value together in a comparable
	 * format.
	 * 
	 * @author Samuel J. Sarjant
	 * 
	 */
	private class PolicyValue implements Comparable<PolicyValue> {
		/** The policy. */
		private Policy policy_;
		/** The estimated value of the policy. */
		private float value_;

		/**
		 * A constructor for storing the members.
		 * 
		 * @param pol
		 *            The policy.
		 * @param value
		 *            The (estimated) value
		 */
		public PolicyValue(Policy pol, float value) {
			policy_ = pol;
			value_ = value;
		}

		/**
		 * Gets the policy for this policy-value.
		 * 
		 * @return The policy.
		 */
		public Policy getPolicy() {
			return policy_;
		}

		/**
		 * Gets the value for this policy-value.
		 * 
		 * @return The value.
		 */
		public float getValue() {
			return value_;
		}

		// @Override
		public int compareTo(PolicyValue o) {
			if ((o == null) || (!(o instanceof PolicyValue)))
				return -1;
			PolicyValue pv = o;
			// If this value is bigger, it comes first
			if (value_ > pv.value_) {
				return -1;
			} else if (value_ < pv.value_) {
				// Else it is after
				return 1;
			} else {
				// If all else fails, order by hash code
				return Float.compare(hashCode(), o.hashCode());
			}
		}

		// @Override
		@Override
		public boolean equals(Object obj) {
			if ((obj == null) || (!(obj instanceof PolicyValue)))
				return false;
			PolicyValue pv = (PolicyValue) obj;
			if (value_ == pv.value_) {
				if (policy_ == pv.policy_) {
					return true;
				}
			}
			return false;
		}

		// @Override
		@Override
		public int hashCode() {
			return (int) (value_ * policy_.hashCode());
		}
	}
}
