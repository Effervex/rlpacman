package relationalFramework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mandarax.kernel.Fact;
import org.mandarax.kernel.InferenceEngine;
import org.mandarax.kernel.KnowledgeBase;
import org.mandarax.kernel.Prerequisite;
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
	public static final char DELIMITER = ',';
	/** The rules of this policy, under their respective priorities. */
	private GuidedRule[] priorityRules_;
	private boolean[] triggered_;

	/**
	 * A constructor for creating a new policy.
	 * 
	 * @param policySize
	 *            The maximum size of the policy.
	 */
	public Policy(int policySize) {
		priorityRules_ = new GuidedRule[policySize];
		triggered_ = new boolean[policySize];
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
		priorityRules_[index] = rule;
	}

	/**
	 * Gets the rules of the policy.
	 * 
	 * @return The rules of the policy.
	 */
	public GuidedRule[] getRules() {
		return priorityRules_;
	}

	/**
	 * Gets the rules that fired from this policy.
	 * 
	 * @return The rules that fired in this policy
	 */
	public GuidedRule[] getFiringRules() {
		GuidedRule[] firedRules = new GuidedRule[priorityRules_.length];
		for (int i = 0; i < firedRules.length; i++) {
			if (triggered_[i])
				firedRules[i] = priorityRules_[i];
		}
		return firedRules;
	}

	/**
	 * Returns a string version of the policy, parseable by an agent.
	 */
	public String toParseableString() {
		StringBuffer buffer = new StringBuffer(PREFIX);
		for (int i = 0; i < priorityRules_.length; i++) {
			buffer.append(DELIMITER);
			if (priorityRules_[i] != null)
				buffer.append(RuleBase.getInstance().indexOf(priorityRules_[i],
						i));
		}
		buffer.append(DELIMITER + "END");
		return buffer.toString();
	}

	/**
	 * Converts the triggered array into a string.
	 * 
	 * @return A string equal to the state of the triggered list.
	 */
	public String getFiredString() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < triggered_.length; i++) {
			buffer.append(triggered_[i] + "" + DELIMITER);
		}
		return buffer.toString();
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < priorityRules_.length; i++) {
			if (priorityRules_[i] != null) {
				buffer.append("[" + (getPriority(i, priorityRules_.length) + 1)
						+ "]: ");
				buffer.append(StateSpec.encodeRule(priorityRules_[i].getRule()) + "\n");
			}
		}
		return buffer.toString();
	}

	/**
	 * Gets the priority of a slot within the policy.
	 * 
	 * @param slot
	 *            The slot within the policy.
	 * @param policySize
	 *            The size of the policy.
	 * @return A value between 1-NUM_PRIORITIES.
	 */
	public static int getPriority(int slot, int policySize) {
		int priorityNumber = policySize / ActionSwitch.NUM_PRIORITIES + 1;
		return slot / priorityNumber;
	}

	/**
	 * Gets the first element of a priority level.
	 * 
	 * @param priorityLevel
	 *            The priority level to go to.
	 * @param policySize
	 *            The size of the policy.
	 * @return The first element of a priority level.
	 */
	public static int priorityZero(int priorityLevel, int policySize) {
		int priorityNumber = policySize / ActionSwitch.NUM_PRIORITIES + 1;
		return priorityLevel * priorityNumber;
	}

	/**
	 * Parses a policy from a string representation of the policy.
	 * 
	 * @param strPolicy
	 *            The string version of the policy.
	 * @return The parsed policy or null if invalid.
	 */
	public static Policy parsePolicy(String strPolicy) {
		String[] split = strPolicy.split("" + DELIMITER);
		if (!split[0].equals(PREFIX))
			return null;

		// Parse the rules
		Policy policy = new Policy(split.length - 2);
		for (int i = 1; i < split.length - 1; i++) {
			// If there is a rule
			if (split[i].length() != 0) {
				policy.addRule(i - 1, RuleBase.getInstance().getRule(
						Integer.parseInt(split[i]), i - 1));
			}
		}
		return policy;
	}

	/**
	 * Evaluates the policy for applicable rules within each priority. If
	 * multiple rules are applicable, only apply the highest priority ones.
	 * 
	 * @param state
	 *            The current state in predicates.
	 * @param actionSwitch
	 *            The current actions.
	 */
	public void evaluatePolicy(KnowledgeBase state, ActionSwitch actionSwitch) {
		// Get the applicable actions from the priority levels.
		List<Fact>[] results = evaluateRules(state);

		// Apply the actions in the action switch.
		for (int i = 0; i < results.length; i++) {
			if (results[i] != null) {
				actionSwitch.switchOn(results[i], i);
			}
		}
	}

	/**
	 * Evaluates the rules within the policy, returning the sets of rules
	 * applicable at each policy level. The policy is ordered as a decision
	 * list, so the first rule to activate is the rule to use at that priority
	 * level and further evaluation ceases at the activated level.
	 * 
	 * @param state
	 *            The state of the system, in predicate form.
	 * @return A set of grounded facts for each priority level.
	 */
	private List<Fact>[] evaluateRules(KnowledgeBase state) {
		List<Fact>[] activatedActions = new List[ActionSwitch.NUM_PRIORITIES];

		// Logic constructs
		LogicFactorySupport factorySupport = new LogicFactorySupport(RuleBase
				.getInstance().getLogicFactory());
		InferenceEngine ie = RuleBase.getInstance().getInferenceEngine();

		Map<RuleCondition, ResultSet> resultTable = new HashMap<RuleCondition, ResultSet>();
		// Check every slot, from top-to-bottom until one activates
		for (int i = 0; i < priorityRules_.length; i++) {
			int priorityLevel = getPriority(i, priorityRules_.length);
			List<Fact> priorityActions = activatedActions[priorityLevel];
			// Check if the rule exists and the current priority level hasn't
			// fired.
			if (priorityRules_[i] != null) {
				Rule rule = priorityRules_[i].getRule();

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
						priorityActions = new ArrayList<Fact>();
						// For each possible replacement
						do {
							Map<Term, Term> replacementMap = results
									.getResults();
							Collection<Replacement> replacements = new ArrayList<Replacement>();
							// Find the replacements for the variable terms in
							// the action
							for (Term var : rule.getHead().getTerms()) {
								if (var instanceof VariableTerm) {
									replacements.add(new Replacement(var,
											replacementMap.get(var)));
								} else {
									replacements.add(new Replacement(var, var));
								}
							}

							// Apply the replacements and add the fact to the
							// set
							Fact groundAction = rule.getHead().applyToFact(
									replacements);
							// If the action is ground
							if (!priorityActions.contains(groundAction))
								priorityActions.add(groundAction);
						} while (results.next());
					}
					results.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if ((priorityActions != null) && (!priorityActions.isEmpty())) {
					i = priorityZero(priorityLevel + 1, priorityRules_.length) - 1;
					activatedActions[priorityLevel] = priorityActions;
				}
			}
		}

		return activatedActions;
	}

	/**
	 * Sets the fired rules of this policy to that given in string form.
	 * 
	 * @param firedString
	 */
	public void setFired(String firedString) {
		String[] split = firedString.split(DELIMITER + "");
		for (int i = 0; i < split.length; i++) {
			triggered_[i] = Boolean.parseBoolean(split[i]);
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