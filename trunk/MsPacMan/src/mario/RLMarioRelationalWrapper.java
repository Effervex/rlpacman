package mario;

import relationalFramework.FiredAction;
import relationalFramework.PolicyActions;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalWrapper;
import relationalFramework.StateSpec;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import cerrla.PolicyGenerator;
import ch.idsia.benchmark.mario.engine.LevelScene;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;
import jess.JessException;
import jess.Rete;

public class RLMarioRelationalWrapper extends RelationalWrapper {
	public static final int CELL_SIZE = ch.idsia.benchmark.mario.engine.LevelScene.cellSize;
	public static final float MAX_JUMP_DIST = 79;
	public static final float MAX_JUMP_DIST_RUNNING = 130;
	public static final float MAX_JUMP_HEIGHT = 66.4f;

	private byte[][] basicLevelObs_;
	private float[] currentEndDiffs_ = new float[2];
	private int directedCellSize_;
	private int direction_;
	private boolean isMarioInAir_;
	private int marioCentreX_;
	private int marioCentreY_;
	/** Mario's facing direction. */
	private int marioDirection_ = Environment.MARIO_KEY_RIGHT;
	private float[] marioGroundPos_ = { 32, 32 };
	private int negModifier_;
	private int oppDirection_;
	/** Records Mario's previous boolean array action. */
	private boolean[] prevBoolAction_ = new boolean[Environment.numberOfKeys];
	/** Needed to check if the shells are passive. */
	private Collection<String> shellPositions_ = new HashSet<String>();
	private float[] startEndDiffs_ = new float[2];
	private Collection<String> staticObjectFacts_ = new HashSet<String>();

	/**
	 * Asserts any enemy objects present at a given point within the observation
	 * field and any relevant relations that object has. Also returns any shells
	 * present.
	 * 
	 * @param rete
	 *            The Rete object to assert to.
	 * @param environment
	 * @param kind
	 *            The enemy type.
	 * @param relX
	 *            The x coord to examine.
	 * @param relY
	 *            The y coord to examine.
	 * @param levelObs
	 *            The level observations.
	 * @return Returns the name of a shell, if the object was a shell.
	 * @throws Exception
	 *             Should something go awry.
	 */
	private String assertEnemyObjects(Rete rete, MarioEnvironment environment,
			float kind, float relX, float relY, byte[][] levelObs)
			throws Exception {
		int modX = (int) (environment.getMarioFloatPos()[0] + relX);
		int modY = (int) (environment.getMarioFloatPos()[1] + relY);
		int gridX = (int) (relX / LevelScene.cellSize) + marioCentreX_;
		int gridY = (int) (relY / LevelScene.cellSize) + marioCentreY_;

		switch ((int) kind) {
		// Enemies
		case (ObservationConstants.ENMY_BULLET_BILL):
			assertFloatThing(rete, environment, "bulletBill", "bb", modX, modY,
					gridX, gridY, false, levelObs, 1, false);
			break;
		case (ObservationConstants.ENMY_ENEMY_FLOWER):
			assertFloatThing(rete, environment, "pirahnaPlant", "pp", modX,
					modY, gridX, gridY, false, levelObs, 1, false);
			break;
		case (ObservationConstants.ENMY_GOOMBA_WINGED):
			assertFloatThing(rete, environment, "flying", "gmba", modX, modY,
					gridX, gridY, false, levelObs, 1, false);
		case (ObservationConstants.ENMY_GOOMBA):
			assertFloatThing(rete, environment, "goomba", "gmba", modX, modY,
					gridX, gridY, false, levelObs, 1, false);
			break;
		case (ObservationConstants.ENMY_GREEN_KOOPA_WINGED):
			assertFloatThing(rete, environment, "flying", "grKpa", modX, modY,
					gridX, gridY, false, levelObs, 1, false);
		case (ObservationConstants.ENMY_GREEN_KOOPA):
			assertFloatThing(rete, environment, "greenKoopa", "grKpa", modX,
					modY, gridX, gridY, false, levelObs, 1, false);
			break;
		case (ObservationConstants.ENMY_RED_KOOPA_WINGED):
			assertFloatThing(rete, environment, "flying", "redKpa", modX, modY,
					gridX, gridY, false, levelObs, 1, false);
		case (ObservationConstants.ENMY_RED_KOOPA):
			assertFloatThing(rete, environment, "redKoopa", "redKpa", modX,
					modY, gridX, gridY, false, levelObs, 1, false);
			break;
		case (ObservationConstants.ENMY_SPIKY_WINGED):
			assertFloatThing(rete, environment, "flying", "spiky", modX, modY,
					gridX, gridY, false, levelObs, 1, false);
		case (ObservationConstants.ENMY_SPIKY):
			assertFloatThing(rete, environment, "spiky", "spiky", modX, modY,
					gridX, gridY, false, levelObs, 1, false);
			break;
		case (ObservationConstants.ENMY_GENERAL_ENEMY):
			assertFloatThing(rete, environment, "enemy", "enemy", modX, modY,
					gridX, gridY, false, levelObs, 1, false);
			break;
		case (ObservationConstants.ENMY_SHELL):
			return assertFloatThing(rete, environment, "shell", "shell", modX,
					modY, gridX, gridY, true, levelObs, 1, false);
		}
		return null;
	}

