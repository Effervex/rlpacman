package mario;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import jess.Fact;
import jess.JessException;
import jess.Rete;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;

import ch.idsia.benchmark.mario.engine.GlobalOptions;
import ch.idsia.benchmark.mario.engine.LevelScene;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;
import ch.idsia.tools.MarioAIOptions;

import relationalFramework.ActionChoice;
import relationalFramework.MultiMap;
import relationalFramework.ObjectObservations;
import relationalFramework.PolicyGenerator;
import relationalFramework.RuleAction;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

public class RLMarioEnvironment implements EnvironmentInterface {
	private Rete rete_;
	private MarioEnvironment environment_;
	private Collection<Fact> staticObjectFacts_;
	private boolean isMarioInAir_;
	/** Needed to check if the shells are passive. */
	private Collection<String> shellPositions_ = new HashSet<String>();
	private boolean experimentMode_ = false;
	private MarioAIOptions cmdLineOptions_;
	/** Records Mario's previous boolean array action. */
	private boolean[] prevBoolAction_;
	/** The agent's previous reward. */
	private double prevReward_;
	/** The agent's previous reward. */
	private double sumReward_;
	/** Mario's facing direction. */
	private int marioDirection_;
	private int marioCentreX_;
	private int marioCentreY_;
	private int levelDifficulty_;

	@Override
	public void env_cleanup() {
		environment_ = null;
	}

	@Override
	public String env_init() {
		environment_ = MarioEnvironment.getInstance();
		cmdLineOptions_ = new MarioAIOptions();
		cmdLineOptions_.setVisualization(!experimentMode_);
		staticObjectFacts_ = new HashSet<Fact>();
		// cmdLineOptions_.setEnemies("off");
		// cmdLineOptions_.setFPS(1);
		// cmdLineOptions_.setTimeLimit(50);
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
		levelDifficulty_ = 0;
		resetEnvironment();
		environment_.tick();

		while (GlobalOptions.isGameplayStopped) {
			// Idle...
			environment_.tick();
		}

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
		if (!environment_.isMarioOnGround())
			isMarioInAir_ = true;
		else {
			isMarioInAir_ = false;
			staticObjectFacts_.clear();
		}

		Observation obs = formObservations(rete_, 1);
		double thisStepReward = environment_.getIntermediateReward();
		double reward = thisStepReward - prevReward_;
		prevReward_ = thisStepReward;
		int terminal = isTerminal();
		sumReward_ += reward;
		if (terminal == 1)
			reward = environment_.getEvaluationInfo().computeWeightedFitness()
					- sumReward_;
		else if (terminal == 2) {
			// Increase the difficulty
			levelDifficulty_++;
			reward = environment_.getEvaluationInfo().computeWeightedFitness()
					- sumReward_;
			resetEnvironment();
			terminal = 0;
		}

		Reward_observation_terminal rot = new Reward_observation_terminal(
				reward, obs, terminal);
		return rot;
	}

