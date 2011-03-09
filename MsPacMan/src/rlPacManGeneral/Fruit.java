package rlPacManGeneral;

import java.awt.*;
import java.util.Random;

class Fruit extends Thing {
	Color m_color;
	int m_destinationX;
	int m_destinationY;
	boolean m_bAvailable; // FALSE if the fruit has been eaten for this round
	int m_nTicks2Show;
	int m_nTicks2Hide;
	int m_bounceCount;
	int m_nTicks2Popup;
	int m_eatenPoints;

	boolean m_bounceUp = true;
	double m_bounceY = 0;

	Fruit(GameModel gameModel, byte type, int startX, int startY,
			boolean bMiddleX, Random random) {
		super(gameModel, startX, startY, bMiddleX);
		m_deltaMax = 5;
		random_ = random;
	}

	// Overriden to draw Fruit
	@Override
	public void draw(GameUI gameUI, Graphics g2) {
		double fruitX = gameUI.m_gridInset + m_locX * gameUI.CELL_LENGTH;
		double fruitY = gameUI.m_gridInset + m_locY * gameUI.CELL_LENGTH;

		if (m_deltaLocX != 0)
			fruitX += m_deltaLocX
					* (gameUI.CELL_LENGTH / (m_deltaMax * 2.0 - 1));
		else if (m_deltaLocY != 0)
			fruitY += m_deltaLocY
					* (gameUI.CELL_LENGTH / (m_deltaMax * 2.0 - 1));

		// If Pacman just ate this Fruit, draw the point worth of
		// the fruit.
		if (m_nTicks2Popup > 0) {
			g2.setColor(Color.cyan);
			g2.setFont(m_gameModel.m_pacMan.m_gameUI.m_font);
			FontMetrics fm = g2.getFontMetrics();
			g2.drawString(Integer.toString(m_eatenPoints), (int) fruitX,
					(int) fruitY + fm.getAscent());
			m_gameModel.m_pacMan.m_gameUI.m_bRedrawAll = true;
			return;
		}

		if (!m_bVisible || !m_bAvailable)
			return;

		fruitY += Math.sin(0.017 * m_gameModel.m_pacMan.m_globalTickCount * 25)
				* gameUI.WALL1;

		g2.setColor(new Color(254, 184, 4));
		g2.fillOval((int) (fruitX), (int) (fruitY), gameUI.CELL_LENGTH,
				gameUI.CELL_LENGTH);
		g2.setColor(new Color(28, 232, 4));
		g2.fillRect((int) (fruitX), (int) (fruitY), gameUI.WALL1, gameUI.WALL1);
		g2.fillRect((int) (fruitX + gameUI.WALL1),
				(int) (fruitY + gameUI.WALL1), gameUI.WALL1, gameUI.WALL1);

		/*
		 * g2.setColor (new Color (0, 255, 0)); g2.fillArc ((int)(fruitX -
		 * gameUI.WALL1), (int)(fruitY - gameUI.WALL1), gameUI.CELL_LENGTH +
		 * gameUI.WALL1 + gameUI.WALL1, gameUI.CELL_LENGTH, 10, -210);
		 * g2.setColor (Color.red); g2.fillArc ((int)(fruitX), (int)(fruitY -
		 * 1), gameUI.CELL_LENGTH, gameUI.WALL3, 10, -210);
		 */

		m_boundingBox.setBounds((int) (fruitX), (int) (fruitY),
				gameUI.CELL_LENGTH, gameUI.CELL_LENGTH);

		// g2.setColor (Color.white);
		// g2.drawRect (m_boundingBox.x, m_boundingBox.y, m_boundingBox.width,
		// m_boundingBox.height);
	}

