/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/relationalFramework/agentObservations/InvariantObservations.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package relationalFramework.agentObservations;

import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
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

	/** The generalised 'existence' variants (sometimes present, sometimes not). */
	private Collection<String> generalVariants_;

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
	public boolean noteAllInvariants(
			Collection<RelationalPredicate> stateFacts,
			Collection<String> generalStateFacts) {
		// Note specific invariants
		boolean result = noteSpecificInvariants(stateFacts);

		// If the first pass, invariants are just the state.
		if (generalInvariants_ == null) {
			generalInvariants_ = new TreeSet<String>(generalStateFacts);
			generalVariants_ = new TreeSet<String>();
			neverPresent_.removeAll(generalStateFacts);
			return true;
		}

		// Otherwise, perform a basic retainAll operation
		generalVariants_.addAll(generalStateFacts);
		generalVariants_.addAll(generalInvariants_);
		result |= generalInvariants_.retainAll(generalStateFacts);
		generalVariants_.removeAll(generalInvariants_);
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
	public boolean noteSpecificInvariants(
			Collection<RelationalPredicate> stateFacts) {
		counter_++;
		if (specificInvariants_ == null) {
			specificInvariants_ = new TreeSet<RelationalPredicate>(stateFacts);
			return true;
		}

		boolean result = specificInvariants_.retainAll(stateFacts);
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
		StringBuffer buffer = new StringBuffer();
		if (specificInvariants_ == null) {
			buffer.append("NONE RECORDED");
			return buffer.toString();
		}

		buffer.append("Specific Invariants: " + specificInvariants_.toString());
		if (generalInvariants_ != null)
			buffer.append("\n" + "General Invariants: "
					+ generalInvariants_.toString());
		if (generalVariants_ != null)
			buffer.append("\n" + "General Variants: "
					+ generalVariants_.toString());
		buffer.append("\n" + "Never Present: " + neverPresent_.toString());
		buffer.append("\n" + "Invariants count: " + counter_);
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

	/**
	 * Intersects the action conditions sets, handling numerical values as a
	 * special case.
	 * 
	 * @param actionConds
	 *            The action conditions being added.
	 * @param invariants
	 *            Invariant action conditions. Can only get smaller.
	 * @param variants
	 *            Variant action conditions. Can only get bigger.
	 * 
	 * @return True if the action conditions changed, false otherwise.
	 */
	public static boolean intersectActionConditions(
			Collection<RelationalPredicate> actionConds,
			Collection<RelationalPredicate> invariants,
			Collection<RelationalPredicate> variants) {
		boolean changed = false;
		
		Collection<RelationalPredicate> mergeState = new HashSet<RelationalPredicate>();
		Collection<RelationalPredicate> remainFacts = new HashSet<RelationalPredicate>();
		for (RelationalPredicate oldFact : invariants) {
			// Simple contains check
			if (actionConds.contains(oldFact)) {
				mergeState.add(oldFact);
				actionConds.remove(oldFact);
			} else {
				remainFacts.add(oldFact);
				changed = true;
			}
		}

		invariants.clear();
		invariants.addAll(mergeState);
		variants.addAll(remainFacts);
		return changed;
	}

	public Collection<String> getGeneralVariants() {
		return generalVariants_;
	}
}
