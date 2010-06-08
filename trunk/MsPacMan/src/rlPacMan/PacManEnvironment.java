package rlPacMan;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Arrays;

import jess.JessException;
import jess.Rete;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;

import relationalFramework.ActionChoice;
import relationalFramework.ObjectObservations;
import relationalFramework.Policy;
import relationalFramework.PolicyActor;
import relationalFramework.PolicyGenerator;
import relationalFramework.StateSpec;

public class PacManEnvironment implements EnvironmentInterface {
	public static int playerDelay_ = 0;
	private Rete rete_;
	private PacMan environment_;
	private int prevScore_;
	private GameModel model_;
	private int[][] distanceGrid_;
	private SortedSet<Junction> pacJunctions_;
	private Point gridStart_;

	@Override
	public void env_cleanup() {
		environment_ = null;
	}

	@Override
	public String env_init() {
		environment_ = new PacMan();
		environment_.init();

		model_ = environment_.getGameModel();

		// Initialise the observations
		resetDistanceGrid();

		try {
			Thread.sleep(512);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public String env_message(String arg0) {
		if (arg0.equals("maxSteps")) {
			return 1000000 + "";
		} else if (arg0.equals("freeze")) {
			PolicyGenerator.getInstance().freeze(true);
			return null;
		} else if (arg0.equals("unfreeze")) {
			PolicyGenerator.getInstance().freeze(false);
			return null;
		} else if ((arg0.length() > 7)
				&& (arg0.substring(0, 6).equals("simple"))) {
			// PacMan simplified by removing certain aspects of it.
			String param = arg0.substring(7);
			StateSpec.reinitInstance(param);
			if (param.equals("noDots")) {
				model_.noDots_ = true;
			} else if (param.equals("noPowerDots")) {
				model_.noPowerDots_ = true;
			}
		} else {
			try {
				int delay = Integer.parseInt(arg0);
			} catch (Exception e) {

			}
		}
		return null;
	}

	@Override
	public Observation env_start() {
		resetEnvironment();

		// Run the optimal policy if it hasn't yet been run
		if (!PolicyGenerator.getInstance().hasPreGoal()) {
			optimalPolicy();
		}

		// If we're not in full experiment mode, redraw the scene.
		if (!environment_.experimentMode_) {
			environment_.m_gameUI.m_bRedrawAll = true;
			environment_.m_gameUI.repaint();
			environment_.m_topCanvas.repaint();
		} else {
			environment_.m_gameUI.update(null);
			environment_.m_gameUI.m_bRedrawAll = false;
		}

		return calculateObservations(rete_);
	}

	@Override
	public Reward_observation_terminal env_step(Action arg0) {
		// Letting the thread 'sleep', so that the game still runs.
		try {
			if (!environment_.experimentMode_)
				Thread.sleep(playerDelay_);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Applying the action (up down left right or nothing)
		ActionChoice actions = (ActionChoice) ObjectObservations.getInstance().objectArray[0];
		environment_.simulateKeyPress(chooseLowAction(actions.getActions())
				.getKey());

		synchronized (environment_) {
			int i = 0;
			while ((i < model_.m_player.m_deltaMax * 2 - 1)
					|| (!model_.isLearning())) {
				environment_.tick(false);
				i++;
			}
		}

		// If we're not in full experiment mode, redraw the scene.
		if (!environment_.experimentMode_) {
			drawActions(actions.getActions());
			environment_.m_gameUI.m_bRedrawAll = true;
			environment_.m_gameUI.repaint();
			environment_.m_topCanvas.repaint();
			environment_.m_bottomCanvas.repaint();
		} else {
			environment_.m_gameUI.update(null);
			environment_.m_bottomCanvas.setActionsList(null);
		}
		environment_.m_gameUI.m_bRedrawAll = false;

		Observation obs = calculateObservations(rete_);
		Reward_observation_terminal rot = new Reward_observation_terminal(
				calculateReward(), obs, isTerminal(obs));
		return rot;
	}

	/**
	 * Runs the optimal policy until a pre-goal is obtained.
	 */
	private void optimalPolicy() {
		Policy optimalPolicy = StateSpec.getInstance().getOptimalPolicy();

		// Run the policy through the environment until goal is satisfied.
		while (!PolicyGenerator.getInstance().hasPreGoal()) {
			PolicyActor optimalAgent = new PolicyActor();
			ObjectObservations.getInstance().objectArray = new Policy[] { optimalPolicy };
			optimalAgent.agent_message("Optimal");
			optimalAgent.agent_message("Policy");
			Action act = optimalAgent.agent_start(calculateObservations(rete_));
			// Loop until the task is complete
			Reward_observation_terminal rot = env_step(act);
			while ((rot == null) || !rot.isTerminal()) {
				optimalAgent.agent_step(rot.r, rot.o);
				rot = env_step(act);
			}

			// Form the pre-goal.
			if (!ObjectObservations.getInstance().objectArray[0]
					.equals(ObjectObservations.NO_PRE_GOAL)
					&& !PolicyGenerator.getInstance().hasPreGoal())
				optimalAgent.agent_message("formPreGoal");

			// Return the state to normal
			resetEnvironment();
		}
	}

	/**
	 * Resets the environment back to normal.
	 */
	public void resetEnvironment() {
		boolean noDots = model_.noDots_;
		boolean noPowerDots = model_.noPowerDots_;

		environment_.reinit();

		model_ = environment_.getGameModel();
		model_.noDots_ = noDots;
		model_.noPowerDots_ = noPowerDots;
		rete_ = StateSpec.getInstance().getRete();

		// Initialise the observations
		resetDistanceGrid();

		prevScore_ = 0;
		gridStart_ = null;

		// Letting the thread 'sleep' when not experiment mode, so it's
		// watchable for humans.
		try {
			rete_.reset();
			if (!environment_.experimentMode_)
				Thread.sleep(playerDelay_);
		} catch (Exception e) {
			e.printStackTrace();
		}
		model_.m_state = GameModel.STATE_NEWGAME;
		environment_.m_gameUI.m_bDrawPaused = false;

		synchronized (environment_) {
			while (!model_.isLearning()) {
				environment_.tick(false);
			}
		}
	}

	/**
	 * Draws the agent's action switch.
	 * 
	 * @param actions
	 *            The agent's current actions. Should always be the same size
	 *            with possible null elements.
	 */
	private void drawActions(ArrayList<List<String>> actions) {
		String[] actionList = new String[actions.size()];
		for (int i = 0; i < actions.size(); i++) {
			StringBuffer buffer = new StringBuffer("[" + (i + 1) + "]: ");
			// If the action is null
			if ((actions.get(i) == null) || (actions.get(i).isEmpty())) {
				buffer.append("null");
			} else {
				buffer.append(actions.get(i));
			}
			actionList[i] = buffer.toString();
		}
		environment_.m_bottomCanvas.setActionsList(actionList);
	}

	/**
	 * Chooses a direction based off the chosen high actions to follow.
	 * 
	 * @param actions
	 *            The prioritised actions the agent will take, joined together
	 *            into a weighted singular direction to take.
	 * @return The low action to use.
	 */
	private PacManLowAction chooseLowAction(ArrayList<List<String>> actions) {
		// Find the valid directions
		ArrayList<PacManLowAction> directions = new ArrayList<PacManLowAction>();
		Point blag = new Point();
		int x = model_.m_player.m_locX;
		int y = model_.m_player.m_locY;
		if (Thing.getDestination(Thing.UP, x, y, blag, model_))
			directions.add(PacManLowAction.UP);
		if (Thing.getDestination(Thing.DOWN, x, y, blag, model_))
			directions.add(PacManLowAction.DOWN);
		if (Thing.getDestination(Thing.LEFT, x, y, blag, model_))
			directions.add(PacManLowAction.LEFT);
		if (Thing.getDestination(Thing.RIGHT, x, y, blag, model_))
			directions.add(PacManLowAction.RIGHT);
		double inverseDirs = 1d / directions.size();

		// Compile the state
		Object[] stateObjs = { model_.m_ghosts, model_.m_fruit, distanceGrid_ };
		PacManState state = new PacManState(stateObjs);

		// Run through each action, until a clear singular direction is arrived
		// upon.
		for (int i = 0; i < actions.size(); i++) {
			double[] directionVote = new double[PacManLowAction.values().length];
			double best = 0;
			double worst = 0;
			// Find the individual distance weighting and direction of each
			// action in the ArrayList.
			for (String action : actions.get(i)) {
				// For each rule, a list of actions are returned
				// double weighting = 1.0 / (i + 1);
				if (actions.get(i) != null) {
					WeightedDirection weightedDir = ((PacManStateSpec) StateSpec
							.getInstance()).applyAction(action, state);

					// Use a linearly decreasing weight and the object proximity
					double weighting = weightedDir.getWeight();
					byte dir = (byte) Math.abs(weightedDir.getDirection());
					// byte oppositeDir = (byte) (((dir % 2) == 1) ? dir + 1 :
					// dir - 1);
					if (weightedDir.getDirection() > 0) {
						directionVote[dir] += weighting;
					} else if (weightedDir.getDirection() < 0) {
						directionVote[dir] -= weighting;
					}

					// Recording best and worst
					best = Math.max(best, directionVote[dir]);
					worst = Math.min(worst, directionVote[dir]);
				}
			}

			// Normalise the directionVote and remove any directions not
			// significantly weighted.
			ArrayList<PacManLowAction> backupDirections = new ArrayList<PacManLowAction>(
					directions);
			for (int d = 0; d < directionVote.length; d++) {
				directionVote[d] = (directionVote[d] - worst) / (best - worst);
				// If the vote is less than 1 - # valid directions, then remove
				// it.
				if (directionVote[d] <= 1d - inverseDirs)
					directions.remove(PacManLowAction.values()[d]);

				// Resetting the direction vote
				directionVote[d] = 0;
			}

			// If only a single direction left, return
			if (directions.size() == 1)
				return directions.get(0);

			if (directions.isEmpty())
				directions = backupDirections;
		}

		// Choose the first valid direction available.
		return directions.get(0);
	}

	/**
	 * Calculates the reward. At the moment, reward is purely based on actual
	 * game reward, so no negative rewards.
	 * 
	 * @return The reward based on the score.
	 */
	private double calculateReward() {
		int scoreDiff = model_.m_player.m_score - prevScore_;
		prevScore_ += scoreDiff;
		return scoreDiff;
	}

	/**
	 * Checks if the episode has terminated.
	 * 
	 * @param obs
	 * 
	 * @return True if Game over or level complete, false otherwise.
	 */
	private int isTerminal(Observation obs) {
		int state = model_.m_state;
		if (state == GameModel.STATE_GAMEOVER) {
			ObjectObservations.getInstance().setNoPreGoal();
			return 1;
		}
		if (StateSpec.getInstance().isGoal(rete_))
			return 1;
		return 0;
	}

	/**
	 * A method for calculating observations about the current PacMan state.
	 * These are put into the Rete object, and placed into the
	 * ObjectObservations object.
	 * 
	 * @param rete
	 *            The rete object to add observations to.
	 * @return An observation of the current state
	 */
	private Observation calculateObservations(Rete rete) {
		try {
			rete.reset();

			// Player
			rete_.eval("(assert (pacman player))");

			// Calculate the distances and junctions
			pacJunctions_ = searchMaze(model_.m_player);

			// Ghosts
			for (Ghost ghost : model_.m_ghosts) {
				// Don't note ghost if it is running back to hideout or if it is
				// in hideout
				if ((ghost.m_nTicks2Exit <= 0) && (!ghost.m_bEaten)) {
					rete_.eval("(assert (ghost " + ghost + "))");
					// If edible, add assertion
					if (ghost.isEdible()) {
						rete_.eval("(assert (edible " + ghost + "))");
					} else {
						rete_.eval("(assert (aggressive " + ghost + "))");
					}
					// If flashing, add assertion
					if (ghost.isBlinking()) {
						rete_.eval("(assert (blinking " + ghost + "))");
					} else {
						rete_.eval("(assert (nonblinking " + ghost + "))");
					}

					// Distances from pacman to ghost
					distanceAssertions(ghost, ghost.toString());
				}
			}

			// Dots
			for (Dot dot : model_.m_dots.values()) {
				String dotName = "dot_" + dot.m_locX + "_" + dot.m_locY;

				rete_.eval("(assert (dot " + dotName + "))");

				// Distances
				distanceAssertions(dot, dotName);
			}

			// Powerdots
			for (PowerDot powerdot : model_.m_powerdots.values()) {
				String pdotName = "powerDot_" + powerdot.m_locX + "_"
						+ powerdot.m_locY;
				rete_.eval("(assert (powerDot " + pdotName + "))");

				// Distances
				distanceAssertions(powerdot, pdotName);
			}

			// Fruit
			if (model_.m_fruit.isEdible()) {
				rete_.eval("(assert (fruit " + model_.m_fruit + "))");

				// Distances
				distanceAssertions(model_.m_fruit, model_.m_fruit.toString());
			}

			// Score, level, lives, highScore
			rete_.eval("(assert (level " + model_.m_stage + "))");
			rete_.eval("(assert (lives " + model_.m_nLives + "))");
			rete_.eval("(assert (score " + model_.m_player.m_score + "))");
			rete_.eval("(assert (highScore " + model_.m_highScore + "))");

			rete_.run();

			// Adding the valid actions
			ObjectObservations.getInstance().validActions = StateSpec
					.getInstance().generateValidActions(rete_);

		} catch (Exception e) {
			e.printStackTrace();
		}

		// Send the state of the system
		ObjectObservations.getInstance().predicateKB = rete;
		Observation obs = new Observation();
		obs.charArray = ObjectObservations.OBSERVATION_ID.toCharArray();

		return obs;
	}

	/**
	 * Make a distance assertion from the player to an object.
	 * 
	 * @param thing
	 *            The thing as arg2 of the distance.
	 * @param thingName
	 *            The JESS name of the thing.
	 * @throws JessException
	 *             If Jess goes wrong.
	 */
	private void distanceAssertions(PacPoint thing, String thingName)
			throws JessException {
		if (distanceGrid_[thing.m_locX][thing.m_locY] < Integer.MAX_VALUE / 2)
			;
		rete_.eval("(assert (distance" + thing.getClass().getSimpleName()
				+ " player " + thingName + " "
				+ distanceGrid_[thing.m_locX][thing.m_locY] + "))");
	}

	/**
	 * Searches the maze for observations. Does this by expanding outwards in
	 * junctions, recording the distance to each.
	 * 
	 * @param thing
	 *            The searching from.
	 * @return A mapping of minimal distances to junctions, given by points.
	 */
	public SortedSet<Junction> searchMaze(Thing thing) {
		SortedSet<Junction> closeJunctions = new TreeSet<Junction>();
		Collection<Dot> dots = model_.m_dots.values();

		Point playerLoc = new Point(thing.m_locX, thing.m_locY);

		// If Pacman has moved, update the distance grid.
		if ((gridStart_ == null) || (!gridStart_.equals(playerLoc))) {
			gridStart_ = playerLoc;
			// Redoing the observations
			resetDistanceGrid();

			// Update the distance grid
			Set<Point> knownJunctions = new HashSet<Point>();
			// Check for junctions here.
			Set<Junction> thisLoc = isJunction(playerLoc, 0);
			if (thisLoc != null) {
				Point p = thisLoc.iterator().next().getLocation();
				knownJunctions.add(p);
			}

			SortedSet<Junction> junctionStack = new TreeSet<Junction>();
			// Add the initial junction points to the stack
			junctionStack.add(new Junction(playerLoc, Thing.UP, 0));
			junctionStack.add(new Junction(playerLoc, Thing.DOWN, 0));
			junctionStack.add(new Junction(playerLoc, Thing.LEFT, 0));
			junctionStack.add(new Junction(playerLoc, Thing.RIGHT, 0));
			distanceGrid_[playerLoc.x][playerLoc.y] = 0;

			// Keep following junctions until all have been found
			while (!junctionStack.isEmpty()) {
				Junction point = junctionStack.first();
				junctionStack.remove(point);

				Set<Junction> nextJunction = searchToJunction(point,
						knownJunctions);
				junctionStack.addAll(nextJunction);

				// Checking for the immediate junctions
				if ((!nextJunction.isEmpty())
						&& (point.getLocation().equals(playerLoc))) {
					closeJunctions.add(nextJunction.iterator().next());
				}
			}

			// Calculate the centre of the dots
			int dotX = 0;
			int dotY = 0;
			int i = 0;
			for (Dot dot : dots) {
				i++;
				dotX += dot.m_locX;
				dotY += dot.m_locY;
			}
			// Special case if player eats all dots
			if (i == 0) {
				i = 1;
				dotX = model_.m_player.m_locX;
				dotY = model_.m_player.m_locY;
			}
			return closeJunctions;
		}
		return pacJunctions_;
	}

	/**
	 * A method for searching for the shortest distance from junction to
	 * junction.
	 * 
	 * @param startingPoint
	 *            The starting point for the junction search.
	 * @param knownJunctions
	 *            The known junctions.
	 * @return The set of starting points for the next found junction or an
	 *         empty set.
	 */
	private Set<Junction> searchToJunction(Junction startingPoint,
			Set<Point> knownJunctions) {
		byte direction = startingPoint.getDirection();
		int x = startingPoint.getLocation().x;
		int y = startingPoint.getLocation().y;
		int distance = startingPoint.getDistance();

		// Checking for an invalid request to move
		if (!Thing.getDestination(direction, x, y, new Point(), model_)) {
			return new HashSet<Junction>();
		}

		// Move in the direction
		byte oldDir = 0;
		Set<Junction> isJunct = null;
		boolean changed = false;
		do {
			changed = false;
			switch (direction) {
			case Thing.UP:
				y--;
				oldDir = Thing.DOWN;
				break;
			case Thing.DOWN:
				y++;
				oldDir = Thing.UP;
				break;
			case Thing.LEFT:
				x--;
				oldDir = Thing.RIGHT;
				break;
			case Thing.RIGHT:
				x++;
				oldDir = Thing.LEFT;
				break;
			}
			// Modulus the coordinates for the warp paths
			x = (x + model_.m_gameSizeX) % model_.m_gameSizeX;
			y = (y + model_.m_gameSizeY) % model_.m_gameSizeY;

			// Note the distance
			distance++;
			if (distance < distanceGrid_[x][y]) {
				changed = true;
				distanceGrid_[x][y] = distance;
			}

			// Check if the new position is a junction
			isJunct = isJunction(new Point(x, y), distance);

			// If not, find the next direction
			if (isJunct == null) {
				for (byte d = 1; d <= 4; d++) {
					// If the direction isn't the direction came from
					if (d != oldDir) {
						if (Thing.getDestination(d, x, y, new Point(), model_)) {
							direction = d;
							break;
						}
					}
				}
			}
		} while (isJunct == null);

		// Post-process the junction to remove the old direction scan
		Junction removal = null;
		for (Junction jp : isJunct) {
			if (jp.getDirection() == oldDir) {
				removal = jp;
				break;
			}
		}
		isJunct.remove(removal);

		// Check if the junction has been found
		Point junction = isJunct.iterator().next().getLocation();
		if (knownJunctions.contains(junction)) {
			if (changed)
				return isJunct;
		} else {
			knownJunctions.add(junction);
			return isJunct;
		}
		return new HashSet<Junction>();
	}

	/**
	 * Checks if the coordinates are a junction. If they are, returns a bitwise
	 * representation of the directions to go.
	 * 
	 * @param loc
	 *            The location of the possible junction.
	 * @param distance
	 *            The current distance of the junction.
	 * @return A list of the possible directions the junction goes or null if no
	 *         junction.
	 */
	private Set<Junction> isJunction(Point loc, int distance) {
		Set<Junction> dirs = new HashSet<Junction>();
		if ((model_.m_gameState[loc.x][loc.y] & GameModel.GS_NORTH) == 0)
			dirs.add(new Junction(loc, Thing.UP, distance));
		if ((model_.m_gameState[loc.x][loc.y] & GameModel.GS_SOUTH) == 0)
			dirs.add(new Junction(loc, Thing.DOWN, distance));
		if ((model_.m_gameState[loc.x][loc.y] & GameModel.GS_EAST) == 0)
			dirs.add(new Junction(loc, Thing.RIGHT, distance));
		if ((model_.m_gameState[loc.x][loc.y] & GameModel.GS_WEST) == 0)
			dirs.add(new Junction(loc, Thing.LEFT, distance));

		if (dirs.size() > 2)
			return dirs;
		return null;
	}

	/**
	 * Resets the observation and distance grid array.
	 */
	private void resetDistanceGrid() {
		distanceGrid_ = new int[model_.m_gameSizeX][model_.m_gameSizeY];
		for (int x = 0; x < distanceGrid_.length; x++)
			Arrays.fill(distanceGrid_[x], Integer.MAX_VALUE);
	}

	/**
	 * @return the model_
	 */
	public GameModel getModel() {
		return model_;
	}

	public int[][] getDistanceGrid() {
		return distanceGrid_;
	}

	public void resetGridStart() {
		gridStart_ = null;
	}
}
