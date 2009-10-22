package blocksWorld;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mandarax.kernel.ClauseSet;
import org.mandarax.kernel.ConstantTerm;
import org.mandarax.kernel.Fact;
import org.mandarax.kernel.Goal;
import org.mandarax.kernel.KnowledgeBase;
import org.mandarax.kernel.LogicFactory;
import org.mandarax.kernel.Predicate;
import org.mandarax.kernel.Prerequisite;
import org.mandarax.kernel.Rule;
import org.mandarax.kernel.SimplePredicate;
import org.mandarax.kernel.Term;
import org.mandarax.kernel.meta.JPredicate;

import relationalFramework.GuidedPredicate;
import relationalFramework.PredTerm;
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
	protected KnowledgeBase initialiseBackgroundKnowledge(LogicFactory factory) {
		KnowledgeBase backgroundKB = new org.mandarax.reference.KnowledgeBase();

		// On(X,Y) -> Above(X,Y)
		Term[] terms = new Term[2];
		terms[0] = factory.createVariableTerm("X", Block.class);
		terms[1] = factory.createVariableTerm("Y", Block.class);
		List<Prerequisite> prereqs = getGuidedPredicate("on").factify(factory, terms, false, false);
		Term[] terms2 = new Term[3];
		terms2[0] = StateSpec.getStateTerm(factory);
		terms2[1] = terms[0];
		terms2[2] = terms[1];
		Fact fact = factory.createFact(getGuidedPredicate("above").getPredicate(),
				terms2);
		backgroundKB.add(factory.createRule(prereqs, fact));

		// On(X,Y) & Above(Y,Z) -> Above(X,Z)
		terms = new Term[2];
		terms[0] = factory.createVariableTerm("X", Block.class);
		terms[1] = factory.createVariableTerm("Y", Block.class);
		prereqs = getGuidedPredicate("on").factify(factory, terms, false, false);
		terms2 = new Term[2];
		terms2[0] = terms[1];
		terms2[1] = factory.createVariableTerm("Z", Block.class);
		addContains(prereqs, getGuidedPredicate("above").factify(factory,
				terms2, false, false));
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
		String goal = "unstack";

		try {
		// On(a,b) goal
		if (goal.equals("onab")) {
			Predicate goalPred = getGuidedPredicate("on").getPredicate();
			Term[] terms = new Term[3];
			terms[0] = StateSpec.getStateTerm(factory);
			terms[1] = factory.createConstantTerm(new Block("a"), Block.class);
			terms[2] = factory.createConstantTerm(new Block("b"), Block.class);
			prereqs.add(factory.createPrerequisite(goalPred, terms, false));
		}

		// Unstack goal
		if (goal.equals("unstack")) {
			Class[] types = new Class[1];
			types[0] = Object[].class;
			Method method = BlocksWorldStateSpec.class.getMethod("unstacked", types);
			Predicate goalPred = new JPredicate(method);
			Term[] terms = new Term[2];
			terms[0] = StateSpec.getSpecTerm(factory);
			terms[1] = StateSpec.getStateTerm(factory);
			Term[] terms2 = new Term[1];
			terms2[0] = StateSpec.getStateTerm(factory);
			prereqs.add(factory.createPrerequisite(getTypePredicate(types[0]), terms2, false));
			prereqs.add(factory.createPrerequisite(goalPred, terms, false));
		}
		
		// Stack goal
		if (goal.equals("stack")) {
			Class[] types = new Class[1];
			types[0] = Object[].class;
			Method method = BlocksWorldStateSpec.class.getMethod("stacked", types);
			Predicate goalPred = new JPredicate(method);
			Term[] terms = new Term[2];
			terms[0] = StateSpec.getSpecTerm(factory);
			terms[1] = StateSpec.getStateTerm(factory);
			Term[] terms2 = new Term[1];
			terms2[0] = StateSpec.getStateTerm(factory);
			prereqs.add(factory.createPrerequisite(getTypePredicate(types[0]), terms2, false));
			prereqs.add(factory.createPrerequisite(goalPred, terms, false));
		}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return factory.createRule(prereqs, getTerminalFact(factory));
	}

	@Override
	protected List<GuidedPredicate> initialisePredicates() {
		List<GuidedPredicate> predicates = new ArrayList<GuidedPredicate>();

		// On predicate
		Class[] types = { Block.class, Block.class };
		String[] typeNames = { "On", "On'ed" };
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

		try {
			// Highest predicate (requires JPredicate)
			types = new Class[2];
			types[0] = Object[].class;
			types[1] = Block.class;
			PredTerm[][] predValues = new PredTerm[types.length][];
			predValues[0] = createTied("State", types[0]);
			predValues[1] = createTiedAndFree("Highest", types[1]);
			predicates.add(createDefinedPredicate(BlocksWorldStateSpec.class,
					types, predValues, "highest"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//predicates.add(getInequalityPredicate());

		return predicates;
	}

	@Override
	protected Map<Class, Predicate> initialiseTypePredicates() {
		Map<Class, Predicate> typePreds = new HashMap<Class, Predicate>();

		typePreds.put(Block.class, new SimplePredicate("block",
				new Class[] { Block.class }));

		typePreds.put(Object[].class, new SimplePredicate("state",
				new Class[] { Object[].class }));

		return typePreds;
	}

	/**
	 * Creates a simple predicate given some basic predicate information.
	 * 
	 * @param name
	 *            The predicate name.
	 * @param types
	 *            The args in the predicate.
	 * @param typeNames
	 *            The names for the slots of the args.
	 * @return A GuidedPredicate covering the created predicate.
	 */
	private GuidedPredicate createSimplePredicate(String name, Class[] types,
			String[] typeNames, boolean onlyTied) {
		types = insertState(types);
		Predicate predicate = new SimplePredicate(name, types);
		PredTerm[][] predValues = new PredTerm[types.length][];
		predValues[0] = createTied("State", types[0]);
		for (int i = 1; i < predValues.length; i++) {
			if (onlyTied)
				predValues[i] = createTied(typeNames[i - 1], types[i]);
			else
				predValues[i] = createTiedAndFree(typeNames[i - 1], types[i]);
		}
		return new GuidedPredicate(predicate, predValues);
	}

	/**
	 * Method for determining if a block is the highest.
	 * 
	 * @param state
	 *            The state (actually an Integer[])
	 * @param block
	 *            The block being checked.
	 * @return True if the block if the highest.
	 */
	public boolean highest(Object[] state, Block block) {
		Integer highBlock = BlocksWorldState.getHighestBlock(state);
		Integer[] intState = BlocksWorldState.getIntState(state);
		int blockIndex = block.getName().charAt(0) - 'a';

		int blockHeight = 1;
		while (intState[blockIndex] > 0) {
			blockIndex = intState[blockIndex] - 1;
			blockHeight++;
		}

		if (blockHeight == highBlock)
			return true;
		return false;
	}
	
	/**
	 * Goal predicate to check if the blocks are unstacked.
	 * 
	 * @param state The state of the world. Needs to be all 0s.
	 * @return True if the state is unstacked.
	 */
	public boolean unstacked(Object[] state) {
		Integer[] worldState = BlocksWorldState.getIntState(state);
		for (int i = 0; i < worldState.length; i++) {
			if (worldState[i] != 0)
				return false;
		}
		return true;
	}
	
	/**
	 * Goal predicate to check if the blocks are stacked.
	 * 
	 * @param state The state of the world. Only one can be a 0.
	 * @return True if the state is stacked.
	 */
	public boolean stacked(Object[] state) {
		Integer[] worldState = BlocksWorldState.getIntState(state);
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
