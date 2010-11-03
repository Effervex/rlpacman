package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import relationalFramework.StateSpec;
import relationalFramework.StringFact;
import relationalFramework.agentObservations.ConditionBeliefs;

public class ConditionBeliefsTest {

	@Before
	public void setUp() throws Exception {
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
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
		assertTrue(cb.noteTrueRelativeFacts(new ArrayList<StringFact>()));
		assertTrue(cb.getAlwaysTrue().isEmpty());
		assertTrue(cb.getOccasionallyTrue().isEmpty());
		assertFalse(cb.getNeverTrue().isEmpty());
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(on ?X ?)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(on ? ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(highest ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ?X ?)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ? ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(onFloor ?X)")));
		assertFalse(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(clear ?X)")));
		assertEquals(cb.getNeverTrue().size(), 6);

		cb = new ConditionBeliefs("on");
		assertTrue(cb.noteTrueRelativeFacts(new ArrayList<StringFact>()));
		assertFalse(cb.getAlwaysTrue().isEmpty());
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
				StateSpec.toStringFact("(onFloor ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(onFloor ?Y)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(clear ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(clear ?Y)")));
		assertEquals(cb.getNeverTrue().size(), 13);
	}

	@Test
	public void testAddNeverSeenPredsEmptyTyped() {
		StateSpec.initInstance("rlPacMan.PacMan");

		ConditionBeliefs cb = new ConditionBeliefs("edible");
		assertTrue(cb.noteTrueRelativeFacts(new ArrayList<StringFact>()));
		assertTrue(cb.getAlwaysTrue().isEmpty());
		assertTrue(cb.getOccasionallyTrue().isEmpty());
		assertFalse(cb.getNeverTrue().isEmpty());
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(distanceGhost ? ?X ?)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(blinking ?X)")));
		assertFalse(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(edible ?X)")));
		assertEquals(cb.getNeverTrue().size(), 2);

		cb = new ConditionBeliefs("distanceGhost");
		assertTrue(cb.noteTrueRelativeFacts(new ArrayList<StringFact>()));
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
		assertEquals(cb.getNeverTrue().size(), 6);
	}

	@Test
	public void testNoteTrueRelativeFacts() {
		// Testing adding of never facts with already added true facts
		ConditionBeliefs cb = new ConditionBeliefs("clear");
		Collection<StringFact> trueFacts = new ArrayList<StringFact>();
		trueFacts.add(StateSpec.toStringFact("on ?X ?)"));
		trueFacts.add(StateSpec.toStringFact("above ?X ?)"));
		trueFacts.add(StateSpec.toStringFact("highest ?X)"));
		assertTrue(cb.noteTrueRelativeFacts(trueFacts));
		assertFalse(cb.getAlwaysTrue().isEmpty());
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(on ?X ?)")));
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(above ?X ?)")));
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(highest ?X)")));
		assertEquals(cb.getAlwaysTrue().size(), 3);
		assertTrue(cb.getOccasionallyTrue().isEmpty());
		assertFalse(cb.getNeverTrue().isEmpty());
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(on ? ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(above ? ?X)")));
		assertTrue(cb.getNeverTrue().contains(
				StateSpec.toStringFact("(onFloor ?X)")));
		assertEquals(cb.getNeverTrue().size(), 3);
		
		// Asserting the same again
		assertFalse(cb.noteTrueRelativeFacts(trueFacts));

		// Adding more true facts, some of which have changed
		trueFacts.clear();
		trueFacts.add(StateSpec.toStringFact("highest ?X)"));
		trueFacts.add(StateSpec.toStringFact("onFloor ?X)"));
		assertTrue(cb.noteTrueRelativeFacts(trueFacts));
		assertFalse(cb.getAlwaysTrue().isEmpty());
		assertTrue(cb.getAlwaysTrue().contains(
				StateSpec.toStringFact("(highest ?X)")));
		assertEquals(cb.getAlwaysTrue().size(), 1);
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
		assertEquals(cb.getNeverTrue().size(), 2);

		// Adding even more true facts, resulting in a fully indecisive
		// condition
		trueFacts.clear();
		trueFacts.add(StateSpec.toStringFact("onFloor ?X)"));
		trueFacts.add(StateSpec.toStringFact("on ? ?X)"));
		trueFacts.add(StateSpec.toStringFact("above ? ?X)"));
		assertTrue(cb.noteTrueRelativeFacts(trueFacts));
		assertTrue(cb.getAlwaysTrue().isEmpty());
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
		assertTrue(cb.getNeverTrue().isEmpty());
		
		// One more time
		assertFalse(cb.noteTrueRelativeFacts(trueFacts));
	}
}
