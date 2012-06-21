package cerrla;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import cerrla.modular.GeneralGoalCondition;
import cerrla.modular.GoalCondition;
import cerrla.modular.ModularPolicy;
import cerrla.modular.ModularSubGoal;
import cerrla.modular.PolicyItem;
import cerrla.modular.SpecificGoalCondition;

import relationalFramework.PolicyActions;
import relationalFramework.RelationalArgument;
import relationalFramework.StateSpec;
import rrlFramework.Config;
import rrlFramework.RRLActions;
import rrlFramework.RRLAgent;
import rrlFramework.RRLObservations;
import util.GoalConditionComparator;
import util.Recursive;

/**
 * The CERRLA agent. This uses a number of distributions of relational rules to
 * generate policies for effectively acting within environments. The
 * distributions are optimised using the cross-entropy method.
 * 
 * @author Sam Sarjant
 */
public class CERRLA implements RRLAgent {
	/** The current modular policy being tested per agent. */
	private Map<String, ModularPolicy> agentPolicy_;

	/** A map of agents that have started the episode (for mid-episode starts). */
	private Collection<String> startedAgents_;

	/** The set of learned behaviours the agent is maintaining concurrently. */
	private Map<GoalCondition, LocalCrossEntropyDistribution> goalMappedGenerators_;

	/** The CEDistribution for the environment goal. */
	private LocalCrossEntropyDistribution mainGoalCECortex_;

	/**
	 * 
	 * Inserts a modular policy within this policy.
	 * 
	 * @param subGoal
	 *            The sub-goal of the policy to fill.
	 * @param priorPolicies
	 *            The collection of previous policies that were recursively
	 *            added (to avoid infinite loops).
	 * @param moduleReplacementMap
	 *            The replacement map to use for the created modular policy.
	 * @param modularPolicy
	 *            The policy to add to.
	 * @param goalCondition
	 *            The goal condition to use.
	 * @param subGoalPolicies
	 *            The subgoal policies added to the main policy.
	 * @param priorSubGoals
	 *            The sub-goals which already have policies from earlier in the
	 *            policy.
	 */
	private void addModuleToPolicy(ModularSubGoal subGoal,
			Collection<GoalCondition> priorPolicies,
			Map<RelationalArgument, RelationalArgument> moduleReplacementMap,
			ModularPolicy modularPolicy,
			Collection<ModularPolicy> subGoalPolicies,
			SortedSet<GoalCondition> priorSubGoals) {
		GoalCondition subGoalCondition = subGoal.getGoalCondition();
		LocalCrossEntropyDistribution moduleDistribution = goalMappedGenerators_
				.get(subGoalCondition);

		// If there is an active module to replace this index, do so
		if (moduleDistribution != null
				&& moduleDistribution != mainGoalCECortex_
				&& !priorPolicies.contains(subGoalCondition)) {
			Collection<GoalCondition> modulePrior = new HashSet<GoalCondition>(
					priorPolicies);
			modulePrior.add(subGoalCondition);

			SortedSet<GoalCondition> priorSubGoalsLocal = new TreeSet<GoalCondition>(
					priorSubGoals);

			// If the condition is general, then also note that the opposite of
			// the general is used (aka always true when this is false)
			if (subGoalCondition instanceof GeneralGoalCondition) {
				modulePrior.add(((GeneralGoalCondition) subGoalCondition)
						.getNegation());
			}

			if (subGoalCondition instanceof SpecificGoalCondition) {
				// Apply the replacement map
				Map<RelationalArgument, RelationalArgument> newModuleReplacementMap = new HashMap<RelationalArgument, RelationalArgument>();
				ArrayList<RelationalArgument> args = ((SpecificGoalCondition) subGoalCondition)
						.getConstantArgs();
				for (int i = 0; i < args.size(); i++) {
					RelationalArgument goalTerm = RelationalArgument
							.createGoalTerm(i);
					RelationalArgument argTerm = args.get(i);
					if (moduleReplacementMap == null
							|| moduleReplacementMap.isEmpty())
						newModuleReplacementMap.put(goalTerm, argTerm);
					else
						newModuleReplacementMap.put(goalTerm,
								moduleReplacementMap.get(argTerm));
				}

				// Add the policy
				subGoal.setModularPolicy(regeneratePolicy(moduleDistribution,
						modulePrior, newModuleReplacementMap, subGoalPolicies, priorSubGoalsLocal));
			} else {
				subGoal.setModularPolicy(regeneratePolicy(moduleDistribution,
						modulePrior, moduleReplacementMap, subGoalPolicies, priorSubGoalsLocal));
			}
		}
	}

