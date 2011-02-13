package relationalFramework.agentObservations;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import jess.Fact;
import relationalFramework.ConditionComparator;
import relationalFramework.RuleCreation;
import relationalFramework.MultiMap;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

/**
 * A class for containing all environmental observations the agent makes while
 * learning. This class is used to help guide the agent's learning process.
 * 
 * @author Sam Sarjant
 */
public class AgentObservations implements Serializable {
	private static final long serialVersionUID = 790371205436473937L;

	private static final String ACTION_CONDITIONS_FILE = "actionConditions&Ranges.txt";

	private static final String CONDITION_BELIEF_FILE = "conditionBeliefs.txt";

	private static final String PREGOAL_FILE = "preGoal.txt";

	private static final String SERIALISATION_FILE = "agentObservations.ser";

	/** The amount of inactivity before an observation is considered converged. */
	// TODO Set this as a better measure
	public static final int INACTIVITY_THRESHOLD = 500;

	/** The agent observations directory. */
	public static final File AGENT_OBSERVATIONS_DIR = new File(
			"agentObservations/");

	/** A hash code to track when an observation changes. */
	private transient Integer observationHash_ = null;

	/** The agent's beliefs about the condition inter-relations. */
	private Map<String, ConditionBeliefs> conditionBeliefs_;

	/**
	 * The agent's beliefs about the negated condition inter-relations (when
	 * conditions AREN'T true), ordered using a second mapping of argument index
	 */
	private Map<String, Map<Byte, ConditionBeliefs>> negatedConditionBeliefs_;

	/** The inactivity counter for the condition beliefs. */
	private int conditionBeliefInactivity_ = 0;

	// TODO Note facts which are always true or untrue and can be left out of
	// learned knowledge
	/** The observed invariants of the environment. */
	private ConditionBeliefs invariants_;

	/** The rules about the environment learned by the agent. */
	private SortedSet<BackgroundKnowledge> learnedEnvironmentRules_;

	/**
	 * The same rules organised into a mapping based on what predicates are
	 * present in the rule.
	 */
	private MultiMap<String, BackgroundKnowledge> mappedEnvironmentRules_;

	/** A transient group of facts indexed by terms used within. */
	private transient MultiMap<String, StringFact> termMappedFacts_;

	/** The action based observations, keyed by action predicate. */
	private Map<String, ActionBasedObservations> actionBasedObservations_;

	/**
	 * The constructor for the agent observations.
	 */
	public AgentObservations() {
		conditionBeliefs_ = new TreeMap<String, ConditionBeliefs>();
		negatedConditionBeliefs_ = new TreeMap<String, Map<Byte, ConditionBeliefs>>();
		actionBasedObservations_ = new HashMap<String, ActionBasedObservations>();
		observationHash_ = null;
		learnedEnvironmentRules_ = formBackgroundKnowledge();

		AGENT_OBSERVATIONS_DIR.mkdir();
	}

	/**
	 * A method for scanning the state and assigning facts to term based maps.
	 * During the scan, condition inter-relations are noted (is not yet
	 * settled).
	 * 
	 * @param state
	 *            The state in raw fact form.
	 * @return The mapping of terms to facts.
	 */
	public void scanState(Collection<Fact> state) {
		// Run through the facts, adding to term mapped facts and adding the raw
		// facts for condition belief scanning.
		Collection<StringFact> stateFacts = new ArrayList<StringFact>();
		termMappedFacts_ = new MultiMap<String, StringFact>();
		for (Fact stateFact : state) {
			// Ignore the type, inequal and actions pred
			String[] split = StateSpec.splitFact(stateFact.toString());
			if (StateSpec.getInstance().isUsefulPredicate(split[0])) {
				String[] arguments = new String[split.length - 1];
				System.arraycopy(split, 1, arguments, 0, arguments.length);
				StringFact strFact = new StringFact(StateSpec.getInstance()
						.getStringFact(split[0]), arguments);

				stateFacts.add(strFact);

				// Run through the arguments and index the fact by term
				for (int i = 0; i < arguments.length; i++) {
					// Ignore numerical terms
					if (!StateSpec.isNumber(arguments[i]))
						termMappedFacts_.putContains(arguments[i], strFact);
				}
			}
		}

		// Use the term mapped facts to generate collections of true facts.
		recordConditionAssociations(stateFacts);
	}

