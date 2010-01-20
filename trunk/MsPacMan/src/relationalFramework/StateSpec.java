package relationalFramework;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mandarax.kernel.ConstantTerm;
import org.mandarax.kernel.Fact;
import org.mandarax.kernel.InferenceEngine;
import org.mandarax.kernel.KnowledgeBase;
import org.mandarax.kernel.LogicFactory;
import org.mandarax.kernel.Predicate;
import org.mandarax.kernel.Prerequisite;
import org.mandarax.kernel.Query;
import org.mandarax.kernel.Replacement;
import org.mandarax.kernel.ResultSet;
import org.mandarax.kernel.Rule;
import org.mandarax.kernel.SimplePredicate;
import org.mandarax.kernel.Term;
import org.mandarax.kernel.VariableTerm;
import org.mandarax.kernel.meta.JConstructor;
import org.mandarax.kernel.meta.JPredicate;
import org.mandarax.util.LogicFactorySupport;

/**
 * A class to outline the specifications of the environment.
 * 
 * @author Sam Sarjant
 */
public abstract class StateSpec {
	/** The singleton instance. */
	private static StateSpec instance_;

	/** The terminal fact. */
	private static Fact terminal_;

	/** The variable state term. */
	private static Term stateTerm_;

	/** The constant state spec term. */
	private static Term specTerm_;

	/** The predicate for handling inequality. */
	private static Predicate inequalityPred_;

	/** The predicate containing the valid actions. */
	private static Predicate validActionsPred_;

	/** The prerequisites of the rules. */
	private List<GuidedPredicate> predicates_;

	/** The type predicates, only used implicitly. */
	private Map<Class, Predicate> typePredicates_;

	/** The actions of the rules. */
	private List<GuidedPredicate> actions_;

	/** The number of simultaneous actions per step to take. */
	private int actionNum_;

	/** The rules relating to an actions precondition. */
	private Map<Predicate, Rule> actionPreconditions_;

	/** A map for retrieving predicates by name. */
	private Map<String, GuidedPredicate> predByNames_;

	/** The state the agent must reach to successfully end the episode. */
	private org.mandarax.kernel.Rule goalState_;

	/** The constants found within the goal. */
	private Map<String, Object> constants_;

	/** The name of the goal. */
	protected String goal_;

	/** The optimal policy for the goal. */
	private Policy optimalPolicy_;

	/** The background knowledge regarding the predicates and actions. */
	private KnowledgeBase backgroundKnowledge_;

	/** The suffix to this class for use with dynamically loaded classes. */
	public static final String CLASS_SUFFIX = "StateSpec";

	/** The name of the inequal predicate. */
	public static final String INEQUAL = "inequal";

	/** The LogicFactory for the experiment. */
	private LogicFactory factory_;

	/**
	 * The constructor for a state specification.
	 */
	private final void initialise(LogicFactory factory) {
		factory_ = factory;
		constants_ = new HashMap<String, Object>();
		typePredicates_ = initialiseTypePredicates();
		predicates_ = initialisePredicates();
		actions_ = initialiseActions();
		actionNum_ = initialiseActionsPerStep();
		predByNames_ = createPredNameMap();
		goalState_ = initialiseGoalState(factory_);
		backgroundKnowledge_ = initialiseBackgroundKnowledge(factory_);
		addGoalConstants(predicates_, goalState_);

		actionPreconditions_ = initialiseActionPreconditions(actions_);
	}

	/**
	 * Creates a map of predicates accessed by their name from the existing
	 * predicates and action predicates.
	 * 
	 * @return The mapping of Predicates to Strings.
	 */
	private Map<String, GuidedPredicate> createPredNameMap() {
		Map<String, GuidedPredicate> mapping = new HashMap<String, GuidedPredicate>();

		// Scanning the predicates
		for (GuidedPredicate gp : predicates_) {
			mapping.put(gp.getPredicate().getName(), gp);
		}

		// Scanning the actions
		for (GuidedPredicate gp : actions_) {
			mapping.put(gp.getPredicate().getName(), gp);
		}

		return mapping;
	}

	/**
	 * Initialises the state type predicates.
	 * 
	 * @return The list of guided predicates.
	 */
	protected abstract Map<Class, Predicate> initialiseTypePredicates();

	/**
	 * Initialises the state predicates.
	 * 
	 * @return The list of guided predicates.
	 */
	protected abstract List<GuidedPredicate> initialisePredicates();

