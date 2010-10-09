package relationalFramework.agentObservations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import relationalFramework.ActionChoice;
import relationalFramework.MultiMap;
import relationalFramework.RuleAction;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

import jess.Fact;

/**
 * A class for containing all environmental observations the agent makes while
 * learning. This class is used to help guide the agent's learning process.
 * 
 * @author Sam Sarjant
 */
public class AgentObservations {
	/** The amount of inactivity before an observation is considered converged. */
	public static final int INACTIVITY_THRESHOLD = 50;

	/** A hash code to track when an observation changes. */
	private Integer observationHash_ = null;

	/** The agent's beliefs about the condition inter-relations. */
	private SortedSet<ConditionBeliefs> conditionBeliefs_;

	/** The inactivity counter for the condition beliefs. */
	private int conditionBeliefInactivity_ = 0;

	/** The observed invariants of the environment. */
	private Collection<String> invariants_;

	/** The rules about the environment learned by the agent. */
	private Collection<BackgroundKnowledge> learnedEnvironmentRules_;

	/** A group of facts indexed by terms used within. */
	private MultiMap<String, StringFact> termMappedFacts_;

	/** The action based observations, keyed by action predicate. */
	private Map<String, ActionBasedObservations> actionBasedObservations_;

	/**
	 * The constructor for the agent observations.
	 */
	public AgentObservations() {
		conditionBeliefs_ = new TreeSet<ConditionBeliefs>();
		learnedEnvironmentRules_ = new HashSet<BackgroundKnowledge>();
		actionBasedObservations_ = new HashMap<String, ActionBasedObservations>();
		updateHash();
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
		// TODO Make condition notes.
		// If condition beliefs changed, signal for a hash update
		observationHash_ = null;
		updateHash();

		termMappedFacts_ = new MultiMap<String, StringFact>();
		for (Fact stateFact : state) {
			// Ignore the type, inequal and actions pred
			String[] split = StateSpec.splitFact(stateFact.toString());
			if (StateSpec.getInstance().isUsefulPredicate(split[0])) {
				String[] arguments = new String[split.length - 1];
				System.arraycopy(split, 1, arguments, 0, arguments.length);
				StringFact strFact = new StringFact(StateSpec.getInstance()
						.getPredicates().get(split[0]), arguments);

				// Run through the arguments and index the fact by term
				for (int i = 0; i < arguments.length; i++) {
					// Ignore numerical terms
					if (!StateSpec.isNumber(arguments[i]))
						termMappedFacts_.putContains(arguments[i], strFact);
				}
			}
		}
	}

	/**
	 * Gathers all relevant facts for a particular action and returns them.
	 * 
	 * @param action
	 *            The action (wit arguments).
	 * @return The relevant facts pertaining to the action.
	 */
	public Collection<StringFact> gatherActionFacts(StringFact action) {
		// Note down action conditions if still unsettled.
		observationHash_ = null;
		updateHash();
		
		Collection<StringFact> actionFacts = new HashSet<StringFact>();
		for (String argument : action.getArguments()) {
			List<StringFact> termFacts = termMappedFacts_.get(argument);
			if (termFacts != null) {
				for (StringFact termFact : termFacts) {
					if (!actionFacts.contains(termFact))
						actionFacts.add(termFact);
				}
			}
		}
		
		return actionFacts;
	}

	/**
	 * Updates the observation hash code.
	 */
	private void updateHash() {
		if (observationHash_ == null) {
			// Update the hash
		}
	}

	public int getObservationHash() {
		return observationHash_;
	}

	public SortedSet<ConditionBeliefs> getConditionBeliefs() {
		return conditionBeliefs_;
	}

	public Collection<BackgroundKnowledge> getLearnedBackgroundKnowledge() {
		return learnedEnvironmentRules_;
	}

	public Collection<String> formPreGoalState(Collection<Fact> preGoalState,
			ActionChoice actionChoice, List<String> constants) {
		for (RuleAction ruleAction : actionChoice.getActions()) {
			String actionPred = ruleAction.getRule().getActionPredicate();
			// If the state isn't yet settled, try unification
			if (!actionBasedObservations_.get(actionPred).getPGI().isSettled()) {
				List<String> actions = ruleAction.getUtilisedActions();

				// Create a pre-goal from every action in the actions list.
				if (actions != null) {
					for (String action : actions) {
						// Inversely substitute the old pregoal state
						String[] actionSplit = StateSpec.splitFact(action);
						String[] actionTerms = new String[actionSplit.length - 1];
						System.arraycopy(actionSplit, 1, actionTerms, 0,
								actionTerms.length);

						// The actions become constants if possible
						List<String> newConstants = new ArrayList<String>(
								constants);
						List<String> newStateTerms = new ArrayList<String>();
						for (String actionTerm : actionTerms) {
							newStateTerms.add(actionTerm);
							// Ignore numerical terms and terms already added
							if (!newConstants.contains(actionTerm))
								newConstants.add(actionTerm);
						}
						Collection<String> preGoalStringState = inverselySubstitute(
								preGoalState, actionSplit, newConstants);
						removeUselessFacts(preGoalStringState);

						// Unify with the old state
						PreGoalInformation preGoal = preGoals_
								.get(actionSplit[0]);
						if (preGoal == null) {
							preGoals_.put(actionSplit[0],
									new PreGoalInformation(
											(List<String>) preGoalStringState,
											newStateTerms));
							calculateMutationHash(actionSplit[0]);
						} else {
							// Unify the two states and check if it has changed
							// at all.
							int result = unifyStates(preGoal.getState(),
									preGoalStringState, preGoal
											.getActionTerms(), newStateTerms);

							// If the states unified, reset the counter,
							// otherwise increment.
							if (result == 1) {
								preGoal.resetInactivity();
								calculateMutationHash(actionSplit[0]);
							} else if (result == 0) {
								if (!formedActions.contains(actionPred))
									preGoal.incrementInactivity();
							} else if (result == -1) {
								throw new RuntimeException(
										"Pre-goal states did not unify: "
												+ preGoal + ", "
												+ preGoalStringState);
							}

							formedActions.add(actionPred);
						}
					}
				}
			}
		}
		
		Collection<String> settled = new ArrayList<String>();
		for (String preGoalAction : preGoals_.keySet()) {
			if (isPreGoalSettled(preGoalAction))
				settled.add(preGoalAction);
		}
		return settled;
	}

	/**
	 * An internal class to note the action-based observations.
	 * 
	 * @author Sam Sarjant
	 */
	private class ActionBasedObservations {
		/** The pre-goal information. */
		private PreGoalInformation pgi_;

		/** The conditions observed to have been true for the action. */
		private List<String> actionConditions_;

		/** The inactivity of the action conditions. */
		private int actionConditionInactivity_ = 0;

		/** The maximum ranges of the conditions seen by this action. */
		private List<RangedCondition> conditionRanges_;

		/** The inactivity of the ranged conditions. */
		private int rangedConditionInactivity_ = 0;

		public PreGoalInformation getPGI() {
			return pgi_;
		}
	}
}
