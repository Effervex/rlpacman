package relationalFramework;

import java.io.Serializable;

/**
 * A class defining the bounds of a variable numerical range.
 */
public class RangeBound implements Serializable, Comparable<RangeBound> {
	private static final long serialVersionUID = -7844419524661814286L;
	public static final String MIN = "min";
	public static final String MAX = "max";

	/** The value of the range bound. */
	private Object value_;

	/** The string of the bound. */
	private String boundString_;

	public RangeBound(String bound) {
		if (bound.endsWith(MIN))
			value_ = MIN;
		else if (bound.endsWith(MAX))
			value_ = MAX;
		else
			value_ = Double.parseDouble(bound);
		boundString_ = bound;
	}

	public RangeBound(double num) {
		value_ = num;
		boundString_ = num + "";
	}

	public RangeBound(String rangeVariable, String minOrMax) {
		if (minOrMax.equals(MIN))
			value_ = MIN;
		else if (minOrMax.equals(MAX))
			value_ = MAX;
		boundString_ = rangeVariable + minOrMax;
	}

	@Override
	public String toString() {
		return boundString_;
	}

	@Override
	public int compareTo(RangeBound o) {
		if (o == null)
			return -1;
		return boundString_.compareTo(o.boundString_);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value_ == null) ? 0 : value_.hashCode());
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
		RangeBound other = (RangeBound) obj;
		if (value_ == null) {
			if (other.value_ != null)
				return false;
		} else if (!value_.equals(other.value_))
			return false;
		return true;
	}
	
	public double getValue(double[] minMaxRange) {
		if (value_.equals(MIN))
			return minMaxRange[0];
		if (value_.equals(MAX))
			return minMaxRange[1];
		return (Double) value_;
	}
}
