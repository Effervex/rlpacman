package relationalFramework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An inner class forming the slot of the policy generator. Contains the rules
 * within and any other pertinent information.
 * 
 * @author Samuel J. Sarjant
 */
public class Slot implements Serializable {
	private static final long serialVersionUID = 1013881731353664622L;

	private static final String ELEMENT_DELIMITER = ":";

	public static double INITIAL_SLOT_PROB = 1;

	/** The maximum amount of variance the selection probability can have. */
	public static final double MAX_SELECTION_VARIANCE = 0.5;

	private static final String FIXED = "FIXED";

	/** The rule generator within the slot. */
	private ProbabilityDistribution<GuidedRule> ruleGenerator_;

	/** The backup generator for the slot. */
	private ProbabilityDistribution<GuidedRule> backupGenerator_;

	/** The action which all rules within lead to. */
	private String action_;

	/** The facts used in this slot split, if it has any. */
	private Collection<StringFact> slotSplitFacts_;

	/** The seed (first) rule for the slot. */
	private GuidedRule seedRule_;

	/** If this slot is fixed. */
	private boolean fixed_ = false;

	/** The chance of this slot being selected. */
	private double selectionProb_;

	/** The variance on the chances of the slot being selected. */
	private double selectionSD_;

	/** The number of times the slot should be used. */
	private int numSlotUses_;

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
		if (INITIAL_SLOT_PROB == -1)
			selectionProb_ = 1;
		else
			selectionProb_ = INITIAL_SLOT_PROB;
		selectionSD_ = 0.5;
		numSlotUses_ = 0;
	}

	/**
	 * A constructor for a specialised slot which only contains rules concerning
	 * the splitFacts facts.
	 * 
	 * @param action
	 *            The action the slot rules lead to.
	 * @param slotConditions
	 *            The facts present in all rules in this slot.
	 */
	public Slot(String action, Collection<StringFact> slotConditions) {
		this(action);
		slotSplitFacts_ = slotConditions;
	}

	/**
	 * Adds a new rule to the slot with an average probability of being
	 * selected.
	 * 
	 * @param guidedRule
	 *            The rule being added.
	 */
	public void addNewRule(GuidedRule guidedRule) {
		if (seedRule_ == null)
			seedRule_ = guidedRule;

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

			// ruleGenerator_ = ruleGenerator_.bindProbs(false);
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
	 * @return A boolean array of two elements: whether to use the slot and
	 *         whether to keep the slot. Note that !Use -> !Keep
	 */
	public boolean[] shouldUseSlot(Random random) {
		boolean[] useSlot = new boolean[2];
		if (numSlotUses_ <= 0) {
			// The slot has not been sampled yet and needs to set a selection
			// probability.
			double gauss = random.nextGaussian();
			double selectionProb = selectionProb_ + selectionSD_ * gauss;
			numSlotUses_ = 0;
			while (random.nextDouble() < selectionProb) {
				numSlotUses_++;
				selectionProb--;
			}

			// PoissonDistribution p = new
			// PoissonDistributionImpl(selectionProb_
			// * selectionModifier);
			// try {
			// double cumulativeProb = random.nextDouble();
			// numSlotUses_ = p.inverseCumulativeProbability(cumulativeProb) +
			// 1;
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
		}

		// Use the slot if available, and remove it if no more uses are left.
		if (numSlotUses_ > 0)
			useSlot[0] = true;
		numSlotUses_--;
		if (numSlotUses_ > 0)
			useSlot[1] = true;

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
	public void updateSelectionValues(double mean, double sd, double stepSize) {
		selectionProb_ = mean * stepSize + (1 - stepSize) * selectionProb_;
		selectionSD_ = sd * stepSize + (1 - stepSize) * selectionSD_;

		// Capping variance at max.
		if (selectionSD_ > MAX_SELECTION_VARIANCE)
			selectionSD_ = MAX_SELECTION_VARIANCE;
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
		for (GuidedRule rule : influencedDistribution) {
			int uses = rule.getUses();
			if (uses < threshold) {
				double newProb = influencedDistribution.getProb(rule)
						* (threshold + 1 - uses) / influenceThreshold
						* influenceBoost;
				influencedDistribution.set(rule, newProb);
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

	public void setSelectionProb(double prob) {
		selectionProb_ = prob;
	}

	public double getSelectionSD() {
		return selectionSD_;
	}

	public void setSelectionSD(double sd) {
		selectionSD_ = sd;
	}

	public Collection<StringFact> getSlotSplitFacts() {
		return slotSplitFacts_;
	}

	public GuidedRule getSeedRule() {
		return seedRule_;
	}

	public GuidedRule sample(boolean useMostLikely) {
		return ruleGenerator_.sample(useMostLikely);
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

	public int size() {
		return ruleGenerator_.size();
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
		clone.selectionSD_ = selectionSD_;
		clone.fixed_ = fixed_;
		clone.numSlotUses_ = numSlotUses_;
		clone.ruleGenerator_ = ruleGenerator_.clone();
		if (slotSplitFacts_ != null)
			clone.slotSplitFacts_ = new ArrayList<StringFact>(slotSplitFacts_);
		clone.seedRule_ = seedRule_;
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
		if (slotSplitFacts_ == null) {
			if (other.slotSplitFacts_ != null)
				return false;
		} else if (!slotSplitFacts_.equals(other.slotSplitFacts_))
			return false;
		if (fixed_ != other.fixed_)
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
				+ ((slotSplitFacts_ == null) ? 0 : slotSplitFacts_.hashCode());
		return result;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer((fixed_) ? "FIXED " : "");
		buffer.append("Slot (");
		if (slotSplitFacts_ != null) {
			boolean first = true;
			for (StringFact fact : slotSplitFacts_) {
				if (!first)
					buffer.append(" ");
				buffer.append(fact);
				first = false;
			}
			buffer.append(" -> ");
		}
		buffer.append(action_.toString() + ")");
		buffer.append(" " + ruleGenerator_.toString() + "," + selectionProb_
				+ "\u00b1" + selectionSD_);
		return buffer.toString();
	}

	public boolean isEmpty() {
		return ruleGenerator_.isEmpty();
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
		int fixIndex = FIXED.length() + ELEMENT_DELIMITER.length();
		if (slotString.substring(0, fixIndex).equals(FIXED + ELEMENT_DELIMITER)) {
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
			GuidedRule guidedRule = new GuidedRule(m.group(1));
			guidedRule.setSlot(slot);
			double prob = Double.parseDouble(m.group(2));
			slot.addNewRule(guidedRule, prob);
		}

		if (fixed)
			slot.fixSlot();

		return slot;
	}
}
