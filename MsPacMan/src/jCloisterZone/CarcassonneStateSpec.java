package jCloisterZone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import relationalFramework.BasicRelationalPolicy;
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
		// Movement entails getting to a point by moving in that direction at
		// speed and jumping if stuck until at the point (or jumping fails)
		preconds.put("move", "(canJumpOn ?X) (thing ?X) "
				+ "(distance ?X ?Y&~:(<= -16 ?Y 16))");
		// Search entails being under a searchable block and moving towards it
		// (possibly jumping).
		preconds.put(
				"search",
				"(brick ?X) (not (marioPower small)) (distance ?X ?D&:(<= -32 ?D 32)) "
						+ "(heightDiff ?X ?Y&:(<= 16 ?Y 80))");
		// Jump onto entails jumping onto a specific thing, moving towards it if
		// necessary.
		preconds.put("jumpOnto",
				"(canJumpOn ?X) (thing ?X) (distance ?X ?Y&:(<= -160 ?Y 160))");
		// Jump over entails jumping over a specific thing, moving towards it if
		// necessary.
		preconds.put("jumpOver", "(canJumpOver ?X) (thing ?X) "
				+ "(distance ?X ?Y&:(<= -160 ?Y 160)) (width ?X ?Z)");

		// Pickup a shell
		preconds.put("pickup",
				"(canJumpOn ?X) (passive ?X) (shell ?X) (distance ?X ?Y)");

		// Shoot a fireball at an enemy
		preconds.put("shootFireball", "(marioPower ?Z&:(= ?Z fire)) "
				+ "(distance ?X ?Y) "
				+ "(heightDiff ?X ?H&:(<= -16 ?H 16)) (enemy ?X)");

		// Shoot a held shell at an enemy
		preconds.put("shootShell", "(carrying ?Z) (shell ?Z) (distance ?X ?Y) "
				+ "(heightDiff ?X ?H&:(<= -16 ?H 16)) (enemy ?X)");

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
		BasicRelationalPolicy goodPolicy = new BasicRelationalPolicy();

		return goodPolicy;
	}

	@Override
	protected Collection<RelationalPredicate> initialisePredicateTemplates() {
		Collection<RelationalPredicate> predicates = new ArrayList<RelationalPredicate>();

		String[] structure = new String[4];
		structure[0] = "terrain";
		structure[1] = "terrain";
		structure[2] = "terrain";
		structure[3] = "terrain";
		predicates.add(new RelationalPredicate("currentTile", structure));
		
		structure = new String[2];
		structure[0] = "location";
		structure[1] = "terrain";
		predicates.add(new RelationalPredicate("nextTo", structure));
		
		structure = new String[3];
		structure[0] = "terrain";
		structure[1] = "player";
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("owner", structure));
		
		structure = new String[2];
		structure[0] = "terrain";
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("worth", structure));
		
		structure = new String[2];
		structure[0] = "player";
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("score", structure));
		
		structure = new String[1];
		structure[1] = NumberEnum.Integer.toString();
		predicates.add(new RelationalPredicate("tilesLeft", structure));

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
		types.put("location", null);
		types.put("player", null);
		types.put("orientation", null);

		return types;
	}
}
