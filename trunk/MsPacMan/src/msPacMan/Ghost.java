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
 *    src/msPacMan/Ghost.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package msPacMan;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Random;


public class Ghost extends Thing {
	// Blinky (Red) behaviour (Aggressive)
	public static final byte BLINKY = 0;
	// Pinky behaviour (Ambush)
	public static final byte PINKY = 1;
	// Inky (Blue) behaviour (Erratic)
	public static final byte INKY = 2;
	// Clyde (Orange) behaviour (Ignorant)
	public static final byte CLYDE = 3;

	/** The density ratio of the ghosts. */
	public static final double DENSITY_RADIUS = 10;

	int[] m_ghostMouthX; // X points of Ghost's crooked mouth when Pacman
	// powersup
	int[] m_ghostMouthY; // Y points of Ghost's crooked mouth when Pacman
	// powersup
	Polygon m_ghostPolygon; // Alternate between these two polygons to draw the
	// ghost
	Polygon m_ghostPolygon2;
	boolean m_bOtherPolygon = false;
	int m_lastDirection;
	int m_destinationX;
	int m_destinationY;
	int m_targetX; // The actual X target of this ghost. May not be related to
	// the player
	int m_targetY; // The actual Y target of this ghost. May not be related to
	// the player
	Color m_color;
	/** The type of this ghost */
	byte m_type;
	/** Ticks before ghost is allowed to exit. */
	public int m_nTicks2Exit;
	/** Milliseconds before exiting. */
	int m_nExitMilliSec;
	/** How long the Ghost will run from Pacman */
	int m_nTicks2Flee = 0;
	/** Set to true when Pacman has eaten this ghost */
	public boolean m_bEaten = false;
	int m_ghostDeltaMax = 3; // Should never change
	int m_eatenPoints; // Point worth for eaten Ghost
	/** Ticks to display eaten points */
	int m_nTicks2Popup;
	boolean m_bEnteringDoor = false;
	boolean m_bChaseMode = false; // Chase Pacman, or scatter when false
	boolean m_bOldChaseMode = false;
	private int m_cornerX; // The ghost's corner to scatter to when chase mode
	// is off.
	private int m_cornerY; // The ghost's corner to scatter to when chase mode
	// is off.

	// Variables to toggle Ghost AI
	boolean m_bCanFollow = true; // Can ghosts follow each other, i.e. Same
	// destination and direction
	boolean m_bCanBackTrack = false; // Can ghost go back the direction they
	// came
	boolean m_bCanUseNextBest = true; // Can ghost try the next best direction
	// first 25% of the time
	boolean m_bInsaneAI = false; // No holds barred!
	/** If the ghost is flashing. */
	private boolean flashing_;

	Ghost(GameModel gameModel, byte type, int startX, int startY,
			boolean bMiddle, int nExitMilliSec, Random random) {
		super(gameModel, startX, startY, bMiddle);
		m_deltaMax = m_ghostDeltaMax;
		m_destinationX = -1;
		m_destinationY = -1;
		m_targetX = -1;
		m_targetY = -1;

		// Setting the ghosts colour and behaviour based on type
		m_type = type;
		switch (type) {
		case BLINKY:
			m_color = Color.RED;
			m_cornerX = m_gameModel.m_gameSizeX - 1;
			m_cornerY = 0;
			break;
		case PINKY:
			m_color = Color.PINK;
			m_cornerX = 0;
			m_cornerY = 0;
			break;
		case INKY:
			m_color = Color.CYAN;
			m_cornerX = m_gameModel.m_gameSizeX - 1;
			m_cornerY = m_gameModel.m_gameSizeY - 1;
			break;
		case CLYDE:
			m_color = Color.ORANGE;
			m_cornerX = 0;
			m_cornerY = m_gameModel.m_gameSizeY - 1;
			break;
		}

		m_bInsideRoom = true;
		m_nExitMilliSec = nExitMilliSec;
		m_nTicks2Exit = m_nExitMilliSec / gameModel.m_pacMan.m_delay;
		random_ = random;
	}

	/**
	 * Clones this ghost (in white form).
	 * 
	 * @return A clone of the ghost.
	 */
	@Override
	public Object clone() {
		Ghost clone = new Ghost(m_gameModel, Ghost.BLINKY, m_locX, m_locY,
				true, m_nExitMilliSec, random_);
		clone.m_lastDirection = m_lastDirection;
		clone.m_destinationX = m_destinationX;
		clone.m_destinationY = m_destinationY;
		clone.m_nTicks2Exit = m_nTicks2Exit;
		clone.m_nExitMilliSec = m_nExitMilliSec;
		clone.m_nTicks2Flee = m_nTicks2Flee;
		clone.m_bEaten = m_bEaten;
		clone.m_ghostDeltaMax = m_ghostDeltaMax;
		clone.m_eatenPoints = m_eatenPoints;
		clone.m_nTicks2Popup = m_nTicks2Popup;
		clone.m_bEnteringDoor = m_bEnteringDoor;
		clone.m_cornerX = m_cornerX;
		clone.m_cornerY = m_cornerY;
		clone.m_bInsideRoom = m_bInsideRoom;

		clone.m_locX = m_locX;
		clone.m_locY = m_locY;
		clone.m_lastLocX = m_lastLocX;
		clone.m_lastLocY = m_lastLocY;
		clone.m_direction = m_direction;
		clone.m_startX = m_startX;
		clone.m_startY = m_startY;
		clone.m_deltaStartX = m_deltaStartX;
		return clone;
	}