	/**
	 * Records all conditions associations from the current state using the term
	 * mapped facts to form the relations.
	 * 
	 * @param stateFacts
	 *            The facts of the state in StringFact form.
	 */
	private void recordConditionAssociations(Collection<StringFact> stateFacts) {
		if (conditionBeliefInactivity_ >= INACTIVITY_THRESHOLD)
			return;

		boolean changed = false;
		for (StringFact baseFact : stateFacts) {
			// Getting the ConditionBeliefs object
			ConditionBeliefs cb = conditionBeliefs_.get(baseFact.getFactName());
			if (cb == null) {
				cb = new ConditionBeliefs(baseFact.getFactName());
				conditionBeliefs_.put(baseFact.getFactName(), cb);
				observationHash_ = null;
			}

			// Create a replacement map here (excluding numerical values)
			Map<String, String> replacementMap = baseFact
					.createVariableTermReplacementMap();

			// Replace facts for all relevant facts and store as condition
			// beliefs.
			Collection<StringFact> relativeFacts = new HashSet<StringFact>();
			for (String term : baseFact.getArguments()) {
				Collection<StringFact> termFacts = termMappedFacts_.get(term);
				if (termFacts != null) {
					for (StringFact termFact : termFacts) {
						StringFact relativeFact = new StringFact(termFact);
						relativeFact.replaceArguments(replacementMap, false);
						relativeFacts.add(relativeFact);
					}
				}
			}

			// Note the relative facts for the main pred and for every
			// negated pred not present
			Collection<StringFact> notRelativeFacts = new HashSet<StringFact>();
			if (cb.noteTrueRelativeFacts(relativeFacts, notRelativeFacts, true)) {
				changed = true;
				observationHash_ = null;
			}

			// Note the invariants
//			if (invariants_ == null)
//				invariants_ = new ConditionBeliefs("invariants");
//			invariants_.noteFacts(cb.getAlwaysTrue(), cb.getNeverTrue(), cb
//					.getOccasionallyTrue());

			// Form the condition beliefs for the not relative facts, using the
			// relative facts as always true values (only for non-types
			// predicates)
			// if (!StateSpec.getInstance()
			// .isTypePredicate(baseFact.getFactName()))
			changed |= recordUntrueConditionAssociations(relativeFacts,
					notRelativeFacts);
		}

		if (changed) {
			learnedEnvironmentRules_ = formBackgroundKnowledge();
			conditionBeliefInactivity_ = 0;
		} else
			conditionBeliefInactivity_++;
	}

	/**
	 * Notes the condition associations for all conditions that are untrue for a
	 * particular term.
	 * 
	 * @param trueRelativeFacts
	 *            The currently true relative facts.
	 * @param falseRelativeFacts
	 *            The currently false relative facts.
	 * @return True if the condition beliefs changed.
	 */
	private boolean recordUntrueConditionAssociations(
			Collection<StringFact> trueRelativeFacts,
			Collection<StringFact> falseRelativeFacts) {
		boolean changed = false;
		for (StringFact untrueFact : falseRelativeFacts) {
			String factName = untrueFact.getFactName();

			// Getting the ConditionBeliefs object
			Map<Byte, ConditionBeliefs> untrueCBs = negatedConditionBeliefs_
					.get(factName);
			if (untrueCBs == null) {
				untrueCBs = new HashMap<Byte, ConditionBeliefs>();
				negatedConditionBeliefs_.put(factName, untrueCBs);
			}

			// Create a separate condition beliefs object for each non-anonymous
			// argument position
			String[] untrueFactArgs = untrueFact.getArguments();
			byte argState = determineArgState(untrueFact);

			ConditionBeliefs untrueCB = untrueCBs.get(argState);
			if (untrueCB == null) {
				untrueCB = new ConditionBeliefs(factName, argState);
				untrueCBs.put(argState, untrueCB);
				observationHash_ = null;
			}

			// Make the untrue fact the 'main' fact (align
			// variables properly with replacement map)
			Map<String, String> replacementMap = new HashMap<String, String>();
			for (int i = 0; i < untrueFactArgs.length; i++) {
				if (!untrueFactArgs[i].equals("?"))
					replacementMap.put(untrueFactArgs[i], RuleCreation
							.getVariableTermString(i));
			}

			Collection<StringFact> modTrueRelativeFacts = new HashSet<StringFact>();
			for (StringFact trueFact : trueRelativeFacts) {
				StringFact modTrueFact = new StringFact(trueFact);
				if (modTrueFact.replaceArguments(replacementMap, false))
					modTrueRelativeFacts.add(modTrueFact);
			}

			// Note the relative facts for untrue conditions.
			if (untrueCB.noteTrueRelativeFacts(modTrueRelativeFacts, null,
					false)) {
				changed = true;
				observationHash_ = null;
			}
		}

		return changed;
	}

