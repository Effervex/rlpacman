package relationalFramework.agentObservations;

import java.util.List;

/**
 * A basic object for holding pre-goal state information for an action.
 * 
 * @author Samuel J. Sarjant
 */
public class PreGoalInformation {
	private List<String> state_;
	private List<String> actionTerms_;
	private int inactivity_ = 0;

	public PreGoalInformation(List<String> state, List<String> actionTerms) {
		state_ = state;
		actionTerms_ = actionTerms;
	}

	public int incrementInactivity() {
		inactivity_++;
		return inactivity_;
	}

	public void resetInactivity() {
		inactivity_ = 0;
	}

	public boolean isSettled() {
		if (inactivity_ < AgentObservations.INACTIVITY_THRESHOLD)
			return false;
		return true;
	}

	public boolean isRecentlyChanged() {
		if (inactivity_ == 0)
			return true;
		return false;
	}

	public List<String> getState() {
		return state_;
	}

	public List<String> getActionTerms() {
		return actionTerms_;
	}

	@Override
	public String toString() {
		return state_.toString() + " : " + actionTerms_.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		int actionResult = 0;
		for (String condition : actionTerms_)
			actionResult += condition.hashCode();
		result = prime * result + actionResult;
		int stateResult = 0;
		for (String condition : state_)
			stateResult += condition.hashCode();
		result = prime * result + stateResult;
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
		PreGoalInformation other = (PreGoalInformation) obj;
		if (actionTerms_ == null) {
			if (other.actionTerms_ != null)
				return false;
		} else if (!actionTerms_.containsAll(other.actionTerms_))
			return false;
		else if (!other.actionTerms_.containsAll(actionTerms_))
			return false;
		if (state_ == null) {
			if (other.state_ != null)
				return false;
		} else if (!state_.containsAll(other.state_))
			return false;
		else if (!other.state_.containsAll(state_))
			return false;
		return true;
	}
}