	// Overriden to draw Ghosts
	@Override
	public void draw(GameUI gameUI, Graphics g2) {
		if (!m_bVisible)
			return;

		// Ghost Head Diameter is also the Width and Height of the Ghost
		int ghostHeadDiameter = gameUI.CELL_LENGTH + gameUI.WALL1
				+ gameUI.WALL1;
		int ghostLegHalf = ghostHeadDiameter / 2;
		int ghostLegQuarter = ghostHeadDiameter / 4;
		int ghostLegUnit = ghostLegQuarter / 4;
		int ghostLegHeight = ghostLegQuarter * 3 / 4;

		if (m_ghostPolygon == null) // I.e. Ghosts is not Inited yet
		{
			int[] xPoints = { 0, 0, ghostLegUnit,
					ghostLegQuarter - ghostLegUnit,
					ghostLegQuarter + ghostLegUnit,
					ghostLegHalf - ghostLegUnit - ghostLegUnit,
					ghostLegHalf - ghostLegUnit - ghostLegUnit,
					ghostLegHalf + ghostLegUnit + ghostLegUnit,
					ghostLegHalf + ghostLegUnit + ghostLegUnit,
					ghostLegHalf + ghostLegQuarter - ghostLegUnit,
					ghostLegHalf + ghostLegQuarter + ghostLegUnit,
					ghostHeadDiameter - ghostLegUnit, ghostHeadDiameter,
					ghostHeadDiameter };

			int[] yPoints = { ghostHeadDiameter / 2, ghostHeadDiameter,
					ghostHeadDiameter, ghostHeadDiameter - ghostLegHeight,
					ghostHeadDiameter, ghostHeadDiameter,
					ghostHeadDiameter - ghostLegHeight,
					ghostHeadDiameter - ghostLegHeight, ghostHeadDiameter,
					ghostHeadDiameter, ghostHeadDiameter - ghostLegHeight,
					ghostHeadDiameter, ghostHeadDiameter, ghostHeadDiameter / 2 };

			int[] xPoints2 = { 0, 0, ghostLegUnit,
					ghostLegQuarter - ghostLegUnit, ghostLegQuarter,
					ghostLegHalf - ghostLegUnit - ghostLegUnit,
					ghostLegHalf + ghostLegUnit + ghostLegUnit,
					ghostLegHalf + ghostLegQuarter,
					ghostHeadDiameter - ghostLegQuarter + ghostLegUnit,
					ghostHeadDiameter - ghostLegUnit, ghostHeadDiameter,
					ghostHeadDiameter };

			int[] yPoints2 = { ghostHeadDiameter / 2,
					ghostHeadDiameter - ghostLegHeight, ghostHeadDiameter,
					ghostHeadDiameter, ghostHeadDiameter - ghostLegHeight,
					ghostHeadDiameter, ghostHeadDiameter,
					ghostHeadDiameter - ghostLegHeight, ghostHeadDiameter,
					ghostHeadDiameter, ghostHeadDiameter - ghostLegHeight,
					ghostHeadDiameter / 2 };

			m_ghostPolygon = new Polygon(xPoints, yPoints, xPoints.length);
			m_ghostPolygon2 = new Polygon(xPoints2, yPoints2, xPoints2.length);

			int ghostMouthHalf = ghostHeadDiameter / 2;
			int ghostMouthQuarter = ghostMouthHalf / 2;
			int ghostMouthTeeth = ghostMouthQuarter / 2;
			int ghostMouthY1 = ghostHeadDiameter / 2 + ghostHeadDiameter / 4;
			int ghostMouthY2 = ghostHeadDiameter / 2 + ghostHeadDiameter / 7;

			m_ghostMouthX = new int[7];
			m_ghostMouthX[0] = ghostMouthTeeth;
			m_ghostMouthX[1] = ghostMouthQuarter;
			m_ghostMouthX[2] = ghostMouthQuarter + ghostMouthTeeth;
			m_ghostMouthX[3] = ghostMouthHalf;
			m_ghostMouthX[4] = ghostMouthHalf + ghostMouthTeeth;
			m_ghostMouthX[5] = ghostMouthHalf + ghostMouthQuarter;
			m_ghostMouthX[6] = ghostMouthHalf + ghostMouthQuarter
					+ ghostMouthTeeth;

			m_ghostMouthY = new int[7];
			m_ghostMouthY[0] = ghostMouthY1;
			m_ghostMouthY[1] = ghostMouthY2;
			m_ghostMouthY[2] = ghostMouthY1;
			m_ghostMouthY[3] = ghostMouthY2;
			m_ghostMouthY[4] = ghostMouthY1;
			m_ghostMouthY[5] = ghostMouthY2;
			m_ghostMouthY[6] = ghostMouthY1;

		}

		Polygon polygon;
		int ghostX = gameUI.m_gridInset
				+ (int) (m_locX * gameUI.CELL_LENGTH - ghostHeadDiameter / 2.0
						+ gameUI.CELL_LENGTH / 2.0 + m_deltaLocX
						* (gameUI.CELL_LENGTH / (m_deltaMax * 2.0 - 1)));
		int ghostY = gameUI.m_gridInset
				+ (int) (m_locY * gameUI.CELL_LENGTH - ghostHeadDiameter / 2.0
						+ gameUI.CELL_LENGTH / 2.0 + m_deltaLocY
						* (gameUI.CELL_LENGTH / (m_deltaMax * 2.0 - 1)));

		// If Pacman just ate this Ghost, draw the point worth of
		// the ghost.
		if (m_nTicks2Popup > 0) {
			g2.setColor(Color.cyan);
			g2.setFont(m_gameModel.m_pacMan.m_gameUI.m_font);
			FontMetrics fm = g2.getFontMetrics();
			g2.drawString(Integer.toString(m_eatenPoints), ghostX, ghostY
					+ fm.getAscent());
			m_gameModel.m_pacMan.m_gameUI.m_bRedrawAll = true;
			return;
		}

		// Alter the Ghost's color if Pacman ate a Powerup
		if (!isEdible()) {
			g2.setColor(m_color);
		} else {
			// Check if the Powerup is almost out for this ghost,
			// if so, flash white.
			if (m_nTicks2Flee < 2000 / m_gameModel.m_pacMan.m_delay) {
				if ((m_nTicks2Flee % (200 / m_gameModel.m_pacMan.m_delay)) < (100 / m_gameModel.m_pacMan.m_delay)) {
					g2.setColor(Color.WHITE);
				} else {
					g2.setColor(Color.BLUE);
				}
			} else {
				g2.setColor(Color.BLUE);
			}
		}

		// If the ghost is eaten, then do not draw the body
		if (!m_bEaten) {
			g2.fillArc(ghostX, ghostY, ghostHeadDiameter, ghostHeadDiameter, 0,
					180);
			if (!m_bOtherPolygon) {
				polygon = new Polygon(m_ghostPolygon.xpoints,
						m_ghostPolygon.ypoints, m_ghostPolygon.npoints);
				polygon.translate(ghostX, ghostY);
			} else {
				polygon = new Polygon(m_ghostPolygon2.xpoints,
						m_ghostPolygon2.ypoints, m_ghostPolygon2.npoints);
				polygon.translate(ghostX, ghostY);
			}
			if ((m_gameModel.m_pacMan.m_globalTickCount % (m_ghostDeltaMax * 2)) == 0)
				m_bOtherPolygon = !m_bOtherPolygon;
			g2.fillPolygon(polygon);
		}

		// Draw Eyes
		double crossEyeDelta = 1;
		double ghostEyeWidth = ghostHeadDiameter / 2.7;
		double ghostEyeHeight = ghostHeadDiameter / 2.0;

		double ghostEyeX = 0;
		double ghostEyeY = 0;

		double ghostEyeDiameter = ghostHeadDiameter / 5.0;
		double ghostEyeBallX = 0;
		double ghostEyeBallY = 0;

		if (isEdible() && !m_bEaten) {
			crossEyeDelta = 2;
			ghostEyeX = ghostX + ghostHeadDiameter / 4.0 - ghostEyeWidth / 2.0;
			ghostEyeY = ghostY + ghostHeadDiameter / 7.0;
			ghostEyeBallX = ghostEyeX + ghostEyeWidth / 2.0 - ghostEyeDiameter
					/ 2.0;
			ghostEyeBallY = ghostEyeY + ghostEyeHeight / 2.0 - ghostEyeDiameter
					/ 2.0;

		} else if (m_direction == STILL) {
			// Look right for now
			/*
			 * ghostEyeX = ghostX + ghostHeadDiameter / 4 - ghostEyeWidth / 2;
			 * ghostEyeY = ghostY + ghostHeadDiameter / 5; ghostEyeBallX =
			 * ghostEyeX + ghostEyeWidth / 2 - ghostEyeDiameter / 2;
			 * ghostEyeBallY = ghostEyeY + ghostEyeHeight / 2 - ghostEyeDiameter
			 * / 2;
			 */
			ghostEyeX = ghostX + ghostHeadDiameter / 4.0 - ghostEyeWidth / 2.0;
			ghostEyeY = ghostY + ghostHeadDiameter / 5.0;
			ghostEyeBallX = ghostEyeX + ghostEyeWidth - ghostEyeDiameter;
			ghostEyeBallY = ghostEyeY + ghostEyeHeight / 2.0 - ghostEyeDiameter
					/ 2.0;

		} else if (m_direction == UP) {
			if (!m_bEaten)
				ghostEyeHeight = ghostHeadDiameter / 3.0;
			ghostEyeX = ghostX + ghostHeadDiameter / 4.0 - ghostEyeWidth / 2.0;
			ghostEyeY = ghostY + ghostHeadDiameter / 7.0;
			ghostEyeBallX = ghostEyeX + ghostEyeWidth / 2.0 - ghostEyeDiameter
					/ 2.0;
			ghostEyeBallY = ghostEyeY;

		} else if (m_direction == LEFT) {
			ghostEyeX = ghostX + ghostHeadDiameter / 4.0 - ghostEyeWidth / 2.0;
			ghostEyeY = ghostY + ghostHeadDiameter / 5.0;
			ghostEyeBallX = ghostEyeX;
			ghostEyeBallY = ghostEyeY + ghostEyeHeight / 2.0 - ghostEyeDiameter
					/ 2.0;

		} else if (m_direction == RIGHT) {
			ghostEyeX = ghostX + ghostHeadDiameter / 4.0 - ghostEyeWidth / 2.0;
			ghostEyeY = ghostY + ghostHeadDiameter / 5.0;
			ghostEyeBallX = ghostEyeX + ghostEyeWidth - ghostEyeDiameter;
			ghostEyeBallY = ghostEyeY + ghostEyeHeight / 2.0 - ghostEyeDiameter
					/ 2.0;
		} else if (m_direction == DOWN) {
			ghostEyeX = ghostX + ghostHeadDiameter / 4.0 - ghostEyeWidth / 2.0;
			ghostEyeY = ghostY + ghostHeadDiameter / 4.0;
			ghostEyeBallX = ghostEyeX + ghostEyeWidth / 2.0 - ghostEyeDiameter
					/ 2.0;
			ghostEyeBallY = ghostEyeY + ghostEyeHeight - ghostEyeDiameter;
		}

		// Draw the ghost eyes while it's chasing Pacman
		if (m_nTicks2Flee == 0 && !m_bEaten) {
			g2.setColor(Color.white);
			// Left Eye
			g2.fillOval((int) (ghostEyeX + crossEyeDelta), (int) (ghostEyeY),
					(int) (ghostEyeWidth), (int) ghostEyeHeight);
			// Right Eye
			g2
					.fillOval(
							(int) (ghostEyeX + ghostHeadDiameter / 2.0 - crossEyeDelta),
							(int) (ghostEyeY), (int) (ghostEyeWidth),
							(int) ghostEyeHeight);

			if (m_bInsaneAI || m_bChaseMode)
				g2.setColor(Color.red);
			else
				g2.setColor(Color.blue);

			// Left Eye Ball
			g2.fillRoundRect((int) (ghostEyeBallX + crossEyeDelta),
					(int) (ghostEyeBallY), (int) (ghostEyeDiameter),
					(int) (ghostEyeDiameter), (int) (ghostEyeDiameter),
					(int) (ghostEyeDiameter));
			// Right Eye Ball
			g2
					.fillRoundRect((int) (ghostEyeBallX + ghostHeadDiameter
							/ 2.0 - crossEyeDelta), (int) (ghostEyeBallY),
							(int) (ghostEyeDiameter), (int) (ghostEyeDiameter),
							(int) (ghostEyeDiameter), (int) (ghostEyeDiameter));

		} else if (isEdible() && !m_bEaten) {
			// Draw the ghost running away
			g2.setColor(Color.lightGray);
			// Left Eye Ball
			g2.fillRoundRect((int) (ghostEyeBallX + crossEyeDelta),
					(int) (ghostEyeBallY), (int) (ghostEyeDiameter),
					(int) (ghostEyeDiameter), (int) (ghostEyeDiameter),
					(int) (ghostEyeDiameter));
			// Right Eye Ball
			g2
					.fillRoundRect((int) (ghostEyeBallX + ghostHeadDiameter
							/ 2.0 - crossEyeDelta), (int) (ghostEyeBallY),
							(int) (ghostEyeDiameter), (int) (ghostEyeDiameter),
							(int) (ghostEyeDiameter), (int) (ghostEyeDiameter));
			// Draw Crooked Grin
			for (int i = 0; i < m_ghostMouthX.length - 1; i++) {
				g2.drawLine((ghostX + m_ghostMouthX[i]),
						(ghostY + m_ghostMouthY[i]),
						(ghostX + m_ghostMouthX[i + 1]),
						(ghostY + m_ghostMouthY[i + 1]));
				g2.drawLine((ghostX + m_ghostMouthX[i] - 1),
						(ghostY + m_ghostMouthY[i]), (ghostX
								+ m_ghostMouthX[i + 1] - 1),
						(ghostY + m_ghostMouthY[i + 1]));
			}

		} else {
			// Draw the eaten ghost returning to hideout.
			g2.setColor(Color.lightGray);
			// Left Eye
			g2.fillOval((int) (ghostEyeX + crossEyeDelta), (int) (ghostEyeY),
					(int) (ghostEyeWidth), (int) ghostEyeHeight);
			// Right Eye
			g2
					.fillOval(
							(int) (ghostEyeX + ghostHeadDiameter / 2.0 - crossEyeDelta),
							(int) (ghostEyeY), (int) (ghostEyeWidth),
							(int) ghostEyeHeight);

			// Left Eye Ball
			g2.setColor(Color.blue);
			g2.fillRoundRect((int) (ghostEyeBallX + crossEyeDelta),
					(int) (ghostEyeBallY), (int) (ghostEyeDiameter),
					(int) (ghostEyeDiameter), (int) (ghostEyeDiameter),
					(int) (ghostEyeDiameter));
			// Right Eye Ball
			g2
					.fillRoundRect((int) (ghostEyeBallX + ghostHeadDiameter
							/ 2.0 - crossEyeDelta), (int) (ghostEyeBallY),
							(int) (ghostEyeDiameter), (int) (ghostEyeDiameter),
							(int) (ghostEyeDiameter), (int) (ghostEyeDiameter));

		}

		m_boundingBox.setBounds((ghostX), (ghostY), ghostHeadDiameter,
				ghostHeadDiameter);
		m_boundingBox.grow(-ghostHeadDiameter / 4, -ghostHeadDiameter / 4);
	}

