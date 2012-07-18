package test;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.LocalAgentObservations;
import cerrla.LocalCrossEntropyDistribution;
import cerrla.Slot;
import cerrla.modular.GoalCondition;

public class SlotTest {
	private LocalCrossEntropyDistribution lced;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld", "onab");
		GoalCondition gc = GoalCondition.parseGoalCondition("on$A$B");
		LocalAgentObservations lao = LocalAgentObservations
				.loadAgentObservations(gc, null);
		assertNotNull("No onAB agent observations. Cannot run test.", lao);
		lced = new LocalCrossEntropyDistribution(gc);
	}

	@Test
	public void testSlotSplitToString() {
		// Basic case
		RelationalRule seedRule = new RelationalRule(
				"(clear ?X) (clear ?Y) (block ?X) (height ?X ?#_0) (height ?Y ?#_1) "
						+ "=> (move ?X ?Y)");
		Slot s = new Slot(seedRule, false, 1, lced.getPolicyGenerator());
		Collection<RelationalPredicate> slotSplitFacts = s.getSlotSplitFacts();
		assertTrue(slotSplitFacts.isEmpty());

		seedRule = new RelationalRule(
				"(above ?X ?G_1) (clear ?X) (clear ?Y) (block ?X) "
						+ "(height ?X ?#_0) (height ?Y ?#_1&:(range ?#_1min 0.0 ?#_1 ?#_1max 0.5)) "
						+ "=> (move ?X ?Y)");
		s = new Slot(seedRule, false, 1, lced.getPolicyGenerator());
		slotSplitFacts = s.getSlotSplitFacts();
		assertFalse(slotSplitFacts.isEmpty());
		assertTrue(slotSplitFacts.contains(StateSpec
				.toRelationalPredicate("(above ?X ?G_1)")));
		assertTrue(slotSplitFacts
				.contains(StateSpec
						.toRelationalPredicate("(height ?Y ?#_1&:(range ?#_1min 0.0 ?#_1 ?#_1max 0.5))")));
		assertEquals(slotSplitFacts.size(), 2);
	}
}
