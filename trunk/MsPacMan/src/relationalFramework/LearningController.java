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
	/** The first run number. */
	private int repetitionsStart_ = 0;
	/** The last run number. */
	private int repetitionsEnd_ = 1;
	/** The ratio of samples to use as 'elite' samples. */
	private static final double POPULATION_CONSTANT = 10;
	/** The ratio of samples to use as 'elite' samples. */
	private static final double SELECTION_RATIO = 0.1;
	/** The rate at which the weights change. */
	private static final double STEP_SIZE = 0.6;
	/** The internal prefix for messages to the agent regarding internal goal. */
	public static final String INTERNAL_PREFIX = "internal";
	/** The marker for the end of a successfully completed performance file. */
	private static final String END_PERFORMANCE = "<--END-->";
	/** The time that the experiment started. */
	private long experimentStart_;
	/** The time at which the learning started */
	private long learningStartTime_;
	/** The amount of time the experiment has taken, excluding testing. */
	private long learningRunTime_ = 0;
	/** The time the run started. */
	private long runStart_;
	/** The extra arguments to message the environment. */
	private String[] extraArgs_;
	/** The maximum number of steps the agent can take. */
	private int maxSteps_;
	/** The generator file to load. */
	private File loadedGeneratorFile_;
	/*** If the algorithm is running tests throughout the learning. */
	private boolean runningTests_ = true;

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
			String[] repetitionsStr = bf.readLine().split("-");
			// Num repetitions specified by a range for seeding random number.
			Integer repetitionsStart = 0;
			Integer repetitionsEnd = Integer.parseInt(repetitionsStr[0]);
			if (repetitionsStr.length == 2) {
				repetitionsStart = repetitionsEnd;
				repetitionsEnd = Integer.parseInt(repetitionsStr[1]);
			}
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

			initialise(environmentClass, repetitionsStart, repetitionsEnd,
					episodes, policyFile, generatorFile, performanceFile,
					extraArgsList.toArray(new String[extraArgsList.size()]));

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
				} else if (args[i].equals("-t"))
					runningTests_ = false;
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
	 * @param repetitionsStart
	 *            The first run number seed.
	 * @param repetitionsEnd
	 *            The last run number seed.
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
	private void initialise(String environmentClass, int repetitionsStart,
			int repetitionsEnd, int episodeCount, String policyFile,
			String generatorFile, String performanceFile, String[] extraArgs) {
		repetitionsStart_ = repetitionsStart;
		repetitionsEnd_ = repetitionsEnd;
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
		int[] startPoint = checkFiles(repetitionsStart_);
		int run = startPoint[0];
		// TODO Load the temp generators with all the necessary info
		// (pre-goal too)
		int iteration = -1;// startPoint[1];

		// The ultra-outer loop, for averaging experiment results
		for (; run < repetitionsEnd_; run++) {
			runStart_ = System.currentTimeMillis();
			// Initialise a new policy generator.
			PolicyGenerator localPolicy = PolicyGenerator.newInstance(run);
			if (loadedGeneratorFile_ != null) {
				localPolicy.loadGenerators(loadedGeneratorFile_);
				localPolicy.freeze(true);
			}

			developPolicy(localPolicy, run, iteration);

			// Flushing the rete object.
			StateSpec.reinitInstance();

			// Resetting experiment values
			PolicyGenerator.getInstance().resetGenerator();
		}

		RLGlue.RL_cleanup();

		if (repetitionsStart_ == 0) {
			try {
				combineGenerators(repetitionsStart_, repetitionsEnd_);
				compilePerformanceAverage(repetitionsStart_, repetitionsEnd_);
			} catch (Exception e) {
				e.printStackTrace();
			}
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
	 * @param startIteration
	 *            The iteration point to start from.
	 */
	private void developPolicy(PolicyGenerator localPolicy, int run,
			int startIteration) {
		PolicyValue bestPolicy = null;

		// Run the preliminary action discovery phase, only to create an initial
		// number of rules.
		if (loadedGeneratorFile_ == null)
			preliminaryProcessing();

		// The outer loop, for refinement episode by episode
		ArrayList<Float> episodePerformances = new ArrayList<Float>();

		// Forming a population of solutions
		List<PolicyValue> pvs = new ArrayList<PolicyValue>();
		// How many steps to wait for testing
		int testingStep = 1;
		if (ENTROBEAM) {
			testingStep = (int) (1 / (SELECTION_RATIO * SELECTION_RATIO));
		} else if (SLIDING_WINDOW) {
			testingStep = (int) (1 / SELECTION_RATIO);
		} else {
			testingStep = 1;
		}
		// Learn for a finite number of episodes, or until it is converged.
		int finiteNum = maxEpisodes_ * testingStep;
		if (maxEpisodes_ < 0)
			finiteNum = Integer.MAX_VALUE;

		// Determining the start point;
		int t = 0;
		if (startIteration >= 0)
			t = startIteration * testingStep + 1;

		// A value to track how many intervals since the last test.
		int sinceLastTest = 0;
		while ((t < finiteNum) && (!localPolicy.isConverged())) {
			if (PolicyGenerator.getInstance().useModules_) {
				// Check if the agent needs to drop into learning a module
				checkForModularLearning(localPolicy);
			}

			int pvsSizeInitial = pvs.size();

			// Determine the dynamic population, based on rule-base size
			int population = determinePopulation(localPolicy);
			// If entrobeam, only get the minimum number of samples.
			if (ENTROBEAM) {
				testingStep = population;
				finiteNum = maxEpisodes_ * testingStep;
				if (maxEpisodes_ < 0)
					finiteNum = Integer.MAX_VALUE;
				population = (int) Math.max(SELECTION_RATIO * population,
						POPULATION_CONSTANT);
			}

			int samples = 0;
			int maxSamples = population;
			if (SLIDING_WINDOW) {
				samples = pvs.size() - pvsSizeInitial;
				maxSamples = population - pvsSizeInitial;
			}

			boolean restart = false;
			// Fill the Policy Values list.
			int maxSize = (t == 0) ? population * 2 : population;
			do {
				Policy pol = localPolicy.generatePolicy(true);
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
				estimateETA(samples, maxSamples, t, finiteNum, run
						- repetitionsStart_, repetitionsEnd_
						- repetitionsStart_, experimentStart_, true);

				// Debug - Looking at rule values
				// printRuleWorths(localPolicy);
			} while (pvs.size() < maxSize);

			if (!restart) {
				Collections.sort(pvs);
				// Update the weights for all distributions using only the elite
				// samples
				int numElite = (int) Math.ceil(population * SELECTION_RATIO);
				double alphaUpdate = STEP_SIZE / testingStep;
				if (ENTROBEAM) {
					numElite = population;
				}

				// Clean up the policy values
				pvs = preUpdateModification(pvs, numElite);

				localPolicy.updateDistributions(pvs, numElite, alphaUpdate);

				pvs = postUpdateModification(pvs, t, testingStep);

				// Only test the agent every number of steps, otherwise more
				// time is spent testing than evaluating. (And at the first and
				// last steps).
				if ((t == finiteNum - 1) || (sinceLastTest >= testingStep)
						|| (t == 0)) {
					testRecordAgent(localPolicy, run, episodePerformances, pvs,
							finiteNum, t);
					sinceLastTest = 0;
				}

				// Run the post update operations
				localPolicy.postUpdateOperations(population, Math.pow(
						(1 - STEP_SIZE), 2));

				t++;
				sinceLastTest++;

				// Clear the restart
				localPolicy.shouldRestart();
			} else {
				// Instead of starting over, just remove any policies
				// containing non-existant or recently changed rules.
				filterPolicyValues(pvs, localPolicy);
			}
		}

		// If the agent finished prematurely, note the results.
		if (sinceLastTest > 0) {
			testRecordAgent(localPolicy, run, episodePerformances, pvs,
					finiteNum, t);
		}

		try {
			savePerformance(episodePerformances, run, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Tests and records the agent's progress.
	 * 
	 * @param localPolicy
	 *            The local PolicyGenerator.
	 * @param run
	 *            The current run.
	 * @param episodePerformances
	 *            The previous episode performances.
	 * @param pvs
	 *            The elite policy values.
	 * @param finiteNum
	 *            The maximum number of iterations to learn in.
	 * @param t
	 *            The current progress of the iterations.
	 */
	private void testRecordAgent(PolicyGenerator localPolicy, int run,
			ArrayList<Float> episodePerformances, List<PolicyValue> pvs,
			int finiteNum, int t) {
		// Test the agent and record the performances
		double expProg = ((1.0 * (t + 1)) / finiteNum + (1.0 * (run - repetitionsStart_)))
				/ (repetitionsEnd_ - repetitionsStart_);
		episodePerformances.add(testAgent(t, maxSteps_, run,
				(repetitionsEnd_ - repetitionsStart_), expProg));

		// Save the results at each episode
		try {
			File tempGen = null;
			if (localPolicy.isModuleGenerator())
				tempGen = new File(Module.MODULE_DIR + "/" + TEMP_FOLDER + "/"
						+ localPolicy.getModuleName()
						+ generatorFile_.getName());
			else
				tempGen = new File(TEMP_FOLDER + "/" + generatorFile_.getName()
						+ run);
			tempGen.createNewFile();
			localPolicy.saveGenerators(tempGen);
			saveElitePolicies(pvs);
			// Output the episode averages
			if (runningTests_)
				savePerformance(episodePerformances, run, false);
			File preGoalFile = new File(TEMP_FOLDER + "/preGoals.txt" + run);
			preGoalFile.createNewFile();
			localPolicy.savePreGoals(preGoalFile);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Modifies the policy values before updating (cutting the values down to
	 * size).
	 * 
	 * @param pvs
	 *            The policy values to modify.
	 * @param numElite
	 *            The number of elite samples to use when updating. The size
	 *            policy values should be.
	 * @return The modified policy values list.
	 */
	private List<PolicyValue> preUpdateModification(List<PolicyValue> pvs,
			int numElite) {
		if (ENTROBEAM) {
			if (pvs.size() > numElite) {
				for (int i = pvs.size() - 1; i >= numElite; i--)
					pvs.remove(i);
			}
		} else if (SLIDING_WINDOW)
			pvs = pvs.subList(0, pvs.size() - numElite);
		return pvs;
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
	 * @return The cleaned list of policy values.
	 */
	private List<PolicyValue> postUpdateModification(List<PolicyValue> pvs,
			int iteration, double staleValue) {
		// Remove any stale policies
		for (Iterator<PolicyValue> iter = pvs.iterator(); iter.hasNext();) {
			PolicyValue pv = iter.next();
			if (iteration - pv.getIteration() >= staleValue) {
				iter.remove();
			}
		}
		return pvs;
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
	 *            The current policy generator.
	 */
	private void checkForModularLearning(PolicyGenerator policyGenerator) {
		// Run through each rule in the policy generator, noting which ones
		// require module learning.
		// TODO Problem: Have duplicate facts here. Maybe just check the facts
		// have not already been used.
		SortedSet<ConstantPred> constantFacts = policyGenerator
				.getConstantFacts();

		// Check if we have a module file for each.
		Set<String> usedPreds = new HashSet<String>();
		for (Iterator<ConstantPred> factIter = constantFacts.iterator(); factIter
				.hasNext();) {
			ConstantPred pred = factIter.next();
			if (Module.moduleExists(StateSpec.getInstance()
					.getEnvironmentName(), pred.getFacts())
					|| usedPreds.contains(pred.toString())) {
				factIter.remove();
			}
			usedPreds.add(pred.toString());
		}

		// We should be left with whatever modules do not yet exist
		if (!constantFacts.isEmpty()) {
			int modsComplete = 0;
			// Commence learning of the module
			for (ConstantPred internalGoal : constantFacts) {
				createModule(policyGenerator, constantFacts, modsComplete,
						internalGoal);
			}

			// Ensure to reset the policy generator
			PolicyGenerator.setInstance(policyGenerator);
		}
	}

	/**
	 * Creates a module by initialising generators.
	 * 
	 * @param policyGenerator
	 *            The current policy generator.
	 * @param constantFacts
	 *            All the constant facts in the policy.
	 * @param modsComplete
	 *            How many modules are complete.
	 * @param internalGoal
	 *            The current module being created.
	 */
	private void createModule(PolicyGenerator policyGenerator,
			SortedSet<ConstantPred> constantFacts, int modsComplete,
			ConstantPred internalGoal) {
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
		String[] moduleGoal = new String[internalGoal.getFacts().size()];
		for (int i = 0; i < internalGoal.getFacts().size(); i++)
			moduleGoal[i] = internalGoal.getFacts().get(i).getFactName();
		ObjectObservations.getInstance().objectArray = moduleGoal;
		RLGlue.RL_agent_message(INTERNAL_PREFIX);
		String[] oldInternalGoal = (String[]) ObjectObservations.getInstance().objectArray;

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
			for (StringFact fact : internalGoal.getFacts()) {
				Module partialMod = Module.loadModule(StateSpec.getInstance()
						.getEnvironmentName(), fact.getFactName());
				if (partialMod == null) {
					unsetInternalGoal(oldInternalGoal);
					return;
				}

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
			modularGenerator = PolicyGenerator.newInstance(policyGenerator,
					rules, newQueryParams, internalGoal.getFacts());
		} else {
			modularGenerator = PolicyGenerator.newInstance(policyGenerator,
					internalGoal.getFacts());
		}
		developPolicy(modularGenerator, -constantFacts.size() + modsComplete, 0);

		// Save the module
		Module.saveModule(internalGoal.getFacts(), StateSpec.getInstance()
				.getEnvironmentName(), modularGenerator.getGenerator());

		modsComplete++;

		// Unset the internal goal
		unsetInternalGoal(oldInternalGoal);
	}

	/**
	 * Unsets the internal goal of the policy.
	 * 
	 * @param oldInternalGoal
	 *            The old internal goal to set.
	 */
	private void unsetInternalGoal(String[] oldInternalGoal) {
		if (oldInternalGoal == null) {
			ObjectObservations.getInstance().objectArray = null;
			RLGlue.RL_agent_message(INTERNAL_PREFIX);
		} else {
			ObjectObservations.getInstance().objectArray = oldInternalGoal;
			RLGlue.RL_agent_message(INTERNAL_PREFIX);
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

		double sumSlot = 0;
		for (Slot slot : policyGenerator.getGenerator()) {
			double weight = slot.getSelectionProbability()
					+ slot.getSelectionSD();
			if (weight > 1)
				weight = 1;
			sumSlot += (slot.getGenerator().size() * weight);
		}
		return (int) (POPULATION_CONSTANT * (sumSlot / policyGenerator
				.getGenerator().size()));
	}

	/**
	 * Run the agent over the environment until we have a single pre-goal and
	 * some rules to work with.
	 */
	private void preliminaryProcessing() {
		while (!PolicyGenerator.getInstance().isSettled(false)) {
			Policy pol = PolicyGenerator.getInstance().generatePolicy(false);
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
	 * generators and trialling the agent several times over the environment to
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
		float averageScore = 0;
		RLGlue.RL_env_message("freeze");
		if (runningTests_) {
			System.out.println();
			System.out
					.println("Beginning testing for episode " + episode + ".");
			System.out.println();

			long startTime = System.currentTimeMillis();

			// Run the agent through several test iterations, resampling the
			// agent
			// at each step
			for (int i = 0; i < TEST_ITERATIONS; i++) {
				estimateTestTime(i, TEST_ITERATIONS, expProg, startTime);

				Policy pol = PolicyGenerator.getInstance()
						.generatePolicy(false);
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
		}

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
		long totalRemainingTime = (long) ((currentTime - experimentStart_)
				/ totalRunComplete - (currentTime - experimentStart_));
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
	 * Saves the elite policies to file.
	 * 
	 * @param elites
	 *            The best policy, in string format.
	 */
	private void saveElitePolicies(List<PolicyValue> elites) throws Exception {
		FileWriter wr = new FileWriter(policyFile_);
		BufferedWriter buf = new BufferedWriter(wr);

		for (PolicyValue pv : elites) {
			buf.write(pv.getPolicy().toString(false) + "\n");
			buf.write(pv.getValue() + "\n\n");
		}

		buf.close();
		wr.close();
	}

	/**
	 * Saves the performance to file and outputs them.
	 * 
	 * @param episodeAverage
	 *            The saved episode average performances.
	 * @param finalWrite
	 *            If this write was the final write for the run.
	 */
	private void savePerformance(ArrayList<Float> episodeAverage, int run,
			boolean finalWrite) throws Exception {
		File tempPerf = null;
		File detailedPerf = null;
		if (PolicyGenerator.getInstance().isModuleGenerator()) {
			tempPerf = new File(Module.MODULE_DIR + "/" + TEMP_FOLDER + "/"
					+ PolicyGenerator.getInstance().getModuleName()
					+ performanceFile_.getName());
			detailedPerf = new File(Module.MODULE_DIR + "/" + TEMP_FOLDER + "/"
					+ PolicyGenerator.getInstance().getModuleName()
					+ "detailed" + performanceFile_.getName());
		} else {
			tempPerf = new File(TEMP_FOLDER + "/" + performanceFile_.getName()
					+ run);
			detailedPerf = new File(TEMP_FOLDER + "/" + "detailed"
					+ performanceFile_.getName() + run);
		}
		tempPerf.createNewFile();
		detailedPerf.createNewFile();
		FileWriter wr = new FileWriter(tempPerf);
		BufferedWriter buf = new BufferedWriter(wr);
		FileWriter detWR = new FileWriter(detailedPerf);
		BufferedWriter detBuf = new BufferedWriter(detWR);

		System.out.println("Average episode elite scores:");
		for (float perf : episodeAverage) {
			PolicyGenerator.getInstance().saveHumanGenerators(detBuf);
			detBuf.write("\n");
			PolicyGenerator.getInstance().saveGenerators(detBuf);
			detBuf.write("\n\n" + perf + "\n");
			detBuf.write("\n\n\n");
			
			buf.write(perf + "\n");
			System.out.println(perf);
		}

		if (finalWrite) {
			buf.write(END_PERFORMANCE + "\n");
			buf.write("Total run time: "
					+ toTimeFormat(System.currentTimeMillis() - runStart_));
		}

		detBuf.close();
		detWR.close();
		buf.close();
		wr.close();
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
				iteration++;
			}

			result[0] = run - 1;
			result[1] = iteration;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Compiles the performance files togetrher into a single file, detailing
	 * the average, min and max performances.
	 * 
	 * @param runStart
	 *            The first run.
	 * @param runEnd
	 *            The last run.
	 */
	private void compilePerformanceAverage(int runStart, int runEnd)
			throws Exception {
		double[][] performances = new double[maxEpisodes_][runEnd - runStart];
		float min = Float.MAX_VALUE;
		int minIndex = -1;
		float max = -Float.MAX_VALUE;
		int maxIndex = -1;
		// For every performance file
		for (int i = runStart; i < runEnd; i++) {
			File tempPerf = new File(TEMP_FOLDER + "/" + performanceFile_ + i);
			FileReader reader = new FileReader(tempPerf);
			BufferedReader buf = new BufferedReader(reader);

			// For every value within the performance file
			float sum = 0;
			float val = 0;
			boolean noNote = false;
			for (int e = 0; e < maxEpisodes_; e++) {
				// Some performance files may be cut off, so just use the last
				// recorded value.
				String input = buf.readLine();
				if ((input == null) || (input.equals(END_PERFORMANCE)))
					noNote = true;
				if (!noNote) {
					val = Float.parseFloat(input);
				}
				performances[e][i - runStart] = val;
				sum += val;
			}

			// Find min or max runs
			if (sum < min) {
				min = sum;
				minIndex = i - runStart;
			}
			if (sum > max) {
				max = sum;
				maxIndex = i - runStart;
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

		buf.write("Total Run Time: "
				+ toTimeFormat(System.currentTimeMillis() - experimentStart_)
				+ "\n");
		buf.write("Total Learning Time: " + toTimeFormat(learningRunTime_)
				+ "\n");

		buf.close();
		writer.close();
	}

	/**
	 * Combines the generators into a single file, averaging the generator
	 * values.
	 * 
	 * @param runStart
	 *            The first run.
	 * @param runEnd
	 *            The last run.
	 * @throws Exception
	 *             Should something go awry...
	 */
	private void combineGenerators(int runStart, int runEnd) throws Exception {
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
