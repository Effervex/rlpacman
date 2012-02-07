package test;

import static org.junit.Assert.*;

import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jess.Rete;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.junit.Before;
import org.junit.Test;

import cerrla.LocalCrossEntropyDistribution;
import cerrla.ProgramArgument;
import cerrla.Slot;
import cerrla.modular.GoalCondition;

import rrlFramework.RRLObservations;
import util.ArgumentComparator;
import util.MultiMap;

public class LocalCrossEntropyDistributionTest {
	private LocalCrossEntropyDistribution sut_;

	@Before
	public void setUp() {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
		sut_ = new LocalCrossEntropyDistribution(
				GoalCondition.parseGoalCondition("on$A$B"), 0);
	}

	@Test
	public void testCoverState() throws Exception {
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

		BidiMap goalReplacements = new DualHashBidiMap();
		goalReplacements.put("z", "?G_0");
		goalReplacements.put("x", "?G_1");
		List<RelationalRule> rlggRules = sut_.coverState(null,
				new RRLObservations(state, validActions, 0d, goalReplacements,
						false), activatedActions, null);
		RelationalRule rlggRule = new RelationalRule(
				"(above ?X ?) (height ?X ?#_0) (clear ?X) => (moveFloor ?X)");
		List<String> queryParameters = new ArrayList<String>();
		queryParameters.add("?G_0");
		queryParameters.add("?G_1");
		rlggRule.setQueryParams(queryParameters);
		assertTrue(rlggRules.contains(rlggRule));
		rlggRule = new RelationalRule("(clear ?X) (clear ?Y) => (move ?X ?Y)");
		rlggRule.setQueryParams(queryParameters);
		assertTrue(rlggRules.contains(rlggRule));
		assertEquals(rlggRules.size(), 2);

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

		rlggRules = sut_.coverState(null, new RRLObservations(state,
				validActions, 0d, goalReplacements, false), activatedActions,
				null);
		assertTrue(rlggRules.isEmpty());

		// Test the state of the slot generator
		Collection<Slot> slotGenerator = sut_.getPolicyGenerator()
				.getGenerator();
		for (Slot slot : slotGenerator) {
			if (slot.getAction().equals("move"))
				assertEquals(slot.size(), 25);
			else if (slot.getAction().equals("moveFloor"))
				assertEquals(slot.size(), 11);
		}
	}

	@Test
	public void testPacManTriggerRLGGCovering() throws Exception {
		// Init PacMan
		StateSpec.initInstance("rlPacMan.PacMan");
		sut_ = new LocalCrossEntropyDistribution(GoalCondition.parseGoalCondition("levelmax"),
				0);

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

		List<RelationalRule> rlggRules = sut_.coverState(null,
				new RRLObservations(state, validActions, 0d,
						new DualHashBidiMap(), false), activatedActions, null);

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

		rlggRules = sut_.coverState(null, new RRLObservations(state,
				validActions, 0d, new DualHashBidiMap(), false),
				activatedActions, null);

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

		rlggRules = sut_.coverState(null, new RRLObservations(state,
				validActions, 0d, new DualHashBidiMap(), false),
				activatedActions, null);
		RelationalRule rlggRule = new RelationalRule(
				"(distanceGhost ? ?X ?__Num0&:(betweenRange ?__Num0 4.0 25.0))"
						+ " => (toGhost ?X ?__Num0)");
		assertTrue(rlggRules.toString(), rlggRules.contains(rlggRule));
		rlggRule = new RelationalRule(
				"(distanceGhost ? ?X ?__Num1&:(betweenRange ?__Num1 4.0 25.0))"
						+ " => (fromGhost ?X ?__Num1)");
		assertTrue(rlggRules.toString(), rlggRules.contains(rlggRule));
		assertEquals(rlggRules.size(), 2);

		// Test the state of the slot generator
		Collection<Slot> slotGenerator = sut_.getPolicyGenerator()
				.getGenerator();
		assertEquals(slotGenerator.size(), 10);
	}
}
