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
 *    src/blocksWorldAlternative/BlocksWorldEnvironment.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package blocksWorldAlternative;

import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import rrlFramework.RRLExperiment;
import util.MultiMap;
import java.util.List;

import jess.Rete;

import blocksWorld.BlocksState;

/**
 * The environment for the blocks world interface.
 * 
 * @author Sam Sarjant
 */
public class BlocksWorldEnvironment extends blocksWorld.BlocksWorldEnvironment {
	@Override
	protected void assertStateFacts(Rete rete, List<String> goalArgs)
			throws Exception {
		if (steps_ != 0) {
			// Apply the action
			rete.run();
			return;
		}

		Integer[] intState = state_.getState();

		// Scanning through, making predicates (On, OnFloor)
		for (int i = 0; i < state_.length; i++) {
			// On the floor
			if (intState[i] == 0) {
				rete.assertString("(onFloor " + (char) ('a' + i) + ")");
			} else {
				// On another block
				rete.assertString("(on " + (char) ('a' + i) + " "
						+ (char) ('a' + intState[i] - 1) + ")");
			}

			// Assert the blocks
			rete.assertString("(block " + (char) ('a' + i) + ")");
		}
	}

	protected RelationalPredicate randomAction() {
		// Select random action predicate
		MultiMap<String, String[]> validActions = observations_
				.getValidActions();
		Object[] moveArgs = new Object[0];
		Object[] moveFlArgs = new Object[0];
		try {
			if (validActions.containsKey("move"))
				moveArgs = validActions.getSortedSet("move").toArray();
			if (validActions.containsKey("moveFloor"))
				moveFlArgs = validActions.getSortedSet("moveFloor").toArray();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		int randomIndex = RRLExperiment.random_.nextInt(moveArgs.length
				+ moveFlArgs.length);
		if (randomIndex < moveArgs.length) {
			Object randomAction = moveArgs[randomIndex];
			return new RelationalPredicate(StateSpec.getInstance().getActions()
					.get("move"), (String[]) randomAction);
		} else {
			Object randomAction = moveFlArgs[randomIndex - moveArgs.length];
			return new RelationalPredicate(StateSpec.getInstance().getActions()
					.get("moveFloor"), (String[]) randomAction);
		}
	}

	@Override
	protected void resolveAction(BlocksState newState,
			RelationalPredicate actionFact) {
		// Finding the block objects
		if (actionFact.getFactName().equals("move")) {
			int[] indices = new int[2];

			// Convert the blocks to indices
			for (int i = 0; i < indices.length; i++) {
				if (actionFact.getArguments()[i].equals("floor"))
					indices[i] = -1;
				else
					indices[i] = (actionFact.getArguments()[i].charAt(0)) - ('a');
			}

			// Perform the action
			newState.getState()[indices[0]] = indices[1] + 1;
		} else if (actionFact.getFactName().equals("moveFloor")) {
			int index = actionFact.getArguments()[0].charAt(0) - 'a';
			newState.getState()[index] = 0;
		}
	}

	@Override
	protected RelationalPolicy createOptimalPolicy(String goal) {
		// Defining the optimal policy based on the goal
		String[] rules = null;
		if (goal.equals("onab") || goal.equals("on$A$B")) {
			rules = new String[3];
			rules[0] = "(" + StateSpec.GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?G_0) (clear ?G_1) => (move ?G_0 ?G_1)";
			rules[1] = "(" + StateSpec.GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?A) (above ?A ?G_0) => (moveFloor ?A)";
			rules[2] = "(" + StateSpec.GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?A) (above ?A ?G_1) => (moveFloor ?A)";
		} else if (goal.equals("stack")) {
			rules = new String[1];
			rules[0] = "(clear ?A) (highest ?B) => (move ?A ?B)";
		} else if (goal.equals("unstack")) {
			rules = new String[1];
			rules[0] = "(highest ?A) => (moveFloor ?A)";
		} else if (goal.equals("clearA")
				|| goal.equals("clear$A")) {
			rules = new String[1];
			rules[0] = "(" + StateSpec.GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?A) (above ?A ?G_0) => (moveFloor ?A)";
		} else if (goal.equals("highestA")
				|| goal.equals("highest$A")) {
			rules = new String[2];
			rules[0] = "(" + StateSpec.GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?A) (above ?A ?G_0) => (moveFloor ?A)";
			rules[1] = "(" + StateSpec.GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?G_0) (highest ?B) => (move ?G_0 ?B)";
		}

		RelationalPolicy optimal = new RelationalPolicy();
		for (int i = 0; i < rules.length; i++)
			optimal.addRule(new RelationalRule(rules[i]));

		return optimal;
	}
}
