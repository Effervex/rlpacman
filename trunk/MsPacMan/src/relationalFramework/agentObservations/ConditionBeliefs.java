package relationalFramework.agentObservations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import relationalFramework.RuleCreation;
import relationalFramework.MultiMap;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

/**
 * A class representing the beliefs the agent has regarding the inter-relations
 * between conditions in the environment.
 * 
 * @author Sam Sarjant
 */
public class ConditionBeliefs implements Serializable {
	private static final long serialVersionUID = 1256381798685341459L;

	/** The condition this class represents as a base. */
	private String condition_;

	/** The set of conditions that are always true when this condition is true. */
	private Collection<StringFact> alwaysTrue_;

	/** The set of conditions that are never true when this condition is true. */
	private Collection<StringFact> neverTrue_;

	/** A flag to note if the never true values have been initialised yet. */
	private boolean firstState = true;

	/**
	 * The set of conditions that are occasionally true when this condition is
	 * true.
	 */
	private Collection<StringFact> occasionallyTrue_;

	/**
	 * The set of conditions with predicate condition that are more general
	 * version of this condition.
	 */
	private Collection<StringFact> disallowed_;

	/**
	 * The argument index for the condition belief. Only used for negated
	 * condition beliefs.
	 */
	private byte argIndex_ = -1;

	/**
	 * The initialisation for condition beliefs, taking the condition, and the
	 * initial relative state it was created in.
	 * 
	 * @param condition
	 *            The condition base.
	 */
	public ConditionBeliefs(String condition) {
		condition_ = condition;
		alwaysTrue_ = new TreeSet<StringFact>();
		neverTrue_ = new TreeSet<StringFact>();
		occasionallyTrue_ = new TreeSet<StringFact>();
		disallowed_ = new TreeSet<StringFact>();
	}

	/**
	 * A constructor for a negated, argument-index specific condition beliefs
	 * object.
	 * 
	 * @param factName
	 *            The (negated) fact name.
	 * @param argIndex
	 *            he argument index this condition beliefs object represents.
	 */
	public ConditionBeliefs(String factName, byte argIndex) {
		this(factName);
		argIndex_ = argIndex;
	}

	/**
	 * Notes a true relative fact to the condition. Note that the fact arguments
	 * must all be variables or anonymous.
	 * 
	 * @param trueFacts
	 *            The relative facts that were true under a single observation.
	 * @param untrueFacts
	 *            The relative facts that are untrue with regards to the true
	 *            facts. To be filled.
	 * @param addGeneralities
	 *            If more general versions of the main predicate should be
	 *            added.
	 * @return True if the condition beliefs changed at all.
	 */
	public boolean noteTrueRelativeFacts(Collection<StringFact> trueFacts,
			Collection<StringFact> untrueFacts, boolean addGeneralities) {
		// If this is our first state, all true facts are always true, and all
		// others are always false.
		if (firstState) {
			alwaysTrue_.addAll(trueFacts);
			addNeverSeenPreds();
			if (addGeneralities)
				rearrangeGeneralRules();
			if (untrueFacts != null) {
				// Note the untrue facts
				untrueFacts.addAll(neverTrue_);
			}
			return true;
		}
		// If the condition has no relations don't bother updating.
		else if (alwaysTrue_.isEmpty() && neverTrue_.isEmpty())
			return false;

		// Clone the collection so no changes are made.
		trueFacts = new HashSet<StringFact>(trueFacts);

		// Expand the true facts first
		trueFacts.addAll(generateGenerals(trueFacts));

		if (untrueFacts != null) {
			// Note the untrue facts
			untrueFacts.addAll(alwaysTrue_);
			untrueFacts.addAll(occasionallyTrue_);
			untrueFacts.addAll(neverTrue_);
			untrueFacts.removeAll(trueFacts);
		}

		// Filter the disallowed facts
		trueFacts.removeAll(disallowed_);

		// Otherwise, perform a number of intersections to determine the sets.
		// Find any predicates present in this trueFacts not in alwaysTrue
		Collection<StringFact> union = new HashSet<StringFact>(trueFacts);
		union.addAll(alwaysTrue_);
		alwaysTrue_.retainAll(trueFacts);
		union.removeAll(alwaysTrue_);
		if (!union.isEmpty()) {
			neverTrue_.removeAll(union);
			int occSize = occasionallyTrue_.size();
			occasionallyTrue_.addAll(union);
			if (addGeneralities)
				rearrangeGeneralRules();
			if (occasionallyTrue_.size() != occSize)
				return true;
			else
				return false;
		}
		return false;
	}

