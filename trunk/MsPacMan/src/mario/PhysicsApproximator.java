package mario;

import java.awt.Point;

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
	private static final float MAX_JUMP_HEIGHT = 66.4f;
	private static final float MAX_JUMP_DIST = 80;
	private static final float MAX_JUMP_DIST_RUNNING = 150;
	private static PhysicsApproximator instance_;

	private PhysicsApproximator() {
	}

	public static PhysicsApproximator getInstance() {
		if (instance_ == null)
			instance_ = new PhysicsApproximator();
		return instance_;
	}
	
	// TODO Shift all methods to RLMarioStateSpec

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
	public boolean canJumpTo(float x, float y, MarioEnvironment environment,
			boolean jumpInto) {
		float cellSize = ch.idsia.benchmark.mario.engine.LevelScene.cellSize;
		float[] marioPos = environment.getMarioFloatPos();

		// Mario can jump into objects above him to a particular
		// height (assuming nothing is blocking)
		float extra = (environment.getMarioMode() > 0) ? cellSize * 2
				: cellSize;

		float[] diffs = { Math.abs(marioPos[0] - x), marioPos[1] - y };
		// If the x difference is on the same column
		if (diffs[0] <= cellSize) {
			if (jumpInto) {
				// If the y difference is greater than the jump height (+ extra)
				// return false. OR if the thing is under Mario
				if (diffs[1] > MAX_JUMP_HEIGHT + extra || diffs[1] < 0)
					return false;
				return true;
			}
			// Can always jump on the thing you're already on
			if (diffs[1] <= cellSize)
				return true;
			return false;
		}

		// If not in the same axis, 'reduce' the height of the thing being
		// jumped into if jumping into something
		if (jumpInto)
			diffs[1] -= extra;

		// Use a basic graph for distance-jumping mapping
		if (diffs[0] <= MAX_JUMP_DIST_RUNNING / 2 + cellSize / 2) {
			if (diffs[1] <= MAX_JUMP_HEIGHT)
				return true;
			return false;
		} else {
			// Use a basic linear gradient
			float gradient = -MAX_JUMP_HEIGHT
					/ (MAX_JUMP_DIST_RUNNING + cellSize / 2);
			if (diffs[0] <= MAX_JUMP_DIST_RUNNING + cellSize / 2) {
				float maxY = MAX_JUMP_HEIGHT + gradient * diffs[0];
				if (diffs[1] <= maxY)
					return true;
			}
		}

		return false;
	}

	/**
	 * Returns the actions necessary to take if Mario wants to jump onto a
	 * specified location.
	 * 
	 * @param startX
	 *            The x point from which the jump was made.
	 * @param startY
	 *            The y point from which the jump was made.
	 * @param currentX
	 *            Mario's current x point.
	 * @param currentY
	 *            Mario's current y point.
	 * @param endX
	 *            The x location to jump to.
	 * @param endY
	 *            The y location to jump to.
	 * @return A boolean array of action required to jump to the object
	 */
	public boolean[] jumpTo(float startX, float startY, float currentX,
			float currentY, float endX, float endY) {
		boolean[] actionArray = new boolean[Environment.numberOfKeys];
		int cellSize = ch.idsia.benchmark.mario.engine.LevelScene.cellSize;
		int direction, oppDirection, directedCellSize;
		if (endX > startX) {
			direction = Environment.MARIO_KEY_RIGHT;
			oppDirection = Environment.MARIO_KEY_LEFT;
			directedCellSize = cellSize;
		} else {
			direction = Environment.MARIO_KEY_LEFT;
			oppDirection = Environment.MARIO_KEY_RIGHT;
			directedCellSize = -cellSize;
			// Modify the parameters into negative for relations to make sense
			startX *= -1;
			currentX *= -1;
			endX *= -1;
		}

		float[] startEndDiffs = { Math.abs(endX - startX), startY - endY };
		float startCurrentDiffX = Math.abs(currentX - startX);
		float[] currentEndDiffs = { Math.abs(endX - currentX), currentY - endY };

		// If Mario is already on the end point, do nothing
		if (startEndDiffs[0] < cellSize / 2
				&& Math.abs(startEndDiffs[1]) < cellSize)
			return actionArray;
		
		
		
		// VERTICAL MOVEMENT

		// MINIMISING JUMP HEIGHT
		// Continue to jump only if the current x position is less than halfway
		// through the jump.
		
		// Modify the values for jumping if the y location is lower
		float lowerX = (endY > startY) ? endX - (endY - startY) : endX;
		float lowerY = (endY > startY) ? startY : endY;
		float releaseJumpPoint = (float) Point.distance(startX, startY, lowerX,
				lowerY) / 4;
		releaseJumpPoint = Math.max(releaseJumpPoint, 1);
		if (startCurrentDiffX <= releaseJumpPoint || currentEndDiffs[1] > 0)
			actionArray[Mario.KEY_JUMP] = true;
		else
			actionArray[Mario.KEY_JUMP] = false;

		// Running or not
		if (startEndDiffs[0] > MAX_JUMP_DIST)
			actionArray[Mario.KEY_SPEED] = true;

		
		
		// HORIZONTAL MOVEMENT
		float nearEndX = endX - directedCellSize / 2;
		if (actionArray[Mario.KEY_SPEED])
			nearEndX = endX - directedCellSize / 1.5f;
		float farEndX = endX + directedCellSize / 2;
		// Check for special case when Mario is below object and next to
		if (currentEndDiffs[1] > 0 && currentEndDiffs[0] <= cellSize) {
			// Move from the object so as not to get caught under it
			actionArray[direction] = false;
			actionArray[oppDirection] = true;
		} else {
			// Start of jump
			if (currentX < nearEndX) {
				actionArray[direction] = true;
				actionArray[oppDirection] = false;
			} else if (currentX < endX) {
				// Between nearEnd and end
				actionArray[direction] = false;
				actionArray[oppDirection] = false;
			} else if (currentX < farEndX) {
				// Between end and farEnd
				actionArray[direction] = false;
				actionArray[oppDirection] = true;
			} else {
				// Past the end point
				actionArray[Mario.KEY_SPEED] = true;
				actionArray[direction] = false;
				actionArray[oppDirection] = true;
			}
		}

		return actionArray;
	}

	/**
	 * Returns the actions necessary to take if Mario wants to jump over a
	 * specified location.
	 * 
	 * @param x
	 *            The x location to jump over.
	 * @param y
	 *            The y location to jump over.
	 * @return A boolean array of action required to jump over the object
	 */
	public boolean[] jumpOver(float x, float y) {
		return null;
	}
}
