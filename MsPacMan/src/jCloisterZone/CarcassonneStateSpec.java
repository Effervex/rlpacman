package jCloisterZone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.jcloisterzone.board.Rotation;

import relationalFramework.NumberEnum;
import relationalFramework.RelationalPolicy;
import relationalFramework.StateSpec;
import relationalFramework.RelationalPredicate;
import relationalFramework.agentObservations.BackgroundKnowledge;

/**
 * The state specifications for the PacMan domain.
 * 
 * @author Sam Sarjant
 */
public class CarcassonneStateSpec extends StateSpec {
	@Override
	protected Map<String, String> initialiseActionPreconditions() {
		Map<String, String> preconds = new HashMap<String, String>();
		// A tile may be placed if all edges fit nicely
		preconds.put("placeTile",
				"(currentPlayer ?X) (currentTile ?Y) (validLoc ?Y ?Z ?A) "
						+ "(location ?Z) (orientation ?A)");

		// A meeple can be played when it is meeple playing stage on the placed
		// tile
		preconds.put("placeMeeple", "(currentPlayer ?X) (meepleLoc ?Y ?Z)");

		return preconds;
	}

	@Override
	protected int initialiseActionsPerStep() {
		return 1;
	}

	@Override
	protected Collection<RelationalPredicate> initialiseActionTemplates() {
		Collection<RelationalPredicate> actions = new ArrayList<RelationalPredicate>();

		// Jump onto something
		String[] structure = new String[4];
		structure[0] = "player";
		structure[1] = "tile";
		structure[2] = "location";
		structure[3] = "orientation";
		actions.add(new RelationalPredicate("placeTile", structure));

		// Jump over something
		structure = new String[3];
		structure[0] = "player";
		structure[1] = "tile";
		structure[2] = "terrain";
		actions.add(new RelationalPredicate("placeMeeple", structure));

		return actions;
	}

	@Override
	protected Map<String, BackgroundKnowledge> initialiseBackgroundKnowledge() {
		Map<String, BackgroundKnowledge> bckKnowledge = new HashMap<String, BackgroundKnowledge>();

		// Edge axioms
		bckKnowledge.put("edgeAxiomGround", new BackgroundKnowledge(
				"(edge north) => (assert (cEdge north east) "
						+ "(ccEdge north west) (oppEdge north south))", false));
		bckKnowledge.put("edgeAxiom", new BackgroundKnowledge(
				"(cEdge ?N ?E) (ccEdge ?N ?W) (oppEdge ?N ?S) => (assert (cEdge ?E ?S) "
						+ "(ccEdge ?E ?N) (oppEdge ?E ?W))", false));

		// Next to rule
		// TODO This nextTo rule isn't working...
		bckKnowledge
				.put("nextToRule",
						new BackgroundKnowledge(
								"(locationXY ?L1 ?X ?Y) (not (tileLocation ?T1 ?L1)) "
										+ "(edgeDirection ?E ?Ex ?Ey) (locationXY ?L2 ?X2&:(= ?X2 (+ ?X ?Ex)) ?Y2&:(= ?Y2 (+ ?Y ?Ey))) "
										+ "(tileLocation ?T2 ?L2) (oppEdge ?E ?Eopp) "
										+ "(tileEdge ?T2 ?Eopp ?Ter) => (assert (nextTo ?L1 ?E ?Ter))",
								false));

		return bckKnowledge;
	}

	@Override
	protected String[] initialiseGoalState() {
		// The goal is 0 units away.
		String[] result = { "goal", "(tilesLeft -1)" };
		return result;
	}

	@Override
	protected RelationalPolicy initialiseHandCodedPolicy() {
		RelationalPolicy goodPolicy = new RelationalPolicy();

		return goodPolicy;
	}

