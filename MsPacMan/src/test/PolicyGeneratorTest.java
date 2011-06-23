package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jess.Rete;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.GuidedRule;
import relationalFramework.PolicyGenerator;
import relationalFramework.Slot;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.AgentObservations;
import relationalFramework.util.ArgumentComparator;
import relationalFramework.util.MultiMap;

public class PolicyGeneratorTest {
	private PolicyGenerator sut_;

	@Before
	public void setUp() {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
		sut_ = PolicyGenerator.newInstance(0);
	}

	@Test
	public void testTriggerRLGGCovering() throws Exception {
		assertTrue("No agent observations. Cannot run test.",
				AgentObservations.loadAgentObservations());
		AgentObservations.getInstance().clearLocalObservations();
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

		Map<String, String> goalReplacements = new HashMap<String, String>();
		goalReplacements.put("z", "?G_0");
		goalReplacements.put("x", "?G_1");
		List<GuidedRule> rlggRules = sut_.triggerRLGGCovering(state,
				validActions, goalReplacements, activatedActions, true);

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
				goalReplacements, activatedActions, true);

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
				goalReplacements, activatedActions, true);
		GuidedRule rlggRule = new GuidedRule(
				"(above ?X ?) (clear ?X) => (moveFloor ?X)");
		List<String> queryParameters = new ArrayList<String>();
		queryParameters.add("?G_0");
		queryParameters.add("?G_1");
		rlggRule.setQueryParams(queryParameters);
		assertTrue(rlggRules.contains(rlggRule));
		rlggRule = new GuidedRule("(clear ?X) (clear ?Y) => (move ?X ?Y)");
		rlggRule.setQueryParams(queryParameters);
		assertTrue(rlggRules.contains(rlggRule));
		assertEquals(rlggRules.size(), 2);

		// Test the state of the slot generator
		Collection<Slot> slotGenerator = sut_.getGenerator();
		assertEquals(slotGenerator.size(), 12);
	}

	@Test
	public void testPacManTriggerRLGGCovering() throws Exception {
		// Init PacMan
		StateSpec.initInstance("rlPacMan.PacMan");
		sut_ = PolicyGenerator.newInstance(0);
		assertTrue("No agent observations. Cannot run test.",
				AgentObservations.loadAgentObservations());
		AgentObservations.getInstance().clearLocalObservations();

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
				validActions, null, activatedActions, true);

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

		rlggRules = sut_.triggerRLGGCovering(state, validActions, null,
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

		rlggRules = sut_.triggerRLGGCovering(state, validActions, null,
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
}
