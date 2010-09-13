package test;

import static org.junit.Assert.*;

import java.util.List;

import jess.QueryResult;
import jess.Rete;
import jess.ValueVector;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import relationalFramework.GuidedRule;
import relationalFramework.MultiMap;
import relationalFramework.StateSpec;

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
		assertEquals(rule.getConditions().size(), 2);
		assertTrue(rule.getConditions().contains("(clear ?X)"));
		assertTrue(rule.getConditions().contains("(block ?X)"));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertTrue(rule.getAction().equals("(moveFloor ?X)"));

		// Test for a constant
		rule = new GuidedRule("(clear a) => (moveFloor a)");
		// 2 assertions in the body: clear, and block, with a constant
		// variable
		assertEquals(rule.getConditions().size(), 2);
		assertTrue(rule.getConditions().contains("(clear a)"));
		assertTrue(rule.getConditions().contains("(block a)"));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(), "(moveFloor a)");

		// Test for constants (no inequals)
		rule = new GuidedRule("(clear a) (clear b) => (moveFloor a)");
		// 4 assertions in the body: 2 clears, and 2 blocks, without an
		// inequality test
		assertEquals(rule.getConditions().size(), 4);
		assertTrue(rule.getConditions().contains("(clear a)"));
		assertTrue(rule.getConditions().contains("(block a)"));
		assertTrue(rule.getConditions().contains("(clear b)"));
		assertTrue(rule.getConditions().contains("(block b)"));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(), "(moveFloor a)");

		// Multiple conditions, one term
		rule = new GuidedRule("(clear ?X) (highest ?X) => (moveFloor ?X)");
		// 3 assertions in the body: clear, highest, and a single block
		assertEquals(rule.getConditions().size(), 3);
		assertTrue(rule.getConditions().contains("(clear ?X)"));
		assertTrue(rule.getConditions().contains("(block ?X)"));
		assertTrue(rule.getConditions().contains("(highest ?X)"));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(), "(moveFloor ?X)");

		// Multiple conditions, two terms
		rule = new GuidedRule("(clear ?X) (highest ?Y) => (moveFloor ?X)");
		// 5 assertions in the body: clear, highest, two blocks, and an
		// inequals test
		assertEquals(rule.getConditions().size(), 5);
		assertTrue(rule.getConditions().contains("(clear ?X)"));
		assertTrue(rule.getConditions().contains("(block ?X)"));
		assertTrue(rule.getConditions().contains("(highest ?Y)"));
		assertTrue(rule.getConditions().contains("(block ?Y)"));
		assertTrue(rule.getConditions().contains("(test (<> ?Y ?X))"));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("test"));
		assertTrue(rule.getStringConditions().indexOf("test") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(), "(moveFloor ?X)");

		// Variables and constants
		rule = new GuidedRule("(on ?X a) (above b ?Y) => (moveFloor ?X)");
		// 5 assertions in the body: clear, highest, two blocks, and an
		// inequals test
		assertEquals(rule.getConditions().size(), 8);
		assertTrue(rule.getConditions().contains("(on ?X a)"));
		assertTrue(rule.getConditions().contains("(block ?X)"));
		assertTrue(rule.getConditions().contains("(block a)"));
		assertTrue(rule.getConditions().contains("(above b ?Y)"));
		assertTrue(rule.getConditions().contains("(block b)"));
		assertTrue(rule.getConditions().contains("(block ?Y)"));
		assertTrue(rule.getConditions().contains("(test (<> ?Y ?X a b))"));
		assertTrue(rule.getConditions().contains("(test (<> ?X a b))"));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("test"));
		assertTrue(rule.getStringConditions().indexOf("test") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(), "(moveFloor ?X)");

		// Test anonymous variable
		rule = new GuidedRule("(clear ?X) (on ?X ?) => (moveFloor ?X)");
		// 5 assertions in the body: clear, on, two blocks, and an
		// inequals
		assertEquals(rule.getConditions().size(), 3);
		assertTrue(rule.getConditions().contains("(clear ?X)"));
		assertTrue(rule.getConditions().contains("(block ?X)"));
		assertTrue(rule.getConditions().contains("(on ?X ?)"));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(), "(moveFloor ?X)");

		// Test anonymous variables
		rule = new GuidedRule(
				"(clear ?X) (on ?X ?) (on ?Y ?) => (moveFloor ?X)");
		// 10 assertions in the body: clear, 2 ons, 4 blocks, and 3
		// inequals
		// Note no inequals between ?1 and ?2
		assertEquals(rule.getConditions().size(), 6);
		assertTrue(rule.getConditions().contains("(clear ?X)"));
		assertTrue(rule.getConditions().contains("(block ?X)"));
		assertTrue(rule.getConditions().contains("(on ?X ?)"));
		assertTrue(rule.getConditions().contains("(on ?Y ?)"));
		assertTrue(rule.getConditions().contains("(block ?Y)"));
		assertTrue(rule.getConditions().contains("(test (<> ?Y ?X))"));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("test"));
		assertTrue(rule.getStringConditions().indexOf("test") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(), "(moveFloor ?X)");

		// Test type predicate
		rule = new GuidedRule("(block ?X) => (moveFloor ?X)");
		assertEquals(rule.getConditions().size(), 1);
		assertTrue(rule.getConditions().contains("(block ?X)"));
		assertEquals(rule.getAction(), "(moveFloor ?X)");

		// Test inequal type predicates
		rule = new GuidedRule("(block ?X) (block ?Y) => (move ?X ?Y)");
		assertEquals(rule.getConditions().size(), 3);
		assertTrue(rule.getConditions().contains("(block ?X)"));
		assertTrue(rule.getConditions().contains("(block ?Y)"));
		assertTrue(rule.getConditions().contains("(test (<> ?Y ?X))"));
		assertEquals(rule.getAction(), "(move ?X ?Y)");

		// Test existing type predicate
		rule = new GuidedRule("(clear ?X) (block ?X) => (moveFloor ?X)");
		assertEquals(rule.getConditions().size(), 2);
		assertTrue(rule.getConditions().contains("(block ?X)"));
		assertTrue(rule.getConditions().contains("(clear ?X)"));
		assertEquals(rule.getAction(), "(moveFloor ?X)");

		// Test inequals parsing
		rule = new GuidedRule(
				"(clear ?X) (clear ?Y) (test (<> ?Y ?X)) (block ?X) (block ?Y) => (move ?X ?Y)");
		assertEquals(rule.getConditions().size(), 5);
		assertTrue(rule.getConditions().contains("(clear ?X)"));
		assertTrue(rule.getConditions().contains("(clear ?Y)"));
		assertTrue(rule.getConditions().contains("(test (<> ?Y ?X))"));
		assertTrue(rule.getConditions().contains("(block ?X)"));
		assertTrue(rule.getConditions().contains("(block ?Y)"));
		assertEquals(rule.getAction(), "(move ?X ?Y)");
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("test"));
		assertTrue(rule.getStringConditions().indexOf("test") < rule
				.getStringConditions().indexOf("block"));

		// Testing module syntax
		rule = new GuidedRule("(above ?X ?_MOD_a) (clear ?X) => (moveFloor ?X)");
		assertTrue(rule.getConditions().contains("(block ?X)"));
		assertTrue(rule.getConditions().contains("(block ?_MOD_a)"));
		assertTrue(rule.getConditions().contains("(above ?X ?_MOD_a)"));
		assertTrue(rule.getConditions().contains("(clear ?X)"));
		assertTrue(rule.getConditions().contains("(test (<> ?_MOD_a ?X))"));
		assertEquals(rule.getAction(), "(moveFloor ?X)");
		assertEquals(rule.getConditions().size(), 5);

		// Testing negation
		rule = new GuidedRule("(clear ?X) (not (highest ?X)) => (moveFloor ?X)");
		assertTrue(rule.getConditions().contains("(block ?X)"));
		assertTrue(rule.getConditions().contains("(clear ?X)"));
		assertTrue(rule.getConditions().contains("(not (highest ?X))"));
		assertEquals(rule.getAction(), "(moveFloor ?X)");
		assertEquals(rule.getConditions().size(), 3);

		// Testing negation (with extra terms)
		rule = new GuidedRule("(clear ?X) (not (highest ?X)) => (moveFloor ?X)");
		assertTrue(rule.getConditions().contains("(block ?X)"));
		assertTrue(rule.getConditions().contains("(clear ?X)"));
		assertTrue(rule.getConditions().contains("(not (highest ?X))"));
		assertEquals(rule.getAction(), "(moveFloor ?X)");
		assertEquals(rule.getConditions().size(), 3);
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
