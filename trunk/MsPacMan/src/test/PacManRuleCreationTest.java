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
import relationalFramework.agentObservations.AgentObservations;
import relationalFramework.agentObservations.PreGoalInformation;

public class PacManRuleCreationTest {
	private RuleCreation sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("rlPacMan.PacMan");
		sut_ = new RuleCreation();
	}

	@Test
	public void testSpecialiseToPreGoal() {
		GuidedRule rule = new GuidedRule(
				"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 0.0 36.0)) "
						+ "(ghost ?X) (pacman player) => (toGhost ?X ?__Num0)");
		Collection<GuidedRule> results = sut_.specialiseToPreGoal(rule);
		GuidedRule mutant = new GuidedRule(
				"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 0.0 18.0)) "
						+ "(ghost ?X) (pacman player) => (toGhost ?X ?__Num0)",
				rule);
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 18.0 36.0)) "
						+ "(ghost ?X) (pacman player) => (toGhost ?X ?__Num0)",
				rule);
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 9.0 27.0)) "
						+ "(ghost ?X) (pacman player) => (toGhost ?X ?__Num0)",
				rule);
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 3);

		// Specialising a range without pregoal, but rule is mutant (failure)
		rule = new GuidedRule(
				"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 9.0 18.0)) "
						+ "(ghost ?X) (pacman player) => (toGhost ?X ?__Num0)");
		rule.setMutant(rule);
		results = sut_.specialiseToPreGoal(rule);
		mutant = new GuidedRule(
				"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 9.0 13.5)) "
						+ "(ghost ?X) (pacman player) => (toGhost ?X ?__Num0)",
				rule);
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 13.5 18.0)) "
						+ "(ghost ?X) (pacman player) => (toGhost ?X ?__Num0)",
				rule);
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(distanceGhost player ?X ?__Num0&:(betweenRange ?__Num0 11.25 15.75)) "
						+ "(ghost ?X) (pacman player) => (toGhost ?X ?__Num0)",
				rule);
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 3);

		// Specialising a range with a single numerical pregoal
		List<StringFact> pregoal = new ArrayList<StringFact>();
		pregoal.add(StateSpec.toStringFact("(dot ?X)"));
		pregoal.add(StateSpec.toStringFact("(pacman player)"));
		pregoal.add(StateSpec.toStringFact("(distanceDot player ?X 14.0)"));
		AgentObservations.getInstance().setPreGoal("toDot",
				new PreGoalInformation(pregoal, new String[] { "?X", "14.0" }));

		rule = new GuidedRule(
				"(distanceDot player ?X ?__Num3&:(betweenRange ?__Num3 0.0 36.0)) "
						+ "(dot ?X) (pacman player) => (toDot ?X ?__Num3)");
		results = sut_.specialiseToPreGoal(rule);

		// The point itself
		assertTrue(results.contains(new GuidedRule(
				"(distanceDot player ?X 14.0) "
						+ "(dot ?X) (pacman player) => (toDot ?X 14.0)", rule)));
		assertEquals(results.size(), 4);

		// Specialising a range to a ranged pre-goal

		pregoal.clear();
		pregoal.add(StateSpec.toStringFact("(dot ?X)"));
		pregoal.add(StateSpec.toStringFact("(pacman player)"));
		pregoal
				.add(StateSpec
						.toStringFact("(distanceDot player ?X ?__Num3&:(betweenRange ?__Num3 14.0 23.0))"));
		AgentObservations.getInstance().setPreGoal(
				"toDot",
				new PreGoalInformation(pregoal,
						new String[] { "?X", "?__Num3" }));

		rule = new GuidedRule(
				"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 0.0 36.0)) "
						+ "(dot ?X) (pacman player) => (toDot ?X ?__Num0)");
		results = sut_.specialiseToPreGoal(rule);
		assertTrue(results.contains(new GuidedRule(
				"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 14.0 23.0))) "
						+ "(dot ?X) (pacman player) => (toDot ?X ?__Num0)",
				rule)));
		assertEquals(results.size(), 4);

		// Special case: Range goes through 0 (no pregoal)
		AgentObservations.getInstance().clearPreGoal();

		rule = new GuidedRule(
				"(junctionSafety ?X ?__Num0&:(betweenRange ?__Num0 -16.0 26.0)) "
						+ "(junction ?X) => (toJunction ?X ?__Num0)");
		results = sut_.specialiseToPreGoal(rule);
		mutant = new GuidedRule(
				"(junctionSafety ?X ?__Num0&:(betweenRange ?__Num0 -16.0 0.0)) "
						+ "(junction ?X) => (toJunction ?X ?__Num0)", rule);
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(junctionSafety ?X ?__Num0&:(betweenRange ?__Num0 0.0 26.0)) "
						+ "(junction ?X) => (toJunction ?X ?__Num0)", rule);
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 5);
	}

	@Test
	public void testSpecialiseRangedPreGoal() throws Exception {
		GuidedRule rule = new GuidedRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 -10.0 25.0))"
						+ " => (toGhost ?X ?__Num0)");
		Set<GuidedRule> specialisedRules = sut_.specialiseToPreGoal(rule);

		// Ranges should be 5 basic sets: those under 0 and those over 0, and 3
		// overlapping equal ranges.
		rule = new GuidedRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 -10.0 7.5))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new GuidedRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 7.5 25.0))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new GuidedRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 -1.25 16.25))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new GuidedRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 -10.0 0.0))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new GuidedRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 0.0 25.0))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		assertEquals(specialisedRules.size(), 5);

		// Specialise again
		specialisedRules = sut_.specialiseToPreGoal(rule);

		// Another split, this time with 3 overlapping ranges
		rule = new GuidedRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 0.0 12.5))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new GuidedRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 6.25 18.75))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new GuidedRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 12.5 25.0))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		assertEquals(specialisedRules.size(), 3);

		// One more split
		specialisedRules = sut_.specialiseToPreGoal(rule);

		// Another split, this time with 3 overlapping ranges
		rule = new GuidedRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 12.5 18.75))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new GuidedRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 15.625 21.875))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new GuidedRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 18.75 25.0))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		assertEquals(specialisedRules.size(), 3);
	}

	@Test
	public void testSpecialiseRule() {
		// Set up the allowable conditions
		Collection<StringFact> conditions = new HashSet<StringFact>();
		conditions.add(StateSpec.toStringFact("(edible ?X)"));
		conditions.add(StateSpec.toStringFact("(blinking ?X)"));
		conditions.add(StateSpec.toStringFact("(not (edible ?X))"));
		conditions.add(StateSpec.toStringFact("(not (blinking ?X))"));
		AgentObservations.getInstance().setActionConditions("toGhost",
				conditions);

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
