package blocksWorldMove;

import relationalFramework.FiredAction;
import relationalFramework.PolicyActions;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;
import rrlFramework.RRLEnvironment;
import rrlFramework.RRLExperiment;
import rrlFramework.RRLObservations;
import util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jess.Rete;

import cerrla.PolicyGenerator;

/**
 * The environment for the blocks world interface.
 * 
 * @author Sam Sarjant
 */
public class BlocksWorldEnvironment extends RRLEnvironment {
	/** The minimal reward the agent can receive. */
	private static final float MINIMAL_REWARD = -10;

	/** The number of blocks. Default 5. */
	protected int numBlocks_ = 5;

	/** The probability of an action succeeding (assuming it is legal). */
	protected double actionSuccess_ = 1.0;

	/** The maximum number of steps the agent is allocated. */
	protected int maxSteps_;

	/** The state of the blocks world. */
	protected BlocksState state_;

	/** The number of steps taken. */
	protected int steps_;

	/** The optimal number of steps for a state to be solved. */
	private Map<BlocksState, Integer> optimalMap_ = new HashMap<BlocksState, Integer>();

	/** The optimal number of steps. */
	private int optimalSteps_;

	/** If we're running an optimal agent. */
	private boolean optimal_ = false;

	/** Similar to debug mode, but only shows ASCII representation of blocks. */
	private boolean viewingMode_ = false;

	private List<String> goalArgs_;

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
					optimalMap_ = new HashMap<BlocksState, Integer>();
				} catch (Exception e) {
				}
			}
		}

		maxSteps_ = (int) (numBlocks_ / actionSuccess_) + 1;
	}

	/**
	 * Initialises the blocks world to a random, non-goal state.
	 * 
	 * @param numBlocks
	 *            The number of blocks in the world.
	 * @param goalName
	 *            The goal name.
	 */
	protected void initialiseBlocksState(int numBlocks) {
		Integer[] worldState = new Integer[numBlocks];
		List<Double> contourState = new ArrayList<Double>();
		contourState.add(0d);
		List<Integer> blocksLeft = new ArrayList<Integer>();
		goalArgs_ = null;
		for (int i = 1; i <= numBlocks; i++) {
			blocksLeft.add(i);
		}

		while (!blocksLeft.isEmpty()) {
			// Get a random block
			Integer block = blocksLeft
					.remove(RRLExperiment.random_.nextInt(blocksLeft.size()));

			// Put the block in a random position, influenced by the number of
			// free blocks.
			int index = RRLExperiment.random_.nextInt(contourState.size());
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
		double oldActionSuccess = actionSuccess_;
		actionSuccess_ = 1.0;

		// Loop until the task is complete
		int numActions = StateSpec.getInstance()
				.getNumReturnedActions();
		RRLObservations obs = startEpisode();
		while (!obs.isTerminal()) {
			// Check if the optimal policy has already seen this state
			if (optimalMap_.containsKey(state_)) {
				steps_ += optimalMap_.get(state_);
				break;
			}

			// Apply the policy
			PolicyActions actions = optimalPolicy.evaluatePolicy(obs,
					numActions);
			obs = step(actions);
		}

		// Return the state to normal
		state_ = initialState;
		actionSuccess_ = oldActionSuccess;
		// formState(state_);
		optimalMap_.put(state_, steps_);
		optimal_ = false;
		return steps_;
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
	protected void assertStateFacts(Rete rete, List<String> goalArgs)
			throws Exception {
		if (steps_ != 0) {
			// Apply the action
			rete.run();
			return;
		}

		// Assert the floor
		rete.assertString("(floor floor)");

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
	protected double calculateReward(boolean isTerminal) {
		if (isTerminal || steps_ == maxSteps_) {
			if (optimalSteps_ >= maxSteps_)
				return 0;
			else
				return MINIMAL_REWARD * (steps_ - optimalSteps_)
						/ (maxSteps_ - optimalSteps_);
		}
		return 0;
	}

	@Override
	protected boolean isTerminal() {
		return steps_ == maxSteps_ || super.isTerminal();
	}

	@Override
	protected Object groundActions(PolicyActions actions) {
		Collection<FiredAction> firedActions = actions.getFirstActionList();
		RelationalPredicate action = null;
		if (firedActions != null) {
			List<FiredAction> actionsList = new ArrayList<FiredAction>(
					firedActions);
			FiredAction selectedAction = actionsList
					.get(RRLExperiment.random_.nextInt(actionsList.size()));
			selectedAction.triggerRule();
			action = selectedAction.getAction();

			// Assert the action to the Rete object.
			try {
				StateSpec.getInstance().getRete()
						.assertString(action.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (action == null)
			return state_;

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

	@Override
	protected boolean isReteDriven() {
		return true;
	}

	@Override
	protected List<String> getGoalArgs() {
		return goalArgs_;
	}
}
