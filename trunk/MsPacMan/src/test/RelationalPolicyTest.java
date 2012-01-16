package test;

import static org.junit.Assert.*;

import relationalFramework.CoveringRelationalPolicy;
import relationalFramework.GoalCondition;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.LocalAgentObservations;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.junit.Before;
import org.junit.Test;

import cerrla.Module;
import cerrla.PolicyGenerator;


public class RelationalPolicyTest {
	private PolicyGenerator policyGenerator_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
		LocalAgentObservations
				.loadAgentObservations(new GoalCondition("on$A$B"));
	}

	@Test
	public void testAddRule() {
		// Basic rule adding

		CoveringRelationalPolicy pol = new CoveringRelationalPolicy(
				policyGenerator_);
		RelationalRule rule = new RelationalRule("(clear ?X) => (moveFloor ?X)");
		pol.addRule(rule);
		assertEquals(pol.getPolicyRules().size(), 1);
		assertTrue(pol.getPolicyRules().contains(rule));
		assertTrue(pol.getFiringRules().isEmpty());

		// Rule adding with modular check (but no module defined)
		pol = new CoveringRelationalPolicy(policyGenerator_);
		rule = new RelationalRule("(clear ?X) => (moveFloor ?X)");
		pol.addRule(rule);
		assertEquals(pol.getPolicyRules().size(), 1);
		assertTrue(pol.getPolicyRules().contains(rule));

		// Check the clear module exists
		if (Module.loadModule("blocksWorld", "clear") == null)
			fail("'clear' module doesn't exist!");
		int clearRulesNum = Module.loadModule("blocksWorld", "clear")
				.getModuleRules().size();

		// Rule adding with modular check (module fires)
		pol = new CoveringRelationalPolicy(policyGenerator_);
		rule = new RelationalRule("(clear a) => (moveFloor a)");
		pol.addRule(rule);

		assertEquals(pol.getPolicyRules().size(), clearRulesNum + 1);
		assertTrue(pol.getPolicyRules().contains(rule));
		List<String> queryParams = new ArrayList<String>();
		queryParams.add("?G_0");
		RelationalRule modRule = new RelationalRule(
				"(above ?X ?G_0) (clear ?X) => (moveFloor ?X)", queryParams);
		BidiMap params = new DualHashBidiMap();
		params.put("a", "?G_0");
		modRule.setParameters(params);
		assertTrue(pol.getPolicyRules().contains(modRule));

		if (Module.loadModule("blocksWorld", "clear&clear") == null)
			fail("'clear&clear' module doesn't exist!");

		// Rule adding with multiple modular check
		pol = new CoveringRelationalPolicy(policyGenerator_);
		rule = new RelationalRule("(clear a) (clear b) => (moveFloor a)");
		pol.addRule(rule);
		assertEquals(pol.getPolicyRules().size(), clearRulesNum * 2 + 1);
		assertTrue(pol.getPolicyRules().contains(rule));
		int i = 0;
		for (RelationalRule gr : pol.getPolicyRules()) {
			if (i < clearRulesNum) {
				assertTrue(gr.getParameters().size() == 2);
				assertTrue(gr.getParameters().contains("a"));
			} else if (i < (clearRulesNum * 2)) {
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
		pol = new CoveringRelationalPolicy(policyGenerator_);
		rule = new RelationalRule("(on a b) => (moveFloor a)");
		pol.addRule(rule);
		assertEquals(pol.getPolicyRules().size(), 6);
		assertTrue(pol.getPolicyRules().contains(rule));
		// Clear rules
		queryParams = new ArrayList<String>();
		queryParams.add("?G_0");
		queryParams.add("?G_1");
		modRule = new RelationalRule(
				"(above ?X ?G_0) (clear ?X) => (moveFloor ?X)", queryParams);
		params.clear();
		params.put("a", "?G_0");
		params.put("b", "?G_1");
		modRule.setParameters(params);
		assertTrue(pol.getPolicyRules().contains(modRule));
		modRule = new RelationalRule(
				"(above ?X ?G_1) (clear ?X) => (moveFloor ?X)", queryParams);
		modRule.setParameters(params);
		assertTrue(pol.getPolicyRules().contains(modRule));
		// On rule
		queryParams = new ArrayList<String>();
		queryParams.add("?G_0");
		queryParams.add("?G_1");
		modRule = new RelationalRule(
				"(clear ?G_0) (clear ?G_1) => (move ?G_0 ?G_1)",
				queryParams);
		params.clear();
		params.put("a", "?G_0");
		params.put("b", "?G_1");
		modRule.setParameters(params);
		assertTrue(pol.getPolicyRules().contains(modRule));
	}

}
