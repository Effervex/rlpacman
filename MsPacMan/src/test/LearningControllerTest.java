package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import relationalFramework.GuidedRule;
import relationalFramework.LearningController;
import relationalFramework.Policy;
import relationalFramework.PolicyGenerator;
import relationalFramework.PolicyValue;
import relationalFramework.ProbabilityDistribution;
import relationalFramework.Slot;
import relationalFramework.StateSpec;

public class LearningControllerTest {
	private LearningController sut_;

	@Before
	public void setUp() throws Exception {
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
		StateSpec.initInstance("rlPacMan.PacMan");
		sut_ = new LearningController(new String[] { "pacManArguments.txt" });
	}

	@Test
	public void testUpdateDistributions() {
		// Parse this slot and test removal.
		PolicyGenerator localPolicy = PolicyGenerator.newInstance(0);
		Slot ruleSlot = Slot
				.parseSlotString("(Slot (fromPowerDot) {((distancePowerDot player ?X ?__Num1&:(betweenRange ?__Num1 37.75 50.0)) (powerDot ?X) (test (<> ?X player)) => (fromPowerDot ?X ?__Num1):0.4013070052778781),"
						+ " ((distancePowerDot player ?X ?__Num1&:(betweenRange ?__Num1 25.5 37.75)) (powerDot ?X) (test (<> ?X player)) => (fromPowerDot ?X ?__Num1):0.3436855034648942),"
						+ " ((distancePowerDot player ?X ?__Num1&:(betweenRange ?__Num1 37.5 50.0)) (powerDot ?X) (test (<> ?X player)) => (fromPowerDot ?X ?__Num1):0.16679938374483333),"
						+ " ((distancePowerDot player ?X ?__Num1&:(betweenRange ?__Num1 12.5 25.0)) (powerDot ?X) (test (<> ?X player)) => (fromPowerDot ?X ?__Num1):0.08820810751239643)},0.7513931524666813,0.5:0.22540766013609628)");
		localPolicy.getGenerator().add(ruleSlot);

		List<PolicyValue> elites = new ArrayList<PolicyValue>();
		Policy pol = new Policy();
		pol.addRule(ruleSlot.getGenerator().sample(true), false, false);
		elites.add(new PolicyValue(pol, 400, 3));
		pol = new Policy();
		pol.addRule(ruleSlot.getGenerator().sample(false), false, false);
		elites.add(new PolicyValue(pol, 300, 3));
		pol = new Policy();
		pol.addRule(ruleSlot.getGenerator().sample(true), false, false);
		elites.add(new PolicyValue(pol, 500, 3));
		
		ArrayList<Float> episodePerformances = new ArrayList<Float>();

		int sinceLastTest = sut_.updateDistributions(localPolicy, elites, 10,
				1, 3, 0, 100, 0, episodePerformances);
	}
}
