package rlPacMan;

import java.awt.Point;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mandarax.kernel.ConstantTerm;
import org.mandarax.kernel.Fact;
import org.mandarax.kernel.KnowledgeBase;
import org.mandarax.kernel.LogicFactory;
import org.mandarax.kernel.Predicate;
import org.mandarax.kernel.Prerequisite;
import org.mandarax.kernel.Rule;
import org.mandarax.kernel.SimplePredicate;
import org.mandarax.kernel.Term;
import org.mandarax.kernel.meta.JPredicate;

import relationalFramework.GuidedPredicate;
import relationalFramework.PredTerm;
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
				Object[].class };
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
			predicates.add(pacPointPredicate("Ghost", new Integer[] { 3, 6, 10,
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
			predicates.add(createDefinedPredicate(
					new Class[] { Object[].class },
					new PredTerm[][] { createTied("State", Object[].class) },
					"ghostsFlashing"));

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
			Class[] moveStructure = { Object[].class, PacPoint.class };
			PredTerm[][] predVals = new PredTerm[2][];
			predVals[0] = createTied("State", Object[].class);
			predVals[1] = createTied("Location", PacPoint.class);
			actions.add(createDefinedPredicate(moveStructure, predVals,
					"moveTowards"));

			// MOVE FROM
			actions.add(createDefinedPredicate(moveStructure, predVals,
					"moveFrom"));

			// KEEP DIRECTION
			moveStructure = new Class[1];
			moveStructure[0] = Object[].class;
			predVals = new PredTerm[1][];
			predVals[0] = createTied("State", Object[].class);
			actions.add(createDefinedPredicate(moveStructure, predVals,
					"keepDirection"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return actions;
	}

	@Override
	protected Rule initialiseGoalState(LogicFactory factory) {
		// Creating the prereqs
		List<Prerequisite> prereqs = new ArrayList<Prerequisite>();
		// TODO Fill in the goal.

		return factory.createRule(prereqs, StateSpec.getTerminalFact(factory));
	}

	@Override
	protected KnowledgeBase initialiseBackgroundKnowledge(LogicFactory factory) {
		KnowledgeBase kb = new org.mandarax.reference.KnowledgeBase();
		// There are no illegal relations in PacMan, as far as I'm aware anyway

		// There is a little type hierarchy to infer that some things are
		// Locations
		List<Prerequisite> subPacPreqs = new LinkedList<Prerequisite>();
		Class[] subPacClasses = { Player.class, Ghost.class, Dot.class,
				PowerDot.class, JunctionPoint.class, Fruit.class };
		for (int i = 0; i < subPacClasses.length; i++) {
			Term[] variable = { factory.createVariableTerm("X",
					subPacClasses[i]) };
			subPacPreqs.add(factory.createPrerequisite(
					getTypePredicate(subPacClasses[i]), variable, false));
			Rule subPacRule = factory.createRule(subPacPreqs, factory
					.createFact(getTypePredicate(PacPoint.class), variable));
			kb.add(subPacRule);
			subPacPreqs.clear();
		}

		return kb;
	}

	@Override
	protected ConstantTerm[] addGoalConstants(List<GuidedPredicate> predicates,
			org.mandarax.kernel.Rule goalState) {
		List<Fact> body = goalState.getBody();
		Map<Class, Set<ConstantTerm>> constantMap = new HashMap<Class, Set<ConstantTerm>>();
		// For every fact in the body of the rule, extract the constants
		for (Fact fact : body) {
			Term[] factTerms = fact.getTerms();
			for (Term term : factTerms) {
				// Add any constant terms found.
				if (term.isConstant()) {
					Set<ConstantTerm> constants = constantMap.get(term
							.getType());
					if (constants == null) {
						constants = new HashSet<ConstantTerm>();
						constantMap.put(term.getType(), constants);
					}
					constants.add((ConstantTerm) term);
				}
			}
		}

		// If there are no constants, exit
		if (constantMap.isEmpty())
			return new ConstantTerm[0];

		// Now with the constants, add those to the predicates that can accept
		// them
		// TODO Check the predicate JStructure isn't messing things up
		for (GuidedPredicate pred : predicates) {
			Class[] predStructure = pred.getPredicate().getStructure();
			// For each of the classes in the predicate
			for (int i = 0; i < predStructure.length; i++) {
				for (Class constClass : constantMap.keySet()) {
					// If the predicate is the same or superclass of the
					// constant
					if (predStructure[i].isAssignableFrom(constClass)) {
						// Check that this pred slot can take consts
						boolean addConsts = false;
						for (int j = 0; j < pred.getPredValues()[i].length; j++) {
							if (pred.getPredValues()[i][j].getTermType() != PredTerm.VALUE) {
								addConsts = true;
								break;
							}
						}

						// If there is a free or tied value, we can add
						// constants
						if (addConsts)
							pred.getPredValues()[i] = addConstants(pred
									.getPredValues()[i], constantMap
									.get(constClass));
					}
				}
			}
		}

		return null;
	}

	/**
	 * Adds the constants to the array of pred terms, if they're not already
	 * there.
	 * 
	 * @param predTerms
	 *            The predicate term array to add to.
	 * @param constants
	 *            The constants to be added.
	 * @return The expanded term array.
	 */
	private PredTerm[] addConstants(PredTerm[] predTerms,
			Set<ConstantTerm> constants) {
		// Move the existing terms into a set
		Set<PredTerm> newTerms = new HashSet<PredTerm>();
		for (PredTerm predTerm : predTerms) {
			newTerms.add(predTerm);
		}

		// Add the constants to the set
		for (ConstantTerm ct : constants) {
			newTerms.add(new PredTerm(ct.getObject()));
		}

		return newTerms.toArray(new PredTerm[newTerms.size()]);
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
	private GuidedPredicate valuePredicate(Double[] vals, String methodName)
			throws NoSuchMethodException {
		// Defining the predicate values
		PredTerm[][] predValues = new PredTerm[2][];
		predValues[0] = createTied("State", Object[].class);
		predValues[1] = PredTerm.createValueArray(vals);

		// Defining the predicate
		Class[] predicateStructure = new Class[2];
		predicateStructure[0] = Object[].class;
		predicateStructure[1] = Double.class;
		return createDefinedPredicate(predicateStructure, predValues,
				methodName);
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
			Integer[] distanceVals, Class secondClass, String methodName)
			throws NoSuchMethodException {
		// Defining the predicate values
		PredTerm[][] predValues = new PredTerm[3][];
		predValues[0] = createTied("State", Object[].class);
		predValues[1] = createTiedAndFree(secondParamName, secondClass);
		predValues[2] = PredTerm.createValueArray(distanceVals);

		// Defining the predicate
		Class[] predicateStructure = new Class[3];
		predicateStructure[0] = Object[].class;
		predicateStructure[1] = secondClass;
		predicateStructure[2] = Integer.class;
		return createDefinedPredicate(predicateStructure, predValues,
				methodName);
	}

	/**
	 * Convenience method for creating a defined predicate.
	 * 
	 * @param predicateStructure
	 *            The structure of the method/predicate.
	 * @param predValues
	 *            The possible values to be used within the predicate.
	 * @param methodName
	 *            The name of the method/predicate.
	 * @return A new defined predicate from the above parameters.
	 * @throws NoSuchMethodException
	 *             If the method doesn't exist.
	 */
	private GuidedPredicate createDefinedPredicate(Class[] predicateStructure,
			PredTerm[][] predValues, String methodName)
			throws NoSuchMethodException {
		Method method = PacManStateSpec.class.getMethod(methodName,
				predicateStructure);
		Predicate predicate = new JPredicate(method);
		return new GuidedPredicate(predicate, predValues);
	}

	/**
	 * Convenience method for creating a tied PredTerm.
	 * 
	 * @param termName
	 *            The term name.
	 * @return The array containing the tied term.
	 */
	private PredTerm[] createTied(String termName, Class termClass) {
		PredTerm[] terms = { new PredTerm(termName, termClass, PredTerm.TIED) };
		return terms;
	}

	/**
	 * Convenience method for creating a tied and free PredTerm.
	 * 
	 * @param termName
	 *            The term name.
	 * @return The array containing the tied and free terms.
	 */
	private PredTerm[] createTiedAndFree(String termName, Class termClass) {
		PredTerm[] terms = { new PredTerm(termName, termClass, PredTerm.TIED),
				new PredTerm(termName, termClass, PredTerm.FREE) };
		return terms;
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
	public boolean proximalDot(Object[] state, Dot dot, Integer distance) {
		return proximalThing(PacManState.getDistanceGrid(state), dot, distance);
	}

	public boolean proximalPowerDot(Object[] state, PowerDot powerDot,
			Integer distance) {
		return proximalThing(PacManState.getDistanceGrid(state), powerDot,
				distance);
	}

	public boolean proximalGhost(Object[] state, Ghost ghost, Integer distance) {
		if ((ghost.m_nTicks2Exit <= 0) && (!ghost.m_bEaten)) {
			if (ghost.m_nTicks2Flee <= 0)
				return proximalThing(PacManState.getDistanceGrid(state), ghost,
						distance);
		}
		return false;
	}

	public boolean proximalEdibleGhost(Object[] state, Ghost ghost,
			Integer distance) {
		if ((ghost.m_nTicks2Exit <= 0) && (!ghost.m_bEaten)) {
			if (ghost.m_nTicks2Flee > 0)
				return proximalThing(PacManState.getDistanceGrid(state), ghost,
						distance);
		}
		return false;
	}

	public boolean proximalFruit(Object[] state, Fruit fruit, Integer distance) {
		return proximalThing(PacManState.getDistanceGrid(state), fruit,
				distance);
	}

	public boolean safeJunction(Object[] state, JunctionPoint junction,
			Integer safety) {
		// Make the check
		if (junction.getSafety() <= safety) {
			return true;
		}
		return false;
	}

	public boolean ghostCentre(Object[] state, Double distance) {
		double sumX = 0;
		double sumY = 0;
		Ghost[] ghosts = PacManState.getGhosts(state);
		Player pacman = PacManState.getPlayer(state);
		for (Ghost ghost : ghosts) {
			sumX += ghost.m_locX;
			sumY += ghost.m_locY;
		}
		double centreX = sumX / ghosts.length;
		double centreY = sumY / ghosts.length;
		if (Point.distance(centreX, centreY, pacman.m_locX, pacman.m_locY) <= distance)
			return true;

		return false;
	}

	public boolean dotCentre(Object[] state, Double distance) {
		double sumX = 0;
		double sumY = 0;
		Collection<Dot> dots = PacManState.getDots(state);
		Player pacman = PacManState.getPlayer(state);
		for (Dot dot : dots) {
			sumX += dot.m_locX;
			sumY += dot.m_locY;
		}
		double centreX = sumX / dots.size();
		double centreY = sumY / dots.size();
		if (Point.distance(centreX, centreY, pacman.m_locX, pacman.m_locY) <= distance)
			return true;

		return false;
	}

	public boolean ghostDensity(Object[] state, Double density) {
		if (calculateDensity(PacManState.getGhosts(state)) <= density)
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
						- Point.distance(thisGhost.m_locX, thisGhost.m_locY,
								thatGhost.m_locX, thatGhost.m_locY);
				if (distance > 0)
					density += distance;
			}
		}
		return density;
	}

	public boolean ghostDistanceSum(Object[] state, Double distance) {
		Thing currentThing = PacManState.getPlayer(state);
		Set<Thing> thingsToVisit = new HashSet<Thing>();
		for (Ghost ghost : PacManState.getGhosts(state))
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

	public boolean ghostsFlashing(Object[] state) {
		for (Ghost ghost : PacManState.getGhosts(state)) {
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
	public Byte moveTowards(Object[] state, PacPoint point) {
		return (byte) followPath(point.m_locX, point.m_locY,
				PacManState.getDistanceGrid(state)).ordinal();
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
	public Byte moveFrom(Object[] state, PacPoint point) {
		return (byte) (-followPath(point.m_locX, point.m_locY,
				PacManState.getDistanceGrid(state)).ordinal());
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
	public Byte keepDirection(Object[] state) {
		Player player = PacManState.getPlayer(state);
		int[][] distanceGrid = PacManState.getDistanceGrid(state);

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
		if (distanceGrid[player.m_locX + xOffset][player.m_locY + yOffset] < Integer.MAX_VALUE) {
			return player.m_direction;
		} else {
			return (byte) (-notDirection);
		}
	}
}
