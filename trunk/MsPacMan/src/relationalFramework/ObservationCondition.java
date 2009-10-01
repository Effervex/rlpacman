package relationalFramework;

import java.util.ArrayList;

/**
 * An abstract class for representing an observation type to be used within
 * rules. This condition incorporates checking less than or equal against a
 * double value.
 * 
 * @author Samuel J. Sarjant
 */
public abstract class ObservationCondition extends Condition {
	/** The suffix to this class for use with dynamically loaded classes. */
	public static final String CLASS_SUFFIX = "Observation";

	/** This class's comparable value. */
	private double conditionValue_;

	/**
	 * A constructor for a ObservationCondition.
	 * 
	 * @param valueCondition
	 *            The condition that has a value.
	 * @param value
	 *            The value to be compared against.
	 */
	public ObservationCondition(ValuedConditionObject valueCondition,
			double value) {
		super(valueCondition);
		conditionValue_ = value;
	}

	/**
	 * A nullary constructor.
	 */
	public ObservationCondition() {	}

	/**
	 * Initialises the arguments of this object.
	 * 
	 * @param valueCondition
	 *            The condition that has a value.
	 * @param value
	 *            The value to be compared against.
	 */
	public void initialise(ValuedConditionObject valueCondition,
			double value) {
		initialise(valueCondition);
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

	/**
	 * Evaluates this condition using the given observations.
	 * 
	 * @param observations
	 *            The current observation values, ordered by index.
	 * @return True if the condition is met, false otherwise.
	 */
	public boolean evaluateCondition(double[] obsValues) {
		// Empty condition
		if (this.getCondition() == null)
			return true;
		if (obsValues[this.getCondition().ordinal()] <= getValue()) {
			// X <= Value
			return true;
		}
		return false;
	}

	@Override
	public String toParseableString() {
		if (this.getCondition() == null)
			return "";
		return this.getCondition().ordinal()
				+ PRE_SEPARATOR + getValue();
	}

	@Override
	public String toString() {
		// Empty condition
		if (this.getCondition() == null)
			return "true";

		StringBuffer buffer = new StringBuffer(getCondition() + " ");
		buffer.append("<= ");
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
	 * @param values
	 *            The values ArrayList to add to.
	 * @return The end index after parsing.
	 */
	public static int parseObservationCondition(String[] conditionSplit,
			int index, ArrayList<Integer> observations, ArrayList<Double> values) {
		observations.add(Integer.parseInt(conditionSplit[index]));
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
	 * @param value
	 *            The value to compare the operator.
	 * @return The newly created ObservationCondition instantiation.
	 */
	public static ObservationCondition createObservation(String classPrefix,
			ValuedConditionObject condition, double value) {
		ObservationCondition observation = emptyObservation(classPrefix);
		observation.initialise(condition, value);
		return observation;
	}

	/**
	 * A streamlined method for creating an ObservationCondition using an index
	 * argument.
	 * 
	 * @param classPrefix
	 *            The class prefix used in the environment.
	 * @param condIndex
	 *            The observation condition index.
	 * @param value
	 *            The value to compare the operator.
	 * @return The newly created ObservationCondition instantiation.
	 */
	public static ObservationCondition createObservation(String classPrefix,
			int condIndex, double value) {
		return createObservation(
				classPrefix,
				(ValuedConditionObject) getObservationValues(classPrefix)[condIndex],
				value);
	}

	/**
	 * Creates an empty condition.
	 * 
	 * @param classPrefix
	 *            The class prefix used in the environment.
	 * @return The newly created empty ObservationCondition.
	 */
	public static ObservationCondition emptyObservation(String classPrefix) {
		try {
			return (ObservationCondition) Class.forName(
					classPrefix + ObservationCondition.CLASS_SUFFIX)
					.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
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
		 * Gets the set of values that this condition object can be compared
		 * against.
		 * 
		 * @return An array of values to compare the condition against.
		 */
		public double[] getSetOfVals();
	}
}
