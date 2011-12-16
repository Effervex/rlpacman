package cerrla;

import java.util.HashMap;
import java.util.Map;

import relationalFramework.CoveringRelationalPolicy;
import relationalFramework.RelationalPolicy;
import relationalFramework.StateSpec;
import rrlFramework.Config;
import rrlFramework.RRLActions;
import rrlFramework.RRLAgent;
import rrlFramework.RRLObservations;

/**
 * The CERRLA agent. This uses a number of distributions of relational rules to
 * generate policies for effectively acting within environments. The
 * distributions are optimised using the cross-entropy method.
 * 
 * @author Sam Sarjant
 */
public class CERRLA implements RRLAgent {
	/** The set of learned behaviours the agent is maintaining concurrently. */
	private Map<String, LocalCrossEntropyDistribution> goalMappedGenerators_;

	/** The current behaviour for the goal at hand. Convenience member. */
	private LocalCrossEntropyDistribution currentCECortex_;

	/** The current policy being tested. */
	private RelationalPolicy currentPolicy_;

	/** The number of episodes the policy has been involved in. */
	private int policyEpisodes_;

	/** The reward received this episode. */
	private double episodeReward_;

	/** The total reward received by this policy. */
	private double totalPolicyReward_;

	@Override
	public void initialise(int run) {
		goalMappedGenerators_ = new HashMap<String, LocalCrossEntropyDistribution>();
		currentCECortex_ = new LocalCrossEntropyDistribution(Config
				.getInstance().getGoal(), run);
	}

	@Override
	public boolean isConverged() {
		return currentCECortex_.isConverged();
	}

	@Override
	public void cleanup() {
		// Save the final output
		currentCECortex_.finalWrite();
	}

	@Override
	public RRLActions startEpisode(RRLObservations observations) {
		episodeReward_ = 0;
		if (currentPolicy_ == null) {
			// Generate a new policy
			currentPolicy_ = currentCECortex_.generatePolicy();
			policyEpisodes_ = 0;
			totalPolicyReward_ = 0;
		}
		// TODO Policy restarts...
		return new RRLActions(currentPolicy_.evaluatePolicy(observations,
				StateSpec.getInstance().getNumReturnedActions()));
	}

	@Override
	public RRLActions stepEpisode(RRLObservations observations) {
		episodeReward_ += currentCECortex_.determineReward(observations
				.getReward());
		return new RRLActions(currentPolicy_.evaluatePolicy(observations,
				StateSpec.getInstance().getNumReturnedActions()));
	}

	@Override
	public void endEpisode(RRLObservations observations) {
		// Note figures
		episodeReward_ += currentCECortex_.determineReward(observations
				.getReward());
		totalPolicyReward_ += episodeReward_;

		// Note the rewards.
		currentCECortex_.noteEpisodeReward(episodeReward_, policyEpisodes_);
		policyEpisodes_++;
		if (policyEpisodes_ >= ProgramArgument.POLICY_REPEATS.intValue()) {
			currentCECortex_.recordSample(
					(CoveringRelationalPolicy) currentPolicy_,
					totalPolicyReward_ / policyEpisodes_);
			currentPolicy_ = null;
		}
	}

	@Override
	public void freeze(boolean b) {
		currentCECortex_.freeze(b);
	}

}
