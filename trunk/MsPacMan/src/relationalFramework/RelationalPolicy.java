package relationalFramework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import jess.QueryResult;
import jess.Rete;
import jess.ValueVector;
import util.MultiMap;

/**
 * A skeletal implementation of a relational policy.
 * 
 * @author Sam Sarjant
 */
public abstract class RelationalPolicy implements Serializable {
	private static final long serialVersionUID = -5575181715619476335L;
	/** The rules of this policy, organised in a deterministic list format. */
	protected List<RelationalRule> policyRules_;
	/** The number of rules added to this policy (excluding modular) */
	protected int policySize_;

	/**
	 * Basic constructor.
	 */
	public RelationalPolicy() {
		policyRules_ = new ArrayList<RelationalRule>();
		policySize_ = 0;
	}

	/**
	 * Adds a rule to the policy.
	 * 
	 * @param rule
	 *            The rule to be added.
	 */
	public void addRule(RelationalRule rule) {
		policyRules_.add(rule);
		policySize_++;
	}

	/**
	 * Evaluates a single rule against the state to see which actions fire.
	 * 
	 * @param rule
	 *            The rule to be evaluated.
	 * @param state
	 *            The state to be evaluated against.
	 * @param validActions
	 *            The set of valid actions the agent can take at this state.
	 * @param activatedActions
	 *            The (possibly null) to-be-filled activated actions.
	 * @return A collection of actions which the rule creates.
	 */
	protected final Collection<FiredAction> evaluateRule(RelationalRule rule,
			Rete state, SortedSet<String[]> validActions,
			SortedSet<String[]> activatedActions) throws Exception {
		Collection<FiredAction> returnedActions = new TreeSet<FiredAction>();

		// Forming the query
		String query = StateSpec.getInstance().getRuleQuery(rule);
		// If there are parameters, temp or concrete, insert them here
		ValueVector vv = new ValueVector();
		if (rule.getQueryParameters() != null) {
			if (rule.getParameters() != null) {
				for (String param : rule.getParameters())
					vv.add(param);
			} else {
				// Use anonymous placeholder
				for (int i = 0; i < rule.getQueryParameters().size(); i++)
					vv.add(StateSpec.ANONYMOUS);
			}
		}
		QueryResult results = state.runQueryStar(query, vv);

		// If there is at least one result
		if (results.next()) {
			// For each possible replacement
			do {
				// Find the arguments.
				String[] arguments = rule.getAction().getArguments();
				for (int i = 0; i < arguments.length; i++) {
					// If the action is variable, use the replacement
					if (arguments[i].charAt(0) == '?')
						arguments[i] = results.getSymbol(arguments[i]
								.substring(1));
				}

				// Check this is a valid action
				if (isValidAction(arguments, validActions)) {
					if (activatedActions != null)
						activatedActions.add(arguments);

					// Create the swapped action
					RelationalPredicate action = new RelationalPredicate(
							rule.getAction(), arguments);
					returnedActions.add(new FiredAction(action, rule, this));
				}
			} while (results.next());
		}
		results.close();

		return returnedActions;
	}

	/**
	 * Checks if this action is in the valid actions.
	 * 
	 * @param actionArgs
	 *            The action args being checked.
	 * @param validArgs
	 *            The set of valid action args.
	 * @return True if the action is valid, false otherwise.
	 */
	private boolean isValidAction(String[] actionArgs,
			SortedSet<String[]> validArgs) {
		// If there are no chances for this action at all, return false.
		if (validArgs == null)
			return false;

		if (validArgs.contains(actionArgs))
			return true;
		return false;
	}

	/**
	 * Evaluates a policy for a number of firing rules, and switches on the
	 * necessary rules.
	 * 
	 * @param state
	 *            The current state in predicates.
	 * @param validActions
	 *            The set of valid actions to choose from in the state.
	 * @param goalReplacements
	 *            The goal term replacements.
	 * @param actionsReturned
	 *            The number of actions to be returned, or if -1, all actions.
	 * @return The actions being returned by the policy.
	 */
	public abstract PolicyActions evaluatePolicy(Rete state,
			MultiMap<String, String[]> validActions,
			Map<String, String> goalReplacements, int actionsReturned);

	/**
	 * Apply arguments to any parameterised rules contained within this policy.
	 * 
	 * @param goalState
	 *            The arguments to apply to the parameters.
	 */
	public void parameterArgs(Map<String, String> goalArgs) {
		List<String> params = null;
		if (goalArgs != null) {
			params = new ArrayList<String>();
			for (int i = 0; i < goalArgs.size(); i++)
				params.add(goalArgs.get(StateSpec.createGoalTerm(i)));
		}

		// Set the parameters for the policy rules.
		for (RelationalRule gr : policyRules_) {
			gr.setParameters(params);
		}
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
			buffer.append(rule.toNiceString());
			first = false;
		}
		return buffer.toString();
	}
}
