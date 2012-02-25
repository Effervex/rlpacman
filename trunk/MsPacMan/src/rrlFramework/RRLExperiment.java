package rrlFramework;

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
import cerrla.PerformanceReader;
import cerrla.ProgramArgument;

/**
 * An abstract experiment class for running an RRL experiment.
 * 
 * @author Sam Sarjant
 */
public class RRLExperiment {
	/** The random number generator. */
	public static Random random_ = new Random(0);

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
		File lastPerf = null;
		File tempPerf = new File(Config.TEMP_FOLDER + "/"
				+ Config.getInstance().getPerformanceFile().getName() + run);
		while (tempPerf.exists()) {
			run++;
			lastPerf = tempPerf;
			tempPerf = new File(Config.TEMP_FOLDER + "/"
					+ Config.getInstance().getPerformanceFile().getName() + run);
		}

		// If there aren't any performance files, return 0,0
		if (lastPerf == null)
			return result;

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
	private void combineTempFiles(File performanceFile, int runEnd,
			long experimentStart) throws Exception {
		List<List<Float>> performances = new ArrayList<List<Float>>();
		float min = Float.MAX_VALUE;
		int minRun = -1;
		float max = -Float.MAX_VALUE;
		int maxRun = -1;
		double[] episodeLengths = new double[runEnd];
		double[] numSlots = new double[runEnd];

		if (!performanceFile.exists())
			performanceFile.createNewFile();
		// For every performance file
		for (int i = 0; i < runEnd; i++) {
			File tempPerf = new File(Config.TEMP_FOLDER + "/" + performanceFile
					+ i);
			if (!PerformanceReader.readPerformanceFile(tempPerf, true)) {
				System.err.println("Error reading performance file.");
				return;
			}

			List<Float> thisRunPerformances = new ArrayList<Float>();
			performances.add(thisRunPerformances);

			// Run through the performances and place them in the matrix
			SortedMap<Integer, Float> runPerformances = PerformanceReader
					.getPerformanceArray();
			numSlots[i] = PerformanceReader.getNumSlots();
			Iterator<Integer> iter = runPerformances.keySet().iterator();
			Integer current = iter.next();
			Integer previous = null;
			int currentKeyframeEpisode = 0;
			// Run through the performances, using linear interpolation to
			// get estimates of the performance at a given interval.
			float interpolated = 0;
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
				if (previous == null) {
					// Add to the previous value.
					thisRunPerformances.add(runPerformances.get(current));
				} else {
					// Interpolate from the previous value to the current
					// one.
					interpolated = 0;
					if (previous == current) {
						interpolated = runPerformances.get(current);
					} else {
						float prevVal = (previous == 0) ? 0 : runPerformances
								.get(previous);
						float currentVal = runPerformances.get(current);
						interpolated = (currentVal - prevVal)
								* (1f * (currentKeyframeEpisode - previous) / (current - previous))
								+ prevVal;
					}

					// Add to the performances
					thisRunPerformances.add(interpolated);
				}

				// To the next increment
				currentKeyframeEpisode += ProgramArgument.PERFORMANCE_EPISODE_GAP
						.intValue();
			} while (currentKeyframeEpisode <= runPerformances.lastKey());
			thisRunPerformances.add(runPerformances.get(runPerformances
					.lastKey()));
			System.out.println(runPerformances.get(runPerformances.lastKey()));

			// Find min or max runs
			float runVal = runPerformances.get(runPerformances.lastKey());
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
		FileWriter writer = new FileWriter(performanceFile);
		BufferedWriter buf = new BufferedWriter(writer);
		Config.writeFileHeader(buf, Config.getInstance().getGoal());

		buf.write("Episode\tAverage\tSD\tMin\tMax\n");
		boolean moreEpisodes = true;
		int index = 0;
		Mean mean = new Mean();
		StandardDeviation sd = new StandardDeviation();
		while (moreEpisodes) {
			moreEpisodes = false;
			// Compile the array of performances for the given index
			double[] performanceArray = new double[performances.size()];
			double maxVal = 0;
			double minVal = 0;
			for (int run = 0; run < performanceArray.length; run++) {
				List<Float> runPerformanceList = performances.get(run);
				int thisIndex = Math.min(index, runPerformanceList.size() - 1);
				if (index < runPerformanceList.size() - 1)
					moreEpisodes = true;
				performanceArray[run] = runPerformanceList.get(thisIndex);

				// Max and min
				if (run == minRun)
					minVal = performanceArray[run];
				if (run == maxRun)
					maxVal = performanceArray[run];
			}

			// Find the statistics
			int episodeNum = index
					* ProgramArgument.PERFORMANCE_EPISODE_GAP.intValue();
			buf.write(episodeNum + "\t" + mean.evaluate(performanceArray)
					+ "\t" + sd.evaluate(performanceArray) + "\t" + minVal
					+ "\t" + maxVal + "\n");
			index++;
		}

		buf.write("Total Run Time: "
				+ toTimeFormat(System.currentTimeMillis() - experimentStart)
				+ "\n");

		// Write the average episode length
		buf.write("\nAverage episode length: " + mean.evaluate(episodeLengths)
				+ " +- " + sd.evaluate(episodeLengths) + "\n");
		buf.write("\nAverage num slots: " + mean.evaluate(numSlots) + " +- "
				+ sd.evaluate(numSlots) + "\n");

		buf.close();
		writer.close();
	}

	/**
	 * Run a single episode in the given environment.
	 */
	protected void episode() {
		// Form the initial observations and feed them to the agent.
		// Ensure that the goal isn't met immediately
		RRLObservations observations = environment_.startEpisode();
		while (observations.isTerminal())
			observations = environment_.startEpisode();
		RRLActions actions = agent_.startEpisode(observations);

		// Continue through the episode until it's over, or the agent calls it
		// over.
		while (true) {
			// Compile observations
			observations = environment_.step(actions.getActions());
			if (observations.isTerminal())
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

		// Continue to run episodes until either the agent states it is
		// converged, or a finite pre-specified number of episodes have passed.
		if (finiteEpisodes == -1)
			finiteEpisodes = Integer.MAX_VALUE;
		int episodeCount = 0;
		while (!agent_.isLearningComplete()) {
			episode();

			episodeCount++;
			if (episodeCount >= finiteEpisodes)
				agent_.freeze(true);
		}

		agent_.cleanup();
		environment_.cleanup();
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
			run(i, Config.getInstance().getMaxEpisodes());
		}

		// Compile the files
		if (Config.getInstance().getRepetitionsStart() == 0
				&& !ProgramArgument.TESTING.booleanValue()) {
			try {
				combineTempFiles(Config.getInstance().getPerformanceFile(),
						Config.getInstance().getRepetitionsEnd(),
						experimentStart);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		System.out.println("Total learning time: "
				+ toTimeFormat(System.currentTimeMillis() - experimentStart));
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
