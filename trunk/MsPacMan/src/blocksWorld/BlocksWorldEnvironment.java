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
 *    src/blocksWorld/BlocksWorldEnvironment.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package blocksWorld;

import relationalFramework.PolicyActions;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
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
	private List<String> goalArgs_;

	/** If we're running an optimal agent. */
	private boolean optimal_ = false;

	/** The optimal number of steps. */
	private int optimalSteps_;

	/** The optimal policy for this goal. */
	private RelationalPolicy optimalPolicy_;

	/** Similar to debug mode, but only shows ASCII representation of blocks. */
	private boolean viewingMode_ = false;

	/** The probability of an action succeeding (assuming it is legal). */
	protected double actionSuccess_ = 1.0;

	/** The maximum number of steps the agent is allocated. */
	protected int maxSteps_;

	/** The number of blocks. Default 10. */
	protected int maxBlocks_ = 10;

	/** The number of blocks. Default 5. */
	protected int minBlocks_ = 10;

	/** The state of the blocks world. */
	protected BlocksState state_;

	/** The number of steps taken. */
	protected int steps_;

	/** A count of the number of failed actions. */
	protected int failedActions_;

	/** The BWStates ratio 1d array (use pos()). */
	private float[] ratio;

	/**
	 * Calculates the optimal number of steps to solve the problem.
	 * 
	 * @return The minimal number of steps to take for solving.
	 */
	private int optimalSteps() {
		RelationalPolicy optimalPolicy = optimalPolicy_;
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
		while (obs.isTerminal() != TERMINAL_WIN) {
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
	protected double[] calculateReward(int isTerminal) {
		double[] reward = new double[2];
		if (isTerminal == TERMINAL_WIN) {
			if (steps_ == optimalSteps_)
				reward[0] = 1;
			else
				reward[0] = 1 - 1.0 * (steps_ - optimalSteps_)
						/ (maxSteps_ - optimalSteps_);
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
		return action;
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
	public void initialiseBlocksState(int numBlocks) {
		maxSteps_ = (int) (numBlocks / actionSuccess_);
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
			float rat = Ratio(ratio, maxBlocks_, nft, nrt);
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
	protected int isTerminal() {
		if (super.isTerminal() == TERMINAL_WIN)
			return TERMINAL_WIN;
		if (steps_ == maxSteps_)
			return TERMINAL_LOSE;
		return 0;
	}

	@Override
	protected void startState() {
		if (!optimal_) {
			// If action is null, then this is the first episode.
			failedActions_ = 0;
			initialiseBlocksState(RRLExperiment.random_.nextInt(maxBlocks_
					- minBlocks_ + 1)
					+ minBlocks_);
			optimalSteps_ = optimalSteps();
			// maxSteps_ = optimalSteps_;
			if (RRLExperiment.debugMode_ || viewingMode_) {
				System.out.println("\tAgent:\n" + state_);
			}
			steps_ = 0;
		}
	}

	@Override
	protected void stepState(Object action) {
		// We have an action, apply it
		BlocksState newState = state_;
		boolean actionFailed = false;
		RelationalPredicate actionFact = null;
		if (RRLExperiment.random_.nextDouble() >= actionSuccess_) {
			actionFailed = true;
			failedActions_++;

			// No action or random action
			if (RRLExperiment.random_.nextBoolean())
				action = randomAction();
		} else if (action == null) {
			action = randomAction();
		}

		if (action != null) {
			actionFact = (RelationalPredicate) action;
			try {
				StateSpec.getInstance().getRete()
						.assertString(action.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}

			newState = state_.clone();

			resolveAction(newState, actionFact);
		}

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

	/**
	 * Resolve the action on the integer representation.
	 * 
	 * @param newState The state of the world to alter. 
	 * @param actionFact The action being resolved.
	 */
	protected void resolveAction(BlocksState newState,
			RelationalPredicate actionFact) {
		// Finding the block objects
		int[] indices = new int[2];

		// Convert the blocks to indices
		for (int i = 0; i < indices.length; i++) {
			if (actionFact.getArguments()[i].equals("floor"))
				indices[i] = -1;
			else
				indices[i] = (actionFact.getArguments()[i].charAt(0)) - ('a');
		}

		// Perform the action
		newState.getState()[indices[0]] = indices[1] + 1;
	}

	/**
	 * Selects a random action from the available actions.
	 * 
	 * @return A random action from the available actions.
	 */
	protected RelationalPredicate randomAction() {
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
		return new RelationalPredicate(StateSpec.getInstance().getActions()
				.get("move"), (String[]) randomAction);
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
				boolean found = false;
				try {
					maxBlocks_ = Integer.parseInt(arg);
					minBlocks_ = maxBlocks_;
				} catch (Exception e) {
					found = true;
				}
				if (!found) {
					String[] split = arg.split("-");
					if (split.length == 2)
						try {
							minBlocks_ = Integer.parseInt(split[0]);
							maxBlocks_ = Integer.parseInt(split[1]);
						} catch (Exception e) {
						}
				}
			}
		}

		precalculateBlockRatios(maxBlocks_);
		optimalPolicy_ = createOptimalPolicy(StateSpec.getInstance().getGoalName());
	}

	/**
	 * Pre-calculates the ratios for various combinations of grounded and
	 * ungrounded blocks.
	 * 
	 * @param maxBlocks
	 *            The maximum possible number of blocks in the world.
	 */
	private void precalculateBlockRatios(int maxBlocks) {
		ratio = new float[maxBlocks * maxBlocks];

		int n, k;
		float temp[] = new float[maxBlocks + 1];
		Arrays.fill(temp, 1.0f);

		for (n = 0; n <= maxBlocks; n++)
			for (k = 0; k + n <= maxBlocks; k++) {
				if (n < 1)
					ratio[pos(maxBlocks, n, k)] = 1.0f;
				else {
					temp[k] = (temp[k] * (temp[k + 1] + n + k))
							/ (temp[k] + n + k - 1.0f);
					if ((n % 2) == 0)
						ratio[pos(maxBlocks, n / 2, k)] = temp[k];
				}
			}
	}

	/**
	 * Maps a 2-dimensional index into a 1-dimensional index.
	 * 
	 * @param N
	 *            The maximum number of blocks.
	 * @param x
	 *            The number of grounded towers.
	 * @param y
	 *            The number of ungrounded towers.
	 * @return The 1-dimensional index.
	 */
	private int pos(int N, int x, int y) {
		return ((x * (N + 2 - x)) + y);
	}

	/**
	 * Calculate the ratio of the grounded vs ungrounded blocks. Replicated from
	 * bwstates (Slaney and Thiebaux (2001))
	 * http://users.cecs.anu.edu.au/~jks/bw.html
	 * 
	 * @param ratio
	 *            The precalculated ratios.
	 * @param N
	 *            The maximum number of blocks.
	 * @param nrt
	 *            The number of grounded towers.
	 * @param nft
	 *            The number of ungrounded towers.
	 * @return The ratio between grounded and ungrounded towers.
	 */
	private float Ratio(float ratio[], int N, int nrt, int nft) {
		int z;

		z = pos(N, nrt / 2, nft);
		if (nrt % 2 != 0)
			return (ratio[z + 1] + nrt + nft)
					/ (((1 / ratio[z]) * (nrt + nft - 1)) + 1);
		else
			return ratio[z];
	}

	/**
	 * Testing method. Gets the current state of the blocks world.
	 * 
	 * @return Gets the state.
	 */
	public BlocksState getState() {
		return state_;
	}

	/**
	 * Create an optimal policy for the given goal.
	 * 
	 * @param goal
	 *            The goal for the optimal policy to achieve.
	 * @return The optimal policy.
	 */
	protected RelationalPolicy createOptimalPolicy(String goal) {
		// Defining the optimal policy based on the goal
		String[] rules = null;
		if (goal.equals("onab") || goal.equals("on$A$B")) {
			rules = new String[3];
			rules[0] = "(" + StateSpec.GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?G_0) (clear ?G_1) => (move ?G_0 ?G_1)";
			rules[1] = "(" + StateSpec.GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?A) (above ?A ?G_0) (floor ?B) => (move ?A ?B)";
			rules[2] = "(" + StateSpec.GOALARGS_PRED + " ? ?G_0 ?G_1) "
					+ "(clear ?A) (above ?A ?G_1) (floor ?B) => (move ?A ?B)";
		} else if (goal.equals("on$A$Bon$B$C")) {
			rules = new String[5];
			rules[0] = "(" + StateSpec.GOALARGS_PRED + " ? ?G_0 ?G_1 ?G_2) "
					+ "(clear ?A) (above ?A ?G_0) (floor ?B) => (move ?A ?B)";
			rules[1] = "(" + StateSpec.GOALARGS_PRED + " ? ?G_0 ?G_1 ?G_2) "
					+ "(clear ?A) (above ?A ?G_1) (floor ?B) => (move ?A ?B)";
			rules[2] = "(" + StateSpec.GOALARGS_PRED + " ? ?G_0 ?G_1 ?G_2) "
					+ "(clear ?A) (above ?A ?G_2) (floor ?B) => (move ?A ?B)";
			rules[3] = "(" + StateSpec.GOALARGS_PRED + " ? ?G_0 ?G_1 ?G_2) "
					+ "(clear ?G_1) (clear ?G_2) => (move ?G_1 ?G_2)";
			rules[4] = "(" + StateSpec.GOALARGS_PRED + " ? ?G_0 ?G_1 ?G_2) "
					+ "(clear ?G_0) (clear ?G_1) => (move ?G_0 ?G_1)";
		} else if (goal.equals("stack")) {
			rules = new String[1];
			rules[0] = "(clear ?A) (highest ?B) => (move ?A ?B)";
		} else if (goal.equals("unstack")) {
			rules = new String[1];
			rules[0] = "(highest ?A) (floor ?B) => (move ?A ?B)";
		} else if (goal.equals("clearA") || goal.equals("clear$A")) {
			rules = new String[1];
			rules[0] = "(" + StateSpec.GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?A) (above ?A ?G_0) (floor ?B) => (move ?A ?B)";
		} else if (goal.equals("highestA") || goal.equals("highest$A")) {
			rules = new String[2];
			rules[0] = "(" + StateSpec.GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?A) (above ?A ?G_0) (floor ?B) => (move ?A ?B)";
			rules[1] = "(" + StateSpec.GOALARGS_PRED + " ? ?G_0) "
					+ "(clear ?G_0) (highest ?B) => (move ?G_0 ?B)";
		}

		RelationalPolicy optimal = new RelationalPolicy();
		for (int i = 0; i < rules.length; i++)
			optimal.addRule(new RelationalRule(rules[i]));

		return optimal;
	}
}
