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
 *    src/rlPacMan/WeightedDirection.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package rlPacMan;

import msPacMan.Thing;

/**
 * A simple class combining weight and direction.
 * 
 * @author Sam Sarjant
 */
public class WeightedDirection {
	private byte direction_;
	private double weight_;

	public WeightedDirection(byte direction, double weight) {
		direction_ = direction;
		weight_ = weight;
	}

	/**
	 * @return the direction_
	 */
	public byte getDirection() {
		return direction_;
	}

	/**
	 * @return the weight_
	 */
	public double getWeight() {
		return weight_;
	}

	@Override
	public String toString() {
		String buffer = null;
		switch (direction_) {
		case Thing.UP:
			buffer = "UP";
			break;
		case Thing.DOWN:
			buffer = "DOWN";
			break;
		case Thing.LEFT:
			buffer = "LEFT";
			break;
		case Thing.RIGHT:
			buffer = "RIGHT";
			break;
		}
		return buffer + ": " + weight_;
	}
}