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
 *    src/test/BlocksWorldStateSpecTest.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package test;

import static org.junit.Assert.*;

import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedSet;

import jess.Rete;

import org.junit.Before;
import org.junit.Test;

import cerrla.modular.GeneralGoalCondition;
import cerrla.modular.SpecificGoalCondition;

import util.MultiMap;

public class BlocksWorldStateSpecTest {

	private StateSpec spec_;

	@Before
	public void setUp() throws Exception {
		spec_ = StateSpec.initInstance("blocksWorld.BlocksWorld");
	}

	@Test
	public void testReinitInstance() {
		try {
			spec_.getRete().eval("(assert (on a b))");
			spec_.getRete().eval("(facts)");
			StateSpec.reinitInstance(true);
			spec_.getRete().eval("(facts)");
		} catch (Exception e) {
			fail("Exception occured.");
		}
	}

	@Test
	public void testRuleCreation() {
		// Basic variable test
		RelationalRule rule = new RelationalRule("(clear ?X) => (moveFloor ?X)");
		// 2 assertions in the body: clear, and block
		assertEquals(rule.getSimplifiedConditions(false).size(), 2);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(),
				StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		Collection<SpecificGoalCondition> constants = rule
				.getSpecificSubGoals();
		assertTrue(constants.isEmpty());
		Collection<GeneralGoalCondition>[] generalConds = rule
				.getGeneralisedConditions();
		assertFalse(generalConds[0].isEmpty());
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("clear"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("block"))));
		assertEquals(generalConds[0].size(), 2);
		assertTrue(generalConds[1].isEmpty());


		// Test for a constant
		rule = new RelationalRule("(clear a) => (moveFloor a)");
		// 2 assertions in the body: clear, and block, with a constant
		// variable
		assertEquals(rule.getSimplifiedConditions(false).size(), 2);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear a)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block a)")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(),
				StateSpec.toRelationalPredicate("(moveFloor a)"));
		constants = rule.getSpecificSubGoals();
		generalConds = rule.getGeneralisedConditions();
		RelationalPredicate strFact = StateSpec.getInstance().getPredicates()
				.get("clear");
		assertFalse(constants.isEmpty());
		assertTrue(constants.contains(new SpecificGoalCondition(
				new RelationalPredicate(strFact, new String[] { "a" }))));
		assertEquals(constants.size(), 1);
		assertFalse(generalConds[0].isEmpty());
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("clear"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("block"))));
		assertEquals(generalConds[0].size(), 2);
		assertTrue(generalConds[1].isEmpty());

		// Test for constants (no inequals)
		rule = new RelationalRule("(clear a) (clear b) => (moveFloor a)");
		// 4 assertions in the body: 2 clears, and 2 blocks, without an
		// inequality test
		assertEquals(rule.getSimplifiedConditions(false).size(), 4);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear a)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block a)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear b)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block b)")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(),
				StateSpec.toRelationalPredicate("(moveFloor a)"));
		constants = rule.getSpecificSubGoals();
		generalConds = rule.getGeneralisedConditions();
		strFact = StateSpec.getInstance().getPredicates().get("clear");
		Collection<RelationalPredicate> constantConds = new ArrayList<RelationalPredicate>();
		constantConds
				.add(new RelationalPredicate(strFact, new String[] { "a" }));
		constantConds
				.add(new RelationalPredicate(strFact, new String[] { "b" }));
		assertFalse(constants.isEmpty());
		assertTrue(constants.contains(new SpecificGoalCondition(
				new RelationalPredicate(strFact, new String[] { "a" }))));
		assertTrue(constants.contains(new SpecificGoalCondition(
				new RelationalPredicate(strFact, new String[] { "b" }))));
		assertEquals(constants.size(), 2);
		assertFalse(generalConds[0].isEmpty());
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("clear"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("block"))));
		assertEquals(generalConds[0].size(), 2);
		assertTrue(generalConds[1].isEmpty());

		// Multiple conditions, one term
		rule = new RelationalRule("(clear ?X) (highest ?X) => (moveFloor ?X)");
		// 3 assertions in the body: clear, highest, and a single block
		assertEquals(rule.getSimplifiedConditions(false).size(), 3);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(highest ?X)")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(),
				StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		constants = rule.getSpecificSubGoals();
		generalConds = rule.getGeneralisedConditions();
		assertTrue(constants.isEmpty());
		assertFalse(generalConds[0].isEmpty());
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("clear"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("highest"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("block"))));
		assertEquals(generalConds[0].size(), 3);
		assertTrue(generalConds[1].isEmpty());

		// Multiple conditions, two terms
		rule = new RelationalRule("(clear ?X) (highest ?Y) => (moveFloor ?X)");
		// 5 assertions in the body: clear, highest, two blocks, and an
		// inequals test
		assertEquals(rule.getSimplifiedConditions(false).size(), 5);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(highest ?Y)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?Y)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?Y ?X))")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertTrue(rule.getStringConditions().indexOf("block") > rule
				.getStringConditions().indexOf("test"));
		assertEquals(rule.getAction(),
				StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		constants = rule.getSpecificSubGoals();
		generalConds = rule.getGeneralisedConditions();
		assertTrue(constants.isEmpty());
		assertFalse(generalConds[0].isEmpty());
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("clear"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("highest"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("block"))));
		assertEquals(generalConds[0].size(), 3);
		assertTrue(generalConds[1].isEmpty());

		// Variables and constants
		rule = new RelationalRule("(on ?X a) (above b ?Y) => (moveFloor ?X)");
		// 5 assertions in the body: clear, highest, two blocks, and an
		// inequals test
		assertEquals(rule.getSimplifiedConditions(false).size(), 8);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?X a)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block a)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(above b ?Y)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block b)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?Y)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?X ?Y a b))")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?Y a b))")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertTrue(rule.getStringConditions().indexOf("block") > rule
				.getStringConditions().indexOf("test"));
		assertEquals(rule.getAction(),
				StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		constants = rule.getSpecificSubGoals();
		generalConds = rule.getGeneralisedConditions();
		assertTrue(constants.isEmpty());
		assertFalse(generalConds[0].isEmpty());
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("on"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("above"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("block"))));
		assertEquals(generalConds[0].size(), 3);
		assertTrue(generalConds[1].isEmpty());

		// Test anonymous variable
		rule = new RelationalRule("(clear ?X) (on ?X ?Y) => (moveFloor ?X)");
		// 5 assertions in the body: clear, on, two blocks, and an
		// inequals
		assertEquals(rule.getSimplifiedConditions(false).size(), 5);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?X ?Y)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?Y)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?Y ?X))")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertEquals(rule.getAction(),
				StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		constants = rule.getSpecificSubGoals();
		generalConds = rule.getGeneralisedConditions();
		assertTrue(constants.isEmpty());
		assertFalse(generalConds[0].isEmpty());
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("on"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("clear"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("block"))));
		assertEquals(generalConds[0].size(), 3);
		assertTrue(generalConds[1].isEmpty());

		// Test anonymous variables
		rule = new RelationalRule(
				"(clear ?X) (on ?X ?Z) (on ?Y ?A) => (moveFloor ?X)");
		// 10 assertions in the body: clear, 2 ons, 4 blocks, and 3
		// inequals
		// Note no inequals between ?1 and ?2
		assertEquals(rule.getSimplifiedConditions(false).size(), 10);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?X ?Y)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?Z ?A)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?Y)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?Z)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?A)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?A ?Z ?Y ?X))")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?Z ?Y ?X))")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?Y ?X))")));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertTrue(rule.getStringConditions().indexOf("block") > rule
				.getStringConditions().indexOf("test"));
		assertEquals(rule.getAction(),
				StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		constants = rule.getSpecificSubGoals();
		generalConds = rule.getGeneralisedConditions();
		assertTrue(constants.isEmpty());
		assertFalse(generalConds[0].isEmpty());
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("on"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("clear"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("block"))));
		assertEquals(generalConds[0].size(), 3);
		assertTrue(generalConds[1].isEmpty());

		// Test type predicate
		rule = new RelationalRule("(block ?X) => (moveFloor ?X)");
		assertEquals(rule.getSimplifiedConditions(false).size(), 1);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertEquals(rule.getAction(),
				StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		constants = rule.getSpecificSubGoals();
		generalConds = rule.getGeneralisedConditions();
		assertTrue(constants.isEmpty());
		assertFalse(generalConds[0].isEmpty());
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("block"))));
		assertEquals(generalConds[0].size(), 1);
		assertTrue(generalConds[1].isEmpty());

		// Test inequal type predicates
		rule = new RelationalRule("(block ?X) (block ?Y) => (move ?X ?Y)");
		assertEquals(rule.getSimplifiedConditions(false).size(), 3);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?Y)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?Y ?X))")));
		assertEquals(rule.getAction(),
				StateSpec.toRelationalPredicate("(move ?X ?Y)"));
		constants = rule.getSpecificSubGoals();
		generalConds = rule.getGeneralisedConditions();
		assertTrue(constants.isEmpty());
		assertTrue(constants.isEmpty());
		assertFalse(generalConds[0].isEmpty());
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("block"))));
		assertEquals(generalConds[0].size(), 1);
		assertTrue(generalConds[1].isEmpty());

		// Test existing type predicate
		rule = new RelationalRule("(clear ?X) (block ?X) => (moveFloor ?X)");
		assertEquals(rule.getSimplifiedConditions(false).size(), 2);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertEquals(rule.getAction(),
				StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		constants = rule.getSpecificSubGoals();
		generalConds = rule.getGeneralisedConditions();
		assertTrue(constants.isEmpty());
		assertFalse(generalConds[0].isEmpty());
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("clear"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("block"))));
		assertEquals(generalConds[0].size(), 2);
		assertTrue(generalConds[1].isEmpty());

		// Test inequals parsing
		rule = new RelationalRule(
				"(clear ?X) (clear ?Y) (test (<> ?Y ?X)) (block ?X) (block ?Y) => (move ?X ?Y)");
		assertEquals(rule.getSimplifiedConditions(false).size(), 5);
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?Y ?X))")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?Y)")));
		assertEquals(rule.getAction(),
				StateSpec.toRelationalPredicate("(move ?X ?Y)"));
		assertTrue(rule.getStringConditions().indexOf("clear") < rule
				.getStringConditions().indexOf("block"));
		assertTrue(rule.getStringConditions().indexOf("block") > rule
				.getStringConditions().indexOf("test"));
		constants = rule.getSpecificSubGoals();
		generalConds = rule.getGeneralisedConditions();
		assertTrue(constants.isEmpty());
		assertFalse(generalConds[0].isEmpty());
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("clear"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("block"))));
		assertEquals(generalConds[0].size(), 2);
		assertTrue(generalConds[1].isEmpty());

		// Testing module syntax
		rule = new RelationalRule(
				"(above ?X ?G_0) (clear ?X) => (moveFloor ?X)");
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?G_0)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(above ?X ?G_0)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?X ?G_0))")));
		assertEquals(rule.getAction(),
				StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		assertEquals(rule.getSimplifiedConditions(false).size(), 5);
		constants = rule.getSpecificSubGoals();
		generalConds = rule.getGeneralisedConditions();
		assertTrue(constants.isEmpty());
		assertFalse(generalConds[0].isEmpty());
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("clear"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("above"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("block"))));
		assertEquals(generalConds[0].size(), 3);
		assertTrue(generalConds[1].isEmpty());

		// Testing module constants
		rule = new RelationalRule(
				"(clear ?G_0) (on ?G_0 ?Y) => (moveFloor ?G_0)");
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?G_0)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(on ?G_0 ?Y)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?G_0)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?Y)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?Y ?G_0))")));
		assertEquals(rule.getAction(),
				StateSpec.toRelationalPredicate("(moveFloor ?G_0)"));
		assertEquals(rule.getSimplifiedConditions(false).size(), 5);
		constants = rule.getSpecificSubGoals();
		generalConds = rule.getGeneralisedConditions();
		strFact = StateSpec.getInstance().getPredicates().get("clear");
		assertFalse(constants.isEmpty());
		assertTrue(constants.contains(new SpecificGoalCondition(
				new RelationalPredicate(strFact, new String[] { "?G_0" }))));
		assertEquals(constants.size(), 1);
		assertFalse(generalConds[0].isEmpty());
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("clear"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("on"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("block"))));
		assertEquals(generalConds[0].size(), 3);
		assertTrue(generalConds[1].isEmpty());

		// Testing negation
		rule = new RelationalRule(
				"(clear ?X) (not (highest ?X)) => (moveFloor ?X)");
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(rule.getSimplifiedConditions(false).contains(
				StateSpec.toRelationalPredicate("(not (highest ?X))")));
		assertEquals(rule.getAction(),
				StateSpec.toRelationalPredicate("(moveFloor ?X)"));
		assertEquals(rule.getSimplifiedConditions(false).size(), 3);
		constants = rule.getSpecificSubGoals();
		generalConds = rule.getGeneralisedConditions();
		assertTrue(constants.isEmpty());
		assertFalse(generalConds[0].isEmpty());
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("clear"))));
		assertTrue(generalConds[0].contains(new GeneralGoalCondition(StateSpec
				.getInstance().getPredicateByName("block"))));
		assertEquals(generalConds[0].size(), 2);
		assertFalse(generalConds[1].isEmpty());
		RelationalPredicate negHighest = (StateSpec.getInstance()
				.getPredicateByName("highest"));
		negHighest.swapNegated();
		assertTrue(generalConds[1]
				.contains(new GeneralGoalCondition(negHighest)));
		assertEquals(generalConds[1].size(), 1);
	}

	@Test
	public void testToString() {
		assertEquals("StateSpec", spec_.toString());
	}

	@Test
	public void testInsertValidActions() throws Exception {
		Rete state = spec_.getRete();

		// Empty case
		MultiMap<String, String[]> validActions = spec_
				.generateValidActions(state);
		state.eval("(facts)");
		assertNotNull(validActions);
		assertNull(validActions.getSortedSet("move"));
		assertNull(validActions.getSortedSet("moveFloor"));
		state.reset();

		// Simple move case
		state.eval("(assert (clear a))");
		state.eval("(assert (clear b))");
		validActions = spec_.generateValidActions(state);
		state.eval("(facts)");
		assertNotNull(validActions);
		SortedSet<String[]> moveResult = validActions.getSortedSet("move");
		assertTrue(moveResult.contains(new String[] { "a", "b" }));
		assertTrue(moveResult.contains(new String[] { "b", "a" }));
		SortedSet<String[]> moveFloorResult = validActions
				.getSortedSet("moveFloor");
		assertNull(moveFloorResult);
		state.reset();

		// Simple moveFloor case
		state.eval("(assert (clear v))");
		state.eval("(assert (on v y))");
		validActions = spec_.generateValidActions(state);
		state.eval("(facts)");
		assertNotNull(validActions);
		moveResult = validActions.getSortedSet("move");
		assertNull(moveResult);
		moveFloorResult = validActions.getSortedSet("moveFloor");
		assertTrue(moveFloorResult.contains(new String[] { "v" }));
		state.reset();

		// Complex both case
		state.eval("(assert (clear d))");
		state.eval("(assert (clear e))");
		state.eval("(assert (clear c))");
		state.eval("(assert (on e b))");
		state.eval("(assert (on d a))");
		state.eval("(assert (onFloor c))");
		validActions = spec_.generateValidActions(state);
		state.eval("(facts)");
		assertNotNull(validActions);
		moveResult = validActions.getSortedSet("move");
		assertTrue(moveResult.contains(new String[] { "d", "e" }));
		assertTrue(moveResult.contains(new String[] { "d", "c" }));
		assertTrue(moveResult.contains(new String[] { "e", "d" }));
		assertTrue(moveResult.contains(new String[] { "e", "c" }));
		assertTrue(moveResult.contains(new String[] { "c", "d" }));
		assertTrue(moveResult.contains(new String[] { "c", "e" }));
		moveFloorResult = validActions.getSortedSet("moveFloor");
		assertTrue(moveFloorResult.contains(new String[] { "d" }));
		assertTrue(moveFloorResult.contains(new String[] { "e" }));
	}

	@Test
	public void testSplitFact() {
		// Basic
		String[] result = StateSpec.splitFact("(clear a)");
		assertArrayEquals(new String[] { "clear", "a" }, result);

		// More complex
		result = StateSpec.splitFact("(on a b)");
		assertArrayEquals(new String[] { "on", "a", "b" }, result);

		// No parentheses
		result = StateSpec.splitFact("clear a");
		assertArrayEquals(new String[] { "clear", "a" }, result);

		// Module declaration
		result = StateSpec.splitFact("(MAIN::clear a)");
		assertArrayEquals(new String[] { "clear", "a" }, result);

		// Inner condition
		result = StateSpec.splitFact("(clear ?X&:(<> ?X hat))");
		assertArrayEquals(new String[] { "clear", "?X&:(<> ?X hat)" }, result);

		// Test condition
		result = StateSpec.splitFact("(test (<> ?X hat))");
		assertArrayEquals(new String[] { "test", "(<> ?X hat)" }, result);

		// Not condition
		result = StateSpec.splitFact("(not (clear ?X))");
		assertArrayEquals(new String[] { "not", "(clear ?X)" }, result);

		// Add condition
		result = StateSpec.splitFact("(height ?X (+ ?N 1))");
		assertArrayEquals(new String[] { "height", "?X", "(+ ?N 1)" }, result);
	}
}
