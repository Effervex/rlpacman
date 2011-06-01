package rlPacMan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
	protected Collection<StringFact> initialiseActionTemplates() {
		Collection<StringFact> actions = new ArrayList<StringFact>();

		// Actions have a type and a distance
		String[] structure = new String[2];
		structure[0] = "dot";
		structure[1] = Number.Integer.toString();
		actions.add(new StringFact("toDot", structure));

		structure = new String[2];
		structure[0] = "powerDot";
		structure[1] = Number.Integer.toString();
		actions.add(new StringFact("toPowerDot", structure));

		structure = new String[2];
		structure[0] = "powerDot";
		structure[1] = Number.Integer.toString();
		actions.add(new StringFact("fromPowerDot", structure));

		structure = new String[2];
		structure[0] = "fruit";
		structure[1] = Number.Integer.toString();
		actions.add(new StringFact("toFruit", structure));

		structure = new String[2];
		structure[0] = "ghost";
		structure[1] = Number.Integer.toString();
		actions.add(new StringFact("toGhost", structure));

		structure = new String[2];
		structure[0] = "ghost";
		structure[1] = Number.Integer.toString();
		actions.add(new StringFact("fromGhost", structure));

		structure = new String[2];
		structure[0] = "ghostCentre";
		structure[1] = Number.Double.toString();
		actions.add(new StringFact("toGhostCentre", structure));

		structure = new String[2];
		structure[0] = "ghostCentre";
		structure[1] = Number.Double.toString();
		actions.add(new StringFact("fromGhostCentre", structure));

		structure = new String[2];
		structure[0] = "junction";
		structure[1] = Number.Integer.toString();
		actions.add(new StringFact("toJunction", structure));
		
		// TODO Add ghost density

		return actions;
	}

	@Override
	protected Map<String, BackgroundKnowledge> initialiseBackgroundKnowledge() {
		Map<String, BackgroundKnowledge> bckKnowledge = new HashMap<String, BackgroundKnowledge>();

		bckKnowledge.put("blinkingRule", new BackgroundKnowledge(
				"(blinking ?X) => (edible ?X)", false));

		return bckKnowledge;
	}

	@Override
	protected String[] initialiseGoalState() {
		if (envParameter_ == null)
			envParameter_ = "10000";

		String[] result = new String[2];
		if (envParameter_.equals("lvl10")) {
			// Actual goal condition
			result[0] = "10levels";
			result[1] = "(level 11)";
			return result;
		}
		
		if (envParameter_.equals("10000")) {
			// Score maximisation
			result[0] = "10000points";
			result[1] = "(score ?Y &:(>= ?Y 10000))";
			return result;
		}
		
		if (envParameter_.equals("levelMax")
				|| envParameter_.equals("oneLevel")) {
			// Score maximisation over a single level
			result[0] = "1level";
			result[1] = "(level 2)";
			return result;
		}

		// Score maximisation by default
		return null;
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
							+ "(pacman ?Player) (not (edible ?Ghost)) (ghost ?Ghost) "
							+ "=> (fromGhost ?Ghost ?Dist0)");
			rules
					.add("(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 0 15)) "
							+ "(edible ?Ghost) (not (blinking ?Ghost)) (pacman ?Player) "
							+ "(ghost ?Ghost) => (toGhost ?Ghost ?Dist0)");
			rules
					.add("(not (edible ?Ghost)) "
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
							+ "(pacman ?Player) (not (edible ?Ghost)) (ghost ?Ghost) "
							+ "=> (fromGhost ?Ghost ?Dist0)");
			rules
					.add("(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 0 10)) "
							+ "(pacman ?Player) (not (edible ?Ghost)) (ghost ?Ghost) "
							+ "=> (fromGhost ?Ghost ?Dist0)");
			rules
					.add("(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 0 15)) "
							+ "(edible ?Ghost) (not (blinking ?Ghost)) (pacman ?Player) "
							+ "(ghost ?Ghost) => (toGhost ?Ghost ?Dist0)");
			rules
					.add("(not (edible ?Ghost)) "
							+ "(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 0 10)) "
							+ "(distancePowerDot ?Player ?PowerDot ?Dist1&:(betweenRange ?Dist1 0 10)) "
							+ "(pacman ?Player) (ghost ?Ghost) "
							+ "(powerDot ?PowerDot) => (toPowerDot ?PowerDot ?Dist1)");
			rules
					.add("(distancePowerDot ?Player ?PowerDot ?Dist0&:(betweenRange ?Dist0 0 3)) "
							+ "(distanceGhost ?Player ?Ghost ?Dist1&:(betweenRange ?Dist1 0 15)) "
							+ "(edible ?Ghost) (pacman ?Player) (powerDot ?PowerDot) "
							+ "=> (fromPowerDot ?PowerDot ?Dist0)");
			rules
					.add("(distanceFruit ?Player ?Fruit ?Dist0&:(betweenRange ?Dist0 0 20)) "
							+ "(pacman ?Player) (fruit ?Fruit) => (toFruit ?Fruit ?Dist0)");
			rules.add("(distanceDot ?Player ?Dot ?Dist0) (pacman ?Player) "
					+ "(dot ?Dot) => (toDot ?Dot ?Dist0)");
			rules.add("(distanceGhost ?Player ?Ghost ?Dist0) "
					+ "(pacman ?Player) (not (edible ?Ghost)) (ghost ?Ghost) "
					+ "=> (fromGhost ?Ghost ?Dist0)");
		}

		for (String rule : rules)
			goodPolicy.addRule(new GuidedRule(rule), false, false);

		return goodPolicy;
	}

	@Override
	protected Collection<StringFact> initialisePredicateTemplates() {
		Collection<StringFact> predicates = new ArrayList<StringFact>();

		// Score
		String[] structure = new String[1];
		structure[0] = Number.Integer.toString();
		predicates.add(new StringFact("score", structure));

		// High Score
		structure = new String[1];
		structure[0] = Number.Integer.toString();
		predicates.add(new StringFact("highScore", structure));

		// Lives
		structure = new String[1];
		structure[0] = Number.Integer.toString();
		predicates.add(new StringFact("lives", structure));

		// Level
		structure = new String[1];
		structure[0] = Number.Integer.toString();
		predicates.add(new StringFact("level", structure));

		// Edible
		structure = new String[1];
		structure[0] = "ghost";
		predicates.add(new StringFact("edible", structure));

		// Blinking
		structure = new String[1];
		structure[0] = "ghost";
		predicates.add(new StringFact("blinking", structure));

		// Distance Metrics
		structure = new String[3];
		structure[0] = "pacman";
		structure[1] = "dot";
		structure[2] = Number.Integer.toString();
		predicates.add(new StringFact("distanceDot", structure));

		structure = new String[3];
		structure[0] = "pacman";
		structure[1] = "powerDot";
		structure[2] = Number.Integer.toString();
		predicates.add(new StringFact("distancePowerDot", structure));

		structure = new String[3];
		structure[0] = "pacman";
		structure[1] = "ghost";
		structure[2] = Number.Integer.toString();
		predicates.add(new StringFact("distanceGhost", structure));

		structure = new String[3];
		structure[0] = "pacman";
		structure[1] = "ghostCentre";
		structure[2] = Number.Double.toString();
		predicates.add(new StringFact("distanceGhostCentre", structure));

		structure = new String[3];
		structure[0] = "pacman";
		structure[1] = "fruit";
		structure[2] = Number.Integer.toString();
		predicates.add(new StringFact("distanceFruit", structure));

		structure = new String[2];
		structure[0] = "junction";
		structure[1] = Number.Integer.toString();
		predicates.add(new StringFact("junctionSafety", structure));

		return predicates;
	}

	@Override
	protected Map<String, String> initialiseTypePredicateTemplates() {
		Map<String, String> typeMap = new HashMap<String, String>();

		typeMap.put("pacman", null);
		typeMap.put("dot", null);
		typeMap.put("powerDot", null);
		typeMap.put("ghost", null);
		typeMap.put("fruit", null);
		typeMap.put("ghostCentre", null);
		typeMap.put("junction", null);

		return typeMap;
	}

	/**
	 * Applies an action and returns a direction to move towards/from.
	 * 
	 * @param action
	 *            The action to apply.
	 * @return A Byte direction to move towards/from.
	 */
	public WeightedDirection applyAction(StringFact action, PacManState state) {
		String[] arguments = action.getArguments();
		double weight = determineWeight(Integer.parseInt(arguments[1]));

		// Move towards static points (dots, powerdots, junctions)
		if ((action.getFactName().equals("toDot"))
				|| (action.getFactName().equals("toPowerDot"))
				|| (action.getFactName().equals("fromPowerDot"))
				|| (action.getFactName().equals("toGhostCentre"))
				|| (action.getFactName().equals("fromGhostCentre"))) {
			// To Dot, to/from powerdot
			String[] coords = arguments[0].split("_");
			DistanceDir distanceGrid = state.getDistanceGrid()[Integer
					.parseInt(coords[1])][Integer.parseInt(coords[2])];
			if (distanceGrid == null)
				return null;
			byte path = distanceGrid.getDirection();
			if ((action.getFactName().equals("fromPowerDot"))
					|| (action.getFactName().equals("fromGhostCentre")))
				path *= -1;
			return new WeightedDirection(path, weight);

		} else if (action.getFactName().equals("toFruit")) {
			// To fruit
			DistanceDir distanceGrid = state.getDistanceGrid()[state.getFruit().m_locX][state
					.getFruit().m_locY];
			if (distanceGrid == null)
				return null;
			return new WeightedDirection(distanceGrid.getDirection(), weight);

		} else if (action.getFactName().equals("toJunction")) {
			String[] coords = arguments[0].split("_");
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
			int juncVal = Integer.parseInt(arguments[1]);
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

		} else if ((action.getFactName().equals("toGhost"))
				|| (action.getFactName().equals("fromGhost"))) {
			int ghostIndex = 0;
			if (arguments[0].equals("blinky"))
				ghostIndex = Ghost.BLINKY;
			else if (arguments[0].equals("pinky"))
				ghostIndex = Ghost.PINKY;
			else if (arguments[0].equals("inky"))
				ghostIndex = Ghost.INKY;
			else if (arguments[0].equals("clyde"))
				ghostIndex = Ghost.CLYDE;

			DistanceDir distanceGrid = state.getDistanceGrid()[state
					.getGhosts()[ghostIndex].m_locX][state.getGhosts()[ghostIndex].m_locY];
			if (distanceGrid == null)
				return null;
			byte path = distanceGrid.getDirection();
			if (action.getFactName().equals("toGhost"))
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
