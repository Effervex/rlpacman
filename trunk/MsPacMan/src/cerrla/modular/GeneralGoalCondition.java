package cerrla.modular;

import java.util.regex.Pattern;

import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

public class GeneralGoalCondition extends GoalCondition {
	private static final long serialVersionUID = -2766333865819489817L;

	/** The suffix to append to existence preds. */
	public static final String EXISTENCE_SUFFIX = "EXST";

	/** The suffix to append to non-existence preds. */
	public static final String NEGATED_PREFIX = "!";

	public static final Pattern PARSE_PATTERN = Pattern.compile("^!?(\\w+)"
			+ EXISTENCE_SUFFIX + "$");

	/** The negated version of this goal condition (for efficiency). */
	private GeneralGoalCondition negatedCondition_;

	/**
	 * A constructor for creating a negated fact.
	 * 
	 * @param negFact
	 *            The negated fact being created.
	 * @param posFact
	 *            The positive fact which triggered this fact's creation.
	 */
	private GeneralGoalCondition(RelationalPredicate negFact,
			GeneralGoalCondition posFact) {
		super(negFact);
		negatedCondition_ = posFact;
	}

	public GeneralGoalCondition(GeneralGoalCondition goalCondition) {
		super(goalCondition);
		createNegatedGoalCondition();
	}

	public GeneralGoalCondition(RelationalPredicate fact) {
		super(fact);
		createNegatedGoalCondition();
	}

	public GeneralGoalCondition(String strFact) {
		super(strFact);
		createNegatedGoalCondition();
	}

	private void createNegatedGoalCondition() {
		if (fact_ == null)
			return;
		RelationalPredicate negFact = new RelationalPredicate(fact_);
		negFact.swapNegated();
		negatedCondition_ = new GeneralGoalCondition(negFact, this);
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
	public GoalCondition clone() {
		return new GeneralGoalCondition(this);
	}

	@Override
	public String formName(RelationalPredicate fact) {
		StringBuffer buffer = new StringBuffer();
		if (fact.isNegated())
			buffer.append(NEGATED_PREFIX);
		buffer.append(fact.getFactName());
		buffer.append(EXISTENCE_SUFFIX);
		return buffer.toString();
	}

	/**
	 * Gets the negation of this general goal condition.
	 * 
	 * @return A negated version fo this goal condition.
	 */
	public GeneralGoalCondition getNegation() {
		return negatedCondition_;
	}
}
