package blocksWorldMove;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
	protected Collection<StringFact> initialiseActionTemplates() {
		Collection<StringFact> actions = new ArrayList<StringFact>();

		// Move action
		String[] structure = new String[2];
		structure[0] = "block";
		structure[1] = "thing";
		actions.add(new StringFact("move", structure));

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
	protected String initialiseGoalState() {
		if (envParameter_ == null)
			envParameter_ = "onab";

		// On(a,b) goal
		if (envParameter_.equals("onab")) {
			return "(on a b)";
//			return "(on ?G1 ?G2) (block ?G1) (block ?G2)";
		}

		// Unstack goal
		if (envParameter_.equals("unstack")) {
			return "(forall (block ?X) (clear ?X))";
		}

		// Stack goal
		if (envParameter_.equals("stack")) {
			return "(floor ?Y) (on ?X ?Y) (not (on ?Z ?Y&:(<> ?Z ?X)))";
		}

		// Clear goal
		if (envParameter_.equals("clearA")) {
			return "(clear a)";
		}

		if (envParameter_.equals("highestA")) {
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
			rules[1] = "(clear ?X) (above ?X a) (floor ?Y) => (move ?X ?Y)";
			rules[2] = "(clear ?X) (above ?X b) (floor ?Y) => (move ?X ?Y)";
		} else if (envParameter_.equals("stack")) {
			rules = new String[1];
			rules[0] = "(clear ?X) (highest ?Y) => (move ?X ?Y)";
		} else if (envParameter_.equals("unstack")) {
			rules = new String[1];
			rules[0] = "(highest ?X) (floor ?Y) => (move ?X ?Y)";
		} else if (envParameter_.equals("clearA")) {
			rules = new String[1];
			rules[0] = "(clear ?X) (above ?X a) (floor ?Y) => (move ?X ?Y)";
		} else if (envParameter_.equals("highestA")) {
			rules = new String[2];
			rules[0] = "(clear ?X) (above ?X a) (floor ?Y) => (move ?X ?Y)";
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
		String[] structure = new String[2];
		structure[0] = "block";
		structure[1] = "thing";
		predicates.add(new StringFact("on", structure));

		// Clear predicate
		structure = new String[1];
		structure[0] = "thing";
		predicates.add(new StringFact("clear", structure));

		// Above predicate
		structure = new String[2];
		structure[0] = "block";
		structure[1] = "thing";
		predicates.add(new StringFact("above", structure));

		// Highest predicate
		structure = new String[1];
		structure[0] = "block";
		predicates.add(new StringFact("highest", structure));

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
