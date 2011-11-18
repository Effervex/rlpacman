package relationalFramework;

import java.util.Arrays;

/**
 * Relational arguments are what go within relational predicates. In most cases,
 * these are just simple strings, but when dealing with numbers, they can
 * express a range.
 * 
 * @author Sam Sarjant
 */
public class RelationalArgument {
	/** The range, if any. */
	private double[] rangeArg_;
	
	/** The argument represented by this arg. */
	private String stringArg_;
	
	/**
	 * The constructor. This deconstructs ranges into their components.
	 */
	public RelationalArgument(String arg) {
		
	}
	
	/**
	 * A constructor for a new range.
	 * 
	 * @param variable The variable of the range.
	 * @param min The minimum value of the range.
	 * @param max The maximum value of the range.
	 */
	public RelationalArgument(String variable, double min, double max) {
		stringArg_ = variable;
		rangeArg_ = new double[2];
		rangeArg_[0] = min;
		rangeArg_[1] = max;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RelationalArgument other = (RelationalArgument) obj;
		if (!Arrays.equals(rangeArg_, other.rangeArg_))
			return false;
		if (stringArg_ == null) {
			if (other.stringArg_ != null)
				return false;
		} else if (!stringArg_.equals(other.stringArg_))
			return false;
		return true;
	}

	public double[] getRangeArg() {
		return rangeArg_;
	}

	public String getStringArg() {
		return stringArg_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(rangeArg_);
		result = prime * result
				+ ((stringArg_ == null) ? 0 : stringArg_.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "RelationalArgument [stringArg_=" + stringArg_ + ", rangeArg_="
				+ Arrays.toString(rangeArg_) + "]";
	}
}
