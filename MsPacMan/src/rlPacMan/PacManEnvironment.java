package rlPacMan;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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

import relationalFramework.ObjectObservations;
import relationalFramework.Policy;
import relationalFramework.PolicyActor;
import relationalFramework.PolicyGenerator;
import relationalFramework.StateSpec;

public class PacManEnvironment implements EnvironmentInterface {
	public static final int PLAYER_SPEED = 5;
	private Rete rete_;
	private PacMan environment_;
	private int prevScore_;
	private GameModel model_;
	private int[][] distanceGrid_;
	private SortedSet<JunctionPoint> pacJunctions_;
	private Point gridStart_;
	private ArrayList<Double>[] observationFreqs_;

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
		if (arg0.equals("writeFreqs")) {
			writeFreqs();
		}
		if (arg0.equals("maxSteps")) {
			return 1000000 + "";
		}
		if (arg0.equals("freeze")) {
			PolicyGenerator.getInstance().freeze(true);
			return null;
		}
		if (arg0.equals("unfreeze")) {
			PolicyGenerator.getInstance().freeze(false);
			return null;
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
				Thread.sleep(PLAYER_SPEED);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Applying the action (up down left right or nothing)
		String[] actions = (String[]) ObjectObservations.getInstance().objectArray;
		environment_.simulateKeyPress(chooseLowAction(actions).getKey());

		synchronized (environment_) {
			for (int i = 0; i < model_.m_player.m_deltaMax; i++) {
				environment_.tick(false);
			}
		}

		// If we're not in full experiment mode, redraw the scene.
		if (!environment_.experimentMode_) {
			drawActions(actions);
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
				calculateReward(), obs, isTerminal());
		return rot;
	}

	/**
	 * Runs the optimal policy until a pre-goal is obtained.
	 */
	private void optimalPolicy() {
		Policy optimalPolicy = StateSpec.getInstance().getOptimalPolicy();

		// Run the policy through the environment until goal is satisfied.
		PolicyActor optimalAgent = new PolicyActor();
		ObjectObservations.getInstance().objectArray = new Policy[] { optimalPolicy };
		optimalAgent.agent_message("Optimal");
		optimalAgent.agent_message("Policy");
		Action act = optimalAgent.agent_start(calculateObservations(rete_));
		// Loop until the task is complete
		Reward_observation_terminal rot = env_step(act);
		while ((rot == null) || (!rot.isTerminal())) {
			optimalAgent.agent_step(rot.r, rot.o);
			rot = env_step(act);
		}

		// Form the pre-goal.
		if (!PolicyGenerator.getInstance().hasPreGoal())
			optimalAgent.agent_message("formPreGoal");

		// Return the state to normal
		resetEnvironment();
	}

	/**
	 * Resets the environment back to normal.
	 */
	private void resetEnvironment() {
		environment_.reinit();

		model_ = environment_.getGameModel();
		rete_ = StateSpec.getInstance().getRete();

		// Initialise the observations
		resetDistanceGrid();

		prevScore_ = 0;
		gridStart_ = null;

		// Letting the thread 'sleep' when not experiment mode, so it's
		// watchable for humans.
		try {
			if (!environment_.experimentMode_)
				Thread.sleep(PLAYER_SPEED);
		} catch (Exception e) {
			e.printStackTrace();
		}
		model_.m_state = GameModel.STATE_NEWGAME;
		environment_.m_gameUI.m_bDrawPaused = false;

		synchronized (environment_) {
			for (int i = 0; i < model_.m_player.m_deltaMax; i++) {
				environment_.tick(false);
			}
		}
	}

	/**
	 * Draws the agent's action switch.
	 * 
	 * @param actionPriority
	 *            The agent's current actions. Should always be the same size
	 *            with possible null elements.
	 */
	private void drawActions(String[] actionPriority) {
		String[] actionList = new String[actionPriority.length];
		for (int i = 0; i < actionPriority.length; i++) {
			StringBuffer buffer = new StringBuffer("[" + (i + 1) + "]: ");
			// If the action is null
			if ((actionPriority[i] == null) || (actionPriority[i].isEmpty())) {
				buffer.append("null");
			} else {
				buffer.append(actionPriority[i]);
			}
			actionList[i] = buffer.toString();
		}
		environment_.m_bottomCanvas.setActionsList(actionList);
	}

