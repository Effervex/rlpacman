package cerrla.modular;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

public class SpecificGoalCondition extends GoalCondition {
	private static final long serialVersionUID = 8962378512823604600L;

	public static final Pattern PARSE_PATTERN = Pattern
			.compile("^(\\w+)((?:\\$[A-Z])+)$");

	/** The number of arguments the goal condition has. */
	private ArrayList<String> goalArgs_;

	public SpecificGoalCondition(RelationalPredicate fact) {
		super(fact);

		goalArgs_ = new ArrayList<String>();
		for (String arg : fact.getArguments()) {
			if (!goalArgs_.contains(arg))
				goalArgs_.add(arg);
		}
	}

	public SpecificGoalCondition(SpecificGoalCondition goalCondition) {
		super(goalCondition);
		goalArgs_ = new ArrayList<String>(goalCondition.goalArgs_);
	}

	public SpecificGoalCondition(String strFact) {
		super(strFact);
	}

	@Override
	public GoalCondition clone() {
		return new SpecificGoalCondition(this);
	}

	@Override
	protected RelationalPredicate extractFact(String predName, String suffix) {
		// Determine the arguments
		goalArgs_ = new ArrayList<String>();
		String[] arguments = new String[suffix.length() / 2];
		for (int a = 0; a < arguments.length; a++) {
			int argIndex = suffix.charAt(a * 2 + 1) - 'A';
			arguments[a] = RelationalArgument.createGoalTerm(argIndex);
			if (!goalArgs_.contains(arguments[a]))
				goalArgs_.add(arguments[a]);
		}

		// Finding the predicate
		RelationalPredicate pred = StateSpec.getInstance().getPredicateByName(
				predName);
		return new RelationalPredicate(pred, arguments);
	}

	@Override
	protected Pattern initialiseParsePattern() {
		return PARSE_PATTERN;
	}

	@Override
	public String formName(RelationalPredicate fact) {
		StringBuffer buffer = new StringBuffer();
		Map<String, Character> argMapping = new HashMap<String, Character>();
		int i = 0;
		buffer.append(fact.getFactName());
		for (String arg : fact.getArguments()) {
			if (!argMapping.containsKey(arg))
				argMapping.put(arg, (char) ('A' + i++));
			buffer.append("$" + argMapping.get(arg));
		}
		return buffer.toString();
	}

	/**
	 * Gets the args used in this particular goal condition.
	 * 
	 * @return The args used in these goal conditions.
	 */
	public ArrayList<String> getConstantArgs() {
		return goalArgs_;
	}

	@Override
	public int getNumArgs() {
		return goalArgs_.size();
	}

	/**
	 * Shifts the variables of the goal condition fact arguments such that they
	 * start from ?G_0 and continue from there.
	 */
	public void normaliseArgs() {
		if (fact_ == null)
			return;

		goalArgs_.clear();
		Map<String, String> replacements = new HashMap<String, String>();
		int i = 0;
		for (String arg : fact_.getArguments()) {
			if (!replacements.containsKey(arg)) {
				String goalTerm = RelationalArgument.createGoalTerm(i++);
				goalArgs_.add(goalTerm);
				replacements.put(arg, goalTerm);
			}
		}
		fact_.replaceArguments(replacements, false, false);
	}
}
