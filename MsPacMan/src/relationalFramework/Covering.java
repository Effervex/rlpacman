package relationalFramework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import jess.Fact;
import jess.Rete;

/**
 * A class which deals specifically with covering rules from a state.
 * 
 * @author Samuel J. Sarjant
 */
public class Covering {
	/** The starting character for variables. */
	private static final char STARTING_CHAR = 'X';
	/** The final character for variables. */
	private static final char MODULO_LETTERS = 26;
	/** The first character for variables. */
	private static final char FIRST_CHAR = 'A';
	/** The amount of state inactivity until it is considered settled. */
	private static final int MAX_STATE_UNIFICATION_INACTIVITY = 50;
	/** The prefix for range variables. */
	public static final String RANGE_VARIABLE_PREFIX = "__Num";
	/** The number of discretised ranges the rules are split into. */
	public static final int NUM_DISCRETE_RANGES = 4;

	/**
	 * The pre-goal state for each action predicate, for use in covering
	 * specialisations.
	 */
	private Map<String, PreGoalInformation> preGoals_;

	/** The hash codes for each pre-goal. */
	private Map<String, Integer> mutationHashs;

	/**
	 * The conditions that have been observed to be true at least for each
	 * action.
	 */
	private MultiMap<String, String> actionConditions_;

	/** The set of conditions that are always true. */
	private Collection<String> environmentInvariants;
	// TODO Set up environment invariants
	/** The number of states the invariants have remained the same. */
	private int stableInvariants_ = 0;

	/** A suffix numbered variable to use when defining new ranged variables. */
	private int rangeIndex_ = 0;

	/** A recorded map of maximum ranges for each action and condition. */
	private MultiMap<String, RangedCondition> actionRanges_;

	public Covering(int numActions) {
		clearPreGoalState(numActions);
		actionRanges_ = new MultiMap<String, RangedCondition>();
		actionConditions_ = new MultiMap<String, String>();
	}

	/**
	 * Covers a state by creating a rule for every action type present in the
	 * valid actions for the state.
	 * 
	 * @param state
	 *            The state of the environment, containing the valid actions.
	 * @param validActions
	 *            The set of valid actions to choose from.
	 * @param coveredRules
	 *            A starting point for the rules, if any exist.
	 * @return A list of guided rules, one for each action type.
	 */
	public List<GuidedRule> coverState(Rete state,
			MultiMap<String, String> validActions,
			MultiMap<String, GuidedRule> coveredRules) throws Exception {
		List<String> constants = StateSpec.getInstance().getConstants();

		// The relevant facts which contain the key term
		MultiMap<String, String> relevantConditions = compileRelevantConditionMap(state);

		// Maintain a mapping for each action, to be used in unification between
		// actions
		List<GuidedRule> generalActions = new ArrayList<GuidedRule>();

		// Run through each valid action.
		for (String action : validActions.keySet()) {
			// Get the list of previous rules, both LGG and non-LGG.
			List<GuidedRule> previousRules = coveredRules.get(action);
			if (previousRules == null)
				previousRules = new ArrayList<GuidedRule>();

			// Cover the state, using the previous rules and/or newly created
			// rules while noting valid conditions for later rule mutation
			// TODO Simplify the rules to remove unnecessary background
			// conditions.
			List<GuidedRule> actionRules = unifyActionRules(validActions
					.get(action), relevantConditions, action, previousRules,
					constants);
			generalActions.addAll(actionRules);
		}

		return generalActions;
	}