	/**
	 * Resets the environment back to normal.
	 */
	public void resetEnvironment() {
		cmdLineOptions_.setLevelRandSeed(PolicyGenerator.random_.nextInt());
		cmdLineOptions_.setLevelDifficulty(levelDifficulty_);
		environment_.reset(cmdLineOptions_);
		if (!experimentMode_ && !GlobalOptions.isScale2x)
			GlobalOptions.changeScale2x();

		marioCentreX_ = environment_.getReceptiveFieldWidth() / 2;
		marioCentreY_ = environment_.getReceptiveFieldHeight() / 2;

		rete_ = StateSpec.getInstance().getRete();
		staticObjectFacts_.clear();
		
		isMarioInAir_ = false;
		prevBoolAction_ = new boolean[Environment.numberOfKeys];
		marioDirection_ = Environment.MARIO_KEY_RIGHT;
		prevReward_ = environment_.getIntermediateReward();
		sumReward_ = 0;
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
		boolean bigMario = environment_.getMarioMode() != 0;

		// TODO This vote thing more or less works, but it may ignore shooting,
		// which is really only governed by one action. The problem with Mario
		// is that the agent takes multiple actions at once. This could be
		// remedied by using votes for left/right jump/not jump shoot/don't
		// shoot, but this could 1) produce erratic behaviour, 2) weight towards
		// clusters of objects.

		// TODO Implement voting and strict action selection (take action
		// immediately, if rule is weighted high enough)

		// Run through the actions, storing identical action mappings under the
		// same weight. Storage is through bit shifted operations.
		int i = 0;
		int bestBitwise = -1;
		double bestWeight = -1;
		MultiMap<Integer, RuleAction> actionDirections = MultiMap
				.createListMultiMap();
		for (RuleAction ruleAction : ruleActions) {
			Collection<StringFact> actionStrings = ruleAction.getActions();
			double policyWeight = (1.0 * ruleActions.size() - i)
					/ ruleActions.size();
			policyWeight *= policyWeight;

			// Find the individual distance weighting and direction of each
			// action in the ArrayList
			for (StringFact action : actionStrings) {
				boolean[] actionArray = ((RLMarioStateSpec) StateSpec
						.getInstance()).applyAction(action, startPos,
						marioFloatPos, basicLevelObservation, prevBoolAction_,
						marioDirection_, bigMario);
				double distance = Math.abs(Double.parseDouble(action
						.getArguments()[1]));
				int bitwise = booleanArrayToInt(actionArray);
				actionDirections.putContains(bitwise, ruleAction);

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

			i++;
		}

		// If Mario is on the ground and cannot jump, allow him time to breathe
		prevBoolAction_ = intToBooleanArray(bestBitwise);
		if (environment_.isMarioOnGround() && !environment_.isMarioAbleToJump())
			prevBoolAction_[Environment.MARIO_KEY_JUMP] = false;

		// Output the 'firing' action.
		 SortedMap<Double, StringFact> bestRules = representativeRule
		 .get(bestBitwise);
		 StringFact bestAction = bestRules.get(bestRules.lastKey());
		 System.out.println("Action: " + bestAction);

		if (prevBoolAction_[Environment.MARIO_KEY_RIGHT]
				&& !prevBoolAction_[Environment.MARIO_KEY_LEFT])
			marioDirection_ = Environment.MARIO_KEY_RIGHT;
		else if (prevBoolAction_[Environment.MARIO_KEY_LEFT]
				&& !prevBoolAction_[Environment.MARIO_KEY_RIGHT])
			marioDirection_ = Environment.MARIO_KEY_LEFT;

		// Trigger the winning rules in the policy
		if (actionDirections.containsKey(bestBitwise)) {
			for (RuleAction ra : actionDirections.get(bestBitwise))
				ra.triggerRule();
		}

		return prevBoolAction_;
	}

	/**
	 * Checks if the episode has terminated.
	 * 
	 * @return 2 if level completed, 1 if Mario died, 0 otherwise.
	 */
	private int isTerminal() {
		if (environment_.isLevelFinished()) {
			if (environment_.getMarioStatus() == Environment.MARIO_STATUS_DEAD
					|| environment_.getMarioFloatPos()[1] > environment_
							.getLevelHeight()
							* LevelScene.cellSize) {
				ObjectObservations.getInstance().setNoPreGoal();
				return 1;
			}

			// If Mario didn't die, increment the difficulty
			return 2;
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
			// Mario state
			switch (environment_.getMarioMode()) {
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
			byte[][] levelObs = environment_
					.getLevelSceneObservationZ(levelZoom);
			byte[][] enemyObs = environment_.getEnemiesObservationZ(0);
			float[] enemyPos = environment_.getEnemiesFloatPos();

			// Assert the level objects
			for (byte y = 0; y < levelObs.length; y++) {
				for (byte x = 0; x < levelObs[y].length; x++) {
					// Level objects, like coins and solid objects
					assertLevelObjects(rete, levelObs, enemyObs, x, y);
				}
				if (PolicyGenerator.debugMode_)
					System.out.println();
			}

			// Reassert static objects
			if (isMarioInAir_)
				for (Fact fact : staticObjectFacts_)
					rete.assertFact(fact);

			// Assert the enemies
			Collection<String> currentShells = new HashSet<String>();
			boolean assertedEnemies = false;
			for (int e = 0; e < enemyPos.length; e++) {
				float enemyType = enemyPos[e++];
				float x = enemyPos[e++];
				float y = enemyPos[e];
				// Check it's not stuck in geometry
				if (!stuckInGeometry(x, y, levelObs)) {
					// Enemy objects, like fireFlower, mushroom, all enemies and
					// projectiles
					String isShell = assertEnemyObjects(rete, enemyType, x, y,
							levelObs);
					if (isShell != null)
						currentShells.add(isShell);
					assertedEnemies = true;
				}
			}
			shellPositions_ = currentShells;

			// Ever present goal
			rete.assertString("(flag goal))");
			rete.assertString("(distance goal " + marioCentreX_
					* LevelScene.cellSize + ")");
			rete.assertString("(canJumpOn goal)");
			rete.assertString("(heightDiff goal 0)");

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
	 * Checks if an enemy coordinate is stuck in geometry.
	 * 
	 * @param x
	 *            The enemy's x coord.
	 * @param y
	 *            The enemy's y coord.
	 * @param levelObs
	 *            The level observations. The enemy may be outside these bounds.
	 * @return True if the enemy is stuck in geometry, false otherwise.
	 */
	private boolean stuckInGeometry(float x, float y, byte[][] levelObs) {
		for (int i = -1; i <= 1; i++) {
			int relGridX = Math.round((x + i * LevelScene.cellSize / 2)
					/ LevelScene.cellSize)
					+ marioCentreX_;
			int relGridY = Math.round((y - LevelScene.cellSize / 2)
					/ LevelScene.cellSize)
					+ marioCentreY_;
			if (relGridY < 0 || relGridY >= levelObs.length || relGridX < 0
					|| relGridX >= levelObs[0].length)
				return false;
			if (isBlank(levelObs[relGridY][relGridX]))
				return false;
		}
		return true;
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
	private void assertLevelObjects(Rete rete, byte[][] levelObs,
			byte[][] enemyObs, byte x, byte y) throws Exception {
		byte levelVal = levelObs[y][x];
		switch (levelVal) {
		// Brick
		case (ObservationConstants.LVL_BRICK):
		case (ObservationConstants.LVL_BREAKABLE_BRICK):
		case (ObservationConstants.LVL_UNBREAKABLE_BRICK):
			// If searchable, assert brick. Otherwise it's just terrain
			if (canMarioFit(x, y, levelObs, true)) {
				assertThing(rete, "brick", "brk", x, y, false, levelObs, 1);

				if (PolicyGenerator.debugMode_)
					System.out.print("b ");
				break;
			}
			// Terrain
		case (ObservationConstants.LVL_BORDER_HILL):
		case (ObservationConstants.LVL_CANNON_MUZZLE):
		case (ObservationConstants.LVL_CANNON_TRUNK):
		case (ObservationConstants.LVL_FLOWER_POT):
		case (ObservationConstants.LVL_FLOWER_POT_OR_CANNON):
		case (ObservationConstants.LVL_BORDER_CANNOT_PASS_THROUGH):
			checkForPit(rete, levelObs, x, y);
			if (PolicyGenerator.debugMode_)
				System.out.print("# ");
			break;
		// Coin
		case (ObservationConstants.LVL_COIN):
			assertThing(rete, "coin", "coin", x, y, true, levelObs, 1);
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
			assertThing(rete, "fireFlower", "ff", x, y, true, levelObs, 1);
			break;
		case (ObservationConstants.ENMY_MUSHROOM):
			assertThing(rete, "mushroom", "mush", x, y, true, levelObs, 1);
			break;
		}
	}

	/**
	 * Checks if mario can fit under/over a thing.
	 * 
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
	private boolean canMarioFit(int x, int y, byte[][] levelObs, boolean under) {
		int space = 1;
		if (environment_.getMarioMode() != 0)
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
	 * @param levelObs
	 *            The current level observations.
	 * @param x
	 *            The x position to start from (known to be solid).
	 * @param y
	 *            The y position to start from (known to be solid).
	 */
	private void checkForPit(Rete rete, byte[][] levelObs, byte x, byte y)
			throws Exception {
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
					assertThing(rete, "pit", "pit", x + 1, y, false, levelObs,
							xMod - x - 1);
				}
			}
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
	 * Asserts any enemy objects present at a given point within the observation
	 * field and any relevant relations that object has. Also returns any shells
	 * present.
	 * 
	 * @param rete
	 *            The Rete object to assert to.
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
	private String assertEnemyObjects(Rete rete, float kind, float relX,
			float relY, byte[][] levelObs) throws Exception {
		int modX = (int) (environment_.getMarioFloatPos()[0] + relX);
		int modY = (int) (environment_.getMarioFloatPos()[1] + relY);
		int gridX = (int) (relX / LevelScene.cellSize) + marioCentreX_;
		int gridY = (int) (relY / LevelScene.cellSize) + marioCentreY_;

		switch ((int) kind) {
		// Enemies
		case (ObservationConstants.ENMY_BULLET_BILL):
			assertFloatThing(rete, "bulletBill", "bb", modX, modY, gridX,
					gridY, false, levelObs, 1, false);
			break;
		case (ObservationConstants.ENMY_ENEMY_FLOWER):
			assertFloatThing(rete, "pirahnaPlant", "pp", modX, modY, gridX,
					gridY, false, levelObs, 1, false);
			break;
		case (ObservationConstants.ENMY_GOOMBA_WINGED):
			assertFloatThing(rete, "flying", "gmba", modX, modY, gridX, gridY,
					false, levelObs, 1, false);
		case (ObservationConstants.ENMY_GOOMBA):
			assertFloatThing(rete, "goomba", "gmba", modX, modY, gridX, gridY,
					false, levelObs, 1, false);
			break;
		case (ObservationConstants.ENMY_GREEN_KOOPA_WINGED):
			assertFloatThing(rete, "flying", "grKpa", modX, modY, gridX, gridY,
					false, levelObs, 1, false);
		case (ObservationConstants.ENMY_GREEN_KOOPA):
			assertFloatThing(rete, "greenKoopa", "grKpa", modX, modY, gridX,
					gridY, false, levelObs, 1, false);
			break;
		case (ObservationConstants.ENMY_RED_KOOPA_WINGED):
			assertFloatThing(rete, "flying", "redKpa", modX, modY, gridX,
					gridY, false, levelObs, 1, false);
		case (ObservationConstants.ENMY_RED_KOOPA):
			assertFloatThing(rete, "redKoopa", "redKpa", modX, modY, gridX,
					gridY, false, levelObs, 1, false);
			break;
		case (ObservationConstants.ENMY_SPIKY_WINGED):
			assertFloatThing(rete, "flying", "spiky", modX, modY, gridX, gridY,
					false, levelObs, 1, false);
		case (ObservationConstants.ENMY_SPIKY):
			assertFloatThing(rete, "spiky", "spiky", modX, modY, gridX, gridY,
					false, levelObs, 1, false);
			break;
		case (ObservationConstants.ENMY_GENERAL_ENEMY):
			assertFloatThing(rete, "enemy", "enemy", modX, modY, gridX, gridY,
					false, levelObs, 1, false);
			break;
		// Items
		// case (ObservationConstants.ENMY_FIRE_FLOWER):
		// assertFloatThing(rete, "fireFlower", "ff", modX, modY, gridX,
		// gridY, false, levelObs, 1);
		// break;
		// case (ObservationConstants.ENMY_MUSHROOM):
		// assertFloatThing(rete, "mushroom", "mush", modX, modY, gridX,
		// gridY, false, levelObs, 1);
		// break;
		case (ObservationConstants.ENMY_SHELL):
			return assertFloatThing(rete, "shell", "shell", modX, modY, gridX,
					gridY, true, levelObs, 1, false);
		}
		return null;
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
	 * @param levelObs
	 *            The level observations.
	 * @param width
	 *            The width of the thing.
	 */
	private void assertThing(Rete rete, String condition, String thingPrefix,
			int relX, int relY, boolean jumpInto, byte[][] levelObs, int width)
			throws Exception {
		if (isMarioInAir_)
			return;

		// Can use Mario's current location to fix level object names
		Point2D.Float p = RLMarioStateSpec.relativeToGlobal(relX, relY,
				environment_);
		assertFloatThing(rete, condition, thingPrefix, (int) p.x, (int) p.y,
				relX, relY, jumpInto, levelObs, width, !condition
						.equals("mushroom"));
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
	private String assertFloatThing(Rete rete, String condition,
			String thingPrefix, int x, int y, int gridX, int gridY,
			boolean jumpInto, byte[][] levelObs, int width, boolean isStatic)
			throws JessException, Exception {
		String thing = thingPrefix + "_" + x + "_" + y;
		Fact fact = rete.assertString("(" + condition + " " + thing + ")");
		if (isStatic && !isMarioInAir_)
			staticObjectFacts_.add(fact);

		// Distance & direction assertions.
		float[] marioPos = environment_.getMarioFloatPos();
		int dist = (int) Math.round(Point2D.distance(marioPos[0], marioPos[1],
				x, y));

		// Special checks for shells
		if (thingPrefix.equals("shell")) {
			// Check for carrying
			if (environment_.isMarioCarrying() && dist < LevelScene.cellSize) {
				rete.assertString("(carrying " + thing + ")");
				return thing;
			}
			// Check for passive shells
			if (shellPositions_.contains(thing))
				rete.assertString("(passive " + thing + ")");
			else
				jumpInto = false;
		}

		if (x < marioPos[0])
			dist *= -1;
		fact = rete.assertString("(distance " + thing + " " + dist + ")");
		if (isStatic && !isMarioInAir_)
			staticObjectFacts_.add(fact);

		// Height diff
		int heightDiff = (int) Math.round(marioPos[1] - y);
		fact = rete.assertString("(heightDiff " + thing + " " + heightDiff
				+ ")");
		if (isStatic && !isMarioInAir_)
			staticObjectFacts_.add(fact);

		// Can jump on/over assertions
		assertJumpOnOver(rete, thing, x, y, gridX, gridY, levelObs, jumpInto);

		// Width assertion
		fact = rete.assertString("(width " + thing + " " + width + ")");
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
	private void assertJumpOnOver(Rete rete, String thing, float x, float y,
			int gridX, int gridY, byte[][] levelObs, boolean jumpInto)
			throws Exception {
		// Check Mario isn't blocked by geometry
		boolean[] jumpOnOver = ((RLMarioStateSpec) StateSpec.getInstance())
				.canJumpOnOver(x, y, environment_, jumpInto, isMarioInAir_);
		if ((jumpOnOver[0] || jumpOnOver[1])
				&& !jumpBlocked(gridX, gridY, levelObs, jumpInto)) {
			if (jumpOnOver[0])
				rete.assertString("(canJumpOn " + thing + ")");
			if (jumpOnOver[1])
				rete.assertString("(canJumpOver " + thing + ")");
		}
	}

	/**
	 * Roughly checks if Mario's jump will be blocked by geometry.
	 * 
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
	private boolean jumpBlocked(int gridX, int gridY, byte[][] levelObs,
			boolean jumpInto) {
		// If off screen, return false
		if (gridY < 0 || gridY >= levelObs.length || gridX < 0
				|| gridX >= levelObs[0].length)
			return false;

		int marioSize = (environment_.getMarioMode() == 0) ? 1 : 2;
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
			if (!jumpInto && !canMarioFit(gridX, gridY, levelObs, false))
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
