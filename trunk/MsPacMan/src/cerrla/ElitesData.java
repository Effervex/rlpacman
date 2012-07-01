package cerrla;

import relationalFramework.RelationalRule;
import util.MultiMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math.stat.descriptive.moment.Mean;

/**
 * A class for containing all of the data gathered about the elite solutions,
 * such as counts, positions, etc.
 * 
 * @author Sam Sarjant
 */
public class ElitesData {
	/** The average positions for each slot within the elite policies. */
	private Map<Slot, SlotData> slotData_;

	/** The elite values. */
	private ArrayList<Double> elitesValues_;

	public ElitesData(int numElites) {
		slotData_ = new HashMap<Slot, SlotData>();
		elitesValues_ = new ArrayList<Double>(numElites);
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
	 * @param ruleSlot
	 * 
	 * @param rule
	 *            The rule for which he data is being recorded.
	 * @param weight
	 *            The weight to add to the rule [0-1].
	 */
	public void addRuleCount(Slot ruleSlot, RelationalRule rule, int weight) {
		Map<RelationalRule, Integer> ruleCounts = getSlotData(ruleSlot)
				.getRuleCounts();
		Integer oldWeight = ruleCounts.get(rule);
		if (oldWeight == null)
			oldWeight = 0;
		ruleCounts.put(rule, oldWeight + weight);
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
	 * @param slot
	 *            The slot for which the data is being recorded.
	 * @param mean
	 *            The mean of the slot usage.
	 */
	public void setUsageStats(Slot slot, double mean) {
		SlotData slotData = getSlotData(slot);
		slotData.setMean(mean);
	}

	public Double getSlotPosition(Slot slot) {
		if (slotData_.containsKey(slot))
			return slotData_.get(slot).getAverageOrdering();
		return null;
	}

	/**
	 * Gets the rule counts.
	 * 
	 * @param slot
	 *            The slot to get the rule counts for.
	 * @return A mapping of rules to their weighted counts.
	 */
	public Map<RelationalRule, Integer> getSlotRuleCounts(Slot slot) {
		SlotData slotData = slotData_.get(slot);
		if (slotData == null)
			return null;
		return slotData.getRuleCounts();
	}

	/**
	 * Gets the count for a slot.
	 * 
	 * @param slot
	 *            The slot being searched for.
	 * @return The counts it has, or 0.
	 */
	public double getSlotCount(Slot slot) {
		if (slotData_.containsKey(slot))
			return slotData_.get(slot).getCount();
		return 0;
	}

	/**
	 * Gets the slot numeracy value.
	 * 
	 * @param slot
	 *            The slot to get the data for.
	 * @return The numeracy value, or 0 if not recorded.
	 */
	public double getSlotNumeracyMean(Slot slot) {
		if (slotData_.containsKey(slot))
			return slotData_.get(slot).getNumeracy();
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
			slotData_.put(slot, new SlotData(slot.size()));
		return slotData_.get(slot);
	}

	public Double getMaxEliteValue() {
		if (elitesValues_.isEmpty())
			return null;
		return elitesValues_.get(0);
	}

	public Double getMeanEliteValue() {
		if (elitesValues_.isEmpty())
			return null;
		double[] values = new double[elitesValues_.size()];
		int i = 0;
		for (Double val : elitesValues_)
			values[i++] = val;
		Mean m = new Mean();
		return m.evaluate(values);
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("Slot counts: \n");
		MultiMap<Integer, Slot> orderedMap = MultiMap.createSortedSetMultiMap();
		SortedSet<Integer> keys = new TreeSet<Integer>();
		for (Slot slot : slotData_.keySet()) {
			int count = (int) slotData_.get(slot).getCount();
			orderedMap.put(count, slot);
			keys.add(count);
		}
		for (Integer count : keys) {
			for (Slot slot : orderedMap.get(count)) {
				SlotData slotData = slotData_.get(slot);
				if (slot.getSlotSplitFacts().isEmpty())
					buffer.append("\tSlot " + slot.getAction() + ":\n"
							+ slotData + "\n");
				else
					buffer.append("\tSlot " + slot.getSlotSplitFacts() + " => "
							+ slot.getAction() + ":\n" + slotData_.get(slot)
							+ "\n");
			}
		}



		return buffer.toString();
	}

	/**
	 * A class for holding slot data.
	 * 
	 * @author Sam Sarjant
	 */
	private class SlotData {
		/** The number of times this slot is present in the elite policies. */
		private double count_;

		/** The raw count of the number of slots in the elite policies. */
		private int rawCount_;

		/** The average position of the slot in the policies. */
		private Double position_ = null;

		/** The average number of slots per policy. */
		private double mean_;

		/** The rule counts for this slot. */
		private Map<RelationalRule, Integer> ruleCounts_;

		public SlotData(int numRules) {
			ruleCounts_ = new HashMap<RelationalRule, Integer>(numRules);
		}

		public void addCount(double weight) {
			count_ += weight;
		}

		public Map<RelationalRule, Integer> getRuleCounts() {
			return ruleCounts_;
		}

		public void setMean(double mean) {
			mean_ = mean;
		}

		public double getCount() {
			return count_;
		}

		public void addOrdering(double relValue) {
			if (position_ == null)
				position_ = 0d;
			position_ += relValue;
		}

		public void incrementRawCount() {
			rawCount_++;
		}

		public double getAverageOrdering() {
			if (position_ == null)
				return 0.5d;
			return position_ / rawCount_;
		}

		public double getNumeracy() {
			return mean_;
		}

		@Override
		public String toString() {
			StringBuffer buffer = new StringBuffer("\tCount: " + count_);
			buffer.append("\tRaw Count: " + rawCount_);
			buffer.append("\tPosition: " + getAverageOrdering());
			buffer.append("\tNumeracy: " + mean_);

			// Rule counts for the slot.
			buffer.append("\n\tRule counts: \n");
			MultiMap<Integer, RelationalRule> orderedMap = MultiMap
					.createSortedSetMultiMap();
			SortedSet<Integer> keys = new TreeSet<Integer>();
			for (RelationalRule rule : ruleCounts_.keySet()) {
				orderedMap.put(ruleCounts_.get(rule), rule);
				keys.add(ruleCounts_.get(rule));
			}
			for (Integer count : keys) {
				for (RelationalRule rule : orderedMap.get(count))
					buffer.append("\t\t" + rule + ": " + count + "\n");
			}
			return buffer.toString();
		}
	}

	public void noteSampleValue(double value) {
		elitesValues_.add(value);
	}

	/**
	 * If all elite values are the same value, then set all probabilities to
	 * one.
	 */
	public void setEqualValues() {
		for (SlotData sd : slotData_.values()) {
			if (sd.mean_ > 0) {
				sd.mean_ = 1;
				sd.count_ = 1;
			}
		}
	}
}
