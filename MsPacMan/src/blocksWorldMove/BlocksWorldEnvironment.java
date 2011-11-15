package blocksWorldMove;

import relationalFramework.ObjectObservations;
import relationalFramework.PolicyActions;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalWrapper;
import relationalFramework.StateSpec;
import util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import jess.Rete;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;

import blocksWorld.BlocksState;

import cerrla.PolicyActor;
import cerrla.PolicyGenerator;

/**
 * The environment for the blocks world interface.
 * 
 * @author Sam Sarjant
 */
public class BlocksWorldEnvironment implements EnvironmentInterface {
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

	/** The state of the blocks world in base predicates. */
	protected RelationalWrapper wrapper_;

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
		wrapper_ = null;
		state_ = null;
	}

	// @Override
	public String env_init() {
		wrapper_ = new BlocksWorldRelationalWrapper();
		return null;
	}

	// @Override
	public String env_message(String arg0) {
		if (arg0.equals("maxSteps")) {
			maxSteps_ = (int) (numBlocks_ / actionSuccess_) + 1;
			return (maxSteps_ + 1) + "";
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
		wrapper_.newEpisode();
		ObjectObservations.getInstance().predicateKB = wrapper_
				.formObservations(state_, goalArgs_);
		ObjectObservations.getInstance().earlyExit = false;
		return obs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Reward_observation_terminal env_step(Action arg0) {
		Observation obs = new Observation();
		obs.charArray = ObjectObservations.OBSERVATION_ID.toCharArray();
		// Check for an early exit
		if (ObjectObservations.getInstance().earlyExit)
			return new Reward_observation_terminal(0, obs, true);

		// Action can fail
		BlocksState newState = state_;
		boolean actionFailed = false;
		RelationalPredicate action = null;
		if (PolicyGenerator.random_.nextDouble() < actionSuccess_) {
			Object result = wrapper_
					.groundActions(
							(PolicyActions) ObjectObservations.getInstance().objectArray[0],
							state_);
			if (result instanceof Pair) {
				action = (RelationalPredicate) ((Pair<BlocksState, RelationalPredicate>) result).objB_;
				newState = ((Pair<BlocksState, RelationalPredicate>) result).objA_;
			} else
				newState = (BlocksState) result;
		} else
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
			ObjectObservations.getInstance().predicateKB = wrapper_
					.formObservations(state_, goalArgs_);
		} else if (!actionFailed) {
			// If the agent caused the state to remain the same, exit the
			// episode with max negative reward.
			return new Reward_observation_terminal(MINIMAL_REWARD,
					new Observation(), true);
		}

		steps_++;

		double reward = 0;
		int isGoal = wrapper_.isTerminal();
		if (isGoal == 1 || steps_ == maxSteps_) {
			if (optimalSteps_ >= maxSteps_)
				reward = 0;
			else
				reward = MINIMAL_REWARD * (steps_ - optimalSteps_)
						/ (maxSteps_ - optimalSteps_);
			if (reward > 0)
				reward = 0;
		}
		// reward = -1;
		Reward_observation_terminal rot = new Reward_observation_terminal(
				reward, obs, isGoal);

		return rot;
	}

	/**
	 * Initialises the blocks world to a random, non-goal state.
	 * 
	 * @param numBlocks
	 *            The number of blocks in the world.
	 * @param goalName
	 *            The goal name.
	 * @return The newly initialised blocks world state.
	 */
	protected BlocksState initialiseWorld(int numBlocks) {
		Random random = PolicyGenerator.random_;
		Integer[] worldState = new Integer[numBlocks];
		List<Double> contourState = new ArrayList<Double>();
		contourState.add(0d);
		List<Integer> blocksLeft = new ArrayList<Integer>();
		goalArgs_ = null;
		wrapper_.newEpisode();
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
		BlocksState blocksState = new BlocksState(worldState);
		Rete rete = wrapper_.formObservations(blocksState, goalArgs_);
		if (rete == null)
			return initialiseWorld(numBlocks);

		return blocksState;
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
		// formState(state_);
		optimalMap_.put(state_, steps_);
		optimal_ = false;
		return steps_;
	}
}
