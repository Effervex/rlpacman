package jCloisterZone;

import java.lang.reflect.Proxy;
import java.util.List;
import jess.Rete;
import relationalFramework.PolicyActions;
import rrlFramework.RRLEnvironment;
import rrlFramework.RRLExperiment;
import util.Pair;

import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.figure.SmallFollower;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.PlayerSlot;
import com.jcloisterzone.game.phase.ActionPhase;
import com.jcloisterzone.game.phase.GameOverPhase;
import com.jcloisterzone.game.phase.Phase;
import com.jcloisterzone.game.phase.TilePhase;
import com.jcloisterzone.rmi.ServerIF;
import com.jcloisterzone.rmi.mina.ClientStub;

public class CarcassonneEnvironment extends RRLEnvironment {
	private Game environment_;
	private boolean guiMode_ = false;
	private int prevScore_;
	private RRLJCloisterClient client_;
	private ServerIF server_;
	private CarcassonneRelationalWrapper relationalWrapper_ = new CarcassonneRelationalWrapper();
	private boolean earlyExit_;

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
			PlayerSlot slot = new PlayerSlot(0, PlayerSlot.SlotType.PLAYER,
					"CERRLA", clientID);
			slot.setOwner(clientID);
			server_.updateSlot(slot, null);
			environment_ = client_.getGame();
			while (environment_ == null) {
				try {
					Thread.yield();
				} catch (Exception e) {
				}
				environment_ = client_.getGame();
			}
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
				return;
			}

			// Place a meeple
			Feature feature = (Feature) action;
			server_.deployMeeple(feature.getTile().getPosition(),
					feature.getLocation(), SmallFollower.class);
		}
	}

	@Override
	protected boolean isTerminal() {
		return earlyExit_ || environment_.getPhase() instanceof GameOverPhase
				|| super.isTerminal();
	}

	@Override
	public void cleanup() {
		// TODO Auto-generated method stub

	}

	@Override
	public void initialise(int runIndex, String[] extraArg) {
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
