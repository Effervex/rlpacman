package relationalFramework;

import java.io.Serializable;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import relationalFramework.agentObservations.LocalAgentObservations;
import relationalFramework.agentObservations.RangeContext;

/**
 * Relational arguments are what go within relational predicates. In most cases,
 * these are just simple strings, but when dealing with numbers, they can
 * express a range.
 * 
 * @author Sam Sarjant
 */
public class RelationalArgument implements Comparable<RelationalArgument>,
		Serializable {
	/** The old pattern for finding ranges. */
	private static Pattern DEPRECATED_RANGE_PATTERN = Pattern
			.compile("(.+)&:\\(<= (.+?) \\1 (.+?)\\)");
	/** The human-readable format for an explicit range. */
	private static final Pattern HUMAN_RANGE_PATTERN = Pattern
			.compile("\\((.+?) <= (.+?) <= (.+?)\\)");
	/** The first character for variables. */
	private static final char FIRST_CHAR = 'A';
	/** The minimum possible number of chars for a range definition. */
	private static final int MIN_RANGE = 5;
	/** The final character for variables. */
	private static final char MODULO_LETTERS = 26;
	private static Pattern RANGE_PATTERN = Pattern.compile("(.+)&:\\("
			+ StateSpec.RANGE_TEST
			+ " (.+) ([\\d.E-]+) \\1 (.+) ([\\d.E-]+)\\)");
	private static final long serialVersionUID = -7883241266827157231L;

	/** The starting character for variables. */
	private static final char STARTING_CHAR = 'A';
	/** Variable extension for non-action variables.. */
	private static final String VARIABLE_PREFIX = "?V_";
	/** Anonymous term for situations where variables don't matter. */
	public static final RelationalArgument ANONYMOUS = new RelationalArgument(
			"?");
	/** The goal variable prefix. */
	public static final String GOAL_VARIABLE_PREFIX = "?G_";
	/** The prefix for range variables. */
	public static final String RANGE_VARIABLE_PREFIX = "?#_";
	/** Defines the arg type. */
	private ArgumentType argType_;
	/**
	 * If this arg is a free (lonesome) variable. Only true if non-action
	 * variable.
	 */
	private boolean freeVariable_;

	private final RangeBound[] rangeBounds_ = new RangeBound[2];

	/** The context of the range if this is a range term. */
	private final RangeContext rangeContext_;

	/**
	 * The range, if any. If both values are the same, then the range is a
	 * single number.
	 */
	private final double[] rangeFrac_ = new double[2];

	/** The argument represented by this arg. */
	private String stringArg_;

	/**
	 * The constructor. This deconstructs ranges into their components.
	 * 
	 * @param arg
	 *            The string argument to parse. Could be anything.
	 */
	public RelationalArgument(String arg) {
		rangeContext_ = null;

		// Check for a numerical range
		if (arg.length() >= MIN_RANGE) {
			Matcher m = RANGE_PATTERN.matcher(arg);

			// Ranges are not free if they have bounds
			if (m.find()) {
				stringArg_ = m.group(1);
				rangeBounds_[0] = new RangeBound(m.group(2));
				rangeFrac_[0] = Double.parseDouble(m.group(3));
				rangeBounds_[1] = new RangeBound(m.group(4));
				rangeFrac_[1] = Double.parseDouble(m.group(5));
				argType_ = ArgumentType.NUMBER_RANGE;
				freeVariable_ = false;
				return;
			}

			// The slightly older range pattern
			m = DEPRECATED_RANGE_PATTERN.matcher(arg);

			if (m.find()) {
				stringArg_ = m.group(1);
				rangeBounds_[0] = new RangeBound(m.group(2));
				rangeFrac_[0] = 0;
				rangeBounds_[1] = new RangeBound(m.group(3));
				rangeFrac_[1] = 1;
				argType_ = ArgumentType.NUMBER_RANGE;
				freeVariable_ = false;
				return;
			}

			m = HUMAN_RANGE_PATTERN.matcher(arg);

			if (m.find()) {
				stringArg_ = m.group(2);
				rangeBounds_[0] = new RangeBound(m.group(1));
				rangeFrac_[0] = 0;
				rangeBounds_[1] = new RangeBound(m.group(3));
				rangeFrac_[1] = 1;
				argType_ = ArgumentType.NUMBER_RANGE;
				freeVariable_ = false;
				return;
			}
		}

		stringArg_ = arg;

		// The arg could still be a number
		try {
			double num = Double.parseDouble(arg);
			rangeBounds_[0] = new RangeBound(num);
			rangeBounds_[1] = new RangeBound(num);
			argType_ = ArgumentType.NUMBER_CONST;
			return;
		} catch (Exception e) {
		}

		// Determine arg type
		if (stringArg_.charAt(0) == '?') {
			if (stringArg_.length() == 1) {
				argType_ = ArgumentType.ANON;
				freeVariable_ = true;
			} else if (stringArg_.startsWith(GOAL_VARIABLE_PREFIX)) {
				argType_ = ArgumentType.GOAL_VARIABLE;
				freeVariable_ = false;
			} else {
				if (stringArg_.startsWith("?Unb_")
						|| stringArg_.startsWith("?V_")) {
					argType_ = ArgumentType.NON_ACTION;
					freeVariable_ = true;
				} else if (stringArg_.startsWith("?Bnd_")) {
					argType_ = ArgumentType.NON_ACTION;
					freeVariable_ = false;
				} else if (stringArg_.startsWith(RANGE_VARIABLE_PREFIX)) {
					argType_ = ArgumentType.NUMBER_RANGE;
					freeVariable_ = true; // Can change
				} else {
					argType_ = ArgumentType.ACTION_VAR;
					freeVariable_ = false;
				}
			}
		} else {
			argType_ = ArgumentType.CONST;
		}
	}

	/**
	 * A constructor for a new range which uses explicit bounds.
	 * 
	 * @param variable
	 *            The variable of the range.
	 * @param minBound
	 *            The minimum bound.
	 * @param maxBound
	 *            The maximum bound.
	 * @param context
	 *            The given context of this range.
	 */
	public RelationalArgument(String variable, double minBound, double maxBound) {
		rangeContext_ = null;
		stringArg_ = variable;
		rangeBounds_[0] = new RangeBound(minBound);
		rangeBounds_[1] = new RangeBound(maxBound);
		rangeFrac_[0] = 0;
		rangeFrac_[1] = 1;
		argType_ = ArgumentType.NUMBER_RANGE;
	}

	/**
	 * A constructor for a new range with given bounds and fracs between bounds.
	 * 
	 * @param variable
	 *            The variable of the range.
	 * @param minBound
	 *            The minimum bound.
	 * @param minFrac
	 *            The minimum value of the range.
	 * @param maxBound
	 *            The maximum bound.
	 * @param maxFrac
	 *            The maximum value of the range.
	 * @param context
	 *            The context of this range.
	 */
	public RelationalArgument(String variable, RangeBound minBound,
			double minFrac, RangeBound maxBound, double maxFrac,
			RangeContext context) {
		rangeContext_ = context;
		stringArg_ = variable;
		rangeBounds_[0] = minBound;
		rangeBounds_[1] = maxBound;
		rangeFrac_[0] = Math.max(0, minFrac);
		rangeFrac_[1] = Math.min(1, maxFrac);
		argType_ = ArgumentType.NUMBER_RANGE;
	}

	public RelationalArgument(RelationalArgument otherArg, RangeContext context) {
		this(otherArg.stringArg_, otherArg.rangeBounds_[0],
				otherArg.rangeFrac_[0], otherArg.rangeBounds_[1],
				otherArg.rangeFrac_[1], context);
	}

	/**
	 * Creates a perfect duplicate of this arg.
	 * 
	 * @return The clone of this arg.
	 */
	@Override
	public RelationalArgument clone() {
		RelationalArgument relArg = new RelationalArgument(stringArg_,
				rangeBounds_[0], rangeFrac_[0], rangeBounds_[1], rangeFrac_[1],
				rangeContext_);
		relArg.argType_ = argType_;
		relArg.freeVariable_ = freeVariable_;
		return relArg;
	}

	@Override
	public int compareTo(RelationalArgument ra) {
		// Arg type
		int result = argType_.compareTo(ra.argType_);
		if (result != 0)
			return result;

		// Ranges
		if (rangeBounds_[0] == null && ra.rangeBounds_[0] != null)
			return -1;
		else if (rangeBounds_[0] != null && ra.rangeBounds_[0] == null)
			return 1;
		else if (rangeBounds_[0] != null && ra.rangeBounds_[0] != null) {
			result = rangeBounds_[0].compareTo(ra.rangeBounds_[0]);
			if (result != 0)
				return result;
			result = rangeBounds_[1].compareTo(ra.rangeBounds_[1]);
			if (result != 0)
				return result;
			result = Double.compare(rangeFrac_[0], ra.rangeFrac_[0]);
			if (result != 0)
				return result;
			result = Double.compare(rangeFrac_[1], ra.rangeFrac_[1]);
			if (result != 0)
				return result;
		}
		
		// If at least one is free
		if (freeVariable_ || ra.freeVariable_) {
			// If both are free, they are equal
			if (freeVariable_ && ra.freeVariable_)
				return 0;
			else if (freeVariable_)
				return 1;
			else
				return -1;
		}

		return stringArg_.compareTo(ra.stringArg_);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RelationalArgument other = (RelationalArgument) obj;
		if (argType_ != other.argType_)
			return false;
		if (freeVariable_ != other.freeVariable_)
			return false;
		if (!Arrays.equals(rangeBounds_, other.rangeBounds_))
			return false;
		if (!Arrays.equals(rangeFrac_, other.rangeFrac_))
			return false;
		if (!freeVariable_) {
			if (stringArg_ == null) {
				if (other.stringArg_ != null)
					return false;
			} else if (!stringArg_.equals(other.stringArg_))
				return false;
		}
		return true;
	}



	public ArgumentType getArgumentType() {
		return argType_;
	}

	public double[] getExplicitRange() {
		double[] explicitRange = { rangeBounds_[0].getValue(null),
				rangeBounds_[1].getValue(null) };
		return explicitRange;
	}

	public RangeBound[] getRangeBounds() {
		if (isNumber())
			return rangeBounds_;
		return null;
	}

	public RangeContext getRangeContext() {
		return rangeContext_;
	}

	public double[] getRangeFrac() {
		if (isNumber())
			return rangeFrac_;
		return null;
	}

	public String getStringArg() {
		return stringArg_;
	}

	/**
	 * This method gets the int this arg's variable points to with regards to
	 * the action, or -1 if invalid.
	 * 
	 * @return An integer corresponding to the position in the action the
	 *         variable refers to or -1 if invalid.
	 */
	public int getVariableTermIndex() {
		// Don't swap constants or anonymous
		if (argType_ != ArgumentType.ACTION_VAR)
			return -1;

		int termIndex = (stringArg_.charAt(1) + MODULO_LETTERS - STARTING_CHAR)
				% MODULO_LETTERS;
		return termIndex;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((argType_ == null) ? 0 : argType_.hashCode());
		result = prime * result + Arrays.hashCode(rangeBounds_);
		result = prime * result + Arrays.hashCode(rangeFrac_);
		if (freeVariable_) {
			result = prime * result + 1231;
		} else {
			result = prime * result
					+ ((stringArg_ == null) ? 0 : stringArg_.hashCode());
		}
		return result;
	}

	public boolean isAnonymous() {
		return argType_ == ArgumentType.ANON;
	}

	/**
	 * If this argument is a bound free variable.
	 * 
	 * @return True if this argument is a bound free variable.
	 */
	public boolean isBoundVariable() {
		return argType_ == ArgumentType.NON_ACTION && !freeVariable_;
	}

	public boolean isConstant() {
		if (argType_ == ArgumentType.CONST
				|| argType_ == ArgumentType.GOAL_VARIABLE)
			return true;
		return false;
	}

	/**
	 * If this argument is a free variable.
	 * 
	 * @return True if this argument is a free variable.
	 */
	public boolean isFreeVariable() {
		return freeVariable_;
	}

	public boolean isGoalVariable() {
		return argType_ == ArgumentType.GOAL_VARIABLE;
	}

	public boolean isNumber() {
		return argType_ == ArgumentType.NUMBER_RANGE
				|| argType_ == ArgumentType.NUMBER_CONST;
	}

	public boolean isRange(boolean isDefinedRange) {
		return argType_ == ArgumentType.NUMBER_RANGE
				&& (!isDefinedRange || rangeBounds_[0] != null);
	}

	public boolean isRangeVariable() {
		return argType_ == ArgumentType.NUMBER_RANGE;
	}

	/**
	 * If this argument is an unbound variable.
	 * 
	 * @return True if this argument is an unbound variable.
	 */
	public boolean isUnboundVariable() {
		if (argType_ == ArgumentType.NON_ACTION && freeVariable_
				|| argType_ == ArgumentType.ANON)
			return true;
		return false;
	}

	public boolean isNonActionVar() {
		return argType_ == ArgumentType.NON_ACTION;
	}

	public boolean isVariable() {
		return argType_ == ArgumentType.ACTION_VAR
				|| argType_ == ArgumentType.NUMBER_RANGE
				|| argType_ == ArgumentType.NON_ACTION;
	}

	/**
	 * Sets this variable as a free variable (or not).
	 */
	public void setFreeVariable(boolean isFree) {
		if (!isVariable())
			throw new IllegalAccessError(this
					+ " is not a variable. Cannot be set as free!");
		freeVariable_ = isFree;
	}

	/**
	 * Formats the argument as a nice, human-readable string (by modifying how
	 * the range looks).
	 * 
	 * @return The argument, possibly as a nice range.
	 */
	public String toNiceString() {
		if (isNumber() && (rangeFrac_[0] != rangeFrac_[1])) {
			if (rangeContext_ != null) {
				try {
					double[] minMax = LocalAgentObservations.getActionRanges(
							rangeContext_, null);
					double minBound = (rangeBounds_[0].getValue(minMax));
					double maxBound = (rangeBounds_[1].getValue(minMax));
					double diff = maxBound - minBound;
					return "(" + (minBound + diff * rangeFrac_[0]) + " <= "
							+ stringArg_ + " <= "
							+ (minBound + diff * rangeFrac_[1]) + ")";
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return toString();
		}
		return stringArg_;
	}

	@Override
	public String toString() {
		if (isNumber() && (rangeFrac_[0] != rangeFrac_[1])) {
			// Dealing with a X-Y range -- no fractions
			if (rangeFrac_[0] == 0 && rangeFrac_[1] == 1) {
				return stringArg_ + "&:(<= " + rangeBounds_[0] + " "
						+ stringArg_ + " " + rangeBounds_[1] + ")";
			}
			return stringArg_ + "&:(" + StateSpec.RANGE_TEST + " "
					+ rangeBounds_[0] + " " + rangeFrac_[0] + " " + stringArg_
					+ " " + rangeBounds_[1] + " " + rangeFrac_[1] + ")";
		}

		return stringArg_;
	}

	/**
	 * Creates an unbound variable (equivalent to an anon variable).
	 * 
	 * @param i
	 *            The number of existing unbound variables already.
	 * @param unboundStart
	 *            A provided index for the unbound variables.
	 * @return An unbound variable (locally to this predicate, anyway).
	 */
	public static RelationalArgument createBoundVariable(int i) {
		RelationalArgument bound = new RelationalArgument(VARIABLE_PREFIX + i);
		bound.setFreeVariable(false);
		return bound;
	}

	/**
	 * Creates a variable representing a goal term at a given index.
	 * 
	 * @param i
	 *            The index of the goal condition.
	 * @return The variable goal condition.
	 */
	public static RelationalArgument createGoalTerm(int i) {
		return new RelationalArgument(GOAL_VARIABLE_PREFIX + i);
	}

	/**
	 * Creates an unbound variable (equivalent to an anon variable).
	 * 
	 * @param i
	 *            The number of existing unbound variables already.
	 * @param unboundStart
	 *            A provided index for the unbound variables.
	 * @return An unbound variable (locally to this predicate, anyway).
	 */
	public static RelationalArgument createUnboundVariable(int i) {
		RelationalArgument unbound = new RelationalArgument(VARIABLE_PREFIX + i);
		unbound.setFreeVariable(true);
		return unbound;
	}

	/**
	 * Generates a variable term string from the index given.
	 * 
	 * @param i
	 *            The index of the string.
	 * @return A string in variable format, with the name of the variable in the
	 *         middle.
	 */
	public static RelationalArgument createVariableTermArg(int i) {
		char variable = (char) (FIRST_CHAR + i);
		return new RelationalArgument("?" + variable);
	}

	public static boolean isGoalCondition(String arg) {
		return arg.startsWith(GOAL_VARIABLE_PREFIX);
	}

	public static boolean isRangeVariable(String arg) {
		return arg.startsWith(RANGE_VARIABLE_PREFIX);
	}
}
