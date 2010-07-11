package hanoi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import relationalFramework.GuidedRule;
import relationalFramework.MultiMap;
import relationalFramework.Policy;
import relationalFramework.StateSpec;

public class HanoiStateSpec extends StateSpec {

	@Override
	protected Map<String, String> initialiseActionPreconditions() {
		Map<String, String> actionPreconditions = new HashMap<String, String>();

		// Put the pure precondition in, the rest is taken care of...
		actionPreconditions.put("move",
				"(clear ?X ?Ta) (clear ?Y ?Tb&:(neq ?Tb ?Ta)) "
						+ "(smaller ?X ?Y)");

		return actionPreconditions;
	}

	@Override
	protected int initialiseActionsPerStep() {
		return 1;
	}

	@Override
	protected MultiMap<String, Class> initialiseActionTemplates() {
		MultiMap<String, Class> actions = new MultiMap<String, Class>();

		// Move action
		List<Class> structure = new ArrayList<Class>();
		structure.add(Tile.class);
		structure.add(Tile.class);
		structure.add(Tower.class);
		actions.putCollection("move", structure);

		return actions;
	}

	@Override
	protected Map<String, String> initialiseBackgroundKnowledge() {
		Map<String, String> bkMap = new HashMap<String, String>();

		// TowerBase -> Tile
		bkMap.put("towerBaseImplication", "(towerBase ?X) => (tile ?X)");

		// Block(Y) & !On(X,Y) -> Clear(Y)
		bkMap
				.put("clearRule",
						"(tile ?Y) (tower ?T) (not (on ?X ?Y ?T)) => (assert (clear ?Y ?T))");

		// On(X,Y) -> Above(X,Y)
		bkMap.put("aboveRule1", "(on ?X ?Y ?T) => (assert (above ?X ?Y ?T))");

		// On(X,Y) & Above(Y,Z) -> Above(X,Z)
		bkMap.put("aboveRule2",
				"(on ?X ?Y ?T) (above ?Y ?Z ?T) => (assert (above ?X ?Z ?T))");

		// Smaller(X,Y) rule
		bkMap.put("smallerRule",
				"(tile ?X) (tile ?Y&:(< ?X ?Y)) => (assert (smaller ?X ?Y))");

		return bkMap;
	}

	@Override
	protected String initialiseGoalState(List<String> constants) {
		return "(tower t2) (forall (tile ?X) (not (towerBase ?X)) (on ?X ? t2))";
	}

	@Override
	protected Policy initialiseOptimalPolicy() {
		Policy optimal = null;

		// TODO Defining the optimal policy
		String[] rules = null;

		optimal = new Policy();
		for (int i = 0; i < rules.length; i++)
			optimal.addRule(new GuidedRule(parseRule(rules[i])), false, false);

		return optimal;
	}

	@Override
	protected MultiMap<String, Class> initialisePredicateTemplates() {
		MultiMap<String, Class> predicates = new MultiMap<String, Class>();

		// On predicate
		List<Class> structure = new ArrayList<Class>();
		structure.add(Tile.class);
		structure.add(Tile.class);
		structure.add(Tower.class);
		predicates.putCollection("on", structure);

		// Clear predicate
		structure = new ArrayList<Class>();
		structure.add(Tile.class);
		structure.add(Tower.class);
		predicates.putCollection("clear", structure);

		// Above predicate
		structure = new ArrayList<Class>();
		structure.add(Tile.class);
		structure.add(Tile.class);
		structure.add(Tower.class);
		predicates.putCollection("above", structure);
		
		// Smaller predicate
		structure = new ArrayList<Class>();
		structure.add(Tile.class);
		structure.add(Tile.class);
		predicates.putCollection("smaller", structure);
		
		// NumTiles predicate
		structure = new ArrayList<Class>();
		structure.add(EvenOdd.class);
		predicates.putCollection("numTiles", structure);
		
		// LastMoved predicate
		structure = new ArrayList<Class>();
		structure.add(Tile.class);
		predicates.putCollection("lastMoved", structure);

		return predicates;
	}

	@Override
	protected Map<Class, String> initialiseTypePredicateTemplates() {
		Map<Class, String> typePreds = new HashMap<Class, String>();

		typePreds.put(Tile.class, "tile");
		typePreds.put(Tower.class, "tower");
		typePreds.put(TowerBase.class, "towerBase");

		return typePreds;
	}
}
