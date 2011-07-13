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
	private static final long serialVersionUID = -2424334194647477333L;

	/**
	 * If the summed total update value is only at this percentage of the
	 * update, the distribution is converged.
	 */
	private static final double CONVERGED_EPSILON = 0.01;

	/**
	 * The coefficient towards extra influence the rules gain when lowly tested.
	 */
	private static final double INFLUENCE_BOOST = 1.0;

	/**
	 * The threshold coefficient relative to distribution size at which rules
	 * have extra influence.
	 */
	private static final double INFLUENCE_THRESHOLD = 1.0;

	/** The instance. */
	private static PolicyGenerator instance_;

	/** The minimum value for weight updating. */
	private static final double MIN_UPDATE = 0.1;

	/**
	 * The converged must remain converged for X updates before considered
	 * converged.
	 */
	private static final int NUM_COVERGED_UPDATES = 10;

	/**
	 * The increment for an ordering value to increase when resolving ordering
	 * value clashes.
	 */
	private static final double ORDER_CLASH_INCREMENT = 0.001;

	/** If we're running the experiment in debug mode. */
	public static boolean debugMode_ = false;

	/** The element delimiter between elements in the generator files. */
	public static final String ELEMENT_DELIMITER = ",";

	/**
	 * The number of numerical splits when specialising (disregarding special
	 * negative case.
	 */
	public static final int NUM_NUMERICAL_SPLITS = 3;

	/** The random number generator. */
	public static Random random_ = new Random(0);

	/**
	 * The resampling bound. If an agent completes that many * the average
	 * episode length without changing action or state, resample policy.
	 */
	public static final double RESAMPLE_POLICY_BOUND = 0.1;

	/** The name of a module-saved policy generator serialised file. */
	public static final String SERIALISED_FILENAME = "policyGenerator.ser";

	/** The actions set the generator was initialised with. */
	private Map<String, RelationalPredicate> actionSet_;

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
	private MultiMap<String, RelationalRule> rlggRules_;

	/** The probability distributions defining the policy generator. */
	private SelectableSet<Slot> slotGenerator_;

	/** The rule creation object. */
	protected RuleCreation ruleCreation_;

	/** If modules are being used. */
	public boolean useModules_ = true;

	/** If we're using weighted elite samples. */
	public boolean weightedElites_ = false;

	/**
	 * The constructor for creating a new Policy Generator.
	 */
	public PolicyGenerator() {
		actionSet_ = StateSpec.getInstance().getActions();
		rlggRules_ = MultiMap.createListMultiMap();
		ruleCreation_ = new RuleCreation();
		mutatedRules_ = MultiMap.createListMultiMap();
		removedRules_ = new HashSet<RelationalRule>();
		currentRules_ = new HashSet<RelationalRule>();
		klDivergence_ = Double.MAX_VALUE;

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
		if (weightedElites_) {
			double diffValues = (elites.first().getValue() - elites.last()
					.getValue());
			if (diffValues != 0)
				gradient = (1 - MIN_UPDATE) / diffValues;
			offset = 1 - gradient * elites.first().getValue();
		}

		// Only selecting the top elite samples
		MultiMap<Slot, Double> slotNumeracy = MultiMap.createListMultiMap();
		for (PolicyValue pv : elites) {
			Map<Slot, Integer> policySlotCounts = new HashMap<Slot, Integer>();
			double weight = pv.getValue() * gradient + offset;
			RelationalPolicy eliteSolution = pv.getPolicy();

			// Count the occurrences of rules and slots in the policy
			Collection<RelationalRule> firingRules = eliteSolution.getFiringRules();
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
	private void mutateRule(RelationalRule baseRule, Slot ruleSlot, int mutationUses) {
		int preGoalHash = AgentObservations.getInstance().getObservationHash();
		// If the rule can mutate.
		if (ruleCanMutate(baseRule, mutationUses, preGoalHash)) {
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
			if (ruleSlot.getSlotSplitFacts() != null)
				mutants.addAll(ruleCreation_.specialiseRule(baseRule));
			baseRule.setSpawned(preGoalHash);

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
			ProbabilityDistribution<RelationalRule> distribution = rule.getSlot()
					.getGenerator();
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
	private void splitSlots() {
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

				SortedSet<RelationalPredicate> rlggConditions = rlgg
						.getConditions(false);
				Slot rlggSlot = rlgg.getSlot();
				// Split the slots and add them.
				Set<RelationalRule> splitSeeds = ruleCreation_.specialiseRule(rlgg);
				currentRules_.addAll(splitSeeds);
				for (RelationalRule seedRule : splitSeeds) {
					// The seed rule becomes parent-less and a non-mutant
					seedRule.removeMutation();

					SortedSet<RelationalPredicate> slotConditions = seedRule
							.getConditions(true);
					slotConditions.removeAll(rlggConditions);

					Slot splitSlot = new Slot(rlggSlot.getAction(),
							slotConditions);
					splitSlot.addNewRule(seedRule);

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
	 * Adds LGG rules to this generator.
	 * 
	 * @param rlggRules
	 *            The lgg rules (found previously).
	 */
	public void addCoveredRules(MultiMap<String, RelationalRule> rlggRules) {
		rlggRules_ = MultiMap.createListMultiMap();
		for (String action : rlggRules.keySet()) {
			// Adding the lgg rules to the slots
			Slot slot = findSlot(action);
			for (RelationalRule rule : rlggRules.get(action)) {
				rule = (RelationalRule) rule.clone();
				rule.setSpawned(null);
				slot.addNewRule(rule);
				rlggRules_.put(action, rule);
			}
		}
		splitSlots();
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
				int slotUses = slot.numSlotUses(random_);
				for (int i = 0; i < slotUses; i++) {
					double slotOrderVal = slotOrdering + random_.nextGaussian()
							* slotOrderSD;
					// Ensure the slot is placed in a unique order - no clashes.
					while (policyOrdering.containsKey(slotOrderVal))
						slotOrderVal += ORDER_CLASH_INCREMENT;

					// Sample a rule and add it
					RelationalRule gr = null;
					if (influenceUntestedRules) {
						gr = slot.sample(INFLUENCE_THRESHOLD, INFLUENCE_BOOST);
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

	public Collection<Slot> getGenerator() {
		return slotGenerator_;
	}

	public double getKLDivergence() {
		return klDivergence_;
	}

	public GoalCondition getModuleGoal() {
		return moduleGoal_;
	}

	public Collection<RelationalRule> getRLGGRules() {
		return rlggRules_.values();
	}

	public String getLocalGoal() {
		return localGoal_;
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
	 */
	public void postUpdateOperations(int mutationUses, double pruneConst) {
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
	 * Resets the generator to where it started.
	 */
	public void resetGenerator() {
		slotGenerator_ = new SelectableSet<Slot>();
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
		for (int i = 0; i < LearningController.TEST_ITERATIONS; i++) {
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
						goalReplacements, rlggRules_);
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
					Slot slot = findSlot(action.getFactName());

					// Adding the rule to the slot
					slot.addNewRule(coveredRule);
				}

				// Add to the covered rules
				rlggRules_.putContains(action.getFactName(), coveredRule);
			}

			// Split the slots
			splitSlots();

			return covered;
		}
		return null;
	}

	/**
	 * Updates the distributions contained within using the counts.
	 * 
	 * @param elites
	 *            The sorted policy values.
	 * @param stepSize
	 *            The step size update parameter.
	 */
	public void updateDistributions(SortedSet<PolicyValue> elites,
			double stepSize) {
		// Keep count of the rules seen (and slots used)
		ElitesData ed = countRules(elites);

		klDivergence_ = 0;

		// Update the rule distributions and slot activation probability
		for (Slot slot : slotGenerator_) {
			// Slot values
			Double ordering = ed.getSlotPosition(slot);
			double mean = ed.getSlotNumeracyMean(slot);
			double sd = ed.getSlotNumeracySD(slot);
			slot.updateValues(ordering, mean, sd, stepSize);

			// double numeracyBalancedStepSize = stepSize * mean;
			if (!slot.isFixed())
				klDivergence_ += slot.getGenerator().updateDistribution(
						ed.getSlotCount(slot), ed.getRuleCounts(), stepSize);
			// stepSizes.put(slot, numeracyBalancedStepSize);
		}

		convergedValue_ = stepSize * CONVERGED_EPSILON;
	}

	/**
	 * Updates the distributions contained within using the counts as well as
	 * 'negatively' updating (updating each bad sample with a dynamically scaled
	 * alpha using count of 0) any removed samples using the min reward to scale
	 * alpha.
	 * 
	 * @param elites
	 *            The elite values to positively update.
	 * @param stepSize
	 *            The stepwise parameter. Will be modified for the negative
	 *            samples.
	 * @param removed
	 *            The samples this iteration that did not make the elite
	 *            samples.
	 * @param minReward
	 *            The minimum possible reward observed.
	 */
	public void updateDistributionsWithNegative(SortedSet<PolicyValue> elites,
			double stepSize, SortedSet<PolicyValue> removed, double minReward) {
		convergedValue_ = stepSize * CONVERGED_EPSILON;

		// Keep count of the rules seen (and slots used)
		ElitesData ed = countRules(elites);

		klDivergence_ = 0;

		// Update the rule distributions and slot activation probability
		for (Slot slot : slotGenerator_) {
			// Slot values
			Double ordering = ed.getSlotPosition(slot);
			double mean = ed.getSlotNumeracyMean(slot);
			double sd = ed.getSlotNumeracySD(slot);
			slot.updateValues(ordering, mean, sd, stepSize);

			// double numeracyBalancedStepSize = stepSize * mean;
			if (!slot.isFixed())
				klDivergence_ += slot.getGenerator().updateDistribution(
						ed.getSlotCount(slot), ed.getRuleCounts(), stepSize);
			// stepSizes.put(slot, numeracyBalancedStepSize);
		}

		if (removed == null || removed.isEmpty())
			return;

		// 'Negative' updates
		float gamma = elites.last().getValue();
		double denominator = gamma - minReward;
		for (PolicyValue negVal : removed) {
			// Calculate the dynamic alpha value based on how bad a sample this
			// was.
			double negAlpha = stepSize * Math.abs(gamma - negVal.getValue())
					/ denominator;

			// Negative update each rule
			for (RelationalRule gr : negVal.getPolicy().getFiringRules()) {
				Slot slot = gr.getSlot();
				// Calculate the update appropriation based on how influential
				// the rule is within the slot.
				double ruleProb = slot.getGenerator().getProb(gr);
				double ruleRatio = ruleProb * slot.size();
				ruleRatio /= (ruleRatio + 1);

				slot.updateValues(slot.getOrdering(), 0, slot.getSelectionSD(),
						negAlpha * (1 - ruleRatio));
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