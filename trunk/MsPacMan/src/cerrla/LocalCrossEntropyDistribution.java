package cerrla;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.BidiMap;

import jess.QueryResult;
import jess.Rete;
import jess.ValueVector;

import relationalFramework.GoalCondition;
import relationalFramework.ModularPolicy;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.LocalAgentObservations;
import rrlFramework.RRLObservations;
import util.MultiMap;

/**
 * A localised-to-goal class containing the agent's behaviour, the current goal,
 * and performance observations relating to this behaviour.
 * 
 * @author Sam Sarjant
 */
public class LocalCrossEntropyDistribution implements Serializable {

	/** The collection of non existant modules. */
	private static final Collection<String> nonExistantModule_ = new HashSet<String>();

	private static final long serialVersionUID = 6883881456264179505L;

	/** The relative directory in which modules are stored. */
	public static final String MODULE_DIR = "modules";

	/** TODO The suffix for module files. */
	public static final String MODULE_SUFFIX = ".mod";

	/** The suffix for module files. */
	public static final String SERIALISED_SUFFIX = ".ser";

	/** The current episode as evidenced by this generator. */
	private int currentEpisode_;

	/** The current policy being used for these (3) episodes. */
	// private transient ModularPolicy currentPolicy_;

	/** The elites set. */
	private SortedSet<PolicyValue> elites_;

	/** The reward received this episode. */
	// private transient double episodeReward_;

	/** If this generator is currently frozen (not learning). */
	private transient boolean frozen_;

	/** An internal flag to check if the sub-goal has been achieved. */
	// private boolean goalAchieved_;

	/** The goal condition for this cross-entropy behaviour. */
	private final GoalCondition goalCondition_;

	/** The created goal rule for checking if the internal goal is achieved. */
	private final RelationalRule goalRule_;

	/** The localised agent observations for this goal. */
	private transient LocalAgentObservations localAgentObservations_;

	/** The minimum number of elites value. */
	private int numElites_;

	/** If the AgentObsrvations were settled last episode. */
	private boolean oldAOSettled_;

	/** The performance object, noting figures. */
	private final Performance performance_;

	/** The distributions of rules. */
	private final PolicyGenerator policyGenerator_;

	/** The population value. */
	private int population_;

	/** The current testing episode. */
	private transient int testEpisode_;

	/**
	 * Create new sub-goal behaviour using information from another
	 * distribution.
	 * 
	 * @param goal
	 *            The goal of this behaviour.
	 */
	public LocalCrossEntropyDistribution(GoalCondition goal) {
		this(goal, -1);
	}

	/**
	 * Initialise new learned behaviour with the given goal.
	 * 
	 * @param goal
	 *            The goal of the behaviour.
	 * @param run
	 *            The run this generator is for.
	 */
	public LocalCrossEntropyDistribution(GoalCondition goal, int run) {
		if (goal.isMainGoal())
			goalCondition_ = goal;
		else {
			goalCondition_ = new GoalCondition(goal);
			goalCondition_.normaliseArgs();
		}

		policyGenerator_ = new PolicyGenerator(this);
		if (run == -1)
			performance_ = new Performance(true);
		else
			performance_ = new Performance(run);
		elites_ = new TreeSet<PolicyValue>();
		population_ = Integer.MAX_VALUE;
		numElites_ = Integer.MAX_VALUE;

		// Form the goal rule
		if (!goalCondition_.isMainGoal()) {
			SortedSet<RelationalPredicate> conditions = new TreeSet<RelationalPredicate>(
					goalCondition_.getFacts());
			goalRule_ = new RelationalRule(conditions, null, null);
			goalRule_.expandConditions();
		} else
			goalRule_ = null;

		// Load the local agent observations
		localAgentObservations_ = LocalAgentObservations
				.loadAgentObservations(goal);
	}

