package cerrla.modular;

import java.util.regex.Pattern;

import relationalFramework.RelationalPredicate;

public class UndefinedGoalCondition extends GoalCondition {
	private static final long serialVersionUID = 926840740471344995L;

	public UndefinedGoalCondition(String goalString) {
		super(goalString);
	}

	public UndefinedGoalCondition(UndefinedGoalCondition goalCondition) {
		fact_ = null;

		goalName_ = goalCondition.goalName_;
		isMainGoal_ = goalCondition.isMainGoal_;
	}

	@Override
	protected RelationalPredicate extractFact(String predName, String suffix) {
		// No extracting.
		return null;
	}

	@Override
	protected Pattern initialiseParsePattern() {
		// No parse pattern.
		return null;
	}

	@Override
	public GoalCondition clone() {
		return new UndefinedGoalCondition(this);
	}

	@Override
	public String formName(RelationalPredicate fact) {
		// No name forming.
		return null;
	}

}
