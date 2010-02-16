package relationalFramework;

import java.util.ArrayList;
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
import jess.Value;
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
	private static final int MAX_UNIFICATION_INACTIVITY = 3;
	private static final int MAX_STATE_UNIFICATION_INACTIVITY = 10;

	/** The pre-goal state, for use in covering specialisations. */
	private List<String> preGoalState_;

	/** The last time since the pre-goal state changed. */
	private int preGoalUnificationInactivity_ = 0;

	/**
	 * Covers a state by creating a rule for every action type present in the
	 * valid actions for the state.
	 * 
	 * @param state
	 *            The state of the environment, containing the valid actions.
	 * @return A list of guided rules, one for each action type.
	 */
	public List<GuidedRule> coverState(Rete state) throws Exception {
		// The relevant facts which contain the key term
		MultiMap<String, Fact> relevantConditions = new MultiMap<String, Fact>();
		Fact actionFact = compileRelevantConditionMap(state, relevantConditions);

		// Maintain a mapping for each action, to be used in unification between
		// actions
		List<GuidedRule> generalActions = new ArrayList<GuidedRule>();

		// A multimap with the action predicate as key and args as values.
		MultiMap<String, String> validActions = arrangeActions(actionFact);
		for (String action : validActions.keySet()) {
			GuidedRule actionRule = unifyActionRules(validActions.get(action),
					relevantConditions, action);
			generalActions.add(actionRule);
		}

		return generalActions;
	}

	/**
	 * Specialises a rule to match the state (ideally in a minimal way). There
	 * can be multiple specialisations.
	 * 
	 * @param rule
	 *            The general rule to be specialised.
	 * @param state
	 *            The state used as a specialisation.
	 * @return A list of newly specialised rules, where each is more specialised
	 *         than the general rule but still match the state.
	 */
	public List<GuidedRule> specialiseRule(GuidedRule rule, Rete state) {
		// TODO Specialise rule to a state
		return null;
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
		// TODO Specialise rule to pre-goal state
		return null;
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
		// If the preGoal state hasn't changed for
		// MAX_STATE_UNIFICATION_INACTIVITY steps, don't bother unifying it
		// again, it's probably already at minimum.
		if (preGoalUnificationInactivity_ < MAX_STATE_UNIFICATION_INACTIVITY) {

			// TODO Form the pre goal state in a general form.
			return true;
		}
		return false;
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
	 * @return A Rule representing a general action.
	 */
	private GuidedRule unifyActionRules(List<String> argsList,
			MultiMap<String, Fact> relevantConditions, String actionPred) {
		// The general rule for the action
		Collection<String> generalRule = null;
		Collection<String> stringTerms = new HashSet<String>();
		String actionString = formatAction(actionPred, stringTerms);

		int lastChanged = 0;
		Iterator<String> argIter = argsList.iterator();
		// Do until:
		// 1) We have no actions left to look at
		// 2) Or the general rule isn't minimal
		boolean isMinimal = false;
		while ((argIter.hasNext()) && (!isMinimal)) {
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
			}

			// Inversely substitute the terms for variables (in string form)
			Collection<String> inverseSubbed = inverselySubstitute(actionFacts,
					terms);

			// Unify with other action rules of the same action
			if (generalRule == null) {
				generalRule = inverseSubbed;
			} else {
				// Unify the rules through a simply retainment operation.
				boolean changed = generalRule.retainAll(inverseSubbed);
				if (changed)
					lastChanged = 0;
				else
					lastChanged++;
			}

			isMinimal = isMinimal(generalRule, stringTerms);
		}

		// Use the unified rules to create new rules
		String joinedRule = joinRule(generalRule, actionString);
		GuidedRule rule = new GuidedRule(joinedRule, isMinimal, false, null);
		return rule;
	}

	/**
	 * Checks if a rule is minimal (only enough conditions to satisfy the
	 * action).
	 * 
	 * @param conditions
	 *            The conditions of the rule being checked.
	 * @param terms
	 *            The terms in the action.
	 * @return True if the rule is minimal, false otherwise.
	 */
	public boolean isMinimal(Collection<String> conditions,
			Collection<String> terms) {
		if (conditions == null)
			return false;
		if (conditions.isEmpty()) {
			System.err.println("Conditions have been over-shrunk: "
					+ conditions + ", " + terms);
			return false;
		}

		terms = new HashSet<String>(terms);

		// Run through the conditions, ensuring each one has at least one unique
		// term seen in the action.
		for (String condition : conditions) {
			boolean contains = false;

			// Check if any of the terms are in the condition
			for (Iterator<String> i = terms.iterator(); i.hasNext();) {
				String term = i.next();
				if (condition.contains(term)) {
					i.remove();
					contains = true;
				}
			}
			// If no term is in the condition, return false
			if (!contains)
				return false;
		}

		return true;
	}

	/**
	 * A simple method for joining a collection of condition fact strings and an
	 * action together into a rule.
	 * 
	 * @param conditions
	 *            The condition strings of the rule.
	 * @param actionPred
	 *            The action the conditions lead to.
	 * @return A rule string made by joining the conditions to the action.
	 */
	public String joinRule(Collection<String> conditions, String action) {
		StringBuffer buffer = new StringBuffer();
		for (String condition : conditions) {
			buffer.append(condition + " ");
		}

		buffer.append(StateSpec.INFERS_ACTION + " " + action);
		return buffer.toString();
	}

	/**
	 * Formats the action into a variable action string. So (move a b) becomes
	 * (move ?X ?Y), and the actionTerms simply note which terms are in the
	 * action.
	 * 
	 * @param actionPred
	 *            The action predicate being formatted.
	 * @param actionTerms
	 *            The terms present in the action to be filled.
	 * @return A string version of the predicate, with variable terms.
	 */
	public String formatAction(String actionPred, Collection<String> actionTerms) {
		StringBuffer buffer = new StringBuffer();

		// Formatting the action
		buffer.append("(" + actionPred);
		for (int i = 0; i < StateSpec.getInstance().getActions()
				.get(actionPred).size(); i++) {
			String term = getVariableTermString(i);
			actionTerms.add(term);
			buffer.append(" " + term);
		}
		buffer.append(")");
		return buffer.toString();
	}

	/**
	 * Inversely substitutes a rule for a general form containing only variable
	 * terms. The returned value is in string format for later parsing.
	 * 
	 * @param actionFacts
	 *            The facts relating to this action.
	 * @param actionTerms
	 *            The terms of the action.
	 * @return A collection of the facts in inversely substituted string format.
	 */
	private Collection<String> inverselySubstitute(List<Fact> actionFacts,
			String[] actionTerms) {
		// Building the mapping from necessary constants to variables
		Map<String, String> termMapping = new HashMap<String, String>();
		int i = 0;
		for (String term : actionTerms) {
			termMapping.put(term,
					getVariableTermString(i));
			i++;
		}

		Collection<String> substitution = new ArrayList<String>();
		// Applying the mapping to each condition, making unnecessary terms into
		// anonymous terms.
		for (Fact fact : actionFacts) {
			String[] factSplit = StateSpec.splitFact(fact.toString());
			// Replace all constant terms in the action with matching variables or anonymous variables
			for (int j = 1; j < factSplit.length; j++) {
				String replacementTerm = termMapping.get(factSplit[j]);
				if (replacementTerm != null)
					factSplit[j] = replacementTerm;
				else
					factSplit[j] = StateSpec.ANONYMOUS + "";
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
			// TODO May have to provide context
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
		if (!notUsedMap.isEmpty()) {
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
				// Find the action fact
			} else if (split[0].equals(StateSpec.VALID_ACTIONS))
				actionFact = stateFact;
		}

		return actionFact;
	}

	/**
	 * Gets the pre-goal general state, seen by the agent.
	 * 
	 * @return The pre-goal state, in the form of a list of facts.
	 */
	public List<String> getPreGoalState() {
		return preGoalState_;
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
}
