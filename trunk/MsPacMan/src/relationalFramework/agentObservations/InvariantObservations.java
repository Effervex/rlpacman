package relationalFramework.agentObservations;

import java.io.Serializable;
import java.util.Collection;
import java.util.TreeSet;

import relationalFramework.StringFact;

/**
 * A class for noting the perceived invariant observations the agent observes.
 * These invariants are used to simplify rules and modular learning.
 * 
 * @author Sam Sarjant
 */
public class InvariantObservations implements Serializable {
	private static final long serialVersionUID = -3315975123179700215L;

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
			
		buffer.append("Specific: " + specificInvariants_.toString() + "\n");
		buffer.append("General: " + generalInvariants_.toString());
		return buffer.toString();
	}

	public Collection<StringFact> getSpecificInvariants() {
		if (specificInvariants_ == null)
			return new TreeSet<StringFact>();
		return specificInvariants_;
	}
}
