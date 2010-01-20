package blocksWorld;

import relationalFramework.State;

public class BlocksWorldState extends State {
	public static final int INT_STATE = 0;
	public static final int HIGHEST_BLOCK = 1;
	
	public BlocksWorldState(Object[] stateArray) {
		super(stateArray);
	}

	public Integer getHighestBlock() {
		return (Integer) getStateArray()[HIGHEST_BLOCK];
	}
	
	public Integer[] getIntState() {
		return (Integer[]) getStateArray()[INT_STATE];
	}
}
