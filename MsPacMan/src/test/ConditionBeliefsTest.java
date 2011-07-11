package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.StateSpec;
import relationalFramework.RelationalPredicate;
import relationalFramework.agentObservations.ConditionBeliefs;

public class ConditionBeliefsTest {

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
	}

	@Test
	public void testConditionsBelief() {
		ConditionBeliefs cb = new ConditionBeliefs("clear");
		assertEquals(cb.getCondition(), "clear");
		assertNotNull(cb.getAlwaysTrue());
		assertTrue(cb.getAlwaysTrue().isEmpty());
		assertNotNull(cb.getNeverTrue());
		assertTrue(cb.getNeverTrue().isEmpty());
		assertNotNull(cb.getOccasionallyTrue());
		assertTrue(cb.getOccasionallyTrue().isEmpty());
	}

	@Test
	public void testNoteRelativeFactsEmpty() {
		// Adding no true facts (all never used)
		ConditionBeliefs cb = new ConditionBeliefs("clear");
		Collection<RelationalPredicate> untrueFacts = new ArrayList<RelationalPredicate>();
		assertTrue(cb.noteTrueRelativeFacts(new ArrayList<RelationalPredicate>(),
				untrueFacts, true));
		assertFalse(cb.getAlwaysTrue().isEmpty());
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(clear ?X)")));
		assertTrue(cb.getOccasionallyTrue().isEmpty());
		assertFalse(cb.getNeverTrue().isEmpty());
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(on ?X ?)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(on ? ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(on ?X ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(highest ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ?X ?)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ? ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ?X ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(onFloor ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(block ?X)")));
		assertEquals(cb.getNeverTrue().size(), 9);
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(on ?X ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(on ? ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(on ?X ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(highest ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(above ? ?X)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toStringFact("(above ?X ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(onFloor ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(block ?X)")));
		assertFalse(untrueFacts.contains(StateSpec.toStringFact("(clear ?X)")));
		assertEquals(untrueFacts.size(), 9);

		cb = new ConditionBeliefs("on");
		untrueFacts.clear();
		assertTrue(cb.noteTrueRelativeFacts(new ArrayList<RelationalPredicate>(),
				untrueFacts, true));
		assertFalse(cb.getAlwaysTrue().isEmpty());
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(on ?X ?Y)")));
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(on ?X ?)")));
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(on ? ?Y)")));
		assertTrue(cb.getOccasionallyTrue().isEmpty());
		assertFalse(cb.getNeverTrue().isEmpty());
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(on ?Y ?)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(on ? ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(on ?X ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(on ?Y ?Y)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(on ?Y ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(highest ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(highest ?Y)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ?X ?)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ?Y ?)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ? ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ? ?Y)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ?X ?Y)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ?Y ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ?X ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ?Y ?Y)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(onFloor ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(onFloor ?Y)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(clear ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(clear ?Y)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(block ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(block ?Y)")));
		assertEquals(cb.getNeverTrue().size(), 21);
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(on ?Y ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(on ? ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(on ?X ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(on ?Y ?Y)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(on ?Y ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(highest ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(highest ?Y)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(above ?Y ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(above ? ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(above ? ?Y)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toStringFact("(above ?X ?Y)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toStringFact("(above ?Y ?X)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toStringFact("(above ?X ?X)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toStringFact("(above ?Y ?Y)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(onFloor ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(onFloor ?Y)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(clear ?Y)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(block ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(block ?Y)")));
		assertEquals(untrueFacts.size(), 21);
	}

	@Test
	public void testAddNeverSeenPredsEmptyTyped() {
		StateSpec.initInstance("rlPacMan.PacMan");

		ConditionBeliefs cb = new ConditionBeliefs("edible");
		Collection<RelationalPredicate> untrueFacts = new ArrayList<RelationalPredicate>();
		assertTrue(cb.noteTrueRelativeFacts(new ArrayList<RelationalPredicate>(),
				untrueFacts, true));
		assertFalse(cb.getAlwaysTrue().isEmpty());
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(edible ?X)")));
		assertTrue(cb.getOccasionallyTrue().isEmpty());
		assertFalse(cb.getNeverTrue().isEmpty());
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(distanceGhost ? ?X ?)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(blinking ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(ghost ?X)")));
		assertEquals(cb.getNeverTrue().size(), 3);
		assertTrue(untrueFacts.contains(StateSpec
				.toStringFact("(distanceGhost ? ?X ?)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toStringFact("(blinking ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(ghost ?X)")));
		assertFalse(untrueFacts.contains(StateSpec.toStringFact("(edible ?X)")));
		assertEquals(untrueFacts.size(), 3);

		cb = new ConditionBeliefs("distanceGhost");
		untrueFacts.clear();
		assertTrue(cb.noteTrueRelativeFacts(new ArrayList<RelationalPredicate>(),
				untrueFacts, true));
		assertFalse(cb.getAlwaysTrue().isEmpty());
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(distanceGhost ?X ? ?)")));
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(distanceGhost ? ?Y ?)")));
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(distanceGhost ?X ?Y ?)")));
		assertTrue(cb.getOccasionallyTrue().isEmpty());
		assertFalse(cb.getNeverTrue().isEmpty());
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(distancePowerDot ?X ? ?)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(distanceDot ?X ? ?)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(distanceFruit ?X ? ?)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(distanceGhostCentre ?X ? ?)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(edible ?Y)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(blinking ?Y)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(pacman ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(ghost ?Y)")));
		assertEquals(cb.getNeverTrue().size(), 8);
		assertTrue(untrueFacts.contains(StateSpec
				.toStringFact("(distancePowerDot ?X ? ?)")));
		assertTrue(untrueFacts.contains(StateSpec
				.toStringFact("(distanceDot ?X ? ?)")));
		assertTrue(untrueFacts.contains(StateSpec
				.toStringFact("(distanceFruit ?X ? ?)")));
		assertTrue(untrueFacts.contains(StateSpec
				.toStringFact("(distanceGhostCentre ?X ? ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(edible ?Y)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toStringFact("(blinking ?Y)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(ghost ?Y)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(pacman ?X)")));
		assertEquals(untrueFacts.size(), 8);
	}

	@Test
	public void testNoteTrueRelativeFacts() {
		// Testing adding of never facts with already added true facts
		ConditionBeliefs cb = new ConditionBeliefs("clear");
		Collection<RelationalPredicate> trueFacts = new ArrayList<RelationalPredicate>();
		trueFacts.add(StateSpec.toStringFact("on ?X ?)"));
		trueFacts.add(StateSpec.toStringFact("above ?X ?)"));
		trueFacts.add(StateSpec.toStringFact("highest ?X)"));
		Collection<RelationalPredicate> untrueFacts = new ArrayList<RelationalPredicate>();
		assertTrue(cb.noteTrueRelativeFacts(trueFacts, untrueFacts, true));
		assertFalse(cb.getAlwaysTrue().isEmpty());
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(clear ?X)")));
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(on ?X ?)")));
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(above ?X ?)")));
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(highest ?X)")));
		assertEquals(cb.getAlwaysTrue().size(), 4);
		assertTrue(cb.getOccasionallyTrue().isEmpty());
		assertFalse(cb.getNeverTrue().isEmpty());
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(on ? ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ? ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(on ?X ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ?X ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(onFloor ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(block ?X)")));
		assertEquals(cb.getNeverTrue().size(), 6);
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(on ? ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(above ? ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(onFloor ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(on ?X ?X)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toStringFact("(above ?X ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(block ?X)")));
		assertEquals(untrueFacts.size(), 6);

		// Asserting the same again
		untrueFacts.clear();
		assertFalse(cb.noteTrueRelativeFacts(trueFacts, untrueFacts, true));

		// Adding more true facts, some of which have changed
		trueFacts.clear();
		trueFacts.add(StateSpec.toStringFact("highest ?X)"));
		trueFacts.add(StateSpec.toStringFact("onFloor ?X)"));
		untrueFacts.clear();
		assertTrue(cb.noteTrueRelativeFacts(trueFacts, untrueFacts, true));
		assertFalse(cb.getAlwaysTrue().isEmpty());
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(clear ?X)")));
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(highest ?X)")));
		assertEquals(cb.getAlwaysTrue().size(), 2);
		assertFalse(cb.getOccasionallyTrue().isEmpty());
		assertTrue(cb.getOccasionallyTrue().contains(
				StateSpec.toStringFact("(on ?X ?)")));
		assertTrue(cb.getOccasionallyTrue().contains(
				StateSpec.toStringFact("(above ?X ?)")));
		assertTrue(cb.getOccasionallyTrue().contains(
				StateSpec.toStringFact("(onFloor ?X)")));
		assertEquals(cb.getOccasionallyTrue().size(), 3);
		assertFalse(cb.getNeverTrue().isEmpty());
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(on ? ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ? ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(on ?X ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ?X ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(block ?X)")));
		assertEquals(cb.getNeverTrue().size(), 5);
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(on ? ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(above ? ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(on ?X ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(on ?X ?X)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toStringFact("(above ?X ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(block ?X)")));
		assertEquals(untrueFacts.size(), 7);

		// Adding even more true facts, resulting in a fully indecisive
		// condition
		trueFacts.clear();
		trueFacts.add(StateSpec.toStringFact("onFloor ?X)"));
		trueFacts.add(StateSpec.toStringFact("on ? ?X)"));
		trueFacts.add(StateSpec.toStringFact("above ? ?X)"));
		untrueFacts.clear();
		assertTrue(cb.noteTrueRelativeFacts(trueFacts, untrueFacts, true));
		assertFalse(cb.getAlwaysTrue().isEmpty());
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(clear ?X)")));
		assertFalse(cb.getOccasionallyTrue().isEmpty());
		assertTrue(cb.getOccasionallyTrue().contains(
				StateSpec.toStringFact("(on ?X ?)")));
		assertTrue(cb.getOccasionallyTrue().contains(
				StateSpec.toStringFact("(above ?X ?)")));
		assertTrue(cb.getOccasionallyTrue().contains(
				StateSpec.toStringFact("(onFloor ?X)")));
		assertTrue(cb.getOccasionallyTrue().contains(
				StateSpec.toStringFact("(highest ?X)")));
		assertTrue(cb.getOccasionallyTrue().contains(
				StateSpec.toStringFact("(on ? ?X)")));
		assertTrue(cb.getOccasionallyTrue().contains(
				StateSpec.toStringFact("(above ? ?X)")));
		assertFalse(cb.getNeverTrue().isEmpty());
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(on ?X ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ?X ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(block ?X)")));
		assertEquals(cb.getNeverTrue().size(), 3);
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(highest ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(on ?X ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(on ?X ?X)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toStringFact("(above ?X ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toStringFact("(block ?X)")));
		assertEquals(untrueFacts.size(), 6);

		// One more time
		untrueFacts.clear();
		assertFalse(cb.noteTrueRelativeFacts(trueFacts, untrueFacts, true));
	}
}
