package relationalFramework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
	private List<String> ruleConditions_;

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
	}

	/**
	 * A constructor taking in the raw conditions and actions.
	 * 
	 * @param conditions
	 *            The conditions for the rule.
	 * @param action
	 *            The actions for the rule.
	 */
	public GuidedRule(Collection<String> conditions, String action) {
		ruleConditions_ = new ArrayList<String>(conditions);
		ruleAction_ = action;
		actionTerms_ = findTerms(ruleAction_);
		slot_ = null;
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
	 */
	public GuidedRule(String ruleString, boolean maxGeneral, boolean mutant,
			Slot slot) {
		this(ruleString);
		lgg_ = maxGeneral;
		mutant_ = mutant;
		slot_ = slot;
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
		for (String cond : ruleConditions_)
			buffer.append(cond + " ");
		return buffer.toString();
	}

	public String getAction() {
		return ruleAction_;
	}

	public List<String> getActionTerms() {
		return new ArrayList<String>(actionTerms_);
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
		if (statesSeen_ > SETTLED_RULE_STATES)
			return true;
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

	@Override
	public String toString() {
		return getStringConditions() + "=> " + ruleAction_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (hasSpawned_ ? 1231 : 1237);
		result = prime * result + (lgg_ ? 1231 : 1237);
		result = prime * result + (mutant_ ? 1231 : 1237);
		result = prime * result
				+ ((ruleAction_ == null) ? 0 : ruleAction_.hashCode());
		result = prime * result
				+ ((ruleConditions_ == null) ? 0 : ruleConditions_.hashCode());
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
		if (hasSpawned_ != other.hasSpawned_)
			return false;
		if (lgg_ != other.lgg_)
			return false;
		if (mutant_ != other.mutant_)
			return false;
		if (ruleAction_ == null) {
			if (other.ruleAction_ != null)
				return false;
		} else if (!ruleAction_.equals(other.ruleAction_))
			return false;
		if (ruleConditions_ == null) {
			if (other.ruleConditions_ != null)
				return false;
		} else if (!ruleConditions_.equals(other.ruleConditions_))
			return false;
		return true;
	}
}