	/**
	 * Removes the useless facts from the pre-goal state. Useless facts are
	 * validActions pred, initial-fact and fully anonymous facts.
	 * 
	 * @param preGoalStringState
	 *            The state to remove useless facts from.
	 */
	private void removeUselessFacts(Collection<String> preGoalStringState) {
		for (Iterator<String> iter = preGoalStringState.iterator(); iter
				.hasNext();) {
			String fact = iter.next();
			if (fact.matches("\\(" + StateSpec.VALID_ACTIONS + " .+"))
				iter.remove();
			else if (fact.equals("(initial-fact)"))
				iter.remove();
			else {
				String[] split = StateSpec.splitFact(fact);
				boolean anonymous = true;
				for (int i = 1; i < split.length; i++) {
					if (!split[i].equals("?")) {
						anonymous = false;
						break;
					}
				}

				if (anonymous)
					iter.remove();
			}
		}
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
	public int unifyStates(Collection<String> oldState,
			Collection<String> newState, List<String> oldTerms,
			List<String> newTerms) {
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
	public int unifyStates(Collection<String> oldState,
			Collection<String> newState, BidiMap replacementMap) {
		return unifyStates(oldState, newState, new ArrayList<String>(), true,
				new DualHashBidiMap(), replacementMap);
	}

	/**
	 * The actual unify state method used by the two outer methods.
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
	private int unifyStates(Collection<String> oldState,
			Collection<String> newState, List<String> oldTerms,
			boolean flexibleReplacement, BidiMap oldReplacementMap,
			BidiMap newReplacementMap) {
		// For each item in the old state, see if it is present in the new state
		boolean hasChanged = false;
		List<String> oldStateRepl = new ArrayList<String>();
		for (String oldStateFact : oldState) {
			Collection<String> modFacts = unifyFact(oldStateFact, newState,
					oldReplacementMap, newReplacementMap, oldTerms,
					flexibleReplacement);

			if (modFacts.isEmpty())
				hasChanged = true;

			for (String modFact : modFacts) {
				// Check for a change
				if (!oldStateFact.equals(modFact))
					hasChanged = true;

				if ((modFact != null) && (!oldStateRepl.contains(modFact)))
					oldStateRepl.add(modFact);
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
	private boolean createReplacementMaps(List<String> oldTerms,
			List<String> newTerms, BidiMap oldReplacementMap,
			BidiMap newReplacementMap) {
		for (int i = 0; i < oldTerms.size(); i++) {
			// If this index of terms don't match up
			String oldTerm = oldTerms.get(i);
			String newTerm = newTerms.get(i);
			// If the terms don't match and aren't numbers, create a replacement
			if (!oldTerm.equals(newTerm) && !StateSpec.isNumber(oldTerm)
					&& !StateSpec.isNumber(newTerm)) {
				boolean bothVariables = true;
				String variable = getVariableTermString(i);
				// Replace old term if necessary
				if (oldTerm.charAt(0) != '?') {
					oldReplacementMap.put(oldTerm, variable);
					bothVariables = false;
				}
				if (newTerm.charAt(0) != '?') {
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
	 * @return The unified version of the fact (possibly more general than the
	 *         input fact) or null if no unification.
	 */
	@SuppressWarnings("unchecked")
	private Collection<String> unifyFact(String fact,
			Collection<String> unityFacts, BidiMap factReplacementMap,
			BidiMap unityReplacementMap, List<String> factTerms,
			boolean flexibleReplacement) {
		Collection<String> result = new ArrayList<String>();

		// Split the fact up and apply the replacements
		String[] factSplit = StateSpec.splitFact(fact);

		// Maintain a check on what is the best unification (should better ones
		// be present)
		int generalisation = Integer.MAX_VALUE;
		// Check against each item in the unity state.
		// TODO Speed this up by assigning the facts to a map so they are
		// quickly accessible.
		for (Iterator<String> iter = unityFacts.iterator(); iter.hasNext();) {
			String[] unitySplit = StateSpec.splitFact(iter.next());

			// Check it if the same fact
			boolean sameFact = factSplit[0].equals(unitySplit[0]);
			int compareIndex = 1;
			if (sameFact && factSplit[0].equals("not")) {
				sameFact = factSplit[1].equals(unitySplit[1]);
				compareIndex++;
			}

			if (sameFact) {
				String[] unification = new String[factSplit.length];
				int thisGeneralness = 0;
				boolean notAnonymous = false;
				BidiMap tempUnityReplacementMap = new DualHashBidiMap();

				// Unify each term
				for (int i = compareIndex; i < factSplit.length; i++) {
					// Apply the replacements
					String factTerm = (String) ((factReplacementMap
							.containsKey(factSplit[i])) ? factReplacementMap
							.get(factSplit[i]) : factSplit[i]);
					String unityTerm = findUnityReplacement(
							unityReplacementMap, unitySplit[i],
							flexibleReplacement, tempUnityReplacementMap,
							factTerm);

					// If either are anonymous, the unification must be
					// anonymous
					if (factTerm.equals(StateSpec.ANONYMOUS)) {
						unification[i] = StateSpec.ANONYMOUS;
					} else if (factTerm.equals(unityTerm)) {
						// If the two are the same term (not anonymous) and
						// their replacements match up, use that
						unification[i] = factTerm;
						notAnonymous = true;
					} else if (!numericalValueCheck(factSplit[i],
							unitySplit[i], unification, i, factTerms)) {
						// Failing that simply use an anonymous variable
						unification[i] = StateSpec.ANONYMOUS;
						thisGeneralness++;
					}
				}

				// Store if:
				// 1. The fact is not fully anonymous
				// 2. The fact is less general than the current best
				if (notAnonymous) {
					if (thisGeneralness < generalisation) {
						result.clear();
						generalisation = thisGeneralness;
					}
					if (thisGeneralness == generalisation) {
						unification[0] = factSplit[0];
						if (factSplit[0].equals("not"))
							unification[1] = factSplit[1];
						result.add(StateSpec.reformFact(unification));

						// If we're using flexible replacements, store any
						// temporary replacements created
						if (flexibleReplacement) {
							unityReplacementMap.putAll(tempUnityReplacementMap);
						}
					}
				}
			}
		}

		if (!result.isEmpty()) {
			// Setting the fact terms
			for (Object factKey : factReplacementMap.keySet())
				Collections.replaceAll(factTerms, (String) factKey,
						(String) factReplacementMap.get(factKey));
		}

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
	 * @return The replacement term, or the same term if no replacement
	 *         used/created.
	 */
	private String findUnityReplacement(BidiMap unityReplacementMap,
			String unityTerm, boolean flexibleReplacement,
			BidiMap tempUnityReplacementMap, String factTerm) {
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
				tempUnityReplacementMap.put(unityTerm, factTerm);
				return factTerm;
			}
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
			String[] unification, int index, List<String> factTerms) {
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
			if (factTerms.contains(factValue)) {
				factTerms.set(factTerms.indexOf(factValue), rangeVariable);
			}
			return true;
		}
	}

	/**
	 * Unifies action rules together into one general all-covering rule. Also
	 * notes down conditions which are true/not always true for use in later
	 * rule specialisation.
	 * 
	 * @param argsList
	 *            A heuristically sorted list of actions for a single predicate.
	 * @param relevantConditions
	 *            The relevant conditions for each term in the state.
	 * @param actionPred
	 *            The action predicate spawning this rule.
	 * @param previousRules
	 *            A pre-existing list of previously created rules, both LGG and
	 *            non-LGG.
	 * @param constants
	 *            The constants to maintain in rules.
	 * @return All rules representing a general action.
	 */
	private List<GuidedRule> unifyActionRules(List<String> argsList,
			MultiMap<String, String> relevantConditions, String actionPred,
			List<GuidedRule> previousRules, List<String> constants) {
		// The terms in the action
		String[] action = new String[1 + StateSpec.getInstance().getActions()
				.get(actionPred).size()];
		action[0] = actionPred;

		Iterator<String> argIter = argsList.iterator();
		// Do until:
		// 1) We have no actions left to look at
		// 2) Every rule is not yet minimal
		while (argIter.hasNext()) {
			String arg = argIter.next();
			List<String> actionFacts = new ArrayList<String>();

			String[] terms = arg.split(" ");
			// Find the facts containing the same (useful) terms in the action
			for (int i = 0; i < terms.length; i++) {
				List<String> termFacts = relevantConditions.get(terms[i]);
				if (termFacts != null) {
					for (String termFact : termFacts) {
						if (!actionFacts.contains(termFact))
							actionFacts.add(termFact);
					}
				}

				action[i + 1] = terms[i];
			}

			// Inversely substitute the terms for variables (in string form)
			GuidedRule inverseSubbed = new GuidedRule(convertAndNoteFacts(
					actionFacts, action), StateSpec.reformFact(action), false);

			// Unify with the previous rules, unless it causes the rule to
			// become invalid
			if (previousRules.isEmpty()) {
				previousRules.add(inverseSubbed);
			} else {
				// Unify with each rule in the previous rule/s
				boolean createNewRule = true;
				for (int i = 0; i < previousRules.size(); i++) {
					GuidedRule prev = previousRules.get(i);

					// Unify the prev rule and the inverse subbed rule
					SortedSet<String> ruleConditions = prev.getConditions();
					List<String> ruleTerms = prev.getActionTerms();
					int changed = unifyStates(ruleConditions, inverseSubbed
							.getConditions(), ruleTerms, inverseSubbed
							.getActionTerms());

					if (changed == 1) {
						prev.setActionTerms(ruleTerms);
						prev.setConditions(ruleConditions, true);
					}

					// The rule isn't minimal (but it changed)
					createNewRule = false;

				}

				// If all rules became invalid, we need a new rule
				if (createNewRule) {
					previousRules.add(inverseSubbed);
				}
			}
		}

		// Return the old, possibly modified rules and any new ones.
		for (GuidedRule prev : previousRules) {
			simplifyRule(prev);
			prev.expandConditions();
			prev.incrementStatesCovered();
		}
		return previousRules;
	}

	/**
	 * Simplifies a rule by running through background conditions and removing
	 * any unnecessary conditions.
	 * 
	 * @param rule
	 *            The rule to simplify.
	 */
	private void simplifyRule(GuidedRule rule) {
		SortedSet<String> ruleConds = rule.getConditions();
		boolean changed = false;
		for (BackgroundKnowledge bckKnow : StateSpec.getInstance()
				.getBackgroundKnowledgeConditions()) {
			BidiMap replacementTerms = new DualHashBidiMap();
			int result = unifyStates(bckKnow.getAllConditions(), ruleConds,
					replacementTerms);
			// If all conditions are present, remove the action
			if (result == 0) {
				String cond = bckKnow.getPostCond(replacementTerms);
				ruleConds.remove(cond);
				changed = true;
			}
		}

		if (changed)
			rule.setConditions(ruleConds, false);
	}

	/**
	 * Inversely substitutes a rule for a general form containing only variable
	 * terms. The returned value is in string format for later parsing.
	 * 
	 * @param actionFacts
	 *            The facts relating to this action.
	 * @param actionTerms
	 *            The terms of the action.
	 * @param constants
	 *            The constants to not generalise.
	 * @return A collection of the facts in inversely substituted string format.
	 */
	public Collection<String> inverselySubstitute(Collection<Fact> actionFacts,
			String[] actionTerms, List<String> constants) {
		// Building the mapping from necessary constants to variables
		Map<String, String> termMapping = new HashMap<String, String>();
		int i = 0;
		for (String term : actionTerms) {
			termMapping.put(term, getVariableTermString(i));
			i++;
		}

		Collection<String> substitution = new ArrayList<String>();
		// Applying the mapping to each condition, making unnecessary terms into
		// anonymous terms.
		for (Fact fact : actionFacts) {
			String[] factSplit = StateSpec.splitFact(fact.toString());

			// Maintain a backup to include terms used in facts with action
			// variables/constants
			String[] backupFactSplit = Arrays.copyOf(factSplit,
					factSplit.length);
			boolean useBackup = false;
			Collection<String> backupTypeFacts = new ArrayList<String>();

			// Replace all constant terms in the action with matching variables
			// or anonymous variables
			for (int j = 1; j < factSplit.length; j++) {
				// If the term isn't a constant, replace it with a variable
				if (!constants.contains(factSplit[j])) {
					String replacementTerm = termMapping.get(factSplit[j]);
					// If using variables, keep all other terms in the fact.
					if (replacementTerm != null) {
						factSplit[j] = replacementTerm;
						backupFactSplit[j] = replacementTerm;
						useBackup = true;
					} else {
						factSplit[j] = "?";
						// Note down type predicates as a backup, we may need
						// them
						addTypePred(backupTypeFacts, backupFactSplit, j);
					}
				} else {
					// If we have a numerical value, make it anonymous unless
					// we're using the backup
					if (StateSpec.isNumber(factSplit[j])) {
						factSplit[j] = "?";
					} else {
						// If using a constant, keep all other terms
						useBackup = true;

						// Add the constant type predicate
						addTypePred(substitution, factSplit, j);
					}
				}
			}

			// Reform the fact, possibly using the backup
			String reformedFact = null;
			if (useBackup) {
				reformedFact = StateSpec.reformFact(backupFactSplit);
				// Also add any backup types if necessary
				for (String backupTypeFact : backupTypeFacts) {
					if (!substitution.contains(backupTypeFact))
						substitution.add(backupTypeFact);
				}
			} else
				reformedFact = StateSpec.reformFact(factSplit);

			// Add the fact
			if (!substitution.contains(reformedFact))
				substitution.add(reformedFact);
		}
		return substitution;
	}

	/**
	 * A method which simply converts a List of Facts into a Collection of
	 * Strings of the same facts.
	 * 
	 * @param facts
	 *            The list of facts to be converted into strings.
	 * @param actionTerms
	 *            The action split into terms.
	 * @return A collection of strings of the same facts.
	 */
	private Collection<String> convertAndNoteFacts(List<String> facts,
			String[] actionTerms) {
		Collection<String> strings = new ArrayList<String>();
		for (String fact : facts) {
			strings.add(fact);

			// Replace variables
			String[] factSplit = StateSpec.splitFact(fact);
			for (int i = 1; i < factSplit.length; i++) {
				if (StateSpec.isNumber(factSplit[i]))
					factSplit[i] = "?";
				else {
					boolean replaced = false;
					// Replace each action term present in the fact with a
					// variable
					for (int j = 1; j < actionTerms.length; j++) {
						if (factSplit[i].equals(actionTerms[j])) {
							factSplit[i] = Covering
									.getVariableTermString(j - 1);
							replaced = true;
							break;
						}
					}
					// Replace all else with anonymous
					if (!replaced)
						factSplit[i] = "?";
				}
			}
			fact = StateSpec.reformFact(factSplit);

			// Note down the conditions that can exist for the action
			// TODO This needs to be move alongside the invariants
			actionConditions_.putContains(actionTerms[0], fact);
		}
		return strings;
	}

	/**
	 * Adds a type predicate to a collection for the jth term in the fact split.
	 * 
	 * @param collection
	 *            The collection of facts.
	 * @param factSplit
	 *            The fact split.
	 * @param j
	 *            The jth index.
	 */
	private void addTypePred(Collection<String> collection, String[] factSplit,
			int j) {
		String[] typePred = new String[2];
		typePred[0] = StateSpec.getInstance().getTypePredicate(factSplit[0],
				j - 1);
		// Check it is a valid type pred.
		if (typePred[0] == null)
			return;

		typePred[1] = factSplit[j];

		String formedType = StateSpec.reformFact(typePred);
		if (!collection.contains(formedType))
			collection.add(formedType);
	}

	/**
	 * Compiles the relevant term conditions from the state into map format,
	 * with the term as the key and the fact as the value. This makes finding
	 * relevant conditions a quick matter.
	 * 
	 * @param state
	 *            The state containing the conditions.
	 * @return The relevant conditions multimap.
	 */
	@SuppressWarnings("unchecked")
	private MultiMap<String, String> compileRelevantConditionMap(Rete state) {
		MultiMap<String, String> relevantConditions = new MultiMap<String, String>();
		Collection<String> stateConds = new HashSet<String>();

		for (Iterator<Fact> factIter = state.listFacts(); factIter.hasNext();) {
			Fact stateFact = factIter.next();
			// Ignore the type, inequal and actions pred
			String[] split = StateSpec.splitFact(stateFact.toString());
			if (StateSpec.getInstance().isUsefulPredicate(split[0])) {
				String fact = StateSpec.reformFact(split);
				stateConds.add(fact);
				for (int i = 1; i < split.length; i++) {
					// Ignore numerical terms
					if (!StateSpec.isNumber(split[i]))
						relevantConditions.putContains(split[i], fact);
				}
			}
		}

		// Noting the always true conditions
		if (environmentInvariants == null)
			environmentInvariants = stateConds;
		else if (!environmentInvariants.isEmpty())
			environmentInvariants.retainAll(stateConds);

		return relevantConditions;
	}

	/**
	 * Specialises a rule towards conditions seen in the pre-goal state.
	 * 
	 * @param rule
	 *            The rule to specialise.
	 * @return All single-step mutations the rule can take towards matching the
	 *         pre-goal state.
	 */
	public Set<GuidedRule> specialiseToPreGoal(GuidedRule rule) {
		Set<GuidedRule> mutants = new HashSet<GuidedRule>();

		// Get a single fact or variable specialisation from the pre goal for
		// each mutation
		String actionPred = rule.getActionPredicate();
		PreGoalInformation preGoal = preGoals_.get(actionPred);

		// If we have a pre goal state
		SortedSet<String> ruleConditions = rule.getConditions();
		if (preGoal != null) {
			List<String> preGoalState = preGoal.getState();

			// Form a replacement terms map.
			Map<String, String> replacementTerms = new HashMap<String, String>();
			Map<String, String> reverseReplacementTerms = new HashMap<String, String>();
			List<String> ruleTerms = rule.getActionTerms();
			List<String> preGoalTerms = preGoal.getActionTerms();
			for (int i = 0; i < preGoalTerms.size(); i++) {
				if (!preGoalTerms.get(i).equals(ruleTerms.get(i))) {
					replacementTerms.put(preGoalTerms.get(i), ruleTerms.get(i));
					// Only reverse replace if the term is a variable
					if (ruleTerms.get(i).charAt(0) == '?')
						reverseReplacementTerms.put(ruleTerms.get(i),
								preGoalTerms.get(i));
				}
			}

			// Algorithm detail:
			// 1. Run through each fact in the pre-goal
			// 2. Run through each cond in the rule conditions
			// 3. If the fact pred isn't in the cond preds, add it, replacing
			// pre-goal terms with rule terms
			// 4a. If the fact pred matches the rule pred, try replacing all
			// occurrences of each cond term with the fact term in every cond
			// 4b. If the new cond is valid (through unification), keep it and
			// note that the replacement has occured
			// 5a. If the cond term is ? and the preds match, use the fact term
			// 5b. If the cond term is a variable, the preds match and the fact
			// term isn't part of the action, use the fact term.

			// 1. Run through each fact in the pre-goal
			for (String preGoalFact : preGoalState) {
				String[] preGoalSplit = StateSpec.splitFact(preGoalFact);
				boolean hasPred = false;

				// 2. Run through each cond in the rule conditions
				for (String cond : ruleConditions) {
					String[] condSplit = StateSpec.splitFact(cond);

					hasPred |= factMutation(rule.getAction(), mutants,
							actionPred, preGoalState, ruleConditions,
							replacementTerms, reverseReplacementTerms,
							ruleTerms, preGoalTerms, preGoalSplit, cond,
							condSplit, rule.getQueryParameters(), rule
									.isMutant());
				}

				// 3. If the fact pred isn't in the cond preds, add it,
				// replacing pre-goal terms with rule terms
				if (!hasPred) {
					// Replace pre-goal action terms with local rule terms
					for (String replaceKey : replacementTerms.keySet()) {
						preGoalFact = preGoalFact.replaceAll(" "
								+ Pattern.quote(replaceKey) + "(?=( |\\)))",
								" " + replacementTerms.get(replaceKey));
					}

					// If the fact isn't in the rule, we have a mutation
					if (!ruleConditions.contains(preGoalFact)) {
						SortedSet<String> mutatedConditions = new TreeSet<String>(
								ruleConditions);
						if (isLegalRule(mutatedConditions, preGoalFact, null)) {
							mutatedConditions.add(preGoalFact);
							GuidedRule mutant = new GuidedRule(
									mutatedConditions, rule.getAction(), true);
							mutant.setQueryParams(rule.getQueryParameters());
							mutant.expandConditions();
							mutants.add(mutant);
						}
					}
				}
			}
		} else {
			// Split any ranges up
			mutants.addAll(splitRanges(ruleConditions, rule.getAction(), rule
					.getQueryParameters(), rule.getActionPredicate(), null, 0,
					rule.isMutant()));
		}

		return mutants;
	}

	/**
	 * Specialise a rule by adding a condition to it which includes a term used
	 * in the action.
	 * 
	 * @param rule
	 *            The rule to specialise.
	 * @return A collection of possible specialisations of the rule.
	 */
	public Set<GuidedRule> specialiseRule(GuidedRule rule) {
		Set<GuidedRule> specialisations = new HashSet<GuidedRule>();

		String actionPred = rule.getActionPredicate();
		if (!actionConditions_.containsKey(actionPred))
			return specialisations;
		SortedSet<String> conditions = rule.getConditions();
		String action = rule.getAction();
		List<String> actionTerms = rule.getActionTerms();

		// Add conditions, one-by-one, using both negation and regular
		for (String condition : actionConditions_.get(actionPred)) {
			// Modify the condition arguments to match the action arguments.
			String[] condSplit = StateSpec.splitFact(condition);
			for (int i = 1; i < condSplit.length; i++) {
				int termIndex = getVariableTermIndex(condSplit[i]);
				if (termIndex != -1)
					condSplit[i] = actionTerms.get(termIndex);
			}
			condition = StateSpec.reformFact(condSplit);
			String negCondition = "(not " + condition + ")";

			// If the rule doesn't contain either the condition of the negated
			// condition, add both.
			if (isLegalRule(conditions, condition, negCondition)) {
				SortedSet<String> speConditions = new TreeSet<String>(
						conditions);

				// Add the condition and check if it exists
				speConditions.add(condition);
				GuidedRule specialisation = new GuidedRule(speConditions,
						action, true);
				if (!specialisations.contains(specialisation))
					specialisations.add(specialisation);

				speConditions = new TreeSet<String>(conditions);

				// Add the condition and check if it exists
				speConditions.add(negCondition);
				specialisation = new GuidedRule(speConditions, action, true);
				if (!specialisations.contains(specialisation))
					specialisations.add(specialisation);
			}
		}

		return specialisations;
	}

	/**
	 * Checks if a rule is legal/useful based on logical negation and inference.
	 * 
	 * @param conditions
	 *            The existing conditions of a rule.
	 * @param condition
	 *            The condition that is to be added.
	 * @param negCondition
	 *            The negation of the condition to be added.
	 * @return True if the added condition is legal/not useless.
	 */
	private boolean isLegalRule(SortedSet<String> conditions, String condition,
			String negCondition) {
		Collection<String> unification = unifyFact(condition, conditions,
				new DualHashBidiMap(), new DualHashBidiMap(),
				new ArrayList<String>(), true);
		if (!unification.isEmpty())
			return false;
		if (negCondition != null) {
			Collection<String> negUnification = unifyFact(negCondition,
					conditions, new DualHashBidiMap(), new DualHashBidiMap(),
					new ArrayList<String>(), true);
			if (!negUnification.isEmpty())
				return false;
		}

		// Checking background knowledge preventable rules
		Collection<String> checkedConds = new ArrayList<String>();
		checkedConds.add(condition);
		if (negCondition != null)
			checkedConds.add(negCondition);
		for (String condAdded : checkedConds) {
			Collection<String> addedConditionCol = new TreeSet<String>(
					conditions);
			addedConditionCol.add(condAdded);
			for (BackgroundKnowledge bckKnow : StateSpec.getInstance()
					.getBackgroundKnowledgeConditions()) {
				// Pointless specialisations
				Collection<String> knowledgeConditions = bckKnow
						.getAllConditions();
				int result = unifyStates(knowledgeConditions,
						addedConditionCol, new DualHashBidiMap());
				// If the state remained the same, then the rule has pointless
				// assertions in it.
				if (result == 0)
					return false;

				// Illegal specialisations
				knowledgeConditions = bckKnow.getConjugatedConditions();
				result = unifyStates(knowledgeConditions, addedConditionCol,
						new DualHashBidiMap());
				// If the state remained the same, then the rule is illegal
				if (result == 0)
					return false;
			}
		}

		return true;
	}

	/**
	 * Splits any ranges in a rule into a number of smaller uniform sub-ranges.
	 * Only mutates rules which are either covered rules, or mutants with ranges
	 * equal to the covered rules.
	 * 
	 * @param ruleConditions
	 *            The original conditions of the rule.
	 * @param ruleAction
	 *            The original action of the rule. Shouldn't need to change.
	 * @param queryParameters
	 *            The query parameters the rule may take.
	 * @param actionPred
	 *            The action predicate.
	 * @param preGoalSplit
	 *            The split term in the pre-goal matching a ranged condition in
	 *            the rule. Can be null.
	 * @param numericalIndex
	 *            The index in the pre-goal split of the numerical value. Not
	 *            used if preGoalSplit is null.
	 * @param isRuleMutant
	 *            If the rule is a mutant or not.
	 * @return A collection of any sub-ranged mutants created from the rule.
	 */
	private Collection<GuidedRule> splitRanges(
			Collection<String> ruleConditions, String ruleAction,
			List<String> queryParameters, String actionPred,
			String[] preGoalSplit, int numericalIndex, boolean isRuleMutant) {
		Collection<GuidedRule> subranges = new ArrayList<GuidedRule>();

		// Run through each condition
		int c = 0;
		for (String condition : ruleConditions) {
			String[] condSplit = StateSpec.splitFact(condition);
			for (int i = 1; i < condSplit.length; i++) {
				String origCond = condSplit[i];
				int suchThatIndex = condSplit[i].indexOf("&:");

				if (suchThatIndex != -1) {
					// We have a range
					String[] betweenRangeSplit = StateSpec
							.splitFact(condSplit[i]
									.substring(suchThatIndex + 2));
					// Find the values
					double min = Double.parseDouble(betweenRangeSplit[2]);
					double max = Double.parseDouble(betweenRangeSplit[3]);
					double rangeSplit = (max - min) / NUM_DISCRETE_RANGES;
					RangedCondition rc = new RangedCondition(condSplit[0], min,
							max);

					// If dealing with a covered rule, record rule details
					boolean mutate = false;
					if (!isRuleMutant) {
						mutate = true;
						actionRanges_.putReplace(actionPred, rc);
					} else {
						// Test to see if the range matches the action range.
						if (actionRanges_.containsKey(actionPred)) {
							int index = actionRanges_.get(actionPred).indexOf(
									rc);
							if (index != -1) {
								// There is an action range
								RangedCondition coveredRange = actionRanges_
										.getIndex(actionPred, index);
								if (rc.equalRange(coveredRange)) {
									mutate = true;
								}
							}
						}
					}

					// Check if the pre-goal matches the value
					if (mutate) {
						determineRanges(ruleConditions, ruleAction,
								queryParameters, preGoalSplit, numericalIndex,
								subranges, c, condSplit, i,
								betweenRangeSplit[1], min, max, rangeSplit);

						condSplit[i] = origCond;
					}
				}
			}
		}

		return subranges;
	}

	/**
	 * Determines the ranges and creates mutants for them.
	 * 
	 * @param ruleConditions
	 *            The rule conditions.
	 * @param ruleAction
	 *            The rule action.
	 * @param queryParameters
	 *            The rule's possible query parameters.
	 * @param preGoalSplit
	 *            The possibly null pre-goal split condition.
	 * @param numericalIndex
	 *            The index of the range in the pre-goal split.
	 * @param subranges
	 *            The collection of rules to add to.
	 * @param c
	 *            The condition index.
	 * @param condSplit
	 *            The split condition.
	 * @param i
	 *            The index of the split condition.
	 * @param rangeVariable
	 *            the range variable.
	 * @param min
	 *            The minimum value of the range.
	 * @param max
	 *            The maximum value of the range.
	 * @param rangeSplit
	 *            The size of the subranges.
	 */
	private void determineRanges(Collection<String> ruleConditions,
			String ruleAction, List<String> queryParameters,
			String[] preGoalSplit, int numericalIndex,
			Collection<GuidedRule> subranges, int c, String[] condSplit, int i,
			String rangeVariable, double min, double max, double rangeSplit) {
		// Add the default ranged split
		ArrayList<Double> values = new ArrayList<Double>();
		values.add(min);
		values.add(max);

		// If the ranges go through 0, add that as a stopping point.
		if (min * max < 0)
			values.add(0d);

		Collections.sort(values);
		subranges
				.addAll(createRangedMutations(ruleConditions, ruleAction,
						queryParameters, c, condSplit, i, rangeVariable, values
								.toArray(new Double[values.size()]),
						rangeSplit, false));

		// If we have a pre-goal and the current split is the same condition.
		if ((preGoalSplit != null) && (condSplit[0].equals(preGoalSplit[0]))) {
			values.clear();

			// Split the pre-goal range into chunks of max size rangeSplit
			if (preGoalSplit[numericalIndex].contains(StateSpec.BETWEEN_RANGE)) {
				// Mutate to a pre-goal range
				int preGoalSuchThatIndex = preGoalSplit[numericalIndex]
						.indexOf("&:");
				String[] preGoalRangeSplit = StateSpec
						.splitFact(preGoalSplit[numericalIndex]
								.substring(preGoalSuchThatIndex + 2));
				double startPoint = Double.parseDouble(preGoalRangeSplit[2]);
				if (startPoint < min)
					startPoint = min;
				double endPoint = Double.parseDouble(preGoalRangeSplit[3]);
				if (endPoint > max)
					endPoint = max;

				if (startPoint * endPoint < 0)
					values.add(0d);

				// Determine the ranges
				values.add(startPoint);
				values.add(endPoint);

				Collections.sort(values);
				subranges.addAll(createRangedMutations(ruleConditions,
						ruleAction, queryParameters, c, condSplit, i,
						rangeVariable, values
								.toArray(new Double[values.size()]),
						rangeSplit, true));
			} else {
				double point = Double.parseDouble(preGoalSplit[numericalIndex]);
				if (point < min)
					point = min;
				if (point > max)
					point = max;

				// Create the point mutant itself
				condSplit[i] = point + "";
				String fullCondition = StateSpec.reformFact(condSplit);
				List<String> cloneConds = new ArrayList<String>(ruleConditions);
				cloneConds.set(c, fullCondition);

				String newAction = ruleAction.replaceAll(Pattern
						.quote(rangeVariable), point + "");
				GuidedRule mutant = new GuidedRule(cloneConds, newAction, true);
				mutant.setQueryParams(queryParameters);
				subranges.add(mutant);
			}
		}
	}

	/**
	 * Create a number of ranged mutations between two points.
	 * 
	 * @param ruleConditions
	 *            The rule conditions clone and modify.
	 * @param ruleAction
	 *            The rule action.
	 * @param queryParameters
	 *            The query parameters.
	 * @param condIndex
	 *            The index of the condition within the rule.
	 * @param condSplit
	 *            The split condition.
	 * @param condSplitIndex
	 *            The index within the split condition being modified.
	 * @param rangeVariable
	 *            The variable used to represent the range.
	 * @param values
	 *            The values to create ranges in between.
	 * @param normalRange
	 *            The normal range to split by.
	 * @param createMaxRange
	 *            If a maximum all encompassing range should be created as well.
	 * @return A collection of mutated rules, each representing a portion of the
	 *         range given. The number of rules equals the number of ranges.
	 */
	private Collection<GuidedRule> createRangedMutations(
			Collection<String> ruleConditions, String ruleAction,
			List<String> queryParameters, int condIndex, String[] condSplit,
			int condSplitIndex, String rangeVariable, Double[] values,
			double normalRange, boolean createMaxRange) {
		Collection<GuidedRule> subranges = new ArrayList<GuidedRule>();

		// Run through each range
		int numRanges = 0;
		for (int i = 0; i < values.length - 1; i++) {
			double min = values[i];
			double max = values[i + 1];
			numRanges = (int) Math.ceil((max - min) / normalRange);

			if (numRanges > 0) {
				double rangeSplit = (max - min) / numRanges;

				// Create NUM_DISCRETE_RANGES mutants per range
				for (int j = 0; j < numRanges; j++) {
					condSplit[condSplitIndex] = rangeVariable + "&:("
							+ StateSpec.BETWEEN_RANGE + " " + rangeVariable
							+ " " + (min + rangeSplit * j) + " "
							+ (min + rangeSplit * (j + 1)) + ")";
					String fullCondition = StateSpec.reformFact(condSplit);
					List<String> cloneConds = new ArrayList<String>(
							ruleConditions);
					cloneConds.set(condIndex, fullCondition);

					GuidedRule mutant = new GuidedRule(cloneConds, ruleAction,
							true);
					mutant.setQueryParams(queryParameters);
					subranges.add(mutant);
				}
			}
		}

		// If we're creating a range from min to max value and it hasn't already
		// been created (only 1 range)
		if (createMaxRange && (numRanges > 1)) {
			condSplit[condSplitIndex] = rangeVariable + "&:("
					+ StateSpec.BETWEEN_RANGE + " " + rangeVariable + " "
					+ values[0] + " " + values[values.length - 1] + ")";
			String fullCondition = StateSpec.reformFact(condSplit);
			List<String> cloneConds = new ArrayList<String>(ruleConditions);
			cloneConds.set(condIndex, fullCondition);

			GuidedRule mutant = new GuidedRule(cloneConds, ruleAction, true);
			mutant.setQueryParams(queryParameters);
			subranges.add(mutant);
		}

		return subranges;
	}

	/**
	 * Individual mutation of facts method. This method creates mutations that
	 * operate on the very facts themselves, by swapping fact terms for other
	 * terms. This method was automatically extracted, so it's a bit messy with
	 * its arguments.
	 * 
	 * @param ruleAction
	 *            The rule's action.
	 * @param mutants
	 *            The collection of mutants to add to.
	 * @param actionPred
	 *            The action predicate for the rule.
	 * @param preGoalState
	 *            The pre-goal state being mutated towards.
	 * @param ruleConditions
	 *            The rule's conditions.
	 * @param replacementTerms
	 *            The terms to replace in the pre-goal.
	 * @param reverseReplacementTerms
	 *            The terms to replace in the conditions (modifiable).
	 * @param ruleTerms
	 *            The terms used in the rule's action.
	 * @param preGoalTerms
	 *            The terms used in the pre-goal's action.
	 * @param preGoalSplit
	 *            The current fact's split form.
	 * @param cond
	 *            The current rule condition being looked at.
	 * @param condSplit
	 *            The split form of the cond.
	 * @param queryParameters
	 *            The query parameters of the parent rule, if any.
	 * @param isRuleMutant
	 *            If the rule is a mutant oir not.
	 * @return True if the cond and pre-goal fact match.
	 */
	private boolean factMutation(String ruleAction, Set<GuidedRule> mutants,
			String actionPred, List<String> preGoalState,
			Collection<String> ruleConditions,
			Map<String, String> replacementTerms,
			Map<String, String> reverseReplacementTerms,
			List<String> ruleTerms, List<String> preGoalTerms,
			String[] preGoalSplit, String cond, String[] condSplit,
			List<String> queryParameters, boolean isRuleMutant) {
		// 4a. If the fact pred matches the rule pred, try replacing all
		// occurrences of each cond term with the fact term in every cond
		boolean hasPred = false;
		if (condSplit[0].equals(preGoalSplit[0])) {
			hasPred = true;

			// Run through each term
			boolean ceaseMutation = false;
			for (int i = 1; i < condSplit.length; i++) {
				// Refining ranged numerical variables
				String preGoalITerm = preGoalSplit[i];

				// Finding the replacement term
				String replacedTerm = (replacementTerms
						.containsKey(preGoalITerm)) ? replacementTerms
						.get(preGoalITerm) : preGoalITerm;

				// 5. Replacing variable terms
				if (condSplit[i].charAt(0) == '?') {
					if (!condSplit[i].contains("&:")) {
						String backup = condSplit[i];
						// The pregoal term is not anonymous
						if (!ceaseMutation
								&& !preGoalITerm.equals(StateSpec.ANONYMOUS)) {
							// 5a. If the cond term is ? and the preds match,
							// use
							// the fact term
							boolean continueCreation = true;
							// If the condition is anonymous, replace it with
							// any
							// term
							if (condSplit[i].equals(StateSpec.ANONYMOUS))
								condSplit[i] = replacedTerm;
							// Otherwise, a variable can be replaced with a
							// non-action constant
							else if (!preGoalTerms.contains(preGoalITerm))
								condSplit[i] = preGoalITerm;
							// Otherwise, we're looking at two non-mutatable
							// actions.
							else {
								continueCreation = false;
								// If the actions are different, cease mutation
								if (!condSplit[i].equals(replacedTerm))
									ceaseMutation = true;
							}

							if (continueCreation) {
								// Create the mutant
								String newCond = StateSpec
										.reformFact(condSplit);
								if (!ruleConditions.contains(newCond)) {
									List<String> mutatedConditions = new ArrayList<String>(
											ruleConditions);
									mutatedConditions.set(mutatedConditions
											.indexOf(cond), newCond);
									GuidedRule mutant = new GuidedRule(
											mutatedConditions, ruleAction, true);
									mutant.setQueryParams(queryParameters);
									// Here, needs to add inequals and types
									// that may not be present.
									mutant.expandConditions();
									mutants.add(mutant);
								}

								// Revert the cond split back
								condSplit[i] = backup;
							}
						}
					}
				} else if (!condSplit[i].equals(replacedTerm)) {
					// If the terms don't match up, then the entire fact won't,
					// so skip it.
					break;
				}

				// If we haven't looked at a replacement yet
				if (reverseReplacementTerms.containsKey(condSplit[i])) {
					// Replacing the actions
					List<String> replActionTerms = new ArrayList<String>(
							ruleTerms);
					replActionTerms.set(replActionTerms.indexOf(condSplit[i]),
							reverseReplacementTerms.get(condSplit[i]));

					GuidedRule mutant = replaceActionTerm(actionPred,
							preGoalState, preGoalTerms, ruleConditions,
							condSplit[i], reverseReplacementTerms
									.get(condSplit[i]), replActionTerms, true);
					if (mutant != null)
						mutants.add(mutant);

					// Whatever the outcome, remove the reverse replacement
					// as it has already been tested.
					reverseReplacementTerms.remove(condSplit[i]);
				}

				// If the condSplit is a numerical range, create a number of
				// sub-ranged mutations, using the range found in the pre-goal.
				if (condSplit[i].contains(StateSpec.BETWEEN_RANGE)) {
					mutants.addAll(splitRanges(ruleConditions, ruleAction,
							queryParameters, actionPred, preGoalSplit, i,
							isRuleMutant));
				}
			}
		}
		return hasPred;
	}

	/**
	 * Replaces an action term with the action term used in the pre-goal.
	 * 
	 * @param actionPred
	 *            The action predicate.
	 * @param preGoalState
	 *            The current pre-goal state.
	 * @param preGoalTerms
	 *            The pre-goal action terms.
	 * @param ruleConditions
	 *            The rule conditions in the to-be-mutated rule.
	 * @param replacedTerm
	 *            The term to be replaced.
	 * @param replacementTerm
	 *            The term that will be replacing the above term.
	 * @param replActionTerms
	 *            The action terms, already with the replaced term included.
	 * @param requireUnification
	 *            If unification is required to verify the swap.
	 * @return The mutant rule.
	 */
	private GuidedRule replaceActionTerm(String actionPred,
			List<String> preGoalState, List<String> preGoalTerms,
			Collection<String> ruleConditions, String replacedTerm,
			String replacementTerm, List<String> replActionTerms,
			boolean requireUnification) {
		// Replace the variables with their replacements.
		SortedSet<String> replacementConds = new TreeSet<String>(
				ConditionComparator.getInstance());
		for (String condition : ruleConditions) {
			// Exclude inequals checks
			if ((condition.length() <= 5)
					|| (!condition.substring(0, 5).equals("(test"))) {
				// Replace variables
				String replCond = condition.replaceAll(" "
						+ Pattern.quote(replacedTerm) + "(?=( |\\)))", " "
						+ replacementTerm);
				replacementConds.add(replCond);
			}
		}

		// 4b. If the new cond is valid (through unification),
		// keep it and note that the replacement has occurred
		if (!requireUnification
				|| (unifyStates(replacementConds, preGoalState,
						replActionTerms, preGoalTerms) == 0)) {
			// Reforming the action
			String[] replActionArray = new String[replActionTerms.size() + 1];
			replActionArray[0] = actionPred;
			for (int j = 0; j < replActionTerms.size(); j++)
				replActionArray[j + 1] = replActionTerms.get(j);
			String action = StateSpec.reformFact(replActionArray);

			// Adding the mutated rule
			GuidedRule mutant = new GuidedRule(replacementConds, action, true);
			// Should only need to add inequals here.
			mutant.expandConditions();
			return mutant;
		}
		return null;
	}

	/**
	 * Forms the pre-goal state by adding to it a pre-goal state the agent has
	 * seen (or was given). This method forms the pre-goal state by finding the
	 * bare minimal conditions seen in every pre-goal state.
	 * 
	 * @param preGoalState
	 *            The pre-goal state seen by the agent.
	 * @param actions
	 *            The final action/s taken by the agent.
	 * @param constants
	 *            The list of constants used in forming the pre-goal state.
	 * @return A collection of actions which have settled pre-goals.
	 */
	public Collection<String> formPreGoalState(Collection<Fact> preGoalState,
			ActionChoice actionChoice, List<String> constants) {
		// A set to note which actions have been covered this state to avoid
		// premature settling.
		Set<String> formedActions = new HashSet<String>();

		for (RuleAction ruleAction : actionChoice.getActions()) {
			String actionPred = ruleAction.getRule().getActionPredicate();
			// If the state isn't yet settled, try unification
			if (!isPreGoalSettled(actionPred)) {

				List<String> actions = ruleAction.getUtilisedActions();

				// Create a pre-goal from every action in the actions list.
				if (actions != null) {
					for (String action : actions) {
						// Inversely substitute the old pregoal state
						String[] actionSplit = StateSpec.splitFact(action);
						String[] actionTerms = Arrays.copyOfRange(actionSplit,
								1, actionSplit.length);

						// The actions become constants if possible
						List<String> newConstants = new ArrayList<String>(
								constants);
						List<String> newStateTerms = new ArrayList<String>();
						for (String actionTerm : actionTerms) {
							newStateTerms.add(actionTerm);
							// Ignore numerical terms and terms already added
							if (!newConstants.contains(actionTerm))
								newConstants.add(actionTerm);
						}
						Collection<String> preGoalStringState = inverselySubstitute(
								preGoalState, actionTerms, newConstants);
						removeUselessFacts(preGoalStringState);

						// Unify with the old state
						PreGoalInformation preGoal = preGoals_
								.get(actionSplit[0]);
						if (preGoal == null) {
							preGoals_.put(actionSplit[0],
									new PreGoalInformation(
											(List<String>) preGoalStringState,
											newStateTerms));
							calculateMutationHash(actionSplit[0]);
						} else {
							// Unify the two states and check if it has changed
							// at all.
							int result = unifyStates(preGoal.getState(),
									preGoalStringState, preGoal
											.getActionTerms(), newStateTerms);

							// If the states unified, reset the counter,
							// otherwise increment.
							if (result == 1) {
								preGoal.resetInactivity();
								calculateMutationHash(actionSplit[0]);
							} else if (result == 0) {
								if (!formedActions.contains(actionPred))
									preGoal.incrementInactivity();
							} else if (result == -1) {
								throw new RuntimeException(
										"Pre-goal states did not unify: "
												+ preGoal + ", "
												+ preGoalStringState);
							}

							formedActions.add(actionPred);
						}
					}
				}
			}
		}

		Collection<String> settled = new ArrayList<String>();
		for (String preGoalAction : preGoals_.keySet()) {
			if (isPreGoalSettled(preGoalAction))
				settled.add(preGoalAction);
		}
		return settled;
	}

	/**
	 * Calculates the hash value for a pre-goal and stores it.
	 * 
	 * @param action
	 *            The action for the pre-goal.
	 */
	private void calculateMutationHash(String action) {
		PreGoalInformation pgi = preGoals_.get(action);
		int hash = 0;
		if (pgi != null)
			hash = pgi.hashCode();

		int actionHash = 0;
		if (actionConditions_.containsKey(action))
			for (String actionCond : actionConditions_.get(action))
				actionHash += actionCond.hashCode();

		int prime = 17;
		mutationHashs.put(action, prime * hash + actionHash);
	}

	/**
	 * Clears the pregoal state.
	 */
	public void clearPreGoalState(int numActions) {
		preGoals_ = new HashMap<String, PreGoalInformation>(numActions);
		mutationHashs = new HashMap<String, Integer>(numActions);
	}

	/**
	 * Sets the pre-goal state to the arguments given. Should just be used for
	 * testing.
	 * 
	 * @param action
	 *            The full action in least general terms.
	 * @param preGoal
	 *            The state itself.
	 */
	public void setPreGoal(String action, List<String> preGoal) {
		String[] split = StateSpec.splitFact(action);
		List<String> terms = new ArrayList<String>();
		for (int i = 1; i < split.length; i++)
			terms.add(split[i]);
		preGoals_.put(split[0], new PreGoalInformation(preGoal, terms));
		calculateMutationHash(split[0]);
	}

	/**
	 * Sets the individual conditions that have been observed to be true for
	 * executing an action. Should just be used for testing.
	 * 
	 * @param action
	 *            The action to set the conditions for.
	 * @param conditions
	 *            The conditions that have been observed to be true for the
	 *            action.
	 */
	public void setAllowedActionConditions(String action,
			Collection<String> conditions) {
		actionConditions_.putContains(action, conditions);
	}

	/**
	 * If the pre-goal state has settled to a stable state.
	 * 
	 * @param actionPred
	 *            The action predicate to check for pre-goal settlement.
	 * @return True if the state has settled, false otherwise.
	 */
	public boolean isPreGoalSettled(String actionPred) {
		// If the pre-goal isn't empty and is settled, return true
		if (preGoals_.containsKey(actionPred)) {
			if (preGoals_.get(actionPred).isSettled())
				return true;
		}
		return false;
	}

	/**
	 * Checks if the pregoal for the given action is rececntly changed.
	 * 
	 * @param actionPred
	 *            The action predicate.
	 * @return True if the pregoal was recently modified, false otherwise.
	 */
	public boolean isPreGoalRecentlyChanged(String actionPred) {
		if (preGoals_.containsKey(actionPred)) {
			return preGoals_.get(actionPred).isRecentlyChanged();
		}
		return false;
	}

	/**
	 * Gets the pre-goal general state, seen by the agent.
	 * 
	 * @param action
	 *            The pre-goal action.
	 * @return The pre-goal state, in the form of a list of facts.
	 */
	public List<String> getPreGoalState(String action) {
		if (preGoals_.get(action) == null)
			return null;
		return preGoals_.get(action).getState();
	}

	/**
	 * Gets the pre-goal action (either using constants, or using variables).
	 * 
	 * @param action
	 *            The pre-goal action. If using constants, can be used to create
	 *            a 'perfect' rule.
	 * @return The pre-goal action.
	 */
	public List<String> getPreGoalAction(String action) {
		return preGoals_.get(action).getActionTerms();
	}

	/**
	 * Gets the hash code for the current pre-goal.
	 * 
	 * @param actionPredicate
	 *            The action predicate for the pre-goal.
	 * @return An integer hash code corresponding to the pre-goal state.
	 */
	public int getMutationHash(String actionPredicate) {
		Integer hash = mutationHashs.get(actionPredicate);
		if (hash == null)
			return 0;
		return hash;
	}

	/**
	 * Generates a variable term string from the index given.
	 * 
	 * @param i
	 *            The index of the string.
	 * @return A string in variable format, with the name of the variable in the
	 *         middle.
	 */
	public static String getVariableTermString(int i) {
		char variable = (char) (FIRST_CHAR + (STARTING_CHAR - FIRST_CHAR + i)
				% MODULO_LETTERS);
		return "?" + variable;
	}

	/**
	 * The opposite of getVariableTermString, this method gets the integer the
	 * variable corresponds to, or -1 if invalid.
	 * 
	 * @param variable
	 *            The variable string to check.
	 * @return An integer corresponding to the position in the action the
	 *         variable refers to or -1 if invalid.
	 */
	private static int getVariableTermIndex(String variable) {
		if (variable.length() < 2)
			return -1;

		int termIndex = (variable.charAt(1) + MODULO_LETTERS - STARTING_CHAR)
				% MODULO_LETTERS;
		return termIndex;
	}

	/**
	 * If a pre-goal has been initialised.
	 * 
	 * @return True if there is a pre-goal, false otherwise.
	 */
	public boolean hasPreGoal() {
		if (preGoals_.isEmpty())
			return false;
		return true;
	}

	/**
	 * A basic object for holding pre-goal state information for an action.
	 * 
	 * @author Samuel J. Sarjant
	 */
	private class PreGoalInformation {
		private List<String> state_;
		private List<String> actionTerms_;
		private int inactivity_ = 0;

		public PreGoalInformation(List<String> state, List<String> actionTerms) {
			state_ = state;
			actionTerms_ = actionTerms;
		}

		public int incrementInactivity() {
			inactivity_++;
			return inactivity_;
		}

		public void resetInactivity() {
			inactivity_ = 0;
		}

		public boolean isSettled() {
			if (inactivity_ < MAX_STATE_UNIFICATION_INACTIVITY)
				return false;
			return true;
		}

		public boolean isRecentlyChanged() {
			if (inactivity_ == 0)
				return true;
			return false;
		}

		public List<String> getState() {
			return state_;
		}

		public List<String> getActionTerms() {
			return actionTerms_;
		}

		@Override
		public String toString() {
			return state_.toString() + " : " + actionTerms_.toString();
		}

		private Covering getOuterType() {
			return Covering.this;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			int actionResult = 0;
			for (String condition : actionTerms_)
				actionResult += condition.hashCode();
			result = prime * result + actionResult;
			int stateResult = 0;
			for (String condition : state_)
				stateResult += condition.hashCode();
			result = prime * result + stateResult;
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
			PreGoalInformation other = (PreGoalInformation) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (actionTerms_ == null) {
				if (other.actionTerms_ != null)
					return false;
			} else if (!actionTerms_.containsAll(other.actionTerms_))
				return false;
			else if (!other.actionTerms_.containsAll(actionTerms_))
				return false;
			if (state_ == null) {
				if (other.state_ != null)
					return false;
			} else if (!state_.containsAll(other.state_))
				return false;
			else if (!other.state_.containsAll(state_))
				return false;
			return true;
		}
	}

	/**
	 * A class to represent a range of values for a condition. Used for
	 * recording the maximum values recorded by the covered rules.
	 * 
	 * @author Sam Sarjant
	 */
	private class RangedCondition {
		/** The condition (distanceDot, etc) */
		private String condition_;
		/** The minimum value. */
		private double minimum_;

		/** The maximum value */
		private double maximum_;

		/**
		 * The ranged condition constructor.
		 * 
		 * @param condition
		 *            The condition.
		 * @param minimum
		 *            The minimum value.
		 * @param maximum
		 *            The maximum value.
		 */
		public RangedCondition(String condition, double minimum, double maximum) {
			condition_ = condition;
			minimum_ = minimum;
			maximum_ = maximum;
		}

		public String getCondition() {
			return condition_;
		}

		public double getMinimum() {
			return minimum_;
		}

		public double getMaximum() {
			return maximum_;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((condition_ == null) ? 0 : condition_.hashCode());
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
			RangedCondition other = (RangedCondition) obj;
			if (condition_ == null) {
				if (other.condition_ != null)
					return false;
			} else if (!condition_.equals(other.condition_))
				return false;
			return true;
		}

		/**
		 * Checks if the ranges of two RangedConditions are equal.
		 * 
		 * @param coveredRange
		 *            The other ranged condition.
		 * @return True if the ranges are equal, false otherwise.
		 */
		public boolean equalRange(RangedCondition coveredRange) {
			if ((coveredRange.minimum_ == minimum_)
					&& (coveredRange.maximum_ == maximum_))
				return true;
			return false;
		}

		@Override
		public String toString() {
			return condition_ + " {" + minimum_ + "-" + maximum_ + "}";
		}
	}
}
