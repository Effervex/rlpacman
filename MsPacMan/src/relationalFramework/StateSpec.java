package relationalFramework;

import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

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

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import cerrla.Unification;

import relationalFramework.agentObservations.BackgroundKnowledge;
import relationalFramework.agentObservations.RangeContext;
import util.ArgumentComparator;
import util.MultiMap;
import util.Pair;

import jess.Fact;
import jess.JessException;
import jess.QueryResult;
import jess.Rete;
import jess.ValueVector;

/**
 * A class to outline the specifications of the environment.
 * 
 * @author Sam Sarjant
 */
public abstract class StateSpec {
	/** The singleton instance. */
	private static StateSpec instance_;

	/** The name of the linear interpolation function. */
	private static final String LINEAR_INTERPOLATE = "lerp";

	/** The suffix for an action precondition. */
	public static final String ACTION_PRECOND_SUFFIX = "PreCond";

	/** The goal query name */
	public static final String GOAL_QUERY = "isGoal";

	/** The goal predicate. */
	public static final String GOALARGS_PRED = "goalArgs";

	/** The infers symbol. */
	public static final String INFERS_ACTION = "=>";

	/** The policy rule prefix. */
	public static final String POLICY_QUERY_PREFIX = "polRule";

	public static final String RANGE_TEST = "range";

	/** The StringFact definition for the test predicate. */
	public static final RelationalPredicate TEST_DEFINITION = new RelationalPredicate(
			"test", new String[] { "testArgs" });

	/** The valid actions predicate name. */
	public static final String VALID_ACTIONS = "validActions";

	/** The number of simultaneous actions per step to take. */
	private int actionNum_;

	/** The terms present in an action's precondition. */
	private MultiMap<String, String> actionPreconditions_;

	/** The actions of the rules and their structure. */
	private Map<String, RelationalPredicate> actions_;

	/** The background rules and illegalities of the state. */
	private Map<String, BackgroundKnowledge> backgroundRules_;

	/** The constants found within the goal. */
	private List<String> constants_;

	/** The environment package name. */
	private String environment_;

	/** The name of the goal. */
	private String goalName_;

	/** The state the agent must reach to successfully end the episode. */
	private String goalState_;

	/**
	 * The goal StringFact definition (goalArgs on a b...). Is simply a
	 * StringFact definition created with arguments equalling the number of goal
	 * arguments.
	 */
	private RelationalPredicate goalStringFactDef_;

	/** The hand coded policy for the goal. */
	private RelationalPolicy handCodedPolicy_;

	/** The prerequisites of the rules and their structure. */
	private Map<String, RelationalPredicate> predicates_;

	/** The count value for the query names. */
	private int queryCount_;

	/** The mapping for rules to queries in the Rete object. */
	private Map<Object, String> queryNames_;

	/** The LogicFactory for the experiment. */
	private Rete rete_;

	/** The type hierarchy. */
	private Map<String, ParentChildren> typeHierarchy_;

	/** The type predicates, only used implicitly. */
	private Map<String, RelationalPredicate> typePredicates_;

	/** The parameter of the environment. */
	protected String envParameter_;

	/**
	 * Extracts the constants from the goal (also includes goal variables).
	 * 
	 * @param goalState
	 *            The goal state to parse.
	 */
	private List<String> extractConstants(String goalState) {
		List<String> constants = new ArrayList<String>();

		// Find each term used in the goal
		Pattern p = Pattern.compile(" ([?\\w].*?)(?=\\)| |&)");
		Matcher m = p.matcher(goalState);
		while (m.find()) {
			RelationalArgument term = new RelationalArgument(m.group(1));
			if (!constants.contains(term.toString())) {
				// If the fact is a constant (not a number)
				if (term.isConstant())
					constants.add(term.toString());
			}
		}

		return constants;
	}

	/**
	 * Forms the list of variable action terms the action expects to be in the
	 * precondition rule.
	 * 
	 * @param action
	 *            The StringFact definition for the action.
	 * @return A list of variable terms, from ?X onwards.
	 */
	private List<String> formActionTerms(RelationalPredicate action) {
		List<String> terms = new ArrayList<String>(action.getArgTypes().length);
		for (int i = 0; i < action.getArgTypes().length; i++)
			terms.add(RelationalArgument.getVariableTermArg(i).toString()
					.substring(1));
		return terms;
	}

