package test;

import static org.junit.Assert.*;

import java.text.Bidi;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.junit.Before;
import org.junit.Test;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.NonRedundantBackgroundKnowledge;

public class NonRedundantBackgroundKnowledgeTest {
	private NonRedundantBackgroundKnowledge sut_;

	@Before
	public void setUp() {
		StateSpec.initInstance("blocksWorldMove.BlocksWorld");
		sut_ = new NonRedundantBackgroundKnowledge();
	}

	@Test
	public void testToFromConstantForm() {
		RelationalPredicate pred = StateSpec
				.toRelationalPredicate("(above ?X ?Y)");
		BidiMap map = new DualHashBidiMap();
		String result = sut_.toConstantFormFull(pred, map);
		assertEquals(result, "(above id0 id1)");
		assertEquals(map.toString(), map.size(), 2);
		RelationalPredicate fromResult = sut_.fromConstantForm(result, map);
		assertEquals(fromResult, pred);

		// No reinit of map
		pred = StateSpec.toRelationalPredicate("(above ?X ?Z)");
		result = sut_.toConstantFormFull(pred, map);
		assertEquals(result, "(above id0 id2)");
		assertEquals(map.toString(), map.size(), 3);
		fromResult = sut_.fromConstantForm(result, map);
		assertEquals(fromResult, pred);

		// Constants and frees
		map.clear();
		pred = StateSpec.toRelationalPredicate("(above ? a)");
		result = sut_.toConstantFormFull(pred, map);
		assertEquals(result, "(above free id0)");
		assertEquals(map.toString(), map.size(), 1);
		fromResult = sut_.fromConstantForm(result, map);
		assertEquals(fromResult, pred);

		// Negation
		map.clear();
		pred = StateSpec.toRelationalPredicate("(not (above ? a))");
		result = sut_.toConstantFormFull(pred, map);
		assertEquals(result, "(neg_above free id0)");
		assertEquals(map.toString(), map.size(), 1);
		fromResult = sut_.fromConstantForm(result, map);
		assertEquals(fromResult, pred);

		// Numbers
		map.clear();
		pred = StateSpec.toRelationalPredicate("(height b 5)");
		result = sut_.toConstantFormFull(pred, map);
		assertEquals(result, "(height id0 5)");
		assertEquals(map.toString(), map.size(), 1);
		fromResult = sut_.fromConstantForm(result, map);
		assertEquals(fromResult, pred);

		// Ranges
		map.clear();
		pred = StateSpec
				.toRelationalPredicate("(height b ?#_0&:(range ?#_0min 0.5 ?#_0 ?#_0max 1.0))");
		result = sut_.toConstantFormFull(pred, map);
		assertEquals(result, "(height id0 id1)");
		assertEquals(map.toString(), map.size(), 2);
		fromResult = sut_.fromConstantForm(result, map);
		assertEquals(fromResult, pred);

		// Free ranges
		map.clear();
		pred = StateSpec.toRelationalPredicate("(height b ?#_0)");
		pred.getActualArguments()[1].setFreeVariable(true);
		result = sut_.toConstantFormFull(pred, map);
		assertEquals(result, "(height id0 free)");
		assertEquals(map.toString(), map.size(), 1);
		fromResult = sut_.fromConstantForm(result, map);
		assertEquals(fromResult, pred);

		// Dual free ranges
		pred = StateSpec.toRelationalPredicate("(height b ?#_1)");
		pred.getActualArguments()[1].setFreeVariable(true);
		result = sut_.toConstantFormFull(pred, map);
		assertEquals(result, "(height id0 free)");
		assertEquals(map.toString(), map.size(), 1);
		fromResult = sut_.fromConstantForm(result, map);
		assertEquals(fromResult, pred);
	}

	@Test
	public void testToConstantVariable() {
		RelationalPredicate pred = StateSpec
				.toRelationalPredicate("(above ?X ?Y)");
		Collection<RelationalArgument> variables = new HashSet<RelationalArgument>();
		String result = sut_.toConstantFormVariable(pred, variables);
		assertEquals(result, "(above ?X&:(<> ?X free) ?Y&:(<> ?Y free ?X))");
		assertEquals(variables.toString(), variables.size(), 2);
		assertTrue(variables.contains(new RelationalArgument("?X")));
		assertTrue(variables.contains(new RelationalArgument("?Y")));

		// Same variables, so no need to test them
		pred = StateSpec.toRelationalPredicate("(on ?X ?)");
		result = sut_.toConstantFormVariable(pred, variables);
		assertEquals(result, "(on ?X free)");
		assertEquals(variables.toString(), variables.size(), 2);
		assertTrue(variables.contains(new RelationalArgument("?X")));
		assertTrue(variables.contains(new RelationalArgument("?Y")));
	}
}
