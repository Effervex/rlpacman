package cerrla;

import java.io.Serializable;

import org.apache.commons.collections.BidiMap;

import relationalFramework.GoalCondition;
import relationalFramework.RelationallyEvaluatableObject;

public class ModularHole implements RelationallyEvaluatableObject, Serializable {
	private static final long serialVersionUID = -5264925508869209636L;
	/** The subgoal this hole defines. */
	private final GoalCondition subgoal_;

	/**
	 * Creates a new modular hole which defines a sub-goal condition that can
	 * belong here.
	 * 
	 * @param goalCondition
	 *            The goal of this hole.
	 */
	public ModularHole(GoalCondition goalCondition) {
		subgoal_ = goalCondition;
	}

	@Override
	public void setParameters(BidiMap goalArgs) {
		// Do nothing, as this isn't evaluated.
	}

	@Override
	public String toNiceString() {
		return "Sub-goal hole: " + subgoal_.getFacts();
	}

	@Override
	public boolean shouldRegenerate() {
		// Always regenerate.
		return true;
	}

	@Override
	public GoalCondition getGoalCondition() {
		return subgoal_;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((subgoal_ == null) ? 0 : subgoal_.hashCode());
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
		ModularHole other = (ModularHole) obj;
		if (subgoal_ == null) {
			if (other.subgoal_ != null)
				return false;
		} else if (!subgoal_.equals(other.subgoal_))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "Modular Hole " + subgoal_;
	}
}
