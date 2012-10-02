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
 *    src/relationalFramework/agentObservations/ConditionBeliefs.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package relationalFramework.agentObservations;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import util.MultiMap;

/**
 * A class representing the beliefs the agent has regarding the inter-relations
 * between conditions in the environment.
 * 
 * @author Sam Sarjant
 */
public class ConditionBeliefs implements Serializable {
	private static final long serialVersionUID = 4750317671587987827L;

	/**
	 * The argument index for the condition belief. Only used for negated
	 * condition beliefs.
	 */
	private RelationalArgument[] args_ = null;

	/** The fact this {@link ConditionBeliefs} represents. */
	private RelationalPredicate cbFact_;

	/** The condition this class represents as a base. */
	private String condition_;

	/**
	 * The set of conditions with predicate condition that are more general
	 * version of this condition.
	 */
	private Set<RelationalPredicate> disallowed_;

	/** More general (and same) versions of this rule. */
	private Set<RelationalPredicate> generalities_;

	/** The beliefs for each typed argument and the overall type-less beliefs. */
	private Map<RelationalPredicate, TypedBeliefs> typedCondBeliefs_;

	/**
	 * The initialisation for condition beliefs, taking the condition, and the
	 * initial relative state it was created in.
	 * 
	 * @param condition
	 *            The condition base.
	 */
	public ConditionBeliefs(String condition) {
		condition_ = condition;
		typedCondBeliefs_ = new HashMap<RelationalPredicate, TypedBeliefs>();
		disallowed_ = new TreeSet<RelationalPredicate>();
		formCBFact();
	}

