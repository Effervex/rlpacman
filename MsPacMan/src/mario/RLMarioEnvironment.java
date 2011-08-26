package mario;

import relationalFramework.ObjectObservations;
import relationalFramework.PolicyActions;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalWrapper;
import relationalFramework.StateSpec;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;

import cerrla.PolicyActor;
import cerrla.PolicyGenerator;
import ch.idsia.benchmark.mario.engine.GlobalOptions;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;
import ch.idsia.tools.MarioAIOptions;


public class RLMarioEnvironment implements EnvironmentInterface {
	private MarioAIOptions cmdLineOptions_;
	private MarioEnvironment environment_;
	private boolean experimentMode_ = false;
	private int levelDifficulty_;
	private RelationalWrapper wrapper_;

	/**
	 * Calculates the reward.
	 * 
	 * @return The reward received from the environment.
	 */
	private double calculateReward(int terminal) {
		if (terminal == 1 || terminal == 2) {
			return environment_.getEvaluationInfo().computeWeightedFitness();
		}
		return 0;
	}

	/**
	 * Runs the hand-coded policy.
	 */
	private int handCodedPolicy() {
		RelationalPolicy handCodedPolicy = StateSpec.getInstance()
				.getHandCodedPolicy();

		// Run the policy through the environment until goal is satisfied.
		PolicyActor handCodedAgent = new PolicyActor();
		ObjectObservations.getInstance().objectArray = new RelationalPolicy[] { handCodedPolicy };
		handCodedAgent.agent_message("Optimal");
		handCodedAgent.agent_message("SetPolicy");
		ObjectObservations.getInstance().predicateKB = wrapper_
				.formObservations(environment_);
		Action act = handCodedAgent.agent_start(new Observation());
		// Loop until the task is complete
		Reward_observation_terminal rot = env_step(act);
		while ((rot == null) || !rot.isTerminal()) {
			handCodedAgent.agent_step(rot.r, rot.o);
			rot = env_step(act);
		}
		int score = environment_.getEvaluationInfo().computeWeightedFitness();

		// Return the state to normal
		resetEnvironment();
		return score;
	}

	@Override
	public void env_cleanup() {
		environment_ = null;
	}

	@Override
	public String env_init() {
		environment_ = MarioEnvironment.getInstance();
		cmdLineOptions_ = new MarioAIOptions();
		cmdLineOptions_.setVisualization(!experimentMode_);
		wrapper_ = new RLMarioRelationalWrapper();

		return null;
	}

	@Override
	public String env_message(String arg0) {
		if (arg0.equals("maxSteps")) {
			return 10000 + "";
		} else if (arg0.equals("freeze")) {
			PolicyGenerator.getInstance().freeze(true);
			return null;
		} else if (arg0.equals("unfreeze")) {
			PolicyGenerator.getInstance().freeze(false);
			return null;
		} else if (arg0.equals("-e")) {
			// Run the program in experiment mode (No GUI).
			experimentMode_ = true;
		}
		return null;
	}

	@Override
	public Observation env_start() {
		levelDifficulty_ = 1;
		resetEnvironment();

		environment_.tick();

		while (GlobalOptions.isGameplayStopped) {
			// Idle...
			environment_.tick();
		}

		ObjectObservations.getInstance().predicateKB = wrapper_
				.formObservations(environment_);
		return new Observation();
	}

	@Override
	public Reward_observation_terminal env_step(Action arg0) {
		// Applying the action (up down left right or nothing)
		PolicyActions actions = (PolicyActions) ObjectObservations
				.getInstance().objectArray[0];
		boolean[] groundAction = (boolean[]) wrapper_.groundActions(actions,
				environment_);
		environment_.performAction(groundAction);
		environment_.tick();

		while (GlobalOptions.isGameplayStopped) {
			// Idle...
			environment_.tick();
		}

		ObjectObservations.getInstance().predicateKB = wrapper_
				.formObservations(environment_);
		int terminal = wrapper_.isTerminal(environment_);
		double reward = calculateReward(terminal);

		Reward_observation_terminal rot = new Reward_observation_terminal(
				reward, new Observation(), terminal);
		return rot;
	}

	/**
	 * Resets the environment back to normal.
	 */
	public void resetEnvironment() {
		cmdLineOptions_.setLevelRandSeed(PolicyGenerator.random_.nextInt());
		cmdLineOptions_.setLevelDifficulty(levelDifficulty_);
		environment_.reset(cmdLineOptions_);
		if (!experimentMode_ && !GlobalOptions.isScale2x)
			GlobalOptions.changeScale2x();
	}
}
