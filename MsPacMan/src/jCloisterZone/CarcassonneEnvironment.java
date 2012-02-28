package jCloisterZone;

import java.lang.reflect.Proxy;
import java.util.List;
import jess.Rete;
import relationalFramework.PolicyActions;
import rrlFramework.RRLEnvironment;
import rrlFramework.RRLExperiment;
import util.Pair;

import cerrla.ProgramArgument;

import com.jcloisterzone.ai.AiPlayer;
import com.jcloisterzone.ai.LegacyAiPlayer;
import com.jcloisterzone.ai.RandomAIPlayer;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.figure.SmallFollower;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.PlayerSlot;
import com.jcloisterzone.game.phase.ActionPhase;
import com.jcloisterzone.game.phase.DrawPhase;
import com.jcloisterzone.game.phase.GameOverPhase;
import com.jcloisterzone.game.phase.Phase;
import com.jcloisterzone.game.phase.TilePhase;
import com.jcloisterzone.rmi.ServerIF;
import com.jcloisterzone.rmi.mina.ClientStub;

public class CarcassonneEnvironment extends RRLEnvironment {
	/** The Carcassonne client. */
	private RRLJCloisterClient client_;
	/** If the game should exit early. */
	private boolean earlyExit_;
	/** The current environment. */
	private Game environment_;
	/** If viewing experiment in GUI. */
	private boolean guiMode_ = true;
	/** The player delay when viewing GUI version. */
	private int playerDelay = 0;
	/** The previous score. */
	private int prevScore_;
	/** The relational wrapper for (de)relationalising the game. */
	private CarcassonneRelationalWrapper relationalWrapper_ = new CarcassonneRelationalWrapper();
	/** The Carcassonne server. */
	private ServerIF server_;
	/** The AI players. */
	private AiPlayer[] players_ = {};

	/**
	 * Cycles through the phases whenever necessary. Also replaces the
	 * proxy-based DrawPhase with a proxyless version.
	 */
	private void runPhases() {
		// Sleep for visual aid.
		if (playerDelay > 0) {
			try {
				Thread.sleep(playerDelay);
			} catch (InterruptedException e) {
			}
		}

		Phase phase = environment_.getPhase();

		// Cycle through (probably only once) to keep the game moving.
		while (phase != null && !phase.isEntered()) {
			// Modifying DrawPhase to proxyless version
			if (phase.getClass().equals(DrawPhase.class))
				phase = environment_.getPhases().get(ProxylessDrawPhase.class);

			phase.setEntered(true);
			phase.enter();
			phase = environment_.getPhase();
		}
	}

	@Override
	protected void assertStateFacts(Rete rete, List<String> goalArgs)
			throws Exception {
		relationalWrapper_.assertStateFacts(rete, environment_);
	}

	@Override
	protected double calculateReward(boolean isTerminal) {
		int currentScore = environment_.getTurnPlayer().getPoints();
		int diff = currentScore - prevScore_;
		prevScore_ = currentScore;
		return diff;
	}

	@Override
	protected List<String> getGoalArgs() {
		// No goal args
		return null;
	}

	@Override
	protected Object groundActions(PolicyActions actions) {
		return relationalWrapper_.groundActions(actions, environment_);
	}

	@Override
	protected boolean isReteDriven() {
		return false;
	}

	@Override
	protected boolean isTerminal() {
		boolean terminal = earlyExit_
				|| environment_.getPhase() instanceof GameOverPhase
				|| super.isTerminal();
		if (terminal && playerDelay > 0) {
			try {
				Thread.sleep(playerDelay * 10);
			} catch (InterruptedException e) {
			}
		}
		return terminal;
	}

	@Override
	protected void startState() {
		relationalWrapper_.startState();

		while (client_.isRunning()) {
			try {
				Thread.yield();
			} catch (Exception e) {
			}
		}
		client_.createGame();
		earlyExit_ = false;
		prevScore_ = 0;

		if (environment_ == null) {
			// Sleep only as long as it needs to to get the clientID.
			long clientID = -1;
			while (clientID == -1) {
				try {
					Thread.yield();
					clientID = client_.getClientId();
				} catch (Exception e) {
				}
			}

			server_ = client_.getServer();
			server_.setRandomGenerator(RRLExperiment.random_);

			// Handle number of players playing
			int slotIndex = 0;
			PlayerSlot slot = new PlayerSlot(slotIndex++,
					PlayerSlot.SlotType.PLAYER, "CERRLA", clientID);
			server_.updateSlot(slot, null);
			// AI Players
			for (AiPlayer ai : players_) {
				slot = new PlayerSlot(slotIndex, PlayerSlot.SlotType.AI, "AI"
						+ slotIndex, clientID);
				slot.setAiClassName(RandomAIPlayer.class.getName());
				server_.updateSlot(slot, LegacyAiPlayer.supportedExpansions());
				slotIndex++;
			}

			// Start the game.
			environment_ = client_.getGame();
			while (environment_ == null) {
				try {
					Thread.yield();
				} catch (Exception e) {
				}
				environment_ = client_.getGame();
			}
			relationalWrapper_.setGame(environment_);
			environment_.addUserInterface(relationalWrapper_);
			environment_.addGameListener(relationalWrapper_);
		}

		server_.startGame();
		// Sleep until game has started
		while (environment_ == null || environment_.getBoard() == null
				|| environment_.getTilePack() == null) {
			environment_ = ((ClientStub) Proxy.getInvocationHandler(server_))
					.getGame();
			try {
				Thread.yield();
			} catch (Exception e) {
			}
		}

		runPhases();
	}

	@Override
	protected void stepState(Object action) {
		// If null, exit the episode
		if (action == null) {
			earlyExit_ = true;
			client_.closeGame(true);
			return;
		}

		Phase phase = environment_.getPhase();
		if (phase instanceof TilePhase) {
			// Place the tile at the given position and rotation
			@SuppressWarnings("unchecked")
			Pair<Position, Rotation> posRot = (Pair<Position, Rotation>) action;
			server_.placeTile(posRot.objB_, posRot.objA_);
		} else if (phase instanceof ActionPhase) {
			// If no action, place no figure
			if (action.equals(CarcassonneRelationalWrapper.NO_ACTION)) {
				server_.placeNoFigure();
			} else {
				// Place a meeple
				Feature feature = (Feature) action;
				server_.deployMeeple(feature.getTile().getPosition(),
						feature.getLocation(), SmallFollower.class);
			}
		}

		runPhases();
	}

	@Override
	public void cleanup() {
		// TODO Auto-generated method stub

	}

	@Override
	public void initialise(int runIndex, String[] extraArg) {
		guiMode_ = !ProgramArgument.EXPERIMENT_MODE.booleanValue();

		// Only initialise the client if it's not already initialised.
		if (client_ == null) {
			// Parse play speed
			for (String arg : extraArg) {
				// Setting up multiple agents
				if (arg.startsWith("multi")) {
					String[] split = arg.split(" ");
					if (split[1].equals("AI")) {
						players_ = new AiPlayer[Integer.parseInt(split[2]) - 1];
						for (int i = 0; i < players_.length; i++)
							players_[i] = new LegacyAiPlayer();
					}
				}
				try {
					int playSpeed = Integer.parseInt(arg);
					if (guiMode_)
						playerDelay = playSpeed;
				} catch (Exception e) {
				}
			}

			if (guiMode_) {
				client_ = new GuiCarcassonneClient("config.ini", true);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			} else
				client_ = new LocalCarcassonneClient("config.ini");
		}
	}
}
