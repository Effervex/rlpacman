package relationalFramework;

import java.util.Collection;

import jess.Fact;
import jess.Rete;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

/**
 * An agent that chooses its decisions based on a fixed policy, fed in via agent
 * message.
 * 
 * @author Sam Sarjant
 */
public class PolicyActor implements AgentInterface {
	/** The current agent policy. */
	private Policy policy_;

	/** The currently switched actions. */
	private ActionSwitch actionsModule_;

	/** The previous state seen by the agent. */
	private Collection<Fact> prevState_;

	/**
	 * If this agent is the optimal agent (defined by the environment, not how
	 * the agent sees itself).
	 */
	private boolean optimal_;

	// @Override
	public void agent_cleanup() {
	}

	// @Override
	public void agent_end(double arg0) {
		// Save the pre-goal state and goal action
		PolicyGenerator.getInstance().formPreGoalState(prevState_,
				actionsModule_.getPrioritisedActions());
		
		actionsModule_ = null;
	}

	// @Override
	public void agent_init(String arg0) {
		prevState_ = null;
		optimal_ = false;
	}

	// @Override
	public String agent_message(String arg0) {
		// Receive a policy
		if (arg0.equals("Policy")) {
			policy_ = (Policy) ObjectObservations.getInstance().objectArray[0];
		} else if (arg0.equals("Optimal")) {
			optimal_ = true;
		}
		return null;
	}

	// @Override
	public Action agent_start(Observation arg0) {
		// Initialising the actions
		actionsModule_ = new ActionSwitch(StateSpec.getInstance()
				.getNumActions());

		Action action = new Action(0, 0);
		action.charArray = ObjectObservations.OBSERVATION_ID.toCharArray();
		ObjectObservations.getInstance().objectArray = chooseAction(ObjectObservations
				.getInstance().predicateKB);

		return action;
	}

	// @Override
	public Action agent_step(double arg0, Observation arg1) {
		Action action = new Action(0, 0);
		action.charArray = ObjectObservations.OBSERVATION_ID.toCharArray();
		ObjectObservations.getInstance().objectArray = chooseAction(ObjectObservations
				.getInstance().predicateKB);

		return action;
	}

	/**
	 * Chooses the action based on what higher actions are switched on.
	 * 
	 * @param state
	 *            The state of the system as given by predicates.
	 * @return A relational action.
	 */
	private String[] chooseAction(Rete state) {
		actionsModule_.switchOffAll();
		// Evaluate the policy for true rules and activates
		policy_.evaluatePolicy(state, actionsModule_, StateSpec.getInstance()
				.getNumActions(), optimal_, false);

		// Save the previous state (if not an optimal agent).
		prevState_ = StateSpec.extractFacts(state);

		// Return the actions.
		return actionsModule_.getPrioritisedActions();
	}
}