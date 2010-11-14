package mario;

import java.awt.Point;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jess.Rete;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;

import ch.idsia.benchmark.mario.engine.GlobalOptions;
import ch.idsia.benchmark.mario.engine.LevelScene;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;
import ch.idsia.tools.CmdLineOptions;

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
	private CmdLineOptions cmdLineOptions_;

	@Override
	public void env_cleanup() {
		environment_ = null;
	}

	@Override
	public String env_init() {
		environment_ = MarioEnvironment.getInstance();
		cmdLineOptions_ = new CmdLineOptions();
		cmdLineOptions_.setVisualization(!experimentMode_);
		// cmdLineOptions_.setEnemies("off");
		cmdLineOptions_.setLevelRandSeed(6);
		cmdLineOptions_.setLevelDifficulty(1);
		 cmdLineOptions_.setFPS(100);
		 cmdLineOptions_.setTimeLimit(50);
//		 GlobalOptions.isShowReceptiveField = true;

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

		return formObservations(rete_, 1, 0);
	}

	@Override
	public Reward_observation_terminal env_step(Action arg0) {
		// Applying the action (up down left right or nothing)
		RuleAction action = ((ActionChoice) ObjectObservations.getInstance().objectArray[0])
				.getFirstActionList();
		environment_.performAction(chooseLowAction(action));

		environment_.tick();
		float reward = environment_.getIntermediateReward();

		while (GlobalOptions.isGameplayStopped) {
			// Idle...
		}

		Observation obs = formObservations(rete_, 1, 0);
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
	 * Chooses a direction based off the chosen high action to follow.
	 * 
	 * @param ruleAction
	 *            The action the agent will take. May be multiple versions of
	 *            the same action with different arguments, based on what
	 *            matched the action conditions.
	 * @return The low action to use.
	 */
	private boolean[] chooseLowAction(RuleAction ruleAction) {
		if (ruleAction == null)
			return new boolean[Environment.numberOfButtons];

		// Find the action with the lowest distance; that will be the chosen
		// action
		List<StringFact> actions = ruleAction.getTriggerActions();
		// Sort the facts by distance
		Collections.sort(actions, new ActionComparator<StringFact>());
//		System.out.println(actions.get(0));
		return ((RLMarioStateSpec) StateSpec.getInstance()).applyAction(actions
				.get(0), environment_);
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
			rete.assertString("(heightDiff goal " + 0 + ")");
			// Other details
			rete.assertString("(coins "
					+ environment_.getEvaluationInfo().coinsGained + ")");
			rete.assertString("(time "
					+ environment_.getEvaluationInfo().timeLeft + ")");

			rete.run();

			// Adding the valid actions
			ObjectObservations.getInstance().validActions = StateSpec
					.getInstance().generateValidActions(rete_);
//			 rete.eval("(facts)");
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
			break;
		// Box
		case (ObservationConstants.LVL_UNBREAKABLE_BRICK):
			assertThing(rete, "box", "box", x, y, false);
			break;
		// Terrain
		case (ObservationConstants.LVL_BORDER_HILL):
		case (ObservationConstants.LVL_CANNON_MUZZLE):
		case (ObservationConstants.LVL_CANNON_TRUNK):
		case (ObservationConstants.LVL_FLOWER_POT):
		case (ObservationConstants.LVL_FLOWER_POT_OR_CANNON):
		case (ObservationConstants.LVL_BORDER_CANNOT_PASS_THROUGH):
			// if (isEdge(levelObs, x, y))
			// assertThing(rete, "edge", "ed", x, y, false);
			break;
		// Coin
		case (ObservationConstants.LVL_COIN):
			assertThing(rete, "coin", "coin", x, y, false);
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
			assertThing(rete, "bulletBill", "bb", x, y, false);
			break;
		case (ObservationConstants.ENMY_ENEMY_FLOWER):
			assertThing(rete, "pirahnaPlant", "pp", x, y, false);
			break;
		case (ObservationConstants.ENMY_GOOMBA_WINGED):
			assertThing(rete, "flying", "gmba", x, y, false);
		case (ObservationConstants.ENMY_GOOMBA):
			assertThing(rete, "goomba", "gmba", x, y, false);
			break;
		case (ObservationConstants.ENMY_GREEN_KOOPA_WINGED):
			assertThing(rete, "flying", "grKpa", x, y, false);
		case (ObservationConstants.ENMY_GREEN_KOOPA):
			assertThing(rete, "greenKoopa", "grKpa", x, y, false);
			break;
		case (ObservationConstants.ENMY_RED_KOOPA_WINGED):
			assertThing(rete, "flying", "redKpa", x, y, false);
		case (ObservationConstants.ENMY_RED_KOOPA):
			assertThing(rete, "redKoopa", "redKpa", x, y, false);
			break;
		case (ObservationConstants.ENMY_SPIKY_WINGED):
			assertThing(rete, "flying", "spiky", x, y, false);
		case (ObservationConstants.ENMY_SPIKY):
			assertThing(rete, "spiky", "spiky", x, y, false);
			break;
		case (ObservationConstants.ENMY_GENERAL_ENEMY):
			assertThing(rete, "enemy", "enemy", x, y, false);
			break;
		// Collectables
		case (ObservationConstants.ENMY_FIRE_FLOWER):
			assertThing(rete, "fireFlower", "ff", x, y, false);
			break;
		case (ObservationConstants.ENMY_MUSHROOM):
			assertThing(rete, "mushroom", "mush", x, y, false);
			break;
		// Projectiles
		case (ObservationConstants.ENMY_FIREBALL):
			rete.assertString("(fireball fball)");
			break;
		case (ObservationConstants.ENMY_SHELL):
			// Special checks for shells
			if (environment_.isMarioCarrying()
					&& (Math.abs(x
							- environment_.getMarioReceptiveFieldCenter()[0]) <= 1)
					&& (Math.abs(y
							- environment_.getMarioReceptiveFieldCenter()[1]) <= 1)) {
				// Mario may be carrying this very shell
				// TODO Not asserting at the moment, but need to assert a
				// carrying pred while Mario cannot jump on this shell.
			} else {
				assertThing(rete, "shell", "shell", x, y, true);
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
	 * @param x
	 *            The y loc of the thing.
	 * @param y
	 *            The x loc of the thing.
	 * @param jumpInto
	 *            If the object can be jumped into (from the side) rather than
	 *            onto (from above)
	 */
	private void assertThing(Rete rete, String condition, String thingPrefix,
			byte x, byte y, boolean jumpInto) throws Exception {
		// Can use Mario's current location to fix level object names
		Point p = RLMarioStateSpec.determineGlobalPos(x, y, environment_);
		String thing = thingPrefix + "_" + p.x + "_" + p.y;
		rete.assertString("(" + condition + " " + thing + ")");

		// Distance assertions.
		int[] originPoint = environment_.getMarioReceptiveFieldCenter();
		double dist = Math.hypot(x - originPoint[0], y - originPoint[1]);
		rete.assertString("(distance " + thing + " " + dist + ")");

		// Height diff
		double heightDiff = originPoint[1] - y;
		rete.assertString("(heightDiff " + thing + " " + heightDiff + ")");

		// Can jump on/over assertions
		assertJumpOnOver(rete, thing, x, y, jumpInto);
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
	private void assertJumpOnOver(Rete rete, String thing, int x, int y,
			boolean jumpInto) throws Exception {
		// TODO Check if Mario can jump on something from his current position
		// This assumes that he jumps from this point (or continues to jump)
		// This will require on ground checks, velocity checks and possibly
		// obstacle checks
		if (PhysicsApproximator.canJumpTo(x, y, environment_, jumpInto)) {
			rete.assertString("(canJumpOn " + thing + ")");
		}
	}

	private class ActionComparator<T> implements Comparator<StringFact> {

		@Override
		public int compare(StringFact o1, StringFact o2) {
			Double o1Val = Double.parseDouble(o1.getArguments()[o1
					.getArgTypes().length - 1]);
			Double o2Val = Double.parseDouble(o2.getArguments()[o2
					.getArgTypes().length - 1]);

			return o1Val.compareTo(o2Val);
		}

	}
}
