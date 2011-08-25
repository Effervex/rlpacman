package blocksWorld;

import org.rlcommunity.rlglue.codec.*;

import cerrla.LearningController;
import cerrla.CERRLA;



public class RunBlocksWorldExperiment {
	public static void main(String[] args) {
		// Create the Agent
		AgentInterface theAgent = new CERRLA();

		// Create the Environment
		EnvironmentInterface theEnvironment = new BlocksWorldEnvironment();

		LocalGlue localGlueImplementation = new LocalGlue(theEnvironment,
				theAgent);
		RLGlue.setGlue(localGlueImplementation);
		
		LearningController.main(args);
	}
}
