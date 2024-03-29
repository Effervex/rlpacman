/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/cerrla/LocalCrossEntropyDistribution.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package cerrla;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.BidiMap;

import cerrla.modular.GoalCondition;
import cerrla.modular.ModularPolicy;
import cerrla.modular.ModularSubGoal;
import cerrla.modular.PolicyItem;
import cerrla.modular.SpecificGoalCondition;

import jess.QueryResult;
import jess.Rete;
import jess.ValueVector;

import relationalFramework.RelationalArgument;
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
 * and performance observations relating to this behaviour. This class is
 * essentially the core of CERRLA.
 * 
 * @author Sam Sarjant
 */
public class LocalCrossEntropyDistribution implements Serializable,
		ModularBehaviour {

	/** The collection of non existant modules. */
	private static final Collection<String> nonExistantModule_ = new HashSet<String>();

	private static final long serialVersionUID = 6883881456264179505L;

	public static final String MODULAR_SUFFIX = ".mod";

	/** The relative directory in which modules are stored. */
	public static final String MODULE_DIR = "modules";

	/** The suffix for module files. */
	public static final String SERIALISED_SUFFIX = ".ser";

	/** Best policy to be tested */
	private ModularPolicy bestPolicy_;

	/** The current best policy testing episode. */
	private transient int bestPolicyEpisode_ = -1;

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

	/** If this generator is not specialising new rules. */
	private transient boolean isSpecialising_;

	/** The localised agent observations for this goal. */
	private transient LocalAgentObservations localAgentObservations_;

	/** The minimum number of elites value. */
	private int numElites_ = Integer.MAX_VALUE;

	/** If the AgentObsrvations were settled last episode. */
	private boolean oldAOSettled_;

	/** If the policy generator has updated yet. */
	private boolean oldUpdated_;

	/** The performance object, noting figures. */
	private final Performance performance_;

	/** The distributions of rules. */
	private final PolicyGenerator policyGenerator_;

	/**
	 * The ID counter for making each policy unique, even if it has the same
	 * rules.
	 */
	private transient int policyIDCounter_;

	/** The population value. */
	private int population_ = Integer.MAX_VALUE;

	/** A map of sub-goal distributions and the last time they were encountered. */
	private transient Map<LocalCrossEntropyDistribution, Integer> relevantSubDistEpisodeMap_;

	/** The state of the algorithm. */
	private transient AlgorithmState state_ = AlgorithmState.TRAINING;

	/** The current testing episode. */
	private transient int testEpisode_;

	/** A stack of policies that have not been tested fully. */
	private transient Queue<ModularPolicy> undertestedPolicies_;

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
		boolean modular = !goal.isMainGoal();
		if (!modular)
			goalCondition_ = goal;
		else {
			goalCondition_ = goal.clone();
			if (goal instanceof SpecificGoalCondition)
				((SpecificGoalCondition) goalCondition_).normaliseArgs();
		}

		policyGenerator_ = new PolicyGenerator(this);
		if (modular)
			performance_ = new Performance(true, run);
		else
			performance_ = new Performance(run);
		elites_ = new TreeSet<PolicyValue>();

		undertestedPolicies_ = new LinkedList<ModularPolicy>();

		// Load the local agent observations
		localAgentObservations_ = LocalAgentObservations.loadAgentObservations(
				goalCondition_, this);
		policyIDCounter_ = 0;
		isSpecialising_ = true;

		// Load in RLGG rules and mutate them if possible
		Collection<RelationalRule> covered = localAgentObservations_
				.getRLGGRules(new HashSet<RelationalRule>());
		if (!covered.isEmpty()) {
			policyGenerator_.addRLGGRules(covered);
			policyGenerator_.mutateRLGGRules();
		}

		if (Config.getInstance() != null) {
			File seedRules = Config.getInstance().getSeedRuleFile();
			if (seedRules != null)
				policyGenerator_.seedRules(seedRules);
		}

		setState(AlgorithmState.TRAINING);
	}

	/**
	 * If the sample being recorded is a valid sample (consists of current
	 * rules).
	 * 
	 * @param sample
	 *            The sample being recorded.
	 * @return True if the sample contains only valid rules, false if it
	 *         contains old, invalid rules.
	 */
	private boolean isValidSample(ModularPolicy sample, boolean checkFired) {
		// If no rules fired, this sample is invalid.
		if (checkFired && sample.getFiringRules().isEmpty())
			return false;
		// Check each rule for validity
		for (PolicyItem pi : sample.getRules()) {
			if (pi instanceof RelationalRule) {
				// Check that the rule is valid within this distribution
				if (!policyGenerator_.isRuleExists((RelationalRule) pi))
					return false;
			} else if (pi instanceof ModularSubGoal) {
				ModularPolicy innerModPol = ((ModularSubGoal) pi)
						.getModularPolicy();
				if (innerModPol != null
						&& !innerModPol.getLocalCEDistribution().isValidSample(
								innerModPol, false))
					return false;
			}
		}
		return true;
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

		// Make a backup - just in case the elites are empty afterwards
		SortedSet<PolicyValue> backup = new TreeSet<PolicyValue>(elites);

		// Only remove stuff if the elites are a representative solution
		if (!ProgramArgument.GLOBAL_ELITES.booleanValue()) {
			int iteration = policyGenerator_.getPoliciesEvaluated();
			for (Iterator<PolicyValue> iter = elites.iterator(); iter.hasNext();) {
				PolicyValue pv = iter.next();
				if (iteration - pv.getIteration() >= staleValue) {
					if (ProgramArgument.RETEST_STALE_POLICIES.booleanValue())
						policyGenerator_.retestPolicy(pv.getPolicy());
					iter.remove();
				}
			}
		}
		if (elites.isEmpty())
			elites.addAll(backup);

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
				goalRule_ = new RelationalRule(conditions, null, null, this);
				if (!goalRule_.isLegal())
					System.err.println("Illegal goal condition: " + conditions);
			}
		} else
			goalRule_ = null;

		try {
			// Assign the parameters
			Rete state = observations.getState();
			// System.out.println(StateSpec.extractFacts(state));
			ValueVector vv = new ValueVector();
			goalRule_.setParameters(goalReplacements);
			if (goalRule_.getParameters() != null)
				for (RelationalArgument param : goalRule_.getParameters())
					vv.add(param.toString());

			// Run the query
			String query = StateSpec.getInstance().getRuleQuery(goalRule_,
					false);
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
	 * @param population
	 *            The population size.
	 * @param numElites
	 *            The minimum number of elites.
	 */
	private void updateDistributions(SortedSet<PolicyValue> elites,
			int population, int numElites) {
		if (population == 0)
			return;

		double minReward = performance_.getMinimumReward();

		// Clean up the policy values
		SortedSet<PolicyValue> removed = preUpdateModification(elites,
				numElites, population, minReward);

		ElitesData ed = policyGenerator_.updateDistributions(elites,
				ProgramArgument.ALPHA.doubleValue(), numElites, population,
				minReward);
		if (ed != null)
			performance_.noteElitesReward(currentEpisode_,
					ed.getMeanEliteValue(), ed.getMaxEliteValue());

		// Negative updates:
		if (ProgramArgument.NEGATIVE_UPDATES.booleanValue())
			policyGenerator_.updateNegative(elites,
					ProgramArgument.ALPHA.doubleValue(), population, numElites,
					removed);

		// Run the post update operations
		boolean newSlotCreated = policyGenerator_
				.postUpdateOperations(numElites);
		if (ProgramArgument.RESET_ELITES.booleanValue() && newSlotCreated)
			elites.clear();
	}

	public void cleanup() {
		if (!ProgramArgument.LOAD_AGENT_OBSERVATIONS.booleanValue())
			localAgentObservations_.cleanup();
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

		if (!frozen_) {
			if (goalCondition_.isMainGoal()) {
				// Test the learned behaviour
				System.out.println();
				System.out.println("Beginning [" + goalCondition_
						+ "] testing for episode " + currentEpisode_ + ".");

				System.out.println();
				if (!ProgramArgument.SYSTEM_OUTPUT.booleanValue())
					System.out.println("Testing...");
			}

			// Determine best elite sample
			if (Config.getInstance().getGeneratorFile() == null)
				bestPolicy_ = getBestElite();
		}

		setState(AlgorithmState.TESTING);
	}

	/**
	 * Generates a policy from the current distribution.
	 * 
	 * @param existingSubGoals
	 *            A collection of all existing sub-goals in the parent policy
	 *            this policy is to be put into.
	 * @return A newly generated policy from the current distribution.
	 */
	public ModularPolicy generatePolicy(
			Collection<ModularPolicy> existingSubGoals) {
		// If testing greedy policies
		if (Config.getInstance().getGeneratorFile() != null) {
			if (bestPolicy_ == null
					|| testEpisode_ >= ProgramArgument.TEST_ITERATIONS
							.intValue()) {
				SortedMap<Integer, RelationalPolicy> greedyPolicies = policyGenerator_
						.getGreedyPolicyMap();
				SortedMap<Integer, RelationalPolicy> nextKey = greedyPolicies
						.tailMap(currentEpisode_ + 1);

				if (ProgramArgument.TESTING.booleanValue()) {
					currentEpisode_ = greedyPolicies.lastKey();
					bestPolicy_ = new ModularPolicy(
							greedyPolicies.get(currentEpisode_), this);
					testEpisode_ = 0;
				} else if (nextKey == null || nextKey.isEmpty()) {
					// End of testing. Exit.
					bestPolicyEpisode_ = ProgramArgument.TEST_ITERATIONS
							.intValue();
				} else {
					// Next policy and next episode.
					currentEpisode_ = nextKey.firstKey();
					bestPolicy_ = new ModularPolicy(
							greedyPolicies.get(currentEpisode_), this);
					testEpisode_ = 0;
				}
			}

			bestPolicy_.clearPolicyRewards();
			return bestPolicy_;
		}

		if (frozen_ && state_ == AlgorithmState.BEST_POLICY) {
			bestPolicy_.clearPolicyRewards();
			return bestPolicy_;
		}

		// Initialise undertested
		if (undertestedPolicies_ == null)
			undertestedPolicies_ = new LinkedList<ModularPolicy>();

		// If there remains an undertested policy not already in the parent
		// policy, use that
		for (Iterator<ModularPolicy> iter = undertestedPolicies_.iterator(); iter
				.hasNext();) {
			ModularPolicy undertested = iter.next();
			if (undertested.shouldRegenerate()
					|| !isValidSample(undertested, false))
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
		RelationalPolicy newPol = policyGenerator_.generatePolicy(true, false);
		ModularPolicy newModPol = null;
		if (newPol instanceof ModularPolicy)
			newModPol = new ModularPolicy((ModularPolicy) newPol);
		else
			newModPol = new ModularPolicy(newPol, this);
		undertestedPolicies_.add(newModPol);
		return newModPol;
	}

	/**
	 * Generates a unique policy ID (at least local to this unserialised run).
	 * 
	 * @return A String with a unique policy ID.
	 */
	public String generateUniquePolicyID() {
		return goalCondition_.toString() + "_" + policyIDCounter_++;
	}

	/**
	 * Gets the best (or majority, if equally valued) elite sample from the
	 * current elite samples.
	 * 
	 * @return The best elite sample or null if empty.
	 */
	public ModularPolicy getBestElite() {
		if (elites_.isEmpty())
			return null;

		Map<ModularPolicy, Integer> policyCount = new HashMap<ModularPolicy, Integer>();
		double bestValue = elites_.first().getValue();
		ModularPolicy bestPol = null;
		int largestCount = 0;
		for (PolicyValue pv : elites_) {
			if (pv.getValue() == bestValue) {
				Integer count = policyCount.get(pv.getPolicy());
				if (count == null)
					count = 0;
				count++;
				policyCount.put(pv.getPolicy(), count);

				if (count > largestCount) {
					largestCount = count;
					bestPol = pv.getPolicy();
				}
			} else
				break;
		}

		bestPol = new ModularPolicy(bestPol);
		bestPol.clearChildren();
		return bestPol;
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
	 * Gets the number of policy repeats policies should be performing.
	 * 
	 * @return The number of times a policy should be tested.
	 */
	public int getPolicyRepeats() {
		// If frozen, only once
		if (frozen_)
			return 1;

		// If modular policy, test twice as long
		if (!goalCondition_.isMainGoal())
			return ProgramArgument.POLICY_REPEATS.intValue() * 2;
		return ProgramArgument.POLICY_REPEATS.intValue();
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

	public AlgorithmState getState() {
		return state_;
	}

	/**
	 * Checks if this CE distribution is converged.
	 * 
	 * @return True if the distribution is converged.
	 */
	public boolean isConverged() {
		// If there are a finite number of episodes, do not converge
		if (Config.getInstance().getMaxEpisodes() != -1)
			return false;

		// Only converged if the relevant sub-goals are converged
		if (relevantSubDistEpisodeMap_ == null)
			relevantSubDistEpisodeMap_ = new HashMap<LocalCrossEntropyDistribution, Integer>();
		for (LocalCrossEntropyDistribution subGoal : relevantSubDistEpisodeMap_
				.keySet()) {
			if (currentEpisode_ - relevantSubDistEpisodeMap_.get(subGoal) < population_
					&& !subGoal.isConverged())
				return false;
		}

		// Check elite convergence
		if (ProgramArgument.ELITES_CONVERGENCE.booleanValue()
				&& !elites_.isEmpty()
				&& elites_.size() >= population_
						* (1 - ProgramArgument.RHO.doubleValue())
				&& elites_.first().getValue() == elites_.last().getValue()
				&& elites_.first().getValue() > performance_.getMinimumReward())
			return true;

		// Check distribution convergence (Need at least N samples)
		if (currentEpisode_ >= population_
				* ProgramArgument.POLICY_REPEATS.intValue()
				&& policyGenerator_.isConverged())
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
				&& testEpisode_ >= ProgramArgument.TEST_ITERATIONS.intValue()
				&& bestPolicyEpisode_ >= ProgramArgument.TEST_ITERATIONS
						.intValue();
	}

	public boolean isSpecialising() {
		return isSpecialising_;
	}

	/**
	 * Records a given sample with a given reward.
	 * 
	 * @param sample
	 *            The sample being recorded.
	 * @param policyRewards
	 *            The internal and environmental rewards the policy has
	 *            received.
	 */
	public void recordSample(ModularPolicy sample,
			ArrayList<double[]> policyRewards) {
		// Performance
		if (!frozen_)
			currentEpisode_ += policyRewards.size();
		double average = 0;
		if (!goalCondition_.isMainGoal()
				|| state_ != AlgorithmState.BEST_POLICY) {
			average = performance_.noteSampleRewards(policyRewards,
					currentEpisode_);
		} else {
			average = performance_.noteBestPolicyValue(policyRewards);
		}

		// Mutate new rules, if necessary.
		policyGenerator_.mutateRLGGRules();

		// Calculate the population and number of elites
		population_ = policyGenerator_.determinePopulation();
		numElites_ = (int) Math.ceil(population_
				* ProgramArgument.RHO.doubleValue());
		if (!frozen_) {
			// Add sample to elites
			if (isValidSample(sample, true)) {
				PolicyValue pv = new PolicyValue(sample, average,
						policyGenerator_.getPoliciesEvaluated());
				elites_.add(pv);
			}
			policyGenerator_.incrementPoliciesEvaluated();

			// Update distributions (depending on number of elites)
			updateDistributions(elites_, population_, numElites_);

			// TODO Change this to only note if sub-goal dists are USED.
			// Noting relevant sub-goal distributions
			if (relevantSubDistEpisodeMap_ == null)
				relevantSubDistEpisodeMap_ = new HashMap<LocalCrossEntropyDistribution, Integer>();

			Collection<ModularPolicy> subPols = sample.getAllPolicies(false,
					true, null);
			for (ModularPolicy subPol : subPols) {
				if (subPol != sample)
					relevantSubDistEpisodeMap_.put(
							subPol.getLocalCEDistribution(), currentEpisode_);
			}
		}

		// Estimate experiment convergence
		double convergence = policyGenerator_.getConvergenceValue();
		if (frozen_) {
			if (state_ == AlgorithmState.TESTING) {
				testEpisode_++;
				convergence = testEpisode_
						/ ProgramArgument.TEST_ITERATIONS.doubleValue();
				if (testEpisode_ == ProgramArgument.TEST_ITERATIONS
						.doubleValue()) {
					if (Config.getInstance().getGeneratorFile() == null) {
						if (goalCondition_.isMainGoal()) {
							// End of testing, test best policy
							System.out.println("Beginning [" + goalCondition_
									+ "] BEST POLICY testing for episode "
									+ currentEpisode_ + ".");
							setState(AlgorithmState.BEST_POLICY);
						}
					} else {
						// End of this greedy generator test
						performance_.recordPerformanceScore(currentEpisode_);
						performance_.freeze(true);
					}
				}
			} else if (goalCondition_.isMainGoal()) {
				bestPolicyEpisode_++;
				convergence = bestPolicyEpisode_
						/ ProgramArgument.TEST_ITERATIONS.doubleValue();
			}
		}
		int numSlots = policyGenerator_.size();
		performance_.estimateETA(convergence, numElites_, elites_, numSlots,
				goalCondition_);

		// Output system output
		if (ProgramArgument.SYSTEM_OUTPUT.booleanValue()) {
			System.out.println("[" + goalCondition_ + "] " + currentEpisode_
					+ ": " + average);
			System.out.println();
		}



		if (!frozen_) {
			performance_.noteGeneratorDetails(currentEpisode_,
					policyGenerator_, population_, convergence);

			// Save files if necessary
			if ((localAgentObservations_.isSettled() && !oldAOSettled_)
					|| (policyGenerator_.hasUpdated() && !oldUpdated_)
					|| policyGenerator_.getPoliciesEvaluated()
							% ProgramArgument.PERFORMANCE_TESTING_SIZE
									.doubleValue() == 1) {
				performance_.saveFiles(this, elites_, currentEpisode_,
						policyGenerator_.hasUpdated(), false);
				if (goalCondition_.isMainGoal()
						&& policyGenerator_.getPoliciesEvaluated()
								% ProgramArgument.PERFORMANCE_TESTING_SIZE
										.doubleValue() == 1) {
					StateSpec.reinitInstance(false);
				}
			}

			oldAOSettled_ = localAgentObservations_.isSettled();
			oldUpdated_ = policyGenerator_.hasUpdated();
		}
	}

	/**
	 * Serialises this generator to file.
	 * 
	 * @param serFile
	 *            The file to serialise to.
	 * @param saveEnvAgentObservations
	 *            If the environment's agent observations should be saved also.
	 * @param run
	 *            The current experiment run.
	 */
	public void saveCEDistribution(File serFile,
			boolean saveEnvAgentObservations, int run) {
		try {
			// Write the main behaviour to temp and module
			if (goalCondition_.isMainGoal()) {
				FileOutputStream fos = new FileOutputStream(serFile);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(this);
				oos.close();
			}

			// Also note Local Agent Observations (as they are transient)
			localAgentObservations_
					.saveLocalObservations(saveEnvAgentObservations);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Saves the best behaviour to a text file as a static module.
	 * 
	 * @param run
	 *            The run this module is saved for.
	 */
	public void saveModule(int run) {
		// Don't save the main goal as a module.
		if (goalCondition_.isMainGoal() || !isConverged())
			return;

		try {
			File modFolder = getModFolder(goalCondition_.toString(), run);
			File genFile = new File(modFolder, goalCondition_
					+ SERIALISED_SUFFIX);
			genFile.createNewFile();

			saveCEDistribution(genFile, false, run);

			File modFile = new File(modFolder, goalCondition_ + MODULAR_SUFFIX);
			modFile.createNewFile();

			policyGenerator_.saveModule(modFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setSpecialisation(boolean b) {
		isSpecialising_ = b;
	}

	/**
	 * Sets the state of this generator and all others.
	 * 
	 * @param state
	 *            The state this generator is set to.
	 */
	public void setState(AlgorithmState state) {
		if (state_ != null && state_.equals(state))
			return;

		state_ = state;
		switch (state) {
		case BEST_POLICY:
			if (!frozen_) {
				frozen_ = true;
				policyGenerator_.freeze(true);
				performance_.freeze(true);
			}
			bestPolicyEpisode_ = 0;
			break;
		case TESTING:
			if (!frozen_) {
				frozen_ = true;
				policyGenerator_.freeze(true);
				performance_.freeze(true);
			}
			testEpisode_ = 0;
			bestPolicyEpisode_ = -1;
			break;
		case TRAINING:
			if (frozen_) {
				frozen_ = false;
				policyGenerator_.freeze(false);
				performance_.freeze(false);
			}
			break;
		}
	}

	/**
	 * Simply resets episode reward.
	 */
	public void startEpisode() {
		// Check for convergence
		if (isConverged())
			freeze(true);
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
	 * @param run
	 *            The current experiment run.
	 * 
	 * @return The File path to the module.
	 */
	public static File getModFolder(String modName, int run) {
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
					performanceFile.length() - 4)
					+ run;
			goalModFolder = new File(goalModFolder, performanceFile
					+ File.separator);
			goalModFolder.mkdir();
		}
		return goalModFolder;
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
						.loadAgentObservations(lced.getGoalCondition(), lced);
				lced.policyGenerator_.rebuildCurrentData();
				lced.isSpecialising_ = true;
				lced.setState(AlgorithmState.TRAINING);

				return lced;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
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

		if (!ProgramArgument.SAVE_EXPERIMENT_FILES.booleanValue())
			return loadDistribution(new File(getModFolder(moduleName, 0),
					moduleName + SERIALISED_SUFFIX));
		return null;
	}

	public enum AlgorithmState {
		BEST_POLICY, TESTING, TRAINING;
	}
}
