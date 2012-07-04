package blocksWorld;

import relationalFramework.NumberEnum;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.agentObservations.BackgroundKnowledge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class BlocksWorldStateSpec extends blocksWorldMove.BlocksWorldStateSpec {

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
	protected RelationalPolicy initialiseHandCodedPolicy() {
		// Defining the optimal policy based on the goal
		String[] rules = null;
		if (envParameter_.equals("onab") || envParameter_.equals("on$A$B")) {
			rules = new String[3];
			rules[0] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?G_0) (clear ?G_1) => (move ?G_0 ?G_1)";
			rules[1] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?A) (above ?A ?G_0) => (moveFloor ?A)";
			rules[2] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?A) (above ?A ?G_1) => (moveFloor ?A)";
		} else if (envParameter_.equals("stack")) {
			rules = new String[1];
			rules[0] = "(clear ?A) (highest ?B) => (move ?A ?B)";
		} else if (envParameter_.equals("unstack")) {
			rules = new String[1];
			rules[0] = "(highest ?A) => (moveFloor ?A)";
		} else if (envParameter_.equals("clearA")
				|| envParameter_.equals("clear$A")) {
			rules = new String[1];
			rules[0] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?A) (above ?A ?G_0) => (moveFloor ?A)";
		} else if (envParameter_.equals("highestA")
				|| envParameter_.equals("highest$A")) {
			rules = new String[2];
			rules[0] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?A) (above ?A ?G_0) => (moveFloor ?A)";
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
