package cerrla;

import java.io.Serializable;
import java.util.Arrays;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;

/**
 * A class which defines a specialsiation operator.
 * 
 * @author Sam Sarjant
 */
public class SpecialisationOperator implements Serializable,
		Comparable<SpecialisationOperator> {
	private static final long serialVersionUID = -840644767403204492L;

	/** The predicate added. */
	private RelationalPredicate addedPredicate_;

	/** The term swapped. */
	private String[] swappedTerm_;

	/** The range split. */
	private RelationalArgument splitRange_;

	/** The type of specialisation this class represents. */
	private int specialisationType_;

	public static final int PREDICATE = 0;
	public static final int RANGE_SPLIT = 1;
	public static final int TERM_SWAP = 2;

	/**
	 * A constructor for an added predicate specialisation operation.
	 * 
	 * @param addedPredicate
	 *            The predicate that was added.
	 */
	public SpecialisationOperator(RelationalPredicate addedPredicate) {
		addedPredicate_ = addedPredicate;
		specialisationType_ = PREDICATE;
	}

	/**
	 * A constructor for a swapped term specialisation operation.
	 * 
	 * @param term
	 *            The term to swap.
	 * @paran swap The term to replace term with.
	 */
	public SpecialisationOperator(String term, String swap) {
		swappedTerm_ = new String[] { term, swap };
		specialisationType_ = TERM_SWAP;
	}

	/**
	 * A constructor for a range split specialisation operation.
	 * 
	 * @param rangeSplit
	 *            The bounds of the range split.
	 */
	public SpecialisationOperator(RelationalArgument rangeSplit) {
		splitRange_ = rangeSplit;
		specialisationType_ = RANGE_SPLIT;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((addedPredicate_ == null) ? 0 : addedPredicate_.hashCode());
		result = prime * result + specialisationType_;
		result = prime * result
				+ ((splitRange_ == null) ? 0 : splitRange_.hashCode());
		result = prime * result + Arrays.hashCode(swappedTerm_);
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
		SpecialisationOperator other = (SpecialisationOperator) obj;
		if (specialisationType_ != other.specialisationType_)
			return false;
		if (addedPredicate_ == null) {
			if (other.addedPredicate_ != null)
				return false;
		} else if (!addedPredicate_.equals(other.addedPredicate_))
			return false;
		if (splitRange_ == null) {
			if (other.splitRange_ != null)
				return false;
		} else if (!splitRange_.equals(other.splitRange_))
			return false;
		if (!Arrays.equals(swappedTerm_, other.swappedTerm_))
			return false;
		return true;
	}

	@Override
	public String toString() {
		switch (specialisationType_) {
		case PREDICATE:
			return addedPredicate_.toString();
		case TERM_SWAP:
			return "[" + swappedTerm_[0] + "/" + swappedTerm_[1] + "]";
		case RANGE_SPLIT:
			return splitRange_.toString();
		}
		return "?";
	}

	@Override
	public int compareTo(SpecialisationOperator o) {
		int result = Double.compare(specialisationType_, o.specialisationType_);
		if (result != 0)
			return result;

		switch (specialisationType_) {
		case PREDICATE:
			return addedPredicate_.compareTo(o.addedPredicate_);
		case TERM_SWAP:
			result = swappedTerm_[0].compareTo(o.swappedTerm_[0]);
			if (result != 0)
				return result;
			result = swappedTerm_[1].compareTo(o.swappedTerm_[1]);
			if (result != 0)
				return result;
			break;
		case RANGE_SPLIT:
			return splitRange_.compareTo(o.splitRange_);
		}
		return 0;
	}
}