	// Overriden to update Ghost's directions
	@Override
	public void tickThing(GameUI gameUI) {
		super.tickThing(gameUI);
		boolean bBackoff = false;
		// Don't let the ghost go back the way it came.
		byte prevDirection = STILL;

		// Count down for how long the Points for eating the Ghost popup
		if (m_nTicks2Popup > 0) {
			m_nTicks2Popup--;
			if (m_nTicks2Popup == 0) {
				m_gameModel.setPausedGame(false);
				m_gameModel.m_player.setVisible(true);
				// m_gameModel.m_pacMan.m_soundMgr.playSound
				// (SoundManager.SOUND_RETURNGHOST);
			}
		}

		// Count down until Ghost can leave Hideout
		if (m_nTicks2Exit > 0) {
			m_nTicks2Exit--;
			if (m_nTicks2Exit == 0) {
				m_destinationX = -1;
				m_destinationY = -1;
			}
		}

		// Count down until the powerup expires
		if (isEdible()) {
			m_nTicks2Flee--;
			if (m_nTicks2Flee == 0 && !m_bEaten) {
				m_deltaMax = m_ghostDeltaMax;
				m_bEaten = false;
				m_destinationX = -1;
				m_destinationY = -1;
			}

			if ((m_nTicks2Flee > 0)
					&& (m_nTicks2Flee < 2000 / m_gameModel.m_pacMan.m_delay)) {
				flashing_ = true;
			} else {
				flashing_ = false;
			}
		} else
			flashing_ = false;

		// If the ghost is located at the door and is ready to enter because
		// he was eaten, then let him in.
		if (m_bEaten && m_locX == m_gameModel.m_doorLocX
				&& m_locY == (m_gameModel.m_doorLocY - 1) && m_deltaLocX == 0
				&& m_deltaLocY == 0) {
			m_destinationX = m_gameModel.m_doorLocX;
			m_destinationY = m_gameModel.m_doorLocY + 2;
			m_direction = DOWN;
			m_deltaLocY = 1;
			m_bInsideRoom = true;
			m_nTicks2Flee = 0;
			m_bEnteringDoor = true;
			m_deltaMax = m_ghostDeltaMax;
			return;
		}

		// If the ghost has entered the room and was just eaten,
		// reset it so it can wander in the room a bit before coming out
		if (m_bEaten && m_locX == m_gameModel.m_doorLocX
				&& m_locY == (m_gameModel.m_doorLocY + 2) && m_deltaLocX == 0
				&& m_deltaLocY == 0) {
			m_destinationX = -1;
			m_destinationY = -1;
			m_direction = STILL;
			m_nTicks2Exit = 3000 / m_gameModel.m_pacMan.m_delay;
			m_bEnteringDoor = false;
			m_bEaten = false;
			return;
		}

		// If the ghost was just eaten and is returning to the hideout,
		// if during this time Pacman eats another powerup, we need
		// to set the destinationX and Y back so that the ghost will continue
		// to enter the room and not get stuck
		if (m_bEnteringDoor) {
			m_destinationX = m_gameModel.m_doorLocX;
			m_destinationY = m_gameModel.m_doorLocY + 2;
			m_direction = DOWN;
		}

		// If the ghost is located at the door and is ready to leave,
		// then let him out.
		if (m_bInsideRoom && m_locX == m_gameModel.m_doorLocX
				&& m_locY == m_gameModel.m_doorLocY + 2 && m_deltaLocX == 0
				&& m_deltaLocY == 0 && m_nTicks2Exit == 0) {
			m_destinationX = m_locX;
			m_destinationY = m_gameModel.m_doorLocY - 1;
			m_direction = UP;
			m_deltaLocY = -1;
			m_bInsideRoom = false;
			m_bEnteringDoor = false;
			m_bEaten = false;
			return;
		}

		// A ghost will back off only if:
		// 1. It's not waiting to leave the room.
		// 2. It's not entering the door.
		// 3. It's not eaten.
		// 4. It's not leaving the room.
		// 5. Time to backoff is here.
		// 6. Insane AI is off
		if (m_gameModel.m_state == GameModel.STATE_PLAYING
				&& m_bInsideRoom == false
				&& m_bEnteringDoor == false
				&& m_bEaten == false
				&& (m_destinationX != m_gameModel.m_doorLocX && m_destinationY != m_gameModel.m_doorLocY - 1)
				&& (m_bChaseMode != m_bOldChaseMode) && m_bInsaneAI == false) {
			m_destinationX = -1;
			m_destinationY = -1;
			bBackoff = true;
		}

		// If there is a destination, then check if the destination has been
		// reached.
		if (m_destinationX >= 0 && m_destinationY >= 0) {
			// Check if the destination has been reached, if so, then
			// get new destination.
			if (m_destinationX == m_locX && m_destinationY == m_locY
					&& m_deltaLocX == 0 && m_deltaLocY == 0) {
				m_destinationX = -1;
				m_destinationY = -1;
				prevDirection = m_direction;
			} else {
				// Otherwise, we haven't reached the destination so
				// continue in same direction.
				return;
			}
		}

		// Reset the previous direction to allow backtracking
		if (bBackoff || (!m_bEaten && m_bCanBackTrack))
			prevDirection = STILL;

		// Get the next direction of the ghost.
		// This is where different AIs can be plugged.
		setNextDirection(prevDirection, bBackoff);
		m_bOldChaseMode = m_bChaseMode;
	}

