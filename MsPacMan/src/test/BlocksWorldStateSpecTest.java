package test;

import static org.junit.Assert.*;

import java.util.*;

import jess.Fact;
import jess.QueryResult;
import jess.Rete;
import jess.ValueVector;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;
import org.mandarax.kernel.LogicFactory;
import org.mandarax.kernel.Rule;
import org.mandarax.reference.DefaultLogicFactory;

import blocksWorld.Block;

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
	public void testParseRule() {
		// Basic variable test
		String rule = spec_.parseRule("(clear ?X) => (moveFloor ?X)");
		String body = rule.split("=>")[0];
		int condCount = body.replaceAll("\\(.+?\\)( |$)", ".").length();
		String head = rule.split("=>")[1];
		// 2 assertions in the body: clear, and block
		assertEquals(condCount, 2);
		assertTrue(body.contains("(clear ?X)"));
		assertTrue(body.contains("(block ?X)"));
		assertTrue(body.indexOf("clear") < body.indexOf("block"));
		assertTrue(head.contains("(moveFloor ?X)"));

		// Test for a constant
		rule = spec_.parseRule("(clear a) => (moveFloor a)");
		body = rule.split("=>")[0];
		condCount = body.replaceAll("\\(.+?\\)( |$)", ".").length();
		head = rule.split("=>")[1];
		// 2 assertions in the body: clear, and block, with a constant
		// variable
		assertEquals(condCount, 2);
		assertTrue(body.contains("(clear a)"));
		assertTrue(body.contains("(block a)"));
		assertTrue(body.indexOf("clear") < body.indexOf("block"));
		assertTrue(head.contains("(moveFloor a)"));

		// Test for constants (no inequals)
		rule = spec_.parseRule("(clear a) (clear b) => (moveFloor a)");
		body = rule.split("=>")[0];
		condCount = body.replaceAll("\\(.+?\\)( |$)", ".").length();
		head = rule.split("=>")[1];
		// 4 assertions in the body: 2 clears, and 2 blocks, without an
		// inequality test
		assertEquals(condCount, 4);
		assertTrue(body.contains("(clear a)"));
		assertTrue(body.contains("(block a)"));
		assertTrue(body.contains("(clear b)"));
		assertTrue(body.contains("(block b)"));
		assertTrue(body.indexOf("clear") < body.indexOf("block"));
		assertTrue(head.contains("(moveFloor a)"));

		// Multiple conditions, one term
		rule = spec_.parseRule("(clear ?X) (highest ?X) => (moveFloor ?X)");
		body = rule.split("=>")[0];
		condCount = body.replaceAll("\\(.+?\\)( |$)", ".").length();
		head = rule.split("=>")[1];
		// 3 assertions in the body: clear, highest, and a single block
		assertEquals(condCount, 3);
		assertTrue(body.contains("(clear ?X)"));
		assertTrue(body.contains("(block ?X)"));
		assertTrue(body.contains("(highest ?X)"));
		assertTrue(body.indexOf("clear") < body.indexOf("block"));
		assertTrue(head.contains("(moveFloor ?X)"));

		// Multiple conditions, two terms
		rule = spec_.parseRule("(clear ?X) (highest ?Y) => (moveFloor ?X)");
		body = rule.split("=>")[0];
		condCount = body.replaceAll("\\(.+?\\)( |$)", ".").length();
		head = rule.split("=>")[1];
		// 5 assertions in the body: clear, highest, two blocks, and an
		// inequals test
		assertEquals(condCount, 5);
		assertTrue(body.contains("(clear ?X)"));
		assertTrue(body.contains("(block ?X)"));
		assertTrue(body.contains("(highest ?Y)"));
		assertTrue(body.contains("(block ?Y)"));
		assertTrue(body.contains("(test (<> ?X ?Y))"));
		assertTrue(body.indexOf("clear") < body.indexOf("test"));
		assertTrue(body.indexOf("test") < body.indexOf("block"));
		assertTrue(head.contains("(moveFloor ?X)"));

		// Variables and constants
		rule = spec_.parseRule("(on ?X a) (above b ?Y) => (moveFloor ?X)");
		body = rule.split("=>")[0];
		condCount = body.replaceAll("\\(.+?\\)( |$)", ".").length();
		head = rule.split("=>")[1];
		// 5 assertions in the body: clear, highest, two blocks, and an
		// inequals test
		assertEquals(condCount, 8);
		assertTrue(body.contains("(on ?X a)"));
		assertTrue(body.contains("(block ?X)"));
		assertTrue(body.contains("(block a)"));
		assertTrue(body.contains("(above b ?Y)"));
		assertTrue(body.contains("(block b)"));
		assertTrue(body.contains("(block ?Y)"));
		assertTrue(body.contains("(test (<> ?X a b ?Y))"));
		assertTrue(body.contains("(test (<> ?Y a b))"));
		assertTrue(body.indexOf("clear") < body.indexOf("test"));
		assertTrue(body.indexOf("test") < body.indexOf("block"));
		assertTrue(head.contains("(moveFloor ?X)"));

		// Test anonymous variable
		rule = spec_.parseRule("(clear ?X) (on ?X _) => (moveFloor ?X)");
		body = rule.split("=>")[0];
		condCount = body.replaceAll("\\(.+?\\)( |$)", ".").length();
		head = rule.split("=>")[1];
		// 5 assertions in the body: clear, on, two blocks, and an
		// inequals
		assertEquals(condCount, 5);
		assertTrue(body.contains("(clear ?X)"));
		assertTrue(body.contains("(block ?X)"));
		assertTrue(body.contains("(on ?X ?_0)"));
		assertTrue(body.contains("(block ?_0)"));
		assertTrue(body.contains("(test (<> ?X ?_0))"));
		assertTrue(body.indexOf("clear") < body.indexOf("test"));
		assertTrue(body.indexOf("test") < body.indexOf("block"));
		assertTrue(head.contains("(moveFloor ?X)"));

		// Test anonymous variables
		rule = spec_
				.parseRule("(clear ?X) (on ?X _) (on ?Y _) => (moveFloor ?X)");
		body = rule.split("=>")[0];
		condCount = body.replaceAll("\\(.+?\\)( |$)", ".").length();
		head = rule.split("=>")[1];
		// 10 assertions in the body: clear, 2 ons, 4 blocks, and 3
		// inequals
		// Note no inequals between _1 and _2
		assertEquals(condCount, 10);
		assertTrue(body.contains("(clear ?X)"));
		assertTrue(body.contains("(block ?X)"));
		assertTrue(body.contains("(on ?X ?_0)"));
		assertTrue(body.contains("(block ?_0)"));
		assertTrue(body.contains("(on ?Y ?_1)"));
		assertTrue(body.contains("(block ?Y)"));
		assertTrue(body.contains("(block ?_1)"));
		assertTrue(body.contains("(test (<> ?X ?_0 ?Y ?_1))"));
		assertTrue(body.contains("(test (<> ?_0 ?Y ?_1))"));
		assertTrue(body.contains("(test (<> ?Y ?_1))"));
		assertTrue(body.indexOf("clear") < body.indexOf("test"));
		assertTrue(body.indexOf("test") < body.indexOf("block"));
		assertTrue(head.contains("(moveFloor ?X)"));

		// Test type predicate
		rule = spec_.parseRule("(block ?X) => (moveFloor ?X)");
		body = rule.split("=>")[0];
		condCount = body.replaceAll("\\(.+?\\)( |$)", ".").length();
		head = rule.split("=>")[1];
		assertEquals(condCount, 1);
		assertTrue(body.contains("(block ?X)"));
		assertTrue(head.contains("(moveFloor ?X)"));

		// Test inequal type predicates
		rule = spec_.parseRule("(block ?X) (block ?Y) => (move ?X ?Y)");
		body = rule.split("=>")[0];
		condCount = body.replaceAll("\\(.+?\\)( |$)", ".").length();
		head = rule.split("=>")[1];
		assertEquals(condCount, 3);
		assertTrue(body.contains("(block ?X)"));
		assertTrue(body.contains("(block ?Y)"));
		assertTrue(body.contains("(test (<> ?X ?Y))"));
		assertTrue(head.contains("(move ?X ?Y)"));

		// Test existing type predicate
		rule = spec_.parseRule("(clear ?X) (block ?X) => (moveFloor ?X)");
		body = rule.split("=>")[0];
		condCount = body.replaceAll("\\(.+?\\)( |$)", ".").length();
		head = rule.split("=>")[1];
		assertEquals(condCount, 2);
		assertTrue(body.contains("(block ?X)"));
		assertTrue(body.contains("(clear ?X)"));
		assertTrue(head.contains("(moveFloor ?X)"));
	}

	@Test
	public void testToString() {
		assertEquals("StateSpec", spec_.toString());
	}

	@Test
	public void testEncodeRule() {
		String result = spec_
				.encodeRule("(block a) (clear a) => (moveFloor a)");
		assertTrue(result.equals("(clear a) => (moveFloor a)"));

		result = spec_.encodeRule("(block a) (block b) (on a b) => (move a b)");
		assertTrue(result.equals("(on a b) => (move a b)"));

		result = spec_.encodeRule("(block ?X) (clear ?X) => (moveFloor ?X)");
		assertTrue(result.equals("(clear ?X) => (moveFloor ?X)"));

		result = spec_.encodeRule("(block ?X) (block ?Y) (test (<> ?X ?Y)) "
				+ "(clear ?X) (clear ?Y) => (move ?X ?Y)");
		assertTrue(result.equals("(clear ?X) (clear ?Y) => (move ?X ?Y)"));

		result = spec_.encodeRule("(block a) => (moveFloor a)");
		assertTrue(result.equals("(block a) => (moveFloor a)"));
	}

	@Test
	public void testInsertValidActions() throws Exception {
		Rete state = spec_.getRete();

		// Set up the query to find the facts
		state.eval("(defquery listActions (" + StateSpec.VALID_ACTIONS
				+ " (move $?X) (moveFloor $?Y)))");

		// Empty case
		spec_.insertValidActions(state);
		state.eval("(facts)");
		QueryResult result = state.runQueryStar("listActions",
				new ValueVector());
		assertTrue(result.next());
		String moveResult = result.get("X").toString();
		assertEquals("", moveResult);
		String moveFloorResult = result.get("Y").toString();
		assertEquals("", moveFloorResult);
		state.reset();

		// Simple move case
		state.eval("(assert (clear a))");
		state.eval("(assert (clear b))");
		spec_.insertValidActions(state);
		state.eval("(facts)");
		result = state.runQueryStar("listActions", new ValueVector());
		assertTrue(result.next());
		moveResult = result.get("X").toString();
		assertTrue(moveResult.contains("\"a b\""));
		assertTrue(moveResult.contains("\"b a\""));
		moveFloorResult = result.get("Y").toString();
		assertEquals("", result.get("Y").toString());
		state.reset();

		// Simple moveFloor case
		state.eval("(assert (clear v))");
		state.eval("(assert (on v y))");
		spec_.insertValidActions(state);
		state.eval("(facts)");
		result = state.runQueryStar("listActions", new ValueVector());
		assertTrue(result.next());
		moveResult = result.get("X").toString();
		assertEquals("", result.get("X").toString());
		moveFloorResult = result.get("Y").toString();
		assertEquals("\"v\"", result.get("Y").toString());
		state.reset();

		// Complex both case
		state.eval("(assert (clear d))");
		state.eval("(assert (clear e))");
		state.eval("(assert (clear c))");
		state.eval("(assert (on e b))");
		state.eval("(assert (on d a))");
		state.eval("(assert (onFloor c))");
		spec_.insertValidActions(state);
		state.eval("(facts)");
		result = state.runQueryStar("listActions", new ValueVector());
		assertTrue(result.next());
		moveResult = result.get("X").toString();
		assertTrue(moveResult.contains("\"d e\""));
		assertTrue(moveResult.contains("\"d c\""));
		assertTrue(moveResult.contains("\"e d\""));
		assertTrue(moveResult.contains("\"e c\""));
		assertTrue(moveResult.contains("\"c d\""));
		assertTrue(moveResult.contains("\"c e\""));
		moveFloorResult = result.get("Y").toString();
		assertTrue(moveFloorResult.contains("\"d\""));
		assertTrue(moveFloorResult.contains("\"e\""));
	}
	
	@Test
	public void testSplitFact() {
		// Basic
		String[] result = StateSpec.splitFact("(clear a)");
		assertArrayEquals(new String[] {"clear", "a"}, result);
		
		// More complex
		result = StateSpec.splitFact("(on a b)");
		assertArrayEquals(new String[] {"on", "a", "b"}, result);
		
		// No parentheses
		result = StateSpec.splitFact("clear a");
		assertArrayEquals(new String[] {"clear", "a"}, result);
		
		// Module declaration
		result = StateSpec.splitFact("(MAIN::clear a)");
		assertArrayEquals(new String[] {"clear", "a"}, result);
	}
}
