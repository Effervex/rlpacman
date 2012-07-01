package test;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import relationalFramework.StateSpec;
import rrlFramework.RRLExperiment;

import blocksWorldMove.BlocksState;
import blocksWorldMove.BlocksWorldEnvironment;

public class BlocksWorldEnvironmentTest {

	@Test
	public void testStateEquals() {
		Map<BlocksState, Integer> map = new HashMap<BlocksState, Integer>();
		BlocksState state1 = new BlocksState(new Integer[] { 0, 0, 0, 2, 1 });
		map.put(state1, 3);
		BlocksState state2 = new BlocksState(new Integer[] { 0, 0, 0, 2, 1 });
		assertTrue(state2.equals(state1));
		assertTrue(map.containsKey(state2));
	}

	@Test
	public void testInitialiseState() {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld");
		BlocksWorldEnvironment bwe = new BlocksWorldEnvironment();
		bwe.initialise(0, new String[] { "3" });
		Map<BlocksState, Integer> stateCounter = new HashMap<BlocksState, Integer>();
		for (int i = 0; i < 10000; i++) {
			bwe.initialiseBlocksState(3);
			Integer count = stateCounter.get(bwe.getState());
			if (count == null)
				count = 0;
			stateCounter.put(bwe.getState(), count + 1);
		}
		
		for (BlocksState bState : stateCounter.keySet()) {
			double ratio = stateCounter.get(bState) / 10000.0;
			System.out.println(ratio);
			assertEquals(ratio, 1.0/13, 0.01);
		}
		
		
		bwe.initialise(0, new String[] { "10" });
		stateCounter.clear();
		for (int i = 0; i < 10000; i++) {
			bwe.initialiseBlocksState(3);
			Integer count = stateCounter.get(bwe.getState());
			if (count == null)
				count = 0;
			stateCounter.put(bwe.getState(), count + 1);
		}
		
		for (BlocksState bState : stateCounter.keySet()) {
			double ratio = stateCounter.get(bState) / 10000.0;
			System.out.println(ratio);
			assertEquals(ratio, 1.0/13, 0.01);
		}
		
		
		
		bwe.initialise(0, new String[] { "4" });
		stateCounter.clear();
		for (int i = 0; i < 10000; i++) {
			bwe.initialiseBlocksState(4);
			Integer count = stateCounter.get(bwe.getState());
			if (count == null)
				count = 0;
			stateCounter.put(bwe.getState(), count + 1);
		}
		
		for (BlocksState bState : stateCounter.keySet()) {
			double ratio = stateCounter.get(bState) / 10000.0;
			System.out.println(ratio);
			assertEquals(ratio, 1.0/73, 0.005);
		}
		
		
		bwe.initialise(0, new String[] { "10" });
		stateCounter.clear();
		for (int i = 0; i < 10000; i++) {
			bwe.initialiseBlocksState(4);
			Integer count = stateCounter.get(bwe.getState());
			if (count == null)
				count = 0;
			stateCounter.put(bwe.getState(), count + 1);
		}
		
		for (BlocksState bState : stateCounter.keySet()) {
			double ratio = stateCounter.get(bState) / 10000.0;
			System.out.println(ratio);
			assertEquals(ratio, 1.0/73, 0.005);
		}
	}
}
