package rlPacMan;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import relationalFramework.GuidedRule;
import relationalFramework.MultiMap;
import relationalFramework.Policy;
import relationalFramework.State;
import relationalFramework.StateSpec;

/**
 * The state specifications for the PacMan domain.
 * 
 * @author Sam Sarjant
 */
public class PacManStateSpec extends StateSpec {
	@Override
	protected Map<String, String> initialiseActionPreconditions() {
		Map<String, String> preconds = new HashMap<String, String>();
		// Basic preconditions for actions
		preconds.put("toDot", "(dot ?X)");
		preconds.put("toPowerDot", "(powerDot ?X)");
		preconds.put("fromPowerDot", "(powerDot ?X)");
		preconds.put("toFruit", "(fruit ?X)");
		preconds.put("toGhost", "(ghost ?X)");
		preconds.put("fromGhost", "(ghost ?X)");
		preconds.put("toJunction", "(junction ?X)");

		return preconds;
	}

	@Override
	protected int initialiseActionsPerStep() {
		return 10;
	}

	@Override
	protected MultiMap<String, Class> initialiseActionTemplates() {
		MultiMap<String, Class> actions = new MultiMap<String, Class>();

		List<Class> structure = new ArrayList<Class>();
		structure.add(Dot.class);
		actions.putCollection("toDot", structure);

		structure = new ArrayList<Class>();
		structure.add(PowerDot.class);
		actions.putCollection("toPowerDot", structure);

		structure = new ArrayList<Class>();
		structure.add(PowerDot.class);
		actions.putCollection("fromPowerDot", structure);

		structure = new ArrayList<Class>();
		structure.add(Fruit.class);
		actions.putCollection("toFruit", structure);

		structure = new ArrayList<Class>();
		structure.add(Ghost.class);
		actions.putCollection("toGhost", structure);

		structure = new ArrayList<Class>();
		structure.add(Ghost.class);
		actions.putCollection("fromGhost", structure);

		structure = new ArrayList<Class>();
		structure.add(JunctionPoint.class);
		actions.putCollection("toJunction", structure);

		return actions;
	}

	@Override
	protected Map<String, String> initialiseBackgroundKnowledge() {
		Map<String, String> bckKnowledge = new HashMap<String, String>();

		// Type generalisation
		String[] type = { "pacman", "ghost", "dot", "powerDot", "junction",
				"fruit" };
		for (int i = 0; i < type.length; i++) {
			bckKnowledge.put("pacPoint" + i, "(" + type[i]
					+ " ?X) => (assert (pacPoint ?X))");
		}

		// Distance expanding
		bckKnowledge.put("nearMid",
				"(distance ?X ?Y near) => (assert (distance ?X ?Y mid))");
		bckKnowledge.put("midFar",
				"(distance ?X ?Y mid) => (assert (distance ?X ?Y far))");

		return bckKnowledge;
	}

	@Override
	protected String initialiseGoalState(List<String> constants) {
		constants.add("player");
		// Actual goal condition
		// return "(level 10) (not (exists (dot ?X)";

		// Score maximisation
		return "(highScore ?X) (score ?Y &:(>= ?Y ?X))";
	}

	@Override
	protected Policy initialiseOptimalPolicy() {
		Policy goodPolicy = new Policy();

		// Defining a good policy
		ArrayList<String> rules = new ArrayList<String>();
		rules.add("(distance ?Player ?Ghost near) (edible ?Ghost) "
				+ "(pacman ?Player) (ghost ?Ghost) => (toGhost ?Ghost)");
		rules.add("(distance ?Player ?PowerDot near) (edible ?Ghost) "
				+ "(pacman ?Player) (powerDot ?PowerDot) "
				+ "=> (fromPowerDot ?PowerDot)");
		rules.add("(distance ?Player ?Ghost near) (pacman ?Player) "
				+ "(ghost ?Ghost) => (fromGhost ?Ghost)");
		rules.add("(distance ?Player ?Fruit mid) (pacman ?Player) "
				+ "(fruit ?Fruit) => (toFruit ?Fruit)");
		rules.add("(closest ?Player ?Dot) (pacman ?Player) "
				+ "(dot ?Dot) => (toDot ?Dot)");

		for (String rule : rules)
			goodPolicy.addRule(new GuidedRule(parseRule(rule)), false);

		return goodPolicy;
	}

