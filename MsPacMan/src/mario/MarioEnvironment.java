package mario;

import java.util.ArrayList;

import jess.Rete;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;

import ch.idsia.agents.controllers.human.HumanKeyboardAgent;
import ch.idsia.benchmark.mario.engine.GlobalOptions;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.tools.CmdLineOptions;

import relationalFramework.ActionChoice;
import relationalFramework.ObjectObservations;
import relationalFramework.PolicyGenerator;
import relationalFramework.RuleAction;
import relationalFramework.StateSpec;

public class MarioEnvironment implements EnvironmentInterface {
	private static final int CANNON_MUZZLE = -82;
	private static final int CANNON_TRUNK = -80;
	private static final int COIN_ANIM = Sprite.KIND_COIN_ANIM; // 1
	private static final int BREAKABLE_BRICK = -20;
	private static final int UNBREAKABLE_BRICK = -22; // a rock with animated
	// question mark
	private static final int BRICK = -24; // a rock with animated question mark
	private static final int FLOWER_POT = -90;
	private static final int BORDER_CANNOT_PASS_THROUGH = -60;
	private static final int BORDER_HILL = -62;
	// TODO:TASK:!H! : resolve (document why) this: FLOWER_POT_OR_CANNON = -85;
	private static final int FLOWER_POT_OR_CANNON = -85;

	private Rete rete_;
	private Environment environment_;
	private boolean experimentMode_ = false;
	private CmdLineOptions cmdLineOptions_;

	@Override
	public void env_cleanup() {
		environment_ = null;
	}

	@Override
	public String env_init() {
		environment_ = ch.idsia.benchmark.mario.environments.MarioEnvironment
				.getInstance();
		cmdLineOptions_ = new CmdLineOptions();
		cmdLineOptions_.setVisualization(!experimentMode_);
		//cmdLineOptions_.setEnemies("off");
		cmdLineOptions_.setLevelRandSeed(51);
		cmdLineOptions_.setLevelDifficulty(1);
		// cmdLineOptions_.setFPS(2);
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
		}

