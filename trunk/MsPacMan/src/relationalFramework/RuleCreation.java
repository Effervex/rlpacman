package relationalFramework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import relationalFramework.agentObservations.AgentObservations;
import relationalFramework.agentObservations.PreGoalInformation;

import jess.Fact;
import jess.Rete;

/**
 * A class which deals specifically with covering rules from a state.
 * 
 * @author Samuel J. Sarjant
 */
public class RuleCreation implements Serializable {
	/** The first character for variables. */
	private static final char FIRST_CHAR = 'A';
	/** The final character for variables. */
	private static final char MODULO_LETTERS = 26;
	private static final long serialVersionUID = 5933775044794142265L;
	/** The starting character for variables. */
	private static final char STARTING_CHAR = 'X';

	/**
	 * The backup pre-goal information for when the agent observations object is
	 * migrated.
	 */
	private Map<String, PreGoalInformation> backupPreGoals_;

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
	private boolean checkConditionTypes(SortedSet<StringFact> ruleConds,
			StringFact condition) {
		// Run through the rule conditions.
		for (StringFact ruleCond : ruleConds) {
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
	 * Checks a fact for the presence of numerical arguments.
	 * 
	 * @param fact
	 *            The fact to check.
	 * @return Returns the indices of any numerical arguments.
	 */
	private ArrayList<Integer> checkForNumerical(StringFact fact) {
		ArrayList<Integer> numericalIndices = new ArrayList<Integer>();
		String[] argTypes = fact.getArgTypes();
		for (int i = 0; i < argTypes.length; i++) {
			if (StateSpec.isNumberType(argTypes[i]))
				numericalIndices.add(i);
		}
		return numericalIndices;
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
	private GuidedRule createRangedSpecialisation(GuidedRule baseRule,
			StringFact condition, int condArgIndex, String rangeVariable,
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

		StringFact mutantFact = new StringFact(condition);
		mutantFact.getArguments()[condArgIndex] = replacementTerm;
		SortedSet<StringFact> cloneConds = baseRule.getConditions(false);
		cloneConds.remove(condition);
		cloneConds.add(mutantFact);

		GuidedRule mutant = new GuidedRule(cloneConds, baseRule.getAction(),
				baseRule);
		// If just a point, change the action.
		if (lowerBound == upperBound)
			mutant.getAction().replaceArguments(rangeVariable, lowerBound + "");
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
	private void createRangeSpecialisations(GuidedRule baseRule,
			StringFact preGoalFact, int preGoalIndex,
			Set<GuidedRule> subranges, StringFact condition, int condArgIndex,
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
	private Set<GuidedRule> splitRanges(GuidedRule baseRule,
			StringFact preGoalFact, int preGoalIndex, boolean isRuleMutant) {
		Set<GuidedRule> subranges = new HashSet<GuidedRule>();

		// Run through each condition
		for (StringFact condition : baseRule.getConditions(false)) {
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
	 * @param coveredRules
	 *            A starting point for the rules, if any exist.
	 * @return A list of guided rules, one for each action type.
	 */
	public List<GuidedRule> rlggState(Rete state,
			MultiMap<String, String[]> validActions,
			MultiMap<String, GuidedRule> coveredRules) throws Exception {
		// The relevant facts which contain the key term
		AgentObservations.getInstance()
				.scanState(StateSpec.extractFacts(state));

		// Run through each valid action.
		for (String action : validActions.keySet()) {
			// Get the list of previous rules, both LGG and non-LGG.
			List<GuidedRule> previousRules = coveredRules.getList(action);
			if (previousRules == null)
				previousRules = new ArrayList<GuidedRule>();

			// Gather the action facts for each valid action
			StringFact baseAction = StateSpec.getInstance().getStringFact(
					action);
			for (String[] actionArgs : validActions.get(action)) {
				StringFact actionFact = new StringFact(baseAction, actionArgs);
				AgentObservations.getInstance().gatherActionFacts(actionFact,
						null, false, null);
			}
		}

		// Get the RLGG from the invariant conditions from the action
		// conditions.
		return AgentObservations.getInstance().getRLGGActionRules();
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
	public SortedSet<StringFact> simplifyRule(SortedSet<StringFact> ruleConds,
			StringFact condition, boolean testForIllegalRule,
			boolean checkConditionUnification) {
		SortedSet<StringFact> simplified = new TreeSet<StringFact>(ruleConds);

		// If we have an optional added condition, check for duplicates/negation
		if (condition != null) {
			if (checkConditionUnification) {
				StringFact unification = Unification.getInstance().unifyFact(
						condition, ruleConds, new DualHashBidiMap(),
						new DualHashBidiMap(), new String[0], false, false);
				if (unification != null)
					return null;
			}

			condition.swapNegated();
			StringFact negUnification = Unification.getInstance().unifyFact(
					condition, ruleConds, new DualHashBidiMap(),
					new DualHashBidiMap(), new String[0], false, false);
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
	public Set<GuidedRule> specialiseRule(GuidedRule rule) {
		// TODO Look at using goal constants/module constants for rule
		// specialisations.
		Set<GuidedRule> specialisations = new HashSet<GuidedRule>();

		String actionPred = rule.getActionPredicate();
		// If the action has no action conditions, return empty
		Collection<StringFact> actionConditions = AgentObservations
				.getInstance().getSpecialisationConditions(actionPred);
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
					condition, true, true);
			if (specConditions != null) {
				GuidedRule specialisation = new GuidedRule(specConditions,
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
	 * Specialises a rule towards conditions seen in the pre-goal state.
	 * 
	 * @param rule
	 *            The rule to specialise.
	 * @return All single-step mutations the rule can take towards matching the
	 *         pre-goal state.
	 */
	@SuppressWarnings("unchecked")
	public Set<GuidedRule> specialiseToPreGoal(GuidedRule rule) {
		// TODO Specialise using the known goal constants. Maybe by using the
		// goal itself as a pre-goal and swapping action variables around:
		// E.g. (distance goal 0) & Agent has rule ... (distance ?X ?Y) ... =>
		// (move ?X ?Y ?Z)
		// So, swap all ?X with goal and maybe (numerical may be different) all
		// ?Y with 0.
		Set<GuidedRule> mutants = new HashSet<GuidedRule>();
		String actionPred = rule.getActionPredicate();
		PreGoalInformation preGoal = AgentObservations.getInstance()
				.getPreGoal(actionPred);

		// If we have a pre goal state
		SortedSet<StringFact> ruleConditions = rule.getConditions(false);
		if (preGoal != null) {
			Collection<StringFact> preGoalState = preGoal.getState();

			// Form a replacement terms map and possibly create term swapped
			// specialisations
			BidiMap replacementTerms = new DualHashBidiMap();
			String[] ruleTerms = rule.getAction().getArguments();
			String[] preGoalTerms = preGoal.getActionTerms();
			for (int i = 0; i < preGoalTerms.length; i++) {
				if (!preGoalTerms[i].equals(ruleTerms[i])
						&& !StateSpec.isNumber(preGoalTerms[i])) {
					replacementTerms.put(preGoalTerms[i], ruleTerms[i]);

					// Try to create term swapped specialisations
					// If the rule term is variable and not equal to the
					// pre-goal term, term swap.
					if (ruleTerms[i].charAt(0) == '?'
							&& !ruleTerms[i].equals(preGoalTerms[i])) {
						SortedSet<StringFact> specConditions = new TreeSet<StringFact>(
								ruleConditions.comparator());
						boolean isTypeValid = true;
						for (StringFact condition : ruleConditions) {
							// If this condition is a type predicate, ensure it
							// fits with the pre-goal term types
							if (StateSpec.getInstance().isTypePredicate(
									condition.getFactName())) {
								Collection<String> lineage = new HashSet<String>();
								StateSpec.getInstance().getTypeLineage(
										condition.getFactName(), lineage);
								Collection<String> termTypes = preGoal
										.getTermTypes(preGoalTerms[i]);
								if (termTypes == null
										|| !lineage.containsAll(termTypes)) {
									isTypeValid = false;
									break;
								}
							}

							StringFact replacement = new StringFact(condition);
							replacement.replaceArguments(ruleTerms[i],
									preGoalTerms[i]);
							specConditions.add(replacement);
						}

						// Only create if the term swapping is type valid.
						if (isTypeValid) {
							StringFact replAction = new StringFact(rule
									.getAction());
							replAction.replaceArguments(ruleTerms[i],
									preGoalTerms[i]);

							// Create the mutant
							GuidedRule mutant = new GuidedRule(specConditions,
									replAction, rule);
							mutant.setQueryParams(rule.getQueryParameters());
							mutant.expandConditions();
							mutants.add(mutant);
						}
					}
				}
			}

			// Attempt to add each condition in the pre-goal (using action terms
			// where appropriate)
			for (StringFact preGoalFact : preGoalState) {
				// Check if the fact is numerical
				ArrayList<Integer> indices = checkForNumerical(preGoalFact);
				if (!indices.isEmpty()) {
					// Split any ranges up
					for (Integer index : indices)
						mutants.addAll(splitRanges(rule, preGoalFact, index,
								rule.isMutant()));
				} else {
					StringFact replacedFact = new StringFact(preGoalFact);
					replacedFact.replaceArguments(replacementTerms, true);
					SortedSet<StringFact> specConditions = simplifyRule(
							ruleConditions, replacedFact, true, false);
					// Add the rule
					if (specConditions != null) {
						GuidedRule mutant = new GuidedRule(specConditions, rule
								.getAction(), rule);
						mutant.setQueryParams(rule.getQueryParameters());
						mutant.expandConditions();
						if (!mutant.equals(rule))
							mutants.add(mutant);
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

		// Don't swap modular variables
		if (variable.contains(Module.MOD_VARIABLE_PREFIX))
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
