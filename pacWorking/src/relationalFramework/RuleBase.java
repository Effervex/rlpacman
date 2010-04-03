package relationalFramework;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.mandarax.kernel.ConstantTerm;
import org.mandarax.kernel.Fact;
import org.mandarax.kernel.InferenceEngine;
import org.mandarax.kernel.KnowledgeBase;
import org.mandarax.kernel.LogicFactory;
import org.mandarax.kernel.Prerequisite;
import org.mandarax.kernel.Rule;
import org.mandarax.kernel.Term;
import org.mandarax.kernel.VariableTerm;
import org.mandarax.kernel.meta.JConstructor;
import org.mandarax.reference.DefaultInferenceEngine;

/**
 * A singleton implementation of the current rule base.
 * 
 * @author Samuel J. Sarjant
 */
public class RuleBase {
	/** The delimiter character between rules within the same rule base. */
	public static final String RULE_DELIMITER = "@";

	/** The distance constant used during the reintegration process. */
	private static final float DIST_CONSTANT = 0.4f;

	/** The constant values for regeneration strategies. */
	private static final byte INDIVIDUAL = 0;
	private static final byte SINGLE = 1;
	private static final byte PRIORITY = 2;

	/** The instance. */
	private static RuleBase instance_;

	/** The single logic factory. */
	private LogicFactory factory_;

	/** The single inference engine. */
	private InferenceEngine inferenceEngine_;

	/** The cross-entropy generators for the rules within the policy. */
	private ProbabilityDistribution<GuidedRule>[] ruleGenerators_;

	/** The backup generators to use when unfreezing. */
	private ProbabilityDistribution<GuidedRule>[] backupRuleGenerators_;

	/** The cross-entropy generators for the conditions within the rules. */
	private ProbabilityDistribution<GuidedPredicate>[] conditionGenerators_;

	/** The cross-entropy generators for the actions within the rules. */
	private ProbabilityDistribution<GuidedPredicate>[] actionGenerators_;

	/** The random number generator. */
	private Random random_ = new Random();

	/** The regeneration strategy to use. */
	private byte regenerationStrategy_ = INDIVIDUAL;

	/** Holding variables until the post-update operation is called. */
	private int[][] conditionCounts_;
	private int[][] actionCounts_;
	private int[][] totalCount_;

	/** The file the initial rulebase is read from. */
	private File initialFile_;

	/** The class prefix for the experiment. */
	private String classPrefix_;

	/**
	 * A private constructor for generating a rule base from random.
	 * 
	 * @param classPrefix
	 *            The class prefix to the environment classes.
	 */
	private RuleBase(String classPrefix, int ruleBases) {
		factory_ = LogicFactory.getDefaultFactory();
		inferenceEngine_ = new DefaultInferenceEngine();
		StateSpec.initInstance(classPrefix, factory_);
		classPrefix_ = classPrefix;
		ruleBases = initialiseConditionGenerators(ruleBases, formConditions(),
				formActions());
		ruleGenerators_ = new ProbabilityDistribution[ruleBases];
		for (int i = 0; i < ruleBases; i++) {
			ruleGenerators_[i] = new ProbabilityDistribution<GuidedRule>();
			ruleGenerators_[i].addAll(generateRandomRules(ruleBases));
		}

		initialFile_ = null;
	}

	/**
	 * A private constructor for creating a rule base from file.
	 * 
	 * @param classPrefix
	 *            The class prefix for the environment.
	 * @param ruleBaseFile
	 *            The file from which to load the rules.
	 */
	private void loadRuleBase(String classPrefix, File ruleBaseFile) {
		initialFile_ = ruleBaseFile;
		ruleGenerators_ = loadRulesFromFile(initialFile_,
				conditionGenerators_[0], actionGenerators_[0]);
		initialiseConditionGenerators(ruleGenerators_.length,
				conditionGenerators_[0], actionGenerators_[0]);
	}

