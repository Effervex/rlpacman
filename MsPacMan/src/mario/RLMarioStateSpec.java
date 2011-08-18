package mario;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import cerrla.NumberEnum;
import ch.idsia.benchmark.mario.engine.LevelScene;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;

import relationalFramework.RelationalRule;
import relationalFramework.RelationalPolicy;
import relationalFramework.StateSpec;
import relationalFramework.RelationalPredicate;
import relationalFramework.agentObservations.BackgroundKnowledge;

/**
 * The state specifications for the PacMan domain.
 * 
 * @author Sam Sarjant
 */
public class RLMarioStateSpec extends StateSpec {
	public static final float MAX_JUMP_HEIGHT = 66.4f;
	private static final float MAX_JUMP_DIST = 79;
	private static final float MAX_JUMP_DIST_RUNNING = 130;
	private int cellSize = ch.idsia.benchmark.mario.engine.LevelScene.cellSize;
	private byte[][] basicLevelObs_;
	private int direction_;
	private int directedCellSize_;
	private int negModifier_;
	private int oppDirection_;
	private float[] startEndDiffs_ = new float[2];
	private float[] currentEndDiffs_ = new float[2];

	@Override
	protected Map<String, String> initialiseActionPreconditions() {
		Map<String, String> preconds = new HashMap<String, String>();
		// Movement entails getting to a point by moving in that direction at
		// speed and jumping if stuck until at the point (or jumping fails)
		preconds.put("move", "(canJumpOn ?X) (thing ?X) "
				+ "(distance ?X ?Y&:(outsideRange ?Y -16 16))");
		// Search entails being under a searchable block and moving towards it
		// (possibly jumping).
		preconds.put(
				"search",
				"(brick ?X) (not (marioPower small)) (distance ?X ?D&:(betweenRange ?D -32 32)) "
						+ "(heightDiff ?X ?Y&:(betweenRange ?Y 16 80))");
		// Jump onto entails jumping onto a specific thing, moving towards it if
		// necessary.
		preconds.put("jumpOnto",
				"(canJumpOn ?X) (thing ?X) (distance ?X ?Y&:(betweenRange ?Y -160 160))");
		// Jump over entails jumping over a specific thing, moving towards it if
		// necessary.
		preconds.put("jumpOver", "(canJumpOver ?X) (thing ?X) "
				+ "(distance ?X ?Y&:(betweenRange ?Y -160 160)) (width ?X ?Z)");

		// Pickup a shell
		preconds.put("pickup",
				"(canJumpOn ?X) (passive ?X) (shell ?X) (distance ?X ?Y)");

		// Shoot a fireball at an enemy
		preconds.put("shootFireball", "(marioPower ?Z&:(= ?Z fire)) "
				+ "(distance ?X ?Y&:(outsideRange ?Y -8 8)) "
				+ "(heightDiff ?X ?H&:(betweenRange ?H -16 16)) (enemy ?X)");

		// Shoot a held shell at an enemy
		preconds.put("shootShell", "(carrying ?Z) (shell ?Z) (distance ?X ?Y) "
				+ "(heightDiff ?X ?H&:(betweenRange ?H -16 16)) (enemy ?X)");

		return preconds;
	}

	@Override
	protected int initialiseActionsPerStep() {
		return 1;
	}

	@Override
	protected Collection<RelationalPredicate> initialiseActionTemplates() {
		Collection<RelationalPredicate> actions = new ArrayList<RelationalPredicate>();

		// Jump onto something
		String[] structure = new String[2];
		structure[0] = "thing";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("jumpOnto", structure));

