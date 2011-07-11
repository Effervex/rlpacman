package relationalFramework.util;

import java.io.Serializable;
import java.util.Comparator;

import relationalFramework.StateSpec;
import relationalFramework.RelationalPredicate;

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
	private final int INEQ = 2;
	private final int TYPE = 1;

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

		// Determine the type of condition
		RelationalPredicate str0 = (RelationalPredicate) arg0;
		RelationalPredicate str1 = (RelationalPredicate) arg1;
		int condType0 = BASE;
		int condType1 = BASE;

		// Checking str0
		if (str0.getFactName().equals(StateSpec.GOALARGS_PRED))
			condType0 = GOAL;
		else if (StateSpec.getInstance().isTypePredicate(str0.getFactName()))
			condType0 = TYPE;
		else if (str0.getFactName().equals("test"))
			condType0 = INEQ;

		// Checking str1
		if (str1.getFactName().equals(StateSpec.GOALARGS_PRED))
			condType1 = GOAL;
		else if (StateSpec.getInstance().isTypePredicate(str1.getFactName()))
			condType1 = TYPE;
		else if (str1.getFactName().equals("test"))
			condType1 = INEQ;

		if (condType0 < condType1)
			return -1;
		if (condType0 > condType1)
			return 1;
		return str0.compareTo(str1);
	}

	public static Comparator<RelationalPredicate> getInstance() {
		if (instance_ == null)
			instance_ = new ConditionComparator();
		return instance_;
	}
}
