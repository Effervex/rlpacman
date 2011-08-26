package cerrla;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.rlcommunity.rlglue.codec.RLGlue;

import relationalFramework.CoveringRelationalPolicy;
import relationalFramework.GoalCondition;
import relationalFramework.ObjectObservations;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.AgentObservations;
import relationalFramework.agentObservations.LocalAgentObservations;
import util.Pair;

/**
 * The class that concerns the running of the experimenter. If using
 * parallelisation, this class controls it.
 * 
 * @author Sam Sarjant
 */
public class CrossEntropyRun {
	/** The singleton instance. */
	private static CrossEntropyRun instance_;

	/** Converged counter (how long the agent has been converged). */
	private int convergedCount_;

	/** Current converged value. */
	private double convergedMean_;

	/** The experiment controller containing various program args. */
	private LearningController experimentController_;

	/** The policy generator for this CE method. */
	private PolicyGenerator policyGenerator_;

	/** The time at which the run started */
	private long runStart_;

	/**
	 * Creates a new cross-entropy run with a given policy generator.
	 * 
	 * @param policyGenerator
	 *            The policy generator for this run.
	 */
	private CrossEntropyRun(PolicyGenerator policyGenerator,
			LearningController experimentController) {
		policyGenerator_ = policyGenerator;
		experimentController_ = experimentController;
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
	 * Checks for modular learning - if the agent needs to learn a module as an
	 * internal goal.
	 */
	private void checkForModularLearning(int maxSteps, int maxEpisodes) {
		PolicyGenerator currentPolicyGenerator = policyGenerator_;

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
						currentPolicyGenerator, gc);
				CrossEntropyRun prelimModuleRun = CrossEntropyRun.newInstance(
						localPolicy, experimentController_);
				prelimModuleRun.beginRun(-1, true, maxSteps, maxEpisodes);
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
				localGenerator = PolicyGenerator.newInstance(
						currentPolicyGenerator, orderGC);
			CrossEntropyRun prelimModuleRun = CrossEntropyRun.newInstance(
					localGenerator, experimentController_);
			prelimModuleRun.beginRun(-1, false, maxSteps, maxEpisodes);
		}

		// TODO Don't need this when PG stops being singleton.
		PolicyGenerator.setInstance(currentPolicyGenerator);
		policyGenerator_ = currentPolicyGenerator;
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

