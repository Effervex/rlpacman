package relationalFramework;

import java.io.Serializable;
import java.util.ArrayList;
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

import relationalFramework.agentObservations.AgentObservations;
import relationalFramework.agentObservations.BackgroundKnowledge;
import relationalFramework.agentObservations.PreGoalInformation;
import relationalFramework.agentObservations.RangedCondition;

import jess.Fact;
import jess.Rete;

/**
 * A class which deals specifically with covering rules from a state.
 * 
 * @author Samuel J. Sarjant
 */
public class Covering implements Serializable {
	private static final long serialVersionUID = -902117668118335074L;
	/** The starting character for variables. */
	private static final char STARTING_CHAR = 'X';
	/** The final character for variables. */
	private static final char MODULO_LETTERS = 26;
	/** The first character for variables. */
	private static final char FIRST_CHAR = 'A';
	/** The prefix for range variables. */
	public static final String RANGE_VARIABLE_PREFIX = "__Num";
	/** The number of discretised ranges the rules are split into. */
	public static final int NUM_DISCRETE_RANGES = 4;

	/** The incrementing unique index of ranged values. */
	private int rangeIndex_ = 0;
	/** The observations the agent has made about the environment. */
	private AgentObservations ao_;
	/**
	 * The backup pre-goal information for when the agent observations object is
	 * migrated.
	 */
	private Map<String, PreGoalInformation> backupPreGoals_;

	/**
	 * Basic constructor.
	 */
	public Covering() {
		ao_ = new AgentObservations();
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
		ao_.scanState(StateSpec.extractFacts(state));

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
			// rules while noting valid conditions for later rule mutation\
			List<GuidedRule> actionRules = unifyActionRules(validActions
					.get(action), action, previousRules, constants);
			generalActions.addAll(actionRules);
		}

