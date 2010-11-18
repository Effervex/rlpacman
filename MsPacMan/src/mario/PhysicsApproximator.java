package mario;

import mario.robinbaumgarten.astar.LevelScene;
import mario.robinbaumgarten.astar.level.Level;

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
	private static final int TIMEOUT = 30;
	private static final int MAX_JUMP_HEIGHT = 0;
	private static PhysicsApproximator instance_;

	private PhysicsApproximator() {
		initialise();
	}

	public static PhysicsApproximator getInstance() {
		if (instance_ == null)
			instance_ = new PhysicsApproximator();
		return instance_;
	}

	private LevelScene levelScene_;
	private float[] lastMarioPos_;

	public void initialise() {
		levelScene_ = new LevelScene();
		levelScene_.init();
		levelScene_.level = new Level(1500, 15);
		lastMarioPos_ = new float[2];
	}

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
			boolean jumpInto) throws Exception {
		float cellSize = ch.idsia.benchmark.mario.engine.LevelScene.cellSize;
		// If we have to jump on the thing, move the jump point up one tile
		if (!jumpInto)
			y -= cellSize;
		
		// Cut out possibilities if Mario is on the ground
		if (levelScene_.mario.isOnGround()) {
			if (jumpInto) {
				// Mario can jump into objects above him to a particular
				// height (assuming nothing is blocking)
				float extra = (environment.getMarioMode() > 0) ? cellSize * 1.5f
						: cellSize * 0.5f;
				if (environment.getMarioFloatPos()[1] - y > MAX_JUMP_HEIGHT + extra)
					return false;
			}
		}

		// Use the simulation to check if Mario can jump to that point by taking
		// the regular actions.
		LevelScene workingScene = (LevelScene) levelScene_.clone();
		int count = 0;
		do {
			workingScene.mario.setKeys(jumpTo(x, y, workingScene));
			workingScene.tick();
			// If Mario's coordinates are 'reasonably' close to the point in
			// question, return true.
			// Reasonably = half a cell's distance
			if ((Math.abs(workingScene.mario.x - x) <= cellSize)
					&& (Math.abs(workingScene.mario.y - y) <= cellSize))
				return true;
			count++;
		} while (!workingScene.mario.isOnGround() && count < TIMEOUT);

		return false;
	}

	/**
	 * Returns the actions necessary to take if Mario wants to jump to a
	 * specified location.
	 * 
	 * @param x
	 *            The x location of the place to jump to.
	 * @param y
	 *            The y location of the place to jump to.
	 * @return A boolean array of keystroke actions to take.
	 */
	public boolean[] jumpTo(float x, float y) {
		boolean[] actionArray = jumpTo(x, y, levelScene_);

		return actionArray;
	}

	/**
	 * The meat of the jump to method which actually does the calculations for
	 * jumping to a given point.
	 * 
	 * @param x
	 *            The x point to jump to.
	 * @param y
	 *            The y point to jump to.
	 * @param levelScene
	 *            The simulated level scene.
	 * @return A boolean array of actions required to jump to the point.
	 */
	private boolean[] jumpTo(float x, float y, LevelScene levelScene) {
		boolean[] actionArray = new boolean[Environment.numberOfKeys];
		actionArray[Mario.KEY_SPEED] = true;
		float marioX = levelScene.mario.x;
		float marioY = levelScene.mario.y;
		int cellSize = ch.idsia.benchmark.mario.engine.LevelScene.cellSize;

		// If the object is to Mario's right
		if (x > marioX) {
			// If the object is above Mario and he is not adjacent to it, move
			// towards it
			if (y < marioY) {
				if (x > marioX + cellSize)
					actionArray[Environment.MARIO_KEY_RIGHT] = true;
				else {
					// Decelerate
					actionArray[Environment.MARIO_KEY_LEFT] = true;
					actionArray[Mario.KEY_SPEED] = false;
				}
			} else
				actionArray[Environment.MARIO_KEY_RIGHT] = true;
		} else if (x < marioX) {
			// If the object is above Mario and he is not adjacent to it, move
			// towards it
			if (y < marioY) {
				if (x < marioX - cellSize)
					actionArray[Environment.MARIO_KEY_LEFT] = true;
				else {
					// Decelerate
					actionArray[Environment.MARIO_KEY_RIGHT] = true;
					actionArray[Mario.KEY_SPEED] = false;
				}
			} else
				actionArray[Environment.MARIO_KEY_LEFT] = true;
		} else {
			// Mario is at the same x as the object
			// Adjust velocity so he remains above the object
			if (levelScene.mario.xa > 0)
				actionArray[Mario.KEY_LEFT] = true;
			else if (levelScene.mario.xa < 0)
				actionArray[Mario.KEY_RIGHT] = true;
		}

		if (levelScene.mario.mayJump() || !levelScene.mario.isOnGround())
			actionArray[Mario.KEY_JUMP] = true;

		return actionArray;
	}

	/**
	 * Applies the chosen action to the simulated level scene.
	 * 
	 * @param actionArray
	 *            The actions to apply.
	 */
	public void applyAction(boolean[] actionArray) {
		levelScene_.mario.setKeys(actionArray);
		levelScene_.tick();
	}

	/**
	 * Synchronises the actula observations with the simulated observations.
	 * 
	 * @param levelObs
	 *            The level observations.
	 * @param enemyPos
	 *            The enemy positions in float.
	 * @param marioPos
	 *            Mario's actual position in float.
	 */
	public void synchroniseSimulation(byte[][] levelObs, float[] enemyPos,
			float[] marioPos) {
		// If Mario's sim coords are off
		if (levelScene_.mario.x != marioPos[0]
				|| levelScene_.mario.y != marioPos[1]) {
			levelScene_.mario.x = marioPos[0];
			levelScene_.mario.xa = (marioPos[0] - lastMarioPos_[0]) * 0.89f;
			if (Math.abs(levelScene_.mario.y - marioPos[1]) > 0.1f)
				levelScene_.mario.ya = (marioPos[1] - lastMarioPos_[1]) * 0.85f;

			levelScene_.mario.y = marioPos[1];
		}

		levelScene_.setLevelScene(levelObs);
		levelScene_.setEnemies(enemyPos);

		lastMarioPos_[0] = marioPos[0];
		lastMarioPos_[1] = marioPos[1];
	}
}
