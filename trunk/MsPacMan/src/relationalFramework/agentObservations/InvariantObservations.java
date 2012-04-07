package relationalFramework.agentObservations;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import cerrla.Unification;
import cerrla.UnifiedFact;

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

		// // Run through each fact, only noting it down if the replacement
		// applies
		// // to it.
		// Collection<RelationalPredicate> goalFacts = new
		// TreeSet<RelationalPredicate>();
		// for (RelationalPredicate stateFact : stateFacts) {
		// RelationalPredicate checkFact = new RelationalPredicate(stateFact);
		// if (checkFact.replaceArguments(goalReplacements, false, false)) {
		// RelationalPredicate replFact = new RelationalPredicate(
		// stateFact);
		// replFact.replaceArguments(goalReplacements, true, false);
		// if (counter_ == 1)
		// specificInvariants_.add(replFact);
		// goalFacts.add(replFact);
		// }
		// }
		//
		// if (counter_ == 1)
		// return true;
		// boolean result = specificInvariants_.retainAll(goalFacts);
		// if (result)
		// expandedSpecificInvariants_ = null;
		// return result;
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
	 * @param newAction
	 *            The action currently being used for intersection. Possibly
	 *            null (doesn't matter).
	 * @param oldAction
	 *            The old unified action to be modified. Possibly null (doesn't
	 *            matter).
	 * @param actionConds
	 *            The action conditions being added.
	 * @param invariants
	 *            Invariant action conditions. Can only get smaller.
	 * @param variants
	 *            Variant action conditions. Can only get bigger.
	 * @param actionRanges
	 *            The observed ranges for various conditions.
	 * @param goalReplacements
	 *            The (possibly null) replacements for the goal variables.
	 * @return True if the action conditions changed, false otherwise.
	 */
	public static boolean intersectActionConditions(
			RelationalPredicate newAction, RelationalPredicate oldAction,
			Collection<RelationalPredicate> actionConds,
			Collection<RelationalPredicate> invariants,
			Collection<RelationalPredicate> variants,
			Map<RangeContext, double[]> actionRanges,
			Map<RelationalArgument, RelationalArgument> goalReplacements) {
		boolean changed = false;

		// Generalise the action if necessary
		RelationalArgument[] actionArgs = (oldAction != null) ? oldAction
				.getRelationalArguments() : new RelationalArgument[0];
		BidiMap replacementMap = new DualHashBidiMap();
		// Add the goal replacements
		if (goalReplacements != null) {
			for (RelationalArgument goalArg : goalReplacements.values())
				replacementMap.put(goalArg, goalArg);
		}

		for (int i = 0; i < actionArgs.length; i++) {
			RelationalArgument argument = actionArgs[i];
			RelationalArgument variableArg = RelationalArgument
					.createVariableTermArg(i);
			replacementMap.put(variableArg, variableArg);

			// If the action isn't variable, but doesn't match with the
			// current action, generalise it.
			if (!argument.isVariable()
					&& !argument.isNumber()
					&& (!argument.equals(newAction.getRelationalArguments()[i]))) {
				actionArgs[i] = variableArg;
				changed = true;
			}
		}

		// Run through each invariant fact
		Collection<RelationalPredicate> variantActionConds = new HashSet<RelationalPredicate>(
				actionConds);
		int result = Unification.getInstance().rlggUnification(invariants,
				variantActionConds, replacementMap, actionArgs);
		if (result == Unification.UNIFIED_CHANGE)
			changed = true;
		else if (result == Unification.CANNOT_UNIFY) {
			// No invariants can be unified, they are all variants
			variantActionConds.addAll(invariants);
			invariants.clear();
		}

		// Note ranges
		for (RelationalPredicate invFact : invariants) {
			if (invFact.isNumerical()) {
				noteRanges(oldAction.getFactName(), invFact, actionRanges);
			}
		}

		// Add any remaining action conds to the variants, merging any
		// numerical ranges together
		for (RelationalPredicate varFact : variantActionConds) {
			// Anonymise the fact and add it
			varFact.replaceUnboundWithAnonymous();
			if (varFact.isNumerical()) {
				// If numerical, attempt to unify
				Collection<UnifiedFact> mergedFacts = Unification.getInstance()
						.unifyFactToState(varFact, variants,
								new DualHashBidiMap(),
								new RelationalArgument[0], false);
				// Should only merge with one fact (if at all)
				if (!mergedFacts.isEmpty()) {
					UnifiedFact unifiedFact = mergedFacts.iterator().next();
					varFact = unifiedFact.getResultFact();
					if (oldAction != null) {
						noteRanges(oldAction.getFactName(), varFact,
								actionRanges);
					}
					// If the range changed, update the variant value
					if (!varFact.equals(unifiedFact.getUnityFact())) {
						changed = true;
						variants.remove(unifiedFact.getUnityFact());
					}
				}
			}
			changed |= variants.add(varFact);
		}

		if (changed && oldAction != null)
			oldAction.setArguments(actionArgs);
		return changed;
	}

	/**
	 * Notes the range from a given fact.
	 * 
	 * @param actionName
	 *            The name of the action being noted.
	 * @param numberFact
	 *            The range fact.
	 * @param actionRanges
	 *            The map of facts, mapped by factName-range variable.
	 * @return The range context for the fact if the range beinbg noted is >=
	 *         the existing noted range. Else, null.
	 */
	private static Collection<RangeContext> noteRanges(String actionName,
			RelationalPredicate numberFact,
			Map<RangeContext, double[]> actionRanges) {
		Collection<RangeContext> result = new HashSet<RangeContext>();
		if (actionRanges == null || !numberFact.isNumerical())
			return null;
		RelationalArgument[] factArgs = numberFact.getRelationalArguments();
		for (int i = 0; i < factArgs.length; i++) {
			// Only note ranges
			if (factArgs[i].isRange(false)) {
				RangeContext context = new RangeContext(i, numberFact,
						actionName);
				double[] range = actionRanges.get(result);
				double[] explicitRange = factArgs[i].getExplicitRange();
				if (range == null) {
					range = new double[2];
					System.arraycopy(explicitRange, 0, range, 0, 2);
					actionRanges.put(context, range);
				}
				// Check that the range is bigger - only return if same or
				// bigger
				if (explicitRange[0] <= range[0]
						&& explicitRange[1] >= range[1]) {
					System.arraycopy(explicitRange, 0, range, 0, 2);
					result.add(context);
				}
			}
		}
		return result;
	}

	public Collection<String> getGeneralVariants() {
		return generalVariants_;
	}
}
