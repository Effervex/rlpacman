package relationalFramework;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mandarax.kernel.ConstantTerm;
import org.mandarax.kernel.Fact;
import org.mandarax.kernel.InferenceEngine;
import org.mandarax.kernel.KnowledgeBase;
import org.mandarax.kernel.LogicFactory;
import org.mandarax.kernel.Rule;
import org.mandarax.kernel.Term;
import org.mandarax.kernel.VariableTerm;
import org.mandarax.kernel.meta.JConstructor;
import org.mandarax.reference.DefaultInferenceEngine;

/**
 * A class for the generation of policies. The number of slots and the rules
 * within the slots can dynamically change.
 * 
 * @author Samuel J. Sarjant
 */
public class PolicyGenerator {
	/** The element delimiter between elements in the generator files. */
	public static final String ELEMENT_DELIMITER = ",";

	/** The delimiter character between rules within the same rule base. */
	public static final String RULE_DELIMITER = "@";

	/** The probability distributions defining the policy generator. */
	private ProbabilityDistribution<Slot> policyGenerator_;

	/** A hang on for when the generator is reset. */
	private Collection<GuidedPredicate> actionSet_;

	/** If the generator is currently frozen. */
	private boolean frozen_ = false;

	/** The instance. */
	private static PolicyGenerator instance_;

	/** The single logic factory. */
	private LogicFactory factory_;

	/** The single inference engine. */
	private InferenceEngine inferenceEngine_;

	/** The cross-entropy generators for the conditions within the rules. */
	private ProbabilityDistribution<GuidedPredicate> conditionGenerators_;

	/** The cross-entropy generators for the actions within the rules. */
	private ProbabilityDistribution<GuidedPredicate> actionGenerators_;

	/** The file the initial rulebase is read from. */
	private File initialFile_;

	/** The class prefix for the experiment. */
	private String classPrefix_;

