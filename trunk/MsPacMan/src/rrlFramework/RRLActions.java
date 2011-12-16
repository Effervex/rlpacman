package rrlFramework;

import relationalFramework.PolicyActions;

/**
 * The action(s) a relational agent returns when given observations. Might also
 * include other flags, such as end episode.
 * 
 * @author Sam Sarjant
 */
public class RRLActions {
	/** The action(s) returned by the agent. */
	private PolicyActions actions_;

	/** If the agent signals it wants to end this episode early. */
	private boolean earlyExit_;

	/**
	 * The actions returned by an agent.
	 * 
	 * @param actions
	 *            The actions being returned.
	 */
	public RRLActions(PolicyActions actions) {
		actions_ = actions;
	}

	/**
	 * The agent signals it does not want to perform any more actions and end
	 * the episode early.
	 * 
	 * @param endEpisode
	 *            Agent wants to end the episode (if true).
	 */
	public RRLActions(boolean endEpisode) {
		earlyExit_ = endEpisode;
	}

	public boolean isEarlyExit() {
		return earlyExit_;
	}

	public PolicyActions getActions() {
		return actions_;
	}
}
