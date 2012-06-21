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
