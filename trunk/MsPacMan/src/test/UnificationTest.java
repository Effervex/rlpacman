package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.junit.Before;
import org.junit.Test;

import relationalFramework.StateSpec;
import relationalFramework.StringFact;
import relationalFramework.Unification;

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
		List<StringFact> oldState = new ArrayList<StringFact>();
		oldState.add(StateSpec.toStringFact("(clear ?X)"));
		List<StringFact> newState = new ArrayList<StringFact>();
		newState.add(StateSpec.toStringFact("(clear x)"));
		String[] oldTerms = new String[1];
		oldTerms[0] = "?X";
		String[] newTerms = new String[1];
		newTerms[0] = "x";
		int result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?X)")));
		assertEquals(oldTerms[0], "?X");

		// No change with constants
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(clear a)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(clear a)"));
		oldTerms = new String[1];
		oldTerms[0] = "a";
		newTerms = new String[1];
		newTerms[0] = "a";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear a)")));
		assertEquals(oldTerms[0], "a");

		// Basic removal of preds unification
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(clear ?X)"));
		oldState.add(StateSpec.toStringFact("(on ?X ?)"));
		oldState.add(StateSpec.toStringFact("(clear ?)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on z x)"));
		newState.add(StateSpec.toStringFact("(highest a)"));
		newState.add(StateSpec.toStringFact("(clear x)"));
		oldTerms = new String[1];
		oldTerms[0] = "?X";
		newTerms = new String[1];
		newTerms[0] = "x";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?X)")));
		assertEquals(oldTerms[0], "?X");

		// Simple unification
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(clear ?X)"));
		oldState.add(StateSpec.toStringFact("(clear ?Y)"));
		oldState.add(StateSpec.toStringFact("(on ?X ?)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(clear y)"));
		newState.add(StateSpec.toStringFact("(clear x)"));
		oldState.add(StateSpec.toStringFact("(on y z)"));
		oldTerms = new String[2];
		oldTerms[0] = "?X";
		oldTerms[1] = "?Y";
		newTerms = new String[2];
		newTerms[0] = "x";
		newTerms[1] = "y";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?Y)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?Y");

		// Absorption
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(clear ?X)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(clear a)"));
		oldTerms = new String[1];
		oldTerms[0] = "?X";
		newTerms = new String[1];
		newTerms[0] = "a";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?X)")));
		assertEquals(oldTerms[0], "?X");

		// Generalisation
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(clear a)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(clear x)"));
		oldTerms = new String[1];
		oldTerms[0] = "a";
		newTerms = new String[1];
		newTerms[0] = "x";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?X)")));
		assertEquals(oldTerms[0], "?X");

		// Mutual generalisation
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(clear a)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(clear b)"));
		oldTerms = new String[1];
		oldTerms[0] = "a";
		newTerms = new String[1];
		newTerms[0] = "b";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?X)")));
		assertEquals(oldTerms[0], "?X");

		// Two terms
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(on a b)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on b a)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "b";
		newTerms = new String[2];
		newTerms[0] = "b";
		newTerms[1] = "a";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(on ?X ?Y)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?Y");

		// Two terms in differing order
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(on a b)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on a b)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "b";
		newTerms = new String[2];
		newTerms[0] = "b";
		newTerms[1] = "a";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(-1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(on a b)")));

		// Two terms with two aligned preds
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(on a b)"));
		oldState.add(StateSpec.toStringFact("(clear a)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on b a)"));
		newState.add(StateSpec.toStringFact("(clear b)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "b";
		newTerms = new String[2];
		newTerms[0] = "b";
		newTerms[1] = "a";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(on ?X ?Y)")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?X)")));
		assertEquals(oldTerms[0], "?X");

		// Two terms with two misaligned preds
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(on a b)"));
		oldState.add(StateSpec.toStringFact("(clear a)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on b a)"));
		newState.add(StateSpec.toStringFact("(clear a)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "b";
		newTerms = new String[2];
		newTerms[0] = "b";
		newTerms[1] = "a";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(on ?X ?Y)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?Y");

		// Generalisation to anonymous
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(clear ?X)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(clear z)"));
		oldTerms = new String[1];
		oldTerms[0] = "?X";
		newTerms = new String[1];
		newTerms[0] = "x";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(-1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?X)")));

		// Constant and variable case
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(on a ?X)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on b x)"));
		oldTerms = new String[1];
		oldTerms[0] = "?X";
		newTerms = new String[1];
		newTerms[0] = "x";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(on ? ?X)")));
		assertEquals(oldTerms[0], "?X");

		// Tough case
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(on a ?Y)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on z y)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "?Y";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "y";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(on ? ?Y)")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?Y");

		// Tough case 2
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(on ?X a)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on z y)"));
		oldTerms = new String[2];
		oldTerms[0] = "?X";
		oldTerms[1] = "a";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "y";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(on ? ?Y)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?Y");

		// Tough case 3
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(on a ?Y)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on a z)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "?Y";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "y";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(on a ?)")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?Y");

		// Early generalisation test
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(on a ?Y)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on a z)"));
		newState.add(StateSpec.toStringFact("(on a y)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "?Y";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "y";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(on a ?Y)")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?Y");

		oldState.clear();
		oldState.add(StateSpec.toStringFact("(on a ?Y)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on x y)"));
		newState.add(StateSpec.toStringFact("(on a y)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "?Y";
		newTerms = new String[2];
		newTerms[0] = "x";
		newTerms[1] = "y";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(on ?X ?Y)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?Y");

		// Using the same fact for unification
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(on a ?Y)"));
		oldState.add(StateSpec.toStringFact("(on ?X ?Y)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on x y)"));
		oldTerms = new String[2];
		oldTerms[0] = "?X";
		oldTerms[1] = "?Y";
		newTerms = new String[2];
		newTerms[0] = "x";
		newTerms[1] = "y";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(on ? ?Y)")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(on ?X ?Y)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?Y");

		// Left with constant predicate
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(on ?X b)"));
		oldState.add(StateSpec.toStringFact("(clear ?X)"));
		oldState.add(StateSpec.toStringFact("(clear ?Y)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on y b)"));
		newState.add(StateSpec.toStringFact("(clear x)"));
		newState.add(StateSpec.toStringFact("(clear y)"));
		oldTerms = new String[2];
		oldTerms[0] = "?X";
		oldTerms[1] = "?Y";
		newTerms = new String[2];
		newTerms[0] = "x";
		newTerms[1] = "y";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(3, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(on ? b)")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?Y)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?Y");

		// Un-unifiable
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(clear ?X)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on a b)"));
		oldTerms = new String[1];
		oldTerms[0] = "?X";
		newTerms = new String[1];
		newTerms[0] = "a";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(-1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?X)")));

		// Interesting case
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(on a c)"));
		oldState.add(StateSpec.toStringFact("(on c ?)"));
		oldState.add(StateSpec.toStringFact("(on ?X d)"));
		oldState.add(StateSpec.toStringFact("(onFloor e)"));
		oldState.add(StateSpec.toStringFact("(onFloor d)"));
		oldState.add(StateSpec.toStringFact("(clear a)"));
		oldState.add(StateSpec.toStringFact("(clear ?X)"));
		oldState.add(StateSpec.toStringFact("(highest a)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on b c)"));
		newState.add(StateSpec.toStringFact("(on c f)"));
		newState.add(StateSpec.toStringFact("(on a e)"));
		newState.add(StateSpec.toStringFact("(onFloor d)"));
		newState.add(StateSpec.toStringFact("(onFloor f)"));
		newState.add(StateSpec.toStringFact("(onFloor e)"));
		newState.add(StateSpec.toStringFact("(clear d)"));
		newState.add(StateSpec.toStringFact("(clear b)"));
		newState.add(StateSpec.toStringFact("(clear a)"));
		newState.add(StateSpec.toStringFact("(highest b)"));
		oldTerms = new String[2];
		oldTerms[0] = "?X";
		oldTerms[1] = "a";
		newTerms = new String[2];
		newTerms[0] = "d";
		newTerms[1] = "b";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(6, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(on ?Y c)")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(on c ?)")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(onFloor e)")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?Y)")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(highest ?Y)")));

		// Action precedence
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(on a c)"));
		oldState.add(StateSpec.toStringFact("(on c ?)"));
		oldState.add(StateSpec.toStringFact("(on b ?)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on b c)"));
		newState.add(StateSpec.toStringFact("(on c f)"));
		newState.add(StateSpec.toStringFact("(on a e)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "b";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "b";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(3, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(on a ?)")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(on c ?)")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(on b ?)")));

		// Double unification
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(on c e)"));
		oldState.add(StateSpec.toStringFact("(on f g)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on c g)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "b";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "b";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(on c ?)")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(on ? g)")));

		// Double unification
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(on c g)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(on c e)"));
		newState.add(StateSpec.toStringFact("(on f g)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "b";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "b";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(on ? g)")));

		// Unifying with an inequality test present
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(clear ?X)"));
		oldState.add(StateSpec.toStringFact("(clear ?Y)"));
		oldState.add(StateSpec.toStringFact("(test (<> ?X ?Y))"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(clear a)"));
		newState.add(StateSpec.toStringFact("(clear b)"));
		oldTerms = new String[2];
		oldTerms[0] = "?X";
		oldTerms[1] = "?Y";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "b";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?Y)")));

		// Modular unification
//		oldState.clear();
//		oldState.add(StateSpec.toStringFact("(clear ?X)"));
//		oldState.add(StateSpec.toStringFact("(clear ?Y)"));
//		newState.clear();
//		newState.add(StateSpec.toStringFact("(clear ?_MOD_a)"));
//		newState.add(StateSpec.toStringFact("(clear b)"));
//		oldTerms = new String[2];
//		oldTerms[0] = "?X";
//		oldTerms[1] = "?Y";
//		newTerms = new String[2];
//		newTerms[0] = "?_MOD_a";
//		newTerms[1] = "b";
//		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
//		assertEquals(0, result);
//		assertEquals(2, oldState.size());
//		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?X)")));
//		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?Y)")));
	}

	@Test
	public void testTermlessUnifyStates() {
		// Basic unification
		List<StringFact> oldState = new ArrayList<StringFact>();
		oldState.add(StateSpec.toStringFact("(clear ?X)"));
		List<StringFact> newState = new ArrayList<StringFact>();
		newState.add(StateSpec.toStringFact("(clear ?X)"));
		BidiMap replacementMap = new DualHashBidiMap();
		int result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(replacementMap.containsKey("?X"));
		assertEquals(replacementMap.get("?X"), "?X");

		// Negation unification
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(not (clear ?X))"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(not (clear ?X))"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState
				.contains(StateSpec.toStringFact("(not (clear ?X))")));
		assertTrue(replacementMap.containsKey("?X"));
		assertEquals(replacementMap.get("?X"), "?X");

		// Substitution unification
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(clear ?X)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(clear ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(replacementMap.containsKey("?Y"));
		assertEquals(replacementMap.get("?Y"), "?X");

		// More complex substitution unification
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(clear ?X)"));
		oldState.add(StateSpec.toStringFact("(highest ?X)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(clear ?Y)"));
		newState.add(StateSpec.toStringFact("(highest ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(highest ?X)")));
		assertTrue(replacementMap.containsKey("?Y"));
		assertEquals(replacementMap.get("?Y"), "?X");

		// Tricky complex substitution unification (could be either case)
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(clear ?X)"));
		oldState.add(StateSpec.toStringFact("(highest ?Y)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(clear ?Y)"));
		newState.add(StateSpec.toStringFact("(highest ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(replacementMap.containsKey("?Y"));
		assertEquals(replacementMap.get("?Y"), "?X");

		// Unifying with a negated condition
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(block ?X)"));
		oldState.add(StateSpec.toStringFact("(clear ?X)"));
		oldState.add(StateSpec.toStringFact("(not (clear ?Y))"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(block ?X)"));
		newState.add(StateSpec.toStringFact("(not (clear ?X))"));
		newState.add(StateSpec.toStringFact("(clear ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(block ?X)")));

		oldState.clear();
		oldState.add(StateSpec.toStringFact("(block ?X)"));
		oldState.add(StateSpec.toStringFact("(clear ?Y)"));
		oldState.add(StateSpec.toStringFact("(not (highest ?X))"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(block ?X)"));
		newState.add(StateSpec.toStringFact("(not (clear ?X))"));
		newState.add(StateSpec.toStringFact("(clear ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(block ?X)")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?Y)")));

		oldState.clear();
		oldState.add(StateSpec.toStringFact("(block ?X)"));
		oldState.add(StateSpec.toStringFact("(clear ?Y)"));
		oldState.add(StateSpec.toStringFact("(not (on ?X ?Y))"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(block ?X)"));
		newState.add(StateSpec.toStringFact("(not (clear ?X))"));
		newState.add(StateSpec.toStringFact("(clear ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(block ?X)")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(clear ?Y)")));

		// Problem with generalisation unification
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(on ?X ?Y)"));
		oldState.add(StateSpec.toStringFact("(on ?X ?)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(clear ?X)"));
		newState.add(StateSpec.toStringFact("(on ?X ?)"));
		newState.add(StateSpec.toStringFact("(block ?X)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(on ?X ?)")));
		
		// Negated generalised unification (ILLEGAL)
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(not (above ?X ?))"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(not (above ?X ?Y))"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(-1, result);
		
		// Mirrored case
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(not (above ?X ?Y))"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(not (above ?X ?))"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(-1, result);
		
		// Same negation is fine
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(not (above ?X ?))"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(not (above ?X ?))"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		
		// Same negation term-swapped
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(not (above ?X ?))"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(not (above ?Y ?))"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		
		// Un-negated case is fine
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(above ?X ?)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(above ?X ?Y)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(above ?X ?Y)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(above ?X ?)"));
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(1, result);
		assertTrue(oldState.contains(StateSpec.toStringFact("(above ?X ?)")));
	}

}
