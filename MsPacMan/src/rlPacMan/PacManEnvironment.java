package rlPacMan;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Arrays;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;


import relationalFramework.ObservationCondition;
import relationalFramework.Rule;
import rlPacMan.PacManAction.PacManActionSet;
import rlPacMan.PacManObservation.PacManObservationSet;

public class PacManEnvironment implements EnvironmentInterface {
	public static final int PLAYER_SPEED = 20;
	private PacMan environment_;
	private int prevScore_;
	private Observation obs_;
	private GameModel model_;
	private int[][] distanceGrid_;
	private Set<Point> pacJunctions_;
	private Map<Point, Integer> junctionSafety_;
	private Point gridStart_;
	private ArrayList<Double>[] observationFreqs_;
	private boolean noteFreqs_ = false;

	// @Override
	public void env_cleanup() {
		environment_ = null;
	}

	// @Override
	public String env_init() {
		environment_ = new PacMan();
		environment_.init();

		model_ = environment_.getGameModel();

		// Initialise the observations
		resetObservations();

		try {
			Thread.sleep(4096);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// @Override
	public String env_message(String arg0) {
		// Return the score
		if (arg0.equals("score"))
			return model_.m_player.m_score + "";
		if (arg0.equals("noteObs")) {
			noteFreqs_ = true;
			observationFreqs_ = new ArrayList[PacManObservationSet.values().length];
			for (int i = 0; i < observationFreqs_.length; i++) {
				observationFreqs_[i] = new ArrayList<Double>();
			}
		}
		if (arg0.equals("writeFreqs")) {
			writeFreqs();
		}
		return null;
	}

	// @Override
	public Observation env_start() {
		environment_.reinit();

		model_ = environment_.getGameModel();

		// Initialise the observations
		resetObservations();
		// Letting the thread 'sleep', so that the game still runs.
		try {
			if (!environment_.experimentMode_)
				Thread.currentThread().sleep(PLAYER_SPEED);
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

		// If we're not in full experiment mode, redraw the scene.
		if (!environment_.experimentMode_) {
			environment_.m_gameUI.m_bRedrawAll = true;
			environment_.m_gameUI.repaint();
			environment_.m_topCanvas.repaint();
		} else {
			environment_.m_gameUI.update(null);
			environment_.m_gameUI.m_bRedrawAll = false;
		}

		prevScore_ = 0;
		gridStart_ = null;
		return calculateObservations();
	}

	// @Override
	public Reward_observation_terminal env_step(Action arg0) {
		// Letting the thread 'sleep', so that the game still runs.
		try {
			if (!environment_.experimentMode_)
				Thread.currentThread().sleep(PLAYER_SPEED);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Applying the action (up down left right or nothing)
		environment_.simulateKeyPress(chooseLowAction(arg0.intArray).getKey());

		synchronized (environment_) {
			for (int i = 0; i < model_.m_player.m_deltaMax; i++) {
				environment_.tick(false);
				// If the agent has gone through 10 stages, end the episode
				if (model_.m_stage > 10) {
					return new Reward_observation_terminal(0,
							calculateObservations(), true);
				}
			}
		}

		// If we're not in full experiment mode, redraw the scene.
		if (!environment_.experimentMode_) {
			drawActions(arg0.intArray);
			environment_.m_gameUI.m_bRedrawAll = true;
			environment_.m_gameUI.repaint();
			environment_.m_topCanvas.repaint();
			environment_.m_bottomCanvas.repaint();
		} else {
			environment_.m_gameUI.update(null);
			environment_.m_bottomCanvas.setActionsList(null);
		}
		environment_.m_gameUI.m_bRedrawAll = false;

		Reward_observation_terminal rot = new Reward_observation_terminal(
				calculateReward(), calculateObservations(), isTerminal());
		return rot;
	}
	
	/**
	 * Loads the hand-coded rules with default weights.
	 * 
	 * @return The list of hand coded rules.
	 */
	public ArrayList<Rule> loadHandCodedRules() {
		ArrayList<Rule> handRules = new ArrayList<Rule>();

		// Go to dot
		handRules.add(new Rule(new PacManAction(
				PacManActionSet.TO_DOT, true)));
		// Go to centre of dots
		handRules.add(new Rule(new PacManAction(
				PacManActionSet.TO_CENTRE_OF_DOTS, true)));
		// From nearest ghost 1
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.LESS_THAN, 3), new PacManAction(
				PacManActionSet.FROM_GHOST, true)));
		// From nearest ghost 2
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.LESS_THAN, 4), new PacManAction(
				PacManActionSet.FROM_GHOST, true)));
		// From nearest ghost 3
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.LESS_THAN, 5), new PacManAction(
				PacManActionSet.FROM_GHOST, true)));
		// Stop running from nearest ghost 1
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.GREATER_EQ_THAN, 5), new PacManAction(
				PacManActionSet.FROM_GHOST, false)));
		// Stop running from nearest ghost 2
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.GREATER_EQ_THAN, 6), new PacManAction(
				PacManActionSet.FROM_GHOST, false)));
		// Stop running from nearest ghost 3
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.GREATER_EQ_THAN, 7), new PacManAction(
				PacManActionSet.FROM_GHOST, false)));
		// Towards safe junction
		handRules.add(new Rule(new PacManAction(
				PacManActionSet.TO_SAFE_JUNCTION, true)));
		// Towards maximally safe junction 1
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.LESS_THAN, 3), new PacManAction(
				PacManActionSet.TO_SAFE_JUNCTION, true)));
		// Towards maximally safe junction 2
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.LESS_THAN, 2), new PacManAction(
				PacManActionSet.TO_SAFE_JUNCTION, true)));
		// Maximally safe junction off 1
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.GREATER_EQ_THAN, 3), new PacManAction(
				PacManActionSet.TO_SAFE_JUNCTION, false)));
		// Safe to stop running 1
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.GREATER_EQ_THAN, 3), new PacManAction(
				PacManActionSet.FROM_GHOST, false)));
		// Maximally safe junction off 2
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.GREATER_EQ_THAN, 5), new PacManAction(
				PacManActionSet.TO_SAFE_JUNCTION, false)));
		// Safe to stop running 2
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.GREATER_EQ_THAN, 5), new PacManAction(
				PacManActionSet.FROM_GHOST, false)));
		// Keep on moving from ghosts
		handRules.add(new Rule(new PacManAction(
				PacManActionSet.FROM_GHOST, true)));
		// Eat edible ghosts
		handRules.add(new Rule(new PacManAction(
				PacManActionSet.TO_ED_GHOST, true)));
		// Ghost coming, chase powerdots
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.LESS_THAN, 4), new PacManAction(
				PacManActionSet.TO_POWER_DOT, true)));
		// If edible ghosts, don't chase power dots
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_ED_GHOST,
				ObservationCondition.LESS_THAN, 99), new PacManAction(
				PacManActionSet.TO_POWER_DOT, false)));
		// If edible ghosts and we're close to a powerdot, move away from it
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_ED_GHOST,
				ObservationCondition.LESS_THAN, 99), new PacManObservation(
				PacManObservationSet.NEAREST_POWER_DOT,
				ObservationCondition.LESS_THAN, 5), new PacManAction(
				PacManActionSet.FROM_POWER_DOT, true)));
		// If edible ghosts, move away from powerdots
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_ED_GHOST,
				ObservationCondition.LESS_THAN, 99), new PacManAction(
				PacManActionSet.FROM_POWER_DOT, true)));
		// If no edible ghosts, stop moving from powerdots
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_ED_GHOST,
				ObservationCondition.GREATER_EQ_THAN, 99), new PacManAction(
				PacManActionSet.FROM_POWER_DOT, false)));
		// If no edible ghosts, chase powerdots
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_ED_GHOST,
				ObservationCondition.GREATER_EQ_THAN, 99), new PacManAction(
				PacManActionSet.TO_POWER_DOT, true)));
		// If we're close to a ghost and powerdot, chase the powerdot 1
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_POWER_DOT,
				ObservationCondition.LESS_THAN, 2), new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.LESS_THAN, 5), new PacManAction(
				PacManActionSet.TO_POWER_DOT, true)));
		// If we're close to a ghost and powerdot, chase the powerdot 2
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_POWER_DOT,
				ObservationCondition.LESS_THAN, 4), new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.LESS_THAN, 5), new PacManAction(
				PacManActionSet.TO_POWER_DOT, true)));
		// In the clear
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.GREATER_EQ_THAN, 7),
				new PacManObservation(PacManObservationSet.MAX_JUNCTION_SAFETY,
						ObservationCondition.GREATER_EQ_THAN, 4),
				new PacManAction(PacManActionSet.FROM_GHOST, false)));
		// Ghosts are not close, eat a dot 1
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.GHOST_DENSITY,
				ObservationCondition.LESS_THAN, 1.5), new PacManObservation(
				PacManObservationSet.NEAREST_POWER_DOT,
				ObservationCondition.LESS_THAN, 5), new PacManAction(
				PacManActionSet.FROM_POWER_DOT, true)));
		// Far from power dot, stop running
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_POWER_DOT,
				ObservationCondition.GREATER_EQ_THAN, 10), new PacManAction(
				PacManActionSet.FROM_POWER_DOT, false)));
		// Ghosts are too spread, move away from power dot
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.TOTAL_DIST_TO_GHOSTS,
				ObservationCondition.GREATER_EQ_THAN, 30), new PacManAction(
				PacManActionSet.FROM_POWER_DOT, true)));
		// Unsafe junction, run from ghosts 1
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.LESS_THAN, 3), new PacManAction(
				PacManActionSet.FROM_GHOST, true)));
		// Unsafe junction, run from ghosts 2
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.LESS_THAN, 2), new PacManAction(
				PacManActionSet.FROM_GHOST, true)));
		// Unsafe junction, run from ghosts 3
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.LESS_THAN, 1), new PacManAction(
				PacManActionSet.FROM_GHOST, true)));
		// Move from ghost centre
		handRules.add(new Rule(new PacManAction(
				PacManActionSet.FROM_GHOST_CENTRE, true)));
		// Stop chasing edible ghosts if none
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_ED_GHOST,
				ObservationCondition.GREATER_EQ_THAN, 99), new PacManAction(
				PacManActionSet.TO_ED_GHOST, false)));
		// Chasing edible ghosts
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_ED_GHOST,
				ObservationCondition.LESS_THAN, 99), new PacManAction(
				PacManActionSet.TO_ED_GHOST, true)));
		// If not running from powerdot, move towards it
		handRules.add(new Rule(new PacManAction(PacManActionSet.FROM_POWER_DOT,
				false), new PacManAction(PacManActionSet.TO_POWER_DOT, true)));
		// If there is a fruit, chase it
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_FRUIT,
				ObservationCondition.LESS_THAN, 99), new PacManAction(
				PacManActionSet.TO_FRUIT, true)));
		// If there isn't a fruit, stop chasing it
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_FRUIT,
				ObservationCondition.GREATER_EQ_THAN, 99), new PacManAction(
				PacManActionSet.TO_FRUIT, false)));
		// If ghosts are edible and not flashing, chase them
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_ED_GHOST,
				ObservationCondition.LESS_THAN, 99), new PacManObservation(
				PacManObservationSet.GHOSTS_FLASHING,
				ObservationCondition.LESS_THAN, 1), new PacManAction(
				PacManActionSet.TO_ED_GHOST, true)));
		// If the ghosts are flashing, stop chasing them
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.GHOSTS_FLASHING,
				ObservationCondition.GREATER_EQ_THAN, 1), new PacManAction(
				PacManActionSet.TO_ED_GHOST, false)));
		// If the ghosts are flashing, run from them
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.GHOSTS_FLASHING,
				ObservationCondition.GREATER_EQ_THAN, 1), new PacManAction(
				PacManActionSet.FROM_GHOST, true)));

		return handRules;
	}

	/**
	 * Draws the agent's action switch.
	 * 
	 * @param actions
	 *            The agent's current actions. Should always be the same size
	 *            with possible null elements.
	 */
	private void drawActions(int[] actions) {
		String[] actionList = new String[actions.length];
		for (int i = 0; i < actions.length; i++) {
			StringBuffer buffer = new StringBuffer("[" + (i + 1) + "]: ");
			// If the action is null
			if (actions[i] == -1) {
				buffer.append("null");
			} else {
				buffer.append(PacManAction.getConditionAt(actions[i]));
			}
			actionList[i] = buffer.toString();
		}
		environment_.m_bottomCanvas.setActionsList(actionList);
	}

	/**
	 * Chooses a low action when given a high action.
	 * 
	 * @param highActions
	 *            The high action to take, sorted in order from most to least
	 *            priority, being processed into a single low action.
	 * @return The low action to use.
	 */
	private PacManLowAction chooseLowAction(int[] highActions) {
		Player thePlayer = model_.m_player;
		ArrayList<PacManLowAction> actionDirection = ActionConverter
				.validDirections(thePlayer.m_locX, thePlayer.m_locY, model_);
		int i = 0;
		do {
			ArrayList<PacManLowAction> levelDirections = null;

			if (highActions[i] >= 0) {
				// The actions returned are guaranteed not to move into a wall.
				switch (PacManAction.getConditionAt(highActions[i])) {
				case TO_DOT:
					levelDirections = ActionConverter
							.toDot(
									model_,
									distanceGrid_,
									(int) obs_.doubleArray[PacManObservationSet.NEAREST_DOT
											.ordinal()]);
					break;
				case TO_POWER_DOT:
					levelDirections = ActionConverter
							.toPowerDot(
									model_,
									thePlayer,
									distanceGrid_,
									(int) obs_.doubleArray[PacManObservationSet.NEAREST_POWER_DOT
											.ordinal()], true);
					break;
				case FROM_POWER_DOT:
					levelDirections = ActionConverter
							.toPowerDot(
									model_,
									thePlayer,
									distanceGrid_,
									(int) obs_.doubleArray[PacManObservationSet.NEAREST_POWER_DOT
											.ordinal()], false);
					break;
				case TO_ED_GHOST:
					levelDirections = ActionConverter
							.toEdGhost(
									model_,
									distanceGrid_,
									(int) obs_.doubleArray[PacManObservationSet.NEAREST_ED_GHOST
											.ordinal()]);
					break;
				case TO_FRUIT:
					levelDirections = ActionConverter.toFruit(model_,
							thePlayer, distanceGrid_);
					break;
				case FROM_GHOST:
					levelDirections = ActionConverter
							.fromGhost(
									model_,
									thePlayer,
									distanceGrid_,
									(int) obs_.doubleArray[PacManObservationSet.NEAREST_GHOST
											.ordinal()]);
					break;
				case TO_SAFE_JUNCTION:
					levelDirections = ActionConverter
							.toSafeJunction(
									junctionSafety_,
									(int) obs_.doubleArray[PacManObservationSet.MAX_JUNCTION_SAFETY
											.ordinal()], distanceGrid_);
					break;
				case FROM_GHOST_CENTRE:
					levelDirections = ActionConverter.fromGhostCentre(
							thePlayer, model_);
					break;
				case KEEP_DIRECTION:
					levelDirections = ActionConverter.keepDirection(thePlayer,
							model_);
					break;
				// case TO_LOWER_GHOST_DENSITY:
				// case TO_GHOST_FREE_AREA:
				case TO_CENTRE_OF_DOTS:
					levelDirections = ActionConverter.toCentreOfDots(thePlayer,
							model_);
					break;
				default:
					levelDirections = actionDirection;
				}

				// Remove any directions not in the higher priority direction
				// list
				levelDirections.retainAll(actionDirection);
				if (levelDirections.size() > 0)
					actionDirection = levelDirections;
			}
			i++;
		} while ((i < highActions.length) && (actionDirection.size() > 1));

		// Choose a random action from the directions (may only be one)
		return actionDirection.get((int) (Math.random() * actionDirection
				.size()));
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
	private boolean isTerminal() {
		int state = model_.m_state;
		if (state == GameModel.STATE_GAMEOVER)
			return true;
		return false;
	}

	/**
	 * A method for calculating observations about the current PacMan state.
	 * 
	 * Currently extracts features from the state, rather than the basic state.
	 * 
	 * @return An observation of the current state
	 */
	private Observation calculateObservations() {
		// Find the player related observations
		pacJunctions_ = searchMaze(model_.m_player, obs_.doubleArray);
		junctionSafety_ = new HashMap<Point, Integer>();
		for (Point junc : pacJunctions_)
			junctionSafety_.put(junc, Integer.MAX_VALUE);
		/*
		 * for (int y = 0; y < model_.m_gameSizeY; y++) { for (int x = 0; x <
		 * model_.m_gameSizeX; x++) { DecimalFormat df = new
		 * DecimalFormat("00"); System.out.print(df.format(distanceGrid_[x][y])
		 * + " "); } System.out.println(); }
		 */

		// Find the ghost related observations
		double ghostX = 0;
		double ghostY = 0;
		int numGhosts = 0;
		Set<Point> ghostCoords = new HashSet<Point>();
		obs_.doubleArray[PacManObservationSet.NEAREST_GHOST.ordinal()] = Integer.MAX_VALUE;
		obs_.doubleArray[PacManObservationSet.NEAREST_ED_GHOST.ordinal()] = Integer.MAX_VALUE;
		for (int i = 0; i < model_.m_ghosts.length; i++) {
			// If the ghost is active and hostile
			Ghost ghost = model_.m_ghosts[i];
			if ((ghost.m_nTicks2Exit <= 0) && (!ghost.m_bEaten)) {
				int isEd = 0;
				if (ghost.m_nTicks2Flee <= 0) {
					isEd = PacManObservationSet.NEAREST_GHOST.ordinal();
				} else {
					isEd = PacManObservationSet.NEAREST_ED_GHOST.ordinal();
					// Check flashing
					if (ghost.isFlashing())
						obs_.doubleArray[PacManObservationSet.GHOSTS_FLASHING
								.ordinal()] = 1;
				}
				obs_.doubleArray[isEd] = Math.min(obs_.doubleArray[isEd],
						distanceGrid_[ghost.m_locX][ghost.m_locY]);

				ghostX += ghost.m_locX;
				ghostY += ghost.m_locY;
				numGhosts++;
				ghostCoords.add(new Point(ghost.m_locX, ghost.m_locY));

				// Calculate the ghost's distance from each of the player's
				// junctions
				if (ghost.m_nTicks2Flee <= 0) {
					for (Point p : pacJunctions_) {
						int ghostDist = calculateDistance(ghost, p);
						int result = ghostDist - distanceGrid_[p.x][p.y];
						// Looking for the minimally safe junction
						if ((result > Integer.MIN_VALUE)
								&& (result < junctionSafety_.get(p)))
							junctionSafety_.put(p, result);
					}
				}
			}
		}
		// Work out the ghost centre dist
		if (numGhosts != 0) {
			obs_.doubleArray[PacManObservationSet.GHOST_CENTRE_DIST.ordinal()] = Point
					.distance(ghostX / numGhosts, ghostY / numGhosts,
							model_.m_player.m_locX, model_.m_player.m_locY);

			// Calculate the density
			obs_.doubleArray[PacManObservationSet.GHOST_DENSITY.ordinal()] = calculateDensity(ghostCoords);
		}

		// Choose the maximally safe junction
		obs_.doubleArray[PacManObservationSet.MAX_JUNCTION_SAFETY.ordinal()] = Integer.MIN_VALUE;
		for (Point p : pacJunctions_) {
			if (junctionSafety_.get(p) > obs_.doubleArray[PacManObservationSet.MAX_JUNCTION_SAFETY
					.ordinal()])
				obs_.doubleArray[PacManObservationSet.MAX_JUNCTION_SAFETY
						.ordinal()] = junctionSafety_.get(p);
		}

		// Calculate the travelling salesman distance
		Thing currentThing = model_.m_player;
		Set<Thing> thingsToVisit = new HashSet<Thing>();
		for (Ghost ghost : model_.m_ghosts)
			thingsToVisit.add(ghost);
		double totalDistance = 0;
		// While there are things to go to.
		while (!thingsToVisit.isEmpty()) {
			// Find the closest thing to the current thing
			double closest = Integer.MAX_VALUE;
			Thing closestThing = null;
			for (Thing thing : thingsToVisit) {
				double distanceBetween = Point.distance(currentThing.m_locX,
						currentThing.m_locY, thing.m_locX, thing.m_locY);
				if (distanceBetween < closest) {
					closest = distanceBetween;
					closestThing = thing;
				}
			}

			// Add tot he total distance, and repeat the loop, minus the closest
			// thing
			totalDistance += closest;
			currentThing = closestThing;
			thingsToVisit.remove(currentThing);
		}
		obs_.doubleArray[PacManObservationSet.TOTAL_DIST_TO_GHOSTS.ordinal()] = totalDistance;

		// Calculate the fruit distance
		if ((model_.m_fruit.m_nTicks2Show == 0)
				&& (model_.m_fruit.m_bAvailable)) {
			obs_.doubleArray[PacManObservationSet.NEAREST_FRUIT.ordinal()] = distanceGrid_[model_.m_fruit.m_locX][model_.m_fruit.m_locY];
		}

		if (noteFreqs_) {
			// Update the observation values
			for (int i = 1; i < obs_.doubleArray.length; i++) {
				double val = obs_.doubleArray[i];
				if (val < Integer.MAX_VALUE)
					observationFreqs_[i].add(val);
			}
		}

		return obs_;
	}

	/**
	 * Calculates the density of the ghosts.
	 * 
	 * @param ghostCoords
	 *            The coordinates of every ghost.
	 * @return The density of the ghosts, or 0 if only one ghost.
	 */
	private double calculateDensity(Set<Point> ghostCoords) {
		// If we can compare densities between ghosts
		double density = 0;
		Point[] coordArray = ghostCoords.toArray(new Point[ghostCoords.size()]);
		for (int i = 0; i < coordArray.length - 1; i++) {
			Point thisGhost = coordArray[i];
			for (int j = i + 1; j < coordArray.length; j++) {
				Point thatGhost = coordArray[j];
				double distance = Ghost.DENSITY_RADIUS
						- thisGhost.distance(thatGhost);
				if (distance > 0)
					density += distance;
			}
		}
		return density;
	}

	/**
	 * Calculates the distance for a thing to a point
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
	 * @param observations
	 *            The observations being made during the search.
	 * @return A mapping of minimal distances to junctions, given by points.
	 */
	private Set<Point> searchMaze(Thing thing, double[] observations) {
		Set<Point> closeJunctions = new HashSet<Point>();
		Set<Point> dots = new HashSet<Point>();

		Point playerLoc = new Point(thing.m_locX, thing.m_locY);

		// If Pacman has moved, update the distance grid.
		if ((gridStart_ == null) || (!gridStart_.equals(playerLoc))) {
			gridStart_ = playerLoc;
			// Redoing the observations
			resetObservations();

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
						knownJunctions, observations, dots);
				junctionStack.addAll(nextJunction);

				// Checking for the immediate junctions
				if ((!nextJunction.isEmpty())
						&& (point.getLocation().equals(playerLoc))) {
					closeJunctions.add(nextJunction.iterator().next()
							.getLocation());
				}
			}

			// Calculate the centre of the dots
			int dotX = 0;
			int dotY = 0;
			int i = 0;
			for (Point dot : dots) {
				i++;
				dotX += dot.x;
				dotY += dot.y;
			}
			// Special case if player eats all dots
			if (i == 0) {
				i = 1;
				dotX = model_.m_player.m_locX;
				dotY = model_.m_player.m_locY;
			}
			observations[PacManObservationSet.DOT_CENTRE_DIST.ordinal()] = Point
					.distance(dotX / i, dotY / i, model_.m_player.m_locX,
							model_.m_player.m_locY);
			return closeJunctions;
		}
		return pacJunctions_;
	}

	/**
	 * A method for searching for the shortest distance from junction to
	 * junction. Also makes observations about dots and ghosts along the way.
	 * 
	 * @param startingPoint
	 *            The starting point for the junction search.
	 * @param knownJunctions
	 *            The known junctions.
	 * @param observations
	 *            The observations to be made.
	 * @param dots
	 *            The dots present in the maze. Used for calculating the centre.
	 * @return The set of starting points for the next found junction or an
	 *         empty set.
	 */
	private Set<JunctionPoint> searchToJunction(JunctionPoint startingPoint,
			Set<Point> knownJunctions, double[] observations, Set<Point> dots) {
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

			// Update any observations

			if ((model_.m_gameState[x][y] & GameModel.GS_FOOD) != 0) {
				if (distance < observations[PacManObservationSet.NEAREST_DOT
						.ordinal()])
					observations[PacManObservationSet.NEAREST_DOT.ordinal()] = distance;
				dots.add(new Point(x, y));
			}
			if (((model_.m_gameState[x][y] & GameModel.GS_POWERUP) != 0)
					&& (distance < observations[PacManObservationSet.NEAREST_POWER_DOT
							.ordinal()]))
				observations[PacManObservationSet.NEAREST_POWER_DOT.ordinal()] = distance;

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
	private void resetObservations() {
		if (obs_ == null)
			obs_ = new Observation(0, PacManObservationSet.values().length);
		for (int i = 0; i < obs_.doubleArray.length; i++) {
			obs_.doubleArray[i] = Integer.MAX_VALUE;
		}
		obs_.doubleArray[PacManObservationSet.GHOSTS_FLASHING.ordinal()] = 0;

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

	/**
	 * A static enclosing class for handling action conversions from high to
	 * low.
	 * 
	 * @author Samuel J. Sarjant
	 */
	public static class ActionConverter {

		/**
		 * Creates a list of valid directions to move in (not blocked by a
		 * wall).
		 * 
		 * @param x
		 *            The x location.
		 * @param y
		 *            The y location.
		 * @param model
		 *            The current game model.
		 * @return A list of valid directions (should always be at least 2
		 *         elements)
		 */
		public static ArrayList<PacManLowAction> validDirections(int x, int y,
				GameModel model) {
			ArrayList<PacManLowAction> directions = new ArrayList<PacManLowAction>();
			Point blag = new Point();
			if (Thing.getDestination(Thing.UP, x, y, blag, model))
				directions.add(PacManLowAction.UP);
			if (Thing.getDestination(Thing.DOWN, x, y, blag, model))
				directions.add(PacManLowAction.DOWN);
			if (Thing.getDestination(Thing.LEFT, x, y, blag, model))
				directions.add(PacManLowAction.LEFT);
			if (Thing.getDestination(Thing.RIGHT, x, y, blag, model))
				directions.add(PacManLowAction.RIGHT);
			return directions;
		}

		/**
		 * The keep direction high action simply keeps Pacman going in the same
		 * direction, or chooses a random valid direction (except going
		 * backwards) when Pacman can no longer proceed in the given direction.
		 * 
		 * @param thePlayer
		 *            The player.
		 * @param model
		 *            The current game model.
		 * @return The direction/s to go.
		 */
		public static ArrayList<PacManLowAction> keepDirection(
				Player thePlayer, GameModel model) {
			// Call the move around wall procedure.
			return moveAroundWall(
					PacManLowAction.values()[thePlayer.m_direction],
					thePlayer.m_locX, thePlayer.m_locY, model);
		}

		/**
		 * If the direction in the specified position is moving into a wall,
		 * choose a random valid direction to 90 degrees of the original
		 * direction.
		 * 
		 * @param direction
		 *            The direction to move.
		 * @param x
		 *            The x location.
		 * @param y
		 *            The y location.
		 * @param model
		 *            The current game model.
		 * @return The possible direction/s to go after adjusting for possible
		 *         walls.
		 */
		private static ArrayList<PacManLowAction> moveAroundWall(
				PacManLowAction direction, int x, int y, GameModel model) {
			ArrayList<PacManLowAction> endDirections = new ArrayList<PacManLowAction>();
			if (!Thing.getDestination(direction.ordinal(), x, y, new Point(),
					model)) {
				// The player cannot go in the same or opposite direction
				byte[] directions = new byte[2];
				if (direction.ordinal() == 0)
					return endDirections;

				// If the player is going up or down
				if (direction.ordinal() <= Thing.DOWN) {
					directions[0] = Thing.LEFT;
					directions[1] = Thing.RIGHT;
				} else {
					// The player is going left or right
					directions[0] = Thing.UP;
					directions[1] = Thing.DOWN;
				}

				// Check if both directions are valid. Else choose the only
				// valid one.
				if (Thing.getDestination(directions[0], x, y, new Point(),
						model))
					endDirections.add(PacManLowAction.values()[directions[0]]);
				// If the other direction is valid
				if (Thing.getDestination(directions[1], x, y, new Point(),
						model)) {
					endDirections.add(PacManLowAction.values()[directions[1]]);
				}

				return endDirections;
			}
			endDirections.add(direction);
			return endDirections;
		}

		/**
		 * The to dot high action moves the player towards the nearest dot
		 * following the shortest possible path.
		 * 
		 * @param model
		 *            The current game model.
		 * @param distanceGrid
		 *            The distance grid for the player.
		 * @param nearestDotDist
		 *            The nearest dot to the player in terms of distance.
		 * @return The direction/s following the shortest path to the nearest
		 *         dot.
		 */
		public static ArrayList<PacManLowAction> toDot(GameModel model,
				int[][] distanceGrid, int nearestDotDist) {
			return scanDistanceGrid(model, distanceGrid, nearestDotDist,
					GameModel.GS_FOOD);
		}

		/**
		 * The to power dot high action moves the player towards the nearest
		 * power dot following the shortest possible path.
		 * 
		 * @param thePlayer
		 *            The player.
		 * @param model
		 *            The current game model.
		 * @param distanceGrid
		 *            The distance grid for the player.
		 * @param nearestDotDist
		 *            The nearest dot to the player in terms of distance.
		 * @param isTowards
		 * @return The direction following the shortest path to the nearest dot.
		 */
		public static ArrayList<PacManLowAction> toPowerDot(GameModel model,
				Player thePlayer, int[][] distanceGrid, int nearestDotDist,
				boolean isTowards) {
			ArrayList<PacManLowAction> endDirections = scanDistanceGrid(model,
					distanceGrid, nearestDotDist, GameModel.GS_POWERUP);
			if (isTowards)
				return endDirections;
			else {
				return oppositeDirection(model, thePlayer, endDirections);
			}
		}

		/**
		 * Moves the player in the opposite direction to the directions in the
		 * directions list (the player will not move towards any of the
		 * directions).
		 * 
		 * @param model
		 *            The current game model.
		 * @param thePlayer
		 *            The player.
		 * @param directions
		 *            The possible directions to go opposite to.
		 * @return A direction away from one of the given directions.
		 */
		private static ArrayList<PacManLowAction> oppositeDirection(
				GameModel model, Player thePlayer,
				ArrayList<PacManLowAction> directions) {
			if (directions.isEmpty())
				return directions;
			// Return the inverse of the directions present
			ArrayList<PacManLowAction> validDirs = validDirections(
					thePlayer.m_locX, thePlayer.m_locY, model);
			validDirs.removeAll(directions);
			return validDirs;
		}

		/**
		 * The to edible ghost high action moves Pacman towards the nearest
		 * edible ghost following the shortest path.
		 * 
		 * @param model
		 *            The current game model.
		 * @param distanceGrid
		 *            The distance grid for the player.
		 * @param nearestGhostDist
		 *            The nearest edible ghost distance to the player.
		 * @return The direction following the shortest path to the nearest
		 *         edible ghost.
		 */
		public static ArrayList<PacManLowAction> toEdGhost(GameModel model,
				int[][] distanceGrid, int nearestGhostDist) {
			return chaseGhost(model, distanceGrid, nearestGhostDist, true);
		}

		/**
		 * The from ghost high action moves Pacman away from the nearest
		 * non-edible ghost in the opposite to the shortest path towards.
		 * 
		 * @param model
		 *            The current game model.
		 * @param thePlayer
		 *            The player.
		 * @param distanceGrid
		 *            The distance grid for the player.
		 * @param nearestGhostDist
		 *            The nearest ghost distance to the player.
		 * @return The opposite direction following the shortest path to the
		 *         nearest ghost.
		 */
		public static ArrayList<PacManLowAction> fromGhost(GameModel model,
				Player thePlayer, int[][] distanceGrid, int nearestGhostDist) {
			return oppositeDirection(model, thePlayer, chaseGhost(model,
					distanceGrid, nearestGhostDist, false));
		}

		/**
		 * The to fruit action moves the player towards the fruit, taking the
		 * shortest possible route.
		 * 
		 * @param model
		 *            The current game model.
		 * @param thePlayer
		 *            The player.
		 * @param distanceGrid
		 *            The distance grid for the player.
		 * @return The possible direction/s to take for chasing the fruit (if
		 *         existant).
		 */
		public static ArrayList<PacManLowAction> toFruit(GameModel model,
				Player thePlayer, int[][] distanceGrid) {
			ArrayList<PacManLowAction> endDirections = new ArrayList<PacManLowAction>();
			// If there is a fruit, chase it
			if (model.m_fruit.m_nTicks2Show == 0) {
				endDirections.add(followPath(model.m_fruit.m_locX,
						model.m_fruit.m_locY, distanceGrid));
			}
			return endDirections;
		}

		/**
		 * The to safe junction high action moves Pacman towards the maximally
		 * safe junction from the possible junctions Pacman can go towards.
		 * 
		 * @param junctionSafety
		 *            The possible junctions Pacman can go to, with safety
		 *            values.
		 * @param safestJunction
		 *            The safest junction value.
		 * @param distanceGrid
		 *            The distance grid for the player.
		 * @return The direction following the shortest path to the maximally
		 *         safe junction.
		 */
		public static ArrayList<PacManLowAction> toSafeJunction(
				Map<Point, Integer> junctionSafety, int safestJunction,
				int[][] distanceGrid) {
			// For every junction
			ArrayList<PacManLowAction> endDirections = new ArrayList<PacManLowAction>();
			for (Point p : junctionSafety.keySet()) {
				// If this is the safest junction
				if (junctionSafety.get(p) == safestJunction) {
					endDirections.add(followPath(p.x, p.y, distanceGrid));
				}
			}
			return endDirections;
		}

		/**
		 * The from ghost centre high action moves Pacman away from the ghost
		 * centre by maximising the Euclidean distance between Pacman and the
		 * centre.
		 * 
		 * @param thePlayer
		 *            The player.
		 * @param model
		 *            The current game model.
		 * @return The direction that maximises the distance from the ghost
		 *         centre.
		 */
		public static ArrayList<PacManLowAction> fromGhostCentre(
				Player thePlayer, GameModel model) {
			double ghostX = 0;
			double ghostY = 0;
			int numGhosts = 0;
			// Check each ghost
			for (Ghost ghost : model.m_ghosts) {
				// If the ghost is out there
				if ((ghost.m_nTicks2Exit <= 0) && (!ghost.m_bEaten)) {
					ghostX += ghost.m_locX;
					ghostY += ghost.m_locY;
					numGhosts++;
				}
			}

			// If there is at least one ghost out there
			if (numGhosts != 0) {
				ghostX /= numGhosts;
				ghostY /= numGhosts;
				return maximiseEuclideanDistance(thePlayer, ghostX, ghostY,
						model);
			}
			return new ArrayList<PacManLowAction>();
		}

		/**
		 * The to centre of dots high action chooses a direction that minimises
		 * the Euclidean distance between the player and the centre of the dots.
		 * 
		 * @param thePlayer
		 *            The player.
		 * @param model
		 *            The current game model.
		 * @return The direction that minimises the Euclidean distance between
		 *         the player and the centre point.
		 */
		public static ArrayList<PacManLowAction> toCentreOfDots(
				Player thePlayer, GameModel model) {
			// Scan the state for dots
			double dotX = 0;
			double dotY = 0;
			int numDots = 0;
			for (int x = 0; x < model.m_gameState.length; x++) {
				for (int y = 0; y < model.m_gameState[x].length; y++) {
					if ((model.m_gameState[x][y] & GameModel.GS_FOOD) != 0) {
						dotX += x;
						dotY += y;
						numDots++;
					}
				}
			}

			if (numDots != 0) {
				dotX /= numDots;
				dotY /= numDots;
				return oppositeDirection(model, thePlayer,
						maximiseEuclideanDistance(thePlayer, dotX, dotY, model));
			}
			return new ArrayList<PacManLowAction>();
		}

		/**
		 * Chases a ghost (edible or not) by taking the shortest possible path
		 * towards it.
		 * 
		 * @param model
		 *            The current game model.
		 * @param distanceGrid
		 *            The distance grid for the player.
		 * @param nearestGhostDist
		 *            The nearest ghost distance (edible or not as by next
		 *            param) to the player.
		 * @param edibleGhost
		 *            If the player is chasing edible ghosts or not.
		 * @return The direction following the shortest path to the nearest
		 *         edible/non-edible ghost.
		 */
		private static ArrayList<PacManLowAction> chaseGhost(GameModel model,
				int[][] distanceGrid, int nearestGhostDist, boolean edibleGhost) {
			ArrayList<PacManLowAction> endDirections = new ArrayList<PacManLowAction>();
			// Check each ghost
			for (Ghost ghost : model.m_ghosts) {
				// If the ghost is out there
				if ((ghost.m_nTicks2Exit <= 0) && (!ghost.m_bEaten)) {
					// If the edibility equals the parameter
					if ((edibleGhost && (ghost.m_nTicks2Flee > 0))
							|| (!edibleGhost && (ghost.m_nTicks2Flee <= 0))) {
						// If this ghost is the closest one, go for it
						if (distanceGrid[ghost.m_locX][ghost.m_locY] == nearestGhostDist)
							endDirections.add(followPath(ghost.m_locX,
									ghost.m_locY, distanceGrid));
					}
				}
			}
			return endDirections;
		}

		/**
		 * Maximises the Euclidean distance between the player and a point in 2D
		 * space.
		 * 
		 * @param thePlayer
		 *            The player.
		 * @param x
		 *            The x location of the point.
		 * @param y
		 *            the y location of the point.
		 * @return The movement action that maximises the Euclidean distance
		 *         between the player and the point.
		 */
		private static ArrayList<PacManLowAction> maximiseEuclideanDistance(
				Player thePlayer, double x, double y, GameModel model) {
			// Find the distances between Pacman and the point centre
			double distX = thePlayer.m_locX - x;
			double distY = thePlayer.m_locY - y;
			ArrayList<PacManLowAction> endDirections = new ArrayList<PacManLowAction>();
			// Move left/right
			if (Math.abs(distX) > Math.abs(distY)) {
				if (distX > 0)
					endDirections.add(PacManLowAction.RIGHT);
				else
					endDirections.add(PacManLowAction.LEFT);
			} else {
				// Move up/down
				if (distY > 0)
					endDirections.add(PacManLowAction.DOWN);
				else
					endDirections.add(PacManLowAction.UP);
			}
			// Be sure the player is travelling in a valid direction.
			endDirections.retainAll(validDirections(thePlayer.m_locX,
					thePlayer.m_locY, model));
			return endDirections;
		}

		/**
		 * A general method for scanning the distance grid for a type of dot at
		 * a specified distance.
		 * 
		 * @param model
		 *            The current game model.
		 * @param distanceGrid
		 *            The distance grid for the player.
		 * @param nearestDotDist
		 *            The distance for the nearest dot.
		 * @param dotType
		 *            The particular type of dot (regular or power).
		 * @return The direction following the shortest path to the nearest dot
		 *         type.
		 */
		private static ArrayList<PacManLowAction> scanDistanceGrid(
				GameModel model, int[][] distanceGrid, int nearestDotDist,
				int dotType) {
			ArrayList<PacManLowAction> endDirections = new ArrayList<PacManLowAction>();
			// First, find the nearest dot location/s
			for (int x = 0; x < distanceGrid.length; x++) {
				for (int y = 0; y < distanceGrid[x].length; y++) {
					// If a possible location contains a dot, follow it back
					if (distanceGrid[x][y] == nearestDotDist) {
						if ((model.m_gameState[x][y] & dotType) != 0)
							endDirections.add(followPath(x, y, distanceGrid));
					}
				}
			}
			return endDirections;
		}

		/**
		 * Follows a path from a particular location back to point 0: where
		 * Pacman currently is. This procedure thereby finds the quickest path
		 * to a point by giving the initial direction that Pacman needs to take
		 * to get to the goal.
		 * 
		 * @param x
		 *            The x location of the point.
		 * @param y
		 *            The y location of the point.
		 * @param distanceGrid
		 *            The distance grid for the player, where a value of 0 is
		 *            the player position.
		 * @return The initial direction to take to follow the path.
		 */
		private static PacManLowAction followPath(int x, int y,
				int[][] distanceGrid) {
			PacManLowAction prevLocation = PacManLowAction.NOTHING;
			// Repeat until the distance grid coords are equal to 0
			int width = distanceGrid.length;
			int height = distanceGrid[x].length;
			while (distanceGrid[x][y] != 0) {
				int currentVal = distanceGrid[x][y];
				// Check all directions
				if (distanceGrid[(x + 1) % width][y] == currentVal - 1) {
					x = (x + 1) % width;
					prevLocation = PacManLowAction.LEFT;
				} else if (distanceGrid[(x - 1 + width) % width][y] == currentVal - 1) {
					x = (x - 1 + width) % width;
					prevLocation = PacManLowAction.RIGHT;
				} else if (distanceGrid[x][(y + 1) % height] == currentVal - 1) {
					y = (y + 1) % height;
					prevLocation = PacManLowAction.UP;
				} else if (distanceGrid[x][(y - 1 + height) % height] == currentVal - 1) {
					y = (y - 1 + height) % height;
					prevLocation = PacManLowAction.DOWN;
				} else {
					return prevLocation;
				}
			}
			return prevLocation;
		}
	}
}
