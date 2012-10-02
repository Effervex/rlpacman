/*
 *    This file is part of the CERRLA algorithm, but was originally obtained
 *    from bennychow.com. Used with permission.
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
 *    src/msPacMan/Thing.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package msPacMan;

import java.awt.*;
import java.util.Random;


// Pacman and Ghosts
public abstract class Thing extends PacPoint {
	// Thing's next step is one of these constants
	public static final byte STILL = 0;
	public static final byte UP = 1;
	public static final byte DOWN = 2;
	public static final byte LEFT = 3;
	public static final byte RIGHT = 4;
	public int m_deltaMax = 3; // m_deltaMax * 2 - 1 Intervals between two cells for
	// smooth animation

	// Thing Type
	static final byte PACMAN = 0;
	static final byte GHOST = 1;
	static final byte FRUIT = 2;

	boolean m_bInsideRoom;

	public int m_deltaLocX = 0; // Delta between cells, i.e. x -> x+1
	public int m_deltaLocY = 0; // Delta between cells, i.e. x -> x+1
	int m_lastLocX;
	int m_lastLocY;
	int m_lastDeltaLocX = 0; // Delta between cells, i.e. x -> x+1
	int m_lastDeltaLocY = 0; // Delta between cells, i.e. x -> x+1
	byte m_direction;
	public int m_startX; // Starting X location of Thing when game is reset
	public int m_startY; // Starting Y location of Thing when game is reset
	int m_deltaStartX; // Starting deltaX in case Thing needs to be between
	// cells
	GameModel m_gameModel;
	Rectangle m_boundingBox; // Bounding box of Thing in pixels
	boolean m_bPaused = false;
	boolean m_bVisible = false;
	
	int pixelX_;
	int pixelY_;
	int pixelSize_;
	int pixelShrink_;
	
	Random random_ = new Random(0);

	public Thing(GameModel gameModel, int startX, int startY, boolean bMiddleX) {
		m_startX = startX;
		m_startY = startY;
		m_deltaStartX = 0;
		m_locX = -1;
		m_locY = -1;
		m_lastLocX = m_startX;
		m_lastLocY = m_startY;
		m_direction = STILL;
		m_gameModel = gameModel;
		m_bInsideRoom = false;
		m_boundingBox = new Rectangle();

		if (bMiddleX) {
			m_deltaLocX = m_deltaMax - 1;
			m_lastDeltaLocX = m_deltaLocX;
			m_deltaStartX = m_deltaLocX;
		}
	}

	public void eatItem() {
	}

	public void draw(GameUI gameUI, Graphics g2) {
	}

	public void tickThing(GameUI gameUI) {
		updatePixelVals(gameUI);
		m_boundingBox.setBounds(pixelX_, pixelY_, pixelSize_, pixelSize_);
		m_boundingBox.grow(pixelShrink_, pixelShrink_);
	}

	public int checkCollision(Player player) {
		return 0;
	}

	protected abstract void updatePixelVals(GameUI gameUI);

	// Called to return the Thing back to starting location
	public void returnToStart() {
		m_locX = m_startX;
		m_locY = m_startY;
		m_lastLocX = m_startX;
		m_lastLocY = m_startY;
		m_deltaLocX = m_deltaStartX;
		m_deltaLocY = 0;
		m_lastDeltaLocX = m_deltaStartX;
		m_lastDeltaLocY = 0;
		m_bPaused = false;
		m_direction = STILL;
		m_boundingBox.setBounds(0, 0, 0, 0);
	}

	public boolean canMove() {
		return !m_bPaused;
	}

	public void setVisible(boolean bVisible) {
		m_bVisible = bVisible;
	}

	public void setPaused(boolean bPaused) {
		m_bPaused = bPaused;
	}
	
	/**
	 * Checks if a movement is valid.
	 * 
	 * @param direction The direction to move in.
	 * @param locX The x location.
	 * @param locY The y location.
	 * @param model The game model.
	 * @return True if the movement is allowed, false otherwise.
	 */
	public static boolean isValidMove(int direction, int locX, int locY, GameModel model) {
		// If the request direction is blocked by a wall, then just return the
		// current location
		if ((direction == UP && (model.m_gameState[locX][locY] & GameModel.GS_NORTH) != 0)
				|| (direction == LEFT && (model.m_gameState[locX][locY] & GameModel.GS_WEST) != 0)
				|| (direction == DOWN && (model.m_gameState[locX][locY] & GameModel.GS_SOUTH) != 0)
				|| (direction == RIGHT && (model.m_gameState[locX][locY] & GameModel.GS_EAST) != 0)) {
			return false;
		}
		return true;
	}

	// This method will take the specified location and direction and determine
	// for the given location if the thing moved in that direction, what the
	// next possible turning location would be.
	public static boolean getDestination(int direction, int locX, int locY,
			Point point, GameModel model) {
		if (!isValidMove(direction, locX, locY, model))
			return false;

		// Start off by advancing one in direction for specified location
		switch (direction) {
		case UP:
			locY--;
			break;
		case DOWN:
			locY++;
			break;
		case LEFT:
			locX--;
			break;
		case RIGHT:
			locX++;
			break;
		}

		locX = (locX + model.m_gameSizeX) % model.m_gameSizeX;

		// If we violate the grid boundary,
		// then return false.
		if (locY < 0 || locX < 0 || locY >= model.m_gameSizeY
				|| locX >= model.m_gameSizeX)
			return false;

		// Determine next turning location..
		while (true) {
			if (direction == UP || direction == DOWN) {
				if ((model.m_gameState[locX][locY] & GameModel.GS_EAST) == 0
						|| (model.m_gameState[locX][locY] & GameModel.GS_WEST) == 0
						|| (model.m_gameState[locX][locY] & GameModel.GS_NORTH) != 0
						|| (model.m_gameState[locX][locY] & GameModel.GS_SOUTH) != 0) {
					point.setLocation(locX, locY);
					break;
				} else {
					if (direction == UP) {
						// Check for Top Warp
						if (locY == 0) {
							point.setLocation(locX, model.m_gameSizeY - 1);
							break;
						} else {
							locY--;
						}
					} else {
						// Check for Bottom Warp
						if (locY == model.m_gameSizeY - 1) {
							point.setLocation(locX, 0);
							break;
						} else {
							locY++;
						}
					}
				}
			} else {
				if ((model.m_gameState[locX][locY] & GameModel.GS_NORTH) == 0
						|| (model.m_gameState[locX][locY] & GameModel.GS_SOUTH) == 0
						|| (model.m_gameState[locX][locY] & GameModel.GS_EAST) != 0
						|| (model.m_gameState[locX][locY] & GameModel.GS_WEST) != 0) {
					point.setLocation(locX, locY);
					break;
				} else {
					if (direction == LEFT) {
						// Check for Left Warp
						if (locX == 0) {
							point.setLocation(model.m_gameSizeX - 1, locY);
							break;
						} else {
							locX--;
						}
					} else {
						// Check for Right Warp
						if (locX == model.m_gameSizeX - 1) {
							point.setLocation(0, locY);
							break;
						} else {
							locX++;
						}
					}
				}
			}
		}
		return true;
	}
}
