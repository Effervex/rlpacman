package cerrla;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import util.Pair;

public enum ProgramArgument implements Serializable {
	ALPHA(0.6, "alpha", null, ParameterType.UPDATING, "Step size update"),
	BETA(0.0001, "beta", null, ParameterType.CONVERGENCE,
			"If KL sum updates are less than Beta * Alpha"),
	BOUNDED_ELITES(true, "boundedElites", null, ParameterType.UPDATING,
			"If the minimum number of elites = |S|."),
	CHI(0.0, "chi", null, ParameterType.SAMPLING,
			"The resampling percentage of average episode steps"),
	DYNAMIC_SLOTS(true, "dynamicSlots", null, ParameterType.SPECIALISATION,
			"If the slots grow dynamically"),
	EARLY_UPDATING(true, "earlyUpdating", null, ParameterType.UPDATING,
			"If the algorithm should perform updates using incomplete, but viable, elites."),
	ELITES_SIZE(0, "elitesSize", null, ParameterType.SAMPLING,
			"The size of the elites: 0=Av # rules, 1=Sum slot means, 2=Sum # KL rules"),
	ENSEMBLE_EVALUATION(false, "ensembleEvaluation", null,
			ParameterType.EVALUATION, "If using ensemble evaluation"),
	ENSEMBLE_SIZE(100, "ensembleSize", null, ParameterType.EVALUATION,
			"The size of the ensemble"),
	INHERIT_PARENT_SLOT_VALS(true, "inheritParent", null,
			ParameterType.SPECIALISATION,
			"If newly created slots inherit mu(S) and o(S) of the parent slot."),
	INITIAL_ORDERING_SD(0.25, "initialSlotOrderingSD", null,
			ParameterType.SAMPLING, "The SD of the slot order"),
	INITIAL_SLOT_MEAN(1, "initialSlotMean", "-slotProb",
			ParameterType.SAMPLING, "The initial slot mu probabilities."),
	LOCAL_ALPHA(true, "localAlpha", null, ParameterType.UPDATING,
			"If updates are performed slot locally"),
	MIN_WEIGHTED_UPDATE(0.1, "minWeightedUpdate", null, ParameterType.UPDATING,
			"The minimal update when using weighted updates"),
	MULTI_MODULES(false, "multiMods", null, ParameterType.SAMPLING,
			"If multi-condition modules are to be learned/used"),
	NEGATIVE_UPDATES(false, "negativeUpdates", null, ParameterType.UPDATING,
			"If performing negative updates"),
	NUM_NUMERICAL_SPLITS(3, "numNumericalSplits", null,
			ParameterType.SPECIALISATION, "The number of numerical splits"),
	NUM_PERFORMANCES_CONVERGED(3, "numPerformancesConverged", null,
			ParameterType.CONVERGENCE,
			"If the average performance has remained the same for this many iterations"),
	NUM_UPDATES_CONVERGED(10, "numUpdatesConverged", null,
			ParameterType.CONVERGENCE,
			"If KL sum updates remain below Beta for this many updates"),
	ONLY_GOAL_RULES(false, "onlyGoalRules", "-goalRules",
			ParameterType.SPECIALISATION,
			"If the agent should only create rules with the goal condition in it"),
	PERFORMANCE_CONVERGENCE(false, "performanceConvergence", null,
			ParameterType.CONVERGENCE,
			"If converging when performance has settled"),
	PERFORMANCE_EPISODE_GAP(100, "performanceEpisodeGap", null,
			ParameterType.EVALUATION, "The gap between measuring performances"),
	PERFORMANCE_TESTING_SIZE(100, "policyTestingSize", null,
			ParameterType.EVALUATION,
			"Size of average performance sliding window"),
	POLICY_REPEATS(3, "policyRepeats", null, ParameterType.EVALUATION,
			"Number of times policy is repeated"),
	RESET_ELITES(false, "resetElites", null, ParameterType.UPDATING,
			"If the entire elites are reset when a new slot is created."),
	RETEST_STALE_POLICIES(true, "retestStale", null, ParameterType.SAMPLING,
			"If stale policies should be immediately retested."),
	RHO(0.1, "rho", null, ParameterType.UPDATING, "N_E's proportion of N"),
	SEED_MODULE_RULES(false, "seedModuleRules", null, ParameterType.SAMPLING,
			"If module rules should just be loaded & seeded _once_."),
	SLOT_FIXING(false, "slotFixing", null, ParameterType.CONVERGENCE,
			"If slots can be fixed."),
	SLOT_THRESHOLD(0.5, "slotThreshold", null, ParameterType.SPECIALISATION,
			"The slot splitting threshold. -1 means use |S|-1 threshold"),
	SYSTEM_OUTPUT(true, "systemOutput", "-sysOut", ParameterType.EVALUATION,
			"If the console should output data during execution."),
	TEST_ITERATIONS(100, "testIterations", null, ParameterType.EVALUATION,
			"Number of iterations to test the final testing for"),
	TESTING(false, "test", "-t", ParameterType.EVALUATION,
			"If just running tests"),
	USE_MODULES(true, "useModules", null, ParameterType.SAMPLING,
			"If using/learning modules"),
	WEIGHTED_UPDATES(false, "weightedUpdates", null, ParameterType.UPDATING,
			"If using weighted updates");

