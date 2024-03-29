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
 *    src/mario/RLMarioEnvironment.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package mario;

import java.util.Arrays;
import java.util.List;

import jess.Rete;

import relationalFramework.PolicyActions;
import relationalFramework.StateSpec;
import rrlFramework.RRLEnvironment;
import rrlFramework.RRLExperiment;

import cerrla.ProgramArgument;
import ch.idsia.benchmark.mario.engine.GlobalOptions;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;
import ch.idsia.tools.EvaluationInfo;
import ch.idsia.tools.MarioAIOptions;

public class RLMarioEnvironment extends RRLEnvironment {
	private static final boolean[] NO_ACTION = new boolean[Environment.numberOfKeys];
	private static final boolean[] HOLD_ACTION = {false, false, false, false, true, false};
	private static final int TIMEOUT_THRESHOLD = 30;
	private MarioAIOptions cmdLineOptions_;
	private MarioEnvironment environment_;
	private boolean experimentMode_ = false;
	private int levelDifficulty_ = 1;
	private RLMarioMovement marioMovement_;
	private int noActionCount_;

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
	protected double[] calculateReward(int isTerminal) {
		double[] reward = new double[2];
		if (isTerminal == TERMINAL_LOSE) {
			EvaluationInfo ei = environment_.getEvaluationInfo();
			ei.timeSpent += ei.timeLeft;
			ei.timeLeft = 0;
			reward[0] = ei.computeWeightedFitness();
		} else if (isTerminal == TERMINAL_WIN) {
			reward[0] = environment_.getEvaluationInfo()
					.computeWeightedFitness();
		}
		reward[1] = reward[0];
		return reward;
	}

	@Override
	protected List<String> getGoalArgs() {
		return null;
	}

	@Override
	protected Object groundActions(PolicyActions actions) {
		return marioMovement_.groundActions(actions, environment_);
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
		cmdLineOptions_.setLevelRandSeed(RRLExperiment.random_.nextInt());
		cmdLineOptions_.setLevelDifficulty(levelDifficulty_);
//		cmdLineOptions_.setLevelLength(128);
//		cmdLineOptions_.setTimeLimit(100);
		environment_.reset(cmdLineOptions_);
		if (!experimentMode_ && !GlobalOptions.isScale2x)
			GlobalOptions.changeScale2x();

		environment_.tick();

		while (GlobalOptions.isGameplayStopped) {
			// Idle...
			environment_.tick();
		}

		noActionCount_ = 0;
	}

	@Override
	protected void stepState(Object action) {
		boolean[] groundAction = (boolean[]) action;
		if (Arrays.equals(groundAction, NO_ACTION))
			noActionCount_++;
		else
			noActionCount_ = 0;

		environment_.performAction(groundAction);

		environment_.tick();

		while (GlobalOptions.isGameplayStopped) {
			// Idle...
			environment_.tick();
		}
	}

	@Override
	protected int isTerminal() {
		int result = super.isTerminal();
		if (environment_.isLevelFinished())
			return 1;
		if (noActionCount_ >= TIMEOUT_THRESHOLD)
			return -1;
		return result;
	}

	@Override
	public void cleanup() {
		environment_ = null;
	}

	@Override
	public void initialise(int runIndex, String[] extraArgs) {
		environment_ = MarioEnvironment.getInstance();
		marioMovement_ = new RLMarioMovement();

		String goalName = StateSpec.getInstance().getGoalName();
		levelDifficulty_ = Integer.parseInt(goalName.substring(4));

		cmdLineOptions_ = new MarioAIOptions();
		cmdLineOptions_.setVisualization(!ProgramArgument.EXPERIMENT_MODE
				.booleanValue());
	}
}
