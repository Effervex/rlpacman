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
public class RuleCreation implements Serializable {
	private static final long serialVersionUID = -902117668118335074L;
	/** The starting character for variables. */
	private static final char STARTING_CHAR = 'X';
	/** The final character for variables. */
	private static final char MODULO_LETTERS = 26;
	/** The first character for variables. */
	private static final char FIRST_CHAR = 'A';
	/** The number of discretised ranges the rules are split into. */
	public static final int NUM_DISCRETE_RANGES = 4;

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
	public RuleCreation() {
		ao_ = loadAgentObservations();
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

			// TODO May be able to simply learn the RLGG here!
			Collection<StringFact> actionFacts = ao_.gatherActionFacts(action);

			// Inversely substitute the terms for variables (in string form)
			GuidedRule inverseSubbed = new GuidedRule(actionFacts, action, null);

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
					int changed = Unification.getInstance().unifyStates(
							ruleConditions, inverseSubbed.getConditions(false),
							ruleTerms, inverseSubbed.getActionTerms());

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
					.getConditions(false), null, false, true);
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
						new DualHashBidiMap(), new String[0], false);
				if (unification != null)
					return null;
			}

			condition.swapNegated();
			StringFact negUnification = Unification.getInstance().unifyFact(
					condition, ruleConds, new DualHashBidiMap(),
					new DualHashBidiMap(), new String[0], false);
			condition.swapNegated();
			if (negUnification != null)
				return null;

			simplified.add(condition);
		}

		// Simplify using the learned background knowledge
		ao_.simplifyRule(simplified, testForIllegalRule, false);

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
	@SuppressWarnings("unchecked")
	public Set<GuidedRule> specialiseToPreGoal(GuidedRule rule) {
		Set<GuidedRule> mutants = new HashSet<GuidedRule>();
		String actionPred = rule.getActionPredicate();
		PreGoalInformation preGoal = ao_.getPreGoal(actionPred);

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
					if (preGoalTerms[i].charAt(0) != '?'
							&& ruleTerms[i].charAt(0) == '?') {
						SortedSet<StringFact> specConditions = new TreeSet<StringFact>(
								ruleConditions.comparator());
						for (StringFact condition : ruleConditions) {
							StringFact replacement = new StringFact(condition);
							replacement.replaceArguments(ruleTerms[i],
									preGoalTerms[i]);
							specConditions.add(replacement);
						}
						StringFact replAction = new StringFact(rule.getAction());
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
				.getSpecialisationConditions(actionPred);
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
				double endPoint = Double.parseDouble(preGoalRangeSplit[3]);

				if (startPoint * endPoint < 0)
					values.add(0d);

				// Determine the ranges
				values.add(startPoint);
				values.add(endPoint);

				// Modify the range split if necessary
				if (((endPoint - startPoint) / NUM_DISCRETE_RANGES) > rangeSplit)
					rangeSplit = (endPoint - startPoint) / NUM_DISCRETE_RANGES;

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
				baseRule);
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
							// use the fact term
							boolean continueCreation = true;
							// If the condition is anonymous, replace it with
							// any term
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
											mutatedConditions, newCond, false,
											true);
									if (mutatedConditions != null) {
										GuidedRule mutant = new GuidedRule(
												mutatedConditions, baseRule
														.getAction(), baseRule);
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
					GuidedRule mutant = replaceActionTerm(baseRule, condArg,
							reverseReplacementTerms.get(condArg), preGoalState,
							preGoalTerms);
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
	 * @param baseRule
	 *            The base rule that is being specialised.
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
	private GuidedRule replaceActionTerm(GuidedRule baseRule,
			String replacedTerm, String replacementTerm,
			Collection<StringFact> preGoalState, String[] preGoalTerms) {
		// Replace the variables with their replacements.
		SortedSet<StringFact> ruleConditions = baseRule.getConditions(false);
		StringFact ruleAction = baseRule.getAction();
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
		if (Unification.getInstance().unifyStates(replacementConds,
				preGoalState, ruleAction.getArguments(), preGoalTerms) == 0) {
			// Adding the mutated rule
			GuidedRule mutant = new GuidedRule(replacementConds, ruleAction,
					baseRule);
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
	 * @return A collection of actions which have settled pre-goals.
	 */
	public Collection<String> formPreGoalState(Collection<Fact> preGoalState,
			ActionChoice actionChoice) {
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
							int result = Unification.getInstance().unifyStates(
									preGoal.getState(), preGoalStringState,
									preGoal.getActionTerms(),
									action.getArguments());

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
	public void migrateAgentObservations(RuleCreation otherCovering) {
		if (backupPreGoals_ == null)
			backupPreGoals_ = ao_.backupAndClearPreGoals();
		else
			ao_.clearPreGoal();
		otherCovering.ao_ = ao_;
	}

	/**
	 * Saves the agent observations to file, in the agent obs directory.
	 */
	public void saveAgentObservations() {
		ao_.saveAgentObservations();
	}

	/**
	 * Loads the agent observations from file.
	 */
	private AgentObservations loadAgentObservations() {
		AgentObservations ao = AgentObservations.loadAgentObservations();
		if (ao != null)
			return ao;
		else
			return new AgentObservations();
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
	 * Test method for adding background knowledge.
	 * 
	 * @param backKnow
	 *            The background knowledge to add.
	 */
	public void setBackgroundKnowledge(SortedSet<BackgroundKnowledge> backKnow) {
		ao_.setBackgroundKnowledge(backKnow);
	}
}
