package test;

import static org.junit.Assert.*;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.junit.Before;
import org.junit.Test;

import cerrla.RLGGMerger;
import cerrla.MergedFact;

public class UnificationTest {
	private RLGGMerger sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
		sut_ = RLGGMerger.getInstance();
	}

	@Test
	public void testUnifyStates() {
		// No change unification
		List<RelationalPredicate> oldState = new ArrayList<RelationalPredicate>();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		List<RelationalPredicate> newState = new ArrayList<RelationalPredicate>();
		newState.add(StateSpec.toRelationalPredicate("(clear x)"));
		BidiMap replacementMap = new DualHashBidiMap();
		List<MergedFact> result = sut_.unifyStates(oldState, newState,
				replacementMap);
		assertEquals(result.size(), 1);
		MergedFact lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultReplacements().size(), 1);
		assertEquals(lastFact.getResultFact(),
				StateSpec.toRelationalPredicate("(clear ?X)"));
		assertEquals(lastFact.getBaseFact(),
				StateSpec.toRelationalPredicate("(clear ?X)"));
		assertEquals(lastFact.getUnityFact(),
				StateSpec.toRelationalPredicate("(clear x)"));
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("x")), new RelationalArgument(
						"?X"));

		// No change with constants
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear a)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear a)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 1);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultReplacements().size(), 1);
		assertEquals(lastFact.getResultFact(),
				StateSpec.toRelationalPredicate("(clear a)"));
		assertEquals(lastFact.getBaseFact(),
				StateSpec.toRelationalPredicate("(clear a)"));
		assertEquals(lastFact.getUnityFact(),
				StateSpec.toRelationalPredicate("(clear a)"));
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("a")), new RelationalArgument(
						"a"));

		// Basic removal of preds unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(on ?X ?)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear ?)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on z x)"));
		newState.add(StateSpec.toRelationalPredicate("(highest a)"));
		newState.add(StateSpec.toRelationalPredicate("(clear x)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 1);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultReplacements().size(), 1);
		assertEquals(lastFact.getResultFact(),
				StateSpec.toRelationalPredicate("(clear ?X)"));
		assertEquals(lastFact.getBaseFact(),
				StateSpec.toRelationalPredicate("(clear ?X)"));
		assertEquals(lastFact.getUnityFact(),
				StateSpec.toRelationalPredicate("(clear x)"));
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("x")), new RelationalArgument(
						"?X"));

		// Simple unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		oldState.add(StateSpec.toRelationalPredicate("(on ?X ?Z)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear y)"));
		newState.add(StateSpec.toRelationalPredicate("(clear x)"));
		newState.add(StateSpec.toRelationalPredicate("(on y z)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 3);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultReplacements().size(), 3);
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("y")), new RelationalArgument(
						"?X"));
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("x")), new RelationalArgument(
						"?Y"));
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("z")), new RelationalArgument(
						"?Z"));

		// Negation unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(not (clear ?X))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(not (clear ?X))"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 1);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultReplacements().size(), 1);
		assertEquals(lastFact.getResultFact(),
				StateSpec.toRelationalPredicate("(not (clear ?X))"));
		assertEquals(lastFact.getBaseFact(),
				StateSpec.toRelationalPredicate("(not (clear ?X))"));
		assertEquals(lastFact.getUnityFact(),
				StateSpec.toRelationalPredicate("(not (clear ?X))"));
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?X")), new RelationalArgument(
						"?X"));

		// Substitution unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 1);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultReplacements().size(), 1);
		assertEquals(lastFact.getResultFact(),
				StateSpec.toRelationalPredicate("(clear ?X)"));
		assertEquals(lastFact.getBaseFact(),
				StateSpec.toRelationalPredicate("(clear ?X)"));
		assertEquals(lastFact.getUnityFact(),
				StateSpec.toRelationalPredicate("(clear ?Y)"));
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?Y")), new RelationalArgument(
						"?X"));

		// More complex substitution unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(highest ?X)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		newState.add(StateSpec.toRelationalPredicate("(highest ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 2);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultReplacements().size(), 1);
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?Y")), new RelationalArgument(
						"?X"));

		// Tricky complex substitution unification (could be either case)
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(highest ?Y)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		newState.add(StateSpec.toRelationalPredicate("(highest ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 1);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultReplacements().size(), 1);
		assertEquals(lastFact.getResultFact(),
				StateSpec.toRelationalPredicate("(clear ?X)"));
		assertEquals(lastFact.getBaseFact(),
				StateSpec.toRelationalPredicate("(clear ?X)"));
		assertEquals(lastFact.getUnityFact(),
				StateSpec.toRelationalPredicate("(clear ?Y)"));
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?Y")), new RelationalArgument(
						"?X"));

		// Unifying with a negated condition
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(block ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(block ?Y)"));
		oldState.add(StateSpec.toRelationalPredicate("(not (clear ?Y))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(block ?X)"));
		newState.add(StateSpec.toRelationalPredicate("(block ?Y)"));
		newState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		newState.add(StateSpec.toRelationalPredicate("(not (clear ?X))"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 4);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultReplacements().size(), 2);
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?X")), new RelationalArgument(
						"?Y"));
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?Y")), new RelationalArgument(
						"?X"));

		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(block ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		oldState.add(StateSpec.toRelationalPredicate("(not (highest ?X))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(block ?X)"));
		newState.add(StateSpec.toRelationalPredicate("(not (clear ?X))"));
		newState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 2);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultReplacements().size(), 2);
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?X")), new RelationalArgument(
						"?X"));
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?Y")), new RelationalArgument(
						"?Y"));

		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(block ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		oldState.add(StateSpec.toRelationalPredicate("(not (on ?X ?Y))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(block ?X)"));
		newState.add(StateSpec.toRelationalPredicate("(not (clear ?X))"));
		newState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 2);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultReplacements().size(), 2);
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?X")), new RelationalArgument(
						"?X"));
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?Y")), new RelationalArgument(
						"?Y"));

		// Problem with generalisation unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on ?X ?Y)"));
		oldState.add(StateSpec.toRelationalPredicate("(on ?X ?)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		newState.add(StateSpec.toRelationalPredicate("(on ?X ?)"));
		newState.add(StateSpec.toRelationalPredicate("(block ?X)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 1);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultFact(),
				StateSpec.toRelationalPredicate("(on ?X ?)"));
		assertEquals(lastFact.getBaseFact(),
				StateSpec.toRelationalPredicate("(on ?X ?)"));
		assertEquals(lastFact.getUnityFact(),
				StateSpec.toRelationalPredicate("(on ?X ?)"));
		assertEquals(lastFact.getResultReplacements().size(), 1);
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?X")), new RelationalArgument(
						"?X"));

		// Negated generalised unification (ILLEGAL)
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(not (above ?X ?))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(not (above ?X ?Y))"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertTrue(result.isEmpty());

		// Mirrored case
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(not (above ?X ?Y))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(not (above ?X ?))"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertTrue(result.isEmpty());

		// Same negation is fine
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(not (above ?X ?))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(not (above ?X ?))"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 1);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultFact(),
				StateSpec.toRelationalPredicate("(not (above ?X ?))"));
		assertEquals(lastFact.getBaseFact(),
				StateSpec.toRelationalPredicate("(not (above ?X ?))"));
		assertEquals(lastFact.getUnityFact(),
				StateSpec.toRelationalPredicate("(not (above ?X ?))"));
		assertEquals(lastFact.getResultReplacements().size(), 1);
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?X")), new RelationalArgument(
						"?X"));

		// Same negation term-swapped
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(not (above ?X ?))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(not (above ?Y ?))"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 1);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultFact(),
				StateSpec.toRelationalPredicate("(not (above ?X ?))"));
		assertEquals(lastFact.getBaseFact(),
				StateSpec.toRelationalPredicate("(not (above ?X ?))"));
		assertEquals(lastFact.getUnityFact(),
				StateSpec.toRelationalPredicate("(not (above ?Y ?))"));
		assertEquals(lastFact.getResultReplacements().size(), 1);
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?Y")), new RelationalArgument(
						"?X"));

		// Un-negated case is fine
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(above ?X ?Z)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(above ?X ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 1);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultFact(),
				StateSpec.toRelationalPredicate("(above ?X ?Z)"));
		assertEquals(lastFact.getBaseFact(),
				StateSpec.toRelationalPredicate("(above ?X ?Z)"));
		assertEquals(lastFact.getUnityFact(),
				StateSpec.toRelationalPredicate("(above ?X ?Y)"));
		assertEquals(lastFact.getResultReplacements().size(), 2);
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?X")), new RelationalArgument(
						"?X"));
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?Y")), new RelationalArgument(
						"?Z"));

		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(above ?X ?Y)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(above ?X ?Z)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 1);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultFact(),
				StateSpec.toRelationalPredicate("(above ?X ?Y)"));
		assertEquals(lastFact.getBaseFact(),
				StateSpec.toRelationalPredicate("(above ?X ?Y)"));
		assertEquals(lastFact.getUnityFact(),
				StateSpec.toRelationalPredicate("(above ?X ?Z)"));
		assertEquals(lastFact.getResultReplacements().size(), 2);
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?X")), new RelationalArgument(
						"?X"));
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?Z")), new RelationalArgument(
						"?Y"));

		// Unification order bug
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(highest ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		newState.add(StateSpec.toRelationalPredicate("(highest ?X)"));
		newState.add(StateSpec.toRelationalPredicate("(highest ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 2);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultReplacements().size(), 1);
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?X")), new RelationalArgument(
						"?X"));

		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(highest ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		newState.add(StateSpec.toRelationalPredicate("(highest ?X)"));
		newState.add(StateSpec.toRelationalPredicate("(highest ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 2);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultReplacements().size(), 1);
		assertEquals(
				lastFact.getResultReplacements().get(
						new RelationalArgument("?Y")), new RelationalArgument(
						"?X"));
	}

	@Test
	public void testRLGGUnification() {
		// Strict existing replacements
		List<RelationalPredicate> oldState = new ArrayList<RelationalPredicate>();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		List<RelationalPredicate> newState = new ArrayList<RelationalPredicate>();
		newState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		BidiMap replacementMap = new DualHashBidiMap();
		replacementMap.put(new RelationalArgument("?X"),
				new RelationalArgument("?X"));
		replacementMap.put(new RelationalArgument("?Y"),
				new RelationalArgument("?Y"));
		RelationalArgument[] terms = new RelationalArgument[0];
		int oldStateHash = oldState.hashCode();
		int newStateHash = newState.hashCode();
		int result = sut_.rlggUnification(oldState, newState, replacementMap,
				terms);
		assertEquals(oldState.toString(), RLGGMerger.CANNOT_UNIFY, result);
		assertEquals(oldStateHash, oldState.hashCode());
		assertEquals(newStateHash, newState.hashCode());

		// Breaking apart unbound variables
		oldState.clear();
		RelationalArgument[] arguments = new RelationalArgument[2];
		arguments[0] = new RelationalArgument("?X");
		arguments[1] = new RelationalArgument("?Z");
		arguments[1].setAsUnboundVariable();
		RelationalPredicate predA = new RelationalPredicate(StateSpec
				.getInstance().getPredicateByName("on"), arguments);
		oldState.add(predA);
		RelationalPredicate predB = new RelationalPredicate(StateSpec
				.getInstance().getPredicateByName("above"), arguments);
		oldState.add(predB);
		newState.clear();
		arguments = new RelationalArgument[2];
		arguments[0] = new RelationalArgument("?X");
		arguments[1] = new RelationalArgument("?Z");
		arguments[1].setAsUnboundVariable();
		RelationalPredicate predC = new RelationalPredicate(StateSpec
				.getInstance().getPredicateByName("on"), arguments);
		newState.add(predC);
		arguments = new RelationalArgument[2];
		arguments[0] = new RelationalArgument("?X");
		arguments[1] = new RelationalArgument("?A");
		arguments[1].setAsUnboundVariable();
		RelationalPredicate predD = new RelationalPredicate(StateSpec
				.getInstance().getPredicateByName("above"), arguments);
		newState.add(predD);

		replacementMap.clear();
		replacementMap.put(new RelationalArgument("?X"),
				new RelationalArgument("?X"));
		replacementMap.put(new RelationalArgument("?Y"),
				new RelationalArgument("?Y"));
		oldStateHash = oldState.hashCode();
		newStateHash = newState.hashCode();
		result = sut_
				.rlggUnification(oldState, newState, replacementMap, terms);
		assertEquals(RLGGMerger.UNIFIED_CHANGE, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(predA));
		assertTrue(newState.contains(predD));
		assertFalse(oldStateHash == oldState.hashCode());
		assertFalse(newStateHash == newState.hashCode());
		
		
		// Fact dropping test
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear a)"));
		oldState.add(StateSpec.toRelationalPredicate("(on a b)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear a)"));
		newState.add(StateSpec.toRelationalPredicate("(on b a)"));
		replacementMap.clear();
		result = sut_.rlggUnification(oldState, newState, replacementMap, new RelationalArgument[0]);
		assertTrue(result == 1);
		assertTrue(oldState.size() == 1);
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear a)")));
		assertTrue(newState.size() == 2);
		assertTrue(newState.contains(StateSpec.toRelationalPredicate("(on b a)")));
		assertTrue(newState.contains(StateSpec.toRelationalPredicate("(on a b)")));
	}

	@Test
	public void testUnifyNumerical() {
		StateSpec.initInstance("rlPacManGeneral.PacMan");
		sut_ = RLGGMerger.getInstance();

		// Basic unification
		List<RelationalPredicate> oldState = new ArrayList<RelationalPredicate>();
		oldState.add(StateSpec.toRelationalPredicate("(distance blinky 5)"));
		List<RelationalPredicate> newState = new ArrayList<RelationalPredicate>();
		newState.add(StateSpec.toRelationalPredicate("(distance blinky 5)"));
		BidiMap replacementMap = new DualHashBidiMap();
		List<MergedFact> result = sut_.unifyStates(oldState, newState,
				replacementMap);
		assertEquals(result.size(), 1);
		MergedFact lastFact = result.get(result.size() - 1);
		RelationalPredicate expected = StateSpec
				.toRelationalPredicate("(distance blinky "
						+ RelationalArgument.RANGE_VARIABLE_PREFIX
						+ "0&:(<= 5.0 "
						+ RelationalArgument.RANGE_VARIABLE_PREFIX + "0 5.0))");
		assertEquals(lastFact.getResultFact(), expected);

		// Numerical unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(distance blinky 10)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(distance pinky 5)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(result.size(), 1);
		lastFact = result.get(result.size() - 1);
		assertEquals(lastFact.getResultFact(),
				StateSpec
						.toRelationalPredicate("(distance blinky "
								+ RelationalArgument.RANGE_VARIABLE_PREFIX
								+ "1&:(<= 5.0 "
								+ RelationalArgument.RANGE_VARIABLE_PREFIX
								+ "1 10.0))"));

		// Unify a numeral to a range (as opposed to range to a numeral)
		RelationalPredicate numeralFact = StateSpec
				.toRelationalPredicate("(distance blinky 5");
		newState.clear();
		newState.add(StateSpec
				.toRelationalPredicate("(distance blinky ?#_4&:(<= 0.0 ?#_4 5.0))"));
		Collection<MergedFact> unified = RLGGMerger.getInstance()
				.unifyFactToState(numeralFact, newState, new DualHashBidiMap(),
						new RelationalArgument[0], false);
		assertEquals(unified.size(), 1);
		MergedFact unifact = unified.iterator().next();
		assertTrue(unifact
				.getResultFact()
				.equals(StateSpec
						.toRelationalPredicate("(distance blinky ?#_4&:(<= 0.0 ?#_4 5.0))")));
		assertTrue(unifact
				.getUnityFact()
				.equals(StateSpec
						.toRelationalPredicate("(distance blinky ?#_4&:(<= 0.0 ?#_4 5.0))")));
		assertTrue(unifact.getBaseFact().equals(
				StateSpec.toRelationalPredicate("(distance blinky 5)")));


		numeralFact = StateSpec.toRelationalPredicate("(distance blinky 10");
		newState.clear();
		newState.add(StateSpec
				.toRelationalPredicate("(distance blinky ?#_4&:(<= 0.0 ?#_4 5.0))"));
		unified = RLGGMerger.getInstance().unifyFactToState(numeralFact,
				newState, new DualHashBidiMap(), new RelationalArgument[0],
				false);
		assertEquals(unified.size(), 1);
		unifact = unified.iterator().next();
		assertTrue(unifact.getResultFact().equals(StateSpec
				.toRelationalPredicate(
				"(distance blinky ?#_4&:(<= 0.0 ?#_4 10.0))")));
		assertTrue(unifact.getUnityFact().equals(StateSpec
				.toRelationalPredicate(
				"(distance blinky ?#_4&:(<= 0.0 ?#_4 5.0))")));
		assertTrue(unifact.getBaseFact().equals(StateSpec
				.toRelationalPredicate("(distance blinky 10)")));
		
		numeralFact = StateSpec.toRelationalPredicate("(distance blinky 1");
		newState.clear();
		newState.add(StateSpec
				.toRelationalPredicate("(distance blinky ?#_4&:(<= 1.0 ?#_4 5.0))"));
		unified = RLGGMerger.getInstance().unifyFactToState(numeralFact,
				newState, new DualHashBidiMap(), new RelationalArgument[0],
				false);
		assertEquals(unified.size(), 1);
		unifact = unified.iterator().next();
		assertTrue(unifact.getResultFact().equals(StateSpec
				.toRelationalPredicate(
				"(distance blinky ?#_4&:(<= 1.0 ?#_4 5.0))")));
		assertTrue(unifact.getUnityFact().equals(StateSpec
				.toRelationalPredicate(
				"(distance blinky ?#_4&:(<= 1.0 ?#_4 5.0))")));
		assertTrue(unifact.getBaseFact().equals(StateSpec
				.toRelationalPredicate("(distance blinky 1)")));
	}
}
