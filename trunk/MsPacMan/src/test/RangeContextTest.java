package test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.StateSpec;
import relationalFramework.agentObservations.RangeContext;

public class RangeContextTest {
	@Before
	public void setUp() {
		StateSpec.initInstance("blocksWorld.BlocksWorld");
	}

	@Test
	public void test() {
		RangeContext rc1 = new RangeContext(1,
				StateSpec.toRelationalPredicate("(height ?A 1)"),
				StateSpec.toRelationalPredicate("(move ?A ?B)"));
		RangeContext rc2 = new RangeContext(1,
				StateSpec.toRelationalPredicate("(height ?A 4)"),
				StateSpec.toRelationalPredicate("(move ?A ?B)"));
		assertTrue(rc1.equals(rc2));
		assertTrue(rc1.hashCode() == rc2.hashCode());
		
		rc2 = new RangeContext(1,
				StateSpec.toRelationalPredicate("(height ?B 4)"),
				StateSpec.toRelationalPredicate("(move ?A ?B)"));
		assertFalse(rc1.equals(rc2));
		assertFalse(rc1.hashCode() == rc2.hashCode());
		
		rc2 = new RangeContext(1,
				StateSpec.toRelationalPredicate("(height ?A 4)"),
				StateSpec.toRelationalPredicate("(move cat dog)"));
		assertTrue(rc1.equals(rc2));
		assertTrue(rc1.hashCode() == rc2.hashCode());
	}
}
