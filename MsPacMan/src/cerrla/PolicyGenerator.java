package cerrla;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

import relationalFramework.GoalCondition;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import relationalFramework.agentObservations.AgentObservations;
import relationalFramework.util.MultiMap;
import relationalFramework.util.ProbabilityDistribution;
import relationalFramework.util.SelectableSet;
import relationalFramework.util.SlotOrderComparator;

import jess.Rete;

/**
 * A class for the generation of policies. The number of slots and the rules
 * within the slots can dynamically change.
 * 
 * @author Samuel J. Sarjant
 */
public final class PolicyGenerator implements Serializable {
	private static final long serialVersionUID = -7316823916026570650L;

	/** The instance. */
	private static PolicyGenerator instance_;

	/**
	 * The increment for an ordering value to increase when resolving ordering
	 * value clashes.
	 */
	private static final double ORDER_CLASH_INCREMENT = 0.001;

	/** If we're running the experiment in debug mode. */
	public static boolean debugMode_ = false;

	/** The element delimiter between elements in the generator files. */
	public static final String ELEMENT_DELIMITER = ",";

	/** The random number generator. */
	public static Random random_ = new Random(0);

	/** The name of a module-saved policy generator serialised file. */
	public static final String SERIALISED_FILENAME = "policyGenerator.ser";

	/** Policies awaiting testing. */
	private Stack<RelationalPolicy> awaitingTest_;

	/** The number of times in a row the updates have been convergent. */
	private transient int convergedStrike_ = 0;

	/**
	 * The maximum amount of change between the slots before it is considered
	 * converged.
	 */
	private transient double convergedValue_ = 0;

	/**
	 * The collection of currently active rules, so no duplicate rules are
	 * created.
	 */
	private Collection<RelationalRule> currentRules_;

	/** The current slots used. */
	private MultiMap<String, Slot> currentSlots_;

	/** If the generator is currently frozen. */
	private boolean frozen_ = false;

	/**
	 * The last amount of difference in distribution probabilities from the
	 * update.
	 */
	private transient double klDivergence_;

	/** Gets the local goal this policy generator is working towards. */
	private String localGoal_;

	/** If this policy generator is being used for learning a module. */
	private boolean moduleGenerator_;

	/** The goal this generator is working towards if modular. */
	private GoalCondition moduleGoal_;

	/** The list of mutated rules. Mutually exclusive from the other LGG lists. */
	private MultiMap<Slot, RelationalRule> mutatedRules_;

	/** The collection of rules that have been removed. */
	private Collection<RelationalRule> removedRules_;

	/**
	 * A flag for when the experiment needs to restart (due to new rules being
	 * added/removed)
	 */
	private transient boolean restart_ = false;

	/** The set of rlgg rules, some which may be LGG, others not. */
	private Map<String, RelationalRule> rlggRules_;

	/** The probability distributions defining the policy generator. */
	private SelectableSet<Slot> slotGenerator_;

	/** The rule creation object. */
	protected RuleCreation ruleCreation_;

	/** Total policies evaluated. */
	private int policiesEvaluated_;

