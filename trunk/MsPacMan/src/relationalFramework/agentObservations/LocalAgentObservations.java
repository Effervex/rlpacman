package relationalFramework.agentObservations;

import relationalFramework.GoalCondition;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

import util.MultiMap;

/**
 * A part of AgentObservations. The LocalAgentObservations contain observation
 * information regarding the local current goal. These are loaded and saved
 * serparately from the AgentObservations.
 * 
 * @author Sam Sarjant
 */
public class LocalAgentObservations implements Serializable {
	private static final long serialVersionUID = -7205802892263530446L;

	private static final String SERIALISATION_FILE = "localObservations.ser";

	public static final String LOCAL_GOAL_COND_FILE = "observedGoalFacts.txt";

	/** The local goal for this particular AgentObservations object. */
	private String localGoal_;

	/** The number of arguments this goal takes. */
	private int numGoalArgs_;

	/** The action conditions orientated towards the goal. */
	private MultiMap<String, RelationalPredicate> invariantGoalActionConditions_;

	/** The action conditions orientated towards the goal. */
	private MultiMap<String, RelationalPredicate> variantGoalActionConditions_;

	/** The observed predicates mentioning goal terms that have been true. */
	private MultiMap<String, RelationalPredicate> observedGoalPredicates_;

	/** The goal arguments that have been observed. */
	private Collection<GoalArg> observedGoalArgs_;

	/** The inactivity of the goal arguments collection. */
	private int goalArgsInactivity_ = 0;

	/** The last time the goal args were noted. */
	private int lastGoalArgsAdd_ = 0;

	/** The invariants relating to goal observations. */
	private InvariantObservations localInvariants_;

	/** The goal conditions tied to the observed goal predicates. */
	private Collection<GoalCondition> specificGoalConds_;

	/** A flag which denotes if these observations were loaded. */
	private boolean loaded_;

	/**
	 * The constructor for a new local goal object.
	 * 
	 * @param localGoal
	 *            The local goal these observations relate to.
	 */
	private LocalAgentObservations(String localGoal) {
		localGoal_ = localGoal;
		invariantGoalActionConditions_ = MultiMap.createSortedSetMultiMap();
		variantGoalActionConditions_ = MultiMap.createSortedSetMultiMap();
		observedGoalPredicates_ = MultiMap.createSortedSetMultiMap();
		observedGoalArgs_ = new TreeSet<GoalArg>();
		localInvariants_ = new InvariantObservations();
		loaded_ = false;
	}

	/**
	 * Initialises the action conditions if necessary.
	 * 
	 * @param goalActionConds
	 *            The goal action conditions to initialise with.
	 * @return True if the collections were initialised.
	 */
	public boolean initLocalActionConds(String action,
			Collection<RelationalPredicate> goalActionConds) {
		if (!invariantGoalActionConditions_.containsKey(action)) {
			invariantGoalActionConditions_.putCollection(action,
					goalActionConds);
			variantGoalActionConditions_.putCollection(action,
					new TreeSet<RelationalPredicate>());
			return true;
		}
		return false;
	}

	/**
	 * Adds a goal fact to the observed goal facts collection.
	 * 
	 * @param goalTerm
	 *            The goal term to add it under.
	 * @param goalFact
	 *            The goal fact to add (it may be negated).
	 * @return True if the fact was added, false if it was already there.
	 */
	public boolean addGoalFact(String goalTerm, RelationalPredicate goalFact) {
		boolean result = observedGoalPredicates_
				.putContains(goalTerm, goalFact);
		if (result)
			specificGoalConds_ = null;
		return result;
	}

