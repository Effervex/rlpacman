package relationalFramework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
	 * @param alreadyCovered
	 *            If the policy has already covered this iteration (due to
	 *            recursive calls).
	 */
	public void evaluatePolicy(Rete state, ActionSwitch actionSwitch,
			int actionsReturned, boolean optimal, boolean alreadyCovered) {

		// Check every slot, from top-to-bottom until one activates
		int actionsFound = 0;
		Iterator<GuidedRule> iter = policyRules_.iterator();
		int ruleNumber = 0;
		while ((actionsFound < actionsReturned) && (iter.hasNext())) {
			GuidedRule gr = iter.next();

			// Find the result set
			// TODO Evaluate rules in a step-wise fashion, to infer how much of
			// a rule fires
			try {
				// Forming the query
				String query = StateSpec.getInstance().getRuleQuery(gr);
				QueryResult results = state.runQueryStar(query,
						new ValueVector());

				// If there is at least one result
				if (results.next()) {
					triggeredRules_.add(gr);
					List<String> actionsList = new ArrayList<String>();

					// For each possible replacement
					do {
						// Get the rule action, without brackets
						String[] split = StateSpec.splitFact(gr.getAction());
						StringBuffer actBuffer = new StringBuffer("("
								+ split[0]);

						// TODO Check that the action is within the valid action
						// set

						for (int i = 1; i < split.length; i++) {
							String value = split[i];
							if (value.charAt(0) == '?')
								value = results
										.getSymbol(split[i].substring(1));
							actBuffer.append(" " + value);
						}
						actBuffer.append(")");

						// Use the found action set as a result.
						actionsList.add(actBuffer.toString());
					} while (results.next());

					// Switch on as many random actions as required.
					while (!actionsList.isEmpty()
							&& (actionsFound < actionsReturned)) {
						actionSwitch.switchOn(actionsList
								.remove(PolicyGenerator.random_
										.nextInt(actionsList.size())),
								actionsFound);
						actionsFound++;
					}
				}
				results.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			ruleNumber++;
		}

		// If the policy didn't generate enough rules, cover a set of new rules
		// for each action.
		if (actionsFound < actionsReturned) {
			List<GuidedRule> coveredRules = PolicyGenerator.getInstance()
					.triggerCovering(state, true);
			// Add any new rules to the policy
			for (GuidedRule gr : coveredRules) {
				if (!policyRules_.contains(gr))
					policyRules_.add(gr);
			}
			evaluatePolicy(state, actionSwitch, actionsReturned, optimal, true);
		} else if (!alreadyCovered && !optimal) {
			PolicyGenerator.getInstance().triggerCovering(state, false);
		}
	}
}