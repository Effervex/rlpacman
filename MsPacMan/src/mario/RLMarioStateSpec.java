package mario;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ch.idsia.benchmark.mario.engine.LevelScene;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;

import relationalFramework.GuidedRule;
import relationalFramework.Number;
import relationalFramework.Policy;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;
import relationalFramework.agentObservations.BackgroundKnowledge;

/**
 * The state specifications for the PacMan domain.
 * 
 * @author Sam Sarjant
 */
public class RLMarioStateSpec extends StateSpec {
	private static final float MAX_JUMP_HEIGHT = 66.4f;
	private static final float MAX_JUMP_DIST = 79;
	private static final float MAX_JUMP_DIST_RUNNING = 130;
	private int cellSize = ch.idsia.benchmark.mario.engine.LevelScene.cellSize;
	private byte[][] basicLevelObs_;
	private int direction_;
	private int directedCellSize_;
	private int negModifier_;
	private int oppDirection_;
	private float[] startEndDiffs_ = new float[2];
	private float startCurrentDiffX_;
	private float[] currentEndDiffs_ = new float[2];

	@Override
	protected Map<String, String> initialiseActionPreconditions() {
		Map<String, String> preconds = new HashMap<String, String>();
		// Movement entails getting to a point by moving in that direction at
		// speed and jumping if stuck until at the point (or jumping fails)
		preconds.put("move", "(thing ?X) (distance ?X ?Y) (direction ?X ?Z)");
		// Search entails being under a searchable block and moving towards it
		// (possibly jumping).
		preconds.put("search",
				"(brick ?X)  (heightDiff ?X ?Y&:(betweenRange ?Y 16 80))");
		// Jump onto entails jumping onto a specific thing, moving towards it if
		// necessary.
		preconds
				.put("jumpOnto",
						"(canJumpOn ?X) (thing ?X) (distance ?X ?Y&:(betweenRange ?Y 16 160))");
		// Jump over entails jumping over a specific thing, moving towards it if
		// necessary.
		preconds.put("jumpOver", "(canJumpOver ?X) (thing ?X) "
				+ "(distance ?X ?Y&:(betweenRange ?Y 16 160)) (width ?X ?Z)");

		return preconds;
	}

	@Override
	protected int initialiseActionsPerStep() {
		return -1;
	}

