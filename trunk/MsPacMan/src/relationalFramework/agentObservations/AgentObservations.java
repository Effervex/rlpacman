package relationalFramework.agentObservations;

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

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import jess.Fact;
import relationalFramework.ConditionComparator;
import relationalFramework.GuidedRule;
import relationalFramework.PolicyGenerator;
import relationalFramework.RuleCreation;
import relationalFramework.MultiMap;
import relationalFramework.StateSpec;
import relationalFramework.StringFact;
import relationalFramework.Unification;

/**
 * A class for containing all environmental observations the agent makes while
 * learning. This class is used to help guide the agent's learning process.
 * 
 * @author Sam Sarjant
 */
public final class AgentObservations implements Serializable {
	private static final String ACTION_CONDITIONS_FILE = "actionConditions&Ranges.txt";

	private static final String CONDITION_BELIEF_FILE = "conditionBeliefs.txt";

	private static final String SERIALISATION_FILE = "agentObservations.ser";

	/** The agent observations directory. */
	public static final File AGENT_OBSERVATIONS_DIR = new File(
			"agentObservations/");

	/** The AgentObservations instance. */
	private static AgentObservations instance_;

	/** The condition observations. */
	private ConditionObservations conditionObservations_;

	/** The action based observations, keyed by action predicate. */
	private Map<String, ActionBasedObservations> actionBasedObservations_;

	/** A hash code to track when an observation changes. */
	private transient Integer observationHash_ = null;

	/** The amount of inactivity the observations has accrued. */
	private transient int inactivity_ = 0;

	/** The number of steps since last covering the state. */
	private transient int lastCover_ = 0;

	/** A transient group of facts indexed by terms used within. */
	private transient MultiMap<String, StringFact> termMappedFacts_;

	/** The observed predicates mentioning goal terms that have been true. */
	private transient MultiMap<String, StringFact> observedGoalPredicates_;

	/** The general goal args pred to add to rules. */
	private transient StringFact goalArgsPred_;

	/**
	 * The constructor for the agent observations.
	 */
	private AgentObservations() {
		conditionObservations_ = new ConditionObservations();
		actionBasedObservations_ = new HashMap<String, ActionBasedObservations>();
		observationHash_ = null;
		inactivity_ = 0;
		lastCover_ = 0;
		observedGoalPredicates_ = MultiMap.createSortedSetMultiMap();

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

			// Note the goal predicate observations
			observationHash_ = prime * observationHash_
					+ observedGoalPredicates_.hashCode();
		}
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
		if (lastCover_ >= inactivity_) {
			lastCover_ = 0;
			return true;
		}
		lastCover_++;

