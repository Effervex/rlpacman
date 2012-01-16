package test;

import static org.junit.Assert.*;

import relationalFramework.GoalCondition;
import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import jess.Fact;
import jess.Rete;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.agentObservations.EnvironmentAgentObservations;
import relationalFramework.agentObservations.BackgroundKnowledge;
import relationalFramework.agentObservations.ConditionBeliefs;
import relationalFramework.agentObservations.IntegerArray;
import relationalFramework.agentObservations.LocalAgentObservations;
import util.MultiMap;

public class AgentObservationsTest {
	private LocalAgentObservations sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
		sut_ = new LocalAgentObservations(new GoalCondition("on$A$B"));
	}

	private void assertBeliefs(ConditionBeliefs cb,
			Collection<String> alwaysTrue, Collection<String> occasionallyTrue,
			Collection<String> neverTrue) {
		assertBeliefs2(alwaysTrue, cb.getAlwaysTrue(null));
		assertBeliefs2(occasionallyTrue, cb.getOccasionallyTrue(null));
		assertBeliefs2(neverTrue, cb.getNeverTrue(null));
	}

	private void assertBeliefs2(Collection<String> beliefs,
			Collection<RelationalPredicate> agentBeliefs) {
		for (String fact : beliefs) {
			assertTrue(
					beliefs + " did not match " + agentBeliefs,
					agentBeliefs.contains(StateSpec.toRelationalPredicate(fact)));
		}
		assertEquals(beliefs + " did not match " + agentBeliefs,
				agentBeliefs.size(), beliefs.size());
	}

	private void assertRules(Collection<String[]> equivalencePairs,
			String relation) {
		for (String[] pair : equivalencePairs) {
			BackgroundKnowledge bk = new BackgroundKnowledge(pair[0] + relation
					+ pair[1]);
			assertTrue(bk.toString() + " not found.", sut_.
					.getLearnedBackgroundKnowledge().contains(bk));
			sut_.getLearnedBackgroundKnowledge().remove(bk);
		}
	}

	private void assertBeliefContains(String condition, IntegerArray argState,
			String alwaysTrue,
			Map<String, Map<IntegerArray, ConditionBeliefs>> negatedCondBeliefs) {
		if (negatedCondBeliefs.containsKey(condition)
				&& negatedCondBeliefs.get(condition).containsKey(argState)) {
			ConditionBeliefs cb = negatedCondBeliefs.get(condition).get(
					argState);
			assertTrue(
					alwaysTrue + " not present in " + cb,
					cb.getAlwaysTrue(null).contains(
							StateSpec.toRelationalPredicate(alwaysTrue)));
		}
	}

	@Test
	public void testScanState() throws Exception {
		// [e]
		// [b][d]
		// [f][a][c]
		sut_ = EnvironmentAgentObservations.newInstance("blah");
		Rete state = StateSpec.getInstance().getRete();
		state.eval("(assert (clear d))");
		state.eval("(assert (clear e))");
		state.eval("(assert (clear c))");
		state.eval("(assert (highest e))");
		state.eval("(assert (on d a))");
		state.eval("(assert (on e b))");
		state.eval("(assert (on b f))");
		state.eval("(assert (above e b))");
		state.eval("(assert (above e f))");
		state.eval("(assert (above b f))");
		state.eval("(assert (above d a))");
		state.eval("(assert (onFloor c))");
		state.eval("(assert (onFloor a))");
		state.eval("(assert (onFloor f))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (block f))");
		Collection<Fact> facts = StateSpec.extractFacts(state);

		sut_.scanState(facts, null);
		Map<String, ConditionBeliefs> condBeliefs = sut_.getConditionBeliefs();

		// Clear
		ConditionBeliefs cb = condBeliefs.get("clear");
		Collection<String> alwaysTrue = new ArrayList<String>();
		alwaysTrue.add("(block ?X)");
		alwaysTrue.add("(clear ?X)");
		Collection<String> occasionallyTrue = new ArrayList<String>();
		occasionallyTrue.add("(on ?X ?)");
		occasionallyTrue.add("(above ?X ?)");
		occasionallyTrue.add("(onFloor ?X)");
		occasionallyTrue.add("(highest ?X)");
		Collection<String> neverTrue = new ArrayList<String>();
		neverTrue.add("(on ? ?X)");
		neverTrue.add("(above ? ?X)");
		neverTrue.add("(on ?X ?X)");
		neverTrue.add("(above ?X ?X)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// Above
		cb = condBeliefs.get("above");
		alwaysTrue.clear();
		alwaysTrue.add("(above ?X ?Y)");
		alwaysTrue.add("(above ?X ?)");
		alwaysTrue.add("(above ? ?Y)");
		alwaysTrue.add("(on ?X ?)");
		alwaysTrue.add("(on ? ?Y)");
		alwaysTrue.add("(block ?X)");
		alwaysTrue.add("(block ?Y)");
		occasionallyTrue.clear();
		occasionallyTrue.add("(on ?X ?Y)");
		occasionallyTrue.add("(on ?Y ?)");
		occasionallyTrue.add("(on ? ?X)");
		occasionallyTrue.add("(above ?Y ?)");
		occasionallyTrue.add("(above ? ?X)");
		occasionallyTrue.add("(clear ?X)");
		occasionallyTrue.add("(highest ?X)");
		occasionallyTrue.add("(onFloor ?Y)");
		neverTrue.clear();
		neverTrue.add("(onFloor ?X)");
		neverTrue.add("(clear ?Y)");
		neverTrue.add("(highest ?Y)");
		neverTrue.add("(on ?X ?X)");
		neverTrue.add("(on ?Y ?X)");
		neverTrue.add("(on ?Y ?Y)");
		neverTrue.add("(above ?X ?X)");
		neverTrue.add("(above ?Y ?X)");
		neverTrue.add("(above ?Y ?Y)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// On
		cb = condBeliefs.get("on");
		alwaysTrue.clear();
		alwaysTrue.add("(above ?X ?Y)");
		alwaysTrue.add("(above ?X ?)");
		alwaysTrue.add("(above ? ?Y)");
		alwaysTrue.add("(on ?X ?Y)");
		alwaysTrue.add("(on ?X ?)");
		alwaysTrue.add("(on ? ?Y)");
		alwaysTrue.add("(block ?X)");
		alwaysTrue.add("(block ?Y)");
		occasionallyTrue.clear();
		occasionallyTrue.add("(on ?Y ?)");
		occasionallyTrue.add("(on ? ?X)");
		occasionallyTrue.add("(above ?Y ?)");
		occasionallyTrue.add("(above ? ?X)");
		occasionallyTrue.add("(clear ?X)");
		occasionallyTrue.add("(highest ?X)");
		occasionallyTrue.add("(onFloor ?Y)");
		neverTrue.clear();
		neverTrue.add("(onFloor ?X)");
		neverTrue.add("(clear ?Y)");
		neverTrue.add("(highest ?Y)");
		neverTrue.add("(on ?X ?X)");
		neverTrue.add("(on ?Y ?X)");
		neverTrue.add("(on ?Y ?Y)");
		neverTrue.add("(above ?X ?X)");
		neverTrue.add("(above ?Y ?X)");
		neverTrue.add("(above ?Y ?Y)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// Highest (Only 1 observation so no occasionals)
		cb = condBeliefs.get("highest");
		alwaysTrue.clear();
		alwaysTrue.add("(clear ?X)");
		alwaysTrue.add("(highest ?X)");
		alwaysTrue.add("(on ?X ?)");
		alwaysTrue.add("(above ?X ?)");
		alwaysTrue.add("(block ?X)");
		occasionallyTrue.clear();
		neverTrue.clear();
		neverTrue.add("(on ? ?X)");
		neverTrue.add("(above ? ?X)");
		neverTrue.add("(onFloor ?X)");
		neverTrue.add("(on ?X ?X)");
		neverTrue.add("(above ?X ?X)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// OnFloor
		cb = condBeliefs.get("onFloor");
		alwaysTrue.clear();
		alwaysTrue.add("(onFloor ?X)");
		alwaysTrue.add("(block ?X)");
		occasionallyTrue.clear();
		occasionallyTrue.add("(clear ?X)");
		occasionallyTrue.add("(on ? ?X)");
		occasionallyTrue.add("(above ? ?X)");
		neverTrue.clear();
		neverTrue.add("(on ?X ?)");
		neverTrue.add("(above ?X ?)");
		neverTrue.add("(highest ?X)");
		neverTrue.add("(on ?X ?X)");
		neverTrue.add("(above ?X ?X)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// Testing learned equivalence relations
		Collection<String[]> equivalencePairs = new HashSet<String[]>();
		equivalencePairs.add(new String[] { "(above ?X ?)", "(on ?X ?)" });
		equivalencePairs.add(new String[] { "(above ? ?Y)", "(on ? ?Y)" });
		equivalencePairs
				.add(new String[] { "(onFloor ?X)", "(not (on ?X ?))" });
		equivalencePairs.add(new String[] { "(onFloor ?X)",
				"(not (above ?X ?))" });
		equivalencePairs
				.add(new String[] { "(on ?X ?)", "(not (onFloor ?X))" });
		equivalencePairs.add(new String[] { "(above ?X ?)",
				"(not (onFloor ?X))" });
		equivalencePairs.add(new String[] { "(on ? ?Y)", "(not (clear ?Y))" });
		equivalencePairs
				.add(new String[] { "(above ? ?Y)", "(not (clear ?Y))" });
		equivalencePairs
				.add(new String[] { "(clear ?X)", "(not (above ? ?X))" });
		equivalencePairs.add(new String[] { "(clear ?X)", "(not (on ? ?X))" });
		assertRules(equivalencePairs, " <=> ");

		Collection<String[]> inferencePairs = new HashSet<String[]>();
		inferencePairs.add(new String[] { "(highest ?X)", "(clear ?X)" });
		inferencePairs.add(new String[] { "(highest ?X)", "(on ?X ?)" });
		inferencePairs.add(new String[] { "(highest ?X)", "(above ?X ?)" });
		inferencePairs
				.add(new String[] { "(highest ?X)", "(not (above ? ?X))" });
		inferencePairs.add(new String[] { "(highest ?X)", "(not (on ? ?X))" });
		inferencePairs
				.add(new String[] { "(highest ?X)", "(not (onFloor ?X))" });
		inferencePairs.add(new String[] { "(above ?X ?Y)", "(above ?X ?)" });
		inferencePairs.add(new String[] { "(above ?X ?Y)", "(above ? ?Y)" });
		inferencePairs
				.add(new String[] { "(above ?X ?Y)", "(not (highest ?Y))" });
		inferencePairs.add(new String[] { "(on ?X ?Y)", "(on ?X ?)" });
		inferencePairs.add(new String[] { "(on ?X ?Y)", "(on ? ?Y)" });
		inferencePairs.add(new String[] { "(on ?X ?Y)", "(above ?X ?Y)" });
		inferencePairs.add(new String[] { "(on ?X ?Y)", "(not (highest ?Y))" });
		inferencePairs
				.add(new String[] { "(onFloor ?X)", "(not (highest ?X))" });
		assertRules(inferencePairs, " => ");

		// Cover another different (flat) state
		// [a][b][c][d][e][f]
		state.clear();
		state.eval("(assert (clear a))");
		state.eval("(assert (clear b))");
		state.eval("(assert (clear c))");
		state.eval("(assert (clear d))");
		state.eval("(assert (clear e))");
		state.eval("(assert (clear f))");
		state.eval("(assert (highest a))");
		state.eval("(assert (highest b))");
		state.eval("(assert (highest c))");
		state.eval("(assert (highest d))");
		state.eval("(assert (highest e))");
		state.eval("(assert (highest f))");
		state.eval("(assert (onFloor a))");
		state.eval("(assert (onFloor b))");
		state.eval("(assert (onFloor c))");
		state.eval("(assert (onFloor d))");
		state.eval("(assert (onFloor e))");
		state.eval("(assert (onFloor f))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (block f))");
		facts = StateSpec.extractFacts(state);

		sut_.scanState(facts, null);
		condBeliefs = sut_.getConditionBeliefs();

		// Clear
		cb = condBeliefs.get("clear");
		alwaysTrue.clear();
		alwaysTrue.add("(clear ?X)");
		alwaysTrue.add("(block ?X)");
		occasionallyTrue.clear();
		occasionallyTrue.add("(on ?X ?)");
		occasionallyTrue.add("(above ?X ?)");
		occasionallyTrue.add("(onFloor ?X)");
		occasionallyTrue.add("(highest ?X)");
		neverTrue.clear();
		neverTrue.add("(on ? ?X)");
		neverTrue.add("(above ? ?X)");
		neverTrue.add("(on ?X ?X)");
		neverTrue.add("(above ?X ?X)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// Above
		cb = condBeliefs.get("above");
		alwaysTrue.clear();
		alwaysTrue.add("(above ?X ?Y)");
		alwaysTrue.add("(above ?X ?)");
		alwaysTrue.add("(above ? ?Y)");
		alwaysTrue.add("(on ?X ?)");
		alwaysTrue.add("(on ? ?Y)");
		alwaysTrue.add("(block ?X)");
		alwaysTrue.add("(block ?Y)");
		occasionallyTrue.clear();
		occasionallyTrue.add("(on ?X ?Y)");
		occasionallyTrue.add("(on ?Y ?)");
		occasionallyTrue.add("(on ? ?X)");
		occasionallyTrue.add("(above ?Y ?)");
		occasionallyTrue.add("(above ? ?X)");
		occasionallyTrue.add("(clear ?X)");
		occasionallyTrue.add("(highest ?X)");
		occasionallyTrue.add("(onFloor ?Y)");
		neverTrue.clear();
		neverTrue.add("(onFloor ?X)");
		neverTrue.add("(clear ?Y)");
		neverTrue.add("(highest ?Y)");
		neverTrue.add("(on ?X ?X)");
		neverTrue.add("(on ?Y ?X)");
		neverTrue.add("(on ?Y ?Y)");
		neverTrue.add("(above ?X ?X)");
		neverTrue.add("(above ?Y ?X)");
		neverTrue.add("(above ?Y ?Y)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// On
		cb = condBeliefs.get("on");
		alwaysTrue.clear();
		alwaysTrue.add("(above ?X ?Y)");
		alwaysTrue.add("(above ?X ?)");
		alwaysTrue.add("(above ? ?Y)");
		alwaysTrue.add("(on ?X ?Y)");
		alwaysTrue.add("(on ?X ?)");
		alwaysTrue.add("(on ? ?Y)");
		alwaysTrue.add("(block ?X)");
		alwaysTrue.add("(block ?Y)");
		occasionallyTrue.clear();
		occasionallyTrue.add("(on ?Y ?)");
		occasionallyTrue.add("(on ? ?X)");
		occasionallyTrue.add("(above ?Y ?)");
		occasionallyTrue.add("(above ? ?X)");
		occasionallyTrue.add("(clear ?X)");
		occasionallyTrue.add("(highest ?X)");
		occasionallyTrue.add("(onFloor ?Y)");
		neverTrue.clear();
		neverTrue.add("(onFloor ?X)");
		neverTrue.add("(clear ?Y)");
		neverTrue.add("(highest ?Y)");
		neverTrue.add("(on ?X ?X)");
		neverTrue.add("(on ?Y ?X)");
		neverTrue.add("(on ?Y ?Y)");
		neverTrue.add("(above ?X ?X)");
		neverTrue.add("(above ?Y ?X)");
		neverTrue.add("(above ?Y ?Y)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// Highest (Only 1 observation so no occasionals)
		cb = condBeliefs.get("highest");
		alwaysTrue.clear();
		alwaysTrue.add("(highest ?X)");
		alwaysTrue.add("(clear ?X)");
		alwaysTrue.add("(block ?X)");
		occasionallyTrue.clear();
		occasionallyTrue.add("(on ?X ?)");
		occasionallyTrue.add("(above ?X ?)");
		occasionallyTrue.add("(onFloor ?X)");
		neverTrue.clear();
		neverTrue.add("(on ? ?X)");
		neverTrue.add("(above ? ?X)");
		neverTrue.add("(on ?X ?X)");
		neverTrue.add("(above ?X ?X)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// OnFloor
		cb = condBeliefs.get("onFloor");
		alwaysTrue.clear();
		alwaysTrue.add("(onFloor ?X)");
		alwaysTrue.add("(block ?X)");
		occasionallyTrue.clear();
		occasionallyTrue.add("(clear ?X)");
		occasionallyTrue.add("(on ? ?X)");
		occasionallyTrue.add("(above ? ?X)");
		occasionallyTrue.add("(highest ?X)");
		neverTrue.clear();
		neverTrue.add("(on ?X ?)");
		neverTrue.add("(above ?X ?)");
		neverTrue.add("(on ?X ?X)");
		neverTrue.add("(above ?X ?X)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// Testing learned equivalence relations
		equivalencePairs.clear();
		equivalencePairs.add(new String[] { "(above ?X ?)", "(on ?X ?)" });
		equivalencePairs.add(new String[] { "(above ? ?Y)", "(on ? ?Y)" });
		equivalencePairs
				.add(new String[] { "(onFloor ?X)", "(not (on ?X ?))" });
		equivalencePairs.add(new String[] { "(onFloor ?X)",
				"(not (above ?X ?))" });
		equivalencePairs
				.add(new String[] { "(on ?X ?)", "(not (onFloor ?X))" });
		equivalencePairs.add(new String[] { "(above ?X ?)",
				"(not (onFloor ?X))" });
		equivalencePairs.add(new String[] { "(on ? ?Y)", "(not (clear ?Y))" });
		equivalencePairs
				.add(new String[] { "(above ? ?Y)", "(not (clear ?Y))" });
		equivalencePairs
				.add(new String[] { "(clear ?X)", "(not (above ? ?X))" });
		equivalencePairs.add(new String[] { "(clear ?X)", "(not (on ? ?X))" });
		assertRules(equivalencePairs, " <=> ");

		inferencePairs.clear();
		inferencePairs.add(new String[] { "(highest ?X)", "(clear ?X)" });
		inferencePairs
				.add(new String[] { "(highest ?X)", "(not (above ? ?X))" });
		inferencePairs.add(new String[] { "(highest ?X)", "(not (on ? ?X))" });
		inferencePairs.add(new String[] { "(above ?X ?Y)", "(above ?X ?)" });
		inferencePairs.add(new String[] { "(above ?X ?Y)", "(above ? ?Y)" });
		inferencePairs
				.add(new String[] { "(above ?X ?Y)", "(not (highest ?Y))" });
		inferencePairs.add(new String[] { "(on ?X ?Y)", "(on ?X ?)" });
		inferencePairs.add(new String[] { "(on ?X ?Y)", "(on ? ?Y)" });
		inferencePairs.add(new String[] { "(on ?X ?Y)", "(above ?X ?Y)" });
		inferencePairs.add(new String[] { "(on ?X ?Y)", "(not (highest ?Y))" });
		assertRules(inferencePairs, " => ");
	}

	@Test
	public void testScanStateWithGoal() throws Exception {
		// [e]
		// [b][d]
		// [f][a][c]
		sut_ = EnvironmentAgentObservations.newInstance("blah");
		Rete state = StateSpec.getInstance().getRete();
		state.eval("(assert (clear d))");
		state.eval("(assert (clear e))");
		state.eval("(assert (clear c))");
		state.eval("(assert (highest e))");
		state.eval("(assert (on d a))");
		state.eval("(assert (on e b))");
		state.eval("(assert (on b f))");
		state.eval("(assert (above e b))");
		state.eval("(assert (above e f))");
		state.eval("(assert (above b f))");
		state.eval("(assert (above d a))");
		state.eval("(assert (onFloor c))");
		state.eval("(assert (onFloor a))");
		state.eval("(assert (onFloor f))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (block f))");
		Collection<Fact> facts = StateSpec.extractFacts(state);

		Map<String, String> goalReplacements = new HashMap<String, String>();
		goalReplacements.put("a", RelationalArgument.createGoalTerm(0));
		goalReplacements.put("b", RelationalArgument.createGoalTerm(1));
		sut_.scanState(facts, goalReplacements);
		MultiMap<String, RelationalPredicate> goalPredicates = sut_
				.getGoalPredicateMap();
		Collection<RelationalPredicate> goalTermPreds = goalPredicates
				.get(RelationalArgument.createGoalTerm(0));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(on ? ?G_0)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(above ? ?G_0)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(onFloor ?G_0)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(block ?G_0)")));
		// Negated facts too
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(not (on ? ?G_0))")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(not (above ? ?G_0))")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(not (onFloor ?G_0))")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(not (block ?G_0))")));
		assertEquals(goalTermPreds.size(), 8);

		goalTermPreds = goalPredicates.get(RelationalArgument.createGoalTerm(1));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(on ? ?G_1)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(on ?G_1 ?)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(above ? ?G_1)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(above ?G_1 ?)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(block ?G_1)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(not (on ? ?G_1))")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(not (on ?G_1 ?))")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(not (above ? ?G_1))")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(not (above ?G_1 ?))")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(not (block ?G_1))")));
		assertEquals(goalTermPreds.size(), 10);

		// Different goal terms
		goalReplacements.clear();
		goalReplacements.put("e", RelationalArgument.createGoalTerm(0));
		goalReplacements.put("c", RelationalArgument.createGoalTerm(1));
		sut_.scanState(facts, goalReplacements);
		goalPredicates = sut_.getGoalPredicateMap();
		goalTermPreds = goalPredicates.get(RelationalArgument.createGoalTerm(0));
		// OLD ONES
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(on ? ?G_0)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(above ? ?G_0)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(onFloor ?G_0)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(block ?G_0)")));
		// NEW ONES
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(on ?G_0 ?)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(above ?G_0 ?)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(clear ?G_0)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(highest ?G_0)")));
		// Include negated too
		assertEquals(goalTermPreds.size(), 16);

		goalTermPreds = goalPredicates.get(RelationalArgument.createGoalTerm(1));
		// OLD ONES
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(on ? ?G_1)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(on ?G_1 ?)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(above ? ?G_1)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(above ?G_1 ?)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(block ?G_1)")));
		// NEW ONES
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(onFloor ?G_1)")));
		assertTrue(goalTermPreds.contains(StateSpec
				.toRelationalPredicate("(clear ?G_1)")));
		// Include negated too
		assertEquals(goalTermPreds.size(), 14);
	}

	@Test
	public void testMoveBWScanState() throws Exception {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld");
		sut_ = EnvironmentAgentObservations.newInstance("blah");

		// [e]
		// [b][d]
		// [f][a][c]
		Rete state = StateSpec.getInstance().getRete();
		state.eval("(assert (floor floor))");
		state.eval("(assert (highest e))");
		state.eval("(assert (on d a))");
		state.eval("(assert (on e b))");
		state.eval("(assert (on b f))");
		state.eval("(assert (on c floor))");
		state.eval("(assert (on a floor))");
		state.eval("(assert (on f floor))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (block f))");
		state.run();
		Collection<Fact> facts = StateSpec.extractFacts(state);

		sut_.scanState(facts, null);
		Map<String, ConditionBeliefs> condBeliefs = sut_.getConditionBeliefs();

		// Clear
		ConditionBeliefs cb = condBeliefs.get("clear");
		Collection<String> alwaysTrue = new ArrayList<String>();
		alwaysTrue.add("(clear ?X)");
		alwaysTrue.add("(thing ?X)");
		Collection<String> occasionallyTrue = new ArrayList<String>();
		occasionallyTrue.add("(on ?X ?)");
		occasionallyTrue.add("(above ?X ?)");
		occasionallyTrue.add("(highest ?X)");
		occasionallyTrue.add("(on ? ?X)");
		occasionallyTrue.add("(above ? ?X)");
		occasionallyTrue.add("(block ?X)");
		occasionallyTrue.add("(floor ?X)");
		Collection<String> neverTrue = new ArrayList<String>();
		neverTrue.add("(on ?X ?X)");
		neverTrue.add("(above ?X ?X)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// Above
		cb = condBeliefs.get("above");
		alwaysTrue.clear();
		alwaysTrue.add("(above ?X ?Y)");
		alwaysTrue.add("(above ?X ?)");
		alwaysTrue.add("(above ? ?Y)");
		alwaysTrue.add("(on ?X ?)");
		alwaysTrue.add("(on ? ?Y)");
		alwaysTrue.add("(block ?X)");
		alwaysTrue.add("(thing ?X)");
		alwaysTrue.add("(thing ?Y)");
		occasionallyTrue.clear();
		occasionallyTrue.add("(on ?X ?Y)");
		occasionallyTrue.add("(on ?Y ?)");
		occasionallyTrue.add("(on ? ?X)");
		occasionallyTrue.add("(above ?Y ?)");
		occasionallyTrue.add("(above ? ?X)");
		occasionallyTrue.add("(clear ?X)");
		occasionallyTrue.add("(highest ?X)");
		occasionallyTrue.add("(clear ?Y)");
		occasionallyTrue.add("(block ?Y)");
		occasionallyTrue.add("(floor ?Y)");
		neverTrue.clear();
		neverTrue.add("(highest ?Y)");
		neverTrue.add("(on ?X ?X)");
		neverTrue.add("(on ?Y ?X)");
		neverTrue.add("(on ?Y ?Y)");
		neverTrue.add("(above ?X ?X)");
		neverTrue.add("(above ?Y ?X)");
		neverTrue.add("(above ?Y ?Y)");
		cb.toString();
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// On
		cb = condBeliefs.get("on");
		alwaysTrue.clear();
		alwaysTrue.add("(above ?X ?Y)");
		alwaysTrue.add("(above ?X ?)");
		alwaysTrue.add("(above ? ?Y)");
		alwaysTrue.add("(on ?X ?Y)");
		alwaysTrue.add("(on ?X ?)");
		alwaysTrue.add("(on ? ?Y)");
		alwaysTrue.add("(block ?X)");
		alwaysTrue.add("(thing ?X)");
		alwaysTrue.add("(thing ?Y)");
		occasionallyTrue.clear();
		occasionallyTrue.add("(on ?Y ?)");
		occasionallyTrue.add("(on ? ?X)");
		occasionallyTrue.add("(above ?Y ?)");
		occasionallyTrue.add("(above ? ?X)");
		occasionallyTrue.add("(clear ?X)");
		occasionallyTrue.add("(highest ?X)");
		occasionallyTrue.add("(clear ?Y)");
		occasionallyTrue.add("(block ?Y)");
		occasionallyTrue.add("(floor ?Y)");
		neverTrue.clear();
		neverTrue.add("(highest ?Y)");
		neverTrue.add("(on ?X ?X)");
		neverTrue.add("(on ?Y ?X)");
		neverTrue.add("(on ?Y ?Y)");
		neverTrue.add("(above ?X ?X)");
		neverTrue.add("(above ?Y ?X)");
		neverTrue.add("(above ?Y ?Y)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// Highest (Only 1 observation so no occasionnals)
		cb = condBeliefs.get("highest");
		alwaysTrue.clear();
		alwaysTrue.add("(highest ?X)");
		alwaysTrue.add("(clear ?X)");
		alwaysTrue.add("(on ?X ?)");
		alwaysTrue.add("(above ?X ?)");
		alwaysTrue.add("(block ?X)");
		alwaysTrue.add("(thing ?X)");
		occasionallyTrue.clear();
		neverTrue.clear();
		neverTrue.add("(on ? ?X)");
		neverTrue.add("(above ? ?X)");
		neverTrue.add("(on ?X ?X)");
		neverTrue.add("(above ?X ?X)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// Block
		cb = condBeliefs.get("block");
		alwaysTrue.clear();
		alwaysTrue.add("(block ?X)");
		alwaysTrue.add("(above ?X ?)");
		alwaysTrue.add("(on ?X ?)");
		alwaysTrue.add("(thing ?X)");
		occasionallyTrue.clear();
		occasionallyTrue.add("(above ? ?X)");
		occasionallyTrue.add("(on ? ?X)");
		occasionallyTrue.add("(clear ?X)");
		occasionallyTrue.add("(highest ?X)");
		neverTrue.clear();
		neverTrue.add("(on ?X ?X)");
		neverTrue.add("(above ?X ?X)");
		neverTrue.add("(floor ?X)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// Floor
		cb = condBeliefs.get("floor");
		alwaysTrue.clear();
		alwaysTrue.add("(floor ?X)");
		alwaysTrue.add("(above ? ?X)");
		alwaysTrue.add("(on ? ?X)");
		alwaysTrue.add("(clear ?X)");
		alwaysTrue.add("(thing ?X)");
		occasionallyTrue.clear();
		neverTrue.clear();
		neverTrue.add("(block ?X)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// Thing
		cb = condBeliefs.get("thing");
		alwaysTrue.clear();
		alwaysTrue.add("(thing ?X)");
		occasionallyTrue.clear();
		occasionallyTrue.add("(above ?X ?)");
		occasionallyTrue.add("(above ? ?X)");
		occasionallyTrue.add("(on ?X ?)");
		occasionallyTrue.add("(on ? ?X)");
		occasionallyTrue.add("(clear ?X)");
		occasionallyTrue.add("(highest ?X)");
		occasionallyTrue.add("(floor ?X)");
		occasionallyTrue.add("(block ?X)");
		neverTrue.clear();
		neverTrue.add("(on ?X ?X)");
		neverTrue.add("(above ?X ?X)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// // Testing learned equivalence relations
		// Collection<String[]> equivalencePairs = new HashSet<String[]>();
		// equivalencePairs.add(new String[] { "(above ?X ?)", "(on ?X ?)" });
		// equivalencePairs.add(new String[] { "(above ? ?Y)", "(on ? ?Y)" });
		// equivalencePairs
		// .add(new String[] { "(onFloor ?X)", "(not (on ?X ?))" });
		// equivalencePairs.add(new String[] { "(onFloor ?X)",
		// "(not (above ?X ?))" });
		// equivalencePairs
		// .add(new String[] { "(on ?X ?)", "(not (onFloor ?X))" });
		// equivalencePairs.add(new String[] { "(above ?X ?)",
		// "(not (onFloor ?X))" });
		// equivalencePairs.add(new String[] { "(on ? ?Y)", "(not (clear ?Y))"
		// });
		// equivalencePairs
		// .add(new String[] { "(above ? ?Y)", "(not (clear ?Y))" });
		// equivalencePairs
		// .add(new String[] { "(clear ?X)", "(not (above ? ?X))" });
		// equivalencePairs.add(new String[] { "(clear ?X)", "(not (on ? ?X))"
		// });
		// assertRules(equivalencePairs, " <=> ");
		//
		// Collection<String[]> inferencePairs = new HashSet<String[]>();
		// inferencePairs.add(new String[] { "(highest ?X)", "(clear ?X)" });
		// inferencePairs.add(new String[] { "(highest ?X)", "(on ?X ?)" });
		// inferencePairs.add(new String[] { "(highest ?X)", "(above ?X ?)" });
		// inferencePairs
		// .add(new String[] { "(highest ?X)", "(not (above ? ?X))" });
		// inferencePairs.add(new String[] { "(highest ?X)", "(not (on ? ?X))"
		// });
		// inferencePairs
		// .add(new String[] { "(highest ?X)", "(not (onFloor ?X))" });
		// inferencePairs.add(new String[] { "(above ?X ?Y)", "(above ?X ?)" });
		// inferencePairs.add(new String[] { "(above ?X ?Y)", "(above ? ?Y)" });
		// inferencePairs
		// .add(new String[] { "(above ?X ?Y)", "(not (highest ?Y))" });
		// inferencePairs.add(new String[] { "(on ?X ?Y)", "(on ?X ?)" });
		// inferencePairs.add(new String[] { "(on ?X ?Y)", "(on ? ?Y)" });
		// inferencePairs.add(new String[] { "(on ?X ?Y)", "(above ?X ?Y)" });
		// inferencePairs.add(new String[] { "(on ?X ?Y)", "(not (highest ?Y))"
		// });
		// inferencePairs
		// .add(new String[] { "(onFloor ?X)", "(not (highest ?X))" });
		// assertRules(inferencePairs, " => ");
		// // Remove any state spec, and the rules should be empty
		// for (BackgroundKnowledge stateSpecBK : BlocksWorldStateSpec
		// .getInstance().getBackgroundKnowledgeConditions())
		// sut_.getLearnedBackgroundKnowledge().remove(stateSpecBK);
		// assertEquals(sut_.getLearnedBackgroundKnowledge().size(), 0);

		// Cover another different (flat) state
		// [a][b][c][d][e][f]
		state.clear();
		state.eval("(assert (floor a))");
		state.eval("(assert (clear a))");
		state.eval("(assert (clear b))");
		state.eval("(assert (clear c))");
		state.eval("(assert (clear d))");
		state.eval("(assert (clear e))");
		state.eval("(assert (clear f))");
		state.eval("(assert (highest a))");
		state.eval("(assert (highest b))");
		state.eval("(assert (highest c))");
		state.eval("(assert (highest d))");
		state.eval("(assert (highest e))");
		state.eval("(assert (highest f))");
		state.eval("(assert (on a floor))");
		state.eval("(assert (on b floor))");
		state.eval("(assert (on c floor))");
		state.eval("(assert (on d floor))");
		state.eval("(assert (on e floor))");
		state.eval("(assert (on f floor))");
		state.eval("(assert (above a floor))");
		state.eval("(assert (above b floor))");
		state.eval("(assert (above c floor))");
		state.eval("(assert (above d floor))");
		state.eval("(assert (above e floor))");
		state.eval("(assert (above f floor))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (block f))");
		facts = StateSpec.extractFacts(state);

		sut_.scanState(facts, null);
		condBeliefs = sut_.getConditionBeliefs();

		/*
		 * // Clear cb = condBeliefs.get("clear"); alwaysTrue.clear();
		 * occasionallyTrue.clear(); occasionallyTrue.add("(on ?X ?)");
		 * occasionallyTrue.add("(above ?X ?)");
		 * occasionallyTrue.add("(onFloor ?X)");
		 * occasionallyTrue.add("(highest ?X)"); neverTrue.clear();
		 * neverTrue.add("(on ? ?X)"); neverTrue.add("(above ? ?X)");
		 * assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);
		 * 
		 * // Above cb = condBeliefs.get("above"); alwaysTrue.clear();
		 * alwaysTrue.add("(above ?X ?)"); alwaysTrue.add("(above ? ?Y)");
		 * alwaysTrue.add("(on ?X ?)"); alwaysTrue.add("(on ? ?Y)");
		 * occasionallyTrue.clear(); occasionallyTrue.add("(on ?X ?Y)");
		 * occasionallyTrue.add("(on ?Y ?)"); occasionallyTrue.add("(on ? ?X)");
		 * occasionallyTrue.add("(above ?Y ?)");
		 * occasionallyTrue.add("(above ? ?X)");
		 * occasionallyTrue.add("(clear ?X)");
		 * occasionallyTrue.add("(highest ?X)");
		 * occasionallyTrue.add("(onFloor ?Y)"); neverTrue.clear();
		 * neverTrue.add("(onFloor ?X)"); neverTrue.add("(clear ?Y)");
		 * neverTrue.add("(highest ?Y)"); assertBeliefs(cb, alwaysTrue,
		 * occasionallyTrue, neverTrue);
		 * 
		 * // On cb = condBeliefs.get("on"); alwaysTrue.clear();
		 * alwaysTrue.add("(above ?X ?Y)"); alwaysTrue.add("(above ?X ?)");
		 * alwaysTrue.add("(above ? ?Y)"); alwaysTrue.add("(on ?X ?)");
		 * alwaysTrue.add("(on ? ?Y)"); occasionallyTrue.clear();
		 * occasionallyTrue.add("(on ?Y ?)"); occasionallyTrue.add("(on ? ?X)");
		 * occasionallyTrue.add("(above ?Y ?)");
		 * occasionallyTrue.add("(above ? ?X)");
		 * occasionallyTrue.add("(clear ?X)");
		 * occasionallyTrue.add("(highest ?X)");
		 * occasionallyTrue.add("(onFloor ?Y)"); neverTrue.clear();
		 * neverTrue.add("(onFloor ?X)"); neverTrue.add("(clear ?Y)");
		 * neverTrue.add("(highest ?Y)"); assertBeliefs(cb, alwaysTrue,
		 * occasionallyTrue, neverTrue);
		 * 
		 * // Highest (Only 1 observation so no occasionals) cb =
		 * condBeliefs.get("highest"); alwaysTrue.clear();
		 * alwaysTrue.add("(clear ?X)"); occasionallyTrue.clear();
		 * occasionallyTrue.add("(on ?X ?)");
		 * occasionallyTrue.add("(above ?X ?)");
		 * occasionallyTrue.add("(onFloor ?X)"); neverTrue.clear();
		 * neverTrue.add("(on ? ?X)"); neverTrue.add("(above ? ?X)");
		 * assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);
		 * 
		 * // OnFloor cb = condBeliefs.get("onFloor"); alwaysTrue.clear();
		 * occasionallyTrue.clear(); occasionallyTrue.add("(clear ?X)");
		 * occasionallyTrue.add("(on ? ?X)");
		 * occasionallyTrue.add("(above ? ?X)");
		 * occasionallyTrue.add("(highest ?X)"); neverTrue.clear();
		 * neverTrue.add("(on ?X ?)"); neverTrue.add("(above ?X ?)");
		 * assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);
		 */

		// // Testing learned equivalence relations
		// equivalencePairs.clear();
		// equivalencePairs.add(new String[] { "(above ?X ?)", "(on ?X ?)" });
		// equivalencePairs.add(new String[] { "(above ? ?Y)", "(on ? ?Y)" });
		// equivalencePairs
		// .add(new String[] { "(onFloor ?X)", "(not (on ?X ?))" });
		// equivalencePairs.add(new String[] { "(onFloor ?X)",
		// "(not (above ?X ?))" });
		// equivalencePairs
		// .add(new String[] { "(on ?X ?)", "(not (onFloor ?X))" });
		// equivalencePairs.add(new String[] { "(above ?X ?)",
		// "(not (onFloor ?X))" });
		// equivalencePairs.add(new String[] { "(on ? ?Y)", "(not (clear ?Y))"
		// });
		// equivalencePairs
		// .add(new String[] { "(above ? ?Y)", "(not (clear ?Y))" });
		// equivalencePairs
		// .add(new String[] { "(clear ?X)", "(not (above ? ?X))" });
		// equivalencePairs.add(new String[] { "(clear ?X)", "(not (on ? ?X))"
		// });
		// assertRules(equivalencePairs, " <=> ");
		//
		// inferencePairs.clear();
		// inferencePairs.add(new String[] { "(highest ?X)", "(clear ?X)" });
		// inferencePairs
		// .add(new String[] { "(highest ?X)", "(not (above ? ?X))" });
		// inferencePairs.add(new String[] { "(highest ?X)", "(not (on ? ?X))"
		// });
		// inferencePairs.add(new String[] { "(above ?X ?Y)", "(above ?X ?)" });
		// inferencePairs.add(new String[] { "(above ?X ?Y)", "(above ? ?Y)" });
		// inferencePairs
		// .add(new String[] { "(above ?X ?Y)", "(not (highest ?Y))" });
		// inferencePairs.add(new String[] { "(on ?X ?Y)", "(on ?X ?)" });
		// inferencePairs.add(new String[] { "(on ?X ?Y)", "(on ? ?Y)" });
		// inferencePairs.add(new String[] { "(on ?X ?Y)", "(above ?X ?Y)" });
		// inferencePairs.add(new String[] { "(on ?X ?Y)", "(not (highest ?Y))"
		// });
		// assertRules(inferencePairs, " => ");
		// // Remove any state spec, and the rules should be empty
		// for (BackgroundKnowledge stateSpecBK : BlocksWorldStateSpec
		// .getInstance().getBackgroundKnowledgeConditions())
		// sut_.getLearnedBackgroundKnowledge().remove(stateSpecBK);
		// assertEquals(sut_.getLearnedBackgroundKnowledge().size(), 0);
	}

	@Test
	public void testLearnNegatedConditionBeliefs() throws Exception {
		Rete state = StateSpec.getInstance().getRete();
		state.eval("(assert (above e f))");
		state.eval("(assert (clear c))");
		state.eval("(assert (on e b))");
		state.eval("(assert (clear d))");
		state.eval("(assert (clear e))");
		state.eval("(assert (highest e))");
		state.eval("(assert (on b f))");
		state.eval("(assert (on d a))");
		state.eval("(assert (above e b))");
		state.eval("(assert (above b f))");
		state.eval("(assert (above d a))");
		state.eval("(assert (onFloor c))");
		state.eval("(assert (onFloor a))");
		state.eval("(assert (onFloor f))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (block f))");
		Collection<Fact> facts = StateSpec.extractFacts(state);
		sut_.scanState(facts, null);

		// Check the negated assertion rules that should always hold
		Map<String, Map<IntegerArray, ConditionBeliefs>> negatedConds = sut_
				.getNegatedConditionBeliefs();
		IntegerArray argState = new IntegerArray(new int[] { 1, 0 });
		assertBeliefContains("above", argState, "(onFloor ?X)", negatedConds);
		assertBeliefContains("on", argState, "(onFloor ?X)", negatedConds);
		argState = new IntegerArray(new int[] { 0, 1 });
		assertBeliefContains("above", argState, "(clear ?Y)", negatedConds);
		assertBeliefContains("on", argState, "(clear ?Y)", negatedConds);
		argState = new IntegerArray(new int[] { 1 });
		assertBeliefContains("clear", argState, "(above ? ?X)", negatedConds);
		assertBeliefContains("clear", argState, "(on ? ?X)", negatedConds);
		assertBeliefContains("onFloor", argState, "(above ?X ?)", negatedConds);
		assertBeliefContains("onFloor", argState, "(on ?X ?)", negatedConds);
	}

	@Test
	public void testGatherActionFacts() throws Exception {
		// [e]
		// [b][d]
		// [f][a][c]
		sut_ = EnvironmentAgentObservations.newInstance("blah");
		Rete state = StateSpec.getInstance().getRete();
		state.eval("(assert (clear d))");
		state.eval("(assert (clear e))");
		state.eval("(assert (clear c))");
		state.eval("(assert (highest e))");
		state.eval("(assert (on e b))");
		state.eval("(assert (on b f))");
		state.eval("(assert (on d a))");
		state.eval("(assert (above e b))");
		state.eval("(assert (above e f))");
		state.eval("(assert (above b f))");
		state.eval("(assert (above d a))");
		state.eval("(assert (onFloor c))");
		state.eval("(assert (onFloor a))");
		state.eval("(assert (onFloor f))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (block f))");
		Collection<Fact> facts = StateSpec.extractFacts(state);
		sut_.scanState(facts, null);

		Collection<RelationalPredicate> relevantFacts = sut_.gatherActionFacts(
				StateSpec.toRelationalPredicate("(move c e)"), null);
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(clear e)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(clear c)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(block e)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(block c)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(highest e)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(on e b)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(above e b)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(above e f)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(onFloor c)")));
		assertEquals(relevantFacts.size(), 9);
		// Testing the action conditions (empty as we have only one action)
		assertTrue(sut_.getSpecialisationConditions("move").isEmpty());

		// A different move action
		relevantFacts = sut_.gatherActionFacts(
				StateSpec.toRelationalPredicate("(move d c)"), null);
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(clear d)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(clear c)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(block d)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(block c)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(on d a)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(above d a)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(onFloor c)")));
		assertEquals(relevantFacts.size(), 7);
		// Testing the action conditions
		Collection<RelationalPredicate> specialisationConditions = sut_
				.getSpecialisationConditions("move");
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(highest ?Y)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(not (highest ?Y))")));
		// These two are equivalent to above
		assertFalse(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(on ?X ?)")));
		assertFalse(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(on ?Y ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(above ?X ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(above ?Y ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(onFloor ?X)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(onFloor ?Y)")));
		assertEquals(specialisationConditions.size(), 6);

		// And another
		relevantFacts = sut_.gatherActionFacts(
				StateSpec.toRelationalPredicate("(move e c)"), null);
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(clear e)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(clear c)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(block e)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(block c)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(highest e)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(on e b)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(above e b)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(above e f)")));
		assertTrue(relevantFacts.contains(StateSpec
				.toRelationalPredicate("(onFloor c)")));
		assertEquals(relevantFacts.size(), 9);
		// Testing the action conditions
		specialisationConditions = sut_.getSpecialisationConditions("move");
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(highest ?X)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(highest ?Y)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(not (highest ?X))")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(not (highest ?Y))")));
		assertFalse(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(on ?X ?)")));
		assertFalse(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(on ?Y ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(above ?X ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(above ?Y ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(onFloor ?X)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(onFloor ?Y)")));
		assertEquals(specialisationConditions.size(), 8);

		// [e]
		// [b][d]
		// [f][a][c]
		// Goal terms
		Map<String, String> goalReplacements = new HashMap<String, String>();
		goalReplacements.put("f", RelationalArgument.createGoalTerm(0));
		goalReplacements.put("c", RelationalArgument.createGoalTerm(1));
		relevantFacts = sut_
				.gatherActionFacts(
						StateSpec.toRelationalPredicate("(move e c)"),
						goalReplacements);
		specialisationConditions = sut_.getSpecialisationConditions("move");
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(highest ?X)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(highest ?Y)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(not (highest ?X))")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(not (highest ?Y))")));
		assertFalse(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(on ?X ?)")));
		assertFalse(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(on ?Y ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(above ?X ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(above ?X "
						+ RelationalArgument.createGoalTerm(0) + ")")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(above ?Y ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(onFloor ?X)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(onFloor ?Y)")));
		assertEquals(specialisationConditions.size(), 9);

		// [e]
		// [b][d]
		// [f][a][c]
		// Goal terms
		goalReplacements.clear();
		goalReplacements.put("b", RelationalArgument.createGoalTerm(0));
		goalReplacements.put("d", RelationalArgument.createGoalTerm(1));
		relevantFacts = sut_
				.gatherActionFacts(
						StateSpec.toRelationalPredicate("(move e c)"),
						goalReplacements);
		specialisationConditions = sut_.getSpecialisationConditions("move");
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(highest ?X)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(highest ?Y)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(not (highest ?X))")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(not (highest ?Y))")));
		assertFalse(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(on ?X ?)")));
		assertFalse(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(on ?Y ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(above ?X ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(above ?X "
						+ RelationalArgument.createGoalTerm(0) + ")")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(on ?X "
						+ RelationalArgument.createGoalTerm(0)
						+ ")")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(above ?Y ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(onFloor ?X)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toRelationalPredicate("(onFloor ?Y)")));
		assertEquals(specialisationConditions.size(), 10);
	}

	@Test
	public void testLoadedBackgroundRules() {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld");
		assertTrue(EnvironmentAgentObservations.loadAgentObservations("onab"));
		sut_ = EnvironmentAgentObservations.getInstance();

		// Basic test
		Collection<BackgroundKnowledge> backgroundKnowledge = sut_
				.getLearnedBackgroundKnowledge();
		BackgroundKnowledge bk = new BackgroundKnowledge("(highest ?X) => (clear ?X)");
		assertTrue(backgroundKnowledge.contains(bk));
		
		// Basic equivalence
		bk = new BackgroundKnowledge("(floor ?X) <=> (not (block ?X))");
		assertTrue(backgroundKnowledge.contains(bk));
		
		// floor/clear/above rules
		bk = new BackgroundKnowledge("(floor ?X) <=> (clear ?X) (above ? ?X)");
		assertTrue(backgroundKnowledge.contains(bk));
		// Floor supercedes this rule
		bk = new BackgroundKnowledge("(clear ?X) (above ? ?X) <=> (not (block ?X))");
		assertFalse(backgroundKnowledge.contains(bk));
		
		// Goal rule
		bk = new BackgroundKnowledge("(floor ?X) <=> (clear ?X) (above ?Y ?X)");
		assertTrue(backgroundKnowledge.contains(bk));
	}
}
