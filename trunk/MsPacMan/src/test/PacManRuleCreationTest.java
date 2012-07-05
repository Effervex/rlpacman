package test;

import static org.junit.Assert.*;

import java.util.Set;

import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.LocalAgentObservations;

import org.junit.Before;
import org.junit.Test;

import cerrla.modular.GoalCondition;

public class PacManRuleCreationTest {
	private LocalAgentObservations.RuleMutation sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("rlPacManGeneral.PacMan");
		LocalAgentObservations lao = LocalAgentObservations
				.loadAgentObservations(null, GoalCondition.parseGoalCondition("blah"));
		sut_ = lao.getRuleMutation();
	}

	@Test
	public void testTypePreds() {
		// Normal RLGG case
		RelationalRule rule = new RelationalRule(
				"(thing ?X) (distance ?X ?#_2) => (moveTo ?X ?#_2)");
		Set<RelationalRule> mutants = sut_.specialiseRule(rule);
		assertEquals(mutants.size(), 12);
		
		mutants = sut_.specialiseRuleMinor(rule);
		assertEquals(mutants.size(), 3);
		
		rule = new RelationalRule(
				"(thing ?X) (distance ?X ?#_2) (not (dot ?X)) => (moveTo ?X ?#_2)");
		mutants = sut_.specialiseRule(rule);
		assertEquals(mutants.size(), 10);
		
		rule = new RelationalRule(
				"(distance ?X ?#_2) (edible ?X) => (moveTo ?X ?#_2)");
		mutants = sut_.specialiseRule(rule);
		assertEquals(mutants.size(), 2);
		
		rule = new RelationalRule(
				"(distance ?X ?#_2) (dot ?X) => (moveTo ?X ?#_2)");
		mutants = sut_.specialiseRule(rule);
		assertEquals(mutants.size(), 0);
	}
}
