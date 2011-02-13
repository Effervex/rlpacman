package relationalFramework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import relationalFramework.agentObservations.BackgroundKnowledge;

import jess.Fact;
import jess.QueryResult;
import jess.Rete;
import jess.ValueVector;

/**
 * A class to outline the specifications of the environment.
 * 
 * @author Sam Sarjant
 */
public abstract class StateSpec {
	/** The infers symbol. */
	public static final String INFERS_ACTION = "=>";

	/** The equivalent symbol. */
	public static final String EQUIVALENT_RULE = "<=>";

	/** The anonymous variable symbol. */
	public static final String ANONYMOUS = "?";

	/** The suffix for an action precondition. */
	public static final String ACTION_PRECOND_SUFFIX = "PreCond";

	/** The valid actions predicate name. */
	public static final String VALID_ACTIONS = "validActions";

	/** The goal query name */
	public static final String GOAL_QUERY = "isGoal";

	/** The policy rule prefix. */
	public static final String POLICY_QUERY_PREFIX = "polRule";

	/** The between a range of values function name. */
	public static final String BETWEEN_RANGE = "betweenRange";

	/** The replacement value for unnecessary facts. */
	private static final String ACTION_TERM_REPL = "__TYPE__";

	/** The StringFact definition for the test predicate. */
	private static final StringFact TEST_DEFINITION = new StringFact("test",
			new String[] { "testArgs" });

	/** The singleton instance. */
	private static StateSpec instance_;

	/** The environment package name. */
	private String environment_;

	/** The prerequisites of the rules and their structure. */
	private Map<String, StringFact> predicates_;

	/** The type predicates, only used implicitly. */
	private Map<String, StringFact> typePredicates_;

	/** The type hierarchy. */
	private Map<String, ParentChildren> typeHierarchy_;

	/** The actions of the rules and their structure. */
	private Map<String, StringFact> actions_;

	/** The background rules and illegalities of the state. */
	private Map<String, BackgroundKnowledge> backgroundRules_;

	/** The number of simultaneous actions per step to take. */
	private int actionNum_;

	/** The terms present in an action's precondition. */
	private MultiMap<String, String> actionPreconditions_;

	/** The state the agent must reach to successfully end the episode. */
	private String goalState_;

	/** The constants found within the goal. */
	private List<String> constants_;

	/** The parameter of the environment. */
	protected String envParameter_;

	/** The hand coded policy for the goal. */
	private Policy handCodedPolicy_;

	/** The LogicFactory for the experiment. */
	private Rete rete_;

	/** A RegExp for filtering out unnecessary facts from a rule. */
	private String unnecessaries_;

	/** The mapping for rules to queries in the Rete object. */
	private Map<GuidedRule, String> queryNames_;

	/** The count value for the query names. */
	private int queryCount_;

	/**
	 * Extract the terms from a set of facts.
	 * 
	 * @param facts
	 *            The facts to extract the terms from.
	 * @return The terms in a list.
	 */
	private List<String> extractTerms(String facts) {
		List<String> terms = new ArrayList<String>();
		// ?X ?x ?_4 ?g4 ?#...
		Pattern p = Pattern.compile("\\?[A-Za-z$*=+/<>_?#.][\\w$*=+/<>_?#.]*");
		Matcher m = p.matcher(facts);
		while (m.find()) {
			// Removing the '?'
			String term = m.group().substring(1);
			if (!terms.contains(term))
				terms.add(term);
		}

		return terms;
	}

	/**
	 * Forms the unnecessaries regexp replacement string to only replace
	 * unnecessary type facts that are present in the action.
	 * 
	 * @param terms
	 *            The action terms of the rule.
	 * @return A regexp modified to match the rule.
	 */
	private String formRuleSpecificRegexp(String[] terms) {
		StringBuffer actionRepl = new StringBuffer("(");
		boolean first = true;
		for (String term : terms) {
			if (!first)
				actionRepl.append("|");
			actionRepl.append("(" + Pattern.quote(term) + ")");
			first = false;
		}
		actionRepl.append(")");
		return unnecessaries_.replace(ACTION_TERM_REPL, actionRepl.toString());
	}

	/**
	 * Forms the unnecessary facts regexp string.
	 * 
	 * @param types
	 *            The types that need not be in rules.
	 * @return The regexp string.
	 */
	private String formUnnecessaryString(Collection<String> types) {
		StringBuffer buffer = new StringBuffer("((\\(test \\(<> .+?\\)\\))");
		for (String type : types)
			buffer.append("|(\\(" + type + " " + ACTION_TERM_REPL + "\\))");
		buffer.append(")( |$)");
		return buffer.toString();
	}

