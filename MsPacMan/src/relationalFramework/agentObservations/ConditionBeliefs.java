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
	private static final long serialVersionUID = 5023779765740732969L;

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

	/** More general (and same) versions of this rule. */
	private Collection<StringFact> generalities_;

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
			generalities_ = createGeneralities();
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
			untrueFacts.removeAll(generalities_);
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
	 * Creates the generalities of this fact and this fact itself.
	 * 
	 * @return A collection of all generalities of this rule.
	 */
	private Collection<StringFact> createGeneralities() {
		Collection<StringFact> generalities = new TreeSet<StringFact>();

		// Add general rules to always true (on ?X ?Y) => (on ?X ?), (on ? ?Y)
		StringFact thisFact = StateSpec.getInstance().getStringFact(condition_);
		int permutations = (int) Math.pow(2, thisFact.getArgTypes().length);
		// Create all generalisations of this fact
		for (int p = 1; p < permutations; p++) {
			String[] genArguments = new String[thisFact.getArgTypes().length];
			// Run through each index location, using bitwise ops
			for (int i = 0; i < genArguments.length; i++) {
				// If the argument is not 0, enter a variable.
				if ((p & (int) Math.pow(2, i)) != 0)
					genArguments[i] = RuleCreation
							.getVariableTermString(i);
				else
					genArguments[i] = StateSpec.ANONYMOUS;
			}

			generalities.add(new StringFact(thisFact, genArguments));
		}

		return generalities;
	}

	/**
	 * Not to be confused with noteTrueRelativeFacts, noteTrueFacts takes the
	 * facts that are true and the facts that are false and performs no extra
	 * reasoning for further facts.
	 * 
	 * The occasionally used facts contain previously used facts that have been
	 * both true and false. Note that newly added true or false facts that are
	 * not present in true, false or occasionally used will be added to their
	 * appropriate location (true or false).
	 * 
	 * @param trueFacts
	 *            The facts that are true.
	 * @param untrueFacts
	 *            The facts that are false.
	 * @param occasionalFacts
	 *            The facts that are sometimes true, sometimes false.
	 */
	public boolean noteFacts(Collection<StringFact> trueFacts,
			Collection<StringFact> untrueFacts,
			Collection<StringFact> occasionalFacts) {
		if (firstState) {
			alwaysTrue_.addAll(trueFacts);
			neverTrue_.addAll(untrueFacts);
			occasionallyTrue_.addAll(occasionalFacts);
			firstState = false;
			return true;
		}

		// Union the facts (and add any unseen facts)
		boolean changed = false;
//		changed |= alwaysTrue_.retainAll(trueFacts);
//		changed |= neverTrue_.retainAll(untrueFacts);
//		changed |= occasionallyTrue_.retainAll(occasionalFacts);
		changed |= alwaysTrue_.removeAll(occasionalFacts);
		changed |= neverTrue_.removeAll(occasionalFacts);
		changed |= occasionallyTrue_.addAll(occasionalFacts);

		for (StringFact trueFact : trueFacts) {
			// If always doesn't contain the fact
			if (!alwaysTrue_.contains(trueFact)) {
				if (neverTrue_.contains(trueFact)) {
					// ...and never does, remove it from neverTrue and add to
					// occasional
					neverTrue_.remove(trueFact);
					occasionallyTrue_.add(trueFact);
					changed = true;
				} else if (!occasionallyTrue_.contains(trueFact)) {
					// ...and neither never nor occasional do, add to always
					alwaysTrue_.add(trueFact);
					changed = true;
				}
			}
		}

		for (StringFact untrueFact : untrueFacts) {
			// If always doesn't contain the fact
			if (!neverTrue_.contains(untrueFact)) {
				if (alwaysTrue_.contains(untrueFact)) {
					// ...and never does, remove it from neverTrue and add to
					// occasional
					alwaysTrue_.remove(untrueFact);
					occasionallyTrue_.add(untrueFact);
					changed = true;
				} else if (!occasionallyTrue_.contains(untrueFact)) {
					// ...and neither never nor occasional do, add to always
					neverTrue_.add(untrueFact);
					changed = true;
				}
			}
		}
		return changed;
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

		for (StringFact general : generalities_) {
			occasionallyTrue_.remove(general);
			neverTrue_.remove(general);
			alwaysTrue_.add(general);
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
		Set<String> predicates = new HashSet<String>();
		predicates.addAll(StateSpec.getInstance().getTypePredicates().keySet());
		predicates.addAll(StateSpec.getInstance().getPredicates().keySet());
		for (String pred : predicates) {
			// Run by the possible combinations within the predicates.
			for (StringFact fact : createPossibleFacts(pred, possibleTerms)) {
				if (!alwaysTrue_.contains(fact)
						&& !occasionallyTrue_.contains(fact))
					neverTrue_.add(fact);
			}

			// If the same pred, remove the disallowed values from always
			// true as well.
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
	 * the location of the argument within the predicate. Note that type
	 * hierarchies are also included.
	 * 
	 * @param predicate
	 *            The predicate to form action term map from.
	 * @return The mapping of argument type classes to argument variables.
	 */
	private MultiMap<String, String> createActionTerms(StringFact predicate) {
		MultiMap<String, String> actionTerms = MultiMap.createListMultiMap();

		String[] argTypes = predicate.getArgTypes();
		for (int i = 0; i < argTypes.length; i++) {
			if (!StateSpec.isNumberType(argTypes[i])) {
				String varName = RuleCreation.getVariableTermString(i);
				actionTerms.putContains(argTypes[i], varName);
				// Also put any parent type of the given type
				Collection<String> parentTypes = new HashSet<String>();
				StateSpec.getInstance()
						.getTypeLineage(argTypes[i], parentTypes);
				for (String parent : parentTypes)
					actionTerms.putContains(parent, varName);
			}
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
				if (!arguments[i].equals("?")) {
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
		for (String term : terms) {
			arguments[index] = term;

			// Recurse further
			formPossibleFact(arguments, index + 1, possibleTerms, baseFact,
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
