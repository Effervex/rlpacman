package relationalFramework;

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

import jess.Rete;
import cerrla.LocalCrossEntropyDistribution;
import cerrla.PolicyGenerator;

import rrlFramework.RRLObservations;
import util.ArgumentComparator;
import util.MultiMap;
import util.Recursive;

public class ModularPolicy extends RelationalPolicy implements
		RelationallyEvaluatableObject {
	private static final long serialVersionUID = 7855536761222318011L;

	/** The distribution that created this modular policy. */
	private LocalCrossEntropyDistribution ceDistribution_;

	/** The goal replacements for this episode. */
	private transient Map<String, String> episodeGoalReplacements_;

	/** A map for transforming goal replacements into the appropriate args. */
	private Map<String, String> moduleParamReplacements_;

	/** A collection of the distributions (modules) used in this policy. */
	private Collection<LocalCrossEntropyDistribution> relevantCEDistributions_;

	/** The rules that have fired. */
	private Set<RelationalRule> triggeredRules_;

	public ModularPolicy(LocalCrossEntropyDistribution policyGenerator,
			Map<String, String> paramReplacementMap) {
		super();
		policySize_ = 0;
		ceDistribution_ = policyGenerator;
		relevantCEDistributions_ = new HashSet<LocalCrossEntropyDistribution>();
		relevantCEDistributions_.add(ceDistribution_);
		moduleParamReplacements_ = paramReplacementMap;
		triggeredRules_ = new HashSet<RelationalRule>();
	}

	/**
	 * A constructor for a new policy using the same rules from an old policy.
	 * 
	 * @param policy
	 *            The old policy.
	 */
	public ModularPolicy(ModularPolicy policy) {
		this(policy.ceDistribution_, policy.moduleParamReplacements_);
		for (RelationallyEvaluatableObject reo : policy.policyRules_) {
			if (reo instanceof ModularPolicy)
				policyRules_.add(new ModularPolicy((ModularPolicy) reo));
			else
				policyRules_.add(reo);
		}
		policySize_ = policy.policySize_;
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
		// Next trigger covering, storing the rules in the policy if
		// required
		List<RelationalRule> coveredRules = ceDistribution_.coverState(
				observations, activatedActions, episodeGoalReplacements_);
		// If the policy is empty, store the rules in it.
		if (activatedActions.isKeysEmpty() && coveredRules != null) {
			Collections.shuffle(coveredRules, PolicyGenerator.random_);
			// Add any new rules to the policy
			for (RelationalRule gr : coveredRules) {
				if (!policyRules_.contains(gr))
					policyRules_.add(gr);
			}
			// TODO I don't think this will quite work for modules.
			parameterArgs(observations.getGoalReplacements());
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
				policyActions.addFiredRule(firedActions, polRule);
				actionsFound += firedActions.size();
			} else {
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
						buffer.append(" ");
					buffer.append(reo.toNiceString());
					buffer.append("\n");
				}
			} else if (reo instanceof ModularPolicy)
				recursePolicyToString(buffer, depth + 1, onlyTriggered);
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
		return true;
	}

	/**
	 * Adds a policy to this modular policy.
	 * 
	 * @param modularPolicy
	 *            The policy to internally add to this policy.
	 */
	public void addPolicy(ModularPolicy modularPolicy) {
		policyRules_.add(modularPolicy);
		policySize_ += modularPolicy.policySize_;
		relevantCEDistributions_.addAll(modularPolicy
				.getRelevantCEDistributions());
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

	/**
	 * Gets all relevant distributions (the modules used by this policy).
	 * 
	 * @return A collection of all relevant distributions.
	 */
	public Collection<LocalCrossEntropyDistribution> getRelevantCEDistributions() {
		return relevantCEDistributions_;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setParameters(BidiMap goalArgs) {
		BidiMap transformedArgs = transformGoalReplacements(goalArgs);
		episodeGoalReplacements_ = transformedArgs;
		for (RelationallyEvaluatableObject obj : policyRules_) {
			obj.setParameters(transformedArgs);
		}
	}

	/**
	 * If the learning process should restart because rules have been removed.
	 * 
	 * @return True if any of the relevant distributions have recently removed
	 *         rules via covering.
	 */
	public boolean shouldRestart() {
		for (LocalCrossEntropyDistribution distribution : relevantCEDistributions_)
			if (distribution.getPolicyGenerator().shouldRestart())
				return true;
		return false;
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
}
