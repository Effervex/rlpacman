package test;

import static org.junit.Assert.*;

import relationalFramework.GoalCondition;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.util.Collection;
import java.util.SortedSet;

import jess.Rete;

import org.junit.Before;
import org.junit.Test;

import util.MultiMap;

public class BlocksWorldStateSpecTest {

	private StateSpec spec_;

	@Before
	public void setUp() throws Exception {
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
		RelationalRule rule = new RelationalRule("(clear ?X) => (moveFloor ?X)");
		// 2 assertions in the body: clear, and block
		assertEquals(rule.getConditions(false).size(), 2);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(), StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		Collection<GoalCondition> constants = rule.getConstantConditions();
		assertNull(constants);

		// Test for a constant
		rule = new RelationalRule("(clear a) => (moveFloor a)");
		// 2 assertions in the body: clear, and block, with a constant
		// variable
		assertEquals(rule.getConditions(false).size(), 2);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear a)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block a)")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(), StateSpec.toRelationalPredicate("(moveFloor a)"));
		constants = rule.getConstantConditions();
		RelationalPredicate strFact = StateSpec.getInstance().getPredicates()
				.get("clear");
		assertTrue(constants.contains(new GoalCondition(new RelationalPredicate(strFact,
				new String[] { "a" }))));
		assertEquals(constants.size(), 1);

		// Test for constants (no inequals)
		rule = new RelationalRule("(clear a) (clear b) => (moveFloor a)");
		// 4 assertions in the body: 2 clears, and 2 blocks, without an
		// inequality test
		assertEquals(rule.getConditions(false).size(), 4);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear a)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block a)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear b)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block b)")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(), StateSpec.toRelationalPredicate("(moveFloor a)"));
		constants = rule.getConstantConditions();
		strFact = StateSpec.getInstance().getPredicates().get("clear");
		assertTrue(constants.contains(new GoalCondition(new RelationalPredicate(strFact,
				new String[] { "a" }))));
		assertTrue(constants.contains(new GoalCondition(new RelationalPredicate(strFact,
				new String[] { "b" }))));
		assertEquals(constants.size(), 2);

		// Multiple conditions, one term
		rule = new RelationalRule("(clear ?X) (highest ?X) => (moveFloor ?X)");
		// 3 assertions in the body: clear, highest, and a single block
		assertEquals(rule.getConditions(false).size(), 3);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(highest ?X)")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(), StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Multiple conditions, two terms
		rule = new RelationalRule("(clear ?X) (highest ?Y) => (moveFloor ?X)");
		// 5 assertions in the body: clear, highest, two blocks, and an
		// inequals test
		assertEquals(rule.getConditions(false).size(), 5);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(highest ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?Y ?X))")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertTrue(rule.getStringConditions().indexOf("block") < rule
				.getStringConditions().indexOf("test"));
		assertEquals(rule.getAction(), StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Variables and constants
		rule = new RelationalRule("(on ?X a) (above b ?Y) => (moveFloor ?X)");
		// 5 assertions in the body: clear, highest, two blocks, and an
		// inequals test
		assertEquals(rule.getConditions(false).size(), 8);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?X a)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block a)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(above b ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block b)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?Y ?X a b))")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?X a b))")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertTrue(rule.getStringConditions().indexOf("block") < rule
				.getStringConditions().indexOf("test"));
		assertEquals(rule.getAction(), StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Test anonymous variable
		rule = new RelationalRule("(clear ?X) (on ?X ?) => (moveFloor ?X)");
		// 5 assertions in the body: clear, on, two blocks, and an
		// inequals
		assertEquals(rule.getConditions(false).size(), 3);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?X ?)")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(), StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Test anonymous variables
		rule = new RelationalRule(
				"(clear ?X) (on ?X ?) (on ?Y ?) => (moveFloor ?X)");
		// 10 assertions in the body: clear, 2 ons, 4 blocks, and 3
		// inequals
		// Note no inequals between ?1 and ?2
		assertEquals(rule.getConditions(false).size(), 6);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?X ?)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?Y ?)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?Y ?X))")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertTrue(rule.getStringConditions().indexOf("block") < rule
				.getStringConditions().indexOf("test"));
		assertEquals(rule.getAction(), StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Test type predicate
		rule = new RelationalRule("(block ?X) => (moveFloor ?X)");
		assertEquals(rule.getConditions(false).size(), 1);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertEquals(rule.getAction(), StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Test inequal type predicates
		rule = new RelationalRule("(block ?X) (block ?Y) => (move ?X ?Y)");
		assertEquals(rule.getConditions(false).size(), 3);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?Y ?X))")));
		assertEquals(rule.getAction(), StateSpec.toRelationalPredicate("(move ?X ?Y)"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Test existing type predicate
		rule = new RelationalRule("(clear ?X) (block ?X) => (moveFloor ?X)");
		assertEquals(rule.getConditions(false).size(), 2);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertEquals(rule.getAction(), StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Test inequals parsing
		rule = new RelationalRule(
				"(clear ?X) (clear ?Y) (test (<> ?Y ?X)) (block ?X) (block ?Y) => (move ?X ?Y)");
		assertEquals(rule.getConditions(false).size(), 5);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?Y ?X))")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?Y)")));
		assertEquals(rule.getAction(), StateSpec.toRelationalPredicate("(move ?X ?Y)"));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertTrue(rule.getStringConditions().indexOf("block") < rule
				.getStringConditions().indexOf("test"));
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Testing module syntax
		rule = new RelationalRule("(above ?X ?_MOD_a) (clear ?X) => (moveFloor ?X)");
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?_MOD_a)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(above ?X ?_MOD_a)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?_MOD_a ?X))")));
		assertEquals(rule.getAction(), StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		assertEquals(rule.getConditions(false).size(), 5);
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Testing module constants
		rule = new RelationalRule(
				"(clear ?G_0) (on ?G_0 ?) => (moveFloor ?G_0)");
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?G_0)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?G_0 ?)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?G_0)")));
		assertEquals(rule.getAction(),
				StateSpec.toRelationalPredicate("(moveFloor ?G_0)"));
		assertEquals(rule.getConditions(false).size(), 3);
		constants = rule.getConstantConditions();
		strFact = StateSpec.getInstance().getPredicates().get("clear");
		assertTrue(constants.contains(new GoalCondition(new RelationalPredicate(strFact,
				new String[] { "?G_0" }))));
		assertEquals(constants.size(), 1);

		// Testing negation
		rule = new RelationalRule("(clear ?X) (not (highest ?X)) => (moveFloor ?X)");
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(not (highest ?X))")));
		assertEquals(rule.getAction(), StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		assertEquals(rule.getConditions(false).size(), 3);
		constants = rule.getConstantConditions();
		assertNull(constants);

		// Testing negation (with extra terms)
		rule = new RelationalRule("(clear ?X) (not (highest ?X)) => (moveFloor ?X)");
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(not (highest ?X))")));
		assertEquals(rule.getAction(), StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		assertEquals(rule.getConditions(false).size(), 3);
		constants = rule.getConstantConditions();
		assertNull(constants);
	}

	@Test
	public void testToString() {
		assertEquals("StateSpec", spec_.toString());
	}

	@Test
	public void testInsertValidActions() throws Exception {
		Rete state = spec_.getRete();

		// Empty case
		MultiMap<String, String[]> validActions = spec_
				.generateValidActions(state);
		state.eval("(facts)");
		assertNotNull(validActions);
		assertNull(validActions.getSortedSet("move"));
		assertNull(validActions.getSortedSet("moveFloor"));
		state.reset();

		// Simple move case
		state.eval("(assert (clear a))");
		state.eval("(assert (clear b))");
		validActions = spec_.generateValidActions(state);
		state.eval("(facts)");
		assertNotNull(validActions);
		SortedSet<String[]> moveResult = validActions.getSortedSet("move");
		assertTrue(moveResult.contains(new String[] { "a", "b" }));
		assertTrue(moveResult.contains(new String[] { "b", "a" }));
		SortedSet<String[]> moveFloorResult = validActions
				.getSortedSet("moveFloor");
		assertNull(moveFloorResult);
		state.reset();

		// Simple moveFloor case
		state.eval("(assert (clear v))");
		state.eval("(assert (on v y))");
		validActions = spec_.generateValidActions(state);
		state.eval("(facts)");
		assertNotNull(validActions);
		moveResult = validActions.getSortedSet("move");
		assertNull(moveResult);
		moveFloorResult = validActions.getSortedSet("moveFloor");
		assertTrue(moveFloorResult.contains(new String[] { "v" }));
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
		moveResult = validActions.getSortedSet("move");
		assertTrue(moveResult.contains(new String[] { "d", "e" }));
		assertTrue(moveResult.contains(new String[] { "d", "c" }));
		assertTrue(moveResult.contains(new String[] { "e", "d" }));
		assertTrue(moveResult.contains(new String[] { "e", "c" }));
		assertTrue(moveResult.contains(new String[] { "c", "d" }));
		assertTrue(moveResult.contains(new String[] { "c", "e" }));
		moveFloorResult = validActions.getSortedSet("moveFloor");
		assertTrue(moveFloorResult.contains(new String[] { "d" }));
		assertTrue(moveFloorResult.contains(new String[] { "e" }));
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
