package msPacManVsGhosts;

import game.controllers.GhostController;
import game.controllers.examples.Legacy;
import game.controllers.examples.Legacy2TheReckoning;
import game.core.G;
import game.core.GameView;
import game.core._G_;
import relationalFramework.ObjectObservations;
import relationalFramework.PolicyActions;
import relationalFramework.RelationalEnvironment;
import relationalFramework.StateSpec;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;


public class MsPacManGhostEnvironment implements EnvironmentInterface {
	public static int playerDelay_ = 0;

	private _G_ game_;
	private boolean experimentMode_ = false;
	private GameView gv_;
	private int prevScore_;
	private int prevLives_;
	private RelationalEnvironment wrapper_;
	private GhostController ghosts_;

	/**
	 * Calculates the reward between states.. May also include an artificial
	 * reward of -10000 if a life is lost.
	 * 
	 * @return The difference in score.
	 */
	private double calculateReward() {
		double scoreDiff = game_.getScore() - prevScore_;
		prevScore_ = game_.getScore();
		if (StateSpec.getInstance().getGoalName().equals("survive")
				&& game_.getLivesRemaining() < prevLives_)
			scoreDiff -= (prevLives_ - game_.getLivesRemaining()) * 10000;
		prevLives_ = game_.getLivesRemaining();
		return scoreDiff;
	}

	@Override
	public void env_cleanup() {
		game_ = null;
		wrapper_ = null;
	}

	@Override
	public String env_init() {
		game_ = new _G_();
		wrapper_ = new MsPacManGhostRelationalWrapper();
		return null;
	}

	@Override
	public String env_message(String arg0) {
		if (arg0.equals("maxSteps")) {
			return 10000 + "";
		} else if (arg0.equals("-e")) {
			// Run the program in experiment mode (No GUI).
			experimentMode_ = true;
		} else {
			try {
				int delay = Integer.parseInt(arg0);
				playerDelay_ = delay;
			} catch (Exception e) {

			}
		}
		return null;
	}

	@Override
	public Observation env_start() {
		resetEnvironment();

		ObjectObservations.getInstance().predicateKB = wrapper_
				.formObservations(game_);
		return new Observation();
	}

	@Override
	public Reward_observation_terminal env_step(Action arg0) {
		// Applying the action (up down left right or nothing)
		PolicyActions actions = (PolicyActions) ObjectObservations
				.getInstance().objectArray[0];
		int action = (Integer) wrapper_.groundActions(actions, game_);
		//for (int i = 0; i < 2; i++)
			game_.advanceGame(action, ghosts_.getActions(game_, G.DELAY));
		if (!experimentMode_) {
			gv_.repaint();
			//GameView.addPoints(game_, Color.GREEN, action);
		}

		ObjectObservations.getInstance().predicateKB = wrapper_
				.formObservations(game_);
		Reward_observation_terminal rot = new Reward_observation_terminal(
				calculateReward(), new Observation(),
				wrapper_.isTerminal(game_));
		return rot;
	}

	/**
	 * Resets the environment back to normal.
	 */
	public void resetEnvironment() {
		game_.newGame();
		if (!experimentMode_ && gv_ == null)
			gv_ = new GameView(game_).showGame();
		// TODO Define proper Ms. Pac-Man Ghost behaviour
		ghosts_ = new Legacy2TheReckoning();
		prevScore_ = game_.getScore();
		prevLives_ = game_.getLivesRemaining();
	}
}
