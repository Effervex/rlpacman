package test;

import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.junit.Before;
import org.junit.Test;

import relationalFramework.StateSpec;
import relationalFramework.agentObservations.LocalAgentObservations;
import rrlFramework.Config;
import rrlFramework.RRLObservations;
import util.MultiMap;

import cerrla.CERRLA;
import cerrla.modular.GoalCondition;

public class CERRLATest {
	private CERRLA sut_;

	@Before
	public void setUp() {
		sut_ = new CERRLA();
		StateSpec.initInstance("blocksWorldMove.BlocksWorld", "onab");

		LocalAgentObservations.loadAgentObservations(GoalCondition
				.parseGoalCondition(StateSpec.getInstance().getGoalName()),
				null);
		Config.newInstance(new String[] { "blocksMoveArguments.txt" });
		sut_.initialise(0);
	}

	@Test
	public void testStartEpisode() {
		MultiMap<String, String[]> validActions = MultiMap.createListMultiMap();
		RRLObservations obs = new RRLObservations(StateSpec.getInstance()
				.getRete(), validActions, new double[] { 0, 0 },
				new DualHashBidiMap(), 0);
		sut_.startEpisode(obs);
	}

}