	/**
	 * Assert a thing using float coordinates for maximum precision.
	 * 
	 * @param rete
	 *            The Rete object to assert to.
	 * @param environment
	 *            The MarioEnvironment.
	 * @param condition
	 *            The condition being asserted.
	 * @param thingPrefix
	 *            The thing's prefix (to be appended by xy coords)
	 * @param x
	 *            The actual x location of the thing.
	 * @param y
	 *            The actual y location of the thing.
	 * @param gridX
	 *            The grid X position of the thing.
	 * @param gridY
	 *            The grid Y position of the thing.
	 * @param jumpInto
	 *            If the object can be jumped into (from the side) rather than
	 *            onto (from above)
	 * @param levelObs
	 *            The level observations.
	 * @param width
	 *            The width of the thing being asserted.
	 * @param isStatic
	 *            If the object is static (not moving).
	 */
	private String assertFloatThing(Rete rete, MarioEnvironment environment,
			String condition, String thingPrefix, int x, int y, int gridX,
			int gridY, boolean jumpInto, byte[][] levelObs, int width,
			boolean isStatic) throws JessException, Exception {
		String thing = thingPrefix + "_" + x + "_" + y;
		String fact = "(" + condition + " " + thing + ")";
		rete.assertString(fact);
		if (isStatic && !isMarioInAir_)
			staticObjectFacts_.add(fact);

		// Distance & direction assertions.
		float[] marioPos = environment.getMarioFloatPos();
		int dist = (int) (x - marioPos[0]);

		// Special checks for shells
		if (thingPrefix.equals("shell")) {
			// Check for carrying
			if (environment.isMarioCarrying() && dist < LevelScene.cellSize) {
				rete.assertString("(carrying " + thing + ")");
				return thing;
			}
			// Check for passive shells
			if (shellPositions_.contains(thing))
				rete.assertString("(passive " + thing + ")");
			else
				jumpInto = false;
		}

		fact = "(distance " + thing + " " + dist + ")";
		rete.assertString(fact);
		if (isStatic && !isMarioInAir_)
			staticObjectFacts_.add(fact);

		// Height diff
		int heightDiff = (int) Math.round(marioPos[1] - y);
		fact = "(heightDiff " + thing + " " + heightDiff + ")";
		rete.assertString(fact);
		if (isStatic && !isMarioInAir_)
			staticObjectFacts_.add(fact);

		// Can jump on/over assertions
		Collection<String> facts = assertJumpOnOver(rete, environment, thing,
				x, y, gridX, gridY, levelObs, jumpInto);
		if (isStatic && !isMarioInAir_)
			staticObjectFacts_.addAll(facts);

		// Width assertion
		fact = "(width " + thing + " " + width + ")";
		rete.assertString(fact);
		if (isStatic && !isMarioInAir_)
			staticObjectFacts_.add(fact);
		return thing;
	}

	/**
	 * Asserts if Mario can jump onto/over the object in question. Note that for
	 * some objects (like items) Mario need only jump into, not onto.
	 * 
	 * @param rete
	 *            The Rete object to assert to.
	 * @param environment
	 *            The MarioEnvironment.
	 * @param thing
	 *            The thing being possibly asserted.
	 * @param x
	 *            The x coord of the thing.
	 * @param y
	 *            The y coord of the thing.
	 * @param gridX
	 *            The grid X position of the thing.
	 * @param gridY
	 *            The grid Y position of the thing.
	 * @param levelObs
	 *            The level observations.
	 * @param jumpInto
	 *            If the thing can be jumped into.
	 */
	private Collection<String> assertJumpOnOver(Rete rete,
			MarioEnvironment environment, String thing, float x, float y,
			int gridX, int gridY, byte[][] levelObs, boolean jumpInto)
			throws Exception {
		// Check Mario isn't blocked by geometry
		boolean[] jumpOnOver = canJumpOnOver(x, y, environment, jumpInto,
				isMarioInAir_);
		Collection<String> facts = new ArrayList<String>(2);
		if ((jumpOnOver[0] || jumpOnOver[1])
				&& !jumpBlocked(environment, gridX, gridY, levelObs, jumpInto)) {
			if (jumpOnOver[0]) {
				String fact = "(canJumpOn " + thing + ")";
				rete.assertString(fact);
				facts.add(fact);
			}
			if (jumpOnOver[1]) {
				String fact = "(canJumpOver " + thing + ")";
				rete.assertString(fact);
				facts.add(fact);
			}
		}
		return facts;
	}