	/**
	 * Determines the state of a string fact by encoding whether an argument is
	 * non-anonymous using bitwise indexing operations.
	 * 
	 * @param fact
	 *            The fact being analysed.
	 * @return A byte >= 0 representing the fact's arguments state.
	 */
	private byte determineArgState(StringFact fact) {
		byte state = 0;
		String[] factArgs = fact.getArguments();
		for (int argIndex = 0; argIndex < factArgs.length; argIndex++) {
			if (!factArgs[argIndex].equals("?"))
				state += 1 << argIndex;
		}
		return state;
	}

	/**
	 * Forms the background knowledge from the condition beliefs.
	 */
	private SortedSet<BackgroundKnowledge> formBackgroundKnowledge() {
		SortedSet<BackgroundKnowledge> backgroundKnowledge = new TreeSet<BackgroundKnowledge>();

		// Add the knowledge provided by the state specification
		Collection<BackgroundKnowledge> stateSpecKnowledge = StateSpec
				.getInstance().getBackgroundKnowledgeConditions();
		backgroundKnowledge.addAll(stateSpecKnowledge);
		mappedEnvironmentRules_ = new MultiMap<String, BackgroundKnowledge>();
		for (BackgroundKnowledge bckKnow : stateSpecKnowledge) {
			Collection<String> relevantPreds = bckKnow.getRelevantPreds();
			for (String pred : relevantPreds)
				mappedEnvironmentRules_.putContains(pred, bckKnow);
		}

		// Run through every condition in the beliefs
		// Form equivalence (<=>) relations wherever possible
		for (String cond : conditionBeliefs_.keySet()) {
			ConditionBeliefs cb = conditionBeliefs_.get(cond);
			StringFact cbFact = StateSpec.getInstance().getStringFact(
					cb.getCondition());
			String[] arguments = new String[cbFact.getArguments().length];
			for (int i = 0; i < arguments.length; i++)
				arguments[i] = RuleCreation.getVariableTermString(i);
			cbFact = new StringFact(cbFact, arguments);

			// Assert the true facts
			for (StringFact alwaysTrue : cb.getAlwaysTrue()) {
				// Check for equivalence relation
				// Don't note single type relations unless this is a type
				if (StateSpec.getInstance().isTypePredicate(cond)
						|| !StateSpec.getInstance().isTypePredicate(
								alwaysTrue.getFactName()))
					createRelation(cbFact, alwaysTrue, true,
							backgroundKnowledge);
			}

			// Assert the false facts
			for (StringFact neverTrue : cb.getNeverTrue()) {
				createRelation(cbFact, neverTrue, false, backgroundKnowledge);
			}
		}
		return backgroundKnowledge;
	}

