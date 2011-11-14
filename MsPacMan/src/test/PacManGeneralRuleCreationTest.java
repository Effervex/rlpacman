package test;

import static org.junit.Assert.*;

import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import cerrla.RuleCreation;

import relationalFramework.agentObservations.AgentObservations;

public class PacManGeneralRuleCreationTest {
	private RuleCreation sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("rlPacManGeneral.PacMan");
		sut_ = new RuleCreation();

		// Set up the allowable conditions
		AgentObservations.loadAgentObservations("blah");
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
}