	/**
	 * Asserts any level objects present at a given point within the observation
	 * field and any relevant relations that object has.
	 * 
	 * @param rete
	 *            The Rete object to assert to.
	 * @param environment
	 *            The MarioEnvironment.
	 * @param levelObs
	 *            The level observation field; only detailing the relevant
	 *            objects at a distance from Mario.
	 * @param enemyObs
	 *            The enemy observations. This is the only way to get the fire
	 *            flower and mushroom item assertions.
	 * @param x
	 *            The x coord to examine.
	 * @param y
	 *            The y coord to examine.
	 * @throws Exception
	 *             Should something go awry.
	 */
	private void assertLevelObjects(Rete rete, MarioEnvironment environment,
			byte[][] levelObs, byte[][] enemyObs, byte x, byte y)
			throws Exception {
		byte levelVal = levelObs[y][x];
		switch (levelVal) {
		// Brick
		case (ObservationConstants.LVL_BRICK):
		case (ObservationConstants.LVL_BREAKABLE_BRICK):
			// If searchable, assert brick. Otherwise it's just terrain
			if (canMarioFit(environment, x, y, levelObs, true)) {
				assertThing(rete, environment, "brick", "brk", x, y, false,
						levelObs, 1);

				if (PolicyGenerator.debugMode_)
					System.out.print("b ");
				break;
			}
		case (ObservationConstants.LVL_UNBREAKABLE_BRICK):
			// If searchable, assert brick. Otherwise it's just terrain
			if (canMarioFit(environment, x, y, levelObs, true)) {
				assertThing(rete, environment, "box", "box", x, y, false,
						levelObs, 1);

				if (PolicyGenerator.debugMode_)
					System.out.print("B ");
				break;
			}
			// Terrain
		case (ObservationConstants.LVL_BORDER_HILL):
		case (ObservationConstants.LVL_CANNON_MUZZLE):
		case (ObservationConstants.LVL_CANNON_TRUNK):
		case (ObservationConstants.LVL_FLOWER_POT):
		case (ObservationConstants.LVL_FLOWER_POT_OR_CANNON):
		case (ObservationConstants.LVL_BORDER_CANNOT_PASS_THROUGH):
			checkForPit(rete, environment, levelObs, x, y);
			if (PolicyGenerator.debugMode_)
				System.out.print("# ");
			break;
		// Coin
		case (ObservationConstants.LVL_COIN):
			assertThing(rete, environment, "coin", "coin", x, y, true,
					levelObs, 1);
			if (PolicyGenerator.debugMode_)
				System.out.print("c ");
			break;
		default:
			if (PolicyGenerator.debugMode_) {
				if (x == 9 && y == 9)
					System.out.print("M ");
				else
					System.out.print(". ");
			}
		}

		// Check for mushroom or fireflower
		byte enemyVal = enemyObs[y][x];
		switch (enemyVal) {
		case (ObservationConstants.ENMY_FIRE_FLOWER):
			assertThing(rete, environment, "fireFlower", "ff", x, y, true,
					levelObs, 1);
			break;
		case (ObservationConstants.ENMY_MUSHROOM):
			assertThing(rete, environment, "mushroom", "mush", x, y, true,
					levelObs, 1);
			break;
		}
	}

	/**
	 * Asserts a thing into rete and makes distance assertions upon it.
	 * 
	 * @param rete
	 *            The Rete object to assert to.
	 * @param environment
	 *            The MarioEnvironment.
	 * @param condition
	 *            The condition being asserted.
	 * @param thingPrefix
	 *            The thing's prefix (to be appended by xy coords)
	 * @param relX
	 *            The relative x location of the thing.
	 * @param relY
	 *            The relative y location of the thing.
	 * @param jumpInto
	 *            If the object can be jumped into (from the side) rather than
	 *            onto (from above)
	 * @param levelObs
	 *            The level observations.
	 * @param width
	 *            The width of the thing.
	 */
	private void assertThing(Rete rete, MarioEnvironment environment,
			String condition, String thingPrefix, int relX, int relY,
			boolean jumpInto, byte[][] levelObs, int width) throws Exception {
		if (isMarioInAir_ && !condition.equals("mushroom"))
			return;

		// Can use Mario's current location to fix level object names
		Point2D.Float p = RLMarioStateSpec.relativeToGlobal(relX, relY,
				environment);
		assertFloatThing(rete, environment, condition, thingPrefix, (int) p.x,
				(int) p.y, relX, relY, jumpInto, levelObs, width,
				!condition.equals("mushroom"));
	}

	/**
	 * Calculates the direction variables for Mario.
	 * 
	 * @param startX
	 *            Mario's starting x jump point.
	 * @param startY
	 *            Mario's starting y jump point.
	 * @param marioX
	 *            Mario's current x point.
	 * @param marioY
	 *            Mario's current y point.
	 * @param x
	 *            Mario's goal x point.
	 * @param y
	 *            Mario's goal y point.
	 */
	private void calculateDirectionVariables(float startX, float startY,
			float marioX, float marioY, float x, float y) {
		startEndDiffs_[0] = Math.abs(x - startX);
		startEndDiffs_[1] = startY - y;
		currentEndDiffs_[0] = Math.abs(x - marioX);
		currentEndDiffs_[1] = marioY - y;
		if (x > startX) {
			direction_ = Environment.MARIO_KEY_RIGHT;
			oppDirection_ = Environment.MARIO_KEY_LEFT;
			directedCellSize_ = CELL_SIZE;
			negModifier_ = 1;
		} else {
			direction_ = Environment.MARIO_KEY_LEFT;
			oppDirection_ = Environment.MARIO_KEY_RIGHT;
			directedCellSize_ = -CELL_SIZE;
			negModifier_ = -1;
		}
	}

	/**
	 * A basic method which checks if a thing can be jumped onto/over
	 * 
	 * @param diffs
	 *            The x,y diffs between the point and Mario. A positive y
	 *            indicates the object is higher than Mario.
	 * @param extraDist
	 *            If there is extra distance to cover (for jumping over
	 *            something).
	 * @return True if Mario can jump onto/over the thing.
	 */
	private boolean canJump(float[] diffs, float extraDist) {
		// Vertical limit
		float vertLimit = (extraDist != 0) ? CELL_SIZE * 1.5f : 0;
		if (diffs[1] <= MAX_JUMP_HEIGHT - vertLimit) {
			// Horizontal limit
			float canJumpDist = diffs[0] + diffs[1] + extraDist;
			if (canJumpDist < MAX_JUMP_DIST_RUNNING)
				return true;
		}
		return false;
	}

