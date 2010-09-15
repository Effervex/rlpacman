package relationalFramework;

import java.util.Comparator;

/**
 * A comparator for comparing conditions within a rule. The comparator performs
 * such that base conditions come first, then inequality predicates, then type
 * predicates.
 * 
 * @author Sam Sarjant
 */
public class ConditionComparator<T> implements Comparator<T> {
	private static Comparator<String> instance_;
	private final int BASE = 0;
	private final int INEQ = 2;
	private final int TYPE = 1;
	
	private ConditionComparator() {
		
	}

	@Override
	public int compare(T arg0, T arg1) {
		if (arg0 == null || !(arg0 instanceof String)) {
			if (arg1 == null || !(arg1 instanceof String))
				return 0;
			else
				return 1;
		} else if (arg1 == null || !(arg1 instanceof String)) {
			return -1;
		}

		// Determine the type of condition
		String str0 = (String) arg0;
		String str1 = (String) arg1;
		String[] split0 = StateSpec.splitFact(str0);
		String[] split1 = StateSpec.splitFact(str1);
		int condType0 = BASE;
		int condType1 = BASE;

		if (StateSpec.getInstance().isTypePredicate(split0[0]))
			condType0 = TYPE;
		else if (split0[0].equals("test"))
			condType0 = INEQ;

		if (StateSpec.getInstance().isTypePredicate(split1[0]))
			condType1 = TYPE;
		else if (split1[0].equals("test"))
			condType1 = INEQ;

		if (condType0 < condType1)
			return -1;
		if (condType0 > condType1)
			return 1;
		return str0.compareTo(str1);
	}

	public static Comparator<String> getInstance() {
		if (instance_ == null)
			instance_ = new ConditionComparator<String>();
		return instance_;
	}
}
