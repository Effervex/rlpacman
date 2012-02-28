package jCloisterZone;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import relationalFramework.PolicyActions;
import relationalFramework.RelationalPredicate;
import rrlFramework.RRLExperiment;
import util.Pair;

import jess.JessException;
import jess.Rete;

import com.jcloisterzone.Expansion;
import com.jcloisterzone.Player;
import com.jcloisterzone.UserInterface;
import com.jcloisterzone.action.CaptureAction;
import com.jcloisterzone.action.MeepleAction;
import com.jcloisterzone.action.PlayerAction;
import com.jcloisterzone.board.Board;
import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.board.Tile;
import com.jcloisterzone.event.GameEventListener;
import com.jcloisterzone.feature.City;
import com.jcloisterzone.feature.Cloister;
import com.jcloisterzone.feature.Completable;
import com.jcloisterzone.feature.Farm;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.feature.Road;
import com.jcloisterzone.feature.visitor.FindMaster;
import com.jcloisterzone.feature.visitor.score.AbstractScoreContext;
import com.jcloisterzone.feature.visitor.score.CityScoreContext;
import com.jcloisterzone.feature.visitor.score.CompletableScoreContext;
import com.jcloisterzone.feature.visitor.score.FarmScoreContext;
import com.jcloisterzone.feature.visitor.score.PositionCollectingScoreContext;
import com.jcloisterzone.feature.visitor.score.RoadScoreContext;
import com.jcloisterzone.figure.Follower;
import com.jcloisterzone.figure.Meeple;
import com.jcloisterzone.game.CustomRule;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.PlayerSlot;
import com.jcloisterzone.game.Snapshot;
import com.jcloisterzone.game.PlayerSlot.SlotType;
import com.jcloisterzone.game.phase.ActionPhase;
import com.jcloisterzone.game.phase.GameOverPhase;
import com.jcloisterzone.game.phase.Phase;
import com.jcloisterzone.game.phase.TilePhase;

