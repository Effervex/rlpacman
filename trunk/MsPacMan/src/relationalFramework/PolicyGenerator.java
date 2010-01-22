package relationalFramework;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mandarax.kernel.ClauseSet;
import org.mandarax.kernel.ConstantTerm;
import org.mandarax.kernel.Fact;
import org.mandarax.kernel.InferenceEngine;
import org.mandarax.kernel.KnowledgeBase;
import org.mandarax.kernel.LogicFactory;
import org.mandarax.kernel.Predicate;
import org.mandarax.kernel.Replacement;
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
	private Collection<GuidedPredicate> conditionGenerator_;

	/** The cross-entropy generators for the actions within the rules. */
	private Collection<GuidedPredicate> actionGenerator_;

	/** The class prefix for the experiment. */
	private String classPrefix_;

	/** The covering object. */
	private Covering covering_;

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
		conditionGenerator_ = formConditions();
		actionGenerator_ = formActions();
		covering_ = new Covering(factory_);

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
	private ProbabilityDistribution<Slot> loadRuleBase(String classPrefix,
			File ruleBaseFile) {
		return RuleFileManager.loadRulesFromFile(ruleBaseFile,
				conditionGenerator_, actionGenerator_);
	}

	/**
	 * Loads the generator values into the rule base. This method needs to be
	 * modified later.
	 * 
	 * @param input
	 *            The generator file.
	 */
	public void loadGenerators(File input) {
		RuleFileManager.loadGenerators(input, policyGenerator_);
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
				policy.addRule(gr);
		}
		return policy;
	}

	/**
	 * Triggers covering of a state. This works by finding the maximally general
	 * (valid) set of conditions for each valid action type in the state (not
	 * individual actions).
	 * 
	 * @param state
	 *            The observations of the current state, including the valid
	 *            actions to take.
	 * @return The list of covered rules, one for each action type.
	 */
	public List<GuidedRule> triggerCovering(KnowledgeBase state) {
		System.out.println("<COVERING TRIGGERED:>");

		List<Rule> covered = covering_.coverState(state);

		// Format rules into GuidedRules.
		List<GuidedRule> coveredGuidedRules = new ArrayList<GuidedRule>();
		for (Rule coveredRule : covered) {
			ArrayList<GuidedPredicate> rulePreds = inferGuidedPreds(coveredRule);
			GuidedPredicate action = rulePreds.remove(rulePreds.size() - 1);
			Slot slot = findSlot(coveredRule.getHead().getPredicate());
			GuidedRule guidedRule = new GuidedRule(coveredRule, rulePreds,
					action, slot);
			coveredGuidedRules.add(guidedRule);

			// Adding the rule to the slot
			slot.addNewRule(guidedRule);
		}

		Collections.shuffle(coveredGuidedRules);
		return coveredGuidedRules;
	}

	/**
	 * Finds the slot for the action given, as each slot has an action assigned
	 * to it. Some slots may be fixed,a nd not allow new rules, in which case
	 * another slot of the same action will be available to add to.
	 * 
	 * @param action
	 *            The action predicate.
	 * @return The slot representing rules for that slot.
	 */
	private Slot findSlot(Predicate action) {
		for (Slot slot : policyGenerator_) {
			if ((slot.getAction().equals(action)) && (!slot.isFixed()))
				return slot;
		}
		return null;
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
	 * Infers GuidedPredicates from a rule - how it was composed.
	 * 
	 * @param rule
	 *            The rule being inferred.
	 * @return The guided predicates used to make up the rule, with the action
	 *         predicate at the end.
	 */
	public ArrayList<GuidedPredicate> inferGuidedPreds(Rule rule) {
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
			RuleFileManager.saveRulesToFile(ruleBaseFile);

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

	public boolean isFrozen() {
		return frozen_;
	}

	public ProbabilityDistribution<Slot> getGenerator() {
		return policyGenerator_;
	}
	
	@Override
	public String toString() {
		return policyGenerator_.toString();
	}
}