package blocksWorld;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import blocksWorld.BlocksWorldEnvironment.State;

public class BlocksWorldEnvironmentTest {

	@Test
	public void testStateEquals() {
		BlocksWorldEnvironment env = new BlocksWorldEnvironment();
		Map<State, Integer> map = new HashMap<State, Integer>();
		State state1 = env.new State(new Integer[] { 0, 0, 0, 2, 1 });
		map.put(state1, 3);
		State state2 = env.new State(new Integer[] { 0, 0, 0, 2, 1 });
		assertTrue(state2.equals(state1));
		assertTrue(map.containsKey(state2));
	}

}
