package relationalFramework.agentObservations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import cerrla.RLGGMerger;

import jess.Fact;
import jess.QueryResult;
import jess.Rete;
import jess.ValueVector;

import relationalFramework.ArgumentType;
import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;
import util.MultiMap;

/**
 * A class which records and organises learned background knowledge.
 * 
 * @author Sam Sarjant
 */
public class NonRedundantBackgroundKnowledge implements Serializable {
	private static final long serialVersionUID = -7845690550224825015L;

	private static final String NEG_PREFIX = "neg_";

	private static final String ID_PREFIX = "id";

	private static final String FREE_SYMBOL = "free";

	private static final String ILLEGAL_FACT = "(illegal)";

	private static final String ILLEGAL_QUERY = "illegalQuery";

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
	private transient Rete simplificationEngine_;

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
		rebuildEngine_ = true;
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
		rebuildEngine_ = true;
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
		if (simplificationEngine_ == null) {
			try {
				simplificationEngine_ = new Rete();
				simplificationEngine_.reset();
				rebuildEngine_ = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		Collection<RelationalPredicate> simplified = new HashSet<RelationalPredicate>();
		try {
			// If the engine needs to be rebuilt, rebuild it.
			if (rebuildEngine_)
				rebuildEngine();
			simplificationEngine_.reset();

			// Assert the rules in constant form
			BidiMap variableMap = new DualHashBidiMap();
			for (RelationalPredicate cond : conds) {
				simplificationEngine_.assertString(toConstantFormFull(cond,
						variableMap));
			}

			// simplificationEngine_.eval("(facts)");
			simplificationEngine_.run();
			// simplificationEngine_.eval("(facts)");

			// Check for illegal state
			QueryResult result = simplificationEngine_.runQueryStar(
					ILLEGAL_QUERY, new ValueVector());
			if (result.next())
				return null;

			Collection<Fact> facts = StateSpec
					.extractFacts(simplificationEngine_);
			for (Fact fact : facts) {
				RelationalPredicate rebuiltCond = fromConstantForm(
						fact.toString(), variableMap);
				if (rebuiltCond != null)
					simplified.add(rebuiltCond);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return simplified;
	}

	/**
	 * Converts a constant-form variable string to a relational predicate.
	 * 
	 * @param stringFact
	 *            The string form predicate.
	 * @param variableMap
	 *            The replacement map used for creating the string.
	 * @return An equivalent {@link RelationalPredicate} of stringFact.
	 */
	public RelationalPredicate fromConstantForm(String stringFact,
			BidiMap variableMap) {
		String[] split = StateSpec.splitFact(stringFact);
		if (split[0].equals("initial-fact"))
			return null;

		boolean isNegated = false;
		if (split[0].startsWith(NEG_PREFIX)) {
			isNegated = true;
			split[0] = split[0].substring(NEG_PREFIX.length());
		}

		RelationalPredicate pred = StateSpec.getInstance().getPredicateByName(
				split[0]);
		RelationalArgument[] args = new RelationalArgument[split.length - 1];
		for (int i = 1; i < split.length; i++) {
			if (variableMap.containsValue(split[i]))
				args[i - 1] = (RelationalArgument) variableMap.getKey(split[i]);
			else if (split[i].equals(FREE_SYMBOL)) {
				if (!isNegated
						&& StateSpec.isNumberType(pred.getArgTypes()[i - 1])) {
					args[i - 1] = RLGGMerger.getInstance()
							.createRangeVariable();
					args[i - 1].setFreeVariable(true);
				} else
					args[i - 1] = RelationalArgument.ANONYMOUS;
			} else
				args[i - 1] = new RelationalArgument(split[i]);
		}
		return new RelationalPredicate(pred, args, isNegated);
	}

	/**
	 * Converts a {@link RelationalPredicate} condition to a constant-form
	 * variable string. Also assigns mappings between {@link RelationalArgument}
	 * s and string IDs.
	 * 
	 * @param cond
	 *            The predicate being converted.
	 * @param variableMap
	 *            the map to fill with mappings for changing it back.
	 * @return An equivalent String of cond.
	 */
	public String toConstantFormFull(RelationalPredicate cond,
			BidiMap variableMap) {
		if (variableMap.containsKey(cond))
			return (String) variableMap.get(cond);

		StringBuffer condStr = new StringBuffer("(");
		if (cond.isNegated())
			condStr.append(NEG_PREFIX);
		condStr.append(cond.getFactName());
		for (RelationalArgument arg : cond.getRelationalArguments()) {
			if (variableMap.containsKey(arg))
				condStr.append(" " + variableMap.get(arg));
			else if (arg.isFreeVariable())
				condStr.append(" " + FREE_SYMBOL);
			else {
				String id = ID_PREFIX + variableMap.size();
				variableMap.put(arg, id);
				condStr.append(" " + id);
			}
		}

		condStr.append(")");
		String result = condStr.toString();
		return result;
	}

	/**
	 * Converts a {@link RelationalPredicate} to constant form, but retains the
	 * variables.
	 * 
	 * @param cond
	 *            The predicate being converted.
	 * @param variables
	 *            The existing variables in the rule to create test for.
	 * @return An equivalent string containing variables of cond.
	 */
	public String toConstantFormVariable(RelationalPredicate cond,
			Collection<RelationalArgument> variables) {
		StringBuffer condStr = new StringBuffer("(");
		if (cond.isNegated())
			condStr.append(NEG_PREFIX);
		condStr.append(cond.getFactName());
		for (RelationalArgument arg : cond.getRelationalArguments()) {
			if (arg.isFreeVariable())
				condStr.append(" " + FREE_SYMBOL);
			else if (arg.getArgumentType() == ArgumentType.NUMBER_CONST)
				condStr.append(" " + arg);
			else {
				// Ensure each variable is inequal to free and other variables
				StringBuffer inequality = new StringBuffer();
				if (!variables.contains(arg)) {
					inequality.append("&:(<> " + arg + " free");
					for (RelationalArgument otherArg : variables)
						inequality.append(" " + otherArg);
					inequality.append(")");
				}

				condStr.append(" " + arg + inequality);
				variables.add(arg);
			}
		}

		condStr.append(")");
		String result = condStr.toString();
		return result;
	}

	/**
	 * Rebuilds the Rete network consisting of all the simplification rules.
	 * 
	 * @throws Exception
	 *             Should something go awry...
	 */
	private void rebuildEngine() throws Exception {
		simplificationEngine_.clear();

		// Assert every rule
		int id = 0;
		for (BackgroundKnowledge bckKnow : currentKnowledge_.values()) {
			Collection<RelationalPredicate> preferred = bckKnow
					.getPreferredFacts();
			Collection<RelationalPredicate> nonPreferred = bckKnow
					.getNonPreferredFacts();

			// First assert illegal rule
			String illegalRule = composeIllegalRule(id++, preferred,
					nonPreferred);
			simplificationEngine_.eval(illegalRule);

			// Then assert the rule
			String redundantRule = null;
			if (bckKnow.isEquivalence())
				redundantRule = composeEquivalenceRule(id++, preferred,
						nonPreferred);
			else
				redundantRule = composeImplicationRule(id++, preferred,
						nonPreferred);
			simplificationEngine_.eval(redundantRule);
		}

		// Build the illegal query
		String illegalQuery = "(defquery " + ILLEGAL_QUERY + " " + ILLEGAL_FACT
				+ ")";
		simplificationEngine_.eval(illegalQuery);
		// simplificationEngine_.eval("(watch all)");

		rebuildEngine_ = false;
	}

	/**
	 * Define the implication rule given the preferred and non-preferred facts.
	 * 
	 * @param id
	 *            The id counter for rule names.
	 * @param preferred
	 *            The preferred fact(s).
	 * @param nonPreferred
	 *            The non-preferred fact(s).
	 * @return A JESS implication rule definition string.
	 */
	private String composeImplicationRule(int id,
			Collection<RelationalPredicate> preferred,
			Collection<RelationalPredicate> nonPreferred) {
		StringBuffer ruleName = new StringBuffer();
		StringBuffer buffer = new StringBuffer();
		Collection<RelationalArgument> variables = new HashSet<RelationalArgument>();
		for (RelationalPredicate pred : preferred) {
			String constPred = toConstantFormVariable(pred, variables);
			buffer.append(constPred + " ");
			ruleName.append(shortenPredName(constPred));
		}

		ruleName.append("IMP");

		// Non-preferred
		int numNon = 0;
		for (RelationalPredicate pred : nonPreferred) {
			String constPred = toConstantFormVariable(pred, variables);
			buffer.append("?Y" + numNon++ + " <- " + constPred);
			ruleName.append(shortenPredName(constPred));
		}

		buffer.append(" => (retract");
		for (int i = 0; i < numNon; i++)
			buffer.append(" ?Y" + i);
		buffer.append("))");
		return "(defrule " + ruleName + " (declare (salience 1)) " + buffer;
	}

	/**
	 * Define the equivalence rule given the preferred and non-preferred facts.
	 * 
	 * @param id
	 *            The id counter for rule names.
	 * @param preferred
	 *            The preferred fact(s).
	 * @param nonPreferred
	 *            The non-preferred fact(s).
	 * @return A JESS equivalence rule definition string.
	 */
	private String composeEquivalenceRule(int id,
			Collection<RelationalPredicate> preferred,
			Collection<RelationalPredicate> nonPreferred) {
		StringBuffer suffix = new StringBuffer();
		StringBuffer prefix = new StringBuffer();
		StringBuffer buffer = new StringBuffer();
		Collection<RelationalArgument> variables = new HashSet<RelationalArgument>();
		// Non-preferred
		int numNon = 0;
		for (RelationalPredicate pred : nonPreferred) {
			String constPred = toConstantFormVariable(pred, variables);
			buffer.append("?Y" + numNon++ + " <- " + constPred + " ");
			suffix.append(shortenPredName(constPred));
		}

		buffer.append("=> (retract");
		for (int i = 0; i < numNon; i++)
			buffer.append(" ?Y" + i);
		buffer.append(") (assert");
		// Preferred facts
		for (RelationalPredicate pred : preferred) {
			String constPred = toConstantFormVariable(pred, variables);
			buffer.append(" " + constPred);
			prefix.append(shortenPredName(constPred));
		}
		buffer.append("))");
		return "(defrule " + prefix + "EQV" + suffix
				+ " (declare (salience 2)) " + buffer;
	}

	/**
	 * Define an illegal state rule given the preferred and non-preferred facts.
	 * 
	 * @param id
	 *            The id counter for rule names.
	 * @param preferred
	 *            The preferred fact(s).
	 * @param nonPreferred
	 *            The non-preferred fact(s).
	 * @return A JESS illegal state rule definition string.
	 */
	public String composeIllegalRule(int id,
			Collection<RelationalPredicate> preferred,
			Collection<RelationalPredicate> nonPreferred) {
		StringBuffer ruleName = new StringBuffer();
		StringBuffer buffer = new StringBuffer();
		Collection<RelationalArgument> variables = new HashSet<RelationalArgument>();
		for (RelationalPredicate pred : preferred) {
			String constPred = toConstantFormVariable(pred, variables);
			buffer.append(constPred + " ");
			ruleName.append(shortenPredName(constPred));
		}

		ruleName.append("ILL");

		// Negated non-preferred
		if (nonPreferred.size() > 1)
			buffer.append("(or ");
		boolean first = true;
		for (RelationalPredicate pred : nonPreferred) {
			// Negate the predicate
			if (!first && nonPreferred.size() > 1)
				buffer.append(" ");
			pred.swapNegated();
			String constPred = toConstantFormVariable(pred, variables);
			buffer.append(constPred);
			ruleName.append(shortenPredName(constPred));
			pred.swapNegated();
			first = false;
		}
		if (nonPreferred.size() > 1)
			buffer.append(")");

		buffer.append(" => (assert " + ILLEGAL_FACT + "))");
		return "(defrule " + ruleName + " (declare (salience 3)) " + buffer;
	}

	protected String shortenPredName(String constPred) {
		return constPred.replaceAll("(\\&:\\(.+?\\))|[() ]", "");
	}
}