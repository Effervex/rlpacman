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
						+ "(controls CERRLA0 ?V_1) (placedMeeples CERRLA0 (1.0 <= ?#_9 <= 2.0) ?V_2) "
						+ "(meepleLoc ?B ?C) (tileEdge ?B ?V_0 ?C) (worth ?C (4.0 <= ?#_5 <= 12.0)) "
						+ "=> (placeMeeple CERRLA0 ?B ?C)");
		simplifiedConditions = rule.getSimplifiedConditions(false);
		assertEquals(simplifiedConditions.size(), 12);
		System.out.println(simplifiedConditions);

		StateSpec.initInstance("mario.RLMario", "Diff1");
		lced_ = new LocalCrossEntropyDistribution(
				GoalCondition.parseGoalCondition("Diff1"));
		rule = new RelationalRule(
				"(marioPower fire) (enemy ?A) (test (<> ?A fire)) (distance ?A ?#_41) "
						+ "(width ?A ?#_40&:(range ?#_40min 0.5 ?#_40 ?#_40max 1.0)) "
						+ "=> (shootFireball ?A ?#_41 fire)", lced_);
		simplifiedConditions = rule.getSimplifiedConditions(false);
//		assertEquals(simplifiedConditions.size(), 12);
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
		assertEquals(simplifiedConditions.size(), 3);
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?A)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));

		// Unbound normalisation
		rule = new RelationalRule(
				"(clear ?F) (clear ?G) (on ?F ?V_5) => (move ?F ?G)", lced_);
		assertEquals(simplifiedConditions.size(), 5);
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?A)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(on ?A ?V_0)")));

		// Bound normalisation
		rule = new RelationalRule("(clear ?F) (clear ?G) (above ?F ?V_5) "
				+ "(on ?F ?V_5) => (move ?F ?G)", lced_);
		assertEquals(simplifiedConditions.size(), 6);
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?A)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));
		assertFalse(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(above ?A ?V_0)")));
		assertFalse(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(on ?A ?V_0)")));

		// Unbound and bound
		rule = new RelationalRule("(clear ?F) (clear ?G) (above ?F ?V_5) "
				+ "(on ?F ?V_4) (clear ?V_5) => (move ?F ?G)", lced_);
		assertEquals(simplifiedConditions.size(), 8);
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?A)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(above ?A ?V_0)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(on ?A ?V_0)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?V_0)")));

		// Binding the unbound
		rule = new RelationalRule("(clear ?F) (clear ?G) (above ?F ?V_5) "
				+ "(on ?F ?V_5) => (move ?F ?G)", lced_);
		assertEquals(simplifiedConditions.size(), 6);
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?A)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(above ?A ?V_0)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(on ?A ?V_0)")));

		// Unbounding the anonymous
		rule = new RelationalRule("(clear ?F) (clear ?G) (above ?F ?) "
				+ "(on ?F ?) => (move ?F ?G)", lced_);
		assertEquals(simplifiedConditions.size(), 7);
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?A)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(above ?A ?V_0)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(on ?A ?V_1)")));

		// Binding negated anonymous
		rule = new RelationalRule("(clear ?F) (clear ?G) "
				+ "(not (on ?F ?)) => (move ?F ?G)", lced_);
		assertEquals(simplifiedConditions.size(), 4);
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?A)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(not (on ?A ?))")));

		// Binding negated unbound (the only way to bind unbound negated
		// variables)
		rule = new RelationalRule("(clear ?F) (clear ?G) "
				+ "(thing ?V_0) (not (on ?G ?V_0)) => (move ?F ?G)", lced_);
		assertEquals(simplifiedConditions.size(), 6);
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?A)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(thing ?V_0)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(not (on ?B ?V_0))")));

		// Unbinding lonely variables
		rule = new RelationalRule("(clear ?F) (clear ?G) "
				+ "(not (on ?F ?V_0)) => (move ?F ?G)", lced_);
		assertEquals(simplifiedConditions.size(), 4);
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?A)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(not (on ?A ?))")));

		rule = new RelationalRule("(clear ?F) (clear ?G) "
				+ "(thing ?V_4) (not (on ?F ?V_4)) => (move ?F ?G)", lced_);
		assertEquals(simplifiedConditions.size(), 6);
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?A)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(thing ?V_0)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(not (on ?A ?V_0))")));

		// Negated numerical condition
		rule = new RelationalRule("(clear ?F) (clear ?G) "
				+ "(thing ?V_4) (not (height ?F ?)) => (move ?F ?G)", lced_);
		assertEquals(simplifiedConditions.size(), 6);
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?A)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(clear ?B)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(thing ?V_0)")));
		assertTrue(simplifiedConditions.contains(StateSpec
				.toRelationalPredicate("(not (height ?A ?))")));
	}

	@Test
	public void testNonActionSimplification() {
		// Basic test
		RelationalRule rule = new RelationalRule(
				"(clear ?A) (clear ?B) (above ?A ?V_0) (block ?A) "
						+ "=> (move ?A ?B)", lced_);
		// No more automatically added type conds
		assertFalse(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?B)")));
		assertEquals(rule.getSimplifiedConditions(false).size(), 4);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?A)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?B)")));
		assertFalse(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(above ?A ?V_0)")));

		// Binding
		rule = new RelationalRule("(clear ?V_0) (block ?V_0) => (move ?A ?B)",
				lced_);
		assertEquals(rule.toString(), rule.getSimplifiedConditions(false)
				.size(), 2);
		RelationalArgument[] argsA = { new RelationalArgument("?V_0") };
		argsA[0].setFreeVariable(false);
		RelationalPredicate predA = new RelationalPredicate(StateSpec
				.getInstance().getPredicateByName("clear"), argsA);
		assertTrue(rule.getSimplifiedConditions(false).contains(predA));
		RelationalArgument[] argsB = { new RelationalArgument("?V_0") };
		argsB[0].setFreeVariable(false);
		RelationalPredicate predB = new RelationalPredicate(StateSpec
				.getInstance().getPredicateByName("block"), argsB);
		assertTrue(rule.getSimplifiedConditions(false).contains(predB));

		// Not binding
		rule = new RelationalRule("(clear ?V_0) (block ?V_1) => (move ?A ?B)",
				lced_);
		assertEquals(rule.toString(), rule.getSimplifiedConditions(false)
				.size(), 2);
		argsA[0] = new RelationalArgument("?V_1");
		argsA[0].setFreeVariable(true);
		predA = new RelationalPredicate(StateSpec.getInstance()
				.getPredicateByName("clear"), argsA);
		assertTrue(rule.getSimplifiedConditions(false).contains(predA));
		argsB[0] = new RelationalArgument("?V_0");
		argsB[0].setFreeVariable(true);
		predB = new RelationalPredicate(StateSpec.getInstance()
				.getPredicateByName("block"), argsB);
		assertTrue(rule.getSimplifiedConditions(false).contains(predB));

		// Chain test
		rule = new RelationalRule(
				"(clear ?A) (clear ?B) (above ?A ?Bnd_0) (on ?A ?Bnd_0) "
						+ "(block ?A) => (move ?A ?B)", lced_);
		// No more automatically added type conds
		assertFalse(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?B)")));
		assertEquals(rule.toString(), rule.getSimplifiedConditions(false)
				.size(), 4);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?A)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?B)")));
		assertFalse(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(above ?A ?V_0)")));
		assertFalse(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?A ?V_0)")));
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
				"(clear ?A) (above ?A ?V_1) (block ?A) (on ?A ?V_0) "
						+ "(not (above ?B ?V_2)) (thing ?B) (above ?A ?G_0) "
						+ "(thing ?A) (clear ?B) => (move ?A ?B", lced_);
		simpConds = rule.getSimplifiedConditions(true);
		assertFalse(simpConds.contains(StateSpec
				.toRelationalPredicate("(not (above ?B ?V_0))")));
		assertEquals(simpConds.size(), 3);
	}

	@Test
	public void testInequalityCreation() {
		RelationalRule ruleA = new RelationalRule(
				"(not (on ?A ?V_0)) (not (on ?B ?V_0)) => (move ?A ?B)");
		List<RelationalPredicate> conds = ruleA.getSimplifiedConditions(false);
		assertEquals(conds.size(), 2);
		assertTrue(ruleA.toString(), conds.contains(StateSpec
				.toRelationalPredicate("(not (on ?A ?V_1))")));
		assertTrue(conds.contains(StateSpec
				.toRelationalPredicate("(not (on ?B ?V_1))")));

		ruleA = new RelationalRule(
				"(clear ?A) (block ?V_0) (not (on ?A ?V_0)) (not (on ?B ?V_0)) => (move ?A ?B)");
		conds = ruleA.getSimplifiedConditions(false);
		assertEquals(conds.size(), 5);
		assertTrue(ruleA.toString(),
				conds.contains(StateSpec.toRelationalPredicate("(clear ?A)")));
		assertTrue(conds.contains(StateSpec
				.toRelationalPredicate("(block ?V_1)")));
		assertTrue(ruleA.toString(), conds.contains(StateSpec
				.toRelationalPredicate("(not (on ?A ?V_1))")));
		assertTrue(conds.contains(StateSpec
				.toRelationalPredicate("(not (on ?B ?V_1))")));

		ruleA = new RelationalRule(
				"(not (on ?A ?)) (not (on ?B ?)) => (move ?A ?B)");
		conds = ruleA.getSimplifiedConditions(false);
		assertEquals(conds.size(), 2);
		assertTrue(ruleA.toString(), conds.contains(StateSpec
				.toRelationalPredicate("(not (on ?A ?V_1))")));
		assertTrue(conds.contains(StateSpec
				.toRelationalPredicate("(not (on ?B ?V_0))")));
	}
}
