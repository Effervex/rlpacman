package cerrla;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import relationalFramework.CoveringRelationalPolicy;
import relationalFramework.RelationalPolicy;
import relationalFramework.agentObservations.AgentObservations;

/**
 * A localised-to-goal class containing the agent's behaviour, the current goal,
 * and performance observations relating to this behaviour.
 * 
 * @author Sam Sarjant
 */
public class LocalCrossEntropyDistribution {
	/** The current episode as evidenced by this generator. */
	private int currentEpisode_;
	/** The elites set. */
	private SortedSet<PolicyValue> elites_;
	/** The population value. */
	private int population_;
	/** The minimum number of elites value. */
	private int numElites_;
	/** The goal condition for this cross-entropy behaviour. */
	private String goalCondition_;
	/** The performance object, noting figures. */
	private Performance performance_;
	/** The distributions of rules. */
	private PolicyGenerator policyGenerator_;
	/** If the AgentObsrvations were settled last episode. */
	private boolean oldAOSettled_;
	/** If this generator is currently frozen (not learning). */
	private boolean frozen_;

	/**
	 * Initialise new learned behaviour with the given goal.
	 * 
	 * @param goal
	 *            The goal of the behaviour.
	 * @param run
	 *            The run this generator is for.
	 */
	public LocalCrossEntropyDistribution(String goal, int run) {
		goalCondition_ = goal;
		policyGenerator_ = new PolicyGenerator(run);
		performance_ = new Performance(run);
		elites_ = new TreeSet<PolicyValue>();
	}

	/**
	 * Modifies the policy values before updating (cutting the values down to
	 * size).
	 * 
	 * @param elites
	 *            The policy values to modify.
	 * @param numElite
	 *            The minimum number of elite samples.
	 * @param staleValue
	 *            The number of policies a sample hangs around for.
	 * @param minValue
	 *            The minimum observed value.
	 * @return The policy values that were removed.
	 */
	private SortedSet<PolicyValue> preUpdateModification(
			SortedSet<PolicyValue> elites, int numElite, int staleValue,
			double minValue) {
		// Firstly, remove any policy values that have been around for more
		// than N steps

		// Only remove stuff if the elites are a representative solution
		int iteration = policyGenerator_.getPoliciesEvaluated();
		for (Iterator<PolicyValue> iter = elites.iterator(); iter.hasNext();) {
			PolicyValue pv = iter.next();
			if (iteration - pv.getIteration() >= staleValue) {
				if (ProgramArgument.RETEST_STALE_POLICIES.booleanValue())
					policyGenerator_.retestPolicy(pv.getPolicy());
				iter.remove();
			}
		}

		SortedSet<PolicyValue> tailSet = null;
		if (elites.size() > numElite) {
			// Find the N_E value
			Iterator<PolicyValue> pvIter = elites.iterator();
			PolicyValue currentPV = null;
			for (int i = 0; i < numElite; i++)
				currentPV = pvIter.next();

			// Iter at N_E value. Remove any values less than N_E's value
			tailSet = new TreeSet<PolicyValue>(elites.tailSet(new PolicyValue(
					null, currentPV.getValue(), -1)));
			elites.removeAll(tailSet);
		}

		return tailSet;
	}

	/**
	 * Updates the distributions based on the current state of the elites.
	 * 
	 * @param elites
	 *            The elite values (+ 1 more sample).
	 * @return True if the distribution changed drastically (new slots/rules
	 *         created).
	 */
	private boolean updateDistributions(SortedSet<PolicyValue> elites,
			int population, int numElites) {
		double minReward = performance_.getMinimumReward();

		// Clean up the policy values
		SortedSet<PolicyValue> removed = preUpdateModification(elites,
				numElites, population, minReward);

		policyGenerator_.updateDistributions(elites,
				ProgramArgument.ALPHA.doubleValue(), population, numElites,
				minReward);
		// Negative updates:
		if (ProgramArgument.NEGATIVE_UPDATES.booleanValue())
			policyGenerator_.updateNegative(elites,
					ProgramArgument.ALPHA.doubleValue(), population, numElites,
					removed);

		// Run the post update operations
		boolean resetElites = policyGenerator_.postUpdateOperations(numElites);

		// Clear the restart
		policyGenerator_.shouldRestart();
		return resetElites;
	}

