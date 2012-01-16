package test;

import static org.junit.Assert.*;

import relationalFramework.GoalCondition;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.agentObservations.LocalAgentObservations;

public class PacManGeneralRuleCreationTest {
	private LocalAgentObservations.RuleMutation sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("rlPacManGeneral.PacMan");
		LocalAgentObservations lao = LocalAgentObservations
				.loadAgentObservations(new GoalCondition("blah"));
		sut_ = lao.getRuleMutation();
	}

	@Test
	public void testSpecialiseRule() {
		// Checking that no specialisations are created (edible and blinking
		// aren't added)
		RelationalRule rule = new RelationalRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 0.0 52.0)) "
						+ "(dot ?X) => (moveTo ?X ?__Num0)");
		Set<RelationalRule> specialisations = sut_.specialiseRule(rule);
		assertTrue(specialisations.toString(), specialisations.isEmpty());

		// They should be added here
		rule = new RelationalRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 0.0 52.0)) "
						+ "(ghost ?X) => (moveTo ?X ?__Num0)");
		specialisations = sut_.specialiseRule(rule);
		assertFalse(specialisations.toString(), specialisations.isEmpty());
		RelationalRule mutant = new RelationalRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 0.0 52.0)) "
						+ "(ghost ?X) (edible ?X) => (moveTo ?X ?__Num0)");
		assertTrue(specialisations.contains(mutant));
		mutant = new RelationalRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 0.0 52.0)) "
						+ "(ghost ?X) (blinking ?X) => (moveTo ?X ?__Num0)");
		assertTrue(specialisations.contains(mutant));
		mutant = new RelationalRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 0.0 52.0)) "
						+ "(ghost ?X) (not (edible ?X)) => (moveTo ?X ?__Num0)");
		assertTrue(specialisations.contains(mutant));
		mutant = new RelationalRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 0.0 52.0)) "
						+ "(ghost ?X) (not (blinking ?X)) => (moveTo ?X ?__Num0)");
		assertTrue(specialisations.contains(mutant));
	}
	
	@Test
	public void testGeneralRangedConditions() {
		// A split containing multiple ranges
		RelationalRule rule = new RelationalRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 0.0 52.0)) (powerDot ?X) "
						+ "(distance ?Y ?__Num1&:(betweenRange ?__Num1 0.0 10.0)) (ghost ?Y)"
						+ " => (moveTo ?X ?__Num0)");
		Set<RelationalRule> specialisedRules = sut_.specialiseRuleMinor(rule);
		rule = new RelationalRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 0.0 26.0)) (powerDot ?X) "
						+ "(distance ?Y ?__Num1&:(betweenRange ?__Num1 0.0 10.0)) (ghost ?Y)"
						+ " => (moveTo ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new RelationalRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 26.0 52.0)) (powerDot ?X) "
						+ "(distance ?Y ?__Num1&:(betweenRange ?__Num1 0.0 10.0)) (ghost ?Y)"
						+ " => (moveTo ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new RelationalRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 13.0 39.0)) (powerDot ?X) "
						+ "(distance ?Y ?__Num1&:(betweenRange ?__Num1 0.0 10.0)) (ghost ?Y)"
						+ " => (moveTo ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new RelationalRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 0.0 52.0)) (powerDot ?X) "
						+ "(distance ?Y ?__Num1&:(betweenRange ?__Num1 0.0 5.0)) (ghost ?Y)"
						+ " => (moveTo ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new RelationalRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 0.0 52.0)) (powerDot ?X) "
						+ "(distance ?Y ?__Num1&:(betweenRange ?__Num1 5.0 10.0)) (ghost ?Y)"
						+ " => (moveTo ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new RelationalRule(
				"(distance ?X ?__Num0&:(betweenRange ?__Num0 0.0 52.0)) (powerDot ?X) "
						+ "(distance ?Y ?__Num1&:(betweenRange ?__Num1 2.5 7.5)) (ghost ?Y)"
						+ " => (moveTo ?X ?__Num0)");
		assertEquals(specialisedRules.size(), 6);
	}
}
