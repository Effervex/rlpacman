package test;

import static org.junit.Assert.*;

import java.util.*;

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
		spec_ = StateSpec.initInstance("blocksWorld.BlocksWorld", LogicFactory
				.getDefaultFactory());
	}

	@Test
	public void testInitialise() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitialiseTypePredicates() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitialisePredicates() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitialiseActions() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitialiseGoalState() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitialiseOptimalPolicy() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitialiseBackgroundKnowledge() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddConstant() {
		fail("Not yet implemented");
	}

	@Test
	public void testParseRule() {
		// Basic variable test
		String rule = spec_.parseRule("(clear ?X) => (moveFloor ?X)");
		String body = rule.split("=>")[0];
		int condCount = body.replaceAll("\\(.+?\\) ", ".").length();
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
		condCount = body.replaceAll("\\(.+?\\) ", ".").length();
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
		condCount = body.replaceAll("\\(.+?\\) ", ".").length();
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
		condCount = body.replaceAll("\\(.+?\\) ", ".").length();
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
		condCount = body.replaceAll("\\(.+?\\) ", ".").length();
		head = rule.split("=>")[1];
		// 5 assertions in the body: clear, highest, two blocks, and an
		// inequals test
		assertEquals(condCount, 5);
		assertTrue(body.contains("(clear ?X)"));
		assertTrue(body.contains("(block ?X)"));
		assertTrue(body.contains("(highest ?Y)"));
		assertTrue(body.contains("(block ?Y)"));
		assertTrue(body.contains("(test (<> ?X ?Y)"));
		assertTrue(body.indexOf("clear") < body.indexOf("test"));
		assertTrue(body.indexOf("test") < body.indexOf("block"));
		assertTrue(head.contains("(moveFloor ?X)"));
		
		// Variables and constants
		rule = spec_.parseRule("(on ?X a) (above b ?Y) => (moveFloor ?X)");
		body = rule.split("=>")[0];
		condCount = body.replaceAll("\\(.+?\\) ", ".").length();
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
		assertTrue(body.contains("(test (<> ?X a b ?Y)"));
		assertTrue(body.contains("(test (<> ?Y a b)"));
		assertTrue(body.indexOf("clear") < body.indexOf("test"));
		assertTrue(body.indexOf("test") < body.indexOf("block"));
		assertTrue(head.contains("(moveFloor ?X)"));

		// Test anonymous variable
		rule = spec_.parseRule("(clear ?X) (on ?X _) => (moveFloor ?X)");
		body = rule.split("=>")[0];
		condCount = body.replaceAll("\\(.+?\\) ", ".").length();
		head = rule.split("=>")[1];
		// 5 assertions in the body: clear, on, two blocks, and an
		// inequals
		assertEquals(condCount, 5);
		assertTrue(body.contains("(clear ?X)"));
		assertTrue(body.contains("(block ?X)"));
		assertTrue(body.contains("(on ?X ?_0)"));
		assertTrue(body.contains("(block ?_0)"));
		assertTrue(body.contains("(test (<> ?X ?_0)"));
		assertTrue(body.indexOf("clear") < body.indexOf("test"));
		assertTrue(body.indexOf("test") < body.indexOf("block"));
		assertTrue(head.contains("(moveFloor ?X)"));

		// Test anonymous variables
		rule = spec_
				.parseRule("(clear ?X) (on ?X _) (on ?Y _) => (moveFloor ?X)");
		body = rule.split("=>")[0];
		condCount = body.replaceAll("\\(.+?\\) ", ".").length();
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
		assertTrue(body.contains("(test (<> ?X ?_0 ?Y ?_1)"));
		assertTrue(body.contains("(test (<> ?_0 ?Y ?_1)"));
		assertTrue(body.contains("(test (<> ?Y ?_1)"));
		assertTrue(body.indexOf("clear") < body.indexOf("test"));
		assertTrue(body.indexOf("test") < body.indexOf("block"));
		assertTrue(head.contains("(moveFloor ?X)"));

		// Test type predicate
		rule = spec_.parseRule("(block ?X) => (moveFloor ?X)");
		body = rule.split("=>")[0];
		condCount = body.replaceAll("\\(.+?\\) ", ".").length();
		head = rule.split("=>")[1];
		assertEquals(condCount, 1);
		assertTrue(body.contains("(block ?X)"));
		assertTrue(head.contains("(moveFloor ?X)"));

		// Test inequal type predicates
		rule = spec_.parseRule("(block ?X) (block ?Y) => (move ?X ?Y)");
		body = rule.split("=>")[0];
		condCount = body.replaceAll("\\(.+?\\) ", ".").length();
		head = rule.split("=>")[1];
		assertEquals(condCount, 3);
		assertTrue(body.contains("(block ?X)"));
		assertTrue(body.contains("(block ?Y)"));
		assertTrue(body.contains("(test (<> ?X ?Y)"));
		assertTrue(head.contains("(move ?X ?Y)"));

		// Test existing type predicate
		rule = spec_.parseRule("(clear ?X) (block ?X) => (moveFloor ?X)");
		body = rule.split("=>")[0];
		condCount = body.replaceAll("\\(.+?\\) ", ".").length();
		head = rule.split("=>")[1];
		assertEquals(condCount, 2);
		assertTrue(body.contains("(block ?X)"));
		assertTrue(body.contains("(clear ?X)"));
		assertTrue(head.contains("(moveFloor ?X)"));
	}

	@Test
	public void testGetPredicates() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetActions() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetGoalState() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetOptimalPolicy() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetBackgroundKnowledge() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetConstants() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetTypePredicate() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsTypePredicate() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetGuidedPredicate() {
		fail("Not yet implemented");
	}

	@Test
	public void testToString() {
		assertEquals("StateSpec", spec_.toString());
	}

	@Test
	public void testCreateDefinedPredicate() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreateTied() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreateTiedAndFree() {
		fail("Not yet implemented");
	}

	@Test
	public void testInsertState() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetTerminalFact() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetStateTerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSpecTerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetInequalityPredicate() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddContains() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetInstance() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitInstance() {
		fail("Not yet implemented");
	}

	@Test
	public void testEncodeRule() {
		String result = StateSpec
				.encodeRule("(block a) (clear a) => (moveFloor a)");
		assertTrue(result.equals("(clear a) => (moveFloor a)"));

		result = StateSpec
				.encodeRule("(block a) (block b) (on a b) => (move a b)");
		assertTrue(result.equals("(on a b) => (move a b)"));

		result = StateSpec
				.encodeRule("(block ?X) (clear ?X) => (moveFloor ?X)");
		assertTrue(result.equals("(clear ?X) => (moveFloor ?X)"));

		result = StateSpec
				.encodeRule("(block ?X) (block ?Y) (test (<> ?X ?Y)) "
						+ "(clear ?X) (clear ?Y) => (move ?X ?Y)");
		assertTrue(result.equals("(clear ?X) (clear ?Y) => (move ?X ?Y)"));
	}

	@Test
	public void testLightenFact() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddKBFact() {
		fail("Not yet implemented");
	}

	@Test
	public void testInequal() {
		fail("Not yet implemented");
	}

}
