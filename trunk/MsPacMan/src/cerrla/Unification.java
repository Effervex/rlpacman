package cerrla;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

public class Unification {
	/** The cost of a fact not unifying. */
	private static final int NO_FACT_UNIFY = 1000;

	/** The incrementing unique index of ranged values. */
	private int rangeIndex_;

	/** The singleton instance. */
	private static Unification instance_;

	private Unification() {
		resetRangeIndex();
	}

	/**
	 * Unifies two states together but in this method neither state has terms
	 * affiliated with it. This method attempts to unify the new state with the
	 * old state by dynamically replacing terms with old state terms for
	 * matching purposes. Note that the terms may not be replaced in a manner
	 * that maximises conditions unified, but if terms are replaced, then at
	 * least one condition will be unified with.
	 * 
	 * @param oldState
	 *            The old state to unify with.
	 * @param newState
	 *            The flexible new state which can have terms replaced.
	 * @param replacementMap
	 *            The replacement map to fill with RelationalArguments.
	 * @return -1 if the states cannot unify, 0 if they unified perfectly, 1 if
	 *         they unified but the old state was changed. In the two latter
	 *         cases, the replacement map will contain replacement terms.
	 */
	public int unifyStates(Collection<RelationalPredicate> oldState,
			Collection<RelationalPredicate> newState, BidiMap replacementMap) {
		Collection<RelationalPredicate> oldStateClone = new HashSet<RelationalPredicate>(
				oldState);
		recursivelyUnify(oldState, newState, new RelationalArgument[0],
				replacementMap);

		if (oldState.isEmpty()) {
			oldState.addAll(oldStateClone);
			return -1;
		} else if (oldState.containsAll(oldStateClone)
				&& oldStateClone.containsAll(oldState))
			return 0;
		else
			return 1;
	}

	/**
	 * Recursively unifies two states together in a fact-by-fact basis, starting
	 * different recursion paths if necessary.
	 * 
	 * @param oldState
	 *            The old state to unify with. This is modified in the process.
	 * @param newState
	 *            The new state for use in unification.
	 * @param oldTerms
	 *            The terms the old state uses. This is modified in the process.
	 * @param newReplacementMap
	 *            The replacement map for the new state.
	 * @param flexibleReplacement
	 *            If the process can create flexible replacements.
	 * @return The generalisation value for this recursive unification.
	 */
	@SuppressWarnings("unchecked")
	private int recursivelyUnify(Collection<RelationalPredicate> oldState,
			Collection<RelationalPredicate> newState,
			RelationalArgument[] oldTerms, BidiMap newReplacementMap) {
		// Base condition.
		if (oldState.isEmpty())
			return 0;

		// For each item in the old state, see if it is present in the new state
		for (RelationalPredicate oldStateFact : oldState) {
			Collection<UnifiedFact> modFacts = unifyFact(oldStateFact,
					newState, newReplacementMap, oldTerms, true, false);

			// If the fact didn't unify, remove it and continue the recursion.
			if (modFacts.isEmpty()) {
				Collection<RelationalPredicate> reducedOldState = new HashSet<RelationalPredicate>(
						oldState);
				reducedOldState.remove(oldStateFact);

				int generalisationValue = recursivelyUnify(reducedOldState,
						newState, oldTerms, newReplacementMap) + NO_FACT_UNIFY;
				oldState.clear();
				oldState.addAll(reducedOldState);
				return generalisationValue;
			} else {
				// Recursively step into each fact unification path
				int bestGeneralisation = Integer.MAX_VALUE;
				Collection<RelationalPredicate> bestOldState = null;
				RelationalArgument[] bestOldTerms = null;
				BidiMap bestReplacements = null;
				for (UnifiedFact modFact : modFacts) {
					Collection<RelationalPredicate> reducedNewState = new HashSet<RelationalPredicate>(
							newState);
					reducedNewState.remove(modFact.getUnityFact());
					Collection<RelationalPredicate> reducedOldState = new HashSet<RelationalPredicate>(
							oldState);
					reducedOldState.remove(oldStateFact);

					RelationalArgument[] recursiveOldTerms = modFact
							.getFactTerms();
					BidiMap recursiveReplacements = modFact
							.getResultReplacements();

					int generalisationValue = recursivelyUnify(reducedOldState,
							newState, recursiveOldTerms, recursiveReplacements)
							+ modFact.getGeneralisation();

					// Note the best unification
					if (generalisationValue < bestGeneralisation) {
						bestGeneralisation = generalisationValue;
						bestOldState = reducedOldState;
						bestOldState.add(modFact.getResultFact());
						bestOldTerms = recursiveOldTerms;
						bestReplacements = recursiveReplacements;
					}
				}

				// Apply the best values to the parameters
				oldState.clear();
				oldState.addAll(bestOldState);
				System.arraycopy(bestOldTerms, 0, oldTerms, 0,
						bestOldTerms.length);
				newReplacementMap.putAll(bestReplacements);
				return bestGeneralisation;
			}
		}

		return Integer.MAX_VALUE;
	}

