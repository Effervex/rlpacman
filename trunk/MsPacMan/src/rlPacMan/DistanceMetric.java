package rlPacMan;

/**
 * A discretisation of distance for use in the distance predicate.
 * 
 * @author Sam Sarjant
 */
public enum DistanceMetric {
	NEAR(5),
	MID(15),
	FAR(99);
	
	private int maxDistance_;
	
	private DistanceMetric(int distance) {
		maxDistance_ = distance;
	}
	
	public int getMaxDistance() {
		return maxDistance_;
	}
}
