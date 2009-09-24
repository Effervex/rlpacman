package rlPacMan;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import rlPacMan.Condition.ConditionObject;
import rlPacMan.ObservationCondition.ValuedConditionObject;
import rlPacMan.PacManAction.PacManActionSet;
import rlPacMan.PacManObservation.PacManObservationSet;

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

	/** The distance constant used during the reintegration process. */
	private static final float DIST_CONSTANT = 0.4f;

	/** The constant values for regeneration strategies. */
	private static final byte INDIVIDUAL = 0;
	private static final byte SINGLE = 1;
	private static final byte PRIORITY = 2;

	/** The instance. */
	private static RuleBase instance_;

	/** The cross-entropy generators for the rules within the policy. */
	private ProbabilityDistribution<Rule>[] ruleGenerators_;

	/** The cross-entropy generators for the conditions within the rules. */
	private ProbabilityDistribution<Condition>[] conditionGenerators_;

	/** The cross-entropy generators for the actions within the rules. */
	private ProbabilityDistribution<ActionCondition>[] actionsGenerators_;

	/** The random number generator. */
	private Random random_ = new Random();

	/** The regeneration strategy to use. */
	private byte regenerationStrategy_ = INDIVIDUAL;

	/** Holding variables until the post-update operation is called. */
	private int[][] conditionCounts_;
	private int[][] actionCounts_;
	private int[][] totalCount_;

	/** The file the initial rulebase is read from. */
	private final File initialFile_;

	/** The class prefix for the experiment. */
	private String classPrefix_;

	/**
	 * A private constructor for generating a rule base from random.
	 * 
	 * @param ruleBases
	 *            The number of rule bases to generate if random.
	 * @param classPrefix
	 *            The class prefix to the environment classes.
	 */
	private RuleBase(int ruleBases, String classPrefix) {
		classPrefix_ = classPrefix;
		ruleGenerators_ = new ProbabilityDistribution[ruleBases];
		for (int i = 0; i < ruleBases; i++) {
			ruleGenerators_[i] = new ProbabilityDistribution<Rule>();
			ruleGenerators_[i].addAll(generateRandomRules(RANDOM_RULE_NUMBER));
		}

		initialFile_ = null;

		initialiseConditionGenerators(ruleBases, classPrefix);
	}

	/**
	 * A private constructor for creating a rule base from file.
	 * 
	 * @param ruleBaseFile
	 *            The file from which to load the rules.
	 */
	private RuleBase(File ruleBaseFile) {
		initialFile_ = ruleBaseFile;
		String prefix = loadRulesFromFile(initialFile_);
		initialiseConditionGenerators(ruleGenerators_.length, prefix);
	}

	/**
	 * Resets the instance to it's default values.
	 */
	public void resetInstance() {
		// Resetting the rule values
		if (initialFile_ != null) {
			loadRulesFromFile(initialFile_);
		}
		for (ProbabilityDistribution<Rule> pd : ruleGenerators_) {
			pd.resetProbs();
		}
		// Resetting the condition and action values
		if (conditionGenerators_ != null) {
			for (ProbabilityDistribution<Condition> pd : conditionGenerators_) {
				pd.resetProbs();
			}
		}
		if (actionsGenerators_ != null) {
			for (ProbabilityDistribution<ActionCondition> pd : actionsGenerators_) {
				pd.resetProbs();
			}
		}
	}

	/**
	 * Initialises the condition generators.
	 */
	private void initialiseConditionGenerators(int ruleBases, String classPrefix) {
		if (regenerationStrategy_ == SINGLE) {
			ruleBases = 1;
		} else if (regenerationStrategy_ == PRIORITY) {
			ruleBases = ActionSwitch.NUM_PRIORITIES;
		}
		conditionGenerators_ = new ProbabilityDistribution[ruleBases];
		actionsGenerators_ = new ProbabilityDistribution[ruleBases];

		// Adding the conditions to the first generator.
		conditionGenerators_[0] = new ProbabilityDistribution<Condition>();
		actionsGenerators_[0] = new ProbabilityDistribution<ActionCondition>();
		try {
			addConditions(conditionGenerators_[0], actionsGenerators_[0],
					classPrefix);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Cloning the initialised generator.
		for (int i = 1; i < ruleBases; i++) {
			conditionGenerators_[i] = conditionGenerators_[0].clone();
			actionsGenerators_[i] = actionsGenerators_[0].clone();
		}

		// May need holding variables during the update process.
		if (regenerationStrategy_ == PRIORITY) {
			conditionCounts_ = new int[ActionSwitch.NUM_PRIORITIES][];
			actionCounts_ = new int[ActionSwitch.NUM_PRIORITIES][];
			totalCount_ = new int[ActionSwitch.NUM_PRIORITIES][2];
		} else if (regenerationStrategy_ == SINGLE) {
			conditionCounts_ = new int[1][];
			actionCounts_ = new int[1][];
			totalCount_ = new int[1][2];
		}
	}

	/**
	 * Adds all possible conditions to the generator sets.
	 * 
	 * @param obsGenerator
	 *            The observation generator to add to.
	 * @param actGenerator
	 *            the action generator to add to.
	 */
	private void addConditions(ProbabilityDistribution<Condition> obsGenerator,
			ProbabilityDistribution<ActionCondition> actGenerator,
			String classPrefix) {
		// Work out the base weight for each item
		// Note the 2 is because each

		ConditionObject[] observationVals = ObservationCondition
				.getObservationValues(classPrefix);
		ConditionObject[] actionVals = ActionCondition
				.getActionValues(classPrefix);

		double baseWeight = 1.0 / (2 * (observationVals.length + actionVals.length));

		for (ConditionObject cond : observationVals) {
			ValuedConditionObject obs = (ValuedConditionObject) cond;
			double thisWeight = baseWeight / obs.getSetOfVals().length;
			// For each of the double values, add the observation with a
			// decreased weight
			for (double obsVal : obs.getSetOfVals()) {
				ObservationCondition observation = ObservationCondition
						.createObservation(classPrefix, obs, false, obsVal);
				obsGenerator.add(observation, thisWeight);

				observation = ObservationCondition.createObservation(
						classPrefix, obs, true, obsVal);
				obsGenerator.add(observation, thisWeight);
			}
		}

		double actionWeight = 1.0 / (2 * actionVals.length);
		// Adding the actions
		for (ConditionObject act : actionVals) {
			ActionCondition action = ActionCondition.createAction(classPrefix,
					act, false);
			obsGenerator.add(action, baseWeight);
			actGenerator.add(action, actionWeight);

			action = ActionCondition.createAction(classPrefix, act, true);
			obsGenerator.add(action, baseWeight);
			actGenerator.add(action, actionWeight);
		}

		// Check the values sum to one
		if (!obsGenerator.sumsToOne())
			obsGenerator.normaliseProbs();
		if (!actGenerator.sumsToOne())
			actGenerator.normaliseProbs();
	}

	/**
	 * Load the rules from a file.
	 * 
	 * @param ruleBaseFile
	 *            The file to load the rules from.
	 * @return The prefix of the experiment classes used to generate the rules.
	 */
	private String loadRulesFromFile(File ruleBaseFile) {
		ArrayList<ProbabilityDistribution<Rule>> ruleBases = new ArrayList<ProbabilityDistribution<Rule>>();
		String classPrefix = null;
		try {
			FileReader reader = new FileReader(ruleBaseFile);
			BufferedReader bf = new BufferedReader(reader);

			String input = bf.readLine();
			// First, read the environment class prefix
			classPrefix = input;
			if (classPrefix == null)
				throw new Exception("No class prefix found!");

			// Then read the rules in.
			while (((input = bf.readLine()) != null) && (!input.equals(""))) {
				ProbabilityDistribution<Rule> ruleBase = new ProbabilityDistribution<Rule>();
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

			ruleGenerators_ = ruleBases
					.toArray(new ProbabilityDistribution[ruleBases.size()]);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return classPrefix;
	}

	/**
	 * Save rules to a file.
	 * 
	 * @param ruleBaseFile
	 *            The file to save the rules to.
	 */
	public void saveRulesToFile(File ruleBaseFile, String classPrefix) {
		try {
			if (!ruleBaseFile.exists())
				ruleBaseFile.createNewFile();

			FileWriter writer = new FileWriter(ruleBaseFile);
			BufferedWriter bf = new BufferedWriter(writer);

			// First, write the class prefix
			bf.write(classPrefix + "\n");

			// For each of the rule bases
			for (int i = 0; i < ruleGenerators_.length; i++) {
				// For each of the rules
				for (Rule r : ruleGenerators_[i]) {
					bf.write(r.toParseableString() + RULE_DELIMITER);
				}
				bf.write("\n");
			}

			System.out.println("Random rulebases saved to: " + ruleBaseFile);

			bf.close();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes the generators out to file.
	 * 
	 * @param buf
	 *            The buffered writer to use.
	 * @throws Exception
	 *             If something goes awry...
	 */
	public void writeGenerators(BufferedWriter buf) throws Exception {
		StringBuffer strBuffer = new StringBuffer();

		// For each of the rule generators
		for (int slot = 0; slot < ruleGenerators_.length; slot++) {
			strBuffer = new StringBuffer();
			// For each rule within the generators.
			for (int i = 0; i < ruleGenerators_[slot].size(); i++) {
				strBuffer.append(ruleGenerators_[slot].getProb(i)
						+ CrossEntropyExperiment.ELEMENT_DELIMITER);
			}
			strBuffer.append("\n");
			buf.write(strBuffer.toString());
		}
	}

	/**
	 * Reads the rule distributions from file.
	 * 
	 * @param buf
	 *            The buffered reader to use.
	 * @throws Exception
	 *             If something goes awry...
	 */
	public void readGenerators(BufferedReader buf) throws Exception {
		// For each of the generators
		for (int s = 0; s < ruleGenerators_.length; s++) {
			String[] split = buf.readLine().split(
					CrossEntropyExperiment.ELEMENT_DELIMITER);
			// For each rule within the generators.
			for (int i = 0; i < split.length; i++) {
				ruleGenerators_[s].set(i, Double.parseDouble(split[i]));
			}
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
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.CONSTANT,
				ObservationCondition.GREATER_EQ_THAN, 0), new PacManAction(
				PacManActionSet.TO_DOT, true)));
		// Go to centre of dots
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.CONSTANT,
				ObservationCondition.GREATER_EQ_THAN, 0), new PacManAction(
				PacManActionSet.TO_CENTRE_OF_DOTS, true)));
		// From nearest ghost 1
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.LESS_THAN, 3), new PacManAction(
				PacManActionSet.FROM_GHOST, true)));
		// From nearest ghost 2
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.LESS_THAN, 4), new PacManAction(
				PacManActionSet.FROM_GHOST, true)));
		// From nearest ghost 3
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.LESS_THAN, 5), new PacManAction(
				PacManActionSet.FROM_GHOST, true)));
		// Stop running from nearest ghost 1
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.GREATER_EQ_THAN, 5), new PacManAction(
				PacManActionSet.FROM_GHOST, false)));
		// Stop running from nearest ghost 2
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.GREATER_EQ_THAN, 6), new PacManAction(
				PacManActionSet.FROM_GHOST, false)));
		// Stop running from nearest ghost 3
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.GREATER_EQ_THAN, 7), new PacManAction(
				PacManActionSet.FROM_GHOST, false)));
		// Towards safe junction
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.CONSTANT,
				ObservationCondition.GREATER_EQ_THAN, 0), new PacManAction(
				PacManActionSet.TO_SAFE_JUNCTION, true)));
		// Towards maximally safe junction 1
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.LESS_THAN, 3), new PacManAction(
				PacManActionSet.TO_SAFE_JUNCTION, true)));
		// Towards maximally safe junction 2
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.LESS_THAN, 2), new PacManAction(
				PacManActionSet.TO_SAFE_JUNCTION, true)));
		// Maximally safe junction off 1
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.GREATER_EQ_THAN, 3), new PacManAction(
				PacManActionSet.TO_SAFE_JUNCTION, false)));
		// Safe to stop running 1
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.GREATER_EQ_THAN, 3), new PacManAction(
				PacManActionSet.FROM_GHOST, false)));
		// Maximally safe junction off 2
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.GREATER_EQ_THAN, 5), new PacManAction(
				PacManActionSet.TO_SAFE_JUNCTION, false)));
		// Safe to stop running 2
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.GREATER_EQ_THAN, 5), new PacManAction(
				PacManActionSet.FROM_GHOST, false)));
		// Keep on moving from ghosts
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.CONSTANT,
				ObservationCondition.GREATER_EQ_THAN, 0), new PacManAction(
				PacManActionSet.FROM_GHOST, true)));
		// Eat edible ghosts
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.CONSTANT,
				ObservationCondition.GREATER_EQ_THAN, 0), new PacManAction(
				PacManActionSet.TO_ED_GHOST, true)));
		// Ghost coming, chase powerdots
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.LESS_THAN, 4), new PacManAction(
				PacManActionSet.TO_POWER_DOT, true)));
		// If edible ghosts, don't chase power dots
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_ED_GHOST,
				ObservationCondition.LESS_THAN, 99), new PacManAction(
				PacManActionSet.TO_POWER_DOT, false)));
		// If edible ghosts and we're close to a powerdot, move away from it
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_ED_GHOST,
				ObservationCondition.LESS_THAN, 99), new PacManObservation(
				PacManObservationSet.NEAREST_POWER_DOT,
				ObservationCondition.LESS_THAN, 5), new PacManAction(
				PacManActionSet.FROM_POWER_DOT, true)));
		// If edible ghosts, move away from powerdots
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_ED_GHOST,
				ObservationCondition.LESS_THAN, 99), new PacManAction(
				PacManActionSet.FROM_POWER_DOT, true)));
		// If no edible ghosts, stop moving from powerdots
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_ED_GHOST,
				ObservationCondition.GREATER_EQ_THAN, 99), new PacManAction(
				PacManActionSet.FROM_POWER_DOT, false)));
		// If no edible ghosts, chase powerdots
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_ED_GHOST,
				ObservationCondition.GREATER_EQ_THAN, 99), new PacManAction(
				PacManActionSet.TO_POWER_DOT, true)));
		// If we're close to a ghost and powerdot, chase the powerdot 1
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_POWER_DOT,
				ObservationCondition.LESS_THAN, 2), new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.LESS_THAN, 5), new PacManAction(
				PacManActionSet.TO_POWER_DOT, true)));
		// If we're close to a ghost and powerdot, chase the powerdot 2
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_POWER_DOT,
				ObservationCondition.LESS_THAN, 4), new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.LESS_THAN, 5), new PacManAction(
				PacManActionSet.TO_POWER_DOT, true)));
		// In the clear
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_GHOST,
				ObservationCondition.GREATER_EQ_THAN, 7),
				new PacManObservation(PacManObservationSet.MAX_JUNCTION_SAFETY,
						ObservationCondition.GREATER_EQ_THAN, 4),
				new PacManAction(PacManActionSet.FROM_GHOST, false)));
		// Ghosts are not close, eat a dot 1
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.GHOST_DENSITY,
				ObservationCondition.LESS_THAN, 1.5), new PacManObservation(
				PacManObservationSet.NEAREST_POWER_DOT,
				ObservationCondition.LESS_THAN, 5), new PacManAction(
				PacManActionSet.FROM_POWER_DOT, true)));
		// Far from power dot, stop running
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_POWER_DOT,
				ObservationCondition.GREATER_EQ_THAN, 10), new PacManAction(
				PacManActionSet.FROM_POWER_DOT, false)));
		// Ghosts are too spread, move away from power dot
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.TOTAL_DIST_TO_GHOSTS,
				ObservationCondition.GREATER_EQ_THAN, 30), new PacManAction(
				PacManActionSet.FROM_POWER_DOT, true)));
		// Unsafe junction, run from ghosts 1
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.LESS_THAN, 3), new PacManAction(
				PacManActionSet.FROM_GHOST, true)));
		// Unsafe junction, run from ghosts 2
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.LESS_THAN, 2), new PacManAction(
				PacManActionSet.FROM_GHOST, true)));
		// Unsafe junction, run from ghosts 3
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.MAX_JUNCTION_SAFETY,
				ObservationCondition.LESS_THAN, 1), new PacManAction(
				PacManActionSet.FROM_GHOST, true)));
		// Move from ghost centre
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.CONSTANT,
				ObservationCondition.GREATER_EQ_THAN, 0), new PacManAction(
				PacManActionSet.FROM_GHOST_CENTRE, true)));
		// Stop chasing edible ghosts if none
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_ED_GHOST,
				ObservationCondition.GREATER_EQ_THAN, 99), new PacManAction(
				PacManActionSet.TO_ED_GHOST, false)));
		// Chasing edible ghosts
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_ED_GHOST,
				ObservationCondition.LESS_THAN, 99), new PacManAction(
				PacManActionSet.TO_ED_GHOST, true)));
		// If not running from powerdot, move towards it
		handRules.add(new Rule(new PacManAction(PacManActionSet.FROM_POWER_DOT,
				false), new PacManAction(PacManActionSet.TO_POWER_DOT, true)));
		// If there is a fruit, chase it
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_FRUIT,
				ObservationCondition.LESS_THAN, 99), new PacManAction(
				PacManActionSet.TO_FRUIT, true)));
		// If there isn't a fruit, stop chasing it
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_FRUIT,
				ObservationCondition.GREATER_EQ_THAN, 99), new PacManAction(
				PacManActionSet.TO_FRUIT, false)));
		// If ghosts are edible and not flashing, chase them
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.NEAREST_ED_GHOST,
				ObservationCondition.LESS_THAN, 99), new PacManObservation(
				PacManObservationSet.GHOSTS_FLASHING,
				ObservationCondition.LESS_THAN, 1), new PacManAction(
				PacManActionSet.TO_ED_GHOST, true)));
		// If the ghosts are flashing, stop chasing them
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.GHOSTS_FLASHING,
				ObservationCondition.GREATER_EQ_THAN, 1), new PacManAction(
				PacManActionSet.TO_ED_GHOST, false)));
		// If the ghosts are flashing, run from them
		handRules.add(new Rule(new PacManObservation(
				PacManObservationSet.GHOSTS_FLASHING,
				ObservationCondition.GREATER_EQ_THAN, 1), new PacManAction(
				PacManActionSet.FROM_GHOST, true)));

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

		// For each of the rules in the rule base
		for (int s = 0; s < baseSize; s++) {
			randomRules.add(generateRule(false, 0, classPrefix_));
		}
		return randomRules;
	}

	/**
	 * Generates a random rule using the set of observations and actions. A rule
	 * can consist of 1 or 2 conditions.
	 * 
	 * @param useDistributions
	 *            Whether to use the observed distributions of the rules.
	 * @param ruleSlot
	 *            The rule slot number to generate a rule from.
	 * @param classPrefix
	 *            The class prefix from which we generate rules.
	 * @return The randomly generated rule.
	 */
	private Rule generateRule(boolean useDistributions, int ruleSlot,
			String classPrefix) {
		ConditionObject[] observationVals = ObservationCondition
				.getObservationValues(classPrefix);
		ConditionObject[] actionVals = ActionCondition
				.getActionValues(classPrefix);
		int observationsSize = observationVals.length;
		int actionsSize = actionVals.length;

		ArrayList<ObservationCondition> obs = new ArrayList<ObservationCondition>();
		ActionCondition preAction = null;

		int numIters = random_.nextInt(2);
		// Number of iterations
		for (int i = 0; i <= numIters; i++) {
			// If using the distributions, simply sample.
			if (useDistributions) {
				Condition cond = conditionGenerators_[getRegenIndex(ruleSlot)]
						.sample();
				if (cond instanceof ActionCondition)
					preAction = (ActionCondition) cond;
				else {
					ObservationCondition observation = (ObservationCondition) cond;
					obs.add(observation);
				}
			} else {
				// Choose a random set, either observations or actions
				int index = random_.nextInt(observationsSize + actionsSize);
				if (index < observationsSize) {
					// Choose an observation
					ValuedConditionObject condition = (ValuedConditionObject) observationVals[index];
					index = random_.nextInt(condition.getSetOfVals().length);
					double value = condition.getSetOfVals()[index];

					ObservationCondition observation = ObservationCondition
							.createObservation(classPrefix, condition, random_
									.nextBoolean(), value);
					obs.add(observation);
				} else {
					// Choose an action
					index -= observationsSize;
					preAction = ActionCondition.createAction(classPrefix,
							actionVals[index], random_.nextBoolean());
				}
			}
		}

		ActionCondition action = null;
		// Can just sample if using a distribution
		if (useDistributions) {
			action = actionsGenerators_[getRegenIndex(ruleSlot)]
					.sample();
		} else {
			action = ActionCondition.createAction(classPrefix, actionVals[random_
					.nextInt(actionsSize)], random_.nextBoolean());
		}

		// Creating the rule
		if (preAction != null) {
			// Rule has an action
			if (obs.size() == 0) {
				// Just an action
				return new Rule(preAction, action);
			} else {
				return new Rule(obs.get(0), preAction, action);
			}
		} else {
			if (obs.size() == 1) {
				return new Rule(obs.get(0), action);
			} else {
				return new Rule(obs.get(0), obs.get(1), action);
			}
		}
	}

	/**
	 * Updates one of the rule distributions using the cross-entropy method.
	 * 
	 * @param distIndex
	 *            The index of the distribution.
	 * @param numSamples
	 *            The number of samples used for the counts.
	 * @param counts
	 *            The counts of each of the elements.
	 * @param offsetIndex
	 *            The starting index of the counts.
	 * @param stepSize
	 *            The step size for the update.
	 * @param valueModifier
	 *            The value modifier for the update.
	 */
	public void updateDistribution(int distIndex, int[] counts,
			int offsetIndex, double stepSize, int valueModifier) {
		// Updates the distribution.
		ruleGenerators_[distIndex].updateDistribution(counts[0], counts,
				offsetIndex, stepSize, valueModifier);

		// Only note this down if we have generators
		if (conditionGenerators_ != null && actionsGenerators_ != null) {
			// We also want to note down the conditions and actions within the
			// elite
			// rules.
			int[] conditionCounts = new int[conditionGenerators_[getRegenIndex(distIndex)]
					.size()];
			int[] actionCounts = new int[actionsGenerators_[getRegenIndex(distIndex)]
					.size()];
			int[] totalCounts = calculateConditionActionCounts(counts,
					offsetIndex, conditionCounts, actionCounts, distIndex);
			// If we're performing individual updates, go ahead
			if (regenerationStrategy_ == INDIVIDUAL) {
				// Update the distributions
				conditionGenerators_[getRegenIndex(distIndex)]
						.updateDistribution(totalCounts[0], conditionCounts, 0,
								stepSize, 1);
				actionsGenerators_[getRegenIndex(distIndex)]
						.updateDistribution(totalCounts[1], actionCounts, 0,
								stepSize, 1);
			} else {
				// Otherwise, store the counts and totals and perform the update
				// in the post-update operations.
				conditionCounts_[getRegenIndex(distIndex)] = sumArrays(
						conditionCounts_[getRegenIndex(distIndex)],
						conditionCounts);
				actionCounts_[getRegenIndex(distIndex)] = sumArrays(
						actionCounts_[getRegenIndex(distIndex)], actionCounts);
				totalCount_[getRegenIndex(distIndex)] = sumArrays(
						totalCount_[getRegenIndex(distIndex)], totalCounts);
			}
		}
	}

	/**
	 * Performs any post-update operations on the rule distributions.
	 * 
	 * @param thisAverage
	 *            The current running average of the samples.
	 * @param ratioChanged
	 *            The ratio of rules to modify.
	 * @return The new ratio changed value.
	 */
	public float postUpdateOperations(float thisAverage, float ratioChanged,
			double stepSize) {
		// Only update the condition generators if they exist
		if (conditionGenerators_ != null && actionsGenerators_ != null) {
			// Update the condition and action counts if needed
			if (regenerationStrategy_ != INDIVIDUAL) {
				for (int i = 0; i < totalCount_.length; i++) {
					conditionGenerators_[i].updateDistribution(
							totalCount_[i][0], conditionCounts_[i], 0,
							stepSize, 1);
					actionsGenerators_[i].updateDistribution(totalCount_[i][1],
							actionCounts_[i], 0, stepSize, 1);
				}
			}
		}

		// if (thisAverage < 0) {
		// Modify the distributions by reintegration rules from neighbouring
		// distributions.
		// ruleGenerators_ = reintegrateRules(ratioChanged, DIST_CONSTANT);
		if (conditionGenerators_ != null && actionsGenerators_ != null)
			regenerateRules(ratioChanged);

		// ratioShared_ *= slotDecayRate_;
		// }
		return ratioChanged;
	}

	/**
	 * Sums the arrays into
	 * 
	 * @param conditionCounts
	 */
	private int[] sumArrays(int[] summingArray, int[] addedArray) {
		if (summingArray == null) {
			summingArray = addedArray;
		} else {
			for (int i = 0; i < addedArray.length; i++) {
				summingArray[i] += addedArray[i];
			}
		}
		return summingArray;
	}

	/**
	 * Calculates the counts of the conditions and actions within the elite
	 * rules.
	 * 
	 * @param ruleCounts
	 *            The counts of the rules among the elite sample. Used to
	 *            determine what conditions and actions are present in the elite
	 *            samples.
	 * @param offsetIndex
	 *            The offset index for the rule counts.
	 * @param conditionCounts
	 *            The to-be-filled array of condition counts.
	 * @param actionCounts
	 *            The to-be-filled array of action counts.
	 * @param distIndex
	 *            The index of the distribution.
	 */
	private int[] calculateConditionActionCounts(int[] ruleCounts,
			int offsetIndex, int[] conditionCounts, int[] actionCounts,
			int distIndex) {
		int[] total = new int[2];
		// For each of the rules, use their counts to go towards the conditions
		// and actions within.
		for (int i = 0; i < ruleGenerators_[distIndex].size(); i++) {
			int ruleCount = ruleCounts[i + offsetIndex];
			// If this rule is used at least once, note its conditions and
			// actions down.
			if (ruleCount > 0) {
				Rule thisRule = ruleGenerators_[distIndex].getElement(i);
				Condition[] conditions = thisRule.getConditions();
				// Note the condition(s) and store their counts
				for (Condition cond : conditions) {
					conditionCounts[conditionGenerators_[getRegenIndex(distIndex)]
							.indexOf(cond)] += ruleCount;
					total[0] += ruleCount;
				}
				// Note the action
				actionCounts[actionsGenerators_[getRegenIndex(distIndex)]
						.indexOf(thisRule.getAction())] += ruleCount;
				total[1] += ruleCount;
			}
		}

		return total;
	}

	/**
	 * Reintegrates the rules from neighbouring distributions.
	 * 
	 * @param sharedRatio
	 *            The ratio of rules to share from the rules within the
	 *            distributions.
	 * @param distConstant
	 *            The constant to apply to the share ratio based on distance
	 *            from the distribution.
	 * @return The new rule distribution.
	 */
	@SuppressWarnings("unchecked")
	private ProbabilityDistribution<Rule>[] reintegrateRules(float sharedRatio,
			float distConstant) {
		// Has to use forward step distribution so full sweeps can be performed.
		ProbabilityDistribution<Rule>[] newDistributions = new ProbabilityDistribution[ruleGenerators_.length];

		// Run through each distribution
		for (int slot = 0; slot < newDistributions.length; slot++) {
			// Clone the old distribution
			newDistributions[slot] = ruleGenerators_[slot].clone();

			int thisPriority = Policy
					.getPriority(slot, newDistributions.length);

			// Get rules from either side of this slot.
			ArrayList<Rule> sharedRules = new ArrayList<Rule>();
			int sideModifier = -1;
			// Cover both sides of this slot
			do {
				// Loop variables
				int neighbourSlot = sideModifier + slot;
				int neighbourPriority = Policy.getPriority(neighbourSlot,
						newDistributions.length);
				int numRules = Math.round(sharedRatio * size());

				// Only use valid slots (same priority)
				while ((neighbourSlot >= 0) // Above 0
						&& (neighbourSlot < newDistributions.length) // Below
						// max
						&& (neighbourPriority == thisPriority) // Same priority
						&& (numRules > 0)) // Getting at least one rule
				{
					// Get the n best rules
					sharedRules.addAll(ruleGenerators_[neighbourSlot]
							.getNBest(numRules));

					// Update the variables
					neighbourSlot += sideModifier;
					neighbourPriority = Policy.getPriority(neighbourSlot,
							newDistributions.length);
					numRules *= distConstant;
				}

				sideModifier *= -1;
			} while (sideModifier == 1);

			// Now replace the N worst rules from this distribution with the N
			// shared rules.
			double reintegralProbability = newDistributions[slot]
					.removeNWorst(sharedRules.size());
			newDistributions[slot].addAll(sharedRules, reintegralProbability);
			newDistributions[slot].normaliseProbs();
		}

		// Save the rules to file as they will not be standard
		saveRulesToFile(new File("reintegralRuleBase.txt"), classPrefix_);

		return newDistributions;
	}

	/**
	 * Regenerates new rules after removing a number of bad rules, increasing
	 * the likelihood of having a useful rule set. This method is best used with
	 * random rules.
	 * 
	 * @param regenRatio
	 *            The ratio of rules to be regenerated.
	 */
	private void regenerateRules(float regenRatio) {
		int numRegened = (int) (size() * regenRatio);
		// For each slot
		for (int slot = 0; slot < ruleGenerators_.length; slot++) {
			double regenProbability = ruleGenerators_[slot]
					.removeNWorst(numRegened);
			// For each newly generated rule
			for (int i = 0; i < numRegened; i++) {
				ruleGenerators_[slot].add(
						generateRule(true, slot, classPrefix_),
						regenProbability);
			}
			ruleGenerators_[slot].normaliseProbs();
		}

		// Save the rules to file
		saveRulesToFile(new File("regeneratedRuleBase.txt"), classPrefix_);
	}

	/**
	 * Gets the regeneration index, taking into account the regeneration
	 * strategy.
	 * 
	 * @param distIndex
	 *            The initial index to be modified.
	 * @return A value dependent on the regeneration strategy.
	 */
	private int getRegenIndex(int distIndex) {
		switch (regenerationStrategy_) {
		case INDIVIDUAL:
			return distIndex;
		case SINGLE:
			return 0;
		case PRIORITY:
			return Policy.getPriority(distIndex, ruleGenerators_.length);
		}
		return -1;
	}

	/**
	 * Normalises the rule distributions.
	 */
	public void normaliseDistributions() {
		for (ProbabilityDistribution<Rule> pd : ruleGenerators_) {
			pd.normaliseProbs();
		}
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
		if (slot >= ruleGenerators_.length)
			slot = 0;
		if (index >= ruleGenerators_[slot].size())
			return null;
		return ruleGenerators_[slot].getElement(index);
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
		if (slot >= ruleGenerators_.length)
			slot = 0;
		return ruleGenerators_[slot];
	}

	/**
	 * Gets the rule generator from the ith slot.
	 * 
	 * @param i
	 *            The slot to get the generator from.
	 * @return The rule generator.
	 */
	public ProbabilityDistribution<Rule> getRuleGenerator(int i) {
		return ruleGenerators_[i];
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
		if (slot >= ruleGenerators_.length)
			slot = 0;
		return ruleGenerators_[slot].indexOf(rule);
	}

	/**
	 * Gets the action values used in this experiment.
	 * 
	 * @param classPrefix
	 *            The class prefix used in the environment.
	 * @return The action values used.
	 * @throws Exception
	 *             If something goes awry...
	 */
	public ConditionObject[] getActionValues(String classPrefix)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
		return ((ActionCondition) Class.forName(
				classPrefix + ActionCondition.CLASS_SUFFIX).newInstance())
				.getEnumValues();
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
	 * Initialises the instance as a set of random rule bases.
	 * 
	 * @param policySize
	 *            The number of rule bases to generate if the rules are random.
	 */
	public static void initInstance(int policySize, String classPrefix) {
		instance_ = new RuleBase(policySize, classPrefix);
		try {
			File ruleBaseFile = new File("ruleBase.txt");
			instance_.saveRulesToFile(ruleBaseFile, classPrefix);

		} catch (Exception e) {
			e.printStackTrace();
		}
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

	/**
	 * The string version of this RuleBase
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < ruleGenerators_.length; i++) {
			buffer.append("Slot " + i + ":\n");
			for (Rule r : ruleGenerators_[i]) {
				buffer.append(" " + r + "\n");
			}
		}
		return buffer.toString();
	}

	/**
	 * Gets the number of rules in the rulebase (within a slot).
	 * 
	 * @return The number of rules per slot.
	 */
	public int size() {
		return ruleGenerators_[0].size();
	}
}
