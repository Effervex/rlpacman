package cerrla;

import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import cerrla.modular.GoalCondition;
import cerrla.modular.ModularPolicy;
import cerrla.modular.ModularSubGoal;
import cerrla.modular.PolicyItem;

import relationalFramework.agentObservations.LocalAgentObservations.RuleMutation;
import rrlFramework.RRLExperiment;
import util.MultiMap;
import util.Pair;
import util.ProbabilityDistribution;
import util.Recursive;
import util.SelectableSet;
import util.SlotOrderComparator;

/**
 * A class for the generation of policies. The number of slots and the rules
 * within the slots can dynamically change.
 * 
 * @author Samuel J. Sarjant
 */
public final class PolicyGenerator implements Serializable {
	/** The point at which low mean slots are ignored for text output. */
	private static final double LOW_SLOT_THRESHOLD = 0.001;

	/**
	 * The increment for an ordering value to increase when resolving ordering
	 * value clashes.
	 */
	private static final double ORDER_CLASH_INCREMENT = 0.001;

	private static final long serialVersionUID = 3157117448981353095L;

	/**
	 * The multiplier to test 95% of the rules at least once in the largest
	 * slot.
	 */
	public static final double CONFIDENCE_INTERVAL = 6.0;

	/** The element delimiter between elements in the generator files. */
	public static final String ELEMENT_DELIMITER = ",";

	/** The value of convergence if the algorithm hasn't updated yet. */
	public static final double NO_UPDATES_CONVERGENCE = -1;

	/** The name of a module-saved policy generator serialised file. */
	public static final String SERIALISED_FILENAME = "policyGenerator.ser";

	/** Policies awaiting testing. */
	private Queue<ModularPolicy> awaitingTest_;

	/** The number of slots which have both a fixed rule and a converged mean. */
	private transient int convergedSlots_;

	/**
	 * The maximum amount of change between the slots before it is considered
	 * converged.
	 */
	private transient double convergedValue_ = NO_UPDATES_CONVERGENCE;

	/**
	 * The collection of currently active rules, so no duplicate rules are
	 * created.
	 */
	private SelectableSet<RelationalRule> currentRules_;

	/** The current slots used. */
	private MultiMap<String, Slot> currentSlots_;

	/** If the generator is currently frozen. */
	private transient boolean frozen_ = false;

	/** The trace of the slot splits. */
	private SortedMap<Double, RelationalRule> mutationTree_;

	/** The learning distribution this is contained by. */
	private LocalCrossEntropyDistribution parentLearner_;

	/** Total policies evaluated. */
	private int policiesEvaluated_;

	/** The set of rlgg rules, some which may be LGG, others not. */
	private Map<String, RelationalRule> rlggRules_;

	/** The probability distributions defining the policy generator. */
	private SelectableSet<Slot> slotGenerator_;

	/**
	 * The constructor for creating a new Policy Generator.
	 * 
	 * @param parentLearner
	 *            The parent learner controlling how this is used.
	 */
	public PolicyGenerator(LocalCrossEntropyDistribution parentLearner) {
		rlggRules_ = new HashMap<String, RelationalRule>();
		currentRules_ = new SelectableSet<RelationalRule>();
		currentSlots_ = MultiMap.createListMultiMap();
		policiesEvaluated_ = 0;
		mutationTree_ = new TreeMap<Double, RelationalRule>();
		parentLearner_ = parentLearner;

		resetGenerator();
	}

	/**
	 * Simple method that just sets the minimum number of elites to the bounded
	 * value if using bounds.
	 * 
	 * @param numElites
	 *            The current number of elites.
	 * @return The changed number of elites (if necessary).
	 */
	private int checkPopulationBounding(int population) {
		if (ProgramArgument.BOUNDED_ELITES.booleanValue()) {
			int generatorSize = slotGenerator_.size() - getNumConvergedSlots();
			population = Math.max(
					(int) (generatorSize / ProgramArgument.RHO.doubleValue()),
					population);
		}
		return population;
	}

