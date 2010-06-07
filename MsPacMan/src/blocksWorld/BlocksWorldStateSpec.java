package blocksWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import relationalFramework.GuidedRule;
import relationalFramework.MultiMap;
import relationalFramework.Policy;
import relationalFramework.StateSpec;

public class BlocksWorldStateSpec extends StateSpec {

	@Override
	protected Map<String, String> initialiseActionPreconditions() {
		Map<String, String> actionPreconditions = new HashMap<String, String>();

		// Put the pure precondition in, the rest is taken care of...
		actionPreconditions.put("move", "(clear ?X) (clear ?Y &:(neq ?X ?Y))");

		actionPreconditions.put("moveFloor", "(clear ?X) (on ?X ?)");

		return actionPreconditions;
	}

	@Override
	protected int initialiseActionsPerStep() {
		return 1;
	}

	@Override
	protected MultiMap<String, Class> initialiseActionTemplates() {
		MultiMap<String, Class> actions = new MultiMap<String, Class>();

		// Move action
		List<Class> structure = new ArrayList<Class>();
		structure.add(Block.class);
		structure.add(Block.class);
		actions.putCollection("move", structure);

		// MoveFloor action
		structure = new ArrayList<Class>();
		structure.add(Block.class);
		actions.putCollection("moveFloor", structure);

		return actions;
	}

	@Override
	protected Map<String, String> initialiseBackgroundKnowledge() {
		Map<String, String> bkMap = new HashMap<String, String>();

		// Block(Y) & !On(X,Y) -> Clear(Y)
		bkMap.put("clearRule",
				"(block ?Y) (not (on ?X ?Y)) => (assert (clear ?Y))");

		// On(X,Y) -> Above(X,Y)
		bkMap.put("aboveRule1",
				"(on ?X ?Y) => (assert (above ?X ?Y))");

		// On(X,Y) & Above(Y,Z) -> Above(X,Z)
		bkMap.put("aboveRule2",
				"(on ?X ?Y) (above ?Y ?Z) => (assert (above ?X ?Z))");

		return bkMap;
	}

	@Override
	protected String initialiseGoalState(List<String> constants) {
		if (envParameter_ == null)
			envParameter_ = "onab";
		
		// On(a,b) goal
		if (envParameter_.equals("onab")) {
			constants.add("a");
			constants.add("b");
			return "(on a b)";
		}

		// Unstack goal
		if (envParameter_.equals("unstack")) {
			return "(not (on ?X ?Y))";
		}

		// Stack goal
		if (envParameter_.equals("stack")) {
			return "(onFloor ?X) (not (onFloor ?Y &:(<> ?Y ?X)))";
		}

		// Clear goal
		if (envParameter_.equals("clearA")) {
			constants.add("a");
			return "(clear a)";
		}
		
		return null;
	}

	@Override
	protected Policy initialiseOptimalPolicy() {
		Policy optimal = null;

		// Defining the optimal policy based on the goal
		String[] rules = null;
		if (envParameter_.equals("onab")) {
			rules = new String[3];
			rules[0] = "(clear a) (clear b) => (move a b)";
			rules[1] = "(clear ?X) (above ?X a) => (moveFloor ?X)";
			rules[2] = "(clear ?X) (above ?X b) => (moveFloor ?X)";
		} else if (envParameter_.equals("stack")) {
			rules = new String[1];
			rules[0] = "(clear ?X) (highest ?Y) => (move ?X ?Y)";
		} else if (envParameter_.equals("unstack")) {
			rules = new String[1];
			rules[0] = "(highest ?X) => (moveFloor ?X)";
		} else if (envParameter_.equals("clearA")) {
			rules = new String[1];
			rules[0] = "(clear ?X) (above ?X a) => (moveFloor ?X)";
		}

		optimal = new Policy();
		for (int i = 0; i < rules.length; i++)
			optimal.addRule(new GuidedRule(parseRule(rules[i])), false);

		return optimal;
	}

	@Override
	protected MultiMap<String, Class> initialisePredicateTemplates() {
		MultiMap<String, Class> predicates = new MultiMap<String, Class>();

		// On predicate
		List<Class> structure = new ArrayList<Class>();
		structure.add(Block.class);
		structure.add(Block.class);
		predicates.putCollection("on", structure);

		// OnFloor predicate
		structure = new ArrayList<Class>();
		structure.add(Block.class);
		predicates.putCollection("onFloor", structure);

		// Clear predicate
		structure = new ArrayList<Class>();
		structure.add(Block.class);
		predicates.putCollection("clear", structure);

		// Above predicate
		structure = new ArrayList<Class>();
		structure.add(Block.class);
		structure.add(Block.class);
		predicates.putCollection("above", structure);

		// Highest predicate
		structure = new ArrayList<Class>();
		structure.add(Block.class);
		predicates.putCollection("highest", structure);

		return predicates;
	}

	@Override
	protected Map<Class, String> initialiseTypePredicateTemplates() {
		Map<Class, String> typePreds = new HashMap<Class, String>();

		typePreds.put(Block.class, "block");

		return typePreds;
	}
}
