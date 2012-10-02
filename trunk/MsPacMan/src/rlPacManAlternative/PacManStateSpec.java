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
 *    src/rlPacManAlternative/PacManStateSpec.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package rlPacManAlternative;

import relationalFramework.NumberEnum;
import relationalFramework.RelationalPredicate;
import rlPacMan.DistanceDir;
import rlPacMan.PacManState;
import rlPacMan.WeightedDirection;

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
public class PacManStateSpec extends rlPacMan.PacManStateSpec {
	@Override
	protected Map<String, String> initialiseActionPreconditions() {
		Map<String, String> preconds = new HashMap<String, String>();
		// Basic preconditions for actions
		preconds.put("toDot", "(dot ?A) (distance ?A ?B)");
		preconds.put("toPowerDot",
				"(powerDot ?A) (distance ?A ?B)");
		preconds.put("fromPowerDot",
				"(powerDot ?A) (distance ?A ?B)");
		preconds.put("toFruit", "(fruit ?A) (distance ?A ?B)");
		preconds.put("toGhost", "(ghost ?A) (distance ?A ?B)");
		preconds.put("fromGhost", "(ghost ?A) (distance ?A ?B)");
		preconds.put("toGhostCentre",
				"(ghostCentre ?A) (distance ?A ?B)");
		preconds.put("fromGhostCentre",
				"(ghostCentre ?A) (distance ?A ?B)");
		preconds.put("toJunction", "(junction ?A) (junctionSafety ?A ?B)");

		return preconds;
	}

	@Override
	protected Collection<RelationalPredicate> initialiseActionTemplates() {
		Collection<RelationalPredicate> actions = new ArrayList<RelationalPredicate>();

		// Actions have a type and a distance
		String[] structure = new String[2];
		structure[0] = "dot";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("toDot", structure, false));

		structure = new String[2];
		structure[0] = "powerDot";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("toPowerDot", structure, false));

		structure = new String[2];
		structure[0] = "powerDot";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("fromPowerDot", structure, false));

		structure = new String[2];
		structure[0] = "fruit";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("toFruit", structure, false));

		structure = new String[2];
		structure[0] = "ghost";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("toGhost", structure, false));

		structure = new String[2];
		structure[0] = "ghost";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("fromGhost", structure, false));

		structure = new String[2];
		structure[0] = "ghostCentre";
		structure[1] = NumberEnum.Double.toString();
		actions.add(new RelationalPredicate("toGhostCentre", structure, false));

		structure = new String[2];
		structure[0] = "ghostCentre";
		structure[1] = NumberEnum.Double.toString();
		actions.add(new RelationalPredicate("fromGhostCentre", structure, false));

		structure = new String[2];
		structure[0] = "junction";
		structure[1] = NumberEnum.Integer.toString();
		actions.add(new RelationalPredicate("toJunction", structure, false));

		return actions;
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
