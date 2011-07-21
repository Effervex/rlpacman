package relationalFramework.agentObservations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedSet;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import cerrla.Unification;
import cerrla.UnifiedFact;

import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import relationalFramework.RelationalPredicate;

/**
 * A class representing background knowledge assertions
 * 
 * @author Sam Sarjant
 */
public class BackgroundKnowledge implements Comparable<BackgroundKnowledge>,
		Serializable {
	private static final long serialVersionUID = 8755871369618060470L;

	/** The JESS compatible assertion string. */
	private String assertionString_;

	private boolean jessAssert_;

	/** The preconditions for the background knowledge. */
	private Collection<RelationalPredicate> preConds_;

	/** The postcondition (asserted value) for the background knowledge. */
	private RelationalPredicate postCondition_;

	/** If this background knowledge is equivalent or just inferred. */
	private boolean equivalentRule_;

	/**
	 * A constructor for the background knowledge.
	 * 
	 * @param assertion
	 *            The assertion in JESS format.
	 * @param jessAssert
	 *            If this rule is to be asserted in JESS or not.
	 */
	public BackgroundKnowledge(String assertion, boolean jessAssert) {
		assertionString_ = assertion;
		jessAssert_ = jessAssert;
		String splitter = StateSpec.INFERS_ACTION;
		equivalentRule_ = false;
		if (assertion.contains(StateSpec.EQUIVALENT_RULE)) {
			equivalentRule_ = true;
			splitter = StateSpec.EQUIVALENT_RULE;
		}
		String[] split = assertion.split(splitter);
		preConds_ = RelationalRule.splitConditions(split[0]);

		split[1] = split[1].trim();
		String assertStr = "(assert ";
		if (split[1].contains(assertStr))
			postCondition_ = StateSpec.toRelationalPredicate(split[1]
					.substring(assertStr.length(), split[1].length() - 1));
		else
			postCondition_ = StateSpec.toRelationalPredicate(split[1].trim());
	}

	/**
	 * Gets all conditions used in this background knowledge. That's both the
	 * conditions and the asserted information together.
	 * 
	 * @return A collection of all conditions shown in the background rule.
	 */
	private Collection<RelationalPredicate> getAllConditions() {
		Collection<RelationalPredicate> backgroundConditions = new ArrayList<RelationalPredicate>(
				preConds_);
		backgroundConditions.add(postCondition_);
		return backgroundConditions;
	}

	/**
	 * Gets the rule in conjugate form, such that the postcondition is negated.
	 * 
	 * @return A collection of all conditions with the postcondition negated.
	 */
	private Collection<RelationalPredicate> getConjugatedConditions() {
		Collection<RelationalPredicate> backgroundConditions = new ArrayList<RelationalPredicate>(
				preConds_);
		RelationalPredicate negated = new RelationalPredicate(postCondition_);
		negated.swapNegated();
		backgroundConditions.add(negated);

		return backgroundConditions;
	}

	/**
	 * Gets the preconditions of the background knowledge with optionally
	 * replaced arguments.
	 * 
	 * @param replacementTerms
	 *            The replacement map, or null.
	 * @return The preconditions of the background knowledge.
	 */
	@SuppressWarnings("unchecked")
	private Collection<RelationalPredicate> getPreConds(BidiMap replacementTerms) {
		Collection<RelationalPredicate> preConds = new ArrayList<RelationalPredicate>(
				preConds_.size());
		for (RelationalPredicate preCond : preConds_) {
			RelationalPredicate replacedFact = new RelationalPredicate(preCond);
			if (replacementTerms != null)
				replacedFact.replaceArguments(
						replacementTerms.inverseBidiMap(), true, false);
			if (!replacedFact.isFullyAnonymous())
				preConds.add(replacedFact);
			else
				return null;
		}

		return preConds;
	}

	/**
	 * Gets the post condition of the background knowledge with the terms of the
	 * condition swapped for the replacement terms.
	 * 
	 * @param replacementTerms
	 *            The replacement terms to swap the terms with.
	 */
	@SuppressWarnings("unchecked")
	private RelationalPredicate getPostCond(BidiMap replacementTerms) {
		RelationalPredicate replacedFact = new RelationalPredicate(
				postCondition_);
		if (replacementTerms != null)
			replacedFact.replaceArguments(replacementTerms.inverseBidiMap(),
					true, false);
		return replacedFact;
	}

	/**
	 * Gets the fact predicates relevant to this background knowledge. That's
	 * both sides if equivalence relation otherwise just the left side preds.
	 * 
	 * @return The relevant preds.
	 */
	public Collection<String> getRelevantPreds() {
		Collection<String> relevantPreds = new HashSet<String>();
		for (RelationalPredicate lhs : preConds_) {
			relevantPreds.add(lhs.getFactName());
		}
		if (equivalentRule_)
			relevantPreds.add(postCondition_.getFactName());
		return relevantPreds;
	}

	/**
	 * Simplifies a set of rule conditions using this background knowledge.
	 * 
	 * @param ruleConds
	 *            The rule conditions to simplify.
	 * @param testForIllegalRule
	 *            If the conditions are being tested for illegal conditions too.
	 * @return True if the conditions were simplified, false otherwise.
	 */
	@SuppressWarnings("unchecked")
	public boolean simplify(SortedSet<RelationalPredicate> ruleConds,
			boolean testForIllegalRule) {
		boolean changed = false;
		BidiMap replacementTerms = new DualHashBidiMap();
		Collection<RelationalPredicate> bckConditions = getAllConditions();
		int result = Unification.getInstance().unifyStates(bckConditions,
				ruleConds, replacementTerms);
		// If all conditions within a background rule are present, remove
		// the inferred condition
		if (result == 0) {
			RelationalPredicate cond = getPostCond(replacementTerms);
			if (ruleConds.remove(cond))
				changed = true;
		}

		// Simplify to the left for equivalent background knowledge
		if (equivalentRule_) {
			replacementTerms.clear();

			Collection<UnifiedFact> unifiedEquivs = Unification.getInstance()
					.unifyFact(getPostCond(null), ruleConds,
							new DualHashBidiMap(), replacementTerms,
							new String[0], true, false);
			for (UnifiedFact unified : unifiedEquivs) {
				replacementTerms = unified.getResultReplacements();
				// Remove any replacements which specialise anonymous terms
				replacementTerms.removeValue("?");
				RelationalPredicate unifiedFact = unified.getResultFact();

				// If there is a unification, attempt to remove the right side
				if (!unifiedEquivs.isEmpty()) {
					unifiedFact.replaceArguments(
							replacementTerms.inverseBidiMap(), true, false);

					// Swap variable
					Collection<RelationalPredicate> equivFacts = getPreConds(replacementTerms);
					if (equivFacts != null && ruleConds.remove(unifiedFact)) {
						// Add all equivalent facts.
						for (RelationalPredicate equivFact : equivFacts) {
							if (!ruleConds.contains(equivFact)) {
								ruleConds.add(equivFact);
								changed = true;
							}
						}
					}
				}
			}
		}

		if (testForIllegalRule) {
			replacementTerms.clear();
			result = Unification.getInstance().unifyStates(
					getConjugatedConditions(), ruleConds, replacementTerms);
			// If the rule is found to be illegal using the conjugated
			// conditions, remove the illegal condition
			if (result == 0) {
				RelationalPredicate cond = getPostCond(replacementTerms);
				cond.swapNegated();
				if (ruleConds.remove(cond))
					changed = true;
			}
		}

		return changed;
	}

	public boolean isEquivalence() {
		return equivalentRule_;
	}

	/**
	 * If this rule should be asserted in JESS. Otherwise, the rule is just used
	 * for clarifying illegal rules.
	 * 
	 * @return The state of this rule being asserted in JESS.
	 */
	public boolean assertInJess() {
		return jessAssert_;
	}

	@Override
	public String toString() {
		return assertionString_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (equivalentRule_ ? 1231 : 1237);
		result = prime * result
				+ ((postCondition_ == null) ? 0 : postCondition_.hashCode());
		result = prime * result
				+ ((preConds_ == null) ? 0 : preConds_.hashCode());
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
		BackgroundKnowledge other = (BackgroundKnowledge) obj;
		if (equivalentRule_ != other.equivalentRule_)
			return false;
		if (postCondition_ == null) {
			if (other.postCondition_ != null)
				return false;
		} else if (!postCondition_.equals(other.postCondition_))
			return false;
		if (preConds_ == null) {
			if (other.preConds_ != null)
				return false;
		} else if (!preConds_.equals(other.preConds_))
			return false;
		return true;
	}

	@Override
	public int compareTo(BackgroundKnowledge other) {
		if (other == null)
			return -1;

		int result = 0;

		// Compare by equivalence
		if (equivalentRule_ != other.equivalentRule_) {
			return (equivalentRule_) ? -1 : 1;
		}

		// Compare by pre conds
		Iterator<RelationalPredicate> thisIter = preConds_.iterator();
		Iterator<RelationalPredicate> otherIter = other.preConds_.iterator();
		// Iterate through the rules, until all matched, or a mismatch
		while (thisIter.hasNext() || otherIter.hasNext()) {
			// If either ruleset is smaller, return that as the smaller one.
			if (!thisIter.hasNext())
				return -1;
			if (!otherIter.hasNext())
				return 1;

			result = thisIter.next().compareTo(otherIter.next());
			if (result != 0)
				return result;
		}

		// Compare by postCond
		result = postCondition_.compareTo(other.postCondition_);
		if (result != 0)
			return result;

		return 0;
	}
}
