package relationalFramework.agentObservations;

import relationalFramework.RelationalArgument;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateSpec;
import rrlFramework.Config;

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

import cerrla.ProgramArgument;
import cerrla.RLGGMerger;
import cerrla.modular.GeneralGoalCondition;
import jess.Fact;
import jess.QueryResult;
import jess.Rete;
import jess.ValueVector;
import util.ConditionComparator;
import util.MultiMap;

/**
 * A class for containing all environmental observations the agent makes while
 * learning. This class is used to help guide the agent's learning process.
 * 
 * @author Sam Sarjant
 */
public final class EnvironmentAgentObservations extends SettlingScan implements
		Serializable {
	private static final long serialVersionUID = 3176852561676107021L;

	/** The AgentObservations instance. */
	private static EnvironmentAgentObservations instance_;

	public static final String ACTION_CONDITIONS_FILE = "actionConditions&Ranges.txt";

	/** The agent observations directory. */
	public static final File AGENT_OBSERVATIONS_DIR = new File(
			"agentObservations/");

	public static final String CONDITION_BELIEF_FILE = "conditionBeliefs.txt";

	public static final String SERIALISATION_FILE = "agentObservations.ser";

	/** The action based observations, keyed by action predicate. */
	private Map<String, ActionBasedObservations> actionBasedObservations_;

	/** The condition observations. */
	private ConditionObservations conditionObservations_;

	/** The environment these observations are for. */
	private String environment_;

	/** Records the last scanned state to prevent redundant scanning. */
	private transient Collection<Fact> lastScannedState_;

	/** A transient group of facts indexed by terms used within. */
	private transient MultiMap<RelationalArgument, RelationalPredicate> termMappedFacts_;

	/**
	 * The constructor for the agent observations.
	 */
	private EnvironmentAgentObservations() {
		conditionObservations_ = new ConditionObservations();
		actionBasedObservations_ = new HashMap<String, ActionBasedObservations>();
		environment_ = StateSpec.getInstance().getEnvironmentName();

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
	 * Creates the specialisation conditions from the variants by simply using
	 * negated and non-negated conditions.
	 * 
	 * @param variants
	 *            The variants the specialisations are composed of.
	 * @param addNegated
	 *            If negated versions of the specialisations should also be
	 *            added.
	 * @return The specialisation conditions.
	 */
	protected Collection<RelationalPredicate> createSpecialisations(
			Collection<RelationalPredicate> variants, boolean addNegated,
			String action, Collection<RelationalPredicate> localInvariants,
			Collection<RelationalPredicate> localVariants) {
		SortedSet<RelationalPredicate> specialisations = new TreeSet<RelationalPredicate>(
				ConditionComparator.getInstance());
		if (variants == null)
			return specialisations;

		for (RelationalPredicate condition : variants) {
			// Simplify defined numerical ranges into variables.
			if (condition.isNumerical()) {
				condition = new RelationalPredicate(condition);
				condition.clearRanges();
			}

			// Add variant to specialisations
			if (condition != null && specialisations.add(condition)) {
				// Check the negated version
				if (addNegated) {
					RelationalArgument[] negArgs = new RelationalArgument[condition
							.getArgTypes().length];
					// Special case for numerical values - negated
					// numericals are made anonymous
					for (int i = 0; i < condition.getArgTypes().length; i++) {
						if (StateSpec.isNumberType(condition.getArgTypes()[i]))
							negArgs[i] = RelationalArgument.ANONYMOUS;
						else
							negArgs[i] = condition.getRelationalArguments()[i]
									.clone();
					}

					RelationalPredicate negCondition = new RelationalPredicate(
							condition, negArgs);
					negCondition.swapNegated();
					// negCondition = simplifyCondition(negCondition, action,
					// localInvariants, localVariants);
					if (negCondition != null)
						specialisations.add(negCondition);
				}
			}
		}

		return specialisations;
	}

	@Override
	protected int updateHash() {
		// Update the hash
		final int prime = 31;
		int newHash = 1;

		// Note the condition observations
		newHash = prime * newHash + conditionObservations_.hashCode();

		// Note the action observations
		newHash = prime * newHash + actionBasedObservations_.hashCode();
		return newHash;
	}

	public void clearActionBasedObservations() {
		actionBasedObservations_ = new HashMap<String, ActionBasedObservations>();
	}

	/**
	 * Gathers all relevant facts for a particular action and returns them.
	 * 
	 * @param stateFacts
	 *            The state facts of this state.
	 * @param action
	 *            The action (with arguments).
	 * @param actionRanges
	 *            The action ranges to observe.
	 * @param goalTerms
	 *            The terms present in the goal.
	 * @return The relevant facts pertaining to the action.
	 */
	public Collection<RelationalPredicate> gatherActionFacts(
			Collection<Fact> stateFacts, RelationalPredicate action,
			Map<RelationalArgument, RelationalArgument> goalReplacements,
			Map<RangeContext, double[]> actionRanges) {
		// If the state has been scanned, then the actions do not need to be
		// rescanned.
		boolean needToScan = (lastScannedState_ == null || !lastScannedState_
				.equals(stateFacts));

		// Note down action conditions if still unsettled.
		Map<RelationalArgument, RelationalArgument> replacementMap = action
				.createVariableTermReplacementMap(false, false);
		int replBefore = replacementMap.size();
		if (goalReplacements != null) {
			goalReplacements = new HashMap<RelationalArgument, RelationalArgument>(
					goalReplacements);
			goalReplacements.putAll(replacementMap);
		}

		Collection<RelationalPredicate> actionFacts = new HashSet<RelationalPredicate>();
		Set<RelationalPredicate> actionConds = new HashSet<RelationalPredicate>();
		Set<RelationalPredicate> goalActionConds = new HashSet<RelationalPredicate>();
		// Gather facts for each (non-number) action argument
		for (RelationalArgument argument : action.getRelationalArguments()) {
			if (!argument.isNumber()) {
				Collection<RelationalPredicate> termFacts = termMappedFacts_
						.get(argument);
				// Modify the term facts, retaining constants, replacing terms
				for (RelationalPredicate termFact : termFacts) {
					// If the action needs to be scanned.
					RelationalPredicate notedFact = new RelationalPredicate(
							termFact);

					if (!actionFacts.contains(notedFact))
						actionFacts.add(notedFact);

					// Note the action condition
					RelationalPredicate actionCond = new RelationalPredicate(
							termFact);
					int unboundOffset = replacementMap.size() - replBefore;
					actionCond.replaceArguments(replacementMap, false, true);
					actionConds.add(actionCond);


					// Note the goal action condition
					if (goalReplacements != null) {
						RelationalPredicate goalCond = new RelationalPredicate(
								termFact);
						goalCond.flexibleReplaceArguments(goalReplacements,
								unboundOffset);
						if (!actionConds.contains(goalCond))
							goalActionConds.add(goalCond);
					}
				}
			}
		}

		// If the environment needs to be scanned.
		ActionBasedObservations abo = getActionBasedObservation(action
				.getFactName());
		if (needToScan) {
			if (!actionConds.isEmpty()
					&& abo.addActionConditions(actionConds, action,
							actionRanges)) {
				resetInactivity();
			}
		}

		// Note the action ranges
		abo.noteRanges(actionRanges);

		return goalActionConds;
	}

	public Map<String, ConditionBeliefs> getConditionBeliefs() {
		return conditionObservations_.conditionBeliefs_;
	}

	public Collection<String> getGeneralInvariants() {
		return conditionObservations_.invariants_.getGeneralInvariants();
	}

	public Collection<RelationalPredicate> getGeneralSpecialisationConditions() {
		return conditionObservations_.getGeneralSpecialisationConditions();
	}

	/**
	 * Gets a collection of general goal conditions noted by the observations
	 * which represent general sub-goals to achieve. Note that these goal
	 * conditions can be negated.
	 * 
	 * @return A collection of general sub-goals.
	 */
	public Collection<GeneralGoalCondition> getGeneralGoalConditions() {
		return conditionObservations_.getGeneralGoalConditions();
	}

	public Collection<BackgroundKnowledge> getLearnedBackgroundKnowledge() {
		return conditionObservations_.inferredRules_
				.getAllBackgroundKnowledge();
	}

	public Map<String, Map<IntegerArray, ConditionBeliefs>> getNegatedConditionBeliefs() {
		return conditionObservations_.negatedConditionBeliefs_;
	}

	public Collection<String> getNeverSeenInvariants() {
		return conditionObservations_.invariants_.getNeverSeenPredicates();
	}

	public Collection<BackgroundKnowledge> getReverseMappedConditions(
			String condition) {
		return conditionObservations_.inferredRules_
				.getReversePredicateMappedRules().get(condition);
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

	/**
	 * Gets the environmental RLGG rules found through the action observations.
	 * This is found by observing the invariants in the actions.
	 * 
	 * @return The RLGGs for every action.
	 */
	public List<RelationalRule> getRLGGActionRules() {
		List<RelationalRule> rlggRules = new ArrayList<RelationalRule>();
		for (ActionBasedObservations abo : actionBasedObservations_.values()) {
			RelationalRule rlggRule = abo.getRLGGRule();
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
		Collection<RelationalPredicate> rlggConds = actionBasedObservations_
				.get(action.getFactName()).getRLGGRule().getRawConditions(true);
		Collection<RelationalPredicate> termSwappedConds = new ArrayList<RelationalPredicate>(
				rlggConds.size());
		RelationalArgument[] actionTerms = action.getRelationalArguments();
		for (RelationalPredicate rlggCond : rlggConds) {
			rlggCond = new RelationalPredicate(rlggCond);
			rlggCond.replaceArguments(actionTerms);
			termSwappedConds.add(rlggCond);
		}
		return termSwappedConds;
	}

	public double[] getGlobalRange(RangeContext rangeContext) {
		return conditionObservations_.conditionRanges_.get(rangeContext);
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

	/**
	 * A method which checks if covering is necessary or required, based on the
	 * valid actions of the state.
	 * 
	 * @param state
	 *            The current state.
	 * @param validActions
	 *            The set of valid actions for the state. If we're creating LGG
	 *            covering rules, the activated actions need to equal this set.
	 * @param activatedActions
	 *            The set of actions already activated by the policy. We only
	 *            consider these when we're creating new rules.
	 * @return True if covering is needed.
	 */
	public boolean isCoveringNeeded(Rete state,
			MultiMap<String, String[]> validActions,
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

		// Check for unseen predicates
		if (conditionObservations_.checkForUnseenPreds(state)) {
			return true;
		}

		boolean changed = isScanNeeded();
		return changed;
	}

	/**
	 * Note the last scanned state to avoid redundant scanning.
	 * 
	 * @param stateFacts
	 *            The facts of the state.
	 */
	public void noteScannedState(Collection<Fact> stateFacts) {
		lastScannedState_ = stateFacts;
	}

	@Override
	public void resetInactivity() {
		super.resetInactivity();
		for (ActionBasedObservations abo : actionBasedObservations_.values()) {
			abo.recreateRLGG_ = true;
		}
	}

	/**
	 * Saves agent observations to file in the agent observations directory.
	 * 
	 * @param goal
	 *            The goal of this generator.
	 */
	public void saveAgentObservations() {
		try {
			// Make the environment directory if necessary
			File environmentDir = new File(AGENT_OBSERVATIONS_DIR, StateSpec
					.getInstance().getEnvironmentName() + File.separatorChar);
			environmentDir.mkdir();

			// Condition beliefs
			conditionObservations_.saveConditionBeliefs(environmentDir);

			// Global action Conditions and ranges
			File actionCondsFile = new File(environmentDir,
					ACTION_CONDITIONS_FILE);
			actionCondsFile.createNewFile();
			FileWriter wr = new FileWriter(actionCondsFile);
			BufferedWriter buf = new BufferedWriter(wr);

			// Write action conditions and ranges
			for (ActionBasedObservations abo : actionBasedObservations_
					.values()) {
				abo.saveActionBasedObservations(buf);
			}

			buf.close();
			wr.close();

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
	 *            The variable replacements for the local goal.
	 * @return All facts which contain a goal term.
	 */
	public Collection<RelationalPredicate> scanState(Collection<Fact> state,
			Map<RelationalArgument, RelationalArgument> goalReplacements) {
		Collection<RelationalPredicate> goalFacts = new HashSet<RelationalPredicate>();


		// If the state was already scanned, no need to scan again.
		if (lastScannedState_ != null && lastScannedState_.equals(state)) {
			for (RelationalArgument term : goalReplacements.keySet()) {
				Collection<RelationalPredicate> goalTermFacts = termMappedFacts_
						.get(term);
				for (RelationalPredicate goalTermFact : goalTermFacts) {
					RelationalPredicate replFact = new RelationalPredicate(
							goalTermFact);
					replFact.replaceArguments(goalReplacements, true, false);
					goalFacts.add(replFact);
				}
			}
		} else {



			// Run through the facts, adding to term mapped facts and adding the
			// raw facts for condition belief scanning.
			Collection<RelationalPredicate> stateFacts = new ArrayList<RelationalPredicate>(
					state.size());
			Collection<String> generalStateFacts = new HashSet<String>();
			termMappedFacts_ = MultiMap.createSortedSetMultiMap();
			for (Fact stateFact : state) {
				RelationalPredicate strFact = null;
				strFact = StateSpec.toRelationalPredicate(stateFact.toString());

				// Ignore the type, goal, inequal and actions pred
				if (strFact != null) {
					if (StateSpec.getInstance().isNotInternalPredicate(
							strFact.getFactName())) {
						stateFacts.add(strFact);
						generalStateFacts.add(strFact.getFactName());

						// Run through the arguments and index the fact by term
						for (RelationalArgument arg : strFact
								.getRelationalArguments()) {
							// Ignore numerical terms
							// if (!arg.isNumber())
							termMappedFacts_.putContains(arg, strFact);

							// Note goal facts
							if (goalReplacements.containsKey(arg)) {
								RelationalPredicate replFact = new RelationalPredicate(
										strFact);
								replFact.replaceArguments(goalReplacements,
										true, false);
								goalFacts.add(replFact);
							}
						}
					}
				}
			}

			// Note the condition mappings.
			if (conditionObservations_.noteObservations(stateFacts,
					generalStateFacts, goalReplacements)) {
				resetInactivity();
			} else
				incrementInactivity();
		}

		return goalFacts;
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
	 * @param localConditionInvariants
	 *            The optional local condition invariants.
	 * @return 1 if the condition was simplified, 0 if no change, -1 if illegal
	 *         rule (and exiting with illegal rules).
	 */
	public int simplifyRule(Collection<RelationalPredicate> simplified,
			boolean exitIfIllegalRule, boolean onlyEquivalencies,
			Collection<RelationalPredicate> localConditionInvariants) {
		// Simplify using background knowledge
		boolean changed = false;
		int simplResult = 0;
		do {
			// Update the non action variable bindings
			if (ProgramArgument.USING_UNBOUND_VARS.booleanValue())
				updateNonActionBindings(simplified);

			// Simplify
			simplResult = conditionObservations_.simplifyRule(simplified,
					exitIfIllegalRule, onlyEquivalencies,
					localConditionInvariants);
			changed |= (simplResult == 1);
			if (simplResult == -1)
				return -1;
		} while (simplResult == 1);

		return (changed) ? 1 : 0;
	}

	/**
	 * Updates the free/not free status of non-action variable bindings.
	 * 
	 * @param simplified
	 *            The facts being simplified and updated.
	 */
	private void updateNonActionBindings(
			Collection<RelationalPredicate> simplified) {
		// A map which maps a particularly named non-action variable to a
		// collection of direct pointers to the arguments.
		MultiMap<String, RelationalArgument> nonActionMap = MultiMap
				.createListMultiMap();
		Collection<RelationalPredicate> simpChanged = new HashSet<RelationalPredicate>();
		for (RelationalPredicate cond : simplified) {
			for (RelationalArgument arg : cond.getActualArguments()) {
				if (arg.isNonActionVar()) {
					nonActionMap.put(arg.toString(), arg);

					// Resolve immediately
					try {
						List<RelationalArgument> args = nonActionMap
								.getList(arg.toString());
						if (args.size() == 1) {
							// Treat it as unbound
							arg.setFreeVariable(true);
						} else if (args.size() == 2) {
							// Treat the two as bound
							args.get(0).setFreeVariable(false);
							arg.setFreeVariable(false);
						} else if (args.size() > 2) {
							// Treat this arg as bound
							arg.setFreeVariable(false);
						}
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
			simpChanged.add(cond);
		}
		simplified.clear();
		simplified.addAll(simpChanged);
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
		RelationalArgument[] args = fact.getRelationalArguments();
		int[] structure = new int[args.length];
		Map<RelationalArgument, Integer> seenArgs = new HashMap<RelationalArgument, Integer>();
		for (int i = 0; i < args.length; i++) {
			if (!args[i].isAnonymous()) {
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
	protected static EnvironmentAgentObservations getInstance() {
		if (instance_ == null)
			instance_ = new EnvironmentAgentObservations();
		return instance_;
	}

	/**
	 * Loads an AgentObservation serialised class from file, or if no file
	 * available, creates a new one.
	 * 
	 * @param localGoal
	 *            The local goal observations to load.
	 * @return True If there were AgentObservations to load, false otherwise.
	 */
	protected static boolean loadAgentObservations() {
		String environment = StateSpec.getInstance().getEnvironmentName();
		// Only load them once.
		if (instance_ != null && instance_.environment_.equals(environment))
			return false;

		if (ProgramArgument.LOAD_AGENT_OBSERVATIONS.booleanValue()
				|| Config.getInstance().shouldLoadAgentObservations()) {
			try {
				File globalObsFile = new File(AGENT_OBSERVATIONS_DIR,
						environment + File.separatorChar + SERIALISATION_FILE);
				if (globalObsFile.exists()) {
					FileInputStream fis = new FileInputStream(globalObsFile);
					ObjectInputStream ois = new ObjectInputStream(fis);
					EnvironmentAgentObservations ao = (EnvironmentAgentObservations) ois
							.readObject();
					if (ao != null) {
						instance_ = ao;
						ao.environment_ = environment;

						return true;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		instance_ = getInstance();
		return false;
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

		private EnvironmentAgentObservations getOuterType() {
			return EnvironmentAgentObservations.this;
		}

		protected void saveActionBasedObservations(BufferedWriter buf)
				throws Exception {
			buf.write(action_.getFactName() + "\n");
			SortedSet<RelationalPredicate> rlggConds = new TreeSet<RelationalPredicate>(
					ConditionComparator.getInstance());
			rlggConds.addAll(invariantActionConditions_);
			buf.write("RLGG conditions: " + rlggConds + "\n");
			buf.write("Specialisation conditions: "
					+ createSpecialisations(variantActionConditions_, true,
							action_.getFactName(), null, null) + "\n\n");
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
		 * @param actionRanges
		 *            The action ranges to observe.
		 * @return True if the set of action conditions changed because of the
		 *         addition.
		 */
		private boolean addActionConditions(
				Collection<RelationalPredicate> actionConds,
				RelationalPredicate action,
				Map<RangeContext, double[]> actionRanges) {
			boolean changed = false;

			// If this is the first action condition, then all the actions are
			// considered invariant.
			if (invariantActionConditions_ == null) {
				invariantActionConditions_ = new TreeSet<RelationalPredicate>(
						actionConds);
				variantActionConditions_ = new TreeSet<RelationalPredicate>();
				action_ = new RelationalPredicate(action);
				recreateRLGG_ = true;
				return true;
			}


			// Sort the invariant and variant conditions, making a special case
			// for numerical conditions.
			changed = InvariantObservations.intersectActionConditions(action,
					action_, actionConds, invariantActionConditions_,
					variantActionConditions_, actionRanges, null);

			if (changed) {
				specialisationConditions_ = null;
				recreateRLGG_ = true;
				return true;
			}
			return false;
		}

		/**
		 * Notes all the ranges from the observations.
		 * 
		 * @param actionRanges
		 *            The map of facts, mapped by factName-range variable.
		 */
		private void noteRanges(Map<RangeContext, double[]> actionRanges) {
			for (RelationalPredicate pred : invariantActionConditions_) {
				if (pred.isNumerical())
					noteRange(pred, actionRanges);
			}
			for (RelationalPredicate pred : variantActionConditions_) {
				if (pred.isNumerical())
					noteRange(pred, actionRanges);
			}
		}

		/**
		 * Notes a single range from a fact.
		 * 
		 * @param numberFact
		 *            The fact with the number range.
		 * @param actionRanges
		 *            The action ranges to note the range under.
		 */
		private void noteRange(RelationalPredicate numberFact,
				Map<RangeContext, double[]> actionRanges) {

			RelationalArgument[] factArgs = numberFact.getRelationalArguments();
			for (int i = 0; i < factArgs.length; i++) {
				// Only note ranges
				if (factArgs[i].isRange(false)) {
					RangeContext context = new RangeContext(i, numberFact,
							action_.getFactName());
					double[] range = actionRanges.get(context);
					double[] explicitRange = factArgs[i].getExplicitRange();
					if (range == null) {
						range = new double[2];
						System.arraycopy(explicitRange, 0, range, 0, 2);
						actionRanges.put(context, range);
					} else {
						// Expand the range.
						if (explicitRange[0] < range[0])
							range[0] = explicitRange[0];
						if (explicitRange[1] > range[1]) {
							range[1] = explicitRange[1];
						}
					}
				}
			}
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
				RelationalArgument[] replacements = null;
				for (String arg : action_.getArguments()) {
					if (arg.charAt(0) != '?') {
						replacements = action_.getRelationalArguments();
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
				rlggRule_ = new RelationalRule(ruleConds, action_, null, null);

				recreateRLGG_ = false;
			}
			return rlggRule_;
		}

		public Collection<RelationalPredicate> getSpecialisationConditions() {
			if (specialisationConditions_ == null)
				specialisationConditions_ = createSpecialisations(
						variantActionConditions_, true, action_.getFactName(),
						null, null);
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
		private static final long serialVersionUID = 8571836859159633591L;

		/** The agent's beliefs about the condition inter-relations. */
		private Map<String, ConditionBeliefs> conditionBeliefs_;

		/**
		 * A map (by fact name) of various conditions which note the maximum
		 * bounds of observed ranges.
		 */
		private Map<RangeContext, double[]> conditionRanges_;

		/** The general goal conditions tied to the observed predicates. */
		private transient Collection<GeneralGoalCondition> generalGoalConds_;

		/** The observed invariants of the environment. */
		private InvariantObservations invariants_;

		/** The rules about the environment learned by the agent. */
		private NonRedundantBackgroundKnowledge inferredRules_;;

		/**
		 * The agent's beliefs about the negated condition inter-relations (when
		 * conditions AREN'T true), ordered using a second mapping of argument
		 * index
		 */
		private Map<String, Map<IntegerArray, ConditionBeliefs>> negatedConditionBeliefs_;

		/** The collection of unseen predicates. */
		private Collection<RelationalPredicate> unseenPreds_;

		/**
		 * A collection of specialisation conditions using only anon conditions.
		 */
		private Collection<RelationalPredicate> generalSpecialisationConditions_;

		public ConditionObservations() {
			conditionBeliefs_ = new TreeMap<String, ConditionBeliefs>();
			negatedConditionBeliefs_ = new TreeMap<String, Map<IntegerArray, ConditionBeliefs>>();
			inferredRules_ = formBackgroundKnowledge();
			invariants_ = new InvariantObservations();
			conditionRanges_ = new HashMap<RangeContext, double[]>();

			unseenPreds_ = new HashSet<RelationalPredicate>();
			unseenPreds_.addAll(StateSpec.getInstance().getPredicates()
					.values());
			unseenPreds_.addAll(StateSpec.getInstance().getTypePredicates()
					.values());
		}

		/**
		 * Gets the general specialisation conditions.
		 * 
		 * @return A set of anonymous conditions representing the general
		 *         specialisations.
		 */
		public Collection<RelationalPredicate> getGeneralSpecialisationConditions() {
			if (generalSpecialisationConditions_ == null) {
				generalSpecialisationConditions_ = new HashSet<RelationalPredicate>();
				for (String variant : invariants_.getGeneralVariants()) {
					// Create both negated and non-negated versions
					RelationalPredicate predicate = StateSpec.getInstance()
							.getPredicateByName(variant);
					generalSpecialisationConditions_
							.add(new RelationalPredicate(predicate));

					RelationalPredicate negPred = new RelationalPredicate(
							predicate);
					negPred.swapNegated();
					generalSpecialisationConditions_.add(negPred);
				}
			}
			return generalSpecialisationConditions_;
		}

		/**
		 * Gets a collection of general goal conditions noted by the
		 * observations which represent general sub-goals to achieve. Note that
		 * these goal conditions can be negated.
		 * 
		 * @return A collection of general sub-goals.
		 */
		private Collection<GeneralGoalCondition> getGeneralGoalConditions() {
			if (generalGoalConds_ == null) {
				generalGoalConds_ = new HashSet<GeneralGoalCondition>();
				for (RelationalPredicate genPredicate : getGeneralSpecialisationConditions())
					generalGoalConds_
							.add(new GeneralGoalCondition(genPredicate));
			}
			return generalGoalConds_;
		}

		/**
		 * Runs through the set of unseen predicates to check if the state
		 * contains them. This method is used to capture variant action
		 * conditions.
		 * 
		 * @param state
		 *            The current state.
		 * @return True if the state does need to be scanned.
		 */
		private boolean checkForUnseenPreds(Rete state) {
			boolean triggerCovering = false;
			try {
				// Run through the unseen preds, checking if they are present.
				Collection<RelationalPredicate> removables = new HashSet<RelationalPredicate>();
				for (RelationalPredicate unseenPred : unseenPreds_) {
					String query = StateSpec.getInstance().getRuleQuery(
							unseenPred, false);
					QueryResult results = state.runQueryStar(query,
							new ValueVector());
					if (results.next()) {
						// The unseen pred exists - trigger covering
						triggerCovering = true;
						removables.add(unseenPred);
					}
				}

				// If any unseen preds are seen, trigger covering.
				if (triggerCovering)
					unseenPreds_.removeAll(removables);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return triggerCovering;
		}

		/**
		 * Forms the background knowledge from the condition beliefs.
		 */
		private NonRedundantBackgroundKnowledge formBackgroundKnowledge() {
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
			return currentKnowledge;
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
				Collection<RelationalPredicate> stateFacts) {
			boolean changed = false;
			for (RelationalPredicate baseFact : stateFacts) {
				// Getting the ConditionBeliefs object
				ConditionBeliefs cb = conditionBeliefs_.get(baseFact
						.getFactName());
				if (cb == null) {
					cb = new ConditionBeliefs(baseFact.getFactName());
					conditionBeliefs_.put(baseFact.getFactName(), cb);
				}

				// Create a replacement map here (excluding numerical
				// values)
				Map<RelationalArgument, RelationalArgument> replacementMap = baseFact
						.createVariableTermReplacementMap(true, false);

				// Replace facts for all relevant facts and store as
				// condition beliefs.
				Collection<RelationalPredicate> relativeFacts = new HashSet<RelationalPredicate>();
				for (RelationalArgument term : baseFact
						.getRelationalArguments()) {
					Collection<RelationalPredicate> termFacts = termMappedFacts_
							.get(term);
					if (termFacts != null) {
						for (RelationalPredicate termFact : termFacts) {
							// if (!termFact.isNumerical()) {
							RelationalPredicate relativeFact = new RelationalPredicate(
									termFact);
							relativeFact.replaceArguments(replacementMap,
									false, false);
							relativeFacts.add(relativeFact);
							// }
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
				}

				// Form the condition beliefs for the not relative facts,
				// using the relative facts as always true values
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
								RLGGMerger.getInstance().unifyRange(range,
										baseVal);
							}
						}
					}
				}

				// Remove from unseen preds
				if (!unseenPreds_.isEmpty())
					unseenPreds_.remove(StateSpec.getInstance()
							.getPredicateByName(baseFact.getFactName()));
			}

			if (changed) {
				inferredRules_ = formBackgroundKnowledge();
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
				RelationalArgument[] untrueFactArgs = untrueFact
						.getRelationalArguments();
				// Form the replacement map, modifying the args if necessary
				// (such that ?X is first arg, ?Y second, etc).

				Map<RelationalArgument, RelationalArgument> replacementMap = new HashMap<RelationalArgument, RelationalArgument>();
				for (int i = 0; i < untrueFactArgs.length; i++) {
					if (!untrueFactArgs[i].isAnonymous()) {
						if (!replacementMap.containsKey(untrueFactArgs[i]))
							replacementMap
									.put(untrueFactArgs[i], RelationalArgument
											.createVariableTermArg(i));
						untrueFactArgs[i] = replacementMap
								.get(untrueFactArgs[i]);
					}
				}

				IntegerArray argState = determineArgState(untrueFact);
				ConditionBeliefs untrueCB = untrueCBs.get(argState);
				if (untrueCB == null) {
					untrueCB = new ConditionBeliefs(factName, untrueFactArgs);
					untrueCBs.put(argState, untrueCB);
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
			for (BackgroundKnowledge bk : conditionObservations_.inferredRules_
					.getAllBackgroundKnowledge()) {
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
				Map<RelationalArgument, RelationalArgument> goalReplacements) {
			boolean changed = false;
			// Note the invariants
			changed |= invariants_.noteAllInvariants(stateFacts,
					generalStateFacts);
			if (changed) {
				conditionObservations_.generalGoalConds_ = null;
				conditionObservations_.generalSpecialisationConditions_ = null;
			}

			// Use the term mapped facts to generate collections of true
			// facts.
			changed |= recordConditionAssociations(stateFacts);
			return changed;
		}

		/**
		 * Simplifies a rule using the observed condition associations.
		 * 
		 * @param conds
		 *            The to-be-simplified set of conditions.
		 * @param exitIfIllegal
		 *            If the conditions are to be tested for conflicting
		 *            conditions.
		 * @param onlyEquivalencies
		 *            If the simplification only looks at equivalencies.
		 * @param localConditionInvariants
		 *            The optional local condition invariants.
		 * @return 1 if the rule is simplified/altered, 0 if no change, -1 if
		 *         rule conditions are illegal.
		 */
		public int simplifyRule(Collection<RelationalPredicate> conds,
				boolean exitIfIllegal, boolean onlyEquivalencies,
				Collection<RelationalPredicate> localConditionInvariants) {
			boolean changedOverall = false;

			Collection<RelationalPredicate> simplified = inferredRules_
					.simplify(conds);
			if (simplified == null || simplified.isEmpty())
				return -1;

			if (!conds.containsAll(simplified)
					|| conds.size() != simplified.size())
				changedOverall = true;
			conds.clear();
			conds.addAll(simplified);

			return (changedOverall) ? 1 : 0;
		}
	}

	/**
	 * Sets the instance as clear.
	 */
	public static void clearInstance() {
		instance_ = null;
	}
}
