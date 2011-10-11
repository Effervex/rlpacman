package relationalFramework;

import relationalFramework.GoalCondition;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
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

import cerrla.Slot;

import util.ConditionComparator;

/**
 * A class that keeps track of the guided predicates that make up the rule
 * contained within.
 * 
 * @author Sam Sarjant
 */
public class RelationalRule implements Serializable, Comparable<RelationalRule> {
	private static final long serialVersionUID = -7517726681678896438L;

	/**
	 * The rule has to see 5 states without changing to be considered
	 * artificially LGG.
	 */
	private static final int SETTLED_RULE_STATES = 50;

	/** The ancestry count of the rule (how far from the RLGG). */
	private int ancestryCount_;

	/** The constant facts in the rule conditions, if any. Excludes type conds. */
	private GoalCondition constantCondition_;

	/** If this rule has spawned any mutant rules yet. */
	private Integer hasSpawned_ = null;

	/** The rule's internal count of value updates. */
	private int internalCount_ = 0;

	/** The rule's internal mean. */
	private double internalMean_ = 0;

	/** The rule's internal value for calculating standard deviation. */
	private double internalS_ = 0;

	/** The modular parameters for loaded module rules. */
	private List<String> moduleParams_;

	/** If this slot is a mutation. */
	private boolean mutant_ = false;

	/** If the rule has mutated, the children of the mutation. */
	private Collection<RelationalRule> mutantChildren_;

	/** If the rule is a mutant, the parent of the mutant. */
	private Collection<RelationalRule> mutantParents_;

	/** The actual parameters given for this rule. */
	private List<String> parameters_;

	/** The query parameters associated with this rule. */
	private List<String> queryParams_;

	/** The guided predicate that defined the action. */
	private RelationalPredicate ruleAction_;

	/** The conditions of the rule. */
	private SortedSet<RelationalPredicate> ruleConditions_;

	/** The hash value of the rule. */
	private Integer ruleHash_ = null;

	/** The number of times this rule has been used in a policy. */
	private int ruleUses_ = 0;

	/** The slot this rule was generated from. */
	private Slot slot_;

	/** The number of states seen by this rule. */
	private int statesSeen_ = 0;

	/** The term replacement map built from the parameter members. */
	private Map<String, String> termReplacements_;

	/**
	 * A private constructor used only for the clone.
	 * 
	 * @param conditions
	 *            The conditions for the rule.
	 * @param action
	 *            The actions for the rule.
	 */
	private RelationalRule(Collection<RelationalPredicate> cloneConds,
			RelationalPredicate ruleAction) {
		this(cloneConds, ruleAction, null);
	}

	/**
	 * A constructor taking in the raw conditions and actions.
	 * 
	 * @param conditions
	 *            The conditions for the rule.
	 * @param action
	 *            The actions for the rule.
	 * @param parent
	 *            If this rule has a parent - hence is a mutant (null if not).
	 */
	public RelationalRule(Collection<RelationalPredicate> conditions,
			RelationalPredicate action, RelationalRule parent) {
		if (!(conditions instanceof SortedSet)) {
			ruleConditions_ = new TreeSet<RelationalPredicate>(
					ConditionComparator.getInstance());
			ruleConditions_.addAll(conditions);
		} else
			ruleConditions_ = (SortedSet<RelationalPredicate>) conditions;
		if (action != null)
			ruleAction_ = new RelationalPredicate(action);
		if (parent != null)
			setMutant(parent);
		slot_ = null;
		findConstants();
		ruleHash_ = hashCode();
	}

	/**
	 * A constructor taking the bare minimum for a guided rule.
	 * 
	 * @param rule
	 *            The rule this rule represents
	 */
	public RelationalRule(String ruleString) {
		String[] split = ruleString.split(StateSpec.INFERS_ACTION);
		ruleConditions_ = splitConditions(split[0]);
		if (split.length == 2)
			ruleAction_ = StateSpec.toRelationalPredicate(split[1].trim());
		slot_ = null;
		ancestryCount_ = 0;
		expandConditions();
		findConstants();
	}

	/**
	 * Creates a rule which is part of a parameterised query.
	 * 
	 * @param ruleString
	 *            The rule string.
	 * @param queryParams
	 *            The parameters used in the query.
	 */
	public RelationalRule(String ruleString, List<String> queryParams) {
		this(ruleString);
		queryParams_ = queryParams;
		findConstants();
	}

	/**
	 * Builds the module replacement map which swaps the rule's query parameters
	 * with module ones.
	 */
	private void buildModuleReplacementMap() {
		termReplacements_ = new HashMap<String, String>();
		if (queryParams_ == null)
			return;

		for (int i = 0; i < queryParams_.size(); i++) {
			if (moduleParams_ != null)
				termReplacements_
						.put(queryParams_.get(i), moduleParams_.get(i));
		}
	}

