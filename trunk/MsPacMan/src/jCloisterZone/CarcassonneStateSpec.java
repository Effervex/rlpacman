package jCloisterZone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
				"(validLoc ?L ?O) (location ?L) (orientation ?O)");

		// A meeple can be played when it is meeple playing stage on the placed
		// tile
		preconds.put("placeMeeple",
				"(meepleLoc ?L ?X) (location ?L) (orientation ?O)");

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
		String[] structure = new String[2];
		structure[0] = "location";
		structure[1] = "orientation";
		actions.add(new RelationalPredicate("placeTile", structure));

		// Jump over something
		structure = new String[2];
		structure[0] = "location";
		structure[1] = "terrain";
		actions.add(new RelationalPredicate("placeMeeple", structure));

		return actions;
	}

	@Override
	protected Map<String, BackgroundKnowledge> initialiseBackgroundKnowledge() {
		Map<String, BackgroundKnowledge> bckKnowledge = new HashMap<String, BackgroundKnowledge>();

		// Edge axioms
		bckKnowledge.put("edgeAxiomGround", new BackgroundKnowledge(
				"(edge north) => (assert (cEdge north east) "
						+ "(ccEdge north west) (oppEdge north south))", true));
		bckKnowledge.put("edgeAxiomClockwise", new BackgroundKnowledge(
				"(cEdge ?N ?E) (ccEdge ?N ?W) (oppEdge ?N ?S) => (assert (cEdge ?E ?S) "
						+ "(ccEdge ?E ?N) (oppEdge ?E ?W))", true));

		return bckKnowledge;
	}

	@Override
	protected String[] initialiseGoalState() {
		// The goal is 0 units away.
		String[] result = { "goal", "(tilesLeft 0)" };
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
		// TODO Won't work due to multiple terrains per side (road+farms)
		String[] structure = new String[4];
		structure[0] = "terrain";
		structure[1] = "terrain";
		structure[2] = "terrain";
		structure[3] = "terrain";
		predicates.add(new RelationalPredicate("currentTile", structure));

		// What a particular location is next to. (The meat of the observations)
		structure = new String[3];
		structure[0] = "location";
		structure[1] = "edge";
		structure[2] = "terrain";
		predicates.add(new RelationalPredicate("nextTo", structure));

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
		predicates.add(new RelationalPredicate("meeples", structure));

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
		types.put("edge", null); // Four possible edges
		types.put("orientation", null); // Four possible orientations

		return types;
	}
}
