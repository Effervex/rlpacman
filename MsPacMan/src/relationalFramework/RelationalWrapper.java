package relationalFramework;

import jess.Rete;

/**
 * The framework which transforms state observations into relational
 * observations and also transforms relational actions into low-level state
 * actions.
 * 
 * @author Sam Sarjant
 */
public abstract class RelationalWrapper {
	
	/**
	 * Forms the relational observations using the args as the input for relationalising.
	 * 
	 * @param args The necessary state information.
	 * @return The Rete object of observations.
	 */
	public abstract Rete formObservations(Object... args);
	
	/**
	 * Grounds a collection of relational actions into a single low-level object to act upon the state.
	 * 
	 * @param actions The actions to ground.
	 * @param args Any necessary state objects.
	 * @return The low-level action output.
	 */
	public abstract Object groundActions(PolicyActions actions, Object... args);
	
	/**
	 * Checks if the state is terminal.
	 * 
	 * @param args The state parameters.
	 * @return 1 if terminal, 0 otherwise.
	 */
	public int isTerminal(Object... args) {
		if (StateSpec.getInstance().isGoal(StateSpec.getInstance().getRete()))
			return 1;
		return 0;
	}
}
