package relationalFramework;


/**
 * A small class for defining the type of values that can be in a defined
 * predicate. Values can be either constants, free variables, or tied variables.
 * 
 * @author Sam Sarjant
 */
public class PredTerm {
	/** If this PredTerm is using a concrete value. */
	public static final byte VALUE = 0;

	/** If this PredTerm is using a free value, with name as the value. */
	public static final byte FREE = 1;

	/**
	 * If this PredTerm is using a tied value, with name as value if no ties
	 * exist.
	 */
	public static final byte TIED = 2;

	/** The value of the Term. */
	private Object value_;

	/** The expected class of the term. */
	private Class termClass_;

	/** The type of PredTerm. */
	private byte type_;

	/**
	 * A constructor for the PredTerm.
	 * 
	 * @param value
	 *            The value of the term, or name if not VALUE.
	 * @param clazz
	 *            The expected class of the term.
	 * @param type
	 *            The type of the term: VALUE, FREE or TIED.
	 */
	public PredTerm(Object value, Class clazz, byte type) {
		value_ = value;
		type_ = type;
		termClass_ = clazz;
	}

	/**
	 * A constructor for the PredTerm using a value.
	 * 
	 * @param value
	 *            The value of the term.
	 */
	public PredTerm(Object value) {
		value_ = value;
		type_ = VALUE;
		termClass_ = value.getClass();
	}

	public Object getValue() {
		return value_;
	}

	public byte getTermType() {
		return type_;
	}

	public Class getValueType() {
		return termClass_;
	}

	public String toString() {
		return value_.toString();
	}

	@Override
	public boolean equals(Object object) {
		if ((object != null) && (object instanceof PredTerm)) {
			PredTerm otherTerm = (PredTerm) object;
			if (type_ == otherTerm.type_) {
				if ((type_ != VALUE) || (value_.equals(otherTerm.value_))) {
					if (termClass_.equals(termClass_)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return type_ + value_.hashCode() + termClass_.hashCode();
	}

	/**
	 * A convenience method for creating an array of PredTerms
	 * 
	 * @param objs
	 *            The objects from which to create the array.
	 * @return The array of the terms in PredTerm form.
	 */
	public static PredTerm[] createValueArray(Object[] objs) {
		PredTerm[] terms = new PredTerm[objs.length];
		for (int i = 0; i < objs.length; i++) {
			terms[i] = new PredTerm(objs[i]);
			StateSpec.getInstance().addConstant(objs[i].toString(), objs[i]);
		}
		return terms;
	}
}
