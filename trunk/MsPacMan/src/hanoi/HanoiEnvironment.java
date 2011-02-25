package hanoi;

import java.util.List;
import java.util.Stack;

import jess.Rete;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;

import relationalFramework.ActionChoice;
import relationalFramework.ObjectObservations;
import relationalFramework.Policy;
import relationalFramework.PolicyActor;
import relationalFramework.PolicyGenerator;
import relationalFramework.RuleAction;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

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

	private static final String TOWER_BASE_PREFIX = "base";

	/** The state of the hanoi problem. Start with just 3 towers. */
	private HanoiState state_;

	/** The number of tiles the tower is made of. */
	private int numTiles_ = 4;

	/** The state of the blocks world in base predicates. */
	private Rete rete_;

	/** The number of steps the agent took. */
	private int steps_ = 0;

	/** The maximum number of steps for the agent to complete the task. */
	private int maxSteps_;

	/** If we're running an optimal agent. */
	private boolean optimal_ = false;

	private Character lastMoved_ = null;

	// @Override
	public void env_cleanup() {
		rete_ = null;
		state_ = null;
	}

	// @Override
	public String env_init() {
		maxSteps(numTiles_);
		return null;
	}

	private void maxSteps(int numTiles) {
		maxSteps_ = (int) (Math.pow(2, numTiles_) - 1) * STEP_CONSTANT + 1;
	}

	// @Override
	public String env_message(String arg0) {
		if (arg0.equals("maxSteps"))
			return maxSteps_ + "";
		if (arg0.equals("freeze")) {
			PolicyGenerator.getInstance().freeze(true);
			return null;
		}
		if (arg0.equals("unfreeze")) {
			PolicyGenerator.getInstance().freeze(false);
			return null;
		}
		try {
			numTiles_ = Integer.parseInt(arg0);
			maxSteps(numTiles_);
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

		// Run the optimal policy if it hasn't yet been run
		if (!PolicyGenerator.getInstance().hasPreGoal()) {
			optimalSteps();
		}

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
		List<StringFact> actions = ruleAction.getTriggerActions();
		StringFact action = actions.get(PolicyGenerator.random_.nextInt(actions
				.size()));

		HanoiState newState = actOnAction(action, state_);
		if (PolicyGenerator.debugMode_) {
			if (action != null)
				System.out.println("\t" + action + " ->\n" + newState);
			else
				System.out.println("\t\t\tNo action chosen.");
		}

		Observation obs = new Observation();
		obs.charArray = ObjectObservations.OBSERVATION_ID.toCharArray();
		// If our new state is different, update observations
		if (!state_.equals(newState)) {
			state_ = newState;
			formObservation();
		} else {
			ObjectObservations.getInstance().setNoPreGoal();
			return new Reward_observation_terminal(-1 * maxSteps_ - steps_,
					new Observation(), true);
		}

		steps_++;

		Reward_observation_terminal rot = new Reward_observation_terminal(-1,
				obs, StateSpec.getInstance().isGoal(rete_));

		return rot;
	}

	/**
	 * Form the (useless) observation object and rete object.
	 * 
	 * @return The (useless) observation.
	 */
	private Observation formObservation() {
		try {
			rete_.reset();

			// Make the initial observations
			// Num tiles
			if (numTiles_ % 2 == 0)
				rete_.eval("(assert (numTiles even))");
			else
				rete_.eval("(assert (numTiles odd))");

			// Towers
			for (int i = 0; i < NUM_TOWERS; i++) {
				rete_.eval("(assert (tower t" + i + "))");
				rete_.eval("(assert (nextTower t" + i + " t"
						+ ((i + 1) % NUM_TOWERS) + "))");
				rete_.eval("(assert (prevTower t" + i + " t"
						+ ((i + NUM_TOWERS - 1) % NUM_TOWERS) + "))");
			}

			// Tiles
			Stack<Character>[] state = state_.getState();
			for (int t = 0; t < state.length; t++) {
				Stack<Character> tileStack = state[t];
				for (int i = tileStack.size() - 1; i >= 0; i--) {
					// On
					String underneath = null;
					if (i - 1 >= 0)
						underneath = tileStack.get(i - 1).toString();
					else
						underneath = TOWER_BASE_PREFIX + t;

					rete_.eval("(assert (tile " + tileStack.get(i) + "))");
					rete_.eval("(assert (on " + tileStack.get(i) + " "
							+ underneath + " t" + t + "))");

					// Last Moved
					if ((lastMoved_ != null)
							&& (tileStack.get(i).equals(lastMoved_))) {
						rete_.eval("(assert (lastMoved " + tileStack.get(i)
								+ "))");
					} else
						rete_.eval("(assert (notLastMoved " + tileStack.get(i)
								+ "))");
				}

				// Tower base
				rete_
						.eval("(assert (towerBase " + TOWER_BASE_PREFIX + t
								+ "))");
				if (tileStack.isEmpty())
					rete_.eval("(assert (clear " + TOWER_BASE_PREFIX + t + " t"
							+ t + "))");
			}

			rete_.run();

			// Adding the valid actions
			ObjectObservations.getInstance().validActions = StateSpec
					.getInstance().generateValidActions(rete_);
			ObjectObservations.getInstance().predicateKB = rete_;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Observation();
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
	private HanoiState actOnAction(StringFact action, HanoiState worldState) {
		if (action == null)
			return worldState;

		String[] arguments = action.getArguments();

		HanoiState newState = worldState.clone();

		// Convert the blocks to indices
		Stack<Character>[] stateStacks = newState.getState();
		int fromIndex = Integer.parseInt(arguments[1].charAt(1) + "");
		int toIndex = Integer.parseInt(arguments[3].charAt(1) + "");
		Character from = stateStacks[fromIndex].pop();
		String to = null;
		if (!stateStacks[toIndex].isEmpty())
			to = stateStacks[toIndex].peek() + "";
		else
			to = TOWER_BASE_PREFIX;

		// Check the elements add up
		if ((arguments[0].charAt(0) == from.charValue())
				&& (arguments[2].substring(0, to.length()).equals(to)))
			stateStacks[toIndex].push(from);
		else
			System.err
					.println("The action argument doesn't add up to the state.");

		lastMoved_ = from;

		return newState;
	}

	/**
	 * Calculates the optimal number of steps to solve the problem.
	 * 
	 * @return The minimal number of steps to take for solving.
	 */
	private int optimalSteps() {
		Policy optimalPolicy = StateSpec.getInstance().getHandCodedPolicy();
		steps_ = 0;
		optimal_ = true;

		// Run the policy through the environment until goal is satisfied.
		PolicyActor optimalAgent = new PolicyActor();
		ObjectObservations.getInstance().objectArray = new Policy[] { optimalPolicy };
		optimalAgent.agent_message("Optimal");
		optimalAgent.agent_message("SetPolicy");
		Action act = optimalAgent.agent_start(formObservation());
		// Loop until the task is complete
		Reward_observation_terminal rot = env_step(act);
		while ((rot == null) || (!rot.isTerminal())) {
			// Check if the optimal policy has already seen this state
			optimalAgent.agent_step(rot.r, rot.o);
			rot = env_step(act);
		}

		// Form the first pre-goal.
		if (!PolicyGenerator.getInstance().hasPreGoal())
			optimalAgent.agent_message("formPreGoal");

		// Return the state to normal
		state_ = new HanoiState(numTiles_);
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

		private HanoiState() {
			tileState_ = new Stack[NUM_TOWERS];
			for (int i = 0; i < tileState_.length; i++) {
				tileState_[i] = new Stack<Character>();
			}
		}

		public Stack<Character>[] getState() {
			return tileState_;
		}

		@Override
		public HanoiState clone() {
			HanoiState clone = new HanoiState();
			for (int i = 0; i < tileState_.length; i++) {
				clone.tileState_[i] = (Stack) tileState_[i].clone();
			}

			return clone;
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
					if (tileState_[i].size() - 1 >= n) {
						usedLevel = true;
						tempBuffer.append('[');
						char tile = tileState_[i].get(n);
						int tileSize = tile - 'a' + 1;
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
				buffer.append(TOWER_BASE_PREFIX + i);
				for (int s = TOWER_BASE_PREFIX.length() + 1; s < maxTileWidth - 1; s++)
					buffer.append('-');
				buffer.append('|');
			}

			return buffer.toString();
		}
	}
}
