/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/rlPacMan/PacManEnvironment.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package rlPacMan;

import relationalFramework.FiredAction;
import relationalFramework.PolicyActions;
import relationalFramework.StateSpec;
import rrlFramework.RRLEnvironment;
import rrlFramework.RRLExperiment;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import cerrla.ProgramArgument;

import jess.JessException;
import jess.Rete;

import msPacMan.Dot;
import msPacMan.GameModel;
import msPacMan.Ghost;
import msPacMan.GhostCentre;
import msPacMan.Junction;
import msPacMan.PacMan;
import msPacMan.PacPoint;
import msPacMan.Player;
import msPacMan.PowerDot;
import msPacMan.Thing;

public class PacManEnvironment extends RRLEnvironment {
	public static int playerDelay_ = 0;
	private Collection<Junction> closeJunctions_;
	private DistanceDir[][] distanceGrid_;
	private DistanceGridCache distanceGridCache_;
	private PacMan environment_;
	private boolean experimentMode_ = false;
	private PacManLowAction lastDirection_;
	private GameModel model_;
	private int prevScore_;

	/**
	 * Resets the observation and distance grid array.
	 */
	private void cacheDistanceGrids() {
		distanceGridCache_ = new DistanceGridCache(model_,
				model_.m_player.m_startX, model_.m_player.m_startY);
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
			Player pacMan, Rete rete) throws JessException {
		if (distanceGrid_[thing.m_locX][thing.m_locY] != null) {
			// Use Ms. PacMan's natural distance (manhatten)
			rete.assertString("(distance " + thingName + " "
					+ distanceGrid_[thing.m_locX][thing.m_locY].getDistance()
					+ ")");
		} else {
			// Use Euclidean distance, rounding
			int distance = (int) Math.round(Point2D.distance(thing.m_locX,
					thing.m_locY, pacMan.m_locX, pacMan.m_locY));
			rete.assertString("(distance " + thingName + " " + distance + ")");
		}
	}
	
	/**
	 * Draws the agent's action switch.
	 * 
	 * @param actions
	 *            The agent's current actions. Should always be the same size
	 *            with possible null elements.
	 */
	private void drawActions(ArrayList<Collection<FiredAction>> actions) {
		if (!experimentMode_) {
			FiredAction[] firedArray = new FiredAction[actions.size()];
			for (int i = 0; i < actions.size(); i++)
				firedArray[i] = actions.get(i).iterator().next();
			environment_.m_bottomCanvas.setActionsList(firedArray);
		}
	}

	@Override
	protected void assertStateFacts(Rete rete, List<String> goalArgs)
			throws Exception {
		// Load distance grid measures
		distanceGrid_ = distanceGridCache_.getGrid(model_.m_stage,
				model_.m_player.m_locX, model_.m_player.m_locY, Thing.STILL);
		closeJunctions_ = distanceGridCache_.getCloseJunctions(model_.m_stage,
				model_.m_player.m_locX, model_.m_player.m_locY);

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
				rete.assertString("(ghost " + ghost + ")");
				// If edible, add assertion
				if (ghost.isEdible()) {
					rete.assertString("(edible " + ghost + ")");
				}
				// If flashing, add assertion
				if (ghost.isBlinking()) {
					rete.assertString("(blinking " + ghost + ")");
				}

				// Distances from pacman to ghost
				distanceAssertions(ghost, ghost.toString(), model_.m_player,
						rete);

				// Junction distance
				for (Junction junc : closeJunctions_) {
					if (!junctionsNoted) {
						// Assert types
						rete.assertString("(junction " + junc + ")");
						// Max safety
						junc.setSafety(model_.m_gameSizeX);
					}

					// Junction Safety
					int safety = model_.m_gameSizeX;
					if (!ghost.isEdible()) {
						DistanceDir[][] ghostGrid = distanceGridCache_.getGrid(
								model_.m_stage, ghost.m_locX, ghost.m_locY,
								Thing.STILL);
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
				rete.assertString("(junction " + junc + ")");
				// Max safety
				junc.setSafety(model_.m_gameSizeX);
			}

			// Assert safety
			rete.assertString("(junctionSafety " + junc + " "
					+ junc.getSafety() + ")");
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

			rete.assertString("(ghostCentre " + gc + ")");

			distanceAssertions(gc, gc.toString(), model_.m_player, rete);
		}

		// Dots
		for (Dot dot : model_.m_dots.values()) {
			rete.assertString("(dot " + dot + ")");

			// Distances
			distanceAssertions(dot, dot.toString(), model_.m_player, rete);
		}

		// Powerdots
		for (PowerDot powerdot : model_.m_powerdots.values()) {
			rete.assertString("(powerDot " + powerdot + ")");

			// Distances
			distanceAssertions(powerdot, powerdot.toString(), model_.m_player,
					rete);
		}

		// Fruit
		if (model_.m_fruit.isEdible()) {
			rete.assertString("(fruit " + model_.m_fruit + ")");

			// Distances
			distanceAssertions(model_.m_fruit, model_.m_fruit.toString(),
					model_.m_player, rete);
		}

		// Score, level, lives, highScore
		rete.assertString("(level " + model_.m_stage + ")");
		rete.assertString("(lives " + model_.m_nLives + ")");
		rete.assertString("(score " + model_.m_player.m_score + ")");
		rete.assertString("(highScore " + model_.m_highScore + ")");
	}

