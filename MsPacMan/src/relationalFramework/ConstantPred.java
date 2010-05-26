package relationalFramework;

import java.util.ArrayList;
import java.util.Arrays;
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
	private ArrayList<String> facts_;

	public ConstantPred(String fact) {
		facts_ = new ArrayList<String>();
		facts_.add(StateSpec.splitFact(fact)[0]);
	}

	public ConstantPred(Collection<String> facts) {
		facts_ = new ArrayList<String>();
		for (String fact : facts)
			facts_.add(StateSpec.splitFact(fact)[0]);
		Collections.sort(facts_);
	}

	public ArrayList<String> getFacts() {
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
		if (facts_.size() == 1)
			return facts_.get(0);
		return facts_.toString();
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
