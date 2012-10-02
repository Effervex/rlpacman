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
 *    src/relationalFramework/StateAction.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package relationalFramework;

import relationalFramework.PolicyActions;
import relationalFramework.StateAction;

import java.util.Collection;

import jess.Fact;

/**
 * A class for recording a state, and the action taken at that state.
 * 
 * @author Sam Sarjant
 */
public class StateAction {
	private Collection<Fact> stateFacts_;
	private PolicyActions action_;
	
	public StateAction(Collection<Fact> state, PolicyActions action) {
		stateFacts_ = state;
		action_ = action;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action_ == null) ? 0 : action_.hashCode());
		result = prime * result
				+ ((stateFacts_ == null) ? 0 : stateFacts_.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StateAction other = (StateAction) obj;
		if (action_ == null) {
			if (other.action_ != null)
				return false;
		} else if (!action_.equals(other.action_))
			return false;
		if (stateFacts_ == null) {
			if (other.stateFacts_ != null)
				return false;
		} else if (!stateFacts_.equals(other.stateFacts_))
			return false;
		return true;
	}
}
