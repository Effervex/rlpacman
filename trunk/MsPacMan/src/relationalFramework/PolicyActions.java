package relationalFramework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

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

	/**
	 * A constructor for a new ActionChoice, which initialises the action array.
	 */
	public PolicyActions() {
		activeActions_ = new ArrayList<Collection<FiredAction>>();
	}

	/**
	 * Switches on actions.
	 * 
	 * @param firedActions
	 *            The actions being switched on.
	 */
	public void addFiredRule(Collection<FiredAction> firedActions) {
		if ((firedActions != null) && (!firedActions.isEmpty())) {
			activeActions_.add(firedActions);
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
}
