package blocksWorld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.mandarax.kernel.ClauseSet;
import org.mandarax.kernel.ConstantTerm;
import org.mandarax.kernel.Fact;
import org.mandarax.kernel.InferenceEngine;
import org.mandarax.kernel.KnowledgeBase;
import org.mandarax.kernel.LogicFactory;
import org.mandarax.kernel.Prerequisite;
import org.mandarax.kernel.Query;
import org.mandarax.kernel.Rule;
import org.mandarax.kernel.Term;
import org.mandarax.util.LogicFactorySupport;
import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;

import relationalFramework.ObjectObservations;
import relationalFramework.Policy;
import relationalFramework.PolicyAgent;
import relationalFramework.PolicyGenerator;
import relationalFramework.State;
import relationalFramework.StateSpec;

/**
 * The environment for the blocks world interface.
 * 
 * @author Sam Sarjant
 */
public class BlocksWorldEnvironment implements EnvironmentInterface {
	/** The constant for extending the episode. */
	public static final int STEP_CONSTANT = 2;

	/** The number of blocks. Default 5. */
	private int numBlocks_ = 5;

	/** The state of the blocks world. */
	private BlocksState state_;

	/** The state of the blocks world in base predicates. */
	private KnowledgeBase stateKB_;

	/** The blocks contained within the environment. */
	private ConstantTerm[] blocks_;

	/** The number of steps taken. */
	private int steps_;

	/** The optimal number of steps for a state to be solved. */
	private Map<BlocksState, Integer> optimalMap_ = new HashMap<BlocksState, Integer>();

	/** The optimal number of steps. */
	private int optimalSteps_;

	// @Override
	public void env_cleanup() {
		stateKB_ = null;
		state_ = null;
		blocks_ = null;
	}

	// @Override
	public String env_init() {
		stateKB_ = new org.mandarax.reference.KnowledgeBase();
		// Assign the blocks
		blocks_ = createBlocks(numBlocks_);
		return null;
	}

	// @Override
	public String env_message(String arg0) {
		if (arg0.equals("maxSteps"))
			return (numBlocks_ * STEP_CONSTANT + 1) + "";
		if (arg0.equals("freeze")) {
			PolicyGenerator.getInstance().freeze(true);
			return null;
		}
		if (arg0.equals("unfreeze")) {
			PolicyGenerator.getInstance().freeze(false);
			return null;
		}
		try {
			numBlocks_ = Integer.parseInt(arg0);
			// Assign the blocks
			blocks_ = createBlocks(numBlocks_);
			optimalMap_ = new HashMap<BlocksState, Integer>();
			return null;
		} catch (Exception e) {

		}
		return null;
	}

	// @Override
	public Observation env_start() {
		// Generate a random blocks world
		state_ = initialiseWorld(numBlocks_, StateSpec.getInstance()
				.getGoalState());
		System.out.println("\t\t\tOptimal test: "
				+ Arrays.toString(state_.getState()));
		optimalSteps_ = optimalSteps();
		System.out
				.println("\t\t\tAgent: " + Arrays.toString(state_.getState()));
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
		ObjectObservations.getInstance().predicateKB = stateKB_;
		return obs;
	}

