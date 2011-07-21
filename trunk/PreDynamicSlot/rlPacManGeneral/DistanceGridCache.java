package rlPacManGeneral;

import java.awt.Color;
import java.awt.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import msPacMan.GameModel;
import msPacMan.Junction;
import msPacMan.Thing;

/**
 * A cache of precalculated distance grids for each and every valid position in
 * all of Ms. PacMan's levels.
 * 
 * @author Sam Sarjant
 */
public class DistanceGridCache {
	/** The collection of grids mapped to each level. */
	private Map<Integer, DistanceGrid[][]> grids_;

	public DistanceGridCache(GameModel model, int startX, int startY) {
		grids_ = new HashMap<Integer, DistanceGrid[][]>();
		initialiseGrids(model, startX, startY);
	}

	/**
	 * Initialises the distance grids.
	 * 
	 * @param model
	 *            The model of the levels for Ms. PacMan.
	 * @param startY
	 *            The starting Y position.
	 * @param startX
	 *            The starting X position.
	 */
	private void initialiseGrids(GameModel model, int startX, int startY) {
		model.m_stage = 0;
		model.loadNextLevel();

		// For each stage
		Color prevLevelColor = null;
		for (int stage = 0; stage < GameModel.MAX_LEVELS; stage++) {
			// If this level is the same as the last (determined by colour), use
			// the last level's calculations.
			if (model.m_pacMan.m_gameUI.m_wallColor.equals(prevLevelColor)) {
				grids_.put(stage, grids_.get(stage - 1));
			} else {
				DistanceGrid[][] grids = new DistanceGrid[model.m_gameSizeX][model.m_gameSizeY];

				// Maintain a collection of valid positions to take distances
				// from.
				Collection<Point> validPositions = new HashSet<Point>();

				Iterator<Point> iter = null;
				// Perform the initial scan, then iterate through every other
				// valid position.
				while ((iter == null) || (iter.hasNext())) {
					// Initialise a new distance grid.
					Map<Byte, DistanceDir[][]> distanceGrids = new HashMap<Byte, DistanceDir[][]>();
					//for (byte b = 0; b < PacManLowAction.values().length; b++) {
					for (byte b = 0; b < 1; b++) {
						distanceGrids
								.put(
										b,
										new DistanceDir[model.m_gameSizeX][model.m_gameSizeY]);
					}
					SortedSet<Junction> closeJunctions = null;

					Point origin = null;
					if (iter == null) {
						origin = new Point(startX, startY);
						// Initial searching to fill the validPositions
						closeJunctions = searchMaze(startX, startY,
								distanceGrids, validPositions, model);
						// Remove the initial position, so it isn't scanned
						// twice and set the iterator.
						validPositions.remove(origin);
						iter = validPositions.iterator();
					} else {
						origin = iter.next();
						// Search from a valid position
						closeJunctions = searchMaze(origin.x, origin.y,
								distanceGrids, null, model);
					}

					// Insert the grid
					grids[origin.x][origin.y] = new DistanceGrid(distanceGrids,
							closeJunctions);
				}

				// Add the grids
				grids_.put(stage, grids);
			}
			prevLevelColor = model.m_pacMan.m_gameUI.m_wallColor;
			model.loadNextLevel();
		}
	}

