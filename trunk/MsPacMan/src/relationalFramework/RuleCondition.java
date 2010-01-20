package relationalFramework;

import java.util.List;

import org.mandarax.kernel.Fact;

/**
 * A small internal class for representing which conditions make up a rule.
 * 
 * @author Samuel J. Sarjant
 */
public class RuleCondition {
	private List conditions_;

	public RuleCondition(List prereqs) {
		conditions_ = prereqs;
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj != null) && (obj instanceof RuleCondition)) {
			RuleCondition other = (RuleCondition) obj;
			// If the lists contain the same elements
			if ((conditions_.containsAll(other.conditions_))
					&& (other.conditions_.containsAll(conditions_)))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return conditions_.hashCode();
	}

	public Fact[] getFactArray() {
		return (Fact[]) conditions_.toArray(new Fact[conditions_.size()]);
	}

	@Override
	public String toString() {
		return conditions_.toString();
	}
}
