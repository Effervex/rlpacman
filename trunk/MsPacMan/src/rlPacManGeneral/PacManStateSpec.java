package rlPacManGeneral;

import relationalFramework.BasicRelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import msPacMan.Ghost;

import cerrla.NumberEnum;

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
		preconds.put("moveTo", "(thing ?X) (distance ?X ?Y)");
		preconds.put("moveFrom", "(thing ?X) (distance ?X ?Y)");
		preconds.put("toJunction", "(junction ?X) (junctionSafety ?X ?Y)");

		return preconds;
	}

	@Override
	protected int initialiseActionsPerStep() {
		return -1;
	}

	@Override
	protected Collection<RelationalPredicate> initialiseActionTemplates() {
		Collection<RelationalPredicate> actions = new ArrayList<RelationalPredicate>();

		// Actions have a type and a distance
		String[] structure = new String[2];
		structure[0] = "thing";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("moveTo", structure));

		structure = new String[2];
		structure[0] = "thing";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("moveFrom", structure));

		structure = new String[2];
		structure[0] = "junction";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("toJunction", structure));

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
			result[1] = "(score ?Y&:(>= ?Y 10000))";
			return result;
		}
		
		if (envParameter_.equals("levelMax")
				|| envParameter_.equals("oneLevel")) {
			// Score maximisation over a single level
			result[0] = "1level";
			result[1] = "(level 2)";
			return result;
		}
		
		if (envParameter_.equals("survival")
				|| envParameter_.equals("survive")) {
			// Score maximisation over a single level
			result[0] = "survive";
			result[1] = "(level 11)";
			return result;
		}

		// Score maximisation by default
		return null;
	}

	@Override
	protected BasicRelationalPolicy initialiseHandCodedPolicy() {
		BasicRelationalPolicy goodPolicy = new BasicRelationalPolicy();

		// Defining a good policy
		ArrayList<String> rules = new ArrayList<String>();

		if (envParameter_ != null && envParameter_.equals("noDots")) {
			// Good policy for no dots play
			rules
					.add("(distanceGhost ?Ghost ?Dist0&:(betweenRange ?Dist0 0 5)) "
							+ "(not (edible ?Ghost)) (ghost ?Ghost) "
							+ "=> (moveFrom ?Ghost ?Dist0)");
			rules.add("(distance ?Ghost ?Dist0&:(betweenRange ?Dist0 0 15)) "
					+ "(edible ?Ghost) (not (blinking ?Ghost)) "
					+ "(ghost ?Ghost) => (moveTo ?Ghost ?Dist0)");
			rules
					.add("(not (edible ?Ghost)) "
							+ "(distance ?Ghost ?Dist0&:(betweenRange ?Dist0 0 10)) "
							+ "(distance ?PowerDot ?Dist1&:(betweenRange ?Dist1 0 10)) "
							+ "(ghost ?Ghost) "
							+ "(powerDot ?PowerDot) => (moveTo ?PowerDot ?Dist1)");
			rules.add("(distance ?PowerDot ?Dist0&:(betweenRange ?Dist0 0 2)) "
					+ "(distance ?Ghost ?Dist1&:(betweenRange ?Dist1 0 15)) "
					+ "(edible ?Ghost) (powerDot ?PowerDot) "
					+ "=> (moveFrom ?PowerDot ?Dist0)");
			rules.add("(distance ?Fruit ?Dist0&:(betweenRange ?Dist0 0 20)) "
					+ "(fruit ?Fruit) => (moveTo ?Fruit ?Dist0)");
			rules.add("(distance ?PDot ?Dist0&:(betweenRange ?Dist0 2 99)) "
					+ "(powerDot ?PDot) " + "=> (moveTo ?PDot ?Dist0)");
		} else if (envParameter_ != null && envParameter_.equals("noPowerDots")) {
			// Good policy for no power dot play
			rules.add("(distance ?Ghost ?Dist0&:(betweenRange ?Dist0 0 5)) "
					+ "(ghost ?Ghost) => (moveFrom ?Ghost ?Dist0)");
			rules.add("(distance ?Fruit ?Dist0&:(betweenRange ?Dist0 0 20)) "
					+ "(fruit ?Fruit) => (moveTo ?Fruit ?Dist0)");
			rules.add("(distanceDot ?Player ?Dot ?Dist0) "
					+ "(dot ?Dot) => (moveTo ?Dot ?Dist0)");
		} else {
			// Good policy for regular play
			rules.add("(distance ?Ghost ?Dist0&:(betweenRange ?Dist0 0 5)) "
					+ "(not (edible ?Ghost)) (ghost ?Ghost) "
					+ "=> (moveFrom ?Ghost ?Dist0)");
			rules.add("(distance ?Ghost ?Dist0&:(betweenRange ?Dist0 0 30)) "
					+ "(edible ?Ghost) (not (blinking ?Ghost)) "
					+ "(ghost ?Ghost) => (moveTo ?Ghost ?Dist0)");
			rules
					.add("(not (edible ?Ghost)) "
							+ "(distance ?Ghost ?Dist0&:(betweenRange ?Dist0 0 10)) "
							+ "(distance ?PowerDot ?Dist1&:(betweenRange ?Dist1 0 10)) "
							+ "(ghost ?Ghost) "
							+ "(powerDot ?PowerDot) => (moveTo ?PowerDot ?Dist1)");
			rules.add("(distance ?PowerDot ?Dist0&:(betweenRange ?Dist0 0 3)) "
					+ "(distance ?Ghost ?Dist1&:(betweenRange ?Dist1 0 15)) "
					+ "(edible ?Ghost) (powerDot ?PowerDot) "
					+ "=> (moveFrom ?PowerDot ?Dist0)");
			rules.add("(distance ?Fruit ?Dist0&:(betweenRange ?Dist0 0 20)) "
					+ "(fruit ?Fruit) => (moveTo ?Fruit ?Dist0)");
			rules.add("(distance ?Dot ?Dist0) "
					+ "(dot ?Dot) => (moveTo ?Dot ?Dist0)");
			rules
					.add("(junctionSafety ?X ?__Num3&:(betweenRange ?__Num3 -16.0 28.0)) "
							+ "=> (toJunction ?X ?__Num3)");
		}

		for (String rule : rules)
			goodPolicy.addRule(new RelationalRule(rule));

		return goodPolicy;
	}

	@Override
	protected Collection<RelationalPredicate> initialisePredicateTemplates() {
		Collection<RelationalPredicate> predicates = new ArrayList<RelationalPredicate>();

		// Score
		String[] structure = new String[1];
		structure[0] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("score", structure));

		// High Score
		structure = new String[1];
		structure[0] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("highScore", structure));

		// Lives
		structure = new String[1];
		structure[0] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("lives", structure));

		// Level
		structure = new String[1];
		structure[0] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("level", structure));

		// Edible
		structure = new String[1];
		structure[0] = "ghost";
		predicates.add(new RelationalPredicate("edible", structure));

		// Blinking
		structure = new String[1];
		structure[0] = "ghost";
		predicates.add(new RelationalPredicate("blinking", structure));

		// Distance Metrics
		structure = new String[2];
		structure[0] = "thing";
		structure[1] = NumberEnum.Double.toString();
		predicates.add(new RelationalPredicate("distance", structure));

		structure = new String[2];
		structure[0] = "junction";
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("junctionSafety", structure));

		return predicates;
	}

	@Override
	protected Map<String, String> initialiseTypePredicateTemplates() {
		Map<String, String> typeMap = new HashMap<String, String>();

		typeMap.put("thing", null);
		typeMap.put("dot", "thing");
		typeMap.put("powerDot", "thing");
		typeMap.put("ghost", "thing");
		typeMap.put("fruit", "thing");
		typeMap.put("ghostCentre", "thing");
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
	public WeightedDirection applyAction(RelationalPredicate action, PacManState state) {
		String[] arguments = action.getArguments();
		double weight = determineWeight(Integer.parseInt(arguments[1]));

		// First parse the location of the object
		int x = 0;
		int y = 0;
		if (arguments[0].equals("fruit")) {
			x = state.getFruit().m_locX;
			y = state.getFruit().m_locY;
		} else if (arguments[0].equals("blinky")) {
			x = state.getGhosts()[Ghost.BLINKY].m_locX;
			y = state.getGhosts()[Ghost.BLINKY].m_locY;
		} else if (arguments[0].equals("inky")) {
			x = state.getGhosts()[Ghost.INKY].m_locX;
			y = state.getGhosts()[Ghost.INKY].m_locY;
		} else if (arguments[0].equals("pinky")) {
			x = state.getGhosts()[Ghost.PINKY].m_locX;
			y = state.getGhosts()[Ghost.PINKY].m_locY;
		} else if (arguments[0].equals("clyde")) {
			x = state.getGhosts()[Ghost.CLYDE].m_locX;
			y = state.getGhosts()[Ghost.CLYDE].m_locY;
		} else {
			String[] coords = arguments[0].split("_");
			x = Integer.parseInt(coords[1]);
			y = Integer.parseInt(coords[2]);
		}

		DistanceDir distanceGrid = state.getDistanceGrid()[x][y];
		if (distanceGrid == null)
			return null;
		byte path = distanceGrid.getDirection();
		if (action.getFactName().equals("moveTo"))
			return new WeightedDirection(path, weight);
		else if (action.getFactName().equals("moveFrom"))
			return new WeightedDirection((byte) (-path), weight);
		else if (action.getFactName().equals("toJunction")) {
			// Junction value
			int juncVal = Integer.parseInt(arguments[1]);

			return new WeightedDirection(distanceGrid.getDirection(), juncVal);
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
