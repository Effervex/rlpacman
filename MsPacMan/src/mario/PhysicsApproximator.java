package mario;

import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;

/**
 * This class approximates Mario's movements and environment so that all actions
 * and observations are approximately correct.
 * 
 * @author Sam Sarjant
 */
public class PhysicsApproximator {
	public static final int MAX_JUMP_HEIGHT = 5;

	/**
	 * Checks if Mario can jump to a given point from his current location,
	 * given the observations around him.
	 * 
	 * @param x
	 *            The x location to jump to.
	 * @param y
	 *            The y location to jump to.
	 * @param environment
	 *            The state of the environment.
	 * @param jumpInto
	 *            If Mario only has to jump into the space, not onto.
	 * @return True if Mario can jump to the point, false otherwise.
	 */
	public static boolean canJumpTo(int x, int y, MarioEnvironment environment,
			boolean jumpInto) {
		int[] originSquare = environment.getMarioReceptiveFieldCenter();
		// Mario cannot reach jump onto objects above his jump height
		if (originSquare[1] - y >= MAX_JUMP_HEIGHT)
			return false;

		// Handling Mario being on the ground
		if (environment.isMarioOnGround()) {
			if (jumpInto) {
				// Mario can jump into objects above him to a particular height
				// (assuming nothing is blocking)
				int extra = (environment.getMarioMode() > 0) ? 1 : 0;
				if (originSquare[1] - y > MAX_JUMP_HEIGHT + extra)
					return false;
			} else {
				// Mario cannot jump onto objects directly above/below him
				if (x == originSquare[0] && y != originSquare[1])
					return false;
			}
		} else {
		}
		return true;
	}

	/**
	 * Returns the actions necessary to take if Mario wants to jump to a
	 * specified location.
	 * 
	 * @param x
	 *            The x location of the place to jump to.
	 * @param y
	 *            The y location of the place to jump to.
	 * @param environment
	 *            The environment state/
	 * @return A boolean array of keystroke actions to take.
	 */
	public static boolean[] jumpTo(int x, int y, MarioEnvironment environment) {
		boolean[] actionArray = new boolean[Environment.numberOfButtons];
		int[] originPoint = environment.getMarioReceptiveFieldCenter();

		actionArray[Mario.KEY_SPEED] = true;

		// If the object is to Mario's right
		if (x > originPoint[0]) {
			// If the object is above Mario and he is not adjacent to it, move
			// towards it
			if (y < originPoint[1]) {
				if (x > originPoint[0] + 1)
					actionArray[Environment.MARIO_KEY_RIGHT] = true;
				else {
					actionArray[Environment.MARIO_KEY_LEFT] = true;
					actionArray[Mario.KEY_SPEED] = false;
				}
			} else
				actionArray[Environment.MARIO_KEY_RIGHT] = true;
		} else if (x < originPoint[0]) {
			// If the object is above Mario and he is not adjacent to it, move
			// towards it
			if (y < originPoint[1]) {
				if (x < originPoint[0] - 1)
					actionArray[Environment.MARIO_KEY_LEFT] = true;
				else {
					actionArray[Environment.MARIO_KEY_RIGHT] = true;
					actionArray[Mario.KEY_SPEED] = false;
				}
			} else
				actionArray[Environment.MARIO_KEY_LEFT] = true;
		}

		// actionArray[Mario.KEY_RIGHT] = true;
		// actionArray[Mario.KEY_SPEED] = true;
		if (environment.isMarioAbleToJump() || !environment.isMarioOnGround())
			actionArray[Mario.KEY_JUMP] = true;
		return actionArray;
	}
}
