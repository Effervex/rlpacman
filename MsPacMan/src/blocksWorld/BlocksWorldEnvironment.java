package blocksWorld;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import jess.Rete;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;

import cerrla.PolicyActor;
import cerrla.PolicyGenerator;

import relationalFramework.FiredAction;
import relationalFramework.PolicyActions;
import relationalFramework.ObjectObservations;
import relationalFramework.RelationalPolicy;
import relationalFramework.StateSpec;
import relationalFramework.RelationalPredicate;

/**
 * The environment for the blocks world interface.
 * 
 * @author Sam Sarjant
 */
public class BlocksWorldEnvironment implements EnvironmentInterface {
	/** The minimal reward the agent can receive. */
	private static final float MINIMAL_REWARD = -10;

	/** The number of blocks. Default 5. */
	private int numBlocks_ = 5;

	/** The probability of an action succeeding (assuming it is legal). */
	private double actionSuccess_ = 1.0;

	/** The maximum number of steps the agent is allocated. */
	private int maxSteps_;

	/** The state of the blocks world. */
	private BlocksState state_;

	/** The state of the blocks world in base predicates. */
	private Rete rete_;

	/** The number of steps taken. */
	private int steps_;

	/** The optimal number of steps for a state to be solved. */
	private Map<BlocksState, Integer> optimalMap_ = new HashMap<BlocksState, Integer>();

	/** The optimal number of steps. */
	private int optimalSteps_;

	/** If we're running an optimal agent. */
	private boolean optimal_ = false;

	/** Similar to debug mode, but only shows ASCII representation of blocks. */
	private boolean viewingMode_ = false;

