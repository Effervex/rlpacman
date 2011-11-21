package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.junit.Before;
import org.junit.Test;

import cerrla.Unification;

public class UnificationTest {
	private Unification sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
		sut_ = Unification.getInstance();
	}

	@Test
	public void testUnifyStates() {
		// No change unification
		List<RelationalPredicate> oldState = new ArrayList<RelationalPredicate>();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		List<RelationalPredicate> newState = new ArrayList<RelationalPredicate>();
		newState.add(StateSpec.toRelationalPredicate("(clear x)"));
		BidiMap replacementMap = new DualHashBidiMap();
		int result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(clear ?X)")));
		assertEquals(replacementMap.size(), 1);
		assertEquals(replacementMap.get(new RelationalArgument("x")),
				new RelationalArgument("?X"));

		// No change with constants
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear a)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear a)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(clear a)")));
		assertEquals(replacementMap.size(), 1);
		assertEquals(replacementMap.get(new RelationalArgument("a")),
				new RelationalArgument("a"));

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
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(clear ?X)")));
		assertEquals(replacementMap.size(), 1);
		assertEquals(replacementMap.get(new RelationalArgument("x")),
				new RelationalArgument("?X"));

		// Simple unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		oldState.add(StateSpec.toRelationalPredicate("(on ?X ?)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear y)"));
		newState.add(StateSpec.toRelationalPredicate("(clear x)"));
		newState.add(StateSpec.toRelationalPredicate("(on y z)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(3, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(clear ?X)")));
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(clear ?Y)")));
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(on ?X ?)")));
		assertEquals(replacementMap.size(), 2);
		assertEquals(replacementMap.get(new RelationalArgument("y")),
				new RelationalArgument("?X"));
		assertEquals(replacementMap.get(new RelationalArgument("x")),
				new RelationalArgument("?Y"));

		// Absorption
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear a)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(clear ?X)")));
		assertEquals(replacementMap.size(), 1);
		assertEquals(replacementMap.get(new RelationalArgument("a")),
				new RelationalArgument("?X"));

		// Two terms in differing order
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on a b)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on a b)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(on a b)")));
	}

	@Test
	public void testUnifyNumerical() {
		StateSpec.initInstance("rlPacManGeneral.PacMan");
		sut_ = Unification.getInstance();

		// Basic unification
		List<RelationalPredicate> oldState = new ArrayList<RelationalPredicate>();
		oldState.add(StateSpec.toRelationalPredicate("(distance blinky 5)"));
		List<RelationalPredicate> newState = new ArrayList<RelationalPredicate>();
		newState.add(StateSpec.toRelationalPredicate("(distance blinky 5)"));
		BidiMap replacementMap = new DualHashBidiMap();
		int result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(distance blinky 5)")));

		// Numerical unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(distance blinky 10)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(distance pinky 5)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(1, result);
		assertTrue(oldState
				.contains(StateSpec
						.toRelationalPredicate("(distance blinky ?__Num0&:(<= 5.0 ?__Num0 10.0)")));
	}

	@Test
	public void testTermlessUnifyStates() {
		// Basic unification
		List<RelationalPredicate> oldState = new ArrayList<RelationalPredicate>();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		List<RelationalPredicate> newState = new ArrayList<RelationalPredicate>();
		newState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		BidiMap replacementMap = new DualHashBidiMap();
		int result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(clear ?X)")));
		assertEquals(replacementMap.get(new RelationalArgument("?X")), new RelationalArgument("?X"));

		// Negation unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(not (clear ?X))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(not (clear ?X))"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(not (clear ?X))")));
		assertEquals(replacementMap.get(new RelationalArgument("?X")), new RelationalArgument("?X"));

		// Substitution unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(clear ?X)")));
		assertEquals(replacementMap.get(new RelationalArgument("?Y")), new RelationalArgument("?X"));

		// More complex substitution unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(highest ?X)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		newState.add(StateSpec.toRelationalPredicate("(highest ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(clear ?X)")));
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(highest ?X)")));
		assertEquals(replacementMap.get(new RelationalArgument("?Y")), new RelationalArgument("?X"));

		// Tricky complex substitution unification (could be either case)
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(highest ?Y)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		newState.add(StateSpec.toRelationalPredicate("(highest ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(clear ?X)")));
		assertEquals(replacementMap.get(new RelationalArgument("?Y")), new RelationalArgument("?X"));

		// Unifying with a negated condition
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(block ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(not (clear ?Y))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(block ?X)"));
		newState.add(StateSpec.toRelationalPredicate("(not (clear ?X))"));
		newState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(block ?X)")));

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
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(block ?X)")));
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(clear ?Y)")));

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
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(block ?X)")));
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(clear ?Y)")));

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
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(on ?X ?)")));

		// Negated generalised unification (ILLEGAL)
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(not (above ?X ?))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(not (above ?X ?Y))"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(-1, result);

		// Mirrored case
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(not (above ?X ?Y))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(not (above ?X ?))"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(-1, result);

		// Same negation is fine
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(not (above ?X ?))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(not (above ?X ?))"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);

		// Same negation term-swapped
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(not (above ?X ?))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(not (above ?Y ?))"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);

		// Un-negated case is fine
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(above ?X ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);

		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(above ?X ?Y)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(above ?X ?)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(1, result);
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(above ?X ?)")));

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
		assertEquals(0, result);

		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(highest ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		newState.add(StateSpec.toRelationalPredicate("(highest ?X)"));
		newState.add(StateSpec.toRelationalPredicate("(highest ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
	}
}
