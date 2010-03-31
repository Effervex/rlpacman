package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jess.Fact;
import jess.Rete;
import jess.ValueVector;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import relationalFramework.Covering;
import relationalFramework.GuidedRule;
import relationalFramework.MultiMap;
import relationalFramework.StateSpec;

public class CoveringTest {
	private Covering sut_;

	@Before
	public void setUp() throws Exception {
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
		sut_ = new Covering(2);
		StateSpec.initInstance("blocksWorld.BlocksWorld");
	}

	@Test
	public void testCoverState() throws Exception {
		// No constants
		List<String> constants = new ArrayList<String>();
		StateSpec.getInstance().setConstants(constants);

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
		StateSpec.getInstance().insertValidActions(state);

		List<GuidedRule> rules = sut_.coverState(state,
				new MultiMap<String, GuidedRule>(), true);
		assertEquals(rules.size(), 2);
		for (GuidedRule gr : rules) {
			System.out.println(gr);
			if (gr.getAction().contains("moveFloor")) {
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(3, condCount);
				assertFalse(gr.getConditions(false).contains("(highest ?X)"));
				assertTrue(gr.getConditions(false).contains("(clear ?X)"));
				assertTrue(gr.getConditions(false).contains("(on ?X ?)"));
				assertTrue(gr.getConditions(false).contains("(above ?X ?)"));
				assertFalse(gr.getConditions(false).contains("(onFloor ?X)"));
				assertEquals("(moveFloor ?X)", gr.getAction());
			} else {
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(2, condCount);
				assertTrue(gr.getConditions(false).contains("(clear ?X)"));
				assertTrue(gr.getConditions(false).contains("(clear ?Y)"));
				assertFalse(gr.getConditions(false).contains("(on ?X ?)"));
				assertFalse(gr.getConditions(false).contains("(above ?X ?)"));
				assertFalse(gr.getConditions(false).contains("(highest ?X)"));
				assertFalse(gr.getConditions(false).contains("(on ?Y ?)"));
				assertFalse(gr.getConditions(false).contains("(above ?Y ?)"));
				assertFalse(gr.getConditions(false).contains("(highest ?Y)"));
				assertEquals("(move ?X ?Y)", gr.getAction());
			}
		}

		// A flat state with previous rules
		state.reset();
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
		StateSpec.getInstance().insertValidActions(state);

		MultiMap<String, GuidedRule> existingRules = new MultiMap<String, GuidedRule>();
		existingRules.put("move", new GuidedRule(
				"(on ?X ?) (above ?X ?) (clear ?X) (on ?Y ?) "
						+ "(above ?Y ?) (clear ?Y) (test (<> ?X ?Y)) "
						+ "(block ?X) (block ?Y) => (move ?X ?Y)"));
		existingRules.put("moveFloor", new GuidedRule(
				"(on ?X ?) (above ?X ?) (clear ?X) "
						+ "(highest ?X) (block ?X) => (moveFloor ?X)"));

		rules = sut_.coverState(state, existingRules, true);
		assertEquals(rules.size(), 1);
		assertEquals(existingRules.sizeTotal(), 2);
		for (GuidedRule gr : existingRules.values()) {
			System.out.println(gr);
			if (gr.getAction().contains("moveFloor")) {
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(4, condCount);
				assertTrue(gr.getConditions(false).contains("(highest ?X)"));
				assertTrue(gr.getConditions(false).contains("(clear ?X)"));
				assertTrue(gr.getConditions(false).contains("(on ?X ?)"));
				assertTrue(gr.getConditions(false).contains("(above ?X ?)"));
				assertFalse(gr.getConditions(false).contains("(onFloor ?X)"));
				assertEquals("(moveFloor ?X)", gr.getAction());
			} else {
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(2, condCount);
				assertTrue(gr.getConditions(false).contains("(clear ?X)"));
				assertTrue(gr.getConditions(false).contains("(clear ?Y)"));
				assertFalse(gr.getConditions(false).contains("(on ?X ?)"));
				assertFalse(gr.getConditions(false).contains("(above ?X ?)"));
				assertFalse(gr.getConditions(false).contains("(highest ?X)"));
				assertFalse(gr.getConditions(false).contains("(on ?Y ?)"));
				assertFalse(gr.getConditions(false).contains("(above ?Y ?)"));
				assertFalse(gr.getConditions(false).contains("(highest ?Y)"));
				assertEquals("(move ?X ?Y)", gr.getAction());
			}
		}

		// Test the rule refinement flag.
		rules = sut_.coverState(state, new MultiMap<String, GuidedRule>(),
				false);
		assertTrue(rules.isEmpty());

		// Consecutive covering (w/ constants)
		constants.add("a");
		constants.add("b");

		state.reset();
		state.eval("(assert (onFloor c))");
		state.eval("(assert (onFloor e))");
		state.eval("(assert (on a c))");
		state.eval("(assert (on b a))");
		state.eval("(assert (on d b))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (highest d))");
		state.run();
		StateSpec.getInstance().insertValidActions(state);

		rules = sut_
				.coverState(state, new MultiMap<String, GuidedRule>(), true);
		assertEquals(2, rules.size());
		existingRules.clear();
		for (GuidedRule gr : rules) {
			System.out.println(gr.toString());
			if (gr.getAction().contains("moveFloor")) {
				assertTrue(gr.getConditions(false).contains("(highest ?X)"));
				assertTrue(gr.getConditions(false).contains("(clear ?X)"));
				assertTrue(gr.getConditions(false).contains("(on ?X b)"));
				assertTrue(gr.getConditions(false).contains("(above ?X b)"));
				assertTrue(gr.getConditions(false).contains("(above ?X a)"));
				assertTrue(gr.getConditions(false).contains("(above ?X ?)"));
				assertTrue(gr.getConditions(false).contains("(block a)"));
				assertTrue(gr.getConditions(false).contains("(block b)"));
				assertEquals("(moveFloor ?X)", gr.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(8, condCount);
				existingRules.put("moveFloor", gr);
			} else {
				assertTrue(gr.getConditions(false).contains("(clear ?X)"));
				assertTrue(gr.getConditions(false).contains("(clear ?Y)"));
				assertFalse(gr.getConditions(false).contains("(on ?X ?)"));
				assertFalse(gr.getConditions(false).contains("(on ? b)"));
				assertFalse(gr.getConditions(false).contains("(above ? b)"));
				assertFalse(gr.getConditions(false).contains("(above ? a)"));
				assertFalse(gr.getConditions(false).contains("(above ?X ?)"));
				assertFalse(gr.getConditions(false).contains("(highest ?X)"));
				assertFalse(gr.getConditions(false).contains("(on ?Y ?)"));
				assertFalse(gr.getConditions(false).contains("(above ?Y ?)"));
				assertFalse(gr.getConditions(false).contains("(highest ?Y)"));
				assertEquals("(move ?X ?Y)", gr.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(2, condCount);
				existingRules.put("move", gr);
			}
		}

		state.reset();
		state.eval("(assert (onFloor c))");
		state.eval("(assert (onFloor e))");
		state.eval("(assert (onFloor d))");
		state.eval("(assert (on a c))");
		state.eval("(assert (on b a))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (highest b))");
		state.run();
		StateSpec.getInstance().insertValidActions(state);

		rules = sut_.coverState(state, existingRules, true);
		assertEquals(2, rules.size());
		assertEquals(2, existingRules.sizeTotal());
		for (GuidedRule gr : existingRules.values()) {
			System.out.println(gr.toString());
			if (gr.getAction().contains("moveFloor")) {
				assertTrue(gr.getConditions(false).contains("(highest ?X)"));
				assertTrue(gr.getConditions(false).contains("(clear ?X)"));
				assertTrue(gr.getConditions(false).contains("(on ?X ?)"));
				assertFalse(gr.getConditions(false).contains("(on ?X b)"));
				assertFalse(gr.getConditions(false).contains("(above ?X b)"));
				assertTrue(gr.getConditions(false).contains("(above ?X a)"));
				assertTrue(gr.getConditions(false).contains("(above ?X ?)"));
				assertTrue(gr.getConditions(false).contains("(block a)"));
				assertEquals("(moveFloor ?X)", gr.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(6, condCount);
			} else {
				assertTrue(gr.getConditions(false).contains("(clear ?X)"));
				assertTrue(gr.getConditions(false).contains("(clear ?Y)"));
				assertFalse(gr.getConditions(false).contains("(on ?X ?)"));
				assertFalse(gr.getConditions(false).contains("(on ? b)"));
				assertFalse(gr.getConditions(false).contains("(above ? b)"));
				assertFalse(gr.getConditions(false).contains("(above ? a)"));
				assertFalse(gr.getConditions(false).contains("(above ?X ?)"));
				assertFalse(gr.getConditions(false).contains("(highest ?X)"));
				assertFalse(gr.getConditions(false).contains("(on ?Y ?)"));
				assertFalse(gr.getConditions(false).contains("(above ?Y ?)"));
				assertFalse(gr.getConditions(false).contains("(highest ?Y)"));
				assertEquals("(move ?X ?Y)", gr.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(2, condCount);
			}
		}

		// Constants covering
		state.reset();
		state.eval("(assert (onFloor c))");
		state.eval("(assert (onFloor e))");
		state.eval("(assert (on a c))");
		state.eval("(assert (on d a))");
		state.eval("(assert (on b d))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (highest b))");
		state.run();
		StateSpec.getInstance().insertValidActions(state);

		rules = sut_
				.coverState(state, new MultiMap<String, GuidedRule>(), true);
		assertEquals(2, rules.size());
		for (GuidedRule gr : rules) {
			System.out.println(gr.toString());
			if (gr.getAction().contains("moveFloor")) {
				assertTrue(gr.getConditions(false).contains("(highest b)"));
				assertTrue(gr.getConditions(false).contains("(clear b)"));
				assertTrue(gr.getConditions(false).contains("(on b ?)"));
				assertTrue(gr.getConditions(false).contains("(above b a)"));
				assertTrue(gr.getConditions(false).contains("(above b ?)"));
				assertTrue(gr.getConditions(false).contains("(block a)"));
				assertEquals("(moveFloor b)", gr.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(6, condCount);
			} else {
				assertTrue(gr.getConditions(false).contains("(clear ?X)"));
				assertTrue(gr.getConditions(false).contains("(clear ?Y)"));
				assertFalse(gr.getConditions(false).contains("(on ?X ?)"));
				assertFalse(gr.getConditions(false).contains("(above ?X ?)"));
				assertFalse(gr.getConditions(false).contains("(highest ?X)"));
				assertFalse(gr.getConditions(false).contains("(on ?Y ?)"));
				assertFalse(gr.getConditions(false).contains("(above ?Y ?)"));
				assertFalse(gr.getConditions(false).contains("(highest ?Y)"));
				assertEquals("(move ?X ?Y)", gr.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(2, condCount);
			}
		}
	}

	@Test
	public void testInverselySubstitute() throws Exception {
		// Ensuring we have no constants
		List<String> constants = new ArrayList<String>();

		// Basic substitution
		Rete state = StateSpec.getInstance().getRete();
		state.eval("(assert (clear a))");
		Collection<Fact> facts = StateSpec.extractFacts(state);
		String[] actionTerms = { "a" };
		Collection<String> result = sut_.inverselySubstitute(facts,
				actionTerms, constants);
		assertEquals(result.size(), 1);
		assertTrue(result.contains("(clear ?X)"));

		// Substitution of an unknown variable
		actionTerms[0] = "b";
		result = sut_.inverselySubstitute(facts, actionTerms, constants);
		assertEquals(result.size(), 1);
		assertTrue(result.contains("(clear ?)"));

		// Only return one anonymous fact if there is more than one
		state.eval("(assert (clear b))");
		state.eval("(assert (clear c))");
		facts = StateSpec.extractFacts(state);
		actionTerms[0] = "a";
		result = sut_.inverselySubstitute(facts, actionTerms, constants);
		assertEquals(result.size(), 2);
		assertTrue(result.contains("(clear ?X)"));
		assertTrue(result.contains("(clear ?)"));

		// Swap over many facts
		state.eval("(assert (on a d))");
		state.eval("(assert (on b e))");
		state.eval("(assert (onFloor c))");
		facts = StateSpec.extractFacts(state);
		actionTerms[0] = "a";
		result = sut_.inverselySubstitute(facts, actionTerms, constants);
		assertEquals(result.size(), 5);
		assertTrue(result.contains("(clear ?X)"));
		assertTrue(result.contains("(clear ?)"));
		assertTrue(result.contains("(on ?X ?)"));
		assertTrue(result.contains("(on ? ?)"));
		assertTrue(result.contains("(onFloor ?)"));

		// Multiple action terms
		actionTerms = new String[2];
		actionTerms[0] = "a";
		actionTerms[1] = "c";
		result = sut_.inverselySubstitute(facts, actionTerms, constants);
		assertEquals(result.size(), 6);
		assertTrue(result.contains("(clear ?X)"));
		assertTrue(result.contains("(clear ?Y)"));
		assertTrue(result.contains("(clear ?)"));
		assertTrue(result.contains("(on ?X ?)"));
		assertTrue(result.contains("(on ? ?)"));
		assertTrue(result.contains("(onFloor ?Y)"));

		// Testing constants
		constants.add("a");
		actionTerms = new String[1];
		actionTerms[0] = "a";
		result = sut_.inverselySubstitute(facts, actionTerms, constants);
		assertEquals(result.size(), 6);
		assertTrue(result.contains("(clear a)"));
		assertTrue(result.contains("(block a)"));
		assertTrue(result.contains("(clear ?)"));
		assertTrue(result.contains("(on a ?)"));
		assertTrue(result.contains("(on ? ?)"));
		assertTrue(result.contains("(onFloor ?)"));

		actionTerms[0] = "b";
		result = sut_.inverselySubstitute(facts, actionTerms, constants);
		assertEquals(result.size(), 7);
		assertTrue(result.contains("(clear a)"));
		assertTrue(result.contains("(block a)"));
		assertTrue(result.contains("(clear ?X)"));
		assertTrue(result.contains("(clear ?)"));
		assertTrue(result.contains("(on a ?)"));
		assertTrue(result.contains("(on ?X ?)"));
		assertTrue(result.contains("(onFloor ?)"));
	}

	@Test
	public void testFormPreGoalState() throws Exception {
		// Ensuring we have no constants
		List<String> constants = new ArrayList<String>();
		StateSpec.getInstance().setConstants(constants);

		// Basic covering
		Rete state = StateSpec.getInstance().getRete();
		state.eval("(assert (clear a))");
		Collection<Fact> facts = StateSpec.extractFacts(state);
		sut_.formPreGoalState(facts, "(moveFloor a)", StateSpec.getInstance().getConstants());
		List<String> preGoal = sut_.getPreGoalState("moveFloor");
		assertEquals(preGoal.size(), 2);
		assertTrue(preGoal.contains("(clear a)"));
		assertTrue(preGoal.contains("(block a)"));

		// Generalisation (same predicate and action)
		state.reset();
		state.eval("(assert (clear b))");
		facts = StateSpec.extractFacts(state);
		sut_.formPreGoalState(facts, "(moveFloor b)", StateSpec.getInstance().getConstants());
		preGoal = sut_.getPreGoalState("moveFloor");
		assertEquals(preGoal.size(), 2);
		assertTrue(preGoal.contains("(clear ?X)"));
		assertTrue(preGoal.contains("(block ?X)"));

		// Generalising test (different predicates, same action)
		sut_.clearPreGoalState(2);
		state.reset();
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (on a d))");
		state.eval("(assert (on d c))");
		state.eval("(assert (on b e))");
		state.eval("(assert (onFloor e))");
		state.eval("(assert (onFloor c))");
		state.eval("(assert (highest a))");
		state.run();
		StateSpec.getInstance().insertValidActions(state);
		facts = StateSpec.extractFacts(state);
		sut_.formPreGoalState(facts, "(move a b)", StateSpec.getInstance().getConstants());
		preGoal = sut_.getPreGoalState("move");
		// Contains the defined preds, above and clear preds
		assertEquals(9, preGoal.size());
		assertTrue(preGoal.contains("(clear a)"));
		assertTrue(preGoal.contains("(clear b)"));
		assertTrue(preGoal.contains("(block a)"));
		assertTrue(preGoal.contains("(block b)"));
		assertTrue(preGoal.contains("(on a ?)"));
		assertTrue(preGoal.contains("(on b ?)"));
		assertTrue(preGoal.contains("(highest a)"));
		assertTrue(preGoal.contains("(above a ?)"));
		assertTrue(preGoal.contains("(above b ?)"));

		state.reset();
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (onFloor a))");
		state.eval("(assert (onFloor d))");
		state.eval("(assert (on b e))");
		state.eval("(assert (onFloor e))");
		state.eval("(assert (onFloor c))");
		state.eval("(assert (highest b))");
		state.run();
		StateSpec.getInstance().insertValidActions(state);
		facts = StateSpec.extractFacts(state);
		sut_.formPreGoalState(facts, "(move a b)", StateSpec.getInstance().getConstants());
		preGoal = sut_.getPreGoalState("move");
		// Contains less than the defined preds, above and clear preds
		assertEquals(6, preGoal.size());
		assertTrue(preGoal.contains("(clear a)"));
		assertTrue(preGoal.contains("(clear b)"));
		assertTrue(preGoal.contains("(block a)"));
		assertTrue(preGoal.contains("(block b)"));
		assertTrue(preGoal.contains("(on b ?)"));
		assertTrue(preGoal.contains("(above b ?)"));

		// Generalising the action
		sut_.formPreGoalState(facts, "(move b a)", StateSpec.getInstance().getConstants());
		preGoal = sut_.getPreGoalState("move");
		// Contains less than the defined preds, above and clear preds
		assertEquals(4, preGoal.size());
		assertTrue(preGoal.contains("(clear ?X)"));
		assertTrue(preGoal.contains("(clear ?Y)"));
		assertTrue(preGoal.contains("(block ?X)"));
		assertTrue(preGoal.contains("(block ?Y)"));
	}

	@Test
	public void testUnifyStates() {
		// No change unification
		List<String> oldState = new ArrayList<String>();
		oldState.add("(clear ?X)");
		List<String> newState = new ArrayList<String>();
		newState.add("(clear ?X)");
		List<String> oldTerms = new ArrayList<String>();
		oldTerms.add("?X");
		List<String> newTerms = new ArrayList<String>();
		newTerms.add("?X");
		int result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldTerms.contains("?X"));

		// No change with constants
		oldState.clear();
		oldState.add("(clear a)");
		newState.clear();
		newState.add("(clear a)");
		oldTerms.clear();
		oldTerms.add("a");
		newTerms.clear();
		newTerms.add("a");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear a)"));
		assertTrue(oldTerms.contains("a"));

		// Basic removal of preds unification
		oldState.clear();
		oldState.add("(clear ?X)");
		oldState.add("(on ?X ?)");
		oldState.add("(clear ?)");
		newState.clear();
		newState.add("(on ? ?X)");
		newState.add("(highest ?)");
		newState.add("(clear ?X)");
		oldTerms.clear();
		oldTerms.add("?X");
		newTerms.clear();
		newTerms.add("?X");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldTerms.contains("?X"));

		// Simple unification
		oldState.clear();
		oldState.add("(clear ?X)");
		oldState.add("(clear ?Y)");
		oldState.add("(on ?X ?)");
		newState.clear();
		newState.add("(clear ?Y)");
		newState.add("(clear ?X)");
		oldState.add("(on ?Y ?)");
		oldTerms.clear();
		oldTerms.add("?X");
		oldTerms.add("?Y");
		newTerms.clear();
		newTerms.add("?X");
		newTerms.add("?Y");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldState.contains("(clear ?Y)"));
		assertTrue(oldTerms.contains("?X"));
		assertTrue(oldTerms.contains("?Y"));

		// Absorption
		oldState.clear();
		oldState.add("(clear ?X)");
		newState.clear();
		newState.add("(clear a)");
		oldTerms.clear();
		oldTerms.add("?X");
		newTerms.clear();
		newTerms.add("a");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldTerms.contains("?X"));