	/**
	 * Counts the rules from the elite samples and stores their frequencies and
	 * total score.
	 * 
	 * @param elites
	 *            The elite samples to iterate through.
	 * @param ed
	 *            The elites data to record into.
	 * @param minValue
	 *            The minimum value the agent has observed so far.
	 * @return The number of elites samples used for the count (max |elites|).
	 */
	private int countRules(SortedSet<PolicyValue> elites, ElitesData ed,
			double minValue) {
		// If no samples better than others, return null
		if (elites == null || elites.isEmpty()
				|| elites.first().getValue() == minValue)
			return 0;

		// Sub-elites remove any minimally valued samples (useless updates).
		SortedSet<PolicyValue> subElites = (elites.last().getValue() == minValue) ? elites
				.headSet(new PolicyValue(null, minValue, Integer.MAX_VALUE))
				: elites;

		// WEIGHTED UPDATES CODE
		double gradient = 0;
		double offset = 1;
		if (ProgramArgument.WEIGHTED_UPDATES.booleanValue()) {
			double diffValues = (subElites.first().getValue() - subElites
					.last().getValue());
			if (diffValues != 0)
				gradient = (1 - ProgramArgument.MIN_WEIGHTED_UPDATE
						.doubleValue()) / diffValues;
			offset = 1 - gradient * subElites.first().getValue();
		}
		// /////////////////////


		// Only selecting the top elite samples
		Map<Slot, Double> slotMean = new HashMap<Slot, Double>();
		for (PolicyValue pv : subElites) {
			double weight = pv.getValue() * gradient + offset;
			ModularPolicy eliteSolution = pv.getPolicy();

			// Count the occurrences of rules and slots in the policy
			Collection<Slot> meanNotedSlots = new HashSet<Slot>();
			recurseCountPolicyRules(eliteSolution, weight, meanNotedSlots, ed,
					slotMean, null);
		}


		// Calculate the Bernoulli probabilities of each slot
		for (Slot slot : slotMean.keySet()) {
			// Each slot should have elites number of counts
			double meanVal = slotMean.get(slot) / subElites.size();
			ed.setUsageStats(slot, meanVal);
		}

		return subElites.size();
	}

	/**
	 * Recursively counts the firing rules in the Modular Policy.
	 * 
	 * @param policy
	 *            The current modular policy being looked at.
	 * @param weight
	 *            The weighted count (or 1, if not weighting).
	 * @param meanNotedSlots
	 *            The slots that have already been noted in this policy so far.
	 * @param ed
	 *            The ElitesData to add to.
	 * @param slotMean
	 *            The slot mean to add to.
	 * @param orderedFiredSlots
	 *            An ordered list of slots that fired.
	 */
	@Recursive
	private void recurseCountPolicyRules(ModularPolicy policy, double weight,
			Collection<Slot> meanNotedSlots, ElitesData ed,
			Map<Slot, Double> slotMean, List<Slot> orderedFiredSlots) {
		Set<RelationalRule> firingRules = policy.getFiringRules();
		boolean mainPolicy = false;
		if (orderedFiredSlots == null) {
			mainPolicy = true;
			orderedFiredSlots = new ArrayList<Slot>();
		}

		// Recurse through the rule/policies, noting fired rules/policies.
		for (PolicyItem reo : policy.getRules()) {
			// If reo is another policy, get/create the corresponding rule
			// in this generator.
			if (reo instanceof ModularSubGoal) {
				// TODO Not counting modular policies unless the module is
				// converged
				ModularPolicy modPol = ((ModularSubGoal) reo)
						.getModularPolicy();
				// Adding modular rules to the main policy only if the sub goal
				// is converged.
				// if (modPol.getLocalCEDistribution().isConverged())
				// recurseCountPolicyRules(modPol, weight, meanNotedSlots, ed,
				// slotMean, orderedFiredSlots);
			} else if (firingRules.contains(reo)) {
				RelationalRule rule = ((RelationalRule) reo);
				// May have to get/create new rule
				// TODO Unsure about this...
				// if (!mainPolicy)
				// rule = getCreateCorrespondingRule(rule,
				// policy.getModularReplacementMap());

				Slot ruleSlot = rule.getSlot();
				// Slot counts
				ed.addSlotCount(ruleSlot, weight);

				// Noting slot order
				orderedFiredSlots.add(ruleSlot);

				// Rule counts
				ed.addRuleCount(rule, weight);

				// Note which slots were active in this policy
				if (!meanNotedSlots.contains(ruleSlot)) {
					Double val = slotMean.get(ruleSlot);
					if (val == null)
						val = 0.0;
					slotMean.put(ruleSlot, val + 1);
					meanNotedSlots.add(ruleSlot);
				}
			}
		}

		// Note the order of the policies.
		if (mainPolicy) {
			int index = 0;
			for (Slot orderedSlot : orderedFiredSlots) {
				// Slot ordering
				double order = (orderedFiredSlots.size() == 1) ? 0.5 : 1.0
						* index / (orderedFiredSlots.size() - 1);
				ed.addSlotOrdering(orderedSlot, order);
				index++;
			}
		}
	}

