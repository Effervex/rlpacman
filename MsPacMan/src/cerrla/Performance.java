package cerrla;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

import relationalFramework.agentObservations.AgentObservations;
import rrlFramework.Config;
import rrlFramework.RRLExperiment;

/**
 * This object notes performance of the agent with regards to a particular goal.
 * 
 * @author Sam Sarjant
 */
public class Performance implements Serializable {
	private static final long serialVersionUID = -2959329608447253751L;

	/**
	 * The amount of SD the score is allowed to wander (currently corresponds to
	 * 5% +- difference).
	 */
	private static final double CONVERGENCE_PERCENT_BUFFER = 0.13;

	/** Unicode symbol for +-. */
	public static final String SD_SYMBOL = "\u00b1";

	/**
	 * The number of updates that the performance has remained close to the
	 * mean.
	 */
	private int convergedCount_;
	/** The convergence value for performance convergence. */
	private double convergedMean_;
	/** The episodic reward (including policy repetitions). */
	private SortedMap<Integer, Double> episodeMeans_;
	/** The rewards received from each episode. */
	private double[] episodeRewards_;
	/** The episodic SD (including policy repetitions). */
	private SortedMap<Integer, Double> episodeSDs_;
	/** If the performance is frozen. */
	private boolean frozen_;
	/** A queue of the standard deviation for each single policy. */
	private Queue<Double> internalSDs_;
	/** The minimum reward recieved in the episodeRewards. */
	private double minEpisodeReward_;
	/** Notes the minimum and maximum reward recieved. */
	private double[] minMaxReward_;
	/** A queue of the most recent episodic rewards. */
	private Queue<Double> recentScores_;
	/** The current run for this performance object. */
	private int runIndex_;
	/** The time at which this performance object was created. */
	private long startTime_;

	/**
	 * A constructor for a fresh performance object.
	 */
	public Performance(int runIndex) {
		episodeMeans_ = new TreeMap<Integer, Double>();
		episodeSDs_ = new TreeMap<Integer, Double>();
		recentScores_ = new LinkedList<Double>();
		internalSDs_ = new LinkedList<Double>();
		minMaxReward_ = new double[2];
		minMaxReward_[0] = Float.MAX_VALUE;
		minMaxReward_[1] = -Float.MAX_VALUE;
		startTime_ = System.currentTimeMillis();
		runIndex_ = runIndex;
		resetPolicyValues();
	}

	/**
	 * Records performance scores using sliding windows of results.
	 * 
	 * @param currentEpisode
	 *            The current episode.
	 */
	private void recordPerformanceScore(int currentEpisode) {
		// Transform the queues into arrays
		double[] vals = new double[recentScores_.size()];
		int i = 0;
		for (Double val : recentScores_)
			vals[i++] = val.doubleValue();
		double[] envSDs = new double[internalSDs_.size()];
		i = 0;
		for (Double envSD : internalSDs_)
			envSDs[i++] = envSD.doubleValue();

		Mean m = new Mean();
		StandardDeviation sd = new StandardDeviation();
		double mean = m.evaluate(vals);
		double meanDeviation = sd.evaluate(envSDs) * CONVERGENCE_PERCENT_BUFFER;

		episodeMeans_.put(currentEpisode, mean);
		episodeSDs_.put(currentEpisode, sd.evaluate(vals));

		if (Math.abs(mean - convergedMean_) > meanDeviation) {
			convergedMean_ = mean;
			convergedCount_ = -1;
		}
		convergedCount_++;
	}

	/**
	 * Saves the elite policies to file.
	 * 
	 * @param elites
	 *            The best policy, in string format.
	 */
	private void saveElitePolicies(Collection<PolicyValue> elites)
			throws Exception {
		File outputFile = new File(Config.TEMP_FOLDER, Config.getInstance()
				.getElitesFile().getName());
		outputFile.createNewFile();
		FileWriter wr = new FileWriter(outputFile);
		BufferedWriter buf = new BufferedWriter(wr);

		Config.writeFileHeader(buf);

		for (PolicyValue pv : elites) {
			buf.write(pv.getPolicy().toOnlyUsedString() + "\n");
			buf.write(pv.getValue() + "\n\n");
		}

		buf.close();
		wr.close();
	}

