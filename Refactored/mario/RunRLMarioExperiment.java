package mario;

import org.rlcommunity.rlglue.codec.*;

import cerrla.LearningController;
import cerrla.CERRLA;



public class RunRLMarioExperiment {
	public static void main(String[] args) {
		// Create the Agent
		AgentInterface theAgent = new CERRLA();

		// Create the Environment
		EnvironmentInterface theEnvironment = new RLMarioEnvironment();

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
