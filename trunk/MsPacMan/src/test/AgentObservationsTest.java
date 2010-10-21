package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import jess.Fact;
import jess.Rete;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import relationalFramework.Covering;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;
import relationalFramework.agentObservations.AgentObservations;
import relationalFramework.agentObservations.BackgroundKnowledge;
import relationalFramework.agentObservations.ConditionBeliefs;

public class AgentObservationsTest {
	private AgentObservations sut_;

	@Before
	public void setUp() throws Exception {
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
		sut_ = new AgentObservations();
		StateSpec.initInstance("blocksWorld.BlocksWorld");
	}

	private void assertBeliefs(ConditionBeliefs cb,
			Collection<String> alwaysTrue, Collection<String> occasionallyTrue,
			Collection<String> neverTrue) {
		assertBeliefs2(alwaysTrue, cb.getAlwaysTrue());
		assertBeliefs2(occasionallyTrue, cb.getOccasionallyTrue());
		assertBeliefs2(neverTrue, cb.getNeverTrue());

		// Asserting background knowledge
		StringFact cbFact = StateSpec.getInstance().getStringFact(
				cb.getCondition());
		String[] arguments = new String[cbFact.getArguments().length];
		for (int i = 0; i < arguments.length; i++)
			arguments[i] = Covering.getVariableTermString(i);
		cbFact = new StringFact(cbFact, arguments);

		for (String trues : alwaysTrue) {
			BackgroundKnowledge bk = new BackgroundKnowledge(cbFact + " => "
					+ trues, false);
			assertTrue(sut_.getLearnedBackgroundKnowledge().contains(bk));
		}
		
		for (String falses : neverTrue) {
			BackgroundKnowledge bk = new BackgroundKnowledge(cbFact + " => "
					+ "(not " + falses + ")", false);
			assertTrue(sut_.getLearnedBackgroundKnowledge().contains(bk));
		}
	}