	/**
	 * Checks if a module needs to be learned. If so, it creates a new generator
	 * for the sub-goal which will be used/improved throughout learning.
	 */
	private void checkForModularGoals() {
		// Find the constants present in the rules of the main generator
		if (mainGoalCECortex_.getLocalAgentObservations().isSettled()) {
			for (GoalCondition gc : mainGoalCECortex_.getPotentialModuleGoals()) {
				// Check if the goal condition already exists
				if (!goalMappedGenerators_.containsKey(gc)) {
					// Attempt to load it
					LocalCrossEntropyDistribution loadedModule = LocalCrossEntropyDistribution
							.loadModule(Config.getInstance()
									.getEnvironmentClass(), gc);
					if (loadedModule != null)
						goalMappedGenerators_.put(gc, loadedModule);
					else {
						// The module may be learned
						LocalCrossEntropyDistribution newModule = new LocalCrossEntropyDistribution(
								gc);
						goalMappedGenerators_.put(gc, newModule);
					}
				}
			}
		}
	}

	/**
	 * Evaluates the policy but also notes which CEDistributions are currently
	 * being learned.
	 * 
	 * @param observations
	 *            The relational observations for this state.
	 * @return The actions the policy returned.
	 */
	private RRLActions evaluatePolicy(RRLObservations observations) {
		PolicyActions policyActions = agentPolicy_.get(
				observations.getAgentTurn()).evaluatePolicy(observations,
				StateSpec.getInstance().getNumReturnedActions());

		return new RRLActions(policyActions);
	}

	/**
	 * Recreates the current policy.
	 * 
	 * @param currentPolicy
	 *            The policy to recreate.
	 * @return The recreated policy. May or may not be the same object.
	 */
	private ModularPolicy recreateCurrentPolicy(
			Collection<ModularPolicy> existingPolicies) {
		// First, determine which policies already exist
		Collection<ModularPolicy> subGoalPolicies = new HashSet<ModularPolicy>();
		for (ModularPolicy modPol : existingPolicies)
			subGoalPolicies = modPol.getAllPolicies(true, subGoalPolicies);

		return regeneratePolicy(mainGoalCECortex_, null, null, subGoalPolicies,
				null);
	}

	/**
	 * Recursively (re)generates a modular and inspective policy which both
	 * combines existing modules and also examines states for new information.
	 * The policy may contain existing parts but also balnk parts to regenerate.
	 * 
	 * @param policyGenerator
	 *            The distribution to generate the policy.
	 * @param priorPolicies
	 *            The goal condition policies already recursively used.
	 * @param moduleReplacementMap
	 *            The modular replacement map
	 * @param subGoalPolicies
	 *            The subgoal policies already existing in the main policy.
	 * @return A modular policy, both utilising sub-behaviours and learning new
	 *         behaviour.
	 */
	@Recursive
	private ModularPolicy regeneratePolicy(
			LocalCrossEntropyDistribution policyGenerator,
			Collection<GoalCondition> priorPolicies,
			Map<RelationalArgument, RelationalArgument> moduleReplacementMap,
			Collection<ModularPolicy> subGoalPolicies,
			SortedSet<GoalCondition> priorSubGoals) {
		// Initialise null collections
		if (priorPolicies == null)
			priorPolicies = new HashSet<GoalCondition>();
		if (subGoalPolicies == null)
			subGoalPolicies = new HashSet<ModularPolicy>();
		if (priorSubGoals == null)
			priorSubGoals = new TreeSet<GoalCondition>(
					new GoalConditionComparator());

		// Build the module parameter replacement map
		ModularPolicy modularPolicy = policyGenerator
				.generatePolicy(subGoalPolicies);
		modularPolicy.setModularParameters(moduleReplacementMap);
		subGoalPolicies.add(modularPolicy);

		// Add all rules in the basic policy
		for (Iterator<PolicyItem> iter = modularPolicy.getRules().iterator(); iter
				.hasNext();) {
			PolicyItem reo = iter.next();
			// If there is a modular sub-goal that needs regenerating
			if (reo.shouldRegenerate()) {
				ModularSubGoal subGoal = (ModularSubGoal) reo;
				GoalCondition gc = subGoal.getGoalCondition();
				if (!priorSubGoals.contains(gc)) {
					addModuleToPolicy(subGoal, priorPolicies,
							moduleReplacementMap, modularPolicy,
							subGoalPolicies, priorSubGoals);
					priorSubGoals.add(subGoal.getGoalCondition());
				}
			}
		}

		return modularPolicy;
	}