	/**
	 * Initialises the state actions.
	 * 
	 * @return The list of guided actions.
	 */
	protected abstract List<GuidedPredicate> initialiseActions();

	/**
	 * Initialises the number of actions to take per time step.
	 * 
	 * @return The number of actions to take per step.
	 */
	protected abstract int initialiseActionsPerStep();

	/**
	 * Initialises the rules for finding valid actions.
	 * 
	 * @return A map of actions and their corresponding preconditions to be
	 *         valid.
	 */
	protected abstract Map<Predicate, Rule> initialiseActionPreconditions(
			List<GuidedPredicate> actions);

	/**
	 * Initialises the goal state.
	 * 
	 * @return The rule that is true when it is the goal state,
	 */
	protected abstract org.mandarax.kernel.Rule initialiseGoalState(
			LogicFactory factory);

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
	 * Initialises the background knowledge.
	 * 
	 * @return The background knowledge base.
	 */
	protected abstract KnowledgeBase initialiseBackgroundKnowledge(
			LogicFactory factory);

	/**
	 * Inserts the valid actions into the state knowledge base using the state
	 * observations to determine validity.
	 * 
	 * @param stateKB
	 *            The current observations of the state to be inserted into.
	 */
	public final void insertValidActions(KnowledgeBase stateKB) {
		// Logic constructs
		LogicFactory factory = PolicyGenerator.getInstance().getLogicFactory();
		LogicFactorySupport factorySupport = new LogicFactorySupport(factory);
		InferenceEngine ie = PolicyGenerator.getInstance().getInferenceEngine();

		Set<Fact> validActions = new HashSet<Fact>();

		try {
			for (Predicate action : actionPreconditions_.keySet()) {
				Rule actionRule = actionPreconditions_.get(action);
				RuleCondition ruleConds = new RuleCondition(actionRule
						.getBody());

				// Forming the query
				Query query = factorySupport.query(ruleConds.getFactArray(),
						actionRule.toString());

				ResultSet results = ie.query(query, stateKB,
						InferenceEngine.ALL, InferenceEngine.BUBBLE_EXCEPTIONS);

				if (results.next()) {
					do {
						Map<Term, Term> replacementMap = results.getResults();
						Collection<Replacement> replacements = new ArrayList<Replacement>();
						// Find the replacements for the variable terms
						// in the action
						for (Term var : actionRule.getHead().getTerms()) {
							if (var instanceof VariableTerm) {
								replacements.add(new Replacement(var,
										replacementMap.get(var)));
							} else {
								replacements.add(new Replacement(var, var));
							}
						}

						// Apply the replacements and add the fact to
						// the set
						Fact groundAction = actionRule.getHead().applyToFact(
								replacements);

						// If the action is ground
						validActions.add(groundAction);
					} while (results.next());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Add the valid actions fact to the knowledge base
		Term[] terms = { factory.createConstantTerm(validActions) };
		Fact validActionsFact = factory.createFact(getValidActionsPredicate(),
				terms);
		stateKB.add(validActionsFact);
	}

	/**
	 * Adds a string id to a constant used.
	 * 
	 * @param id
	 *            The string id, sans the "[X]" brackets
	 * @param obj
	 *            The object linked to.
	 */
	public void addConstant(String id, Object obj) {
		constants_.put(id, obj);
	}

	/**
	 * Parses a rule from a human readable string. the string is in the format
	 * 'predicate(arg[,arg]) [& predicate(arg[,arg])] -> predicate(arg[,arg])'.
	 * If an arg is uppercase, it is considered a variable. If lowercase, a
	 * constant which must be referenced in the constant terms map.
	 * 
	 * @param rule
	 *            The string representation of the rule.
	 * @param constantTerms
	 *            The objects to replace the constants with or null
	 * 
	 * @return An instantiated rule.
	 */
	public final Rule parseRule(String rule, Map<String, Object> constantTerms) {
		String[][] info = extractInfo(rule);
		// Compiling the initial term map of constants
		Map<String, Term> termMap = new HashMap<String, Term>();
		if (constantTerms != null) {
			for (String constant : constantTerms.keySet()) {
				termMap.put(constant, factory_.createConstantTerm(constantTerms
						.get(constant)));
			}
		}

		// Form the rule
		List<Prerequisite> prereqs = new ArrayList<Prerequisite>();
		Fact actionFact = null;
		Set<Term> allTerms = new HashSet<Term>();
		for (int i = 0; i < info.length; i++) {
			// Get the guided predicate
			GuidedPredicate cond = getGuidedPredicate(info[i][0]);
			// Check if it's a JConstructor
			int offset = (cond.getPredicate() instanceof JConstructor) ? 1 : 0;
			// Find the terms used in the predicate arguments
			Term[] terms = findTerms(factory_, termMap, info[i], cond
					.getPredicate().getStructure(), offset);
			// Instantiate the guided predicate with the args.
			List<Prerequisite> rulePreqs = cond.factify(factory_, terms, false,
					false, allTerms);
			// Add the resulting prerequisites
			for (Prerequisite prereq : rulePreqs) {
				// Separate the action
				if (i < info.length - 1) {
					// Add as condition
					if (!prereqs.contains(prereq))
						prereqs.add(prereq);
				} else {
					// Add as action
					if (prereq.getPredicate().getName().equals(info[i][0])) {
						actionFact = prereq;
					}
				}
			}
		}

		return factory_.createRule(prereqs, actionFact);
	}

	/**
	 * Finds or creates the terms present in the predicate
	 * 
	 * @param factory
	 *            The factory to create terms.
	 * @param termMap
	 *            The map of existing terms.
	 * @param predArgs
	 *            The predicate and its arguments.
	 * 
	 * @return An array of terms used in the predicate.
	 */
	private Term[] findTerms(LogicFactory factory, Map<String, Term> termMap,
			String[] predArgs, Class[] predStructure, int offset) {
		Term[] terms = new Term[predArgs.length - 1];

		for (int i = 0; i < terms.length; i++) {
			// If the term is a constant or has occurred already, get it from
			// the map
			if (termMap.containsKey(predArgs[i + 1])) {
				terms[i] = termMap.get(predArgs[i + 1]);
			} else {
				// The term is a new variable
				terms[i] = factory.createVariableTerm(predArgs[i + 1],
						predStructure[i + 1 + offset]);
				termMap.put(predArgs[i + 1], terms[i]);
			}
		}
		return terms;
	}

	/**
	 * Extracts the predicates and predicate arguments from a string rule.
	 * 
	 * @param rule
	 *            The rule being extracted
	 * @return A 2D array of predicates, with an array per predicate, and each
	 *         array containing the pred name and the arguments. The last
	 *         predicate is the action.
	 */
	private String[][] extractInfo(String rule) {
		// Split the rule into conditions and actions
		String[] split = rule.split("->");
		if (split.length < 2)
			return null;

		String[] conditions = split[0].split("&");
		String[][] info = new String[conditions.length + 1][];
		for (int i = 0; i < info.length; i++) {
			String predicate = (i < info.length - 1) ? conditions[i] : split[1];

			// A Regexp looking for a predicate with args 'clear([a])'
			// 'move([b],<X>)' 'cool()'
			Pattern p = Pattern
					.compile("(\\w+)\\(((?:(?:(?:\\[\\w+\\])|(?:<\\w+>))"
							+ "(?:,(?:(?:\\[\\w+\\])|(?:<\\w+>)))*)*)\\)");
			Matcher m = p.matcher(predicate);
			if (m.find()) {
				// Group 1 is the predicate name, Group 2 is the arg(s)
				String arguments = m.group(2).replaceAll("(\\[|\\]|<|>)", "");
				String[] args = arguments.split(",");
				info[i] = new String[args.length + 1];
				info[i][0] = m.group(1);
				for (int j = 0; j < args.length; j++) {
					info[i][j + 1] = args[j];
				}
			} else {
				return null;
			}
		}

		return info;
	}

	/**
	 * Adds the constants used in the goal to all predicates in the system.
	 * 
	 * @param predicates
	 *            The predicates in the system.
	 * @param goalState
	 *            The goal state.
	 */
	private ConstantTerm[] addGoalConstants(List<GuidedPredicate> predicates,
			org.mandarax.kernel.Rule goalState) {
		List<ConstantTerm> constantTerms = new ArrayList<ConstantTerm>();
		List<Fact> body = goalState.getBody();
		MultiMap<Class, ConstantTerm> constantMap = new MultiMap<Class, ConstantTerm>();
		// For every fact in the body of the rule, extract the constants
		for (Fact fact : body) {
			Term[] factTerms = fact.getTerms();
			for (Term term : factTerms) {
				// Add any constant terms found.
				if (term.isConstant()) {
					constantMap.putContains(term.getType(), (ConstantTerm) term);
					constantTerms.add((ConstantTerm) term);
				}
			}
		}

		// If there are no constants, exit
		if (constantMap.isEmpty())
			return new ConstantTerm[0];

		// Now with the constants, add those to the predicates that can accept
		// them
		for (GuidedPredicate pred : predicates) {
			Class[] predStructure = pred.getPredicate().getStructure();
			int offset = 0;
			if (pred.getPredicate() instanceof JConstructor)
				offset = 1;

			// For each of the classes in the predicate
			for (int i = 0; i < predStructure.length - offset; i++) {
				for (Class constClass : constantMap.keySet()) {
					// If the predicate is the same or superclass of the
					// constant
					if (predStructure[i + offset].isAssignableFrom(constClass)) {
						// Check that this pred slot can take consts
						boolean addConsts = false;
						for (int j = 0; j < pred.getPredValues()[i].length; j++) {
							if (pred.getPredValues()[i][j].getTermType() != PredTerm.VALUE) {
								addConsts = true;
								break;
							}
						}

						// If there is a free or tied value, we can add
						// constants
						if (addConsts)
							pred.getPredValues()[i] = addConstants(pred
									.getPredValues()[i], constantMap
									.get(constClass));
					}
				}
			}
		}

		return constantTerms.toArray(new ConstantTerm[constantTerms.size()]);
	}

	/**
	 * Adds the constants to the array of pred terms, if they're not already
	 * there.
	 * 
	 * @param predTerms
	 *            The predicate term array to add to.
	 * @param constants
	 *            The constants to be added.
	 * @return The expanded term array.
	 */
	private PredTerm[] addConstants(PredTerm[] predTerms,
			Collection<ConstantTerm> constants) {
		// Move the existing terms into a set
		Set<PredTerm> newTerms = new HashSet<PredTerm>();
		for (PredTerm predTerm : predTerms) {
			newTerms.add(predTerm);
		}

		// Add the constants to the set
		for (ConstantTerm ct : constants) {
			newTerms.add(new PredTerm(ct.getObject()));
		}

		return newTerms.toArray(new PredTerm[newTerms.size()]);
	}

	public List<GuidedPredicate> getPredicates() {
		return predicates_;
	}

	public List<GuidedPredicate> getActions() {
		return actions_;
	}

	public int getNumActions() {
		return actionNum_;
	}

	public org.mandarax.kernel.Rule getGoalState() {
		return goalState_;
	}

	/**
	 * Gets the optimal policy for the problem. Or at least gets a good policy.
	 * 
	 * @return The policy that is optimal.
	 */
	public Policy getOptimalPolicy() {
		if (optimalPolicy_ == null)
			optimalPolicy_ = initialiseOptimalPolicy();
		return optimalPolicy_;
	}

	public KnowledgeBase getBackgroundKnowledge() {
		return backgroundKnowledge_;
	}

	public Map<String, Object> getConstants() {
		return constants_;
	}

	/**
	 * Gets the type predicate using the given key, if such a predicate exists.
	 * 
	 * @return The predicate associated with the class, or null if no such class
	 *         key.
	 */
	public Predicate getTypePredicate(Class key) {
		return typePredicates_.get(key);
	}

	public boolean isTypePredicate(Predicate predicate) {
		return typePredicates_.values().contains(predicate);
	}

	/**
	 * Checks if the predicate is useful (not type, action or inequal).
	 * 
	 * @param predicate
	 *            The predicate being checked.
	 * @return True if the predicate is useful, false if it is type, action,
	 *         inequal or otherwise.
	 */
	public boolean isUsefulPredicate(Predicate predicate) {
		if (predicate.equals(getInequalityPredicate()))
			return false;
		if (predicate.equals(getValidActionsPredicate()))
			return false;
		if (isTypePredicate(predicate))
			return false;
		return true;
	}

	/**
	 * Checks if a term is useful (not a state or spec term).
	 * 
	 * @param term
	 *            The term being checked.
	 * @param factory
	 *            The logic factory.
	 * @return True if the term is useful, false otherwise.
	 */
	@SuppressWarnings("unchecked")
	public static boolean isUsefulTerm(ConstantTerm term, LogicFactory factory) {
		// Checking spec term
		if (term.equals(getSpecTerm(factory)))
			return false;
		// Checking state term
		Class termClass = term.getObject().getClass();
		Class stateClass = getStateTerm(factory).getType();
		if (stateClass.isAssignableFrom(termClass))
			return false;
		return true;
	}

	public GuidedPredicate getGuidedPredicate(String name) {
		if (name != null)
			return predByNames_.get(name);
		return null;
	}

	@Override
	public String toString() {
		return "StateSpec";
	}

	/**
	 * Convenience method for creating a defined predicate.
	 * 
	 * @param stateSpecClass
	 *            The class containing the method.
	 * @param predicateStructure
	 *            The structure of the method/predicate.
	 * @param predValues
	 *            The possible values to be used within the predicate.
	 * @param methodName
	 *            The name of the method/predicate.
	 * 
	 * @return A new defined predicate from the above parameters.
	 * @throws NoSuchMethodException
	 *             If the method doesn't exist.
	 */
	protected GuidedPredicate createDefinedPredicate(Class stateSpecClass,
			Class[] predicateStructure, PredTerm[][] predValues,
			String methodName) throws NoSuchMethodException {
		Method method = stateSpecClass
				.getMethod(methodName, predicateStructure);
		Predicate predicate = new JPredicate(method);
		return new GuidedPredicate(predicate, predValues);
	}

	/**
	 * Convenience method for creating a tied PredTerm.
	 * 
	 * @param termName
	 *            The term name.
	 * @return The array containing the tied term.
	 */
	protected PredTerm[] createTied(String termName, Class termClass) {
		PredTerm[] terms = { new PredTerm(termName, termClass, PredTerm.TIED) };
		return terms;
	}

	/**
	 * Convenience method for creating a tied and free PredTerm.
	 * 
	 * @param termName
	 *            The term name.
	 * @return The array containing the tied and free terms.
	 */
	protected PredTerm[] createTiedAndFree(String termName, Class termClass) {
		PredTerm[] terms = { new PredTerm(termName, termClass, PredTerm.TIED),
				new PredTerm(termName, termClass, PredTerm.FREE) };
		return terms;
	}

	/**
	 * Inserts the state argument into a type array.
	 * 
	 * @param types
	 *            The type array.
	 * @return An expanded array with the state slot at position 0.
	 */
	protected Class[] insertState(Class[] types) {
		Class[] stateTypes = new Class[types.length + 1];
		stateTypes[0] = State.class;
		for (int i = 0; i < types.length; i++) {
			stateTypes[i + 1] = types[i];
		}

		return stateTypes;
	}

	/**
	 * Gets the terminal fact used in all environments. If true, then the
	 * episode should be successfully completed.
	 * 
	 * @param factory
	 *            The factory to generate the fact.
	 * @return The terminal fact.
	 */
	protected static Fact getTerminalFact(LogicFactory factory) {
		if (terminal_ == null) {
			Predicate termPred = new SimplePredicate("terminal",
					new Class[] { State.class });
			terminal_ = factory.createFact(termPred,
					new Term[] { getStateTerm(factory) });
		}
		return terminal_;
	}

	/**
	 * Gets the state term to be shared among the predicates.
	 * 
	 * @param factory
	 *            The factory to generate the term.
	 * @return The variable state term.
	 */
	protected static Term getStateTerm(LogicFactory factory) {
		if (stateTerm_ == null) {
			stateTerm_ = factory.createVariableTerm("State", State.class);
		}
		return stateTerm_;
	}

	/**
	 * Gets the state specification term to be shared among the predicates. To
	 * be used with JPredicates.
	 * 
	 * @param factory
	 *            The factory to generate the term.
	 * @return The state specification term.
	 */
	protected static Term getSpecTerm(LogicFactory factory) {
		if (specTerm_ == null) {
			specTerm_ = factory.createConstantTerm(getInstance());
		}
		return specTerm_;
	}

	/**
	 * Gets the inequality predicate, used for enforcing the inequality rule
	 * among differing terms.
	 * 
	 * @return The inequality predicate.
	 */
	protected static Predicate getInequalityPredicate() {
		if (inequalityPred_ == null) {
			try {
				Class[] types = { Object.class, Object.class };
				Method method = StateSpec.class.getMethod(INEQUAL, types);
				inequalityPred_ = new JPredicate(method);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return inequalityPred_;
	}

	/**
	 * Gets the valid actions predicate, for conveying the valid actions to take
	 * in the state to the agent. The argument for the fact is a Set<Fact>.
	 * 
	 * @return The valid actions predicate.
	 */
	protected static Predicate getValidActionsPredicate() {
		if (validActionsPred_ == null) {
			try {
				Class[] types = { Set.class };
				validActionsPred_ = new SimplePredicate("validActions", types);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return validActionsPred_;
	}

	/**
	 * Adds all elements from a list to another if the list being added to
	 * doesn't already contain them.
	 * 
	 * @param addTo
	 *            The list to add to.
	 * @param addFrom
	 *            The list to add from, if the terms are not contained.
	 */
	public static void addContains(List<Prerequisite> addTo,
			List<Prerequisite> addFrom) {
		for (Prerequisite prereq : addFrom) {
			if (!addTo.contains(prereq)) {
				addTo.add(prereq);
			}
		}
	}

	/**
	 * Gets the singleton instance of the state spec.
	 * 
	 * @return The instance.
	 */
	public static StateSpec getInstance() {
		return instance_;
	}

	public static StateSpec initInstance(String classPrefix,
			LogicFactory factory) {
		try {
			instance_ = (StateSpec) Class.forName(
					classPrefix + StateSpec.CLASS_SUFFIX).newInstance();
			instance_.initialise(factory);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return instance_;
	}

	/**
	 * Creates a string representation of a rule, in a light, easy-to-read, and
	 * parsable format.
	 * 
	 * @param rule
	 *            The rule begin output.
	 * @return A light, parsable representation of the rule.
	 */
	public static String encodeRule(Rule rule) {
		StringBuffer buffer = new StringBuffer();
		// Only output the prereqs that aren't type preds
		List<Prerequisite> body = rule.getBody();

		boolean plural = false;
		// Check all prereqs, only outputting the Java preds
		for (Prerequisite prereq : body) {
			// Don't show type and inequal predicates.
			if ((!StateSpec.getInstance()
					.isTypePredicate(prereq.getPredicate()))
					&& (!prereq.getPredicate().getName().equals(
							StateSpec.INEQUAL))) {
				// If we have more than one condition
				if (plural)
					buffer.append("& ");

				buffer.append(StateSpec.lightenFact(prereq));
				plural = true;
			}
		}

		buffer.append("-> ");
		buffer.append(StateSpec.lightenFact(rule.getHead()));

		return buffer.toString();
	}

	/**
	 * Encodes a fact by removing unnecessary terms from the description.
	 * 
	 * @param fact
	 *            The fact being simplified.
	 * @return A string output of the simplified fact.
	 */
	public static String lightenFact(Fact fact) {
		StringBuffer buffer = new StringBuffer();

		// Output the i+1 arguments and the predicate
		buffer.append(fact.getPredicate().getName() + "(");
		boolean plural = false;
		Term[] terms = fact.getTerms();

		int jSkip = 0;
		if (fact.getPredicate() instanceof JConstructor)
			jSkip = 1;
		for (int i = jSkip; i < terms.length; i++) {
			// Don't bother noting the state term
			if (!Object[].class.isAssignableFrom(terms[i].getType())) {
				if (plural)
					buffer.append(",");
				buffer.append(terms[i]);
				plural = true;
			}
		}
		buffer.append(") ");
		return buffer.toString();
	}

	/**
	 * Adds a fact to the KB, using the given object as a constant and a class
	 * for the object.
	 * 
	 * @param obj
	 *            The object to add as a constant.
	 * @param clazz
	 *            The class of the object.
	 * @param returnedKB
	 *            The kb to add to.
	 * @param factory
	 *            The logic factory.
	 * @param classPrefix
	 *            The class prefix of the environment.
	 * @return the newly created fact.
	 */
	public static Fact addKBFact(Object obj, Class clazz,
			KnowledgeBase returnedKB, LogicFactory factory, String classPrefix) {
		Term[] terms = { factory.createConstantTerm(obj, clazz) };
		Fact fact = factory.createFact(StateSpec.getInstance()
				.getTypePredicate(clazz), terms);
		returnedKB.add(fact);
		return fact;
	}

	/**
	 * A JPredicate for the inequality clause. It is implicit that all differing
	 * terms are inequal and this predicate implements the test.
	 * 
	 * @param state
	 *            The current state.
	 * @param objA
	 *            The first object.
	 * @param objB
	 *            The second object.
	 * @return True if the objects are inequal.
	 */
	public boolean inequal(Object objA, Object objB) {
		if (objA.equals(objB))
			return false;
		return true;
	}
}