		// Generalisation
		oldState.clear();
		oldState.add("(clear a)");
		newState.clear();
		newState.add("(clear ?X)");
		oldTerms.clear();
		oldTerms.add("a");
		newTerms.clear();
		newTerms.add("?X");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldTerms.contains("?X"));

		// Mutual generalisation
		oldState.clear();
		oldState.add("(clear a)");
		newState.clear();
		newState.add("(clear b)");
		oldTerms.clear();
		oldTerms.add("a");
		newTerms.clear();
		newTerms.add("b");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldTerms.contains("?X"));

		// Two terms
		oldState.clear();
		oldState.add("(on a b)");
		newState.clear();
		newState.add("(on b a)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("b");
		newTerms.clear();
		newTerms.add("b");
		newTerms.add("a");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on ?X ?Y)"));
		assertTrue(oldTerms.contains("?X"));
		assertTrue(oldTerms.contains("?Y"));

		// Two terms in differing order
		oldState.clear();
		oldState.add("(on a b)");
		newState.clear();
		newState.add("(on a b)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("b");
		newTerms.clear();
		newTerms.add("b");
		newTerms.add("a");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(-1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on a b)"));

		// Two terms with two aligned preds
		oldState.clear();
		oldState.add("(on a b)");
		oldState.add("(clear a)");
		newState.clear();
		newState.add("(on b a)");
		newState.add("(clear b)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("b");
		newTerms.clear();
		newTerms.add("b");
		newTerms.add("a");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains("(on ?X ?Y)"));
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldTerms.contains("?X"));

		// Two terms with two misaligned preds
		oldState.clear();
		oldState.add("(on a b)");
		oldState.add("(clear a)");
		newState.clear();
		newState.add("(on b a)");
		newState.add("(clear a)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("b");
		newTerms.clear();
		newTerms.add("b");
		newTerms.add("a");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on ?X ?Y)"));
		assertTrue(oldTerms.contains("?X"));
		assertTrue(oldTerms.contains("?Y"));

		// Generalisation to anonymous
		oldState.clear();
		oldState.add("(clear ?X)");
		newState.clear();
		newState.add("(clear ?)");
		oldTerms.clear();
		oldTerms.add("?X");
		newTerms.clear();
		newTerms.add("?X");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(-1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));

		// Constant and variable case
		oldState.clear();
		oldState.add("(on a ?X)");
		newState.clear();
		newState.add("(on b ?X)");
		oldTerms.clear();
		oldTerms.add("?X");
		newTerms.clear();
		newTerms.add("?X");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on ? ?X)"));
		assertTrue(oldTerms.contains("?X"));

		// Tough case
		oldState.clear();
		oldState.add("(on a ?Y)");
		newState.clear();
		newState.add("(on ? ?Y)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("?Y");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("?Y");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on ? ?Y)"));
		assertTrue(oldTerms.contains("a"));
		assertTrue(oldTerms.contains("?Y"));

		// Tough case 2
		oldState.clear();
		oldState.add("(on ?X a)");
		newState.clear();
		newState.add("(on ? ?Y)");
		oldTerms.clear();
		oldTerms.add("?X");
		oldTerms.add("a");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("?Y");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on ? ?Y)"));
		assertTrue(oldTerms.contains("?X"));
		assertTrue(oldTerms.contains("?Y"));

		// Tough case 3
		oldState.clear();
		oldState.add("(on a ?Y)");
		newState.clear();
		newState.add("(on a ?)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("?Y");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("?Y");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on a ?)"));
		assertTrue(oldTerms.contains("a"));
		assertTrue(oldTerms.contains("?Y"));

		// Early generalisation test
		oldState.clear();
		oldState.add("(on a ?Y)");
		newState.clear();
		newState.add("(on a ?)");
		newState.add("(on a ?Y)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("?Y");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("?Y");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on a ?Y)"));
		assertTrue(oldTerms.contains("a"));
		assertTrue(oldTerms.contains("?Y"));

		// Unnecessary replacement avoidance
		oldState.clear();
		oldState.add("(on a ?Y)");
		newState.clear();
		newState.add("(on ?X ?Y)");
		newState.add("(on a ?Y)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("?Y");
		newTerms.clear();
		newTerms.add("?X");
		newTerms.add("?Y");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on a ?Y)"));
		assertTrue(oldTerms.contains("a"));
		assertTrue(oldTerms.contains("?Y"));

		// Using the same fact for unification
		oldState.clear();
		oldState.add("(on a ?Y)");
		oldState.add("(on ?X ?Y)");
		newState.clear();
		newState.add("(on ?X ?Y)");
		oldTerms.clear();
		oldTerms.add("?X");
		oldTerms.add("?Y");
		newTerms.clear();
		newTerms.add("?X");
		newTerms.add("?Y");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains("(on ? ?Y)"));
		assertTrue(oldState.contains("(on ?X ?Y)"));
		assertTrue(oldTerms.contains("?X"));
		assertTrue(oldTerms.contains("?Y"));

		// Left with constant predicate
		oldState.clear();
		oldState.add("(on ?X b)");
		oldState.add("(clear ?X)");
		oldState.add("(clear ?Y)");
		newState.clear();
		newState.add("(on ?Y b)");
		newState.add("(clear ?X)");
		newState.add("(clear ?Y)");
		oldTerms.clear();
		oldTerms.add("?X");
		oldTerms.add("?Y");
		newTerms.clear();
		newTerms.add("?X");
		newTerms.add("?Y");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(3, oldState.size());
		assertTrue(oldState.contains("(on ? b)"));
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldState.contains("(clear ?Y)"));
		assertTrue(oldTerms.contains("?X"));
		assertTrue(oldTerms.contains("?Y"));

		// Un-unifiable
		oldState.clear();
		oldState.add("(clear ?X)");
		newState.clear();
		newState.add("(on ? ?X)");
		oldTerms.clear();
		oldTerms.add("?X");
		newTerms.clear();
		newTerms.add("?X");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(-1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));

		// Proper unification
		oldState.clear();
		oldState.add("(on a ?Y)");
		oldState.add("(on a b)");
		oldState.add("(clear a)");
		newState.clear();
		newState.add("(on a b)");
		newState.add("(on a c)");
		newState.add("(clear a)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("?Y");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("b");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(3, oldState.size());
		assertTrue(oldState.contains("(on a ?Y)"));
		assertTrue(oldState.contains("(on a b)"));
		assertTrue(oldState.contains("(clear a)"));

		// Improper unification
//		oldState.clear();
//		oldState.add("(on a ?Y)");
//		oldState.add("(on a b)");
//		oldState.add("(clear a)");
//		newState.clear();
//		newState.add("(on a b)");
//		newState.add("(clear a)");
//		oldTerms.clear();
//		oldTerms.add("a");
//		oldTerms.add("?Y");
//		newTerms.clear();
//		newTerms.add("a");
//		newTerms.add("b");
//		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
//		assertEquals(1, result);
//		assertEquals(2, oldState.size());
//		assertTrue(oldState.contains("(on a b)"));
//		assertTrue(oldState.contains("(clear a)"));
	}

	@Test
	public void testSpecialiseToPreGoal() {
		// Basic stack test
		List<String> pregoal = new ArrayList<String>();
		pregoal.add("(onFloor ?X)");
		pregoal.add("(clear ?X)");
		pregoal.add("(block ?X)");
		pregoal.add("(on ?Y ?)");
		pregoal.add("(clear ?Y)");
		pregoal.add("(block ?Y)");
		sut_.setPreGoal("(move ?X ?Y)", pregoal);

		GuidedRule rule = new GuidedRule(
				"(clear ?X) (clear ?Y) => (move ?X ?Y)");
		rule.expandConditions();
		assertEquals(5, rule.getConditions(false).size());
		Collection<GuidedRule> results = sut_.specialiseToPreGoal(rule);
		assertEquals(2, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear ?Y) (onFloor ?X) => (move ?X ?Y)", false,
				true, null)));
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear ?Y) (on ?Y ?) => (move ?X ?Y)", false, true,
				null)));

		// Full covering
		rule = new GuidedRule(
				"(clear ?X) (clear ?Y) (onFloor ?X) => (move ?X ?Y)");
		rule.expandConditions();
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(1, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear ?Y) (onFloor ?X) (on ?Y ?) => (move ?X ?Y)",
				false, true, null)));

		// Empty case
		pregoal.clear();
		pregoal.add("(clear ?X)");
		pregoal.add("(block ?X)");
		pregoal.add("(clear ?Y)");
		pregoal.add("(block ?Y)");
		sut_.setPreGoal("(move ?X ?Y)", pregoal);

		rule = new GuidedRule("(clear ?X) (clear ?Y) => (move ?X ?Y)");
		rule.expandConditions();
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(0, results.size());

		// Empty case 2
		rule = new GuidedRule("(on ?X ?) (clear ?X) => (moveFloor ?X)");
		rule.expandConditions();
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(0, results.size());

		// Specialisation to constant
		pregoal.clear();
		pregoal.add("(clear a)");
		pregoal.add("(block a)");
		pregoal.add("(clear b)");
		pregoal.add("(block b)");
		sut_.setPreGoal("(move a b)", pregoal);

		rule = new GuidedRule("(clear ?X) (clear ?Y) => (move ?X ?Y)");
		rule.expandConditions();
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(2, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(clear a) (clear ?Y) => (move a ?Y)", false, true, null)));
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear b) => (move ?X b)", false, true, null)));

		// Constant to variable (invalid!)
		pregoal.clear();
		pregoal.add("(clear ?X)");
		pregoal.add("(block ?X)");
		pregoal.add("(clear ?Y)");
		pregoal.add("(block ?Y)");
		sut_.setPreGoal("(move ?X ?Y)", pregoal);

		rule = new GuidedRule("(clear a) (clear b) => (move a b)");
		rule.expandConditions();
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(0, results.size());

		// Using constants for general rule addition
		pregoal.clear();
		pregoal.add("(onFloor a)");
		pregoal.add("(clear a)");
		pregoal.add("(block a)");
		pregoal.add("(on b ?)");
		pregoal.add("(clear b)");
		pregoal.add("(block b)");
		sut_.setPreGoal("(move a b)", pregoal);

		rule = new GuidedRule("(clear ?X) (clear ?Y) => (move ?X ?Y)");
		rule.expandConditions();
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(4, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear ?Y) (onFloor ?X) => (move ?X ?Y)", false,
				true, null)));
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear ?Y) (on ?Y ?) => (move ?X ?Y)", false, true,
				null)));
		assertTrue(results.contains(new GuidedRule(
				"(clear a) (clear ?Y) => (move a ?Y)", false, true, null)));
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear b) => (move ?X b)", false, true, null)));

		// Inner predicate specialisation
		pregoal.clear();
		pregoal.add("(on ?X a)");
		pregoal.add("(on ?Y b)");
		sut_.setPreGoal("(move ?X ?Y)", pregoal);

		rule = new GuidedRule("(on ?X ?) (on ?Y ?) => (move ?X ?Y)");
		rule.expandConditions();
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(2, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(on ?X a) (on ?Y ?) => (move ?X ?Y)", false, true, null)));
		assertTrue(results.contains(new GuidedRule(
				"(on ?X ?) (on ?Y b) => (move ?X ?Y)", false, true, null)));

		// Inner predicate specialisation with terms
		pregoal.clear();
		pregoal.add("(on ?X ?Y)");
		pregoal.add("(on ?Y b)");
		sut_.setPreGoal("(move ?X ?Y)", pregoal);

		rule = new GuidedRule("(on ?X ?) => (move ?X a)");
		rule.expandConditions();
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(1, results.size());
		assertTrue(results.contains(new GuidedRule("(on ?X a) => (move ?X a)",
				false, true, null)));

		// Adding constant facts
		pregoal.clear();
		pregoal.add("(onFloor a)");
		pregoal.add("(clear ?X)");
		pregoal.add("(block ?X)");
		pregoal.add("(on b ?)");
		pregoal.add("(clear ?Y)");
		pregoal.add("(block ?Y)");
		sut_.setPreGoal("(move ?X ?Y)", pregoal);

		rule = new GuidedRule(
				"(clear ?X) (onFloor ?X) (clear ?Y) (on ?Y ?) => (move ?X ?Y)");
		rule.expandConditions();
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(2, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (onFloor a) (clear ?Y) (on ?Y ?) => (move ?X ?Y)",
				false, true, null)));
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (onFloor ?X) (clear ?Y) (on b ?) => (move ?X ?Y)",
				false, true, null)));

		// Constant substitution 2
		pregoal.clear();
		pregoal.add("(on a b)");
		pregoal.add("(clear a)");
		pregoal.add("(block a)");
		pregoal.add("(block b)");
		sut_.setPreGoal("(move a b)", pregoal);

		rule = new GuidedRule("(on ?X ?Y) (clear ?X) => (move ?X ?Y)");
		rule.expandConditions();
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(2, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(on a ?Y) (clear a) => (move a ?Y)", false, true, null)));
		assertTrue(results.contains(new GuidedRule(
				"(on ?X b) (clear ?X) => (move ?X b)", false, true, null)));

		// Secondary mutation case
		pregoal.clear();
		pregoal.add("(block a)");
		pregoal.add("(block ?X)");
		pregoal.add("(on ?X a)");
		pregoal.add("(clear ?X)");
		pregoal.add("(above ?X a)");
		sut_.setPreGoal("(moveFloor ?X)", pregoal);

		rule = new GuidedRule(
				"(on ?X a) (clear ?X) (above ?X ?) (block ?X) (block a) => (moveFloor ?X)");
		rule.expandConditions();
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(1, results.size());
		assertTrue(results
				.contains(new GuidedRule(
						"(on ?X a) (clear ?X) (above ?X a) (block ?X) (block a) => (moveFloor ?X)",
						false, true, null)));
	}
}
