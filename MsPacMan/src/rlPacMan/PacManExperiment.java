package rlPacMan;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import org.rlcommunity.rlglue.codec.RLGlue;

public class PacManExperiment {
	private static final int NUM_EPISODES = 25;
	private static final int NUM_REWARD_PRINTS = 20;

	private void saveResultsToCSVFile(Vector<EvaluationPoint> results, String fileName) {
        try {
            FileWriter FW = new FileWriter(new File(fileName));
            FW.write("#Results from SampleExperiment.java.  First line is means, second line is standard deviations.\n");
            for (EvaluationPoint point : results) {
                FW.write("" + point.mean + ",");
            }
            FW.write("\n");
            for (EvaluationPoint point : results) {
                FW.write("" + point.standardDeviation + ",");
            }
            FW.write("\n");
            FW.close();
        } catch (IOException ex) {
            System.out.println("Problem writing results out to file: " + fileName + " :: " + ex);
        }
    }

    class EvaluationPoint {

        public double mean;
        public double standardDeviation;

        public EvaluationPoint(double mean, double standardDeviation) {
            this.mean = mean;
            this.standardDeviation = standardDeviation;
        }
    }

    /**
     * Tell the agent to stop learning, then execute n episodes with his current
     * policy.  Estimate the mean and variance of the return over these episodes.
     * @return
     */
    EvaluationPoint evaluateAgent() {
        int i = 0;
        double sum = 0;
        double sum_of_squares = 0;
        double this_return = 0;
        double mean;
        double variance;
        int n = 10;

        RLGlue.RL_agent_message("freeze learning");
        for (i = 0; i < n; i++) {
            /* We use a cutoff here in case the policy is bad
            and will never end an episode */
            RLGlue.RL_episode(0);
            this_return = RLGlue.RL_return();
            sum += this_return;
            sum_of_squares += this_return * this_return;
        }
        RLGlue.RL_agent_message("unfreeze learning");

        mean = sum / n;
        variance = (sum_of_squares - n * mean * mean) / ((double)n - 1.0f);
        return new EvaluationPoint(mean, Math.sqrt(variance));
    }
    /*
        This function will freeze the agent's policy and test it after every 25 episodes.
     */
    void printScore(int afterEpisodes, EvaluationPoint theScore) {
        System.out.printf("%d\t\t%.2f\t\t%.2f\n", afterEpisodes, theScore.mean, theScore.standardDeviation);
    }

    void offlineDemo() {
        Vector<EvaluationPoint> results = new Vector<EvaluationPoint>();
        EvaluationPoint initialScore = evaluateAgent();
        printScore(0, initialScore);
        // Every j episodes, report the mean reward, for i iterations (i*j episodes total) 
        for (int i = 0; i < NUM_REWARD_PRINTS; i++) {
            for (int j = 0; j < NUM_EPISODES; j++) {
                RLGlue.RL_episode(0);
            }
            EvaluationPoint currentScore = evaluateAgent();
            printScore((i + 1) * NUM_EPISODES, currentScore);
            results.add(currentScore);
        }

        System.out.println("The results of this experiment have been saved to a" +
                " comma-separated value file called results.csv that you may open with Matlab, Octave, Excel, etc.");

        saveResultsToCSVFile(results, "results.csv");

    }

    public void runExperiment() {
        RLGlue.RL_init();
        System.out.println("Starting offline demo\n----------------------------\nWill alternate learning for " + NUM_EPISODES + " episodes, then freeze policy and evaluate for 10 episodes.\n");
        System.out.println("After Episode\tMean Return\tStandard Deviation\n-------------------------------------------------------------------------");
        offlineDemo();

        System.out.println("\nNow we will save the agent's learned value function to a file....");

        RLGlue.RL_agent_message("save_policy valuefunction.dat");

        System.out.println("\nCalling RL_cleanup and RL_init to clear the agent's memory...");

        RLGlue.RL_cleanup();
        RLGlue.RL_init();


        System.out.println("Evaluating the agent's default policy:\n\t\tMean Return\tStandardDeviation\n------------------------------------------------------");
        EvaluationPoint thisScore=evaluateAgent();
        printScore(0, thisScore);

        System.out.println("\nLoading up the value function we saved earlier.");
        RLGlue.RL_agent_message("load_policy valuefunction.dat");

        System.out.println("Evaluating the agent after loading the value function:\n\t\tMean Return\tStandardDeviation\n------------------------------------------------------");
        thisScore=evaluateAgent();
        printScore(0, thisScore);

        System.out.println("Telling the environment to use fixed start state of 2,3.");
        RLGlue.RL_env_message("set-start-state 2 3");

        RLGlue.RL_start();

        System.out.println("Telling the environment to print the current state to the screen.");
        RLGlue.RL_env_message("print-state");

        System.out.println("Evaluating the agent a few times from a fixed start state of 2,3:\n\t\tMean Return\tStandardDeviation\n-------------------------------------------");
        thisScore=evaluateAgent();
        printScore(0, thisScore);

        System.out.println("Evaluating the agent again with the random start state:\n\t\tMean Return\tStandardDeviation\n------------------------------------------------------");
        RLGlue.RL_env_message("set-random-start-state");
        thisScore=evaluateAgent();
        printScore(0, thisScore);

        System.out.println("\nProgram Complete.");
        RLGlue.RL_cleanup();


    }

    public static void main(String[] args) {
        PacManExperiment theExperiment = new PacManExperiment();
        theExperiment.runExperiment();
    }

}
