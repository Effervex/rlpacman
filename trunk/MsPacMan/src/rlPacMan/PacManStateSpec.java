package rlPacMan;

import relationalFramework.NumberEnum;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import rlPacManGeneral.DistanceDir;
import rlPacManGeneral.PacManState;
import rlPacManGeneral.WeightedDirection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import msPacMan.Ghost;

/**
 * The state specifications for the PacMan domain.
 * 
 * @author Sam Sarjant
 */
public class PacManStateSpec extends rlPacManGeneral.PacManStateSpec {
	@Override
	protected Map<String, String> initialiseActionPreconditions() {
		Map<String, String> preconds = new HashMap<String, String>();
		// Basic preconditions for actions
		preconds.put("toDot", "(dot ?X) (distance ?X ?Y)");
		preconds.put("toPowerDot",
				"(powerDot ?X) (distance ?X ?Y)");
		preconds.put("fromPowerDot",
				"(powerDot ?X) (distance ?X ?Y)");
		preconds.put("toFruit", "(fruit ?X) (distance ?X ?Y)");
		preconds.put("toGhost", "(ghost ?X) (distance ?X ?Y)");
		preconds.put("fromGhost", "(ghost ?X) (distance ?X ?Y)");
		preconds.put("toGhostCentre",
				"(ghostCentre ?X) (distance ?X ?Y)");
		preconds.put("fromGhostCentre",
				"(ghostCentre ?X) (distance ?X ?Y)");
		preconds.put("toJunction", "(junction ?X) (junctionSafety ?X ?Y)");

		return preconds;
	}

	@Override
	protected Collection<RelationalPredicate> initialiseActionTemplates() {
		Collection<RelationalPredicate> actions = new ArrayList<RelationalPredicate>();

		// Actions have a type and a distance
		String[] structure = new String[2];
		structure[0] = "dot";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("toDot", structure));

