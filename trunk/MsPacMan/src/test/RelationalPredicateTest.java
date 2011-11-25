package test;

import static org.junit.Assert.*;

import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;


public class RelationalPredicateTest {

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
	}

	@Test
	public void testReplaceArguments() {
		RelationalPredicate strFact = new RelationalPredicate(StateSpec.getInstance()
				.getPredicateByName("clear"), new String[] { "a" });
		assertArrayEquals(strFact.getArguments(), new String[] { "a" });
		Map<String, String> replacementMap = new HashMap<String, String>();
		replacementMap.put("a", "b");
		strFact.replaceArguments(replacementMap, true, false);
		assertArrayEquals(strFact.getArguments(), new String[] { "b" });

		// Swapsies
		strFact = new RelationalPredicate(StateSpec.getInstance().getPredicateByName("on"),
				new String[] { "a", "b" });
		assertArrayEquals(strFact.getArguments(), new String[] { "a", "b" });
		replacementMap = new HashMap<String, String>();
		replacementMap.put("a", "b");
		replacementMap.put("b", "a");
		strFact.replaceArguments(replacementMap, true, false);
		assertArrayEquals(strFact.getArguments(), new String[] { "b", "a" });
	}

	@Test
	public void testCompareTo() {
		// Equals
		RelationalPredicate strFact1 = new RelationalPredicate(StateSpec.getInstance()
				.getPredicateByName("clear"));
		RelationalPredicate strFact2 = new RelationalPredicate(StateSpec.getInstance()
				.getPredicateByName("clear"));
		assertEquals(strFact1.compareTo(strFact2), 0);
		assertEquals(strFact2.compareTo(strFact1), 0);

		// Inequals
		strFact1 = new RelationalPredicate(StateSpec.getInstance()
				.getPredicateByName("clear"));
		strFact2 = new RelationalPredicate(StateSpec.getInstance()
				.getPredicateByName("block"));
		assertEquals(strFact1.compareTo(strFact2), 1);
		assertEquals(strFact2.compareTo(strFact1), -1);

		// Inequal, same pred
		strFact1 = new RelationalPredicate(StateSpec.getInstance()
				.getPredicateByName("block"), new String[] { "b" });
		strFact2 = new RelationalPredicate(StateSpec.getInstance()
				.getPredicateByName("block"), new String[] { "a" });
		assertEquals(strFact1.compareTo(strFact2), 1);
		assertEquals(strFact2.compareTo(strFact1), -1);

		// Inequal, same sort of arguments
		strFact1 = new RelationalPredicate(StateSpec.getInstance().getPredicateByName("on"),
				new String[] { "a", "b" });
		strFact2 = new RelationalPredicate(StateSpec.getInstance().getPredicateByName("on"),
				new String[] { "b", "a" });
		assertEquals(strFact1.compareTo(strFact2), -1);
		assertEquals(strFact2.compareTo(strFact1), 1);
	}
}
