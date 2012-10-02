/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/rrlFramework/RRLActions.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
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
