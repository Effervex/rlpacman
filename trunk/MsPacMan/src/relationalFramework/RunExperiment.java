package relationalFramework;

import org.rlcommunity.rlglue.codec.*;

import rlPacMan.PacManEnvironment;


public class RunExperiment {
	public static void main(String[] args) {
		// Create the Agent
		AgentInterface theAgent = new PolicyAgent();

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
		//PacManExperiment.main(args);
		CrossEntropyExperiment.main(args);
		System.out.println("RunMinesSarsaExperimentNoSockets Complete");
	}
}