	@Override
	protected double[] calculateReward(int isTerminal) {
		int scoreDiff = model_.m_player.m_score - prevScore_;
		prevScore_ += scoreDiff;
		return new double[] { scoreDiff, scoreDiff };
	}

	@Override
	protected List<String> getGoalArgs() {
		// There are no goal args in Ms. Pac-Man
		return null;
	}

	@Override
	protected Object groundActions(PolicyActions actions) {
		ArrayList<Collection<FiredAction>> policyActions = actions.getActions();

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



		// Compile the state
		Object[] stateObjs = { model_.m_ghosts, model_.m_fruit, distanceGrid_ };
		PacManState state = new PacManState(stateObjs);



		// Run through each action, until a clear singular direction is arrived
		// upon.
		Collection<FiredAction> firedRules = new HashSet<FiredAction>();
		for (Collection<FiredAction> firedActions : policyActions) {
			ArrayList<PacManLowAction> actionDirections = new ArrayList<PacManLowAction>();
			double bestWeight = Integer.MIN_VALUE;

			// Only use the closest object(s) to make decisions
			for (FiredAction firedAction : firedActions) {
				WeightedDirection weightedDir = ((PacManStateSpec) StateSpec
						.getInstance()).applyAction(firedAction.getAction(),
						state);

				if (weightedDir != null) {
					// Use a linearly decreasing weight and the object proximity
					double weighting = weightedDir.getWeight();
					// If weighting is higher, clear the previous decisions and
					// use this one.
					if (weighting > bestWeight) {
						actionDirections.clear();
						bestWeight = weighting;
					}
					byte dir = weightedDir.getDirection();
					// If this weighting is the same as the best weight, note
					// the direction.
					if (weighting == bestWeight) {
						if (dir < 0) {
							actionDirections.addAll(directions);
							actionDirections
									.remove(PacManLowAction.values()[-dir]);
						} else
							actionDirections.add(PacManLowAction.values()[dir]);
					}
				}
			}

			// Refine the directions selected by this rule
			ArrayList<PacManLowAction> backupDirections = new ArrayList<PacManLowAction>(
					directions);
			directions.retainAll(actionDirections);
			// If no legal directions selected
			if (directions.isEmpty())
				directions = backupDirections;
			// If the possible directions has shrunk, this rule has been
			// utilised
			if (directions.size() < backupDirections.size())
				firedRules.addAll(firedActions);
			// If there is only one direction left, use that
			if (directions.size() == 1) {
				lastDirection_ = directions.get(0);
				break;
			}
		}



		if (!directions.contains(lastDirection_)) {
			// If possible, continue with the last direction.
			// Otherwise take a direction perpendicular to the last
			// direction.
			for (PacManLowAction dir : directions) {
				if (dir != lastDirection_.opposite()) {
					lastDirection_ = dir;
				}
			}
		}



		// Trigger the rules leading to this direction
		for (FiredAction fa : firedRules)
			fa.triggerRule();

		// Draw the actions.
		if (!experimentMode_) {
			drawActions(policyActions);
		}

		return lastDirection_;
	}

	@Override
	protected boolean isReteDriven() {
		return false;
	}

	/**
	 * Checks if the episode has terminated.
	 * 
	 * @param obs
	 * 
	 * @return True if Game over or level complete, false otherwise.
	 */
	@Override
	protected int isTerminal() {
		if (super.isTerminal() == 1)
			return 1;
		if (model_.m_state == GameModel.STATE_GAMEOVER)
			return -1;
		return 0;
	}

	/**
	 * Resets the environment back to normal.
	 */
	@Override
	protected void startState() {
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
		model_.setRandom(RRLExperiment.random_);

		prevScore_ = 0;

		// Letting the thread 'sleep' when not experiment mode, so it's
		// watchable for humans.
		try {
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


		// If we're not in full experiment mode, redraw the scene.
		if (!experimentMode_) {
			environment_.m_gameUI.m_bRedrawAll = true;
			environment_.m_gameUI.repaint();
			environment_.m_topCanvas.repaint();
		} else {
			environment_.m_gameUI.update(null);
			environment_.m_gameUI.m_bRedrawAll = false;
		}
	}

	@Override
	protected void stepState(Object action) {
		try {
			if (!experimentMode_) {
				Thread.sleep(playerDelay_);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Apply the key
		environment_.simulateKeyPress(((PacManLowAction) action).getKey());

		// Cycle through a full cell
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
	}

	@Override
	public void cleanup() {
		environment_ = null;
	}

	@Override
	public void initialise(int runIndex, String[] extraArg) {
		environment_ = new PacMan();
		environment_.init(ProgramArgument.EXPERIMENT_MODE.booleanValue());

		model_ = environment_.getGameModel();
		// Survival mode
		if (StateSpec.getInstance().getGoalName().equals("survive"))
			model_.oneLife_ = true;

		// Initialise the observations
		cacheDistanceGrids();

		for (String arg : extraArg) {
			if ((arg.length() > 7) && (arg.substring(0, 6).equals("simple"))) {
				// PacMan simplified by removing certain aspects of it.
				String param = arg.substring(7);
				StateSpec.reinitInstance(param);
				if (param.equals("noDots")) {
					model_.noDots_ = true;
				} else if (param.equals("noPowerDots")) {
					model_.noPowerDots_ = true;
				}
			} else {
				try {
					int delay = Integer.parseInt(arg);
					playerDelay_ = delay;
				} catch (Exception e) {

				}
			}
		}
	}
}
