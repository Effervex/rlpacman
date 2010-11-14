package relationalFramework;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A comparator for comparing conditions within a rule. The comparator performs
 * such that base conditions come first, then inequality predicates, then type
 * predicates.
 * 
 * @author Sam Sarjant
 */
public class ConditionComparator<T> implements Comparator<T>, Serializable {
	private static final long serialVersionUID = -357269694510525864L;
	private static Comparator<StringFact> instance_;
	private final int BASE = 0;
	private final int INEQ = 2;
	private final int TYPE = 1;
	
	private ConditionComparator() {
		
	}

	@Override
	public int compare(T arg0, T arg1) {
		if (arg0 == null || !(arg0 instanceof StringFact)) {
			if (arg1 == null || !(arg1 instanceof StringFact))
				return 0;
			else
				return 1;
		} else if (arg1 == null || !(arg1 instanceof StringFact)) {
			return -1;
		}

		// Determine the type of condition
		StringFact str0 = (StringFact) arg0;
		StringFact str1 = (StringFact) arg1;
		int condType0 = BASE;
		int condType1 = BASE;

		if (StateSpec.getInstance().isTypePredicate(str0.getFactName()))
			condType0 = TYPE;
		else if (str0.getFactName().equals("test"))
			condType0 = INEQ;

		if (StateSpec.getInstance().isTypePredicate(str1.getFactName()))
			condType1 = TYPE;
		else if (str1.getFactName().equals("test"))
			condType1 = INEQ;

		if (condType0 < condType1)
			return -1;
		if (condType0 > condType1)
			return 1;
		return str0.compareTo(str1);
	}

	public static Comparator<StringFact> getInstance() {
		if (instance_ == null)
			instance_ = new ConditionComparator<StringFact>();
		return instance_;
	}
}
