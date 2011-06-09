package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import relationalFramework.GuidedRule;
import relationalFramework.Slot;
import relationalFramework.StateSpec;
import relationalFramework.util.ProbabilityDistribution;

public class PacManStateSpecTest {

	private StateSpec spec_;

	@Before
	public void setUp() throws Exception {
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
		spec_ = StateSpec.initInstance("rlPacMan.PacMan");
	}

	@Test
	public void testRuleCreation() {
		// All the main tests are covered in the BlocksWorld StateSpec test

		// Testing numbers
		GuidedRule rule = new GuidedRule(
				"(distanceGhost ?Player ?Ghost 4) => (fromGhost ?Ghost)");
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toStringFact("(distanceGhost ?Player ?Ghost 4)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toStringFact("(test (<> ?Player ?Ghost))")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toStringFact("(pacman ?Player)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toStringFact("(ghost ?Ghost)")));
		assertEquals(rule.getConditions(false).size(), 4);
		assertTrue(rule.getStringConditions().indexOf("distanceGhost") < rule
				.getStringConditions().indexOf("pacman"));
		assertTrue(rule.getStringConditions().indexOf("pacman") < rule
				.getStringConditions().indexOf("test"));
		assertEquals(rule.getAction(), StateSpec
				.toStringFact("(fromGhost ?Ghost)"));

		// Testing conditional &:elements
		rule = new GuidedRule(
				"(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 1 4))"
						+ " => (fromGhost ?Ghost)");
		assertEquals(rule.getConditions(false).size(), 4);
		assertTrue(rule
				.getConditions(false)
				.contains(
						StateSpec
								.toStringFact("(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 1 4))")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toStringFact("(test (<> ?Player ?Ghost))")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toStringFact("(pacman ?Player)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toStringFact("(ghost ?Ghost)")));
		assertTrue(rule.getStringConditions().indexOf("distanceGhost") < rule
				.getStringConditions().indexOf("pacman"));
		assertTrue(rule.getStringConditions().indexOf("pacman") < rule
				.getStringConditions().indexOf("test"));
		assertEquals(rule.getAction(), StateSpec
				.toStringFact("(fromGhost ?Ghost)"));
	}

	@Test
	public void testRuleRemoval() {
		// Parse this slot and test removal.
		Slot ruleSlot = Slot
				.parseSlotString("(Slot (fromPowerDot) {((distancePowerDot player ?X ?__Num1&:(betweenRange ?__Num1 37.75 50.0)) (powerDot ?X) (test (<> ?X player)) => (fromPowerDot ?X ?__Num1):0.4013070052778781),"
						+ " ((distancePowerDot player ?X ?__Num1&:(betweenRange ?__Num1 25.5 37.75)) (powerDot ?X) (test (<> ?X player)) => (fromPowerDot ?X ?__Num1):0.3436855034648942),"
						+ " ((distancePowerDot player ?X ?__Num1&:(betweenRange ?__Num1 37.5 50.0)) (powerDot ?X) (test (<> ?X player)) => (fromPowerDot ?X ?__Num1):0.16679938374483333),"
						+ " ((distancePowerDot player ?X ?__Num1&:(betweenRange ?__Num1 12.5 25.0)) (powerDot ?X) (test (<> ?X player)) => (fromPowerDot ?X ?__Num1):0.08820810751239643)},0.7513931524666813,0.5:0.22540766013609628)");

		ProbabilityDistribution<GuidedRule> distribution = ruleSlot
				.getGenerator();
		double pruneProb = (1.0 / distribution.size()) * 0.4;
		Collection<GuidedRule> removables = new ArrayList<GuidedRule>();
		for (GuidedRule rule : distribution) {
			double ruleProb = distribution.getProb(rule);
			if ((ruleProb != -1) && (ruleProb <= pruneProb)) {
				removables.add(rule);
			}
		}

		// If rules are to be removed, remove them.
		if (!removables.isEmpty()) {
			for (GuidedRule rule : removables) {
				distribution.remove(rule);
				System.out.println("\tREMOVED RULE: " + rule);
			}

			distribution.normaliseProbs();
		}

		assertEquals(distribution.size(), 3);
	}
}
