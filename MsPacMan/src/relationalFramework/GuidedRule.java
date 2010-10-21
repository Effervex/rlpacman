package relationalFramework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that keeps track of the guided predicates that make up the rule
 * contained within.
 * 
 * @author Sam Sarjant
 */
public class GuidedRule {
	/**
	 * The rule has to see 5 states without changing to be considered
	 * artificially LGG.
	 */
	private static final int SETTLED_RULE_STATES = 50;

	/** The conditions of the rule. */
	private SortedSet<StringFact> ruleConditions_;
	
	/** The hash value of the rule. */
	private Integer ruleHash_ = null;

	/** The constant facts in the rule conditions, if any. Excludes type conds. */
	private ConstantPred constantConditions_;

	/** The guided predicate that defined the action. */
	private StringFact ruleAction_;

	/** The slot this rule was generated from. */
	private Slot slot_;

	/** If this slot is a mutation. */
	private boolean mutant_ = false;

	/** If this rule has spawned any mutant rules yet. */
	private Integer hasSpawned_ = null;

	/** The number of states seen by this rule. */
	private int statesSeen_ = 0;

	/** The number of times this rule has been used in a policy. */
	private int ruleUses_ = 0;

	/** The query parameters associated with this rule. */
	private List<String> queryParams_;

	/** The actual parameters given for this rule. */
	private List<String> parameters_;

	/** If this rule is from a loaded module. */
	private boolean isLoadedModule_ = false;

	/** The rule's internal mean. */
	private double internalMean_ = 0;

	/** The rule's internal value for calculating standard deviation. */
	private double internalS_ = 0;

	/** The rule's internal count of value updates. */
	private int internalCount_ = 0;

	/**
	 * A constructor taking the bare minimum for a guided rule.
	 * 
	 * @param rule
	 *            The rule this rule represents
	 */
	public GuidedRule(String ruleString) {
		String[] split = ruleString.split(StateSpec.INFERS_ACTION);
		ruleConditions_ = splitConditions(split[0]);
		ruleAction_ = StateSpec.toStringFact(split[1].trim());
		slot_ = null;
		expandConditions();
		findConstants();
	}

	/**
	 * A constructor taking in the raw conditions and actions.
	 * 
	 * @param conditions
	 *            The conditions for the rule.
	 * @param action
	 *            The actions for the rule.
	 * @param mutant
	 *            If this rule is a mutant rule.
	 */
	@SuppressWarnings("unchecked")
	public GuidedRule(Collection<StringFact> conditions, StringFact action,
			boolean mutant) {
		if (!(conditions instanceof SortedSet)) {
			ruleConditions_ = new TreeSet<StringFact>(ConditionComparator
					.getInstance());
			ruleConditions_.addAll(conditions);
		} else
			ruleConditions_ = (SortedSet<StringFact>) conditions;
		ruleAction_ = new StringFact(action);
		mutant_ = mutant;
		slot_ = null;
		findConstants();
		ruleHash_ = hashCode();
	}

	/**
	 * A constructor taking the rule and slot.
	 * 
	 * @param rule
	 *            The rule this rule represents
	 * @param slot
	 *            The slot this rule is under.
	 */
	public GuidedRule(String ruleString, Slot slot) {
		this(ruleString);
		slot_ = slot;
	}

	/**
	 * A constructor taking the bare minimum for a guided rule.
	 * 
	 * @param ruleString
	 *            The string representing this rule.
	 * @param maxGeneral
	 *            If this rule is maximally general.
	 * @param mutant
	 *            If this rule is a mutant (implying max general is false).
	 * @param slot
	 *            The slot this rule starts under.
	 */
	public GuidedRule(String ruleString, boolean maxGeneral, boolean mutant,
			Slot slot) {
		this(ruleString);
		mutant_ = mutant;
		slot_ = slot;
	}

	/**
	 * Creates a rule which is part of a parameterised query.
	 * 
	 * @param ruleString
	 *            The rule string.
	 * @param queryParams
	 *            The parameters used in the query.
	 */
	public GuidedRule(String ruleString, List<String> queryParams) {
		this(ruleString);
		queryParams_ = queryParams;
		isLoadedModule_ = true;
		findConstants();
	}