	/**
	 * Finds the constants in the rule conditions.
	 */
	private void findConstants() {
		List<RelationalPredicate> constants = new ArrayList<RelationalPredicate>();
		for (RelationalPredicate cond : ruleConditions_) {
			// If the condition isn't a type predicate or test
			if (!StateSpec.getInstance().isTypePredicate(cond.getFactName())
					&& StateSpec.getInstance().isUsefulPredicate(
							cond.getFactName()) && !cond.isNegated()) {
				// If the condition doesn't contain variables - except modular
				// variables
				boolean isConstant = true;
				for (String argument : cond.getArguments()) {
					// If the arg isn't a constant or a goal term, the condition
					// isn't a constant condition.
					if (argument.startsWith("?")
							&& !argument
									.startsWith(StateSpec.GOAL_VARIABLE_PREFIX)) {
						isConstant = false;
						break;
					}
				}

				if (isConstant) {
					constants.add(cond);
				}
			}
		}

		if (constants.isEmpty())
			constantCondition_ = null;
		else
			constantCondition_ = new GoalCondition(constants);
	}

	/**
	 * Removes unnecessary conditions from the set of conditions by removing any
	 * not containing action terms.
	 * 
	 * @param conditions
	 *            The conditions to scan through and remove.
	 */
	private void removeUnnecessaryFacts(
			SortedSet<RelationalPredicate> conditions) {
		// Run through the conditions, ensuring each one has at least one unique
		// term seen in the action.
		for (Iterator<RelationalPredicate> iter = conditions.iterator(); iter
				.hasNext();) {
			RelationalPredicate condition = iter.next();

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
	 * Adds all parents to this rule.
	 * 
	 * @param parentRules
	 *            The parent rules to add.
	 */
	public void addParents(Collection<RelationalRule> parentRules) {
		if (mutantParents_ == null)
			mutantParents_ = new ArrayList<RelationalRule>();
		mutantParents_.addAll(parentRules);
	}

	/**
	 * Clone this rule.
	 * 
	 * @param cloneInternalMembers If internal counters should also be cloned.
	 * @return A clone of this rule.
	 */
	public RelationalRule clone(boolean cloneInternalMembers) {
		Collection<RelationalPredicate> cloneConds = new ArrayList<RelationalPredicate>();
		for (RelationalPredicate cond : ruleConditions_)
			cloneConds.add(new RelationalPredicate(cond));
		RelationalRule clone = new RelationalRule(cloneConds, ruleAction_);
		if (!cloneInternalMembers)
			return clone;
		
		clone.hasSpawned_ = hasSpawned_;
		clone.statesSeen_ = statesSeen_;
		clone.mutant_ = mutant_;
		if (mutantParents_ != null)
			clone.mutantParents_ = new ArrayList<RelationalRule>(mutantParents_);

		if (queryParams_ != null)
			clone.queryParams_ = new ArrayList<String>(queryParams_);
		if (parameters_ != null)
			clone.parameters_ = new ArrayList<String>(parameters_);
		return clone;
	}

	@Override
	public int compareTo(RelationalRule o) {
		if (o == null)
			return -1;
		int result = Double.compare(ruleHash_, o.ruleHash_);
		if (result != 0)
			return result;

		return toString().compareTo(o.toString());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RelationalRule other = (RelationalRule) obj;
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
		return true;
	}

	/**
	 * Expands the conditions by parsing the base definitions of the condition
	 * into a fully type and inequal'ed condition.
	 */
	public void expandConditions() {
		Set<RelationalPredicate> addedConditions = new TreeSet<RelationalPredicate>();
		Set<RelationalPredicate> removedConditions = new TreeSet<RelationalPredicate>();

		Set<String> variableTerms = new TreeSet<String>();
		Set<String> constantTerms = new TreeSet<String>();
		for (RelationalPredicate condition : ruleConditions_) {
			if (StateSpec.getInstance().isUsefulPredicate(
					condition.getFactName())) {
				Map<String, RelationalPredicate> predicates = StateSpec
						.getInstance().getPredicates();

				// Adding the terms
				String[] arguments = condition.getArguments();
				for (int i = 0; i < arguments.length; i++) {
					// Ignore numerical terms
					if ((predicates.get(condition.getFactName()) == null)
							|| (!StateSpec
									.isNumberType(condition.getArgTypes()[i]))) {
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
			} else if (condition.getFactName().equals(StateSpec.GOALARGS_PRED))
				addedConditions.add(condition);
			else
				removedConditions.add(condition);
		}

		if (ruleAction_ != null)
			addedConditions.addAll(StateSpec.getInstance().createTypeConds(
					ruleAction_));

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

	public RelationalPredicate getAction() {
		return ruleAction_;
	}

	/**
	 * Gets the action predicate.
	 * 
	 * @return The action predicate for the action.
	 */
	public String getActionPredicate() {
		return ruleAction_.getFactName();
	}

	public String[] getActionTerms() {
		return ruleAction_.getArguments();
	}

	public Collection<RelationalRule> getChildrenRules() {
		return mutantChildren_;
	}

	/**
	 * Gets the conditions in a sorted order, including the inequals predicate.
	 * 
	 * @param withoutInequals
	 *            If inequals predicates should be removed.
	 * @return The conditions of the rule.
	 */
	public SortedSet<RelationalPredicate> getConditions(boolean withoutInequals) {
		if (withoutInequals) {
			SortedSet<RelationalPredicate> conds = new TreeSet<RelationalPredicate>(
					ruleConditions_.comparator());
			for (RelationalPredicate cond : ruleConditions_) {
				if (!cond.getFactName().equals("test"))
					conds.add(cond);
			}
			return conds;
		}
		return new TreeSet<RelationalPredicate>(ruleConditions_);
	}

	public GoalCondition getConstantCondition() {
		return constantCondition_;
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

	public List<String> getModuleParameters() {
		return moduleParams_;
	}

	public List<String> getParameters() {
		return parameters_;
	}

	public Collection<RelationalRule> getParentRules() {
		return mutantParents_;
	}

	public List<String> getQueryParameters() {
		return queryParams_;
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

	public Slot getSlot() {
		return slot_;
	}

	public String getStringConditions() {
		return StateSpec.conditionsToString(ruleConditions_);
	}

	public int getUses() {
		return ruleUses_;
	}

	/**
	 * Grounds this rule into a rule without modular parameters by swapping the
	 * rule conditions for the parameters.
	 * 
	 * @return A cloned, but modularly grounded, rule.
	 */
	public RelationalRule groundModular() {
		if (moduleParams_ == null) {
			return clone(false);
		}

		SortedSet<RelationalPredicate> groundConditions = new TreeSet<RelationalPredicate>(
				ruleConditions_.comparator());
		for (RelationalPredicate ruleCond : getConditions(true)) {
			RelationalPredicate groundCond = new RelationalPredicate(ruleCond);
			groundCond.replaceArguments(termReplacements_, true, false);
			groundConditions.add(groundCond);
		}
		RelationalPredicate groundAction = new RelationalPredicate(ruleAction_);
		groundAction.replaceArguments(termReplacements_, true, false);

		RelationalRule groundRule = new RelationalRule(groundConditions,
				groundAction, null);
		groundRule.expandConditions();
		groundRule.findConstants();
		
		return groundRule;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((ruleAction_ == null) ? 0 : ruleAction_.hashCode());
		int conditionResult = 0;
		if (ruleConditions_ != null)
			for (RelationalPredicate condition : ruleConditions_)
				conditionResult += condition.hashCode();
		result = prime * result + conditionResult;
		return result;
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

	public void incrementRuleUses() {
		ruleUses_++;
	}

	/**
	 * Increments the state seen counter, if necessary.
	 */
	public void incrementStatesCovered() {
		if (statesSeen_ <= SETTLED_RULE_STATES)
			statesSeen_++;
	}

	public boolean isMutant() {
		return mutant_;
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
	 * Returns true if the rule has no parents.
	 * 
	 * @return True if the rule now has no parents.
	 */
	public boolean isWithoutParents() {
		return (mutantParents_ == null) || (mutantParents_.isEmpty());
	}

	/**
	 * Removes the fact that this rule is a mutant and removes any parents.
	 */
	public void removeMutation() {
		mutant_ = false;
		mutantParents_ = null;
	}

	public void removeParameters() {
		parameters_ = null;
	}

	/**
	 * Removes a parent rule from this rule, possibly nullifying the set of
	 * parent rules for this rule.
	 * 
	 * @param parent
	 *            The parent rule to remove.
	 */
	public void removeParent(RelationalRule parent) {
		mutantParents_.remove(parent);
		if (mutantParents_.isEmpty())
			mutantParents_ = null;
	}

	/**
	 * Removes a group of parents from a rule.
	 * 
	 * @param parents
	 *            The group of parents to remove.
	 */
	public void removeParents(Collection<RelationalRule> parents) {
		for (RelationalRule parent : parents)
			removeParent(parent);
	}

	/**
	 * Sets the new action terms.
	 * 
	 * @param terms
	 *            The new action terms.
	 */
	public void setActionTerms(String[] terms) {
		ruleAction_ = new RelationalPredicate(ruleAction_, terms);
	}

	public void setChildren(Collection<RelationalRule> children) {
		mutantChildren_ = children;
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
	public boolean setConditions(SortedSet<RelationalPredicate> conditions,
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
	 * Sets the modular parameters (only when loading a modular rule). These are
	 * a special case which direct the module parameters to the appropriate goal
	 * parameters they are geared towards.
	 * 
	 * @param parameters
	 *            The special module parameters which act as a bridge between
	 *            the query params and the episodic parameters.
	 */
	public void setModularParameters(List<String> parameters) {
		moduleParams_ = parameters;
		buildModuleReplacementMap();
	}

	/**
	 * Sets this rule as a mutant and adds the parent rule.
	 * 
	 * @param parent
	 *            The parent rule to add.
	 */
	public void setMutant(RelationalRule parent) {
		if (mutantParents_ == null) {
			mutantParents_ = new ArrayList<RelationalRule>();
			ancestryCount_ = parent.ancestryCount_ + 1;
		}
		mutant_ = true;
		mutantParents_.add(parent);
		if (parent.ancestryCount_ < ancestryCount_ - 1)
			ancestryCount_ = parent.ancestryCount_ + 1;
	}

	public int getAncestryCount() {
		return ancestryCount_;
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
				queryParams_.add(StateSpec.createGoalTerm(i));
			findConstants();
		}
	}

	/**
	 * Sets the parameters of the the query parameters (given by the key) to the
	 * values.
	 * 
	 * @param parameterMap
	 *            The map of parameters.
	 */
	public void setParameters(Map<String, String> parameterMap) {
		boolean hasQueryParams = queryParams_ != null;
		if (!hasQueryParams)
			queryParams_ = new ArrayList<String>(parameterMap.size());
		parameters_ = new ArrayList<String>(parameterMap.size());
		for (int i = 0; i < parameterMap.size(); i++) {
			String goalTerm = StateSpec.createGoalTerm(i);
			if (!hasQueryParams)
				queryParams_.add(goalTerm);
			parameters_.add(parameterMap.get(goalTerm));
		}
		if (!hasQueryParams)
			findConstants();
	}

	/**
	 * Sets this rule's query parameters (replaceable variables). Generally
	 * these are just goal args.
	 * 
	 * @param queryParameters
	 *            The parameters to set.
	 */
	public void setQueryParams(List<String> queryParameters) {
		if (queryParameters != null)
			queryParams_ = new ArrayList<String>(queryParameters);
		else
			queryParams_ = null;
		findConstants();
	}

	public void setSlot(Slot slot) {
		slot_ = slot;
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
	 * Outputs the rule in a simplified, but essentially equivalent (assuming
	 * inequality and type definitions) format.
	 * 
	 * @return A nice, shortened version of the rule.
	 */
	public String toNiceString() {
		StringBuffer niceString = new StringBuffer();

		// Run through each condition, adding regular conditions and
		// non-standard type predicates
		Collection<RelationalPredicate> standardType = new HashSet<RelationalPredicate>();
		for (RelationalPredicate stringFact : ruleConditions_) {

			if (StateSpec.getInstance().isTypePredicate(
					stringFact.getFactName())) {
				// If a type predicate, only add it if it's non-standard
				if (!standardType.contains(stringFact))
					niceString.append(stringFact
							.toNiceString(termReplacements_) + " ");
			} else if (stringFact.getFactName().equals(StateSpec.GOALARGS_PRED)) {
				// Add the goal arg pred.
				// niceString.append(stringFact + " ");
			} else if (!stringFact.getFactName().equals("test")) {
				// If not a type or test, add the fact.
				niceString.append(stringFact.toNiceString(termReplacements_)
						+ " ");

				// Scan the arguments and extract the standard type preds
				for (int i = 0; i < stringFact.getArgTypes().length; i++) {
					// If the type isn't a number and isn't anonymous, add it to
					// the standards
					if (!stringFact.getArguments()[i].equals("?")
							&& !StateSpec
									.isNumberType(stringFact.getArgTypes()[i])) {
						RelationalPredicate type = new RelationalPredicate(
								StateSpec.getInstance().getStringFact(
										stringFact.getArgTypes()[i]),
								new String[] { stringFact.getArguments()[i] });
						standardType.add(type);
					}
				}
			}
		}
		niceString.append("=> " + ruleAction_.toNiceString(termReplacements_));

		return niceString.toString();
	}

	@Override
	public String toString() {
		return getStringConditions() + " => " + ruleAction_;
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
	 * Splits a conditions string into individual facts.
	 * 
	 * @param conditionString
	 *            The condition string to be split.
	 * @return The facts of the string in segments.
	 */
	public static SortedSet<RelationalPredicate> splitConditions(
			String conditionString) {
		SortedSet<RelationalPredicate> conds = new TreeSet<RelationalPredicate>(
				ConditionComparator.getInstance());
		Pattern p = Pattern.compile("\\(.+?\\)( |$)");
		Matcher m = p.matcher(conditionString);
		while (m.find()) {
			RelationalPredicate cond = StateSpec.toRelationalPredicate(m
					.group());
			conds.add(cond);
		}
		return conds;
	}
}