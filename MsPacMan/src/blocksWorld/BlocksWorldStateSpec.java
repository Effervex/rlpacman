package blocksWorld;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mandarax.kernel.Fact;
import org.mandarax.kernel.KnowledgeBase;
import org.mandarax.kernel.LogicFactory;
import org.mandarax.kernel.Predicate;
import org.mandarax.kernel.Prerequisite;
import org.mandarax.kernel.Rule;
import org.mandarax.kernel.Term;
import org.mandarax.kernel.meta.JPredicate;

import relationalFramework.GuidedPredicate;
import relationalFramework.GuidedRule;
import relationalFramework.Policy;
import relationalFramework.PredTerm;
import relationalFramework.State;
import relationalFramework.StateSpec;

public class BlocksWorldStateSpec extends StateSpec {

	@Override
	protected List<GuidedPredicate> initialiseActions() {
		List<GuidedPredicate> actions = new ArrayList<GuidedPredicate>();

		// Move action
		Class[] types = { Block.class, Block.class };
		String[] typeNames = { "MovedBlock", "CoveredBlock" };
		actions.add(createSimplePredicate("move", types, typeNames, true));

		// MoveFloor action
		types = new Class[1];
		types[0] = Block.class;
		typeNames = new String[1];
		typeNames[0] = "MovedBlock";
		actions.add(createSimplePredicate("moveFloor", types, typeNames, true));

		return actions;
	}

	@Override
	protected int initialiseActionsPerStep() {
		return 1;
	}

	@Override
	protected Map<Predicate, Rule> initialiseActionPreconditions(
			List<GuidedPredicate> actions) {
		Map<Predicate, Rule> actionPreconditions = new HashMap<Predicate, Rule>();

		// Run through each action
		for (GuidedPredicate action : actions) {
			Predicate actionPred = action.getPredicate();
			String ruleString = null;
			if (actionPred.getName().equals("move")) {
				ruleString = "clear(<X>) & clear(<Y>) -> move(<X>,<Y>)";
			} else if (actionPred.getName().equals("moveFloor")) {
				ruleString = "clear(<X>) & on(<X>,<Y>) -> moveFloor(<X>)";
			}
			Rule actRule = parseRule(ruleString, null);
			actionPreconditions.put(actionPred, actRule);
		}
		return actionPreconditions;
	}

	@Override
	protected KnowledgeBase initialiseBackgroundKnowledge(LogicFactory factory) {
		KnowledgeBase backgroundKB = new org.mandarax.reference.KnowledgeBase();

		// On(X,Y) -> Above(X,Y)
		Term[] terms = new Term[2];
		terms[0] = factory.createVariableTerm("X", Block.class);
		terms[1] = factory.createVariableTerm("Y", Block.class);
		List<Prerequisite> prereqs = getGuidedPredicate("on").factify(factory,
				terms, false, false, null, null);
		Term[] terms2 = new Term[3];
		terms2[0] = StateSpec.getStateTerm(factory);
		terms2[1] = terms[0];
		terms2[2] = terms[1];
		Fact fact = factory.createFact(getGuidedPredicate("above")
				.getPredicate(), terms2);
		backgroundKB.add(factory.createRule(prereqs, fact));

		// On(X,Y) & Above(Y,Z) -> Above(X,Z)
		terms = new Term[2];
		terms[0] = factory.createVariableTerm("X", Block.class);
		terms[1] = factory.createVariableTerm("Y", Block.class);
		Set<Term> allTerms = new HashSet<Term>();
		allTerms.add(terms[0]);
		prereqs = getGuidedPredicate("on").factify(factory, terms, false,
				false, null, null);
		terms2 = new Term[2];
		terms2[0] = terms[1];
		terms2[1] = factory.createVariableTerm("Z", Block.class);
		addContains(prereqs, getGuidedPredicate("above").factify(factory,
				terms2, false, false, null, null));
		Term[] terms3 = new Term[3];
		terms3[0] = StateSpec.getStateTerm(factory);
		terms3[1] = terms[0];
		terms3[2] = terms2[1];
		fact = factory.createPrerequisite(getGuidedPredicate("above")
				.getPredicate(), terms3, false);
		backgroundKB.add(factory.createRule(prereqs, fact));

		return backgroundKB;
	}

