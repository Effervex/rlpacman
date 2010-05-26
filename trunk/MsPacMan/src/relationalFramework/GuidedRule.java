package relationalFramework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jess.ValueVector;

/**
 * A class that keeps track of the guided predicates that make up the rule
 * contained within.
 * 
 * @author Sam Sarjant
 */
/**
 * @author Sam Sarjant
 * 
 */
public class GuidedRule {
	/**
	 * The rule has to see 5 states without changing to be considered
	 * artificially LGG.
	 */
	private static final int SETTLED_RULE_STATES = 50;

	/** The conditions of the rule. */
	private List<String> ruleConditions_;

	/** The constant facts in the rule conditions, if any. Excludes type conds. */
	private List<String> constantConditions_;

	/** The guided predicate that defined the action. */
	private String ruleAction_;

	/** The terms present in the action. */
	private List<String> actionTerms_;

	/** The slot this rule was generated from. */
	private Slot slot_;

	/** If this slot is a mutation. */
	private boolean mutant_ = false;

	/** If this rule has spawned any mutant rules yet. */
	private boolean hasSpawned_ = false;

	/** If this rule is a least general generalisation. */
	private boolean lgg_ = false;

	/** The number of states seen by this rule. */
	private int statesSeen_ = 0;

	/** If this rule is assured to be without inequals preds. */
	private boolean withoutInequals_ = false;

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
		String[] split = ruleString.split("=>");
		ruleConditions_ = splitConditions(split[0]);
		ruleAction_ = split[1].trim();
		actionTerms_ = findTerms(ruleAction_);
		slot_ = null;
		expandConditions();
		constantConditions_ = findConstants();
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
	public GuidedRule(Collection<String> conditions, String action,
			boolean mutant) {
		ruleConditions_ = new ArrayList<String>(conditions);
		ruleAction_ = action;
		actionTerms_ = findTerms(ruleAction_);
		mutant_ = mutant;
		slot_ = null;
		constantConditions_ = findConstants();
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
		lgg_ = maxGeneral;
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
		constantConditions_ = findConstants();
	}

	/**
	 * Sets the parameters for a cloned GuidedRule.
	 * 
	 * @param parameters
	 *            The parameters to set.
	 * @return A new rule, the same as this, but with parameters set.
	 */
	public void setParameters(List<String> parameters) {
		parameters_ = parameters;
	}

