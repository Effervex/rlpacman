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
 *    src/util/GoalConditionComparator.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package util;

import java.util.Comparator;

import cerrla.modular.GoalCondition;

import relationalFramework.RelationalArgument;

/**
 * A special comparator which overrides the equals method of GoalCondition.
 * 
 * @author Sam Sarjant
 */
public class GoalConditionComparator implements Comparator<GoalCondition> {
	@Override
	public int compare(GoalCondition o1, GoalCondition o2) {
		// If unequal
		if (!o1.equals(o2))
			return Double.compare(o1.hashCode(), o2.hashCode());
		
		RelationalArgument[] o1args = o1.getFact().getRelationalArguments();
		RelationalArgument[] o2args = o2.getFact().getRelationalArguments();
		for (int i = 0; i < o1args.length; i++) {
			int result = o1args[i].compareTo(o2args[i]);
			if (result != 0)
				return result;
		}
		return 0;
	}
}
