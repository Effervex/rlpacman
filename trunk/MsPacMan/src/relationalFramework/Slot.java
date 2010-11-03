package relationalFramework;

import java.util.Random;
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

	/** The maximum amount of variance the selection probability can have. */
	public static final double MAX_SELECTION_VARIANCE = 0.5;

	/** The rule generator within the slot. */
	private ProbabilityDistribution<GuidedRule> ruleGenerator_;

	/** The backup generator for the slot. */
	private ProbabilityDistribution<GuidedRule> backupGenerator_;

	/** The action which all rules within lead to. */
	private String action_;

	/** If this slot is fixed. */
	private boolean fixed_ = false;

	/** The chance of this slot being selected. */
	private double selectionProb_;

	/** The variance on the chances of the slot being selected. */
	private double selectionVariance_;

	/**
	 * The current selection probability of the slot being used. Reset every
	 * time the slot is called upon for selection.
	 */
	private double currentSelectionProb_;

	/**
	 * The constructor for a new Slot.
	 * 
	 * @param action
	 *            The action the slot rules lead to.
	 */
	public Slot(String action) {
		action_ = action;
		ruleGenerator_ = new ProbabilityDistribution<GuidedRule>(
				PolicyGenerator.random_);
		selectionProb_ = 1.0;
		selectionVariance_ = MAX_SELECTION_VARIANCE;
		currentSelectionProb_ = 0;
	}

	/**
	 * Adds a new rule to the slot with an average probability of being
	 * selected.
	 * 
	 * @param guidedRule
	 *            The rule being added.
	 */
	public void addNewRule(GuidedRule guidedRule) {
		if (!ruleGenerator_.contains(guidedRule)) {
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
	 * Adds a rule to the slot with a set probability. Used when loading
	 * existing generators.
	 * 
	 * @param guidedRule
	 *            The rule being loaded.
	 * @param ruleProb
	 *            The probability of the rule.
	 */
	public void addRule(GuidedRule guidedRule, double ruleProb) {
		ruleGenerator_.add(guidedRule, ruleProb);
		guidedRule.setSlot(this);
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
	 * Checks if this slot should be used and also if it should stay in the
	 * distribution. Note that if it is not used, it must be removed.
	 * 
	 * @param random
	 *            The random number generator used for generating a gaussian
	 *            value.
	 * @param frozen
	 *            If the generator is frozen, in which case selection is split
	 *            uniformly without variance.
	 * @return A boolean array of two elements: whether to use the slot and
	 *         whether to keep the slot. Note that !Use -> !Keep
	 */
	public boolean[] shouldUseSlot(Random random, boolean frozen) {
		boolean[] useSlot = new boolean[2];
		if (currentSelectionProb_ <= 0) {
			// The slot has not been sampled yet and needs to set a selection
			// probability.
			if (frozen) {
				// If frozen, just use the rounded value for selection
				currentSelectionProb_ = Math.round(selectionProb_);
			} else {
				// Selection prob becomes a gaussian value centred about the
				// selection prob.
				double gauss = random.nextGaussian();
				currentSelectionProb_ = selectionProb_ + gauss
						* selectionVariance_;
			}
		}

		// Randomly use the slot with probability currentSelectionProb
		if (random.nextDouble() < currentSelectionProb_)
			useSlot[0] = true;
		else
			useSlot[0] = false;

		currentSelectionProb_ -= 1;
		if (currentSelectionProb_ > 0)
			useSlot[1] = true;
		else
			useSlot[1] = false;

		return useSlot;
	}

	/**
	 * Updates the selection values of this slot stepwise towards the given
	 * values.
	 * 
	 * @param mean
	 *            The mean value to step towards.
	 * @param variance
	 *            The variance to step towards.
	 * @param stepSize
	 *            The amount the value should update by.
	 */
	public void updateSelectionValues(double mean, double variance,
			double stepSize) {
		selectionProb_ = mean * stepSize + (1 - stepSize) * selectionProb_;
		selectionVariance_ = variance * stepSize + (1 - stepSize)
				* selectionVariance_;

		// Capping variance at max.
		if (selectionVariance_ > MAX_SELECTION_VARIANCE)
			selectionVariance_ = MAX_SELECTION_VARIANCE;
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
	 * Creates an influenced distribution which is more likely to sample rules
	 * used few or none times. If all rules have been used a reasonable number
	 * of times, then the distribution is identical.
	 * 
	 * @param influenceThreshold
	 *            The coefficient for determining the influence threshold.
	 * @param influenceBoost
	 *            The boost coefficient influenced rules receive.
	 * @return A probability distribution, differing from the normal
	 *         distribution only if some rules are under the influence
	 *         threshold.
	 */
	public ProbabilityDistribution<GuidedRule> getInfluencedDistribution(
			double influenceThreshold, double influenceBoost) {
		ProbabilityDistribution<GuidedRule> influencedDistribution = ruleGenerator_
				.clone();

		int threshold = (int) (influenceThreshold * influencedDistribution
				.size());
		// Run through the elements, modifying probabilities if necessary
		for (int i = 0; i < influencedDistribution.size(); i++) {
			int uses = influencedDistribution.getElement(i).getUses();
			if (uses < threshold) {
				double newProb = influencedDistribution.getProb(i)
						* (threshold + 1 - uses) / influenceThreshold
						* influenceBoost;
				influencedDistribution.set(i, newProb);
			}
		}

		influencedDistribution.normaliseProbs();
		return influencedDistribution;
	}

	/**
	 * Gets the action this slot covers.
	 * 
	 * @return The action the slot covers.
	 */
	public String getAction() {
		return action_;
	}

	public double getSelectionProbability() {
		return selectionProb_;
	}

	public double getSelectionSD() {
		return selectionVariance_;
	}

	public void setSelectionProb(double prob) {
		selectionProb_ = prob;
	}

	public void setSelectionSD(double sd) {
		selectionVariance_ = sd;
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

	/**
	 * Clones this slot, but does not clone the rules within it.
	 * 
	 * @return A cloned version of this slot. This clone clones the
	 *         distributions within, but no deeper.
	 */
	@Override
	public Slot clone() {
		Slot clone = new Slot(action_);
		clone.selectionProb_ = selectionProb_;
		clone.selectionVariance_ = selectionVariance_;
		clone.fixed_ = fixed_;
		clone.currentSelectionProb_ = currentSelectionProb_;
		clone.ruleGenerator_ = ruleGenerator_.clone();
		if (backupGenerator_ != null)
			clone.backupGenerator_ = backupGenerator_.clone();
		return clone;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Slot other = (Slot) obj;
		if (action_ == null) {
			if (other.action_ != null)
				return false;
		} else if (!action_.equals(other.action_))
			return false;
		if (fixed_ != other.fixed_)
			return false;
		if (ruleGenerator_ == null) {
			if (other.ruleGenerator_ != null)
				return false;
		} else if (!ruleGenerator_.equals(other.ruleGenerator_))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action_ == null) ? 0 : action_.hashCode());
		result = prime * result + (fixed_ ? 1231 : 1237);
		result = prime * result
				+ ((ruleGenerator_ == null) ? 0 : ruleGenerator_.hashCode());
		return result;
	}

	@Override
	public String toString() {
		String result = (fixed_) ? "FIXED " : "";
		return result + "Slot (" + action_.toString() + ") "
				+ ruleGenerator_.toString() + "," + selectionProb_ + ","
				+ selectionVariance_;
	}

	public boolean isEmpty() {
		return ruleGenerator_.isEmpty();
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
				buffer.append(ruleGenerator_.toString());
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
