package relationalFramework;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import jess.Fact;
import jess.Rete;

/**
 * A class for the generation of policies. The number of slots and the rules
 * within the slots can dynamically change.
 * 
 * @author Samuel J. Sarjant
 */
public class PolicyGenerator {
	/** The probability distributions defining the policy generator. */
	private ProbabilityDistribution<Slot> policyGenerator_;

	/** The actions set the generator was initialised with. */
	private MultiMap<String, Class> actionSet_;

	/** If the generator is currently frozen. */
	private boolean frozen_ = false;

	/** The instance. */
	private static PolicyGenerator instance_;

	/** The class prefix for the experiment. */
	private String classPrefix_;

	/** The covering object. */
	private Covering covering_;

	/** The maximally general rules for each action. */
	private MultiMap<String, GuidedRule> lggRules_;

	/**
	 * The covered rules which are not yet LGG. Items in this list are mutually
	 * exclusive from the lggRules list.
	 */
	private MultiMap<String, GuidedRule> nonLGGCoveredRules_;

	/** The list of mutated rules. Mutually exclusive from the other LGG lists. */
	private MultiMap<String, GuidedRule> mutatedRules_;

	/** The random number generator. */
	public static Random random_ = new Random();

	/**
	 * The constructor for creating a new Policy Generator.
	 * 
	 * @param classPrefix
	 *            The class prefix for the environment.
	 */
	public PolicyGenerator(String classPrefix) {
		StateSpec.initInstance(classPrefix);
		actionSet_ = StateSpec.getInstance().getActions();
		classPrefix_ = classPrefix;
		nonLGGCoveredRules_ = new MultiMap<String, GuidedRule>();
		covering_ = new Covering();
		lggRules_ = new MultiMap<String, GuidedRule>();
		mutatedRules_ = new MultiMap<String, GuidedRule>();

		resetGenerator();
	}

