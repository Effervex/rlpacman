package mario;

import relationalFramework.FiredAction;
import relationalFramework.ObjectObservations;
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
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;
import jess.JessException;
import jess.Rete;

public class RLMarioRelationalWrapper extends RelationalWrapper {
	private int marioCentreX_;
	private int marioCentreY_;
	/** Needed to check if the shells are passive. */
	private Collection<String> shellPositions_ = new HashSet<String>();
	private boolean isMarioInAir_;
	/** Mario's facing direction. */
	private int marioDirection_ = Environment.MARIO_KEY_RIGHT;
	private float[] marioGroundPos_ = { 32, 32 };
	/** Records Mario's previous boolean array action. */
	private boolean[] prevBoolAction_ = new boolean[Environment.numberOfKeys];
	private Collection<String> staticObjectFacts_ = new HashSet<String>();

	@Override
	public Rete formObservations(Object... args) {
		Rete rete = StateSpec.getInstance().getRete();
		try {
			rete.reset();
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
					assertLevelObjects(rete, environment, levelObs, enemyObs,
							x, y);
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

			StateSpec.getInstance().assertGoalPred(new ArrayList<String>(),
					rete);
			rete.run();

			// Adding the valid actions
			ObjectObservations.getInstance().validActions = StateSpec
					.getInstance().generateValidActions(rete);

		} catch (Exception e) {
			e.printStackTrace();
		}

		// Send the state of the system
		return rete;
	}

	/**
	 * Initialises extra environment variables that are used.
	 * 
	 * @param environment The MarioEnvironment.
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

	@Override
	public Object groundActions(PolicyActions actions, Object... args) {
		MarioEnvironment environment = (MarioEnvironment) args[0];
		byte[][] basicLevelObservation = environment
				.getLevelSceneObservationZ(2);
		float[] marioFloatPos = environment.getMarioFloatPos();
		boolean bigMario = environment.getMarioMode() != 0;

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
				boolean[] actionArray = ((RLMarioStateSpec) StateSpec
						.getInstance()).applyAction(action, marioGroundPos_,
						marioFloatPos, basicLevelObservation, prevBoolAction_,
						marioDirection_, bigMario);
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
		boolean[] jumpOnOver = ((RLMarioStateSpec) StateSpec.getInstance())
				.canJumpOnOver(x, y, environment, jumpInto, isMarioInAir_);
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
						byte yScan = (byte) (y
								- RLMarioStateSpec.MAX_JUMP_HEIGHT / 16 + 1);
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
