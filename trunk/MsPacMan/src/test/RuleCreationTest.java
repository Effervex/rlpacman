package test;

import static org.junit.Assert.*;

import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

import cerrla.RuleCreation;

import relationalFramework.agentObservations.EnvironmentAgentObservations;
import relationalFramework.agentObservations.BackgroundKnowledge;
import util.ConditionComparator;

public class RuleCreationTest {
	private RuleCreation sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld", "onab");
		sut_ = new RuleCreation();
		assertTrue("No onAB agent observations. Cannot run test.",
				EnvironmentAgentObservations.loadAgentObservations("onab"));
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
				"(clear ?X) (above ?X ?) => (moveFloor ?X)");
		Collection<RelationalRule> results = sut_.specialiseRule(rule);

		RelationalRule mutant = new RelationalRule(
				"(above ?X ?) (highest ?X) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (above ?X ?) (not (highest ?X)) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		// No onFloor rules for moveFloor
		mutant = new RelationalRule(
				"(clear ?X) (above ?X ?) (onFloor ?X) => (moveFloor ?X)");
		assertFalse(results.contains(mutant));
		// Local specialisations
		mutant = new RelationalRule(
				"(clear ?X) (above ?X ?G_0) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?X) (above ?X ?G_1) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear ?X) (on ?X ?G_0) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear ?X) (on ?X ?G_1) => (moveFloor ?X)");
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
		mutant = new RelationalRule("(clear a) (on a ?G_0) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule("(clear a) (on a ?G_1) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 6);

		// Constant term in rule
		rule = new RelationalRule(
				"(clear a) (above ?X ?) (clear ?X) => (moveFloor ?X)");
		results = sut_.specialiseRule(rule);

		mutant = new RelationalRule(
				"(clear a) (above ?X ?) (highest ?X) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?X) (above ?X ?) (not (highest ?X)) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?X) (above ?X ?G_0) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?X) (above ?X ?G_1) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?X) (on ?X ?G_0) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?X) (on ?X ?G_1) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 6);

		// Harder action
		rule = new RelationalRule("(clear a) (clear ?Y) => (move a ?Y)");
		results = sut_.specialiseRule(rule);

		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (above a ?) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear a) (clear ?Y) (above ?Y ?) => (move a ?Y)");
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
		sut_ = new RuleCreation();
		assertTrue("No loaded onAB agent observations. Cannot run test.",
				EnvironmentAgentObservations.loadAgentObservations("onab"));

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
	}

	@Test
	public void testSpecialiseRuleMinor() {
		// Basic moveFloor variable swapping
		RelationalRule rule = new RelationalRule(
				"(above ?X ?) (highest ?X) => (moveFloor ?X)");
		Collection<RelationalRule> results = sut_.specialiseRuleMinor(rule);

		RelationalRule mutant = new RelationalRule(
				"(above ?G_0 ?) (highest ?G_0) => (moveFloor ?G_0)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(above ?G_1 ?) (highest ?G_1) => (moveFloor ?G_1)");
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
		sut_ = new RuleCreation();
		assertTrue("No loaded onAB agent observations. Cannot run test.",
				EnvironmentAgentObservations.loadAgentObservations("onab"));
		
		RelationalRule rule = new RelationalRule(
				"(clear ?X) (floor ?Y) => (move ?X ?Y)");
		Collection<RelationalRule> results = sut_.specialiseRuleMinor(rule);

		RelationalRule mutant = new RelationalRule(
				"(clear ?G_0) (floor ?Y) => (move ?G_0 ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new RelationalRule(
				"(clear ?G_1) (floor ?Y) => (move ?G_1 ?Y)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 2);
	}

	@Test
	public void testSimplifyRule() {
		// Simple no-effect test
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>(
				ConditionComparator.getInstance());
		ruleConds.add(StateSpec.toRelationalPredicate("(clear a)"));
		SortedSet<RelationalPredicate> results = sut_.simplifyRule(ruleConds,
				null, null, false);
		assertNull(results);

		// Equivalence condition removal
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(on ?X ?)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?X ?)")));
		assertEquals(results.size(), 1);

		// Using an added condition (null result)
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds,
				StateSpec.toRelationalPredicate("(on ?X ?)"), null, false);
		assertNull(results);

		// Using an added condition (no simplification)
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds,
				StateSpec.toRelationalPredicate("(clear ?X)"), null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?X ?)")));
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(clear ?X)")));
		assertEquals(results.size(), 2);

		// Using an added condition (swapped result)
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(on ?X ?)"));
		results = sut_.simplifyRule(ruleConds,
				StateSpec.toRelationalPredicate("(above ?X ?)"), null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?X ?)")));
		assertEquals(results.size(), 1);

		// Testing double-negated condition
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds,
				StateSpec.toRelationalPredicate("(not (above ?X ?))"), null,
				false);
		assertNull(results);

		// Testing illegal condition
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(onFloor ?X)"));
		results = sut_.simplifyRule(ruleConds, null, null, true);
		assertNull(results);
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(onFloor ?X)")));
		assertEquals(results.size(), 1);

		// Testing same condition
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds,
				StateSpec.toRelationalPredicate("(above ?X ?)"), null, false);
		assertNull(results);

		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(on ?X ?)"));
		results = sut_
				.simplifyRule(ruleConds,
						StateSpec.toRelationalPredicate("(not (on ?X ?))"),
						null, false);
		assertNull(results);

		// Testing unification
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(on ?X ?)"));
		results = sut_.simplifyRule(ruleConds,
				StateSpec.toRelationalPredicate("(on ?X ?Y)"), null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(on ?X ?Y)")));
		assertEquals(results.size(), 1);

		// Testing double unification (onX? -> aboveX? which is removed)
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(on ?X ?)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X a)"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?X a)")));
		assertEquals(results.size(), 1);

		// Testing complex simplification
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?Y)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ? ?Y)"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?X ?Y)")));
		assertEquals(results.size(), 1);

		// Even more complex
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?Y)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(on ? ?Y)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(not (onFloor ?X))"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?X ?Y)")));
		assertEquals(results.size(), 1);

		// Testing equivalent conditions (prefer left side of equation to right)
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(not (on ?X ?))"));
		ruleConds.add(StateSpec.toRelationalPredicate("(onFloor ?X)"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(onFloor ?X)")));
		assertEquals(results.size(), 1);

		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(not (onFloor ?X))"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?X ?)")));
		assertEquals(results.size(), 1);

		// Testing swapped for left equivalent conditions
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(not (on ?X ?))"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(onFloor ?X)")));
		assertEquals(results.size(), 1);

		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(not (onFloor ?X))"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?X ?)")));
		assertEquals(results.size(), 1);

		// Testing unification of background knowledge
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(on ?X ?Y)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(on ?X ?Y)")));
		assertEquals(results.size(), 1);

		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(on ?X ?)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?Y)"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?X ?Y)")));
		assertEquals(results.size(), 1);

		// Testing unification on a number of matches
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?Y b)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?Y ?)"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?X ?)")));
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?Y b)")));
		assertEquals(results.size(), 2);

		// Testing unification on a number of matches
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?Y b)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?Y ?)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?X ?)")));
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?Y b)")));
		assertEquals(results.size(), 2);

		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X a)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?Y ?)"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?X a)")));
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?Y ?)")));
		assertEquals(results.size(), 2);

		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(highest ?Y)"));
		results = sut_.simplifyRule(ruleConds,
				StateSpec.toRelationalPredicate("(highest ?X)"), null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(highest ?X)")));
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(highest ?Y)")));
		assertEquals(results.size(), 2);
	}

	@Test
	public void testSimplifyRuleBWMove() {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld", "onab");
		sut_ = new RuleCreation();
		assertTrue("No loaded onAB agent observations. Cannot run test.",
				EnvironmentAgentObservations.loadAgentObservations("onab"));

		// Strange issue:
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>(
				ConditionComparator.getInstance());
		ruleConds.add(StateSpec.toRelationalPredicate("(highest ?Y)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?Y ?)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(block ?Y)"));
		SortedSet<RelationalPredicate> results = sut_.simplifyRule(ruleConds,
				null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(highest ?Y)")));
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(block ?Y)")));
		assertEquals(results.size(), 2);

		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(highest ?Y)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?Y floor)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?Y ?)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(block ?Y)"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(highest ?Y)")));
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?Y floor)")));
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(block ?Y)")));
		assertEquals(results.size(), 3);

		// Test the (block X) <=> (above X ?) rule
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(block ?X)")));
		assertEquals(results.size(), 1);

		// Test the invariants
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear floor)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(floor floor)"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.isEmpty());

		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ? floor)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(floor floor)"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.isEmpty());

		// Disallowing an illegal rule
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(highest ?Y)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(block ?X)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(block ?Y)"));
		results = sut_.simplifyRule(ruleConds,
				StateSpec.toRelationalPredicate("(above ?X ?Y)"), null, true);
		assertNull(results);

		// Floor simplification
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?G_0)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?Y)"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(clear ?X)")));
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(floor ?Y)")));
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?X ?G_0)")));
		assertEquals(results.size(), 3);

		// On X G0 Simplification
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(block ?X)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(thing ?Y)"));
		results = sut_.simplifyRule(ruleConds,
				StateSpec.toRelationalPredicate("(on ?X ?G_0)"), null, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(clear ?X)")));
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(clear ?Y)")));
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(on ?X ?G_0)")));
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(block ?X)")));
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(thing ?Y)")));
		assertEquals(results.size(), 5);

		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		results = sut_.simplifyRule(ruleConds,
				StateSpec.toRelationalPredicate("(not (above ?X ?Y))"), null,
				false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(clear ?X)")));
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(clear ?Y)")));
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(not (above ?X ?Y))")));
		assertEquals(results.size(), 3);
	}

	@Test
	public void testEquivalenceRules() {
		// Set up the allowable conditions
		Collection<RelationalPredicate> conditions = new HashSet<RelationalPredicate>();
		conditions.add(StateSpec.toRelationalPredicate("(on ?X ?)"));
		conditions.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		conditions.add(StateSpec.toRelationalPredicate("(highest ?X)"));
		conditions.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		EnvironmentAgentObservations.getInstance().setActionConditions("moveFloor",
				conditions);
		conditions = new HashSet<RelationalPredicate>();
		conditions.add(StateSpec.toRelationalPredicate("(on ?X ?)"));
		conditions.add(StateSpec.toRelationalPredicate("(on ?Y ?)"));
		conditions.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		conditions.add(StateSpec.toRelationalPredicate("(above ?Y ?)"));
		conditions.add(StateSpec.toRelationalPredicate("(highest ?X)"));
		conditions.add(StateSpec.toRelationalPredicate("(highest ?Y)"));
		conditions.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		conditions.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		conditions.add(StateSpec.toRelationalPredicate("(onFloor ?X)"));
		conditions.add(StateSpec.toRelationalPredicate("(onFloor ?Y)"));
		EnvironmentAgentObservations.getInstance().setActionConditions("move", conditions);

		// Set up the equivalence and other rules
		SortedSet<BackgroundKnowledge> backKnow = new TreeSet<BackgroundKnowledge>();
		backKnow.add(new BackgroundKnowledge("(above ?X ?) <=> (on ?X ?)"));
		backKnow.add(new BackgroundKnowledge(
				"(above ?X ?) <=> (not (onFloor ?X))"));
		backKnow.add(new BackgroundKnowledge("(above ? ?Y) <=> (on ? ?Y)"));
		backKnow.add(new BackgroundKnowledge(
				"(above ? ?Y) <=> (not (clear ?Y))"));
		backKnow.add(new BackgroundKnowledge(
				"(clear ?X) <=> (not (above ? ?X))"));
		backKnow.add(new BackgroundKnowledge("(clear ?X) <=> (not (on ? ?X))"));
		backKnow.add(new BackgroundKnowledge("(on ?X ?) <=> (not (onFloor ?X))"));
		backKnow.add(new BackgroundKnowledge("(on ? ?Y) <=> (not (clear ?Y))"));
		backKnow.add(new BackgroundKnowledge(
				"(onFloor ?X) <=> (not (above ?X ?))"));
		backKnow.add(new BackgroundKnowledge("(onFloor ?X) <=> (not (on ?X ?))"));
		backKnow.add(new BackgroundKnowledge("(above ?X ?Y) => (above ?X ?)"));
		backKnow.add(new BackgroundKnowledge("(above ?X ?Y) => (above ? ?Y)"));
		backKnow.add(new BackgroundKnowledge(
				"(on ?X ?Y) (above ?Y ?Z) => (assert (above ?X ?Z))"));
		backKnow.add(new BackgroundKnowledge("(highest ?X) => (clear ?X)"));
		backKnow.add(new BackgroundKnowledge(
				"(on ?X ?Y) => (assert (above ?X ?Y))"));
		backKnow.add(new BackgroundKnowledge("(on ?X ?Y) => (on ?X ?)"));
		backKnow.add(new BackgroundKnowledge("(on ?X ?Y) => (on ? ?Y)"));
		backKnow.add(new BackgroundKnowledge(
				"(block ?Z) (on ?X ?Y) => (not (on ?X ?Z))"));
		backKnow.add(new BackgroundKnowledge("(on ?X ?) => (above ?X ?)"));
		backKnow.add(new BackgroundKnowledge(
				"(block ?Y) (not (on ? ?Y)) => (assert (clear ?Y))"));
		EnvironmentAgentObservations.getInstance().setBackgroundKnowledge(backKnow);

		// Basic implication test
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>(
				ConditionComparator.getInstance());
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(highest ?X)"));
		SortedSet<RelationalPredicate> results = sut_.simplifyRule(ruleConds,
				null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(highest ?X)")));
		assertEquals(results.size(), 1);

		// Basic equivalency swap test
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(on ?X ?)"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ?X ?)")));
		assertEquals(results.size(), 1);

		// Basic negated equivalency swap test
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(not (clear ?X))"));
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec
				.toRelationalPredicate("(above ? ?X)")));
		assertEquals(results.size(), 1);
	}
}