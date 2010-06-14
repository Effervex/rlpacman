package relationalFramework;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

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

	/**
	 * The last amount of difference in distribution probabilities from the
	 * update.
	 */
	private double updateDifference_;

	/** If this policy generator is being used for learning a module. */
	private boolean moduleGenerator_;

	/** The goal this generator is working towards if modular. */
	private ArrayList<String> moduleGoal_;

	/** If modules are being used. */
	public boolean useModules_ = true;

	/**
	 * If this policy generator only updates the ordering of slots - no rule
	 * creation or modification.
	 */
	private boolean slotOptimisation_ = false;

	/**
	 * If the experiment should restart due to new rules being introduced
	 * mid-learning.
	 */
	private boolean restart_ = false;

	/** The random number generator. */
	public static Random random_ = new Random(1);

	/** If we're running the experiment in debug mode. */
	public static boolean debugMode_ = false;

	/** The element delimiter between elements in the generator files. */
	public static final String ELEMENT_DELIMITER = ",";

	/** The delimiter character between rules within the same rule base. */
	public static final String RULE_DELIMITER = "@";

	/**
	 * The maximum amount of change between the slots before it is considered
	 * converged.
	 */
	private static final double CONVERGED = 0.05;

	/**
	 * The constructor for creating a new Policy Generator.
	 */
	public PolicyGenerator() {
		actionSet_ = StateSpec.getInstance().getActions();
		coveredRules_ = new MultiMap<String, GuidedRule>();
		covering_ = new Covering(actionSet_.size());
		mutatedRules_ = new MultiMap<String, GuidedRule>();
		updateDifference_ = Double.MAX_VALUE;

		resetGenerator();
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
				policy.addRule(gr, true);
		}

		// Append the general rules as well (if they aren't already in there) to
		// ensure unnecessary covering isn't triggered.
		for (GuidedRule coveredRule : coveredRules_.values()) {
			if (!policy.contains(coveredRule))
				policy.addRule(coveredRule, false);
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
			// createNewRules = true;

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
	 * Mutates a rule (if it hasn't spawned already) and creates and adds
	 * children to the slots.
	 * 
	 * @param baseRule
	 *            The rule to mutate.
	 * @param removeOld
	 *            If the pre-goal has settled.
	 */
	private void mutateRule(GuidedRule baseRule, Slot ruleSlot,
			boolean removeOld, boolean settled) {
		// If the base rule hasn't already spawned pre-goal mutants
		if (!baseRule.hasSpawned()) {

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

			Collection<GuidedRule> mutants = covering_
					.specialiseToPreGoal(baseRule);

			String actionPred = baseRule.getActionPredicate();
			// If the slot has settled, remove any mutants not present in
			// the permanent mutant set
			if (removeOld) {
				if (settled)
					baseRule.setSpawned(true);

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
				// Only add if not already in there
				if (!ruleSlot.contains(gr)) {
					ruleSlot.addNewRule(gr, false);
				}

				if (debugMode_) {
					System.out.println("\tADDED/EXISTING MUTANT: " + gr);

				}

				mutatedRules_.putContains(ruleSlot.getAction(), gr);
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
		// All pre-goals must be settled (or null)
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

		coveredRules_.clear();
		mutatedRules_.clear();
		policyGenerator_.normaliseProbs();
	}

	/**
	 * If the slots and rules within are converged to stability.
	 * 
	 * @return True if the generator is converged, false otherwise.
	 */
	public boolean isConverged() {
		if (updateDifference_ < CONVERGED) {
			return true;
		}
		return false;
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
			Map<Slot, Double> slotCounts, Map<GuidedRule, Double> ruleCounts,
			double stepSize) {
		// Update the slot distribution
		updateDifference_ = policyGenerator_.updateDistribution(numSamples,
				slotCounts, stepSize);

		if (!slotOptimisation_) {
			for (Slot slot : policyGenerator_) {
				double slotCount = 0;
				if (slotCounts.containsKey(slot))
					slotCount = slotCounts.get(slot);
				updateDifference_ += slot.getGenerator().updateDistribution(
						slotCount, ruleCounts, stepSize);
			}
		}
	}

	/**
	 * Operations to run after the distributions have been updated. This
	 * includes further mutation of useful rules.
	 */
	public void postUpdateOperations() {
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

		// For each slot, run N mutations (where N = slot.size()) on the sampled
		// N rules (so basically only mutate the top rules).
		for (Slot slot : policyGenerator_) {
			// Get the slot information before making mutations
			ProbabilityDistribution<GuidedRule> slotRules = slot.getGenerator()
					.clone();
			int slotSize = slot.getGenerator().size();
			slotRules.normaliseProbs();
			for (int i = 0; i < slotSize; i++) {
				GuidedRule rule = slotRules.sample();
				// Create definite mutants
				if (rule == null)
					System.out.println("Problem...");
				mutateRule(rule, slot, false, true);
			}
		}

		// Mutate the rules further
		if (debugMode_) {
			try {
				System.out.println(policyGenerator_);
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
		for (Slot slot : policyGenerator_) {
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
	public static PolicyGenerator newInstance() {
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
		instance_.policyGenerator_.clear();
		for (GuidedRule rule : rules) {
			rule.setQueryParams(newQueryParams);
			Slot slot = new Slot(rule.getActionPredicate());
			slot.addNewRule(rule, false);
			instance_.policyGenerator_.add(slot);
		}
		instance_.policyGenerator_.normaliseProbs();
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

	public ProbabilityDistribution<Slot> getGenerator() {
		return policyGenerator_;
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
				rule.setSpawned(false);
				slot.addNewRule(rule, false);
				coveredRules_.put(action, rule);
			}
		}
	}

	@Override
	public String toString() {
		return policyGenerator_.toString();
	}

	/**
	 * Save rules to a file in the format
	 * 
	 * @param ruleBaseFile
	 *            The file to save the rules to.
	 */
	public static void saveRulesToFile(File ruleBaseFile) {
		ProbabilityDistribution<Slot> policyGenerator = PolicyGenerator
				.getInstance().getGenerator();

		try {
			if (!ruleBaseFile.exists())
				ruleBaseFile.createNewFile();

			FileWriter writer = new FileWriter(ruleBaseFile);
			BufferedWriter bf = new BufferedWriter(writer);

			bf.write(StateSpec.getInstance().getGoalState() + "\n");
			// For each of the rule bases
			for (Slot slot : policyGenerator) {
				// For each of the rules
				for (GuidedRule r : slot.getGenerator()) {
					bf.write(StateSpec.getInstance().encodeRule(r)
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
	public static ProbabilityDistribution<Slot> loadRulesFromFile(
			File ruleBaseFile) {
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

			// Read in a line of rules, all infering the same slot.
			while (((input = bf.readLine()) != null) && (!input.equals(""))) {
				Slot slot = null;
				// Split the base into rules
				String[] split = input.split(RULE_DELIMITER);
				// For each rule, add it to the rulebase
				for (int i = 0; i < split.length; i++) {
					String rule = StateSpec.getInstance().parseRule(split[i]);
					if (slot == null) {
						slot = new Slot(StateSpec.splitFact(rule
								.split(StateSpec.INFERS_ACTION)[1].trim())[0]);
					}

					GuidedRule gr = new GuidedRule(rule, slot);
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
	 * Saves the generators/distributions to file.
	 * 
	 * @param output
	 *            The file to output the generator to.
	 * @throws Exception
	 *             Should something go awry.
	 */
	public static void saveGenerators(File output) throws Exception {
		ProbabilityDistribution<Slot> policyGenerator = PolicyGenerator
				.getInstance().getGenerator();

		FileWriter wr = new FileWriter(output);
		BufferedWriter buf = new BufferedWriter(wr);

		// For each of the rule generators
		for (int i = 0; i < policyGenerator.size(); i++) {
			buf.write("(" + policyGenerator.getElement(i).toParsableString()
					+ ")" + ELEMENT_DELIMITER + policyGenerator.getProb(i)
					+ "\n");
		}

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
	public static void saveHumanGenerators(File output) throws Exception {
		ProbabilityDistribution<Slot> policyGenerator = PolicyGenerator
				.getInstance().getGenerator();

		FileWriter wr = new FileWriter(output);
		BufferedWriter buf = new BufferedWriter(wr);

		buf.write("A typical policy:\n");

		// Go through each slot, writing out those that fire
		ArrayList<Slot> probs = policyGenerator.getOrderedElements();
		for (Slot slot : probs) {
			// Output every non-zero rule
			boolean single = true;
			for (GuidedRule rule : slot.getGenerator()
					.getNonZeroOrderedElements()) {
				if (!single)
					buf.write(" / ");
				buf.write(StateSpec.getInstance().encodeRule(rule));
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
	 * @return The policy generator to load to.
	 */
	public static ProbabilityDistribution<Slot> loadGenerators(File input) {
		ProbabilityDistribution<Slot> dist = new ProbabilityDistribution<Slot>();
		try {
			FileReader reader = new FileReader(input);
			BufferedReader buf = new BufferedReader(reader);

			// Parse the slots
			String in = null;
			while ((in = buf.readLine()) != null) {
				// Get the slot string, ignoring the () brackets
				String slotString = in.substring(1, in
						.lastIndexOf(ELEMENT_DELIMITER) - 1);
				Slot slot = Slot.parseSlotString(slotString);
				Double prob = Double.parseDouble(in.substring(in
						.lastIndexOf(ELEMENT_DELIMITER) + 1));

				// Parse the rules
				dist.add(slot, prob);
			}

			buf.close();
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return dist;
	}
}