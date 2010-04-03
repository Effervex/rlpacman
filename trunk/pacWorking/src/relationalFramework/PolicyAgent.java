package relationalFramework;

import java.util.List;

import org.mandarax.kernel.KnowledgeBase;
import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

/**
 * An agent that chooses its decisions based on a fixed policy, fed in via agent message.
 * 
 * @author Sam Sarjant
 */
public class PolicyAgent implements AgentInterface {
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
		if (arg0.equals("Policy")) {
			policy_ = (Policy) ObjectObservations.getInstance().objectArray[0];
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
		action.charArray = ObjectObservations.OBSERVATION_ID.toCharArray();
		ObjectObservations.getInstance().objectArray = chooseAction(ObjectObservations.getInstance().predicateKB);
		
		return action;
	}

	// @Override
	public Action agent_step(double arg0, Observation arg1) {
		Action action = new Action(0, 0);
		action.charArray = ObjectObservations.OBSERVATION_ID.toCharArray();
		ObjectObservations.getInstance().objectArray = chooseAction(ObjectObservations.getInstance().predicateKB);

		return action;
	}

	/**
	 * Chooses the action based on what higher actions are switched on.
	 * 
	 * @param state
	 *            The state of the system as given by predicates.
	 * @return A relational action.
	 */
	private List[] chooseAction(KnowledgeBase state) {
		actionsModule_.switchOffAll();
		// Evaluate the policy for true rules and activates
		policy_.evaluatePolicy(state, actionsModule_);

		// Return the actions.
		return actionsModule_.getPrioritisedActions();
	}
}