package test;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.ConditionComparator;
import relationalFramework.RuleCreation;
import relationalFramework.GuidedRule;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;
import relationalFramework.agentObservations.AgentObservations;
import relationalFramework.agentObservations.BackgroundKnowledge;

public class RuleCreationTest {
	private RuleCreation sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld", "onab");
		sut_ = new RuleCreation();
		assertTrue(
				"No onAB agent observations. Cannot run test.",
				AgentObservations
						.loadAgentObservations());
	}

	@Test
	public void testTest() throws Exception {
		StringFact strFactA = StateSpec.toStringFact("(clear ?X)");
		StringFact strFactB = StateSpec.toStringFact("(clear ?Y)");
		assertFalse(strFactA.equals(strFactB));
		assertFalse(strFactA.hashCode() == strFactB.hashCode());

		GuidedRule gr = new GuidedRule("(clear a) (block a) => (moveFloor a)");
		assertTrue(gr.getConditions(false).contains(
				StateSpec.toStringFact("(clear a)")));
		assertTrue(gr.getConditions(false).contains(
				StateSpec.toStringFact("(block a)")));
	}

	@Test
	public void testSpecialiseRule() {
		// Basic single action specialisation
		GuidedRule rule = new GuidedRule(
				"(clear ?X) (above ?X ?) => (moveFloor ?X)");
		Collection<GuidedRule> results = sut_.specialiseRule(rule);

		GuidedRule mutant = new GuidedRule(
				"(above ?X ?) (highest ?X) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear ?X) (above ?X ?) (not (highest ?X)) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		// No onFloor rules for moveFloor
		mutant = new GuidedRule(
				"(clear ?X) (above ?X ?) (onFloor ?X) => (moveFloor ?X)");
		assertFalse(results.contains(mutant));
		assertEquals(results.size(), 2);

		// Second (impossible) step specialisation
		Collection<GuidedRule> subResults = new HashSet<GuidedRule>();
		for (GuidedRule gr : results)
			subResults.addAll(sut_.specialiseRule(gr));
		assertEquals(subResults.size(), 0);

		// Constant term in action
		rule = new GuidedRule("(clear a) (above a ?) => (moveFloor a)");
		results = sut_.specialiseRule(rule);

		mutant = new GuidedRule("(highest a) (above a ?) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (not (highest a)) (above a ?) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 2);

		// Constant term in rule
		rule = new GuidedRule(
				"(clear a) (above ?X ?) (clear ?X) => (moveFloor ?X)");
		results = sut_.specialiseRule(rule);

		mutant = new GuidedRule(
				"(clear a) (above ?X ?) (highest ?X) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?X) (above ?X ?) (not (highest ?X)) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 2);

		// Harder action
		rule = new GuidedRule("(clear a) (clear ?Y) => (move a ?Y)");
		results = sut_.specialiseRule(rule);

		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (above a ?) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (above ?Y ?) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule("(clear ?Y) (highest a) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule("(clear a) (highest ?Y) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (onFloor a) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (onFloor ?Y) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (not (highest a)) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (not (highest ?Y)) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		// Due to equality background knowledge, can remove these negated rules.
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (not (onFloor a)) => (move a ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (not (onFloor ?Y)) => (move a ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (not (above a ?)) => (move a ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (not (above ?Y ?)) => (move a ?Y)");
		assertFalse(results.contains(mutant));
		assertEquals(results.size(), 8);

		// Avoiding impossible specialisations
		rule = new GuidedRule(
				"(clear ?X) (clear ?Y) (on ?X ?Y) => (move ?X ?Y)");
		results = sut_.specialiseRule(rule);

		// Should not be there
		mutant = new GuidedRule(
				"(clear ?X) (clear ?Y) (on ?X ?Y) (on ?X ?) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear ?X) (clear ?Y) (on ?X ?Y) (not (on ?X ?)) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));
		// Using background knowledge to disallow pointless and illegal
		// mutations
		mutant = new GuidedRule(
				"(clear ?X) (clear ?Y) (on ?X ?Y) (onFloor ?X) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear ?X) (clear ?Y) (on ?X ?Y) (not (onFloor ?X)) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));

		// Adding to the right term
		rule = new GuidedRule("(clear ?X) (block ?Y) => (move ?X ?Y)");
		results = sut_.specialiseRule(rule);

		mutant = new GuidedRule("(highest ?X) (block ?Y) => (move ?X ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule("(clear ?X) (highest ?Y) => (move ?X ?Y)");
		assertTrue(results.contains(mutant));
	}

	@Test
	public void testSpecialiseRuleMinor() {
		// Basic moveFloor variable swapping
		GuidedRule rule = new GuidedRule(
				"(above ?X ?) (highest ?X) => (moveFloor ?X)");
		Collection<GuidedRule> results = sut_.specialiseRuleMinor(rule);

		GuidedRule mutant = new GuidedRule(
				"(above ?G_0 ?) (highest ?G_0) => (moveFloor ?G_0)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(above ?G_1 ?) (highest ?G_1) => (moveFloor ?G_1)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 2);

		// Move variable swapping
		rule = new GuidedRule("(clear ?X) (clear ?Y) => (move ?X ?Y)");
		results = sut_.specialiseRuleMinor(rule);

		mutant = new GuidedRule("(clear ?G_0) (clear ?Y) => (move ?G_0 ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule("(clear ?X) (clear ?G_0) => (move ?X ?G_0)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule("(clear ?G_1) (clear ?Y) => (move ?G_1 ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule("(clear ?X) (clear ?G_1) => (move ?X ?G_1)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 4);
		
		// Second level specialisation
		rule = new GuidedRule("(clear ?G_0) (clear ?Y) => (move ?G_0 ?Y)");
		results = sut_.specialiseRuleMinor(rule);
		
		mutant = new GuidedRule("(clear ?G_0) (clear ?G_1) => (move ?G_0 ?G_1)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 1);

		// Impossible specialisation
		rule = new GuidedRule("(clear ?X) (on ?X ?Y) => (move ?X ?Y)");
		results = sut_.specialiseRuleMinor(rule);
		
		assertEquals(results.size(), 0);
	}

	@Test
	public void testSimplifyRule() {
		// Simple no-effect test
		SortedSet<StringFact> ruleConds = new TreeSet<StringFact>(
				ConditionComparator.getInstance());
		ruleConds.add(StateSpec.toStringFact("(clear a)"));
		SortedSet<StringFact> results = sut_.simplifyRule(ruleConds, null,
				false, true);
		assertNull(results);

		// Equivalence condition removal
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?)"));
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertEquals(results.size(), 1);

		// Using an added condition (null result)
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, StateSpec
				.toStringFact("(on ?X ?)"), false, true);
		assertNull(results);

		// Using an added condition (no simplification)
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, StateSpec
				.toStringFact("(clear ?X)"), false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertTrue(results.contains(StateSpec.toStringFact("(clear ?X)")));
		assertEquals(results.size(), 2);

		// Using an added condition (swapped result)
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?)"));
		results = sut_.simplifyRule(ruleConds, StateSpec
				.toStringFact("(above ?X ?)"), false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertEquals(results.size(), 1);

		// Testing double-negated condition
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, StateSpec
				.toStringFact("(not (above ?X ?))"), false, true);
		assertNull(results);

		// Testing illegal condition
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		ruleConds.add(StateSpec.toStringFact("(onFloor ?X)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNull(results);
		results = sut_.simplifyRule(ruleConds, null, true, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(onFloor ?X)")));
		assertEquals(results.size(), 1);

		// Testing same condition
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?)"));
		results = sut_.simplifyRule(ruleConds, StateSpec
				.toStringFact("(on ?X ?)"), false, true);
		assertNull(results);

		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?)"));
		results = sut_.simplifyRule(ruleConds, StateSpec
				.toStringFact("(not (on ?X ?))"), false, true);
		assertNull(results);

		// Testing unification
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?)"));
		results = sut_.simplifyRule(ruleConds, StateSpec
				.toStringFact("(on ?X ?Y)"), false, true);
		assertNull(results);

		// Testing double unification (onX? -> aboveX? which is removed)
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?)"));
		ruleConds.add(StateSpec.toStringFact("(above ?X a)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X a)")));
		assertEquals(results.size(), 1);

		// Testing complex simplification
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		ruleConds.add(StateSpec.toStringFact("(above ?X ?Y)"));
		ruleConds.add(StateSpec.toStringFact("(above ? ?Y)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?Y)")));
		assertEquals(results.size(), 1);

		// Even more complex
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?Y)"));
		ruleConds.add(StateSpec.toStringFact("(on ? ?Y)"));
		ruleConds.add(StateSpec.toStringFact("(not (onFloor ?X))"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?Y)")));
		assertEquals(results.size(), 1);

		// Testing equivalent conditions (prefer left side of equation to right)
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(not (on ?X ?))"));
		ruleConds.add(StateSpec.toStringFact("(onFloor ?X)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(onFloor ?X)")));
		assertEquals(results.size(), 1);

		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		ruleConds.add(StateSpec.toStringFact("(not (onFloor ?X))"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertEquals(results.size(), 1);

		// Testing swapped for left equivalent conditions
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(not (on ?X ?))"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(onFloor ?X)")));
		assertEquals(results.size(), 1);

		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(not (onFloor ?X))"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertEquals(results.size(), 1);

		// Testing unification of background knowledge
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?Y)"));
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(on ?X ?Y)")));
		assertEquals(results.size(), 1);

		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?)"));
		ruleConds.add(StateSpec.toStringFact("(above ?X ?Y)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?Y)")));
		assertEquals(results.size(), 1);

		// Testing unification on a number of matches
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		ruleConds.add(StateSpec.toStringFact("(above ?Y b)"));
		ruleConds.add(StateSpec.toStringFact("(above ?Y ?)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertTrue(results.contains(StateSpec.toStringFact("(above ?Y b)")));
		assertEquals(results.size(), 2);

		// Testing unification on a number of matches
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?Y b)"));
		ruleConds.add(StateSpec.toStringFact("(above ?Y ?)"));
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertTrue(results.contains(StateSpec.toStringFact("(above ?Y b)")));
		assertEquals(results.size(), 2);

		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X a)"));
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		ruleConds.add(StateSpec.toStringFact("(above ?Y ?)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X a)")));
		assertTrue(results.contains(StateSpec.toStringFact("(above ?Y ?)")));
		assertEquals(results.size(), 2);
	}

	@Test
	public void testSimplifyRuleBWMove() {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld", "onAB");
		sut_ = new RuleCreation();
		assertTrue("No loaded agent observations. Cannot run test.",
				AgentObservations.loadAgentObservations());

		// Strange issue:
		SortedSet<StringFact> ruleConds = new TreeSet<StringFact>(
				ConditionComparator.getInstance());
		ruleConds.add(StateSpec.toStringFact("(highest ?Y)"));
		ruleConds.add(StateSpec.toStringFact("(above ?Y ?)"));
		ruleConds.add(StateSpec.toStringFact("(block ?Y)"));
		SortedSet<StringFact> results = sut_.simplifyRule(ruleConds, null,
				false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(highest ?Y)")));
		assertTrue(results.contains(StateSpec.toStringFact("(block ?Y)")));
		assertEquals(results.size(), 2);

		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(highest ?Y)"));
		ruleConds.add(StateSpec.toStringFact("(above ?Y floor)"));
		ruleConds.add(StateSpec.toStringFact("(above ?Y ?)"));
		ruleConds.add(StateSpec.toStringFact("(block ?Y)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(highest ?Y)")));
		assertTrue(results.contains(StateSpec.toStringFact("(above ?Y floor)")));
		assertTrue(results.contains(StateSpec.toStringFact("(block ?Y)")));
		assertEquals(results.size(), 3);

		// Test the (block X) <=> (above X ?) rule
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(block ?X)")));
		assertEquals(results.size(), 1);

		// Test the invariants
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(clear floor)"));
		ruleConds.add(StateSpec.toStringFact("(floor floor)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(floor floor)")));
		assertEquals(results.size(), 1);

		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above a floor)"));
		ruleConds.add(StateSpec.toStringFact("(floor floor)"));
		ruleConds.add(StateSpec.toStringFact("(block a)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(floor floor)")));
		assertTrue(results.contains(StateSpec.toStringFact("(block a)")));
		assertEquals(results.size(), 2);
	}

	@Test
	public void testEquivalenceRules() {
		// Set up the allowable conditions
		Collection<StringFact> conditions = new HashSet<StringFact>();
		conditions.add(StateSpec.toStringFact("(on ?X ?)"));
		conditions.add(StateSpec.toStringFact("(above ?X ?)"));
		conditions.add(StateSpec.toStringFact("(highest ?X)"));
		conditions.add(StateSpec.toStringFact("(clear ?X)"));
		AgentObservations.getInstance().setActionConditions("moveFloor",
				conditions);
		conditions = new HashSet<StringFact>();
		conditions.add(StateSpec.toStringFact("(on ?X ?)"));
		conditions.add(StateSpec.toStringFact("(on ?Y ?)"));
		conditions.add(StateSpec.toStringFact("(above ?X ?)"));
		conditions.add(StateSpec.toStringFact("(above ?Y ?)"));
		conditions.add(StateSpec.toStringFact("(highest ?X)"));
		conditions.add(StateSpec.toStringFact("(highest ?Y)"));
		conditions.add(StateSpec.toStringFact("(clear ?X)"));
		conditions.add(StateSpec.toStringFact("(clear ?Y)"));
		conditions.add(StateSpec.toStringFact("(onFloor ?X)"));
		conditions.add(StateSpec.toStringFact("(onFloor ?Y)"));
		AgentObservations.getInstance().setActionConditions("move", conditions);

		// Set up the equivalence and other rules
		SortedSet<BackgroundKnowledge> backKnow = new TreeSet<BackgroundKnowledge>();
		backKnow.add(new BackgroundKnowledge("(above ?X ?) <=> (on ?X ?)",
				false));
		backKnow.add(new BackgroundKnowledge(
				"(above ?X ?) <=> (not (onFloor ?X))", false));
		backKnow.add(new BackgroundKnowledge("(above ? ?Y) <=> (on ? ?Y)",
				false));
		backKnow.add(new BackgroundKnowledge(
				"(above ? ?Y) <=> (not (clear ?Y))", false));
		backKnow.add(new BackgroundKnowledge(
				"(clear ?X) <=> (not (above ? ?X))", false));
		backKnow.add(new BackgroundKnowledge("(clear ?X) <=> (not (on ? ?X))",
				false));
		backKnow.add(new BackgroundKnowledge(
				"(on ?X ?) <=> (not (onFloor ?X))", false));
		backKnow.add(new BackgroundKnowledge("(on ? ?Y) <=> (not (clear ?Y))",
				false));
		backKnow.add(new BackgroundKnowledge(
				"(onFloor ?X) <=> (not (above ?X ?))", false));
		backKnow.add(new BackgroundKnowledge(
				"(onFloor ?X) <=> (not (on ?X ?))", false));
		backKnow.add(new BackgroundKnowledge("(above ?X ?Y) => (above ?X ?)",
				false));
		backKnow.add(new BackgroundKnowledge("(above ?X ?Y) => (above ? ?Y)",
				false));
		backKnow.add(new BackgroundKnowledge(
				"(on ?X ?Y) (above ?Y ?Z) => (assert (above ?X ?Z))", false));
		backKnow.add(new BackgroundKnowledge("(highest ?X) => (clear ?X)",
				false));
		backKnow.add(new BackgroundKnowledge(
				"(on ?X ?Y) => (assert (above ?X ?Y))", false));
		backKnow.add(new BackgroundKnowledge("(on ?X ?Y) => (on ?X ?)", false));
		backKnow.add(new BackgroundKnowledge("(on ?X ?Y) => (on ? ?Y)", false));
		backKnow.add(new BackgroundKnowledge(
				"(block ?Z) (on ?X ?Y) => (not (on ?X ?Z))", false));
		backKnow
				.add(new BackgroundKnowledge("(on ?X ?) => (above ?X ?)", false));
		backKnow.add(new BackgroundKnowledge(
				"(block ?Y) (not (on ? ?Y)) => (assert (clear ?Y))", false));
		AgentObservations.getInstance().setBackgroundKnowledge(backKnow);

		// Basic implication test
		SortedSet<StringFact> ruleConds = new TreeSet<StringFact>(
				ConditionComparator.getInstance());
		ruleConds.add(StateSpec.toStringFact("(clear ?X)"));
		ruleConds.add(StateSpec.toStringFact("(highest ?X)"));
		SortedSet<StringFact> results = sut_.simplifyRule(ruleConds, null,
				false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(highest ?X)")));
		assertEquals(results.size(), 1);

		// Basic equivalency swap test
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertEquals(results.size(), 1);

		// Basic negated equivalency swap test
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(not (clear ?X))"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ? ?X)")));
		assertEquals(results.size(), 1);
	}
}