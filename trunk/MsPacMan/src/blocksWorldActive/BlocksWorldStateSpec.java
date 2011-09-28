package blocksWorldActive;

import relationalFramework.BasicRelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BlocksWorldStateSpec extends blocksWorldMove.BlocksWorldStateSpec {

	@Override
	protected Map<String, String> initialiseActionPreconditions() {
		Map<String, String> actionPreconditions = super
				.initialiseActionPreconditions();

		// Activates or deactivates a block
		actionPreconditions.put("toggle", "(block ?X)");

		return actionPreconditions;
	}

	@Override
	protected Collection<RelationalPredicate> initialiseActionTemplates() {
		Collection<RelationalPredicate> actions = super
				.initialiseActionTemplates();

		// Move action
		String[] structure = new String[1];
		structure[0] = "block";
		actions.add(new RelationalPredicate("toggle", structure));

		return actions;
	}

	@Override
	protected String[] initialiseGoalState() {
		if (envParameter_ == null)
			envParameter_ = "onab";

		String[] result = new String[2];
		// On(a,b) goal
		if (envParameter_.equals("onab")) {
			result[0] = "onAB";
			String[] goalBlocks = { StateSpec.createGoalTerm(0),
					StateSpec.createGoalTerm(1) };
			result[1] = "(on " + goalBlocks[0] + " " + goalBlocks[1]
					+ ") (active " + goalBlocks[0] + ") (active "
					+ goalBlocks[1] + ") (not (active ?X&:(<> ?X "
					+ goalBlocks[0] + " " + goalBlocks[1] + ")))";
			return result;
		}

		// Unstack goal
		if (envParameter_.equals("unstack")) {
			result[0] = "unstack";
			result[1] = "(forall (block ?X) (clear ?X) (active ?X))";
			return result;
		}

		// Stack goal
		if (envParameter_.equals("stack")) {
			result[0] = "stack";
			result[1] = "(floor ?Y) (on ?X ?Y) (not (on ?Z ?Y&:(<> ?Z ?X))) "
					+ "(forall (block ?A) (active ?A))";
			return result;
		}

		// Clear goal
		if (envParameter_.equals("clearA")) {
			result[0] = "clearA";
			String goalBlock = StateSpec.createGoalTerm(0);
			result[1] = "(clear " + goalBlock + ") (block " + goalBlock
					+ ") (active " + goalBlock + ") (not (active ?X&:(<> ?X "
					+ goalBlock + ")))";
			return result;
		}

		if (envParameter_.equals("highestA")) {
			result[0] = "highestA";
			String goalBlock = StateSpec.createGoalTerm(0);
			result[1] = "(highest " + goalBlock + ") (active " + goalBlock
					+ ") (not (active ?X&:(<> ?X " + goalBlock + ")))";
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
