package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import jess.Fact;
import jess.Rete;

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
		sut_ = new Covering();
		StateSpec.initInstance("blocksWorld.BlocksWorld");
	}

	@Test
	public void testCoverState() throws Exception {
		// No constants
		StateSpec.getInstance().setConstants(new ArrayList<String>());
		
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
				int condCount = StateSpec.getInstance().encodeRule(
						gr.getStringConditions()).replaceAll("\\(.+?\\)( |$)", ".")
						.length();
				assertEquals(3, condCount);
				assertFalse(gr.getConditions(false).contains("(highest ?X)"));
				assertTrue(gr.getConditions(false).contains("(clear ?X)"));
				assertTrue(gr.getConditions(false).contains("(on ?X ?)"));
				assertTrue(gr.getConditions(false).contains("(above ?X ?)"));
				assertFalse(gr.getConditions(false).contains("(onFloor ?X)"));
				assertEquals("(moveFloor ?X)", gr.getAction());
			} else {
				int condCount = StateSpec.getInstance().encodeRule(
						gr.getStringConditions()).replaceAll("\\(.+?\\)( |$)", ".")
						.length();
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
				int condCount = StateSpec.getInstance().encodeRule(
						gr.getStringConditions()).replaceAll("\\(.+?\\)( |$)", ".")
						.length();
				assertEquals(4, condCount);
				assertTrue(gr.getConditions(false).contains("(highest ?X)"));
				assertTrue(gr.getConditions(false).contains("(clear ?X)"));
				assertTrue(gr.getConditions(false).contains("(on ?X ?)"));
				assertTrue(gr.getConditions(false).contains("(above ?X ?)"));
				assertFalse(gr.getConditions(false).contains("(onFloor ?X)"));
				assertEquals("(moveFloor ?X)", gr.getAction());
			} else {
				int condCount = StateSpec.getInstance().encodeRule(
						gr.getStringConditions()).replaceAll("\\(.+?\\)( |$)", ".")
						.length();
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
		rules = sut_.coverState(state, new MultiMap<String, GuidedRule>(), false);
		assertTrue(rules.isEmpty());
		
		// TODO Test for constant covering
	}

//	@Test
//	public void testSpecialiseRule() {
//		fail("Not yet implemented");
//	}

	@Test
	public void testFormatAction() {
		// Simple action
		String actionPred = "moveFloor";
		assertEquals("(moveFloor ?X)", sut_.formatAction(actionPred));

		// Larger action
		String movePred = "move";
		assertEquals("(move ?X ?Y)", sut_.formatAction(movePred));
	}
	
	@Test
	public void testInverselySubstitute() throws Exception {
		// Ensuring we have no constants
		List<String> constants = new ArrayList<String>();
		
		// Basic substitution
		Rete state = StateSpec.getInstance().getRete();
		state.eval("(assert (clear a))");
		Collection<Fact> facts = StateSpec.extractFacts(state);
		String[] actionTerms = {"a"};
		Collection<String> result = sut_.inverselySubstitute(facts, actionTerms, constants);
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
		assertEquals(result.size(), 5);
		assertTrue(result.contains("(clear a)"));
		assertTrue(result.contains("(clear ?)"));
		assertTrue(result.contains("(on a ?)"));
		assertTrue(result.contains("(on ? ?)"));
		assertTrue(result.contains("(onFloor ?)"));
		
		actionTerms[0] = "b";
		result = sut_.inverselySubstitute(facts, actionTerms, constants);
		assertEquals(result.size(), 6);
		assertTrue(result.contains("(clear a)"));
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
		boolean result = sut_.formPreGoalState(facts, "(moveFloor a)");
		assertTrue(result);
		List<String> preGoal = sut_.getPreGoalState("moveFloor");
		assertEquals(preGoal.size(), 1);
		assertTrue(preGoal.contains("(clear a)"));
		
		// Generalisation (same predicate and action)
		state.reset();
		state.eval("(assert (clear b))");
		facts = StateSpec.extractFacts(state);
		result = sut_.formPreGoalState(facts, "(moveFloor b)");
		assertTrue(result);
		preGoal = sut_.getPreGoalState("moveFloor");
		assertEquals(preGoal.size(), 1);
		assertTrue(preGoal.contains("(clear ?X)"));
		
		// Generalising test (different predicates, same action)
		sut_.clearPreGoalState();
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
		result = sut_.formPreGoalState(facts, "(move a b)");
		assertTrue(result);
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
		result = sut_.formPreGoalState(facts, "(move a b)");
		assertTrue(result);
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
		result = sut_.formPreGoalState(facts, "(move b a)");
		assertTrue(result);
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
	}
}
