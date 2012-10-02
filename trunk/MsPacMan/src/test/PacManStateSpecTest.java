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
 *    src/test/PacManStateSpecTest.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package test;

import static org.junit.Assert.*;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import org.junit.Before;
import org.junit.Test;


public class PacManStateSpecTest {
	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("rlPacMan.PacMan");
	}

	@Test
	public void testRuleCreation() {
		// All the main tests are covered in the BlocksWorld StateSpec test

		// Testing numbers
		RelationalRule rule = new RelationalRule(
				"(distanceGhost ?Player ?Ghost 4) => (fromGhost ?Ghost 4)");
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(distanceGhost ?Player ?Ghost 4)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?Player ?Ghost))")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(pacman ?Player)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(ghost ?Ghost)")));
		assertEquals(rule.getSimplifiedConditions(false).size(), 4);
		assertTrue(rule.getStringConditions().indexOf("distanceGhost") < rule
				.getStringConditions().indexOf("pacman"));
		assertTrue(rule.getStringConditions().indexOf("pacman") < rule
				.getStringConditions().indexOf("test"));
		assertEquals(rule.getAction(), StateSpec
				.toRelationalPredicate("(fromGhost ?Ghost 4)"));

		// Testing conditional &:elements
		rule = new RelationalRule(
				"(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 1 4))"
						+ " => (fromGhost ?Ghost ?Dist0)");
		assertEquals(rule.getSimplifiedConditions(false).size(), 4);
		assertTrue(rule
				.getSimplifiedConditions(false)
				.contains(
						StateSpec
								.toRelationalPredicate("(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 1 4))")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?Player ?Ghost))")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(pacman ?Player)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(ghost ?Ghost)")));
		assertTrue(rule.getStringConditions().indexOf("distanceGhost") < rule
				.getStringConditions().indexOf("pacman"));
		assertTrue(rule.getStringConditions().indexOf("pacman") < rule
				.getStringConditions().indexOf("test"));
		assertEquals(rule.getAction(), StateSpec
				.toRelationalPredicate("(fromGhost ?Ghost ?Dist0)"));
	}
}
