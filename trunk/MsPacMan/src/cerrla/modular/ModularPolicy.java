package cerrla.modular;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import cerrla.LocalCrossEntropyDistribution;
import cerrla.ProgramArgument;

import jess.Rete;

import relationalFramework.FiredAction;
import relationalFramework.PolicyActions;
import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalRule;
import rrlFramework.RRLExperiment;
import rrlFramework.RRLObservations;
import util.ArgumentComparator;
import util.MultiMap;
import util.Recursive;

public class ModularPolicy extends RelationalPolicy {
	/** The minimum 'goal-not-achieved' value. */
	private static final double MINIMUM_REWARD = -Integer.MIN_VALUE;

	private static final long serialVersionUID = 7855536761222318011L;

	/** The reward received at every step by the sub-goal. */
	private static final double SUB_GOAL_REWARD = -1;

	/** The distribution that created this modular policy. */
	private LocalCrossEntropyDistribution ceDistribution_;

	/** The collection of policies this policy directly contains. */
	private MultiMap<RelationalRule, ModularSubGoal> childrenPolicies_;

	/** The goal replacements for this episode ('?G_0 -> a' format). */
	private transient BidiMap episodeGoalReplacements_;

	/** The reward received this episode. */
	private transient double[] episodeReward_;

	/** If this learning episode has started where the goal is unachieved. */
	private transient boolean episodeStarted_;

	/** Gets the rules that fired last step. */
	private transient Set<RelationalRule> firedLastStep_;

	/** If the internal goal of this policy is currently achieved. */
	private transient boolean goalAchievedCurrently_;

	/** If the internal goal of this policy was achieved this episode. */
	private transient boolean goalAchievedEpisode_;

	/** A map for transforming goal replacements into the appropriate args. */
	private Map<RelationalArgument, RelationalArgument> moduleParamReplacements_;

	/** The rewards this policy achieved for each episode. */
	private ArrayList<double[]> policyRewards_;

	/** The rules that have fired. */
	private Set<RelationalRule> triggeredRules_;

	/** A unique ID for this modular policy. */
	private String uniqueID_;

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
		childrenPolicies_ = MultiMap.createListMultiMap();
		policyRewards_ = new ArrayList<double[]>();