	@Override
	protected Collection<RelationalPredicate> initialisePredicateTemplates() {
		Collection<RelationalPredicate> predicates = new ArrayList<RelationalPredicate>();

		// The current tile
		String[] structure = new String[1];
		structure[0] = "tile";
		predicates.add(new RelationalPredicate("currentTile", structure));

		structure = new String[1];
		structure[0] = "player";
		predicates.add(new RelationalPredicate("currentPlayer", structure));

		// Tile edge
		structure = new String[3];
		structure[0] = "tile";
		structure[1] = "edge";
		structure[2] = "terrain";
		predicates.add(new RelationalPredicate("tileEdge", structure));

		// Tile contains
		structure = new String[2];
		structure[0] = "tile";
		structure[1] = "terrain";
		predicates.add(new RelationalPredicate("tileContains", structure));

		// Tile location
		structure = new String[2];
		structure[0] = "tile";
		structure[1] = "location";
		predicates.add(new RelationalPredicate("tileLocation", structure));

		// What a particular location is next to. (The meat of the observations)
		structure = new String[3];
		structure[0] = "location";
		structure[1] = "edge";
		structure[2] = "terrain";
		predicates.add(new RelationalPredicate("nextTo", structure));

		// The number of surrounding tiles for a location
		structure = new String[2];
		structure[0] = "location";
		structure[1] = NumberEnum.Integer.toString();
		predicates
				.add(new RelationalPredicate("numSurroundingTiles", structure));

		// If the location is in a cloister's zone.
		structure = new String[2];
		structure[0] = "location";
		structure[1] = "cloister";
		predicates.add(new RelationalPredicate("cloisterZone", structure));

		// The x y coords of a location point
		structure = new String[3];
		structure[0] = "location";
		structure[1] = NumberEnum.Integer.toString();
		structure[2] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("locationXY", structure));

		// The numerical direction of the edge
		structure = new String[3];
		structure[0] = "edge";
		structure[1] = NumberEnum.Integer.toString();
		structure[2] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("edgeDirection", structure));

		// A valid location to place the current tile.
		structure = new String[3];
		structure[0] = "tile";
		structure[1] = "location";
		structure[2] = "orientation";
		predicates.add(new RelationalPredicate("validLoc", structure));

		// A valid location to place a meeple.
		structure = new String[2];
		structure[0] = "tile";
		structure[1] = "terrain";
		predicates.add(new RelationalPredicate("meepleLoc", structure));

		// The owner of a particular terrain feature
		structure = new String[2];
		structure[0] = "player";
		structure[1] = "terrain";
		predicates.add(new RelationalPredicate("controls", structure));

		// The claimants to a terrain (with count)
		structure = new String[3];
		structure[0] = "player";
		structure[1] = NumberEnum.Integer.toString();
		structure[2] = "terrain";
		predicates.add(new RelationalPredicate("placedMeeples", structure));

		// The number of open spaces of a terrain
		structure = new String[2];
		structure[0] = "terrain";
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("open", structure));

		// If a terrain is closed (cities and roads)
		structure = new String[1];
		structure[0] = "terrain";
		predicates.add(new RelationalPredicate("completed", structure));

		// Remaining meeples
		structure = new String[2];
		structure[0] = "player";
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("meeplesLeft", structure));

		// The current worth of a terrain feature
		structure = new String[2];
		structure[0] = "terrain";
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("worth", structure));

		// Each player's current score.
		structure = new String[2];
		structure[0] = "player";
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("score", structure));

		// The number of tiles left
		structure = new String[1];
		structure[0] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("tilesLeft", structure));

		// Edge axioms
		structure = new String[2];
		structure[0] = "edge";
		structure[1] = "edge";
		predicates.add(new RelationalPredicate("cEdge", structure));

		structure = new String[2];
		structure[0] = "edge";
		structure[1] = "edge";
		predicates.add(new RelationalPredicate("ccEdge", structure));

		structure = new String[2];
		structure[0] = "edge";
		structure[1] = "edge";
		predicates.add(new RelationalPredicate("oppEdge", structure));

		return predicates;
	}

	@Override
	protected Map<String, String> initialiseTypePredicateTemplates() {
		Map<String, String> types = new HashMap<String, String>();

		// Various types of terrain
		types.put("terrain", null);
		types.put("city", "terrain");
		types.put("road", "terrain");
		types.put("farm", "terrain");
		types.put("cloister", "terrain");

		// Abstract types
		types.put("location", null); // At least 4 locations, many at the end
		types.put("player", null); // Possibly multiple players
		types.put("edge", null); // Four possible edges (constants)
		types.put("orientation", null); // Four possible orientations
										// (constants)
		types.put("tile", null); // Each tile

		return types;
	}

	@Override
	protected Collection<String> initialiseConstantFacts() {
		Collection<String> constants = new ArrayList<String>();
		// The edges of tiles
		constants.add("(edge north)");
		constants.add("(edge east)");
		constants.add("(edge south)");
		constants.add("(edge west)");
		// Edge directions
		constants.add("(edgeDirection north 0 -1)");
		constants.add("(edgeDirection east 1 0)");
		constants.add("(edgeDirection south 0 1)");
		constants.add("(edgeDirection west -1 0)");

		// Orientations
		for (Rotation r : Rotation.values())
			constants.add("(orientation " + r.toString() + ")");
		return constants;
	}
}
