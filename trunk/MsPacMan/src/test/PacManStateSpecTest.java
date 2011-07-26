package test;

import static org.junit.Assert.*;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

public class PacManStateSpecTest {
	@Before
	public void setUp() throws Exception {
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
		StateSpec.initInstance("rlPacMan.PacMan");
	}

	@Test
	public void testRuleCreation() {
		// All the main tests are covered in the BlocksWorld StateSpec test

		// Testing numbers
		RelationalRule rule = new RelationalRule(
				"(distanceGhost ?Player ?Ghost 4) => (fromGhost ?Ghost)");
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(distanceGhost ?Player ?Ghost 4)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?Player ?Ghost))")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(pacman ?Player)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(ghost ?Ghost)")));
		assertEquals(rule.getConditions(false).size(), 4);
		assertTrue(rule.getStringConditions().indexOf("distanceGhost") < rule
				.getStringConditions().indexOf("pacman"));
		assertTrue(rule.getStringConditions().indexOf("pacman") < rule
				.getStringConditions().indexOf("test"));
		assertEquals(rule.getAction(), StateSpec
				.toRelationalPredicate("(fromGhost ?Ghost)"));

		// Testing conditional &:elements
		rule = new RelationalRule(
				"(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 1 4))"
						+ " => (fromGhost ?Ghost)");
		assertEquals(rule.getConditions(false).size(), 4);
		assertTrue(rule
				.getConditions(false)
				.contains(
						StateSpec
								.toRelationalPredicate("(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 1 4))")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(test (<> ?Player ?Ghost))")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(pacman ?Player)")));
		assertTrue(rule.getConditions(false).contains(
				StateSpec.toRelationalPredicate("(ghost ?Ghost)")));
		assertTrue(rule.getStringConditions().indexOf("distanceGhost") < rule
				.getStringConditions().indexOf("pacman"));
		assertTrue(rule.getStringConditions().indexOf("pacman") < rule
				.getStringConditions().indexOf("test"));
		assertEquals(rule.getAction(), StateSpec
				.toRelationalPredicate("(fromGhost ?Ghost)"));
	}
}
