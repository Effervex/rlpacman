package cerrla;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

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
	private int numUpdates_;

	/** The ordering of the slot (0..1) */
	private double ordering_;

	/** The rule generator within the slot. */
	private ProbabilityDistribution<RelationalRule> ruleGenerator_;

	/** The seed (first) rule for the slot. */
	private RelationalRule seedRule_;

	/** The chance of this slot being selected. */
	private double selectionProb_;

	/** The hashcode for the slot. */
	private int slotHash_;

	/** The slot level (number of splits frm RLGG slot). */
	private int slotLevel_;

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
	public Slot(RelationalRule seedRule, boolean fixed, int level) {
		action_ = seedRule.getActionPredicate();
		fixed_ = fixed;
		seedRule.setSlot(this);
		seedRule_ = seedRule;
		if (fixed) {
			selectionProb_ = 1;
			fixedRule_ = seedRule;
		} else {
			selectionProb_ = ProgramArgument.INITIAL_SLOT_MEAN.doubleValue();
			ruleGenerator_ = new ProbabilityDistribution<RelationalRule>(
					PolicyGenerator.random_);
			ruleGenerator_.add(seedRule);
			slotLevel_ = level;
		}
		ordering_ = 0.5;
		calculateHash();
	}

	/**
	 * Calculates the has for this slot, which remains the same on initialisation.
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
			guidedRule.setSlot(this);
		}
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

	/**
	 * Gets the local alpha for this slot.
	 * 
	 * @param population
	 *            The size of the population.
	 * @param numElites
	 *            The minimum number of elite samples.
	 * @param policiesEvaluated
	 *            The number of policies updated.
	 * @return
	 */
	public double getLocalAlpha(double alpha, int population, int numElites) {
		int policiesEvaluated = (ProgramArgument.LOCAL_ALPHA.booleanValue()) ? numUpdates_
				: PolicyGenerator.getInstance().getPoliciesEvaluated();
		return alpha / Math.max(population - policiesEvaluated, numElites);
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

	public double getOrdering() {
		return ordering_;
	}

	public double getOrderingSD() {
		int maxCapacity = getMaximumCapacity();
		double slotFillLevel = (maxCapacity > 1) ? (1.0 * klSize() - 1)
				/ (maxCapacity - 1) : 1;
		slotFillLevel = Math.max(slotFillLevel, 0);
		slotFillLevel = Math.min(slotFillLevel, 1);
		return ProgramArgument.INITIAL_ORDERING_SD.doubleValue()
				* slotFillLevel;
	}

	public RelationalRule getSeedRule() {
		return seedRule_;
	}

	public double getSelectionProbability() {
		return selectionProb_;
	}

	/**
	 * Calculates what facts from the seed rule differ from the RLGG rule.
	 * 
	 * @return A collection of facts that differ (if any).
	 */
	public Collection<RelationalPredicate> getSlotSplitFacts() {
		Collection<RelationalPredicate> splitFacts = new HashSet<RelationalPredicate>(
				seedRule_.getConditions(true));
		RelationalRule rlggRule = PolicyGenerator.getInstance().getRLGGRules()
				.get(action_);
		if (rlggRule != null)
			splitFacts.removeAll(rlggRule.getConditions(true));
		return splitFacts;
	}

	@Override
	public int hashCode() {
		return slotHash_;
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

		double threshold = ProgramArgument.SLOT_THRESHOLD.doubleValue();
		if (threshold == -1)
			threshold = 1 - 1.0 / size();
		if (klSize() < Math.pow(threshold, slotLevel_ + 1) * size())
			return true;
		return false;
	}

	public double klSize() {
		if (fixed_)
			return 1;
		return ruleGenerator_.klSize();
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
		selectionProb_ = prob;
	}

	public int size() {
		if (fixed_)
			return 1;
		return ruleGenerator_.size();
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer((fixed_) ? "FIXED " : "");
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
		buffer.append(" [MU:" + selectionProb_ + ";ORD:" + ordering_
				+ ";KL_SIZE:" + klSize() + ";SIZE:" + size() + ";LVL:"
				+ slotLevel_ + ";#UPDATES:" + numUpdates_ + "]");
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
	 * @param ordering
	 *            The (possibly null) average ordering of the slot (0..1)
	 * @param mean
	 *            The mean value to step towards.
	 * @param alpha
	 *            The amount the value should update by.
	 * @param population
	 *            The size of the population.
	 * @param numElites
	 *            The minimum number of elite samples.
	 * @param totalPoliciesEvaluated
	 *            The global number of policies evaluated.
	 * @return A normalised (to alpha) KL divergence of the generators.
	 */
	public double updateProbabilities(ElitesData ed, double alpha,
			int population, int numElites) {
		numUpdates_++;
		if (ed == null)
			return alpha;

		// If using local or global
		int policiesEvaluated = numUpdates_;
		// If not using a local alpha, or not using dynamic slots, use global
		// policies evaluated
		if (!ProgramArgument.LOCAL_ALPHA.booleanValue()
				|| !ProgramArgument.DYNAMIC_SLOTS.booleanValue())
			policiesEvaluated = PolicyGenerator.getInstance()
					.getPoliciesEvaluated();

		if (policiesEvaluated >= 2 * numElites) {
			double alphaPrime = getLocalAlpha(alpha, population, numElites);

			Double ordering = ed.getSlotPosition(this);
			if (ordering != null)
				ordering_ = ordering * alphaPrime + (1 - alphaPrime)
						* ordering_;
			selectionProb_ = ed.getSlotNumeracyMean(this) * alphaPrime
					+ (1 - alphaPrime) * selectionProb_;

			if (!fixed_) {
				double updateModifier = alpha / alphaPrime;
				return updateModifier
						* ruleGenerator_.updateDistribution(
								ed.getSlotCount(this), ed.getRuleCounts(),
								alphaPrime);
			}
		}
		return alpha;
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
	 */
	public void updateSlotValues(Double ordering, double mean, double alpha) {
		if (ordering != null)
			ordering_ = ordering * alpha + (1 - alpha) * ordering_;
		selectionProb_ = mean * alpha + (1 - alpha) * selectionProb_;
	}

	/**
	 * Checks if this slot should be used.
	 * 
	 * @param random
	 *            The random number generator.
	 * @return True if the slot should be used.
	 */
	public boolean useSlot(Random random) {
		return random.nextDouble() < selectionProb_;
	}

	public void resetPolicyCount() {
		numUpdates_ = 0;
	}
}