	/**
	 * Rearranges the rules about if there are specific rules in always or never
	 * such that more general version of those rules are also in always or
	 * never.
	 * 
	 * Also ensures that there are self rules that add more general versions of
	 * this condition to the always true set of rules.
	 */
	private void rearrangeGeneralRules() {
		Collection<StringFact> generals = generateGenerals(alwaysTrue_);
		occasionallyTrue_.removeAll(generals);
		neverTrue_.removeAll(generals);
		alwaysTrue_.addAll(generals);

		// Add general rules to always true
		for (StringFact general : disallowed_) {
			// Do not add fully anonymous/not anonymous at all rules.
			boolean fullyAnon = true;
			boolean fullyVar = true;
			for (String arg : general.getArguments()) {
				if (arg.equals("?"))
					fullyVar = false;
				else
					fullyAnon = false;
			}

			if (!fullyAnon && !fullyVar) {
				occasionallyTrue_.remove(general);
				neverTrue_.remove(general);
				alwaysTrue_.add(general);
			}
		}
	}

	/**
	 * Generates more general versions of each base fact in the base set. Each
	 * fact must be non-anonymous and not the same as the base fact.
	 * 
	 * @param baseSet
	 *            The set of base facts to generalise.
	 * @return The collection of more general facts for the facts from base set.
	 */
	private Collection<StringFact> generateGenerals(
			Collection<StringFact> baseSet) {
		Collection<StringFact> addedGeneralisations = new HashSet<StringFact>();
		// Run through each fact in the base set.
		for (StringFact baseFact : baseSet) {
			// Run through each possible binary implementation of the arguments,
			// adding to the added args. (ignoring first and last case)
			for (int b = 1; b < Math.pow(2, baseFact.getArguments().length) - 1; b++) {
				StringFact general = new StringFact(baseFact);
				String[] factArgs = general.getArguments();
				// Change each argument based on the binary representation.
				boolean changed = false;
				boolean anonymous = true;
				for (int i = 0; i < factArgs.length; i++) {
					// If the argument originally isn't anonymous
					if (!factArgs[i].equals("?")) {
						// Make it anonymous
						if ((b & (int) Math.pow(2, i)) == 0) {
							factArgs[i] = "?";
							changed = true;
						} else
							anonymous = false;
					}
				}

				if (changed && !anonymous)
					addedGeneralisations.add(general);
			}
		}

		return addedGeneralisations;
	}

	/**
	 * Adds all other preds not present in always true to the never true
	 * collection if the never true collection is null.
	 * 
	 * @return True if the collection hasn't been initialised yet.
	 */
	private boolean addNeverSeenPreds() {
		if (firstState == false)
			return false;

		// Run through every possible string fact and add those not present in
		// the other lists.
		MultiMap<String, String> possibleTerms = createActionTerms(StateSpec
				.getInstance().getStringFact(condition_));

		// Run by the predicates
		Set<String> predicates = null;
		if (StateSpec.getInstance().isTypePredicate(condition_))
			predicates = StateSpec.getInstance().getTypePredicates().keySet();
		else
			predicates = StateSpec.getInstance().getPredicates().keySet();
		for (String pred : predicates) {
			// Run by the possible combinations within the predicates.
			for (StringFact fact : createPossibleFacts(pred, possibleTerms)) {
				if (!alwaysTrue_.contains(fact)
						&& !occasionallyTrue_.contains(fact))
					neverTrue_.add(fact);
			}

			// If the same pred, remove the disallowed values from always true
			// as well.
			if (pred.equals(condition_))
				alwaysTrue_.removeAll(disallowed_);
		}
		firstState = false;
		return true;
	}

	/**
	 * Creates a collection of all possible facts that the base condition could
	 * be paired with. This is achieved by recursively placing arguments
	 * throughout every condition. This method is only called once.
	 * 
	 * @param pred
	 *            The predicate to create possible facts for.
	 * @param possibleTerms
	 *            The terms available for the predicate to use on a class mapped
	 *            collection.
	 * @return A collection of facts representing the possible paired
	 *         predicates.
	 */
	private Collection<StringFact> createPossibleFacts(String pred,
			MultiMap<String, String> possibleTerms) {
		Collection<StringFact> shapedFacts = new HashSet<StringFact>();

		// Run through each base fact, shaping as it goes.
		StringFact predFact = StateSpec.getInstance().getStringFact(pred);
		formPossibleFact(new String[predFact.getArguments().length], 0,
				possibleTerms, predFact, shapedFacts);

		// Removing the base condition fact itself
		if (pred.equals(condition_)) {
			String[] baseArgs = new String[predFact.getArguments().length];
			for (int i = 0; i < baseArgs.length; i++)
				baseArgs[i] = RuleCreation.getVariableTermString(i);
			shapedFacts.remove(new StringFact(predFact, baseArgs));
		}

		return shapedFacts;
	}

	/**
	 * Creates the mapping of variable action terms to argument types, based on
	 * the location of the argument within the predicate.
	 * 
	 * @param predicate
	 *            The predicate to form action term map from.
	 * @return The mapping of argument type classes to argument variables.
	 */
	private MultiMap<String, String> createActionTerms(StringFact predicate) {
		MultiMap<String, String> actionTerms = new MultiMap<String, String>();

		String[] argTypes = predicate.getArgTypes();
		for (int i = 0; i < argTypes.length; i++) {
			if (!StateSpec.isNumberType(argTypes[i]))
				actionTerms.put(argTypes[i], RuleCreation
						.getVariableTermString(i));
		}

		return actionTerms;
	}

