package relationalFramework;

/**
 * A class for storing the items and associated probabilities.
 */
public class ItemProb<T> implements Comparable<ItemProb> {
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
		if (prob_ < o.prob_) {
			return -1;
		} else if (prob_ > o.prob_) {
			return 1;
		} else {
			return Float.compare(element_.hashCode(), o.element_.hashCode());
		}
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj == null)
				|| (!(obj instanceof ItemProb)))
			return false;
		ItemProb ip = (ItemProb) obj;
		if (ip.element_.equals(element_)) {
			if (ip.prob_ == prob_) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (int) (element_.hashCode() * prob_);
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("(" + element_ + ": " + prob_ + ")");
		return buffer.toString();
	}
}
