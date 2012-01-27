package cerrla.modular;

import java.util.regex.Pattern;

import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

public class GeneralGoalCondition extends GoalCondition {
	private static final long serialVersionUID = -2766333865819489817L;

	/** The suffix to append to existence preds. */
	public static final String EXISTENCE_SUFFIX = "EXST";

	/** The suffix to append to non-existence preds. */
	public static final String NON_EXISTENCE_SUFFIX = "!" + EXISTENCE_SUFFIX;

	public static final Pattern PARSE_PATTERN = Pattern.compile("^(\\w+)!?"
			+ EXISTENCE_SUFFIX + "$");

	public GeneralGoalCondition(RelationalPredicate fact) {
		super(fact);
	}

	public GeneralGoalCondition(String strFact) {
		super(strFact);
	}

	public GeneralGoalCondition(GeneralGoalCondition goalCondition) {
		super(goalCondition);
	}

	@Override
	public GoalCondition clone() {
		return new GeneralGoalCondition(this);
	}

	@Override
	protected RelationalPredicate extractFact(String predName, String suffix) {
		RelationalPredicate pred = new RelationalPredicate(StateSpec
				.getInstance().getPredicateByName(predName));
		if (suffix.startsWith("!"))
			pred.swapNegated();
		return pred;
	}

	@Override
	protected Pattern initialiseParsePattern() {
		return PARSE_PATTERN;
	}

	@Override
	public String formName(RelationalPredicate fact) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(fact.getFactName());
		if (!fact.isNegated())
			buffer.append(EXISTENCE_SUFFIX);
		else
			buffer.append(NON_EXISTENCE_SUFFIX);
		return buffer.toString();
	}
}
