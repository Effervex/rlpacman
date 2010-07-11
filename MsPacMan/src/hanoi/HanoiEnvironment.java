package hanoi;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

import jess.Rete;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;

import relationalFramework.ActionChoice;
import relationalFramework.MultiMap;
import relationalFramework.ObjectObservations;
import relationalFramework.Policy;
import relationalFramework.PolicyActor;
import relationalFramework.PolicyGenerator;
import relationalFramework.RuleAction;
import relationalFramework.StateSpec;

/**
 * The environment for the blocks world interface.
 * 
 * @author Sam Sarjant
 */
public class HanoiEnvironment implements EnvironmentInterface {
	/** A constant for learning. */
	public static final int STEP_CONSTANT = 2;

	/** The number of towers in the problem. */
	public static final int NUM_TOWERS = 3;

	/** The state of the hanoi problem. Start with just 3 towers. */
	private HanoiState state_;

	/** The number of tiles the tower is made of. */
	private int numTiles_ = 3;

	/** The state of the blocks world in base predicates. */
	private Rete rete_;

	/** The number of steps the agent took. */
	private int steps_ = 0;

	/** The minimum number of optimal steps to solve the environment. */
	private int optimalSteps_ = 0;

	// @Override
	public void env_cleanup() {
		rete_ = null;
		state_ = null;
	}

	// @Override
	public String env_init() {
		return null;
	}

	// @Override
	public String env_message(String arg0) {
		if (arg0.equals("maxSteps"))
			return (numTiles_ * STEP_CONSTANT + 1) + "";
		if (arg0.equals("freeze")) {
			PolicyGenerator.getInstance().freeze(true);
			return null;
		}
		if (arg0.equals("unfreeze")) {
			PolicyGenerator.getInstance().freeze(false);
			return null;
		}
		if ((arg0.length() > 4) && (arg0.substring(0, 4).equals("goal"))) {
			StateSpec.reinitInstance(arg0.substring(5));
		}
		try {
			numTiles_ = Integer.parseInt(arg0);
			return null;
		} catch (Exception e) {

		}
		return null;
	}

	// @Override
	public Observation env_start() {
		rete_ = StateSpec.getInstance().getRete();
		// Generate a random blocks world
		state_ = initialiseHanoi(numTiles_);
		optimalSteps_ = optimalSteps();
		if (PolicyGenerator.debugMode_) {
			System.out.println("\tAgent:\n" + state_);
		}
		steps_ = 0;

		return formObservation();
	}

	/**
	 * Initialises the Hanoi state.
	 * 
	 * @param numTiles
	 *            The number of tiles in the Hanoi environment.
	 * @return The Hanoi state with all tiles in one stack on the left.
	 */
	private HanoiState initialiseHanoi(int numTiles) {
		return new HanoiState(numTiles);
	}

	@Override
	public Reward_observation_terminal env_step(Action arg0) {
		RuleAction ruleAction = ((ActionChoice) ObjectObservations
				.getInstance().objectArray[0]).getFirstActionList();
		List<String> actions = ruleAction.getTriggerActions();
		String action = actions.get(PolicyGenerator.random_.nextInt(actions
				.size()));

		BlocksState newState = actOnAction(action, state_);
		if (PolicyGenerator.debugMode_ && !optimal_) {
			if (action != null)
				System.out.println("\t" + action + " ->\n" + newState);
			else
				System.out.println("\t\t\tNo action chosen.");
		}

		double nonOptimalSteps = numBlocks_ * STEP_CONSTANT - optimalSteps_;
		Observation obs = new Observation();
		obs.charArray = ObjectObservations.OBSERVATION_ID.toCharArray();
		// If our new state is different, update observations
		if (!state_.equals(newState)) {
			state_ = newState;
			formState(state_.getState());
		} else {
			double excess = (steps_ > optimalSteps_) ? steps_ - optimalSteps_
					: 0;
			ObjectObservations.getInstance().setNoPreGoal();
			return new Reward_observation_terminal(MINIMAL_REWARD
					+ (excess * -MINIMAL_REWARD) / nonOptimalSteps,
					new Observation(), true);
		}

		steps_++;
		ObjectObservations.getInstance().predicateKB = rete_;

		double reward = (steps_ <= optimalSteps_) ? 0 : MINIMAL_REWARD
				/ nonOptimalSteps;
		Reward_observation_terminal rot = new Reward_observation_terminal(
				reward, obs, StateSpec.getInstance().isGoal(rete_));

		return rot;
	}
	
