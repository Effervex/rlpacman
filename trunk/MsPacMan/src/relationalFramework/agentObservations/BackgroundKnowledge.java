package relationalFramework.agentObservations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedSet;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import relationalFramework.Covering;
import relationalFramework.GuidedRule;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

/**
 * A class representing background knowledge assertions
 * 
 * @author Sam Sarjant
 */
public class BackgroundKnowledge {
	/** The JESS compatible assertion string. */
	private String assertionString_;

	private boolean jessAssert_;

	/** The preconditions for the background knowledge. */
	private Collection<StringFact> preConds_;

	/** The postcondition (asserted value) for the background knowledge. */
	private StringFact postCondition_;

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
		preConds_ = GuidedRule.splitConditions(split[0]);

		split[1] = split[1].trim();
		String assertStr = "(assert ";
		if (split[1].contains(assertStr))
			postCondition_ = StateSpec.toStringFact(split[1].substring(
					assertStr.length(), split[1].length() - 1));
		else
			postCondition_ = StateSpec.toStringFact(split[1].trim());
	}

	/**
	 * Gets all conditions used in this background knowledge. That's both the
	 * conditions and the asserted information together.
	 * 
	 * @return A collection of all conditions shown in the background rule.
	 */
	private Collection<StringFact> getAllConditions() {
		Collection<StringFact> backgroundConditions = new ArrayList<StringFact>(
				preConds_);
		backgroundConditions.add(postCondition_);
		return backgroundConditions;
	}

	/**
	 * Gets the rule in conjugate form, such that the postcondition is negated.
	 * 
	 * @return A collection of all conditions with the postcondition negated.
	 */
	private Collection<StringFact> getConjugatedConditions() {
		Collection<StringFact> backgroundConditions = new ArrayList<StringFact>(
				preConds_);
		StringFact negated = new StringFact(postCondition_);
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
	private Collection<StringFact> getPreConds(BidiMap replacementTerms) {
		Collection<StringFact> preConds = new ArrayList<StringFact>(preConds_
				.size());
		for (StringFact preCond : preConds_) {
			StringFact replacedFact = new StringFact(preCond);
			replacedFact.replaceArguments(replacementTerms.inverseBidiMap());
			preConds.add(replacedFact);
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
	private StringFact getPostCond(BidiMap replacementTerms) {
		StringFact replacedFact = new StringFact(postCondition_);
		if (replacementTerms != null)
			replacedFact.replaceArguments(replacementTerms.inverseBidiMap());
		return replacedFact;
	}

	/**
	 * Simplifies a set of rule conditions using this background knowledge.
	 * 
	 * @param ruleConds
	 *            The rule conditions to simplify.
	 * @param coveringObj
	 *            The covering object to use unify with.
	 * @param testForIllegalRule
	 *            If the conditions are being tested for illegal conditions too.
	 * @return True if the conditions were simplified, false otherwise.
	 */
	public boolean simplify(SortedSet<StringFact> ruleConds,
			Covering coveringObj, boolean testForIllegalRule) {
		boolean changed = false;
		BidiMap replacementTerms = new DualHashBidiMap();
		int result = coveringObj.unifyStates(getAllConditions(), ruleConds,
				replacementTerms);
		// If all conditions within a background rule are present, remove
		// the inferred condition
		if (result == 0) {
			StringFact cond = getPostCond(replacementTerms);
			if (ruleConds.remove(cond))
				changed = true;
		}

		// Simplify to the left for equivalent background knowledge
		if (equivalentRule_) {
			replacementTerms.clear();
			Collection<StringFact> unifiedEquiv = coveringObj.unifyFact(
					getPostCond(null), ruleConds, new DualHashBidiMap(),
					replacementTerms, new String[0], true);
			for (StringFact unifiedFact : unifiedEquiv) {
				StringFact removed = new StringFact(unifiedFact);
				removed.replaceArguments(replacementTerms.inverseBidiMap());
				ruleConds.remove(removed);
				Collection<StringFact> equivFacts = getPreConds(replacementTerms);
				for (StringFact equivFact : equivFacts) {
					if (!ruleConds.contains(equivFact)) {
						ruleConds.add(equivFact);
						changed = true;
					}
				}
			}
		}

		if (testForIllegalRule) {
			replacementTerms.clear();
			result = coveringObj.unifyStates(getConjugatedConditions(),
					ruleConds, replacementTerms);
			// If the rule is found to be illegal using the conjugated
			// conditions, remove the illegal condition
			if (result == 0) {
				StringFact cond = getPostCond(replacementTerms);
				cond.swapNegated();
				if (ruleConds.remove(cond))
					changed = true;
			}
		}

		return changed;
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
		if (jessAssert_)
			return assertionString_;
		return "STATE RULE: " + assertionString_;
	}
}
