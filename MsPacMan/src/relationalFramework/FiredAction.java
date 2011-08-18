package relationalFramework;

import relationalFramework.FiredAction;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import cerrla.Slot;

/**
 * An action fired from a rule (probably from within a policy) with the given
 * arguments.
 * 
 * @author Sam Sarjant
 */
public class FiredAction implements Comparable<FiredAction> {
	/** The actual action. */
	private RelationalPredicate action_;

	/** The rule the action fired from. */
	private RelationalRule firingRule_;

	/** The policy the rule was stored in (if any). */
	private RelationalPolicy firingPolicy_;

	/** If this action has been utilised for acting. */
	private boolean utilised_ = false;

	/**
	 * A fired action from a policy-less rule.
	 * 
	 * @param action
	 *            The action.
	 * @param rule
	 *            The rule which created the action.
	 */
	public FiredAction(RelationalPredicate action, RelationalRule rule) {
		action_ = action;
		firingRule_ = rule;
		firingPolicy_ = null;
	}

	/**
	 * A fired action from a rule within a policy.
	 * 
	 * @param action
	 *            The action.
	 * @param rule
	 *            The rule which created the action.
	 * @param policy
	 *            The policy the rule was stored in.
	 */
	public FiredAction(RelationalPredicate action, RelationalRule rule,
			RelationalPolicy policy) {
		action_ = action;
		firingRule_ = rule;
		firingPolicy_ = policy;
	}

	/**
	 * Triggers the rule within its policy.
	 */
	public void triggerRule() {
		utilised_ = true;
		firingPolicy_.addTriggeredRule(firingRule_);
	}

	/**
	 * Encodes this rule action in a readable string which gives an idea of what
	 * it does.
	 * 
	 * @return A string representing the basic idea of the action
	 */
	public String getActionString() {
		RelationalPredicate action = firingRule_.getAction();
		StringBuffer buffer = new StringBuffer(action.getFactName());
		Slot ruleSlot = firingRule_.getSlot();
		if (ruleSlot == null || ruleSlot.getSlotSplitFacts().isEmpty())
			buffer.append("()");
		else
			buffer.append(ruleSlot.getSlotSplitFacts().iterator().next());
		return buffer.toString();
	}

	public RelationalPredicate getAction() {
		return action_;
	}

	public RelationalRule getFiringRule() {
		return firingRule_;
	}

	public RelationalPolicy getFiringPolicy() {
		return firingPolicy_;
	}

	public boolean isUtilised() {
		return utilised_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action_ == null) ? 0 : action_.hashCode());
		result = prime * result
				+ ((firingPolicy_ == null) ? 0 : firingPolicy_.hashCode());
		result = prime * result
				+ ((firingRule_ == null) ? 0 : firingRule_.hashCode());
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
		FiredAction other = (FiredAction) obj;
		if (action_ == null) {
			if (other.action_ != null)
				return false;
		} else if (!action_.equals(other.action_))
			return false;
		if (firingPolicy_ == null) {
			if (other.firingPolicy_ != null)
				return false;
		} else if (!firingPolicy_.equals(other.firingPolicy_))
			return false;
		if (firingRule_ == null) {
			if (other.firingRule_ != null)
				return false;
		} else if (!firingRule_.equals(other.firingRule_))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return action_.toString();
	}

	@Override
	public int compareTo(FiredAction o) {
		if (o == null)
			return -1;
		int result = action_.compareTo(o.action_);
		if (result != 0)
			return result;
		result = firingRule_.compareTo(o.firingRule_);
		if (result != 0)
			return result;
		if (firingPolicy_ == null) {
			if (o.firingPolicy_ != null)
				return 1;
		} else {
			if (o.firingPolicy_ == null)
				return -1;
			else
				return Double.compare(firingPolicy_.hashCode(),
						o.firingPolicy_.hashCode());
		}
		return 0;
	}
}
