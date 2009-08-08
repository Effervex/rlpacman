package rlPacMan;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.*;

import org.rlcommunity.rlglue.codec.RLGlue;

public class CrossEntropyExperiment {
	/** The number of iterations a policy is repeated to get an average score. */
	public static final int AVERAGE_ITERATIONS = 3;
	/** The best policy found output file. */
	private final File policyFile_;
	/** The generator states file. */
	private final File generatorFile_;
	public static final String ELEMENT_DELIMITER = ",";

	/** The population size of the experiment. */
	private int population_;
	/** The number of episodes to run. */
	private int episodes_;
	/** The ratio of samples to use as 'elite' samples. */
	private double selectionRatio_;
	/** The rate at which the weights change. */
	private double stepSize_;
	/** The rate of decay on the slots. */
	private double slotDecayRate_;
	/** The cross-entropy generator for the slots in the policy. */
	private ProbabilityDistribution<Integer> slotGenerator_;
	/** The maximum size of the policy. */
	private final int policySize_;
	/** The time that the experiment started. */
	private long experimentStart_;
	/** The ratio of shared/regenerated rules. */
	private float ratioShared_ = 0.08f;

	/**
	 * A constructor for initialising the cross-entropy generators and
	 * experiment parameters.
	 * 
	 * @param populationSize
	 *            The size of the population used in calculations.
	 * @param episodeCount
	 *            The number of episodes to perform.
	 * @param selectionRatio
	 *            The percentage of 'elite' samples to use from the population.
	 * @param stepSize
	 *            The step size for learning weights.
	 * @param slotDecayRate
	 *            The rate at which the slot probabilities decay per episode.
	 * @param policySize
	 *            The maximum number of rules in the policy.
	 */
	@SuppressWarnings("unchecked")
	public CrossEntropyExperiment(int populationSize, int episodeCount,
			double selectionRatio, double stepSize, double slotDecayRate,
			int policySize, boolean handCoded, String policyFile,
			String generatorFile) {
		population_ = populationSize;
		episodes_ = episodeCount;
		selectionRatio_ = selectionRatio;
		stepSize_ = stepSize;
		slotDecayRate_ = slotDecayRate;
		slotGenerator_ = new ProbabilityDistribution<Integer>();
		policySize_ = policySize;
		RuleBase.initInstance(handCoded, policySize);
		// Filling the generators
		for (int i = 0; i < policySize; i++) {
			slotGenerator_.add(i, 0.5);
		}

		// Create the output files if necessary
		policyFile_ = new File(policyFile);
		generatorFile_ = new File(generatorFile);
		try {
			if (!policyFile_.exists())
				policyFile_.createNewFile();
			if (!generatorFile_.exists())
				generatorFile_.createNewFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * A constructor for the typical arguments plus a state of the generators
	 * file.
	 * 
	 * @param populationSize
	 *            The size of the population used in calculations.
	 * @param episodeCount
	 *            The number of episodes to perform.
	 * @param selectionRatio
	 *            The percentage of 'elite' samples to use from the population.
	 * @param stepSize
	 *            The step size for learning weights.
	 * @param slotDecayRate
	 *            The rate at which the slot probabilities decay per episode.
	 * @param policySize
	 *            The maximum number of rules in the policy.
	 */
	@SuppressWarnings("unchecked")
	public CrossEntropyExperiment(int populationSize, int episodeCount,
			double selectionRatio, double stepSize, double slotDecayRate,
			int policySize, boolean handCoded, String policyFile,
			String generatorFile, String genInputFile) {
		this(populationSize, episodeCount, selectionRatio, stepSize,
				slotDecayRate, policySize, handCoded, policyFile, generatorFile);
		// Load the generators from the input file
		try {
			loadGenerators(new File(genInputFile));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * A constructor for the typical arguments plus a state of the generators
	 * file.
	 * 
	 * @param populationSize
	 *            The size of the population used in calculations.
	 * @param episodeCount
	 *            The number of episodes to perform.
	 * @param selectionRatio
	 *            The percentage of 'elite' samples to use from the population.
	 * @param stepSize
	 *            The step size for learning weights.
	 * @param slotDecayRate
	 *            The rate at which the slot probabilities decay per episode.
	 * @param policySize
	 *            The maximum number of rules in the policy.
	 */
	@SuppressWarnings("unchecked")
	public CrossEntropyExperiment(int populationSize, int episodeCount,
			double selectionRatio, double stepSize, double slotDecayRate,
			int policySize, String ruleFile, String policyFile,
			String generatorFile, String genInputFile) {
		this(populationSize, episodeCount, selectionRatio, stepSize,
				slotDecayRate, policySize, false, policyFile, generatorFile);
		// Load the generators from the input file
		try {
			RuleBase.initInstance(new File(ruleFile));
			loadGenerators(new File(genInputFile));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Runs the experiment.
	 */
	public void runExperiment() {
		experimentStart_ = System.currentTimeMillis();

		// Initialise the environment/agent
		RLGlue.RL_init();

		// The outer loop, for refinement episode by episode
		// Most outputs/frozen tests will be performed at the end of each of
		// these.
		PolicyValue bestPolicy = null;
		float[] episodeAverage = new float[episodes_];
		float runningAverage = 0;
		for (int t = 0; t < episodes_; t++) {
			// Forming a population of solutions
			SortedSet<PolicyValue> pvs = new TreeSet<PolicyValue>();
			for (int i = 0; i < population_; i++) {
				Policy pol = generatePolicy();
				// pol = paperPolicy();
				// Send the agent a generated policy
				RLGlue.RL_agent_message(pol.toParseableString());

				float score = 0;
				for (int j = 0; j < AVERAGE_ITERATIONS; j++) {
					RLGlue.RL_episode(1000000);
					score += Float.parseFloat(RLGlue.RL_env_message("score"));
				}
				score /= AVERAGE_ITERATIONS;
				// Set the fired rules back to this policy
				pol.setFired(RLGlue.RL_agent_message("getFired"));

				PolicyValue thisPolicy = new PolicyValue(pol, score);
				pvs.add(thisPolicy);
				// Storing the best policy
				if ((bestPolicy == null)
						|| (thisPolicy.getValue() > bestPolicy.getValue()))
					bestPolicy = thisPolicy;

				// Give an ETA
				estimateETA(t * population_ + i + 1, episodes_ * population_);
			}

			// Update the weights for all distributions using only the elite
			// samples
			float value = (t == 0) ? 0 : episodeAverage[t - 1];
			episodeAverage[t] = updateWeights(pvs.iterator(), population_
					* selectionRatio_, value, runningAverage);
			runningAverage = ((episodeAverage[t] - value) + runningAverage) / 2;

			// Save the results at each episode
			try {
				saveGenerators(t);
				saveBestPolicy(bestPolicy);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Output the episode averages
		System.out.println("Average episode elite scores:");
		for (int e = 0; e < episodeAverage.length; e++) {
			System.out.println(episodeAverage[e]);
		}

		RLGlue.RL_cleanup();
	}

	/**
	 * Prints out the percentage complete, time elapsed and estimated time to
	 * completion.
	 * 
	 * @param totalProg
	 *            The total amount of progress to cover.
	 * @param currentProg
	 *            The current progress.
	 */
	private void estimateETA(int currentProg, int totalProg) {
		long elapsedTime = System.currentTimeMillis() - experimentStart_;
		double percent = (currentProg * 1.0) / totalProg;

		DecimalFormat formatter = new DecimalFormat("#0.00000");
		String percentStr = formatter.format(100 * percent) + "% complete.";
		String elapsed = "Elapsed: " + elapsedTime / (1000 * 60 * 60) + ":"
				+ (elapsedTime / (1000 * 60)) % 60 + ":" + (elapsedTime / 1000)
				% 60;
		long remainingTime = (long) (elapsedTime / percent - elapsedTime);
		String remaining = "Remaining: " + remainingTime / (1000 * 60 * 60)
				+ ":" + (remainingTime / (1000 * 60)) % 60 + ":"
				+ (remainingTime / 1000) % 60;
		System.out.println(percentStr + " " + elapsed + ", " + remaining);
		System.out.println();
	}

	/**
	 * Saves the best policy to a file.
	 * 
	 * @param bestPolicy
	 *            The best policy, in string format.
	 */
	private void saveBestPolicy(PolicyValue bestPolicy) throws Exception {
		FileWriter wr = new FileWriter(policyFile_);
		BufferedWriter buf = new BufferedWriter(wr);

		buf.write(bestPolicy.getPolicy().toParseableString() + "\n");
		buf.write(bestPolicy.getValue() + "\n");

		buf.close();
		wr.close();
	}

	/**
	 * Saves the generators/distributions to file.
	 * 
	 * @throws Exception
	 *             Should something go awry.
	 */
	private void saveGenerators(int episode) throws Exception {
		FileWriter wr = new FileWriter(generatorFile_);
		BufferedWriter buf = new BufferedWriter(wr);

		StringBuffer strBuffer = new StringBuffer();
		// Write the slot generator
		for (int i = 0; i < slotGenerator_.size(); i++) {
			strBuffer.append(slotGenerator_.getProb(i) + ELEMENT_DELIMITER);
		}
		strBuffer.append("\n");
		buf.write(strBuffer.toString());
		// Write the rule generators
		RuleBase.getInstance().writeGenerators(buf);
		buf.write(episode);

		buf.close();
		wr.close();
	}

	/**
	 * Loads the generators/distributions from file.
	 * 
	 * @param file
	 *            The file to laod from.
	 * @throws Exception
	 *             Should something go awry.
	 */
	private void loadGenerators(File file) throws Exception {
		FileReader reader = new FileReader(file);
		BufferedReader buf = new BufferedReader(reader);

		// Parse the slots
		String[] split = buf.readLine().split(ELEMENT_DELIMITER);
		for (int i = 0; i < split.length; i++) {
			slotGenerator_.set(i, Double.parseDouble(split[i]));
		}

		// Parse the rules
		RuleBase.getInstance().readGenerators(buf);

		buf.close();
		reader.close();
	}

	// private Policy paperPolicy() {
	// Policy pol = new Policy(9);
	// pol.addRule(0, RuleBase.getInstance().getRule(2, 0));
	// pol.addRule(1, RuleBase.getInstance().getRule(12, 0));
	// pol.addRule(3, RuleBase.getInstance().getRule(22, 0));
	// pol.addRule(4, RuleBase.getInstance().getRule(34, 0));
	// pol.addRule(5, RuleBase.getInstance().getRule(26, 0));
	// pol.addRule(6, RuleBase.getInstance().getRule(0, 0));
	// return pol;
	// }

	/**
	 * Updates the weights in the probability distributions according to their
	 * frequency within the 'elite' samples.
	 * 
	 * @param iter
	 *            The iterator over the samples.
	 * @param numElite
	 *            The number of samples to form the 'elite' samples.
	 * @param lastEpisode
	 *            The average elites value for the last episode. Used for
	 *            determining if the performance is settling.
	 * @param runningAverage
	 *            The running average score among the elites. Used for
	 *            determining if the performance is settling.
	 */
	private float updateWeights(Iterator<PolicyValue> iter, double numElite,
			float lastEpisode, float runningAverage) {
		// Keep count of the rules seen (and slots used)
		int[][] slotCounter = new int[policySize_][1 + RuleBase.getInstance()
				.size()];
		float episodeValue = countRules(iter, numElite, slotCounter);
		float thisAverage = ((episodeValue - lastEpisode) + runningAverage) / 2;

		// Apply the weights to the distributions
		for (int s = 0; s < slotGenerator_.size(); s++) {
			// Change the slot probabilities, factoring in decay
			slotGenerator_.updateElement(numElite, slotCounter[s][0],
					stepSize_, slotDecayRate_, s);

			// Update the internal rule distributions
			RuleBase.getInstance().updateDistribution(s, numElite,
					slotCounter[s], 1, stepSize_, 1);
		}

		// Further updates on the distributions
		ratioShared_ = RuleBase.getInstance().postUpdateOperations(thisAverage,
				ratioShared_, stepSize_);

		return episodeValue;
	}

	/**
	 * Counts the rules from the elite samples and stores their frequencies and
	 * total score.
	 * 
	 * @param iter
	 *            The iterator through the samples.
	 * @param numElite
	 *            The number of elite samples to iterate through.
	 * @param slotCounter
	 *            The storage for the rule counts.
	 * @return The average value of the elite samples.
	 */
	private float countRules(Iterator<PolicyValue> iter, double numElite,
			int[][] slotCounter) {
		float total = 0;
		// Only selecting the top elite samples
		for (int k = 0; k < numElite; k++) {
			PolicyValue pv = iter.next();
			total += pv.value_;
			Policy eliteSolution = pv.getPolicy();

			// Count the occurrences of rules and slots in the policy
			Rule[] polRules = eliteSolution.getRules();
			for (int i = 0; i < polRules.length; i++) {
				// If there is a rule
				if (polRules[i] != null) {
					slotCounter[i][0]++;
					slotCounter[i][1 + RuleBase.getInstance().indexOf(
							polRules[i], i)]++;
				}
			}
		}

		// Maintaining values
		float episodeValue = (float) (total / numElite);
		return episodeValue;
	}

	/**
	 * Generates a random policy using the weights present in the probability
	 * distribution.
	 * 
	 * @return A new policy, formed using weights from the probability
	 *         distributions.
	 */
	private Policy generatePolicy() {
		Policy policy = new Policy(policySize_);

		// Run through the policy, adding any rule with probability p and a
		// particular rule with probability q.
		for (int i = 0; i < policySize_; i++) {
			if (slotGenerator_.bernoulliSample(i) != null) {
				policy.addRule(i, RuleBase.getInstance().getRuleGenerator(i)
						.sample());
			}
		}
		return policy;
	}

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments for the program.
	 */
	public static void main(String[] args) {
		CrossEntropyExperiment theExperiment = null;
		if (args.length == 9) {
			theExperiment = new CrossEntropyExperiment(Integer
					.parseInt(args[0]), Integer.parseInt(args[1]), Double
					.parseDouble(args[2]), Double.parseDouble(args[3]), Double
					.parseDouble(args[4]), Integer.parseInt(args[5]), Boolean
					.parseBoolean(args[6]), args[7], args[8]);
		} else if (args.length == 10) {
			theExperiment = new CrossEntropyExperiment(Integer
					.parseInt(args[0]), Integer.parseInt(args[1]), Double
					.parseDouble(args[2]), Double.parseDouble(args[3]), Double
					.parseDouble(args[4]), Integer.parseInt(args[5]), Boolean
					.parseBoolean(args[6]), args[7], args[8], args[9]);
		} else if (args.length == 11) {
			theExperiment = new CrossEntropyExperiment(Integer
					.parseInt(args[0]), Integer.parseInt(args[1]), Double
					.parseDouble(args[2]), Double.parseDouble(args[3]), Double
					.parseDouble(args[4]), Integer.parseInt(args[5]), args[7],
					args[8], args[9], args[10]);
		}
		theExperiment.runExperiment();
		System.exit(0);
	}

	/**
	 * A simple class for binding a policy and a value together in a comparable
	 * format.
	 * 
	 * @author Samuel J. Sarjant
	 * 
	 */
	private class PolicyValue implements Comparable<PolicyValue> {
		/** The policy. */
		private Policy policy_;
		/** The estimated value of the policy. */
		private float value_;

		/**
		 * A constructor for storing the members.
		 * 
		 * @param pol
		 *            The policy.
		 * @param value
		 *            The (estimated) value
		 */
		public PolicyValue(Policy pol, float value) {
			policy_ = pol;
			value_ = value;
		}

		/**
		 * Gets the policy for this policy-value.
		 * 
		 * @return The policy.
		 */
		public Policy getPolicy() {
			return policy_;
		}

		/**
		 * Gets the value for this policy-value.
		 * 
		 * @return The value.
		 */
		public float getValue() {
			return value_;
		}

		// @Override
		public int compareTo(PolicyValue o) {
			if ((o == null) || (!(o instanceof PolicyValue)))
				return -1;
			PolicyValue pv = (PolicyValue) o;
			// If this value is bigger, it comes first
			if (value_ > pv.value_) {
				return -1;
			} else if (value_ < pv.value_) {
				// Else it is after
				return 1;
			} else {
				// If all else fails, order by hash code
				return Float.compare(hashCode(), o.hashCode());
			}
		}

		// @Override
		public boolean equals(Object obj) {
			if ((obj == null) || (!(obj instanceof PolicyValue)))
				return false;
			PolicyValue pv = (PolicyValue) obj;
			if (value_ == pv.value_) {
				if (policy_ == pv.policy_) {
					return true;
				}
			}
			return false;
		}

		// @Override
		public int hashCode() {
			return (int) (value_ * policy_.hashCode());
		}
	}
}
