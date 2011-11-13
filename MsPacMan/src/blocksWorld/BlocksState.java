package blocksWorld;

import java.awt.Point;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper class for blocks world states
 * 
 * @author Samuel J. Sarjant
 */
public class BlocksState {
	private Integer[] intState_;
	public int length;

	public BlocksState(Integer[] state) {
		intState_ = state;
		length = state.length;
	}

	public Integer[] getState() {
		return intState_;
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj != null) && (obj instanceof BlocksState)) {
			BlocksState other = (BlocksState) obj;
			if (Arrays.equals(intState_, other.intState_))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		int sum = 0;
		for (int i = 0; i < intState_.length; i++) {
			sum += intState_[i] * 6451;
		}
		return sum;
	}

	@Override
	public BlocksState clone() {
		Integer[] cloneState = new Integer[intState_.length];
		for (int i = 0; i < intState_.length; i++) {
			cloneState[i] = intState_[i];
		}
		return new BlocksState(cloneState);
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
			column = recursiveBuild(i, intState_, column, posMap, blocksChars);
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
					else
						buffer.append("[" + blocksChars[x][y] + "]");
				}

				if (y != 0)
					buffer.append("\n");
			}
		}
		return buffer.toString();
	}

	/**
	 * Builds the blocks state recursively.
	 * 
	 * @param currBlock
	 *            The current block index.
	 * @param blocks
	 *            The locations of the blocks, in block index form.
	 * @param column
	 *            The first empty column
	 * @param posMap
	 *            The position mapping for each block.
	 * @param blocksChars
	 *            The output character map, with an extra column for denoting if
	 *            a row has any blocks in it.
	 * @return The new value of column (same or + 1).
	 */
	protected int recursiveBuild(int currBlock, Integer[] blocks, int column,
			Map<Integer, Point> posMap, char[][] blocksChars) {
		if (!posMap.containsKey(currBlock)) {
			if (blocks[currBlock] == 0) {
				posMap.put(currBlock, new Point(column, 0));
				blocksChars[column][0] = (char) ('a' + currBlock);
				blocksChars[blocks.length][0] = '+';
				column++;
			} else {
				int underBlock = blocks[currBlock] - 1;
				column = recursiveBuild(underBlock, blocks, column, posMap,
						blocksChars);
				Point pos = new Point(posMap.get(underBlock));
				pos.y++;
				posMap.put(currBlock, pos);
				blocksChars[pos.x][pos.y] = (char) ('a' + currBlock);
				blocksChars[blocks.length][pos.y] = '+';
			}
		}
		return column;
	}
}