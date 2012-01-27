package cerrla.modular;

import org.apache.commons.collections.BidiMap;

/**
 * A simple interface to denote if an object is evaluatable as a relational
 * object.
 * 
 * @author Sam Sarjant
 */
public interface PolicyItem {

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

	/**
	 * If the object should regenerate into a new modular policy.
	 * 
	 * @return True if the object should regenerate into a new modular policy.
	 */
	public boolean shouldRegenerate();

	/**
	 * Gets the size of this object.
	 * 
	 * @return 1 if this is a single object, more if it is a collection.
	 */
	public int size();
}
