package test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import cerrla.PolicyGenerator;

import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

public class RelationalRuleTest {
	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld", "onab");
		PolicyGenerator.newInstance(0);
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
}
