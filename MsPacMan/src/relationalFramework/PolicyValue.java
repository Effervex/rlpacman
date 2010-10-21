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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + iteration_;
		result = prime * result + ((policy_ == null) ? 0 : policy_.hashCode());
		result = prime * result + Float.floatToIntBits(value_);
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
		PolicyValue other = (PolicyValue) obj;
		if (iteration_ != other.iteration_)
			return false;
		if (policy_ == null) {
			if (other.policy_ != null)
				return false;
		} else if (!policy_.equals(other.policy_))
			return false;
		if (Float.floatToIntBits(value_) != Float.floatToIntBits(other.value_))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Policy Value: " + value_;
	}
}