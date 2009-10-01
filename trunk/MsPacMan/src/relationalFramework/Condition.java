package relationalFramework;

import java.util.ArrayList;

import org.mandarax.kernel.Constructor;

/**
 * A high level abstract class for the basics of a rule condition. A rule
 * condition will have at least a condition value.
 * 
 * @author Samuel J. Sarjant
 */
public abstract class Condition {
	/** The separator for the parseable format of the condition. */
	public static final String PRE_SEPARATOR = ":";

	/** This class's condition. */
	private ConditionObject condition_;

	/**
	 * A constructor for a Condition.
	 * 
	 * @param condition
	 *            The condition, as an enum that implements ConditionObject.
	 */
	public Condition(ConditionObject condition) {
		condition_ = condition;
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
	 */
	public void initialise(ConditionObject condition) {
		condition_ = condition;
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
		return condition_.ordinal() + "";
	}

	@Override
	public abstract String toString();

	@Override
	public boolean equals(Object obj) {
		if ((obj != null) && (obj instanceof Condition)) {
			Condition cond = (Condition) obj;
			if (condition_ == null) {
				if (cond.condition_ == null)
					return true;
				else
					return false;
			}

			if (condition_.equals(cond.condition_)) {
				return true;
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
	 * @return The end index after parsing.
	 */
	public static int parseCondition(String[] conditionSplit, int index,
			ArrayList<Integer> conditions) {
		conditions.add(Integer.parseInt(conditionSplit[index]));
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
		
		/**
		 * Gets the predicate or function of the ConditionObject.
		 * 
		 * @return The predicate or function.
		 */
		public Constructor getConstructor();
	}
}