	/**
	 * Creates the background knowledge to represent the condition relations.
	 * This knowledge may be an equivalence relation if the relations between
	 * conditions have equivalences.
	 * 
	 * @param cond
	 *            The positive, usually left-side condition.
	 * @param otherCond
	 *            The possibly negated, usually right-side condition.
	 * @param negationType
	 *            The state of negation: true if not negated, false if negated.
	 * @param relations
	 *            The set of knowledge to add to.
	 */
	@SuppressWarnings("unchecked")
	private void createRelation(StringFact cond, StringFact otherCond,
			boolean negationType, SortedSet<BackgroundKnowledge> relations) {
		// Don't make rules to itself.
		if (cond.equals(otherCond))
			return;
		StringFact left = cond;
		StringFact right = otherCond;
		String relation = " => ";

		// Create the basic inference relation
		BackgroundKnowledge bckKnow = null;
		if (negationType)
			bckKnow = new BackgroundKnowledge(left + relation + right, false);
		else
			bckKnow = new BackgroundKnowledge(left + relation + "(not " + right
					+ ")", false);
		mappedEnvironmentRules_.putContains(left.getFactName(), bckKnow);
		relations.add(bckKnow);

		// Create an equivalence relation if possible.
		// Get the relations to use for equivalency comparisons
		Collection<StringFact> otherRelations = getOtherConditionRelations(
				otherCond, negationType);

		// Don't bother with self-condition relations
		if (!otherCond.getFactName().equals(cond.getFactName())
				&& otherRelations != null && !otherRelations.isEmpty()) {
			// Replace arguments for this fact to match those seen in the
			// otherCond (anonymise variables)
			BidiMap replacementMap = new DualHashBidiMap();
			String[] otherArgs = otherCond.getArguments();
			for (int i = 0; i < otherArgs.length; i++) {
				if (!otherArgs[i].equals("?"))
					replacementMap.put(otherArgs[i], RuleCreation
							.getVariableTermString(i));
			}

			StringFact modCond = new StringFact(cond);
			modCond.replaceArguments(replacementMap, false);
			// If the fact is present, then there is an equivalency!
			if (otherRelations.contains(modCond)) {
				// Change the modArg back to its original arguments (though not
				// changing anonymous values)
				modCond
						.replaceArguments(replacementMap.inverseBidiMap(),
								false);

				left = modCond;
				// Order the two facts with the simplest fact on the LHS
				if (negationType && modCond.compareTo(otherCond) > 0) {
					left = otherCond;
					right = modCond;
				}
				relation = " <=> ";

				if (negationType)
					bckKnow = new BackgroundKnowledge(left + relation + right,
							false);
				else
					bckKnow = new BackgroundKnowledge(left + relation + "(not "
							+ right + ")", false);
				mappedEnvironmentRules_
						.putContains(left.getFactName(), bckKnow);
				mappedEnvironmentRules_.putContains(right.getFactName(),
						bckKnow);
				relations.add(bckKnow);
			}
		}
	}

	/**
	 * Gets the relations or the other condition's condition beliefs to
	 * cross-compare between the sets for equivalence checking.
	 * 
	 * @param otherCond
	 *            The other condition.
	 * @param negationType
	 *            The state of negation: true if not negated, false if negated.
	 * @return The set of facts used for comparing with equivalence.
	 */
	private Collection<StringFact> getOtherConditionRelations(
			StringFact otherCond, boolean negationType) {
		String factName = otherCond.getFactName();
		if (negationType) {
			// No negation - simply return the set given by the fact name
			if (conditionBeliefs_.containsKey(factName))
				return conditionBeliefs_.get(factName).getAlwaysTrue();
		} else {
			// Negation - return the appropriate conjunction of condition
			// beliefs
			if (negatedConditionBeliefs_.containsKey(factName)) {
				Map<Byte, ConditionBeliefs> negatedCBs = negatedConditionBeliefs_
						.get(factName);
				byte argState = determineArgState(otherCond);
				if (negatedCBs.containsKey(argState))
					return negatedCBs.get(argState).getAlwaysTrue();
			}
		}
		return null;
	}

	/**
	 * Gathers all relevant facts for a particular action and returns them.
	 * 
	 * @param action
	 *            The action (with arguments).
	 * @param onlyActionTerms
	 *            If the method should anonymise non-action terms.
	 * @return The relevant facts pertaining to the action.
	 */
	public Collection<StringFact> gatherActionFacts(StringFact action,
			boolean onlyActionTerms) {
		// Note down action conditions if still unsettled.
		Map<String, String> replacementMap = action
				.createVariableTermReplacementMap();
		Collection<StringFact> actionFacts = new HashSet<StringFact>();
		Set<StringFact> actionConds = new HashSet<StringFact>();
		for (String argument : action.getArguments()) {
			if (!StateSpec.isNumber(argument)) {
				List<StringFact> termFacts = termMappedFacts_.get(argument);
				for (StringFact termFact : termFacts) {
					StringFact notedFact = termFact;
					if (onlyActionTerms) {
						notedFact = new StringFact(termFact);
						notedFact.retainArguments(action.getArguments());
					}

					if (!actionFacts.contains(notedFact))
						actionFacts.add(notedFact);

					// If the action conditions have not settled, note the
					// action conditions.
					if (getActionBasedObservation(action.getFactName()).actionConditionInactivity_ < INACTIVITY_THRESHOLD) {
						// Note the action condition
						StringFact actionCond = new StringFact(termFact);
						actionCond.replaceArguments(replacementMap, false);
						actionConds.add(actionCond);
					}
				}
			}
		}
		if (!actionConds.isEmpty()
				&& getActionBasedObservation(action.getFactName())
						.addActionConditions(actionConds))
			observationHash_ = null;

		return actionFacts;
	}

