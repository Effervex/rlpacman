package relationalFramework;

import org.apache.commons.collections.BidiMap;

/**
 * A simple interface to denote if an object is evaluatable as a relational
 * object.
 * 
 * @author Sam Sarjant
 */
public interface RelationallyEvaluatableObject {

	/**
	 * The object needs to have goal parameters set to it.
	 * 
	 * @param goalArgs
	 */
	public void setParameters(BidiMap goalArgs);

	/**
	 * The object should have a method of looking like a nice string.
	 * 
	 * @return A nice String representation of the object.
	 */
	public String toNiceString();
}
