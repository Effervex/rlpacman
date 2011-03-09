package mario;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import jess.JessException;
import jess.Rete;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;

import competition.cig.robinbaumgarten.astar.sprites.Mario;

import ch.idsia.benchmark.mario.engine.GlobalOptions;
import ch.idsia.benchmark.mario.engine.LevelScene;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;
import ch.idsia.tools.MarioAIOptions;

import relationalFramework.ActionChoice;
import relationalFramework.ObjectObservations;
import relationalFramework.PolicyGenerator;
import relationalFramework.RuleAction;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

public class RLMarioEnvironment implements EnvironmentInterface {
	private Rete rete_;
	private MarioEnvironment environment_;
	private boolean experimentMode_ = false;
	private MarioAIOptions cmdLineOptions_;
	private StringFact prevAction_;

	@Override
	public void env_cleanup() {
		environment_ = null;
	}

	@Override
	public String env_init() {
		environment_ = MarioEnvironment.getInstance();
		cmdLineOptions_ = new MarioAIOptions();
		cmdLineOptions_.setVisualization(!experimentMode_);
		cmdLineOptions_.setEnemies("off");
		cmdLineOptions_.setLevelDifficulty(0);
//		 cmdLineOptions_.setFPS(100);
		cmdLineOptions_.setTimeLimit(50);
		// GlobalOptions.isShowReceptiveField = true;

		return null;
	}

	@Override
	public String env_message(String arg0) {
		if (arg0.equals("maxSteps")) {
			return 10000 + "";
		} else if (arg0.equals("freeze")) {
			PolicyGenerator.getInstance().freeze(true);
			return null;
		} else if (arg0.equals("unfreeze")) {
			PolicyGenerator.getInstance().freeze(false);
			return null;
		} else if (arg0.equals("-e")) {
			// Run the program in experiment mode (No GUI).
			experimentMode_ = true;
		}
		return null;
	}

	@Override
	public Observation env_start() {
		resetEnvironment();
		environment_.tick();

		while (GlobalOptions.isGameplayStopped) {
			// Idle...
			environment_.tick();
		}
		prevAction_ = null;

		// Only make decisions when Mario is on the ground
		while (!environment_.isMarioOnGround())
			environment_.tick();

		return formObservations(rete_, 1);
	}

	@Override
	public Reward_observation_terminal env_step(Action arg0) {
		// Applying the action (up down left right or nothing)
		ActionChoice actions = (ActionChoice) ObjectObservations.getInstance().objectArray[0];
		float[] marioPos = Arrays.copyOf(environment_.getMarioFloatPos(), 2);
		environment_.performAction(chooseLowAction(actions.getActions(),
				marioPos));
		environment_.tick();

		while (GlobalOptions.isGameplayStopped) {
			// Idle...
			environment_.tick();
		}

		// Only make decisions when Mario is on the ground
		while (!environment_.isMarioOnGround()
				&& environment_.getMarioStatus() != Mario.STATUS_WIN) {
			environment_.performAction(chooseLowAction(actions.getActions(),
					marioPos));
			environment_.tick();

			// Check for endless fall bug
			if (environment_.getMarioFloatPos()[1] > environment_
					.getLevelHeight()
					* LevelScene.cellSize)
				break;
		}
		prevAction_ = null;

		Observation obs = formObservations(rete_, 1);
		float reward = 0;
		int terminal = isTerminal();
		if (terminal == 1)
			reward = environment_.getEvaluationInfo().computeWeightedFitness();
		Reward_observation_terminal rot = new Reward_observation_terminal(
				reward, obs, terminal);
		return rot;
	}

	/**
	 * Resets the environment back to normal.
	 */
	public void resetEnvironment() {
		cmdLineOptions_.setLevelRandSeed(PolicyGenerator.random_.nextInt());
		// cmdLineOptions_.setLevelRandSeed(-1808813221);
		environment_.reset(cmdLineOptions_);
		if (!experimentMode_ && !GlobalOptions.isScale2x)
			GlobalOptions.changeScale2x();

		rete_ = StateSpec.getInstance().getRete();
	}

