package relationalFramework;

import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.RangeContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.BidiMap;

import cerrla.LocalCrossEntropyDistribution;
import cerrla.Slot;
import cerrla.modular.GeneralGoalCondition;
import cerrla.modular.PolicyItem;
import cerrla.modular.SpecificGoalCondition;

import util.ConditionComparator;
import util.MultiMap;

/**
 * A class that keeps track of the guided predicates that make up the rule
 * contained within.
 * 
 * @author Sam Sarjant
 */
public class RelationalRule implements Serializable,
		Comparable<RelationalRule>, PolicyItem, RelationalQuery {
	private static final long serialVersionUID = -7517726681678896438L;

	/**
	 * The rule has to see 5 states without changing to be considered
	 * artificially LGG.
	 */
	private static final int SETTLED_RULE_STATES = 50;

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
	private List<RelationalArgument> parameters_;

	/** The query parameters associated with this rule. */
	private List<RelationalArgument> queryParams_;

	/** The collection of ranges contained within the rule. */
	private SortedSet<RangeContext> rangeContexts_;

	/** The guided predicate that defined the action. */
	private RelationalPredicate ruleAction_;

	/** The conditions of the rule. */
	private Collection<RelationalPredicate> rawConditions_;

	/** The conditions of the rule. */
	private List<RelationalPredicate> simplifiedConditions_;

	/** The hash value of the rule. */
	private Integer ruleHash_ = null;

	/** The number of times this rule has been used in a policy. */
	private int ruleUses_ = 0;

	/** The slot this rule was generated from. */
	private Slot slot_;

	/** The number of states seen by this rule. */
	private int statesSeen_ = 0;

	/** A map noting the types of unbound variables in this rule. */
	private MultiMap<RelationalArgument, String> unboundTypeMap_;

	/** A link to the containing distribution for simplification tasks. */
	private LocalCrossEntropyDistribution lced_;

	/**
	 * A private constructor used only for the clone.
	 * 
	 * @param conditions
	 *            The conditions for the rule.
	 * @param action
	 *            The actions for the rule.
	 * @param agentObservations
	 *            The agent observations to simplify the rule's conditions.
	 */
	private RelationalRule(Collection<RelationalPredicate> cloneConds,
			RelationalPredicate ruleAction,
			LocalCrossEntropyDistribution agentObservations) {
		this(cloneConds, ruleAction, null, agentObservations);
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
	 * @param lced
	 *            The local cross-entropy distribution containing this rule.
	 */
	public RelationalRule(Collection<RelationalPredicate> conditions,
			RelationalPredicate action, RelationalRule parent,
			LocalCrossEntropyDistribution lced) {
		rawConditions_ = new HashSet<RelationalPredicate>(conditions);
		if (action != null)
			ruleAction_ = new RelationalPredicate(action);
		if (parent != null)
			setMutant(parent);
		lced_ = lced;
		slot_ = null;
		expandConditions(null);
		findConstantsAndRanges();
		ruleHash_ = hashCode();
	}

	/**
	 * A constructor taking the bare minimum for a guided rule.
	 * 
	 * @param rule
	 *            The rule this rule represents
	 */
	public RelationalRule(String ruleString) {
		this(ruleString, null);
	}

	/**
	 * A constructor taking a rule string and the rule generator (for
	 * simplification).
	 * 
	 * @param ruleString
	 *            The rule string.
	 * @param lced
	 *            The local rule generator.
	 */
	public RelationalRule(String ruleString, LocalCrossEntropyDistribution lced) {
		lced_ = lced;
		String[] split = ruleString.split(StateSpec.INFERS_ACTION);
		if (split.length == 2)
			ruleAction_ = StateSpec.toRelationalPredicate(split[1].trim());
		rawConditions_ = splitConditions(split[0], ruleAction_);
		slot_ = null;
		ancestryCount_ = 0;
		expandConditions(null);
		findConstantsAndRanges();
	}

	/**
	 * A relational rule constructor for specialised rules that add conditions.
	 * 
	 * @param baseRule
	 *            The base rule to specialise.
	 * @param condition
	 *            The condition being added.
	 */
	public RelationalRule(RelationalRule baseRule, RelationalPredicate condition) {
		lced_ = baseRule.lced_;
		rawConditions_ = new HashSet<RelationalPredicate>(
				baseRule.rawConditions_.size() + 1);
		for (RelationalPredicate cond : baseRule.rawConditions_) {
			rawConditions_.add(new RelationalPredicate(cond));
		}
		ruleAction_ = new RelationalPredicate(baseRule.getAction());
		slot_ = null;
		setMutant(baseRule);

		expandConditions(condition);
		findConstantsAndRanges();
		ruleHash_ = hashCode();
	}

	/**
	 * Creates the inequals tests from the terms given. Note anonymous terms are
	 * special in that they aren't inequal to one-another.
	 * 
	 * @param ruleConditions
	 *            The current conditions for the rule.
	 * @param variableTerms
	 *            The variable terms in the rule.
	 * @param constantTerms
	 *            The constant terms in the rule.
	 */
	private void addInequalityTests(List<RelationalPredicate> ruleConditions,
			Collection<RelationalArgument> constantTerms) {
		List<RelationalArgument> variableTerms = new ArrayList<RelationalArgument>();
		int lastVars = 0;
		int ruleConditionIndex = 0;
		for (Object condObj : ruleConditions.toArray()) {
			RelationalPredicate cond = (RelationalPredicate) condObj;
			RelationalArgument[] condArgs = cond.getRelationalArguments();
			String[] argTypes = cond.getArgTypes();
			for (int i = 0; i < condArgs.length; i++) {
				// Is an unseen variable (but not a numerical variable)
				if (condArgs[i].isVariable()
						&& !StateSpec.isNumberType(argTypes[i])
						&& !variableTerms.contains(condArgs[i])
						&& !cond.isNegated())
					variableTerms.add(condArgs[i]);
			}

			int numTerms = variableTerms.size();
			for (int i = numTerms; i > lastVars
					&& numTerms + constantTerms.size() >= 2; i--) {
				StringBuffer buffer = new StringBuffer("(<>");
				// (Potentially) Create an inequality test
				RelationalArgument varTermA = null;
				boolean isValid = false;
				for (ListIterator<RelationalArgument> varIter = variableTerms
						.listIterator(i); varIter.hasPrevious();) {
					RelationalArgument varTerm = varIter.previous();
					if (varTermA == null) {
						// First term
						varTermA = varTerm;
						buffer.append(" " + varTermA);
					} else if (!varTermA.isNonActionVar()
							|| !varTerm.isNonActionVar()) {
						isValid = true;
						buffer.append(" " + varTerm);
					}
				}

				// Adding constant terms
				for (RelationalArgument constant : constantTerms) {
					isValid = true;
					buffer.append(" " + constant);
				}

				if (isValid) {
					buffer.append(")");
					RelationalPredicate inequality = new RelationalPredicate(
							StateSpec.TEST_DEFINITION,
							new String[] { buffer.toString() });
					ruleConditionIndex++;
					ruleConditions.add(ruleConditionIndex, inequality);
				}
			}
			lastVars = numTerms;
			ruleConditionIndex++;
		}
	}

	/**
	 * Finds the constants in the rule conditions.
	 */
	private void findConstantsAndRanges() {
		rangeContexts_ = new TreeSet<RangeContext>();
		constantCondition_ = new ArrayList<SpecificGoalCondition>();
		generalConditions_[0] = new HashSet<GeneralGoalCondition>();
		generalConditions_[1] = new HashSet<GeneralGoalCondition>();

		Map<String, String> emptyReplacements = new HashMap<String, String>();
		for (RelationalPredicate cond : simplifiedConditions_) {
			// If the condition isn't a type predicate or test
			if (StateSpec.getInstance().isNotInternalPredicate(
					cond.getFactName())) {
				if (!StateSpec.getInstance()
						.isTypePredicate(cond.getFactName())
						&& !cond.isNegated()) {
					// If the condition doesn't contain variables - except
					// modular variables
					boolean isConstant = true;
					for (RelationalArgument argument : cond
							.getRelationalArguments()) {
						// If the arg isn't a constant or a goal term, the
						// condition isn't a constant condition.
						if (!argument.isConstant()) {
							isConstant = false;
							break;
						}
					}

					if (isConstant) {
						constantCondition_.add(new SpecificGoalCondition(cond));
					}

					// Checking for RangeContexts
					rangeContexts_.addAll(cond.getRangeContexts());
				}

				// Adding generalised condition
				RelationalPredicate general = new RelationalPredicate(cond);
				general.replaceArguments(emptyReplacements, false, false);
				if (!cond.isNegated())
					generalConditions_[0]
							.add(new GeneralGoalCondition(general));
				else {
					generalConditions_[1]
							.add(new GeneralGoalCondition(general));
				}
			}
		}
	}

	/**
	 * Initialises the variable normalisation map with the action terms, so the
	 * fundamental structure of the rule doesn't change.
	 * 
	 * @return The normalisation map, containing any variable action terms.
	 */
	private Map<RelationalArgument, RelationalArgument> initialiseNormalisationMap() {
		if (ruleAction_ == null)
			return null;

		Map<RelationalArgument, RelationalArgument> normalisationMap = new HashMap<RelationalArgument, RelationalArgument>();
		RelationalArgument[] actionArgs = ruleAction_.getRelationalArguments();
		for (int i = 0; i < actionArgs.length; i++) {
			if (actionArgs[i].isVariable()) {
				if (actionArgs[i].isGoalVariable()
						|| actionArgs[i].isRangeVariable())
					normalisationMap.put(actionArgs[i], actionArgs[i]);
				else if (!normalisationMap.containsKey(actionArgs[i]))
					normalisationMap.put(actionArgs[i],
							RelationalArgument.createVariableTermArg(i));
			}
		}
		return normalisationMap;
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
	 * @param lced
	 *            The distribution the clone rule is contained within.
	 * @return A clone of this rule.
	 */
	public RelationalRule clone(boolean cloneInternalMembers,
			LocalCrossEntropyDistribution lced) {
		Collection<RelationalPredicate> cloneConds = new ArrayList<RelationalPredicate>();
		for (RelationalPredicate cond : rawConditions_)
			cloneConds.add(new RelationalPredicate(cond));
		RelationalRule clone = new RelationalRule(cloneConds, ruleAction_, lced);
		if (!cloneInternalMembers)
			return clone;

		clone.hasSpawned_ = hasSpawned_;
		clone.statesSeen_ = statesSeen_;
		clone.mutant_ = mutant_;
		if (mutantParents_ != null)
			clone.mutantParents_ = new ArrayList<RelationalRule>(mutantParents_);

		if (queryParams_ != null)
			clone.queryParams_ = new ArrayList<RelationalArgument>(queryParams_);
		if (parameters_ != null)
			clone.parameters_ = new ArrayList<RelationalArgument>(parameters_);
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
		if (simplifiedConditions_ == null) {
			if (other.simplifiedConditions_ != null)
				return false;
		} else if (!simplifiedConditions_
				.containsAll(other.simplifiedConditions_))
			return false;
		else if (!other.simplifiedConditions_
				.containsAll(simplifiedConditions_))
			return false;
		return true;
	}

	/**
	 * Expands the conditions by parsing the base definitions of the condition
	 * into a fully type and inequal'ed condition. Also normalises the
	 * variables.
	 * 
	 * @param condition
	 *            An optional added condition for the raw conditions.
	 */
	public void expandConditions(RelationalPredicate condition) {
//		condition = preProcessRawConds(condition);

		unboundTypeMap_ = null;
		simplifiedConditions_ = new ArrayList<RelationalPredicate>();
		Collection<RelationalPredicate> simplified = null;
		if (lced_ != null && ruleAction_ != null)
			simplified = lced_.getLocalAgentObservations().simplifyRule(
					rawConditions_, condition, ruleAction_, true);
		else
			simplified = new HashSet<RelationalPredicate>(rawConditions_);
		if (simplified == null)
			return;
		else if (condition != null)
			rawConditions_.add(condition);

		Set<RelationalArgument> constantTerms = new TreeSet<RelationalArgument>();

		Map<RelationalArgument, RelationalArgument> normalisationMap = initialiseNormalisationMap();
		int normalisedIndex[] = new int[2]; // [non-action, action]
		normalisedIndex[1] = (ruleAction_ != null) ? ruleAction_.factTypes_.length
				: 0;

		// Unbound collections
		MultiMap<RelationalArgument, Integer> indexedUnbounds = MultiMap
				.createListMultiMap();

		// Scan each condition
		int index = 0;
		for (RelationalPredicate simpCond : simplified) {
			if (StateSpec.getInstance().isNotInternalPredicate(
					simpCond.getFactName())) {
				// Adding the terms
				RelationalArgument[] arguments = simpCond
						.getRelationalArguments();
				for (int i = 0; i < arguments.length; i++) {

					if (StateSpec.isNumberType(simpCond.getArgTypes()[i])
							&& arguments[i].isRangeVariable()) {
						// Numerical argument - resolve range context
						RangeContext rc = new RangeContext(i, simpCond,
								ruleAction_);
						arguments[i] = new RelationalArgument(arguments[i], rc);
						simpCond.getRangeContexts().add(rc);
					} else {
						// Adding variable terms
						if (arguments[i].isVariable()
								&& normalisationMap != null) {
							// Normalise the variable terms
							if (!normalisationMap.containsKey(arguments[i])) {
								if (arguments[i].isGoalVariable())
									// Maintain goal conditions.
									normalisationMap.put(arguments[i],
											arguments[i]);
								else if (arguments[i].isUnboundVariable()) {
									// Note the unbound variable
									normalisationMap
											.put(arguments[i],
													RelationalArgument
															.createUnboundVariable(normalisedIndex[0]++));
								} else if (arguments[i].isBoundVariable()) {
									// Normalise bound variables.
									normalisationMap
											.put(arguments[i],
													RelationalArgument
															.createBoundVariable(normalisedIndex[0]++));
								} else {
									normalisationMap
											.put(arguments[i],
													RelationalArgument
															.createVariableTermArg(normalisedIndex[1]++));
								}
							}
							arguments[i] = normalisationMap.get(arguments[i]);

							// Note the free variables.
							if (arguments[i].isFreeVariable()
									&& !arguments[i].isAnonymous())
								indexedUnbounds.put(arguments[i], index);
						} else if (arguments[i].isAnonymous()) {
							// If the argument is anonymous, convert it to an
							// unbound variable.
							arguments[i] = RelationalArgument
									.createUnboundVariable(normalisedIndex[0]++);
						} else if (arguments[i].isConstant())
							// Adding constant terms
							constantTerms.add(arguments[i]);
					}
				}
				simpCond = new RelationalPredicate(simpCond, arguments,
						simpCond.isNegated());

				simplifiedConditions_.add(simpCond);
				index++;
			} else if (simpCond.getFactName().equals(StateSpec.GOALARGS_PRED)) {
				simplifiedConditions_.add(simpCond);
				index++;
			}
		}

		// Run through and rename bound unbound variables
		for (RelationalArgument arg : indexedUnbounds.keySet()) {
			if (indexedUnbounds.get(arg).size() == 1) {
				RelationalPredicate singleCond = simplifiedConditions_
						.get(indexedUnbounds.get(arg).iterator().next());
				// If the condition is negated, swap the unbound variable
				// for an anonymous variable
				if (arg.isBoundVariable()) {
					// If a variable was bound, but simplified to a single, need
					// to rerun simplification again.
					singleCond.replaceArguments(arg, RelationalArgument
							.createUnboundVariable(normalisedIndex[0]++), true);
				}
			}
			if (indexedUnbounds.get(arg).size() > 1 && arg.isUnboundVariable()) {
				RelationalArgument boundVariable = RelationalArgument
						.createBoundVariable(normalisedIndex[0]++);
				// Swap the unbound args for bound args
				for (Integer condIndex : indexedUnbounds.get(arg))
					simplifiedConditions_.get(condIndex).replaceArguments(arg,
							boundVariable, true);
			}
		}

		if (ruleAction_ != null) {
			ruleAction_.replaceArguments(normalisationMap, true, true);
		}

		Collections.sort(simplifiedConditions_,
				ConditionComparator.getInstance());
		// Adding the inequality predicates
		addInequalityTests(simplifiedConditions_, constantTerms);

		Integer oldHash = ruleHash_;
		ruleHash_ = null;
		Integer newHash = hashCode();
		if (!newHash.equals(oldHash))
			statesSeen_ = 0;
	}

	/**
	 * Swaps all occurrences of anonymous variables in the raw conditions (and
	 * the added condition, if applicable) for a unique unbound variable. This
	 * also binds any necessary raw conds to non action variables in the
	 * optional added condition.
	 * 
	 * @param condition
	 *            An optional condition to swap anon terms for.
	 */
	private RelationalPredicate preProcessRawConds(RelationalPredicate condition) {
		// Run through each raw condition, swapping anonymous for unbound.
		int unboundIndex = 0;
		for (RelationalPredicate rawCond : rawConditions_) {
			unboundIndex = rawCond.replaceAnonymousWithUnbound(unboundIndex);
		}
		if (condition != null)
			condition.replaceAnonymousWithUnbound(unboundIndex);

		return condition;
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

	public RelationalArgument[] getActionTerms() {
		return ruleAction_.getRelationalArguments();
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
	public List<RelationalPredicate> getSimplifiedConditions(
			boolean withoutInequals) {
		if (withoutInequals) {
			List<RelationalPredicate> conds = new ArrayList<RelationalPredicate>(
					simplifiedConditions_.size());

			for (RelationalPredicate cond : simplifiedConditions_) {
				if (!cond.getFactName().equals("test"))
					conds.add(cond);
			}
			return conds;
		}
		return new ArrayList<RelationalPredicate>(simplifiedConditions_);
	}

	public Collection<RelationalPredicate> getRawConditions(
			boolean withoutInequals) {
		if (withoutInequals) {
			HashSet<RelationalPredicate> conds = new HashSet<RelationalPredicate>(
					rawConditions_.size());

			for (RelationalPredicate cond : rawConditions_) {
				if (!cond.getFactName().equals("test"))
					conds.add(cond);
			}
			return conds;
		}
		return new HashSet<RelationalPredicate>(rawConditions_);
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

	public List<RelationalArgument> getParameters() {
		return parameters_;
	}

	public Collection<RelationalRule> getParentRules() {
		return mutantParents_;
	}

	public List<RelationalArgument> getQueryParameters() {
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
	public RelationalArgument getReplacementParameter(
			RelationalArgument queryParam) {
		if (parameters_ == null)
			return queryParam;

		return parameters_.get(queryParams_.indexOf(queryParam));
	}

	public Slot getSlot() {
		return slot_;
	}

	public Collection<SpecificGoalCondition> getSpecificSubGoals() {
		return constantCondition_;
	}

	public String getStringConditions() {
		return StateSpec.conditionsToString(simplifiedConditions_);
	}

	/**
	 * Gets the unbound type conditions present in the rule. These allow
	 * guidance towards anonymous variable specialisation.
	 * 
	 * @return A map of unbound variables to type predicate names.
	 */
	public MultiMap<RelationalArgument, String> getUnboundTypeConditions() {
		if (unboundTypeMap_ != null)
			return unboundTypeMap_;

		// Initialise the type map.
		unboundTypeMap_ = MultiMap.createSortedSetMultiMap();
		for (RelationalPredicate condition : rawConditions_) {
			// Only types of unbound variables
			RelationalArgument[] args = condition.getRelationalArguments();
			String[] types = condition.getArgTypes();
			for (int i = 0; i < args.length; i++) {
				if (args[i].isNonActionVar())
					unboundTypeMap_.put(args[i], types[i]);
			}
		}
		return unboundTypeMap_;
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
			return clone(false, lced_);
		}

		List<RelationalPredicate> groundConditions = new ArrayList<RelationalPredicate>(
				rawConditions_.size());
		for (RelationalPredicate ruleCond : getRawConditions(true)) {
			RelationalPredicate groundCond = new RelationalPredicate(ruleCond);
			groundCond.replaceArguments(paramReplacements, true, false);
			groundConditions.add(groundCond);
		}
		RelationalPredicate groundAction = new RelationalPredicate(ruleAction_);
		groundAction.replaceArguments(paramReplacements, true, false);

		RelationalRule groundRule = new RelationalRule(groundConditions,
				groundAction, null, lced_);
		groundRule.expandConditions(null);
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
		if (simplifiedConditions_ != null)
			for (RelationalPredicate condition : simplifiedConditions_)
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
	public boolean setActionTerms(RelationalArgument[] terms) {
		boolean changed = !Arrays.equals(ruleAction_.getRelationalArguments(),
				terms);
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
	 * @return True if the newly set conditions are different from the old.
	 */
	public boolean setConditions(Collection<RelationalPredicate> conditions) {
		// If the conditions are the same, return true.
		if (conditions.equals(rawConditions_)) {
			return false;
		}

		// Reset the states seen, as the rule has changed.
		hasSpawned_ = null;
		rawConditions_ = new HashSet<RelationalPredicate>(conditions);
		expandConditions(null);
		findConstantsAndRanges();
		return true;
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
		if (parameterMap == null || parameterMap.isEmpty()) {
			parameters_ = null;
			return;
		}

		// Creating new query params if necessary (shouldn't be)
		boolean hasQueryParams = queryParams_ != null;
		if (!hasQueryParams) {
			queryParams_ = new ArrayList<RelationalArgument>(
					parameterMap.size());
			for (int i = 0; i < parameterMap.size(); i++)
				queryParams_.add(RelationalArgument.createGoalTerm(i));
		}

		// Setting the parameters
		parameters_ = new ArrayList<RelationalArgument>(parameterMap.size());
		for (RelationalArgument queryParam : queryParams_)
			parameters_.add((RelationalArgument) parameterMap
					.getKey(queryParam));

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
	public boolean setQueryParams(List<RelationalArgument> queryParameters) {
		boolean changed = false;
		if (queryParameters != null) {
			if (!queryParameters.equals(queryParams_))
				changed = true;
			queryParams_ = new ArrayList<RelationalArgument>(queryParameters);
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

	@Override
	public boolean shouldRegenerate() {
		// Never regenerate.
		return false;
	}

	@Override
	public int size() {
		return 1;
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
	public String toNiceString(
			Map<RelationalArgument, RelationalArgument> paramReplacements) {
		StringBuffer niceString = new StringBuffer();

		// Run through each condition, adding regular conditions and
		// non-standard type predicates
		Collection<RelationalPredicate> standardType = new HashSet<RelationalPredicate>();
		for (RelationalPredicate stringFact : simplifiedConditions_) {

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
	 * @return The facts of the string in segments.
	 */
	public static List<RelationalPredicate> splitConditions(
			String conditionString, RelationalPredicate action) {
		List<RelationalPredicate> conds = null;
		conds = new ArrayList<RelationalPredicate>();
		Pattern p = Pattern.compile("\\(.+?\\)(?=( (\\(|$))|$)");
		Matcher m = p.matcher(conditionString);
		while (m.find()) {
			RelationalPredicate cond = StateSpec.toRelationalPredicate(m
					.group());
			if (cond.isNumerical() && action != null)
				cond.configureRangeContexts(action);
			conds.add(cond);
		}
		return conds;
	}

	public boolean isLegal() {
		return !simplifiedConditions_.isEmpty();
	}

	@Override
	public void getRuleQuery() {
		// TODO Auto-generated method stub
		
	}
}