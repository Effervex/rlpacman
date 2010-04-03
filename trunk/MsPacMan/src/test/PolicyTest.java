package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import jess.Rete;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.GuidedRule;
import relationalFramework.Module;
import relationalFramework.Policy;
import relationalFramework.StateSpec;

public class PolicyTest {

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
	}

	@Test
	public void testAddRule() {
		// Basic rule adding
		Policy pol = new Policy();
		GuidedRule rule = new GuidedRule("(clear ?X) => (moveFloor ?X)");
		pol.addRule(rule, false);
		assertEquals(pol.getPolicyRules().size(), 1);
		assertTrue(pol.getPolicyRules().contains(rule));
		assertTrue(pol.getFiringRules().isEmpty());

		// Rule adding with modular check (but no module defined)
		pol = new Policy();
		rule = new GuidedRule("(clear ?X) => (moveFloor ?X)");
		pol.addRule(rule, true);
		assertEquals(pol.getPolicyRules().size(), 1);
		assertTrue(pol.getPolicyRules().contains(rule));
		
		// Check the clear module exists
		if (Module.loadModule("blocksWorld", "clear") == null)
			fail("'clear' module doesn't exist!");

		// Rule adding with modular check (module fires)
		pol = new Policy();
		rule = new GuidedRule("(clear a) => (moveFloor a)");
		pol.addRule(rule, true);
		assertEquals(pol.getPolicyRules().size(), 2);
		assertTrue(pol.getPolicyRules().contains(rule));
		List<String> queryParams = new ArrayList<String>();
		queryParams.add("?_MOD_a");
		GuidedRule modRule = new GuidedRule("(above ?X ?_MOD_a) (clear ?X) => (moveFloor ?X)", queryParams);
		List<String> params = new ArrayList<String>();
		params.add("a");
		modRule.setParameters(params);
		assertTrue(pol.getPolicyRules().contains(modRule));
		
		// Check the on module exists
		if (Module.loadModule("blocksWorld", "on") == null)
			fail("'on' module doesn't exist!");

		// Rule adding with modular check (recursive modular firing where on
		// module calls clear module)
		pol = new Policy();
		rule = new GuidedRule("(on a b) => (moveFloor a)");
		pol.addRule(rule, true);
		assertEquals(pol.getPolicyRules().size(), 4);
		assertTrue(pol.getPolicyRules().contains(rule));
		// Clear rules
		queryParams = new ArrayList<String>();
		queryParams.add("?_MOD_a");
		modRule = new GuidedRule("(above ?X ?_MOD_a) (clear ?X) => (moveFloor ?X)", queryParams);
		params = new ArrayList<String>();
		params.add("a");
		modRule.setParameters(params);
		assertTrue(pol.getPolicyRules().contains(modRule));
		params.clear();
		params.add("b");
		modRule.setParameters(params);
		assertTrue(pol.getPolicyRules().contains(modRule));
		// On rule
		queryParams = new ArrayList<String>();
		queryParams.add("?_MOD_a");
		queryParams.add("?_MOD_b");
		modRule = new GuidedRule("(clear ?_MOD_a) (clear ?_MOD_b) => (move ?_MOD_a ?_MOD_b)", queryParams);
		params = new ArrayList<String>();
		params.add("a");
		params.add("b");
		modRule.setParameters(params);
		assertTrue(pol.getPolicyRules().contains(modRule));
	}

	@Test
	public void testGetFiringRules() {
		fail("Not yet implemented");
	}

	@Test
	public void testEvaluatePolicy() {
		fail("Not yet implemented");
	}

}
