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
 *    src/blocksWorld/BlocksWorldStateSpec.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package blocksWorld;

import relationalFramework.NumberEnum;
import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import relationalFramework.agentObservations.BackgroundKnowledge;

public class BlocksWorldStateSpec extends StateSpec {

	@Override
	protected Map<String, String> initialiseActionPreconditions() {
		Map<String, String> actionPreconditions = new HashMap<String, String>();

		actionPreconditions.put("move",
				"(clear ?A) (block ?A) (clear ?B &:(neq ?A ?B)) "
						+ "(not (on ?A ?B))");

		return actionPreconditions;
	}

	@Override
	protected int initialiseActionsPerStep() {
		return 1;
	}

	@Override
	protected Collection<RelationalPredicate> initialiseActionTemplates() {
		Collection<RelationalPredicate> actions = new ArrayList<RelationalPredicate>();

		// Move action
		String[] structure = new String[2];
		structure[0] = "block";
		structure[1] = "thing";
		actions.add(new RelationalPredicate("move", structure, false));

		return actions;
	}

	@Override
	protected Collection<String> initialiseActionRules() {
		Collection<String> actionRules = new ArrayList<String>();
		// Block to block movement
		actionRules.add("?action <- (move ?A ?B) ?oldOn <- (on ?A ?C)"
				+ " => (assert (on ?A ?B)) (retract ?oldOn ?action)");
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

		// On(X,Y) & Above(Y,Z) -> Above(X,Z)
		bkMap.put("aboveRule2", new BackgroundKnowledge(
				"(on ?A ?B) (above ?B ?C) => (assert (above ?A ?C))", true));

		// Height of individual blocks (for highest calcs)
		bkMap.put("heightRule", new BackgroundKnowledge(
				"(on ?A ?B) (height ?B ?N) => (assert (height ?A (+ ?N 1)))",
				true));

		// Highest rule
		bkMap.put("highestRule", new BackgroundKnowledge(
				"(height ?A ?N) (forall (thing ?B) (height ?B ?M&:(<= ?M ?N)))"
						+ " => (assert (highest ?A))", true));

		return bkMap;
	}

	@Override
	protected String[] initialiseGoalState() {
		if (envParameter_ == null)
			envParameter_ = "onab";

		String[] result = new String[2];
		// On(a,b) goal
		if (envParameter_.equals("onab") || envParameter_.equals("on$A$B")) {
			result[0] = "on$A$B";
			result[1] = "(on " + RelationalArgument.createGoalTerm(0) + " "
					+ RelationalArgument.createGoalTerm(1) + ") (block "
					+ RelationalArgument.createGoalTerm(0) + ") (block "
					+ RelationalArgument.createGoalTerm(1) + ")";
			return result;
		}

		// On(a,b,c) goal
		if (envParameter_.equals("onabc") || envParameter_.equals("on$A$Bon$B$C")) {
			result[0] = "on$A$Bon$B$C";
			result[1] = "(on " + RelationalArgument.createGoalTerm(0) + " "
					+ RelationalArgument.createGoalTerm(1) + ") (on "
					+ RelationalArgument.createGoalTerm(1) + " "
					+ RelationalArgument.createGoalTerm(2) + ") (block "
					+ RelationalArgument.createGoalTerm(0) + ") (block "
					+ RelationalArgument.createGoalTerm(1) + ") (block "
					+ RelationalArgument.createGoalTerm(2) + ")";
			return result;
		}

		// Unstack goal
		if (envParameter_.equals("unstack")) {
			result[0] = "unstack";
			result[1] = "(forall (block ?A) (clear ?A))";
			return result;
		}

		// Stack goal
		if (envParameter_.equals("stack")) {
			result[0] = "stack";
			result[1] = "(floor ?B) (on ?A ?B) (not (on ?C&:(<> ?C ?A) ?B))";
			return result;
		}

		// Clear goal
		if (envParameter_.equals("clearA") || envParameter_.equals("clear$A")) {
			result[0] = "clear$A";
			result[1] = "(clear " + RelationalArgument.createGoalTerm(0)
					+ ") (block " + RelationalArgument.createGoalTerm(0) + ")";
			return result;
		}

		if (envParameter_.equals("highestA")
				|| envParameter_.equals("highest$A")) {
			result[0] = "highest$A";
			result[1] = "(highest " + RelationalArgument.createGoalTerm(0)
					+ ")";
			return result;
		}
		return null;
	}

	@Override
	protected Collection<RelationalPredicate> initialisePredicateTemplates() {
		Collection<RelationalPredicate> predicates = new ArrayList<RelationalPredicate>();

		// On predicate
		String[] structure = new String[2];
		structure[0] = "block";
		structure[1] = "thing";
		predicates.add(new RelationalPredicate("on", structure, false));

		// Clear predicate
		structure = new String[1];
		structure[0] = "thing";
		predicates.add(new RelationalPredicate("clear", structure, false));

		// Above predicate
		structure = new String[2];
		structure[0] = "block";
		structure[1] = "thing";
		predicates.add(new RelationalPredicate("above", structure, false));

		// Height predicate
		structure = new String[2];
		structure[0] = "thing";
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

		typePreds.put("thing", null);
		typePreds.put("block", "thing");
		typePreds.put("floor", "thing");

		return typePreds;
	}

	@Override
	protected Collection<String> initialiseConstantFacts() {
		Collection<String> constants = new ArrayList<String>();
		// The floor is constant.
		constants.add("(floor floor)");
		constants.add("(clear floor)");
		constants.add("(height floor 0)");
		return constants;
	}
}