	@Override
	public void tickThing(GameUI gameUI) {
		super.tickThing(gameUI);
		// Count down for how long the Points for eating the Fruit popup
		if (m_nTicks2Popup > 0) {
			m_nTicks2Popup--;
			if (m_nTicks2Popup == 0) {
				m_gameModel.setPausedGame(false);
				m_gameModel.m_player.setVisible(true);
			}
		}

		byte prevDirection = STILL;
		Point nextLocation = new Point();

		// Count down until Fruit can appear in game
		if (m_nTicks2Show > 0) {
			m_nTicks2Show--;
			if (m_nTicks2Show == 0) {
				returnToStart();
				m_bVisible = true;
				m_nTicks2Hide = 15000 / m_gameModel.m_pacMan.m_delay;
			} else {
				m_bVisible = false;
				return;
			}
		}

		// Count down until fruit disappears from board
		if (m_nTicks2Show == 0 && m_nTicks2Hide > 0) {
			m_nTicks2Hide--;
			if (m_nTicks2Hide == 0) {
				m_bVisible = false;
				m_nTicks2Show = 10000 / m_gameModel.m_pacMan.m_delay;
				// returnToStart ();
				return;
			}
		}

		if (m_bAvailable == false)
			return;

		if (m_destinationX >= 0 && m_destinationY >= 0) {
			// Check if the destination has been reached, if so, then
			// get new destination.
			if (m_destinationX == m_locX && m_destinationY == m_locY
					&& m_deltaLocX == 0 && m_deltaLocY == 0) {
				m_destinationX = -1;
				m_destinationY = -1;
				prevDirection = m_direction;
			} else {
				// Otherwise, we haven't reached the destionation so
				// continue in same direction.
				return;
			}
		}

		m_direction = STILL;

		// Simple algorithm that keeps picking random directions until one
		// is acceptable for the fruit to travel in.
		while (true) {
			double randomNum = random_.nextDouble();
			if (randomNum < .25
					&& prevDirection != DOWN
					&& (m_gameModel.m_gameState[m_locX][m_locY] & GameModel.GS_NORTH) == 0) {
				if (!getDestination(UP, m_locX, m_locY, nextLocation,
						m_gameModel))
					continue;
				m_destinationX = nextLocation.x;
				m_destinationY = nextLocation.y;
				m_direction = UP;
				break;

			} else if (randomNum < .50
					&& prevDirection != LEFT
					&& (m_gameModel.m_gameState[m_locX][m_locY] & GameModel.GS_EAST) == 0) {
				if (!getDestination(RIGHT, m_locX, m_locY, nextLocation,
						m_gameModel))
					continue;
				m_destinationX = nextLocation.x;
				m_destinationY = nextLocation.y;
				m_direction = RIGHT;
				break;

			} else if (randomNum < .75
					&& prevDirection != UP
					&& (m_gameModel.m_gameState[m_locX][m_locY] & GameModel.GS_SOUTH) == 0) {
				if (!getDestination(DOWN, m_locX, m_locY, nextLocation,
						m_gameModel))
					continue;
				m_destinationX = nextLocation.x;
				m_destinationY = nextLocation.y;
				m_direction = DOWN;
				break;

			} else if (randomNum < 1.0
					&& prevDirection != RIGHT
					&& (m_gameModel.m_gameState[m_locX][m_locY] & GameModel.GS_WEST) == 0) {
				if (!getDestination(LEFT, m_locX, m_locY, nextLocation,
						m_gameModel))
					continue;
				m_destinationX = nextLocation.x;
				m_destinationY = nextLocation.y;
				m_direction = LEFT;
				break;
			}
		}
	}

	@Override
	public void returnToStart() {
		super.returnToStart();
		m_destinationX = -1;
		m_destinationY = -1;
		m_bVisible = false;
		// m_nTicks2Show += 5000 / m_gameModel.m_pacMan.m_delay;
	}

	// This method will check if the bounding box of the fruit intersects with
	// the bound box of the player. If so, then player eats the fruit for points
	// return: 0 for no collision, 3 for ate the fruit
	@Override
	public int checkCollision(Player player) {
		if (!m_bAvailable || !m_bVisible)
			return 0;

		Rectangle intersectRect;
		intersectRect = m_boundingBox.intersection(player.m_boundingBox);
		if (!intersectRect.isEmpty()) {
			// If the ghost was fleeing and is not eaten,
			// then Pacman caught the Ghost.
			m_eatenPoints = m_gameModel.m_stage * 750;
			// Cap fruit worth to 7500 points
			if (m_eatenPoints > 7500)
				m_eatenPoints = 7500;
			player.m_score += m_eatenPoints;
			m_destinationX = -1;
			m_destinationY = -1;
			setVisible(false);
			m_bAvailable = false;

			// Pause the game to display the points for eating the fruit.
			m_gameModel.setPausedGame(true);
			m_nTicks2Popup = 500 / m_gameModel.m_pacMan.m_delay;
			player.setVisible(false);

			return 3;
		}
		return 0;
	}

	@Override
	protected void updatePixelVals(GameUI gameUI) {
		double fruitX = gameUI.m_gridInset + m_locX * gameUI.CELL_LENGTH;
		double fruitY = gameUI.m_gridInset + m_locY * gameUI.CELL_LENGTH;

		if (m_deltaLocX != 0)
			fruitX += m_deltaLocX
					* (gameUI.CELL_LENGTH / (m_deltaMax * 2.0 - 1));
		else if (m_deltaLocY != 0)
			fruitY += m_deltaLocY
					* (gameUI.CELL_LENGTH / (m_deltaMax * 2.0 - 1));

		pixelX_ = (int) fruitX;
		pixelY_ = (int) fruitY;
		pixelSize_ = gameUI.CELL_LENGTH;
		pixelShrink_ = 0;
	}
	
	@Override
	public String getObjectName() {
		return "fruit";
	}
	
	@Override
	public String toString() {
		return "fruit";
	}
	
	public boolean isEdible() {
		if (m_bAvailable && m_bVisible && m_nTicks2Hide > 0) {
			return true;
		}
		return false;
	}
}
