package relationalFramework;

import java.io.Serializable;

/**
 * A class for storing the items and associated probabilities.
 */
public class ItemProb<T> implements Comparable<ItemProb>, Serializable {
	private static final long serialVersionUID = -6821883735025271856L;
	/** The element stored. */
	private T element_;
	/** The probability, or weight. */
	private double prob_;

	/**
	 * A constructor for an item-probability pair.
	 * 
	 * @param element
	 *            The element.
	 * @param prob
	 *            The probability.
	 */
	public ItemProb(T element, double prob) {
		element_ = element;
		prob_ = prob;
	}

	/**
	 * Sets the probability of this ItemProb.
	 * 
	 * @param newProb
	 *            The new probability.
	 */
	public void setProbability(double newProb) {
		prob_ = newProb;
	}

	/**
	 * Gets the probability of this ItemProb.
	 * 
	 * @return The probability of the ItemProb.
	 */
	public double getProbability() {
		return prob_;
	}

	/**
	 * Gets the item of this ItemProb.
	 * 
	 * @return The item of the ItemProb.
	 */
	public T getItem() {
		return element_;
	}

	// @Override
	public int compareTo(ItemProb o) {
		if (prob_ > o.prob_) {
			return -1;
		} else if (prob_ < o.prob_) {
			return 1;
		} else {
			return Float.compare(element_.hashCode(), o.element_.hashCode());
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((element_ == null) ? 0 : element_.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ItemProb other = (ItemProb) obj;
		if (element_ == null) {
			if (other.element_ != null)
				return false;
		} else if (!element_.equals(other.element_))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("(" + element_ + ":" + prob_ + ")");
		return buffer.toString();
	}
}