	/**
	 * Creates a slot seeded with a given rule and adds it to the slot
	 * distribution.
	 * 
	 * @param rule
	 *            The rule to seed the slot with.
	 */
	private void createSeededSlot(RelationalRule rule) {
		currentRules_.add(rule);
		// Create a new slot with that rule in it (Min level 2)
		Slot newSlot = new Slot(rule, false, 0, this);
		mutateRule(rule, newSlot, -1);
		slotGenerator_.add(newSlot);
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
		int observationHash = parentLearner_.getLocalAgentObservations()
				.getObservationHash();
		if (ruleCanMutate(baseRule, mutationUses, observationHash)) {
			// If the rule can mutate.
			boolean needToPause = false;
			// Remove old rules if this rule is an LGG rule.
			boolean removeOld = (baseRule.isMutant()) ? false : true;
			if (RRLExperiment.debugMode_) {
				try {
					System.out.println(" [" + getGoalCondition()
							+ "] MUTATING " + baseRule);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// Create the mutants.
			RuleMutation ruleMutation = parentLearner_
					.getLocalAgentObservations().getRuleMutation();
			Set<RelationalRule> mutants = ruleMutation
					.specialiseRuleMinor(baseRule);
			if (ProgramArgument.DYNAMIC_SLOTS.booleanValue()
					|| !rlggRules_.containsValue(ruleSlot.getSeedRule()))
				mutants.addAll(ruleMutation.specialiseRule(baseRule));
			baseRule.setSpawned(observationHash);

			// Ensure no duplicate rules.
			// mutants.removeAll(currentRules_);

			// If the slot has settled, remove any mutants not present in
			// the permanent mutant set
			Collection<RelationalRule> previousChildren = baseRule
					.getChildrenRules();
			if (removeOld && previousChildren != null) {
				Collection<RelationalRule> removables = new ArrayList<RelationalRule>(
						previousChildren);
				removables.removeAll(mutants);

				// Remove any parent-child links from the rule first
				for (Iterator<RelationalRule> iter = removables.iterator(); iter
						.hasNext();) {
					RelationalRule rr = iter.next();
					needToPause = true;
					rr.removeParent(baseRule);
					if (!rr.isWithoutParents())
						iter.remove();

					if (RRLExperiment.debugMode_) {
						System.out.println(" [" + getGoalCondition()
								+ "] REMOVING MUTANT: " + rr);
					}
				}

				// Setting the restart value.
				if (!removables.isEmpty()) {
					baseRule.getSlot().resetPolicyCount();
				}

				if (ruleSlot.getGenerator().removeAll(removables)) {
					currentRules_.removeAll(removables);
					ruleSlot.getGenerator().normaliseProbs();
				}
			}

			// Add all mutants to the ruleSlot
			for (RelationalRule rr : mutants) {
				// Only add if not already in there
				ruleSlot.addNewRule(rr);
				currentRules_.add(rr);

				if (RRLExperiment.debugMode_) {
					if (previousChildren != null
							&& previousChildren.contains(rr))
						System.out.println(" [" + getGoalCondition()
								+ "] EXISTING MUTANT: " + rr);
					else {
						needToPause = true;
						System.out.println(" [" + getGoalCondition()
								+ "] ADDED MUTANT: " + rr);
					}
				}
			}
			baseRule.setChildren(mutants);
			noteMutationTree(baseRule, parentLearner_.getCurrentEpisode());

			// Debug text input
			if (RRLExperiment.debugMode_ && needToPause) {
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
	 * Note down the mutation in the mutation tree.
	 * 
	 * @param baseRule
	 *            The rule being mutated.
	 * @param currentEpisode
	 *            The current episode.
	 */
	private void noteMutationTree(RelationalRule baseRule, int currentEpisode) {
		// Modify the episode value by a small amount if two or more rules
		// mutate on the same episode.
		Double episodeF = Double.valueOf(currentEpisode);
		while (mutationTree_.containsKey(episodeF))
			episodeF += ORDER_CLASH_INCREMENT;
		mutationTree_.put(episodeF, baseRule);
	}

	/**
	 * Recurse through a multimap tree.
	 * 
	 * @param rule
	 *            The current rule.
	 * @param mutationTree
	 *            The mutation tree to traverse
	 * @param buf
	 *            The writer to write out to.
	 */
	private void recurseMutationTree(
			Pair<RelationalRule, Integer> rule,
			MultiMap<RelationalRule, Pair<RelationalRule, Integer>> mutationTree,
			BufferedWriter buf) throws Exception {
		for (int i = rule.objA_.getAncestryCount() - 1; i >= 0; i--) {
			if (i == 0)
				buf.append("|-");
			else
				buf.append(" ");
		}
		buf.append(rule.objA_.toNiceString() + ":" + rule.objB_ + "\n");

		// Recurse through the rules
		Collection<Pair<RelationalRule, Integer>> children = mutationTree
				.get(rule.objA_);
		if (children != null)
			for (Pair<RelationalRule, Integer> childRule : mutationTree
					.get(rule.objA_))
				recurseMutationTree(childRule, mutationTree, buf);
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

		// If using dynamic slots, that's all we need to check.
		if (ProgramArgument.DYNAMIC_SLOTS.booleanValue())
			return true;

		// (Only check for postUpdate mutations - not split slot mutations)
		// If the rule's slot isn't already full (using klSize() to simulate
		// pruning)
		int maximumSlotCapacity = parentLearner_.getLocalAgentObservations()
				.getNumSpecialisations(rule.getActionPredicate());
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
		boolean changed = false;
		for (RelationalRule rlgg : rlggRules_.values()) {
			// Only re-split the slots if the observation hash has changed.
			if (ruleCanMutate(rlgg, -1, parentLearner_
					.getLocalAgentObservations().getObservationHash())) {
				if (RRLExperiment.debugMode_) {
					try {
						System.out.println(" [" + getGoalCondition()
								+ "] SPLITTING SLOT "
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
				Set<RelationalRule> splitSeeds = parentLearner_
						.getLocalAgentObservations().getRuleMutation()
						.specialiseRule(rlgg);
				currentRules_.addAll(splitSeeds);
				for (RelationalRule seedRule : splitSeeds) {
					// The seed rule becomes parent-less and a non-mutant
					seedRule.removeMutation();

					Slot splitSlot = new Slot(seedRule, false, 1, this);

					// Check if the slot already exists
					Slot existingSlot = slotGenerator_.findMatch(splitSlot);
					if (existingSlot != null) {
						if (existingSlot.getSeedRule().equals(seedRule)) {
							splitSlot = existingSlot;
							seedRule = existingSlot.getSeedRule();
						} else {
							slotGenerator_.remove(existingSlot);
						}

						if (RRLExperiment.debugMode_) {
							try {
								System.out.println(" [" + getGoalCondition()
										+ "] EXISTING SLOT SEED: "
										+ splitSlot.getSlotSplitFacts()
										+ " => " + rlgg.getActionPredicate());
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} else if (RRLExperiment.debugMode_) {
						try {
							System.out.println(" [" + getGoalCondition()
									+ "] NEW SLOT SEED: "
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
	 * Determines the population (N) of rules to use for optimisation.
	 * 
	 * @param policyGenerator
	 *            The policy generator to determine the populations from.
	 * @return A population of rules, large enough to reasonably test most
	 *         combinations of rules.
	 */
	public int determinePopulation() {
		// N_E = Max(average # rules in high mu(S) slots, Sum mu(S))
		double maxWeightedRuleCount = 0;
		double maxAbsWeightedRuleCount = 0;
		double maxSlotMean = 0;
		double sumKLWeightedRuleCount = 0;
		double sumSlotMean = 0;
		for (Slot slot : slotGenerator_) {
			// If using slot fixing, don't count fixed slots.
			if (!ProgramArgument.SLOT_FIXING.booleanValue() || !slot.isFixed()) {
				double weight = slot.getSelectionProbability();
				if (weight > 1)
					weight = 1;
				if (weight > maxSlotMean)
					maxSlotMean = weight;
				sumSlotMean += weight;
				// Use klSize to determine the skew of the slot size
				double klSize = slot.klSize();
				double weightedRuleCount = klSize * weight;
				sumKLWeightedRuleCount += weightedRuleCount;
				if (weightedRuleCount > maxWeightedRuleCount)
					maxWeightedRuleCount = weightedRuleCount;

				double absWeightedRuleCount = slot.size() * weight;
				if (absWeightedRuleCount > maxAbsWeightedRuleCount)
					maxAbsWeightedRuleCount = absWeightedRuleCount;
			}
		}

		double population = 1;
		double rho = ProgramArgument.RHO.doubleValue();
		switch (ProgramArgument.ELITES_FUNCTION.intValue()) {
		case ProgramArgument.ELITES_SIZE_SUM_SLOTS:
			// Elites is equal to the sum of the slot means
			population = sumSlotMean / rho;
			break;
		case ProgramArgument.ELITES_SIZE_SUM_RULES:
			// Elites is equal to the total number of rules (KL sized)
			population = Math.max(sumKLWeightedRuleCount, sumSlotMean) / rho;
			break;
		case ProgramArgument.ELITES_SIZE_MAX_RULES:
			// Elites is equal to the (weighted) maximum slot size
			population = Math.max(maxWeightedRuleCount, sumSlotMean) / rho;
			break;
		case ProgramArgument.ELITES_SIZE_MAX_RULE_NUM_SLOTS:
			// Population is equal to the maximum (weighted) slot size * the
			// number of slots * 3
			population = CONFIDENCE_INTERVAL * maxWeightedRuleCount
					* sumSlotMean;
			break;
		case ProgramArgument.ELITES_SIZE_AV_RULES:
		default:
			// Elites is equal to the average number of rules in high mean
			// slots.
			population = Math.max(sumKLWeightedRuleCount / sumSlotMean,
					sumSlotMean) / rho;
		}

		population *= ProgramArgument.ELITES_MULTIPLE.doubleValue();

		// values_[MAX_KL][iter_] = maxWeightedRuleCount;
		// values_[SUM_KL][iter_] = sumWeightedRuleCount;
		// values_[SUM_MU][iter_] = sumSlotMean;
		// values_[SIZE_D_S][iter_] = slotGenerator_.size();
		// iter_++;
		// if (iter_ >= REPS) {
		// try {
		// File file = new File("distributionDebug.csv");
		// FileWriter fw = new FileWriter(file);
		// BufferedWriter bw = new BufferedWriter(fw);
		// bw.write("MaxKL,SumKL,SumMu,SizeDS,\n");
		// for (int i = 0; i < REPS; i++) {
		// for (int j = 0; j <= SIZE_D_S; j++)
		// bw.write(values_[j][i] + ",");
		// bw.write("\n");
		// }
		//
		// bw.close();
		// fw.close();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// System.exit(1);
		// }

		// Check elite bounding
		return checkPopulationBounding((int) Math.ceil(population));
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
		if (!frozen_ && !awaitingTest_.isEmpty())
			return awaitingTest_.poll();

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
				if (slot.useSlot(RRLExperiment.random_)) {
					double slotOrderVal = slotOrdering
							+ RRLExperiment.random_.nextGaussian()
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
			policy.addRule(rule);
		}

		return policy;
	}

	/**
	 * Gets the *rough* amount that this generator has converged (between 0 and
	 * 1).
	 * 
	 * @return The *rough* estimate of convergence.
	 */
	public double getConvergenceValue() {
		return convergedValue_;
	}

	/**
	 * Gets (if it exists) or creates a new rule and slot corresponding to a
	 * rule in the current generator. If the generator does not contain the
	 * rule, it is created in a slot of its own.
	 * 
	 * @param rule
	 *            The rule to get the corresponding rule for.
	 * @param paramReplacementMap
	 *            The replacement map for goal conditions.
	 * @return The relational rule from within this generator corresponding to
	 *         the rule.
	 */
	public RelationalRule getCreateCorrespondingRule(RelationalRule rule,
			Map<String, String> paramReplacementMap) {
		rule = rule.groundModular(paramReplacementMap);
		RelationalRule match = currentRules_.findMatch(rule);
		// If there is no match, or the matched rule isn't a seed rule, create a
		// new slot.
		if (match == null
				|| (ProgramArgument.SEED_MODULE_RULES.booleanValue() && !match
						.getSlot().getSeedRule().equals(match))) {
			createSeededSlot(rule);

			if (match != null) {
				ProbabilityDistribution<RelationalRule> ruleGenerator = match
						.getSlot().getGenerator();
				ruleGenerator.remove(match);
				ruleGenerator.normaliseProbs();
			}

			match = rule;
		}

		return match;
	}

	public Collection<Slot> getGenerator() {
		return slotGenerator_;
	}

	public Collection<RelationalRule> getMutatedRules() {
		return mutationTree_.values();
	}

	public int getNumConvergedSlots() {
		return convergedSlots_;
	}

	public int getNumMutations() {
		return mutationTree_.size();
	}

	public int getPoliciesEvaluated() {
		return policiesEvaluated_;
	}

	public Map<String, RelationalRule> getRLGGRules() {
		return rlggRules_;
	}

	/**
	 * If the distribution has been updated at all.
	 * 
	 * @return True if the distribution has updated (isn't uniform anymore).
	 */
	public boolean hasUpdated() {
		return convergedValue_ != NO_UPDATES_CONVERGENCE;
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
		if (slotGenerator_.isEmpty())
			return false;

		for (Slot slot : slotGenerator_) {
			if (!slot.isConverged())
				return false;
		}
		return true;
	}

	/**
	 * If a rule is within this policy generator.
	 * 
	 * @param rule
	 *            The rule being checked.
	 * @return True if the rule exists within this generator.
	 */
	public boolean isRuleExists(RelationalRule rule) {
		return currentRules_.contains(rule);
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
		if (RRLExperiment.debugMode_) {
			try {
				System.out.println(" [" + getGoalCondition()
						+ "] PERFORMING POST-UPDATE OPERATIONS");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Check if slots are ready for splitting //
		boolean mutated = false;
		convergedSlots_ = 0;

		// For each slot
		Collection<Slot> newSlots = new ArrayList<Slot>();
		for (Object oSlot : slotGenerator_.toArray()) {
			Slot slot = (Slot) oSlot;
			if (slot.isConverged())
				convergedSlots_++;

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
				int newSlotLevel = slot.getLevel() + 1;
				// Continue to split slot until its KL size has increased to a
				// non-splitting threshold.
				while (slot.isSplittable()) {
					ProbabilityDistribution<RelationalRule> ruleGenerator = slot
							.getGenerator();
					RelationalRule bestRule = ruleGenerator.getBestElement();
					// If the best rule isn't the seed, create a new slot
					if (!bestRule.equals(slot.getSeedRule())) {
						Slot newSlot = new Slot(bestRule, false, newSlotLevel,
								this);
						if (slotGenerator_.contains(newSlot)) {
							// If the slot already exists, update the level if
							// lower
							Slot existingSlot = slotGenerator_
									.findMatch(newSlot);
							existingSlot.updateLevel(newSlotLevel);
							// Move the rule's slot to the existing slot for
							// further update operations.
							bestRule.setSlot(existingSlot);
						} else {
							if (ProgramArgument.INHERIT_PARENT_SLOT_VALS
									.booleanValue()) {
								// Set ordering to be the same
								newSlot.setOrdering(slot.getOrdering());
								newSlot.setSelectionProb(slot
										.getSelectionProbability());
							}
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
		if (RRLExperiment.debugMode_) {
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
		currentRules_.clear();
		currentSlots_.clear();

		awaitingTest_ = new LinkedList<ModularPolicy>();
	}

	/**
	 * Retests a policy by adding it to a stack of policies for use later when a
	 * generated policy is requested.
	 * 
	 * @param policy
	 *            The policy to retest.
	 */
	public void retestPolicy(ModularPolicy policy) {
		awaitingTest_.add(new ModularPolicy(policy));
	}

	/**
	 * Saves the generators into a predefined stream.
	 * 
	 * @param buf
	 *            The predefined stream to write to.
	 */
	public void saveGenerators(BufferedWriter buf) throws Exception {
		SortedSet<Slot> orderSlots = new TreeSet<Slot>(
				SlotOrderComparator.getInstance());
		orderSlots.addAll(slotGenerator_);
		StringBuffer slotBuffer = new StringBuffer();
		Slot uselessSlot = null;
		for (Slot s : orderSlots) {
			if (s.getSelectionProbability() < LOW_SLOT_THRESHOLD) {
				uselessSlot = s;
				break;
			}
			slotBuffer.append(s.toString() + "\n");
		}
		if (uselessSlot != null) {
			SortedSet<Slot> unusedSlots = orderSlots.tailSet(uselessSlot);
			slotBuffer.append("+ " + unusedSlots.size() + " OTHER MU < "
					+ LOW_SLOT_THRESHOLD + " SLOTS: {");
			boolean first = true;
			for (Slot unused : unusedSlots) {
				if (!first)
					slotBuffer.append(", ");
				slotBuffer.append(unused.slotSplitToString());
				first = false;
			}
			slotBuffer.append("}\n");
		}

		// For each of the rule generators
		buf.write(slotBuffer.toString() + "\n");
		// Write the number of slots
		buf.write("Num slots: " + slotGenerator_.size() + "\n");
		DecimalFormat formatter = new DecimalFormat("#0.0000");
		buf.write("Estimated Convergence: "
				+ formatter.format(100 * convergedValue_) + "%");
	}

	/**
	 * Saves the generators to an existing stream.
	 * 
	 * @param buf
	 *            The stream to save the generators to.
	 * @param finalWrite
	 *            If this is the final output for the run.
	 */
	public void saveHumanGenerators(BufferedWriter buf, boolean finalWrite)
			throws IOException {
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

		ModularPolicy modPol = new ModularPolicy(bestPolicy, parentLearner_);
		modPol.setModularParameters(null);
		buf.write(bestPolicy.toString());

		if (finalWrite)
			System.out.println("Best policy:\n" + bestPolicy);
	}

	/**
	 * Saves a trace of the mutations to file.
	 * 
	 * @param buf
	 *            The buffer to save to.
	 */
	public void saveMutationTree(BufferedWriter buf) throws Exception {
		Map<String, MultiMap<RelationalRule, Pair<RelationalRule, Integer>>> actionRules = new HashMap<String, MultiMap<RelationalRule, Pair<RelationalRule, Integer>>>();
		MultiMap<String, Pair<RelationalRule, Integer>> parentlessRules = MultiMap
				.createSortedSetMultiMap(MutationEpisodeComparator
						.getInstance());
		// Write an episodic list of mutations
		buf.append("Per episode mutations\n");
		for (Double episode : mutationTree_.keySet()) {
			int episodeInt = episode.intValue();
			buf.append(episodeInt + ":");
			RelationalRule rule = mutationTree_.get(episode);
			for (int i = rule.getAncestryCount() - 1; i >= 0; i--) {
				if (i == 0)
					buf.append("|-");
				else
					buf.append(" ");
			}
			buf.append(rule.toNiceString() + "\n");

			// Noting the rules
			MultiMap<RelationalRule, Pair<RelationalRule, Integer>> rules = actionRules
					.get(rule.getActionPredicate());
			// Initialise the multimap
			if (rules == null) {
				rules = MultiMap
						.createSortedSetMultiMap(MutationEpisodeComparator
								.getInstance());
				actionRules.put(rule.getActionPredicate(), rules);
			}

			// Add the rule to the multimap, with the parent rule as the key
			Pair<RelationalRule, Integer> pair = new Pair<RelationalRule, Integer>(
					rule, episodeInt);
			if (!rule.isWithoutParents()) {
				rules.put(rule.getParentRules().iterator().next(), pair);
			} else {
				// If the rule has no parents, use it as a base rule
				parentlessRules.put(rule.getActionPredicate(), pair);
			}
		}
		buf.append("\n");

		// Output each action mutation tree
		for (String action : actionRules.keySet()) {
			buf.append("Per action (" + action + ")\n");
			for (Pair<RelationalRule, Integer> rule : parentlessRules
					.get(action)) {
				recurseMutationTree(rule, actionRules.get(action), buf);
			}
		}
	}

	/**
	 * Seed rules from a file as new slots.
	 * 
	 * @param ruleFile
	 */
	public void seedRules(File ruleFile) {
		try {
			FileReader fr = new FileReader(ruleFile);
			BufferedReader br = new BufferedReader(fr);

			String input = null;
			while ((input = br.readLine()) != null) {
				RelationalRule seedRule = new RelationalRule(input);
				SortedSet<RelationalPredicate> ruleConds = seedRule
						.getConditions(false);
				ruleConds = parentLearner_.getLocalAgentObservations()
						.simplifyRule(ruleConds, null, seedRule.getAction(),
								false);
				seedRule = new RelationalRule(ruleConds, seedRule.getAction(),
						null, null);
				createSeededSlot(seedRule);
			}

			br.close();
			fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setPoliciesEvaluated(int pe) {
		policiesEvaluated_ = pe;
	}

	public int size() {
		return slotGenerator_.size();
	}

	@Override
	public String toString() {
		return slotGenerator_.toString();
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
	 * @param minValue
	 *            The minimum value the agent has seen.
	 */
	public void updateDistributions(SortedSet<PolicyValue> elites,
			double alpha, int population, int numElites, double minValue) {
		// Keep count of the rules seen (and slots used)
		double maxUpdate = alpha * 3;
		ElitesData ed = new ElitesData();
		int numEliteSamples = countRules(elites, ed, minValue);
		double modAlpha = (1.0 * numEliteSamples) / numElites;
		if (modAlpha < 1) {
			if (ProgramArgument.EARLY_UPDATING.booleanValue())
				alpha *= modAlpha;
			else
				ed = null;
		}

		// Update the rule distributions and slot activation probability
		boolean updated = false;
		convergedValue_ = 0;
		for (Slot slot : slotGenerator_) {
			// Update the slot
			Double result = slot.updateProbabilities(ed, alpha, population,
					numElites, policiesEvaluated_);
			if (result != null) {
				updated = true;
				convergedValue_ += Math.min(
						result - ProgramArgument.BETA.doubleValue(), maxUpdate);
			} else {
				convergedValue_ += maxUpdate;
			}
		}

		if (!updated)
			convergedValue_ = NO_UPDATES_CONVERGENCE;
		else {
			// Normalise converged value between 0 and 1 (1 is fully converged)
			convergedValue_ = 1 - convergedValue_
					/ (slotGenerator_.size() * maxUpdate);
		}
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
	 */
	public void updateNegative(SortedSet<PolicyValue> elites, double alpha,
			int population, int numElites, SortedSet<PolicyValue> removed) {
		if (removed == null || removed.isEmpty())
			return;

		// 'Negative' updates
		double bestVal = elites.first().getValue();
		double gamma = elites.last().getValue();
		double denominator = bestVal - gamma;
		for (PolicyValue negVal : removed) {
			// Calculate the dynamic alpha value based on how bad a sample this
			// was.
			double negModifier = (denominator == 0) ? 1 : Math.min(
					(gamma - negVal.getValue()) / denominator, 1);

			// Negative update each rule
			for (RelationalRule gr : negVal.getPolicy().getFiringRules()) {
				Slot slot = gr.getSlot();
				if (slot != null) {
					double negAlpha = negModifier
							* slot.getLocalAlpha(alpha, population, numElites,
									policiesEvaluated_) / numElites;

					// If the slot is ready for updates.
					if (negAlpha != 0) {
						slot.getGenerator().updateElement(gr, 1, 0, negAlpha);
						slot.getGenerator().normaliseProbs();
					}
				}
			}
		}
	}

	/**
	 * Simply removes the RLGG rules from the current rules collection.
	 * 
	 * @return The removed RLGG rules.
	 */
	protected Collection<RelationalRule> removeRLGGRules() {
		Collection<RelationalRule> oldRLGGs = new HashSet<RelationalRule>(
				rlggRules_.values());
		currentRules_.removeAll(oldRLGGs);
		rlggRules_.clear();
		return oldRLGGs;
	}

	/**
	 * Adds a collection of RLGG rules to this generator.
	 * 
	 * @param rlggRules
	 *            The rlgg rules to add.
	 * @return A list of new covered rules which are only being added for the
	 *         first time.
	 */
	protected List<RelationalRule> addRLGGRules(
			Collection<RelationalRule> rlggRules) {
		currentRules_.addAll(rlggRules);
		List<RelationalRule> newlyCovered = new ArrayList<RelationalRule>();

		// Add remaining information to rules.
		for (RelationalRule coveredRule : rlggRules) {
			Slot rlggSlot = coveredRule.getSlot();
			coveredRule.incrementStatesCovered();
			if (coveredRule.isRecentlyModified()) {
				System.out.println(" [" + getGoalCondition()
						+ "] COVERED RULE: " + coveredRule);
				if (!ProgramArgument.DYNAMIC_SLOTS.booleanValue()) {
					if (rlggSlot != null)
						rlggSlot.resetPolicyCount();
				}
			}

			RelationalPredicate action = coveredRule.getAction();
			if (rlggSlot == null) {
				rlggSlot = new Slot(coveredRule, false, 0, this);
				slotGenerator_.add(rlggSlot);
				currentSlots_.clearValues(coveredRule.getActionPredicate());
				currentSlots_.put(coveredRule.getActionPredicate(), rlggSlot);

				newlyCovered.add(coveredRule);
			}

			// Add to the covered rules
			rlggRules_.put(action.getFactName(), coveredRule);
		}

		// If not using dynamic slots, split the slots now.
		if (!ProgramArgument.DYNAMIC_SLOTS.booleanValue()) {
			splitRLGGSlots();
		}

		// Re-mutate all slot seed rules (if observations have changed)
		for (Slot slot : slotGenerator_)
			mutateRule(slot.getSeedRule(), slot, -1);

		return newlyCovered;
	}

	public GoalCondition getGoalCondition() {
		return parentLearner_.getGoalCondition();
	}
}