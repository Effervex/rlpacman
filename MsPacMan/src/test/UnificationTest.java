package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.junit.Before;
import org.junit.Test;

import cerrla.Unification;

import relationalFramework.StateSpec;
import relationalFramework.RelationalPredicate;

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
		String[] oldTerms = new String[1];
		oldTerms[0] = "?X";
		String[] newTerms = new String[1];
		newTerms[0] = "x";
		int result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?X)")));
		assertEquals(oldTerms[0], "?X");

		// No change with constants
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear a)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear a)"));
		oldTerms = new String[1];
		oldTerms[0] = "a";
		newTerms = new String[1];
		newTerms[0] = "a";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear a)")));
		assertEquals(oldTerms[0], "a");

		// Basic removal of preds unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(on ?X ?)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear ?)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on z x)"));
		newState.add(StateSpec.toRelationalPredicate("(highest a)"));
		newState.add(StateSpec.toRelationalPredicate("(clear x)"));
		oldTerms = new String[1];
		oldTerms[0] = "?X";
		newTerms = new String[1];
		newTerms[0] = "x";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?X)")));
		assertEquals(oldTerms[0], "?X");

		// Simple unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		oldState.add(StateSpec.toRelationalPredicate("(on ?X ?)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear y)"));
		newState.add(StateSpec.toRelationalPredicate("(clear x)"));
		oldState.add(StateSpec.toRelationalPredicate("(on y z)"));
		oldTerms = new String[2];
		oldTerms[0] = "?X";
		oldTerms[1] = "?Y";
		newTerms = new String[2];
		newTerms[0] = "x";
		newTerms[1] = "y";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?Y");

		// Absorption
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear a)"));
		oldTerms = new String[1];
		oldTerms[0] = "?X";
		newTerms = new String[1];
		newTerms[0] = "a";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?X)")));
		assertEquals(oldTerms[0], "?X");

		// Generalisation
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear a)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear x)"));
		oldTerms = new String[1];
		oldTerms[0] = "a";
		newTerms = new String[1];
		newTerms[0] = "x";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?X)")));
		assertEquals(oldTerms[0], "?X");

		// Mutual generalisation
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear a)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear b)"));
		oldTerms = new String[1];
		oldTerms[0] = "a";
		newTerms = new String[1];
		newTerms[0] = "b";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?X)")));
		assertEquals(oldTerms[0], "?X");

		// Two terms
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on a b)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on b a)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "b";
		newTerms = new String[2];
		newTerms[0] = "b";
		newTerms[1] = "a";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on ?X ?Y)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?Y");

		// Two terms in differing order
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on a b)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on a b)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "b";
		newTerms = new String[2];
		newTerms[0] = "b";
		newTerms[1] = "a";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(-1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on a b)")));

		// Two terms with two aligned preds
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on a b)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear a)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on b a)"));
		newState.add(StateSpec.toRelationalPredicate("(clear b)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "b";
		newTerms = new String[2];
		newTerms[0] = "b";
		newTerms[1] = "a";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on ?X ?Y)")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?X)")));
		assertEquals(oldTerms[0], "?X");

		// Two terms with two misaligned preds
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on a b)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear a)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on b a)"));
		newState.add(StateSpec.toRelationalPredicate("(clear a)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "b";
		newTerms = new String[2];
		newTerms[0] = "b";
		newTerms[1] = "a";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on ?X ?Y)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?Y");

		// Generalisation to anonymous
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear z)"));
		oldTerms = new String[1];
		oldTerms[0] = "?X";
		newTerms = new String[1];
		newTerms[0] = "x";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(-1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?X)")));

		// Constant and variable case
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on a ?X)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on b x)"));
		oldTerms = new String[1];
		oldTerms[0] = "?X";
		newTerms = new String[1];
		newTerms[0] = "x";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on ? ?X)")));
		assertEquals(oldTerms[0], "?X");

		// Tough case
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on a ?Y)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on z y)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "?Y";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "y";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on ? ?Y)")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?Y");

		// Tough case 2
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on ?X a)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on z y)"));
		oldTerms = new String[2];
		oldTerms[0] = "?X";
		oldTerms[1] = "a";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "y";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on ? ?Y)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?Y");

		// Tough case 3
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on a ?Y)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on a z)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "?Y";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "y";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on a ?)")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?Y");

		// Early generalisation test
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on a ?Y)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on a z)"));
		newState.add(StateSpec.toRelationalPredicate("(on a y)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "?Y";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "y";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on a ?Y)")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?Y");

		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on a ?Y)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on x y)"));
		newState.add(StateSpec.toRelationalPredicate("(on a y)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "?Y";
		newTerms = new String[2];
		newTerms[0] = "x";
		newTerms[1] = "y";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on ?X ?Y)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?Y");

		// Using the same fact for unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on a ?Y)"));
		oldState.add(StateSpec.toRelationalPredicate("(on ?X ?Y)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on x y)"));
		oldTerms = new String[2];
		oldTerms[0] = "?X";
		oldTerms[1] = "?Y";
		newTerms = new String[2];
		newTerms[0] = "x";
		newTerms[1] = "y";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on ? ?Y)")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on ?X ?Y)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?Y");

		// Left with constant predicate
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on ?X b)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on y b)"));
		newState.add(StateSpec.toRelationalPredicate("(clear x)"));
		newState.add(StateSpec.toRelationalPredicate("(clear y)"));
		oldTerms = new String[2];
		oldTerms[0] = "?X";
		oldTerms[1] = "?Y";
		newTerms = new String[2];
		newTerms[0] = "x";
		newTerms[1] = "y";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(3, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on ? b)")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?Y");

		// Un-unifiable
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on a b)"));
		oldTerms = new String[1];
		oldTerms[0] = "?X";
		newTerms = new String[1];
		newTerms[0] = "a";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(-1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?X)")));

		// Interesting case
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on a c)"));
		oldState.add(StateSpec.toRelationalPredicate("(on c ?)"));
		oldState.add(StateSpec.toRelationalPredicate("(on ?X d)"));
		oldState.add(StateSpec.toRelationalPredicate("(onFloor e)"));
		oldState.add(StateSpec.toRelationalPredicate("(onFloor d)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear a)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(highest a)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on b c)"));
		newState.add(StateSpec.toRelationalPredicate("(on c f)"));
		newState.add(StateSpec.toRelationalPredicate("(on a e)"));
		newState.add(StateSpec.toRelationalPredicate("(onFloor d)"));
		newState.add(StateSpec.toRelationalPredicate("(onFloor f)"));
		newState.add(StateSpec.toRelationalPredicate("(onFloor e)"));
		newState.add(StateSpec.toRelationalPredicate("(clear d)"));
		newState.add(StateSpec.toRelationalPredicate("(clear b)"));
		newState.add(StateSpec.toRelationalPredicate("(clear a)"));
		newState.add(StateSpec.toRelationalPredicate("(highest b)"));
		oldTerms = new String[2];
		oldTerms[0] = "?X";
		oldTerms[1] = "a";
		newTerms = new String[2];
		newTerms[0] = "d";
		newTerms[1] = "b";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(6, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on ?Y c)")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on c ?)")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(onFloor e)")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?Y)")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(highest ?Y)")));

		// Action precedence
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on a c)"));
		oldState.add(StateSpec.toRelationalPredicate("(on c ?)"));
		oldState.add(StateSpec.toRelationalPredicate("(on b ?)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on b c)"));
		newState.add(StateSpec.toRelationalPredicate("(on c f)"));
		newState.add(StateSpec.toRelationalPredicate("(on a e)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "b";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "b";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(3, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on a ?)")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on c ?)")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on b ?)")));

		// Double unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on c e)"));
		oldState.add(StateSpec.toRelationalPredicate("(on f g)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on c g)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "b";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "b";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on c ?)")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on ? g)")));

		// Double unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(on c g)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(on c e)"));
		newState.add(StateSpec.toRelationalPredicate("(on f g)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "b";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "b";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on ? g)")));

		// Unifying with an inequality test present
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		oldState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		oldState.add(StateSpec.toRelationalPredicate("(test (<> ?X ?Y))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear a)"));
		newState.add(StateSpec.toRelationalPredicate("(clear b)"));
		oldTerms = new String[2];
		oldTerms[0] = "?X";
		oldTerms[1] = "?Y";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "b";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?Y)")));
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
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(replacementMap.containsKey("?X"));
		assertEquals(replacementMap.get("?X"), "?X");

		// Negation unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(not (clear ?X))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(not (clear ?X))"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState
				.contains(StateSpec.toRelationalPredicate("(not (clear ?X))")));
		assertTrue(replacementMap.containsKey("?X"));
		assertEquals(replacementMap.get("?X"), "?X");

		// Substitution unification
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(clear ?X)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(clear ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(replacementMap.containsKey("?Y"));
		assertEquals(replacementMap.get("?Y"), "?X");

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
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(highest ?X)")));
		assertTrue(replacementMap.containsKey("?Y"));
		assertEquals(replacementMap.get("?Y"), "?X");

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
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?X)")));
		assertTrue(replacementMap.containsKey("?Y"));
		assertEquals(replacementMap.get("?Y"), "?X");

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
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(block ?X)")));

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
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?Y)")));

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
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(block ?X)")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(clear ?Y)")));

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
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(on ?X ?)")));
		
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
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(above ?X ?)")));
		
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
