package relationalFramework.agentObservations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import jess.Fact;
import jess.Rete;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import util.MultiMap;

/**
 * A class which records and organises learned background knowledge.
 * 
 * @author Sam Sarjant
 */
public class NonRedundantBackgroundKnowledge implements Serializable {
	private static final long serialVersionUID = -7845690550224825015L;

	/**
	 * The background knowledge rules ordered by what they simplify (in String
	 * ID).
	 */
	private MultiMap<String, BackgroundKnowledge> currentKnowledge_;

	/** The equivalence post conditions. */
	private Collection<String> equivalencePostConds_;

	/** The rules mapped by the left side predicates (or either if equivalent). */
	private MultiMap<String, BackgroundKnowledge> predicateMap_;

	/** The rules mapped by the right side predicates (or either if equivalent). */
	private MultiMap<String, BackgroundKnowledge> reversePredicateMap_;

	/** The Rete algorithm for simplification. */
	private Rete simplificationEngine_;

	/**
	 * If the simplification rules of the simplification engine should be
	 * rebuilt.
	 */
	private boolean rebuildEngine_ = true;

	public NonRedundantBackgroundKnowledge() {
		currentKnowledge_ = MultiMap.createSortedSetMultiMap();
		equivalencePostConds_ = new HashSet<String>();
		predicateMap_ = MultiMap.createSortedSetMultiMap();
		reversePredicateMap_ = MultiMap.createSortedSetMultiMap();
		// try {
		// simplificationEngine_ = new Rete();
		// simplificationEngine_.reset();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
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
			String[] factStrings = formFactsKeys(preferredFacts,
					nonPreferredFacts);
			// If an implication rule
			if (!bckKnow.isEquivalence()) {
				for (String equivPostString : equivalencePostConds_) {
					// If any equivalent post conditions are in this implication
					// rule, return false
					if (factStrings[0].contains(equivPostString)
							|| factStrings[0].contains(equivPostString))
						return false;
				}

				// Rule isn't present, can add freely
				addRule(bckKnow, preferredFacts, nonPreferredFacts,
						factStrings[1]);
				return true;
			} else {
				// Equivalence rule
				if (currentKnowledge_.containsKey(factStrings[0])) {
					// If the background knowledge rule is an equivalence rule,
					// it may be redundant
					SortedSet<BackgroundKnowledge> existingRules = currentKnowledge_
							.getSortedSet(factStrings[0]);
					// If the existing rules are only an equivalence rule, this
					// rule is redundant
					if (existingRules.size() == 1
							&& existingRules.first().isEquivalence()) {
						return false;
					}
				}
				if (currentKnowledge_.containsKey(factStrings[1])) {
					// Fact already exists in another rule - it may be redundant
					SortedSet<BackgroundKnowledge> existingRules = currentKnowledge_
							.getSortedSet(factStrings[1]);
					if (existingRules.size() > 1
							|| !existingRules.first().isEquivalence()) {
						// If the existing rules are inference rules, this rule
						// trumps them all
						removeRules(factStrings[1]);
						addRule(bckKnow, preferredFacts, nonPreferredFacts,
								factStrings[1]);
						return true;
					} else {
						// Check if this rule's preconditions are more general
						// than the existing equivalence rule's
						if (bckKnow.compareTo(existingRules.first()) == -1) {
							removeRules(factStrings[1]);
							addRule(bckKnow, preferredFacts, nonPreferredFacts,
									factStrings[1]);
							return true;
						}
					}

					return false;
				}
				// Rule isn't present, can add freely
				addRule(bckKnow, preferredFacts, nonPreferredFacts,
						factStrings[1]);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Forms the key for a group of facts.
	 * 
	 * @param facts
	 *            The facts to form the string key for.
	 * @return A string representing the facts in generalised format (variables
	 *         are fully variable).
	 */
	private String[] formFactsKeys(
			SortedSet<RelationalPredicate> preferredFacts,
			SortedSet<RelationalPredicate> nonPreferredFacts) {
		String[] factsKeys = new String[2];
		StringBuffer buffer = new StringBuffer();
		Map<RelationalArgument, Character> replMap = new HashMap<RelationalArgument, Character>();
		replMap.put(RelationalArgument.ANONYMOUS, '?');

		// Replace non-preferred facts first
		int charIndex = 0;
		for (RelationalPredicate fact : nonPreferredFacts) {
			buffer.append("(");
			if (fact.isNegated())
				buffer.append("!");
			buffer.append(fact.getFactName());
			for (RelationalArgument arg : fact.getRelationalArguments()) {
				if (!replMap.containsKey(arg))
					replMap.put(arg, (char) ('A' + charIndex++));
				buffer.append(replMap.get(arg));
			}
			buffer.append(")");
		}
		factsKeys[1] = buffer.toString();

		// Replace preferred facts, using same replacement map
		buffer = new StringBuffer();
		for (RelationalPredicate fact : preferredFacts) {
			buffer.append("(");
			if (fact.isNegated())
				buffer.append("!");
			buffer.append(fact.getFactName());
			for (RelationalArgument arg : fact.getRelationalArguments()) {
				if (!replMap.containsKey(arg))
					replMap.put(arg, (char) ('A' + charIndex++));
				buffer.append(replMap.get(arg));
			}
			buffer.append(")");
		}
		factsKeys[0] = buffer.toString();
		return factsKeys;
	}

	/**
	 * Removes all rules from the given set from the member variables.
	 * 
	 * @param removeKey
	 *            The key of the rules to remove.
	 */
	private void removeRules(String removeKey) {
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
			SortedSet<RelationalPredicate> nonPreferredFacts, String factString) {
		currentKnowledge_.put(factString, bckKnow);
		if (bckKnow.isEquivalence())
			equivalencePostConds_.add(factString);
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

	public Collection<RelationalPredicate> simplify(
			Collection<RelationalPredicate> conds) {
		// If no simplification rules, then no simplification can be performed.
		if (currentKnowledge_.isKeysEmpty())
			return conds;

		Collection<RelationalPredicate> simplified = new ArrayList<RelationalPredicate>();
		try {
			// If the engine needs to be rebuilt, rebuild it.
			if (rebuildEngine_)
				rebuildEngine();

			// Assert the rules in constant form
			for (RelationalPredicate cond : conds) {
				simplificationEngine_.assertString(toConstantForm(cond));
			}

			simplificationEngine_.run();

			Collection<Fact> facts = StateSpec
					.extractFacts(simplificationEngine_);
			for (Fact fact : facts) {
				RelationalPredicate rebuiltCond = fromConstantForm(fact
						.toString());
				simplified.add(rebuiltCond);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return simplified;
	}

	private RelationalPredicate fromConstantForm(String string) {
		// TODO Auto-generated method stub
		return null;
	}

	private String toConstantForm(RelationalPredicate cond) {
		String condStr = cond.toString();
		// TODO Auto-generated method stub
		// Need to deal with: FreeVars, Variables, Ranges, Constants, Negation
		return condStr;
	}

	private void rebuildEngine() {
		// TODO Auto-generated method stub

	}
}