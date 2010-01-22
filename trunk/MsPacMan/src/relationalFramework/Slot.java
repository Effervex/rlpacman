package relationalFramework;

import org.mandarax.kernel.Predicate;

/**
 * An inner class forming the slot of the policy generator. Contains the rules
 * within and any other pertinent information.
 * 
 * @author Samuel J. Sarjant
 */
public class Slot {
	/** The rule generator within the slot. */
	private ProbabilityDistribution<GuidedRule> ruleGenerator_;

	/** The backup generator for the slot. */
	private ProbabilityDistribution<GuidedRule> backupGenerator_;

	/** The action which all rules within lead to. */
	private Predicate action_;

	/** If this slot is fixed. */
	private boolean fixed_ = false;

	/**
	 * The constructor for a new Slot.
	 * 
	 * @param action
	 *            The action the slot rules lead to.
	 */
	public Slot(Predicate action) {
		action_ = action;
		ruleGenerator_ = new ProbabilityDistribution<GuidedRule>();
	}

	/**
	 * Adds a new rule to the slot with an average probability of being
	 * selected.
	 * 
	 * @param guidedRule
	 *            The rule being added.
	 */
	public void addNewRule(GuidedRule guidedRule) {
		if (ruleGenerator_.isEmpty())
			ruleGenerator_.add(guidedRule, 1);
		else {
			double averageProb = 1 / ruleGenerator_.size();
			ruleGenerator_.add(guidedRule, averageProb);
			ruleGenerator_.normaliseProbs();
		}
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
	public Predicate getAction() {
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
}
