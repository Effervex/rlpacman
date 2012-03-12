package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import relationalFramework.RelationalPredicate;
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

		rule = new RelationalRule("(clear ?X) (clear ?Y) => (move ?X ?Y)");
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

	@Test
	public void testVariableNormalisation() {
		// No change
		String ruleString = "(clear ?X) (clear ?Y) (on ?X ?Z) => (move ?X ?Y)";
		RelationalRule rule = new RelationalRule(ruleString);
		assertEquals(rule.toNiceString(), ruleString);

		// Normalised variable
		String ruleString2 = "(clear ?X) (clear ?Y) (on ?X ?A) => (move ?X ?Y)";
		rule = new RelationalRule(ruleString2);
		assertEquals(rule.toNiceString(), ruleString);

		// Multiple variables
		ruleString = "(clear ?X) (clear ?Y) (on ?X ?R) (on ?Y ?K) => (move ?X ?Y)";
		rule = new RelationalRule(ruleString);
		ruleString2 = "(clear ?X) (clear ?Y) (on ?X ?Z) (on ?Y ?A) => (move ?X ?Y)";
		assertEquals(rule.toNiceString(), ruleString2);

		// Unbound variables
		String unboundRuleA = "(clear ?X) (clear ?Y) (on ?X ?A_Unb) => (move ?X ?Y)";
		String unboundRuleB = "(clear ?X) (clear ?Y) (on ?X ?Z_Unb) => (move ?X ?Y)";
		rule = new RelationalRule(unboundRuleA);
		assertEquals(rule.toNiceString(), unboundRuleB);
		rule = new RelationalRule(unboundRuleB);
		assertEquals(rule.toNiceString(), unboundRuleB);

		// Anonymous variables
		String anonRule = "(clear ?X) (clear ?Y) (on ?X ?) => (move ?X ?Y)";
		rule = new RelationalRule(anonRule);
		assertEquals(rule.toNiceString(), unboundRuleB);

		// Multiple anonymous variables
		String multiAnonRule = "(clear ?X) (clear ?Y) (above ?Y ?) (on ?X ?) => (move ?X ?Y)";
		rule = new RelationalRule(multiAnonRule);
		String unboundAnonRule = "(clear ?X) (clear ?Y) (above ?Y ?Z_Unb) (on ?X ?A_Unb) => (move ?X ?Y)";
		assertEquals(rule.toNiceString(), unboundAnonRule);
	}
}