		return generalActions;
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
	public int unifyStates(Collection<StringFact> oldState,
			Collection<StringFact> newState, String[] oldTerms,
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
	public int unifyStates(Collection<StringFact> oldState,
			Collection<StringFact> newState, BidiMap replacementMap) {
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
	private int unifyStates(Collection<StringFact> oldState,
			Collection<StringFact> newState, String[] oldTerms,
			boolean flexibleReplacement, BidiMap oldReplacementMap,
			BidiMap newReplacementMap) {
		// Copy the new state as it is going to be modified.
		newState = new ArrayList<StringFact>(newState);

		// For each item in the old state, see if it is present in the new state
		boolean hasChanged = false;
		List<StringFact> oldStateRepl = new ArrayList<StringFact>();
		for (StringFact oldStateFact : oldState) {
			StringFact modFact = unifyFact(oldStateFact, newState,
					oldReplacementMap, newReplacementMap, oldTerms,
					flexibleReplacement);

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
				StringFact replFact = new StringFact(modFact, newReplacementMap
						.inverseBidiMap(), true);
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
				String variable = getVariableTermString(i);
				// Replace old term if necessary
				if ((oldTerm.charAt(0) != '?')
						|| (oldTerm.contains(Module.MOD_VARIABLE_PREFIX))) {
					oldReplacementMap.put(oldTerm, variable);
					bothVariables = false;
				}
				if ((newTerm.charAt(0) != '?')
						|| (newTerm.contains(Module.MOD_VARIABLE_PREFIX))) {
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
	public StringFact unifyFact(StringFact fact,
			Collection<StringFact> unityFacts, BidiMap factReplacementMap,
			BidiMap unityReplacementMap, String[] factTerms,
			boolean flexibleReplacement) {
		StringFact result = null;

		// Maintain a check on what is the best unification (should better ones
		// be present)
		int generalisation = Integer.MAX_VALUE;
		// Check against each item in the unity state.
		for (StringFact unityFact : unityFacts) {
			// Check it if the same fact
			boolean sameFact = fact.getFactName().equals(
					unityFact.getFactName())
					&& (fact.isNegated() == unityFact.isNegated());

			// If dealing with the same fact
			if (sameFact) {
				String[] factArguments = fact.getArguments();
				String[] unityArguments = unityFact.getArguments();
				String[] unification = new String[factArguments.length];
				int thisGeneralness = 0;
				boolean notAnonymous = false;
				BidiMap tempUnityReplacementMap = new DualHashBidiMap();

				// Unify each term
				for (int i = 0; i < factArguments.length; i++) {
					// Apply the replacements
					String factTerm = (String) ((factReplacementMap
							.containsKey(factArguments[i])) ? factReplacementMap
							.get(factArguments[i])
							: factArguments[i]);
					String unityTerm = findUnityReplacement(
							unityReplacementMap, unityArguments[i],
							flexibleReplacement, tempUnityReplacementMap,
							factTerm);

					// If either are anonymous, the unification must be
					// anonymous
					if (factTerm.equals(StateSpec.ANONYMOUS)
							|| ((unityTerm != null) && (unityTerm
									.equals(StateSpec.ANONYMOUS)))) {
						unification[i] = StateSpec.ANONYMOUS;
					} else if (factTerm.equals(unityTerm)) {
						// If the two are the same term (not anonymous) and
						// their replacements match up, use that
						unification[i] = factTerm;
						notAnonymous = true;
					} else if (!numericalValueCheck(factArguments[i],
							unityArguments[i], unification, i, factTerms)) {
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
						generalisation = thisGeneralness;
					}
					if (thisGeneralness == generalisation) {
						StringFact unifact = new StringFact(fact, unification,
								fact.isNegated());
						result = unifact;

						// If we're using flexible replacements, store any
						// temporary replacements created
						if (flexibleReplacement) {
							unityReplacementMap.putAll(tempUnityReplacementMap);
						}
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
				if (!factTerm.equals(StateSpec.ANONYMOUS))
					tempUnityReplacementMap.put(unityTerm, factTerm);
				return factTerm;
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

	/**
	 * Unifies action rules together into one general all-covering rule. Also
	 * notes down conditions which are true/not always true for use in later
	 * rule specialisation.
	 * 
	 * @param argsList
	 *            A heuristically sorted list of actions for a single predicate.
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
			String actionPred, List<GuidedRule> previousRules,
			List<String> constants) {
		// The terms in the action
		StringFact baseAction = StateSpec.getInstance().getActions().get(
				actionPred);

		Iterator<String> argIter = argsList.iterator();
		// Do until:
		// 1) We have no actions left to look at
		// 2) Every rule is not yet minimal
		while (argIter.hasNext()) {
			String arg = argIter.next();

			String[] args = arg.split(" ");
			StringFact action = new StringFact(baseAction, args);

			Collection<StringFact> actionFacts = ao_.gatherActionFacts(action);

			// Inversely substitute the terms for variables (in string form)
			GuidedRule inverseSubbed = new GuidedRule(actionFacts, action,
					false);

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
					SortedSet<StringFact> ruleConditions = prev
							.getConditions(true);
					String[] ruleTerms = prev.getActionTerms();
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
			SortedSet<StringFact> ruleConds = simplifyRule(prev
					.getConditions(false), null, false);
			if (ruleConds != null)
				prev.setConditions(ruleConds, false);
			prev.expandConditions();
			prev.incrementStatesCovered();
		}
		return previousRules;
	}

	/**
	 * Simplifies a rule with an optional added condition by removing any
	 * non-necessary conditions and returning either a different set of
	 * conditions, or null.
	 * 
	 * @param ruleConds
	 *            The rule conditions to simplify.
	 * @param condition
	 *            The optional condition to be added to the rule conditions.
	 *            Useful for quickly checking duplicates/negations.
	 * @param testForIllegalRule
	 *            If the conditions need to be tested for illegal combinations
	 *            as well (use background knowledge in conjugated form)
	 * @return A modified version of the input rule conditions, or null if no
	 *         change made.
	 */
	public SortedSet<StringFact> simplifyRule(SortedSet<StringFact> ruleConds,
			StringFact condition, boolean testForIllegalRule) {
		SortedSet<StringFact> simplified = new TreeSet<StringFact>(ruleConds);

		// If we have an optional added condition, check for duplicates/negation
		if (condition != null) {
			StringFact unification = unifyFact(condition, ruleConds,
					new DualHashBidiMap(), new DualHashBidiMap(),
					new String[0], false);
			if (unification != null)
				return null;

			condition.swapNegated();
			StringFact negUnification = unifyFact(condition, ruleConds,
					new DualHashBidiMap(), new DualHashBidiMap(),
					new String[0], false);
			condition.swapNegated();
			if (negUnification != null)
				return null;

			simplified.add(condition);
		}

		for (BackgroundKnowledge bckKnow : ao_.getLearnedBackgroundKnowledge()) {
			bckKnow.simplify(simplified, this, testForIllegalRule);
		}

		if (simplified.equals(ruleConds))
			return null;
		return simplified;
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
		PreGoalInformation preGoal = ao_.getPreGoal(actionPred);

		// If we have a pre goal state
		SortedSet<StringFact> ruleConditions = rule.getConditions(false);
		if (preGoal != null) {
			Collection<StringFact> preGoalState = preGoal.getState();

			// Form a replacement terms map.
			Map<String, String> replacementTerms = new HashMap<String, String>();
			Map<String, String> reverseReplacementTerms = new HashMap<String, String>();
			String[] ruleTerms = rule.getAction().getArguments();
			String[] preGoalTerms = preGoal.getActionTerms();
			for (int i = 0; i < preGoalTerms.length; i++) {
				if (!preGoalTerms[i].equals(ruleTerms[i])) {
					replacementTerms.put(preGoalTerms[i], ruleTerms[i]);
					// Only reverse replace if the term is a variable
					if (ruleTerms[i].charAt(0) == '?')
						reverseReplacementTerms.put(ruleTerms[i],
								preGoalTerms[i]);
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
			for (StringFact preGoalFact : preGoalState) {
				boolean hasPred = false;

				// 2. Run through each cond in the rule conditions
				for (StringFact condFact : ruleConditions) {
					hasPred |= factMutation(rule, mutants, preGoalState,
							replacementTerms, reverseReplacementTerms,
							condFact, preGoalFact, preGoalTerms, rule
									.isMutant());
				}

				// 3. If the fact pred isn't in the cond preds, add it,
				// replacing pre-goal terms with rule terms
				if (!hasPred) {
					// Replace pre-goal action terms with local rule terms
					preGoalFact = new StringFact(preGoalFact, replacementTerms,
							true);

					// If the fact isn't in the rule, we have a mutation
					if (!ruleConditions.contains(preGoalFact)) {
						SortedSet<StringFact> mutatedConditions = simplifyRule(
								ruleConditions, preGoalFact, false);
						if (mutatedConditions != null) {
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
			mutants.addAll(splitRanges(rule, null, 0, rule.isMutant()));
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
		// TODO Look at using goal constants/module constants for rule
		// specialisations.
		Set<GuidedRule> specialisations = new HashSet<GuidedRule>();

		String actionPred = rule.getActionPredicate();
		// If the action has no action conditions, return empty
		Collection<StringFact> actionConditions = ao_
				.getActionConditions(actionPred);
		if (actionConditions == null)
			return specialisations;
		SortedSet<StringFact> conditions = rule.getConditions(false);
		StringFact action = rule.getAction();
		String[] actionTerms = action.getArguments();

		// Add conditions, one-by-one, using both negation and regular
		for (StringFact condition : actionConditions) {
			// Modify the condition arguments to match the action arguments.
			condition = new StringFact(condition);
			condition.replaceArguments(actionTerms);

			// Check for the regular condition
			SortedSet<StringFact> specConditions = simplifyRule(conditions,
					condition, true);
			if (specConditions != null) {
				GuidedRule specialisation = new GuidedRule(specConditions,
						action, true);
				specialisation.setQueryParams(rule.getQueryParameters());
				specialisation.expandConditions();
				if (!specialisations.contains(specialisation))
					specialisations.add(specialisation);
			}

			// Check for the negated condition
			StringFact negCondition = new StringFact(condition);
			negCondition.swapNegated();
			specConditions = simplifyRule(conditions, negCondition, true);
			if (specConditions != null) {
				GuidedRule specialisation = new GuidedRule(specConditions,
						action, true);
				if (!specialisations.contains(specialisation))
					specialisations.add(specialisation);
			}
		}

		return specialisations;
	}

	/**
	 * Splits any ranges in a rule into a number of smaller uniform sub-ranges.
	 * Only mutates rules which are either covered rules, or mutants with ranges
	 * equal to the covered rules.
	 * 
	 * @param baseRule
	 *            The base rule to mutate.
	 * @param preGoalFact
	 *            The split term in the pre-goal matching a ranged condition in
	 *            the rule. Can be null.
	 * @param numericalIndex
	 *            The index in the pre-goal split of the numerical value. Not
	 *            used if preGoalSplit is null.
	 * @param isRuleMutant
	 *            If the rule is a mutant or not.
	 * 
	 * @return A collection of any sub-ranged mutants created from the rule.
	 */
	private Collection<GuidedRule> splitRanges(GuidedRule baseRule,
			StringFact preGoalFact, int numericalIndex, boolean isRuleMutant) {
		Collection<GuidedRule> subranges = new ArrayList<GuidedRule>();

		// Run through each condition
		int c = 0;
		for (StringFact condition : baseRule.getConditions(false)) {
			for (int i = 0; i < condition.getArguments().length; i++) {
				String arg = condition.getArguments()[i];
				int suchThatIndex = arg.indexOf("&:");

				if (suchThatIndex != -1) {
					// We have a range
					String[] betweenRangeSplit = StateSpec.splitFact(arg
							.substring(suchThatIndex + 2));
					// Find the values
					double min = Double.parseDouble(betweenRangeSplit[2]);
					double max = Double.parseDouble(betweenRangeSplit[3]);
					double rangeSplit = (max - min) / NUM_DISCRETE_RANGES;
					RangedCondition rc = new RangedCondition(condition
							.getFactName(), min, max);

					// If dealing with a covered rule, record rule details
					boolean mutate = false;
					if (!isRuleMutant) {
						mutate = true;
						ao_.setActionRange(baseRule.getAction().getFactName(),
								rc);
					} else {
						// Test to see if the range matches the action range.
						List<RangedCondition> existingRange = ao_
								.getActionRange(baseRule.getAction()
										.getFactName());
						if (existingRange != null) {
							int index = existingRange.indexOf(rc);
							if (index != -1) {
								// There is an action range
								RangedCondition coveredRange = existingRange
										.get(index);
								if (rc.equalRange(coveredRange)) {
									mutate = true;
								}
							}
						}
					}

					// Check if the pre-goal matches the value
					if (mutate) {
						determineRanges(baseRule, preGoalFact, numericalIndex,
								subranges, c, condition, i,
								betweenRangeSplit[1], min, max, rangeSplit);
					}
				}
			}
		}

		return subranges;
	}

	/**
	 * Determines the ranges and creates mutants for them.
	 * 
	 * @param baseRule
	 *            The base rule to mutate.
	 * @param preGoalFact
	 *            The possibly null pre-goal split condition.
	 * @param preGoalIndex
	 *            The index of the range in the pre-goal split.
	 * @param subranges
	 *            The collection of rules to add to.
	 * @param c
	 *            The condition index.
	 * @param condition
	 *            The original condition being mutated.
	 * @param i
	 *            The index of the mutation.
	 * @param rangeVariable
	 *            the range variable.
	 * @param min
	 *            The minimum value of the range.
	 * @param max
	 *            The maximum value of the range.
	 * @param rangeSplit
	 *            The size of the subranges.
	 */
	private void determineRanges(GuidedRule baseRule, StringFact preGoalFact,
			int preGoalIndex, Collection<GuidedRule> subranges, int c,
			StringFact condition, int i, String rangeVariable, double min,
			double max, double rangeSplit) {
		// Add the default ranged split
		ArrayList<Double> values = new ArrayList<Double>();
		values.add(min);
		values.add(max);

		// If the ranges go through 0, add that as a stopping point.
		if (min * max < 0)
			values.add(0d);

		Collections.sort(values);
		subranges.addAll(createRangedMutations(baseRule, c, condition, i,
				rangeVariable, values.toArray(new Double[values.size()]),
				rangeSplit, false));

		// If we have a pre-goal and the current split is the same condition.
		if ((preGoalFact != null)
				&& (condition.getFactName().equals(preGoalFact.getFactName()))) {
			values.clear();

			// Split the pre-goal range into chunks of max size rangeSplit
			String preGoalTerm = preGoalFact.getArguments()[preGoalIndex];
			if (preGoalTerm.contains(StateSpec.BETWEEN_RANGE)) {
				// Mutate to a pre-goal range
				int preGoalSuchThatIndex = preGoalTerm.indexOf("&:");
				String[] preGoalRangeSplit = StateSpec.splitFact(preGoalTerm
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
				subranges.addAll(createRangedMutations(baseRule, c, condition,
						i, rangeVariable, values.toArray(new Double[values
								.size()]), rangeSplit, true));
			} else {
				double point = Double.parseDouble(preGoalTerm);
				if (point < min)
					point = min;
				if (point > max)
					point = max;

				// Create the point mutant itself
				GuidedRule pointMutant = replaceConditionTerm(baseRule,
						condition, i, point + "");
				pointMutant.getAction().replaceArguments(rangeVariable,
						point + "");
				subranges.add(pointMutant);

			}
		}
	}

	/**
	 * Create a number of ranged mutations between two points.
	 * 
	 * @param baseRule
	 *            The base rule to mutate.
	 * @param condIndex
	 *            The index of the condition within the rule.
	 * @param condition
	 *            The condition being mutated.
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
	private Collection<GuidedRule> createRangedMutations(GuidedRule baseRule,
			int condIndex, StringFact condition, int condSplitIndex,
			String rangeVariable, Double[] values, double normalRange,
			boolean createMaxRange) {
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
					String range = rangeVariable + "&:("
							+ StateSpec.BETWEEN_RANGE + " " + rangeVariable
							+ " " + (min + rangeSplit * j) + " "
							+ (min + rangeSplit * (j + 1)) + ")";
					subranges.add(replaceConditionTerm(baseRule, condition,
							condSplitIndex, range));
				}
			}
		}

		// If we're creating a range from min to max value and it hasn't already
		// been created (only 1 range)
		if (createMaxRange && (numRanges > 1)) {
			String range = rangeVariable + "&:(" + StateSpec.BETWEEN_RANGE
					+ " " + rangeVariable + " " + values[0] + " "
					+ values[values.length - 1] + ")";
			subranges.add(replaceConditionTerm(baseRule, condition,
					condSplitIndex, range));
		}

		return subranges;
	}

	/**
	 * Simple procedural method that replaces a particular term in a rule
	 * condition with another, creating a new rule.
	 * 
	 * @param baseRule
	 *            The base rule to mutate.
	 * @param condition
	 *            The condition being modified.
	 * @param condSplitIndex
	 *            The argument of the condition being replaced.
	 * @param replacementTerm
	 *            The term replacing the old term.
	 * @return The replaced condition term rule.
	 */
	private GuidedRule replaceConditionTerm(GuidedRule baseRule,
			StringFact condition, int condSplitIndex, String replacementTerm) {
		StringFact mutantFact = new StringFact(condition);
		mutantFact.getArguments()[condSplitIndex] = replacementTerm;
		SortedSet<StringFact> cloneConds = baseRule.getConditions(false);
		cloneConds.remove(condition);
		cloneConds.add(mutantFact);

		GuidedRule mutant = new GuidedRule(cloneConds, baseRule.getAction(),
				true);
		mutant.setQueryParams(baseRule.getQueryParameters());
		return mutant;
	}

	/**
	 * Individual mutation of facts method. This method creates mutations that
	 * operate on the very facts themselves, by swapping fact terms for other
	 * terms. This method was automatically extracted, so it's a bit messy with
	 * its arguments.
	 * 
	 * @param baseRule
	 *            The base rule to mutate.
	 * @param mutants
	 *            The collection of mutants to add to.
	 * @param preGoalState
	 *            The pre-goal state being mutated towards.
	 * @param replacementTerms
	 *            The terms to replace in the pre-goal.
	 * @param reverseReplacementTerms
	 *            The terms to replace in the conditions (modifiable).
	 * @param condFact
	 *            The current rule condition being looked at.
	 * @param preGoalFact
	 *            The current fact's split form.
	 * @param preGoalTerms
	 *            The terms used in the pre-goal's action.
	 * @param isRuleMutant
	 *            If the rule is a mutant or not.
	 * @param ruleTerms
	 *            The terms used in the rule's action.
	 * 
	 * @return True if the cond and pre-goal fact match.
	 */
	private boolean factMutation(GuidedRule baseRule, Set<GuidedRule> mutants,
			Collection<StringFact> preGoalState,
			Map<String, String> replacementTerms,
			Map<String, String> reverseReplacementTerms, StringFact condFact,
			StringFact preGoalFact, String[] preGoalTerms, boolean isRuleMutant) {
		// 4a. If the fact pred matches the rule pred, try replacing all
		// occurrences of each cond term with the fact term in every cond
		boolean hasPred = false;
		if (preGoalFact.getFactName().equals(condFact.getFactName())) {
			hasPred = true;

			// Run through each term
			boolean ceaseMutation = false;
			String[] condArguments = condFact.getArguments();
			for (int i = 0; i < condArguments.length; i++) {
				// Refining ranged numerical variables
				String preGoalArg = preGoalFact.getArguments()[i];
				String condArg = condArguments[i];

				// Finding the replacement term
				String replacedTerm = (replacementTerms.containsKey(preGoalArg)) ? replacementTerms
						.get(preGoalArg)
						: preGoalArg;

				// 5. Replacing variable terms
				if (condArg.charAt(0) == '?') {
					if (!condArg.contains("&:")) {
						// The pregoal term is not anonymous
						if (!ceaseMutation
								&& !preGoalArg.equals(StateSpec.ANONYMOUS)) {
							// 5a. If the cond term is ? and the preds match,
							// use
							// the fact term
							boolean continueCreation = true;
							// If the condition is anonymous, replace it with
							// any
							// term
							if (condArg.equals(StateSpec.ANONYMOUS))
								condArg = replacedTerm;
							// Otherwise, a variable can be replaced with a
							// non-action constant
							else if (!StateSpec.arrayContains(preGoalTerms,
									preGoalArg))
								condArg = preGoalArg;
							// Otherwise, we're looking at two non-mutatable
							// actions.
							else {
								continueCreation = false;
								// If the actions are different, cease mutation
								if (!condArg.equals(replacedTerm))
									ceaseMutation = true;
							}

							if (continueCreation) {
								// Create the mutant
								StringFact newCond = new StringFact(condFact);
								newCond.getArguments()[i] = condArg;
								if (!baseRule.getConditions(false).contains(
										newCond)) {
									SortedSet<StringFact> mutatedConditions = new TreeSet<StringFact>(
											baseRule.getConditions(false));
									mutatedConditions.remove(condFact);
									mutatedConditions = simplifyRule(
											mutatedConditions, newCond, false);
									if (mutatedConditions != null) {
										GuidedRule mutant = new GuidedRule(
												mutatedConditions, baseRule
														.getAction(), true);
										mutant.setQueryParams(baseRule
												.getQueryParameters());
										// Here, needs to add inequals and types
										// that may not be present.
										mutant.expandConditions();
										mutants.add(mutant);
									}
								}
							}
						}
					}
				} else if (!condArg.equals(replacedTerm)) {
					// If the terms don't match up, then the entire fact won't,
					// so skip it.
					break;
				}

				// If we haven't looked at a replacement yet
				if (reverseReplacementTerms.containsKey(condArg)) {
					GuidedRule mutant = replaceActionTerm(baseRule
							.getConditions(false), baseRule.getAction(),
							condArg, reverseReplacementTerms.get(condArg),
							preGoalState, preGoalTerms);
					if (mutant != null) {
						mutant.setQueryParams(baseRule.getQueryParameters());
						mutants.add(mutant);
					}

					// Whatever the outcome, remove the reverse replacement
					// as it has already been tested.
					reverseReplacementTerms.remove(condArg);
				}

				// If the condSplit is a numerical range, create a number of
				// sub-ranged mutations, using the range found in the pre-goal.
				if (condArg.contains(StateSpec.BETWEEN_RANGE)) {
					mutants.addAll(splitRanges(baseRule, preGoalFact, i,
							isRuleMutant));
				}
			}
		}
		return hasPred;
	}

	/**
	 * Replaces an action term with the action term used in the pre-goal.
	 * 
	 * @param ruleConditions
	 *            The rule conditions in the to-be-mutated rule.
	 * @param ruleAction
	 *            The rule action in the to-be-mutated rule.
	 * @param replacedTerm
	 *            The term to be replaced.
	 * @param replacementTerm
	 *            The term that will be replacing the above term.
	 * @param preGoalState
	 *            The current pre-goal state.
	 * @param preGoalTerms
	 *            The pre-goal action terms.
	 * 
	 * @return The mutant rule.
	 */
	private GuidedRule replaceActionTerm(SortedSet<StringFact> ruleConditions,
			StringFact ruleAction, String replacedTerm, String replacementTerm,
			Collection<StringFact> preGoalState, String[] preGoalTerms) {
		// Replace the variables with their replacements.
		SortedSet<StringFact> replacementConds = new TreeSet<StringFact>(
				ConditionComparator.getInstance());
		for (StringFact condition : ruleConditions) {
			// Exclude inequals checks
			if (!condition.getFactName().equals("test")) {
				StringFact repl = new StringFact(condition);
				repl.replaceArguments(replacedTerm, replacementTerm);
				replacementConds.add(repl);
			}
		}
		ruleAction = new StringFact(ruleAction);
		ruleAction.replaceArguments(replacedTerm, replacementTerm);

		// 4b. If the new cond is valid (through unification),
		// keep it and note that the replacement has occurred
		if (unifyStates(replacementConds, preGoalState, ruleAction
				.getArguments(), preGoalTerms) == 0) {
			// Adding the mutated rule
			GuidedRule mutant = new GuidedRule(replacementConds, ruleAction,
					true);
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
		// Scan the state first to create the term-mapped facts
		// Check unifying pre-goal constants (specifically highest)
		ao_.scanState(preGoalState);

		Set<String> formedActions = new HashSet<String>();

		for (RuleAction ruleAction : actionChoice.getActions()) {
			String actionPred = ruleAction.getRule().getActionPredicate();
			// If the state isn't yet settled, try unification
			PreGoalInformation preGoal = ao_.getPreGoal(actionPred);
			if ((preGoal == null) || (!preGoal.isSettled())) {
				List<StringFact> actions = ruleAction.getUtilisedActions();

				// Create a pre-goal from every action in the actions list.
				if (actions != null) {
					for (StringFact action : actions) {
						Collection<StringFact> preGoalStringState = ao_
								.gatherActionFacts(action);

						if (preGoal == null) {
							preGoal = new PreGoalInformation(
									preGoalStringState, action.getArguments());
							ao_.setPreGoal(actionPred, preGoal);
						} else {
							// Unify the two states and check if it has changed
							// at all.
							int result = unifyStates(preGoal.getState(),
									preGoalStringState, preGoal
											.getActionTerms(), action
											.getArguments());

							// If the states unified, reset the counter,
							// otherwise increment.
							if (result == 1) {
								preGoal.resetInactivity();
								ao_.clearHash();
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
		for (String action : StateSpec.getInstance().getActions().keySet()) {
			if (isPreGoalSettled(action))
				settled.add(action);
		}
		return settled;
	}

	/**
	 * Migrates any relevant agent observations from one covering object to
	 * another.
	 * 
	 * @param otherCovering
	 *            The other covering object to migrate the observations to.
	 */
	public void migrateAgentObservations(Covering otherCovering) {
		if (backupPreGoals_ == null)
			backupPreGoals_ = ao_.backupAndClearPreGoals();
		else
			ao_.clearPreGoal();
		otherCovering.ao_ = ao_;
	}

	public void loadBackupPreGoal() {
		if (backupPreGoals_ == null)
			return;
		ao_.loadPreGoals(backupPreGoals_);
		backupPreGoals_ = null;
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
	public void setPreGoal(StringFact action, List<StringFact> preGoal) {
		ao_.setPreGoal(action.getFactName(), new PreGoalInformation(preGoal,
				action.getArguments()));
	}

	/**
	 * Sets the individual conditions that have been observed to be true for
	 * executing an action. Testing method.
	 * 
	 * @param action
	 *            The action to set the conditions for.
	 * @param conditions
	 *            The conditions that have been observed to be true for the
	 *            action.
	 */
	public void setAllowedActionConditions(String action,
			Collection<StringFact> conditions) {
		ao_.setActionConditions(action, conditions);
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
		PreGoalInformation pgi = ao_.getPreGoal(actionPred);
		if (pgi == null)
			return false;
		return pgi.isSettled();
	}

	/**
	 * Checks if the pregoal for the given action is rececntly changed.
	 * 
	 * @param actionPred
	 *            The action predicate.
	 * @return True if the pregoal was recently modified, false otherwise.
	 */
	public boolean isPreGoalRecentlyChanged(String actionPred) {
		PreGoalInformation pgi = ao_.getPreGoal(actionPred);
		if (pgi == null)
			return false;
		return pgi.isRecentlyChanged();
	}

	/**
	 * If the AgentObservations are settled.
	 * 
	 * @return True if the observations are settled.
	 */
	public boolean isSettled() {
		return ao_.isConditionBeliefsSettled()
				&& ao_.isActionConditionSettled()
				&& ao_.isRangedConditionSettled();
	}

	/**
	 * Resets the inactivity counters of the AgentObservations object.
	 */
	public void resetInactivity() {
		ao_.resetInactivity();
	}

	/**
	 * Gets the pre-goal general state, seen by the agent.
	 * 
	 * @param action
	 *            The pre-goal action.
	 * @return The pre-goal state, in the form of a list of facts.
	 */
	public Collection<StringFact> getPreGoalState(String action) {
		PreGoalInformation pgi = ao_.getPreGoal(action);
		if (pgi == null)
			return null;
		return pgi.getState();
	}

	/**
	 * Gets the hash code for the current pre-goal.
	 * 
	 * @param actionPredicate
	 *            The action predicate for the pre-goal.
	 * @return An integer hash code corresponding to the pre-goal state.
	 */
	public int getMutationHash(String actionPredicate) {
		Integer hash = ao_.getObservationHash();
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
	public static int getVariableTermIndex(String variable) {
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
		return ao_.hasPreGoal();
	}

	/**
	 * Clears the pre-goal from the agent observations. Generally a testing
	 * method.
	 */
	public void clearPreGoalState() {
		ao_.clearPreGoal();
	}

	/**
	 * Saves the agent observations to file, in the agent obs directory.
	 */
	public void saveAgentObservations(int run) {
		ao_.saveAgentObservations(run);
	}
}
