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
			GuidedPredicate action) {
		rule_ = rule;
		conditions_ = conditions;
		action_ = action;
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

	public boolean equals(Object obj) {
		if ((obj != null) && (obj instanceof GuidedRule)) {
			GuidedRule gr = (GuidedRule) obj;
			if (rule_.equals(gr.rule_)) {
				if (conditions_.equals(gr.conditions_)) {
					if (action_.equals(gr.action_)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public int hashCode() {
		return rule_.hashCode() * conditions_.hashCode() * action_.hashCode();
	}
}
