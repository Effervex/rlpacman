package relationalFramework;

import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import cerrla.RuleCreation;

/**
 * A class containing the definitions/instantiations of a relational predicate,
 * which is usually converted to a String.
 * 
 * @author Sam Sarjant
 */
public class RelationalPredicate implements Comparable<RelationalPredicate>,
		Serializable {
	private static final long serialVersionUID = -8297103733587908025L;
	private final int CONST = 0;
	private final int VAR = 1;
	private final int ANON = 2;
	/** The fact name. */
	private String factName_;
	/** If this fact is negated (prefixed by not) */
	private boolean negated_ = false;
	/** The types of the fact arguments. */
	private String[] factTypes_;
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
	public RelationalPredicate(String factName, String[] factTypes) {
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
	public RelationalPredicate(RelationalPredicate fact, String[] arguments) {
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
	public RelationalPredicate(RelationalPredicate fact, String[] arguments,
			boolean negated) {
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
	public RelationalPredicate(RelationalPredicate stringFact) {
		factName_ = stringFact.factName_;
		negated_ = stringFact.negated_;
		factTypes_ = stringFact.factTypes_;
		arguments_ = stringFact.arguments_.clone();
	}

	/**
	 * Replaces all occurrences of an argument with another value.
	 * 
	 * @param replacementMap
	 *            The replacement map for the arguments.
	 * @param retainOtherArgs
	 *            If arguments that have no replacement should be retained (or
	 *            turned anonymous).
	 * @param retainNumbers
	 *            If not retaining other arguments, if numbers should be an
	 *            exception.
	 * @return True if the fact is still valid (not anonymous)
	 */
	public boolean replaceArguments(Map<String, String> replacementMap,
			boolean retainOtherArgs, boolean retainNumbers) {
		String[] newArguments = Arrays.copyOf(arguments_, arguments_.length);
		boolean notAnonymous = false;
		for (int i = 0; i < arguments_.length; i++) {
			boolean hasReplacement = false;
			for (String key : replacementMap.keySet()) {
				if (arguments_[i].equals(key)) {
					newArguments[i] = replacementMap.get(key);
					hasReplacement = true;
				}
			}

			if (!retainOtherArgs && !hasReplacement) {
				if (!retainNumbers || !StateSpec.isNumber(arguments_[i]))
					newArguments[i] = "?";
			}

			if (!newArguments[i].equals("?"))
				notAnonymous = true;
		}
		arguments_ = newArguments;
		return notAnonymous;
	}

	/**
	 * Replaces all variable indexed terms (?X,?Y,?Z...) with the given indexed
	 * terms, be they variable or otherwise.
	 * 
	 * So if the fact is blah(?X ?Z ?Y) and the replacements given are [a, b, c]
	 * then the result is blah(a, c, b)
	 * 
	 * @param replacements
	 *            The terms to replace any variable terms with.
	 */
	public void replaceArguments(String[] replacements) {
		// Run through the arguments, replacing variable args with the
		// replacements.
		for (int i = 0; i < arguments_.length; i++) {
			int termIndex = RuleCreation.getVariableTermIndex(arguments_[i]);
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
	 * @param retainOtherArgs
	 *            If retaining the other arguments.
	 * @return True if the fact is still valid (not anonymous)
	 */
	public boolean replaceArguments(String replacedTerm,
			String replacementTerm, boolean retainOtherArgs) {
		boolean notAnonymous = false;
		for (int i = 0; i < arguments_.length; i++) {
			if (arguments_[i].equals(replacedTerm)) {
				arguments_[i] = replacementTerm;
				notAnonymous = true;
			} else if (!retainOtherArgs)
				arguments_[i] = "?";
		}
		return notAnonymous;
	}

	/**
	 * Only retains the arguments in the array and modifies all other arguments
	 * to anonymous terms.
	 * 
	 * @param retainedArgs
	 *            The arguments to retain.
	 */
	public void retainArguments(Collection<String> retainedArgs) {
		for (int i = 0; i < arguments_.length; i++) {
			if (!retainedArgs.contains(arguments_[i]))
				arguments_[i] = "?";
		}
	}

	/**
	 * Creates all possible non-anonymous sub-facts of this fact by replacing
	 * arguments with anonymous terms (and optionally creating variable args).
	 * 
	 * @param generateArgs
	 *            If variable arguments should be generated to fill
	 *            non-anonymous args, or if the fact should just use existing
	 *            args.
	 * @param includeFullyAnonymous
	 *            If the fully anonymous sub-fact should be created.
	 * @return A collection of all sub-facts.
	 */
	public Collection<RelationalPredicate> createSubFacts(boolean generateArgs,
			boolean includeFullyAnonymous) {
		Collection<RelationalPredicate> generalities = new TreeSet<RelationalPredicate>();

		int permutations = (int) Math.pow(2, arguments_.length);
		if (!includeFullyAnonymous)
			permutations--;
		// Create all generalisations of this fact
		for (int p = 1; p < permutations; p++) {
			String[] genArguments = new String[arguments_.length];
			// Run through each index location, using bitwise ops
			for (int i = 0; i < genArguments.length; i++) {
				// If the argument is not 0, enter a variable.
				if ((p & (int) Math.pow(2, i)) != 0) {
					if (generateArgs)
						genArguments[i] = RuleCreation.getVariableTermString(i);
					else
						genArguments[i] = arguments_[i];
				} else
					genArguments[i] = StateSpec.ANONYMOUS;
			}

			generalities.add(new RelationalPredicate(this, genArguments));
		}

		return generalities;
	}

	/**
	 * Creates a variable term replacement map using this StringFacts
	 * non-numerical arguments as the terms being replaced.
	 * 
	 * @param formNumericalReplacement
	 *            If the numerical values should have variable replacements or
	 *            not. If not, they remain as is.
	 * @param ignoreAnonymous
	 *            If anonymous terms should be used for the replacement map.
	 * @return A replacement map which converts this string fact's non-numerical
	 *         terms into ordered variable terms.
	 */
	public Map<String, String> createVariableTermReplacementMap(
			boolean formNumericalReplacement, boolean ignoreAnonymous) {
		Map<String, String> replacementMap = new HashMap<String, String>();
		for (int i = 0; i < arguments_.length; i++) {
			if (!StateSpec.isNumberType(factTypes_[i])
					|| formNumericalReplacement) {
				if (!arguments_[i].equals("?") || !ignoreAnonymous)
					replacementMap.put(arguments_[i],
							RuleCreation.getVariableTermString(i));
			} else
				replacementMap.put(arguments_[i], arguments_[i]);
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
	public String[] getArgTypes() {
		return factTypes_;
	}

	/**
	 * Gets the arguments for the fact.
	 * 
	 * @return The fact arguments.
	 */
	public String[] getArguments() {
		String[] argCopy = new String[arguments_.length];
		System.arraycopy(arguments_, 0, argCopy, 0, arguments_.length);
		return argCopy;
	}

	public boolean isNegated() {
		return negated_;
	}

	public void swapNegated() {
		negated_ = !negated_;
	}

	/**
	 * Checks if this fact is fully anonymous.
	 * 
	 * @return True if all arguments for the fact are anonymous arguments.
	 */
	public boolean isFullyAnonymous() {
		for (String argument : arguments_)
			if (!argument.equals(StateSpec.ANONYMOUS))
				return false;
		return true;
	}

	/**
	 * Checks if this fact is fully NOT anonymous (has no anonymous terms).
	 * 
	 * @return True if the fact contains no anonymous terms.
	 */
	public boolean isFullyNotAnonymous() {
		for (String argument : arguments_)
			if (argument.equals(StateSpec.ANONYMOUS))
				return false;
		return true;
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

	/**
	 * Formats the string as a nice string (includes goal args).
	 * 
	 * @return The StringFact as a nice string (including goal args).
	 */
	public String toNiceString(Map<String, String> replacements) {
		StringBuffer buffer = new StringBuffer();
		if (negated_)
			buffer.append("(not ");
		buffer.append("(" + factName_);
		for (int i = 0; i < arguments_.length; i++) {
			String arg = arguments_[i];
			if (replacements != null && replacements.containsKey(arg))
				arg = replacements.get(arg);

			buffer.append(" " + arg);
		}
		buffer.append(")");
		if (negated_)
			buffer.append(")");
		return buffer.toString();
	}

	@Override
	public int compareTo(RelationalPredicate sf) {
		if (negated_ != sf.negated_) {
			if (!negated_)
				return -1;
			else
				return 1;
		}

		// Compare by number of arguments (complexity)
		int result = Double.compare(arguments_.length, sf.arguments_.length);
		if (result != 0)
			return result;

		// Type predicates trump regular predicates
		if (StateSpec.getInstance().isTypePredicate(factName_)) {
			if (!StateSpec.getInstance().isTypePredicate(sf.factName_))
				return -1;
		} else if (StateSpec.getInstance().isTypePredicate(sf.factName_))
			return 1;

		result = factName_.compareTo(sf.factName_);
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
		RelationalPredicate other = (RelationalPredicate) obj;
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
