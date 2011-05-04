package relationalFramework.agentObservations;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

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
			specificInvariants_ = new HashSet<StringFact>(stateFacts);
			generalInvariants_ = new HashSet<String>(generalStateFacts);
			return true;
		}

		// Otherwise, perform a basic retainAll operation
		boolean result = specificInvariants_.retainAll(stateFacts);
		result |= generalInvariants_.retainAll(generalStateFacts);
		return result;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer("Invariants (" + counter_
				+ " counts):\n");
		buffer.append("Specific: " + specificInvariants_.toString() + "\n");
		buffer.append("General: " + generalInvariants_.toString());
		return buffer.toString();
	}
}
