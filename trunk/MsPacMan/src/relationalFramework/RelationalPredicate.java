package relationalFramework;

import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.LocalAgentObservations;
import relationalFramework.agentObservations.RangeContext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A class containing the definitions/instantiations of a relational predicate,
 * which is usually converted to a String.
 * 
 * @author Sam Sarjant
 */
public class RelationalPredicate implements Comparable<RelationalPredicate>,
		Serializable {
	private static final long serialVersionUID = 6131063892766663639L;
	/** A collection of contexts which define ranges. */
	private SortedSet<RangeContext> rangeContexts_;
	/** The actual arguments of the fact. */
	protected RelationalArgument[] arguments_;
	/** The fact name. */
	protected String factName_;
	/** The types of the fact arguments. */
	protected String[] factTypes_;
	/** If this fact is negated (prefixed by not) */
	protected boolean negated_ = false;

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
		arguments_ = cloneArgs(stringFact.arguments_);
	}

	/**
	 * A filled fact constructor.
	 * 
	 * @param fact
	 *            The fact definition to use.
	 * @param arguments
	 *            The arguments for the fact.
	 */
	public RelationalPredicate(RelationalPredicate fact,
			RelationalArgument[] arguments) {
		factName_ = fact.factName_;
		factTypes_ = fact.factTypes_;
		arguments_ = cloneArgs(arguments);
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
	public RelationalPredicate(RelationalPredicate fact,
			RelationalArgument[] arguments, boolean negated) {
		this(fact, arguments);
		negated_ = negated;
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
		arguments_ = cloneArgs(arguments);
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
		this(fact, arguments);
		negated_ = negated;
	}

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
		arguments_ = new RelationalArgument[factTypes.length];
		Arrays.fill(arguments_, RelationalArgument.ANONYMOUS);
	}

	private RelationalArgument[] cloneArgs(RelationalArgument[] arguments) {
		rangeContexts_ = new TreeSet<RangeContext>();
		RelationalArgument[] newArgs = new RelationalArgument[arguments.length];
		for (int i = 0; i < arguments.length; i++) {
			newArgs[i] = arguments[i].clone();
			RangeContext rc = newArgs[i].getRangeContext();
			if (rc != null)
				rangeContexts_.add(rc);
		}
		return newArgs;
	}

	private RelationalArgument[] cloneArgs(String[] arguments) {
		rangeContexts_ = new TreeSet<RangeContext>();
		RelationalArgument[] newArgs = new RelationalArgument[arguments.length];
		for (int i = 0; i < arguments.length; i++) {
			newArgs[i] = new RelationalArgument(arguments[i]);
			RangeContext rc = newArgs[i].getRangeContext();
			if (rc != null)
				rangeContexts_.add(rc);
		}
		return newArgs;
	}

	/**
	 * Internal replacement method.
	 * 
	 * @param <T>
	 * 
	 * @param replacementMap
	 *            The replacement map for the arguments. Can be of type String
	 *            or RelationalArgument.
	 * @param retainOtherArgs
	 *            If arguments that have no replacement should be retained (or
	 *            turned anonymous).
	 * @param retainNumbers
	 *            If not retaining other arguments, if numbers should be an
	 *            exception.
	 * @param flexibleReplace
	 *            If the replacement map should be appended to.
	 * @return True if the fact is still valid (not anonymous)
	 */
	@SuppressWarnings("unchecked")
	private <T> boolean replaceArguments(Map<T, T> replacementMap,
			boolean retainOtherArgs, boolean retainNumbers,
			boolean flexibleReplace, int unboundStart) {
		RelationalArgument[] newArguments = new RelationalArgument[arguments_.length];
		boolean notAnonymous = false;
		for (int i = 0; i < arguments_.length; i++) {
			newArguments[i] = arguments_[i].clone();
			boolean hasReplacement = false;

			// Determine what type of map is being used for replacements
			RelationalArgument replacement = (RelationalArgument) replacementMap
					.get(arguments_[i]);
			if (replacement == null) {
				String strReplacement = (String) replacementMap
						.get(arguments_[i].toString());
				if (strReplacement != null)
					replacement = new RelationalArgument(strReplacement);
			}

			// Apply the replacement (if not null)
			if (replacement != null) {
				newArguments[i] = replacement;
				hasReplacement = true;
			}

			// Retaining args
			if (!retainOtherArgs && !hasReplacement) {
				if (!retainNumbers || !arguments_[i].isNumber()) {
					if (flexibleReplace) {
						newArguments[i] = RelationalArgument
								.createUnboundVariable(unboundStart);
						replacementMap.put((T) arguments_[i],
								(T) newArguments[i]);
					} else
						newArguments[i] = RelationalArgument.ANONYMOUS;
				}
			}

			// Anonymous checks.
			if (!newArguments[i].isAnonymous())
				notAnonymous = true;
		}
		arguments_ = newArguments;
		return notAnonymous;
	}

	/**
	 * Removes any numerical ranges from this rule.
	 */
	public void clearRanges() {
		for (int i = 0; i < arguments_.length; i++) {
			if (arguments_[i].isRange(false)) {
				arguments_[i] = new RelationalArgument(
						arguments_[i].getStringArg());
			}
		}
	}

	@Override
	public int compareTo(RelationalPredicate arg1) {
		return compareTo(arg1, true);
	}

	/**
	 * Compares this predicate against another predicate.
	 * 
	 * @param arg1
	 *            The predicate to compare to.
	 * @param ascending
	 *            If # arguments is ascending.
	 * @return -1 if this predicate is before the other, 1 if after, 0 if equal.
	 */
	public int compareTo(RelationalPredicate arg1, boolean ascending) {
		// Compare by negation
		if (negated_ != arg1.negated_) {
			if (!negated_)
				return -1;
			else
				return 1;
		}

		// Type predicates trump non-type predicates
		if (StateSpec.getInstance().isTypePredicate(factName_)) {
			if (!StateSpec.getInstance().isTypePredicate(arg1.factName_))
				return -1;
		} else if (StateSpec.getInstance().isTypePredicate(arg1.factName_))
			return 1;

		// Compare by number of arguments (complexity)
		int result = Double.compare(arguments_.length, arg1.arguments_.length);
		if (result != 0)
			return (ascending) ? result : -result;

		// Check arguments
		for (int i = 0; i < arguments_.length; i++) {
			result = arguments_[i].compareTo(arg1.arguments_[i]);
			if (result != 0)
				return result;
		}

		result = factName_.compareTo(arg1.factName_);
		if (result != 0)
			return result;
		return 0;
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
	public Set<RelationalPredicate> createSubFacts(boolean generateArgs,
			boolean includeFullyAnonymous) {
		Set<RelationalPredicate> generalities = new TreeSet<RelationalPredicate>();

		int permutations = (int) Math.pow(2, arguments_.length);
		if (!includeFullyAnonymous)
			permutations--;
		// Create all generalisations of this fact
		for (int p = 1; p < permutations; p++) {
			RelationalArgument[] genArguments = new RelationalArgument[arguments_.length];
			// Run through each index location, using bitwise ops
			for (int i = 0; i < genArguments.length; i++) {
				// If the argument is not 0, enter a variable.
				if ((p & (int) Math.pow(2, i)) != 0) {
					if (generateArgs)
						genArguments[i] = RelationalArgument
								.createVariableTermArg(i);
					else
						genArguments[i] = arguments_[i];
				} else
					genArguments[i] = RelationalArgument.ANONYMOUS;
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
	public Map<RelationalArgument, RelationalArgument> createVariableTermReplacementMap(
			boolean formNumericalReplacement, boolean ignoreAnonymous) {
		Map<RelationalArgument, RelationalArgument> replacementMap = new HashMap<RelationalArgument, RelationalArgument>();
		for (int i = 0; i < arguments_.length; i++) {
			if (!StateSpec.isNumberType(factTypes_[i])
					|| formNumericalReplacement) {
				if (!arguments_[i].equals(RelationalArgument.ANONYMOUS)
						|| !ignoreAnonymous) {
					if (!replacementMap.containsKey(arguments_[i]))
						replacementMap.put(arguments_[i],
								RelationalArgument.createVariableTermArg(i));
				}
			} else if (!replacementMap.containsKey(arguments_[i]))
				replacementMap.put(arguments_[i], arguments_[i]);
		}
		return replacementMap;
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

	/**
	 * Applies a replacement map to this predicate, while flexibly creating
	 * unbound terms for non replacement terms.
	 * 
	 * @param replacementMap
	 *            The replacement map to apply, and modify.
	 * @param unboundStart
	 *            The start index for the unbound variables.
	 */
	public void flexibleReplaceArguments(
			Map<RelationalArgument, RelationalArgument> replacementMap,
			int unboundStart) {
		replaceArguments(replacementMap, false, true, true, unboundStart);
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
		for (int i = 0; i < arguments_.length; i++)
			argCopy[i] = arguments_[i].toString();
		return argCopy;
	}

	/**
	 * Gets the name of the fact.
	 * 
	 * @return The name of the fact.
	 */
	public String getFactName() {
		return factName_;
	}

	public SortedSet<RangeContext> getRangeContexts() {
		return rangeContexts_;
	}

	public RelationalArgument[] getRelationalArguments() {
		return cloneArgs(arguments_);
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

	/**
	 * Checks if this fact is fully anonymous.
	 * 
	 * @return True if all arguments for the fact are anonymous arguments.
	 */
	public boolean isFullyAnonymous() {
		for (RelationalArgument argument : arguments_)
			if (!argument.equals(RelationalArgument.ANONYMOUS))
				return false;
		return true;
	}

	/**
	 * Checks if this fact is fully NOT anonymous (has no anonymous terms).
	 * 
	 * @return True if the fact contains no anonymous terms.
	 */
	public boolean isFullyNotAnonymous() {
		for (RelationalArgument argument : arguments_)
			if (argument.equals(RelationalArgument.ANONYMOUS))
				return false;
		return true;
	}

	public boolean isNegated() {
		return negated_;
	}

	/**
	 * If the predicate contains numerical args.
	 * 
	 * @return True if the predicate contains numerical args
	 */
	public boolean isNumerical() {
		return StateSpec.isNumerical(this.factName_);
	}

	/**
	 * Replaces all occurrences of an argument with another value.
	 * 
	 * @param replacementMap
	 *            The replacement map for the arguments. Can be of type String
	 *            or RelationalArgument.
	 * @param retainOtherArgs
	 *            If arguments that have no replacement should be retained (or
	 *            turned anonymous).
	 * @param retainNumbers
	 *            If not retaining other arguments, if numbers should be an
	 *            exception.
	 * @return True if the fact is still valid (not anonymous)
	 */
	public <T> boolean replaceArguments(Map<T, T> replacementMap,
			boolean retainOtherArgs, boolean retainNumbers) {
		return replaceArguments(replacementMap, retainOtherArgs, retainNumbers,
				false, 0);
	}

	public boolean replaceArguments(RelationalArgument replacedTerm,
			RelationalArgument replacementTerm, boolean retainOtherArgs) {
		boolean notAnonymous = false;
		for (int i = 0; i < arguments_.length; i++) {
			if (arguments_[i].equals(replacedTerm)) {
				arguments_[i] = replacementTerm.clone();
				notAnonymous = true;
			} else if (!retainOtherArgs)
				arguments_[i] = RelationalArgument.ANONYMOUS;
		}
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
			// Only replace action variables!
			if (!arguments_[i].isFreeVariable()) {
				int termIndex = arguments_[i].getVariableTermIndex();
				if (termIndex != -1)
					arguments_[i] = new RelationalArgument(
							replacements[termIndex]);
			}
		}
	}

	/**
	 * Replaces all unbound variables with anonymous variables.
	 */
	public void replaceUnboundWithAnonymous() {
		for (int i = 0; i < arguments_.length; i++) {
			if (arguments_[i].isUnboundVariable())
				arguments_[i] = RelationalArgument.ANONYMOUS;
		}
	}

	/**
	 * Only retains the arguments in the array and modifies all other arguments
	 * to anonymous terms.
	 * 
	 * @param retainedArgs
	 *            The arguments to retain.
	 */
	public void retainArguments(Collection<RelationalArgument> retainedArgs) {
		for (int i = 0; i < arguments_.length; i++) {
			if (!retainedArgs.contains(arguments_[i]))
				arguments_[i] = RelationalArgument.ANONYMOUS;
		}
	}

	/**
	 * Replaces the args of a predicate with the replacement args, but replaces
	 * any existing args with other variables so as not to compromise the
	 * structure of the fact.
	 * 
	 * @param replacementMap
	 */
	public void safeReplaceArgs(
			Map<RelationalArgument, RelationalArgument> replacementMap) {
		Map<RelationalArgument, RelationalArgument> tempRepl = new HashMap<RelationalArgument, RelationalArgument>(
				arguments_.length);
		int tempCounter = 0;
		// Run through each argument, swapping existing args with unbound temp
		// variables.
		for (int i = 0; i < arguments_.length; i++) {
			// Increment tempCounter to an unbound variable
			while (replacementMap.containsValue(RelationalArgument
					.createVariableTermArg(tempCounter)))
				tempCounter++;

			RelationalArgument arg = arguments_[i];
			if (replacementMap.containsKey(arg))
				arguments_[i] = replacementMap.get(arg).clone();
			else if (tempRepl.containsKey(arg))
				arguments_[i] = tempRepl.get(arg).clone();
			else if (!arg.isAnonymous()) {
				tempRepl.put(arg,
						RelationalArgument.createVariableTermArg(tempCounter++));
				arguments_[i] = tempRepl.get(arg).clone();
			}
		}
	}

	/**
	 * Sets the arguments of this predicate. This method should be used
	 * carefully, as the hashcode may change.
	 * 
	 * @param arguments
	 *            The new arguments for the
	 */
	public void setArguments(RelationalArgument[] actionArgs) {
		arguments_ = cloneArgs(actionArgs);
	}

	public void swapNegated() {
		negated_ = !negated_;
	}

	/**
	 * Formats the predicate as a nice string (includes goal args).
	 * 
	 * @return The StringFact as a nice string (including goal args).
	 */
	public String toNiceString(
			Map<RelationalArgument, RelationalArgument> replacements) {
		StringBuffer buffer = new StringBuffer();
		if (negated_)
			buffer.append("(not ");
		buffer.append("(" + factName_);
		for (int i = 0; i < arguments_.length; i++) {
			RelationalArgument arg = arguments_[i];
			if (replacements != null && replacements.containsKey(arg))
				arg = replacements.get(arg);

			buffer.append(" " + arg.toNiceString());
		}
		buffer.append(")");
		if (negated_)
			buffer.append(")");
		return buffer.toString();
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
	 * Creates the range contexts for Relational Arguments using the given
	 * action to identify the context.
	 * 
	 * @param action
	 *            The action this predicate is a condition for.
	 */
	public void configureRangeContexts(RelationalPredicate action) {
		for (int i = 0; i < arguments_.length; i++) {
			if (arguments_[i].isRangeVariable() && arguments_[i].getRangeBounds()[0] != null) {
				RangeContext context = new RangeContext(i, this, action);
				arguments_[i] = new RelationalArgument(arguments_[i], context);
				rangeContexts_.add(context);
			}
		}
	}
}
