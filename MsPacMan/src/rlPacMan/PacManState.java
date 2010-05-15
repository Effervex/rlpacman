package rlPacMan;

import java.util.Collection;

import relationalFramework.State;

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

	public int[][] getDistanceGrid() {
		return (int[][]) getStateArray()[DISTANCE_GRID];
	}
}