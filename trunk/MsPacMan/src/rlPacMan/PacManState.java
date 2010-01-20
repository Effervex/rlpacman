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
	public static final int PACMAN = 0;
	public static final int DOT_COLLECTION = 1;
	public static final int POWERDOT_COLLECTION = 2;
	public static final int GHOST_ARRAY = 3;
	public static final int FRUIT = 4;
	public static final int DISTANCE_GRID = 5;

	/**
	 * A constructor for a pacman state.
	 * 
	 * @param stateArray
	 *            The state array.
	 */
	public PacManState(Object[] stateArray) {
		super(stateArray);
	}

	public Player getPlayer() {
		return (Player) getStateArray()[PACMAN];
	}

	public Collection<Dot> getDots() {
		return (Collection<Dot>) getStateArray()[DOT_COLLECTION];
	}

	public Collection<PowerDot> getPowerDots() {
		return (Collection<PowerDot>) getStateArray()[POWERDOT_COLLECTION];
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
