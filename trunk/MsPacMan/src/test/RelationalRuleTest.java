package test;

import static org.junit.Assert.*;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import org.junit.Before;
import org.junit.Test;


public class RelationalRuleTest {
	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld", "onab");
	}

	@Test
	public void testExpandConditions() {
		RelationalRule rule = new RelationalRule(
				"(clear ?X) (clear ?Y) (above ?X ?G_0) (not (highest ?Y))"
						+ " => (move ?X ?Y)");
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?Y)")));
		
		rule = new RelationalRule(
				"(clear ?X) (clear ?Y) => (move ?X ?Y)");
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertFalse(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?Y)")));
	}
	
	@Test
	public void testHashCode() {
		RelationalRule ruleA = new RelationalRule(
				"(clear ?X) (clear ?Y) (above ?X ?G_0) (not (highest ?Y))"
						+ " => (move ?X ?Y)");
		RelationalRule ruleB = new RelationalRule(
				"(clear ?X) (clear ?Y) (above ?X ?G_0) (not (highest ?Y))"
						+ " => (move ?X ?Y)");
		assertEquals(ruleA, ruleB);
		assertEquals(ruleA.hashCode(), ruleB.hashCode(), 0);
		
		RelationalRule ruleC = new RelationalRule(
				"(clear ?X) (clear ?Y) (above ?X ?G_1) (not (highest ?Y))"
						+ " => (move ?X ?Y)");
		assertFalse(ruleC.equals(ruleA));
		assertFalse(ruleC.equals(ruleB));
		assertTrue(ruleC.hashCode() > ruleA.hashCode());
	}
}
