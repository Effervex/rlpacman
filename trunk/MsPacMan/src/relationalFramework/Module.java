package relationalFramework;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class representing a loaded module. Modules typically have parameterisable
 * arguments and return a rule(s) which solve the module. Note that if modules
 * call modules, the rules returned will be gathered recursively.
 * 
 * @author Samuel J. Sarjant
 */
public class Module {
	public static final String MOD_VARIABLE_PREFIX = "?_MOD_";

	/** The relative directory in which modules are stored. */
	public static final String MODULE_DIR = "modules";

	/** The suffix for module files. */
	private static final String MODULE_SUFFIX = ".mod";

	/** The string for joining predicates. */
	private static final String MODULE_JOIN = "&";

	/**
	 * The policy rules which solve this module (not necessarily optimal). Note
	 * that these are in parameter form.
	 */
	private final ArrayList<GuidedRule> moduleRules_;

	/** The predicate this module solves. */
	private final String modulePredicate_;

	/** The parameter terms the module takes. */
	private final ArrayList<String> parameterTerms_;

	/** A collection which notes non-existant files to speed execution. */
	private static final Collection<String> nonExistantModules_ = new ArrayList<String>();

	/** A mapping of loaded modules. */
	private static final Map<String, Module> loadedModules_ = new HashMap<String, Module>();

	/**
	 * Creates a new module, using the goal as the module goal, and the slot
	 * distribution to create the module rules.
	 * 
	 * @param facts
	 *            The goal the agent was working towards.
	 * @param orderedDistribution
	 *            The state of the distribution for the agent.
	 */
	private Module(ArrayList<String> facts,
			OrderedDistribution<Slot> orderedDistribution) {
		parameterTerms_ = new ArrayList<String>();
		// Run through the facts (probably only 1)
		modulePredicate_ = formName(facts);
		for (String fact : facts) {
			String[] splitGoal = StateSpec.splitFact(fact);

			for (int i = 1; i < splitGoal.length; i++) {
				parameterTerms_.add(createModuleParameter(i - 1));
			}
		}

		// Add the rules by taking the most likely rule from the ordered slots.
		moduleRules_ = new ArrayList<GuidedRule>();
		List<Slot> orderedSlots = orderedDistribution.getOrderedElements();
		for (Slot slot : orderedSlots) {
			Slot removalSlot = slot.clone();
			double repetitions = Math.round(slot.getSelectionProbability());
			for (int i = 0; i < repetitions; i++) {
				if (!removalSlot.isEmpty()) {
					GuidedRule rule = removalSlot.getGenerator()
							.sampleWithRemoval(true);
					rule.setAsLoadedModuleRule(true);

					moduleRules_.add(rule);
				}
			}
		}
	}

	/**
	 * A basic constructor used when reading modules from file.
	 * 
	 * @param predicate
	 *            The module predicate.
	 * @param parameters
	 *            The predicate parameters.
	 * @param rules
	 *            The rules for solving the module.
	 */
	private Module(String predicate, ArrayList<String> parameters,
			ArrayList<GuidedRule> rules) {
		modulePredicate_ = predicate;
		parameterTerms_ = parameters;
		moduleRules_ = rules;
	}

