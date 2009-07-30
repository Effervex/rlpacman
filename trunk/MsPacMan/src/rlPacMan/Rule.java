package rlPacMan;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class represents a rule given in and 'if (condition) then (action)'. The
 * condition is given as an inequity/s from PacManObservations and the action is
 * given as a + or - from PacManHighActions.
 * 
 * @author Samuel J. Sarjant
 */
public class Rule {
	public static final boolean GREATER_THAN = true; // X > 5
	public static final boolean LESS_THAN = false; // X < 5
	public static final String OBSERVATION = "o";
	public static final String ACTION = "a";
	public static final String PRE_SEPARATOR = ":";
	public static final String ACTION_SEPARATOR = "=";

	private Condition[] conditionObs_;
	private boolean[] conditionOps_;
	private double[] conditionVals_;
	private Action action_;
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
	public Rule(PacManObservation obs, boolean op, double value,
			PacManHighAction action, boolean switched) {
		conditionObs_ = new PacManObservation[1];
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
	public Rule(PacManObservation obs0, boolean op0, double value0,
			PacManObservation obs1, boolean op1, double value1,
			PacManHighAction action, boolean switched) {
		conditionObs_ = new PacManObservation[2];
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
	public Rule(PacManObservation obs, boolean op, double value,
			PacManHighAction condAction, boolean acSwitch,
			PacManHighAction action, boolean switched) {
		conditionObs_ = new Condition[2];
		conditionObs_[0] = obs;
		conditionObs_[1] = condAction;

		conditionOps_ = new boolean[2];
		conditionOps_[0] = op;
		conditionOps_[1] = acSwitch;

		conditionVals_ = new double[1];
		conditionVals_[0] = value;

		action_ = action;
		actionOn_ = switched;
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
		conditionObs_ = new PacManHighAction[1];
		conditionObs_[0] = condAction;

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
			Condition cond = conditionObs_[i];
			if (cond instanceof PacManObservation) {
				// X > Value (failure)
				if ((conditionOps_[i] == GREATER_THAN)
						&& (observations[cond.ordinal()] < conditionVals_[i])) {
					return false;
				} else if ((conditionOps_[i] == LESS_THAN)
						&& (observations[cond.ordinal()] >= conditionVals_[i])) {
					// X < Value (failure)
					return false;
				}
			} else if (cond instanceof PacManHighAction) {
				// Action check (failure)
				if (!actionSwitch.isActionActive((Action) cond)) {
					return false;
				}
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
	 * Converts the rule into parseable format.
	 * 
	 * @return A parsable String representing the rule.
	 */
	public String toParseableString() {
		StringBuffer buffer = new StringBuffer();

		// Just action case
		if (conditionObs_[0] instanceof Action) {
			buffer.append(ACTION + PRE_SEPARATOR + conditionObs_[0].ordinal()
					+ PRE_SEPARATOR + conditionOps_[0]);
		} else {
			buffer.append(OBSERVATION + PRE_SEPARATOR
					+ conditionObs_[0].ordinal() + PRE_SEPARATOR
					+ conditionOps_[0] + PRE_SEPARATOR + conditionVals_[0]);
			// Check for more observations
			int i = 1;
			if (conditionObs_.length > 1) {
				for (; i < conditionObs_.length; i++) {
					buffer.append(PRE_SEPARATOR);
					if (conditionObs_[i] instanceof Action) {
						buffer.append(ACTION + PRE_SEPARATOR
								+ conditionObs_[i].ordinal() + PRE_SEPARATOR
								+ conditionOps_[i]);
					} else {
						buffer.append(OBSERVATION + PRE_SEPARATOR
								+ conditionObs_[i].ordinal() + PRE_SEPARATOR
								+ conditionOps_[i] + PRE_SEPARATOR
								+ conditionVals_[i]);
					}
				}
			}
		}

		// Then action
		buffer.append(ACTION_SEPARATOR);
		buffer.append(action_.ordinal() + PRE_SEPARATOR + actionOn_);
		return buffer.toString();
	}

	/**
	 * Parses a rule from a string representation of the rule.
	 * 
	 * @param ruleString
	 *            The rule in String form.
	 * @return The Rule from the String or null.
	 */
	public static Rule parseRule(String ruleString) {
		ArrayList<Integer> observations = new ArrayList<Integer>();
		ArrayList<Boolean> operators = new ArrayList<Boolean>();
		ArrayList<Double> values = new ArrayList<Double>();
		int preAction = -1;
		PacManHighAction action = null;
		boolean actionVal = false;

		String[] split = ruleString.split(ACTION_SEPARATOR);
		String[] preconditionSplit = split[0].split(PRE_SEPARATOR);

		// Parsing the pre conditions
		int index = 0;
		while (index < preconditionSplit.length) {
			if (preconditionSplit[index].equals(OBSERVATION)) {
				// Observation splits
				index++;
				observations.add(Integer.parseInt(preconditionSplit[index]));
				index++;
				operators.add(Boolean.parseBoolean(preconditionSplit[index]));
				index++;
				values.add(Double.parseDouble(preconditionSplit[index]));
				index++;
			} else if (preconditionSplit[index].equals(ACTION)) {
				// Action splits
				index++;
				preAction = Integer.parseInt(preconditionSplit[index]);
				index++;
				operators.add(Boolean.parseBoolean(preconditionSplit[index]));
				index++;
			}
		}

		// Parsing the action
		String[] actionSplit = split[1].split(PRE_SEPARATOR);
		action = PacManHighAction.values()[Integer.parseInt(actionSplit[0])];
		actionVal = Boolean.parseBoolean(actionSplit[1]);

		// Choosing the appropriate constructor
		if (observations.isEmpty()) {
			// One action
			return new Rule(PacManHighAction.values()[preAction], operators
					.get(0), action, actionVal);
		} else {
			if (preAction != -1) {
				// One obs and one action
				return new Rule(
						PacManObservation.values()[observations.get(0)],
						operators.get(0), values.get(0), PacManHighAction
								.values()[preAction], operators.get(1), action,
						actionVal);
			} else {
				if (observations.size() == 1) {
					return new Rule(PacManObservation.values()[observations
							.get(0)], operators.get(0), values.get(0), action,
							actionVal);
				} else {
					return new Rule(PacManObservation.values()[observations
							.get(0)], operators.get(0), values.get(0),
							PacManObservation.values()[observations.get(1)],
							operators.get(1), values.get(1), action, actionVal);
				}
			}
		}
	}

	/**
	 * 
	 * Creates a string representation of the rule.
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("if ");
		// Just action case
		if (conditionObs_[0] instanceof Action) {
			actionToString(buffer, (Action) conditionObs_[0], conditionOps_[0]);
		} else {
			observationToString(buffer, 0);
			// Check for more observations
			int i = 1;
			if (conditionObs_.length > 1) {
				for (; i < conditionObs_.length; i++) {
					buffer.append("and ");
					if (conditionObs_[i] instanceof Action)
						actionToString(buffer, (Action) conditionObs_[i],
								conditionOps_[i]);
					else
						observationToString(buffer, i);
				}
			}
		}

		// Then action
		buffer.append("then ");
		actionToString(buffer, action_, actionOn_);
		return buffer.toString();
	}

	// @Override
	public boolean equals(Object obj) {
		if ((obj == null) || (!(obj instanceof Rule)))
			return false;

		Rule otherRule = (Rule) obj;
		if (Arrays.equals(conditionObs_, otherRule.conditionObs_)) {
			if (Arrays.equals(conditionOps_, otherRule.conditionOps_)) {
				if (Arrays.equals(conditionVals_, otherRule.conditionVals_)) {
					if (action_.equals(otherRule.action_)) {
						if (actionOn_ == otherRule.actionOn_) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	// @Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Appends an observation in string format.
	 * 
	 * @param buffer
	 *            The StringBuffer to append to.
	 * @param index
	 *            The index of the observation.
	 */
	private void observationToString(StringBuffer buffer, int index) {
		buffer.append(conditionObs_[index]);
		if (conditionOps_[index] == GREATER_THAN)
			buffer.append(">=");
		else
			buffer.append("<");
		buffer.append(conditionVals_[index] + " ");
	}

	/**
	 * Appends an action in string format.
	 * 
	 * @param buffer
	 *            The StringBuffer to append to.
	 * @param action
	 *            The action.
	 * @param switched
	 *            The action switch.
	 */
	private void actionToString(StringBuffer buffer, Action action,
			boolean switched) {
		buffer.append(action);
		if (switched)
			buffer.append("+ ");
		else
			buffer.append("- ");
	}

	/**
	 * Gets the conditions of this rule, but not the operators or values.
	 * 
	 * @return The conditions of the rule.
	 */
	public Condition[] getConditions() {
		return conditionObs_;
	}
	
	/**
	 * Returns the action for this rule, but not the operator for it.
	 * 
	 * @return The rule action.
	 */
	public Action getAction() {
		return action_;
	}
}
