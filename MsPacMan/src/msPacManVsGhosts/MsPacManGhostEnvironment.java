package msPacManVsGhosts;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import jess.JessException;
import jess.Rete;
import game.controllers.GhostController;
import game.controllers.examples.Legacy2TheReckoning;
import game.core.G;
import game.core.Game;
import game.core.GameView;
import game.core._G_;
import game.core.Game.DM;
import relationalFramework.FiredAction;
import relationalFramework.PolicyActions;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;
import rrlFramework.RRLEnvironment;
import util.Pair;

public class MsPacManGhostEnvironment extends RRLEnvironment {
	private boolean experimentMode_ = false;
	private _G_ game_;
	private final String[] ghostNames = { "blinky", "pinky", "inky", "clyde" };
	private GhostController ghosts_;
	private GameView gv_;
	private int prevLives_;
	private int prevScore_;

	/**
	 * Makes an assertion of distance for a particular thing.
	 * 
	 * @param thing
	 *            The thing to assert to.
	 * @param distance
	 *            The distance of the thing from pacman.
	 * @param rete
	 *            The relational state.
	 * @throws JessException
	 *             If something goes awry...
	 */
	private void distanceAssertion(String thing, int distance, Rete rete)
			throws JessException {
		rete.assertString("(distance " + thing + " " + (distance / 4) + ")");
	}

	/**
	 * Gets the direction of this action.
	 * 
	 * @param action
	 *            The action to apply.
	 * @param game
	 *            The game state.
	 * @return A pair: an int direction to move to + 1 (so UP == 1 instead of
	 *         0). If the direction is negative, that means NOT moving to that
	 *         particular positive direction (still factoring in the +1); and
	 *         the location of the thing being acted upon.
	 */
	private Pair<Integer, Integer> getDirection(RelationalPredicate action,
			Game game) {
		String[] arguments = action.getArguments();

		// First parse the location of the object
		int index = -1;
		String[] split = arguments[0].split("_");
		if (split.length < 2) {
			// Check for ghost name
			for (int i = 0; i < ghostNames.length; i++) {
				if (split[0].equals(ghostNames[i])) {
					index = game.getCurGhostLoc(i);
					break;
				}
			}
		} else
			index = Integer.parseInt(split[1]);

		Pair<Integer, Integer> result = new Pair<Integer, Integer>(
				Integer.MAX_VALUE, -1);
		if (action.getFactName().equals("moveTo")
				|| action.getFactName().equals("toJunction")) {
			result = new Pair<Integer, Integer>(game.getNextPacManDir(index,
					true, DM.PATH) + 1, index);
		} else if (action.getFactName().equals("moveFrom")) {
			// result = new Pair<Integer, Integer>(-game.getNextPacManDir(index,
			// true, DM.PATH) - 1, index);
			result = new Pair<Integer, Integer>(game.getNextPacManDir(index,
					false, DM.PATH) + 1, index);
		}

		return result;
	}

	/**
	 * Determines the weight of the action based on the proximity of the object
	 * and action type.
	 * 
	 * @param action
	 *            The action to get the weight for.
	 * @return A weight inversely proportional to the distance or equal to the
	 *         junction safety.
	 */
	private double getWeight(RelationalPredicate action) {
		double numArg = Double.parseDouble(action.getArguments()[1]);
		if (action.getFactName().equals("toJunction"))
			return numArg;

		// Fixing the distance to a minimum of 1
		if (numArg >= 0 && numArg < 1)
			numArg = 0.01;
		if (numArg <= 0 && numArg > -1)
			numArg = -0.01;

		return 1.0 / numArg;
	}