	@Override
	protected MultiMap<String, Class> initialisePredicateTemplates() {
		MultiMap<String, Class> predicates = new MultiMap<String, Class>();

		// Score
		List<Class> structure = new ArrayList<Class>();
		structure.add(Integer.class);
		predicates.putCollection("score", structure);

		// High Score
		structure = new ArrayList<Class>();
		structure.add(Integer.class);
		predicates.putCollection("highScore", structure);

		// Lives
		structure = new ArrayList<Class>();
		structure.add(Integer.class);
		predicates.putCollection("lives", structure);

		// Level
		structure = new ArrayList<Class>();
		structure.add(Integer.class);
		predicates.putCollection("level", structure);

		// Edible
		structure = new ArrayList<Class>();
		structure.add(Ghost.class);
		predicates.putCollection("edible", structure);

		// Blinking
		structure = new ArrayList<Class>();
		structure.add(Ghost.class);
		predicates.putCollection("blinking", structure);

		// Distance Metric
		structure = new ArrayList<Class>();
		structure.add(PacPoint.class);
		structure.add(PacPoint.class);
		structure.add(DistanceMetric.class);
		predicates.putCollection("distance", structure);

		// Closest Metric
		structure = new ArrayList<Class>();
		structure.add(PacPoint.class);
		structure.add(PacPoint.class);
		predicates.putCollection("closest", structure);

		return predicates;
	}

	@Override
	protected Map<Class, String> initialiseTypePredicateTemplates() {
		Map<Class, String> typeMap = new HashMap<Class, String>();

		typeMap.put(Player.class, "pacman");
		typeMap.put(Dot.class, "dot");
		typeMap.put(PowerDot.class, "powerDot");
		typeMap.put(Ghost.class, "ghost");
		typeMap.put(Fruit.class, "fruit");
		typeMap.put(JunctionPoint.class, "junction");
		typeMap.put(DistanceMetric.class, "distanceMetric");
		typeMap.put(PacPoint.class, "pacPoint");

		return typeMap;
	}

	/**
	 * Follows a path from a particular location back to point 0: where Pacman
	 * currently is. This procedure thereby finds the quickest path to a point
	 * by giving the initial direction that Pacman needs to take to get to the
	 * goal.
	 * 
	 * @param x
	 *            The x location of the point.
	 * @param y
	 *            The y location of the point.
	 * @param distanceGrid
	 *            The distance grid for the player, where a value of 0 is the
	 *            player position.
	 * @return The initial direction to take to follow the path.
	 */
	private PacManLowAction followPath(int x, int y, int[][] distanceGrid) {
		PacManLowAction prevLocation = PacManLowAction.NOTHING;
		// Repeat until the distance grid coords are equal to 0
		int width = distanceGrid.length;
		int height = distanceGrid[x].length;
		while (distanceGrid[x][y] != 0) {
			int currentVal = distanceGrid[x][y];
			// Check all directions
			if (distanceGrid[(x + 1) % width][y] == currentVal - 1) {
				x = (x + 1) % width;
				prevLocation = PacManLowAction.LEFT;
			} else if (distanceGrid[(x - 1 + width) % width][y] == currentVal - 1) {
				x = (x - 1 + width) % width;
				prevLocation = PacManLowAction.RIGHT;
			} else if (distanceGrid[x][(y + 1) % height] == currentVal - 1) {
				y = (y + 1) % height;
				prevLocation = PacManLowAction.UP;
			} else if (distanceGrid[x][(y - 1 + height) % height] == currentVal - 1) {
				y = (y - 1 + height) % height;
				prevLocation = PacManLowAction.DOWN;
			} else {
				return prevLocation;
			}
		}
		return prevLocation;
	}

	/**
	 * Applies an action and returns a direction to move towards/from.
	 * 
	 * @param action
	 *            The action to apply.
	 * @return A Byte direction to move towards/from.
	 */
	public Byte applyAction(String action, PacManState state) {
		String[] actionSplit = StateSpec.splitFact(action);
		// Move towards static points (dots, powerdots, junctions)
		if ((actionSplit[0].equals("toDot"))
				|| (actionSplit[0].equals("toPowerDot"))
				|| (actionSplit[0].equals("toJunction"))) {
			String[] coords = actionSplit[1].split("_");
			return (byte) followPath(Integer.parseInt(coords[1]),
					Integer.parseInt(coords[2]), state.getDistanceGrid())
					.ordinal();
		} else if (actionSplit[0].equals("toFruit")) {
			return (byte) followPath(state.getFruit().m_locX,
					state.getFruit().m_locY, state.getDistanceGrid()).ordinal();
		} else {
			int ghostIndex = 0;
			if (actionSplit[1].equals("blinky"))
				ghostIndex = Ghost.BLINKY;
			else if (actionSplit[1].equals("pinky"))
				ghostIndex = Ghost.PINKY;
			else if (actionSplit[1].equals("inky"))
				ghostIndex = Ghost.INKY;
			else if (actionSplit[1].equals("clyde"))
				ghostIndex = Ghost.CLYDE;

			byte path = (byte) followPath(state.getGhosts()[ghostIndex].m_locX,
					state.getGhosts()[ghostIndex].m_locY,
					state.getDistanceGrid()).ordinal();
			if (actionSplit[0].equals("toGhost"))
				return path;
			else
				return (byte) -path;
		}
	}
}
