package jCloisterZone;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

import jess.Rete;
import relationalFramework.PolicyActions;
import relationalFramework.StateSpec;
import rrlFramework.RRLEnvironment;
import rrlFramework.RRLExperiment;
import rrlFramework.RRLObservations;
import util.MultiMap;
import util.Pair;

import cerrla.ProgramArgument;

import com.jcloisterzone.Player;
import com.jcloisterzone.UserInterface;
import com.jcloisterzone.ai.legacyplayer.LegacyAiPlayer;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.figure.SmallFollower;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.PlayerSlot;
import com.jcloisterzone.game.phase.ActionPhase;
import com.jcloisterzone.game.phase.CreateGamePhase;
import com.jcloisterzone.game.phase.DrawPhase;
import com.jcloisterzone.game.phase.GameOverPhase;
import com.jcloisterzone.game.phase.Phase;
import com.jcloisterzone.game.phase.TilePhase;
import com.jcloisterzone.rmi.ServerIF;
import com.jcloisterzone.rmi.mina.ClientStub;

public class CarcassonneEnvironment extends RRLEnvironment {
	private static final String AI_NAME = "AI";
	private static final String RANDOM_NAME = "RANDOM";
	private static final String CERRLA_NAME = "CERRLA";
	private static final String HUMAN_NAME = "HUMAN";
	private static final int NO_ACTION_PENALTY = -1000;
	/** The Carcassonne client. */
	private RRLJCloisterClient client_;
	/** The user interface for the client. */
	private UserInterface clientInterface_;
	/** The current player. */
	private Player currentPlayer_;
	/** If the game should exit early. */
	private boolean earlyExit_;
	/** The current environment. */
	private Game environment_;
	/** If viewing experiment in GUI. */
	private boolean guiMode_ = true;
	/** If there are multiple learning agents at once. */
	private boolean multiLearners_ = false;
	/** The player delay when viewing GUI version. */
	private int playerDelay = 0;
	/** The AI players. */
	private String[] players_;
	/** The previous score. */
	private Map<Player, Integer> prevScores_ = new HashMap<Player, Integer>();
	/** The players that did not place a tile during the game. */
	private Collection<Player> earlyExitPlayers_ = new HashSet<Player>();
	/** The relational wrapper for (de)relationalising the game. */
	protected CarcassonneRelationalWrapper relationalWrapper_ = new CarcassonneRelationalWrapper();
	/** The Carcassonne server. */
	private ServerIF server_;
	/** The current players of the game. */
	private ArrayList<PlayerSlot> slots_;

	/**
	 * Calculates a player's reward between steps.
	 * 
	 * @param player
	 *            The player to calculate reward for.
	 * @return The reward received in one step.
	 */
	private double[] calculateReward(Player player) {
		int prevScore = getPlayerPrevScore(player);

		int currentScore = player.getPoints();
		int diff = currentScore - prevScore;
		prevScores_.put(player, currentScore);

		double[] reward = new double[2];
		reward[RRLObservations.ENVIRONMENTAL_INDEX] = diff;
		reward[RRLObservations.INTERNAL_INDEX] = (earlyExitPlayers_
				.contains(player)) ? NO_ACTION_PENALTY : diff;
		return reward;
	}

	/**
	 * Checks if the game should be initialised with multiple players.
	 * 
	 * @param arg
	 *            The arg to check.
	 */
	private void checkMultiplayer(String arg) {
		// Setting up multiple agents
		if (arg.startsWith("multi")) {
			if (arg.startsWith("multi[")) {
				// Using a defined list of players
				String playerList = arg.substring(6, arg.indexOf("]"));
				String[] split = playerList.split(",");
				players_ = new String[split.length];
				for (int i = 0; i < split.length; i++) {
					players_[i] = split[i].trim();
				}
				multiLearners_ = true;
			} else if (arg.startsWith("multi")) {
				// 1 agent + X other players.
				Pattern parser = Pattern.compile("multi([A-Z]+)(\\d)");
				Matcher matcher = parser.matcher(arg);

				if (matcher.find()) {
					players_ = new String[Integer.parseInt(matcher.group(2))];
					// At least one learner
					players_[0] = CERRLA_NAME;
					// The rest of the learners
					for (int i = 1; i < players_.length; i++) {
						players_[i] = matcher.group(1);
						if (players_[i].equals(CERRLA_NAME))
							multiLearners_ = true;
					}
				}
			}
		}
	}

