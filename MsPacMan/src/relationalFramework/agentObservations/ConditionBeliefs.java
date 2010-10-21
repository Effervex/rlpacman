package relationalFramework.agentObservations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import relationalFramework.Covering;
import relationalFramework.MultiMap;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

/**
 * A class representing the beliefs the agent has regarding the inter-relations
 * between conditions in the environment.
 * 
 * @author Sam Sarjant
 */
public class ConditionBeliefs {
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
	}

	/**
	 * Notes a true relative fact to the condition. Note that the fact arguments
	 * must all be variables or anonymous.
	 * 
	 * @param trueFacts
	 *            The relative facts that were true under a single observation.
	 * @return True if the condition beliefs changed at all.
	 */
	public boolean noteTrueRelativeFacts(Collection<StringFact> trueFacts) {
		// If this is our first state, all true facts are always true, and all
		// others are always false.
		if (firstState) {
			alwaysTrue_.addAll(trueFacts);
			addNeverSeenPreds();
			return true;
		}
		// If the condition has no relations don't bother updating.
		else if (alwaysTrue_.isEmpty() && neverTrue_.isEmpty())
			return false;

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
			if (occasionallyTrue_.size() != occSize)
				return true;
			else
				return false;
		}
		return false;
	}

	/**
	 * Adds all other preds not present in always true to the never true
	 * collection if the never true collection is null.
	 * 
	 * @return True if the collection hasn't been initialised yet.
	 */
	@SuppressWarnings("unchecked")
	private boolean addNeverSeenPreds() {
		if (firstState == false)
			return false;

		// Run through every possible string fact and add those not present in
		// the other lists.

		MultiMap<Class, String> possibleTerms = createActionTerms(StateSpec
				.getInstance().getStringFact(condition_));

		// Run by the predicates
		for (String pred : StateSpec.getInstance().getPredicates().keySet()) {
			// Run by the possible combinations within the predicates.
			for (StringFact fact : createPossibleFacts(pred, possibleTerms)) {
				if (!alwaysTrue_.contains(fact)
						&& !occasionallyTrue_.contains(fact))
					neverTrue_.add(fact);
			}
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
	@SuppressWarnings("unchecked")
	private Collection<StringFact> createPossibleFacts(String pred,
			MultiMap<Class, String> possibleTerms) {
		Collection<StringFact> shapedFacts = new HashSet<StringFact>();

		// Run through each base fact, shaping as it goes.
		StringFact predFact = StateSpec.getInstance().getStringFact(pred);
		formPossibleFact(new String[predFact.getArguments().length], 0,
				possibleTerms, predFact, shapedFacts);

		// Removing the base condition fact itself
		if (pred.equals(condition_)) {
			String[] baseArgs = new String[predFact.getArguments().length];
			for (int i = 0; i < baseArgs.length; i++)
				baseArgs[i] = Covering.getVariableTermString(i);
			shapedFacts.remove(new StringFact(predFact, baseArgs));
		}
		// TODO Expand this to remove preds more general than the pred.

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
	@SuppressWarnings("unchecked")
	private MultiMap<Class, String> createActionTerms(StringFact predicate) {
		MultiMap<Class, String> actionTerms = new MultiMap<Class, String>();

		Class[] argTypes = predicate.getArgTypes();
		for (int i = 0; i < argTypes.length; i++) {
			if (!StateSpec.isNumberClass(argTypes[i]))
				actionTerms.put(argTypes[i], Covering.getVariableTermString(i));
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
	@SuppressWarnings("unchecked")
	private void formPossibleFact(String[] arguments, int index,
			MultiMap<Class, String> possibleTerms, StringFact baseFact,
			Collection<StringFact> possibleFacts) {
		// Base case, if index is outside arguments, build the fact
		Class[] argTypes = baseFact.getArgTypes();
		if (index >= argTypes.length) {
			// Check the arguments aren't fully anonymous
			boolean anonymous = true;
			for (String arg : arguments) {
				if (!arg.equals("?")) {
					anonymous = false;
					break;
				}
			}

			// If not anonymous, form the fact
			if (!anonymous) {
				possibleFacts.add(new StringFact(baseFact, Arrays.copyOf(
						arguments, arguments.length)));
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
		MultiMap<Class, String> termsClone = new MultiMap<Class, String>(
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
		StringBuffer buffer = new StringBuffer(condition_ + ":\n");
		buffer.append("\tAlways True: " + alwaysTrue_.toString() + "\n");
		buffer.append("\tSometimes True: " + occasionallyTrue_.toString()
				+ "\n");
		buffer.append("\tNever True: " + neverTrue_.toString());
		return buffer.toString();
	}
}
