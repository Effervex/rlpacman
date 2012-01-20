package relationalFramework.agentObservations;

/**
 * A class which implements a settling factor for scans, such that it only
 * returns true for scanning on an exponentially decline frequency if no
 * modifications are made to the implementing class.
 * 
 * @author Sam Sarjant
 */
public abstract class SettlingScan {
	/** 2^SETTLED inactive iterations before considered settled. */
	private static final int SETTLED_THRESHOLD = 7;

	/** The amount of (2^) inactivity the observations has accrued. */
	private transient int inactivity_ = 0;

	/** The number of steps since last covering the state. */
	private transient int lastCover_ = 0;

	/** A hash code to track when observations change. */
	private transient Integer observationHash_ = null;

	protected int getInactivity() {
		return inactivity_;
	}

	/**
	 * Increments the inactivity counter (that is, this many scans have not
	 * changed the state).
	 */
	protected void incrementInactivity() {
		inactivity_++;
		lastCover_ = 0;
	}

	/**
	 * Checks if the subclass should scan based on exponential decreasing
	 * scanning frequencies.
	 * 
	 * @return True if the caller should scan.
	 */
	protected boolean isScanNeeded() {
		// Cover every X episodes, checking more and more infrequently if no
		// changes occur.
		if (lastCover_ >= (Math.pow(2, inactivity_) - 1)) {
			lastCover_ = 0;
			return true;
		}

		lastCover_++; // TODO This will be called multiple times.
		return false;
	}

	/**
	 * Updates the observation hash
	 */
	protected abstract int updateHash();

	/**
	 * Gets the observation hash for the current observations.
	 * @return
	 */
	public int getObservationHash() {
		if (observationHash_ == null)
			observationHash_ = updateHash();
		return observationHash_;
	}

	/**
	 * If the scan has reset due to a change.
	 * 
	 * @return True if the {@link SettlingScan} recently reset/was initialised.
	 */
	public boolean isChanged() {
		return inactivity_ == 0;
	}

	/**
	 * If the agent observations are basically settled (have not changed in X
	 * iterations).
	 * 
	 * @return If the inactivity has exceeded a particular threshold.
	 */
	public boolean isSettled() {
		return inactivity_ >= SETTLED_THRESHOLD;
	}
	
	/**
	 * Resets inactivity to 0.
	 */
	public void resetInactivity() {
		inactivity_ = 0;
		observationHash_ = null;
	}
}
