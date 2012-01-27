package test;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

import jess.JessException;
import jess.Rete;

import org.junit.Before;
import org.junit.Test;

import cerrla.modular.GoalCondition;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.LocalAgentObservations;
import util.ConditionComparator;

public class LocalAgentObservationsTest {
	private LocalAgentObservations sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
		sut_ = LocalAgentObservations.loadAgentObservations(new GoalCondition(
				StateSpec.getInstance().getGoalName()));
		assertNotNull("No loaded onAB agent observations. Cannot run test.",
				sut_);
	}

	@Test
	public void testHashCode() throws JessException {
		sut_ = new LocalAgentObservations(new GoalCondition(StateSpec
				.getInstance().getGoalName()));
		int prevHash = sut_.hashCode();

		Rete state = StateSpec.getInstance().getRete();
		state.assertString("(clear a)");
		sut_.scanState(StateSpec.extractFacts(state),
				new HashMap<String, String>());
		int newHash = sut_.hashCode();
		assertFalse(prevHash == newHash);
		prevHash = newHash;

		assertTrue(newHash == sut_.hashCode());

		state.reset();
		state.assertString("(on a b)");
		sut_.scanState(StateSpec.extractFacts(state),
				new HashMap<String, String>());
		newHash = sut_.hashCode();
		assertFalse(prevHash == newHash);

		assertTrue(newHash == sut_.hashCode());
	}

	@Test
	public void testGatherActionFacts() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetRLGGRules() {
		Collection<RelationalRule> rlggRules = sut_.getRLGGRules(new HashSet<RelationalRule>());
		for (RelationalRule rr : rlggRules) {
			SortedSet<RelationalPredicate> conds = rr.getConditions(false);
			if (rr.getActionPredicate().equals("move")) {
				// Clear ?X and Clear ?Y
				RelationalArgument[] args = { new RelationalArgument("?X") };
				RelationalPredicate fact = new RelationalPredicate(StateSpec
						.getInstance().getPredicateByName("clear"), args);
				assertTrue(conds.contains(fact));

				args[0] = new RelationalArgument("?Y");
				fact = new RelationalPredicate(StateSpec.getInstance()
						.getPredicateByName("clear"), args);
				assertTrue(conds.contains(fact));

				// 4 other conditions.
				assertTrue(conds.size() == 6);
			} else if (rr.getActionPredicate().equals("moveFloor")) {
				// Clear ?X and Clear ?Y
				RelationalArgument[] args = { new RelationalArgument("?X") };
				RelationalPredicate fact = new RelationalPredicate(StateSpec
						.getInstance().getPredicateByName("clear"), args);
				assertTrue(conds.contains(fact));

				// 4 other conditions.
				assertTrue(conds.size() == 4);
			} else
				fail("Untested action RLGG.");
		}
	}

	@Test
	public void testGetSpecificGoalConditions() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsValidGoalCondition() {
		fail("Not yet implemented");
	}

	@Test
	public void testObserveState() {
		fail("Not yet implemented");
	}

	@Test
	public void testScanState() {
		fail("Not yet implemented");
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
		sut_ = LocalAgentObservations.loadAgentObservations(new GoalCondition(
				StateSpec.getInstance().getGoalName()));
		assertNotNull("No loaded onAB agent observations. Cannot run test.",
				sut_);

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
