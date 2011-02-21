package relationalFramework;

import jess.Rete;

/**
 * A bridge for the observations that need to be as Java Objects, rather than
 * primitives.
 * 
 * @author Sam Sarjant
 */
public class ObjectObservations {
	/** The ID used in the regular observations to identify this. */
	public static final String OBSERVATION_ID = "ObjectObservations";

	public static final String NO_PRE_GOAL = "NoPreGoal";

	/** The private singleton instance. */
	private static ObjectObservations instance_;

	/** The array of objects begin used as observations. */
	public Object[] objectArray;

	/** The knowledge base of current state predicate and background knowledge. */
	public Rete predicateKB;

	/** The valid actions for the above state. */
	public MultiMap<String, String[]> validActions;

	/**
	 * The private, empty constructor
	 */
	private ObjectObservations() {
		objectArray = new Object[1];
	}

	public static ObjectObservations getInstance() {
		if (instance_ == null)
			instance_ = new ObjectObservations();
		return instance_;
	}

	/**
	 * Gets object at index.
	 * 
	 * @param index
	 *            The index of the object.
	 * @return The object at index.
	 */
	public Object getObject(int index) {
		return objectArray[index];
	}

	/**
	 * Sets an object at an index.
	 * 
	 * @param index
	 *            The index to set at.
	 * @param object
	 *            The object being set.
	 */
	public void setObject(int index, Object object) {
		objectArray[index] = object;
	}

	/**
	 * Gets the number of objects present.
	 * 
	 * @return The number of objects.
	 */
	public int getNumObject() {
		return objectArray.length;
	}

	/**
	 * Sets the no pre-goal observation.
	 */
	public void setNoPreGoal() {
		objectArray = new String[] { NO_PRE_GOAL };
	}
}
