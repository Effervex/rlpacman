package relationalFramework.agentObservations;

import relationalFramework.GoalCondition;
import relationalFramework.RangeBound;
import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import rrlFramework.RRLExperiment;
import rrlFramework.RRLObservations;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import jess.Fact;
import jess.Rete;
import cerrla.ProgramArgument;
import cerrla.Unification;
import cerrla.UnifiedFact;

import util.MultiMap;

/**
 * A part of AgentObservations. The LocalAgentObservations contain observation
 * information regarding the local current goal. These are loaded and saved
 * serparately from the AgentObservations.
 * 
 * @author Sam Sarjant
 */
public class LocalAgentObservations extends SettlingScan implements
		Serializable {
	private static final String SERIALISATION_FILE = "localObservations.ser";

	private static final long serialVersionUID = -7205802892263530446L;

	public static final String LOCAL_GOAL_COND_FILE = "observedGoalFacts.txt";

	/** The action conditions orientated towards the goal. */
	private MultiMap<String, RelationalPredicate> invariantGoalActionConditions_;

	/** The local goal for this particular AgentObservations object. */
	private final GoalCondition localGoal_;

	/** The invariants relating to goal observations. */
	private InvariantObservations localInvariants_;

	/** The observed predicates mentioning goal terms that have been true. */
	private MultiMap<String, RelationalPredicate> observedGoalPredicates_;

	/**
	 * The RLGG rules for this generator. Same form as environment RLGG rules,
	 * but the query terms may differ.
	 */
	private List<RelationalRule> rlggRules_;

	/** The internal rule creation class. */
	private final RuleMutation ruleMutation_;

	/** The goal conditions tied to the observed goal predicates. */
	private Collection<GoalCondition> specificGoalConds_;

	/** The action conditions orientated towards the goal. */
	private MultiMap<String, RelationalPredicate> variantGoalActionConditions_;

	/**
	 * The constructor for a new local goal object.
	 * 
	 * @param localGoal
	 *            The local goal these observations relate to.
	 */
	public LocalAgentObservations(GoalCondition localGoal) {
		localGoal_ = localGoal;
		invariantGoalActionConditions_ = MultiMap.createSortedSetMultiMap();
		variantGoalActionConditions_ = MultiMap.createSortedSetMultiMap();
		observedGoalPredicates_ = MultiMap.createSortedSetMultiMap();
		localInvariants_ = new InvariantObservations();

		EnvironmentAgentObservations.loadAgentObservations();

		ruleMutation_ = new RuleMutation();
	}

	/**
	 * Adds a goal fact to the observed goal predicates.
	 * 
	 * @param goalFact
	 *            The fact being added.
	 * @param term
	 *            The goal term to add it under.
	 * @return True if the observed facts changed.
	 */
	private boolean addGoalFact(RelationalPredicate goalFact,
			RelationalArgument term) {
		boolean observedGoalChanged = observedGoalPredicates_.putContains(
				term.toString(), goalFact);
		if (observedGoalChanged)
			specificGoalConds_ = null;
		return observedGoalChanged;
	}

	/**
	 * Checks if the type conditions of the added condition conflict with the
	 * existing types.
	 * 
	 * @param ruleConds
	 *            The existing rule conditions.
	 * @param condition
	 *            The condition to be added.
	 * @return False if the condition conflicts, true otherwise.
	 */
	private boolean checkConditionTypes(
			SortedSet<RelationalPredicate> ruleConds,
			RelationalPredicate condition) {
		// Run through the rule conditions.
		for (RelationalPredicate ruleCond : ruleConds) {
			// Only check type preds
			if (StateSpec.getInstance().isTypePredicate(ruleCond.getFactName())) {
				// Check if the type argument conflicts with the conditions
				// arguments
				String[] condArgs = condition.getArguments();
				for (int i = 0; i < condArgs.length; i++) {
					// Matching argument. Check if types conflict
					if (condArgs[i].equals(ruleCond.getArguments()[0])) {
						Collection<String> lineage = new HashSet<String>();
						StateSpec.getInstance().getTypeLineage(
								condition.getArgTypes()[i], lineage);
						if (!lineage.contains(ruleCond.getFactName()))
							return false;
					}
				}
			}
		}

		return true;
	}

	/**
	 * Initialises the action conditions if necessary.
	 * 
	 * @param goalActionConds
	 *            The goal action conditions to initialise with.
	 * @return True if the collections were initialised.
	 */
	private boolean initLocalActionConds(String action,
			Collection<RelationalPredicate> goalActionConds) {
		if (!invariantGoalActionConditions_.containsKey(action)) {
			invariantGoalActionConditions_.putCollection(action,
					goalActionConds);
			variantGoalActionConditions_.putCollection(action,
					new TreeSet<RelationalPredicate>());
			return true;
		}
		return false;
	}

	/**
	 * Checks if covering is needed.
	 * 
	 * @param state
	 *            The current state.
	 * @param validActions
	 *            The collection of all possible actions.
	 * @param activatedActions
	 *            The actions the agent's current RLGG rules produce.
	 * @return True if covering is needed - because the current RLGG rules are
	 *         not enough, or because the environment has not been observed for
	 *         some time.
	 */
	private boolean isCoveringNeeded(Rete state,
			MultiMap<String, String[]> validActions,
			MultiMap<String, String[]> activatedActions) {
		boolean changed = isScanNeeded();
		changed |= EnvironmentAgentObservations.getInstance().isCoveringNeeded(
				state, validActions, activatedActions);
		return changed;
	}

	@Override
	protected int updateHash() {
		final int prime = 31;
		int newHash = 1;
		newHash = prime
				* newHash
				+ ((invariantGoalActionConditions_ == null) ? 0
						: invariantGoalActionConditions_.hashCode());
		newHash = prime * newHash
				+ ((localGoal_ == null) ? 0 : localGoal_.hashCode());
		newHash = prime
				* newHash
				+ ((localInvariants_ == null) ? 0 : localInvariants_.hashCode());
		newHash = prime
				* newHash
				+ ((observedGoalPredicates_ == null) ? 0
						: observedGoalPredicates_.hashCode());
		newHash = prime
				* newHash
				+ ((specificGoalConds_ == null) ? 0 : specificGoalConds_
						.hashCode());
		newHash = prime
				* newHash
				+ ((variantGoalActionConditions_ == null) ? 0
						: variantGoalActionConditions_.hashCode());
		return newHash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocalAgentObservations other = (LocalAgentObservations) obj;
		if (invariantGoalActionConditions_ == null) {
			if (other.invariantGoalActionConditions_ != null)
				return false;
		} else if (!invariantGoalActionConditions_
				.equals(other.invariantGoalActionConditions_))
			return false;
		if (localGoal_ == null) {
			if (other.localGoal_ != null)
				return false;
		} else if (!localGoal_.equals(other.localGoal_))
			return false;
		if (localInvariants_ == null) {
			if (other.localInvariants_ != null)
				return false;
		} else if (!localInvariants_.equals(other.localInvariants_))
			return false;
		if (observedGoalPredicates_ == null) {
			if (other.observedGoalPredicates_ != null)
				return false;
		} else if (!observedGoalPredicates_
				.equals(other.observedGoalPredicates_))
			return false;
		if (specificGoalConds_ == null) {
			if (other.specificGoalConds_ != null)
				return false;
		} else if (!specificGoalConds_.equals(other.specificGoalConds_))
			return false;
		if (variantGoalActionConditions_ == null) {
			if (other.variantGoalActionConditions_ != null)
				return false;
		} else if (!variantGoalActionConditions_
				.equals(other.variantGoalActionConditions_))
			return false;
		return true;
	}

	/**
	 * Gathers the facts associated with the given action together and records
	 * them.
	 * 
	 * @param action
	 *            The action to gather facts for.
	 * @param goalReplacements
	 *            The current goal replacements.
	 * @param stateFacts
	 *            The state facts of this state.
	 * @return True if the action observations have changed from this operation.
	 */
	public boolean gatherActionFacts(RelationalPredicate action,
			Map<String, String> goalReplacements, Collection<Fact> stateFacts) {
		// Gather the facts
		Collection<RelationalPredicate> goalActionConds = EnvironmentAgentObservations
				.getInstance().gatherActionFacts(stateFacts, action,
						goalReplacements);
		boolean changed = initLocalActionConds(action.getFactName(),
				goalActionConds);
		if (changed)
			return true;

		// Revise the invariant/variants (return true if the method return not
		// null)
		changed = InvariantObservations.intersectActionConditions(null, null,
				goalActionConds,
				invariantGoalActionConditions_.get(action.getFactName()),
				variantGoalActionConditions_.get(action.getFactName()), null);

		// If it changed, recreate the specialisations
		if (changed)
			resetInactivity();

		return changed;
	}

	@Override
	public int getObservationHash() {
		final int prime = 13;
		return super.getObservationHash()
				* prime
				+ EnvironmentAgentObservations.getInstance()
						.getObservationHash();
	}

	/**
	 * Gets the RLGG rules with respect to the goal replacements for this goal.
	 * 
	 * @return A list of RLGG rules - at most one for each action.
	 */
	public List<RelationalRule> getRLGGRules() {
		// If the RLGG hasn't changed, return the already calculated RLGG rules.
		if (EnvironmentAgentObservations.getInstance().isChanged()
				|| rlggRules_ == null) {
			rlggRules_ = new ArrayList<RelationalRule>();

			// Get the RLGG from the invariant conditions from the action
			// conditions.
			int numGoalArgs = localGoal_.getNumArgs();
			List<String> queryTerms = new ArrayList<String>(numGoalArgs);
			for (int i = 0; i < numGoalArgs; i++)
				queryTerms.add(RelationalArgument.createGoalTerm(i));
			for (RelationalRule envRLGG : EnvironmentAgentObservations
					.getInstance().getRLGGActionRules()) {
				RelationalRule newRLGGRule = envRLGG.clone(true);
				newRLGGRule.setQueryParams(queryTerms);
				rlggRules_.add(newRLGGRule);
			}
		}
		return rlggRules_;
	}

	/**
	 * Gets the rule mutation object.
	 * 
	 * @return The internal rule mutation object.
	 */
	public RuleMutation getRuleMutation() {
		return ruleMutation_;
	}

	/**
	 * Gets a collection of goal conditions formulated from the goal predicates
	 * which can be possible goals to achieve.
	 * 
	 * TODO This could easily be used in online module learning.
	 * 
	 * @return A collection of goal conditions.
	 */
	public Collection<GoalCondition> getSpecificGoalConditions() {
		if (specificGoalConds_ == null) {
			specificGoalConds_ = new HashSet<GoalCondition>();
			for (RelationalPredicate gc : observedGoalPredicates_.values()) {
				// If the fact isn't an invariant, isn't negated, and isn't the
				// goal args predicate, add it.
				if (!localInvariants_.getSpecificInvariants().contains(gc)
						&& !gc.isNegated()
						&& !gc.getFactName().equals(StateSpec.GOALARGS_PRED)
						&& gc.isFullyNotAnonymous()
						&& !GoalCondition.formName(gc).equals(localGoal_))
					specificGoalConds_.add(new GoalCondition(gc));
			}
		}
		return specificGoalConds_;
	}

	/**
	 * Gets the maximum number of specialisations for a given action.
	 * 
	 * @param actionPred
	 *            The action used for determining the number of specialisations.
	 * @return The total number of specialisations for an action.
	 */
	public int getNumSpecialisations(String actionPred) {
		int num = ruleMutation_.getSpecialisationConditions(actionPred).size();
		if (StateSpec.getInstance().getPredicateByName(actionPred)
				.isNumerical())
			num += ProgramArgument.NUM_NUMERICAL_SPLITS.intValue();
		return num;
	}

	@Override
	public int hashCode() {
		return getObservationHash();
	}

	/**
	 * If these and the environment-wide observations are settled.
	 * 
	 * @return True if the observations are settled.
	 */
	@Override
	public boolean isSettled() {
		return super.isSettled()
				&& EnvironmentAgentObservations.getInstance().isSettled();
	}

	/**
	 * Checks if a condition is a valid condition based on the known goal
	 * conditions. The condition will either be fully anonymous, or contain goal
	 * conditions in such a way as to match the goal conditions.
	 * 
	 * @param cond
	 *            The condition to check.
	 * @param replacementMap
	 *            The replacement map to use. Must not have variables as values
	 *            (only as keys).
	 * 
	 * @return True if the condition is valid, false otherwise.
	 */
	public boolean isValidGoalCondition(RelationalPredicate cond,
			Map<String, String> replacementMap) {
		RelationalPredicate checkedCond = new RelationalPredicate(cond);
		boolean notAnonymous = checkedCond.replaceArguments(replacementMap,
				false, false);
		return observedGoalPredicates_.containsValue(checkedCond)
				|| !notAnonymous
				|| cond.getFactName().equals(StateSpec.GOALARGS_PRED);
	}

	/**
	 * Observes a state by scanning it thoroughly, noting condition relations
	 * and extracting action-related conditions.
	 * 
	 * @param observations
	 *            The state to observe.
	 * @param activatedActions
	 *            The actions the current RLGG rules output.
	 * @param goalReplacements
	 *            Optional module parameter replacements to apply to the current
	 *            goal replacements. If null, use the observation class standard
	 * @return True if the observations modified the state of the agent
	 *         observations.
	 */
	@SuppressWarnings("unchecked")
	public boolean observeState(RRLObservations observations,
			MultiMap<String, String[]> activatedActions,
			Map<String, String> goalReplacements) {
		// First check if the agent needs to observe the state
		MultiMap<String, String[]> validActions = observations
				.getValidActions();
		if (!isCoveringNeeded(observations.getState(), validActions,
				activatedActions)) {
			// Set the last state to null just once (from the main behaviour).
			if (localGoal_.isMainGoal())
				EnvironmentAgentObservations.getInstance().noteScannedState(
						null);
			return false;
		}

		if (RRLExperiment.debugMode_)
			System.out.println(localGoal_ + " Covering " + getInactivity());
		
		// The relevant facts which contain the key term
		Rete state = observations.getState();
		if (goalReplacements == null)
			goalReplacements = observations.getGoalReplacements();

		// Scan the state for condition inter-relations
		Collection<Fact> stateFacts = StateSpec.extractFacts(state);
		boolean changed = scanState(stateFacts, goalReplacements);


		// Run through each valid action.
		for (String action : validActions.keySet()) {
			// Gather the action facts for each valid action
			RelationalPredicate baseAction = StateSpec.getInstance()
					.getPredicateByName(action);
			for (String[] actionArgs : validActions.get(action)) {
				RelationalPredicate actionFact = new RelationalPredicate(
						baseAction, actionArgs);
				changed |= gatherActionFacts(actionFact, goalReplacements,
						stateFacts);
			}
		}

		boolean observeState = isChanged()
				|| EnvironmentAgentObservations.getInstance().isChanged();

		// If this didn't change, increment inactivity
		if (changed) {
			resetInactivity();
		} else
			incrementInactivity();

		// Environmental changes
		EnvironmentAgentObservations.getInstance().noteScannedState(stateFacts);

		return observeState;
	}

	@Override
	public String toString() {
		return "'" + localGoal_.toString() + "' local agent observations.";
	}

	/**
	 * Saves the current local agent observations to file.
	 * 
	 * @throws Exception
	 *             Should something go awry...
	 */
	public void saveLocalObservations(boolean saveEnvAgentObservations)
			throws Exception {
		if (saveEnvAgentObservations)
			EnvironmentAgentObservations.getInstance().saveAgentObservations();

		File environmentDir = new File(
				EnvironmentAgentObservations.AGENT_OBSERVATIONS_DIR, StateSpec
						.getInstance().getEnvironmentName()
						+ File.separatorChar);
		environmentDir.mkdir();
		File localEnvironmentDir = new File(environmentDir,
				localGoal_.toString() + File.separatorChar);
		localEnvironmentDir.mkdir();

		// Save the observed goal predicates and invariants (condition beliefs)
		File localGoalConds = new File(localEnvironmentDir,
				LocalAgentObservations.LOCAL_GOAL_COND_FILE);
		FileWriter wr = new FileWriter(localGoalConds);
		BufferedWriter buf = new BufferedWriter(wr);
		for (String term : observedGoalPredicates_.keySet()) {
			buf.write(term + ":\n  " + observedGoalPredicates_.get(term) + "\n");
		}

		buf.write("\n");
		buf.write("Local Invariants\n");
		buf.write(localInvariants_.toString());

		buf.close();
		wr.close();

		// Save the action based observations.
		File localActionCondsFile = new File(localEnvironmentDir,
				EnvironmentAgentObservations.ACTION_CONDITIONS_FILE);
		localActionCondsFile.createNewFile();
		FileWriter localWR = new FileWriter(localActionCondsFile);
		BufferedWriter localBuf = new BufferedWriter(localWR);

		// Write action conditions and ranges
		for (String action : StateSpec.getInstance().getActions().keySet()) {
			localBuf.write(action + "\n");
			localBuf.write("Local conditions: "
					+ EnvironmentAgentObservations.getInstance()
							.createSpecialisations(
									variantGoalActionConditions_.get(action),
									false, action,
									invariantGoalActionConditions_.get(action),
									variantGoalActionConditions_.get(action))
					+ "\n");
			localBuf.write("\n");
		}

		localBuf.close();
		localWR.close();

		// Serialise the observations
		File serialisedFile = new File(localEnvironmentDir, SERIALISATION_FILE);
		serialisedFile.createNewFile();
		FileOutputStream fos = new FileOutputStream(serialisedFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(this);

		oos.close();
		fos.close();
	}

	/**
	 * Scans the current state and extracts observations from the information
	 * gathered.
	 * 
	 * @param stateFacts
	 *            The facts of the state.
	 * @param goalReplacements
	 *            The current goal replacements (a -> ?G_0).
	 * @return True if the scan modified the agent observations at all.
	 */
	public boolean scanState(Collection<Fact> stateFacts,
			Map<String, String> goalReplacements) {
		// Scan the environment's state
		Collection<RelationalPredicate> goalFacts = EnvironmentAgentObservations
				.getInstance().scanState(stateFacts, goalReplacements);
		boolean changed = localInvariants_.noteSpecificInvariants(goalFacts);

		// Remove non-goal terms from goal replacements
		for (RelationalPredicate goalFact : goalFacts) {
			RelationalPredicate goalFactAnon = new RelationalPredicate(goalFact);
			goalFactAnon.retainArguments(goalReplacements.values());

			// For each goal term within the fact
			for (RelationalArgument term : goalFactAnon
					.getRelationalArguments()) {
				if (!term.isAnonymous()) {
					changed |= addGoalFact(goalFactAnon, term);

					// Negated form too
					if (!goalFactAnon.isNumerical()) {
						RelationalPredicate negFact = new RelationalPredicate(
								goalFactAnon);
						negFact.swapNegated();
						changed |= addGoalFact(negFact, term);
					}
				}
			}
		}

		if (changed)
			resetInactivity();
		return changed;
	}

	/**
	 * Simplifies a rule with an optional added condition by removing any
	 * non-necessary conditions and returning either a different set of
	 * conditions, or null.
	 * 
	 * @param ruleConds
	 *            The rule conditions to simplify.
	 * @param condition
	 *            The optional condition to be added to the rule conditions.
	 *            Useful for quickly checking duplicates/negations.
	 * @param ruleAction
	 *            The optional action for the rule being simplified.
	 * @param exitIfIllegalRule
	 *            If the conditions need to be tested for illegal combinations
	 *            as well (use background knowledge in conjugated form)
	 * @return A modified version of the input rule conditions, or null if no
	 *         change made.
	 */
	public SortedSet<RelationalPredicate> simplifyRule(
			SortedSet<RelationalPredicate> ruleConds,
			RelationalPredicate condition, RelationalPredicate ruleAction,
			boolean exitIfIllegalRule) {
		SortedSet<RelationalPredicate> simplified = new TreeSet<RelationalPredicate>(
				ruleConds);
		// Add the RLGG conditions to assist in simplification (don't add
		// numerical predicates)
		if (ruleAction != null) {
			for (RelationalPredicate rlggPred : EnvironmentAgentObservations
					.getInstance().getRLGGConditions(ruleAction)) {
				if (!rlggPred.isNumerical())
					simplified.add(rlggPred);
			}
		}

		// If we have an optional added condition, check for
		// duplicates/negation
		if (condition != null) {
			condition.swapNegated();
			Collection<UnifiedFact> negUnification = Unification.getInstance()
					.unifyFact(condition, ruleConds, null, null, false, false);
			condition.swapNegated();
			if (!negUnification.isEmpty())
				return null;

			// Need to check type conditions
			if (!checkConditionTypes(ruleConds, condition))
				return null;

			simplified.add(condition);
		}

		// Simplify using the learned background knowledge and local
		// invariants
		int result = EnvironmentAgentObservations.getInstance().simplifyRule(
				simplified, exitIfIllegalRule, false,
				localInvariants_.getSpecificInvariants());
		// If rule is illegal, return null
		if (exitIfIllegalRule && result == -1)
			return null;

		// If no change from original condition, return null
		if (simplified.equals(ruleConds))
			return null;

		// Check the rule contains only valid goal facts
		Map<String, String> replacementMap = new HashMap<String, String>();
		for (String goalTerm : observedGoalPredicates_.keySet())
			replacementMap.put(goalTerm, goalTerm);

		for (RelationalPredicate sf : simplified)
			if (!isValidGoalCondition(sf, replacementMap))
				return null;

		return simplified;
	}

	/**
	 * Gets the local observations file.
	 * 
	 * @param localGoal
	 *            The given goal.
	 * @return The File for that goal.
	 */
	private static File getLocalFile(String localGoal) {
		return new File(EnvironmentAgentObservations.AGENT_OBSERVATIONS_DIR,
				StateSpec.getInstance().getEnvironmentName()
						+ File.separatorChar + localGoal + File.separatorChar
						+ SERIALISATION_FILE);
	}

	/**
	 * Loads the local agent observations from serialised file if possible.
	 * Otherwise, it just creates a new local agent observations object.
	 * 
	 * @param localGoal
	 *            The local goal observations to load.
	 * @return The local agent observations object (loaded or new).
	 */
	public static LocalAgentObservations loadAgentObservations(
			GoalCondition localGoal) {
		// First load the singleton environment AgentObservations.
		EnvironmentAgentObservations.loadAgentObservations();

		try {
			File localObsFile = getLocalFile(localGoal.toString());
			if (localObsFile.exists()) {
				FileInputStream fis = new FileInputStream(localObsFile);
				ObjectInputStream ois = new ObjectInputStream(fis);
				LocalAgentObservations lao = (LocalAgentObservations) ois
						.readObject();
				// Reset the rlgg rules so they can be recalculated for this
				// generator.
				lao.rlggRules_ = null;
				if (lao != null) {
					return lao;
				}
			}
		} catch (Exception e) {
		}
		System.out.println("No local agent observations to load.");
		return new LocalAgentObservations(localGoal);
	}

	/**
	 * An internal class which handles the
	 * 
	 * @author Sam Sarjant
	 * 
	 */
	public class RuleMutation implements Serializable {
		private static final long serialVersionUID = -5239359052379344563L;

		/**
		 * The conditions that can be added to rules for specialisation.
		 * Essentially the variant action conditions both negated and normal,
		 * then simplified.
		 */
		private transient MultiMap<String, RelationalPredicate> specialisationConditions_;

		/**
		 * Creates a range using the minimum and maximum observed ranges.
		 * 
		 * @param baseRule
		 *            The base rule to mutate.
		 * @param condition
		 *            The original condition being mutated.
		 * @param condArgIndex
		 *            The index of the mutation.
		 * @param rangeVariable
		 *            The range variable.
		 * @param context
		 *            The context of the range.
		 * @param throughZeroRange
		 *            If this range goes through 0 (one side is negative, the
		 *            other positive).
		 * @return The subranges.
		 */
		private Collection<RelationalRule> createNewSubRanges(
				RelationalRule baseRule, RelationalPredicate condition,
				int condArgIndex, String rangeVariable, RangeContext context,
				boolean throughZeroRange) {
			// Create 3 ranges, the min and max split in two and a range
			// overlapping each centred about the middle.
			RangeBound minBound = new RangeBound(rangeVariable, RangeBound.MIN);
			RangeBound maxBound = new RangeBound(rangeVariable, RangeBound.MAX);
			Collection<RelationalRule> subranges = splitIntoThree(baseRule,
					condition, condArgIndex, rangeVariable, minBound, maxBound,
					0, 1, context);
			if (!baseRule.isMutant() && throughZeroRange) {
				subranges.add(createRangedSpecialisation(baseRule, condition,
						condArgIndex, new RelationalArgument(rangeVariable,
								minBound, 0d, new RangeBound(0), 1d, context)));
				subranges.add(createRangedSpecialisation(baseRule, condition,
						condArgIndex, new RelationalArgument(rangeVariable,
								new RangeBound(0), 0d, maxBound, 1d, context)));
			}
			return subranges;
		}

		/**
		 * Creates a ranged specialisation of an existing range within a rule.
		 * 
		 * @param baseRule
		 *            The base rule to specialise.
		 * @param condition
		 *            The condition within the rule to specialise.
		 * @param condArgIndex
		 *            The argument index in the condition to specialise.
		 * @param rangeVariable
		 *            The range variable at that index.
		 * @param lowerBound
		 *            The lower bound of the range.
		 * @param upperBound
		 *            The upper bound of the range.
		 * @return A specialised GuidedRule in the same form as baseRule, but
		 *         with a smaller range.
		 */
		private RelationalRule createRangedSpecialisation(
				RelationalRule baseRule, RelationalPredicate condition,
				int condArgIndex, RelationalArgument subRangeArg) {
			RelationalArgument[] mutantArgs = condition
					.getRelationalArguments();
			mutantArgs[condArgIndex] = subRangeArg;
			RelationalPredicate mutantFact = new RelationalPredicate(condition,
					mutantArgs);
			SortedSet<RelationalPredicate> cloneConds = baseRule
					.getConditions(false);
			cloneConds.remove(condition);
			cloneConds.add(mutantFact);

			RelationalRule mutant = new RelationalRule(cloneConds,
					baseRule.getAction(), baseRule);
			mutant.setQueryParams(baseRule.getQueryParameters());
			return mutant;
		}

		/**
		 * Gets the specialisation conditions (both global and local conditions)
		 * for an action.
		 * 
		 * @param actionPred
		 *            The action for getting the conditions.
		 * @return A collection of specialisation conditions.
		 */
		public Collection<RelationalPredicate> getSpecialisationConditions(
				String actionPred) {
			if (specialisationConditions_ == null
					|| specialisationConditions_.get(actionPred) == null)
				recreateAllSpecialisations(actionPred);
			return specialisationConditions_.get(actionPred);
		}

		/**
		 * A wrapper method for ease.
		 * 
		 * @param baseRule
		 *            The base rule to mutate.
		 * @param condition
		 *            The original condition being mutated.
		 * @param condArgIndex
		 *            The index of the mutation.
		 * @param rangeArg
		 *            The actual ranged argument itself.
		 * @param context
		 *            The context of the range.
		 * @return Three subranges.
		 */
		private Collection<RelationalRule> splitExistingRange(
				RelationalRule baseRule, RelationalPredicate condition,
				int condArgIndex, RelationalArgument rangeArg,
				RangeContext context) {
			RangeBound[] rangeBounds = rangeArg.getRangeBounds();
			double[] rangeFracs = rangeArg.getRangeFrac();

			return splitIntoThree(baseRule, condition, condArgIndex,
					rangeArg.getStringArg(), rangeBounds[0], rangeBounds[1],
					rangeFracs[0], rangeFracs[1], context);
		}

		/**
		 * Splits an existing range into 3: first half, last half, and middle
		 * half.
		 * 
		 * @param baseRule
		 *            The rule to insert the subrange into.
		 * @param condition
		 *            The condition to insert the subrange into.
		 * @param condArgIndex
		 *            The index of the subrange.
		 * @param rangeVariable
		 *            The range variable.
		 * @param minBound
		 *            The lower bound of the range (possibly null).
		 * @param maxBound
		 *            The upper bound of the range (possibly null).
		 * @param minFrac
		 *            The lower fraction of the range.
		 * @param maxFrac
		 *            The upper fraction of the range.
		 * @param context
		 *            The context of the range.
		 */
		private Collection<RelationalRule> splitIntoThree(
				RelationalRule baseRule, RelationalPredicate condition,
				int condArgIndex, String rangeVariable, RangeBound minBound,
				RangeBound maxBound, double minFrac, double maxFrac,
				RangeContext context) {
			Collection<RelationalRule> subranges = new HashSet<RelationalRule>();
			double diff = maxFrac - minFrac;
			// First half
			RelationalArgument subrange = new RelationalArgument(rangeVariable,
					minBound, minFrac, maxBound, minFrac + 0.5 * diff, context);
			subranges.add(createRangedSpecialisation(baseRule, condition,
					condArgIndex, subrange));

			// Last half
			subrange = new RelationalArgument(rangeVariable, minBound, minFrac
					+ 0.5 * diff, maxBound, maxFrac, context);
			subranges.add(createRangedSpecialisation(baseRule, condition,
					condArgIndex, subrange));

			// Middle half
			subrange = new RelationalArgument(rangeVariable, minBound, minFrac
					+ 0.25 * diff, maxBound, minFrac + 0.75 * diff, context);
			subranges.add(createRangedSpecialisation(baseRule, condition,
					condArgIndex, subrange));
			return subranges;
		}

		/**
		 * Splits any ranges in a rule into a number of smaller uniform
		 * sub-ranges.
		 * 
		 * @param baseRule
		 *            The base rule to mutate.
		 * @param isRuleMutant
		 *            If the rule is a mutant or not.
		 * @return A collection of any sub-ranged mutants created from the rule.
		 */
		private Set<RelationalRule> splitRanges(RelationalRule baseRule,
				boolean isRuleMutant) {
			Set<RelationalRule> subranges = new HashSet<RelationalRule>();

			// Run through each condition
			for (RelationalPredicate condition : baseRule.getConditions(false)) {
				if (condition.isNumerical() && !condition.isNegated()) {
					String[] argTypes = condition.getArgTypes();
					for (int i = 0; i < condition.getArguments().length; i++) {
						RelationalArgument arg = condition
								.getRelationalArguments()[i];
						// If the arg is a number
						if (StateSpec.isNumberType(argTypes[i])
								&& !arg.equals(RelationalArgument.ANONYMOUS)) {
							RangeContext context = new RangeContext(i,
									condition, baseRule.getAction());
							// If the arg is a range or represents a range, can
							// split it
							if (arg.isRange()) {
								subranges.addAll(splitExistingRange(baseRule,
										condition, i, arg, context));
							} else {
								double[] range = EnvironmentAgentObservations
										.getActionRanges(context);
								if (range != null) {
									subranges.addAll(createNewSubRanges(
											baseRule, condition, i,
											arg.getStringArg(), context,
											range[0] * range[1] < 0));
								}
							}
						}
					}
				}
			}

			return subranges;
		}

		/**
		 * Swaps a term in a rule for another goal term, assuming the swap is
		 * valid in regards to the existing conditions.
		 * 
		 * @param rule
		 *            The rule.
		 * @param goalPredicates
		 *            The goal predicate replacement map.
		 * @param oldTerms
		 *            The rule action terms.
		 * @param i
		 *            The action term index.
		 * @param goalTerm
		 *            The goal term to replace in.
		 * @return The swapped rule term (if valid).
		 */
		private RelationalRule swapRuleTerm(RelationalRule rule,
				String[] oldTerms, int i, String goalTerm) {
			String[] newTerms = Arrays.copyOf(oldTerms, oldTerms.length);
			newTerms[i] = goalTerm;
			SortedSet<RelationalPredicate> ruleConditions = rule
					.getConditions(true);
			SortedSet<RelationalPredicate> specConditions = new TreeSet<RelationalPredicate>(
					ruleConditions.comparator());
			// Form the replacement map
			Map<String, String> replacementMap = new HashMap<String, String>();
			replacementMap.put(oldTerms[i], newTerms[i]);
			for (String gTerm : observedGoalPredicates_.keySet()) {
				replacementMap.put(gTerm, gTerm);
			}

			// Run through the rule conditions, checking each replaced
			// term is valid in regards to goal options.
			boolean validRule = true;
			for (RelationalPredicate cond : ruleConditions) {
				// Check if the condition is a valid goal condition.
				if (isValidGoalCondition(cond, replacementMap)) {
					// If the replacement is valid, then add a replaced
					// fact (retaining other args) to the specialised
					// conditions.
					RelationalPredicate specCond = new RelationalPredicate(cond);
					specCond.replaceArguments(replacementMap, true, false);
					specConditions.add(specCond);
				} else {
					validRule = false;
					break;
				}
			}

			EnvironmentAgentObservations.getInstance().simplifyRule(
					specConditions, false, false,
					localInvariants_.getSpecificInvariants());

			if (validRule) {
				// Create the mutant
				RelationalRule mutant = new RelationalRule(specConditions,
						new RelationalPredicate(rule.getAction(), newTerms),
						rule);
				mutant.setQueryParams(rule.getQueryParameters());
				mutant.expandConditions();
				return mutant;
			}
			return null;
		}



		/**
		 * Recreates all the specialisation conditions (local and environmental)
		 * that can be added to rules.
		 * 
		 * @param actionPred
		 *            The action to create the conditions for.
		 */
		public void recreateAllSpecialisations(String actionPred) {
			if (specialisationConditions_ == null)
				specialisationConditions_ = MultiMap.createSortedSetMultiMap();
			else
				specialisationConditions_.clear();

			// Add the environment specialisation conditions
			specialisationConditions_.putCollection(actionPred,
					EnvironmentAgentObservations.getInstance()
							.getSpecialisationConditions(actionPred));
			specialisationConditions_.putCollection(
					actionPred,
					EnvironmentAgentObservations.getInstance()
							.createSpecialisations(
									variantGoalActionConditions_
											.get(actionPred),
									false,
									actionPred,
									invariantGoalActionConditions_
											.get(actionPred),
									variantGoalActionConditions_
											.get(actionPred)));
		}

		/**
		 * Specialise a rule by adding a condition to it which includes a term
		 * used in the action.
		 * 
		 * @param rule
		 *            The rule to specialise.
		 * @return A collection of possible specialisations of the rule.
		 */
		public Set<RelationalRule> specialiseRule(RelationalRule rule) {
			Set<RelationalRule> specialisations = new HashSet<RelationalRule>();

			String actionPred = rule.getActionPredicate();
			// If the action has no action conditions, return empty
			Collection<RelationalPredicate> actionConditions = getSpecialisationConditions(actionPred);
			if (actionConditions == null)
				return specialisations;
			SortedSet<RelationalPredicate> conditions = rule
					.getConditions(false);
			RelationalPredicate action = rule.getAction();
			String[] actionTerms = action.getArguments();

			// Add conditions, one-by-one, using both negation and regular
			for (RelationalPredicate condition : actionConditions) {
				// Modify the condition arguments to match the action arguments.
				condition = new RelationalPredicate(condition);
				condition.replaceArguments(actionTerms);

				// Check for the regular condition
				SortedSet<RelationalPredicate> specConditions = simplifyRule(
						conditions, condition, action, true);
				if (specConditions != null) {
					RelationalRule specialisation = new RelationalRule(
							specConditions, action, rule);
					specialisation.setQueryParams(rule.getQueryParameters());
					specialisation.expandConditions();
					if (!specialisations.contains(specialisation)
							&& !specialisation.equals(rule)) {
						// Only add specialisations if they contain goal
						// conditions (if there are goal conditions).
						if (!ProgramArgument.ONLY_GOAL_RULES.booleanValue()
								|| localGoal_.getNumArgs() == 0
								|| specConditions
										.toString()
										.contains(
												RelationalArgument.GOAL_VARIABLE_PREFIX)) {
							specialisations.add(specialisation);
						}
					}
				}
			}

			return specialisations;
		}

		/**
		 * Specialises a rule by making minor (non slot-splitting) changes to
		 * the rule's conditions. These include range splitting and specialising
		 * variables to constants.
		 * 
		 * @param rule
		 *            The rule to specialise.
		 * @return All single-step mutations the rule can take towards matching
		 *         the pre-goal state.
		 */
		public Set<RelationalRule> specialiseRuleMinor(RelationalRule rule) {
			Set<RelationalRule> mutants = new HashSet<RelationalRule>();

			// Replace the variables with constants
			String[] oldTerms = rule.getActionTerms();
			// For every goal term
			for (String goalTerm : observedGoalPredicates_.keySet()) {
				boolean termPresent = false;
				// If the old terms already contain the goal term, can't swap
				for (String oldTerm : oldTerms) {
					if (goalTerm.equals(oldTerm)) {
						termPresent = true;
						break;
					}
				}

				if (!termPresent) {
					// For every action term
					for (int i = 0; i < oldTerms.length; i++) {
						// If the term is already a goal term, can't swap
						if (!RelationalArgument.isGoalCondition(oldTerms[i])) {
							RelationalRule swappedRule = swapRuleTerm(rule,
									oldTerms, i, goalTerm);
							if (swappedRule != null)
								mutants.add(swappedRule);
						}
					}
				}
			}

			// Split any ranges up
			mutants.addAll(splitRanges(rule, rule.isMutant()));

			return mutants;
		}
	}
}