	/**
	 * Saves the performance to file and outputs them. Saves to two files: One
	 * with a breakdown of the generators, and another with purely episodic
	 * performances.
	 * 
	 * @param policyGenerator
	 *            The policy generator to save the distributions from.
	 * @param perfFile
	 *            The performance file to save to.
	 * @param finalWrite
	 *            If this write was the final write for the run.
	 */
	private void savePerformance(PolicyGenerator policyGenerator,
			File perfFile, boolean finalWrite) throws Exception {
		if (episodeMeans_.isEmpty())
			return;

		// If the file has just been created, add the arguments to the head of
		// the file
		boolean newFile = perfFile.createNewFile();

		FileWriter wr = new FileWriter(perfFile, true);
		BufferedWriter buf = new BufferedWriter(wr);

		// If the file is fresh, add the program args to the top
		if (newFile)
			Config.writeFileHeader(buf);

		policyGenerator.saveHumanGenerators(buf, finalWrite);
		buf.write("\n\n");
		policyGenerator.saveGenerators(buf);
		int lastKey = episodeMeans_.lastKey();
		buf.write("\n\n" + lastKey + "\t" + episodeMeans_.get(lastKey) + "\n");
		buf.write("\n\n\n");

		if (finalWrite) {
			buf.write(Config.END_PERFORMANCE + "\n");
			buf.write("Total run time: "
					+ RRLExperiment.toTimeFormat(System.currentTimeMillis()
							- startTime_));
		}

		buf.close();
		wr.close();

		// Writing the raw performance
		File rawNumbers = new File(perfFile.getAbsoluteFile() + "raw");

		wr = new FileWriter(rawNumbers);
		buf = new BufferedWriter(wr);

		if (ProgramArgument.SYSTEM_OUTPUT.booleanValue()) {
			System.out.println("Average episode scores:");
			for (Integer episode : episodeMeans_.keySet()) {
				buf.write(episode + "\t" + episodeMeans_.get(episode) + "\t"
						+ episodeSDs_.get(episode) + "\n");
				System.out.println(episode + "\t" + episodeMeans_.get(episode)
						+ "\t" + SD_SYMBOL + "\t" + episodeSDs_.get(episode));
			}
		}

		buf.close();
		wr.close();

		// Writing the mutation tree
		File mutationTreeFile = new File(perfFile.getAbsoluteFile()
				+ "mutation");

		wr = new FileWriter(mutationTreeFile);
		buf = new BufferedWriter(wr);
		policyGenerator.saveMutationTree(buf);

		buf.close();
		wr.close();
	}

	/**
	 * Outputs performance information and estimates convergence.
	 * 
	 * @param convergence
	 *            The convergence as given by the rule distributions.
	 * @param numElites
	 *            The minimum number of elites.
	 * @param elites
	 *            The current elites.
	 * @param numSlots
	 *            The number of slots in the distribution.
	 * @param moduleGoal
	 *            If the learner is learning a module, null otherwise.
	 */
	public void estimateETA(double convergence, int numElites,
			SortedSet<PolicyValue> elites, int numSlots, String moduleGoal) {
		if (!ProgramArgument.SYSTEM_OUTPUT.booleanValue())
			return;

		long currentTime = System.currentTimeMillis();

		long elapsedTime = currentTime - startTime_;
		String elapsed = "Elapsed: " + RRLExperiment.toTimeFormat(elapsedTime);
		System.out.println(elapsed);

		boolean noUpdates = false;
		if (convergence == PolicyGenerator.NO_UPDATES_CONVERGENCE) {
			noUpdates = true;
			convergence = 0;
		}
		double totalRunComplete = (1.0 * runIndex_ + convergence)
				/ Config.getInstance().getNumRepetitions();

		DecimalFormat formatter = new DecimalFormat("#0.0000");
		String modular = "";
		if (moduleGoal != null)
			modular = "MODULAR: [" + moduleGoal + "] ";
		// No updates yet, convergence unknown
		String percentStr = null;
		if (noUpdates) {
			percentStr = "Unknown convergence; No updates yet.";
		} else {
			percentStr = "~" + formatter.format(100 * convergence) + "% "
					+ modular + "converged (" + numSlots + " slots).";
		}
		System.out.println(percentStr);

		// Adjust numElites if using bounded elites
		String eliteString = "N_E: " + numElites + ", |E|: " + elites.size()
				+ ", E_best: " + formatter.format(elites.first().getValue())
				+ ", E_worst: " + formatter.format(elites.last().getValue());
		System.out.println(eliteString);

		String totalPercentStr = formatter.format(100 * totalRunComplete)
				+ "% experiment complete.";
		System.out.println(totalPercentStr);

		// New lines.
		System.out.println();
		System.out.println();
	}

	/**
	 * Wipes the performance learning so that formal testing figures can take
	 * place.
	 * 
	 * @param b
	 *            Freezing or unfreezing.
	 */
	public void freeze(boolean b) {
		frozen_ = b;
		recentScores_.clear();
		internalSDs_.clear();
	}

