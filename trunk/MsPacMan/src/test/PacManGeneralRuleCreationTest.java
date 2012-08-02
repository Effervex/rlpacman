package test;

import static org.junit.Assert.*;

import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import cerrla.LocalCrossEntropyDistribution;
import cerrla.modular.GeneralGoalCondition;
import cerrla.modular.GoalCondition;
import cerrla.modular.UndefinedGoalCondition;

import relationalFramework.agentObservations.LocalAgentObservations;

public class PacManGeneralRuleCreationTest {
	private LocalAgentObservations.RuleMutation sut_;
	private LocalCrossEntropyDistribution lced_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("rlPacManGeneral.PacMan");
		lced_ = new LocalCrossEntropyDistribution(
				new UndefinedGoalCondition("1level"));
		sut_ = lced_.getLocalAgentObservations().getRuleMutation();
	}

	@Test
	public void testSpecialiseRule() {
		// RLGG rule
		RelationalRule rule = new RelationalRule(
				"(distance ?A ?#_0) (thing ?A) => (moveTo ?A ?#_0)", lced_);
		Set<RelationalRule> specialisations = sut_.specialiseRule(rule);
		assertEquals(specialisations.size(), 12);

		// Checking that no specialisations are created (edible and blinking
		// aren't added)
		rule = new RelationalRule(
				"(distance ?A ?#_0) (dot ?A) => (moveTo ?A ?#_0)", lced_);
		specialisations = sut_.specialiseRule(rule);
		assertTrue(specialisations.toString(), specialisations.isEmpty());

		// They should be added here
		rule = new RelationalRule(
				"(distance ?A ?#_0) (ghost ?A) => (moveTo ?A ?#_0)", lced_);
		specialisations = sut_.specialiseRule(rule);
		assertFalse(specialisations.toString(), specialisations.isEmpty());
		RelationalRule mutant = new RelationalRule(
				"(distance ?A ?#_0) (edible ?A) => (moveTo ?A ?#_0)");
		assertTrue(specialisations.contains(mutant));
		mutant = new RelationalRule(
				"(distance ?A ?#_0) (blinking ?A) => (moveTo ?A ?#_0)");
		assertTrue(specialisations.contains(mutant));
		mutant = new RelationalRule(
				"(distance ?A ?#_0) "
						+ "(ghost ?A) (not (edible ?A)) => (moveTo ?A ?#_0)");
		assertTrue(specialisations.contains(mutant));
		mutant = new RelationalRule(
				"(distance ?A ?#_0) "
						+ "(ghost ?A) (not (blinking ?A)) => (moveTo ?A ?#_0)");
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
