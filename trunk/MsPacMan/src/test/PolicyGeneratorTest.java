package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jess.Fact;
import jess.Rete;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.ActionChoice;
import relationalFramework.ArgumentComparator;
import relationalFramework.GuidedRule;
import relationalFramework.MultiMap;
import relationalFramework.OrderedDistribution;
import relationalFramework.Policy;
import relationalFramework.PolicyGenerator;
import relationalFramework.ProbabilityDistribution;
import relationalFramework.RuleAction;
import relationalFramework.Slot;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

public class PolicyGeneratorTest {
	private PolicyGenerator sut_;

	@Before
	public void setUp() {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
		sut_ = PolicyGenerator.newInstance(0);
	}

	@Test
	public void testTriggerRLGGCovering() throws Exception {
		sut_.clearAgentObservations();
		Rete state = StateSpec.getInstance().getRete();
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
		MultiMap<String, String[]> validActions = StateSpec.getInstance()
				.generateValidActions(state);
		MultiMap<String, String[]> activatedActions = MultiMap
				.createSortedSetMultiMap(ArgumentComparator.getInstance());

		List<GuidedRule> rlggRules = sut_.triggerRLGGCovering(state,
				validActions, activatedActions, true);

		// [e]
		// [b][d]
		// [f][a][c]
		state.reset();
		state.eval("(assert (clear d))");
		state.eval("(assert (clear e))");
		state.eval("(assert (clear c))");
		state.eval("(assert (highest e))");
		state.eval("(assert (on d a))");
		state.eval("(assert (on e b))");
		state.eval("(assert (on b f))");
		state.eval("(assert (above e b))");
		state.eval("(assert (above e f))");
		state.eval("(assert (above b f))");
		state.eval("(assert (above d a))");
		state.eval("(assert (onFloor c))");
		state.eval("(assert (onFloor a))");
		state.eval("(assert (onFloor f))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (block f))");
		validActions = StateSpec.getInstance().generateValidActions(state);

		rlggRules = sut_.triggerRLGGCovering(state, validActions,
				activatedActions, true);

		state.reset();
		state.eval("(assert (clear d))");
		state.eval("(assert (clear e))");
		state.eval("(assert (clear c))");
		state.eval("(assert (highest e))");
		state.eval("(assert (on d a))");
		state.eval("(assert (on e b))");
		state.eval("(assert (on b f))");
		state.eval("(assert (above e b))");
		state.eval("(assert (above e f))");
		state.eval("(assert (above b f))");
		state.eval("(assert (above d a))");
		state.eval("(assert (onFloor c))");
		state.eval("(assert (onFloor a))");
		state.eval("(assert (onFloor f))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (block f))");
		validActions = StateSpec.getInstance().generateValidActions(state);

		rlggRules = sut_.triggerRLGGCovering(state, validActions,
				activatedActions, true);
		GuidedRule rlggRule = new GuidedRule(
				"(above ?X ?) (clear ?X) => (moveFloor ?X)");
		assertTrue(rlggRules.contains(rlggRule));
		rlggRule = new GuidedRule("(clear ?X) (clear ?Y) => (move ?X ?Y)");
		assertTrue(rlggRules.contains(rlggRule));
		assertEquals(rlggRules.size(), 2);

		// Test the state of the slot generator
		Collection<Slot> slotGenerator = sut_.getGenerator();
		assertEquals(slotGenerator.size(), 12);
	}