	/**
	 * Loads the generator values into the rule base. This method needs to be
	 * modified later.
	 * 
	 * @param input
	 *            The generator file.
	 */
	public void loadGenerators(File input) {
		policyGenerator_ = RuleFileManager.loadGenerators(input);
		policyGenerator_.normaliseProbs();
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
		List<GuidedRule> lggRules = new ArrayList<GuidedRule>();
		for (int i = 0; i < policyGenerator_.size(); i++) {
			// If frozen, use from list, else sample with removal.
			Slot slot = (!frozen_) ? removalDist.sampleWithRemoval() : iter
					.next();
			GuidedRule gr = slot.getGenerator().sample();
			if (gr != null)
				policy.addRule(gr);

			// Get the lgg rule for this slot
			if (lggRules_.containsKey(slot.getAction()))
				lggRules.addAll(lggRules_.get(slot.getAction()));
		}

		// Append the general rules as well (if they aren't already in there) to
		// ensure unnecessary covering isn't triggered.
		for (GuidedRule lggRule : lggRules) {
			if (!policy.contains(lggRule))
				policy.addRule(lggRule);
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
	 * @param createNewRules
	 *            Whether the covering algorithm should create new rules or only
	 *            refine existing one.
	 * @return The list of covered rules, one for each action type.
	 */
	public List<GuidedRule> triggerCovering(Rete state, boolean createNewRules) {
		if (!frozen_) {
			if (createNewRules)
				System.out.println("\t<COVERING TRIGGERED:>");

			List<GuidedRule> covered = null;
			try {
				covered = covering_.coverState(state, nonLGGCoveredRules_,
						createNewRules);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Add remaining information to rules.
			for (GuidedRule coveredRule : covered) {
				if (coveredRule.isRecentlyModified()) {
					if (createNewRules)
						System.out.println("\tCOVERED RULE: " + coveredRule);
					else
						System.out.println("\tREFINED RULE: " + coveredRule);
				}
				String actionPred = StateSpec
						.splitFact(coveredRule.getAction())[0];
				if (coveredRule.getSlot() == null) {
					Slot slot = findSlot(actionPred);

					// Adding the rule to the slot
					slot.addNewRule(coveredRule, false);
				}

				// If the rule is maximally general, mutate and store it
				if (coveredRule.isLGG()) {
					nonLGGCoveredRules_.get(actionPred).remove(coveredRule);
					lggRules_.put(actionPred, coveredRule);
					System.out.println("\tLGG RULE FOUND: " + coveredRule);

					// Mutate unless already mutated
					if (!covering_.isPreGoalSettled(actionPred)) {
						mutateRule(coveredRule, false);
					}
				} else if (createNewRules) {
					nonLGGCoveredRules_.put(actionPred, coveredRule);
				}
			}

			Collections.shuffle(covered, random_);
			return covered;
		}
		return null;
	}

	/**
	 * Mutates a rule and creates and adds children to the slots.
	 * 
	 * @param baseRule
	 *            The rule to mutate.
	 * @param settledPreGoal
	 *            If the pre-goal has settled.
	 */
	private void mutateRule(GuidedRule baseRule, boolean settledPreGoal) {
		// If the base rule hasn't already spawned pre-goal mutants
		if (!baseRule.hasSpawned()) {
			Collection<GuidedRule> mutants = covering_
					.specialiseToPreGoal(baseRule);

			Slot ruleSlot = baseRule.getSlot();
			String actionPred = baseRule.getActionPredicate();

			// If the slot has settled, remove any mutants not present in
			// the permanent mutant set
			if (settledPreGoal) {
				baseRule.setSpawned(true);
				if (!mutants.isEmpty()
						&& (mutatedRules_.get(actionPred) != null)) {
					// Run through the rules in the slot, removing any mutants
					// not in the permanent mutant set.
					List<GuidedRule> removables = new ArrayList<GuidedRule>();
					for (GuidedRule gr : ruleSlot.getGenerator()) {
						if (gr.isMutant() && !mutants.contains(gr))
							removables.add(gr);
					}
					if (ruleSlot.getGenerator().removeAll(removables))
						ruleSlot.getGenerator().normaliseProbs();
					mutatedRules_.get(actionPred).removeAll(removables);
				}
			}

			// Add all mutants to the ruleSlot
			for (GuidedRule gr : mutants) {
				// Only add if not already in there
				if (!ruleSlot.contains(gr)) {
					ruleSlot.addNewRule(gr, false);
				}

				mutatedRules_.putContains(ruleSlot.getAction(), gr);
			}
		}
	}

	/**
	 * Forms the pre-goal state using the given pre-goal state seen by the
	 * agent.
	 * 
	 * @param preGoalState
	 *            The pre-goal state seen by the agent.
	 * @param actions
	 *            The final action(s) taken by the agent.
	 */
	public void formPreGoalState(Collection<Fact> preGoalState, String[] actions) {
		if (!frozen_) {
			// If the state has settled and is probably at minimum, trigger
			// mutation.
			Collection<String> settledGoals = covering_.formPreGoalState(
					preGoalState, actions[0]);
			String actionPred = StateSpec.splitFact(actions[0])[0];
			if (!settledGoals.isEmpty()) {
				System.out.println("\tSETTLED PRE-GOAL STATE " + "("
						+ actionPred + "):");
				// For each maximally general rule
				for (String settledAction : settledGoals) {
					if (lggRules_.containsKey(settledAction))
						for (GuidedRule general : lggRules_.get(settledAction))
							mutateRule(general, true);
				}
			} else {
				System.out.println("\tFORMING PRE-GOAL STATE " + "("
						+ actionPred + "):");
			}

			for (String action : StateSpec.getInstance().getActions().keySet()) {
				System.out.println(action + ": "
						+ covering_.getPreGoalState(action));
			}
		}
	}

	/**
	 * If the pre-goal state exists.
	 * 
	 * @return True if the state exists, false otherwise.
	 */
	public boolean hasPreGoal() {
		return covering_.hasPreGoal();
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
	private Slot findSlot(String action) {
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
		for (String action : actionSet_.keySet()) {
			policyGenerator_.add(new Slot(action));
		}

		nonLGGCoveredRules_.clear();
		lggRules_.clear();
		mutatedRules_.clear();
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
	 * Operations to run after the distributions have been updated. This
	 * includes further mutation of useful rules.
	 */
	public void postUpdateOperations() {
		// Mutate the rules further
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
		// instance_.loadRuleBase(classPrefix, ruleBaseFile);
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