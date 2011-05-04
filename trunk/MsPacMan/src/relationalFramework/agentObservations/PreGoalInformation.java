package relationalFramework.agentObservations;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import relationalFramework.Module;
import relationalFramework.MultiMap;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

/**
 * A basic object for holding pre-goal state information for an action.
 * 
 * @author Samuel J. Sarjant
 */
public class PreGoalInformation implements Serializable {
	private static final long serialVersionUID = 83704495471286796L;
	private Collection<StringFact> state_;
	private String[] actionTerms_;
	private MultiMap<String, String> termTypes_;
	private int inactivity_ = 0;

	public PreGoalInformation(Collection<StringFact> state, String[] actionTerms) {
		state_ = state;
		actionTerms_ = actionTerms;
		determineTermTypes();
	}

	/**
	 * Determines what type any constant terms are.
	 */
	private void determineTermTypes() {
		// Check if there are constant terms at all
		Collection<String> constantTerms = new HashSet<String>();
		for (String term : actionTerms_) {
			if ((term.charAt(0) != '?' || term
					.contains(Module.MOD_VARIABLE_PREFIX))
					&& !StateSpec.isNumber(term)) {
				constantTerms.add(term);
			}
		}
		// If no constants, stop there.
		if (constantTerms.isEmpty())
			return;

		termTypes_ = MultiMap.createSortedSetMultiMap();
		// Run through the state
		for (StringFact stateFact : state_) {
			// Only look at type preds
			if (StateSpec.getInstance()
					.isTypePredicate(stateFact.getFactName())) {
				if (constantTerms.contains(stateFact.getArguments()[0])) {
					termTypes_.put(stateFact.getArguments()[0], stateFact
							.getFactName());
				}
			}
		}
	}

	/**
	 * Gets the types for a given action term.
	 * 
	 * @param term
	 *            The action term of the pre-goal.
	 * @return The types this term is under (only a lineage).
	 */
	public Collection<String> getTermTypes(String term) {
		if (termTypes_ != null)
			return termTypes_.get(term);
		return null;
	}

	public int incrementInactivity() {
		inactivity_++;
		return inactivity_;
	}

	public void resetInactivity() {
		inactivity_ = 0;
		determineTermTypes();
	}

	public boolean isRecentlyChanged() {
		if (inactivity_ == 0)
			return true;
		return false;
	}

	public Collection<StringFact> getState() {
		return state_;
	}

	public String[] getActionTerms() {
		return actionTerms_;
	}

	@Override
	public String toString() {
		return state_.toString() + " : " + Arrays.toString(actionTerms_);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(actionTerms_);
		int subresult = 1;
		for (StringFact fact : state_)
			subresult = prime * subresult + fact.hashCode();
		result = prime * subresult;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PreGoalInformation other = (PreGoalInformation) obj;
		if (!Arrays.equals(actionTerms_, other.actionTerms_))
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