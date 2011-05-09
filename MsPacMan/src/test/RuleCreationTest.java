package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import jess.Fact;
import jess.JessException;
import jess.Rete;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.ActionChoice;
import relationalFramework.ConditionComparator;
import relationalFramework.Module;
import relationalFramework.RuleCreation;
import relationalFramework.GuidedRule;
import relationalFramework.Policy;
import relationalFramework.RuleAction;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;
import relationalFramework.agentObservations.AgentObservations;
import relationalFramework.agentObservations.BackgroundKnowledge;

public class RuleCreationTest {
	private RuleCreation sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
		sut_ = new RuleCreation();
		assertTrue("No loaded agent observations. Cannot run test.",
				AgentObservations.loadAgentObservations());
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
		Set<StringFact> actions = new HashSet<StringFact>();
		actions.add(StateSpec.toStringFact("(moveFloor a)"));
		Policy policy = new Policy();
		RuleAction ra = new RuleAction(new GuidedRule(
				"(clear a) => (moveFloor a)"), actions, policy);
		ra.triggerRule();
		ac.switchOn(ra);
		sut_.formPreGoalState(facts, ac, null, null);
		Collection<StringFact> preGoal = AgentObservations.getInstance()
				.getPreGoal("moveFloor").getState();
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
		ra.triggerRule();
		ac.switchOn(ra);
		sut_.formPreGoalState(facts, ac, null, null);
		preGoal = AgentObservations.getInstance().getPreGoal("moveFloor")
				.getState();
		assertEquals(preGoal.size(), 2);
		assertTrue(preGoal.contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(block ?X)")));

		// Generalising test (different predicates, same action)
		AgentObservations.getInstance().clearPreGoal();
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
		ra.triggerRule();
		ac.switchOn(ra);
		sut_.formPreGoalState(facts, ac, null, null);
		preGoal = AgentObservations.getInstance().getPreGoal("move").getState();
		// Contains the defined preds, above and clear preds
		assertEquals(10, preGoal.size());
		assertTrue(preGoal.contains(StateSpec.toStringFact("(clear a)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(clear b)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(block a)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(block b)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(on a d)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(on b e)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(highest a)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(above a d)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(above a c)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(above b e)")));

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
		ra.triggerRule();
		ac.switchOn(ra);
		sut_.formPreGoalState(facts, ac, null, null);
		preGoal = AgentObservations.getInstance().getPreGoal("move").getState();
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
		ra.triggerRule();
		ac.switchOn(ra);
		sut_.formPreGoalState(facts, ac, null, null);
		preGoal = AgentObservations.getInstance().getPreGoal("move").getState();
		// Contains less than the defined preds, above and clear preds
		assertEquals(4, preGoal.size());
		assertTrue(preGoal.contains(StateSpec.toStringFact("(clear ?X)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(clear ?Y)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(block ?X)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(block ?Y)")));

		// Modular pre-goal
		ac = new ActionChoice();
		actions.clear();
		actions.add(StateSpec.toStringFact("(move b a)"));
		ra = new RuleAction(
				new GuidedRule("(clear a) (clear b) => (move b a)"), actions,
				policy);
		ra.triggerRule();
		ac.switchOn(ra);
		Map<String, String> replacement = new HashMap<String, String>();
		replacement.put("b", Module.createModuleParameter(0));
		AgentObservations.getInstance().clearPreGoal();
		sut_.formPreGoalState(facts, ac, null, replacement);
		preGoal = AgentObservations.getInstance().getPreGoal("move").getState();
		// Contains less than the defined preds, above and clear preds
		assertEquals(preGoal.toString(), 8, preGoal.size());
		assertTrue(preGoal.contains(StateSpec.toStringFact("(clear a)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(onFloor a)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(clear "
				+ Module.createModuleParameter(0) + ")")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(highest "
				+ Module.createModuleParameter(0) + ")")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(block a)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(block "
				+ Module.createModuleParameter(0) + ")")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(on "
				+ Module.createModuleParameter(0) + " f)")));
		assertTrue(preGoal.contains(StateSpec.toStringFact("(above "
				+ Module.createModuleParameter(0) + " f)")));
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
		AgentObservations.getInstance().setPreGoal(
				StateSpec.toStringFact("(move ?X ?Y)"), pregoal);

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
		AgentObservations.getInstance().setPreGoal(
				StateSpec.toStringFact("(move ?X ?Y)"), pregoal);

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
		AgentObservations.getInstance().setPreGoal(
				StateSpec.toStringFact("(move a b)"), pregoal);

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
		AgentObservations.getInstance().setPreGoal(
				StateSpec.toStringFact("(move ?X ?Y)"), pregoal);

		rule = new GuidedRule("(clear a) (clear b) => (move a b)");
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(results.toString(), 0, results.size());

		// Using constants for general rule addition
		pregoal.clear();
		pregoal.add(StateSpec.toStringFact("(onFloor a)"));
		pregoal.add(StateSpec.toStringFact("(clear a)"));
		pregoal.add(StateSpec.toStringFact("(block a)"));
		pregoal.add(StateSpec.toStringFact("(above b ?)"));
		pregoal.add(StateSpec.toStringFact("(clear b)"));
		pregoal.add(StateSpec.toStringFact("(block b)"));
		AgentObservations.getInstance().setPreGoal(
				StateSpec.toStringFact("(move a b)"), pregoal);

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
		AgentObservations.getInstance().setPreGoal(
				StateSpec.toStringFact("(move ?X ?Y)"), pregoal);

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
		AgentObservations.getInstance().setPreGoal(
				StateSpec.toStringFact("(move ?X ?Y)"), pregoal);

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
		AgentObservations.getInstance().setPreGoal(
				StateSpec.toStringFact("(move ?X ?Y)"), pregoal);

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
		AgentObservations.getInstance().setPreGoal(
				StateSpec.toStringFact("(move a b)"), pregoal);

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
		AgentObservations.getInstance().setPreGoal(
				StateSpec.toStringFact("(moveFloor ?X)"), pregoal);

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
		AgentObservations.getInstance().setPreGoal(
				StateSpec.toStringFact("(moveFloor ?X)"), pregoal);

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
		AgentObservations.getInstance().setPreGoal(
				StateSpec.toStringFact("(moveFloor d)"), pregoal);

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
		assertTrue(results.contains(StateSpec.toStringFact("(onFloor ?X)")));
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
	public void testSimplifyRuleBWMove() {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld");
		sut_ = new RuleCreation();
		assertTrue("No loaded agent observations. Cannot run test.",
				AgentObservations.loadAgentObservations());

		// Strange issue:
		SortedSet<StringFact> ruleConds = new TreeSet<StringFact>(
				ConditionComparator.getInstance());
		ruleConds.add(StateSpec.toStringFact("(highest ?Y)"));
		ruleConds.add(StateSpec.toStringFact("(above ?Y ?)"));
		ruleConds.add(StateSpec.toStringFact("(block ?Y)"));
		SortedSet<StringFact> results = sut_.simplifyRule(ruleConds, null,
				false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(highest ?Y)")));
		assertTrue(results.contains(StateSpec.toStringFact("(block ?Y)")));
		assertEquals(results.size(), 2);

		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(highest ?Y)"));
		ruleConds.add(StateSpec.toStringFact("(above ?Y floor)"));
		ruleConds.add(StateSpec.toStringFact("(above ?Y ?)"));
		ruleConds.add(StateSpec.toStringFact("(block ?Y)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(highest ?Y)")));
		assertTrue(results.contains(StateSpec.toStringFact("(above ?Y floor)")));
		assertTrue(results.contains(StateSpec.toStringFact("(block ?Y)")));
		assertEquals(results.size(), 3);

		// Test the (block X) <=> (above X ?) rule
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above ?X ?)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(block ?X)")));
		assertEquals(results.size(), 1);

		// Test the invariants
		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(clear floor)"));
		ruleConds.add(StateSpec.toStringFact("(floor floor)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(floor floor)")));
		assertEquals(results.size(), 1);

		ruleConds.clear();
		ruleConds.add(StateSpec.toStringFact("(above a floor)"));
		ruleConds.add(StateSpec.toStringFact("(floor floor)"));
		ruleConds.add(StateSpec.toStringFact("(block a)"));
		results = sut_.simplifyRule(ruleConds, null, false, true);
		assertNotNull(results);
		assertTrue(results.contains(StateSpec.toStringFact("(floor floor)")));
		assertTrue(results.contains(StateSpec.toStringFact("(block a)")));
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
		AgentObservations.getInstance().setActionConditions("moveFloor", conditions);
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
		AgentObservations.getInstance().setActionConditions("move", conditions);

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
		AgentObservations.getInstance().setBackgroundKnowledge(backKnow);

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
	}
}