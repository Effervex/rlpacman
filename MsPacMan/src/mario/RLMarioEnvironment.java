package mario;

import java.util.Arrays;
import java.util.List;

import jess.Rete;

import relationalFramework.PolicyActions;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalWrapper;
import relationalFramework.StateSpec;
import rrlFramework.RRLEnvironment;

import cerrla.PolicyGenerator;
import ch.idsia.benchmark.mario.engine.GlobalOptions;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;
import ch.idsia.tools.EvaluationInfo;
import ch.idsia.tools.MarioAIOptions;

public class RLMarioEnvironment extends RRLEnvironment {
	private static final boolean[] NO_ACTION = new boolean[Environment.numberOfKeys];
	private MarioAIOptions cmdLineOptions_;
	private MarioEnvironment environment_;
	private boolean experimentMode_ = false;
	private int levelDifficulty_ = 1;
	private RLMarioMovement marioMovement_;

	@Override
	protected void assertStateFacts(Rete rete, List<String> goalArgs)
			throws Exception {
		marioMovement_.assertStateFacts(rete, environment_);
	}

	// /**
	// * Runs the hand-coded policy.
	// */
	// private int handCodedPolicy() {
	// RelationalPolicy handCodedPolicy = StateSpec.getInstance()
	// .getHandCodedPolicy();
	//
	// // Run the policy through the environment until goal is satisfied.
	// PolicyActor handCodedAgent = new PolicyActor();
	// ObjectObservations.getInstance().objectArray = new RelationalPolicy[] {
	// handCodedPolicy };
	// handCodedAgent.agent_message("Optimal");
	// handCodedAgent.agent_message("SetPolicy");
	// ObjectObservations.getInstance().predicateKB = wrapper_
	// .formObservations(environment_);
	// Action act = handCodedAgent.agent_start(new Observation());
	// // Loop until the task is complete
	// Reward_observation_terminal rot = env_step(act);
	// while ((rot == null) || !rot.isTerminal()) {
	// handCodedAgent.agent_step(rot.r, rot.o);
	// rot = env_step(act);
	// }
	// int score = environment_.getEvaluationInfo().computeWeightedFitness();
	//
	// // Return the state to normal
	// resetEnvironment();
	// return score;
	// }

	/**
	 * Calculates the reward.
	 * 
	 * @return The reward received from the environment.
	 */
	@Override
	protected double calculateReward(boolean isTerminal) {
		if (terminal == 2) {
			EvaluationInfo ei = environment_.getEvaluationInfo();
			ei.timeSpent += ei.timeLeft;
			ei.timeLeft = 0;
			return ei.computeWeightedFitness();
		}
		if (terminal == 1) {
			return environment_.getEvaluationInfo().computeWeightedFitness();
		}
		return 0;
	}

	@Override
	protected List<String> getGoalArgs() {
		return null;
	}

	@Override
	protected Object groundActions(PolicyActions actions) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean isReteDriven() {
		return false;
	}

	/**
	 * Resets the environment back to normal.
	 */
	@Override
	protected void startState() {
		cmdLineOptions_.setLevelRandSeed(PolicyGenerator.random_.nextInt());
		cmdLineOptions_.setLevelDifficulty(levelDifficulty_);
		environment_.reset(cmdLineOptions_);
		if (!experimentMode_ && !GlobalOptions.isScale2x)
			GlobalOptions.changeScale2x();

		environment_.tick();

		while (GlobalOptions.isGameplayStopped) {
			// Idle...
			environment_.tick();
		}
	}

	@Override
	protected void stepState(Object action) {
		boolean[] groundAction = (boolean[]) action;

		environment_.performAction(groundAction);

		environment_.tick();

		while (GlobalOptions.isGameplayStopped) {
			// Idle...
			environment_.tick();
		}
	}

	@Override
	public void cleanup() {
		environment_ = null;
	}

	@Override
	public void initialise(int runIndex, String[] extraArgs) {
		environment_ = MarioEnvironment.getInstance();
		marioMovement_ = new RLMarioMovement();
		cmdLineOptions_ = new MarioAIOptions();
		cmdLineOptions_.setVisualization(!experimentMode_);

		for (String arg0 : extraArgs) {
			if (arg0.equals("-e")) {
				// Run the program in experiment mode (No GUI).
				experimentMode_ = true;
			} else if (arg0.startsWith("Diff")) {
				levelDifficulty_ = Integer.parseInt(arg0.split(" ")[1]);
			}
		}
	}
}
