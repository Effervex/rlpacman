/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/test/BlocksWorldEnvironmentTest.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package test;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import relationalFramework.StateSpec;
import rrlFramework.RRLExperiment;

import blocksWorld.BlocksState;
import blocksWorld.BlocksWorldEnvironment;

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