	private void assertBeliefs2(Collection<String> beliefs,
			Collection<StringFact> agentBeliefs) {
		for (String fact : beliefs) {
			assertTrue(agentBeliefs.contains(StateSpec.toStringFact(fact)));
		}
		assertEquals(agentBeliefs.size(), beliefs.size());
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
		Map<String, ConditionBeliefs> condBeliefs = sut_.getConditionBeliefs();

		// Clear
		ConditionBeliefs cb = condBeliefs.get("clear");
		Collection<String> alwaysTrue = new ArrayList<String>();
		Collection<String> occasionallyTrue = new ArrayList<String>();
		occasionallyTrue.add("(on ?X ?)");
		occasionallyTrue.add("(above ?X ?)");
		occasionallyTrue.add("(onFloor ?X)");
		occasionallyTrue.add("(highest ?X)");
		Collection<String> neverTrue = new ArrayList<String>();
		neverTrue.add("(on ? ?X)");
		neverTrue.add("(above ? ?X)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// Above
		cb = condBeliefs.get("above");
		alwaysTrue.clear();
		occasionallyTrue.clear();
		occasionallyTrue.add("(on ?X ?)");
		occasionallyTrue.add("(on ?X ?Y)");
		occasionallyTrue.add("(on ? ?Y)");
		occasionallyTrue.add("(on ?Y ?)");
		occasionallyTrue.add("(on ? ?X)");
		occasionallyTrue.add("(above ?X ?)");
		occasionallyTrue.add("(above ? ?Y)");
		occasionallyTrue.add("(above ?Y ?)");
		occasionallyTrue.add("(above ? ?X)");
		occasionallyTrue.add("(clear ?X)");
		occasionallyTrue.add("(highest ?X)");
		occasionallyTrue.add("(onFloor ?Y)");
		neverTrue.clear();
		neverTrue.add("(onFloor ?X)");
		neverTrue.add("(clear ?Y)");
		neverTrue.add("(highest ?Y)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// On
		cb = condBeliefs.get("on");
		alwaysTrue.clear();
		alwaysTrue.add("(above ?X ?Y)");
		occasionallyTrue.clear();
		occasionallyTrue.add("(on ?Y ?)");
		occasionallyTrue.add("(on ? ?X)");
		occasionallyTrue.add("(above ?X ?)");
		occasionallyTrue.add("(above ? ?Y)");
		occasionallyTrue.add("(above ?Y ?)");
		occasionallyTrue.add("(above ? ?X)");
		occasionallyTrue.add("(clear ?X)");
		occasionallyTrue.add("(highest ?X)");
		occasionallyTrue.add("(onFloor ?Y)");
		neverTrue.clear();
		neverTrue.add("(onFloor ?X)");
		neverTrue.add("(clear ?Y)");
		neverTrue.add("(highest ?Y)");
		neverTrue.add("(on ?X ?)");
		neverTrue.add("(on ? ?Y)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// Highest (Only 1 observation so no occasionals)
		cb = condBeliefs.get("highest");
		alwaysTrue.clear();
		alwaysTrue.add("(clear ?X)");
		alwaysTrue.add("(on ?X ?)");
		alwaysTrue.add("(above ?X ?)");
		occasionallyTrue.clear();
		neverTrue.clear();
		neverTrue.add("(on ? ?X)");
		neverTrue.add("(above ? ?X)");
		neverTrue.add("(onFloor ?X)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// OnFloor
		cb = condBeliefs.get("onFloor");
		alwaysTrue.clear();
		occasionallyTrue.clear();
		occasionallyTrue.add("(clear ?X)");
		occasionallyTrue.add("(on ? ?X)");
		occasionallyTrue.add("(above ? ?X)");
		neverTrue.clear();
		neverTrue.add("(on ?X ?)");
		neverTrue.add("(above ?X ?)");
		neverTrue.add("(highest ?X)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

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
		occasionallyTrue.clear();
		occasionallyTrue.add("(on ?X ?)");
		occasionallyTrue.add("(above ?X ?)");
		occasionallyTrue.add("(onFloor ?X)");
		occasionallyTrue.add("(highest ?X)");
		neverTrue.clear();
		neverTrue.add("(on ? ?X)");
		neverTrue.add("(above ? ?X)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// Above
		cb = condBeliefs.get("above");
		alwaysTrue.clear();
		occasionallyTrue.clear();
		occasionallyTrue.add("(on ?X ?)");
		occasionallyTrue.add("(on ?X ?Y)");
		occasionallyTrue.add("(on ? ?Y)");
		occasionallyTrue.add("(on ?Y ?)");
		occasionallyTrue.add("(on ? ?X)");
		occasionallyTrue.add("(above ?X ?)");
		occasionallyTrue.add("(above ? ?Y)");
		occasionallyTrue.add("(above ?Y ?)");
		occasionallyTrue.add("(above ? ?X)");
		occasionallyTrue.add("(clear ?X)");
		occasionallyTrue.add("(highest ?X)");
		occasionallyTrue.add("(onFloor ?Y)");
		neverTrue.clear();
		neverTrue.add("(onFloor ?X)");
		neverTrue.add("(clear ?Y)");
		neverTrue.add("(highest ?Y)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// On
		cb = condBeliefs.get("on");
		alwaysTrue.clear();
		alwaysTrue.add("(above ?X ?Y)");
		occasionallyTrue.clear();
		occasionallyTrue.add("(on ?Y ?)");
		occasionallyTrue.add("(on ? ?X)");
		occasionallyTrue.add("(above ?X ?)");
		occasionallyTrue.add("(above ? ?Y)");
		occasionallyTrue.add("(above ?Y ?)");
		occasionallyTrue.add("(above ? ?X)");
		occasionallyTrue.add("(clear ?X)");
		occasionallyTrue.add("(highest ?X)");
		occasionallyTrue.add("(onFloor ?Y)");
		neverTrue.clear();
		neverTrue.add("(onFloor ?X)");
		neverTrue.add("(clear ?Y)");
		neverTrue.add("(highest ?Y)");
		neverTrue.add("(on ?X ?)");
		neverTrue.add("(on ? ?Y)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// Highest (Only 1 observation so no occasionals)
		cb = condBeliefs.get("highest");
		alwaysTrue.clear();
		alwaysTrue.add("(clear ?X)");
		occasionallyTrue.clear();
		occasionallyTrue.add("(on ?X ?)");
		occasionallyTrue.add("(above ?X ?)");
		occasionallyTrue.add("(onFloor ?X)");
		neverTrue.clear();
		neverTrue.add("(on ? ?X)");
		neverTrue.add("(above ? ?X)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);

		// OnFloor
		cb = condBeliefs.get("onFloor");
		alwaysTrue.clear();
		occasionallyTrue.clear();
		occasionallyTrue.add("(clear ?X)");
		occasionallyTrue.add("(on ? ?X)");
		occasionallyTrue.add("(above ? ?X)");
		occasionallyTrue.add("(highest ?X)");
		neverTrue.clear();
		neverTrue.add("(on ?X ?)");
		neverTrue.add("(above ?X ?)");
		assertBeliefs(cb, alwaysTrue, occasionallyTrue, neverTrue);
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
				.toStringFact("(move c e)"));
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
		Collection<StringFact> actionConditions = sut_
				.getActionConditions("move");
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(clear ?X)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(clear ?Y)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(highest ?Y)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(on ?Y ?)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(above ?Y ?)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(onFloor ?X)")));
		assertEquals(actionConditions.size(), 6);

		// A different move action
		relevantFacts = sut_.gatherActionFacts(StateSpec
				.toStringFact("(move d c)"));
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
		actionConditions = sut_.getActionConditions("move");
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(clear ?X)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(clear ?Y)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(highest ?Y)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(on ?X ?)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(on ?Y ?)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(above ?X ?)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(above ?Y ?)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(onFloor ?X)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(onFloor ?Y)")));
		assertEquals(actionConditions.size(), 9);

		// And another
		relevantFacts = sut_.gatherActionFacts(StateSpec
				.toStringFact("(move e c)"));
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
		actionConditions = sut_.getActionConditions("move");
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(clear ?X)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(clear ?Y)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(highest ?X)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(highest ?Y)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(on ?X ?)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(on ?Y ?)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(above ?X ?)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(above ?Y ?)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(onFloor ?X)")));
		assertTrue(actionConditions.contains(StateSpec
				.toStringFact("(onFloor ?Y)")));
		assertEquals(actionConditions.size(), 10);
	}
}