	/**
	 * Determines the reward received, given the environment's reward.
	 * 
	 * @param environmentReward
	 *            The reward provided by the environment.
	 * @return The reward to be recorded.
	 */
	public double determineReward(double environmentReward) {
		// TODO Modify if internal reward.
		return environmentReward;
	}

	/**
	 * Generates a policy from the current distribution.
	 * 
	 * @return A newly generated policy from the current distribution.
	 */
	public RelationalPolicy generatePolicy() {
		RelationalPolicy policy = policyGenerator_.generatePolicy(false);
		if (ProgramArgument.SYSTEM_OUTPUT.booleanValue()) {
			System.out.println(policy);
		}

		return policy;
	}

	/**
	 * Checks if this CE distribution is converged.
	 * 
	 * @return True if the distribution is converged.
	 */
	public boolean isConverged() {
		// Check performance convergence
		if (ProgramArgument.PERFORMANCE_CONVERGENCE.booleanValue())
			if (performance_.isConverged())
				return true;

		// Check elite convergence
		if (elites_.size() >= population_
				* (1 - ProgramArgument.RHO.doubleValue())
				&& elites_.first().getValue() == elites_.last().getValue()
				&& elites_.first().getValue() > performance_.getMinimumReward())
			return true;

		// Check distribution convergence
		if (policyGenerator_.isConverged())
			return true;

		return false;
	}

	/**
	 * Notes the reward for a _single_ episode.
	 * 
	 * @param episodeReward
	 *            The total reward received this episode.
	 * @param policyEpisode
	 *            The policy relative episode index.
	 */
	public void noteEpisodeReward(double episodeReward, int policyEpisode) {
		performance_.noteEpisodeReward(episodeReward, policyEpisode);
		if (!frozen_)
			currentEpisode_++;
	}

	/**
	 * Records a given sample with a given reward.
	 * 
	 * @param sample
	 *            The sample policy to record.
	 * @param value
	 *            The value of the sample.
	 */
	public void recordSample(CoveringRelationalPolicy sample, double value) {
		if (!frozen_) {
			// Add sample to elites
			PolicyValue pv = new PolicyValue(sample, value,
					policyGenerator_.getPoliciesEvaluated());
			elites_.add(pv);
			policyGenerator_.incrementPoliciesEvaluated();

			// Calculate the population and number of elites
			population_ = policyGenerator_.determinePopulation();
			numElites_ = (int) Math.ceil(population_
					* ProgramArgument.RHO.doubleValue());
			// Update distributions (depending on number of elites)
			updateDistributions(elites_, population_, numElites_);
		}



		// Note performance values
		if (ProgramArgument.SYSTEM_OUTPUT.booleanValue()) {
			System.out.println();
			System.out.println(currentEpisode_ + ": " + value);
		}
		performance_.noteSampleReward(value, currentEpisode_);

		// Estimate experiment convergence
		double convergence = policyGenerator_.getConvergenceValue();
		int numSlots = policyGenerator_.size();
		String moduleGoal = (policyGenerator_.isModuleGenerator()) ? policyGenerator_
				.getLocalGoal() : null;
		performance_.estimateETA(convergence, numElites_, elites_, numSlots,
				moduleGoal);



		if (!frozen_) {
			// Save files if necessary
			if ((AgentObservations.getInstance().isSettled() && !oldAOSettled_)
					|| policyGenerator_.getPoliciesEvaluated()
							% ProgramArgument.PERFORMANCE_TESTING_SIZE
									.doubleValue() == 1) {
				performance_.saveFiles(policyGenerator_, elites_,
						currentEpisode_);
			}

			oldAOSettled_ = AgentObservations.getInstance().isSettled();
		}
	}

	/**
	 * Performs a final write of the behaviour.
	 */
	public void finalWrite() {
		// Finalise the testing
		performance_.saveFiles(policyGenerator_, elites_, currentEpisode_);
	}

	/**
	 * Freeze learning (and begin testing).
	 * 
	 * @param b
	 *            To freeze or unfreeze
	 */
	public void freeze(boolean b) {
		frozen_ = b;
		policyGenerator_.freeze(b);
		performance_.freeze(b);
	}
}
