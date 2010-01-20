package rlPacMan;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mandarax.kernel.KnowledgeBase;
import org.mandarax.kernel.LogicFactory;
import org.mandarax.kernel.Predicate;
import org.mandarax.kernel.Prerequisite;
import org.mandarax.kernel.Rule;
import org.mandarax.kernel.SimplePredicate;
import org.mandarax.kernel.Term;

import relationalFramework.GuidedPredicate;
import relationalFramework.Policy;
import relationalFramework.PredTerm;
import relationalFramework.State;
import relationalFramework.StateSpec;

/**
 * The state specifications for the PacMan domain.
 * 
 * @author Sam Sarjant
 */
public class PacManStateSpec extends StateSpec {
	@Override
	protected Map<Class, Predicate> initialiseTypePredicates() {
		Map<Class, Predicate> typeMap = new HashMap<Class, Predicate>();
		// Simply load the arrays with names and classes and loop through.
		Class[] typeClasses = { Player.class, Dot.class, PowerDot.class,
				Ghost.class, Fruit.class, JunctionPoint.class, PacPoint.class,
				State.class };
		String[] predNames = { "pacman", "dot", "powerdot", "ghost", "fruit",
				"junction", "location", "state" };

		// Creating each type predicate
		for (int pred = 0; pred < typeClasses.length; pred++) {
			Class[] typeClass = { typeClasses[pred] };
			typeMap.put(typeClass[0], new SimplePredicate(predNames[pred],
					typeClass));
		}
		return typeMap;
	}

