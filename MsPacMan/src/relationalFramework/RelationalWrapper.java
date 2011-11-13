package relationalFramework;

import relationalFramework.PolicyActions;
import relationalFramework.StateSpec;
import jess.Rete;

/**
 * The framework which transforms state observations into relational
 * observations and also transforms relational actions into low-level state
 * actions.
 * 
 * @author Sam Sarjant
 */
public abstract class RelationalWrapper {
	protected Object[] prevState_;

	/**
	 * Forms the relational observations using the args as the input for
	 * relationalising.
	 * 
	 * @param args
	 *            The necessary state information.
	 * @return The Rete object of observations or null if the state is invalid.
	 */
	public final Rete formObservations(Object... args) {
		Rete rete = StateSpec.getInstance().getRete();
		try {
			// TODO Change this to assert/retract rather than reset
			rete.reset();

			rete = assertStateFacts(rete, args);
			if (rete == null)
				return null;
			rete.run();

			ObjectObservations.getInstance().validActions = StateSpec
					.getInstance().generateValidActions(rete);
		} catch (Exception e) {
			e.printStackTrace();
		}
		prevState_ = args;
		return rete;
	}

	/**
	 * Performs the actual asserting/retracting of facts based on the args
	 * given. The valid actions and background knowledge are added outside of
	 * this method.
	 * 
	 * @param rete
	 *            The rete object to assert/retract the facts.
	 * @param args
	 *            The args given to form a state.
	 * @return The rete object with all non-background knowledge current
	 *         observations asserted.
	 */
	protected abstract Rete assertStateFacts(Rete rete, Object... args) throws Exception;

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
	public abstract Object groundActions(PolicyActions actions, Object... args);

	/**
	 * Checks if the state is terminal.
	 * 
	 * @param args
	 *            The state parameters.
	 * @return 1 if terminal, 0 otherwise.
	 */
	public int isTerminal(Object... args) {
		if (StateSpec.getInstance().isGoal(StateSpec.getInstance().getRete()))
			return 1;
		return 0;
	}
}
