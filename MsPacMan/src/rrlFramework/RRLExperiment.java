package rrlFramework;

import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

import relationalFramework.StateSpec;
import cerrla.CERRLA;
import cerrla.Performance;
import cerrla.Performance.PerformanceDetails;
import cerrla.ProgramArgument;

/**
 * An abstract experiment class for running an RRL experiment.
 * 
 * @author Sam Sarjant
 */
public class RRLExperiment {
	/** The random number generator. */
	public static Random random_ = new Random();

	/** If we're running the experiment in debug mode. */
	public static boolean debugMode_ = false;

	/** The agent to use for experiments. */
	private RRLAgent agent_;

	/** The environment to use for experiments. */
	private RRLEnvironment environment_;

	/**
	 * Start a new experiment with the given args.
	 * 
	 * @param args
	 *            The provided arguments (filename, etc).
	 */
	public RRLExperiment(String[] args) {
		Config.newInstance(args);

		try {
			agent_ = setAgent();
			environment_ = (RRLEnvironment) Class.forName(
					Config.getInstance().getEnvironmentClass()
							+ RRLEnvironment.ENVIRONMENT_CLASS_SUFFIX)
					.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks the files for pre-existing versions so runs do not have to be
	 * re-run.
	 * 
	 * @return The run number that the files stopped at and the point at which
	 *         the experiment stopped.
	 */
	private int[] checkFiles(int startPoint) {
		// Check the performance files
		int[] result = new int[2];
		// Find the last file created
		int run = startPoint;
		result[0] = run;
		if (Config.getInstance().getSerializedFile() != null)
			return result;

		File lastPerf = null;
		String tempPerfName = Config.TEMP_FOLDER + "/"
				+ Config.getInstance().getPerformanceFile().getName() + run;
		if (Config.getInstance().getGeneratorFile() != null)
			tempPerfName = tempPerfName + "greedy";
		File tempPerf = new File(tempPerfName);
		while (tempPerf.exists()) {
			run++;
			lastPerf = tempPerf;

			tempPerfName = Config.TEMP_FOLDER + "/"
					+ Config.getInstance().getPerformanceFile().getName() + run;
			if (Config.getInstance().getGeneratorFile() != null)
				tempPerfName = tempPerfName + "greedy";
			tempPerf = new File(tempPerfName);
		}

		// If there aren't any performance files, return 0,0
		if (lastPerf == null)
			return result;

		// If greedy generators, a file means the run is complete
		if (Config.getInstance().getGeneratorFile() != null) {
			result[0] = run + 1;
			return result;
		}


		// Otherwise, scan the last file for how far in it got through
		try {
			FileReader reader = new FileReader(lastPerf);
			BufferedReader br = new BufferedReader(reader);
			int iteration = -1;
			String input = null;
			// Read lines until end performance marker, or null lines.
			while ((input = br.readLine()) != null) {
				if (input.equals(Config.END_PERFORMANCE)) {
					result[0] = run;
					result[1] = -1;
					return result;
				}

				// If the value is a number, increment iteration
				try {
					String[] split = input.split("\t");
					if (split.length == 2) {
						Integer.parseInt(split[0]);
						Double.parseDouble(split[1]);
						iteration++;
					}
				} catch (Exception e) {
				}
			}

			result[0] = run - 1;
			result[1] = iteration;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Compiles the performance files together into a single file, detailing the
	 * average, min and max performances.
	 * 
	 * @param runEnd
	 *            The last run.
	 * @param byEpisode
	 *            If the performances are being combined by episode (in
	 *            intervals) or by regular CE interval.
	 */
	private long combineTempFiles(File performanceFile, int runEnd,
			long experimentStart) throws Exception {
		List<List<Float[]>> performances = new ArrayList<List<Float[]>>();
		float min = Float.MAX_VALUE;
		int minRun = -1;
		float max = -Float.MAX_VALUE;
		int maxRun = -1;
		double[] episodeLengths = new double[runEnd];
		double[] numSlots = new double[runEnd];
		long averageRunTime = 0;

		File combinedPerfFile = performanceFile;
		if (Config.getInstance().getGeneratorFile() != null) {
			combinedPerfFile = new File(performanceFile.getAbsolutePath()
					+ "greedy");
			ProgramArgument.PERFORMANCE_EPISODE_GAP
					.setDoubleValue(ProgramArgument.PERFORMANCE_TESTING_SIZE
							.intValue()
							* ProgramArgument.POLICY_REPEATS.intValue());
		}
		if (!combinedPerfFile.exists())
			combinedPerfFile.createNewFile();
		// For every performance file
		for (int i = 0; i < runEnd; i++) {
			File tempPerf = new File(Config.TEMP_FOLDER + "/" + performanceFile
					+ i);
			if (!Performance.readRawPerformanceFile(tempPerf, true)) {
				System.err.println("Error reading performance file.");
				return 0;
			}

			List<Float[]> thisRunPerformances = new ArrayList<Float[]>();
			performances.add(thisRunPerformances);

			// Run through the performances and place them in the matrix
			SortedMap<Integer, Float[]> runPerformances = Performance
					.getPerformanceArray();
			averageRunTime += Performance.getRunTime();
			Iterator<Integer> iter = runPerformances.keySet().iterator();
			Integer current = iter.next();
			Integer previous = null;
			int currentKeyframeEpisode = ProgramArgument.PERFORMANCE_EPISODE_GAP
					.intValue();
			// Run through the performances, using linear interpolation to
			// get estimates of the performance at a given interval.
			do {
				// If the current segment is further along than the current
				// value, advance to the next value.
				while (currentKeyframeEpisode > current) {
					previous = current;
					if (iter.hasNext())
						current = iter.next();
					else
						break;
				}

				// If the keyframe isn't up to the first episode, just use
				// the current value
				Float[] episodePerformance = runPerformances.get(current);
				if (previous == null) {
					// Add to the previous value.
					thisRunPerformances.add(episodePerformance);
				} else {
					// Interpolate from the previous value to the current
					// one.
					Float[] interpolatedPerformance = new Float[episodePerformance.length];
					if (previous == current) {
						interpolatedPerformance = episodePerformance;
					} else {
						Float[] prevPerformance = runPerformances.get(previous);

						for (int j = 0; j < episodePerformance.length; j++) {
							Float currPerf = episodePerformance[j];
							Float prevPerf = prevPerformance[j];
							// Adjust for null elites
							if (j == PerformanceDetails.ELITEMAX.ordinal()
									|| j == PerformanceDetails.ELITEMEAN
											.ordinal()) {
								if (currPerf == null)
									currPerf = episodePerformance[PerformanceDetails.MEAN
											.ordinal()];
								if (prevPerf == null)
									prevPerf = prevPerformance[PerformanceDetails.MEAN
											.ordinal()];
							}

							if (currPerf == null || prevPerf == null)
								interpolatedPerformance[j] = null;
							else
								interpolatedPerformance[j] = (currPerf - prevPerf)
										* (1f * (currentKeyframeEpisode - previous) / (current - previous))
										+ prevPerf;
						}
					}

					// Add to the performances
					thisRunPerformances.add(interpolatedPerformance);
				}

				// To the next increment
				currentKeyframeEpisode += ProgramArgument.PERFORMANCE_EPISODE_GAP
						.intValue();
			} while (currentKeyframeEpisode <= runPerformances.lastKey());
			Float[] lastPerf = runPerformances.get(runPerformances.lastKey());
			thisRunPerformances.add(lastPerf);
			System.out
					.println(runPerformances.get(runPerformances.lastKey())[PerformanceDetails.MEAN
							.ordinal()]);

			// Find min or max runs
			float runVal = runPerformances.get(runPerformances.lastKey())[PerformanceDetails.MEAN
					.ordinal()];
			if (runVal < min) {
				min = runVal;
				minRun = i;
			}
			if (runVal > max) {
				max = runVal;
				maxRun = i;
			}
			episodeLengths[i] = runPerformances.lastKey();
		}

		// Calculate the average and print out the stats
		FileWriter writer = new FileWriter(combinedPerfFile);
		BufferedWriter buf = new BufferedWriter(writer);
		Config.writeFileHeader(buf, Config.getInstance().getGoal());

		buf.write("Episode\tAverage\tSD\tMin\tMax\tElite-Average\tElite-SD\tNumSlots\tSlots-SD\tNumRules\tRules-SD\n");
		boolean moreEpisodes = true;
		int index = 0;
		Mean mean = new Mean();
		StandardDeviation sd = new StandardDeviation();
		while (moreEpisodes) {
			moreEpisodes = false;
			// Compile the array of performances for the given index
			double[][] performanceArray = new double[PerformanceDetails
					.values().length][performances.size()];
			double maxVal = 0;
			double minVal = 0;
			for (int run = 0; run < performances.size(); run++) {
				List<Float[]> runPerformanceList = performances.get(run);
				int thisIndex = Math.min(index, runPerformanceList.size() - 1);
				if (index < runPerformanceList.size() - 1)
					moreEpisodes = true;
				Float[] performanceDetails = runPerformanceList.get(thisIndex);
				for (int j = 0; j < performanceDetails.length; j++) {
					if (performanceDetails[j] != null)
						performanceArray[j][run] = performanceDetails[j];
				}

				// Max and min
				if (run == minRun)
					minVal = performanceArray[PerformanceDetails.MEAN.ordinal()][run];
				if (run == maxRun)
					maxVal = performanceArray[PerformanceDetails.MEAN.ordinal()][run];
			}

			// Find the statistics
			int episodeNum = (index + 1)
					* ProgramArgument.PERFORMANCE_EPISODE_GAP.intValue();
			buf.write(episodeNum
					+ "\t"
					+ mean.evaluate(performanceArray[PerformanceDetails.MEAN
							.ordinal()])
					+ "\t"
					+ sd.evaluate(performanceArray[PerformanceDetails.MEAN
							.ordinal()])
					+ "\t"
					+ minVal
					+ "\t"
					+ maxVal
					+ "\t"
					+ mean.evaluate(performanceArray[PerformanceDetails.ELITEMEAN
							.ordinal()])
					+ "\t"
					+ sd.evaluate(performanceArray[PerformanceDetails.ELITEMEAN
							.ordinal()])
					+ "\t"
					+ mean.evaluate(performanceArray[PerformanceDetails.NUMSLOTS
							.ordinal()])
					+ "\t"
					+ sd.evaluate(performanceArray[PerformanceDetails.NUMSLOTS
							.ordinal()])
					+ "\t"
					+ mean.evaluate(performanceArray[PerformanceDetails.NUMRULES
							.ordinal()])
					+ "\t"
					+ sd.evaluate(performanceArray[PerformanceDetails.NUMRULES
							.ordinal()]) + "\n");
			index++;
		}

		averageRunTime /= runEnd;
		buf.write("Average Run Time: " + toTimeFormat(averageRunTime) + "\n");

		// Write the average episode length
		buf.write("\nAverage episode length: " + mean.evaluate(episodeLengths)
				+ " +- " + sd.evaluate(episodeLengths) + "\n");
		buf.write("\nAverage num slots: " + mean.evaluate(numSlots) + " +- "
				+ sd.evaluate(numSlots) + "\n");

		buf.close();
		writer.close();
		return averageRunTime;
	}

	/**
	 * Run a single episode in the given environment.
	 */
	protected void episode() {
		// Form the initial observations and feed them to the agent.
		// Ensure that the goal isn't met immediately
		RRLObservations observations = environment_.startEpisode();
		while (observations.isTerminal() == RRLEnvironment.TERMINAL_WIN)
			observations = environment_.startEpisode();
		RRLActions actions = agent_.startEpisode(observations);

		// Continue through the episode until it's over, or the agent calls it
		// over.
		while (true) {
			// Compile observations
			observations = environment_.step(actions.getActions());
			if (observations.isTerminal() != RRLEnvironment.NOT_TERMINAL)
				break;

			// Determine actions
			actions = agent_.stepEpisode(observations);
			if (actions.isEarlyExit())
				break;
		}

		agent_.endEpisode(observations);
	}

	/**
	 * Initialise the agent to be used in this experiment.
	 * 
	 * @return The environment to be used in this experiment.
	 */
	protected RRLAgent setAgent() {
		return new CERRLA();
	}

	/**
	 * Perform one run, recording statistics as it goes.
	 * 
	 * @param finiteEpisodes
	 */
	public void run(int runIndex, int finiteEpisodes) {
		// Initialise the agent and environment
		random_ = new Random(runIndex);
		System.out.println("Goal: " + StateSpec.getInstance().getGoalState());

		agent_.initialise(runIndex);
		environment_.initialise(runIndex, Config.getInstance().getExtraArgs());

		if (ProgramArgument.TESTING.booleanValue()
				|| Config.getInstance().getGeneratorFile() != null) {
			agent_.freeze(true);
			environment_.freeze(true);
		}

		// Continue to run episodes until either the agent states it is
		// converged, or a finite pre-specified number of episodes have passed.
		if (finiteEpisodes == -1)
			finiteEpisodes = Integer.MAX_VALUE;
		while (!agent_.isLearningComplete()
				|| ProgramArgument.TESTING.booleanValue()) {
			episode();

			if (Config.getInstance().getGeneratorFile() == null) {
				int splitBuffer = (int) ((1 - ProgramArgument.SPLIT_BUFFER
						.doubleValue()) * finiteEpisodes);
				if (agent_.getNumEpisodes() >= splitBuffer)
					agent_.setSpecialisations(false);
				if (agent_.getNumEpisodes()
						+ ProgramArgument.POLICY_REPEATS.intValue() >= finiteEpisodes) {
					agent_.freeze(true);
					environment_.freeze(true);
				}
			}
		}

		agent_.cleanup();
		environment_.cleanup();
		environment_.freeze(false);

		System.gc();
	}

	/**
	 * Run multiple runs, each with an optional finite number of episodes.
	 * 
	 * @param numRuns
	 *            The number of runs to run.
	 * @param finiteEpisodes
	 *            A finite number of episodes for each run to go through (or -1
	 *            if infinite).
	 */
	public void runExperiment() {
		StateSpec.initInstance(Config.getInstance().getEnvironmentClass(),
				Config.getInstance().getGoalString());
		Config.getInstance().setGoal(StateSpec.getInstance().getGoalName());

		long experimentStart = System.currentTimeMillis();

		// Determine the initial run (as previous runs may have already been
		// done in a previous experiment)
		int[] startPoint = checkFiles(Config.getInstance()
				.getRepetitionsStart());
		int run = startPoint[0];

		// Load existing runs and start from there.
		for (int i = run; i < Config.getInstance().getRepetitionsEnd(); i++) {
			if (i > run)
				StateSpec.reinitInstance();
			run(i, Config.getInstance().getMaxEpisodes());
			Config.getInstance().removeSerialised();
		}

		// Compile the files
		long runTime = System.currentTimeMillis() - experimentStart;
		if (Config.getInstance().getRepetitionsStart() == 0
				&& !ProgramArgument.TESTING.booleanValue()) {
			try {
				runTime = combineTempFiles(Config.getInstance()
						.getPerformanceFile(), Config.getInstance()
						.getRepetitionsEnd(), experimentStart);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		System.out.println("Average learning time: " + toTimeFormat(runTime));
//		playSoundComplete();
	}

	private void playSoundComplete() {
		int oneTick = 300;
		try {
			Toolkit.getDefaultToolkit().beep();
			Thread.sleep(oneTick);
			for (int i = 0; i < 3; i++) {
				Toolkit.getDefaultToolkit().beep();
				Thread.sleep((int) (oneTick / 3.0));
			}
			Toolkit.getDefaultToolkit().beep();
			Thread.sleep(oneTick);
			Toolkit.getDefaultToolkit().beep();
			Thread.sleep(2 * oneTick);
			Toolkit.getDefaultToolkit().beep();
			Thread.sleep(oneTick);
			Toolkit.getDefaultToolkit().beep();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * The main method to get the experiment running.
	 * 
	 * @param args
	 *            The config filename defining environment + any other args.
	 */
	public static void main(String[] args) {
		RRLExperiment experiment = new RRLExperiment(args);
		experiment.runExperiment();
	}

	/**
	 * Simple tool for converting long to a string of time.
	 * 
	 * @param time
	 *            The time in millis.
	 * @return A string representing the time.
	 */
	public static String toTimeFormat(long time) {
		String timeString = time / (1000 * 60 * 60) + ":"
				+ (time / (1000 * 60)) % 60 + ":" + (time / 1000) % 60;
		return timeString;
	}
}
