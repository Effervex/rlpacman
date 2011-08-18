package cerrla;

import relationalFramework.GoalCondition;
import relationalFramework.ObjectObservations;
import relationalFramework.CoveringRelationalPolicy;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.rlcommunity.rlglue.codec.RLGlue;

import relationalFramework.agentObservations.AgentObservations;
import relationalFramework.agentObservations.LocalAgentObservations;
import util.Pair;

/**
 * The cross entropy algorithm implementation.
 * 
 * @author Sam Sarjant
 */
public class LearningController {
	/** The folder to store the temp files. */
	private static final File TEMP_FOLDER = new File("temp"
			+ File.separatorChar);

	/** The marker for the end of a successfully completed performance file. */
	public static final String END_PERFORMANCE = "<--END-->";
	/** The internal prefix for messages to the agent regarding internal goal. */
	public static final String INTERNAL_PREFIX = "internal";
	/**
	 * An optional comment to append to the beginning of performance and elite
	 * files.
	 */
	private String comment_;
	/** Converged counter (how long the agent has been converged). */
	private int convergedCount_;
	/** Current converged value. */
	private double convergedMean_;

	/** The time that the experiment started. */
	private long experimentStart_;
	/** The extra arguments to message the environment. */
	private String[] extraArgs_;
	/** The amount of time the experiment has taken, excluding testing. */
	private long learningRunTime_ = 0;
	/** The time at which the learning started */
	private long learningStartTime_;
	/** The number of episodes to run. */
	private int maxEpisodes_;
	/** The maximum number of steps the agent can take. */
	private int maxSteps_;
	/** If the performance is saved by episode or by CE iteration. */
	private boolean performanceByEpisode_ = true;
	/** The performance output file. */
	private File performanceFile_;
	/** The best policy found output file. */
	private File elitesFile_;
	/** The last run number. */
	private int repetitionsEnd_ = 1;
	/** The first run number. */
	private int repetitionsStart_ = 0;
	/** If optiona rules are seeded from file. */
	private File ruleFile_;
	/** The time the run started. */
	private long runStart_;

	/** The loaded serializable file. */
	private File serializedFile_;
	/**
	 * If the agent is doing no learning - just testing. Works with serialised
	 * files.
	 */
	private boolean testing_ = false;

