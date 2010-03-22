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
	 * Adds a rule to the policy, adding necessary modular rules if the rule
	 * contains constant facts.
	 * 
	 * @param rule
	 *            The rule to be added.
	 * @param checkModular
	 *            If we're checking for modular facts.
	 */
	public void addRule(GuidedRule rule, boolean checkModular) {
		if (!policyRules_.contains(rule)) {
			// Check if the rule contains constant facts that could invoke
			// modular rules.
			if (checkModular)
				checkModular(rule);
			policyRules_.add(rule);
		}
	}

	/**
	 * Checks if a rule contains constant facts which can be achieved using
	 * modules. If so, the rules are loaded and internally added to the policy.
	 * 
	 * TODO Ensure that the modules call other modules recursively (on calling clear)
	 * 
	 * @param rule
	 *            The rule being checked.
	 */
	private void checkModular(GuidedRule rule) {
		List<String> conditions = rule.getConditions(false);
		for (String cond : conditions) {
			// If the condition doesn't contain any variables and isn't a type
			// predicate, trigger module loading.
			String[] condSplit = StateSpec.splitFact(cond);
			if (!cond.contains(" ?")
					&& !StateSpec.getInstance().isTypePredicate(condSplit[0])) {
				Module module = Module.loadModule(StateSpec.getInstance()
						.getEnvironmentName(), condSplit[0]);
				// If the module exists
				if (module != null) {
					// Put the parameters into an arraylist
					ArrayList<String> parameters = new ArrayList<String>();
					for (int i = 1; i < condSplit.length; i++)
						parameters.add(condSplit[i]);

					// Add the module rules.
					for (GuidedRule gr : module.getModuleRules()) {
						policyRules_.add(gr.setParameters(parameters));
					}
				}
			}
		}
	}

	/**
	 * If the policy contains a rule.
	 * 
	 * @param rule
	 *            The rule being checked.
	 * @return True if the rule is within the policy, false otherwise.
	 */
	public boolean contains(GuidedRule rule) {
		return policyRules_.contains(rule);
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
			if (rule.getQueryParameters() == null) {
				buffer.append(StateSpec.getInstance().encodeRule(rule) + "\n");
			} else {
				buffer.append("MODULAR: " + StateSpec.getInstance().encodeRule(rule) + "\n");
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
				// If there are parameters, insert them here
				ValueVector vv = new ValueVector();
				if (gr.getParameters() != null) {
					for (String param : gr.getParameters())
						vv.add(param);
				}
				QueryResult results = state.runQueryStar(query, vv);

				// If there is at least one result
				if (results.next()) {
					// Only add non-modular rules
					if (gr.getQueryParameters() == null)
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