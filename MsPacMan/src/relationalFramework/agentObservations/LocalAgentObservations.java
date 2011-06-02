package relationalFramework.agentObservations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;

import relationalFramework.MultiMap;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

/**
 * A part of AgentObservations. The LocalAgentObservations contain observation
 * information regarding the local current goal. These are loaded and saved
 * serparately from the AgentObservations.
 * 
 * @author Sam Sarjant
 */
public class LocalAgentObservations implements Serializable {
	private static final long serialVersionUID = 5224766802305732980L;

	private static final String SERIALISATION_FILE = "localObservations.ser";

	/** The local goal for this particular AgentObservations object. */
	private String localGoal_;

	/** The general goal args pred to add to rules. */
	private StringFact goalArgsPred_;

	/** The action conditions orientated towards the goal. */
	private MultiMap<String, StringFact> invariantGoalActionConditions_;

	/** The action conditions orientated towards the goal. */
	private MultiMap<String, StringFact> variantGoalActionConditions_;

	/** The observed predicates mentioning goal terms that have been true. */
	private MultiMap<String, StringFact> observedGoalPredicates_;

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
	}

	/**
	 * Notes down the goal predicate structure if necessary.
	 * 
	 * @param goalFact
	 *            The goal fact.
	 * @param goalReplacements
	 *            The replacements for the goal terms.
	 */
	public void noteGoalPred(StringFact strFact,
			Map<String, String> goalReplacements) {
		if (goalArgsPred_ == null
				|| !strFact.getArguments()[0].equals(goalArgsPred_
						.getArguments()[0])) {
			goalArgsPred_ = new StringFact(strFact);
			goalArgsPred_.replaceArguments(goalReplacements, true);
			// Add it to the observed goal predicates for
			// unification purposes
			for (String term : goalReplacements.values())
				observedGoalPredicates_.put(term, goalArgsPred_);
		}
	}

	/**
	 * Initialises the action conditions if necessary.
	 * 
	 * @param goalActionConds
	 *            The goal action conditions to initialise with.
	 * @return True if the collections were initialised.
	 */
	public boolean initLocalActionConds(String action,
			Collection<StringFact> goalActionConds) {
		if (!invariantGoalActionConditions_.containsKey(action)) {
			invariantGoalActionConditions_.putCollection(action,
					goalActionConds);
			variantGoalActionConditions_.putCollection(action,
					new TreeSet<StringFact>());
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
	public boolean addGoalFact(String goalTerm, StringFact goalFact) {
		return observedGoalPredicates_.putContains(goalTerm, goalFact);
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

	public StringFact getGoalArgsPred() {
		return goalArgsPred_;
	}

	public MultiMap<String, StringFact> getGoalPredicateMap() {
		return observedGoalPredicates_;
	}

	public Collection<StringFact> getInvariantGoalActionConditions(String action) {
		return invariantGoalActionConditions_.get(action);
	}

	public Collection<StringFact> getVariantGoalActionConditions(String action) {
		return variantGoalActionConditions_.get(action);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((goalArgsPred_ == null) ? 0 : goalArgsPred_.hashCode());
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
		if (goalArgsPred_ == null) {
			if (other.goalArgsPred_ != null)
				return false;
		} else if (!goalArgsPred_.equals(other.goalArgsPred_))
			return false;
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
	 * @return The local agent observations object (loaded or new).
	 */
	public static LocalAgentObservations loadAgentObservations() {
		String localGoal = StateSpec.getInstance().getGoalName();
		try {
			// TODO Load two files: the global environment one and the smaller
			// local one (if available).
			File localObsFile = new File(
					AgentObservations.AGENT_OBSERVATIONS_DIR, StateSpec
							.getInstance().getEnvironmentName()
							+ File.separatorChar
							+ localGoal
							+ File.separatorChar + SERIALISATION_FILE);
			if (localObsFile.exists()) {
				FileInputStream fis = new FileInputStream(localObsFile);
				ObjectInputStream ois = new ObjectInputStream(fis);
				LocalAgentObservations lao = (LocalAgentObservations) ois
						.readObject();
				if (lao != null)
					return lao;
			}
		} catch (Exception e) {
		}
		System.out.println("No local agent observations to load.");
		return new LocalAgentObservations(localGoal);
	}
}
