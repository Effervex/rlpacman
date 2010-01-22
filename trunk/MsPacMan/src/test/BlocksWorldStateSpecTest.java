package test;

import static org.junit.Assert.*;

import java.util.*;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;
import org.mandarax.kernel.LogicFactory;
import org.mandarax.kernel.Rule;
import org.mandarax.reference.DefaultLogicFactory;

import blocksWorld.Block;

import relationalFramework.StateSpec;

public class BlocksWorldStateSpecTest {

	private StateSpec spec_;

	@Before
	public void setUp() throws Exception {
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
		spec_ = StateSpec.initInstance("blocksWorld.BlocksWorld",
				LogicFactory.getDefaultFactory());
	}

	@Test
	public void testInitialise() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitialiseTypePredicates() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitialisePredicates() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitialiseActions() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitialiseGoalState() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitialiseOptimalPolicy() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitialiseBackgroundKnowledge() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddConstant() {
		fail("Not yet implemented");
	}

	@Test
	public void testParseRule() {
		// Basic variable test
		Map<String, Object> constants = new HashMap<String, Object>();
		Rule rule = spec_.parseRule("clear(<X>) -> moveFloor(<X>)", constants);
		List body = rule.getBody();
		// Just use a string comparison for what it should look like.
		Set<String> stringBody = new HashSet<String>();
		for (Object obj : body)
			stringBody.add(obj.toString());
		// 3 assertions in the body, state, clear, and block
		assertEquals(3, stringBody.size());
		assertTrue(stringBody.contains("clear(<State>,<X>)"));
		assertTrue(stringBody.contains("state(<State>)"));
		assertTrue(stringBody.contains("block(<X>)"));
		stringBody.clear();
		constants.clear();

		// Test for a constant
		constants.put("a", new Block("a"));
		rule = spec_.parseRule("clear([a]) -> moveFloor([a])", constants);
		body = rule.getBody();
		for (Object obj : body)
			stringBody.add(obj.toString());
		// 3 assertions in the body, state, clear, and block, with a constant
		// variable
		assertEquals(3, stringBody.size());
		assertTrue(stringBody.contains("clear(<State>,[a])"));
		assertTrue(stringBody.contains("state(<State>)"));
		assertTrue(stringBody.contains("block([a])"));
		stringBody.clear();
		constants.clear();

		// Multiple conditions, one term
		rule = spec_.parseRule("clear(<X>) & highest(<X>) -> moveFloor(<X>)",
				constants);
		body = rule.getBody();
		for (Object obj : body)
			stringBody.add(obj.toString());
		// 4 assertions in the body, state, clear, highest, and a single block
		assertEquals(4, stringBody.size());
		assertTrue(stringBody.contains("clear(<State>,<X>)"));
		assertTrue(stringBody.contains("highest([StateSpec],<State>,<X>)"));
		assertTrue(stringBody.contains("state(<State>)"));
		assertTrue(stringBody.contains("block(<X>)"));
		stringBody.clear();

		// Multiple conditions, two terms
		rule = spec_.parseRule("clear(<X>) & highest(<Y>) -> moveFloor(<X>)",
				constants);
		body = rule.getBody();
		for (Object obj : body)
			stringBody.add(obj.toString());
		// 6 assertions in the body, state, clear, highest, two blocks, and an
		// inequals
		assertEquals(6, stringBody.size());
		assertTrue(stringBody.contains("clear(<State>,<X>)"));
		assertTrue(stringBody.contains("highest([StateSpec],<State>,<Y>)"));
		assertTrue(stringBody.contains("state(<State>)"));
		assertTrue(stringBody.contains("block(<X>)"));
		assertTrue(stringBody.contains("block(<Y>)"));
		assertTrue(stringBody.contains("inequal([StateSpec],<X>,<Y>)")
				|| stringBody.contains("inequal([StateSpec],<Y>,<X>)"));
		stringBody.clear();
		
		// Test anonymous variable
		rule = spec_.parseRule("clear(<X>) & on(<X>,_) -> moveFloor(<X>)", null);
		body = rule.getBody();
		for (Object obj : body)
			stringBody.add(obj.toString());
		// 6 assertions in the body: state, clear, on, two blocks, and an inequals
		assertEquals(6, stringBody.size());
		assertTrue(stringBody.contains("clear(<State>,<X>)"));
		assertTrue(stringBody.contains("on(<State>,<X>,<_0>)"));
		assertTrue(stringBody.contains("state(<State>)"));
		assertTrue(stringBody.contains("block(<X>)"));
		assertTrue(stringBody.contains("block(<_0>)"));
		assertTrue(stringBody.contains("inequal([StateSpec],<X>,<_0>)")
				|| stringBody.contains("inequal([StateSpec],<_0>,<X>)"));
		stringBody.clear();
		
		// Test anonymous variables
		rule = spec_.parseRule("clear(<X>) & on(<X>,_) & on(<Y>,_) -> moveFloor(<X>)", null);
		body = rule.getBody();
		for (Object obj : body)
			stringBody.add(obj.toString());
		// 13 assertions in the body: state, clear, 2 ons, 4 blocks, and 5 inequals
		// Note no inequals between _1 and _2
		assertEquals(13, stringBody.size());
		assertTrue(stringBody.contains("clear(<State>,<X>)"));
		assertTrue(stringBody.contains("on(<State>,<X>,<_0>)"));
		assertTrue(stringBody.contains("on(<State>,<Y>,<_1>)"));
		assertTrue(stringBody.contains("state(<State>)"));
		assertTrue(stringBody.contains("block(<X>)"));
		assertTrue(stringBody.contains("block(<Y>)"));
		assertTrue(stringBody.contains("block(<_0>)"));
		assertTrue(stringBody.contains("block(<_1>)"));
		assertTrue(stringBody.contains("inequal([StateSpec],<X>,<_0>)")
				|| stringBody.contains("inequal([StateSpec],<_0>,<X>)"));
		assertTrue(stringBody.contains("inequal([StateSpec],<X>,<_1>)")
				|| stringBody.contains("inequal([StateSpec],<_1>,<X>)"));
		assertTrue(stringBody.contains("inequal([StateSpec],<Y>,<_0>)")
				|| stringBody.contains("inequal([StateSpec],<_0>,<Y>)"));
		assertTrue(stringBody.contains("inequal([StateSpec],<Y>,<_1>)")
				|| stringBody.contains("inequal([StateSpec],<_1>,<Y>)"));
		assertTrue(stringBody.contains("inequal([StateSpec],<Y>,<X>)")
				|| stringBody.contains("inequal([StateSpec],<X>,<Y>)"));
		stringBody.clear();
	}

	@Test
	public void testGetPredicates() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetActions() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetGoalState() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetOptimalPolicy() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetBackgroundKnowledge() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetConstants() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetTypePredicate() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsTypePredicate() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetGuidedPredicate() {
		fail("Not yet implemented");
	}

	@Test
	public void testToString() {
		assertEquals("StateSpec", spec_.toString());
	}

	@Test
	public void testCreateDefinedPredicate() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreateTied() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreateTiedAndFree() {
		fail("Not yet implemented");
	}

	@Test
	public void testInsertState() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetTerminalFact() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetStateTerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSpecTerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetInequalityPredicate() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddContains() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetInstance() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitInstance() {
		fail("Not yet implemented");
	}

	@Test
	public void testEncodeRule() {
		fail("Not yet implemented");
	}

	@Test
	public void testLightenFact() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddKBFact() {
		fail("Not yet implemented");
	}

	@Test
	public void testInequal() {
		fail("Not yet implemented");
	}

}
