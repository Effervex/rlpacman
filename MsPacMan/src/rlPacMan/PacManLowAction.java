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
 *    src/rlPacMan/PacManLowAction.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package rlPacMan;

import java.awt.event.KeyEvent;

public enum PacManLowAction {
	NOTHING(KeyEvent.VK_0), // Do nothing (PacMan will continue in the same
	// direction)
	UP(KeyEvent.VK_UP), // Move up (when possible)
	DOWN(KeyEvent.VK_DOWN), // Move down (when possible)
	LEFT(KeyEvent.VK_LEFT), // Move left (when possible)
	RIGHT(KeyEvent.VK_RIGHT); // Move right (when possible)

	/** The associated key for the movement. */
	private int associatedKey_;

	/**
	 * The private constructor for the enum.
	 * 
	 * @param key
	 *            The key associated with the movement.
	 */
	private PacManLowAction(int key) {
		associatedKey_ = key;
	}

	/**
	 * Gets the associated key with the movement.
	 * 
	 * @return The value of the key.
	 */
	public int getKey() {
		return associatedKey_;
	}

	/**
	 * A simple method that returns the opposite to the current direction.
	 * Nothing's opposite is nothing.
	 * 
	 * @return The opposite direction to this current direction.
	 */
	public PacManLowAction opposite() {
		switch (this) {
		case UP:
			return DOWN;
		case DOWN:
			return UP;
		case LEFT:
			return RIGHT;
		case RIGHT:
			return LEFT;
		}
		return NOTHING;
	}
}
