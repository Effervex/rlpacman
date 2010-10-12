package blocksWorld;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import relationalFramework.GuidedRule;
import relationalFramework.Policy;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;
import relationalFramework.agentObservations.BackgroundKnowledge;

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
	protected Collection<StringFact> initialiseActionTemplates() {
		Collection<StringFact> actions = new ArrayList<StringFact>();

		// Move action
		Class[] structure = new Class[2];
		structure[0] = Block.class;
		structure[1] = Block.class;
		actions.add(new StringFact("move", structure));

		// MoveFloor action
		structure = new Class[1];
		structure[0] = Block.class;
		actions.add(new StringFact("moveFloor", structure));

		return actions;
	}

	@Override
	protected Map<String, BackgroundKnowledge> initialiseBackgroundKnowledge() {
		Map<String, BackgroundKnowledge> bkMap = new HashMap<String, BackgroundKnowledge>();

		// Block(Y) & !On(?,Y) -> Clear(Y)
		bkMap.put("clearRule", new BackgroundKnowledge(
				"(block ?Y) (not (on ? ?Y)) => (assert (clear ?Y))", true));

		// OnFloor(X) <-> !On(X,?)
		bkMap.put("onFloorRule1", new BackgroundKnowledge(
				"(onFloor ?X) <=> (not (on ?X ?))", false));

		// On(X,?) <-> !OnFloor(X)
		bkMap.put("onFloorRule2", new BackgroundKnowledge(
				"(on ?X ?) <=> (not (onFloor ?X))", false));

		// Block(Z) & On(X,Y) -> !On(X,Z)
		bkMap.put("onRule", new BackgroundKnowledge(
				"(block ?Z) (on ?X ?Y) => (not (on ?X ?Z))", false));

		// Highest(X) -> Clear(X)
		bkMap.put("highestRule", new BackgroundKnowledge(
				"(highest ?X) => (clear ?X)", false));

		// On(X,Y) -> Above(X,Y)
		bkMap.put("aboveRule1", new BackgroundKnowledge(
				"(on ?X ?Y) => (assert (above ?X ?Y))", true));

		bkMap.put("aboveRule1.5", new BackgroundKnowledge(
				"(on ?X ?) => (above ?X ?)", false));

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

		if (envParameter_.equals("highestA")) {
			constants.add("a");
			return "(highest a)";
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
		} else if (envParameter_.equals("highestA")) {
			rules = new String[2];
			rules[0] = "(clear ?X) (above ?X a) => (moveFloor ?X)";
			rules[1] = "(clear a) (highest ?Y) => (move a ?Y)";
		}

		optimal = new Policy();
		for (int i = 0; i < rules.length; i++)
			optimal.addRule(new GuidedRule(rules[i]), false, false);

		return optimal;
	}

	@Override
	protected Collection<StringFact> initialisePredicateTemplates() {
		Collection<StringFact> predicates = new ArrayList<StringFact>();

		// On predicate
		Class[] structure = new Class[2];
		structure[0] = Block.class;
		structure[1] = Block.class;
		predicates.add(new StringFact("on", structure));

		// OnFloor predicate
		structure = new Class[1];
		structure[0] = Block.class;
		predicates.add(new StringFact("onFloor", structure));

		// Clear predicate
		structure = new Class[1];
		structure[0] = Block.class;
		predicates.add(new StringFact("clear", structure));

		// Above predicate
		structure = new Class[2];
		structure[0] = Block.class;
		structure[1] = Block.class;
		predicates.add(new StringFact("above", structure));

		// Highest predicate
		structure = new Class[1];
		structure[0] = Block.class;
		predicates.add(new StringFact("highest", structure));

		return predicates;
	}

	@Override
	protected Collection<StringFact> initialiseTypePredicateTemplates() {
		Collection<StringFact> typePreds = new ArrayList<StringFact>();

		typePreds.add(new StringFact("block", new Class[]{Block.class}));

		return typePreds;
	}
}