	/**
	 * A constructor for a negated, argument-index specific condition beliefs
	 * object.
	 * 
	 * @param factName
	 *            The (negated) fact name.
	 * @param untrueFactArgs
	 *            The arguments this condition beliefs object represents.
	 */
	public ConditionBeliefs(String factName, RelationalArgument[] untrueFactArgs) {
		this(factName);
		args_ = untrueFactArgs;
		formCBFact();
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
	private MultiMap<String, RelationalArgument> createActionTerms(
			RelationalPredicate predicate) {
		MultiMap<String, RelationalArgument> actionTerms = MultiMap
				.createListMultiMap();

		String[] argTypes = predicate.getArgTypes();
		for (int i = 0; i < argTypes.length; i++) {
			if (!StateSpec.isNumberType(argTypes[i])) {
				RelationalArgument varName = null;
				if (args_ != null)
					varName = args_[i];
				else
					varName = RelationalArgument.createVariableTermArg(i);
				if (!varName.isAnonymous()) {
					actionTerms.putContains(argTypes[i], varName);
					// Also put any parent type of the given type
					Collection<String> parentTypes = StateSpec.getInstance()
							.getTypeLineage(argTypes[i]);
					for (String parent : parentTypes)
						actionTerms.putContains(parent, varName);
				}
			}
		}

		return actionTerms;
	}

	/**
	 * Creates the actual background knowledge rule if the rule isn't superceded
	 * by an existing rule. Returns the mapping to the rule.
	 * 
	 * @param leftConds
	 *            The left side conditions of the rule.
	 * @param right
	 *            The right side condition of the rule.
	 * @param negationType
	 *            If the right side is negated.
	 * @param isEquivalenceRule
	 *            If true: equivalence rule, false: inference rule.
	 * @param currentKnowledge
	 *            The current background knowledge to add to/evaluate against.
	 * @return If the background rule was created.
	 */
	private boolean createBackgroundRule(
			SortedSet<RelationalPredicate> leftConds,
			RelationalPredicate right, boolean negationType,
			boolean isEquivalenceRule,
			NonRedundantBackgroundKnowledge currentKnowledge) {
		// Create the rule
		if (!negationType)
			right.swapNegated();
		BackgroundKnowledge bckKnow = new BackgroundKnowledge(leftConds,
				isEquivalenceRule, right);

		// Add the rule to current knowledge
		return currentKnowledge.addBackgroundKnowledge(bckKnow);
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
	private Collection<RelationalPredicate> createPossibleFacts(String pred,
			MultiMap<String, RelationalArgument> possibleTerms) {
		Collection<RelationalPredicate> shapedFacts = new HashSet<RelationalPredicate>();

		// Run through each base fact, shaping as it goes.
		RelationalPredicate predFact = StateSpec.getInstance()
				.getPredicateByName(pred);
		// Special case - if this condition is a type and the fact is a type add
		// it simply.
		if (StateSpec.getInstance().isTypePredicate(condition_)
				&& StateSpec.getInstance().isTypePredicate(pred)) {
			predFact = new RelationalPredicate(predFact,
					new RelationalArgument[] { possibleTerms.values()
							.iterator().next() });
			shapedFacts.add(predFact);
		} else {
			formPossibleFact(
					new RelationalArgument[predFact.getArgTypes().length], 0,
					possibleTerms, predFact, shapedFacts);
		}

		// Removing the base condition fact itself
		if (args_ == null && pred.equals(condition_)) {
			String[] baseArgs = new String[predFact.getArgTypes().length];
			for (int i = 0; i < baseArgs.length; i++)
				baseArgs[i] = RelationalArgument.createVariableTermArg(i)
						.toString();
			shapedFacts.remove(new RelationalPredicate(predFact, baseArgs));
		}

		return shapedFacts;
	}

	/**
	 * Creates the background knowledge to represent the condition relations.
	 * This knowledge may be an equivalence relation if the relations between
	 * conditions have equivalences.
	 * 
	 * @param extraCond
	 *            The extra condition associated with the main condition.
	 * @param otherCond
	 *            The possibly negated, usually right-side condition.
	 * @param negationType
	 *            The state of negation: true if not negated, false if negated.
	 * @param conditionBeliefs
	 *            The set of all positive condition beliefs.
	 * @param negatedConditionBeliefs
	 *            The set of all negated condition beliefs.
	 * @param currentKnowledge
	 *            The current background knowledge to add to.
	 * @return If any relations were created.
	 */
	@SuppressWarnings("unchecked")
	private boolean createRelation(
			RelationalPredicate extraCond,
			RelationalPredicate otherCond,
			boolean negationType,
			Map<String, ConditionBeliefs> conditionBeliefs,
			Map<String, Map<IntegerArray, ConditionBeliefs>> negatedConditionBeliefs,
			NonRedundantBackgroundKnowledge currentKnowledge) {
		RelationalPredicate left = new RelationalPredicate(cbFact_);
		RelationalPredicate right = new RelationalPredicate(otherCond);
		if (left.equals(right) || otherCond.equals(extraCond))
			return false;

		SortedSet<RelationalPredicate> leftConds = new TreeSet<RelationalPredicate>();
		leftConds.add(left);
		if (extraCond != null)
			leftConds.add(extraCond);
		// Create an equivalence relation if possible.
		if (!otherCond.getFactName().equals(condition_)) {
			// Form the anti-replacement map
			BidiMap replacementMap = new DualHashBidiMap();
			RelationalArgument[] otherArgs = otherCond.getRelationalArguments();
			for (int i = 0; i < otherArgs.length; i++) {
				if (!otherArgs[i].isAnonymous())
					replacementMap.put(otherArgs[i],
							RelationalArgument.createVariableTermArg(i));
			}

			// If the sets are equal, the relations are equivalent!
			if (isEquivalentConditions(extraCond, otherCond, negationType,
					conditionBeliefs, negatedConditionBeliefs, replacementMap)) {
				// Swap the facts if the otherCond is simpler
				if (negationType && cbFact_.compareTo(otherCond) > 0) {
					left.safeReplaceArgs(replacementMap);
					right.replaceArguments(replacementMap, false, false);
					// Swap left and right if left is negated
					if (left.isNegated() && extraCond == null) {
						RelationalPredicate backup = left;
						left = right;
						right = backup;
					}

					leftConds.clear();
					leftConds.add(left);
					if (extraCond != null) {
						RelationalPredicate modExtraCond = new RelationalPredicate(
								extraCond);
						modExtraCond.replaceArguments(replacementMap, false,
								false);
						leftConds.add(modExtraCond);
					}
				}

				return createBackgroundRule(leftConds, right, negationType, true,
						currentKnowledge);
			}
		}

		// Create the basic inference relation
		return createBackgroundRule(leftConds, right, negationType, false,
				currentKnowledge);
	}

	/**
	 * Forms the {@link ConditionBeliefs} fact this represents.
	 */
	private void formCBFact() {
		cbFact_ = StateSpec.getInstance().getPredicateByName(condition_);
		RelationalArgument[] arguments = new RelationalArgument[cbFact_
				.getArgTypes().length];
		for (int i = 0; i < arguments.length; i++) {
			if (args_ == null)
				arguments[i] = RelationalArgument.createVariableTermArg(i);
			else
				arguments[i] = args_[i];
		}

		cbFact_ = new RelationalPredicate(cbFact_, arguments);
		if (args_ != null)
			cbFact_.swapNegated();
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
	private void formPossibleFact(RelationalArgument[] arguments, int index,
			MultiMap<String, RelationalArgument> possibleTerms,
			RelationalPredicate baseFact,
			Collection<RelationalPredicate> possibleFacts) {
		// Base case, if index is outside arguments, build the fact
		String[] argTypes = baseFact.getArgTypes();
		if (index >= argTypes.length) {
			// Check the arguments aren't anonymous and/or a generalisation of
			// the condition itself.
			boolean keepRule = false;
			for (int i = 0; i < arguments.length; i++) {
				if (!arguments[i].isAnonymous()) {
					keepRule = true;
					break;
				}
			}

			// If not anonymous, form the fact
			RelationalPredicate possible = new RelationalPredicate(baseFact,
					Arrays.copyOf(arguments, arguments.length));
			if (keepRule) {
				possibleFacts.add(possible);
			} else {
				disallowed_.add(possible);
			}
			return;
		}

		// Use all terms for the slot and '?'
		List<RelationalArgument> terms = new ArrayList<RelationalArgument>();
		terms.add(RelationalArgument.ANONYMOUS);
		if (possibleTerms.containsKey(argTypes[index])) {
			terms.addAll(possibleTerms.get(argTypes[index]));
		}

		// For each term
		for (RelationalArgument term : terms) {
			arguments[index] = term;

			// Recurse further
			formPossibleFact(arguments, index + 1, possibleTerms, baseFact,
					possibleFacts);
		}
	}

	/**
	 * Gets the base (un-typed) beliefs of a condition belief.
	 * 
	 * @return The base beliefs.
	 */
	private TypedBeliefs getBaseBeliefs() {
		return typedCondBeliefs_.get(cbFact_);
	}

	/**
	 * Gets the set of condition beliefs for a given condition and negation
	 * type.
	 * 
	 * @param otherCond
	 *            The other condition.
	 * @param negationType
	 *            The state of negation: true if not negated, false if negated.
	 * @param conditionBeliefs
	 *            The other condition beliefs.
	 * @param negatedConditionBeliefs
	 *            The other negated condition beliefs.
	 * @return The set of facts used for comparing with equivalence.
	 */
	private ConditionBeliefs getOtherConditionRelations(
			RelationalPredicate otherCond,
			boolean negationType,
			Map<String, ConditionBeliefs> conditionBeliefs,
			Map<String, Map<IntegerArray, ConditionBeliefs>> negatedConditionBeliefs) {
		String factName = otherCond.getFactName();
		if (negationType) {
			// No negation - simply return the set given by the fact name
			if (conditionBeliefs.containsKey(factName))
				return conditionBeliefs.get(factName);
		} else {
			// Negation - return the appropriate conjunction of condition
			// beliefs
			if (negatedConditionBeliefs.containsKey(factName)) {
				Map<IntegerArray, ConditionBeliefs> negatedCBs = negatedConditionBeliefs
						.get(factName);
				IntegerArray argState = EnvironmentAgentObservations
						.determineArgState(otherCond);
				if (negatedCBs.containsKey(argState))
					return negatedCBs.get(argState);
			}
		}
		return null;
	}

	/**
	 * Checks if this condition is equivalent to another by comparing the facts
	 * seen by each of them (taking into account optional extra conditions).
	 * 
	 * @param extraCond
	 *            The optional extra condition.
	 * @param otherCond
	 *            The condition to check against.
	 * @param negationType
	 *            The negated state of the other condition.
	 * @param conditionBeliefs
	 *            The global condition beliefs.
	 * @param negatedConditionBeliefs
	 *            The global negated condition beliefs.
	 * @param replacementMap
	 *            The replacement map to normalise this fact to the other fact.
	 * @return True if this condition (with extra condition) is equivalent to
	 *         the other condition.
	 */
	@SuppressWarnings("unchecked")
	private boolean isEquivalentConditions(
			RelationalPredicate extraCond,
			RelationalPredicate otherCond,
			boolean negationType,
			Map<String, ConditionBeliefs> conditionBeliefs,
			Map<String, Map<IntegerArray, ConditionBeliefs>> negatedConditionBeliefs,
			BidiMap replacementMap) {
		// Get the conditions for this cond.
		Set<RelationalPredicate> thisAlwaysTrue = getAlwaysTrue(extraCond);
		Set<RelationalPredicate> thisSometimesTrue = getOccasionallyTrue(extraCond);

		// Get the other condition beliefs based on negation type.
		ConditionBeliefs otherCBs = getOtherConditionRelations(otherCond,
				negationType, conditionBeliefs, negatedConditionBeliefs);
		Set<RelationalPredicate> thatAlwaysTrue = otherCBs.getAlwaysTrue(null);
		Set<RelationalPredicate> thatSometimesTrue = otherCBs
				.getOccasionallyTrue(null);

		// Shape the sets if variables don't match (but cannot purge negated
		// predicates)
		thatAlwaysTrue = shapeFacts(thatAlwaysTrue,
				replacementMap.inverseBidiMap(), negationType, true);
		thatSometimesTrue = shapeFacts(thatSometimesTrue,
				replacementMap.inverseBidiMap(), negationType, true);

		// This conds always true set has to be shaped
		thisAlwaysTrue = shapeFacts(thisAlwaysTrue, replacementMap,
				!cbFact_.isNegated(), false);
		thisSometimesTrue = shapeFacts(thisSometimesTrue, replacementMap,
				!cbFact_.isNegated(), false);

		return thisAlwaysTrue.equals(thatAlwaysTrue)
				&& thisSometimesTrue.equals(thatSometimesTrue);
	}

	/**
	 * Checks if a relation is worth being created based on whether it is
	 * contained in the core beliefs, and also if it's in the type's core
	 * condition beliefs.
	 * 
	 * @param relativeFact
	 *            The relative fact being checked for usefulness.
	 * @param isAlwaysTrue
	 *            If the fact is an always true or never true fact.
	 * @param typeBeliefs
	 *            The beliefs of the type being checked. May be null.
	 * @param typeVarReplacements
	 *            The replacements for the type. May be null.
	 * @return True if the relative fact adds new information and should create
	 *         a relation.
	 */
	private boolean isUsefulRelation(RelationalPredicate relativeFact,
			boolean isAlwaysTrue, TypedBeliefs typeBeliefs,
			Map<RelationalArgument, RelationalArgument> typeVarReplacements) {
		// If the predicate is a never-seen invariant, return false
		if (EnvironmentAgentObservations.getInstance().getNeverSeenInvariants()
				.contains(relativeFact.getFactName()))
			return false;

		// If there are no type beliefs
		if (typeBeliefs == null)
			return true;

		TypedBeliefs coreBeliefs = typedCondBeliefs_.get(cbFact_);
		Collection<RelationalPredicate> coreCollection = (isAlwaysTrue) ? coreBeliefs.alwaysTrue_
				: coreBeliefs.neverTrue_;
		if (coreCollection.contains(relativeFact))
			return false;
		else {
			Collection<RelationalPredicate> typeCollection = (isAlwaysTrue) ? typeBeliefs.alwaysTrue_
					: typeBeliefs.neverTrue_;
			RelationalPredicate modFact = new RelationalPredicate(relativeFact);
			modFact.replaceArguments(typeVarReplacements, false, false);
			if (!typeBeliefs.totalFacts_.contains(modFact))
				return false;
			return !typeCollection.contains(modFact);
		}
	}

	/**
	 * If it is worth noting the type down (the information it presents differs
	 * from the non-typed information).
	 * 
	 * @param type
	 *            The type to check.
	 * @return True if the type has unique type local information.
	 */
	private boolean isUsefulType(RelationalPredicate type) {
		return type == cbFact_
				|| !typedCondBeliefs_.get(cbFact_).alwaysTrue_.contains(type);
	}

	/**
	 * Removes facts from a set of facts which aren't contained in the
	 * keptFacts. The remaining facts are also replaced.
	 * 
	 * @param factSet
	 *            The set of facts to cut down.
	 * @param keptArgs
	 *            The valid arguments to keep (and replace).
	 * @param removeFacts
	 *            If the facts present should be removed at all.
	 * @param replaceArgs
	 *            If the args should also be replaced or just used as a retainer
	 *            set.
	 * @return A cloned set of the factSet with less facts.
	 */
	private Set<RelationalPredicate> shapeFacts(
			Set<RelationalPredicate> factSet,
			Map<RelationalArgument, RelationalArgument> keptArgs,
			boolean removeFacts, boolean replaceArgs) {
		Set<RelationalPredicate> newFactSet = new HashSet<RelationalPredicate>();
		for (RelationalPredicate fact : factSet) {
			boolean keepFact = true;
			for (RelationalArgument arg : fact.getRelationalArguments()) {
				// If the arg isn't anonymous and isn't a valid variable, don't
				// note it.
				if (!arg.isAnonymous() && !keptArgs.containsKey(arg)) {
					keepFact = false;
					break;
				}
			}

			if (keepFact || !removeFacts) {
				fact = new RelationalPredicate(fact);
				if (replaceArgs)
					fact.replaceArguments(keptArgs, true, false);
				newFactSet.add(fact);
			}
		}

		return newFactSet;
	}

	/**
	 * Creates the background knowledge rules generated by the current condition
	 * beliefs. Also creates equivalence rules using the other condition
	 * beliefs.
	 * 
	 * @param conditionBeliefs
	 *            The other condition beliefs.
	 * @param negatedConditionBeliefs
	 *            The other negated condition beliefs.
	 * @param currentKnowledge
	 *            The existing background knowledge.
	 */
	public void createRelationRules(
			Map<String, ConditionBeliefs> conditionBeliefs,
			Map<String, Map<IntegerArray, ConditionBeliefs>> negatedConditionBeliefs,
			NonRedundantBackgroundKnowledge currentKnowledge) {
		// Create relations for each type
		for (RelationalPredicate type : typedCondBeliefs_.keySet()) {
			// Only note types that aren't inherently always true
			if (isUsefulType(type)) {
				TypedBeliefs beliefs = typedCondBeliefs_.get(type);
				TypedBeliefs typeBeliefs = null;
				Map<RelationalArgument, RelationalArgument> typeVarReplacements = null;
				if (type == cbFact_)
					type = null;
				else {
					typeBeliefs = conditionBeliefs.get(type.getFactName())
							.getBaseBeliefs();
					typeVarReplacements = type
							.createVariableTermReplacementMap(false, true);
				}

				// Assert the true facts
				for (RelationalPredicate alwaysTrue : beliefs.alwaysTrue_) {
					// Only create the relation if it unique to the type and
					// isn't already realised by the type itself
					if (isUsefulRelation(alwaysTrue, true, typeBeliefs,
							typeVarReplacements)
							/*&& shouldCreateRelation(cbFact_, alwaysTrue)*/)
						createRelation(type, alwaysTrue, true,
								conditionBeliefs, negatedConditionBeliefs,
								currentKnowledge);
				}

				if (args_ == null) {
					// Assert the false facts
					for (RelationalPredicate neverTrue : beliefs.neverTrue_) {
						// Only create the relation if it is unique to the type
						// and isn't already realised by the type itself.
						if (isUsefulRelation(neverTrue, false, typeBeliefs,
								typeVarReplacements))
							createRelation(type, neverTrue, false,
									conditionBeliefs, negatedConditionBeliefs,
									currentKnowledge);
					}
				}
			}
		}
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
		if (!Arrays.equals(args_, other.args_))
			return false;
		if (condition_ == null) {
			if (other.condition_ != null)
				return false;
		} else if (!condition_.equals(other.condition_))
			return false;
		if (typedCondBeliefs_ == null) {
			if (other.typedCondBeliefs_ != null)
				return false;
		} else if (!typedCondBeliefs_.equals(other.typedCondBeliefs_))
			return false;
		return true;
	}

	public Set<RelationalPredicate> getAlwaysTrue(RelationalPredicate type) {
		if (type != null && typedCondBeliefs_.containsKey(type))
			return typedCondBeliefs_.get(type).alwaysTrue_;
		else
			return typedCondBeliefs_.get(cbFact_).alwaysTrue_;
	}

	public String getCondition() {
		return condition_;
	}

	/**
	 * Gets the condition fact this condition represents.
	 * 
	 * @return A condition fact with the correct arguments and negation.
	 */
	public RelationalPredicate getConditionFact() {
		return cbFact_;
	}

	public Set<RelationalPredicate> getNeverTrue(RelationalPredicate type) {
		if (type != null && typedCondBeliefs_.containsKey(type))
			return typedCondBeliefs_.get(type).neverTrue_;
		else
			return typedCondBeliefs_.get(cbFact_).neverTrue_;
	}

	public Set<RelationalPredicate> getOccasionallyTrue(RelationalPredicate type) {
		if (type != null && typedCondBeliefs_.containsKey(type))
			return typedCondBeliefs_.get(type).occasionallyTrue_;
		else
			return typedCondBeliefs_.get(cbFact_).occasionallyTrue_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(args_);
		result = prime * result
				+ ((condition_ == null) ? 0 : condition_.hashCode());
		result = prime
				* result
				+ ((typedCondBeliefs_ == null) ? 0 : typedCondBeliefs_
						.hashCode());
		return result;
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
	public boolean noteTrueRelativeFacts(
			Collection<RelationalPredicate> trueFacts,
			Collection<RelationalPredicate> untrueFacts, boolean addGeneralities) {
		// For each type fact
		boolean changed = false;
		for (RelationalPredicate typeCond : trueFacts) {
			if (typeCond.equals(cbFact_))
				typeCond = cbFact_;
			if (!typedCondBeliefs_.containsKey(typeCond))
				typedCondBeliefs_.put(typeCond, new TypedBeliefs());
			TypedBeliefs typedBelief = typedCondBeliefs_.get(typeCond);
			changed |= typedBelief.noteTypedFacts(trueFacts, untrueFacts,
					addGeneralities);
		}

		// Note the combined condition beliefs (typeless)
		if (!trueFacts.contains(cbFact_)) {
			if (!typedCondBeliefs_.containsKey(cbFact_))
				typedCondBeliefs_.put(cbFact_, new TypedBeliefs());
			TypedBeliefs coreBelief = typedCondBeliefs_.get(cbFact_);
			changed |= coreBelief.noteTypedFacts(trueFacts, untrueFacts,
					addGeneralities);
		}
		return changed;
	}

	@Override
	public String toString() {
		StringBuffer totalBuffer = new StringBuffer();
		boolean first = true;
		for (RelationalPredicate type : typedCondBeliefs_.keySet()) {
			// Don't include types that are inherently true
			if (isUsefulType(type)) {
				StringBuffer buffer = new StringBuffer(getConditionFact()
						.toString() + ":\n");
				// Replace variables with types
				if (type != cbFact_) {
					String arg = null;
					for (RelationalArgument relArg : type
							.getRelationalArguments()) {
						arg = relArg.toString();
						if (!relArg.isAnonymous())
							break;
					}
					int index = buffer.indexOf(arg);
					buffer.replace(index, index + arg.length(), type.toString());
				}

				TypedBeliefs tb = typedCondBeliefs_.get(type);
				buffer.append("\tAlways True: " + tb.alwaysTrue_.toString()
						+ "\n");
				buffer.append("\tNever True: " + tb.neverTrue_.toString()
						+ "\n");
				buffer.append("\tSometimes True: "
						+ tb.occasionallyTrue_.toString());

				if (!first)
					totalBuffer.append("\n");
				totalBuffer.append(buffer);
				first = false;
			}
		}
		return totalBuffer.toString();
	}

	/**
	 * A small class for holding data regarding individual typed condition
	 * beliefs.
	 * 
	 * @author Sam Sarjant
	 * 
	 */
	private class TypedBeliefs implements Serializable {
		private static final long serialVersionUID = 1196761005343390732L;

		/**
		 * The set of conditions that are always true when this condition is
		 * true.
		 */
		private Set<RelationalPredicate> alwaysTrue_;

		/** A flag to note if the never true values have been initialised yet. */
		private boolean firstState_ = true;

		/**
		 * The set of conditions that are never true when this condition is
		 * true.
		 */
		private Set<RelationalPredicate> neverTrue_;

		/**
		 * The set of conditions that are occasionally true when this condition
		 * is true.
		 */
		private Set<RelationalPredicate> occasionallyTrue_;

		/** The total facts (a union of all three). */
		private Set<RelationalPredicate> totalFacts_;

		public TypedBeliefs() {
			alwaysTrue_ = new HashSet<RelationalPredicate>();
			neverTrue_ = new HashSet<RelationalPredicate>();
			occasionallyTrue_ = new HashSet<RelationalPredicate>();
			totalFacts_ = new HashSet<RelationalPredicate>();
		}

		/**
		 * Adds all other preds not present in always true to the never true
		 * collection if the never true collection is null.
		 * 
		 * @param typedBelief
		 *            The current typed belief this condition belief is for.
		 * @return True if the collection hasn't been initialised yet.
		 */
		private boolean addNeverSeenPreds() {
			if (!firstState_)
				return false;

			// Run through every possible string fact and add those not present
			// in the other lists.
			MultiMap<String, RelationalArgument> possibleTerms = createActionTerms(StateSpec
					.getInstance().getPredicateByName(condition_));

			// Run by the predicates
			Set<String> predicates = new HashSet<String>();
			predicates.addAll(StateSpec.getInstance().getTypePredicates()
					.keySet());
			predicates.addAll(StateSpec.getInstance().getPredicates().keySet());
			for (String pred : predicates) {
				if (!StateSpec.getInstance().getPredicateByName(pred)
						.isNumerical()) {
					// Run by the possible combinations within the predicates.
					for (RelationalPredicate fact : createPossibleFacts(pred,
							possibleTerms)) {
						if (!alwaysTrue_.contains(fact)
								&& !occasionallyTrue_.contains(fact))
							neverTrue_.add(fact);
					}

					// If the same pred, remove the disallowed values from
					// always true as well.
					if (pred.equals(condition_))
						alwaysTrue_.removeAll(disallowed_);
				}
			}
			firstState_ = false;
			return true;
		}

		/**
		 * Generates more general versions of each base fact in the base set.
		 * Each fact must be non-anonymous and not the same as the base fact.
		 * 
		 * @param baseSet
		 *            The set of base facts to generalise.
		 * @return The collection of more general facts for the facts from base
		 *         set.
		 */
		private Collection<RelationalPredicate> generateGenerals(
				Collection<RelationalPredicate> baseSet) {
			Collection<RelationalPredicate> addedGeneralisations = new HashSet<RelationalPredicate>();
			// Run through each fact in the base set.
			for (RelationalPredicate baseFact : baseSet) {
				// Run through each possible binary implementation of the
				// arguments,
				// adding to the added args. (ignoring first and last case)
				for (int b = 1; b < Math.pow(2, baseFact.getArgTypes().length) - 1; b++) {
					RelationalArgument[] factArgs = baseFact
							.getRelationalArguments();
					// Change each argument based on the binary representation.
					boolean changed = false;
					boolean anonymous = true;
					for (int i = 0; i < factArgs.length; i++) {
						// If the argument originally isn't anonymous
						if (!factArgs[i].isAnonymous()) {
							// Make it anonymous
							if ((b & (int) Math.pow(2, i)) == 0) {
								factArgs[i] = RelationalArgument.ANONYMOUS;
								changed = true;
							} else
								anonymous = false;
						}
					}

					if (changed && !anonymous) {
						RelationalPredicate general = new RelationalPredicate(
								baseFact, factArgs);
						addedGeneralisations.add(general);
					}
				}
			}

			return addedGeneralisations;
		}

		/**
		 * Note the typed condition beliefs into their given typed belief
		 * objects.
		 * 
		 * @param trueFacts
		 *            The relative facts that were true under a single
		 *            observation.
		 * @param untrueFacts
		 *            The relative facts that are untrue with regards to the
		 *            true facts. To be filled.
		 * @param addGeneralities
		 *            If more general versions of the main predicate should be
		 *            added.
		 * @return True if the beliefs changed at all.
		 */
		private boolean noteTypedFacts(
				Collection<RelationalPredicate> trueFacts,
				Collection<RelationalPredicate> untrueFacts,
				boolean addGeneralities) {
			// If this is our first state, all true facts are always true, and
			// all others are always false.
			if (firstState_) {
				RelationalPredicate thisFact = StateSpec.getInstance()
						.getPredicateByName(condition_);
				generalities_ = thisFact.createSubFacts(true, true);
				alwaysTrue_.addAll(trueFacts);
				addNeverSeenPreds();
				if (addGeneralities)
					rearrangeGeneralRules();
				if (untrueFacts != null) {
					// Note the untrue facts
					untrueFacts.addAll(neverTrue_);
				}
				totalFacts_.addAll(alwaysTrue_);
				totalFacts_.addAll(neverTrue_);
				return true;
			}
			// If the condition has no relations don't bother updating.
			else if (alwaysTrue_.isEmpty() && neverTrue_.isEmpty())
				return false;

			// Grab local references for clean code.
			Collection<RelationalPredicate> alwaysTrue = alwaysTrue_;
			Collection<RelationalPredicate> occasionallyTrue = occasionallyTrue_;
			Collection<RelationalPredicate> neverTrue = neverTrue_;

			// Clone the collection so no changes are made.
			trueFacts = new HashSet<RelationalPredicate>(trueFacts);

			// Expand the true facts first
			trueFacts.addAll(generateGenerals(trueFacts));

			if (untrueFacts != null) {
				// Note the untrue facts
				untrueFacts.addAll(alwaysTrue);
				untrueFacts.addAll(occasionallyTrue);
				untrueFacts.addAll(neverTrue);
				untrueFacts.removeAll(trueFacts);
				untrueFacts.removeAll(generalities_);
			}

			// Filter the disallowed facts
			trueFacts.removeAll(disallowed_);
			totalFacts_.addAll(trueFacts);

			// Otherwise, perform a number of intersections to determine the
			// sets.
			// Find any predicates present in this trueFacts not in alwaysTrue
			Collection<RelationalPredicate> union = new HashSet<RelationalPredicate>(
					trueFacts);
			union.addAll(alwaysTrue);
			alwaysTrue.retainAll(trueFacts);
			union.removeAll(alwaysTrue);
			if (!union.isEmpty()) {
				neverTrue.removeAll(union);
				int occSize = occasionallyTrue.size();
				occasionallyTrue.addAll(union);
				if (addGeneralities)
					rearrangeGeneralRules();
				if (occasionallyTrue.size() != occSize)
					return true;
				else
					return false;
			}

			return false;
		}

		/**
		 * Rearranges the rules about if there are specific rules in always or
		 * never such that more general version of those rules are also in
		 * always or never.
		 * 
		 * Also ensures that there are self rules that add more general versions
		 * of this condition to the always true set of rules.
		 * 
		 * @param typedBelief
		 *            The current typed belief this condition belief is for.
		 */
		private void rearrangeGeneralRules() {
			Collection<RelationalPredicate> generals = generateGenerals(alwaysTrue_);
			occasionallyTrue_.removeAll(generals);
			neverTrue_.removeAll(generals);
			alwaysTrue_.addAll(generals);

			for (RelationalPredicate general : generalities_) {
				occasionallyTrue_.remove(general);
				neverTrue_.remove(general);
				alwaysTrue_.add(general);
			}
		}

		@Override
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("Always True: " + alwaysTrue_.toString() + "\n");
			buffer.append("Never True: " + neverTrue_.toString() + "\n");
			buffer.append("Sometimes True: " + occasionallyTrue_.toString()
					+ "\n");
			return buffer.toString();
		}
	}
}