public class CarcassonneRelationalWrapper implements GameEventListener,
		UserInterface {
	public static final String NO_ACTION = "No Action";
	/** The available actions to take per action phase. */
	private List<PlayerAction> actions_;
	/** Features that have already been asserted this iteration. */
	private Collection<Feature> assertedFeatures_ = new HashSet<Feature>();
	/** Cache of cities for farm evaluation. */
	private Map<City, CityScoreContext> cityCache_ = new HashMap<City, CityScoreContext>();
	/** The current game environment. */
	private Game environment_;
	/** A map for storing meeple feature locations. */
	private Map<String, Feature> featureMap_ = new HashMap<String, Feature>();
	/** A map for storing valid locations. */
	private Map<String, Position> locationMap_ = new HashMap<String, Position>();
	/** The count of tiles for unique IDs. */
	private int tileCount_ = 0;
	/** The available tile positions per tile phase. */
	private Map<Position, Set<Rotation>> tilePositions_;
	/** If this thread is safe to execute. */
	private volatile CountDownLatch readyToExecute_;

	/**
	 * Asserts a city.
	 * 
	 * @param rete
	 *            The rete object to assert/retract the facts.
	 * @param road
	 *            The (portion of) city to assert.
	 */
	private String assertCity(Rete rete, City city) throws JessException {
		CityScoreContext cityContext = new CityScoreContext(environment_);
		city.walk(cityContext);
		Feature master = cityContext.getMasterFeature();
		String cityName = formatFeature(master);
		if (!assertedFeatures_.contains(master)) {
			rete.assertString("(city " + cityName + ")");

			// Completable assertions
			assertCompletable(rete, cityName, cityContext);
			assertedFeatures_.add(master);
		}
		return cityName;
	}

	/**
	 * Asserts a cloister.
	 * 
	 * @param rete
	 *            The rete object to assert/retract the facts.
	 * @param road
	 *            The cloister to assert.
	 */
	private String assertCloister(Rete rete, Cloister cloister)
			throws JessException {
		String cloisterName = formatFeature(cloister);
		rete.assertString("(cloister " + cloisterName + ")");
		// Don't assert other information about unplaced cloisters.
		if (cloister.getNeighbouring() != null) {
			if (cloister.isFeatureCompleted())
				rete.assertString("(completed " + cloisterName + ")");
			else {
				rete.assertString("(worth " + cloisterName + " "
						+ cloister.getScoreContext().getPoints() + ")");
			}

			// Assert surrounding zone locations
			Position cloisterLoc = cloister.getTile().getPosition();
			for (Position adjDiag : Position.ADJACENT_AND_DIAGONAL.values()) {
				Position p = cloisterLoc.add(adjDiag);
				String loc = assertPosition(rete, p);
				rete.assertString("(cloisterZone " + loc + " " + cloisterName
						+ ")");
			}
		}
		return cloisterName;
	}

	/**
	 * Asserts components of a completable feature.
	 * 
	 * @param rete
	 *            The rete object to assert/retract the facts.
	 * @param featureName
	 *            The name of the feature.
	 * @param context
	 *            The context (details) of the feature.
	 * @throws JessException
	 *             Should something go awry...
	 */
	private void assertCompletable(Rete rete, String featureName,
			PositionCollectingScoreContext context) throws JessException {
		if (context.isCompleted())
			rete.assertString("(completed " + featureName + ")");
		else {
			// Assert open edges
			rete.assertString("(open " + featureName + " "
					+ context.getOpenEdges() + ")");

			assertWorthAndMeeples(rete, featureName, context);
		}
	}

	/**
	 * Asserts a farm.
	 * 
	 * @param rete
	 *            The rete object to assert/retract the facts.
	 * @param road
	 *            The (portion of) farm to assert.
	 */
	private String assertFarm(Rete rete, Farm farm) throws JessException {
		FarmScoreContext farmContext = new FarmScoreContext(environment_);
		farmContext.setCityCache(cityCache_);
		farm.walk(farmContext);
		Feature master = farmContext.getMasterFeature();
		String farmName = formatFeature(master);
		if (!assertedFeatures_.contains(master)) {
			rete.assertString("(farm " + farmName + ")");

			// Assert player meeple control
			for (Player p : farmContext.getMajorOwners()) {
				rete.assertString("(controls " + formatPlayer(p) + " "
						+ farmName + ")");
			}
			assertWorthAndMeeples(rete, farmName, farmContext);
			assertedFeatures_.add(master);
		}
		return farmName;
	}

	/**
	 * Asserts a feature of a tile.
	 * 
	 * @param rete
	 *            The rete object to assert/retract the facts.
	 * @param terrain
	 *            The feature to assert.
	 * @param tileName
	 *            The name of the tile.
	 * @throws JessException
	 *             Should something go awry...
	 */
	private void assertFeature(Rete rete, Feature terrain, String tileName)
			throws JessException {
		String featureName = null;
		if (terrain instanceof Road) {
			featureName = assertRoad(rete, (Road) terrain);
		} else if (terrain instanceof City) {
			featureName = assertCity(rete, (City) terrain);
		} else if (terrain instanceof Farm) {
			featureName = assertFarm(rete, (Farm) terrain);
		} else if (terrain instanceof Cloister) {
			featureName = assertCloister(rete, (Cloister) terrain);
		}

		// Assert the feature in the tile
		rete.assertString("(tileContains " + tileName + " " + featureName + ")");

		// Assert the tile edges
		Location tileLocation = terrain.getLocation();
		for (Location side : Location.sides()) {
			// Check the side location.
			if (side.isPartOf(tileLocation)) {
				// Name the edge.
				String edge = null;
				if (side.equals(Location.N))
					edge = "north";
				else if (side.equals(Location.E))
					edge = "east";
				else if (side.equals(Location.S))
					edge = "south";
				else if (side.equals(Location.W))
					edge = "west";

				rete.assertString("(tileEdge " + tileName + " " + edge + " "
						+ featureName + ")");
			}
		}
	}

	/**
	 * Asserts phase specific assertions.
	 * 
	 * @param rete
	 *            The rete object to assert/retract to.
	 * @param currentTile
	 *            The current tile being placed/meeple'd
	 * @param tileStr
	 *            The name of the tile.
	 * @throws JessException
	 *             Should something go awry...
	 */
	private void assertPhaseSpecific(Rete rete) throws JessException {
		Phase phase = environment_.getPhase();

		// Assert current tile features.
		Tile currentTile = environment_.getTilePack().getCurrentTile();
		String tileStr = assertTile(rete, currentTile, false);
		rete.assertString("(currentTile " + tileStr + ")");

		// Phase specific assertions
		if (phase instanceof TilePhase) {
			// Board positions
			for (Position pos : tilePositions_.keySet()) {
				// Assert the position
				String loc = assertPosition(rete, pos);
				Set<Rotation> rotations = tilePositions_.get(pos);
				for (Rotation rot : rotations) {
					rete.assertString("(validLoc " + loc + " " + rot + ")");
				}

				locationMap_.put(loc, pos);
			}
		} else if (phase instanceof ActionPhase) {
			// Assert meeple features.
			for (PlayerAction pa : actions_) {
				if (pa instanceof MeepleAction) {
					MeepleAction ma = (MeepleAction) pa;
					// For every meeple location
					for (Location loc : ma.getSites().get(
							currentTile.getPosition())) {
						// Find the feature master
						Feature terrain = currentTile.getFeature(loc);
						FindMaster master = new FindMaster();
						terrain.walk(master);
						String masterStr = formatFeature(master
								.getMasterFeature());

						// Assert the meeple loc.
						rete.assertString("(meepleLoc " + tileStr + " "
								+ masterStr + ")");

						featureMap_.put(masterStr, terrain);
					}
				}
			}
		}
	}

	/**
	 * Formats a position as a JESS constant.
	 * 
	 * @param rete
	 *            The rete object to assert/retract the facts.
	 * @param position
	 *            The position to format.
	 * @return A String version of the position.
	 */
	private String assertPosition(Rete rete, Position position)
			throws JessException {
		String loc = "loc_" + position.x + "_" + position.y;
		rete.assertString("(location " + loc + ")");
		rete.assertString("(locationXY " + loc + " " + position.x + " "
				+ position.y + ")");

		// Assert the number of surrounding tiles
		int numAdjacentAndDiagonal = environment_.getBoard()
				.getAllNeigbourTiles(position).size();
		rete.assertString("(numSurroundingTiles " + loc + " "
				+ numAdjacentAndDiagonal + ")");

		return loc;
	}

	/**
	 * Asserts a road.
	 * 
	 * @param rete
	 *            The rete object to assert/retract the facts.
	 * @param road
	 *            The (portion of) road to assert.
	 */
	private String assertRoad(Rete rete, Road road) throws JessException {
		RoadScoreContext roadContext = new RoadScoreContext(environment_);
		road.walk(roadContext);
		Feature master = roadContext.getMasterFeature();
		String roadName = formatFeature(master);
		if (!assertedFeatures_.contains(master)) {
			rete.assertString("(road " + roadName + ")");

			// Completable assertions
			assertCompletable(rete, roadName, roadContext);
			assertedFeatures_.add(master);
		}
		return roadName;
	}

	/**
	 * Asserts a tile (and its edges)
	 * 
	 * @param rete
	 *            The rete object to assert/retract the facts.
	 * @param tile
	 *            The tile to assert
	 * @param assertPos
	 *            If the tile location should be asserted.
	 * @throws JessException
	 *             Should something go awry...
	 */
	private String assertTile(Rete rete, Tile tile, boolean assertPos)
			throws JessException {
		String tileStr = tile.toString();
		tileStr = tileStr.substring(0, tileStr.indexOf('(')) + (tileCount_++);
		rete.assertString("(tile " + tileStr + ")");

		if (assertPos) {
			// Assert tile location
			Position tilePos = tile.getPosition();
			rete.assertString("(tileLocation " + tileStr + " "
					+ assertPosition(rete, tilePos) + ")");
		}

		// Assert tile features
		for (Feature terrain : tile.getFeatures()) {
			assertFeature(rete, terrain, tileStr);
		}

		return tileStr;
	}

	/**
	 * Assert the worth of a feature and the meeple(s) on it.
	 * 
	 * @param rete
	 *            The rete object to assert/retract to.
	 * @param featureName
	 *            The name fo the feature.
	 * @param context
	 *            The context of the feature.
	 * @throws JessException
	 *             Should something fo awry...
	 */
	private void assertWorthAndMeeples(Rete rete, String featureName,
			AbstractScoreContext context) throws JessException {
		// Assert terrain worth
		int worth = 0;
		if (context instanceof PositionCollectingScoreContext)
			worth = ((PositionCollectingScoreContext) context).getPoints(true);
		else if (context instanceof FarmScoreContext)
			worth = ((FarmScoreContext) context).getPoints(null);
		rete.assertString("(worth " + featureName + " " + worth + ")");

		// Assert player meeple control
		for (Player p : context.getMajorOwners()) {
			rete.assertString("(controls " + formatPlayer(p) + " "
					+ featureName + ")");
		}

		// Assert placedMeeples
		Map<Player, Integer> meepleCount = new HashMap<Player, Integer>();
		for (Follower follower : context.getFollowers()) {
			Player player = follower.getPlayer();
			Integer count = (meepleCount.get(player) == null) ? 0 : meepleCount
					.get(player);
			meepleCount.put(player, count + 1);
		}
		for (Player p : meepleCount.keySet())
			rete.assertString("(placedMeeples " + formatPlayer(p) + " "
					+ meepleCount.get(p) + " " + featureName + ")");
	}

	/**
	 * Formats the name of a feature.
	 * 
	 * @param feature
	 *            The feature to format.
	 * @return A JESS-readable String representing the feature.
	 */
	private String formatFeature(Feature feature) {
		return feature.toString().replaceAll("@", "");
	}

	private String formatPlayer(Player p) {
		return p.getNick().trim().replaceAll(" ", "_");
	}

	/**
	 * Asserts the facts of the state.
	 * 
	 * @param rete
	 *            The rete object to assert/retract to.
	 * @param goalArgs
	 *            Any goal arguments provided.
	 * @throws Exception
	 *             Should something go awry...
	 */
	public void assertStateFacts(Rete rete, Game game) throws Exception {
		// Sleep until ready to execute
		if (sleepUntilReady(game))
			return;

		// Other initialisations
		assertedFeatures_.clear();
		tileCount_ = 0;
		environment_ = game;
		featureMap_.clear();

		// Assert board features.
		Board board = environment_.getBoard();
		// Assert all tiles and terrain
		for (Tile tile : board.getAllTiles()) {
			assertTile(rete, tile, true);
		}
		// Assert remaining tiles.
		int tilePackSize = environment_.getTilePack().size();
		rete.assertString("(tilesLeft " + tilePackSize + ")");

		// Assert player scores
		for (Player p : environment_.getAllPlayers()) {
			String pName = formatPlayer(p);
			rete.assertString("(player " + pName + ")");
			rete.assertString("(score " + pName + " " + p.getPoints() + ")");
			// Assert player meeple count
			rete.assertString("(meeplesLeft " + pName + " "
					+ p.getUndeployedFollowers().size() + ")");
			if (p == environment_.getTurnPlayer())
				rete.assertString("(currentPlayer " + pName + ")");
		}

		try {
			assertPhaseSpecific(rete);
		} catch (NullPointerException ne) {
			ne.printStackTrace();
			System.out.println(game.getPhase());
		}
	}

	/**
	 * Sleeps (yields thread) until the game is ready to accept actions.
	 * 
	 * @param game
	 *            The game.
	 * @return True if the game is over.
	 */
	private boolean sleepUntilReady(Game game) {
		// Wait for the game.
		try {
			readyToExecute_.await();
		} catch (Exception e) {
		}

		// If the game is over, return true.
		if (game.getPhase() instanceof GameOverPhase)
			return true;

		// Restart the counter.
		readyToExecute_ = new CountDownLatch(1);
		return false;
	}

	/**
	 * Grounds the relational action into a Carcassonne action.
	 * 
	 * TODO Apply the action logically too
	 * 
	 * @param actions
	 *            The actions to ground
	 * @param game
	 *            The current state of the game.
	 * @return Either a location and rotation to place a tile, or a location to
	 *         place a meeple (or null for no action).
	 */
	public Object groundActions(PolicyActions actions, Game game) {
		Phase phase = environment_.getPhase();
		RelationalPredicate action = actions.getFirstRandomAction();
		if (phase instanceof TilePhase) {
			// Exit the game if no action choice made.
			if (action == null) {
				readyToExecute_.countDown();

				if (RRLExperiment.debugMode_) {
					System.out.println("Tile phase: NO ACTION SELECTED");
				}
				return null;
			}

			// Get position and rotation
			String[] args = action.getArguments();
			Position pos = locationMap_.get(args[1]);
			Rotation rot = Rotation.valueOf(args[2]);

			if (RRLExperiment.debugMode_) {
				System.out.println("Tile phase: " + action.toString());
			}
			return new Pair<Position, Rotation>(pos, rot);
		} else if (phase instanceof ActionPhase) {
			// If no action, then simply don't place a meeple
			if (action == null) {
				if (RRLExperiment.debugMode_) {
					System.out.println("Action phase: No meeple placed");
				}
				return NO_ACTION;
			}

			// Get the terrain
			String[] args = action.getArguments();

			if (RRLExperiment.debugMode_) {
				System.out.println("Action phase: " + action.toString());
			}
			return featureMap_.get(args[2]);
		}
		return null;
	}

	@Override
	public void selectAbbeyPlacement(Set<Position> positions) {
		// N/A
	}

	@Override
	public synchronized void selectAction(List<PlayerAction> actions) {
		if (environment_.getTurnPlayer().getSlot().getType() == SlotType.PLAYER) {
			actions_ = actions;
			readyToExecute_.countDown();
		}
	}

	@Override
	public void selectDragonMove(Set<Position> positions, int movesLeft) {
		// N/A
	}

	@Override
	public synchronized void selectTilePlacement(
			Map<Position, Set<Rotation>> placements) {
		if (environment_.getTurnPlayer().getSlot().getType() == SlotType.PLAYER) {
			tilePositions_ = placements;
			readyToExecute_.countDown();
		}
	}

	@Override
	public void selectTowerCapture(CaptureAction action) {
		// N/A
	}

	public void startState() {
		readyToExecute_ = new CountDownLatch(1);
	}

	@Override
	public void gameOver() {
		readyToExecute_.countDown();
	}


	// ////////////////// UNUSED METHODS ///////////////
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

	public void setGame(Game game) {
		environment_ = game;
	}

}
