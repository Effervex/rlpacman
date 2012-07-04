package cerrla;

import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import rrlFramework.RRLExperiment;

import java.io.Serializable;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import util.ProbabilityDistribution;

/**
 * An inner class forming the slot of the policy generator. Contains the rules
 * within and any other pertinent information.
 * 
 * @author Samuel J. Sarjant
 */
public class Slot implements Serializable, Comparable<Slot> {
	private static final long serialVersionUID = -8194795456340834012L;

	/** The action which all rules within lead to. */
	private String action_;

	/** The backup generator for the slot. */
	private ProbabilityDistribution<RelationalRule> backupGenerator_;

	/** If this slot is fixed. */
	private boolean fixed_ = false;

	/** The fixed rule if this slot is fixed. */
	private RelationalRule fixedRule_;

	/** The number of updates this slot has performed. */
	private int numSamples_;

	/** The ordering of the slot (0..1) */
	private double ordering_;

	/** The distribution containing this slot. */
	private PolicyGenerator parentDistribution_;

	/** The rule generator within the slot. */
	private ProbabilityDistribution<RelationalRule> ruleGenerator_;

	/** The seed (first) rule for the slot. */
	private RelationalRule seedRule_;

	/** The hashcode for the slot. */
	private int slotHash_;

	/** The slot level (number of splits frm RLGG slot). */
	private int slotLevel_;

	/** The chance of this slot being selected. */
	private double slotMean_;

	/** The amount of updating this slot is receiving. */
	private double updateDelta_ = Integer.MAX_VALUE;

	private int convergedCount_;

	/**
	 * A constructor for a new slot. The slot may be fixed (only one rule
	 * allowed) and is initialised with a level (distance of splits from RLGG
	 * slot).
	 * 
	 * @param seedRule
	 *            The rule to initialise the slot with.
	 * @param fixed
	 *            If this slot is fixed.
	 * @param level
	 *            The number of splits this slot is from RLGG slot.
	 */
	public Slot(RelationalRule seedRule, boolean fixed, int level,
			PolicyGenerator parentDistribution) {
		action_ = seedRule.getActionPredicate();
		fixed_ = fixed;
		seedRule.setSlot(this);
		seedRule_ = seedRule;
		if (fixed) {
			slotMean_ = 1;
			fixedRule_ = seedRule;
		} else {
			slotMean_ = ProgramArgument.INITIAL_SLOT_MEAN.doubleValue();
			ruleGenerator_ = new ProbabilityDistribution<RelationalRule>(
					RRLExperiment.random_);
			ruleGenerator_.add(seedRule);
			slotLevel_ = level;
		}
		convergedCount_ = 0;
		ordering_ = 0.5;
		parentDistribution_ = parentDistribution;
		calculateHash();
	}

