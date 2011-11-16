package blocksWorldBounded;

import blocksWorld.BlocksState;

/**
 * The environment for the blocks world interface.
 * 
 * @author Sam Sarjant
 */
public class BlocksWorldEnvironment extends
		blocksWorldMove.BlocksWorldEnvironment {
	public String env_init() {
		wrapper_ = new BlocksWorldRelationalWrapper();
		return null;
	}
	
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
		return new BoundedBlocksState(bs.getState());
	}
}
