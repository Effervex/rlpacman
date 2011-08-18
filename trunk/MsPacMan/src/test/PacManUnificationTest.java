package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import cerrla.Unification;


public class PacManUnificationTest {
	private Unification sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("rlPacMan.PacMan");
		sut_ = Unification.getInstance();
	}

	@Test
	public void testNumericalUnifyStates() {
		// No change unification
		List<RelationalPredicate> oldState = new ArrayList<RelationalPredicate>();
		oldState.add(StateSpec.toRelationalPredicate("(distanceDot a b 1)"));
		List<RelationalPredicate> newState = new ArrayList<RelationalPredicate>();
		newState.add(StateSpec.toRelationalPredicate("(distanceDot a b 1)"));
		String[] oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "1";
		String[] newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "1";
		int result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(distanceDot a b 1)")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "1");

		// Range addition
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(distanceDot a b 1)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(distanceDot a b 2)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "1";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "2";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		int index = 0;
		assertTrue(oldState
				.contains(StateSpec
						.toRelationalPredicate("(distanceDot a b ?"
								+ Unification.RANGE_VARIABLE_PREFIX + index
								+ "&:(" + StateSpec.BETWEEN_RANGE + " ?"
								+ Unification.RANGE_VARIABLE_PREFIX + index
								+ " 1.0 2.0))")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?" + Unification.RANGE_VARIABLE_PREFIX + index);

		// Range addition (reversed)
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(distanceDot a b 2)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(distanceDot a b 1)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "2";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "1";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		index++;
		assertTrue(oldState
				.contains(StateSpec
						.toRelationalPredicate("(distanceDot a b ?"
								+ Unification.RANGE_VARIABLE_PREFIX + index
								+ "&:(" + StateSpec.BETWEEN_RANGE + " ?"
								+ Unification.RANGE_VARIABLE_PREFIX + index
								+ " 1.0 2.0))")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?" + Unification.RANGE_VARIABLE_PREFIX + index);

		// Negative range addition
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(distanceDot a b -1)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(distanceDot a b 2)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "-1";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "2";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		index++;
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(distanceDot a b ?"
						+ Unification.RANGE_VARIABLE_PREFIX + index + "&:("
						+ StateSpec.BETWEEN_RANGE + " ?"
						+ Unification.RANGE_VARIABLE_PREFIX + index
						+ " -1.0 2.0))")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?" + Unification.RANGE_VARIABLE_PREFIX + index);

		// Tiny value range addition
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(distanceDot a b -1)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(distanceDot a b 2.567483E-64)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "-1";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "2.567483E-64";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		index++;
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(distanceDot a b ?"
						+ Unification.RANGE_VARIABLE_PREFIX + index + "&:("
						+ StateSpec.BETWEEN_RANGE + " ?"
						+ Unification.RANGE_VARIABLE_PREFIX + index
						+ " -1.0 2.567483E-64))")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?" + Unification.RANGE_VARIABLE_PREFIX + index);

		// Regular variable unification with numerical values too
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(distanceDot x y -1)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(distanceDot a b 2)"));
		oldTerms = new String[2];
		oldTerms[0] = "x";
		oldTerms[1] = "-1";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "2";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		index++;
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(distanceDot ?X ? ?"
						+ Unification.RANGE_VARIABLE_PREFIX + index + "&:("
						+ StateSpec.BETWEEN_RANGE + " ?"
						+ Unification.RANGE_VARIABLE_PREFIX + index
						+ " -1.0 2.0))")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?" + Unification.RANGE_VARIABLE_PREFIX + index);

		// Unification under an existing range term
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(distanceDot a b ?"
				+ Unification.RANGE_VARIABLE_PREFIX + "0&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Unification.RANGE_VARIABLE_PREFIX + "0 1.0 3.0))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(distanceDot a b 2)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "?" + Unification.RANGE_VARIABLE_PREFIX + "0";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "2";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(distanceDot a b ?"
						+ Unification.RANGE_VARIABLE_PREFIX + "0&:("
						+ StateSpec.BETWEEN_RANGE + " ?"
						+ Unification.RANGE_VARIABLE_PREFIX + "0 1.0 3.0))")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?" + Unification.RANGE_VARIABLE_PREFIX + "0");

		// Unification under an existing range term (extension)
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(distanceDot a b ?"
				+ Unification.RANGE_VARIABLE_PREFIX + "0&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Unification.RANGE_VARIABLE_PREFIX + "0 1.0 3.0))"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(distanceDot a b -2)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "?" + Unification.RANGE_VARIABLE_PREFIX + "0";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "-2";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(distanceDot a b ?"
						+ Unification.RANGE_VARIABLE_PREFIX + "0&:("
						+ StateSpec.BETWEEN_RANGE + " ?"
						+ Unification.RANGE_VARIABLE_PREFIX + "0 -2.0 3.0))")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?" + Unification.RANGE_VARIABLE_PREFIX + "0");

		// Multiple numerical terms
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(distanceDot a b 1)"));
		oldState.add(StateSpec.toRelationalPredicate("(level a 1)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(distanceDot a b -2)"));
		newState.add(StateSpec.toRelationalPredicate("(level a 3)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "1";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "-2";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		index++;
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(distanceDot a b ?"
						+ Unification.RANGE_VARIABLE_PREFIX + index + "&:("
						+ StateSpec.BETWEEN_RANGE + " ?"
						+ Unification.RANGE_VARIABLE_PREFIX + index
						+ " -2.0 1.0))")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?" + Unification.RANGE_VARIABLE_PREFIX + index);
		index++;
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(level a ?"
				+ Unification.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Unification.RANGE_VARIABLE_PREFIX + index + " 1.0 3.0))")));

		// Multiple numerical terms with existing range term
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(distanceDot a b ?"
				+ Unification.RANGE_VARIABLE_PREFIX + "0&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Unification.RANGE_VARIABLE_PREFIX + "0 1.0 3.0))"));
		oldState.add(StateSpec.toRelationalPredicate("(level a 1)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(distanceDot a b -2)"));
		newState.add(StateSpec.toRelationalPredicate("(level a 3)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "?" + Unification.RANGE_VARIABLE_PREFIX + "0";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "-2";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec
				.toRelationalPredicate("(distanceDot a b ?"
						+ Unification.RANGE_VARIABLE_PREFIX + "0&:("
						+ StateSpec.BETWEEN_RANGE + " ?"
						+ Unification.RANGE_VARIABLE_PREFIX + "0 -2.0 3.0))")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?" + Unification.RANGE_VARIABLE_PREFIX + "0");
		index++;
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(level a ?"
				+ Unification.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Unification.RANGE_VARIABLE_PREFIX + index + " 1.0 3.0))")));

		// Variables and numerical unification (differing distance)
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(distanceDot ? ?X 1)"));
		oldState.add(StateSpec.toRelationalPredicate("(dot ?X)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(distanceGhost player blinky 2)"));
		newState.add(StateSpec.toRelationalPredicate("(ghost blinky)"));
		newState.add(StateSpec.toRelationalPredicate("(distanceDot player dot_1 5)"));
		newState.add(StateSpec.toRelationalPredicate("(dot dot_1)"));
		oldTerms = new String[2];
		oldTerms[0] = "?X";
		oldTerms[1] = "1";
		newTerms = new String[2];
		newTerms[0] = "dot_1";
		newTerms[1] = "5";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		index++;
		assertTrue(oldState
				.contains(StateSpec
						.toRelationalPredicate("(distanceDot ? ?X ?"
								+ Unification.RANGE_VARIABLE_PREFIX + index
								+ "&:(" + StateSpec.BETWEEN_RANGE + " ?"
								+ Unification.RANGE_VARIABLE_PREFIX + index
								+ " 1.0 5.0))")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(dot ?X)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?" + Unification.RANGE_VARIABLE_PREFIX + index);

		// Variables and numerical unification (out-of-range distance)
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(distanceDot ? ?X 1)"));
		oldState.add(StateSpec.toRelationalPredicate("(dot ?X)"));
		newState.clear();
		newState
				.add(StateSpec.toRelationalPredicate("(distanceGhost player blinky 10)"));
		newState.add(StateSpec.toRelationalPredicate("(ghost blinky)"));
		newState.add(StateSpec.toRelationalPredicate("(distanceDot player dot_1 5)"));
		newState.add(StateSpec.toRelationalPredicate("(dot dot_1)"));
		oldTerms = new String[2];
		oldTerms[0] = "?X";
		oldTerms[1] = "1";
		newTerms = new String[2];
		newTerms[0] = "dot_1";
		newTerms[1] = "5";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		index++;
		assertTrue(oldState
				.contains(StateSpec
						.toRelationalPredicate("(distanceDot ? ?X ?"
								+ Unification.RANGE_VARIABLE_PREFIX + index
								+ "&:(" + StateSpec.BETWEEN_RANGE + " ?"
								+ Unification.RANGE_VARIABLE_PREFIX + index
								+ " 1.0 5.0))")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(dot ?X)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?" + Unification.RANGE_VARIABLE_PREFIX + index);

		// Variables and numerical unification (same distance)
		oldState.clear();
		oldState.add(StateSpec.toRelationalPredicate("(distanceDot ? ?X 1)"));
		oldState.add(StateSpec.toRelationalPredicate("(dot ?X)"));
		newState.clear();
		newState.add(StateSpec.toRelationalPredicate("(distanceGhost player blinky 1)"));
		newState.add(StateSpec.toRelationalPredicate("(ghost blinky)"));
		newState.add(StateSpec.toRelationalPredicate("(distanceDot player dot_1 5)"));
		newState.add(StateSpec.toRelationalPredicate("(dot dot_1)"));
		oldTerms = new String[2];
		oldTerms[0] = "?X";
		oldTerms[1] = "1";
		newTerms = new String[2];
		newTerms[0] = "dot_1";
		newTerms[1] = "5";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		index++;
		assertTrue(oldState
				.contains(StateSpec
						.toRelationalPredicate("(distanceDot ? ?X ?"
								+ Unification.RANGE_VARIABLE_PREFIX + index
								+ "&:(" + StateSpec.BETWEEN_RANGE + " ?"
								+ Unification.RANGE_VARIABLE_PREFIX + index
								+ " 1.0 5.0))")));
		assertTrue(oldState.contains(StateSpec.toRelationalPredicate("(dot ?X)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?" + Unification.RANGE_VARIABLE_PREFIX + index);
	}

}
