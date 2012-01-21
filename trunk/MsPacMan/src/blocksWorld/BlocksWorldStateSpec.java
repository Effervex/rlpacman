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

		actionPreconditions.put("move", "(clear ?X) (clear ?Y &:(neq ?X ?Y))");
		actionPreconditions.put("moveFloor", "(clear ?X) (on ?X ?)");

		return actionPreconditions;
	}

	@Override
	protected Collection<RelationalPredicate> initialiseActionTemplates() {
		Collection<RelationalPredicate> actions = new ArrayList<RelationalPredicate>();

		// Move action
		String[] structure = new String[2];
		structure[0] = "block";
		structure[1] = "block";
		actions.add(new RelationalPredicate("move", structure));

		// MoveFloor action
		structure = new String[1];
		structure[0] = "block";
		actions.add(new RelationalPredicate("moveFloor", structure));

		return actions;
	}

	@Override
	protected Collection<String> initialiseActionRules() {
		Collection<String> actionRules = new ArrayList<String>();
		// Block to block movement
		actionRules.add("?action <- (move ?X ?Y) ?oldOn <- (on ?X ?Z)"
				+ " => (assert (on ?X ?Y)) (retract ?oldOn ?action)");
		// Floor block to block movement
		actionRules.add("?action <- (move ?X ?Y) ?oldOnFl <- (onFloor ?X)"
				+ " => (assert (on ?X ?Y)) (retract ?oldOnFl ?action)");
		// Move floor movement
		actionRules.add("?action <- (moveFloor ?X) ?oldOn <- (on ?X ?Z)"
				+ " => (assert (onFloor ?X)) (retract ?oldOn ?action)");
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

		// Blocks on floor is at height a
		bkMap.put("floorTruthB", new BackgroundKnowledge(
				"(onFloor ?X) => (assert (height ?X 1))", true));

		// On(X,Y) & Above(Y,Z) -> Above(X,Z)
		bkMap.put("aboveRule2", new BackgroundKnowledge(
				"(on ?X ?Y) (above ?Y ?Z) => (assert (above ?X ?Z))", true));

		// Height of individual blocks (for highest calcs)
		bkMap.put("heightRule", new BackgroundKnowledge(
				"(on ?X ?Y) (height ?Y ?N) => (assert (height ?X (+ ?N 1)))",
				true));

		// Highest rule
		bkMap.put("highestRule", new BackgroundKnowledge(
				"(height ?X ?N) (forall (block ?Y) (height ?Y ?M&:(<= ?M ?N)))"
						+ " => (assert (highest ?X))", true));

		return bkMap;
	}

	@Override
	protected String[] initialiseGoalState() {
		String[] result = super.initialiseGoalState();

		// Unstack goal
		if (envParameter_.equals("unstack")) {
			result[0] = "unstack";
			result[1] = "(not (on ?X ?Y))";
			return result;
		}

		// Stack goal
		if (envParameter_.equals("stack")) {
			result[0] = "stack";
			result[1] = "(onFloor ?X) (not (onFloor ?Z&:(<> ?Z ?X)))";
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
					+ "(clear ?X) (above ?X ?G_0) => (moveFloor ?X)";
			rules[2] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?X) (above ?X ?G_1) => (moveFloor ?X)";
		} else if (envParameter_.equals("stack")) {
			rules = new String[1];
			rules[0] = "(clear ?X) (highest ?Y) => (move ?X ?Y)";
		} else if (envParameter_.equals("unstack")) {
			rules = new String[1];
			rules[0] = "(highest ?X) => (moveFloor ?X)";
		} else if (envParameter_.equals("clearA") || envParameter_.equals("clear$A")) {
			rules = new String[1];
			rules[0] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?X) (above ?X ?G_0) => (moveFloor ?X)";
		} else if (envParameter_.equals("highestA") || envParameter_.equals("highest$A")) {
			rules = new String[2];
			rules[0] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?X) (above ?X ?G_0) => (moveFloor ?X)";
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
		structure[1] = "block";
		predicates.add(new RelationalPredicate("on", structure));

		// OnFloor predicate
		structure = new String[1];
		structure[0] = "block";
		predicates.add(new RelationalPredicate("onFloor", structure));

		// Clear predicate
		structure = new String[1];
		structure[0] = "block";
		predicates.add(new RelationalPredicate("clear", structure));

		// Above predicate
		structure = new String[2];
		structure[0] = "block";
		structure[1] = "block";
		predicates.add(new RelationalPredicate("above", structure));

		// Height predicate
		structure = new String[2];
		structure[0] = "block";
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

		typePreds.put("block", null);

		return typePreds;
	}
}
