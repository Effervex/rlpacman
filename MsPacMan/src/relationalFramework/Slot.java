package relationalFramework;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An inner class forming the slot of the policy generator. Contains the rules
 * within and any other pertinent information.
 * 
 * @author Samuel J. Sarjant
 */
public class Slot {
	private static final String ELEMENT_DELIMITER = ":";

	/** The rule generator within the slot. */
	private ProbabilityDistribution<GuidedRule> ruleGenerator_;

	/** The backup generator for the slot. */
	private ProbabilityDistribution<GuidedRule> backupGenerator_;

	/** The action which all rules within lead to. */
	private String action_;

	/** If this slot is fixed. */
	private boolean fixed_ = false;

	/**
	 * The constructor for a new Slot.
	 * 
	 * @param action
	 *            The action the slot rules lead to.
	 */
	public Slot(String action) {
		action_ = action;
		ruleGenerator_ = new ProbabilityDistribution<GuidedRule>();
	}

	/**
	 * Adds a new rule to the slot with an average probability of being
	 * selected.
	 * 
	 * @param guidedRule
	 *            The rule being added.
	 * @param duplicates
	 *            If addition allows duplicates
	 */
	public void addNewRule(GuidedRule guidedRule, boolean duplicates) {
		if (duplicates || !ruleGenerator_.contains(guidedRule)) {
			if (ruleGenerator_.isEmpty())
				ruleGenerator_.add(guidedRule, 1);
			else {
				double averageProb = 1.0 / ruleGenerator_.size();
				ruleGenerator_.add(guidedRule, averageProb);
				ruleGenerator_.normaliseProbs();
			}
			guidedRule.setSlot(this);
		}
	}

	/**
	 * Adds a new rule to the slot with an given probability of being selected.
	 * 
	 * @param guidedRule
	 *            The rule being added.
	 */
	private void addNewRule(GuidedRule guidedRule, double prob) {
		ruleGenerator_.add(guidedRule, prob);
	}

	/**
	 * Checks if this slot contains a rule within it's generator.
	 * 
	 * @param gr
	 *            The rule being checked for.
	 * @return True if the rule is within the slot, false otherwise.
	 */
	public boolean contains(GuidedRule gr) {
		return ruleGenerator_.contains(gr);
	}

	/**
	 * Freezes or unfreezes this slot.
	 * 
	 * @param freeze
	 *            To freeze or unfreeze.
	 */
	public void freeze(boolean freeze) {
		if (freeze) {
			backupGenerator_ = ruleGenerator_;

			ruleGenerator_ = ruleGenerator_.bindProbs(false);
		} else {
			ruleGenerator_ = backupGenerator_;
		}
	}

	/**
	 * Gets the slot's rule generator.
	 * 
	 * @return The slot's rule generator.
	 */
	public ProbabilityDistribution<GuidedRule> getGenerator() {
		return ruleGenerator_;
	}

	/**
	 * Gets the action this slot covers.
	 * 
	 * @return The action the slot covers.
	 */
	public String getAction() {
		return action_;
	}

	/**
	 * If this slot is fixed (not necessarily frozen).
	 * 
	 * @return If this slot is fixed.
	 */
	public boolean isFixed() {
		return fixed_;
	}

	/**
	 * Fixes the slot in place.
	 */
	public void fixSlot() {
		fixed_ = true;
		// TODO Fix the slot in place by creating a single rule for it
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj != null) && (obj instanceof Slot)) {
			Slot sl = (Slot) obj;
			if (ruleGenerator_.equals(sl.ruleGenerator_)) {
				if (action_.equals(sl.action_)) {
					if (fixed_ == sl.fixed_) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		int val = (fixed_) ? 31 : 73;
		return ruleGenerator_.hashCode() * action_.hashCode() + val;
	}

	@Override
	public String toString() {
		String result = (fixed_) ? "FIXED " : "";
		return result + "Slot (" + action_.toString() + "):\n"
				+ ruleGenerator_.toString();
	}

	/**
	 * Converts this slot into a parsable string format.
	 * 
	 * @return This slot in string format.
	 */
	public String toParsableString() {
		StringBuffer buffer = new StringBuffer();
		if (fixed_)
			buffer.append("FIXED" + ELEMENT_DELIMITER);
		buffer.append(action_ + "{");
		for (int i = 0; i < ruleGenerator_.size(); i++) {
			if (ruleGenerator_.getProb(i) > 0)
				buffer.append("(" + ruleGenerator_.getElement(i)
						+ ELEMENT_DELIMITER + ruleGenerator_.getProb(i) + ")");
		}
		buffer.append("}");
		return buffer.toString();
	}

	/**
	 * Parses a slot from a parsable slot string.
	 * 
	 * @param slotString
	 *            The string detailing the slot.
	 * @return A new slot, which can be formatted into the same input string.
	 */
	public static Slot parseSlotString(String slotString) {
		int index = 0;
		// Checking if slot is fixed
		boolean fixed = false;
		int fixIndex = "FIXED".length() + ELEMENT_DELIMITER.length();
		if (slotString.substring(0, fixIndex).equals(
				"FIXED" + ELEMENT_DELIMITER)) {
			fixed = true;
			index = fixIndex;
		}

		// Finding the slot action
		int bracketIndex = slotString.indexOf('{');
		String action = slotString.substring(index, bracketIndex);
		Slot slot = new Slot(action);

		// Parsing the rules and adding them
		// Group 1 is the rule, group 2 is the prob
		Pattern p = Pattern.compile("\\((.+? => .+?):([0-9.]+)\\)");
		Matcher m = p.matcher(slotString.substring(bracketIndex + 1));
		while (m.find()) {
			GuidedRule guidedRule = new GuidedRule(m.group(1), slot);
			double prob = Double.parseDouble(m.group(2));
			slot.addNewRule(guidedRule, prob);
		}

		if (fixed)
			slot.fixSlot();

		return slot;
	}
}
