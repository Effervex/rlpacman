package blocksWorldMove;

import relationalFramework.PolicyActions;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;
import rrlFramework.RRLEnvironment;
import rrlFramework.RRLExperiment;
import rrlFramework.RRLObservations;
import util.MultiMap;
import util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jess.Rete;

/**
 * The environment for the blocks world interface.
 * 
 * @author Sam Sarjant
 */
public class BlocksWorldEnvironment extends RRLEnvironment {
	/** The minimal reward the agent can receive. */
	private static final float MINIMAL_REWARD = -10;

	private List<String> goalArgs_;

	/** If we're running an optimal agent. */
	private boolean optimal_ = false;

	/** The optimal number of steps. */
	private int optimalSteps_;

	/** Similar to debug mode, but only shows ASCII representation of blocks. */
	private boolean viewingMode_ = false;

	/** The probability of an action succeeding (assuming it is legal). */
	protected double actionSuccess_ = 1.0;

	/** The maximum number of steps the agent is allocated. */
	protected int maxSteps_;

	/** The number of blocks. Default 5. */
	protected int numBlocks_ = 5;

	/** The state of the blocks world. */
	protected BlocksState state_;

	/** The number of steps taken. */
	protected int steps_;

	/** The block ratios pre-calculated, using [ungrounded][grounded] index. */
	private float[][] blockRatios_;

	/** The BWStates ratio 1d array (use pos()). */
	private float[] ratio;

	/**
	 * Calculates the optimal number of steps to solve the problem.
	 * 
	 * @return The minimal number of steps to take for solving.
	 */
	private int optimalSteps() {
		RelationalPolicy optimalPolicy = StateSpec.getInstance()
				.getHandCodedPolicy();
		steps_ = 0;

		optimal_ = true;
		optimalSteps_ = 0;
		BlocksState initialState = state_.clone();
		// Run the policy through the environment until goal is satisfied.
		double oldActionSuccess = actionSuccess_;
		actionSuccess_ = 1.0;

		// Loop until the task is complete
		int numActions = StateSpec.getInstance().getNumReturnedActions();
		RRLObservations obs = startEpisode();
		while (!obs.isTerminal()) {
			// Apply the policy
			PolicyActions actions = optimalPolicy.evaluatePolicy(obs,
					numActions);
			obs = step(actions);
		}

		// Return the state to normal
		state_ = initialState;
		actionSuccess_ = oldActionSuccess;
		optimal_ = false;
		return steps_;
	}

	@Override
	protected void assertStateFacts(Rete rete, List<String> goalArgs)
			throws Exception {
		if (steps_ != 0) {
			// Apply the action
			rete.run();
			return;
		}

		Integer[] intState = state_.getState();

		// Scanning through, making predicates (On)
		for (int i = 0; i < state_.length; i++) {
			// On the floor
			if (intState[i] == 0) {
				rete.assertString("(on " + (char) ('a' + i) + " floor)");
			} else {
				// On another block
				rete.assertString("(on " + (char) ('a' + i) + " "
						+ (char) ('a' + intState[i] - 1) + ")");
			}

			// Assert the blocks
			rete.assertString("(block " + (char) ('a' + i) + ")");
		}
	}

	@Override
	protected double[] calculateReward(boolean isTerminal) {
		double[] reward = new double[2];
		if (isTerminal || steps_ == maxSteps_) {
			if (optimalSteps_ >= maxSteps_)
				reward[0] = 0;
			else
				reward[0] = MINIMAL_REWARD * (steps_ - optimalSteps_)
						/ (maxSteps_ - optimalSteps_);
			
			// Adjust to 0..1
			reward[0] = -1 * (reward[0] - MINIMAL_REWARD) / MINIMAL_REWARD;
		}
		reward[1] = reward[0];
		return reward;
	}

	@Override
	protected List<String> getGoalArgs() {
		return goalArgs_;
	}

