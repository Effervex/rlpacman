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
 *    src/test/PacManRuleCreationTest.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package test;

import static org.junit.Assert.*;

import java.util.Set;

import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.LocalAgentObservations;

import org.junit.Before;
import org.junit.Test;

import cerrla.modular.GoalCondition;

public class PacManRuleCreationTest {
	private LocalAgentObservations.RuleMutation sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("rlPacManGeneral.PacMan");
		LocalAgentObservations lao = LocalAgentObservations
				.loadAgentObservations(GoalCondition.parseGoalCondition("blah"), null);
		sut_ = lao.getRuleMutation();
	}

	@Test
	public void testTypePreds() {
		// Normal RLGG case
		RelationalRule rule = new RelationalRule(
				"(thing ?X) (distance ?X ?#_2) => (moveTo ?X ?#_2)");
		Set<RelationalRule> mutants = sut_.specialiseRule(rule);
		assertEquals(mutants.size(), 12);
		
		mutants = sut_.specialiseRuleMinor(rule);
		assertEquals(mutants.size(), 3);
		
		rule = new RelationalRule(
				"(thing ?X) (distance ?X ?#_2) (not (dot ?X)) => (moveTo ?X ?#_2)");
		mutants = sut_.specialiseRule(rule);
		assertEquals(mutants.size(), 10);
		
		rule = new RelationalRule(
				"(distance ?X ?#_2) (edible ?X) => (moveTo ?X ?#_2)");
		mutants = sut_.specialiseRule(rule);
		assertEquals(mutants.size(), 2);
		
		rule = new RelationalRule(
				"(distance ?X ?#_2) (dot ?X) => (moveTo ?X ?#_2)");
		mutants = sut_.specialiseRule(rule);
		assertEquals(mutants.size(), 0);
	}
}