	/**
	 * Sets temporary parameter replacements for any parameterisable terms
	 * within. The temp parameters don't affect the rule itself, just the
	 * evaluation, so rule updating will not be affected.
	 * 
	 * @param arguments
	 *            The arguments being set.
	 */
	public void setParameters(ValueVector arguments) {
		if (arguments == null) {
			parameters_ = null;
			return;
		}

		try {
			if (parameters_ == null)
				parameters_ = new ArrayList<String>();
			else
				parameters_.clear();

			for (int i = 0; i < arguments.size(); i++) {
				parameters_.add(arguments.get(i).stringValue(null));
			}
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
	 * Finds the terms in the action.
	 * 
	 * @param ruleAction
	 *            The action.
	 * @return The terms in the action.
	 */
	private List<String> findTerms(String ruleAction) {
		List<String> terms = new ArrayList<String>();
		String[] split = StateSpec.splitFact(ruleAction);
		for (int i = 1; i < split.length; i++)
			terms.add(split[i]);
		return terms;
	}

	/**
	 * Splits a conditions string into individual facts.
	 * 
	 * @param conditionString
	 *            The condition string to be split.
	 * @return The facts of the string in segments.
	 */
	private List<String> splitConditions(String conditionString) {
		List<String> conds = new ArrayList<String>();
		Pattern p = Pattern.compile("\\(.+?\\)( |$)");
		Matcher m = p.matcher(conditionString);
		while (m.find())
			conds.add(m.group().trim());
		return conds;
	}

	/**
	 * Remove inequality preds
	 */
	private void removeInequals() {
		if (!withoutInequals_) {
			for (Iterator<String> condIter = ruleConditions_.iterator(); condIter
					.hasNext();) {
				String cond = condIter.next();
				if (cond.matches("\\(test \\(<> .+?\\)\\)")) {
					condIter.remove();
				}
			}
			withoutInequals_ = true;
		}
	}

	/**
	 * Expands the conditions by parsing the base definitions of the condition
	 * into a fully type and inequal'ed condition.
	 */
	public void expandConditions() {
		ruleConditions_ = splitConditions(StateSpec.getInstance().parseRule(
				toString()).split(StateSpec.INFERS_ACTION)[0]);
		withoutInequals_ = false;
	}

	/**
	 * Finds the constants in the rule conditions.
	 */
	private List<String> findConstants() {
		List<String> constants = new ArrayList<String>();
		for (String cond : ruleConditions_) {
			String[] condSplit = StateSpec.splitFact(cond);
			// If the condition isn't a type predicate or test
			if (!StateSpec.getInstance().isTypePredicate(condSplit[0])
					&& StateSpec.getInstance().isUsefulPredicate(condSplit[0])) {
				// If the condition doesn't contain variables - except modular
				// variables
				boolean isConstant = true;
				for (int i = 1; i < condSplit.length; i++) {
					// If we're looking at a variable
					if (condSplit[i].contains("?")) {
						// It may be a parameter, else return false.
						if ((queryParams_ == null)
								|| (!queryParams_.contains(condSplit[i]))) {
							isConstant = false;
							break;
						}
					}
				}

				if (isConstant)
					constants.add(cond);
			}
		}

		return constants;
	}

	/**
	 * Checks if inequals is present (i.e. rule is fully expanded.)
	 */
	public void checkInequals() {
		if (withoutInequals_)
			expandConditions();
	}

	/**
	 * Determines the LGG status of this rule. Also has the option of removing
	 * unnecessary rules as it runs through the rules.
	 * 
	 * @param removeUnnecessaryFacts
	 *            If, during the sweep through the rules, unnecessary rules
	 *            should be removed.
	 * @return 1 if the rule is minimal, 0 if not minimal, and -1 if invalid
	 *         (too few conditions).
	 */
	private int determineLGGStatus(List<String> conditions,
			boolean removeUnnecessaryFacts) {
		if (conditions.isEmpty()) {
			System.err.println("Conditions have been over-shrunk: "
					+ conditions + ", " + ruleAction_);
			return -1;
		}

		Set<String> usedTerms = new HashSet<String>();

		// Run through the conditions, ensuring each one has at least one unique
		// term seen in the action.
		boolean notLGG = false;
		for (Iterator<String> iter = conditions.iterator(); iter.hasNext();) {
			String condition = iter.next();

			boolean contains = false;

			// Check if any of the terms are in the condition
			boolean containsAny = false;
			for (String term : actionTerms_) {
				// If the condition contains a term
				if (condition.contains(term)) {
					// If the term hasn't already been used, keep it
					if (!usedTerms.contains(term)) {
						usedTerms.add(term);
						contains = true;
					}
					containsAny = true;
				}
			}

			// Removing unnecessary terms
			if (!containsAny && removeUnnecessaryFacts) {
				iter.remove();
			} else {
				// If no term is in the condition, return false
				if (!contains) {
					lgg_ = false;
					notLGG = true;
				}
			}
		}

		// If terms occur multiple times, the rule isn't LGG
		if (notLGG)
			return 0;

		// If there are terms remaining, the condition is invalid
		if (usedTerms.size() < actionTerms_.size())
			return -1;

		lgg_ = true;
		return 1;
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

	public Slot getSlot() {
		return slot_;
	}

	/**
	 * Gets the conditions, with or without inequality predicates.
	 * 
	 * @param withoutInequals
	 *            Whether to exclude inequal preds or not.
	 * @return The conditions, with or without inequal predicates.
	 */
	public List<String> getConditions(boolean withoutInequals) {
		if (withoutInequals)
			removeInequals();
		return new ArrayList<String>(ruleConditions_);
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
	public boolean setConditions(List<String> conditions,
			boolean removeUnnecessaryFacts) {
		// If the conditions are the same, return true.
		if (conditions.equals(ruleConditions_)) {
			return true;
		}

		int result = determineLGGStatus(conditions, removeUnnecessaryFacts);
		// If the rule is invalid, return false.
		if (result == -1) {
			return false;
		}

		// Reset the states seen, as the rule has changed.
		hasSpawned_ = false;
		statesSeen_ = 0;
		ruleConditions_ = conditions;
		return true;
	}

	public String getStringConditions() {
		StringBuffer buffer = new StringBuffer();
		for (String cond : ruleConditions_) {
			buffer.append(cond + " ");
		}

		String result = buffer.toString();
		return result;
	}

	public List<String> getConstantConditions() {
		return constantConditions_;
	}

	public String getAction() {
		return ruleAction_;
	}

	public List<String> getActionTerms() {
		return new ArrayList<String>(actionTerms_);
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
	public void setAsLoadedModuleRule() {
		isLoadedModule_ = true;
	}

	/**
	 * Gets the action predicate.
	 * 
	 * @return The action predicate for the action.
	 */
	public String getActionPredicate() {
		return StateSpec.splitFact(ruleAction_)[0];
	}

	/**
	 * Sets the new action terms, and modifies the action if necessary.
	 * 
	 * @param terms
	 *            The new action terms.
	 */
	public void setActionTerms(List<String> terms) {
		if (!terms.equals(actionTerms_)) {
			actionTerms_ = terms;
			String[] modified = StateSpec.splitFact(ruleAction_);
			for (int i = 1; i < modified.length; i++) {
				modified[i] = actionTerms_.get(i - 1);
			}
			ruleAction_ = StateSpec.reformFact(modified);
		}
	}

	public void setSlot(Slot slot) {
		slot_ = slot;
	}

	public boolean isLGG() {
		// If the rule has seen enough states, assume it is LGG
		if (statesSeen_ > SETTLED_RULE_STATES) {
			lgg_ = true;
			return true;
		}
		return lgg_;
	}

	public boolean isMutant() {
		return mutant_;
	}

	public void setSpawned(boolean spawned) {
		hasSpawned_ = spawned;
	}

	public boolean hasSpawned() {
		return hasSpawned_;
	}

	/**
	 * Increments the state seen counter, if necessary.
	 */
	public void incrementStatesCovered() {
		if (statesSeen_ <= SETTLED_RULE_STATES)
			statesSeen_++;
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
		GuidedRule clone = new GuidedRule(ruleConditions_, ruleAction_, mutant_);
		clone.hasSpawned_ = hasSpawned_;
		clone.lgg_ = lgg_;
		clone.withoutInequals_ = withoutInequals_;
		clone.isLoadedModule_ = isLoadedModule_;
		clone.constantConditions_ = new ArrayList<String>(constantConditions_);
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
		result = prime * result
				+ ((ruleConditions_ == null) ? 0 : ruleConditions_.hashCode());
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
		else if (!ruleConditions_.containsAll(other.ruleConditions_))
			return false;
		else if (!other.ruleConditions_.containsAll(ruleConditions_))
			return false;
		if (queryParams_ == null) {
			if (other.queryParams_ != null)
				return false;
		} else if (!queryParams_.equals(other.queryParams_))
			return false;
		return true;
	}
}