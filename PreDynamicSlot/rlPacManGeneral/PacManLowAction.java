package rlPacManGeneral;

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