	/**
	 * The constructor for a state specification.
	 */
	private final void initialise() {
		try {
			rete_ = new Rete();

			environment_ = this.getClass().getPackage().getName();

			// Type predicates and their hierarchy background rules.
			typePredicates_ = new HashMap<String, StringFact>();
			Map<String, String> typeAssertions = new HashMap<String, String>();
			Map<String, String> typeParents = initialiseTypePredicateTemplates();
			typeHierarchy_ = new HashMap<String, ParentChildren>();
			for (String type : typeParents.keySet()) {
				typePredicates_.put(type, new StringFact(type,
						new String[] { type }));
				defineTemplate(type, rete_);

				// Define background knowledge rule
				if (typeParents.get(type) != null) {
					String assertion = "(" + type + " ?X) " + INFERS_ACTION
							+ " (assert (" + typeParents.get(type) + " ?X))";
					typeAssertions.put(type + "ParentRule", assertion);

					// Record the parent children relationship
					ParentChildren thisPC = (typeHierarchy_.containsKey(type)) ? typeHierarchy_
							.get(type)
							: new ParentChildren();
					String parentType = typeParents.get(type);
					thisPC.setParent(parentType);
					typeHierarchy_.put(type, thisPC);

					ParentChildren parentPC = (typeHierarchy_
							.containsKey(parentType)) ? typeHierarchy_
							.get(parentType) : new ParentChildren();
					parentPC.addChild(type);
					typeHierarchy_.put(parentType, parentPC);
				}
			}
			unnecessaries_ = formUnnecessaryString(typePredicates_.keySet());

			// Main predicates
			predicates_ = new HashMap<String, StringFact>();
			for (StringFact pred : initialisePredicateTemplates()) {
				predicates_.put(pred.getFactName(), pred);
				defineTemplate(pred.getFactName(), rete_);
			}

			// Set up the valid actions template
			StringBuffer actBuf = new StringBuffer("(deftemplate "
					+ VALID_ACTIONS);
			// Actions
			actions_ = new HashMap<String, StringFact>();
			for (StringFact action : initialiseActionTemplates()) {
				actions_.put(action.getFactName(), action);
				defineTemplate(action.getFactName(), rete_);
				actBuf.append(" (multislot " + action.getFactName() + ")");
			}
			actBuf.append(")");
			rete_.eval(actBuf.toString());
			actionNum_ = initialiseActionsPerStep();

			// Initialise the background knowledge rules

			// Type hierarchy rules
			backgroundRules_ = new HashMap<String, BackgroundKnowledge>();
			for (String name : typeAssertions.keySet())
				backgroundRules_.put(name, new BackgroundKnowledge(
						typeAssertions.get(name), true));

			// State Spec rules
			backgroundRules_.putAll(initialiseBackgroundKnowledge());
			for (String ruleNames : backgroundRules_.keySet()) {
				if (backgroundRules_.get(ruleNames).assertInJess())
					rete_.eval("(defrule " + ruleNames + " "
							+ backgroundRules_.get(ruleNames) + ")");
			}

			// Initialise the betweenRange function
			rete_.eval("(deffunction " + BETWEEN_RANGE + " (?val ?low ?high) "
					+ "(if (and (>= ?val ?low) (<= ?val ?high)) then "
					+ "return TRUE))");

			// Initialise the goal state rules
			constants_ = new ArrayList<String>();
			goalState_ = initialiseGoalState(constants_);
			rete_.eval("(deftemplate goal (slot goalMet))");
			rete_.eval("(defrule goalState " + goalState_
					+ " => (assert (goal (goalMet TRUE))))");
			// Initialise the goal checking query
			rete_.eval("(defquery " + GOAL_QUERY + " (goal (goalMet ?)))");

			// Initialise the queries for determining action preconditions
			Map<String, String> purePreConds = initialiseActionPreconditions();
			actionPreconditions_ = new MultiMap<String, String>();
			for (String action : purePreConds.keySet()) {
				String query = "(defquery " + action + ACTION_PRECOND_SUFFIX
						+ " " + purePreConds.get(action) + ")";
				rete_.eval(query);
				actionPreconditions_.putCollection(action,
						extractTerms(purePreConds.get(action)));
			}

			// Initialise the optimal policy
			handCodedPolicy_ = initialiseOptimalPolicy();

			queryNames_ = new HashMap<GuidedRule, String>();
			queryCount_ = 0;

			Unification.getInstance().resetRangeIndex();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initialises the rules for finding valid actions.
	 * 
	 * @return A map of actions and the pure preconditions to be valid.
	 */
	protected abstract Map<String, String> initialiseActionPreconditions();

	/**
	 * Initialises the number of actions to take per time step.
	 * 
	 * @return The number of actions to take per step.
	 */
	protected abstract int initialiseActionsPerStep();

	/**
	 * Initialises the state actions.
	 * 
	 * @return The list of guided actions.
	 */
	protected abstract Collection<StringFact> initialiseActionTemplates();

	/**
	 * Initialises the background knowledge.
	 * 
	 * @return A mapping of rule names to the pure rules themselves (just pre =>
	 *         post).
	 */
	protected abstract Map<String, BackgroundKnowledge> initialiseBackgroundKnowledge();

	/**
	 * Initialises the goal state.
	 * 
	 * @param constants
	 *            The constants that are used in the goal. To be filled.
	 * @return The minimal state that is true when the goal is satisfied.
	 */
	protected abstract String initialiseGoalState(List<String> constants);

	/**
	 * Initialises the optimal policy for the goal.
	 * 
	 * @param factory
	 *            The factory for creating rules.
	 * 
	 * @return The policy that solves the goal in optimal time.
	 */
	protected abstract Policy initialiseOptimalPolicy();

	/**
	 * Initialises the state predicates.
	 * 
	 * @return The list of guided predicate names.
	 */
	protected abstract Collection<StringFact> initialisePredicateTemplates();

	/**
	 * Initialises the state type predicates.
	 * 
	 * @param rete
	 *            The rete object.
	 * @return A map of types, where the possibly null value corresponds to the
	 *         keys parent in the type hierarchy.
	 */
	protected abstract Map<String, String> initialiseTypePredicateTemplates();

	/**
	 * Adds type predicates to the rule from a fact if they are not already
	 * there.
	 * 
	 * @param fact
	 *            The fact.
	 * @param termIndex
	 *            The index where the first term is found.
	 */
	public Collection<StringFact> createTypeConds(StringFact fact) {
		Collection<StringFact> typeConds = new HashSet<StringFact>();
		// If the term itself is a type pred, return.
		if (isTypePredicate(fact.getFactName())) {
			return typeConds;
		}

		String[] types = fact.getArgTypes();
		for (int i = 0; i < types.length; i++) {
			if (!fact.getArguments()[i].equals("?")) {
				StringFact typeFact = typePredicates_.get(types[i]);
				if (typeFact != null) {
					typeConds.add(new StringFact(typeFact, new String[] { fact
							.getArguments()[i] }));
				}
			}
		}
		return typeConds;
	}

	/**
	 * Creates a template for a fact using the given arguments and asserts it to
	 * the rete object. Returns the name of the fact template.
	 * 
	 * @param factName
	 *            The name of the fact template.
	 * @param rete
	 *            The rete object being asserted to.
	 * @return The name of the fact template.
	 */
	public String defineTemplate(String factName, Rete rete) {
		try {
			rete
					.eval("(deftemplate " + factName
							+ " (declare (ordered TRUE)))");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return factName;
	}

	/**
	 * Creates a string representation of a rule, in a light, easy-to-read, and
	 * parsable format.
	 * 
	 * @param rule
	 *            The rule begin output.
	 * @return A light, parsable representation of the rule.
	 */
	public final String encodeRule(GuidedRule rule) {
		String ruleSpecificUnnecessaries = formRuleSpecificRegexp(rule
				.getActionTerms());
		String replacement = rule.toString().replaceAll(
				ruleSpecificUnnecessaries, "");
		if (replacement.split(INFERS_ACTION)[0].isEmpty())
			return rule.toString();

		// If there are parameters, swap them in
		List<String> parameters = rule.getParameters();
		if (parameters != null) {
			List<String> queryParameters = rule.getQueryParameters();
			for (int i = 0; i < parameters.size(); i++) {
				String temp = " " + Pattern.quote(queryParameters.get(i))
						+ "(?=( |\\)))";
				replacement = replacement.replaceAll(temp, " "
						+ parameters.get(i));
			}
		}
		return replacement;
	}

	/**
	 * Generates the valid actions for the state into the state for the agent to
	 * use. Actions are given in string format of just the arguments.
	 * 
	 * @param state
	 *            The state from which the actions are generated.
	 * @return A multimap with each valid action predicate as the key and the
	 *         values as the arguments.
	 */
	public final MultiMap<String, String> generateValidActions(Rete state) {
		MultiMap<String, String> validActions = new MultiMap<String, String>();

		try {
			for (String action : actions_.keySet()) {
				QueryResult result = state.runQueryStar(action
						+ ACTION_PRECOND_SUFFIX, new ValueVector());
				while (result.next()) {
					StringBuffer factBuffer = new StringBuffer();
					boolean first = true;
					for (String term : actionPreconditions_.get(action)) {
						if (!first)
							factBuffer.append(" ");
						factBuffer.append(result.getSymbol(term));
						first = false;
					}
					validActions.put(action, factBuffer.toString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return validActions;
	}

	public Map<String, StringFact> getActions() {
		return actions_;
	}

	public Collection<BackgroundKnowledge> getBackgroundKnowledgeConditions() {
		return backgroundRules_.values();
	}

	public List<String> getConstants() {
		return constants_;
	}

	public String getEnvironmentName() {
		return environment_;
	}

	public String getGoalState() {
		return goalState_;
	}

	/**
	 * Gets the optimal policy for the problem. Or at least gets a good policy.
	 * 
	 * @return The policy that is optimal.
	 */
	public Policy getHandCodedPolicy() {
		return handCodedPolicy_;
	}

	public int getNumReturnedActions() {
		return actionNum_;
	}

	/**
	 * Gets the parent, child, and itself types of this given type, so unless
	 * this type is alone, the returned collection will be greater than size 1.
	 * 
	 * @param type
	 *            The type to get the lineage for, from parent to child (not
	 *            necessarily in order).
	 * @param lineage
	 *            A to-be-filled collection of all types that are in the type
	 *            lineage, be they parent, grandparent or child, grandchild,
	 *            etc.
	 */
	public void getTypeLineage(String type, Collection<String> lineage) {
		// Add itself
		lineage.add(type);

		ParentChildren typePC = typeHierarchy_.get(type);
		if (typePC != null) {
			// Add children
			recurseChildrenTypes(typePC, lineage);

			// Add parents
			recurseParentTypes(typePC, lineage);
		}
	}

	/**
	 * Recurse through the parent types.
	 * 
	 * @param typePC
	 *            The type Parent-Child relationship.
	 * @param lineage
	 *            The lineage to add to.
	 */
	private void recurseChildrenTypes(ParentChildren typePC,
			Collection<String> lineage) {
		if (typePC != null) {
			Collection<String> children = typePC.getChildren();
			if (children != null) {
				for (String child : children) {
					lineage.add(child);
					recurseChildrenTypes(typeHierarchy_.get(child), lineage);
				}
			}
		}
	}

	/**
	 * Recurse through the parent types.
	 * 
	 * @param typePC
	 *            The type Parent-Child relationship.
	 * @param lineage
	 *            The lineage to add to.
	 */
	private void recurseParentTypes(ParentChildren typePC,
			Collection<String> lineage) {
		if (typePC != null) {
			String parent = typePC.getParent();
			if (parent != null) {
				lineage.add(parent);
				recurseParentTypes(typeHierarchy_.get(parent), lineage);
			}
		}
	}

	public Map<String, StringFact> getPredicates() {
		return predicates_;
	}

	public Rete getRete() {
		return rete_;
	}

	/**
	 * Gets or creates a rule query for a guided rule.
	 * 
	 * @param gr
	 *            The guided rule associated with a query.
	 * @return The query from the rule (possibly newly created).
	 */
	public String getRuleQuery(GuidedRule gr) {
		String result = queryNames_.get(gr);
		if (result == null) {
			try {
				result = POLICY_QUERY_PREFIX + queryCount_++;
				// If the rule has parameters, declare them as variables.
				if (gr.getQueryParameters() == null) {
					rete_.eval("(defquery " + result + " "
							+ gr.getStringConditions() + ")");
				} else {
					StringBuffer declares = new StringBuffer(
							"(declare (variables");

					for (String param : gr.getQueryParameters()) {
						declares.append(" " + param);
					}
					declares.append("))");

					rete_.eval("(defquery " + result + " "
							+ declares.toString() + " "
							+ gr.getStringConditions() + ")");
				}
				queryNames_.put(gr, result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return result;
	}

	/**
	 * Gets a string fact by the fact name.
	 * 
	 * @param factName
	 *            The fact name.
	 * @return The StringFact corresponding to the fact name, or null if
	 *         non-existant.
	 */
	public StringFact getStringFact(String factName) {
		if (predicates_.containsKey(factName))
			return predicates_.get(factName);
		if (actions_.containsKey(factName))
			return actions_.get(factName);
		if (typePredicates_.containsKey(factName))
			return typePredicates_.get(factName);
		if (factName.equals("test"))
			return TEST_DEFINITION;
		return null;
	}

	public Map<String, StringFact> getTypePredicates() {
		return typePredicates_;
	}

	/**
	 * Checks if the current state is a goal state by looking for the terminal
	 * fact.
	 * 
	 * @param rete
	 *            The state.
	 * @return True if we're in the goal state, false otherwise.
	 */
	public boolean isGoal(Rete rete) {
		try {
			QueryResult result = rete.runQueryStar(StateSpec.GOAL_QUERY,
					new ValueVector());
			if (result.next())
				return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean isTypePredicate(String predicate) {
		if (typePredicates_.containsKey(predicate))
			return true;
		return false;
	}

	/**
	 * Checks if the predicate is useful (not action or inequal).
	 * 
	 * @param predicate
	 *            The predicate being checked.
	 * @return True if the predicate is useful, false if it is action, inequal
	 *         or otherwise.
	 */
	public boolean isUsefulPredicate(String predicate) {
		if (predicate.equals("test"))
			return false;
		if (predicate.equals(VALID_ACTIONS))
			return false;
		if (predicate.equals("initial-fact"))
			return false;
		return true;
	}

	/**
	 * This should really only be used by the test class.
	 * 
	 * @param constants
	 *            The constants set in the state.
	 */
	public void setConstants(List<String> constants) {
		constants_ = constants;
	}

	@Override
	public String toString() {
		return "StateSpec";
	}

	/**
	 * Adds an array element to a list of arrays if the array is not already in
	 * there.
	 * 
	 * @param addTo
	 *            The list to add to.
	 * @param addFrom
	 *            The list to add from, if the terms are not contained.
	 */
	public static boolean addContainsArray(List<String[]> arrayList,
			String[] array) {
		Iterator<String[]> iter = arrayList.iterator();
		while (iter.hasNext()) {
			if (Arrays.equals(iter.next(), array))
				return false;
		}

		arrayList.add(array);
		return true;
	}

	/**
	 * Basic method which checks if an array contains a string.
	 * 
	 * @param array
	 *            The array to search.
	 * @param str
	 *            The string to search for.
	 * @return True if the string is in the array, false otherwise.
	 */
	public static boolean arrayContains(String[] array, String str) {
		for (String arg : array)
			if (arg.equals(str))
				return true;
		return false;
	}

	/**
	 * Creates the inequals tests from the terms given. Note anonymous terms are
	 * special in that they aren't inequal to one-another.
	 * 
	 * @return The string for detailing inequality '(test (<> ?X a b ?Y ?_0))
	 *         (test (<> ?Y a ...))'
	 */
	public static Collection<StringFact> createInequalityTests(
			Collection<String> variableTerms, Collection<String> constantTerms) {
		Collection<StringFact> tests = new ArrayList<StringFact>();

		// Run through each variable term
		for (Iterator<String> termIter = variableTerms.iterator(); termIter
				.hasNext();) {
			String varTermA = termIter.next();
			StringBuffer buffer = new StringBuffer("(<> " + varTermA);
			boolean isValid = false;
			// Adding other variable terms
			for (Iterator<String> subIter = variableTerms.iterator(); subIter
					.hasNext();) {
				String varTermB = subIter.next();
				if (varTermB != varTermA) {
					isValid = true;
					buffer.append(" " + varTermB);
				} else
					break;
			}

			// Adding constant terms
			for (String constant : constantTerms) {
				isValid = true;
				buffer.append(" " + constant);
			}

			if (isValid) {
				buffer.append(")");
				tests.add(new StringFact(TEST_DEFINITION, new String[] { buffer
						.toString() }));
			}
		}
		return tests;
	}

	/**
	 * Extracts a collection of facts from the state.
	 * 
	 * @param state
	 *            The rete state to extract facts from.
	 * @return A collection of facts in the state.
	 */
	@SuppressWarnings("unchecked")
	public static Collection<Fact> extractFacts(Rete state) {
		Collection<Fact> facts = new ArrayList<Fact>();
		for (Iterator<Fact> factIter = state.listFacts(); factIter.hasNext();) {
			facts.add(factIter.next());
		}

		return facts;
	}

	/**
	 * Gets the singleton instance of the state spec.
	 * 
	 * @return The instance.
	 */
	public static StateSpec getInstance() {
		return instance_;
	}

	public static StateSpec initInstance(String classPrefix) {
		try {
			instance_ = (StateSpec) Class.forName(
					classPrefix + StateSpec.class.getSimpleName())
					.newInstance();
			instance_.initialise();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return instance_;
	}

	/**
	 * Checks if this String can be parsed into a double.
	 * 
	 * @param unityValue
	 *            The value being checked.
	 * @return True if the String can be put into a numerical form.
	 */
	public static boolean isNumber(String string) {
		try {
			Double.parseDouble(string);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Checks if a type name represents a number.
	 * 
	 * @param type
	 *            The type name being checked.
	 * @return True if it represents a number, false otherwise.
	 */
	public static boolean isNumberType(String type) {
		try {
			Number.valueOf(type);
			return true;
		} catch (IllegalArgumentException iae) {
		}
		return false;
	}

	/**
	 * Reforms a fact back together again from a split array.
	 * 
	 * @param factSplit
	 *            The split fact.
	 * @return A string representation of the fact.
	 */
	public static String reformFact(String[] factSplit) {
		StringBuffer buffer = new StringBuffer("(" + factSplit[0]);
		// Reforming negation
		if (factSplit[0].equals("not"))
			return buffer.toString()
					+ " "
					+ reformFact(Arrays.copyOfRange(factSplit, 1,
							factSplit.length)) + ")";

		for (int i = 1; i < factSplit.length; i++) {
			buffer.append(" " + factSplit[i]);
		}
		buffer.append(")");
		return buffer.toString();
	}

	public static void reinitInstance() {
		try {
			instance_.rete_.clear();
			instance_.initialise();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reinits the instance with a set goal.
	 * 
	 * @param goalString
	 *            The new goal string.
	 */
	public static void reinitInstance(String goalString) {
		try {
			instance_.rete_.clear();
			instance_.envParameter_ = goalString;
			instance_.initialise();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Splits a fact up into an array format, with the first index the fact
	 * name.
	 * 
	 * @param fact
	 *            The fact being split.
	 * @return A string array of the facts.
	 */
	public static String[] splitFact(String fact) {
		Pattern p = Pattern
				.compile("(?:[^()\\s:]|(?:\\?.+&:\\(.+?\\)))+(?= |\\)|$)");
		Matcher m = p.matcher(fact);
		ArrayList<String> matches = new ArrayList<String>();
		while (m.find())
			matches.add(m.group());

		return matches.toArray(new String[matches.size()]);
	}

	/**
	 * Transforms a String fact into a StringFact.
	 * 
	 * @param fact
	 *            The String fact.
	 * @return A StringFact version of the fact.
	 */
	public static StringFact toStringFact(String fact) {
		String[] condSplit = StateSpec.splitFact(fact);
		int offset = 1;
		boolean negated = false;
		// Negated case
		if (condSplit[0].equals("not")) {
			offset = 2;
			negated = true;
		}
		// Test case
		if (condSplit[0].equals("test")) {
			StringBuffer testedGroup = new StringBuffer("(");
			boolean first = true;
			for (int i = 1; i < condSplit.length; i++) {
				if (!first)
					testedGroup.append(" ");
				testedGroup.append(condSplit[i]);
				first = false;
			}
			testedGroup.append(")");
			condSplit = new String[2];
			condSplit[0] = "test";
			condSplit[1] = testedGroup.toString();
		}
		String[] arguments = new String[condSplit.length - offset];
		System.arraycopy(condSplit, offset, arguments, 0, arguments.length);
		return new StringFact(StateSpec.getInstance().getStringFact(
				condSplit[offset - 1]), arguments, negated);
	}

	/**
	 * A basic class which holds parent-child data for a given type predicate.
	 * 
	 * @author Sam Sarjant
	 */
	private class ParentChildren {
		private String parent_;
		private Collection<String> children_;

		public String getParent() {
			return parent_;
		}

		public void setParent(String parent) {
			parent_ = parent;
		}

		public Collection<String> getChildren() {
			return children_;
		}

		public void addChild(String child) {
			if (children_ == null)
				children_ = new HashSet<String>();
			children_.add(child);
		}
	}
}