	@Override
	protected Object groundActions(PolicyActions actions) {
		RelationalPredicate action = null;
		if (!actions.isEmpty())
			action = actions.getFirstRandomAction();

		// Select a random action
		if (action == null) {
			// return state_;
			// Select random action predicate
			MultiMap<String, String[]> validActions = observations_
					.getValidActions();
			Object[] moveArgs = null;
			try {
				moveArgs = validActions.getSortedSet("move").toArray();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}

			Object randomAction = moveArgs[RRLExperiment.random_
					.nextInt(moveArgs.length)];
			action = new RelationalPredicate(StateSpec.getInstance()
					.getActions().get("move"), (String[]) randomAction);
		}

		// Assert the action to the Rete object.
		try {
			StateSpec.getInstance().getRete().assertString(action.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}

		BlocksState newState = state_.clone();

		// Finding the block objects
		int[] indices = new int[2];

		// Convert the blocks to indices
		for (int i = 0; i < indices.length; i++) {
			if (action.getArguments()[i].equals("floor"))
				indices[i] = -1;
			else
				indices[i] = (action.getArguments()[i].charAt(0)) - ('a');
		}

		// Perform the action
		newState.getState()[indices[0]] = indices[1] + 1;

		return new Pair<BlocksState, RelationalPredicate>(newState, action);
	}

	/**
	 * Initialises the blocks world to a random, non-goal state.
	 * 
	 * @param numBlocks
	 *            The number of blocks in the world.
	 * @param goalName
	 *            The goal name.
	 */
	@SuppressWarnings("unchecked")
	protected void initialiseBlocksState(int numBlocks) {
		Pair<Integer, Integer>[] rooted = new Pair[numBlocks];
		Pair<Integer, Integer>[] floating = new Pair[numBlocks];
		Integer[] worldState = new Integer[numBlocks];

		for (int x = 0; x < numBlocks; x++) {
			rooted[x] = new Pair<Integer, Integer>(-1, -1);
			floating[x] = new Pair<Integer, Integer>(x, x);
			worldState[x] = 0;
		} /* Initially, each block is a floating tower */
		int nrt = 0;
		int nft = numBlocks;

		while (nft-- != 0) {
			float r = RRLExperiment.random_.nextFloat();
			int choice = nft + nrt;
			float rat = Ratio(ratio,numBlocks,nft,nrt);
			float p = rat / (rat + choice);
			if (r <= p) { /* Put the next block on the table */
				rooted[nrt].objA_ = floating[nft].objA_;
				rooted[nrt].objB_ = floating[nft].objB_;
				nrt++;
			} else { /* Put the next block on some b */
				int b = (int) Math.round(Math.floor((r - p)
						/ ((1.0 - p) / choice)));
				if (b < nrt) { /* Destination is a rooted tower */
					worldState[floating[nft].objB_] = rooted[b].objA_ + 1;
					rooted[b].objA_ = floating[nft].objA_;
				} else { /* Destination is a floating tower */
					b -= nrt;
					worldState[floating[nft].objB_] = floating[b].objA_ + 1;
					floating[b].objA_ = floating[nft].objA_;
				}
			}
		}
					
		//
		//
		// Integer[] worldState = new Integer[numBlocks];
		// List<Double> contourState = new ArrayList<Double>();
		// contourState.add(0d);
		// List<Integer> blocksLeft = new ArrayList<Integer>();
		// goalArgs_ = null;
		// for (int i = 1; i <= numBlocks; i++) {
		// blocksLeft.add(i);
		// }
		//
		// while (!blocksLeft.isEmpty()) {
		// // Get a random block
		// Integer block = blocksLeft.remove(RRLExperiment.random_
		// .nextInt(blocksLeft.size()));
		//
		// // Put the block in a random position, influenced by the number of
		// // free blocks.
		// int index = RRLExperiment.random_.nextInt(contourState.size());
		// worldState[block - 1] = contourState.get(index).intValue();
		// if (worldState[block - 1] == 0) {
		// contourState.add(new Double(block));
		// } else {
		// contourState.set(index, new Double(block));
		// }
		// }

		// Set the goal
		int[] params = null;
		String goalName = StateSpec.getInstance().getGoalName();
		if (goalName.equals("on$A$B")) {
			boolean valid = false;
			while (!valid) {
				params = new int[2];
				params[0] = RRLExperiment.random_.nextInt(numBlocks) + 1;
				params[1] = RRLExperiment.random_.nextInt(numBlocks) + 1;
				// Cannot be the same block, and cannot already be achieved.
				valid = (params[0] != params[1])
						&& (worldState[params[0] - 1] != params[1]);
			}
		} else if (goalName.equals("clear$A") || goalName.equals("highest$A")) {
			params = new int[1];
			List<Integer> unclears = new ArrayList<Integer>();
			for (int block : worldState)
				if (block != 0)
					unclears.add(block);
			if (unclears.isEmpty()) {
				initialiseBlocksState(numBlocks);
				return;
			}

			params[0] = unclears.get(RRLExperiment.random_.nextInt(unclears
					.size()));
		}

		if (params != null) {
			goalArgs_ = new ArrayList<String>(params.length);
			for (int param : params)
				goalArgs_.add((char) ('a' + (param - 1)) + "");
		}

		// Check this isn't the goal state
		state_ = new BlocksState(worldState);
	}

	@Override
	protected boolean isReteDriven() {
		return true;
	}

	@Override
	protected boolean isTerminal() {
		return steps_ == maxSteps_ || super.isTerminal();
	}

	@Override
	protected void startState() {
		if (!optimal_) {
			// If action is null, thn this is the first episode.
			initialiseBlocksState(numBlocks_);
			optimalSteps_ = optimalSteps();
			if (RRLExperiment.debugMode_ || viewingMode_) {
				System.out.println("\tAgent:\n" + state_);
			}
			steps_ = 0;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void stepState(Object action) {
		// We have an action, apply it
		BlocksState newState = state_;
		boolean actionFailed = false;
		RelationalPredicate actionFact = null;
		if (RRLExperiment.random_.nextDouble() < actionSuccess_) {
			if (action instanceof Pair) {
				actionFact = ((Pair<BlocksState, RelationalPredicate>) action).objB_;
				newState = ((Pair<BlocksState, RelationalPredicate>) action).objA_;
			} else
				newState = (BlocksState) action;
		} else
			actionFailed = true;

		// Notify the user what the action is if outputting.
		if ((RRLExperiment.debugMode_ || viewingMode_) && !optimal_) {
			if (actionFact != null)
				System.out.println("\t" + actionFact + " ->\n" + newState);
			else
				System.out.println("\t\t\tNo action chosen.");
		}


		// If our new state is different, update observations
		if (!state_.equals(newState)) {
			state_ = newState;
		} else if (!actionFailed) {
			// If the agent caused the state to remain the same, exit the
			// episode with max negative reward.
			steps_ = maxSteps_;
			return;
		}

		steps_++;
	}

	@Override
	public void cleanup() {
		state_ = null;
	}

	@Override
	public void initialise(int runIndex, String[] extraArgs) {
		for (String arg : extraArgs) {
			if ((arg.startsWith("goal"))) {
				StateSpec.reinitInstance(arg.substring(5));
			} else if (arg.startsWith("nonDet")) {
				actionSuccess_ = Double.parseDouble(arg.substring(6));
			} else {
				try {
					numBlocks_ = Integer.parseInt(arg);
				} catch (Exception e) {
				}
			}
		}

		maxSteps_ = (int) (numBlocks_ / actionSuccess_) + 1;
		// maxSteps_ *= 2;

		precalculateBlockRatios(numBlocks_);
	}

	private void precalculateBlockRatios(int numBlocks) {
		// blockRatios_ = new float[numBlocks + 1][numBlocks + 1];
		ratio = new float[numBlocks * numBlocks];

		// int n, k;
		// float[] temp = new float[numBlocks + 1];
		// Arrays.fill(temp, 1.0f);
		//
		// for (n = 0; n <= numBlocks; n++)
		// for (k = 0; k + n <= numBlocks; k++) {
		// if (n <= 1)
		// blockRatios_[n][k] = 1.0f;
		// else {
		// blockRatios_[n][k] = (blockRatios_[n - 1][k] * (n - 1 + k +
		// blockRatios_[n - 1][k + 1]))
		// / (n - 2 + k + blockRatios_[n - 1][k]);
		// // temp[k] = (temp[k] * (temp[k + 1] + n + k))
		// // / (temp[k] + n + k - 1.0f);
		// // if (n % 2 == 0)
		// // blockRatios_[n / 2][k] = temp[k];
		// }
		// }

		int n, k;
		float temp[] = new float[numBlocks + 1];
		Arrays.fill(temp, 1.0f);

		for (n = 0; n <= numBlocks; n++)
			for (k = 0; k + n <= numBlocks; k++) {
				if (n < 1)
					ratio[pos(numBlocks, n, k)] = 1.0f;
				else {
					temp[k] = (temp[k] * (temp[k + 1] + n + k))
							/ (temp[k] + n + k - 1.0f);
					if ((n % 2) == 0)
						ratio[pos(numBlocks, n / 2, k)] = temp[k];
				}
			}
	}

	private int pos(int N, int x, int y) {
		return ((x * (N + 2 - x)) + y);
	}

	private float Ratio(float ratio[], int N, int x, int y) {
		int z;

		z = pos(N, x / 2, y);
		if (x % 2 != 0)
			return (ratio[z + 1] + x + y)
					/ (((1 / ratio[z]) * (x + y - 1)) + 1);
		else
			return ratio[z];
	}
}
