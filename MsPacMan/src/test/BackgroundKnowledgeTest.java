package test;

import static org.junit.Assert.*;

import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.agentObservations.BackgroundKnowledge;

public class BackgroundKnowledgeTest {
	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld");
	}

	@Test
	public void testSimplifyA() {
		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(floor ?X) <=> (clear ?X) (above ? ?X)");
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear a)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ? a)"));
		boolean result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(floor a)")));

		// Other way around
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(floor b)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);

		// More to it
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear a)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above b a)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);
		
		// Different rule
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear a)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above b a)"));
		bk = new BackgroundKnowledge(
				"(floor ?X) <=> (clear ?X) (above ?Y ?X)");
		result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(floor a)")));
	}

	@Test
	public void testSimplifyB() {
		// Other equivalent rule
		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(above ?X ?) <=> (on ?X ?)");
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(on b ?)"));
		boolean result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(above b ?)")));

		// Ensure it doesn't work in this case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(on b c)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);
	}

	@Test
	public void testSimplifyC() {
		// Positive case
		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(above ?X ?) <=> (block ?X)");
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(block b)"));
		boolean result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(above b ?)")));

		// Negative case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above b ?)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);
	}
}