	@Override
	public void cleanup() {
		// Save the final output
		for (LocalCrossEntropyDistribution lced : goalMappedGenerators_
				.values()) {
			if (lced == mainGoalCECortex_)
				lced.finalWrite();
			else
				lced.saveModule();
		}

		mainGoalCECortex_ = null;
		goalMappedGenerators_.clear();
		agentPolicy_.clear();
		startedAgents_.clear();
	}

	@Override
	public void endEpisode(RRLObservations observations) {
		// End the episode for ALL players
		for (String player : agentPolicy_.keySet()) {
			ModularPolicy currentPolicy = agentPolicy_.get(player);
			currentPolicy.noteStepReward(observations.getRewards(player));
			boolean regeneratePolicy = currentPolicy.endEpisode();
			if (regeneratePolicy)
				agentPolicy_.put(player,
						recreateCurrentPolicy(agentPolicy_.values()));
		}
		startedAgents_.clear();
	}

	@Override
	public void freeze(boolean b) {
		for (LocalCrossEntropyDistribution distribution : goalMappedGenerators_
				.values())
			distribution.freeze(b);
	}

	@Override
	public void initialise(int run) {
		goalMappedGenerators_ = new HashMap<GoalCondition, LocalCrossEntropyDistribution>();
		GoalCondition mainGC = Config.getInstance().getGoal();
		if (Config.getInstance().getSerializedFile() != null) {
			mainGoalCECortex_ = LocalCrossEntropyDistribution
					.loadDistribution(Config.getInstance().getSerializedFile());
			if (mainGoalCECortex_ != null)
				mainGoalCECortex_.getGoalCondition().setAsMainGoal();
			else
				System.out
						.println("No serialised file found. Using new distribution.");
		}
		if (mainGoalCECortex_ == null)
			mainGoalCECortex_ = new LocalCrossEntropyDistribution(mainGC, run);
		goalMappedGenerators_.put(mainGC, mainGoalCECortex_);
		agentPolicy_ = new HashMap<String, ModularPolicy>();
		startedAgents_ = new HashSet<String>();
	}

	@Override
	public boolean isLearningComplete() {
		return mainGoalCECortex_.isLearningComplete();
	}

	@Override
	public RRLActions startEpisode(RRLObservations observations) {
		// Check for module stuff
		if (ProgramArgument.USE_MODULES.booleanValue())
			checkForModularGoals();

		String playerID = observations.getAgentTurn();
		startedAgents_.add(playerID);
		ModularPolicy currentPolicy = agentPolicy_.get(playerID);
		if (currentPolicy == null || currentPolicy.shouldRegenerate()) {
			// Generate a new policy
			currentPolicy = recreateCurrentPolicy(agentPolicy_.values());
			agentPolicy_.put(playerID, currentPolicy);
		}

		currentPolicy.startEpisode();

		if (currentPolicy.isFresh()) {
			if (ProgramArgument.SYSTEM_OUTPUT.booleanValue()) {
				System.out.println();
				if (playerID.equals(RRLObservations.ALL_PLAYERS))
					System.out.println(currentPolicy);
				else
					System.out.println(playerID + ": " + currentPolicy);
			}
		}

		// If the main generator is frozen, freeze the others too.
		for (LocalCrossEntropyDistribution dist : goalMappedGenerators_
				.values()) {
			if (dist != mainGoalCECortex_) {
				// If the main goal is frozen, freeze the others too.
				if (mainGoalCECortex_.isFrozen() && !dist.isConverged())
					dist.freeze(true);
			}
		}

		currentPolicy.parameterArgs(observations.getGoalReplacements());

		return evaluatePolicy(observations);
	}

	@Override
	public RRLActions stepEpisode(RRLObservations observations) {
		String playerID = observations.getAgentTurn();
		// If agent hasn't started, do that.
		if (!startedAgents_.contains(playerID))
			return startEpisode(observations);

		// Note the reward for the relevant distributions
		agentPolicy_.get(playerID).noteStepReward(
				observations.getRewards(playerID));

		// Evaluate the policy.
		return evaluatePolicy(observations);
	}

}
