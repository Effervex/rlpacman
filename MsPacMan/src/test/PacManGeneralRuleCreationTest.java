package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.RuleCreation;
import relationalFramework.GuidedRule;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

public class PacManGeneralRuleCreationTest {
	private RuleCreation sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("rlPacManGeneral.PacMan");
		sut_ = new RuleCreation();

		// Set up the allowable conditions
		assertTrue("No loaded agent observations. Cannot run test.", sut_
				.hasAgentObservations());
	}

	@Test
	public void testSpecialiseToPreGoal() {
		// Test typed and constant specialising
		List<StringFact> pregoal = new ArrayList<StringFact>();
		pregoal.add(StateSpec.toStringFact("(ghost blinky)"));
		pregoal.add(StateSpec.toStringFact("(thing blinky)"));
		pregoal.add(StateSpec.toStringFact("(edible blinky)"));
		pregoal.add(StateSpec.toStringFact("(distance blinky 2)"));
		sut_.setPreGoal(StateSpec.toStringFact("(moveTo blinky 2)"), pregoal);

		GuidedRule rule = new GuidedRule(
				"(distance ?X ?__Num3&:(betweenRange ?__Num3 0.0 36.0)) "
						+ "(dot ?X) => (moveTo ?X ?__Num3)");
		Set<GuidedRule> results = sut_.specialiseToPreGoal(rule);

		// The 4 mutants (including the point).
		assertTrue(results.contains(new GuidedRule(
				"(distance ?X ?__Num3&:(betweenRange ?__Num3 0.0 18.0)) "
						+ "(dot ?X) => (moveTo ?X ?__Num3)", rule)));
		assertTrue(results.contains(new GuidedRule(
				"(distance ?X ?__Num3&:(betweenRange ?__Num3 18.0 36.0)) "
						+ "(dot ?X) => (moveTo ?X ?__Num3)", rule)));
		assertTrue(results.contains(new GuidedRule(
				"(distance ?X ?__Num3&:(betweenRange ?__Num3 9.0 27.0)) "
						+ "(dot ?X) => (moveTo ?X ?__Num3)", rule)));
		assertTrue(results.contains(new GuidedRule("(distance ?X 2.0) "
				+ "(dot ?X) => (moveTo ?X 2.0)", rule)));
		assertFalse(results.contains(new GuidedRule(
				"(distance ?X ?__Num3&:(betweenRange ?__Num3 0.0 36.0)) "
						+ "(edible ?X) (dot ?X) => (moveTo ?X ?__Num3)", rule)));
		assertFalse(results.contains(new GuidedRule(
				"(distance blinky ?__Num3&:(betweenRange ?__Num3 0.0 36.0)) "
						+ "(dot blinky) => (moveTo blinky ?__Num3)", rule)));
		assertEquals(results.size(), 4);

		// Valid specialisations
		rule = new GuidedRule(
				"(distance ?X ?__Num3&:(betweenRange ?__Num3 0.0 36.0)) "
						+ "(ghost ?X) => (moveTo ?X ?__Num3)");
		results = sut_.specialiseToPreGoal(rule);

		// The 5 mutants (adding edible condition)
		assertTrue(results.contains(new GuidedRule(
				"(distance ?X ?__Num3&:(betweenRange ?__Num3 0.0 18.0)) "
						+ "(ghost ?X) => (moveTo ?X ?__Num3)", rule)));
		assertTrue(results.contains(new GuidedRule(
				"(distance ?X ?__Num3&:(betweenRange ?__Num3 18.0 36.0)) "
						+ "(ghost ?X) => (moveTo ?X ?__Num3)", rule)));
		assertTrue(results.contains(new GuidedRule(
				"(distance ?X ?__Num3&:(betweenRange ?__Num3 9.0 27.0)) "
						+ "(ghost ?X) => (moveTo ?X ?__Num3)", rule)));
		assertTrue(results.contains(new GuidedRule("(distance ?X 2.0) "
				+ "(ghost ?X) => (moveTo ?X 2.0)", rule)));
		assertTrue(results
				.contains(new GuidedRule(
						"(distance ?X ?__Num3&:(betweenRange ?__Num3 0.0 36.0)) "
								+ "(edible ?X) (ghost ?X) => (moveTo ?X ?__Num3)",
						rule)));
		assertTrue(results.contains(new GuidedRule(
				"(distance blinky ?__Num3&:(betweenRange ?__Num3 0.0 36.0)) "
						+ "(ghost blinky) => (moveTo blinky ?__Num3)", rule)));
		assertEquals(results.size(), 6);
	}

	@Test
	public void testSpecialiseRule() {
		// Checking that no specialisations are created (edible and blinking
		// aren't added)
		GuidedRule rule = new GuidedRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 0.0 52.0)) "
						+ "(dot ?X) => (moveTo ?X ?__Num0)");
		Set<GuidedRule> specialisations = sut_.specialiseRule(rule);
		assertTrue(specialisations.toString(), specialisations.isEmpty());

		// They should be added here
		rule = new GuidedRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 0.0 52.0)) "
						+ "(ghost ?X) => (moveTo ?X ?__Num0)");
		specialisations = sut_.specialiseRule(rule);
		assertFalse(specialisations.toString(), specialisations.isEmpty());
		GuidedRule mutant = new GuidedRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 0.0 52.0)) "
						+ "(ghost ?X) (edible ?X) => (moveTo ?X ?__Num0)");
		assertTrue(specialisations.contains(mutant));
		mutant = new GuidedRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 0.0 52.0)) "
						+ "(ghost ?X) (blinking ?X) => (moveTo ?X ?__Num0)");
		assertTrue(specialisations.contains(mutant));
		mutant = new GuidedRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 0.0 52.0)) "
						+ "(ghost ?X) (not (edible ?X)) => (moveTo ?X ?__Num0)");
		assertTrue(specialisations.contains(mutant));
		mutant = new GuidedRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 0.0 52.0)) "
						+ "(ghost ?X) (not (blinking ?X)) => (moveTo ?X ?__Num0)");
		assertTrue(specialisations.contains(mutant));
	}
}
