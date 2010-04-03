package relationalFramework;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jess.ValueVector;

/**
 * A class representing a loaded module. Modules typically have parameterisable
 * arguments and return a rule(s) which solve the module. Note that if modules
 * call modules, the rules returned will be gathered recursively.
 * 
 * @author Samuel J. Sarjant
 */
public class Module {
	/** The relative directory in which modules are stored. */
	private static final String MODULE_DIR = "modules";

	/** The suffix for module files. */
	private static final String MODULE_SUFFIX = ".mod";

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
	 * @param moduleGoal
	 *            The goal the agent was working towards.
	 * @param slotDistribution
	 *            The state of the distribution for the agent.
	 */
	private Module(String moduleGoal,
			ProbabilityDistribution<Slot> slotDistribution) {
		String[] splitGoal = StateSpec.splitFact(moduleGoal);
		modulePredicate_ = splitGoal[0];
		parameterTerms_ = new ArrayList<String>();
		ArrayList<String> oldTerms = new ArrayList<String>();
		for (int i = 1; i < splitGoal.length; i++) {
			oldTerms.add(splitGoal[i]);
			parameterTerms_.add(createModuleParameter(i - 1));
		}

		moduleRules_ = new ArrayList<GuidedRule>();
		ArrayList<Slot> orderedSlots = slotDistribution.getOrderedElements();
		for (Slot slot : orderedSlots) {
			GuidedRule rule = slot.getGenerator().getOrderedElements().get(0);
			rule.setAsLoadedModuleRule();

			// Replace the constants with parameters
			moduleRules_.add(rule);
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
	 * @param internalGoal
	 *            The goal the module works towards.
	 * @param environment
	 *            The environment the module exists within.
	 * @param generator
	 *            The policy generator which solves the goal.
	 */
	public static void saveModule(String internalGoal, String environment,
			ProbabilityDistribution<Slot> generator) {
		Module newModule = new Module(internalGoal, generator);
		nonExistantModules_.remove(internalGoal);
		loadedModules_.put(internalGoal, newModule);

		try {
			File modLocation = new File(MODULE_DIR + File.separatorChar
					+ environment + File.separatorChar + internalGoal
					+ MODULE_SUFFIX);
			if (modLocation.createNewFile()) {

				FileWriter writer = new FileWriter(modLocation);
				BufferedWriter bf = new BufferedWriter(writer);

				bf.write("(declare (variables");
				for (int i = 0; i < StateSpec.getInstance().getPredicates()
						.get(internalGoal).size(); i++) {
					bf.write(" " + createModuleParameter(i));
				}
				bf.write("))");
				
				// Writing the rules
				for (GuidedRule gr : newModule.moduleRules_) {
					ValueVector vv = null;
					gr.setParameters(vv);
					bf.write("\n" + StateSpec.getInstance().encodeRule(gr));
				}

				bf.close();
				writer.close();
			} else {
				System.err.println("Module file exists! Saving to temp file.");
				saveModule(internalGoal + "temp", environment, generator);
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
	 * @param predicate
	 *            The predicate name (module to load).
	 * @return True, if it module exists, else false.
	 */
	public static boolean moduleExists(String environmentName, String predicate) {
		// Checks to skip loading.
		if (nonExistantModules_.contains(predicate))
			return false;
		if (loadedModules_.containsKey(predicate))
			return true;

		File modLocation = new File(MODULE_DIR + File.separatorChar
				+ environmentName + File.separatorChar + predicate
				+ MODULE_SUFFIX);

		// If the module file exists, load it up.
		if (modLocation.exists()) {
			return true;
		}

		nonExistantModules_.add(predicate);
		return false;
	}

	/**
	 * Creates a module parameter.
	 * 
	 * @param paramIndex
	 *            The index of the parameter.
	 * @return The name of the parameter.
	 */
	public static String createModuleParameter(int paramIndex) {
		return "?_MOD_" + (char) ('a' + paramIndex);
	}

	/**
	 * Replaces all constants in the goal in a rule with parameterisable terms
	 * (?_MOD_a, ?_MOD_b, etc).
	 * 
	 * @param rule
	 *            The rule to be parameterised.
	 * @param terms
	 *            The terms to be swapped out.
	 * @return The rule with goal constants swapped with parameterisable terms.
	 */
	private String parameteriseRule(String rule, ArrayList<String> terms) {
		for (int i = 0; i < parameterTerms_.size(); i++) {
			rule = rule.replaceAll(" " + terms.get(i) + "(?= |\\))", " "
					+ parameterTerms_.get(i));
		}
		return rule;
	}

	/**
	 * @return the moduleRules_
	 */
	public ArrayList<GuidedRule> getModuleRules() {
		return moduleRules_;
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