	void setNextDirection(byte prevDirection, boolean bBackoff) {
		int deltaX, deltaY;
		Point target;
		Point nextLocation = new Point();
		byte[] bestDirection = new byte[4];

		target = setTarget();

		deltaX = m_locX - target.x;
		deltaY = m_locY - target.y;

		if (Math.abs(deltaX) > Math.abs(deltaY)) {
			if (deltaX > 0) {
				bestDirection[0] = LEFT;
				bestDirection[3] = RIGHT;
				if (deltaY > 0) {
					bestDirection[1] = UP;
					bestDirection[2] = DOWN;
				} else {
					bestDirection[1] = DOWN;
					bestDirection[2] = UP;
				}
			} else {
				bestDirection[0] = RIGHT;
				bestDirection[3] = LEFT;
				if (deltaY > 0) {
					bestDirection[1] = UP;
					bestDirection[2] = DOWN;
				} else {
					bestDirection[1] = DOWN;
					bestDirection[2] = UP;
				}
			}
		} else {
			if (deltaY > 0) {
				bestDirection[0] = UP;
				bestDirection[3] = DOWN;
				if (deltaX > 0) {
					bestDirection[1] = LEFT;
					bestDirection[2] = RIGHT;
				} else {
					bestDirection[1] = RIGHT;
					bestDirection[2] = LEFT;
				}

			} else {
				bestDirection[0] = DOWN;
				bestDirection[3] = UP;
				if (deltaX > 0) {
					bestDirection[1] = LEFT;
					bestDirection[2] = RIGHT;
				} else {
					bestDirection[1] = RIGHT;
					bestDirection[2] = LEFT;
				}
			}
		}

		// There's a 20% chance that the ghost will try the sub-optimal
		// direction first.
		// This will keep the ghosts from following each other and to trap
		// Pacman.
		if (!m_bInsaneAI && m_bCanUseNextBest && random_.nextDouble() < .2) {
			ArrayList<Byte> directions = new ArrayList<Byte>();
			directions.add(UP);
			directions.add(DOWN);
			directions.add(RIGHT);
			directions.add(LEFT);
			bestDirection[0] = directions
					.remove((int) (random_.nextDouble() * directions.size()));
			bestDirection[1] = directions
					.remove((int) (random_.nextDouble() * directions.size()));
			bestDirection[2] = directions
					.remove((int) (random_.nextDouble() * directions.size()));
			bestDirection[3] = directions
					.remove((int) (random_.nextDouble() * directions.size()));
		}

		// If the ghost is fleeing and not eaten, then reverse the array of best
		// directions to go.
		if (bBackoff || (isEdible() && !m_bEaten)) {
			byte temp = bestDirection[0];
			bestDirection[0] = bestDirection[3];
			bestDirection[3] = temp;

			temp = bestDirection[1];
			bestDirection[1] = bestDirection[2];
			bestDirection[2] = temp;
		}

		for (int i = 0; i < 4; i++) {
			if (bestDirection[i] == UP
					&& (m_gameModel.m_gameState[m_locX][m_locY] & GameModel.GS_NORTH) == 0
					&& m_deltaLocX == 0 && prevDirection != DOWN) {
				if (!getDestination(UP, m_locX, m_locY, nextLocation,
						m_gameModel))
					continue;
				m_destinationX = nextLocation.x;
				m_destinationY = nextLocation.y;
				m_direction = UP;
				if (m_bCanFollow || !isFollowing())
					break;

			} else if (bestDirection[i] == DOWN
					&& (m_gameModel.m_gameState[m_locX][m_locY] & GameModel.GS_SOUTH) == 0
					&& m_deltaLocX == 0 && prevDirection != UP) {
				if (!getDestination(DOWN, m_locX, m_locY, nextLocation,
						m_gameModel))
					continue;
				m_destinationX = nextLocation.x;
				m_destinationY = nextLocation.y;
				m_direction = DOWN;
				if (m_bCanFollow || !isFollowing())
					break;

			} else if (bestDirection[i] == RIGHT
					&& (m_gameModel.m_gameState[m_locX][m_locY] & GameModel.GS_EAST) == 0
					&& m_deltaLocY == 0 && prevDirection != LEFT)

			{
				if (!getDestination(RIGHT, m_locX, m_locY, nextLocation,
						m_gameModel))
					continue;
				m_destinationX = nextLocation.x;
				m_destinationY = nextLocation.y;
				m_direction = RIGHT;
				if (m_bCanFollow || !isFollowing())
					break;

			} else if (bestDirection[i] == LEFT
					&& (m_gameModel.m_gameState[m_locX][m_locY] & GameModel.GS_WEST) == 0
					&& m_deltaLocY == 0 && prevDirection != RIGHT) {
				if (!getDestination(LEFT, m_locX, m_locY, nextLocation,
						m_gameModel))
					continue;
				m_destinationX = nextLocation.x;
				m_destinationY = nextLocation.y;
				m_direction = LEFT;
				if (m_bCanFollow || !isFollowing())
					break;

			}
		}
	}

