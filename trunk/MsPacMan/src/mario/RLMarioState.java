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
 *    src/mario/RLMarioState.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package mario;

import relationalFramework.State;

/**
 * The state values present in Object Observations and associated methods for
 * extracting them from the observation array.
 * 
 * @author Sam Sarjant
 */
public class RLMarioState extends State {
	public static final int IS_MARIO_IN_AIR = 0;
	public static final int MARIO_DIRECTION = 1;
	public static final int MARIO_GROUND_POS = 2;
	public static final int PREV_BOOL_ACTION = 3;
	public static final int STATIC_OBJECT_FACTS = 4;

	/**
	 * A constructor for a pacman state.
	 * 
	 * @param stateArray
	 *            The state array.
	 */
	public RLMarioState(Object[] stateArray) {
		super(stateArray);
	}
}
