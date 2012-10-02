/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/rlPacMan/DistanceDir.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
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
