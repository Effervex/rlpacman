package hanoi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import relationalFramework.GuidedRule;
import relationalFramework.MultiMap;
import relationalFramework.Policy;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;
import relationalFramework.agentObservations.BackgroundKnowledge;

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
	protected Collection<StringFact> initialiseActionTemplates() {
		Collection<StringFact> actions = new ArrayList<StringFact>();

		// Move action
		Class[] structure = { Tile.class, Tower.class, Tile.class, Tower.class };
		actions.add(new StringFact("move", structure));

		return actions;
	}

	@Override
	protected Map<String, BackgroundKnowledge> initialiseBackgroundKnowledge() {
		Map<String, BackgroundKnowledge> bkMap = new HashMap<String, BackgroundKnowledge>();

		// TowerBase -> Tile
		bkMap.put("towerBaseImplication", new BackgroundKnowledge(
				"(towerBase ?X) => (assert (tile ?X))", true));

		// Block(Y) & !On(X,Y) -> Clear(Y)
		bkMap.put("clearTileRule", new BackgroundKnowledge(
				"(tile ?Y) (tower ?T) (on ?Y ? ?T) (not (on ? ?Y ?T)) "
						+ "=> (assert (clear ?Y ?T))", true));

		// On(X,Y) -> Above(X,Y)
		bkMap.put("aboveRule1", new BackgroundKnowledge(
				"(on ?X ?Y ?T) => (assert (above ?X ?Y ?T))", true));

		// On(X,Y) & Above(Y,Z) -> Above(X,Z)
		bkMap.put("aboveRule2", new BackgroundKnowledge(
				"(on ?X ?Y ?T) (above ?Y ?Z ?T) => (assert (above ?X ?Z ?T))",
				true));

		// Smaller(X,Y) rule A
		bkMap.put("smallerRule1", new BackgroundKnowledge(
				"(tile ?X) (not (towerBase ?X)) (tile ?Y&:(< ?X ?Y)) "
						+ "=> (assert (smaller ?X ?Y))", true));
		// Smaller(X,Y) rule B
		bkMap.put("smallerRule2", new BackgroundKnowledge(
				"(tile ?X) (not (towerBase ?X)) (towerBase ?Y) "
						+ "=> (assert (smaller ?X ?Y))", true));

		// Tile(Z) & On(X,Y) -> !On(X,Z)
		bkMap.put("onRule", new BackgroundKnowledge(
				"(tile ?Z) (on ?X ?Y) => (not (on ?X ?Z))", false));

		return bkMap;
	}

	@Override
	protected String initialiseGoalState(List<String> constants) {
		return "(tower t2) (forall (tile ?X) (or (towerBase ?X) (on ?X ? t2)))";
	}

	@Override
	protected Policy initialiseOptimalPolicy() {
		Policy optimal = null;

		// Defining the optimal policy (has to be split for even/odd)
		String[] rules = new String[3];
		rules[0] = "(numTiles even) (tile ?X) (notLastMoved ?X) "
				+ "(clear ?X ?Ta) (clear ?Y ?Tb) (smaller ?X ?Y) "
				+ "(nextTower ?Ta ?Tb) => (move ?X ?Ta ?Y ?Tb)";
		rules[1] = "(numTiles odd) (tile ?X) (notLastMoved ?X) "
				+ "(clear ?X ?Ta) (clear ?Y ?Tb) (smaller ?X ?Y) "
				+ "(prevTower ?Ta ?Tb) => (move ?X ?Ta ?Y ?Tb)";
		rules[2] = "(tile ?X) (notLastMoved ?X) "
				+ "(clear ?X ?Ta) (clear ?Y ?Tb) (smaller ?X ?Y) "
				+ "=> (move ?X ?Ta ?Y ?Tb)";

		optimal = new Policy();
		for (int i = 0; i < rules.length; i++)
			optimal.addRule(new GuidedRule(rules[i]), false, false);

		return optimal;
	}

	@Override
	protected Collection<StringFact> initialisePredicateTemplates() {
		Collection<StringFact> predicates = new ArrayList<StringFact>();

		// On predicate
		Class[] structure = new Class[3];
		structure[0] = Tile.class;
		structure[1] = Tile.class;
		structure[2] = Tower.class;
		predicates.add(new StringFact("on", structure));

		// Clear predicate
		structure = new Class[2];
		structure[0] = Tile.class;
		structure[1] = Tower.class;
		predicates.add(new StringFact("clear", structure));

		// Above predicate
		structure = new Class[2];
		structure[0] = Tile.class;
		structure[1] = Tile.class;
		structure[2] = Tower.class;
		predicates.add(new StringFact("above", structure));

		// Smaller predicate
		structure = new Class[2];
		structure[0] = Tile.class;
		structure[1] = Tile.class;
		predicates.add(new StringFact("smaller", structure));

		// NumTiles predicate
		structure = new Class[2];
		structure[0] = EvenOdd.class;
		predicates.add(new StringFact("numTiles", structure));

		// LastMoved predicate
		structure = new Class[2];
		structure[0] = Tile.class;
		predicates.add(new StringFact("lastMoved", structure));

		// LastMoved predicate
		structure = new Class[2];
		structure[0] = Tile.class;
		predicates.add(new StringFact("notLastMoved", structure));

		// NextTower predicate
		structure = new Class[2];
		structure[0] = Tower.class;
		structure[1] = Tower.class;
		predicates.add(new StringFact("nextTower", structure));

		// PrevTower predicate
		structure = new Class[2];
		structure[0] = Tower.class;
		structure[1] = Tower.class;
		predicates.add(new StringFact("prevTower", structure));

		return predicates;
	}

	@Override
	protected Collection<StringFact> initialiseTypePredicateTemplates() {
		Collection<StringFact> typePreds = new ArrayList<StringFact>();

		typePreds.add(new StringFact("tile", new Class[] { Tile.class }));
		typePreds.add(new StringFact("tower", new Class[] { Tower.class }));
		typePreds.add(new StringFact("towerBase",
				new Class[] { TowerBase.class }));

		return typePreds;
	}
}