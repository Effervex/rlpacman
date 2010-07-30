package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.Covering;
import relationalFramework.GuidedRule;
import relationalFramework.StateSpec;

public class PacManCoveringTest {
	private Covering sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("rlPacMan.PacMan");
		sut_ = new Covering(StateSpec.getInstance().getActions().size());
	}

	@Test
	public void testSpecialiseToPreGoal() {
		// Specialising a range without a pregoal (splitting an LGG rule)
		sut_.clearPreGoalState(StateSpec.getInstance().getActions().size());

		GuidedRule rule = new GuidedRule(
				"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 0.0 36.0)) "
						+ "(ghost ?X) (pacman player) => (toGhost ?X ?__Num0)");
		rule.expandConditions();
		Collection<GuidedRule> results = sut_.specialiseToPreGoal(rule);
		assertEquals(Covering.NUM_DISCRETE_RANGES, results.size());
		double interval = 36 / Covering.NUM_DISCRETE_RANGES;
		for (int i = 0; i < Covering.NUM_DISCRETE_RANGES; i++) {
			assertTrue(results
					.contains(new GuidedRule(
							"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 "
									+ (interval * i)
									+ " "
									+ (interval * (i + 1))
									+ ") (ghost ?X) (pacman player) => (toGhost ?X ?__Num0)",
							false, true, null)));
		}

		// Specialising a range without pregoal, but rule is mutant (failure)
		rule = new GuidedRule(
				"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 9.0 18.0)) "
						+ "(ghost ?X) (pacman player) => (toGhost ?X ?__Num0)");
		rule.setMutant(true);
		rule.expandConditions();
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(0, results.size());

		// Specialising a range without a pregoal, but rule is a mutant with
		// same range as covered rule.
		rule = new GuidedRule(
				"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 0.0 36.0)) "
						+ "(edible ?X) (ghost ?X) (pacman player) => "
						+ "(toGhost ?X ?__Num0)");
		rule.setMutant(true);
		rule.expandConditions();
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
									+ ") (edible ?X) (ghost ?X) (pacman player) => (toGhost ?X ?__Num0)",
							false, true, null)));
		}

		// Specialising a range with a single numerical pregoal
		double[] points = { 0, 4, 9, 10, 30, 36 };

		List<String> pregoal = new ArrayList<String>();
		for (double point : points) {
			pregoal.clear();
			pregoal.add("(dot ?X)");
			pregoal.add("(pacman player)");
			pregoal.add("(distanceDot player ?X " + point + ")");
			sut_.setPreGoal("(toDot ?X " + point + ")", pregoal);

			rule = new GuidedRule(
					"(distanceDot player ?X ?__Num3&:(betweenRange ?__Num3 0.0 36.0)) "
							+ "(dot ?X) (pacman player) => (toDot ?X ?__Num3)");
			rule.expandConditions();
			results = sut_.specialiseToPreGoal(rule);
			assertTrue(results.size() >= Covering.NUM_DISCRETE_RANGES + 1);
			interval = 36 / Covering.NUM_DISCRETE_RANGES;
			int beforeIntervals = (int) Math.ceil(point / interval);
			double beforeInterval = 1.0 * point / beforeIntervals;
			int afterIntervals = (int) Math.ceil((36 - point) / interval);
			double afterInterval = 1.0 * (36 - point) / afterIntervals;
			// Before interval(s)
			for (int i = 0; i < beforeIntervals; i++)
				assertTrue(results
						.contains(new GuidedRule(
								"(distanceDot player ?X ?__Num3&:(betweenRange ?__Num3 "
										+ (beforeInterval * i)
										+ " "
										+ (beforeInterval * (i + 1))
										+ ") (dot ?X) (pacman player) => (toDot ?X ?__Num3)",
								false, true, null)));

			// The point itself
			assertTrue(results.contains(new GuidedRule(
					"(distanceDot player ?X " + point
							+ ") (dot ?X) (pacman player) => (toDot ?X "
							+ point + ")", false, true, null)));

			// After interval(s)
			for (int i = 0; i < afterIntervals; i++)
				assertTrue(results
						.contains(new GuidedRule(
								"(distanceDot player ?X ?__Num3&:(betweenRange ?__Num3 "
										+ (point + afterInterval * i)
										+ " "
										+ (point + afterInterval * (i + 1))
										+ ") (dot ?X) (pacman player) => (toDot ?X ?__Num3)",
								false, true, null)));
		}

		// Specialising a range to a ranged pre-goal
		for (int p = 1; p < points.length; p++) {
			double startPoint = points[p - 1];
			double endPoint = points[p];

			pregoal.clear();
			pregoal.add("(dot ?X)");
			pregoal.add("(pacman player)");
			pregoal
					.add("(distanceDot player ?X ?__Num3&:(betweenRange ?__Num3 "
							+ startPoint + " " + endPoint + "))");
			sut_.setPreGoal("(toDot ?X ?__Num3)", pregoal);

			rule = new GuidedRule(
					"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 0.0 36.0)) "
							+ "(dot ?X) (pacman player) => (toDot ?X ?__Num0)");
			rule.expandConditions();
			results = sut_.specialiseToPreGoal(rule);
			assertTrue(results.size() >= Covering.NUM_DISCRETE_RANGES + 1);
			interval = 36 / Covering.NUM_DISCRETE_RANGES;
			int beforeIntervals = (int) Math.ceil(startPoint / interval);
			double beforeInterval = 1.0 * startPoint / beforeIntervals;
			int midIntervals = (int) Math.ceil((endPoint - startPoint)
					/ interval);
			double midInterval = 1.0 * (endPoint - startPoint) / midIntervals;
			int afterIntervals = (int) Math.ceil((36 - endPoint) / interval);
			double afterInterval = 1.0 * (36 - endPoint) / afterIntervals;
			// Before interval(s)
			for (int i = 0; i < beforeIntervals; i++)
				assertTrue(results
						.contains(new GuidedRule(
								"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 "
										+ (beforeInterval * i)
										+ " "
										+ (beforeInterval * (i + 1))
										+ ") (dot ?X) (pacman player) => (toDot ?X ?__Num0)",
								false, true, null)));

			// Mid interval(s)
			for (int i = 0; i < midIntervals; i++)
				assertTrue(results
						.contains(new GuidedRule(
								"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 "
										+ (startPoint + midInterval * i)
										+ " "
										+ (startPoint + midInterval * (i + 1))
										+ ") (dot ?X) (pacman player) => (toDot ?X ?__Num0)",
								false, true, null)));

			// After interval(s)
			for (int i = 0; i < afterIntervals; i++)
				assertTrue(results
						.contains(new GuidedRule(
								"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 "
										+ (endPoint + afterInterval * i)
										+ " "
										+ (endPoint + afterInterval * (i + 1))
										+ ") (dot ?X) (pacman player) => (toDot ?X ?__Num0)",
								false, true, null)));
		}

		// Special case: Range goes through 0 (no pregoal)
		sut_.clearPreGoalState(StateSpec.getInstance().getActions().size());

		rule = new GuidedRule(
				"(junctionSafety ?X ?__Num0&:(betweenRange ?__Num0 -16.0 26.0)) "
						+ "(junction ?X) => (toJunction ?X ?__Num0)");
		rule.expandConditions();
		results = sut_.specialiseToPreGoal(rule);
		assertTrue(results.size() >= Covering.NUM_DISCRETE_RANGES + 1);
		interval = 42 / Covering.NUM_DISCRETE_RANGES;
		int beforeIntervals = (int) Math.ceil(16 / interval);
		double beforeInterval = 1.0 * 16 / beforeIntervals;
		int afterIntervals = (int) Math.ceil(26 / interval);
		double afterInterval = 1.0 * 26 / afterIntervals;
		// Before interval(s)
		for (int i = 0; i < beforeIntervals; i++)
			assertTrue(results
					.contains(new GuidedRule(
							"(junctionSafety ?X ?__Num0&:(betweenRange ?__Num0 "
									+ (-16 + beforeInterval * i)
									+ " "
									+ (-16 + beforeInterval * (i + 1))
									+ ") (junction ?X) => (toJunction ?X ?__Num0)",
							false, true, null)));

		// After interval(s)
		for (int i = 0; i < afterIntervals; i++)
			assertTrue(results
					.contains(new GuidedRule(
							"(junctionSafety ?X ?__Num0&:(betweenRange ?__Num0 "
									+ (afterInterval * i)
									+ " "
									+ (afterInterval * (i + 1))
									+ ") (junction ?X) => (toJunction ?X ?__Num0)",
							false, true, null)));
	}
}
