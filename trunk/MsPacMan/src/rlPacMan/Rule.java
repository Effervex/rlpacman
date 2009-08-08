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
	public static final String OBSERVATION = "o";
	public static final String ACTION = "a";
	public static final String ACTION_SEPARATOR = "=";

	private Condition[] conditions_;
	private ActionCondition action_;

	/**
	 * A basic constructor for a single condition rule.
	 * 
	 * @param obs
	 *            The observation condition
	 * @param action
	 *            The action to trigger if condition met.
	 */
	public Rule(Condition obs, ActionCondition action) {
		conditions_ = new PacManObservation[1];
		conditions_[0] = obs;

		action_ = action;
	}

	/**
	 * A constructor for a two condition rule made up of observations.
	 * 
	 * @param obs0
	 *            The first observation condition
	 * @param obs1
	 *            The second observation condition
	 * @param action
	 *            The action to trigger if condition met.
	 */
	public Rule(Condition obs0, Condition obs1, ActionCondition action) {
		conditions_ = new Condition[2];
		conditions_[0] = obs0;
		conditions_[1] = obs1;

		action_ = action;
	}

	/**
	 * A constructor for a two condition rule made up of an observation and
	 * action check.
	 * 
	 * @param obs
	 *            The observation condition
	 * @param condAction
	 *            The action condition
	 * @param action
	 *            The action to trigger if condition met.
	 */
	public Rule(Condition obs, ActionCondition condAction,
			ActionCondition action) {
		conditions_ = new Condition[2];
		conditions_[0] = obs;
		conditions_[1] = condAction;

		action_ = action;
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
	public Rule(ActionCondition condAction, ActionCondition action) {
		conditions_ = new ActionCondition[1];
		conditions_[0] = condAction;

		action_ = action;
	}

	/**
	 * Evaluates the condition of this rule to see if it is true in the given
	 * scenario.
	 * 
	 * @param observations
	 *            The scenario observations, ordered by ObservationConditions.
	 * @param actionSwitch
	 *            The actions switch, displaying which actions are
	 *            activated/deactivated.
	 * @return True if the conditions of this rule are met, false otherwise.
	 */
	public boolean evaluateConditions(double[] observations,
			ActionSwitch actionSwitch) {
		int i = 0;
		// Check every observation
		for (i = 0; i < conditions_.length; i++) {
			Condition cond = conditions_[i];
			if (cond instanceof ObservationCondition) {
				// If failed, return false
				if (!cond.evaluateCondition(observations))
					return false;
			} else if (cond instanceof ActionCondition) {
				if (!cond.evaluateCondition(actionSwitch))
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
		if (action_.getOperator())
			actionSwitch.switchOn(action_.getCondition(), priority);
		else
			actionSwitch.switchOff(action_.getCondition());
	}

	/**
	 * Checks if this rule is an activation rule.
	 * 
	 * @return True if it is, false otherwise.
	 */
	public boolean isActivator() {
		return action_.getOperator();
	}

	/**
	 * Converts the rule into parseable format.
	 * 
	 * @return A parsable String representing the rule.
	 */
	public String toParseableString() {
		StringBuffer buffer = new StringBuffer();

		// Just action case
		if (conditions_[0] instanceof ActionCondition) {
			buffer.append(ACTION + Condition.PRE_SEPARATOR
					+ conditions_[0].toParseableString());
		} else {
			buffer.append(OBSERVATION + Condition.PRE_SEPARATOR
					+ conditions_[0].toParseableString());
			// Check for more observations
			int i = 1;
			if (conditions_.length > 1) {
				for (; i < conditions_.length; i++) {
					buffer.append(Condition.PRE_SEPARATOR);
					if (conditions_[i] instanceof ActionCondition) {
						buffer.append(ACTION + Condition.PRE_SEPARATOR
								+ conditions_[i].toParseableString());
					} else {
						buffer.append(OBSERVATION + Condition.PRE_SEPARATOR
								+ conditions_[i].toParseableString());
					}
				}
			}
		}

		// Then action
		buffer.append(ACTION_SEPARATOR);
		buffer.append(action_.toParseableString());
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
		ArrayList<Integer> actions = new ArrayList<Integer>();

		String[] split = ruleString.split(ACTION_SEPARATOR);
		String[] preconditionSplit = split[0].split(Condition.PRE_SEPARATOR);

		// Parsing the pre conditions
		int index = 0;
		while (index < preconditionSplit.length) {
			if (preconditionSplit[index].equals(OBSERVATION)) {
				// Observation splits
				index++;
				index = ObservationCondition.parseObservationCondition(
						preconditionSplit, index, observations, operators,
						values);
			} else if (preconditionSplit[index].equals(ACTION)) {
				// Action splits
				index++;
				index = ActionCondition.parseCondition(preconditionSplit,
						index, actions, operators);
			}
		}

		// Parsing the action
		String[] actionSplit = split[1].split(Condition.PRE_SEPARATOR);
		ActionCondition.parseCondition(actionSplit, 0, actions, operators);

		// Choosing the appropriate constructor
		if (observations.isEmpty()) {
			// One action
			return new Rule(new PacManHighAction(actions.get(0), operators
					.get(0)), new PacManHighAction(actions.get(1), operators
					.get(1)));
		} else {
			if (actions.size() > 1) {
				// One obs and one action
				return new Rule(new PacManObservation(observations.get(0),
						operators.get(0), values.get(0)), new PacManHighAction(
						actions.get(0), operators.get(1)),
						new PacManHighAction(actions.get(1), operators.get(2)));
			} else {
				// One observation rule
				if (observations.size() == 1) {
					return new Rule(new PacManObservation(observations.get(0),
							operators.get(0), values.get(0)),
							new PacManHighAction(actions.get(0), operators
									.get(1)));
				} else {
					// Two observation rule
					return new Rule(new PacManObservation(observations.get(0),
							operators.get(0), values.get(0)),
							new PacManObservation(observations.get(1),
									operators.get(1), values.get(1)),
							new PacManHighAction(actions.get(0), operators
									.get(2)));
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
		for (int i = 0; i < conditions_.length; i++) {
			if (i != 0)
				buffer.append("and ");
			buffer.append(conditions_[i].toString() + " ");
		}

		// Then action
		buffer.append("then " + action_.toString());
		return buffer.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj == null) || (!(obj instanceof Rule)))
			return false;

		Rule otherRule = (Rule) obj;
		if (Arrays.equals(conditions_, otherRule.conditions_)) {
			if (action_.equals(otherRule.action_)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Gets the conditions of this rule.
	 * 
	 * @return The conditions of the rule.
	 */
	public Condition[] getConditions() {
		return conditions_;
	}

	/**
	 * Returns the action for this rule, but not the operator for it.
	 * 
	 * @return The rule action.
	 */
	public ActionCondition getAction() {
		return action_;
	}
}