package cerrla;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

public class Unification {
	/** The prefix for range variables. */
	public static final String RANGE_VARIABLE_PREFIX = "__Num";

	/** The incrementing unique index of ranged values. */
	private int rangeIndex_;

	/** The singleton instance. */
	private static Unification instance_;

	private Unification() {
		resetRangeIndex();
	}

	/**
	 * Unifies two states together. This is more than simply a retainAll
	 * operation, as it can also generalise constants into variables during the
	 * unification process. This process does not keep useless facts around
	 * (facts using only anonymous terms). It also removes facts known to
	 * already by true (on ?X ?Y) implies (on ?X ?) is also true.
	 * 
	 * This process performs special unification on numerical variables, by
	 * creating a range function under which the variables fall.
	 * 
	 * @param oldState
	 *            The old state to be unified with. May contain constants,
	 *            variables and anonymous values. This may be modified.
	 * @param newState
	 *            The new state being unified with. Will only contain constant
	 *            terms and facts.
	 * @param oldTerms
	 *            The terms for the actions on the old state. Should be for the
	 *            same action as newTerms. This may be modified.
	 * @param newTerms
	 *            The terms for the actions on the new state. Should be for the
	 *            same action as oldTerms.
	 * @return 1 if the old state changed from the unification, 0 if the state
	 *         remained the same, -1 if the states did not unify and returned
	 *         the empty set.
	 */
	public int unifyStates(Collection<RelationalPredicate> oldState,
			Collection<RelationalPredicate> newState, String[] oldTerms,
			String[] newTerms) {
		// If the terms don't match up, create a replacement map
		BidiMap oldReplacementMap = new DualHashBidiMap();
		BidiMap newReplacementMap = new DualHashBidiMap();
		if (!createReplacementMaps(oldTerms, newTerms, oldReplacementMap,
				newReplacementMap))
			return -1;

		return unifyStates(oldState, newState, oldTerms, false,
				oldReplacementMap, newReplacementMap);
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
	 *            The replacement map to fill.
	 * @return -1 if the states cannot unify, 0 if they unified perfectly, 1 if
	 *         they unified but the old state was changed. In the two latter
	 *         cases, the replacement map will contain replacement terms.
	 */
	public int unifyStates(Collection<RelationalPredicate> oldState,
			Collection<RelationalPredicate> newState, BidiMap replacementMap) {
		return unifyStates(oldState, newState, new String[0], true,
				new DualHashBidiMap(), replacementMap);
	}

	/**
	 * The actual unify state method used by the two outer methods. This method
	 * has a some guarantees:
	 * 
	 * The unified old state can only ever get more general - either by losing
	 * predicates or making terms more general. Which means that the number of
	 * preds in the post-unified state will only be <= the old state.
	 * 
	 * The unification process may not be the most ideal one, it is a single
	 * pass process, but it does try to unify with the most specific terms in
	 * the new state. Each term in the new state can only be unified with once.
	 * 
	 * @param oldState
	 *            The old state to be unified with.
	 * @param newState
	 *            The new state for unifying. May possibly have flexible terms.
	 * @param oldTerms
	 *            The terms used in the old state, if any.
	 * @param flexibleReplacement
	 *            If the replacement map can be modified dynamically to match
	 *            the old state.
	 * @param oldReplacementMap
	 *            The replacement map for the old state.
	 * @param newReplacementMap
	 *            The replacement map for the new state. May be flexible.
	 * @return -1 if no unification possible, 0 if perfect unification, 1 if
	 *         unification with old state changed.
	 */
	@SuppressWarnings("unchecked")
	private int unifyStates(Collection<RelationalPredicate> oldState,
			Collection<RelationalPredicate> newState, String[] oldTerms,
			boolean flexibleReplacement, BidiMap oldReplacementMap,
			BidiMap newReplacementMap) {
		// Copy the new state as it is going to be modified.
		newState = new ArrayList<RelationalPredicate>(newState);

		// For each item in the old state, see if it is present in the new state
		boolean hasChanged = false;
		List<RelationalPredicate> oldStateRepl = new ArrayList<RelationalPredicate>();
		for (RelationalPredicate oldStateFact : oldState) {
			RelationalPredicate modFact = unifyFact(oldStateFact, newState,
					oldReplacementMap, newReplacementMap, oldTerms,
					flexibleReplacement, false, false);

			// If the fact is null, then there was no unification.
			if (modFact == null)
				hasChanged = true;
			else {
				// Check for a change
				if (!oldStateFact.equals(modFact))
					hasChanged = true;

				if (!oldStateRepl.contains(modFact))
					oldStateRepl.add(modFact);

				// Remove the fact from the new state so it cannot be used for
				// unifying again.
				RelationalPredicate replFact = new RelationalPredicate(modFact);
				replFact.replaceArguments(newReplacementMap.inverseBidiMap(),
						true, false);
				newState.remove(replFact);
			}
		}

		// Replace the oldState
		if (!oldStateRepl.isEmpty()) {
			oldState.clear();
			oldState.addAll(oldStateRepl);
		}

		// Determine the return code.
		if (oldStateRepl.isEmpty())
			return -1;
		else if (hasChanged)
			return 1;
		else
			return 0;
	}

	/**
	 * Creates replacement maps if necessary. Also, if replacements are
	 * impossible, unification is impossible, so return false.
	 * 
	 * @param oldTerms
	 *            The old terms.
	 * @param newTerms
	 *            The new terms.
	 * @param oldReplacementMap
	 *            The replacement map for the old terms.
	 * @param newReplacementMap
	 *            The replacement map for the new terms.
	 * @return False if replacement is impossible, true otherwise.
	 */
	private boolean createReplacementMaps(String[] oldTerms, String[] newTerms,
			BidiMap oldReplacementMap, BidiMap newReplacementMap) {
		for (int i = 0; i < oldTerms.length; i++) {
			// If this index of terms don't match up
			String oldTerm = oldTerms[i];
			String newTerm = newTerms[i];
			// If the terms don't match and aren't numbers, create a replacement
			if (!oldTerm.equals(newTerm) && !StateSpec.isNumber(oldTerm)
					&& !StateSpec.isNumber(newTerm)) {
				boolean bothVariables = true;
				String variable = RuleCreation.getVariableTermString(i);
				// Replace old term if necessary
				if ((oldTerm.charAt(0) != '?')
						|| (oldTerm.contains(StateSpec.GOAL_VARIABLE_PREFIX))) {
					oldReplacementMap.put(oldTerm, variable);
					bothVariables = false;
				}
				if ((newTerm.charAt(0) != '?')
						|| (newTerm.contains(StateSpec.GOAL_VARIABLE_PREFIX))) {
					newReplacementMap.put(newTerm, variable);
					bothVariables = false;
				}

				// Check that both slots aren't inequal variables
				if (bothVariables) {
					return false;
				}
			}
		}
		return true;
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
	 * @param factReplacementMap
	 *            The replacement map to apply to the fact.
	 * @param unityReplacementMap
	 *            The replacement map to apply to the unity collection.
	 * @param factTerms
	 *            The terms of the action. To be modified, depending on the
	 *            resultant string.
	 * @param flexibleReplacement
	 *            If the replacement maps can be filled dynamically to match
	 *            terms.
	 * @param removeUnifiedFact
	 *            If the unityFact this fact unifies with should be removed from
	 *            the unityFacts.
	 * @param noGeneralisations
	 *            If the unification process doesn't allow generalisation - only
	 *            term swap or range split.
	 * @return The unified version of the fact (possibly more general than the
	 *         input fact) or null if no unification.
	 */
	@SuppressWarnings("unchecked")
	public RelationalPredicate unifyFact(RelationalPredicate fact,
			Collection<RelationalPredicate> unityFacts, BidiMap factReplacementMap,
			BidiMap unityReplacementMap, String[] factTerms,
			boolean flexibleReplacement, boolean removeUnifiedFact,
			boolean noGeneralisations) {
		RelationalPredicate result = null;
		RelationalPredicate unityResult = null;
		BidiMap resultReplacements = null;

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

				String[] factArguments = fact.getArguments();
				String[] unityArguments = unityFact.getArguments();
				String[] unification = new String[factArguments.length];
				int thisGeneralness = 0;
				boolean validFact = false;
				BidiMap tempUnityReplacementMap = new DualHashBidiMap();

				// Unify each term
				for (int i = 0; i < factArguments.length; i++) {
					// Apply the replacements
					String factTerm = (String) ((factReplacementMap
							.containsKey(factArguments[i])) ? factReplacementMap
							.get(factArguments[i]) : factArguments[i]);
					String unityTerm = findUnityReplacement(
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
						validFact |= !factTerm.equals(StateSpec.ANONYMOUS);
					} else if (!numericalValueCheck(factArguments[i],
							unityArguments[i], unification, i, factTerms)) {
						// Not a number, we have differing terms here. Use
						// anonymous
						if (noGeneralisations) {
							// If anonymous generalisation is not allowed, break
							validFact = false;
							break;
						}
						unification[i] = StateSpec.ANONYMOUS;
						thisGeneralness++;
					}
				}

				// Store if:
				// 1. The fact is not fully anonymous
				// 2. The fact is less general than the current best
				if (validFact) {
					if (thisGeneralness < generalisation) {
						generalisation = thisGeneralness;
					}
					if (thisGeneralness == generalisation) {
						RelationalPredicate unifact = new RelationalPredicate(fact, unification,
								fact.isNegated());
						result = unifact;
						unityResult = unityFact;

						// If we're using flexible replacements, store any
						// temporary replacements created
						if (flexibleReplacement)
							resultReplacements = tempUnityReplacementMap;
					}
				}
			}
		}

		if (result != null) {
			// Setting the fact terms
			for (int i = 0; i < factTerms.length; i++) {
				for (Object factKey : factReplacementMap.keySet())
					if (factTerms[i].equals(factKey))
						factTerms[i] = (String) factReplacementMap.get(factKey);
			}
			// Setting the unity replacements
			if (flexibleReplacement)
				unityReplacementMap.putAll(resultReplacements);
		}

		// If removing the unityFact, do so here
		if (removeUnifiedFact)
			unityFacts.remove(unityResult);

		return result;
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
	private String findUnityReplacement(BidiMap unityReplacementMap,
			String unityTerm, boolean flexibleReplacement,
			BidiMap tempUnityReplacementMap, String factTerm,
			boolean noGeneralisations) {
		// CHECK THE REPLACEMENT MAPS FIRST
		// If the temp already has a replacement, then replace.
		if (tempUnityReplacementMap.containsKey(unityTerm))
			return (String) tempUnityReplacementMap.get(unityTerm);
		// If the unity map already has a replacement, then replace
		if (unityReplacementMap.containsKey(unityTerm))
			return (String) unityReplacementMap.get(unityTerm);

		// If we're allowing flexible replacements, create a replacement and use
		// the replacement
		if (flexibleReplacement) {
			// If the fact term hasn't already been mapped to another unity term
			if (!unityReplacementMap.containsValue(factTerm)
					&& !tempUnityReplacementMap.containsValue(factTerm)) {
				// If the fact isn't anonymous, assign it as the replacement
				if (!factTerm.equals(StateSpec.ANONYMOUS)
						&& !unityTerm.equals(StateSpec.ANONYMOUS)) {
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
	private boolean numericalValueCheck(String factValue, String unityValue,
			String[] unification, int index, String[] factTerms) {
		// Check the unity is a number
		double unityDouble = 0;
		if (StateSpec.isNumber(unityValue))
			unityDouble = Double.parseDouble(unityValue);
		else
			return false;

		// The factValue may be a range
		Pattern rangePattern = Pattern.compile("(\\?"
				+ Pattern.quote(RANGE_VARIABLE_PREFIX) + "[\\d]+)&:\\("
				+ StateSpec.BETWEEN_RANGE + " \\1 ([-\\dE.]+) ([-\\dE.]+)\\)");
		Matcher m = rangePattern.matcher(factValue);

		if (m.find()) {
			// We have a pre-existing range
			String variableName = m.group(1);
			double min = Double.parseDouble(m.group(2));
			double max = Double.parseDouble(m.group(3));

			// Possibly expand the range if need be
			boolean redefine = false;
			if (unityDouble < min) {
				min = unityDouble;
				redefine = true;
			} else if (unityDouble > max) {
				max = unityDouble;
				redefine = true;
			}

			// If the min or max has changed, redefine the range
			if (redefine) {
				unification[index] = variableName + "&:("
						+ StateSpec.BETWEEN_RANGE + " " + variableName + " "
						+ min + " " + max + ")";
			} else {
				unification[index] = factValue;
			}

			return true;
		} else {
			// Check that the fact value is a number
			double factDouble = 0;
			if (StateSpec.isNumber(factValue))
				factDouble = Double.parseDouble(factValue);
			else
				return false;

			// Find the min, max, then form the range
			double min = Math.min(factDouble, unityDouble);
			double max = Math.max(factDouble, unityDouble);
			String rangeVariable = "?" + RANGE_VARIABLE_PREFIX + rangeIndex_++;
			unification[index] = rangeVariable + "&:("
					+ StateSpec.BETWEEN_RANGE + " " + rangeVariable + " " + min
					+ " " + max + ")";

			// Change the action terms as well.
			for (int i = 0; i < factTerms.length; i++) {
				if (factTerms[i].equals(factValue))
					factTerms[i] = rangeVariable;
			}
			return true;
		}
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
