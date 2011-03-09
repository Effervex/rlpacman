package rlPacManGeneral;

import org.rlcommunity.rlglue.codec.*;

import relationalFramework.LearningController;
import relationalFramework.PolicyActor;


public class RunPacManGeneralExperiment {
	public static void main(String[] args) {
		// Create the Agent
		AgentInterface theAgent = new PolicyActor();

		// Create the Environment
		EnvironmentInterface theEnvironment = new PacManEnvironment();

		LocalGlue localGlueImplementation = new LocalGlue(theEnvironment,
				theAgent);
		RLGlue.setGlue(localGlueImplementation);

		// Run the main method of the Sample Experiment, using the arguments
		// were were passed
		// This will run the experiment in the main thread. The Agent and
		// Environment will run
		// locally, without sockets.
		LearningController.main(args);
	}
}
