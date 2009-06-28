package rlPacMan;

/**
 * This class represents a rule given in and 'if (condition) then (action)'. The
 * condition is given as an inequity/s from PacManObservations and the action is
 * given as a + or - from PacManHighActions.
 * 
 * @author Samuel J. Sarjant
 */
public class Rule {
	public static boolean GREATER_THAN = true; // X > 5
	public static boolean LESS_THAN = false; // X < 5

	private PacManObservations[] conditionObs_;
	private PacManHighAction conditionAc_;
	private boolean[] conditionOps_;
	private double[] conditionVals_;
	private PacManHighAction action_;
	private boolean actionOn_;

	/**
	 * A basic constructor for a single condition rule.
	 * 
	 * @param obs
	 *            The observation condition
	 * @param op
	 *            True if greater than (>), false if less than (<)
	 * @param value
	 *            The value for condition comparison.
	 * @param action
	 *            The action to trigger if condition met.
	 * @param switched
	 *            Whether to activate or deactivate the action.
	 */
	public Rule(PacManObservations obs, boolean op, double value,
			PacManHighAction action, boolean switched) {
		conditionObs_ = new PacManObservations[1];
		conditionObs_[0] = obs;

		conditionOps_ = new boolean[1];
		conditionOps_[0] = op;

		conditionVals_ = new double[1];
		conditionVals_[0] = value;

		action_ = action;
		actionOn_ = switched;
	}

	/**
	 * A constructor for a two condition rule made up of observations.
	 * 
	 * @param obs0
	 *            The first observation condition
	 * @param op0
	 *            True if greater than (>), false if less than (<)
	 * @param value0
	 *            The first value for condition comparison.
	 * @param obs1
	 *            The second observation condition
	 * @param op1
	 *            True if greater than (>), false if less than (<)
	 * @param value1
	 *            The second value for condition comparison.
	 * @param action
	 *            The action to trigger if condition met.
	 * @param switched
	 *            Whether to activate or deactivate the action.
	 */
	public Rule(PacManObservations obs0, boolean op0, double value0,
			PacManObservations obs1, boolean op1, double value1,
			PacManHighAction action, boolean switched) {
		conditionObs_ = new PacManObservations[2];
		conditionObs_[0] = obs0;
		conditionObs_[1] = obs1;

		conditionOps_ = new boolean[2];
		conditionOps_[0] = op0;
		conditionOps_[1] = op1;

		conditionVals_ = new double[2];
		conditionVals_[0] = value0;
		conditionVals_[1] = value1;

		action_ = action;
		actionOn_ = switched;
	}

	/**
	 * A constructor for a two condition rule made up of an observation and
	 * action check.
	 * 
	 * @param obs
	 *            The observation condition
	 * @param op
	 *            True if greater than (>), false if less than (<)
	 * @param value
	 *            The value for condition comparison.
	 * @param condAction
	 *            The action condition
	 * @param acSwitch
	 *            The switch value of the action condition.
	 * @param action
	 *            The action to trigger if condition met.
	 * @param switched
	 *            Whether to activate or deactivate the action.
	 */
	public Rule(PacManObservations obs, boolean op, double value,
			PacManHighAction condAction, boolean acSwitch,
			PacManHighAction action, boolean switched) {
		this(obs, op, value, action, switched);

		conditionAc_ = condAction;

		conditionOps_ = new boolean[2];
		conditionOps_[0] = op;
		conditionOps_[1] = acSwitch;
	}

	/**
	 * A constructor for a simple action switch check.
	 * 
	 * @param condAction
	 *            The action condition.
	 * @param acSwitch
	 *            The switch value of the action condition.
	 * @param action
	 *            The action to trigger if condition met.
	 * @param switched
	 *            Whether to activate or deactivate the action.
	 */
	public Rule(PacManHighAction condAction, boolean acSwitch,
			PacManHighAction action, boolean switched) {
		conditionObs_ = new PacManObservations[0];
		conditionAc_ = condAction;

		conditionOps_ = new boolean[1];
		conditionOps_[0] = acSwitch;

		action_ = action;
		actionOn_ = switched;
	}

	/**
	 * Evaluates the condition of this rule to see if it is true in the given
	 * scenario.
	 * 
	 * @param observations
	 *            The scenario observations, ordered by PacManObservations.
	 * @param actionSwitch
	 *            The actions switch, displaying which actions are
	 *            activated/deactivated.
	 * @return True if the conditions of this rule are met, false otherwise.
	 */
	public boolean evaluateCondition(double[] observations,
			ActionSwitch actionSwitch) {
		int i = 0;
		// Check every observation
		for (i = 0; i < conditionObs_.length; i++) {
			PacManObservations obs = conditionObs_[i];
			// X > Value (failure)
			if ((conditionOps_[i] == GREATER_THAN)
					&& (observations[obs.ordinal()] <= conditionVals_[i])) {
				return false;
			} else if ((conditionOps_[i] == LESS_THAN)
					&& (observations[obs.ordinal()] >= conditionVals_[i])) {
				// X < Value (failure)
				return false;
			}
		}

		// If there is an action condition, check that
		if (conditionAc_ != null) {
			// Action check (failure)
			if (!actionSwitch.isActionActive(conditionAc_)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Applies this rule's action to the action module.
	 * 
	 * @param actionSwitch
	 *            The actions module, to be (possibly) changed by this rule's
	 *            action.
	 */
	public void applyAction(ActionSwitch actionSwitch, int priority) {
		if (actionOn_)
			actionSwitch.switchOn(action_, priority);
		else
			actionSwitch.switchOff(action_);
	}
	
	/**
	 * Checks if this rule is an activation rule.
	 * 
	 * @return True if it is, false otherwise.
	 */
	public boolean isActivator() {
		return actionOn_;
	}

	/**
	 * Creates a string representation of the rule.
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("if ");
		// Just action case
		if (conditionObs_.length == 0) {
			actionToString(buffer, conditionAc_, conditionOps_[0]);
		} else {
			observationToString(buffer, 0);
			// Check for more observations
			int i = 1;
			if (conditionObs_.length > 1) {
				for (; i < conditionObs_.length; i++) {
					buffer.append("and ");
					observationToString(buffer, i);
				}
			}
			// Check for action case
			if (conditionAc_ != null) {
				buffer.append("and ");
				actionToString(buffer, conditionAc_, conditionOps_[i]);
			}	
		}
		
		// Then action
		buffer.append("then ");
		actionToString(buffer, action_, actionOn_);
		return buffer.toString();
	}

	/**
	 * Appends an observation in string format.
	 * 
	 * @param buffer The StringBuffer to append to.
	 * @param index The index of the observation.
	 */
	private void observationToString(StringBuffer buffer, int index) {
		buffer.append(conditionObs_[index]);
		if (conditionOps_[index] == GREATER_THAN)
			buffer.append(">");
		else
			buffer.append("<");
		buffer.append(conditionVals_[index] + " ");
	}

	/**
	 * Appends an action in string format.
	 * 
	 * @param buffer The StringBuffer to append to.
	 * @param action The action.
	 * @param switched The action switch.
	 */
	private void actionToString(StringBuffer buffer, PacManHighAction action, boolean switched) {
		buffer.append(action);
		if (switched)
			buffer.append("+ ");
		else
			buffer.append("- ");
	}
}
