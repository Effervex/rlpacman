package rlPacMan;

import java.awt.Point;

public class JunctionPoint extends PacPoint implements
		Comparable<JunctionPoint> {
	private byte direction_;
	private int distance_;
	private int safety_ = Integer.MAX_VALUE;

	public JunctionPoint(Point loc, byte dir, int dist) {
		m_locX = loc.x;
		m_locY = loc.y;
		direction_ = dir;
		distance_ = dist;
	}

	// @Override
	public int compareTo(JunctionPoint otherPoint) {
		// Mainly comparable by distance
		if (otherPoint.distance_ < distance_)
			return 1;
		if (otherPoint.distance_ > distance_)
			return -1;

		// Compare by trivial matters
		if (otherPoint.m_locX < m_locX)
			return 1;
		if (otherPoint.m_locX > m_locX)
			return -1;
		if (otherPoint.m_locY < m_locY)
			return 1;
		if (otherPoint.m_locY > m_locY)
			return -1;
		if (otherPoint.direction_ < direction_)
			return 1;
		if (otherPoint.direction_ > direction_)
			return -1;
		if (otherPoint.safety_ < safety_)
			return 1;
		if (otherPoint.safety_ > safety_)
			return -1;
		return 0;
	}

	// @Override
	public boolean equals(Object obj) {
		if ((obj == null) || (!(obj instanceof JunctionPoint)))
			return false;
		JunctionPoint other = (JunctionPoint) obj;
		// All must be equal for truth
		if ((m_locX == other.m_locX) && (m_locY == other.m_locY)
				&& (distance_ == other.distance_)
				&& (direction_ == other.direction_)
				&& (safety_ == other.safety_))
			return true;
		return false;
	}

	// @Override
	public int hashCode() {
		return distance_ + direction_ + m_locX + m_locY;
	}

	@Override
	public String toString() {
		return "Junction: " + m_locX + "," + m_locY;
	}

	public int getDistance() {
		return distance_;
	}

	public Point getLocation() {
		return new Point(m_locX, m_locY);
	}

	public byte getDirection() {
		return direction_;
	}

	public void setSafety(int safety) {
		safety_ = safety;
	}

	public int getSafety() {
		return safety_;
	}
}
