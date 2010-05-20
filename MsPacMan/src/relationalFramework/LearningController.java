package relationalFramework;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class LearningController {
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

	/** The number of episodes to run. */
	private int maxEpisodes_;
	/** The number of times to repeat the experiment. */
	private int repetitions_ = 1;
	/** The ratio of samples to use as 'elite' samples. */
	private static final double POPULATION_CONSTANT = 50;
	/** The ratio of samples to use as 'elite' samples. */
	private static final double SELECTION_RATIO = 0.1;
	/** The rate at which the weights change. */
	private static final double STEP_SIZE = 0.6;
	/** The minimum value for weight updating. */
	private static final double MIN_UPDATE = 0.1;
	/** The internal prefix for messages to the agent regarding internal goal. */
	public static final String INTERNAL_PREFIX = "internal";
	/** The time that the experiment started. */
	private long experimentStart_;
	/** The extra arguments to message the environment. */
	private String[] extraArgs_;
	/** The maximum number of steps the agent can take. */
	private int maxSteps_;
	/** If we're using weighted elite samples. */
	private boolean weightedElites_ = true;

	/**
	 * A constructor for initialising the cross-entropy generators and
	 * experiment parameters from an argument file.
	 * 
	 * @param argumentFile
	 *            The file containing the arguments.
	 */
	public LearningController(String[] args) {
		File argumentFile = new File(args[0]);
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);

		// Read the arguments in from file.
		try {
			FileReader reader = new FileReader(argumentFile);
			BufferedReader bf = new BufferedReader(reader);

			String environmentClass = bf.readLine();
			Integer repetitions = Integer.parseInt(bf.readLine());
			Integer episodes = Integer.parseInt(bf.readLine());
			String policyFile = bf.readLine();
			String generatorFile = bf.readLine();
			String performanceFile = bf.readLine();

			ArrayList<String> extraArgsList = new ArrayList<String>();
			String extraArgs = bf.readLine();
			if (extraArgs != null) {
				Pattern p = Pattern.compile("((\".+?\")|\\w+)");
				Matcher m = p.matcher(extraArgs);
				while (m.find())
					extraArgsList.add(m.group().replaceAll("\"", ""));
			}

			bf.close();
			reader.close();

			initialise(environmentClass, repetitions, episodes, policyFile,
					generatorFile, performanceFile, extraArgsList
							.toArray(new String[extraArgsList.size()]));

			if ((args.length > 1) && (args[1].equals("-d")))
				PolicyGenerator.debugMode_ = true;

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
	 * @param repetitions
	 *            The number of times to repeat the experiment.
	 * @param episodeCount
	 *            The number of episodes to perform.
	 * @param policyFile
	 *            The output file for the best policy.
	 * @param generatorFile
	 *            The output file for the generators.
	 * @param performanceFile
	 *            The output file for the agent's performance.
	 * @param extraArgs
	 *            The extra arguments for the environment to take.
	 */
	private void initialise(String environmentClass, int repetitions,
			int episodeCount, String policyFile, String generatorFile,
			String performanceFile, String[] extraArgs) {
		repetitions_ = repetitions;
		maxEpisodes_ = episodeCount;

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
		extraArgs_ = extraArgs;

		// Initialise the state spec.
		StateSpec.initInstance(environmentClass);
	}

	/**
	 * Runs the experiment through a number of iterations and saves the averaged
	 * performance.
	 * 
	 * @param runs
	 *            The number of runs to average the results over.
	 */
	public void runExperiment() {
		experimentStart_ = System.currentTimeMillis();

		// Initialise the environment/agent
		RLGlue.RL_init();
		for (String extraArg : extraArgs_)
			RLGlue.RL_env_message(extraArg);
		maxSteps_ = Integer.parseInt(RLGlue.RL_env_message("maxSteps"));
		System.out.println("Goal: " + StateSpec.getInstance().getGoalState());

		// Determine the initial run (as previous runs may have already been
		// done in a previous experiment)
		int run = checkFiles();

		// The ultra-outer loop, for averaging experiment results
		for (; run < repetitions_; run++) {
			// Initialise a new policy generator.
			PolicyGenerator localPolicy = PolicyGenerator.newInstance();

			developPolicy(localPolicy, run);

			// Flushing the rete object.
			StateSpec.reinitInstance();

			// Resetting experiment values
			PolicyGenerator.getInstance().resetGenerator();
		}

		RLGlue.RL_cleanup();

		try {
			combineGenerators(repetitions_);
			compilePerformanceAverage(repetitions_);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * The policy optimisation loop, which runs through the environment until
	 * the agent has developed a reasonable converged policy.
	 * 
	 * @param localPolicy
	 *            The local policy to develop.
	 * @param run
	 *            The run number of the policy.
	 */
	private void developPolicy(PolicyGenerator localPolicy, int run) {
		PolicyValue bestPolicy = null;

		// Run the preliminary action discovery phase, only starting real
		// optimisation once pre-goal has settled and LGG rules for each
		// action have been found.
		preliminaryProcessing();

		// The outer loop, for refinement episode by episode
		ArrayList<Float> episodePerformances = new ArrayList<Float>();
		int t = 0;
		while ((t < maxEpisodes_) && (!localPolicy.isConverged())) {
			if (PolicyGenerator.getInstance().useModules_) {
				// Check if the agent needs to drop into learning a module
				checkForModularLearning(localPolicy);
			}

			// Determine the dynamic population, based on rule-base size
			int population = determinePopulation();

			// Forming a population of solutions
			List<PolicyValue> pvs = new ArrayList<PolicyValue>(population);
			int expProg = 0;
			for (int i = 0; i < population; i++) {
				Policy pol = localPolicy.generatePolicy();
				System.out.println(pol);
				// Send the agent a generated policy
				ObjectObservations.getInstance().objectArray = new Policy[] { pol };
				RLGlue.RL_agent_message("Policy");

				float score = 0;
				for (int j = 0; j < AVERAGE_ITERATIONS; j++) {
					RLGlue.RL_episode(maxSteps_);
					if (localPolicy.isModuleGenerator())
						score += Double.parseDouble(RLGlue
								.RL_agent_message("internalReward"));
					else
						score += RLGlue.RL_return();
				}
				score /= AVERAGE_ITERATIONS;
				System.out.println(score);

				pol.parameterArgs(null);
				PolicyValue thisPolicy = new PolicyValue(pol, score);
				pvs.add(thisPolicy);
				// Storing the best policy
				if ((bestPolicy == null)
						|| (thisPolicy.getValue() > bestPolicy.getValue()))
					bestPolicy = thisPolicy;

				// Give an ETA
				expProg = t * population + i + 1;
				estimateETA(experimentStart_, expProg, expProg, run,
						maxEpisodes_ * population, repetitions_, "experiment");
			}

			Collections.sort(pvs);
			// Update the weights for all distributions using only the elite
			// samples
			updateWeights(pvs, (int) Math.ceil(population * SELECTION_RATIO));

			// Test the agent and record the performances
			episodePerformances.add(testAgent(t, maxSteps_, run, repetitions_,
					expProg));

			// Save the results at each episode
			try {
				File tempGen = null;
				if (PolicyGenerator.getInstance().isModuleGenerator())
					tempGen = new File(Module.MODULE_DIR + "/" + TEMP_FOLDER
							+ "/"
							+ PolicyGenerator.getInstance().getModuleGoal()
							+ generatorFile_.getName() + run);
				else
					tempGen = new File(TEMP_FOLDER + "/"
							+ generatorFile_.getName() + run);
				tempGen.createNewFile();
				PolicyGenerator.saveGenerators(tempGen);
				saveBestPolicy(bestPolicy);
				// Output the episode averages
				savePerformance(episodePerformances, run);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Run the post update operations
			localPolicy.postUpdateOperations();

			t++;
		}
	}

	/**
	 * Checks for modular learning - if the agent needs to learn a module as an
	 * internal goal.
	 * 
	 * @param policyGenerator
	 *            The policy generator.
	 */
	private void checkForModularLearning(PolicyGenerator policyGenerator) {
		// Run through each rule in the policy generator, noting which ones
		// require module learning.
		Collection<String> modularFacts = policyGenerator.getConstantFacts();

		// Check if we have a module file for each.
		for (Iterator<String> factIter = modularFacts.iterator(); factIter
				.hasNext();) {
			String pred = factIter.next();
			if (Module.moduleExists(StateSpec.getInstance()
					.getEnvironmentName(), pred)) {
				factIter.remove();
			}
		}

		// We should be left with whatever modules do not yet exist
		if (!modularFacts.isEmpty()) {
			// Commence learning of the module
			for (String internalGoal : modularFacts) {
				if (PolicyGenerator.debugMode_) {
					try {
						System.out.println("\n\n\n------LEARNING MODULE: "
								+ internalGoal + "------\n\n\n");
						System.out.println("Press Enter to continue.");
						System.in.read();
						System.in.read();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				// Set the internal goal
				String oldInternalGoal = RLGlue
						.RL_agent_message(INTERNAL_PREFIX + " " + internalGoal);

				// Begin development
				PolicyGenerator modularGenerator = PolicyGenerator.newInstance(
						policyGenerator, internalGoal);
				developPolicy(modularGenerator, -1);

				// Save the module
				Module.saveModule(internalGoal, StateSpec.getInstance()
						.getEnvironmentName(), modularGenerator.getGenerator());

				// Unset the internal goal
				if (oldInternalGoal == null)
					RLGlue.RL_agent_message(INTERNAL_PREFIX + " ");
				else
					RLGlue.RL_agent_message(INTERNAL_PREFIX + " "
							+ oldInternalGoal);
			}

			// Ensure to reset the policy generator
			PolicyGenerator.setInstance(policyGenerator);
		}
	}

	/**
	 * Determines the population of rules to use for optimisation.
	 * 
	 * @return A population of rules, large enough to reasonably test most
	 *         combinations of rules.
	 */
	private int determinePopulation() {
		// Currently just using 10 * the largest slot
		int largestSlot = 0;
		for (Slot slot : PolicyGenerator.getInstance().getGenerator()) {
			largestSlot = Math.max(largestSlot, slot.getGenerator().size());
		}
		return (int) (POPULATION_CONSTANT * largestSlot);
	}

	/**
	 * Run the agent over the environment until pre-goal has settled and lgg
	 * rules have all been found.
	 */
	private void preliminaryProcessing() {
		while (!PolicyGenerator.getInstance().isSettled()) {
			Policy pol = PolicyGenerator.getInstance().generatePolicy();
			System.out.println(pol);
			// Send the agent a generated policy
			ObjectObservations.getInstance().objectArray = new Policy[] { pol };
			RLGlue.RL_agent_message("Policy");
			RLGlue.RL_episode(maxSteps_);
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
	public float testAgent(int episode, int maxSteps, int run, int runs,
			int expProg) {
		long testStart = System.currentTimeMillis();
		System.out.println();
		System.out.println("Beginning testing for episode " + episode + ".");
		System.out.println();
		float averageScore = 0;
		RLGlue.RL_env_message("freeze");

		// Run the agent through several test iterations, resampling the agent
		// at each step
		for (int i = 0; i < TEST_ITERATIONS; i++) {
			Policy pol = PolicyGenerator.getInstance().generatePolicy();
			System.out.println(pol);
			// Send the agent a generated policy
			ObjectObservations.getInstance().objectArray = new Policy[] { pol };
			RLGlue.RL_agent_message("Policy");

			double score = 0;
			for (int j = 0; j < AVERAGE_ITERATIONS; j++) {
				RLGlue.RL_episode(maxSteps);
				if (PolicyGenerator.getInstance().isModuleGenerator())
					score += Double.parseDouble(RLGlue
							.RL_agent_message("internalReward"));
				else
					score += RLGlue.RL_return();
			}
			averageScore += score;
			pol.parameterArgs(null);
			System.out.println(score / AVERAGE_ITERATIONS + "\n");

			System.out.println("For episode test: " + episode);
			estimateETA(testStart, i + 1, expProg, run, TEST_ITERATIONS, runs,
					"test");
			System.out.println();
		}
		averageScore /= (AVERAGE_ITERATIONS * TEST_ITERATIONS);

		// Write the state of the generators out in human readable form
		try {
			File output = null;
			if (PolicyGenerator.getInstance().isModuleGenerator())
				output = new File(Module.MODULE_DIR + "/" + TEMP_FOLDER + "/"
						+ PolicyGenerator.getInstance().getModuleGoal()
						+ humanGeneratorFile_.getName() + run);
			else
				output = new File(TEMP_FOLDER + "/"
						+ humanGeneratorFile_.getName() + run);
			output.createNewFile();
			PolicyGenerator.saveHumanGenerators(output);
		} catch (Exception e) {
			e.printStackTrace();
		}

		RLGlue.RL_env_message("unfreeze");
		return averageScore;
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
	private void updateWeights(List<PolicyValue> sortedPolicies, int numElite) {
		// Keep count of the rules seen (and slots used)
		Map<Slot, Double> slotCounts = new HashMap<Slot, Double>();
		Map<GuidedRule, Double> ruleCounts = new HashMap<GuidedRule, Double>();
		countRules(sortedPolicies.subList(0, numElite), slotCounts, ruleCounts);

		// Apply the weights to the distributions
		PolicyGenerator.getInstance().updateDistributions(numElite, slotCounts,
				ruleCounts, STEP_SIZE);
	}

	/**
	 * Counts the rules from the elite samples and stores their frequencies and
	 * total score.
	 * 
	 * @param elites
	 *            The elite samples to iterate through.
	 * @param slotCounts
	 *            The counts for the slots
	 * @param ruleCounts
	 *            The counts for the individual rules.
	 * @return The average value of the elite samples.
	 */
	private void countRules(List<PolicyValue> elites,
			Map<Slot, Double> slotCounts, Map<GuidedRule, Double> ruleCounts) {
		double gradient = 0;
		double offset = 1;
		if (weightedElites_) {
			double diffValues = (elites.get(0).getValue() - elites.get(
					elites.size() - 1).getValue());
			if (diffValues != 0)
				gradient = (1 - MIN_UPDATE) / diffValues;
			offset = 1 - gradient * elites.get(0).getValue();
		}

		// Only selecting the top elite samples
		for (PolicyValue pv : elites) {
			Policy eliteSolution = pv.getPolicy();

			// Count the occurrences of rules and slots in the policy
			Collection<GuidedRule> firingRules = eliteSolution.getFiringRules();
			for (GuidedRule rule : firingRules) {
				double weight = pv.getValue() * gradient + offset;

				// Slot counts
				Slot ruleSlot = rule.getSlot();
				Double count = slotCounts.get(ruleSlot);
				if (count == null)
					count = 0d;
				slotCounts.put(ruleSlot, count + weight);

				// Rule counts
				count = ruleCounts.get(rule);
				if (count == null)
					count = 0d;
				ruleCounts.put(rule, count + weight);
			}
		}
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
	private void estimateETA(long timeStart, int currentProg, int expProg,
			int run, int totalProg, int runs, String stringType) {
		long elapsedTime = System.currentTimeMillis() - timeStart;
		double percent = (currentProg * 1.0) / totalProg;
		double totalPercent = (expProg * 1.0 + run * totalProg)
				/ (totalProg * runs);

		DecimalFormat formatter = new DecimalFormat("#0.000");
		String percentStr = formatter.format(100 * percent) + "% " + stringType
				+ " run complete.";

		System.out.println(percentStr);
		String totalPercentStr = formatter.format(100 * totalPercent) + "% "
				+ stringType + " complete.";
		String elapsed = "Elapsed: " + elapsedTime / (1000 * 60 * 60) + ":"
				+ (elapsedTime / (1000 * 60)) % 60 + ":" + (elapsedTime / 1000)
				% 60;
		long remainingTime = (long) (elapsedTime / totalPercent - elapsedTime);
		String remaining = "Remaining: " + remainingTime / (1000 * 60 * 60)
				+ ":" + (remainingTime / (1000 * 60)) % 60 + ":"
				+ (remainingTime / 1000) % 60;
		System.out.println(totalPercentStr + " " + elapsed + ", " + remaining);
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
	private void savePerformance(ArrayList<Float> episodeAverage, int run)
			throws Exception {
		File tempPerf = null;
		if (PolicyGenerator.getInstance().isModuleGenerator())
			tempPerf = new File(Module.MODULE_DIR + "/" + TEMP_FOLDER + "/"
					+ PolicyGenerator.getInstance().getModuleGoal()
					+ performanceFile_.getName() + run);
		else
			tempPerf = new File(TEMP_FOLDER + "/" + performanceFile_.getName()
					+ run);
		tempPerf.createNewFile();
		FileWriter wr = new FileWriter(tempPerf);
		BufferedWriter buf = new BufferedWriter(wr);

		System.out.println("Average episode elite scores:");
		for (float perf : episodeAverage) {
			buf.write(perf + "\n");
			System.out.println(perf);
		}

		buf.close();
		wr.close();
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
	 * Compiles the performance files togetrher into a single file, detailing
	 * the average, min and max performances.
	 * 
	 * @param runs
	 *            The number of runs involved in the experiment.
	 */
	private void compilePerformanceAverage(int runs) throws Exception {
		double[][] performances = new double[maxEpisodes_][runs];
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
			float val = 0;
			for (int e = 0; e < maxEpisodes_; e++) {
				// Some performance files may be cut off, so just use the last
				// recorded value.
				String input = buf.readLine();
				if (input != null)
					val = Float.parseFloat(input);
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
	 * The main method.
	 * 
	 * @param args
	 *            the arguments for the program.
	 */
	public static void main(String[] args) {
		LearningController controller = new LearningController(args);

		controller.runExperiment();
		System.exit(0);
	}

	/**
	 * A simple class for binding a policy and a value together in a comparable
	 * format. Also updates internal rule worth for rules within the policy.
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

			updateInternalRuleValues(pol, value);
		}

		/**
		 * Updates the internal rule values for the rules within the policy.
		 * 
		 * @param pol
		 *            The policy with the active rules.
		 * @param value
		 *            The value the policy achieved
		 */
		private void updateInternalRuleValues(Policy pol, float value) {
			for (GuidedRule rule : pol.getFiringRules()) {
				rule.updateInternalValue(value);
			}
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

		@Override
		public String toString() {
			return "Policy Value: " + value_;
		}
	}
}