	@Override
	protected Collection<StringFact> initialiseActionTemplates() {
		Collection<StringFact> actions = new ArrayList<StringFact>();

		// Jump onto something
		String[] structure = new String[2];
		structure[0] = "thing";
		structure[1] = Number.Double.toString();
		actions.add(new StringFact("jumpOnto", structure));

		// Jump over something
		structure = new String[3];
		structure[0] = "thing";
		structure[1] = Number.Double.toString();
		structure[2] = Number.Integer.toString();
		actions.add(new StringFact("jumpOver", structure));

		// Move towards something
		structure = new String[3];
		structure[0] = "thing";
		structure[1] = Number.Double.toString();
		structure[2] = "directionType";
		actions.add(new StringFact("move", structure));

		// Search a brick
		structure = new String[2];
		structure[0] = "brick";
		structure[1] = Number.Double.toString();
		actions.add(new StringFact("search", structure));

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
	protected String initialiseGoalState() {
		// The goal is 0 units away.
		return "(distance goal 0)";
	}

	@Override
	protected Policy initialiseOptimalPolicy() {
		Policy goodPolicy = new Policy();

		// Defining a good policy (basic at the moment)
		// TODO Mario REALLY needs a pre-goal for constant specialisation. Maybe
		// just load one in from file obtained by a human playing (and an agent
		// watching)
		ArrayList<String> rules = new ArrayList<String>();

		rules.add("(distance goal ?Y) (direction goal ?Z) "
				+ "(flag goal) => (move goal ?Y ?Z)");

		for (String rule : rules)
			goodPolicy.addRule(new GuidedRule(rule), false, false);

		return goodPolicy;
	}

	@Override
	protected Collection<StringFact> initialisePredicateTemplates() {
		Collection<StringFact> predicates = new ArrayList<StringFact>();

		// Mario state
		String[] structure = new String[1];
		structure[0] = "marioPower";
		predicates.add(new StringFact("marioState", structure));

		// Coins (score)
		structure = new String[1];
		structure[0] = Number.Integer.toString();
		predicates.add(new StringFact("numCoins", structure));

		// Lives
		structure = new String[1];
		structure[0] = Number.Integer.toString();
		predicates.add(new StringFact("numLives", structure));

		// World
		structure = new String[1];
		structure[0] = Number.Integer.toString();
		predicates.add(new StringFact("world", structure));

		// Time
		structure = new String[1];
		structure[0] = Number.Integer.toString();
		predicates.add(new StringFact("time", structure));

		// Distance
		structure = new String[2];
		structure[0] = "thing";
		structure[1] = Number.Double.toString();
		predicates.add(new StringFact("distance", structure));

		// Height diff
		structure = new String[2];
		structure[0] = "thing";
		structure[1] = Number.Double.toString();
		predicates.add(new StringFact("heightDiff", structure));

		// Width (usually 1, except for pits)
		structure = new String[2];
		structure[0] = "thing";
		structure[1] = Number.Integer.toString();
		predicates.add(new StringFact("width", structure));

		// Can Jump On
		structure = new String[1];
		structure[0] = "thing";
		predicates.add(new StringFact("canJumpOn", structure));

		// Can Jump On
		structure = new String[1];
		structure[0] = "thing";
		predicates.add(new StringFact("canJumpOver", structure));

		// Direction
		structure = new String[2];
		structure[0] = "thing";
		structure[1] = "directionType";
		predicates.add(new StringFact("direction", structure));

		// Flying (enemy)
		structure = new String[1];
		structure[0] = "enemy";
		predicates.add(new StringFact("flying", structure));

		// Squashable (enemy)
		structure = new String[1];
		structure[0] = "enemy";
		predicates.add(new StringFact("squashable", structure));

		// Blastable (enemy)
		structure = new String[1];
		structure[0] = "enemy";
		predicates.add(new StringFact("blastable", structure));

		return predicates;
	}

	@Override
	protected Map<String, String> initialiseTypePredicateTemplates() {
		Map<String, String> types = new HashMap<String, String>();

		// Direction
		types.put("directionType", null);

		// Everything is a 'thing'
		types.put("thing", null);
		types.put("flag", "thing");
		types.put("pit", "thing");

		// Collectable items
		types.put("collectable", "thing");
		types.put("coin", "collectable");
		types.put("mushroom", "collectable");
		types.put("fireFlower", "collectable");

		// Solid objects
		types.put("solid", "thing");
		types.put("edge", "solid");
		types.put("brick", "solid");
		types.put("box", "brick");

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

		// Fired projectiles
		types.put("shell", "thing");
		types.put("fireball", null);

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
	 * @return A boolean array of size 2: Can Mario: jump on the thing, and jump
	 *         over the thing.
	 */
	public boolean[] canJumpOnOver(float x, float y,
			MarioEnvironment environment, boolean jumpInto) {
		boolean[] canJumpOnOver = new boolean[2];
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
				// If the y difference is less than the jump height (+ extra)
				// AND the thing is not under Mario, return true.
				if (diffs[1] <= MAX_JUMP_HEIGHT + extra && diffs[1] >= 0)
					canJumpOnOver[0] = true;
			} else {
				// Can always jump on the thing you're already on
				if (diffs[1] <= cellSize)
					canJumpOnOver[0] = true;
			}

			// Cannot jump over the thing in the same axis
			canJumpOnOver[1] = false;
		} else {

			// If not in the same axis, 'reduce' the height of the thing being
			// jumped into if jumping into something
			float jumpIntoBonus = (jumpInto) ? extra : 0;
			if (diffs[1] - jumpIntoBonus <= MAX_JUMP_HEIGHT)
				canJumpOnOver[0] = true;

			// Can only jump over things about a cell and a half lower than MAX
			// JUMP HEIGHT
			if (diffs[1] <= MAX_JUMP_HEIGHT - cellSize * 1.5)
				canJumpOnOver[1] = true;
		}

		// return false;
		return canJumpOnOver;
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
	 * @return A boolean array of keystroke actions to take at the time.
	 */
	public boolean[] applyAction(StringFact action, float[] startPos,
			float[] marioPos, byte[][] basicLevelObs) {
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
		startEndDiffs_[0] = Math.abs(x - startPos[0]);
		startEndDiffs_[1] = startPos[1] - y;
		startCurrentDiffX_ = Math.abs(marioPos[0] - startPos[0]);
		currentEndDiffs_[0] = Math.abs(x - marioPos[0]);
		currentEndDiffs_[1] = marioPos[1] - y;

		// Direction specific variables
		if (x > startPos[0]) {
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

		// Determine the actions
		if (action.getFactName().equals("move"))
			actionArray = move(startPos[0], startPos[1], marioPos[0],
					marioPos[1], x, y);
		else if (action.getFactName().equals("search"))
			actionArray = search(startPos[0], startPos[1], marioPos[0],
					marioPos[1], x, y);
		else if (action.getFactName().equals("jumpOnto")) {
			actionArray = preJumpOnto(startPos[0], startPos[1], marioPos[0],
					marioPos[1], x, y);
		} else if (action.getFactName().equals("jumpOver")) {
			int width = Integer.parseInt(action.getArguments()[2]);
			actionArray = preJumpOver(startPos[0], startPos[1], marioPos[0],
					marioPos[1], x, y, width);
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
	 * @return A boolean array of action required to move to the object
	 */
	private boolean[] move(float startX, float startY, float marioX,
			float marioY, float endX, float endY) {
		boolean[] actionArray = new boolean[Environment.numberOfKeys];

		actionArray[direction_] = true;
		actionArray[Environment.MARIO_KEY_SPEED] = true;

		// Check if obstacles are in the way
		int midY = basicLevelObs_.length / 2;
		int midX = basicLevelObs_[midY].length / 2;
		for (int i = (int) Math.ceil(MAX_JUMP_DIST / cellSize); i >= 1; i--) {
			// If there is an obstacle
			if (!ObservationConstants.isEmptyCell(basicLevelObs_[midY][midX
					+ (i * negModifier_)])) {
				// Find the highest point of the obstacle
				int yOffset = 0;
				while (yOffset < midY
						&& !ObservationConstants
								.isEmptyCell(basicLevelObs_[midY - yOffset - 1][midX
										+ (i * negModifier_)]))
					yOffset++;

				return jumpOnto(startX, startY, marioX, marioY, startX
						+ directedCellSize_ * i, startY - cellSize * yOffset);
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
	 * @return A boolean array of action required to search the brick.
	 */
	private boolean[] search(float startX, float startY, float currentX,
			float currentY, float endX, float endY) {
		boolean[] actionArray = new boolean[Environment.numberOfKeys];

		// Get within one cell beneath the object
		if (startEndDiffs_[0] > cellSize) {
			return move(startX, startY, currentX, currentY, endX, endY);
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
	 * @return A boolean array of action required to jump to the object
	 */
	private boolean[] preJumpOnto(float startX, float startY, float currentX,
			float currentY, float endX, float endY) {
		// If Mario is beyond jumping distance, move closer.
		float canJumpDist = currentEndDiffs_[0] + currentEndDiffs_[1];
		if (canJumpDist < MAX_JUMP_DIST_RUNNING) {
			return jumpOnto(startX, startY, currentX, currentY, endX, endY);
		} else
			return move(startX, startY, currentX, currentY, endX, endY);
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
		float lowerX = (endY > startY) ? endX - (endY - startY) : endX;
		float lowerY = (endY > startY) ? startY : endY;
		float releaseJumpPoint = (float) Point.distance(startX, startY, lowerX,
				lowerY) / 4;
		releaseJumpPoint = Math.max(releaseJumpPoint, 1);
		if (startCurrentDiffX_ <= releaseJumpPoint || currentEndDiffs_[1] > 0)
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
	 * @return A boolean array of action required to jump to the object
	 */
	private boolean[] preJumpOver(float startX, float startY, float currentX,
			float currentY, float endX, float endY, int width) {
		// If Mario is beyond jumping distance, move closer.
		float jumpPoint = (width + 1.5f) * cellSize;
		float canJumpDist = currentEndDiffs_[0] + currentEndDiffs_[1]
				+ jumpPoint;
		if (canJumpDist < MAX_JUMP_DIST_RUNNING) {
			return jumpOnto(startX, startY, currentX, currentY, endX
					+ (negModifier_ * jumpPoint), endY);
		} else
			return move(startX, startY, currentX, currentY, endX, endY);
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
