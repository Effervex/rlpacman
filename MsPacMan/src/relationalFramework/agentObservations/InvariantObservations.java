package relationalFramework.agentObservations;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;

import relationalFramework.StringFact;

/**
 * A class for noting the perceived invariant observations the agent observes.
 * These invariants are used to simplify rules and modular learning.
 * 
 * @author Sam Sarjant
 */
public class InvariantObservations implements Serializable {
	private static final long serialVersionUID = 6532453706652428545L;

	/** The specific predicates (fully fleshed out preds). */
	private Collection<StringFact> specificInvariants_;

	/**
	 * The generalised 'existence' invariants (only noting if a predicate is
	 * always present, regardless of its arguments).
	 */
	private Collection<String> generalInvariants_;

	/** A counter for the number of times the invariants have been scanned. */
	private int counter_ = 0;

	/**
	 * Note the state down and renew the invariants.
	 * 
	 * @param stateFacts
	 *            The facts of the state.
	 * @return True if the invariants collections changed at all.
	 */
	public boolean noteInvariants(Collection<StringFact> stateFacts,
			Collection<String> generalStateFacts) {
		// If the first pass, invariants are just the state.
		counter_++;
		if (specificInvariants_ == null) {
			specificInvariants_ = new TreeSet<StringFact>(stateFacts);
			generalInvariants_ = new TreeSet<String>(generalStateFacts);
			return true;
		}

		// Otherwise, perform a basic retainAll operation
		boolean result = specificInvariants_.retainAll(stateFacts);
		result |= generalInvariants_.retainAll(generalStateFacts);
		return result;
	}

	/**
	 * Note the goal terms in the state down using the given replacements and
	 * renew the invariants.
	 * 
	 * @param stateFacts
	 *            The state facts (to be replaced).
	 * @param goalReplacements
	 *            The goal replacement terms.
	 * @return True if the invariants collection changed at all.
	 */
	public boolean noteInvariants(Collection<StringFact> stateFacts,
			Map<String, String> goalReplacements) {
		counter_++;
		if (specificInvariants_ == null) {
			specificInvariants_ = new TreeSet<StringFact>();
		}

		// Run through each fact, only noting it down if the replacement applies
		// to it.
		Collection<StringFact> goalFacts = new TreeSet<StringFact>();
		for (StringFact stateFact : stateFacts) {
			StringFact checkFact = new StringFact(stateFact);
			if (checkFact.replaceArguments(goalReplacements, false)) {
				StringFact replFact = new StringFact(stateFact);
				replFact.replaceArguments(goalReplacements, true);
				if (counter_ == 1)
					specificInvariants_.add(replFact);
				goalFacts.add(replFact);
			}
		}

		if (counter_ == 1)
			return true;
		return specificInvariants_.retainAll(goalFacts);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((generalInvariants_ == null) ? 0 : generalInvariants_
						.hashCode());
		result = prime
				* result
				+ ((specificInvariants_ == null) ? 0 : specificInvariants_
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InvariantObservations other = (InvariantObservations) obj;
		if (generalInvariants_ == null) {
			if (other.generalInvariants_ != null)
				return false;
		} else if (!generalInvariants_.equals(other.generalInvariants_))
			return false;
		if (specificInvariants_ == null) {
			if (other.specificInvariants_ != null)
				return false;
		} else if (!specificInvariants_.equals(other.specificInvariants_))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer("Invariants (" + counter_
				+ " counts):\n");
		if (specificInvariants_ == null) {
			buffer.append("NONE RECORDED");
			return buffer.toString();
		}

		buffer.append("Specific: " + specificInvariants_.toString());
		if (generalInvariants_ != null)
			buffer.append("\n" + "General: " + generalInvariants_.toString());
		return buffer.toString();
	}

	public Collection<StringFact> getSpecificInvariants() {
		if (specificInvariants_ == null)
			return new TreeSet<StringFact>();
		return specificInvariants_;
	}
}