	@Override
	protected List<GuidedPredicate> initialisePredicates() {
		List<GuidedPredicate> predicates = new ArrayList<GuidedPredicate>();

		try {
			// NEAREST DOT
			predicates.add(pacPointPredicate("Dot", new Integer[] { 1, 1, 3, 6,
					12, 99 }, Dot.class, "proximalDot"));

			// NEAREST POWERDOT
			predicates.add(pacPointPredicate("Powerdot", new Integer[] { 9, 14,
					19, 24, 31, 99 }, PowerDot.class, "proximalPowerDot"));

			// NEAREST GHOST
			predicates.add(pacPointPredicate("Ghost", new Integer[] { 4, 6, 10,
					15, 21, 99 }, Ghost.class, "proximalGhost"));

			// NEAREST EDIBLE GHOST
			predicates
					.add(pacPointPredicate("Ghost", new Integer[] { 3, 6, 10,
							13, 19, 99 }, Ghost.class, "proximalEdibleGhost"));

			// NEAREST FRUIT
			predicates.add(pacPointPredicate("Fruit", new Integer[] { 2, 8, 13,
					18, 24, 99 }, Fruit.class, "proximalFruit"));

			// MAX JUNCTION SAFETY
			predicates.add(pacPointPredicate("Junction", new Integer[] { 2, 4,
					8, 13, 19, 99 }, JunctionPoint.class, "safeJunction"));

			// GHOST CENTRE DIST
			predicates.add(valuePredicate(new Double[] { 5.41, 7.92, 10.15,
					12.5, 15.79, 99.0 }, "ghostCentre"));

			// DOT CENTRE DIST
			predicates.add(valuePredicate(new Double[] { 4.12, 8.49, 11.31,
					14.0, 17.12, 99.0 }, "dotCentre"));

			// GHOST DENSITY
			predicates.add(valuePredicate(new Double[] { 0.0, 1.51, 4.90, 7.79,
					12.96, 99.0 }, "ghostDensity"));

			// TOTAL DIST TO GHOSTS
			predicates.add(valuePredicate(new Double[] { 19.38, 24.1, 28.25,
					33.08, 39.77, 99.0 }, "ghostDistanceSum"));

			// GHOSTS FLASHING
			predicates.add(createDefinedPredicate(PacManStateSpec.class,
					new Class[] { State.class },
					new PredTerm[][] { createTied("State", State.class) },
					"ghostsFlashing"));

			// EMPTY PREDICATE
			Predicate empty = new SimplePredicate("true",
					new Class[] { State.class });
			predicates.add(new GuidedPredicate(empty,
					new PredTerm[][] { createTied("State", State.class) }));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return predicates;
	}

	@Override
	protected List<GuidedPredicate> initialiseActions() {
		List<GuidedPredicate> actions = new ArrayList<GuidedPredicate>();

		try {
			// MOVE TOWARDS
			Class[] moveStructure = { State.class, PacPoint.class };
			PredTerm[][] predVals = new PredTerm[2][];
			predVals[0] = createTied("State", State.class);
			predVals[1] = createTied("Location", PacPoint.class);
			actions.add(createDefinedPredicate(PacManStateSpec.class,
					moveStructure, predVals, "moveTowards"));

			// MOVE FROM
			actions.add(createDefinedPredicate(PacManStateSpec.class,
					moveStructure, predVals, "moveFrom"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return actions;
	}

	@Override
	protected int initialiseActionsPerStep() {
		return 3;
	}

	@Override
	protected Map<Predicate, Rule> initialiseActionPreconditions(List<GuidedPredicate> actions) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Rule initialiseGoalState(LogicFactory factory) {
		// Creating the prereqs
		List<Prerequisite> prereqs = new ArrayList<Prerequisite>();
		// TODO Fill in the goal.

		return factory.createRule(prereqs, StateSpec.getTerminalFact(factory));
	}

	@Override
	protected Policy initialiseOptimalPolicy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected KnowledgeBase initialiseBackgroundKnowledge(LogicFactory factory) {
		KnowledgeBase kb = new org.mandarax.reference.KnowledgeBase();
		// There are no illegal relations in PacMan, as far as I'm aware anyway

		// There is a little type hierarchy to infer that some things are
		// Locations
		Class[] subPacClasses = { Player.class, Ghost.class, Dot.class,
				PowerDot.class, JunctionPoint.class, Fruit.class };
		for (int i = 0; i < subPacClasses.length; i++) {
			List<Prerequisite> subPacPreqs = new LinkedList<Prerequisite>();
			Term[] variable = { factory.createVariableTerm("X",
					subPacClasses[i]) };
			subPacPreqs.add(factory.createPrerequisite(
					getTypePredicate(subPacClasses[i]), variable, false));
			Rule subPacRule = factory.createRule(subPacPreqs, factory
					.createFact(getTypePredicate(PacPoint.class), variable));
			kb.add(subPacRule);
		}

		return kb;
	}

	/**
	 * Method for creating predicates relating to direct value comparisons.
	 * 
	 * @param vals
	 *            The values to compare against.
	 * @param methodName
	 *            The name of the method/predicate.
	 * @return A DefinedPredicate of the above information.
	 * @throws NoSuchMethodException
	 *             If the method doesn't exist.
	 */
	private GuidedPredicate valuePredicate(Double[] vals, String methodName) throws NoSuchMethodException {
		// Defining the predicate values
		PredTerm[][] predValues = new PredTerm[2][];
		predValues[0] = createTied("State", State.class);
		predValues[1] = PredTerm.createValueArray(vals);

		// Defining the predicate
		Class[] predicateStructure = new Class[2];
		predicateStructure[0] = State.class;
		predicateStructure[1] = Double.class;
		return createDefinedPredicate(PacManStateSpec.class,
				predicateStructure, predValues, methodName);
	}

	/**
	 * Method for creating predicates relating to the distance between Pacman
	 * and a point on the map.
	 * 
	 * @param secondParamName
	 *            The name of the second free parameter.
	 * @param distanceVals
	 *            The constant distance values for comparison.
	 * @param secondClass
	 *            The class of the second free parameter.
	 * @param methodName
	 *            The name of the method/predicate.
	 * @return A DefinedPredicate of the above information.
	 * @throws NoSuchMethodException
	 *             If the method does not exist.
	 */
	private GuidedPredicate pacPointPredicate(String secondParamName,
			Integer[] distanceVals, Class secondClass, String methodName) throws NoSuchMethodException {
		// Defining the predicate values
		PredTerm[][] predValues = new PredTerm[3][];
		predValues[0] = createTied("State", State.class);
		predValues[1] = createTiedAndFree(secondParamName, secondClass);
		predValues[2] = PredTerm.createValueArray(distanceVals);

		// Defining the predicate
		Class[] predicateStructure = new Class[3];
		predicateStructure[0] = State.class;
		predicateStructure[1] = secondClass;
		predicateStructure[2] = Integer.class;
		return createDefinedPredicate(PacManStateSpec.class,
				predicateStructure, predValues, methodName);
	}

	// // // // // THE PREDICATE METHODS // // // // //

	/**
	 * Calculates whether a particular thing is within a distance from PacMan.
	 * 
	 * @param distanceGrid
	 *            The grid of distances for PacMan.
	 * @param thing
	 *            The thing to be checked.
	 * @param distance
	 *            The distance range the thing needs to be in.
	 * @return True if the thing is within the distance from PacMan.
	 */
	public boolean proximalThing(int[][] distanceGrid, Thing thing,
			Integer distance) {
		if (distanceGrid[thing.m_locX][thing.m_locY] <= distance)
			return true;
		return false;
	}

	/*
	 * An assortment of wrapper methods checking distance between Pacman and a
	 * Thing.
	 */
	public boolean proximalDot(State state, Dot dot, Integer distance) {
		PacManState pacState = (PacManState) state;
		return proximalThing(pacState.getDistanceGrid(), dot, distance);
	}

	public boolean proximalPowerDot(State state, PowerDot powerDot,
			Integer distance) {
		PacManState pacState = (PacManState) state;
		return proximalThing(pacState.getDistanceGrid(), powerDot,
				distance);
	}

	public boolean proximalGhost(State state, Ghost ghost, Integer distance) {
		PacManState pacState = (PacManState) state;
		if ((ghost.m_nTicks2Exit <= 0) && (!ghost.m_bEaten)) {
			if (ghost.m_nTicks2Flee <= 0)
				return proximalThing(pacState.getDistanceGrid(), ghost,
						distance);
		}
		return false;
	}

	public boolean proximalEdibleGhost(State state, Ghost ghost,
			Integer distance) {
		PacManState pacState = (PacManState) state;
		if ((ghost.m_nTicks2Exit <= 0) && (!ghost.m_bEaten)) {
			if (ghost.m_nTicks2Flee > 0)
				return proximalThing(pacState.getDistanceGrid(), ghost,
						distance);
		}
		return false;
	}

	public boolean proximalFruit(State state, Fruit fruit, Integer distance) {
		PacManState pacState = (PacManState) state;
		if ((fruit.m_nTicks2Show == 0) && (fruit.m_bAvailable)) {
			return proximalThing(pacState.getDistanceGrid(), fruit,
					distance);
		}
		return false;
	}

	public boolean safeJunction(State state, JunctionPoint junction,
			Integer safety) {
		// Make the check
		if (junction.getSafety() <= safety) {
			return true;
		}
		return false;
	}

	public boolean ghostCentre(State state, Double distance) {
		PacManState pacState = (PacManState) state;
		double sumX = 0;
		double sumY = 0;
		Ghost[] ghosts = pacState.getGhosts();
		Player pacman = pacState.getPlayer();
		for (Ghost ghost : ghosts) {
			sumX += ghost.m_locX;
			sumY += ghost.m_locY;
		}
		double centreX = sumX / ghosts.length;
		double centreY = sumY / ghosts.length;
		if (Point2D.distance(centreX, centreY, pacman.m_locX, pacman.m_locY) <= distance)
			return true;

		return false;
	}

	public boolean dotCentre(State state, Double distance) {
		PacManState pacState = (PacManState) state;
		double sumX = 0;
		double sumY = 0;
		Collection<Dot> dots = pacState.getDots();
		Player pacman = pacState.getPlayer();
		for (Dot dot : dots) {
			sumX += dot.m_locX;
			sumY += dot.m_locY;
		}
		double centreX = sumX / dots.size();
		double centreY = sumY / dots.size();
		if (Point2D.distance(centreX, centreY, pacman.m_locX, pacman.m_locY) <= distance)
			return true;

		return false;
	}

	public boolean ghostDensity(State state, Double density) {
		PacManState pacState = (PacManState) state;
		if (calculateDensity(pacState.getGhosts()) <= density)
			return true;
		return false;
	}

	/**
	 * Calculates the density of the ghosts.
	 * 
	 * @return The density of the ghosts, or 0 if only one ghost.
	 */
	private double calculateDensity(Ghost[] ghosts) {
		// If we can compare densities between ghosts
		double density = 0;
		for (int i = 0; i < ghosts.length - 1; i++) {
			Ghost thisGhost = ghosts[i];
			for (int j = i + 1; j < ghosts.length; j++) {
				Ghost thatGhost = ghosts[j];
				double distance = Ghost.DENSITY_RADIUS
						- Point2D.distance(thisGhost.m_locX, thisGhost.m_locY,
								thatGhost.m_locX, thatGhost.m_locY);
				if (distance > 0)
					density += distance;
			}
		}
		return density;
	}

	public boolean ghostDistanceSum(State state, Double distance) {
		PacManState pacState = (PacManState) state;
		Thing currentThing = pacState.getPlayer();
		Set<Thing> thingsToVisit = new HashSet<Thing>();
		for (Ghost ghost : pacState.getGhosts())
			thingsToVisit.add(ghost);
		double totalDistance = 0;
		// While there are things to go to.
		while (!thingsToVisit.isEmpty()) {
			// Find the closest thing to the current thing
			double closest = Integer.MAX_VALUE;
			Thing closestThing = null;
			for (Thing thing : thingsToVisit) {
				double distanceBetween = Point2D.distance(currentThing.m_locX,
						currentThing.m_locY, thing.m_locX, thing.m_locY);
				if (distanceBetween < closest) {
					closest = distanceBetween;
					closestThing = thing;
				}
			}

			// Add to the total distance, and repeat the loop, minus the
			// closest
			// thing
			totalDistance += closest;
			currentThing = closestThing;
			thingsToVisit.remove(currentThing);
		}

		// The comparison
		if (totalDistance <= distance)
			return true;

		return false;
	}

	public boolean ghostsFlashing(State state) {
		PacManState pacState = (PacManState) state;
		for (Ghost ghost : pacState.getGhosts()) {
			if (ghost.isFlashing())
				return true;
		}
		return false;
	}

	/**
	 * Moves towards a thing. Returns a positive byte <= 4.
	 * 
	 * @param state
	 *            The state of the environment.
	 * @param point
	 *            The point to move towards.
	 * @return A byte corresponding to a constant given in Thing
	 */
	public Byte moveTowards(State state, PacPoint point) {
		PacManState pacState = (PacManState) state;
		return (byte) followPath(point.m_locX, point.m_locY,
				pacState.getDistanceGrid()).ordinal();
	}

	/**
	 * Moves away from a thing. Returns a negative byte >= -4.
	 * 
	 * @param state
	 *            The state of the environment.
	 * @param point
	 *            The point to move away from.
	 * @return A negative of a byte corresponding to a constant given in Thing
	 */
	public Byte moveFrom(State state, PacPoint point) {
		PacManState pacState = (PacManState) state;
		return (byte) (-followPath(point.m_locX, point.m_locY,
				pacState.getDistanceGrid()).ordinal());
	}

	/**
	 * Follows a path from a particular location back to point 0: where Pacman
	 * currently is. This procedure thereby finds the quickest path to a point
	 * by giving the initial direction that Pacman needs to take to get to the
	 * goal.
	 * 
	 * @param x
	 *            The x location of the point.
	 * @param y
	 *            The y location of the point.
	 * @param distanceGrid
	 *            The distance grid for the player, where a value of 0 is the
	 *            player position.
	 * @return The initial direction to take to follow the path.
	 */
	private PacManLowAction followPath(int x, int y, int[][] distanceGrid) {
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

	/**
	 * Continues in the same direction, or a random direction at 90 degrees if
	 * impossible.
	 * 
	 * @param state
	 *            The state of the environment.
	 * @return A positive byte between 1 and 4 corresponding to a constant given
	 *         in Thing.
	 */
	public Byte keepDirection(State state) {
		PacManState pacState = (PacManState) state;
		Player player = pacState.getPlayer();
		int[][] distanceGrid = pacState.getDistanceGrid();

		int xOffset = 0;
		int yOffset = 0;
		byte notDirection = 0;
		switch (player.m_direction) {
		case Thing.UP:
			yOffset = -1;
			notDirection = Thing.DOWN;
			break;
		case Thing.DOWN:
			yOffset = 1;
			notDirection = Thing.UP;
			break;
		case Thing.LEFT:
			xOffset = -1;
			notDirection = Thing.RIGHT;
			break;
		case Thing.RIGHT:
			xOffset = 1;
			notDirection = Thing.LEFT;
			break;
		default:
			return player.m_direction;
		}

		// Test if the specified location is free
		int x = (player.m_locX + xOffset + distanceGrid.length)
				% distanceGrid.length;
		int y = (player.m_locY + yOffset + distanceGrid[0].length)
				% distanceGrid[0].length;
		if (distanceGrid[x][y] < Integer.MAX_VALUE) {
			return player.m_direction;
		} else {
			return (byte) (-notDirection);
		}
	}
}