		uniqueID_ = ceDistribution_.generateUniquePolicyID();
	}

	/**
	 * A constructor for a new policy using the same rules from an old policy.
	 * 
	 * @param policy
	 *            The old policy.
	 */
	public ModularPolicy(ModularPolicy policy) {
		this(policy.ceDistribution_);
		for (PolicyItem reo : policy.policyRules_)
			policyRules_.add(reo);

		policySize_ = policy.policySize_;
		childrenPolicies_ = new MultiMap<RelationalRule, ModularSubGoal>(
				policy.childrenPolicies_);
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
		policySize_ = newPol.size();

		// Add the rules, creating ModularHoles where appropriate.
		for (PolicyItem reo : newPol.getRules()) {
			if (reo instanceof RelationalRule) {
				RelationalRule rule = (RelationalRule) reo;
				policyRules_.add(reo);

				// Checking for sub-goals
				if (ProgramArgument.USE_MODULES.booleanValue()) {
					Collection<SpecificGoalCondition> goalConds = rule
							.getSpecificSubGoals();
					for (GoalCondition gc : goalConds) {
						ModularSubGoal subGoal = new ModularSubGoal(gc, rule);
						policyRules_.add(subGoal);
						childrenPolicies_.put(rule, subGoal);
					}

					// General sub-goals
					if (ProgramArgument.USE_GENERAL_MODULES.booleanValue()) {
						Collection<GeneralGoalCondition>[] generalisedConds = rule
								.getGeneralisedConditions();
						// Add all general conditions, and fill in the blanks
						// when necessary.
						for (GoalCondition gc : generalisedConds[0]) {
							ModularSubGoal subGoal = new ModularSubGoal(gc,
									rule);
							policyRules_.add(subGoal);
							childrenPolicies_.put(rule, subGoal);
						}
						for (GoalCondition gc : generalisedConds[1]) {
							ModularSubGoal subGoal = new ModularSubGoal(gc,
									rule);
							policyRules_.add(subGoal);
							childrenPolicies_.put(rule, subGoal);
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
				policySize_++;
			}
			// Add parameters to the rule.
			parameterArgs(transformGoalReplacements(observations
					.getGoalReplacements()));
		}

		// If the goal has been achieved, don't evaluate this policy
		if (goalAchievedCurrently_)
			return;

		// Evaluate the rules/policies recursively.
		Rete state = observations.getState();

		Iterator<PolicyItem> iter = policyRules_.iterator();
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
				if (childrenPolicies_.containsKey(polRule)) {
					for (ModularSubGoal modSubGoal : childrenPolicies_
							.get(polRule)) {
						if (!firedActions.isEmpty())
							modSubGoal.setGoalAchieved(true);
						else
							modSubGoal.setGoalAchieved(false);
					}
				}
			} else if (polObject instanceof ModularSubGoal) {
				// Evaluate the internal policy.
				ModularPolicy internalPolicy = ((ModularSubGoal) polObject)
						.getModularPolicy();
				if (internalPolicy != null)
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
		for (PolicyItem reo : policyRules_) {
			if (reo instanceof RelationalRule) {
				// If only triggered rules, just print rules that were
				// triggered.
				if (!onlyTriggered || triggeredRules_.contains(reo)) {
					for (int i = 0; i < depth; i++) {
						if (i < depth - 1)
							buffer.append(" ");
						else
							buffer.append(" |"
									+ ceDistribution_.getGoalCondition() + "|");
					}
					buffer.append(((RelationalRule) reo)
							.toNiceString(moduleParamReplacements_));
					buffer.append("\n");
				}
			} else if (reo instanceof ModularSubGoal) {
				ModularPolicy internalPolicy = ((ModularSubGoal) reo)
						.getModularPolicy();
				if (internalPolicy != null)
					internalPolicy.recursePolicyToString(buffer, depth + 1,
							onlyTriggered);
			}
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
			for (RelationalArgument ruleParam : moduleParamReplacements_
					.keySet()) {
				RelationalArgument goalParam = moduleParamReplacements_
						.get(ruleParam);
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
	public boolean addTriggeredRule(RelationalRule rule) {
		triggeredRules_.add(rule);
		firedLastStep_.add(rule);
		return true;
	}

	/**
	 * Removes all child policies (which can be put back in later).
	 */
	public void clearChildren() {
		for (ModularSubGoal subGoal : childrenPolicies_.values())
			subGoal.setModularPolicy(null);
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
		// If the episode never started, record nothing
		if (!episodeStarted_)
			return false;

		// Modify the reward if the goal hasn't been achieved if a sub-goal
		// generator
		if (!ceDistribution_.getGoalCondition().isMainGoal()
				&& !goalAchievedEpisode_) {
			episodeReward_[0] = MINIMUM_REWARD;
			episodeReward_[1] = MINIMUM_REWARD;
		}

		// Note the episode reward in the generator.
		if (ceDistribution_.getGoalCondition().isMainGoal()
				|| episodeReward_[0] != 0) {
			policyRewards_.add(episodeReward_);
			episodeReward_ = null;
		}

		// End episode for all children.
		boolean regeneratePolicy = false;
		for (ModularSubGoal child : childrenPolicies_.values()) {
			ModularPolicy childPol = child.getModularPolicy();
			if (childPol != null)
				regeneratePolicy |= childPol.endEpisode();
		}

		// Check if sample needs to be recorded
		if (policyRewards_.size() >= ProgramArgument.POLICY_REPEATS.intValue()) {
			// Record the sample.
			ceDistribution_.recordSample(this, policyRewards_);
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
		if (uniqueID_ == null) {
			if (other.uniqueID_ != null)
				return false;
		} else if (!uniqueID_.equals(other.uniqueID_))
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
		for (ModularSubGoal child : childrenPolicies_.values()) {
			ModularPolicy childPol = child.getModularPolicy();
			if (childPol != null)
				childPol.getAllPolicies(undertestedOnly, recursiveCollection);
		}

		return recursiveCollection;
	}

	/**
	 * Gets the rules that fired from this policy.
	 * 
	 * @return The rules that fired in this policy
	 */
	public Set<RelationalRule> getFiringRules() {
		return triggeredRules_;
	}

	public LocalCrossEntropyDistribution getLocalCEDistribution() {
		return ceDistribution_;
	}

	/**
	 * Gets the modular replacement map.
	 * 
	 * @return The replacement map for this policy.
	 */
	public Map<RelationalArgument, RelationalArgument> getModularReplacementMap() {
		return moduleParamReplacements_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime
				* result
				+ ((moduleParamReplacements_ == null) ? 0
						: moduleParamReplacements_.hashCode());
		result = prime * result
				+ ((uniqueID_ == null) ? 0 : uniqueID_.hashCode());
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
	public boolean isGoalCurrentlyAchieved() {
		return goalAchievedCurrently_;
	}

	/**
	 * Notes the environment reward if main modular policy, otherwise it uses an
	 * internal reward.
	 */
	@Recursive
	public boolean noteStepReward(double[] reward) {
		if (episodeReward_ == null)
			episodeReward_ = new double[2];
		
		// Note reward if a rule in this policy fired (or it's the main policy).
		boolean noteReward = ceDistribution_.getGoalCondition().isMainGoal()
				|| !firedLastStep_.isEmpty();

		// Drop down and reward from the bottom up.
		for (ModularSubGoal child : childrenPolicies_.values()) {
			ModularPolicy childPol = child.getModularPolicy();
			if (childPol != null)
				noteReward |= childPol.noteStepReward(reward);
		}

		// Only note the reward if a rule within this policy fired.
		if (noteReward) {
			// If this is an unachieved sub-goal, note reward.
			if (!ceDistribution_.getGoalCondition().isMainGoal()) {
				// If the episode has started and the goal hasn't been achieved,
				// note reward.
				if (episodeStarted_ && !goalAchievedEpisode_) {
					episodeReward_[0] += SUB_GOAL_REWARD;
					episodeReward_[1] += SUB_GOAL_REWARD;
				}
			} else {
				episodeReward_[0] += reward[0];
				episodeReward_[1] += reward[1];
			}
		}

		return noteReward;
	}

	/**
	 * Sets the goal (episodic and current) achieved.
	 */
	public void setGoalAchieved() {
		goalAchievedCurrently_ = true;
		if (episodeStarted_)
			goalAchievedEpisode_ = true;
	}

	/**
	 * Sets the current goal unachieved.
	 * 
	 * @param startEpisode
	 *            If the modular sub-goal episode should be started (because the
	 *            goal is currently unmet).
	 */
	public void setGoalUnachieved(boolean startEpisode) {
		goalAchievedCurrently_ = false;
		episodeStarted_ |= startEpisode;
	}

	/**
	 * Sets the modular replacements for this policy.
	 * 
	 * @param paramReplacementMap
	 *            The modular replacements to set.
	 */
	public void setModularParameters(
			Map<RelationalArgument, RelationalArgument> paramReplacementMap) {
		moduleParamReplacements_ = paramReplacementMap;
	}

	public void setParameters(BidiMap goalArgs) {
		BidiMap transformedArgs = transformGoalReplacements(goalArgs);
		episodeGoalReplacements_ = transformedArgs;
		for (PolicyItem obj : policyRules_) {
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

	public int size() {
		return policySize_;
	}

	/**
	 * Starts a new episode, so reward observation begins on a new episode.
	 */
	@Recursive
	public void startEpisode() {
		episodeReward_ = null;
		if (ceDistribution_.getGoalCondition().isMainGoal())
			episodeStarted_ = true;
		else
			episodeStarted_ = false;
		goalAchievedEpisode_ = false;
		goalAchievedCurrently_ = false;
		firedLastStep_ = new HashSet<RelationalRule>();

		ceDistribution_.startEpisode();

		// Start episode for all children
		for (ModularSubGoal child : childrenPolicies_.values()) {
			ModularPolicy childPol = child.getModularPolicy();
			if (childPol != null)
				childPol.startEpisode();
		}
	}

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
}