	/**
	 * Chooses a direction based off the chosen high action to follow.
	 * 
	 * @param ruleActions
	 *            The actions the agent will take. May be multiple versions of
	 *            the same action with different arguments, based on what
	 *            matched the action conditions.
	 * @param startPos
	 *            The starting position of Mario in the case of jumping.
	 * @return The low action to use.
	 */
	private boolean[] chooseLowAction(ArrayList<RuleAction> ruleActions,
			float[] startPos) {
		Map<Integer, SortedMap<Double, StringFact>> representativeRule = new HashMap<Integer, SortedMap<Double, StringFact>>();
		Map<Integer, Double> weightedActions = new HashMap<Integer, Double>();
		byte[][] basicLevelObservation = environment_
				.getLevelSceneObservationZ(2);
		float[] marioFloatPos = environment_.getMarioFloatPos();
		
		// If there is a previous action take that (for when Mario is in the air
		// and other non-decision making situations)
		if (prevAction_ != null)
			return ((RLMarioStateSpec) StateSpec
					.getInstance()).applyAction(prevAction_, startPos,
							marioFloatPos, basicLevelObservation);

		// Run through the actions, storing identical action mappings under the
		// same weight. Storage is through bit shifted operations.
		int i = 0;
		int bestBitwise = -1;
		double bestWeight = -1;
		for (RuleAction ruleAction : ruleActions) {
			List<StringFact> actionStrings = ruleAction.getTriggerActions();
			double policyWeight = (1.0 * ruleActions.size() - i)
					/ ruleActions.size();
			policyWeight *= policyWeight;

			// Find the individual distance weighting and direction of each
			// action in the ArrayList
			for (StringFact action : actionStrings) {
				boolean[] actionArray = ((RLMarioStateSpec) StateSpec
						.getInstance()).applyAction(action, startPos,
						marioFloatPos, basicLevelObservation);
				int bitwise = booleanArrayToInt(actionArray);
				double distance = Double.parseDouble(action.getArguments()[1]);

				// Store the values, adding to existing values if necessary
				Double oldWeight = weightedActions.get(bitwise);
				SortedMap<Double, StringFact> bestActions = representativeRule
						.get(bitwise);
				if (oldWeight == null) {
					oldWeight = 0d;
					bestActions = new TreeMap<Double, StringFact>();
					representativeRule.put(bitwise, bestActions);
				}
				// Add the weighted inverse distance
				double actionWeight = (policyWeight / distance);
				double newWeight = oldWeight + actionWeight;
				weightedActions.put(bitwise, newWeight);
				bestActions.put(actionWeight, action);

				// Update best weights
				if (newWeight > bestWeight) {
					bestWeight = newWeight;
					bestBitwise = bitwise;
				}
			}
		}

		// If Mario is on the ground and cannot jump, allow him time to breathe
		boolean[] actionArray = intToBooleanArray(bestBitwise);
		if (environment_.isMarioOnGround() && !environment_.isMarioAbleToJump())
			actionArray[Environment.MARIO_KEY_JUMP] = false;

		// Output the 'firing' action.
		SortedMap<Double, StringFact> bestRules = representativeRule
				.get(bestBitwise);
		StringFact bestAction = bestRules.get(bestRules.lastKey());
//		System.out.println("Action: " + bestAction);
		prevAction_ = bestAction;

		return actionArray;
	}

	/**
	 * Checks if the episode has terminated.
	 * 
	 * @return 1 if Game over or level complete, false otherwise.
	 */
	private int isTerminal() {
		if (environment_.isLevelFinished()) {
			if (environment_.getMarioStatus() == Environment.MARIO_STATUS_DEAD
					|| environment_.getMarioFloatPos()[1] > environment_
							.getLevelHeight()
							* LevelScene.cellSize)
				ObjectObservations.getInstance().setNoPreGoal();
			return 1;
		}
		return 0;
	}

