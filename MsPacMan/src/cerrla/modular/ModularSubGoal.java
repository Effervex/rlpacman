/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/cerrla/modular/ModularSubGoal.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
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

	public void setGoalAchieved(boolean achieved) {
		if (filledPolicy_ != null) {
			if (achieved)
				filledPolicy_.setGoalAchieved();
			else
				filledPolicy_.setGoalUnachieved(false);
		}
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
}
