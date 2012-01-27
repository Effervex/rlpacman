package cerrla.modular;

import java.io.Serializable;

import org.apache.commons.collections.BidiMap;

import relationalFramework.RelationalRule;

public class ModularSubGoal implements PolicyItem, Serializable {
	private static final long serialVersionUID = -5264925508869209636L;
	/** The policy that is currently filling this sub-goal. */
	private ModularPolicy filledPolicy_;
	/** The rule that defines this modular subgoal. */
	private final RelationalRule parentRule_;
	/** The subgoal this hole defines. */
	private final GoalCondition subgoal_;

	// TODO Goal Achieved?

	/**
	 * Creates a new modular hole which defines a sub-goal condition that can
	 * belong here.
	 * 
	 * @param goalCondition
	 *            The goal of this hole.
	 */
	public ModularSubGoal(GoalCondition goalCondition, RelationalRule parent) {
		subgoal_ = goalCondition;
		parentRule_ = parent;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModularSubGoal other = (ModularSubGoal) obj;
		if (parentRule_ == null) {
			if (other.parentRule_ != null)
				return false;
		} else if (!parentRule_.equals(other.parentRule_))
			return false;
		if (subgoal_ == null) {
			if (other.subgoal_ != null)
				return false;
		} else if (!subgoal_.equals(other.subgoal_))
			return false;
		return true;
	}

	/**
	 * Gets the sub-goal defined by this ModularSubGoal.
	 * 
	 * @return The subgoal condition.
	 */
	public GoalCondition getGoalCondition() {
		return subgoal_;
	}

	public ModularPolicy getModularPolicy() {
		return filledPolicy_;
	}

	public RelationalRule getParentRule() {
		return parentRule_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((parentRule_ == null) ? 0 : parentRule_.hashCode());
		result = prime * result
				+ ((subgoal_ == null) ? 0 : subgoal_.hashCode());
		return result;
	}

	public void setModularPolicy(ModularPolicy policy) {
		filledPolicy_ = policy;
	}

	@Override
	public void setParameters(BidiMap goalArgs) {
		if (filledPolicy_ != null)
			filledPolicy_.setParameters(goalArgs);
	}

	@Override
	public boolean shouldRegenerate() {
		// If no policy, it should
		if (filledPolicy_ == null || filledPolicy_.shouldRegenerate())
			return true;
		return false;
	}

	@Override
	public int size() {
		if (filledPolicy_ == null)
			return 0;
		return filledPolicy_.size();
	}

	@Override
	public String toNiceString() {
		return "Sub-goal hole: " + subgoal_.getFact();
	}

	@Override
	public String toString() {
		if (filledPolicy_ == null)
			return "Modular Sub-goal " + subgoal_;
		else
			return filledPolicy_.toString();
	}

	public void setGoalAchieved(boolean b) {
		if (filledPolicy_ != null)
			filledPolicy_.setGoalAchieved(true);
	}
}