	/**
	 * Modifies the policy values before updating (cutting the values down to
	 * size).
	 * 
	 * @param elites
	 *            The policy values to modify.
	 * @param numElite
	 *            The minimum number of elite samples.
	 * @param staleValue
	 *            The number of policies a sample hangs around for.
	 * @param minValue
	 *            The minimum observed value.
	 * @return The policy values that were removed.
	 */
	private SortedSet<PolicyValue> preUpdateModification(
			SortedSet<PolicyValue> elites, int numElite, int staleValue,
			double minValue) {
		// Firstly, remove any policy values that have been around for more
		// than N steps

		// Only remove stuff if the elites are a representative solution
		int iteration = policyGenerator_.getPoliciesEvaluated();
		for (Iterator<PolicyValue> iter = elites.iterator(); iter.hasNext();) {
			PolicyValue pv = iter.next();
			if (iteration - pv.getIteration() >= staleValue) {
				if (ProgramArgument.RETEST_STALE_POLICIES.booleanValue())
					policyGenerator_.retestPolicy(pv.getPolicy());
				iter.remove();
			}
		}

		SortedSet<PolicyValue> tailSet = null;
		if (elites.size() > numElite) {
			// Find the N_E value
			Iterator<PolicyValue> pvIter = elites.iterator();
			PolicyValue currentPV = null;
			for (int i = 0; i < numElite; i++)
				currentPV = pvIter.next();

			// Iter at N_E value. Remove any values less than N_E's value
			tailSet = new TreeSet<PolicyValue>(elites.tailSet(new PolicyValue(
					null, currentPV.getValue(), -1)));
			elites.removeAll(tailSet);
		}

		return tailSet;
	}

