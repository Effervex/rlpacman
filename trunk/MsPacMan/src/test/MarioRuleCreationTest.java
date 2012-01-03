package test;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import cerrla.RuleCreation;

public class MarioRuleCreationTest {
	private RuleCreation sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("mario.RLMario");
		sut_ = new RuleCreation();
	}

	@Test
	public void testSpecialiseRuleMinor() {
		RelationalRule rule = new RelationalRule("(thing ?X) (canJumpOn ?X) "
				+ "(distance ?X ?#_2&:(<= -159.0 ?#_2 160.0)) "
				+ "(heightDiff ?X ?#_3&:(<= -242.0 ?#_3 87.0)) "
				+ "(not (width ?X ?)) => (jumpOnto ?X ?#_2)");
		Set<RelationalRule> specialisedRules = sut_.specialiseRuleMinor(rule);

		assertEquals(specialisedRules.size(), 6);
	}
}
