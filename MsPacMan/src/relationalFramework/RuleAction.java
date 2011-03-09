package relationalFramework;

import java.util.List;
import java.util.Map;

/**
 * A class which describes he actions returned by a specific GuidedRule.
 * 
 * @author Sam Sarjant
 */
public class RuleAction {
	/** The rule which spawned the action. */
	private GuidedRule rule_;

	/** The actions spawned by the rule. */
	private List<StringFact> actions_;

	/** The policy that created this rule action. */
	private Policy policy_;

	/**
	 * A field which notes if this rule was utilised in the environment's
	 * decision making.
	 */
	private boolean utilised_;

	public RuleAction(GuidedRule rule, List<StringFact> actionsList,
			Policy policy) {
		rule_ = rule;
		actions_ = actionsList;
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
		for (StringFact action : actions_) {
			action.replaceArguments(replacements, true);
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
	public List<StringFact> getTriggerActions() {
		// Trigger the action rule and return actions
		if (!isEmpty()) {
			policy_.addTriggeredRule(rule_);
			utilised_ = true;
		}
		return actions_;
	}

	/**
	 * Gets the actions which have been triggered or utilised by the
	 * environment.
	 * 
	 * @return The rule's actions if this was utilised otherwise null.
	 */
	public List<StringFact> getUtilisedActions() {
		if (utilised_)
			return actions_;
		return null;
	}

	/**
	 * Encodes this rule action in a readable string which gives an idea of what
	 * it does.
	 * 
	 * @return A string representing the basic idea of the action
	 */
	public String getActionString() {
		StringFact action = rule_.getAction();
		StringBuffer buffer = new StringBuffer(action.getFactName());
		Slot ruleSlot = rule_.getSlot();
		if (ruleSlot == null || ruleSlot.getSlotSplitFacts() == null)
			buffer.append("()");
		else
			buffer.append(ruleSlot.getSlotSplitFacts());
		return buffer.toString();
	}
}
