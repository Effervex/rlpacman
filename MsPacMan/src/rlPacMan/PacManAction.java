package rlPacMan;

import relationalFramework.ActionCondition;

/**
 * A class containing the actions used in Ms. PacMan.
 * 
 * @author Samuel J. Sarjant
 */
public class PacManAction extends ActionCondition {
	/**
	 * A constructor for a Ms. PacMan action with operator.
	 * 
	 * @param action The action.
	 * @param actionOperator The operator on the action.
	 */
	public PacManAction(PacManActionSet action, boolean actionOperator) {
		super(action, actionOperator);
	}
	
	/**
	 * A constructor that uses the raw index of the action.
	 * 
	 * @param actionIndex The index of the action.
	 * @param actionOperator The operator of the action.
	 */
	public PacManAction(int actionIndex, boolean actionOperator) {
		this(getConditionAt(actionIndex), actionOperator);
	}
	
	public PacManAction() {
		
	}

	/**
	 * Gets the condition object at the given index.
	 * 
	 * @param index
	 *            The index of the object.
	 * @return The object at the index or null.
	 */
	public static PacManActionSet getConditionAt(int index) {
		PacManActionSet[] values = PacManActionSet.values();
		if (index >= values.length)
			return null;
		return values[index];
	}
	
	public ConditionObject[] getEnumValues() {
		return PacManActionSet.values();
	}
	
	/**
	 * An enumeration of the actions present in ms. PacMan.
	 * 
	 * @author Samuel J. Sarjant
	 */
	public static enum PacManActionSet implements ActionConditionObject {
		TO_DOT, // Go towards the nearest dot
		TO_POWER_DOT, // Go towards the nearest power dot
		TO_FRUIT, // Go towards the fruit (NEW ACTION)
		FROM_POWER_DOT, // Go in the direction opposite to the nearest power dot
		TO_ED_GHOST, // Go towards the nearest edible ghost
		FROM_GHOST, // Go in a direction opposite the nearest ghost
		TO_SAFE_JUNCTION, // Go towards the maximally safe junction
		FROM_GHOST_CENTRE, // Go in a direction which maximises the Euclidean
		// distance from the ghost centre
		KEEP_DIRECTION, // Continue in the current direction or choose a random
		// available direction (except turning back) if that is
		// impossible
		// TO_LOWER_GHOST_DENSITY, // Go in the direction where the cumulative
		// ghost
		// density decreases fastest
		// TO_GHOST_FREE_AREA, // Head towards the location where the minimum
		// ghost
		// distance is largest
		TO_CENTRE_OF_DOTS; // An unlisted action that moves in a direction that
		// minimises the Euclidean distance from the centre of
		// the dots.
	}
}
