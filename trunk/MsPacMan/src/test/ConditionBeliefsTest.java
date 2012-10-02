/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/test/ConditionBeliefsTest.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package test;

import static org.junit.Assert.*;

import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.agentObservations.ConditionBeliefs;

public class ConditionBeliefsTest {

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
	}

	@Test
	public void testNoteRelativeFactsEmpty() {
		// Adding no true facts (all never used)
		ConditionBeliefs cb = new ConditionBeliefs("clear");
		Collection<RelationalPredicate> untrueFacts = new ArrayList<RelationalPredicate>();
		assertTrue(cb.noteTrueRelativeFacts(new ArrayList<RelationalPredicate>(),
				untrueFacts, true));
		assertFalse(cb.getAlwaysTrue(null).isEmpty());
		assertTrue(cb.getAlwaysTrue(null).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(cb.getOccasionallyTrue(null).isEmpty());
		assertFalse(cb.getNeverTrue(null).isEmpty());
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ?X ?)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ? ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ?X ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(highest ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ?X ?)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ? ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ?X ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(onFloor ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertEquals(cb.getNeverTrue(null).size(), 9);
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(on ?X ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(on ? ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(on ?X ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(highest ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(above ?X ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(above ? ?X)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toRelationalPredicate("(above ?X ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(onFloor ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(block ?X)")));
		assertFalse(untrueFacts.contains(StateSpec.toRelationalPredicate("(clear ?X)")));
		assertEquals(untrueFacts.size(), 9);

		cb = new ConditionBeliefs("on");
		untrueFacts.clear();
		assertTrue(cb.noteTrueRelativeFacts(new ArrayList<RelationalPredicate>(),
				untrueFacts, true));
		assertFalse(cb.getAlwaysTrue(null).isEmpty());
		assertTrue(cb.getAlwaysTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ?X ?Y)")));
		assertTrue(cb.getAlwaysTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ?X ?)")));
		assertTrue(cb.getAlwaysTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ? ?Y)")));
		assertTrue(cb.getOccasionallyTrue(null).isEmpty());
		assertFalse(cb.getNeverTrue(null).isEmpty());
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ?Y ?)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ? ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ?X ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ?Y ?Y)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ?Y ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(highest ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(highest ?Y)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ?X ?)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ?Y ?)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ? ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ? ?Y)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ?X ?Y)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ?Y ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ?X ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ?Y ?Y)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(onFloor ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(onFloor ?Y)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(block ?Y)")));
		assertEquals(cb.getNeverTrue(null).size(), 21);
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(on ?Y ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(on ? ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(on ?X ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(on ?Y ?Y)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(on ?Y ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(highest ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(highest ?Y)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(above ?X ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(above ?Y ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(above ? ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(above ? ?Y)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toRelationalPredicate("(above ?X ?Y)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toRelationalPredicate("(above ?Y ?X)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toRelationalPredicate("(above ?X ?X)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toRelationalPredicate("(above ?Y ?Y)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(onFloor ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(onFloor ?Y)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(block ?Y)")));
		assertEquals(untrueFacts.size(), 21);
	}

	@Test
	public void testAddNeverSeenPredsEmptyTyped() {
		StateSpec.initInstance("rlPacMan.PacMan");

		ConditionBeliefs cb = new ConditionBeliefs("edible");
		Collection<RelationalPredicate> untrueFacts = new ArrayList<RelationalPredicate>();
		assertTrue(cb.noteTrueRelativeFacts(new ArrayList<RelationalPredicate>(),
				untrueFacts, true));
		assertFalse(cb.getAlwaysTrue(null).isEmpty());
		assertTrue(cb.getAlwaysTrue(null).contains(
				StateSpec.toRelationalPredicate("(edible ?X)")));
		assertTrue(cb.getOccasionallyTrue(null).isEmpty());
		assertFalse(cb.getNeverTrue(null).isEmpty());
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(distance ?X ?)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(blinking ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(ghost ?X)")));
		assertEquals(cb.getNeverTrue(null).size(), 3);
		assertTrue(untrueFacts.contains(StateSpec
				.toRelationalPredicate("(distance ?X ?)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toRelationalPredicate("(blinking ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(ghost ?X)")));
		assertFalse(untrueFacts.contains(StateSpec.toRelationalPredicate("(edible ?X)")));
		assertEquals(untrueFacts.size(), 3);

		cb = new ConditionBeliefs("distanceGhost");
		untrueFacts.clear();
		assertTrue(cb.noteTrueRelativeFacts(new ArrayList<RelationalPredicate>(),
				untrueFacts, true));
		assertFalse(cb.getAlwaysTrue(null).isEmpty());
		assertTrue(cb.getAlwaysTrue(null).contains(
				StateSpec.toRelationalPredicate("(distanceGhost ?X ? ?)")));
		assertTrue(cb.getAlwaysTrue(null).contains(
				StateSpec.toRelationalPredicate("(distanceGhost ? ?Y ?)")));
		assertTrue(cb.getAlwaysTrue(null).contains(
				StateSpec.toRelationalPredicate("(distanceGhost ?X ?Y ?)")));
		assertTrue(cb.getOccasionallyTrue(null).isEmpty());
		assertFalse(cb.getNeverTrue(null).isEmpty());
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(distancePowerDot ?X ? ?)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(distanceDot ?X ? ?)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(distanceFruit ?X ? ?)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(distanceGhostCentre ?X ? ?)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(edible ?Y)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(blinking ?Y)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(pacman ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(ghost ?Y)")));
		assertEquals(cb.getNeverTrue(null).size(), 8);
		assertTrue(untrueFacts.contains(StateSpec
				.toRelationalPredicate("(distancePowerDot ?X ? ?)")));
		assertTrue(untrueFacts.contains(StateSpec
				.toRelationalPredicate("(distanceDot ?X ? ?)")));
		assertTrue(untrueFacts.contains(StateSpec
				.toRelationalPredicate("(distanceFruit ?X ? ?)")));
		assertTrue(untrueFacts.contains(StateSpec
				.toRelationalPredicate("(distanceGhostCentre ?X ? ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(edible ?Y)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toRelationalPredicate("(blinking ?Y)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(ghost ?Y)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(pacman ?X)")));
		assertEquals(untrueFacts.size(), 8);
	}

	@Test
	public void testNoteTrueRelativeFacts() {
		// Testing adding of never facts with already added true facts
		ConditionBeliefs cb = new ConditionBeliefs("clear");
		Collection<RelationalPredicate> trueFacts = new ArrayList<RelationalPredicate>();
		trueFacts.add(StateSpec.toRelationalPredicate("on ?X ?)"));
		trueFacts.add(StateSpec.toRelationalPredicate("above ?X ?)"));
		trueFacts.add(StateSpec.toRelationalPredicate("highest ?X)"));
		Collection<RelationalPredicate> untrueFacts = new HashSet<RelationalPredicate>();
		assertTrue(cb.noteTrueRelativeFacts(trueFacts, untrueFacts, true));
		assertFalse(cb.getAlwaysTrue(null).isEmpty());
		assertTrue(cb.getAlwaysTrue(null).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(cb.getAlwaysTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ?X ?)")));
		assertTrue(cb.getAlwaysTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ?X ?)")));
		assertTrue(cb.getAlwaysTrue(null).contains(
				StateSpec.toRelationalPredicate("(highest ?X)")));
		assertEquals(cb.getAlwaysTrue(null).size(), 4);
		assertTrue(cb.getOccasionallyTrue(null).isEmpty());
		assertFalse(cb.getNeverTrue(null).isEmpty());
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ? ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ? ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ?X ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ?X ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(onFloor ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertEquals(cb.getNeverTrue(null).size(), 6);
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(on ? ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(above ? ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(onFloor ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(on ?X ?X)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toRelationalPredicate("(above ?X ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(block ?X)")));
		assertEquals(untrueFacts.size(), 6);

		// Asserting the same again
		untrueFacts.clear();
		assertFalse(cb.noteTrueRelativeFacts(trueFacts, untrueFacts, true));

		// Adding more true facts, some of which have changed
		trueFacts.clear();
		trueFacts.add(StateSpec.toRelationalPredicate("highest ?X)"));
		trueFacts.add(StateSpec.toRelationalPredicate("onFloor ?X)"));
		untrueFacts.clear();
		assertTrue(cb.noteTrueRelativeFacts(trueFacts, untrueFacts, true));
		assertFalse(cb.getAlwaysTrue(null).isEmpty());
		assertTrue(cb.getAlwaysTrue(null).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(cb.getAlwaysTrue(null).contains(
				StateSpec.toRelationalPredicate("(highest ?X)")));
		assertEquals(cb.getAlwaysTrue(null).size(), 2);
		assertFalse(cb.getOccasionallyTrue(null).isEmpty());
		assertTrue(cb.getOccasionallyTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ?X ?)")));
		assertTrue(cb.getOccasionallyTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ?X ?)")));
		assertTrue(cb.getOccasionallyTrue(null).contains(
				StateSpec.toRelationalPredicate("(onFloor ?X)")));
		assertEquals(cb.getOccasionallyTrue(null).size(), 3);
		assertFalse(cb.getNeverTrue(null).isEmpty());
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ? ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ? ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ?X ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ?X ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertEquals(cb.getNeverTrue(null).size(), 5);
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(on ? ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(above ? ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(on ?X ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(above ?X ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(on ?X ?X)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toRelationalPredicate("(above ?X ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(block ?X)")));
		assertEquals(untrueFacts.size(), 7);

		// Adding even more true facts, resulting in a fully indecisive
		// condition
		trueFacts.clear();
		trueFacts.add(StateSpec.toRelationalPredicate("onFloor ?X)"));
		trueFacts.add(StateSpec.toRelationalPredicate("on ? ?X)"));
		trueFacts.add(StateSpec.toRelationalPredicate("above ? ?X)"));
		untrueFacts.clear();
		assertTrue(cb.noteTrueRelativeFacts(trueFacts, untrueFacts, true));
		assertFalse(cb.getAlwaysTrue(null).isEmpty());
		assertTrue(cb.getAlwaysTrue(null).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertFalse(cb.getOccasionallyTrue(null).isEmpty());
		assertTrue(cb.getOccasionallyTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ?X ?)")));
		assertTrue(cb.getOccasionallyTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ?X ?)")));
		assertTrue(cb.getOccasionallyTrue(null).contains(
				StateSpec.toRelationalPredicate("(onFloor ?X)")));
		assertTrue(cb.getOccasionallyTrue(null).contains(
				StateSpec.toRelationalPredicate("(highest ?X)")));
		assertTrue(cb.getOccasionallyTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ? ?X)")));
		assertTrue(cb.getOccasionallyTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ? ?X)")));
		assertFalse(cb.getNeverTrue(null).isEmpty());
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(on ?X ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(above ?X ?X)")));
		assertTrue(cb.getNeverTrue(null).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertEquals(cb.getNeverTrue(null).size(), 3);
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(highest ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(on ?X ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(above ?X ?)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(on ?X ?X)")));
		assertTrue(untrueFacts
				.contains(StateSpec.toRelationalPredicate("(above ?X ?X)")));
		assertTrue(untrueFacts.contains(StateSpec.toRelationalPredicate("(block ?X)")));
		assertEquals(untrueFacts.size(), 6);

		// One more time
		untrueFacts.clear();
		assertFalse(cb.noteTrueRelativeFacts(trueFacts, untrueFacts, true));
	}
}
