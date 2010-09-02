package blocksWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import relationalFramework.BackgroundKnowledge;
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
	protected Map<String, BackgroundKnowledge> initialiseBackgroundKnowledge() {
		Map<String, BackgroundKnowledge> bkMap = new HashMap<String, BackgroundKnowledge>();

		// Block(Y) & !On(?,Y) -> Clear(Y)
		bkMap.put("clearRule", new BackgroundKnowledge(
				"(block ?Y) (not (on ? ?Y)) => (assert (clear ?Y))", true));

		// Block(X) & !On(X,?) -> OnFloor(X)
		bkMap.put("onFloorRule1", new BackgroundKnowledge(
				"(block ?X) (not (on ?X ?)) => (onFloor ?X)", false));

		// Block(X) & On(X,?) -> !OnFloor(X)
		bkMap.put("onFloorRule2", new BackgroundKnowledge(
				"(block ?X) (on ?X ?) => (not (onFloor ?X))", false));

		// Block(Z) & On(X,Y) -> !On(X,Z)
		bkMap.put("onRule", new BackgroundKnowledge(
				"(block ?Z) (on ?X ?Y) => (not (on ?X ?Z))", false));

		// Highest(X) -> Clear(X)
		bkMap.put("highestRule", new BackgroundKnowledge(
				"(highest ?X) => (clear ?X)", false));

		// On(X,Y) -> Above(X,Y)
		bkMap.put("aboveRule1", new BackgroundKnowledge(
				"(on ?X ?Y) => (assert (above ?X ?Y))", true));

		// On(X,Y) & Above(Y,Z) -> Above(X,Z)
		bkMap.put("aboveRule2", new BackgroundKnowledge(
				"(on ?X ?Y) (above ?Y ?Z) => (assert (above ?X ?Z))", true));

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
			optimal.addRule(new GuidedRule(rules[i]), false, false);

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
