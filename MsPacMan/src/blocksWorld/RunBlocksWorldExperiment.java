package blocksWorld;

import org.rlcommunity.rlglue.codec.*;

import relationalFramework.LearningController;
import relationalFramework.PolicyActor;


public class RunBlocksWorldExperiment {
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
