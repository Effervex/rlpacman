package rlPacMan;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URL;

import javax.swing.JFrame;

import org.rlcommunity.rlglue.codec.*;

public class PacMan {
	private JFrame gui_;
	protected GameModel m_gameModel;
	protected TopCanvas m_topCanvas;
	protected BottomCanvas m_bottomCanvas;
	protected GameUI m_gameUI;
	protected Ticker m_ticker; // Used to update the game state and UI
	// protected SoundManager m_soundMgr;
	protected int m_globalTickCount = 0;

	protected int m_ticksPerSec; // These two variables control game speed
	protected int m_delay; // Milliseconds between ticks

	/** The agent interfacing with this program. */
	protected AgentInterface agent_;
	protected PacManKeyAdapter keyAdapter_;

	public boolean experimentMode_ = false;

	public void init() {
		setTicksPerSec(35);

		// Create canvases and layout
		m_gameModel = new GameModel(this);
		m_gameUI = new GameUI(this, m_gameModel, 409, 450);
		m_topCanvas = new TopCanvas(m_gameModel, 400, 200);
		m_bottomCanvas = new BottomCanvas(this, m_gameModel, 400, 250);

		keyAdapter_ = new PacManKeyAdapter(this);
		if (!experimentMode_) {
			gui_ = new JFrame("Reinforcement Learning Ms. PacMan");
			
			GridBagLayout gridBag = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();

			gui_.setLayout(gridBag);

			c.gridwidth = 1;
			c.gridheight = 3;

			gridBag.setConstraints(m_gameUI, c);
			gui_.add(m_gameUI);

			c.gridwidth = GridBagConstraints.REMAINDER;
			c.gridheight = 1;
			gridBag.setConstraints(m_topCanvas, c);
			gui_.add(m_topCanvas);

			gridBag.setConstraints(m_bottomCanvas, c);
			gui_.add(m_bottomCanvas);

			gui_.requestFocus();
			// Add event subscribers
			gui_.addKeyListener(keyAdapter_);

			gui_.validate();

			gui_.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			gui_.pack();
			gui_.setVisible(true);
		}
	}

	public void reinit() {
		m_gameModel = new GameModel(this);
		m_gameUI.m_gameModel = m_gameModel;
		m_topCanvas.m_gameModel = m_gameModel;
		m_bottomCanvas.m_gameModel = m_gameModel;

		m_globalTickCount = 0;
	}

