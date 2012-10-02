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
 *    src/relationalFramework/State.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package relationalFramework;

import relationalFramework.State;

import java.util.Arrays;

/**
 * An interface for the state enum, which categorises state variables.
 * 
 * @author Sam Sarjant
 */
public abstract class State {
	/** The suffix to this class for use with dynamically loaded classes. */
	public static final String CLASS_SUFFIX = "State";
	
	/** The state array. */
	private Object[] stateArray_;
	
	/**
	 * Constructor for a new state.
	 * 
	 * @param stateArray The state array.
	 */
	public State(Object[] stateArray) {
		stateArray_ = stateArray;
	}
	
	/**
	 * Simple toString method.
	 */
	@Override
	public String toString() {
		return "State";
	}

	public Object[] getStateArray() {
		return stateArray_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(stateArray_);
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
		final State other = (State) obj;
		if (!Arrays.equals(stateArray_, other.stateArray_))
			return false;
		return true;
	}
}
