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
 *    src/test/BackgroundKnowledgeTest.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
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
				"(above ?A ?B) <=> (on ?A ?)");
		assertEquals(bk.toString(), "(above ?A ?) <=> (on ?A ?)");
		
		bk = new BackgroundKnowledge(
				"(on ?A ?B) <=> (above ?A ?)");
		assertEquals(bk.toString(), "(above ?A ?) <=> (on ?A ?)");
	}

	@Test
	public void testSimplifyA() {
		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(clear ?A) (above ? ?A) <=> (floor ?A)");
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?A)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?Unb_0 ?A)"));
		boolean result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(floor ?A)")));

		// Implication test
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?A)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ? ?A)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(floor ?A)"));
		result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(floor ?A)")));

		// Not using unbound
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?A)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?C ?A)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);

		// Other way around
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(floor ?A)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);

		// Bound conditions
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?A)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?Bnd_0 ?A)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);

		// Not matching
		bk = new BackgroundKnowledge("(clear ?A) (above ?B ?A) <=> (floor ?A)");
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?B)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?A)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?A ?)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);

		// Bound conditions
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(clear ?A)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?Bnd_0 ?A)"));
		result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(floor ?A)")));
	}

	@Test
	public void testSimplifyB() {
		// Other equivalent rule
		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(above ?A ?) <=> (on ?A ?)");
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(on ?A ?Unb_0)"));
		boolean result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(above ?A ?)")));

		// Not using unbound
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(on ?A ?C)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);

		// Bound variable case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(on ?A ?Bnd_0)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);

		// Other way around
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?A ?Unb_0)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);
	}

	@Test
	public void testSimplifyC() {
		// Positive case
		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(block ?A) <=> (above ?A ?)");
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?A ?)"));
		boolean result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(block ?A)")));

		// Unbound case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?A ?Unb_0)"));
		result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(block ?A)")));

		// Negative case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(block ?A)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);
	}

	//@Test
	public void testSimplifyCarcassonne() {
		StateSpec.initInstance("jCloisterZone.Carcassonne");
		LocalAgentObservations.loadAgentObservations(GoalCondition
				.parseGoalCondition("cool"), null);

		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(controls ?A ?C) => (tileEdge ? ? ?C)");
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(controls ?A ?)"));
		ruleConds.add(StateSpec
				.toRelationalPredicate("(tileEdge ?B ?Unb_4 ?Unb_5)"));
		boolean result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);

		// True case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(controls ?B ?A)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(tileEdge ? ? ?A)"));
		result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.size() == 1);

		// True case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(controls ?B ?A)"));
		ruleConds.add(StateSpec
				.toRelationalPredicate("(tileEdge ?Unb_3 ?Unb_4 ?A)"));
		result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.size() == 1);

		// True case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(controls ?B ?A)"));
		ruleConds.add(StateSpec
				.toRelationalPredicate("(tileEdge ?Unb_3 ?Unb_4 ?A)"));
		result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.size() == 1);

		// Self test
		bk = new BackgroundKnowledge("(controls ?A ?B) => (controls ?A ?)");
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(controls ?A ?)"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);

		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(controls ?A ?B)"));
		ruleConds.add(StateSpec.toRelationalPredicate("(controls ?A ?)"));
		result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.size() == 1);
		assertTrue(ruleConds.toString().contains("(controls ?A ?B)"));
	}
	
	@Test
	public void testUnboundRangeRemoval() {
		// Replacing an unbound range
		BackgroundKnowledge bk = new BackgroundKnowledge(
				"(thing ?A) <=> (height ?A ?)");
		SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>();
		ruleConds.add(StateSpec.toRelationalPredicate("(height ?A ?#_0)"));
		boolean result = bk.simplify(ruleConds);
		assertTrue(ruleConds.toString(), result);
		assertTrue(ruleConds.contains(StateSpec
				.toRelationalPredicate("(thing ?A)")));
		assertTrue(ruleConds.size() == 1);

		// Bound case
		ruleConds.clear();
		ruleConds.add(StateSpec.toRelationalPredicate("(above ?A ?#_0&:(0.0 <= ?#_0 <= 1.0))"));
		result = bk.simplify(ruleConds);
		assertFalse(ruleConds.toString(), result);
	}
}
