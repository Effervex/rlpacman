package jCloisterZone;

import java.util.EnumSet;

import org.ini4j.Ini;

import com.jcloisterzone.Expansion;
import com.jcloisterzone.Player;
import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Tile;
import com.jcloisterzone.event.GameEventListener;
import com.jcloisterzone.feature.Castle;
import com.jcloisterzone.feature.Completable;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.feature.visitor.score.CompletableScoreContext;
import com.jcloisterzone.figure.Follower;
import com.jcloisterzone.figure.Meeple;
import com.jcloisterzone.game.CustomRule;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.PlayerSlot;
import com.jcloisterzone.game.Snapshot;
import com.jcloisterzone.game.phase.Phase;
import com.jcloisterzone.rmi.ServerIF;
import com.jcloisterzone.ui.Client;

public class LocalCarcassonneClient implements RRLJCloisterClient,
		GameEventListener {
	private final Ini config;
	private Game game;

	private boolean running_;

	private ServerIF server;

	public LocalCarcassonneClient(String configFile) {
		config = new Ini();
		try {
			config.load(Client.class.getClassLoader().getResource(configFile));
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public boolean closeGame(boolean force) {
		running_ = false;
		return true;
	}

	@Override
	public void createGame() {
		// Initialise the server
		if (server == null) {
			game = new Game();
			server = new LocalCarcassonneServer(game);
			
			game.addGameListener(this);
			game.getPhases().put(ProxylessCreateGamePhase.class,
					new ProxylessCreateGamePhase(game, server));
			game.setConfig(config);
		}

		// Start the game.
		Phase createGamePhase = game.getPhases().get(
				ProxylessCreateGamePhase.class);
		game.setPhase(createGamePhase);
		running_ = true;
	}

	@Override
	public long getClientId() {
		return 1;
	}

	@Override
	public Game getGame() {
		return game;
	}

	@Override
	public ServerIF getServer() {
		return server;
	}

	@Override
	public boolean isRunning() {
		return running_;
	}

	@Override
	public void updateSlot(PlayerSlot slot) {
		// N/A
	}

	@Override
	public void updateExpansion(Expansion expansion, Boolean enabled) {
		// N/A
	}

	@Override
	public void updateCustomRule(CustomRule rule, Boolean enabled) {
		// N/A
	}

	@Override
	public void updateSupportedExpansions(EnumSet<Expansion> expansions) {
		// N/A
	}

	@Override
	public void started(Snapshot snapshot) {
		// N/A
	}

	@Override
	public void playerActivated(Player turnPlayer, Player activePlayer) {
		// N/A
	}

	@Override
	public void ransomPaid(Player from, Player to, Follower meeple) {
		// N/A
	}

	@Override
	public void tileDrawn(Tile tile) {
		// N/A
	}

	@Override
	public void tileDiscarded(String tileId) {
		// N/A
	}

	@Override
	public void tilePlaced(Tile tile) {
		// N/A
	}

	@Override
	public void dragonMoved(Position p) {
		// N/A
	}

	@Override
	public void fairyMoved(Position p) {
		// N/A
	}

	@Override
	public void towerIncreased(Position p, Integer height) {
		// N/A
	}

	@Override
	public void tunnelPiecePlaced(Player player, Position p, Location d,
			boolean isSecondPiece) {
		// N/A
	}

	@Override
	public void completed(Completable feature, CompletableScoreContext ctx) {
		// N/A
	}

	@Override
	public void scored(Feature feature, int points, String label,
			Meeple meeple, boolean isFinal) {
		// N/A
	}

	@Override
	public void scored(Position position, Player player, int points,
			String label, boolean isFinal) {
		// N/A
	}

	@Override
	public void deployed(Meeple meeple) {
		// N/A
	}

	@Override
	public void undeployed(Meeple meeple) {
		// N/A
	}

	@Override
	public void gameOver() {
		closeGame(true);
	}

	@Override
	public void bridgeDeployed(Position pos, Location loc) {
		// N/A
	}

	@Override
	public void castleDeployed(Castle castle1, Castle castle2) {
		// N/A
	}
}
