package rrlFramework;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rlcommunity.rlglue.codec.RLGlue;

import relationalFramework.GoalCondition;
import relationalFramework.StateSpec;

import util.Pair;
import cerrla.PolicyGenerator;
import cerrla.ProgramArgument;

/**
 * A singleton class containing configuration details loaded in at the start of
 * learning.
 * 
 * @author Sam Sarjant
 */
public class Config {
	/** The folder to store the temp files. */
	public static final File TEMP_FOLDER = new File("temp" + File.separatorChar);

	/** The marker for the end of a successfully completed performance file. */
	public static final String END_PERFORMANCE = "<--END-->";

	/**
	 * An optional comment to append to the beginning of performance and elite
	 * files.
	 */
	private String comment_;
	/** The class prefix for the environment. */
	private String environmentClass_;
	/** The time that the experiment started. */
	private long experimentStart_;
	/** The extra arguments to message the environment. */
	private String[] extraArgs_;
	/** The number of episodes to run. */
	private int maxEpisodes_;
	/** The performance output file. */
	private File performanceFile_;
	/** The best policy found output file. */
	private File elitesFile_;
	/** The primary goal of the experiment. */
	private GoalCondition mainGoal_;
	/** The last run number. */
	private int repetitionsEnd_ = 1;
	/** The first run number. */
	private int repetitionsStart_ = 0;
	/** If optional rules are seeded from file. */
	private File ruleFile_;

	/** The loaded serializable file. */
	private File serializedFile_;

	/** The singleton instance. */
	private static Config instance_;

