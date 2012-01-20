package relationalFramework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import jess.Rete;
import cerrla.LocalCrossEntropyDistribution;
import cerrla.ModularHole;
import cerrla.ProgramArgument;

import rrlFramework.RRLExperiment;
import rrlFramework.RRLObservations;
import util.ArgumentComparator;
import util.MultiMap;
import util.Recursive;

public class ModularPolicy extends RelationalPolicy implements
		RelationallyEvaluatableObject {
	/** The minimum 'goal-not-achieved' value. */
	private static final double MINIMUM_REWARD = -Integer.MIN_VALUE;

	private static final long serialVersionUID = 7855536761222318011L;

	/** The reward received at every step by the sub-goal. */
	private static final double SUB_GOAL_REWARD = -1;

	/** The distribution that created this modular policy. */
	private LocalCrossEntropyDistribution ceDistribution_;

	/** The collection of policies this policy directly contains. */
	private Map<SortedSet<RelationalPredicate>, ModularPolicy> childrenPolicies_;

	/** The goal replacements for this episode ('?G_0 -> a' format). */
	private transient BidiMap episodeGoalReplacements_;

	/** The reward received this episode. */
	private transient double episodeReward_;

	/** Gets the rules that fired last step. */
	private transient Set<RelationalRule> firedLastStep_ = new HashSet<RelationalRule>();

	/** If the internal goal of this policy was achieved this episode. */
	private transient boolean goalAchieved_;

	/** The goal condition this policy is assigned to complete. */
	private GoalCondition goalCondition_;

	/** A map for transforming goal replacements into the appropriate args. */
	private Map<String, String> moduleParamReplacements_;

	/** The rewards this policy achieved for each episode. */
	private transient ArrayList<Double> policyRewards_ = new ArrayList<Double>();

	/** The rules that have fired. */
	private Set<RelationalRule> triggeredRules_;

	/**
	 * A constructor for a blank modular policy.
	 * 
	 * @param policyGenerator
	 *            The generator that created this policy.
	 */
	public ModularPolicy(LocalCrossEntropyDistribution policyGenerator) {
		super();
		ceDistribution_ = policyGenerator;
		triggeredRules_ = new HashSet<RelationalRule>();
		childrenPolicies_ = new HashMap<SortedSet<RelationalPredicate>, ModularPolicy>();
	}

	/**
	 * A constructor for a new policy using the same rules from an old policy.
	 * 
	 * @param policy
	 *            The old policy.
	 */
	public ModularPolicy(ModularPolicy policy) {
		this(policy.ceDistribution_);
		for (RelationallyEvaluatableObject reo : policy.policyRules_)
			policyRules_.add(reo);

		policySize_ = policy.policySize_;
	}

	/**
	 * Creates a new modular policy from an existing basic relational policy.
	 * 
	 * @param basicPolicy
	 *            The basic policy with rules to transfer to this policy.
	 * @param policyGenerator
	 *            The generator that created this policy.
	 */
	public ModularPolicy(RelationalPolicy newPol,
			LocalCrossEntropyDistribution policyGenerator) {
		this(policyGenerator);
		policySize_ = newPol.policyRules_.size();

		// Add the rules, creating ModularHoles where appropriate.
		for (RelationallyEvaluatableObject reo : newPol.getRules()) {
			if (reo instanceof RelationalRule) {
				policyRules_.add(reo);

				// Checking for sub-goals
				GoalCondition ruleGCs = reo.getGoalCondition();
				if (ruleGCs != null) {
					if (ProgramArgument.MULTI_MODULES.booleanValue()) {
						policyRules_.add(new ModularHole(ruleGCs));
					} else {
						for (GoalCondition splitGC : ruleGCs.splitCondition()) {
							policyRules_.add(new ModularHole(splitGC));
						}
					}
				}
			}
		}
	}

	/**
	 * Evaluates the rules of this policy (with recursion).
	 * 
	 * @param observations
	 *            The relational observations for the state.
	 * @param policyActions
	 *            The collection to add the actions to.
	 * @param activatedActions
	 *            The actions the RLGG rules return.
	 * @param actionsFound
	 *            The number of actions found so far.
	 * @param actionsRequired
	 *            The number of actions required to return.
	 * @return The resultant actions.
	 * @throws Exception
	 *             Should something go awry...
	 */
	private void evaluateInternalPolicy(RRLObservations observations,
			PolicyActions policyActions,
			MultiMap<String, String[]> activatedActions, int actionsFound,
			int actionsRequired) throws Exception {
		firedLastStep_.clear();

		// Run the cover state method to possibly scan this state (depending on
		// current RLGG rules and scan intervals).
		List<RelationalRule> coveredRules = ceDistribution_.coverState(this,
				observations, activatedActions, episodeGoalReplacements_);
		// If the policy is empty, store the rules in it.
		if (coveredRules != null && !coveredRules.isEmpty()) {
			Collections.shuffle(coveredRules, RRLExperiment.random_);
			// Add any new rules to the policy
			for (RelationalRule gr : coveredRules) {
				policyRules_.add(gr);
			}
			// Add parameters to the rule.
			parameterArgs(transformGoalReplacements(observations
					.getGoalReplacements()));
		}



		// Evaluate the rules/policies recursively.
		Rete state = observations.getState();

		Iterator<RelationallyEvaluatableObject> iter = policyRules_.iterator();
		while (iter.hasNext() && actionsFound < actionsRequired) {
			Object polObject = iter.next();
			if (polObject instanceof RelationalRule) {
				// Evaluate the rule
				RelationalRule polRule = (RelationalRule) polObject;
				Collection<FiredAction> firedActions = evaluateRule(polRule,
						state, observations.getValidActions(polRule
								.getActionPredicate()), null);
				policyActions.addFiredRule(firedActions, this);
				actionsFound += firedActions.size();

				// If this rule created a sub-goal, mark the goal achieved.
				GoalCondition gc = polRule.getConstantCondition();
				if (gc != null && !firedActions.isEmpty()) {
					if (ProgramArgument.MULTI_MODULES.booleanValue()) {
						SortedSet<RelationalPredicate> goalFacts = gc
								.getFacts();
						if (childrenPolicies_.containsKey(goalFacts))
							childrenPolicies_.get(goalFacts).goalAchieved_ = true;
					} else {
						for (GoalCondition splitGC : gc.splitCondition()) {
							SortedSet<RelationalPredicate> goalFacts = splitGC
									.getFacts();
							if (childrenPolicies_.containsKey(goalFacts))
								childrenPolicies_.get(goalFacts).goalAchieved_ = true;
						}
					}
				}
			} else if (polObject instanceof ModularPolicy) {
				// Evaluate the internal policy.
				ModularPolicy internalPolicy = (ModularPolicy) polObject;
				internalPolicy.evaluateInternalPolicy(observations,
						policyActions, activatedActions, actionsFound,
						actionsRequired);
			}
		}
	}

	/**
	 * Recursively prints out the policy, incrementing relational policies along
	 * the way.
	 * 
	 * @param buffer
	 *            The buffer to print to.
	 * @param depth
	 *            The amount of incrementing to do.
	 * @return The String version of this policy.
	 */
	@Recursive
	private String recursePolicyToString(StringBuffer buffer, int depth,
			boolean onlyTriggered) {
		for (RelationallyEvaluatableObject reo : policyRules_) {
			if (reo instanceof RelationalRule) {
				// If only triggered rules, just print rules that were
				// triggered.
				if (!onlyTriggered || triggeredRules_.contains(reo)) {
					for (int i = 0; i < depth; i++)
						buffer.append("  ");
					buffer.append(((RelationalRule) reo)
							.toNiceString(moduleParamReplacements_));
					buffer.append("\n");
				}
			} else if (reo instanceof ModularPolicy)
				((ModularPolicy) reo).recursePolicyToString(buffer, depth + 1,
						onlyTriggered);
		}
		return buffer.toString();
	}

	/**
	 * Transforms the given goal replacements into potentially different (but
	 * always smaller/equal size) replacements based on how this policy is
	 * defined.
	 * 
	 * @param originalGoalReplacements
	 *            The original goal replacements to modify.
	 * @return The transformed replacements. Always smaller/equal size and using
	 *         the same args.
	 */
	private BidiMap transformGoalReplacements(BidiMap originalGoalReplacements) {
		// Modify the goal replacements based on the moduleParamReplacements
		BidiMap goalReplacements = originalGoalReplacements;
		if (moduleParamReplacements_ != null
				&& !moduleParamReplacements_.isEmpty()) {
			// Swap any terms shown in the replacements
			BidiMap modGoalReplacements = new DualHashBidiMap();
			for (String ruleParam : moduleParamReplacements_.keySet()) {
				String goalParam = moduleParamReplacements_.get(ruleParam);
				modGoalReplacements.put(goalReplacements.getKey(goalParam),
						ruleParam);
			}

			goalReplacements = modGoalReplacements;
		}
		return goalReplacements;
	}

	/**
	 * Adds a rule to the set of triggered rules. Some circumstances may forbid
	 * the rule being added.
	 * 
	 * @param rule
	 *            The rule to be added.
	 * @return True if the rule was successfully added, or is already present.
	 *         False if the rule was not allowed to be added.
	 */
	protected boolean addTriggeredRule(RelationalRule rule) {
		triggeredRules_.add(rule);
		firedLastStep_.add(rule);
		return true;
	}

	/**
	 * Adds a policy to this modular policy.
	 * 
	 * @param modularPolicy
	 *            The policy to internally add to this policy.
	 */
	public void addPolicy(SortedSet<RelationalPredicate> constantFacts,
			ModularPolicy modularPolicy) {
		policyRules_.add(modularPolicy);
		policySize_ += modularPolicy.policySize_;
		childrenPolicies_.put(constantFacts, modularPolicy);
	}

	/**
	 * Notes the final reward received for this episode.
	 * 
	 * @param reward
	 *            The reward received for the episode.
	 * @return True if the modular policy needs to be regenerated due to a
	 *         part(s) of it being fully tested.
	 */
	@Recursive
	public boolean endEpisode() {
		// Modify the reward if the goal hasn't been achieved if a sub-goal
		// generator
		if (!ceDistribution_.getGoalCondition().isMainGoal() && !goalAchieved_)
			episodeReward_ = MINIMUM_REWARD;

		// Note the episode reward in the generator.
		if (ceDistribution_.getGoalCondition().isMainGoal()
				|| episodeReward_ != 0) {
			policyRewards_.add(episodeReward_);
		}

		// End episode for all children.
		boolean regeneratePolicy = false;
		for (ModularPolicy child : childrenPolicies_.values())
			regeneratePolicy |= child.endEpisode();

		// Check if sample needs to be recorded
		if (policyRewards_.size() >= ProgramArgument.POLICY_REPEATS.intValue()) {
			// Record the sample.
			ceDistribution_.recordSample(this,
					policyRewards_.toArray(new Double[policyRewards_.size()]));
			regeneratePolicy = true;
		}
		return regeneratePolicy;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModularPolicy other = (ModularPolicy) obj;
		if (moduleParamReplacements_ == null) {
			if (other.moduleParamReplacements_ != null)
				return false;
		} else if (!moduleParamReplacements_
				.equals(other.moduleParamReplacements_))
			return false;
		return true;
	}

	@Override
	public PolicyActions evaluatePolicy(RRLObservations observations,
			int actionsReturned) {
		PolicyActions policyActions = new PolicyActions();
		MultiMap<String, String[]> activatedActions = MultiMap
				.createSortedSetMultiMap(ArgumentComparator.getInstance());
		int actionsReturnedModified = (actionsReturned <= -1) ? Integer.MAX_VALUE
				: actionsReturned;
		Rete state = observations.getState();

		try {
			// First evaluate the RLGG rules (if any). If the actions there
			// don't match up to the activated actions, covering will be
			// required.
			for (RelationalRule rlgg : ceDistribution_.getPolicyGenerator()
					.getRLGGRules().values()) {
				SortedSet<String[]> rlggActions = new TreeSet<String[]>(
						ArgumentComparator.getInstance());
				evaluateRule(
						rlgg,
						state,
						observations.getValidActions(rlgg.getActionPredicate()),
						rlggActions);
				activatedActions.putCollection(rlgg.getActionPredicate(),
						rlggActions);
			}

			// Next, evaluate the rest of the policy until an adequate number of
			// rules are evaluated (usually 1 or all; may be the entire policy).
			evaluateInternalPolicy(observations, policyActions,
					activatedActions, 0, actionsReturnedModified);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return policyActions;
	}

	/**
	 * Gets the rules that fired from this policy.
	 * 
	 * @return The rules that fired in this policy
	 */
	public Set<RelationalRule> getFiringRules() {
		return triggeredRules_;
	}

	@Override
	public GoalCondition getGoalCondition() {
		return goalCondition_;
	}

	public LocalCrossEntropyDistribution getLocalCEDistribution() {
		return ceDistribution_;
	}

	/**
	 * Gets all the policies this policy contains (recursively), including
	 * itself.
	 * 
	 * @param undertestedOnly
	 *            If only collecting undertested policies.
	 * @return A collection of policies of size at least 1.
	 */
	public Collection<ModularPolicy> getAllPolicies(boolean undertestedOnly,
			Collection<ModularPolicy> recursiveCollection) {
		// Initialise recursive collection.
		if (recursiveCollection == null)
			recursiveCollection = new HashSet<ModularPolicy>();
		if (undertestedOnly && shouldRegenerate())
			return recursiveCollection;

		recursiveCollection.add(this);

		// Run through all children policies
		for (ModularPolicy child : childrenPolicies_.values())
			child.getAllPolicies(undertestedOnly, recursiveCollection);

		return recursiveCollection;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime
				* result
				+ ((moduleParamReplacements_ == null) ? 0
						: moduleParamReplacements_.hashCode());
		return result;
	}

	public boolean isFresh() {
		return policyRewards_.isEmpty();
	}

	/**
	 * If the goal has been achieved.
	 * 
	 * @return True if the goal has been achieved.
	 */
	public boolean isGoalAchieved() {
		return goalAchieved_;
	}

	/**
	 * Notes the environment reward if main modular policy, otherwise it uses an
	 * internal reward.
	 */
	@Recursive
	public boolean noteStepReward(double reward) {
		// Note reward if a rule in this policy fired (or it's the main policy).
		boolean noteReward = ceDistribution_.getGoalCondition().isMainGoal()
				|| !firedLastStep_.isEmpty();

		// Drop down and reward from the bottom up.
		for (ModularPolicy child : childrenPolicies_.values())
			noteReward |= child.noteStepReward(reward);

		// Only note the reward if a rule within this policy fired.
		if (noteReward) {
			// If this is an unachieved sub-goal, note reward.
			if (!ceDistribution_.getGoalCondition().isMainGoal()) {
				if (!goalAchieved_)
					episodeReward_ += SUB_GOAL_REWARD;
			} else
				episodeReward_ += reward;
		}

		return noteReward;
	}

	/**
	 * Replaces the index of an old {@link RelationallyEvaluatableObject} with a
	 * undertested, potentially fresh, policy.
	 * 
	 * @param i
	 *            The location of the item to replace.
	 * @param replacement
	 *            The replacement policy.
	 */
	public void replaceIndex(int i, RelationallyEvaluatableObject replacement) {
		RelationallyEvaluatableObject oldObject = policyRules_.get(i);
		policyRules_.set(i, replacement);
		policySize_ += replacement.size() - oldObject.size();

		if (replacement instanceof ModularPolicy) {
			ModularPolicy modPol = (ModularPolicy) replacement;
			modPol.goalCondition_ = oldObject.getGoalCondition();
			childrenPolicies_.put(modPol.goalCondition_.getFacts(), modPol);
		}
	}

	/**
	 * Sets if the goal has been achieved.
	 * 
	 * @param b
	 *            The state of goal achievement.
	 */
	public void setGoalAchieved(boolean b) {
		goalAchieved_ = b;
	}

	/**
	 * Sets the modular replacements for this policy.
	 * 
	 * @param paramReplacementMap
	 *            The modular replacements to set.
	 */
	public void setModularParameters(Map<String, String> paramReplacementMap) {
		moduleParamReplacements_ = paramReplacementMap;
		// for (RelationallyEvaluatableObject reo : policyRules_) {
		// if (reo instanceof RelationalRule)
		// ((RelationalRule) reo)
		// .setModularParameters(paramReplacementMap);
		// }
	}

	@Override
	public void setParameters(BidiMap goalArgs) {
		BidiMap transformedArgs = transformGoalReplacements(goalArgs);
		episodeGoalReplacements_ = transformedArgs;
		for (RelationallyEvaluatableObject obj : policyRules_) {
			obj.setParameters(goalArgs);
		}
	}

	/**
	 * If this policy should be replaced with another policy.
	 * 
	 * @return True if this policy has collected enough rewards to be noted.
	 */
	public boolean shouldRegenerate() {
		return policyRewards_.size() >= ProgramArgument.POLICY_REPEATS
				.intValue();
	}

	/**
	 * If the learning process should restart because rules have been removed.
	 * 
	 * @return True if any of the relevant distributions have recently removed
	 *         rules via covering.
	 */
	@Recursive
	public boolean shouldRestart() {
		if (ceDistribution_.getPolicyGenerator().shouldRestart())
			return true;

		for (ModularPolicy children : childrenPolicies_.values()) {
			if (children.shouldRestart())
				return true;
		}
		return false;
	}

	@Override
	public int size() {
		return policySize_;
	}

	/**
	 * Starts a new episode, so reward observation begins on a new episode.
	 */
	@Recursive
	public void startEpisode() {
		episodeReward_ = 0;
		goalAchieved_ = false;

		// Start episode for all children
		for (ModularPolicy child : childrenPolicies_.values())
			child.startEpisode();
	}

	@Override
	public String toNiceString() {
		if (policyRules_.isEmpty())
			return "<EMPTY POLICY>";

		StringBuffer buffer = new StringBuffer("Policy:\n");
		return recursePolicyToString(buffer, 0, false);
	}

	/**
	 * A method for displaying only the rules used within the policy.
	 * 
	 * @return The policy in string format, minus the unused rules.
	 */
	public String toOnlyUsedString() {
		if (policyRules_.isEmpty())
			return "<EMPTY POLICY>";

		StringBuffer buffer = new StringBuffer("Policy:\n");
		return recursePolicyToString(buffer, 0, true);
	}

	@Override
	public String toString() {
		return toNiceString();
	}

	/**
	 * Gets the modular replacement map.
	 * 
	 * @return The replacement map for this policy.
	 */
	public Map<String, String> getModularReplacementMap() {
		return moduleParamReplacements_;
	}


}
