package relationalFramework;

import relationalFramework.FiredAction;
import relationalFramework.GoalCondition;
import relationalFramework.PolicyActions;
import relationalFramework.CoveringRelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import cerrla.Module;
import cerrla.PolicyGenerator;
import cerrla.ProgramArgument;

import util.ArgumentComparator;
import util.MultiMap;

import jess.Rete;

/**
 * This class represents a policy that the agent can use.
 * 
 * @author Samuel J. Sarjant
 */
public class CoveringRelationalPolicy extends RelationalPolicy {
	private static final long serialVersionUID = 1936194991813447116L;
	/** The modular rules in the policy. */
	private Collection<RelationalRule> modularRules_;
	/** The triggered rules in the policy */
	private Set<RelationalRule> triggeredRules_;

	/**
	 * A constructor for creating a new policy.
	 * 
	 * @param policySize
	 *            The maximum size of the policy.
	 */
	public CoveringRelationalPolicy() {
		super();
		triggeredRules_ = new HashSet<RelationalRule>();
		modularRules_ = new HashSet<RelationalRule>();
	}

	/**
	 * A constructor for a new policy using the same rules from an old policy.
	 * 
	 * @param policy
	 *            The old policy.
	 */
	public CoveringRelationalPolicy(RelationalPolicy policy) {
		this();
		policyRules_.addAll(policy.policyRules_);
		policySize_ = policy.policySize_;
	}

