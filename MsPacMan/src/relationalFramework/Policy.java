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
	 * @param rule
	 *            The rule being checked.
	 */
	private void checkModular(GuidedRule rule) {
		List<String> conditions = rule.getConditions(false);
		for (String cond : conditions) {
			// If the condition doesn't contain any variables and isn't a type
			// predicate, trigger module loading.
			String[] condSplit = StateSpec.splitFact(cond);
			if (isModularisable(condSplit, rule.getQueryParameters())) {
				Module module = Module.loadModule(StateSpec.getInstance()
						.getEnvironmentName(), condSplit[0]);
				// If the module exists
				if (module != null) {
					// Put the parameters into an arraylist
					ArrayList<String> parameters = new ArrayList<String>();
					for (int i = 1; i < condSplit.length; i++) {
						// May need to replace parameters if modular is
						// recursive
						if (rule.getParameters() != null) {
							parameters.add(rule
									.getReplacementParameter(condSplit[i]));
						} else {
							parameters.add(condSplit[i]);
						}
					}

					// Add the module rules.
					for (GuidedRule gr : module.getModuleRules()) {
						gr.setParameters(parameters);
						checkModular(gr);
						policyRules_.add(gr);
					}
				}
			}
		}
	}

	/**
	 * Small function for determining if a condition is modularisable.
	 * 
	 * @param condSplit
	 *            The condition split up.
	 * @param queryParams
	 *            The query parameters for the rule, if any.
	 * @return True if the condition is modularisable (is a constant or
	 *         parameterised constant).
	 */
	private boolean isModularisable(String[] condSplit, List<String> queryParams) {
		// Ignore type predicates
		if (StateSpec.getInstance().isTypePredicate(condSplit[0]))
			return false;

		for (int i = 1; i < condSplit.length; i++) {
			// If we're looking at a variable
			if (condSplit[i].contains("?")) {
				// It may be a parameter, else return false.
				if ((queryParams == null)
						|| (!queryParams.contains(condSplit[i])))
					return false;
			}
		}
		return true;
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

	/**
	 * Gets the rules that this policy is made up of.
	 * 
	 * @return The rule for the policy.
	 */
	public Collection<GuidedRule> getPolicyRules() {
		return policyRules_;
	}

	/**
	 * Apply arguments to any parameterised rules contained within this policy.
	 * 
	 * @param arguments
	 *            The arguments to apply to the parameters.
	 */
	public void parameterArgs(ValueVector arguments) {
		for (GuidedRule gr : policyRules_) {
			gr.setParameters(arguments);
		}
	}

	@Override
	public String toString() {
		if (policyRules_.isEmpty())
			return "<EMPTY POLICY>";

		StringBuffer buffer = new StringBuffer("Policy:\n");
		for (GuidedRule rule : policyRules_) {
			if (!rule.isLoadedModuleRule()) {
				buffer.append(StateSpec.getInstance().encodeRule(rule) + "\n");
			} else {
				buffer.append("MODULAR: "
						+ StateSpec.getInstance().encodeRule(rule) + "\n");
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
	 *            The number of actions to be returned, or if -1, all actions.
	 * @param optimal
	 *            If the policy is an optimal test one.
	 * @param alreadyCovered
	 *            If the policy has already covered this iteration (due to
	 *            recursive calls).
	 * @param noteTriggered
	 *            If this policy is noting the rules fired as triggered. Usually
	 *            deactivated after agent has found internal goal.
	 */
	public void evaluatePolicy(Rete state, ActionSwitch actionSwitch,
			int actionsReturned, boolean optimal, boolean alreadyCovered,
			boolean noteTriggered) {
		
		if (actionsReturned == -1)
			actionsReturned = Integer.MAX_VALUE;

		// Check every slot, from top-to-bottom until one activates
		int actionsFound = 0;
		Iterator<GuidedRule> iter = policyRules_.iterator();
		while ((actionsFound < actionsReturned) && (iter.hasNext())) {
			GuidedRule gr = iter.next();

			// Find the result set
			// TODO Evaluate rules in a step-wise fashion, to infer how much of
			// a rule fires
			try {
				// Forming the query
				String query = StateSpec.getInstance().getRuleQuery(gr);
				// If there are parameters, temp or concrete, insert them here
				ValueVector vv = new ValueVector();
				if (gr.getParameters() != null) {
					for (String param : gr.getParameters())
						vv.add(param);
				}
				QueryResult results = state.runQueryStar(query, vv);

				// If there is at least one result
				if (results.next()) {
					// Only add non-modular rules if we're noting rules.
					if (!gr.isLoadedModuleRule() && noteTriggered)
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
										.nextInt(actionsList.size())));
						actionsFound++;
					}
				}
				results.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// If optimal, just exit
		if (optimal)
			return;

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
			evaluatePolicy(state, actionSwitch, actionsReturned, optimal, true,
					noteTriggered);
		} else if (!alreadyCovered) {
			PolicyGenerator.getInstance().triggerCovering(state, false);
		}
	}
}