		structure = new String[2];
		structure[0] = "powerDot";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("toPowerDot", structure));

		structure = new String[2];
		structure[0] = "powerDot";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("fromPowerDot", structure));

		structure = new String[2];
		structure[0] = "fruit";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("toFruit", structure));

		structure = new String[2];
		structure[0] = "ghost";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("toGhost", structure));

		structure = new String[2];
		structure[0] = "ghost";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("fromGhost", structure));

		structure = new String[2];
		structure[0] = "ghostCentre";
		structure[1] = NumberEnum.Double.toString();
		actions.add(new RelationalPredicate("toGhostCentre", structure));

		structure = new String[2];
		structure[0] = "ghostCentre";
		structure[1] = NumberEnum.Double.toString();
		actions.add(new RelationalPredicate("fromGhostCentre", structure));

		structure = new String[2];
		structure[0] = "junction";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("toJunction", structure));

		// TODO Add ghost density

		return actions;
	}

	@Override
	protected RelationalPolicy initialiseHandCodedPolicy() {
		RelationalPolicy goodPolicy = new RelationalPolicy();

		// Defining a good policy
		ArrayList<String> rules = new ArrayList<String>();

		if (envParameter_ != null && envParameter_.equals("noDots")) {
			// Good policy for no dots play
			rules.add("(distance ?Ghost ?Dist0&:(betweenRange ?Dist0 0 5)) "
					+ "(not (edible ?Ghost)) (ghost ?Ghost) "
					+ "=> (fromGhost ?Ghost ?Dist0)");
			rules.add("(distance ?Ghost ?Dist0&:(betweenRange ?Dist0 0 15)) "
					+ "(edible ?Ghost) (not (blinking ?Ghost)) "
					+ "(ghost ?Ghost) => (toGhost ?Ghost ?Dist0)");
			rules.add("(not (edible ?Ghost)) "
					+ "(distance ?Ghost ?Dist0&:(betweenRange ?Dist0 0 10)) "
					+ "(distance ?PowerDot ?Dist1&:(betweenRange ?Dist1 0 10)) "
					+ "(ghost ?Ghost) "
					+ "(powerDot ?PowerDot) => (toPowerDot ?PowerDot ?Dist1)");
			rules.add("(distance ?PowerDot ?Dist0&:(betweenRange ?Dist0 0 2)) "
					+ "(distance ?Ghost ?Dist1&:(betweenRange ?Dist1 0 15)) "
					+ "(edible ?Ghost) (powerDot ?PowerDot) "
					+ "=> (fromPowerDot ?PowerDot ?Dist0)");
			rules.add("(distance ?Fruit ?Dist0&:(betweenRange ?Dist0 0 20)) "
					+ "(fruit ?Fruit) => (toFruit ?Fruit ?Dist0)");
			rules.add("(distance ?PDot ?Dist0&:(betweenRange ?Dist0 2 99)) "
					+ "(powerDot ?PDot) " + "=> (toPowerDot ?PDot ?Dist0)");
		} else if (envParameter_ != null && envParameter_.equals("noPowerDots")) {
			// Good policy for no power dot play
			rules.add("(distance ?Ghost ?Dist0&:(betweenRange ?Dist0 0 5)) "
					+ "(ghost ?Ghost) => (fromGhost ?Ghost ?Dist0)");
			rules.add("(distance ?Fruit ?Dist0&:(betweenRange ?Dist0 0 20)) "
					+ "(fruit ?Fruit) => (toFruit ?Fruit ?Dist0)");
			rules.add("(distance ?Dot ?Dist0) "
					+ "(dot ?Dot) => (toDot ?Dot ?Dist0)");
		} else {
			// Good policy for regular play
			rules.add("(distance ?Ghost ?Dist0&:(betweenRange ?Dist0 0 5)) "
					+ "(not (edible ?Ghost)) (ghost ?Ghost) "
					+ "=> (fromGhost ?Ghost ?Dist0)");
			rules.add("(distance ?Ghost ?Dist0&:(betweenRange ?Dist0 0 10)) "
					+ "(not (edible ?Ghost)) (ghost ?Ghost) "
					+ "=> (fromGhost ?Ghost ?Dist0)");
			rules.add("(distance ?Ghost ?Dist0&:(betweenRange ?Dist0 0 15)) "
					+ "(edible ?Ghost) (not (blinking ?Ghost)) "
					+ "(ghost ?Ghost) => (toGhost ?Ghost ?Dist0)");
			rules.add("(not (edible ?Ghost)) "
					+ "(distance ?Ghost ?Dist0&:(betweenRange ?Dist0 0 10)) "
					+ "(distance ?PowerDot ?Dist1&:(betweenRange ?Dist1 0 10)) "
					+ "(ghost ?Ghost) "
					+ "(powerDot ?PowerDot) => (toPowerDot ?PowerDot ?Dist1)");
			rules.add("(distance ?PowerDot ?Dist0&:(betweenRange ?Dist0 0 3)) "
					+ "(distance ?Ghost ?Dist1&:(betweenRange ?Dist1 0 15)) "
					+ "(edible ?Ghost) (powerDot ?PowerDot) "
					+ "=> (fromPowerDot ?PowerDot ?Dist0)");
			rules.add("(distance ?Fruit ?Dist0&:(betweenRange ?Dist0 0 20)) "
					+ "(fruit ?Fruit) => (toFruit ?Fruit ?Dist0)");
			rules.add("(distance ?Dot ?Dist0) "
					+ "(dot ?Dot) => (toDot ?Dot ?Dist0)");
			rules.add("(distance ?Ghost ?Dist0) "
					+ "(not (edible ?Ghost)) (ghost ?Ghost) "
					+ "=> (fromGhost ?Ghost ?Dist0)");
		}

		for (String rule : rules)
			goodPolicy.addRule(new RelationalRule(rule));

		return goodPolicy;
	}

	/**
	 * Applies an action and returns a direction to move towards/from.
	 * 
	 * @param action
	 *            The action to apply.
	 * @return A Byte direction to move towards/from.
	 */
	@Override
	public WeightedDirection applyAction(RelationalPredicate action,
			PacManState state) {
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

			// Junction value
			int juncVal = Integer.parseInt(arguments[1]);
			// If the junction has safety 0, there is neither weight towards,
			// nor from it.
			DistanceDir distanceGrid = state.getDistanceGrid()[Integer
					.parseInt(coords[1])][Integer.parseInt(coords[2])];
			return new WeightedDirection(distanceGrid.getDirection(), juncVal);

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
