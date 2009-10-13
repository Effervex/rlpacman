package relationalFramework;

import java.util.List;
import java.util.Map;

import org.mandarax.kernel.ConstantTerm;
import org.mandarax.kernel.Fact;
import org.mandarax.kernel.KnowledgeBase;
import org.mandarax.kernel.LogicFactory;
import org.mandarax.kernel.Predicate;
import org.mandarax.kernel.SimplePredicate;
import org.mandarax.kernel.Term;

/**
 * A class to outline the specifications of the environment.
 * 
 * @author Sam Sarjant
 */
public abstract class StateSpec {
	/** The singleton instance. */
	private static StateSpec instance_;
	
	/** The prerequisites of the rules. */
	private List<GuidedPredicate> predicates_;

	/** The type predicates, only used implicitly. */
	private Map<Class, Predicate> typePredicates_;

	/** The actions of the rules. */
	private List<GuidedPredicate> actions_;

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
		goalState_ = initialiseGoalState(factory);
		backgroundKnowledge_ = initialiseBackgroundKnowledge(factory);
		goalConstants_ = addGoalConstants(predicates_, goalState_);
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
	protected abstract org.mandarax.kernel.Rule initialiseGoalState(LogicFactory factory);

	/**
	 * Initialises the background knowledge.
	 * 
	 * @return The background knowledge base.
	 */
	protected abstract KnowledgeBase initialiseBackgroundKnowledge(LogicFactory factory);

	/**
	 * Extracts and adds the goal constants to the current predicates, if they
	 * are of he correct types. Returns the goal constants.
	 * 
	 * @return The goal constants.
	 */
	protected abstract ConstantTerm[] addGoalConstants(
			List<GuidedPredicate> predicates, org.mandarax.kernel.Rule goalState);

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
	
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("StateSpec: ");
		buffer.append(predicates_.size() + " preds, ");
		buffer.append(typePredicates_.size() + " type preds, ");
		buffer.append(actions_.size() + " actions");
		return buffer.toString();
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
		Predicate termPred = new SimplePredicate("terminal", new Class[0]);
		return factory.createFact(termPred, new Term[0]);
	}

	/**
	 * Gets the singleton instance of the state spec.
	 * 
	 * @return The instance.
	 */
	public static StateSpec getInstance() {
		return instance_;
	}

	public static StateSpec initInstance(String classPrefix, LogicFactory factory) {
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
		for (int i = 1; i < terms.length; i++) {
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
}
