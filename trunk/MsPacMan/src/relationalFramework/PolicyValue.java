package relationalFramework;

/**
 * A simple class for binding a policy and a value together in a comparable
 * format. Also updates internal rule worth for rules within the policy.
 * 
 * @author Samuel J. Sarjant
 */
public class PolicyValue implements Comparable<PolicyValue> {
	/** The policy. */
	private Policy policy_;
	/** The estimated value of the policy. */
	private float value_;
	/** The iteration this policy value was created at. */
	private int iteration_;

	/**
	 * A constructor for storing the members.
	 * 
	 * @param pol
	 *            The policy.
	 * @param value
	 *            The (estimated) value
	 */
	public PolicyValue(Policy pol, float value, int iteration) {
		policy_ = pol;
		value_ = value;
		iteration_ = iteration;

		updateInternalRuleValues(pol, value);
	}

	/**
	 * Updates the internal rule values for the rules within the policy.
	 * 
	 * @param pol
	 *            The policy with the active rules.
	 * @param value
	 *            The value the policy achieved
	 */
	private void updateInternalRuleValues(Policy pol, float value) {
		for (GuidedRule rule : pol.getFiringRules()) {
			rule.updateInternalValue(value);
		}
	}

	/**
	 * Gets the policy for this policy-value.
	 * 
	 * @return The policy.
	 */
	public Policy getPolicy() {
		return policy_;
	}

	/**
	 * Gets the value for this policy-value.
	 * 
	 * @return The value.
	 */
	public float getValue() {
		return value_;
	}
	
	/**
	 * Gets the iteration the policy value was create at.
	 * 
	 * @return The iteration the value was created at.
	 */
	public int getIteration() {
		return iteration_;
	}

	// @Override
	public int compareTo(PolicyValue o) {
		if ((o == null) || (!(o instanceof PolicyValue)))
			return -1;
		PolicyValue pv = o;
		// If this value is bigger, it comes first
		if (value_ > pv.value_) {
			return -1;
		} else if (value_ < pv.value_) {
			// Else it is after
			return 1;
		}
		// If the values are equal, the more recent policy value is better to
		// keep.
		if (iteration_ < pv.iteration_)
			return 1;
		else if (iteration_ > pv.iteration_)
			return -1;
		// Otherwise, just compare by hashCode
		return Float.compare(hashCode(), o.hashCode());

	}

	// @Override
	@Override
	public boolean equals(Object obj) {
		if ((obj == null) || (!(obj instanceof PolicyValue)))
			return false;
		PolicyValue pv = (PolicyValue) obj;
		if (value_ == pv.value_) {
			if (policy_.equals(pv.policy_)) {
				if (iteration_ == pv.iteration_)
					return true;
			}
		}
		return false;
	}

	// @Override
	@Override
	public int hashCode() {
		return (int) (value_ * policy_.hashCode() * (iteration_ + 165781));
	}

	@Override
	public String toString() {
		return "Policy Value: " + value_;
	}
}