package relationalFramework.agentObservations;

/**
 * A class to represent a range of values for a condition. Used for
 * recording the maximum values recorded by the covered rules.
 * 
 * @author Sam Sarjant
 */
public class RangedCondition {
	private String condition_;
	private double minimum_;
	private double maximum_;

	/**
	 * The ranged condition constructor.
	 * 
	 * @param condition
	 *            The condition.
	 * @param minimum
	 *            The minimum value.
	 * @param maximum
	 *            The maximum value.
	 */
	public RangedCondition(String condition, double minimum, double maximum) {
		condition_ = condition;
		minimum_ = minimum;
		maximum_ = maximum;
	}

	public String getCondition() {
		return condition_;
	}

	public double getMinimum() {
		return minimum_;
	}

	public double getMaximum() {
		return maximum_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((condition_ == null) ? 0 : condition_.hashCode());
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
		RangedCondition other = (RangedCondition) obj;
		if (condition_ == null) {
			if (other.condition_ != null)
				return false;
		} else if (!condition_.equals(other.condition_))
			return false;
		return true;
	}

	/**
	 * Checks if the ranges of two RangedConditions are equal.
	 * 
	 * @param coveredRange
	 *            The other ranged condition.
	 * @return True if the ranges are equal, false otherwise.
	 */
	public boolean equalRange(RangedCondition coveredRange) {
		if (coveredRange == null)
			return false;
		if ((coveredRange.minimum_ == minimum_)
				&& (coveredRange.maximum_ == maximum_))
			return true;
		return false;
	}

	@Override
	public String toString() {
		return condition_ + " {" + minimum_ + "-" + maximum_ + "}";
	}
}