	/**
	 * A constructor for initialising the cross-entropy generators and
	 * experiment parameters from an argument file.
	 * 
	 * @param argumentFile
	 *            The file containing the arguments.
	 */
	public LearningController(String[] args) {
		File argumentFile = new File(args[0]);
		ProgramArgument.loadArgs();
		ProgramArgument.saveArgsFile();

		// Read the arguments in from file.
		try {
			FileReader reader = new FileReader(argumentFile);
			BufferedReader bf = new BufferedReader(reader);

			String environmentClass = bf.readLine();
			String[] repetitionsStr = bf.readLine().split("-");
			// Num repetitions specified by a range for seeding random number.
			Integer repetitionsStart = 0;
			Integer repetitionsEnd = Integer.parseInt(repetitionsStr[0]);
			if (repetitionsStr.length == 2) {
				repetitionsStart = repetitionsEnd;
				repetitionsEnd = Integer.parseInt(repetitionsStr[1]);
			}
			Integer episodes = Integer.parseInt(bf.readLine());
			String elitesFile = bf.readLine();
			String performanceFile = bf.readLine();

			ArrayList<String> extraArgsList = new ArrayList<String>();
			String extraArgs = bf.readLine();
			if (extraArgs != null) {
				Pattern p = Pattern.compile("((\".+?\")|\\w+)");
				Matcher m = p.matcher(extraArgs);
				while (m.find()) {
					String extraArg = m.group().replaceAll("\"", "");
					// Performance file comment
					if (extraArg.charAt(0) == '%') {
						comment_ = extraArg;
						System.out.println(comment_);
					} else
						extraArgsList.add(extraArg);
				}
			}

			bf.close();
			reader.close();

			initialise(environmentClass, repetitionsStart, repetitionsEnd,
					episodes, elitesFile, performanceFile,
					extraArgsList.toArray(new String[extraArgsList.size()]));

			for (int i = 1; i < args.length; i++) {
				if (args[i].equals("-d"))
					// Enable debug mode
					PolicyGenerator.debugMode_ = true;
				else if (args[i].equals("-e"))
					// Set the environment to experiment mode
					RLGlue.RL_env_message("-e");
				else if (args[i].equals("-m")) {
					Module.saveAtEnd_ = true;
				} else if (args[i].equals("-s")) {
					i++;
					serializedFile_ = new File(args[i]);
				} else if (args[i].equals("-t") || args[i].equals("-test")) {
					testing_ = true;
				} else if (args[i].equals("-ruleFile")) {
					i++;
					ruleFile_ = new File(args[i]);
				} else {
					// Handle the argument
					i = ProgramArgument.handleArg(i, args);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks if the algorithm has converged in regards to the average
	 * performance.
	 * 
	 * @param episodeMeans
	 *            The episode means for the run.
	 * @param episodeSDs
	 *            The episode standard deviations for the run.
	 * @param valueQueue
	 *            The mean scores for the last N policies.
	 * @param averageValues
	 *            The adjusted-to-0 scores for the last N policies.
	 * @param numEpisodes
	 *            The number of episodes passed.
	 * @param population
	 *            The number of samples.
	 * @return True if the algorithm is considered converged.
	 */
	private boolean checkConvergenceValues(
			SortedMap<Integer, Double> episodeMeans,
			SortedMap<Integer, Double> episodeSDs, Queue<Double> valueQueue,
			Queue<Double> averageValues, int numEpisodes, int population) {
		if (valueQueue.size() == ProgramArgument.PERFORMANCE_TESTING_SIZE
				.intValue()) {
			// Transform the queues into arrays
			double[] vals = new double[ProgramArgument.PERFORMANCE_TESTING_SIZE
					.intValue()];
			int i = 0;
			for (Double val : valueQueue)
				vals[i++] = val.doubleValue();
			double[] envSDs = new double[averageValues.size()];
			i = 0;
			for (Double envSD : averageValues)
				envSDs[i++] = envSD.doubleValue();

			Mean m = new Mean();
			StandardDeviation sd = new StandardDeviation();
			double mean = m.evaluate(vals);
			double meanDeviation = sd.evaluate(envSDs);
			// double meanDeviation = (maxReward - minReward) * 0.1;

			if (Math.abs(mean - convergedMean_) > meanDeviation) {
				convergedMean_ = mean;
				convergedCount_ = -1;
			}
			convergedCount_++;
			int convergedSteps = population
					* ProgramArgument.NUM_PERFORMANCES_CONVERGED.intValue();
			episodeMeans.put(numEpisodes, mean);
			episodeSDs.put(numEpisodes, sd.evaluate(vals));

			DecimalFormat formatter = new DecimalFormat("#0.00");
			System.out.println(formatter.format(convergedCount_ * 100.0
					/ convergedSteps)
					+ "% converged at value: "
					+ formatter.format(mean)
					+ " \u00b1 " + meanDeviation);

			if (convergedCount_ > convergedSteps
					&& ProgramArgument.PERFORMANCE_CONVERGENCE.booleanValue()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks the files for pre-existing versions so runs do not have to be
	 * re-run.
	 * 
	 * @return The run number that the files stopped at and the point at which
	 *         the experiment stopped.
	 */
	private int[] checkFiles(int startPoint) {
		// Check the performance files
		int[] result = new int[2];
		// Find the last file created
		int run = startPoint;
		result[0] = run;
		File lastPerf = null;
		File tempPerf = new File(TEMP_FOLDER + "/" + performanceFile_.getName()
				+ run);
		while (tempPerf.exists()) {
			run++;
			lastPerf = tempPerf;
			tempPerf = new File(TEMP_FOLDER + "/" + performanceFile_.getName()
					+ run);
		}

		// If there aren't any performance files, return 0,0
		if (lastPerf == null)
			return result;

		// Otherwise, scan the last file for how far in it got through
		try {
			FileReader reader = new FileReader(lastPerf);
			BufferedReader br = new BufferedReader(reader);
			int iteration = -1;
			String input = null;
			// Read lines until end performance marker, or null lines.
			while ((input = br.readLine()) != null) {
				if (input.equals(END_PERFORMANCE)) {
					result[0] = run;
					result[1] = -1;
					return result;
				}

				// If the value is a number, increment iteration
				try {
					String[] split = input.split("\t");
					if (split.length == 2) {
						Integer.parseInt(split[0]);
						Double.parseDouble(split[1]);
						iteration++;
					}
				} catch (Exception e) {
				}
			}

			result[0] = run - 1;
			result[1] = iteration;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Checks for modular learning - if the agent needs to learn a module as an
	 * internal goal.
	 * 
	 * @param policyGenerator
	 *            The current policy generator.
	 */
	private void checkForModularLearning(PolicyGenerator policyGenerator) {
		// Get the goal conditions from the local agent observations
		Collection<GoalCondition> goalConditions = AgentObservations
				.getInstance().getLocalSpecificGoalConditions();

		// Find out which modules already exist (remove any goal conditions that
		// already exist)
		Collection<GoalCondition> newGConds = new HashSet<GoalCondition>(
				goalConditions);
		for (GoalCondition gc : goalConditions) {
			// Be sure to check that the module being learned isn't the goal
			// being learned.
			if (Module.moduleExists(StateSpec.getInstance()
					.getEnvironmentName(), gc.toString())
					|| gc.toString().equals(
							PolicyGenerator.getInstance().getLocalGoal()))
				newGConds.remove(gc);
		}

		// Run a preliminary test on each to determine which module has the
		// least further goal conditions (relies on the least other modules to
		// be created).
		SortedMap<Double, GoalCondition> orderedModules = new TreeMap<Double, GoalCondition>();
		for (GoalCondition gc : newGConds) {
			if (!LocalAgentObservations.observationsExist(gc.toString())) {
				System.out.println("\n\n\n------PRELIMINARY MODULE RUN: "
						+ gc.toString() + "------\n\n\n");
				PolicyGenerator localPolicy = PolicyGenerator.newInstance(
						policyGenerator, gc);
				developPolicy(localPolicy, -1, true);
			}

			// Determine how many local goal conditions this module relies
			// on.
			Collection<GoalCondition> moduleGConds = AgentObservations
					.getInstance().getLocalSpecificGoalConditions();
			double size = moduleGConds.size();
			while (orderedModules.containsKey(size))
				size += 0.01;
			orderedModules.put(size, gc);
		}

		// Create each module in order from least reliant to most
		for (GoalCondition orderGC : orderedModules.values()) {
			System.out.println("\n\n\n------LEARNING MODULE: "
					+ orderGC.toString() + "------\n\n\n");
			if (PolicyGenerator.debugMode_) {
				try {
					System.out.println("Press Enter to continue.");
					System.in.read();
					System.in.read();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			PolicyGenerator localGenerator = Module.loadGenerator(orderGC);
			if (localGenerator == null)
				localGenerator = PolicyGenerator.newInstance(policyGenerator,
						orderGC);
			developPolicy(localGenerator, -1, false);
		}

		PolicyGenerator.setInstance(policyGenerator);
	}

	/**
	 * Compiles the performance files together into a single file, detailing the
	 * average, min and max performances.
	 * 
	 * @param runEnd
	 *            The last run.
	 * @param byEpisode
	 *            If the performances are being combined by episode (in
	 *            intervals) or by regular CE interval.
	 */
	private void combineTempFiles(int runEnd, boolean byEpisode)
			throws Exception {
		List<List<Float>> performances = new ArrayList<List<Float>>();
		float min = Float.MAX_VALUE;
		int minRun = -1;
		float max = -Float.MAX_VALUE;
		int maxRun = -1;

		if (!performanceFile_.exists())
			performanceFile_.createNewFile();
		// For every performance file
		for (int i = 0; i < runEnd; i++) {
			File tempPerf = new File(TEMP_FOLDER + "/" + performanceFile_ + i);
			if (!PerformanceReader.readPerformanceFile(tempPerf, true)) {
				System.err.println("Error reading performance file.");
				return;
			}

			List<Float> thisRunPerformances = new ArrayList<Float>();
			performances.add(thisRunPerformances);

			// Run through the performances and place them in the matrix
			SortedMap<Integer, Float> runPerformances = PerformanceReader
					.getPerformanceArray();
			if (byEpisode) {
				Iterator<Integer> iter = runPerformances.keySet().iterator();
				Integer current = iter.next();
				Integer previous = null;
				int currentKeyframeEpisode = 0;
				// Run through the performances, using linear interpolation to
				// get estimates of the performance at a given interval.
				float interpolated = 0;
				do {
					// If the current segment is further along than the current
					// value, advance to the next value.
					while (currentKeyframeEpisode > current) {
						previous = current;
						if (iter.hasNext())
							current = iter.next();
						else
							break;
					}

					// If the keyframe isn't up to the first episode, just use
					// the current value
					if (previous == null) {
						// Add to the previous value.
						thisRunPerformances.add(runPerformances.get(current));
					} else {
						// Interpolate from the previous value to the current
						// one.
						interpolated = 0;
						if (previous == current) {
							interpolated = runPerformances.get(current);
						} else {
							float prevVal = (previous == 0) ? 0
									: runPerformances.get(previous);
							float currentVal = runPerformances.get(current);
							interpolated = (currentVal - prevVal)
									* (1f * (currentKeyframeEpisode - previous) / (current - previous))
									+ prevVal;
						}

						// Add to the performances
						thisRunPerformances.add(interpolated);
					}

					// To the next increment
					currentKeyframeEpisode += ProgramArgument.PERFORMANCE_EPISODE_GAP
							.intValue();
				} while (currentKeyframeEpisode <= runPerformances.lastKey());
				thisRunPerformances.add(runPerformances.get(current));
				System.out.println(thisRunPerformances.get(thisRunPerformances
						.size() - 1));
			} else {
				// Take the values directly from the run performances
				for (Integer key : runPerformances.keySet()) {
					thisRunPerformances.add(runPerformances.get(key));
				}
			}

			// Find min or max runs
			float runVal = runPerformances.get(runPerformances.lastKey());
			if (runVal < min) {
				min = runVal;
				minRun = i;
			}
			if (runVal > max) {
				max = runVal;
				maxRun = i;
			}
		}

		// Calculate the average and print out the stats
		FileWriter writer = new FileWriter(performanceFile_);
		BufferedWriter buf = new BufferedWriter(writer);
		writeFileHeader(buf);

		buf.write("Episode\tAverage\tSD\tMin\tMax\n");
		boolean moreEpisodes = true;
		int index = 0;
		while (moreEpisodes) {
			moreEpisodes = false;
			// Compile the array of performances for the given index
			double[] performanceArray = new double[performances.size()];
			double maxVal = 0;
			double minVal = 0;
			for (int run = 0; run < performanceArray.length; run++) {
				List<Float> runPerformanceList = performances.get(run);
				int thisIndex = Math.min(index, runPerformanceList.size() - 1);
				if (index < runPerformanceList.size())
					moreEpisodes = true;
				performanceArray[run] = runPerformanceList.get(thisIndex);

				// Max and min
				if (run == minRun)
					minVal = performanceArray[run];
				if (run == maxRun)
					maxVal = performanceArray[run];
			}

			// Find the statistics
			Mean mean = new Mean();
			StandardDeviation sd = new StandardDeviation();
			int episodeNum = (int) ((byEpisode) ? index
					* ProgramArgument.PERFORMANCE_EPISODE_GAP.doubleValue()
					: index + 1);
			buf.write(episodeNum + "\t" + mean.evaluate(performanceArray)
					+ "\t" + sd.evaluate(performanceArray) + "\t" + minVal
					+ "\t" + maxVal + "\n");
			index++;
		}

		buf.write("Total Run Time: "
				+ toTimeFormat(System.currentTimeMillis() - experimentStart_)
				+ "\n");
		buf.write("Total Learning Time: " + toTimeFormat(learningRunTime_)
				+ "\n");

		buf.close();
		writer.close();
	}

	/**
	 * Determines the population (N) of rules to use for optimisation.
	 * 
	 * @param policyGenerator
	 *            The policy generator to determine the populations from.
	 * @return A population of rules, large enough to reasonably test most
	 *         combinations of rules.
	 */
	private int determinePopulation(PolicyGenerator policyGenerator) {

		// If the generator is just a slot optimiser, use 50 * slot number
		// if (policyGenerator.isSlotOptimiser()) {
		// return (int) (POPULATION_CONSTANT * policyGenerator.getGenerator()
		// .size());
		// }
		//
		// double sumSlot = 0;
		// double maxSlotMean = 0;
		// for (Slot slot : policyGenerator.getGenerator()) {
		// double weight = slot.getSelectionProbability();
		// if (weight > 1)
		// weight = 1;
		// if (weight > maxSlotMean)
		// maxSlotMean = weight;
		// sumSlot += (slot.size() * weight);
		// }
		// sumSlot /= maxSlotMean;
		// return (int) (POPULATION_CONSTANT * (sumSlot / policyGenerator
		// .getGenerator().size()));
		//

		// N_E = Max(average # rules in high mu(S) slots, Sum mu(S))
		double maxWeightedRuleCount = 0;
		double maxSlotMean = 0;
		double sumWeightedRuleCount = 0;
		double sumSlotMean = 0;
		for (Slot slot : policyGenerator.getGenerator()) {
			double weight = slot.getSelectionProbability();
			if (weight > 1)
				weight = 1;
			if (weight > maxSlotMean)
				maxSlotMean = weight;
			sumSlotMean += weight;
			// Use klSize to determine the skew of the slot size
			double klSize = slot.klSize();
			double weightedRuleCount = klSize * weight;
			sumWeightedRuleCount += weightedRuleCount;
			if (weightedRuleCount > maxWeightedRuleCount)
				maxWeightedRuleCount = weightedRuleCount;
		}

		// Always have a minimum of |D_S| elite samples.
		// double numElites =
		// maxWeightedRuleCount / maxSlotMean;

		// Elites is equal to the average number of rules in high mean slots.
		double numElites = Math.max(sumWeightedRuleCount / sumSlotMean,
				sumSlotMean);
		// System.out.println("POPOPOPOPOPOPOPOPOPOPOP: " + numElites);
		// System.out.println("SLOTS: " +
		// policyGenerator.getGenerator().size());
		return (int) Math.max(1,
				Math.round(numElites / ProgramArgument.RHO.doubleValue()));

		// N_E = Max(# specialisations)
		// int maxSpecialisations = 0;
		// for (String action : StateSpec.getInstance().getActions().keySet()) {
		// maxSpecialisations = Math.max(PolicyGenerator.getInstance()
		// .getNumSpecialisations(action), maxSpecialisations);
		// }
		// return (int) (maxSpecialisations / SELECTION_RATIO);
	}

	/**
	 * The policy optimisation loop, which runs through the environment until
	 * the agent has developed a reasonable converged policy.
	 * 
	 * @param localPolicy
	 *            The local policy to develop.
	 * @param run
	 *            The run number of the policy.
	 * @param prelimRun
	 *            If the algorithm is only running the policy generator through
	 *            a small number of episodes (N_E).
	 * @return The best policy from the elites.
	 */
	private PolicyValue developPolicy(PolicyGenerator localPolicy, int run,
			boolean prelimRun) {
		// Run the preliminary action discovery phase, only to create an initial
		// number of rules.
		if (serializedFile_ == null)
			preliminaryProcessing();

		convergedCount_ = 0;
		convergedMean_ = Double.MAX_VALUE;

		// The performance and convergence measures.
		SortedMap<Integer, Double> episodeMeans = new TreeMap<Integer, Double>();
		SortedMap<Integer, Double> episodeSDs = new TreeMap<Integer, Double>();
		Queue<Double> valueQueue = new LinkedList<Double>();
		Queue<Double> averageValues = new LinkedList<Double>();
		Queue<Double> eliteAverageValues = new LinkedList<Double>();

		// Forming a population of solutions
		SortedSet<PolicyValue> pvs = new TreeSet<PolicyValue>();

		// Learn for a finite number of episodes, or until it is converged.
		int finiteNum = Integer.MAX_VALUE;

		// Noting min/max rewards
		double maxReward = -(Float.MAX_VALUE - 1);
		double minReward = Float.MAX_VALUE;

		int numEpisodes = 0;
		boolean isConverged = false;
		boolean hasUpdated = false;
		// Test until: finite steps, not converged, not doing just testing
		while ((localPolicy.getPoliciesEvaluated() < finiteNum) && !isConverged
				&& !testing_) {
			// Clear any 'waiting' flags
			PolicyGenerator.getInstance().shouldRestart();
			RLGlue.RL_agent_message("GetPolicy");

			boolean oldAOSettled = AgentObservations.getInstance().isSettled();
			if (ProgramArgument.USE_MODULES.booleanValue() && oldAOSettled
					&& !PolicyGenerator.getInstance().isModuleGenerator()) {
				// Check if the agent needs to drop into learning a module
				checkForModularLearning(localPolicy);
			}

			// Determine the dynamic population, based on rule-base size
			int population = determinePopulation(localPolicy);
			int numElites = (int) Math.ceil(ProgramArgument.RHO.doubleValue()
					* population);
			finiteNum = maxEpisodes_ * population;
			if (maxEpisodes_ < 0)
				finiteNum = Integer.MAX_VALUE;

			// Test the policy against the environment a number of times.
			boolean restart = false;
			float score = 0;
			double minScore = Double.MAX_VALUE;
			double[] scores = new double[ProgramArgument.POLICY_REPEATS
					.intValue()];
			for (int j = 0; j < ProgramArgument.POLICY_REPEATS.intValue(); j++) {
				double scoreThisIter = 0;
				numEpisodes++;
				RLGlue.RL_episode(maxSteps_);
				if (localPolicy.isModuleGenerator())
					scoreThisIter = Double.parseDouble(RLGlue
							.RL_agent_message("internalReward"));
				else
					scoreThisIter = RLGlue.RL_return();
				maxReward = (scoreThisIter > maxReward) ? scoreThisIter
						: maxReward;
				minReward = (scoreThisIter < minReward) ? scoreThisIter
						: minReward;
				score += scoreThisIter;
				scores[j] = scoreThisIter;
				minScore = (scoreThisIter < minScore) ? scoreThisIter
						: minScore;

				// Check for a restart
				if (localPolicy.shouldRestart()) {
					restart = true;
					break;
				}
			}

			if (!restart) {
				// Storing the policy value.
				RLGlue.RL_agent_message("GetPolicy");
				@SuppressWarnings("unchecked")
				Pair<CoveringRelationalPolicy, Double> policy = (Pair<CoveringRelationalPolicy, Double>) ObjectObservations
						.getInstance().objectArray[0];
				score /= ProgramArgument.POLICY_REPEATS.doubleValue();
				System.out.println();
				//System.out.println(policy.objA_);
				if (ProgramArgument.ENSEMBLE_EVALUATION.booleanValue())
					System.out.println("Policy consistency: " + policy.objB_);
				System.out.println(numEpisodes + ": " + score);

				float worst = (!pvs.isEmpty()) ? pvs.last().getValue()
						: Float.NaN;
				PolicyValue thisPolicy = new PolicyValue(policy.objA_, score,
						localPolicy.getPoliciesEvaluated());
				pvs.add(thisPolicy);
				localPolicy.incrementPoliciesEvaluated();

				if (worst == Float.NaN)
					worst = pvs.first().getValue();

				// Give an ETA
				estimateETA(localPolicy.getKLDivergence(),
						localPolicy.getConvergenceValue(), run
								- repetitionsStart_, repetitionsEnd_
								- repetitionsStart_, experimentStart_, true,
						numElites, pvs.first().getValue(), worst);

				// Noting averaged performance
				if (valueQueue.size() == ProgramArgument.PERFORMANCE_TESTING_SIZE
						.doubleValue())
					valueQueue.poll();
				valueQueue.offer((double) score);

				// Noting average score (for environment SD)
				for (int i = 0; i < ProgramArgument.POLICY_REPEATS
						.doubleValue(); i++) {
					if (averageValues.size() == ProgramArgument.PERFORMANCE_TESTING_SIZE
							.doubleValue()
							* ProgramArgument.POLICY_REPEATS.doubleValue())
						averageValues.poll();
					averageValues.add(scores[i] - minScore);

					// Noting average elite fluctuation values
					// If elites represent actual elite (not all values)
					if (worst > minReward && score >= worst) {
						if (eliteAverageValues.size() == ProgramArgument.PERFORMANCE_TESTING_SIZE
								.doubleValue()
								* ProgramArgument.POLICY_REPEATS.doubleValue())
							eliteAverageValues.poll();
						eliteAverageValues.add(scores[i] - minScore);
					}
				}
				isConverged |= checkConvergenceValues(episodeMeans, episodeSDs,
						valueQueue, averageValues, numEpisodes, population);

				// Update the distributions
				boolean clearElites = updateDistributions(localPolicy, pvs,
						population, numElites, minReward);

				// If elites are all the same (within an interval)
				isElitesConverged(eliteAverageValues, pvs, numElites);

				hasUpdated = localPolicy.getConvergenceValue() != PolicyGenerator.NO_UPDATES_CONVERGENCE;

				// Write generators
				if ((AgentObservations.getInstance().isSettled() && !oldAOSettled)
						|| localPolicy.getPoliciesEvaluated()
								% ProgramArgument.PERFORMANCE_TESTING_SIZE
										.doubleValue() == 1) {
					try {
						saveFiles(run, episodeMeans, episodeSDs, pvs,
								!hasUpdated, false);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				if (hasUpdated && prelimRun)
					break;

				isConverged |= localPolicy.isConverged();

				System.out.println();
				System.out.println();
			} else {
				filterPolicyValues(pvs, localPolicy);
				localPolicy.setPoliciesEvaluated(pvs.size());
			}
		}

		// Perform a final test
		if (!prelimRun)
			testRecordAgent(localPolicy, run, episodeMeans, episodeSDs, pvs,
					finiteNum, numEpisodes);
		if (testing_)
			return null;

		// Return the best policy. if multiple policies have the same value,
		// return the most common one.
		float threshold = pvs.first().getValue();
		Map<CoveringRelationalPolicy, Integer> bestPolicyMap = new HashMap<CoveringRelationalPolicy, Integer>();
		PolicyValue bestPolicy = null;
		int mostCounts = 0;
		for (PolicyValue pv : pvs) {
			CoveringRelationalPolicy thisPolicy = pv.getPolicy();
			float thisValue = pv.getValue();
			if (thisValue < threshold)
				break;

			// Note the policy count
			Integer count = bestPolicyMap.get(thisPolicy);
			if (count == null)
				count = 0;
			count++;
			bestPolicyMap.put(thisPolicy, count);
			// If count is higher, this is a better policy
			if (count > mostCounts) {
				bestPolicy = pv;
				mostCounts = count;
			}
		}

		if (!prelimRun)
			Module.saveModule(AgentObservations.getInstance()
					.getLocalGoalName(), AgentObservations.getInstance()
					.getNumGoalArgs(), bestPolicy.getPolicy());
		return bestPolicy;
	}

	/**
	 * Prints out the percentage complete, time elapsed and estimated time
	 * remaining.
	 * 
	 * @param klDivergence
	 *            The amount of divergence the generators are experiencing in
	 *            updates.
	 * @param convergenceValue
	 *            The amount of divergence required for convergence to be
	 *            called.
	 * @param run
	 *            The run number.
	 * @param maxRuns
	 *            The total number of runs.
	 * @param startTime
	 *            The time the experiment was started.
	 * @param noteLearningTime
	 *            Whether to note learning time or not.
	 * @param numElites
	 *            The minimal size of the elites.
	 * @param bestElite
	 *            The best value of the elite samples.
	 * @param worstElite
	 *            the worst value of the elite samples.
	 * 
	 */
	private void estimateETA(double klDivergence, double convergenceValue,
			int run, int maxRuns, long startTime, boolean noteLearningTime,
			int numElites, float bestElite, float worstElite) {
		long currentTime = System.currentTimeMillis();
		if (noteLearningTime) {
			learningRunTime_ += currentTime - learningStartTime_;
			learningStartTime_ = currentTime;
		}

		long elapsedTime = currentTime - startTime;
		String elapsed = "Elapsed: " + toTimeFormat(elapsedTime);
		String learningElapsed = "Learning elapsed: "
				+ toTimeFormat(learningRunTime_);
		System.out.println(elapsed + ", " + learningElapsed);

		double convergencePercent = Math
				.min(convergenceValue / klDivergence, 1);
		if (convergenceValue == PolicyGenerator.NO_UPDATES_CONVERGENCE)
			convergencePercent = 0;
		double totalRunComplete = (1.0 * run + convergencePercent) / maxRuns;

		DecimalFormat formatter = new DecimalFormat("#0.0000");
		String modular = "";
		if (PolicyGenerator.getInstance().isModuleGenerator())
			modular = "MODULAR: ["
					+ PolicyGenerator.getInstance().getLocalGoal() + "] ";
		// No updates yet, convergence unknown
		String percentStr = null;
		if (convergenceValue == PolicyGenerator.NO_UPDATES_CONVERGENCE) {
			percentStr = "Unknown convergence; No updates yet.";
		} else {
			percentStr = formatter.format(100 * convergencePercent) + "% "
					+ modular + "converged.";
		}
		String eliteString = "N_E: " + numElites + ", E_best: "
				+ formatter.format(bestElite) + ", E_worst: "
				+ formatter.format(worstElite);
		System.out.println(percentStr + " " + eliteString);

		String totalPercentStr = formatter.format(100 * totalRunComplete)
				+ "% experiment complete.";
		System.out.println(totalPercentStr);
	}

	/**
	 * Estimates the time left for a test to complete.
	 * 
	 * @param percentComplete
	 *            The percentage of test complete.
	 * @param expProg
	 *            The percentage of experiment complete.
	 * @param startTime
	 *            The time the test started.
	 */
	private void estimateTestTime(double percentComplete, double expProg,
			long startTime) {
		// Test time elapsed, with static learning time
		long testElapsedTime = System.currentTimeMillis() - startTime;
		String elapsed = "Elapsed: " + toTimeFormat(testElapsedTime);
		String learningElapsed = "Learning elapsed: "
				+ toTimeFormat(learningRunTime_);
		System.out.println(elapsed + ", " + learningElapsed);

		// Test percent with ETA for test
		DecimalFormat formatter = new DecimalFormat("#0.0000");
		String percentStr = formatter.format(100 * percentComplete)
				+ "% test complete.";
		long testRemainingTime = (long) (testElapsedTime / percentComplete - testElapsedTime);
		System.out.println(percentStr + " Remaining "
				+ toTimeFormat(testRemainingTime));

		// Experiment percent with ETA for experiment
		long expElapsedTime = System.currentTimeMillis() - experimentStart_;
		long totalRemainingTime = (long) (expElapsedTime / expProg - expElapsedTime);
		String expStr = formatter.format(100 * expProg)
				+ "% experiment complete.";
		System.out.println(expStr + " Remaining "
				+ toTimeFormat(totalRemainingTime));
	}

	/**
	 * Filters out any policies containing rules that are no longer in the
	 * policy generator.
	 * 
	 * @param pvs
	 *            The list of policy values.
	 */
	private void filterPolicyValues(Collection<PolicyValue> pvs,
			PolicyGenerator localPolicyGenerator) {
		for (Iterator<PolicyValue> pvIter = pvs.iterator(); pvIter.hasNext();) {
			PolicyValue pv = pvIter.next();
			Collection<RelationalRule> policyRules = pv.getPolicy()
					.getFiringRules();
			boolean remove = false;
			// Check each firing rule in the policy.
			for (RelationalRule gr : policyRules) {
				if (!localPolicyGenerator.contains(gr)) {
					remove = true;
					break;
				}
			}

			// If the policy value was to be removed, remove it
			if (remove) {
				pvIter.remove();
			}
		}
	}

	/**
	 * A constructor for the typical arguments and a randomly generated rule
	 * base.
	 * 
	 * @param environmentClass
	 *            The name of the environment class files.
	 * @param repetitionsStart
	 *            The first run number seed.
	 * @param repetitionsEnd
	 *            The last run number seed.
	 * @param episodeCount
	 *            The number of episodes to perform.
	 * @param elitesFile
	 *            The output file for the best policy.
	 * @param performanceFile
	 *            The output file for the agent's performance.
	 * @param extraArgs
	 *            The extra arguments for the environment to take.
	 */
	private void initialise(String environmentClass, int repetitionsStart,
			int repetitionsEnd, int episodeCount, String elitesFile,
			String performanceFile, String[] extraArgs) {
		repetitionsStart_ = repetitionsStart;
		repetitionsEnd_ = repetitionsEnd;
		maxEpisodes_ = episodeCount;

		elitesFile_ = new File(elitesFile);
		performanceFile_ = new File(performanceFile);

		// Create temp folder
		try {
			if (!elitesFile_.exists())
				elitesFile_.createNewFile();
			TEMP_FOLDER.mkdir();
		} catch (Exception e) {
			e.printStackTrace();
		}
		extraArgs_ = extraArgs;
		String goalArg = null;
		for (String extraArg : extraArgs)
			if (extraArg.startsWith("goal"))
				goalArg = extraArg.substring(5);

		// Initialise the state spec.
		StateSpec.initInstance(environmentClass, goalArg);
	}

	private void isElitesConverged(Queue<Double> eliteAverageValues,
			SortedSet<PolicyValue> pvs, int numElites) {
		StandardDeviation sd = new StandardDeviation();
		double[] eliteAverages = new double[eliteAverageValues.size()];
		int i = 0;
		for (Double val : eliteAverageValues)
			eliteAverages[i++] = val;
		if (eliteAverages.length >= ProgramArgument.PERFORMANCE_TESTING_SIZE
				.doubleValue() * ProgramArgument.POLICY_REPEATS.doubleValue()
				&& pvs.size() >= numElites * 2
				&& pvs.first().getValue() - pvs.last().getValue() <= sd
						.evaluate(eliteAverages)) {
			System.out.println("---------ELITE CONVERGED!-----------");
		}
		if (eliteAverages.length > 1)
			System.out.println("EliteSD:" + sd.evaluate(eliteAverages));
	}

	/**
	 * Cleans the elite policy values up by removing stale policy values.
	 * 
	 * @param pvs
	 *            The policy values list.
	 * @param iteration
	 *            The current iteration.
	 * @param staleValue
	 *            The number of iterations to pass before a policy value becomes
	 *            stale.
	 * @param localPolicy
	 *            The local policy generator.
	 * @return The cleaned list of policy values.
	 */
	private void postUpdateModification(Collection<PolicyValue> pvs,
			int iteration, int staleValue, PolicyGenerator localPolicy) {
		// Remove any stale policies
		for (Iterator<PolicyValue> iter = pvs.iterator(); iter.hasNext();) {
			PolicyValue pv = iter.next();
			if (iteration - pv.getIteration() >= staleValue) {
				localPolicy.retestPolicy(pv.getPolicy());
				iter.remove();
			}
		}
	}

	/**
	 * Run the agent over the environment until we have a single pre-goal and
	 * some rules to work with.
	 */
	private void preliminaryProcessing() {
		PolicyGenerator.getInstance().shouldRestart();
		RLGlue.RL_agent_message("GetPolicy");
		RLGlue.RL_episode(maxSteps_);
	}

	/**
	 * Modifies the policy values before updating (cutting the values down to
	 * size).
	 * 
	 * @param elites
	 *            The policy values to modify.
	 * @param numElite
	 *            The number of elite samples to use when updating. The size
	 *            policy values should be.
	 * @return The policy values that were removed.
	 */
	private SortedSet<PolicyValue> preUpdateModification(
			SortedSet<PolicyValue> elites, int numElite) {
		// Remove any values worse than the value at numElites
		SortedSet<PolicyValue> tailSet = null;
		if (elites.size() > numElite) {
			// Find the N_E value
			Iterator<PolicyValue> pvIter = elites.iterator();
			PolicyValue currentPV = null;
			for (int i = 0; i < numElite; i++)
				currentPV = pvIter.next();
			PolicyValue neValue = currentPV;
			// Iter at N_E value. Remove any values less than N_E's value
			do {
				if (pvIter.hasNext())
					currentPV = pvIter.next();
				else
					currentPV = null;
			} while (currentPV != null
					&& currentPV.getValue() == neValue.getValue());
			// Remove the tail set
			if (currentPV != null) {
				tailSet = new TreeSet<PolicyValue>(elites.tailSet(currentPV));
				elites.removeAll(tailSet);
			}
		}

		return tailSet;
	}

	@SuppressWarnings("unused")
	private void printRuleWorths(PolicyGenerator localPolicy) {
		System.out.println("\tRULE WORTHS");
		Comparator<RelationalRule> comp = new Comparator<RelationalRule>() {

			@Override
			public int compare(RelationalRule o1, RelationalRule o2) {
				// Bigger is better
				if (o1.getInternalMean() > o2.getInternalMean())
					return -1;
				if (o1.getInternalMean() < o2.getInternalMean())
					return 1;
				// Smaller SD is better
				if (o1.getInternalSD() < o2.getInternalSD())
					return -1;
				if (o1.getInternalSD() > o2.getInternalSD())
					return 1;
				// Else, just return a hashcode equality relation
				if (o1.hashCode() < o2.hashCode())
					return -1;
				if (o1.hashCode() > o2.hashCode())
					return 1;
				return 0;
			}
		};
		SortedSet<RelationalRule> sortedRules = new TreeSet<RelationalRule>(
				comp);
		for (Slot slot : localPolicy.getGenerator()) {
			for (RelationalRule rule : slot.getGenerator()) {
				sortedRules.add(rule);
			}
		}

		for (RelationalRule rule : sortedRules)
			System.out.println(rule.toNiceString() + ": "
					+ rule.getInternalMean() + ((char) 177)
					+ rule.getInternalSD());
	}

	/**
	 * Saves the elite policies to file.
	 * 
	 * @param elites
	 *            The best policy, in string format.
	 */
	private void saveElitePolicies(Collection<PolicyValue> elites)
			throws Exception {
		FileWriter wr = new FileWriter(elitesFile_);
		BufferedWriter buf = new BufferedWriter(wr);

		writeFileHeader(buf);

		if (comment_ != null)
			buf.write(comment_ + "\n");

		for (PolicyValue pv : elites) {
			buf.write(pv.getPolicy().toOnlyUsedString() + "\n");
			buf.write(pv.getValue() + "\n\n");
		}

		buf.close();
		wr.close();
	}

	/**
	 * Saves the performance to file and outputs them. Saves to two files: One
	 * with a breakdown of the generators, and another with purely episodic
	 * performances.
	 * 
	 * @param episodeMeans
	 *            The saved episode average performances mapped by number of
	 *            episodes passed.
	 * @param finalWrite
	 *            If this write was the final write for the run.
	 */
	private void savePerformance(SortedMap<Integer, Double> episodeMeans,
			SortedMap<Integer, Double> episodeSDs, File perfFile,
			boolean finalWrite) throws Exception {
		if (episodeMeans.isEmpty())
			return;

		// If the file has just been created, add the arguments to the head of
		// the file
		boolean newFile = perfFile.createNewFile();

		FileWriter wr = new FileWriter(perfFile, true);
		BufferedWriter buf = new BufferedWriter(wr);

		// If the file is fresh, add the program args to the top
		if (newFile)
			writeFileHeader(buf);

		if (comment_ != null)
			buf.write(comment_ + "\n");

		PolicyGenerator.getInstance().saveHumanGenerators(buf);
		buf.write("\n\n");
		PolicyGenerator.getInstance().saveGenerators(buf, perfFile.getPath());
		int lastKey = episodeMeans.lastKey();
		buf.write("\n\n" + lastKey + "\t" + episodeMeans.get(lastKey) + "\n");
		buf.write("\n\n\n");

		if (finalWrite) {
			buf.write(END_PERFORMANCE + "\n");
			buf.write("Total run time: "
					+ toTimeFormat(System.currentTimeMillis() - runStart_));
		}

		buf.close();
		wr.close();

		// Writing the raw performance
		File rawNumbers = new File(perfFile.getAbsoluteFile() + "raw");

		wr = new FileWriter(rawNumbers);
		buf = new BufferedWriter(wr);

		System.out.println("Average episode scores:");
		for (Integer episode : episodeMeans.keySet()) {
			buf.write(episode + "\t" + episodeMeans.get(episode) + "\t"
					+ episodeSDs.get(episode) + "\n");
			System.out.println(episode + "\t" + episodeMeans.get(episode)
					+ "\t\u00b1\t" + episodeSDs.get(episode));
		}

		buf.close();
		wr.close();
	}

	/**
	 * Tests and records the agent's progress.
	 * 
	 * @param localPolicy
	 *            The local PolicyGenerator.
	 * @param run
	 *            The current run.
	 * @param episodeMeans
	 *            The previous episode performances mapped by episode number.
	 * @param pvs
	 *            The elite policy values.
	 * @param finiteNum
	 *            The maximum number of iterations to learn in.
	 * @param t
	 *            The current progress of the iterations.
	 */
	private void testRecordAgent(PolicyGenerator localPolicy, int run,
			SortedMap<Integer, Double> episodeMeans,
			SortedMap<Integer, Double> episodeSDs, SortedSet<PolicyValue> pvs,
			int finiteNum, int t) {
		// Test the agent and record the performances
		double expProg = ((1.0 * (t + 1)) / finiteNum + (1.0 * (run - repetitionsStart_)))
				/ (repetitionsEnd_ - repetitionsStart_);
		int numEpisodes = localPolicy.getPoliciesEvaluated();

		// Pre-testing fixing
		localPolicy.freeze(true);
		String oldEnsSize = null;
		if (ProgramArgument.ENSEMBLE_EVALUATION.booleanValue())
			oldEnsSize = RLGlue.RL_agent_message("ensemble "
					+ ProgramArgument.ENSEMBLE_SIZE.intValue());

		// System output
		System.out.println();
		if (!ProgramArgument.ENSEMBLE_EVALUATION.booleanValue())
			System.out.println("Beginning testing for episode " + t + ".");
		else
			System.out.println("Beginning ensemble testing for episode " + t
					+ ".");
		System.out.println();

		long startTime = System.currentTimeMillis();

		// Run the agent through several test iterations, resampling the
		// agent at each step
		double[] scores = new double[ProgramArgument.TEST_ITERATIONS.intValue()];
		for (int i = 0; i < ProgramArgument.TEST_ITERATIONS.intValue(); i++) {
			estimateTestTime(
					(1.0 * i) / ProgramArgument.TEST_ITERATIONS.intValue(),
					expProg, startTime);

			for (int j = 0; j < ProgramArgument.POLICY_REPEATS.intValue(); j++) {
				RLGlue.RL_episode(maxSteps_);
				if (PolicyGenerator.getInstance().isModuleGenerator())
					scores[i] += Double.parseDouble(RLGlue
							.RL_agent_message("internalReward"));
				else
					scores[i] += RLGlue.RL_return();
			}
			scores[i] /= ProgramArgument.POLICY_REPEATS.intValue();

			RLGlue.RL_agent_message("GetPolicy");
			@SuppressWarnings("unchecked")
			Pair<CoveringRelationalPolicy, Double> pol = (Pair<CoveringRelationalPolicy, Double>) ObjectObservations
					.getInstance().objectArray[0];
			System.out.println(pol.objA_);
			if (ProgramArgument.ENSEMBLE_EVALUATION.booleanValue())
				System.out.println("Policy consistency: " + pol.objB_);
			System.out.println(numEpisodes + ": " + scores[i] + "\n");
		}

		// Post-testing unfixing
		localPolicy.freeze(false);
		if (ProgramArgument.ENSEMBLE_EVALUATION.booleanValue())
			RLGlue.RL_agent_message("ensemble " + oldEnsSize);
		learningStartTime_ = System.currentTimeMillis();

		// Episode performance output
		Mean mean = new Mean();
		episodeMeans.put(numEpisodes, mean.evaluate(scores));
		StandardDeviation sd = new StandardDeviation();
		episodeSDs.put(numEpisodes, sd.evaluate(scores));

		// Save the results at each episode
		try {
			saveFiles(run, episodeMeans, episodeSDs, pvs, false, true);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Simple tool for converting long to a string of time.
	 * 
	 * @param time
	 *            The time in millis.
	 * @return A string representing the time.
	 */
	private String toTimeFormat(long time) {
		String timeString = time / (1000 * 60 * 60) + ":"
				+ (time / (1000 * 60)) % 60 + ":" + (time / 1000) % 60;
		return timeString;
	}

	private void writeFileHeader(BufferedWriter buf) throws IOException {
		buf.write("---PROGRAM ARGUMENTS---\n");
		ProgramArgument.saveArgs(buf, true);
		buf.write("-----------------------\n");
		buf.write("GOAL (" + StateSpec.getInstance().getGoalName() + "): "
				+ StateSpec.getInstance().getGoalState() + "\n\n");
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
		learningStartTime_ = experimentStart_;

		// Initialise the environment/agent
		RLGlue.RL_init();
		for (String extraArg : extraArgs_)
			RLGlue.RL_env_message(extraArg);
		maxSteps_ = Integer.parseInt(RLGlue.RL_env_message("maxSteps"));
		System.out.println("Goal: " + StateSpec.getInstance().getGoalState());

		// Determine the initial run (as previous runs may have already been
		// done in a previous experiment)
		int[] startPoint = checkFiles(repetitionsStart_);
		int run = startPoint[0];

		// The ultra-outer loop, for averaging experiment results
		for (; run < repetitionsEnd_; run++) {
			runStart_ = System.currentTimeMillis();
			// Initialise a new policy generator.
			PolicyGenerator localPolicy = null;
			if (serializedFile_ != null)
				localPolicy = PolicyGenerator
						.loadPolicyGenerator(serializedFile_);
			if (localPolicy == null) {
				if (serializedFile_ != null)
					System.err.println("Could not load " + serializedFile_
							+ "\nUsing new policy generator");
				localPolicy = PolicyGenerator.newInstance(run);
			}

			if (ruleFile_ != null)
				localPolicy.seedRules(ruleFile_);
			developPolicy(localPolicy, run, false);

			if (testing_)
				break;

			// Resetting experiment values
			PolicyGenerator.getInstance().resetGenerator();
			AgentObservations.loadAgentObservations();
		}

		Module.saveAllModules();

		RLGlue.RL_cleanup();

		if (repetitionsStart_ == 0 && !testing_) {
			try {
				combineTempFiles(repetitionsEnd_, performanceByEpisode_);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		System.out.println("Total learning time: "
				+ toTimeFormat(learningRunTime_));
	}

	/**
	 * Saves any output files and serialisations required.
	 * 
	 * @param run
	 *            The current run.
	 * @param episodeMeans
	 *            The episode mean performances, sorted by episode number.
	 * @param episodeSDs
	 *            The episode standard deviations, sorted by episode number.
	 * @param pvs
	 *            The current elites.
	 * @param serialiseOnly
	 *            If only serialisation files should be saved.
	 * @param finalWrite
	 *            If this is the final write of the performance file.
	 * @throws Exception
	 *             If something goes awry...
	 */
	public void saveFiles(int run, SortedMap<Integer, Double> episodeMeans,
			SortedMap<Integer, Double> episodeSDs, SortedSet<PolicyValue> pvs,
			boolean serialiseOnly, boolean finalWrite) throws Exception {
		File tempPerf = null;
		if (PolicyGenerator.getInstance().isModuleGenerator()) {
			File modTemps = new File(Module.MODULE_DIR + File.separatorChar
					+ TEMP_FOLDER + File.separatorChar);
			modTemps.mkdirs();
			tempPerf = new File(modTemps, PolicyGenerator.getInstance()
					.getLocalGoal() + performanceFile_.getName());
		} else {
			TEMP_FOLDER.mkdir();
			tempPerf = new File(TEMP_FOLDER, performanceFile_.getName() + run);
		}

		// Remove any old file if this is the first run
		if (episodeMeans.size() <= 1 && serializedFile_ == null)
			tempPerf.delete();

		if (!serialiseOnly) {
			saveElitePolicies(pvs);
			// Output the episode averages
			savePerformance(episodeMeans, episodeSDs, tempPerf, finalWrite);
		}
		PolicyGenerator.getInstance().savePolicyGenerator(
				new File(tempPerf + ".ser"));
		if (!Module.saveAtEnd_)
			AgentObservations.getInstance().saveAgentObservations();
	}

	/**
	 * Updates the distributions using the observed elite samples as a target
	 * distribution to move towards.
	 * 
	 * @param localPolicy
	 *            The policy distribution to update.
	 * @param elites
	 *            The elite samples.
	 * @param population
	 *            The number of steps to take before testing.
	 * @param numElites
	 *            The population value.
	 * @param minReward
	 *            The minimum observed reward.
	 * @return True if the elites should be cleared and restarted.
	 */
	public boolean updateDistributions(PolicyGenerator localPolicy,
			SortedSet<PolicyValue> elites, int population, int numElites,
			double minReward) {
		// Clean up the policy values
		SortedSet<PolicyValue> removed = preUpdateModification(elites,
				numElites);

		localPolicy.updateDistributions(elites,
				ProgramArgument.ALPHA.doubleValue(), population, numElites,
				minReward);
		// Negative updates:
		// localPolicy.updateNegative(elites, STEP_SIZE, removed, minReward);

		postUpdateModification(elites, localPolicy.getPoliciesEvaluated(),
				population, localPolicy);

		// Run the post update operations
		boolean clearElites = localPolicy.postUpdateOperations(numElites);

		// Clear the restart
		localPolicy.shouldRestart();

		return clearElites;
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
}
