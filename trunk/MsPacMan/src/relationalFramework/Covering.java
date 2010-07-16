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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private static final char MODULO_CHAR = 'Z' + 1;
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

	/** A suffix numbered variable to use when defining new ranged variables. */
	private int rangeIndex_ = 0;

	public Covering(int numActions) {
		clearPreGoalState(numActions);
	}

	/**
	 * Covers a state by creating a rule for every action type present in the
	 * valid actions for the state.
	 * 
	 * @param state
	 *            The state of the environment, containing the valid actions.
	 * @param validActions
	 *            The set of valid actions ot choose from.
	 * @param coveredRules
	 *            A starting point for the rules, if any exist.
	 * @return A list of guided rules, one for each action type.
	 */
	public List<GuidedRule> coverState(Rete state,
			MultiMap<String, String> validActions,
			MultiMap<String, GuidedRule> coveredRules) throws Exception {
		List<String> constants = StateSpec.getInstance().getConstants();

		// The relevant facts which contain the key term
		MultiMap<String, Fact> relevantConditions = compileRelevantConditionMap(state);

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
	public int unifyStates(List<String> oldState, Collection<String> newState,
			List<String> oldTerms, List<String> newTerms) {
		boolean hasChanged = false;

		// If the terms don't match up, create a replacement map
		Map<String, String> oldReplacementMap = new HashMap<String, String>();
		Map<String, String> newReplacementMap = new HashMap<String, String>();
		if (!createReplacementMaps(oldTerms, newTerms, oldReplacementMap,
				newReplacementMap))
			return -1;

		// For each item in the old state, see if it is present in the new state
		List<String> oldStateRepl = new ArrayList<String>();
		for (String oldStateFact : oldState) {
			Collection<String> modFacts = unifyFact(oldStateFact, newState,
					oldReplacementMap, newReplacementMap, oldTerms);

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
			List<String> newTerms, Map<String, String> oldReplacementMap,
			Map<String, String> newReplacementMap) {
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
					// hasChanged = true;
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
	 * @return The unified version of the fact (possibly more general than the
	 *         input fact) or null if no unification.
	 */
	private Collection<String> unifyFact(String fact,
			Collection<String> unityFacts,
			Map<String, String> factReplacementMap,
			Map<String, String> unityReplacementMap, List<String> factTerms) {
		Collection<String> result = new ArrayList<String>();

		// Split the fact up and apply the replacements
		String[] factSplit = StateSpec.splitFact(fact);

		// Maintain a check on what is the best unification (should better ones
		// be present)
		int generalisation = Integer.MAX_VALUE;
		// Check against each item in the unity state,
		// TODO Speed this up by assigning the facts to a map so tey are quickly
		// accessible.
		for (Iterator<String> iter = unityFacts.iterator(); iter.hasNext();) {
			String[] unitySplit = StateSpec.splitFact(iter.next());

			// Check it if the same fact
			if (factSplit[0].equals(unitySplit[0])) {
				String[] unification = new String[factSplit.length];
				int thisGeneralness = 0;
				boolean notAnonymous = false;

				// Unify each term
				for (int i = 1; i < factSplit.length; i++) {
					// Apply the replacements
					String factTerm = (factReplacementMap
							.containsKey(factSplit[i])) ? factReplacementMap
							.get(factSplit[i]) : factSplit[i];
					String unityTerm = (unityReplacementMap
							.containsKey(unitySplit[i])) ? unityReplacementMap
							.get(unitySplit[i]) : unitySplit[i];

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
						result.add(StateSpec.reformFact(unification));
					}
				}
			}
		}

		if (!result.isEmpty()) {
			// Setting the fact terms
			for (String factKey : factReplacementMap.keySet())
				Collections.replaceAll(factTerms, factKey, factReplacementMap
						.get(factKey));
		}

		return result;
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
	 * Unifies action rules together into one general all-covering rule.
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
		while (argIter.hasNext()) {
			String arg = argIter.next();
			List<Fact> actionFacts = new ArrayList<Fact>();

			String[] terms = arg.split(" ");
			// Find the facts containing the same (useful) terms in the action
			for (int i = 0; i < terms.length; i++) {
				List<Fact> termFacts = relevantConditions.get(terms[i]);
				if (termFacts != null) {
					for (Fact termFact : termFacts) {
						if (!actionFacts.contains(termFact))
							actionFacts.add(termFact);
					}
				}

				action[i + 1] = terms[i];
			}

			// Inversely substitute the terms for variables (in string form)
			GuidedRule inverseSubbed = new GuidedRule(
					convertFactsToStrings(actionFacts), StateSpec
							.reformFact(action), false);

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
					List<String> ruleConditions = prev.getConditions(true);
					List<String> ruleTerms = prev.getActionTerms();
					int changed = unifyStates(ruleConditions, inverseSubbed
							.getConditions(false), ruleTerms, inverseSubbed
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
	 * @return A collection of strings of the same facts.
	 */
	private Collection<String> convertFactsToStrings(List<Fact> facts) {
		Collection<String> strings = new ArrayList<String>();
		for (Fact fact : facts) {
			strings.add(fact.toString().replaceAll("\\(.+?::", "("));
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
	private MultiMap<String, Fact> compileRelevantConditionMap(Rete state) {
		MultiMap<String, Fact> relevantConditions = new MultiMap<String, Fact>();

		for (Iterator<Fact> factIter = state.listFacts(); factIter.hasNext();) {
			Fact stateFact = factIter.next();
			// Ignore the type, inequal and actions pred
			String[] split = StateSpec.splitFact(stateFact.toString());
			if (StateSpec.getInstance().isUsefulPredicate(split[0])) {
				for (int i = 1; i < split.length; i++) {
					// Ignore numerical terms
					if (!StateSpec.isNumber(split[i]))
						relevantConditions.putContains(split[i], stateFact);
				}
			}
		}

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
	public Collection<GuidedRule> specialiseToPreGoal(GuidedRule rule) {
		Set<GuidedRule> mutants = new HashSet<GuidedRule>();

		// Get a single fact or variable specialisation from the pre goal for
		// each mutation
		String actionPred = rule.getActionPredicate();
		PreGoalInformation preGoal = preGoals_.get(actionPred);

		// If we have a pre goal state
		if (preGoal != null) {
			List<String> preGoalState = preGoal.getState();
			List<String> ruleConditions = rule.getConditions(true);

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
							condSplit, rule.getQueryParameters());
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
						List<String> mutatedConditions = new ArrayList<String>(
								ruleConditions);
						mutatedConditions.add(preGoalFact);
						GuidedRule mutant = new GuidedRule(mutatedConditions,
								rule.getAction(), true);
						mutant.setQueryParams(rule.getQueryParameters());
						mutant.expandConditions();
						mutants.add(mutant);
					}
				}
			}
		}

		rule.checkInequals();
		return mutants;
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
	 * @return True if the cond and pre-goal fact match.
	 */
	private boolean factMutation(String ruleAction, Set<GuidedRule> mutants,
			String actionPred, List<String> preGoalState,
			List<String> ruleConditions, Map<String, String> replacementTerms,
			Map<String, String> reverseReplacementTerms,
			List<String> ruleTerms, List<String> preGoalTerms,
			String[] preGoalSplit, String cond, String[] condSplit,
			List<String> queryParameters) {
		// 4a. If the fact pred matches the rule pred, try replacing all
		// occurrences of each cond term with the fact term in every cond
		boolean hasPred = false;
		if (condSplit[0].equals(preGoalSplit[0])) {
			hasPred = true;

			// Run through each term
			boolean ceaseMutation = false;
			for (int i = 1; i < condSplit.length; i++) {
				// Refining ranged numerical variables
				int suchThatIndex = preGoalSplit[i].indexOf("&:");
				String preGoalITerm = preGoalSplit[i];
				if (suchThatIndex != -1)
					preGoalITerm = preGoalSplit[i].substring(0, suchThatIndex);

				// Finding the replacement term
				String replacedTerm = (replacementTerms
						.containsKey(preGoalITerm)) ? replacementTerms
						.get(preGoalITerm) : preGoalITerm;

				// 5. Replacing variable terms
				if (condSplit[i].charAt(0) == '?') {
					String backup = condSplit[i];
					// The pregoal term is not anonymous
					if (!ceaseMutation
							&& !preGoalITerm.equals(StateSpec.ANONYMOUS)) {
						// 5a. If the cond term is ? and the preds match, use
						// the fact term
						boolean continueCreation = true;
						// If the condition is anonymous, replace it with any
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
							String newCond = StateSpec.reformFact(condSplit);
							if (!ruleConditions.contains(newCond)) {
								List<String> mutatedConditions = new ArrayList<String>(
										ruleConditions);
								mutatedConditions.set(mutatedConditions
										.indexOf(cond), newCond);
								GuidedRule mutant = new GuidedRule(
										mutatedConditions, ruleAction, true);
								mutant.setQueryParams(queryParameters);
								mutant.expandConditions();
								mutants.add(mutant);
							}

							// Revert the cond split back
							condSplit[i] = backup;
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

					// If we're dealing with a numerical term
					if (suchThatIndex != -1) {
						String[] betweenRangeSplit = preGoalSplit[i].split(" ");
						// Find the values
						double min = Double.parseDouble(betweenRangeSplit[2]);
						double max = Double
								.parseDouble(betweenRangeSplit[3].substring(0,
										betweenRangeSplit[3].length() - 1));
						double rangeSplit = (max - min) / NUM_DISCRETE_RANGES;
						// Create a mutant for each range
						for (int r = 0; r < NUM_DISCRETE_RANGES; r++) {
							String replacementTerm = preGoalITerm
									+ "&:("
									+ StateSpec.BETWEEN_RANGE
									+ " "
									+ preGoalITerm
									+ " "
									+ (min + rangeSplit * r)
									+ " "
									+ (max - rangeSplit
											* (NUM_DISCRETE_RANGES - r - 1))
									+ ")";
							GuidedRule mutant = replaceActionTerm(actionPred,
									preGoalState, preGoalTerms, ruleConditions,
									condSplit[i], replacementTerm,
									replActionTerms, false);
							if (mutant != null)
								mutants.add(mutant);
						}
					} else {
						GuidedRule mutant = replaceActionTerm(actionPred,
								preGoalState, preGoalTerms, ruleConditions,
								condSplit[i], reverseReplacementTerms
										.get(condSplit[i]), replActionTerms,
								true);
						if (mutant != null)
							mutants.add(mutant);
					}

					// Whatever the outcome, remove the reverse replacement
					// as it has already been tested.
					reverseReplacementTerms.remove(condSplit[i]);
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
			List<String> ruleConditions, String replacedTerm,
			String replacementTerm, List<String> replActionTerms,
			boolean requireUnification) {
		// Replace the variables with their replacements.
		List<String> replacementConds = new ArrayList<String>(ruleConditions
				.size());
		for (String condition : ruleConditions) {
			// Replace variables
			String replCond = condition.replaceAll(" "
					+ Pattern.quote(replacedTerm) + "(?=( |\\)))", " "
					+ replacementTerm);
			replacementConds.add(replCond);
		}

		// 4b. If the new cond is valid (through unification),
		// keep
		// it and note that the replacement has occurred
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
	 * Clears the pregoal state.
	 */
	public void clearPreGoalState(int numActions) {
		preGoals_ = new HashMap<String, PreGoalInformation>(numActions);
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
	}
}
