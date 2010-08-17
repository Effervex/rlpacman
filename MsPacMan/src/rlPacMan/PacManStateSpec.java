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
		preconds.put("toGhostCentre",
				"(ghostCentre ?X) (distanceGhostCentre player ?X ?Y)");
		preconds.put("fromGhostCentre",
				"(ghostCentre ?X) (distanceGhostCentre player ?X ?Y)");
		preconds.put("toJunction", "(junction ?X) (junctionSafety ?X ?Y)");

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

		structure = new ArrayList<Class>();
		structure.add(GhostCentre.class);
		structure.add(Double.class);
		actions.putCollection("toGhostCentre", structure);

		structure = new ArrayList<Class>();
		structure.add(GhostCentre.class);
		structure.add(Double.class);
		actions.putCollection("fromGhostCentre", structure);

		structure = new ArrayList<Class>();
		structure.add(Junction.class);
		structure.add(Integer.class);
		actions.putCollection("toJunction", structure);

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
		// return "(level 10) (not (dot ?X)) (not (powerDot ?X))";

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
					.add("(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 0 5)) "
							+ "(pacman ?Player) (aggressive ?Ghost) (ghost ?Ghost) "
							+ "=> (fromGhost ?Ghost ?Dist0)");
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
					.add("(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 0 5)) "
							+ "(pacman ?Player) (aggressive ?Ghost) (ghost ?Ghost) "
							+ "=> (fromGhost ?Ghost ?Dist0)");
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
					.add("(distanceFruit ?Player ?Fruit ?Dist0&:(betweenRange ?Dist0 0 20)) "
							+ "(pacman ?Player) (fruit ?Fruit) => (toFruit ?Fruit ?Dist0)");
			rules.add("(distanceDot ?Player ?Dot ?Dist0) (pacman ?Player) "
					+ "(dot ?Dot) => (toDot ?Dot ?Dist0)");
		}

		for (String rule : rules)
			goodPolicy.addRule(new GuidedRule(parseRule(rule)), false, false);

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
		structure.add(GhostCentre.class);
		structure.add(Double.class);
		predicates.putCollection("distanceGhostCentre", structure);

		structure = new ArrayList<Class>();
		structure.add(Player.class);
		structure.add(Fruit.class);
		structure.add(Integer.class);
		predicates.putCollection("distanceFruit", structure);

		structure = new ArrayList<Class>();
		structure.add(Junction.class);
		structure.add(Integer.class);
		predicates.putCollection("junctionSafety", structure);

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
		typeMap.put(GhostCentre.class, "ghostCentre");
		typeMap.put(Junction.class, "junction");

		return typeMap;
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
				|| (actionSplit[0].equals("toPowerDot"))
				|| (actionSplit[0].equals("fromPowerDot"))
				|| (actionSplit[0].equals("toGhostCentre"))
				|| (actionSplit[0].equals("fromGhostCentre"))) {
			// To Dot, to/from powerdot
			String[] coords = actionSplit[1].split("_");
			DistanceDir distanceGrid = state.getDistanceGrid()[Integer
					.parseInt(coords[1])][Integer.parseInt(coords[2])];
			if (distanceGrid == null)
				return null;
			byte path = distanceGrid.getDirection();
			if ((actionSplit[0].equals("fromPowerDot"))
					|| (actionSplit[0].equals("fromGhostCentre")))
				path *= -1;
			return new WeightedDirection(path, weight);

		} else if (actionSplit[0].equals("toFruit")) {
			// To fruit
			DistanceDir distanceGrid = state.getDistanceGrid()[state.getFruit().m_locX][state
					.getFruit().m_locY];
			if (distanceGrid == null)
				return null;
			return new WeightedDirection(distanceGrid.getDirection(), weight);

		} else if (actionSplit[0].equals("toJunction")) {
			String[] coords = actionSplit[1].split("_");
			// Modify the distances such that a higher safety has a lower
			// positive value
			double normalisedSafety = state.getSafestJunction();
			// If the best is 0, make it 1
			if (normalisedSafety == 0)
				normalisedSafety = 1;
			// If the best is negative, make it positive
			if (normalisedSafety < 0)
				normalisedSafety *= -1;

			// Junction value
			int juncVal = Integer.parseInt(actionSplit[2]);
			// If the junction has safety 0, there is neither weight towards,
			// nor from it.
			DistanceDir distanceGrid = state.getDistanceGrid()[Integer
					.parseInt(coords[1])][Integer.parseInt(coords[2])];
			if (distanceGrid == null)
				return null;
			if (juncVal == 0)
				return new WeightedDirection(distanceGrid.getDirection(), 0);

			normalisedSafety /= juncVal;

			weight = determineWeight(normalisedSafety);
			return new WeightedDirection(distanceGrid.getDirection(), weight);

		} else if ((actionSplit[0].equals("toGhost"))
				|| (actionSplit[0].equals("fromGhost"))) {
			int ghostIndex = 0;
			if (actionSplit[1].equals("blinky"))
				ghostIndex = Ghost.BLINKY;
			else if (actionSplit[1].equals("pinky"))
				ghostIndex = Ghost.PINKY;
			else if (actionSplit[1].equals("inky"))
				ghostIndex = Ghost.INKY;
			else if (actionSplit[1].equals("clyde"))
				ghostIndex = Ghost.CLYDE;

			DistanceDir distanceGrid = state.getDistanceGrid()[state
					.getGhosts()[ghostIndex].m_locX][state.getGhosts()[ghostIndex].m_locY];
			if (distanceGrid == null)
				return null;
			byte path = distanceGrid.getDirection();
			if (actionSplit[0].equals("toGhost"))
				return new WeightedDirection(path, weight);
			else
				return new WeightedDirection((byte) -path, weight);
		}
		return null;
	}

	/**
	 * Determines the weight of the action based on the proximity of the object.
	 * 
	 * @param distance
	 *            The distance to the object.
	 * @return A weight inversely proportional to the distance.
	 */
	private double determineWeight(double distance) {
		// Fixing the distance to a minimum of 1
		if (distance >= 0 && distance < 1)
			distance = 1;
		if (distance <= 0 && distance > -1)
			distance = -1;

		return 1.0 / distance;
	}
}
