package relationalFramework.agentObservations;

import java.util.Collection;

import relationalFramework.StringFact;

/**
 * A class representing the beliefs the agent has regarding the inter-relations
 * between conditions in the environment.
 * 
 * @author Sam Sarjant
 */
public class ConditionBeliefs {
	/** The condition this class represents as a base. */
	private String condition_;

	/** The set of conditions that are always true when this condition is true. */
	private Collection<StringFact> alwaysTrue_;

	/** The set of conditions that are never true when this condition is true. */
	private Collection<StringFact> neverTrue_;

	/**
	 * The set of conditions that are occasionally true when this condition is
	 * true.
	 */
	private Collection<StringFact> occasionallyTrue_;
}
