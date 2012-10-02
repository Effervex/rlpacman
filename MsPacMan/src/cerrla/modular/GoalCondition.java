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
 *    src/cerrla/modular/GoalCondition.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package cerrla.modular;

import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cerrla.modular.GoalCondition;

/**
 * A small class representing a possible goal condition for the agent to pursue.
 * 
 * @author Samuel J. Sarjant
 */
public abstract class GoalCondition implements Serializable {
	private static final long serialVersionUID = 578463340574407596L;

	/** The name of the goal. */
	protected String goalName_;

	/** The pattern used to parse this goal type from Strings. */
	private final Pattern parsePattern_;

	/** A sorted list of facts using only constant terms */
	protected RelationalPredicate fact_;

	/** If this goal condition represents the environments main goal. */
	protected transient boolean isMainGoal_;

	/**
	 * Internal constructor. Just initialises the parse pattern.
	 */
	protected GoalCondition() {
		parsePattern_ = initialiseParsePattern();
	}

	/**
	 * Creates a duplicate goal condition.
	 * 
	 * @param goalCondition
	 *            The goal condition to duplicate.
	 */
	public GoalCondition(GoalCondition goalCondition) {
		this();
		fact_ = new RelationalPredicate(goalCondition.fact_);

		goalName_ = goalCondition.goalName_;
		isMainGoal_ = goalCondition.isMainGoal_;
	}

	/**
	 * A goal condition for a single fact goal.
	 * 
	 * @param fact
	 *            The single fact.
	 */
	public GoalCondition(RelationalPredicate fact) {
		this();
		fact_ = new RelationalPredicate(fact);
		goalName_ = formName(fact_);
		isMainGoal_ = false;
	}

	/**
	 * A goal condition for a String representing a single fact.
	 * 
	 * @param strFact
	 *            The string fact.
	 */
	public GoalCondition(String strFact) {
		this();
		// Attempt to extract a goal from the string.
		fact_ = extractFact(strFact);

		goalName_ = strFact;
		isMainGoal_ = false;
	}

	/**
	 * Extracts a fact using a given pre-processed matcher.
	 * 
	 * @param predName
	 *            The name of the pred extracted.
	 * @param suffix
	 *            The rest of the string after the name.
	 * @return The extracted fact or null.
	 */
	protected abstract RelationalPredicate extractFact(String predName,
			String suffix);

	/**
	 * Initialise the parse pattern for this goal type, where the first group
	 * always contains the pred name.
	 * 
	 * @return The regex parse pattern or null.
	 */
	protected abstract Pattern initialiseParsePattern();

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
	 * Clones this goal condition.
	 * 
	 * @return A clone of this GoalCondition.
	 */
	public abstract GoalCondition clone();

	/**
	 * Extracts a String fact from a goal.
	 * 
	 * @param strFact
	 *            The string fact to extract a goal from.
	 * @return The RelationalPredicate extracted or null.
	 */
	public RelationalPredicate extractFact(String strFact) {
		if (parsePattern_ == null)
			return null;

		Matcher m = parsePattern_.matcher(strFact);
		if (m.find()) {
			String predName = m.group(1);
			return extractFact(m.group(1), strFact.substring(predName.length()));
		}
		return null;
	}

	/**
	 * Forms the String name of a sub-goal given a fact. The fact should be in
	 * the same format as the parse pattern.
	 * 
	 * @param fact
	 *            The fact to form a name for.
	 * @return The String version of the fact.
	 */
	public abstract String formName(RelationalPredicate fact);

	public RelationalPredicate getFact() {
		return fact_;
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
	 * Sets this goal as the main goal.
	 */
	public void setAsMainGoal() {
		isMainGoal_ = true;
	}

	@Override
	public String toString() {
		return goalName_;
	}

	/**
	 * Parses a goal condition from a String.
	 * 
	 * @param goalString
	 *            The goal condition string.
	 * @return The parsed GoalCondition.
	 */
	public static GoalCondition parseGoalCondition(String goalString) {
		// Attempt to parse the string over all different subclasses.
		// Specific
		GoalCondition parsed = new SpecificGoalCondition(goalString);
		if (parsed.getFact() != null)
			return parsed;

		// General
		parsed = new GeneralGoalCondition(goalString);
		if (parsed.getFact() != null)
			return parsed;

		// Accumulate
		parsed = new AccumulativeGoalCondition(goalString);
		if (parsed.getFact() != null)
			return parsed;

		return new UndefinedGoalCondition(goalString);
	}

	/**
	 * Gets the number of arguments this goal takes.
	 * 
	 * @return The number of args the goal takes.
	 */
	public int getNumArgs() {
		if (isMainGoal_)
			return StateSpec.getInstance().getConstants().size();
		return 0;
	}

	public String getFactName() {
		if (fact_ == null)
			return goalName_;
		return fact_.getFactName();
	}
}
