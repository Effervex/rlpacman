package blocksWorldMove;

import relationalFramework.BasicRelationalPolicy;
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

		// Slightly obscure rule which stops blocks on the floor moving to the
		// floor again.
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
	protected Map<String, BackgroundKnowledge> initialiseBackgroundKnowledge() {
		Map<String, BackgroundKnowledge> bkMap = new HashMap<String, BackgroundKnowledge>();

		// Block(Y) & !On(?,Y) -> Clear(Y)
		bkMap.put("clearRule", new BackgroundKnowledge(
				"(block ?Y) (not (on ? ?Y)) => (assert (clear ?Y))", true));

		// On(X,Y) -> Above(X,Y)
		bkMap.put("aboveRule1", new BackgroundKnowledge(
				"(on ?X ?Y) => (assert (above ?X ?Y))", true));

		// Floor is always clear
		bkMap.put("floorClear", new BackgroundKnowledge(
				"(floor ?X) => (assert (clear ?X))", true));

		// On(X,Y) & Above(Y,Z) -> Above(X,Z)
		bkMap.put("aboveRule2", new BackgroundKnowledge(
				"(on ?X ?Y) (above ?Y ?Z) => (assert (above ?X ?Z))", true));

		return bkMap;
	}

	@Override
	protected String[] initialiseGoalState() {
		if (envParameter_ == null)
			envParameter_ = "onab";

		String[] result = new String[2];
		// On(a,b) goal
		if (envParameter_.equals("onab")) {
			result[0] = "onAB";
			result[1] = "(on " + StateSpec.createGoalTerm(0) + " "
					+ StateSpec.createGoalTerm(1) + ") (block "
					+ StateSpec.createGoalTerm(0) + ") (block "
					+ StateSpec.createGoalTerm(1) + ")";
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
		if (envParameter_.equals("clearA")) {
			result[0] = "clearA";
			result[1] = "(clear " + StateSpec.createGoalTerm(0) + ") (block "
					+ StateSpec.createGoalTerm(0) + ")";
			return result;
		}

		if (envParameter_.equals("highestA")) {
			result[0] = "highestA";
			result[1] = "(highest " + StateSpec.createGoalTerm(0) + ")";
			return result;
		}
		return null;
	}

	@Override
	protected BasicRelationalPolicy initialiseHandCodedPolicy() {
		// Defining the optimal policy based on the goal
		String[] rules = null;
		if (envParameter_.equals("onab")) {
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
		} else if (envParameter_.equals("clearA")) {
			rules = new String[1];
			rules[0] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?X) (above ?X ?G_0) (floor ?Y) => (move ?X ?Y)";
		} else if (envParameter_.equals("highestA")) {
			rules = new String[2];
			rules[0] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?X) (above ?X ?G_0) (floor ?Y) => (move ?X ?Y)";
			rules[1] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?G_0) (highest ?Y) => (move ?G_0 ?Y)";
		}

		BasicRelationalPolicy optimal = new BasicRelationalPolicy();
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
}
