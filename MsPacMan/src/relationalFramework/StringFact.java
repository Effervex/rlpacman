package relationalFramework;

import java.util.Arrays;
import java.util.Map;

/**
 * A class containing the definitions of a Fact, which is usually converted to a
 * String.
 * 
 * @author Sam Sarjant
 */
public class StringFact implements Comparable<StringFact> {
	/** The fact name. */
	private String factName_;
	/** If this fact is negated (prefixed by not) */
	private boolean negated_ = false;
	/** The types of the fact arguments. */
	@SuppressWarnings("unchecked")
	private Class[] factTypes_;
	/** The actual arguments of the fact. */
	private String[] arguments_;

	/**
	 * A basic definition constructor.
	 * 
	 * @param factName
	 *            The name of the fact.
	 * @param factTypes
	 *            The types of the fact.
	 */
	@SuppressWarnings("unchecked")
	public StringFact(String factName, Class[] factTypes) {
		factName_ = factName;
		factTypes_ = factTypes;
		arguments_ = new String[factTypes.length];
		Arrays.fill(arguments_, "?");
	}

	/**
	 * A filled fact constructor.
	 * 
	 * @param fact
	 *            The fact definition to use.
	 * @param arguments
	 *            The arguments for the fact.
	 */
	public StringFact(StringFact fact, String[] arguments) {
		factName_ = fact.factName_;
		factTypes_ = fact.factTypes_;
		arguments_ = arguments;
	}

	/**
	 * A negated fact constructor.
	 * 
	 * @param fact
	 *            The fact definition to use.
	 * @param arguments
	 *            The arguments for the fact.
	 * @param negated
	 *            If this is negated.
	 */
	public StringFact(StringFact fact, String[] arguments, boolean negated) {
		factName_ = fact.factName_;
		factTypes_ = fact.factTypes_;
		arguments_ = arguments;
		negated_ = negated;
	}

	/**
	 * Constructor for a clone StringFact.
	 * 
	 * @param stringFact
	 *            The StringFact to clone.
	 */
	public StringFact(StringFact stringFact) {
		factName_ = stringFact.factName_;
		negated_ = stringFact.negated_;
		factTypes_ = Arrays.copyOf(stringFact.factTypes_,
				stringFact.factTypes_.length);
		arguments_ = Arrays.copyOf(stringFact.arguments_,
				stringFact.arguments_.length);
	}

	/**
	 * Replaces all occurrences of an argument with another value.
	 * 
	 * @param replacementMap
	 *            The replacement map for the arguments.
	 */
	public void replaceArguments(Map<String, String> replacementMap) {
		String[] newArguments = Arrays.copyOf(arguments_, arguments_.length);
		for (int i = 0; i < arguments_.length; i++) {
			for (String key : replacementMap.keySet()) {
				if (arguments_[i].equals(key))
					newArguments[i] = replacementMap.get(key);
			}
		}
		arguments_ = newArguments;
	}

	/**
	 * Gets the name of the fact.
	 * 
	 * @return The name of the fact.
	 */
	public String getFactName() {
		return factName_;
	}

	/**
	 * Gets the argument types for the fact.
	 * 
	 * @return The argument types.
	 */
	@SuppressWarnings("unchecked")
	public Class[] getArgTypes() {
		return factTypes_;
	}

	/**
	 * Gets the arguments for the fact.
	 * 
	 * @return The fact arguments.
	 */
	public String[] getArguments() {
		return arguments_;
	}

	public boolean isNegated() {
		return negated_;
	}

	public void swapNegated() {
		negated_ = !negated_;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		if (negated_)
			buffer.append("(not ");
		buffer.append("(" + factName_);
		for (int i = 0; i < arguments_.length; i++)
			buffer.append(" " + arguments_[i]);
		buffer.append(")");
		if (negated_)
			buffer.append(")");
		return buffer.toString();
	}

	@Override
	public int compareTo(StringFact sf) {
		if (negated_ != sf.negated_) {
			if (!negated_)
				return -1;
			else
				return 1;
		}

		int result = factName_.compareTo(sf.factName_);
		if (result != 0)
			return result;

		// Fact Types should be the same if both names are the same
		// Check arguments
		for (int i = 0; i < arguments_.length; i++) {
			result = arguments_[i].compareTo(sf.arguments_[i]);
			if (result != 0)
				return result;
		}
		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(arguments_);
		result = prime * result
				+ ((factName_ == null) ? 0 : factName_.hashCode());
		result = prime * result + (negated_ ? 1231 : 1237);
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
		StringFact other = (StringFact) obj;
		if (!Arrays.equals(arguments_, other.arguments_))
			return false;
		if (factName_ == null) {
			if (other.factName_ != null)
				return false;
		} else if (!factName_.equals(other.factName_))
			return false;
		if (negated_ != other.negated_)
			return false;
		return true;
	}
}
