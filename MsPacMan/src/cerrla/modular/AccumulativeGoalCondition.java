package cerrla.modular;

import java.util.regex.Pattern;

import relationalFramework.RelationalPredicate;

public class AccumulativeGoalCondition extends GoalCondition {
	private static final long serialVersionUID = -4654647252530864598L;

	public static final Pattern PARSE_PATTERN = Pattern
			.compile("^(\\w+)(\\+|-)$");

	public AccumulativeGoalCondition(RelationalPredicate fact) {
		super(fact);
	}

	public AccumulativeGoalCondition(String strFact) {
		super(strFact);
	}

	public AccumulativeGoalCondition(AccumulativeGoalCondition goalCondition) {
		super(goalCondition);
	}

	@Override
	public GoalCondition clone() {
		return new AccumulativeGoalCondition(this);
	}

	@Override
	protected RelationalPredicate extractFact(String predName, String suffix) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Pattern initialiseParsePattern() {
		return PARSE_PATTERN;
	}

	@Override
	public String formName(RelationalPredicate fact) {
		// TODO Auto-generated method stub
		return null;
	}

}