	/**
	 * Sets the ghost's target, depending on what type of ghost it is.
	 * 
	 * @return The target location for the ghost to move towards.
	 */
	private Point setTarget() {
		int targetX = 0;
		int targetY = 0;

		// If the ghost is inside the room, he needs to move to the door to get
		// out.
		if (m_bInsideRoom) {
			targetX = m_gameModel.m_doorLocX;
			targetY = m_gameModel.m_doorLocY;
		} else if (m_bEaten) {
			// If the ghost is eaten, it needs to return to the hideout.
			targetX = m_gameModel.m_doorLocX;
			targetY = m_gameModel.m_doorLocY - 1;

		} else {
			// Otherwise, he is outside the door and chasing Pacman
			if (!m_bInsaneAI) {
				// If chasing, go for Pacman
				if (m_bChaseMode) {
					// Not insanely chasing Pacman, using regular behaviour
					switch (m_type) {
					case BLINKY:
						// Blinky simply goes for Pacman's current location
						targetX = m_gameModel.m_player.m_locX;
						targetY = m_gameModel.m_player.m_locY;
						break;
					case PINKY:
					case INKY:
						// Pinky goes for the location 4 tiles away from
						// Pacman's
						// location in the direction of Pacman.

						// Inky sort of uses the same logic as Pinky for the
						// first
						// part of it's calculation.

						int offset = 4;
						if (m_type == INKY)
							offset = 2;

						// Set the target a certain offset from Pacman
						targetX = m_gameModel.m_player.m_locX;
						targetY = m_gameModel.m_player.m_locY;
						switch (m_gameModel.m_player.m_direction) {
						case Thing.UP:
							targetY -= offset;
							break;
						case Thing.DOWN:
							targetY += offset;
							break;
						case Thing.LEFT:
							targetX -= offset;
							break;
						case Thing.RIGHT:
							targetX += offset;
							break;
						}

						// If we're looking at Pinky, break here
						if (m_type == PINKY)
							break;

						// Continue calculations for Inky
						int blinkyPosX = m_gameModel.m_ghosts[BLINKY].m_locX;
						int blinkyPosY = m_gameModel.m_ghosts[BLINKY].m_locY;
						targetX = (2 * targetX) - blinkyPosX;
						targetY = (2 * targetY) - blinkyPosY;
						break;
					case CLYDE:
						// Clyde's behaviour is greedy when distant, but
						// ignorant
						// when close to Pacman.

						// Calculate the distance Clyde is from Pacman
						double distance = Point2D.distance(
								m_gameModel.m_player.m_locX,
								m_gameModel.m_player.m_locY, m_locX, m_locY);
						// If distant, go directly for Pacman
						if (distance > 8) {
							targetX = m_gameModel.m_player.m_locX;
							targetY = m_gameModel.m_player.m_locY;
						} else {
							targetX = m_cornerX;
							targetY = m_cornerY;
						}
					}
				} else {
					targetX = m_cornerX;
					targetY = m_cornerY;
				}
			} else {
				// Get Pacman's location and use that as the target.
				targetX = m_gameModel.m_player.m_locX;
				targetY = m_gameModel.m_player.m_locY;
			}

			// Overriden to chase target if there is one
			if ((m_targetX >= 0) && (m_targetX >= 0)) {
				targetX = m_targetX;
				targetY = m_targetY;
			}
		}

		// Modulo the x coordinate for wrapping
		if ((targetX >= m_gameModel.m_gameSizeX) || (targetX < 0))
			targetX = (targetX + m_gameModel.m_gameSizeX)
					% m_gameModel.m_gameSizeX;

		return new Point(targetX, targetY);
	}

