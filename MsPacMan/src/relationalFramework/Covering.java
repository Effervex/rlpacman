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
import java.util.regex.Pattern;

import jess.Fact;
import jess.Rete;
import jess.ValueVector;

/**
 * A class which deals specifically with covering rules from a state.
 * 
 * @author Samuel J. Sarjant
 */
public class Covering {
	private static final char STARTING_CHAR = 'X';
	private static final char MODULO_CHAR = 'Z' + 1;
	private static final char FIRST_CHAR = 'A';
	private static final int MAX_STATE_UNIFICATION_INACTIVITY = 10;
	private static final List<GuidedRule> EMPTY_LIST = new ArrayList<GuidedRule>();

	/**
	 * The pre-goal state for each action predicate, for use in covering
	 * specialisations.
	 */
	private Map<String, List<String>> preGoalState_;

	/**
	 * The final action terms, either constants or variables for each action
	 * predicate.
	 */
	private Map<String, List<String>> preGoalActionTerms_;

	/** The last time since the pre-goal state changed. */
	private int preGoalUnificationInactivity_ = 0;

	/**
	 * If this coverer should be creating new rules or just refining existing
	 * ones.
	 */
	private boolean createNewRules_;

	public Covering() {
		clearPreGoalState();
	}

	/**
	 * Covers a state by creating a rule for every action type present in the
	 * valid actions for the state.
	 * 
	 * @param state
	 *            The state of the environment, containing the valid actions.
	 * @param nonLGGCoveredRules
	 *            A starting point for the rules, if any exist.
	 * @param createNewRules
	 *            If this should cover new rules.
	 * @return A list of guided rules, one for each action type.
	 */
	public List<GuidedRule> coverState(Rete state,
			MultiMap<String, GuidedRule> nonLGGCoveredRules,
			boolean createNewRules) throws Exception {
		// If we're not creating new rules and the nonLGGs are empty, return
		if (!createNewRules && nonLGGCoveredRules.allValuesEmpty())
			return EMPTY_LIST;
		createNewRules_ = createNewRules;

		List<String> constants = StateSpec.getInstance().getConstants();

		// The relevant facts which contain the key term
		MultiMap<String, Fact> relevantConditions = new MultiMap<String, Fact>();
		Fact actionFact = compileRelevantConditionMap(state, relevantConditions);

		// Maintain a mapping for each action, to be used in unification between
		// actions
		List<GuidedRule> generalActions = new ArrayList<GuidedRule>();

		// A multimap with the action predicate as key and args as values.
		MultiMap<String, String> validActions = arrangeActions(actionFact);
		for (String action : validActions.keySet()) {
			// Format any previous rules into a list of strings
			List<GuidedRule> previousRules = nonLGGCoveredRules.get(action);
			if (previousRules == null)
				previousRules = new ArrayList<GuidedRule>();

			// Cover the state, using the previous rules and/or newly created
			// rules
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
	 * (facts using only anonymous terms).
	 * 
	 * @param oldState
	 *            The old state to be unified with. This may be modified.
	 * @param newState
	 *            The new state being unified with.
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
	public int unifyStates(List<String> oldState, Collection<String> newState,
			List<String> oldTerms, List<String> newTerms) {
		boolean hasChanged = false;

		// If the terms don't match up, create a replacement map
		Map<String, String> oldReplacementMap = new HashMap<String, String>();
		Map<String, String> newReplacementMap = new HashMap<String, String>();
		for (int i = 0; i < oldTerms.size(); i++) {
			// If this index of terms don't match up
			String oldTerm = oldTerms.get(i);
			String newTerm = newTerms.get(i);
			if (!oldTerm.equals(newTerm)) {
				boolean bothVariables = true;
				String variable = getVariableTermString(i);
				// Replace old term if necessary
				if (oldTerm.charAt(0) != '?') {
					oldReplacementMap.put(oldTerm, variable);
					// hasChanged = true;
					bothVariables = false;
				}
				if (newTerm.charAt(0) != '?') {
					newReplacementMap.put(newTerm, variable);
					bothVariables = false;
				}

				// Check that both slots aren't inequal variables
				if (bothVariables) {
					return -1;
				}
			}
		}

		// For each item in the old state, see if it is present in the new state
		List<String> oldStateRepl = new ArrayList<String>();
		for (String oldStateFact : oldState) {
			String modFact = unifyFact(oldStateFact, newState,
					oldReplacementMap, newReplacementMap, oldTerms);

			// Check for a change
			if (!oldStateFact.equals(modFact))
				hasChanged = true;

			if ((modFact != null) && (!oldStateRepl.contains(modFact)))
				oldStateRepl.add(modFact);
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
	 * Unifies a single fact by searching for the term itself or a generalised
	 * form of the term within a collection. The search is special because it
	 * can generalise terms to anonymous terms if necessary for unification.
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
	 * @return The unified version of the fact (possibly more general than the
	 *         input fact) or null if no unification.
	 */
	private String unifyFact(String fact, Collection<String> unityFacts,
			Map<String, String> factReplacementMap,
			Map<String, String> unityReplacementMap, List<String> factTerms) {
		// Split the fact up and apply the replacements
		String[] factSplit = StateSpec.splitFact(fact);

		// Maintain a check on what is the best unification (should better ones
		// be present)
		String[] bestUnified = null;
		int generalisation = 0;
		List<String> bestTerms = null;
		// Check against each item in the unity state,
		for (Iterator<String> iter = unityFacts.iterator(); iter.hasNext();) {
			String[] unitySplit = StateSpec.splitFact(iter.next());

			// Check it if the same fact
			if (factSplit[0].equals(unitySplit[0])) {
				String[] unification = new String[factSplit.length];
				List<String> thisTerms = new ArrayList<String>(factTerms);
				int thisGeneralness = 0;
				boolean notAnonymous = false;

				// Unify each term
				for (int i = 1; i < factSplit.length; i++) {
					// If either are anonymous, the unification must be
					// anonymous
					if (factSplit[i].equals(StateSpec.ANONYMOUS)
							|| unitySplit.equals(StateSpec.ANONYMOUS)) {
						unification[i] = StateSpec.ANONYMOUS;
						// If the fact was not originally anonymous, increment
						// generalisation
						if (factSplit[i].equals(StateSpec.ANONYMOUS))
							thisGeneralness++;
					} else if (factSplit[i].equals(unitySplit[i])
							&& equalReplacements(factReplacementMap
									.get(factSplit[i]), unityReplacementMap
									.get(unitySplit[i]))) {
						// If the two are the same term (not anonymous) and
						// their replacements match up, use that
						unification[i] = factSplit[i];
						notAnonymous = true;
					} else {
						// Apply replacement operators
						// Use the unification array to hold a temp value
						if (factReplacementMap.containsKey(factSplit[i])) {
							unification[i] = factReplacementMap
									.get(factSplit[i]);
							Collections.replaceAll(thisTerms, factSplit[i],
									unification[i]);
						} else
							unification[i] = factSplit[i];

						if (unityReplacementMap.containsKey(unitySplit[i]))
							unitySplit[i] = unityReplacementMap
									.get(unitySplit[i]);

						// If the replaced values are equal, use them
						if (unitySplit[i].equals(unification[i])) {
							thisGeneralness++;
							notAnonymous = true;
						} else {
							// Failing that simply use an anonymous variable
							unification[i] = StateSpec.ANONYMOUS;
							thisGeneralness++;
						}
					}
				}

				// Store if:
				// 1. The fact is not fully anonymous
				// 2. The fact is less general than the current best
				if (notAnonymous
						&& ((bestUnified == null) || (thisGeneralness < generalisation))) {
					bestUnified = unification;
					bestTerms = thisTerms;
					generalisation = thisGeneralness;
				}
			}
		}

		if (bestUnified == null)
			return null;

		// Setting the fact terms
		factTerms.clear();
		factTerms.addAll(bestTerms);
		bestUnified[0] = factSplit[0];
		return StateSpec.reformFact(bestUnified);
	}

	/**
	 * Simple equality method for comparing two possibly null strings.
	 * 
	 * @param string
	 *            String 1.
	 * @param string2
	 *            String 2.
	 * @return True if either strings are null, or if both are equal.
	 */
	private boolean equalReplacements(String string, String string2) {
		if ((string == null) || (string2 == null))
			return true;
		if (!string.equals(string2))
			return false;
		return true;
	}

	/**
	 * Unifies action rules together into one general all-covering rule.
	 * 
	 * @param argsList
	 *            A heuristically sorted list of actions for a single predicate.
	 * @param relevantConditions
	 *            The relevant conditions for each term in the state.
	 * @param actionPred
	 *            The action predicate spawning this rule.
	 * @param previousRules
	 *            A pre-existing list of rules that each need to be generalised.
	 *            Note the rules are exclusive from one-another.
	 * @param constants
	 *            TODO
	 * @return A Rule representing a general action.
	 */
	private List<GuidedRule> unifyActionRules(List<String> argsList,
			MultiMap<String, Fact> relevantConditions, String actionPred,
			List<GuidedRule> previousRules, List<String> constants) {
		// The terms in the action
		String[] action = new String[1 + StateSpec.getInstance().getActions()
				.get(actionPred).size()];
		action[0] = actionPred;

		Iterator<String> argIter = argsList.iterator();
		// Do until:
		// 1) We have no actions left to look at
		// 2) Every rule is not yet minimal
		Set<GuidedRule> minimalRules = null;
		while ((argIter.hasNext())
				&& ((minimalRules == null) || (minimalRules.size() < previousRules
						.size()))) {
			String arg = argIter.next();
			List<Fact> actionFacts = new ArrayList<Fact>();

			String[] terms = arg.split(" ");
			// Find the facts containing the same (useful) terms in the action
			for (int i = 0; i < terms.length; i++) {
				List<Fact> termFacts = relevantConditions.get(terms[i]);
				for (Fact termFact : termFacts) {
					if (!actionFacts.contains(termFact))
						actionFacts.add(termFact);
				}

				// Format the action
				if (constants.contains(terms[i]))
					action[i + 1] = terms[i];
				else
					action[i + 1] = getVariableTermString(i);
			}

			// Inversely substitute the terms for variables (in string form)
			GuidedRule inverseSubbed = new GuidedRule(inverselySubstitute(
					actionFacts, terms, constants), StateSpec
					.reformFact(action), false);

			if (minimalRules == null)
				minimalRules = new HashSet<GuidedRule>();

			// Unify with the previous rules, unless it causes the rule to
			// become invalid
			if (previousRules.isEmpty()) {
				// Only create new rules if necessary.
				if (createNewRules_) {
					previousRules.add(inverseSubbed);
					if (inverseSubbed.isLGG())
						minimalRules.add(inverseSubbed);
				}
			} else {
				// Unify with each rule in the previous rule/s
				boolean createNewRule = true;
				for (int i = 0; i < previousRules.size(); i++) {
					GuidedRule prev = previousRules.get(i);

					// Unify the prev rule and the inverse subbed rule
					List<String> ruleConditions = prev.getConditions(true);
					List<String> ruleTerms = prev.getActionTerms();
					int changed = unifyStates(ruleConditions, inverseSubbed
							.getConditions(false), ruleTerms, inverseSubbed
							.getActionTerms());

					if (changed == 1) {
						prev.setActionTerms(ruleTerms);
						prev.setConditions(ruleConditions, true);
					}

					if (prev.isLGG()) {
						// The rule is minimal
						minimalRules.add(prev);
						createNewRule = false;
					} else {
						// The rule isn't minimal (but it changed)
						createNewRule = false;
					}
				}

				// If all rules became invalid, we need a new rule
				if (createNewRule) {
					// Only create new rules if necessary.
					if (createNewRules_) {
						previousRules.add(inverseSubbed);
						if (inverseSubbed.isLGG())
							minimalRules.add(inverseSubbed);
					}
				}
			}
		}

		// Return the old, possibly modified rules and any new ones.
		for (GuidedRule prev : previousRules) {
			prev.expandConditions();
			prev.incrementStatesCovered();
		}
		return previousRules;
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
			// Replace all constant terms in the action with matching variables
			// or anonymous variables
			for (int j = 1; j < factSplit.length; j++) {
				// If the term isn't a constant, replace it with a variable
				if (!constants.contains(factSplit[j])) {
					String replacementTerm = termMapping.get(factSplit[j]);
					if (replacementTerm != null)
						factSplit[j] = replacementTerm;
					else
						factSplit[j] = "?";
				} else {
					// Add the constant type predicate
					String[] typePred = new String[2];
					typePred[0] = StateSpec.getInstance().getTypePredicate(
							factSplit[0], j - 1);
					typePred[1] = factSplit[j];

					String formedType = StateSpec.reformFact(typePred);
					if (!substitution.contains(formedType))
						substitution.add(formedType);
				}
			}

			// Reform the fact and add it back
			String reformedFact = StateSpec.reformFact(factSplit);
			if (!substitution.contains(reformedFact))
				substitution.add(reformedFact);
		}
		return substitution;
	}

	/**
	 * Arranges the collection of actions into a heuristical ordering which
	 * attempts to find maximally dissimilar actions of the same type. The list
	 * is ordered such that the actions of the same predicate, each one with
	 * different arguments from the last, are first, followed by randomly
	 * ordered remaining actions.
	 * 
	 * @param validActions
	 *            The set of valid actions.
	 * @return A multimap of each action predicate, with the action as key and
	 *         the args as values, containing the heuristically ordered actions.
	 */
	private MultiMap<String, String> arrangeActions(Fact validActions)
			throws Exception {
		// Initialise a map for each action predicate (move {"a b" "b c"...})
		MultiMap<String, String> actionsMap = new MultiMap<String, String>();
		// A multimap which deals with each argument (move {{a,b,c} {b,e,d}})
		MultiMap<String, Set<String>> usedTermsMap = new MultiMap<String, Set<String>>();

		// A multimap recording similar actions (move {"a b" "a c"...})
		MultiMap<String, String> notUsedMap = new MultiMap<String, String>();
		// For each action
		for (String action : StateSpec.getInstance().getActions().keySet()) {
			ValueVector args = validActions.getSlotValue(action)
					.listValue(null);
			// Run through the valid args for the action
			for (int i = 0; i < args.size(); i++) {
				String arg = args.get(i).stringValue(null);
				if (isDissimilarAction(arg, action, usedTermsMap)) {
					actionsMap.put(action, arg);
				} else {
					notUsedMap.put(action, arg);
				}
			}
		}

		// If we have some actions not used (likely, then shuffle the ordering
		// and add them to the end of the actions map.
		if (!notUsedMap.isKeysEmpty()) {
			for (List<String> notUsed : notUsedMap.valuesLists()) {
				Collections.shuffle(notUsed);
			}
			actionsMap.putAll(notUsedMap);
		}

		return actionsMap;
	}

	/**
	 * Is the given action dissimilar from the already seen actions? This is
	 * measured by which terms have already been seen in their appropriate
	 * predicate slots.
	 * 
	 * @param arg
	 *            The arguments to the action "a b".
	 * @param action
	 *            The action name.
	 * @param usedTermsMap
	 *            The already used terms mapping.
	 * @return True if the action is dissimilar, false otherwise.
	 */
	private boolean isDissimilarAction(String arg, String action,
			MultiMap<String, Set<String>> usedTermsMap) {
		String[] terms = arg.split(" ");
		// Run through each term
		for (int i = 0; i < terms.length; i++) {
			// Checking if the term set already exists
			Set<String> usedTerms = usedTermsMap.getIndex(action, i);
			if (usedTerms == null) {
				usedTerms = new HashSet<String>();
				usedTermsMap.put(action, usedTerms);
			}

			// If the term has already been used (but isn't a state or state
			// spec term), return false
			if (usedTerms.contains(terms[i]))
				return false;
		}

		// If the terms have not been used before, add them all to their
		// appropriate sets.
		for (int i = 0; i < terms.length; i++) {
			usedTermsMap.getIndex(action, i).add(terms[i]);
		}

		return true;
	}

	/**
	 * Compiles the relevant term conditions from the state into map format,
	 * with the term as the key and the fact as the value. This makes finding
	 * relevant conditions a quick matter.
	 * 
	 * @param state
	 *            The state containing the conditions.
	 * @param relevantConditions
	 *            The relevant conditions mapping to be filled.
	 * @return The action fact.
	 */
	@SuppressWarnings("unchecked")
	private Fact compileRelevantConditionMap(Rete state,
			MultiMap<String, Fact> relevantConditions) {
		Fact actionFact = null;

		for (Iterator<Fact> factIter = state.listFacts(); factIter.hasNext();) {
			Fact stateFact = factIter.next();
			// Ignore the type, inequal and actions pred
			String[] split = StateSpec.splitFact(stateFact.toString());
			if (StateSpec.getInstance().isUsefulPredicate(split[0])) {
				for (int i = 1; i < split.length; i++) {
					relevantConditions.putContains(split[i], stateFact);
				}
			} else if (split[0].equals(StateSpec.VALID_ACTIONS))
				// Find the action fact
				actionFact = stateFact;
		}

		return actionFact;
	}

	/**
	 * Specialises a rule towards conditions seen in the pre-goal state.
	 * 
	 * @param rule
	 *            The rule to specialise.
	 * @return All single-step mutations the rule can take towards matching the
	 *         pre-goal state.
	 */
	public List<GuidedRule> specialiseToPreGoal(GuidedRule rule) {
		List<GuidedRule> mutants = new ArrayList<GuidedRule>();

		// Get a single fact or variable specialisation from the pre goal for
		// each mutation
		String actionPred = rule.getActionPredicate();
		List<String> preGoalState = preGoalState_.get(actionPred);

		// If we have a pre goal state
		if (preGoalState != null) {
			List<String> ruleConditions = rule.getConditions(true);

			// Form a replacement terms map.
			Map<String, String> replacementTerms = new HashMap<String, String>();
			List<String> ruleTerms = rule.getActionTerms();
			List<String> preGoalTerms = preGoalActionTerms_.get(actionPred);
			for (int i = 0; i < preGoalTerms.size(); i++) {
				if (!preGoalTerms.get(i).equals(ruleTerms.get(i)))
					replacementTerms.put(preGoalTerms.get(i), ruleTerms.get(i));
			}

			// Run through each fact in the pre-goal state, using any relevant
			// ones for mutations.
			for (String preGoalFact : preGoalState) {
				// Replace pre-goal action terms with local rule terms
				for (String replaceKey : replacementTerms.keySet()) {
					preGoalFact = preGoalFact.replaceAll(" "
							+ Pattern.quote(replaceKey) + "(?=( |\\)))",
							" " + replacementTerms.get(replaceKey));
				}

				// If the fact isn't in the rule, we have a mutation
				if (!ruleConditions.contains(preGoalFact)) {
					List<String> mutatedConditions = new ArrayList<String>(
							ruleConditions);
					mutatedConditions.add(preGoalFact);
					GuidedRule mutant = new GuidedRule(mutatedConditions, rule
							.getAction(), true);
					mutant.expandConditions();
					mutants.add(mutant);
				}
			}

			
			
			// Checking for constant specialisations
			MultiMap<String, String> replacementMutants = new MultiMap<String, String>();
			for (String cond : ruleConditions) {
				for (String replaceKey : replacementTerms.keySet()) {
					String action = rule.getAction().replaceAll(
							" "
									+ Pattern.quote(replacementTerms
											.get(replaceKey)) + "(?=( |\\)))",
							" " + replaceKey);
					String repCond = cond.replaceAll(" "
							+ Pattern.quote(replacementTerms.get(replaceKey))
							+ "(?=( |\\)))", " " + replaceKey);
					replacementMutants.putContains(action, repCond);
				}
			}
			
			// For every action in the replacement mutants
			for (String action : replacementMutants.keySet()) {
				List<String> conditions = replacementMutants.get(action);
				String[] actionSplit = StateSpec.splitFact(action);
				List<String> actionList = new ArrayList<String>();
				for (int i = 1; i < actionSplit.length; i++)
					actionList.add(actionSplit[i]);
				
				// If the unification fits (no change), then the replacement works
				if (unifyStates(conditions, preGoalState, actionList, preGoalTerms) == 0) {
					GuidedRule mutant = new GuidedRule(conditions, action, true);
					mutant.expandConditions();
					mutants.add(mutant);
				}
			}
		}

		rule.checkInequals();
		return mutants;
	}

	/**
	 * Forms the pre-goal state by adding to it a pre-goal state the agent has
	 * seen (or was given). This method forms the pre-goal state by finding the
	 * bare minimal conditions seen in every pre-goal state.
	 * 
	 * @param preGoalState
	 *            The pre-goal state seen by the agent.
	 * @param action
	 *            The final action taken by the agent.
	 * @return True if the state was last active at most
	 *         MAX_STATE_UNIFICATION_INACTIVITY steps ago.
	 */
	public boolean formPreGoalState(Collection<Fact> preGoalState, String action) {
		// If the state isn't yet settled, try unification
		if (!isPreGoalSettled()) {
			// Inversely substitute the old pregoal state
			String[] actionSplit = StateSpec.splitFact(action);
			String[] actionTerms = Arrays.copyOfRange(actionSplit, 1,
					actionSplit.length);
			// The actions become constants if possible
			List<String> newStateTerms = new ArrayList<String>();
			for (String actionTerm : actionTerms)
				newStateTerms.add(actionTerm);
			Collection<String> preGoalStringState = inverselySubstitute(
					preGoalState, actionTerms, newStateTerms);
			removeUselessFacts(preGoalStringState);

			// Unify with the old state
			List<String> oldPreGoalState = preGoalState_.get(actionSplit[0]);
			if (oldPreGoalState == null) {
				preGoalState_.put(actionSplit[0],
						(List<String>) preGoalStringState);
				preGoalActionTerms_.put(actionSplit[0], newStateTerms);
			} else {
				// Unify the two states and check if it has changed at all.
				int result = unifyStates(preGoalState_.get(actionSplit[0]),
						preGoalStringState, preGoalActionTerms_
								.get(actionSplit[0]), newStateTerms);

				// If the states unified, reset the counter, otherwise
				// increment.
				// Hopefully there aren't any errors.
				if (result == 1)
					preGoalUnificationInactivity_ = 0;
				else if (result == 0)
					preGoalUnificationInactivity_++;
				else if (result == -1)
					throw new RuntimeException(
							"Pre-goal states did not unify: "
									+ preGoalState_.get(actionSplit[0]) + ", "
									+ preGoalStringState);
			}
			return true;
		}
		return false;
	}

	/**
	 * Clears the pregoal state.
	 */
	public void clearPreGoalState() {
		preGoalState_ = new HashMap<String, List<String>>();
		preGoalActionTerms_ = new HashMap<String, List<String>>();
		preGoalUnificationInactivity_ = 0;
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
		preGoalState_.put(split[0], preGoal);
		List<String> terms = new ArrayList<String>();
		for (int i = 1; i < split.length; i++)
			terms.add(split[i]);
		preGoalActionTerms_.put(split[0], terms);
	}

	/**
	 * If the pre-goal state has settled to a stable state.
	 * 
	 * @return True if the state has settled, false otherwise.
	 */
	public boolean isPreGoalSettled() {
		if (preGoalUnificationInactivity_ < MAX_STATE_UNIFICATION_INACTIVITY)
			return false;
		return true;
	}

	/**
	 * Gets the pre-goal general state, seen by the agent.
	 * 
	 * @param action
	 *            The pre-goal action.
	 * @return The pre-goal state, in the form of a list of facts.
	 */
	public List<String> getPreGoalState(String action) {
		return preGoalState_.get(action);
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
		return preGoalActionTerms_.get(action);
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
				% (MODULO_CHAR - FIRST_CHAR));
		return "?" + variable;
	}

	public static String getConstantTermString(Object obj) {
		return "[" + obj + "]";
	}

	/**
	 * If a pre-goal has been initialised.
	 * 
	 * @return True if there is a pre-goal, false otherwise.
	 */
	public boolean hasPreGoal() {
		if (preGoalState_.isEmpty())
			return false;
		return true;
	}
}
