package relationalFramework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.BidiMap;

import cerrla.modular.PolicyItem;

import relationalFramework.agentObservations.LocalAgentObservations;
import relationalFramework.agentObservations.RangeContext;
import rrlFramework.RRLObservations;

import jess.QueryResult;
import jess.Rete;
import jess.ValueVector;

/**
 * A skeletal implementation of a relational policy.
 * 
 * @author Sam Sarjant
 */
public class RelationalPolicy implements Serializable {
	private static final long serialVersionUID = -5575181715619476335L;
	/** The rules of this policy, organised in a deterministic list format. */
	protected List<PolicyItem> policyRules_;
	/** The number of rules added to this policy (excluding modular) */
	protected int policySize_;

	/**
	 * Basic constructor.
	 */
	public RelationalPolicy() {
		policyRules_ = new ArrayList<PolicyItem>();
		policySize_ = 0;
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
	 * @param isTransient
	 *            TODO
	 * @return A collection of actions which the rule creates.
	 */
	protected final Collection<FiredAction> evaluateRule(RelationalRule rule,
			Rete state, SortedSet<String[]> validActions,
			SortedSet<String[]> activatedActions, boolean isTransient)
			throws Exception {
		Collection<FiredAction> returnedActions = new TreeSet<FiredAction>();

		// Forming the query
		String query = StateSpec.getInstance().getRuleQuery(rule, isTransient);
		// If there are parameters, temp or concrete, insert them here
		ValueVector vv = new ValueVector();
		for (RangeContext rangeContext : rule.getRangeContexts()) {
			double[] minMax = LocalAgentObservations.getActionRanges(
					rangeContext, null);
			if (minMax != null) {
				vv.add(minMax[0]);
				vv.add(minMax[1]);
			} else {
				vv.add(Integer.MIN_VALUE);
				vv.add(Integer.MAX_VALUE);
			}
		}
		if (rule.getQueryParameters() != null) {
			if (rule.getParameters() != null) {
				for (RelationalArgument param : rule.getParameters())
					vv.add(param.toString());
			} else {
				// Use anonymous placeholder
				for (int i = 0; i < rule.getQueryParameters().size(); i++)
					vv.add(RelationalArgument.ANONYMOUS.toString());
			}
		}
		QueryResult results = state.runQueryStar(query, vv);

		// If there is at least one result
		if (results.next()) {
			// For each possible replacement
			do {
				// Find the arguments.
				String[] arguments = rule.getAction().getArguments();
				try {
					for (int i = 0; i < arguments.length; i++) {
						// If the action is variable, use the replacement
						if (arguments[i].charAt(0) == '?')
							arguments[i] = results.getSymbol(arguments[i]
									.substring(1));
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
	 * Adds a rule to the policy.
	 * 
	 * @param rule
	 *            The rule to be added.
	 */
	public void addRule(RelationalRule rule) {
		if (!policyRules_.contains(rule)) {
			policyRules_.add(rule);
			policySize_++;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RelationalPolicy other = (RelationalPolicy) obj;
		if (policyRules_ == null) {
			if (other.policyRules_ != null)
				return false;
		} else if (!policyRules_.equals(other.policyRules_))
			return false;
		return true;
	}

	/**
	 * Evaluates a policy for a number of firing rules, and switches on the
	 * necessary rules.
	 * 
	 * @param observations
	 *            The observations of the state.
	 * @param actionsReturned
	 *            The number of actions to be returned, or if -1, all actions.
	 * @return The actions being returned by the policy.
	 */
	public PolicyActions evaluatePolicy(RRLObservations observations,
			int actionsReturned) {
		PolicyActions actionSwitch = new PolicyActions();
		int actionsFound = 0;
		int actionsReturnedModified = (actionsReturned <= -1) ? Integer.MAX_VALUE
				: actionsReturned;

		try {
			// Evaluate the policy rules.
			Iterator<PolicyItem> iter = policyRules_.iterator();
			while (iter.hasNext() && actionsFound < actionsReturnedModified) {
				RelationalRule polRule = (RelationalRule) iter.next();
				Collection<FiredAction> firedActions = evaluateRule(polRule,
						observations.getState(),
						observations.getValidActions(polRule
								.getActionPredicate()), null, false);
				actionSwitch.addFiredRule(firedActions, this);
				actionsFound += firedActions.size();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return actionSwitch;
	}

	/**
	 * Gets the rules of this object.
	 * 
	 * @return The rules/relationally evaluable objects of the policy.
	 */
	public List<PolicyItem> getRules() {
		return policyRules_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((policyRules_ == null) ? 0 : policyRules_.hashCode());
		return result;
	}

	/**
	 * Apply arguments to any parameterised rules contained within this policy.
	 * 
	 * @param goalState
	 *            The arguments to apply to the parameters.
	 */
	public void parameterArgs(BidiMap goalArgs) {
		// Set the parameters for the policy rules.
		for (PolicyItem reo : policyRules_) {
			reo.setParameters(goalArgs);
		}
	}

	@Override
	public String toString() {
		if (policyRules_.isEmpty())
			return "<EMPTY POLICY>";

		StringBuffer buffer = new StringBuffer("Policy");
		buffer.append(":\n");

		boolean first = true;
		for (PolicyItem reo : policyRules_) {
			if (!first)
				buffer.append("\n");
			buffer.append(reo.toNiceString());
			first = false;
		}
		return buffer.toString();
	}

	/**
	 * Loads a policy from file.
	 * 
	 * @param polFile
	 *            The policy file.
	 * @return The loaded policy.
	 */
	public static RelationalPolicy loadPolicyFile(File polFile)
			throws Exception {
		RelationalPolicy policy = new RelationalPolicy();
		FileReader fr = new FileReader(polFile);
		BufferedReader br = new BufferedReader(fr);

		String input = null;
		while ((input = br.readLine()) != null)
			policy.addRule(new RelationalRule(input));

		br.close();
		fr.close();
		return policy;
	}

	public int size() {
		return policySize_;
	}
}
