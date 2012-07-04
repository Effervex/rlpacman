package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import org.junit.Before;
import org.junit.Test;

import cerrla.LocalCrossEntropyDistribution;
import cerrla.modular.GoalCondition;


public class RelationalRuleTest {
	@Before
	public void setUp() throws Exception {
		
		StateSpec.initInstance("blocksWorldMove.BlocksWorld", "onab");
	}

	@Test
	public void testExpandConditions() {
		// Basic test
		RelationalRule rule = new RelationalRule(
				"(clear ?A) (clear ?B) (above ?A ?G_0) (not (highest ?B))"
						+ " => (move ?A ?B)");
		// No more automatically added type conds
		assertFalse(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?A)")));
		assertFalse(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?B)")));
		assertEquals(rule.getSimplifiedConditions(false).size(), 6);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?A)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?B)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(above ?A ?G_0)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(not (highest ?B))")));

		rule = new RelationalRule("(clear ?A) (clear ?B) => (move ?A ?B)");
		assertFalse(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?A)")));
		assertFalse(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?B)")));
		assertEquals(rule.getSimplifiedConditions(false).size(), 3);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?A)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?B)")));

		// Variable normalisation
		rule = new RelationalRule("(clear ?F) (clear ?G) => (move ?F ?G)");
		assertEquals(rule.getSimplifiedConditions(false).size(), 3);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?A)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?B)")));

		// Unbound normalisation
		rule = new RelationalRule(
				"(clear ?F) (clear ?G) (on ?F ?V_5) => (move ?F ?G)");
		assertEquals(rule.getSimplifiedConditions(false).size(), 5);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?A)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?B)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?A ?V_0)")));

		// Bound normalisation
		rule = new RelationalRule("(clear ?F) (clear ?G) (above ?F ?V_5) "
				+ "(on ?F ?V_5) => (move ?F ?G)");
		assertEquals(rule.getSimplifiedConditions(false).size(), 6);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?A)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?B)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(above ?A ?V_0)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?A ?V_0)")));

		// Unbound and bound
		rule = new RelationalRule("(clear ?F) (clear ?G) (above ?F ?V_5) "
				+ "(on ?F ?V_4) (clear ?V_5) => (move ?F ?G)");
		assertEquals(rule.getSimplifiedConditions(false).size(), 8);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?A)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?B)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(above ?A ?V_0)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?A ?V_0)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?V_0)")));

		// Binding the unbound
		rule = new RelationalRule("(clear ?F) (clear ?G) (above ?F ?V_5) "
				+ "(on ?F ?V_5) => (move ?F ?G)");
		assertEquals(rule.getSimplifiedConditions(false).size(), 6);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?A)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?B)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(above ?A ?V_0)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?A ?V_0)")));

		// Unbounding the anonymous
		rule = new RelationalRule("(clear ?F) (clear ?G) (above ?F ?) "
				+ "(on ?F ?) => (move ?F ?G)");
		assertEquals(rule.getSimplifiedConditions(false).size(), 7);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?A)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?B)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(above ?A ?V_0)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?A ?V_1)")));

		// Binding negated anonymous
		rule = new RelationalRule("(clear ?F) (clear ?G) "
				+ "(not (on ?F ?)) => (move ?F ?G)");
		assertEquals(rule.getSimplifiedConditions(false).size(), 4);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?A)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?B)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(not (on ?A ?))")));

		// Binding negated unbound (the only way to bind unbound negated
		// variables)
		rule = new RelationalRule("(clear ?F) (clear ?G) "
				+ "(thing ?V_0) (not (on ?G ?V_0)) => (move ?F ?G)");
		assertEquals(rule.getSimplifiedConditions(false).size(), 6);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?A)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?B)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(thing ?V_0)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(not (on ?B ?V_0))")));

		// Unbinding lonely variables
		rule = new RelationalRule("(clear ?F) (clear ?G) "
				+ "(not (on ?F ?V_0)) => (move ?F ?G)");
		assertEquals(rule.getSimplifiedConditions(false).size(), 4);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?A)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?B)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(not (on ?A ?))")));

		rule = new RelationalRule("(clear ?F) (clear ?G) "
				+ "(thing ?V_4) (not (on ?F ?V_4)) => (move ?F ?G)");
		assertEquals(rule.getSimplifiedConditions(false).size(), 6);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?A)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?B)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(thing ?V_0)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(not (on ?A ?V_0))")));

		// Negated numerical condition
		rule = new RelationalRule("(clear ?F) (clear ?G) "
				+ "(thing ?V_4) (not (height ?F ?)) => (move ?F ?G)");
		assertEquals(rule.getSimplifiedConditions(false).size(), 6);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?A)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?B)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(thing ?V_0)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(not (height ?A ?))")));
	}

	@Test
	public void testConditionOrdering() {
		Collection<RelationalPredicate> conditions = new HashSet<RelationalPredicate>();
		conditions.add(StateSpec.toRelationalPredicate("(above ?A ?G_0)"));
		conditions.add(StateSpec.toRelationalPredicate("(clear ?A)"));
		conditions.add(StateSpec.toRelationalPredicate("(clear ?G_1)"));
		conditions.add(StateSpec.toRelationalPredicate("(above ?G_1 ?V_0)"));
		conditions.add(StateSpec.toRelationalPredicate("(above ?A ?B)"));
		conditions.add(StateSpec.toRelationalPredicate("(on ?A ?V_1)"));
		RelationalPredicate action = StateSpec
				.toRelationalPredicate("(move ?A ?G_1)");
		RelationalRule rule = new RelationalRule(conditions, action, null, null);
		List<RelationalPredicate> ruleConditions = rule
				.getSimplifiedConditions(true);
		// No more automatically added type conds
		assertEquals(ruleConditions.get(0).toString(), "(clear ?G_1)");
		assertEquals(ruleConditions.get(1).toString(), "(above ?A ?G_0)");
		assertEquals(ruleConditions.get(2).toString(), "(above ?G_1 ?V_1)");
		assertEquals(ruleConditions.get(3).toString(), "(clear ?A)");
		assertEquals(ruleConditions.get(4).toString(), "(above ?A ?C)");
		assertEquals(ruleConditions.get(5).toString(), "(on ?A ?V_0)");
	}

	@Test
	public void testHashCode() {
		RelationalRule ruleA = new RelationalRule(
				"(clear ?A) (clear ?B) (above ?A ?G_0) (not (highest ?B))"
						+ " => (move ?A ?B)");
		RelationalRule ruleB = new RelationalRule(
				"(clear ?A) (clear ?B) (above ?A ?G_0) (not (highest ?B))"
						+ " => (move ?A ?B)");
		assertEquals(ruleA, ruleB);
		assertEquals(ruleA.hashCode(), ruleB.hashCode(), 0);

		RelationalRule ruleC = new RelationalRule(
				"(clear ?A) (clear ?B) (above ?A ?G_1) (not (highest ?B))"
						+ " => (move ?A ?B)");
		assertFalse(ruleC.equals(ruleA));
		assertFalse(ruleC.equals(ruleB));
		assertTrue(ruleC.hashCode() > ruleA.hashCode());
	}

	@Test
	public void testRuleSimplification() {
		LocalCrossEntropyDistribution lced = new LocalCrossEntropyDistribution(
				GoalCondition.parseGoalCondition("on$A$B"));
		Collection<RelationalPredicate> conditions = new ArrayList<RelationalPredicate>();
		conditions.add(StateSpec.toRelationalPredicate("(clear ?A)"));
		conditions.add(StateSpec.toRelationalPredicate("(clear ?B)"));
		conditions.add(StateSpec.toRelationalPredicate("(block ?A)"));
		conditions.add(StateSpec.toRelationalPredicate("(thing ?B)"));
		RelationalRule rule = new RelationalRule(conditions,
				StateSpec.toRelationalPredicate("(move ?A ?B)"), null, lced);
		List<RelationalPredicate> simpConds = rule
				.getSimplifiedConditions(true);
		assertEquals(simpConds.size(), 4);
	}
}