	/**
	 * Form the (useless) observation object and rete object.
	 * 
	 * @return The (useless) observation.
	 */
	private Observation formObservation() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Acts on the action given. (e.g. 'move a b' 'moveFloor z')
	 * 
	 * @param action
	 *            The action to act upon.
	 * @param worldState
	 *            The old state of the world, before the action.
	 * @return The state of the new world.
	 */
	private BlocksState actOnAction(String action, BlocksState worldState) {
		if (action == null)
			return worldState;

		Integer[] newState = new Integer[worldState.length];

		String[] split = StateSpec.splitFact(action);

		// Finding the block objects
		int[] indices = null;
		if (split[0].equals("move")) {
			indices = new int[2];
		} else {
			indices = new int[1];
		}

		// Convert the blocks to indices
		Integer[] stateArray = worldState.getState();
		for (int i = 0; i < indices.length; i++) {
			indices[i] = (split[i + 1].charAt(0)) - ('a');
			// In order to do either action, both blocks must be free
			for (int j = 0; j < worldState.length; j++) {
				newState[j] = stateArray[j];
				// If something is on that index/block, return the unchanged
				// state
				if (stateArray[j] - 1 == indices[i])
					return worldState;
			}
		}

		// Perform the action
		if (indices.length == 1) {
			newState[indices[0]] = 0;
		} else if (indices[0] != indices[1]) {
			newState[indices[0]] = indices[1] + 1;
		}

		return new BlocksState(newState);
	}

	/**
	 * Creates the block terms.
	 * 
	 * @param numBlocks
	 *            The number of blocks.
	 * @return The blocks array.
	 */
	private Block[] createBlocks(int numBlocks) {
		Block[] blocks = new Block[numBlocks];
		for (int i = 0; i < numBlocks; i++) {
			String name = (char) ('a' + i) + "";
			blocks[i] = new Block(name);
		}

		return blocks;
	}

	/**
	 * Initialises the blocks world to a random, non-goal state.
	 * 
	 * @param numBlocks
	 *            The number of blocks in the world.
	 * @param goalState
	 *            The goal state.
	 * @return The newly initialised blocks world state.
	 */
	private BlocksState initialiseWorld(int numBlocks, String goalState) {
		Random random = PolicyGenerator.random_;
		Integer[] worldState = new Integer[numBlocks];
		List<Double> contourState = new ArrayList<Double>();
		contourState.add(0d);
		List<Integer> blocksLeft = new ArrayList<Integer>();
		for (int i = 1; i <= numBlocks; i++) {
			blocksLeft.add(i);
		}

		while (!blocksLeft.isEmpty()) {
			// Get a random block
			Integer block = blocksLeft
					.remove(random.nextInt(blocksLeft.size()));

			// Put the block in a random position, influenced by the number of
			// free blocks.
			int index = random.nextInt(contourState.size());
			worldState[block - 1] = contourState.get(index).intValue();
			if (worldState[block - 1] == 0) {
				contourState.add(new Double(block));
			} else {
				contourState.set(index, new Double(block));
			}
		}

		// Check this isn't the goal state
		formState(worldState);
		if (StateSpec.getInstance().isGoal(rete_))
			return initialiseWorld(numBlocks, goalState);
		else
			return new BlocksState(worldState);
	}