	@Override
	protected void assertStateFacts(Rete rete, List<String> goalArgs)
			throws Exception {

		int pacPos = game_.getCurPacManLoc();

		// Calculate closest junctions
		int[] closestJuncs = new int[4];
		Arrays.fill(closestJuncs, -1);
		int[] juncDists = new int[4];
		Arrays.fill(juncDists, Integer.MAX_VALUE);
		for (int junc : game_.getJunctionIndices()) {
			// If Pac-Man is already at this junction, disregard it
			if (pacPos != junc) {
				// Find the direction and distance to a junction
				int dir = game_.getNextPacManDir(junc, true, DM.PATH);
				int dist = game_.getPathDistance(pacPos, junc);
				// Find closest junction
				if (dist < juncDists[dir]) {
					juncDists[dir] = dist;
					closestJuncs[dir] = junc;
				}
			}
		}

		// Ghosts
		int[] juncSafety = new int[4];
		Arrays.fill(juncSafety, Integer.MAX_VALUE);
		// int numActiveGhosts = 0;
		// double centreX = 0;
		// double centreY = 0;
		for (int g = 0; g < Game.NUM_GHOSTS; g++) {
			int ghostPos = game_.getCurGhostLoc(g);
			if (game_.getLairTime(g) == 0) {
				String ghost = ghostNames[g];
				rete.assertString("(ghost " + ghost + ")");

				int pacGhostDist = game_.getPathDistance(pacPos, ghostPos);
				distanceAssertion(ghost, pacGhostDist, rete);

				// If edible, add assertion
				if (game_.isEdible(g)) {
					rete.assertString("(edible " + ghost + ")");

					// If flashing, add assertion
					if (game_.getEdibleTime(g) <= _G_.EDIBLE_ALERT) {
						rete.assertString("(blinking " + ghost + ")");
					}
				} else {
					// Calculating junction safety
					for (int j = 0; j < closestJuncs.length; j++) {
						if (closestJuncs[j] != -1) {
							int ghostDistance = game_.getGhostPathDistance(g,
									closestJuncs[j]);
							int thisGhostDist = ghostDistance - juncDists[j];
							// Special case for when a ghost is between
							// PacMan and the junction
							int ghostJuncDist = game_.getPathDistance(ghostPos,
									closestJuncs[j]);
							if (pacGhostDist <= ghostJuncDist
									&& game_.getNextPacManDir(ghostPos, true,
											DM.PATH) == game_.getNextPacManDir(
											closestJuncs[j], true, DM.PATH))
								thisGhostDist = ghostJuncDist - juncDists[j];

							juncSafety[j] = Math.min(juncSafety[j],
									thisGhostDist / 4);
						}
					}
				}
			}
		}

		// Asserting junctions
		for (int j = 0; j < closestJuncs.length; j++) {
			if (closestJuncs[j] != -1) {
				String junc = "junc_" + closestJuncs[j];
				rete.assertString("(junction " + junc + ")");
				if (juncSafety[j] == Integer.MAX_VALUE)
					juncSafety[j] = 1;
				rete.assertString("(junctionSafety " + junc + " "
						+ juncSafety[j] + ")");
			}
		}

		// Asserting ghost centre
		// if (numActiveGhosts > 0) {
		// centreX /= numActiveGhosts;
		// centreY /= numActiveGhosts;
		// double dist = Point2D.distance(game.getX(pacPos),
		// game.getY(pacPos), centreX, centreY);
		// String gc = "ghostCentre_"
		// + (int) (Math.round(centreY) + Math
		// .round(centreX));
		// rete_.assertString("(ghostCentre " + gc + ")");
		// distanceAssertion(gc, (int) Math.round(dist));
		// }

		// Dots
		int[] dots = game_.getPillIndices();
		for (int i = 0; i < dots.length; i++) {
			if (game_.checkPill(i)) {
				String dotName = "dot_" + dots[i];
				rete.assertString("(dot " + dotName + ")");

				// Distances
				int dist = game_.getPathDistance(pacPos, dots[i]);
				distanceAssertion(dotName, dist, rete);
			}
		}

		// PowerDots
		int[] powerDots = game_.getPowerPillIndices();
		for (int i = 0; i < powerDots.length; i++) {
			if (game_.checkPowerPill(i)) {
				String pDotName = "powerDot_" + powerDots[i];
				rete.assertString("(powerDot " + pDotName + ")");

				// Distances
				distanceAssertion(pDotName,
						game_.getPathDistance(pacPos, powerDots[i]), rete);
			}
		}

		// Score, level, lives, highscore...
		rete.assertString("(level " + game_.getCurLevel() + ")");
		rete.assertString("(lives " + game_.getLivesRemaining() + ")");
		rete.assertString("(score " + game_.getScore() + ")");
	}

