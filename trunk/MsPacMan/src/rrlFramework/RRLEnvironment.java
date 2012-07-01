package rrlFramework;

import java.util.List;

import jess.Rete;

import org.apache.commons.collections.BidiMap;

import relationalFramework.PolicyActions;
import relationalFramework.StateSpec;
import util.MultiMap;

/**
 * An interface for a RRL environment.
 * 
 * @author Sam Sarjant
 */
public abstract class RRLEnvironment {
	public static final String ENVIRONMENT_CLASS_SUFFIX = "Environment";
	
	public static final int TERMINAL_WIN = 1;
	public static final int TERMINAL_LOSE = -1;
	public static final int NOT_TERMINAL = 0;

	/** If the environment is updated by Rete or scanning the state. */
	private boolean reteDriven_;

	/** The goal replacement map. */
	private BidiMap goalReplacementMap_;

	/** The observations from the last state. */
	protected RRLObservations observations_;

	public RRLEnvironment() {
		reteDriven_ = isReteDriven();
	}

	/**
	 * Forms the relational observations using the args as the input for
	 * relationalising.
	 * 
	 * @param goalArgs
	 *            The goal arguments given for the episode.
	 * @param firstState
	 *            If this is the first state.
	 * @return The Rete object of observations or null if the state is invalid.
	 */
	private final RRLObservations formObservations(List<String> goalArgs,
			boolean firstState) {
		Rete rete = StateSpec.getInstance().getRete();
		try {
			if (!reteDriven_ || firstState) {
				rete.reset();
				goalReplacementMap_ = null;
			}

			// Assert the state facts and goal replacements.
			assertStateFacts(rete, goalArgs);
			if (goalReplacementMap_ == null) {
				goalReplacementMap_ = StateSpec.getInstance().assertGoalPred(
						goalArgs, rete);

				if (RRLExperiment.debugMode_ && !goalReplacementMap_.isEmpty()) {
					System.out.println(goalReplacementMap_.inverseBidiMap());
				}
			}
			if (rete == null)
				return null;
			rete.run();
		} catch (Exception e) {
			e.printStackTrace();
		}

		int isTerminal = isTerminal();
		return compileObservation(rete, StateSpec.getInstance()
				.generateValidActions(rete), goalReplacementMap_, isTerminal);
	}

	/**
	 * Compiles the RRLObservations object (default single player).
	 * 
	 * @param rete
	 *            The current Rete state.
	 * @param validActions
	 *            The computed valid actions for the state.
	 * @param goalReplacements
	 *            The goal replacement map (if any).
	 * @param isTerminal
	 *            If this is the terminal state.
	 * @return An RRLObservations object.
	 */
	protected RRLObservations compileObservation(Rete rete,
			MultiMap<String, String[]> validActions, BidiMap goalReplacements,
			int isTerminal) {
		return new RRLObservations(rete, validActions,
				calculateReward(isTerminal), goalReplacements, isTerminal);
	}

	/**
	 * Gets the player ID (for multi-agent environments).
	 * 
	 * @return A blank string in this, single-player, case.
	 */
	protected String getPlayerID() {
		return "";
	}

	/**
	 * Starts the episode by initialising the environment state.
	 */
	protected abstract void startState();

	/**
	 * Moves the state forward by applying the action to the environment.
	 * 
	 * @param action
	 *            The action to apply.
	 */
	protected abstract void stepState(Object action);

	/**
	 * Performs the actual asserting/retracting of facts based on the args
	 * given. The valid actions and background knowledge are added outside of
	 * this method.
	 * 
	 * @param rete
	 *            The rete object to assert/retract the facts.
	 * @param goalArgs
	 *            The goal arguments (if any).
	 * @return The goal replacement map.
	 */
	protected abstract void assertStateFacts(Rete rete, List<String> goalArgs)
			throws Exception;

	/**
	 * Calculates the reward.
	 * 
	 * @param isTerminal
	 *            If at the terminal state.
	 * @return The reward received at this given interval.
	 */
	protected abstract double[] calculateReward(int isTerminal);

	/**
	 * Get the goal argument list (if any).
	 * 
	 * @return The goal argument list or null.
	 */
	protected abstract List<String> getGoalArgs();

	/**
	 * Grounds a collection of relational actions into a single low-level object
	 * to act upon the state.
	 * 
	 * @param actions
	 *            The actions to ground.
	 * @return The low-level action output.
	 */
	protected abstract Object groundActions(PolicyActions actions);

	/**
	 * Determines how the Rete object is asserted to: via interacting rules or
	 * continual state scans.
	 * 
	 * @return True if the environment uses Rete rules to update state or false
	 *         if states are updated by scans.
	 */
	protected abstract boolean isReteDriven();

	/**
	 * Checks if the state is terminal.
	 * 
	 * @param args
	 *            The state parameters.
	 * @return 1 if goal met, 0 if not terminal, -1 if terminal but goal not met.
	 */
	protected int isTerminal() {
		if (StateSpec.getInstance().isGoal(StateSpec.getInstance().getRete()))
			return 1;
		return 0;
	}

	/**
	 * Cleans up environment data.
	 */
	public abstract void cleanup();

	/**
	 * Initialise the environment.
	 * 
	 * @param runIndex
	 *            The index of the run this environment is being initialised
	 *            for.
	 * @param extraArg
	 *            Any extra args to give to the environment.
	 */
	public abstract void initialise(int runIndex, String[] extraArg);

	/**
	 * Starts the episode by compiling the initial set of observations and goal
	 * arguments.
	 * 
	 * @return The first observations of the episode.
	 */
	public final RRLObservations startEpisode() {
		// Initialise the environment observations
		startState();

		// Form the relational observations
		observations_ = formObservations(getGoalArgs(), true);
		return observations_;
	}

	/**
	 * Accepts actions and applies them, then returns the resultant
	 * observations.
	 * 
	 * @param actions
	 *            The actions to apply to the environment.
	 * @return The resultant observations.
	 */
	public final RRLObservations step(PolicyActions actions) {
		// Apply the actions
		Object groundAction = groundActions(actions);

		// Apply the action to the state
		stepState(groundAction);

		// Form the relational observations
		observations_ = formObservations(getGoalArgs(), false);
		return observations_;
	}
}
