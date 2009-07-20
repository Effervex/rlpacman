package rlPacMan;

import java.awt.Point;

public class JunctionPoint implements Comparable<JunctionPoint> {
	private Point location_;
	private byte direction_;
	private int distance_;

	public JunctionPoint(Point loc, byte dir, int dist) {
		location_ = loc;
		direction_ = dir;
		distance_ = dist;
	}

	//@Override
	public int compareTo(JunctionPoint otherPoint) {
		// Mainly comaparable by distance
		if (otherPoint.distance_ < distance_)
			return 1;
		if (otherPoint.distance_ > distance_)
			return -1;

		// Compare by hashcode
		if (otherPoint.hashCode() < hashCode())
			return 1;
		if (otherPoint.hashCode() > hashCode())
			return -1;
		return 0;
	}

	//@Override
	public boolean equals(Object obj) {
		if ((obj == null) || (!(obj instanceof JunctionPoint)))
			return false;
		JunctionPoint other = (JunctionPoint) obj;
		// All must be equal for truth
		if ((location_.equals(other.location_))
				&& (distance_ == other.distance_)
				&& (direction_ == other.direction_))
			return true;
		return false;
	}

	//@Override
	public int hashCode() {
		return distance_ + direction_ + location_.hashCode();
	}
	
	@Override
	public String toString() {
		return location_.toString() + ", " + direction_ + ": " + distance_;
	}

	public int getDistance() {
		return distance_;
	}

	public Point getLocation() {
		return location_;
	}

	public byte getDirection() {
		return direction_;
	}
}
