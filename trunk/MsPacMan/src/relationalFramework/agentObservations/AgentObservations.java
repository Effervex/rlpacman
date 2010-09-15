package relationalFramework.agentObservations;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

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
	private int observationHash_;

	/** The agent's beliefs about the condition inter-relations. */
	private SortedSet<ConditionBeliefs> conditionBeliefs_;

	/** The inactivity counter for the condition beliefs. */
	private int conditionBeliefInactivity_ = 0;
	
	/** The observed invariants of the environment. */
	private Collection<String> invariants_;

	/** The rules about the environment learned by the agent. */
	private Collection<BackgroundKnowledge> learnedEnvironmentRules_;

	/** The action based observations. */
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
	 * Updates the observation hash code.
	 */
	private void updateHash() {
		observationHash_ = 0;
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
	}
}
