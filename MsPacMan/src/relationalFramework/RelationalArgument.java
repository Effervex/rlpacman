package relationalFramework;

import java.io.Serializable;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private static Pattern RANGE_PATTERN = Pattern
			.compile("(.+)&:\\(<= ([\\dE.-]+) \\1 ([\\dE.-]+)\\)");
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
	private final double[] rangeArg_ = new double[2];

	/** The argument represented by this arg. */
	private String stringArg_;

	/**
	 * The constructor. This deconstructs ranges into their components.
	 */
	public RelationalArgument(String arg) {
		// Check for a numerical range
		if (arg.length() >= MIN_RANGE) {
			Matcher m = RANGE_PATTERN.matcher(arg);

			if (m.find()) {
				stringArg_ = m.group(1);
				rangeArg_[0] = Double.parseDouble(m.group(2));
				rangeArg_[1] = Double.parseDouble(m.group(3));
				argType_ = ArgType.NUMBER_RANGE;
				return;
			}
		}

		stringArg_ = arg;

		// The arg could still be a number
		try {
			double num = Double.parseDouble(arg);
			rangeArg_[0] = num;
			rangeArg_[1] = num;
			argType_ = ArgType.NUMBER_CONST;
			return;
		} catch (Exception e) {
		}

		// Determine arg type
		if (stringArg_.charAt(0) == '?') {
			if (stringArg_.length() == 1)
				argType_ = ArgType.ANON;
			argType_ = ArgType.VAR;
		} else {
			argType_ = ArgType.CONST;
		}
	}

	/**
	 * A constructor for a new range.
	 * 
	 * @param variable
	 *            The variable of the range.
	 * @param min
	 *            The minimum value of the range.
	 * @param max
	 *            The maximum value of the range.
	 */
	public RelationalArgument(String variable, double min, double max) {
		stringArg_ = variable;
		rangeArg_[0] = min;
		rangeArg_[1] = max;
		if (min == max)
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
				rangeArg_[0], rangeArg_[1]);
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
		result = Double.compare(rangeArg_[0], ra.rangeArg_[0]);
		if (result != 0)
			return result;
		result = Double.compare(rangeArg_[1], ra.rangeArg_[1]);
		if (result != 0)
			return result;

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
		if (!Arrays.equals(rangeArg_, other.rangeArg_))
			return false;
		if (stringArg_ == null) {
			if (other.stringArg_ != null)
				return false;
		} else if (!stringArg_.equals(other.stringArg_))
			return false;
		return true;
	}

	public double[] getRangeArg() {
		if (isNumber())
			return rangeArg_;
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
		if (stringArg_.charAt(0) != '?' || stringArg_.length() < 2)
			return -1;

		// Don't swap number variables
		if (stringArg_.startsWith(RANGE_VARIABLE_PREFIX))
			return -1;

		if (stringArg_.startsWith(GOAL_VARIABLE_PREFIX))
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
		result = prime * result + Arrays.hashCode(rangeArg_);
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
		if (argType_ == ArgType.CONST
				|| stringArg_.startsWith(GOAL_VARIABLE_PREFIX))
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
		if (isNumber() && (rangeArg_[0] != rangeArg_[1])) {
			return "(" + rangeArg_[0] + " <= " + stringArg_ + " <= "
					+ rangeArg_[1] + ")";
		}
		return stringArg_;
	}

	@Override
	public String toString() {
		if (isNumber() && (rangeArg_[0] != rangeArg_[1])) {
			return stringArg_ + "&:(<= " + rangeArg_[0] + " " + stringArg_
					+ " " + rangeArg_[1] + ")";
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
