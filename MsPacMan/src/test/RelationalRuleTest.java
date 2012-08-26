package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import org.junit.Before;
import org.junit.Test;

import cerrla.LocalCrossEntropyDistribution;
import cerrla.modular.GoalCondition;


public class RelationalRuleTest {
	private LocalCrossEntropyDistribution lced_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld", "onab");
		lced_ = new LocalCrossEntropyDistribution(
				GoalCondition.parseGoalCondition("on$A$B"));
	}

	@Test
	public void testParseRule() {
		RelationalRule rule = new RelationalRule(
				"(above ?A ?G_0) (above ?B ?G_1) (clear ?A) (clear ?B) => (move ?A ?B)",
				lced_);
		List<RelationalPredicate> simplifiedConditions = rule
				.getSimplifiedConditions(false);
		assertEquals(simplifiedConditions.size(), 6);
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(above ?A ?G_0)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(above ?B ?G_1)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?A)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(test (<> ?A ?G_0 ?G_1))")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(test (<> ?B ?A ?G_0 ?G_1))")));

		StateSpec.initInstance("jCloisterZone.Carcassonne", "onab");
		lced_ = new LocalCrossEntropyDistribution(
				GoalCondition.parseGoalCondition("SinglePlayer"));
		rule = new RelationalRule(
				"(player CERRLA0) (meeplesLeft CERRLA0 (2.5 <= ?#_4 <= 5.5)) "
						+ "(controls CERRLA0 ?) (placedMeeples CERRLA0 (1.0 <= ?#_9 <= 2.0) ?) "
						+ "(meepleLoc ?B ?C) (tileEdge ?B ? ?C) (worth ?C (4.0 <= ?#_5 <= 12.0)) "
						+ "=> (placeMeeple CERRLA0 ?B ?C)");
		simplifiedConditions = rule.getSimplifiedConditions(false);
		assertEquals(simplifiedConditions.toString(),
				simplifiedConditions.size(), 9);

		StateSpec.initInstance("mario.RLMario", "Diff1");
		lced_ = new LocalCrossEntropyDistribution(
				GoalCondition.parseGoalCondition("Diff1"));
		rule = new RelationalRule(
				"(marioPower fire) (enemy ?A) (test (<> ?A fire)) (distance ?A ?#_41) "
						+ "(width ?A ?#_40&:(range ?#_40min 0.5 ?#_40 ?#_40max 1.0)) "
						+ "=> (shootFireball ?A ?#_41 fire)", lced_);
		simplifiedConditions = rule.getSimplifiedConditions(false);
		// assertEquals(simplifiedConditions.size(), 12);
		System.out.println(simplifiedConditions);
		System.out.println(rule.getRangeContexts());
	}

	@Test
	public void testExpandConditions() {
		// Basic test
		RelationalRule rule = new RelationalRule(
				"(clear ?A) (clear ?B) (above ?A ?G_0) (not (highest ?B))"
						+ " => (move ?A ?B)", lced_);
		// No more automatically added type conds
		List<RelationalPredicate> simplifiedConditions = rule
				.getSimplifiedConditions(false);
		assertFalse(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(block ?A)")));
		assertFalse(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(block ?B)")));
		assertEquals(simplifiedConditions.size(), 6);
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?A)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(above ?A ?G_0)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(not (highest ?B))")));

		rule = new RelationalRule("(clear ?A) (clear ?B) => (move ?A ?B)",
				lced_);
		simplifiedConditions = rule.getSimplifiedConditions(false);
		assertFalse(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(block ?A)")));
		assertFalse(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(block ?B)")));
		assertEquals(simplifiedConditions.size(), 3);
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?A)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));

		// Variable normalisation
		rule = new RelationalRule("(clear ?F) (clear ?G) => (move ?F ?G)",
				lced_);
		simplifiedConditions = rule.getSimplifiedConditions(false);
		assertEquals(simplifiedConditions.size(), 3);
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?A)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));
	}

	@Test
	public void testConditionOrdering() {
		Collection<RelationalPredicate> conditions = new HashSet<RelationalPredicate>();
		conditions.add(StateSpec.toRelationalPredicate("(above ?A ?G_0)"));
		conditions.add(StateSpec.toRelationalPredicate("(clear ?A)"));
		conditions.add(StateSpec.toRelationalPredicate("(clear ?G_1)"));
		conditions.add(StateSpec.toRelationalPredicate("(above ?G_1 ?)"));
		conditions.add(StateSpec.toRelationalPredicate("(above ?A ?B)"));
		conditions.add(StateSpec.toRelationalPredicate("(on ?A ?)"));
		RelationalPredicate action = StateSpec
				.toRelationalPredicate("(move ?A ?G_1)");
		RelationalRule rule = new RelationalRule(conditions, action, null, null);
		List<RelationalPredicate> ruleConditions = rule
				.getSimplifiedConditions(true);
		// No more automatically added type conds
		assertEquals(ruleConditions.get(0).toString(), "(clear ?G_1)");
		assertEquals(ruleConditions.get(1).toString(), "(above ?A ?G_0)");
		assertEquals(ruleConditions.get(2).toString(), "(above ?G_1 ?)");
		assertEquals(ruleConditions.get(3).toString(), "(above ?A ?C)");
		assertEquals(ruleConditions.get(4).toString(), "(clear ?A)");
		assertEquals(ruleConditions.get(5).toString(), "(on ?A ?)");
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
		Collection<RelationalPredicate> conditions = new ArrayList<RelationalPredicate>();
		conditions.add(StateSpec.toRelationalPredicate("(clear ?A)"));
		conditions.add(StateSpec.toRelationalPredicate("(clear ?B)"));
		conditions.add(StateSpec.toRelationalPredicate("(block ?A)"));
		conditions.add(StateSpec.toRelationalPredicate("(thing ?B)"));
		RelationalRule rule = new RelationalRule(conditions,
				StateSpec.toRelationalPredicate("(move ?A ?B)"), null, lced_);
		List<RelationalPredicate> simpConds = rule
				.getSimplifiedConditions(true);
		assertEquals(simpConds.size(), 3);
		assertTrue(simpConds.contains(StateSpec
				.toRelationalPredicate("(clear ?A)")));
		assertTrue(simpConds.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));
		assertTrue(simpConds.contains(StateSpec
				.toRelationalPredicate("(block ?A)")));

		conditions = new ArrayList<RelationalPredicate>();
		conditions.add(StateSpec.toRelationalPredicate("(clear ?A)"));
		conditions.add(StateSpec.toRelationalPredicate("(floor ?B)"));
		conditions.add(StateSpec.toRelationalPredicate("(above ?A ?G_0)"));
		conditions.add(StateSpec.toRelationalPredicate("(not (highest ?B))"));
		rule = new RelationalRule(conditions,
				StateSpec.toRelationalPredicate("(move ?A ?B)"), null, lced_);
		simpConds = rule.getSimplifiedConditions(true);
		assertEquals(simpConds.size(), 4);

		rule = new RelationalRule(
				"(clear ?A) (above ?A ?) (block ?A) (on ?A ?) "
						+ "(not (above ?B ?)) (thing ?B) (above ?A ?G_0) "
						+ "(thing ?A) (clear ?B) => (move ?A ?B", lced_);
		simpConds = rule.getSimplifiedConditions(true);
		assertFalse(simpConds.contains(StateSpec
				.toRelationalPredicate("(not (above ?B ?))")));
		assertEquals(simpConds.size(), 4);

		// Floor rule
		rule = new RelationalRule(
				"(clear ?A) (clear ?B) (block ?A) (thing ?A) (thing ?B) "
						+ "(above ?A ?) (on ?A ?) (floor ?B) (above ?G_1 ?B) => (move ?A ?B)",
				lced_);
		simpConds = rule.getSimplifiedConditions(true);
		assertTrue(simpConds.toString(), simpConds.contains(StateSpec
				.toRelationalPredicate("(floor ?B)")));
		assertFalse(simpConds.toString(), simpConds.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));
		assertFalse(simpConds.toString(), simpConds.contains(StateSpec
				.toRelationalPredicate("(above ?G_1 ?B)")));
		assertEquals(simpConds.toString(), simpConds.size(), 3);
		
		// Floor rule (more complex)
		rule = new RelationalRule(
				"(clear ?A) (clear ?B) (block ?A) (thing ?A) (thing ?B) "
						+ "(above ?A ?) (on ?A ?) (above ?B ?G_0) (above ?G_1 ?B) => (move ?A ?B)",
				lced_);
		simpConds = rule.getSimplifiedConditions(true);
		assertTrue(simpConds.toString(), simpConds.contains(StateSpec
				.toRelationalPredicate("(floor ?B)")));
		assertFalse(simpConds.toString(), simpConds.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));
		assertFalse(simpConds.toString(), simpConds.contains(StateSpec
				.toRelationalPredicate("(above ?G_1 ?B)")));
		assertFalse(simpConds.toString(), simpConds.contains(StateSpec
				.toRelationalPredicate("(above ?B ?G_0)")));
		assertEquals(simpConds.toString(), simpConds.size(), 3);
	}

	@Test
	public void testInequalityCreation() {
		RelationalRule ruleA = new RelationalRule(
				"(not (on ?A ?)) (not (on ?B ?)) => (move ?A ?B)");
		List<RelationalPredicate> conds = ruleA.getSimplifiedConditions(false);
		assertEquals(conds.size(), 2);
		assertTrue(ruleA.toString(), conds.contains(StateSpec
				.toRelationalPredicate("(not (on ?A ?))")));
		assertTrue(conds.contains(StateSpec
				.toRelationalPredicate("(not (on ?B ?))")));

		ruleA = new RelationalRule(
				"(clear ?A) (block ?) (not (on ?A ?)) (not (on ?B ?)) => (move ?A ?B)");
		conds = ruleA.getSimplifiedConditions(false);
		assertEquals(conds.size(), 5);
		assertTrue(ruleA.toString(),
				conds.contains(StateSpec.toRelationalPredicate("(clear ?A)")));
		assertTrue(conds.contains(StateSpec.toRelationalPredicate("(block ?)")));
		assertTrue(ruleA.toString(), conds.contains(StateSpec
				.toRelationalPredicate("(not (on ?A ?))")));
		assertTrue(conds.contains(StateSpec
				.toRelationalPredicate("(not (on ?B ?))")));

		ruleA = new RelationalRule(
				"(not (on ?A ?)) (not (on ?B ?)) => (move ?A ?B)");
		conds = ruleA.getSimplifiedConditions(false);
		assertEquals(conds.size(), 2);
		assertTrue(ruleA.toString(), conds.contains(StateSpec
				.toRelationalPredicate("(not (on ?A ?))")));
		assertTrue(conds.contains(StateSpec
				.toRelationalPredicate("(not (on ?B ?))")));
	}
}
