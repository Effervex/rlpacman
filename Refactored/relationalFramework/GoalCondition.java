package relationalFramework;

import relationalFramework.GoalCondition;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A small class representing a possible goal condition for the agent to pursue.
 * 
 * @author Samuel J. Sarjant
 */
public class GoalCondition implements Serializable {
	private static final long serialVersionUID = 578463340574407596L;

	/** The string for joining predicates. */
	private static final String MODULE_JOIN = "&";

	/** A sorted list of facts using only constant terms */
	private RelationalPredicate fact_;

	/** The number of arguments the goal condition has. */
	private int numArgs_;

	/** The name of the goal. */
	private String goalName_;

	/**
	 * A goal condition for a single fact goal.
	 * 
	 * @param fact
	 *            The single fact.
	 */
	public GoalCondition(RelationalPredicate fact) {
		fact_ = new RelationalPredicate(fact);
		Set<String> seenArgs = new HashSet<String>();
		for (String arg : fact.getArguments())
			seenArgs.add(arg);
		numArgs_ = seenArgs.size();
		goalName_ = formName(fact_);
	}

	public RelationalPredicate getFact() {
		return fact_;
	}

	public int getNumArgs() {
		return numArgs_;
	}

	/**
	 * Shifts the variables of the goal condition fact arguments such that they
	 * start from ?G_0 and continue from there.
	 */
	public void normaliseArgs() {
		Map<String, String> replacements = new HashMap<String, String>();
		int i = 0;
		for (String arg : fact_.getArguments()) {
			if (!replacements.containsKey(arg))
				replacements.put(arg, StateSpec.createGoalTerm(i++));
		}
		fact_.replaceArguments(replacements, false, false);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((goalName_ == null) ? 0 : goalName_.hashCode());
		return result;
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
		boolean first = true;
		Map<String, Character> argMapping = new HashMap<String, Character>();
		int i = 0;
		if (!first)
			buffer.append(MODULE_JOIN);
		buffer.append(fact.getFactName());
		for (String arg : fact.getArguments()) {
			if (!argMapping.containsKey(arg))
				argMapping.put(arg, (char) ('A' + i++));
			buffer.append(argMapping.get(arg));
		}

		first = false;

		return buffer.toString();
	}
}
