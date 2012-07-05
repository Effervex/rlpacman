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
	protected RelationalPolicy initialiseHandCodedPolicy() {
		// Defining the optimal policy based on the goal
		String[] rules = null;
		if (envParameter_.equals("onab") || envParameter_.equals("on$A$B")) {
			rules = new String[3];
			rules[0] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?G_0) (clear ?G_1) => (move ?G_0 ?G_1)";
			rules[1] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?A) (above ?A ?G_0) (floor ?B) => (move ?A ?B)";
			rules[2] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?A) (above ?A ?G_1) (floor ?B) => (move ?A ?B)";
		} else if (envParameter_.equals("on$A$Bon$B$C")) {
			rules = new String[5];
			rules[0] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1 ?G_2) "
					+ "(clear ?A) (above ?A ?G_0) (floor ?B) => (move ?A ?B)";
			rules[1] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1 ?G_2) "
					+ "(clear ?A) (above ?A ?G_1) (floor ?B) => (move ?A ?B)";
			rules[2] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1 ?G_2) "
					+ "(clear ?A) (above ?A ?G_2) (floor ?B) => (move ?A ?B)";
			rules[3] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1 ?G_2) "
					+ "(clear ?G_1) (clear ?G_2) => (move ?G_1 ?G_2)";
			rules[4] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1 ?G_2) "
					+ "(clear ?G_0) (clear ?G_1) => (move ?G_0 ?G_1)";
		} else if (envParameter_.equals("stack")) {
			rules = new String[1];
			rules[0] = "(clear ?A) (highest ?B) => (move ?A ?B)";
		} else if (envParameter_.equals("unstack")) {
			rules = new String[1];
			rules[0] = "(highest ?A) (floor ?B) => (move ?A ?B)";
		} else if (envParameter_.equals("clearA")
				|| envParameter_.equals("clear$A")) {
			rules = new String[1];
			rules[0] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?A) (above ?A ?G_0) (floor ?B) => (move ?A ?B)";
		} else if (envParameter_.equals("highestA")
				|| envParameter_.equals("highest$A")) {
			rules = new String[2];
			rules[0] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?A) (above ?A ?G_0) (floor ?B) => (move ?A ?B)";
			rules[1] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?G_0) (highest ?B) => (move ?G_0 ?B)";
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