	/**
	 * Initialising the configuration details.
	 */
	private Config(String[] args) {
		try {
			if (args.length == 0)
				throw new Exception("No environment config file provided!");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		File argumentFile = new File(args[0]);
		ProgramArgument.loadArgs();
		ProgramArgument.saveArgsFile();

		// Read the arguments in from file.
		try {
			FileReader reader = new FileReader(argumentFile);
			BufferedReader bf = new BufferedReader(reader);

			environmentClass_ = bf.readLine();
			String[] repetitionsStr = bf.readLine().split("-");
			// Num repetitions specified by a range for seeding random number.
			Integer repetitionsStart = 0;
			Integer repetitionsEnd = Integer.parseInt(repetitionsStr[0]);
			if (repetitionsStr.length == 2) {
				repetitionsStart = repetitionsEnd;
				repetitionsEnd = Integer.parseInt(repetitionsStr[1]);
			}
			Integer episodes = Integer.parseInt(bf.readLine());

			// Attempt to read in given filenames
			String elitesFile = bf.readLine();
			String performanceFile = null;
			String extraArgs = null;
			// If there are file paths, use them
			if (elitesFile != null && elitesFile.endsWith(".txt")) {
				performanceFile = bf.readLine();
				extraArgs = bf.readLine();
			} else {
				extraArgs = elitesFile;
				elitesFile = null;
			}

			ArrayList<String> extraArgsList = new ArrayList<String>();
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

			ArrayList<String> handledArgs = new ArrayList<String>();
			for (int i = 1; i < args.length; i++) {
				if (args[i].equals("-d"))
					// Enable debug mode
					RRLExperiment.debugMode_ = true;
				else if (args[i].equals("-e"))
					// Set the environment to experiment mode
					RLGlue.RL_env_message("-e");
				else if (args[i].equals("-s")) {
					i++;
					serializedFile_ = new File(args[i]);
				} else if (args[i].equals("-ruleFile")) {
					i++;
					ruleFile_ = new File(args[i]);
				} else {
					// Handle the argument
					Pair<Integer, String> handled = ProgramArgument.handleArg(
							i, args);
					i = handled.objA_;
					handledArgs.add(handled.objB_);
				}
			}

			initialise(environmentClass_, repetitionsStart, repetitionsEnd,
					episodes, elitesFile, performanceFile, handledArgs,
					extraArgsList.toArray(new String[extraArgsList.size()]));
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	 * @param handledArgs
	 *            The handled arguments added to the command line.
	 * @param extraArgs
	 *            The extra arguments for the environment to take.
	 */
	private void initialise(String environmentClass, int repetitionsStart,
			int repetitionsEnd, int episodeCount, String elitesFile,
			String performanceFile, ArrayList<String> handledArgs,
			String[] extraArgs) {
		repetitionsStart_ = repetitionsStart;
		repetitionsEnd_ = repetitionsEnd;
		maxEpisodes_ = episodeCount;

		// Create temp folder
		try {
			TEMP_FOLDER.mkdir();
		} catch (Exception e) {
			e.printStackTrace();
		}
		extraArgs_ = extraArgs;
		for (String extraArg : extraArgs)
			if (extraArg.startsWith("goal"))
				mainGoal_ = new GoalCondition(extraArg.substring(5));

		// If the elites and performance file are null, generate filenames
		if (elitesFile == null || performanceFile == null) {
			String[] fileNames = generateFileNames(handledArgs);
			elitesFile = fileNames[0];
			performanceFile = fileNames[1];
		}

		elitesFile_ = new File(elitesFile);
		performanceFile_ = new File(performanceFile);
	}

	/**
	 * Generates the filenames for the elites and performance files.
	 * 
	 * @param handledArgs
	 *            The args added to the run begin.
	 * @return The filenames for elites and performance.
	 */
	private String[] generateFileNames(ArrayList<String> handledArgs) {
		String[] filenames = new String[2];
		StringBuffer buffer = new StringBuffer();
		// First part: date
		DateFormat dateFormat = new SimpleDateFormat("yyMMdd");
		Date date = new Date();
		buffer.append(dateFormat.format(date));
		// Environment name
		buffer.append(StateSpec.getInstance().getEnvironmentName());

		// Last part: params
		StringBuffer argsBuffer = new StringBuffer();
		for (String arg : handledArgs)
			argsBuffer.append(arg);
		if (handledArgs.isEmpty())
			argsBuffer.append("Control");

		File tempFile = null;
		// Modify filename if the file already exists
		int i = 0;
		do {
			String extension = ".txt";
			if (i > 0)
				extension = "." + i + ".txt";
			filenames[0] = buffer.toString() + "Elites" + argsBuffer.toString()
					+ extension;
			String goalName = StateSpec.getInstance().getGoalName();
			filenames[1] = buffer.toString()
					+ goalName.substring(0, 1).toUpperCase()
					+ goalName.substring(1) + argsBuffer.toString() + extension;

			// Check if the performance filename exists.
			tempFile = new File(TEMP_FOLDER, filenames[1] + "0");
			i++;
		} while (tempFile.exists());

		return filenames;
	}

	/**
	 * Creates a new instance of the configuration, using the given args to set
	 * various options.
	 * 
	 * @param args
	 *            The args to load in.
	 */
	public static void newInstance(String[] args) {
		instance_ = new Config(args);
	}

	/**
	 * Gets the config instance.
	 * 
	 * @return The configuration instance.
	 */
	public static Config getInstance() {
		return instance_;
	}

	public GoalCondition getGoal() {
		return mainGoal_;
	}

	public File getPerformanceFile() {
		return performanceFile_;
	}

	public File getSerializedFile() {
		return serializedFile_;
	}

	public String getComment() {
		return comment_;
	}

	public File getElitesFile() {
		return elitesFile_;
	}

	public int getMaxEpisodes() {
		return maxEpisodes_;
	}

	public int getRepetitionsStart() {
		return repetitionsStart_;
	}

	public int getRepetitionsEnd() {
		return repetitionsEnd_;
	}

	public int getNumRepetitions() {
		return repetitionsEnd_ - repetitionsStart_;
	}

	/**
	 * Writes the file header to a buffer.
	 * 
	 * @param buf
	 *            The buffered writer.
	 * @param goal
	 *            The goal of the behaviour.
	 * @throws IOException
	 *             The exception.
	 */
	public static void writeFileHeader(BufferedWriter buf, GoalCondition goal)
			throws IOException {
		// Program Arguments
		buf.write("---PROGRAM ARGUMENTS---\n");
		ProgramArgument.saveArgs(buf, true);
		buf.write("-----------------------\n");
		buf.write("GOAL (" + goal + "): "
				+ StateSpec.getInstance().getGoalState() + "\n\n");

		// Comments
		if (Config.getInstance().getComment() != null)
			buf.write(Config.getInstance().getComment() + "\n");
	}

	public String[] getExtraArgs() {
		return extraArgs_;
	}

	public String getEnvironmentClass() {
		return environmentClass_;
	}

	public void setGoal(String goalName) {
		if (mainGoal_ == null || !mainGoal_.toString().equals(goalName))
			mainGoal_ = new GoalCondition(goalName);
	}

	public String getGoalString() {
		if (mainGoal_ == null)
			return null;
		return mainGoal_.toString();
	}
}