	/**
	 * Attempts to load a module from file.
	 * 
	 * @param packageName
	 *            The name of the environment.
	 * @param predicate
	 *            The predicate name (module to load).
	 * @return The module, if it exists, or null.
	 */
	public static Module loadModule(String environmentName, String predicate) {
		// Checks to skip loading.
		if (nonExistantModules_.contains(predicate))
			return null;
		if (loadedModules_.containsKey(predicate))
			return loadedModules_.get(predicate);

		try {
			File modLocation = new File(MODULE_DIR + File.separatorChar
					+ environmentName + File.separatorChar + predicate
					+ MODULE_SUFFIX);

			// If the module file exists, load it up.
			if (modLocation.exists()) {
				FileReader reader = new FileReader(modLocation);
				BufferedReader bf = new BufferedReader(reader);

				ArrayList<String> parameters = new ArrayList<String>();
				ArrayList<GuidedRule> rules = new ArrayList<GuidedRule>();
				// Read in the parameters
				String input = bf.readLine();
				// Skip through comments.
				while (input.substring(0, 2).equals(";;"))
					input = bf.readLine();
				// The first line is typically the parameter declaration
				if (input.substring(0, 8).equals("(declare")) {
					// Parse the declares line for parameters
					Pattern p = Pattern
							.compile("\\(declare \\(variables((?: ?.+?)+)\\)\\)");
					Matcher m = p.matcher(input);

					m.find();
					String[] parameterArray = m.group(1).trim().split(" ");
					for (String param : parameterArray) {
						parameters.add(param);
					}

					input = bf.readLine();
				}

				// Read in the rules
				do {
					// Skip through comments.
					while (input.substring(0, 2).equals(";;"))
						input = bf.readLine();
					rules.add(new GuidedRule(input, parameters));
					input = bf.readLine();
				} while (input != null);

				bf.close();
				reader.close();

				Module module = new Module(predicate, parameters, rules);
				loadedModules_.put(predicate, module);
				return module;
			} else {
				nonExistantModules_.add(predicate);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Saves a module to file by taking the state of the policy generator and
	 * saving the necessary details about it.
	 * 
	 * @param facts
	 *            The goal the module works towards.
	 * @param environment
	 *            The environment the module exists within.
	 * @param orderedDistribution
	 *            The policy generator which solves the goal.
	 */
	public static void saveModule(ArrayList<String> facts, String environment,
			OrderedDistribution<Slot> orderedDistribution) {
		Module newModule = new Module(facts, orderedDistribution);
		String modName = formName(facts);
		nonExistantModules_.remove(modName);
		loadedModules_.put(modName, newModule);

		try {
			File modLocation = new File(MODULE_DIR + File.separatorChar
					+ environment + File.separatorChar + formName(facts)
					+ MODULE_SUFFIX);
			if (modLocation.createNewFile()) {

				FileWriter writer = new FileWriter(modLocation);
				BufferedWriter bf = new BufferedWriter(writer);

				bf
						.write(";; The parameter variables given to the module on loading.\n");
				bf.write("(declare (variables");
				// Write the parameters out for each fact in the module.
				int j = 0;
				for (String fact : facts) {
					int i = 0;
					for (; i < StateSpec.getInstance().getPredicates()
							.get(fact).size(); i++) {
						bf.write(" " + createModuleParameter(i + j));
					}
					j += i;
				}
				bf.write("))\n");

				// Writing the rules
				bf.write(";; The rules of the module, evaluated "
						+ "in order with variables replaced by parameters.");
				for (GuidedRule gr : newModule.moduleRules_) {
					gr.setParameters(null);
					bf.write("\n" + StateSpec.getInstance().encodeRule(gr));
				}

				bf.close();
				writer.close();
			} else {
				System.err.println("Module file exists!");
				System.err.println(facts.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Function to check if a module exists.
	 * 
	 * @param packageName
	 *            The name of the environment.
	 * @param constantPred
	 *            The predicate name(s) (module to load).
	 * @return True, if it module exists, else false.
	 */
	public static boolean moduleExists(String environmentName,
			ArrayList<String> constantPred) {
		String modName = formName(constantPred);

		// Checks to skip loading.
		if (nonExistantModules_.contains(modName))
			return false;
		if (loadedModules_.containsKey(modName))
			return true;

		File modLocation = new File(MODULE_DIR + File.separatorChar
				+ environmentName + File.separatorChar + modName
				+ MODULE_SUFFIX);

		// If the module file exists, load it up.
		if (modLocation.exists()) {
			return true;
		}

		nonExistantModules_.add(modName);
		return false;
	}

	/**
	 * Forms the name of a module file: just the predicate name if the
	 * constantFacts only contains one pred, otherwise it will be an ordered
	 * module name of predicates.
	 * 
	 * @param constantPred
	 *            The constant pred(s).
	 * @return A String representing the filename of the module.
	 */
	public static String formName(List<String> constantPred) {
		Collections.sort(constantPred);
		StringBuffer buffer = new StringBuffer(constantPred.get(0));
		for (int i = 1; i < constantPred.size(); i++) {
			buffer.append(MODULE_JOIN + constantPred.get(i));
		}

		return buffer.toString();
	}

	/**
	 * Creates a module parameter.
	 * 
	 * @param paramIndex
	 *            The index of the parameter.
	 * @return The name of the parameter.
	 */
	public static String createModuleParameter(int paramIndex) {
		return MOD_VARIABLE_PREFIX + (char) ('a' + paramIndex);
	}

	/**
	 * Gets the rules for this module. Note that the rules returned are unique
	 * from the module (clones).
	 * 
	 * @return A cloned copy of the module rules.
	 */
	public ArrayList<GuidedRule> getModuleRules() {
		ArrayList<GuidedRule> clonedRules = new ArrayList<GuidedRule>();
		for (GuidedRule gr : moduleRules_) {
			clonedRules.add((GuidedRule) gr.clone());
		}
		return clonedRules;
	}

	/**
	 * @return the modulePredicate_
	 */
	public String getModulePredicate() {
		return modulePredicate_;
	}

	/**
	 * @return the parameterTerms_
	 */
	public ArrayList<String> getParameterTerms() {
		return parameterTerms_;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer("(" + modulePredicate_);
		for (String param : parameterTerms_) {
			buffer.append(" " + param);
		}
		buffer.append("):\n");
		for (GuidedRule rule : moduleRules_) {
			buffer.append(rule + "\n");
		}
		return buffer.toString();
	}
}
