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
	private static final int MAX_UNIFICATION_INACTIVITY = 3;

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
		// TODO Covering
		System.out.println("<COVERING TRIGGERED:>");

		// Find the relevant conditions for each term
		MultiMap<Term, Fact> relevantConditions = new MultiMap<Term, Fact>();
		Fact actionFact = compileRelevantConditionMap(state, relevantConditions);

		// Maintain a mapping for each action, to be used in unification between
		// actions
		List<GuidedRule> generalActions = new ArrayList<GuidedRule>();

		// Arrange the actions in a heuristical order such that unification
		// should be most effective.
		@SuppressWarnings("unchecked")
		MultiMap<Predicate, Fact> validActions = arrangeActions((Set<Fact>) ((ConstantTerm) actionFact
				.getTerms()[0]).getObject());
		for (Predicate action : validActions.keySet()) {
			GuidedRule actionRule = unifyActionRules(validActions.get(action),
					relevantConditions);
			generalActions.add(actionRule);
		}

		return generalActions;
	}

	/**
	 * Unifies action rules together into one general all-covering rule.
	 * 
	 * @param actionsList
	 *            A heuristically sorted list of actions for a single predicate.
	 * @param relevantConditions
	 *            The relevant conditions for each term in the state.
	 * @return A GuidedRule representing a general action.
	 */
	private GuidedRule unifyActionRules(List<Fact> actionsList,
			MultiMap<Term, Fact> relevantConditions) {
		// TODO Modify this to a while loop.
		for (Fact action : actionsList) {
			List<Fact> actionFacts = new ArrayList<Fact>();

			Term[] actionTerms = action.getTerms();
			// Find the facts containing the same (useful) terms in the action
			for (int i = 0; i < actionTerms.length; i++) {
				Term term = actionTerms[i];
				if (StateSpec.getInstance().isUsefulTerm((ConstantTerm) term,
						factory_)) {
					List<Fact> termFacts = relevantConditions.get(term);
					for (Fact termFact : termFacts) {
						if (!actionFacts.contains(termFact))
							actionFacts.add(termFact);
					}
				}
			}

			// Inversely substitute the terms for variables

			// Unify with other action rules of the same action
		}

		// Use the unified rules to create new rules
		return null;
	}

	/**
	 * Arranges the collection of actions into a heuristical ordering which
	 * attempts to find maximally dissimilar actions of the same type. The list
	 * is ordered such that the actions of the same predicate, each one with
	 * different arguments from the last, are first, followed by randomly
	 * ordered remaining actions.
	 * 
	 * @param validActions
	 *            The set of valid actions.
	 * @return A multimap of each action predicate, containing the heuristically
	 *         ordered actions.
	 */
	private MultiMap<Predicate, Fact> arrangeActions(Set<Fact> validActions) {
		// Initialise a map for each action predicate
		MultiMap<Predicate, Fact> actionsMap = new MultiMap<Predicate, Fact>();
		MultiMap<Predicate, Set<Term>> usedTermsMap = new MultiMap<Predicate, Set<Term>>();

		MultiMap<Predicate, Fact> notUsedMap = new MultiMap<Predicate, Fact>();
		// For each action
		for (Fact action : validActions) {
			Predicate actionPred = action.getPredicate();
			if (isDissimilarAction(action, usedTermsMap)) {
				actionsMap.put(actionPred, action);
			} else {
				notUsedMap.put(actionPred, action);
			}
		}

		// If we have some actions not used (likely, then shuffle the ordering
		// and add them to the end of the actions map.
		if (!notUsedMap.isEmpty()) {
			for (List<Fact> notUsed : notUsedMap.valuesLists()) {
				Collections.shuffle(notUsed);
			}
			actionsMap.putAll(notUsedMap);
		}

		return actionsMap;
	}

	/**
	 * Is the given action dissimilar from the already seen actions? This is
	 * measured by which terms have already been seen in their appropriate
	 * predicate slots.
	 * 
	 * @param action
	 *            The action being checked.
	 * @param usedTermsMap
	 *            The already used terms mapping.
	 * @return True if the action is dissimilar, false otherwise.
	 */
	private boolean isDissimilarAction(Fact action,
			MultiMap<Predicate, Set<Term>> usedTermsMap) {
		Term[] terms = action.getTerms();
		Predicate actionPred = action.getPredicate();
		// Run through each term
		for (int i = 0; i < terms.length; i++) {
			// Checking if the term set already exists
			Set<Term> usedTerms = usedTermsMap.getIndex(actionPred, i);
			if (usedTerms == null) {
				usedTerms = new HashSet<Term>();
				usedTermsMap.put(actionPred, usedTerms);
			}

			ConstantTerm term = (ConstantTerm) terms[i];
			// If the term has already been used (but isn't a state or state
			// spec term), return false
			if ((usedTerms.contains(term))
					&& (StateSpec.getInstance().isUsefulTerm(term, factory_)))
				return false;
		}

		// If the terms have not been used before, add them all to their
		// appropriate sets.
		for (int i = 0; i < terms.length; i++) {
			usedTermsMap.getIndex(actionPred, i).add(terms[i]);
		}

		return true;
	}

	/**
	 * Compiles the relevant term conditions from the state into map format,
	 * with the term as the key and the fact as the value. This makes finding
	 * relevant conditions a quick matter.
	 * 
	 * @param state
	 *            The state containing the conditions.
	 * @return A mapping of conditions with terms as the key. Ignores type,
	 *         inequal, action facts and state and state spec terms.
	 */
	private Fact compileRelevantConditionMap(KnowledgeBase state,
			MultiMap<Term, Fact> relevantConditions) {
		Fact actionFact = null;

		List<ClauseSet> clauseSets = state.getClauseSets();
		for (ClauseSet cs : clauseSets) {
			// If the clause is a fact, use it
			if (cs instanceof Fact) {
				Fact stateFact = (Fact) cs;
				// Ignore the type, inequal and actions pred
				if (StateSpec.getInstance().isUsefulPredicate(
						stateFact.getPredicate()))
					extractTerms(stateFact, relevantConditions);
				// Find the action fact
				else if (stateFact.getPredicate().equals(
						StateSpec.getValidActionsPredicate()))
					actionFact = stateFact;
			}

			// TODO If the clause is a rule, use the rule to find background
			// clause facts.
		}

		return actionFact;
	}

	/**
	 * Extracts the terms from a fact and adds them to an appropriate position
	 * within the conditionMap.
	 * 
	 * @param stateFact
	 *            The fact being examined.
	 * @param conditionMap
	 *            The condition map to add to.
	 */
	private void extractTerms(Fact stateFact, MultiMap<Term, Fact> conditionMap) {
		Term[] terms = stateFact.getTerms();
		for (Term term : terms) {
			// Ignore the state and state spec terms
			if (StateSpec.getInstance().isUsefulTerm((ConstantTerm) term,
					factory_)) {
				// Add to map, if not already there
				conditionMap.putContains(term, stateFact);
			}
		}
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
}
