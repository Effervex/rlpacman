package cerrla;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.agentObservations.AgentObservations;
import relationalFramework.util.ProbabilityDistribution;

/**
 * An inner class forming the slot of the policy generator. Contains the rules
 * within and any other pertinent information.
 * 
 * @author Samuel J. Sarjant
 */
public class Slot implements Serializable, Comparable<Slot> {
	private static final long serialVersionUID = -6398791301712325018L;

	private static final String ELEMENT_DELIMITER = ":";

	public static double INITIAL_SLOT_PROB = 1;

	/** The maximum amount of variance the selection probability can have. */
	public static final double MAX_SELECTION_VARIANCE = 0.5;

	/** String prefix for fixed. */
	private static final String FIXED = "FIXED";

	/** The initial ordering SD. */
	private static final double INITIAL_ORDERING_SD = 0.25;

	/** The rule generator within the slot. */
	private ProbabilityDistribution<RelationalRule> ruleGenerator_;

	/** The backup generator for the slot. */
	private ProbabilityDistribution<RelationalRule> backupGenerator_;

	/** The action which all rules within lead to. */
	private String action_;

	/** The facts used in this slot split, if it has any. */
	private Collection<RelationalPredicate> slotSplitFacts_;

	/** The seed (first) rule for the slot. */
	private RelationalRule seedRule_;

	/** If this slot is fixed. */
	private boolean fixed_ = false;

	/** The fixed rule if this slot is fixed. */
	private RelationalRule fixedRule_;

	/** The chance of this slot being selected. */
	private double selectionProb_;

	/** The variance on the chances of the slot being selected. */
	private double selectionSD_;

	/** The ordering of the slot (0..1) */
	private double ordering_;