	/**
	 * Checks if mario can fit under/over a thing.
	 * 
	 * @param environment
	 *            The MarioEnvironment.
	 * @param x
	 *            Thing x coords.
	 * @param y
	 *            Thing y coords.
	 * @param levelObs
	 *            The level observations.
	 * @param under
	 *            If checking UNDER the object.
	 * @return True if Mario can fit.
	 */
	private boolean canMarioFit(MarioEnvironment environment, int x, int y,
			byte[][] levelObs, boolean under) {
		int space = 1;
		if (environment.getMarioMode() != 0)
			space = 2;
		for (int yMod = 1; yMod <= space; yMod++) {
			int changedY = (under) ? yMod : yMod * -1;
			if (y + changedY < 0)
				return true;
			if (y + changedY >= levelObs.length
					|| !isBlank(levelObs[y + changedY][x]))
				return false;
		}
		return true;
	}

	/**
	 * A pit is defined as an edge with no bottom. The width of a pit is defined
	 * by how far across to the other side. The location of the pit is the
	 * leftmost side.
	 * 
	 * @param rete
	 *            The Rete object to assert to.
	 * @param environment
	 *            The MarioEnvironment.
	 * @param levelObs
	 *            The current level observations.
	 * @param x
	 *            The x position to start from (known to be solid).
	 * @param y
	 *            The y position to start from (known to be solid).
	 */
	private void checkForPit(Rete rete, MarioEnvironment environment,
			byte[][] levelObs, byte x, byte y) throws Exception {
		// First check if this is an edge
		// Is there anything above this?
		if (((y - 1) >= 0) && isBlank(levelObs[y - 1][x])) {
			// Only check to the right.
			int xMod = x + 1;
			// This side is blank - we have an edge
			if ((xMod < levelObs[y].length) && isBlank(levelObs[y][xMod])) {
				// Decrease y to the bottom. If blank, we have a pit
				boolean isPit = true;
				int yMod = y + 1;
				while (isPit && yMod < levelObs.length) {
					isPit = isBlank(levelObs[yMod][xMod]);
					yMod++;
				}

				// If a pit, determine the width
				if (isPit) {
					xMod++;
					boolean isSolid = false;
					while (!isSolid) {
						xMod++;
						if (xMod >= levelObs[y].length)
							break;
						// Scan down from y + JUMP_HEIGHT
						byte yScan = (byte) (y - MAX_JUMP_HEIGHT / 16 + 1);
						yScan = (byte) Math.max(0, yScan);
						while (yScan < levelObs.length && !isSolid) {
							if (!isBlank(levelObs[yScan][xMod])) {
								isSolid = true;
								// If the other side is higher, use that as y
								// coord
								if (yScan < y)
									y = yScan;
							}
							yScan++;
						}
					}

					// Assert the pit
					// Find the left side
					assertThing(rete, environment, "pit", "pit", x + 1, y,
							false, levelObs, xMod - x - 1);
				}
			}
		}
	}

	/**
	 * Initialises extra environment variables that are used.
	 * 
	 * @param environment
	 *            The MarioEnvironment.
	 */
	private void initialiseExtraEnvironmentObservations(
			MarioEnvironment environment) {
		marioCentreX_ = environment.getReceptiveFieldWidth() / 2;
		marioCentreY_ = environment.getReceptiveFieldHeight() / 2;

		if (environment.getTick() <= 1) {
			// Environment initialisation
			staticObjectFacts_.clear();

			isMarioInAir_ = false;
			prevBoolAction_ = new boolean[Environment.numberOfKeys];
			marioDirection_ = Environment.MARIO_KEY_RIGHT;
			marioGroundPos_ = new float[2];
			return;
		}

		// Note static objects when Mario is on the ground.
		if (!environment.isMarioOnGround())
			isMarioInAir_ = true;
		else {
			isMarioInAir_ = false;
			marioGroundPos_ = Arrays.copyOf(environment.getMarioFloatPos(), 2);
			staticObjectFacts_.clear();
		}
	}

	/**
	 * If the given value represents a level observation 'blank', or passable
	 * tile that can be entered.
	 * 
	 * @param val
	 *            The value being checked.
	 * @return True if the value is blank.
	 */
	private boolean isBlank(byte val) {
		if ((val == 0) || (val == ObservationConstants.LVL_COIN))
			return true;
		return false;
	}

	/**
	 * Roughly checks if Mario's jump will be blocked by geometry.
	 * 
	 * @param environment
	 *            The MarioEnvironment.
	 * @param gridX
	 *            The x location to jump to.
	 * @param gridY
	 *            The y location to jump to.
	 * @param levelObs
	 *            The level observation.
	 * @param jumpInto
	 *            If the object can be jumped into.
	 * @return True if the jump is (approximately) blocked.
	 */
	private boolean jumpBlocked(MarioEnvironment environment, int gridX,
			int gridY, byte[][] levelObs, boolean jumpInto) {
		// If off screen, return false
		if (gridY < 0 || gridY >= levelObs.length || gridX < 0
				|| gridX >= levelObs[0].length)
			return false;

		int marioSize = (environment.getMarioMode() == 0) ? 1 : 2;
		// If the object is above Mario
		if (gridY <= marioCentreY_) {
			// Decrement y above mario, checking for objects immediately
			// overhead.
			int yMod = marioCentreY_ - marioSize;
			do {
				if (!isBlank(levelObs[yMod][marioCentreX_]))
					return true;
				yMod--;
			} while (yMod >= gridY);

			// Also, check that Mario can jump on it (if not into) - that is,
			// there is space above the thing for Mario to fit.
			if (!jumpInto
					&& !canMarioFit(environment, gridX, gridY, levelObs, false))
				return true;
		} else {
			// Perform the same operation, but starting from the object and
			// going up to Mario.
			int yMod = gridY - 1;
			do {
				if (!isBlank(levelObs[yMod][gridX]))
					return true;
				yMod--;
			} while (yMod >= marioCentreY_);
		}
		return false;
	}