	// @Override
	public Reward_observation_terminal env_step(Action arg0) {
		Fact action = null;
		Random random = new Random();
		for (int i = 0; i < ObjectObservations.getInstance().objectArray.length; i++) {
			List<Fact> actions = (List<Fact>) ObjectObservations.getInstance().objectArray[i];
			if ((actions != null) && (!actions.isEmpty()))
				action = actions.get(random.nextInt(actions.size()));
			if (action != null)
				break;
		}

		BlocksState newState = actOnAction(action, state_);
		if (action != null)
			System.out.println("\t\t\t" + StateSpec.lightenFact(action)
					+ "\t->  " + Arrays.toString(newState.intState_));
		else
			System.out.println("\t\t\tNo action chosen.");

		Observation obs = new Observation();
		obs.charArray = ObjectObservations.OBSERVATION_ID.toCharArray();
		// If our new state is different, update observations
		if (!state_.equals(newState)) {
			state_ = newState;
			stateKB_ = formState(state_.getState());
		} else {
			double excess = (steps_ > optimalSteps_) ? steps_ - optimalSteps_
					: 0;
			return new Reward_observation_terminal(-numBlocks_ * STEP_CONSTANT
					+ excess, new Observation(), true);
		}

		steps_++;
		ObjectObservations.getInstance().predicateKB = stateKB_;

		double reward = (steps_ <= optimalSteps_) ? 0
				: ((-numBlocks_ * STEP_CONSTANT * 1.0) / (numBlocks_
						* STEP_CONSTANT - optimalSteps_));
		Reward_observation_terminal rot = new Reward_observation_terminal(
				reward, obs, isGoal(stateKB_, StateSpec.getInstance()
						.getGoalState()));

		return rot;
	}

