package mario;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.idsia.benchmark.mario.engine.LevelScene;
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

		// More specific actions TODO Remove these
		preconds.put("jumpOntoEnemy",
				"(canJumpOn ?X) (enemy ?X) (distance ?X ?Y)");
		preconds.put("jumpOntoSolid",
				"(canJumpOn ?X) (solid ?X) (distance ?X ?Y)");
		preconds.put("jumpOntoCollectable",
				"(canJumpOn ?X) (collectable ?X) (distance ?X ?Y)");
		preconds.put("jumpOntoShell",
				"(canJumpOn ?X) (shell ?X) (distance ?X ?Y)");

		return preconds;
	}

	@Override
	protected int initialiseActionsPerStep() {
		return -1;
	}

	@Override
	protected Collection<StringFact> initialiseActionTemplates() {
		Collection<StringFact> actions = new ArrayList<StringFact>();

		// Actions have a type and a distance
		String[] structure = new String[2];
		structure[0] = "thing";
		structure[1] = Number.Integer.toString();
		actions.add(new StringFact("jumpOnto", structure));

		structure = new String[2];
		structure[0] = "enemy";
		structure[1] = Number.Integer.toString();
		actions.add(new StringFact("jumpOntoEnemy", structure));

		structure = new String[2];
		structure[0] = "solid";
		structure[1] = Number.Integer.toString();
		actions.add(new StringFact("jumpOntoSolid", structure));

		structure = new String[2];
		structure[0] = "collectable";
		structure[1] = Number.Integer.toString();
		actions.add(new StringFact("jumpOntoCollectable", structure));

		structure = new String[2];
		structure[0] = "shell";
		structure[1] = Number.Integer.toString();
		actions.add(new StringFact("jumpOntoShell", structure));

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
		predicates.add(new StringFact("coins", structure));

		// Lives
		structure = new String[1];
		structure[0] = Number.Integer.toString();
		predicates.add(new StringFact("lives", structure));

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
	 * @return A boolean array of keystroke actions to take at the time.
	 */
	public boolean[] applyAction(StringFact action) {
		boolean[] actionArray = null;
		// If no actions available, just move right.
		// This should never happen in the final version.
		if (action == null) {
			actionArray = new boolean[Environment.numberOfKeys];
			actionArray[Environment.MARIO_KEY_RIGHT] = true;
			actionArray[Environment.MARIO_KEY_SPEED] = true;
		} else {
			// Extract the coords of he thing to take action upon and apply the
			// actions.
			String[] argSplit = action.getArguments()[0].split("_");
			float x = Float.parseFloat(argSplit[1]);
			float y = Float.parseFloat(argSplit[2]);
			// TODO Jump onto and jump over are different values.
			actionArray = PhysicsApproximator.getInstance().jumpTo(x, y);
		}

		PhysicsApproximator.getInstance().applyAction(actionArray);
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
	public static Point2D.Float determineGlobalPos(int x, int y,
			MarioEnvironment environment) {
		// TODO Ensure this is working correctly - I haven't checked it. It's
		// probably still int'ing.
		float[] marioPos = environment.getMarioFloatPos();
		float globalX = (int) ((marioPos[0] + LevelScene.cellSize
				* (x - (environment.getReceptiveFieldWidth() / 2))) / LevelScene.cellSize);
		globalX = (globalX + 0.5f) * LevelScene.cellSize;
		float globalY = (int) ((marioPos[1] + LevelScene.cellSize
				* (y - (environment.getReceptiveFieldHeight() / 2))) / LevelScene.cellSize);
		globalY = (globalY + 0.5f) * LevelScene.cellSize;

		return new Point2D.Float(globalX, globalY);
	}
}
