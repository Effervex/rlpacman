package relationalFramework;

import relationalFramework.GoalCondition;
import relationalFramework.RelationalPredicate;
import util.ConditionComparator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A small class representing a possible goal condition for the agent to pursue.
 * 
 * @author Samuel J. Sarjant
 */
public class GoalCondition implements Serializable {
	/** The string for joining predicates. */
	private static final String MODULE_JOIN = "";

	/** The format individual goals take. */
	private static final Pattern GOAL_FORM = Pattern.compile("\\w+(\\$[A-Z])+");

	private static final long serialVersionUID = 578463340574407596L;

	/** A sorted list of facts using only constant terms */
	private SortedSet<RelationalPredicate> facts_;

	/** The number of arguments the goal condition has. */
	private ArrayList<String> goalArgs_;

	/** The name of the goal. */
	private String goalName_;

	/** If this goal condition represents the environments main goal. */
	private boolean isMainGoal_;

	/**
	 * A goal condition for a single fact goal.
	 * 
	 * @param facts
	 *            A collection of facts.
	 */
	public GoalCondition(Collection<RelationalPredicate> facts) {
		facts_ = new TreeSet<RelationalPredicate>(
				ConditionComparator.getInstance());
		goalArgs_ = new ArrayList<String>();
		for (RelationalPredicate fact : facts) {
			facts_.add(new RelationalPredicate(fact));
			for (String arg : fact.getArguments()) {
				if (!goalArgs_.contains(arg))
					goalArgs_.add(arg);
			}
		}
		goalName_ = formName(facts_);
		isMainGoal_ = false;
	}

	/**
	 * A goal condition for a single fact goal.
	 * 
	 * @param fact
	 *            The single fact.
	 */
	public GoalCondition(RelationalPredicate fact) {
		facts_ = new TreeSet<RelationalPredicate>(
				ConditionComparator.getInstance());
		facts_.add(new RelationalPredicate(fact));
		goalArgs_ = new ArrayList<String>();
		for (String arg : fact.getArguments()) {
			if (!goalArgs_.contains(arg))
				goalArgs_.add(arg);
		}
		goalName_ = formName(facts_);
		isMainGoal_ = false;
	}

	/**
	 * A goal condition for an undefined String goal (usually the one given by
	 * the environment).
	 * 
	 * @param goalString
	 *            The goal String.
	 */
	public GoalCondition(String goalString) {
		extractGoal(goalString);
		isMainGoal_ = true;
	}

	/**
	 * Attempts to extract a goal from a String representation, using the way
	 * the String is formatted.
	 * 
	 * @param goalString
	 */
	private void extractGoal(String goalString) {
		goalName_ = goalString;
		goalArgs_ = new ArrayList<String>();
		facts_ = new TreeSet<RelationalPredicate>(
				ConditionComparator.getInstance());
		
		// Parse for a specific goal
		Matcher m = GOAL_FORM.matcher(goalString);
		while (m.find()) {
			String goal = m.group();
			int specIndex = goal.indexOf('$');
			
			// Determine the arguments
			String args = goalString.substring(specIndex);
			String[] arguments = new String[args.length() / 2];
			for (int a = 0; a < arguments.length; a++) {
				int argIndex = goalString.charAt(specIndex + a * 2 + 1) - 'A';
				arguments[a] = RelationalArgument.createGoalTerm(argIndex);
				if (!goalArgs_.contains(arguments[a]))
					goalArgs_.add(arguments[a]);
			}
			
			// Finding the predicate
			String predName = goalString.substring(0, specIndex);
			RelationalPredicate pred = StateSpec.getInstance().getPredicateByName(predName);
			pred = new RelationalPredicate(pred, arguments);
			facts_.add(pred);
		}
	}

	/**
	 * Creates a duplicate goal condition.
	 * 
	 * @param goalCondition
	 *            The goal condition to duplicate.
	 */
	public GoalCondition(GoalCondition goalCondition) {
		facts_ = new TreeSet<RelationalPredicate>(
				ConditionComparator.getInstance());
		for (RelationalPredicate fact : goalCondition.facts_)
			facts_.add(new RelationalPredicate(fact));

		goalArgs_ = new ArrayList<String>(goalCondition.goalArgs_);
		goalName_ = goalCondition.goalName_;
		isMainGoal_ = goalCondition.isMainGoal_;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GoalCondition other = (GoalCondition) obj;
		if (goalName_ == null) {
			if (other.goalName_ != null)
				return false;
		} else if (!goalName_.equals(other.goalName_))
			return false;
		return true;
	}

	/**
	 * Gets the args used in this particular goal condition.
	 * 
	 * @return The args used in these goal conditions.
	 */
	public ArrayList<String> getConstantArgs() {
		return goalArgs_;
	}

	public SortedSet<RelationalPredicate> getFacts() {
		return facts_;
	}

	public int getNumArgs() {
		if (isMainGoal_)
			return StateSpec.getInstance().getConstants().size();
		if (goalArgs_ == null)
			return 0;
		return goalArgs_.size();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((goalName_ == null) ? 0 : goalName_.hashCode());
		return result;
	}

	public boolean isMainGoal() {
		return isMainGoal_;
	}

	/**
	 * Shifts the variables of the goal condition fact arguments such that they
	 * start from ?G_0 and continue from there.
	 */
	public void normaliseArgs() {
		if (facts_ == null)
			return;

		goalArgs_.clear();
		Map<String, String> replacements = new HashMap<String, String>();
		int i = 0;
		for (RelationalPredicate fact : facts_) {
			for (String arg : fact.getArguments()) {
				if (!replacements.containsKey(arg)) {
					String goalTerm = RelationalArgument.createGoalTerm(i++);
					goalArgs_.add(goalTerm);
					replacements.put(arg, goalTerm);
				}
			}
			fact.replaceArguments(replacements, false, false);
		}
	}

	/**
	 * Splits this goal condition into singular predicate goal conditions.
	 * 
	 * @return A collection of goal conditions which make up this goal
	 *         condition.
	 */
	public Collection<GoalCondition> splitCondition() {
		if (facts_ == null) {
			Collection<GoalCondition> goalConds = new ArrayList<GoalCondition>(
					1);
			goalConds.add(this);
			return goalConds;
		}
		Collection<GoalCondition> goalConds = new ArrayList<GoalCondition>(
				facts_.size());
		if (facts_.size() == 1) {
			goalConds.add(this);
			return goalConds;
		}

		for (RelationalPredicate fact : facts_) {
			goalConds.add(new GoalCondition(fact));
		}
		return goalConds;
	}

	@Override
	public String toString() {
		return goalName_;
	}

	/**
	 * Forms the name of a module file.
	 * 
	 * @param fact
	 *            The fact to create a name for.
	 * @return A String representing the filename of the module.
	 */
	public static String formName(RelationalPredicate fact) {
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
	 * Forms the name of a module file.
	 * 
	 * @param facts
	 *            The facts to create a name for.
	 * @return A String representing the filename of the module.
	 */
	public static String formName(SortedSet<RelationalPredicate> facts) {
		StringBuffer buffer = new StringBuffer();
		boolean first = true;
		Map<String, Character> argMapping = new HashMap<String, Character>();
		int i = 0;
		for (RelationalPredicate fact : facts) {
			if (!first)
				buffer.append(MODULE_JOIN);
			buffer.append(fact.getFactName());
			for (String arg : fact.getArguments()) {
				if (!argMapping.containsKey(arg))
					argMapping.put(arg, (char) ('A' + i++));
				buffer.append("$" + argMapping.get(arg));
			}

			first = false;
		}

		return buffer.toString();
	}
}