	/**
	 * Unifies a single fact by searching for the term itself or a generalised
	 * form of the term within a collection. The search is special because it
	 * can generalise terms to anonymous terms if necessary for unification. It
	 * can also introduce new range terms for numerical unification.
	 * 
	 * @param fact
	 *            The fact being searched for in the unity collection.
	 * @param unityFacts
	 *            The collection of facts to search through for unification.
	 * @param unityReplacementMap
	 *            The replacement map to apply to the unity collection. If null,
	 *            then the replacements are ignored.
	 * @param actionTerms
	 *            The terms of the action. (possibly null).
	 * @param flexibleReplacement
	 *            If the replacement maps can be filled dynamically to match
	 *            terms.
	 * @param noGeneralisations
	 *            If the unification process doesn't allow generalisation - only
	 *            term swap or range split.
	 * @return The unified version of the fact (possibly more general than the
	 *         input fact) or null if no unification.
	 */
	@SuppressWarnings("unchecked")
	public Collection<UnifiedFact> unifyFact(RelationalPredicate fact,
			Collection<RelationalPredicate> unityFacts,
			BidiMap unityReplacementMap, RelationalArgument[] actionTerms,
			boolean flexibleReplacement, boolean noGeneralisations) {
		Collection<UnifiedFact> unifiedFacts = new HashSet<UnifiedFact>();

		RelationalArgument[] newActionTerms = null;
		if (actionTerms == null)
			newActionTerms = new RelationalArgument[0];
		else {
			newActionTerms = new RelationalArgument[actionTerms.length];
			System.arraycopy(actionTerms, 0, newActionTerms, 0,
					actionTerms.length);
		}

		// Maintain a check on what is the best unification (should better ones
		// be present)
		int generalisation = Integer.MAX_VALUE;

		// Check every fact in the unity facts.
		for (RelationalPredicate unityFact : unityFacts) {
			// Check it if the same fact
			boolean sameFact = fact.getFactName().equals(
					unityFact.getFactName())
					&& (fact.isNegated() == unityFact.isNegated());

			// If dealing with the same fact
			if (sameFact) {
				// If dealing with negated facts, ensure both are the same, but
				// do not allow generalisations. (!abv(X,Y) != !abv(X,?)
				if (fact.isNegated()) {
					noGeneralisations = true;
				}

				RelationalArgument[] factArguments = fact
						.getRelationalArguments();
				RelationalArgument[] unityArguments = unityFact
						.getRelationalArguments();
				RelationalArgument[] unification = new RelationalArgument[factArguments.length];
				int thisGeneralness = 0;
				boolean validFact = false;
				BidiMap tempUnityReplacementMap = new DualHashBidiMap();

				// Unify each term
				for (int i = 0; i < factArguments.length; i++) {
					// Apply the replacements
					RelationalArgument factTerm = factArguments[i];
					RelationalArgument unityTerm = findUnityReplacement(
							unityReplacementMap, unityArguments[i],
							flexibleReplacement, tempUnityReplacementMap,
							factTerm, noGeneralisations);

					// If the unity term was ununifiable, the fact is
					// ununifiable
					if (unityTerm == null) {
						validFact = false;
						break;
					}

					// If the fact and unity term are the same, great! So far,
					// so good
					if (factTerm.equals(unityTerm)) {
						unification[i] = factTerm;
						validFact |= !factTerm
								.equals(RelationalArgument.ANONYMOUS);
					} else if (!numericalValueCheck(factArguments[i],
							unityArguments[i], unification, i, newActionTerms)) {
						// Not a number, we have differing terms here. Use
						// anonymous
						if (noGeneralisations) {
							// If anonymous generalisation is not allowed, break
							validFact = false;
							break;
						}
						unification[i] = RelationalArgument.ANONYMOUS;
						thisGeneralness++;
						// Check if the term was an action term
						for (RelationalArgument actionTerm : newActionTerms) {
							if (factTerm.equals(actionTerm))
								thisGeneralness++;
						}
					}
				}

				// Store if:
				// 1. The fact is not fully anonymous
				// 2. The fact is less general than the current best
				if (validFact) {
					if (thisGeneralness < generalisation) {
						generalisation = thisGeneralness;
						unifiedFacts.clear();
					}
					if (thisGeneralness == generalisation) {
						RelationalPredicate unifact = new RelationalPredicate(
								fact, unification, fact.isNegated());
						if (unityReplacementMap != null)
							tempUnityReplacementMap.putAll(unityReplacementMap);
						UnifiedFact unifiedFact = new UnifiedFact(unifact,
								newActionTerms, unityFact,
								tempUnityReplacementMap, thisGeneralness);
						unifiedFacts.add(unifiedFact);
					}
				}
			}
		}

		return unifiedFacts;
	}

