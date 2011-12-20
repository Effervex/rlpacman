package relationalFramework;

import java.util.List;

import org.apache.commons.collections.BidiMap;

import relationalFramework.PolicyActions;
import relationalFramework.StateSpec;
import rrlFramework.RRLObservations;
import jess.Rete;

/**
 * The framework which transforms state observations into relational
 * observations and also transforms relational actions into low-level state
 * actions.
 * 
 * @author Sam Sarjant
 */
public abstract class RelationalWrapper {
	/** If this state is the first state in the episode. */
	private boolean firstState_;

	/** If the environment is updated by Rete or scanning the state. */
	private boolean reteDriven_;

	/** Notes the previous state. */
	protected Object[] prevState_;

	public RelationalWrapper() {
		reteDriven_ = isReteDriven();
	}

	/**
	 * Determines how the Rete object is asserted to: via interacting rules or
	 * continual state scans.
	 * 
	 * @return True if the environment uses Rete rules to update state or false
	 *         if states are updated by scans.
	 */
	protected abstract boolean isReteDriven();

	/**
	 * Performs the actual asserting/retracting of facts based on the args
	 * given. The valid actions and background knowledge are added outside of
	 * this method.
	 * 
	 * @param rete
	 *            The rete object to assert/retract the facts.
	 * @param goalArgs
	 *            The goal arguments (if any).
	 * @param args
	 *            The args given to form a state.
	 * @return The goal replacement map.
	 */
	protected abstract BidiMap assertStateFacts(Rete rete,
			List<String> goalArgs, Object... args) throws Exception;

	/**
	 * Checks if the current state is the first state in the episode.
	 * 
	 * @return True if it's the first state in the episode.
	 */
	protected boolean isFirstStateInEpisode() {
		return firstState_;
	}

	/**
	 * Forms the relational observations using the args as the input for
	 * relationalising.
	 * 
	 * @param goalArgs
	 *            The goal arguments given for the episode.
	 * @param args
	 *            The necessary state information.
	 * @return The Rete object of observations or null if the state is invalid.
	 */
	public final RRLObservations formObservations(List<String> goalArgs,
			Object... args) {
		Rete rete = StateSpec.getInstance().getRete();
		BidiMap goalReplacementMap = null;
		try {
			if (!reteDriven_ || firstState_)
				rete.reset();

			goalReplacementMap = assertStateFacts(rete, goalArgs, args);
			if (rete == null)
				return null;
			rete.run();

			firstState_ = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		prevState_ = args;

		boolean isTerminal = isTerminal(args);
		return new RRLObservations(rete, StateSpec.getInstance()
				.generateValidActions(rete), calculateReward(isTerminal, args),
				goalReplacementMap, isTerminal);
	}

	/**
	 * Calculates the reward.
	 * 
	 * @param isTerminal
	 *            If at the terminal state.
	 * @param args
	 *            Args required to calculate the reward.
	 * @return The reward received at this given interval.
	 */
	protected abstract double calculateReward(boolean isTerminal,
			Object... args);

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
	 * @return True if terminal, false otherwise.
	 */
	protected boolean isTerminal(Object... args) {
		if (StateSpec.getInstance().isGoal(StateSpec.getInstance().getRete()))
			return true;
		return false;
	}

	/**
	 * A method to call when a new episode begins.
	 */
	public void newEpisode() {
		firstState_ = true;
	}
}
