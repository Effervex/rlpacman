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
		// Basic test
		RelationalRule rule = new RelationalRule(
				"(clear ?X) (clear ?Y) (above ?X ?G_0) (not (highest ?Y))"
						+ " => (move ?X ?Y)");
		// No more automatically added type conds
		assertFalse(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertFalse(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?Y)")));
		assertEquals(rule.getConditions(false).size(), 6);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(above ?X ?G_0)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(not (highest ?Y))")));

		rule = new RelationalRule("(clear ?X) (clear ?Y) => (move ?X ?Y)");
		assertFalse(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertFalse(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?Y)")));
		assertEquals(rule.getConditions(false).size(), 3);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?Y)")));

		// Variable normalisation
		rule = new RelationalRule("(clear ?A) (clear ?D) => (move ?A ?D)");
		assertEquals(rule.getConditions(false).size(), 3);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?Y)")));

		// Unbound normalisation
		rule = new RelationalRule(
				"(clear ?A) (clear ?D) (on ?A ?Unb_5) => (move ?A ?D)");
		assertEquals(rule.getConditions(false).size(), 5);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?X ?Unb_0)")));

		// Bound normalisation
		rule = new RelationalRule("(clear ?A) (clear ?D) (above ?A ?Bnd_5) "
				+ "(on ?A ?Bnd_5) => (move ?A ?D)");
		assertEquals(rule.getConditions(false).size(), 6);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(above ?X ?Bnd_0)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?X ?Bnd_0)")));

		// Unbound and bound
		rule = new RelationalRule("(clear ?A) (clear ?D) (above ?A ?Bnd_5) "
				+ "(on ?A ?Unb_4) (clear ?Bnd_5) => (move ?A ?D)");
		assertEquals(rule.getConditions(false).size(), 8);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(above ?X ?Bnd_0)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?X ?Unb_0)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?Bnd_0)")));

		// Binding the unbound
		rule = new RelationalRule("(clear ?A) (clear ?D) (above ?A ?Unb_5) "
				+ "(on ?A ?Unb_5) => (move ?A ?D)");
		assertEquals(rule.getConditions(false).size(), 6);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(above ?X ?Bnd_0)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?X ?Bnd_0)")));

		// Unbounding the anonymous
		rule = new RelationalRule("(clear ?A) (clear ?D) (above ?A ?) "
				+ "(on ?A ?) => (move ?A ?D)");
		assertEquals(rule.getConditions(false).size(), 7);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(above ?X ?Unb_0)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?X ?Unb_1)")));

		// Binding negated anonymous
		rule = new RelationalRule("(clear ?A) (clear ?D) "
				+ "(not (on ?A ?)) => (move ?A ?D)");
		assertEquals(rule.getConditions(false).size(), 4);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(not (on ?X ?))")));
		
		// Binding negated unbound (the only way to bind unbound negated variables)
		rule = new RelationalRule("(clear ?A) (clear ?D) "
				+ "(thing ?Unb_0) (not (on ?D ?Unb_0)) => (move ?A ?D)");
		assertEquals(rule.getConditions(false).size(), 6);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(thing ?Bnd_0)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(not (on ?Y ?Bnd_0))")));
		
		// Unbinding lonely variables
		rule = new RelationalRule("(clear ?A) (clear ?D) "
				+ "(not (on ?A ?Bnd_0)) => (move ?A ?D)");
		assertEquals(rule.getConditions(false).size(), 4);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(not (on ?X ?))")));
		
		rule = new RelationalRule("(clear ?A) (clear ?D) "
				+ "(thing ?Bnd_4) (not (on ?A ?Bnd_4)) => (move ?A ?D)");
		assertEquals(rule.getConditions(false).size(), 6);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(thing ?Bnd_0)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(not (on ?X ?Bnd_0))")));
		
		// Negated numerical condition
		rule = new RelationalRule("(clear ?A) (clear ?D) "
				+ "(thing ?Bnd_4) (not (height ?A ?)) => (move ?A ?D)");
		assertEquals(rule.getConditions(false).size(), 6);
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(thing ?Unb_0)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(not (height ?X ?))")));
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