	public void saveLocalObservations(File environmentDir) throws Exception {
		File localFile = new File(environmentDir, localGoal_
				+ File.separatorChar);
		localFile.mkdir();
		File serialisedFile = new File(localFile, SERIALISATION_FILE);
		serialisedFile.createNewFile();
		FileOutputStream fos = new FileOutputStream(serialisedFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(this);

		oos.close();
		fos.close();
	}

	public MultiMap<String, RelationalPredicate> getGoalPredicateMap() {
		return observedGoalPredicates_;
	}

	/**
	 * Notes the goal arguments down.
	 * 
	 * @param stateGoals
	 *            The goal arguments for the state to note.
	 */
	public boolean noteGoalArgs(Collection<GoalArg> stateGoals) {
		if (observedGoalArgs_.addAll(stateGoals)) {
			lastGoalArgsAdd_ = 0;
			goalArgsInactivity_ = 0;
			return true;
		} else {
			goalArgsInactivity_++;
			return false;
		}
	}

	public Collection<GoalArg> getObservedGoalArgs() {
		return observedGoalArgs_;
	}

	/**
	 * Checks if the agent should search all goal args this step based on local
	 * inactivity.
	 * 
	 * @return True if the agent should check all goal arguments.
	 */
	public boolean searchAllGoalArgs() {
		boolean result = lastGoalArgsAdd_ >= (Math.pow(2, goalArgsInactivity_) - 1);
		lastGoalArgsAdd_++;
		if (result)
			lastGoalArgsAdd_ = 0;
		return result;
	}

	/**
	 * Gets a collection of goal conditions formulated from the goal predicates
	 * which can be possible goals to achieve.
	 * 
	 * @return A collection of goal conditions.
	 */
	public Collection<GoalCondition> getSpecificGoalConditions() {
		if (specificGoalConds_ == null) {
			specificGoalConds_ = new HashSet<GoalCondition>();
			for (RelationalPredicate gc : observedGoalPredicates_.values()) {
				// If the fact isn't an invariant, isn't negated, and isn't the
				// goal args predicate, add it.
				if (!localInvariants_.getSpecificInvariants().contains(gc)
						&& !gc.isNegated()
						&& !gc.getFactName().equals(StateSpec.GOALARGS_PRED)
						&& gc.isFullyNotAnonymous()
						&& !GoalCondition.formName(gc).equals(localGoal_))
					specificGoalConds_.add(new GoalCondition(gc));
			}
		}
		return specificGoalConds_;
	}

	public Collection<RelationalPredicate> getInvariantGoalActionConditions(String action) {
		return invariantGoalActionConditions_.get(action);
	}

	public Collection<RelationalPredicate> getVariantGoalActionConditions(String action) {
		return variantGoalActionConditions_.get(action);
	}

	public boolean invariantActionsContains(String action, RelationalPredicate fact) {
		if (invariantGoalActionConditions_.containsKey(action))
			return invariantGoalActionConditions_.get(action).contains(fact);
		return false;
	}

	public boolean variantActionsContains(String action, RelationalPredicate fact) {
		if (variantGoalActionConditions_.containsKey(action))
			return variantGoalActionConditions_.get(action).contains(fact);
		return false;
	}

	public boolean noteInvariantConditions(Collection<RelationalPredicate> stateFacts,
			Map<String, String> goalReplacements) {
		if (goalReplacements != null)
			return localInvariants_
					.noteInvariants(stateFacts, goalReplacements);
		return false;
	}

	public InvariantObservations getConditionInvariants() {
		return localInvariants_;
	}

	public String getLocalGoalName() {
		return localGoal_;
	}

	public int getNumGoalArgs() {
		return numGoalArgs_;
	}

	public void setNumGoalArgs(int num) {
		numGoalArgs_ = num;
	}

	public boolean isLoaded() {
		return loaded_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((invariantGoalActionConditions_ == null) ? 0
						: invariantGoalActionConditions_.hashCode());
		result = prime * result
				+ ((localGoal_ == null) ? 0 : localGoal_.hashCode());
		result = prime
				* result
				+ ((observedGoalPredicates_ == null) ? 0
						: observedGoalPredicates_.hashCode());
		result = prime
				* result
				+ ((variantGoalActionConditions_ == null) ? 0
						: variantGoalActionConditions_.hashCode());
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
		LocalAgentObservations other = (LocalAgentObservations) obj;
		if (invariantGoalActionConditions_ == null) {
			if (other.invariantGoalActionConditions_ != null)
				return false;
		} else if (!invariantGoalActionConditions_
				.equals(other.invariantGoalActionConditions_))
			return false;
		if (localGoal_ == null) {
			if (other.localGoal_ != null)
				return false;
		} else if (!localGoal_.equals(other.localGoal_))
			return false;
		if (observedGoalPredicates_ == null) {
			if (other.observedGoalPredicates_ != null)
				return false;
		} else if (!observedGoalPredicates_
				.equals(other.observedGoalPredicates_))
			return false;
		if (variantGoalActionConditions_ == null) {
			if (other.variantGoalActionConditions_ != null)
				return false;
		} else if (!variantGoalActionConditions_
				.equals(other.variantGoalActionConditions_))
			return false;
		return true;
	}

	/**
	 * Loads the local agent observations from serialised file if possible.
	 * Otherwise, it just creates a new local agent observations object.
	 * 
	 * @param localGoal The local goal observations to load.
	 * @return The local agent observations object (loaded or new).
	 */
	public static LocalAgentObservations loadAgentObservations(String localGoal) {
		try {
			File localObsFile = getLocalFile(localGoal);
			if (localObsFile.exists()) {
				FileInputStream fis = new FileInputStream(localObsFile);
				ObjectInputStream ois = new ObjectInputStream(fis);
				LocalAgentObservations lao = (LocalAgentObservations) ois
						.readObject();
				if (lao != null) {
					lao.loaded_ = true;
					return lao;
				}
			}
		} catch (Exception e) {
		}
		System.out.println("No local agent observations to load.");
		return new LocalAgentObservations(localGoal);
	}

	private static File getLocalFile(String localGoal) {
		return new File(AgentObservations.AGENT_OBSERVATIONS_DIR, StateSpec
				.getInstance().getEnvironmentName()
				+ File.separatorChar
				+ localGoal + File.separatorChar + SERIALISATION_FILE);
	}

	public static LocalAgentObservations newAgentObservations(String localGoal) {
		return new LocalAgentObservations(localGoal);
	}

	/**
	 * Checks if local observations exist for a given goal.
	 * 
	 * @param goalName
	 *            The name of the goal to check for.
	 * @return True if there are local agent observations for the given goal.
	 */
	public static boolean observationsExist(String goalName) {
		return getLocalFile(goalName).exists();
	}
}
