package blocksWorldBounded;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.agentObservations.BackgroundKnowledge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;


public class BlocksWorldStateSpec extends blocksWorldMove.BlocksWorldStateSpec {

	@Override
	protected Map<String, String> initialiseActionPreconditions() {
		Map<String, String> actionPreconditions = super
				.initialiseActionPreconditions();

		// Activates or deactivates a block
		actionPreconditions.put("bind",
				"(on ?X ?Y) (clear ?X) (block ?Y) (not (bound ?X ?Y))");
		actionPreconditions.put("unbind", "(bound ?X ?Y) (clear ?X)");

		return actionPreconditions;
	}

	@Override
	protected Collection<RelationalPredicate> initialiseActionTemplates() {
		Collection<RelationalPredicate> actions = super
				.initialiseActionTemplates();

		// Move action
		String[] structure = new String[2];
		structure[0] = "block";
		structure[1] = "block";
		actions.add(new RelationalPredicate("bind", structure));

		structure = new String[2];
		structure[0] = "block";
		structure[1] = "block";
		actions.add(new RelationalPredicate("unbind", structure));

		return actions;
	}
	
	@Override
	protected Collection<String> initialiseActionRules() {
		Collection<String> actionRules = new ArrayList<String>();
		// Bound block movement
		actionRules.add("?action <- (move ?X ?Y) (bound ?X ?Z)"
				+ " => (assert (move ?Z ?Y)) (retract ?action)");
		// Unbound block to block movement
		actionRules.add("?action <- (move ?X ?Y) ?oldOn <- (on ?X ?Z&:(<> ?Z ?Y)) (not (bound ?X ?Z))"
				+ " => (assert (on ?X ?Y)) (retract ?oldOn ?action)");
		
		// Binding
		actionRules.add("?action <- (bind ?X ?Y)"
				+ " => (assert (bound ?X ?Y)) (retract ?action)");
		
		// Unbinding
		actionRules.add("?action <- (unbind ?X ?Y) ?bound <- (bound ?X ?Y)"
				+ " => (retract ?bound ?action)");
		return actionRules;
	}
	
	@Override
	protected Map<String, BackgroundKnowledge> initialiseBackgroundKnowledge() {
		Map<String, BackgroundKnowledge> bckKnow = super.initialiseBackgroundKnowledge();
		bckKnow.put("bindRule", new BackgroundKnowledge(
				"(bound ?X ?Y) (bound ?Y ?Z) => (assert (bound ?X ?Z))",
				true));
		
		return bckKnow;
	}

	@Override
	protected String[] initialiseGoalState() {
		if (envParameter_ == null)
			envParameter_ = "onab";

		String[] result = new String[2];
		// On(a,b) goal
		if (envParameter_.equals("onab")) {
			result[0] = "on$A$B";
			String[] goalBlocks = { RelationalArgument.createGoalTerm(0),
					RelationalArgument.createGoalTerm(1) };
			result[1] = "(on " + goalBlocks[0] + " " + goalBlocks[1]
					+ ") (bound " + goalBlocks[0] + " " + goalBlocks[1] + ")";
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
			result[1] = "(forall (block ?X) (or (bound ?X ?Y) (bound ?Y ?X)))";
			return result;
		}

		// Clear goal
		if (envParameter_.equals("clearA")) {
			result[0] = "clear$A";
			String goalBlock = RelationalArgument.createGoalTerm(0);
			result[1] = "(clear " + goalBlock + ") (block " + goalBlock
					+ ") (not (bound " + goalBlock + " ?))";
			return result;
		}

		if (envParameter_.equals("highestA")) {
			result[0] = "highest$A";
			String goalBlock = RelationalArgument.createGoalTerm(0);
			result[1] = "(highest " + goalBlock + ") (block " + goalBlock
					+ ") (not (bound " + goalBlock + " ?))";
			return result;
		}
		return null;
	}

	@Override
	protected RelationalPolicy initialiseHandCodedPolicy() {
		// Defining the optimal policy based on the goal
		String[] rules = null;
		if (envParameter_.equals("onab")) {
			rules = new String[4];
			rules[0] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(on ?G_0 ?G_1) => (bind ?G_0 ?G_1)";
			rules[1] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?G_0) (clear ?G_1) => (move ?G_0 ?G_1)";
			rules[2] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?X) (above ?X ?G_0) (floor ?Y) => (move ?X ?Y)";
			rules[3] = "(" + GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?X) (above ?X ?G_1) (floor ?Y) => (move ?X ?Y)";
		} else if (envParameter_.equals("stack")) {
			rules = new String[2];
			rules[0] = "(on ?X ?Y) (clear ?X) (block ?Y) (not (bound ?X ?Y)) => (bind ?X ?Y)";
			rules[1] = "(clear ?X) (highest ?Y) => (move ?X ?Y)";
		} else if (envParameter_.equals("unstack")) {
			rules = new String[2];
			rules[0] = "(bound ?X ?Y) (clear ?X) => (unbind ?X ?Y)";
			rules[1] = "(highest ?X) (floor ?Y) => (move ?X ?Y)";
		} else if (envParameter_.equals("clearA")) {
			rules = new String[2];
			rules[0] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?X) (above ?X ?G_0) (floor ?Y) => (move ?X ?Y)";
			rules[1] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?G_0) (bound ?G_0 ?Y) => (unbind ?X ?Y)";
		} else if (envParameter_.equals("highestA")) {
			rules = new String[3];
			rules[0] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?X) (above ?X ?G_0) (floor ?Y) => (move ?X ?Y)";
			rules[1] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?G_0) (bound ?G_0 ?Y) => (unbind ?X ?Y)";
			rules[2] = "(" + GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?G_0) (highest ?Y) => (move ?G_0 ?Y)";
		}

		RelationalPolicy optimal = new RelationalPolicy();
		for (int i = 0; i < rules.length; i++)
			optimal.addRule(new RelationalRule(rules[i]));

		return optimal;
	}

	@Override
	protected Collection<RelationalPredicate> initialisePredicateTemplates() {
		Collection<RelationalPredicate> predicates = super.initialisePredicateTemplates();

		// Active predicate
		String[] structure = new String[2];
		structure[0] = "block";
		structure[1] = "block";
		predicates.add(new RelationalPredicate("bound", structure));

		return predicates;
	}
}
