package blocksWorldActive;

import java.awt.Point;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import blocksWorld.BlocksState;

/**
 * The blocks state containing active and non-active blocks.
 * 
 * @author Sam Sarjant
 */
public class ActiveBlocksState extends BlocksState {
	private boolean[] activeBlocks_;

	public ActiveBlocksState(Integer[] state) {
		super(state);
		activeBlocks_ = new boolean[state.length];
	}

	public ActiveBlocksState(Integer[] state, boolean[] activeBlocks) {
		super(state);
		activeBlocks_ = activeBlocks;
	}
	
	public boolean[] getActiveBlocks() {
		return activeBlocks_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(activeBlocks_);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ActiveBlocksState other = (ActiveBlocksState) obj;
		if (!Arrays.equals(activeBlocks_, other.activeBlocks_))
			return false;
		return true;
	}

	@Override
	public ActiveBlocksState clone() {
		BlocksState bs = super.clone();
		boolean[] activeClone = new boolean[activeBlocks_.length];
		for (int i = 0; i < activeBlocks_.length; i++)
			activeClone[i] = activeBlocks_[i];
		return new ActiveBlocksState(bs.getState(), activeClone);
	}

	@Override
	public String toString() {
		// The last column of the blocksChars denotes if there is a block in
		// the row.
		char[][] blocksChars = new char[length + 1][length];
		Map<Integer, Point> posMap = new HashMap<Integer, Point>();
		int column = 0;
		int i = 0;
		while (posMap.size() < length) {
			column = recursiveBuild(i, getState(), column, posMap, blocksChars);
			i++;
		}

		// Print the char map
		StringBuffer buffer = new StringBuffer();
		for (int y = length - 1; y >= 0; y--) {
			if (blocksChars[length][y] == '+') {
				buffer.append("\t\t");
				for (int x = 0; x < column; x++) {
					if (blocksChars[x][y] == 0)
						buffer.append("   ");
					else if (activeBlocks_[blocksChars[x][y] - 'a'])
						buffer.append("<" + blocksChars[x][y] + ">");
					else
						buffer.append("[" + blocksChars[x][y] + "]");
				}

				if (y != 0)
					buffer.append("\n");
			}
		}
		return buffer.toString();
	}
}
