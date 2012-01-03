package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import cerrla.RuleCreation;

public class PacManRuleCreationTest {
	private RuleCreation sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("rlPacMan.PacMan");
		sut_ = new RuleCreation();
	}

	@Test
	public void testSpecialiseRangedConditions() throws Exception {
		RelationalRule rule = new RelationalRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 -10.0 25.0))"
						+ " => (toGhost ?X ?__Num0)");
		Set<RelationalRule> specialisedRules = sut_.specialiseRuleMinor(rule);

		// Ranges should be 5 basic sets: those under 0 and those over 0, and 3
		// overlapping equal ranges.
		rule = new RelationalRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 -10.0 7.5))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new RelationalRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 7.5 25.0))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new RelationalRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 -1.25 16.25))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new RelationalRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 -10.0 0.0))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new RelationalRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 0.0 25.0))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		assertEquals(specialisedRules.size(), 5);

		// Specialise again
		specialisedRules = sut_.specialiseRuleMinor(rule);

		// Another split, this time with 3 overlapping ranges
		rule = new RelationalRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 0.0 12.5))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new RelationalRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 6.25 18.75))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new RelationalRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 12.5 25.0))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		assertEquals(specialisedRules.size(), 3);

		// One more split
		specialisedRules = sut_.specialiseRuleMinor(rule);

		// Another split, this time with 3 overlapping ranges
		rule = new RelationalRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 12.5 18.75))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new RelationalRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 15.625 21.875))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		rule = new RelationalRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 18.75 25.0))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(specialisedRules.toString(), specialisedRules.contains(rule));
		assertEquals(specialisedRules.size(), 3);
	}

	@Test
	public void testGeneralRangedConditions() {
		StateSpec.initInstance("rlPacManGeneral.PacMan");
		sut_ = new RuleCreation();

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
