package blocksWorldActive;

import java.awt.Point;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import jess.Rete;

import relationalFramework.RelationalPredicate;

/**
 * The environment for the blocks world interface.
 * 
 * @author Sam Sarjant
 */
public class BlocksWorldEnvironment extends
		blocksWorldMove.BlocksWorldEnvironment {
	public String env_message(String arg0) {
		String result = super.env_message(arg0);
		if (arg0.equals("maxSteps")) {
			maxSteps_ = (int) (2 * numBlocks_ / actionSuccess_) + 1;
			result = (maxSteps_ + 1) + "";
		}
		return result;
	}

	@Override
	protected BlocksState initialiseWorld(int numBlocks) {
		BlocksState bs = super.initialiseWorld(numBlocks);
//		boolean[] activeBlocks = new boolean[numBlocks];
//		for (int i = 0; i < numBlocks; i++) {
//			activeBlocks[i] = PolicyGenerator.random_.nextBoolean();
//		}
		return new ActiveBlocksState(bs.getState());
	}

	@Override
	protected BlocksState actOnAction(RelationalPredicate action,
			BlocksState worldState) {
		if (action == null)
			return worldState;

		ActiveBlocksState newState = ((ActiveBlocksState) worldState).clone();

		if (action.getFactName().equals("move")) {
			// Finding the block objects
			int[] indices = new int[2];

			// Convert the blocks to indices
			Integer[] stateArray = worldState.getState();
			for (int i = 0; i < indices.length; i++) {
				if (action.getArguments()[i].equals("floor")) {
					indices[i] = -1;
					// Cannot move the floor
					if (i == 0)
						return worldState;
				} else
					indices[i] = (action.getArguments()[i].charAt(0)) - ('a');
				// In order to do either action, both blocks must be free
				for (int j = 0; j < stateArray.length; j++) {
					// If something is on that index/block (except the floor),
					// return the unchanged state
					if (indices[i] != -1 && stateArray[j] == indices[i] + 1)
						return worldState;
				}
			}

			// Perform the action
			newState.getState()[indices[0]] = indices[1] + 1;
		} else if (action.getFactName().equals("toggle")) {
			int index = action.getArguments()[0].charAt(0) - 'a';
			newState.activeBlocks_[index] = !newState.activeBlocks_[index];
		}
		return newState;
	}

	@Override
	protected boolean assertFacts(BlocksState blocksState, Rete rete)
			throws Exception {
		boolean result = super.assertFacts(blocksState, rete);
		if (!result)
			return result;

		if (blocksState instanceof ActiveBlocksState) {
			ActiveBlocksState activeBlocksState = (ActiveBlocksState) blocksState;
			try {
				for (int i = 0; i < activeBlocksState.activeBlocks_.length; i++) {
					if (activeBlocksState.activeBlocks_[i])
						rete_.assertString("(active " + (char) ('a' + i) + ")");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return result;
	}

	public class ActiveBlocksState extends BlocksState {
		private boolean[] activeBlocks_;

		public ActiveBlocksState(Integer[] state) {
			super(state);
			activeBlocks_ = new boolean[numBlocks_];
		}

		public ActiveBlocksState(Integer[] state, boolean[] activeBlocks) {
			super(state);
			activeBlocks_ = activeBlocks;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
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
			if (!getOuterType().equals(other.getOuterType()))
				return false;
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

		private BlocksWorldEnvironment getOuterType() {
			return BlocksWorldEnvironment.this;
		}

		@Override
		public String toString() {
			// The last column of the blocksChars denotes if there is a block in
			// the row.
			char[][] blocksChars = new char[numBlocks_ + 1][numBlocks_];
			Map<Integer, Point> posMap = new HashMap<Integer, Point>();
			int column = 0;
			int i = 0;
			while (posMap.size() < numBlocks_) {
				column = recursiveBuild(i, getState(), column, posMap,
						blocksChars);
				i++;
			}

			// Print the char map
			StringBuffer buffer = new StringBuffer();
			for (int y = numBlocks_ - 1; y >= 0; y--) {
				if (blocksChars[numBlocks_][y] == '+') {
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
}
