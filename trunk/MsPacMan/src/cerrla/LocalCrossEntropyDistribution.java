package cerrla;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.BidiMap;

import cerrla.modular.GoalCondition;
import cerrla.modular.ModularPolicy;
import cerrla.modular.SpecificGoalCondition;

import jess.QueryResult;
import jess.Rete;
import jess.ValueVector;

import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.LocalAgentObservations;
import rrlFramework.Config;
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

	/** The elites set. */
	private SortedSet<PolicyValue> elites_;

	/** If this generator is currently frozen (not learning). */
	private transient boolean frozen_;

	/** The goal condition for this cross-entropy behaviour. */
	private final GoalCondition goalCondition_;

	/** The created goal rule for checking if the internal goal is achieved. */
	private transient RelationalRule goalRule_;

	/** The localised agent observations for this goal. */
	private transient LocalAgentObservations localAgentObservations_;

	/** The minimum number of elites value. */
	private int numElites_ = Integer.MAX_VALUE;

	/** If the AgentObsrvations were settled last episode. */
	private boolean oldAOSettled_;

	/** The performance object, noting figures. */
	private final Performance performance_;

	/** The distributions of rules. */
	private final PolicyGenerator policyGenerator_;

	/** The population value. */
	private int population_ = Integer.MAX_VALUE;

	/** A map of sub-goal distributions and the last time they were encountered. */
	private transient Map<LocalCrossEntropyDistribution, Integer> relevantSubDistEpisodeMap_;

	/** The current testing episode. */
	private transient int testEpisode_;

	/** A stack of policies that have not been tested fully. */
	private transient Queue<ModularPolicy> undertestedPolicies_;

	/**
	 * The ID counter for making each policy unique, even if it has the same
	 * rules.
	 */
	private transient int policyIDCounter_;

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
			goalCondition_ = goal.clone();
			if (goal instanceof SpecificGoalCondition)
				((SpecificGoalCondition) goalCondition_).normaliseArgs();
		}

		policyGenerator_ = new PolicyGenerator(this);
		if (run == -1)
			performance_ = new Performance(true);
		else
			performance_ = new Performance(run);
		elites_ = new TreeSet<PolicyValue>();

		undertestedPolicies_ = new LinkedList<ModularPolicy>();

		// Load the local agent observations
		localAgentObservations_ = LocalAgentObservations
				.loadAgentObservations(goal);
		policyIDCounter_ = 0;
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
		if (modularPolicy.isGoalCurrentlyAchieved())
			return true;

		// Form the goal rule
		if (!goalCondition_.isMainGoal()) {
			if (goalRule_ == null) {
				SortedSet<RelationalPredicate> conditions = new TreeSet<RelationalPredicate>();
				conditions.add(goalCondition_.getFact());
				goalRule_ = new RelationalRule(conditions, null, null, null);
				goalRule_.expandConditions();
			}
		} else
			goalRule_ = null;

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
				modularPolicy.setGoalAchieved();
			} else {
				modularPolicy.setGoalUnachieved(true);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return modularPolicy.isGoalCurrentlyAchieved();
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
	 * @return Any newly created (not modified) RLGG rules, or null if no
	 *         change/no new rules.
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
			// Remove the old RLGGs
			Collection<RelationalRule> oldRLGGs = policyGenerator_
					.removeRLGGRules();
			Collection<RelationalRule> covered = localAgentObservations_
					.getRLGGRules(oldRLGGs);

			return policyGenerator_.addRLGGRules(covered);
		}

		return null;
	}

	/**
	 * Performs a final write of the behaviour.
	 */
	public void finalWrite() {
		// Finalise the testing
		performance_.saveFiles(this, elites_, currentEpisode_, true, true);
	}

	/**
	 * Freeze learning (and begin testing).
	 * 
	 * @param b
	 *            To freeze or unfreeze
	 */
	public void freeze(boolean b) {
		// If same instruction, do nothing.
		if (frozen_ == b)
			return;

		frozen_ = b;
		policyGenerator_.freeze(b);
		performance_.freeze(b);
		testEpisode_ = 0;
	}

	/**
	 * Generates a policy from the current distribution.
	 * 
	 * @param existingSubGoal
	 *            A collection of all existing sub-goals in the parent policy
	 *            this policy is to be put into.
	 * @return A newly generated policy from the current distribution.
	 */
	public ModularPolicy generatePolicy(
			Collection<ModularPolicy> existingSubGoals) {
		// Initialise undertested
		if (undertestedPolicies_ == null)
			undertestedPolicies_ = new LinkedList<ModularPolicy>();

		// If there remains an undertested policy not already in the parent
		// policy, use that
		for (Iterator<ModularPolicy> iter = undertestedPolicies_.iterator(); iter
				.hasNext();) {
			ModularPolicy undertested = iter.next();
			if (undertested.shouldRegenerate())
				// If the element is fully tested, remove it.
				iter.remove();
			else if (!existingSubGoals.contains(undertested)) {
				// If the parent policy doesn't already contain the undertested
				// policy, return it.
				undertested.clearChildren();
				return undertested;
			}
		}

		// Otherwise generate a new policy
		RelationalPolicy newPol = policyGenerator_.generatePolicy(false);
		ModularPolicy newModPol = null;
		if (newPol instanceof ModularPolicy)
			newModPol = new ModularPolicy((ModularPolicy) newPol);
		else
			newModPol = new ModularPolicy(newPol, this);
		undertestedPolicies_.add(newModPol);
		return newModPol;
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
	 * Gets a collection of goal conditions that represent potential modules for
	 * the rules contained within this CE cortex.
	 * 
	 * @return All potential module conditions.
	 */
	public Collection<GoalCondition> getPotentialModuleGoals() {
		return localAgentObservations_.getObservedSubGoals();
	}

	/**
	 * Checks if this CE distribution is converged.
	 * 
	 * @return True if the distribution is converged.
	 */
	public boolean isConverged() {
		// Only converged if the relevant sub-goals are converged
		if (relevantSubDistEpisodeMap_ == null)
			relevantSubDistEpisodeMap_ = new HashMap<LocalCrossEntropyDistribution, Integer>();
		for (LocalCrossEntropyDistribution subGoal : relevantSubDistEpisodeMap_
				.keySet()) {
			if (currentEpisode_ - relevantSubDistEpisodeMap_.get(subGoal) < population_
					&& !subGoal.isConverged())
				return false;
		}

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

	public boolean isFrozen() {
		return frozen_;
	}

	/**
	 * If this behaviour has finished learning and final testing.
	 * 
	 * @return True if the behaviour has finished learning and testing.
	 */
	public boolean isLearningComplete() {
		return frozen_
				&& testEpisode_ >= ProgramArgument.TEST_ITERATIONS.intValue();
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
		// Performance
		if (!frozen_)
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

			// Noting relevant sub-goal distributions
			if (relevantSubDistEpisodeMap_ == null)
				relevantSubDistEpisodeMap_ = new HashMap<LocalCrossEntropyDistribution, Integer>();
			Collection<ModularPolicy> subPols = sample.getAllPolicies(false,
					null);
			for (ModularPolicy subPol : subPols) {
				if (subPol != sample)
					relevantSubDistEpisodeMap_.put(
							subPol.getLocalCEDistribution(), currentEpisode_);
			}
		}



		// Output system output
		if (ProgramArgument.SYSTEM_OUTPUT.booleanValue()) {
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

			System.out.println("[" + goalCondition_ + "] " + currentEpisode_
					+ ": " + average);
			System.out.println();
		}



		if (!frozen_) {
			// Save files if necessary
			if ((localAgentObservations_.isSettled() && !oldAOSettled_)
					|| policyGenerator_.getPoliciesEvaluated()
							% ProgramArgument.PERFORMANCE_TESTING_SIZE
									.doubleValue() == 1) {
				performance_.saveFiles(this, elites_, currentEpisode_,
						policyGenerator_.hasUpdated(), false);
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
			File moduleFolder = getModFolder(goalCondition_.toString());
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
			File modFolder = getModFolder(goalCondition_.toString());
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

	@Override
	public String toString() {
		return goalCondition_.toString() + " Behaviour";
	}

	/**
	 * Basic method which fetches a module location for a given environment and
	 * local goal.
	 * 
	 * @param modName
	 *            The name of the module.
	 * 
	 * @return The File path to the module.
	 */
	public static File getModFolder(String modName) {
		File modDir = new File(MODULE_DIR);
		modDir.mkdir();
		File modFolder = new File(MODULE_DIR + File.separatorChar
				+ StateSpec.getInstance().getEnvironmentName());
		modFolder.mkdir();
		File goalModFolder = new File(modFolder, modName);
		goalModFolder.mkdir();
		if (ProgramArgument.SAVE_EXPERIMENT_FILES.booleanValue()) {
			String performanceFile = Config.getInstance().getPerformanceFile()
					.toString();
			performanceFile = performanceFile.substring(0,
					performanceFile.length() - 4);
			goalModFolder = new File(goalModFolder, performanceFile
					+ File.separator);
			goalModFolder.mkdir();
		}
		return goalModFolder;
	}

	/**
	 * Loads a module from the module directory.
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

		return loadDistribution(new File(getModFolder(moduleName), moduleName
				+ SERIALISED_SUFFIX));
	}

	/**
	 * Loads a serialised {@link LocalCrossEntropyDistribution} from file (if it
	 * exists).
	 * 
	 * @param serializedFile
	 *            The serialised file.
	 * @return The loaded distribution, or null.
	 */
	public static LocalCrossEntropyDistribution loadDistribution(
			File serializedFile) {
		try {
			if (serializedFile.exists()) {
				// The file exists!
				FileInputStream fis = new FileInputStream(serializedFile);
				ObjectInputStream ois = new ObjectInputStream(fis);
				LocalCrossEntropyDistribution lced = (LocalCrossEntropyDistribution) ois
						.readObject();
				ois.close();
				fis.close();

				// Load Local Agent Observations
				lced.localAgentObservations_ = LocalAgentObservations
						.loadAgentObservations(lced.goalCondition_);

				return lced;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Generates a unique policy ID (at least local to this unserialised run).
	 * 
	 * @return A String with a unique policy ID.
	 */
	public String generateUniquePolicyID() {
		return goalCondition_.toString() + "_" + policyIDCounter_++;
	}
}
