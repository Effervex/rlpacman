package cerrla;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;

import org.apache.commons.collections.BidiMap;


/**
 * A unified fact is the resulting information from a successful unification.
 * 
 * @author Sam Sarjant
 */
public class UnifiedFact {
	/** The resulting unified fact. */
	private RelationalPredicate resultFact_;

	/** The action terms associated with the result fact. */
	private RelationalArgument[] factTerms_;

	/** The fact used for the unification. */
	private RelationalPredicate unityFact_;

	/** The necessary replacements to turn unityFact into resultFact. */
	private BidiMap resultReplacements_;

	/** The generalisation of this unification. */
	private int generalisation_;

	public UnifiedFact(RelationalPredicate resultFact, RelationalArgument[] factTerms,
			RelationalPredicate unityFact, BidiMap replacements,
			int generalisation) {
		resultFact_ = resultFact;
		factTerms_ = factTerms;
		unityFact_ = unityFact;
		resultReplacements_ = replacements;
		generalisation_ = generalisation;
	}

	public RelationalPredicate getResultFact() {
		return resultFact_;
	}

	public RelationalArgument[] getFactTerms() {
		return factTerms_;
	}

	public RelationalPredicate getUnityFact() {
		return unityFact_;
	}

	public int getGeneralisation() {
		return generalisation_;
	}

	public BidiMap getResultReplacements() {
		return resultReplacements_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((resultFact_ == null) ? 0 : resultFact_.hashCode());
		result = prime * result
				+ ((unityFact_ == null) ? 0 : unityFact_.hashCode());
		result = prime * result + generalisation_;
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
		UnifiedFact other = (UnifiedFact) obj;
		if (resultFact_ == null) {
			if (other.resultFact_ != null)
				return false;
		} else if (!resultFact_.equals(other.resultFact_))
			return false;
		if (unityFact_ == null) {
			if (other.unityFact_ != null)
				return false;
		} else if (!unityFact_.equals(other.unityFact_))
			return false;
		else if (generalisation_ != other.generalisation_)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return resultFact_.toString();
	}
}
