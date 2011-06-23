package relationalFramework.agentObservations;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import relationalFramework.GoalCondition;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

/**
 * A class for representing a possible goal state to use (i.e. one that has
 * occurred).
 * 
 * @author Sam Sarjant
 */
public class GoalArg implements Comparable<GoalArg>, Serializable {
	private static final long serialVersionUID = 4406724165957986857L;

	/** The facts present in the state. */
	private Map<String, String> args_;

	/** The goal facts defined by the arguments. */
	private StringFact goalFact_;

	/**
	 * Constructor for a new GoalState.
	 * 
	 * @param facts
	 *            The facts of the goal state, sorted by predicate.
	 * @param currentGoal
	 *            The current goal.
	 */
	public GoalArg(Map<String, String> args, GoalCondition currentGoal) {
		args_ = args;

		// Replace the arguments and set the goal.
		goalFact_ = new StringFact(currentGoal.getFact());
		goalFact_.replaceArguments(args, true);
	}

	/**
	 * If this goal instantiation contains any of the parameterised invariants.
	 * 
	 * @param invariants
	 *            The invariants to check against.
	 * @return True if the goal arguments contains the invariants.
	 */
	public boolean includesInvariants(Collection<StringFact> invariants) {
		if (invariants.isEmpty())
			return false;

		// Don't check type preds, as they are generally always true.
		if (!StateSpec.getInstance().isTypePredicate(goalFact_.getFactName())
				&& invariants.contains(goalFact_))
			return true;
		return false;
	}

	/**
	 * Gets the facts.
	 * 
	 * @return The arguments for the goal.
	 */
	public Map<String, String> getGoalArgs() {
		return args_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((args_ == null) ? 0 : args_.hashCode());
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
		GoalArg other = (GoalArg) obj;
		if (args_ == null) {
			if (other.args_ != null)
				return false;
		} else if (!args_.equals(other.args_))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return goalFact_.toString();
	}

	@Override
	public int compareTo(GoalArg arg0) {
		return goalFact_.compareTo(arg0.goalFact_);
	}
}
