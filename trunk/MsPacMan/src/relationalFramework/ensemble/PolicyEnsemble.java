package relationalFramework.ensemble;

import relationalFramework.PolicyActions;
import relationalFramework.RelationalPolicy;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jess.Rete;

import org.apache.commons.collections.BidiMap;

import util.MultiMap;
import util.Pair;

/**
 * A class for possibly holding multiple policies.
 * 
 * @author Sam Sarjant
 * 
 */
public class PolicyEnsemble {
	/** The policies. */
	private Set<RelationalPolicy> policies_;

	/** The policy weights (number of policy duplicates) */
	private Map<RelationalPolicy, Integer> duplicateCount_;

	/** The policy consistencies (number of times the policy is correct). */
	private Map<RelationalPolicy, Double> policyConsistency_;

	/** The number of evaluations the ensemble has encountered. */
	private Integer numEvaluations_;

	/**
	 * Initialise the ensemble with just one policy.
	 * 
	 * @param singlePolicy
	 *            A single policy.
	 */
	public PolicyEnsemble(RelationalPolicy singlePolicy) {
		policies_ = new HashSet<RelationalPolicy>(1);
		policies_.add(singlePolicy);
		duplicateCount_ = new HashMap<RelationalPolicy, Integer>();
		duplicateCount_.put(singlePolicy, 1);
		policyConsistency_ = new HashMap<RelationalPolicy, Double>();
		numEvaluations_ = 0;
	}

	/**
	 * Initialise the ensemble with a number of policies.
	 * 
	 * @param policies
	 *            The policies making up the ensemble.
	 */
	public PolicyEnsemble(Collection<RelationalPolicy> policies) {
		policies_ = new HashSet<RelationalPolicy>(policies);
		duplicateCount_ = new HashMap<RelationalPolicy, Integer>();
		for (RelationalPolicy pol : policies)
			add(pol);
		policyConsistency_ = new HashMap<RelationalPolicy, Double>();
		numEvaluations_ = 0;
	}

	/**
	 * Adds a policy to the ensemble.
	 * 
	 * @param pol
	 *            The policy to add.
	 */
	public void add(RelationalPolicy pol) {
		policies_.add(pol);
		if (!duplicateCount_.containsKey(pol))
			duplicateCount_.put(pol, 1);
		else
			duplicateCount_.put(pol, duplicateCount_.get(pol) + 1);
	}

	/**
	 * Evaluates the ensemble by evaluating each policy within the ensemble.
	 * 
	 * @param state
	 *            The state to evaluate on.
	 * @param validActions
	 *            The valid actions for the state.
	 * @param goalArgs
	 *            The goal argument map.
	 * @param actions
	 *            The action choice to return.
	 * @param numReturnedActions
	 *            The minimum number of actions for the policies to gather.
	 * @param handCoded
	 *            If the policy is hand-coded.
	 * @param noteTriggered
	 *            If the policies note rules fired as triggered.
	 * @return The selected action choice (decided by ensemble vote).
	 */
	@SuppressWarnings("unchecked")
	public PolicyActions evaluatePolicy(Rete state,
			MultiMap<String, String[]> validActions, BidiMap goalArgs,
			int numReturnedActions) {
		ActionChoiceEnsemble ace = new ActionChoiceEnsemble();
		for (RelationalPolicy pol : policies_) {
			PolicyActions actions = pol.evaluatePolicy(state, validActions,
					goalArgs, numReturnedActions);
			// If we only have one policy, just use that.
			if (policies_.size() == 1)
				return actions;
			ace.addActionChoice(actions, duplicateCount_.get(pol));
		}

		PolicyActions result = ace.getVotedActionChoice();
		Set<RelationalPolicy> votedPolicies = ace
				.getPolicyConsistency();
		for (RelationalPolicy rp : votedPolicies) {
			Double count = policyConsistency_.get(rp);
			if (count == null)
				count = 0d;
			policyConsistency_.put(rp, count + 1);
		}
		numEvaluations_++;
		return result;
	}

	/**
	 * Apply the goal arguments to each policy.
	 * 
	 * @param goalArgs
	 *            The goal args.
	 */
	@SuppressWarnings("unchecked")
	public void parameterArgs(BidiMap goalArgs) {
		for (RelationalPolicy pol : policies_)
			pol.parameterArgs(goalArgs);
	}

	/**
	 * Gets the best policy from the ensemble (the one which has been included
	 * in the ensemble selected action the most.)
	 * 
	 * @return The policy which is the most significant in the ensemble.
	 */
	public Pair<RelationalPolicy, Double> getMajorPolicy() {
		if (policies_.size() == 1)
			return new Pair<RelationalPolicy, Double>(policies_
					.iterator().next(), 1d);

		// Use the policy with the greatest consistency
		double bestWeight = 0;
		RelationalPolicy bestPolicy = null;
		for (RelationalPolicy rp : policyConsistency_.keySet()) {
			double weight = policyConsistency_.get(rp) / numEvaluations_;
			if (weight > bestWeight) {
				bestWeight = weight;
				bestPolicy = rp;
			}
		}
		return new Pair<RelationalPolicy, Double>(bestPolicy,
				bestWeight);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((duplicateCount_ == null) ? 0 : duplicateCount_.hashCode());
		result = prime * result
				+ ((policies_ == null) ? 0 : policies_.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PolicyEnsemble other = (PolicyEnsemble) obj;
		if (duplicateCount_ == null) {
			if (other.duplicateCount_ != null)
				return false;
		} else if (!duplicateCount_.equals(other.duplicateCount_))
			return false;
		if (policies_ == null) {
			if (other.policies_ != null)
				return false;
		} else if (!policies_.equals(other.policies_))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (RelationalPolicy pol : policies_) {
			if (policies_.size() > 1)
				buffer.append("WEIGHT " + duplicateCount_.get(pol) + "\n");
			buffer.append(pol.toString());
		}

		return buffer.toString();
	}

	public int numPolicies() {
		return policies_.size();
	}
}
