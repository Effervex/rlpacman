package rrlFramework;

import java.util.SortedSet;

import org.apache.commons.collections.BidiMap;

import jess.Rete;
import util.MultiMap;

/**
 * The set of observations an agent receives at every step. The state, valid
 * actions and reward.
 * 
 * @author Sam Sarjant
 */
public class RRLObservations {
	/** The goal replacement map (a -> ?G_0) form. */
	private BidiMap goalReplacements_;

	/** The reward received (if any). */
	private double reward_;

	/** The current relational state. */
	private Rete state_;

	/** Flag if this state is a terminal state. */
	private boolean terminal_;

	/** The valid actions the agent can take. */
	private MultiMap<String, String[]> validActions_;

	/**
	 * A constructor with a goal observations and a reward.
	 * 
	 * @param state
	 *            The state of the system.
	 * @param validActions
	 *            The valid actions within the system.
	 * @param reward
	 *            The reward received from the previous action.
	 * @param goalReplacements
	 *            The goal replacements (if any).
	 * @param terminal
	 *            If this state is terminal.
	 */
	public RRLObservations(Rete state, MultiMap<String, String[]> validActions,
			double reward, BidiMap goalReplacements, boolean terminal) {
		state_ = state;
		validActions_ = validActions;
		goalReplacements_ = goalReplacements;
		reward_ = reward;
		terminal_ = terminal;
	}

	public BidiMap getGoalReplacements() {
		return goalReplacements_;
	}

	public double getReward() {
		return reward_;
	}

	public Rete getState() {
		return state_;
	}

	/**
	 * Gets the actions for a given action predicate.
	 * 
	 * @param actionPred
	 *            The action to get the args for.
	 * @return The arguments for a given action predicate.
	 */
	public SortedSet<String[]> getValidActions(String actionPred) {
		try {
			return validActions_.getSortedSet(actionPred);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public MultiMap<String, String[]> getValidActions() {
		return validActions_;
	}

	public boolean isTerminal() {
		return terminal_;
	}
}
