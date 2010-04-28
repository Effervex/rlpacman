package test;

import static org.junit.Assert.*;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

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
	public void testApplyAction() {
		fail("Not yet implemented");
	}

	@Test
	public void testParseRule() {
		// All the main tests are covered in the BlocksWorld StateSpec test

		// Testing numbers
		String rule = spec_
				.parseRule("(distance ?Player ?Ghost 4) => (fromGhost ?Ghost)");
		String body = rule.split("=>")[0];
		int condCount = body.replaceAll("\\(.+?\\)( |$)", ".").length();
		String head = rule.split("=>")[1];
		// 2 assertions in the body: clear, and block
		assertEquals(condCount, 4);
		assertTrue(body.contains("(distance ?Player ?Ghost 4)"));
		assertTrue(body.contains("(test (<> ?Player ?Ghost))"));
		assertTrue(body.contains("(pacPoint ?Player)"));
		assertTrue(body.contains("(pacPoint ?Ghost)"));
		assertTrue(body.indexOf("distance") < body.indexOf("test"));
		assertTrue(body.indexOf("test") < body.indexOf("pacPoint"));
		assertTrue(head.contains("(fromGhost ?Ghost)"));

		// Testing conditional &:elements
		rule = spec_
				.parseRule("(distance ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 1 4))"
						+ " => (fromGhost ?Ghost)");
		body = rule.split("=>")[0];
		condCount = body.replaceAll("\\(.+?\\)( |$)", ".").length();
		head = rule.split("=>")[1];
		// 2 assertions in the body: clear, and block
		assertEquals(condCount, 4);
		assertTrue(body.contains("(distance ?Player ?Ghost ?Dist0&:(betweenRange ?Dist0 1 4))"));
		assertTrue(body.contains("(test (<> ?Player ?Ghost))"));
		assertTrue(body.contains("(pacPoint ?Player)"));
		assertTrue(body.contains("(pacPoint ?Ghost)"));
		assertTrue(body.indexOf("distance") < body.indexOf("test"));
		assertTrue(body.indexOf("test") < body.indexOf("pacPoint"));
		assertTrue(head.contains("(fromGhost ?Ghost)"));
	}
}