		// Jump over something
		structure = new String[3];
		structure[0] = "thing";
		structure[1] = NumberEnum.Integer.toString();
		structure[2] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("jumpOver", structure));

		// Move towards something
		structure = new String[2];
		structure[0] = "thing";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("move", structure));

		// Search a brick
		structure = new String[2];
		structure[0] = "brick";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("search", structure));

		// Pickup a shell
		structure = new String[2];
		structure[0] = "shell";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("pickup", structure));

		// Shoot an enemy with a fireball
		structure = new String[3];
		structure[0] = "enemy";
		structure[1] = NumberEnum.Integer.toString();
		structure[2] = "marioPower";
		actions.add(new RelationalPredicate("shootFireball", structure));

		// Shoot an enemy with a held shell
		structure = new String[3];
		structure[0] = "enemy";
		structure[1] = NumberEnum.Integer.toString();
		structure[2] = "shell";
		actions.add(new RelationalPredicate("shootShell", structure));

		return actions;
	}

	@Override
	protected Map<String, BackgroundKnowledge> initialiseBackgroundKnowledge() {
		Map<String, BackgroundKnowledge> bckKnowledge = new HashMap<String, BackgroundKnowledge>();

		// TODO These rules should really be learned by the agent so remove
		// later on.
		// Squashable enemies
		bckKnowledge.put("squashableRule", new BackgroundKnowledge(
				"(enemy ?X) (not (spiky ?X)) " + "(not (pirahnaPlant ?X)) "
						+ "=> (assert (squashable ?X))", true));

		// Blastable enemies
		bckKnowledge.put("blastableRule", new BackgroundKnowledge(
				"(enemy ?X) (not (spiky ?X)) " + "=> (assert (blastable ?X))",
				true));

		return bckKnowledge;
	}

	@Override
	protected String[] initialiseGoalState() {
		// The goal is 0 units away.
		String[] result = { "goal", "(distance goal 0)" };
		return result;
	}

	@Override
	protected RelationalPolicy initialiseOptimalPolicy() {
		RelationalPolicy goodPolicy = new RelationalPolicy();

		// Defining a good policy (basic at the moment)
		ArrayList<String> rules = new ArrayList<String>();

		// Jump pit
		rules.add("(pit ?X) (distance ?X ?Y&:(> ?Y 0))"
				+ " (width ?X ?Z) => (jumpOver ?X ?Y ?Z)");
		// Shoot enemies
		rules.add("(blastable ?X) (distance ?X ?Y&:(betweenRange ?Y -32 100))"
				+ " (heightDiff ?X ?Z&:(betweenRange ?Z -10 32))"
				+ " (marioPower fire) => (shootFireball ?X ?Y fire)");
		// Pickup shell
		rules.add("(passive ?X) (distance ?X ?Y) (carrying ?X) (shell ?X)"
				+ "=> (pickup ?X ?Y)");
		// Shoot shell
		rules.add("(enemy ?X) (distance ?X ?Y&:(betweenRange ?Y -32 64))"
				+ " => (shootShell ?X ?Y fire)");
		// Stomp enemies
		rules.add("(canJumpOn ?X) (squashable ?X) (distance ?X ?Y&:(betweenRange ?Y -32 64))"
				+ " (heightDiff ?X ?Z&:(< ?Z 32)) => (jumpOnto ?X ?Y)");
		// Collect powerups
		rules.add("(canJumpOn ?X) (powerup ?X) (distance ?X ?Y) => (jumpOnto ?X ?Y)");
		// Search bricks
		rules.add("(brick ?X) (heightDiff ?X ?Y) => (search ?X ?Y)");
		// To the goal
		rules.add("(flag ?X) (distance ?X ?Y) => (move ?X ?Y)");

		for (String rule : rules)
			goodPolicy.addRule(new RelationalRule(rule), false, false);

		return goodPolicy;
	}

	@Override
	protected Collection<RelationalPredicate> initialisePredicateTemplates() {
		Collection<RelationalPredicate> predicates = new ArrayList<RelationalPredicate>();

		// Distance
		String[] structure = new String[2];
		structure[0] = "thing";
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("distance", structure));

		// Height diff
		structure = new String[2];
		structure[0] = "thing";
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("heightDiff", structure));

		// Width (usually 1, except for pits)
		structure = new String[2];
		structure[0] = "thing";
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("width", structure));

		// Can Jump On
		structure = new String[1];
		structure[0] = "thing";
		predicates.add(new RelationalPredicate("canJumpOn", structure));

		// Can Jump On
		structure = new String[1];
		structure[0] = "thing";
		predicates.add(new RelationalPredicate("canJumpOver", structure));

		// Flying (enemy)
		structure = new String[1];
		structure[0] = "enemy";
		predicates.add(new RelationalPredicate("flying", structure));

		// Squashable (enemy)
		structure = new String[1];
		structure[0] = "enemy";
		predicates.add(new RelationalPredicate("squashable", structure));

		// Blastable (enemy)
		structure = new String[1];
		structure[0] = "enemy";
		predicates.add(new RelationalPredicate("blastable", structure));

		// Holding a shell
		structure = new String[1];
		structure[0] = "shell";
		predicates.add(new RelationalPredicate("carrying", structure));

		// Shell is passive (not moving)
		structure = new String[1];
		structure[0] = "shell";
		predicates.add(new RelationalPredicate("passive", structure));

		return predicates;
	}

	@Override
	protected Map<String, String> initialiseTypePredicateTemplates() {
		Map<String, String> types = new HashMap<String, String>();

		// Everything is a 'thing'
		types.put("thing", null);
		types.put("flag", "thing");
		types.put("pit", "thing");

		// Items
		types.put("item", "thing");
		types.put("coin", "item");
		// Powerups
		types.put("powerup", "item");
		types.put("mushroom", "powerup");
		types.put("fireFlower", "powerup");

		// Solid objects (Brick)
		types.put("brick", "thing");

		// Mario and his state
		types.put("mario", null);
		types.put("marioPower", null);

		// Enemies
		types.put("enemy", "thing");
		types.put("goomba", "enemy");
		types.put("koopa", "enemy");
		types.put("redKoopa", "koopa");
		types.put("greenKoopa", "koopa");
		types.put("spiky", "enemy");
		types.put("pirahnaPlant", "enemy");
		types.put("bulletBill", "enemy");
		types.put("shell", "thing");

		return types;
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
		float vertLimit = (extraDist != 0) ? cellSize * 1.5f : 0;
		if (diffs[1] <= MAX_JUMP_HEIGHT - vertLimit) {
			// Horizontal limit
			float canJumpDist = diffs[0] + diffs[1] + extraDist;
			if (canJumpDist < MAX_JUMP_DIST_RUNNING)
				return true;
		}
		return false;
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
	 * @return A boolean array of keystroke actions to take at the time.
	 */
	public boolean[] applyAction(RelationalPredicate action, float[] startPos,
			float[] marioPos, byte[][] basicLevelObs, boolean[] prevAction,
			int marioDirection, boolean bigMario) {
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
		}

		return actionArray;
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
			directedCellSize_ = cellSize;
			negModifier_ = 1;
		} else {
			direction_ = Environment.MARIO_KEY_LEFT;
			oppDirection_ = Environment.MARIO_KEY_RIGHT;
			directedCellSize_ = -cellSize;
			negModifier_ = -1;
		}
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
		if (startEndDiffs_[0] < cellSize && startEndDiffs_[1] > 0)
			actionArray[Environment.MARIO_KEY_JUMP] = true;

		// Check if obstacles are in the way
		int midY = basicLevelObs_.length / 2;
		int midX = basicLevelObs_[midY].length / 2;
		int jumpPoint = (int) Math.ceil(Math.min(
				currentEndDiffs_[0] / cellSize, MAX_JUMP_DIST / cellSize));
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
						+ directedCellSize_ * x, startY - cellSize * yOffset);
			}
		}

		return actionArray;
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
		int movement = (int) (Math.max(1, startEndDiffs_[1] / (1 * cellSize)));
		if (startEndDiffs_[0] > cellSize * movement) {
			return move(startX, startY, currentX, currentY, endX, endY,
					bigMario);
		}

		// Otherwise, Mario is under it enough to jump and centre on it
		actionArray[Environment.MARIO_KEY_JUMP] = true;
		if (currentX < endX - cellSize / 2)
			actionArray[Environment.MARIO_KEY_RIGHT] = true;
		else if (currentX > endX + cellSize / 2)
			actionArray[Environment.MARIO_KEY_LEFT] = true;

		return actionArray;
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
		if (startEndDiffs_[0] < cellSize / 2
				&& Math.abs(startEndDiffs_[1]) < cellSize)
			return actionArray;

		// VERTICAL MOVEMENT

		// MINIMISING JUMP HEIGHT
		// Continue to jump only if the current x position is less than halfway
		// through the jump.

		// Modify the values for jumping if the y location is lower
		float releaseJumpPoint = startEndDiffs_[0] / 3;
		releaseJumpPoint = Math.max(releaseJumpPoint, 1);
		// Special case, if the object is immediately beside Mario
		if (startEndDiffs_[0] <= cellSize && startEndDiffs_[1] > 0)
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
		if (currentEndDiffs_[1] > 0 && currentEndDiffs_[0] <= cellSize) {
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
		float jumpPoint = (width + 1.5f) * cellSize;
		if (canJump(currentEndDiffs_, jumpPoint)) {
			if (negModifier_ > 0)
				endX = endX + jumpPoint;
			else
				endX = endX - 1.5f * cellSize;
			calculateDirectionVariables(startX, startY, currentX, currentY,
					endX, endY);
			return jumpOnto(startX, startY, currentX, currentY, endX, endY);
		} else
			return move(startX, startY, currentX, currentY, endX, endY,
					bigMario);
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
		}

		// If holding fire, release it and vice-versa
		actionArray[Environment.MARIO_KEY_SPEED] = !prevAction[Environment.MARIO_KEY_SPEED];
		return actionArray;
	}

	/**
	 * Determines the global position of the relative coords given.
	 * 
	 * @param x
	 *            The local x pos.
	 * @param y
	 *            The local y pos.
	 * @param environment
	 *            The Mario environment.
	 * @return The global x and y positions.
	 */
	public static Point2D.Float relativeToGlobal(int x, int y,
			MarioEnvironment environment) {
		float[] marioPos = environment.getMarioFloatPos();
		int c = LevelScene.cellSize;

		float globalX = ((int) (marioPos[0] / c) + x
				- environment.getReceptiveFieldWidth() / 2 + 0.5f)
				* c;
		float globalY = ((int) (marioPos[1] / c) + y
				- environment.getReceptiveFieldHeight() / 2 + 0.5f)
				* c;

		return new Point2D.Float(globalX, globalY);
	}

	/**
	 * Determines the relative position of the global coordinates given.
	 * 
	 * @param x
	 *            The global x position, between 0 and the level size.
	 * @param y
	 *            The global y position, between 0 and the level height.
	 * @param environment
	 *            The Mario environment.
	 * @return The relative x and y positions, between 0 and the receptive field
	 *         width.
	 */

	public static Point globalToRelative(float x, float y,
			MarioEnvironment environment) {
		float[] marioPos = environment.getMarioFloatPos();
		int c = LevelScene.cellSize;

		int localX = (int) (x / c + environment.getReceptiveFieldWidth() / 2 - marioPos[0]
				/ c);
		int localY = (int) (y / c + environment.getReceptiveFieldHeight() / 2 - marioPos[1]
				/ c);

		return new Point(localX, localY);
	}
}
