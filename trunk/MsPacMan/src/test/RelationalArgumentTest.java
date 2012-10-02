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
 *    src/test/RelationalArgumentTest.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.RangeBound;
import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.RangeContext;

public class RelationalArgumentTest {
	@Before
	public void setUp() {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld");
	}

	@Test
	public void testRelationalArgumentString() {
		// Type tests
		RelationalArgument ra = new RelationalArgument("?");
		assertTrue(ra.equals(RelationalArgument.ANONYMOUS));
		assertFalse(ra.isVariable());
		assertFalse(ra.isConstant());
		assertFalse(ra.isRange(false));
		assertFalse(ra.isNumber());

		ra = new RelationalArgument("?X");
		assertTrue(ra.isVariable());
		assertFalse(ra.isConstant());
		assertFalse(ra.isRange(false));
		assertFalse(ra.isNumber());

		ra = new RelationalArgument("X");
		assertFalse(ra.isVariable());
		assertTrue(ra.isConstant());
		assertFalse(ra.isRange(false));
		assertFalse(ra.isNumber());

		ra = new RelationalArgument("14.4");
		assertFalse(ra.isVariable());
		assertFalse(ra.isConstant());
		assertFalse(ra.isRange(false));
		assertTrue(ra.isNumber());

		ra = new RelationalArgument(RelationalArgument.GOAL_VARIABLE_PREFIX
				+ "0");
		assertFalse(ra.isVariable());
		assertTrue(ra.isConstant());
		assertFalse(ra.isRange(false));
		assertFalse(ra.isNumber());

		ra = new RelationalArgument(RelationalArgument.RANGE_VARIABLE_PREFIX
				+ "0&:(<= 0 " + RelationalArgument.RANGE_VARIABLE_PREFIX
				+ "0 10)");
		assertTrue(ra.isVariable());
		assertFalse(ra.isConstant());
		assertTrue(ra.isRange(false));
		assertEquals(ra.getRangeBounds()[0], new RangeBound(0));
		assertEquals(ra.getRangeBounds()[1], new RangeBound(10));
		assertEquals(ra.getRangeFrac()[0], 0, 0);
		assertEquals(ra.getRangeFrac()[1], 1, 0);
		assertTrue(ra.isNumber());
		
		ra = new RelationalArgument("?#_0&:(<= ?#_0min ?#_0 0.0)");
		assertTrue(ra.isRange(false));
		assertEquals(ra.getRangeBounds()[0], new RangeBound("?#_0min"));
		assertEquals(ra.getRangeBounds()[1], new RangeBound(0));
		assertEquals(ra.getRangeFrac()[0], 0, 0);
		assertEquals(ra.getRangeFrac()[1], 1, 0);
	}

	@Test
	public void testRelationalArgumentStringDoubleDoubleRangeContext() {
		RelationalArgument ra = new RelationalArgument("?#_1", -4, 56);
		assertEquals(-4, ra.getExplicitRange()[0], 0);
		assertEquals(56, ra.getExplicitRange()[1], 0);
		assertEquals(0, ra.getRangeFrac()[0], 0);
		assertEquals(1, ra.getRangeFrac()[1], 0);
		assertEquals(new RangeBound(-4), ra.getRangeBounds()[0]);
		assertEquals(new RangeBound(56), ra.getRangeBounds()[1]);
		assertNull(ra.getRangeContext());
	}

	@Test
	public void testRelationalArgumentStringRangeBoundDoubleRangeBoundDoubleRangeContext() {
		RangeBound lower = new RangeBound("?#_1" + RangeBound.MIN);
		RangeBound upper = new RangeBound(0);
		RelationalPredicate predicate = new RelationalPredicate(StateSpec
				.getInstance().getPredicateByName("height"), new String[] {
				"?G_0", "3" });
		RelationalPredicate action = new RelationalPredicate(StateSpec
				.getInstance().getPredicateByName("move"), new String[] {
				"?G_0", "?Y" });
		RangeContext context = new RangeContext(1, predicate, action);
		RelationalArgument ra = new RelationalArgument("?#_1", lower, 0.25,
				upper, 0.4, context);
		assertEquals(0.25, ra.getRangeFrac()[0], 0);
		assertEquals(0.4, ra.getRangeFrac()[1], 0);
		assertEquals(lower, ra.getRangeBounds()[0]);
		assertEquals(upper, ra.getRangeBounds()[1]);
		assertEquals(context, ra.getRangeContext());
		
		// Capped ranges
		ra = new RelationalArgument("?#_1", lower, -0.25, upper, 1.75, context);
		assertEquals(0, ra.getRangeFrac()[0], 0);
		assertEquals(1, ra.getRangeFrac()[1], 0);
	}
	
	@Test
	public void testEqualsCompareTo() {
		RelationalArgument ra1 = new RelationalArgument("?X");
		RelationalArgument ra2 = new RelationalArgument("?X");
		assertTrue(ra1.equals(ra2));
		assertTrue(ra1.hashCode() == ra2.hashCode());
		assertTrue(ra1.compareTo(ra2) == 0);
		assertTrue(ra2.compareTo(ra1) == 0);
		
		ra2 = new RelationalArgument("?Y");
		assertFalse(ra1.equals(ra2));
		assertFalse(ra1.compareTo(ra2) == 0);
		assertFalse(ra2.compareTo(ra1) == 0);
		
		// Ranges (Free)
		ra1 = new RelationalArgument("?#_1");
		ra2 = new RelationalArgument("?#_0");
		assertTrue(ra1.equals(ra2));
		assertTrue(ra1.hashCode() == ra2.hashCode());
		assertTrue(ra1.compareTo(ra2) == 0);
		assertTrue(ra2.compareTo(ra1) == 0);
		
		ra1 = new RelationalArgument("?#_1");
		ra2 = new RelationalArgument("?#_1&:(range ?#_1min 0.0 ?#_1 ?#_1max 0.5)");
		assertFalse(ra1.equals(ra2));
		assertFalse(ra1.compareTo(ra2) == 0);
		assertFalse(ra2.compareTo(ra1) == 0);
		
		// Equal free variables
		ra1 = new RelationalArgument("?X");
		ra2 = new RelationalArgument("?Y");
		assertFalse(ra1.equals(ra2));
		assertFalse(ra1.compareTo(ra2) == 0);
		assertFalse(ra2.compareTo(ra1) == 0);
		ra1.setFreeVariable(true);
		ra2.setFreeVariable(true);
		assertTrue(ra1.equals(ra2));
		assertTrue(ra1.hashCode() == ra2.hashCode());
		assertTrue(ra1.compareTo(ra2) == 0);
		assertTrue(ra2.compareTo(ra1) == 0);
		
		// Free-unfree inequality
		ra2 = new RelationalArgument("?X");
		assertFalse(ra1.equals(ra2));
		assertFalse(ra1.compareTo(ra2) == 0);
		assertFalse(ra2.compareTo(ra1) == 0);
	}
	
	@Test
	public void testVariableTermArg() {
		assertEquals(RelationalArgument.createVariableTermArg(0).getStringArg(), "?A");
		assertEquals(RelationalArgument.createVariableTermArg(1).getStringArg(), "?B");
	}
}
