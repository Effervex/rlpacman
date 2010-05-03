package relationalFramework;

import jess.Rete;

/**
 * A bridge for the observations that need to be as Java Objects, rather than
 * primitives.
 * 
 * @author Sam Sarjant
 */
public class RelationalObservation {
	public static final String NO_PRE_GOAL = "NoPreGoal";

	/** The array of objects begin used as observations. */
	public Object[] objectArray;

	/** The knowledge base of current state predicate and background knowledge. */
	public Rete predicateKB;

	/**
	 * The private, empty constructor
	 */
	public RelationalObservation() {
		objectArray = new Object[1];
	}
}
