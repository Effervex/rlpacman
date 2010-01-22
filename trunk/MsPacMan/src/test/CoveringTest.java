package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;
import org.mandarax.kernel.LogicFactory;
import org.mandarax.kernel.Predicate;
import org.mandarax.kernel.SimplePredicate;

import relationalFramework.Covering;
import relationalFramework.State;
import relationalFramework.StateSpec;

public class CoveringTest {
	private Covering sut_;

	@Before
	public void setUp() throws Exception {
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
		sut_ = new Covering(LogicFactory.getDefaultFactory());
		StateSpec.initInstance("blocksWorld.BlocksWorld", LogicFactory
				.getDefaultFactory());
	}

	@Test
	public void testCoverState() {
		fail("Not yet implemented");
	}

	@Test
	public void testSpecialiseRule() {
		fail("Not yet implemented");
	}

	@Test
	public void testJoinRule() {
		// Simple action
		Collection<String> conditions = new ArrayList<String>();
		conditions.add("clear(<X>)");
		String actionString = "moveFloor(<X>)";
		assertEquals("clear(<X>) -> moveFloor(<X>)", sut_.joinRule(conditions,
				actionString));

		// Two conditions
		conditions.add("highest(<X>)");
		assertEquals("clear(<X>) & highest(<X>) -> moveFloor(<X>)", sut_
				.joinRule(conditions, actionString));

		// Larger action and conditions
		conditions.clear();
		conditions.add("on(<X>,_)");
		conditions.add("clear(<X>)");
		conditions.add("on(<Y>,_)");
		conditions.add("clear(<Y>)");
		actionString = "move(<X>,<Y>)";
		assertEquals(
				"on(<X>,_) & clear(<X>) & on(<Y>,_) & clear(<Y>) -> move(<X>,<Y>)",
				sut_.joinRule(conditions, actionString));
	}

	@Test
	public void testFormatAction() {
		// Simple action
		Class[] types = { Object.class };
		Predicate actionPred = new SimplePredicate("moveFloor", types);
		Collection<String> actionTerms = new HashSet<String>();
		assertEquals("moveFloor(<X>)", sut_.formatAction(actionPred, actionTerms));
		assertTrue(actionTerms.contains("<X>"));

		// Larger action
		Class[] moveTypes = { Object.class, Object.class };
		Predicate movePred = new SimplePredicate("move", moveTypes);
		actionTerms.clear();
		assertEquals("move(<X>,<Y>)", sut_.formatAction(movePred, actionTerms));
		assertTrue(actionTerms.contains("<X>"));
		assertTrue(actionTerms.contains("<Y>"));

		// Test State and State Spec actions
		Class[] stateTypes = { State.class, StateSpec.class, Object.class };
		Predicate statePred = new SimplePredicate("states", stateTypes);
		actionTerms.clear();
		assertEquals("states(<X>)", sut_.formatAction(statePred, actionTerms));
		assertTrue(actionTerms.contains("<X>"));

		// Test large action
		Class[] bigTypes = { Object.class, Object.class, Object.class,
				Object.class, Object.class, Object.class };
		Predicate bigAction = new SimplePredicate("big", bigTypes);
		actionTerms.clear();
		assertEquals("big(<X>,<Y>,<Z>,<A>,<B>,<C>)", sut_
				.formatAction(bigAction, actionTerms));
		assertTrue(actionTerms.contains("<X>"));
		assertTrue(actionTerms.contains("<Y>"));
		assertTrue(actionTerms.contains("<Z>"));
		assertTrue(actionTerms.contains("<A>"));
		assertTrue(actionTerms.contains("<B>"));
		assertTrue(actionTerms.contains("<C>"));
	}
	
	@Test
	public void testIsMinimal() {
		// Simple minimal case
		Collection<String> conditions = new ArrayList<String>();
		conditions.add("clear(<X>)");
		Collection<String> actionTerms = new HashSet<String>();
		actionTerms.add("<X>");
		assertTrue(sut_.isMinimal(conditions, actionTerms));
		
		// Simple not minimal
		conditions.add("highest(<X>)");
		assertFalse(sut_.isMinimal(conditions, actionTerms));
		
		// Minimal case
		conditions.clear();
		conditions.add("clear(<X>)");
		conditions.add("clear(<Y>)");
		actionTerms.add("<Y>");
		assertTrue(sut_.isMinimal(conditions, actionTerms));
		
		// Minimal extended case
		conditions.clear();
		conditions.add("clear(<X>)");
		conditions.add("on(<Y>,_)");
		assertTrue(sut_.isMinimal(conditions, actionTerms));
		
		// Minimal compacted case
		conditions.clear();
		conditions.add("on(<Y>,<X>)");
		assertTrue(sut_.isMinimal(conditions, actionTerms));
		
		// False case
		conditions.add("on(<X>,_)");
		assertFalse(sut_.isMinimal(conditions, actionTerms));
		
		// Complex case
		conditions.clear();
		conditions.add("clear(<X>)");
		conditions.add("above(<X>,<Y>)");
		conditions.add("on(<B>,<Z>)");
		conditions.add("on(<A>,_)");
		conditions.add("highest(<C>)");
		actionTerms.add("<Z>");
		actionTerms.add("<A>");
		actionTerms.add("<B>");
		actionTerms.add("<C>");
		assertTrue(sut_.isMinimal(conditions, actionTerms));
	}
}