	public URL getCodeBase() {
		File here = new File(".");
		try {
			return here.toURI().toURL();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Gets the JFrame this object uses.
	 * 
	 * @return The JFram of the PacMan object.
	 */
	public JFrame getFrame() {
		return gui_;
	}

	// Master ticker that runs various parts of the game
	// based on the GameModel's STATE
	public void tick(boolean redraw) {
		// long temp = System.currentTimeMillis ();
		m_globalTickCount++;

		if (m_gameModel.m_state == GameModel.STATE_ABOUT) {
			// m_soundMgr.stop();
			m_gameModel.m_bIntroInited = false;
			m_gameUI.m_bShowIntro = false;
			m_gameUI.m_bShowAbout = true;
			m_gameUI.m_bRedrawAll = true;
			m_gameModel.m_nTicks2AboutShow++;
			if (m_gameModel.m_nTicks2AboutShow == 15000 / m_delay) {
				m_gameModel.m_state = GameModel.STATE_INTRO;
				m_gameModel.m_nTicks2AboutShow = 0;
			}

		} else if (m_gameModel.m_state == GameModel.STATE_INTRO) {
			tickIntro();
			m_gameUI.m_bShowIntro = true;

		} else if (m_gameModel.m_state == GameModel.STATE_PAUSED) {
			m_gameUI.m_bDrawPaused = true;
			m_gameUI.m_bRedrawAll = true;
			m_gameUI.repaint();
			return;

		} else if (m_gameModel.m_state == GameModel.STATE_NEWGAME) {
			// m_soundMgr.stop();
			m_gameModel.newGame();
			m_gameModel.m_state = GameModel.STATE_BEGIN_PLAY;
			m_gameModel.m_nTicks2BeginPlay = 0;
			m_gameModel.m_bIntroInited = false;
			m_gameUI.m_bShowIntro = false;
			m_gameUI.m_bShowAbout = false;
			m_gameUI.m_bRedrawAll = true;

		} else if (m_gameModel.m_state == GameModel.STATE_GAMEOVER) {
			if (m_gameModel.m_nTicks2GameOver == 0) {
				if (m_gameModel.m_player.m_score > m_gameModel.m_highScore) {
					m_gameModel.m_highScore = m_gameModel.m_player.m_score;
					m_topCanvas.repaint();
				}
			}

			m_gameModel.m_nTicks2GameOver++;

			// After 3 seconds go to the intro page
			if (m_gameModel.m_nTicks2GameOver == 3000 / m_delay) {
				m_gameModel.m_state = GameModel.STATE_INTRO;
				m_gameModel.m_nTicks2GameOver = 0;
			}

			m_gameUI.m_bDrawGameOver = true;
			m_gameUI.m_bRedrawAll = true;
			m_gameUI.repaint();
			return;

		} else if (m_gameModel.m_state == GameModel.STATE_LEVELCOMPLETE) {
			// m_soundMgr.stop();
			tickLevelComplete();

		} else if (m_gameModel.m_state == GameModel.STATE_DEADPACMAN) {
			// m_soundMgr.stop();
			if (m_gameModel.m_nLives == 0) {
				m_gameModel.m_state = GameModel.STATE_GAMEOVER;
				m_gameModel.m_nTicks2GameOver = 0;

			} else {
				m_gameModel.restartGame();
				m_gameModel.m_state = GameModel.STATE_BEGIN_PLAY;
				m_bottomCanvas.repaint();

			}

		} else if (m_gameModel.m_state == GameModel.STATE_BEGIN_PLAY) {
			tickBeginPlay();

		} else if (m_gameModel.m_state == GameModel.STATE_PLAYING) {
			tickGamePlay();
		} else if (m_gameModel.m_state == GameModel.STATE_DEAD_PLAY) {
			tickDeadPlay();
		}

		if (redraw) {
			m_gameUI.repaint();
			m_topCanvas.repaint();
		}

	}

	// Ticked when level has completed
	public void tickLevelComplete() {
		if (m_gameModel.m_nTicks2LevelComp == 0) {
			m_gameModel.setPausedGame(true);
			m_gameUI.m_bRedrawAll = true;
		}

		m_gameModel.m_nTicks2LevelComp++;

		// One second later, hide things and start flashing the board
		if (m_gameModel.m_nTicks2LevelComp == 600 / m_delay) {
			m_gameModel.setVisibleThings(false);
			m_gameModel.m_player.m_bVisible = true;
			m_gameUI.m_bFlipWallColor = true;
			m_gameUI.m_bRedrawAll = true;

		} else if (m_gameModel.m_nTicks2LevelComp > 600 / m_delay
				&& (m_gameModel.m_nTicks2LevelComp % (200 / m_delay)) == 0) {
			m_gameUI.m_bFlipWallColor = !m_gameUI.m_bFlipWallColor;
			m_gameUI.m_bRedrawAll = true;
		}

		if (m_gameModel.m_nTicks2LevelComp == 1900 / m_delay) {
			// This will advance the level and set the State to STATE_BEGIN_PLAY
			m_gameModel.loadNextLevel();
			m_gameModel.m_state = GameModel.STATE_BEGIN_PLAY;
			m_gameModel.m_nTicks2LevelComp = 0;
			m_gameUI.m_bFlipWallColor = false;
			m_gameUI.m_bRedrawAll = true;
			m_bottomCanvas.repaint();
		}
	}

	// Ticked when Pacman has died
	public void tickDeadPlay() {
		if (m_gameModel.m_nTicks2DeadPlay == 0) {
			m_gameModel.setPausedGame(true);
			m_gameModel.m_player.m_rotationDying = 0;
			m_gameModel.m_player.m_mouthDegreeDying = 45;
			m_gameModel.m_player.m_mouthArcDying = 135;
			m_gameUI.m_bRedrawAll = true;
			m_gameModel.m_nOrigTicksPerSecond = m_ticksPerSec;
			setTicksPerSec(35);
			// m_soundMgr.stop();
		}

		m_gameModel.m_nTicks2DeadPlay++;

		if (m_gameModel.m_nTicks2DeadPlay == 1000 / m_delay) {
			m_gameModel.m_player.m_bDrawDead = true;
			for (int i = 0; i < m_gameModel.m_ghosts.length; i++) {
				m_gameModel.m_ghosts[i].setVisible(false);
			}
			m_gameModel.m_fruit.setVisible(false);
			m_gameUI.m_bRedrawAll = true;
			// m_soundMgr.playSound(SoundManager.SOUND_PACMANDIES);
		}

		if (m_gameModel.m_nTicks2DeadPlay == (SoundManager.SOUND_PACMANDIES_LENGTH + 1000)
				/ m_delay) {
			m_gameModel.m_state = GameModel.STATE_DEADPACMAN; // STATE_LEVELCOMPLETE
			m_gameModel.m_nTicks2DeadPlay = 0;
			setTicksPerSec(m_gameModel.m_nOrigTicksPerSecond);
			m_gameUI.m_bRedrawAll = true;
		}
	}

	// Ticked when the game is about to begin play
	public void tickBeginPlay() {
		if (m_gameModel.m_nTicks2BeginPlay == 0) {
			m_gameModel.setVisibleThings(false);
			m_gameModel.setPausedGame(true);
			m_gameUI.m_bDrawReady = true;
			m_gameUI.m_bDrawGameOver = false;
			m_gameUI.m_bRedrawAll = true;
			m_gameUI.m_bFlipWallColor = false;
			m_gameUI.refreshRedrawHash();
			if (m_gameModel.m_bPlayStartClip) {
				// m_soundMgr.playSound(SoundManager.SOUND_START);
				m_gameModel.m_bPlayStartClip = false;
			}
			m_bottomCanvas.repaint();
		}

		m_gameModel.m_nTicks2BeginPlay++;

		if (m_gameModel.m_nTicks2BeginPlay == 500 / m_delay) {
			m_gameModel.setVisibleThings(true);
			m_gameModel.m_fruit.setVisible(false);
		}

		// if ((m_gameModel.m_nTicks2BeginPlay ==
		// SoundManager.SOUND_START_LENGTH
		// / m_delay && !m_gameModel.m_bStartClipPlayed)
		// || (m_gameModel.m_nTicks2BeginPlay == 1000 / m_delay &&
		// m_gameModel.m_bStartClipPlayed)) {
		// Ignoring the 'Ready' pause
		if (true) {
			m_gameModel.m_state = GameModel.STATE_PLAYING;
			m_gameModel.setVisibleThings(true);
			m_gameModel.m_fruit.setVisible(false);
			m_gameModel.setPausedGame(false);
			m_gameUI.m_bDrawReady = false;
			m_gameUI.m_bRedrawAll = true;
			m_gameModel.m_nTicks2BeginPlay = 0;
			m_gameModel.m_bStartClipPlayed = true;
			// m_soundMgr.playSound(SoundManager.SOUND_SIREN);
		}
	}

	// Ticked when the game is playing normally
	public void tickGamePlay() {
		boolean bFleeing = false;
		int nCollisionCode;

		// Check if player has earned free life
		if (m_gameModel.m_player.m_score >= m_gameModel.m_nextFreeUp) {
			// m_soundMgr.playSound(SoundManager.SOUND_EXTRAPAC);
			m_gameModel.m_nLives += 1;
			m_gameModel.m_nextFreeUp += 10000;
			m_bottomCanvas.repaint();
		}

		// Check for collisions between Things and Pacman
		for (int i = 0; i < m_gameModel.m_things.length; i++) {
			nCollisionCode = m_gameModel.m_things[i]
					.checkCollision(m_gameModel.m_player);

			if (nCollisionCode == 1) // Ghost was eaten
			{
				// m_soundMgr.playSound(SoundManager.SOUND_EATGHOST);
				break; // Must be eaten one tick at a time
			} else if (nCollisionCode == 2) // Pacman was caught.
			{
				m_gameModel.m_state = GameModel.STATE_DEAD_PLAY; // STATE_LEVELCOMPLETE;
				m_gameModel.m_player.m_direction = Thing.STILL;
				m_gameModel.m_nTicks2DeadPlay = 0;
				return;
			} else if (nCollisionCode == 3) // Pacman ate a Fruit
			{
				// m_soundMgr.playSound(SoundManager.SOUND_EATFRUIT);
				break; // Must be eaten one tick at a time
			}
		}

		// Tick and then Move each Thing (includes Pacman and Ghosts)
		for (int i = 0; i < m_gameModel.m_things.length; i++) {
			m_gameModel.m_things[i].tickThing(m_gameUI);
			if (m_gameModel.m_things[i].canMove())
				Move(m_gameModel.m_things[i]);
		}

		// Check to see if there are any fleeing Ghosts left
		// because of a power up pacman ate.
		for (int i = 0; i < m_gameModel.m_ghosts.length; i++) {
			bFleeing |= m_gameModel.m_ghosts[i].isEdible();
		}
		// If no fleeing ghosts, then reset the Power Up eat ghost score back to
		// 200
		// and kill the BlueGhost loop
		if (bFleeing != true) {
			m_gameModel.m_eatGhostPoints = 200;
			// m_soundMgr.stopSound(SoundManager.SOUND_GHOSTBLUE);
			// m_soundMgr.playSound(SoundManager.SOUND_SIREN);
		}

		if (m_gameModel.m_totalFoodCount == m_gameModel.m_currentFoodCount) {
			m_gameModel.m_state = GameModel.STATE_LEVELCOMPLETE;
			m_gameModel.m_nTicks2LevelComp = 0;
		}
		// Tick the sound manager (mainly to check if the Chomping loop needs to
		// be stopped)
		// m_soundMgr.tickSound();

		m_gameModel.swapModes();
	}

	// Ticked when the game is running the intro
	public void tickIntro() {
		if (!m_gameModel.m_bIntroInited) {
			m_gameModel.initIntro();
			setTicksPerSec(35);
			m_gameModel.m_bIntroInited = true;
			m_gameUI.m_bRedrawAll = true;
		}

		// Check if Ghost has run to the left of the Runway
		for (int i = 0; i < m_gameModel.m_ghosts.length; i++) {
			if (m_gameModel.m_ghosts[i].m_locX == 19)
				m_gameModel.m_ghosts[i].m_bPaused = true;
		}

		if (!m_gameModel.m_ghosts[0].m_bVisible) {
			m_gameModel.m_ghosts[0].m_bVisible = true;
			m_gameModel.m_ghosts[0].m_bPaused = false;
		}

		if (!m_gameModel.m_ghosts[1].m_bVisible
				&& m_gameModel.m_ghosts[0].m_locX == 19) {
			m_gameModel.m_ghosts[1].m_bVisible = true;
			m_gameModel.m_ghosts[1].m_bPaused = false;
		}

		if (!m_gameModel.m_ghosts[2].m_bVisible
				&& m_gameModel.m_ghosts[1].m_locX == 19) {
			m_gameModel.m_ghosts[2].m_bVisible = true;
			m_gameModel.m_ghosts[2].m_bPaused = false;
		}

		if (!m_gameModel.m_ghosts[3].m_bVisible
				&& m_gameModel.m_ghosts[2].m_locX == 19) {
			m_gameModel.m_ghosts[3].m_bVisible = true;
			m_gameModel.m_ghosts[3].m_bPaused = false;
		}

		if (!m_gameModel.m_player.m_bVisible
				&& m_gameModel.m_ghosts[3].m_locX == 19) {
			m_gameModel.m_player.m_bVisible = true;
			m_gameModel.m_player.m_bPaused = false;
		}

		if (m_gameModel.m_player.m_locX == 23)
			m_gameModel.m_player.m_requestedDirection = Thing.LEFT;

		if (m_gameModel.m_player.m_locX == 5)
			m_gameModel.m_player.m_requestedDirection = Thing.RIGHT;

		// Tick and then Move each Thing (includes Pacman and Ghosts)
		for (int i = 0; i < m_gameModel.m_things.length; i++) {
			m_gameModel.m_things[i].tickThing(m_gameUI);
			if (m_gameModel.m_things[i].canMove())
				Move(m_gameModel.m_things[i]);
		}
	}

	// This method is called to update the Thing's Location and delta Locations
	// based on Thing's m_direction. The ONLY update to Thing's m_direction is
	// if Thing hits a wall and m_direction is set to STILL. Otherwise, all
	// m_direction changes occur in the Thing's virtual method tickThing ().
	public void Move(Thing thing) {
		if (thing.m_direction == Thing.STILL)
			return;

		boolean bMoved = false;

		thing.m_lastLocX = thing.m_locX;
		thing.m_lastLocY = thing.m_locY;
		thing.m_lastDeltaLocX = thing.m_deltaLocX;
		thing.m_lastDeltaLocY = thing.m_deltaLocY;

		// See if thing can eat any nearby items
		thing.eatItem();

		// Based on the current direction, update thing's location in that
		// direction.
		// The thing.m_deltaLocX != 0 is so that if the thing is in a cell with
		// a wall
		// and the thing is directed towards the wall, he will still need to
		// move towards the
		// wall until the thing is dead center in the cell.
		if (thing.m_direction == Thing.LEFT
				&& ((m_gameModel.m_gameState[thing.m_locX][thing.m_locY] & GameModel.GS_WEST) == 0 || thing.m_deltaLocX != 0)) {
			thing.m_deltaLocX--;
			bMoved = true;
		} else if (thing.m_direction == Thing.RIGHT
				&& ((m_gameModel.m_gameState[thing.m_locX][thing.m_locY] & GameModel.GS_EAST) == 0 || thing.m_deltaLocX != 0)) {
			thing.m_deltaLocX++;
			bMoved = true;
		} else if (thing.m_direction == Thing.UP
				&& ((m_gameModel.m_gameState[thing.m_locX][thing.m_locY] & GameModel.GS_NORTH) == 0 || thing.m_deltaLocY != 0)) {
			thing.m_deltaLocY--;
			bMoved = true;
		} else if (thing.m_direction == Thing.DOWN
				&& ((m_gameModel.m_gameState[thing.m_locX][thing.m_locY] & GameModel.GS_SOUTH) == 0 || thing.m_deltaLocY != 0)) {
			thing.m_deltaLocY++;
			bMoved = true;
		}

		// If the thing has moved past the middle of the two cells, then switch
		// his location to the other side.
		if (thing.m_deltaLocX <= -thing.m_deltaMax) // Shift thing to adjacent
		// cell on left
		{
			if (thing.m_locX != 0) {
				thing.m_deltaLocX = thing.m_deltaMax - 1;
				thing.m_locX--;
				bMoved = true;

			} else {
				// Check to see if thing should warp to right side
				if (thing.m_deltaLocX < -thing.m_deltaMax) {
					thing.m_deltaLocX = thing.m_deltaMax - 1;
					thing.m_locX = m_gameModel.m_gameSizeX - 1;
					bMoved = true;
				}
			}
		} else if (thing.m_deltaLocX >= thing.m_deltaMax) // Shift thing to
		// adjacent cell on
		// right
		{
			if (thing.m_locX != m_gameModel.m_gameSizeX - 1) {
				thing.m_deltaLocX = 1 - thing.m_deltaMax;
				thing.m_locX++;
				bMoved = true;
			} else {
				// Check to see if thing should warp to left side
				if (thing.m_deltaLocX > thing.m_deltaMax) {
					thing.m_deltaLocX = 1 - thing.m_deltaMax;
					thing.m_locX = 0;
					bMoved = true;
				}
			}
		} else if (thing.m_deltaLocY <= -thing.m_deltaMax) // Shift thing to
		// adjacent cell on
		// top
		{
			if (thing.m_locY != 0) {
				thing.m_deltaLocY = thing.m_deltaMax - 1;
				thing.m_locY--;
				bMoved = true;

			} else {
				// Check to see if thing should warp to bottom side
				if (thing.m_deltaLocY < -thing.m_deltaMax) {
					thing.m_deltaLocY = thing.m_deltaMax - 1;
					thing.m_locY = m_gameModel.m_gameSizeY - 1;
					bMoved = true;
				}
			}
		} else if (thing.m_deltaLocY >= thing.m_deltaMax) // Shift thing to
		// adjacent cell on
		// bottom
		{
			if (thing.m_locY != m_gameModel.m_gameSizeY - 1) {
				thing.m_deltaLocY = 1 - thing.m_deltaMax;
				thing.m_locY++;
				bMoved = true;
			} else {
				// Check to see if thing should warp to top side
				if (thing.m_deltaLocY > thing.m_deltaMax) {
					thing.m_deltaLocY = 1 - thing.m_deltaMax;
					thing.m_locY = 0;
					bMoved = true;
				}
			}
		}

		if (!bMoved)
			thing.m_direction = Thing.STILL;
	}

	public void start() {
		if (m_ticker == null) {
			m_ticker = new Ticker(this);
			m_ticker.start();
		}
	}

	public void stop() {
		m_ticker = null;
		// m_soundMgr.stop();
	}

	void setTicksPerSec(int ticks) {
		m_ticksPerSec = ticks;
		m_delay = 1000 / m_ticksPerSec;
	}

	void toggleGhostAI() {
		for (int i = 0; i < m_gameModel.m_ghosts.length; i++) {
			m_gameModel.m_ghosts[i].m_bInsaneAI = !m_gameModel.m_ghosts[i].m_bInsaneAI;
		}
	}

	/**
	 * Gets the current score.
	 * 
	 * @return The score.
	 */
	public GameModel getGameModel() {
		return m_gameModel;
	}

	/**
	 * Simulates a key press by the user. Easier than routing methods directly.
	 * 
	 * @param event
	 *            The simulated key press.
	 */
	public void simulateKeyPress(int event) {
		keyAdapter_.activateEvent(event);
	}

	// Can't run Pacman as an application since it use sound-related methods.
	public static void main(String args[]) {
		// MainFrame frame = new MainFrame ("PacMan");

		// Create PacMan instance
		PacMan pacMan = new PacMan();

		// Initialize instance
		pacMan.init();

		pacMan.start();
	}

	/*
	 * class MainFrame extends Frame { MainFrame (String title) { super (title); }
	 * 
	 * public boolean handleEvent (Event e) { if (e.id ==Event.WINDOW_DESTROY) {
	 * System.exit (0); } return super.handleEvent (e); } }
	 */

	// Ticker thread that updates the game state and refreshes the UI
	// 
	class Ticker extends Thread {

		PacMan m_pacMan;

		public Ticker(PacMan pacMan) {
			m_pacMan = pacMan;
		}

		@Override
		public void run() {
			while (Thread.currentThread() == m_pacMan.m_ticker) {
				try {
					Thread.sleep(m_pacMan.m_delay);

				} catch (InterruptedException e) {
					break;
				}

				m_pacMan.tick(true);
				if (m_pacMan.getFrame() != null)
					m_pacMan.getFrame().requestFocus();
			}
		}
	}

	// Key event handlers
	class PacManKeyAdapter extends KeyAdapter {
		PacMan m_pacMan;

		PacManKeyAdapter(PacMan pacMan) {
			super();
			m_pacMan = pacMan;
		}

		@Override
		public void keyPressed(KeyEvent event) {
			activateEvent(event.getKeyCode());
		}

		public void activateEvent(int event) {
			switch (event) {

			case KeyEvent.VK_LEFT:
				m_pacMan.m_gameModel.m_player.m_requestedDirection = Thing.LEFT;
				break;

			case KeyEvent.VK_RIGHT:
				m_pacMan.m_gameModel.m_player.m_requestedDirection = Thing.RIGHT;
				break;

			case KeyEvent.VK_UP:
				m_pacMan.m_gameModel.m_player.m_requestedDirection = Thing.UP;
				break;

			case KeyEvent.VK_DOWN:
				m_pacMan.m_gameModel.m_player.m_requestedDirection = Thing.DOWN;
				break;

			case KeyEvent.VK_N:
				m_pacMan.m_gameModel.m_state = GameModel.STATE_NEWGAME;
				m_pacMan.m_gameUI.m_bDrawPaused = false;
				break;

			case KeyEvent.VK_P:
				if (m_pacMan.m_gameModel.m_state == GameModel.STATE_GAMEOVER)
					break;

				if (m_pacMan.m_gameModel.m_state == GameModel.STATE_PAUSED) {
					m_pacMan.m_gameModel.m_state = m_pacMan.m_gameModel.m_pausedState;
					m_pacMan.m_gameUI.m_bDrawPaused = false;
					m_pacMan.m_gameUI.m_bRedrawAll = true;

				} else {
					m_pacMan.m_gameModel.m_pausedState = m_pacMan.m_gameModel.m_state;
					m_pacMan.m_gameModel.m_state = GameModel.STATE_PAUSED;
				}
				break;

			case KeyEvent.VK_A:
				m_pacMan.m_gameModel.m_state = GameModel.STATE_ABOUT;
				m_pacMan.m_gameModel.m_nTicks2AboutShow = 0;
				break;

			// case KeyEvent.VK_S:
			// m_pacMan.m_soundMgr.m_bEnabled = !m_pacMan.m_soundMgr.m_bEnabled;
			// if (m_pacMan.m_soundMgr.m_bEnabled == false)
			// m_pacMan.m_soundMgr.stop();
			// m_pacMan.m_bottomCanvas.repaint();
			// break;

			case KeyEvent.VK_I:
				m_pacMan.toggleGhostAI();
				break;

			case KeyEvent.VK_ESCAPE:
				System.exit(0);
				break;

			case KeyEvent.VK_SPACE:
			case KeyEvent.VK_NUMPAD0:
				m_pacMan.experimentMode_ = !m_pacMan.experimentMode_;
				break;
			}
		}
	}
}
