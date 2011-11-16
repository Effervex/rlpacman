package blocksWorldBounded;

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
public class BoundedBlocksState extends BlocksState {
	private int[] boundBlocks_;

	public BoundedBlocksState(Integer[] state) {
		super(state);
		boundBlocks_ = new int[state.length];
	}

	public BoundedBlocksState(Integer[] state, int[] boundBlocks) {
		super(state);
		boundBlocks_ = boundBlocks;
	}
	
	public int[] getBoundBlocks() {
		return boundBlocks_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(boundBlocks_);
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
		BoundedBlocksState other = (BoundedBlocksState) obj;
		if (!Arrays.equals(boundBlocks_, other.boundBlocks_))
			return false;
		return true;
	}

	@Override
	public BoundedBlocksState clone() {
		BlocksState bs = super.clone();
		int[] boundClone = new int[boundBlocks_.length];
		for (int i = 0; i < boundBlocks_.length; i++)
			boundClone[i] = boundBlocks_[i];
		return new BoundedBlocksState(bs.getState(), boundClone);
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
					else if (boundBlocks_[blocksChars[x][y] - 'a'] != 0) {
						char boundBlock = (char) (boundBlocks_[blocksChars[x][y] - 'a'] + 'a');
						buffer.append("" + boundBlock + blocksChars[x][y] + boundBlock);
					} else
						buffer.append("[" + blocksChars[x][y] + "]");
				}

				if (y != 0)
					buffer.append("\n");
			}
		}
		return buffer.toString();
	}
}