	/**
	 * Calculates the reward between states.. May also include an artificial
	 * reward of -10000 if a life is lost.
	 * 
	 * @return The difference in score.
	 */
	protected double calculateReward(boolean isTerminal) {
		double scoreDiff = game_.getScore() - prevScore_;
		prevScore_ = game_.getScore();
		if (StateSpec.getInstance().getGoalName().equals("survive")
				&& game_.getLivesRemaining() < prevLives_)
			scoreDiff -= (prevLives_ - game_.getLivesRemaining()) * 10000;
		prevLives_ = game_.getLivesRemaining();
		return scoreDiff;
	}

	@Override
	protected List<String> getGoalArgs() {
		return null;
	}

	@Override
	protected Object groundActions(PolicyActions actions) {
		Collection<Integer> pacManDirs = new HashSet<Integer>();
		for (int pacDir : game_.getPossiblePacManDirs(true))
			pacManDirs.add(pacDir);

		// Run through the actions until a decision is reached.
		for (Collection<FiredAction> firedActions : actions.getActions()) {
			Collection<Integer> actionDirections = new HashSet<Integer>();
			double bestWeight = Integer.MIN_VALUE;

			// Only use the closest object(s) to make decisions
			for (FiredAction firedAction : firedActions) {
				Pair<Integer, Integer> direction = getDirection(
						firedAction.getAction(), game_);
				double weight = getWeight(firedAction.getAction());
				// Only note the best weighted actions
				if (weight > bestWeight) {
					actionDirections.clear();
					bestWeight = weight;
				}
				if (weight == bestWeight) {
					if (direction.objA_ < 0) {
						// Add all directions BUT this one
						for (int pacDir : pacManDirs) {
							if (pacDir != -direction.objA_ - 1)
								actionDirections.add(pacDir);
						}
					} else {
						actionDirections.add(direction.objA_ - 1);
					}
				}
			}

			// Narrow the set of directions
			Collection<Integer> backupDirections = new HashSet<Integer>(
					pacManDirs);
			pacManDirs.retainAll(actionDirections);
			// If no legal directions selected
			if (pacManDirs.isEmpty())
				pacManDirs = backupDirections;
			// If the possible directions has shrunk, this rule has been
			// utilised
			if (pacManDirs.size() < backupDirections.size()) {
				for (FiredAction fa : firedActions)
					fa.triggerRule();
			}
			// If there is only one direction left, use that
			if (pacManDirs.size() == 1)
				return pacManDirs.iterator().next();
		}

		int lastDir = game_.getCurPacManDir();
		if (!pacManDirs.contains(lastDir)) {
			// If Ms. Pac-Man cannot continue in the same direction, choose a
			// perpendicular direction
			for (Integer dir : pacManDirs) {
				if (dir != game_.getReverse(lastDir))
					return dir;
			}
		}
		return lastDir;
	}

	@Override
	protected boolean isReteDriven() {
		return false;
	}

	/**
	 * Resets the environment back to normal.
	 */
	@Override
	protected void startState() {
		game_.newGame();
		if (!experimentMode_ && gv_ == null)
			gv_ = new GameView(game_).showGame();
		// TODO Define proper Ms. Pac-Man Ghost behaviour
		ghosts_ = new Legacy2TheReckoning();
		prevScore_ = game_.getScore();
		prevLives_ = game_.getLivesRemaining();
	}

	@Override
	protected void stepState(Object action) {
		game_.advanceGame((Integer) action, ghosts_.getActions(game_, G.DELAY));
		if (!experimentMode_) {
			gv_.repaint();
		}
	}

	@Override
	public void cleanup() {
		game_ = null;
	}

	@Override
	public void initialise(int runIndex, String[] extraArgs) {
		game_ = new _G_();

		for (String arg : extraArgs) {
			if (arg.equals("-e")) {
				// Run the program in experiment mode (No GUI).
				experimentMode_ = true;
			}
		}
	}

	@Override
	public boolean isTerminal() {
		return game_.gameOver() || super.isTerminal();
	}
}
