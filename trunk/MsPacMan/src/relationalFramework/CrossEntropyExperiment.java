package relationalFramework;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.mandarax.kernel.Rule;
import org.rlcommunity.rlglue.codec.RLGlue;
import org.rlcommunity.rlglue.codec.types.RL_abstract_type;

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
	/** The performance output file. */
	private File performanceFile_;
	/** The folder to store the temp files. */
	private static final File TEMP_FOLDER = new File("temp/");

	public static final String ELEMENT_DELIMITER = ",";

	/** The population size of the experiment. */
	private int population_;
	/** The number of episodes to run. */
	private int episodes_;
	/** The ratio of samples to use as 'elite' samples. */
	private static final double SELECTION_RATIO = 0.05;
	/** The rate at which the weights change. */
	private static final double STEP_SIZE = 0.6;
	/** The rate of decay on the slots. */
	private static final double SLOT_DECAY_RATE = 0.98;
	/** The cross-entropy generator for the slots in the policy. */
	private ProbabilityDistribution<Integer> slotGenerator_;
	/** The maximum size of the policy. */
	private int policySize_;
	/** The time that the experiment started. */
	private long experimentStart_;
	/** The ratio of shared/regenerated rules. */
	private float ratioShared_ = 0.08f;

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
	 * A constructor for the typical arguments plus an initial rule base.
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
		population_ = populationSize;
		episodes_ = episodeCount;

		// Load the generators from the input file
		RuleBase.initInstance(0, environmentClass);
		policySize_ = RuleBase.getInstance().getNumSlots();

		slotGenerator_ = new ProbabilityDistribution<Integer>();
		// Filling the generators
		for (int i = 0; i < policySize_; i++) {
			slotGenerator_.add(i, 0.5);
		}

		// Create the output files if necessary
		policyFile_ = new File(policyFile);
		generatorFile_ = new File(generatorFile);
		performanceFile_ = new File(performanceFile);
		try {
			if (!policyFile_.exists())
				policyFile_.createNewFile();
			if (!generatorFile_.exists())
				generatorFile_.createNewFile();
			if (!performanceFile_.exists())
				performanceFile_.createNewFile();
			TEMP_FOLDER.mkdir();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		initialise(environmentClass, populationSize, episodeCount, 1,
				policyFile, generatorFile, performanceFile);

		// Load the generators from the input file
		RuleBase.initInstance(new File(ruleFile));
		RuleBase.getInstance().normaliseDistributions();
		policySize_ = RuleBase.getInstance().getNumSlots();

		slotGenerator_ = new ProbabilityDistribution<Integer>();
		// Filling the generators
		for (int i = 0; i < policySize_; i++) {
			slotGenerator_.add(i, 0.5);
		}
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
			loadGenerators(new File(genInputFile));
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
		RLGlue.RL_env_message("25");
		System.out.println("Goal: " + StateSpec.getInstance().getGoalState());

		PolicyValue bestPolicy = null;

		// Determine the iitial run (as previous runs may hve already been done
		// in a previous experiment)
		// TODO int run = checkFiles();
		int run = 0;

		// The ultra-outer loop, for averaging experiment results
		for (; run < runs; run++) {
			float[] episodePerformances = new float[episodes_ + 1];
			episodePerformances[0] = testAgent(-1, maxSteps);
			float runningAverage = 0;
			// The outer loop, for refinement episode by episode
			for (int t = 0; t < episodes_; t++) {
				// Forming a population of solutions
				SortedSet<PolicyValue> pvs = new TreeSet<PolicyValue>();
				// TODO Even this population value can change. Blocks World is
				// showing good results even with populations of 10. Perhaps
				// this could be equal to the policy size * 10?
				for (int i = 0; i < population_; i++) {
					Policy pol = generatePolicy(policySize_, slotGenerator_);
					System.out.println(pol);
					// Send the agent a generated policy
					RLGlue.RL_agent_message(pol.toParseableString());

					float score = 0;
					for (int j = 0; j < AVERAGE_ITERATIONS; j++) {
						RLGlue.RL_episode(maxSteps);
						score += RLGlue.RL_return();
					}
					score /= AVERAGE_ITERATIONS;
					System.out.println(score);
					// Set the fired rules back to this policy
					pol.setFired(RLGlue.RL_agent_message("getFired"));

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
				episodePerformances[t + 1] = testAgent(t, maxSteps);
				runningAverage = ((episodePerformances[t + 1] - episodePerformances[t]) + runningAverage) / 2;

				// Run post-update operations on the distributions
				double sineRatio = (-Math.cos((t * 2 * Math.PI)
						/ (episodes_ - 1)) + 1) / 2 * 0.08;
				ratioShared_ = RuleBase.getInstance().postUpdateOperations(
						runningAverage, ratioShared_, STEP_SIZE);

				// Save the results at each episode
				try {
					saveGenerators(t, run);
					saveBestPolicy(bestPolicy);
					// Output the episode averages
					savePerformance(episodePerformances, run);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// Resetting experiment values
			RuleBase.getInstance().resetInstance();
			slotGenerator_.resetProbs(0.5);
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
	 * generators and trialling the agent several times over the environment to
	 * get an idea of the average performance at this point.
	 * 
	 * @param maxSteps
	 *            The maximum number of allowed.
	 * @return The average performance of the agent.
	 */
	private float testAgent(int episode, int maxSteps) {
		long testStart = System.currentTimeMillis();
		System.out.println();
		System.out.println("Beginning testing for episode " + episode + ".");
		System.out.println();
		float averageScore = 0;
		RLGlue.RL_env_message("freeze");

		ProbabilityDistribution<Integer> frozenSlotGen = slotGenerator_
				.bindProbs(true);
		// Run the agent through several test iterations, resampling the agent
		// at each step
		for (int i = 0; i < TEST_ITERATIONS; i++) {
			Policy pol = generatePolicy(policySize_, frozenSlotGen);
			System.out.println(pol);
			// Send the agent a generated policy
			RLGlue.RL_agent_message(pol.toParseableString());

			for (int j = 0; j < AVERAGE_ITERATIONS; j++) {
				RLGlue.RL_episode(maxSteps);
				averageScore += RLGlue.RL_return();
			}
			System.out.println("For episode test: " + episode);
			estimateETA(testStart, i + 1, 0, TEST_ITERATIONS, 1, "test");
			System.out.println();
		}
		averageScore /= (AVERAGE_ITERATIONS * TEST_ITERATIONS);

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
		String totalPercentStr = formatter.format(100 * totalPercent)
				+ "% " + stringType + " complete.";
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

		buf.write(bestPolicy.getPolicy().toParseableString() + "\n");
		buf.write(bestPolicy.getValue() + "\n");

		buf.close();
		wr.close();
	}

	/**
	 * Saves the generators/distributions to file.
	 * 
	 * @throws Exception
	 *             Should something go awry.
	 */
	private void saveGenerators(int episode, int run) throws Exception {
		File tempGen = new File(TEMP_FOLDER + "/" + generatorFile_.getName()
				+ run);
		tempGen.createNewFile();
		FileWriter wr = new FileWriter(tempGen);
		BufferedWriter buf = new BufferedWriter(wr);

		StringBuffer strBuffer = new StringBuffer();
		// Write the slot generator
		for (int i = 0; i < slotGenerator_.size(); i++) {
			strBuffer.append(slotGenerator_.getProb(i) + ELEMENT_DELIMITER);
		}
		strBuffer.append("\n");
		buf.write(strBuffer.toString());
		// Write the rule generators
		RuleBase.getInstance().writeGenerators(buf);
		buf.write(episode + "");

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
		Double[][] probs = new Double[policySize_ + 1][];
		// For every generator file
		for (int i = 0; i < runs; i++) {
			File tempGen = new File(TEMP_FOLDER + "/" + generatorFile_ + i);
			FileReader reader = new FileReader(tempGen);
			BufferedReader buf = new BufferedReader(reader);

			int dist = 0;
			String input;
			// For each value in the generator file
			while (((input = buf.readLine()) != null) && (!input.equals(""))) {
				ArrayList<Double> vals = new ArrayList<Double>();
				String[] split = input.split(ELEMENT_DELIMITER);
				if (split.length > 1) {
					for (String str : split) {
						vals.add(Double.parseDouble(str));
					}

					// Adding the values
					if (probs[dist] == null)
						probs[dist] = vals.toArray(new Double[vals.size()]);
					else {
						for (int j = 0; j < vals.size(); j++) {
							probs[dist][j] += vals.get(j);
						}
					}

					dist++;
				}
			}

			buf.close();
			reader.close();
		}

		// Average and write the generators out
		FileWriter writer = new FileWriter(generatorFile_);
		BufferedWriter buf = new BufferedWriter(writer);
		for (Double[] values : probs) {
			for (Double val : values) {
				buf.write((val / runs) + ELEMENT_DELIMITER);
			}
			buf.write("\n");
		}
		buf.close();
		writer.close();
	}

	/**
	 * Loads the generators/distributions from file.
	 * 
	 * @param file
	 *            The file to laod from.
	 * @throws Exception
	 *             Should something go awry.
	 */
	private void loadGenerators(File file) throws Exception {
		FileReader reader = new FileReader(file);
		BufferedReader buf = new BufferedReader(reader);

		// Parse the slots
		String[] split = buf.readLine().split(ELEMENT_DELIMITER);
		for (int i = 0; i < split.length; i++) {
			slotGenerator_.set(i, Double.parseDouble(split[i]));
		}

		// Parse the rules
		RuleBase.getInstance().readGenerators(buf);

		buf.close();
		reader.close();
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
		int[][] slotCounter = new int[policySize_][1 + RuleBase.getInstance()
				.size()];
		countRules(iter, numElite, slotCounter);

		// Apply the weights to the distributions
		for (int s = 0; s < slotGenerator_.size(); s++) {
			// Change the slot probabilities, factoring in decay
			slotGenerator_.updateElement(numElite, slotCounter[s][0],
					STEP_SIZE, SLOT_DECAY_RATE, s);

			// Update the internal rule distributions
			RuleBase.getInstance().updateDistribution(s, slotCounter[s], 1,
					STEP_SIZE, 1);
		}
	}

	/**
	 * Counts the rules from the elite samples and stores their frequencies and
	 * total score.
	 * 
	 * @param iter
	 *            The iterator through the samples.
	 * @param numElite
	 *            The number of elite samples to iterate through.
	 * @param slotCounter
	 *            The storage for the rule counts.
	 * @return The average value of the elite samples.
	 */
	private void countRules(Iterator<PolicyValue> iter, int numElite,
			int[][] slotCounter) {
		// Only selecting the top elite samples
		for (int k = 0; k < numElite; k++) {
			PolicyValue pv = iter.next();
			Policy eliteSolution = pv.getPolicy();

			// Count the occurrences of rules and slots in the policy
			GuidedRule[] polRules = eliteSolution.getRules();
			for (int i = 0; i < polRules.length; i++) {
				// If there is a rule
				if (polRules[i] != null) {
					slotCounter[i][0]++;
					slotCounter[i][1 + RuleBase.getInstance().indexOf(
							polRules[i], i)]++;
				}
			}
		}
	}

	/**
	 * Generates a random policy using the weights present in the probability
	 * distribution.
	 * 
	 * @param policySize
	 *            The size of the policy to create.
	 * @param generator
	 *            The generator for generating the policy.
	 * @return A new policy, formed using weights from the probability
	 *         distributions.
	 */
	private Policy generatePolicy(int policySize,
			ProbabilityDistribution<Integer> generator) {
		Policy policy = new Policy(policySize);

		// Run through the policy, adding any rule with probability p and a
		// particular rule with probability q.
		for (int i = 0; i < policySize; i++) {
			if (generator.bernoulliSample(i) != null) {
				policy.addRule(i, RuleBase.getInstance().getRuleGenerator(i)
						.sample());
			}
		}
		return policy;
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

		theExperiment.runExperiment(1);
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
			PolicyValue pv = (PolicyValue) o;
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
		public int hashCode() {
			return (int) (value_ * policy_.hashCode());
		}
	}
}
