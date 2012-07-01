package rrlFramework;

/**
 * An interface for a RRL agent.
 * 
 * @author Sam Sarjant
 */
public interface RRLAgent {

	/**
	 * Initialise the agent.
	 * 
	 * @param runIndex
	 *            The index of the run this agent is being initialised for.
	 */
	public void initialise(int runIndex);

	/**
	 * Checks if the agent is converged (does not need to learn anymore).
	 * 
	 * @return True if the agent is done learning.
	 */
	public boolean isLearningComplete();

	/**
	 * Cleans up any fields.
	 */
	public void cleanup();

	/**
	 * Receives the first observation of the episode and produces an action.
	 * 
	 * @param observations
	 *            The first observations of the episode.
	 * @return The actions the agent returns.
	 */
	public RRLActions startEpisode(RRLObservations observations);

	/**
	 * Receives observations throughout the episode, outputting actions.
	 * 
	 * @param observations
	 *            The observations throughout the episode.
	 * @return The actions the agent returns.
	 */
	public RRLActions stepEpisode(RRLObservations observations);

	/**
	 * The final observation for the episode
	 * 
	 * @param observations
	 */
	public void endEpisode(RRLObservations observations);

	/**
	 * Freezes/unfreezes the agent's behaviour so no more learning occurs.
	 * 
	 * @param If
	 *            the behaviour should be frozen.
	 */
	public void freeze(boolean b);

	/**
	 * Gets the number of episodes that the agent has been learning for.
	 * 
	 * @return The number of episodes that have passed for the agent so far.
	 */
	public int getNumEpisodes();

	/**
	 * If the agent is allowed to specialise new rules.
	 * 
	 * @param b
	 *            The setting of specialisations.
	 */
	public void setSpecialisations(boolean b);
}
