package rlPacMan;

import java.util.Collection;

import relationalFramework.State;

/**
 * The state values present in Object Observations and associated methods for
 * extracting them from the observation array.
 * 
 * @author Sam Sarjant
 */
public enum PacManState implements State {
	PACMAN, DOT_COLLECTION, POWERDOT_COLLECTION, GHOST_ARRAY, FRUIT, DISTANCE_GRID;

	public static Player getPlayer(Object[] observations) {
		return (Player) observations[PACMAN.ordinal()];
	}

	public static Collection<Dot> getDots(Object[] observations) {
		return (Collection<Dot>) observations[DOT_COLLECTION.ordinal()];
	}

	public static Collection<PowerDot> getPowerDots(Object[] observations) {
		return (Collection<PowerDot>) observations[POWERDOT_COLLECTION
				.ordinal()];
	}

	public static Ghost[] getGhosts(Object[] observations) {
		return (Ghost[]) observations[GHOST_ARRAY.ordinal()];
	}

	public static Fruit getFruit(Object[] observations) {
		return (Fruit) observations[FRUIT.ordinal()];
	}

	public static int[][] getDistanceGrid(Object[] observations) {
		return (int[][]) observations[DISTANCE_GRID.ordinal()];
	}
}
