package relationalFramework;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A class containing the definitions of a Fact, which is usually converted to a
 * String.
 * 
 * @author Sam Sarjant
 */
public class StringFact implements Comparable<StringFact> {
	private final int CONST = 0;
	private final int VAR = 1;
	private final int ANON = 2;
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
	 * Constructor for a clone StringFact with the arguments replaced.
	 * 
	 * @param stringFact
	 *            The StringFact to clone.
	 * @param replacementMap
	 *            The replacement map for the arguments.
	 * @param retainOtherArgs
	 *            If arguments that have no replacement should be retained (or
	 *            turned anonymous).
	 */
	public StringFact(StringFact stringFact,
			Map<String, String> replacementMap, boolean retainOtherArgs) {
		this(stringFact);
		replaceArguments(replacementMap, retainOtherArgs);
	}

	/**
	 * Replaces all occurrences of an argument with another value.
	 * 
	 * @param replacementMap
	 *            The replacement map for the arguments.
	 * @param retainOtherArgs
	 *            If arguments that have no replacement should be retained (or
	 *            turned anonymous).
	 */
	public void replaceArguments(Map<String, String> replacementMap,
			boolean retainOtherArgs) {
		String[] newArguments = Arrays.copyOf(arguments_, arguments_.length);
		for (int i = 0; i < arguments_.length; i++) {
			boolean hasReplacement = false;
			for (String key : replacementMap.keySet()) {
				if (arguments_[i].equals(key)) {
					newArguments[i] = replacementMap.get(key);
					hasReplacement = true;
				}
			}

			if (!retainOtherArgs && !hasReplacement)
				newArguments[i] = "?";
		}
		arguments_ = newArguments;
	}

	/**
	 * Replaces all variable indexed terms (?X,?Y,?Z...) with the given indexed
	 * terms, be they variable or otherwise.
	 * 
	 * @param replacements
	 *            The terms to replace any variable terms with.
	 */
	public void replaceArguments(String[] replacements) {
		// Run through the arguments, replacing variable args with the
		// replacements.
		for (int i = 0; i < arguments_.length; i++) {
			int termIndex = Covering.getVariableTermIndex(arguments_[i]);
			if (termIndex != -1)
				arguments_[i] = replacements[termIndex];
		}
	}

	/**
	 * Replaces a single argument by another.
	 * 
	 * @param replacedTerm
	 *            The term to be replaced.
	 * @param replacementTerm
	 *            the term to replace the old term.
	 */
	public void replaceArguments(String replacedTerm, String replacementTerm) {
		for (int i = 0; i < arguments_.length; i++) {
			if (arguments_[i].equals(replacedTerm))
				arguments_[i] = replacementTerm;
		}
	}

	/**
	 * Creates a variable term replacement map using this StringFacts
	 * non-numerical arguments as the terms being replaced.
	 * 
	 * @return A replacement map which converts this string fact's non-numerical
	 *         terms into ordered variable terms.
	 */
	public Map<String, String> createVariableTermReplacementMap() {
		Map<String, String> replacementMap = new HashMap<String, String>();
		for (int i = 0; i < arguments_.length; i++) {
			if (!Number.class.isAssignableFrom(factTypes_[i]))
				replacementMap.put(arguments_[i], Covering
						.getVariableTermString(i));
		}
		return replacementMap;
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
			// Special comparison here: constants outrank variables outrank
			// anonymous
			int argType0 = CONST;
			int argType1 = CONST;

			if (arguments_[i].charAt(0) == '?') {
				if (arguments_[i].length() > 1)
					argType0 = VAR;
				else
					argType0 = ANON;
			}

			if (sf.arguments_[i].charAt(0) == '?') {
				if (sf.arguments_[i].length() > 1)
					argType1 = VAR;
				else
					argType1 = ANON;
			}

			if (argType0 < argType1)
				return -1;
			if (argType0 > argType1)
				return 1;
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
