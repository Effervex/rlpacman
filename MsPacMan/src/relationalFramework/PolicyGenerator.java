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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

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
	private OrderedDistribution<Slot> slotGenerator_;

	/** The actions set the generator was initialised with. */
	@SuppressWarnings("unchecked")
	private MultiMap<String, Class> actionSet_;

	/** If the generator is currently frozen. */
	private boolean frozen_ = false;

	/** The instance. */
	private static PolicyGenerator instance_;

	/** The covering object. */
	private Covering covering_;

	/** The set of covered rules, some which may be LGG, others not. */
	private MultiMap<String, GuidedRule> coveredRules_;

	/** The list of mutated rules. Mutually exclusive from the other LGG lists. */
	private MultiMap<String, GuidedRule> mutatedRules_;

	/** The collection of rules that have been removed. */
	private Collection<GuidedRule> removedRules_;

	/**
	 * The last amount of difference in distribution probabilities from the
	 * update.
	 */
	private double updateDifference_;

	/** If this policy generator is being used for learning a module. */
	private boolean moduleGenerator_;

	/** The goal this generator is working towards if modular. */
	private ArrayList<String> moduleGoal_;

	/**
	 * If this policy generator only updates the ordering of slots - no rule
	 * creation or modification.
	 */
	private boolean slotOptimisation_ = false;

	/**
	 * A flag for when the experiment needs to restart (due to new rules being
	 * added/removed)
	 */
	private boolean restart_ = false;

	/** If we're using weighted elite samples. */
	public boolean weightedElites_ = false;

	/** If modules are being used. */
	public boolean useModules_ = true;

	/** If the slots can be dynamically added/removed. */
	public boolean dynamicSlotNumber_ = true;

	/**
	 * The maximum amount of change between the slots before it is considered
	 * converged.
	 */
	private double convergedValue_ = 0;

	/** The number of times in a row the updates have been convergent. */
	private int convergedStrike_ = 0;

	/** The random number generator. */
	public static Random random_ = new Random();

	/** If we're running the experiment in debug mode. */
	public static boolean debugMode_ = false;

	/** The element delimiter between elements in the generator files. */
	public static final String ELEMENT_DELIMITER = ",";

	/** The minimum value for weight updating. */
	private static final double MIN_UPDATE = 0.1;

	/**
	 * The threshold coefficient relative to distribution size at which rules
	 * have extra influence.
	 */
	private static final double INFLUENCE_THRESHOLD = 1.0;

	/**
	 * The coefficient towards extra influence the rules gain when lowly tested.
	 */
	private static final double INFLUENCE_BOOST = 1.0;

	/**
	 * The converged must remain converged for X updates before considered
	 * converged.
	 */
	private static final int NUM_COVERGED_UPDATES = 10;

	/**
	 * The constant used to determine when a rule is pruned from the
	 * distribution.
	 */
	private static final double PRUNING_CONST = 0.05;

	/**
	 * The constructor for creating a new Policy Generator.
	 */
	public PolicyGenerator() {
		actionSet_ = StateSpec.getInstance().getActions();
		coveredRules_ = new MultiMap<String, GuidedRule>();
		covering_ = new Covering(actionSet_.size());
		mutatedRules_ = new MultiMap<String, GuidedRule>();
		removedRules_ = new HashSet<GuidedRule>();
		updateDifference_ = Double.MAX_VALUE;

		resetGenerator();
	}

	/**
	 * Generates a random policy using the weights present in the probability
	 * distribution.
	 * 
	 * @param influenceUntestedRules
	 *            Influence selection of rules towards untested rules.
	 * @return A new policy, formed using weights from the probability
	 *         distributions.
	 */
	public Policy generatePolicy(boolean influenceUntestedRules) {
		Policy policy = new Policy();

		// Clone the ordered distribution so slots and rules from slots can be
		// removed without consequence.
		OrderedDistribution<Slot> removalDist = new OrderedDistribution<Slot>(
				random_);
		for (Slot slot : slotGenerator_) {
			removalDist.add(slot.clone(), slotGenerator_.getOrdering(slot));
		}

		int i = 0;
		while (!removalDist.isEmpty()) {
			Slot slot = null;
			if (!dynamicSlotNumber_ || slotOptimisation_) {
				// Sample with removal, getting the most likely if frozen.
				slot = removalDist.sampleWithRemoval(i, slotGenerator_.size(),
						frozen_);
			} else {
				slot = removalDist.sample(i, slotGenerator_.size(), frozen_);
				boolean[] useSlot = slot.shouldUseSlot(random_, frozen_);
				// If we are removing the slot, remove it
				if (!useSlot[1])
					removalDist.remove(slot);
				// If we aren't using the slot, set it to null.
				if (!useSlot[0])
					slot = null;
			}

			if ((slot != null) && (!slot.isEmpty())) {
				// If influencing towards untested rules, skew the slot
				// distribution towards lesser used rules.
				GuidedRule gr = null;
				if (influenceUntestedRules) {
					gr = slot.getInfluencedDistribution(INFLUENCE_THRESHOLD,
							INFLUENCE_BOOST).sample(false);
					gr.incrementRuleUses();
				} else
					gr = slot.getGenerator().sample(false);
				if (gr != null) {
					policy.addRule(gr, true, false);
					slot.getGenerator().remove(gr);
					slot.getGenerator().normaliseProbs();
				}
			}
		}

		// Append the general rules as well (if they aren't already in there) to
		// ensure unnecessary covering isn't triggered.
		for (GuidedRule coveredRule : coveredRules_.values()) {
			if (!policy.contains(coveredRule))
				policy.addRule(coveredRule, false, true);
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
	 * @param validActions
	 *            The set of valid actions to choose from.
	 * @param activatedActions
	 *            The set of actions that have already been activated by
	 *            existing rules in the policy. By the way policies are set up,
	 *            LGG rules will always be in them.
	 * @param createNewRules
	 *            Whether the covering algorithm should create new rules or only
	 *            refine existing one. This is false when the policy already has
	 *            enough actions, true when more actions are required.
	 * @return The list of covered rules, one for each action type.
	 */
	public List<GuidedRule> triggerCovering(Rete state,
			MultiMap<String, String> validActions,
			MultiMap<String, String> activatedActions, boolean createNewRules) {
		// If there are actions to cover, cover them.
		if (!frozen_ && !slotOptimisation_
				&& !coveredValidActions(validActions, activatedActions)) {
			if (createNewRules)
				System.out.println("\t<COVERING TRIGGERED:>");

			List<GuidedRule> covered = null;
			try {
				covered = covering_.coverState(state, validActions,
						coveredRules_);
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

					String actionPred = StateSpec.splitFact(coveredRule
							.getAction())[0];
					if (coveredRule.getSlot() == null) {
						Slot slot = findSlot(actionPred);

						// Adding the rule to the slot
						slot.addNewRule(coveredRule, false);
					}

					// If the rule is maximally general, mutate and store it
					coveredRules_.putContains(actionPred, coveredRule);

					// Mutate unless already mutated
					if (!covering_.isPreGoalSettled(actionPred)) {
						mutateRule(coveredRule, coveredRule.getSlot(), true,
								false);
					}

					// Rules were modified so learning needs to restart.
					restart_ = true;
				}
			}

			if (createNewRules)
				Collections.shuffle(covered, random_);
			return covered;
		}
		return null;
	}

	/**
	 * A method which checks if covering is necessary or required, based on the
	 * valid actions of the state.
	 * 
	 * @param validActions
	 *            The set of valid actions for the state. If we're creating LGG
	 *            covering rules, the activated actions need to equal this set.
	 * @param activatedActions
	 *            The set of actions already activated by the policy. We only
	 *            consider these when we're creating new rules.
	 * @return True if the activated actions cover all valid actions, false
	 *         otherwise.
	 */
	private boolean coveredValidActions(MultiMap<String, String> validActions,
			MultiMap<String, String> activatedActions) {
		for (String action : validActions.keySet()) {
			// If the activated actions don't even contain the key, return
			// false.
			if (!activatedActions.containsKey(action))
				return false;

			// Check each set of actions match up
			if (!activatedActions.get(action).containsAll(
					(validActions.get(action))))
				return false;
		}
		return true;
	}

	/**
	 * Mutates a rule (if it hasn't spawned already, though the covered rule is
	 * always checked) and creates and adds children to the slots.
	 * 
	 * @param baseRule
	 *            The rule to mutate.
	 * @param removeOld
	 *            If the pre-goal has settled.
	 */
	private void mutateRule(GuidedRule baseRule, Slot ruleSlot,
			boolean removeOld, boolean settled) {
		int preGoalHash = covering_.getMutationHash(baseRule
				.getActionPredicate());

		// If the base rule hasn't already spawned mutants for this pre-goal.
		if (!baseRule.hasSpawned(preGoalHash)) {

			if (debugMode_) {
				try {
					System.out.println("\tMUTATING " + baseRule);
					System.out.println("PRE-GOAL: ");
					for (String action : StateSpec.getInstance().getActions()
							.keySet()) {
						System.out.println(action + ": "
								+ covering_.getPreGoalState(action));
					}
					System.out.println("Press Enter to continue.");
					System.in.read();
					System.in.read();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			Set<GuidedRule> mutants = covering_.specialiseToPreGoal(baseRule);
			mutants.addAll(covering_.specialiseRule(baseRule));
			baseRule.setSpawned(preGoalHash);

			String actionPred = baseRule.getActionPredicate();
			// If the slot has settled, remove any mutants not present in
			// the permanent mutant set
			if (removeOld) {

				// If the rule isn't a mutant rule, we have mutants and have
				// already created temp mutants, run through the mutations and
				// remove unnecessary temp mutants.
				if (!baseRule.isMutant() && !mutants.isEmpty()
						&& (mutatedRules_.get(actionPred) != null)) {
					// Run through the rules in the slot, removing any mutants
					// not in the permanent mutant set.
					List<GuidedRule> removables = new ArrayList<GuidedRule>();
					for (GuidedRule gr : ruleSlot.getGenerator()) {
						if (gr.isMutant() && !mutants.contains(gr)) {
							removables.add(gr);

							if (debugMode_) {
								System.out.println("\tREMOVING MUTANT: " + gr);
							}
						}
					}

					// Setting the restart value.
					if (!removables.isEmpty()) {
						restart_ = true;
						if (debugMode_) {
							try {
								System.out.println("Press Enter to continue.");
								System.in.read();
								System.in.read();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}

					if (ruleSlot.getGenerator().removeAll(removables))
						ruleSlot.getGenerator().normaliseProbs();
					mutatedRules_.get(actionPred).removeAll(removables);
				}
			} else {
				restart_ = false;
			}

			// Add all mutants to the ruleSlot
			for (GuidedRule gr : mutants) {
				if (!removedRules_.contains(gr)) {
					// Only add if not already in there
					if (!ruleSlot.contains(gr)) {
						ruleSlot.addNewRule(gr, false);
					}

					if (debugMode_) {
						System.out.println("\tADDED/EXISTING MUTANT: " + gr);

					}

					mutatedRules_.putContains(ruleSlot.getAction(), gr);
				}
			}

			if (debugMode_ && !mutants.isEmpty()) {
				try {
					System.out.println("Press Enter to continue.");
					System.in.read();
					System.in.read();
				} catch (Exception e) {
					e.printStackTrace();
				}
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
	 * @param constants
	 *            The constants used in the pre-goal formation.
	 */
	public void formPreGoalState(Collection<Fact> preGoalState,
			ActionChoice actions, List<String> constants) {
		if (!frozen_ && !slotOptimisation_) {
			// Form the pre-goal using the final action/s as a parameter.
			Collection<String> settledGoals = covering_.formPreGoalState(
					preGoalState, actions, constants);
			String actionPred = actions.getActionPreds();
			if (debugMode_) {
				try {
					if (settledGoals.contains(actionPred))
						System.out.println("\tSETTLED PRE-GOAL STATE " + "("
								+ actionPred + "):");
					else
						System.out.println("\tFORMING PRE-GOAL STATE " + "("
								+ actionPred + "):");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// If the pre-goal has recently changed and we have an LGG rule for
			// it, create and remove mutants.
			for (String action : StateSpec.getInstance().getActions().keySet()) {
				if (covering_.isPreGoalRecentlyChanged(action)
						&& coveredRules_.containsKey(action)) {
					boolean settled = covering_.isPreGoalSettled(action);
					for (GuidedRule general : coveredRules_.get(action))
						mutateRule(general, general.getSlot(), true, settled);
				}
			}

			if (debugMode_) {
				try {
					for (String stateAction : StateSpec.getInstance()
							.getActions().keySet()) {
						System.out.println(stateAction + ": "
								+ covering_.getPreGoalState(stateAction));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
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
	 * Checks if the state has settled. This means that the pre-goal has settled
	 * and there are no non-LGG rules.
	 * 
	 * @param checkPreGoal
	 *            If we're considering the pre-goal for settling.
	 * 
	 * @return True if the generator values are settled, false otherwise.
	 */
	public boolean isSettled(boolean checkPreGoal) {
		// If we're just optimising the slot ordering, then it's settled.
		if (slotOptimisation_)
			return true;
		// Must have at least one pre-goal
		if (!hasPreGoal())
			return false;
		// If checking pregoal, all pre-goals must be settled (or null)
		if (checkPreGoal) {
			for (String action : actionSet_.keySet()) {
				if ((covering_.getPreGoalState(action) != null)
						&& !covering_.isPreGoalSettled(action))
					return false;
			}
		}

		// Rules need to have been created at one point
		if (coveredRules_.isKeysEmpty())
			return false;
		return true;
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
		for (Slot slot : slotGenerator_) {
			if ((slot.getAction().equals(action)) && (!slot.isFixed()))
				return slot;
		}
		return null;
	}

	/**
	 * Un/Freezes the state of the policy.
	 * 
	 * @param freeze
	 *            Whether to freeze or unfreeze
	 */
	public void freeze(boolean freeze) {
		frozen_ = freeze;
		if (freeze) {
			for (Slot slot : slotGenerator_.getElements()) {
				slot.freeze(freeze);
			}
		} else {
			for (Slot slot : slotGenerator_.getElements()) {
				slot.freeze(freeze);
			}
		}
	}

	/**
	 * Resets the generator to where it started.
	 */
	public void resetGenerator() {
		slotGenerator_ = new OrderedDistribution<Slot>(random_);
		for (String action : actionSet_.keySet()) {
			slotGenerator_.add(new Slot(action));
		}

		coveredRules_.clear();
		mutatedRules_.clear();
		removedRules_.clear();
	}

	/**
	 * If the slots and rules within are converged to stability.
	 * 
	 * @return True if the generator is converged, false otherwise.
	 */
	public boolean isConverged() {
		if (updateDifference_ < convergedValue_)
			convergedStrike_++;
		else
			convergedStrike_ = 0;

		if (convergedStrike_ >= NUM_COVERGED_UPDATES)
			return true;
		return false;
	}

	/**
	 * Updates the distributions contained within using the counts.
	 * 
	 * @param sortedPolicies
	 *            The sorted policy values.
	 * @param numElite
	 *            The number of elite samples to use.
	 * @param stepSize
	 *            The step size update parameter.
	 */
	public void updateDistributions(List<PolicyValue> sortedPolicies,
			int numElite, double stepSize) {
		// Keep count of the rules seen (and slots used)
		ElitesData ed = countRules(sortedPolicies.subList(0, numElite));

		// Update the slot distribution
		updateDifference_ = slotGenerator_.updateDistribution(ed
				.getSlotPositions(), stepSize);

		// Update the rule distributions and slot activation probability
		if (!slotOptimisation_) {
			for (Slot slot : slotGenerator_) {
				// Slot selection values
				double mean = ed.getSlotNumeracyMean(slot);
				double sd = ed.getSlotNumeracySD(slot);
				slot.updateSelectionValues(mean, sd, stepSize);

				updateDifference_ += slot.getGenerator().updateDistribution(
						ed.getSlotCount(slot), ed.getRuleCounts(), stepSize);
			}
		}

		convergedValue_ = stepSize / 10;
	}

	/**
	 * Counts the rules from the elite samples and stores their frequencies and
	 * total score.
	 * 
	 * @param elites
	 *            The elite samples to iterate through.
	 * @param slotCounts
	 *            The counts for the slots
	 * @param ruleCounts
	 *            The counts for the individual rules.
	 * @param slotPositions
	 *            The relative positions of slots within the elite policies.
	 * @return The average value of the elite samples.
	 */
	private ElitesData countRules(List<PolicyValue> elites) {
		ElitesData ed = new ElitesData();

		double gradient = 0;
		double offset = 1;
		if (weightedElites_) {
			double diffValues = (elites.get(0).getValue() - elites.get(
					elites.size() - 1).getValue());
			if (diffValues != 0)
				gradient = (1 - MIN_UPDATE) / diffValues;
			offset = 1 - gradient * elites.get(0).getValue();
		}

		// Only selecting the top elite samples
		MultiMap<Slot, Double> slotNumeracy = new MultiMap<Slot, Double>();
		for (PolicyValue pv : elites) {
			Map<Slot, Integer> policySlotCounts = new HashMap<Slot, Integer>();
			double weight = pv.getValue() * gradient + offset;
			Policy eliteSolution = pv.getPolicy();

			// Count the occurrences of rules and slots in the policy
			Collection<GuidedRule> policyRules = eliteSolution
					.getPolicyRules(true);
			Collection<GuidedRule> firingRules = eliteSolution.getFiringRules();
			int firedRuleIndex = 0;
			for (GuidedRule rule : policyRules) {
				Slot ruleSlot = rule.getSlot();

				// If the rule is in the fired rules
				if (firingRules.contains(rule)) {
					// Slot counts
					ed.addSlotCount(ruleSlot, weight);

					// Slot ordering
					double relValue = OrderedDistribution.getRelativePosition(
							firedRuleIndex, eliteSolution.size());
					ed.addSlotOrdering(ruleSlot, relValue);
					firedRuleIndex++;

					// Rule counts
					ed.addRuleCount(rule, weight);

					// Note the slot count within the policy
					Integer policySlotCount = policySlotCounts.get(ruleSlot);
					if (policySlotCount == null)
						policySlotCount = 0;
					policySlotCounts.put(ruleSlot, policySlotCount + 1);

				}
			}

			// Add to the slot numeracy multimap
			for (Slot slot : policySlotCounts.keySet()) {
				slotNumeracy
						.put(slot, policySlotCounts.get(slot).doubleValue());
			}
		}

		// Calculate the slot numeracy data.
		for (Slot slot : slotNumeracy.keySet()) {
			// Each slot should have elites number of counts
			double[] slotCounts = new double[elites.size()];
			int i = 0;
			for (Double val : slotNumeracy.get(slot)) {
				slotCounts[i] = val;
				i++;
			}

			Mean mean = new Mean();
			StandardDeviation sd = new StandardDeviation();
			double meanVal = mean.evaluate(slotCounts);
			ed.setUsageStats(slot, meanVal, sd.evaluate(slotCounts, meanVal));
		}

		return ed;
	}

	/**
	 * Operations to run after the distributions have been updated. This
	 * includes further mutation of useful rules.
	 * 
	 * @param mutationUses
	 *            The number of times a rule can be used before it can create
	 *            mutations.
	 */
	public void postUpdateOperations(int mutationUses) {
		if (slotOptimisation_)
			return;

		// Mutate the rules further
		if (debugMode_) {
			try {
				System.out.println("\tPERFORMING POST-UPDATE OPERATIONS");
				System.out.println("Press Enter to continue.");
				System.in.read();
				System.in.read();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// For each slot
		for (Slot slot : slotGenerator_) {
			// Pruning unused rules from the slots
			ProbabilityDistribution<GuidedRule> distribution = slot
					.getGenerator();
			double pruneProb = (1.0 / distribution.size()) * PRUNING_CONST;
			boolean removed = false;
			for (Iterator<GuidedRule> iter = distribution.iterator(); iter
					.hasNext();) {
				GuidedRule rule = iter.next();

				if (distribution.getProb(rule) <= pruneProb) {
					iter.remove();
					removed = true;
					// Note the rule in the no-create list
					removedRules_.add(rule);
				}
			}
			if (removed)
				distribution.normaliseProbs();

			// Sample a rule from the slot and mutate it if it has seen
			// sufficient states.
			GuidedRule rule = slot.getGenerator().sample(false);
			if (rule == null)
				System.out.println("Problem...");
			if (rule.getUses() >= mutationUses)
				mutateRule(rule, slot, false, true);
		}

		// Mutate the rules further
		if (debugMode_) {
			try {
				System.out.println(slotGenerator_);
				System.out.println("Press Enter to continue.");
				System.in.read();
				System.in.read();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Gets the predicates for every rule fact which is a constant fact and has
	 * a settled pre-goal.
	 * 
	 * @return A collection of predicates, each of which is involved in a rule
	 *         where the predicate has only constant terms.
	 */
	public SortedSet<ConstantPred> getConstantFacts() {
		SortedSet<ConstantPred> constantFacts = new TreeSet<ConstantPred>();

		// Run through each slot and rule for conditions that only use constant
		// terms.
		for (Slot slot : slotGenerator_) {
			for (GuidedRule gr : slot.getGenerator()) {
				// Check the rule's pre-goal is settled
				if (covering_.isPreGoalSettled(gr.getActionPredicate())) {
					List<String> constants = gr.getConstantConditions();
					for (String fact : constants)
						constantFacts.add(new ConstantPred(fact));
					if (constants.size() > 1)
						constantFacts.add(new ConstantPred(constants));
				}
			}
		}
		return constantFacts;
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
	 * Initialises the instance as a new policy generator.
	 * 
	 * @return The new PolicyGenerator.
	 */
	public static PolicyGenerator newInstance(int randSeed) {
		random_ = new Random(randSeed);
		instance_ = new PolicyGenerator();
		instance_.moduleGenerator_ = false;
		return instance_;
	}

	/**
	 * Initialises the instance as a new policy generator which uses information
	 * from the old generator.
	 * 
	 * @param policyGenerator
	 *            The policy generator containing the old LGG rules.
	 * @param internalGoal
	 *            The modular goal being worked towards.
	 * @return The new PolicyGenerator.
	 */
	public static PolicyGenerator newInstance(PolicyGenerator policyGenerator,
			ArrayList<String> internalGoal) {
		instance_ = new PolicyGenerator();
		instance_.addCoveredRules(policyGenerator.coveredRules_);
		instance_.moduleGenerator_ = true;
		instance_.moduleGoal_ = internalGoal;
		return instance_;
	}

	/**
	 * Initialises the instance as a new policy generator which only optimises
	 * the ordering of the provided rules.
	 * 
	 * @param policyGenerator
	 *            The policy generator containing the old LGG rules.
	 * @param rules
	 *            The rules to be optimised orderly.
	 * @return The new PolicyGenerator
	 */
	public static PolicyGenerator newInstance(PolicyGenerator policyGenerator,
			Collection<GuidedRule> rules, List<String> newQueryParams,
			ArrayList<String> internalGoal) {
		instance_ = newInstance(policyGenerator, internalGoal);
		instance_.slotOptimisation_ = true;

		// Set the slot rules
		instance_.slotGenerator_.clear();
		for (GuidedRule rule : rules) {
			rule.setQueryParams(newQueryParams);
			Slot slot = new Slot(rule.getActionPredicate());
			slot.addNewRule(rule, false);
			instance_.slotGenerator_.add(slot);
		}
		return instance_;
	}

	/**
	 * Sets the instance to a particular generator.
	 * 
	 * @param generator
	 *            The PolicyGenerator instance.
	 */
	public static void setInstance(PolicyGenerator generator) {
		instance_ = generator;
		instance_.moduleGenerator_ = false;
		instance_.moduleGoal_ = null;
	}

	public boolean isFrozen() {
		return frozen_;
	}

	public boolean isModuleGenerator() {
		return moduleGenerator_;
	}

	public boolean isSlotOptimiser() {
		return slotOptimisation_;
	}

	/**
	 * If the experiment should restart learning because the rules have changed.
	 * When this method is called, the restart is no longer active until
	 * triggered again.
	 * 
	 * @return True if rules have recently changed, false otherwise.
	 */
	public boolean shouldRestart() {
		if (restart_) {
			restart_ = false;
			return true;
		}
		return false;
	}

	public String getModuleName() {
		return Module.formName(moduleGoal_);
	}

	public OrderedDistribution<Slot> getGenerator() {
		return slotGenerator_;
	}

	/**
	 * Checks if a particular rule is present in the policy generator.
	 * 
	 * @param gr
	 *            The rule to check.
	 * @return True if the rule is contained within the generator.
	 */
	public boolean contains(GuidedRule gr) {
		for (Slot slot : slotGenerator_) {
			if (slot.getGenerator().contains(gr))
				return true;
		}
		return false;
	}

	/**
	 * Adds LGG rules to this generator.
	 * 
	 * @param lggRules
	 *            The lgg rules (found previously).
	 */
	public void addCoveredRules(MultiMap<String, GuidedRule> lggRules) {
		coveredRules_ = new MultiMap<String, GuidedRule>();
		for (String action : lggRules.keySet()) {
			// Adding the lgg rules to the slots
			Slot slot = findSlot(action);
			for (GuidedRule rule : lggRules.get(action)) {
				rule = (GuidedRule) rule.clone();
				rule.setSpawned(null);
				slot.addNewRule(rule, false);
				coveredRules_.put(action, rule);
			}
		}
	}

	@Override
	public String toString() {
		return slotGenerator_.toString();
	}

	/**
	 * Saves the generators/distributions to file.
	 * 
	 * @param output
	 *            The file to output the generator to.
	 * @throws Exception
	 *             Should something go awry.
	 */
	public void saveGenerators(File output) throws Exception {
		OrderedDistribution<Slot> policyGenerator = PolicyGenerator
				.getInstance().getGenerator();

		FileWriter wr = new FileWriter(output);
		BufferedWriter buf = new BufferedWriter(wr);

		// For each of the rule generators
		for (Slot slot : policyGenerator) {
			buf.write("(" + slot.toParsableString() + ")" + ELEMENT_DELIMITER
					+ policyGenerator.getOrdering(slot) + ELEMENT_DELIMITER
					+ slot.getSelectionProbability() + ELEMENT_DELIMITER
					+ slot.getSelectionSD() + "\n");
		}
		buf.write("Total Update Size: " + updateDifference_ + "\n");
		buf.write("Converged Value: " + convergedValue_);

		buf.close();
		wr.close();
	}

	/**
	 * Saves a human readable version of the generators out to file. Typically
	 * the generators are frozen before this method is called.
	 * 
	 * @param output
	 *            The file to output the human readable generators to.
	 */
	public void saveHumanGenerators(File output) throws Exception {
		OrderedDistribution<Slot> policyGenerator = PolicyGenerator
				.getInstance().getGenerator();

		FileWriter wr = new FileWriter(output);
		BufferedWriter buf = new BufferedWriter(wr);

		buf.write("A typical policy:\n");

		// Go through each slot, writing out those that fire
		List<Slot> orderedElements = policyGenerator.getOrderedElements();
		for (Slot slot : orderedElements) {
			// Output each slot a number of times based on its selection
			// probability.
			double repetitions = Math.round(slot.getSelectionProbability());
			StringBuffer slotOutput = null;
			for (int i = 0; i < repetitions; i++) {
				if (slotOutput == null) {
					slotOutput = new StringBuffer();
					// Output every non-zero rule
					boolean single = true;
					for (GuidedRule rule : slot.getGenerator()
							.getNonZeroOrderedElements()) {
						if (!single)
							slotOutput.append(" / ");
						slotOutput.append(StateSpec.getInstance().encodeRule(
								rule));
						single = false;
					}
					slotOutput.append("\n");
				}

				buf.write(slotOutput.toString());
			}
		}

		buf.close();
		wr.close();
	}

	/**
	 * Loads the generators/distributions from file.
	 * 
	 * @param file
	 *            The file to load from.
	 * @return The policy generator to load to.
	 */
	public void loadGenerators(File input) {
		OrderedDistribution<Slot> loadedDist = new OrderedDistribution<Slot>(
				random_);
		MultiMap<String, GuidedRule> coveredRules = new MultiMap<String, GuidedRule>();

		try {
			FileReader reader = new FileReader(input);
			BufferedReader buf = new BufferedReader(reader);

			// Parse the slots, line by line
			String in = null;
			while ((in = buf.readLine()) != null) {
				// Slot action, rules, and slot ordering
				// e.g. (move{((on a b) (cat c) => (move c))}),0.6532
				Pattern p = Pattern.compile("\\((\\w+)" // (move
						+ "\\{(.*?)\\}" // {((on a b) (cat c) => (move c))}
						+ "\\),([\\d.E-]+),([\\d.E-]+),([\\d.E-]+)"); // ),0.6532,1.3,0.4
				Matcher m = p.matcher(in);

				if (m.find()) {
					// Split the input up
					Slot slot = new Slot(m.group(1));
					String rules = m.group(2);
					Double slotOrder = Double.parseDouble(m.group(3));
					slot.setSelectionProb(Double.parseDouble(m.group(4)));
					slot.setSelectionSD(Double.parseDouble(m.group(5)));

					// Add the rules to the slot
					Pattern rp = Pattern
							.compile("\\(((?:\\(.+?\\) )+=> \\(.+?\\))" // Rule
									+ ":([\\d.E-]+)\\)"); // Rule Prob
					Matcher rm = rp.matcher(rules);
					boolean firstCoveredRule = true;
					while (rm.find()) {
						GuidedRule rule = new GuidedRule(rm.group(1));
						Double ruleProb = Double.parseDouble(rm.group(2));

						slot.addRule(rule, ruleProb);

						if (firstCoveredRule)
							coveredRules.put(slot.getAction(), rule);
						firstCoveredRule = false;
					}

					// Add the slot to the distribution.
					loadedDist.add(slot, slotOrder);
				}
			}

			buf.close();
			reader.close();
		} catch (Exception e) {
			System.err
					.println("Error parsing generator file. Not loading generator.");
			e.printStackTrace();
		}

		slotGenerator_ = loadedDist;
		coveredRules_ = coveredRules;
	}
}