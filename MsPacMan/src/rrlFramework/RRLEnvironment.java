package rrlFramework;

/**
 * An interface for a RRL environment.
 * 
 * @author Sam Sarjant
 */
public interface RRLEnvironment {

	/**
	 * Initialise the environment.
	 * 
	 * @param runIndex The index of the run this environment is being initialised for.
	 */
	public void initialise(int runIndex);

	/**
	 * Cleans up environment data.
	 */
	public void cleanup();

	/**
	 * Starts the episode by compiling the initial set of observations.
	 * 
	 * @return The first observations of the episode.
	 */
	public RRLObservations startEpisode();

	/**
	 * Accepts actions and applies them, then returns the resultant
	 * observations.
	 * 
	 * @param actions
	 *            The actions to apply to the environment.
	 * @return The resultant observations.
	 */
	public RRLObservations step(RRLActions actions);

}
