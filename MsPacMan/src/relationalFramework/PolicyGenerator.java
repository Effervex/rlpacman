package relationalFramework;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

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
public class PolicyGenerator implements Serializable {
	private static final long serialVersionUID = -3840268992962159336L;

	/** The probability distributions defining the policy generator. */
	private OrderedDistribution<Slot> slotGenerator_;

	/** The current slots used. */
	private MultiMap<String, Slot> currentSlots_;

	/** The actions set the generator was initialised with. */
	private Map<String, StringFact> actionSet_;

	/** If the generator is currently frozen. */
	private boolean frozen_ = false;

	/** The instance. */
	private static PolicyGenerator instance_;

	/** The rule creation object. */
	protected RuleCreation ruleCreation_;

	/** The set of rlgg rules, some which may be LGG, others not. */
	private MultiMap<String, GuidedRule> rlggRules_;

	/** The list of mutated rules. Mutually exclusive from the other LGG lists. */
	private MultiMap<Slot, GuidedRule> mutatedRules_;

	/** The collection of rules that have been removed. */
	private Collection<GuidedRule> removedRules_;

	/**
	 * The collection of currently active rules, so no duplicate rules are
	 * created.
	 */
	private Collection<GuidedRule> currentRules_;

	/**
	 * The last amount of difference in distribution probabilities from the
	 * update.
	 */
	private transient double klDivergence_;

	/** If this policy generator is being used for learning a module. */
	private transient boolean moduleGenerator_;

	/** The goal this generator is working towards if modular. */
	private transient ArrayList<StringFact> moduleGoal_;

	/** Policies awaiting testing. */
	private Stack<Policy> awaitingTest_;

	/**
	 * If this policy generator only updates the ordering of slots - no rule
	 * creation or modification.
	 */
	private transient boolean slotOptimisation_ = false;

	/**
	 * A flag for when the experiment needs to restart (due to new rules being
	 * added/removed)
	 */
	private transient boolean restart_ = false;

	/** If we're using weighted elite samples. */
	public boolean weightedElites_ = false;

	/** If modules are being used. */
	public boolean useModules_ = false;

	/** If the slots can be dynamically added/removed. */
	public boolean dynamicSlotNumber_ = true;

	/**
	 * The maximum amount of change between the slots before it is considered
	 * converged.
	 */
	private transient double convergedValue_ = 0;

	/** The number of times in a row the updates have been convergent. */
	private transient int convergedStrike_ = 0;

	/** The random number generator. */
	public static Random random_ = new Random();

	/** If we're running the experiment in debug mode. */
	public static boolean debugMode_ = false;

	/** The element delimiter between elements in the generator files. */
	public static final String ELEMENT_DELIMITER = ",";

	/** The minimum value for weight updating. */
	private static final double MIN_UPDATE = 0.1;

	/**
	 * If the summed total update value is only at this percentage of the
	 * update, the distribution is converged.
	 */
	private static final double CONVERGED_EPSILON = 0.01;

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
	 * The resampling bound. If an agent completes that many * the average
	 * episode length without changing action or state, resample policy.
	 */
	public static final double RESAMPLE_POLICY_BOUND = 0.1;

