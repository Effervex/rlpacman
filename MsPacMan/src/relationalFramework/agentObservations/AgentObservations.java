package relationalFramework.agentObservations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import jess.Fact;
import relationalFramework.MultiMap;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

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

	/** A transient group of facts indexed by terms used within. */
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
		observationHash_ = null;
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
						.getStringFact(split[0]), arguments);

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
	 * Updates the observation hash code. // TODO Make this only called from the
	 * mutation stage, so it is only updated once per iteration.
	 */
	private void updateHash() {
		if (observationHash_ == null) {
			// TODO Update the hash
		}
	}

	public int getObservationHash() {
		updateHash();
		return observationHash_;
	}

	public SortedSet<ConditionBeliefs> getConditionBeliefs() {
		return conditionBeliefs_;
	}

	public Collection<BackgroundKnowledge> getLearnedBackgroundKnowledge() {
		return learnedEnvironmentRules_;
	}

	public PreGoalInformation getPreGoal(String actionPred) {
		if (!actionBasedObservations_.containsKey(actionPred))
			return null;
		return actionBasedObservations_.get(actionPred).getPGI();
	}

	public void setPreGoal(String actionPred, PreGoalInformation preGoal) {
		getActionBasedObservation(actionPred).setPGI(preGoal);
		observationHash_ = null;
		updateHash();
	}

	public boolean hasPreGoal() {
		for (String action : actionBasedObservations_.keySet()) {
			if (actionBasedObservations_.get(action).getPGI() != null)
				return true;
		}
		return false;
	}

	public Collection<StringFact> getActionConditions(String actionPred) {
		if (!actionBasedObservations_.containsKey(actionPred))
			return null;
		return actionBasedObservations_.get(actionPred).getActionConditions();
	}

	public void setActionConditions(String action,
			Collection<StringFact> conditions) {
		getActionBasedObservation(action).setActionConditions(conditions);
		observationHash_ = null;
		updateHash();
	}

	public List<RangedCondition> getActionRange(String actionPred) {
		if (!actionBasedObservations_.containsKey(actionPred))
			return null;
		return actionBasedObservations_.get(actionPred).getActionRange();
	}

	public void setActionRange(String actionPred, RangedCondition rc) {
		getActionBasedObservation(actionPred).setActionRange(rc);
		observationHash_ = null;
		updateHash();
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
		updateHash();
	}

	/**
	 * Clears the observation hash for the agent observations so the hash code
	 * must be updated again.
	 */
	public void clearHash() {
		observationHash_ = null;
		updateHash();
	}

	public void clearPreGoal() {
		for (String action : actionBasedObservations_.keySet()) {
			actionBasedObservations_.get(action).setPGI(null);
		}
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
	 * An internal class to note the action-based observations.
	 * 
	 * @author Sam Sarjant
	 */
	private class ActionBasedObservations {
		/** The pre-goal information. */
		private PreGoalInformation pgi_;

		/** The conditions observed to have been true for the action. */
		private Collection<StringFact> actionConditions_;

		/** The inactivity of the action conditions. */
		private int actionConditionInactivity_ = 0;

		/** The maximum ranges of the conditions seen by this action. */
		private List<RangedCondition> conditionRanges_;

		/** The inactivity of the ranged conditions. */
		private int rangedConditionInactivity_ = 0;

		public PreGoalInformation getPGI() {
			return pgi_;
		}

		public void setPGI(PreGoalInformation preGoal) {
			pgi_ = preGoal;
		}

		public Collection<StringFact> getActionConditions() {
			return actionConditions_;
		}

		public void setActionConditions(Collection<StringFact> conditions) {
			actionConditions_ = conditions;
		}

		public List<RangedCondition> getActionRange() {
			return conditionRanges_;
		}

		public void setActionRange(RangedCondition rc) {
			if (conditionRanges_ == null)
				conditionRanges_ = new ArrayList<RangedCondition>();
			conditionRanges_.remove(rc);
			conditionRanges_.add(rc);
		}
	}
}
