package mario;

import relationalFramework.State;

/**
 * The state values present in Object Observations and associated methods for
 * extracting them from the observation array.
 * 
 * @author Sam Sarjant
 */
public class MarioState extends State {
	public static final int GHOST_ARRAY = 0;
	public static final int FRUIT = 1;
	public static final int DISTANCE_GRID = 2;
	public static final int SAFEST_JUNCTION = 3;
	public static final int PLAYER = 4;

	/**
	 * A constructor for a pacman state.
	 * 
	 * @param stateArray
	 *            The state array.
	 */
	public MarioState(Object[] stateArray) {
		super(stateArray);
	}
}