	/**
	 * A method which finds the replacement term for a unity variable if there
	 * is one available. If the terms are flexible though, a replacement term
	 * could be created to match the fact term.
	 * 
	 * @param unityReplacementMap
	 *            The mapping for terms to be replaced.
	 * @param unityTerm
	 *            The term to possibly be replaced.
	 * @param flexibleReplacement
	 *            If allowing flexible replacements.
	 * @param tempUnityReplacementMap
	 *            The temporary replacement map to fill is a flexible
	 *            replacement is created.
	 * @param factTerm
	 *            The term the fact is using.
	 * @param noGeneralisations
	 *            If generalisations (using the anonymous variable) are not
	 *            allowed.
	 * @return The replacement term, or the same term if no replacement
	 *         used/created.
	 */
	private RelationalArgument findUnityReplacement(
			BidiMap unityReplacementMap, RelationalArgument unityTerm,
			boolean flexibleReplacement, BidiMap tempUnityReplacementMap,
			RelationalArgument factTerm, boolean noGeneralisations) {
		// CHECK THE REPLACEMENT MAPS FIRST
		// If the temp already has a replacement, then replace.
		if (tempUnityReplacementMap.containsKey(unityTerm))
			return (RelationalArgument) tempUnityReplacementMap.get(unityTerm);
		// If the unity map already has a replacement, then replace
		if (unityReplacementMap != null
				&& unityReplacementMap.containsKey(unityTerm))
			return (RelationalArgument) unityReplacementMap.get(unityTerm);

		// If we're allowing flexible replacements, create a replacement and use
		// the replacement (not for numbers)
		if (flexibleReplacement && !unityTerm.isNumber()) {
			// If the fact term hasn't already been mapped to another unity term
			if ((unityReplacementMap == null || !unityReplacementMap
					.containsValue(factTerm))
					&& !tempUnityReplacementMap.containsValue(factTerm)) {
				// If the fact isn't anonymous, assign it as the replacement
				if (!factTerm.equals(RelationalArgument.ANONYMOUS)
						&& !unityTerm.equals(RelationalArgument.ANONYMOUS)) {
					tempUnityReplacementMap.put(unityTerm, factTerm);
					return factTerm;
				}
				// If fact term is anonymous, it doesn't matter what the unity
				// term is.
				return unityTerm;
			}
			return null;
		}

		// Otherwise return the term.
		return unityTerm;
	}

	/**
	 * Check for numerical or numerical range values for a fact.
	 * 
	 * @param factValue
	 *            The term in the fact.
	 * @param unityValue
	 *            The term in the unity fact.
	 * @param unification
	 *            The unification between the two.
	 * @param index
	 *            The index of the unification to fill.
	 * @param factTerms
	 *            The terms the fact uses as action parameters.
	 * @return True if the two are numerical and could be unified into a ranged
	 *         variable.
	 */
	private boolean numericalValueCheck(RelationalArgument factValue,
			RelationalArgument unityValue, RelationalArgument[] unification,
			int index, RelationalArgument[] factTerms) {
		// Check the unity is a number
		if (!factValue.isNumber() || !unityValue.isNumber())
			return false;

		// Expand the range if need be.
		double min = Math.min(factValue.getRangeArg()[0],
				unityValue.getRangeArg()[0]);
		double max = Math.max(factValue.getRangeArg()[1],
				unityValue.getRangeArg()[0]);
		if (min == max || min == factValue.getRangeArg()[0] && max == factValue.getRangeArg()[1]) {
			unification[index] = factValue;
			return true;
		}

		String variable = (factValue.isVariable()) ? factValue.getStringArg()
				: "?" + RelationalArgument.RANGE_VARIABLE_PREFIX
						+ rangeIndex_++;

		RelationalArgument unifiedArg = new RelationalArgument(variable, min,
				max);
		unification[index] = unifiedArg;

		return true;
	}

	public void resetRangeIndex() {
		rangeIndex_ = 0;
	}

	public static Unification getInstance() {
		if (instance_ == null)
			instance_ = new Unification();
		return instance_;
	}
}
