package test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import relationalFramework.Covering;
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
		params.add("?*a*");
		assertEquals(params, result.getParameterTerms());
		ArrayList<GuidedRule> rules = new ArrayList<GuidedRule>();
		rules.add(new GuidedRule(
				"(above ?X ?*a*) (clear ?X) => (moveFloor ?X)", params));
		rules
				.add(new GuidedRule(
						"(above ?X ?*a*) (clear ?X) (clear ?Y) => (move ?X ?Y)",
						params));
		assertEquals(rules, result.getModuleRules());

		// Null module
		result = Module.loadModule("blocksWorld", "magicWin");
		assertNull(result);
	}

}