	/**
	 * The constructor for creating a new Policy Generator.
	 * 
	 * @param classPrefix
	 *            The class prefix for the environment.
	 */
	public PolicyGenerator(String classPrefix) {
		factory_ = LogicFactory.getDefaultFactory();
		inferenceEngine_ = new DefaultInferenceEngine();
		StateSpec.initInstance(classPrefix, factory_);
		actionSet_ = StateSpec.getInstance().getActions();
		classPrefix_ = classPrefix;
		conditionGenerators_ = formConditions();
		actionGenerators_ = formActions();

		initialFile_ = null;
		resetGenerator();
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
		policyGenerator_ = loadRulesFromFile(initialFile_, conditionGenerators_,
				actionGenerators_);
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
	 * Generates a random policy using the weights present in the probability
	 * distribution.
	 * 
	 * @return A new policy, formed using weights from the probability
	 *         distributions.
	 */
	public Policy generatePolicy() {
		Policy policy = new Policy();

		// If frozen, use the slots in strict ordering.
		Iterator<Slot> iter = null;
		if (frozen_)
			iter = policyGenerator_.getOrderedElements().iterator();

		// Sample each slot from the policy with removal, forming a
		// deterministic policy.
		ProbabilityDistribution<Slot> removalDist = policyGenerator_.clone();
		for (int i = 0; i < policyGenerator_.size(); i++) {
			// If frozen, use from list, else sample with removal.
			Slot slot = (!frozen_) ? removalDist.sampleWithRemoval() : iter
					.next();
			GuidedRule gr = slot.getGenerator().sample();
			if (gr != null)
				policy.addRule(i, gr);
		}
		return policy;
	}

	/**
	 * Freezes the state of the policy.
	 * 
	 * @param freeze
	 *            Whether to freeze or unfreeze
	 */
	public void freeze(boolean freeze) {
		frozen_ = freeze;
		if (freeze) {
			for (Slot slot : policyGenerator_.getNonZero()) {
				slot.freeze(freeze);
			}
		} else {
			for (Slot slot : policyGenerator_.getNonZero()) {
				slot.freeze(freeze);
			}
		}
	}

	/**
	 * Resets the generator to where it started.
	 */
	public void resetGenerator() {
		policyGenerator_ = new ProbabilityDistribution<Slot>();
		for (GuidedPredicate action : actionSet_) {
			policyGenerator_.add(new Slot(action.getPredicate()));
		}

		policyGenerator_.normaliseProbs();
	}

	/**
	 * Normalises the distributions.
	 */
	public void normaliseDistributions() {
		policyGenerator_.normaliseProbs();
		for (Slot slot : policyGenerator_)
			slot.getGenerator().normaliseProbs();
	}

	/**
	 * Updates the distributions contained within using the counts.
	 * 
	 * @param numSamples
	 *            The number of elite samples.
	 * @param slotCounts
	 *            The count of each slot's usage in the elite samples.
	 * @param ruleCounts
	 *            The count of which rules were used within the slots.
	 * @param stepSize
	 *            The step size update parameter.
	 */
	public void updateDistributions(int numSamples,
			Map<Slot, Integer> slotCounts, Map<GuidedRule, Integer> ruleCounts,
			double stepSize) {
		// Update the slot distribution
		policyGenerator_.updateDistribution(numSamples, slotCounts, stepSize);

		for (Slot slot : policyGenerator_) {
			slot.getGenerator().updateDistribution(numSamples, ruleCounts,
					stepSize);
		}
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
			for (Slot slot : policyGenerator_) {
				// For each of the rules
				for (GuidedRule r : slot.getGenerator()) {
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
	 * Load the rules from a file.
	 * 
	 * @param ruleBaseFile
	 *            The file to load the rules from.
	 * @param actGenerator
	 *            The actions generator.
	 * @param condGenerator
	 *            The conditions generator.
	 * @return The rules loaded in.
	 */
	private ProbabilityDistribution<Slot> loadRulesFromFile(File ruleBaseFile,
			ProbabilityDistribution<GuidedPredicate> condGenerator,
			ProbabilityDistribution<GuidedPredicate> actGenerator) {
		ProbabilityDistribution<Slot> ruleBases = new ProbabilityDistribution<Slot>();

		try {
			FileReader reader = new FileReader(ruleBaseFile);
			BufferedReader bf = new BufferedReader(reader);

			// Checking the environment goals match.
			String input = bf.readLine();
			if (!input
					.equals(StateSpec.getInstance().getGoalState().toString())) {
				System.err
						.println("Environment goal does not match! Crashing...");
				return null;
			}

			// Read the rules in.
			Map<String, Object> constants = StateSpec.getInstance()
					.getConstants();
			while (((input = bf.readLine()) != null) && (!input.equals(""))) {
				Slot slot = null;
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
					if (slot == null) {
						slot = new Slot(action.getPredicate());
					}

					GuidedRule gr = new GuidedRule(rule, condsAct, action, slot);
					slot.getGenerator().add(gr);
				}
				slot.getGenerator().normaliseProbs();
				ruleBases.add(slot);
			}

			bf.close();
			reader.close();

			return ruleBases;
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
	 * Saves the generators/distributions to file. TODO Modify this method to
	 * save the generators in a more dynamic format.
	 * 
	 * @param output
	 *            The file to output the generator to.
	 * @throws Exception
	 *             Should something go awry.
	 */
	public void saveGenerators(File output) throws Exception {
		FileWriter wr = new FileWriter(output);
		BufferedWriter buf = new BufferedWriter(wr);

		buf.write(policyGenerator_.generatorString(ELEMENT_DELIMITER) + "\n");

		// For each of the rule generators
		for (Slot slot : policyGenerator_) {
			buf.write(slot.getGenerator().generatorString(ELEMENT_DELIMITER)
					+ "\n");
		}

		buf.close();
		wr.close();
	}

	/**
	 * Saves a frozen, human readable version of the generators out
	 * 
	 * @param output
	 *            The file to output the human readable generators to.
	 */
	public void saveHumanGenerators(File output) throws Exception {
		FileWriter wr = new FileWriter(output);
		BufferedWriter buf = new BufferedWriter(wr);

		// Go through each slot, writing out those that fire
		ArrayList<Slot> probs = policyGenerator_.getOrderedElements();
		for (Slot slot : probs) {
			// Output every non-zero rule
			boolean single = true;
			for (GuidedRule rule : slot.getGenerator().getNonZero()) {
				if (!single)
					buf.write("/ ");
				buf.write(StateSpec.encodeRule(rule.getRule()));
				single = false;
			}
			buf.write("\n");
		}

		buf.close();
		wr.close();
	}

	/**
	 * Loads the generators/distributions from file.
	 * 
	 * @param file
	 *            The file to load from.
	 * @throws Exception
	 *             Should something go awry.
	 */
	public void loadGenerators(File input) throws Exception {
		// TODO Modify this method
		FileReader reader = new FileReader(input);
		BufferedReader buf = new BufferedReader(reader);

		// Parse the slots
		String[] split = buf.readLine().split(ELEMENT_DELIMITER);
		for (int i = 0; i < split.length; i++) {
			policyGenerator_.set(i, Double.parseDouble(split[i]));
		}

		// Parse the rules
		// RuleBase.getInstance().readGenerators(buf);

		buf.close();
		reader.close();
	}

	/**
	 * Gets the rule base instance.
	 * 
	 * @return The rule base instance or null if not yet initialised.
	 */
	public static PolicyGenerator getInstance() {
		return instance_;
	}

	/**
	 * Initialises the instance as a set of random rule bases.
	 */
	public static void initInstance(String classPrefix) {
		instance_ = new PolicyGenerator(classPrefix);
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
		instance_ = new PolicyGenerator(classPrefix);
		instance_.loadRuleBase(classPrefix, ruleBaseFile);
	}

	public InferenceEngine getInferenceEngine() {
		return inferenceEngine_;
	}

	public LogicFactory getLogicFactory() {
		return factory_;
	}

	public String getClassPrefix() {
		return classPrefix_;
	}
}
