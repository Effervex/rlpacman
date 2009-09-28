package rlPacMan;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import crossEntropyFramework.ActionSwitch;
import crossEntropyFramework.Policy;

public class PacManAgent implements AgentInterface {
	/** The current agent policy. */
	private Policy policy_;
	/** The currently switched actions. */
	private ActionSwitch actionsModule_;

	// @Override
	public void agent_cleanup() {
		// Save the generator values

		// Save the final policy
	}

	// @Override
	public void agent_end(double arg0) {
		actionsModule_ = null;		
	}

	// @Override
	public void agent_init(String arg0) {
		// Initialise the generator

	}

	// @Override
	public String agent_message(String arg0) {
		// Receive a policy
		if (arg0.substring(0, Policy.PREFIX.length()).equals(Policy.PREFIX)) {
			policy_ = Policy.parsePolicy(arg0);
			System.out.println(policy_);
		} else if (arg0.equals("getFired")) {
			return policy_.getFiredString();
		}
		return null;
	}

	// @Override
	public Action agent_start(Observation arg0) {
		// Initialising the actions
		actionsModule_ = new ActionSwitch();

		Action action = new Action(0, 0);
		action.intArray = chooseAction(arg0.doubleArray);
		return action;
	}

	// @Override
	public Action agent_step(double arg0, Observation arg1) {
		Action action = new Action(0, 0);
		action.intArray = chooseAction(arg1.doubleArray);

		return action;
	}

	/**
	 * Chooses the action based on what higher actions are switched on.
	 * 
	 * @param observations
	 *            The observations made, sorted by PacManObservations.
	 * @return A high-level action based on the current observations and
	 *         switches.
	 */
	private int[] chooseAction(double[] observations) {
		// Evaluate the policy for true rules and activates
		policy_.evaluatePolicy(observations, actionsModule_);

		// Return the actions.
		return actionsModule_.getPrioritisedActions();
	}
}