	/**
	 * Simplifies a set of facts using the learned background knowledge.
	 * 
	 * @param simplified
	 *            The facts to be simplified.
	 * @param testForIllegalRule
	 *            If the procedure is also testing for illegal rules.
	 * @param onlyEquivalencies
	 *            If only equivalent rules should be tested.
	 * @return True if the condition was simplified.
	 */
	public boolean simplifyRule(SortedSet<StringFact> simplified,
			boolean testForIllegalRule, boolean onlyEquivalencies) {
		boolean changedOverall = false;
		// Note which facts have already been tested, so changes don't restart
		// the process.
		SortedSet<StringFact> testedFacts = new TreeSet<StringFact>(simplified
				.comparator());
		boolean changedThisIter = true;

		// Check each fact for simplifications, and check new facts when they're
		// added
		while (changedThisIter) {
			SortedSet<BackgroundKnowledge> testedBackground = new TreeSet<BackgroundKnowledge>();
			changedThisIter = false;
			// Iterate through the facts in the conditions
			for (StringFact fact : new TreeSet<StringFact>(simplified)) {
				// Only test untested facts, facts still present in
				// simplified and facts associated with rules.
				if (!testedFacts.contains(fact)
						&& simplified.contains(fact)
						&& mappedEnvironmentRules_.containsKey(fact
								.getFactName())) {
					// Test against every background knowledge regarding this
					// fact pred.
					for (BackgroundKnowledge bckKnow : mappedEnvironmentRules_
							.get(fact.getFactName())) {
						// If the knowledge hasn't been tested and is an
						// equivalence if only testing equivalences
						if (!testedBackground.contains(bckKnow)
								&& (!onlyEquivalencies || bckKnow
										.isEquivalence())) {
							// If the simplification process changes things
							if (bckKnow
									.simplify(simplified, testForIllegalRule)) {
								changedThisIter = true;
								testedBackground.clear();
							} else
								testedBackground.add(bckKnow);
						}
					}
					testedFacts.add(fact);
				}
			}
			changedOverall |= changedThisIter;
		}

		return changedOverall;
	}

	/**
	 * Updates the observation hash code.
	 */
	private void updateHash() {
		if (observationHash_ == null) {
			// Update the hash
			final int prime = 31;
			observationHash_ = 1;

			// Note the condition beliefs - the invariants and background
			// knowledge depend on it anyway.
			int subresult = 1;
			for (ConditionBeliefs cb : conditionBeliefs_.values())
				subresult = prime * subresult + cb.hashCode();
			observationHash_ = prime * observationHash_ + subresult;

			// Note the pre goal, action conditions and condition ranges
			observationHash_ = prime * observationHash_
					+ actionBasedObservations_.hashCode();
		}
	}

	public int getObservationHash() {
		updateHash();
		return observationHash_;
	}

	public Map<String, ConditionBeliefs> getConditionBeliefs() {
		return conditionBeliefs_;
	}

	public Map<String, Map<Byte, ConditionBeliefs>> getNegatedConditionBeliefs() {
		return negatedConditionBeliefs_;
	}

	public Collection<BackgroundKnowledge> getLearnedBackgroundKnowledge() {
		return learnedEnvironmentRules_;
	}

	public void setBackgroundKnowledge(SortedSet<BackgroundKnowledge> backKnow) {
		learnedEnvironmentRules_ = backKnow;
	}

	public PreGoalInformation getPreGoal(String actionPred) {
		if (!actionBasedObservations_.containsKey(actionPred))
			return null;
		return actionBasedObservations_.get(actionPred).getPGI();
	}

	public void setPreGoal(String actionPred, PreGoalInformation preGoal) {
		getActionBasedObservation(actionPred).setPGI(preGoal);
		observationHash_ = null;
	}

	public boolean hasPreGoal() {
		for (String action : actionBasedObservations_.keySet()) {
			if (actionBasedObservations_.get(action).getPGI() != null)
				return true;
		}
		return false;
	}

	public Collection<StringFact> getSpecialisationConditions(String actionPred) {
		if (!actionBasedObservations_.containsKey(actionPred))
			return null;
		return actionBasedObservations_.get(actionPred)
				.getSpecialisationConditions();
	}

