package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import jess.Fact;
import jess.Rete;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import relationalFramework.ActionChoice;
import relationalFramework.ConditionComparator;
import relationalFramework.Covering;
import relationalFramework.GuidedRule;
import relationalFramework.MultiMap;
import relationalFramework.Policy;
import relationalFramework.RuleAction;
import relationalFramework.StateSpec;

public class CoveringTest {
	private Covering sut_;

	@Before
	public void setUp() throws Exception {
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
		sut_ = new Covering(2);
		StateSpec.initInstance("blocksWorld.BlocksWorld");
	}

	@Test
	public void testCoverState() throws Exception {
		// No constants
		List<String> constants = new ArrayList<String>();
		StateSpec.getInstance().setConstants(constants);

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
		MultiMap<String, String> validActions = StateSpec.getInstance()
				.generateValidActions(state);

		List<GuidedRule> rules = sut_.coverState(state, validActions,
				new MultiMap<String, GuidedRule>());
		assertEquals(rules.size(), 2);
		for (GuidedRule gr : rules) {
			System.out.println(gr);
			if (gr.getAction().contains("moveFloor")) {
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(2, condCount);
				assertFalse(gr.getConditions().contains("(highest ?X)"));
				assertTrue(gr.getConditions().contains("(clear ?X)"));
				assertTrue(gr.getConditions().contains("(on ?X ?)"));
				// on implies above
				assertFalse(gr.getConditions().contains("(above ?X ?)"));
				assertFalse(gr.getConditions().contains("(onFloor ?X)"));
				assertEquals("(moveFloor ?X)", gr.getAction());
			} else {
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
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
		validActions = StateSpec.getInstance().generateValidActions(state);

		MultiMap<String, GuidedRule> existingRules = new MultiMap<String, GuidedRule>();
		existingRules.put("move", new GuidedRule(
				"(on ?X ?) (clear ?X) (on ?Y ?) (clear ?Y) (test (<> ?X ?Y)) "
						+ "(block ?X) (block ?Y) => (move ?X ?Y)"));
		existingRules.put("moveFloor", new GuidedRule(
				"(on ?X ?) (highest ?X) (block ?X) => (moveFloor ?X)"));

		rules = sut_.coverState(state, validActions, existingRules);
		assertEquals(rules.size(), 1);
		assertEquals(existingRules.sizeTotal(), 2);
		for (GuidedRule gr : existingRules.values()) {
			System.out.println(gr);
			if (gr.getAction().contains("moveFloor")) {
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(2, condCount);
				assertTrue(gr.getConditions().contains("(highest ?X)"));
				assertFalse(gr.getConditions().contains("(clear ?X)"));
				assertTrue(gr.getConditions().contains("(on ?X ?)"));
				assertFalse(gr.getConditions().contains("(above ?X ?)"));
				assertFalse(gr.getConditions().contains("(onFloor ?X)"));
				assertEquals("(moveFloor ?X)", gr.getAction());
			} else {
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
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

		// Consecutive covering (w/ constants)
		constants.add("a");
		constants.add("b");

		state.reset();
		state.eval("(assert (on a c))");
		state.eval("(assert (on b a))");
		state.eval("(assert (on d b))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (highest d))");
		state.run();
		validActions = StateSpec.getInstance().generateValidActions(state);

		rules = sut_.coverState(state, validActions,
				new MultiMap<String, GuidedRule>());
		assertEquals(2, rules.size());
		existingRules.clear();
		for (GuidedRule gr : rules) {
			System.out.println(gr.toString());
			if (gr.getAction().contains("moveFloor")) {
				assertTrue(gr.getConditions().contains("(highest d)"));
				assertFalse(gr.getConditions().contains("(clear d)"));
				assertTrue(gr.getConditions().contains("(on d b)"));
				assertFalse(gr.getConditions().contains("(above d b)"));
				assertTrue(gr.getConditions().contains("(above d a)"));
				assertTrue(gr.getConditions().contains("(above d c)"));
				assertTrue(gr.getConditions().contains("(block a)"));
				assertTrue(gr.getConditions().contains("(block b)"));
				assertTrue(gr.getConditions().contains("(block c)"));
				assertEquals("(moveFloor d)", gr.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(7, condCount);
				existingRules.put("moveFloor", gr);
			} else {
				assertTrue(gr.getConditions().contains("(clear ?X)"));
				assertTrue(gr.getConditions().contains("(clear ?Y)"));
				assertFalse(gr.getConditions().contains("(on ?X ?)"));
				assertFalse(gr.getConditions().contains("(on ? b)"));
				assertFalse(gr.getConditions().contains("(above ? b)"));
				assertFalse(gr.getConditions().contains("(above ? a)"));
				assertFalse(gr.getConditions().contains("(above ?X ?)"));
				assertFalse(gr.getConditions().contains("(highest ?X)"));
				assertFalse(gr.getConditions().contains("(on ?Y ?)"));
				assertFalse(gr.getConditions().contains("(above ?Y ?)"));
				assertFalse(gr.getConditions().contains("(highest ?Y)"));
				assertEquals("(move ?X ?Y)", gr.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(2, condCount);
				existingRules.put("move", gr);
			}
		}

		state.reset();
		state.eval("(assert (onFloor c))");
		state.eval("(assert (onFloor e))");
		state.eval("(assert (onFloor d))");
		state.eval("(assert (on a c))");
		state.eval("(assert (on b a))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (highest b))");
		state.run();
		validActions = StateSpec.getInstance().generateValidActions(state);

		rules = sut_.coverState(state, validActions, existingRules);
		assertEquals(2, rules.size());
		assertEquals(2, existingRules.sizeTotal());
		for (GuidedRule gr : existingRules.values()) {
			System.out.println(gr.toString());
			if (gr.getAction().contains("moveFloor")) {
				assertTrue(gr.getConditions().contains("(highest ?X)"));
				assertFalse(gr.getConditions().contains("(clear ?X)"));
				assertTrue(gr.getConditions().contains("(on ?X ?)"));
				assertFalse(gr.getConditions().contains("(on ?X b)"));
				assertFalse(gr.getConditions().contains("(above ?X b)"));
				assertTrue(gr.getConditions().contains("(above ?X a)"));
				assertTrue(gr.getConditions().contains("(above ?X c)"));
				assertFalse(gr.getConditions().contains("(above ?X ?)"));
				assertTrue(gr.getConditions().contains("(block a)"));
				assertTrue(gr.getConditions().contains("(block c)"));
				assertEquals("(moveFloor ?X)", gr.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(6, condCount);
			} else {
				assertTrue(gr.getConditions().contains("(clear ?X)"));
				assertTrue(gr.getConditions().contains("(clear ?Y)"));
				assertFalse(gr.getConditions().contains("(on ?X ?)"));
				assertFalse(gr.getConditions().contains("(on ? b)"));
				assertFalse(gr.getConditions().contains("(above ? b)"));
				assertFalse(gr.getConditions().contains("(above ? a)"));
				assertFalse(gr.getConditions().contains("(above ?X ?)"));
				assertFalse(gr.getConditions().contains("(highest ?X)"));
				assertFalse(gr.getConditions().contains("(on ?Y ?)"));
				assertFalse(gr.getConditions().contains("(above ?Y ?)"));
				assertFalse(gr.getConditions().contains("(highest ?Y)"));
				assertEquals("(move ?X ?Y)", gr.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(2, condCount);
			}
		}

		state.reset();
		state.eval("(assert (onFloor c))");
		state.eval("(assert (onFloor e))");
		state.eval("(assert (onFloor d))");
		state.eval("(assert (on a d))");
		state.eval("(assert (on b a))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (highest b))");
		state.run();
		validActions = StateSpec.getInstance().generateValidActions(state);

		rules = sut_.coverState(state, validActions, existingRules);
		assertEquals(2, rules.size());
		assertEquals(2, existingRules.sizeTotal());
		for (GuidedRule gr : existingRules.values()) {
			System.out.println(gr.toString());
			if (gr.getAction().contains("moveFloor")) {
				assertTrue(gr.getConditions().contains("(highest ?X)"));
				assertFalse(gr.getConditions().contains("(clear ?X)"));
				assertTrue(gr.getConditions().contains("(on ?X ?)"));
				assertFalse(gr.getConditions().contains("(on ?X b)"));
				assertFalse(gr.getConditions().contains("(above ?X b)"));
				assertTrue(gr.getConditions().contains("(above ?X a)"));
				assertFalse(gr.getConditions().contains("(above ?X ?)"));
				assertTrue(gr.getConditions().contains("(block a)"));
				assertEquals("(moveFloor ?X)", gr.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(4, condCount);
			} else {
				assertTrue(gr.getConditions().contains("(clear ?X)"));
				assertTrue(gr.getConditions().contains("(clear ?Y)"));
				assertFalse(gr.getConditions().contains("(on ?X ?)"));
				assertFalse(gr.getConditions().contains("(on ? b)"));
				assertFalse(gr.getConditions().contains("(above ? b)"));
				assertFalse(gr.getConditions().contains("(above ? a)"));
				assertFalse(gr.getConditions().contains("(above ?X ?)"));
				assertFalse(gr.getConditions().contains("(highest ?X)"));
				assertFalse(gr.getConditions().contains("(on ?Y ?)"));
				assertFalse(gr.getConditions().contains("(above ?Y ?)"));
				assertFalse(gr.getConditions().contains("(highest ?Y)"));
				assertEquals("(move ?X ?Y)", gr.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(2, condCount);
			}
		}

		// Constants covering
		existingRules.clear();
		state.reset();
		state.eval("(assert (onFloor c))");
		state.eval("(assert (onFloor e))");
		state.eval("(assert (on a c))");
		state.eval("(assert (on d a))");
		state.eval("(assert (on b d))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (highest b))");
		state.run();
		validActions = StateSpec.getInstance().generateValidActions(state);

		rules = sut_.coverState(state, validActions,
				new MultiMap<String, GuidedRule>());
		assertEquals(2, rules.size());
		for (GuidedRule gr : rules) {
			System.out.println(gr.toString());
			if (gr.getAction().contains("moveFloor")) {
				assertTrue(gr.getConditions().contains("(highest b)"));
				assertFalse(gr.getConditions().contains("(clear b)"));
				assertTrue(gr.getConditions().contains("(on b d)"));
				assertTrue(gr.getConditions().contains("(above b a)"));
				assertFalse(gr.getConditions().contains("(above b d)"));
				assertTrue(gr.getConditions().contains("(above b c)"));
				assertTrue(gr.getConditions().contains("(block a)"));
				assertTrue(gr.getConditions().contains("(block b)"));
				assertTrue(gr.getConditions().contains("(block c)"));
				assertTrue(gr.getConditions().contains("(block d)"));
				assertEquals("(moveFloor b)", gr.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(7, condCount);
				existingRules.put("moveFloor", gr);
			} else {
				assertTrue(gr.getConditions().contains("(clear ?X)"));
				assertTrue(gr.getConditions().contains("(clear ?Y)"));
				assertFalse(gr.getConditions().contains("(on ?X ?)"));
				assertFalse(gr.getConditions().contains("(above ?X ?)"));
				assertFalse(gr.getConditions().contains("(highest ?X)"));
				assertFalse(gr.getConditions().contains("(on ?Y ?)"));
				assertFalse(gr.getConditions().contains("(above ?Y ?)"));
				assertFalse(gr.getConditions().contains("(highest ?Y)"));
				assertEquals("(move ?X ?Y)", gr.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(2, condCount);
				existingRules.put("move", gr);
			}
		}

		state.reset();
		state.eval("(assert (onFloor g))");
		state.eval("(assert (on f g))");
		state.eval("(assert (on e f))");
		state.eval("(assert (on b e))");
		state.eval("(assert (block b))");
		state.eval("(assert (block e))");
		state.eval("(assert (block f))");
		state.eval("(assert (block g))");
		state.eval("(assert (highest b))");
		state.run();
		validActions = StateSpec.getInstance().generateValidActions(state);

		rules = sut_.coverState(state, validActions, existingRules);
		assertEquals(1, rules.size());
		assertEquals(2, existingRules.sizeTotal());
		for (GuidedRule gr : rules) {
			System.out.println(gr.toString());
			if (gr.getAction().contains("moveFloor")) {
				assertTrue(gr.getConditions().contains("(highest b)"));
				assertFalse(gr.getConditions().contains("(clear b)"));
				assertTrue(gr.getConditions().contains("(on b ?)"));
				assertFalse(gr.getConditions().contains("(above b ?)"));
				assertTrue(gr.getConditions().contains("(block b)"));
				assertEquals("(moveFloor b)", gr.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(2, condCount);
			}
		}
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testInverselySubstitute() throws Exception {
		// Ensuring we have no constants
		List<String> constants = new ArrayList<String>();

		// Basic substitution
		Rete state = StateSpec.getInstance().getRete();
		state.eval("(assert (clear a))");
		Collection<Fact> facts = StateSpec.extractFacts(state);
		String[] actionTerms = { "a" };
		Collection<String> result = sut_.inverselySubstitute(facts,
				actionTerms, constants);
		assertEquals(result.size(), 1);
		assertTrue(result.contains("(clear ?X)"));

		// Substitution of an unknown variable
		actionTerms[0] = "b";
		result = sut_.inverselySubstitute(facts, actionTerms, constants);
		assertEquals(result.size(), 1);
		assertTrue(result.contains("(clear ?)"));

		// Only return one anonymous fact if there is more than one
		state.eval("(assert (clear b))");
		state.eval("(assert (clear c))");
		facts = StateSpec.extractFacts(state);
		actionTerms[0] = "a";
		result = sut_.inverselySubstitute(facts, actionTerms, constants);
		assertEquals(result.size(), 2);
		assertTrue(result.contains("(clear ?X)"));
		assertTrue(result.contains("(clear ?)"));

		// Swap over many facts
		state.eval("(assert (on a d))");
		state.eval("(assert (on b e))");
		state.eval("(assert (onFloor c))");
		facts = StateSpec.extractFacts(state);
		actionTerms[0] = "a";
		result = sut_.inverselySubstitute(facts, actionTerms, constants);
		assertEquals(result.size(), 6);
		assertTrue(result.contains("(clear ?X)"));
		assertTrue(result.contains("(clear ?)"));
		assertTrue(result.contains("(on ?X d)"));
		assertTrue(result.contains("(block d)"));
		assertTrue(result.contains("(on ? ?)"));
		assertTrue(result.contains("(onFloor ?)"));

		// Multiple action terms
		actionTerms = new String[2];
		actionTerms[0] = "a";
		actionTerms[1] = "c";
		result = sut_.inverselySubstitute(facts, actionTerms, constants);
		assertEquals(result.size(), 7);
		assertTrue(result.contains("(clear ?X)"));
		assertTrue(result.contains("(clear ?Y)"));
		assertTrue(result.contains("(clear ?)"));
		assertTrue(result.contains("(on ?X d)"));
		assertTrue(result.contains("(block d)"));
		assertTrue(result.contains("(on ? ?)"));
		assertTrue(result.contains("(onFloor ?Y)"));

		// Testing constants
		constants.add("a");
		actionTerms = new String[1];
		actionTerms[0] = "a";
		result = sut_.inverselySubstitute(facts, actionTerms, constants);
		assertEquals(result.size(), 7);
		assertTrue(result.contains("(clear a)"));
		assertTrue(result.contains("(block a)"));
		assertTrue(result.contains("(clear ?)"));
		assertTrue(result.contains("(on a d)"));
		assertTrue(result.contains("(block d)"));
		assertTrue(result.contains("(on ? ?)"));
		assertTrue(result.contains("(onFloor ?)"));

		actionTerms[0] = "b";
		result = sut_.inverselySubstitute(facts, actionTerms, constants);
		assertEquals(result.size(), 9);
		assertTrue(result.contains("(clear a)"));
		assertTrue(result.contains("(block a)"));
		assertTrue(result.contains("(clear ?X)"));
		assertTrue(result.contains("(clear ?)"));
		assertTrue(result.contains("(on a d)"));
		assertTrue(result.contains("(block d)"));
		assertTrue(result.contains("(on ?X e)"));
		assertTrue(result.contains("(block e)"));
		assertTrue(result.contains("(onFloor ?)"));

		// Numerical values (Same dist)
		state.reset();
		state.eval("(assert (distance player dotA 3))");
		state.eval("(assert (dot dotA))");
		state.eval("(assert (distance player blinky 3))");
		state.eval("(assert (ghost blinky))");
		constants.clear();
		constants.add("dotA");
		constants.add("3");
		actionTerms = new String[2];
		actionTerms[0] = "dotA";
		actionTerms[1] = "3";
		facts = StateSpec.extractFacts(state);
		result = sut_.inverselySubstitute(facts, actionTerms, constants);
		assertTrue(result.contains("(initial-fact)"));
		assertTrue(result.contains("(distance player dotA 3)"));
		assertTrue(result.contains("(dot dotA)"));
		assertTrue(result.contains("(distance ? ? ?)"));
		assertTrue(result.contains("(ghost ?)"));
		assertEquals(result.size(), 5);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testFormPreGoalState() throws Exception {
		// Ensuring we have no constants
		List<String> constants = new ArrayList<String>();
		StateSpec.getInstance().setConstants(constants);

		// Basic covering
		Rete state = StateSpec.getInstance().getRete();
		state.eval("(assert (clear a))");
		Collection<Fact> facts = StateSpec.extractFacts(state);
		ActionChoice ac = new ActionChoice();
		List<String> actions = new ArrayList<String>();
		actions.add("(moveFloor a)");
		Policy policy = new Policy();
		RuleAction ra = new RuleAction(new GuidedRule(
				"(clear a) => (moveFloor a)"), actions, policy);
		ra.getTriggerActions();
		ac.switchOn(ra);
		sut_
				.formPreGoalState(facts, ac, StateSpec.getInstance()
						.getConstants());
		List<String> preGoal = sut_.getPreGoalState("moveFloor");
		assertEquals(preGoal.size(), 2);
		assertTrue(preGoal.contains("(clear a)"));
		assertTrue(preGoal.contains("(block a)"));

		// Generalisation (same predicate and action)
		state.reset();
		state.eval("(assert (clear b))");
		facts = StateSpec.extractFacts(state);
		ac = new ActionChoice();
		actions.clear();
		actions.add("(moveFloor b)");
		ra = new RuleAction(new GuidedRule("(clear b) => (moveFloor b)"),
				actions, policy);
		ra.getTriggerActions();
		ac.switchOn(ra);
		sut_
				.formPreGoalState(facts, ac, StateSpec.getInstance()
						.getConstants());
		preGoal = sut_.getPreGoalState("moveFloor");
		assertEquals(preGoal.size(), 2);
		assertTrue(preGoal.contains("(clear ?X)"));
		assertTrue(preGoal.contains("(block ?X)"));

		// Generalising test (different predicates, same action)
		sut_.clearPreGoalState(2);
		state.reset();
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (on a d))");
		state.eval("(assert (on d c))");
		state.eval("(assert (on b e))");
		state.eval("(assert (onFloor e))");
		state.eval("(assert (onFloor c))");
		state.eval("(assert (highest a))");
		state.run();
		StateSpec.getInstance().generateValidActions(state);
		facts = StateSpec.extractFacts(state);
		ac = new ActionChoice();
		actions.clear();
		actions.add("(move a b)");
		ra = new RuleAction(
				new GuidedRule("(clear a) (clear b) => (move a b)"), actions,
				policy);
		ra.getTriggerActions();
		ac.switchOn(ra);
		sut_
				.formPreGoalState(facts, ac, StateSpec.getInstance()
						.getConstants());
		preGoal = sut_.getPreGoalState("move");
		// Contains the defined preds, above and clear preds
		assertEquals(13, preGoal.size());
		assertTrue(preGoal.contains("(clear a)"));
		assertTrue(preGoal.contains("(clear b)"));
		assertTrue(preGoal.contains("(block a)"));
		assertTrue(preGoal.contains("(block b)"));
		assertTrue(preGoal.contains("(block c)"));
		assertTrue(preGoal.contains("(block d)"));
		assertTrue(preGoal.contains("(block e)"));
		assertTrue(preGoal.contains("(on a d)"));
		assertTrue(preGoal.contains("(on b e)"));
		assertTrue(preGoal.contains("(highest a)"));
		assertTrue(preGoal.contains("(above a d)"));
		assertTrue(preGoal.contains("(above a c)"));
		assertTrue(preGoal.contains("(above b e)"));

		state.reset();
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (onFloor a))");
		state.eval("(assert (onFloor d))");
		state.eval("(assert (on b f))");
		state.eval("(assert (onFloor e))");
		state.eval("(assert (onFloor c))");
		state.eval("(assert (highest b))");
		state.run();
		StateSpec.getInstance().generateValidActions(state);
		facts = StateSpec.extractFacts(state);
		ac = new ActionChoice();
		ra = new RuleAction(
				new GuidedRule("(clear a) (clear b) => (move a b)"), actions,
				policy);
		ra.getTriggerActions();
		ac.switchOn(ra);
		sut_
				.formPreGoalState(facts, ac, StateSpec.getInstance()
						.getConstants());
		preGoal = sut_.getPreGoalState("move");
		// Contains less than the defined preds, above and clear preds
		assertEquals(6, preGoal.size());
		assertTrue(preGoal.contains("(clear a)"));
		assertTrue(preGoal.contains("(clear b)"));
		assertTrue(preGoal.contains("(block a)"));
		assertTrue(preGoal.contains("(block b)"));
		assertTrue(preGoal.contains("(on b ?)"));
		assertTrue(preGoal.contains("(above b ?)"));

		// Generalising the action
		ac = new ActionChoice();
		actions.clear();
		actions.add("(move b a)");
		ra = new RuleAction(
				new GuidedRule("(clear a) (clear b) => (move b a)"), actions,
				policy);
		ra.getTriggerActions();
		ac.switchOn(ra);
		sut_
				.formPreGoalState(facts, ac, StateSpec.getInstance()
						.getConstants());
		preGoal = sut_.getPreGoalState("move");
		// Contains less than the defined preds, above and clear preds
		assertEquals(4, preGoal.size());
		assertTrue(preGoal.contains("(clear ?X)"));
		assertTrue(preGoal.contains("(clear ?Y)"));
		assertTrue(preGoal.contains("(block ?X)"));
		assertTrue(preGoal.contains("(block ?Y)"));
	}

	/**
	 * 
	 */
	@Test
	public void testUnifyStates() {
		// No change unification
		List<String> oldState = new ArrayList<String>();
		oldState.add("(clear ?X)");
		List<String> newState = new ArrayList<String>();
		newState.add("(clear x)");
		List<String> oldTerms = new ArrayList<String>();
		oldTerms.add("?X");
		List<String> newTerms = new ArrayList<String>();
		newTerms.add("x");
		int result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldTerms.contains("?X"));

		// No change with constants
		oldState.clear();
		oldState.add("(clear a)");
		newState.clear();
		newState.add("(clear a)");
		oldTerms.clear();
		oldTerms.add("a");
		newTerms.clear();
		newTerms.add("a");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear a)"));
		assertTrue(oldTerms.contains("a"));

		// Basic removal of preds unification
		oldState.clear();
		oldState.add("(clear ?X)");
		oldState.add("(on ?X ?)");
		oldState.add("(clear ?)");
		newState.clear();
		newState.add("(on z x)");
		newState.add("(highest a)");
		newState.add("(clear x)");
		oldTerms.clear();
		oldTerms.add("?X");
		newTerms.clear();
		newTerms.add("x");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldTerms.contains("?X"));

		// Simple unification
		oldState.clear();
		oldState.add("(clear ?X)");
		oldState.add("(clear ?Y)");
		oldState.add("(on ?X ?)");
		newState.clear();
		newState.add("(clear y)");
		newState.add("(clear x)");
		oldState.add("(on y z)");
		oldTerms.clear();
		oldTerms.add("?X");
		oldTerms.add("?Y");
		newTerms.clear();
		newTerms.add("x");
		newTerms.add("y");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldState.contains("(clear ?Y)"));
		assertTrue(oldTerms.contains("?X"));
		assertTrue(oldTerms.contains("?Y"));

		// Absorption
		oldState.clear();
		oldState.add("(clear ?X)");
		newState.clear();
		newState.add("(clear a)");
		oldTerms.clear();
		oldTerms.add("?X");
		newTerms.clear();
		newTerms.add("a");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldTerms.contains("?X"));

		// Generalisation
		oldState.clear();
		oldState.add("(clear a)");
		newState.clear();
		newState.add("(clear x)");
		oldTerms.clear();
		oldTerms.add("a");
		newTerms.clear();
		newTerms.add("x");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldTerms.contains("?X"));

		// Mutual generalisation
		oldState.clear();
		oldState.add("(clear a)");
		newState.clear();
		newState.add("(clear b)");
		oldTerms.clear();
		oldTerms.add("a");
		newTerms.clear();
		newTerms.add("b");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldTerms.contains("?X"));

		// Two terms
		oldState.clear();
		oldState.add("(on a b)");
		newState.clear();
		newState.add("(on b a)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("b");
		newTerms.clear();
		newTerms.add("b");
		newTerms.add("a");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on ?X ?Y)"));
		assertTrue(oldTerms.contains("?X"));
		assertTrue(oldTerms.contains("?Y"));

		// Two terms in differing order
		oldState.clear();
		oldState.add("(on a b)");
		newState.clear();
		newState.add("(on a b)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("b");
		newTerms.clear();
		newTerms.add("b");
		newTerms.add("a");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(-1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on a b)"));

		// Two terms with two aligned preds
		oldState.clear();
		oldState.add("(on a b)");
		oldState.add("(clear a)");
		newState.clear();
		newState.add("(on b a)");
		newState.add("(clear b)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("b");
		newTerms.clear();
		newTerms.add("b");
		newTerms.add("a");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains("(on ?X ?Y)"));
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldTerms.contains("?X"));

		// Two terms with two misaligned preds
		oldState.clear();
		oldState.add("(on a b)");
		oldState.add("(clear a)");
		newState.clear();
		newState.add("(on b a)");
		newState.add("(clear a)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("b");
		newTerms.clear();
		newTerms.add("b");
		newTerms.add("a");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on ?X ?Y)"));
		assertTrue(oldTerms.contains("?X"));
		assertTrue(oldTerms.contains("?Y"));

		// Generalisation to anonymous
		oldState.clear();
		oldState.add("(clear ?X)");
		newState.clear();
		newState.add("(clear z)");
		oldTerms.clear();
		oldTerms.add("?X");
		newTerms.clear();
		newTerms.add("x");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(-1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));

		// Constant and variable case
		oldState.clear();
		oldState.add("(on a ?X)");
		newState.clear();
		newState.add("(on b x)");
		oldTerms.clear();
		oldTerms.add("?X");
		newTerms.clear();
		newTerms.add("x");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on ? ?X)"));
		assertTrue(oldTerms.contains("?X"));

		// Tough case
		oldState.clear();
		oldState.add("(on a ?Y)");
		newState.clear();
		newState.add("(on z y)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("?Y");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("y");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on ? ?Y)"));
		assertTrue(oldTerms.contains("a"));
		assertTrue(oldTerms.contains("?Y"));

		// Tough case 2
		oldState.clear();
		oldState.add("(on ?X a)");
		newState.clear();
		newState.add("(on z y)");
		oldTerms.clear();
		oldTerms.add("?X");
		oldTerms.add("a");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("y");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on ? ?Y)"));
		assertTrue(oldTerms.contains("?X"));
		assertTrue(oldTerms.contains("?Y"));

		// Tough case 3
		oldState.clear();
		oldState.add("(on a ?Y)");
		newState.clear();
		newState.add("(on a z)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("?Y");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("y");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on a ?)"));
		assertTrue(oldTerms.contains("a"));
		assertTrue(oldTerms.contains("?Y"));

		// Early generalisation test
		oldState.clear();
		oldState.add("(on a ?Y)");
		newState.clear();
		newState.add("(on a z)");
		newState.add("(on a y)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("?Y");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("y");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on a ?Y)"));
		assertTrue(oldTerms.contains("a"));
		assertTrue(oldTerms.contains("?Y"));

		oldState.clear();
		oldState.add("(on a ?Y)");
		newState.clear();
		newState.add("(on x y)");
		newState.add("(on a y)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("?Y");
		newTerms.clear();
		newTerms.add("x");
		newTerms.add("y");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(on ?X ?Y)"));
		assertTrue(oldTerms.contains("?X"));
		assertTrue(oldTerms.contains("?Y"));

		// Using the same fact for unification
		oldState.clear();
		oldState.add("(on a ?Y)");
		oldState.add("(on ?X ?Y)");
		newState.clear();
		newState.add("(on x y)");
		oldTerms.clear();
		oldTerms.add("?X");
		oldTerms.add("?Y");
		newTerms.clear();
		newTerms.add("x");
		newTerms.add("y");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains("(on ? ?Y)"));
		assertTrue(oldState.contains("(on ?X ?Y)"));
		assertTrue(oldTerms.contains("?X"));
		assertTrue(oldTerms.contains("?Y"));

		// Left with constant predicate
		oldState.clear();
		oldState.add("(on ?X b)");
		oldState.add("(clear ?X)");
		oldState.add("(clear ?Y)");
		newState.clear();
		newState.add("(on y b)");
		newState.add("(clear x)");
		newState.add("(clear y)");
		oldTerms.clear();
		oldTerms.add("?X");
		oldTerms.add("?Y");
		newTerms.clear();
		newTerms.add("x");
		newTerms.add("y");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(3, oldState.size());
		assertTrue(oldState.contains("(on ? b)"));
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldState.contains("(clear ?Y)"));
		assertTrue(oldTerms.contains("?X"));
		assertTrue(oldTerms.contains("?Y"));