	@Override
	protected Rule initialiseGoalState(LogicFactory factory) {
		List<Prerequisite> prereqs = new ArrayList<Prerequisite>();
		goal_ = "unstack";

		try {
			// On(a,b) goal
			if (goal_.equals("onab")) {
				Predicate goalPred = getGuidedPredicate("on").getPredicate();
				Term[] terms = new Term[3];
				terms[0] = StateSpec.getStateTerm(factory);
				Block aBlock = new Block("a");
				terms[1] = factory.createConstantTerm(aBlock, Block.class);
				addConstant("a", aBlock);
				Block bBlock = new Block("b");
				terms[2] = factory.createConstantTerm(bBlock, Block.class);
				addConstant("b", bBlock);
				prereqs.add(factory.createPrerequisite(goalPred, terms, false));
			}

			// Unstack goal
			if (goal_.equals("unstack")) {
				Class[] types = new Class[1];
				types[0] = State.class;
				Method method = BlocksWorldStateSpec.class.getMethod(
						"unstacked", types);
				Predicate goalPred = new JPredicate(method);
				Term[] terms = new Term[2];
				terms[0] = StateSpec.getSpecTerm(factory);
				terms[1] = StateSpec.getStateTerm(factory);
				Term[] terms2 = new Term[1];
				terms2[0] = StateSpec.getStateTerm(factory);
				prereqs.add(factory.createPrerequisite(
						getTypePredicate(types[0]), terms2, false));
				prereqs.add(factory.createPrerequisite(goalPred, terms, false));
			}

			// Stack goal
			if (goal_.equals("stack")) {
				Class[] types = new Class[1];
				types[0] = State.class;
				Method method = BlocksWorldStateSpec.class.getMethod("stacked",
						types);
				Predicate goalPred = new JPredicate(method);
				Term[] terms = new Term[2];
				terms[0] = StateSpec.getSpecTerm(factory);
				terms[1] = StateSpec.getStateTerm(factory);
				Term[] terms2 = new Term[1];
				terms2[0] = StateSpec.getStateTerm(factory);
				prereqs.add(factory.createPrerequisite(
						getTypePredicate(types[0]), terms2, false));
				prereqs.add(factory.createPrerequisite(goalPred, terms, false));
			}

			// Clear goal
			if (goal_.equals("clearA")) {
				Predicate goalPred = getGuidedPredicate("clear").getPredicate();
				Term[] terms = new Term[2];
				terms[0] = StateSpec.getStateTerm(factory);
				Block aBlock = new Block("a");
				terms[1] = factory.createConstantTerm(aBlock, Block.class);
				addConstant("a", aBlock);
				prereqs.add(factory.createPrerequisite(goalPred, terms, false));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return factory.createRule(prereqs, getTerminalFact(factory));
	}

	@Override
	protected Policy initialiseOptimalPolicy() {
		Policy optimal = null;

		// Defining the optimal policy based on the goal
		String[] rules = null;
		Map<String, Object> constantMap = new HashMap<String, Object>();
		if (goal_.equals("onab")) {
			rules = new String[3];
			rules[0] = "clear([a]) & clear([b]) -> move([a],[b])";
			rules[1] = "clear(<X>) & above(<X>,[a]) -> moveFloor(<X>)";
			rules[2] = "clear(<X>) & above(<X>,[b]) -> moveFloor(<X>)";
			constantMap.put("a", new Block("a"));
			constantMap.put("b", new Block("b"));
		} else if (goal_.equals("stack")) {
			rules = new String[1];
			rules[0] = "clear(<X>) & highest(<Y>) -> move(<X>,<Y>)";
		} else if (goal_.equals("unstack")) {
			rules = new String[1];
			rules[0] = "highest(<X>) -> moveFloor(<X>)";
		} else if (goal_.equals("clearA")) {
			rules = new String[1];
			rules[0] = "clear(<X>) & above(<X>,[a]) -> moveFloor(<X>)";
			constantMap.put("a", new Block("a"));
		}

		optimal = new Policy();
		for (int i = 0; i < rules.length; i++)
			optimal.addRule(new GuidedRule(parseRule(rules[i], constantMap),
					null, null, null));

		return optimal;
	}

	@Override
	protected List<GuidedPredicate> initialisePredicates() {
		List<GuidedPredicate> predicates = new ArrayList<GuidedPredicate>();

		// On predicate
		Class[] types = { Block.class, Block.class };
		String[] typeNames = { "On", "Oned" };
		predicates.add(createSimplePredicate("on", types, typeNames, false));

		// OnFloor predicate
		types = new Class[1];
		types[0] = Block.class;
		typeNames = new String[1];
		typeNames[0] = "OnFloor";
		predicates
				.add(createSimplePredicate("onFloor", types, typeNames, false));

		// Clear predicate
		types = new Class[1];
		types[0] = Block.class;
		typeNames = new String[1];
		typeNames[0] = "Clear";
		predicates.add(createSimplePredicate("clear", types, typeNames, false));

		// Above predicate
		types = new Class[2];
		types[0] = Block.class;
		types[1] = Block.class;
		typeNames = new String[2];
		typeNames[0] = "Above";
		typeNames[1] = "Below";
		predicates.add(createSimplePredicate("above", types, typeNames, false));

		// Highest predicate (requires JPredicate)
		types = new Class[1];
		types[0] = Block.class;
		typeNames = new String[1];
		typeNames[0] = "Highest";
		predicates
				.add(createSimplePredicate("highest", types, typeNames, false));

		return predicates;
	}

	@Override
	protected Map<Class, GuidedPredicate> initialiseTypePredicates() {
		Map<Class, GuidedPredicate> typePreds = new HashMap<Class, GuidedPredicate>();

		typePreds.put(Block.class, createTypeGuidedPredicate("block",
				Block.class));

		typePreds.put(State.class, createTypeGuidedPredicate("state",
				State.class));

		return typePreds;
	}

	/**
	 * Goal predicate to check if the blocks are unstacked.
	 * 
	 * @param state
	 *            The state of the world. Needs to be all 0s.
	 * @return True if the state is unstacked.
	 */
	public boolean unstacked(State state) {
		BlocksWorldState bwState = (BlocksWorldState) state;
		Integer[] worldState = bwState.getIntState();
		for (int i = 0; i < worldState.length; i++) {
			if (worldState[i] != 0)
				return false;
		}
		return true;
	}

	/**
	 * Goal predicate to check if the blocks are stacked.
	 * 
	 * @param state
	 *            The state of the world. Only one can be a 0.
	 * @return True if the state is stacked.
	 */
	public boolean stacked(State state) {
		BlocksWorldState bwState = (BlocksWorldState) state;
		Integer[] worldState = bwState.getIntState();
		boolean oneFound = false;
		for (int i = 0; i < worldState.length; i++) {
			if (worldState[i] == 0) {
				if (oneFound)
					return false;
				oneFound = true;
			}
		}
		return oneFound;
	}
}
