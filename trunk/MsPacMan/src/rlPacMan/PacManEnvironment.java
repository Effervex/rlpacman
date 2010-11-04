package rlPacMan;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import relationalFramework.RuleAction;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;

public class PacManEnvironment implements EnvironmentInterface {
	public static int playerDelay_ = 0;
	private Rete rete_;
	private PacMan environment_;
	private int prevScore_;
	private GameModel model_;
	private DistanceGridCache distanceGridCache_;
	private boolean experimentMode_ = false;
	private PacManLowAction lastDirection_;
	private DistanceDir[][] distanceGrid_;
	private Collection<Junction> closeJunctions_;

	@Override
	public void env_cleanup() {
		environment_ = null;
	}

	@Override
	public String env_init() {
		environment_ = new PacMan();
		environment_.init(experimentMode_);

		model_ = environment_.getGameModel();

		// Initialise the observations
		cacheDistanceGrids();

		return null;
	}

	@Override
	public String env_message(String arg0) {
		if (arg0.equals("maxSteps")) {
			return 10000 + "";
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
		} else if (arg0.equals("-e")) {
			// Run the program in experiment mode (No GUI).
			experimentMode_ = true;
		}
		if ((arg0.length() > 4) && (arg0.substring(0, 4).equals("goal"))) {
			StateSpec.reinitInstance(arg0.substring(5));
			if (arg0.substring(4).contains("levelMax")
					|| arg0.substring(4).contains("oneLevel"))
				model_.oneLife_ = true;
		} else {
			try {
				int delay = Integer.parseInt(arg0);
				playerDelay_ = delay;
			} catch (Exception e) {

			}
		}
		return null;
	}

	@Override
	public Observation env_start() {
		resetEnvironment();

		// Run the optimal policy if it hasn't yet been run
		if (!PolicyGenerator.getInstance().hasPreGoal()
				&& !PolicyGenerator.getInstance().isFrozen()) {
			optimalPolicy();
		}

		// If we're not in full experiment mode, redraw the scene.
		if (!experimentMode_) {
			environment_.m_gameUI.m_bRedrawAll = true;
			environment_.m_gameUI.repaint();
			environment_.m_topCanvas.repaint();
		} else {
			environment_.m_gameUI.update(null);
			environment_.m_gameUI.m_bRedrawAll = false;
		}

		return formObservations(rete_);
	}

