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
	private final String ruleConditions_;

	/** The guided predicate that defined the action. */
	private final String ruleAction_;

	/** The slot this rule was generated from. */
	private Slot slot_;

	/** If this slot is a mutation. */
	private boolean mutant_ = false;

	/** If this rule has spawned any mutant rules yet. */
	private boolean hasSpawned_ = false;

	/** If this rule is maximally general. */
	private boolean maxGeneral_ = false;

	/**
	 * A constructor taking the bare minimum for a guided rule.
	 * 
	 * @param rule
	 *            The rule this rule represents
	 */
	public GuidedRule(String ruleString) {
		String[] split = ruleString.split("=>");
		ruleConditions_ = split[0].trim();
		ruleAction_ = split[1].trim();
		slot_ = null;
	}

	/**
	 * A constructor taking the rule and slot.
	 * 
	 * @param rule
	 *            The rule this rule represents
	 * @param slot
	 *            The slot this rule is under.
	 */
	public GuidedRule(String ruleString, Slot slot) {
		this(ruleString);
		slot_ = slot;
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
	public GuidedRule(String ruleString, boolean maxGeneral, boolean mutant,
			Slot slot) {
		this(ruleString);
		maxGeneral_ = maxGeneral;
		mutant_ = mutant;
		slot_ = slot;
	}

	public Slot getSlot() {
		return slot_;
	}

	public String getConditions() {
		return ruleConditions_;
	}

	public String getAction() {
		return ruleAction_;
	}

	public void setSlot(Slot slot) {
		slot_ = slot;
	}

	public boolean isMaximallyGeneral() {
		return maxGeneral_;
	}

	@Override
	public String toString() {
		return ruleConditions_ + " => " + ruleAction_;
	}
}
