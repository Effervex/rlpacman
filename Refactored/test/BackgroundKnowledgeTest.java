package test;

import static org.junit.Assert.*;

import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.agentObservations.BackgroundKnowledge;
import cerrla.PolicyGenerator;

public class BackgroundKnowledgeTest {
	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld");
		PolicyGenerator.newInstance(0);
	}

	@Test
	public void testSimplifyA() {
		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(clear ?X) (above ? ?X) <=> (floor ?X)", false);
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear a)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ? a)"));
		boolean result = bk.simplify(ruleConds, false);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(floor a)")));

		// Other way around
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(floor b)"));
		result = bk.simplify(ruleConds, false);
		assertFalse(ruleConds.toString(), result);

		// More to it
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear a)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above b a)"));
		result = bk.simplify(ruleConds, false);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(floor a)")));
	}

	@Test
	public void testSimplifyB() {
		// Other equivalent rule
		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(above ?X ?) <=> (on ?X ?)", false);
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(on b ?)"));
		boolean result = bk.simplify(ruleConds, false);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(above b ?)")));

		// Ensure it doesn't work in this case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(on b c)"));
		result = bk.simplify(ruleConds, false);
		assertFalse(ruleConds.toString(), result);
	}

	@Test
	public void testSimplifyC() {
		// Positive case
		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(block ?X) <=> (above ?X ?)", false);
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(above b ?)"));
		boolean result = bk.simplify(ruleConds, false);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(block b)")));

		// Negative case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above b c)"));
		result = bk.simplify(ruleConds, false);
		assertFalse(ruleConds.toString(), result);
	}
}
