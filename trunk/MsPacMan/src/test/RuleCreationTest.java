package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import jess.Fact;
import jess.JessException;
import jess.Rete;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.ActionChoice;
import relationalFramework.ConditionComparator;
import relationalFramework.RuleCreation;
import relationalFramework.GuidedRule;
import relationalFramework.MultiMap;
import relationalFramework.Policy;
import relationalFramework.RuleAction;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;
import relationalFramework.agentObservations.BackgroundKnowledge;

public class RuleCreationTest {
	private RuleCreation sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
		sut_ = new RuleCreation();
	}

	@Test
	public void testTest() throws Exception {
		StringFact strFactA = StateSpec.toStringFact("(clear ?X)");
		StringFact strFactB = StateSpec.toStringFact("(clear ?Y)");
		assertFalse(strFactA.equals(strFactB));
		assertFalse(strFactA.hashCode() == strFactB.hashCode());

		GuidedRule gr = new GuidedRule("(clear a) (block a) => (moveFloor a)");
		assertTrue(gr.getConditions(false).contains(
				StateSpec.toStringFact("(clear a)")));
		assertTrue(gr.getConditions(false).contains(
				StateSpec.toStringFact("(block a)")));
	}

	@Test
	public void testRLGGState() throws Exception {
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

		List<GuidedRule> rules = sut_.rlggState(state, validActions,
				new MultiMap<String, GuidedRule>());
		assertEquals(rules.size(), 2);
		for (GuidedRule gr : rules) {
			System.out.println(gr);
			if (gr.getAction().getFactName().equals("moveFloor")) {
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(2, condCount);
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest ?X)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear ?X)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?X ?)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?X ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(onFloor ?X)")));
				assertEquals(StateSpec.toStringFact("(moveFloor ?X)"), gr
						.getAction());
			} else {
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(2, condCount);
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear ?X)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear ?Y)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?X ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?X ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest ?X)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?Y ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?Y ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest ?Y)")));
				assertEquals(StateSpec.toStringFact("(move ?X ?Y)"), gr
						.getAction());
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

		rules = sut_.rlggState(state, validActions, existingRules);
		assertEquals(rules.size(), 1);
		assertEquals(existingRules.sizeTotal(), 2);
		for (GuidedRule gr : existingRules.values()) {
			System.out.println(gr);
			if (gr.getAction().getFactName().equals("moveFloor")) {
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(2, condCount);
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest ?X)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear ?X)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?X ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?X ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(onFloor ?X)")));
				assertEquals(StateSpec.toStringFact("(moveFloor ?X)"), gr
						.getAction());
			} else {
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(2, condCount);
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear ?X)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear ?Y)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?X ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?X ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest ?X)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?Y ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?Y ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest ?Y)")));
				assertEquals(StateSpec.toStringFact("(move ?X ?Y)"), gr
						.getAction());
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

		rules = sut_.rlggState(state, validActions,
				new MultiMap<String, GuidedRule>());
		assertEquals(2, rules.size());
		existingRules.clear();
		for (GuidedRule gr : rules) {
			System.out.println(gr.toString());
			if (gr.getAction().getFactName().equals("moveFloor")) {
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest d)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear d)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on d b)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above d b)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above d a)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above d c)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(block a)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(block b)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(block c)")));
				assertEquals(StateSpec.toStringFact("(moveFloor d)"), gr
						.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(7, condCount);
				existingRules.put("moveFloor", gr);
			} else {
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear ?X)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear ?Y)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?X ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ? b)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ? b)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ? a)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?X ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest ?X)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?Y ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?Y ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest ?Y)")));
				assertEquals(StateSpec.toStringFact("(move ?X ?Y)"), gr
						.getAction());
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

		rules = sut_.rlggState(state, validActions, existingRules);
		assertEquals(2, rules.size());
		assertEquals(2, existingRules.sizeTotal());
		for (GuidedRule gr : existingRules.values()) {
			System.out.println(gr.toString());
			if (gr.getAction().getFactName().equals("moveFloor")) {
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest ?X)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear ?X)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?X ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?X b)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?X b)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?X a)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?X c)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?X ?)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(block a)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(block c)")));
				assertEquals(StateSpec.toStringFact("(moveFloor ?X)"), gr
						.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(5, condCount);
			} else {
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear ?X)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear ?Y)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?X ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ? b)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ? b)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ? a)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?X ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest ?X)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?Y ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?Y ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest ?Y)")));
				assertEquals(StateSpec.toStringFact("(move ?X ?Y)"), gr
						.getAction());
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

		rules = sut_.rlggState(state, validActions, existingRules);
		assertEquals(2, rules.size());
		assertEquals(2, existingRules.sizeTotal());
		for (GuidedRule gr : existingRules.values()) {
			System.out.println(gr.toString());
			if (gr.getAction().getFactName().equals("moveFloor")) {
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest ?X)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear ?X)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?X ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?X b)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?X b)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?X a)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?X ?)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(block a)")));
				assertEquals(StateSpec.toStringFact("(moveFloor ?X)"), gr
						.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(3, condCount);
			} else {
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear ?X)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear ?Y)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?X ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ? b)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ? b)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ? a)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?X ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest ?X)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?Y ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?Y ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest ?Y)")));
				assertEquals(StateSpec.toStringFact("(move ?X ?Y)"), gr
						.getAction());
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

		rules = sut_.rlggState(state, validActions,
				new MultiMap<String, GuidedRule>());
		assertEquals(2, rules.size());
		for (GuidedRule gr : rules) {
			System.out.println(gr.toString());
			if (gr.getAction().getFactName().equals("moveFloor")) {
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest b)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear b)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on b d)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above b a)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above b d)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above b c)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(block a)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(block b)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(block c)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(block d)")));
				assertEquals(StateSpec.toStringFact("(moveFloor b)"), gr
						.getAction());
				int condCount = StateSpec.getInstance().encodeRule(gr).split(
						StateSpec.INFERS_ACTION)[0].replaceAll(
						"\\(.+?\\)( |$)", ".").length();
				assertEquals(7, condCount);
				existingRules.put("moveFloor", gr);
			} else {
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear ?X)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear ?Y)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?X ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?X ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest ?X)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on ?Y ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above ?Y ?)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest ?Y)")));
				assertEquals(StateSpec.toStringFact("(move ?X ?Y)"), gr
						.getAction());
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

		rules = sut_.rlggState(state, validActions, existingRules);
		assertEquals(1, rules.size());
		assertEquals(2, existingRules.sizeTotal());
		for (GuidedRule gr : rules) {
			System.out.println(gr.toString());
			if (gr.getAction().getFactName().equals("moveFloor")) {
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(highest b)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(clear b)")));
				assertFalse(gr.getConditions(false).contains(
						StateSpec.toStringFact("(on b ?)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(above b ?)")));
				assertTrue(gr.getConditions(false).contains(
						StateSpec.toStringFact("(block b)")));
				assertEquals(StateSpec.toStringFact("(moveFloor b)"), gr
						.getAction());
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
	/*
	 * @Test public void testInverselySubstitute() throws Exception { //
	 * Ensuring we have no constants List<String> constants = new
	 * ArrayList<String>();
	 * 
	 * // Basic substitution Rete state = StateSpec.getInstance().getRete();
	 * state.eval("(assert (clear a))"); Collection<Fact> facts =
	 * StateSpec.extractFacts(state); String[] actionTerms = { "a" };
	 * Collection<String> result = sut_.inverselySubstitute(facts, actionTerms,
	 * constants); assertEquals(result.size(), 1);
	 * assertTrue(result.contains(StateSpec.toStringFact("(clear ?X)")));
	 * 
	 * // Substitution of an unknown variable actionTerms[0] = "b"; result =
	 * sut_.inverselySubstitute(facts, actionTerms, constants);
	 * assertEquals(result.size(), 1);
	 * assertTrue(result.contains(StateSpec.toStringFact("(clear ?)")));
	 * 
	 * // Only return one anonymous fact if there is more than one
	 * state.eval("(assert (clear b))"); state.eval("(assert (clear c))"); facts
	 * = StateSpec.extractFacts(state); actionTerms[0] = "a"; result =
	 * sut_.inverselySubstitute(facts, actionTerms, constants);
	 * assertEquals(result.size(), 2);
	 * assertTrue(result.contains(StateSpec.toStringFact("(clear ?X)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(clear ?)")));
	 * 
	 * // Swap over many facts state.eval("(assert (on a d))");
	 * state.eval("(assert (on b e))"); state.eval("(assert (onFloor c))");
	 * facts = StateSpec.extractFacts(state); actionTerms[0] = "a"; result =
	 * sut_.inverselySubstitute(facts, actionTerms, constants);
	 * assertEquals(result.size(), 6);
	 * assertTrue(result.contains(StateSpec.toStringFact("(clear ?X)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(clear ?)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(on ?X d)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(block d)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(on ? ?)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(onFloor ?)")));
	 * 
	 * // Multiple action terms actionTerms = new String[2]; actionTerms[0] =
	 * "a"; actionTerms[1] = "c"; result = sut_.inverselySubstitute(facts,
	 * actionTerms, constants); assertEquals(result.size(), 7);
	 * assertTrue(result.contains(StateSpec.toStringFact("(clear ?X)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(clear ?Y)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(clear ?)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(on ?X d)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(block d)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(on ? ?)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(onFloor ?Y)")));
	 * 
	 * // Testing constants constants.add("a"); actionTerms = new String[1];
	 * actionTerms[0] = "a"; result = sut_.inverselySubstitute(facts,
	 * actionTerms, constants); assertEquals(result.size(), 7);
	 * assertTrue(result.contains(StateSpec.toStringFact("(clear a)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(block a)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(clear ?)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(on a d)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(block d)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(on ? ?)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(onFloor ?)")));
	 * 
	 * actionTerms[0] = "b"; result = sut_.inverselySubstitute(facts,
	 * actionTerms, constants); assertEquals(result.size(), 9);
	 * assertTrue(result.contains(StateSpec.toStringFact("(clear a)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(block a)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(clear ?X)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(clear ?)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(on a d)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(block d)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(on ?X e)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(block e)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(onFloor ?)")));
	 * 
	 * // Numerical values (Same dist) state.reset();
	 * state.eval("(assert (distance player dotA 3))");
	 * state.eval("(assert (dot dotA))");
	 * state.eval("(assert (distance player blinky 3))");
	 * state.eval("(assert (ghost blinky))"); constants.clear();
	 * constants.add("dotA"); constants.add("3"); actionTerms = new String[2];
	 * actionTerms[0] = "dotA"; actionTerms[1] = "3"; facts =
	 * StateSpec.extractFacts(state); result = sut_.inverselySubstitute(facts,
	 * actionTerms, constants);
	 * assertTrue(result.contains(StateSpec.toStringFact("(initial-fact)")));
	 * assertTrue
	 * (result.contains(StateSpec.toStringFact("(distance player dotA 3)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(dot dotA)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(distance ? ? ?)")));
	 * assertTrue(result.contains(StateSpec.toStringFact("(ghost ?)")));
	 * assertEquals(result.size(), 5); }
	 */

	/**
	 * @throws Exception
	 */
	@Test
	public void testFormPreGoalState() throws JessException {
		// Ensuring we have no constants
		List<String> constants = new ArrayList<String>();
		StateSpec.getInstance().setConstants(constants);

		// Basic covering
		Rete state = StateSpec.getInstance().getRete();
		state.eval("(assert (clear a))");
		state.eval("(assert (block a))");
		Collection<Fact> facts = StateSpec.extractFacts(state);
		ActionChoice ac = new ActionChoice();
		List<StringFact> actions = new ArrayList<StringFact>();
		actions.add(StateSpec.toStringFact("(moveFloor a)"));
		Policy policy = new Policy();
		RuleAction ra = new RuleAction(new GuidedRule(
				"(clear a) => (moveFloor a)"), actions, policy);
		ra.getTriggerActions();
		ac.switchOn(ra);
		sut_.formPreGoalState(facts, ac);
		Collection<StringFact> preGoal = sut_.getPreGoalState("moveFloor");
		assertEquals(preGoal.size(), 2);
		assertTrue(preGoal.contains(StateSpec.toStringFact("(clear a)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(block a)")));

		// Generalisation (same predicate and action)
		state.reset();
		state.eval("(assert (clear b))");
		state.eval("(assert (block b))");
		facts = StateSpec.extractFacts(state);
		ac = new ActionChoice();
		actions.clear();
		actions.add(StateSpec.toStringFact("(moveFloor b)"));
		ra = new RuleAction(new GuidedRule("(clear b) => (moveFloor b)"),
				actions, policy);
		ra.getTriggerActions();
		ac.switchOn(ra);
		sut_.formPreGoalState(facts, ac);
		preGoal = sut_.getPreGoalState("moveFloor");
		assertEquals(preGoal.size(), 2);
		assertTrue(preGoal.contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(block ?X)")));

		// Generalising test (different predicates, same action)
		sut_.clearPreGoalState();
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
		actions.add(StateSpec.toStringFact("(move a b)"));
		ra = new RuleAction(
				new GuidedRule("(clear a) (clear b) => (move a b)"), actions,
				policy);
		ra.getTriggerActions();
		ac.switchOn(ra);
		sut_.formPreGoalState(facts, ac);
		preGoal = sut_.getPreGoalState("move");
		// Contains the defined preds, above and clear preds
		assertEquals(9, preGoal.size());
		assertTrue(preGoal.contains(StateSpec.toStringFact("(clear a)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(clear b)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(block a)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(block b)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(on a ?)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(on b ?)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(highest a)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(above a ?)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(above b ?)")));

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
		sut_.formPreGoalState(facts, ac);
		preGoal = sut_.getPreGoalState("move");
		// Contains less than the defined preds, above and clear preds
		assertEquals(6, preGoal.size());
		assertTrue(preGoal.contains(StateSpec.toStringFact("(clear a)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(clear b)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(block a)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(block b)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(on b ?)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(above b ?)")));

		// Generalising the action
		ac = new ActionChoice();
		actions.clear();
		actions.add(StateSpec.toStringFact("(move b a)"));
		ra = new RuleAction(
				new GuidedRule("(clear a) (clear b) => (move b a)"), actions,
				policy);
		ra.getTriggerActions();
		ac.switchOn(ra);
		sut_.formPreGoalState(facts, ac);
		preGoal = sut_.getPreGoalState("move");
		// Contains less than the defined preds, above and clear preds
		assertEquals(4, preGoal.size());
		assertTrue(preGoal.contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(clear ?Y)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(block ?X)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(block ?Y)")));
	}

	/**
	 * 
	 */
	@Test
	public void testSpecialiseToPreGoal() {
		// Basic stack test
		List<StringFact> pregoal = new ArrayList<StringFact>();
		pregoal.add(StateSpec.toStringFact("(onFloor ?X)"));
		pregoal.add(StateSpec.toStringFact("(clear ?X)"));
		pregoal.add(StateSpec.toStringFact("(block ?X)"));
		pregoal.add(StateSpec.toStringFact("(above ?Y ?)"));
		pregoal.add(StateSpec.toStringFact("(clear ?Y)"));
		pregoal.add(StateSpec.toStringFact("(block ?Y)"));
		sut_.setPreGoal(StateSpec.toStringFact("(move ?X ?Y)"), pregoal);

		GuidedRule rule = new GuidedRule(
				"(clear ?X) (clear ?Y) => (move ?X ?Y)");
		assertEquals(5, rule.getConditions(false).size());
		Collection<GuidedRule> results = sut_.specialiseToPreGoal(rule);
		assertEquals(2, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear ?Y) (onFloor ?X) => (move ?X ?Y)", rule)));
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear ?Y) (above ?Y ?) => (move ?X ?Y)", rule)));
		for (GuidedRule gr : results) {
			assertTrue(gr.getParentRules().contains(rule));
			assertEquals(gr.getParentRules().size(), 1);
		}

		// Full covering
		rule = new GuidedRule(
				"(clear ?X) (clear ?Y) (onFloor ?X) => (move ?X ?Y)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(1, results.size());
		assertTrue(results
				.contains(new GuidedRule(
						"(clear ?X) (clear ?Y) (onFloor ?X) (above ?Y ?) => (move ?X ?Y)",
						rule)));

		// Empty case
		pregoal.clear();
		pregoal.add(StateSpec.toStringFact("(clear ?X)"));
		pregoal.add(StateSpec.toStringFact("(block ?X)"));
		pregoal.add(StateSpec.toStringFact("(clear ?Y)"));
		pregoal.add(StateSpec.toStringFact("(block ?Y)"));
		sut_.setPreGoal(StateSpec.toStringFact("(move ?X ?Y)"), pregoal);

		rule = new GuidedRule("(clear ?X) (clear ?Y) => (move ?X ?Y)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(0, results.size());

		// Empty case 2
		rule = new GuidedRule("(on ?X ?) (clear ?X) => (moveFloor ?X)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(0, results.size());

		// Specialisation to constant
		pregoal.clear();
		pregoal.add(StateSpec.toStringFact("(clear a)"));
		pregoal.add(StateSpec.toStringFact("(block a)"));
		pregoal.add(StateSpec.toStringFact("(clear b)"));
		pregoal.add(StateSpec.toStringFact("(block b)"));
		sut_.setPreGoal(StateSpec.toStringFact("(move a b)"), pregoal);

		rule = new GuidedRule("(clear ?X) (clear ?Y) => (move ?X ?Y)");
		results = sut_.specialiseToPreGoal(rule);
		assertTrue(results.contains(new GuidedRule(
				"(clear a) (clear ?Y) => (move a ?Y)", rule)));
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear b) => (move ?X b)", rule)));
		assertEquals(2, results.size());

		// Constant to variable (invalid!)
		pregoal.clear();
		pregoal.add(StateSpec.toStringFact("(clear ?X)"));
		pregoal.add(StateSpec.toStringFact("(block ?X)"));
		pregoal.add(StateSpec.toStringFact("(clear ?Y)"));
		pregoal.add(StateSpec.toStringFact("(block ?Y)"));
		sut_.setPreGoal(StateSpec.toStringFact("(move ?X ?Y)"), pregoal);

		rule = new GuidedRule("(clear a) (clear b) => (move a b)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(0, results.size());

		// Using constants for general rule addition
		pregoal.clear();
		pregoal.add(StateSpec.toStringFact("(onFloor a)"));
		pregoal.add(StateSpec.toStringFact("(clear a)"));
		pregoal.add(StateSpec.toStringFact("(block a)"));
		pregoal.add(StateSpec.toStringFact("(above b ?)"));
		pregoal.add(StateSpec.toStringFact("(clear b)"));
		pregoal.add(StateSpec.toStringFact("(block b)"));
		sut_.setPreGoal(StateSpec.toStringFact("(move a b)"), pregoal);

		rule = new GuidedRule("(clear ?X) (clear ?Y) => (move ?X ?Y)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(4, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear ?Y) (onFloor ?X) => (move ?X ?Y)", rule)));
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear ?Y) (above ?Y ?) => (move ?X ?Y)", rule)));
		assertTrue(results.contains(new GuidedRule(
				"(clear a) (clear ?Y) => (move a ?Y)", rule)));
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (clear b) => (move ?X b)", rule)));

		// Inner predicate specialisation
		pregoal.clear();
		pregoal.add(StateSpec.toStringFact("(above ?X a)"));
		pregoal.add(StateSpec.toStringFact("(above ?Y b)"));
		sut_.setPreGoal(StateSpec.toStringFact("(move ?X ?Y)"), pregoal);

		rule = new GuidedRule("(above ?X ?) (above ?Y ?) => (move ?X ?Y)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(2, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(above ?X a) (above ?Y ?) => (move ?X ?Y)", rule)));
		assertTrue(results.contains(new GuidedRule(
				"(above ?X ?) (above ?Y b) => (move ?X ?Y)", rule)));

		// Inner predicate specialisation with terms
		pregoal.clear();
		pregoal.add(StateSpec.toStringFact("(above ?X ?Y)"));
		pregoal.add(StateSpec.toStringFact("(above ?Y b)"));
		sut_.setPreGoal(StateSpec.toStringFact("(move ?X ?Y)"), pregoal);

		rule = new GuidedRule("(above ?X ?) => (move ?X a)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(2, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(above ?X a) => (move ?X a)", rule)));
		assertTrue(results.contains(new GuidedRule(
				"(above ?X ?) (above a b) => (move ?X a)", rule)));

		// Adding constant facts
		pregoal.clear();
		pregoal.add(StateSpec.toStringFact("(onFloor a)"));
		pregoal.add(StateSpec.toStringFact("(clear ?X)"));
		pregoal.add(StateSpec.toStringFact("(block ?X)"));
		pregoal.add(StateSpec.toStringFact("(above b ?)"));
		pregoal.add(StateSpec.toStringFact("(clear ?Y)"));
		pregoal.add(StateSpec.toStringFact("(block ?Y)"));
		sut_.setPreGoal(StateSpec.toStringFact("(move ?X ?Y)"), pregoal);

		rule = new GuidedRule(
				"(clear ?X) (onFloor ?X) (clear ?Y) (above ?Y ?) => (move ?X ?Y)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(2, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (onFloor ?X) (clear ?Y) (above ?Y ?) "
						+ "(onFloor a) => (move ?X ?Y)", rule)));
		assertTrue(results.contains(new GuidedRule(
				"(clear ?X) (onFloor ?X) (clear ?Y) (above ?Y ?) "
						+ "(above b ?) => (move ?X ?Y)", rule)));

		// Constant substitution 2
		pregoal.clear();
		pregoal.add(StateSpec.toStringFact("(on a b)"));
		pregoal.add(StateSpec.toStringFact("(clear a)"));
		pregoal.add(StateSpec.toStringFact("(block a)"));
		pregoal.add(StateSpec.toStringFact("(block b)"));
		sut_.setPreGoal(StateSpec.toStringFact("(move a b)"), pregoal);

		rule = new GuidedRule("(on ?X ?Y) (clear ?X) => (move ?X ?Y)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(2, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(on a ?Y) (clear a) => (move a ?Y)", rule)));
		assertTrue(results.contains(new GuidedRule(
				"(on ?X b) (clear ?X) => (move ?X b)", rule)));

		// Secondary mutation case
		pregoal.clear();
		pregoal.add(StateSpec.toStringFact("(block a)"));
		pregoal.add(StateSpec.toStringFact("(block ?X)"));
		pregoal.add(StateSpec.toStringFact("(on ?X a)"));
		pregoal.add(StateSpec.toStringFact("(clear ?X)"));
		pregoal.add(StateSpec.toStringFact("(above ?X a)"));
		sut_.setPreGoal(StateSpec.toStringFact("(moveFloor ?X)"), pregoal);

		rule = new GuidedRule(
				"(clear ?X) (above ?X ?) (block ?X) (block a) => (moveFloor ?X)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(2, results.size());
		assertTrue(results.contains(new GuidedRule(
				"(on ?X a) (clear ?X) (block ?X) (block a) => (moveFloor ?X)",
				rule)));
		assertTrue(results
				.contains(new GuidedRule(
						"(clear ?X) (above ?X a) (block ?X) (block a) => (moveFloor ?X)",
						rule)));

		pregoal.clear();
		pregoal.add(StateSpec.toStringFact("(block ?X)"));
		pregoal.add(StateSpec.toStringFact("(block ?_MOD_a)"));
		pregoal.add(StateSpec.toStringFact("(on ?X ?_MOD_a)"));
		pregoal.add(StateSpec.toStringFact("(above ?X ?)"));
		pregoal.add(StateSpec.toStringFact("(clear ?X)"));
		pregoal.add(StateSpec.toStringFact("(above ?X ?_MOD_a)"));
		sut_.setPreGoal(StateSpec.toStringFact("(moveFloor ?X)"), pregoal);

		rule = new GuidedRule("(above ?X ?_MOD_a) (clear ?X) => (moveFloor ?X)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(1, results.size());
		assertTrue(results
				.contains(new GuidedRule(
						"(on ?X ?_MOD_a) (clear ?X) (block ?X) => (moveFloor ?X)",
						rule)));

		pregoal.clear();
		pregoal.add(StateSpec.toStringFact("(above d j)"));
		pregoal.add(StateSpec.toStringFact("(clear d)"));
		pregoal.add(StateSpec.toStringFact("(on d j)"));
		pregoal.add(StateSpec.toStringFact("(block d)"));
		pregoal.add(StateSpec.toStringFact("(highest d)"));
		sut_.setPreGoal(StateSpec.toStringFact("(moveFloor d)"), pregoal);

		rule = new GuidedRule(
				"(clear ?X) (above ?X ?) (not (highest ?X)) => (moveFloor ?X)");
		results = sut_.specialiseToPreGoal(rule);
		assertFalse(results
				.contains(new GuidedRule(
						"(above ?X ?) (highest ?X) (not (highest ?X)) => (moveFloor ?X)",
						rule)));
	}

	@Test
	public void testSpecialiseRule() {
		// Basic single action specialisation
		GuidedRule rule = new GuidedRule(
				"(clear ?X) (above ?X ?) => (moveFloor ?X)");
		Collection<GuidedRule> results = sut_.specialiseRule(rule);

		GuidedRule mutant = new GuidedRule(
				"(above ?X ?) (highest ?X) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear ?X) (above ?X ?) (not (highest ?X)) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		// No onFloor rules for moveFloor
		mutant = new GuidedRule(
				"(clear ?X) (above ?X ?) (onFloor ?X) => (moveFloor ?X)");
		assertFalse(results.contains(mutant));
		assertEquals(results.size(), 2);

		// Second (impossible) step specialisation
		Collection<GuidedRule> subResults = new HashSet<GuidedRule>();
		for (GuidedRule gr : results)
			subResults.addAll(sut_.specialiseRule(gr));
		assertEquals(subResults.size(), 0);

		// Constant term in action
		rule = new GuidedRule("(clear a) (above a ?) => (moveFloor a)");
		results = sut_.specialiseRule(rule);

		mutant = new GuidedRule("(highest a) (above a ?) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (not (highest a)) (above a ?) => (moveFloor a)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 2);

		// Constant term in rule
		rule = new GuidedRule(
				"(clear a) (above ?X ?) (clear ?X) => (moveFloor ?X)");
		results = sut_.specialiseRule(rule);

		mutant = new GuidedRule(
				"(clear a) (above ?X ?) (highest ?X) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?X) (above ?X ?) (not (highest ?X)) => (moveFloor ?X)");
		assertTrue(results.contains(mutant));
		assertEquals(results.size(), 2);

		// Harder action
		rule = new GuidedRule("(clear a) (clear ?Y) => (move a ?Y)");
		results = sut_.specialiseRule(rule);

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
				"(clear a) (clear ?Y) (not (above a ?)) => (move a ?Y)");
		assertFalse(results.contains(mutant));
		mutant = new GuidedRule(
				"(clear a) (clear ?Y) (not (above ?Y ?)) => (move a ?Y)");
		assertFalse(results.contains(mutant));
		assertEquals(results.size(), 8);

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
		SortedSet<StringFact> ruleConds = new TreeSet<StringFact>(
				ConditionComparator.getInstance());
		ruleConds.add(StateSpec.toStringFact("(clear a)"));
		SortedSet<StringFact> results = sut_.simplifyRule(ruleConds, null,
				false, true);
		assertNull(results);

		// Equivalence condition removal
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?)"));
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertEquals(results.size(), 1);

		// Using an added condition (null result)
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, StateSpec
				.toStringFact("(on ?X ?)"), false, true);
		assertNull(results);

		// Using an added condition (no simplification)
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, StateSpec
				.toStringFact("(clear ?X)"), false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertTrue(results.contains(StateSpec.toStringFact("(clear ?X)")));
		assertEquals(results.size(), 2);

		// Using an added condition (swapped result)
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?)"));
		results = sut_.simplifyRule(ruleConds, StateSpec
				.toStringFact("(above ?X ?)"), false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertEquals(results.size(), 1);

		// Testing double-negated condition
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, StateSpec
				.toStringFact("(not (above ?X ?))"), false, true);
		assertNull(results);

		// Testing illegal condition
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		ruleConds.add(StateSpec.toStringFact("(onFloor ?X)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNull(results);
		results = sut_.simplifyRule(ruleConds, null, true, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertEquals(results.size(), 1);

		// Testing same condition
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?)"));
		results = sut_.simplifyRule(ruleConds, StateSpec
				.toStringFact("(on ?X ?)"), false, true);
		assertNull(results);

		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?)"));
		results = sut_.simplifyRule(ruleConds, StateSpec
				.toStringFact("(not (on ?X ?))"), false, true);
		assertNull(results);

		// Testing unification
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?)"));
		results = sut_.simplifyRule(ruleConds, StateSpec
				.toStringFact("(on ?X ?Y)"), false, true);
		assertNull(results);

		// Testing double unification (onX? -> aboveX? which is removed)
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?)"));
		ruleConds.add(StateSpec.toStringFact("(above ?X a)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X a)")));
		assertEquals(results.size(), 1);

		// Testing complex simplification
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		ruleConds.add(StateSpec.toStringFact("(above ?X ?Y)"));
		ruleConds.add(StateSpec.toStringFact("(above ? ?Y)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?Y)")));
		assertEquals(results.size(), 1);

		// Even more complex
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?Y)"));
		ruleConds.add(StateSpec.toStringFact("(on ? ?Y)"));
		ruleConds.add(StateSpec.toStringFact("(not (onFloor ?X))"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?Y)")));
		assertEquals(results.size(), 1);

		// Testing equivalent conditions (prefer left side of equation to right)
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(not (on ?X ?))"));
		ruleConds.add(StateSpec.toStringFact("(onFloor ?X)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(onFloor ?X)")));
		assertEquals(results.size(), 1);

		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		ruleConds.add(StateSpec.toStringFact("(not (onFloor ?X))"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertEquals(results.size(), 1);

		// Testing swapped for left equivalent conditions
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(not (on ?X ?))"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(onFloor ?X)")));
		assertEquals(results.size(), 1);

		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(not (onFloor ?X))"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertEquals(results.size(), 1);

		// Testing unification of background knowledge
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?Y)"));
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(on ?X ?Y)")));
		assertEquals(results.size(), 1);

		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?)"));
		ruleConds.add(StateSpec.toStringFact("(above ?X ?Y)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?Y)")));
		assertEquals(results.size(), 1);

		// Testing unification on a number of matches
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		ruleConds.add(StateSpec.toStringFact("(above ?Y b)"));
		ruleConds.add(StateSpec.toStringFact("(above ?Y ?)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertTrue(results.contains(StateSpec.toStringFact("(above ?Y b)")));
		assertEquals(results.size(), 2);

		// Testing unification on a number of matches
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?Y b)"));
		ruleConds.add(StateSpec.toStringFact("(above ?Y ?)"));
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertTrue(results.contains(StateSpec.toStringFact("(above ?Y b)")));
		assertEquals(results.size(), 2);

		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X a)"));
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		ruleConds.add(StateSpec.toStringFact("(above ?Y ?)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X a)")));
		assertTrue(results.contains(StateSpec.toStringFact("(above ?Y ?)")));
		assertEquals(results.size(), 2);
	}

	@Test
	public void testEquivalenceRules() {
		// Set up the allowable conditions
		Collection<StringFact> conditions = new HashSet<StringFact>();
		conditions.add(StateSpec.toStringFact("(on ?X ?)"));
		conditions.add(StateSpec.toStringFact("(above ?X ?)"));
		conditions.add(StateSpec.toStringFact("(highest ?X)"));
		conditions.add(StateSpec.toStringFact("(clear ?X)"));
		sut_.setAllowedActionConditions("moveFloor", conditions);
		conditions = new HashSet<StringFact>();
		conditions.add(StateSpec.toStringFact("(on ?X ?)"));
		conditions.add(StateSpec.toStringFact("(on ?Y ?)"));
		conditions.add(StateSpec.toStringFact("(above ?X ?)"));
		conditions.add(StateSpec.toStringFact("(above ?Y ?)"));
		conditions.add(StateSpec.toStringFact("(highest ?X)"));
		conditions.add(StateSpec.toStringFact("(highest ?Y)"));
		conditions.add(StateSpec.toStringFact("(clear ?X)"));
		conditions.add(StateSpec.toStringFact("(clear ?Y)"));
		conditions.add(StateSpec.toStringFact("(onFloor ?X)"));
		conditions.add(StateSpec.toStringFact("(onFloor ?Y)"));
		sut_.setAllowedActionConditions("move", conditions);

		// Set up the equivalence and other rules
		SortedSet<BackgroundKnowledge> backKnow = new TreeSet<BackgroundKnowledge>();
		backKnow.add(new BackgroundKnowledge("(above ?X ?) <=> (on ?X ?)",
				false));
		backKnow.add(new BackgroundKnowledge(
				"(above ?X ?) <=> (not (onFloor ?X))", false));
		backKnow.add(new BackgroundKnowledge("(above ? ?Y) <=> (on ? ?Y)",
				false));
		backKnow.add(new BackgroundKnowledge(
				"(above ? ?Y) <=> (not (clear ?Y))", false));
		backKnow.add(new BackgroundKnowledge(
				"(clear ?X) <=> (not (above ? ?X))", false));
		backKnow.add(new BackgroundKnowledge("(clear ?X) <=> (not (on ? ?X))",
				false));
		backKnow.add(new BackgroundKnowledge(
				"(on ?X ?) <=> (not (onFloor ?X))", false));
		backKnow.add(new BackgroundKnowledge("(on ? ?Y) <=> (not (clear ?Y))",
				false));
		backKnow.add(new BackgroundKnowledge(
				"(onFloor ?X) <=> (not (above ?X ?))", false));
		backKnow.add(new BackgroundKnowledge(
				"(onFloor ?X) <=> (not (on ?X ?))", false));
		backKnow.add(new BackgroundKnowledge("(above ?X ?Y) => (above ?X ?)",
				false));
		backKnow.add(new BackgroundKnowledge("(above ?X ?Y) => (above ? ?Y)",
				false));
		backKnow.add(new BackgroundKnowledge(
				"(on ?X ?Y) (above ?Y ?Z) => (assert (above ?X ?Z))", false));
		backKnow.add(new BackgroundKnowledge("(highest ?X) => (clear ?X)",
				false));
		backKnow.add(new BackgroundKnowledge(
				"(on ?X ?Y) => (assert (above ?X ?Y))", false));
		backKnow.add(new BackgroundKnowledge("(on ?X ?Y) => (on ?X ?)", false));
		backKnow.add(new BackgroundKnowledge("(on ?X ?Y) => (on ? ?Y)", false));
		backKnow.add(new BackgroundKnowledge(
				"(block ?Z) (on ?X ?Y) => (not (on ?X ?Z))", false));
		backKnow
				.add(new BackgroundKnowledge("(on ?X ?) => (above ?X ?)", false));
		backKnow.add(new BackgroundKnowledge(
				"(block ?Y) (not (on ? ?Y)) => (assert (clear ?Y))", false));
		sut_.setBackgroundKnowledge(backKnow);

		// Basic implication test
		SortedSet<StringFact> ruleConds = new TreeSet<StringFact>(
				ConditionComparator.getInstance());
		ruleConds.add(StateSpec.toStringFact("(clear ?X)"));
		ruleConds.add(StateSpec.toStringFact("(highest ?X)"));
		SortedSet<StringFact> results = sut_.simplifyRule(ruleConds, null,
				false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(highest ?X)")));
		assertEquals(results.size(), 1);

		// Basic equivalency swap test
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(on ?X ?)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ?X ?)")));
		assertEquals(results.size(), 1);

		// Basic negated equivalency swap test
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(not (clear ?X))"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(above ? ?X)")));
		assertEquals(results.size(), 1);

		// TODO Illegal action condition restriction

	}
	
	@Test
	public void testSimplifyRuleMoveBW() {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld");
		sut_ = new RuleCreation();
		
		// Test the (block X) <=> (above X ?) rule
		SortedSet<StringFact> ruleConds = new TreeSet<StringFact>(
				ConditionComparator.getInstance());
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		SortedSet<StringFact> results = sut_.simplifyRule(ruleConds, null,
				false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(block ?X)")));
		assertEquals(results.size(), 1);
	}
}