	/** The arguments for the goal. */
	private List<String> goalArgs_;

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
		if (arg0.equals("maxSteps")) {
			maxSteps_ = (int) (numBlocks_ / actionSuccess_) + 1;
			return maxSteps_ + "";
		}
		if (arg0.equals("freeze")) {
			PolicyGenerator.getInstance().freeze(true);
			return null;
		}
		if (arg0.equals("unfreeze")) {
			PolicyGenerator.getInstance().freeze(false);
			return null;
		}
		if ((arg0.startsWith("goal"))) {
			StateSpec.reinitInstance(arg0.substring(5));
		} else if (arg0.startsWith("nonDet")) {
			actionSuccess_ = Double.parseDouble(arg0.substring(6));
		} else if (arg0.equals("-e"))
			viewingMode_ = true;
		else {
			try {
				numBlocks_ = Integer.parseInt(arg0);
				optimalMap_ = new HashMap<BlocksState, Integer>();
				return null;
			} catch (Exception e) {

			}
		}
		return null;
	}

	// @Override
	public Observation env_start() {
		rete_ = StateSpec.getInstance().getRete();
		// Generate a random blocks world
		state_ = initialiseWorld(numBlocks_);
		optimalSteps_ = optimalSteps();
		if (PolicyGenerator.debugMode_ || viewingMode_) {
			System.out.println("\tAgent:\n" + state_);
		}
		steps_ = 0;

		return formObs_Start();
	}

	/**
	 * Forms the observation for the first step of the experiment.
	 * 
	 * @return The (useless) observation. The real return is the singleton
	 *         ObjectObservation.
	 */
	private Observation formObs_Start() {
		Observation obs = new Observation();
		obs.charArray = ObjectObservations.OBSERVATION_ID.toCharArray();
		ObjectObservations.getInstance().predicateKB = rete_;
		ObjectObservations.getInstance().earlyExit = false;
		return obs;
	}

	@Override
	public Reward_observation_terminal env_step(Action arg0) {
		Observation obs = new Observation();
		obs.charArray = ObjectObservations.OBSERVATION_ID.toCharArray();
		// Check for an early exit
		if (ObjectObservations.getInstance().earlyExit) {
			return new Reward_observation_terminal(0, obs, true);
		}

		Collection<FiredAction> firedActions = ((PolicyActions) ObjectObservations
				.getInstance().objectArray[0]).getFirstActionList();
		RelationalPredicate action = null;
		if (firedActions != null) {
			List<FiredAction> actions = new ArrayList<FiredAction>(firedActions);
			FiredAction selectedAction = actions.get(PolicyGenerator.random_
					.nextInt(actions.size()));
			selectedAction.triggerRule();
			action = selectedAction.getAction();
		}

		// Action can fail
		BlocksState newState = state_;
		boolean actionFailed = false;
		if (PolicyGenerator.random_.nextDouble() < actionSuccess_)
			newState = actOnAction(action, state_);
		else
			actionFailed = true;

		if ((PolicyGenerator.debugMode_ || viewingMode_) && !optimal_) {
			if (action != null)
				System.out.println("\t" + action + " ->\n" + newState);
			else
				System.out.println("\t\t\tNo action chosen.");
		}

		// If our new state is different, update observations
		if (!state_.equals(newState)) {
			state_ = newState;
			formState(state_.getState());
		} else if (!actionFailed) {
			// If the agent caused the state to remain the same, exit the
			// episode with max negative reward.
			ObjectObservations.getInstance().setNoPreGoal();
			return new Reward_observation_terminal(MINIMAL_REWARD,
					new Observation(), true);
		}

		steps_++;
		ObjectObservations.getInstance().predicateKB = rete_;

		double reward = 0;
		boolean isGoal = StateSpec.getInstance().isGoal(rete_)
				|| ObjectObservations.getInstance().earlyExit;
		if (isGoal || steps_ == maxSteps_) {
			if (optimalSteps_ >= maxSteps_)
				reward = 0;
			else
				reward = MINIMAL_REWARD * (steps_ - optimalSteps_)
						/ (maxSteps_ - optimalSteps_);
		}
		Reward_observation_terminal rot = new Reward_observation_terminal(
				reward, obs, isGoal);

		return rot;
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
	private BlocksState actOnAction(RelationalPredicate action,
			BlocksState worldState) {
		if (action == null)
			return worldState;

		Integer[] newState = new Integer[worldState.length];

		// Finding the block objects
		int[] indices = null;
		if (action.getFactName().equals("move")) {
			indices = new int[2];
		} else {
			indices = new int[1];
		}

		// Convert the blocks to indices
		Integer[] stateArray = worldState.getState();
		for (int i = 0; i < indices.length; i++) {
			indices[i] = (action.getArguments()[i].charAt(0)) - ('a');
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
	 * Initialises the blocks world to a random, non-goal state.
	 * 
	 * @param numBlocks
	 *            The number of blocks in the world.
	 * @return The newly initialised blocks world state.
	 */
	private BlocksState initialiseWorld(int numBlocks) {
		// If we have some goal args (not empty ones), reset the goal args.
		if (goalArgs_ != null && !goalArgs_.isEmpty())
			goalArgs_ = null;
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

		// Set the goal
		int[] params = null;
		String goalName = StateSpec.getInstance().getGoalName();
		if (goalName.equals("onAB")) {
			boolean valid = false;
			while (!valid) {
				params = new int[2];
				params[0] = PolicyGenerator.random_.nextInt(numBlocks) + 1;
				params[1] = PolicyGenerator.random_.nextInt(numBlocks) + 1;
				// Cannot be the same block, and cannot already be achieved.
				valid = (params[0] != params[1])
						&& (worldState[params[0] - 1] != params[1]);
			}
		} else if (goalName.equals("clearA")) {
			params = new int[1];
			List<Integer> unclears = new ArrayList<Integer>();
			for (int block : worldState)
				if (block != 0)
					unclears.add(block);
			if (unclears.isEmpty())
				return initialiseWorld(numBlocks);

			params[0] = unclears.get(PolicyGenerator.random_.nextInt(unclears
					.size()));
		}

		if (params != null) {
			goalArgs_ = new ArrayList<String>(params.length);
			for (int param : params)
				goalArgs_.add((char) ('a' + (param - 1)) + "");
		}

		// Check this isn't the goal state
		boolean validWorld = formState(worldState);
		if (!validWorld)
			return initialiseWorld(numBlocks);

		return new BlocksState(worldState);
	}

	/**
	 * Forms the knowledge base of the state using the int array approximation.
	 * 
	 * @param worldState
	 *            The state of the world in int form.
	 */
	private boolean formState(Integer[] worldState) {
		try {
			// Clear the old state
			rete_.reset();

			// Scanning through, making predicates (On, OnFloor, and Highest)
			int[] heightMap = new int[worldState.length];
			int maxHeight = 0;
			List<Integer> highestBlocks = new ArrayList<Integer>();
			List<Integer> allBlocks = new ArrayList<Integer>();
			for (int i = 0; i < worldState.length; i++) {
				// On the floor
				if (worldState[i] == 0) {
					rete_.assertString("(onFloor " + (char) ('a' + i) + "))");
				} else {
					// On another block
					rete_.assertString("(on " + (char) ('a' + i) + " "
							+ (char) ('a' + worldState[i] - 1) + "))");
				}
				allBlocks.add(i);

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
					highestBlocks.add(i);
				}

				// Assert the blocks
				rete_.assertString("(block " + (char) ('a' + i) + "))");
			}

			// Add the highest block/s
			for (Integer block : highestBlocks) {
				rete_.assertString("(highest " + (char) ('a' + block) + "))");
			}

			// Asserting the highest goal
			if (StateSpec.getInstance().getGoalName().equals("highestA")) {
				if (goalArgs_ == null) {
					goalArgs_ = new ArrayList<String>(1);
					allBlocks.removeAll(highestBlocks);
					if (allBlocks.isEmpty())
						return false;
					goalArgs_.add((char) ('a' + allBlocks
							.get(PolicyGenerator.random_.nextInt(allBlocks
									.size())))
							+ "");
				} else
					goalArgs_ = new ArrayList<String>();
			}

			// Add the goal
			StateSpec.getInstance().assertGoalPred(goalArgs_, rete_);
			rete_.run();

			// Adding the valid actions
			ObjectObservations.getInstance().validActions = StateSpec
					.getInstance().generateValidActions(rete_);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
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
		RelationalPolicy optimalPolicy = StateSpec.getInstance()
				.getHandCodedPolicy();
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
		ObjectObservations.getInstance().objectArray = new RelationalPolicy[] { optimalPolicy };
		optimalAgent.agent_message("Optimal");
		optimalAgent.agent_message("SetPolicy");
		double oldActionSuccess = actionSuccess_;
		actionSuccess_ = 1.0;
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

		// Return the state to normal
		state_ = initialState;
		actionSuccess_ = oldActionSuccess;
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
			char[][] blocksChars = new char[numBlocks_ + 1][numBlocks_];
			Map<Integer, Point> posMap = new HashMap<Integer, Point>();
			int column = 0;
			int i = 0;
			while (posMap.size() < numBlocks_) {
				column = recursiveBuild(i, intState_, column, posMap,
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