	// This method returns true if this ghost is traveling to the same
	// destination with the same direction as another ghost.
	boolean isFollowing() {
		boolean bFollowing = false;
		double dRandom;

		// If the ghost is in the same location as another ghost
		// and moving in the same direction, then they are on
		// top of each other and should not follow.
		for (int i = 0; i < m_gameModel.m_ghosts.length; i++) {
			// Ignore myself
			if (this == m_gameModel.m_ghosts[i])
				continue;

			if (m_gameModel.m_ghosts[i].m_locX == m_locX
					&& m_gameModel.m_ghosts[i].m_locY == m_locY
					&& m_gameModel.m_ghosts[i].m_direction == m_direction) {
				return true;
			}
		}

		// This will allow ghosts to often
		// clump together for easier eating
		dRandom = random_.nextDouble();
		if (!m_bInsaneAI && dRandom < .90) {
			// if (m_bInsaneAI && dRandom < .25)
			// return false;
			// else
			return false;
		}

		// If ghost is moving to the same location and using the
		// same direction, then it is following another ghost.
		for (int i = 0; i < m_gameModel.m_ghosts.length; i++) {
			// Ignore myself
			if (this == m_gameModel.m_ghosts[i])
				continue;

			if (m_gameModel.m_ghosts[i].m_destinationX == m_destinationX
					&& m_gameModel.m_ghosts[i].m_destinationY == m_destinationY
					&& m_gameModel.m_ghosts[i].m_direction == m_direction) {
				bFollowing = true;
				break;
			}
		}

		return bFollowing;
	}

