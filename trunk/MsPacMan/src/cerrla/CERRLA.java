package cerrla;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import relationalFramework.GoalCondition;
import relationalFramework.ModularPolicy;
import relationalFramework.PolicyActions;
import relationalFramework.RelationalArgument;
import relationalFramework.RelationallyEvaluatableObject;
import relationalFramework.StateSpec;
import rrlFramework.Config;
import rrlFramework.RRLActions;
import rrlFramework.RRLAgent;
import rrlFramework.RRLObservations;
import util.Recursive;

/**
 * The CERRLA agent. This uses a number of distributions of relational rules to
 * generate policies for effectively acting within environments. The
 * distributions are optimised using the cross-entropy method.
 * 
 * @author Sam Sarjant
 */
public class CERRLA implements RRLAgent {
	/** The modular policy being tested. */
	private ModularPolicy currentPolicy_;

	/** The current run this learner was initialised with. */
	private int currentRun_;

	/** The set of learned behaviours the agent is maintaining concurrently. */
	private Map<GoalCondition, LocalCrossEntropyDistribution> goalMappedGenerators_;

	/** The CEDistribution for the environment goal. */
	private LocalCrossEntropyDistribution mainGoalCECortex_;

	/**
	 * 
	 * Inserts a modular policy within this policy.
	 * 
	 * @param index
	 *            The index of the module to replace.
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
	 */
	private void addModuleToPolicy(int index,
			Collection<GoalCondition> priorPolicies,
			Map<String, String> moduleReplacementMap,
			ModularPolicy modularPolicy, GoalCondition goalCondition,
			Collection<ModularPolicy> subGoalPolicies) {
		LocalCrossEntropyDistribution moduleDistribution = goalMappedGenerators_
				.get(goalCondition);
		// If there is an active module to replace this index, do so
		if (moduleDistribution != null
				&& moduleDistribution != mainGoalCECortex_
				&& !priorPolicies.contains(goalCondition)) {
			Collection<GoalCondition> modulePrior = new HashSet<GoalCondition>(
					priorPolicies);
			modulePrior.add(goalCondition);

			// Apply the replacement map
			Map<String, String> newModuleReplacementMap = new HashMap<String, String>();
			ArrayList<String> args = goalCondition.getConstantArgs();
			for (int i = 0; i < args.size(); i++) {
				String goalTerm = RelationalArgument.createGoalTerm(i);
				String argTerm = args.get(i);
				if (moduleReplacementMap == null
						|| moduleReplacementMap.isEmpty())
					newModuleReplacementMap.put(goalTerm, argTerm);
				else
					newModuleReplacementMap.put(goalTerm,
							moduleReplacementMap.get(argTerm));
			}

			// Add the policy
			modularPolicy.replaceIndex(
					index,
					regeneratePolicy(moduleDistribution, modulePrior,
							newModuleReplacementMap, subGoalPolicies));
		} else {
			// Otherwise, insert a ModularHole
			modularPolicy.replaceIndex(index, new ModularHole(goalCondition));
		}
	}

	/**
	 * Checks if a module needs to be learned. If so, it creates a new generator
	 * for the sub-goal which will be used/improved throughout learning.
	 */
	private void checkForModularLearning() {
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
		PolicyActions policyActions = currentPolicy_.evaluatePolicy(
				observations, StateSpec.getInstance().getNumReturnedActions());

		return new RRLActions(policyActions);
	}

	/**
	 * Recreates the current policy.
	 */
	private void recreateCurrentPolicy() {
		// First, determine which policies already exist
		Collection<ModularPolicy> subGoalPolicies = new HashSet<ModularPolicy>();
		if (currentPolicy_ != null)
			subGoalPolicies = currentPolicy_.getAllPolicies(true,
					subGoalPolicies);

		currentPolicy_ = regeneratePolicy(mainGoalCECortex_, null, null,
				subGoalPolicies);
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
			Map<String, String> moduleReplacementMap,
			Collection<ModularPolicy> subGoalPolicies) {
		// Initialise null collections
		if (priorPolicies == null)
			priorPolicies = new HashSet<GoalCondition>();
		if (subGoalPolicies == null)
			subGoalPolicies = new HashSet<ModularPolicy>();

		// Build the module parameter replacement map
		ModularPolicy modularPolicy = policyGenerator
				.generatePolicy(subGoalPolicies);
		modularPolicy.setModularParameters(moduleReplacementMap);
		subGoalPolicies.add(modularPolicy);

		// TODO Disallow duplicate rules from policies - they have no use.
		// Add all rules in the basic policy
		int i = 0;
		for (Iterator<RelationallyEvaluatableObject> iter = modularPolicy
				.getRules().iterator(); iter.hasNext();) {
			RelationallyEvaluatableObject reo = iter.next();
			if (reo.shouldRegenerate()) {
				addModuleToPolicy(i, priorPolicies, moduleReplacementMap,
						modularPolicy, reo.getGoalCondition(), subGoalPolicies);
			} else if (reo instanceof ModularPolicy)
				subGoalPolicies.add((ModularPolicy) reo);
			i++;
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
	}

	@Override
	public void endEpisode(RRLObservations observations) {
		// Check for restart flag
		if (currentPolicy_.shouldRestart()) {
			currentPolicy_ = null;
			return;
		}

		// Note figures
		currentPolicy_.noteStepReward(observations.getReward());
		boolean regeneratePolicy = currentPolicy_.endEpisode();
		if (regeneratePolicy)
			recreateCurrentPolicy();
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
		mainGoalCECortex_ = new LocalCrossEntropyDistribution(mainGC, run);
		goalMappedGenerators_.put(mainGC, mainGoalCECortex_);
		currentPolicy_ = null;
		currentRun_ = run;
	}

	@Override
	public boolean isLearningComplete() {
		return mainGoalCECortex_.isLearningComplete();
	}

	@Override
	public RRLActions startEpisode(RRLObservations observations) {
		// Check for module stuff
		if (ProgramArgument.USE_MODULES.booleanValue())
			checkForModularLearning();

		if (currentPolicy_ == null || currentPolicy_.shouldRegenerate()) {
			// Generate a new policy
			recreateCurrentPolicy();
		}

		if (currentPolicy_.isFresh()) {
			if (ProgramArgument.SYSTEM_OUTPUT.booleanValue()) {
				System.out.println();
				System.out.println(currentPolicy_);
			}
		}

		currentPolicy_.startEpisode();

		// Start the episode for each contained behaviour.
		// TODO Deal with frozen distribution, etc.
		// mainGoalCECortex_.startEpisode();
		// for (LocalCrossEntropyDistribution dist : currentPolicy_
		// .getRelevantCEDistributions()) {
		// if (dist != mainGoalCECortex_) {
		// dist.startEpisode();
		// // If the main goal is frozen, freeze the others too.
		// if (mainGoalCECortex_.isFrozen() && !dist.isFrozen())
		// dist.freeze(true);
		// }
		// }

		currentPolicy_.parameterArgs(observations.getGoalReplacements());

		return evaluatePolicy(observations);
	}

	@Override
	public RRLActions stepEpisode(RRLObservations observations) {
		// Note the reward for the relevant distributions
		currentPolicy_.noteStepReward(observations.getReward());

		// Evaluate the policy.
		return evaluatePolicy(observations);
	}

}
