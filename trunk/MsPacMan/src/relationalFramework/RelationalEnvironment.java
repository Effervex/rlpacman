package relationalFramework;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;

/**
 * An extension to the RL framework which passes Java objects instead of
 * primitives. This limits its multi-language support, but makes things more
 * efficient when performing relational operations.
 * 
 * @author Samuel J. Sarjant
 */
public abstract class RelationalEnvironment implements EnvironmentInterface {

	public void env_cleanup() {
		// TODO Auto-generated method stub

	}

	public String env_init() {
		// TODO Auto-generated method stub
		return null;
	}

	public String env_message(String arg0) {
		if (arg0.equals("maxSteps"))
			return getMaxSteps() + "";
		if (arg0.equals("freeze")) {
			PolicyGenerator.getInstance().freeze(true);
			return null;
		}
		if (arg0.equals("unfreeze")) {
			PolicyGenerator.getInstance().freeze(false);
			return null;
		}
		return null;
	}

	/**
	 * Get the maximum number of steps in an environment.
	 * 
	 * @return The maximum number of steps.
	 */
	protected abstract int getMaxSteps();

	public final Observation env_start() {
		// Call the relEnv_start method

		// TODO Auto-generated method stub
		return null;
	}
	
	public RelationalObservation relEnv_start() {
		// Use an optimal policy to create the initial pre-goal state
		return null;
	}

	public Reward_observation_terminal env_step(ActionChoice arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