		return formObservations(rete_, 0, 0);
	}

	@Override
	public Reward_observation_terminal env_step(Action arg0) {
		// Applying the action (up down left right or nothing)
		ActionChoice actions = (ActionChoice) ObjectObservations.getInstance().objectArray[0];
		environment_.performAction(chooseLowAction(actions.getActions()));

		environment_.tick();
		float reward = environment_.getIntermediateReward();

		while (GlobalOptions.isGameplayStopped) {
			// Idle...
		}

		Observation obs = formObservations(rete_, 0, 0);
		Reward_observation_terminal rot = new Reward_observation_terminal(
				reward, obs, isTerminal());
		return rot;
	}

	/**
	 * Resets the environment back to normal.
	 */
	public void resetEnvironment() {
		environment_.reset(cmdLineOptions_);
		if (!experimentMode_ && !GlobalOptions.isScale2x)
			GlobalOptions.changeScale2x();

		rete_ = StateSpec.getInstance().getRete();
	}

	/**
	 * Chooses a direction based off the chosen high actions to follow.
	 * 
	 * @param actions
	 *            The prioritised actions the agent will take, joined together
	 *            into a weighted singular direction to take.
	 * @return The low action to use.
	 */
	private boolean[] chooseLowAction(ArrayList<RuleAction> actions) {
		boolean[] actionArray = new boolean[Environment.numberOfButtons];
		// TODO Taking actions.
		actionArray[Mario.KEY_RIGHT] = true;
		actionArray[Mario.KEY_SPEED] = true;
		if (environment_.isMarioAbleToJump() || !environment_.isMarioOnGround())
			actionArray[Mario.KEY_JUMP] = true;
		return actionArray;
	}

	/**
	 * Checks if the episode has terminated.
	 * 
	 * @return 1 if Game over or level complete, false otherwise.
	 */
	private int isTerminal() {
		if (environment_.isLevelFinished()) {
			if (environment_.getMarioStatus() == Environment.MARIO_STATUS_DEAD)
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
	 * @param enemyZoom
	 *            The zoom level for the enemy observations.
	 * @return An observation of the current state
	 */
	private Observation formObservations(Rete rete, int levelZoom, int enemyZoom) {
		try {
			rete.reset();

			// Player
			rete.assertString("(mario player))");
			// Run through the level observations
			byte[][] levelObs = environment_
					.getLevelSceneObservationZ(levelZoom);
			byte[][] enemyObs = environment_.getEnemiesObservationZ(enemyZoom);
			for (byte y = 0; y < levelObs.length; y++) {
				for (byte x = 0; x < levelObs[y].length; x++) {
					// TODO Add Pits
					// Level objects, like coins and solid objects
					assertLevelObjects(rete, levelObs, x, y);

					// Enemy objects, like fireFlower, mushroom, all enemies and
					// projectiles
					assertEnemyObjects(rete, enemyObs, x, y);
				}
			}

			// Ever present goal
			rete.assertString("(flag goal))");
			rete.assertString("(distance goal "
					+ environment_.getEvaluationInfo().levelLength + ")");
			rete.assertString("(heightDiff goal "
					+ 0 + ")");
			// Other details
			rete.assertString("(coins "
					+ environment_.getEvaluationInfo().coinsGained + ")");
			rete.assertString("(time "
					+ environment_.getEvaluationInfo().timeLeft + ")");

			rete.run();

			// Adding the valid actions
			ObjectObservations.getInstance().validActions = StateSpec
					.getInstance().generateValidActions(rete_);

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
			assertThing(rete, "brick", "brk", x, y, levelObs.length / 2);
			break;
		// Box
		case (ObservationConstants.LVL_UNBREAKABLE_BRICK):
			assertThing(rete, "box", "box", x, y, levelObs.length / 2);
			break;
		// Terrain
		case (ObservationConstants.LVL_BORDER_HILL):
		case (ObservationConstants.LVL_CANNON_MUZZLE):
		case (ObservationConstants.LVL_CANNON_TRUNK):
		case (ObservationConstants.LVL_FLOWER_POT):
		case (ObservationConstants.LVL_FLOWER_POT_OR_CANNON):
		case (ObservationConstants.LVL_BORDER_CANNOT_PASS_THROUGH):
			if (isEdge(levelObs, x, y))
				assertThing(rete, "edge", "ed", x, y, levelObs.length / 2);
			break;
		// Coin
		case (ObservationConstants.LVL_COIN):
			assertThing(rete, "coin", "coin", x, y, levelObs.length / 2);
			break;
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
		if ((val == 0) || (val == ObservationConstants.LVL_COIN)
				|| (val == ObservationConstants.LVL_BORDER_HILL))
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
	 * @param x
	 *            The x coord to examine.
	 * @param y
	 *            The y coord to examine.
	 * @throws Exception
	 *             Should something go awry.
	 */
	private void assertEnemyObjects(Rete rete, byte[][] enemyObs, byte x, byte y)
			throws Exception {
		byte enemyVal = enemyObs[y][x];
		switch (enemyVal) {
		// Enemies
		case (ObservationConstants.ENMY_BULLET_BILL):
			assertThing(rete, "bulletBill", "bb", x, y, enemyObs.length / 2);
			break;
		case (ObservationConstants.ENMY_ENEMY_FLOWER):
			assertThing(rete, "pirahnaPlant", "pp", x, y, enemyObs.length / 2);
			break;
		case (ObservationConstants.ENMY_GOOMBA_WINGED):
			assertThing(rete, "flying", "gmba", x, y, enemyObs.length / 2);
		case (ObservationConstants.ENMY_GOOMBA):
			assertThing(rete, "goomba", "gmba", x, y, enemyObs.length / 2);
			break;
		case (ObservationConstants.ENMY_GREEN_KOOPA_WINGED):
			assertThing(rete, "flying", "grKpa", x, y, enemyObs.length / 2);
		case (ObservationConstants.ENMY_GREEN_KOOPA):
			assertThing(rete, "greenKoopa", "grKpa", x, y, enemyObs.length / 2);
			break;
		case (ObservationConstants.ENMY_RED_KOOPA_WINGED):
			assertThing(rete, "flying", "redKpa", x, y, enemyObs.length / 2);
		case (ObservationConstants.ENMY_RED_KOOPA):
			assertThing(rete, "redKoopa", "redKpa", x, y, enemyObs.length / 2);
			break;
		case (ObservationConstants.ENMY_SPIKY_WINGED):
			assertThing(rete, "flying", "spiky", x, y, enemyObs.length / 2);
		case (ObservationConstants.ENMY_SPIKY):
			assertThing(rete, "spiky", "spiky", x, y, enemyObs.length / 2);
			break;
		case (ObservationConstants.ENMY_GENERAL_ENEMY):
			assertThing(rete, "enemy", "enemy", x, y, enemyObs.length / 2);
			break;
		// Collectables
		case (ObservationConstants.ENMY_FIRE_FLOWER):
			assertThing(rete, "fireFlower", "ff", x, y, enemyObs.length / 2);
			break;
		case (ObservationConstants.ENMY_MUSHROOM):
			assertThing(rete, "mushroom", "mush", x, y, enemyObs.length / 2);
			break;
		// Projectiles
		case (ObservationConstants.ENMY_FIREBALL):
			assertThing(rete, "fireball", "fball", x, y, enemyObs.length / 2);
			break;
		case (ObservationConstants.ENMY_SHELL):
			assertThing(rete, "shell", "shell", x, y, enemyObs.length / 2);
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
	 * @param x
	 *            The y loc of the thing.
	 * @param y
	 *            The x loc of the thing.
	 * @param halfObsSize
	 *            Half of the observation size for normalising values to Mario's
	 *            position.
	 */
	private void assertThing(Rete rete, String condition, String thingPrefix,
			byte x, byte y, int halfObsSize) throws Exception {
		String thing = thingPrefix + "_" + x + "_" + y;
		rete.assertString("(" + condition + " " + thing + ")");

		// Distance assertions.
		double dist = Math.hypot(x - halfObsSize, y - halfObsSize);
		rete.assertString("(distance " + thing + " " + dist + ")");

		// Height diff
		double heightDiff = halfObsSize - y;
		rete.assertString("(heightDiff " + thing + " " + heightDiff + ")");
	}
}
