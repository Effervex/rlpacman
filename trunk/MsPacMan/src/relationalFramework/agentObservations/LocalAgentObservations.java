package relationalFramework.agentObservations;

import relationalFramework.RangeBound;
import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import rrlFramework.Config;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jess.Fact;
import jess.Rete;
import cerrla.LocalCrossEntropyDistribution;
import cerrla.ProgramArgument;
import cerrla.RLGGMerger;
import cerrla.MergedFact;
import cerrla.modular.GeneralGoalCondition;
import cerrla.modular.GoalCondition;
import cerrla.modular.SpecificGoalCondition;

import util.MultiMap;

/**
 * A part of AgentObservations. The LocalAgentObservations contain observation
 * information regarding the local current goal. These are loaded and saved
 * separately from the AgentObservations.
 * 
 * @author Sam Sarjant
 */
public class LocalAgentObservations extends SettlingScan implements
		Serializable {
	private static final long serialVersionUID = -8851699143692096901L;

	private static final String SERIALISATION_FILE = "localObservations.ser";

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
	 * but the query terms and slots may differ.
	 */
	private transient Map<String, RelationalRule> rlggRules_;

	/** The internal rule creation class. */
	private final RuleMutation ruleMutation_;

	/** The specific goal conditions tied to the observed goal predicates. */
	private Collection<GoalCondition> specificGoalConds_;

	/** The action conditions orientated towards the goal. */
	private MultiMap<String, RelationalPredicate> variantGoalActionConditions_;

	/** The local distribution for these agent observations. */
	private transient LocalCrossEntropyDistribution lced_;

	/** The observed ranges. */
	private Map<RangeContext, double[]> rangeContexts_;

	/** A static collection of all the active local agent observations. */
	private transient static Map<GoalCondition, LocalAgentObservations> localAOManager_;

	/**
	 * The constructor for a new local goal object.
	 * 
	 * @param localGoal
	 *            The local goal these observations relate to.
	 */
	public LocalAgentObservations(GoalCondition localGoal,
			LocalCrossEntropyDistribution lced) {
		localGoal_ = localGoal;
		lced_ = lced;
		invariantGoalActionConditions_ = MultiMap.createSortedSetMultiMap();
		variantGoalActionConditions_ = MultiMap.createSortedSetMultiMap();
		observedGoalPredicates_ = MultiMap.createSortedSetMultiMap();
		localInvariants_ = new InvariantObservations();
		rangeContexts_ = new HashMap<RangeContext, double[]>();

		EnvironmentAgentObservations.loadAgentObservations();

		ruleMutation_ = new RuleMutation();

		addLAO(this);
	}

	/**
	 * Adds a {@link LocalAgentObservations} to the laoManager.
	 * 
	 * @param lao
	 *            The local agent observations to add.
	 */
	private static void addLAO(LocalAgentObservations lao) {
		if (localAOManager_ == null)
			localAOManager_ = new HashMap<GoalCondition, LocalAgentObservations>();
		localAOManager_.put(lao.localGoal_, lao);
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
			Collection<RelationalPredicate> ruleConds,
			RelationalPredicate condition) {
		Map<String, Collection<String>> typeArguments = new HashMap<String, Collection<String>>();
		RelationalArgument[] condArgs = condition.getRelationalArguments();
		String[] condTypes = condition.getArgTypes();
		for (int i = 0; i < condArgs.length; i++) {
			// Negated type predicate case
			if (condition.isNegated()
					&& StateSpec.getInstance().isTypePredicate(
							condition.getFactName())) {
				typeArguments.put(condArgs[i].toString(), StateSpec
						.getInstance().getTypeParents(condTypes[i]));
			} else if (condArgs[i].isVariable()) {
				typeArguments.put(condArgs[i].toString(), StateSpec
						.getInstance().getTypeLineage(condTypes[i]));
			}
		}

		// Run through the rule conditions.
		Collection<RelationalPredicate> removedNegated = new ArrayList<RelationalPredicate>();
		for (RelationalPredicate ruleCond : ruleConds) {
			// Check the lineage against each fact.
			String[] ruleArgs = ruleCond.getArguments();
			String[] ruleTypes = ruleCond.getArgTypes();
			for (int j = 0; j < ruleArgs.length; j++) {
				if (typeArguments.containsKey(ruleArgs[j])) {
					// Matching argument. Check if types conflict
					Collection<String> lineage = typeArguments.get(ruleArgs[j]);

					// Negated case
					if (ruleCond.isNegated()) {
						// Check the negated argument is still valid
						if (!condition.isNegated()
								&& !lineage.contains(ruleTypes[j])) {
							removedNegated.add(ruleCond);
							break;
						}
					} else {
						if (!lineage.contains(ruleTypes[j]))
							return false;
					}
				}
			}
		}

		ruleConds.removeAll(removedNegated);
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
			Map<RelationalArgument, RelationalArgument> goalReplacements,
			Collection<Fact> stateFacts) {
		// Gather the facts
		Collection<RelationalPredicate> goalActionConds = EnvironmentAgentObservations
				.getInstance().gatherActionFacts(stateFacts, action,
						goalReplacements, rangeContexts_);
		boolean changed = initLocalActionConds(action.getFactName(),
				goalActionConds);
		if (changed)
			return true;

		// Revise the invariant/variants (return true if the method return not
		// null)
		changed = InvariantObservations.intersectActionConditions(action,
				action, goalActionConds,
				invariantGoalActionConditions_.get(action.getFactName()),
				variantGoalActionConditions_.get(action.getFactName()), null,
				goalReplacements);

		// If it changed, recreate the specialisations
		if (changed) {
			resetInactivity();
		}

		return changed;
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
	public int getObservationHash() {
		final int prime = 13;
		return super.getObservationHash()
				* prime
				+ EnvironmentAgentObservations.getInstance()
						.getObservationHash();
	}

	/**
	 * Gets a collection of specific goal conditions noted by the observations
	 * which represent specific sub-goals to achieve.
	 * 
	 * @return A collection of specific goal conditions.
	 */
	public Collection<GoalCondition> getObservedSubGoals() {
		if (specificGoalConds_ == null) {
			specificGoalConds_ = new HashSet<GoalCondition>();
			for (RelationalPredicate pred : observedGoalPredicates_.values()) {
				// If the fact isn't an invariant, isn't negated, and isn't the
				// goal args predicate, add it.
				if (!localInvariants_.getSpecificInvariants().contains(pred)
						&& !pred.isNegated()
						&& !pred.getFactName().equals(StateSpec.GOALARGS_PRED)
						&& pred.isFullyNotAnonymous()) {
					GoalCondition goalCond = new SpecificGoalCondition(pred);
					if (!goalCond.equals(localGoal_))
						specificGoalConds_.add(goalCond);
				}
			}
		}

		// Return either specific goal conds or all (depending on args).
		if (!ProgramArgument.USE_GENERAL_MODULES.booleanValue())
			return specificGoalConds_;
		else {
			Collection<GoalCondition> allGoals = new HashSet<GoalCondition>(
					specificGoalConds_);
			allGoals.addAll(EnvironmentAgentObservations.getInstance()
					.getGeneralGoalConditions());
			return allGoals;
		}
	}

	/**
	 * Gets the RLGG rules with respect to the goal replacements for this goal.
	 * 
	 * @param oldRLGGs
	 *            The old RLGG rules to potentially modify.
	 * @return A collection of RLGG rules - at most one for each action.
	 */
	public Collection<RelationalRule> getRLGGRules(
			Collection<RelationalRule> oldRLGGs) {
		// If the RLGG hasn't changed, return the already calculated RLGG rules.
		if (EnvironmentAgentObservations.getInstance().isChanged()
				|| rlggRules_ == null) {
			if (rlggRules_ == null) {
				rlggRules_ = new HashMap<String, RelationalRule>();
				for (RelationalRule oldRLGG : oldRLGGs)
					rlggRules_.put(oldRLGG.getActionPredicate(), oldRLGG);
			}

			// Get the RLGG from the invariant conditions from the action
			// conditions.
			int numGoalArgs = localGoal_.getNumArgs();
			List<RelationalArgument> queryTerms = new ArrayList<RelationalArgument>(
					numGoalArgs);
			for (int i = 0; i < numGoalArgs; i++)
				queryTerms.add(RelationalArgument.createGoalTerm(i));
			for (RelationalRule envRLGG : EnvironmentAgentObservations
					.getInstance().getRLGGActionRules()) {
				String actionPred = envRLGG.getActionPredicate();
				RelationalRule localisedRLGG = null;
				if (rlggRules_.containsKey(actionPred)) {
					// Modify the local RLGG rule
					localisedRLGG = rlggRules_.get(actionPred);
					localisedRLGG.setActionTerms(envRLGG.getActionTerms());
					localisedRLGG.setQueryParams(queryTerms);
					localisedRLGG
							.setConditions(envRLGG.getRawConditions(false));
				} else {
					// Insert the new RLGG (with query params) into the rlgg
					// rules.
					localisedRLGG = envRLGG.clone(true, lced_);
					localisedRLGG.setQueryParams(queryTerms);
					rlggRules_.put(actionPred, localisedRLGG);
				}

				// Add the negated general goal conditions and check the RLGG is
				// still valid.
				if (localGoal_ instanceof GeneralGoalCondition) {
					// First check that the RLGG doesn't contain the goal
					Collection<RelationalPredicate> rlggConds = localisedRLGG
							.getRawConditions(false);
					boolean validRLGG = true;
					for (RelationalPredicate rlggCond : rlggConds) {
						if (rlggCond.getFactName().equals(
								localGoal_.getFactName())
								&& rlggCond.isNegated() == localGoal_.getFact()
										.isNegated()) {
							// The rule is invalid for this sub-goal and won't
							// be used.
							rlggRules_.remove(actionPred);
							validRLGG = false;
							break;
						}
					}

					// Next, add the general RLGG conditions to the rule
					// (various negations of the goal).
					if (validRLGG) {
						Collection<RelationalPredicate> generalRLGGConds = ruleMutation_
								.getGeneralRLGGConds(actionPred);
						if (generalRLGGConds != null
								&& !generalRLGGConds.isEmpty()) {

							rlggConds.addAll(generalRLGGConds);
							localisedRLGG.setConditions(rlggConds);

							// Checking the RLGG is still valid.
							if (!localisedRLGG.isLegal())
								rlggRules_.remove(actionPred);
						}
					}
				}
			}
		}
		return rlggRules_.values();
	}

	/**
	 * Gets the rule mutation object.
	 * 
	 * @return The internal rule mutation object.
	 */
	public RuleMutation getRuleMutation() {
		return ruleMutation_;
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
			Map<RelationalArgument, RelationalArgument> goalReplacements) {
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

		if (RRLExperiment.debugMode_) {
			System.out.println("Environment Covering "
					+ EnvironmentAgentObservations.getInstance()
							.getInactivity());
			System.out.println(localGoal_ + " Covering " + getInactivity());
		}

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
	public void resetInactivity() {
		super.resetInactivity();
		ruleMutation_.specialisationConditions_ = null;
		ruleMutation_.localRLGGConditions_ = null;
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
			localBuf.write("Observed ranges: [");
			boolean first = true;
			for (RangeContext rc : rangeContexts_.keySet()) {
				if (rc.getAction().equals(action)) {
					if (!first)
						localBuf.write(", ");
					localBuf.write(rc.toString(rangeContexts_.get(rc)));
					first = false;
				}
			}
			localBuf.write("]\n\n");
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
			Map<RelationalArgument, RelationalArgument> goalReplacements) {
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
	public Collection<RelationalPredicate> simplifyRule(
			Collection<RelationalPredicate> ruleConds,
			RelationalPredicate condition, RelationalPredicate ruleAction,
			boolean exitIfIllegalRule) {
		Collection<RelationalPredicate> simplified = new HashSet<RelationalPredicate>();
		RelationalArgument[] actionArgs = null;
		if (ruleAction != null)
			actionArgs = ruleAction.getRelationalArguments();
		for (RelationalPredicate cond : ruleConds) {
			if (ruleAction != null)
				cond.setActionVariables(actionArgs);
			simplified.add(cond);
		}

		// If we have an optional added condition, check for
		// duplicates/negation
		if (condition != null) {
			condition.swapNegated();
			Collection<MergedFact> negUnification = RLGGMerger.getInstance()
					.unifyFactToState(condition, simplified, null, null, false);
			condition.swapNegated();
			if (!negUnification.isEmpty())
				return null;

			// Need to check type conditions
			if (!checkConditionTypes(simplified, condition))
				return null;

			if (ruleAction != null)
				condition.setActionVariables(ruleAction
						.getRelationalArguments());
			simplified.add(condition);
		}

		// Simplify using the learned background knowledge and local
		// invariants
		int result = EnvironmentAgentObservations.getInstance().simplifyRule(
				simplified, exitIfIllegalRule, false,
				localInvariants_.getSpecificInvariants());
		// If rule is illegal, return null
		if (result == -1) {
			// System.out.println("Illegal rule conds: " + simplified);
			return null;
		}

		// Check the rule contains only valid goal facts
		Map<String, String> replacementMap = new HashMap<String, String>();
		for (String goalTerm : observedGoalPredicates_.keySet())
			replacementMap.put(goalTerm, goalTerm);

		// Check that all conditions are valid with regards to observed goal
		// conditions.
		for (RelationalPredicate sf : simplified)
			if (!isValidGoalCondition(sf, replacementMap))
				return null;

		// TODO Need to remove redundant negated predicates (bounded by type)


		return simplified;
	}

	@Override
	public String toString() {
		return "'" + localGoal_.toString() + "' local agent observations.";
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
	 *            The goal condition these agent observations are for.
	 * @param lced
	 *            The local distribution these observations are for.
	 * 
	 * @return The local agent observations object (loaded or new).
	 */
	public static LocalAgentObservations loadAgentObservations(
			GoalCondition localGoal, LocalCrossEntropyDistribution lced) {
		if (ProgramArgument.LOAD_AGENT_OBSERVATIONS.booleanValue()
				|| Config.getInstance().shouldLoadAgentObservations()) {
			// First load the singleton environment AgentObservations.
			EnvironmentAgentObservations.loadAgentObservations();
			try {
				File localObsFile = getLocalFile(localGoal.toString());
				if (localObsFile.exists()) {
					FileInputStream fis = new FileInputStream(localObsFile);
					ObjectInputStream ois = new ObjectInputStream(fis);
					LocalAgentObservations lao = (LocalAgentObservations) ois
							.readObject();
					if (lao != null) {
						lao.lced_ = lced;
						if (localGoal.isMainGoal())
							lao.localGoal_.setAsMainGoal();
						addLAO(lao);

						return lao;
					}
				}
			} catch (Exception e) {
			}
		}
		System.out.println("No local agent observations to load.");
		return new LocalAgentObservations(localGoal, lced);
	}

	/**
	 * Gets the maximal bounds of a numerical range using a specific range
	 * context, if such a range exists.
	 * 
	 * @param rangeContext
	 *            The context of the range being selected.
	 * @param gc
	 *            The specific goal of the object accessing the range or null if
	 *            using main goal.
	 * @return The maximal bounds of the range.
	 */
	public static double[] getActionRanges(RangeContext rangeContext,
			GoalCondition gc) {
		if (gc == null || !localAOManager_.containsKey(gc)) {
			// Use the main goal (or the last one)
			for (GoalCondition localGC : localAOManager_.keySet()) {
				gc = localGC;
				if (gc.isMainGoal())
					break;
			}
		}
		LocalAgentObservations lao = localAOManager_.get(gc);

		// Get the range from the actions
		double[] range = lao.rangeContexts_.get(rangeContext);
		if (range == null) {
			// Use a global range
			rangeContext = new RangeContext(rangeContext);
			rangeContext.removeAction();
			range = EnvironmentAgentObservations.getInstance().getGlobalRange(
					rangeContext);
		}
		return range;
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

		/** Localised RLGG conds to add to the RLGG for general goals. */
		private transient MultiMap<String, RelationalPredicate> localRLGGConditions_;

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
			Collection<RelationalPredicate> cloneConds = baseRule
					.getRawConditions(true);
			cloneConds.remove(condition);
			cloneConds.add(mutantFact);

			RelationalRule mutant = new RelationalRule(cloneConds,
					baseRule.getAction(), baseRule, lced_);
			mutant.setQueryParams(baseRule.getQueryParameters());
			return mutant;
		}

		/**
		 * Creates a specialised rule.
		 * 
		 * @param condition
		 * 
		 * @param rule
		 *            The parent rule.
		 * @param specialisations
		 *            The specialisations set to add to.
		 */
		private void createSpecialisation(RelationalPredicate condition,
				RelationalRule rule, Set<RelationalRule> specialisations) {
			// Create the specialisation
			RelationalRule specialisation = new RelationalRule(rule, condition);
			if (!specialisation.isLegal())
				return;

			specialisation.setQueryParams(rule.getQueryParameters());
			if (condition.isNumerical()
					&& !condition.isNegated()
					&& !specialisation.getSimplifiedConditions(false).contains(
							condition)) {
				// If the numerical condition is already in the rule (with a
				// different number ID)
				int numberIndex = 0;
				String[] condArgs = condition.getArgTypes();
				for (numberIndex = 0; numberIndex < condArgs.length; numberIndex++)
					if (StateSpec.isNumberType(condArgs[numberIndex]))
						break;
				RangeContext rc = new RangeContext(numberIndex, condition,
						rule.getAction());
				if (rule.getRangeContexts().contains(rc))
					return;

				// If a numerical condition was added, but simplified away, then
				// split it and add the splits.
				for (RelationalRule subrange : splitConditionsRanges(
						specialisation, condition)) {
					if (subrange.isLegal()
							&& !specialisations.contains(subrange))
						specialisations.add(subrange);
				}
			}

			if (!specialisations.contains(specialisation)
					&& !specialisation.equals(rule)) {
				// Only add specialisations if they contain goal
				// conditions (if there are goal conditions).
				if (!ProgramArgument.ONLY_GOAL_RULES.booleanValue()
						|| localGoal_.getNumArgs() == 0
						|| rule.toString().contains(
								RelationalArgument.GOAL_VARIABLE_PREFIX)) {
					specialisations.add(specialisation);
				}
			}
		}

		/**
		 * Creates a specialisation from the given conditions by grounding the
		 * rule (replacing anonymous terms with unbound variables) in all
		 * possible combinations based on existing unbound variables.
		 * 
		 * @param condition
		 *            The condition that is to be added.
		 * @param rule
		 *            The rule that the specialised rules came from.
		 * @param specialisations
		 *            The existing specialisations.
		 * @param specConditions
		 *            The specialised conditions of the rule.
		 * @return A collection of all possible specialisations (usually just
		 *         one).
		 */
		private Set<RelationalRule> groundAnonymousTerms(
				RelationalPredicate condition, RelationalRule rule,
				Set<RelationalRule> specialisations) {
			// Modify the condition arguments to match the action arguments.
			condition = new RelationalPredicate(condition);
			condition.replaceArguments(rule.getActionTerms());
			if (condition.isFullyNotAnonymous()) {
				createSpecialisation(condition, rule, specialisations);
			} else {
				// Match the anon argument to the unbound types, wherever
				// possible.
				MultiMap<String, String> unboundTypes = null;
				if (!ProgramArgument.USING_UNBOUND_VARS.booleanValue()) {
					unboundTypes = MultiMap.createSortedSetMultiMap();
					createSpecialisation(condition, rule, specialisations);
				} else {
					unboundTypes = rule.getUnboundTypeConditions();

					// Determine the anon type
					Collection<RelationalPredicate> resultPreds = new ArrayList<RelationalPredicate>();
					RelationalArgument[] args = condition
							.getRelationalArguments();
					recursivelyGroundAnons(args, 0, unboundTypes, condition,
							resultPreds);

					// Swap the grounded result preds in
					for (RelationalPredicate result : resultPreds) {
						createSpecialisation(result, rule, specialisations);
					}
				}
			}
			return specialisations;
		}

		/**
		 * Recursively grounds anonymous variables into rules by testing
		 * existing unbound variables on them.
		 * 
		 * @param args
		 *            The state of the arguments for the predicate.
		 * @param i
		 *            The current index in the args.
		 * @param unboundTypes
		 *            The unbound type map for the rule.
		 * @param anonVariable
		 *            The original anonymous variable to replace.
		 * @param resultPreds
		 *            A to-be-filled collection of grounded relational
		 *            predicates.
		 */
		private void recursivelyGroundAnons(RelationalArgument[] args, int i,
				MultiMap<String, String> unboundTypes,
				RelationalPredicate anonVariable,
				Collection<RelationalPredicate> resultPreds) {
			// Base case, final index reached
			if (i == args.length) {
				RelationalPredicate swappedPred = new RelationalPredicate(
						anonVariable, args);
				if (!swappedPred.isFullyAnonymous()) {
					if (anonVariable.isNegated())
						swappedPred.swapNegated();
					resultPreds.add(swappedPred);
				}
				return;
			}

			// Can always recurse with existing variable (creates new unbound if
			// anonymous).
			recursivelyGroundAnons(args, i + 1, unboundTypes, anonVariable,
					resultPreds);
			if (args[i].isAnonymous()) {
				// Get this index's type lineage
				Collection<String> lineage = StateSpec.getInstance()
						.getTypeLineage(anonVariable.getArgTypes()[i]);
				// For every unbound argument, check that the types are in the
				// lineage.
				for (String unboundArg : unboundTypes.keySet()) {
					if (lineage.containsAll(unboundTypes.get(unboundArg))) {
						args[i] = new RelationalArgument(unboundArg);
						recursivelyGroundAnons(args, i + 1, unboundTypes,
								anonVariable, resultPreds);
						args[i] = RelationalArgument.ANONYMOUS;
					}
				}
			}
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
		 * Splits any ranges in a condition within a rule into a number of
		 * smaller uniform sub-ranges.
		 * 
		 * @param baseRule
		 *            The base rule to mutate.
		 * @param isRuleMutant
		 *            If the rule is a mutant or not.
		 * @return A collection of any sub-ranged mutants created from the rule.
		 */
		protected Set<RelationalRule> splitConditionsRanges(
				RelationalRule baseRule, RelationalPredicate condition) {
			Set<RelationalRule> subranges = new HashSet<RelationalRule>();
			if (condition.isNumerical() && !condition.isNegated()) {
				String[] argTypes = condition.getArgTypes();
				for (int i = 0; i < condition.getArguments().length; i++) {
					RelationalArgument arg = condition.getRelationalArguments()[i];
					// If the arg is a variable number
					if (StateSpec.isNumberType(argTypes[i]) && arg.isVariable()) {
						RangeContext context = new RangeContext(i, condition,
								baseRule.getAction());
						// If the arg is a range or represents a range, can
						// split it
						if (arg.isRange(true)) {
							subranges.addAll(splitExistingRange(baseRule,
									condition, i, arg, context));
						} else {
							double[] range = getActionRanges(context,
									localGoal_);
							if (range != null) {
								subranges.addAll(createNewSubRanges(baseRule,
										condition, i, arg.getStringArg(),
										context, range[0] * range[1] < 0));
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
				RelationalArgument[] oldTerms, int i, String goalTerm) {
			RelationalArgument[] newTerms = new RelationalArgument[oldTerms.length];
			for (int j = 0; j < oldTerms.length; j++)
				newTerms[j] = oldTerms[j].clone();
			newTerms[i] = new RelationalArgument(goalTerm);
			Collection<RelationalPredicate> ruleConditions = rule
					.getRawConditions(true);
			List<RelationalPredicate> specConditions = new ArrayList<RelationalPredicate>(
					ruleConditions.size());
			// Form the replacement map
			Map<String, String> replacementMap = new HashMap<String, String>();
			replacementMap.put(oldTerms[i].toString(), newTerms[i].toString());
			for (String gTerm : observedGoalPredicates_.keySet()) {
				replacementMap.put(gTerm, gTerm);
			}

			// Run through the rule conditions, replacing the arguments
			for (RelationalPredicate cond : ruleConditions) {
				// If the condition already contains the term, return null.
				// if (cond.containsArg(goalTerm))
				// return null;
				RelationalPredicate specCond = new RelationalPredicate(cond);
				specCond.replaceArguments(replacementMap, true, false);
				specConditions.add(specCond);
			}

			// Create the mutant
			RelationalRule mutant = new RelationalRule(specConditions,
					new RelationalPredicate(rule.getAction(), newTerms), rule,
					lced_);
			mutant.setQueryParams(rule.getQueryParameters());

			// Check if each condition is a valid goal condition.
			if (!mutant.isLegal())
				return null;

			return mutant;
		}

		/**
		 * Gets the conditions which are always true whenever the goal is.
		 * 
		 * @param actionPred
		 *            The action for getting the conditions.
		 * @return A collection of conditions that are RLGG conds for this
		 *         general goal.
		 */
		public Collection<RelationalPredicate> getGeneralRLGGConds(
				String actionPred) {
			if (localRLGGConditions_ == null
					|| localRLGGConditions_.get(actionPred) == null)
				recreateAllSpecialisations(actionPred);
			return localRLGGConditions_.get(actionPred);
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

			if (localRLGGConditions_ == null)
				localRLGGConditions_ = MultiMap.createSortedSetMultiMap();
			else
				localRLGGConditions_.clear();

			// Add the environment specialisation conditions
			for (RelationalPredicate specialisation : EnvironmentAgentObservations
					.getInstance().getSpecialisationConditions(actionPred)) {
				// If not a general goal, or not the general goal predicate
				if (!(localGoal_ instanceof GeneralGoalCondition && specialisation
						.getFactName().equals(localGoal_.getFactName())))
					specialisationConditions_.put(actionPred, specialisation);
				else if (specialisation.isNegated() != localGoal_.getFact()
						.isNegated())
					localRLGGConditions_.put(actionPred, specialisation);
			}

			if (localGoal_ instanceof SpecificGoalCondition) {
				// Adding specific goal-orientated conditions for specific goal
				// conds
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

			// Set<RelationalArgument> goalActionTerms = new
			// HashSet<RelationalArgument>();
			// for (RelationalArgument actionTerm : actionTerms)
			// if (actionTerm.isGoalVariable())
			// goalActionTerms.add(actionTerm);

			// Add conditions, one-by-one, using both negation and regular
			for (RelationalPredicate condition : actionConditions) {
				// If adding a goal term, be sure it is not an action term.
				// if (!goalActionTerms.isEmpty()) {
				// boolean doNotAdd = false;
				// for (RelationalArgument term : condition
				// .getRelationalArguments())
				// if (goalActionTerms.contains(term)) {
				// doNotAdd = true;
				// break;
				// }
				//
				// if (doNotAdd)
				// continue;
				// }

				specialisations.addAll(groundAnonymousTerms(condition, rule,
						specialisations));
			}

			// Also add the wider specialisations
			if (ProgramArgument.WIDER_SPECIALISATION.booleanValue()) {
				for (RelationalPredicate generalVariant : EnvironmentAgentObservations
						.getInstance().getGeneralSpecialisationConditions()) {
					specialisations.addAll(groundAnonymousTerms(generalVariant,
							rule, specialisations));
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
			RelationalArgument[] oldTerms = rule.getActionTerms();
			// For every goal term
			for (String goalTerm : observedGoalPredicates_.keySet()) {
				boolean termPresent = false;
				// If the old terms already contain the goal term, can't swap
				for (RelationalArgument oldTerm : oldTerms) {
					if (goalTerm.equals(oldTerm.toString())) {
						termPresent = true;
						break;
					}
				}

				if (!termPresent) {
					// For every action term
					for (int i = 0; i < oldTerms.length; i++) {
						// If the term is already a goal term, or the rule
						// already contains the goal term, can't swap
						if (!RelationalArgument.isGoalCondition(oldTerms[i]
								.toString())) {
							RelationalRule swappedRule = swapRuleTerm(rule,
									oldTerms, i, goalTerm);
							if (swappedRule != null)
								mutants.add(swappedRule);
						}
					}
				}
			}

			// Run through each condition
			for (RelationalPredicate condition : rule.getRawConditions(false)) {
				mutants.addAll(splitConditionsRanges(rule, condition));
			}

			return mutants;
		}
	}

	public void cleanup() {
		localAOManager_ = null;
		EnvironmentAgentObservations.clearInstance();
	}
}
