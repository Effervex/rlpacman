package relationalFramework;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jess.Rete;

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
	public static final String INFERS_ACTION = "=>";

	public static final char ANONYMOUS = '_';

	public static final String ACTION_PRECOND_SUFFIX = "PreCond";

	public static final String VALID_ACTIONS = "validActions";

	/** The singleton instance. */
	private static StateSpec instance_;

	/** The rete rulebase. */
	private Rete ruleBase_;

	/** The prerequisites of the rules and their structure. */
	private MultiMap<String, Class> predicates_;

	/** The type predicates, only used implicitly. */
	private Map<Class, String> typePredicates_;

	/** The actions of the rules and their structure. */
	private MultiMap<String, Class> actions_;

	/** The number of simultaneous actions per step to take. */
	private int actionNum_;

	/** The rules relating to an actions precondition. */
	private Map<String, String> actionPreconditions_;

	/** A map for retrieving predicates by name. */
	private Map<String, GuidedPredicate> predByNames_;

	/** The state the agent must reach to successfully end the episode. */
	private String goalState_;

	/** The constants found within the goal. */
	private List<String> constants_;

	/** The name of the goal. */
	protected String goal_;

	/** The optimal policy for the goal. */
	private Policy optimalPolicy_;

	/** The LogicFactory for the experiment. */
	private Rete rete_;

	/** A RegExp for filtering out unnecessary facts from a rule. */
	private String unnecessaries_;

	/**
	 * The constructor for a state specification.
	 */
	private final void initialise(LogicFactory factory) {
		try {
			rete_ = new Rete();

			// Type predicates
			typePredicates_ = initialiseTypePredicateTemplates();
			for (String typeName : typePredicates_.values()) {
				defineTemplate(typeName, rete_);
			}

			// Main predicates
			predicates_ = initialisePredicateTemplates();
			for (String predName : predicates_.keySet()) {
				defineTemplate(predName, rete_);
			}

			// Set up the valid actions template
			StringBuffer actBuf = new StringBuffer("(deftemplate "
					+ VALID_ACTIONS);
			// Actions
			actions_ = initialiseActionTemplates();
			for (String actName : actions_.keySet()) {
				defineTemplate(actName, rete_);
				actBuf.append(" (multislot " + actName + ")");
			}
			actBuf.append(")");
			rete_.eval(actBuf.toString());
			actionNum_ = initialiseActionsPerStep();

			// Initialise the goal state rules
			constants_ = new ArrayList<String>();
			goalState_ = initialiseGoalState(constants_);
			rete_.eval("(deftemplate goalState (slot goalMet))");
			rete_.eval("(defrule goalState " + goalState_
					+ " => (assert (goal (goalMet TRUE))))");

			// Initialise the background knowledge rules
			Map<String, String> backgroundRules = initialiseBackgroundKnowledge();
			for (String ruleNames : backgroundRules.keySet()) {
				rete_.eval("(defrule " + ruleNames + " "
						+ backgroundRules.get(ruleNames) + ")");
			}

			// Initialise the queries for determining action preconditions
			Map<String, String> purePreConds = initialiseActionPreconditions();
			actionPreconditions_ = new HashMap<String, String>();
			for (String action : purePreConds.keySet()) {
				String query = "(defquery " + action + ACTION_PRECOND_SUFFIX
						+ " " + purePreConds.get(action) + ")";
				rete_.eval(query);
				actionPreconditions_.put(action, purePreConds.get(action));
			}

			unnecessaries_ = formUnnecessaryString(typePredicates_.values());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Forms the unnecessary facts regexp string.
	 * 
	 * @param types The types that need not be in rules.
	 * @return The regexp string.
	 */
	private String formUnnecessaryString(Collection<String> types) {
		StringBuffer buffer = new StringBuffer("((\\(test \\(<> .+?\\)\\))");
		for (String type : types)
			buffer.append("|(\\(" + type + " .+?\\))");
		buffer.append(") ");
		return buffer.toString();
	}

	/**
	 * Initialises the state type predicates.
	 * 
	 * @param rete
	 *            The rete object.
	 * @return A mapping of classes to guided predicate names.
	 */
	protected abstract Map<Class, String> initialiseTypePredicateTemplates();

	/**
	 * Initialises the state predicates.
	 * 
	 * @return The list of guided predicate names.
	 */
	protected abstract MultiMap<String, Class> initialisePredicateTemplates();

	/**
	 * Initialises the state actions.
	 * 
	 * @return The list of guided actions.
	 */
	protected abstract MultiMap<String, Class> initialiseActionTemplates();

	/**
	 * Initialises the number of actions to take per time step.
	 * 
	 * @return The number of actions to take per step.
	 */
	protected abstract int initialiseActionsPerStep();

	/**
	 * Initialises the rules for finding valid actions.
	 * 
	 * @return A map of actions and the pure preconditions to be valid.
	 */
	protected abstract Map<String, String> initialiseActionPreconditions();

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
	 * Initialises the background knowledge.
	 * 
	 * @return A mapping of rule names to the pure rules themselves (just pre =>
	 *         post).
	 */
	protected abstract Map<String, String> initialiseBackgroundKnowledge();

	/**
	 * Parses a rule from a human readable string. the string is in the format
	 * '[(predicate [ arg]) ]+ => (predicate [ arg])'.
	 * 
	 * Generally, lowercase variables are constants, while ?uppercase variables
	 * are variables. Also, anonymous variables are allowed in the body. These
	 * are like fully generalised variables in that anonymous variables may or
	 * may not equal each other.
	 * 
	 * @param rule
	 *            The string representation of the rule.
	 * @return An instantiated rule.
	 */
	public final String parseRule(String rule) {
		StringRule stringRule = extractInfo(rule);

		// Organise the rule back into a string
		StringBuffer buffer = new StringBuffer();
		// Main preds first
		for (String[] fact : stringRule.getMainConditions()) {
			buffer.append("(" + fact[0]);
			for (int i = 1; i < fact.length; i++) {
				buffer.append(" " + fact[i]);
			}
			buffer.append(") ");
		}

		// Inequals tests
		buffer.append(stringRule.createInequalsTests());

		// Type preds
		for (String[] typeFact : stringRule.getTypeConditions()) {
			buffer.append("(" + typeFact[0]);
			for (int i = 1; i < typeFact.length; i++) {
				buffer.append(" " + typeFact[i]);
			}
			buffer.append(") ");
		}

		// Action
		String[] action = stringRule.getAction();
		buffer.append(INFERS_ACTION + " (" + action[0]);
		for (int i = 1; i < action.length; i++) {
			buffer.append(" " + action[i]);
		}
		buffer.append(")");

		return buffer.toString();
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
	private final StringRule extractInfo(String rule) {
		// Split the rule into conditions and actions
		String[] split = rule.split(INFERS_ACTION);
		if (split.length < 2)
			return null;

		StringRule info = new StringRule();
		for (int i = 0; i < split.length; i++) {
			String predicate = split[i];

			// A Regexp looking for a predicate with args '(clear a)'
			// '(move b ?X)' '(cool)' '(on ?X _)'
			Pattern p = null;
			if (i == 0) {
				p = Pattern.compile("\\((\\w+)((?: (?:(?:\\w+)|(?:\\?\\w+)|(?:"
						+ ANONYMOUS + ")))*)\\)");
			} else {
				p = Pattern
						.compile("\\((\\w+)((?: (?:(?:\\w+)|(?:\\?\\w+)))*)\\)");
			}

			Matcher m = p.matcher(predicate);
			while (m.find()) {
				// Group 1 is the predicate name, Group 2 is the arg(s)
				String arguments = m.group(2).trim();
				String[] args = arguments.split(" ");
				String[] fact = new String[args.length + 1];
				fact[0] = m.group(1);
				for (int j = 0; j < args.length; j++) {
					fact[j + 1] = args[j];
				}

				if (i == 0)
					info.addCondition(fact);
				else
					info.setAction(fact);
			}
		}

		return info;
	}

	/**
	 * Creates a string representation of a rule, in a light, easy-to-read, and
	 * parsable format.
	 * 
	 * @param rule
	 *            The rule begin output.
	 * @return A light, parsable representation of the rule.
	 */
	public final String encodeRule(String rule) {
		String replacement = rule.replaceAll(unnecessaries_, "");
		if (replacement.split(INFERS_ACTION)[0].isEmpty())
			return rule;
		return replacement;
	}

	public MultiMap<String, Class> getPredicates() {
		return predicates_;
	}

	public MultiMap<String, Class> getActions() {
		return actions_;
	}

	public int getNumActions() {
		return actionNum_;
	}

	public String getGoalState() {
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

	public List<String> getConstants() {
		return constants_;
	}

	/**
	 * Gets the type predicate using the given key, if such a predicate exists.
	 * 
	 * @return The predicate associated with the class, or null if no such class
	 *         key.
	 */
	public String getTypePredicate(Class key) {
		return typePredicates_.get(key);
	}

	public boolean isTypePredicate(String predicate) {
		if (typePredicates_.values().contains(predicate))
			return true;
		return false;
	}

	/**
	 * Checks if the predicate is useful (not type, action or inequal).
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
	 * Creates a template for a fact using the given arguments and asserts it to
	 * the rete object. Returns the name of the fact template.
	 * 
	 * @param factName
	 *            The name of the fact template.
	 * @param rete
	 *            The rete object being asserted to.
	 * @return The name of the fact template.
	 */
	protected String defineTemplate(String factName, Rete rete) {
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
					classPrefix + StateSpec.class.getSimpleName())
					.newInstance();
			instance_.initialise(factory);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return instance_;
	}

	/**
	 * A class for storing string rule information and organising it properly.
	 * 
	 * @author Samuel J. Sarjant
	 */
	private class StringRule {
		private List<String[]> mainConditions_;
		private List<String[]> typeConditions_;
		private String[] action_;
		private List<String> terms_;
		private int anonVariables = 0;

		public StringRule() {
			mainConditions_ = new ArrayList<String[]>();
			typeConditions_ = new ArrayList<String[]>();
			terms_ = new ArrayList<String>();
		}

		/**
		 * Adds a conditions to the string rule. If the rule is not a type
		 * predicate, types are added automatically. Terms are also added
		 * automatically.
		 * 
		 * @param fact
		 *            The condition being added with the pred at the head, and
		 *            the args as later elements.
		 */
		public void addCondition(String[] fact) {
			// Convert anonymous symbols to variables
			convertAnonymous(fact);

			// If the condition is a main predicate
			if (predicates_.keySet().contains(fact[0])) {
				mainConditions_.add(fact);
				addTerms(fact);
				addTypeConds(fact);
			} else if (isTypePredicate(fact[0])) {
				// Type predicate
				addTerms(fact);
				addTypeConds(fact);
			}
		}

		/**
		 * Converts any anonymous symbols to numbered variables.
		 * 
		 * @param fact
		 *            The fact being converted.
		 */
		private void convertAnonymous(String[] fact) {
			for (int i = 1; i < fact.length; i++) {
				if (fact[i].equals(ANONYMOUS + ""))
					fact[i] = "?" + ANONYMOUS + anonVariables++;
			}
		}

		/**
		 * Adds terms to the rule from a fact.
		 * 
		 * @param fact
		 *            The fact.
		 */
		private void addTerms(String[] fact) {
			// Ignore the first argument
			for (int i = 1; i < fact.length; i++) {
				// If the term is anonymous, convert it
				String term = fact[i];
				if (!terms_.contains(term))
					terms_.add(term);
			}
		}

		/**
		 * Adds type predicates to the rule from a fact if they are not already
		 * there.
		 * 
		 * @param fact
		 *            The fact.
		 */
		private void addTypeConds(String[] fact) {
			if (isTypePredicate(fact[0])) {
				addContainsArray(typeConditions_, fact);
				return;
			}
			List<Class> classes = predicates_.get(fact[0]);
			for (int i = 1; i < fact.length; i++) {
				String[] typePred = new String[2];
				typePred[0] = typePredicates_.get(classes.get(i - 1));
				typePred[1] = fact[i];
				addContainsArray(typeConditions_, typePred);
			}
		}

		/**
		 * Creates the inequals tests from the terms stored. Note anonymous
		 * terms are special in that they aren't inequal to one-another.
		 * 
		 * @return The string for detailing inequality '(test (<> ?X a b ?Y
		 *         ?_0)) (test (<> ?Y a ...))'
		 */
		public String createInequalsTests() {
			StringBuffer buffer = new StringBuffer();
			// Run through each term
			List<String> constants = new ArrayList<String>();
			for (int i = 0; i < terms_.size(); i++) {
				// If the term is a variable, assert an inequals
				if (terms_.get(i).charAt(0) == '?') {
					boolean isAnon = (terms_.get(i).charAt(1) == ANONYMOUS) ? true
							: false;

					StringBuffer subBuffer = new StringBuffer();
					boolean isValid = false;
					// The base term
					subBuffer.append("(test (<> " + terms_.get(i));
					// The constants already seen
					for (String constant : constants) {
						subBuffer.append(" " + constant);
						isValid = true;
					}

					// Later terms seen (excluding anonymous if this term is
					// anonymous
					for (int j = i + 1; j < terms_.size(); j++) {
						// If the current variable isn't anonymous or if it is,
						// the later term isn't anonymous, add it to inequality
						String laterTerm = terms_.get(j);
						if ((!isAnon) || (laterTerm.charAt(0) == '?')
								|| (laterTerm.charAt(1) == ANONYMOUS)) {
							subBuffer.append(" " + laterTerm);
							isValid = true;
						}
					}

					subBuffer.append(")) ");

					// If we have a valid inequality expression
					if (isValid)
						buffer.append(subBuffer);
				} else {
					// Add the constant to the list of constants
					constants.add(terms_.get(i));
				}
			}
			return buffer.toString();
		}

		public String[] getAction() {
			return action_;
		}

		public void setAction(String[] action) {
			action_ = action;
		}

		public List<String[]> getMainConditions() {
			return mainConditions_;
		}

		public List<String[]> getTypeConditions() {
			return typeConditions_;
		}
	}
}
