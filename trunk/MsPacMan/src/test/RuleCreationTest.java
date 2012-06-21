package test;

import static org.junit.Assert.*;

import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

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
				.loadAgentObservations(GoalCondition
						.parseGoalCondition("on$A$B"));
		assertNotNull("No onAB agent observations. Cannot run test.", lao);
		sut_ = lao.getRuleMutation();
	}

	@Test
	public void testTest() throws Exception {
		RelationalPredicate strFactA = StateSpec
				.toRelationalPredicate("(clear ?X)");
		RelationalPredicate strFactB = StateSpec
				.toRelationalPredicate("(clear ?Y)");
		assertFalse(strFactA.equals(strFactB));
		assertFalse(strFactA.hashCode() == strFactB.hashCode());

		RelationalRule gr = new RelationalRule(
				"(clear a) (block a) => (moveFloor a)");
		assertTrue(gr.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear a)")));
		assertTrue(gr.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(block a)")));
	}

	@Test
	public void testSpecialiseRule() {
		// Basic single action specialisation
		RelationalRule rule = new RelationalRule(
				"(clear ?X) (above ?X ?Y) => (moveFloor ?X)");
		Collection<RelationalRule> results = sut_.specialiseRule(rule);

		RelationalRule mutant = new RelationalRule(
				"(above ?X ?Y) (highest ?X) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (above ?X ?Y) (not (highest ?X)) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		// No onFloor rules for moveFloor
		mutant = new RelationalRule(
				"(clear ?X) (above ?X ?Y) (onFloor ?X) => (moveFloor ?X)");
		assertFalse(results.contains(mutant));
		// Local specialisations
		mutant = new RelationalRule(
				"(clear ?X) (above ?X ?G_0) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (above ?X ?G_1) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (above ?X ?Y) (on ?X ?G_0) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (above ?X ?Y) (on ?X ?G_1) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 6);

		// Constant term in action
		rule = new RelationalRule("(clear a) (above a ?Y) => (moveFloor a)");
		results = sut_.specialiseRule(rule);

		mutant = new RelationalRule("(highest a) (above a ?Y) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (not (highest a)) (above a ?Y) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear a) (above a ?G_0) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear a) (above a ?G_1) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (above a ?Y) (on a ?G_0) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (above a ?Y) (on a ?G_1) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 6);

		// Constant term in rule
		rule = new RelationalRule(
				"(clear a) (above ?X ?Y) (clear ?X) => (moveFloor ?X)");
		results = sut_.specialiseRule(rule);

		mutant = new RelationalRule(
				"(clear a) (above ?X ?Y) (highest ?X) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?X) (above ?X ?Y) (not (highest ?X)) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?X) (above ?X ?G_0) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?X) (above ?X ?G_1) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?X) (above ?X ?Y) (on ?X ?G_0) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?X) (above ?X ?Y) (on ?X ?G_1) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 6);

		// Harder action
		rule = new RelationalRule("(clear a) (clear ?Y) => (move a ?Y)");
		results = sut_.specialiseRule(rule);

		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (above a ?Y) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (above ?Y ?Y) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear ?Y) (highest a) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear a) (highest ?Y) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (onFloor a) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (onFloor ?Y) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (not (highest a)) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (not (highest ?Y)) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		// Local specialisations
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (above a ?G_0) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (on a ?G_0) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (above ?Y ?G_0) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (on ?Y ?G_0) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (above a ?G_1) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (on a ?G_1) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (above ?Y ?G_1) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (on ?Y ?G_1) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		// Due to equality background knowledge, can remove these negated rules.
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (not (onFloor a)) => (move a ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (not (onFloor ?Y)) => (move a ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (not (above a ?)) => (move a ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (not (above ?Y ?)) => (move a ?Y)");
		assertFalse(results.contains(mutant));
		assertEquals(results.size(), 16);

		// Avoiding impossible specialisations
		rule = new RelationalRule(
				"(clear ?X) (clear ?Y) (on ?X ?Y) => (move ?X ?Y)");
		results = sut_.specialiseRule(rule);
		assertTrue(results.isEmpty());

		// Adding to the right term
		rule = new RelationalRule(
				"(clear ?X) (clear ?Y) (block ?X) => (move ?X ?Y)");
		results = sut_.specialiseRule(rule);

		mutant = new RelationalRule(
				"(highest ?X) (clear ?Y) (block ?X) => (move ?X ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (highest ?Y) (block ?X) => (move ?X ?Y)");
		assertTrue(results.contains(mutant));

		rule = new RelationalRule(
				"(clear ?G_0) (highest ?Y) (block ?G_0) (block ?Y) => (move ?G_0 ?Y)");
		results = sut_.specialiseRule(rule);

		assertFalse(results.toString().contains("(on ?G_0 ?G_1)"));
	}

	@Test
	public void testSpecialiseRuleBWMove() {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld", "onab");
		LocalAgentObservations lao = LocalAgentObservations
				.loadAgentObservations(GoalCondition
						.parseGoalCondition("on$A$B"));
		assertNotNull("No onAB agent observations. Cannot run test.", lao);
		sut_ = lao.getRuleMutation();

		// Interesting case of losing (clear/highest ?Y)
		RelationalRule rule = new RelationalRule(
				"(clear ?X) (highest ?Y) (block ?X) (block ?Y) => (move ?X ?Y)");
		Set<RelationalRule> results = sut_.specialiseRule(rule);
		RelationalRule mutant = new RelationalRule(
				"(clear ?X) (above ?X ?Y) (block ?X) (block ?Y) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));

		// RLGG specialisation
		rule = new RelationalRule(
				"(clear ?X) (clear ?Y) (block ?X) => (move ?X ?Y)");
		results = sut_.specialiseRule(rule);
		mutant = new RelationalRule(
				"(clear ?X) (clear ?Y) (above ?X ?G_0) (block ?X) => (move ?X ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (clear ?Y) (on ?X ?G_0) (block ?X) => (move ?X ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (floor ?Y) (block ?X) => (move ?X ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (clear ?Y) (above ?X ?Y) (block ?X) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (clear ?Y) (above ?G_0 ?Y) (block ?X) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));

		// Further specialisation (RLGG injection)
		rule = new RelationalRule(
				"(clear ?X) (floor ?Y) (block ?X) => (move ?X ?Y)");
		results = sut_.specialiseRule(rule);
		mutant = new RelationalRule(
				"(clear ?X) (floor ?Y) (above ?X ?G_0) (block ?X) => (move ?X ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (floor ?Y) (above ?X ?Y) (block ?X) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (floor ?Y) (above ?G_0 ?Y) (block ?X) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));
		
		// Illegal specialisation
		rule = new RelationalRule(
				"(clear ?X) (floor ?Y) (block ?X) => (move ?X ?Y)");
		results = sut_.specialiseRule(rule);
		mutant = new RelationalRule(
				"(clear ?X) (floor ?Y) (highest ?Y) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (floor ?Y) (not (highest ?Y)) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));
		
		rule = new RelationalRule(
				"(clear ?X) (highest ?Y) (block ?X) => (move ?X ?Y)");
		results = sut_.specialiseRule(rule);
		mutant = new RelationalRule(
				"(clear ?X) (floor ?Y) (highest ?Y) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));
		
		rule = new RelationalRule(
				"(clear ?X) (clear ?Y) (not (highest ?Y)) (block ?X) => (move ?X ?Y)");
		results = sut_.specialiseRule(rule);
		mutant = new RelationalRule(
				"(clear ?X) (floor ?Y) (not (highest ?Y)) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));
	}

	@Test
	public void testSpecialiseRuleMinor() {
		// Basic moveFloor variable swapping
		RelationalRule rule = new RelationalRule(
				"(above ?X ?Y) (highest ?X) => (moveFloor ?X)");
		Collection<RelationalRule> results = sut_.specialiseRuleMinor(rule);

		RelationalRule mutant = new RelationalRule(
				"(above ?G_0 ?Y) (highest ?G_0) => (moveFloor ?G_0)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(above ?G_1 ?Y) (highest ?G_1) => (moveFloor ?G_1)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 2);

		// Move variable swapping
		rule = new RelationalRule("(clear ?X) (clear ?Y) => (move ?X ?Y)");
		results = sut_.specialiseRuleMinor(rule);

		mutant = new RelationalRule("(clear ?G_0) (clear ?Y) => (move ?G_0 ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear ?X) (clear ?G_0) => (move ?X ?G_0)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear ?G_1) (clear ?Y) => (move ?G_1 ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear ?X) (clear ?G_1) => (move ?X ?G_1)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 4);

		// Second level specialisation
		rule = new RelationalRule("(clear ?G_0) (clear ?Y) => (move ?G_0 ?Y)");
		results = sut_.specialiseRuleMinor(rule);

		mutant = new RelationalRule(
				"(clear ?G_0) (clear ?G_1) => (move ?G_0 ?G_1)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 1);

		rule = new RelationalRule(
				"(clear ?G_0) (highest ?Y) (block ?G_0) (block ?Y) => (move ?G_0 ?Y)");
		results = sut_.specialiseRuleMinor(rule);

		assertFalse(results.toString().contains("(on ?G_0 ?G_1)"));
	}

	@Test
	public void testSpecialiseRuleMinorBWMove() {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld", "onab");
		LocalAgentObservations lao = LocalAgentObservations
				.loadAgentObservations(GoalCondition
						.parseGoalCondition("on$A$B"));
		assertNotNull("No onAB agent observations. Cannot run test.", lao);
		sut_ = lao.getRuleMutation();

		RelationalRule rule = new RelationalRule(
				"(clear ?X) (floor ?Y) => (move ?X ?Y)");
		Collection<RelationalRule> results = sut_.specialiseRuleMinor(rule);

		RelationalRule mutant = new RelationalRule(
				"(clear ?G_0) (floor ?Y) => (move ?G_0 ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear ?G_1) (floor ?Y) => (move ?G_1 ?Y)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 2);

		// Test splitting a (variable) numerical condition
		rule = new RelationalRule(
				"(clear ?X) (height ?X ?#_0) (floor ?Y) => (move ?X ?Y)");
		results = sut_.specialiseRuleMinor(rule);
		mutant = new RelationalRule(
				"(clear ?G_0) (height ?G_0 ?#_0) (floor ?Y) => (move ?G_0 ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?G_1) (height ?G_1 ?#_0) (floor ?Y) => (move ?G_1 ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (height ?X ?#_0&:(range ?#_0min 0 ?#_0 ?#_0max 0.5)) "
						+ "(floor ?Y) => (move ?X ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (height ?X ?#_0&:(range ?#_0min 0.5 ?#_0 ?#_0max 1)) "
						+ "(floor ?Y) => (move ?X ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (height ?X ?#_0&:(range ?#_0min 0.25 ?#_0 ?#_0max 0.75)) "
						+ "(floor ?Y) => (move ?X ?Y)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 5);

		// Test NOT splitting a CONSTANT numerical condition
		rule = new RelationalRule(
				"(clear ?X) (height ?X 1) (floor ?Y) => (move ?X ?Y)");
		results = sut_.specialiseRuleMinor(rule);
		mutant = new RelationalRule(
				"(clear ?G_0) (height ?G_0 1) (floor ?Y) => (move ?G_0 ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?G_1) (height ?G_1 1) (floor ?Y) => (move ?G_1 ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (height ?X 1&:(range 1min 0 1 1max 0.5)) "
						+ "(floor ?Y) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (height ?X 1&:(range 1min 0.5 1 1max 1)) "
						+ "(floor ?Y) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (height ?X 1&:(range 1min 0.25 1 1max 0.75)) "
						+ "(floor ?Y) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));
		assertEquals(results.size(), 2);
	}

	@Test
	public void testSpecialiseRuleMinorMario() {
		StateSpec.initInstance("mario.RLMario");
		LocalAgentObservations lao = LocalAgentObservations
				.loadAgentObservations(GoalCondition.parseGoalCondition("cool"));
		sut_ = lao.getRuleMutation();

		RelationalRule rule = new RelationalRule("(thing ?X) (canJumpOn ?X) "
				+ "(distance ?X ?#_2&:(<= -159.0 ?#_2 160.0)) "
				+ "(heightDiff ?X ?#_3&:(<= -242.0 ?#_3 87.0)) "
				+ "(not (width ?X ?)) => (jumpOnto ?X ?#_2)");
		Set<RelationalRule> specialisedRules = sut_.specialiseRuleMinor(rule);

		assertEquals(specialisedRules.size(), 6);
	}

	@Test
	public void testSpecialiseRuleCarcassonne() {
		StateSpec.initInstance("jCloisterZone.Carcassonne");
		LocalAgentObservations lao = LocalAgentObservations
				.loadAgentObservations(GoalCondition.parseGoalCondition("cool"));
		sut_ = lao.getRuleMutation();

		RelationalRule rule = new RelationalRule(
				"(currentPlayer ?X) (currentTile ?Y) (test (<> ?Y ?X)) "
						+ "(score ?X ?#_0) (meeplesLeft ?X ?#_1) (numSurroundingTiles ?Z ?#_2) "
						+ "(test (<> ?Z ?Y ?X)) (validLoc ?Z ?A) (test (<> ?A ?Z ?Y ?X)) "
						+ "(tileEdge ?Y ?Unb_4 ?Unb_5) (test (<> ?Unb_5 ?A ?Z ?Y ?X)) "
						+ "(test (<> ?Unb_4 ?A ?Z ?Y ?X)) (locationXY ?Z ?#_3 ?#_4) "
						+ "(nextTo ?Z ?Unb_6 ?Unb_7) (test (<> ?Unb_7 ?A ?Z ?Y ?X)) "
						+ "(test (<> ?Unb_6 ?A ?Z ?Y ?X)) (orientation ?A) (edge ?Unb_4) "
						+ "(terrain ?Unb_5) (edge ?Unb_6) (terrain ?Unb_7) (player ?X) "
						+ "(tile ?Y) (location ?Z) => (placeTile ?X ?Y ?Z ?A)");
		// TODO Check here.,
		Set<RelationalRule> specialisedRules = sut_.specialiseRule(rule);
		
		assertEquals(specialisedRules.size(), 19);
	}
	
	@Test
	public void testSpecialiseBindingVariable() {
		
	}
}