package relationalFramework.agentObservations;

import relationalFramework.GoalCondition;
import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;

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
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.collections.bidimap.DualHashBidiMap;

import cerrla.PolicyGenerator;
import cerrla.ProgramArgument;
import cerrla.Unification;
import cerrla.UnifiedFact;

import jess.Fact;
import util.ConditionComparator;
import util.MultiMap;

/**
 * A class for containing all environmental observations the agent makes while
 * learning. This class is used to help guide the agent's learning process.
 * 
 * @author Sam Sarjant
 */
public final class AgentObservations implements Serializable {
	private static final String ACTION_CONDITIONS_FILE = "actionConditions&Ranges.txt";

	private static final String CONDITION_BELIEF_FILE = "conditionBeliefs.txt";

	/** The AgentObservations instance. */
	private static AgentObservations instance_;

	private static final String SERIALISATION_FILE = "agentObservations.ser";

	private static final long serialVersionUID = -3485610187532540886L;

	/** 2^SETTLED inactive iterations before considered settled. */
	private static final int SETTLED_THRESHOLD = 7;

	/** The agent observations directory. */
	public static final File AGENT_OBSERVATIONS_DIR = new File(
			"agentObservations/");

	/** The action based observations, keyed by action predicate. */
	private Map<String, ActionBasedObservations> actionBasedObservations_;

	/** The condition observations. */
	private ConditionObservations conditionObservations_;

	/** The amount of (2^) inactivity the observations has accrued. */
	private transient int inactivity_ = 0;

	/** The number of steps since last covering the state. */
	private transient int lastCover_ = 0;

	/** The local, goal-orientated observations to serialise separately. */
	private transient LocalAgentObservations localAgentObservations_;

	/** A hash code to track when an observation changes. */
	private transient Integer observationHash_ = null;

	/** A transient group of facts indexed by terms used within. */
	private transient MultiMap<String, RelationalPredicate> termMappedFacts_;

	/**
	 * The constructor for the agent observations.
	 */
	private AgentObservations() {
		conditionObservations_ = new ConditionObservations();
		actionBasedObservations_ = new HashMap<String, ActionBasedObservations>();
		observationHash_ = null;
		inactivity_ = 0;
		lastCover_ = 0;

		AGENT_OBSERVATIONS_DIR.mkdir();
	}

	/**
	 * Gets/Initialises the action based observation for the action given.
	 * 
	 * @param actionPred
	 *            The action predicate.
	 * @return The ActionBasedObservation object assigned to the action or a new
	 *         one.
	 */
	private ActionBasedObservations getActionBasedObservation(String actionPred) {
		ActionBasedObservations abo = actionBasedObservations_.get(actionPred);
		if (abo == null) {
			abo = new ActionBasedObservations();
			actionBasedObservations_.put(actionPred, abo);
		}
		return abo;
	}

	/**
	 * Updates the observation hash code.
	 */
	private void updateHash() {
		if (observationHash_ == null) {
			// Update the hash
			final int prime = 31;
			observationHash_ = 1;

			// Note the condition observations
			observationHash_ = prime * observationHash_
					+ conditionObservations_.hashCode();

			// Note the action observations
			observationHash_ = prime * observationHash_
					+ actionBasedObservations_.hashCode();
		}
	}

	public void clearActionBasedObservations() {
		actionBasedObservations_ = new HashMap<String, ActionBasedObservations>();
	}

	public void clearLocalObservations() {
		localAgentObservations_ = LocalAgentObservations
				.newAgentObservations(localAgentObservations_
						.getLocalGoalName());
	}

	/**
	 * Gathers all relevant facts for a particular action and returns them.
	 * 
	 * @param action
	 *            The action (with arguments).
	 * @param goalTerms
	 *            The terms present in the goal.
	 * @return The relevant facts pertaining to the action.
	 */
	public Collection<RelationalPredicate> gatherActionFacts(
			RelationalPredicate action, Map<String, String> goalReplacements) {
		// Note down action conditions if still unsettled.
		Map<String, String> replacementMap = action
				.createVariableTermReplacementMap(false, false);
		if (goalReplacements != null) {
			goalReplacements = new HashMap<String, String>(goalReplacements);
			goalReplacements.putAll(replacementMap);
		}

		Collection<RelationalPredicate> actionFacts = new HashSet<RelationalPredicate>();
		Set<RelationalPredicate> actionConds = new HashSet<RelationalPredicate>();
		Set<RelationalPredicate> goalActionConds = new HashSet<RelationalPredicate>();
		// Gather facts for each (non-number) action argument
		for (RelationalArgument argument : action.getRelationalArguments()) {
			if (!argument.isNumber()) {
				Collection<RelationalPredicate> termFacts = termMappedFacts_
						.get(argument.toString());
				// Modify the term facts, retaining constants, replacing terms
				for (RelationalPredicate termFact : termFacts) {
					RelationalPredicate notedFact = new RelationalPredicate(
							termFact);

					if (!actionFacts.contains(notedFact))
						actionFacts.add(notedFact);

					// Note the action condition
					RelationalPredicate actionCond = new RelationalPredicate(
							termFact);
					actionCond.replaceArguments(replacementMap, false, true);
					actionConds.add(actionCond);

					// Note the goal action condition
					if (goalReplacements != null) {
						RelationalPredicate goalCond = new RelationalPredicate(
								termFact);
						goalCond.replaceArguments(goalReplacements, false, true);
						if (!actionConds.contains(goalCond))
							goalActionConds.add(goalCond);
					}
				}
			}
		}
		if (!actionConds.isEmpty()
				&& getActionBasedObservation(action.getFactName())
						.addActionConditions(actionConds, goalActionConds,
								action)) {
			inactivity_ = 0;
			observationHash_ = null;
		}

		return actionFacts;
	}

	/**
	 * Gets the maximal bounds of a numerical range using a specific range
	 * context, if such a range exists.
	 * 
	 * @param rangeContext
	 *            The context of the range being selected.
	 * @return The maximal bounds of the range.
	 */
	public double[] getActionRanges(RangeContext rangeContext) {
		if (rangeContext.getAction() != null) {
			// Get the range from the actions
			return actionBasedObservations_.get(rangeContext.getAction())
					.getActionRange(rangeContext);
		} else {
			// Get the range from the condition beliefs.
			return conditionObservations_.conditionRanges_.get(rangeContext);
		}
	}

	public Map<String, ConditionBeliefs> getConditionBeliefs() {
		return conditionObservations_.conditionBeliefs_;
	}

