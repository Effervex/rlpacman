package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.GuidedRule;
import relationalFramework.Module;
import relationalFramework.Policy;
import relationalFramework.PolicyGenerator;
import relationalFramework.StateSpec;

public class PolicyTest {

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
		PolicyGenerator.newInstance();
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
		int clearRulesNum = Module.loadModule("blocksWorld", "clear")
				.getModuleRules().size();

		// Rule adding with modular check (module fires)
		pol = new Policy();
		rule = new GuidedRule("(clear a) => (moveFloor a)");
		pol.addRule(rule, true);

		assertEquals(pol.getPolicyRules().size(), clearRulesNum + 1);
		assertTrue(pol.getPolicyRules().contains(rule));
		List<String> queryParams = new ArrayList<String>();
		queryParams.add("?_MOD_a");
		GuidedRule modRule = new GuidedRule(
				"(above ?X ?_MOD_a) (clear ?X) => (moveFloor ?X)", queryParams);
		List<String> params = new ArrayList<String>();
		params.add("a");
		modRule.setParameters(params);
		assertTrue(pol.getPolicyRules().contains(modRule));
		
		if (Module.loadModule("blocksWorld", "clear&clear") == null)
			fail("'clear&clear' module doesn't exist!");

		// Rule adding with multiple modular check
		pol = new Policy();
		rule = new GuidedRule("(clear a) (clear b) => (moveFloor a)");
		pol.addRule(rule, true);
		assertEquals(pol.getPolicyRules().size(), clearRulesNum * 2 + 1);
		assertTrue(pol.getPolicyRules().contains(rule));
		int i = 0;
		for (GuidedRule gr : pol.getPolicyRules()) {
			if (i < clearRulesNum) {
				assertTrue(gr.getParameters().size() == 2);
				assertTrue(gr.getParameters().contains("a"));
			}
			else if (i < (clearRulesNum * 2)) {
				assertTrue(gr.getParameters().size() == 2);
				assertTrue(gr.getParameters().contains("b"));
			} else {
				assertTrue(gr.getParameters() == null);
			}
			i++;
		}

		// Check the on module exists
		if (Module.loadModule("blocksWorld", "on") == null)
			fail("'on' module doesn't exist!");

		// Rule adding with modular check (recursive modular firing where on
		// module calls clear module)
		pol = new Policy();
		rule = new GuidedRule("(on a b) => (moveFloor a)");
		pol.addRule(rule, true);
		assertEquals(pol.getPolicyRules().size(), 6);
		assertTrue(pol.getPolicyRules().contains(rule));
		// Clear rules
		queryParams = new ArrayList<String>();
		queryParams.add("?_MOD_a");
		queryParams.add("?_MOD_b");
		modRule = new GuidedRule(
				"(above ?X ?_MOD_a) (clear ?X) => (moveFloor ?X)", queryParams);
		params = new ArrayList<String>();
		params.add("a");
		params.add("b");
		modRule.setParameters(params);
		assertTrue(pol.getPolicyRules().contains(modRule));
		modRule = new GuidedRule(
				"(above ?X ?_MOD_b) (clear ?X) => (moveFloor ?X)", queryParams);
		modRule.setParameters(params);
		assertTrue(pol.getPolicyRules().contains(modRule));
		// On rule
		queryParams = new ArrayList<String>();
		queryParams.add("?_MOD_a");
		queryParams.add("?_MOD_b");
		modRule = new GuidedRule(
				"(clear ?_MOD_a) (clear ?_MOD_b) => (move ?_MOD_a ?_MOD_b)",
				queryParams);
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
