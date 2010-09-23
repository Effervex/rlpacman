package test;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.SortedSet;

import jess.Fact;
import jess.Rete;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import quicktime.std.clocks.ExtremesCallBack;
import relationalFramework.Covering;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.AgentObservations;
import relationalFramework.agentObservations.ConditionBeliefs;

public class AgentObservationsTest {
	private AgentObservations sut_;

	@Before
	public void setUp() throws Exception {
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
		sut_ = new AgentObservations();
		StateSpec.initInstance("blocksWorld.BlocksWorld");
	}

	@Test
	public void testScanState() throws Exception {
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
		Collection<Fact> facts = StateSpec.extractFacts(state);

		sut_.scanState(facts);
		SortedSet<ConditionBeliefs> condBeliefs = sut_.getConditionBeliefs();
		for (ConditionBeliefs cb : condBeliefs) {
			
		}
	}

	@Test
	public void testGatherActionFacts() {
		fail("Not yet implemented");
	}
}