	/**
	 * A method for calculating observations about the current PacMan state.
	 * These are put into the Rete object, and placed into the
	 * ObjectObservations object.
	 * 
	 * @param rete
	 *            The rete object to add observations to.
	 * @param levelZoom
	 *            The zoom level for the level observations.
	 * @return An observation of the current state
	 */
	private Observation formObservations(Rete rete, int levelZoom) {
		try {
			rete.reset();

			// Player
			rete.assertString("(mario player))");

			// Directions
			rete.assertString("(directionType left))");
			rete.assertString("(directionType right))");

			// Run through the level observations
			byte[][] levelObs = environment_
					.getLevelSceneObservationZ(levelZoom);
			float[] enemyPos = environment_.getEnemiesFloatPos();

			// Assert the level objects
			for (byte y = 0; y < levelObs.length; y++) {
				for (byte x = 0; x < levelObs[y].length; x++) {
					// TODO Add Pits
					// Level objects, like coins and solid objects
					assertLevelObjects(rete, levelObs, x, y);
				}
				if (PolicyGenerator.debugMode_)
					System.out.println();
			}

			// Assert the enemies
			for (int e = 0; e < enemyPos.length; e++) {
				// Enemy objects, like fireFlower, mushroom, all enemies and
				// projectiles
				assertEnemyObjects(rete, enemyPos[e++], enemyPos[e++],
						enemyPos[e]);
			}

			// Ever present goal
			rete.assertString("(flag goal))");
			rete.assertString("(distance goal "
					+ environment_.getReceptiveFieldWidth() / 2
					* LevelScene.cellSize + ")");
			rete.assertString("(direction goal right)");
			rete.assertString("(heightDiff goal 0)");

			// Other details
			rete.assertString("(numCoins "
					+ environment_.getEvaluationInfo().coinsGained + ")");
			rete.assertString("(time "
					+ environment_.getEvaluationInfo().timeLeft + ")");

			rete.run();

			// Adding the valid actions
			ObjectObservations.getInstance().validActions = StateSpec
					.getInstance().generateValidActions(rete_);
			// rete.eval("(facts)");
			// System.out.println(ObjectObservations.getInstance().validActions);

		} catch (Exception e) {
			e.printStackTrace();
		}

		// Send the state of the system
		ObjectObservations.getInstance().predicateKB = rete;
		Observation obs = new Observation();
		obs.charArray = ObjectObservations.OBSERVATION_ID.toCharArray();

		return obs;
	}

