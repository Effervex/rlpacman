package util;

import relationalFramework.ArgumentType;
import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.io.Serializable;
import java.util.Comparator;


/**
 * A comparator for comparing conditions within a rule. The comparator performs
 * such that base conditions come first, then inequality predicates, then type
 * predicates.
 * 
 * @author Sam Sarjant
 */
public class ConditionComparator implements Comparator<RelationalPredicate>,
		Serializable {
	private static final long serialVersionUID = 2187893240541148300L;
	private static Comparator<RelationalPredicate> instance_;
	private final int GOAL = -1;
	private final int BASE = 0;
	private final int TYPE = 1;
	private final int NEG = 3;
	private final int INEQ = 2;

	private ConditionComparator() {

	}

	@Override
	public int compare(RelationalPredicate arg0, RelationalPredicate arg1) {
		if (arg0 == null) {
			if (arg1 == null)
				return 0;
			else
				return 1;
		} else if (arg1 == null) {
			return -1;
		}

		// First, compare by negation
		if (!arg0.isNegated() && arg1.isNegated())
			return -1;
		if (arg0.isNegated() && !arg1.isNegated())
			return 1;

		// Next compare by argument types contained within
		int[] argCounter0 = new int[ArgumentType.values().length];
		double numArgs0 = 0;
		for (RelationalArgument relArg : arg0.getRelationalArguments()) {
			argCounter0[relArg.getArgumentType().ordinal()]++;
			numArgs0++;
		}

		double numArgs1 = 0;
		int[] argCounter1 = new int[ArgumentType.values().length];
		for (RelationalArgument relArg : arg1.getRelationalArguments()) {
			argCounter1[relArg.getArgumentType().ordinal()]++;
			numArgs1++;
		}

		// Compare arg counts
		for (int i = 0; i < argCounter0.length; i++) {
			int result = Double.compare(argCounter0[i] / numArgs0, argCounter1[i] / numArgs1);
			if (result != 0)
				return -result;
		}
		
		// Compare by num arguments
		int result = Double.compare(numArgs0, numArgs1);
		if (result != 0)
			return result;
		
		// Compare by cond type
		int condType0 = BASE;
		int condType1 = BASE;

		// Checking str0
		if (arg0.getFactName().equals(StateSpec.GOALARGS_PRED))
			condType0 = GOAL;
		else if (StateSpec.getInstance().isTypePredicate(arg0.getFactName()))
			condType0 = TYPE;
		else if (arg0.getFactName().equals("test"))
			condType0 = INEQ;
		if (arg0.isNegated())
			condType0 = NEG;

		// Checking str1
		if (arg1.getFactName().equals(StateSpec.GOALARGS_PRED))
			condType1 = GOAL;
		else if (StateSpec.getInstance().isTypePredicate(arg1.getFactName()))
			condType1 = TYPE;
		else if (arg1.getFactName().equals("test"))
			condType1 = INEQ;
		if (arg1.isNegated())
			condType1 = NEG;

		if (condType0 < condType1)
			return -1;
		if (condType0 > condType1)
			return 1;
		return arg0.compareTo(arg1, false);
	}

	public static Comparator<RelationalPredicate> getInstance() {
		if (instance_ == null)
			instance_ = new ConditionComparator();
		return instance_;
	}
}