	/**
	 * Returns the actions necessary to take if Mario wants to jump onto a
	 * specified location. Mario first moves close enough to be able to jump,
	 * then jumps.
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
	private boolean[] jumpOnto(float startX, float startY, float currentX,
			float currentY, float endX, float endY) {
		boolean[] actionArray = new boolean[Environment.numberOfKeys];

		startX *= negModifier_;
		currentX *= negModifier_;
		endX *= negModifier_;

		// If Mario is already on the end point, do nothing
		if (startEndDiffs_[0] < CELL_SIZE / 2
				&& Math.abs(startEndDiffs_[1]) < CELL_SIZE)
			return actionArray;

		// VERTICAL MOVEMENT

		// MINIMISING JUMP HEIGHT
		// Continue to jump only if the current x position is less than halfway
		// through the jump.

		// Modify the values for jumping if the y location is lower
		float releaseJumpPoint = startEndDiffs_[0] / 3;
		releaseJumpPoint = Math.max(releaseJumpPoint, 1);
		// Special case, if the object is immediately beside Mario
		if (startEndDiffs_[0] <= CELL_SIZE && startEndDiffs_[1] > 0)
			releaseJumpPoint = Integer.MAX_VALUE;
		if (currentEndDiffs_[0] > releaseJumpPoint || currentEndDiffs_[1] > 0)
			actionArray[Mario.KEY_JUMP] = true;
		else
			actionArray[Mario.KEY_JUMP] = false;

		// Running or not
		if (startEndDiffs_[0] >= MAX_JUMP_DIST)
			actionArray[Mario.KEY_SPEED] = true;

		// HORIZONTAL MOVEMENT
		float nearEndX = endX - directedCellSize_ / 2;
		if (actionArray[Mario.KEY_SPEED])
			nearEndX = endX - directedCellSize_ / 1.5f;
		float farEndX = endX + directedCellSize_ / 2;

		// Check for special case when Mario is below object and next to
		if (currentEndDiffs_[1] > 0 && currentEndDiffs_[0] <= CELL_SIZE) {
			// Move from the object so as not to get caught under it
			actionArray[direction_] = false;
			actionArray[oppDirection_] = true;
		} else {
			// Start of jump
			if (currentX < nearEndX) {
				actionArray[direction_] = true;
				actionArray[oppDirection_] = false;
			} else if (currentX < endX) {
				// Between nearEnd and end
				actionArray[direction_] = false;
				actionArray[oppDirection_] = false;
			} else if (currentX < farEndX) {
				// Between end and farEnd
				actionArray[direction_] = false;
				actionArray[oppDirection_] = true;
			} else {
				// Past the end point
				actionArray[Mario.KEY_SPEED] = true;
				actionArray[direction_] = false;
				actionArray[oppDirection_] = true;
			}
		}

		return actionArray;
	}

	/**
	 * Returns the actions necessary to take if Mario wants to move to a
	 * particular point. Mario may jump to get there.
	 * 
	 * @param startX
	 *            The x point from which the move was made.
	 * @param startY
	 *            The y point from which the move was made.
	 * @param currentX
	 *            Mario's current x point.
	 * @param currentY
	 *            Mario's current y point.
	 * @param endX
	 *            The x location to move to.
	 * @param endY
	 *            The y location to move to.
	 * @param bigMario
	 *            If Mario is currently big.
	 * @return A boolean array of action required to move to the object
	 */
	private boolean[] move(float startX, float startY, float marioX,
			float marioY, float endX, float endY, boolean bigMario) {
		boolean[] actionArray = new boolean[Environment.numberOfKeys];

		actionArray[direction_] = true;
		actionArray[Environment.MARIO_KEY_SPEED] = true;

		// Jumping if thing is in same column
		if (startEndDiffs_[0] < CELL_SIZE && startEndDiffs_[1] > 0)
			actionArray[Environment.MARIO_KEY_JUMP] = true;

		// Check if obstacles are in the way
		int midY = basicLevelObs_.length / 2;
		int midX = basicLevelObs_[midY].length / 2;
		int jumpPoint = (int) Math.ceil(Math.min(currentEndDiffs_[0]
				/ CELL_SIZE, MAX_JUMP_DIST / CELL_SIZE));
		for (int x = (int) jumpPoint; x >= 1; x--) {
			// If there is an obstacle
			// Big Mario check.
			boolean obstacle = !ObservationConstants
					.isEmptyCell(basicLevelObs_[midY][midX + (x * negModifier_)]);
			int yOffset = 0;
			if (bigMario
					&& !ObservationConstants
							.isEmptyCell(basicLevelObs_[midY - 1][midX
									+ (x * negModifier_)])) {
				yOffset = 1;
				obstacle = true;
			}

			if (obstacle) {
				// Find the highest point of the obstacle
				while (yOffset < midY
						&& !ObservationConstants
								.isEmptyCell(basicLevelObs_[midY - yOffset - 1][midX
										+ (x * negModifier_)]))
					yOffset++;

				return jumpOnto(startX, startY, marioX, marioY, startX
						+ directedCellSize_ * x, startY - CELL_SIZE * yOffset);
			}
		}

		return actionArray;
	}

