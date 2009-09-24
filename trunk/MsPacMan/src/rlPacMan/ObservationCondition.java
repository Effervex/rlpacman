package rlPacMan;

import java.util.ArrayList;

/**
 * An abstract class for representing an observation type to be used within
 * rules. This condition incorporates checking against a double value.
 * 
 * @author Samuel J. Sarjant
 */
public abstract class ObservationCondition extends Condition {
	/** The suffix to this class for use with dynamically loaded classes. */
	public static final String CLASS_SUFFIX = "Observation";

	/** If the observation is greater than or equal to the value. */
	public static final boolean GREATER_EQ_THAN = true;

	/** If the observation is less than the value. */
	public static final boolean LESS_THAN = false;

	/** This class's comparable value. */
	private double conditionValue_;

	/**
	 * A constructor for a ObservationCondition.
	 * 
	 * @param valueCondition
	 *            The condition that has a value.
	 * @param valueOperator
	 *            The operator for comparing the value.
	 * @param value
	 *            The value to be compared against.
	 */
	public ObservationCondition(ValuedConditionObject valueCondition,
			boolean valueOperator, double value) {
		super(valueCondition, valueOperator);
		conditionValue_ = value;
	}

	/**
	 * A nullary constructor.
	 */
	public ObservationCondition() {

	}

	/**
	 * Initialises the arguments of this object.
	 * 
	 * @param valueCondition
	 *            The condition that has a value.
	 * @param valueOperator
	 *            The operator for comparing the value.
	 * @param value
	 *            The value to be compared against.
	 */
	public void initialise(ValuedConditionObject valueCondition,
			boolean valueOperator, double value) {
		initialise(valueCondition, valueOperator);
		conditionValue_ = value;
	}

	/**
	 * Gets the condition value of this condition.
	 * 
	 * @return The condition.
	 */
	public ValuedConditionObject getCondition() {
		return (ValuedConditionObject) super.getCondition();
	}

	/**
	 * Gets the associated comparative value.
	 * 
	 * @return The comparative value.
	 */
	public double getValue() {
		return conditionValue_;
	}

	@Override
	public boolean evaluateCondition(Object observations) {
		if (observations instanceof double[]) {
			double[] obsValues = (double[]) observations;
			return evaluateCondition(obsValues);
		}
		return false;
	}

	@Override
	public String toParseableString() {
		return this.getCondition().ordinal() + PRE_SEPARATOR + getOperator()
				+ PRE_SEPARATOR + getValue();
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer(getCondition() + " ");
		if (getOperator() == GREATER_EQ_THAN)
			buffer.append(">= ");
		else
			buffer.append("< ");
		buffer.append(conditionValue_);
		return buffer.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj != null) && (obj instanceof ObservationCondition)) {
			ObservationCondition obsCond = (ObservationCondition) obj;
			if (super.equals(obsCond)) {
				if (conditionValue_ == obsCond.conditionValue_) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Parses an observation condition from the array at the given index and
	 * puts the results into ArrayLists.
	 * 
	 * @param conditionSplit
	 *            The split String from which the condition is parsed from.
	 * @param index
	 *            The index to start at.
	 * @param observations
	 *            The observations ArrayList to add to.
	 * @param operators
	 *            The operators ArrayList to add to.
	 * @param values
	 *            The values ArrayList to add to.
	 * @return The end index after parsing.
	 */
	public static int parseObservationCondition(String[] conditionSplit,
			int index, ArrayList<Integer> observations,
			ArrayList<Boolean> operators, ArrayList<Double> values) {
		observations.add(Integer.parseInt(conditionSplit[index]));
		index++;
		operators.add(Boolean.parseBoolean(conditionSplit[index]));
		index++;
		values.add(Double.parseDouble(conditionSplit[index]));
		index++;
		return index;
	}

	/**
	 * Gets the observation values used in this experiment.
	 * 
	 * @param classPrefix
	 *            The class prefix used in the environment.
	 * @return The observation values used.
	 */
	public static ConditionObject[] getObservationValues(String classPrefix) {
		ConditionObject[] values = null;
		try {
			values = ((ObservationCondition) Class.forName(
					classPrefix + ObservationCondition.CLASS_SUFFIX)
					.newInstance()).getEnumValues();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return values;
	}

	/**
	 * Creates an observation condition using an instantiatable class.
	 * 
	 * @param classPrefix
	 *            The class prefix used in the environment.
	 * @param condition
	 *            The observation condition.
	 * @param operator
	 *            The operator used on the condition.
	 * @param value
	 *            The value to compare the operator.
	 * @return The newly created ObservationCondition instantiation.
	 */
	public static ObservationCondition createObservation(String classPrefix,
			ValuedConditionObject condition, boolean operator, double value) {
		ObservationCondition observation = null;
		try {
			observation = (ObservationCondition) Class.forName(
					classPrefix + ObservationCondition.CLASS_SUFFIX)
					.newInstance();
			observation.initialise(condition, operator, value);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return observation;
	}

	/**
	 * Evaluates this condition using the given observations.
	 * 
	 * @param observations
	 *            The current observation values, ordered by index.
	 * @return True if the condition is met, false otherwise.
	 */
	public boolean evaluateCondition(double[] obsValues) {
		// X >= Value
		if ((getOperator() == ObservationCondition.GREATER_EQ_THAN)
				&& (obsValues[this.getCondition().ordinal()] >= getValue())) {
			return true;
		} else if ((getOperator() == ObservationCondition.LESS_THAN)
				&& (obsValues[this.getCondition().ordinal()] < getValue())) {
			// X < Value
			return true;
		}
		return false;
	}

	/**
	 * The inner interface enumeration class of conditions.
	 * 
	 * @author Samuel J. Sarjant
	 */
	public interface ValuedConditionObject extends ConditionObject {
		/** The suffix to this class for use with dynamically loaded classes. */
		public static final String CLASS_SUFFIX = "ObservationSet";

		/**
		 * The ordinal of the condition. Required by enumerations.
		 * 
		 * @return The integer value of the ConditionSet.
		 */
		public int ordinal();

		/**
		 * Gets the set of values that this condition object can be compared
		 * against.
		 * 
		 * @return An array of values to compare the condition against.
		 */
		public double[] getSetOfVals();
	}
}
