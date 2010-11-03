package test;

import static org.junit.Assert.*;

import java.util.List;

import jess.Rete;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import relationalFramework.ConstantPred;
import relationalFramework.GuidedRule;
import relationalFramework.MultiMap;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

public class BlocksWorldStateSpecTest {

	private StateSpec spec_;

	@Before
	public void setUp() throws Exception {
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
		spec_ = StateSpec.initInstance("blocksWorld.BlocksWorld");
	}

	@Test
	public void testReinitInstance() {
		try {
			spec_.getRete().eval("(assert (on a b))");
			spec_.getRete().eval("(facts)");
			StateSpec.reinitInstance();
			spec_.getRete().eval("(facts)");
		} catch (Exception e) {
			fail("Exception occured.");
		}
	}

	@Test
	public void testRuleCreation() {
		// Basic variable test
		GuidedRule rule = new GuidedRule("(clear ?X) => (moveFloor ?X)");
		// 2 assertions in the body: clear, and block
		assertEquals(rule.getConditions(false).size(), 2);
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?X)")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(), StateSpec.toStringFact("(moveFloor ?X)"));
		ConstantPred constants = rule.getConstantConditions();
		assertNull(constants);

		// Test for a constant
		rule = new GuidedRule("(clear a) => (moveFloor a)");
		// 2 assertions in the body: clear, and block, with a constant
		// variable
		assertEquals(rule.getConditions(false).size(), 2);
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(clear a)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block a)")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(), StateSpec.toStringFact("(moveFloor a)"));
		constants = rule.getConstantConditions();
		StringFact strFact = StateSpec.getInstance().getPredicates().get(
				"clear");
		assertTrue(constants.getFacts().contains(
				new StringFact(strFact, new String[] { "a" })));
		assertEquals(constants.getFacts().size(), 1);

		// Test for constants (no inequals)
		rule = new GuidedRule("(clear a) (clear b) => (moveFloor a)");
		// 4 assertions in the body: 2 clears, and 2 blocks, without an
		// inequality test
		assertEquals(rule.getConditions(false).size(), 4);
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(clear a)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block a)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(clear b)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block b)")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(), StateSpec.toStringFact("(moveFloor a)"));
		constants = rule.getConstantConditions();
		strFact = StateSpec.getInstance().getPredicates().get(
				"clear");
		assertTrue(constants.getFacts().contains(
				new StringFact(strFact, new String[] { "a" })));
		assertTrue(constants.getFacts().contains(
				new StringFact(strFact, new String[] { "b" })));
		assertEquals(constants.getFacts().size(), 2);

		// Multiple conditions, one term
		rule = new GuidedRule("(clear ?X) (highest ?X) => (moveFloor ?X)");
		// 3 assertions in the body: clear, highest, and a single block
		assertEquals(rule.getConditions(false).size(), 3);
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(highest ?X)")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(), StateSpec.toStringFact("(moveFloor ?X)"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Multiple conditions, two terms
		rule = new GuidedRule("(clear ?X) (highest ?Y) => (moveFloor ?X)");
		// 5 assertions in the body: clear, highest, two blocks, and an
		// inequals test
		assertEquals(rule.getConditions(false).size(), 5);
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(highest ?Y)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?Y)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(test (<> ?Y ?X))")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertTrue(rule.getStringConditions().indexOf("block") < rule
				.getStringConditions().indexOf("test"));
		assertEquals(rule.getAction(), StateSpec.toStringFact("(moveFloor ?X)"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Variables and constants
		rule = new GuidedRule("(on ?X a) (above b ?Y) => (moveFloor ?X)");
		// 5 assertions in the body: clear, highest, two blocks, and an
		// inequals test
		assertEquals(rule.getConditions(false).size(), 8);
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(on ?X a)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block a)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(above b ?Y)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block b)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?Y)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(test (<> ?Y ?X a b))")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(test (<> ?X a b))")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertTrue(rule.getStringConditions().indexOf("block") < rule
				.getStringConditions().indexOf("test"));
		assertEquals(rule.getAction(), StateSpec.toStringFact("(moveFloor ?X)"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Test anonymous variable
		rule = new GuidedRule("(clear ?X) (on ?X ?) => (moveFloor ?X)");
		// 5 assertions in the body: clear, on, two blocks, and an
		// inequals
		assertEquals(rule.getConditions(false).size(), 3);
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(on ?X ?)")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(), StateSpec.toStringFact("(moveFloor ?X)"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Test anonymous variables
		rule = new GuidedRule(
				"(clear ?X) (on ?X ?) (on ?Y ?) => (moveFloor ?X)");
		// 10 assertions in the body: clear, 2 ons, 4 blocks, and 3
		// inequals
		// Note no inequals between ?1 and ?2
		assertEquals(rule.getConditions(false).size(), 6);
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(on ?X ?)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(on ?Y ?)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?Y)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(test (<> ?Y ?X))")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertTrue(rule.getStringConditions().indexOf("block") < rule
				.getStringConditions().indexOf("test"));
		assertEquals(rule.getAction(), StateSpec.toStringFact("(moveFloor ?X)"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Test type predicate
		rule = new GuidedRule("(block ?X) => (moveFloor ?X)");
		assertEquals(rule.getConditions(false).size(), 1);
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?X)")));
		assertEquals(rule.getAction(), StateSpec.toStringFact("(moveFloor ?X)"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Test inequal type predicates
		rule = new GuidedRule("(block ?X) (block ?Y) => (move ?X ?Y)");
		assertEquals(rule.getConditions(false).size(), 3);
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?Y)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(test (<> ?Y ?X))")));
		assertEquals(rule.getAction(), StateSpec.toStringFact("(move ?X ?Y)"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Test existing type predicate
		rule = new GuidedRule("(clear ?X) (block ?X) => (moveFloor ?X)");
		assertEquals(rule.getConditions(false).size(), 2);
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(clear ?X)")));
		assertEquals(rule.getAction(), StateSpec.toStringFact("(moveFloor ?X)"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Test inequals parsing
		rule = new GuidedRule(
				"(clear ?X) (clear ?Y) (test (<> ?Y ?X)) (block ?X) (block ?Y) => (move ?X ?Y)");
		assertEquals(rule.getConditions(false).size(), 5);
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(clear ?Y)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(test (<> ?Y ?X))")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?Y)")));
		assertEquals(rule.getAction(), StateSpec.toStringFact("(move ?X ?Y)"));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertTrue(rule.getStringConditions().indexOf("block") < rule
				.getStringConditions().indexOf("test"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Testing module syntax
		rule = new GuidedRule("(above ?X ?_MOD_a) (clear ?X) => (moveFloor ?X)");
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?_MOD_a)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(above ?X ?_MOD_a)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(test (<> ?_MOD_a ?X))")));
		assertEquals(rule.getAction(), StateSpec.toStringFact("(moveFloor ?X)"));
		assertEquals(rule.getConditions(false).size(), 5);
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Testing module constants
		rule = new GuidedRule(
				"(clear ?_MOD_a) (on ?_MOD_a ?) => (moveFloor ?_MOD_a)");
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?_MOD_a)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(on ?_MOD_a ?)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(clear ?_MOD_a)")));
		assertEquals(rule.getAction(), StateSpec.toStringFact("(moveFloor ?_MOD_a)"));
		assertEquals(rule.getConditions(false).size(), 3);
		constants = rule.getConstantConditions();
		strFact = StateSpec.getInstance().getPredicates().get(
				"clear");
		assertTrue(constants.getFacts().contains(
				new StringFact(strFact, new String[] { "?_MOD_a" })));
		assertEquals(constants.getFacts().size(), 1);

		// Testing negation
		rule = new GuidedRule("(clear ?X) (not (highest ?X)) => (moveFloor ?X)");
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(not (highest ?X))")));
		assertEquals(rule.getAction(), StateSpec.toStringFact("(moveFloor ?X)"));
		assertEquals(rule.getConditions(false).size(), 3);
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Testing negation (with extra terms)
		rule = new GuidedRule("(clear ?X) (not (highest ?X)) => (moveFloor ?X)");
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(StateSpec.toStringFact("(not (highest ?X))")));
		assertEquals(rule.getAction(), StateSpec.toStringFact("(moveFloor ?X)"));
		assertEquals(rule.getConditions(false).size(), 3);
		constants = rule.getConstantConditions();
		assertNull(constants);
	}

	@Test
	public void testToString() {
		assertEquals("StateSpec", spec_.toString());
	}

	@Test
	public void testEncodeRule() {
		String result = spec_.encodeRule(new GuidedRule(
				"(block a) (clear a) => (moveFloor a)"));
		assertTrue(result.equals("(clear a) => (moveFloor a)"));

		result = spec_.encodeRule(new GuidedRule(
				"(block a) (block b) (on a b) => (move a b)"));
		assertTrue(result.equals("(on a b) => (move a b)"));

		result = spec_.encodeRule(new GuidedRule(
				"(block ?X) (clear ?X) => (moveFloor ?X)"));
		assertTrue(result.equals("(clear ?X) => (moveFloor ?X)"));

		result = spec_.encodeRule(new GuidedRule(
				"(block ?X) (block ?Y) (test (<> ?X ?Y)) "
						+ "(clear ?X) (clear ?Y) => (move ?X ?Y)"));
		assertTrue(result.equals("(clear ?X) (clear ?Y) => (move ?X ?Y)"));

		result = spec_.encodeRule(new GuidedRule("(block a) => (moveFloor a)"));
		assertTrue(result.equals("(block a) => (moveFloor a)"));
	}

	@Test
	public void testInsertValidActions() throws Exception {
		Rete state = spec_.getRete();

		// Empty case
		MultiMap<String, String> validActions = spec_
				.generateValidActions(state);
		state.eval("(facts)");
		assertNotNull(validActions);
		assertNull(validActions.get("move"));
		assertNull(validActions.get("moveFloor"));
		state.reset();

		// Simple move case
		state.eval("(assert (clear a))");
		state.eval("(assert (clear b))");
		validActions = spec_.generateValidActions(state);
		state.eval("(facts)");
		assertNotNull(validActions);
		List<String> moveResult = validActions.get("move");
		assertTrue(moveResult.contains("a b"));
		assertTrue(moveResult.contains("b a"));
		List<String> moveFloorResult = validActions.get("moveFloor");
		assertNull(moveFloorResult);
		state.reset();

		// Simple moveFloor case
		state.eval("(assert (clear v))");
		state.eval("(assert (on v y))");
		validActions = spec_.generateValidActions(state);
		state.eval("(facts)");
		assertNotNull(validActions);
		moveResult = validActions.get("move");
		assertNull(moveResult);
		moveFloorResult = validActions.get("moveFloor");
		assertTrue(moveFloorResult.contains("v"));
		state.reset();

		// Complex both case
		state.eval("(assert (clear d))");
		state.eval("(assert (clear e))");
		state.eval("(assert (clear c))");
		state.eval("(assert (on e b))");
		state.eval("(assert (on d a))");
		state.eval("(assert (onFloor c))");
		validActions = spec_.generateValidActions(state);
		state.eval("(facts)");
		assertNotNull(validActions);
		moveResult = validActions.get("move");
		assertTrue(moveResult.contains("d e"));
		assertTrue(moveResult.contains("d c"));
		assertTrue(moveResult.contains("e d"));
		assertTrue(moveResult.contains("e c"));
		assertTrue(moveResult.contains("c d"));
		assertTrue(moveResult.contains("c e"));
		moveFloorResult = validActions.get("moveFloor");
		assertTrue(moveFloorResult.contains("d"));
		assertTrue(moveFloorResult.contains("e"));
	}

	@Test
	public void testSplitFact() {
		// Basic
		String[] result = StateSpec.splitFact("(clear a)");
		assertArrayEquals(new String[] { "clear", "a" }, result);

		// More complex
		result = StateSpec.splitFact("(on a b)");
		assertArrayEquals(new String[] { "on", "a", "b" }, result);

		// No parentheses
		result = StateSpec.splitFact("clear a");
		assertArrayEquals(new String[] { "clear", "a" }, result);

		// Module declaration
		result = StateSpec.splitFact("(MAIN::clear a)");
		assertArrayEquals(new String[] { "clear", "a" }, result);

		// Inner condition
		result = StateSpec.splitFact("(clear ?X&:(<> ?X hat))");
		assertArrayEquals(new String[] { "clear", "?X&:(<> ?X hat)" }, result);

		// Test condition
		result = StateSpec.splitFact("(test (<> ?X hat))");
		assertArrayEquals(new String[] { "test", "<>", "?X", "hat" }, result);

		// Not condition
		result = StateSpec.splitFact("(not (clear ?X))");
		assertArrayEquals(new String[] { "not", "clear", "?X" }, result);
	}
}
