package relationalFramework;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A class which describes he actions returned by a specific GuidedRule.
 * 
 * @author Sam Sarjant
 */
public class RuleAction {
	/** The rule which spawned the action. */
	private GuidedRule rule_;

	/** The actions spawned by the rule. */
	private List<String> actions_;

	/** The policy that created this rule action. */
	private Policy policy_;

	/**
	 * A field which notes if this rule was utilised in the environment's
	 * decision making.
	 */
	private boolean utilised_;

	public RuleAction(GuidedRule rule, List<String> actions, Policy policy) {
		rule_ = rule;
		actions_ = actions;
		policy_ = policy;
	}

	/**
	 * Replaces the terms in the actions with another term.
	 * 
	 * @param replacements
	 *            The replacement map for terms.
	 */
	public void replaceTerms(Map<String, String> replacements) {
		// For each action
		for (int i = 0; i < actions_.size(); i++) {
			String action = actions_.get(i);
			for (String constant : replacements.keySet()) {
				action = action.replaceAll(" " + Pattern.quote(constant)
						+ "(?=( |\\)))", " " + replacements.get(constant));
			}
			actions_.set(i, action);
		}
	}

	public boolean isEmpty() {
		return actions_.isEmpty();
	}

	public GuidedRule getRule() {
		return rule_;
	}

	@Override
	public String toString() {
		return actions_.toString();
	}

	/**
	 * Gets the actions of the rule action and activates the trigger in the
	 * policy.
	 * 
	 * @return The actions for this rule action and also triggers the action in
	 *         the policy.
	 */
	public List<String> getTriggerActions() {
		// Trigger the action rule and return actions
		if (!isEmpty()) {
			policy_.addTriggeredRule(rule_);
			utilised_ = true;
		}
		return actions_;
	}

	/**
	 * Gets the actions which have been triggered or utilised by the environment.
	 * 
	 * @return The rule's actions if this was utilised otherwise null.
	 */
	public List<String> getUtilisedActions() {
		if (utilised_)
			return actions_;
		return null;
	}
}