	public static final File ARG_FILE = new File("cerrlaArgs.txt");
	public static final int ELITES_SIZE_AV_RULES = 0;
	public static final int ELITES_SIZE_SUM_SLOTS = 1;
	public static final int ELITES_SIZE_SUM_RULES = 2;
	public static final int ELITES_SIZE_MAX_RULES = 3;
	private Boolean booleanValue_;
	private String comment_;
	private Object defaultValue_;
	private String name_;
	private Double numberValue_;
	private ParameterType parameterType_;
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
			ParameterType pType, String comment) {
		if (defaultVal instanceof Number) {
			if (defaultVal instanceof Integer)
				defaultVal = ((Integer) defaultVal).doubleValue();
			numberValue_ = (Double) defaultVal;
			defaultValue_ = numberValue_;
		} else if (defaultVal instanceof Boolean) {
			booleanValue_ = (Boolean) defaultVal;
			defaultValue_ = booleanValue_;
		}
		name_ = name;
		shortcut_ = shortcut;
		comment_ = comment;
		parameterType_ = pType;
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
	 * Handles a program argument not caught by the algorithm earlier.
	 * 
	 * @param i
	 *            The current index.
	 * @param args
	 *            The arguments given at command-line.
	 * @return The index after handling.
	 */
	public static Pair<Integer, String> handleArg(int i, String[] args) {
		boolean argFound = false;
		String paramName = "";
		if (args[i].equals("-slotProb")) {
			i++;
			if (args[i].equals("dynamic")) {
				INITIAL_SLOT_MEAN.setDoubleValue(-1);
				paramName = "dynamicSlotProb";
			} else {
				INITIAL_SLOT_MEAN.setDoubleValue(Double.parseDouble(args[i]));
				paramName = "slotProb" + args[i];
			}
			argFound = true;
		} else if (args[i].equals("-ensemble")) {
			ENSEMBLE_EVALUATION.setBooleanValue(true);
			i++;
			ENSEMBLE_SIZE.setDoubleValue(Integer.parseInt(args[i]));
			paramName = "ensemble";
			argFound = true;
		} else if (args[i].equals("-dynamicSlots")) {
			i++;
			DYNAMIC_SLOTS.setValue(args[i]);
			if (DYNAMIC_SLOTS.booleanValue())
				paramName = "dynamicSlots";
			else
				paramName = "staticSlots";
			argFound = true;
		} else if (args[i].charAt(0) == '-') {
			// Check the arg against the rest of the program args
			for (ProgramArgument pa : ProgramArgument.values()) {
				// If the arg equals the shortcut or the name with a hyphen
				// added, use that argument
				if (args[i].equals("-" + pa.name_)
						|| args[i].equals(pa.shortcut_)) {
					i++;
					pa.setValue(args[i]);
					argFound = true;
					Object value = pa.getValue();
					if (value instanceof Boolean) {
						paramName = pa.getName();
						if (!((Boolean) value).booleanValue()) {
							paramName = "No"
									+ paramName.substring(0, 1).toUpperCase()
									+ paramName.substring(1);
						}
					} else {
						paramName = pa.getName() + value;
					}
					break;
				}
			}
		}

		// If no arg found, notify the user
		if (!argFound) {
			System.err.println("Invalid arguments! " + Arrays.toString(args));
		}
		return new Pair<Integer, String>(i, paramName.substring(0, 1)
				.toUpperCase() + paramName.substring(1));
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
				String[] split = input.split("%");
				if (split.length > 1) {
					split = split[0].split("=");
					args.put(split[0], split[1].trim());
				}
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
	 * Saves the arguments into a give {@link BufferedWriter}.
	 * 
	 * @param bw
	 *            The buffered writer.
	 * @param outputAllArgs
	 *            If every argument should be output, or just the changed ones.
	 */
	public static void saveArgs(BufferedWriter bw, boolean outputAllArgs)
			throws IOException {
		SortedSet<ProgramArgument> sortedArgs = new TreeSet<ProgramArgument>(
				new Comparator<ProgramArgument>() {
					@Override
					public int compare(ProgramArgument o1, ProgramArgument o2) {
						int result = o1.parameterType_
								.compareTo(o2.parameterType_);
						if (result != 0)
							return result;
						return o1.name_.compareTo(o2.name_);
					}
				});
		for (ProgramArgument pa : ProgramArgument.values()) {
			sortedArgs.add(pa);
		}

		ParameterType pt = null;
		boolean modifiedParameters = false;
		for (ProgramArgument pa : sortedArgs) {
			String output = null;
			// Determine if outputting an argument (MODIFIED or not)
			if (!pa.getValue().equals(pa.getDefaultValue())) {
				output = pa.getName() + "=" + pa.getValue()
						+ "\t\t\t% -----MODIFIED----- % " + pa.getComment()
						+ "\n";
				modifiedParameters = true;
			} else if (outputAllArgs)
				output = pa.getName() + "=" + pa.getValue() + "\t\t\t% "
						+ pa.getComment() + "\n";

			// Outline the parameter type if the output isn't null and haven't
			// already outlined it.
			if (output != null) {
				if (!pa.parameterType_.equals(pt)) {
					if (pt != null) {
						// If no modified parameters
						if (!modifiedParameters && !outputAllArgs)
							bw.write("<DEFAULT ARGS>\n");
						bw.write("\n");
					}
					pt = pa.parameterType_;
					bw.write("\t-----" + pt + "-----\n");
					modifiedParameters = false;
				}
				bw.write(output);
			}
		}
	}

	/**
	 * Saves the arguments into a file if the file doesn't already exist.
	 */
	public static void saveArgsFile() {
		try {
			if (!ARG_FILE.exists())
				ARG_FILE.createNewFile();
			else
				return;

			FileWriter fw = new FileWriter(ARG_FILE);
			BufferedWriter bw = new BufferedWriter(fw);

			saveArgs(bw, true);

			bw.close();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Simple class for dividing the program args into categories.
	 * 
	 * @author Sam Sarjant
	 */
	private enum ParameterType {
		CONVERGENCE, EVALUATION, SAMPLING, SPECIALISATION, UPDATING;
	}
}
