/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/rlPacMan/PacManStateSpec.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package rlPacMan;

import relationalFramework.NumberEnum;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


import msPacMan.Ghost;


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
		preconds.put("moveTo", "(thing ?A) (distance ?A ?B)");
		preconds.put("moveFrom", "(thing ?A) (distance ?A ?B)");
		preconds.put("toJunction", "(junction ?A) (junctionSafety ?A ?B)");

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
		actions.add(new RelationalPredicate("moveTo", structure, false));

		structure = new String[2];
		structure[0] = "thing";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("moveFrom", structure, false));

		structure = new String[2];
		structure[0] = "junction";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("toJunction", structure, false));

		return actions;
	}

	@Override
	protected Map<String, BackgroundKnowledge> initialiseBackgroundKnowledge() {
		Map<String, BackgroundKnowledge> bckKnowledge = new HashMap<String, BackgroundKnowledge>();

		return bckKnowledge;
	}

	@Override
	protected String[] initialiseGoalState() {
		if (envParameter_ == null)
			envParameter_ = "10000";

		String[] result = new String[2];
		if (envParameter_.equals("lvl10")
				|| envParameter_.equals("10levels")) {
			// Actual goal condition
			result[0] = "10levels";
			result[1] = "(level 11)";
			return result;
		}
		
		if (envParameter_.equals("10000")
				|| envParameter_.equals("10000points")) {
			// Score maximisation
			result[0] = "10000points";
			result[1] = "(score ?B&:(>= ?B 10000))";
			return result;
		}
		
		if (envParameter_.equals("levelMax")
				|| envParameter_.equals("oneLevel")
				|| envParameter_.equals("1Level")) {
			// Score maximisation over a single level
			result[0] = "1level";
			result[1] = "(level 2)";
			return result;
		}
		
		if (envParameter_.equals("survival")
				|| envParameter_.equals("survive")) {
			// Score maximisation over a single life
			result[0] = "survive";
			result[1] = "(level 11)";
			return result;
		}
		
		if (envParameter_.equals("edible")) {
			result[0] = "edible";
			result[1] = "(edible ?)";
			return result;
		}

		// Score maximisation by default
		return null;
	}
	
	@Override
	protected Collection<RelationalPredicate> initialisePredicateTemplates() {
		Collection<RelationalPredicate> predicates = new ArrayList<RelationalPredicate>();

		// Score
		String[] structure = new String[1];
		structure[0] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("score", structure, false));

		// High Score
		structure = new String[1];
		structure[0] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("highScore", structure, false));

		// Lives
		structure = new String[1];
		structure[0] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("lives", structure, false));

		// Level
		structure = new String[1];
		structure[0] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("level", structure, false));

		// Edible
		structure = new String[1];
		structure[0] = "ghost";
		predicates.add(new RelationalPredicate("edible", structure, false));

		// Blinking
		structure = new String[1];
		structure[0] = "ghost";
		predicates.add(new RelationalPredicate("blinking", structure, false));

		// Distance Metric
		structure = new String[2];
		structure[0] = "thing";
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("distance", structure, false));

		structure = new String[2];
		structure[0] = "junction";
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("junctionSafety", structure, false));

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

	@Override
	protected Collection<String> initialiseConstantFacts() {
		return null;
	}
}
