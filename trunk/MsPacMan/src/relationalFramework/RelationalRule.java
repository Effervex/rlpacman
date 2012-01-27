package relationalFramework;

import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.RangeContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.apache.commons.collections.BidiMap;

import cerrla.Slot;
import cerrla.SpecialisationOperator;
import cerrla.modular.GeneralGoalCondition;
import cerrla.modular.PolicyItem;
import cerrla.modular.SpecificGoalCondition;

import util.ConditionComparator;

/**
 * A class that keeps track of the guided predicates that make up the rule
 * contained within.
 * 
 * @author Sam Sarjant
 */
public class RelationalRule implements Serializable,
		Comparable<RelationalRule>, PolicyItem {
	private static final long serialVersionUID = -7517726681678896438L;

	/**
	 * The rule has to see 5 states without changing to be considered
	 * artificially LGG.
	 */
	private static final int SETTLED_RULE_STATES = 50;

	/** An abstract definition of this rule with regards to the RLGG. */
	private SortedSet<SpecialisationOperator> abstractRule_;

	/** The ancestry count of the rule (how far from the RLGG). */
	private int ancestryCount_;

	/** The constant facts in the rule conditions, if any. Excludes type conds. */
	private Collection<SpecificGoalCondition> constantCondition_;

	/** The general conditions of this rule. */
	@SuppressWarnings("unchecked")
	private final Collection<GeneralGoalCondition>[] generalConditions_ = new Collection[2];

	/** If this rule has spawned any mutant rules yet. */
	private Integer hasSpawned_ = null;

	/** The rule's internal count of value updates. */
	private int internalCount_ = 0;

	/** The rule's internal mean. */
	private double internalMean_ = 0;

	/** The rule's internal value for calculating standard deviation. */
	private double internalS_ = 0;

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

	/** The collection of ranges contained within the rule. */
	private SortedSet<RangeContext> rangeContexts_;

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
		this(cloneConds, ruleAction, null, null);
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
			RelationalPredicate action, RelationalRule parent,
			SpecialisationOperator specialisation) {
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
		findConstantsAndRanges();
		ruleHash_ = hashCode();
		abstractRule_ = new TreeSet<SpecialisationOperator>();
		if (specialisation != null) {
			abstractRule_.addAll(parent.abstractRule_);
			abstractRule_.add(specialisation);
		}
	}

	/**
	 * A constructor taking the bare minimum for a guided rule.
	 * 
	 * @param rule
	 *            The rule this rule represents
	 */
	public RelationalRule(String ruleString) {
		String[] split = ruleString.split(StateSpec.INFERS_ACTION);
		ruleConditions_ = splitConditions(split[0], true);
		if (split.length == 2)
			ruleAction_ = StateSpec.toRelationalPredicate(split[1].trim());
		slot_ = null;
		ancestryCount_ = 0;
		expandConditions();
		findConstantsAndRanges();
		abstractRule_ = new TreeSet<SpecialisationOperator>();
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
	}

	/**
	 * Finds the constants in the rule conditions.
	 */
	private void findConstantsAndRanges() {
		rangeContexts_ = new TreeSet<RangeContext>();
		constantCondition_ = new HashSet<SpecificGoalCondition>();
		generalConditions_[0] = new HashSet<GeneralGoalCondition>();
		generalConditions_[1] = new HashSet<GeneralGoalCondition>();

		Map<String, String> emptyReplacements = new HashMap<String, String>();
		for (RelationalPredicate cond : ruleConditions_) {
			// If the condition isn't a type predicate or test
			if (!StateSpec.getInstance().isTypePredicate(cond.getFactName())
					&& StateSpec.getInstance().isUsefulPredicate(
							cond.getFactName()) && !cond.isNegated()) {
				// If the condition doesn't contain variables - except modular
				// variables
				boolean isConstant = true;
				for (RelationalArgument argument : cond
						.getRelationalArguments()) {
					// If the arg isn't a constant or a goal term, the condition
					// isn't a constant condition.
					if (!argument.isConstant()) {
						isConstant = false;
						break;
					}
				}

				if (isConstant) {
					constantCondition_.add(new SpecificGoalCondition(cond));
				}

				// Adding generalised condition
				RelationalPredicate general = new RelationalPredicate(cond);
				general.replaceArguments(emptyReplacements, false, false);
				if (!general.isNegated())
					generalConditions_[0]
							.add(new GeneralGoalCondition(general));
				else
					generalConditions_[1]
							.add(new GeneralGoalCondition(general));

				// Checking for RangeContexts
				rangeContexts_.addAll(cond.getRangeContexts());
			}
		}
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
	 * @param cloneInternalMembers
	 *            If internal counters should also be cloned.
	 * @return A clone of this rule.
	 */
	public RelationalRule clone(boolean cloneInternalMembers) {
		Collection<RelationalPredicate> cloneConds = new ArrayList<RelationalPredicate>();
		for (RelationalPredicate cond : ruleConditions_)
			cloneConds.add(new RelationalPredicate(cond));
		RelationalRule clone = new RelationalRule(cloneConds, ruleAction_);
		if (!cloneInternalMembers)
			return clone;

		clone.abstractRule_ = new TreeSet<SpecialisationOperator>(abstractRule_);
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
		} else if (!ruleConditions_.containsAll(other.ruleConditions_))
			return false;
		else if (!other.ruleConditions_.containsAll(ruleConditions_))
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
		ruleHash_ = null;
		Integer newHash = hashCode();
		if (!newHash.equals(oldHash))
			statesSeen_ = 0;
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

	public int getAncestryCount() {
		return ancestryCount_;
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

	public Collection<SpecificGoalCondition> getSpecificSubGoals() {
		return constantCondition_;
	}

	public Collection<GeneralGoalCondition>[] getGeneralisedConditions() {
		return generalConditions_;
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

	public List<String> getParameters() {
		return parameters_;
	}

	public Collection<RelationalRule> getParentRules() {
		return mutantParents_;
	}

	public List<String> getQueryParameters() {
		return queryParams_;
	}

	public SortedSet<RangeContext> getRangeContexts() {
		return rangeContexts_;
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
	 * @param paramReplacements
	 *            The replacement map for goal terms.
	 * @return A cloned, but modularly grounded, rule.
	 */
	public RelationalRule groundModular(Map<String, String> paramReplacements) {
		if (paramReplacements == null || paramReplacements.isEmpty()) {
			return clone(false);
		}

		SortedSet<RelationalPredicate> groundConditions = new TreeSet<RelationalPredicate>(
				ruleConditions_.comparator());
		for (RelationalPredicate ruleCond : getConditions(true)) {
			RelationalPredicate groundCond = new RelationalPredicate(ruleCond);
			groundCond.replaceArguments(paramReplacements, true, false);
			groundConditions.add(groundCond);
		}
		RelationalPredicate groundAction = new RelationalPredicate(ruleAction_);
		groundAction.replaceArguments(paramReplacements, true, false);

		RelationalRule groundRule = new RelationalRule(groundConditions,
				groundAction, null, null);
		groundRule.expandConditions();
		groundRule.findConstantsAndRanges();

		return groundRule;
	}

	@Override
	public int hashCode() {
		if (ruleHash_ != null)
			return ruleHash_;

		// Calculate the rule hash.
		final int prime = 31;
		ruleHash_ = 1;
		// Rule action
		ruleHash_ = prime * ruleHash_
				+ ((ruleAction_ == null) ? 0 : ruleAction_.hashCode());
		int conditionResult = 0;
		// Rule conditions
		if (ruleConditions_ != null)
			for (RelationalPredicate condition : ruleConditions_)
				conditionResult += condition.hashCode();
		ruleHash_ = prime * ruleHash_ + conditionResult;
		return ruleHash_;
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
	 * @return True if the action changed as a result of this.
	 */
	public boolean setActionTerms(String[] terms) {
		boolean changed = !Arrays.equals(ruleAction_.getArguments(), terms);
		if (changed) {
			statesSeen_ = 0;
			ruleAction_ = new RelationalPredicate(ruleAction_, terms);
		}
		return changed;
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
	 * @return True if the newly set conditions are different from the old.
	 */
	public boolean setConditions(SortedSet<RelationalPredicate> conditions,
			boolean removeUnnecessaryFacts) {
		// If the conditions are the same, return true.
		if (conditions.equals(ruleConditions_)) {
			return false;
		}

		// Instead, introduce method (remove unnecessary conditions).
		if (removeUnnecessaryFacts)
			removeUnnecessaryFacts(conditions);

		// Reset the states seen, as the rule has changed.
		hasSpawned_ = null;
		ruleConditions_ = conditions;
		ruleUses_ = 0;
		statesSeen_ = 0;
		findConstantsAndRanges();
		return true;
	}

	/**
	 * Sets this rule as a mutant and adds the parent rule.
	 * 
	 * @param parent
	 *            The parent rule to add.
	 */
	private void setMutant(RelationalRule parent) {
		if (mutantParents_ == null) {
			mutantParents_ = new ArrayList<RelationalRule>();
			ancestryCount_ = parent.ancestryCount_ + 1;
		}
		mutant_ = true;
		mutantParents_.add(parent);
		if (parent.ancestryCount_ < ancestryCount_ - 1)
			ancestryCount_ = parent.ancestryCount_ + 1;
	}

	/**
	 * Sets the parameters of the the query parameters (given by the value) to
	 * the values.
	 * 
	 * @param parameterMap
	 *            The map of parameters (a -> ?G_0).
	 */
	@Override
	public void setParameters(BidiMap parameterMap) {
		if (parameterMap == null) {
			parameters_ = null;
			return;
		}

		// Creating new query params if necessary (shouldn't be)
		boolean hasQueryParams = queryParams_ != null;
		if (!hasQueryParams) {
			queryParams_ = new ArrayList<String>(parameterMap.size());
			for (int i = 0; i < parameterMap.size(); i++)
				queryParams_.add(RelationalArgument.createGoalTerm(i));
		}

		// Setting the parameters
		parameters_ = new ArrayList<String>(parameterMap.size());
		for (String queryParam : queryParams_)
			parameters_.add((String) parameterMap.getKey(queryParam));

		if (!hasQueryParams)
			findConstantsAndRanges();
	}

	/**
	 * Sets this rule's query parameters (replaceable variables). Generally
	 * these are just goal args.
	 * 
	 * @param queryParameters
	 *            The parameters to set.
	 * @param True
	 *            if the query parameters changed as a result of this call.
	 */
	public boolean setQueryParams(List<String> queryParameters) {
		boolean changed = false;
		if (queryParameters != null) {
			if (!queryParameters.equals(queryParams_))
				changed = true;
			queryParams_ = new ArrayList<String>(queryParameters);
		} else {
			queryParams_ = null;
			changed = true;
		}

		if (changed)
			statesSeen_ = 0;

		return changed;
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
	 * inequality and type definitions) format. Includes a replacement map to
	 * modify the goal parameters.
	 * 
	 * @param paramReplacements
	 *            A (possibly null) replacement map for replacing goal
	 *            variables.
	 * @return A nice, shortened version of the rule.
	 */
	public String toNiceString(Map<String, String> paramReplacements) {
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
							.toNiceString(paramReplacements) + " ");
			} else if (!stringFact.getFactName().equals("test")) {
				// If not a type or test, add the fact.
				niceString.append(stringFact.toNiceString(paramReplacements)
						+ " ");

				// Scan the arguments and extract the standard type preds
				for (int i = 0; i < stringFact.getArgTypes().length; i++) {
					// If the type isn't a number and isn't anonymous, add it to
					// the standards
					if (!stringFact.getArguments()[i].equals("?")
							&& !StateSpec
									.isNumberType(stringFact.getArgTypes()[i])) {
						RelationalPredicate type = new RelationalPredicate(
								StateSpec.getInstance().getPredicateByName(
										stringFact.getArgTypes()[i]),
								new String[] { stringFact.getArguments()[i] });
						standardType.add(type);
					}
				}
			}
		}
		niceString.append("=> " + ruleAction_.toNiceString(paramReplacements));

		return niceString.toString();
	}

	/**
	 * Outputs the rule in a simplified, but essentially equivalent (assuming
	 * inequality and type definitions) format.
	 * 
	 * @return A nice, shortened version of the rule.
	 */
	@Override
	public String toNiceString() {
		return toNiceString(null);
	}

	@Override
	public String toString() {
		return getStringConditions() + " => " + ruleAction_;
	}

	/**
	 * Prints this rule in abstract form of specialisations from the RLGG rule.
	 * 
	 * @return An abstract representation of the rule.
	 */
	public String toAbstractString() {
		if (abstractRule_.isEmpty())
			return getStringConditions() + " => " + ruleAction_;

		StringBuffer buffer = new StringBuffer("RLGG");
		for (SpecialisationOperator spec : abstractRule_) {
			buffer.append("+" + spec.toString());
		}
		return buffer.toString();
	}

	/**
	 * Updates the internal value of this rule, adjusting the rule mean and SD
	 * appropriately.
	 * 
	 * @param value
	 *            The value the rule attained as part of a policy.
	 */
	public void updateInternalValue(double value) {
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
	 * @param useConditionComparator
	 *            If the split should use a condition comparator.
	 * @return The facts of the string in segments.
	 */
	public static SortedSet<RelationalPredicate> splitConditions(
			String conditionString, boolean useConditionComparator) {
		SortedSet<RelationalPredicate> conds = null;
		if (useConditionComparator)
			conds = new TreeSet<RelationalPredicate>(
					ConditionComparator.getInstance());
		else
			conds = new TreeSet<RelationalPredicate>();
		Pattern p = Pattern.compile("\\(.+?\\)( |$)");
		Matcher m = p.matcher(conditionString);
		while (m.find()) {
			RelationalPredicate cond = StateSpec.toRelationalPredicate(m
					.group());
			conds.add(cond);
		}
		return conds;
	}

	@Override
	public boolean shouldRegenerate() {
		// Never regenerate.
		return false;
	}

	@Override
	public int size() {
		return 1;
	}
}