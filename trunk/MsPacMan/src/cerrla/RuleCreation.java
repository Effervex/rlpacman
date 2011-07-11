package cerrla;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.bidimap.DualHashBidiMap;

import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.AgentObservations;
import relationalFramework.util.MultiMap;

import jess.Rete;

/**
 * A class which deals specifically with covering rules from a state.
 * 
 * @author Samuel J. Sarjant
 */
public class RuleCreation implements Serializable {
	private static final long serialVersionUID = 4327985487131940550L;
	/** The first character for variables. */
	private static final char FIRST_CHAR = 'A';
	/** The final character for variables. */
	private static final char MODULO_LETTERS = 26;
	/** The starting character for variables. */
	private static final char STARTING_CHAR = 'X';

	/**
	 * Checks if the type conditions of the added condition conflict with the
	 * existing types.
	 * 
	 * @param ruleConds
	 *            The existing rule conditions.
	 * @param condition
	 *            The condition to be added.
	 * @return False if the condition conflicts, true otherwise.
	 */
	private boolean checkConditionTypes(SortedSet<RelationalPredicate> ruleConds,
			RelationalPredicate condition) {
		// Run through the rule conditions.
		for (RelationalPredicate ruleCond : ruleConds) {
			// Only check type preds
			if (StateSpec.getInstance().isTypePredicate(ruleCond.getFactName())) {
				// Check if the type argument conflicts with the conditions
				// arguments
				String[] condArgs = condition.getArguments();
				for (int i = 0; i < condArgs.length; i++) {
					// Matching argument. Check if types conflict
					if (condArgs[i].equals(ruleCond.getArguments()[0])) {
						Collection<String> lineage = new HashSet<String>();
						StateSpec.getInstance().getTypeLineage(
								condition.getArgTypes()[i], lineage);
						if (!lineage.contains(ruleCond.getFactName()))
							return false;
					}
				}
			}
		}

		return true;
	}