	/**
	 * Calculates the has for this slot, which remains the same on
	 * initialisation.
	 */
	private void calculateHash() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action_ == null) ? 0 : action_.hashCode());
		result = prime * result + (fixed_ ? 1231 : 1237);
		result = prime * result
				+ ((fixedRule_ == null) ? 0 : fixedRule_.hashCode());
		result = prime * result
				+ ((seedRule_ == null) ? 0 : seedRule_.hashCode());
		slotHash_ = result;
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
		}
		guidedRule.setSlot(this);
	}

	@Override
	public int compareTo(Slot other) {
		int result = action_.compareTo(other.action_);
		if (result != 0)
			return result;

		if (seedRule_ == null) {
			if (other.seedRule_ != null)
				return -1;
		} else {
			if (other.seedRule_ == null)
				return 1;
			else
				result = Double.compare(seedRule_.hashCode(),
						other.seedRule_.hashCode());
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
	 * Gets the local alpha for this slot. If the slot is not ready for updates
	 * yet, return 0.
	 * 
	 * @param alpha
	 *            The standard alpha value.
	 * @param population
	 *            The size of the population.
	 * @param numElites
	 *            The minimum number of elite samples.
	 * @param totalPoliciesEvaluated
	 *            The number of policies updated.
	 * @return The local alpha value or 0 if the slot is not ready for updates.
	 */
	public double determineLocalAlpha(double alpha, int population,
			int numElites, int totalPoliciesEvaluated) {
		// If using a local alpha and using dynamic slots, use the number of
		// updates this slot has been present in.
		if (ProgramArgument.LOCAL_ALPHA.booleanValue()
				&& ProgramArgument.DYNAMIC_SLOTS.booleanValue())
			totalPoliciesEvaluated = numSamples_;

		// If the slot has not seen enough samples to update, return 0.
		int evaluationThreshold = determineEvaluationThreshold();
		if (totalPoliciesEvaluated < evaluationThreshold)
			return 0;

		int policiesEvaluated = (ProgramArgument.LOCAL_ALPHA.booleanValue()) ? numSamples_
				: totalPoliciesEvaluated;
		return alpha / Math.max(population - policiesEvaluated, numElites);
	}

	/**
	 * Determines the number of evaluations this slot must undergo before it is
	 * allowed to update.
	 * 
	 * @return The minimum number of slot evaluations required.
	 */
	private int determineEvaluationThreshold() {
		return (int) (size() * ProgramArgument.CONFIDENCE_INTERVAL
				.doubleValue());
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
		if (seedRule_ == null) {
			if (other.seedRule_ != null)
				return false;
		} else if (!seedRule_.equals(other.seedRule_))
			return false;
		if (fixed_ != other.fixed_)
			return false;
		// Special case for fixed slots
		if (fixed_ && !fixedRule_.equals(other.fixedRule_))
			return false;
		return true;
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
	 * Gets the action this slot covers.
	 * 
	 * @return The action the slot covers.
	 */
	public String getAction() {
		return action_;
	}

	public RelationalRule getFixedRule() {
		return fixedRule_;
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

	public int getLevel() {
		return slotLevel_;
	}

	public double getOrdering() {
		return ordering_;
	}

	public double getOrderingSD() {
		double slotFillLevel = 1;
		if (slotMean_ < .5)
			slotFillLevel = 2 * slotMean_;
		else
			slotFillLevel = 2 * (1 - slotMean_);
		// slotFillLevel = (1.0 * klSize() - 1) / size();
		// slotFillLevel = Math.max(slotFillLevel, 0);
		// slotFillLevel = Math.min(slotFillLevel, 1);
		return ProgramArgument.INITIAL_ORDERING_SD.doubleValue()
				* slotFillLevel;
	}

	public RelationalRule getSeedRule() {
		return seedRule_;
	}

	public double getSelectionProbability() {
		return slotMean_;
	}

	/**
	 * Calculates what facts from the seed rule differ from the RLGG rule.
	 * 
	 * @return A collection of facts that differ (if any).
	 */
	public Collection<RelationalPredicate> getSlotSplitFacts() {
		SortedSet<RelationalPredicate> splitFacts = new TreeSet<RelationalPredicate>(
				seedRule_.getSimplifiedConditions(true));
		RelationalRule rlggRule = parentDistribution_.getRLGGRules().get(
				action_);
		if (rlggRule != null) {
			splitFacts.removeAll(rlggRule.getSimplifiedConditions(true));
		}
		return splitFacts;
	}

	@Override
	public int hashCode() {
		return slotHash_;
	}

	/**
	 * Checks if this slot is converged. Also applies slot fixing if that option
	 * is enabled.
	 * 
	 * @return True if this slot's mean has converged to 0 or 1, +- epsilon.
	 */
	public boolean isConverged() {
		// If KL size is 1, then the slot can be fixed.
		if (ProgramArgument.SLOT_FIXING.booleanValue()) {
			if (!fixed_ && klSize() == 1
					&& slotMean_ >= 1 - ProgramArgument.BETA.doubleValue()) {
				fixed_ = true;
				fixedRule_ = ruleGenerator_.getBestElement();
			}
		}
		// If slot is fixed, return true
		if (fixed_)
			return true;
		// If slot has mean ~0, return true;
		if (slotMean_ <= ProgramArgument.BETA.doubleValue())
			return true;
		// If the update delta is low enough (and stays low), return true.
		if (updateDelta_ < ProgramArgument.BETA.doubleValue()) {
			convergedCount_++;
			if (convergedCount_ >= ProgramArgument.NUM_UPDATES_CONVERGED
					.intValue())
				return true;
		} else
			convergedCount_ = 0;
		// if (klSize() == 1)
		// return true;
		return false;
	}

	public boolean isEmpty() {
		if (fixed_)
			return false;
		return ruleGenerator_.isEmpty();
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
	 * Checks if this slot is splittable based on its current KL size, the slot
	 * level and the splitting threshold.
	 * 
	 * @return True if the KL size is low enough.
	 */
	public boolean isSplittable() {
		if (fixed_)
			return false;
		if (ProgramArgument.ONLY_SPLIT_PROBABLE.booleanValue()
				&& slotMean_ < .5)
			return false;

		// double threshold = Math.min(slotMean_,
		// ProgramArgument.SLOT_THRESHOLD.doubleValue());
		double threshold = Math.min(1.0 / slotLevel_, slotMean_);
		if (threshold == -1)
			threshold = 1 - 1.0 / size();
		threshold = Math.max(threshold * size(), 1);
		// double threshold = ProgramArgument.SLOT_THRESHOLD.doubleValue();
		// if (threshold == -1)
		// threshold = 1 - 1.0 / size();
		// threshold = Math.max(Math.pow(threshold, slotLevel_ + 1) * size(),
		// 1);

		if (klSize() <= threshold)
			return true;
		return false;
	}

	public double klSize() {
		if (fixed_)
			return 1;
		return ruleGenerator_.klSize();
	}

	public void resetPolicyCount() {
		numSamples_ = 0;
	}

	public void incrementSamples() {
		numSamples_++;
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
	public RelationalRule sample(double influenceThreshold,
			double influenceBoost) {
		if (fixed_)
			return fixedRule_;
		return getInfluencedDistribution(influenceThreshold, influenceBoost)
				.sample(false);
	}

	public void setOrdering(double ordering) {
		ordering_ = ordering;
	}

	public void setSelectionProb(double prob) {
		slotMean_ = prob;
	}

	public int size() {
		if (fixed_)
			return 1;
		return ruleGenerator_.size();
	}

	/**
	 * Outputs this slot split facts as a string.
	 * 
	 * @return The string form of the slot split facts.
	 */
	public String slotSplitToString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Slot (");
		Collection<RelationalPredicate> splitFacts = getSlotSplitFacts();
		if (!splitFacts.isEmpty()) {
			boolean first = true;
			for (RelationalPredicate fact : splitFacts) {
				if (!first)
					buffer.append(" ");
				buffer.append(fact);
				first = false;
			}
			buffer.append(" -> ");
		}
		buffer.append(action_.toString() + ")");
		return buffer.toString();
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer((fixed_) ? "FIXED " : "");
		buffer.append(slotSplitToString());
		buffer.append(" [MU:" + slotMean_ + ";ORD:" + ordering_ + ";KL_SIZE:"
				+ klSize() + ";SIZE:" + size() + ";LVL:" + slotLevel_
				+ ";#UPDATES:" + numSamples_ + "]");
		if (fixed_)
			buffer.append(" " + fixedRule_.toString());
		else
			buffer.append(" " + ruleGenerator_.toString());
		return buffer.toString();
	}

	public void updateLevel(int slotLevel) {
		if (slotLevel < slotLevel_)
			slotLevel_ = slotLevel;
	}

	/**
	 * Updates the selection values of this slot stepwise towards the given
	 * values.
	 * 
	 * @param ed
	 *            The elites data to perform the update with.
	 * @param alpha
	 *            The amount the value should update by.
	 * @param population
	 *            The current population size.
	 * @param minimumElites
	 *            The minimal number of elite samples.
	 * @return The amount of change 0..1 or Integer.MAX_VALUE if no update.
	 */
	public double updateProbabilities(ElitesData ed, double alpha,
			int population, int minimumElites) {
		// if (updateDelta_ == Integer.MAX_VALUE)
		// alpha *= numSamples_;
		updateDelta_ = Integer.MAX_VALUE;
		if (ed == null || alpha == 0)
			return updateDelta_;

		// double factor = 1;
		// if (numUpdates_ < population) {
		// factor = 1.0 * numUpdates_ / population;
		// alpha = alpha * factor;
		// }
		//
		// if (alpha == 0)
		// return updateDelta_;

		// Update the slot values
		// double slotMean = Math.min(ed.getSlotCount(this) / minimumElites, 1);
		double actualSlotMean = ed.getSlotNumeracyMean(this);
		updateDelta_ = updateSlotValues(ed.getSlotPosition(this),
				actualSlotMean, alpha);

		// If not fixed, update the rule values.
		if (!fixed_) {
			updateDelta_ = slotMean_
					* ruleGenerator_.updateDistribution(ed.getSlotCount(this),
							ed.getSlotRuleCounts(this), alpha);
		}

		// return updateDelta_ / factor;
		// updateDelta_ /= 2;
		return updateDelta_;
	}

	/**
	 * Updates the selection values of this slot stepwise towards the given
	 * values.
	 * 
	 * @param ordering
	 *            The (possibly null) average ordering of the slot (0..1)
	 * @param mean
	 *            The mean value to step towards.
	 * @param alpha
	 *            The amount the value should update by.
	 * @return The absolute difference of the update values, normalised between
	 *         0..2.
	 */
	public double updateSlotValues(Double ordering, double mean, double alpha) {
		double absDiff = 0;
		if (ordering != null) {
			// double diff = ordering_;
			ordering_ = ordering * alpha + (1 - alpha) * ordering_;
			// diff -= ordering_;
			// absDiff += Math.abs(diff) / alpha;
		}
		double diff = slotMean_;
		// if (mean > 0)
		// slotMean_ = alpha * mean + (1 - alpha * mean) * slotMean_;
		// else
		// slotMean_ = (1 - alpha) * slotMean_;
		slotMean_ = alpha * mean + (1 - alpha) * slotMean_;
		diff -= slotMean_;
		absDiff += Math.abs(diff) / alpha;
		return absDiff;
	}

	/**
	 * If this slot is ready to update.
	 * 
	 * @return True if it is ready to update yet.
	 */
	public boolean isUpdating() {
		int threshold = determineEvaluationThreshold();
		if (ProgramArgument.ONLINE_UPDATES.booleanValue()) {
			if (numSamples_ % threshold == 0 && numSamples_ > 0) {
				numSamples_ = 0;
				return true;
			} else
				return false;
		}
		return numSamples_ >= threshold;
	}

	/**
	 * Gets the amount of change the last update created.
	 * 
	 * @return The update difference (delta).
	 */
	public double getUpdateDelta() {
		if (updateDelta_ == Integer.MAX_VALUE)
			return 1;
		return updateDelta_;
	}
}
