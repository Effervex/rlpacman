package blocksWorldActive;

import org.rlcommunity.rlglue.codec.*;

import cerrla.LearningController;
import cerrla.PolicyActor;

public class RunBlocksWorldActiveExperiment {
	public static void main(String[] args) {
		// Create the Agent
		AgentInterface theAgent = new PolicyActor();

		// Create the Environment
		EnvironmentInterface theEnvironment = new BlocksWorldEnvironment();

		LocalGlue localGlueImplementation = new LocalGlue(theEnvironment,
				theAgent);
		RLGlue.setGlue(localGlueImplementation);
		
		LearningController.main(args);
	}
}
