package jCloisterZone;

import relationalFramework.ObjectObservations;
import relationalFramework.PolicyActions;
import relationalFramework.RelationalWrapper;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;

import com.jcloisterzone.game.Game;

public class CarcassonneEnvironment implements EnvironmentInterface {
	private Game environment_;
	private boolean guiMode_ = false;
	private RelationalWrapper wrapper_;

	@Override
	public void env_cleanup() {
		environment_ = null;
	}

	@Override
	public String env_init() {
		wrapper_ = new CarcassonneRelationalWrapper();

		return null;
	}

	@Override
	public String env_message(String arg0) {
		if (arg0.equals("-e")) {
			// Run the program in experiment mode (No GUI).
			guiMode_ = true;
		} else {
			try {
				int numPlayers = Integer.parseInt(arg0);
				// TODO Set the players.
				// TODO If using multiple policies, may need to redesign RLGlue
				// structure

			} catch (Exception e) {
			}
		}
		return null;
	}

	@Override
	public Observation env_start() {
		resetEnvironment();

		environment_.start();

		ObjectObservations.getInstance().predicateKB = wrapper_
				.formObservations(environment_);
		return new Observation();
	}

	@Override
	public Reward_observation_terminal env_step(Action arg0) {
		// Applying the action (up down left right or nothing)
		PolicyActions actions = (PolicyActions) ObjectObservations
				.getInstance().objectArray[0];

		Reward_observation_terminal rot = new Reward_observation_terminal(0,
				new Observation(), 0);
		return rot;
	}

	/**
	 * Resets the environment back to normal.
	 */
	public void resetEnvironment() {
		// TODO Reset (start new game)
	}
}
