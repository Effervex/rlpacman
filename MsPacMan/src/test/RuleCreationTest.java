package test;

import static org.junit.Assert.*;

import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

import cerrla.modular.GoalCondition;

import relationalFramework.agentObservations.LocalAgentObservations;

public class RuleCreationTest {
	private LocalAgentObservations.RuleMutation sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld", "onab");
		LocalAgentObservations lao = LocalAgentObservations
				.loadAgentObservations(null, GoalCondition
						.parseGoalCondition("on$A$B"));
		assertNotNull("No onAB agent observations. Cannot run test.", lao);
		sut_ = lao.getRuleMutation();
	}

	@Test
	public void testTest() throws Exception {
		RelationalPredicate strFactA = StateSpec
				.toRelationalPredicate("(clear ?A)");
		RelationalPredicate strFactB = StateSpec
				.toRelationalPredicate("(clear ?B)");
		assertFalse(strFactA.equals(strFactB));
		assertFalse(strFactA.hashCode() == strFactB.hashCode());

		RelationalRule gr = new RelationalRule(
				"(clear a) (block a) => (moveFloor a)");
		assertTrue(gr.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear a)")));
		assertTrue(gr.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block a)")));
	}

	@Test
	public void testSpecialiseRule() {
		// Basic single action specialisation
		RelationalRule rule = new RelationalRule(
				"(clear ?A) (above ?A ?) => (moveFloor ?A)");
		Collection<RelationalRule> results = sut_.specialiseRule(rule);

		RelationalRule mutant = new RelationalRule(
				"(above ?A ?) (highest ?A) => (moveFloor ?A)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?A) (above ?A ?) (not (highest ?A)) => (moveFloor ?A)");
		assertTrue(results.contains(mutant));
		// No onFloor rules for moveFloor
		mutant = new RelationalRule(
				"(clear ?A) (above ?A ?) (onFloor ?A) => (moveFloor ?A)");
		assertFalse(results.contains(mutant));
		// Local specialisations
		mutant = new RelationalRule(
				"(clear ?A) (above ?A ?G_0) => (moveFloor ?A)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?A) (above ?A ?G_1) => (moveFloor ?A)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?A) (above ?A ?) (on ?A ?G_0) => (moveFloor ?A)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?A) (above ?A ?) (on ?A ?G_1) => (moveFloor ?A)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 6);

		// Constant term in action
		rule = new RelationalRule("(clear a) (above a ?) => (moveFloor a)");
		results = sut_.specialiseRule(rule);

		mutant = new RelationalRule("(highest a) (above a ?) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (not (highest a)) (above a ?) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear a) (above a ?G_0) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear a) (above a ?G_1) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (above a ?) (on a ?G_0) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (above a ?) (on a ?G_1) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 6);

		// Constant term in rule
		rule = new RelationalRule(
				"(clear a) (above ?A ?) (clear ?A) => (moveFloor ?A)");
		results = sut_.specialiseRule(rule);

		mutant = new RelationalRule(
				"(clear a) (above ?A ?) (highest ?A) => (moveFloor ?A)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?A) (above ?A ?) (not (highest ?A)) => (moveFloor ?A)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?A) (above ?A ?G_0) => (moveFloor ?A)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?A) (above ?A ?G_1) => (moveFloor ?A)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?A) (above ?A ?) (on ?A ?G_0) => (moveFloor ?A)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?A) (above ?A ?) (on ?A ?G_1) => (moveFloor ?A)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 6);

		// Harder action
		rule = new RelationalRule("(clear a) (clear ?B) => (move a ?B)");
		results = sut_.specialiseRule(rule);

		mutant = new RelationalRule("(clear ?B) (highest a) => (move a ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear a) (highest ?B) => (move a ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?B) (onFloor a) => (move a ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?B) (onFloor ?B) => (move a ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?B) (not (highest a)) => (move a ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?B) (not (highest ?B)) => (move a ?B)");
		assertTrue(results.contains(mutant));
		// Local specialisations
		mutant = new RelationalRule(
				"(clear a) (clear ?B) (above a ?G_0) => (move a ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?B) (on a ?G_0) => (move a ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?B) (above ?B ?G_0) => (move a ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?B) (on ?B ?G_0) => (move a ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?B) (above a ?G_1) => (move a ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?B) (on a ?G_1) => (move a ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?B) (above ?B ?G_1) => (move a ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?B) (on ?B ?G_1) => (move a ?B)");
		assertTrue(results.contains(mutant));
		// Due to equality background knowledge, can remove these negated rules.
		mutant = new RelationalRule(
				"(clear a) (clear ?B) (not (onFloor a)) => (move a ?B)");
		assertFalse(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?B) (not (onFloor ?B)) => (move a ?B)");
		assertFalse(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?B) (not (above a ?)) => (move a ?B)");
		assertFalse(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?B) (not (above ?B ?)) => (move a ?B)");
		assertFalse(results.contains(mutant));
		assertEquals(results.size(), 16);

		// Adding to the right term
		rule = new RelationalRule(
				"(clear ?A) (clear ?B) (block ?A) => (move ?A ?B)");
		results = sut_.specialiseRule(rule);

		mutant = new RelationalRule("(highest ?A) (clear ?B) => (move ?A ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear ?A) (highest ?B) => (move ?A ?B)");
		assertTrue(results.contains(mutant));

		rule = new RelationalRule(
				"(clear ?G_0) (highest ?B) (block ?G_0) (block ?B) => (move ?G_0 ?B)");
		results = sut_.specialiseRule(rule);

		assertFalse(results.toString().contains("(on ?G_0 ?G_1)"));
	}

	@Test
	public void testSpecialiseRuleBWMove() {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld", "onab");
		LocalAgentObservations lao = LocalAgentObservations
				.loadAgentObservations(null, GoalCondition
						.parseGoalCondition("on$A$B"));
		assertNotNull("No onAB agent observations. Cannot run test.", lao);
		sut_ = lao.getRuleMutation();

		// Interesting case of losing (clear/highest ?B)
		RelationalRule rule = new RelationalRule(
				"(clear ?A) (highest ?B) (block ?A) (block ?B) => (move ?A ?B)");
		Set<RelationalRule> results = sut_.specialiseRule(rule);
		RelationalRule mutant = new RelationalRule(
				"(clear ?A) (above ?A ?B) (block ?A) (block ?B) => (move ?A ?B)");
		assertFalse(results.contains(mutant));

		// RLGG specialisation
		rule = new RelationalRule(
				"(clear ?A) (clear ?B) (block ?A) => (move ?A ?B)");
		results = sut_.specialiseRule(rule);
		mutant = new RelationalRule(
				"(clear ?A) (clear ?B) (above ?A ?G_0) (block ?A) => (move ?A ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?A) (clear ?B) (on ?A ?G_0) (block ?A) => (move ?A ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?A) (floor ?B) (block ?A) => (move ?A ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?A) (clear ?B) (above ?A ?B) (block ?A) => (move ?A ?B)");
		assertFalse(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?A) (clear ?B) (above ?G_0 ?B) (block ?A) => (move ?A ?B)");
		assertFalse(results.contains(mutant));

		// Further specialisation (RLGG injection)
		rule = new RelationalRule(
				"(clear ?A) (floor ?B) (block ?A) => (move ?A ?B)");
		results = sut_.specialiseRule(rule);
		mutant = new RelationalRule(
				"(clear ?A) (floor ?B) (above ?A ?G_0) (block ?A) => (move ?A ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?A) (floor ?B) (above ?A ?B) (block ?A) => (move ?A ?B)");
		assertFalse(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?A) (floor ?B) (above ?G_0 ?B) (block ?A) => (move ?A ?B)");
		assertFalse(results.contains(mutant));

		// Illegal specialisation
		rule = new RelationalRule(
				"(clear ?A) (floor ?B) (block ?A) => (move ?A ?B)");
		results = sut_.specialiseRule(rule);
		mutant = new RelationalRule(
				"(clear ?A) (floor ?B) (highest ?B) => (move ?A ?B)");
		assertFalse(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?A) (floor ?B) (not (highest ?B)) => (move ?A ?B)");
		assertFalse(results.contains(mutant));

		rule = new RelationalRule(
				"(clear ?A) (highest ?B) (block ?A) => (move ?A ?B)");
		results = sut_.specialiseRule(rule);
		mutant = new RelationalRule(
				"(clear ?A) (floor ?B) (highest ?B) => (move ?A ?B)");
		assertFalse(results.contains(mutant));

		rule = new RelationalRule(
				"(clear ?A) (clear ?B) (not (highest ?B)) (block ?A) => (move ?A ?B)");
		results = sut_.specialiseRule(rule);
		mutant = new RelationalRule(
				"(clear ?A) (floor ?B) (not (highest ?B)) => (move ?A ?B)");
		assertFalse(results.contains(mutant));
	}

	@Test
	public void testSpecialiseRuleMinor() {
		// Basic moveFloor variable swapping
		RelationalRule rule = new RelationalRule(
				"(above ?A ?) (highest ?A) => (moveFloor ?A)");
		Collection<RelationalRule> results = sut_.specialiseRuleMinor(rule);

		RelationalRule mutant = new RelationalRule(
				"(above ?G_0 ?) (highest ?G_0) => (moveFloor ?G_0)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(above ?G_1 ?) (highest ?G_1) => (moveFloor ?G_1)");
		assertTrue(results.contains(mutant));
		// Also includes the three subranges
		assertEquals(results.size(), 5);

		// Move variable swapping
		rule = new RelationalRule("(clear ?A) (clear ?B) => (move ?A ?B)");
		results = sut_.specialiseRuleMinor(rule);

		mutant = new RelationalRule("(clear ?G_0) (clear ?B) => (move ?G_0 ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear ?A) (clear ?G_0) => (move ?A ?G_0)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear ?G_1) (clear ?B) => (move ?G_1 ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear ?A) (clear ?G_1) => (move ?A ?G_1)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 10);

		// Second level specialisation
		rule = new RelationalRule("(clear ?G_0) (clear ?B) => (move ?G_0 ?B)");
		results = sut_.specialiseRuleMinor(rule);

		mutant = new RelationalRule(
				"(clear ?G_0) (clear ?G_1) => (move ?G_0 ?G_1)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 7);

		rule = new RelationalRule(
				"(clear ?G_0) (highest ?B) (block ?G_0) (block ?B) => (move ?G_0 ?B)");
		results = sut_.specialiseRuleMinor(rule);

		assertFalse(results.toString().contains("(on ?G_0 ?G_1)"));
	}

	@Test
	public void testSpecialiseRuleMinorBWMove() {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld", "onab");
		LocalAgentObservations lao = LocalAgentObservations
				.loadAgentObservations(null, GoalCondition
						.parseGoalCondition("on$A$B"));
		assertNotNull("No onAB agent observations. Cannot run test.", lao);
		sut_ = lao.getRuleMutation();

		RelationalRule rule = new RelationalRule(
				"(clear ?A) (floor ?B) => (move ?A ?B)");
		Collection<RelationalRule> results = sut_.specialiseRuleMinor(rule);

		RelationalRule mutant = new RelationalRule(
				"(clear ?G_0) (floor ?B) => (move ?G_0 ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear ?G_1) (floor ?B) => (move ?G_1 ?B)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 6);

		// Test splitting a (variable) numerical condition
		rule = new RelationalRule("(clear ?A) (floor ?B) => (move ?A ?B)");
		results = sut_.specialiseRuleMinor(rule);
		mutant = new RelationalRule("(clear ?G_0) (floor ?B) => (move ?G_0 ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear ?G_1) (floor ?B) => (move ?G_1 ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?A) (height ?A ?#_0&:(range ?#_0min 0 ?#_0 ?#_0max 0.5)) "
						+ "(floor ?B) => (move ?A ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?A) (height ?A ?#_0&:(range ?#_0min 0.5 ?#_0 ?#_0max 1)) "
						+ "(floor ?B) => (move ?A ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?A) (height ?A ?#_0&:(range ?#_0min 0.25 ?#_0 ?#_0max 0.75)) "
						+ "(floor ?B) => (move ?A ?B)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 8);

		// Test NOT splitting a CONSTANT numerical condition
		rule = new RelationalRule(
				"(clear ?A) (height ?A 1) (floor ?B) => (move ?A ?B)");
		results = sut_.specialiseRuleMinor(rule);
		mutant = new RelationalRule(
				"(clear ?G_0) (height ?G_0 1) (floor ?B) => (move ?G_0 ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?G_1) (height ?G_1 1) (floor ?B) => (move ?G_1 ?B)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?A) (height ?A 1&:(range 1min 0 1 1max 0.5)) "
						+ "(floor ?B) => (move ?A ?B)");
		assertFalse(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?A) (height ?A 1&:(range 1min 0.5 1 1max 1)) "
						+ "(floor ?B) => (move ?A ?B)");
		assertFalse(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?A) (height ?A 1&:(range 1min 0.25 1 1max 0.75)) "
						+ "(floor ?B) => (move ?A ?B)");
		assertFalse(results.contains(mutant));
		assertEquals(results.size(), 2);
	}

	@Test
	public void testSpecialiseRuleMinorMario() {
		StateSpec.initInstance("mario.RLMario");
		LocalAgentObservations lao = LocalAgentObservations
				.loadAgentObservations(null, GoalCondition.parseGoalCondition("cool"));
		sut_ = lao.getRuleMutation();

		RelationalRule rule = new RelationalRule("(thing ?A) (canJumpOn ?A) "
				+ "(distance ?A ?#_2&:(<= -159.0 ?#_2 160.0)) "
				+ "(heightDiff ?A ?#_3&:(<= -242.0 ?#_3 87.0)) "
				+ "(not (width ?A ?)) => (jumpOnto ?A ?#_2)");
		Set<RelationalRule> specialisedRules = sut_.specialiseRuleMinor(rule);

		assertEquals(specialisedRules.size(), 6);
	}

	@Test
	public void testSpecialiseRuleCarcassonne() {
		StateSpec.initInstance("jCloisterZone.Carcassonne");
		LocalAgentObservations lao = LocalAgentObservations
				.loadAgentObservations(null, GoalCondition.parseGoalCondition("cool"));
		sut_ = lao.getRuleMutation();

		Collection<RelationalRule> rlggs = lao
				.getRLGGRules(new ArrayList<RelationalRule>());
		RelationalRule placeTile = null;
		for (RelationalRule rlgg : rlggs) {
			if (rlgg.getActionPredicate().equals("placeTile")) {
				placeTile = rlgg;
				break;
			}
		}
		// TODO Check here.,
		Set<RelationalRule> specialisedRules = sut_.specialiseRule(placeTile);

		assertEquals(specialisedRules.size(), 19);
	}

	@Test
	public void testSpecialiseBindingVariable() {

	}
}