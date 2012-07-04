package mario;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ch.idsia.benchmark.mario.engine.LevelScene;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;

import relationalFramework.NumberEnum;
import relationalFramework.RelationalPolicy;
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
		// TODO
		preconds.put("moveTo", "(canJumpOn ?A) (thing ?A) "
				+ "(distance ?A ?B&~:(<= -16 ?B 16)) (mario ?C)");
		// Moving away from a given object
		// preconds.put("moveFrom", "(canJumpOn ?A) (thing ?A) "
		// + "(distance ?A ?B&~:(<= -16 ?B 16))");
		// Search entails being under a searchable block and moving towards it
		// (possibly jumping).
		preconds.put("search", "(brick ?A) (distance ?A ?D) "
				+ "(heightDiff ?A ?B&:(<= 16 ?B 80))");
		// Jump onto entails jumping onto a specific thing, moving towards it if
		// necessary.
		preconds.put("jumpOnto", "(canJumpOn ?A) (thing ?A) (distance ?A ?B)");
		// Jump over entails jumping over a specific thing, moving towards it if
		// necessary.
		preconds.put("jumpOver", "(canJumpOver ?A) (thing ?A) "
				+ "(distance ?A ?B) (width ?A ?C)");

		// Pickup a shell
		preconds.put("pickup",
				"(canJumpOn ?A) (passive ?A) (shell ?A) (distance ?A ?B)");

		// Shoot a fireball at an enemy
		preconds.put("shootFireball", "(marioPower ?C&:(= ?C fire)) "
				+ "(distance ?A ?B) "
				+ "(heightDiff ?A ?H&:(<= -16 ?H 16)) (enemy ?A)");

		// Shoot a held shell at an enemy
		preconds.put("shootShell", "(carrying ?C) (shell ?C) (distance ?A ?B) "
				+ "(heightDiff ?A ?H&:(<= -16 ?H 16)) (enemy ?A)");

		return preconds;
	}

	@Override
	protected int initialiseActionsPerStep() {
		return -1;
	}

	@Override
	protected Collection<RelationalPredicate> initialiseActionTemplates() {
		Collection<RelationalPredicate> actions = new ArrayList<RelationalPredicate>();

		// Jump onto something
		String[] structure = new String[2];
		structure[0] = "thing";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("jumpOnto", structure, false));

		// Jump over something
		structure = new String[3];
		structure[0] = "thing";
		structure[1] = NumberEnum.Integer.toString();
		structure[2] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("jumpOver", structure, false));

		// Move towards something
		structure = new String[2];
		structure[0] = "thing";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("moveTo", structure, false));

		// Move away from something
		// structure = new String[2];
		// structure[0] = "thing";
		// structure[1] = NumberEnum.Integer.toString();
		// actions.add(new RelationalPredicate("moveFrom", structure));

		// Search a brick
		structure = new String[2];
		structure[0] = "brick";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("search", structure, false));

		// Pickup a shell
		structure = new String[2];
		structure[0] = "shell";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("pickup", structure, false));

		// Shoot an enemy with a fireball
		structure = new String[3];
		structure[0] = "enemy";
		structure[1] = NumberEnum.Integer.toString();
		structure[2] = "marioPower";
		actions.add(new RelationalPredicate("shootFireball", structure, false));

		// Shoot an enemy with a held shell
		structure = new String[3];
		structure[0] = "enemy";
		structure[1] = NumberEnum.Integer.toString();
		structure[2] = "shell";
		actions.add(new RelationalPredicate("shootShell", structure, false));

		return actions;
	}

	@Override
	protected Map<String, BackgroundKnowledge> initialiseBackgroundKnowledge() {
		Map<String, BackgroundKnowledge> bckKnowledge = new HashMap<String, BackgroundKnowledge>();

		// Squashable enemies
		bckKnowledge.put("squashableRule", new BackgroundKnowledge(
				"(enemy ?A) (not (spiky ?A)) (not (pirahnaPlant ?A)) "
						+ "=> (assert (squashable ?A))", false));

		// Blastable enemies
		bckKnowledge
				.put("blastableRule",
						new BackgroundKnowledge(
								"(enemy ?A) (not (spiky ?A)) => (assert (blastable ?A))",
								false));

		return bckKnowledge;
	}

	@Override
	protected String[] initialiseGoalState() {
		if (envParameter_ == null)
			envParameter_ = "Diff1";

		String[] result = { envParameter_,
				"(distance goal 0)" };
		return result;
	}

	@Override
	protected RelationalPolicy initialiseHandCodedPolicy() {
		RelationalPolicy goodPolicy = new RelationalPolicy();

		// Defining a good policy (basic at the moment)
		ArrayList<String> rules = new ArrayList<String>();

		// Jump pit
		rules.add("(pit ?A) (distance ?A ?B&:(> ?B 0))"
				+ " (width ?A ?C) => (jumpOver ?A ?B ?C)");
		// Shoot enemies
		rules.add("(blastable ?A) (distance ?A ?B&:(betweenRange ?B -32 100))"
				+ " (heightDiff ?A ?C&:(betweenRange ?C -10 32))"
				+ " (marioPower fire) => (shootFireball ?A ?B fire)");
		// Pickup shell
		rules.add("(passive ?A) (distance ?A ?B) (carrying ?A) (shell ?A)"
				+ "=> (pickup ?A ?B)");
		// Shoot shell
		rules.add("(enemy ?A) (distance ?A ?B&:(betweenRange ?B -32 64))"
				+ " => (shootShell ?A ?B fire)");
		// Stomp enemies
		rules.add("(canJumpOn ?A) (squashable ?A) (distance ?A ?B&:(betweenRange ?B -32 64))"
				+ " (heightDiff ?A ?C&:(< ?C 32)) => (jumpOnto ?A ?B)");
		// Collect powerups
		rules.add("(canJumpOn ?A) (powerup ?A) (distance ?A ?B) => (jumpOnto ?A ?B)");
		// Search bricks
		rules.add("(brick ?A) (heightDiff ?A ?B) => (search ?A ?B)");
		// To the goal
		rules.add("(flag ?A) (distance ?A ?B) => (moveTo ?A ?B)");

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
		predicates.add(new RelationalPredicate("distance", structure, false));

		// Height diff
		structure = new String[2];
		structure[0] = "thing";
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("heightDiff", structure, false));

		// Width (usually 1, except for pits)
		structure = new String[2];
		structure[0] = "thing";
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("width", structure, false));

		// Can Jump On
		structure = new String[1];
		structure[0] = "thing";
		predicates.add(new RelationalPredicate("canJumpOn", structure, false));

		// Can Jump On
		structure = new String[1];
		structure[0] = "thing";
		predicates.add(new RelationalPredicate("canJumpOver", structure, false));

		// Flying (enemy)
		structure = new String[1];
		structure[0] = "enemy";
		predicates.add(new RelationalPredicate("flying", structure, false));

		// Squashable (enemy)
		structure = new String[1];
		structure[0] = "enemy";
		predicates.add(new RelationalPredicate("squashable", structure, false));

		// Blastable (enemy)
		structure = new String[1];
		structure[0] = "enemy";
		predicates.add(new RelationalPredicate("blastable", structure, false));

		// Holding a shell
		structure = new String[1];
		structure[0] = "shell";
		predicates.add(new RelationalPredicate("carrying", structure, false));

		// Shell is passive (not moving)
		structure = new String[1];
		structure[0] = "shell";
		predicates.add(new RelationalPredicate("passive", structure, false));

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

	@Override
	protected Collection<String> initialiseConstantFacts() {
		Collection<String> constants = new ArrayList<String>();

		// Ever present goal
		constants.add("(flag goal)");
		constants.add("(canJumpOn goal)");
		constants.add("(heightDiff goal 0)");
		return constants;
	}
}
