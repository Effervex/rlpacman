package hanoi;

import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import relationalFramework.agentObservations.BackgroundKnowledge;

public class HanoiStateSpec extends StateSpec {

	@Override
	protected Map<String, String> initialiseActionPreconditions() {
		Map<String, String> actionPreconditions = new HashMap<String, String>();

		// Put the pure precondition in, the rest is taken care of...
		actionPreconditions
				.put("move", "(clear ?X ?Y) (clear ?Z ?A&:(neq ?A ?Y)) "
						+ "(smaller ?X ?Y)");

		return actionPreconditions;
	}

	@Override
	protected int initialiseActionsPerStep() {
		return 1;
	}

	@Override
	protected Collection<RelationalPredicate> initialiseActionTemplates() {
		Collection<RelationalPredicate> actions = new ArrayList<RelationalPredicate>();

		// Move action
		String[] structure = { "tile", "tower", "tile", "tower" };
		actions.add(new RelationalPredicate("move", structure));

		return actions;
	}

	@Override
	protected Map<String, BackgroundKnowledge> initialiseBackgroundKnowledge() {
		Map<String, BackgroundKnowledge> bkMap = new HashMap<String, BackgroundKnowledge>();

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
				"(tile ?Z) (on ?X ?Y ?T) => (not (on ?X ?Z ?T))", false));

		return bkMap;
	}

	@Override
	protected String[] initialiseGoalState() {
		String[] result = { "hanoi",
				"(tower t2) (forall (tile ?X) (or (towerBase ?X) (on ?X ? t2)))" };
		return result;
	}

	@Override
	protected RelationalPolicy initialiseOptimalPolicy() {
		RelationalPolicy optimal = null;

		// Defining the optimal policy (has to be split for even/odd)
		String[] rules = new String[3];
		rules[0] = "(numTiles even) (tile ?X) (not (lastMoved ?X)) "
				+ "(clear ?X ?Ta) (clear ?Y ?Tb) (smaller ?X ?Y) "
				+ "(nextTower ?Ta ?Tb) => (move ?X ?Ta ?Y ?Tb)";
		rules[1] = "(numTiles odd) (tile ?X) (not (lastMoved ?X)) "
				+ "(clear ?X ?Ta) (clear ?Y ?Tb) (smaller ?X ?Y) "
				+ "(prevTower ?Ta ?Tb) => (move ?X ?Ta ?Y ?Tb)";
		rules[2] = "(tile ?X) (not (lastMoved ?X)) "
				+ "(clear ?X ?Ta) (clear ?Y ?Tb) (smaller ?X ?Y) "
				+ "=> (move ?X ?Ta ?Y ?Tb)";

		optimal = new RelationalPolicy();
		for (int i = 0; i < rules.length; i++)
			optimal.addRule(new RelationalRule(rules[i]), false, false);

		return optimal;
	}

	@Override
	protected Collection<RelationalPredicate> initialisePredicateTemplates() {
		Collection<RelationalPredicate> predicates = new ArrayList<RelationalPredicate>();

		// On predicate
		String[] structure = new String[3];
		structure[0] = "tile";
		structure[1] = "tile";
		structure[2] = "tower";
		predicates.add(new RelationalPredicate("on", structure));

		// Clear predicate
		structure = new String[2];
		structure[0] = "tile";
		structure[1] = "tower";
		predicates.add(new RelationalPredicate("clear", structure));

		// Above predicate
		structure = new String[3];
		structure[0] = "tile";
		structure[1] = "tile";
		structure[2] = "tower";
		predicates.add(new RelationalPredicate("above", structure));

		// Smaller predicate
		structure = new String[2];
		structure[0] = "tile";
		structure[1] = "tile";
		predicates.add(new RelationalPredicate("smaller", structure));

		// NumTiles predicate
		structure = new String[1];
		structure[0] = "evenOdd";
		predicates.add(new RelationalPredicate("numTiles", structure));

		// LastMoved predicate
		structure = new String[1];
		structure[0] = "tile";
		predicates.add(new RelationalPredicate("lastMoved", structure));

		// NextTower predicate
		structure = new String[2];
		structure[0] = "tower";
		structure[1] = "tower";
		predicates.add(new RelationalPredicate("nextTower", structure));

		// PrevTower predicate
		structure = new String[2];
		structure[0] = "tower";
		structure[1] = "tower";
		predicates.add(new RelationalPredicate("prevTower", structure));

		return predicates;
	}

	@Override
	protected Map<String, String> initialiseTypePredicateTemplates() {
		Map<String, String> typePreds = new HashMap<String, String>();

		typePreds.put("tile", null);
		typePreds.put("tower", null);
		typePreds.put("towerBase", "tile");
		typePreds.put("evenOdd", null);

		return typePreds;
	}
}