		// Un-unifiable
		oldState.clear();
		oldState.add("(clear ?X)");
		newState.clear();
		newState.add("(on a b)");
		oldTerms.clear();
		oldTerms.add("?X");
		newTerms.clear();
		newTerms.add("a");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(-1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));

		// Interesting case
		oldState.clear();
		oldState.add("(on a c)");
		oldState.add("(on c ?)");
		oldState.add("(on ?X d)");
		oldState.add("(onFloor e)");
		oldState.add("(onFloor d)");
		oldState.add("(clear a)");
		oldState.add("(clear ?X)");
		oldState.add("(highest a)");
		newState.clear();
		newState.add("(on b c)");
		newState.add("(on c f)");
		newState.add("(on a e)");
		newState.add("(onFloor d)");
		newState.add("(onFloor f)");
		newState.add("(onFloor e)");
		newState.add("(clear d)");
		newState.add("(clear b)");
		newState.add("(clear a)");
		newState.add("(highest b)");
		oldTerms.clear();
		oldTerms.add("?X");
		oldTerms.add("a");
		newTerms.clear();
		newTerms.add("d");
		newTerms.add("b");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(6, oldState.size());
		assertTrue(oldState.contains("(on ?Y c)"));
		assertTrue(oldState.contains("(on c ?)"));
		assertTrue(oldState.contains("(onFloor e)"));
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldState.contains("(clear ?Y)"));
		assertTrue(oldState.contains("(highest ?Y)"));

		// Action precedence
		oldState.clear();
		oldState.add("(on a c)");
		oldState.add("(on c ?)");
		oldState.add("(on b ?)");
		newState.clear();
		newState.add("(on b c)");
		newState.add("(on c f)");
		newState.add("(on a e)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("b");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("b");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(4, oldState.size());
		assertTrue(oldState.contains("(on a ?)"));
		assertTrue(oldState.contains("(on ? c)"));
		assertTrue(oldState.contains("(on c ?)"));
		assertTrue(oldState.contains("(on b ?)"));

		// Double unification
		oldState.clear();
		oldState.add("(on c e)");
		oldState.add("(on f g)");
		newState.clear();
		newState.add("(on c g)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("b");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("b");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains("(on c ?)"));
		assertTrue(oldState.contains("(on ? g)"));

		// Double unification
		oldState.clear();
		oldState.add("(on c g)");
		newState.clear();
		newState.add("(on c e)");
		newState.add("(on f g)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("b");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("b");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains("(on c ?)"));
		assertTrue(oldState.contains("(on ? g)"));

		// Unifying with an inequality test present
		oldState.clear();
		oldState.add("(clear ?X)");
		oldState.add("(clear ?Y)");
		oldState.add("(test (<> ?X ?Y))");
		newState.clear();
		newState.add("(clear a)");
		newState.add("(clear b)");
		oldTerms.clear();
		oldTerms.add("?X");
		oldTerms.add("?Y");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("b");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldState.contains("(clear ?Y)"));
	}

	@Test
	public void testTermlessUnifyStates() {
		// Basic unification
		List<String> oldState = new ArrayList<String>();
		oldState.add("(clear ?X)");
		List<String> newState = new ArrayList<String>();
		newState.add("(clear ?X)");
		BidiMap replacementMap = new DualHashBidiMap();
		int result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(replacementMap.containsKey("?X"));
		assertEquals(replacementMap.get("?X"), "?X");

		// Negation unification
		oldState.clear();
		oldState.add("(not (clear ?X))");
		newState.clear();
		newState.add("(not (clear ?X))");
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(not (clear ?X))"));
		assertTrue(replacementMap.containsKey("?X"));
		assertEquals(replacementMap.get("?X"), "?X");

		// Substitution unification
		oldState.clear();
		oldState.add("(clear ?X)");
		newState.clear();
		newState.add("(clear ?Y)");
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(replacementMap.containsKey("?Y"));
		assertEquals(replacementMap.get("?Y"), "?X");

		// More complex substitution unification
		oldState.clear();
		oldState.add("(clear ?X)");
		oldState.add("(highest ?X)");
		newState.clear();
		newState.add("(clear ?Y)");
		newState.add("(highest ?Y)");
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(0, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(oldState.contains("(highest ?X)"));
		assertTrue(replacementMap.containsKey("?Y"));
		assertEquals(replacementMap.get("?Y"), "?X");

		// Tricky complex substitution unification (could be either case)
		oldState.clear();
		oldState.add("(clear ?X)");
		oldState.add("(highest ?Y)");
		newState.clear();
		newState.add("(clear ?Y)");
		newState.add("(highest ?Y)");
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(clear ?X)"));
		assertTrue(replacementMap.containsKey("?Y"));
		assertEquals(replacementMap.get("?Y"), "?X");

		// Unifying with a negated condition
		oldState.clear();
		oldState.add("(block ?X)");
		oldState.add("(clear ?X)");
		oldState.add("(not (clear ?Y))");
		newState.clear();
		newState.add("(block ?X)");
		newState.add("(not (clear ?X))");
		newState.add("(clear ?Y)");
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(block ?X)"));

		oldState.clear();
		oldState.add("(block ?X)");
		oldState.add("(clear ?Y)");
		oldState.add("(not (highest ?X))");
		newState.clear();
		newState.add("(block ?X)");
		newState.add("(not (clear ?X))");
		newState.add("(clear ?Y)");
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains("(block ?X)"));
		assertTrue(oldState.contains("(clear ?Y)"));

		oldState.clear();
		oldState.add("(block ?X)");
		oldState.add("(clear ?Y)");
		oldState.add("(not (on ?X ?Y))");
		newState.clear();
		newState.add("(block ?X)");
		newState.add("(not (clear ?X))");
		newState.add("(clear ?Y)");
		replacementMap.clear();
		result = sut_.unifyStates(oldState, newState, replacementMap);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains("(block ?X)"));
		assertTrue(oldState.contains("(clear ?Y)"));
	}

	/**
	 * 
	 */
	@Test
	public void testNumericalUnifyStates() {
		// No change unification
		List<String> oldState = new ArrayList<String>();
		oldState.add("(distance a b 1)");
		List<String> newState = new ArrayList<String>();
		newState.add("(distance a b 1)");
		List<String> oldTerms = new ArrayList<String>();
		oldTerms.add("a");
		oldTerms.add("1");
		List<String> newTerms = new ArrayList<String>();
		newTerms.add("a");
		newTerms.add("1");
		int result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(distance a b 1)"));
		assertTrue(oldTerms.contains("a"));
		assertTrue(oldTerms.contains("1"));
		assertEquals(2, oldTerms.size());

		// Range addition
		oldState.clear();
		oldState.add("(distance a b 1)");
		newState.clear();
		newState.add("(distance a b 2)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("1");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("2");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		int index = 0;
		assertTrue(oldState.contains("(distance a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " 1.0 2.0))"));
		assertTrue(oldTerms.contains("a"));
		assertTrue(oldTerms.contains("?" + Covering.RANGE_VARIABLE_PREFIX
				+ index));
		assertEquals(2, oldTerms.size());

		// Range addition (reversed)
		oldState.clear();
		oldState.add("(distance a b 2)");
		newState.clear();
		newState.add("(distance a b 1)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("2");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("1");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		index++;
		assertTrue(oldState.contains("(distance a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " 1.0 2.0))"));
		assertTrue(oldTerms.contains("a"));
		assertTrue(oldTerms.contains("?" + Covering.RANGE_VARIABLE_PREFIX
				+ index));
		assertEquals(2, oldTerms.size());

		// Negative range addition
		oldState.clear();
		oldState.add("(distance a b -1)");
		newState.clear();
		newState.add("(distance a b 2)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("-1");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("2");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		index++;
		assertTrue(oldState.contains("(distance a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " -1.0 2.0))"));
		assertTrue(oldTerms.contains("a"));
		assertTrue(oldTerms.contains("?" + Covering.RANGE_VARIABLE_PREFIX
				+ index));
		assertEquals(2, oldTerms.size());

		// Tiny value range addition
		oldState.clear();
		oldState.add("(distance a b -1)");
		newState.clear();
		newState.add("(distance a b 2.567483E-64)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("-1");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("2.567483E-64");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		index++;
		assertTrue(oldState.contains("(distance a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index
				+ " -1.0 2.567483E-64))"));
		assertTrue(oldTerms.contains("a"));
		assertTrue(oldTerms.contains("?" + Covering.RANGE_VARIABLE_PREFIX
				+ index));
		assertEquals(2, oldTerms.size());

		// Regular variable unification with numerical values too
		oldState.clear();
		oldState.add("(distance x y -1)");
		newState.clear();
		newState.add("(distance a b 2)");
		oldTerms.clear();
		oldTerms.add("x");
		oldTerms.add("-1");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("2");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		index++;
		assertTrue(oldState.contains("(distance ?X ? ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " -1.0 2.0))"));
		assertTrue(oldTerms.contains("?X"));
		assertTrue(oldTerms.contains("?" + Covering.RANGE_VARIABLE_PREFIX
				+ index));
		assertEquals(2, oldTerms.size());

		// Unification under an existing range term
		oldState.clear();
		oldState.add("(distance a b ?" + Covering.RANGE_VARIABLE_PREFIX
				+ "0&:(" + StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0 1.0 3.0))");
		newState.clear();
		newState.add("(distance a b 2)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("?" + Covering.RANGE_VARIABLE_PREFIX + "0");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("2");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(0, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(distance a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0 1.0 3.0))"));
		assertTrue(oldTerms.contains("a"));
		assertTrue(oldTerms
				.contains("?" + Covering.RANGE_VARIABLE_PREFIX + "0"));
		assertEquals(2, oldTerms.size());

		// Unification under an existing range term (extension)
		oldState.clear();
		oldState.add("(distance a b ?" + Covering.RANGE_VARIABLE_PREFIX
				+ "0&:(" + StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0 1.0 3.0))");
		newState.clear();
		newState.add("(distance a b -2)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("?" + Covering.RANGE_VARIABLE_PREFIX + "0");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("-2");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(1, oldState.size());
		assertTrue(oldState.contains("(distance a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0 -2.0 3.0))"));
		assertTrue(oldTerms.contains("a"));
		assertTrue(oldTerms
				.contains("?" + Covering.RANGE_VARIABLE_PREFIX + "0"));
		assertEquals(2, oldTerms.size());

		// Multiple numerical terms
		oldState.clear();
		oldState.add("(distance a b 1)");
		oldState.add("(level a 1)");
		newState.clear();
		newState.add("(distance a b -2)");
		newState.add("(level a 3)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("1");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("-2");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		index++;
		assertTrue(oldState.contains("(distance a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " -2.0 1.0))"));
		assertTrue(oldTerms.contains("a"));
		assertTrue(oldTerms.contains("?" + Covering.RANGE_VARIABLE_PREFIX
				+ index));
		assertEquals(2, oldTerms.size());
		index++;
		assertTrue(oldState.contains("(level a ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " 1.0 3.0))"));

		// Multiple numerical terms with existing range term
		oldState.clear();
		oldState.add("(distance a b ?" + Covering.RANGE_VARIABLE_PREFIX
				+ "0&:(" + StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0 1.0 3.0))");
		oldState.add("(level a 1)");
		newState.clear();
		newState.add("(distance a b -2)");
		newState.add("(level a 3)");
		oldTerms.clear();
		oldTerms.add("a");
		oldTerms.add("?" + Covering.RANGE_VARIABLE_PREFIX + "0");
		newTerms.clear();
		newTerms.add("a");
		newTerms.add("-2");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		assertTrue(oldState.contains("(distance a b ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + "0 -2.0 3.0))"));
		assertTrue(oldTerms.contains("a"));
		assertTrue(oldTerms
				.contains("?" + Covering.RANGE_VARIABLE_PREFIX + "0"));
		assertEquals(2, oldTerms.size());
		index++;
		assertTrue(oldState.contains("(level a ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " 1.0 3.0))"));

		// Variables and numerical unification (differing distance)
		oldState.clear();
		oldState.add("(distanceDot ? ?X 1)");
		oldState.add("(dot ?X)");
		newState.clear();
		newState.add("(distanceGhost player blinky 2)");
		newState.add("(ghost blinky)");
		newState.add("(distanceDot player dot_1 5)");
		newState.add("(dot dot_1)");
		oldTerms.clear();
		oldTerms.add("?X");
		oldTerms.add("1");
		newTerms.clear();
		newTerms.add("dot_1");
		newTerms.add("5");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		index++;
		assertTrue(oldState.contains("(distanceDot ? ?X ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " 1.0 5.0))"));
		assertTrue(oldState.contains("(dot ?X)"));
		assertTrue(oldTerms.contains("?X"));
		assertTrue(oldTerms.contains("?" + Covering.RANGE_VARIABLE_PREFIX
				+ index));
		assertEquals(2, oldTerms.size());

		// Variables and numerical unification (out-of-range distance)
		oldState.clear();
		oldState.add("(distanceDot ? ?X 1)");
		oldState.add("(dot ?X)");
		newState.clear();
		newState.add("(distanceGhost player blinky 10)");
		newState.add("(ghost blinky)");
		newState.add("(distanceDot player dot_1 5)");
		newState.add("(dot dot_1)");
		oldTerms.clear();
		oldTerms.add("?X");
		oldTerms.add("1");
		newTerms.clear();
		newTerms.add("dot_1");
		newTerms.add("5");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		index++;
		assertTrue(oldState.contains("(distanceDot ? ?X ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " 1.0 5.0))"));
		assertTrue(oldState.contains("(dot ?X)"));
		assertTrue(oldTerms.contains("?X"));
		assertTrue(oldTerms.contains("?" + Covering.RANGE_VARIABLE_PREFIX
				+ index));
		assertEquals(2, oldTerms.size());

		// Variables and numerical unification (same distance)
		oldState.clear();
		oldState.add("(distanceDot ? ?X 1)");
		oldState.add("(dot ?X)");
		newState.clear();
		newState.add("(distanceGhost player blinky 1)");
		newState.add("(ghost blinky)");
		newState.add("(distanceDot player dot_1 5)");
		newState.add("(dot dot_1)");
		oldTerms.clear();
		oldTerms.add("?X");
		oldTerms.add("1");
		newTerms.clear();
		newTerms.add("dot_1");
		newTerms.add("5");
		result = sut_.unifyStates(oldState, newState, oldTerms, newTerms);
		assertEquals(1, result);
		assertEquals(2, oldState.size());
		index++;
		assertTrue(oldState.contains("(distanceDot ? ?X ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + "&:("
				+ StateSpec.BETWEEN_RANGE + " ?"
				+ Covering.RANGE_VARIABLE_PREFIX + index + " 1.0 5.0))"));
		assertTrue(oldState.contains("(dot ?X)"));
		assertTrue(oldTerms.contains("?X"));
		assertTrue(oldTerms.contains("?" + Covering.RANGE_VARIABLE_PREFIX
				+ index));
		assertEquals(2, oldTerms.size());
	}

	/**
	 * 
	 */
	@Test
	public void testSpecialiseToPreGoal() {
		// Basic stack test
		List<String> pregoal = new ArrayList<String>();
		pregoal.add("(onFloor ?X)");
		pregoal.add("(clear ?X)");
		pregoal.add("(block ?X)");
		pregoal.add("(on ?Y ?)");
		pregoal.add("(clear ?Y)");
		pregoal.add("(block ?Y)");
		sut_.setPreGoal("(move ?X ?Y)", pregoal);

		GuidedRule rule = new GuidedRule(
				"(clear ?X) (clear ?Y) => (move ?X ?Y)");
		assertEquals(5, rule.getConditions().size());
		Collection<GuidedRule> results = sut_.specialiseToPreGoal(rule);
		assertEquals(2, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear ?Y) (onFloor ?X) => (move ?X ?Y)", false,
				true, null)));
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear ?Y) (on ?Y ?) => (move ?X ?Y)", false, true,
				null)));

		// Full covering
		rule = new GuidedRule(
				"(clear ?X) (clear ?Y) (onFloor ?X) => (move ?X ?Y)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(1, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear ?Y) (onFloor ?X) (on ?Y ?) => (move ?X ?Y)",
				false, true, null)));

		// Empty case
		pregoal.clear();
		pregoal.add("(clear ?X)");
		pregoal.add("(block ?X)");
		pregoal.add("(clear ?Y)");
		pregoal.add("(block ?Y)");
		sut_.setPreGoal("(move ?X ?Y)", pregoal);

		rule = new GuidedRule("(clear ?X) (clear ?Y) => (move ?X ?Y)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(0, results.size());

		// Empty case 2
		rule = new GuidedRule("(on ?X ?) (clear ?X) => (moveFloor ?X)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(0, results.size());

		// Specialisation to constant
		pregoal.clear();
		pregoal.add("(clear a)");
		pregoal.add("(block a)");
		pregoal.add("(clear b)");
		pregoal.add("(block b)");
		sut_.setPreGoal("(move a b)", pregoal);

		rule = new GuidedRule("(clear ?X) (clear ?Y) => (move ?X ?Y)");
		results = sut_.specialiseToPreGoal(rule);
		assertTrue(results.contains(new GuidedRule(
				"(clear a) (clear ?Y) => (move a ?Y)", false, true, null)));
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear b) => (move ?X b)", false, true, null)));
		assertEquals(2, results.size());

		// Constant to variable (invalid!)
		pregoal.clear();
		pregoal.add("(clear ?X)");
		pregoal.add("(block ?X)");
		pregoal.add("(clear ?Y)");
		pregoal.add("(block ?Y)");
		sut_.setPreGoal("(move ?X ?Y)", pregoal);

		rule = new GuidedRule("(clear a) (clear b) => (move a b)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(0, results.size());

		// Using constants for general rule addition
		pregoal.clear();
		pregoal.add("(onFloor a)");
		pregoal.add("(clear a)");
		pregoal.add("(block a)");
		pregoal.add("(on b ?)");
		pregoal.add("(clear b)");
		pregoal.add("(block b)");
		sut_.setPreGoal("(move a b)", pregoal);

		rule = new GuidedRule("(clear ?X) (clear ?Y) => (move ?X ?Y)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(4, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear ?Y) (onFloor ?X) => (move ?X ?Y)", false,
				true, null)));
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear ?Y) (on ?Y ?) => (move ?X ?Y)", false, true,
				null)));
		assertTrue(results.contains(new GuidedRule(
				"(clear a) (clear ?Y) => (move a ?Y)", false, true, null)));
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear b) => (move ?X b)", false, true, null)));

		// Inner predicate specialisation
		pregoal.clear();
		pregoal.add("(on ?X a)");
		pregoal.add("(on ?Y b)");
		sut_.setPreGoal("(move ?X ?Y)", pregoal);

		rule = new GuidedRule("(on ?X ?) (on ?Y ?) => (move ?X ?Y)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(2, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(on ?X a) (on ?Y ?) => (move ?X ?Y)", false, true, null)));
		assertTrue(results.contains(new GuidedRule(
				"(on ?X ?) (on ?Y b) => (move ?X ?Y)", false, true, null)));

		// Inner predicate specialisation with terms
		pregoal.clear();
		pregoal.add("(on ?X ?Y)");
		pregoal.add("(on ?Y b)");
		sut_.setPreGoal("(move ?X ?Y)", pregoal);

		rule = new GuidedRule("(on ?X ?) => (move ?X a)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(1, results.size());
		assertTrue(results.contains(new GuidedRule("(on ?X a) => (move ?X a)",
				false, true, null)));

		// Adding constant facts
		pregoal.clear();
		pregoal.add("(onFloor a)");
		pregoal.add("(clear ?X)");
		pregoal.add("(block ?X)");
		pregoal.add("(on b ?)");
		pregoal.add("(clear ?Y)");
		pregoal.add("(block ?Y)");
		sut_.setPreGoal("(move ?X ?Y)", pregoal);

		rule = new GuidedRule(
				"(clear ?X) (onFloor ?X) (clear ?Y) (on ?Y ?) => (move ?X ?Y)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(2, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (onFloor a) (clear ?Y) (on ?Y ?) => (move ?X ?Y)",
				false, true, null)));
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (onFloor ?X) (clear ?Y) (on b ?) => (move ?X ?Y)",
				false, true, null)));

		// Constant substitution 2
		pregoal.clear();
		pregoal.add("(on a b)");
		pregoal.add("(clear a)");
		pregoal.add("(block a)");
		pregoal.add("(block b)");
		sut_.setPreGoal("(move a b)", pregoal);

		rule = new GuidedRule("(on ?X ?Y) (clear ?X) => (move ?X ?Y)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(2, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(on a ?Y) (clear a) => (move a ?Y)", false, true, null)));
		assertTrue(results.contains(new GuidedRule(
				"(on ?X b) (clear ?X) => (move ?X b)", false, true, null)));

		// Secondary mutation case
		pregoal.clear();
		pregoal.add("(block a)");
		pregoal.add("(block ?X)");
		pregoal.add("(on ?X a)");
		pregoal.add("(clear ?X)");
		pregoal.add("(above ?X a)");
		sut_.setPreGoal("(moveFloor ?X)", pregoal);

		rule = new GuidedRule(
				"(clear ?X) (above ?X ?) (block ?X) (block a) => (moveFloor ?X)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(2, results.size());
		assertTrue(results
				.contains(new GuidedRule(
						"(on ?X a) (clear ?X) (block ?X) (block a) => (moveFloor ?X)",
						false, true, null)));
		assertTrue(results
				.contains(new GuidedRule(
						"(clear ?X) (above ?X a) (block ?X) (block a) => (moveFloor ?X)",
						false, true, null)));

		pregoal.clear();
		pregoal.add("(block ?X)");
		pregoal.add("(block ?_MOD_a)");
		pregoal.add("(on ?X ?_MOD_a)");
		pregoal.add("(above ?X ?)");
		pregoal.add("(clear ?X)");
		pregoal.add("(above ?X ?_MOD_a)");
		sut_.setPreGoal("(moveFloor ?X)", pregoal);

		rule = new GuidedRule(
				"(above ?X ?_MOD_a) (clear ?X) (on ?X ?) => (moveFloor ?X)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(1, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(on ?X ?_MOD_a) (clear ?X) (block ?X) => (moveFloor ?X)",
				false, true, null)));
	}

	@Test
	public void testSpecialiseRule() {
		// Set up the allowable conditions
		Collection<String> conditions = new HashSet<String>();
		conditions.add("(on ?X ?)");
		conditions.add("(above ?X ?)");
		conditions.add("(highest ?X)");
		conditions.add("(clear ?X)");
		sut_.setAllowedActionConditions("moveFloor", conditions);
		conditions = new HashSet<String>();
		conditions.add("(on ?X ?)");
		conditions.add("(on ?Y ?)");
		conditions.add("(above ?X ?)");
		conditions.add("(above ?Y ?)");
		conditions.add("(highest ?X)");
		conditions.add("(highest ?Y)");
		conditions.add("(clear ?X)");
		conditions.add("(clear ?Y)");
		conditions.add("(onFloor ?X)");
		conditions.add("(onFloor ?Y)");
		sut_.setAllowedActionConditions("move", conditions);

		// Basic single action specialisation
		GuidedRule rule = new GuidedRule(
				"(clear ?X) (on ?X ?) => (moveFloor ?X)");
		Collection<GuidedRule> results = sut_.specialiseRule(rule);

		GuidedRule mutant = new GuidedRule(
				"(on ?X ?) (highest ?X) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear ?X) (on ?X ?) (not (highest ?X)) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear ?X) (on ?X ?) (onFloor ?X) => (moveFloor ?X)");
		assertFalse(results.contains(mutant));
		assertEquals(results.size(), 2);

		// Second (impossible) step specialisation
		Collection<GuidedRule> subResults = new HashSet<GuidedRule>();
		for (GuidedRule gr : results)
			subResults.addAll(sut_.specialiseRule(gr));
		assertEquals(subResults.size(), 0);

		// Constant term in action
		rule = new GuidedRule("(clear a) (on a ?) => (moveFloor a)");
		results = sut_.specialiseRule(rule);

		mutant = new GuidedRule("(highest a) (on a ?) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule("(clear a) (not (highest a)) (on a ?) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 2);

		// Constant term in rule
		rule = new GuidedRule("(clear a) (on ?X ?) => (moveFloor ?X)");
		results = sut_.specialiseRule(rule);

		mutant = new GuidedRule(
				"(clear a) (on ?X ?) (highest ?X) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (on ?X ?) (clear ?X) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (on ?X ?) (not (highest ?X)) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (on ?X ?) (not (clear ?X)) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 4);

		// Harder action
		rule = new GuidedRule("(clear a) (clear ?Y) => (move a ?Y)");
		results = sut_.specialiseRule(rule);

		mutant = new GuidedRule("(clear a) (clear ?Y) (on a ?) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule("(clear a) (clear ?Y) (on ?Y ?) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (above a ?) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (above ?Y ?) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule("(clear ?Y) (highest a) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule("(clear a) (highest ?Y) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (onFloor a) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (onFloor ?Y) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (not (above a ?)) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (not (above ?Y ?)) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (not (highest a)) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (not (highest ?Y)) => (move a ?Y)");
		assertTrue(results.contains(mutant));
		// Due to equality background knowledge, can remove these negated rules.
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (not (onFloor a)) => (move a ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (not (onFloor ?Y)) => (move a ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (not (on a ?)) => (move a ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (not (on ?Y ?)) => (move a ?Y)");
		assertFalse(results.contains(mutant));
		assertEquals(results.size(), 12);

		// Avoiding impossible specialisations
		rule = new GuidedRule(
				"(clear ?X) (clear ?Y) (on ?X ?Y) => (move ?X ?Y)");
		results = sut_.specialiseRule(rule);

		// Should not be there
		mutant = new GuidedRule(
				"(clear ?X) (clear ?Y) (on ?X ?Y) (on ?X ?) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear ?X) (clear ?Y) (on ?X ?Y) (not (on ?X ?)) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));
		// Using background knowledge to disallow pointless and illegal
		// mutations
		mutant = new GuidedRule(
				"(clear ?X) (clear ?Y) (on ?X ?Y) (onFloor ?X) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear ?X) (clear ?Y) (on ?X ?Y) (not (onFloor ?X)) => (move ?X ?Y)");
		assertFalse(results.contains(mutant));

		// Adding to the right term
		rule = new GuidedRule("(clear ?X) (block ?Y) => (move ?X ?Y)");
		results = sut_.specialiseRule(rule);

		mutant = new GuidedRule("(highest ?X) (block ?Y) => (move ?X ?Y)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule("(clear ?X) (highest ?Y) => (move ?X ?Y)");
		assertTrue(results.contains(mutant));
	}

	@Test
	public void testSimplifyRule() {
		// Simple no-effect test
		SortedSet<String> ruleConds = new TreeSet<String>(ConditionComparator
				.getInstance());
		ruleConds.add("(clear a)");
		SortedSet<String> results = sut_.simplifyRule(ruleConds, null, null,
				false);
		assertNull(results);

		// Condition removal
		ruleConds.clear();
		ruleConds.add("(on ?X ?)");
		ruleConds.add("(above ?X ?)");
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains("(on ?X ?)"));
		assertEquals(results.size(), 1);

		// Using an added condition (null result)
		ruleConds.clear();
		ruleConds.add("(on ?X ?)");
		results = sut_.simplifyRule(ruleConds, "(above ?X ?)", null, false);
		assertNull(results);

		// Using an added condition (no simplification)
		ruleConds.clear();
		ruleConds.add("(on ?X ?)");
		results = sut_.simplifyRule(ruleConds, "(clear ?X)", null, false);
		assertNotNull(results);
		assertTrue(results.contains("(on ?X ?)"));
		assertTrue(results.contains("(clear ?X)"));
		assertEquals(results.size(), 2);

		// Using an added condition (swapped result)
		ruleConds.clear();
		ruleConds.add("(above ?X ?)");
		results = sut_.simplifyRule(ruleConds, "(on ?X ?)", null, false);
		assertNotNull(results);
		assertTrue(results.contains("(on ?X ?)"));
		assertEquals(results.size(), 1);

		// Testing double-negated condition
		ruleConds.clear();
		ruleConds.add("(above ?X ?)");
		results = sut_.simplifyRule(ruleConds, "(not (above ?X ?))", null,
				false);
		assertNull(results);

		// Testing illegal condition
		ruleConds.clear();
		ruleConds.add("(on ?X ?)");
		ruleConds.add("(not (above ?X ?))");
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNull(results);
		results = sut_.simplifyRule(ruleConds, null, null, true);
		assertNotNull(results);
		assertTrue(results.contains("(on ?X ?)"));
		assertEquals(results.size(), 1);

		// Testing same condition
		ruleConds.clear();
		ruleConds.add("(on ?X ?)");
		results = sut_.simplifyRule(ruleConds, "(on ?X ?)", null, false);
		assertNull(results);

		ruleConds.clear();
		ruleConds.add("(on ?X ?)");
		results = sut_.simplifyRule(ruleConds, "(not (on ?X ?))", "(on ?X ?)",
				false);
		assertNull(results);

		// Testing unification
		ruleConds.clear();
		ruleConds.add("(on ?X ?)");
		results = sut_.simplifyRule(ruleConds, "(on ?X ?Y)", null, false);
		assertNull(results);

		// Testing non-unification
		ruleConds.clear();
		ruleConds.add("(on ?X ?)");
		ruleConds.add("(above ?X a)");
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNull(results);

		// Testing equivalent conditions (prefer left side of equation to right)
		ruleConds.clear();
		ruleConds.add("(not (on ?X ?))");
		ruleConds.add("(onFloor ?X)");
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains("(onFloor ?X)"));
		assertEquals(results.size(), 1);

		ruleConds.clear();
		ruleConds.add("(on ?X ?)");
		ruleConds.add("(not (onFloor ?X))");
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains("(on ?X ?)"));
		assertEquals(results.size(), 1);
		
		// Testing swapped for left equivalent conditions
		ruleConds.clear();
		ruleConds.add("(not (on ?X ?))");
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains("(onFloor ?X)"));
		assertEquals(results.size(), 1);
		
		ruleConds.clear();
		ruleConds.add("(not (onFloor ?X))");
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains("(on ?X ?)"));
		assertEquals(results.size(), 1);

		// Testing unification of background knowledge
		ruleConds.clear();
		ruleConds.add("(on ?X ?Y)");
		ruleConds.add("(above ?X ?)");
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNotNull(results);
		assertTrue(results.contains("(on ?X ?Y)"));
		assertEquals(results.size(), 1);

		ruleConds.clear();
		ruleConds.add("(on ?X ?)");
		ruleConds.add("(above ?X ?Y)");
		results = sut_.simplifyRule(ruleConds, null, null, false);
		assertNull(results);
	}
}