	/**
	 * Forms the goal predicate to link into the definite goal.
	 * 
	 * @param constants
	 *            The goal constants.
	 * @return The goal predicate to inject the variables.
	 */
	private String formGoalPred(Collection<String> constants) {
		StringBuffer buffer = new StringBuffer("(" + GOALARGS_PRED + " "
				+ goalName_);
		for (String constant : constants)
			buffer.append(" " + constant);
		buffer.append(")");
		return buffer.toString();
	}

	/**
	 * The constructor for a state specification.
	 */
	private final void initialise() {
		try {
			rete_ = new Rete();

			environment_ = this.getClass().getPackage().getName();

			// Initialise any deffunctions
			initialiseFunctions();

			// Type predicates and their hierarchy background rules.
			Map<String, String> typeAssertions = initialiseTypePredicates();

			// Main predicates
			initialiseRegularPredicates();

			// Actions
			initialiseActionPredicates();

			// Initialise the background knowledge rules

			initialiseBackgroundRules(typeAssertions);

			// Initialise the goal state rules
			initialiseGoalRules();

			// Initialise the optimal policy
			handCodedPolicy_ = initialiseHandCodedPolicy();

			queryNames_ = new HashMap<Object, String>();
			queryCount_ = 0;

			Unification.getInstance().resetRangeIndex();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initialiseActionPredicates() throws JessException {
		actions_ = new HashMap<String, RelationalPredicate>();
		for (RelationalPredicate action : initialiseActionTemplates()) {
			actions_.put(action.getFactName(), action);
			defineTemplate(action.getFactName(), rete_);
		}
		actionNum_ = initialiseActionsPerStep();
		// Action rules
		int j = 0;
		for (String actionRule : initialiseActionRules())
			rete_.eval("(defrule actionRule" + j++ + " " + actionRule + ")");

		// Initialise the queries for determining action preconditions
		Map<String, String> purePreConds = initialiseActionPreconditions();
		actionPreconditions_ = MultiMap.createListMultiMap();
		for (String action : purePreConds.keySet()) {
			String query = "(defquery " + action + ACTION_PRECOND_SUFFIX + " "
					+ purePreConds.get(action) + ")";
			rete_.eval(query);
			actionPreconditions_.putCollection(action,
					formActionTerms(actions_.get(action)));
		}
	}

	private void initialiseBackgroundRules(Map<String, String> typeAssertions)
			throws JessException {
		// Type hierarchy rules
		backgroundRules_ = new HashMap<String, BackgroundKnowledge>();
		for (String name : typeAssertions.keySet())
			backgroundRules_.put(name,
					new BackgroundKnowledge(typeAssertions.get(name), false));

		// State Spec rules
		backgroundRules_.putAll(initialiseBackgroundKnowledge());
		for (String ruleNames : backgroundRules_.keySet())
			rete_.eval("(defrule " + ruleNames + " "
					+ backgroundRules_.get(ruleNames) + ")");
	}

	private void initialiseFunctions() throws Exception {
		// Define the linear interpolation function
		rete_.eval("(deffunction " + LINEAR_INTERPOLATE
				+ " (?val1 ?amount ?val2) "
				+ "(return (+ ?val1 (* ?amount (- ?val2 ?val1)))))");
		// Define the range bounding function
		rete_.eval("(deffunction " + RANGE_TEST
				+ " (?min ?minFrac ?var ?max ?maxFrac) " + "(return (<= ("
				+ LINEAR_INTERPOLATE + " ?min ?minFrac ?max) ?var ("
				+ LINEAR_INTERPOLATE + " ?min ?maxFrac ?max))))");
	}

	private void initialiseGoalRules() throws JessException {
		String[] goal = initialiseGoalState();
		goalName_ = goal[0];
		goalState_ = goal[1];
		constants_ = extractConstants(goalState_);
		String[] factTypes = new String[constants_.size() + 1];
		factTypes[0] = "goalPred";
		for (int i = 1; i < factTypes.length; i++)
			factTypes[i] = "arg" + (i - 1);
		goalStringFactDef_ = new RelationalPredicate(GOALARGS_PRED, factTypes);
		rete_.eval("(deftemplate goal (slot goalMet))");
		String goalPred = formGoalPred(constants_);
		String goalRule = "(defrule goalState " + goalPred + " " + goalState_
				+ " => (assert (goal (goalMet TRUE))))";
		rete_.eval(goalRule);
		// Initialise the goal checking query
		rete_.eval("(defquery " + GOAL_QUERY + " (goal (goalMet ?)))");
	}

	private void initialiseRegularPredicates() {
		predicates_ = new HashMap<String, RelationalPredicate>();
		for (RelationalPredicate pred : initialisePredicateTemplates()) {
			predicates_.put(pred.getFactName(), pred);
			defineTemplate(pred.getFactName(), rete_);
		}
	}

	private Map<String, String> initialiseTypePredicates() {
		typePredicates_ = new HashMap<String, RelationalPredicate>();
		Map<String, String> typeAssertions = new HashMap<String, String>();
		Map<String, String> typeParents = initialiseTypePredicateTemplates();
		typeHierarchy_ = new HashMap<String, ParentChildren>();
		for (String type : typeParents.keySet()) {
			typePredicates_.put(type, new RelationalPredicate(type,
					new String[] { type }));
			defineTemplate(type, rete_);

			// Define background knowledge rule
			if (typeParents.get(type) != null) {
				String assertion = "(" + type + " ?X) " + INFERS_ACTION
						+ " (assert (" + typeParents.get(type) + " ?X))";
				typeAssertions.put(type + "ParentRule", assertion);

				// Record the parent children relationship
				ParentChildren thisPC = (typeHierarchy_.containsKey(type)) ? typeHierarchy_
						.get(type) : new ParentChildren();
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
		return typeAssertions;
	}

	// /**
	// * Forms the non-goals by negating the non-type conditions required for
	// the
	// * goal to be met.
	// *
	// * @param goalState
	// * The goal state.
	// * @return A negated goal state (except for the type preds).
	// */
	// private String formNonGoal(String goalState) throws Exception {
	// RelationalRule nonGoalRule = new RelationalRule(goalState_);
	// SortedSet<RelationalPredicate> conds = nonGoalRule.getConditions(false);
	//
	// // Split the conds up into 2 sets: types and normal
	// Collection<RelationalPredicate> types = new TreeSet<RelationalPredicate>(
	// conds.comparator());
	// Collection<RelationalPredicate> normal = new
	// TreeSet<RelationalPredicate>(
	// conds.comparator());
	// Collection<RelationalPredicate> inequalities = new
	// TreeSet<RelationalPredicate>(
	// conds.comparator());
	// for (RelationalPredicate cond : conds) {
	// if (isTypePredicate(cond.getFactName()))
	// types.add(cond);
	// else if (cond.getFactName().equals("test"))
	// inequalities.add(cond);
	// else
	// normal.add(cond);
	// }
	// // If there are no normal conds, negate the types instead (otherwise
	// // there would be an infinite number of goals)
	// if (normal.isEmpty()) {
	// throw new Exception("Goal is only type conditions! Cannot negate.");
	// }
	//
	// // Build the string
	// StringBuffer nonGoalStr = new StringBuffer();
	// // Types first
	// for (RelationalPredicate type : types)
	// nonGoalStr.append(type + " ");
	// // Then Inequalities
	// for (RelationalPredicate ineq : inequalities)
	// nonGoalStr.append(ineq + " ");
	// // Then normals
	// nonGoalStr.append("(not");
	// if (normal.size() > 1)
	// nonGoalStr.append(" (and");
	// for (RelationalPredicate norm : normal)
	// nonGoalStr.append(" " + norm);
	// nonGoalStr.append(")");
	// if (normal.size() > 1)
	// nonGoalStr.append(")");
	//
	// return nonGoalStr.toString();
	// }

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

	/**
	 * Initialises the rules for finding valid actions.
	 * 
	 * @return A map of actions and the pure preconditions to be valid.
	 */
	protected abstract Map<String, String> initialiseActionPreconditions();

	/**
	 * Initialises the rules that define how an action evaluates upon the state.
	 * 
	 * @return A collection of rules which affect the state.
	 */
	protected Collection<String> initialiseActionRules() {
		return new ArrayList<String>();
	}

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
	protected abstract Collection<RelationalPredicate> initialiseActionTemplates();

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
	 * @return The name of the goal and the minimal state that is true when the
	 *         goal is satisfied.
	 */
	protected abstract String[] initialiseGoalState();

	/**
	 * Initialises the optimal policy for the goal.
	 * 
	 * @param factory
	 *            The factory for creating rules.
	 * 
	 * @return The policy that solves the goal in optimal time.
	 */
	protected abstract BasicRelationalPolicy initialiseHandCodedPolicy();

	/**
	 * Initialises the state predicates.
	 * 
	 * @return The list of guided predicate names.
	 */
	protected abstract Collection<RelationalPredicate> initialisePredicateTemplates();

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
	 * Simply asserts the goal predicate with the given argument.
	 * 
	 * @param goalArgs
	 *            The goal args to add, or if null, generate new ones.
	 * @param state
	 *            The state to add to.
	 * @return The goal replacements map created from the goal args.
	 */
	public BidiMap assertGoalPred(List<String> goalArgs, Rete state)
			throws Exception {
		StringBuffer goalString = new StringBuffer();
		for (String arg : goalArgs)
			goalString.append(" " + arg);
		String assertion = "(" + GOALARGS_PRED + " " + goalName_ + goalString
				+ ")";
		rete_.assertString(assertion);

		// Setting the ObjectObservations
		BidiMap goalReplacements = new DualHashBidiMap();
		int i = 0;
		for (String goalTerm : goalArgs) {
			if (!goalReplacements.containsKey(goalTerm))
				goalReplacements.put(goalTerm,
						RelationalArgument.createGoalTerm(i));
			i++;
		}

		return goalReplacements;
	}

	/**
	 * Creates the necessary type conditions for a rule.
	 * 
	 * @param fact
	 *            The fact.
	 * @return The type conditions.
	 */
	public Collection<RelationalPredicate> createTypeConds(
			RelationalPredicate fact) {
		Collection<RelationalPredicate> typeConds = new HashSet<RelationalPredicate>();
		// If the term itself is a type pred, return.
		if (isTypePredicate(fact.getFactName())) {
			return typeConds;
		}

		String[] types = fact.getArgTypes();
		for (int i = 0; i < types.length; i++) {
			if (!fact.getArguments()[i].equals("?")) {
				RelationalPredicate typeFact = typePredicates_.get(types[i]);
				if (typeFact != null) {
					typeConds.add(new RelationalPredicate(typeFact,
							new String[] { fact.getArguments()[i] }));
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
			rete.eval("(deftemplate " + factName + " (declare (ordered TRUE)))");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return factName;
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
	public final MultiMap<String, String[]> generateValidActions(Rete state) {
		MultiMap<String, String[]> validActions = MultiMap
				.createSortedSetMultiMap(ArgumentComparator.getInstance());

		for (String action : actions_.keySet()) {
			try {
				QueryResult result = state.runQueryStar(action
						+ ACTION_PRECOND_SUFFIX, new ValueVector());
				while (result.next()) {
					List<String> actionTerms = actionPreconditions_
							.getList(action);
					String[] arguments = new String[actionTerms.size()];
					for (int i = 0; i < arguments.length; i++) {
						arguments[i] = result.getSymbol(actionTerms.get(i));
					}
					validActions.put(action, arguments);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return validActions;
	}

	public Map<String, RelationalPredicate> getActions() {
		return actions_;
	}

	public Collection<String> getConstants() {
		return constants_;
	}

	public String getEnvironmentName() {
		return environment_;
	}

	/**
	 * Gets the goal name defined by the experiment. May not necessarily be the
	 * goal the agent is pursuing.
	 * 
	 * @return The name of the goal.
	 */
	public String getGoalName() {
		return goalName_;
	}

	public String getGoalState() {
		return goalState_;
	}

	/**
	 * Gets the optimal policy for the problem. Or at least gets a good policy.
	 * 
	 * @return The policy that is optimal.
	 */
	public RelationalPolicy getHandCodedPolicy() {
		return handCodedPolicy_;
	}

	public int getNumReturnedActions() {
		return actionNum_;
	}

	public Map<String, RelationalPredicate> getPredicates() {
		return predicates_;
	}

	public Rete getRete() {
		return rete_;
	}

	/**
	 * Gets or creates a rule query for a fact (an anonymous fact which only
	 * matches the existence of the fact).
	 * 
	 * @param fact
	 *            The anonymous fact.
	 * @return The query from the rule (possibly newly created).
	 */
	public String getRuleQuery(RelationalPredicate fact) {
		String result = queryNames_.get(fact);
		if (result == null) {
			try {
				result = POLICY_QUERY_PREFIX + queryCount_++;
				// Create the query
				rete_.eval("(defquery " + result + " " + fact + ")");
				queryNames_.put(fact, result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return result;
	}

	/**
	 * Gets or creates a rule query for a guided rule.
	 * 
	 * @param gr
	 *            The guided rule associated with a query.
	 * @return The query from the rule (possibly newly created).
	 */
	public String getRuleQuery(RelationalRule gr) {
		Pair<RelationalRule, List<String>> ruleQuery = new Pair<RelationalRule, List<String>>(
				gr, gr.getQueryParameters());
		String result = queryNames_.get(ruleQuery);
		if (result == null) {
			try {
				result = POLICY_QUERY_PREFIX + queryCount_++;
				// If the rule has parameters, declare them as variables.
				if (gr.getQueryParameters() != null
						&& !gr.getQueryParameters().isEmpty()
						|| !gr.getRangeContexts().isEmpty()) {
					StringBuffer declares = new StringBuffer(
							"(declare (variables");

					for (RangeContext rc : gr.getRangeContexts()) {
						String rangeVariable = rc.getRangeVariable();
						declares.append(" " + rangeVariable + RangeBound.MIN);
						declares.append(" " + rangeVariable + RangeBound.MAX);
					}
					for (String param : gr.getQueryParameters()) {
						declares.append(" " + param);
					}
					declares.append("))");

					rete_.eval("(defquery " + result + " "
							+ declares.toString() + " "
							+ gr.getStringConditions() + ")");
				} else {
					rete_.eval("(defquery " + result + " "
							+ gr.getStringConditions() + ")");
				}
				queryNames_.put(ruleQuery, result);
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
	public RelationalPredicate getPredicateByName(String factName) {
		if (predicates_.containsKey(factName))
			return predicates_.get(factName);
		if (actions_.containsKey(factName))
			return actions_.get(factName);
		if (typePredicates_.containsKey(factName))
			return typePredicates_.get(factName);
		if (factName.equals("test"))
			return TEST_DEFINITION;
		if (factName.equals(GOALARGS_PRED))
			return goalStringFactDef_;
		return null;
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

	public Map<String, RelationalPredicate> getTypePredicates() {
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
		if (predicate.equals(GOALARGS_PRED))
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
	 * Basic method that converts a Collection of conditions into a neat JESS
	 * parseable String.
	 * 
	 * @param conditions
	 *            The conditions to put into string format.
	 * @return The JESS parseable string of the conditions.
	 */
	public static String conditionsToString(
			Collection<RelationalPredicate> conditions) {
		StringBuffer buffer = new StringBuffer();
		boolean first = true;
		for (RelationalPredicate cond : conditions) {
			if (!first)
				buffer.append(" ");
			buffer.append(cond.toString());
			first = false;
		}
		return buffer.toString();
	}

	/**
	 * Creates the inequals tests from the terms given. Note anonymous terms are
	 * special in that they aren't inequal to one-another.
	 * 
	 * @return The string for detailing inequality '(test (<> ?X a b ?Y ?_0))
	 *         (test (<> ?Y a ...))'
	 */
	public static Collection<RelationalPredicate> createInequalityTests(
			Collection<String> variableTerms, Collection<String> constantTerms) {
		Collection<RelationalPredicate> tests = new ArrayList<RelationalPredicate>();

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
				tests.add(new RelationalPredicate(TEST_DEFINITION,
						new String[] { buffer.toString() }));
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
		Collection<Fact> facts = new HashSet<Fact>();
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

	/**
	 * Initialises the environment without a goal argument (so it uses default).
	 * 
	 * @param classPrefix
	 *            The class prefix of the environment to load.
	 * @return The initialised environment specification.
	 */
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
	 * Initialises the environment with a given goal argument.
	 * 
	 * @param classPrefix
	 *            The class prefix of the environment to load.
	 * @param goalArg
	 *            The goal for the environment.
	 * @return The initialised environment specification.
	 */
	public static StateSpec initInstance(String classPrefix, String goalArg) {
		try {
			instance_ = (StateSpec) Class.forName(
					classPrefix + StateSpec.class.getSimpleName())
					.newInstance();
			instance_.envParameter_ = goalArg;
			instance_.initialise();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return instance_;
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
			NumberEnum.valueOf(type);
			return true;
		} catch (IllegalArgumentException iae) {
		}
		return false;
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
			instance_.envParameter_ = goalString;
			instance_.rete_.clear();
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
		fact = fact.trim();
		ArrayList<String> elements = new ArrayList<String>();
		int bracketCount = 0;
		int elementStart = -1;
		// Run through the fact, char-by-char, recording each element
		for (int i = 0; i < fact.length(); i++) {
			char ch = fact.charAt(i);
			switch (ch) {
			case '(':
				// Capturing brackets (except the beginning one).
				if (i > 0) {
					bracketCount++;
					if (elementStart == -1)
						elementStart = i;
				}
				break;
			case ' ':
				// Separating facts by space (if not capturing an internal fact)
				if (bracketCount == 0) {
					elements.add(fact.substring(elementStart, i));
					elementStart = -1;
				}
				break;
			case ')':
				// Capturing internal facts and finishing this fact
				if (bracketCount > 0)
					bracketCount--;
				else if (bracketCount == 0) {
					elements.add(fact.substring(elementStart, i));
					elementStart = -1;
				}
				break;
			case ':':
				// Dealing with JESS modules.
				if (fact.charAt(i - 1) == ':')
					elementStart = -1;
				break;
			default:
				// Capture element
				if (elementStart == -1)
					elementStart = i;
			}
		}
		if (elementStart != -1)
			elements.add(fact.substring(elementStart));

		return elements.toArray(new String[elements.size()]);
	}

	/**
	 * Transforms a String fact into a StringFact.
	 * 
	 * @param fact
	 *            The String fact.
	 * @return A StringFact version of the fact.
	 */
	public static RelationalPredicate toRelationalPredicate(String fact) {
		String[] condSplit = StateSpec.splitFact(fact);
		if (condSplit[0].equals("initial-fact"))
			return null;
		boolean negated = false;
		// Negated case
		if (condSplit[0].equals("not")) {
			negated = true;
			condSplit = StateSpec.splitFact(condSplit[1]);
		}

		String[] arguments = new String[condSplit.length - 1];
		System.arraycopy(condSplit, 1, arguments, 0, arguments.length);
		return new RelationalPredicate(getInstance().getPredicateByName(
				condSplit[0]), arguments, negated);
	}

	/**
	 * A basic class which holds parent-child data for a given type predicate.
	 * 
	 * @author Sam Sarjant
	 */
	private class ParentChildren {
		private Collection<String> children_;
		private String parent_;

		public void addChild(String child) {
			if (children_ == null)
				children_ = new HashSet<String>();
			children_.add(child);
		}

		public Collection<String> getChildren() {
			return children_;
		}

		public String getParent() {
			return parent_;
		}

		public void setParent(String parent) {
			parent_ = parent;
		}
	}
}
