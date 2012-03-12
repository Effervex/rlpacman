package test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

import cerrla.LocalCrossEntropyDistribution;
import cerrla.modular.GoalCondition;
import cerrla.modular.ModularPolicy;
import cerrla.modular.ModularSubGoal;
import cerrla.modular.PolicyItem;
import cerrla.modular.SpecificGoalCondition;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

public class ModularPolicyTest {

	@Before
	public void setUp() {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld", "onab");
	}

	@Test
	public void testEquals() {
		// Testing uniquely IDed policies
		LocalCrossEntropyDistribution lced = new LocalCrossEntropyDistribution(
				GoalCondition.parseGoalCondition("on$A$B"));
		ModularPolicy modPolA = new ModularPolicy(lced);
		modPolA.addRule(new RelationalRule("(clear ?X) => (move ?X ?X)"));

		ModularPolicy modPolB = new ModularPolicy(lced);
		modPolB.addRule(new RelationalRule("(clear ?X) => (move ?X ?X)"));
		assertEquals(modPolA.getRules(), modPolB.getRules());
		assertFalse(modPolA.equals(modPolB));
	}

	@Test
	public void testDeepModules() {
		// Testing deep modules
		LocalCrossEntropyDistribution lced = new LocalCrossEntropyDistribution(
				GoalCondition.parseGoalCondition("on$A$B"));
		RelationalPolicy relPolA = new RelationalPolicy();
		RelationalRule rule = new RelationalRule(
				"(highest ?G_1) => (move ?G_0 ?G_0)");
		List<RelationalArgument> queryParams = new ArrayList<RelationalArgument>();
		queryParams.add(new RelationalArgument("?G_0"));
		queryParams.add(new RelationalArgument("?G_1"));
		rule.setQueryParams(queryParams);
		relPolA.addRule(rule);
		ModularPolicy modPolA = new ModularPolicy(relPolA, lced);
		SpecificGoalCondition highGoal = new SpecificGoalCondition("highest$A");
		assertEquals(modPolA.getRules().get(1), new ModularSubGoal(highGoal,
				rule));

		// Highest module
		Map<RelationalArgument, RelationalArgument> paramReplacementMap = new HashMap<RelationalArgument, RelationalArgument>();
		paramReplacementMap.put(new RelationalArgument("?G_0"), new RelationalArgument("?G_1"));
		lced = new LocalCrossEntropyDistribution(highGoal);
		RelationalPolicy relPolB = new RelationalPolicy();
		rule = new RelationalRule("(clear ?G_0) => (move ?G_0 ?G_0)");
		queryParams = new ArrayList<RelationalArgument>();
		queryParams.add(new RelationalArgument("?G_0"));
		rule.setQueryParams(queryParams);
		relPolB.addRule(rule);
		ModularPolicy highest = new ModularPolicy(relPolB, lced);
		highest.setModularParameters(paramReplacementMap);
		SpecificGoalCondition clearGoal = new SpecificGoalCondition("clear$A");
		assertEquals(highest.getRules().get(1), new ModularSubGoal(clearGoal,
				rule));
		((ModularSubGoal) modPolA.getRules().get(1)).setModularPolicy(highest);

		// Clear module
		paramReplacementMap = new HashMap<RelationalArgument, RelationalArgument>();
		paramReplacementMap.put(new RelationalArgument("?G_0"), new RelationalArgument("?G_1"));
		lced = new LocalCrossEntropyDistribution(clearGoal);
		RelationalPolicy relPolC = new RelationalPolicy();
		rule = new RelationalRule("(above ?X ?G_0) => (move ?X ?G_0)");
		queryParams = new ArrayList<RelationalArgument>();
		queryParams.add(new RelationalArgument("?G_0"));
		rule.setQueryParams(queryParams);
		relPolC.addRule(rule);
		ModularPolicy clear = new ModularPolicy(relPolC, lced);
		clear.setModularParameters(paramReplacementMap);
		((ModularSubGoal) highest.getRules().get(1)).setModularPolicy(clear);

		System.out.println(modPolA.toNiceString());
	}

	@Test
	public void testDualConditions() {
		// Set up the relational policy
		RelationalPolicy relPol = new RelationalPolicy();
		RelationalRule rule = new RelationalRule(
				"(clear ?G_0) (clear ?G_1) => (move ?G_0 ?G_1)");
		List<RelationalArgument> queryParams = new ArrayList<RelationalArgument>();
		queryParams.add(new RelationalArgument("?G_0"));
		queryParams.add(new RelationalArgument("?G_1"));
		rule.setQueryParams(queryParams);
		relPol.addRule(rule);

		LocalCrossEntropyDistribution lced = new LocalCrossEntropyDistribution(
				GoalCondition.parseGoalCondition("on$A$B"));
		ModularPolicy modPol = new ModularPolicy(relPol, lced);

		List<PolicyItem> polRules = modPol.getRules();
		// Should be three things in there: the rule, and two modular holes.
		assertEquals(polRules.size(), 6);
		assertEquals(polRules.get(0), (rule));
		RelationalPredicate clearFact = new RelationalPredicate(StateSpec
				.getInstance().getPredicateByName("clear"),
				new String[] { "?G_0" });
		assertEquals(polRules.get(1), new ModularSubGoal(
				new SpecificGoalCondition(clearFact), rule));
		clearFact = new RelationalPredicate(StateSpec.getInstance()
				.getPredicateByName("clear"), new String[] { "?G_1" });
		assertEquals(polRules.get(2), new ModularSubGoal(
				new SpecificGoalCondition(clearFact), rule));
	}

	@Test
	public void testSerialisation() throws Exception {
		// Set up the relational policy
		RelationalPolicy relPol = new RelationalPolicy();
		RelationalRule rule = new RelationalRule(
				"(clear ?G_0) (clear ?G_1) => (move ?G_0 ?G_1)");
		List<RelationalArgument> queryParams = new ArrayList<RelationalArgument>();
		queryParams.add(new RelationalArgument("?G_0"));
		queryParams.add(new RelationalArgument("?G_1"));
		rule.setQueryParams(queryParams);
		relPol.addRule(rule);

		LocalCrossEntropyDistribution lced = new LocalCrossEntropyDistribution(
				GoalCondition.parseGoalCondition("on$A$B"));
		ModularPolicy modPol = new ModularPolicy(relPol, lced);
		assertFalse(modPol.shouldRegenerate());
		modPol.noteStepReward(new double[] { -1, -1 });
		modPol.endEpisode();
		assertFalse(modPol.shouldRegenerate());
		modPol.noteStepReward(new double[] { -2, -2 });
		modPol.endEpisode();
		assertFalse(modPol.shouldRegenerate());

		assertTrue(modPol.getRules().contains(rule));
		// Also contains the modular holes.
		assertEquals(modPol.getRules().size(), 6);

		File testFile = new File("testFile.ser");
		testFile.createNewFile();
		testFile.deleteOnExit();
		FileOutputStream fos = new FileOutputStream(testFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(modPol);
		oos.close();
		fos.close();

		assertTrue(testFile.exists());

		FileInputStream fis = new FileInputStream(testFile);
		ObjectInputStream ois = new ObjectInputStream(fis);
		ModularPolicy serPol = (ModularPolicy) ois.readObject();
		ois.close();
		fis.close();

		assertNotNull(serPol);
		assertTrue(serPol.getRules().contains(rule));
		// Also contains the modular holes.
		assertEquals(serPol.getRules().size(), 6);
		assertFalse(serPol.shouldRegenerate());
	}
}
