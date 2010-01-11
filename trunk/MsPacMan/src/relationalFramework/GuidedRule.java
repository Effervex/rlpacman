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

	public boolean equals(Object obj) {
		if ((obj != null) && (obj instanceof GuidedRule)) {
			GuidedRule gr = (GuidedRule) obj;
			if (rule_.equals(gr.rule_)) {
				if (((conditions_ == null) && (conditions_ == gr.conditions_))
						|| (conditions_.equals(gr.conditions_))) {
					if (((action_ == null) && (action_ == gr.action_))
							|| (action_.equals(gr.action_))) {
						if (((slot_ == null) && (slot_ == gr.slot_))
								|| (slot_.equals(gr.slot_))) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		int condVal = 71;
		if (conditions_ != null)
			condVal = slot_.hashCode();
		int actVal = 7689423;
		if (action_ != null)
			actVal = slot_.hashCode();
		int slotVal = 4563;
		if (slot_ != null)
			slotVal = slot_.hashCode();
		return rule_.hashCode() * condVal * actVal * slotVal;
	}
}
