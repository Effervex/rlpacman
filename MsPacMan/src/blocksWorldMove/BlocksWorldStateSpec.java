package blocksWorldMove;

import relationalFramework.NumberEnum;
import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
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
				"(clear ?X) (block ?X) (clear ?Y &:(neq ?X ?Y)) "
						+ "(not (on ?X ?Y))");

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
		actions.add(new RelationalPredicate("move", structure));

		return actions;
	}

	@Override
	protected Collection<String> initialiseActionRules() {
		Collection<String> actionRules = new ArrayList<String>();
		// Block to block movement
		actionRules
				.add("?action <- (move ?X ?Y) ?oldOn <- (on ?X ?Z&:(<> ?Z ?Y))"
						+ " => (assert (on ?X ?Y)) (retract ?oldOn ?action)");
		return actionRules;
	}

	@Override
	protected Map<String, BackgroundKnowledge> initialiseBackgroundKnowledge() {
		Map<String, BackgroundKnowledge> bkMap = new HashMap<String, BackgroundKnowledge>();

		// Block(Y) & !On(?,Y) -> Clear(Y)
		bkMap.put("clearRule", new BackgroundKnowledge(
				"(block ?Y) (not (on ? ?Y)) => (assert (clear ?Y))", true));

		// On(X,Y) -> Above(X,Y)
		bkMap.put("aboveRule1", new BackgroundKnowledge(
				"(on ?X ?Y) => (assert (above ?X ?Y))", true));

		// Floor is always clear
		bkMap.put("floorTruthA", new BackgroundKnowledge(
				"(floor ?X) => (assert (clear ?X))", false));

		// Floor is at height 0
		bkMap.put("floorTruthB", new BackgroundKnowledge(
				"(floor ?X) => (assert (height ?X 0))", false));

		// On(X,Y) & Above(Y,Z) -> Above(X,Z)
		bkMap.put("aboveRule2", new BackgroundKnowledge(
				"(on ?X ?Y) (above ?Y ?Z) => (assert (above ?X ?Z))", true));

		// Height of individual blocks (for highest calcs)
		bkMap.put("heightRule", new BackgroundKnowledge(
				"(on ?X ?Y) (height ?Y ?N) => (assert (height ?X (+ ?N 1)))",
				true));

		// Highest rule
		bkMap.put("highestRule", new BackgroundKnowledge(
				"(height ?X ?N) (forall (thing ?Y) (height ?Y ?M&:(<= ?M ?N)))"
						+ " => (assert (highest ?X))", true));

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

		// Unstack goal
		if (envParameter_.equals("unstack")) {
			result[0] = "unstack";
			result[1] = "(forall (block ?X) (clear ?X))";
			return result;
		}

		// Stack goal
		if (envParameter_.equals("stack")) {
			result[0] = "stack";
			result[1] = "(floor ?Y) (on ?X ?Y) (not (on ?Z ?Y&:(<> ?Z ?X)))";
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
	protected RelationalPolicy initialiseHandCodedPolicy() {
		// Defining the optimal policy based on the goal
		String[] rules = null;
		if (envParameter_.equals("onab") || envParameter_.equals("on$A$B")) {
			rules = new String[3];
			rules[0] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?G_0) (clear ?G_1) => (move ?G_0 ?G_1)";
			rules[1] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?X) (above ?X ?G_0) (floor ?Y) => (move ?X ?Y)";
			rules[2] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?X) (above ?X ?G_1) (floor ?Y) => (move ?X ?Y)";
		} else if (envParameter_.equals("stack")) {
			rules = new String[1];
			rules[0] = "(clear ?X) (highest ?Y) => (move ?X ?Y)";
		} else if (envParameter_.equals("unstack")) {
			rules = new String[1];
			rules[0] = "(highest ?X) (floor ?Y) => (move ?X ?Y)";
		} else if (envParameter_.equals("clearA") || envParameter_.equals("clear$A")) {
			rules = new String[1];
			rules[0] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?X) (above ?X ?G_0) (floor ?Y) => (move ?X ?Y)";
		} else if (envParameter_.equals("highestA") || envParameter_.equals("highest$A")) {
			rules = new String[2];
			rules[0] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?X) (above ?X ?G_0) (floor ?Y) => (move ?X ?Y)";
			rules[1] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?G_0) (highest ?Y) => (move ?G_0 ?Y)";
		}

		RelationalPolicy optimal = new RelationalPolicy();
		for (int i = 0; i < rules.length; i++)
			optimal.addRule(new RelationalRule(rules[i]));

		return optimal;
	}

	@Override
	protected Collection<RelationalPredicate> initialisePredicateTemplates() {
		Collection<RelationalPredicate> predicates = new ArrayList<RelationalPredicate>();

		// On predicate
		String[] structure = new String[2];
		structure[0] = "block";
		structure[1] = "thing";
		predicates.add(new RelationalPredicate("on", structure));

		// Clear predicate
		structure = new String[1];
		structure[0] = "thing";
		predicates.add(new RelationalPredicate("clear", structure));

		// Above predicate
		structure = new String[2];
		structure[0] = "block";
		structure[1] = "thing";
		predicates.add(new RelationalPredicate("above", structure));

		// Height predicate
		structure = new String[2];
		structure[0] = "thing";
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("height", structure));

		// Highest predicate
		structure = new String[1];
		structure[0] = "block";
		predicates.add(new RelationalPredicate("highest", structure));

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
		return constants;
	}
}