	/**
	 * Searches the maze for observations. Does this by expanding outwards in
	 * junctions, recording the distance to each.
	 * 
	 * @param originX
	 *            The origin X position.
	 * @param originY
	 *            The origin Y position.
	 * @param distanceGrids
	 *            The distance grids to fill.
	 * @param validLocations
	 *            A collection to fill with valid locations, or null if not
	 *            filling it.
	 * @param model
	 *            The game model.
	 * @return The closest immediate junctions to the origin point.
	 */
	public SortedSet<Junction> searchMaze(int originX, int originY,
			Map<Byte, DistanceDir[][]> distanceGrids,
			Collection<Point> validLocations, GameModel model) {
		SortedSet<Junction> closeJunctions = new TreeSet<Junction>();

		Point playerLoc = new Point(originX, originY);

		// Check every direction
		//for (byte dir = 0; dir < PacManLowAction.values().length; dir++) {
		for (byte dir = 0; dir < 1; dir++) {
			DistanceDir[][] distanceGrid = distanceGrids.get(dir);

			// Update the distance grid
			Set<Point> knownJunctions = new HashSet<Point>();
			// Check for junctions here.
			Set<Junction> thisLoc = isJunction(playerLoc, 0, model, Thing.STILL);
			if (thisLoc != null) {
				Point p = thisLoc.iterator().next().getLocation();
				knownJunctions.add(p);
			}

			SortedSet<Junction> junctionStack = new TreeSet<Junction>();
			// Add the initial junction points to the stack
			if (dir != Thing.DOWN && dir != Thing.UP) {
				junctionStack
						.add(new Junction(playerLoc, Thing.UP, 0, Thing.UP));
				junctionStack.add(new Junction(playerLoc, Thing.DOWN, 0,
						Thing.DOWN));
			}
			if (dir != Thing.RIGHT && dir != Thing.LEFT) {
				junctionStack.add(new Junction(playerLoc, Thing.LEFT, 0,
						Thing.LEFT));
				junctionStack.add(new Junction(playerLoc, Thing.RIGHT, 0,
						Thing.RIGHT));
			}
			// Special case for directions - backup junctions (may not be used
			// at all)
			Set<Junction> backupJunctions = null;
			if (dir != Thing.STILL) {
				backupJunctions = new HashSet<Junction>(junctionStack);
				junctionStack.clear();
				junctionStack.add(new Junction(playerLoc, dir, 0, dir));
			}

			distanceGrid[playerLoc.x][playerLoc.y] = new DistanceDir(0,
					Thing.STILL);

			// Keep following junctions until all have been found
			while (!junctionStack.isEmpty()) {
				Junction point = junctionStack.first();
				junctionStack.remove(point);

				Set<Junction> nextJunction = searchToJunction(point,
						knownJunctions, model, distanceGrid, validLocations);
				junctionStack.addAll(nextJunction);

				// Checking for the immediate junctions
				if (point.getLocation().equals(playerLoc)) {
					// If the direction is still, record the close junctions
					if (dir == Thing.STILL && !nextJunction.isEmpty()) {
						closeJunctions.add(nextJunction.iterator().next());
					}

					// If we have backup directions and the initial junction was
					// invalid, load the backups.
					if (backupJunctions != null && junctionStack.isEmpty()) {
						junctionStack.addAll(backupJunctions);
						backupJunctions = null;
					}
				}
			}

//			printDistanceGrid(distanceGrid);
		}

		return closeJunctions;
	}

