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
 *    src/cerrla/modular/UndefinedGoalCondition.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package cerrla.modular;

import java.util.regex.Pattern;

import relationalFramework.RelationalPredicate;

public class UndefinedGoalCondition extends GoalCondition {
	private static final long serialVersionUID = 926840740471344995L;

	public UndefinedGoalCondition(String goalString) {
		super(goalString);
	}

	public UndefinedGoalCondition(UndefinedGoalCondition goalCondition) {
		fact_ = null;

		goalName_ = goalCondition.goalName_;
		isMainGoal_ = goalCondition.isMainGoal_;
	}

	@Override
	protected RelationalPredicate extractFact(String predName, String suffix) {
		// No extracting.
		return null;
	}

	@Override
	protected Pattern initialiseParsePattern() {
		// No parse pattern.
		return null;
	}

	@Override
	public GoalCondition clone() {
		return new UndefinedGoalCondition(this);
	}

	@Override
	public String formName(RelationalPredicate fact) {
		// No name forming.
		return null;
	}

}