	public void setActionConditions(String action,
			Collection<StringFact> conditions) {
		getActionBasedObservation(action).setActionConditions(conditions);
		observationHash_ = null;
	}

	public List<RangedCondition> getActionRange(String actionPred) {
		if (!actionBasedObservations_.containsKey(actionPred))
			return null;
		return actionBasedObservations_.get(actionPred).getActionRange();
	}

	public void setActionRange(String actionPred, RangedCondition rc) {
		if (getActionBasedObservation(actionPred).setActionRange(rc))
			observationHash_ = null;
	}

	/**
	 * Backs up the pre-goals into a single mapping and removes them from the
	 * observations so a different learning algorithm can use the agent
	 * observations.
	 * 
	 * @return The backed up pre-goals.
	 */
	public Map<String, PreGoalInformation> backupAndClearPreGoals() {
		Map<String, PreGoalInformation> backups = new HashMap<String, PreGoalInformation>();
		for (String action : actionBasedObservations_.keySet()) {
			backups.put(action, actionBasedObservations_.get(action).getPGI());
			actionBasedObservations_.get(action).setPGI(null);
		}

		return backups;
	}

	/**
	 * Loads a backed up mapping of pregoals into the agent observations.
	 * 
	 * @param backupPreGoals
	 *            The backup pre-goals being loaded.
	 */
	public void loadPreGoals(Map<String, PreGoalInformation> backupPreGoals) {
		for (String key : backupPreGoals.keySet()) {
			getActionBasedObservation(key).setPGI(backupPreGoals.get(key));
		}
		observationHash_ = null;
	}

	/**
	 * Clears the observation hash for the agent observations so the hash code
	 * must be updated again.
	 */
	public void clearHash() {
		observationHash_ = null;
	}

	public void clearPreGoal() {
		for (String action : actionBasedObservations_.keySet()) {
			actionBasedObservations_.get(action).setPGI(null);
		}
	}

	public void resetInactivity() {
		conditionBeliefInactivity_ = 0;
		for (ActionBasedObservations abo : actionBasedObservations_.values()) {
			abo.actionConditionInactivity_ = 0;
			abo.rangedConditionInactivity_ = 0;
		}
	}

	public boolean isConditionBeliefsSettled() {
		return (conditionBeliefInactivity_ >= INACTIVITY_THRESHOLD);
	}

	public boolean isActionConditionSettled() {
		for (ActionBasedObservations abo : actionBasedObservations_.values()) {
			if (abo.actionConditionInactivity_ < INACTIVITY_THRESHOLD)
				return false;
		}
		return true;
	}

	public boolean isRangedConditionSettled() {
		for (ActionBasedObservations abo : actionBasedObservations_.values()) {
			if ((abo.conditionRanges_ != null)
					&& (abo.rangedConditionInactivity_ < INACTIVITY_THRESHOLD))
				return false;
		}
		return true;
	}

	/**
	 * Gets/Initialises the action based observation for the action given.
	 * 
	 * @param actionPred
	 *            The action predicate.
	 * @return The ActionBasedObservation object assigned to the action or a new
	 *         one.
	 */
	private ActionBasedObservations getActionBasedObservation(String actionPred) {
		ActionBasedObservations abo = actionBasedObservations_.get(actionPred);
		if (abo == null) {
			abo = new ActionBasedObservations();
			actionBasedObservations_.put(actionPred, abo);
		}
		return abo;
	}

