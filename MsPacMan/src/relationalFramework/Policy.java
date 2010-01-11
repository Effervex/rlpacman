package relationalFramework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.mandarax.kernel.Fact;
import org.mandarax.kernel.InferenceEngine;
import org.mandarax.kernel.KnowledgeBase;
import org.mandarax.kernel.Query;
import org.mandarax.kernel.Replacement;
import org.mandarax.kernel.ResultSet;
import org.mandarax.kernel.Rule;
import org.mandarax.kernel.Term;
import org.mandarax.kernel.VariableTerm;
import org.mandarax.util.LogicFactorySupport;

/**
 * This class represents a policy that the agent can use.
 * 
 * @author Samuel J. Sarjant
 */
public class Policy {
	public static final String PREFIX = "Policy";
	public static final char DELIMITER = '#';
	/** The rules of this policy, organised in a hierarchy */
	private SortedMap<Integer, Collection<GuidedRule>> ruleHierarchy_;
	/** The triggered rules in the policy */
	private Set<GuidedRule> triggeredRules_;

	/**
	 * A constructor for creating a new policy.
	 * 
	 * @param policySize
	 *            The maximum size of the policy.
	 */
	public Policy() {
		ruleHierarchy_ = new TreeMap<Integer, Collection<GuidedRule>>(
				Collections.reverseOrder());
		triggeredRules_ = new HashSet<GuidedRule>();
	}

	/**
	 * Adds a rule to the policy at a given priority level.
	 * 
	 * @param index
	 *            The index (and priority level) the rule is added at.
	 * @param rule
	 *            The rule to be added.
	 */
	public void addRule(int index, GuidedRule rule) {
		Collection<GuidedRule> rules = ruleHierarchy_.get(index);
		if (rules == null) {
			rules = new ArrayList<GuidedRule>();
			ruleHierarchy_.put(index, rules);
		}

		rules.add(rule);
	}

	/**
	 * Gets the rules that fired from this policy.
	 * 
	 * @return The rules that fired in this policy
	 */
	public Collection<GuidedRule> getFiringRules() {
		return triggeredRules_;
	}

	@Override
	public String toString() {
		if (ruleHierarchy_.isEmpty())
			return "<EMPTY POLICY>";
		
		StringBuffer buffer = new StringBuffer();
		for (Integer key : ruleHierarchy_.keySet()) {
			Collection<GuidedRule> rules = ruleHierarchy_.get(key);
			for (GuidedRule rule : rules) {
				buffer.append("[" + key + "]: ");
				buffer.append(StateSpec.encodeRule(rule.getRule()) + "\n");
			}
		}
		return buffer.toString();
	}

	/**
	 * Evaluates a policy for a number of firing rules, and switches on the
	 * necessary rules.
	 * 
	 * @param state
	 *            The current state in predicates.
	 * @param actionSwitch
	 *            The current actions.
	 * @param actionsReturned
	 *            The number of actions to be returned.
	 */
	public void evaluatePolicy(KnowledgeBase state, ActionSwitch actionSwitch,
			int actionsReturned) {
		// Logic constructs
		LogicFactorySupport factorySupport = new LogicFactorySupport(PolicyGenerator
				.getInstance().getLogicFactory());
		InferenceEngine ie = PolicyGenerator.getInstance().getInferenceEngine();

		// A table for already completed requests
		Map<RuleCondition, ResultSet> resultTable = new HashMap<RuleCondition, ResultSet>();

		// Check every slot, from top-to-bottom until one activates
		int actionsFound = 0;
		Iterator<Integer> iter = ruleHierarchy_.keySet().iterator();
		while ((actionsFound < actionsReturned) && (iter.hasNext())) {
			Integer priority = iter.next();
			Collection<GuidedRule> rules = ruleHierarchy_.get(priority);

			ArrayList<List<Fact>> ruleResults = new ArrayList<List<Fact>>();
			for (GuidedRule gr : rules) {
				Rule rule = gr.getRule();

				// Setting up the necessary variables
				RuleCondition ruleConds = new RuleCondition(rule.getBody());
				ResultSet results = resultTable.get(ruleConds);

				// Find the result set
				try {
					if (results == null) {
						Fact[] ruleConditions = ruleConds.getFactArray();

						// Forming the query
						Query query = factorySupport.query(ruleConditions, rule
								.toString());

						results = ie.query(query, state, InferenceEngine.ALL,
								InferenceEngine.BUBBLE_EXCEPTIONS);
						resultTable.put(ruleConds, results);
					}
					// If there is at least one result
					if (results.next()) {
						triggeredRules_.add(gr);
						List<Fact> actionResults = new ArrayList<Fact>();
						ruleResults.add(actionResults);
						// For each possible replacement
						do {
							Map<Term, Term> replacementMap = results
									.getResults();
							Collection<Replacement> replacements = new ArrayList<Replacement>();
							// Find the replacements for the variable terms
							// in the action
							for (Term var : rule.getHead().getTerms()) {
								if (var instanceof VariableTerm) {
									replacements.add(new Replacement(var,
											replacementMap.get(var)));
								} else {
									replacements.add(new Replacement(var, var));
								}
							}

							// Apply the replacements and add the fact to
							// the set
							Fact groundAction = rule.getHead().applyToFact(
									replacements);
							// If the action is ground
							if (!actionResults.contains(groundAction))
								actionResults.add(groundAction);
						} while (results.next());
					}
					results.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// Choose a random action set from the rules at this level
			if (!ruleResults.isEmpty()) {
				// Add action sets until actions found equals the amount
				// required
				Random random = new Random();
				while ((!ruleResults.isEmpty()) && (actionsFound < actionsReturned)) {
					List<Fact> actionResults = ruleResults.remove(random
							.nextInt(ruleResults.size()));
					actionSwitch.switchOn(actionResults, actionsFound);
					actionsFound++;
				}
			}
		}
	}

	private class RuleCondition {
		private List conditions_;

		public RuleCondition(List prereqs) {
			conditions_ = prereqs;
		}

		public boolean equals(Object obj) {
			if ((obj != null) && (obj instanceof RuleCondition)) {
				RuleCondition other = (RuleCondition) obj;
				// If the lists contain the same elements
				if ((conditions_.containsAll(other.conditions_))
						&& (other.conditions_.containsAll(conditions_)))
					return true;
			}
			return false;
		}

		public int hashCode() {
			return conditions_.hashCode();
		}

		public Fact[] getFactArray() {
			return (Fact[]) conditions_.toArray(new Fact[conditions_.size()]);
		}

		public String toString() {
			return conditions_.toString();
		}
	}
}