	/**
	 * Pickup a shell (and continue to hold it until shot or used as a shield).
	 * 
	 * @param startX
	 *            The x point from which the move was made.
	 * @param startY
	 *            The y point from which the move was made.
	 * @param currentX
	 *            Mario's current x point.
	 * @param currentY
	 *            Mario's current y point.
	 * @param shellX
	 *            The x location of the shell to move to.
	 * @param shellY
	 *            The y location of the shell to move to.
	 * @param bigMario
	 *            If Mario is currently big.
	 * @return A boolean array of action required to move to the object
	 */
	private boolean[] pickup(float startX, float startY, float currentX,
			float currentY, float shellX, float shellY, boolean bigMario) {
		boolean[] actionArray = new boolean[Environment.numberOfKeys];

		// Pickup behaviour
		if (currentEndDiffs_[0] < CELL_SIZE
				&& Math.abs(currentEndDiffs_[1]) < CELL_SIZE) {
			actionArray[Mario.KEY_SPEED] = true;
			actionArray[direction_] = true;
			return actionArray;
		} else
			return move(startX, startY, currentX, currentY, shellX, shellY,
					bigMario);
	}

	/**
	 * Pre-jumping to method to avoid recursive loops.
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
	 * @param bigMario
	 *            If Mario is currently big.
	 * @return A boolean array of action required to jump to the object
	 */
	private boolean[] preJumpOnto(float startX, float startY, float currentX,
			float currentY, float endX, float endY, boolean bigMario) {
		// If Mario is beyond jumping distance, move closer.
		if (canJump(currentEndDiffs_, 0)) {
			return jumpOnto(startX, startY, currentX, currentY, endX, endY);
		} else
			return move(startX, startY, currentX, currentY, endX, endY,
					bigMario);
	}

	/**
	 * Pre-jumping to method to avoid recursive loops.
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
	 * @param width
	 *            The width of the thing to jump over.
	 * @param bigMario
	 *            If Mario is currently big.
	 * @return A boolean array of action required to jump to the object
	 */
	private boolean[] preJumpOver(float startX, float startY, float currentX,
			float currentY, float endX, float endY, int width, boolean bigMario) {
		// If Mario is beyond jumping distance, move closer.
		float jumpPoint = (width + 1.5f) * CELL_SIZE;
		if (canJump(currentEndDiffs_, jumpPoint)) {
			if (negModifier_ > 0)
				endX = endX + jumpPoint;
			else
				endX = endX - 1.5f * CELL_SIZE;
			calculateDirectionVariables(startX, startY, currentX, currentY,
					endX, endY);
			return jumpOnto(startX, startY, currentX, currentY, endX, endY);
		} else
			return move(startX, startY, currentX, currentY, endX, endY,
					bigMario);
	}

	/**
	 * Returns the actions necessary to take if Mario wants to search a block.
	 * Mario will first move under the block, then jump.
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
	 * @param bigMario
	 *            If Mario is currently big.
	 * @return A boolean array of action required to search the brick.
	 */
	private boolean[] search(float startX, float startY, float currentX,
			float currentY, float endX, float endY, boolean bigMario) {
		boolean[] actionArray = new boolean[Environment.numberOfKeys];

		// Get within one cell beneath the object
		int movement = (int) (Math.max(1, startEndDiffs_[1] / (1 * CELL_SIZE)));
		if (startEndDiffs_[0] > CELL_SIZE * movement) {
			return move(startX, startY, currentX, currentY, endX, endY,
					bigMario);
		}

		// Otherwise, Mario is under it enough to jump and centre on it
		actionArray[Environment.MARIO_KEY_JUMP] = true;
		if (currentX < endX - CELL_SIZE / 2)
			actionArray[Environment.MARIO_KEY_RIGHT] = true;
		else if (currentX > endX + CELL_SIZE / 2)
			actionArray[Environment.MARIO_KEY_LEFT] = true;

		return actionArray;
	}

	/**
	 * Shoot an enemy, by turning in that direction and shooting.
	 * 
	 * @param prevAction
	 *            Mario's previous action array.
	 * @param marioDirection
	 *            The direction Mario is facing.
	 * @return A boolean array of action required to shoot.
	 */
	private boolean[] shoot(boolean[] prevAction, int marioDirection) {
		boolean[] actionArray = new boolean[Environment.numberOfKeys];
		if (direction_ != marioDirection) {
			if (marioDirection == Environment.MARIO_KEY_RIGHT)
				actionArray[Environment.MARIO_KEY_LEFT] = true;
			else if (marioDirection == Environment.MARIO_KEY_LEFT)
				actionArray[Environment.MARIO_KEY_RIGHT] = true;
		} else {
			// If holding fire, release it and vice-versa
			actionArray[Environment.MARIO_KEY_SPEED] = !prevAction[Environment.MARIO_KEY_SPEED];
		}
		return actionArray;
	}

	/**
	 * Checks if an enemy coordinate is stuck in geometry. Special case for
	 * Koopa's which are tall
	 * 
	 * @param x
	 *            The enemy's x coord.
	 * @param y
	 *            The enemy's y coord.
	 * @param levelObs
	 *            The level observations. The enemy may be outside these bounds.
	 * @param enemyType
	 *            The type of enemy being asserted.
	 * @return True if the enemy is stuck in geometry, false otherwise.
	 */
	private boolean stuckInGeometry(float x, float y, byte[][] levelObs,
			float enemyType) {
		for (int i = -1; i <= 1; i++) {
			int relGridX = Math.round((x + i * LevelScene.cellSize / 2)
					/ LevelScene.cellSize)
					+ marioCentreX_;
			int relGridY = Math.round((y - LevelScene.cellSize / 2)
					/ LevelScene.cellSize)
					+ marioCentreY_;
			int relGridY1 = Math.max(0, relGridY - 1);
			if (relGridY < 0 || relGridY >= levelObs.length || relGridX < 0
					|| relGridX >= levelObs[0].length)
				return false;
			// Special case for Koopas (tall)
			switch ((int) enemyType) {
			case ObservationConstants.ENMY_GREEN_KOOPA:
			case ObservationConstants.ENMY_GREEN_KOOPA_WINGED:
			case ObservationConstants.ENMY_RED_KOOPA:
			case ObservationConstants.ENMY_RED_KOOPA_WINGED:
				if (isBlank(levelObs[relGridY][relGridX])
						|| isBlank(levelObs[relGridY1][relGridX]))
					return false;
				break;
			default:
				if (isBlank(levelObs[relGridY][relGridX]))
					return false;
			}
		}
		return true;
	}