	/**
	 * The constructor for a new Slot.
	 * 
	 * @param action
	 *            The action the slot rules lead to.
	 */
	public Slot(String action) {
		action_ = action;
		ruleGenerator_ = new ProbabilityDistribution<RelationalRule>(
				PolicyGenerator.random_);
		if (INITIAL_SLOT_PROB == -1)
			selectionProb_ = 1;
		else
			selectionProb_ = INITIAL_SLOT_PROB;
		selectionSD_ = 0.5;
		ordering_ = 0.5;
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
	public Slot(String action, Collection<RelationalPredicate> slotConditions) {
		this(action);
		slotSplitFacts_ = slotConditions;
	}

	/**
	 * A constructor for a new slot of only one fixed rule. The slot's rule will
	 * never change, but the slot parameters may.
	 * 
	 * @param fixedRule
	 *            The rule this slot is fixed on.
	 */
	public Slot(RelationalRule fixedRule) {
		action_ = fixedRule.getActionPredicate();
		selectionProb_ = 1;
		selectionSD_ = 0.5;
		ordering_ = 0.5;
		fixed_ = true;
		fixedRule_ = fixedRule;
	}

	/**
	 * Adds a new rule to the slot with an average probability of being
	 * selected.
	 * 
	 * @param guidedRule
	 *            The rule being added.
	 */
	public void addNewRule(RelationalRule guidedRule) {
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
	private void addNewRule(RelationalRule guidedRule, double prob) {
		ruleGenerator_.add(guidedRule, prob);
	}

	/**
	 * Checks if this slot contains a rule within it's generator.
	 * 
	 * @param gr
	 *            The rule being checked for.
	 * @return True if the rule is within the slot, false otherwise.
	 */
	public boolean contains(RelationalRule gr) {
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
	 *            The random number generator used for generating a Gaussian
	 *            value.
	 * @return True if the slot should be used, false otherwise.
	 */
	public int numSlotUses(Random random) {
		// Generate the probability of slot use
		double gauss = random.nextGaussian();
		double selectionProb = selectionProb_ + selectionSD_ * gauss;
		int numUses = 0;
		while (random.nextDouble() < selectionProb) {
			numUses++;
			selectionProb--;
		}
		return numUses;
	}

	/**
	 * Updates the selection values of this slot stepwise towards the given
	 * values.
	 * 
	 * @param ordering
	 *            The (possibly null) average ordering of the slot (0..1)
	 * @param mean
	 *            The mean value to step towards.
	 * @param sd
	 *            The variance to step towards.
	 * @param stepSize
	 *            The amount the value should update by.
	 */
	public void updateValues(Double ordering, double mean, double sd,
			double stepSize) {
		if (ordering != null)
			ordering_ = ordering * stepSize + (1 - stepSize) * ordering_;
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
	public ProbabilityDistribution<RelationalRule> getGenerator() {
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
	public ProbabilityDistribution<RelationalRule> getInfluencedDistribution(
			double influenceThreshold, double influenceBoost) {
		ProbabilityDistribution<RelationalRule> influencedDistribution = ruleGenerator_
				.clone();

		int threshold = (int) (influenceThreshold * influencedDistribution
				.size());
		// Run through the elements, modifying probabilities if necessary
		for (RelationalRule rule : influencedDistribution) {
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

	public double getOrdering() {
		return ordering_;
	}

	public void setOrdering(double ordering) {
		ordering_ = ordering;
	}

	public Collection<RelationalPredicate> getSlotSplitFacts() {
		return slotSplitFacts_;
	}

	public RelationalRule getSeedRule() {
		return seedRule_;
	}

	/**
	 * Samples a rule from the slot.
	 * 
	 * @param useMostLikely
	 *            If the most likely rule should be sampled.
	 * @return The sampled rule.
	 */
	public RelationalRule sample(boolean useMostLikely) {
		if (fixed_)
			return fixedRule_;
		return ruleGenerator_.sample(useMostLikely);
	}

	/**
	 * Samples a rule from the slot using an influenced distribution with the
	 * given parameters.
	 * 
	 * @param influenceThreshold
	 *            The influence threshold value.
	 * @param influenceBoost
	 *            The influence boost value.
	 * @return The sampled rule.
	 */
	public RelationalRule sample(double influenceThreshold, double influenceBoost) {
		if (fixed_)
			return fixedRule_;
		return getInfluencedDistribution(influenceThreshold, influenceBoost)
				.sample(false);
	}

	/**
	 * If this slot is fixed (not necessarily frozen).
	 * 
	 * @return If this slot is fixed.
	 */
	public boolean isFixed() {
		return fixed_;
	}

	public RelationalRule getFixedRule() {
		return fixedRule_;
	}

	public int size() {
		if (fixed_)
			return 1;
		return ruleGenerator_.size();
	}

	public double klSize() {
		if (fixed_)
			return 1;
		return ruleGenerator_.klSize();
	}

	/**
	 * Gets the slot's maximum capacity, based on the number of possible rule
	 * specialisations.
	 * 
	 * @return The maximum capacity. Note that the size CAN be ovr this, but no
	 *         mutations will be allowed.
	 */
	public int getMaximumCapacity() {
		return AgentObservations.getInstance().getNumSpecialisations(action_) + 1;
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
		if (!fixed_)
			clone.ruleGenerator_ = ruleGenerator_.clone();
		else
			clone.ruleGenerator_ = null;
		if (slotSplitFacts_ != null)
			clone.slotSplitFacts_ = new ArrayList<RelationalPredicate>(slotSplitFacts_);
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
		// Special case for fixed slots
		if (fixed_ && !fixedRule_.equals(other.fixedRule_))
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
				+ ((fixedRule_ == null) ? 0 : fixedRule_.hashCode());
		result = prime * result
				+ ((slotSplitFacts_ == null) ? 0 : slotSplitFacts_.hashCode());
		return result;
	}

	@Override
	public int compareTo(Slot other) {
		int result = action_.compareTo(other.action_);
		if (result != 0)
			return result;

		if (slotSplitFacts_ == null) {
			if (other.slotSplitFacts_ != null)
				return -1;
		} else {
			if (other.slotSplitFacts_ == null)
				return 1;
			else
				result = Double.compare(slotSplitFacts_.hashCode(),
						other.slotSplitFacts_.hashCode());
		}
		if (result != 0)
			return result;

		if (fixed_ && !other.fixed_)
			return -1;
		else if (!fixed_ && other.fixed_)
			return 1;
		else if (fixed_ && other.fixed_)
			return fixedRule_.compareTo(other.fixedRule_);

		return result;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer((fixed_) ? "FIXED " : "");
		buffer.append("Slot (");
		if (slotSplitFacts_ != null) {
			boolean first = true;
			for (RelationalPredicate fact : slotSplitFacts_) {
				if (!first)
					buffer.append(" ");
				buffer.append(fact);
				first = false;
			}
			buffer.append(" -> ");
		}
		buffer.append(action_.toString() + ")");
		buffer.append(" [MU:" + selectionProb_ + "\u00b1" + selectionSD_ + ";ORD:" + ordering_ + "]");
		if (fixed_)
			buffer.append(" " + fixedRule_.toString());
		else
			buffer.append(" " + ruleGenerator_.toString());
		return buffer.toString();
	}

	public boolean isEmpty() {
		if (fixed_)
			return false;
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
		int fixIndex = FIXED.length() + ELEMENT_DELIMITER.length();
		if (slotString.substring(0, fixIndex).equals(FIXED + ELEMENT_DELIMITER)) {
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
			RelationalRule guidedRule = new RelationalRule(m.group(1));
			guidedRule.setSlot(slot);
			double prob = Double.parseDouble(m.group(2));
			slot.addNewRule(guidedRule, prob);
		}

		return slot;
	}

	public double getOrderingSD() {
		int maxCapacity = getMaximumCapacity();
		double slotFillLevel = (maxCapacity > 1) ? (1.0 * klSize() - 1)
				/ (maxCapacity - 1) : 1;
		slotFillLevel = Math.max(slotFillLevel, 0);
		slotFillLevel = Math.min(slotFillLevel, 1);
		return INITIAL_ORDERING_SD * slotFillLevel;
	}
}
