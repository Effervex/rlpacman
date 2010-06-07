package rlPacMan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import relationalFramework.GuidedRule;
import relationalFramework.MultiMap;
import relationalFramework.Policy;
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
		preconds.put("toDot", "(dot ?X) (distanceDot player ?X ?Y)");
		preconds.put("toPowerDot",
				"(powerDot ?X) (distancePowerDot player ?X ?Y)");
		preconds.put("fromPowerDot",
				"(powerDot ?X) (distancePowerDot player ?X ?Y)");
		preconds.put("toFruit", "(fruit ?X) (distanceFruit player ?X ?Y)");
		preconds.put("toGhost", "(ghost ?X) (distanceGhost player ?X ?Y)");
		preconds.put("fromGhost", "(ghost ?X) (distanceGhost player ?X ?Y)");

		return preconds;
	}

	@Override
	protected int initialiseActionsPerStep() {
		return -1;
	}

	@Override
	protected MultiMap<String, Class> initialiseActionTemplates() {
		MultiMap<String, Class> actions = new MultiMap<String, Class>();

		// Actions have a type and a distance
		List<Class> structure = new ArrayList<Class>();
		structure.add(Dot.class);
		structure.add(Integer.class);
		actions.putCollection("toDot", structure);

		structure = new ArrayList<Class>();
		structure.add(PowerDot.class);
		structure.add(Integer.class);
		actions.putCollection("toPowerDot", structure);

		structure = new ArrayList<Class>();
		structure.add(PowerDot.class);
		structure.add(Integer.class);
		actions.putCollection("fromPowerDot", structure);

		structure = new ArrayList<Class>();
		structure.add(Fruit.class);
		structure.add(Integer.class);
		actions.putCollection("toFruit", structure);

		structure = new ArrayList<Class>();
		structure.add(Ghost.class);
		structure.add(Integer.class);
		actions.putCollection("toGhost", structure);

		structure = new ArrayList<Class>();
		structure.add(Ghost.class);
		structure.add(Integer.class);
		actions.putCollection("fromGhost", structure);

		return actions;
	}

	@Override
	protected Map<String, String> initialiseBackgroundKnowledge() {
		Map<String, String> bckKnowledge = new HashMap<String, String>();

		return bckKnowledge;
	}

	@Override
	protected String initialiseGoalState(List<String> constants) {
		// constants.add("player");
		// Actual goal condition
		// return "(level 10) (not (dot ?X))";

		// Score maximisation
		return "(highScore ?X) (score ?Y &:(>= ?Y ?X))";
	}

	@Override
	protected Policy initialiseOptimalPolicy() {
		Policy goodPolicy = new Policy();

		// Defining a good policy
		ArrayList<String> rules = new ArrayList<String>();

		if (envParameter_ != null && envParameter_.equals("noDots")) {
			// Good policy for no dots play
			rules
					.add("(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 0 15)) "
							+ "(edible ?Ghost) (nonblinking ?Ghost) (pacman ?Player) "
							+ "(ghost ?Ghost) => (toGhost ?Ghost ?Dist0)");
			rules
					.add("(aggressive ?Ghost) "
							+ "(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 0 10)) "
							+ "(distancePowerDot ?Player ?PowerDot ?Dist1&:(betweenRange ?Dist1 0 10)) "
							+ "(pacman ?Player) (ghost ?Ghost) "
							+ "(powerDot ?PowerDot) => (toPowerDot ?PowerDot ?Dist1)");
			rules
					.add("(distancePowerDot ?Player ?PowerDot ?Dist0&:(betweenRange ?Dist0 0 2)) "
							+ "(distanceGhost ?Player ?Ghost ?Dist1&:(betweenRange ?Dist1 0 15)) "
							+ "(edible ?Ghost) (pacman ?Player) (powerDot ?PowerDot) "
							+ "=> (fromPowerDot ?PowerDot ?Dist0)");
			rules
					.add("(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 0 5)) "
							+ "(pacman ?Player) (ghost ?Ghost) => (fromGhost ?Ghost ?Dist0)");
			rules
					.add("(distanceFruit ?Player ?Fruit ?Dist0&:(betweenRange ?Dist0 0 20)) "
							+ "(pacman ?Player) (fruit ?Fruit) => (toFruit ?Fruit ?Dist0)");
			rules
					.add("(distancePowerDot ?Player ?PDot ?Dist0&:(betweenRange ?Dist0 2 99)) "
							+ "(pacman ?Player) (powerDot ?PDot) "
							+ "=> (toPowerDot ?PDot ?Dist0)");
		} else if (envParameter_ != null && envParameter_.equals("noPowerDots")) {
			// Good policy for no power dot play
			rules
					.add("(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 0 5)) "
							+ "(pacman ?Player) (ghost ?Ghost) => (fromGhost ?Ghost ?Dist0)");
			rules
					.add("(distanceFruit ?Player ?Fruit ?Dist0&:(betweenRange ?Dist0 0 20)) "
							+ "(pacman ?Player) (fruit ?Fruit) => (toFruit ?Fruit ?Dist0)");
			rules.add("(distanceDot ?Player ?Dot ?Dist0) (pacman ?Player) "
					+ "(dot ?Dot) => (toDot ?Dot ?Dist0)");
		} else {
			// Good policy for regular play
			rules
					.add("(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 0 15)) "
							+ "(edible ?Ghost) (nonblinking ?Ghost) (pacman ?Player) "
							+ "(ghost ?Ghost) => (toGhost ?Ghost ?Dist0)");
			rules
					.add("(distancePowerDot ?Player ?PowerDot ?Dist0&:(betweenRange ?Dist0 0 5)) "
							+ "(distanceGhost ?Player ?Ghost ?Dist1&:(betweenRange ?Dist1 0 15)) "
							+ "(edible ?Ghost) (pacman ?Player) (powerDot ?PowerDot) "
							+ "=> (fromPowerDot ?PowerDot ?Dist0)");
			rules
					.add("(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 0 5)) "
							+ "(pacman ?Player) (ghost ?Ghost) => (fromGhost ?Ghost ?Dist0)");
			rules
					.add("(aggressive ?Ghost) "
							+ "(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 0 10)) "
							+ "(distancePowerDot ?Player ?PowerDot ?Dist1&:(betweenRange ?Dist1 0 10)) "
							+ "(pacman ?Player) (ghost ?Ghost) "
							+ "(powerDot ?PowerDot) => (toPowerDot ?PowerDot ?Dist1)");
			rules
					.add("(distanceFruit ?Player ?Fruit ?Dist0&:(betweenRange ?Dist0 0 20)) "
							+ "(pacman ?Player) (fruit ?Fruit) => (toFruit ?Fruit ?Dist0)");
			rules.add("(distanceDot ?Player ?Dot ?Dist0) (pacman ?Player) "
					+ "(dot ?Dot) => (toDot ?Dot ?Dist0)");
		}

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

		// Aggressive
		structure = new ArrayList<Class>();
		structure.add(Ghost.class);
		predicates.putCollection("aggressive", structure);

		// Blinking
		structure = new ArrayList<Class>();
		structure.add(Ghost.class);
		predicates.putCollection("blinking", structure);

		// Nonblinking
		structure = new ArrayList<Class>();
		structure.add(Ghost.class);
		predicates.putCollection("nonblinking", structure);

		// Distance Metrics
		structure = new ArrayList<Class>();
		structure.add(Player.class);
		structure.add(Dot.class);
		structure.add(Integer.class);
		predicates.putCollection("distanceDot", structure);

		structure = new ArrayList<Class>();
		structure.add(Player.class);
		structure.add(PowerDot.class);
		structure.add(Integer.class);
		predicates.putCollection("distancePowerDot", structure);

		structure = new ArrayList<Class>();
		structure.add(Player.class);
		structure.add(Ghost.class);
		structure.add(Integer.class);
		predicates.putCollection("distanceGhost", structure);

		structure = new ArrayList<Class>();
		structure.add(Player.class);
		structure.add(Fruit.class);
		structure.add(Integer.class);
		predicates.putCollection("distanceFruit", structure);

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
	public WeightedDirection applyAction(String action, PacManState state) {
		String[] actionSplit = StateSpec.splitFact(action);
		double weight = determineWeight(Integer.parseInt(actionSplit[2]));

		// Move towards static points (dots, powerdots, junctions)
		if ((actionSplit[0].equals("toDot"))
				|| (actionSplit[0].equals("toPowerDot"))) {
			String[] coords = actionSplit[1].split("_");
			return new WeightedDirection((byte) followPath(
					Integer.parseInt(coords[1]), Integer.parseInt(coords[2]),
					state.getDistanceGrid()).ordinal(), weight);
		} else if (actionSplit[0].equals("toFruit")) {
			return new WeightedDirection((byte) followPath(
					state.getFruit().m_locX, state.getFruit().m_locY,
					state.getDistanceGrid()).ordinal(), weight);
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
				return new WeightedDirection(path, weight);
			else
				return new WeightedDirection((byte) -path, weight);
		}
	}

	/**
	 * Determines the weight of the action based on the proximity of the object.
	 * 
	 * @param distance
	 *            The distance to the object.
	 * @return A weight inversely proportional to the distance.
	 */
	private double determineWeight(double distance) {
		return (1.0 / Math.pow(distance + 0.01, 2));
	}
}
