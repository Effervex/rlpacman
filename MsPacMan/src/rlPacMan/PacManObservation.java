package rlPacMan;

import crossEntropyFramework.ObservationCondition;

/**
 * A class to represent a full evaluatable rule condition.
 * 
 * @author Samuel J. Sarjant
 */
public class PacManObservation extends ObservationCondition {
	/**
	 * A constructor for creating a new PacManObservation from an observation, a
	 * comparator and a value.
	 * 
	 * @param observation
	 *            The observation.
	 * @param compareType
	 *            The type of value comparison.
	 * @param observationValue
	 *            The value to compare the observation against.
	 */
	public PacManObservation(PacManObservationSet observation,
			boolean compareType, double observationValue) {
		super(observation, compareType, observationValue);
	}
	
	/**
	 * A constructor for creating a new PacManObservation using the observation index.
	 * 
	 * @param observationIndex
	 *            The observation index.
	 * @param compareType
	 *            The type of value comparison.
	 * @param observationValue
	 *            The value to compare the observation against.
	 */
	public PacManObservation(int observationIndex,
			boolean compareType, double observationValue) {
		this(getConditionAt(observationIndex), compareType, observationValue);
	}
	
	public PacManObservation() {
		
	}

	/**
	 * Gets the condition object at the given index.
	 * 
	 * @param index
	 *            The index of the object.
	 * @return The object at the index or null.
	 */
	public static PacManObservationSet getConditionAt(int index) {
		PacManObservationSet[] values = PacManObservationSet.values();
		if (index >= values.length)
			return null;
		return values[index];
	}
	
	public ConditionObject[] getEnumValues() {
		return PacManObservationSet.values();
	}

	/**
	 * An enumeration of the hand-coded feature observations for the PacMan
	 * domain.
	 * 
	 * All distances are given as Manhattan distances, unless stated otherwise.
	 * 
	 * @author Samuel J. Sarjant
	 */
	public static enum PacManObservationSet implements ValuedConditionObject {
		// A constant value of 1
		CONSTANT(new double[] { 1 }),
		// The distance to the nearest dot (if any)
		NEAREST_DOT(new double[] { 1.0, 1.0, 3.0, 6.0, 12.0 }),
		// The distance to the nearest power dot (if any)
		NEAREST_POWER_DOT(new double[] { 9.0, 14.0, 19.0, 24.0, 31.0 }),
		// The distance to the nearest ghost (if any)
		NEAREST_GHOST(new double[] { 4.0, 6.0, 10.0, 15.0, 21.0 }),
		// The distance to the nearest edible ghost (if any)
		NEAREST_ED_GHOST(new double[] { 3.0, 6.0, 10.0, 13.0, 19.0 }),
		// The distance to the nearest fruit (if any)
		NEAREST_FRUIT(new double[] { 2.0, 8.0, 13.0, 18.0, 24.0 }),
		// The value of the safest junction (given as the distance pacman -
		// nearest
		// ghost to that junction)
		MAX_JUNCTION_SAFETY(new double[] { 2.0, 4.0, 8.0, 13.0, 19.0 }),
		// The Euclidean distance from the centre of the ghosts
		GHOST_CENTRE_DIST(new double[] { 5.408326913195984, 7.923242882669809,
				10.151970252123476, 12.5, 15.793810320642846 }),
		// The Euclidean distance from the centre of the dots
		DOT_CENTRE_DIST(new double[] { 4.123105625617661, 8.48528137423857,
				11.313708498984761, 14.0, 17.11724276862369 }),
		// The cumulative density of the ghosts (each ghost has density with
		// radius
		// 10 with linear decay)
		GHOST_DENSITY(new double[] { 0.0, 1.5147186257614305,
				4.9009804864072155, 7.788897449072022, 12.962036773562467 }),
		// The 'travelling salesman' distance from pacman to each ghost
		TOTAL_DIST_TO_GHOSTS(new double[] { 19.379129402873303,
				24.095023109728988, 28.25169480975245, 33.08499319860336,
				39.771071127844365 }),
		// A binary observation whether the ghosts are flashing or not
		GHOSTS_FLASHING(new double[] { 1 });

		/** The evenly distributed set of values for this particular observation. */
		private double[] setOfVals_;

		/**
		 * A constructor for the observation values.
		 * 
		 * @param vals
		 *            The set of observation values to use.
		 */
		private PacManObservationSet(double[] vals) {
			setOfVals_ = vals;
		}

		public double[] getSetOfVals() {
			return setOfVals_;
		}
	}
}
