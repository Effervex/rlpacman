package cerrla;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import relationalFramework.GoalCondition;
import relationalFramework.ModularPolicy;
import relationalFramework.PolicyActions;
import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalRule;
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
	/** The CEDistribution for the environment goal. */
	private LocalCrossEntropyDistribution mainGoalCECortex_;

	/** The modular policy being tested. */
	private ModularPolicy currentPolicy_;

	/** Notes the generators that will be involved in the update process. */
	// private Collection<LocalCrossEntropyDistribution> relevantDistributions_;

	/** The set of learned behaviours the agent is maintaining concurrently. */
	private Map<GoalCondition, LocalCrossEntropyDistribution> goalMappedGenerators_;

	/** The number of episodes the policy has been involved in. */
	private int policyEpisodes_;

	/** The current run this learner was initialised with. */
	private int currentRun_;

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
		policyEpisodes_++;
		currentPolicy_.noteStepReward(observations.getReward());
		currentPolicy_.endEpisode();

		if (policyEpisodes_ >= ProgramArgument.POLICY_REPEATS.intValue()) {
			currentPolicy_.recordSample();
			currentPolicy_ = null;

			if (ProgramArgument.SYSTEM_OUTPUT.booleanValue())
				System.out.println();
		}

		// Extra lines
		if (policyEpisodes_ >= ProgramArgument.POLICY_REPEATS.intValue()
				&& ProgramArgument.SYSTEM_OUTPUT.booleanValue()) {
			System.out.println();
		}
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
		policyEpisodes_ = 0;
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

		if (currentPolicy_ == null) {
			// Generate a new policy
			currentPolicy_ = generatePolicy(mainGoalCECortex_, null, null);
			if (ProgramArgument.SYSTEM_OUTPUT.booleanValue()) {
				System.out.println(currentPolicy_);
				System.out.println();
			}
			policyEpisodes_ = 0;
		}

		currentPolicy_.startEpisode();

		// Start the episode for each contained behaviour.
		// TODO Deal with frozen distribution, etc.
		mainGoalCECortex_.startEpisode();
		for (LocalCrossEntropyDistribution dist : currentPolicy_
				.getRelevantCEDistributions()) {
			if (dist != mainGoalCECortex_) {
				dist.startEpisode();
				// If the main goal is frozen, freeze the others too.
				if (mainGoalCECortex_.isFrozen() && !dist.isFrozen())
					dist.freeze(true);
			}
		}

		currentPolicy_.parameterArgs(observations.getGoalReplacements());

		return evaluatePolicy(observations);
	}

	/**
	 * Recursively generates a modular and inspective policy which both combines
	 * existing modules and also examines states for new information.
	 * 
	 * @param policyGenerator
	 *            The distribution to generate the policy.
	 * @param priorPolicies
	 *            The goal condition policies already recursively used.
	 * @param moduleParams
	 *            The modular parameters to link this policy to the rule which
	 *            fired it.
	 * @return A modular policy, both utilising sub-behaviours and learning new
	 *         behaviour.
	 */
	@Recursive
	private ModularPolicy generatePolicy(
			LocalCrossEntropyDistribution policyGenerator,
			Collection<GoalCondition> priorPolicies,
			ArrayList<String> moduleParams) {
		if (priorPolicies == null)
			priorPolicies = new HashSet<GoalCondition>();

		// Build the module parameter replacement map
		Map<String, String> paramReplacementMap = new HashMap<String, String>();
		if (moduleParams != null && !moduleParams.isEmpty()) {
			for (int i = 0; i < moduleParams.size(); i++)
				paramReplacementMap.put(RelationalArgument.createGoalTerm(i),
						moduleParams.get(i));
		}
		ModularPolicy modularPolicy = new ModularPolicy(policyGenerator,
				paramReplacementMap);

		RelationalPolicy mainPolicy = policyGenerator
				.generatePolicy(modularPolicy);
		// Dealing with re-tested policies.
		if (mainPolicy instanceof ModularPolicy)
			return (ModularPolicy) mainPolicy;

		// TODO Disallow duplicate rules from policies - they have no use.
		// Add all rules in the basic policy
		for (RelationallyEvaluatableObject reo : mainPolicy.getRules()) {
			RelationalRule rule = (RelationalRule) reo;

			// Add the rule first
			if (moduleParams != null)
				rule.setModularParameters(moduleParams);
			modularPolicy.addRule(rule);

			GoalCondition ruleGCs = rule.getConstantCondition();
			LocalCrossEntropyDistribution moduleDistribution = goalMappedGenerators_
					.get(ruleGCs);
			// Add modular rules as they come up (don't get into a potential
			// circular loop though).
			// TODO Split the GC for non multi-modules
			if (ruleGCs != null && moduleDistribution != null
					&& !priorPolicies.contains(ruleGCs)) {
				Collection<GoalCondition> modulePrior = new HashSet<GoalCondition>(
						priorPolicies);
				modulePrior.add(ruleGCs);

				// Apply the replacement map
				ArrayList<String> newModuleParams = new ArrayList<String>();
				for (String constantArg : ruleGCs.getConstantArgs()) {
					if (paramReplacementMap.isEmpty())
						newModuleParams.add(constantArg);
					else
						newModuleParams.add(paramReplacementMap
								.get(constantArg));
				}

				// Add the policy
				modularPolicy.addPolicy(
						ruleGCs.getFacts(),
						generatePolicy(moduleDistribution, modulePrior,
								newModuleParams));
			}
		}

		return modularPolicy;
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
	 * Checks if a module needs to be learned. If so, it creates a new generator
	 * for the sub-goal which will be used/improved throughout learning.
	 */
	private void checkForModularLearning() {
		// Find the constants present in the rules of all policy generators
		for (LocalCrossEntropyDistribution distribution : goalMappedGenerators_
				.values()) {
			if (distribution.getLocalAgentObservations().isSettled()) {
				for (GoalCondition gc : distribution.getPotentialModuleGoals()) {
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
	}

	@Override
	public RRLActions stepEpisode(RRLObservations observations) {
		// Note the reward for the relevant distributions
		currentPolicy_.noteStepReward(observations.getReward());

		// Evaluate the policy.
		return evaluatePolicy(observations);
	}

}
