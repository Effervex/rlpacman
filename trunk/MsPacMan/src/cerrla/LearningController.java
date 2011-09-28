package cerrla;

import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.rlcommunity.rlglue.codec.RLGlue;

/**
 * The cross entropy algorithm implementation.
 * 
 * @author Sam Sarjant
 */
public class LearningController {
	/** The folder to store the temp files. */
	public static final File TEMP_FOLDER = new File("temp" + File.separatorChar);

	/** The marker for the end of a successfully completed performance file. */
	public static final String END_PERFORMANCE = "<--END-->";
	/** The internal prefix for messages to the agent regarding internal goal. */
	public static final String INTERNAL_PREFIX = "internal";
	/**
	 * An optional comment to append to the beginning of performance and elite
	 * files.
	 */
	private String comment_;

	/** The time that the experiment started. */
	private long experimentStart_;
	/** The extra arguments to message the environment. */
	private String[] extraArgs_;
	/** The number of episodes to run. */
	private int maxEpisodes_;
	/** The maximum number of steps the agent can take. */
	private int maxSteps_;
	/** If the performance is saved by episode or by CE iteration. */
	private boolean performanceByEpisode_ = true;
	/** The performance output file. */
	private File performanceFile_;
	/** The best policy found output file. */
	private File elitesFile_;
	/** The last run number. */
	private int repetitionsEnd_ = 1;
	/** The first run number. */
	private int repetitionsStart_ = 0;
	/** If optional rules are seeded from file. */
	private File ruleFile_;

	/** The loaded serializable file. */
	private File serializedFile_;

