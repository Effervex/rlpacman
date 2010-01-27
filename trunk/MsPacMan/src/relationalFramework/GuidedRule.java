package relationalFramework;

import java.util.ArrayList;
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
	private Collection<GuidedPredicate> conditions_;

	/** The guided predicate that defined the action. */
	private GuidedPredicate action_;

	/** The parsable string which defines the rule. */
	private String ruleString_;

	/** The slot this rule was generated from. */
	private Slot slot_;

	/** If this slot is a mutation. */
	private boolean mutant_;
	
	/** If this rule has spawned any mutant rules yet. */
	private boolean hasSpawned_;

	/** If this rule is maximally general. */
	private boolean maxGeneral_;

	/**
	 * A constructor taking the bare minimum for a guided rule.
	 * 
	 * @param rule
	 *            The rule this rule represents
	 */
	public GuidedRule(Rule rule) {
		rule_ = rule;
	}

	/**
	 * A constructor taking the bare minimum for a guided rule.
	 * 
	 * @param ruleString
	 *            The string representing this rule.
	 * @param maxGeneral
	 *            If this rule is maximally general.
	 * @param mutant
	 *            If this rule is a mutant (implying max general is false).
	 */
	public GuidedRule(String ruleString, boolean maxGeneral, boolean mutant) {
		rule_ = StateSpec.getInstance().parseRule(ruleString, null);
		ruleString_ = ruleString;
		maxGeneral_ = maxGeneral;
		mutant_ = mutant;
	}

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

	public void setConditions(ArrayList<GuidedPredicate> conditions) {
		conditions_ = conditions;
	}

	public GuidedPredicate getAction() {
		return action_;
	}
	
	public void setAction(GuidedPredicate action) {
		action_ = action;
	}

	public Slot getSlot() {
		return slot_;
	}

	public void setSlot(Slot slot) {
		slot_ = slot;
	}

	public boolean isMaximallyGeneral() {
		return maxGeneral_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action_ == null) ? 0 : action_.hashCode());
		result = prime * result
				+ ((conditions_ == null) ? 0 : conditions_.hashCode());
		result = prime * result + (maxGeneral_ ? 1231 : 1237);
		result = prime * result + (mutant_ ? 1231 : 1237);
		result = prime * result
				+ ((ruleString_ == null) ? 0 : ruleString_.hashCode());
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
		GuidedRule other = (GuidedRule) obj;
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
		if (maxGeneral_ != other.maxGeneral_)
			return false;
		if (mutant_ != other.mutant_)
			return false;
		if (ruleString_ == null) {
			if (other.ruleString_ != null)
				return false;
		} else if (!ruleString_.equals(other.ruleString_))
			return false;
		if (rule_ == null) {
			if (other.rule_ != null)
				return false;
		} else if (!rule_.equals(other.rule_))
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