	/**
	 * Processes the internal goal (checks if it is achieved).
	 * 
	 * @param modularPolicy
	 *            The policy that called this operation.
	 * @param observations
	 *            The current state observations.
	 * @param goalReplacements
	 *            The current goal replacement variable(s) (a -> ?G_0).
	 * @return True if the internal goal is/has been achieved.
	 */
	private boolean processInternalGoal(ModularPolicy modularPolicy,
			RRLObservations observations, BidiMap goalReplacements) {
		if (modularPolicy.isGoalAchieved())
			return true;

		try {
			// Assign the parameters
			Rete state = observations.getState();
			// System.out.println(StateSpec.extractFacts(state));
			ValueVector vv = new ValueVector();
			goalRule_.setParameters(goalReplacements);
			for (String param : goalRule_.getParameters())
				vv.add(param);

			// Run the query
			String query = StateSpec.getInstance().getRuleQuery(goalRule_);
			QueryResult results = state.runQueryStar(query, vv);

			// If results, then the goal has been met!
			if (results.next()) {
				modularPolicy.setGoalAchieved(true);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return modularPolicy.isGoalAchieved();
	}

	/**
	 * Updates the distributions based on the current state of the elites.
	 * 
	 * @param elites
	 *            The elite values (+ 1 more sample).
	 * @return True if the distribution changed drastically (new slots/rules
	 *         created).
	 */
	private boolean updateDistributions(SortedSet<PolicyValue> elites,
			int population, int numElites) {
		if (population == 0)
			return false;

		double minReward = performance_.getMinimumReward();

		// Clean up the policy values
		SortedSet<PolicyValue> removed = preUpdateModification(elites,
				numElites, population, minReward);

		policyGenerator_.updateDistributions(elites,
				ProgramArgument.ALPHA.doubleValue(), population, numElites,
				minReward);
		// Negative updates:
		if (ProgramArgument.NEGATIVE_UPDATES.booleanValue())
			policyGenerator_.updateNegative(elites,
					ProgramArgument.ALPHA.doubleValue(), population, numElites,
					removed);

		// Run the post update operations
		boolean resetElites = policyGenerator_.postUpdateOperations(numElites);

		// Clear the restart
		policyGenerator_.shouldRestart();
		return resetElites;
	}

	/**
	 * (Potentially) covers the current state depending on whether the agent
	 * believes covering the state will get it more information.
	 * 
	 * @param modularPolicy
	 *            The policy that called this covering operation.
	 * @param observations
	 *            The current state observations.
	 * @param activatedActions
	 *            The actions found by the current RLGG rules.
	 * @param moduleParamReplacements
	 *            Optional module parameter replacements to apply to the current
	 *            goal replacements.
	 * @return Any newly created RLGG rules, or null if no change/no new rules.
	 */
	@SuppressWarnings("unchecked")
	public List<RelationalRule> coverState(ModularPolicy modularPolicy,
			RRLObservations observations,
			MultiMap<String, String[]> activatedActions,
			BidiMap goalReplacements) {
		// Process internal goals and return if goal is already achieved.
		if (!goalCondition_.isMainGoal()) {
			if (processInternalGoal(modularPolicy, observations,
					goalReplacements))
				return null;
		}

		// Only trigger RLGG covering if it is needed.
		if (!frozen_
				&& localAgentObservations_.observeState(observations,
						activatedActions, goalReplacements)) {
			// Old RLGGs
			Collection<RelationalRule> oldRLGGs = new ArrayList<RelationalRule>(
					policyGenerator_.getRLGGRules().values());
			policyGenerator_.removeRLGGRules();
			List<RelationalRule> covered = localAgentObservations_
					.getRLGGRules();

			policyGenerator_.addRLGGRules(covered);
			covered.removeAll(oldRLGGs);
			return covered;
		}

		return null;
	}

	/**
	 * Performs a final write of the behaviour.
	 */
	public void finalWrite() {
		// Finalise the testing
		performance_.saveFiles(this, elites_, currentEpisode_, true);
	}

	/**
	 * Freeze learning (and begin testing).
	 * 
	 * @param b
	 *            To freeze or unfreeze
	 */
	public void freeze(boolean b) {
		frozen_ = b;
		policyGenerator_.freeze(b);
		performance_.freeze(b);
		testEpisode_ = 0;

		if (b) {
			// Test the learned behaviour
			System.out.println();
			if (!ProgramArgument.ENSEMBLE_EVALUATION.booleanValue())
				System.out.println("Beginning [" + goalCondition_
						+ "] testing for episode " + currentEpisode_ + ".");
			else
				System.out.println("Beginning ensemble testing for episode "
						+ currentEpisode_ + ".");
			System.out.println();
			if (!ProgramArgument.SYSTEM_OUTPUT.booleanValue())
				System.out.println("Testing...");
		}
	}

	/**
	 * Generates a policy from the current distribution.
	 * 
	 * @param modularPolicy
	 *            The modular policy that is to be crafted using the rules
	 *            generated.
	 * @return A newly generated policy from the current distribution.
	 */
	public RelationalPolicy generatePolicy(ModularPolicy modularPolicy) {
		return policyGenerator_.generatePolicy(false);
	}

	public int getCurrentEpisode() {
		return currentEpisode_;
	}

	public GoalCondition getGoalCondition() {
		return goalCondition_;
	}

	public LocalAgentObservations getLocalAgentObservations() {
		return localAgentObservations_;
	}

	public PolicyGenerator getPolicyGenerator() {
		return policyGenerator_;
	}

	/**
	 * Gets a collection fo goal conditions that represent potential modules for
	 * the rules contained within this CE cortex.
	 * 
	 * @return All potential module conditions.
	 */
	public Collection<GoalCondition> getPotentialModuleGoals() {
		return localAgentObservations_.getSpecificGoalConditions();
	}

	/**
	 * Checks if this CE distribution is converged.
	 * 
	 * @return True if the distribution is converged.
	 */
	public boolean isConverged() {
		// Check performance convergence
		if (ProgramArgument.PERFORMANCE_CONVERGENCE.booleanValue())
			if (performance_.isConverged())
				return true;

		// Check elite convergence
		if (elites_.size() >= population_
				* (1 - ProgramArgument.RHO.doubleValue())
				&& elites_.first().getValue() == elites_.last().getValue()
				&& elites_.first().getValue() > performance_.getMinimumReward())
			return true;

		// Check distribution convergence
		if (policyGenerator_.isConverged())
			return true;

		return false;
	}

	/**
	 * Notes the reward for a _single_ episode.
	 * 
	 * @param episodeReward
	 *            The total reward received this episode.
	 */
	public void noteEpisodeReward(double episodeReward) {
		// Only note the sample if it was actually used
		if (goalCondition_.isMainGoal() || episodeReward != 0) {
			if (!frozen_)
				currentEpisode_++;
		}
	}

	/**
	 * Records a given sample with a given reward.
	 * 
	 * @param value
	 *            The value of the sample.
	 */
	public void recordSample(ModularPolicy sample, Double[] values) {
		currentEpisode_ += values.length;
		double average = performance_
				.noteSampleRewards(values, currentEpisode_);

		if (!frozen_) {
			// Add sample to elites
			PolicyValue pv = new PolicyValue(sample, average,
					policyGenerator_.getPoliciesEvaluated());
			elites_.add(pv);
			policyGenerator_.incrementPoliciesEvaluated();

			// Calculate the population and number of elites
			population_ = policyGenerator_.determinePopulation();
			numElites_ = (int) Math.ceil(population_
					* ProgramArgument.RHO.doubleValue());
			// Update distributions (depending on number of elites)
			updateDistributions(elites_, population_, numElites_);
		}



		// Output system output
		if (ProgramArgument.SYSTEM_OUTPUT.booleanValue()) {
			System.out.println("[" + goalCondition_ + "] " + currentEpisode_
					+ ": " + average);

			// Estimate experiment convergence
			double convergence = policyGenerator_.getConvergenceValue();
			if (frozen_) {
				testEpisode_++;
				convergence = testEpisode_
						/ ProgramArgument.TEST_ITERATIONS.doubleValue();
			}
			int numSlots = policyGenerator_.size();
			performance_.estimateETA(convergence, numElites_, elites_,
					numSlots, goalCondition_);
		}



		if (!frozen_) {
			// Save files if necessary
			if ((localAgentObservations_.isSettled() && !oldAOSettled_)
					|| policyGenerator_.getPoliciesEvaluated()
							% ProgramArgument.PERFORMANCE_TESTING_SIZE
									.doubleValue() == 1) {
				performance_.saveFiles(this, elites_, currentEpisode_,
						policyGenerator_.hasUpdated());
			}

			oldAOSettled_ = localAgentObservations_.isSettled();
		}
	}

	/**
	 * Serialises this generator to file.
	 * 
	 * @param serFile
	 *            The file to serialise to.
	 * @param saveEnvAgentObservations
	 *            If the environment's agent observations should be saved also.
	 */
	public void saveCEDistribution(File serFile,
			boolean saveEnvAgentObservations) {
		try {
			// Write the main behaviour to temp and module
			if (goalCondition_.isMainGoal()) {
				FileOutputStream fos = new FileOutputStream(serFile);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(this);
				oos.close();
			}

			// Write serialised to module
			File moduleFolder = getModFolder(StateSpec.getInstance()
					.getEnvironmentName(), goalCondition_.toString());
			serFile = new File(moduleFolder, goalCondition_.toString()
					+ LocalCrossEntropyDistribution.SERIALISED_SUFFIX);
			serFile.createNewFile();

			FileOutputStream fos = new FileOutputStream(serFile);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this);
			oos.close();


			// Also note Local Agent Observations (as they are transient)
			localAgentObservations_
					.saveLocalObservations(saveEnvAgentObservations);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Saves the best behaviour to a text file as a static module.
	 */
	public void saveModule() {
		// Don't save the main goal as a module.
		if (goalCondition_.isMainGoal())
			return;

		try {
			File modFolder = getModFolder(StateSpec.getInstance()
					.getEnvironmentName(), goalCondition_.toString());
			File genFile = new File(modFolder,
					PolicyGenerator.SERIALISED_FILENAME);
			genFile.createNewFile();

			saveCEDistribution(genFile, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Simply resets episode reward.
	 */
	public void startEpisode() {
		// Check for convergence
		if (isConverged() && !frozen_) {
			freeze(true);
		}
	}

	@Override
	public String toString() {
		return goalCondition_.toString() + " Behaviour";
	}

	/**
	 * Basic method which fetches a module location for a given environment and
	 * local goal.
	 * 
	 * @param environmentName
	 *            The name of the environment.
	 * @param modName
	 *            The name of the module.
	 * @return The File path to the module.
	 */
	private static File getModFolder(String environmentName, String modName) {
		File modFolder = new File(MODULE_DIR + File.separatorChar
				+ environmentName);
		modFolder.mkdir();
		File goalModFolder = new File(modFolder, modName);
		goalModFolder.mkdir();
		return goalModFolder;
	}

	/**
	 * Loads a serialised {@link LocalCrossEntropyDistribution} from file (if it
	 * exists).
	 * 
	 * @param environmentPrefix
	 *            The environment prefix.
	 * @param moduleName
	 *            The name of the module.
	 * 
	 * @return The loaded distribution, or null.
	 */
	public static LocalCrossEntropyDistribution loadModule(
			String environmentPrefix, GoalCondition moduleGoal) {
		String moduleName = moduleGoal.toString();
		if (nonExistantModule_.contains(moduleName))
			return null;

		try {
			File modFile = new File(getModFolder(StateSpec.getInstance()
					.getEnvironmentName(), moduleName), moduleName
					+ SERIALISED_SUFFIX);
			if (modFile.exists()) {
				// The file exists!
				FileInputStream fis = new FileInputStream(modFile);
				ObjectInputStream ois = new ObjectInputStream(fis);
				LocalCrossEntropyDistribution lced = (LocalCrossEntropyDistribution) ois
						.readObject();
				ois.close();

				// Load Local Agent Observations
				lced.localAgentObservations_ = LocalAgentObservations
						.loadAgentObservations(moduleGoal);

				return lced;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * If this behaviour has finished learning and final testing.
	 * 
	 * @return True if the behaviour has finished learning and testing.
	 */
	public boolean isLearningComplete() {
		return frozen_
				&& testEpisode_ >= ProgramArgument.TEST_ITERATIONS.intValue()
						* ProgramArgument.POLICY_REPEATS.intValue();
	}

	public boolean isFrozen() {
		return frozen_;
	}
}
