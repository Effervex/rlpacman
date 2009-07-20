package rlPacMan;

/**
 * An enumeration of the hand-coded feature observations for the PacMan domain.
 * 
 * All distances are given as Manhattan distances, unless stated otherwise.
 * 
 * @author Samuel J. Sarjant TODO Observation values
 */
public enum PacManObservations {
	// A constant value of 1
	CONSTANT(new double[] { 1, 1, 1, 1, 1 }),
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
	// The value of the safest junction (given as the distance pacman - nearest
	// ghost to that junction)
	MAX_JUNCTION_SAFETY(new double[] { 2.0, 4.0, 8.0, 13.0, 19.0 }),
	// The Euclidean distance from the centre of the ghosts
	GHOST_CENTRE_DIST(new double[] { 5.408326913195984, 7.923242882669809,
			10.151970252123476, 12.5, 15.793810320642846 }),
	// The Euclidean distance from the centre of the dots
	DOT_CENTRE_DIST(new double[] { 4.123105625617661, 8.48528137423857,
			11.313708498984761, 14.0, 17.11724276862369 }),
	// The cumulative density of the ghosts (each ghost has density with radius
	// 10 with linear decay)
	GHOST_DENSITY(new double[] { 0.0, 1.5147186257614305, 4.9009804864072155,
			7.788897449072022, 12.962036773562467 }),
	// The 'travelling salesman' distance from pacman to each ghost
	TOTAL_DIST_TO_GHOSTS(new double[] { 19.379129402873303, 24.095023109728988,
			28.25169480975245, 33.08499319860336, 39.771071127844365 });

	/** The evenly distributed set of values for this particular observation. */
	private double[] setOfVals_;

	/**
	 * A constructor for the observation values.
	 * 
	 * @param vals
	 *            The set of observation values to use.
	 */
	private PacManObservations(double[] vals) {
		setOfVals_ = vals;
	}

	/**
	 * Gets the set of values.
	 * 
	 * @return The set of values for this value.
	 */
	public double[] getSetOfVals() {
		return setOfVals_;
	}
}