	/**
	 * Checks if the current state is a goal state.
	 * 
	 * @param stateKB
	 *            The current state.
	 * @param goalState
	 *            The goal state.
	 * @return True if we're in the goal state, false otherwise.
	 */
	private boolean isGoal(KnowledgeBase stateKB, Rule goalState) {
		LogicFactorySupport factorySupport = new LogicFactorySupport(
				PolicyGenerator.getInstance().getLogicFactory());
		Fact[] ruleConditions = (Fact[]) goalState.getBody().toArray(
				new Fact[goalState.getBody().size()]);
		Query query = factorySupport.query(ruleConditions, "isGoal");
		try {
			org.mandarax.kernel.ResultSet results = PolicyGenerator
					.getInstance().getInferenceEngine().query(query, stateKB,
							InferenceEngine.ONE,
							InferenceEngine.BUBBLE_EXCEPTIONS);
			if (results.next())
				return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Acts on the action given.
	 * 
	 * @param action
	 *            The action to act upon.
	 * @param worldState
	 *            The old state of the world, before the action.
	 * @return The state of the new world.
	 */
	private BlocksState actOnAction(Fact action, BlocksState worldState) {
		if (action == null)
			return worldState;

		Integer[] newState = new Integer[worldState.length];

		Term[] actionTerms = action.getTerms();

		// Finding the block objects
		int[] indices = null;
		Block[] blocks = new Block[2];
		blocks[0] = (Block) ((ConstantTerm) actionTerms[1]).getObject();
		if (action.getPredicate().getName().equals("move")) {
			blocks[1] = (Block) ((ConstantTerm) actionTerms[2]).getObject();
			indices = new int[2];
		} else {
			indices = new int[1];
		}

		// Convert the blocks to indices
		Integer[] stateArray = worldState.getState();
		for (int i = 0; i < indices.length; i++) {
			indices[i] = (blocks[i].getName().charAt(0)) - ('a');
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
	private ConstantTerm[] createBlocks(int numBlocks) {
		ConstantTerm[] blocks = new ConstantTerm[numBlocks];
		LogicFactory factory = PolicyGenerator.getInstance().getLogicFactory();

		for (int i = 0; i < numBlocks; i++) {
			String name = (char) ('a' + i) + "";
			blocks[i] = factory
					.createConstantTerm(new Block(name), Block.class);
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
	private BlocksState initialiseWorld(int numBlocks, Rule goalState) {
		Integer[] worldState = new Integer[numBlocks];
		int[] contourState = new int[numBlocks];
		Random random = new Random();
		List<Integer> blocksLeft = new ArrayList<Integer>();
		for (int i = 1; i <= numBlocks; i++) {
			blocksLeft.add(i);
		}

		while (!blocksLeft.isEmpty()) {
			// Get a random block
			Integer block = blocksLeft
					.remove(random.nextInt(blocksLeft.size()));

			// Put it in a random position in the world (from max numBlocks
			// columns)
			int position = random.nextInt(numBlocks - 1);
			// Put the block on top of whatever was there
			worldState[block - 1] = contourState[position];
			// Then that block is now the top of that stack
			contourState[position] = block;
		}

		// Check this isn't the goal state
		stateKB_ = formState(worldState);
		if (isGoal(stateKB_, goalState))
			return initialiseWorld(numBlocks, goalState);
		else
			return new BlocksState(worldState);
	}

	/**
	 * Forms the knowledge base of the state using the int array approximation.
	 * 
	 * @param worldState
	 *            The state of the world in int form.
	 * @return The knowledge base representing the world.
	 */
	private KnowledgeBase formState(Integer[] worldState) {
		stateKB_.removeAll();
		List<ClauseSet> backgroundClauses = StateSpec.getInstance()
				.getBackgroundKnowledge().getClauseSets();
		for (ClauseSet background : backgroundClauses)
			stateKB_.add(background);

		LogicFactory factory = PolicyGenerator.getInstance().getLogicFactory();

		// Stating everything is clear until covered
		List<Integer> clearBlocks = new ArrayList<Integer>();
		for (int i = 0; i < worldState.length; i++) {
			clearBlocks.add(i);
		}

		// Add the state information for fulfilling goals
		Object[] stateArray = {worldState, 1 };
		BlocksWorldState state = new BlocksWorldState(stateArray);
		Term currentState = factory.createConstantTerm(state, State.class);

		// Scanning through, making predicates
		Integer maxHeight = 0;
		int[] heightMap = new int[worldState.length];
		List<Prerequisite> preds = new ArrayList<Prerequisite>();
		for (int i = 0; i < worldState.length; i++) {
			// On the floor
			if (worldState[i] == 0) {
				Term[] terms = new Term[2];
				terms[0] = currentState;
				terms[1] = blocks_[i];
				StateSpec.addContains(preds, StateSpec.getInstance()
						.getGuidedPredicate("onFloor").factify(factory, terms,
								false, false, null));
			} else {
				// On another block
				Term[] terms = new Term[3];
				terms[0] = currentState;
				terms[1] = blocks_[i];
				terms[2] = blocks_[worldState[i] - 1];
				StateSpec.addContains(preds, StateSpec.getInstance()
						.getGuidedPredicate("on").factify(factory, terms,
								false, false, null));

				// The other block is not clear
				clearBlocks.remove((new Integer(worldState[i] - 1)));
			}

			// Finding the heights
			if (heightMap[i] == 0) {
				maxHeight = Math.max(maxHeight, recurseHeight(i, heightMap,
						worldState));
			}
		}
		
		// Modify the height value
		state.getStateArray()[BlocksWorldState.HIGHEST_BLOCK] = maxHeight;
		// Note the clear blocks
		for (Integer blockInd : clearBlocks) {
			Term[] terms = new Term[2];
			terms[0] = currentState;
			terms[1] = blocks_[blockInd];
			StateSpec.addContains(preds, StateSpec.getInstance()
					.getGuidedPredicate("clear").factify(factory, terms, false,
							false, null));
		}

		// Adding the prereqs
		for (Prerequisite preq : preds) {
			stateKB_.add(preq);
		}

		// Adding the valid actions
		StateSpec.getInstance().insertValidActions(stateKB_);

		return stateKB_;
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
			System.out.println("\t\t\tAlready calculated ("
					+ optimalMap_.get(state_) + ")");
			return optimalMap_.get(state_);
		}

		BlocksState initialState = state_.clone();
		// Run the policy through the environment until goal is satisfied.
		PolicyAgent optimalAgent = new PolicyAgent();
		ObjectObservations.getInstance().objectArray = new Policy[] { optimalPolicy };
		optimalAgent.agent_message("Policy");
		Action act = optimalAgent.agent_start(formObs_Start());
		// Loop until the task is complete
		Reward_observation_terminal rot = null;
		while ((rot == null) || (!rot.isTerminal())) {
			rot = env_step(act);
			optimalAgent.agent_step(rot.r, rot.o);
		}

		// Return the state to normal
		state_ = initialState;
		stateKB_ = formState(state_.getState());
		optimalMap_.put(state_, steps_);
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
			return Arrays.toString(intState_);
		}
	}
}
