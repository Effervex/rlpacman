package rrlFramework;

import java.util.HashMap;
import java.util.Map;
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
	/** If an observation or turn applies to all players. */
	public static final String ALL_PLAYERS = "ALL";

	/** The index of the internal reward. */
	public static final int INTERNAL_INDEX = 1;

	/** The index of the environmental reward. */
	public static final int ENVIRONMENTAL_INDEX = 0;

	/** All observations regarding player-mapped information. */
	private Map<String, double[]> agentRewards_;

	/** The current relational state. */
	private Rete state_;

	/** Flag if this state is a terminal state. */
	private boolean terminal_;

	/** The valid actions the agent can take. */
	private MultiMap<String, String[]> validActions_;

	/** Defines which agent's turn it is. */
	private String agentTurn_;

	/** The goal replacement map (a -> ?G_0) form. */
	private BidiMap goalReplacements_;

	/**
	 * An RRLObservations constructor for a single agent.
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
			double[] reward, BidiMap goalReplacements, boolean terminal) {
		this(state, validActions, terminal, goalReplacements, ALL_PLAYERS);
		agentRewards_.put(ALL_PLAYERS, reward);
		agentTurn_ = ALL_PLAYERS;
	}

	/**
	 * A smaller constructor, which only defines some of the observations. The
	 * rest are added in following calls with accompanied player IDs.
	 * 
	 * @param state
	 *            The state of the system.
	 * @param validActions
	 *            The valid actions within the system.
	 * @param terminal
	 *            If this state is terminal.
	 * @param goalReplacements
	 *            The replacements for the goal terms.
	 */
	public RRLObservations(Rete state, MultiMap<String, String[]> validActions,
			boolean terminal, BidiMap goalReplacements, String agentTurn) {
		state_ = state;
		goalReplacements_ = goalReplacements;
		validActions_ = validActions;
		terminal_ = terminal;
		agentTurn_ = agentTurn;
		agentRewards_ = new HashMap<String, double[]>();
	}

	/**
	 * Adds player-specific observations.
	 * 
	 * @param player
	 *            The observation player.
	 * @param reward
	 *            The reward for the player.
	 */
	public void addPlayerObservations(String player, double[] reward) {
		agentRewards_.put(player, reward);
	}

	public BidiMap getGoalReplacements() {
		return goalReplacements_;
	}

	public double getInternalReward(String player) {
		return agentRewards_.get(player)[INTERNAL_INDEX];
	}

	public double getEnvironmentReward(String player) {
		return agentRewards_.get(player)[ENVIRONMENTAL_INDEX];
	}

	public Rete getState() {
		return state_;
	}

	public String getAgentTurn() {
		return agentTurn_;
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

	public double[] getRewards(String player) {
		return agentRewards_.get(player);
	}
}