	// This method will check if the bounding box of this ghosts intersects with
	// the bound box of the player. If so, then either kill the player or eat
	// the
	// fleeing ghost
	// return: 0 for no collision, 1 for ate a ghost, 2 for pacman died
	@Override
	public int checkCollision(Player player) {
		Rectangle intersectRect;
		intersectRect = m_boundingBox.intersection(player.m_boundingBox);
		if (!intersectRect.isEmpty()) {
			// If the ghost is not fleeing and is not eaten,
			// then Pacman was caught.
			if (m_nTicks2Flee == 0 && !m_bEaten) {
				player.m_direction = Thing.STILL;
				return 2;

			} else if (isEdible() && !m_bEaten) {
				// If the ghost was fleeing and is not eaten,
				// then Pacman caught the Ghost.
				player.m_score += m_gameModel.m_eatGhostPoints;
				m_eatenPoints = m_gameModel.m_eatGhostPoints;
				m_gameModel.m_eatGhostPoints *= 2;
				m_bEaten = true;
				m_destinationX = -1;
				m_destinationY = -1;
				// Boost speed of dead ghost
				// to make the eyes get back to the hideout faster
				m_deltaMax = 2;
				// Pause the game to display the points for eating this ghost.
				m_gameModel.setPausedGame(true);
				m_nTicks2Popup = 500 / m_gameModel.m_pacMan.m_delay;
				player.setVisible(false);
				return 1;
			}
		}
		return 0;

	}

