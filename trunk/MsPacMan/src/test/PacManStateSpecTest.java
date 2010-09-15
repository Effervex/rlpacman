package test;

import static org.junit.Assert.*;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import relationalFramework.GuidedRule;
import relationalFramework.StateSpec;

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
		// 2 assertions in the body: clear, and block
		assertEquals(rule.getConditions().size(), 4);
		assertTrue(rule.getConditions().contains(
				"(distanceGhost ?Player ?Ghost 4)"));
		assertTrue(rule.getConditions().contains("(test (<> ?Player ?Ghost))"));
		assertTrue(rule.getConditions().contains("(pacman ?Player)"));
		assertTrue(rule.getConditions().contains("(ghost ?Ghost)"));
		assertTrue(rule.getStringConditions().indexOf("distanceGhost") < rule
				.getStringConditions().indexOf("pacman"));
		assertTrue(rule.getStringConditions().indexOf("pacman") < rule
				.getStringConditions().indexOf("test"));
		assertEquals(rule.getAction(), "(fromGhost ?Ghost)");

		// Testing conditional &:elements
		rule = new GuidedRule(
				"(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 1 4))"
						+ " => (fromGhost ?Ghost)");
		// 2 assertions in the body: clear, and block
		assertEquals(rule.getConditions().size(), 4);
		assertTrue(rule
				.getConditions()
				.contains(
						"(distanceGhost ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 1 4))"));
		assertTrue(rule.getConditions().contains("(test (<> ?Player ?Ghost))"));
		assertTrue(rule.getConditions().contains("(pacman ?Player)"));
		assertTrue(rule.getConditions().contains("(ghost ?Ghost)"));
		assertTrue(rule.getStringConditions().indexOf("distanceGhost") < rule
				.getStringConditions().indexOf("pacman"));
		assertTrue(rule.getStringConditions().indexOf("pacman") < rule
				.getStringConditions().indexOf("test"));
		assertEquals(rule.getAction(), "(fromGhost ?Ghost)");
	}
}
