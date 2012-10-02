/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/cerrla/PolicyValue.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package cerrla;

import java.io.Serializable;

import cerrla.modular.ModularPolicy;

import relationalFramework.RelationalRule;

/**
 * A simple class for binding a policy and a value together in a comparable
 * format. Also updates internal rule worth for rules within the policy.
 * 
 * @author Samuel J. Sarjant
 */
public class PolicyValue implements Comparable<PolicyValue>, Serializable {
	private static final long serialVersionUID = 6807660104018734424L;
	/** The policy. */
	private ModularPolicy policy_;
	/** The estimated value of the policy. */
	private double value_;
	/** The iteration this policy value was created at. */
	private int iteration_;

	/**
	 * A constructor for storing the members.
	 * 
	 * @param currentPolicy_
	 *            The policy.
	 * @param value
	 *            The (estimated) value
	 */
	public PolicyValue(ModularPolicy currentPolicy_, double value, int iteration) {
		policy_ = currentPolicy_;
		value_ = value;
		iteration_ = iteration;

		updateInternalRuleValues(currentPolicy_, value);
	}

	/**
	 * Updates the internal rule values for the rules within the policy.
	 * 
	 * @param pol
	 *            The policy with the active rules.
	 * @param value
	 *            The value the policy achieved
	 */
	private void updateInternalRuleValues(ModularPolicy pol,
			double value) {
		if (pol != null)
			for (RelationalRule reo : pol.getFiringRules()) {
				reo.updateInternalValue(value);
			}
	}

	/**
	 * Gets the policy for this policy-value.
	 * 
	 * @return The policy.
	 */
	public ModularPolicy getPolicy() {
		return policy_;
	}

	/**
	 * Gets the value for this policy-value.
	 * 
	 * @return The value.
	 */
	public double getValue() {
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
	@Override
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
		long temp;
		temp = Double.doubleToLongBits(value_);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		if (Double.doubleToLongBits(value_) != Double
				.doubleToLongBits(other.value_))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return value_ + ": " + policy_;
	}
}