	public boolean isEdible() {
		return m_nTicks2Flee > 0;
	}

	// This is called each time the game is restarted
	@Override
	public void returnToStart() {
		super.returnToStart();
		m_destinationX = -1;
		m_destinationY = -1;
		// First ghost always starts outside of room
		if (m_gameModel.m_ghosts[0] == this)
			m_bInsideRoom = false;
		else
			m_bInsideRoom = true;

		m_nTicks2Exit = m_nExitMilliSec / m_gameModel.m_pacMan.m_delay;
		m_deltaMax = m_ghostDeltaMax;
		m_nTicks2Flee = 0;
		m_bEaten = false;
		m_nTicks2Popup = 0;
		m_bEnteringDoor = false;
	}

	/**
	 * Checks if this ghost is flashing.
	 * 
	 * @return The state of the ghost's flashing.
	 */
	public boolean isBlinking() {
		return flashing_;
	}

	@Override
	protected void updatePixelVals(GameUI gameUI) {
		pixelSize_ = gameUI.CELL_LENGTH + gameUI.WALL1 + gameUI.WALL1;
		pixelX_ = gameUI.m_gridInset
				+ (int) (m_locX * gameUI.CELL_LENGTH - pixelSize_ / 2.0
						+ gameUI.CELL_LENGTH / 2.0 + m_deltaLocX
						* (gameUI.CELL_LENGTH / (m_deltaMax * 2.0 - 1)));
		pixelY_ = gameUI.m_gridInset
				+ (int) (m_locY * gameUI.CELL_LENGTH - pixelSize_ / 2.0
						+ gameUI.CELL_LENGTH / 2.0 + m_deltaLocY
						* (gameUI.CELL_LENGTH / (m_deltaMax * 2.0 - 1)));
		pixelShrink_ = -pixelSize_ / 4;
	}

	@Override
	public String getObjectName() {
		return "ghost";
	}

	@Override
	public String toString() {
		String ghostString = null;
		switch (m_type) {
		case BLINKY:
			ghostString = "blinky";
			break;
		case PINKY:
			ghostString = "pinky";
			break;
		case INKY:
			ghostString = "inky";
			break;
		case CLYDE:
			ghostString = "clyde";
			break;
		}
		return ghostString;
	}
}
