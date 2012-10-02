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
 *    src/cerrla/modular/AccumulativeGoalCondition.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package cerrla.modular;

import java.util.regex.Pattern;

import relationalFramework.RelationalPredicate;

public class AccumulativeGoalCondition extends GoalCondition {
	private static final long serialVersionUID = -4654647252530864598L;

	public static final Pattern PARSE_PATTERN = Pattern
			.compile("^(\\w+)(\\+|-)$");

	public AccumulativeGoalCondition(RelationalPredicate fact) {
		super(fact);
	}

	public AccumulativeGoalCondition(String strFact) {
		super(strFact);
	}

	public AccumulativeGoalCondition(AccumulativeGoalCondition goalCondition) {
		super(goalCondition);
	}

	@Override
	public GoalCondition clone() {
		return new AccumulativeGoalCondition(this);
	}

	@Override
	protected RelationalPredicate extractFact(String predName, String suffix) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Pattern initialiseParsePattern() {
		return PARSE_PATTERN;
	}

	@Override
	public String formName(RelationalPredicate fact) {
		// TODO Auto-generated method stub
		return null;
	}

}