	/**
	 * Sets temporary parameter replacements for any parameterisable terms
	 * within. The temp parameters don't affect the rule itself, just the
	 * evaluation, so rule updating will not be affected.
	 * 
	 * @param parameters
	 *            The parameters being set.
	 */
	public void setParameters(List<String> parameters) {
		if (parameters == null) {
			parameters_ = null;
			return;
		}

		try {
			// Inits/Clears the parameters
			if (parameters_ == null)
				parameters_ = new ArrayList<String>();
			else
				parameters_.clear();

			// Sets the parameters
			parameters_.addAll(parameters);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (queryParams_ == null) {
			queryParams_ = new ArrayList<String>();
			for (int i = 0; i < parameters_.size(); i++)
				queryParams_.add(Module.createModuleParameter(i));
		}
	}

	/**
	 * Splits a conditions string into individual facts.
	 * 
	 * @param conditionString
	 *            The condition string to be split.
	 * @return The facts of the string in segments.
	 */
	public static SortedSet<StringFact> splitConditions(String conditionString) {
		SortedSet<StringFact> conds = new TreeSet<StringFact>(
				ConditionComparator.getInstance());
		Pattern p = Pattern.compile("\\(.+?\\)( |$)");
		Matcher m = p.matcher(conditionString);
		while (m.find()) {
			StringFact cond = StateSpec.toStringFact(m.group());
			conds.add(cond);
		}
		return conds;
	}

	/**
	 * Expands the conditions by parsing the base definitions of the condition
	 * into a fully type and inequal'ed condition.
	 */
	public void expandConditions() {
		Set<StringFact> addedConditions = new TreeSet<StringFact>();
		Set<StringFact> removedConditions = new TreeSet<StringFact>();

		Set<String> variableTerms = new TreeSet<String>();
		Set<String> constantTerms = new TreeSet<String>();
		for (StringFact condition : ruleConditions_) {
			if (StateSpec.getInstance().isUsefulPredicate(
					condition.getFactName())) {
				Map<String, StringFact> predicates = StateSpec.getInstance()
						.getPredicates();

				// Adding the terms
				String[] arguments = condition.getArguments();
				for (int i = 0; i < arguments.length; i++) {
					// Ignore numerical terms
					if ((predicates.get(condition.getFactName()) == null)
							|| (!StateSpec.isNumberClass(condition
									.getArgTypes()[i]))) {
						// Adding variable terms
						if (arguments[i].charAt(0) == '?') {
							if (arguments[i].length() > 1)
								variableTerms.add(arguments[i]);
						} else {
							// Adding constant terms
							constantTerms.add(arguments[i]);
						}
					}
				}

				// Adding the type arguments
				addedConditions.addAll(StateSpec.getInstance().createTypeConds(
						condition));
			} else {
				removedConditions.add(condition);
			}
		}

		ruleConditions_.removeAll(removedConditions);

		ruleConditions_.addAll(addedConditions);
		// Adding the inequality predicates
		ruleConditions_.addAll(StateSpec.createInequalityTests(variableTerms,
				constantTerms));
		
		Integer oldHash = ruleHash_;
		Integer newHash = hashCode();
		if (!newHash.equals(oldHash))
			statesSeen_ = 0;
		ruleHash_ = newHash;
	}

	/**
	 * Finds the constants in the rule conditions.
	 */
	private void findConstants() {
		List<StringFact> constants = new ArrayList<StringFact>();
		for (StringFact cond : ruleConditions_) {
			// If the condition isn't a type predicate or test
			if (!StateSpec.getInstance().isTypePredicate(cond.getFactName())
					&& StateSpec.getInstance().isUsefulPredicate(
							cond.getFactName()) && !cond.isNegated()) {
				// If the condition doesn't contain variables - except modular
				// variables
				boolean isConstant = true;
				for (String argument : cond.getArguments()) {
					// If we're looking at a variable, but not a module variable
					if (argument.contains("?")
							&& (argument.length() <= Module.MOD_VARIABLE_PREFIX
									.length() || !argument.substring(0,
									Module.MOD_VARIABLE_PREFIX.length())
									.equals(Module.MOD_VARIABLE_PREFIX))) {
						// It may be a parameter, else return false.
						if ((queryParams_ == null)
								|| (!queryParams_.contains(argument))) {
							isConstant = false;
							break;
						}
					}
				}

				if (isConstant) {
					constants.add(cond);
				}
			}
		}

		if (!constants.isEmpty())
			constantConditions_ = new ConstantPred(constants);
		else
			constantConditions_ = null;
	}

	/**
	 * Removes unnecessary conditions from the set of conditions by removing any
	 * not containing action terms.
	 * 
	 * @param conditions
	 *            The conditions to scan through and remove.
	 */
	private void removeUnnecessaryFacts(SortedSet<StringFact> conditions) {
		// Run through the conditions, ensuring each one has at least one unique
		// term seen in the action.
		for (Iterator<StringFact> iter = conditions.iterator(); iter.hasNext();) {
			StringFact condition = iter.next();

			// Check if any of the terms are in the condition
			boolean containsAny = false;
			for (String term : ruleAction_.getArguments()) {
				if (StateSpec.arrayContains(condition.getArguments(), term)) {
					containsAny = true;
					break;
				}
			}

			// Removing unnecessary terms
			if (!containsAny) {
				iter.remove();
			}
		}
	}

	/**
	 * Updates the internal value of this rule, adjusting the rule mean and SD
	 * appropriately.
	 * 
	 * @param value
	 *            The value the rule attained as part of a policy.
	 */
	public void updateInternalValue(float value) {
		internalCount_++;

		if (internalCount_ == 1) {
			internalMean_ = value;
			internalS_ = 0;
		} else {
			double newMean = internalMean_ + (value - internalMean_)
					/ (internalCount_);
			double newS = internalS_ + (value - internalMean_)
					* (value - newMean);
			internalMean_ = newMean;
			internalS_ = newS;
		}
	}

	/**
	 * Gets the internal mean for the rule.
	 * 
	 * @return The rule's internal mean.
	 */
	public double getInternalMean() {
		return internalMean_;
	}

	/**
	 * Gets the internal SD for the rule (which uses the count).
	 * 
	 * @return The internal standard deviation.
	 */
	public double getInternalSD() {
		if (internalCount_ <= 1)
			return 0;
		return Math.sqrt(internalS_ / (internalCount_ - 1));
	}

	public int getUses() {
		return ruleUses_;
	}

	public Slot getSlot() {
		return slot_;
	}

	/**
	 * Gets the conditions in a sorted order, including the inequals predicate.
	 * 
	 * @return The conditions of the rule.
	 */
	public SortedSet<StringFact> getConditions() {
		return new TreeSet<StringFact>(ruleConditions_);
	}

	/**
	 * Sets the conditions of this guided rule, if the conditions are valid.
	 * 
	 * @param conditions
	 *            The conditions for the guided rule.
	 * @param removeUnnecessaryFacts
	 *            If, during the setting process, facts not containing any of
	 *            the action terms should be removed.
	 * @return True if the newly set conditions are valid.
	 */
	public boolean setConditions(SortedSet<StringFact> conditions,
			boolean removeUnnecessaryFacts) {
		// If the conditions are the same, return true.
		if (conditions.equals(ruleConditions_)) {
			return true;
		}

		// Instead, introduce method (remove unnecessary conditions).
		if (removeUnnecessaryFacts)
			removeUnnecessaryFacts(conditions);

		// Reset the states seen, as the rule has changed.
		hasSpawned_ = null;
		ruleConditions_ = conditions;
		ruleUses_ = 0;
		findConstants();
		return true;
	}

	/**
	 * Shifts any modular rule variable names by an amount. e.g. ?_MOD_a ->
	 * ?_MOD_c when i = 2.
	 * 
	 * @param i
	 *            The amount to shift the variable name.
	 */
	public void shiftModularVariables(int i) {
		if (i == 0)
			return;

		// Run through each condition, removing it, then readding it back with
		// possibly changed arguments.
		replaceArguments(i, ruleConditions_);
		// Do the same for the constants
		if (constantConditions_ != null)
			replaceArguments(i, constantConditions_.getFacts());
	}

	/**
	 * Replaces modular arguments with shifted arguments.
	 * 
	 * @param i
	 *            The amount to shift by.
	 * @param strFacts
	 *            The collection to replace the arguments within.
	 */
	private void replaceArguments(int i, Collection<StringFact> strFacts) {
		Map<String, String> replacementMap = new HashMap<String, String>();
		// Create the replacement map.
		for (int j = 0; j < queryParams_.size(); j++) {
			replacementMap.put(Module.createModuleParameter(j), Module
					.createModuleParameter(j + i));
		}

		// Replace for each condition.
		for (Iterator<StringFact> condIter = strFacts.iterator(); condIter
				.hasNext();) {
			StringFact cond = condIter.next();
			cond.replaceArguments(replacementMap, true);
		}
	}

	public String getStringConditions() {
		StringBuffer buffer = new StringBuffer();
		for (StringFact cond : ruleConditions_) {
			buffer.append(cond + " ");
		}

		return buffer.toString();
	}

	public ConstantPred getConstantConditions() {
		return constantConditions_;
	}

	public StringFact getAction() {
		return ruleAction_;
	}

	public String[] getActionTerms() {
		return ruleAction_.getArguments();
	}

	public List<String> getQueryParameters() {
		return queryParams_;
	}

	public void setQueryParams(List<String> queryParameters) {
		if (queryParameters != null)
			queryParams_ = new ArrayList<String>(queryParameters);
		else
			queryParams_ = null;
	}

	public List<String> getParameters() {
		return parameters_;
	}

	/**
	 * Gets the parameter replacement for the query parameter if one exists.
	 * 
	 * @param queryParam
	 *            The query parameter to replace.
	 * @return The replacement parameter or the original variable if none given.
	 */
	public String getReplacementParameter(String queryParam) {
		if (parameters_ == null)
			return queryParam;

		return parameters_.get(queryParams_.indexOf(queryParam));
	}

	public boolean isLoadedModuleRule() {
		return isLoadedModule_;
	}

	/**
	 * Sets this rule as a loaded module.
	 */
	public void setAsLoadedModuleRule(boolean isModule) {
		isLoadedModule_ = isModule;
	}

	/**
	 * Gets the action predicate.
	 * 
	 * @return The action predicate for the action.
	 */
	public String getActionPredicate() {
		return ruleAction_.getFactName();
	}

	/**
	 * Sets the new action terms, and modifies the action if necessary.
	 * 
	 * @param terms
	 *            The new action terms.
	 */
	public void setActionTerms(String[] terms) {
		ruleAction_ = new StringFact(ruleAction_, terms);
	}

	public void setSlot(Slot slot) {
		slot_ = slot;
	}

	public boolean isMutant() {
		return mutant_;
	}

	public void setMutant(boolean mutant) {
		mutant_ = mutant;
	}

	/**
	 * Sets the rules spawned to this pre-goal hash, or null if unspawned.
	 * 
	 * @param preGoalHash
	 *            The hash of the pre-goal the rules spawned to.
	 */
	public void setSpawned(Integer preGoalHash) {
		hasSpawned_ = preGoalHash;
	}

	/**
	 * Checks if this rule has spawned to the current pre-goal.
	 * 
	 * @param preGoalHash
	 *            The hash of the pre-goal.
	 * @return True if the rule has spawned to this pre-goal, false otherwise.
	 */
	public boolean hasSpawned(int preGoalHash) {
		if (hasSpawned_ == null)
			return false;
		return hasSpawned_.equals(preGoalHash);
	}

	/**
	 * Increments the state seen counter, if necessary.
	 */
	public void incrementStatesCovered() {
		if (statesSeen_ <= SETTLED_RULE_STATES)
			statesSeen_++;
	}

	public void incrementRuleUses() {
		ruleUses_++;
	}

	/**
	 * If this rule was recently modified (so states seen is reset).
	 * 
	 * @return True if the rule has recently changed/been created.
	 */
	public boolean isRecentlyModified() {
		if (statesSeen_ <= 1)
			return true;
		return false;
	}

	/**
	 * Clone this rule.
	 * 
	 * @return A clone of this rule.
	 */
	@Override
	public Object clone() {
		Collection<StringFact> cloneConds = new ArrayList<StringFact>();
		for (StringFact cond : ruleConditions_)
			cloneConds.add(new StringFact(cond));
		GuidedRule clone = new GuidedRule(cloneConds, ruleAction_, mutant_);
		clone.hasSpawned_ = hasSpawned_;
		clone.isLoadedModule_ = isLoadedModule_;
		clone.statesSeen_ = statesSeen_;
		
		if (queryParams_ != null)
			clone.queryParams_ = new ArrayList<String>(queryParams_);
		if (parameters_ != null)
			clone.parameters_ = new ArrayList<String>(parameters_);
		return clone;
	}

	@Override
	public String toString() {
		return getStringConditions() + "=> " + ruleAction_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((ruleAction_ == null) ? 0 : ruleAction_.hashCode());
		int conditionResult = 0;
		for (StringFact condition : ruleConditions_)
			conditionResult += condition.hashCode();
		result = prime * result + conditionResult;
		result = prime * result
				+ ((queryParams_ == null) ? 0 : queryParams_.hashCode());
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
		GuidedRule other = (GuidedRule) obj;
		if (ruleAction_ == null) {
			if (other.ruleAction_ != null)
				return false;
		} else if (!ruleAction_.equals(other.ruleAction_))
			return false;
		if (ruleConditions_ == null) {
			if (other.ruleConditions_ != null)
				return false;
		} else if (other.ruleConditions_ == null)
			return false;
		else if (!ruleConditions_.equals(other.ruleConditions_))
			return false;
		if (queryParams_ == null) {
			if (other.queryParams_ != null)
				return false;
		} else if (!queryParams_.equals(other.queryParams_))
			return false;
		return true;
	}
}