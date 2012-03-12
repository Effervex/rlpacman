package relationalFramework.agentObservations;

import java.io.Serializable;
import java.util.Arrays;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

/**
 * A class for defining where a range is and what context surrounds it so it can
 * be correctly identified for min-max range calculations.
 * 
 * @author Sam Sarjant
 * 
 */
public class RangeContext implements Serializable, Comparable<RangeContext> {
	private static final long serialVersionUID = 6893489654355672705L;

	/** The (possibly null) action the range rule outputs. */
	private String action_;

	/** The predicate this range is within. */
	private String predicate_;

	/** The number of args the predicate takes. */
	private int predicateArgLength_;

	/** The (possibly null) structure of the predicate within the rule. */
	private int[] predicateArgStructure_;

	/** The index (position) of the range within the predicate. */
	private int rangeIndex_;

	/** The variable of the range. */
	private String rangeVariable_;

	/**
	 * A constructor for a range, unbound by a rule.
	 * 
	 * @param rangeIndex
	 *            The index of the range within the predicate.
	 * @param predicate
	 *            The predicate name containing the range.
	 */
	public RangeContext(int rangeIndex, RelationalPredicate predicate) {
		rangeIndex_ = rangeIndex;
		predicate_ = predicate.getFactName();
		predicateArgLength_ = predicate.getArgTypes().length;
		rangeVariable_ = predicate.getRelationalArguments()[rangeIndex]
				.getStringArg();
	}

	/**
	 * A constructor for a range within a rule.
	 * 
	 * @param rangeIndex
	 *            The index of the range within the predicate.
	 * @param predicate
	 *            The predicate containing the range.
	 * @param action
	 *            The action the rule outputs.
	 */
	public RangeContext(int rangeIndex, RelationalPredicate predicate,
			RelationalPredicate action) {
		this(rangeIndex, predicate);
		action_ = action.getFactName();

		predicateArgStructure_ = new int[predicate.getArgTypes().length];
		RelationalArgument[] predArgs = predicate.getRelationalArguments();
		String[] predTypes = predicate.getArgTypes();
		RelationalArgument[] actionArgs = action.getRelationalArguments();
		for (int p = 0; p < predicateArgLength_; p++) {
			predicateArgStructure_[p] = -1;
			if (!StateSpec.isNumberType(predTypes[p])) {
				for (int a = 0; a < actionArgs.length; a++) {
					// If the predicate argument is contained in the action,
					// note down its action index
					if (predArgs[p].getStringArg().equals(
							actionArgs[a].getStringArg())) {
						predicateArgStructure_[p] = a;
						break;
					}
				}
			}
		}
	}

	/**
	 * A constructor for a range within a rule but without being given the
	 * action definition. The structure of the predicate is defined by variable
	 * patterns.
	 * 
	 * @param rangeIndex
	 *            The index of the rage within the predicate.
	 * @param predicate
	 *            The predicate containing the range.
	 * @param action
	 *            The name of the action the rule outputs.
	 */
	public RangeContext(int rangeIndex, RelationalPredicate predicate,
			String action) {
		this(rangeIndex, predicate);
		action_ = action;

		predicateArgStructure_ = new int[predicate.getArgTypes().length];
		RelationalArgument[] predArgs = predicate.getRelationalArguments();
		for (int p = 0; p < predicateArgLength_; p++) {
			predicateArgStructure_[p] = predArgs[p].getVariableTermIndex();
		}
	}

	@Override
	public int compareTo(RangeContext o) {
		int result = action_.compareTo(o.action_);
		if (result != 0)
			return result;
		result = predicate_.compareTo(o.predicate_);
		if (result != 0)
			return result;
		result = Double.compare(rangeIndex_, o.rangeIndex_);
		if (result != 0)
			return result;
		for (int i = 0; i < predicateArgLength_; i++) {
			result = Double.compare(predicateArgStructure_[i],
					o.predicateArgStructure_[i]);
			if (result != 0)
				return result;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RangeContext other = (RangeContext) obj;
		if (action_ == null) {
			if (other.action_ != null)
				return false;
		} else if (!action_.equals(other.action_))
			return false;
		if (!Arrays
				.equals(predicateArgStructure_, other.predicateArgStructure_))
			return false;
		if (predicate_ == null) {
			if (other.predicate_ != null)
				return false;
		} else if (!predicate_.equals(other.predicate_))
			return false;
		if (rangeIndex_ != other.rangeIndex_)
			return false;
		return true;
	}

	public String getAction() {
		return action_;
	}

	public String getRangeVariable() {
		return rangeVariable_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action_ == null) ? 0 : action_.hashCode());
		result = prime * result + Arrays.hashCode(predicateArgStructure_);
		result = prime * result
				+ ((predicate_ == null) ? 0 : predicate_.hashCode());
		result = prime * result + rangeIndex_;
		return result;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer("(" + predicate_);
		for (int i = 0; i < predicateArgLength_; i++) {
			// If index
			if (i == rangeIndex_) {
				buffer.append(" " + RelationalArgument.RANGE_VARIABLE_PREFIX
						+ "R");
			} else if (predicateArgStructure_ != null
					&& predicateArgStructure_[i] != -1) {
				// Action term
				buffer.append(" "
						+ RelationalArgument
								.createVariableTermArg(predicateArgStructure_[i]));
			} else {
				buffer.append(" ?");
			}
		}
		buffer.append(")");
		return buffer.toString();
	}

	/**
	 * Outputs the range context with the range included.
	 * 
	 * @param bounds
	 *            The value of the range.
	 * @return The range context String with range included.
	 */
	public String toString(double[] bounds) {
		StringBuffer buffer = new StringBuffer("(" + predicate_);
		for (int i = 0; i < predicateArgLength_; i++) {
			// If index
			if (i == rangeIndex_) {
				buffer.append(" (" + bounds[0] + " <= "
						+ RelationalArgument.RANGE_VARIABLE_PREFIX + "R <= "
						+ bounds[1] + ")");
			} else if (predicateArgStructure_ != null
					&& predicateArgStructure_[i] != -1) {
				// Action term
				buffer.append(" "
						+ RelationalArgument
								.createVariableTermArg(predicateArgStructure_[i]));
			} else {
				buffer.append(" ?");
			}
		}
		buffer.append(")");
		return buffer.toString();
	}
}