	/**
	 * Applies the action chosen by the agent to the environment - returning a
	 * boolean array of keystroke actions to take.
	 * 
	 * @param action
	 *            The action to take.
	 * @param startPos
	 *            The starting position of Mario.
	 * @param marioPos
	 *            The current position of Mario.
	 * @param basicLevelObs
	 *            The basic observation for the level.
	 * @param prevAction
	 *            The previous boolean array of actions.
	 * @param marioDirection
	 *            Mario's current direction (+1 for facing right, -1 for facing
	 *            left.)
	 * @param bigMario
	 *            If Mario is currently big.
	 * @param carrying
	 *            If Mario is currently carrying a shell.
	 * @return A boolean array of keystroke actions to take at the time.
	 */
	public boolean[] applyAction(RelationalPredicate action, float[] startPos,
			float[] marioPos, byte[][] basicLevelObs, boolean[] prevAction,
			int marioDirection, boolean bigMario, boolean carrying) {
		basicLevelObs_ = basicLevelObs;
		boolean[] actionArray = new boolean[Environment.numberOfKeys];
		if (action == null) {
			return actionArray;
		}

		// Parse the coords
		float x, y;
		if (action.getArguments()[0].equals("goal")) {
			x = marioPos[0] + MAX_JUMP_DIST_RUNNING * 2;
			y = marioPos[1];
		} else {
			String[] argSplit = action.getArguments()[0].split("_");
			x = Float.parseFloat(argSplit[1]);
			y = Float.parseFloat(argSplit[2]);
		}

		// Direction specific variables
		calculateDirectionVariables(startPos[0], startPos[1], marioPos[0],
				marioPos[1], x, y);

		// Determine the actions
		if (action.getFactName().equals("move"))
			actionArray = move(startPos[0], startPos[1], marioPos[0],
					marioPos[1], x, y, bigMario);
		else if (action.getFactName().equals("search"))
			actionArray = search(startPos[0], startPos[1], marioPos[0],
					marioPos[1], x, y, bigMario);
		else if (action.getFactName().equals("jumpOnto")) {
			actionArray = preJumpOnto(startPos[0], startPos[1], marioPos[0],
					marioPos[1], x, y, bigMario);
		} else if (action.getFactName().equals("jumpOver")) {
			int width = Integer.parseInt(action.getArguments()[2]);
			actionArray = preJumpOver(startPos[0], startPos[1], marioPos[0],
					marioPos[1], x, y, width, bigMario);
		} else if (action.getFactName().equals("shootFireball")) {
			actionArray = shoot(prevAction, marioDirection);
		} else if (action.getFactName().equals("shootShell")) {
			actionArray = shoot(prevAction, marioDirection);
		} else if (action.getFactName().equals("pickup")) {
			actionArray = pickup(startPos[0], startPos[1], marioPos[0],
					marioPos[1], x, y, bigMario);
		}

		// TODO Mario continues to hold shell as long as possible?
		return actionArray;
	}

	/**
	 * Checks if Mario can jump to/over a given point from his current location,
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
	 * @param isMarioInAir
	 *            If Mario is airbourne.
	 * @return A boolean array of size 2: Can Mario: jump on the thing, and jump
	 *         over the thing.
	 */
	public boolean[] canJumpOnOver(float x, float y,
			MarioEnvironment environment, boolean jumpInto, boolean isMarioInAir) {
		boolean[] canJumpOnOver = new boolean[2];
		float cellSize = ch.idsia.benchmark.mario.engine.LevelScene.cellSize;
		float[] marioPos = environment.getMarioFloatPos();

		// Mario can jump into objects above him to a particular
		// height (assuming nothing is blocking)
		float extra = (environment.getMarioMode() > 0) ? cellSize * 2
				: cellSize;

		float[] diffs = { Math.abs(marioPos[0] - x), marioPos[1] - y };
		// If the x difference is on the same column
		if (diffs[0] <= cellSize / 2) {
			if (jumpInto) {
				// If the y difference is less than the jump height (+ extra)
				// AND the thing is not under Mario, return true.
				if (diffs[1] <= MAX_JUMP_HEIGHT + extra)
					canJumpOnOver[0] = true;
			} else if (isMarioInAir)
				canJumpOnOver[0] = true;
			canJumpOnOver[1] = true;
		} else {
			// If not in the same axis, 'reduce' the height of the thing being
			// jumped into if jumping into something
			if (jumpInto)
				diffs[1] -= extra;

			if (canJump(diffs, 0))
				canJumpOnOver[0] = true;

			// Can only jump over things about a cell and a half lower than MAX
			// JUMP HEIGHT
			if (canJump(diffs, cellSize * 1.5f))
				canJumpOnOver[1] = true;
		}

		// return false;
		return canJumpOnOver;
	}

