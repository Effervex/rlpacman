package test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.GuidedRule;
import relationalFramework.Module;
import relationalFramework.StateSpec;

public class ModuleTest {
	
	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
	}

	@Test
	public void testLoadModule() {
		Module result = Module.loadModule("blocksWorld", "clear");
		assertNotNull(result);
		assertEquals("clear", result.getModulePredicate());
		ArrayList<String> params = new ArrayList<String>();
		params.add("?_MOD_a");
		assertEquals(params, result.getParameterTerms());
		ArrayList<GuidedRule> rules = new ArrayList<GuidedRule>();
		rules.add(new GuidedRule(
				"(above ?X ?_MOD_a) (clear ?X) => (moveFloor ?X)", params));
		assertEquals(rules, result.getModuleRules());

		// Null module
		result = Module.loadModule("blocksWorld", "magicWin");
		assertNull(result);
	}

	@Test
	public void testShiftModularVariables() {
		Module result = Module.loadModule("blocksWorld", "clear");
		assertNotNull(result);
		for (GuidedRule modRule : result.getModuleRules()) {
			modRule.shiftModularVariables(1);
		}
	}
}
