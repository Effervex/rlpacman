package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

import cerrla.LocalCrossEntropyDistribution;
import cerrla.ModularHole;

import relationalFramework.GoalCondition;
import relationalFramework.ModularPolicy;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.RelationallyEvaluatableObject;
import relationalFramework.StateSpec;

public class ModularPolicyTest {

	@Before
	public void setUp() {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld", "onab");
	}

	@Test
	public void testEquals() {
		LocalCrossEntropyDistribution lced = new LocalCrossEntropyDistribution(
				new GoalCondition("on$A$B"));
		ModularPolicy modPolA = new ModularPolicy(lced);
		modPolA.addRule(new RelationalRule("(clear ?A) => (move ?A ?A)"));

		ModularPolicy modPolB = new ModularPolicy(lced);
		modPolB.addRule(new RelationalRule("(clear ?A) => (move ?A ?A)"));
		assertEquals(modPolA, modPolB);

		Map<String, String> modularParams = new HashMap<String, String>();
		modularParams.put("?G_0", "?G_1");
		modPolB.setModularParameters(modularParams);
		assertFalse(modPolA.equals(modPolB));
	}

	@Test
	public void testDeepModules() {
		// Testing deep modules
		LocalCrossEntropyDistribution lced = new LocalCrossEntropyDistribution(
				new GoalCondition("on$A$B"));
		ModularPolicy modPolA = new ModularPolicy(lced);
		RelationalRule rule = new RelationalRule(
				"(highest ?G_1) => (move ?G_0 ?G_0)");
		List<String> queryParams = new ArrayList<String>();
		queryParams.add("?G_0");
		queryParams.add("?G_1");
		rule.setQueryParams(queryParams);
		modPolA.addRule(rule);

		// Highest module
		Map<String, String> paramReplacementMap = new HashMap<String, String>();
		paramReplacementMap.put("?G_0", "?G_1");
		GoalCondition highGoal = new GoalCondition("highest$A");
		lced = new LocalCrossEntropyDistribution(highGoal);
		ModularPolicy highest = new ModularPolicy(lced);
		rule = new RelationalRule("(clear ?G_0) => (move ?G_0 ?G_0)");
		queryParams = new ArrayList<String>();
		queryParams.add("?G_0");
		rule.setQueryParams(queryParams);
		highest.addRule(rule);
		highest.setModularParameters(paramReplacementMap);
		modPolA.addPolicy(highGoal.getFacts(), highest);

		// Clear module
		paramReplacementMap = new HashMap<String, String>();
		paramReplacementMap.put("?G_0", "?G_1");
		GoalCondition clearGoal = new GoalCondition("clear$A");
		lced = new LocalCrossEntropyDistribution(clearGoal);
		ModularPolicy clear = new ModularPolicy(lced);
		rule = new RelationalRule("(above ?X ?G_0) => (move ?X ?G_0)");
		queryParams = new ArrayList<String>();
		queryParams.add("?G_0");
		rule.setQueryParams(queryParams);
		clear.addRule(rule);
		clear.setModularParameters(paramReplacementMap);
		highest.addPolicy(clearGoal.getFacts(), clear);

		System.out.println(modPolA.toNiceString());
	}

	@Test
	public void testDualConditions() {
		// Set up the relational policy
		RelationalPolicy relPol = new RelationalPolicy();
		RelationalRule rule = new RelationalRule(
				"(clear ?G_0) (clear ?G_1) => (move ?G_0 ?G_1)");
		List<String> queryParams = new ArrayList<String>();
		queryParams.add("?G_0");
		queryParams.add("?G_1");
		rule.setQueryParams(queryParams);
		relPol.addRule(rule);

		LocalCrossEntropyDistribution lced = new LocalCrossEntropyDistribution(
				new GoalCondition("on$A$B"));
		ModularPolicy modPol = new ModularPolicy(relPol, lced);

		List<RelationallyEvaluatableObject> polRules = modPol.getRules();
		// Should be three things in there: the rule, and two modular holes.
		assertEquals(polRules.size(), 3);
		assertEquals(polRules.get(0), (rule));
		RelationalPredicate clearFact = new RelationalPredicate(StateSpec
				.getInstance().getPredicateByName("clear"),
				new String[] { "?G_0" });
		assertEquals(polRules.get(1), new ModularHole(new GoalCondition(
				clearFact)));
		clearFact = new RelationalPredicate(StateSpec.getInstance()
				.getPredicateByName("clear"), new String[] { "?G_1" });
		assertEquals(polRules.get(2), new ModularHole(new GoalCondition(
				clearFact)));
	}
}