	public double getMinimumReward() {
		return minMaxReward_[0];
	}

	/**
	 * Checks if the performance appears to be converged (hasn't changed for
	 * some time).
	 * 
	 * @return True if the performance appears to be converged.
	 */
	public boolean isConverged() {
		return convergedCount_ >= ProgramArgument.NUM_PERFORMANCES_CONVERGED
				.intValue();
	}

	/**
	 * Notes the episode reward.
	 * 
	 * @param episodeReward
	 *            The total reward received during the episode.
	 * @param policyEpisode
	 *            The policy relative episode index.
	 */
	public void noteEpisodeReward(double episodeReward, int policyEpisode) {
		minMaxReward_[0] = Math.min(episodeReward, minMaxReward_[0]);
		minMaxReward_[1] = Math.max(episodeReward, minMaxReward_[1]);
		episodeRewards_[policyEpisode] = episodeReward;
		minEpisodeReward_ = Math.min(episodeReward, minEpisodeReward_);
	}

	/**
	 * Notes the averaged reward received for a sample.
	 * 
	 * @param value
	 *            The averaged reward.
	 * @param currentEpisode
	 *            The current episode.
	 */
	public void noteSampleReward(double value, int currentEpisode) {
		// Note the internal policy SDs
		for (double reward : episodeRewards_) {
			if (internalSDs_.size() == ProgramArgument.PERFORMANCE_TESTING_SIZE
					.intValue() * ProgramArgument.POLICY_REPEATS.intValue())
				internalSDs_.poll();
			internalSDs_.add(reward - minEpisodeReward_);
		}

		// Note scores only if there are enough to average (or simply frozen).
		boolean noteScores = frozen_;
		if (!noteScores
				&& recentScores_.size() == ProgramArgument.PERFORMANCE_TESTING_SIZE
						.intValue()) {
			recentScores_.poll();
			noteScores = true;
		}
		recentScores_.add(value);
		if (noteScores)
			recordPerformanceScore(currentEpisode);

		resetPolicyValues();
	}

	/**
	 * Resets performance measures that are local to a policy.
	 */
	public void resetPolicyValues() {
		episodeRewards_ = new double[ProgramArgument.POLICY_REPEATS.intValue()];
		minEpisodeReward_ = Float.MAX_VALUE;
	}

	/**
	 * Saves performance and distributions to file.
	 * 
	 * @param policyGenerator
	 *            The current policy generator.
	 * @param elites
	 *            The current elites.
	 * @param currentEpisode
	 *            The current episode.
	 */
	public void saveFiles(PolicyGenerator policyGenerator,
			SortedSet<PolicyValue> elites, int currentEpisode) {
		boolean hasUpdated = policyGenerator.hasUpdated();

		// Basic update of run
		if (!ProgramArgument.SYSTEM_OUTPUT.booleanValue()) {
			long elapsedTime = System.currentTimeMillis() - startTime_;
			String elapsed = "Elapsed: "
					+ RRLExperiment.toTimeFormat(elapsedTime);
			if (hasUpdated && !episodeMeans_.isEmpty()) {
				System.out.println("Run " + runIndex_ + ", learning: "
						+ currentEpisode + ": "
						+ episodeMeans_.get(episodeMeans_.lastKey()) + ", "
						+ elapsed);
				System.out.println("Learning...");
			} else
				System.out.println("Learning...");
		}

		// Determine the temp filenames
		File tempPerf = null;
		if (policyGenerator.isModuleGenerator()) {
			File modTemps = new File(Module.MODULE_DIR + File.separatorChar
					+ Config.TEMP_FOLDER + File.separatorChar);
			modTemps.mkdirs();
			tempPerf = new File(modTemps, policyGenerator.getLocalGoal()
					+ Config.getInstance().getPerformanceFile().getName());
		} else {
			Config.TEMP_FOLDER.mkdir();
			tempPerf = new File(Config.TEMP_FOLDER, Config.getInstance()
					.getPerformanceFile().getName()
					+ runIndex_);
		}

		// Remove any old file if this is the first run
		if (episodeMeans_.size() <= 1
				&& Config.getInstance().getSerializedFile() == null)
			tempPerf.delete();


		// Write the files
		try {
			if (hasUpdated) {
				saveElitePolicies(elites);
				// Output the episode averages
				savePerformance(policyGenerator, tempPerf, frozen_);
			}
			policyGenerator
					.serialisePolicyGenerator(new File(tempPerf + ".ser"));
			AgentObservations.getInstance().saveAgentObservations(
					policyGenerator);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
