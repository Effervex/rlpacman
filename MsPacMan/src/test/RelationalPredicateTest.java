/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/test/RelationalPredicateTest.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package test;

import static org.junit.Assert.*;

import relationalFramework.RelationalArgument;
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
		RelationalPredicate strFact = new RelationalPredicate(StateSpec
				.getInstance().getPredicateByName("clear"),
				new String[] { "a" });
		assertArrayEquals(strFact.getArguments(), new String[] { "a" });
		Map<String, String> replacementMap = new HashMap<String, String>();
		replacementMap.put("a", "b");
		strFact.replaceArguments(replacementMap, true, false);
		assertArrayEquals(strFact.getArguments(), new String[] { "b" });

		// Swapsies
		strFact = new RelationalPredicate(StateSpec.getInstance()
				.getPredicateByName("on"), new String[] { "a", "b" });
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
		RelationalPredicate strFact1 = new RelationalPredicate(StateSpec
				.getInstance().getPredicateByName("clear"));
		RelationalPredicate strFact2 = new RelationalPredicate(StateSpec
				.getInstance().getPredicateByName("clear"));
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
		strFact1 = new RelationalPredicate(StateSpec.getInstance()
				.getPredicateByName("on"), new String[] { "a", "b" });
		strFact2 = new RelationalPredicate(StateSpec.getInstance()
				.getPredicateByName("on"), new String[] { "b", "a" });
		assertEquals(strFact1.compareTo(strFact2), -1);
		assertEquals(strFact2.compareTo(strFact1), 1);
	}

	@Test
	public void testEquals() {
		RelationalPredicate rp1 = StateSpec
				.toRelationalPredicate("(height ?Y ?#_1)");
		RelationalPredicate rp2 = StateSpec
				.toRelationalPredicate("(height ?Y ?#_1&:(range ?#_1min 0.0 ?#_1 ?#_1max 0.5))");
		assertFalse(rp1.equals(rp2));
		assertFalse(rp1.hashCode() == rp2.hashCode());
		assertFalse(rp1.compareTo(rp2) == 0);
	}

	@Test
	public void testLoadRelPred() {
		RelationalPredicate pred = StateSpec
				.toRelationalPredicate("(not (height ?X ?#_2))");
		RelationalArgument[] args = pred.getRelationalArguments();
		assertTrue(args[0].equals(new RelationalArgument("?X")));
		assertTrue(args[1].equals(new RelationalArgument("?#_2")));
		double[] range = args[1].getExplicitRange();
		assertNull(range);
	}
}
