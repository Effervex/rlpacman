package mario;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
	@Override
	protected Map<String, String> initialiseActionPreconditions() {
		Map<String, String> preconds = new HashMap<String, String>();
		// Basic preconditions for actions
		preconds.put("jumpOnto", "(canJumpOn ?X) (thing ?X) (distance ?X ?Y)");
		preconds
				.put("jumpOver", "(canJumpOver ?X) (thing ?X) (distance ?X ?Y)");
		preconds.put("move", "(thing ?X) (direction ?X ?Y) (distance ?X ?Z)");

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
		structure[1] = Number.Integer.toString();
		actions.add(new StringFact("jumpOnto", structure));

		// Jump over something
		structure = new String[2];
		structure[0] = "thing";
		structure[1] = Number.Integer.toString();
		actions.add(new StringFact("jumpOver", structure));

		// Move towards something
		structure = new String[3];
		structure[0] = "thing";
		structure[1] = "directionType";
		structure[2] = Number.Integer.toString();
		actions.add(new StringFact("move", structure));

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
	protected String initialiseGoalState(List<String> constants) {
		// The goal is 0 units away.
		return "(distance goal 0)";
	}

	@Override
	protected Policy initialiseOptimalPolicy() {
		Policy goodPolicy = new Policy();

		// Defining a good policy (basic at the moment)
		ArrayList<String> rules = new ArrayList<String>();

		rules.add("(distance goal ?Y) (flag goal) => (jumpOnto goal ?Y)");

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

		// Can Jump On
		structure = new String[1];
		structure[0] = "thing";
		predicates.add(new StringFact("canJumpOn", structure));

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
	 * Applies the action chosen by the agent to the environment - returning a
	 * boolean array of keystroke actions to take.
	 * 
	 * @param action
	 *            The action to take.
	 * @param startPos
	 *            The starting position of Mario.
	 * @param marioPos
	 *            The current position of Mario.
	 * @return A boolean array of keystroke actions to take at the time.
	 */
	public boolean[] applyAction(StringFact action, float[] startPos,
			float[] marioPos) {
		boolean[] actionArray = new boolean[Environment.numberOfKeys];
		// If no actions available, just move right.
		// This should never happen in the final version.
		if (action == null) {
			return actionArray;
		}

		// Move action
		String actionName = action.getFactName();
		if (actionName.equals("move")) {
			if (action.getArguments()[1].equals("right"))
				actionArray[Mario.KEY_RIGHT] = true;
			else if (action.getArguments()[1].equals("left"))
				actionArray[Mario.KEY_LEFT] = true;
			actionArray[Mario.KEY_SPEED] = true;
		} else {
			// Extract the coords of the thing to take action upon and apply the
			// actions.
			String[] argSplit = action.getArguments()[0].split("_");
			float x = Float.parseFloat(argSplit[1]);
			float y = Float.parseFloat(argSplit[2]);

			// Different behaviour for different actions
			if (actionName.equals("jumpOnto")) {
				actionArray = PhysicsApproximator.getInstance().jumpTo(
						startPos[0], startPos[1], marioPos[0], marioPos[1], x,
						y);
			} else if (actionName.equals("jumpOver")) {
				actionArray = PhysicsApproximator.getInstance().jumpOver(x, y);
			}
		}

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
		// TODO Ensure this is working correctly - I haven't checked it. It's
		// probably still int'ing.
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
