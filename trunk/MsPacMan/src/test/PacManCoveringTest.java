package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import relationalFramework.Covering;
import relationalFramework.GuidedRule;
import relationalFramework.StateSpec;

public class PacManCoveringTest {
	private Covering sut_;

	@Before
	public void setUp() throws Exception {
		StateSpec.initInstance("rlPacMan.PacMan");
		sut_ = new Covering(StateSpec.getInstance().getActions().size());
	}

	@Test
	public void testSpecialiseToPreGoal() {
		// Specialising to a numerical range
		List<String> pregoal = new ArrayList<String>();
		pregoal.add("(dot ?X)");
		pregoal.add("(pacman player)");
		pregoal
				.add("(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 1.0 17.0))");
		sut_.setPreGoal("(toDot ?X ?__Num0)", pregoal);

		GuidedRule rule = new GuidedRule(
				"(distanceDot player ?X ?Y) (dot ?X) (pacman player) => (toDot ?X ?Y)");
		rule.expandConditions();
		Collection<GuidedRule> results = sut_.specialiseToPreGoal(rule);
		assertEquals(Covering.NUM_DISCRETE_RANGES, results.size());
		double interval = 16 / Covering.NUM_DISCRETE_RANGES;
		for (int i = 0; i < Covering.NUM_DISCRETE_RANGES; i++)
			assertTrue(results
					.contains(new GuidedRule(
							"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 "
									+ (1.0 + interval * i)
									+ " "
									+ (1.0 + interval * (i + 1))
									+ ") (dot ?X) (pacman player) => (toDot ?X ?__Num0)",
							false, true, null)));

		// Further range specialisation
		pregoal.clear();
		pregoal.add("(dot ?X)");
		pregoal.add("(pacman player)");
		pregoal
				.add("(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 1.0 17.0))");
		sut_.setPreGoal("(toDot ?X ?__Num0)", pregoal);

		rule = new GuidedRule(
				"(distanceDot player ?X ?__Num0&:(betweenRange ?__Num0 1.0 5.0)) "
						+ "(dot ?X) (pacman player) => (toDot ?X ?__Num0)");
		rule.expandConditions();
		results = sut_.specialiseToPreGoal(rule);
		assertEquals(0, results.size());
	}

}
