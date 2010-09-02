package relationalFramework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.collections.BidiMap;

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
	private Collection<String> preconds_;

	/** The postcondition (asserted value) for the background knowledge. */
	private String postCondition_;

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
		String[] split = assertion.split(StateSpec.INFERS_ACTION);
		preconds_ = GuidedRule.splitConditions(split[0]);

		split[1] = split[1].trim();
		String assertStr = "(assert ";
		if (split[1].contains(assertStr))
			postCondition_ = split[1].substring(assertStr.length(), split[1]
					.length() - 1);
		else
			postCondition_ = split[1].trim();
	}

	/**
	 * Gets all conditions used in this background knowledge. That's both the
	 * conditions and the asserted information together.
	 * 
	 * @return A collection of all conditions shown in the background rule.
	 */
	public Collection<String> getAllConditions() {
		Collection<String> backgroundConditions = new ArrayList<String>(
				preconds_);
		backgroundConditions.add(postCondition_);
		return backgroundConditions;
	}

	/**
	 * Gets the rule in conjugate form, such that the postcondition is negated.
	 * 
	 * @return A collection of all conditions with the postcondition negated.
	 */
	public Collection<String> getConjugatedConditions() {
		Collection<String> backgroundConditions = new ArrayList<String>(
				preconds_);

		// Dealing with double negation
		String not = "(not ";
		if (postCondition_.substring(0, not.length()).equals(not))
			backgroundConditions.add(postCondition_.substring(not.length(),
					postCondition_.length() - 1));
		else
			backgroundConditions.add(postCondition_);

		return backgroundConditions;
	}

	/**
	 * Gets the post condition of the background knowledge with the terms of the
	 * condition swapped for the replacement terms.
	 * 
	 * @param replacementTerms
	 *            The replacement terms to swap the terms with.
	 */
	public String getPostCond(BidiMap replacementTerms) {
		String cond = postCondition_;
		for (Object key : replacementTerms.values())
			cond = cond.replaceAll(" " + Pattern.quote((String) key)
					+ "(?=( |\\)))", " " + replacementTerms.getKey(key));
		return cond;
	}

	/**
	 * If this rule should be asserted in jess. Otherwise, the rule is just used
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
