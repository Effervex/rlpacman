package relationalFramework.agentObservations;

import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import relationalFramework.RelationalPredicate;
import util.MultiMap;

public class NonRedundantBackgroundKnowledge {
	/** The background knowledge rules ordered by what they simplify. */
	private MultiMap<SortedSet<RelationalPredicate>, BackgroundKnowledge> currentKnowledge_;

	/** The rules ordered by the predicates used within them. */
	private MultiMap<String, BackgroundKnowledge> predicateMap_;

	public NonRedundantBackgroundKnowledge() {
		currentKnowledge_ = MultiMap.createListMultiMap();
		predicateMap_ = MultiMap.createSortedSetMultiMap();
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
				List<BackgroundKnowledge> existingRules = currentKnowledge_
						.getList(preferredFacts);
				// If the existing rules are only an equivalence rule, this
				// rule is redundant
				if (existingRules.size() == 1
						&& existingRules.get(0).isEquivalence()) {
					return false;
				}
			} else if (currentKnowledge_.containsKey(nonPreferredFacts)) {

				// Fact already exists in another rule - it may be redundant
				List<BackgroundKnowledge> existingRules = currentKnowledge_
						.getList(nonPreferredFacts);
				if (!bckKnow.isEquivalence()) {
					// Inference rule
					if (existingRules.size() > 1
							|| !existingRules.get(0).isEquivalence()) {
						// Only add inference rules if there aren't any
						// equivalence rules with the same non-preferred facts
						// (so possibly more than one rule).
						addRule(bckKnow, preferredFacts, nonPreferredFacts);
						return true;
					}
				} else {

					if (existingRules.size() > 1
							|| !existingRules.get(0).isEquivalence()) {
						// If the existing rules are inference rules, this rule
						// trumps them all
						removeRules(nonPreferredFacts);
						addRule(bckKnow, preferredFacts, nonPreferredFacts);
						return true;
					} else {
						// Check if this rule's preconditions are more general
						// than the existing equivalence rule's
						if (bckKnow.compareTo(existingRules.get(0)) == -1) {
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
		for (RelationalPredicate fact : bckKnow.getPreferredFacts())
			predicateMap_.putContains(fact.getFactName(), bckKnow);

		// If an equivalence rule, also put the non-preferred side in.
		if (bckKnow.isEquivalence()) {
			for (RelationalPredicate fact : nonPreferredFacts)
				predicateMap_.putContains(fact.getFactName(), bckKnow);
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
}