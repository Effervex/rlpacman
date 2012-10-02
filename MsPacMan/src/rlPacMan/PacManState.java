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
 *    src/rlPacMan/PacManState.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package rlPacMan;

import relationalFramework.State;
import msPacMan.Fruit;
import msPacMan.Ghost;

/**
 * The state values present in Object Observations and associated methods for
 * extracting them from the observation array.
 * 
 * @author Sam Sarjant
 */
public class PacManState extends State {
	public static final int GHOST_ARRAY = 0;
	public static final int FRUIT = 1;
	public static final int DISTANCE_GRID = 2;

	/**
	 * A constructor for a pacman state.
	 * 
	 * @param stateArray
	 *            The state array.
	 */
	public PacManState(Object[] stateArray) {
		super(stateArray);
	}

	public Ghost[] getGhosts() {
		return (Ghost[]) getStateArray()[GHOST_ARRAY];
	}

	public Fruit getFruit() {
		return (Fruit) getStateArray()[FRUIT];
	}

	public DistanceDir[][] getDistanceGrid() {
		return (DistanceDir[][]) getStateArray()[DISTANCE_GRID];
	}
}