	/**
	 * A constructor for initialising the cross-entropy generators and
	 * experiment parameters from an argument file.
	 * 
	 * @param argumentFile
	 *            The file containing the arguments.
	 */
	public LearningController(String[] args) {
		File argumentFile = new File(args[0]);
		ProgramArgument.loadArgs();
		ProgramArgument.saveArgsFile();

		// Read the arguments in from file.
		try {
			FileReader reader = new FileReader(argumentFile);
			BufferedReader bf = new BufferedReader(reader);

			String environmentClass = bf.readLine();
			String[] repetitionsStr = bf.readLine().split("-");
			// Num repetitions specified by a range for seeding random number.
			Integer repetitionsStart = 0;
			Integer repetitionsEnd = Integer.parseInt(repetitionsStr[0]);
			if (repetitionsStr.length == 2) {
				repetitionsStart = repetitionsEnd;
				repetitionsEnd = Integer.parseInt(repetitionsStr[1]);
			}
			Integer episodes = Integer.parseInt(bf.readLine());
			String elitesFile = bf.readLine();
			String performanceFile = bf.readLine();

			ArrayList<String> extraArgsList = new ArrayList<String>();
			String extraArgs = bf.readLine();
			if (extraArgs != null) {
				Pattern p = Pattern.compile("((\".+?\")|\\w+)");
				Matcher m = p.matcher(extraArgs);
				while (m.find()) {
					String extraArg = m.group().replaceAll("\"", "");
					// Performance file comment
					if (extraArg.charAt(0) == '%') {
						comment_ = extraArg;
						System.out.println(comment_);
					} else
						extraArgsList.add(extraArg);
				}
			}

			bf.close();
			reader.close();

			initialise(environmentClass, repetitionsStart, repetitionsEnd,
					episodes, elitesFile, performanceFile,
					extraArgsList.toArray(new String[extraArgsList.size()]));

			for (int i = 1; i < args.length; i++) {
				if (args[i].equals("-d"))
					// Enable debug mode
					PolicyGenerator.debugMode_ = true;
				else if (args[i].equals("-e"))
					// Set the environment to experiment mode
					RLGlue.RL_env_message("-e");
				else if (args[i].equals("-m")) {
					Module.saveAtEnd_ = true;
				} else if (args[i].equals("-s")) {
					i++;
					serializedFile_ = new File(args[i]);
				} else if (args[i].equals("-ruleFile")) {
					i++;
					ruleFile_ = new File(args[i]);
				} else {
					// Handle the argument
					i = ProgramArgument.handleArg(i, args);
				}
			}

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
		File tempPerf = new File(TEMP_FOLDER + "/" + performanceFile_.getName()
				+ run);
		while (tempPerf.exists()) {
			run++;
			lastPerf = tempPerf;
			tempPerf = new File(TEMP_FOLDER + "/" + performanceFile_.getName()
					+ run);
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
				if (input.equals(END_PERFORMANCE)) {
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
	private void combineTempFiles(int runEnd, boolean byEpisode)
			throws Exception {
		List<List<Float>> performances = new ArrayList<List<Float>>();
		float min = Float.MAX_VALUE;
		int minRun = -1;
		float max = -Float.MAX_VALUE;
		int maxRun = -1;
		double[] episodeLengths = new double[runEnd];

		if (!performanceFile_.exists())
			performanceFile_.createNewFile();
		// For every performance file
		for (int i = 0; i < runEnd; i++) {
			File tempPerf = new File(TEMP_FOLDER + "/" + performanceFile_ + i);
			if (!PerformanceReader.readPerformanceFile(tempPerf, true)) {
				System.err.println("Error reading performance file.");
				return;
			}

			List<Float> thisRunPerformances = new ArrayList<Float>();
			performances.add(thisRunPerformances);

			// Run through the performances and place them in the matrix
			SortedMap<Integer, Float> runPerformances = PerformanceReader
					.getPerformanceArray();
			if (byEpisode) {
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
							float prevVal = (previous == 0) ? 0
									: runPerformances.get(previous);
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
				System.out.println(runPerformances.get(runPerformances
						.lastKey()));
			} else {
				// Take the values directly from the run performances
				for (Integer key : runPerformances.keySet()) {
					thisRunPerformances.add(runPerformances.get(key));
				}
			}

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
		FileWriter writer = new FileWriter(performanceFile_);
		BufferedWriter buf = new BufferedWriter(writer);
		writeFileHeader(buf);

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
			int episodeNum = (int) ((byEpisode) ? index
					* ProgramArgument.PERFORMANCE_EPISODE_GAP.doubleValue()
					: index + 1);
			buf.write(episodeNum + "\t" + mean.evaluate(performanceArray)
					+ "\t" + sd.evaluate(performanceArray) + "\t" + minVal
					+ "\t" + maxVal + "\n");
			index++;
		}

		buf.write("Total Run Time: "
				+ toTimeFormat(System.currentTimeMillis() - experimentStart_)
				+ "\n");

		// Write the average episode length
		buf.write("\nAverage episode length: " + mean.evaluate(episodeLengths)
				+ " +- " + sd.evaluate(episodeLengths) + "\n");

		buf.close();
		writer.close();
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

	/**
	 * A constructor for the typical arguments and a randomly generated rule
	 * base.
	 * 
	 * @param environmentClass
	 *            The name of the environment class files.
	 * @param repetitionsStart
	 *            The first run number seed.
	 * @param repetitionsEnd
	 *            The last run number seed.
	 * @param episodeCount
	 *            The number of episodes to perform.
	 * @param elitesFile
	 *            The output file for the best policy.
	 * @param performanceFile
	 *            The output file for the agent's performance.
	 * @param extraArgs
	 *            The extra arguments for the environment to take.
	 */
	private void initialise(String environmentClass, int repetitionsStart,
			int repetitionsEnd, int episodeCount, String elitesFile,
			String performanceFile, String[] extraArgs) {
		repetitionsStart_ = repetitionsStart;
		repetitionsEnd_ = repetitionsEnd;
		maxEpisodes_ = episodeCount;

		elitesFile_ = new File(elitesFile);
		performanceFile_ = new File(performanceFile);

		// Create temp folder
		try {
			TEMP_FOLDER.mkdir();
		} catch (Exception e) {
			e.printStackTrace();
		}
		extraArgs_ = extraArgs;
		String goalArg = null;
		for (String extraArg : extraArgs)
			if (extraArg.startsWith("goal"))
				goalArg = extraArg.substring(5);

		// Initialise the state spec.
		StateSpec.initInstance(environmentClass, goalArg);
	}

	@SuppressWarnings("unused")
	private void printRuleWorths(PolicyGenerator localPolicy) {
		System.out.println("\tRULE WORTHS");
		Comparator<RelationalRule> comp = new Comparator<RelationalRule>() {

			@Override
			public int compare(RelationalRule o1, RelationalRule o2) {
				// Bigger is better
				if (o1.getInternalMean() > o2.getInternalMean())
					return -1;
				if (o1.getInternalMean() < o2.getInternalMean())
					return 1;
				// Smaller SD is better
				if (o1.getInternalSD() < o2.getInternalSD())
					return -1;
				if (o1.getInternalSD() > o2.getInternalSD())
					return 1;
				// Else, just return a hashcode equality relation
				if (o1.hashCode() < o2.hashCode())
					return -1;
				if (o1.hashCode() > o2.hashCode())
					return 1;
				return 0;
			}
		};
		SortedSet<RelationalRule> sortedRules = new TreeSet<RelationalRule>(
				comp);
		for (Slot slot : localPolicy.getGenerator()) {
			for (RelationalRule rule : slot.getGenerator()) {
				sortedRules.add(rule);
			}
		}

		for (RelationalRule rule : sortedRules)
			System.out.println(rule.toNiceString() + ": "
					+ rule.getInternalMean() + ((char) 177)
					+ rule.getInternalSD());
	}

	/**
	 * Runs the experiment through a number of iterations and saves the averaged
	 * performance.
	 * 
	 * @param runs
	 *            The number of runs to average the results over.
	 */
	public void runExperiment() {
		experimentStart_ = System.currentTimeMillis();

		// Initialise the environment/agent
		RLGlue.RL_init();
		for (String extraArg : extraArgs_)
			RLGlue.RL_env_message(extraArg);
		maxSteps_ = Integer.parseInt(RLGlue.RL_env_message("maxSteps"));
		System.out.println("Goal: " + StateSpec.getInstance().getGoalState());

		// Determine the initial run (as previous runs may have already been
		// done in a previous experiment)
		int[] startPoint = checkFiles(repetitionsStart_);
		int run = startPoint[0];

		// The ultra-outer loop, for averaging experiment results
		for (; run < repetitionsEnd_; run++) {
			// Initialise a new policy generator.
			PolicyGenerator localPolicy = null;
			if (serializedFile_ != null)
				localPolicy = PolicyGenerator
						.loadPolicyGenerator(serializedFile_);
			if (localPolicy == null) {
				if (serializedFile_ != null)
					System.err.println("Could not load " + serializedFile_
							+ "\nUsing new policy generator");
				localPolicy = new PolicyGenerator(run);
			}

			if (ruleFile_ != null)
				localPolicy.seedRules(ruleFile_);
			CrossEntropyRun cer = CrossEntropyRun
					.newInstance(localPolicy, this);
			cer.beginRun(run, false, maxSteps_, maxEpisodes_);

			if (ProgramArgument.TESTING.booleanValue())
				break;
		}

		Module.saveAllModules();

		RLGlue.RL_cleanup();

		if (repetitionsStart_ == 0 && !ProgramArgument.TESTING.booleanValue()) {
			try {
				combineTempFiles(repetitionsEnd_, performanceByEpisode_);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		System.out.println("Total learning time: "
				+ toTimeFormat(System.currentTimeMillis() - experimentStart_));
	}

	public String getComment() {
		return comment_;
	}

	public long getExperimentStart() {
		return experimentStart_;
	}

	public String[] getExtraArgs() {
		return extraArgs_;
	}

	public int getMaxEpisodes() {
		return maxEpisodes_;
	}

	public int getMaxSteps() {
		return maxSteps_;
	}

	public File getPerformanceFile() {
		return performanceFile_;
	}

	public File getElitesFile() {
		return elitesFile_;
	}

	public int getRepetitionsEnd() {
		return repetitionsEnd_;
	}

	public int getRepetitionsStart() {
		return repetitionsStart_;
	}

	public File getRuleFile() {
		return ruleFile_;
	}

	public File getSerializedFile() {
		return serializedFile_;
	}

	/**
	 * Writes the file header to a buffer.
	 * 
	 * @param buf
	 *            The buffered writer.
	 * @throws IOException
	 *             The exception.
	 */
	public static void writeFileHeader(BufferedWriter buf) throws IOException {
		buf.write("---PROGRAM ARGUMENTS---\n");
		ProgramArgument.saveArgs(buf, true);
		buf.write("-----------------------\n");
		buf.write("GOAL (" + StateSpec.getInstance().getGoalName() + "): "
				+ StateSpec.getInstance().getGoalState() + "\n\n");
	}

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments for the program.
	 */
	public static void main(String[] args) {
		LearningController controller = new LearningController(args);

		controller.runExperiment();
		System.exit(0);
	}
}