		return false;
	}

	public void clearActionBasedObservations() {
		actionBasedObservations_ = new HashMap<String, ActionBasedObservations>();
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
	public Collection<StringFact> gatherActionFacts(StringFact action,
			Map<String, String> goalReplacements) {
		// Note down action conditions if still unsettled.
		Map<String, String> replacementMap = action
				.createVariableTermReplacementMap(false);
		if (goalReplacements != null) {
			goalReplacements = new HashMap<String, String>(goalReplacements);
			goalReplacements.putAll(replacementMap);
		}

		Collection<StringFact> actionFacts = new HashSet<StringFact>();
		Set<StringFact> actionConds = new HashSet<StringFact>();
		Set<StringFact> goalActionConds = new HashSet<StringFact>();
		// Gather facts for each (non-number) action argument
		for (String argument : action.getArguments()) {
			if (!StateSpec.isNumber(argument)) {
				Collection<StringFact> termFacts = termMappedFacts_
						.get(argument);
				// Modify the term facts, retaining constants, replacing terms
				for (StringFact termFact : termFacts) {
					StringFact notedFact = new StringFact(termFact);

					if (!actionFacts.contains(notedFact))
						actionFacts.add(notedFact);

					// Note the action condition
					StringFact actionCond = new StringFact(termFact);
					actionCond.replaceArguments(replacementMap, false);
					actionConds.add(actionCond);

					// Note the goal action condition
					if (goalReplacements != null) {
						StringFact goalCond = new StringFact(termFact);
						goalCond.replaceArguments(goalReplacements, false);
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

	public int getObservationHash() {
		updateHash();
		return observationHash_;
	}

	/**
	 * Gets the RLGG rules found through the action observations. This is found
	 * by observing the invariants in the actions.
	 * 
	 * @return The RLGGs for every action.
	 */
	public List<GuidedRule> getRLGGActionRules() {
		List<GuidedRule> rlggRules = new ArrayList<GuidedRule>();
		for (ActionBasedObservations abo : actionBasedObservations_.values()) {
			GuidedRule rlggRule = abo.getRLGGRule();
			rlggRules.add(rlggRule);
		}
		return rlggRules;
	}

	public Collection<StringFact> getSpecialisationConditions(String actionPred) {
		if (!actionBasedObservations_.containsKey(actionPred))
			return null;
		return actionBasedObservations_.get(actionPred)
				.getSpecialisationConditions();
	}

	/**
	 * Gets the number of specialisations an action can have.
	 * 
	 * @param actionPredicate
	 *            The action predicate to check.
	 * @return The total possible number of specialisations the action can make.
	 */
	public int getNumSpecialisations(String action) {
		Collection<StringFact> specConditions = getSpecialisationConditions(action);
		int num = 0;
		if (specConditions != null)
			num = specConditions.size();
		// Also, if the action has numerical arguments, add them to the count
		// too
		for (String type : StateSpec.getInstance().getStringFact(action)
				.getArgTypes()) {
			if (StateSpec.isNumberType(type))
				num += PolicyGenerator.NUM_NUMERICAL_SPLITS;
		}
		return num;
	}

	public Collection<StringFact> getSpecificInvariants() {
		return conditionObservations_.invariants_.getSpecificInvariants();
	}

	public Collection<StringFact> getUnseenPredicates() {
		return conditionObservations_.unseenPreds_;
	}

	public boolean removeUnseenPredicates(Collection<StringFact> removables) {
		return conditionObservations_.unseenPreds_.removeAll(removables);
	}

	public void resetInactivity() {
		inactivity_ = 0;
		for (ActionBasedObservations abo : actionBasedObservations_.values()) {
			abo.recreateRLGG_ = true;
		}
	}

	public MultiMap<String, StringFact> getGoalPredicateMap() {
		return observedGoalPredicates_;
	}

	/**
	 * Saves agent observations to file in the agent observations directory.
	 */
	public void saveAgentObservations() {
		try {
			// Make the environment directory if necessary
			File environmentDir = new File(AGENT_OBSERVATIONS_DIR, StateSpec
					.getInstance().getEnvironmentName()
					+ File.separatorChar);
			environmentDir.mkdir();

			// Condition beliefs
			File condBeliefFile = new File(environmentDir,
					CONDITION_BELIEF_FILE);
			condBeliefFile.createNewFile();
			FileWriter wr = new FileWriter(condBeliefFile);
			BufferedWriter buf = new BufferedWriter(wr);

			// Write condition beliefs
			for (String condition : conditionObservations_.conditionBeliefs_
					.keySet()) {
				buf.write(conditionObservations_.conditionBeliefs_
						.get(condition)
						+ "\n");
			}
			buf.write("\n");
			// TODO Write invariants
			buf.write("Background Knowledge\n");
			for (BackgroundKnowledge bk : conditionObservations_.learnedEnvironmentRules_) {
				buf.write(bk.toString() + "\n");
			}

			buf.close();
			wr.close();

			// Action Conditions and ranges
			File actionCondsFile = new File(environmentDir,
					ACTION_CONDITIONS_FILE);
			actionCondsFile.createNewFile();
			wr = new FileWriter(actionCondsFile);
			buf = new BufferedWriter(wr);

			// Write action conditions and ranges
			for (String action : actionBasedObservations_.keySet()) {
				buf.write(action + "\n");
				buf
						.write("True conditions: "
								+ actionBasedObservations_.get(action).specialisationConditions_
								+ "\n");
				buf.write("\n");
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
	 *            The variable replacements for the goal terms.
	 * @return The mapping of terms to facts.
	 */
	public void scanState(Collection<Fact> state,
			Map<String, String> goalReplacements) {
		// Run through the facts, adding to term mapped facts and adding the raw
		// facts for condition belief scanning.
		Collection<StringFact> stateFacts = new ArrayList<StringFact>();
		Collection<String> generalStateFacts = new HashSet<String>();
		termMappedFacts_ = MultiMap.createSortedSetMultiMap();
		for (Fact stateFact : state) {
			StringFact strFact = StateSpec.toStringFact(stateFact.toString());
			// Ignore the type, goal, inequal and actions pred
			if (strFact != null) {
				if (StateSpec.getInstance().isUsefulPredicate(
						strFact.getFactName())) {
					stateFacts.add(strFact);
					generalStateFacts.add(strFact.getFactName());

					// Run through the arguments and index the fact by term
					String[] arguments = strFact.getArguments();
					for (int i = 0; i < arguments.length; i++) {
						// Ignore numerical terms
						if (!StateSpec.isNumber(arguments[i]))
							termMappedFacts_.putContains(arguments[i], strFact);
					}
				} else if (strFact.getFactName()
						.equals(StateSpec.GOALARGS_PRED)) {
					// Note down the goal predicate structure to include it in
					// rules.
					if (goalArgsPred_ == null
							|| !strFact.getArguments()[1].equals(goalArgsPred_
									.getArguments()[1])) {
						goalArgsPred_ = new StringFact(strFact);
						goalArgsPred_.replaceArguments(goalReplacements, true);
						// Add it to the observed goal predicates for
						// unification purposes
						for (String term : goalReplacements.values())
							observedGoalPredicates_.put(term, goalArgsPred_);
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
	 * Simplifies a set of facts using the learned background knowledge and
	 * invariants.
	 * 
	 * @param simplified
	 *            The facts to be simplified.
	 * @param testForIllegalRule
	 *            If the procedure is also testing for illegal rules.
	 * @param onlyEquivalencies
	 *            If only equivalent rules should be tested.
	 * @return True if the condition was simplified.
	 */
	public boolean simplifyRule(SortedSet<StringFact> simplified,
			boolean testForIllegalRule, boolean onlyEquivalencies) {
		return conditionObservations_.simplifyRule(simplified,
				testForIllegalRule, onlyEquivalencies);
	}

	/**
	 * Loads an AgentObservation serialised class from file, or if no file
	 * available, creates a new one.
	 */
	public static boolean loadAgentObservations() {
		return loadAgentObservations(StateSpec.getInstance()
				.getEnvironmentName()
				+ File.separator + SERIALISATION_FILE);
	}

	/**
	 * Loads a specific AgentObservations file. If no file available, creates a
	 * new one.
	 * 
	 * @param filepath
	 *            The file path.
	 * @return True if the agent observations were loaded.
	 */
	public static boolean loadAgentObservations(String filepath) {
		try {
			File serialisedFile = new File(AGENT_OBSERVATIONS_DIR, filepath);
			if (serialisedFile.exists()) {
				FileInputStream fis = new FileInputStream(serialisedFile);
				ObjectInputStream ois = new ObjectInputStream(fis);
				AgentObservations ao = (AgentObservations) ois.readObject();
				if (ao != null) {
					// Need to recreate the RLGG rule so it gets placed in the
					// appropriate slot.
					if (ao.actionBasedObservations_ != null) {
						for (ActionBasedObservations abo : ao.actionBasedObservations_
								.values())
							abo.rlggRule_ = null;
					}
					instance_ = ao;
					instance_.observedGoalPredicates_ = MultiMap
							.createSortedSetMultiMap();
					return true;
				}
			}
		} catch (Exception e) {
		}
		instance_ = getInstance();
		return false;
	}

	/**
	 * Gets the AgentObservations instance.
	 * 
	 * @return The instance.
	 */
	public static AgentObservations getInstance() {
		if (instance_ == null)
			instance_ = new AgentObservations();
		return instance_;
	}

	/**
	 * Gets the AgentObservations instance.
	 * 
	 * @return The instance.
	 */
	public static AgentObservations newInstance() {
		instance_ = new AgentObservations();
		return instance_;
	}

	/**
	 * An internal class to note the action-based observations.
	 * 
	 * @author Sam Sarjant
	 */
	private class ActionBasedObservations implements Serializable {
		/**
		 * The action itself, usually expressed in variables, but possibly
		 * constants if the action always takes a constant.
		 */
		private StringFact action_;

		/** The conditions observed to always be true for the action. */
		private Collection<StringFact> invariantActionConditions_;

		/** If the RLGG rule needs to be recreated. */
		private boolean recreateRLGG_ = true;

		/** The learned RLGG rule from the action observations. */
		private GuidedRule rlggRule_;

		/**
		 * The conditions that can be added to rules for specialisation.
		 * Essentially the variant action conditions both negated and normal,
		 * then simplified.
		 */
		private Collection<StringFact> specialisationConditions_;

		/** The conditions observed to sometimes be true for the action. */
		private Collection<StringFact> variantActionConditions_;

		/** The action conditions orientated towards the goal. */
		private transient Collection<StringFact> invariantGoalActionConditions_;

		/** The action conditions orientated towards the goal. */
		private transient Collection<StringFact> variantGoalActionConditions_;

		private AgentObservations getOuterType() {
			return AgentObservations.this;
		}

		/**
		 * Intersects the action conditions sets, handling numerical values as a
		 * special case.
		 * 
		 * @param actionConds
		 *            The action conditions being added.
		 * @param invariants
		 *            Invariant action conditions. Can only get smaller.
		 * @param variants
		 *            Variant action conditions. Can only get bigger.
		 * @return True if the action conditions changed, false otherwise.
		 */
		private boolean intersectActionConditions(
				Collection<StringFact> actionConds, String[] actionArgs,
				Collection<StringFact> invariants,
				Collection<StringFact> variants) {
			boolean changed = false;
			Collection<StringFact> newInvariantConditions = new HashSet<StringFact>();

			// Run through each invariant fact
			for (StringFact invFact : invariants) {
				StringFact mergedFact = Unification.getInstance().unifyFact(
						invFact, actionConds, new DualHashBidiMap(),
						new DualHashBidiMap(), actionArgs, false, true);
				if (mergedFact != null) {
					changed |= !invFact.equals(mergedFact);
					newInvariantConditions.add(mergedFact);
				} else {
					variants.add(invFact);
				}
			}
			invariants.clear();
			invariants.addAll(newInvariantConditions);

			// Add any remaining action conds to the variants
			changed |= variants.addAll(actionConds);

			return changed;
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
		private Collection<StringFact> recreateSpecialisations(
				Collection<StringFact> variants, boolean checkNegated) {
			SortedSet<StringFact> specialisations = new TreeSet<StringFact>(
					ConditionComparator.getInstance());
			for (StringFact condition : variants) {
				// Check the non-negated version
				condition = simplifyCondition(condition);
				if (condition != null) {
					specialisations.add(condition);

					// Check the negated version (only for non-types)
					if (checkNegated
							&& !StateSpec.getInstance().isTypePredicate(
									condition.getFactName())) {
						StringFact negCondition = new StringFact(condition);
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
		private StringFact simplifyCondition(StringFact condition) {
			SortedSet<StringFact> set = new TreeSet<StringFact>(
					ConditionComparator.getInstance());
			set.add(condition);
			if (simplifyRule(set, false, true)) {
				StringFact simplify = set.first();
				// If the condition is not in the invariants and if it's not
				// negated, IS in the variants, keep it.
				if (!invariantActionConditions_.contains(simplify)
						&& !invariantGoalActionConditions_.contains(simplify)) {
					if (simplify.isNegated()
							|| variantActionConditions_.contains(simplify)
							|| variantGoalActionConditions_.contains(simplify))
						return simplify;
				}
			} else
				return condition;
			return null;
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
		public boolean addActionConditions(Collection<StringFact> actionConds,
				Collection<StringFact> goalActionConds, StringFact action) {
			boolean changed = false;

			// If the goal conditions haven't been initialised, they start as
			// invariant.
			if (invariantGoalActionConditions_ == null) {
				invariantGoalActionConditions_ = new HashSet<StringFact>(
						goalActionConds);
				variantGoalActionConditions_ = new HashSet<StringFact>();
				changed = true;
			}

			// If this is the first action condition, then all the actions are
			// considered invariant.
			if (invariantActionConditions_ == null) {
				invariantActionConditions_ = new HashSet<StringFact>(
						actionConds);
				variantActionConditions_ = new HashSet<StringFact>();
				specialisationConditions_ = new HashSet<StringFact>();
				action_ = new StringFact(action);
				recreateRLGG_ = true;
				return true;
			}

			// Sort the invariant and variant conditions, making a special case
			// for numerical conditions.
			String[] actionArgs = action_.getArguments();
			changed |= intersectActionConditions(actionConds, actionArgs,
					invariantActionConditions_, variantActionConditions_);
			changed |= intersectActionConditions(goalActionConds, actionArgs,
					invariantGoalActionConditions_,
					variantGoalActionConditions_);

			// Generalise the action if necessary
			for (int i = 0; i < action_.getArguments().length; i++) {
				String argument = action_.getArguments()[i];

				// If the action isn't variable, but doesn't match with the
				// current action, generalise it.
				if ((argument.charAt(0) != '?')
						&& (!argument.equals(action.getArguments()[i]))) {
					action_.getArguments()[i] = RuleCreation
							.getVariableTermString(i);
					changed = true;
				}
			}

			if (changed) {
				specialisationConditions_ = recreateSpecialisations(
						variantActionConditions_, true);
				specialisationConditions_.addAll(recreateSpecialisations(
						variantGoalActionConditions_, false));
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
		public GuidedRule getRLGGRule() {
			// No need to recreate the rule if nothing has changed.
			if (recreateRLGG_ || rlggRule_ == null) {
				SortedSet<StringFact> ruleConds = new TreeSet<StringFact>(
						ConditionComparator.getInstance());
				// Form the replacements if the action is non-variable
				String[] replacements = null;
				for (String arg : action_.getArguments()) {
					if (arg.charAt(0) != '?') {
						replacements = action_.getArguments();
						break;
					}
				}

				for (StringFact invariant : invariantActionConditions_) {
					StringFact modFact = new StringFact(invariant);
					if (replacements != null)
						modFact.replaceArguments(replacements);
					ruleConds.add(modFact);
				}

				// Add the goal condition
				ruleConds.add(goalArgsPred_);

				// Simplify the rule conditions
				simplifyRule(ruleConds, false, false);
				if (rlggRule_ == null)
					rlggRule_ = new GuidedRule(ruleConds, action_, null);
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

		public Collection<StringFact> getSpecialisationConditions() {
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

		public void setActionConditions(Collection<StringFact> conditions) {
			specialisationConditions_ = new HashSet<StringFact>(conditions);
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
		/** The agent's beliefs about the condition inter-relations. */
		private Map<String, ConditionBeliefs> conditionBeliefs_;

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
		private Map<String, Map<Byte, ConditionBeliefs>> negatedConditionBeliefs_;

		/** The collection of unseen predicates. */
		private Collection<StringFact> unseenPreds_;

		public ConditionObservations() {
			conditionBeliefs_ = new TreeMap<String, ConditionBeliefs>();
			negatedConditionBeliefs_ = new TreeMap<String, Map<Byte, ConditionBeliefs>>();
			learnedEnvironmentRules_ = formBackgroundKnowledge();
			invariants_ = new InvariantObservations();

			unseenPreds_ = new HashSet<StringFact>();
			unseenPreds_.addAll(StateSpec.getInstance().getPredicates()
					.values());
			unseenPreds_.addAll(StateSpec.getInstance().getTypePredicates()
					.values());
		}

		/**
		 * Simplifies a rule using the observed condition associations.
		 * 
		 * @param simplified
		 *            The to-be-simplified set of conditions.
		 * @param testForIllegalRule
		 *            If the conditions are to be tested for conflicting
		 *            conditions.
		 * @param onlyEquivalencies
		 *            If the simplification only looks at equivalencies.
		 * @return True if the rule is simplified/altered.
		 */
		public boolean simplifyRule(SortedSet<StringFact> simplified,
				boolean testForIllegalRule, boolean onlyEquivalencies) {
			boolean changedOverall = false;
			// Note which facts have already been tested, so changes don't
			// restart the process.
			SortedSet<StringFact> testedFacts = new TreeSet<StringFact>(
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
				for (StringFact fact : new TreeSet<StringFact>(simplified)) {
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
								// If the simplification process changes things
								if (bckKnow.simplify(simplified,
										testForIllegalRule)) {
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

			return changedOverall;
		}

		/**
		 * Removes all non-type invariants from the conditions.
		 * 
		 * @param conditions
		 *            The conditions to remove invariants from.
		 * @return If invariants were removed.
		 */
		private boolean removeInvariants(SortedSet<StringFact> conditions) {
			boolean changed = false;
			for (StringFact invariant : invariants_.getSpecificInvariants()) {
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
		public boolean noteObservations(Collection<StringFact> stateFacts,
				Collection<String> generalStateFacts,
				Map<String, String> goalReplacements) {
			boolean changed = false;
			// Note the invariants
			changed |= invariants_
					.noteInvariants(stateFacts, generalStateFacts);

			// Use the term mapped facts to generate collections of true facts.
			changed |= recordConditionAssociations(stateFacts, goalReplacements);
			return changed;
		}

		/**
		 * Forms the background knowledge from the condition beliefs.
		 */
		private SortedSet<BackgroundKnowledge> formBackgroundKnowledge() {
			SortedSet<BackgroundKnowledge> backgroundKnowledge = new TreeSet<BackgroundKnowledge>();

			// Add the knowledge provided by the state specification
			Collection<BackgroundKnowledge> stateSpecKnowledge = StateSpec
					.getInstance().getBackgroundKnowledgeConditions();
			backgroundKnowledge.addAll(stateSpecKnowledge);
			mappedEnvironmentRules_ = MultiMap.createSortedSetMultiMap();
			for (BackgroundKnowledge bckKnow : stateSpecKnowledge) {
				Collection<String> relevantPreds = bckKnow.getRelevantPreds();
				for (String pred : relevantPreds)
					mappedEnvironmentRules_.putContains(pred, bckKnow);
			}

			// Run through every condition in the beliefs
			// Form equivalence (<=>) relations wherever possible
			for (String cond : conditionBeliefs_.keySet()) {
				ConditionBeliefs cb = conditionBeliefs_.get(cond);
				StringFact cbFact = cb.getConditionFact();

				// Assert the true facts
				for (StringFact alwaysTrue : cb.getAlwaysTrue()) {
					// Only create the relation if it is useful (not an obvious
					// type relation)
					if (shouldCreateRelation(cbFact, alwaysTrue))
						createRelation(cbFact, alwaysTrue, true,
								backgroundKnowledge, true);
				}

				// Assert the false facts
				for (StringFact neverTrue : cb.getNeverTrue()) {
					// Don't note obvious type rules.
					createRelation(cbFact, neverTrue, false,
							backgroundKnowledge, true);
				}
			}

			// Create the negated condition beliefs (only for the !A => B
			// relations)
			for (String cond : negatedConditionBeliefs_.keySet()) {
				for (ConditionBeliefs negCB : negatedConditionBeliefs_
						.get(cond).values()) {
					StringFact negCBFact = negCB.getConditionFact();
					// Only assert the !A => B assertions. The rest aren't
					// accurate.
					for (StringFact alwaysTrue : negCB.getAlwaysTrue()) {
						if (shouldCreateRelation(negCBFact, alwaysTrue))
							createRelation(negCBFact, alwaysTrue, true,
									backgroundKnowledge, false);
					}
				}
			}
			return backgroundKnowledge;
		}

		/**
		 * If a relation should be created. Ignore relations that are simply
		 * themselves (A -> A) and ignore obvious type relations.
		 * 
		 * @param cbFact
		 *            The condition beliefs fact.
		 * @param relatedFact
		 *            The related fact.
		 * @return True if the relation should be created, false otherwise.
		 */
		private boolean shouldCreateRelation(StringFact cbFact,
				StringFact relatedFact) {
			// Don't make rules to itself.
			if (cbFact.equals(relatedFact))
				return false;

			// If the fact is a type predicate, go for it.
			if (StateSpec.getInstance().isTypePredicate(cbFact.getFactName()))
				return true;
			else {
				// If the rule is a normal predicate, don't make obvious type
				// rules.
				if (StateSpec.getInstance().isTypePredicate(
						relatedFact.getFactName())) {
					int index = RuleCreation.getVariableTermIndex(relatedFact
							.getArguments()[0]);
					if (cbFact.getArgTypes()[index].equals(relatedFact
							.getFactName()))
						return false;
				}

				return true;
			}
		}

		/**
		 * Creates the background knowledge to represent the condition
		 * relations. This knowledge may be an equivalence relation if the
		 * relations between conditions have equivalences.
		 * 
		 * @param cond
		 *            The positive, usually left-side condition.
		 * @param otherCond
		 *            The possibly negated, usually right-side condition.
		 * @param negationType
		 *            The state of negation: true if not negated, false if
		 *            negated.
		 * @param relations
		 *            The set of knowledge to add to.
		 * @param checkEquivalence
		 *            If the method should check for an equivalence.
		 */
		@SuppressWarnings("unchecked")
		private void createRelation(StringFact cond, StringFact otherCond,
				boolean negationType, SortedSet<BackgroundKnowledge> relations,
				boolean checkEquivalence) {
			StringFact left = cond;
			StringFact right = otherCond;
			if (left.equals(right))
				return;
			String relation = " => ";

			// Create the basic inference relation
			BackgroundKnowledge bckKnow = null;
			if (negationType)
				bckKnow = new BackgroundKnowledge(left + relation + right,
						false);
			else
				bckKnow = new BackgroundKnowledge(left + relation + "(not "
						+ right + ")", false);
			mappedEnvironmentRules_.putContains(left.getFactName(), bckKnow);
			relations.add(bckKnow);

			// If not checking equivalences, just return.
			if (!checkEquivalence)
				return;

			// Create an equivalence relation if possible.
			// Get the relations to use for equivalency comparisons
			Collection<StringFact> otherRelations = getOtherConditionRelations(
					otherCond, negationType);

			// Don't bother with self-condition relations
			if (!otherCond.getFactName().equals(cond.getFactName())
					&& otherRelations != null && !otherRelations.isEmpty()) {
				// Replace arguments for this fact to match those seen in the
				// otherCond (anonymise variables)
				BidiMap replacementMap = new DualHashBidiMap();
				String[] otherArgs = otherCond.getArguments();
				for (int i = 0; i < otherArgs.length; i++) {
					if (!otherArgs[i].equals("?"))
						replacementMap.put(otherArgs[i], RuleCreation
								.getVariableTermString(i));
				}

				StringFact modCond = new StringFact(cond);
				modCond.replaceArguments(replacementMap, false);
				// If the fact is present, then there is an equivalency!
				if (otherRelations.contains(modCond)) {
					// Change the modArg back to its original arguments (though
					// not changing anonymous values)
					modCond.replaceArguments(replacementMap.inverseBidiMap(),
							false);

					left = modCond;
					// Order the two facts with the simplest fact on the LHS
					if (negationType && modCond.compareTo(otherCond) > 0) {
						left = otherCond;
						right = modCond;
					}
					relation = " <=> ";

					if (negationType)
						bckKnow = new BackgroundKnowledge(left + relation
								+ right, false);
					else
						bckKnow = new BackgroundKnowledge(left + relation
								+ "(not " + right + ")", false);
					mappedEnvironmentRules_.putContains(left.getFactName(),
							bckKnow);
					mappedEnvironmentRules_.putContains(right.getFactName(),
							bckKnow);
					relations.add(bckKnow);
				}
			}
		}

		/**
		 * Gets the relations or the other condition's condition beliefs to
		 * cross-compare between the sets for equivalence checking.
		 * 
		 * @param otherCond
		 *            The other condition.
		 * @param negationType
		 *            The state of negation: true if not negated, false if
		 *            negated.
		 * @return The set of facts used for comparing with equivalence.
		 */
		private Collection<StringFact> getOtherConditionRelations(
				StringFact otherCond, boolean negationType) {
			String factName = otherCond.getFactName();
			if (negationType) {
				// No negation - simply return the set given by the fact name
				if (conditionBeliefs_.containsKey(factName))
					return conditionBeliefs_.get(factName).getAlwaysTrue();
			} else {
				// Negation - return the appropriate conjunction of condition
				// beliefs
				if (negatedConditionBeliefs_.containsKey(factName)) {
					Map<Byte, ConditionBeliefs> negatedCBs = negatedConditionBeliefs_
							.get(factName);
					byte argState = determineArgState(otherCond);
					if (negatedCBs.containsKey(argState))
						return negatedCBs.get(argState).getAlwaysTrue();
				}
			}
			return null;
		}

		/**
		 * Determines the state of a string fact by encoding whether an argument
		 * is non-anonymous using bitwise indexing operations.
		 * 
		 * @param fact
		 *            The fact being analysed.
		 * @return A byte >= 0 representing the fact's arguments state.
		 */
		private byte determineArgState(StringFact fact) {
			byte state = 0;
			String[] factArgs = fact.getArguments();
			for (int argIndex = 0; argIndex < factArgs.length; argIndex++) {
				if (!factArgs[argIndex].equals("?"))
					state += 1 << argIndex;
			}
			return state;
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
				Collection<StringFact> stateFacts,
				Map<String, String> goalReplacements) {
			boolean changed = false;
			for (StringFact baseFact : stateFacts) {
				// Getting the ConditionBeliefs object
				ConditionBeliefs cb = conditionBeliefs_.get(baseFact
						.getFactName());
				if (cb == null) {
					cb = new ConditionBeliefs(baseFact.getFactName());
					conditionBeliefs_.put(baseFact.getFactName(), cb);
					observationHash_ = null;
				}

				// Create a replacement map here (excluding numerical values)
				Map<String, String> replacementMap = baseFact
						.createVariableTermReplacementMap(true);

				// Replace facts for all relevant facts and store as condition
				// beliefs.
				Collection<StringFact> relativeFacts = new HashSet<StringFact>();
				for (String term : baseFact.getArguments()) {
					Collection<StringFact> termFacts = termMappedFacts_
							.get(term);
					if (termFacts != null) {
						for (StringFact termFact : termFacts) {
							StringFact relativeFact = new StringFact(termFact);
							relativeFact
									.replaceArguments(replacementMap, false);
							relativeFacts.add(relativeFact);
						}
					}

					// Note any goal terms
					if (goalReplacements != null
							&& goalReplacements.containsKey(term)) {
						// Probably need to replace the term here for a
						// parameterisable goal term
						StringFact goalFact = new StringFact(baseFact);
						goalFact.replaceArguments(goalReplacements, false);
						changed |= observedGoalPredicates_.putContains(
								goalReplacements.get(term), goalFact);
						// Negated fact too
						StringFact negFact = new StringFact(goalFact);
						negFact.swapNegated();
						changed |= observedGoalPredicates_.putContains(
								goalReplacements.get(term), negFact);
					}
				}

				// TODO Can use these rules to note the condition ranges
				// So when condX is true, condY is always in range 0-50
				// Works for self conditions too, when cond X is true, condX is
				// in range min-max

				// Note the relative facts for the main pred and for every
				// negated pred not present
				Collection<StringFact> notRelativeFacts = new HashSet<StringFact>();
				if (cb.noteTrueRelativeFacts(relativeFacts, notRelativeFacts,
						true)) {
					changed = true;
					observationHash_ = null;
				}

				// Form the condition beliefs for the not relative facts, using
				// the relative facts as always true values (only for non-types
				// predicates)
				changed |= recordUntrueConditionAssociations(relativeFacts,
						notRelativeFacts);
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
				Collection<StringFact> trueRelativeFacts,
				Collection<StringFact> falseRelativeFacts) {
			boolean changed = false;
			for (StringFact untrueFact : falseRelativeFacts) {
				String factName = untrueFact.getFactName();

				// Getting the ConditionBeliefs object
				Map<Byte, ConditionBeliefs> untrueCBs = negatedConditionBeliefs_
						.get(factName);
				if (untrueCBs == null) {
					untrueCBs = new HashMap<Byte, ConditionBeliefs>();
					negatedConditionBeliefs_.put(factName, untrueCBs);
				}

				// Create a separate condition beliefs object for each
				// non-anonymous argument position
				String[] untrueFactArgs = untrueFact.getArguments();
				byte argState = determineArgState(untrueFact);

				ConditionBeliefs untrueCB = untrueCBs.get(argState);
				if (untrueCB == null) {
					untrueCB = new ConditionBeliefs(factName, argState);
					untrueCBs.put(argState, untrueCB);
					observationHash_ = null;
				}

				// Make the untrue fact the 'main' fact (align variables
				// properly with replacement map)
				Map<String, String> replacementMap = new HashMap<String, String>();
				for (int i = 0; i < untrueFactArgs.length; i++) {
					if (!untrueFactArgs[i].equals("?"))
						replacementMap.put(untrueFactArgs[i], RuleCreation
								.getVariableTermString(i));
				}

				Collection<StringFact> modTrueRelativeFacts = new HashSet<StringFact>();
				for (StringFact trueFact : trueRelativeFacts) {
					StringFact modTrueFact = new StringFact(trueFact);
					if (modTrueFact.replaceArguments(replacementMap, false))
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
	}

	// TEST METHODS
	public Map<String, ConditionBeliefs> getConditionBeliefs() {
		return conditionObservations_.conditionBeliefs_;
	}

	public Collection<BackgroundKnowledge> getLearnedBackgroundKnowledge() {
		return conditionObservations_.learnedEnvironmentRules_;
	}

	public Map<String, Map<Byte, ConditionBeliefs>> getNegatedConditionBeliefs() {
		return conditionObservations_.negatedConditionBeliefs_;
	}

	public void setActionConditions(String action,
			Collection<StringFact> conditions) {
		getActionBasedObservation(action).setActionConditions(conditions);
		observationHash_ = null;
		inactivity_ = 0;
	}

	public void setBackgroundKnowledge(SortedSet<BackgroundKnowledge> backKnow) {
		conditionObservations_.learnedEnvironmentRules_ = backKnow;
	}
}
