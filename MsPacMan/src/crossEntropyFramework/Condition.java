package crossEntropyFramework;

import java.util.ArrayList;

/**
 * A high level abstract class for the basics of a rule condition. A rule
 * condition will have at least a condition value and an associated binary
 * check.
 * 
 * @author Samuel J. Sarjant
 */
public abstract class Condition {
	/** The separator for the parseable format of the condition. */
	public static final String PRE_SEPARATOR = ":";

	/** This class's condition. */
	private ConditionObject condition_;
	/** This class's operator. */
	private boolean operator_;

	/**
	 * A constructor for a Condition.
	 * 
	 * @param condition
	 *            The condition, as an enum that implements ConditionObject.
	 * @param operator
	 *            The boolean operator acting on the condition.
	 */
	public Condition(ConditionObject condition, boolean operator) {
		condition_ = condition;
		operator_ = operator;
	}
	
	/**
	 * A nullary constructor.
	 */
	public Condition() {
	}
	
	/**
	 * Initialises the arguments for this object.
	 * 
	 * @param condition
	 *            The condition, as an enum that implements ConditionObject.
	 * @param operator
	 *            The boolean operator acting on the condition.
	 */
	public void initialise(ConditionObject condition, boolean operator) {
		condition_ = condition;
		operator_ = operator;
	}

	/**
	 * Gets the condition.
	 * 
	 * @return The condition.
	 */
	public ConditionObject getCondition() {
		return condition_;
	}

	/**
	 * Gets the operator on this condition.
	 * 
	 * @return The condition operator.
	 */
	public boolean getOperator() {
		return operator_;
	}

	/**
	 * Evaluates this condition using the given observations.
	 * 
	 * @param observations
	 *            The observations currently active.
	 * @return True if the condition is met, false otherwise.
	 */
	public abstract boolean evaluateCondition(Object observations);

	/**
	 * Converts this Condition into a parseable String format.
	 * 
	 * @return A parseable format of this Condition.
	 */
	public String toParseableString() {
		return condition_.ordinal() + PRE_SEPARATOR + operator_;
	}

	@Override
	public abstract String toString();
	
	@Override
	public boolean equals(Object obj) {
		if ((obj != null) && (obj instanceof Condition)) {
			Condition cond = (Condition) obj;
			if (condition_.equals(cond.condition_)) {
				if (operator_ == cond.operator_) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Parses a condition from the array at the given index and puts the results
	 * into ArrayLists.
	 * 
	 * @param conditionSplit
	 *            The split String from which the condition is parsed from.
	 * @param index
	 *            The index to start at.
	 * @param actions
	 *            The actions ArrayList to add to.
	 * @param operators
	 *            The operators ArrayList to add to.
	 * @return The end index after parsing.
	 */
	public static int parseCondition(String[] conditionSplit, int index,
			ArrayList<Integer> conditions, ArrayList<Boolean> operators) {
		conditions.add(Integer.parseInt(conditionSplit[index]));
		index++;
		operators.add(Boolean.parseBoolean(conditionSplit[index]));
		index++;
		return index;
	}
	
	/**
	 * Gets the values of this enum.
	 * 
	 * @return The values of the enum.
	 */
	public abstract ConditionObject[] getEnumValues();

	/**
	 * The inner interface enumeration class of conditions.
	 * 
	 * @author Samuel J. Sarjant
	 */
	public interface ConditionObject {
		/**
		 * The ordinal of the condition. Required by enumerations.
		 * 
		 * @return The integer value of the ConditionSet.
		 */
		public int ordinal();
	}
}
