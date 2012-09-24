package cerrla;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import util.MultiMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

public class RLGGMerger {
	/** The singleton instance. */
	private static RLGGMerger instance_;

	/** The cost of a fact not unifying. */
	private static final int NO_FACT_UNIFY = 1000;

	/** If the states did no unify. */
	public static final int CANNOT_UNIFY = -1;

	/** If there was no change but the states unified. */
	public static final int NO_CHANGE = 0;

	/** If the states were unified and the original state changed because of it. */
	public static final int UNIFIED_CHANGE = 1;

	/** The incrementing unique index of ranged values. */
	private int rangeIndex_;

	private RLGGMerger() {
		resetRangeIndex();
	}

	/**
	 * Unifies two states together in a smart, efficient manner.
	 * 
	 * @param oldState
	 *            The old state to unify with. This is modified in the process.
	 * @param newState
	 *            The new state for use in unification.
	 * @param oldTerms
	 *            The terms the old state uses. This is modified in the process.
	 * @param replacementMap
	 *            The replacement map for the new state.
	 * @return 1 if the state unified and changed, 0 if unified without change,
	 *         -1 if couldn't unify.
	 */
	private List<MergedFact> bestFirstUnify(
			Collection<RelationalPredicate> oldState,
			Collection<RelationalPredicate> newState,
			RelationalArgument[] oldTerms, BidiMap replacementMap) {
		// Pre-unify and sort the state for more efficient unification
		boolean changed = false;
		MergeCase initialCase = new MergeCase(
				new ArrayList<RelationalPredicate>(oldState), newState,
				new ArrayList<MergedFact>(),
				new HashSet<RelationalPredicate>(), oldTerms, replacementMap,
				0, changed, rangeIndex_, 0);

		int id = 1;
		PriorityQueue<MergeCase> pendingUnifications = new PriorityQueue<MergeCase>();
		pendingUnifications.add(initialCase);

		// Continue to unify until a state (the best unified state) is fully
		// unified.
		while (!pendingUnifications.isEmpty()) {
			// Get the current information
			MergeCase currentUnification = pendingUnifications.poll();
			List<RelationalPredicate> currentOldState = currentUnification
					.getOldState();
			Collection<RelationalPredicate> currentNewState = currentUnification
					.getNewState();
			List<MergedFact> unifiedOldState = currentUnification
					.getUnifiedOldState();
			Collection<RelationalPredicate> droppedOldFacts = currentUnification
					.getDroppedOldFacts();
			BidiMap currentReplacementMap = currentUnification
					.getArgReplacementMap();
			RelationalArgument[] currentActionTerms = currentUnification
					.getActionArgs();
			int currentGeneralisation = currentUnification
					.getGeneralisationValue();
			changed = currentUnification.isChanged();
			rangeIndex_ = currentUnification.getRangeIndex();


			// Breaking condition - no facts left
			if (currentOldState.isEmpty()) {
				return unifiedOldState;
			}

			// Grab the first fact from the old state, or a pending fact
			RelationalPredicate oldStateFact = currentOldState.iterator()
					.next();
			Collection<MergedFact> modFacts = unifyFactToState(oldStateFact,
					currentNewState, currentReplacementMap, currentActionTerms,
					true);

			// If there was a unification(s) explore them
			boolean createNonUnification = false;
			if (modFacts.isEmpty())
				createNonUnification = true;
			else {
				// Recursively step into each fact unification path
				for (MergedFact modFact : modFacts) {
					List<RelationalPredicate> reducedOldState = new ArrayList<RelationalPredicate>(
							currentOldState);
					reducedOldState.remove(oldStateFact);
					Collection<RelationalPredicate> reducedNewState = new HashSet<RelationalPredicate>(
							currentNewState);
					reducedNewState.remove(modFact.getUnityFact());

					RelationalArgument[] recursiveOldTerms = modFact
							.getFactTerms();
					BidiMap recursiveReplacements = modFact
							.getResultReplacements();
					createNonUnification |= !recursiveReplacements
							.equals(currentReplacementMap);

					List<MergedFact> largerUnifiedOldState = new ArrayList<MergedFact>(
							unifiedOldState);
					largerUnifiedOldState.add(modFact);

					// Create a unified case.
					MergeCase unifiedCase = new MergeCase(reducedOldState,
							reducedNewState, largerUnifiedOldState,
							droppedOldFacts, recursiveOldTerms,
							recursiveReplacements, currentGeneralisation
									+ modFact.getGeneralisation(), changed
									|| !modFact.getResultFact().equals(
											oldStateFact), rangeIndex_, id++);
					pendingUnifications.add(unifiedCase);
				}
			}

			// The non-unification case
			if (createNonUnification) {
				List<RelationalPredicate> reducedOldState = new ArrayList<RelationalPredicate>(
						currentOldState);
				reducedOldState.remove(oldStateFact);
				Collection<RelationalPredicate> largerDroppedOldFacts = new HashSet<RelationalPredicate>(
						droppedOldFacts);
				largerDroppedOldFacts.add(oldStateFact);

				// Create an ununified case
				MergeCase noUnifyCase = new MergeCase(reducedOldState,
						currentNewState, unifiedOldState,
						largerDroppedOldFacts, currentActionTerms,
						currentReplacementMap, currentGeneralisation
								+ NO_FACT_UNIFY, true, rangeIndex_, id++);
				pendingUnifications.add(noUnifyCase);
			}
		}
		return null;
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
			RelationalArgument factTerm) {
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
	 * Pre-unifies the old state, finds out the approximate amount of splitting
	 * each fact has, then returns a sorted list such that proper unification is
	 * more efficient.
	 * 
	 * @param oldState
	 *            The old state.
	 * @param newState
	 *            The new state.
	 * @param oldTerms
	 *            The action terms.
	 * @param replacementMap
	 *            The original replacement map.
	 * @return The initial unification case, with a sorted old state.
	 */
	private MergeCase preUnifyAndSort(Collection<RelationalPredicate> oldState,
			Collection<RelationalPredicate> newState,
			RelationalArgument[] oldTerms, BidiMap replacementMap) {
		List<MergedFact> unified = new ArrayList<MergedFact>();
		Collection<RelationalPredicate> dropped = new HashSet<RelationalPredicate>();
		MultiMap<Integer, RelationalPredicate> complexityMap = MultiMap
				.createListMultiMap();

		// Perform basic unification
		int maxComplexity = 0;
		for (RelationalPredicate oldStateFact : oldState) {
			int beforeRangeIndex = rangeIndex_;
			Collection<MergedFact> unifiedFacts = unifyFactToState(
					oldStateFact, newState, replacementMap, oldTerms, true);

			// If only one fact can unify.
			int numUnifiedFacts = unifiedFacts.size();
			if (numUnifiedFacts == 0) {
				dropped.add(oldStateFact);
			} else if (numUnifiedFacts == 1) {
				// If it required replacements or is numerical, store as
				// complexity 1.
				MergedFact unifiedFact = unifiedFacts.iterator().next();
				if (!unifiedFact.getResultReplacements().equals(replacementMap)
						|| unifiedFact.getResultFact().isNumerical()) {
					complexityMap.put(1, oldStateFact);
					maxComplexity = Math.max(maxComplexity, 1);
				} else {
					// Unify the fact here and now.
					MergedFact unifact = unifiedFacts.iterator().next();
					unified.add(unifact);
				}
			} else if (numUnifiedFacts > 1) {
				// If there is a unification that does not require extra
				// replacements, it may still be able to be unified here and
				// now.
				boolean singleUnify = false;
				for (MergedFact unifiedFact : unifiedFacts) {
					if (unifiedFact.getResultReplacements().equals(
							replacementMap)) {
						unified.add(unifiedFact);
						newState.remove(unifiedFact.getUnityFact());
						singleUnify = true;
						break;
					}
				}

				if (!singleUnify) {
					// If there are N > 1 unified facts, return complexity N
					complexityMap.put(numUnifiedFacts, oldStateFact);
					maxComplexity = Math.max(maxComplexity, numUnifiedFacts);
				}
			}
			rangeIndex_ = beforeRangeIndex;
		}

		// Place preds back into a single collection in 'sorted' order
		List<RelationalPredicate> sortedOldState = new ArrayList<RelationalPredicate>(
				oldState.size());
		int complexity = 0;
		while (complexity <= maxComplexity) {
			Collection<RelationalPredicate> facts = complexityMap
					.get(complexity);
			if (facts != null)
				sortedOldState.addAll(facts);
			complexity++;
		}

		// Create the output initial merge case.
		boolean changed = false;
		if (sortedOldState.size() != oldState.size())
			changed = true;

		// Define the initial case.
		MergeCase initialCase = new MergeCase(sortedOldState, newState,
				unified, dropped, oldTerms, replacementMap, 0, changed,
				rangeIndex_, 0);

		return initialCase;
	}

	/**
	 * Checks if a fact unifies with another fact.
	 * 
	 * @param fact
	 *            The initial fact to unify.
	 * @param unityFact
	 *            The fact to potentially unify against.
	 * @param unityReplacementMap
	 *            The existing replacement map.
	 * @param flexibleReplacement
	 *            If allowing flexible replacements (or rigid matching
	 *            unification)
	 * @param actionTerms
	 *            The action terms for the unification.
	 * @return A unified fact (if the facts can be unified), or null.
	 */
	@SuppressWarnings("unchecked")
	private MergedFact unifyFactToFact(RelationalPredicate fact,
			RelationalPredicate unityFact, BidiMap unityReplacementMap,
			boolean flexibleReplacement, RelationalArgument[] actionTerms) {
		// Check it if the same fact
		boolean samePredicate = fact.getFactName().equals(
				unityFact.getFactName())
				&& (fact.isNegated() == unityFact.isNegated());

		// If dealing with the same fact
		if (samePredicate) {
			// Create the new action terms.
			RelationalArgument[] newActionTerms = null;
			if (actionTerms == null)
				newActionTerms = new RelationalArgument[0];
			else {
				newActionTerms = new RelationalArgument[actionTerms.length];
				System.arraycopy(actionTerms, 0, newActionTerms, 0,
						actionTerms.length);
			}

			RelationalArgument[] factArguments = fact.getRelationalArguments();
			RelationalArgument[] unityArguments = unityFact
					.getRelationalArguments();
			RelationalArgument[] unification = new RelationalArgument[factArguments.length];
			boolean validFact = false;
			int thisGeneralness = 0;
			BidiMap tempUnityReplacementMap = new DualHashBidiMap();

			// Unify each term
			for (int i = 0; i < factArguments.length; i++) {
				// Apply the replacements
				RelationalArgument factTerm = factArguments[i];
				RelationalArgument unityTerm = findUnityReplacement(
						unityReplacementMap, unityArguments[i],
						flexibleReplacement, tempUnityReplacementMap, factTerm);

				// If the unity term was ununifiable, the fact is
				// ununifiable
				if (unityTerm == null) {
					validFact = false;
					return null;
				}

				// First case: check numerical
				if (factArguments[i].isNumber() && unityArguments[i].isNumber()) {
					// We have two numerical terms: unify them into a range
					// if necessary.
					unification[i] = unifyRange(factArguments[i],
							unityArguments[i], newActionTerms);
				} else if (factTerm.equals(unityTerm)) {
					// If the fact and unity term are the same, great! So far,
					// so good
					unification[i] = factTerm;
					validFact = true;
				} else if (unityTerm.isFreeVariable()
						&& factTerm.isFreeVariable()) {
					// If the fact term and unity term are unbound variables
					// (and one of them is anonymous)
					unification[i] = RelationalArgument.ANONYMOUS;
					if (!factArguments[i].equals(RelationalArgument.ANONYMOUS))
						thisGeneralness++;
				} else {
					// Break if the terms differ and cannot be unified
					// through replacement.
					validFact = false;
					return null;
				}
			}

			// Store if:
			// 1. The fact is not fully anonymous
			// 2. The fact is less general than the current best
			if (validFact) {
				RelationalPredicate unifact = new RelationalPredicate(fact,
						unification, fact.isNegated());
				if (unityReplacementMap != null) {
					tempUnityReplacementMap.putAll(unityReplacementMap);
				}
				MergedFact unifiedFact = new MergedFact(fact, unityFact,
						newActionTerms, tempUnityReplacementMap, unifact,
						thisGeneralness);
				return unifiedFact;
			}
		}
		return null;
	}

	public void resetRangeIndex() {
		rangeIndex_ = 0;
	}


	public int newRlggUnification(Collection<RelationalPredicate> oldState,
			Collection<RelationalPredicate> newState, BidiMap replacementMap,
			RelationalArgument[] oldTerms) {
		if (oldState.isEmpty() || newState.isEmpty())
			return NO_CHANGE;

		boolean changed = false;
		Collection<RelationalPredicate> mergeState = new HashSet<RelationalPredicate>();
		Collection<RelationalPredicate> remainFacts = new HashSet<RelationalPredicate>();
		for (RelationalPredicate oldFact : oldState) {
			// If numerical, do special stuff
			if (oldFact.isNumerical()) {
				Collection<MergedFact> merges = unifyFactToState(oldFact,
						newState, replacementMap, oldTerms, true);
				if (merges.isEmpty()) {
					remainFacts.add(oldFact);
					changed = true;
				} else {
					if (merges.size() > 1)
						System.out
								.println("WTF? More than one numerical merge?");
					MergedFact mf = merges.iterator().next();
					mergeState.add(mf.getResultFact());
					newState.remove(mf.getUnityFact());
					System.arraycopy(mf.getFactTerms(), 0, oldTerms, 0,
							oldTerms.length);
					changed |= !mf.getResultFact().equals(oldFact);
				}
			} else {
				// Simple contains check
				if (newState.contains(oldFact)) {
					mergeState.add(oldFact);
					newState.remove(oldFact);
				} else {
					remainFacts.add(oldFact);
					changed = true;
				}
			}
		}

		if (mergeState.isEmpty())
			return CANNOT_UNIFY;

		oldState.clear();
		oldState.addAll(mergeState);
		newState.addAll(remainFacts);
		if (changed)
			return UNIFIED_CHANGE;
		else
			return NO_CHANGE;
	}

	/**
	 * Unifies two states together, with the resultant unification in oldState
	 * and any un-unified terms in newState. The oldTerms are also modified.
	 * 
	 * @param oldState
	 *            The old state to unify with and return unified.
	 * @param newState
	 *            The new state to unify together and also represent the
	 *            ununified terms.
	 * @param replacementMap
	 *            The replacement map to fill with {@link RelationalArgument}s.
	 * @param oldTerms
	 *            The old terms to change.
	 * @return -1 if the states cannot unify, 0 if there is no change to
	 *         oldState, 1 if they unified but the old state was changed. In the
	 *         two latter cases, the replacement map will contain replacement
	 *         terms.
	 */
	@SuppressWarnings("unchecked")
	public int rlggUnification(Collection<RelationalPredicate> oldState,
			Collection<RelationalPredicate> newState, BidiMap replacementMap,
			RelationalArgument[] oldTerms) {
		if (oldState.isEmpty() || newState.isEmpty())
			return NO_CHANGE;

		// Unify the states
		List<MergedFact> unified = bestFirstUnify(oldState, newState, oldTerms,
				replacementMap);

		if (unified.isEmpty())
			return CANNOT_UNIFY;

		// Change the parameter values.
		boolean changed = false;
		int oldStateSize = oldState.size();
		newState.addAll(oldState);
		oldState.clear();
		MergedFact lastFact = null;
		for (MergedFact unifact : unified) {
			RelationalPredicate oldFact = unifact.getBaseFact();
			RelationalPredicate resultFact = unifact.getResultFact();
			changed |= !oldFact.equals(resultFact);

			// Modify the states
			oldState.add(resultFact);
			newState.remove(unifact.getBaseFact());
			newState.remove(unifact.getUnityFact());
			lastFact = unifact;
		}
		changed |= oldStateSize != oldState.size();

		RelationalArgument[] unifiedTerms = lastFact.getFactTerms();
		if (!Arrays.equals(unifiedTerms, oldTerms))
			System.arraycopy(unifiedTerms, 0, oldTerms, 0, unifiedTerms.length);
		replacementMap.putAll(lastFact.getResultReplacements());
		if (changed)
			return UNIFIED_CHANGE;
		else
			return NO_CHANGE;
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
	 * @return The unified version of the fact (possibly more general than the
	 *         input fact) or null if no unification.
	 */
	public Collection<MergedFact> unifyFactToState(RelationalPredicate fact,
			Collection<RelationalPredicate> unityFacts,
			BidiMap unityReplacementMap, RelationalArgument[] actionTerms,
			boolean flexibleReplacement) {
		Collection<MergedFact> unifiedFacts = new HashSet<MergedFact>();

		// Check every fact in the unity facts.
		for (RelationalPredicate unityFact : unityFacts) {
			MergedFact unifiedFact = unifyFactToFact(fact, unityFact,
					unityReplacementMap, flexibleReplacement, actionTerms);
			if (unifiedFact != null)
				unifiedFacts.add(unifiedFact);
		}

		return unifiedFacts;
	}

	/**
	 * Simple min-max operation of discovering the limits of a range.
	 * 
	 * @param range
	 *            The range to stretch.
	 * @param baseVal
	 *            The value being evaluated.
	 */
	public void unifyRange(double[] range, double baseVal) {
		range[0] = Math.min(range[0], baseVal);
		range[1] = Math.max(range[1], baseVal);
	}

	/**
	 * Unify two numerical arguments together into a range.
	 * 
	 * @param baseValue
	 *            The base range term.
	 * @param unifiedValue
	 *            The term being incorporated.
	 * @param actionTerms
	 *            The terms the fact uses as action parameters. Optional.
	 * @return The unified range value of the two facts.
	 */
	public RelationalArgument unifyRange(RelationalArgument baseValue,
			RelationalArgument unifiedValue, RelationalArgument[] actionTerms) {
		// Expand the range if need be.
		double[] baseBounds = baseValue.getExplicitRange();
		double[] unityBounds = unifiedValue.getExplicitRange();
		if (baseBounds == null) {
			if (unityBounds == null)
				return RelationalArgument.ANONYMOUS;
			else
				unifiedValue.clone();
		} else if (unityBounds == null)
			return baseValue.clone();
		double min = Math.min(baseBounds[0], unityBounds[0]);
		double max = Math.max(baseBounds[1], unityBounds[1]);

		String variable = baseValue.getStringArg();
		// If the fact isn't a variable yet, change the action terms.
		if (!baseValue.isRange(false)) {
			// If the unity value is a range, use that.
			if (unifiedValue.isRange(false))
				variable = unifiedValue.getStringArg();
			else {
				variable = createRangeVariable().toString();
				// Changing the action terms
				if (actionTerms != null) {
					for (int i = 0; i < actionTerms.length; i++)
						if (actionTerms[i].equals(baseValue))
							actionTerms[i] = new RelationalArgument(variable);
				}
			}
		}

		return new RelationalArgument(variable, min, max);
	}

	/**
	 * Creates a new range argument.
	 * 
	 * @return A unique range argument.
	 */
	public RelationalArgument createRangeVariable() {
		return new RelationalArgument(RelationalArgument.RANGE_VARIABLE_PREFIX
				+ rangeIndex_++);
	}

	public void updateRangeIndex(int index) {
		rangeIndex_ = index + 1;
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
	 *            The old state to unify with and return modified.
	 * @param newState
	 *            The new state to unify together.
	 * @param replacementMap
	 *            The replacement map to fill with RelationalArguments.
	 * @return -1 if the states cannot unify, 0 if there is no change to
	 *         oldState, 1 if they unified but the old state was changed. In the
	 *         two latter cases, the replacement map will contain replacement
	 *         terms.
	 */
	public List<MergedFact> unifyStates(
			Collection<RelationalPredicate> oldState,
			Collection<RelationalPredicate> newState, BidiMap replacementMap) {
		return bestFirstUnify(oldState, newState, new RelationalArgument[0],
				replacementMap);
	}

	public static RLGGMerger getInstance() {
		if (instance_ == null)
			instance_ = new RLGGMerger();
		return instance_;
	}

	/**
	 * A unification case concerning facts to be unified and their unification
	 * information as well as the level of generalisation that has occured with
	 * unification so far.
	 * 
	 * @author Sam Sarjant
	 */
	private class MergeCase implements Comparable<MergeCase> {
		/** The action arguments. */
		private RelationalArgument[] actionArgs_;
		/** The replacement map used to unify. */
		private BidiMap argReplacementMap_;
		/** If the unification has changed the old state. */
		private boolean changed_;
		/** Facts dropped by the old state. */
		private Collection<RelationalPredicate> droppedOldFacts_;
		/** The generalisation of the unifications so far. */
		private int generalisationValue_;
		/** A unique ID per unification case. */
		private int id_;
		/** The local unification range index. */
		private int localUnificationIndex_;
		/** The new state yet to unify. */
		private Collection<RelationalPredicate> newState_;
		/** The old state yet to unify. */
		private List<RelationalPredicate> oldState_;
		/** The old state that has been unified. */
		private List<MergedFact> unifiedOldState_;

		/**
		 * A more advanced case where there is some unification.
		 * 
		 * @param oldState
		 *            The state to modify.
		 * @param newState
		 *            The state to unify with.
		 * @param unifiedOldState
		 *            The old state that has been unified.
		 * @param droppedOldFacts
		 *            Any facts dropped (not unified) from the old state so far.
		 * @param untestedMap
		 *            The untested predicates map.
		 * @param actionArguments
		 *            The action arguments (if any).
		 * @param replacementMap
		 *            The current argument replacement map.
		 * @param generalisation
		 *            The level of generalisation that has occured through
		 *            unifications so far. 0 is the minimum.
		 * @param changed
		 *            If there is a change in the unified state.
		 * @param rangeIndex
		 *            The current index of the range index.
		 * @param id
		 *            The unique ID assigned to this unification case.
		 */
		public MergeCase(List<RelationalPredicate> oldState,
				Collection<RelationalPredicate> newState,
				List<MergedFact> unifiedOldState,
				Collection<RelationalPredicate> droppedOldFacts,
				RelationalArgument[] actionArguments, BidiMap replacementMap,
				int generalisation, boolean changed, int rangeIndex, int id) {
			oldState_ = oldState;
			newState_ = newState;
			unifiedOldState_ = unifiedOldState;
			droppedOldFacts_ = droppedOldFacts;
			actionArgs_ = actionArguments;
			argReplacementMap_ = replacementMap;
			generalisationValue_ = generalisation;
			localUnificationIndex_ = rangeIndex;
			id_ = id;
			changed_ = changed;
		}

		@Override
		public int compareTo(MergeCase uc) {
			int result = Double.compare(generalisationValue_,
					uc.generalisationValue_);
			if (result != 0)
				return result;
			// Emphasize more unified cases over lesser unified
			result = Double.compare(unifiedOldState_.size(),
					uc.unifiedOldState_.size());
			// Swap comparison, as bigger is better.
			if (result != 0)
				return -result;

			// Emphasize less unification remaining
			result = Double.compare(oldState_.size(), uc.oldState_.size());
			if (result != 0)
				return result;

			result = Double.compare(newState_.size(), uc.newState_.size());
			if (result != 0)
				return result;

			// Otherwise, resort to comparing ID (always returns !0)
			return Double.compare(id_, uc.id_);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MergeCase other = (MergeCase) obj;
			if (id_ != other.id_)
				return false;
			return true;
		}

		public RelationalArgument[] getActionArgs() {
			return actionArgs_;
		}

		public BidiMap getArgReplacementMap() {
			return argReplacementMap_;
		}

		public Collection<RelationalPredicate> getDroppedOldFacts() {
			return droppedOldFacts_;
		}

		public int getGeneralisationValue() {
			return generalisationValue_;
		}

		public Collection<RelationalPredicate> getNewState() {
			return newState_;
		}

		public List<RelationalPredicate> getOldState() {
			return oldState_;
		}

		public int getRangeIndex() {
			return localUnificationIndex_;
		}

		public List<MergedFact> getUnifiedOldState() {
			return unifiedOldState_;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id_;
			return result;
		}

		public boolean isChanged() {
			return changed_;
		}

		@Override
		public String toString() {
			return "Unification Case (g=" + generalisationValue_
					+ "): unified: " + unifiedOldState_.size() + ", oldState: "
					+ oldState_.size() + ", newState: " + newState_.size();
		}
	}
}
