package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import jess.Fact;
import jess.Rete;

import org.junit.Before;
import org.junit.Test;

import blocksWorld.BlocksWorldStateSpec;

import relationalFramework.Module;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;
import relationalFramework.agentObservations.AgentObservations;
import relationalFramework.agentObservations.BackgroundKnowledge;
import relationalFramework.agentObservations.ConditionBeliefs;

public class AgentObservationsTest {
	private AgentObservations sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
		sut_ = new AgentObservations();
	}

	private void assertBeliefs(ConditionBeliefs cb,
			Collection<String> alwaysTrue, Collection<String> occasionallyTrue,
			Collection<String> neverTrue) {
		assertBeliefs2(alwaysTrue, cb.getAlwaysTrue());
		assertBeliefs2(occasionallyTrue, cb.getOccasionallyTrue());
		assertBeliefs2(neverTrue, cb.getNeverTrue());
	}

	private void assertBeliefs2(Collection<String> beliefs,
			Collection<StringFact> agentBeliefs) {
		for (String fact : beliefs) {
			assertTrue(beliefs + " did not match " + agentBeliefs, agentBeliefs
					.contains(StateSpec.toStringFact(fact)));
		}
		assertEquals(beliefs + " did not match " + agentBeliefs, agentBeliefs
				.size(), beliefs.size());
	}

	private void assertRules(Collection<String[]> equivalencePairs,
			String relation) {
		for (String[] pair : equivalencePairs) {
			BackgroundKnowledge bk = new BackgroundKnowledge(pair[0] + relation
					+ pair[1], false);
			assertTrue(bk.toString() + " not found.", sut_
					.getLearnedBackgroundKnowledge().contains(bk));
			sut_.getLearnedBackgroundKnowledge().remove(bk);
		}
	}

	private void assertBeliefContains(String condition, byte byteHash,
			String alwaysTrue,
			Map<String, Map<Byte, ConditionBeliefs>> negatedCondBeliefs) {
		if (negatedCondBeliefs.containsKey(condition)
				&& negatedCondBeliefs.get(condition).containsKey(byteHash)) {
			ConditionBeliefs cb = negatedCondBeliefs.get(condition).get(
					byteHash);
			assertTrue(alwaysTrue + " not present in " + cb, cb.getAlwaysTrue()
					.contains(StateSpec.toStringFact(alwaysTrue)));
		}
	}

	@Test
	public void testScanState() throws Exception {
		// [e]
		// [b][d]
		// [f][a][c]
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

		sut_.scanState(facts);
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

		// Highest (Only 1 observation so no occasionnals)
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
		// Remove any state spec, and the rules should be empty
		for (BackgroundKnowledge stateSpecBK : BlocksWorldStateSpec
				.getInstance().getBackgroundKnowledgeConditions())
			sut_.getLearnedBackgroundKnowledge().remove(stateSpecBK);
		// assertEquals(sut_.getLearnedBackgroundKnowledge().size(), 0);

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

		sut_.scanState(facts);
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
		// Remove any state spec, and the rules should be empty
		for (BackgroundKnowledge stateSpecBK : BlocksWorldStateSpec
				.getInstance().getBackgroundKnowledgeConditions())
			sut_.getLearnedBackgroundKnowledge().remove(stateSpecBK);
		// assertEquals(sut_.getLearnedBackgroundKnowledge().size(), 0);
	}

	@Test
	public void testMoveBWScanState() throws Exception {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld");
		sut_ = new AgentObservations();

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

		sut_.scanState(facts);
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

		sut_.scanState(facts);
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
	public void testPacManScanState() {
		StateSpec.initInstance("rlPacMan.PacMan");
		sut_ = new AgentObservations();

		// Rete state = StateSpec.getInstance().getRete();
		// state.eval("(assert (dot d))");
		// state.eval("(assert (dot e))");
		// state.eval("(assert (dot c))");
		// state.eval("(assert (thing ))");
		// state.eval("(assert (thing e))");
		// state.eval("(assert (on d a))");
		// state.eval("(assert (on e b))");
		// state.eval("(assert (on b f))");
		// state.eval("(assert (above e b))");
		// state.eval("(assert (above e f))");
		// state.eval("(assert (above b f))");
		// state.eval("(assert (above d a))");
		// state.eval("(assert (onFloor c))");
		// state.eval("(assert (onFloor a))");
		// state.eval("(assert (onFloor f))");
		// state.eval("(assert (block a))");
		// state.eval("(assert (block b))");
		// state.eval("(assert (block c))");
		// state.eval("(assert (block d))");
		// state.eval("(assert (block e))");
		// state.eval("(assert (block f))");
		// Collection<Fact> facts = StateSpec.extractFacts(state);
		//
		// sut_.scanState(facts);
		// Map<String, ConditionBeliefs> condBeliefs =
		// sut_.getConditionBeliefs();
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
		sut_.scanState(facts);

		// Check the negated assertion rules that should always hold
		Map<String, Map<Byte, ConditionBeliefs>> negatedConds = sut_
				.getNegatedConditionBeliefs();
		byte byteHash = 1;
		assertBeliefContains("above", byteHash, "(onFloor ?X)", negatedConds);
		assertBeliefContains("on", byteHash, "(onFloor ?X)", negatedConds);
		assertBeliefContains("above", byteHash = 2, "(clear ?Y)", negatedConds);
		assertBeliefContains("on", byteHash, "(clear ?Y)", negatedConds);
		assertBeliefContains("clear", byteHash = 1, "(above ? ?X)",
				negatedConds);
		assertBeliefContains("clear", byteHash, "(on ? ?X)", negatedConds);
		assertBeliefContains("onFloor", byteHash, "(above ?X ?)", negatedConds);
		assertBeliefContains("onFloor", byteHash, "(on ?X ?)", negatedConds);
	}

	@Test
	public void testGatherActionFacts() throws Exception {
		// [e]
		// [b][d]
		// [f][a][c]
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
		sut_.scanState(facts);

		Collection<StringFact> relevantFacts = sut_.gatherActionFacts(StateSpec
				.toStringFact("(move c e)"), null, false, null);
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(clear e)")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(clear c)")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(block e)")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(block c)")));
		assertTrue(relevantFacts
				.contains(StateSpec.toStringFact("(highest e)")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(on e b)")));
		assertTrue(relevantFacts
				.contains(StateSpec.toStringFact("(above e b)")));
		assertTrue(relevantFacts
				.contains(StateSpec.toStringFact("(above e f)")));
		assertTrue(relevantFacts
				.contains(StateSpec.toStringFact("(onFloor c)")));
		assertEquals(relevantFacts.size(), 9);
		// Testing the action conditions (empty as we have only one action)
		assertTrue(sut_.getSpecialisationConditions("move").isEmpty());

		// A different move action
		relevantFacts = sut_.gatherActionFacts(StateSpec
				.toStringFact("(move d c)"), null, false, null);
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(clear d)")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(clear c)")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(block d)")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(block c)")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(on d a)")));
		assertTrue(relevantFacts
				.contains(StateSpec.toStringFact("(above d a)")));
		assertTrue(relevantFacts
				.contains(StateSpec.toStringFact("(onFloor c)")));
		assertEquals(relevantFacts.size(), 7);
		// Testing the action conditions
		Collection<StringFact> specialisationConditions = sut_
				.getSpecialisationConditions("move");
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(highest ?Y)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(not (highest ?Y))")));
		// These two are equivalent to above
		assertFalse(specialisationConditions.contains(StateSpec
				.toStringFact("(on ?X ?)")));
		assertFalse(specialisationConditions.contains(StateSpec
				.toStringFact("(on ?Y ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(above ?X ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(above ?Y ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(onFloor ?X)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(onFloor ?Y)")));
		assertEquals(specialisationConditions.size(), 6);

		// And another
		relevantFacts = sut_.gatherActionFacts(StateSpec
				.toStringFact("(move e c)"), null, false, null);
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(clear e)")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(clear c)")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(block e)")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(block c)")));
		assertTrue(relevantFacts
				.contains(StateSpec.toStringFact("(highest e)")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(on e b)")));
		assertTrue(relevantFacts
				.contains(StateSpec.toStringFact("(above e b)")));
		assertTrue(relevantFacts
				.contains(StateSpec.toStringFact("(above e f)")));
		assertTrue(relevantFacts
				.contains(StateSpec.toStringFact("(onFloor c)")));
		assertEquals(relevantFacts.size(), 9);
		// Testing the action conditions
		specialisationConditions = sut_.getSpecialisationConditions("move");
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(highest ?X)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(highest ?Y)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(not (highest ?X))")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(not (highest ?Y))")));
		assertFalse(specialisationConditions.contains(StateSpec
				.toStringFact("(on ?X ?)")));
		assertFalse(specialisationConditions.contains(StateSpec
				.toStringFact("(on ?Y ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(above ?X ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(above ?Y ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(onFloor ?X)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(onFloor ?Y)")));
		assertEquals(specialisationConditions.size(), 8);

		// Replacements
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("e", Module.createModuleParameter(0));
		relevantFacts = sut_.gatherActionFacts(StateSpec
				.toStringFact("(move e c)"), null, true, replacements);
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(clear "
				+ Module.createModuleParameter(0) + ")")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(clear c)")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(block "
				+ Module.createModuleParameter(0) + ")")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(block c)")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(highest "
				+ Module.createModuleParameter(0) + ")")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(on "
				+ Module.createModuleParameter(0) + " ?)")));
		assertTrue(relevantFacts.contains(StateSpec.toStringFact("(above "
				+ Module.createModuleParameter(0) + " ?)")));
		assertTrue(relevantFacts
				.contains(StateSpec.toStringFact("(onFloor c)")));
		assertEquals(relevantFacts.size(), 8);
		// Testing the action conditions
		specialisationConditions = sut_.getSpecialisationConditions("move");
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(highest ?X)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(highest ?Y)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(not (highest ?X))")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(not (highest ?Y))")));
		assertFalse(specialisationConditions.contains(StateSpec
				.toStringFact("(on ?X ?)")));
		assertFalse(specialisationConditions.contains(StateSpec
				.toStringFact("(on ?Y ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(above ?X ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(above ?Y ?)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(onFloor ?X)")));
		assertTrue(specialisationConditions.contains(StateSpec
				.toStringFact("(onFloor ?Y)")));
		assertEquals(specialisationConditions.size(), 8);
	}
}