	public Collection<String> getGeneralInvariants() {
		return conditionObservations_.invariants_.getGeneralInvariants();
	}

	public MultiMap<String, RelationalPredicate> getGoalPredicateMap() {
		return localAgentObservations_.getGoalPredicateMap();
	}

	public Collection<BackgroundKnowledge> getLearnedBackgroundKnowledge() {
		return conditionObservations_.learnedEnvironmentRules_;
	}

	public String getLocalGoalName() {
		return localAgentObservations_.getLocalGoalName();
	}

	public Collection<GoalCondition> getLocalSpecificGoalConditions() {
		return localAgentObservations_.getSpecificGoalConditions();
	}

	public Map<String, Map<IntegerArray, ConditionBeliefs>> getNegatedConditionBeliefs() {
		return conditionObservations_.negatedConditionBeliefs_;
	}

	public Collection<String> getNeverSeenInvariants() {
		return conditionObservations_.invariants_.getNeverSeenPredicates();
	}

	public int getNumGoalArgs() {
		return localAgentObservations_.getNumGoalArgs();
	}

	/**
	 * Gets the number of specialisations an action can have.
	 * 
	 * @param actionPredicate
	 *            The action predicate to check.
	 * @return The total possible number of specialisations the action can make.
	 */
	public int getNumSpecialisations(String action) {
		Collection<RelationalPredicate> specConditions = getSpecialisationConditions(action);
		int num = 0;
		if (specConditions != null)
			num = specConditions.size();
		// Also, if the action has numerical arguments, add them to the count
		// too
		for (String type : StateSpec.getInstance().getPredicateByName(action)
				.getArgTypes()) {
			if (StateSpec.isNumberType(type))
				num += ProgramArgument.NUM_NUMERICAL_SPLITS.doubleValue();
		}
		return num;
	}

	public int getObservationHash() {
		updateHash();
		return observationHash_;
	}

	public Collection<GoalArg> getPossibleGoalArgs() {
		return localAgentObservations_.getObservedGoalArgs();
	}

	/**
	 * Gets the RLGG rules found through the action observations. This is found
	 * by observing the invariants in the actions.
	 * 
	 * @param queryParams
	 *            The query parameters for the current goal (if any).
	 * @return The RLGGs for every action.
	 */
	public List<RelationalRule> getRLGGActionRules(List<String> queryParams) {
		List<RelationalRule> rlggRules = new ArrayList<RelationalRule>();
		for (ActionBasedObservations abo : actionBasedObservations_.values()) {
			RelationalRule rlggRule = abo.getRLGGRule();
			rlggRule.setQueryParams(queryParams);
			rlggRules.add(rlggRule);
		}
		return rlggRules;
	}

	/**
	 * Gets the RLGG conditions for a given action.
	 * 
	 * @param ruleAction
	 *            The action to get the RLGG conditions for. Note the action
	 *            arguments.
	 * @return The conditions for that action's RLGG.
	 */
	public Collection<RelationalPredicate> getRLGGConditions(
			RelationalPredicate action) {
		SortedSet<RelationalPredicate> rlggConds = actionBasedObservations_
				.get(action.getFactName()).getRLGGRule().getConditions(true);
		Collection<RelationalPredicate> termSwappedConds = new TreeSet<RelationalPredicate>(
				rlggConds.comparator());
		String[] actionTerms = action.getArguments();
		for (RelationalPredicate rlggCond : rlggConds) {
			rlggCond = new RelationalPredicate(rlggCond);
			rlggCond.replaceArguments(actionTerms);
			termSwappedConds.add(rlggCond);
		}
		return termSwappedConds;
	}

	public Collection<RelationalPredicate> getSpecialisationConditions(
			String actionPred) {
		if (!actionBasedObservations_.containsKey(actionPred))
			return null;
		return actionBasedObservations_.get(actionPred)
				.getSpecialisationConditions();
	}

	public Collection<RelationalPredicate> getSpecificInvariants() {
		return conditionObservations_.invariants_.getSpecificInvariants();
	}

