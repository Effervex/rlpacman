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

import org.mandarax.kernel.ClauseSet;
import org.mandarax.kernel.ConstantTerm;
import org.mandarax.kernel.Fact;
import org.mandarax.kernel.KnowledgeBase;
import org.mandarax.kernel.LogicFactory;
import org.mandarax.kernel.Predicate;
import org.mandarax.kernel.Rule;
import org.mandarax.kernel.Term;

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

	/** The logic factory for the experiment. */
	private LogicFactory factory_;

	/**
	 * Covering constructor.
	 * 
	 * @param factory
	 *            The logic factory for the experiment.
	 */
	public Covering(LogicFactory factory) {
		factory_ = factory;
	}

	/**
	 * Covers a state by creating a rule for every action type present in the
	 * valid actions for the state.
	 * 
	 * @param state
	 *            The state of the environment, containing the valid actions.
	 * @return A list of guided rules, one for each action type.
	 */
	public List<Rule> coverState(KnowledgeBase state) {
		// Find the relevant conditions for each term
		MultiMap<Term, Fact> relevantConditions = new MultiMap<Term, Fact>();
		Fact actionFact = compileRelevantConditionMap(state, relevantConditions);

		// Maintain a mapping for each action, to be used in unification between
		// actions
		List<Rule> generalActions = new ArrayList<Rule>();

		// Arrange the actions in a heuristical order such that unification
		// should be most effective.
		@SuppressWarnings("unchecked")
		MultiMap<Predicate, Fact> validActions = arrangeActions((Set<Fact>) ((ConstantTerm) actionFact
				.getTerms()[0]).getObject());
		for (Predicate action : validActions.keySet()) {
			Rule actionRule = unifyActionRules(validActions.get(action),
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
	 * @param state
	 * @return
	 */
	public List<GuidedRule> specialiseRule(GuidedRule rule, KnowledgeBase state) {
		return null;
	}

	/**
	 * Unifies action rules together into one general all-covering rule.
	 * 
	 * @param actionsList
	 *            A heuristically sorted list of actions for a single predicate.
	 * @param relevantConditions
	 *            The relevant conditions for each term in the state.
	 * @param actionPred
	 *            The action predicate spawning this rule.
	 * @return A Rule representing a general action.
	 */
	private Rule unifyActionRules(List<Fact> actionsList,
			MultiMap<Term, Fact> relevantConditions, Predicate actionPred) {
		// The general rule for the action
		Collection<String> generalRule = null;
		Collection<String> stringTerms = new HashSet<String>();
		String actionString = formatAction(actionPred, stringTerms);

		int lastChanged = 0;
		Iterator<Fact> actionIter = actionsList.iterator();
		// Do until:
		// 1) We have no actions left to look at
		// 2) Or the general rule isn't minimal
		// 3) Or the general rule hasn't changed for X turns
		while ((actionIter.hasNext())
				&& (!isMinimal(generalRule, stringTerms))
				&& (lastChanged < MAX_UNIFICATION_INACTIVITY)) {
			Fact action = actionIter.next();
			List<Fact> actionFacts = new ArrayList<Fact>();

			Term[] actionTerms = action.getTerms();
			// Find the facts containing the same (useful) terms in the action
			for (int i = 0; i < actionTerms.length; i++) {
				Term term = actionTerms[i];
				if (StateSpec.isUsefulTerm((ConstantTerm) term, factory_)) {
					List<Fact> termFacts = relevantConditions.get(term);
					for (Fact termFact : termFacts) {
						if (!actionFacts.contains(termFact))
							actionFacts.add(termFact);
					}
				}
			}

			// Inversely substitute the terms for variables (in string form)
			Collection<String> inverseSubbed = inverselySubstitute(actionFacts,
					actionTerms);

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
		}

		// Use the unified rules to create new rules
		String joinedRule = joinRule(generalRule, actionString);
		return StateSpec.getInstance().parseRule(joinedRule, null);
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
		boolean first = true;
		for (String condition : conditions) {
			if (!first)
				buffer.append(StateSpec.AND + " ");
			buffer.append(condition + " ");
			first = false;
		}

		buffer.append(StateSpec.INFERS_ACTION + " " + action);
		return buffer.toString();
	}

	/**
	 * Formats the action into a variable action string.
	 * 
	 * @param actionPred
	 *            The action predicate being formatted.
	 * @param actionTerms
	 *            The terms present in the action to be filled.
	 * @return A string version of the predicate, with variable terms.
	 */
	public String formatAction(Predicate actionPred,
			Collection<String> actionTerms) {
		StringBuffer buffer = new StringBuffer();

		// Formatting the action
		buffer.append(actionPred.getName() + "(");
		int i = 0;
		boolean first = true;
		for (Class structure : actionPred.getStructure()) {
			if (StateSpec.isUsefulClass(structure, factory_)) {
				if (!first)
					buffer.append(",");
				String term = getVariableTermString(i);
				actionTerms.add(term);
				buffer.append(term);
				i++;
				first = false;
			}
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
			Term[] actionTerms) {
		// Building the mapping from necessary constants to variables
		Map<String, String> termMapping = new HashMap<String, String>();
		int i = 0;
		for (Term term : actionTerms) {
			ConstantTerm constantTerm = (ConstantTerm) term;
			if (StateSpec.isUsefulTerm(constantTerm, factory_)) {
				termMapping.put(
						getConstantTermString(constantTerm.getObject()),
						getVariableTermString(i));
				i++;
			}
		}

		Collection<String> substitution = new ArrayList<String>();
		// Applying the mapping to each condition, making unnecessary terms into
		// anonymous terms.
		for (Fact fact : actionFacts) {
			String factString = StateSpec.lightenFact(fact);
			// Replace all constant terms in the action with matching variables
			for (String actionTerm : termMapping.keySet()) {
				factString = factString.replaceAll(Pattern.quote(actionTerm),
						termMapping.get(actionTerm));
			}

			// Replace all other terms with anonymous string.
			factString = factString.replaceAll("\\[.+?\\]", StateSpec.ANONYMOUS
					+ "");
			substitution.add(factString);
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
	 * @return A multimap of each action predicate, containing the heuristically
	 *         ordered actions.
	 */
	private MultiMap<Predicate, Fact> arrangeActions(Set<Fact> validActions) {
		// Initialise a map for each action predicate
		MultiMap<Predicate, Fact> actionsMap = new MultiMap<Predicate, Fact>();
		MultiMap<Predicate, Set<Term>> usedTermsMap = new MultiMap<Predicate, Set<Term>>();

		MultiMap<Predicate, Fact> notUsedMap = new MultiMap<Predicate, Fact>();
		// For each action
		for (Fact action : validActions) {
			Predicate actionPred = action.getPredicate();
			if (isDissimilarAction(action, usedTermsMap)) {
				actionsMap.put(actionPred, action);
			} else {
				notUsedMap.put(actionPred, action);
			}
		}

		// If we have some actions not used (likely, then shuffle the ordering
		// and add them to the end of the actions map.
		if (!notUsedMap.isEmpty()) {
			for (List<Fact> notUsed : notUsedMap.valuesLists()) {
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
	 * @param action
	 *            The action being checked.
	 * @param usedTermsMap
	 *            The already used terms mapping.
	 * @return True if the action is dissimilar, false otherwise.
	 */
	private boolean isDissimilarAction(Fact action,
			MultiMap<Predicate, Set<Term>> usedTermsMap) {
		Term[] terms = action.getTerms();
		Predicate actionPred = action.getPredicate();
		// Run through each term
		for (int i = 0; i < terms.length; i++) {
			// Checking if the term set already exists
			Set<Term> usedTerms = usedTermsMap.getIndex(actionPred, i);
			if (usedTerms == null) {
				usedTerms = new HashSet<Term>();
				usedTermsMap.put(actionPred, usedTerms);
			}

			ConstantTerm term = (ConstantTerm) terms[i];
			// If the term has already been used (but isn't a state or state
			// spec term), return false
			if ((usedTerms.contains(term))
					&& (StateSpec.getInstance().isUsefulTerm(term, factory_)))
				return false;
		}

		// If the terms have not been used before, add them all to their
		// appropriate sets.
		for (int i = 0; i < terms.length; i++) {
			usedTermsMap.getIndex(actionPred, i).add(terms[i]);
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
	 * @return A mapping of conditions with terms as the key. Ignores type,
	 *         inequal, action facts and state and state spec terms.
	 */
	private Fact compileRelevantConditionMap(KnowledgeBase state,
			MultiMap<Term, Fact> relevantConditions) {
		Fact actionFact = null;

		List<ClauseSet> clauseSets = state.getClauseSets();
		for (ClauseSet cs : clauseSets) {
			// If the clause is a fact, use it
			if (cs instanceof Fact) {
				Fact stateFact = (Fact) cs;
				// Ignore the type, inequal and actions pred
				if (StateSpec.getInstance().isUsefulPredicate(
						stateFact.getPredicate()))
					extractTerms(stateFact, relevantConditions);
				// Find the action fact
				else if (stateFact.getPredicate().equals(
						StateSpec.getValidActionsPredicate()))
					actionFact = stateFact;
			}

			// TODO If the clause is a rule, use the rule to find background
			// clause facts.
		}

		return actionFact;
	}

	/**
	 * Extracts the terms from a fact and adds them to an appropriate position
	 * within the conditionMap.
	 * 
	 * @param stateFact
	 *            The fact being examined.
	 * @param conditionMap
	 *            The condition map to add to.
	 */
	private void extractTerms(Fact stateFact, MultiMap<Term, Fact> conditionMap) {
		Term[] terms = stateFact.getTerms();
		for (Term term : terms) {
			// Ignore the state and state spec terms
			if (StateSpec.getInstance().isUsefulTerm((ConstantTerm) term,
					factory_)) {
				// Add to map, if not already there
				conditionMap.putContains(term, stateFact);
			}
		}
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
		return "<" + variable + ">";
	}

	public static String getConstantTermString(Object obj) {
		return "[" + obj + "]";
	}
}
