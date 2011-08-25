package relationalFramework;

import relationalFramework.State;

import java.util.Arrays;

/**
 * An interface for the state enum, which categorises state variables.
 * 
 * @author Sam Sarjant
 */
public abstract class State {
	/** The suffix to this class for use with dynamically loaded classes. */
	public static final String CLASS_SUFFIX = "State";
	
	/** The state array. */
	private Object[] stateArray_;
	
	/**
	 * Constructor for a new state.
	 * 
	 * @param stateArray The state array.
	 */
	public State(Object[] stateArray) {
		stateArray_ = stateArray;
	}
	
	/**
	 * Simple toString method.
	 */
	@Override
	public String toString() {
		return "State";
	}

	public Object[] getStateArray() {
		return stateArray_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(stateArray_);
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
		final State other = (State) obj;
		if (!Arrays.equals(stateArray_, other.stateArray_))
			return false;
		return true;
	}
}
