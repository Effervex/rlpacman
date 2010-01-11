package relationalFramework;

import org.mandarax.kernel.Predicate;

/**
 * An inner class forming the slot of the policy generator. Contains the
 * rules within and any other pertinent information.
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
	 * Freezes or unfreezes this slot.
	 * 
	 * @param freeze To freeze or unfreeze.
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
}
