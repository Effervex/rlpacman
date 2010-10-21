package relationalFramework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * A small class representing a constant predicate (or predicates) found in a
 * rule. Note that in the case of multiple constant facts, all are considered
 * together.
 * 
 * @author Samuel J. Sarjant
 */
public class ConstantPred implements Comparable<ConstantPred> {
	/** A sorted list of facts using only constant terms */
	private ArrayList<StringFact> facts_;

	public ConstantPred(StringFact fact) {
		facts_ = new ArrayList<StringFact>();
		facts_.add(fact);
	}

	public ConstantPred(Collection<StringFact> facts) {
		facts_ = new ArrayList<StringFact>();
		for (StringFact fact : facts)
			facts_.add(fact);
		Collections.sort(facts_);
	}

	public ArrayList<StringFact> getFacts() {
		return facts_;
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj != null) && (obj instanceof ConstantPred)) {
			ConstantPred fact = (ConstantPred) obj;
			if (facts_.containsAll(fact.facts_)
					&& fact.facts_.containsAll(facts_))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return facts_.hashCode();
	}

	@Override
	public String toString() {
		return Module.formName(facts_);
	}

	@Override
	public int compareTo(ConstantPred arg0) {
		if (facts_.size() < arg0.facts_.size())
			return -1;
		if (facts_.size() > arg0.facts_.size())
			return 1;

		for (int i = 0; i < facts_.size(); i++) {
			int value = facts_.get(i).compareTo(arg0.facts_.get(i));
			if (value != 0)
				return value;
		}
		return 0;
	}
}
