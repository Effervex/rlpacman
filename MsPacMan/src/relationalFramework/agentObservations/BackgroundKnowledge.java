package relationalFramework.agentObservations;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import cerrla.RLGGMerger;
import cerrla.MergedFact;

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

	/** The string of the rule, if asserted through the environment. */
	private String[] ruleString_;

	/** The preconditions for the background knowledge. */
	private SortedSet<RelationalPredicate> preConds_;

	/** The postcondition (asserted value) for the background knowledge. */
	private RelationalPredicate postCondition_;

	/** If this background knowledge is equivalent or just inferred. */
	private boolean equivalentRule_;

	/** If equivalent, which side of the knowledge will be simplified to. */
	private int precendence_;

	/** If the rule is logical. */
	private boolean logical_;

	/**
	 * A constructor for environment asserted background knowledge. The facts of
	 * the rule will not attempt to be parsed.
	 * 
	 * @param assertion
	 *            The assertion in JESS format.
	 * @param logical
	 *            If the fact is a logical fact (assertions/retractions matter).
	 */
	public BackgroundKnowledge(String assertion, boolean logical) {
		logical_ = logical;
		ruleString_ = assertion.split(" <=> ");
		equivalentRule_ = true;
		if (ruleString_.length == 1) {
			ruleString_ = assertion.split(" => ");
			equivalentRule_ = false;
		}
	}

	/**
	 * Creates background knowledge using left side relations, a relation and a
	 * right side relation.
	 * 
	 * @param leftRelations
	 *            The left side conditions.
	 * @param isEquivalenceRule
	 *            The relation (equivalent or inference).
	 * @param rightRelation
	 *            The right side relation.
	 */
	public BackgroundKnowledge(Collection<RelationalPredicate> leftRelations,
			boolean isEquivalenceRule, RelationalPredicate rightRelation) {
		initialise(leftRelations, isEquivalenceRule, rightRelation);
	}

	/**
	 * Initialises the pre, post and relation members.
	 * 
	 * @param leftRelations
	 *            The left side conditions.
	 * @param isEquivalenceRule
	 *            The relation (equivalent or inference).
	 * @param rightRelation
	 *            The right side relation.
	 */
	private void initialise(Collection<RelationalPredicate> leftRelations,
			boolean isEquivalenceRule, RelationalPredicate rightRelation) {
		equivalentRule_ = isEquivalenceRule;
		precendence_ = LEFT_SIDE;

		preConds_ = new TreeSet<RelationalPredicate>();
		for (RelationalPredicate leftCond : leftRelations)
			preConds_.add(new RelationalPredicate(leftCond));
		postCondition_ = new RelationalPredicate(rightRelation);

		// Determine precendence if equivalent rule
		if (equivalentRule_) {
			// If the left side have more conditions (but the right isn't
			// negated)
			if (preConds_.size() > 1 && !postCondition_.isNegated())
				precendence_ = RIGHT_SIDE;
		}

		normaliseRuleArgs();

		if (equivalentRule_ && preConds_.size() == 1
				&& preConds_.iterator().next().compareTo(postCondition_) > 0)
			// Otherwise check which side is simpler
			precendence_ = RIGHT_SIDE;
	}

	/**
	 * Creates agent learned background knowledge from a String. Should really
	 * only be needed for tests.
	 * 
	 * @param rule
	 *            The rule to parse.
	 */
	public BackgroundKnowledge(String rule) {
		String[] split = rule.split(" <=> ");
		equivalentRule_ = true;
		if (split.length == 1) {
			split = rule.split(" => ");
			equivalentRule_ = false;
		}

		initialise(RelationalRule.splitConditions(split[0], null),
				equivalentRule_, StateSpec.toRelationalPredicate(split[1]));
	}

	/**
	 * Normalises the rule arguments such that the right side only concerns ?X.
	 * Also cleans up equivalencies such that the left side doesn't introduce
	 * any further variables not seen in the right side.
	 */
	private void normaliseRuleArgs() {
		Map<RelationalArgument, RelationalArgument> replacementMap = null;
		if (precendence_ == LEFT_SIDE)
			replacementMap = postCondition_.createVariableTermReplacementMap(
					false, true);
		else
			replacementMap = preConds_.iterator().next()
					.createVariableTermReplacementMap(false, true);
		postCondition_.safeReplaceArgs(replacementMap);
		SortedSet<RelationalPredicate> newPreConds = new TreeSet<RelationalPredicate>(
				preConds_.comparator());
		for (RelationalPredicate preCond : preConds_) {
			if (equivalentRule_ && precendence_ == LEFT_SIDE)
				preCond.replaceArguments(replacementMap, false, false);
			else
				preCond.safeReplaceArgs(replacementMap);
			newPreConds.add(preCond);
		}
		preConds_ = newPreConds;
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
	public boolean simplify(Collection<RelationalPredicate> ruleConds) {
		BidiMap replacementTerms = new DualHashBidiMap();

		// Unify with the preConds to check if they're there
		Collection<RelationalPredicate> allFacts = getAllFacts();
		Collection<MergedFact> unified = RLGGMerger.getInstance().unifyStates(
				allFacts, ruleConds, replacementTerms);
		if (unified.size() == allFacts.size()) {
			Collection<RelationalPredicate> nonPreferred = getNonPreferredFacts();
			for (MergedFact unifact : unified) {
				if (nonPreferred.contains(unifact.getBaseFact()))
					ruleConds.remove(unifact.getUnityFact());
			}
			return true;
		}

		// Equivalent rule simplification
		if (equivalentRule_) {
			// Unify with post conds and if a result, insert the replacement
			Collection<RelationalPredicate> nonPreferred = getNonPreferredFacts();
			replacementTerms.clear();
			unified = RLGGMerger.getInstance().unifyStates(nonPreferred,
					ruleConds, replacementTerms);
			// If the post cond fully unified
			if (unified.size() == nonPreferred.size()) {
				// Remove the post conds.
				for (MergedFact unifact : unified) {
					ruleConds.remove(unifact.getUnityFact());
					replacementTerms = unifact.getResultReplacements();
				}

				// Add the preconds (with replacements)
				replacementTerms = replacementTerms.inverseBidiMap();
				for (RelationalPredicate resultFact : getPreferredFacts()) {
					RelationalPredicate cloneFact = new RelationalPredicate(
							resultFact);
					cloneFact.replaceArguments(replacementTerms, false, false);
					if (!ruleConds.contains(cloneFact))
						ruleConds.add(cloneFact);
				}
				return true;
			}
		}

		return false;
	}

	private Collection<RelationalPredicate> getAllFacts() {
		ArrayList<RelationalPredicate> allFacts = new ArrayList<RelationalPredicate>(
				preConds_);
		allFacts.add(postCondition_);
		return allFacts;
	}

	/**
	 * Checks if a rule is illegal in regards to this background knowledge.
	 * 
	 * @param ruleConds
	 *            The rule conditions to evaluate.
	 * @param fixRule
	 *            If the rule should be modified to become legal.
	 * @return True if the conditions are illegal.
	 */
	public boolean checkIllegalRule(Collection<RelationalPredicate> ruleConds,
			boolean fixRule) {
		// Check for illegal rules
		BidiMap replacementTerms = new DualHashBidiMap();
		Collection<RelationalPredicate> conjugated = getConjugatedConditions();
		Collection<MergedFact> unified = RLGGMerger.getInstance().unifyStates(
				conjugated, ruleConds, replacementTerms);
		// If the rule is found to be illegal using the conjugated
		// conditions, remove the illegal condition
		if (unified.size() == conjugated.size()) {
			if (fixRule) {
				// Fix up the rule (remove illegals)
				Collection<RelationalPredicate> nonPreferred = getNonPreferredFacts();
				for (MergedFact unifact : unified) {
					RelationalPredicate negUniFact = new RelationalPredicate(
							unifact.getBaseFact());
					negUniFact.swapNegated();
					if (nonPreferred.contains(negUniFact))
						ruleConds.remove(unifact.getUnityFact());
				}
			}
			return true;
		}
		return false;
	}

	public boolean isEquivalence() {
		return equivalentRule_;
	}

	@Override
	public String toString() {
		String left, right = null;
		if (ruleString_ == null) {
			// Agent created rule
			// Output the rule differently if precendence is on the right side
			left = StateSpec.conditionsToString(preConds_);
			right = postCondition_.toString();
			if (precendence_ == RIGHT_SIDE) {
				String backup = left;
				left = right;
				right = backup;
			}
		} else {
			// Environment created rule
			left = ruleString_[0];
			right = ruleString_[1];
			if (logical_)
				left = "(logical " + left + ")";
		}

		String relation = " => ";
		if (equivalentRule_)
			relation = " <=> ";

		return left + relation + right;
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

		// Compare by preferred facts
		Iterator<RelationalPredicate> thisIter = getPreferredFacts().iterator();
		Iterator<RelationalPredicate> otherIter = other.getPreferredFacts()
				.iterator();
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

		// Compare by non-preferred facts
		thisIter = getNonPreferredFacts().iterator();
		otherIter = other.getNonPreferredFacts().iterator();
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

		return 0;
	}

	/**
	 * Gets the fact(s) on the non-preferred side of the rule, that is, the
	 * right side when the rule is printed.
	 * 
	 * @return The fact(s) that are to be removed using the opposite side.
	 */
	public Collection<RelationalPredicate> getNonPreferredFacts() {
		if (precendence_ == LEFT_SIDE) {
			Collection<RelationalPredicate> nonPreferred = new ArrayList<RelationalPredicate>();
			nonPreferred.add(postCondition_);
			return nonPreferred;
		} else {
			return new ArrayList<RelationalPredicate>(preConds_);
		}
	}

	/**
	 * Gets the fact(s) on the preferred side of the rule, that is, the left
	 * side when the rule is printed.
	 * 
	 * @return The fact(s) that take precedence or a swapped in.
	 */
	public Collection<RelationalPredicate> getPreferredFacts() {
		if (precendence_ == LEFT_SIDE) {
			return new ArrayList<RelationalPredicate>(preConds_);
		} else {
			Collection<RelationalPredicate> nonPreferred = new ArrayList<RelationalPredicate>();
			nonPreferred.add(postCondition_);
			return nonPreferred;
		}
	}
}
