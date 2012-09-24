package rlPacMan;

/**
 * A small class representing an integer distance and the direction to take to
 * eventually get to the location.
 * 
 * @author Sam Sarjant
 */
public class DistanceDir {
	/** The distance from the origin. */
	private int distance_;

	/** The direction to take to get to the location from the origin. */
	private byte direction_;

	public DistanceDir(int distance, byte direction) {
		distance_ = distance;
		direction_ = direction;
	}

	public int getDistance() {
		return distance_;
	}

	public byte getDirection() {
		return direction_;
	}
}
