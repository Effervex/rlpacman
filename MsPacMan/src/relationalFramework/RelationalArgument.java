package relationalFramework;

import java.io.Serializable;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import relationalFramework.agentObservations.AgentObservations;
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
	/** The first character for variables. */
	private static final char FIRST_CHAR = 'A';
	/** The minimum possible number of chars for a range definition. */
	private static final int MIN_RANGE = 17;
	/** The final character for variables. */
	private static final char MODULO_LETTERS = 26;
	/** The pattern for finding ranges. */
	private static Pattern DEPRECATED_RANGE_PATTERN = Pattern
			.compile("(.+)&:\\(<= ([\\dE.-]+) \\1 ([\\dE.-]+)\\)");
	private static Pattern RANGE_PATTERN = Pattern.compile("(.+)&:\\("
			+ StateSpec.RANGE_TEST
			+ " (.+) ([\\d.E-]+) \\1 (.+) ([\\d.E-]+)\\)");
	private static final long serialVersionUID = -7883241266827157231L;
	/** The starting character for variables. */
	private static final char STARTING_CHAR = 'X';
	/** The representing arg for anonymous args. */
	public static final RelationalArgument ANONYMOUS = new RelationalArgument(
			"?");

	/** The goal variable prefix. */
	public static final String GOAL_VARIABLE_PREFIX = "?G_";
	/** The prefix for range variables. */
	public static final String RANGE_VARIABLE_PREFIX = "?#_";

	/** Defines the arg type. */
	private ArgType argType_;

	/**
	 * The range, if any. If both values are the same, then the range is a
	 * single number.
	 */
	private final double[] rangeFrac_ = new double[2];

	private final RangeBound[] rangeBounds_ = new RangeBound[2];

	/** The context of the range if this is a range term. */
	private final RangeContext rangeContext_;

	/** The argument represented by this arg. */
	private String stringArg_;

	/**
	 * The constructor. This deconstructs ranges into their components.
	 * 
	 * @param arg
	 *            The string argument to parse. Could be anything.
	 */
	public RelationalArgument(String arg) {
		// Cannot determine range.
		rangeContext_ = null;

		// Check for a numerical range
		if (arg.length() >= MIN_RANGE) {
			Matcher m = RANGE_PATTERN.matcher(arg);

			if (m.find()) {
				stringArg_ = m.group(1);
				rangeBounds_[0] = new RangeBound(m.group(2));
				rangeFrac_[0] = Double.parseDouble(m.group(3));
				rangeBounds_[1] = new RangeBound(m.group(4));
				rangeFrac_[1] = Double.parseDouble(m.group(5));
				argType_ = ArgType.NUMBER_RANGE;
				return;
			} else {
				m = DEPRECATED_RANGE_PATTERN.matcher(arg);

				if (m.find()) {
					stringArg_ = m.group(1);
					rangeBounds_[0] = new RangeBound(m.group(2));
					rangeFrac_[0] = 0;
					rangeBounds_[1] = new RangeBound(m.group(3));
					rangeFrac_[1] = 1;
					argType_ = ArgType.NUMBER_RANGE;
					return;
				}
			}
		}

		stringArg_ = arg;

		// The arg could still be a number
		try {
			double num = Double.parseDouble(arg);
			rangeBounds_[0] = new RangeBound(num);
			rangeBounds_[1] = new RangeBound(num);
			argType_ = ArgType.NUMBER_CONST;
			return;
		} catch (Exception e) {
		}

		// Determine arg type
		if (stringArg_.charAt(0) == '?') {
			if (stringArg_.length() == 1)
				argType_ = ArgType.ANON;
			else if (stringArg_.startsWith(GOAL_VARIABLE_PREFIX))
				argType_ = ArgType.CONST;
			else
				argType_ = ArgType.VAR;
		} else {
			argType_ = ArgType.CONST;
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
	public RelationalArgument(String variable, double minBound,
			double maxBound) {
		rangeContext_ = null;
		stringArg_ = variable;
		rangeBounds_[0] = new RangeBound(minBound);
		rangeBounds_[1] = new RangeBound(maxBound);
		rangeFrac_[0] = 0;
		rangeFrac_[1] = 1;
		if (minBound == maxBound)
			argType_ = ArgType.NUMBER_CONST;
		else
			argType_ = ArgType.NUMBER_RANGE;
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
		if (minFrac == maxFrac && minBound == maxBound)
			argType_ = ArgType.NUMBER_CONST;
		else
			argType_ = ArgType.NUMBER_RANGE;
	}

	/**
	 * Creates a perfect duplicate of this arg.
	 * 
	 * @return The clone of this arg.
	 */
	public RelationalArgument clone() {
		RelationalArgument relArg = new RelationalArgument(stringArg_,
				rangeBounds_[0], rangeFrac_[0], rangeBounds_[1], rangeFrac_[1],
				rangeContext_);
		relArg.argType_ = argType_;
		return relArg;
	}

	@Override
	public int compareTo(RelationalArgument ra) {
		// Arg type
		int result = argType_.compareTo(ra.argType_);
		if (result != 0)
			return result;

		// Ranges
		if (rangeBounds_[0] != null && rangeBounds_[1] != null) {
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
		if (!Arrays.equals(rangeBounds_, other.rangeBounds_))
			return false;
		if (!Arrays.equals(rangeFrac_, other.rangeFrac_))
			return false;
		if (stringArg_ == null) {
			if (other.stringArg_ != null)
				return false;
		} else if (!stringArg_.equals(other.stringArg_))
			return false;
		return true;
	}

	public RangeBound[] getRangeBounds() {
		if (isNumber())
			return rangeBounds_;
		return null;
	}

	public double[] getExplicitRange() {
		double[] explicitRange = { rangeBounds_[0].getValue(null),
				rangeBounds_[1].getValue(null) };
		return explicitRange;
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
		if (argType_ != ArgType.VAR)
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
		result = prime * result
				+ ((stringArg_ == null) ? 0 : stringArg_.hashCode());
		return result;
	}

	/**
	 * Checks if this argument is a constant.
	 * 
	 * @return
	 */
	public boolean isConstant() {
		if (argType_ == ArgType.CONST)
			return true;
		return false;
	}

	public boolean isNumber() {
		return argType_ == ArgType.NUMBER_RANGE
				|| argType_ == ArgType.NUMBER_CONST;
	}

	public boolean isRange() {
		return argType_ == ArgType.NUMBER_RANGE;
	}

	public boolean isVariable() {
		return argType_ == ArgType.VAR || argType_ == ArgType.NUMBER_RANGE;
	}

	/**
	 * Formats the argument as a nice, human-readable string (by modifying how
	 * the range looks).
	 * 
	 * @return The argument, possibly as a nice range.
	 */
	public String toNiceString() {
		// TODO Modify this to take into account min and max - need information
		// on argument context to produce explicit numerical range
		if (isNumber() && (rangeFrac_[0] != rangeFrac_[1])) {
			if (rangeContext_ != null) {
				double[] minMax = AgentObservations.getInstance()
						.getActionRanges(rangeContext_);
				double minBound = (rangeBounds_[0].getValue(minMax));
				double maxBound = (rangeBounds_[1].getValue(minMax));
				double diff = maxBound - minBound;
				return "(" + (minBound + diff * rangeFrac_[0]) + " <= "
						+ stringArg_ + " <= "
						+ (minBound + diff * rangeFrac_[1]) + ")";
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
	 * Creates a variable representing a goal term at a given index.
	 * 
	 * @param i
	 *            The index of the goal condition.
	 * @return The variable goal condition.
	 */
	public static String createGoalTerm(int i) {
		return GOAL_VARIABLE_PREFIX + i;
	}

	/**
	 * Generates a variable term string from the index given.
	 * 
	 * @param i
	 *            The index of the string.
	 * @return A string in variable format, with the name of the variable in the
	 *         middle.
	 */
	public static RelationalArgument getVariableTermArg(int i) {
		char variable = (char) (FIRST_CHAR + (STARTING_CHAR - FIRST_CHAR + i)
				% MODULO_LETTERS);
		return new RelationalArgument("?" + variable);
	}

	public static boolean isGoalCondition(String arg) {
		return arg.startsWith(GOAL_VARIABLE_PREFIX);
	}

	/**
	 * An enum that defines what type of argument this argument is. The enum
	 * also defines ordering/ranking of args.
	 */
	private enum ArgType {
		CONST, NUMBER_CONST, NUMBER_RANGE, VAR, ANON;
	}
}