	@Override
	protected Rete assertStateFacts(Rete rete, Object... args) throws Exception {
		MarioEnvironment environment = (MarioEnvironment) args[0];

		initialiseExtraEnvironmentObservations(environment);

		// Player
		rete.assertString("(mario player))");
		// Mario state
		switch (environment.getMarioMode()) {
		case 2:
			rete.assertString("(marioPower fire))");
			break;
		case 1:
			rete.assertString("(marioPower large))");
			break;
		case 0:
			rete.assertString("(marioPower small))");
			break;
		}

		// Run through the level observations
		byte[][] levelObs = environment.getLevelSceneObservationZ(1);
		byte[][] enemyObs = environment.getEnemiesObservationZ(0);
		float[] enemyPos = environment.getEnemiesFloatPos();

		// Assert the level objects
		for (byte y = 0; y < levelObs.length; y++) {
			for (byte x = 0; x < levelObs[y].length; x++) {
				// Level objects, like coins and solid objects
				assertLevelObjects(rete, environment, levelObs, enemyObs, x, y);
			}
			if (PolicyGenerator.debugMode_)
				System.out.println();
		}

		// Reassert static objects
		if (isMarioInAir_)
			for (String fact : staticObjectFacts_)
				rete.assertString(fact);

		// Assert the enemies
		Collection<String> currentShells = new HashSet<String>();
		for (int e = 0; e < enemyPos.length; e++) {
			float enemyType = enemyPos[e++];
			float x = enemyPos[e++];
			float y = enemyPos[e];
			// Check it's not stuck in geometry
			if (!stuckInGeometry(x, y, levelObs, enemyType)) {
				// Enemy objects, like fireFlower, mushroom, all enemies and
				// projectiles
				String isShell = assertEnemyObjects(rete, environment,
						enemyType, x, y, levelObs);
				if (isShell != null)
					currentShells.add(isShell);
			}
		}
		shellPositions_ = currentShells;

		// Ever present goal
		rete.assertString("(flag goal))");
		rete.assertString("(distance goal " + marioCentreX_
				* LevelScene.cellSize + ")");
		rete.assertString("(canJumpOn goal)");
		rete.assertString("(heightDiff goal 0)");

		StateSpec.getInstance().assertGoalPred(new ArrayList<String>(), rete);

		// Send the state of the system
		return rete;
	}

	@Override
	public Object groundActions(PolicyActions actions, Object... args) {
		MarioEnvironment environment = (MarioEnvironment) args[0];
		byte[][] basicLevelObservation = environment
				.getLevelSceneObservationZ(2);
		float[] marioFloatPos = environment.getMarioFloatPos();
		boolean bigMario = environment.getMarioMode() != 0;
		boolean carrying = environment.isMarioCarrying();

		// An array which determines which actions are pressed {dir, jump?,
		// run?} A non-zero value means the action has been selected (-1
		// left/false, +1 right/true)
		RelationalPredicate bestAction = null;
		List<boolean[]> selectedActions = new ArrayList<boolean[]>();
		for (Collection<FiredAction> firedActions : actions.getActions()) {
			double bestWeight = Integer.MIN_VALUE;

			// Find the individual distance weighting and direction of each
			// action in the ArrayList
			for (FiredAction firedAction : firedActions) {
				RelationalPredicate action = firedAction.getAction();
				boolean[] actionArray = applyAction(action, marioGroundPos_,
						marioFloatPos, basicLevelObservation, prevBoolAction_,
						marioDirection_, bigMario, carrying);
				double distance = Math.abs(Double.parseDouble(action
						.getArguments()[1]));
				double actionWeight = (1 / distance);
				// If a closer object has been found, act on that.
				if (actionWeight > bestWeight) {
					bestWeight = actionWeight;
					selectedActions.clear();
					bestAction = action;
				}
				if (actionWeight == bestWeight) {
					// If two objects are of the same distance, and they're not
					// the same boolean[] action, skip this action and move onto
					// the next to decide.
					selectedActions.add(actionArray);
					firedAction.triggerRule();
					break;
				}
			}

			if (!selectedActions.isEmpty()) {
				if (PolicyGenerator.debugMode_)
					System.out.println(bestAction);
				break;
			}
		}

		// If the selected action isn't null, reset the previous bool action.
		if (!selectedActions.isEmpty())
			prevBoolAction_ = selectedActions.get(PolicyGenerator.random_
					.nextInt(selectedActions.size()));

		// If Mario is on the ground and cannot jump, allow him time to breathe
		if (environment.isMarioOnGround() && !environment.isMarioAbleToJump()) {
			prevBoolAction_[Environment.MARIO_KEY_JUMP] = false;
			prevBoolAction_[Environment.MARIO_KEY_LEFT] = false;
			prevBoolAction_[Environment.MARIO_KEY_RIGHT] = false;
		}

		if (prevBoolAction_[Environment.MARIO_KEY_RIGHT]
				&& !prevBoolAction_[Environment.MARIO_KEY_LEFT])
			marioDirection_ = Environment.MARIO_KEY_RIGHT;
		else if (prevBoolAction_[Environment.MARIO_KEY_LEFT]
				&& !prevBoolAction_[Environment.MARIO_KEY_RIGHT])
			marioDirection_ = Environment.MARIO_KEY_LEFT;

		return prevBoolAction_;
	}

	/**
	 * Checks if the episode has terminated.
	 * 
	 * @return 2 if level completed, 1 if Mario died, 0 otherwise.
	 */
	public int isTerminal(Object... args) {
		MarioEnvironment environment = (MarioEnvironment) args[0];
		if (environment.isLevelFinished()) {
			if (environment.getMarioStatus() == Environment.MARIO_STATUS_DEAD
					|| environment.getMarioFloatPos()[1] > environment
							.getLevelHeight() * LevelScene.cellSize) {
				return 1;
			}

			// If Mario didn't die, increment the difficulty
			return 2;
		}
		return 0;
	}
}
