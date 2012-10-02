/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/cerrla/Performance.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package cerrla;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

import cerrla.modular.GoalCondition;
import rrlFramework.Config;
import rrlFramework.RRLExperiment;
import rrlFramework.RRLObservations;

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

	private SortedMap<Integer, Double[]> performanceDetails_;

	/** If the performance is frozen. */
	private transient boolean frozen_;
	/** A queue of the standard deviation for each single policy. */
	private Queue<Double> internalSDs_;
	/** The minimum reward received in the episodeRewards. */
	private double minEpisodeReward_;
	/** Notes the minimum and maximum reward received. */
	private double[] minMaxReward_;
	/** If this performance is for a modular generator. */
	private boolean modularPerformance_;
	/** A queue of the most recent episodic rewards. */
	private Queue<Double> recentScores_;
	/** The current run for this performance object. */
	private int runIndex_;
	/** The time at which this performance object was created. */
	private long trainingStartTime_;
	/** The time at which this performance object was created. */
	private long trainingEndTime_;
	/** The final elite scores received for the best policy. */
	private ArrayList<Double> finalEliteScores_;

	/** A recording of performance scores for each value. */
	private static SortedMap<Integer, Float[]> performanceMap_;
	/** The parsed runtime (in seconds) of the experiment. */
	private static long runTime_;

	/**
	 * A new performance object for a module learner, so files are saved in the
	 * module directory.
	 * 
	 * @param modulePerformance
	 *            A throwaway boolean to denote modular performance.
	 * @param run
	 *            The current run number.
	 */
	public Performance(boolean modulePerformance, int run) {
		this(run);
		modularPerformance_ = true;
	}

	/**
	 * A constructor for a fresh performance object.
	 * 
	 * @param runIndex
	 *            The run index to append to saved files.
	 */
	public Performance(int runIndex) {
		performanceDetails_ = new TreeMap<Integer, Double[]>();
		finalEliteScores_ = new ArrayList<Double>();
		recentScores_ = new LinkedList<Double>();
		internalSDs_ = new LinkedList<Double>();
		minMaxReward_ = new double[2];
		minMaxReward_[0] = Float.MAX_VALUE;
		minMaxReward_[1] = -Float.MAX_VALUE;
		trainingStartTime_ = System.currentTimeMillis();
		runIndex_ = runIndex;
	}

	/**
	 * Records performance scores using sliding windows of results.
	 * 
	 * @param currentEpisode
	 *            The current episode.
	 */
	public void recordPerformanceScore(int currentEpisode) {
		if (recentScores_.isEmpty())
			return;
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

		Double[] details = new Double[PerformanceDetails.values().length];
		details[PerformanceDetails.EPISODE.ordinal()] = Double
				.valueOf(currentEpisode);
		details[PerformanceDetails.MEAN.ordinal()] = mean;
		details[PerformanceDetails.SD.ordinal()] = sd.evaluate(vals);
		performanceDetails_.put(currentEpisode, details);

		// Output current means
		if (ProgramArgument.SYSTEM_OUTPUT.booleanValue() && !frozen_) {
			DecimalFormat formatter = new DecimalFormat("#0.00");
			String meanString = formatter.format(mean);
			String sdString = formatter.format(meanDeviation);
			System.out.println("Average performance: " + meanString + " "
					+ SD_SYMBOL + " " + sdString);
		}
		if (frozen_) {
			System.out.println(currentEpisode + ": "
					+ details[PerformanceDetails.MEAN.ordinal()]);
		}
	}

	/**
	 * Saves the elite policies to file.
	 * 
	 * @param elites
	 *            The best policy, in string format.
	 * @param goal
	 *            The goal of the behaviour.
	 */
	private void saveElitePolicies(Collection<PolicyValue> elites,
			GoalCondition goal) throws Exception {
		File outputFile = new File(Config.TEMP_FOLDER, Config.getInstance()
				.getElitesFile().getName());
		outputFile.createNewFile();
		FileWriter wr = new FileWriter(outputFile);
		BufferedWriter buf = new BufferedWriter(wr);

		Config.writeFileHeader(buf, goal);

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
		// TODO May be memory leak around here somewhere.
		if (performanceDetails_.isEmpty())
			return;

		FileWriter wr = null;
		BufferedWriter buf = null;
		int lastKey = performanceDetails_.lastKey();
		Double[] lastDetails = performanceDetails_.get(lastKey);

		if (Config.getInstance().getGeneratorFile() == null) {
			// If the file has just been created, add the arguments to the head
			// of the file
			boolean newFile = perfFile.createNewFile();

			wr = new FileWriter(perfFile, true);
			buf = new BufferedWriter(wr);

			// If the file is fresh, add the program args to the top
			if (newFile)
				Config.writeFileHeader(buf, policyGenerator.getGoalCondition());

			policyGenerator.saveGenerators(buf, finalWrite);
			buf.write("\n\n" + lastKey + "\t"
					+ lastDetails[PerformanceDetails.MEAN.ordinal()] + "\n");
			buf.write("\n\n\n");

			if (finalWrite) {
				buf.write(Config.END_PERFORMANCE + "\n");
				buf.write("Total training time: "
						+ RRLExperiment.toTimeFormat(trainingEndTime_
								- trainingStartTime_));
			}

			buf.close();
			wr.close();
		}

		// Writing the raw performance
		File rawNumbers = null;
		if (Config.getInstance().getGeneratorFile() == null)
			rawNumbers = new File(perfFile.getAbsoluteFile() + "raw");
		else
			rawNumbers = new File(perfFile.getAbsoluteFile() + "greedy");

		wr = new FileWriter(rawNumbers);
		buf = new BufferedWriter(wr);

		if (ProgramArgument.SYSTEM_OUTPUT.booleanValue()
				&& policyGenerator.getGoalCondition().isMainGoal())
			System.out.println("Average episode scores:");

		if (finalWrite) {
			// Average the final elite scores
			Mean m = new Mean();
			double[] finalElites = new double[finalEliteScores_.size()];
			int i = 0;
			for (Double val : finalEliteScores_)
				finalElites[i++] = val;
			double meanBestVal = m.evaluate(finalElites);
			lastDetails[PerformanceDetails.ELITEMAX.ordinal()] = meanBestVal;
		}

		// Noting the raw numbers
		buf.write("Episode\tMean\tSD\tEliteMean\tEliteMax\tNumSlots\tNumRules\tN\tConvergence\n");
		for (Integer episode : performanceDetails_.keySet()) {
			Double[] details = performanceDetails_.get(episode);
			String performanceData = episode + "\t"
					+ details[PerformanceDetails.MEAN.ordinal()] + "\t"
					+ details[PerformanceDetails.SD.ordinal()] + "\t"
					+ details[PerformanceDetails.ELITEMEAN.ordinal()] + "\t"
					+ details[PerformanceDetails.ELITEMAX.ordinal()] + "\t"
					+ details[PerformanceDetails.NUMSLOTS.ordinal()] + "\t"
					+ details[PerformanceDetails.NUMRULES.ordinal()] + "\t"
					+ details[PerformanceDetails.POPULATION.ordinal()] + "\t"
					+ details[PerformanceDetails.CONVERGENCE.ordinal()] + "\t"
					+ "\n";
			buf.write(performanceData);

			if (ProgramArgument.SYSTEM_OUTPUT.booleanValue()
					&& policyGenerator.getGoalCondition().isMainGoal()) {
				System.out.println(episode + "\t"
						+ details[PerformanceDetails.MEAN.ordinal()] + "\t"
						+ SD_SYMBOL + "\t"
						+ details[PerformanceDetails.SD.ordinal()]);
			}
		}

		buf.close();
		wr.close();

//		if (Config.getInstance().getGeneratorFile() == null) {
//			// Writing the mutation tree
//			File mutationTreeFile = new File(perfFile.getAbsoluteFile()
//					+ "mutation");
//
//			wr = new FileWriter(mutationTreeFile);
//			buf = new BufferedWriter(wr);
//			// policyGenerator.saveMutationTree(buf);
//
//			buf.close();
//			wr.close();
//		}
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
	 * @param goalCondition
	 *            The goal condition this performance is concerned with.
	 */
	public void estimateETA(double convergence, int numElites,
			SortedSet<PolicyValue> elites, int numSlots,
			GoalCondition goalCondition) {
		if (!ProgramArgument.SYSTEM_OUTPUT.booleanValue())
			return;

		boolean mainGoal = goalCondition.isMainGoal();

		if (mainGoal) {
			long currentTime = System.currentTimeMillis();
			long elapsedTime = currentTime - trainingStartTime_;
			String elapsed = "Elapsed: "
					+ RRLExperiment.toTimeFormat(elapsedTime);
			System.out.println(elapsed);
		}

		boolean noUpdates = false;
		if (convergence == PolicyGenerator.NO_UPDATES_CONVERGENCE) {
			noUpdates = true;
			convergence = 0;
		}
		double totalRunComplete = (1.0 * runIndex_ + convergence)
				/ Config.getInstance().getNumRepetitions();
		if (frozen_)
			totalRunComplete = 1.0 * (runIndex_ + 1)
					/ Config.getInstance().getNumRepetitions();

		DecimalFormat formatter = new DecimalFormat("#0.0000");
		String modular = "";
		if (!goalCondition.isMainGoal())
			modular = "MODULAR: [" + goalCondition + "] ";
		// No updates yet, convergence unknown
		String percentStr = null;
		if (noUpdates) {
			percentStr = "Unknown convergence; No updates yet.";
		} else if (!frozen_) {
			percentStr = "~" + formatter.format(100 * convergence) + "% "
					+ modular + "converged (" + numSlots + " slots).";
		} else {
			if (convergence <= 1)
				percentStr = formatter.format(100 * convergence) + "% "
						+ modular + "test complete.";
			else
				percentStr = "---FULLY CONVERGED---";
		}
		System.out.println(percentStr);

		if (!frozen_) {
			// Adjust numElites if using bounded elites
			String best = (!elites.isEmpty()) ? ""
					+ formatter.format(elites.first().getValue()) : "?";
			String worst = (!elites.isEmpty()) ? ""
					+ formatter.format(elites.last().getValue()) : "?";
			String eliteString = "N_E: " + numElites + ", |E|: "
					+ elites.size() + ", E_best: " + best + ", E_worst: "
					+ worst;
			System.out.println(eliteString);
		}

		if (mainGoal) {
			String totalPercentStr = formatter.format(100 * totalRunComplete)
					+ "% experiment complete.";
			System.out.println(totalPercentStr);
		}
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
		if (frozen_)
			trainingEndTime_ = System.currentTimeMillis();
		recentScores_.clear();
		internalSDs_.clear();
	}

	public double getMinimumReward() {
		return minMaxReward_[0];
	}

	public double noteBestPolicyValue(ArrayList<double[]> policyRewards) {
		double average = 0;
		for (double[] reward : policyRewards)
			average += reward[RRLObservations.ENVIRONMENTAL_INDEX];
		average /= policyRewards.size();
		finalEliteScores_.add(average);
		return average;
	}

	public void noteElitesReward(int episode, Double meanEliteValue,
			Double maxEliteValue) {
		if (meanEliteValue == null || maxEliteValue == null)
			return;

		Double[] details = performanceDetails_.get(episode);
		if (details != null) {
			details[PerformanceDetails.ELITEMEAN.ordinal()] = meanEliteValue;
			details[PerformanceDetails.ELITEMAX.ordinal()] = maxEliteValue;
		}
	}

	public void noteGeneratorDetails(int episode, PolicyGenerator generator,
			int population, double convergence) {
		Double[] details = performanceDetails_.get(episode);
		if (details != null) {
			details[PerformanceDetails.NUMSLOTS.ordinal()] = Double
					.valueOf(generator.size());
			double numRules = 0;
			for (Slot s : generator.getGenerator()) {
				numRules += s.size();
			}
			details[PerformanceDetails.NUMRULES.ordinal()] = numRules;
			details[PerformanceDetails.POPULATION.ordinal()] = Double
					.valueOf(population);
			details[PerformanceDetails.CONVERGENCE.ordinal()] = Math.max(0,
					convergence);
		}
	}

	/**
	 * Notes the rewards the sample received.
	 * 
	 * @param policyRewards
	 *            The rewards the sample received.
	 * @param currentEpisode
	 *            The current episode.
	 * @return The computed average of the internal rewards.
	 */
	public double noteSampleRewards(ArrayList<double[]> policyRewards,
			int currentEpisode) {
		// First pass through the rewards to determine min reward.
		double environmentAverage = 0;
		double internalAverage = 0;
		minEpisodeReward_ = Float.MAX_VALUE;
		for (double[] reward : policyRewards) {
			double internalReward = reward[RRLObservations.INTERNAL_INDEX];
			minEpisodeReward_ = Math.min(internalReward, minEpisodeReward_);
			minMaxReward_[0] = Math.min(internalReward, minMaxReward_[0]);
			minMaxReward_[1] = Math.max(internalReward, minMaxReward_[1]);
			internalAverage += internalReward;

			environmentAverage += reward[RRLObservations.ENVIRONMENTAL_INDEX];
		}
		internalAverage /= policyRewards.size();
		environmentAverage /= policyRewards.size();

		// Second pass through to note the internal policy SDs
		for (double[] reward : policyRewards) {
			if (internalSDs_.size() == ProgramArgument.PERFORMANCE_TESTING_SIZE
					.intValue() * ProgramArgument.POLICY_REPEATS.intValue())
				internalSDs_.poll();
			internalSDs_.add(reward[RRLObservations.ENVIRONMENTAL_INDEX]
					- minEpisodeReward_);
		}

		// Note scores only if there are enough to average (or simply frozen).
		boolean noteScores = frozen_;
		if (!noteScores
				&& recentScores_.size() == ProgramArgument.PERFORMANCE_TESTING_SIZE
						.intValue()) {
			recentScores_.poll();
			noteScores = true;
		}
		recentScores_.add(environmentAverage);
		if (!frozen_)
			recordPerformanceScore(currentEpisode);

		return internalAverage;
	}

	/**
	 * Saves performance and distributions to file.
	 * 
	 * @param distribution
	 *            The current CEDistribution.
	 * @param elites
	 *            The current elites.
	 * @param currentEpisode
	 *            The current episode.
	 * @param finalWrite
	 *            If this write is the final write for this generator.
	 */
	public void saveFiles(LocalCrossEntropyDistribution distribution,
			SortedSet<PolicyValue> elites, int currentEpisode,
			boolean hasUpdated, boolean finalWrite) {
		// Determine the temp filenames
		File tempPerf = null;
		if (modularPerformance_) {
			File modTemps = LocalCrossEntropyDistribution.getModFolder(
					distribution.getGoalCondition().toString(), runIndex_);
			tempPerf = new File(modTemps, distribution.getGoalCondition()
					+ "performance.txt");
		} else {
			Config.TEMP_FOLDER.mkdir();
			tempPerf = new File(Config.TEMP_FOLDER, Config.getInstance()
					.getPerformanceFile().getName()
					+ runIndex_);
		}

		// Remove any old file if this is the first run
		if (performanceDetails_.size() <= 1
				&& Config.getInstance().getSerializedFile() == null)
			tempPerf.delete();

		// Write the files
		try {
			if (hasUpdated) {
				saveElitePolicies(elites, distribution.getGoalCondition());
				// Output the episode averages
				if (finalWrite
						&& Config.getInstance().getGeneratorFile() == null)
					recordPerformanceScore(currentEpisode);
				savePerformance(distribution.getPolicyGenerator(), tempPerf,
						finalWrite);
			}

			if (Config.getInstance().getGeneratorFile() == null) {
				// Serialise the generator
				distribution
						.saveCEDistribution(
								new File(
										tempPerf.getAbsolutePath()
												+ LocalCrossEntropyDistribution.SERIALISED_SUFFIX),
								!modularPerformance_, runIndex_);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Basic update of run
		if (!ProgramArgument.SYSTEM_OUTPUT.booleanValue()
				&& !modularPerformance_) {
			long elapsedTime = System.currentTimeMillis() - trainingStartTime_;
			String elapsed = "Elapsed: "
					+ RRLExperiment.toTimeFormat(elapsedTime);
			if (hasUpdated && !performanceDetails_.isEmpty()) {
				PolicyGenerator policyGenerator = distribution
						.getPolicyGenerator();
				DecimalFormat formatter = new DecimalFormat("#0.0000");
				String percentStr = "~"
						+ formatter.format(100 * policyGenerator
								.getConvergenceValue()) + "% " + "converged ("
						+ policyGenerator.getGenerator().size() + " slots).";
				System.out
						.println("Run "
								+ runIndex_
								+ ", learning: "
								+ currentEpisode
								+ ": "
								+ performanceDetails_.get(performanceDetails_
										.lastKey())[PerformanceDetails.MEAN
										.ordinal()] + ", " + elapsed + ", "
								+ percentStr);
				System.out.println("Learning...");
			} else
				System.out.println("Learning...");
		}
	}

	public static SortedMap<Integer, Float[]> getPerformanceArray() {
		return performanceMap_;
	}

	public static long getRunTime() {
		return runTime_;
	}

	/**
	 * Reads a raw numerical performance file and stores the values as
	 * accessible private values.
	 * 
	 * @param perfFile
	 *            The performance file to read.
	 * @return True if the file was read successfully, false otherwise.
	 */
	public static boolean readRawPerformanceFile(File perfFile,
			boolean byEpisode) throws Exception {
		if (Config.getInstance().getGeneratorFile() == null) {
			// First, read the last line of the normal file for the time
			RandomAccessFile raf = new RandomAccessFile(perfFile, "r");
			long pos = perfFile.length() - 1;
			StringBuffer line = new StringBuffer();
			char c;
			boolean foundIt = false;
			do {
				raf.seek(pos);
				c = (char) raf.read();
				foundIt |= Character.isDigit(c);
				line.append(c);
				pos--;
			} while (!foundIt || Character.isDigit(c) || c == ':');
			raf.close();
			String time = line.reverse().toString().trim();
			String[] timeSplit = time.split(":");
			runTime_ = (Long.parseLong(timeSplit[2]) + 60
					* Long.parseLong(timeSplit[1]) + 3600 * Long
					.parseLong(timeSplit[0])) * 1000;
		}

		if (Config.getInstance().getGeneratorFile() == null)
			perfFile = new File(perfFile.getPath() + "raw");
		else
			perfFile = new File(perfFile.getPath() + "greedy");
		performanceMap_ = new TreeMap<Integer, Float[]>();
		FileReader reader = new FileReader(perfFile);
		BufferedReader buf = new BufferedReader(reader);

		// For every value within the performance file
		String input = null;
		Float[] prevPerfs = null;
		while ((input = buf.readLine()) != null) {
			String[] vals = input.split("\t");
			if (vals[PerformanceDetails.EPISODE.ordinal()].equals("Episode"))
				continue;

			Float[] perfs = new Float[PerformanceDetails.values().length];
			int episode = 0;
			for (PerformanceDetails detail : PerformanceDetails.values()) {
				if (vals.length > detail.ordinal()) {
					if (!vals[detail.ordinal()].equals("null"))
						perfs[detail.ordinal()] = Float.parseFloat(vals[detail
								.ordinal()]);
					else if (detail.equals(PerformanceDetails.ELITEMEAN)
							&& !vals[PerformanceDetails.ELITEMAX.ordinal()]
									.equals("null"))
						perfs[detail.ordinal()] = Float
								.parseFloat(vals[PerformanceDetails.ELITEMAX
										.ordinal()]);
					else if (detail.equals(PerformanceDetails.ELITEMEAN)
							|| detail.equals(PerformanceDetails.ELITEMAX))
						perfs[detail.ordinal()] = Float
								.parseFloat(vals[PerformanceDetails.MEAN
										.ordinal()]);
					else if (prevPerfs != null)
						perfs[detail.ordinal()] = prevPerfs[detail.ordinal()];
				}

				if (detail.equals(PerformanceDetails.EPISODE))
					episode = perfs[detail.ordinal()].intValue();
			}

			performanceMap_.put(episode, perfs);
			prevPerfs = perfs;
		}

		buf.close();
		reader.close();

		return true;
	}

	/** The details recorded by Performance. */
	public enum PerformanceDetails {
		EPISODE,
		MEAN,
		SD,
		ELITEMEAN,
		ELITEMAX,
		NUMSLOTS,
		NUMRULES,
		POPULATION,
		CONVERGENCE;
	}
}
