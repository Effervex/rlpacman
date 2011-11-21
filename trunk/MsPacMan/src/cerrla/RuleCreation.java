package cerrla;

import relationalFramework.GoalCondition;
import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

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

import relationalFramework.agentObservations.AgentObservations;
import util.MultiMap;

import jess.Rete;

/**
 * A class which deals specifically with covering rules from a state.
 * 
 * @author Samuel J. Sarjant
 */
public class RuleCreation implements Serializable {
	private static final long serialVersionUID = -5239359052379344563L;

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
	private boolean checkConditionTypes(
			SortedSet<RelationalPredicate> ruleConds,
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
			RelationalPredicate condition, int condArgIndex,
			String rangeVariable, double lowerBound, double upperBound) {
		// Create the replacement term.
		RelationalArgument subrangeArg = new RelationalArgument(rangeVariable,
				lowerBound, upperBound);

		RelationalArgument[] mutantArgs = condition.getRelationalArguments();
		mutantArgs[condArgIndex] = subrangeArg;
		RelationalPredicate mutantFact = new RelationalPredicate(condition,
				mutantArgs);
		SortedSet<RelationalPredicate> cloneConds = baseRule
				.getConditions(false);
		cloneConds.remove(condition);
		cloneConds.add(mutantFact);

		RelationalRule mutant = new RelationalRule(cloneConds,
				baseRule.getAction(), baseRule);
		mutant.setQueryParams(baseRule.getQueryParameters());
		return mutant;
	}

	/**
	 * Determines the ranges and creates mutants for them.
	 * 
	 * @param baseRule
	 *            The base rule to mutate.
	 * @param subranges
	 *            The collection of rules to add to.
	 * @param condition
	 *            The original condition being mutated.
	 * @param condArgIndex
	 *            The index of the mutation.
	 * @param rangeArg
	 *            The actual ranged argument itself.
	 */
	private void createRangeSpecialisations(RelationalRule baseRule,
			Set<RelationalRule> subranges, RelationalPredicate condition,
			int condArgIndex, RelationalArgument rangeArg) {
		double[] ranges = rangeArg.getRangeArg();
		String rangeVariable = rangeArg.getStringArg();
		if (ranges[0] != ranges[1]) {
			// Create 3 ranges, the min and max split in two and a range
			// overlapping each centred about the middle.
			double halfVal = ranges[0] + (ranges[1] - ranges[0]) / 2;
			double quarterAmount = (ranges[1] - ranges[0]) / 4;
			subranges.add(createRangedSpecialisation(baseRule, condition,
					condArgIndex, rangeVariable, ranges[0], halfVal));
			subranges.add(createRangedSpecialisation(baseRule, condition,
					condArgIndex, rangeVariable, halfVal, ranges[1]));
			subranges.add(createRangedSpecialisation(baseRule, condition,
					condArgIndex, rangeVariable, halfVal - quarterAmount,
					halfVal + quarterAmount));
		}

		if (!baseRule.isMutant()) {
			// If the ranges go through 0 and this rule isn't a
			// mutant, split at 0
			if (ranges[0] * ranges[1] < 0) {
				subranges.add(createRangedSpecialisation(baseRule, condition,
						condArgIndex, rangeVariable, ranges[0], 0));
				subranges.add(createRangedSpecialisation(baseRule, condition,
						condArgIndex, rangeVariable, 0, ranges[1]));
			}
		}
	}