	/**
	 * The constructor for creating a new Policy Generator.
	 */
	public PolicyGenerator() {
		actionSet_ = StateSpec.getInstance().getActions();
		rlggRules_ = MultiMap.createListMultiMap();
		ruleCreation_ = new RuleCreation();
		mutatedRules_ = MultiMap.createListMultiMap();
		removedRules_ = new HashSet<GuidedRule>();
		currentRules_ = new HashSet<GuidedRule>();
		klDivergence_ = Double.MAX_VALUE;

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
		if (!awaitingTest_.isEmpty())
			return awaitingTest_.pop();

		Policy policy = new Policy();

		// Clone the ordered distribution and normalise it so the largest slot
		// usage value is at least 1.
		ProbabilityDistribution<Slot> removalDist = new ProbabilityDistribution<Slot>(
				random_);
		for (Slot slot : slotGenerator_) {
			removalDist.add(slot.clone(), slotGenerator_.getOrdering(slot));
		}

		int i = 0;
		boolean emptyPolicy = true;
		while (!removalDist.isEmpty()) {
			Slot slot = null;
			if (!dynamicSlotNumber_ || slotOptimisation_) {
				// Sample with removal, getting the most likely if frozen.
				slot = removalDist.sampleWithRemoval(false);
			} else {
				slot = removalDist.sample(false);
				boolean[] useSlot = slot.shouldUseSlot(random_);
				// If we are removing the slot, remove it
				if (!useSlot[1])
					removalDist.remove(slot);
				// If we aren't using the slot, set it to null.
				if (!useSlot[0])
					slot = null;
			}

			if (slot != null) {
				emptyPolicy = false;
				if (!slot.isEmpty()) {
					// If influencing towards untested rules, skew the slot
					// distribution towards lesser used rules.
					GuidedRule gr = null;
					if (influenceUntestedRules) {
						gr = slot.getInfluencedDistribution(
								INFLUENCE_THRESHOLD, INFLUENCE_BOOST).sample(
								false);
						gr.incrementRuleUses();
					} else
						gr = slot.sample(false);
					if (gr != null) {
						policy.addRule(gr, true, false);
						slot.getGenerator().remove(gr);
						slot.getGenerator().normaliseProbs();
					}
				}
			}
		}

		// If the policy is empty while there are available slots, try again
		if (emptyPolicy)
			return generatePolicy(influenceUntestedRules);

		// Append the general rules as well (if they aren't already in there) to
		// ensure unnecessary covering isn't triggered.

		// First get them in order
		SortedSet<GuidedRule> orderedSlotRules = new TreeSet<GuidedRule>(
				new Comparator<GuidedRule>() {
					/**
					 * Compares two rules and orders them by slot probability.
					 * 
					 * @param r1
					 *            Rule 1.
					 * @param r2
					 *            Rule 2.
					 * @return -1 if rule 1's slot probability is higher, 0 if
					 *         equal, and 1 if lower.
					 */
					@Override
					public int compare(GuidedRule r1, GuidedRule r2) {
						// Null slot case
						if (r1.getSlot() == null) {
							if (r2.getSlot() != null)
								return r1.compareTo(r2);
							return 1;
						} else if (r2.getSlot() == null)
							return -1;

						int result = Double.compare(slotGenerator_
								.getOrdering(r1.getSlot()), slotGenerator_
								.getOrdering(r2.getSlot()));
						if (result != 0)
							return result;
						return r1.compareTo(r2);
					}
				});

		orderedSlotRules.addAll(rlggRules_.values());
		for (GuidedRule coveredRule : orderedSlotRules) {
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
	public List<GuidedRule> triggerRLGGCovering(Rete state,
			MultiMap<String, String[]> validActions,
			MultiMap<String, String[]> activatedActions, boolean createNewRules) {
		// If there are actions to cover, cover them.
		if (!frozen_ && !slotOptimisation_
				&& isCoveringNeeded(validActions, activatedActions)) {
			List<GuidedRule> covered = null;
			try {
				covered = ruleCreation_.rlggState(state, validActions,
						rlggRules_);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Add remaining information to rules.
			for (GuidedRule coveredRule : covered) {
				if (coveredRule.isRecentlyModified() && createNewRules) {
					System.out.println("\tCOVERED RULE: " + coveredRule);
					restart_ = true;
				}

				StringFact action = coveredRule.getAction();
				if (coveredRule.getSlot() == null) {
					Slot slot = findSlot(action.getFactName());

					// Adding the rule to the slot
					slot.addNewRule(coveredRule);
				}

				// Add to the covered rules
				rlggRules_.putContains(action.getFactName(), coveredRule);
			}

			// Split the slots
			splitSlots();

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
	 * @return True if covering is needed.
	 */
	private boolean isCoveringNeeded(MultiMap<String, String[]> validActions,
			MultiMap<String, String[]> activatedActions) {
		for (String action : validActions.keySet()) {
			// If the activated actions don't even contain the key, return
			// true.
			if (!activatedActions.containsKey(action)) {
				ruleCreation_.resetInactivity();
				return true;
			}

			// Check each set of actions match up
			if (!activatedActions.get(action).containsAll(
					(validActions.get(action)))) {
				ruleCreation_.resetInactivity();
				return true;
			}
		}
		return false;
	}

	/**
	 * If this rule is allowed to mutate.
	 * 
	 * @param rule
	 *            The rule to check for mutation criteria.
	 * @param mutationUses
	 *            The minimum number of uses the rule must have to mutate.
	 * @param preGoalHash
	 *            The hash of the state of the pre-goal and other observations.
	 * @return True if the rule can mutate.
	 */
	private boolean ruleCanMutate(GuidedRule rule, int mutationUses,
			int preGoalHash) {
		// If the rule has already spawned under this hash.
		if (rule.hasSpawned(preGoalHash))
			return false;

		// If the rule is below average probability
		if (mutationUses > 0
				&& rule.getSlot().getGenerator().getProb(rule) < (1.0 / rule
						.getSlot().size()))
			return false;

		// If the rule is not experienced enough
		if (rule.getUses() < mutationUses)
			return false;

		if (rule.getParentRules() != null) {
			// If the rule has a worse probability than the average of its
			// parents
			double parentsProb = 0;
			ProbabilityDistribution<GuidedRule> distribution = rule.getSlot()
					.getGenerator();
			Collection<GuidedRule> removedParents = new HashSet<GuidedRule>();
			for (GuidedRule parent : rule.getParentRules()) {
				Double parentProb = distribution.getProb(parent);
				if (parentProb == null)
					removedParents.add(parent);
				else
					parentProb += parentProb;
			}

			rule.removeParents(removedParents);
			if (rule.getParentRules() != null)
				parentsProb /= rule.getParentRules().size();

			if (distribution.getProb(rule) < parentsProb)
				return false;
		}

		return true;
	}

	/**
	 * Splits the slots by mutating the covered rules through the specialiseRule
	 * method and creating new slots from the mutants.
	 */
	private void splitSlots() {
		// Split each rlgg slot, removing old unnecessary slots if needed
		currentRules_.clear();
		currentRules_.addAll(rlggRules_.values());

		boolean changed = false;
		for (GuidedRule rlgg : rlggRules_.values()) {
			// Only re-split the slots if the observation hash has changed.
			if (ruleCanMutate(rlgg, -1, ruleCreation_.getMutationHash(rlgg
					.getActionPredicate()))) {
				if (debugMode_) {
					try {
						System.out.println("\tSPLITTING SLOT "
								+ rlgg.getActionPredicate());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				// Clear the current slots of this action
				String action = rlgg.getActionPredicate();
				currentSlots_.clearValues(action);

				SortedSet<StringFact> rlggConditions = rlgg
						.getConditions(false);
				Slot rlggSlot = rlgg.getSlot();
				// Split the slots and add them.
				Set<GuidedRule> splitSeeds = ruleCreation_.specialiseRule(rlgg);
				currentRules_.addAll(splitSeeds);
				for (GuidedRule seedRule : splitSeeds) {
					// The seed rule becomes parent-less and a non-mutant
					seedRule.removeMutation();

					SortedSet<StringFact> slotConditions = seedRule
							.getConditions(false);
					slotConditions.removeAll(rlggConditions);

					Slot splitSlot = new Slot(rlggSlot.getAction(),
							slotConditions);
					splitSlot.addNewRule(seedRule);

					// Check if the slot already exists
					// TODO Might be better to initialise slots with 1/slot
					// number probability and make them work to get more. Test
					// with experiments.
					// Could be problematic with Ms. Pac-Man and other
					// multi-action environments.

					Slot existingSlot = slotGenerator_.getElement(splitSlot);
					if (existingSlot != null) {
						if (existingSlot.getSeedRule().equals(seedRule)) {
							splitSlot = existingSlot;
							seedRule = existingSlot.getSeedRule();
						} else {
							slotGenerator_.remove(existingSlot);
						}

						if (debugMode_) {
							try {
								System.out.println("\tEXISTING SLOT SEED: "
										+ slotConditions + " => "
										+ rlgg.getActionPredicate());
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} else if (debugMode_) {
						try {
							System.out.println("\tNEW SLOT SEED: "
									+ slotConditions + " => "
									+ rlgg.getActionPredicate());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					// Mutate new rules for the slot
					mutateRule(seedRule, splitSlot, -1);

					currentSlots_.put(action, splitSlot);
				}

				// Mutate the rlgg slot to create any remaining mutations
				mutateRule(rlgg, rlggSlot, -1);
				currentSlots_.put(action, rlggSlot);
				changed = true;
			}
		}

		// If the slots have changed, add the new ones (removing the old).
		if (changed) {
			slotGenerator_.retainAll(currentSlots_.values());
			slotGenerator_.addContainsAll(currentSlots_.values());
			if (Slot.INITIAL_SLOT_PROB == -1) {
				for (Slot slot : slotGenerator_)
					slot.setSelectionProb(1.0 / slotGenerator_.size());
			}
		}
	}

	/**
	 * Mutates a rule (if it hasn't spawned already, though the covered rule is
	 * always checked) and creates and adds children to the slots.
	 * 
	 * @param baseRule
	 *            The rule to mutate.
	 * @param ruleSlot
	 *            The slot in which the base rule is from.
	 * @param mutationUses
	 *            The number of uses the rule must have before mutating. Can be
	 *            -1 for don't care.
	 */
	private void mutateRule(GuidedRule baseRule, Slot ruleSlot, int mutationUses) {
		int preGoalHash = ruleCreation_.getMutationHash(baseRule
				.getActionPredicate());
		// If the rule can mutate.
		if (ruleCanMutate(baseRule, mutationUses, preGoalHash)) {
			boolean needToPause = false;
			// Remove old rules if this rule is an LGG rule.
			boolean removeOld = (baseRule.isMutant()) ? false : true;
			if (debugMode_) {
				try {
					System.out.println("\tMUTATING " + baseRule);
					System.out.println("PRE-GOAL: ");
					for (String action : StateSpec.getInstance().getActions()
							.keySet()) {
						System.out.println(action + ": "
								+ ruleCreation_.getPreGoalState(action));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			Set<GuidedRule> mutants = ruleCreation_
					.specialiseToPreGoal(baseRule);
			if (ruleSlot.getSlotSplitFacts() != null)
				mutants.addAll(ruleCreation_.specialiseRule(baseRule));
			baseRule.setSpawned(preGoalHash);

			// Ensure no duplicate rules.
			mutants.removeAll(currentRules_);

			// If the slot has settled, remove any mutants not present in
			// the permanent mutant set
			if (removeOld) {
				if (mutatedRules_.get(ruleSlot) != null) {
					// Run through the rules in the slot, removing any direct
					// mutants of the base rule not in the current set of direct
					// mutants.
					List<GuidedRule> removables = new ArrayList<GuidedRule>();
					ProbabilityDistribution<GuidedRule> distribution = ruleSlot
							.getGenerator();
					for (GuidedRule gr : distribution) {
						// If the rule was once a child of this rule and the
						// current set of mutants doesn't contain the rule,
						// remove the parent child link and possibly remove the
						// rule as well.
						if ((gr.getParentRules() != null)
								&& gr.getParentRules().contains(baseRule)
								&& !mutants.contains(gr)) {
							needToPause = true;
							gr.removeParent(baseRule);
							if (gr.isWithoutParents())
								removables.add(gr);

							if (debugMode_) {
								System.out.println("\tREMOVING MUTANT: " + gr);
							}
						}
					}

					// Setting the restart value.
					if (!removables.isEmpty()) {
						restart_ = true;
					}

					if (ruleSlot.getGenerator().removeAll(removables))
						ruleSlot.getGenerator().normaliseProbs();
					mutatedRules_.get(ruleSlot).removeAll(removables);
				}
			} else {
				restart_ = false;
			}

			// Add all mutants to the ruleSlot
			for (GuidedRule gr : mutants) {
				if (!removedRules_.contains(gr)) {
					// Only add if not already in there
					ruleSlot.addNewRule(gr);

					if (debugMode_) {
						if (mutatedRules_.containsValue(gr))
							System.out.println("\tEXISTING MUTANT: " + gr);
						else {
							needToPause = true;
							System.out.println("\tADDED MUTANT: " + gr);
						}
					}

					mutatedRules_.putContains(ruleSlot, gr);
					currentRules_.addAll(mutants);
				}
			}

			if (debugMode_ && needToPause) {
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
	 *            Extra constants to note down in the pre-goal.
	 */
	public void formPreGoalState(Collection<Fact> preGoalState,
			ActionChoice actions, Collection<String> constants) {
		if (!frozen_ && !slotOptimisation_) {
			// Form the pre-goal using the final action/s as a parameter.
			Collection<String> settledGoals = ruleCreation_.formPreGoalState(
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
				if (ruleCreation_.isPreGoalRecentlyChanged(action)
						&& rlggRules_.containsKey(action)) {
					splitSlots();
				}
			}

			if (debugMode_) {
				try {
					for (String stateAction : StateSpec.getInstance()
							.getActions().keySet()) {
						System.out.println(stateAction + ": "
								+ ruleCreation_.getPreGoalState(stateAction));
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
		return ruleCreation_.hasPreGoal();
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
			if ((slot.getAction().equals(action)) && (!slot.isFixed())
					&& (slot.getSlotSplitFacts() == null))
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
			for (Slot slot : slotGenerator_) {
				slot.freeze(freeze);
			}
		} else {
			for (Slot slot : slotGenerator_) {
				slot.freeze(freeze);
			}
		}
	}

	/**
	 * Resets the generator to where it started.
	 */
	public void resetGenerator() {
		slotGenerator_ = new OrderedDistribution<Slot>(random_);
		currentSlots_ = MultiMap.createListMultiMap();
		for (String action : actionSet_.keySet()) {
			Slot slot = new Slot(action);
			slotGenerator_.add(slot);
			currentSlots_.put(action, slot);
		}

		rlggRules_.clear();
		mutatedRules_.clear();
		removedRules_.clear();
		currentRules_.clear();

		awaitingTest_ = new Stack<Policy>();
	}

	/**
	 * If the slots and rules within are converged to stability.
	 * 
	 * @return True if the generator is converged, false otherwise.
	 */
	public boolean isConverged() {
		if (klDivergence_ < convergedValue_)
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

		klDivergence_ = 0;

		// Update the rule distributions and slot activation probability
		if (!slotOptimisation_) {
			for (Slot slot : slotGenerator_) {
				// Slot selection values
				double mean = ed.getSlotNumeracyMean(slot);
				double sd = ed.getSlotNumeracySD(slot);
				slot.updateSelectionValues(mean, sd, stepSize);

				// double numeracyBalancedStepSize = stepSize * mean;
				klDivergence_ += slot.getGenerator().updateDistribution(
						ed.getSlotCount(slot), ed.getRuleCounts(), stepSize);
				// stepSizes.put(slot, numeracyBalancedStepSize);
			}
		}
		// Update the slot distribution
		klDivergence_ += slotGenerator_.updateDistribution(ed
				.getSlotPositions(), stepSize);

		convergedValue_ = stepSize * CONVERGED_EPSILON;
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
		MultiMap<Slot, Double> slotNumeracy = MultiMap.createListMultiMap();
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
					ed.addSlotOrdering(ruleSlot,
							(1.0 * firedRuleIndex / eliteSolution.size()));
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
	public void postUpdateOperations(int mutationUses, double pruneConst) {
		if (slotOptimisation_)
			return;

		// Mutate the rules further
		if (debugMode_) {
			try {
				System.out.println("\tPERFORMING POST-UPDATE OPERATIONS");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// For each slot
		for (Slot slot : slotGenerator_) {
			// Pruning unused rules from the slots
			ProbabilityDistribution<GuidedRule> distribution = slot
					.getGenerator();
			// Only prune if the distribution is bigger than 1
			if (distribution.size() > 1) {
				double pruneProb = (1.0 / distribution.size()) * pruneConst;
				Collection<GuidedRule> removables = new ArrayList<GuidedRule>();
				for (GuidedRule rule : distribution) {
					double ruleProb = distribution.getProb(rule);
					if (ruleProb <= pruneProb) {
						removables.add(rule);
						// Note the rule in the no-create list
						removedRules_.add(rule);
						currentRules_.remove(rule);
					}
				}

				// If rules are to be removed, remove them.
				if (!removables.isEmpty()) {
					for (GuidedRule rule : removables) {
						distribution.remove(rule);
						System.out.println("\tREMOVED RULE: " + rule);
					}

					distribution.normaliseProbs();
				}
			}

			// Sample a rule from the slot and mutate it if it has seen
			// sufficient states.
			if (distribution.size() > 0) {
				GuidedRule rule = distribution.sample(false);
				if (distribution.isEmpty() || rule == null)
					System.out.println("Problem... " + slot);
				mutateRule(rule, slot, mutationUses);
			}
		}

		// Mutate the rules further
		if (debugMode_) {
			try {
				System.out.println(slotGenerator_);
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
				if (ruleCreation_.isPreGoalSettled(gr.getActionPredicate())) {
					ConstantPred constants = gr.getConstantConditions();
					if (constants != null)
						constantFacts.add(constants);
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
			ArrayList<StringFact> internalGoal) {
		instance_ = new PolicyGenerator();
		instance_.addCoveredRules(policyGenerator.rlggRules_);
		policyGenerator.ruleCreation_
				.migrateAgentObservations(instance_.ruleCreation_);
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
			ArrayList<StringFact> internalGoal) {
		instance_ = newInstance(policyGenerator, internalGoal);
		instance_.slotOptimisation_ = true;
		policyGenerator.ruleCreation_
				.migrateAgentObservations(instance_.ruleCreation_);

		// Set the slot rules
		instance_.slotGenerator_.clear();
		for (GuidedRule rule : rules) {
			rule.setQueryParams(newQueryParams);
			Slot slot = new Slot(rule.getActionPredicate());
			slot.addNewRule(rule);
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
		if (generator == instance_)
			return;
		instance_ = generator;
		instance_.moduleGenerator_ = false;
		instance_.moduleGoal_ = null;
		instance_.ruleCreation_.loadBackupPreGoal();
	}

	/**
	 * Loads a serialised policy generator.
	 * 
	 * @param serializedFile
	 *            The serialised generator file to load.
	 * @param run
	 *            The run value.
	 * @return The instantiated Policy Generator of the file.
	 */
	public static PolicyGenerator loadPolicyGenerator(File serializedFile) {
		try {
			FileInputStream fis = new FileInputStream(serializedFile);
			ObjectInputStream ois = new ObjectInputStream(fis);
			instance_ = (PolicyGenerator) ois.readObject();
			ois.close();
			return instance_;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
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
			if (slot.contains(gr))
				return true;
		}
		return false;
	}

	/**
	 * Adds LGG rules to this generator.
	 * 
	 * @param rlggRules
	 *            The lgg rules (found previously).
	 */
	public void addCoveredRules(MultiMap<String, GuidedRule> rlggRules) {
		rlggRules_ = MultiMap.createListMultiMap();
		for (String action : rlggRules.keySet()) {
			// Adding the lgg rules to the slots
			Slot slot = findSlot(action);
			for (GuidedRule rule : rlggRules.get(action)) {
				rule = (GuidedRule) rule.clone();
				rule.setSpawned(null);
				slot.addNewRule(rule);
				rlggRules_.put(action, rule);
			}
		}
		splitSlots();
	}

	@Override
	public String toString() {
		return slotGenerator_.toString();
	}

	/**
	 * Saves the generators into a predefined stream.
	 * 
	 * @param buf
	 *            The predefined stream to write to.
	 * @param performanceFile
	 *            The path to the temporary performance file.
	 */
	public void saveGenerators(BufferedWriter buf, String performanceFile)
			throws Exception {
		// For each of the rule generators
		buf.write(slotGenerator_.toString() + "\n");
		buf.write("Total Update Size: " + klDivergence_ + "\n");
		buf.write("Converged Value: " + convergedValue_);
		// Serialise and save policy generator.
		savePolicyGenerator(new File(performanceFile + ".ser"));
	}

	/**
	 * Saves the policy generator in a serializable format.
	 * 
	 * @param serFile
	 *            The file to serialize to.
	 */
	private void savePolicyGenerator(File serFile) throws Exception {
		FileOutputStream fos = new FileOutputStream(serFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);

		oos.writeObject(this);
		oos.close();
	}

	/**
	 * Saves the generators to an existing stream.
	 * 
	 * @param buf
	 *            The stream to save the generators to.
	 */
	public void saveHumanGenerators(BufferedWriter buf) throws IOException {
		// First check if the slots are frozen
		boolean unfreeze = !frozen_;
		if (unfreeze)
			freeze(true);

		buf.write("A typical policy:\n");

		// Go through each slot, writing out those that fire
		List<Slot> orderedElements = slotGenerator_.getOrderedElements();
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
						slotOutput.append(rule.toNiceString());
						single = false;
					}
					slotOutput.append("\n");
				}

				buf.write(slotOutput.toString());
			}
		}

		if (unfreeze)
			freeze(false);
	}

	/**
	 * Saves the agent observations to file.
	 */
	public void saveAgentObservations(int run) {
		ruleCreation_.saveAgentObservations();
	}

	/**
	 * Retests a policy by adding it to a stack of policies for use later when a
	 * generated policy is requested.
	 * 
	 * @param policy
	 *            The policy to retest.
	 */
	public void retestPolicy(Policy policy) {
		awaitingTest_.push(new Policy(policy));
	}

	/**
	 * Testing method. Gets the pregoal for move.
	 * 
	 * @param actionPred
	 *            The name of the action.
	 * @return The pre-goal for that action.
	 */
	public Collection<StringFact> getPreGoal(String actionPred) {
		return ruleCreation_.getPreGoalState(actionPred);
	}

	/**
	 * Should only be a test method.
	 */
	public void clearAgentObservations() {
		ruleCreation_.clearAgentObservations();
	}
}