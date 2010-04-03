package blocksWorld;

import relationalFramework.State;

public enum BlocksWorldState implements State {
	INT_STATE, HIGHEST_BLOCK;
	
	public static Integer getHighestBlock(Object[] observations) {
		return (Integer) observations[HIGHEST_BLOCK.ordinal()];
	}
	
	public static Integer[] getIntState(Object[] observations) {
		return (Integer[]) observations[INT_STATE.ordinal()];
	}
}
