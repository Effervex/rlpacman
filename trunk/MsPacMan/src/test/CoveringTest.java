package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import jess.Rete;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import relationalFramework.Covering;
import relationalFramework.GuidedRule;
import relationalFramework.MultiMap;
import relationalFramework.StateSpec;

public class CoveringTest {
	private Covering sut_;

	@Before
	public void setUp() throws Exception {
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
		sut_ = new Covering();
		StateSpec.initInstance("blocksWorld.BlocksWorld");
	}

	@Test
	public void testCoverState() throws Exception {
		Rete state = StateSpec.getInstance().getRete();
		state.eval("(assert (clear d))");
		state.eval("(assert (clear e))");
		state.eval("(assert (clear c))");
		state.eval("(assert (highest e))");
		state.eval("(assert (on e b))");
		state.eval("(assert (on b f))");
		state.eval("(assert (on d a))");
		state.eval("(assert (above e b))");
		state.eval("(assert (above e f))");
		state.eval("(assert (above b f))");
		state.eval("(assert (above d a))");
		state.eval("(assert (onFloor c))");
		state.eval("(assert (onFloor a))");
		state.eval("(assert (onFloor f))");
		StateSpec.getInstance().insertValidActions(state);

		List<GuidedRule> rules = sut_.coverState(state,
				new MultiMap<String, GuidedRule>(), true);
		assertEquals(rules.size(), 2);
		for (GuidedRule gr : rules) {
			System.out.println(gr);
			if (gr.getAction().contains("moveFloor")) {
				int condCount = StateSpec.getInstance().encodeRule(
						gr.getStringConditions()).replaceAll("\\(.+?\\)( |$)", ".")
						.length();
				assertEquals(3, condCount);
				assertFalse(gr.getConditions().contains("(highest ?X)"));
				assertTrue(gr.getConditions().contains("(clear ?X)"));
				assertTrue(gr.getConditions().contains("(on ?X ?)"));
				assertTrue(gr.getConditions().contains("(above ?X ?)"));
				assertFalse(gr.getConditions().contains("(onFloor ?X)"));
				assertEquals("(moveFloor ?X)", gr.getAction());
			} else {
				int condCount = StateSpec.getInstance().encodeRule(
						gr.getStringConditions()).replaceAll("\\(.+?\\)( |$)", ".")
						.length();
				assertEquals(2, condCount);
				assertTrue(gr.getConditions().contains("(clear ?X)"));
				assertTrue(gr.getConditions().contains("(clear ?Y)"));
				assertFalse(gr.getConditions().contains("(on ?X ?)"));
				assertFalse(gr.getConditions().contains("(above ?X ?)"));
				assertFalse(gr.getConditions().contains("(highest ?X)"));
				assertFalse(gr.getConditions().contains("(on ?Y ?)"));
				assertFalse(gr.getConditions().contains("(above ?Y ?)"));
				assertFalse(gr.getConditions().contains("(highest ?Y)"));
				assertEquals("(move ?X ?Y)", gr.getAction());
			}
		}

		// A flat state with previous rules
		state.reset();
		state.eval("(assert (onFloor a))");
		state.eval("(assert (onFloor b))");
		state.eval("(assert (onFloor c))");
		state.eval("(assert (onFloor d))");
		state.eval("(assert (onFloor e))");
		state.eval("(assert (onFloor f))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (block f))");
		state.eval("(assert (clear a))");
		state.eval("(assert (clear b))");
		state.eval("(assert (clear c))");
		state.eval("(assert (clear d))");
		state.eval("(assert (clear e))");
		state.eval("(assert (clear f))");
		state.eval("(assert (highest a))");
		state.eval("(assert (highest b))");
		state.eval("(assert (highest c))");
		state.eval("(assert (highest d))");
		state.eval("(assert (highest e))");
		state.eval("(assert (highest f))");
		StateSpec.getInstance().insertValidActions(state);

		MultiMap<String, GuidedRule> existingRules = new MultiMap<String, GuidedRule>();
		existingRules.put("move", new GuidedRule(
				"(on ?X ?) (above ?X ?) (clear ?X) (on ?Y ?) "
						+ "(above ?Y ?) (clear ?Y) (test (<> ?X ?Y)) "
						+ "(block ?X) (block ?Y) => (move ?X ?Y)"));
		existingRules.put("moveFloor", new GuidedRule(
				"(on ?X ?) (above ?X ?) (clear ?X) "
						+ "(highest ?X) (block ?X) => (moveFloor ?X)"));

		rules = sut_.coverState(state, existingRules, true);
		assertEquals(rules.size(), 1);
		assertEquals(existingRules.sizeTotal(), 2);
		for (GuidedRule gr : existingRules.values()) {
			System.out.println(gr);
			if (gr.getAction().contains("moveFloor")) {
				int condCount = StateSpec.getInstance().encodeRule(
						gr.getStringConditions()).replaceAll("\\(.+?\\)( |$)", ".")
						.length();
				assertEquals(4, condCount);
				assertTrue(gr.getConditions().contains("(highest ?X)"));
				assertTrue(gr.getConditions().contains("(clear ?X)"));
				assertTrue(gr.getConditions().contains("(on ?X ?)"));
				assertTrue(gr.getConditions().contains("(above ?X ?)"));
				assertFalse(gr.getConditions().contains("(onFloor ?X)"));
				assertEquals("(moveFloor ?X)", gr.getAction());
			} else {
				int condCount = StateSpec.getInstance().encodeRule(
						gr.getStringConditions()).replaceAll("\\(.+?\\)( |$)", ".")
						.length();
				assertEquals(2, condCount);
				assertTrue(gr.getConditions().contains("(clear ?X)"));
				assertTrue(gr.getConditions().contains("(clear ?Y)"));
				assertFalse(gr.getConditions().contains("(on ?X ?)"));
				assertFalse(gr.getConditions().contains("(above ?X ?)"));
				assertFalse(gr.getConditions().contains("(highest ?X)"));
				assertFalse(gr.getConditions().contains("(on ?Y ?)"));
				assertFalse(gr.getConditions().contains("(above ?Y ?)"));
				assertFalse(gr.getConditions().contains("(highest ?Y)"));
				assertEquals("(move ?X ?Y)", gr.getAction());
			}
		}
		
		// Test the rule refinement flag.
		rules = sut_.coverState(state, new MultiMap<String, GuidedRule>(), false);
		assertTrue(rules.isEmpty());
	}

//	@Test
//	public void testSpecialiseRule() {
//		fail("Not yet implemented");
//	}

	@Test
	public void testFormatAction() {
		// Simple action
		String actionPred = "moveFloor";
		assertEquals("(moveFloor ?X)", sut_.formatAction(actionPred));

		// Larger action
		String movePred = "move";
		assertEquals("(move ?X ?Y)", sut_.formatAction(movePred));
	}
}