		// Elites is equal to the average number of rules in high mean slots.
		double numElites = Math.max(sumWeightedRuleCount / sumSlotMean,
				sumSlotMean);
		return (int) Math.max(1,
				Math.round(numElites / ProgramArgument.RHO.doubleValue()));
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
	 * @param numElites
	 *            The minimal size of the elites.
	 * @param bestElite
	 *            The best value of the elite samples.
	 * @param worstElite
	 *            the worst value of the elite samples.
	 * 
	 */
	private void estimateETA(double klDivergence, double convergenceValue,
			int run, int maxRuns, long startTime, int numElites,
			float bestElite, float worstElite) {
		long currentTime = System.currentTimeMillis();

		long elapsedTime = currentTime - startTime;
		String elapsed = "Elapsed: "
				+ LearningController.toTimeFormat(elapsedTime);
		String learningElapsed = "Learning elapsed: "
				+ LearningController.toTimeFormat(runStart_);
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
		String elapsed = "Elapsed: "
				+ LearningController.toTimeFormat(testElapsedTime);
		String learningElapsed = "Learning elapsed: "
				+ LearningController.toTimeFormat(runStart_);
		System.out.println(elapsed + ", " + learningElapsed);

		// Test percent with ETA for test
		DecimalFormat formatter = new DecimalFormat("#0.0000");
		String percentStr = formatter.format(100 * percentComplete)
				+ "% test complete.";
		long testRemainingTime = (long) (testElapsedTime / percentComplete - testElapsedTime);
		System.out.println(percentStr + " Remaining "
				+ LearningController.toTimeFormat(testRemainingTime));

		// Experiment percent with ETA for experiment
		long expElapsedTime = System.currentTimeMillis()
				- experimentController_.getExperimentStart();
		long totalRemainingTime = (long) (expElapsedTime / expProg - expElapsedTime);
		String expStr = formatter.format(100 * expProg)
				+ "% experiment complete.";
		System.out.println(expStr + " Remaining "
				+ LearningController.toTimeFormat(totalRemainingTime));
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
	private void preliminaryProcessing(int maxSteps) {
		PolicyGenerator.getInstance().shouldRestart();
		RLGlue.RL_agent_message("GetPolicy");
		RLGlue.RL_episode(maxSteps);
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

	/**
	 * Saves the elite policies to file.
	 * 
	 * @param elites
	 *            The best policy, in string format.
	 */
	private void saveElitePolicies(Collection<PolicyValue> elites)
			throws Exception {
		FileWriter wr = new FileWriter(experimentController_.getElitesFile());
		BufferedWriter buf = new BufferedWriter(wr);

		LearningController.writeFileHeader(buf);

		if (experimentController_.getComment() != null)
			buf.write(experimentController_.getComment() + "\n");

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
			LearningController.writeFileHeader(buf);

		if (experimentController_.getComment() != null)
			buf.write(experimentController_.getComment() + "\n");

		PolicyGenerator.getInstance().saveHumanGenerators(buf);
		buf.write("\n\n");
		PolicyGenerator.getInstance().saveGenerators(buf, perfFile.getPath());
		int lastKey = episodeMeans.lastKey();
		buf.write("\n\n" + lastKey + "\t" + episodeMeans.get(lastKey) + "\n");
		buf.write("\n\n\n");

		if (finalWrite) {
			buf.write(LearningController.END_PERFORMANCE + "\n");
			buf.write("Total run time: "
					+ LearningController.toTimeFormat(System
							.currentTimeMillis() - runStart_));
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
	 * @param episodeSDs
	 *            The previous episode SDs mapped by episode number.
	 * @param pvs
	 *            The elite policy values.
	 * @param finiteNum
	 *            The maximum number of iterations to learn in.
	 * @param t
	 *            The current progress of the iterations.
	 * @param maxSteps
	 *            The maximum number of steps per episode.
	 */
	private void testRecordAgent(PolicyGenerator localPolicy, int run,
			SortedMap<Integer, Double> episodeMeans,
			SortedMap<Integer, Double> episodeSDs, SortedSet<PolicyValue> pvs,
			int finiteNum, int t, int maxSteps) {
		// Test the agent and record the performances
		double expProg = ((1.0 * (t + 1)) / finiteNum + (1.0 * (run - experimentController_
				.getRepetitionsStart())))
				/ (experimentController_.getRepetitionsEnd() - experimentController_
						.getRepetitionsStart());
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
				RLGlue.RL_episode(maxSteps);
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
	protected void saveFiles(int run, SortedMap<Integer, Double> episodeMeans,
			SortedMap<Integer, Double> episodeSDs, SortedSet<PolicyValue> pvs,
			boolean serialiseOnly, boolean finalWrite) throws Exception {
		File tempPerf = null;
		if (PolicyGenerator.getInstance().isModuleGenerator()) {
			File modTemps = new File(Module.MODULE_DIR + File.separatorChar
					+ LearningController.TEMP_FOLDER + File.separatorChar);
			modTemps.mkdirs();
			tempPerf = new File(modTemps, PolicyGenerator.getInstance()
					.getLocalGoal()
					+ experimentController_.getPerformanceFile().getName());
		} else {
			LearningController.TEMP_FOLDER.mkdir();
			tempPerf = new File(LearningController.TEMP_FOLDER,
					experimentController_.getPerformanceFile().getName() + run);
		}

		// Remove any old file if this is the first run
		if (episodeMeans.size() <= 1
				&& experimentController_.getSerializedFile() == null)
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
	protected boolean updateDistributions(PolicyGenerator localPolicy,
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
	 * Begins a learning run. This loops the agent and the environment in the
	 * goal of locating the best policy.
	 * 
	 * @param run
	 *            The run number of this run (for output and random seed
	 *            purposes).
	 * @param prelimRun
	 *            If only running the experiment long enough to perform a single
	 *            update.
	 * @param maxSteps
	 *            The maximum number of steps per episode.
	 * @param maxEpisodes
	 *            The maximum number of episodes per run.
	 */
	public PolicyValue beginRun(int run, boolean prelimRun, int maxSteps,
			int maxEpisodes) {
		// Run the preliminary action discovery phase, only to create an initial
		// number of rules.
		if (experimentController_.getSerializedFile() == null)
			preliminaryProcessing(maxSteps);

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
		while ((policyGenerator_.getPoliciesEvaluated() < finiteNum)
				&& !isConverged && !ProgramArgument.TESTING.booleanValue()) {
			// Clear any 'waiting' flags
			PolicyGenerator.getInstance().shouldRestart();
			RLGlue.RL_agent_message("GetPolicy");

			boolean oldAOSettled = AgentObservations.getInstance().isSettled();
			if (ProgramArgument.USE_MODULES.booleanValue() && oldAOSettled
					&& !PolicyGenerator.getInstance().isModuleGenerator()) {
				// Check if the agent needs to drop into learning a module
				checkForModularLearning(maxSteps, maxEpisodes);
			}

			// Determine the dynamic population, based on rule-base size
			int population = determinePopulation(policyGenerator_);
			int numElites = (int) Math.ceil(ProgramArgument.RHO.doubleValue()
					* population);
			finiteNum = maxEpisodes * population;
			if (maxEpisodes < 0)
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
				RLGlue.RL_episode(maxSteps);
				if (policyGenerator_.isModuleGenerator())
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
				if (policyGenerator_.shouldRestart()) {
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
				// System.out.println(policy.objA_);
				if (ProgramArgument.ENSEMBLE_EVALUATION.booleanValue())
					System.out.println("Policy consistency: " + policy.objB_);
				System.out.println(numEpisodes + ": " + score);

				float worst = (!pvs.isEmpty()) ? pvs.last().getValue()
						: Float.NaN;
				PolicyValue thisPolicy = new PolicyValue(policy.objA_, score,
						policyGenerator_.getPoliciesEvaluated());
				pvs.add(thisPolicy);
				policyGenerator_.incrementPoliciesEvaluated();

				if (worst == Float.NaN)
					worst = pvs.first().getValue();

				// Give an ETA
				int repetitionsStart = experimentController_
						.getRepetitionsStart();
				int repetitionsEnd = experimentController_.getRepetitionsEnd();
				estimateETA(policyGenerator_.getKLDivergence(),
						policyGenerator_.getConvergenceValue(), run
								- repetitionsStart, repetitionsEnd
								- repetitionsStart,
						experimentController_.getExperimentStart(), numElites,
						pvs.first().getValue(), worst);

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
				boolean clearElites = updateDistributions(policyGenerator_,
						pvs, population, numElites, minReward);

				// If elites are all the same (within an interval)
				isElitesConverged(eliteAverageValues, pvs, numElites);

				hasUpdated = policyGenerator_.getConvergenceValue() != PolicyGenerator.NO_UPDATES_CONVERGENCE;

				// Write generators
				if ((AgentObservations.getInstance().isSettled() && !oldAOSettled)
						|| policyGenerator_.getPoliciesEvaluated()
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

				isConverged |= policyGenerator_.isConverged();

				System.out.println();
				System.out.println();
			} else {
				filterPolicyValues(pvs, policyGenerator_);
				policyGenerator_.setPoliciesEvaluated(pvs.size());
			}
		}

		// Perform a final test
		if (!prelimRun)
			testRecordAgent(policyGenerator_, run, episodeMeans, episodeSDs,
					pvs, finiteNum, numEpisodes, maxSteps);
		if (ProgramArgument.TESTING.booleanValue())
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
	 * Gets the current cross-entropy run.
	 * 
	 * @return The current cross-entropy run.
	 */
	public static CrossEntropyRun getInstance() {
		if (instance_ == null)
			throw new NullPointerException(
					"Cross-Entropy Run not instantiated yet!");
		return instance_;
	}

	/**
	 * Creates a new cross-entropy run with a given policy generator.
	 * 
	 * @param policyGenenerator
	 *            The local policy generator.
	 * @return The {@link CrossEntropyRun} created.
	 */
	public static CrossEntropyRun newInstance(PolicyGenerator policyGenerator,
			LearningController experimentController) {
		instance_ = new CrossEntropyRun(policyGenerator, experimentController);
		return instance_;
	}
}