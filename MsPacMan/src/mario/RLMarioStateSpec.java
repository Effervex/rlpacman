package mario;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ch.idsia.benchmark.mario.engine.LevelScene;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;

import relationalFramework.BasicRelationalPolicy;
import relationalFramework.NumberEnum;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import relationalFramework.RelationalPredicate;
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
		// Movement entails getting to a point by moving in that direction at
		// speed and jumping if stuck until at the point (or jumping fails)
		preconds.put("move", "(canJumpOn ?X) (thing ?X) "
				+ "(distance ?X ?Y&~:(<= -16 ?Y 16))");
		// Search entails being under a searchable block and moving towards it
		// (possibly jumping).
		preconds.put(
				"search",
				"(brick ?X) (not (marioPower small)) (distance ?X ?D&:(<= -32 ?D 32)) "
						+ "(heightDiff ?X ?Y&:(<= 16 ?Y 80))");
		// Jump onto entails jumping onto a specific thing, moving towards it if
		// necessary.
		preconds.put("jumpOnto",
				"(canJumpOn ?X) (thing ?X) (distance ?X ?Y&:(<= -160 ?Y 160))");
		// Jump over entails jumping over a specific thing, moving towards it if
		// necessary.
		preconds.put("jumpOver", "(canJumpOver ?X) (thing ?X) "
				+ "(distance ?X ?Y&:(<= -160 ?Y 160)) (width ?X ?Z)");

		// Pickup a shell
		preconds.put("pickup",
				"(canJumpOn ?X) (passive ?X) (shell ?X) (distance ?X ?Y)");

		// Shoot a fireball at an enemy
		preconds.put("shootFireball", "(marioPower ?Z&:(= ?Z fire)) "
				+ "(distance ?X ?Y) "
				+ "(heightDiff ?X ?H&:(<= -16 ?H 16)) (enemy ?X)");

		// Shoot a held shell at an enemy
		preconds.put("shootShell", "(carrying ?Z) (shell ?Z) (distance ?X ?Y) "
				+ "(heightDiff ?X ?H&:(<= -16 ?H 16)) (enemy ?X)");

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

		// Squashable enemies
		bckKnowledge.put("squashableRule", new BackgroundKnowledge(
				"(enemy ?X) (not (spiky ?X)) " + "(not (pirahnaPlant ?X)) "
						+ "=> (assert (squashable ?X))", false));

		// Blastable enemies
		bckKnowledge.put("blastableRule", new BackgroundKnowledge(
				"(enemy ?X) (not (spiky ?X)) " + "=> (assert (blastable ?X))",
				false));

		return bckKnowledge;
	}

	@Override
	protected String[] initialiseGoalState() {
		// The goal is 0 units away.
		String[] result = { "goal", "(distance goal 0)" };
		return result;
	}

	@Override
	protected BasicRelationalPolicy initialiseHandCodedPolicy() {
		BasicRelationalPolicy goodPolicy = new BasicRelationalPolicy();

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
			goodPolicy.addRule(new RelationalRule(rule));

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