	@Override
	public Reward_observation_terminal env_step(Action arg0) {
		// Letting the thread 'sleep', so that the game still runs.
		try {
			if (!experimentMode_) {
				Thread.sleep(playerDelay_);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Applying the action (up down left right or nothing)
		ActionChoice actions = (ActionChoice) ObjectObservations.getInstance().objectArray[0];
		environment_.simulateKeyPress(chooseLowAction(actions.getActions())
				.getKey());

		int i = 0;
		while ((i < model_.m_player.m_deltaMax * 2 - 1)
				|| (!model_.isLearning())) {
			environment_.tick(false);
			i++;
		}
		model_.m_player.m_deltaLocX = 0;
		model_.m_player.m_deltaLocY = 0;

		// If we're not in full experiment mode, redraw the scene.
		if (!experimentMode_) {
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

		// Set the highscore
		if (model_.m_player.m_score > model_.m_highScore)
			model_.m_highScore = model_.m_player.m_score;

		Observation obs = formObservations(rete_);
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
			Action act = optimalAgent.agent_start(formObservations(rete_));
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
			model_.m_highScore = 0;
			resetEnvironment();
		}
	}

	/**
	 * Resets the environment back to normal.
	 */
	public void resetEnvironment() {
		boolean noDots = model_.noDots_;
		boolean noPowerDots = model_.noPowerDots_;
		boolean oneLife = model_.oneLife_;
		int highScore = model_.m_highScore;

		environment_.reinit();

		model_ = environment_.getGameModel();
		model_.noDots_ = noDots;
		model_.noPowerDots_ = noPowerDots;
		model_.oneLife_ = oneLife;
		model_.m_highScore = highScore;
		model_.setRandom(PolicyGenerator.random_);
		rete_ = StateSpec.getInstance().getRete();

		prevScore_ = 0;

		// Letting the thread 'sleep' when not experiment mode, so it's
		// watchable for humans.
		try {
			rete_.reset();
			if (!experimentMode_)
				Thread.sleep(playerDelay_);
		} catch (Exception e) {
			e.printStackTrace();
		}
		model_.m_state = GameModel.STATE_NEWGAME;
		environment_.m_gameUI.m_bDrawPaused = false;

		lastDirection_ = PacManLowAction.NOTHING;

		while (!model_.isLearning()) {
			environment_.tick(false);
		}
	}

	/**
	 * Draws the agent's action switch.
	 * 
	 * @param actions
	 *            The agent's current actions. Should always be the same size
	 *            with possible null elements.
	 */
	private void drawActions(ArrayList<RuleAction> actions) {
		if (!experimentMode_) {
			environment_.m_bottomCanvas.setActionsList(actions
					.toArray(new RuleAction[actions.size()]));
		}
	}

	/**
	 * Chooses a direction based off the chosen high actions to follow.
	 * 
	 * @param actions
	 *            The prioritised actions the agent will take, joined together
	 *            into a weighted singular direction to take.
	 * @return The low action to use.
	 */
	private PacManLowAction chooseLowAction(ArrayList<RuleAction> actions) {
		// Find the valid directions
		ArrayList<PacManLowAction> directions = new ArrayList<PacManLowAction>();
		int x = model_.m_player.m_locX;
		int y = model_.m_player.m_locY;
		if (Thing.isValidMove(Thing.UP, x, y, model_))
			directions.add(PacManLowAction.UP);
		if (Thing.isValidMove(Thing.DOWN, x, y, model_))
			directions.add(PacManLowAction.DOWN);
		if (Thing.isValidMove(Thing.LEFT, x, y, model_))
			directions.add(PacManLowAction.LEFT);
		if (Thing.isValidMove(Thing.RIGHT, x, y, model_))
			directions.add(PacManLowAction.RIGHT);
		double inverseDirs = 1d / directions.size();

		// Compile the state
		int safestJunction = -Integer.MAX_VALUE;
		for (Junction junc : closeJunctions_)
			safestJunction = Math.max(safestJunction, junc.getSafety());
		Object[] stateObjs = { model_.m_ghosts, model_.m_fruit, distanceGrid_,
				safestJunction, model_.m_player };
		PacManState state = new PacManState(stateObjs);

		// Run through each action, until a clear singular direction is arrived
		// upon.
		int i = 0;
		double[] globalDirectionVote = new double[PacManLowAction.values().length];
		double globalBest = 0;
		for (RuleAction ruleAction : actions) {
			double[] directionVote = new double[PacManLowAction.values().length];
			double worst = 0;
			double bestWeight = 0;
			List<StringFact> actionStrings = ruleAction.getTriggerActions();

			// Find the individual distance weighting and direction of each
			// action in the ArrayList.
			for (StringFact action : actionStrings) {
				// For each rule, a list of actions are returned
				WeightedDirection weightedDir = ((PacManStateSpec) StateSpec
						.getInstance()).applyAction(action, state);

				if (weightedDir != null) {
					// Use a linearly decreasing weight and the object proximity
					double weighting = weightedDir.getWeight();
					bestWeight = Math.max(bestWeight, Math.abs(weighting));
					byte dir = (byte) Math.abs(weightedDir.getDirection());
					if (weightedDir.getDirection() > 0) {
						directionVote[dir] += weighting;
					} else if (weightedDir.getDirection() < 0) {
						directionVote[dir] -= weighting;
					}

					// Recording best and worst
					worst = Math.min(worst, directionVote[dir]);
				}
			}

			// Normalise the values
			double sum = 0;
			for (int j = 1; j < directionVote.length; j++) {
				// Making all values positive
				directionVote[j] -= worst;
				sum += directionVote[j];
			}

			// Using position within the action list as a weighting influence
			double inverseNumberWeight = (1.0 * (actions.size() - i))
					/ actions.size();
			inverseNumberWeight *= inverseNumberWeight;

			// Add to the global direction vote
			for (int j = 1; j < directionVote.length; j++) {
				// Add to the global weight, using the position within the
				// policy and the highest weight as factors.
				directionVote[j] /= sum;
				globalDirectionVote[j] += directionVote[j]
						* inverseNumberWeight * bestWeight;
				if (globalDirectionVote[j] > globalBest)
					globalBest = globalDirectionVote[j];
			}

			i++;
		}

		// Normalise the globalDirectionVote and remove any directions not
		// significantly weighted.
		ArrayList<PacManLowAction> backupDirections = new ArrayList<PacManLowAction>(
				directions);
		for (int d = 0; d < globalDirectionVote.length; d++) {
			globalDirectionVote[d] /= globalBest;
			// If the vote is less than 1 - # valid directions, then remove
			// it.
			if (globalDirectionVote[d] <= 1d - inverseDirs)
				directions.remove(PacManLowAction.values()[d]);
		}
		if (directions.isEmpty())
			directions = backupDirections;

		// If only one direction left, use that
		if (directions.size() == 1) {
			lastDirection_ = directions.get(0);
			return directions.get(0);
		}

		// If possible, continue with the last direction.
		if (directions.contains(lastDirection_)) {
			return lastDirection_;
		}

		// Otherwise take a direction perpendicular to the last direction.
		for (PacManLowAction dir : directions) {
			if (dir != lastDirection_.opposite()) {
				lastDirection_ = dir;
				return dir;
			}
		}

		// Or just take the opposite direction. (Shouldn't get this far...)
		lastDirection_ = lastDirection_.opposite();
		return lastDirection_;
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
	private Observation formObservations(Rete rete) {
		try {
			rete.reset();

			// Player
			rete_.eval("(assert (pacman player))");

			// Load distance grid measures
			distanceGrid_ = distanceGridCache_.getGrid(model_.m_stage,
					model_.m_player.m_locX, model_.m_player.m_locY);
			closeJunctions_ = distanceGridCache_.getCloseJunctions(
					model_.m_stage, model_.m_player.m_locX,
					model_.m_player.m_locY);

			// Ghost Centre. Note that the centre can shift based on how the
			// ghosts are positioned, as the warp points make the map
			// continuous.
			int ghostCount = 0;
			Point naturalCentre = new Point(0, 0);
			Point natPrevGhost = null;
			double naturalDist = 0;
			Point offsetCentre = new Point(0, 0);
			double offsetDist = 0;
			Point offPrevGhost = null;
			// Ghosts
			boolean junctionsNoted = false;
			for (Ghost ghost : model_.m_ghosts) {
				// Don't note ghost if it is running back to hideout or if it is
				// in hideout
				if ((ghost.m_nTicks2Exit <= 0) && (!ghost.m_bEaten)) {
					rete_.eval("(assert (ghost " + ghost + "))");
					// If edible, add assertion
					if (ghost.isEdible()) {
						rete_.eval("(assert (edible " + ghost + "))");
					}
					// If flashing, add assertion
					if (ghost.isBlinking()) {
						rete_.eval("(assert (blinking " + ghost + "))");
					}

					// Distances from pacman to ghost
					distanceAssertions(ghost, ghost.toString(), model_.m_player);

					// Junction distance
					for (Junction junc : closeJunctions_) {
						if (!junctionsNoted) {
							// Assert types
							rete_.eval("(assert (junction " + junc + "))");
							// Max safety
							junc.setSafety(model_.m_gameSizeX);
						}

						// Junction Safety
						int safety = model_.m_gameSizeX;
						if (!ghost.isEdible()) {
							DistanceDir[][] ghostGrid = distanceGridCache_
									.getGrid(model_.m_stage, ghost.m_locX,
											ghost.m_locY);
							// If the ghost is in the ghost area or otherwise
							// not accessible by PacMan, ignore it.
							if (ghostGrid != null) {
								int ghostDistance = ghostGrid[junc.m_locX][junc.m_locY]
										.getDistance();
								if (ghostDistance >= 0)
									safety = ghostDistance - junc.getDistance();
							}
						}

						if (safety < junc.getSafety())
							junc.setSafety(safety);
					}
					junctionsNoted = true;

					// Ghost Centre calcs
					ghostCount++;
					Point natPoint = new Point(ghost.m_locX, ghost.m_locY);
					naturalCentre.x += natPoint.x;
					naturalCentre.y += natPoint.y;
					if (natPrevGhost != null)
						naturalDist += natPoint.distance(natPrevGhost);
					natPrevGhost = natPoint;

					Point offPoint = new Point(
							(ghost.m_locX + model_.m_gameSizeX / 2)
									% model_.m_gameSizeX, ghost.m_locY);
					offsetCentre.x += offPoint.x;
					offsetCentre.y += offPoint.y;
					if (offPrevGhost != null)
						offsetDist += offPoint.distance(offPrevGhost);
					offPrevGhost = offPoint;
				}
			}

			// Assert junctions
			for (Junction junc : closeJunctions_) {
				if (!junctionsNoted) {
					// Assert types
					rete_.eval("(assert (junction " + junc + "))");
					// Max safety
					junc.setSafety(model_.m_gameSizeX);
				}

				// Assert safety
				rete_.eval("(assert (junctionSafety " + junc + " "
						+ junc.getSafety() + "))");
			}

			// Assert ghost centres
			if (ghostCount > 0) {
				Point centrePoint = null;
				if (naturalDist <= offsetDist)
					centrePoint = naturalCentre;
				else {
					// Reset the offset centre
					offsetCentre.x = (offsetCentre.x - model_.m_gameSizeX / 2 + model_.m_gameSizeX)
							% model_.m_gameSizeX;
					centrePoint = offsetCentre;
				}
				centrePoint.x /= ghostCount;
				centrePoint.y /= ghostCount;
				GhostCentre gc = new GhostCentre(centrePoint);

				rete_.eval("(assert (ghostCentre " + gc + "))");

				distanceAssertions(gc, gc.toString(), model_.m_player);
			}

			// Dots
			for (Dot dot : model_.m_dots.values()) {
				rete_.eval("(assert (dot " + dot + "))");

				// Distances
				distanceAssertions(dot, dot.toString(), model_.m_player);
			}

			// Powerdots
			for (PowerDot powerdot : model_.m_powerdots.values()) {
				rete_.eval("(assert (powerDot " + powerdot + "))");

				// Distances
				distanceAssertions(powerdot, powerdot.toString(),
						model_.m_player);
			}

			// Fruit
			if (model_.m_fruit.isEdible()) {
				rete_.eval("(assert (fruit " + model_.m_fruit + "))");

				// Distances
				distanceAssertions(model_.m_fruit, model_.m_fruit.toString(),
						model_.m_player);
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
	 * @param pacMan
	 * @throws JessException
	 *             If Jess goes wrong.
	 */
	private void distanceAssertions(PacPoint thing, String thingName,
			Player pacMan) throws JessException {
		if (distanceGrid_[thing.m_locX][thing.m_locY] != null) {
			// Use Ms. PacMan's natural distance (manhatten)
			rete_.eval("(assert (distance" + thing.getClass().getSimpleName()
					+ " player " + thingName + " "
					+ distanceGrid_[thing.m_locX][thing.m_locY].getDistance()
					+ "))");
		} else {
			// Use Euclidean distance, rounding
			int distance = (int) Math.round(Point2D.distance(thing.m_locX,
					thing.m_locY, pacMan.m_locX, pacMan.m_locY));
			rete_.eval("(assert (distance" + thing.getClass().getSimpleName()
					+ " player " + thingName + " " + distance + "))");
		}
	}

	/**
	 * Resets the observation and distance grid array.
	 */
	private void cacheDistanceGrids() {
		distanceGridCache_ = new DistanceGridCache(model_,
				model_.m_player.m_startX, model_.m_player.m_startY);
	}

	/**
	 * @return the model_
	 */
	public GameModel getModel() {
		return model_;
	}

	public DistanceDir[][] getDistanceGrid() {
		return distanceGrid_;
	}
}
