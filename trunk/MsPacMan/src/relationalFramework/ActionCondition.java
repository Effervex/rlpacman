package relationalFramework;

/**
 * An abstract class for representing an action type to be used within rules.
 * 
 * @author Samuel J. Sarjant
 */
public abstract class ActionCondition extends Condition {
	/** The suffix to this class for use with dynamically loaded classes. */
	public static final String CLASS_SUFFIX = "Action";

	/**
	 * A constructor for an ActionCondition.
	 * 
	 * @param action
	 *            The action, as an enum that implements ConditionObject.
	 */
	public ActionCondition(ConditionObject action) {
		super(action);
	}

	/**
	 * A nullary constructor.
	 */
	public ActionCondition() {

	}

	@Override
	public boolean evaluateCondition(Object observations) {
		if (observations instanceof ActionSwitch) {
			ActionSwitch ac = (ActionSwitch) observations;
			return evaluateCondition(ac);
		}
		return false;
	}

	/**
	 * Evaluates this condition using the currently switched on actions.
	 * 
	 * @param ac
	 *            The ActionSwitch object.
	 * @return True if the condition is met, false otherwise.
	 */
	public boolean evaluateCondition(ActionSwitch ac) {
		// Action check
		if (ac.isActionActive(getCondition())) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return getCondition().toString();
	}

	/**
	 * Gets the observation values used in this experiment.
	 * 
	 * @param classPrefix
	 *            The class prefix used in the environment.
	 * @return The observation values used.
	 */
	public static ConditionObject[] getActionValues(String classPrefix) {
		ConditionObject[] values = null;
		try {
			values = ((ActionCondition) Class.forName(
					classPrefix + ActionCondition.CLASS_SUFFIX).newInstance())
					.getEnumValues();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return values;
	}

	/**
	 * Creates an action condition using an instantiatable class.
	 * 
	 * @param classPrefix
	 *            The class prefix used in the environment.
	 * @param condition
	 *            The action condition object.
	 * @return The newly created ObservationCondition instantiation.
	 */
	public static ActionCondition createAction(String classPrefix,
			ConditionObject condition) {
		ActionCondition action = null;
		try {
			action = (ActionCondition) Class.forName(
					classPrefix + ActionCondition.CLASS_SUFFIX).newInstance();
			action.initialise(condition);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return action;
	}
	
	/**
	 * A streamlined method for creating an ActionCondition using an index argument.
	 * 
	 * @param classPrefix
	 *            The class prefix used in the environment.
	 * @param condIndex
	 *            The action condition index.
	 * @return The newly created ObservationCondition instantiation.
	 */
	public static ActionCondition createAction(String classPrefix, int condIndex) {
		return createAction(classPrefix, getActionValues(classPrefix)[condIndex]);
	}

	/**
	 * The inner interface enumeration class of conditions.
	 * 
	 * @author Samuel J. Sarjant
	 */
	public interface ActionConditionObject extends ConditionObject {
		/** The suffix to this class for use with dynamically loaded classes. */
		public static final String CLASS_SUFFIX = "ActionSet";
	}
}