	/**
	 * Asserts any level objects present at a given point within the observation
	 * field and any relevant relations that object has.
	 * 
	 * @param rete
	 *            The Rete object to assert to.
	 * @param levelObs
	 *            The level observation field; only detailing the relevant
	 *            objects at a distance from Mario.
	 * @param x
	 *            The x coord to examine.
	 * @param y
	 *            The y coord to examine.
	 * @throws Exception
	 *             Should something go awry.
	 */
	private void assertLevelObjects(Rete rete, byte[][] levelObs, byte x, byte y)
			throws Exception {
		byte levelVal = levelObs[y][x];
		switch (levelVal) {
		// Brick
		case (ObservationConstants.LVL_BRICK):
		case (ObservationConstants.LVL_BREAKABLE_BRICK):
			assertThing(rete, "brick", "brk", x, y, false);
			if (PolicyGenerator.debugMode_)
				System.out.print("b ");
			break;
		// Box
		case (ObservationConstants.LVL_UNBREAKABLE_BRICK):
			assertThing(rete, "box", "box", x, y, false);
			if (PolicyGenerator.debugMode_)
				System.out.print("B ");
			break;
		// Terrain
		case (ObservationConstants.LVL_BORDER_HILL):
		case (ObservationConstants.LVL_CANNON_MUZZLE):
		case (ObservationConstants.LVL_CANNON_TRUNK):
		case (ObservationConstants.LVL_FLOWER_POT):
		case (ObservationConstants.LVL_FLOWER_POT_OR_CANNON):
		case (ObservationConstants.LVL_BORDER_CANNOT_PASS_THROUGH):
			if (isEdge(levelObs, x, y))
				assertThing(rete, "edge", "ed", x, y, false);
			if (PolicyGenerator.debugMode_)
				System.out.print("# ");
			break;
		// Coin
		case (ObservationConstants.LVL_COIN):
			assertThing(rete, "coin", "coin", x, y, true);
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
	}

	/**
	 * Checks if an object is an edge by comparing surrounding tiles. A value on
	 * the very edge of the observation is not an edge.
	 * 
	 * @param levelObs
	 *            The level observation field.
	 * @param x
	 *            The x location of the object being checked.
	 * @param y
	 *            The y location of the object being checked.
	 * @return True if the object can be characterised as an edge.
	 */
	private boolean isEdge(byte[][] levelObs, byte x, byte y) {
		// If is blank above
		if (((y - 1) >= 0) && isBlank(levelObs[y - 1][x]))
			// And blank on at least one side.
			if ((((x - 1) >= 0) && isBlank(levelObs[y][x - 1]))
					|| (((x + 1) < levelObs[y].length) && isBlank(levelObs[y][x + 1])))
				return true;
		return false;
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
	 * Asserts any enemy objects present at a given point within the observation
	 * field and any relevant relations that object has.
	 * 
	 * @param rete
	 *            The Rete object to assert to.
	 * @param enemyObs
	 *            The enemy observation field; only detailing the relevant
	 *            enemies at a distance from Mario.
	 * @param relX
	 *            The x coord to examine.
	 * @param relY
	 *            The y coord to examine.
	 * @throws Exception
	 *             Should something go awry.
	 */
	private void assertEnemyObjects(Rete rete, float kind, float relX,
			float relY) throws Exception {
		float modX = environment_.getMarioFloatPos()[0] + relX;
		float modY = environment_.getMarioFloatPos()[1] + relY;

		switch ((int) kind) {
		// Enemies
		case (ObservationConstants.ENMY_BULLET_BILL):
			assertFloatThing(rete, "bulletBill", "bb", modX, modY, false);
			break;
		case (ObservationConstants.ENMY_ENEMY_FLOWER):
			assertFloatThing(rete, "pirahnaPlant", "pp", modX, modY, false);
			break;
		case (ObservationConstants.ENMY_GOOMBA_WINGED):
			assertFloatThing(rete, "flying", "gmba", modX, modY, false);
		case (ObservationConstants.ENMY_GOOMBA):
			assertFloatThing(rete, "goomba", "gmba", modX, modY, false);
			break;
		case (ObservationConstants.ENMY_GREEN_KOOPA_WINGED):
			assertFloatThing(rete, "flying", "grKpa", modX, modY, false);
		case (ObservationConstants.ENMY_GREEN_KOOPA):
			assertFloatThing(rete, "greenKoopa", "grKpa", modX, modY, false);
			break;
		case (ObservationConstants.ENMY_RED_KOOPA_WINGED):
			assertFloatThing(rete, "flying", "redKpa", modX, modY, false);
		case (ObservationConstants.ENMY_RED_KOOPA):
			assertFloatThing(rete, "redKoopa", "redKpa", modX, modY, false);
			break;
		case (ObservationConstants.ENMY_SPIKY_WINGED):
			assertFloatThing(rete, "flying", "spiky", modX, modY, false);
		case (ObservationConstants.ENMY_SPIKY):
			assertFloatThing(rete, "spiky", "spiky", modX, modY, false);
			break;
		case (ObservationConstants.ENMY_GENERAL_ENEMY):
			assertFloatThing(rete, "enemy", "enemy", modX, modY, false);
			break;
		// Collectables
		case (ObservationConstants.ENMY_FIRE_FLOWER):
			assertFloatThing(rete, "fireFlower", "ff", modX, modY, false);
			break;
		case (ObservationConstants.ENMY_MUSHROOM):
			assertFloatThing(rete, "mushroom", "mush", modX, modY, false);
			break;
		// Projectiles
		case (ObservationConstants.ENMY_FIREBALL):
			rete.assertString("(fireball fball)");
			break;
		case (ObservationConstants.ENMY_SHELL):
			// Special checks for shells
			if (environment_.isMarioCarrying()
					&& (Math.abs(modX - environment_.getReceptiveFieldWidth()) <= 1)
					&& (Math.abs(modY - environment_.getReceptiveFieldHeight()) <= 1)) {
				// Mario may be carrying this very shell
				// TODO Not asserting at the moment, but need to assert a
				// carrying pred while Mario cannot jump on this shell.
			} else {
				assertFloatThing(rete, "shell", "shell", modX, modY, true);
			}
			break;
		}
	}

	/**
	 * Asserts a thing into rete and makes distance assertions upon it.
	 * 
	 * @param rete
	 *            The Rete object to assert to.
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
	 */
	private void assertThing(Rete rete, String condition, String thingPrefix,
			int relX, int relY, boolean jumpInto) throws Exception {
		// Can use Mario's current location to fix level object names
		Point2D.Float p = RLMarioStateSpec.relativeToGlobal(relX, relY,
				environment_);
		// Special case - don't assert edges on far left of screen
		if (p.x <= LevelScene.cellSize / 2 && condition.equals("edge"))
			return;
		assertFloatThing(rete, condition, thingPrefix, p.x, p.y, jumpInto);
	}

	/**
	 * Assert a thing using float coordinates for maximum precision.
	 * 
	 * @param rete
	 *            The Rete object to assert to.
	 * @param condition
	 *            The condition being asserted.
	 * @param thingPrefix
	 *            The thing's prefix (to be appended by xy coords)
	 * @param x
	 *            The actual x location of the thing.
	 * @param y
	 *            The actual y location of the thing.
	 * @param jumpInto
	 *            If the object can be jumped into (from the side) rather than
	 *            onto (from above)
	 */
	private void assertFloatThing(Rete rete, String condition,
			String thingPrefix, float x, float y, boolean jumpInto)
			throws JessException, Exception {
		String thing = thingPrefix + "_" + x + "_" + y;
		rete.assertString("(" + condition + " " + thing + ")");

		// Distance & direction assertions.
		float[] marioPos = environment_.getMarioFloatPos();
		double dist = Point2D.distance(marioPos[0], marioPos[1], x, y);
		rete.assertString("(distance " + thing + " " + dist + ")");

		int localX = RLMarioStateSpec.globalToRelative(x, y, environment_).x;
		if (localX < environment_.getReceptiveFieldWidth() / 2)
			rete.assertString("(direction " + thing + " left)");
		else if (localX > environment_.getReceptiveFieldWidth() / 2)
			rete.assertString("(direction " + thing + " right)");

		// Height diff
		double heightDiff = marioPos[1] - y;
		rete.assertString("(heightDiff " + thing + " " + heightDiff + ")");

		// Can jump on/over assertions
		assertJumpOnOver(rete, thing, x, y, jumpInto);

		// Width assertion
		// TODO Change for pits
		rete.assertString("(width " + thing + " 1)");
	}

	/**
	 * Asserts if Mario can jump onto/over the object in question. Note that for
	 * some objects (like collectables) Mario need only jump into, not onto.
	 * 
	 * @param rete
	 *            The Rete object to assert to.
	 * @param thing
	 *            The thing being possibly asserted.
	 * @param x
	 *            The x coord of the thing.
	 * @param y
	 *            The y coord of the thing.
	 */
	private void assertJumpOnOver(Rete rete, String thing, float x, float y,
			boolean jumpInto) throws Exception {
		boolean[] jumpOnOver = ((RLMarioStateSpec) StateSpec.getInstance())
				.canJumpOnOver(x, y, environment_, jumpInto);
		if (jumpOnOver[0])
			rete.assertString("(canJumpOn " + thing + ")");
		if (jumpOnOver[1])
			rete.assertString("(canJumpOver " + thing + ")");
	}

	/**
	 * Converts a boolean array into an integer, where each boolean flag
	 * represents a bit.
	 * 
	 * @param array
	 *            The boolean array to be converted.
	 * @return The resulting integer.
	 */
	public static int booleanArrayToInt(boolean[] array) {
		int result = 0;
		for (int i = 0; i < Environment.numberOfKeys; i++) {
			if (array[i])
				result |= (int) Math.pow(2, i);
		}
		return result;
	}

	/**
	 * Converts an integer into a boolean array. where '1' bits are true and '0'
	 * bits are false.
	 * 
	 * @param integer
	 *            The integer to be converted.
	 * @return The resulting boolean array.
	 */
	public static boolean[] intToBooleanArray(int integer) {
		boolean[] result = new boolean[Environment.numberOfKeys];
		for (int i = 0; i < result.length; i++) {
			if ((integer & (int) Math.pow(2, i)) != 0)
				result[i] = true;
		}
		return result;
	}
}