	/**
	 * Chooses a direction based off the chosen high actions to follow.
	 * 
	 * @param actionArray
	 *            The high actions to take, sorted in order from most to least
	 *            priority, being processed into a single low action.
	 * @return The low action to use.
	 */
	private PacManLowAction chooseLowAction(String[] actionArray) {
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

		// Compile the state
		Object[] stateObjs = { model_.m_ghosts, model_.m_fruit, distanceGrid_ };
		PacManState state = new PacManState(stateObjs);

		double[] directionVote = new double[PacManLowAction.values().length];
		for (int i = 0; i < actionArray.length; i++) {
			if (actionArray[i] != null) {
				// Use a linearly decreasing weight for directional voting.
				double weighting = 1 - ((1.0 * i) / actionArray.length);

				Byte direction = ((PacManStateSpec) StateSpec.getInstance())
						.applyAction(actionArray[i], state);
				if (direction > 0)
					directionVote[direction] += weighting;
				else if (direction < 0)
					directionVote[-direction] -= weighting;
			}
		}

		// Find the best direction/s
		ArrayList<PacManLowAction> chosen = new ArrayList<PacManLowAction>();
		chosen.add(PacManLowAction.values()[1]);
		double best = directionVote[1];
		for (int j = 2; j < directionVote.length; j++) {
			// Same value, add
			if (directionVote[j] == best) {
				chosen.add(PacManLowAction.values()[j]);
			} else if (directionVote[j] > best) {
				// Better value, clear
				chosen.clear();
				chosen.add(PacManLowAction.values()[j]);
				best = directionVote[j];
			}
		}

		// Checking for negative direction
		chosen.retainAll(directions);
		if (!chosen.isEmpty())
			directions = chosen;
		else
			System.out.println("What?");

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
	 * @return True if Game over or level complete, false otherwise.
	 */
	private int isTerminal() {
		int state = model_.m_state;
		if (state == GameModel.STATE_GAMEOVER)
			return -1;
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
			SortedSet<JunctionPoint> newJunctions = searchMaze(model_.m_player);
			// If our junctions have changed, remove the old set and add the new
			int closest = Integer.MAX_VALUE;
			Set<String> closePoints = new HashSet<String>();
			if (!newJunctions.equals(pacJunctions_)) {
				for (JunctionPoint newJP : newJunctions) {
					String juncName = "junc_" + newJP.m_locX + "_"
							+ newJP.m_locY;
					rete_.eval("(assert (junction " + juncName + "))");

					closest = distanceAssertions(newJP, juncName, closest, closePoints);
				}
				pacJunctions_ = newJunctions;
			}
			closest = assertClosePoints(closePoints);

			// Ghost
			for (Ghost ghost : model_.m_ghosts) {
				// Don't note ghost if it is running back to hideout or if it is
				// in hideout
				if ((ghost.m_nTicks2Exit <= 0) && (!ghost.m_bEaten)) {
					rete_.eval("(assert (ghost " + ghost + "))");
					// If edible, add assertion
					if (ghost.isEdible())
						rete_.eval("(assert (edible " + ghost + "))");
					// If flashing, add assertion
					if (ghost.isBlinking())
						rete_.eval("(assert (blinking " + ghost + "))");

					// Distances from pacman to ghost
					closest = distanceAssertions(ghost, ghost.toString(), closest,
							closePoints);
				}
			}
			closest = assertClosePoints(closePoints);

			// Dots
			for (Dot dot : model_.m_dots.values()) {
				String dotName = "dot_" + dot.m_locX + "_" + dot.m_locY;
				rete_.eval("(assert (dot " + dotName + "))");

				// Distances
				closest = distanceAssertions(dot, dotName, closest, closePoints);
			}
			closest = assertClosePoints(closePoints);

			// Powerdots
			for (PowerDot powerdot : model_.m_powerdots.values()) {
				String pdotName = "powerDot_" + powerdot.m_locX + "_"
						+ powerdot.m_locY;
				rete_.eval("(assert (dot " + pdotName + "))");

				// Distances
				closest = distanceAssertions(powerdot, pdotName, closest, closePoints);
			}
			closest = assertClosePoints(closePoints);

			// Fruit
			if (model_.m_fruit.isEdible()) {
				rete_.eval("(assert (fruit " + model_.m_fruit + "))");

				// Distances
				closest = distanceAssertions(model_.m_fruit, model_.m_fruit.toString(),
						closest, closePoints);
			}
			closest = assertClosePoints(closePoints);

			// Score, level, lives, highScore
			rete_.eval("(assert (level " + model_.m_stage + "))");
			rete_.eval("(assert (lives " + model_.m_nLives + "))");
			rete_.eval("(assert (score " + model_.m_player.m_score + "))");
			rete_.eval("(assert (highScore " + model_.m_highScore + "))");

			rete_.run();

			// Adding the valid actions
			StateSpec.getInstance().insertValidActions(rete_);

			//rete_.eval("(facts)");

			// Find the maximally safe junctions
			// for (int i = 0; i < model_.m_ghosts.length; i++) {
			// // If the ghost is active and hostile
			// Ghost ghost = model_.m_ghosts[i];
			// if ((ghost.m_nTicks2Exit <= 0) && (!ghost.m_bEaten)) {
			// // Calculate the ghost's distance from each of the player's
			// // junctions
			// if (ghost.m_nTicks2Flee <= 0) {
			// for (JunctionPoint jp : pacJunctions_) {
			// int ghostDist = calculateDistance(ghost, jp
			// .getLocation());
			// int result = ghostDist
			// - distanceGrid_[jp.getLocation().x][jp
			// .getLocation().y];
			// // Looking for the minimally safe junction
			// if ((result > Integer.MIN_VALUE)
			// && (result < jp.getSafety()))
			// jp.setSafety(result);
			// }
			// }
			// }
			// }

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
	 * Asserts the closest points (ghosts, dots, etc) to PacMan.
	 * 
	 * @param closePoints
	 *            The point names to assert.
	 * @return The new value of closest (Integer.MAX_VALUE).
	 */
	private int assertClosePoints(Set<String> closePoints) throws JessException {
		for (String point : closePoints) {
			rete_.eval("(assert (closest player " + point + "))");
		}
		closePoints.clear();
		return Integer.MAX_VALUE;
	}

	/**
	 * Make a distance assertion from the player to an object.
	 * 
	 * @param thing
	 *            The thing as arg2 of the distance.
	 * @param thingName
	 *            The JESS name of the thing.
	 * @param closePoints
	 *            The set of things that are at closest distance from pacman.
	 * @param closest
	 *            The value of the closest thing to pacman.
	 * @throws JessException
	 *             If Jess goes wrong.
	 */
	private int distanceAssertions(PacPoint thing, String thingName,
			int closest, Set<String> closePoints) throws JessException {
		rete_.eval("(assert (distance player " + thingName + " "
				+ distanceGrid_[thing.m_locX][thing.m_locY] + "))");
		if (distanceGrid_[thing.m_locX][thing.m_locY] < closest) {
			closePoints.clear();
			closest = distanceGrid_[thing.m_locX][thing.m_locY];
		}
		if (distanceGrid_[thing.m_locX][thing.m_locY] == closest)
			closePoints.add(thingName);
		return closest;
	}

	/**
	 * Discretises the distance of a thing to a discrete value.
	 * 
	 * @param distance
	 *            The distance between 2 things.
	 * @return A distance metric which discretises distance values.
	 */
	private String discretiseDistance(int distance) {
		for (DistanceMetric dm : DistanceMetric.values()) {
			if (distance <= dm.getMaxDistance())
				return dm.toString().toLowerCase();
		}
		return DistanceMetric.FAR.toString().toLowerCase();
	}

	/**
	 * Calculates the distance for a ghost to a point
	 * 
	 * @param ghost
	 *            The ghost that is being calculated for.
	 * @param p
	 *            The point the thing is going to.
	 * @return The distance the ghost must travel to get to the destination
	 *         point.
	 */
	private int calculateDistance(Ghost ghost, Point p) {
		// Create a duplicate and move it greedily towards the destination
		Ghost duplicate = (Ghost) ghost.clone();
		// Setting up greedy behaviour
		duplicate.m_bCanFollow = true;
		duplicate.m_bCanUseNextBest = false;
		duplicate.m_bInsaneAI = true;
		duplicate.m_bChaseMode = true;
		duplicate.m_bOldChaseMode = true;
		duplicate.m_deltaMax = 1;
		duplicate.m_deltaStartX = 0;
		duplicate.m_destinationX = -1;
		duplicate.m_destinationY = -1;
		duplicate.m_lastDirection = Thing.STILL;
		duplicate.m_targetX = p.x;
		duplicate.m_targetY = p.y;

		int distance = 0;
		// While the ghost has not arrived at the destination
		while (!((duplicate.m_locX == p.x) && (duplicate.m_locY == p.y))) {
			duplicate.m_deltaMax = 1;
			duplicate.m_deltaLocX = 0;
			duplicate.m_deltaLocY = 0;
			if ((duplicate.m_locX == 13) && (duplicate.m_locY <= 14)
					&& (duplicate.m_locY >= 12)) {
				duplicate.m_lastLocX = duplicate.m_locX;
				duplicate.m_lastLocY = duplicate.m_locY;
				duplicate.m_destinationX = -1;
				duplicate.m_destinationY = -1;
				duplicate.m_locY--;
				duplicate.m_bInsideRoom = false;
				duplicate.m_bEnteringDoor = false;
				duplicate.m_bEaten = false;
			} else {
				duplicate.tickThing(model_.m_pacMan.m_gameUI);
				model_.m_pacMan.Move(duplicate);
			}
			distance++;

			// Break if something goes wrong
			if (distance > 99) {
				distance = -99;
				break;
			}
		}
		return distance;
	}

	/**
	 * Searches the maze for observations. Does this by expanding outwards in
	 * junctions, recording the distance to each.
	 * 
	 * @param thing
	 *            The searching from.
	 * @return A mapping of minimal distances to junctions, given by points.
	 */
	private SortedSet<JunctionPoint> searchMaze(Thing thing) {
		SortedSet<JunctionPoint> closeJunctions = new TreeSet<JunctionPoint>();
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
			Set<JunctionPoint> thisLoc = isJunction(playerLoc, 0);
			if (thisLoc != null) {
				Point p = thisLoc.iterator().next().getLocation();
				knownJunctions.add(p);
			}

			SortedSet<JunctionPoint> junctionStack = new TreeSet<JunctionPoint>();
			// Add the initial junction points to the stack
			junctionStack.add(new JunctionPoint(playerLoc, Thing.UP, 0));
			junctionStack.add(new JunctionPoint(playerLoc, Thing.DOWN, 0));
			junctionStack.add(new JunctionPoint(playerLoc, Thing.LEFT, 0));
			junctionStack.add(new JunctionPoint(playerLoc, Thing.RIGHT, 0));
			distanceGrid_[playerLoc.x][playerLoc.y] = 0;

			// Keep following junctions until all have been found
			while (!junctionStack.isEmpty()) {
				JunctionPoint point = junctionStack.first();
				junctionStack.remove(point);

				Set<JunctionPoint> nextJunction = searchToJunction(point,
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
	private Set<JunctionPoint> searchToJunction(JunctionPoint startingPoint,
			Set<Point> knownJunctions) {
		byte direction = startingPoint.getDirection();
		int x = startingPoint.getLocation().x;
		int y = startingPoint.getLocation().y;
		int distance = startingPoint.getDistance();

		// Checking for an invalid request to move
		if (!Thing.getDestination(direction, x, y, new Point(), model_)) {
			return new HashSet<JunctionPoint>();
		}

		// Move in the direction
		byte oldDir = 0;
		Set<JunctionPoint> isJunct = null;
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
		JunctionPoint removal = null;
		for (JunctionPoint jp : isJunct) {
			if (jp.getDirection() == oldDir) {
				removal = jp;
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
		return new HashSet<JunctionPoint>();
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
	private Set<JunctionPoint> isJunction(Point loc, int distance) {
		Set<JunctionPoint> dirs = new HashSet<JunctionPoint>();
		if ((model_.m_gameState[loc.x][loc.y] & GameModel.GS_NORTH) == 0)
			dirs.add(new JunctionPoint(loc, Thing.UP, distance));
		if ((model_.m_gameState[loc.x][loc.y] & GameModel.GS_SOUTH) == 0)
			dirs.add(new JunctionPoint(loc, Thing.DOWN, distance));
		if ((model_.m_gameState[loc.x][loc.y] & GameModel.GS_EAST) == 0)
			dirs.add(new JunctionPoint(loc, Thing.LEFT, distance));
		if ((model_.m_gameState[loc.x][loc.y] & GameModel.GS_WEST) == 0)
			dirs.add(new JunctionPoint(loc, Thing.RIGHT, distance));

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
	 * Writes the frequencies to file.
	 */
	private void writeFreqs() {
		if (observationFreqs_ != null) {
			try {
				File output = new File("freqs");
				output.createNewFile();

				FileWriter writer = new FileWriter(output);
				BufferedWriter bf = new BufferedWriter(writer);
				bf.write("CONSTANT=1.0\n");

				for (int i = 1; i < observationFreqs_.length; i++) {
					Collections.sort(observationFreqs_[i]);
					for (int j = 1; j <= 5; j++) {
						int splitIndex = (int) (observationFreqs_[i].size() * (j / 6.0));
						bf.write(observationFreqs_[i].get(splitIndex) + ",");
					}
					bf.write("\n");
				}

				bf.close();
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
