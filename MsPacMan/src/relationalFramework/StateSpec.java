package relationalFramework;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mandarax.kernel.ClauseSet;
import org.mandarax.kernel.ConstantTerm;
import org.mandarax.kernel.Fact;
import org.mandarax.kernel.InferenceEngine;
import org.mandarax.kernel.KnowledgeBase;
import org.mandarax.kernel.LogicFactory;
import org.mandarax.kernel.Predicate;
import org.mandarax.kernel.Prerequisite;
import org.mandarax.kernel.Query;
import org.mandarax.kernel.ResultSet;
import org.mandarax.kernel.SimplePredicate;
import org.mandarax.kernel.Term;
import org.mandarax.kernel.meta.JConstructor;
import org.mandarax.kernel.meta.JPredicate;

import rlPacMan.PacManStateSpec;

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

	/** The illegal fact. */
	private static Fact illegal_;

	/** The variable state term. */
	private static Term stateTerm_;

	/** The constant state spec term. */
	private static Term specTerm_;

	/** The predicate for handling inequality. */
	private static Prerequisite inequalityPred_;

	/** The prerequisites of the rules. */
	private List<GuidedPredicate> predicates_;

	/** The type predicates, only used implicitly. */
	private Map<Class, Predicate> typePredicates_;

	/** The actions of the rules. */
	private List<GuidedPredicate> actions_;

	/** A map for retrieving predicates by name. */
	private Map<String, GuidedPredicate> predByNames_;

	/** The state the agent must reach to successfully end the episode. */
	private org.mandarax.kernel.Rule goalState_;

	/** The constants found within the goal. */
	private ConstantTerm[] goalConstants_;

	/** The background knowledge regarding the predicates and actions. */
	private KnowledgeBase backgroundKnowledge_;

	/** The suffix to this class for use with dynamically loaded classes. */
	public static final String CLASS_SUFFIX = "StateSpec";

	/**
	 * The constructor for a state specification.
	 */
	protected void initialise(LogicFactory factory) {
		typePredicates_ = initialiseTypePredicates();
		predicates_ = initialisePredicates();
		actions_ = initialiseActions();
		predByNames_ = createPredNameMap();
		goalState_ = initialiseGoalState(factory);
		backgroundKnowledge_ = initialiseBackgroundKnowledge(factory);
		goalConstants_ = addGoalConstants(predicates_, goalState_);
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
	 * Initialises the goal state.
	 * 
	 * @return The rule that is true when it is the goal state,
	 */
	protected abstract org.mandarax.kernel.Rule initialiseGoalState(
			LogicFactory factory);

	/**
	 * Initialises the background knowledge.
	 * 
	 * @return The background knowledge base.
	 */
	protected abstract KnowledgeBase initialiseBackgroundKnowledge(
			LogicFactory factory);

	protected ConstantTerm[] addGoalConstants(List<GuidedPredicate> predicates,
			org.mandarax.kernel.Rule goalState) {
		List<Fact> body = goalState.getBody();
		Map<Class, Set<ConstantTerm>> constantMap = new HashMap<Class, Set<ConstantTerm>>();
		// For every fact in the body of the rule, extract the constants
		for (Fact fact : body) {
			Term[] factTerms = fact.getTerms();
			for (Term term : factTerms) {
				// Add any constant terms found.
				if (term.isConstant()) {
					Set<ConstantTerm> constants = constantMap.get(term
							.getType());
					if (constants == null) {
						constants = new HashSet<ConstantTerm>();
						constantMap.put(term.getType(), constants);
					}
					constants.add((ConstantTerm) term);
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

		return null;
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
			Set<ConstantTerm> constants) {
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

	public org.mandarax.kernel.Rule getGoalState() {
		return goalState_;
	}

	public KnowledgeBase getBackgroundKnowledge() {
		return backgroundKnowledge_;
	}

	public ConstantTerm[] getGoalConstants() {
		return goalConstants_;
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

	public GuidedPredicate getGuidedPredicate(String name) {
		if (name != null)
			return predByNames_.get(name);
		return null;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("StateSpec: ");
		buffer.append(predicates_.size() + " preds, ");
		buffer.append(typePredicates_.size() + " type preds, ");
		buffer.append(actions_.size() + " actions");
		return buffer.toString();
	}

	/**
	 * Checks if a formed predicate is valid against the background knowledge.
	 * 
	 * @param loosePred
	 *            The yet-to-be formed predicate.
	 * @param backgroundKnowledge
	 *            The background knowledge for the environment.
	 * @param factory
	 *            The LogicFactory in use
	 * @param ie
	 *            The inference engine in use.
	 * @return True if the predicate is valid, false if it is illegal or
	 *         redundant.
	 */
	public boolean isConditionValid(List condition, LogicFactory factory,
			InferenceEngine ie) {
		Query query = factory.createQuery((Fact[]) condition
				.toArray(new Fact[condition.size()]), "test");
		try {
			ResultSet rs = ie.query(query, backgroundKnowledge_,
					InferenceEngine.ONE, InferenceEngine.BUBBLE_EXCEPTIONS);
			while (rs.next()) {
				// I'm not sure...
				return false;
			}
		} catch (Exception e) {
		}
		return true;
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
		stateTypes[0] = Object[].class;
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
					new Class[] { Object[].class });
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
			stateTerm_ = factory.createVariableTerm("State", Object[].class);
		}
		return stateTerm_;
	}

	protected static Term getSpecTerm(LogicFactory factory) {
		if (specTerm_ == null) {
			specTerm_ = factory.createConstantTerm(getInstance());
		}
		return specTerm_;
	}
	
	protected static Prerequisite getInequalityPredicate(LogicFactory factory) {
		if (inequalityPred_ == null) {
//			Class[] types = {
//			Method method = StateSpec.class.getMethod("inequal", types);
//			Predicate inequalPred = new JPredicate(method);
//			inequalityPred_ = factory.createPrerequisite(aPredicate, terms, false);
		}
		return inequalityPred_;
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
	
	public boolean inequal(Object[] state, Object objA, Object objB) {
		if (objA.equals(objB))
			return true;
		return false;
	}
}
