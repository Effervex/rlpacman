/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/relationalFramework/agentObservations/BackgroundKnowledge.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
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