	/**
	 * Creates a ranged specialisation of an existing range within a rule.
	 * 
	 * @param baseRule
	 *            The base rule to specialise.
	 * @param condition
	 *            The condition within the rule to specialise.
	 * @param condArgIndex
	 *            The argument index in the condition to specialise.
	 * @param rangeVariable
	 *            The range variable at that index.
	 * @param lowerBound
	 *            The lower bound of the range.
	 * @param upperBound
	 *            The upper bound of the range.
	 * @return A specialised GuidedRule in the same form as baseRule, but with a
	 *         smaller range.
	 */
	private RelationalRule createRangedSpecialisation(RelationalRule baseRule,
			RelationalPredicate condition, int condArgIndex, String rangeVariable,
			double lowerBound, double upperBound) {
		// Create the replacement term.
		String replacementTerm;
		// If a range, make a range, otherwise make a point.
		if (lowerBound != upperBound)
			replacementTerm = rangeVariable + "&:(" + StateSpec.BETWEEN_RANGE
					+ " " + rangeVariable + " " + lowerBound + " " + upperBound
					+ ")";
		else
			replacementTerm = lowerBound + "";

		RelationalPredicate mutantFact = new RelationalPredicate(condition);
		mutantFact.getArguments()[condArgIndex] = replacementTerm;
		SortedSet<RelationalPredicate> cloneConds = baseRule.getConditions(false);
		cloneConds.remove(condition);
		cloneConds.add(mutantFact);

		RelationalRule mutant = new RelationalRule(cloneConds, baseRule.getAction(),
				baseRule);
		// If just a point, change the action.
		if (lowerBound == upperBound)
			mutant.getAction().replaceArguments(rangeVariable, lowerBound + "",
					true);
		mutant.setQueryParams(baseRule.getQueryParameters());
		return mutant;
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
	 * @param condArgIndex
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
	private void createRangeSpecialisations(RelationalRule baseRule,
			RelationalPredicate preGoalFact, int preGoalIndex,
			Set<RelationalRule> subranges, RelationalPredicate condition, int condArgIndex,
			String rangeVariable, double min, double max) {
		if (min != max) {
			// Create 3 ranges, the min and max split in two and a range
			// overlapping each centred about the middle.
			double halfVal = min + (max - min) / 2;
			double quarterAmount = (max - min) / 4;
			subranges.add(createRangedSpecialisation(baseRule, condition,
					condArgIndex, rangeVariable, min, halfVal));
			subranges.add(createRangedSpecialisation(baseRule, condition,
					condArgIndex, rangeVariable, halfVal, max));
			subranges.add(createRangedSpecialisation(baseRule, condition,
					condArgIndex, rangeVariable, halfVal - quarterAmount,
					halfVal + quarterAmount));
		}

		if (!baseRule.isMutant()) {
			// If the ranges go through 0 and this rule isn't a
			// mutant, split at 0
			if (min * max < 0) {
				subranges.add(createRangedSpecialisation(baseRule, condition,
						condArgIndex, rangeVariable, min, 0));
				subranges.add(createRangedSpecialisation(baseRule, condition,
						condArgIndex, rangeVariable, 0, max));
			}

			// If the pre-goal has a range of different size, create
			// that too.
			if (preGoalFact != null
					&& condition.getFactName()
							.equals(preGoalFact.getFactName())) {
				// Check that the size differs
				String preGoalTerm = preGoalFact.getArguments()[preGoalIndex];
				if (preGoalTerm.contains(StateSpec.BETWEEN_RANGE)) {
					// Mutate to a pre-goal range
					int preGoalSuchThatIndex = preGoalTerm.indexOf("&:");
					String[] preGoalRangeSplit = StateSpec
							.splitFact(preGoalTerm
									.substring(preGoalSuchThatIndex + 2));
					double startPoint = Double
							.parseDouble(preGoalRangeSplit[2]);
					double endPoint = Double.parseDouble(preGoalRangeSplit[3]);

					// Make sure the sizes differ otherwise it's just creating
					// the same range
					if (startPoint != min || endPoint != max)
						subranges.add(createRangedSpecialisation(baseRule,
								condition, condArgIndex, rangeVariable,
								startPoint, endPoint));
				} else {
					double point = Double.parseDouble(preGoalTerm);
					subranges.add(createRangedSpecialisation(baseRule,
							condition, condArgIndex, rangeVariable, point,
							point));
				}
			}
		}
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
	 * @param preGoalIndex
	 *            The index in the pre-goal split of the numerical value. Not
	 *            used if preGoalSplit is null.
	 * @param isRuleMutant
	 *            If the rule is a mutant or not.
	 * 
	 * @return A collection of any sub-ranged mutants created from the rule.
	 */
	private Set<RelationalRule> splitRanges(RelationalRule baseRule,
			RelationalPredicate preGoalFact, int preGoalIndex, boolean isRuleMutant) {
		Set<RelationalRule> subranges = new HashSet<RelationalRule>();

		// Run through each condition
		for (RelationalPredicate condition : baseRule.getConditions(false)) {
			for (int i = 0; i < condition.getArguments().length; i++) {
				String arg = condition.getArguments()[i];
				int suchThatIndex = arg.indexOf("&:");

				// We have a range
				if (suchThatIndex != -1) {
					String[] betweenRangeSplit = StateSpec.splitFact(arg
							.substring(suchThatIndex + 2));
					// Find the values
					double min = Double.parseDouble(betweenRangeSplit[2]);
					double max = Double.parseDouble(betweenRangeSplit[3]);

					createRangeSpecialisations(baseRule, preGoalFact,
							preGoalIndex, subranges, condition, i,
							betweenRangeSplit[1], min, max);
				}
			}
		}

		return subranges;
	}

	/**
	 * Covers a state using RLGG by creating a rule for every action type
	 * present in the valid actions for the state.
	 * 
	 * @param state
	 *            The state of the environment, containing the valid actions.
	 * @param validActions
	 *            The set of valid actions to choose from.
	 * @param goalReplacements
	 *            The goal replacements.
	 * @param coveredRules
	 *            A starting point for the rules, if any exist.
	 * @return A list of guided rules, one for each action type.
	 */
	public List<RelationalRule> rlggState(Rete state,
			MultiMap<String, String[]> validActions,
			Map<String, String> goalReplacements,
			MultiMap<String, RelationalRule> coveredRules) throws Exception {
		// The relevant facts which contain the key term
		AgentObservations.getInstance().scanState(
				StateSpec.extractFacts(state), goalReplacements);

		// Run through each valid action.
		for (String action : validActions.keySet()) {
			// Get the list of previous rules, both LGG and non-LGG.
			List<RelationalRule> previousRules = coveredRules.getList(action);
			if (previousRules == null)
				previousRules = new ArrayList<RelationalRule>();

			// Gather the action facts for each valid action
			RelationalPredicate baseAction = StateSpec.getInstance().getStringFact(
					action);
			for (String[] actionArgs : validActions.get(action)) {
				RelationalPredicate actionFact = new RelationalPredicate(baseAction, actionArgs);
				AgentObservations.getInstance().gatherActionFacts(actionFact,
						goalReplacements);
			}
		}

		// Get the RLGG from the invariant conditions from the action
		// conditions.
		int numGoalArgs = 0;
		if (goalReplacements == null)
			numGoalArgs = PolicyGenerator.getInstance().getModuleGoal()
					.getNumArgs();
		else
			numGoalArgs = goalReplacements.size();
		List<String> queryTerms = new ArrayList<String>(numGoalArgs);
		for (int i = 0; i < numGoalArgs; i++)
			queryTerms.add(StateSpec.createGoalTerm(i));
		return AgentObservations.getInstance().getRLGGActionRules(queryTerms);
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
	 * @param checkConditionUnification
	 *            If the added condition is checked if it already unifies with
	 *            existing conditions.
	 * @return A modified version of the input rule conditions, or null if no
	 *         change made.
	 */
	public SortedSet<RelationalPredicate> simplifyRule(SortedSet<RelationalPredicate> ruleConds,
			RelationalPredicate condition, boolean testForIllegalRule,
			boolean checkConditionUnification) {
		SortedSet<RelationalPredicate> simplified = new TreeSet<RelationalPredicate>(ruleConds);

		// If we have an optional added condition, check for duplicates/negation
		if (condition != null) {
			if (checkConditionUnification) {
				RelationalPredicate unification = Unification.getInstance().unifyFact(
						condition, ruleConds, new DualHashBidiMap(),
						new DualHashBidiMap(), new String[0], false, false,
						false);
				if (unification != null)
					return null;
			}

			condition.swapNegated();
			RelationalPredicate negUnification = Unification.getInstance().unifyFact(
					condition, ruleConds, new DualHashBidiMap(),
					new DualHashBidiMap(), new String[0], false, false, false);
			condition.swapNegated();
			if (negUnification != null)
				return null;

			// Need to check type conditions
			if (!checkConditionTypes(ruleConds, condition))
				return null;

			simplified.add(condition);
		}

		// Simplify using the learned background knowledge
		AgentObservations.getInstance().simplifyRule(simplified,
				testForIllegalRule, false);

		if (simplified.equals(ruleConds))
			return null;

		// Check the rule contains only valid goal facts
		MultiMap<String, RelationalPredicate> goalPreds = AgentObservations
				.getInstance().getGoalPredicateMap();
		Map<String, String> replacementMap = new HashMap<String, String>();
		for (String goalTerm : goalPreds.keySet())
			replacementMap.put(goalTerm, goalTerm);

		for (RelationalPredicate sf : simplified)
			if (!isValidGoalCondition(sf, goalPreds, replacementMap))
				return null;

		return simplified;
	}

	/**
	 * Specialise a rule by adding a condition to it which includes a term used
	 * in the action.
	 * 
	 * @param rule
	 *            The rule to specialise.
	 * @return A collection of possible specialisations of the rule.
	 */
	public Set<RelationalRule> specialiseRule(RelationalRule rule) {
		Set<RelationalRule> specialisations = new HashSet<RelationalRule>();

		String actionPred = rule.getActionPredicate();
		// If the action has no action conditions, return empty
		Collection<RelationalPredicate> actionConditions = AgentObservations
				.getInstance().getSpecialisationConditions(actionPred);
		if (actionConditions == null)
			return specialisations;
		SortedSet<RelationalPredicate> conditions = rule.getConditions(false);
		RelationalPredicate action = rule.getAction();
		String[] actionTerms = action.getArguments();

		// Add conditions, one-by-one, using both negation and regular
		for (RelationalPredicate condition : actionConditions) {
			// Modify the condition arguments to match the action arguments.
			condition = new RelationalPredicate(condition);
			condition.replaceArguments(actionTerms);

			// Check for the regular condition
			SortedSet<RelationalPredicate> specConditions = simplifyRule(conditions,
					condition, true, false);
			if (specConditions != null) {
				RelationalRule specialisation = new RelationalRule(specConditions,
						action, rule);
				specialisation.setQueryParams(rule.getQueryParameters());
				specialisation.expandConditions();
				if (!specialisations.contains(specialisation))
					specialisations.add(specialisation);
			}
		}

		return specialisations;
	}

	/**
	 * Specialises a rule by making minor (non slot-splitting) changes to the
	 * rule's conditions. These include range splitting and specialising
	 * variables to constants.
	 * 
	 * @param rule
	 *            The rule to specialise.
	 * @return All single-step mutations the rule can take towards matching the
	 *         pre-goal state.
	 */
	public Set<RelationalRule> specialiseRuleMinor(RelationalRule rule) {
		Set<RelationalRule> mutants = new HashSet<RelationalRule>();

		// Replace the variables with constants
		MultiMap<String, RelationalPredicate> goalPredicates = AgentObservations
				.getInstance().getGoalPredicateMap();
		String[] oldTerms = rule.getActionTerms();
		// For every goal term
		for (String goalTerm : goalPredicates.keySet()) {
			boolean termPresent = false;
			// If the old terms already contain the goal term, can't swap
			for (String oldTerm : oldTerms) {
				if (goalTerm.equals(oldTerm)) {
					termPresent = true;
					break;
				}
			}

			if (!termPresent)
				// For every action term
				for (int i = 0; i < oldTerms.length; i++) {
					// If the term is already a goal term, can't swap
					if (!oldTerms[i].startsWith(StateSpec.GOAL_VARIABLE_PREFIX))
						swapRuleTerm(rule, mutants, goalPredicates, oldTerms,
								i, goalTerm);
				}
		}

		// Split any ranges up
		mutants.addAll(splitRanges(rule, null, 0, rule.isMutant()));

		return mutants;
	}

	private void swapRuleTerm(RelationalRule rule, Set<RelationalRule> mutants,
			MultiMap<String, RelationalPredicate> goalPredicates, String[] oldTerms,
			int i, String goalTerm) {
		String[] newTerms = Arrays.copyOf(oldTerms, oldTerms.length);
		newTerms[i] = goalTerm;
		SortedSet<RelationalPredicate> ruleConditions = rule.getConditions(true);
		Collection<RelationalPredicate> specConditions = new TreeSet<RelationalPredicate>(
				ruleConditions.comparator());
		// Form the replacement map
		Map<String, String> replacementMap = new HashMap<String, String>();
		replacementMap.put(oldTerms[i], goalTerm);
		for (String gTerm : goalPredicates.keySet()) {
			replacementMap.put(gTerm, gTerm);
		}

		// Run through the rule conditions, checking each replaced
		// term is valid in regards to goal options.
		boolean validRule = true;
		for (RelationalPredicate cond : ruleConditions) {
			// Check if the condition is a valid goal condition.
			if (isValidGoalCondition(cond, goalPredicates, replacementMap)) {
				// If the replacement is valid, then add a replaced
				// fact (retaining other args) to the specialised
				// conditions.
				RelationalPredicate specCond = new RelationalPredicate(cond);
				specCond.replaceArguments(replacementMap, true, false);
				specConditions.add(specCond);
			} else {
				validRule = false;
				break;
			}
		}

		if (validRule) {
			// Create the mutant
			RelationalRule mutant = new RelationalRule(specConditions, new RelationalPredicate(
					rule.getAction(), newTerms), rule);
			mutant.setQueryParams(rule.getQueryParameters());
			mutant.expandConditions();
			mutants.add(mutant);
		}
	}

	/**
	 * Checks if a condition is a valid condition based on the known goal
	 * conditions. The condition will either be fully anonymous, or contain goal
	 * conditions in such a way as to match the goal conditions.
	 * 
	 * @param cond
	 *            The condition to check.
	 * @param goalPredicates
	 *            The goal predicate map.
	 * @param replacementMap
	 *            The replacement map to use. Must not have variables as values
	 *            (only as keys).
	 * 
	 * @return True if the condition is valid, false otherwise.
	 */
	private boolean isValidGoalCondition(RelationalPredicate cond,
			MultiMap<String, RelationalPredicate> goalPredicates,
			Map<String, String> replacementMap) {
		RelationalPredicate checkedCond = new RelationalPredicate(cond);
		boolean notAnonymous = checkedCond.replaceArguments(replacementMap,
				false, false);
		return goalPredicates.values().contains(checkedCond) || !notAnonymous
				|| cond.getFactName().equals(StateSpec.GOALARGS_PRED);
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
		// Don't swap constants or anonymous
		if (variable.charAt(0) != '?' || variable.length() < 2)
			return -1;

		// Don't swap number variables
		if (variable.contains(Unification.RANGE_VARIABLE_PREFIX))
			return -1;

		if (variable.contains(StateSpec.GOAL_VARIABLE_PREFIX))
			return -1;

		int termIndex = (variable.charAt(1) + MODULO_LETTERS - STARTING_CHAR)
				% MODULO_LETTERS;
		return termIndex;
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
}
