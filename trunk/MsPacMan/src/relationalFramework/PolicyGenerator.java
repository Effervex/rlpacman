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

	/** If we're running the experiment in debug mode. */
	public static boolean debugMode_ = false;

	/** The element delimiter between elements in the generator files. */
	public static final String ELEMENT_DELIMITER = ",";

	/** The delimiter character between rules within the same rule base. */
	public static final String RULE_DELIMITER = "@";

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
		covering_ = new Covering(actionSet_.size());
		lggRules_ = new MultiMap<String, GuidedRule>();
		mutatedRules_ = new MultiMap<String, GuidedRule>();

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
		List<GuidedRule> lggRules = new ArrayList<GuidedRule>();
		for (int i = 0; i < policyGenerator_.size(); i++) {
			// If frozen, use from list, else sample with removal.
			Slot slot = (!frozen_) ? removalDist.sampleWithRemoval() : iter
					.next();
			GuidedRule gr = slot.getGenerator().sample();
			if (gr != null)
				policy.addRule(gr, true);

			// Get the lgg rule for this slot
			if (lggRules_.containsKey(slot.getAction()))
				lggRules.addAll(lggRules_.get(slot.getAction()));
		}

		// Append the general rules as well (if they aren't already in there) to
		// ensure unnecessary covering isn't triggered.
		for (GuidedRule lggRule : lggRules) {
			if (!policy.contains(lggRule))
				policy.addRule(lggRule, false);
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
						mutateRule(coveredRule, coveredRule.getSlot(), false);
					}
				} else if (createNewRules) {
					nonLGGCoveredRules_.put(actionPred, coveredRule);
				}
			}

			if (createNewRules)
				Collections.shuffle(covered, random_);
			return covered;
		}
		return null;
	}

	/**
	 * Mutates a rule (if it hasn't spawned already) and creates and adds
	 * children to the slots.
	 * 
	 * @param baseRule
	 *            The rule to mutate.
	 * @param settledPreGoal
	 *            If the pre-goal has settled.
	 */
	private void mutateRule(GuidedRule baseRule, Slot ruleSlot,
			boolean settledPreGoal) {
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
			if (settledPreGoal) {
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
								try {
									System.out.println("\tREMOVING MUTANT: "
											+ gr);
									System.out
											.println("Press Enter to continue.");
									System.in.read();
									System.in.read();
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
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

				if (debugMode_) {
					try {
						System.out.println("\tADDED/EXISTING MUTANT: " + gr);
						System.out.println("Press Enter to continue.");
						System.in.read();
						System.in.read();
					} catch (Exception e) {
						e.printStackTrace();
					}
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
			if (settledGoals.contains(actionPred))
				System.out.println("\tSETTLED PRE-GOAL STATE " + "("
						+ actionPred + "):");
			else
				System.out.println("\tFORMING PRE-GOAL STATE " + "("
						+ actionPred + "):");

			// Check if we have LGG rules to mutate
			if (!settledGoals.isEmpty()) {
				// For each maximally general rule
				for (String settledAction : settledGoals) {
					if (lggRules_.containsKey(settledAction))
						for (GuidedRule general : lggRules_.get(settledAction))
							mutateRule(general, general.getSlot(), true);
				}
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
	 * Checks if the state has settled. This means that the pre-goal has settled
	 * and there are no non-LGG rules.
	 * 
	 * @return True if the generator values are settled, false otherwise.
	 */
	public boolean isSettled() {
		// Must have at least one pre-goal
		if (!hasPreGoal())
			return false;
		// All pre-goals must be settled (or null)
		for (String action : actionSet_.keySet()) {
			if ((covering_.getPreGoalState(action) != null)
					&& !covering_.isPreGoalSettled(action))
				return false;
		}

		// Rules need to have been created at one point
		if (nonLGGCoveredRules_.isKeysEmpty())
			return false;
		// There should be no rules under the nonLGGRules
		if (!nonLGGCoveredRules_.allValuesEmpty()) {
			return false;
		}
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
			Map<Slot, Double> slotCounts, Map<GuidedRule, Double> ruleCounts,
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
			int slotSize = slot.getGenerator().size();
			ProbabilityDistribution<GuidedRule> slotRules = slot.getGenerator()
					.clone();
			for (int i = 0; i < slotSize; i++) {
				GuidedRule rule = slotRules.sample();
				// Create definite mutants
				mutateRule(rule, slot, true);
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
			for (GuidedRule rule : slot.getGenerator().getNonZero()) {
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