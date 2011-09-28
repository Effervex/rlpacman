package relationalFramework.agentObservations;

import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import cerrla.Unification;

/**
 * A class representing background knowledge assertions
 * 
 * @author Sam Sarjant
 */
public class BackgroundKnowledge implements Comparable<BackgroundKnowledge>,
		Serializable {
	private static final long serialVersionUID = 8755871369618060470L;

	/** Precendence constants. */
	private final int LEFT_SIDE = -1;
	private final int RIGHT_SIDE = 1;

	/** The JESS compatible assertion string. */
	private String assertionString_;

	private boolean jessAssert_;

	/** The preconditions for the background knowledge. */
	private Collection<RelationalPredicate> preConds_;

	/** The postcondition (asserted value) for the background knowledge. */
	private RelationalPredicate postCondition_;

	/** If this background knowledge is equivalent or just inferred. */
	private boolean equivalentRule_;

	/** If equivalent, which side of the knowledge will be simplified to. */
	private int precendence_;

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
		precendence_ = LEFT_SIDE;
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

		// Determine precendence if equivalent rule
		if (equivalentRule_) {
			// If the left side have more conditions (but the right isn't
			// negated)
			if (preConds_.size() > 1 && !postCondition_.isNegated())
				precendence_ = RIGHT_SIDE;
		}
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
	 * @return True if the conditions were simplified.
	 */
	@SuppressWarnings("unchecked")
	public boolean simplify(SortedSet<RelationalPredicate> ruleConds) {
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

			Collection<RelationalPredicate> backgroundConds = new ArrayList<RelationalPredicate>();
			backgroundConds.add(new RelationalPredicate(postCondition_));
			Collection<RelationalPredicate> resultantFacts = new ArrayList<RelationalPredicate>();
			for (RelationalPredicate rp : preConds_)
				resultantFacts.add(new RelationalPredicate(rp));

			// If precendence is on the other side, swap facts.
			if (precendence_ == RIGHT_SIDE) {
				Collection<RelationalPredicate> temp = backgroundConds;
				backgroundConds = resultantFacts;
				resultantFacts = temp;
			}

			result = Unification.getInstance().unifyStates(backgroundConds,
					ruleConds, replacementTerms);
			if (result == 0) {
				// Remove any replacements which specialise anonymous terms
				replacementTerms = replacementTerms.inverseBidiMap();

				// Replace the arguments
				for (RelationalPredicate removed : backgroundConds) {
					// If there is a unification, attempt to remove the right
					// side
					removed.replaceArguments(replacementTerms, true, false);
				}

				// Check that all unified conditions exist
				boolean abortAnonymous = false;
				SortedSet<RelationalPredicate> backupConds = new TreeSet<RelationalPredicate>(
						ruleConds);
				if (ruleConds.containsAll(backgroundConds)) {
					changed = true;
					ruleConds.removeAll(backgroundConds);
					for (RelationalPredicate resultFact : resultantFacts) {
						// Check for attempting to create anonymous terms with
						// an anonymous replacement
						if (replacementTerms.containsKey("?")
								&& !replacementTerms.get("?").equals("?")
								&& !resultFact.isFullyNotAnonymous()) {
							abortAnonymous = true;
							break;
						}
						resultFact.replaceArguments(replacementTerms, true,
								false);
						if (!ruleConds.contains(resultFact))
							ruleConds.add(resultFact);
					}
				}

				// If the unification has anonymous unity problems, revert to
				// normal
				if (abortAnonymous) {
					ruleConds.clear();
					ruleConds.addAll(backupConds);
					changed = false;
				}
			}
		}

		return changed;
	}

	/**
	 * Checks if a rule is illegal in regards to this background knowledge.
	 * 
	 * @return True if the conditions are illegal.
	 */
	public boolean checkIllegalRule(SortedSet<RelationalPredicate> ruleConds, boolean fixRule) {
		// Check for illegal rules
		BidiMap replacementTerms = new DualHashBidiMap();
		int result = Unification.getInstance().unifyStates(
				getConjugatedConditions(), ruleConds, replacementTerms);
		// If the rule is found to be illegal using the conjugated
		// conditions, remove the illegal condition
		if (result == 0) {
			RelationalPredicate cond = getPostCond(replacementTerms);
			cond.swapNegated();
			if (ruleConds.contains(cond)) {
				if (fixRule)
					ruleConds.remove(cond);
				return true;
			}
		}
		return false;
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
		// Output the rule differently if precendence is on the right side
		if (precendence_ == RIGHT_SIDE) {
			String[] split = assertionString_.split(" <=> ");
			return split[1] + " <=> " + split[0];
		}
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
