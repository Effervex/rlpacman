package relationalFramework;

import relationalFramework.FiredAction;
import relationalFramework.PolicyActions;
import rrlFramework.RRLExperiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;


/**
 * This class represents the chosen actions the agent returns when making a
 * decision.
 * 
 * @author Samuel J. Sarjant
 * 
 */
public class PolicyActions {
	/**
	 * The prioritised list of switched actions. Each rule may return a list of
	 * actions.
	 */
	private ArrayList<Collection<FiredAction>> activeActions_;

	/** The rules that fired actions. */
	private Collection<RelationalPolicy> firedPolicies_;

	/**
	 * A constructor for a new ActionChoice, which initialises the action array.
	 */
	public PolicyActions() {
		activeActions_ = new ArrayList<Collection<FiredAction>>();
		firedPolicies_ = new HashSet<RelationalPolicy>();
	}

	/**
	 * Switches on actions.
	 * 
	 * @param firedActions
	 *            The actions being switched on.
	 * @param modularPolicy
	 *            The policy containing the rule that produced these actions.
	 */
	public void addFiredRule(Collection<FiredAction> firedActions,
			RelationalPolicy relationalPolicy) {
		if ((firedActions != null) && (!firedActions.isEmpty())) {
			activeActions_.add(firedActions);
			firedPolicies_.add(relationalPolicy);
		}
	}

	/**
	 * Gets the active actions.
	 * 
	 * @return The list of active actions, of arbitrary length.
	 */
	public ArrayList<Collection<FiredAction>> getActions() {
		return activeActions_;
	}

	/**
	 * Converts the action switch into a string version
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (Collection<FiredAction> actions : activeActions_) {
			buffer.append(actions + "\n");
		}
		return buffer.toString();
	}

	/**
	 * Gets the first action list. This may be all the environment needs.
	 * 
	 * @return The first list of actions.
	 */
	public Collection<FiredAction> getFirstActionList() {
		if (activeActions_.isEmpty())
			return null;
		return activeActions_.get(0);
	}

	/**
	 * Gets a random action from the collection of fired actions in the action
	 * list.
	 * 
	 * @return A random action from the first collection of fired actions.
	 */
	public RelationalPredicate getFirstRandomAction() {
		Collection<FiredAction> firedActions = getFirstActionList();
		if (firedActions == null)
			return null;
		List<FiredAction> actionsList = new ArrayList<FiredAction>(firedActions);
		FiredAction selectedAction = actionsList.get(RRLExperiment.random_
				.nextInt(actionsList.size()));
		selectedAction.triggerRule();
		return selectedAction.getAction();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((activeActions_ == null) ? 0 : activeActions_.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PolicyActions other = (PolicyActions) obj;
		if (activeActions_ == null) {
			if (other.activeActions_ != null)
				return false;
		} else if (!activeActions_.equals(other.activeActions_))
			return false;
		return true;
	}

	/**
	 * Gets an collection of all fired policies.
	 * 
	 * @return The list of firing policies.
	 */
	public Collection<RelationalPolicy> getFiredPolicies() {
		return firedPolicies_;
	}

	public boolean isEmpty() {
		return activeActions_.isEmpty();
	}
}