	@Test
	public void testSplitSlotsWithPreGoal() throws Exception {
		// Stack goal

		// Set up the pre-goal
		StateSpec.reinitInstance("stack");
		Rete state = StateSpec.getInstance().getRete();
		state.eval("(assert (clear a))");
		state.eval("(assert (clear f))");
		state.eval("(assert (highest a))");
		state.eval("(assert (on a b))");
		state.eval("(assert (on b c))");
		state.eval("(assert (on c d))");
		state.eval("(assert (on d e))");
		state.eval("(assert (onFloor e))");
		state.eval("(assert (onFloor f))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (block f))");
		state.run();
		Collection<Fact> preGoalFacts = StateSpec.extractFacts(state);
		ActionChoice actions = new ActionChoice();
		GuidedRule stackRule = new GuidedRule(
				"(clear ?X) (highest ?Y) => (move ?X ?Y)");
		Policy policy = new Policy();
		policy.addRule(stackRule, false, false);
		Set<StringFact> actionsList = new HashSet<StringFact>();
		actionsList.add(StateSpec.toStringFact("(move f a)"));
		RuleAction ruleAction = new RuleAction(stackRule, actionsList, policy);
		ruleAction.getActions();
		actions.switchOn(ruleAction);
		assertNull(sut_.getPreGoal("move"));
		sut_.formPreGoalState(preGoalFacts, actions, null);
		Collection<StringFact> preGoalState = new HashSet<StringFact>(sut_
				.getPreGoal("move"));
		assertTrue(sut_.hasPreGoal());

		state.reset();
		state.eval("(assert (clear d))");
		state.eval("(assert (clear e))");
		state.eval("(assert (clear c))");
		state.eval("(assert (highest e))");
		state.eval("(assert (on d a))");
		state.eval("(assert (on e b))");
		state.eval("(assert (on b f))");
		state.eval("(assert (above e b))");
		state.eval("(assert (above e f))");
		state.eval("(assert (above b f))");
		state.eval("(assert (above d a))");
		state.eval("(assert (onFloor c))");
		state.eval("(assert (onFloor a))");
		state.eval("(assert (onFloor f))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (block f))");
		MultiMap<String, String[]> validActions = StateSpec.getInstance()
				.generateValidActions(state);
		MultiMap<String, String[]> activatedActions = MultiMap
				.createSortedSetMultiMap(ArgumentComparator.getInstance());

		List<GuidedRule> rlggRules = sut_.triggerRLGGCovering(state,
				validActions, activatedActions, true);
		GuidedRule rlggRule = new GuidedRule(
				"(above ?X ?) (clear ?X) => (moveFloor ?X)");
		assertTrue(rlggRules.contains(rlggRule));
		rlggRule = new GuidedRule("(clear ?X) (clear ?Y) => (move ?X ?Y)");
		assertTrue(rlggRules.contains(rlggRule));
		assertEquals(rlggRules.size(), 2);

		// Test the state of the slot generator
		Collection<Slot> slotGenerator = sut_.getGenerator();
		assertEquals(slotGenerator.size(), 12);
		// Each move slot should have more than one rule
		StringFact blockA = StateSpec.toStringFact("(block a)");
		Collection<GuidedRule> duplicatesCheck = new HashSet<GuidedRule>();
		for (Slot slot : slotGenerator) {
			if (slot.getAction().equals("move")) {
				assertTrue(slot.toString() + " " + slot.getGenerator(), slot
						.getGenerator().size() > 1);
				// Each slot should contain a rule containing (above ?Y b)
				boolean containsBlockA = false;
				for (GuidedRule rule : slot.getGenerator()) {
					if (rule.getConditions(false).contains(blockA)) {
						containsBlockA = true;
						break;
					}
					assertTrue(duplicatesCheck + " " + rule, duplicatesCheck
							.add(rule));
				}

				assertTrue(slot.toString(), containsBlockA);
			}
		}

		// Change the pre-goal -> Change the rules
		state.reset();
		state.eval("(assert (clear b))");
		state.eval("(assert (clear f))");
		state.eval("(assert (highest f))");
		state.eval("(assert (on f c))");
		state.eval("(assert (on c d))");
		state.eval("(assert (on d e))");
		state.eval("(assert (on e a))");
		state.eval("(assert (onFloor a))");
		state.eval("(assert (onFloor b))");
		state.eval("(assert (block a))");
		state.eval("(assert (block b))");
		state.eval("(assert (block c))");
		state.eval("(assert (block d))");
		state.eval("(assert (block e))");
		state.eval("(assert (block f))");
		state.run();
		preGoalFacts = StateSpec.extractFacts(state);
		actions = new ActionChoice();
		policy = new Policy();
		policy.addRule(stackRule, false, false);
		actionsList = new HashSet<StringFact>();
		actionsList.add(StateSpec.toStringFact("(move b f)"));
		ruleAction = new RuleAction(stackRule, actionsList, policy);
		ruleAction.getActions();
		actions.switchOn(ruleAction);
		assertEquals(preGoalState, sut_.getPreGoal("move"));
		assertTrue(sut_.getPreGoal("move").contains(blockA));
		sut_.formPreGoalState(preGoalFacts, actions, null);
		assertFalse(sut_.getPreGoal("move").contains(blockA));
		assertFalse(preGoalState.equals(sut_.getPreGoal("move")));
		assertTrue(sut_.hasPreGoal());

		// Test the state of the slot generator
		slotGenerator = sut_.getGenerator();
		assertEquals(slotGenerator.size(), 12);
		// Each move slot should have more than one rule
		duplicatesCheck.clear();
		for (Slot slot : slotGenerator) {
			if (slot.getAction().equals("move")) {
				if (slot.getSlotSplitFacts() != null)
					assertTrue(slot.toString() + " " + slot.getGenerator(),
							slot.getGenerator().size() >= 1);
				// Each slot now should NOT contain a rule containing (above ?X
				// b)
				for (GuidedRule rule : slot.getGenerator()) {
					assertFalse(rule.toString(), rule.getConditions(false)
							.contains(blockA));
					assertTrue(duplicatesCheck.add(rule));
				}
			}
		}
	}

