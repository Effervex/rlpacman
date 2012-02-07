package relationalFramework.agentObservations;

import java.io.Serializable;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import relationalFramework.RelationalPredicate;
import util.MultiMap;

/**
 * A class which records and organises learned background knowledge.
 * 
 * @author Sam Sarjant
 */
public class NonRedundantBackgroundKnowledge implements Serializable {
	private static final long serialVersionUID = -7845690550224825015L;

	/** The background knowledge rules ordered by what they simplify. */
	private MultiMap<SortedSet<RelationalPredicate>, BackgroundKnowledge> currentKnowledge_;

	/** The rules mapped by the left side predicates (or either if equivalent). */
	private MultiMap<String, BackgroundKnowledge> predicateMap_;

	/** The rules mapped by the right side predicates (or either if equivalent). */
	private MultiMap<String, BackgroundKnowledge> reversePredicateMap_;

	public NonRedundantBackgroundKnowledge() {
		currentKnowledge_ = MultiMap.createSortedSetMultiMap();
		predicateMap_ = MultiMap.createSortedSetMultiMap();
		reversePredicateMap_ = MultiMap.createSortedSetMultiMap();
	}

	/**
	 * Adds background knowledge to the current knowledge set if it represents a
	 * unique. non-redundant rule. If the knowledge is able to be added, it may
	 * result in other knowledge being removed.
	 * 
	 * @param bckKnow
	 *            The knowledge to add.
	 * @return True if the knowledge was added, false otherwise.
	 */
	public boolean addBackgroundKnowledge(BackgroundKnowledge bckKnow) {
		try {
			SortedSet<RelationalPredicate> nonPreferredFacts = new TreeSet<RelationalPredicate>(
					bckKnow.getNonPreferredFacts());
			SortedSet<RelationalPredicate> preferredFacts = new TreeSet<RelationalPredicate>(
					bckKnow.getPreferredFacts());
			if (bckKnow.isEquivalence()
					&& currentKnowledge_.containsKey(preferredFacts)) {
				// If the background knowledge rule is an equivalence rule, it
				// may be redundant
				SortedSet<BackgroundKnowledge> existingRules = currentKnowledge_
						.getSortedSet(preferredFacts);
				// If the existing rules are only an equivalence rule, this
				// rule is redundant
				if (existingRules.size() == 1
						&& existingRules.first().isEquivalence()) {
					return false;
				}
			} else if (currentKnowledge_.containsKey(nonPreferredFacts)) {

				// Fact already exists in another rule - it may be redundant
				SortedSet<BackgroundKnowledge> existingRules = currentKnowledge_
						.getSortedSet(nonPreferredFacts);
				if (!bckKnow.isEquivalence()) {
					// Inference rule
					if (existingRules.size() > 1
							|| !existingRules.first().isEquivalence()) {
						// Only add inference rules if there aren't any
						// equivalence rules with the same non-preferred facts
						// (so possibly more than one rule).
						addRule(bckKnow, preferredFacts, nonPreferredFacts);
						return true;
					}
				} else {

					if (existingRules.size() > 1
							|| !existingRules.first().isEquivalence()) {
						// If the existing rules are inference rules, this rule
						// trumps them all
						removeRules(nonPreferredFacts);
						addRule(bckKnow, preferredFacts, nonPreferredFacts);
						return true;
					} else {
						// Check if this rule's preconditions are more general
						// than the existing equivalence rule's
						if (bckKnow.compareTo(existingRules.first()) == -1) {
							removeRules(nonPreferredFacts);
							addRule(bckKnow, preferredFacts, nonPreferredFacts);
							return true;
						}
					}
				}

				return false;
			}

			// Rule isn't present, can add freely
			addRule(bckKnow, preferredFacts, nonPreferredFacts);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Removes all rules from the given set from the member variables.
	 * 
	 * @param removeKey
	 *            The key of the rules to remove.
	 */
	private void removeRules(SortedSet<RelationalPredicate> removeKey) {
		Collection<BackgroundKnowledge> removedRules = currentKnowledge_
				.remove(removeKey);
		for (BackgroundKnowledge removed : removedRules) {
			for (RelationalPredicate fact : removed.getPreferredFacts())
				predicateMap_.get(fact.getFactName()).remove(removed);

			// If an equivalence rule, also put the non-preferred side in.
			if (removed.isEquivalence()) {
				for (RelationalPredicate fact : removed.getNonPreferredFacts())
					predicateMap_.get(fact.getFactName()).remove(removed);
			}
		}
	}

	/**
	 * Adds the rule to the collections.
	 * 
	 * @param bckKnow
	 *            The rule to add.
	 * @param preferredFacts
	 *            The preferred facts of the rule.
	 * @param nonPreferredFacts
	 *            The non-preferred facts of the rule.
	 */
	private void addRule(BackgroundKnowledge bckKnow,
			SortedSet<RelationalPredicate> preferredFacts,
			SortedSet<RelationalPredicate> nonPreferredFacts) {
		currentKnowledge_.put(nonPreferredFacts, bckKnow);
		for (RelationalPredicate fact : preferredFacts) {
			predicateMap_.putContains(fact.getFactName(), bckKnow);
			if (bckKnow.isEquivalence())
				reversePredicateMap_.put(fact.getFactName(), bckKnow);
		}
		for (RelationalPredicate fact : nonPreferredFacts) {
			reversePredicateMap_.putContains(fact.getFactName(), bckKnow);
			if (bckKnow.isEquivalence())
				predicateMap_.put(fact.getFactName(), bckKnow);
		}
	}

	/**
	 * Gets all rules in the current knowledge.
	 * 
	 * @return The current rules.
	 */
	public Collection<BackgroundKnowledge> getAllBackgroundKnowledge() {
		return currentKnowledge_.values();
	}

	public MultiMap<String, BackgroundKnowledge> getPredicateMappedRules() {
		return predicateMap_;
	}

	public MultiMap<String, BackgroundKnowledge> getReversePredicateMappedRules() {
		return reversePredicateMap_;
	}
}