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
		
		List<GuidedRule> rules = sut_.coverState(state);
		assertEquals(rules.size(), 2);
		for (GuidedRule gr : rules) {
			System.out.println(gr);
			if (gr.getAction().contains("moveFloor")) {
				int condCount = gr.getConditions().replaceAll("\\(.+?\\)( |$)", ".").length();
				assertEquals(3, condCount);
				assertFalse(gr.getConditions().contains("(highest ?X)"));
				assertTrue(gr.getConditions().contains("(clear ?X)"));
				assertTrue(gr.getConditions().contains("(on ?X _)"));
				assertTrue(gr.getConditions().contains("(above ?X _)"));
				assertFalse(gr.getConditions().contains("(onFloor ?X)"));
				assertEquals("(moveFloor ?X)", gr.getAction());
			} else {
				int condCount = gr.getConditions().replaceAll("\\(.+?\\)( |$)", ".").length();
				assertEquals(2, condCount);
				assertTrue(gr.getConditions().contains("(clear ?X)"));
				assertTrue(gr.getConditions().contains("(clear ?Y)"));
				assertFalse(gr.getConditions().contains("(on ?X _)"));
				assertFalse(gr.getConditions().contains("(above ?X _)"));
				assertFalse(gr.getConditions().contains("(highest ?X)"));
				assertFalse(gr.getConditions().contains("(on ?Y _)"));
				assertFalse(gr.getConditions().contains("(above ?Y _)"));
				assertFalse(gr.getConditions().contains("(highest ?Y)"));
				assertEquals("(move ?X ?Y)", gr.getAction());
			}
		}
	}

	@Test
	public void testSpecialiseRule() {
		fail("Not yet implemented");
	}

	@Test
	public void testJoinRule() {
		// Simple action
		Collection<String> conditions = new ArrayList<String>();
		conditions.add("(clear ?X)");
		String actionString = "(moveFloor ?X)";
		assertEquals("(clear ?X) => (moveFloor ?X)", sut_.joinRule(conditions,
				actionString));

		// Two conditions
		conditions.add("(highest ?X)");
		assertEquals("(clear ?X) (highest ?X) => (moveFloor ?X)", sut_
				.joinRule(conditions, actionString));

		// Larger action and conditions
		conditions.clear();
		conditions.add("(on ?X _)");
		conditions.add("(clear ?X)");
		conditions.add("(on ?Y _)");
		conditions.add("(clear ?Y)");
		actionString = "(move ?X ?Y)";
		assertEquals(
				"(on ?X _) (clear ?X) (on ?Y _) (clear ?Y) => (move ?X ?Y)",
				sut_.joinRule(conditions, actionString));
	}

	@Test
	public void testFormatAction() {
		// Simple action
		Class[] types = { Object.class };
		String actionPred = "moveFloor";
		Collection<String> actionTerms = new HashSet<String>();
		assertEquals("(moveFloor ?X)", sut_.formatAction(actionPred, actionTerms));
		assertTrue(actionTerms.contains("?X"));

		// Larger action
		Class[] moveTypes = { Object.class, Object.class };
		String movePred = "move";
		actionTerms.clear();
		assertEquals("(move ?X ?Y)", sut_.formatAction(movePred, actionTerms));
		assertTrue(actionTerms.contains("?X"));
		assertTrue(actionTerms.contains("?Y"));
	}
	
	@Test
	public void testIsMinimal() {
		// Simple minimal case
		Collection<String> conditions = new ArrayList<String>();
		conditions.add("(clear ?X)");
		Collection<String> actionTerms = new HashSet<String>();
		actionTerms.add("?X");
		assertTrue(sut_.isMinimal(conditions, actionTerms));
		
		// Simple not minimal
		conditions.add("(highest ?X)");
		assertFalse(sut_.isMinimal(conditions, actionTerms));
		
		// Minimal case
		conditions.clear();
		conditions.add("(clear ?X)");
		conditions.add("(clear ?Y)");
		actionTerms.add("?Y");
		assertTrue(sut_.isMinimal(conditions, actionTerms));
		
		// Minimal extended case
		conditions.clear();
		conditions.add("(clear ?X)");
		conditions.add("(on ?Y _)");
		assertTrue(sut_.isMinimal(conditions, actionTerms));
		
		// Minimal compacted case
		conditions.clear();
		conditions.add("(on ?Y ?X)");
		assertTrue(sut_.isMinimal(conditions, actionTerms));
		
		// False case
		conditions.add("(on ?X _)");
		assertFalse(sut_.isMinimal(conditions, actionTerms));
		
		// Complex case
		conditions.clear();
		conditions.add("(clear ?X)");
		conditions.add("(above ?X ?Y)");
		conditions.add("(on ?B ?Z)");
		conditions.add("(on ?A _)");
		conditions.add("(highest ?C)");
		actionTerms.add("?Z");
		actionTerms.add("?A");
		actionTerms.add("?B");
		actionTerms.add("?C");
		assertTrue(sut_.isMinimal(conditions, actionTerms));
	}
}
