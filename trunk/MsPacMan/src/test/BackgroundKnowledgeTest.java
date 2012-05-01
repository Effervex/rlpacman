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
	public void testNormalisation() {
		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(above ?X ?Y) <=> (on ?X ?)");
		assertEquals(bk.toString(), "(above ?X ?) <=> (on ?X ?)");
		
		bk = new BackgroundKnowledge(
				"(on ?X ?Y) <=> (above ?X ?)");
		assertEquals(bk.toString(), "(on ?X ?) <=> (above ?X ?)");
	}

	@Test
	public void testSimplifyA() {
		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(clear ?X) (above ? ?X) <=> (floor ?X)");
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?Unb_0 ?X)"));
		boolean result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(floor ?X)")));

		// Implication test
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ? ?X)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(floor ?X)"));
		result = bk.simplify(ruleConds);
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

		// Bound conditions
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?Bnd_0 ?X)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);

		// Not matching
		bk = new BackgroundKnowledge("(clear ?X) (above ?Y ?X) <=> (floor ?X)");
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);

		// Bound conditions
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?Bnd_0 ?X)"));
		result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(floor ?X)")));
	}

	@Test
	public void testSimplifyB() {
		// Other equivalent rule
		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(above ?X ?) <=> (on ?X ?)");
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(on ?X ?Unb_0)"));
		boolean result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(above ?X ?)")));

		// Not using unbound
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(on ?X ?Z)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);

		// Bound variable case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(on ?X ?Bnd_0)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);

		// Other way around
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?Unb_0)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);
	}

	@Test
	public void testSimplifyC() {
		// Positive case
		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(block ?X) <=> (above ?X ?)");
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		boolean result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(block ?X)")));

		// Unbound case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?X ?Unb_0)"));
		result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(block ?X)")));

		// Negative case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(block ?X)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);
	}

	//@Test
	public void testSimplifyCarcassonne() {
		StateSpec.initInstance("jCloisterZone.Carcassonne");
		LocalAgentObservations.loadAgentObservations(GoalCondition
				.parseGoalCondition("cool"));

		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(controls ?X ?Z) => (tileEdge ? ? ?Z)");
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(controls ?X ?)"));
		ruleConds.add(StateSpec
				.toRelationalPredicate("(tileEdge ?Y ?Unb_4 ?Unb_5)"));
		boolean result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);

		// True case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(controls ?Y ?X)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(tileEdge ? ? ?X)"));
		result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.size() == 1);

		// True case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(controls ?Y ?X)"));
		ruleConds.add(StateSpec
				.toRelationalPredicate("(tileEdge ?Unb_3 ?Unb_4 ?X)"));
		result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.size() == 1);

		// True case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(controls ?Y ?X)"));
		ruleConds.add(StateSpec
				.toRelationalPredicate("(tileEdge ?Unb_3 ?Unb_4 ?X)"));
		result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.size() == 1);

		// Self test
		bk = new BackgroundKnowledge("(controls ?X ?Y) => (controls ?X ?)");
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(controls ?X ?)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);

		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(controls ?X ?Y)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(controls ?X ?)"));
		result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.size() == 1);
		assertTrue(ruleConds.toString().contains("(controls ?X ?Y)"));
	}
}
