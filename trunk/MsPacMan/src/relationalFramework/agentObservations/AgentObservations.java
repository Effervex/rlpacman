package relationalFramework.agentObservations;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

import jess.Fact;
import relationalFramework.Covering;
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
	// TODO Set this as a better measure
	public static final int INACTIVITY_THRESHOLD = 500;

	/** The agent observations directory. */
	public static final File AGENT_OBSERVATIONS_DIR = new File(
			"agentObservations/");

	/** A hash code to track when an observation changes. */
	private Integer observationHash_ = null;

	/** The agent's beliefs about the condition inter-relations. */
	private Map<String, ConditionBeliefs> conditionBeliefs_;

	/** The inactivity counter for the condition beliefs. */
	private int conditionBeliefInactivity_ = 0;

	/** The observed invariants of the environment. */
	private Collection<String> invariants_;

	/** The rules about the environment learned by the agent. */
	private SortedSet<BackgroundKnowledge> learnedEnvironmentRules_;

	/** A transient group of facts indexed by terms used within. */
	private MultiMap<String, StringFact> termMappedFacts_;

	/** The action based observations, keyed by action predicate. */
	private Map<String, ActionBasedObservations> actionBasedObservations_;

	/**
	 * The constructor for the agent observations.
	 */
	public AgentObservations() {
		conditionBeliefs_ = new TreeMap<String, ConditionBeliefs>();
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
				if (!StateSpec.getInstance().isTypePredicate(split[0]))
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
						// Don't include the fact itself or type facts in the
						// condition beliefs.
						if (!termFact.equals(baseFact)
								&& !StateSpec.getInstance().isTypePredicate(
										termFact.getFactName())) {
							StringFact relativeFact = new StringFact(termFact,
									replacementMap, false);
							relativeFacts.add(relativeFact);
						}
					}
				}
			}

			if (cb.noteTrueRelativeFacts(relativeFacts)) {
				changed = true;
				observationHash_ = null;
			}
		}

		if (changed) {
			learnedEnvironmentRules_ = formBackgroundKnowledge();
			conditionBeliefInactivity_ = 0;
		} else
			conditionBeliefInactivity_++;
	}

	/**
	 * Forms the background knowledge from the condition beliefs.
	 */
	private SortedSet<BackgroundKnowledge> formBackgroundKnowledge() {
		SortedSet<BackgroundKnowledge> backgroundKnowledge = new TreeSet<BackgroundKnowledge>();
		// Add the knowledge provided by the state specification
		backgroundKnowledge.addAll(StateSpec.getInstance()
				.getBackgroundKnowledgeConditions());

		// Run through every condition in the beliefs
		for (String cond : conditionBeliefs_.keySet()) {
			ConditionBeliefs cb = conditionBeliefs_.get(cond);
			StringFact cbFact = StateSpec.getInstance().getStringFact(
					cb.getCondition());
			String[] arguments = new String[cbFact.getArguments().length];
			for (int i = 0; i < arguments.length; i++)
				arguments[i] = Covering.getVariableTermString(i);
			cbFact = new StringFact(cbFact, arguments);

			// Assert the true facts
			for (StringFact alwaysTrue : cb.getAlwaysTrue()) {
				backgroundKnowledge.add(new BackgroundKnowledge(cbFact + " => "
						+ alwaysTrue, false));
			}

			// Assert the false facts
			for (StringFact neverTrue : cb.getNeverTrue()) {
				backgroundKnowledge.add(new BackgroundKnowledge(cbFact + " => "
						+ "(not " + neverTrue + ")", false));
			}
		}
		return backgroundKnowledge;
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
		Map<String, String> replacementMap = action
				.createVariableTermReplacementMap();
		Collection<StringFact> actionFacts = new HashSet<StringFact>();
		for (String argument : action.getArguments()) {
			List<StringFact> termFacts = termMappedFacts_.get(argument);
			if (termFacts != null) {
				Set<StringFact> actionConds = new HashSet<StringFact>();
				for (StringFact termFact : termFacts) {
					if (!actionFacts.contains(termFact))
						actionFacts.add(termFact);

					// If the action conditions have not settled, note the
					// action conditions.
					if (getActionBasedObservation(action.getFactName()).actionConditionInactivity_ < INACTIVITY_THRESHOLD) {
						// Ignore type predicates.
						if (!StateSpec.getInstance().isTypePredicate(
								termFact.getFactName())) {
							// Note the action condition
							StringFact actionCond = new StringFact(termFact,
									replacementMap, false);
							actionConds.add(actionCond);
						}
					}
				}
				if (!actionConds.isEmpty()
						&& getActionBasedObservation(action.getFactName())
								.addActionConditions(actionConds))
					observationHash_ = null;
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
		getActionBasedObservation(action).addActionConditions(conditions);
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
	public void saveAgentObservations(int run) {
		try {
			// Make the environment directory if necessary
			File environmentDir = new File(AGENT_OBSERVATIONS_DIR, StateSpec
					.getInstance().getEnvironmentName()
					+ File.separatorChar);
			environmentDir.mkdir();

			// Condition beliefs
			File condBeliefFile = new File(environmentDir,
					"conditionBeliefs.txt" + run);
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
					"actionConditions&Ranges.txt" + run);
			actionCondsFile.createNewFile();
			wr = new FileWriter(actionCondsFile);
			buf = new BufferedWriter(wr);

			// Write action conditions and ranges
			for (String action : actionBasedObservations_.keySet()) {
				buf.write(action + "\n");
				buf
						.write("True conditions: "
								+ actionBasedObservations_.get(action).actionConditions_
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
			File preGoalFile = new File(environmentDir, "preGoal.txt" + run);
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
		} catch (Exception e) {
			e.printStackTrace();
		}
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

		/**
		 * Adds action conditions to the action based observations.
		 * 
		 * @param actionConds
		 *            the action conditions being added.
		 * @return True if the set of action conditions changed because of the
		 *         addition.
		 */
		public boolean addActionConditions(Collection<StringFact> actionConds) {
			if (actionConditions_ == null) {
				actionConditions_ = new HashSet<StringFact>();
			}
			int numConds = actionConditions_.size();
			actionConditions_.addAll(actionConds);

			if ((actionConditions_.size() - numConds) != 0) {
				actionConditionInactivity_ = 0;
				return true;
			}

			actionConditionInactivity_++;
			return false;
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
					+ ((actionConditions_ == null) ? 0 : actionConditions_
							.hashCode());
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
			if (actionConditions_ == null) {
				if (other.actionConditions_ != null)
					return false;
			} else if (!actionConditions_.equals(other.actionConditions_))
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