	private int getPlayerPrevScore(Player player) {
		if (!prevScores_.containsKey(player))
			prevScores_.put(player, 0);
		int prevScore = prevScores_.get(player);
		return prevScore;
	}

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
			// Modifying phases to proxyless versions
			if (!guiMode_) {
				if (phase.getClass().equals(CreateGamePhase.class))
					phase = environment_.getPhases().get(
							ProxylessCreateGamePhase.class);
				if (phase.getClass().equals(DrawPhase.class))
					phase = environment_.getPhases().get(
							ProxylessDrawPhase.class);
			}

			phase.setEntered(true);
			phase.enter();
			phase = environment_.getPhase();
		}

		while (environment_.getTurnPlayer().getNick().startsWith(HUMAN_NAME)) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void assertStateFacts(Rete rete, List<String> goalArgs)
			throws Exception {
		relationalWrapper_.assertStateFacts(rete, environment_);
		if (currentPlayer_ == null)
			currentPlayer_ = environment_.getTurnPlayer();
	}

	@Override
	protected double[] calculateReward(int isTerminal) {
		return calculateReward(currentPlayer_);
	}

	@Override
	protected RRLObservations compileObservation(Rete rete,
			MultiMap<String, String[]> validActions, BidiMap goalReplacements,
			int isTerminal) {
		// If only a single leaerner, proceed as normal
		if (!multiLearners_)
			return super.compileObservation(rete, validActions,
					goalReplacements, isTerminal);

		// Otherwise note the individual rewards
		RRLObservations rrlObs = new RRLObservations(rete, validActions,
				isTerminal, goalReplacements, environment_.getTurnPlayer()
						.getNick());
		for (Player p : environment_.getAllPlayers()) {
			String playerName = p.getNick();
			if (playerName.startsWith(CERRLA_NAME)) {
				double[] playerReward = calculateReward(p);
				rrlObs.addPlayerObservations(playerName, playerReward);
			}
		}
		return rrlObs;
	}

	@Override
	protected List<String> getGoalArgs() {
		// No goal args
		return null;
	}

	@Override
	protected String getPlayerID() {
		return environment_.getTurnPlayer().getNick();
	}

	@Override
	protected Object groundActions(PolicyActions actions) {
		Object action = relationalWrapper_.groundActions(actions, environment_,
				multiLearners_);
		// Check for no tile placement
		if (relationalWrapper_.wasNoTilePlaced()) {
			Player p = environment_.getTurnPlayer();
			earlyExitPlayers_.add(p);
			if (earlyExitPlayers_.size() == players_.length)
				earlyExit_ = true;
		}
		return action;
	}

	@Override
	protected boolean isReteDriven() {
		return false;
	}

	@Override
	protected int isTerminal() {
		boolean terminal = earlyExit_
				|| environment_.getPhase() instanceof GameOverPhase
				|| super.isTerminal() == 1;
		if (terminal && playerDelay > 0) {
			try {
				Thread.sleep(playerDelay * 10);
			} catch (InterruptedException e) {
			}
		}
		return (terminal) ? 1 : 0;
	}

	@Override
	protected void startState() {
		relationalWrapper_.startState();

		while (!ProgramArgument.EXPERIMENT_MODE.booleanValue()
				&& client_.isRunning()) {
			try {
				Thread.yield();
			} catch (Exception e) {
			}
		}
		client_.createGame();
		earlyExit_ = false;
		earlyExitPlayers_.clear();
		prevScores_.clear();

		if (environment_ == null) {
			// Sleep only as long as it needs to to get the clientID.
			long clientID = (ProgramArgument.EXPERIMENT_MODE.booleanValue()) ? client_
					.getClientId() : -1;
			while (!ProgramArgument.EXPERIMENT_MODE.booleanValue()
					&& clientID == -1) {
				try {
					Thread.yield();
					clientID = client_.getClientId();
				} catch (Exception e) {
				}
			}

			server_ = client_.getServer();
			server_.setRandomGenerator(RRLExperiment.random_);

			// Handle number of players playing
			slots_ = new ArrayList<PlayerSlot>(players_.length);
			int slotIndex = 0;
			for (String playerName : players_) {
				String playerNameIndex = playerName + slotIndex;
				if (playerName.equals(CERRLA_NAME)) {
					// Agent-controlled
					slots_.add(new PlayerSlot(slotIndex,
							PlayerSlot.SlotType.PLAYER, playerNameIndex,
							clientID));
				} else if (playerName.equals(AI_NAME)) {
					// AI controlled
					PlayerSlot slot = new PlayerSlot(slotIndex,
							PlayerSlot.SlotType.AI, playerNameIndex, clientID);
					slot.setAiClassName(LegacyAiPlayer.class.getName());
					slots_.add(slot);
				} else if (playerName.equals(RANDOM_NAME)) {
					// AI controlled
					PlayerSlot slot = new PlayerSlot(slotIndex,
							PlayerSlot.SlotType.AI, playerNameIndex, clientID);
					slot.setAiClassName(RandomAIPlayer.class.getName());
					slots_.add(slot);
				} else if (playerName.equals(HUMAN_NAME)) {
					// Human-controlled
					slots_.add(new PlayerSlot(slotIndex,
							PlayerSlot.SlotType.PLAYER, playerNameIndex,
							clientID));
				}
				slotIndex++;
			}

			// Start the game.
			environment_ = client_.getGame();
			while (!ProgramArgument.EXPERIMENT_MODE.booleanValue()
					&& environment_ == null) {
				try {
					Thread.yield();
				} catch (Exception e) {
				}
				environment_ = client_.getGame();
			}
			relationalWrapper_.setGame(environment_);
			environment_.addGameListener(relationalWrapper_);
			// Ad-hoc fix
			if (ProgramArgument.EXPERIMENT_MODE.booleanValue())
				environment_.addUserInterface(relationalWrapper_);
			clientInterface_ = environment_.getUserInterface();
		} else if (players_.length > 1) {
			// Reset the UIs
			server_.stopGame();
			environment_.clearUserInterface();
			environment_.addUserInterface(clientInterface_);

			// Clear the slots and re-add them.
			for (int i = 0; i < PlayerSlot.COUNT; i++) {
				server_.updateSlot(new PlayerSlot(i), null);
			}
		}
		// Ad-hoc fix
		if (!ProgramArgument.EXPERIMENT_MODE.booleanValue()) {
			environment_.addUserInterface(relationalWrapper_);
		}

		// Randomise the slots
		Collections.shuffle(slots_, RRLExperiment.random_);
		for (int i = 0; i < slots_.size(); i++) {
			PlayerSlot slot = slots_.get(i);
			PlayerSlot cloneSlot = new PlayerSlot(i, slot.getType(),
					slot.getNick(), slot.getOwner());
			cloneSlot.setAiClassName(slot.getAiClassName());
			server_.updateSlot(cloneSlot, LegacyAiPlayer.supportedExpansions());
		}

		server_.startGame();
		// Sleep until game has started
		while (!ProgramArgument.EXPERIMENT_MODE.booleanValue()
				&& (environment_ == null || environment_.getBoard() == null || environment_
						.getTilePack() == null)) {
			environment_ = ((ClientStub) Proxy.getInvocationHandler(server_))
					.getGame();
			try {
				Thread.yield();
			} catch (Exception e) {
			}
		}

		runPhases();

		currentPlayer_ = null;
	}

	@Override
	protected void stepState(Object action) {
		// If null, exit the episode
		if (action == null || earlyExit_) {
			earlyExit_ = true;
			relationalWrapper_.gameOver();
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
		client_ = null;
		clientInterface_ = null;
		currentPlayer_ = null;
		slots_ = null;
		if (earlyExitPlayers_ != null)
			earlyExitPlayers_.clear();
		environment_.clearUserInterface();
		environment_ = null;
		server_ = null;
		if (slots_ != null)
			slots_.clear();
	}

	@Override
	public void initialise(int runIndex, String[] extraArg) {
		guiMode_ = !ProgramArgument.EXPERIMENT_MODE.booleanValue();
		String goal = StateSpec.getInstance().getGoalName();
		checkMultiplayer(goal);

		// Only initialise the client if it's not already initialised.
		if (client_ == null) {
			// Parse play speed
			for (String arg : extraArg) {
				checkMultiplayer(arg);
				try {
					int playSpeed = Integer.parseInt(arg);
					if (guiMode_)
						playerDelay = playSpeed;
				} catch (Exception e) {
				}
			}

			// If single player
			if (players_ == null) {
				players_ = new String[1];
				players_[0] = CERRLA_NAME;
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

	public static void main(String[] args) {
		int repetitions = 100;
		double[] scores = new double[repetitions];

		RRLJCloisterClient client = new LocalCarcassonneClient("config.ini");
		ServerIF server = null;
		Game game = client.getGame();
		Player firstPlayer = null;
		ArrayList<PlayerSlot> slots = new ArrayList<PlayerSlot>();
		for (int r = 0; r < repetitions; r++) {
			client.createGame();
			if (game == null) {
				server = new LocalCarcassonneServer(client.getGame());
				PlayerSlot slot = new PlayerSlot(0, PlayerSlot.SlotType.AI,
						"RANDOM" + 0, client.getClientId());
				slot.setAiClassName(RandomAIPlayer.class.getName());
				slots.add(slot);
				for (int j = 1; j < Integer.parseInt(args[0]); j++) {
					slot = new PlayerSlot(j, PlayerSlot.SlotType.AI,
							"AI" + j, client.getClientId());
					slot.setAiClassName(LegacyAiPlayer.class.getName());
					slots.add(slot);
					// slot = new PlayerSlot(1, PlayerSlot.SlotType.AI,
					// "RANDOM" + 1, client.getClientId());
					// slot.setAiClassName(RandomAIPlayer.class.getName());
					// slots.add(slot);
				}
				game = client.getGame();
			} else {
				// Reset the UIs
				server.stopGame();
				game.clearUserInterface();

				// Clear the slots and re-add them.
				for (int i = 0; i < PlayerSlot.COUNT; i++) {
					server.updateSlot(new PlayerSlot(i), null);
				}
			}

			Collections.shuffle(slots);
			for (int i = 0; i < slots.size(); i++) {
				PlayerSlot slot = slots.get(i);
				PlayerSlot cloneSlot = new PlayerSlot(i, slot.getType(),
						slot.getNick(), slot.getOwner());
				cloneSlot.setAiClassName(slot.getAiClassName());
				server.updateSlot(cloneSlot,
						LegacyAiPlayer.supportedExpansions());
			}

			server.startGame();

			Phase phase = game.getPhase();

			// Cycle through (probably only once) to keep the game moving.
			while (phase != null && !phase.isEntered()) {
				// Modifying phases to proxyless versions
				if (phase.getClass().equals(CreateGamePhase.class))
					phase = game.getPhases()
							.get(ProxylessCreateGamePhase.class);
				if (phase.getClass().equals(DrawPhase.class))
					phase = game.getPhases().get(ProxylessDrawPhase.class);

				phase.setEntered(true);
				phase.enter();
				phase = game.getPhase();

				if (game.getTurnPlayer().getNick().equals("RANDOM0"))
					firstPlayer = game.getTurnPlayer();
			}
			int score = firstPlayer.getPoints();
			scores[r] = score;
			System.out.println(score);
		}

		Mean m = new Mean();
		StandardDeviation sd = new StandardDeviation();
		System.out.println("Mean: " + m.evaluate(scores) + ", SD: "
				+ sd.evaluate(scores));
	}
}
