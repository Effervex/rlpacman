package cerrla;

import relationalFramework.GoalCondition;
import relationalFramework.CoveringRelationalPolicy;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
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
	/** The relative directory in which modules are stored. */
	public static final String MODULE_DIR = "modules";

	/** The suffix for module files. */
	private static final String MODULE_SUFFIX = ".mod";

	/**
	 * The policy rules which solve this module (not necessarily optimal). Note
	 * that these are in parameter form.
	 */
	private final ArrayList<RelationalRule> moduleRules_;

	/** The predicate this module solves. */
	private final String modulePredicate_;

	/** The parameter terms the module takes. */
	private final ArrayList<String> parameterTerms_;

	/** A collection which notes non-existant files to speed execution. */
	private static final Collection<String> nonExistantModules_ = new ArrayList<String>();

	/** A mapping of loaded modules. */
	private static final Map<String, Module> loadedModules_ = new HashMap<String, Module>();

	/** Saves the modules at the end of the learning. */
	public static boolean saveAtEnd_ = false;

	/**
	 * Creates a new module, using the goal as the module goal, and the slot
	 * distribution to create the module rules.
	 * 
	 * @param modName
	 *            The name of the module.
	 * @param numArgs
	 *            The number of arguments the module takes.
	 * @param bestPolicy
	 *            The state of the distribution for the agent.
	 */
	private Module(String modName, int numArgs,
			CoveringRelationalPolicy bestPolicy) {
		parameterTerms_ = new ArrayList<String>();
		// Run through the facts (probably only 1)
		modulePredicate_ = modName;
		for (int i = 0; i < numArgs; i++)
			parameterTerms_.add(StateSpec.createGoalTerm(i));

		// Add the rules by taking the most likely rule from the ordered slots.
		List<RelationalRule> policyRules = bestPolicy.getPolicyRules();
		moduleRules_ = new ArrayList<RelationalRule>();
		for (RelationalRule policyRule : policyRules) {
			// Ground modular rules
			policyRule = policyRule.groundModular();
			policyRule.setQueryParams(parameterTerms_);
			moduleRules_.add(policyRule);
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
			ArrayList<RelationalRule> rules) {
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
			File modFile = new File(getModFolder(environmentName, predicate),
					predicate + MODULE_SUFFIX);

			// If the module file exists, load it up.
			if (modFile.exists()) {
				FileReader reader = new FileReader(modFile);
				BufferedReader bf = new BufferedReader(reader);

				ArrayList<String> parameters = new ArrayList<String>();
				ArrayList<RelationalRule> rules = new ArrayList<RelationalRule>();
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
					rules.add(new RelationalRule(input, parameters));
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
	 * @param modName
	 *            The name of the module goal.
	 * @param numArgs
	 *            The number of arguments the module takes.
	 * @param bestPolicy
	 *            The best policy in the elites at the end of learning.
	 */
	public static void saveModule(String modName, int numArgs,
			CoveringRelationalPolicy bestPolicy) {
		if (!moduleExists(StateSpec.getInstance().getEnvironmentName(), modName)) {
			Module newModule = new Module(modName, numArgs, bestPolicy);
			nonExistantModules_.remove(modName);
			loadedModules_.put(modName, newModule);
			if (!saveAtEnd_) {
				saveModule(newModule);
			}
		}
	}

	/**
	 * Internal module saving method. Takes in a list of facts that make up the
	 * module, the environment name and the module itself.
	 * 
	 * @param newModule
	 *            The module being saved.
	 */
	private static void saveModule(Module newModule) {
		try {
			// Module dir
			File modFile = new File(getModFolder(StateSpec.getInstance()
					.getEnvironmentName(), newModule.modulePredicate_),
					newModule.getModulePredicate() + MODULE_SUFFIX);

			FileWriter writer = new FileWriter(modFile);
			BufferedWriter bf = new BufferedWriter(writer);

			bf.write(";; The parameter variables given to the module on loading.\n");
			bf.write("(declare (variables");
			// Write the parameters out for each fact in the module.
			for (String param : newModule.getParameterTerms()) {
				bf.write(" " + param);
			}
			bf.write("))\n");

			// Writing the rules
			bf.write(";; The rules of the module, evaluated "
					+ "in order with variables replaced by parameters.");
			for (RelationalRule gr : newModule.moduleRules_) {
				gr.removeParameters();
				bf.write("\n" + gr.toString());
			}

			bf.close();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Function to check if a module exists.
	 * 
	 * @param environmentName
	 *            The name of the environment.
	 * @param modName
	 *            The module name.
	 * @return True, if it module exists, else false.
	 */
	public static boolean moduleExists(String environmentName, String modName) {
		// Checks to skip loading.
		if (nonExistantModules_.contains(modName))
			return false;
		if (loadedModules_.containsKey(modName))
			return true;

		File modLocation = new File(getModFolder(environmentName, modName),
				modName + MODULE_SUFFIX);

		// If the module file exists, load it up.
		if (modLocation.exists()) {
			return true;
		}

		nonExistantModules_.add(modName);
		return false;
	}

	/**
	 * Basic method which fetches a module location for a given environment and
	 * local goal.
	 * 
	 * @param environmentName
	 *            The name of the environment.
	 * @param modName
	 *            The name of the module.
	 * @return The File path to the module.
	 */
	private static File getModFolder(String environmentName, String modName) {
		return new File(MODULE_DIR + File.separatorChar + environmentName
				+ File.separatorChar + modName + File.separatorChar);
	}

	/**
	 * Saves the generator to its modular location.
	 * 
	 * @param policyGenerator
	 *            The policy generator to save.
	 */
	public static void saveGenerator(PolicyGenerator policyGenerator)
			throws Exception {
		File modFolder = getModFolder(StateSpec.getInstance()
				.getEnvironmentName(), policyGenerator.getLocalGoal());
		modFolder.mkdirs();
		File genFile = new File(modFolder, PolicyGenerator.SERIALISED_FILENAME);
		genFile.createNewFile();

		FileOutputStream fos = new FileOutputStream(genFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);

		oos.writeObject(policyGenerator);
		oos.close();
	}

	/**
	 * Loads a policy generator saved in its modular location.
	 * 
	 * @param goalCondition
	 *            The goal the policy generator is trying to achieve.
	 * @return The loaded Policy Generator or null if it doesn't exist.
	 */
	public static PolicyGenerator loadGenerator(GoalCondition goalCondition) {
		File genFile = new File(getModFolder(StateSpec.getInstance()
				.getEnvironmentName(), goalCondition.toString()),
				PolicyGenerator.SERIALISED_FILENAME);
		if (genFile.exists()) {
			return PolicyGenerator.loadPolicyGenerator(genFile, goalCondition);
		}
		return null;
	}

	/**
	 * Gets the rules for this module. Note that the rules returned are unique
	 * from the module (clones).
	 * 
	 * @return A cloned copy of the module rules.
	 */
	public ArrayList<RelationalRule> getModuleRules() {
		ArrayList<RelationalRule> clonedRules = new ArrayList<RelationalRule>();
		for (RelationalRule gr : moduleRules_) {
			clonedRules.add((RelationalRule) gr.clone(true));
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
		for (RelationalRule rule : moduleRules_) {
			buffer.append(rule + "\n");
		}
		return buffer.toString();
	}
}
