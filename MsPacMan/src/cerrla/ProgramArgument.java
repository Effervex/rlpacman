package cerrla;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public enum ProgramArgument implements Serializable {
	ALPHA(0.6, "alpha", null, "Step size update"),
	BETA(0.01, "beta", null, "If KL sum updates are less than Beta * Alpha"),
	CHI(0.1, "chi", null, "The resampling percentage of average episode steps"),
	DYNAMIC_SLOTS(true, "dynamicSlots", null, "If the slots "
			+ "grow dynamically"),
	ENSEMBLE_EVALUATION(false, "ensembleEvaluation", null, "If using "
			+ "ensemble evaluation"),
	ENSEMBLE_SIZE(100, "ensembleSize", null, "The size of the ensemble"),
	INITIAL_ORDERING_SD(0.25, "initialSlotOrderingSD", null, "The SD of "
			+ "the slot order"),
	INITIAL_SLOT_MEAN(1, "initialSlotMean", "-slotProb", "The initial "
			+ "slot mu probabilities. -1 means use 1/D_S"),
	LOCAL_ALPHA(true, "localAlpha", null, "If updates are performed "
			+ "slot locally"),
	MIN_WEIGHTED_UPDATE(0.1, "minWeightedUpdate", null, "The minimal update "
			+ "when using weighted updates"),
	NUM_NUMERICAL_SPLITS(3, "numNumericalSplits", null, "The number of "
			+ "numerical splits"),
	NUM_PERFORMANCES_CONVERGED(3, "numPerformancesConverged", null, "If "
			+ "the average performance has remained the same for "
			+ "this many iterations"),
	NUM_UPDATES_CONVERGED(10, "numUpdatesConverged", null, "If KL sum "
			+ "updates remain below Beta for this many updates"),
	ONLY_GOAL_RULES(false, "onlyGoalRules", "-goalRules", "If the agent should "
			+ "only create rules with the goal condition in it"),
	PERFORMANCE_EPISODE_GAP(100, "performanceEpisodeGap", null, "The gap "
			+ "between measuring performances"),
	PERFORMANCE_TESTING_SIZE(100, "policyTestingSize", null, "Size of average "
			+ "performance sliding window"),
	POLICY_REPEATS(3, "policyRepeats", null, "Number of times "
			+ "policy is repeated"),
	RHO(0.05, "rho", null, "N_E's proportion of N"),
	SLOT_THRESHOLD(-1, "slotThreshold", null, "The slot splitting threshold. "
			+ "-1 means use |S|-1 threshold"),
	TEST_ITERATIONS(100, "testIterations", null, "Number of iterations "
			+ "to test the final testing for"),
	USE_MODULES(true, "useModules", null, "If using/learning modules"),
	WEIGHTED_UPDATES(false, "weightedUpdates", null, "If using "
			+ "weighted updates");

	public static final File ARG_FILE = new File("cerrlaArgs.txt");
	private Boolean booleanValue_;
	private String comment_;
	private Object defaultValue_;
	private Number numberValue_;
	private String name_;
	private String shortcut_;

	/**
	 * Constructor for numerical arguments.
	 * 
	 * @param val
	 *            The value of the number.
	 * @param name
	 *            The string name of the argument.
	 * @param shortcut
	 *            The string shortcut name of the argument.
	 * @param comment
	 *            The comment about the argument.
	 */
	private ProgramArgument(Object defaultVal, String name, String shortcut,
			String comment) {
		if (defaultVal instanceof Number) {
			numberValue_ = (Number) defaultVal;
			defaultValue_ = numberValue_;
		} else if (defaultVal instanceof Boolean) {
			booleanValue_ = (Boolean) defaultVal;
			defaultValue_ = booleanValue_;
		}
		name_ = name;
		shortcut_ = shortcut;
		comment_ = comment;
	}

	public boolean booleanValue() {
		return booleanValue_.booleanValue();
	}

	public double doubleValue() {
		return numberValue_.doubleValue();
	}

	public String getComment() {
		return comment_;
	}

	public Object getDefaultValue() {
		return defaultValue_;
	}

	public String getName() {
		return name_;
	}

	public String getShortcut() {
		return shortcut_;
	}

	public Object getValue() {
		if (numberValue_ != null)
			return numberValue_;
		if (booleanValue_ != null)
			return booleanValue_;
		return null;
	}

	public int intValue() {
		return numberValue_.intValue();
	}

	public void setBooleanValue(boolean booleanVal) {
		booleanValue_ = booleanVal;
	}

	public void setDoubleValue(double doubleVal) {
		numberValue_ = doubleVal;
	}

	public void setValue(String value) {
		if (numberValue_ != null)
			numberValue_ = Double.parseDouble(value);
		if (booleanValue_ != null)
			booleanValue_ = Boolean.parseBoolean(value);
	}

	/**
	 * Loads the program arguments from file.
	 * 
	 * @param argFile
	 *            The file to load the arguments from.
	 * @return
	 */
	public static void loadArgs() {
		if (!ARG_FILE.exists())
			return;
		
		Map<String, String> args = new HashMap<String, String>();
		try {
			FileReader fr = new FileReader(ARG_FILE);
			BufferedReader br = new BufferedReader(fr);

			String input = null;
			while ((input = br.readLine()) != null) {
				String[] split = input.split("%")[0].split("=");
				args.put(split[0], split[1].trim());
			}

			br.close();
			fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Run through the program arguments and assign any loaded values
		if (args.isEmpty())
			return;
		for (ProgramArgument pa : ProgramArgument.values()) {
			String value = null;
			if (args.containsKey(pa.name_))
				value = args.get(pa.name_);
			if (args.containsKey(pa.shortcut_))
				value = args.get(pa.shortcut_);

			// Parse value
			pa.setValue(value);
		}
	}

	/**
	 * Saves the arguments into a file if the file doesn't already exist.
	 */
	public static void saveArgsFile() {
		try {
			if (!ARG_FILE.exists())
				ARG_FILE.createNewFile();

			FileWriter fw = new FileWriter(ARG_FILE);
			BufferedWriter bw = new BufferedWriter(fw);

			saveArgs(bw);

			bw.close();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Saves the arguments into a give {@link BufferedWriter}.
	 * 
	 * @param bw
	 *            The buffered writer.
	 */
	public static void saveArgs(BufferedWriter bw) throws IOException {
		for (ProgramArgument pa : ProgramArgument.values()) {
			if (!pa.getValue().equals(pa.getDefaultValue()))
				bw.write(pa.getName() + "=" + pa.getValue()
						+ "\t\t\t% -----MODIFIED----- % " + pa.getComment() + "\n");
			else
				bw.write(pa.getName() + "=" + pa.getValue() + "\t\t\t% "
						+ pa.getComment() + "\n");
		}
	}

	/**
	 * Handles a program argument not caught by the algorithm earlier.
	 * 
	 * @param i
	 *            The current index.
	 * @param args
	 *            The arguments given at command-line.
	 * @return The index after handling.
	 */
	public static int handleArg(int i, String[] args) {
		if (args[i].equals("-slotProb")) {
			i++;
			if (args[i].equals("dynamic"))
				INITIAL_SLOT_MEAN.setDoubleValue(-1);
			else
				INITIAL_SLOT_MEAN.setDoubleValue(Double.parseDouble(args[i]));
		} else if (args[i].equals("-ensemble")) {
			ENSEMBLE_EVALUATION.setBooleanValue(true);
			i++;
			ENSEMBLE_SIZE.setDoubleValue(Integer.parseInt(args[i]));
		} else if (args[i].equals("-dynamicSlots")) {
			i++;
			DYNAMIC_SLOTS.setValue(args[i]);
			LOCAL_ALPHA.setBooleanValue(false);
		} else if (args[i].charAt(0) == '-') {
			// Check the arg against the rest of the program args
			for (ProgramArgument pa : ProgramArgument.values()) {
				// If the arg equals the shortcut or the name with a hyphen
				// added, use that argument
				if (args[i].equals("-" + pa.name_)
						|| args[i].equals(pa.shortcut_)) {
					i++;
					pa.setValue(args[i]);
					break;
				}
			}
		}
		return i;
	}
}
