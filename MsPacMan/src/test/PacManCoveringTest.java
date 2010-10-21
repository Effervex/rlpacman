package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.Covering;
import relationalFramework.GuidedRule;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

public class PacManCoveringTest {
	private Covering sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("rlPacMan.PacMan");
		sut_ = new Covering();
	}

	@Test
	public void testSpecialiseToPreGoal() {
		// Specialising a range without a pregoal (splitting an LGG rule)
		sut_.clearPreGoalState();

		GuidedRule rule = new GuidedRule(
				"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 0.0 36.0)) "
						+ "(ghost ?X) (pacman player) => (toGhost ?X ?__Num0)");
		Collection<GuidedRule> results = sut_.specialiseToPreGoal(rule);
		HashSet<GuidedRule> hs = new HashSet<GuidedRule>();
		for (GuidedRule ruley : results) {
			int hashCode = ruley.hashCode();
			hs.add(ruley);
		}
		assertEquals(results, hs);

		assertEquals(Covering.NUM_DISCRETE_RANGES, results.size());
		double interval = 36 / Covering.NUM_DISCRETE_RANGES;
		for (int i = 0; i < Covering.NUM_DISCRETE_RANGES; i++) {
			GuidedRule mutant = new GuidedRule(
					"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 "
							+ (interval * i)
							+ " "
							+ (interval * (i + 1))
							+ ")) (ghost ?X) (pacman player) => (toGhost ?X ?__Num0)",
					false, true, null);
			int hashCode = mutant.hashCode();
			assertTrue(results.contains(mutant));
		}

		// Specialising a range without pregoal, but rule is mutant (failure)
		rule = new GuidedRule(
				"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 9.0 18.0)) "
						+ "(ghost ?X) (pacman player) => (toGhost ?X ?__Num0)");
		rule.setMutant(true);
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(0, results.size());

		// Specialising a range without a pregoal, but rule is a mutant with
		// same range as covered rule.
		rule = new GuidedRule(
				"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 0.0 36.0)) "
						+ "(edible ?X) (ghost ?X) (pacman player) => "
						+ "(toGhost ?X ?__Num0)");
		rule.setMutant(true);
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(Covering.NUM_DISCRETE_RANGES, results.size());
		interval = 36 / Covering.NUM_DISCRETE_RANGES;
		for (int i = 0; i < Covering.NUM_DISCRETE_RANGES; i++) {
			assertTrue(results
					.contains(new GuidedRule(
							"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 "
									+ (interval * i)
									+ " "
									+ (interval * (i + 1))
									+ ")) (edible ?X) (ghost ?X) (pacman player) => (toGhost ?X ?__Num0)",
							false, true, null)));
		}

		// Specialising a range with a single numerical pregoal
		double[] points = { 0, 4, 9, 10, 30, 36 };

		List<StringFact> pregoal = new ArrayList<StringFact>();
		for (double point : points) {
			pregoal.clear();
			pregoal.add(StateSpec.toStringFact("(dot ?X)"));
			pregoal.add(StateSpec.toStringFact("(pacman player)"));
			pregoal.add(StateSpec.toStringFact("(distanceDot player ?X "
					+ point + ")"));
			sut_.setPreGoal(StateSpec.toStringFact("(toDot ?X " + point + ")"),
					pregoal);

			rule = new GuidedRule(
					"(distanceDot player ?X ?__Num3&:(betweenRange ?__Num3 0.0 36.0)) "
							+ "(dot ?X) (pacman player) => (toDot ?X ?__Num3)");
			results = sut_.specialiseToPreGoal(rule);
			assertEquals(Covering.NUM_DISCRETE_RANGES + 1, results.size());
			interval = 36 / Covering.NUM_DISCRETE_RANGES;
			// The regular intervals
			for (int i = 0; i < Covering.NUM_DISCRETE_RANGES; i++) {
				assertTrue(results
						.contains(new GuidedRule(
								"(distanceDot player ?X ?__Num3&:(betweenRange ?__Num3 "
										+ (interval * i)
										+ " "
										+ (interval * (i + 1))
										+ ")) (dot ?X) (pacman player) => (toDot ?X ?__Num3)",
								false, true, null)));
			}

			// The point itself
			assertTrue(results.contains(new GuidedRule(
					"(distanceDot player ?X " + point
							+ ") (dot ?X) (pacman player) => (toDot ?X "
							+ point + ")", false, true, null)));
		}

		// Specialising a range to a ranged pre-goal
		for (int p = 1; p < points.length; p++) {
			double startPoint = points[p - 1];
			double endPoint = points[p];

			pregoal.clear();
			pregoal.add(StateSpec.toStringFact("(dot ?X)"));
			pregoal.add(StateSpec.toStringFact("(pacman player)"));
			pregoal
					.add(StateSpec
							.toStringFact("(distanceDot player ?X ?__Num3&:(betweenRange ?__Num3 "
									+ startPoint + " " + endPoint + "))"));
			sut_.setPreGoal(StateSpec.toStringFact("(toDot ?X ?__Num3)"),
					pregoal);

			rule = new GuidedRule(
					"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 0.0 36.0)) "
							+ "(dot ?X) (pacman player) => (toDot ?X ?__Num0)");
			results = sut_.specialiseToPreGoal(rule);
			assertTrue(results.size() >= Covering.NUM_DISCRETE_RANGES + 1);
			interval = 36 / Covering.NUM_DISCRETE_RANGES;
			int midIntervals = (int) Math.ceil((endPoint - startPoint)
					/ interval);
			double midInterval = 1.0 * (endPoint - startPoint) / midIntervals;
			// The regular intervals
			for (int i = 0; i < Covering.NUM_DISCRETE_RANGES; i++) {
				assertTrue(results
						.contains(new GuidedRule(
								"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 "
										+ (interval * i)
										+ " "
										+ (interval * (i + 1))
										+ ")) (dot ?X) (pacman player) => (toDot ?X ?__Num0)",
								false, true, null)));
			}

			// Pre-goal intervals
			for (int i = 0; i < midIntervals; i++)
				assertTrue(results
						.contains(new GuidedRule(
								"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 "
										+ (startPoint + midInterval * i)
										+ " "
										+ (startPoint + midInterval * (i + 1))
										+ ")) (dot ?X) (pacman player) => (toDot ?X ?__Num0)",
								false, true, null)));

			// Pre-goal range
			assertTrue(results
					.contains(new GuidedRule(
							"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 "
									+ startPoint
									+ " "
									+ endPoint
									+ ")) (dot ?X) (pacman player) => (toDot ?X ?__Num0)",
							false, true, null)));
		}

		// Special case: Range goes through 0 (no pregoal)
		sut_.clearPreGoalState();

		rule = new GuidedRule(
				"(junctionSafety ?X ?__Num0&:(betweenRange ?__Num0 -16.0 26.0)) "
						+ "(junction ?X) => (toJunction ?X ?__Num0)");
		results = sut_.specialiseToPreGoal(rule);
		assertTrue(results.size() >= Covering.NUM_DISCRETE_RANGES + 1);
		interval = 42 / Covering.NUM_DISCRETE_RANGES;
		int beforeIntervals = (int) Math.ceil(16 / interval);
		double beforeInterval = 1.0 * 16 / beforeIntervals;
		int afterIntervals = (int) Math.ceil(26 / interval);
		double afterInterval = 1.0 * 26 / afterIntervals;
		// Before interval(s)
		for (int i = 0; i < beforeIntervals; i++)
			assertTrue(results.contains(new GuidedRule(
					"(junctionSafety ?X ?__Num0&:(betweenRange ?__Num0 "
							+ (-16 + beforeInterval * i) + " "
							+ (-16 + beforeInterval * (i + 1))
							+ ")) (junction ?X) => (toJunction ?X ?__Num0)",
					false, true, null)));

		// After interval(s)
		for (int i = 0; i < afterIntervals; i++)
			assertTrue(results.contains(new GuidedRule(
					"(junctionSafety ?X ?__Num0&:(betweenRange ?__Num0 "
							+ (afterInterval * i) + " "
							+ (afterInterval * (i + 1))
							+ ")) (junction ?X) => (toJunction ?X ?__Num0)",
					false, true, null)));
	}

	@Test
	public void testNumericalUnifyStates() {
		// No change unification
		List<StringFact> oldState = new ArrayList<StringFact>();
		oldState.add(StateSpec.toStringFact("(distanceDot a b 1)"));
		List<StringFact> newState = new ArrayList<StringFact>();
		newState.add(StateSpec.toStringFact("(distanceDot a b 1)"));
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
				.toStringFact("(distanceDot a b 1)")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "1");

		// Range addition
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(distanceDot a b 1)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(distanceDot a b 2)"));
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
						.toStringFact("(distanceDot a b ?"
								+ Covering.RANGE_VARIABLE_PREFIX + index
								+ "&:(" + StateSpec.BETWEEN_RANGE + " ?"
								+ Covering.RANGE_VARIABLE_PREFIX + index
								+ " 1.0 2.0))")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?" + Covering.RANGE_VARIABLE_PREFIX + index);

		// Range addition (reversed)
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(distanceDot a b 2)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(distanceDot a b 1)"));
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
		assertTrue(oldState.contains(StateSpec.toStringFact("(distanceDot a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " 1.0 2.0))")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?" + Covering.RANGE_VARIABLE_PREFIX + index);

		// Negative range addition
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(distanceDot a b -1)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(distanceDot a b 2)"));
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
		assertTrue(oldState.contains(StateSpec.toStringFact("(distanceDot a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " -1.0 2.0))")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?" + Covering.RANGE_VARIABLE_PREFIX + index);

		// Tiny value range addition
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(distanceDot a b -1)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(distanceDot a b 2.567483E-64)"));
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
		assertTrue(oldState.contains(StateSpec.toStringFact("(distanceDot a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index
				+ " -1.0 2.567483E-64))")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?" + Covering.RANGE_VARIABLE_PREFIX + index);

		// Regular variable unification with numerical values too
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(distanceDot x y -1)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(distanceDot a b 2)"));
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
		assertTrue(oldState.contains(StateSpec.toStringFact("(distanceDot ?X ? ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " -1.0 2.0))")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?" + Covering.RANGE_VARIABLE_PREFIX + index);

		// Unification under an existing range term
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(distanceDot a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0 1.0 3.0))"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(distanceDot a b 2)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "?" + Covering.RANGE_VARIABLE_PREFIX + "0";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "2";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(distanceDot a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0 1.0 3.0))")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?" + Covering.RANGE_VARIABLE_PREFIX + "0");

		// Unification under an existing range term (extension)
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(distanceDot a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0 1.0 3.0))"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(distanceDot a b -2)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "?" + Covering.RANGE_VARIABLE_PREFIX + "0";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "-2";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(distanceDot a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0 -2.0 3.0))")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?" + Covering.RANGE_VARIABLE_PREFIX + "0");

		// Multiple numerical terms
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(distanceDot a b 1)"));
		oldState.add(StateSpec.toStringFact("(level a 1)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(distanceDot a b -2)"));
		newState.add(StateSpec.toStringFact("(level a 3)"));
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
		assertTrue(oldState.contains(StateSpec.toStringFact("(distanceDot a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " -2.0 1.0))")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?" + Covering.RANGE_VARIABLE_PREFIX + index);
		index++;
		assertTrue(oldState.contains(StateSpec.toStringFact("(level a ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " 1.0 3.0))")));

		// Multiple numerical terms with existing range term
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(distanceDot a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0 1.0 3.0))"));
		oldState.add(StateSpec.toStringFact("(level a 1)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(distanceDot a b -2)"));
		newState.add(StateSpec.toStringFact("(level a 3)"));
		oldTerms = new String[2];
		oldTerms[0] = "a";
		oldTerms[1] = "?" + Covering.RANGE_VARIABLE_PREFIX + "0";
		newTerms = new String[2];
		newTerms[0] = "a";
		newTerms[1] = "-2";
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains(StateSpec.toStringFact("(distanceDot a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0 -2.0 3.0))")));
		assertEquals(oldTerms[0], "a");
		assertEquals(oldTerms[1], "?" + Covering.RANGE_VARIABLE_PREFIX + "0");
		index++;
		assertTrue(oldState.contains(StateSpec.toStringFact("(level a ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " 1.0 3.0))")));

		// Variables and numerical unification (differing distance)
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(distanceDot ? ?X 1)"));
		oldState.add(StateSpec.toStringFact("(dot ?X)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(distanceGhost player blinky 2)"));
		newState.add(StateSpec.toStringFact("(ghost blinky)"));
		newState.add(StateSpec.toStringFact("(distanceDot player dot_1 5)"));
		newState.add(StateSpec.toStringFact("(dot dot_1)"));
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
		assertTrue(oldState.contains(StateSpec.toStringFact("(distanceDot ? ?X ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " 1.0 5.0))")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(dot ?X)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?" + Covering.RANGE_VARIABLE_PREFIX + index);

		// Variables and numerical unification (out-of-range distance)
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(distanceDot ? ?X 1)"));
		oldState.add(StateSpec.toStringFact("(dot ?X)"));
		newState.clear();
		newState
				.add(StateSpec.toStringFact("(distanceGhost player blinky 10)"));
		newState.add(StateSpec.toStringFact("(ghost blinky)"));
		newState.add(StateSpec.toStringFact("(distanceDot player dot_1 5)"));
		newState.add(StateSpec.toStringFact("(dot dot_1)"));
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
		assertTrue(oldState.contains(StateSpec.toStringFact("(distanceDot ? ?X ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " 1.0 5.0))")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(dot ?X)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?" + Covering.RANGE_VARIABLE_PREFIX + index);

		// Variables and numerical unification (same distance)
		oldState.clear();
		oldState.add(StateSpec.toStringFact("(distanceDot ? ?X 1)"));
		oldState.add(StateSpec.toStringFact("(dot ?X)"));
		newState.clear();
		newState.add(StateSpec.toStringFact("(distanceGhost player blinky 1)"));
		newState.add(StateSpec.toStringFact("(ghost blinky)"));
		newState.add(StateSpec.toStringFact("(distanceDot player dot_1 5)"));
		newState.add(StateSpec.toStringFact("(dot dot_1)"));
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
		assertTrue(oldState.contains(StateSpec.toStringFact("(distanceDot ? ?X ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " 1.0 5.0))")));
		assertTrue(oldState.contains(StateSpec.toStringFact("(dot ?X)")));
		assertEquals(oldTerms[0], "?X");
		assertEquals(oldTerms[1], "?" + Covering.RANGE_VARIABLE_PREFIX + index);
	}
}
