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
		RelationalPolicy relPolA = new RelationalPolicy();
		RelationalRule rule = new RelationalRule(
				"(highest ?G_1) => (move ?G_0 ?G_0)");
		List<String> queryParams = new ArrayList<String>();
		queryParams.add("?G_0");
		queryParams.add("?G_1");
		rule.setQueryParams(queryParams);
		relPolA.addRule(rule);
		ModularPolicy modPolA = new ModularPolicy(relPolA, lced);

		// Highest module
		Map<String, String> paramReplacementMap = new HashMap<String, String>();
		paramReplacementMap.put("?G_0", "?G_1");
		GoalCondition highGoal = new GoalCondition("highest$A");
		lced = new LocalCrossEntropyDistribution(highGoal);
		RelationalPolicy relPolB = new RelationalPolicy();
		rule = new RelationalRule("(clear ?G_0) => (move ?G_0 ?G_0)");
		queryParams = new ArrayList<String>();
		queryParams.add("?G_0");
		rule.setQueryParams(queryParams);
		relPolB.addRule(rule);
		ModularPolicy highest = new ModularPolicy(relPolB, lced);
		highest.setModularParameters(paramReplacementMap);
		modPolA.replaceIndex(1, highest);

		// Clear module
		paramReplacementMap = new HashMap<String, String>();
		paramReplacementMap.put("?G_0", "?G_1");
		GoalCondition clearGoal = new GoalCondition("clear$A");
		lced = new LocalCrossEntropyDistribution(clearGoal);
		RelationalPolicy relPolC = new RelationalPolicy();
		rule = new RelationalRule("(above ?X ?G_0) => (move ?X ?G_0)");
		queryParams = new ArrayList<String>();
		queryParams.add("?G_0");
		rule.setQueryParams(queryParams);
		relPolC.addRule(rule);
		ModularPolicy clear = new ModularPolicy(relPolC, lced);
		clear.setModularParameters(paramReplacementMap);
		highest.replaceIndex(1, clear);

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

		List<PolicyItem> polRules = modPol.getRules();
		// Should be three things in there: the rule, and two modular holes.
		assertEquals(polRules.size(), 3);
		assertEquals(polRules.get(0), (rule));
		RelationalPredicate clearFact = new RelationalPredicate(StateSpec
				.getInstance().getPredicateByName("clear"),
				new String[] { "?G_0" });
		assertEquals(polRules.get(1), new ModularSubGoal(new GoalCondition(
				clearFact)));
		clearFact = new RelationalPredicate(StateSpec.getInstance()
				.getPredicateByName("clear"), new String[] { "?G_1" });
		assertEquals(polRules.get(2), new ModularSubGoal(new GoalCondition(
				clearFact)));
	}

	@Test
	public void testSerialisation() throws Exception {
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
		assertFalse(modPol.shouldRegenerate());
		modPol.noteStepReward(-1);
		modPol.endEpisode();
		assertFalse(modPol.shouldRegenerate());
		modPol.noteStepReward(-2);
		modPol.endEpisode();
		assertFalse(modPol.shouldRegenerate());
		
		assertTrue(modPol.getRules().contains(rule));
		// Also contains the modular holes.
		assertTrue(modPol.getRules().size() == 3);
		
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
		assertTrue(serPol.getRules().size() == 3);
		assertFalse(serPol.shouldRegenerate());
	}
}
