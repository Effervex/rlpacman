package rlPacMan;

/**
 * An enumeration of the hand-coded feature observations for the PacMan domain.
 * 
 * All distances are given as Manhattan distances, unless stated otherwise.
 * 
 * @author Samuel J. Sarjant
 * 
 */
public enum PacManObservations {
	CONSTANT, // A constant value of 1
	NEAREST_DOT, // The distance to the nearest dot
	NEAREST_POWER_DOT, // The distance to the nearest power dot
	NEAREST_GHOST, // The distance to the nearest ghost
	NEAREST_ED_GHOST, // The distance to the nearest edible ghost
	MAX_JUNCTION_SAFETY, // The value of the safest junction (given as the
	// distance pacman - nearest ghost to that junction)
	GHOST_CENTRE_DIST, // The Euclidean distance from the centre of the ghosts
	DOT_CENTRE_DIST, // The Euclidean distance from the centre of the dots
	GHOST_DENSITY, // The cumulative density of the ghosts (each ghost has
	// density with radius 10 with linear decay)
	TOTAL_DIST_TO_GHOSTS; // The 'travelling salesman' distance from pacman to
							// each ghost
}
