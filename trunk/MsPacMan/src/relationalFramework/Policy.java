package relationalFramework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jess.QueryResult;
import jess.Rete;
import jess.ValueVector;

/**
 * This class represents a policy that the agent can use.
 * 
 * @author Samuel J. Sarjant
 */
public class Policy {
	public static final String PREFIX = "Policy";
	public static final char DELIMITER = '#';
	public static final String POLICY_RULE = "polRule";
	public static final String OPTIMAL_RULE = "optimal";
	/** The rules of this policy, organised in a deterministic list format. */
	private List<GuidedRule> policyRules_;
	/** The triggered rules in the policy */
	private Set<GuidedRule> triggeredRules_;

	/**
	 * A constructor for creating a new policy.
	 * 
	 * @param policySize
	 *            The maximum size of the policy.
	 */
	public Policy() {
		policyRules_ = new ArrayList<GuidedRule>();
		triggeredRules_ = new HashSet<GuidedRule>();
	}

	/**
	 * Creates the queries the policy uses to check if it's rules are firing.
	 * 
	 * @param rete
	 *            The rete object to add the queries to.
	 */
	public void createQueries(Rete rete, boolean optimal) {
		try {
			for (int i = 0; i < policyRules_.size(); i++) {
				String prefix = (optimal) ? OPTIMAL_RULE : POLICY_RULE;
				rete.eval("(defquery " + prefix + i + " ("
						+ policyRules_.get(i).getConditions() + "))");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Adds a rule to the policy, with it's placement affected
	 * 
	 * @param rule
	 *            The rule to be added.
	 */
	public void addRule(GuidedRule rule) {
		if (!policyRules_.contains(rule))
			policyRules_.add(rule);
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
		if (policyRules_.isEmpty())
			return "<EMPTY POLICY>";

		StringBuffer buffer = new StringBuffer("Policy:\n");
		for (GuidedRule rule : policyRules_) {
			buffer.append(StateSpec.getInstance().encodeRule(rule.toString())
					+ "\n");
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
	 * @param optimal
	 *            If the policy is an optimal test one.
	 */
	public void evaluatePolicy(Rete state, ActionSwitch actionSwitch,
			int actionsReturned, boolean optimal) {
		// A table for already completed requests
		Map<String, QueryResult> resultTable = new HashMap<String, QueryResult>();

		// Check every slot, from top-to-bottom until one activates
		int actionsFound = 0;
		Iterator<GuidedRule> iter = policyRules_.iterator();
		int ruleNumber = 0;
		while ((actionsFound < actionsReturned) && (iter.hasNext())) {
			GuidedRule gr = iter.next();
			String conditions = gr.getConditions();

			// Setting up the necessary variables
			QueryResult results = resultTable.get(conditions);

			// Find the result set
			// TODO Evaluate rules in a step-wise fashion, to infer how much of
			// a rule fires
			try {
				if (results == null) {
					// Forming the query
					String prefix = (optimal) ? OPTIMAL_RULE : POLICY_RULE;
					results = state.runQueryStar(prefix + ruleNumber,
							new ValueVector());
					resultTable.put(conditions, results);
				}

				// If there is at least one result
				if (results.next()) {
					triggeredRules_.add(gr);
					// For each possible replacement
					do {
						// Get the rule action, without brackets
						String[] split = StateSpec.splitFact(gr.getAction());
						StringBuffer actBuffer = new StringBuffer("("
								+ split[0]);

						// TODO Check that the action is within the valid action
						// set

						for (int i = 1; i < split.length; i++) {
							actBuffer.append(" "
									+ results.getSymbol(split[i].substring(1)));
						}
						actBuffer.append(")");
						
						// Use the found action set as a result.
						actionSwitch.switchOn(actBuffer.toString(), actionsFound);
						actionsFound++;
					} while (results.next());
				}
				results.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// If the policy didn't generate enough rules, cover a set of new rules
		// for each action.
		if (actionsFound < actionsReturned) {
			List<GuidedRule> coveredRules = PolicyGenerator.getInstance()
					.triggerCovering(state);
			policyRules_.addAll(coveredRules);
			createQueries(state, false);
			evaluatePolicy(state, actionSwitch, actionsReturned, optimal);
		}
	}
}