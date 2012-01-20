package test;

import static org.junit.Assert.*;

import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.junit.Before;
import org.junit.Test;

import relationalFramework.GoalCondition;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.LocalAgentObservations;
import rrlFramework.Config;
import rrlFramework.RRLObservations;
import util.MultiMap;

import cerrla.CERRLA;

public class CERRLATest {
	private CERRLA sut_;

	@Before
	public void setUp() {
		sut_ = new CERRLA();
		StateSpec.initInstance("blocksWorldMove.BlocksWorld", "onab");
		LocalAgentObservations.loadAgentObservations(new GoalCondition(
				StateSpec.getInstance().getGoalName()));
		Config.newInstance(new String[] { "blocksMoveArguments.txt"});
		sut_.initialise(0);
	}

	@Test
	public void testStartEpisode() {
		MultiMap<String, String[]> validActions = MultiMap.createListMultiMap();
		RRLObservations obs = new RRLObservations(StateSpec.getInstance()
				.getRete(), validActions, 0d, new DualHashBidiMap(), false);
		sut_.startEpisode(obs);
	}

}
