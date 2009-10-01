package rlPacMan;

import java.lang.reflect.Method;

import org.mandarax.kernel.Constructor;
import org.mandarax.kernel.Predicate;
import org.mandarax.kernel.meta.JPredicate;

import relationalFramework.ObservationCondition;

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
	 * @param observationValue
	 *            The value to compare the observation against.
	 */
	public PacManObservation(PacManObservationSet observation,
			double observationValue) {
		super(observation, observationValue);
	}

	/**
	 * A constructor for creating a new PacManObservation using the observation
	 * index.
	 * 
	 * @param observationIndex
	 *            The observation index.
	 * @param compareType
	 *            The type of value comparison.
	 * @param observationValue
	 *            The value to compare the observation against.
	 */
	public PacManObservation(int observationIndex, double observationValue) {
		this(getConditionAt(observationIndex), observationValue);
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

	@Override
	public ConditionObject[] getEnumValues() {
		return PacManObservationSet.values();
	}

	/**
	 * An enumeration of the hand-coded feature observations for the PacMan
	 * domain.
	 * 
	 * All distances are given as Manhattan distances, unless stated otherwise.
	 * 
	 * All predicate methods are measured as 'is X less than or equal to Val?'.
	 * This may be the distance or other numerical values.
	 * 
	 * @author Samuel J. Sarjant
	 */
	public static enum PacManObservationSet implements ValuedConditionObject {
		// The distance to the nearest dot (if any)
		NEAREST_DOT(new double[] { 1.0, 1.0, 3.0, 6.0, 12.0, 99 },
				"proximalDot", new Class[] { Dot.class, Integer.class }),
		// The distance to the nearest power dot (if any)
		NEAREST_POWER_DOT(new double[] { 9.0, 14.0, 19.0, 24.0, 31.0, 99 },
				"proximalPowerDot",
				new Class[] { PowerDot.class, Integer.class }),
		// The distance to the nearest ghost (if any)
		NEAREST_GHOST(new double[] { 4.0, 6.0, 10.0, 15.0, 21.0, 99 },
				"proximalGhost", new Class[] { Ghost.class, Integer.class }),
		// The distance to the nearest edible ghost (if any)
		NEAREST_ED_GHOST(new double[] { 3.0, 6.0, 10.0, 13.0, 19.0, 99 },
				"proximalEdibleGhost",
				new Class[] { Ghost.class, Integer.class }),
		// The distance to the nearest fruit (if any)
		NEAREST_FRUIT(new double[] { 2.0, 8.0, 13.0, 18.0, 24.0, 99 },
				"proximalFruit", new Class[] { Fruit.class, Integer.class }),
		// The value of the safest junction (given as the distance pacman -
		// nearest ghost to that junction)
		MAX_JUNCTION_SAFETY(new double[] { 2.0, 4.0, 8.0, 13.0, 19.0, 99 },
				"safeJunction", new Class[] { JunctionPoint.class,
						Integer.class }),
		// The Euclidean distance from the centre of the ghosts
		GHOST_CENTRE_DIST(new double[] { 5.41, 7.92, 10.15, 12.5, 15.79, 99 },
				"ghostCentre", new Class[] { Double.class }),
		// The Euclidean distance from the centre of the dots
		DOT_CENTRE_DIST(new double[] { 4.12, 8.49, 11.31, 14.0, 17.12, 99 },
				"dotCentre", new Class[] { Double.class }),
		// The cumulative density of the ghosts (each ghost has density with
		// radius
		// 10 with linear decay)
		GHOST_DENSITY(new double[] { 0.0, 1.51, 4.90, 7.79, 12.96, 99 },
				"ghostDensity", new Class[] { Double.class }),
		// The 'travelling salesman' distance from pacman to each ghost
		TOTAL_DIST_TO_GHOSTS(new double[] { 19.38, 24.1, 28.25, 33.08, 39.77,
				99 }, "ghostDistanceSum", new Class[] { Double.class }),
		// A binary observation whether the ghosts are flashing or not
		GHOSTS_FLASHING(new double[] { 1 }, "ghostsFlashing", new Class[0]);

		/**
		 * The evenly distributed set of values for this particular observation.
		 */
		private double[] setOfVals_;

		/**
		 * The associated method for the predicate.
		 */
		private Predicate predicate_;

		/**
		 * A constructor for the observation values.
		 * 
		 * @param vals
		 *            The set of observation values to use.
		 * @param methodName
		 *            The name of the predicate method to use.
		 * @param predicateStructure
		 *            The arguments taken by the predicate.
		 */
		private PacManObservationSet(double[] vals, String methodName,
				Class[] predicateStructure) {
			setOfVals_ = vals;

			try {
				Method method = PacManEnvironment.class.getMethod(methodName,
						predicateStructure);

				// Predicate
				predicate_ = new JPredicate(method);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		@Override
		public double[] getSetOfVals() {
			return setOfVals_;
		}

		@Override
		public Constructor getConstructor() {
			return predicate_;
		}
	}
}