	@Test
	public void testSplitSlotsWithNumericalPreGoal() throws Exception {
		StateSpec.initInstance("rlPacMan.PacMan");
		sut_ = PolicyGenerator.newInstance(0);

	}

	@Test
	public void testPacManTriggerRLGGCovering() throws Exception {
		// Init PacMan
		StateSpec.initInstance("rlPacMan.PacMan");
		sut_ = PolicyGenerator.newInstance(0);
		sut_.clearAgentObservations();

		Rete state = StateSpec.getInstance().getRete();
		state.eval("(assert (distanceGhost player inky 5))");
		state.eval("(assert (distanceGhost player blinky 10))");
		state.eval("(assert (distanceGhost player clyde 20))");
		state.eval("(assert (edible blinky))");
		state.eval("(assert (pacman player))");
		state.eval("(assert (ghost inky))");
		state.eval("(assert (ghost blinky))");
		state.eval("(assert (ghost clyde))");
		MultiMap<String, String[]> validActions = StateSpec.getInstance()
				.generateValidActions(state);
		MultiMap<String, String[]> activatedActions = MultiMap
				.createSortedSetMultiMap(ArgumentComparator.getInstance());

		List<GuidedRule> rlggRules = sut_.triggerRLGGCovering(state,
				validActions, activatedActions, true);

		state.reset();
		state.eval("(assert (distanceGhost player inky 4))");
		state.eval("(assert (distanceGhost player blinky 12))");
		state.eval("(assert (distanceGhost player clyde 25))");
		state.eval("(assert (edible blinky))");
		state.eval("(assert (blinking blinky))");
		state.eval("(assert (pacman player))");
		state.eval("(assert (ghost inky))");
		state.eval("(assert (ghost blinky))");
		state.eval("(assert (ghost clyde))");
		validActions = StateSpec.getInstance().generateValidActions(state);
		activatedActions.clear();

		rlggRules = sut_.triggerRLGGCovering(state, validActions,
				activatedActions, true);

		state.reset();
		state.eval("(assert (distanceGhost player inky 5))");
		state.eval("(assert (distanceGhost player blinky 10))");
		state.eval("(assert (distanceGhost player clyde 20))");
		state.eval("(assert (edible blinky))");
		state.eval("(assert (pacman player))");
		state.eval("(assert (ghost inky))");
		state.eval("(assert (ghost blinky))");
		state.eval("(assert (ghost clyde))");
		validActions = StateSpec.getInstance().generateValidActions(state);
		activatedActions.clear();

		rlggRules = sut_.triggerRLGGCovering(state, validActions,
				activatedActions, true);
		GuidedRule rlggRule = new GuidedRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 4.0 25.0))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(rlggRules.toString(), rlggRules.contains(rlggRule));
		rlggRule = new GuidedRule(
				"(distanceGhost ? ?X ?__Num1&:(betweenRange ?__Num1 4.0 25.0))"
						+ " => (fromGhost ?X ?__Num1)");
		assertTrue(rlggRules.toString(), rlggRules.contains(rlggRule));
		assertEquals(rlggRules.size(), 2);

		// Test the state of the slot generator
		Collection<Slot> slotGenerator = sut_.getGenerator();
		assertEquals(slotGenerator.size(), 17);
	}

	@Test
	public void testUpdateDistributions() {
		fail("Not yet implemented");
	}

	@Test
	public void testPostUpdateOperations() {
		fail("Not yet implemented");
	}

}