	/**
	 * The constructor for creating a new Policy Generator.
	 */
	public PolicyGenerator() {
		rlggRules_ = new HashMap<String, RelationalRule>();
		ruleCreation_ = new RuleCreation();
		mutatedRules_ = MultiMap.createListMultiMap();
		removedRules_ = new HashSet<RelationalRule>();
		currentRules_ = new HashSet<RelationalRule>();
		currentSlots_ = MultiMap.createListMultiMap();
		klDivergence_ = Double.MAX_VALUE;
		policiesEvaluated_ = 0;

		resetGenerator();
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
	private ElitesData countRules(SortedSet<PolicyValue> elites) {
		ElitesData ed = new ElitesData();

		double gradient = 0;
		double offset = 1;
		if (ProgramArgument.WEIGHTED_UPDATES.booleanValue()) {
			double diffValues = (elites.first().getValue() - elites.last()
					.getValue());
			if (diffValues != 0)
				gradient = (1 - ProgramArgument.MIN_WEIGHTED_UPDATE
						.doubleValue()) / diffValues;
			offset = 1 - gradient * elites.first().getValue();
		}

		// Only selecting the top elite samples
		MultiMap<Slot, Double> slotNumeracy = MultiMap.createListMultiMap();
		for (PolicyValue pv : elites) {
			Map<Slot, Integer> policySlotCounts = new HashMap<Slot, Integer>();
			double weight = pv.getValue() * gradient + offset;
			RelationalPolicy eliteSolution = pv.getPolicy();

			// Count the occurrences of rules and slots in the policy
			Collection<RelationalRule> firingRules = eliteSolution
					.getFiringRules();
			int firedRuleIndex = 0;
			for (RelationalRule rule : firingRules) {
				Slot ruleSlot = rule.getSlot();
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
	 * Mutates a rule (if it hasn't spawned already, though the covered rule is
	 * always checked) and creates and adds children to the slots.
	 * 
	 * @param baseRule
	 *            The rule to mutate.
	 * @param ruleSlot
	 *            The slot in which the base rule is from.
	 * @param mutationUses
	 *            The number of mutations required for a rule to mutate if using
	 *            non-dynamic slots.
	 */
	private void mutateRule(RelationalRule baseRule, Slot ruleSlot,
			int mutationUses) {
		int observationHash = AgentObservations.getInstance()
				.getObservationHash();
		if (ProgramArgument.DYNAMIC_SLOTS.booleanValue()
				|| ruleCanMutate(baseRule, mutationUses, observationHash)) {
			// If the rule can mutate.
			boolean needToPause = false;
			// Remove old rules if this rule is an LGG rule.
			boolean removeOld = (baseRule.isMutant()) ? false : true;
			if (debugMode_) {
				try {
					System.out.println("\tMUTATING " + baseRule);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			Set<RelationalRule> mutants = ruleCreation_
					.specialiseRuleMinor(baseRule);
			if (ProgramArgument.DYNAMIC_SLOTS.booleanValue()
					|| ruleSlot.getSlotSplitFacts() != null)
				mutants.addAll(ruleCreation_.specialiseRule(baseRule));
			baseRule.setSpawned(observationHash);

			// Ensure no duplicate rules.
			// mutants.removeAll(currentRules_);

			// If the slot has settled, remove any mutants not present in
			// the permanent mutant set
			if (removeOld) {
				if (mutatedRules_.get(ruleSlot) != null) {
					// Run through the rules in the slot, removing any direct
					// mutants of the base rule not in the current set of direct
					// mutants.
					List<RelationalRule> removables = new ArrayList<RelationalRule>();
					ProbabilityDistribution<RelationalRule> distribution = ruleSlot
							.getGenerator();
					for (RelationalRule gr : distribution) {
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
			for (RelationalRule gr : mutants) {
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
	 * Checks if a particular rule is present in the policy generator.
	 * 
	 * @param gr
	 *            The rule to check.
	 * @return True if the rule is contained within the generator.
	 */
	public boolean contains(RelationalRule gr) {
		for (Slot slot : slotGenerator_) {
			if (slot.contains(gr))
				return true;
		}
		return false;
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
	 * Generates a random policy using the weights present in the probability
	 * distribution.
	 * 
	 * @param influenceUntestedRules
	 *            Influence selection of rules towards untested rules.
	 * @return A new policy, formed using weights from the probability
	 *         distributions.
	 */
	public RelationalPolicy generatePolicy(boolean influenceUntestedRules) {
		if (!awaitingTest_.isEmpty())
			return awaitingTest_.pop();

		RelationalPolicy policy = new RelationalPolicy();

		SortedMap<Double, RelationalRule> policyOrdering = new TreeMap<Double, RelationalRule>();

		// Run through every slot, adding them where possible. Add slots to a
		// sorted map, which determines the ordering.
		boolean noRules = true;
		for (Slot slot : slotGenerator_) {
			double slotOrdering = slot.getOrdering();
			double slotOrderSD = slot.getOrderingSD();

			// Insert the slot as many times as required.
			if (!slot.isEmpty()) {
				noRules = false;
				if (slot.useSlot(random_)) {
					double slotOrderVal = slotOrdering + random_.nextGaussian()
							* slotOrderSD;
					// Ensure the slot is placed in a unique order - no clashes.
					while (policyOrdering.containsKey(slotOrderVal))
						slotOrderVal += ORDER_CLASH_INCREMENT;

					// Sample a rule and add it
					RelationalRule gr = null;
					if (influenceUntestedRules) {
						gr = slot.sample(1, 1);
					} else
						gr = slot.sample(false);
					gr.incrementRuleUses();
					policyOrdering.put(slotOrderVal, gr);
				}
			}
		}

		// If there are no rules, return an empty policy.
		if (noRules)
			return policy;

		// If the policy is empty while there are available slots, try again
		if (policyOrdering.isEmpty())
			return generatePolicy(influenceUntestedRules);

		// Add the rules, noting the RLGG rules.
		for (Double orders : policyOrdering.keySet()) {
			RelationalRule rule = policyOrdering.get(orders);
			policy.addRule(rule, true, false);
		}

		return policy;
	}

	public double getConvergenceValue() {
		return convergedValue_;
	}

	public Collection<Slot> getGenerator() {
		return slotGenerator_;
	}

	public double getKLDivergence() {
		return klDivergence_;
	}

	public GoalCondition getModuleGoal() {
		return moduleGoal_;
	}

	public Map<String, RelationalRule> getRLGGRules() {
		return rlggRules_;
	}

	public String getLocalGoal() {
		return localGoal_;
	}

	public int getPoliciesEvaluated() {
		return policiesEvaluated_;
	}
	
	public void incrementPoliciesEvaluated() {
		policiesEvaluated_++;
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

		if (convergedStrike_ >= ProgramArgument.NUM_UPDATES_CONVERGED
				.intValue())
			return true;
		return false;
	}

	public boolean isFrozen() {
		return frozen_;
	}

	public boolean isModuleGenerator() {
		return moduleGenerator_;
	}

	/**
	 * Operations to run after the distributions have been updated. This
	 * includes further mutation of useful rules.
	 * 
	 * @param mutationUses
	 *            The number of times a rule can be used before it can create
	 *            mutations.
	 * @return True if new rules were created.
	 */
	public boolean postUpdateOperations(int mutationUses) {
		// Mutate the rules further
		if (debugMode_) {
			try {
				System.out.println("\tPERFORMING POST-UPDATE OPERATIONS");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Check if slots are ready for splitting //
		boolean mutated = false;

		// For each slot
		Collection<Slot> newSlots = new ArrayList<Slot>();
		for (Slot slot : slotGenerator_) {
			if (!ProgramArgument.DYNAMIC_SLOTS.booleanValue()) {
				if (!slot.isFixed()) {
					ProbabilityDistribution<RelationalRule> distribution = slot
							.getGenerator();

					// Sample a rule from the slot and mutate it if it has seen
					// sufficient states.
					if (distribution.size() > 0) {
						RelationalRule rule = distribution.sample(false);
						if (distribution.isEmpty() || rule == null)
							System.out.println("Problem... " + slot);
						mutateRule(rule, slot, mutationUses);
					}
				}
			} else {
				int slotLevel = slot.getLevel() + 1;
				// Continue to split slot until its KL size has increased to a
				// non-splitting threshold.
				while (slot.isSplittable()) {
					ProbabilityDistribution<RelationalRule> ruleGenerator = slot
							.getGenerator();
					RelationalRule bestRule = ruleGenerator.getBestElement();
					// If the best rule isn't the seed, create a new slot
					if (!bestRule.equals(slot.getSeedRule())) {
						Slot newSlot = new Slot(bestRule, false, slotLevel);
						if (slotGenerator_.contains(newSlot)) {
							// If the slot already exists, update the level if
							// lower
							Slot existingSlot = slotGenerator_
									.findMatch(newSlot);
							existingSlot.updateLevel(slotLevel);
							// Move the rule's slot to the existing slot for
							// further update operations.
							bestRule.setSlot(existingSlot);
						} else {
							// Set ordering to be the same
							newSlot.setOrdering(slot.getOrdering());
							mutateRule(bestRule, newSlot, -1);
							newSlots.add(newSlot);
							mutated = true;
						}
						ruleGenerator.remove(bestRule);
						ruleGenerator.normaliseProbs();
					} else {
						break;
					}
				}
			}
		}
		slotGenerator_.addAll(newSlots);

		// Output the slot generator.
		if (debugMode_) {
			try {
				System.out.println(slotGenerator_);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return mutated;
	}

	/**
	 * Resets the generator to where it started.
	 */
	public void resetGenerator() {
		slotGenerator_ = new SelectableSet<Slot>();

		rlggRules_.clear();
		mutatedRules_.clear();
		removedRules_.clear();
		currentRules_.clear();
		currentSlots_.clear();

		awaitingTest_ = new Stack<RelationalPolicy>();
	}

	/**
	 * Retests a policy by adding it to a stack of policies for use later when a
	 * generated policy is requested.
	 * 
	 * @param policy
	 *            The policy to retest.
	 */
	public void retestPolicy(RelationalPolicy policy) {
		awaitingTest_.push(new RelationalPolicy(policy));
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
		SortedSet<Slot> orderSlots = new TreeSet<Slot>(
				SlotOrderComparator.getInstance());
		orderSlots.addAll(slotGenerator_);
		StringBuffer slotBuffer = new StringBuffer();
		for (Slot s : orderSlots)
			slotBuffer.append(s.toString() + "\n");

		// For each of the rule generators
		buf.write(slotBuffer.toString() + "\n");
		buf.write("Total Update Size: " + klDivergence_ + "\n");
		buf.write("Converged Value: " + convergedValue_);
	}

	/**
	 * Saves the generators to an existing stream.
	 * 
	 * @param buf
	 *            The stream to save the generators to.
	 */
	public void saveHumanGenerators(BufferedWriter buf) throws IOException {
		buf.write("A typical policy:\n");

		// Generate a bunch of policies and display the most frequent one
		RelationalPolicy bestPolicy = null;
		int bestCount = 0;
		Map<RelationalPolicy, Integer> policies = new HashMap<RelationalPolicy, Integer>();
		for (int i = 0; i < ProgramArgument.TEST_ITERATIONS.intValue(); i++) {
			RelationalPolicy policy = generatePolicy(false);
			Integer count = policies.get(policy);
			if (count == null)
				count = 0;
			count++;
			policies.put(policy, count);

			if (count > bestCount) {
				bestCount = count;
				bestPolicy = policy;
			}
		}

		buf.write(bestPolicy.toString());
	}

	/**
	 * Saves the policy generator in a serializable format.
	 * 
	 * @param serFile
	 *            The file to serialize to.
	 */
	public void savePolicyGenerator(File serFile) throws Exception {
		FileOutputStream fos = new FileOutputStream(serFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);

		Module.saveGenerator(this);

		oos.writeObject(this);
		oos.close();
	}

	public void setPoliciesEvaluated(int pe) {
		policiesEvaluated_ = pe;
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

	@Override
	public String toString() {
		return slotGenerator_.toString();
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
	 * @param goalTerms
	 *            The replacement terms for the goal.
	 * @return The list of covered rules, one for each action type.
	 */
	public List<RelationalRule> triggerRLGGCovering(Rete state,
			MultiMap<String, String[]> validActions,
			Map<String, String> goalReplacements,
			MultiMap<String, String[]> activatedActions) {
		// If there are actions to cover, cover them.
		if (!frozen_
				&& AgentObservations.getInstance().isCoveringNeeded(
						validActions, activatedActions)) {
			List<RelationalRule> covered = null;
			try {
				covered = ruleCreation_.rlggState(state, validActions,
						goalReplacements);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Add remaining information to rules.
			for (RelationalRule coveredRule : covered) {
				if (coveredRule.isRecentlyModified()) {
					System.out.println("\tCOVERED RULE: " + coveredRule);
					restart_ = true;
				}

				RelationalPredicate action = coveredRule.getAction();
				if (coveredRule.getSlot() == null) {
					Slot slot = new Slot(coveredRule, false, 0);
					if (ProgramArgument.DYNAMIC_SLOTS.booleanValue())
						mutateRule(coveredRule, slot, -1);
					slotGenerator_.add(slot);
					currentSlots_.clearValues(coveredRule.getActionPredicate());
					currentSlots_.put(coveredRule.getActionPredicate(), slot);
				}

				// Add to the covered rules
				rlggRules_.put(action.getFactName(), coveredRule);

			}

			// If not using dynamic slots, split the slots now.
			if (!ProgramArgument.DYNAMIC_SLOTS.booleanValue()) {
				splitRLGGSlots();
			}

			return covered;
		}
		return null;
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
	private boolean ruleCanMutate(RelationalRule rule, int mutationUses,
			int preGoalHash) {
		// If the rule has already spawned under this hash.
		if (rule.hasSpawned(preGoalHash))
			return false;

		// (Only check for postUpdate mutations - not split slot mutations)
		// If the rule's slot isn't already full (using klSize() to simulate
		// pruning)
		int maximumSlotCapacity = rule.getSlot().getMaximumCapacity();
		if ((mutationUses > -1)
				&& rule.getSlot().klSize() > maximumSlotCapacity)
			return false;

		// If the rule is below average probability (this remains as .size() as
		// probabilities will change to reflect a changing size)
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
			ProbabilityDistribution<RelationalRule> distribution = rule
					.getSlot().getGenerator();
			Collection<RelationalRule> removedParents = new HashSet<RelationalRule>();
			for (RelationalRule parent : rule.getParentRules()) {
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
	private void splitRLGGSlots() {
		// Split each rlgg slot, removing old unnecessary slots if needed
		currentRules_.clear();
		currentRules_.addAll(rlggRules_.values());

		boolean changed = false;
		for (RelationalRule rlgg : rlggRules_.values()) {
			// Only re-split the slots if the observation hash has changed.
			if (ruleCanMutate(rlgg, -1, AgentObservations.getInstance()
					.getObservationHash())) {
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

				Slot rlggSlot = rlgg.getSlot();
				// Split the slots and add them.
				Set<RelationalRule> splitSeeds = ruleCreation_
						.specialiseRule(rlgg);
				currentRules_.addAll(splitSeeds);
				for (RelationalRule seedRule : splitSeeds) {
					// The seed rule becomes parent-less and a non-mutant
					seedRule.removeMutation();

					Slot splitSlot = new Slot(seedRule, false, 1);

					// Check if the slot already exists
					Slot existingSlot = slotGenerator_.findMatch(splitSlot);
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
										+ splitSlot.getSlotSplitFacts()
										+ " => " + rlgg.getActionPredicate());
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} else if (debugMode_) {
						try {
							System.out.println("\tNEW SLOT SEED: "
									+ splitSlot.getSlotSplitFacts() + " => "
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
			if (ProgramArgument.INITIAL_SLOT_MEAN.doubleValue() == -1) {
				for (Slot slot : slotGenerator_)
					slot.setSelectionProb(1.0 / slotGenerator_.size());
			}
		}
	}

	/**
	 * Updates the distributions contained within using the counts.
	 * 
	 * @param elites
	 *            The sorted policy values.
	 * @param alpha
	 *            The original, full step size update parameter.
	 * @param population
	 *            The current population value.
	 * @param numElites
	 *            The minimum number of elites.
	 * 
	 */
	public void updateDistributions(SortedSet<PolicyValue> elites,
			double alpha, int population, int numElites) {
		// Keep count of the rules seen (and slots used)
		ElitesData ed = countRules(elites);

		klDivergence_ = 0;

		// Update the rule distributions and slot activation probability
		boolean updated = false;
		for (Slot slot : slotGenerator_) {
			// Update the slot
			double result = slot.updateProbabilities(ed, alpha, population,
					numElites);
			updated |= (result != alpha); 
			klDivergence_ += result;
		}
		
		if (updated)
			convergedValue_ = alpha * ProgramArgument.BETA.doubleValue();
	}

	/**
	 * Updates the distributions negatively by decreasing the probability of
	 * policies that didn't make it into the elites this iteration (updating
	 * each bad sample with a dynamically scaled alpha using count of 0).
	 * Removed samples use the difference between elite values as a scalar.
	 * 
	 * @param elites
	 *            The elite values to positively update.
	 * @param alpha
	 *            The stepwise parameter. Will be modified for the negative
	 *            samples.
	 * @param population
	 *            The size of the population.
	 * @param numElites
	 *            The minimum number of elite samples.
	 * @param removed
	 *            The samples this iteration that did not make the elite
	 *            samples.
	 * @param totalPoliciesEvaluated
	 *            The number of policies evaluated.
	 */
	public void updateNegative(SortedSet<PolicyValue> elites, double alpha,
			int population, int numElites, SortedSet<PolicyValue> removed,
			int totalPoliciesEvaluated) {
		if (removed == null || removed.isEmpty())
			return;

		System.err.println("ERROR: Not properly implemented yet!");
		// 'Negative' updates
		float bestVal = elites.first().getValue();
		float gamma = elites.last().getValue();
		double denominator = bestVal - gamma;
		for (PolicyValue negVal : removed) {
			// Calculate the dynamic alpha value based on how bad a sample this
			// was.
			double negModifier = (denominator == 0) ? 1 : Math.min(
					(gamma - negVal.getValue()) / denominator, 1);

			// Negative update each rule
			for (RelationalRule gr : negVal.getPolicy().getFiringRules()) {
				Slot slot = gr.getSlot();
				// Calculate the update appropriation based on how influential
				// the rule is within the slot.
				double ruleProb = slot.getGenerator().getProb(gr);
				double ruleRatio = ruleProb * slot.size();
				ruleRatio /= (ruleRatio + 1);
				double negAlpha = negModifier
						* slot.getLocalAlpha(alpha, population, numElites)
						/ numElites;

				slot.updateSlotValues(slot.getOrdering(), 0, negAlpha
						* (1 - ruleRatio));
				slot.getGenerator().updateElement(gr, 1, 0,
						negAlpha * ruleRatio);
				slot.getGenerator().normaliseProbs();
			}
		}
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
	 * Loads a serialised policy generator.
	 * 
	 * @param serializedFile
	 *            The serialised generator file to load.
	 * @param run
	 *            The run value.
	 * @return The instantiated Policy Generator of the file.
	 */
	public static PolicyGenerator loadPolicyGenerator(File serializedFile) {
		ObjectInputStream ois = null;
		try {
			FileInputStream fis = new FileInputStream(serializedFile);
			ois = new ObjectInputStream(fis);
			instance_ = (PolicyGenerator) ois.readObject();
			AgentObservations.loadAgentObservations();
			ois.close();
			return instance_;
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Loads a serialised policy generator and sets it as a module generator.
	 * 
	 * @param modFile
	 *            The serialised policy generator module file.
	 * @param goalCondition
	 *            The goal condition to set, if necessary.
	 * @return The loaded policy generator, or null if it doesn't exist.
	 */
	public static PolicyGenerator loadPolicyGenerator(File modFile,
			GoalCondition goalCondition) {
		PolicyGenerator modPG = loadPolicyGenerator(modFile);
		if (modPG != null) {
			modPG.moduleGenerator_ = true;
			goalCondition.normaliseArgs();
			modPG.moduleGoal_ = goalCondition;
		}
		instance_ = modPG;
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
		instance_.localGoal_ = StateSpec.getInstance().getGoalName();
		instance_.moduleGenerator_ = false;

		AgentObservations.loadAgentObservations();
		return instance_;
	}

	/**
	 * Initialises the instance as a new policy generator which uses information
	 * from the old generator.
	 * 
	 * @param policyGenerator
	 *            The policy generator containing the old LGG rules.
	 * @param goalCondition
	 *            The modular goal being worked towards.
	 * @return The new PolicyGenerator.
	 */
	public static PolicyGenerator newInstance(PolicyGenerator policyGenerator,
			GoalCondition goalCondition) {
		instance_ = new PolicyGenerator();

		// Set up the new local goal.
		instance_.localGoal_ = goalCondition.toString();
		AgentObservations.loadAgentObservations();
		instance_.moduleGenerator_ = true;
		goalCondition.normaliseArgs();
		instance_.moduleGoal_ = goalCondition;

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
	}
}