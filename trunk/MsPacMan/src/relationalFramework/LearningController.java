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
	/** If this controller is using sliding window learning. */
	private final boolean SLIDING_WINDOW = true;
	/** If this controller is using cross-entrobeam learning. */
	private final boolean ENTROBEAM = true;

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
	/** The internal prefix for messages to the agent regarding internal goal. */
	public static final String INTERNAL_PREFIX = "internal";
	/** The time that the experiment started. */
	private long experimentStart_;
	/** The time at which the learning started */
	private long learningStartTime_;
	/** The amount of time the experiment has taken, excluding testing. */
	private long learningRunTime_ = 0;
	/** The extra arguments to message the environment. */
	private String[] extraArgs_;
	/** The maximum number of steps the agent can take. */
	private int maxSteps_;
	/** The generator file to load. */
	private File loadedGeneratorFile_;

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

			for (int i = 1; i < args.length; i++) {
				if (args[i].equals("-d"))
					// Enable debug mode
					PolicyGenerator.debugMode_ = true;
				else if (args[i].equals("-e"))
					// Set the environment to experiment mode
					RLGlue.RL_env_message("-e");
				else if (args[i].equals("-g")) {
					// Load a generator file
					i++;
					loadedGeneratorFile_ = new File(args[i]);
				}
			}

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
		learningStartTime_ = experimentStart_;

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
			if (loadedGeneratorFile_ != null) {
				localPolicy.loadGenerators(loadedGeneratorFile_);
				localPolicy.freeze(true);
			}

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

		System.out.println("Total learning time: "
				+ toTimeFormat(learningRunTime_));
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
		long runStart = System.currentTimeMillis();
		PolicyValue bestPolicy = null;

		// Run the preliminary action discovery phase, only to create an initial
		// number of rules.
		if (loadedGeneratorFile_ == null)
			preliminaryProcessing();

		// The outer loop, for refinement episode by episode
		ArrayList<Float> episodePerformances = new ArrayList<Float>();
		int t = 0;
		// Forming a population of solutions
		List<PolicyValue> pvs = new ArrayList<PolicyValue>();
		// Learn for a finite number of episodes, or until it is converged.
		int finiteNum = 0;
		// How many steps to wait for testing
		int testingStep = 1;
		if (ENTROBEAM) {
			// TODO Sort this out.
			finiteNum = (int) (maxEpisodes_ / (SELECTION_RATIO * SELECTION_RATIO));
			testingStep = (int) (1 / (SELECTION_RATIO * SELECTION_RATIO));
		} else if (SLIDING_WINDOW) {
			finiteNum = (int) (maxEpisodes_ / SELECTION_RATIO);
			testingStep = (int) (1 / SELECTION_RATIO);
		} else {
			finiteNum = maxEpisodes_;
			testingStep = 1;
		}

		while ((t < finiteNum) && (!localPolicy.isConverged())) {
			if (PolicyGenerator.getInstance().useModules_) {
				// Check if the agent needs to drop into learning a module
				checkForModularLearning(localPolicy);
			}

			int pvsSizeInitial = pvs.size();

			// Determine the dynamic population, based on rule-base size
			int population = determinePopulation(localPolicy);
			// If entrobeam, only get the minimum number of samples.
			if (ENTROBEAM)
				population = (int) (SELECTION_RATIO * population);

			int samples = 0;
			int maxSamples = population;
			if (SLIDING_WINDOW) {
				samples = pvs.size() - pvsSizeInitial;
				maxSamples = population - pvsSizeInitial;
			}
			estimateETA(samples, maxSamples, t, finiteNum, run, repetitions_,
					runStart, true);

			boolean restart = false;
			// Fill the Policy Values list.
			do {
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

					// Check for a restart
					if (localPolicy.shouldRestart()) {
						restart = true;
						break;
					}
				}

				score /= AVERAGE_ITERATIONS;
				System.out.println(score);

				if (restart)
					break;

				pol.parameterArgs(null);
				PolicyValue thisPolicy = new PolicyValue(pol, score, t);
				pvs.add(thisPolicy);
				// Storing the best policy
				if ((bestPolicy == null)
						|| (thisPolicy.getValue() > bestPolicy.getValue()))
					bestPolicy = thisPolicy;

				// Give an ETA
				samples = pvs.size();
				maxSamples = population;
				if (SLIDING_WINDOW) {
					samples = pvs.size() - pvsSizeInitial;
					maxSamples = population - pvsSizeInitial;
				}
				estimateETA(samples, maxSamples, t, finiteNum, run,
						repetitions_, experimentStart_, true);

				// Debug - Looking at rule values
				// printRuleWorths(localPolicy);
			} while (pvs.size() < population);

			if (!restart) {
				Collections.sort(pvs);
				// Update the weights for all distributions using only the elite
				// samples
				int numElite = (int) Math.ceil(population * SELECTION_RATIO);
				double alphaUpdate = 0;
				if (ENTROBEAM) {
					numElite = population;
					// TODO Try to use just sliding update param - this one is
					// REALLY small
					alphaUpdate = STEP_SIZE * SELECTION_RATIO / population;
				} else if (SLIDING_WINDOW)
					alphaUpdate = STEP_SIZE * SELECTION_RATIO;
				else
					alphaUpdate = STEP_SIZE;
				localPolicy.updateDistributions(pvs, numElite, alphaUpdate);

				// Remove the worst policy values
				if (ENTROBEAM) {
					if (pvs.size() > numElite)
						pvs = pvs.subList(0, numElite);
				} else if (SLIDING_WINDOW)
					pvs = pvs.subList(0, pvs.size() - numElite);
				else
					pvs.clear();

				// Only test the agent every number of steps, otherwise more
				// time is spent testing than evaluating. (And at the first and
				// last steps).
				if (((t + 1) % testingStep == 0) || (t == finiteNum - 1)
						|| (t == 0)) {
					// Test the agent and record the performances
					double expProg = ((1.0 * (t + 1)) / finiteNum + (1.0 * run))
							/ repetitions_;
					episodePerformances.add(testAgent(t, maxSteps_, run,
							repetitions_, expProg));

					// Save the results at each episode
					try {
						File tempGen = null;
						if (PolicyGenerator.getInstance().isModuleGenerator())
							tempGen = new File(Module.MODULE_DIR
									+ "/"
									+ TEMP_FOLDER
									+ "/"
									+ PolicyGenerator.getInstance()
											.getModuleName()
									+ generatorFile_.getName());
						else
							tempGen = new File(TEMP_FOLDER + "/"
									+ generatorFile_.getName() + run);
						tempGen.createNewFile();
						PolicyGenerator.getInstance().saveGenerators(tempGen);
						saveBestPolicy(bestPolicy);
						// Output the episode averages
						savePerformance(episodePerformances, run);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				// Run the post update operations
				localPolicy.postUpdateOperations();

				t++;

				// Clear the restart
				localPolicy.shouldRestart();
			} else {
				// Instead of starting over, just remove any policies
				// containing non-existant or recently changed rules.
				filterPolicyValues(pvs, localPolicy);
			}
		}
	}

	/**
	 * Filters out any policies containing rules that are no longer in the
	 * policy generator.
	 * 
	 * @param pvs
	 *            The list of policy values.
	 */
	private void filterPolicyValues(List<PolicyValue> pvs,
			PolicyGenerator localPolicyGenerator) {
		for (Iterator<PolicyValue> pvIter = pvs.iterator(); pvIter.hasNext();) {
			PolicyValue pv = pvIter.next();
			Collection<GuidedRule> policyRules = pv.getPolicy()
					.getFiringRules();
			boolean remove = false;
			// Check each firing rule in the policy.
			for (GuidedRule gr : policyRules) {
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

	private void printRuleWorths(PolicyGenerator localPolicy) {
		System.out.println("\tRULE WORTHS");
		Comparator<GuidedRule> comp = new Comparator<GuidedRule>() {

			@Override
			public int compare(GuidedRule o1, GuidedRule o2) {
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
		SortedSet<GuidedRule> sortedRules = new TreeSet<GuidedRule>(comp);
		for (Slot slot : localPolicy.getGenerator()) {
			for (GuidedRule rule : slot.getGenerator()) {
				sortedRules.add(rule);
			}
		}

		for (GuidedRule rule : sortedRules)
			System.out.println(StateSpec.getInstance().encodeRule(rule) + ": "
					+ rule.getInternalMean() + ((char) 177)
					+ rule.getInternalSD());
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
		SortedSet<ConstantPred> modularFacts = policyGenerator
				.getConstantFacts();

		// Check if we have a module file for each.
		for (Iterator<ConstantPred> factIter = modularFacts.iterator(); factIter
				.hasNext();) {
			ConstantPred pred = factIter.next();
			if (Module.moduleExists(StateSpec.getInstance()
					.getEnvironmentName(), pred.getFacts())) {
				factIter.remove();
			}
		}

		// We should be left with whatever modules do not yet exist
		if (!modularFacts.isEmpty()) {
			int modsComplete = 0;
			// Commence learning of the module
			for (ConstantPred internalGoal : modularFacts) {
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
				ObjectObservations.getInstance().objectArray = internalGoal
						.getFacts().toArray(
								new String[internalGoal.getFacts().size()]);
				RLGlue.RL_agent_message(INTERNAL_PREFIX);
				String[] oldInternalGoal = (String[]) ObjectObservations
						.getInstance().objectArray;

				// Begin development
				PolicyGenerator modularGenerator = null;
				// If we have several constant facts in one rule, we need to
				// find the distribution of rules in the module that fit it
				// properly.
				if (internalGoal.getFacts().size() > 1) {
					Collection<GuidedRule> rules = new ArrayList<GuidedRule>();
					// Run through each module (known to be created) and add the
					// rules together.
					List<String> newQueryParams = new ArrayList<String>();
					int i = 0;
					for (String fact : internalGoal.getFacts()) {
						Module partialMod = Module.loadModule(StateSpec
								.getInstance().getEnvironmentName(), fact);

						// Reform the rule parameters
						int j = 0;
						for (GuidedRule gr : partialMod.getModuleRules()) {
							if (j == 0) {
								j = gr.getQueryParameters().size();
							}
							gr.shiftModularVariables(i);
							gr.setAsLoadedModuleRule(false);
							rules.add(gr);
						}

						// Forming the new query parameters
						for (int k = i; k < (j + i); k++)
							newQueryParams.add(Module.createModuleParameter(k));

						i += j;
					}

					// Create a policy generator that only updates slot weights.
					modularGenerator = PolicyGenerator.newInstance(
							policyGenerator, rules, newQueryParams,
							internalGoal.getFacts());
				} else {
					modularGenerator = PolicyGenerator.newInstance(
							policyGenerator, internalGoal.getFacts());
				}
				developPolicy(modularGenerator, -modularFacts.size()
						+ modsComplete);

				// Save the module
				Module.saveModule(internalGoal.getFacts(), StateSpec
						.getInstance().getEnvironmentName(), modularGenerator
						.getGenerator());

				modsComplete++;

				// Unset the internal goal
				if (oldInternalGoal == null) {
					ObjectObservations.getInstance().objectArray = null;
					RLGlue.RL_agent_message(INTERNAL_PREFIX);
				} else {
					ObjectObservations.getInstance().objectArray = oldInternalGoal;
					RLGlue.RL_agent_message(INTERNAL_PREFIX);
				}
			}

			// Ensure to reset the policy generator
			PolicyGenerator.setInstance(policyGenerator);
		}
	}

	/**
	 * Determines the population of rules to use for optimisation.
	 * 
	 * @param policyGenerator
	 *            The policy generator to determine the populations from.
	 * @return A population of rules, large enough to reasonably test most
	 *         combinations of rules.
	 */
	private int determinePopulation(PolicyGenerator policyGenerator) {
		// If the generator is just a slot optimiser, use 50 * slot number
		if (policyGenerator.isSlotOptimiser()) {
			return (int) (POPULATION_CONSTANT * policyGenerator.getGenerator()
					.size());
		}
		// Currently just using 50 * the largest slot
		int largestSlot = 0;
		for (Slot slot : PolicyGenerator.getInstance().getGenerator()) {
			largestSlot = Math.max(largestSlot, slot.getGenerator().size());
		}
		return (int) (POPULATION_CONSTANT * largestSlot);
	}

	/**
	 * Run the agent over the environment until we have a single pre-goal and
	 * some rules to work with.
	 */
	private void preliminaryProcessing() {
		while (!PolicyGenerator.getInstance().isSettled(false)) {
			Policy pol = PolicyGenerator.getInstance().generatePolicy();
			System.out.println(pol);
			// Send the agent a generated policy
			ObjectObservations.getInstance().objectArray = new Policy[] { pol };
			RLGlue.RL_agent_message("Policy");
			RLGlue.RL_episode(maxSteps_);
		}
		PolicyGenerator.getInstance().shouldRestart();
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
			double expProg) {
		System.out.println();
		System.out.println("Beginning testing for episode " + episode + ".");
		System.out.println();
		float averageScore = 0;
		RLGlue.RL_env_message("freeze");

		long startTime = System.currentTimeMillis();

		// Run the agent through several test iterations, resampling the agent
		// at each step
		for (int i = 0; i < TEST_ITERATIONS; i++) {
			estimateTestTime(i, TEST_ITERATIONS, expProg, startTime);

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
		}
		averageScore /= (AVERAGE_ITERATIONS * TEST_ITERATIONS);

		// Write the state of the generators out in human readable form
		try {
			File output = null;
			if (PolicyGenerator.getInstance().isModuleGenerator())
				output = new File(Module.MODULE_DIR + "/" + TEMP_FOLDER + "/"
						+ PolicyGenerator.getInstance().getModuleName()
						+ humanGeneratorFile_.getName());
			else
				output = new File(TEMP_FOLDER + "/"
						+ humanGeneratorFile_.getName() + run);
			output.createNewFile();
			PolicyGenerator.getInstance().saveHumanGenerators(output);
		} catch (Exception e) {
			e.printStackTrace();
		}

		RLGlue.RL_env_message("unfreeze");

		learningStartTime_ = System.currentTimeMillis();
		return averageScore;
	}

	private void estimateTestTime(int i, int testIterations, double expProg,
			long startTime) {
		// Test time elapsed, with static learning time
		long testElapsedTime = System.currentTimeMillis() - startTime;
		String elapsed = "Elapsed: " + toTimeFormat(testElapsedTime);
		String learningElapsed = "Learning elapsed: "
				+ toTimeFormat(learningRunTime_);
		System.out.println(elapsed + ", " + learningElapsed);

		// Test percent with ETA for test
		DecimalFormat formatter = new DecimalFormat("#0.0000");
		double testProg = (1.0 * i) / testIterations;
		String percentStr = formatter.format(100 * testProg)
				+ "% test complete.";
		long testRemainingTime = (long) (testElapsedTime / testProg - testElapsedTime);
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
	 * Prints out the percentage complete, time elapsed and estimated time
	 * remaining.
	 * 
	 * @param samples
	 *            The number of samples obtained before updating.
	 * @param maxSamples
	 *            The number of samples required for update.
	 * @param iteration
	 *            The current learning iteration of the run.
	 * @param maxIteration
	 *            The maximum number of iterations per run.
	 * @param run
	 *            The run number.
	 * @param maxRuns
	 *            The total number of runs.
	 * @param startTime
	 *            The time the experiment was started.
	 * @param noteLearningTime
	 *            Whether to note learning time or not.
	 */
	private void estimateETA(int samples, int maxSamples, int iteration,
			int maxIteration, int run, int maxRuns, long startTime,
			boolean noteLearningTime) {
		if (noteLearningTime) {
			learningRunTime_ += System.currentTimeMillis() - learningStartTime_;
			learningStartTime_ = System.currentTimeMillis();
		}

		long elapsedTime = System.currentTimeMillis() - startTime;
		String elapsed = "Elapsed: " + toTimeFormat(elapsedTime);
		String learningElapsed = "Learning elapsed: "
				+ toTimeFormat(learningRunTime_);
		System.out.println(elapsed + ", " + learningElapsed);

		double percentIterComplete = (maxSamples > 0) ? (1.0 * samples)
				/ maxSamples : 1;
		double percentRunComplete = (1.0 * iteration + percentIterComplete)
				/ maxIteration;
		double totalRunComplete = (1.0 * run + percentRunComplete) / maxRuns;

		DecimalFormat formatter = new DecimalFormat("#0.0000");
		String percentStr = formatter.format(100 * percentRunComplete)
				+ "% experiment run complete.";
		long runRemainingTime = (long) (elapsedTime / percentRunComplete - elapsedTime);
		String runRemaining = "Remaining: " + toTimeFormat(runRemainingTime);
		System.out.println(percentStr + " " + runRemaining);

		String totalPercentStr = formatter.format(100 * totalRunComplete)
				+ "% experiment complete.";
		long totalRemainingTime = (long) (experimentStart_ / totalRunComplete - experimentStart_);
		String totalRemaining = "Remaining: "
				+ toTimeFormat(totalRemainingTime);
		System.out.println(totalPercentStr + " " + totalRemaining);
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
					+ PolicyGenerator.getInstance().getModuleName()
					+ performanceFile_.getName());
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
}
