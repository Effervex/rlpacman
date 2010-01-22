package relationalFramework;

import java.util.Collection;

import org.mandarax.kernel.Rule;

/**
 * A class that keeps track of the guided predicates that make up the rule
 * contained within.
 * 
 * @author Sam Sarjant
 */
public class GuidedRule {
	/** The actual rule represented by this class. */
	private final Rule rule_;

	/** The guided predicates that built the condition of the rule. */
	private final Collection<GuidedPredicate> conditions_;

	/** The guided predicate that defined the action. */
	private final GuidedPredicate action_;

	/** The slot this rule was generated from. */
	private final Slot slot_;

	/** If this slot is a mutation. */
	private boolean mutant_;

	/**
	 * A constructor for the guided rule.
	 * 
	 * @param rule
	 *            The rule.
	 * @param conditions
	 *            The guided conditions to build the prerequisites.
	 * @param action
	 *            The guided action to make the action.
	 */
	public GuidedRule(Rule rule, Collection<GuidedPredicate> conditions,
			GuidedPredicate action, Slot slot) {
		rule_ = rule;
		conditions_ = conditions;
		action_ = action;
		slot_ = slot;
		mutant_ = false;
	}

	public Rule getRule() {
		return rule_;
	}

	public Collection<GuidedPredicate> getConditions() {
		return conditions_;
	}

	public GuidedPredicate getAction() {
		return action_;
	}

	public Slot getSlot() {
		return slot_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action_ == null) ? 0 : action_.hashCode());
		result = prime * result
				+ ((conditions_ == null) ? 0 : conditions_.hashCode());
		result = prime * result + (mutant_ ? 1231 : 1237);
		result = prime * result + ((rule_ == null) ? 0 : rule_.hashCode());
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
		final GuidedRule other = (GuidedRule) obj;
		if (action_ == null) {
			if (other.action_ != null)
				return false;
		} else if (!action_.equals(other.action_))
			return false;
		if (conditions_ == null) {
			if (other.conditions_ != null)
				return false;
		} else if (!conditions_.equals(other.conditions_))
			return false;
		if (mutant_ != other.mutant_)
			return false;
		if (rule_ == null) {
			if (other.rule_ != null)
				return false;
		} else if (!rule_.equals(other.rule_))
			return false;
		if (slot_ == null) {
			if (other.slot_ != null)
				return false;
		} else if (!slot_.equals(other.slot_))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (conditions_ == null) {
			return rule_.toString();
		}
		return conditions_.toString() + " " + StateSpec.INFERS_ACTION + " "
				+ action_.toString();
	}
}
