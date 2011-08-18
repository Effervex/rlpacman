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