	public Collection<RelationalPredicate> getUnseenPredicates() {
		return conditionObservations_.unseenPreds_;
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
	public boolean isCoveringNeeded(MultiMap<String, String[]> validActions,
			MultiMap<String, String[]> activatedActions) {
		for (String action : validActions.keySet()) {
			// If the activated actions don't even contain the key, return
			// true.
			if (!activatedActions.containsKey(action)) {
				resetInactivity();
				return true;
			}

			// Check each set of actions match up
			if (!activatedActions.get(action).containsAll(
					(validActions.get(action)))) {
				resetInactivity();
				return true;
			}
		}

		// Also, cover every X episodes, checking more and more
		// infrequently if no changes occur.
		if (lastCover_ >= (Math.pow(2, inactivity_) - 1)) {
			lastCover_ = 0;
			return true;
		}
		lastCover_++;

		return false;
	}

	/**
	 * If the agent observations are basically settled (have not changed in X
	 * iterations).
	 * 
	 * @return
	 */
	public boolean isSettled() {
		return inactivity_ >= SETTLED_THRESHOLD;
	}

	/**
	 * Notes the arguments given for the goal.
	 * 
	 * @param stateGoals
	 *            The goal arguments for the state.
	 */
	public void noteGoalArgs(Collection<GoalArg> stateGoals) {
		localAgentObservations_.noteGoalArgs(stateGoals);
	}

	/**
	 * Notes a single argument for the goal.
	 * 
	 * @param goalArg
	 *            The goal argument.
	 */
	public void noteGoalArgs(GoalArg goalArg) {
		Collection<GoalArg> singleArg = new ArrayList<GoalArg>(1);
		singleArg.add(goalArg);
		localAgentObservations_.noteGoalArgs(singleArg);
	}

	public boolean removeUnseenPredicates(
			Collection<RelationalPredicate> removables) {
		return conditionObservations_.unseenPreds_.removeAll(removables);
	}

	public void resetInactivity() {
		inactivity_ = 0;
		for (ActionBasedObservations abo : actionBasedObservations_.values()) {
			abo.recreateRLGG_ = true;
		}
	}

	/**
	 * Saves agent observations to file in the agent observations directory.
	 * 
	 * @param localGenerator
	 *            The local policy generator.
	 */
	public void saveAgentObservations(PolicyGenerator localGenerator) {
		try {
			// Make the environment directory if necessary
			File environmentDir = new File(AGENT_OBSERVATIONS_DIR, StateSpec
					.getInstance().getEnvironmentName() + File.separatorChar);
			environmentDir.mkdir();
			File localEnvironmentDir = new File(environmentDir,
					localGenerator.getLocalGoal() + File.separatorChar);
			localEnvironmentDir.mkdir();

			// Condition beliefs
			conditionObservations_.saveConditionBeliefs(environmentDir);

			// Local condition beliefs
			localAgentObservations_.saveLocalObservations(localEnvironmentDir);

			// Global action Conditions and ranges
			File actionCondsFile = new File(environmentDir,
					ACTION_CONDITIONS_FILE);
			actionCondsFile.createNewFile();
			FileWriter wr = new FileWriter(actionCondsFile);
			BufferedWriter buf = new BufferedWriter(wr);

			File localActionCondsFile = new File(localEnvironmentDir,
					ACTION_CONDITIONS_FILE);
			localActionCondsFile.createNewFile();
			FileWriter localWR = new FileWriter(localActionCondsFile);
			BufferedWriter localBuf = new BufferedWriter(localWR);

			// Write action conditions and ranges
			for (ActionBasedObservations abo : actionBasedObservations_
					.values()) {
				abo.saveActionBasedObservations(buf);
				abo.saveLocalActionBasedObservations(localBuf);
			}

			buf.close();
			wr.close();

			localBuf.close();
			localWR.close();

			// Serialise observations
			File serialisedFile = new File(environmentDir, SERIALISATION_FILE);
			serialisedFile.createNewFile();
			FileOutputStream fos = new FileOutputStream(serialisedFile);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this);

			oos.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * A method for scanning the state and assigning facts to term based maps.
	 * During the scan, condition inter-relations are noted (is not yet
	 * settled).
	 * 
	 * @param state
	 *            The state in raw fact form.
	 * @param goalReplacements
	 *            The variable replacements for the goal terms.
	 * @return The mapping of terms to facts.
	 */
	public void scanState(Collection<Fact> state,
			Map<String, String> goalReplacements) {
		// Run through the facts, adding to term mapped facts and adding the raw
		// facts for condition belief scanning.
		Collection<RelationalPredicate> stateFacts = new ArrayList<RelationalPredicate>();
		Collection<String> generalStateFacts = new HashSet<String>();
		termMappedFacts_ = MultiMap.createSortedSetMultiMap();
		for (Fact stateFact : state) {
			RelationalPredicate strFact = null;
			strFact = StateSpec.toRelationalPredicate(stateFact.toString());

			// Ignore the type, goal, inequal and actions pred
			if (strFact != null) {
				if (StateSpec.getInstance().isUsefulPredicate(
						strFact.getFactName())) {
					stateFacts.add(strFact);
					generalStateFacts.add(strFact.getFactName());

					// Run through the arguments and index the fact by term
					for (RelationalArgument arg : strFact
							.getRelationalArguments()) {
						// Ignore numerical terms
						if (!arg.isNumber())
							termMappedFacts_.putContains(arg.toString(),
									strFact);
					}
				}
			}
		}

		// Note the condition mappings.
		if (conditionObservations_.noteObservations(stateFacts,
				generalStateFacts, goalReplacements)) {
			observationHash_ = null;
			inactivity_ = 0;
		} else
			inactivity_++;
	}

	/**
	 * Checks if the agent should search all goal args this step based on local
	 * inactivity.
	 * 
	 * @return True if the agent should check all goal arguments.
	 */
	public boolean searchAllGoalArgs() {
		return localAgentObservations_.searchAllGoalArgs();
	}

	public void setActionConditions(String action,
			Collection<RelationalPredicate> conditions) {
		getActionBasedObservation(action).setActionConditions(conditions);
		observationHash_ = null;
		inactivity_ = 0;
	}

	public void setBackgroundKnowledge(SortedSet<BackgroundKnowledge> backKnow) {
		conditionObservations_.learnedEnvironmentRules_ = backKnow;
	}

	public void setNumGoalArgs(int num) {
		localAgentObservations_.setNumGoalArgs(num);
	}

	/**
	 * Simplifies a set of facts using the learned background knowledge and
	 * invariants.
	 * 
	 * @param simplified
	 *            The facts to be simplified.
	 * @param exitIfIllegalRule
	 *            If the procedure is exits if rule is illegal.
	 * @param onlyEquivalencies
	 *            If only equivalent rules should be tested.
	 * @return 1 if the condition was simplified, 0 if no change, -1 if illegal
	 *         rule (and exiting with illegal rules).
	 */
	public int simplifyRule(SortedSet<RelationalPredicate> simplified,
			boolean exitIfIllegalRule, boolean onlyEquivalencies) {
		// Simplify using background knowledge
		int simplResult = conditionObservations_.simplifyRule(simplified,
				exitIfIllegalRule, onlyEquivalencies);
		if (simplResult == -1 && exitIfIllegalRule)
			return -1;
		boolean result = (simplResult != 0);

		// Simplify using invariants
		if (!onlyEquivalencies) {
			result |= simplified.removeAll(conditionObservations_.invariants_
					.getSpecificInvariants());
			result |= simplified.removeAll(localAgentObservations_
					.getConditionInvariants().getSpecificInvariants());
		}
		return (result) ? 1 : 0;
	}

	/**
	 * Converts a StringFact into an integer array representing the structure of
	 * the fact.
	 * 
	 * @param fact
	 *            The fact to convert.
	 * @return An IntegerArray representing the structure of the fact.
	 */
	protected static IntegerArray determineArgState(RelationalPredicate fact) {
		String[] args = fact.getArguments();
		int[] structure = new int[args.length];
		Map<String, Integer> seenArgs = new HashMap<String, Integer>();
		for (int i = 0; i < args.length; i++) {
			if (!args[i].equals("?")) {
				if (!seenArgs.containsKey(args[i]))
					seenArgs.put(args[i], seenArgs.size() + 1);
				structure[i] = seenArgs.get(args[i]);
			}
		}
		return new IntegerArray(structure);
	}

	/**
	 * Gets the AgentObservations instance.
	 * 
	 * @return The instance.
	 */
	public static AgentObservations getInstance() {
		// if (instance_ == null)
		// instance_ = new AgentObservations();
		return instance_;
	}

	/**
	 * Loads an AgentObservation serialised class from file, or if no file
	 * available, creates a new one.
	 * 
	 * @param localGoal
	 *            The local goal observations to load.
	 * @return True if local observations were also loaded.
	 */
	public static boolean loadAgentObservations(String localGoal) {
		try {
			File globalObsFile = new File(AGENT_OBSERVATIONS_DIR, StateSpec
					.getInstance().getEnvironmentName()
					+ File.separatorChar
					+ SERIALISATION_FILE);
			if (globalObsFile.exists()) {
				FileInputStream fis = new FileInputStream(globalObsFile);
				ObjectInputStream ois = new ObjectInputStream(fis);
				AgentObservations ao = (AgentObservations) ois.readObject();
				if (ao != null) {
					instance_ = ao;

					// Attempt to load the local observations pertaining to the
					// goal at hand.
					instance_.localAgentObservations_ = LocalAgentObservations
							.loadAgentObservations(localGoal);

					return instance_.localAgentObservations_.isLoaded();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		instance_ = newInstance(localGoal);
		return false;
	}

	/**
	 * Gets the AgentObservations instance.
	 * 
	 * @param localGoal
	 *            The local goal for local observations.
	 * @return The instance.
	 */
	public static AgentObservations newInstance(String localGoal) {
		instance_ = new AgentObservations();
		instance_.localAgentObservations_ = LocalAgentObservations
				.newAgentObservations(localGoal);
		return instance_;
	}

	/**
	 * An internal class to note the action-based observations.
	 * 
	 * @author Sam Sarjant
	 */
	private class ActionBasedObservations implements Serializable {
		private static final long serialVersionUID = -2176309655989637017L;

		/**
		 * The action itself, usually expressed in variables, but possibly
		 * constants if the action always takes a constant.
		 */
		private RelationalPredicate action_;

		/**
		 * Mapped by pair: fact name and range variable, observed maximal ranges
		 * for the conditions seen in the action.
		 */
		private Map<RangeContext, double[]> actionRanges_;

		/** The conditions observed to always be true for the action. */
		private Collection<RelationalPredicate> invariantActionConditions_;

		/** If the RLGG rule needs to be recreated. */
		private boolean recreateRLGG_ = true;

		/**
		 * The learned RLGG rule from the action observations. Can be recreated
		 * upon deserialisation so it is placed in the correct slot.
		 */
		private transient RelationalRule rlggRule_;

		/**
		 * The conditions that can be added to rules for specialisation.
		 * Essentially the variant action conditions both negated and normal,
		 * then simplified.
		 */
		private transient Collection<RelationalPredicate> specialisationConditions_;

		/** The conditions observed to sometimes be true for the action. */
		private Collection<RelationalPredicate> variantActionConditions_;

		private AgentObservations getOuterType() {
			return AgentObservations.this;
		}

		/**
		 * Gets the maximum range observed so far for a given context.
		 * 
		 * @param rangeContext
		 *            The context in which the range is.
		 * @return The observed range for a given context or null.
		 */
		public double[] getActionRange(RangeContext rangeContext) {
			return actionRanges_.get(rangeContext);
		}

		/**
		 * Intersects the action conditions sets, handling numerical values as a
		 * special case.
		 * 
		 * @param actionConds
		 *            The action conditions being added.
		 * @param actionArgs
		 *            The arguments for the actions.
		 * @param invariants
		 *            Invariant action conditions. Can only get smaller.
		 * @param variants
		 *            Variant action conditions. Can only get bigger.
		 * @param actionRanges
		 *            The observed ranges for various conditions.
		 * @return True if the action conditions changed, false otherwise.
		 */
		private boolean intersectActionConditions(
				Collection<RelationalPredicate> actionConds,
				RelationalArgument[] actionArgs,
				Collection<RelationalPredicate> invariants,
				Collection<RelationalPredicate> variants,
				Map<RangeContext, double[]> actionRanges) {
			boolean changed = false;
			Collection<RelationalPredicate> newInvariantConditions = new HashSet<RelationalPredicate>();

			// Run through each invariant fact
			for (RelationalPredicate invFact : invariants) {
				// Merge any numerical ranges
				Collection<UnifiedFact> mergedFacts = Unification.getInstance()
						.unifyFact(invFact, actionConds, null, actionArgs,
								false, true);
				if (mergedFacts != null && !mergedFacts.isEmpty()) {
					for (UnifiedFact uf : mergedFacts) {
						RelationalPredicate unifiedFact = uf.getResultFact();
						changed |= !invFact.equals(unifiedFact);
						newInvariantConditions.add(unifiedFact);
						actionConds.remove(uf.getUnityFact());
						System.arraycopy(uf.getFactTerms(), 0, actionArgs, 0,
								actionArgs.length);

						noteRange(unifiedFact, actionRanges);
					}
				} else {
					variants.add(invFact);
					changed = true;
				}
			}
			invariants.clear();
			invariants.addAll(newInvariantConditions);

			// Add any remaining action conds to the variants, merging any
			// numerical ranges together
			Collection<RelationalPredicate> newVariantConditions = new HashSet<RelationalPredicate>();
			for (RelationalPredicate variant : variants) {
				Collection<UnifiedFact> mergedFacts = Unification.getInstance()
						.unifyFact(variant, actionConds, new DualHashBidiMap(),
								actionArgs, false, true);
				if (mergedFacts != null && !mergedFacts.isEmpty()) {
					for (UnifiedFact uf : mergedFacts) {
						RelationalPredicate unifiedFact = uf.getResultFact();
						changed |= !variant.equals(unifiedFact);
						newVariantConditions.add(unifiedFact);
						actionConds.remove(uf.getUnityFact());

						noteRange(unifiedFact, actionRanges);
					}
				} else
					newVariantConditions.add(variant);
			}
			variants.clear();
			variants.addAll(newVariantConditions);
			changed |= variants.addAll(actionConds);

			return changed;
		}

		/**
		 * Notes the range from a given fact.
		 * 
		 * @param numberFact
		 *            The range fact.
		 * @param actionRanges
		 *            The map of facts, mapped by factName-range variable.
		 */
		private void noteRange(RelationalPredicate numberFact,
				Map<RangeContext, double[]> actionRanges) {
			if (actionRanges == null || !numberFact.isNumerical())
				return;
			RelationalArgument[] factArgs = numberFact.getRelationalArguments();
			for (int i = 0; i < factArgs.length; i++) {
				// Only note ranges
				if (factArgs[i].isRange()) {
					RangeContext key = new RangeContext(i, numberFact,
							action_.getFactName());
					double[] range = actionRanges.get(key);
					if (range == null) {
						range = new double[2];
						actionRanges.put(key, range);
					}
					System.arraycopy(factArgs[i].getExplicitRange(), 0, range,
							0, 2);
				}
			}
		}

		/**
		 * Recreates the specialisations from both the variant action conditions
		 * and the goal variant action conditions.
		 * 
		 * @param action
		 *            The action to create the conditions for.
		 */
		private void recreateAllSpecialisations(String action) {
			specialisationConditions_ = recreateSpecialisations(
					variantActionConditions_, true);
			if (localAgentObservations_.getVariantGoalActionConditions(action) != null)
				specialisationConditions_
						.addAll(recreateSpecialisations(localAgentObservations_
								.getVariantGoalActionConditions(action), false));
		}

		/**
		 * Recreates the set of specialisation conditions, which are basically
		 * the variant conditions, both negated and normal, and simplified to
		 * exclude the invariant and illegal conditions.
		 * 
		 * @param variants
		 *            The variant conditions to simplify into a smaller subset.
		 * @param checkNegated
		 *            If adding negated versions of the variant too.
		 * @return The collection of specialisation conditions, not containing
		 *         any conditions in the invariants, and not containing any
		 *         conditions not in either invariants or variants.
		 */
		private Collection<RelationalPredicate> recreateSpecialisations(
				Collection<RelationalPredicate> variants, boolean checkNegated) {
			SortedSet<RelationalPredicate> specialisations = new TreeSet<RelationalPredicate>(
					ConditionComparator.getInstance());
			for (RelationalPredicate condition : variants) {
				// Check the non-negated version
				condition = simplifyCondition(condition);
				if (condition != null && specialisations.add(condition)) {
					// Check the negated version (only for non-types)
					if (checkNegated
							&& !StateSpec.getInstance().isTypePredicate(
									condition.getFactName())) {
						String[] negArgs = new String[condition.getArgTypes().length];
						// Special case for numerical values - negated
						// numericals are made anonymous
						for (int i = 0; i < condition.getArgTypes().length; i++) {
							if (StateSpec
									.isNumberType(condition.getArgTypes()[i]))
								negArgs[i] = "?";
							else
								negArgs[i] = condition.getArguments()[i];
						}

						RelationalPredicate negCondition = new RelationalPredicate(
								condition, negArgs);
						negCondition.swapNegated();
						negCondition = simplifyCondition(negCondition);
						if (negCondition != null)
							specialisations.add(negCondition);
					}
				}
			}

			return specialisations;
		}

		/**
		 * Simplifies a single condition using equivalence rules.
		 * 
		 * @param condition
		 *            The condition to simplify.
		 * @return The simplified condition (or the condition itself).
		 */
		private RelationalPredicate simplifyCondition(
				RelationalPredicate condition) {
			SortedSet<RelationalPredicate> set = new TreeSet<RelationalPredicate>(
					ConditionComparator.getInstance());
			set.add(condition);
			if (simplifyRule(set, false, true) == 1) {
				RelationalPredicate simplify = set.first();
				// If the condition is not in the invariants and if it's not
				// negated, IS in the variants, keep it.
				if (!invariantActionConditions_.contains(simplify)
						&& !localAgentObservations_.invariantActionsContains(
								action_.getFactName(), simplify)) {
					if (simplify.isNegated()
							|| variantActionConditions_.contains(simplify)
							|| localAgentObservations_.variantActionsContains(
									action_.getFactName(), simplify))
						return simplify;
				}
			} else
				return condition;
			return null;
		}

		protected void saveActionBasedObservations(BufferedWriter buf)
				throws Exception {
			buf.write(action_.getFactName() + "\n");
			SortedSet<RelationalPredicate> rlggConds = new TreeSet<RelationalPredicate>(
					ConditionComparator.getInstance());
			rlggConds.addAll(invariantActionConditions_);
			buf.write("RLGG conditions: " + rlggConds + "\n");
			buf.write("Global conditions: "
					+ recreateSpecialisations(variantActionConditions_, true)
					+ "\n");
			buf.write("Observed ranges: [");
			boolean first = true;
			for (RangeContext rc : actionRanges_.keySet()) {
				if (!first)
					buf.write(", ");
				buf.write(rc.toString(actionRanges_.get(rc)));
				first = false;
			}
			buf.write("]\n\n");
		}

		protected void saveLocalActionBasedObservations(BufferedWriter localBuf)
				throws Exception {
			localBuf.write(action_.getFactName() + "\n");
			localBuf.write("Local conditions: "
					+ recreateSpecialisations(localAgentObservations_
							.getVariantGoalActionConditions(action_
									.getFactName()), false) + "\n");
			localBuf.write("\n");
		}

		/**
		 * Adds action conditions to the action based observations.
		 * 
		 * @param actionConds
		 *            The action conditions being added.
		 * @param goalActionConds
		 *            The action conditions including the goal terms.
		 * @param action
		 *            The action which added the conditions.
		 * @return True if the set of action conditions changed because of the
		 *         addition.
		 */
		public boolean addActionConditions(
				Collection<RelationalPredicate> actionConds,
				Collection<RelationalPredicate> goalActionConds,
				RelationalPredicate action) {
			boolean changed = false;

			// If the goal conditions haven't been initialised, they start as
			// invariant.
			changed = localAgentObservations_.initLocalActionConds(
					action.getFactName(), goalActionConds);

			// If this is the first action condition, then all the actions are
			// considered invariant.
			if (invariantActionConditions_ == null) {
				invariantActionConditions_ = new HashSet<RelationalPredicate>(
						actionConds);
				variantActionConditions_ = new HashSet<RelationalPredicate>();
				specialisationConditions_ = new HashSet<RelationalPredicate>();
				action_ = new RelationalPredicate(action);
				recreateRLGG_ = true;
				actionRanges_ = new HashMap<RangeContext, double[]>();
				return true;
			}

			// Generalise the action if necessary
			RelationalArgument[] actionArgs = action_.getRelationalArguments();
			for (int i = 0; i < actionArgs.length; i++) {
				RelationalArgument argument = actionArgs[i];

				// If the action isn't variable, but doesn't match with the
				// current action, generalise it.
				if (!argument.isVariable()
						&& !argument.isNumber()
						&& (!argument
								.equals(action.getRelationalArguments()[i]))) {
					actionArgs[i] = RelationalArgument.getVariableTermArg(i);
					changed = true;
				}
			}

			// Sort the invariant and variant conditions, making a special case
			// for numerical conditions.
			changed |= intersectActionConditions(actionConds, actionArgs,
					invariantActionConditions_, variantActionConditions_,
					actionRanges_);
			Collection<RelationalPredicate> localInvariants = localAgentObservations_
					.getInvariantGoalActionConditions(action.getFactName());
			Collection<RelationalPredicate> localVariants = localAgentObservations_
					.getVariantGoalActionConditions(action.getFactName());
			changed |= intersectActionConditions(goalActionConds, actionArgs,
					localInvariants, localVariants, null);

			action_ = new RelationalPredicate(action_, actionArgs);

			if (changed) {
				recreateAllSpecialisations(action.getFactName());
				recreateRLGG_ = true;
				return true;
			}
			return false;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ActionBasedObservations other = (ActionBasedObservations) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (variantActionConditions_ == null) {
				if (other.variantActionConditions_ != null)
					return false;
			} else if (!variantActionConditions_
					.equals(other.variantActionConditions_))
				return false;
			return true;
		}

		/**
		 * Gets the RLGG rule from the observed invariant conditions.
		 * 
		 * @return The RLGG rule created using the invariants observed.
		 */
		public RelationalRule getRLGGRule() {
			// No need to recreate the rule if nothing has changed.
			if (recreateRLGG_ || rlggRule_ == null) {
				SortedSet<RelationalPredicate> ruleConds = new TreeSet<RelationalPredicate>(
						ConditionComparator.getInstance());
				// Form the replacements if the action is non-variable
				String[] replacements = null;
				for (String arg : action_.getArguments()) {
					if (arg.charAt(0) != '?') {
						replacements = action_.getArguments();
						break;
					}
				}

				for (RelationalPredicate invariant : invariantActionConditions_) {
					RelationalPredicate modFact = new RelationalPredicate(
							invariant);
					if (replacements != null)
						modFact.replaceArguments(replacements);
					modFact.clearRanges();
					ruleConds.add(modFact);
				}

				// Simplify the rule conditions
				simplifyRule(ruleConds, false, false);
				if (rlggRule_ == null)
					rlggRule_ = new RelationalRule(ruleConds, action_, null);
				else {
					rlggRule_.setConditions(ruleConds, false);
					rlggRule_.setActionTerms(action_.getArguments());
				}

				rlggRule_.expandConditions();
				recreateRLGG_ = false;
			}

			rlggRule_.incrementStatesCovered();
			return rlggRule_;
		}

		public Collection<RelationalPredicate> getSpecialisationConditions() {
			if (specialisationConditions_ == null)
				recreateAllSpecialisations(action_.getFactName());
			return specialisationConditions_;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime
					* result
					+ ((variantActionConditions_ == null) ? 0
							: variantActionConditions_.hashCode());
			return result;
		}

		public void setActionConditions(
				Collection<RelationalPredicate> conditions) {
			specialisationConditions_ = new HashSet<RelationalPredicate>(
					conditions);
		}

		@Override
		public String toString() {
			return action_.getFactName();
		}
	}

	/**
	 * A serializable collection of observations regarding conditions within an
	 * environment.
	 * 
	 * @author Sam Sarjant
	 * 
	 */
	public class ConditionObservations implements Serializable {
		private static final long serialVersionUID = -3548961174011793059L;

		/** The agent's beliefs about the condition inter-relations. */
		private Map<String, ConditionBeliefs> conditionBeliefs_;

		/**
		 * A map (by fact name) of various conditions which note the maximum
		 * bounds of observed ranges.
		 */
		private Map<RangeContext, double[]> conditionRanges_;

		/** The observed invariants of the environment. */
		private InvariantObservations invariants_;

		/** The rules about the environment learned by the agent. */
		private SortedSet<BackgroundKnowledge> learnedEnvironmentRules_;

		/**
		 * The same rules organised into a mapping based on what predicates are
		 * present in the rule.
		 */
		private MultiMap<String, BackgroundKnowledge> mappedEnvironmentRules_;

		/**
		 * The agent's beliefs about the negated condition inter-relations (when
		 * conditions AREN'T true), ordered using a second mapping of argument
		 * index
		 */
		private Map<String, Map<IntegerArray, ConditionBeliefs>> negatedConditionBeliefs_;

		/** The collection of unseen predicates. */
		private Collection<RelationalPredicate> unseenPreds_;

		public ConditionObservations() {
			conditionBeliefs_ = new TreeMap<String, ConditionBeliefs>();
			negatedConditionBeliefs_ = new TreeMap<String, Map<IntegerArray, ConditionBeliefs>>();
			learnedEnvironmentRules_ = formBackgroundKnowledge();
			invariants_ = new InvariantObservations();
			conditionRanges_ = new HashMap<RangeContext, double[]>();

			unseenPreds_ = new HashSet<RelationalPredicate>();
			unseenPreds_.addAll(StateSpec.getInstance().getPredicates()
					.values());
			unseenPreds_.addAll(StateSpec.getInstance().getTypePredicates()
					.values());
		}

		/**
		 * Forms the background knowledge from the condition beliefs.
		 */
		private SortedSet<BackgroundKnowledge> formBackgroundKnowledge() {
			SortedSet<BackgroundKnowledge> backgroundKnowledge = new TreeSet<BackgroundKnowledge>();

			// Run through every condition in the beliefs
			// Form equivalence (<=>) relations wherever possible
			NonRedundantBackgroundKnowledge currentKnowledge = new NonRedundantBackgroundKnowledge();
			for (String cond : conditionBeliefs_.keySet()) {
				ConditionBeliefs cb = conditionBeliefs_.get(cond);

				cb.createRelationRules(conditionBeliefs_,
						negatedConditionBeliefs_, currentKnowledge);
			}

			// Create the negated condition beliefs (only for the !A => B
			// relations)
			for (String cond : negatedConditionBeliefs_.keySet()) {
				for (ConditionBeliefs negCB : negatedConditionBeliefs_
						.get(cond).values()) {
					negCB.createRelationRules(conditionBeliefs_,
							negatedConditionBeliefs_, currentKnowledge);
				}
			}
			backgroundKnowledge.addAll(currentKnowledge
					.getAllBackgroundKnowledge());
			mappedEnvironmentRules_ = MultiMap.createSortedSetMultiMap();
			mappedEnvironmentRules_.putAll(currentKnowledge
					.getPredicateMappedRules());
			return backgroundKnowledge;
		}

		/**
		 * Records all conditions associations from the current state using the
		 * term mapped facts to form the relations.
		 * 
		 * @param stateFacts
		 *            The facts of the state in StringFact form.
		 * @param goalReplacements2
		 *            The variable replacements for the goal terms.
		 * @return True if the condition beliefs changed at all.
		 */
		private boolean recordConditionAssociations(
				Collection<RelationalPredicate> stateFacts,
				Map<String, String> goalReplacements) {
			boolean changed = false;
			for (RelationalPredicate baseFact : stateFacts) {
				// Getting the ConditionBeliefs object
				ConditionBeliefs cb = conditionBeliefs_.get(baseFact
						.getFactName());
				if (cb == null) {
					cb = new ConditionBeliefs(baseFact.getFactName());
					conditionBeliefs_.put(baseFact.getFactName(), cb);
					observationHash_ = null;
				}

				// Create a replacement map here (excluding numerical
				// values)
				Map<String, String> replacementMap = baseFact
						.createVariableTermReplacementMap(true, false);

				// Replace facts for all relevant facts and store as
				// condition
				// beliefs.
				Collection<RelationalPredicate> relativeFacts = new HashSet<RelationalPredicate>();
				for (String term : baseFact.getArguments()) {
					Collection<RelationalPredicate> termFacts = termMappedFacts_
							.get(term);
					if (termFacts != null) {
						for (RelationalPredicate termFact : termFacts) {
							if (!termFact.isNumerical()) {
								RelationalPredicate relativeFact = new RelationalPredicate(
										termFact);
								relativeFact.replaceArguments(replacementMap,
										false, false);
								relativeFacts.add(relativeFact);
							}
						}
					}

					// Note any goal terms
					if (goalReplacements != null
							&& goalReplacements.containsKey(term)) {
						// Probably need to replace the term here for a
						// parameterisable goal term
						RelationalPredicate goalFact = new RelationalPredicate(
								baseFact);
						goalFact.replaceArguments(goalReplacements, false,
								false);
						changed |= localAgentObservations_.addGoalFact(
								goalReplacements.get(term), goalFact);
						// Negated fact too (if not numerical)
						if (!baseFact.isNumerical()) {
							RelationalPredicate negFact = new RelationalPredicate(
									goalFact);
							negFact.swapNegated();
							changed |= localAgentObservations_.addGoalFact(
									goalReplacements.get(term), negFact);
						}
					}
				}

				// TODO Can use these rules to note the condition ranges
				// So when condX is true, condY is always in range 0-50
				// Works for self conditions too, when cond X is true, condX
				// is in range min-max

				// Note the relative facts for the main pred and for every
				// negated pred not present
				Collection<RelationalPredicate> notRelativeFacts = new HashSet<RelationalPredicate>();
				if (cb.noteTrueRelativeFacts(relativeFacts, notRelativeFacts,
						true)) {
					changed = true;
					observationHash_ = null;
				}

				// Form the condition beliefs for the not relative facts,
				// using the relative facts as always true values (only for
				// non-types predicates)
				changed |= recordUntrueConditionAssociations(relativeFacts,
						notRelativeFacts);

				// Note the ranges of the condition if the fact is numerical

				if (baseFact.isNumerical()) {
					RelationalArgument[] baseArgs = baseFact
							.getRelationalArguments();
					for (int i = 0; i < baseArgs.length; i++) {
						if (baseArgs[i].isNumber()) {
							RangeContext context = new RangeContext(i, baseFact);

							double[] range = conditionRanges_.get(context);
							double baseVal = baseArgs[i].getExplicitRange()[0];
							if (range == null) {
								// A new range
								range = new double[2];
								range[0] = range[1] = baseVal;
								conditionRanges_.put(context, range);
							} else {
								// Unify existing range
								Unification.getInstance().unifyRange(range,
										baseVal);
							}
						}
					}
				}
			}

			if (changed) {
				learnedEnvironmentRules_ = formBackgroundKnowledge();
			}
			return changed;
		}

		/**
		 * Notes the condition associations for all conditions that are untrue
		 * for a particular term.
		 * 
		 * @param trueRelativeFacts
		 *            The currently true relative facts.
		 * @param falseRelativeFacts
		 *            The currently false relative facts.
		 * @return True if the condition beliefs changed.
		 */
		private boolean recordUntrueConditionAssociations(
				Collection<RelationalPredicate> trueRelativeFacts,
				Collection<RelationalPredicate> falseRelativeFacts) {
			boolean changed = false;
			for (RelationalPredicate untrueFact : falseRelativeFacts) {
				untrueFact = new RelationalPredicate(untrueFact);
				String factName = untrueFact.getFactName();

				// Getting the ConditionBeliefs object
				Map<IntegerArray, ConditionBeliefs> untrueCBs = negatedConditionBeliefs_
						.get(factName);
				if (untrueCBs == null) {
					untrueCBs = new HashMap<IntegerArray, ConditionBeliefs>();
					negatedConditionBeliefs_.put(factName, untrueCBs);
				}

				// Create a separate condition beliefs object for each
				// non-anonymous argument position
				String[] untrueFactArgs = untrueFact.getArguments();
				// Form the replacement map, modifying the args if necessary
				// (such that ?X is first arg, ?Y second, etc).
				Map<String, String> replacementMap = new HashMap<String, String>();
				for (int i = 0; i < untrueFactArgs.length; i++) {
					if (!untrueFactArgs[i].equals("?")) {
						if (!replacementMap.containsKey(untrueFactArgs[i]))
							replacementMap.put(untrueFactArgs[i],
									RelationalArgument.getVariableTermArg(i)
											.toString());
						untrueFactArgs[i] = replacementMap
								.get(untrueFactArgs[i]);
					}
				}

				IntegerArray argState = determineArgState(untrueFact);
				ConditionBeliefs untrueCB = untrueCBs.get(argState);
				if (untrueCB == null) {
					untrueCB = new ConditionBeliefs(factName, untrueFactArgs);
					untrueCBs.put(argState, untrueCB);
					observationHash_ = null;
				}

				Collection<RelationalPredicate> modTrueRelativeFacts = new HashSet<RelationalPredicate>();
				for (RelationalPredicate trueFact : trueRelativeFacts) {
					RelationalPredicate modTrueFact = new RelationalPredicate(
							trueFact);
					if (modTrueFact.replaceArguments(replacementMap, false,
							false))
						modTrueRelativeFacts.add(modTrueFact);
				}

				// Note the relative facts for untrue conditions.
				if (untrueCB.noteTrueRelativeFacts(modTrueRelativeFacts, null,
						false)) {
					changed = true;
					observationHash_ = null;
				}
			}

			return changed;
		}

		/**
		 * Removes all non-type invariants from the conditions.
		 * 
		 * @param conditions
		 *            The conditions to remove invariants from.
		 * @return If invariants were removed.
		 */
		private boolean removeInvariants(
				SortedSet<RelationalPredicate> conditions) {
			boolean changed = false;
			for (RelationalPredicate invariant : invariants_
					.getSpecificInvariants()) {
				// Only remove non-type invariants, as type invariants are
				// either needed, or will be added back anyway.
				if (!StateSpec.getInstance().isTypePredicate(
						invariant.getFactName())) {
					changed |= conditions.remove(invariant);
				}
			}
			// Also remove local invariants
			for (RelationalPredicate invariant : localAgentObservations_
					.getConditionInvariants().getSpecificInvariants()) {
				// Only remove non-type invariants, as type invariants are
				// either needed, or will be added back anyway.
				if (!StateSpec.getInstance().isTypePredicate(
						invariant.getFactName())) {
					changed |= conditions.remove(invariant);
				}
			}
			return changed;
		}

		/**
		 * Saves the condition beliefs to a file.
		 * 
		 * @param environmentDir
		 *            The directory of the beliefs.
		 */
		protected void saveConditionBeliefs(File environmentDir)
				throws Exception {
			File condBeliefFile = new File(environmentDir,
					CONDITION_BELIEF_FILE);
			condBeliefFile.createNewFile();
			FileWriter wr = new FileWriter(condBeliefFile);
			BufferedWriter buf = new BufferedWriter(wr);

			// Write condition beliefs
			for (String condition : conditionObservations_.conditionBeliefs_
					.keySet()) {
				buf.write(conditionObservations_.conditionBeliefs_
						.get(condition) + "\n");
			}
			for (String condition : conditionObservations_.negatedConditionBeliefs_
					.keySet()) {
				Map<IntegerArray, ConditionBeliefs> negCBs = conditionObservations_.negatedConditionBeliefs_
						.get(condition);
				for (IntegerArray negCB : negCBs.keySet())
					buf.write(negCBs.get(negCB) + "\n");
			}
			buf.write("\n");
			buf.write("Background Knowledge\n");
			for (BackgroundKnowledge bk : conditionObservations_.learnedEnvironmentRules_) {
				buf.write(bk.toString() + "\n");
			}

			buf.write("\n");
			buf.write("Global Invariants\n");
			buf.write(conditionObservations_.invariants_.toString() + "\n");

			buf.write("\n");
			buf.write("Observed ranges\n");
			boolean first = true;
			for (RangeContext rc : conditionRanges_.keySet()) {
				if (!first)
					buf.write(", ");
				buf.write(rc.toString(conditionRanges_.get(rc)));
				first = false;
			}

			buf.close();
			wr.close();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ConditionObservations other = (ConditionObservations) obj;
			if (conditionBeliefs_ == null) {
				if (other.conditionBeliefs_ != null)
					return false;
			} else if (!conditionBeliefs_.equals(other.conditionBeliefs_))
				return false;
			if (invariants_ == null) {
				if (other.invariants_ != null)
					return false;
			} else if (!invariants_.equals(other.invariants_))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			int subresult = 1;
			for (ConditionBeliefs cb : conditionBeliefs_.values())
				subresult = prime * subresult + cb.hashCode();
			result = prime * result + subresult;
			result = prime * result
					+ ((invariants_ == null) ? 0 : invariants_.hashCode());
			return result;
		}

		/**
		 * Notes the observations from collections of state facts.
		 * 
		 * @param stateFacts
		 *            The raw state facts.
		 * @param generalStateFacts
		 *            the generalised state facts.
		 * @param goalReplacements
		 *            The variable replacements for the goal terms.
		 * @return True if an observations change as a result.
		 */
		public boolean noteObservations(
				Collection<RelationalPredicate> stateFacts,
				Collection<String> generalStateFacts,
				Map<String, String> goalReplacements) {
			boolean changed = false;
			// Note the invariants
			changed |= invariants_
					.noteInvariants(stateFacts, generalStateFacts);
			changed |= localAgentObservations_.noteInvariantConditions(
					stateFacts, goalReplacements);

			// Use the term mapped facts to generate collections of true
			// facts.
			changed |= recordConditionAssociations(stateFacts, goalReplacements);
			return changed;
		}

		/**
		 * Simplifies a rule using the observed condition associations.
		 * 
		 * @param simplified
		 *            The to-be-simplified set of conditions.
		 * @param exitIfIllegal
		 *            If the conditions are to be tested for conflicting
		 *            conditions.
		 * @param onlyEquivalencies
		 *            If the simplification only looks at equivalencies.
		 * @return 1 if the rule is simplified/altered, 0 if no change, -1 if
		 *         rule conditions are illegal.
		 */
		public int simplifyRule(SortedSet<RelationalPredicate> simplified,
				boolean exitIfIllegal, boolean onlyEquivalencies) {
			boolean changedOverall = false;
			// Note which facts have already been tested, so changes don't
			// restart the process.
			SortedSet<RelationalPredicate> testedFacts = new TreeSet<RelationalPredicate>(
					simplified.comparator());
			boolean changedThisIter = true;

			// Simplify using the invariants first
			changedOverall |= removeInvariants(simplified);

			// Check each fact for simplifications, and check new facts when
			// they're added
			while (changedThisIter) {
				SortedSet<BackgroundKnowledge> testedBackground = new TreeSet<BackgroundKnowledge>();
				changedThisIter = false;
				// Iterate through the facts in the conditions
				for (RelationalPredicate fact : new TreeSet<RelationalPredicate>(
						simplified)) {
					// Only test untested facts, facts still present in
					// simplified and facts associated with rules.
					if (!testedFacts.contains(fact)
							&& simplified.contains(fact)
							&& mappedEnvironmentRules_.containsKey(fact
									.getFactName())) {
						// Test against every background knowledge regarding
						// this fact pred.
						for (BackgroundKnowledge bckKnow : mappedEnvironmentRules_
								.get(fact.getFactName())) {
							// If the knowledge hasn't been tested and is an
							// equivalence if only testing equivalences
							if (!testedBackground.contains(bckKnow)
									&& (!onlyEquivalencies || bckKnow
											.isEquivalence())) {
								// If this rule is an illegal rule
								if (bckKnow.checkIllegalRule(simplified,
										!exitIfIllegal)) {
									if (exitIfIllegal)
										return -1;
									changedThisIter = true;
									testedBackground.clear();
								}

								// If the simplification process changes things
								if (bckKnow.simplify(simplified)) {
									changedThisIter = true;
									testedBackground.clear();
								} else
									testedBackground.add(bckKnow);
							}
						}
						testedFacts.add(fact);
					}
				}
				changedOverall |= changedThisIter;
			}

			return (changedOverall) ? 1 : 0;
		}
	}
}