	/**
	 * Recursively forms a possible variable fact that may exist in the
	 * environment.
	 * 
	 * @param arguments
	 *            The developing array of arguments to form into a fact.
	 * @param index
	 *            The current index being filled by the method.
	 * @param possibleTerms
	 *            The possible terms map.
	 * @param baseFact
	 *            The base fact to build facts from.
	 * @param possibleFacts
	 *            The list of facts to fill.
	 */
	private void formPossibleFact(String[] arguments, int index,
			MultiMap<String, String> possibleTerms, StringFact baseFact,
			Collection<StringFact> possibleFacts) {
		// Base case, if index is outside arguments, build the fact
		String[] argTypes = baseFact.getArgTypes();
		if (index >= argTypes.length) {
			// Check the arguments aren't anonymous and/or a generalisation of
			// the condition itself.
			boolean keepRule = false;
			for (int i = 0; i < arguments.length; i++) {
				if (!arguments[i].equals("?")
						&& !(baseFact.getFactName().equals(condition_) && arguments[i]
								.equals(RuleCreation.getVariableTermString(i)))) {
					keepRule = true;
					break;
				}
			}

			// If not anonymous, form the fact
			StringFact possible = new StringFact(baseFact, Arrays.copyOf(
					arguments, arguments.length));
			if (keepRule) {
				possibleFacts.add(possible);
			} else {
				disallowed_.add(possible);
			}
			return;
		}

		// Use all terms for the slot and '?'
		List<String> terms = new ArrayList<String>();
		terms.add("?");
		if (possibleTerms.containsKey(argTypes[index])) {
			terms.addAll(possibleTerms.get(argTypes[index]));
		}

		// For each term
		MultiMap<String, String> termsClone = new MultiMap<String, String>(
				possibleTerms);
		for (String term : terms) {
			arguments[index] = term;
			// If the term isn't anonymous, remove it from the possible terms
			// (clone).
			if (!term.equals("?")) {
				termsClone.get(argTypes[index]).remove(term);
			}

			// Recurse further
			formPossibleFact(arguments, index + 1, termsClone, baseFact,
					possibleFacts);
		}
	}

	public String getCondition() {
		return condition_;
	}

	public Collection<StringFact> getAlwaysTrue() {
		return alwaysTrue_;
	}

	public Collection<StringFact> getNeverTrue() {
		return neverTrue_;
	}

	public Collection<StringFact> getOccasionallyTrue() {
		return occasionallyTrue_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((alwaysTrue_ == null) ? 0 : alwaysTrue_.hashCode());
		result = prime * result
				+ ((condition_ == null) ? 0 : condition_.hashCode());
		result = prime * result
				+ ((neverTrue_ == null) ? 0 : neverTrue_.hashCode());
		result = prime * result + (firstState ? 1231 : 1237);
		result = prime
				* result
				+ ((occasionallyTrue_ == null) ? 0 : occasionallyTrue_
						.hashCode());
		result = prime * result + argIndex_;
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
		ConditionBeliefs other = (ConditionBeliefs) obj;
		if (argIndex_ != other.argIndex_)
			return false;
		if (alwaysTrue_ == null) {
			if (other.alwaysTrue_ != null)
				return false;
		} else if (!alwaysTrue_.equals(other.alwaysTrue_))
			return false;
		if (condition_ == null) {
			if (other.condition_ != null)
				return false;
		} else if (!condition_.equals(other.condition_))
			return false;
		if (neverTrue_ == null) {
			if (other.neverTrue_ != null)
				return false;
		} else if (!neverTrue_.equals(other.neverTrue_))
			return false;
		if (firstState != other.firstState)
			return false;
		if (occasionallyTrue_ == null) {
			if (other.occasionallyTrue_ != null)
				return false;
		} else if (!occasionallyTrue_.equals(other.occasionallyTrue_))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		if (argIndex_ != -1) {
			buffer.append("!" + condition_ + "(");
			for (int i = 0; (1 << i) <= argIndex_; i++) {
				if (i != 0)
					buffer.append(" ");
				if ((argIndex_ & (1 << i)) != 0)
					buffer.append(RuleCreation.getVariableTermString(i));
				else
					buffer.append("?");
			}
			buffer.append("):\n");
		} else
			buffer.append(condition_ + ":\n");
		buffer.append("\tAlways True: " + alwaysTrue_.toString() + "\n");
		buffer.append("\tNever True: " + neverTrue_.toString() + "\n");
		buffer.append("\tSometimes True: " + occasionallyTrue_.toString());
		return buffer.toString();
	}
}
