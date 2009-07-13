package rlPacMan;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

/**
 * A singleton implementation of the current rule base.
 * 
 * @author Samuel J. Sarjant
 * 
 */
public class RuleBase {
	/** The number of random rules to use. */
	private static final int RANDOM_RULE_NUMBER = 100;

	/** The delimiter character between rules within the same rule base. */
	public static final String RULE_DELIMITER = ",";

	/** The instance. */
	private static RuleBase instance_;

	/** The rules in use. */
	private ArrayList<Rule>[] rules_;

	/** The random number generator to use. */
	private Random random_ = new Random();

	/**
	 * A private constructor for generating a rule base from hand or random.
	 * 
	 * @param handCoded
	 *            If the rule base uses hand-coded rules or random rules.
	 * @param ruleBases
	 *            The number of rule bases to generate if random.
	 */
	private RuleBase(boolean handCoded, int ruleBases) {
		if (handCoded) {
			rules_ = new ArrayList[1];
			rules_[0] = loadHandCodedRules();
		} else {
			rules_ = new ArrayList[ruleBases];
			for (int i = 0; i < ruleBases; i++) {
				rules_[i] = generateRandomRules(RANDOM_RULE_NUMBER);
			}
		}
	}

	/**
	 * A private constructor for creating a rule base from file.
	 * 
	 * @param ruleBaseFile
	 *            The file from which to load the rules.
	 */
	private RuleBase(File ruleBaseFile) {
		loadRulesFromFile(ruleBaseFile);
	}