	/**
	 * Saves agent observations to file in the agent observations directory.
	 */
	public void saveAgentObservations() {
		try {
			// Make the environment directory if necessary
			File environmentDir = new File(AGENT_OBSERVATIONS_DIR, StateSpec
					.getInstance().getEnvironmentName()
					+ File.separatorChar);
			environmentDir.mkdir();

			// Condition beliefs
			File condBeliefFile = new File(environmentDir,
					CONDITION_BELIEF_FILE);
			condBeliefFile.createNewFile();
			FileWriter wr = new FileWriter(condBeliefFile);
			BufferedWriter buf = new BufferedWriter(wr);

			// Write condition beliefs
			for (String condition : conditionBeliefs_.keySet()) {
				buf.write(conditionBeliefs_.get(condition) + "\n");
			}
			buf.write("\n");
			// TODO Write invariants
			buf.write("Background Knowledge\n");
			for (BackgroundKnowledge bk : learnedEnvironmentRules_) {
				buf.write(bk.toString() + "\n");
			}

			buf.close();
			wr.close();

			// Action Conditions and ranges
			File actionCondsFile = new File(environmentDir,
					ACTION_CONDITIONS_FILE);
			actionCondsFile.createNewFile();
			wr = new FileWriter(actionCondsFile);
			buf = new BufferedWriter(wr);

			// Write action conditions and ranges
			for (String action : actionBasedObservations_.keySet()) {
				buf.write(action + "\n");
				buf
						.write("True conditions: "
								+ actionBasedObservations_.get(action).specialisationConditions_
								+ "\n");
				if (actionBasedObservations_.get(action).conditionRanges_ != null)
					buf
							.write("Ranged values: "
									+ actionBasedObservations_.get(action).conditionRanges_
									+ "\n");
				buf.write("\n");
			}

			buf.close();
			wr.close();

			// Pre goal saving
			File preGoalFile = new File(environmentDir, PREGOAL_FILE);
			preGoalFile.createNewFile();
			wr = new FileWriter(preGoalFile);
			buf = new BufferedWriter(wr);

			for (String action : actionBasedObservations_.keySet()) {
				PreGoalInformation pgi = actionBasedObservations_.get(action)
						.getPGI();
				if (pgi != null) {
					buf.write(action + "\n");
					buf.write(pgi.toString() + "\n");
				}
			}

			buf.close();
			wr.close();

			// Serialise observations
			File serialisedFile = new File(environmentDir, SERIALISATION_FILE);
			serialisedFile.createNewFile();
			FileOutputStream fos = new FileOutputStream(serialisedFile);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this);

			oos.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static AgentObservations loadAgentObservations() {
		try {
			File environmentDir = new File(AGENT_OBSERVATIONS_DIR, StateSpec
					.getInstance().getEnvironmentName()
					+ File.separatorChar);
			File serialisedFile = new File(environmentDir, SERIALISATION_FILE);
			if (serialisedFile.exists()) {
				FileInputStream fis = new FileInputStream(serialisedFile);
				ObjectInputStream ois = new ObjectInputStream(fis);
				return (AgentObservations) ois.readObject();
			}
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * An internal class to note the action-based observations.
	 * 
	 * @author Sam Sarjant
	 */
	private class ActionBasedObservations implements Serializable {
		private static final long serialVersionUID = 1167175828246036292L;

		/** The pre-goal information. */
		private transient PreGoalInformation pgi_;

		/** The conditions observed to always be true for the action. */
		private Collection<StringFact> invariantActionConditions_;

		/** The conditions observed to sometimes be true for the action. */
		private Collection<StringFact> variantActionConditions_;

		/**
		 * The conditions that can be added to rules for specialisation.
		 * Essentially the variant action conditions both negated and normal,
		 * then simplified.
		 */
		private Collection<StringFact> specialisationConditions_;

		/** The inactivity of the action conditions. */
		private int actionConditionInactivity_ = 0;

		/** The maximum ranges of the conditions seen by this action. */
		private List<RangedCondition> conditionRanges_;

		/** The inactivity of the ranged conditions. */
		private int rangedConditionInactivity_ = 0;

		public PreGoalInformation getPGI() {
			return pgi_;
		}

		public void setActionConditions(Collection<StringFact> conditions) {
			specialisationConditions_ = new HashSet<StringFact>(conditions);
		}

		public void setPGI(PreGoalInformation preGoal) {
			pgi_ = preGoal;
		}

		public Collection<StringFact> getSpecialisationConditions() {
			return specialisationConditions_;
		}

		/**
		 * Adds action conditions to the action based observations.
		 * 
		 * @param actionConds
		 *            the action conditions being added.
		 * @return True if the set of action conditions changed because of the
		 *         addition.
		 */
		public boolean addActionConditions(Collection<StringFact> actionConds) {
			// If this is the first action condition, then all the actions are
			// considered invariant.
			if (invariantActionConditions_ == null) {
				invariantActionConditions_ = new HashSet<StringFact>(
						actionConds);
				variantActionConditions_ = new HashSet<StringFact>();
				specialisationConditions_ = new HashSet<StringFact>();
				actionConditionInactivity_ = 0;
				return true;
			}

			int hash = variantActionConditions_.hashCode();
			variantActionConditions_.addAll(actionConds);
			variantActionConditions_.addAll(invariantActionConditions_);
			boolean changed = invariantActionConditions_.retainAll(actionConds);
			variantActionConditions_.removeAll(invariantActionConditions_);

			changed |= (hash != variantActionConditions_.hashCode());

			if (changed) {
				specialisationConditions_ = recreateSpecialisations(
						variantActionConditions_, invariantActionConditions_);
				actionConditionInactivity_ = 0;
				return true;
			}

			actionConditionInactivity_++;
			return false;
		}

		/**
		 * Recreates the set of specialisation conditions, which are basically
		 * the variant conditions, both negated and normal, and simplified to
		 * exclude the invariant and illegal conditions.
		 * 
		 * @param variantActionConditions
		 *            The action conditions not ALWAYS true for the action, but
		 *            not false either.
		 * @param invariantActionConditions
		 *            The action conditions ALWAYS true for the action.
		 * @return The collection of specialisation conditions, not containing
		 *         any conditions in the invariants, and not containing any
		 *         conditions not in either invariants or variants.
		 */
		private Collection<StringFact> recreateSpecialisations(
				Collection<StringFact> variantActionConditions,
				Collection<StringFact> invariantActionConditions) {
			Collection<StringFact> specialisations = new HashSet<StringFact>();
			for (StringFact condition : variantActionConditions) {
				// Check the non-negated version
				condition = simplifyCondition(condition);
				if (condition != null) {
					specialisations.add(condition);

					// Check the negated version (only for non-types)
					if (!StateSpec.getInstance().isTypePredicate(
							condition.getFactName())) {
						StringFact negCondition = new StringFact(condition);
						negCondition.swapNegated();
						negCondition = simplifyCondition(negCondition);
						if (negCondition != null)
							specialisations.add(negCondition);
					}
				}
			}
			return specialisations;
		}

		/**
		 * Simplifies a single condition using equivalence rules.
		 * 
		 * @param condition
		 *            The condition to simplify.
		 * @return The simplified condition (or the condition itself).
		 */
		private StringFact simplifyCondition(StringFact condition) {
			SortedSet<StringFact> set = new TreeSet<StringFact>(
					ConditionComparator.getInstance());
			set.add(condition);
			if (simplifyRule(set, false, true)) {
				StringFact simplify = set.first();
				// If the condition is not in the invariants and if it's not
				// negated, IS in the variants, keep it.
				if (!invariantActionConditions_.contains(simplify)) {
					if (simplify.isNegated()
							|| variantActionConditions_.contains(simplify))
						return simplify;
				}
			} else
				return condition;
			return null;
		}

		public List<RangedCondition> getActionRange() {
			return conditionRanges_;
		}

		/**
		 * Sets the action range to the specified value.
		 * 
		 * @param rc
		 *            The ranged condition to set.
		 * @return True if the condition range changed, false otherwise.
		 */
		public boolean setActionRange(RangedCondition rc) {
			RangedCondition oldCond = null;
			if (conditionRanges_ == null)
				conditionRanges_ = new ArrayList<RangedCondition>();
			else {
				int index = conditionRanges_.indexOf(rc);
				if (index != -1)
					oldCond = conditionRanges_.remove(index);
			}
			conditionRanges_.add(rc);

			if (!rc.equalRange(oldCond)) {
				rangedConditionInactivity_ = 0;
				return true;
			}
			rangedConditionInactivity_++;
			return false;
		}

		private AgentObservations getOuterType() {
			return AgentObservations.this;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime
					* result
					+ ((variantActionConditions_ == null) ? 0
							: variantActionConditions_.hashCode());
			result = prime
					* result
					+ ((conditionRanges_ == null) ? 0 : conditionRanges_
							.hashCode());
			result = prime * result + ((pgi_ == null) ? 0 : pgi_.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ActionBasedObservations other = (ActionBasedObservations) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (variantActionConditions_ == null) {
				if (other.variantActionConditions_ != null)
					return false;
			} else if (!variantActionConditions_
					.equals(other.variantActionConditions_))
				return false;
			if (conditionRanges_ == null) {
				if (other.conditionRanges_ != null)
					return false;
			} else if (!conditionRanges_.equals(other.conditionRanges_))
				return false;
			if (pgi_ == null) {
				if (other.pgi_ != null)
					return false;
			} else if (!pgi_.equals(other.pgi_))
				return false;
			return true;
		}
	}
}