	/**
	 * Splits any ranges in a rule into a number of smaller uniform sub-ranges.
	 * 
	 * @param baseRule
	 *            The base rule to mutate.
	 * @param isRuleMutant
	 *            If the rule is a mutant or not.
	 * @return A collection of any sub-ranged mutants created from the rule.
	 */
	private Set<RelationalRule> splitRanges(RelationalRule baseRule,
			boolean isRuleMutant) {
		Set<RelationalRule> subranges = new HashSet<RelationalRule>();

		// Run through each condition
		for (RelationalPredicate condition : baseRule.getConditions(false)) {
			for (int i = 0; i < condition.getArguments().length; i++) {
				RelationalArgument arg = condition.getRelationalArguments()[i];
				if (arg.isRange()) {
					createRangeSpecialisations(baseRule, subranges, condition,
							i, arg);
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
	 * @param moduleGoal
	 *            The modular goal (if any).
	 * @return A list of guided rules, one for each action type.
	 */
	public List<RelationalRule> rlggState(Rete state,
			MultiMap<String, String[]> validActions,
			Map<String, String> goalReplacements, GoalCondition moduleGoal)
			throws Exception {
		// The relevant facts which contain the key term
		AgentObservations.getInstance().scanState(
				StateSpec.extractFacts(state), goalReplacements);

		// Run through each valid action.
		for (String action : validActions.keySet()) {
			// Gather the action facts for each valid action
			RelationalPredicate baseAction = StateSpec.getInstance()
					.getStringFact(action);
			for (String[] actionArgs : validActions.get(action)) {
				RelationalPredicate actionFact = new RelationalPredicate(
						baseAction, actionArgs);
				AgentObservations.getInstance().gatherActionFacts(actionFact,
						goalReplacements);
			}
		}

		// Get the RLGG from the invariant conditions from the action
		// conditions.
		int numGoalArgs = 0;
		if (goalReplacements == null)
			numGoalArgs = moduleGoal.getNumArgs();
		else
			numGoalArgs = goalReplacements.size();
		List<String> queryTerms = new ArrayList<String>(numGoalArgs);
		for (int i = 0; i < numGoalArgs; i++)
			queryTerms.add(RelationalArgument.createGoalTerm(i));
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
	 * @param ruleAction
	 *            The optional action for the rule being simplified.
	 * @param exitIfIllegalRule
	 *            If the conditions need to be tested for illegal combinations
	 *            as well (use background knowledge in conjugated form)
	 * @return A modified version of the input rule conditions, or null if no
	 *         change made.
	 */
	public SortedSet<RelationalPredicate> simplifyRule(
			SortedSet<RelationalPredicate> ruleConds,
			RelationalPredicate condition, RelationalPredicate ruleAction,
			boolean exitIfIllegalRule) {
		SortedSet<RelationalPredicate> simplified = new TreeSet<RelationalPredicate>(
				ruleConds);
		// Add the RLGG conditions to assist in simplification (don't add
		// numerical predicates)
		if (ruleAction != null) {
			for (RelationalPredicate rlggPred : AgentObservations.getInstance()
					.getRLGGConditions(ruleAction)) {
				if (!rlggPred.isNumerical())
					simplified.add(rlggPred);
			}
		}

		// If we have an optional added condition, check for duplicates/negation
		if (condition != null) {
			condition.swapNegated();
			Collection<UnifiedFact> negUnification = Unification.getInstance()
					.unifyFact(condition, ruleConds, null, null, false, false);
			condition.swapNegated();
			if (!negUnification.isEmpty())
				return null;

			// Need to check type conditions
			if (!checkConditionTypes(ruleConds, condition))
				return null;

			simplified.add(condition);
		}

		// Simplify using the learned background knowledge and local invariants
		int result = AgentObservations.getInstance().simplifyRule(simplified,
				exitIfIllegalRule, false);
		// If rule is illegal, return null
		if (exitIfIllegalRule && result == -1)
			return null;

		// If no change from original condition, return null
		if (simplified.equals(ruleConds))
			return null;

		// Check the rule contains only valid goal facts
		MultiMap<String, RelationalPredicate> goalPreds = AgentObservations
				.getInstance().getGoalPredicateMap();
		Map<String, String> replacementMap = new HashMap<String, String>();
		for (String goalTerm : goalPreds.keySet())
			replacementMap.put(goalTerm, goalTerm);

		for (RelationalPredicate sf : simplified)
			if (!isValidGoalCondition(sf, goalPreds.values(), replacementMap))
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
			SortedSet<RelationalPredicate> specConditions = simplifyRule(
					conditions, condition, action, true);
			if (specConditions != null) {
				RelationalRule specialisation = new RelationalRule(
						specConditions, action, rule);
				specialisation.setQueryParams(rule.getQueryParameters());
				specialisation.expandConditions();
				if (!specialisations.contains(specialisation)
						&& !specialisation.equals(rule)) {
					// Only add specialisations if they contain goal conditions
					// (if there are goal conditions).
					if (!ProgramArgument.ONLY_GOAL_RULES.booleanValue()
							|| AgentObservations.getInstance().getNumGoalArgs() == 0
							|| specConditions.toString().contains(
									RelationalArgument.GOAL_VARIABLE_PREFIX))
						specialisations.add(specialisation);
				}
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
					if (!RelationalArgument.isGoalCondition(oldTerms[i])) {
						RelationalRule swappedRule = swapRuleTerm(rule,
								goalPredicates, oldTerms, i, goalTerm);
						if (swappedRule != null)
							mutants.add(swappedRule);
					}
				}
		}

		// Split any ranges up
		mutants.addAll(splitRanges(rule, rule.isMutant()));

		return mutants;
	}

	/**
	 * Swaps a term in a rule for another goal term, assuming the swap is valid
	 * in regards to the existing conditions.
	 * 
	 * @param rule
	 *            The rule.
	 * @param goalPredicates
	 *            The goal predicate replacement map.
	 * @param oldTerms
	 *            The rule action terms.
	 * @param i
	 *            The action term index.
	 * @param goalTerm
	 *            The goal term to replace in.
	 * @return The swapped rule term (if valid).
	 */
	private RelationalRule swapRuleTerm(RelationalRule rule,
			MultiMap<String, RelationalPredicate> goalPredicates,
			String[] oldTerms, int i, String goalTerm) {
		String[] newTerms = Arrays.copyOf(oldTerms, oldTerms.length);
		newTerms[i] = goalTerm;
		SortedSet<RelationalPredicate> ruleConditions = rule
				.getConditions(true);
		SortedSet<RelationalPredicate> specConditions = new TreeSet<RelationalPredicate>(
				ruleConditions.comparator());
		// Form the replacement map
		Map<String, String> replacementMap = new HashMap<String, String>();
		replacementMap.put(oldTerms[i], newTerms[i]);
		for (String gTerm : goalPredicates.keySet()) {
			replacementMap.put(gTerm, gTerm);
		}

		// Run through the rule conditions, checking each replaced
		// term is valid in regards to goal options.
		boolean validRule = true;
		for (RelationalPredicate cond : ruleConditions) {
			// Check if the condition is a valid goal condition.
			if (isValidGoalCondition(cond, goalPredicates.values(),
					replacementMap)) {
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

		AgentObservations.getInstance().simplifyRule(specConditions, false,
				false);

		if (validRule) {
			// Create the mutant
			RelationalRule mutant = new RelationalRule(specConditions,
					new RelationalPredicate(rule.getAction(), newTerms), rule);
			mutant.setQueryParams(rule.getQueryParameters());
			mutant.expandConditions();
			return mutant;
		}
		return null;
	}

	/**
	 * Checks if a condition is a valid condition based on the known goal
	 * conditions. The condition will either be fully anonymous, or contain goal
	 * conditions in such a way as to match the goal conditions.
	 * 
	 * @param cond
	 *            The condition to check.
	 * @param goalPredicates
	 *            The observed goal predicates.
	 * @param replacementMap
	 *            The replacement map to use. Must not have variables as values
	 *            (only as keys).
	 * 
	 * @return True if the condition is valid, false otherwise.
	 */
	private boolean isValidGoalCondition(RelationalPredicate cond,
			Collection<RelationalPredicate> goalPredicates,
			Map<String, String> replacementMap) {
		RelationalPredicate checkedCond = new RelationalPredicate(cond);
		boolean notAnonymous = checkedCond.replaceArguments(replacementMap,
				false, false);
		return goalPredicates.contains(checkedCond) || !notAnonymous
				|| cond.getFactName().equals(StateSpec.GOALARGS_PRED);
	}
}
