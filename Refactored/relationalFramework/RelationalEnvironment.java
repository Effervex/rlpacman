package relationalFramework;

import relationalFramework.PolicyActions;
import relationalFramework.StateSpec;

/**
 * The framework which transforms state observations into relational
 * observations and also transforms relational actions into low-level state
 * actions.
 * 
 * @author Sam Sarjant
 */
public abstract class RelationalEnvironment {
	/**
	 * Initialises the environment.
	 */
	public abstract void environmentInit();

	/**
	 * Forms the relational observations using the args as the input for
	 * relationalising.
	 * 
	 * @param args
	 *            The necessary state information.
	 * @return The Rete object of observations.
	 */
	public abstract RelationalObservation formObservations();

	/**
	 * Grounds a collection of relational actions into a single low-level object
	 * to act upon the state.
	 * 
	 * @param actions
	 *            The actions to ground.
	 * @param args
	 *            Any necessary state objects.
	 * @return The low-level action output.
	 */
	public abstract Object groundActions(PolicyActions actions);

	/**
	 * Takes action on the state.
	 * 
	 * @param actions
	 *            The action(s) to take.
	 * @return The reward received from taking the action.
	 */
	public abstract double takeAction(PolicyActions actions);

	/**
	 * Checks if the state is terminal.
	 * 
	 * @return 1 if terminal, 0 otherwise.
	 */
	public boolean isTerminal() {
		if (StateSpec.getInstance().isGoal(StateSpec.getInstance().getRete()))
			return true;
		return false;
	}

	/**
	 * Cleans up the environment.
	 */
	public abstract void environmentCleanup();
	
	/**
	 * Gets the maximum number of steps.
	 * 
	 * @return The maximum number of steps.
	 */
	public abstract int getMaxEpisodeSteps();
}