	/**
	 * Forms the knowledge base of the state using the int array approximation.
	 * 
	 * @param worldState
	 *            The state of the world in int form.
	 */
	private void formState(Integer[] worldState) {
		try {
			// Clear the old state
			rete_.reset();

			// Scanning through, making predicates (On, OnFloor, and Highest)
			int[] heightMap = new int[worldState.length];
			int maxHeight = 0;
			List<Block> highestBlocks = new ArrayList<Block>();
			for (int i = 0; i < worldState.length; i++) {
				// On the floor
				if (worldState[i] == 0) {
					rete_.eval("(assert (onFloor " + blocks_[i].getName()
							+ "))");
				} else {
					// On another block
					rete_.eval("(assert (on " + blocks_[i].getName() + " "
							+ blocks_[worldState[i] - 1].getName() + "))");
				}

				// Finding the heights
				int blockHeight = heightMap[i];
				if (blockHeight == 0) {
					blockHeight = recurseHeight(i, heightMap, worldState);
				}
				if (blockHeight > maxHeight) {
					maxHeight = blockHeight;
					highestBlocks.clear();
				}
				if (blockHeight == maxHeight) {
					highestBlocks.add(blocks_[i]);
				}

				// Assert the blocks
				rete_.eval("(assert (block " + blocks_[i].getName() + "))");
			}

			// Add the highest block/s
			for (Block block : highestBlocks) {
				rete_.eval("(assert (highest " + block.getName() + "))");
			}

			rete_.run();

			// Adding the valid actions
			ObjectObservations.getInstance().validActions = StateSpec
					.getInstance().generateValidActions(rete_);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Finds the height of a block recursively by following a path. Stores the
	 * values.
	 * 
	 * @param start
	 *            The starting index to check.
	 * @param heightMap
	 *            The stored heightMap.
	 * @param worldState
	 *            The state of the world in block links.
	 * @return The maximum height of the block stack.
	 */
	private int recurseHeight(int start, int[] heightMap, Integer[] worldState) {
		if (worldState[start] == 0) {
			heightMap[start] = 1;
			return 1;
		}

		int below = worldState[start] - 1;
		recurseHeight(below, heightMap, worldState);
		heightMap[start] = heightMap[below] + 1;
		return heightMap[start];
	}

	/**
	 * Calculates the optimal number of steps to solve the problem.
	 * 
	 * @return The minimal number of steps to take for solving.
	 */
	private int optimalSteps() {
		Policy optimalPolicy = StateSpec.getInstance().getOptimalPolicy();
		steps_ = 0;

		// Check it hasn't already solved the state
		if (optimalMap_.containsKey(state_)) {
			// System.out.println("\t\t\tAlready calculated ("
			// + optimalMap_.get(state_) + ")");
			return optimalMap_.get(state_);
		}

		optimal_ = true;
		BlocksState initialState = state_.clone();
		// Run the policy through the environment until goal is satisfied.
		PolicyActor optimalAgent = new PolicyActor();
		ObjectObservations.getInstance().objectArray = new Policy[] { optimalPolicy };
		optimalAgent.agent_message("Optimal");
		optimalAgent.agent_message("Policy");
		Action act = optimalAgent.agent_start(formObs_Start());
		// Loop until the task is complete
		Reward_observation_terminal rot = env_step(act);
		while ((rot == null) || (!rot.isTerminal())) {
			// Check if the optimal policy has already seen this state
			if (optimalMap_.containsKey(state_)) {
				steps_ += optimalMap_.get(state_);
				break;
			}
			optimalAgent.agent_step(rot.r, rot.o);
			rot = env_step(act);
		}

		// Form the first pre-goal.
		if (!PolicyGenerator.getInstance().hasPreGoal())
			optimalAgent.agent_message("formPreGoal");

		// Return the state to normal
		state_ = initialState;
		formState(state_.getState());
		optimalMap_.put(state_, steps_);
		optimal_ = false;
		return steps_;
	}

	/**
	 * A wrapper class for blocks world states
	 * 
	 * @author Samuel J. Sarjant
	 */
	public class HanoiState {
		private Stack<Character>[] tileState_;

		public HanoiState(int numTiles) {
			// TODO Could just have random start state
			tileState_ = new Stack[NUM_TOWERS];
			for (int i = 0; i < NUM_TOWERS; i++) {
				tileState_[i] = new Stack<Character>();

				// Initial tower
				if (i == 0) {
					// Place each tile
					for (int j = numTiles - 1; j >= 0; j--) {
						char tile = (char) ('a' + j);
						tileState_[i].push(tile);
					}
				}
			}
		}

		public Stack<Character>[] getState() {
			return tileState_;
		}

		@Override
		public String toString() {
			// Include the opening and closing brackets
			int maxTileWidth = numTiles_ + 2;

			// Run through the stacks, from the top
			StringBuffer buffer = new StringBuffer();
			for (int n = numTiles_ - 1; n >= 0; n--) {
				// Maintain a temp buffer - only used if there are any tiles at
				// this height
				StringBuffer tempBuffer = new StringBuffer();
				boolean usedLevel = false;
				for (int i = 0; i < NUM_TOWERS; i++) {
					int charCount = 0;
					if (tileState_[i].size() <= n) {
						usedLevel = true;
						tempBuffer.append('[');
						char tile = tileState_[i].get(n);
						int tileSize = tile - 'a';
						tempBuffer.append(tile);

						for (int s = 0; s < tileSize - 1; s++)
							tempBuffer.append(' ');

						tempBuffer.append(']');
						charCount += tileSize + 2;
					}

					// Add empty space to buffer tiles
					for (; charCount < maxTileWidth; charCount++)
						tempBuffer.append(' ');
				}

				if (usedLevel)
					buffer.append(tempBuffer + "\n");
			}

			// Adding the guidelines
			for (int i = 0; i < NUM_TOWERS; i++) {
				for (int s = 0; s < maxTileWidth - 1; s++)
					buffer.append(' ');
				buffer.append('|');
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
		 *            The output character map, with an extra column for
		 *            denoting if a row has any blocks in it.
		 * @return The new value of column (same or + 1).
		 */
		private int recursiveBuild(int currBlock, Integer[] blocks, int column,
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
}