	/**
	 * Resets the instance to it's default values.
	 */
	public void resetInstance() {
		for (ProbabilityDistribution<GuidedRule> pd : ruleGenerators_) {
			pd.resetProbs();
		}
		// Resetting the condition and action values
		if (conditionGenerators_ != null) {
			for (ProbabilityDistribution<GuidedPredicate> pd : conditionGenerators_) {
				pd.resetProbs();
			}
		}
		if (actionGenerators_ != null) {
			for (ProbabilityDistribution<GuidedPredicate> pd : actionGenerators_) {
				pd.resetProbs();
			}
		}
		// Resetting the rule values
		if (initialFile_ != null) {
			loadRulesFromFile(initialFile_, conditionGenerators_[0],
					actionGenerators_[0]);
		}
	}

	/**
	 * Initialises the condition generators.
	 * 
	 * @param ruleBases
	 *            The number of rule bases to maintain. Can be determined by
	 *            regeneration strategy or number of conditions.
	 * @param condGenerator
	 *            The distribution containing all possible conditions.
	 * @param actGenerator
	 *            The distribution containing all possible actions.
	 * @return The number of rule bases used.
	 */
	private int initialiseConditionGenerators(int ruleBases,
			ProbabilityDistribution<GuidedPredicate> condGenerator,
			ProbabilityDistribution<GuidedPredicate> actGenerator) {
		if (regenerationStrategy_ == SINGLE) {
			ruleBases = 1;
		} else if (regenerationStrategy_ == PRIORITY) {
			ruleBases = ActionSwitch.NUM_PRIORITIES;
		}

		// Using the max number of conditions/actions for the policy and
		// ruleBase size
		if (ruleBases == 0)
			ruleBases = Math.max(condGenerator.size(), actGenerator.size());
		conditionGenerators_ = new ProbabilityDistribution[ruleBases];
		actionGenerators_ = new ProbabilityDistribution[ruleBases];
		conditionGenerators_[0] = condGenerator;
		actionGenerators_[0] = actGenerator;

		// Cloning the initialised generator.
		for (int i = 1; i < ruleBases; i++) {
			conditionGenerators_[i] = conditionGenerators_[0].clone();
			actionGenerators_[i] = actionGenerators_[0].clone();
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

		return ruleBases;
	}

	/**
	 * Creates all possible conditions from within the StateSpec.
	 * 
	 * @return The conditions generator.
	 */
	private ProbabilityDistribution<GuidedPredicate> formConditions() {
		// Extracting the information from the StateSpec
		StateSpec ss = StateSpec.getInstance();
		KnowledgeBase bk = ss.getBackgroundKnowledge();
		ProbabilityDistribution<GuidedPredicate> obsGenerator = new ProbabilityDistribution<GuidedPredicate>();

		// Adding all the possible conditions
		List<GuidedPredicate> observationPreds = ss.getPredicates();
		// The number of base observation predicates + the empty predicate
		double baseWeight = 1.0 / (observationPreds.size());
		for (GuidedPredicate pred : observationPreds) {
			Collection<GuidedPredicate> loosePredicates = pred
					.createAllLooseInstantiations(bk, factory_,
							inferenceEngine_);
			double thisWeight = baseWeight
					/ (Math.max(1, loosePredicates.size()));
			for (GuidedPredicate loosePred : loosePredicates) {
				obsGenerator.add(loosePred, thisWeight);
			}
		}
		if (!obsGenerator.sumsToOne())
			obsGenerator.normaliseProbs();
		return obsGenerator;
	}

	/**
	 * Creates all possible actions from within the StateSpec.
	 * 
	 * @return The actions generator.
	 */
	private ProbabilityDistribution<GuidedPredicate> formActions() {
		// Extracting the information from the StateSpec
		StateSpec ss = StateSpec.getInstance();
		KnowledgeBase bk = ss.getBackgroundKnowledge();
		ProbabilityDistribution<GuidedPredicate> actGenerator = new ProbabilityDistribution<GuidedPredicate>();

		List<GuidedPredicate> actionPreds = ss.getActions();
		double actionWeight = 1.0 / actionPreds.size();
		// Adding the actions
		for (GuidedPredicate act : actionPreds) {
			Collection<GuidedPredicate> looseActions = act
					.createAllLooseInstantiations(bk, factory_,
							inferenceEngine_);
			double thisWeight = actionWeight
					/ (Math.max(1, looseActions.size()));
			for (GuidedPredicate looseAct : looseActions) {
				actGenerator.add(looseAct, thisWeight);
			}
		}
		if (!actGenerator.sumsToOne())
			actGenerator.normaliseProbs();
		return actGenerator;
	}

	/**
	 * Load the rules from a file.
	 * 
	 * @param ruleBaseFile
	 *            The file to load the rules from.
	 * @param actGenerator
	 * @param condGenerator
	 * @return The rules loaded in.
	 */
	private ProbabilityDistribution<GuidedRule>[] loadRulesFromFile(
			File ruleBaseFile,
			ProbabilityDistribution<GuidedPredicate> condGenerator,
			ProbabilityDistribution<GuidedPredicate> actGenerator) {
		ArrayList<ProbabilityDistribution<GuidedRule>> ruleBases = new ArrayList<ProbabilityDistribution<GuidedRule>>();
		try {
			FileReader reader = new FileReader(ruleBaseFile);
			BufferedReader bf = new BufferedReader(reader);

			String input = bf.readLine();
			if (!input.equals(StateSpec.getInstance().getGoalState().toString())) {
				System.err.println("Environment goal does not match! Crashing...");
				return null;
			}

			// Read the rules in.
			Map<String, Object> constants = StateSpec.getInstance()
					.getConstants();
			while (((input = bf.readLine()) != null) && (!input.equals(""))) {
				ProbabilityDistribution<GuidedRule> ruleBase = new ProbabilityDistribution<GuidedRule>();
				// Split the base into rules
				String[] split = input.split(RULE_DELIMITER);
				// For each rule, add it to the rulebase
				for (int i = 0; i < split.length; i++) {
					Rule rule = StateSpec.getInstance().parseRule(split[i],
							constants);
					ArrayList<GuidedPredicate> condsAct = inferGuidedPreds(
							rule, condGenerator, actGenerator);
					GuidedPredicate action = condsAct
							.remove(condsAct.size() - 1);
					GuidedRule gr = new GuidedRule(rule, condsAct, action);
					ruleBase.add(gr);
				}
				ruleBases.add(ruleBase);
			}

			bf.close();
			reader.close();

			return ruleBases.toArray(new ProbabilityDistribution[ruleBases
					.size()]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Infers GuidedPredicates from a rule - how it was composed.
	 * 
	 * @param rule
	 *            The rule being inferred.
	 * @param condGenerator
	 *            The conditions distribution.
	 * @param actGenerator
	 *            The actions distribution.
	 * @return The guided predicates used to make up the rule, with the action
	 *         predicate at the end.
	 */
	private ArrayList<GuidedPredicate> inferGuidedPreds(Rule rule,
			ProbabilityDistribution<GuidedPredicate> condGenerator,
			ProbabilityDistribution<GuidedPredicate> actGenerator) {
		ArrayList<GuidedPredicate> guidedPreds = new ArrayList<GuidedPredicate>();
		List<Fact> prereqs = rule.getBody();
		
		boolean firstPred = true;
		Collection<Term> usedVariables = new HashSet<Term>();
		for (int f = 0; f <= prereqs.size(); f++) {
			Fact fact = (f < prereqs.size()) ? prereqs.get(f) : rule.getHead();

			int offset = (fact.getPredicate() instanceof JConstructor) ? 1 : 0;
			GuidedPredicate structure = StateSpec.getInstance()
					.getGuidedPredicate(fact.getPredicate().getName());

			if (structure != null) {
				// Creating the pred terms to define the predicate
				PredTerm[] predTerms = new PredTerm[structure.getPredValues().length];

				for (int i = 0; i < predTerms.length; i++) {
					Term matcher = fact.getTerms()[i + offset];

					// If we're dealing with a variable term, it can be TIED
					// or FREE
					if (matcher.isVariable()) {
						VariableTerm varMatcher = (VariableTerm) matcher;
						// If we're at the first predicate, or we've seen
						// the term before, it is tied.
						if ((firstPred) || (usedVariables.contains(matcher))) {
							predTerms[i] = new PredTerm(varMatcher.getName(),
									varMatcher.getType(), PredTerm.TIED);
						} else {
							// It is a newly occurring free variable
							predTerms[i] = new PredTerm(varMatcher.getName(),
									varMatcher.getType(), PredTerm.FREE);
						}

						usedVariables.add(matcher);
					} else {
						// Otherwise, we're dealing with a constant
						predTerms[i] = new PredTerm(((ConstantTerm) matcher)
								.getObject());
					}
				}
				guidedPreds.add(new GuidedPredicate(fact.getPredicate(),
						predTerms));
				firstPred = false;
			}
		}

		return guidedPreds;
	}

	/**
	 * Save rules to a file in the format
	 * 
	 * @param ruleBaseFile
	 *            The file to save the rules to.
	 */
	public void saveRulesToFile(File ruleBaseFile) {
		try {
			if (!ruleBaseFile.exists())
				ruleBaseFile.createNewFile();

			FileWriter writer = new FileWriter(ruleBaseFile);
			BufferedWriter bf = new BufferedWriter(writer);

			bf.write(StateSpec.getInstance().getGoalState() + "\n");
			// For each of the rule bases
			for (int i = 0; i < ruleGenerators_.length; i++) {
				// For each of the rules
				for (GuidedRule r : ruleGenerators_[i]) {
					bf
							.write(StateSpec.encodeRule(r.getRule())
									+ RULE_DELIMITER);
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
	 * Generates random rules.
	 * 
	 * @param baseSize
	 *            The number of random rules to generate.
	 * @return A list of random rules.
	 */
	private ArrayList<GuidedRule> generateRandomRules(int baseSize) {
		ArrayList<GuidedRule> randomRules = new ArrayList<GuidedRule>();

		// For each of the rules in the rule base
		for (int s = 0; s < baseSize; s++) {
			randomRules.add(generateRule(0, classPrefix_));
		}
		return randomRules;
	}

	/**
	 * Generates a random rule using the set of observations and actions. A rule
	 * can consist of 1 or 2 conditions.
	 * 
	 * @param ruleSlot
	 *            The rule slot number to generate a rule from.
	 * @param classPrefix
	 *            The class prefix from which we generate rules.
	 * 
	 * @return The randomly generated rule with the guided predicates that made
	 *         it.
	 */
	private GuidedRule generateRule(int ruleSlot, String classPrefix) {
		// Determining how many (non-type) predicates are in the rule
		int numPrereqs = 0;
		double chance = 1;
		while (random_.nextDouble() < chance) {
			numPrereqs++;
			chance = 1.0 / ((numPrereqs + 1) * (numPrereqs + 1));
		}

		// Sampling the action first, to ensure there are enough conditions to
		// tie to it
		GuidedPredicate action = actionGenerators_[getRegenIndex(ruleSlot)]
				.sample();
		Set<GuidedPredicate> guidedConds = new HashSet<GuidedPredicate>();

		// Number of iterations
		List<Prerequisite> rulePrereqs = new ArrayList<Prerequisite>();
		Set<Term> tiableTerms = new HashSet<Term>();
		int i = 0;
		List<Prerequisite> actPreqs = null;
		do {
			// Sample from the distributions
			GuidedPredicate cond = conditionGenerators_[getRegenIndex(ruleSlot)]
					.sample();
			guidedConds.add(cond);
			Term[] existingTerms = compileTied(tiableTerms, cond
					.getLooseInstantiation());
			// Adding the prereqs, assuming they aren't already
			List<Prerequisite> condPreqs = cond.factify(factory_,
					existingTerms, false, false, tiableTerms);
			for (Prerequisite prereq : condPreqs) {
				if (!rulePrereqs.contains(prereq))
					rulePrereqs.add(prereq);
			}

			// Attempting to factify the action
			existingTerms = compileTied(tiableTerms, action
					.getLooseInstantiation());
			actPreqs = action.factify(factory_, existingTerms, false, true,
					null);
			i++;
		} while ((i < numPrereqs) || (actPreqs == null));

		// Add the type predicates to the condition, and the action prereq as
		// the head
		Fact actionFact = null;
		for (Prerequisite prereq : actPreqs) {
			if (prereq.getPredicate().equals(action.getPredicate())) {
				actionFact = prereq;
			} else if (!rulePrereqs.contains(prereq)) {
				// rulePrereqs.add(prereq);
			}
		}

		// Creating the rule
		if (rulePrereqs.isEmpty())
			return new GuidedRule(factory_.createRule(actionFact), guidedConds,
					action);
		else
			return new GuidedRule(factory_.createRule(rulePrereqs, actionFact),
					guidedConds, action);
	}

	/**
	 * Compiles an array of tied terms.
	 * 
	 * @param tiableTerms
	 *            The set of tiable terms.
	 * @param structure
	 *            The structure of the predicate.
	 * @return The terms to use.
	 */
	private Term[] compileTied(Set<Term> tiableTerms, PredTerm[] looseTerms) {
		if (looseTerms == null)
			return new Term[0];

		Term[] existingTerms = new Term[looseTerms.length];
		Set<Term> stopDuplicates = new HashSet<Term>();

		// Run through the predicate terms
		for (int i = 0; i < looseTerms.length; i++) {
			PredTerm term = looseTerms[i];
			// If we have a tied term, look for tiable terms.
			if (term.getTermType() == PredTerm.TIED) {
				List<Term> possibleTies = new ArrayList<Term>();
				for (Term tiableTerm : tiableTerms) {
					// If the tiable term is of the right type
					if (term.getValueType().isAssignableFrom(
							tiableTerm.getType())) {
						// If the term has not already been used
						if (!stopDuplicates.contains(tiableTerm)) {
							possibleTies.add(tiableTerm);

						}
					}
				}

				// Choose a random swap for the tied term, if we have at least
				// one
				if (possibleTies.isEmpty())
					existingTerms[i] = null;
				else {
					Term existingTerm = possibleTies.get(random_
							.nextInt(possibleTies.size()));
					stopDuplicates.add(existingTerm);
					existingTerms[i] = existingTerm;
				}
			}
		}
		return existingTerms;
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
		if (conditionGenerators_ != null && actionGenerators_ != null) {
			// We also want to note down the conditions and actions within the
			// elite
			// rules.
			int[] conditionCounts = new int[conditionGenerators_[getRegenIndex(distIndex)]
					.size()];
			int[] actionCounts = new int[actionGenerators_[getRegenIndex(distIndex)]
					.size()];
			int[] totalCounts = calculateConditionActionCounts(counts,
					offsetIndex, conditionCounts, actionCounts, distIndex);
			// If we're performing individual updates, go ahead
			if (regenerationStrategy_ == INDIVIDUAL) {
				// Update the distributions
				conditionGenerators_[getRegenIndex(distIndex)]
						.updateDistribution(totalCounts[0], conditionCounts, 0,
								stepSize, 1);
				actionGenerators_[getRegenIndex(distIndex)].updateDistribution(
						totalCounts[1], actionCounts, 0, stepSize, 1);
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
		if (conditionGenerators_ != null && actionGenerators_ != null) {
			// Update the condition and action counts if needed
			if (regenerationStrategy_ != INDIVIDUAL) {
				for (int i = 0; i < totalCount_.length; i++) {
					conditionGenerators_[i].updateDistribution(
							totalCount_[i][0], conditionCounts_[i], 0,
							stepSize, 1);
					actionGenerators_[i].updateDistribution(totalCount_[i][1],
							actionCounts_[i], 0, stepSize, 1);
				}
			}
		}

		// TODO Mutation operators

		// if (thisAverage < 0) {
		// Modify the distributions by reintegration rules from neighbouring
		// distributions.
		// ruleGenerators_ = reintegrateRules(ratioChanged, DIST_CONSTANT);
		if (conditionGenerators_ != null && actionGenerators_ != null)
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
				GuidedRule thisRule = ruleGenerators_[distIndex].getElement(i);
				Collection<GuidedPredicate> conditions = thisRule
						.getConditions();
				// Note the condition(s) and store their counts
				for (GuidedPredicate cond : conditions) {
					conditionCounts[conditionGenerators_[getRegenIndex(distIndex)]
							.indexOf(cond)] += ruleCount;
					total[0] += ruleCount;
				}
				// Note the action
				actionCounts[actionGenerators_[getRegenIndex(distIndex)]
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
	private ProbabilityDistribution<GuidedRule>[] reintegrateRules(
			float sharedRatio, float distConstant) {
		// Has to use forward step distribution so full sweeps can be performed.
		ProbabilityDistribution<GuidedRule>[] newDistributions = new ProbabilityDistribution[ruleGenerators_.length];

		// Run through each distribution
		for (int slot = 0; slot < newDistributions.length; slot++) {
			// Clone the old distribution
			newDistributions[slot] = ruleGenerators_[slot].clone();

			// Get rules from either side of this slot.
			ArrayList<GuidedRule> sharedRules = new ArrayList<GuidedRule>();
			int sideModifier = -1;
			// Cover both sides of this slot
			do {
				// Loop variables
				int neighbourSlot = sideModifier + slot;
				int numRules = Math.round(sharedRatio * size());

				// Only use valid slots (same priority)
				while ((neighbourSlot >= 0) // Above 0
						&& (neighbourSlot < newDistributions.length) // Below
						// max
						&& (numRules > 0)) // Getting at least one rule
				{
					// Get the n best rules
					sharedRules.addAll(ruleGenerators_[neighbourSlot]
							.getNBest(numRules));

					// Update the variables
					neighbourSlot += sideModifier;
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
		saveRulesToFile(new File("reintegralRuleBase.txt"));

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
				ruleGenerators_[slot].add(generateRule(slot, classPrefix_),
						regenProbability);
			}
			ruleGenerators_[slot].normaliseProbs();
		}

		// Save the rules to file
		saveRulesToFile(new File("regeneratedRuleBase.txt"));
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
		for (ProbabilityDistribution<GuidedRule> pd : ruleGenerators_) {
			pd.normaliseProbs();
		}
	}

	/**
	 * Freezes or unfreezes the rule generators contained within. This means
	 * that the generators are bound and the probabilities within are flattened.
	 * 
	 * @param freeze
	 *            If the state is to freeze.
	 */
	public void freezeState(boolean freeze) {
		if (freeze) {
			backupRuleGenerators_ = ruleGenerators_;
			// Clone and bind the generators
			ruleGenerators_ = new ProbabilityDistribution[ruleGenerators_.length];
			for (int i = 0; i < ruleGenerators_.length; i++) {
				ruleGenerators_[i] = backupRuleGenerators_[i].bindProbs(false);
			}
		} else {
			ruleGenerators_ = backupRuleGenerators_;
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
	public GuidedRule getRule(int index, int slot) {
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
	public Collection<GuidedRule> getRules(int slot) {
		if (slot >= ruleGenerators_.length)
			slot = 0;
		return ruleGenerators_[slot];
	}

	/**
	 * Gets the number of slots the ruleGenerator uses.
	 * 
	 * @return the number of slots the rule generator keeps track of.
	 */
	public int getNumSlots() {
		return ruleGenerators_.length;
	}

	/**
	 * Gets the rule generator from the ith slot.
	 * 
	 * @param i
	 *            The slot to get the generator from.
	 * @return The rule generator.
	 */
	public ProbabilityDistribution<GuidedRule> getRuleGenerator(int i) {
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
	public int indexOf(GuidedRule rule, int slot) {
		if (slot >= ruleGenerators_.length)
			slot = 0;
		return ruleGenerators_[slot].indexOf(rule);
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
	 */
	public static void initInstance(String classPrefix) {
		instance_ = new RuleBase(classPrefix, 0);
		try {
			File ruleBaseFile = new File("ruleBase.txt");
			instance_.saveRulesToFile(ruleBaseFile);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initialises the rule base to use a set rule base.
	 * 
	 * @param classPrefix
	 *            The class prefix for the environment.
	 * @param ruleBaseFile
	 *            The file containing the rules used in the rule base.
	 */
	public static void initInstance(String classPrefix, File ruleBaseFile) {
		instance_ = new RuleBase(classPrefix, 1);
		instance_.loadRuleBase(classPrefix, ruleBaseFile);
	}

	/**
	 * The string version of this RuleBase
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < ruleGenerators_.length; i++) {
			buffer.append("Slot " + i + ":\n");
			for (GuidedRule r : ruleGenerators_[i]) {
				buffer.append(" " + r.getRule() + "\n");
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

	/**
	 * Gets the logic factory.
	 * 
	 * @return The logic factory.
	 */
	public LogicFactory getLogicFactory() {
		return factory_;
	}

	/**
	 * Gets the inference engine.
	 * 
	 * @return The inference engine.
	 */
	public InferenceEngine getInferenceEngine() {
		return inferenceEngine_;
	}

	/**
	 * Gets the class prefix.
	 * 
	 * @return The class prefix.
	 */
	public String getClassPrefix() {
		return classPrefix_;
	}
}
