package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.RuleCreation;
import relationalFramework.GuidedRule;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

public class PacManRuleCreationTest {
	private RuleCreation sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("rlPacMan.PacMan");
		sut_ = new RuleCreation();
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

		assertEquals(RuleCreation.NUM_DISCRETE_RANGES, results.size());
		double interval = 36 / RuleCreation.NUM_DISCRETE_RANGES;
		for (int i = 0; i < RuleCreation.NUM_DISCRETE_RANGES; i++) {
			GuidedRule mutant = new GuidedRule(
					"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 "
							+ (interval * i)
							+ " "
							+ (interval * (i + 1))
							+ ")) (ghost ?X) (pacman player) => (toGhost ?X ?__Num0)",
					rule);
			int hashCode = mutant.hashCode();
			assertTrue(results.contains(mutant));
		}

		// Specialising a range without pregoal, but rule is mutant (failure)
		rule = new GuidedRule(
				"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 9.0 18.0)) "
						+ "(ghost ?X) (pacman player) => (toGhost ?X ?__Num0)");
		rule.setMutant(rule);
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(0, results.size());

		// Specialising a range without a pregoal, but rule is a mutant with
		// same range as covered rule.
		rule = new GuidedRule(
				"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 0.0 36.0)) "
						+ "(edible ?X) (ghost ?X) (pacman player) => "
						+ "(toGhost ?X ?__Num0)");
		rule.setMutant(rule);
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(RuleCreation.NUM_DISCRETE_RANGES, results.size());
		interval = 36 / RuleCreation.NUM_DISCRETE_RANGES;
		for (int i = 0; i < RuleCreation.NUM_DISCRETE_RANGES; i++) {
			assertTrue(results
					.contains(new GuidedRule(
							"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 "
									+ (interval * i)
									+ " "
									+ (interval * (i + 1))
									+ ")) (edible ?X) (ghost ?X) (pacman player) => (toGhost ?X ?__Num0)",
							rule)));
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
			assertEquals(RuleCreation.NUM_DISCRETE_RANGES + 1, results.size());
			interval = 36 / RuleCreation.NUM_DISCRETE_RANGES;
			// The regular intervals
			for (int i = 0; i < RuleCreation.NUM_DISCRETE_RANGES; i++) {
				assertTrue(results
						.contains(new GuidedRule(
								"(distanceDot player ?X ?__Num3&:(betweenRange ?__Num3 "
										+ (interval * i)
										+ " "
										+ (interval * (i + 1))
										+ ")) (dot ?X) (pacman player) => (toDot ?X ?__Num3)",
								rule)));
			}

			// The point itself
			assertTrue(results.contains(new GuidedRule(
					"(distanceDot player ?X " + point
							+ ") (dot ?X) (pacman player) => (toDot ?X "
							+ point + ")", rule)));
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
			assertTrue(results.size() >= RuleCreation.NUM_DISCRETE_RANGES + 1);
			interval = 36 / RuleCreation.NUM_DISCRETE_RANGES;
			int midIntervals = (int) Math.ceil((endPoint - startPoint)
					/ interval);
			double midInterval = 1.0 * (endPoint - startPoint) / midIntervals;
			// The regular intervals
			for (int i = 0; i < RuleCreation.NUM_DISCRETE_RANGES; i++) {
				assertTrue(results
						.contains(new GuidedRule(
								"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 "
										+ (interval * i)
										+ " "
										+ (interval * (i + 1))
										+ ")) (dot ?X) (pacman player) => (toDot ?X ?__Num0)",
								rule)));
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
								rule)));

			// Pre-goal range
			assertTrue(results
					.contains(new GuidedRule(
							"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 "
									+ startPoint
									+ " "
									+ endPoint
									+ ")) (dot ?X) (pacman player) => (toDot ?X ?__Num0)",
							rule)));
		}

		// Special case: Range goes through 0 (no pregoal)
		sut_.clearPreGoalState();

		rule = new GuidedRule(
				"(junctionSafety ?X ?__Num0&:(betweenRange ?__Num0 -16.0 26.0)) "
						+ "(junction ?X) => (toJunction ?X ?__Num0)");
		results = sut_.specialiseToPreGoal(rule);
		assertTrue(results.size() >= RuleCreation.NUM_DISCRETE_RANGES + 1);
		interval = 42 / RuleCreation.NUM_DISCRETE_RANGES;
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
					rule)));

		// After interval(s)
		for (int i = 0; i < afterIntervals; i++)
			assertTrue(results.contains(new GuidedRule(
					"(junctionSafety ?X ?__Num0&:(betweenRange ?__Num0 "
							+ (afterInterval * i) + " "
							+ (afterInterval * (i + 1))
							+ ")) (junction ?X) => (toJunction ?X ?__Num0)",
					rule)));

		// Early stage mutations
		pregoal.clear();
		pregoal.add(StateSpec.toStringFact("(dot ?X)"));
		pregoal.add(StateSpec.toStringFact("(pacman player)"));
		pregoal.add(StateSpec.toStringFact("(distanceDot player ?X "
				+ "?__Num3&:(betweenRange ?__Num3 3.0 4.0))"));
		sut_.setPreGoal(StateSpec.toStringFact("(toDot ?X ?__Num3)"), pregoal);

		rule = new GuidedRule(
				"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 10.0 12.0)) "
						+ "(dot ?X) (pacman player) => (toDot ?X ?__Num0)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(results.size(), 7);
		// The regular intervals
		for (int i = 0; i < RuleCreation.NUM_DISCRETE_RANGES; i++) {
			double start = 10.0 + i * (2.0 / RuleCreation.NUM_DISCRETE_RANGES);
			double end = start + (2.0 / RuleCreation.NUM_DISCRETE_RANGES);
			assertTrue(results
					.contains(new GuidedRule(
							"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 "
									+ start
									+ " "
									+ end
									+ ")) (dot ?X) (pacman player) => (toDot ?X ?__Num0)",
							rule)));
		}

		// Pre-goal intervals
		for (int i = 0; i < 2; i++) {
			double start = 3.0 + i * 0.5;
			double end = start + 0.5;
			assertTrue(results
					.contains(new GuidedRule(
							"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 "
									+ start
									+ " "
									+ end
									+ ")) (dot ?X) (pacman player) => (toDot ?X ?__Num0)",
							rule)));
		}

		// Pre-goal range
		assertTrue(results
				.contains(new GuidedRule(
						"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 3.0 4.0)) (dot ?X) (pacman player) => (toDot ?X ?__Num0)",
						rule)));
		
		// Bigger pre-goal range than RLGG range
		pregoal.clear();
		pregoal.add(StateSpec.toStringFact("(dot ?X)"));
		pregoal.add(StateSpec.toStringFact("(pacman player)"));
		pregoal.add(StateSpec.toStringFact("(distanceDot player ?X "
				+ "?__Num3&:(betweenRange ?__Num3 3.0 5.0))"));
		sut_.setPreGoal(StateSpec.toStringFact("(toDot ?X ?__Num3)"), pregoal);

		rule = new GuidedRule(
				"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 10.0 11.0)) "
						+ "(dot ?X) (pacman player) => (toDot ?X ?__Num0)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(results.size(), 9);
		// The regular intervals
		for (int i = 0; i < RuleCreation.NUM_DISCRETE_RANGES; i++) {
			double start = 10.0 + i * (1.0 / RuleCreation.NUM_DISCRETE_RANGES);
			double end = start + (1.0 / RuleCreation.NUM_DISCRETE_RANGES);
			assertTrue(results
					.contains(new GuidedRule(
							"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 "
									+ start
									+ " "
									+ end
									+ ")) (dot ?X) (pacman player) => (toDot ?X ?__Num0)",
							rule)));
		}

		// Pre-goal intervals
		for (int i = 0; i < RuleCreation.NUM_DISCRETE_RANGES; i++) {
			double start = 3.0 + i * 0.5;
			double end = start + 0.5;
			assertTrue(results
					.contains(new GuidedRule(
							"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 "
									+ start
									+ " "
									+ end
									+ ")) (dot ?X) (pacman player) => (toDot ?X ?__Num0)",
							rule)));
		}

		// Pre-goal range
		assertTrue(results
				.contains(new GuidedRule(
						"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 3.0 5.0)) (dot ?X) (pacman player) => (toDot ?X ?__Num0)",
						rule)));
	}

	@Test
	public void testSpecialiseRule() {
		// Set up the allowable conditions
		Collection<StringFact> conditions = new HashSet<StringFact>();
		conditions.add(StateSpec.toStringFact("(edible ?X)"));
		conditions.add(StateSpec.toStringFact("(blinking ?X)"));
		conditions.add(StateSpec.toStringFact("(not (edible ?X))"));
		conditions.add(StateSpec.toStringFact("(not (blinking ?X))"));
		sut_.setAllowedActionConditions("toGhost", conditions);

		GuidedRule rule = new GuidedRule(
				"(distanceGhost player ?X ?__Num6&:(betweenRange ?__Num6 0.0 52.0)) (edible ?X) (pacman player) => (toGhost ?X ?__Num6)");
		Set<GuidedRule> specialisations = sut_.specialiseRule(rule);
		GuidedRule mutant = new GuidedRule(
				"(distanceGhost player ?X ?__Num6&:(betweenRange ?__Num6 0.0 52.0)) (blinking ?X) (pacman player) => (toGhost ?X ?__Num6)");
		assertTrue(specialisations.contains(mutant));
		mutant = new GuidedRule(
				"(distanceGhost player ?X ?__Num6&:(betweenRange ?__Num6 0.0 52.0)) (edible ?X) (not (blinking ?X)) (pacman player) => (toGhost ?X ?__Num6)");
		assertTrue(specialisations.contains(mutant));
		assertEquals(specialisations.size(), 2);
	}
}
