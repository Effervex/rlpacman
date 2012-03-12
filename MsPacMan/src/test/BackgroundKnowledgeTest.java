package test;

import static org.junit.Assert.*;

import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

import cerrla.modular.GoalCondition;

import relationalFramework.agentObservations.BackgroundKnowledge;
import relationalFramework.agentObservations.LocalAgentObservations;

public class BackgroundKnowledgeTest {
	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld");
	}

	@Test
	public void testSimplifyA() {
		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(clear ?X) (above ? ?X) <=> (floor ?X)");
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?Z_Unb ?X)"));
		boolean result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(floor ?X)")));

		// Not using unbound
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?Z ?X)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);

		// Other way around
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(floor ?X)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);
	}

	@Test
	public void testSimplifyB() {
		// Other equivalent rule
		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(above ?X ?) <=> (on ?X ?)");
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(on ?X ?Z_Unb)"));
		boolean result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(above ?X ?)")));

		// Not using unbound
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(on ?X ?Z)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);

		// Other way around
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?Z_Unb)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);
	}

	@Test
	public void testSimplifyC() {
		// Positive case
		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(above ?X ?) <=> (block ?X)");
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(block ?X)"));
		boolean result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(above ?X ?)")));

		// Negative case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?Z_Unb)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);
	}
}
