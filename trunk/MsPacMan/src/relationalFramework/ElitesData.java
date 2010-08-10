package relationalFramework;

import java.util.HashMap;
import java.util.Map;

/**
 * A class for containing all of the data gathered about the elite solutions,
 * such as counts, positions, etc.
 * 
 * @author Sam Sarjant
 */
public class ElitesData {
	/** The average positions for each slot within the elite policies. */
	private Map<Slot, SlotData> slotData_;

	/** The number of times each rule was used within the elite policies. */
	private Map<GuidedRule, Double> ruleCounts_;

	public ElitesData() {
		slotData_ = new HashMap<Slot, SlotData>();
		ruleCounts_ = new HashMap<GuidedRule, Double>();
	}

	/**
	 * Adds the weighted count of a slot in and increments the raw count.
	 * 
	 * @param slot
	 *            The slot for which the data is being recorded.
	 * @param weight
	 *            The weight to add to the slot [0-1].
	 */
	public void addSlotCount(Slot slot, double weight) {
		SlotData sd = getSlotData(slot);

		sd.addCount(weight);
		sd.incrementRawCount();
	}

	/**
	 * Adds the weighted count of a rule in.
	 * 
	 * @param rule
	 *            The rule for which he data is being recorded.
	 * @param weight
	 *            The weight to add to the rule [0-1].
	 */
	public void addRuleCount(GuidedRule rule, double weight) {
		Double oldWeight = ruleCounts_.get(rule);
		if (oldWeight == null)
			oldWeight = 0d;
		ruleCounts_.put(rule, oldWeight + weight);
	}

	/**
	 * Adds a relative ordering value to the slot ordering.
	 * 
	 * @param slot
	 *            The slot for which the data is being recorded.
	 * @param relValue
	 *            The relative ordering value of the slot [0-1].
	 */
	public void addSlotOrdering(Slot slot, double relValue) {
		SlotData sd = getSlotData(slot);

		sd.addOrdering(relValue);
	}

	/**
	 * Sets the slot numeracy mean of the slot to a value.
	 * 
	 * @param slot The slot for which the data is being recorded.
	 * @param mean The mean of the slot usage.
	 * @param sd The sd of the slot usage.
	 */
	public void setUsageStats(Slot slot, double mean, double sd) {
		SlotData slotData = getSlotData(slot);
		slotData.setNumeracy(mean);
		slotData.setSD(sd);
	}

	/**
	 * Gets the slot positions.
	 * 
	 * @return A mapping of slots and their average relative positions.
	 */
	public Map<Slot, Double> getSlotPositions() {
		Map<Slot, Double> slotPositions = new HashMap<Slot, Double>();
		for (Slot slot : slotData_.keySet()) {
			slotPositions.put(slot, slotData_.get(slot).getAverageOrdering());
		}
		return slotPositions;
	}

	/**
	 * Gets the rule counts.
	 * 
	 * @return A mapping of rules to their weighted counts.
	 */
	public Map<GuidedRule, Double> getRuleCounts() {
		return ruleCounts_;
	}

	/**
	 * Gets the count for a slot.
	 * 
	 * @param slot The slot being searched for.
	 * @return The counts it has, or 0.
	 */
	public double getSlotCount(Slot slot) {
		double count = 0;
		if (slotData_.containsKey(slot))
			count = slotData_.get(slot).getCount();
		return count;
	}

	/**
	 * Gets the slot numeracy value.
	 * 
	 * @param slot The slot to get the data for.
	 * @return The numeracy value, or 0 if not recorded.
	 */
	public double getSlotNumeracyMean(Slot slot) {
		if (slotData_.containsKey(slot)) {
			return slotData_.get(slot).getNumeracy();
		}
		return 0;
	}
	
	/**
	 * Gets the slot sd value.
	 * 
	 * @param slot The slot to get the data for.
	 * @return The sd value, or 0 if not recorded.
	 */
	public double getSlotNumeracySD(Slot slot) {
		if (slotData_.containsKey(slot)) {
			return slotData_.get(slot).getSD();
		}
		return 0;
	}

	/**
	 * Gets or initialises the slot data.
	 * 
	 * @param slot
	 *            The slot to get the data for.
	 * @return The data for the slot, either new or existing.
	 */
	private SlotData getSlotData(Slot slot) {
		if (!slotData_.containsKey(slot))
			slotData_.put(slot, new SlotData());
		return slotData_.get(slot);
	}

	/**
	 * A class for holding slot data.
	 * 
	 * @author Sam Sarjant
	 */
	private class SlotData {
		/** The number of times this slot is present in the elite policies. */
		private double count_;

		/** The raw count of the numbe of slots in the elite policies. */
		private int rawCount_;

		/** The average position of the slot in the policies. */
		private double position_;

		/** The average number of slots per policy. */
		private double numeracy_;

		/**
		 * The amount of SD between the number of slots per policy in the
		 * elites.
		 */
		private double sd_;

		public void addCount(double weight) {
			count_ += weight;
		}

		public void setNumeracy(double mean) {
			numeracy_ = mean;
		}
		
		public void setSD(double sd) {
			sd_ = sd;
		}

		public double getCount() {
			return count_;
		}

		public void addOrdering(double relValue) {
			position_ += relValue;
		}

		public void incrementRawCount() {
			rawCount_++;
		}

		public double getAverageOrdering() {
			return position_ / rawCount_;
		}
		
		public double getNumeracy() {
			return numeracy_;
		}
		
		public double getSD() {
			return sd_;
		}
	}
}
