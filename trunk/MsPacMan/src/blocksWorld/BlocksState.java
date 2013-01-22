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
 *    src/blocksWorld/BlocksState.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
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

	/**
	 * Creates a new block state from an integer representation.
	 * 
	 * @param state
	 *            The integer representation of the state.
	 */
	public BlocksState(Integer[] state) {
		intState_ = state;
		length = state.length;
	}

	/**
	 * Gets the integer representation of the state.
	 * 
	 * @return The state in integer form.
	 */
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

	/**
	 * Prints the state in an ASCII graphical format, using characters 'a' to
	 * 'z' for the blocks.
	 */
	@Override
	public String toString() {
		// The last column of the blocksChars denotes if there is a block in
		// the row.
		String[][] blocksChars = new String[length + 1][length];
		Map<Integer, Point> posMap = new HashMap<Integer, Point>();
		int column = 0;
		int i = 0;
		while (posMap.size() < length) {
			column = recursiveBuildString(i, intState_, column, posMap,
					blocksChars);
			i++;
		}

		// Print the char map
		StringBuffer buffer = new StringBuffer();
		for (int y = length - 1; y >= 0; y--) {
			if (blocksChars[length][y] != null
					&& blocksChars[length][y].equals("+")) {
				buffer.append("\t\t");
				for (int x = 0; x < column; x++) {
					if (blocksChars[x][y] == null)
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
	 * Builds the blocks state String recursively.
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
	protected int recursiveBuildString(int currBlock, Integer[] blocks,
			int column, Map<Integer, Point> posMap, String[][] blocksChars) {
		if (!posMap.containsKey(currBlock)) {
			if (blocks[currBlock] == 0) {
				posMap.put(currBlock, new Point(column, 0));
				blocksChars[column][0] = "b" + currBlock;
				blocksChars[blocks.length][0] = "+";
				column++;
			} else {
				int underBlock = blocks[currBlock] - 1;
				column = recursiveBuildString(underBlock, blocks, column,
						posMap, blocksChars);
				Point pos = new Point(posMap.get(underBlock));
				pos.y++;
				posMap.put(currBlock, pos);
				blocksChars[pos.x][pos.y] = "b" + currBlock;
				blocksChars[blocks.length][pos.y] = "+";
			}
		}
		return column;
	}
}