	/**
	 * Load the rules from a file.
	 * 
	 * @param ruleBaseFile
	 *            The file to load the rules from.
	 */
	private void loadRulesFromFile(File ruleBaseFile) {
		ArrayList<ArrayList<Rule>> ruleBases = new ArrayList<ArrayList<Rule>>();
		try {
			FileReader reader = new FileReader(ruleBaseFile);
			BufferedReader bf = new BufferedReader(reader);

			String input = null;
			while (!(input = bf.readLine()).equals("")) {
				ArrayList<Rule> ruleBase = new ArrayList<Rule>();
				// Split the base into rules
				String[] split = input.split(RULE_DELIMITER);
				// For each rule, add it to the rulebase
				for (int i = 0; i < split.length; i++) {
					ruleBase.add(Rule.parseRule(split[i]));
				}
				ruleBases.add(ruleBase);
			}

			bf.close();
			reader.close();

			rules_ = ruleBases.toArray(new ArrayList[ruleBases.size()]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Save rules to a file.
	 * 
	 * @param ruleBaseFile
	 *            The file to save the rules to.
	 */
	public void saveRulesToFile(File ruleBaseFile) {
		try {
			FileWriter writer = new FileWriter(ruleBaseFile);
			BufferedWriter bf = new BufferedWriter(writer);

			// For each of the rule bases
			for (int i = 0; i < rules_.length; i++) {
				// For each of the rules
				for (Rule r : rules_[i]) {
					bf.write(r.toParseableString() + RULE_DELIMITER);
				}
				bf.write("\n");
			}

			bf.close();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads the hand-coded rules with default weights.
	 * 
	 * @param generator
	 *            The generator to receive the rules.
	 * @return The list of hand coded rules.
	 */
	private ArrayList<Rule> loadHandCodedRules() {
		ArrayList<Rule> handRules = new ArrayList<Rule>();

		// Go to dot
		handRules.add(new Rule(PacManObservations.CONSTANT, Rule.GREATER_THAN,
				0, PacManHighAction.TO_DOT, true));
		// Go to centre of dots
		handRules.add(new Rule(PacManObservations.CONSTANT, Rule.GREATER_THAN,
				0, PacManHighAction.TO_CENTRE_OF_DOTS, true));
		// From nearest ghost 1
		handRules.add(new Rule(PacManObservations.NEAREST_GHOST,
				Rule.LESS_THAN, 3, PacManHighAction.FROM_GHOST, true));
		// From nearest ghost 2
		handRules.add(new Rule(PacManObservations.NEAREST_GHOST,
				Rule.LESS_THAN, 4, PacManHighAction.FROM_GHOST, true));
		// From nearest ghost 3
		handRules.add(new Rule(PacManObservations.NEAREST_GHOST,
				Rule.LESS_THAN, 5, PacManHighAction.FROM_GHOST, true));
		// Stop running from nearest ghost 1
		handRules.add(new Rule(PacManObservations.NEAREST_GHOST,
				Rule.GREATER_THAN, 5, PacManHighAction.FROM_GHOST, false));
		// Stop running from nearest ghost 2
		handRules.add(new Rule(PacManObservations.NEAREST_GHOST,
				Rule.GREATER_THAN, 6, PacManHighAction.FROM_GHOST, false));
		// Stop running from nearest ghost 3
		handRules.add(new Rule(PacManObservations.NEAREST_GHOST,
				Rule.GREATER_THAN, 7, PacManHighAction.FROM_GHOST, false));
		// Towards safe junction
		handRules.add(new Rule(PacManObservations.CONSTANT, Rule.GREATER_THAN,
				0, PacManHighAction.TO_SAFE_JUNCTION, true));
		// Towards maximally safe junction 1
		handRules.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
				Rule.LESS_THAN, 3, PacManHighAction.TO_SAFE_JUNCTION, true));
		// Towards maximally safe junction 2
		handRules.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
				Rule.LESS_THAN, 2, PacManHighAction.TO_SAFE_JUNCTION, true));
		// Maximally safe junction off 1
		handRules
				.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
						Rule.GREATER_THAN, 3,
						PacManHighAction.TO_SAFE_JUNCTION, false));
		// Safe to stop running 1
		handRules.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
				Rule.GREATER_THAN, 3, PacManHighAction.FROM_GHOST, false));
		// Maximally safe junction off 2
		handRules
				.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
						Rule.GREATER_THAN, 5,
						PacManHighAction.TO_SAFE_JUNCTION, false));
		// Safe to stop running 2
		handRules.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
				Rule.GREATER_THAN, 5, PacManHighAction.FROM_GHOST, false));
		// Keep on moving from ghosts
		handRules.add(new Rule(PacManObservations.CONSTANT, Rule.GREATER_THAN,
				0, PacManHighAction.FROM_GHOST, true));
		// Eat edible ghosts
		handRules.add(new Rule(PacManObservations.CONSTANT, Rule.GREATER_THAN,
				0, PacManHighAction.TO_ED_GHOST, true));
		// Ghost coming, chase powerdots
		handRules.add(new Rule(PacManObservations.NEAREST_GHOST,
				Rule.LESS_THAN, 4, PacManHighAction.TO_POWER_DOT, true));
		// If edible ghosts, don't chase power dots
		handRules.add(new Rule(PacManObservations.NEAREST_ED_GHOST,
				Rule.LESS_THAN, 99, PacManHighAction.TO_POWER_DOT, false));
		// If edible ghosts and we're close to a powerdot, move away from it
		handRules.add(new Rule(PacManObservations.NEAREST_ED_GHOST,
				Rule.LESS_THAN, 99, PacManObservations.NEAREST_POWER_DOT,
				Rule.LESS_THAN, 5, PacManHighAction.FROM_POWER_DOT, true));
		// If edible ghosts, move away from powerdots
		handRules.add(new Rule(PacManObservations.NEAREST_ED_GHOST,
				Rule.LESS_THAN, 99, PacManHighAction.FROM_POWER_DOT, true));
		// If no edible ghosts, stop moving from powerdots
		handRules.add(new Rule(PacManObservations.NEAREST_ED_GHOST,
				Rule.GREATER_THAN, 99, PacManHighAction.FROM_POWER_DOT, false));
		// If no edible ghosts, chase powerdots
		handRules.add(new Rule(PacManObservations.NEAREST_ED_GHOST,
				Rule.GREATER_THAN, 99, PacManHighAction.TO_POWER_DOT, true));
		// If we're close to a ghost and powerdot, chase the powerdot 1
		handRules.add(new Rule(PacManObservations.NEAREST_POWER_DOT,
				Rule.LESS_THAN, 2, PacManObservations.NEAREST_GHOST,
				Rule.LESS_THAN, 5, PacManHighAction.TO_POWER_DOT, true));
		// If we're close to a ghost and powerdot, chase the powerdot 2
		handRules.add(new Rule(PacManObservations.NEAREST_POWER_DOT,
				Rule.LESS_THAN, 4, PacManObservations.NEAREST_GHOST,
				Rule.LESS_THAN, 5, PacManHighAction.TO_POWER_DOT, true));
		// In the clear
		handRules.add(new Rule(PacManObservations.NEAREST_GHOST,
				Rule.GREATER_THAN, 7, PacManObservations.MAX_JUNCTION_SAFETY,
				Rule.GREATER_THAN, 4, PacManHighAction.FROM_GHOST, false));
		// Ghosts are not close, eat a dot 1
		handRules.add(new Rule(PacManObservations.GHOST_DENSITY,
				Rule.LESS_THAN, 1.5, PacManObservations.NEAREST_POWER_DOT,
				Rule.LESS_THAN, 5, PacManHighAction.FROM_POWER_DOT, true));
		// Far from power dot, stop running
		handRules.add(new Rule(PacManObservations.NEAREST_POWER_DOT,
				Rule.GREATER_THAN, 10, PacManHighAction.FROM_POWER_DOT, false));
		// Ghosts are too spread, move away from power dot
		handRules.add(new Rule(PacManObservations.TOTAL_DIST_TO_GHOSTS,
				Rule.GREATER_THAN, 30, PacManHighAction.FROM_POWER_DOT, true));
		// Unsafe junction, run from ghosts 1
		handRules.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
				Rule.LESS_THAN, 3, PacManHighAction.FROM_GHOST, true));
		// Unsafe junction, run from ghosts 2
		handRules.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
				Rule.LESS_THAN, 2, PacManHighAction.FROM_GHOST, true));
		// Unsafe junction, run from ghosts 3
		handRules.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
				Rule.LESS_THAN, 1, PacManHighAction.FROM_GHOST, true));
		// Move from ghost centre
		handRules.add(new Rule(PacManObservations.CONSTANT, Rule.GREATER_THAN,
				0, PacManHighAction.FROM_GHOST_CENTRE, true));
		// Stop chasing edible ghosts if none
		handRules.add(new Rule(PacManObservations.NEAREST_ED_GHOST,
				Rule.GREATER_THAN, 99, PacManHighAction.TO_ED_GHOST, false));
		// Chasing edible ghosts
		handRules.add(new Rule(PacManObservations.NEAREST_ED_GHOST,
				Rule.LESS_THAN, 99, PacManHighAction.TO_ED_GHOST, true));
		// If not running from powerdot, move towards it
		handRules.add(new Rule(PacManHighAction.FROM_POWER_DOT, false,
				PacManHighAction.TO_POWER_DOT, true));
		// If there is a fruit, chase it
		handRules.add(new Rule(PacManObservations.NEAREST_FRUIT,
				Rule.LESS_THAN, 99, PacManHighAction.TO_FRUIT, true));
		// If there isn't a fruit, stop chasing it
		handRules.add(new Rule(PacManObservations.NEAREST_FRUIT,
				Rule.GREATER_THAN, 99, PacManHighAction.TO_FRUIT, false));

		return handRules;
	}

	/**
	 * Generates random rules.
	 * 
	 * @param baseSize
	 *            The number of random rules to generate.
	 * @return A list of random rules.
	 */
	private ArrayList<Rule> generateRandomRules(int baseSize) {
		ArrayList<Rule> randomRules = new ArrayList<Rule>();
		int observationsSize = PacManObservations.values().length;
		int actionsSize = PacManHighAction.values().length - 1;

		// For each of the rules in the rule base
		for (int s = 0; s < baseSize; s++) {
			ArrayList<PacManObservations> obs = new ArrayList<PacManObservations>();
			ArrayList<Double> vals = new ArrayList<Double>();
			PacManHighAction preAction = null;

			int numIters = random_.nextInt(2);
			// Number of iterations
			for (int i = 0; i <= numIters; i++) {
				// Choose a random set, either observations or actions
				int index = random_.nextInt(observationsSize + actionsSize);
				if (index < observationsSize) {
					// Choose an observation
					PacManObservations observation = PacManObservations
							.values()[index];
					obs.add(observation);
					index = random_.nextInt(observation.getSetOfVals().length);
					vals.add(observation.getSetOfVals()[index]);
				} else {
					// Choose an action
					index -= observationsSize;
					preAction = PacManHighAction.values()[index];
				}
			}

			PacManHighAction action = PacManHighAction.values()[random_
					.nextInt(actionsSize)];
			// Creating the rule
			if (preAction != null) {
				// Rule has an action
				if (obs.size() == 0) {
					// Just an action
					randomRules.add(new Rule(preAction, random_.nextBoolean(),
							action, random_.nextBoolean()));
				} else {
					randomRules.add(new Rule(obs.get(0), random_.nextBoolean(),
							vals.get(0), preAction, random_.nextBoolean(),
							action, random_.nextBoolean()));
				}
			} else {
				if (obs.size() == 1) {
					randomRules.add(new Rule(obs.get(0), random_.nextBoolean(),
							vals.get(0), action, random_.nextBoolean()));
				} else {
					randomRules.add(new Rule(obs.get(0), random_.nextBoolean(),
							vals.get(0), obs.get(1), random_.nextBoolean(),
							vals.get(1), action, random_.nextBoolean()));
				}
			}
		}
		return randomRules;
	}

	/**
	 * Gets the rule at index.
	 * 
	 * @param index
	 *            The index to get the rule from.
	 * @param slot
	 *            The slot to get the rule from.
	 * @return The Rule or null.
	 */
	public Rule getRule(int index, int slot) {
		if (slot >= rules_.length)
			slot = 0;
		if (index >= rules_[slot].size())
			return null;
		return rules_[slot].get(index);
	}

	/**
	 * Gets all the rules in this instance of the rulebase.
	 * 
	 * @param slot
	 *            The slot to get the rule from.
	 * 
	 * @return The rules.
	 */
	public Collection<Rule> getRules(int slot) {
		if (slot >= rules_.length)
			slot = 0;
		return rules_[slot];
	}

	/**
	 * Gets the index of this rule.
	 * 
	 * @param rule
	 *            The rule to get the index for.
	 * @param slot
	 *            The slot to get the rule from.
	 * @return The index or -1 if not present.
	 */
	public int indexOf(Rule rule, int slot) {
		if (slot >= rules_.length)
			slot = 0;
		return rules_[slot].indexOf(rule);
	}

	/**
	 * Gets the rule base instance.
	 * 
	 * @return The rule base instance or null if not yet initialised.
	 */
	public static RuleBase getInstance() {
		return instance_;
	}

	/**
	 * Initialises the instance as a hand coded rule base, or a set of random
	 * rule bases.
	 * 
	 * @param handCoded
	 *            If the rule base uses hand coded rules.
	 * @param policySize
	 *            The number of rule bases to generate if the rules are random.
	 */
	public static void initInstance(boolean handCoded, int policySize) {
		instance_ = new RuleBase(handCoded, policySize);
	}

	/**
	 * Initialises the rule base to use a set rule base.
	 * 
	 * @param ruleBaseFile
	 *            The file containing the rules used in the rule base.
	 */
	public static void initInstance(File ruleBaseFile) {
		instance_ = new RuleBase(ruleBaseFile);
	}
}
