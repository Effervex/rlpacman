package test;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import blocksWorld.BlocksState;

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

}
