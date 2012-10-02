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
 *    src/blocksWorldAlternative/BlocksWorldStateSpec.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package blocksWorldAlternative;

import relationalFramework.NumberEnum;
import relationalFramework.RelationalPredicate;
import relationalFramework.agentObservations.BackgroundKnowledge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class BlocksWorldStateSpec extends blocksWorld.BlocksWorldStateSpec {

	@Override
	protected Map<String, String> initialiseActionPreconditions() {
		Map<String, String> actionPreconditions = new HashMap<String, String>();

		actionPreconditions.put("move", "(clear ?A) (clear ?B &:(neq ?A ?B))");
		actionPreconditions.put("moveFloor", "(clear ?A) (on ?A ?)");

		return actionPreconditions;
	}

	@Override
	protected Collection<RelationalPredicate> initialiseActionTemplates() {
		Collection<RelationalPredicate> actions = new ArrayList<RelationalPredicate>();

		// Move action
		String[] structure = new String[2];
		structure[0] = "block";
		structure[1] = "block";
		actions.add(new RelationalPredicate("move", structure, false));

		// MoveFloor action
		structure = new String[1];
		structure[0] = "block";
		actions.add(new RelationalPredicate("moveFloor", structure, false));

		return actions;
	}

	@Override
	protected Collection<String> initialiseActionRules() {
		Collection<String> actionRules = new ArrayList<String>();
		// Block to block movement
		actionRules.add("?action <- (move ?A ?B) ?oldOn <- (on ?A ?C)"
				+ " => (assert (on ?A ?B)) (retract ?oldOn ?action)");
		// Floor block to block movement
		actionRules.add("?action <- (move ?A ?B) ?oldOnFl <- (onFloor ?A)"
				+ " => (assert (on ?A ?B)) (retract ?oldOnFl ?action)");
		// Move floor movement
		actionRules.add("?action <- (moveFloor ?A) ?oldOn <- (on ?A ?C)"
				+ " => (assert (onFloor ?A)) (retract ?oldOn ?action)");
		return actionRules;
	}

	@Override
	protected Map<String, BackgroundKnowledge> initialiseBackgroundKnowledge() {
		Map<String, BackgroundKnowledge> bkMap = new HashMap<String, BackgroundKnowledge>();

		// Block(Y) & !On(?,Y) -> Clear(Y)
		bkMap.put("clearRule", new BackgroundKnowledge(
				"(block ?B) (not (on ? ?B)) => (assert (clear ?B))", true));

		// On(X,Y) -> Above(X,Y)
		bkMap.put("aboveRule1", new BackgroundKnowledge(
				"(on ?A ?B) => (assert (above ?A ?B))", true));

		// Blocks on floor is at height a
		bkMap.put("floorTruthB", new BackgroundKnowledge(
				"(onFloor ?A) => (assert (height ?A 1))", true));

		// On(X,Y) & Above(Y,Z) -> Above(X,Z)
		bkMap.put("aboveRule2", new BackgroundKnowledge(
				"(on ?A ?B) (above ?B ?C) => (assert (above ?A ?C))", true));

		// Height of individual blocks (for highest calcs)
		bkMap.put("heightRule", new BackgroundKnowledge(
				"(on ?A ?B) (height ?B ?N) => (assert (height ?A (+ ?N 1)))",
				true));

		// Highest rule
		bkMap.put("highestRule", new BackgroundKnowledge(
				"(height ?A ?N) (forall (block ?B) (height ?B ?M&:(<= ?M ?N)))"
						+ " => (assert (highest ?A))", true));

		return bkMap;
	}

	@Override
	protected String[] initialiseGoalState() {
		String[] result = super.initialiseGoalState();

		// Unstack goal
		if (envParameter_.equals("unstack")) {
			result[0] = "unstack";
			result[1] = "(not (on ?A ?B))";
			return result;
		}

		// Stack goal
		if (envParameter_.equals("stack")) {
			result[0] = "stack";
			result[1] = "(onFloor ?A) (not (onFloor ?C&:(<> ?C ?A)))";
			return result;
		}
		return result;
	}

	@Override
	protected Collection<RelationalPredicate> initialisePredicateTemplates() {
		Collection<RelationalPredicate> predicates = new ArrayList<RelationalPredicate>();

		// On predicate
		String[] structure = new String[2];
		structure[0] = "block";
		structure[1] = "block";
		predicates.add(new RelationalPredicate("on", structure, false));

		// OnFloor predicate
		structure = new String[1];
		structure[0] = "block";
		predicates.add(new RelationalPredicate("onFloor", structure, false));

		// Clear predicate
		structure = new String[1];
		structure[0] = "block";
		predicates.add(new RelationalPredicate("clear", structure, false));

		// Above predicate
		structure = new String[2];
		structure[0] = "block";
		structure[1] = "block";
		predicates.add(new RelationalPredicate("above", structure, false));

		// Height predicate
		structure = new String[2];
		structure[0] = "block";
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("height", structure, true));

		// Highest predicate
		structure = new String[1];
		structure[0] = "block";
		predicates.add(new RelationalPredicate("highest", structure, false));

		return predicates;
	}

	@Override
	protected Map<String, String> initialiseTypePredicateTemplates() {
		Map<String, String> typePreds = new HashMap<String, String>();

		typePreds.put("block", null);

		return typePreds;
	}

	@Override
	protected Collection<String> initialiseConstantFacts() {
		return null;
	}
}
