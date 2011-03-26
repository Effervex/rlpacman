package relationalFramework;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A class which describes the actions returned by a specific GuidedRule.
 * 
 * @author Sam Sarjant
 */
public class RuleAction {
	/** The rule which spawned the action. */
	private GuidedRule rule_;

	/** The actions spawned by the rule. */
	private Set<StringFact> actions_;

	/** The policy that created this rule action. */
	private Policy policy_;

	/**
	 * A field which notes if this rule was utilised in the environment's
	 * decision making.
	 */
	private boolean utilised_;

	public RuleAction(GuidedRule rule, Set<StringFact> actionsList,
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
		return rule_ + ": " + actions_.toString();
	}

	/**
	 * Gets the actions of the rule action.
	 * 
	 * @return The actions for this rule action.
	 */
	public Set<StringFact> getActions() {
		return actions_;
	}

	/**
	 * Triggers the rule within its policy.
	 */
	public void triggerRule() {
		utilised_ = true;
		policy_.addTriggeredRule(rule_);
	}

	/**
	 * Gets the actions which have been triggered or utilised by the
	 * environment.
	 * 
	 * @return The rule's actions if this was utilised otherwise null.
	 */
	public Collection<StringFact> getUtilisedActions() {
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
			buffer.append(ruleSlot.getSlotSplitFacts().iterator().next());
		return buffer.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((actions_ == null) ? 0 : actions_.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RuleAction other = (RuleAction) obj;
		if (actions_ == null) {
			if (other.actions_ != null)
				return false;
		} else if (!actions_.equals(other.actions_))
			return false;
		return true;
	}
}