	/**
	 * A method for searching for the shortest distance from junction to
	 * junction.
	 * 
	 * @param startingPoint
	 *            The starting point for the junction search.
	 * @param knownJunctions
	 *            The known junctions.
	 * @param model
	 *            The game model.
	 * @param distanceGrid
	 *            The distance grid to fill.
	 * @param validPositions
	 *            The valid positions to fill (if not null).
	 * @return The set of starting points for the next found junction or an
	 *         empty set.
	 */
	private Set<Junction> searchToJunction(Junction startingPoint,
			Set<Point> knownJunctions, GameModel model,
			DistanceDir[][] distanceGrid, Collection<Point> validPositions) {
		byte direction = startingPoint.getDirection();
		byte origDirection = startingPoint.getOrigDirection();
		int x = startingPoint.getLocation().x;
		int y = startingPoint.getLocation().y;
		int distance = startingPoint.getDistance();

		// Checking for an invalid request to move
		if (!Thing.isValidMove(direction, x, y, model)) {
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
			x = (x + model.m_gameSizeX) % model.m_gameSizeX;
			y = (y + model.m_gameSizeY) % model.m_gameSizeY;

			// Noting the valid positions.
			Point point = new Point(x, y);
			if ((validPositions != null) && (!validPositions.contains(point)))
				validPositions.add(point);

			// Note the distance
			distance++;
			if ((distanceGrid[x][y] == null)
					|| (distance < distanceGrid[x][y].getDistance())) {
				changed = true;
				distanceGrid[x][y] = new DistanceDir(distance, origDirection);
			}

			// Check if the new position is a junction
			isJunct = isJunction(new Point(x, y), distance, model,
					startingPoint.getOrigDirection());

			// If not, find the next direction
			if (isJunct == null) {
				for (byte d = 1; d <= 4; d++) {
					// If the direction isn't the direction came from
					if (d != oldDir) {
						if (Thing.getDestination(d, x, y, new Point(), model)) {
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
	 * Checks if the coordinates are a junction/corner. If they are, returns a
	 * bitwise representation of the directions to go.
	 * 
	 * @param loc
	 *            The location of the possible junction.
	 * @param distance
	 *            The current distance of the junction.
	 * @param model
	 *            The game model.
	 * @param origDir
	 *            The original direction the junction was encountered from.
	 * @return A list of the possible directions the junction goes or null if no
	 *         junction.
	 */
	private Set<Junction> isJunction(Point loc, int distance, GameModel model,
			byte origDir) {
		Set<Junction> dirs = new HashSet<Junction>();
		int[] modelDirs = { GameModel.GS_NORTH, GameModel.GS_SOUTH,
				GameModel.GS_EAST, GameModel.GS_WEST };
		byte[] actionDirs = { Thing.UP, Thing.DOWN, Thing.RIGHT, Thing.LEFT };
		for (int i = 0; i < modelDirs.length; i++) {
			if ((model.m_gameState[loc.x][loc.y] & modelDirs[i]) == 0) {
				if (origDir == Thing.STILL)
					dirs.add(new Junction(loc, actionDirs[i], distance,
							actionDirs[i]));
				else
					dirs
							.add(new Junction(loc, actionDirs[i], distance,
									origDir));
			}
		}

		if (dirs.size() > 2)
			return dirs;
		return null;
	}

	/**
	 * Gets the distance grid for a particular stage at a given location.
	 * 
	 * @param level
	 *            The level to load the grid for.
	 * @param originX
	 *            The x origin point.
	 * @param originY
	 *            The y origin point
	 * @param direction
	 *            The direction of the thing at the origin, if relevant.
	 * @return The distance grid for the given parameters.
	 */
	public DistanceDir[][] getGrid(int level, int originX, int originY,
			byte direction) {
		level = (level - 1) % GameModel.MAX_LEVELS + 1;
		DistanceGrid grid = grids_.get(level - 1)[originX][originY];
		if (grid == null)
			return null;
		return grid.getDistanceGrid(direction);
	}

	/**
	 * Gets the junctions closest to the origin point.
	 * 
	 * @param level
	 *            the level to load the junctions from.
	 * @param originX
	 *            The x origin point.
	 * @param originY
	 *            The y origin point.
	 * @return The immediate junctions for the given parameters.
	 */
	public Collection<Junction> getCloseJunctions(int level, int originX,
			int originY) {
		level = (level - 1) % GameModel.MAX_LEVELS + 1;
		return grids_.get(level - 1)[originX][originY].getCloseJunctions();
	}

	/**
	 * A debug feature that allows visual inspection of the distance grid.
	 */
	public static void printDistanceGrid(DistanceDir[][] distanceGrid) {
		StringBuffer buffer = new StringBuffer();
		for (int y = 0; y < distanceGrid[0].length; y++) {
			for (int x = 0; x < distanceGrid.length; x++) {
				if (distanceGrid[x][y] == null)
					buffer.append("  ");
				else {
					String val = distanceGrid[x][y].getDistance() + "";
//					switch (distanceGrid[x][y].getDirection()) {
//					case Thing.UP:
//						val = "U";
//						break;
//					case Thing.DOWN:
//						val = "D";
//						break;
//					case Thing.LEFT:
//						val = "L";
//						break;
//					case Thing.RIGHT:
//						val = "R";
//						break;
//					}
//					buffer.append(" " + val);
					 if (distanceGrid[x][y].getDistance() < 10)
					 buffer.append(" " + val);
					 else
					 buffer.append(val);
				}
				buffer.append(" ");
			}
			buffer.append("\n");
		}

		System.out.println(buffer);
	}

	/**
	 * A wrapper class for convenience.
	 * 
	 * @author Sam Sarjant
	 */
	private class DistanceGrid {
		/** The actual distance grids for this position, mapped by direction. */
		private Map<Byte, DistanceDir[][]> distanceGrids_;

		/** The closest junctions to the origin. */
		private Collection<Junction> closeJunctions_;

		/**
		 * A constructor for a distance grid.
		 * 
		 * @param distanceGrid
		 *            The distance grids for the location, mapped by direction.
		 * @param closeJunctions
		 *            The closest junctions to the origin location.
		 */
		public DistanceGrid(Map<Byte, DistanceDir[][]> distanceGrids,
				Collection<Junction> closeJunctions) {
			distanceGrids_ = distanceGrids;
			closeJunctions_ = closeJunctions;
		}

		public DistanceDir[][] getDistanceGrid(byte direction) {
			return distanceGrids_.get(direction);
		}

		public Collection<Junction> getCloseJunctions() {
			return closeJunctions_;
		}
	}
}
