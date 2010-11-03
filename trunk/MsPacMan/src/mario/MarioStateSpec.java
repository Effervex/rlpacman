package mario;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.idsia.benchmark.mario.engine.sprites.BulletBill;
import ch.idsia.benchmark.mario.engine.sprites.CoinAnim;
import ch.idsia.benchmark.mario.engine.sprites.Enemy;
import ch.idsia.benchmark.mario.engine.sprites.FireFlower;
import ch.idsia.benchmark.mario.engine.sprites.Fireball;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Mushroom;
import ch.idsia.benchmark.mario.engine.sprites.Shell;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;

import relationalFramework.GuidedRule;
import relationalFramework.Policy;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;
import relationalFramework.agentObservations.BackgroundKnowledge;

/**
 * The state specifications for the PacMan domain.
 * 
 * @author Sam Sarjant
 */
public class MarioStateSpec extends StateSpec {
	@Override
	protected Map<String, String> initialiseActionPreconditions() {
		Map<String, String> preconds = new HashMap<String, String>();
		// Basic preconditions for actions
		preconds.put("moveTo", "(thing ?X) (distance ?X ?Y)");
		preconds.put("moveFrom", "(thing ?X) (distance ?X ?Y)");

		return preconds;
	}

	@Override
	protected int initialiseActionsPerStep() {
		return -1;
	}

	@Override
	protected Collection<StringFact> initialiseActionTemplates() {
		Collection<StringFact> actions = new ArrayList<StringFact>();

		// Actions have a type and a distance
		Class[] structure = new Class[2];
		structure[0] = Direction.class;
		structure[1] = Integer.class;
		actions.add(new StringFact("moveTo", structure));

		structure = new Class[2];
		structure[0] = Direction.class;
		structure[1] = Integer.class;
		actions.add(new StringFact("moveFrom", structure));

		structure = new Class[2];
		structure[0] = Object.class;
		structure[1] = Integer.class;
		actions.add(new StringFact("jump", structure));

		return actions;
	}

	@Override
	protected Map<String, BackgroundKnowledge> initialiseBackgroundKnowledge() {
		Map<String, BackgroundKnowledge> bckKnowledge = new HashMap<String, BackgroundKnowledge>();

		return bckKnowledge;
	}

	@Override
	protected String initialiseGoalState(List<String> constants) {
		// The goal is 0 units away.
		return "(dir flag ? 0)";
	}

	@Override
	protected Policy initialiseOptimalPolicy() {
		Policy goodPolicy = new Policy();

		// Defining a good policy (basic at the moment)
		ArrayList<String> rules = new ArrayList<String>();

		rules.add("(dir flag ?X ?Y) (goal flag) => (moveTo ?X ?Y)");

		for (String rule : rules)
			goodPolicy.addRule(new GuidedRule(rule), false, false);

		return goodPolicy;
	}

	@Override
	protected Collection<StringFact> initialisePredicateTemplates() {
		Collection<StringFact> predicates = new ArrayList<StringFact>();

		// Numerical values
		// Coins (score)
		Class[] structure = new Class[1];
		structure[0] = Integer.class;
		predicates.add(new StringFact("coins", structure));

		// Lives
		structure = new Class[1];
		structure[0] = Integer.class;
		predicates.add(new StringFact("lives", structure));

		// World
		structure = new Class[1];
		structure[0] = Integer.class;
		predicates.add(new StringFact("world", structure));

		// Time
		structure = new Class[1];
		structure[0] = Integer.class;
		predicates.add(new StringFact("time", structure));

		// Distance
		structure = new Class[2];
		structure[0] = Thing.class;
		structure[1] = Double.class;
		predicates.add(new StringFact("distance", structure));
		
		// Flying
		structure = new Class[1];
		structure[0] = Enemy.class;
		predicates.add(new StringFact("time", structure));

		return predicates;
	}

	@Override
	protected Collection<StringFact> initialiseTypePredicateTemplates() {
		Collection<StringFact> typeMap = new ArrayList<StringFact>();

		// Mario and misc items
		typeMap.add(new StringFact("thing", new Class[] { Thing.class }));
		typeMap.add(new StringFact("mario", new Class[] { Mario.class }));
		typeMap.add(new StringFact("coin", new Class[] { CoinAnim.class }));
		typeMap.add(new StringFact("mushroom", new Class[] { Mushroom.class }));
		typeMap.add(new StringFact("fireFlower", new Class[] { FireFlower.class }));
		typeMap.add(new StringFact("brick", new Class[] { Fruit.class }));
		typeMap.add(new StringFact("questionBrick",
				new Class[] { GhostCentre.class }));
		typeMap.add(new StringFact("shell", new Class[] { Shell.class }));
		typeMap.add(new StringFact("fireball", new Class[] { Fireball.class }));
		
		
		// Enemies
		typeMap.add(new StringFact("enemy", new Class[] { Enemy.class }));
		typeMap.add(new StringFact("goomba", new Class[] { Junction.class }));
		typeMap.add(new StringFact("koopa", new Class[] { Junction.class }));
		typeMap.add(new StringFact("redKoopa", new Class[] { Junction.class }));
		typeMap.add(new StringFact("greenKoopa", new Class[] { Junction.class }));
		typeMap.add(new StringFact("spiky", new Class[] { Junction.class }));
		typeMap.add(new StringFact("pirahnaPlant", new Class[] { Junction.class }));
		typeMap.add(new StringFact("bulletBill", new Class[] { BulletBill.class }));

		return typeMap;
	}
}