	/**
	 * Checks if a rule contains constant facts which can be achieved using
	 * modules. If so, the rules are loaded and internally added to the policy.
	 * 
	 * @param rule
	 *            The rule being checked.
	 */
	private void checkModular(RelationalRule rule) {
		Collection<GoalCondition> constantConditions = rule
				.getConstantConditions();
		if (constantConditions == null)
			return;
		// Load every necessary module
		for (GoalCondition constantCondition : constantConditions) {
			Module module = Module.loadModule(StateSpec.getInstance()
					.getEnvironmentName(), constantCondition.toString());
			// If the module exists
			if (module != null) {
				// Put the parameters into an arraylist
				ArrayList<String> parameters = new ArrayList<String>();
				RelationalPredicate cond = constantCondition.getFact();
				// Extract the parameters used in the constant
				// conditions
				for (String arg : cond.getArguments()) {
					// If modules are recursive, set up the parameters
					// accordingly.
					parameters.add(arg);
				}

				// Add the module rules.
				for (RelationalRule gr : module.getModuleRules()) {
					gr.setModularParameters(parameters);
					checkModular(gr);
					// Get/create the corresponding rule in the generator
					gr = PolicyGenerator.getInstance()
							.getCreateCorrespondingRule(gr);
					if (!policyRules_.contains(gr)) {
						gr.setQueryParams(null);
						modularRules_.add(gr);
						policyRules_.add(gr);
						policySize_++;
					}
				}
			}
		}
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
	 * Adds a rule to the policy, adding necessary modular rules if the rule
	 * contains constant facts.
	 * 
	 * @param rule
	 *            The rule to be added.
	 */
	public void addRule(RelationalRule rule) {
		if (!policyRules_.contains(rule)) {
			// Check if the rule contains constant facts that could invoke
			// modular rules.
			if (ProgramArgument.USE_MODULES.booleanValue())
				checkModular(rule);
			policyRules_.add(rule);
			policySize_++;
		}
	}

	/**
	 * If the policy contains a rule.
	 * 
	 * @param rule
	 *            The rule being checked.
	 * @return True if the rule is within the policy, false otherwise.
	 */
	public boolean contains(RelationalRule rule) {
		return policyRules_.contains(rule);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CoveringRelationalPolicy other = (CoveringRelationalPolicy) obj;
		if (policyRules_ == null) {
			if (other.policyRules_ != null)
				return false;
		} else if (!policyRules_.equals(other.policyRules_))
			return false;
		if (policySize_ != other.policySize_)
			return false;
		return true;
	}

	/**
	 * Evaluates a policy for a number of firing rules, and switches on the
	 * necessary rules.
	 * 
	 * @param observation
	 *            The relational observation.
	 * @param actionsReturned
	 *            The number of actions to be returned, or if -1, all actions.
	 */
	public PolicyActions evaluatePolicy(RelationalObservation observation,
			int actionsReturned) {
		PolicyActions actionSwitch = new PolicyActions();
		MultiMap<String, String[]> activatedActions = MultiMap
				.createSortedSetMultiMap(ArgumentComparator.getInstance());
		int actionsFound = 0;
		int actionsReturnedModified = (actionsReturned <= -1) ? Integer.MAX_VALUE
				: actionsReturned;
		Rete state = observation.getStateObservations();
		MultiMap<String, String[]> validActions = observation.getValidActions();

		try {
			// First evaluate the RLGG rules (if any). If the actions there
			// don't match up to the activated actions, covering will be
			// required.
			for (RelationalRule rlgg : PolicyGenerator.getInstance()
					.getRLGGRules().values()) {
				SortedSet<String[]> rlggActions = new TreeSet<String[]>(
						ArgumentComparator.getInstance());
				evaluateRule(rlgg, state,
						validActions.getSortedSet(rlgg.getActionPredicate()),
						rlggActions);
				activatedActions.putCollection(rlgg.getActionPredicate(),
						rlggActions);
			}

			// Next trigger covering, storing the rules in the policy if
			// required
			List<RelationalRule> coveredRules = PolicyGenerator.getInstance()
					.triggerRLGGCovering(observation, activatedActions);
			// If the policy is empty, store the rules in it.
			if (activatedActions.isKeysEmpty() && coveredRules != null) {
				Collections.shuffle(coveredRules, PolicyGenerator.random_);
				// Add any new rules to the policy
				for (RelationalRule gr : coveredRules) {
					if (!policyRules_.contains(gr))
						policyRules_.add(gr);
				}
			}

			// Next, evaluate the rest of the policy until an adequate number of
			// rules are evaluated (usually 1 or all; may be the entire policy).
			Iterator<RelationalRule> iter = policyRules_.iterator();
			while (iter.hasNext() && actionsFound < actionsReturnedModified) {
				RelationalRule polRule = iter.next();
				Collection<FiredAction> firedActions = evaluateRule(
						polRule,
						state,
						validActions.getSortedSet(polRule.getActionPredicate()),
						null);
				actionSwitch.addFiredRule(firedActions);
				actionsFound += firedActions.size();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return actionSwitch;
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
	 * Gets the rules that this policy is made up of.
	 * 
	 * @return The rule for the policy.
	 */
	public List<RelationalRule> getPolicyRules() {
		return policyRules_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((policyRules_ == null) ? 0 : policyRules_.hashCode());
		result = prime * result + policySize_;
		return result;
	}

	/**
	 * Returns the size of the policy (number of non-modular rules in it).
	 * 
	 * @return The size of the policy.
	 */
	public int size() {
		return policySize_;
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
		for (RelationalRule rule : policyRules_) {
			// Don't display module rules or unused rules
			if (triggeredRules_.contains(rule)) {
				buffer.append(rule.toNiceString() + "\n");
			}
		}
		return buffer.toString();
	}

	@Override
	public String toString() {
		if (policyRules_.isEmpty())
			return "<EMPTY POLICY>";

		StringBuffer buffer = new StringBuffer("Policy");
		buffer.append(":\n");

		boolean first = true;
		for (RelationalRule rule : policyRules_) {
			if (!first)
				buffer.append("\n");
			if (!modularRules_.contains(rule)) {
				buffer.append(rule.toNiceString());
			} else {
				buffer.append("MODULAR: " + rule.toNiceString());
			}
			first = false;
		}
		return buffer.toString();
	}
}