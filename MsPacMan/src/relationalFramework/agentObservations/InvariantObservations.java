package relationalFramework.agentObservations;

import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;

/**
 * A class for noting the perceived invariant observations the agent observes.
 * These invariants are used to simplify rules and modular learning.
 * 
 * @author Sam Sarjant
 */
public class InvariantObservations implements Serializable {
	private static final long serialVersionUID = 6532453706652428545L;

	/** The specific predicates (fully fleshed out preds). */
	private Collection<RelationalPredicate> specificInvariants_;

	/**
	 * The specific invariants note only purely instantiated invariants, but not
	 * fully anonymous versions of them are also invariants.
	 */
	private transient Collection<RelationalPredicate> expandedSpecificInvariants_;

	/**
	 * The generalised 'existence' invariants (only noting if a predicate is
	 * always present, regardless of its arguments).
	 */
	private Collection<String> generalInvariants_;

	/** The predicates that have never been observed to be present. */
	private Collection<String> neverPresent_;

	/** A counter for the number of times the invariants have been scanned. */
	private int counter_ = 0;

	public InvariantObservations() {
		// Initialise the never present invariants
		neverPresent_ = new TreeSet<String>();
		neverPresent_.addAll(StateSpec.getInstance().getPredicates().keySet());
		neverPresent_.addAll(StateSpec.getInstance().getTypePredicates()
				.keySet());
	}

	/**
	 * Note the state down and renew the invariants.
	 * 
	 * @param stateFacts
	 *            The facts of the state.
	 * @return True if the invariants collections changed at all.
	 */
	public boolean noteInvariants(Collection<RelationalPredicate> stateFacts,
			Collection<String> generalStateFacts) {
		// If the first pass, invariants are just the state.
		counter_++;
		if (specificInvariants_ == null) {
			specificInvariants_ = new TreeSet<RelationalPredicate>(stateFacts);
			generalInvariants_ = new TreeSet<String>(generalStateFacts);
			neverPresent_.removeAll(generalStateFacts);
			return true;
		}

		// Otherwise, perform a basic retainAll operation
		boolean result = specificInvariants_.retainAll(stateFacts);
		if (result)
			expandedSpecificInvariants_ = null;
		result |= generalInvariants_.retainAll(generalStateFacts);
		result |= neverPresent_.removeAll(generalStateFacts);
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
	public boolean noteInvariants(Collection<RelationalPredicate> stateFacts,
			Map<String, String> goalReplacements) {
		counter_++;
		if (specificInvariants_ == null) {
			specificInvariants_ = new TreeSet<RelationalPredicate>();
		}

		// Run through each fact, only noting it down if the replacement applies
		// to it.
		Collection<RelationalPredicate> goalFacts = new TreeSet<RelationalPredicate>();
		for (RelationalPredicate stateFact : stateFacts) {
			RelationalPredicate checkFact = new RelationalPredicate(stateFact);
			if (checkFact.replaceArguments(goalReplacements, false, false)) {
				RelationalPredicate replFact = new RelationalPredicate(
						stateFact);
				replFact.replaceArguments(goalReplacements, true, false);
				if (counter_ == 1)
					specificInvariants_.add(replFact);
				goalFacts.add(replFact);
			}
		}

		if (counter_ == 1)
			return true;
		boolean result = specificInvariants_.retainAll(goalFacts);
		if (result)
			expandedSpecificInvariants_ = null;
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
		result = prime * result
				+ ((neverPresent_ == null) ? 0 : neverPresent_.hashCode());
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
		if (neverPresent_ == null) {
			if (other.neverPresent_ != null)
				return false;
		} else if (!neverPresent_.equals(other.neverPresent_))
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
		buffer.append("\n" + "Never Present: " + neverPresent_.toString());
		return buffer.toString();
	}

	public Collection<RelationalPredicate> getSpecificInvariants() {
		if (specificInvariants_ == null)
			return new TreeSet<RelationalPredicate>();
		// Return the expanded invariants
		if (expandedSpecificInvariants_ == null) {
			expandedSpecificInvariants_ = new TreeSet<RelationalPredicate>();
			for (RelationalPredicate invariant : specificInvariants_) {
				expandedSpecificInvariants_.add(invariant);
				expandedSpecificInvariants_.addAll(invariant.createSubFacts(
						false, false));
			}
		}

		return expandedSpecificInvariants_;
	}

	public Collection<String> getNeverSeenPredicates() {
		return neverPresent_;
	}

	public Collection<String> getGeneralInvariants() {
		return generalInvariants_;
	}
}
