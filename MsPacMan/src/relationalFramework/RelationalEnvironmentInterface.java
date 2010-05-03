package relationalFramework;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;

/**
 * An extension to the RL framework which passes Java objects instead of
 * primitives. This limits its multi-language support, but makes things more
 * efficient when performing relational operations.
 * 
 * @author Samuel J. Sarjant
 */
public abstract class RelationalEnvironmentInterface {

	public void env_cleanup() {
		// TODO Auto-generated method stub
		
	}

	public String env_init() {
		// TODO Auto-generated method stub
		return null;
	}

	public String env_message(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public RelationalObservation env_start() {
		// Use an optimal policy to create the initial pre-goal state
		
		// TODO Auto-generated method stub
		return null;
	}

	public Reward_observation_terminal env_step(ActionChoice arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
