package relationalFramework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.collections.BidiMap;

import relationalFramework.agentObservations.AgentObservations;

import jess.QueryResult;
import jess.Rete;
import jess.ValueVector;

/**
 * This class represents a policy that the agent can use.
 * 
 * @author Samuel J. Sarjant
 */
public class Policy implements Serializable {
	private static final long serialVersionUID = -8362692831702469438L;
	public static final String PREFIX = "Policy";
	public static final char DELIMITER = '#';
	/** The rules of this policy, organised in a deterministic list format. */
	private List<GuidedRule> policyRules_;
	/** The LGG rules added to the end of the policy. */
	private Set<GuidedRule> rlggRules_;
	/** The triggered rules in the policy */
	private Set<GuidedRule> triggeredRules_;
	/** The number of rules added to this policy (excluding modular) */
	private int policySize_;
	/** Whether to add rules to the triggered rule set or not. */
	private boolean noteTriggered_;

	/**
	 * A constructor for creating a new policy.
	 * 
	 * @param policySize
	 *            The maximum size of the policy.
	 */
	public Policy() {
		policyRules_ = new ArrayList<GuidedRule>();
		triggeredRules_ = new HashSet<GuidedRule>();
		rlggRules_ = new HashSet<GuidedRule>();
		policySize_ = 0;
	}

	/**
	 * A constructor for a new policy using the same rules from an old policy.
	 * 
	 * @param policy
	 *            The old policy.
	 */
	public Policy(Policy policy) {
		this();
		policyRules_.addAll(policy.policyRules_);
		rlggRules_.addAll(policy.rlggRules_);
		policySize_ = policy.policySize_;
	}

	/**
	 * Adds a rule to the policy, adding necessary modular rules if the rule
	 * contains constant facts.
	 * 
	 * @param rule
	 *            The rule to be added.
	 * @param checkModular
	 *            If we're checking for modular facts.
	 * @param isLGGRule
	 *            If the rule being added is an LGG rule added by default.
	 */
	public void addRule(GuidedRule rule, boolean checkModular, boolean isLGGRule) {
		if (!policyRules_.contains(rule)) {
			// Check if the rule contains constant facts that could invoke
			// modular rules.
			if (checkModular && PolicyGenerator.getInstance().useModules_)
				checkModular(rule);
			policyRules_.add(rule);
			policySize_++;
			if (isLGGRule)
				rlggRules_.add(rule);
		}
	}

	/**
	 * Checks if a rule contains constant facts which can be achieved using
	 * modules. If so, the rules are loaded and internally added to the policy.
	 * 
	 * @param rule
	 *            The rule being checked.
	 */
	private void checkModular(GuidedRule rule) {
		ConstantPred constantConditions = rule.getConstantConditions();
		if (constantConditions == null)
			return;
		String modName = constantConditions.toString();

		Module module = Module.loadModule(StateSpec.getInstance()
				.getEnvironmentName(), modName);
		// If the module exists
		if (module != null) {
			// Put the parameters into an arraylist
			ArrayList<String> parameters = new ArrayList<String>();
			for (StringFact cond : constantConditions.getFacts()) {
				// Extract the parameters used in the constant
				// conditions
				for (String arg : cond.getArguments()) {
					// May need to replace parameters if modular is
					// recursive
					if (rule.getParameters() != null) {
						parameters.add(rule.getReplacementParameter(arg));
					} else {
						parameters.add(arg);
					}
				}
			}

			// Add the module rules.
			for (GuidedRule gr : module.getModuleRules()) {
				gr.setParameters(parameters);
				checkModular(gr);
				policyRules_.add(gr);
			}
		}
	}

	/**
	 * If the policy contains a rule.
	 * 
	 * @param rule
	 *            The rule being checked.
	 * @return True if the rule is within the policy, false otherwise.
	 */
	public boolean contains(GuidedRule rule) {
		return policyRules_.contains(rule);
	}

	/**
	 * Gets the rules that fired from this policy.
	 * 
	 * @return The rules that fired in this policy
	 */
	public Set<GuidedRule> getFiringRules() {
		return triggeredRules_;
	}

	/**
	 * Checks if a rule is an RLGG rule added automatically to the end of
	 * policies. Not necessarily if the rule is an RLGG rule, as those can be
	 * added manually too.
	 * 
	 * @param rule
	 *            The rule being checked.
	 * @return True if the rule was an automatically added RLGG rule.
	 */
	public boolean isRLGGRule(GuidedRule rule) {
		return rlggRules_.contains(rule);
	}

	/**
	 * Gets the rules that this policy is made up of.
	 * 
	 * @param excludeModular
	 *            If excluding the modular rules of the policy.
	 * @return The rule for the policy.
	 */
	public List<GuidedRule> getPolicyRules(boolean excludeModular) {
		if (excludeModular) {
			List<GuidedRule> rules = new ArrayList<GuidedRule>();
			for (GuidedRule gr : policyRules_) {
				if (!gr.isLoadedModuleRule())
					rules.add(gr);
			}
			return rules;
		}
		return policyRules_;
	}

	/**
	 * Returns the size of the policy (number of non-modular rules in it).
	 * 
	 * @return The size of the policy.
	 */
	public int size() {
		return policySize_;
	}

	/**
	 * Apply arguments to any parameterised rules contained within this policy.
	 * 
	 * @param goalState
	 *            The arguments to apply to the parameters.
	 */
	public void parameterArgs(String[] goalArgs) {
		List<String> params = null;
		if (goalArgs != null) {
			params = new ArrayList<String>();
			for (String arg : goalArgs)
				params.add(arg);
		}

		// Set the parameters for the policy rules.
		for (GuidedRule gr : policyRules_) {
			gr.setParameters(params);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((rlggRules_ == null) ? 0 : rlggRules_.hashCode());
		result = prime * result + (noteTriggered_ ? 1231 : 1237);
		result = prime * result
				+ ((policyRules_ == null) ? 0 : policyRules_.hashCode());
		result = prime * result + policySize_;
		result = prime * result
				+ ((triggeredRules_ == null) ? 0 : triggeredRules_.hashCode());
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
		Policy other = (Policy) obj;
		if (rlggRules_ == null) {
			if (other.rlggRules_ != null)
				return false;
		} else if (!rlggRules_.equals(other.rlggRules_))
			return false;
		if (noteTriggered_ != other.noteTriggered_)
			return false;
		if (policyRules_ == null) {
			if (other.policyRules_ != null)
				return false;
		} else if (!policyRules_.equals(other.policyRules_))
			return false;
		if (policySize_ != other.policySize_)
			return false;
		if (triggeredRules_ == null) {
			if (other.triggeredRules_ != null)
				return false;
		} else if (!triggeredRules_.equals(other.triggeredRules_))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (policyRules_.isEmpty())
			return "<EMPTY POLICY>";

		StringBuffer buffer = new StringBuffer("Policy");
		if (PolicyGenerator.debugMode_) {
			buffer.append(" (Goal: " + StateSpec.getInstance().formGoalString()
					+ ")");
		}
		buffer.append(":\n");

		for (GuidedRule rule : policyRules_) {
			if (!rule.isLoadedModuleRule()) {
				if (!isRLGGRule(rule))
					buffer.append(rule.toNiceString() + "\n");
			} else {
				buffer.append("MODULAR: " + rule.toNiceString() + "\n");
			}
		}
		return buffer.toString();
	}

	/**
	 * A method for displaying only the rules used within the policy.
	 * 
	 * @return The policy in string format, minus the unused rules.
	 */
	public String toOnlyUsedString() {
		if (policyRules_.isEmpty())
			return "<EMPTY POLICY>";

		StringBuffer buffer = new StringBuffer("Policy:\n");
		for (GuidedRule rule : policyRules_) {
			// Don't display module rules or unused rules
			if (!rule.isLoadedModuleRule() && triggeredRules_.contains(rule)) {
				buffer.append(rule.toNiceString() + "\n");
			}
		}
		return buffer.toString();
	}

	/**
	 * Evaluates a policy for a number of firing rules, and switches on the
	 * necessary rules.
	 * 
	 * @param state
	 *            The current state in predicates.
	 * @param validActions
	 *            The set of valid actions to choose from in the state.
	 * @param goalReplacements
	 *            The goal term replacements.
	 * @param actionSwitch
	 *            The current actions.
	 * @param actionsReturned
	 *            The number of actions to be returned, or if -1, all actions.
	 * @param optimal
	 *            If the policy is an optimal test one.
	 * @param alreadyCovered
	 *            If the policy has already covered this iteration (due to
	 *            recursive calls).
	 * @param noteTriggered
	 *            If this policy is noting the rules fired as triggered. Usually
	 *            deactivated after agent has found internal goal.
	 */
	@SuppressWarnings("unchecked")
	public ActionChoice evaluatePolicy(Rete state,
			MultiMap<String, String[]> validActions, BidiMap goalReplacements,
			ActionChoice actionSwitch, int actionsReturned, boolean optimal,
			boolean alreadyCovered, boolean noteTriggered) {
		noteTriggered_ = noteTriggered;
		actionSwitch.switchOffAll();
		MultiMap<String, String[]> activatedActions = MultiMap
				.createSortedSetMultiMap(ArgumentComparator.getInstance());

		int actionsReturnedModified = (actionsReturned <= -1) ? Integer.MAX_VALUE
				: actionsReturned;
		// Check every slot, from top-to-bottom until enough have activated (as
		// per actionsReturned)
		// TODO Evaluate RLGG rules first, so policy evaluation can stop early
		// if an action has been found (not applicable in environments where all
		// actions are required.)
		int actionsFound = 0;
		Iterator<GuidedRule> iter = policyRules_.iterator();
		while (iter.hasNext()) {
			GuidedRule gr = iter.next();

			// Find the result set
			try {
				// Forming the query
				String query = StateSpec.getInstance().getRuleQuery(gr);
				// If there are parameters, temp or concrete, insert them here
				ValueVector vv = new ValueVector();
				if (gr.getQueryParameters() != null) {
					if (gr.getParameters() != null) {
						for (String param : gr.getParameters())
							vv.add(param);
					} else {
						// Use anonymous placeholder
						for (int i = 0; i < gr.getQueryParameters().size(); i++)
							vv.add(StateSpec.ANONYMOUS);
					}
				}
				QueryResult results = state.runQueryStar(query, vv);

				// If there is at least one result
				if (results.next()) {
					List<StringFact> actionsCollection = new ArrayList<StringFact>();

					// For each possible replacement
					do {
						// Get the rule action, without brackets
						StringFact action = new StringFact(gr.getAction());

						// Find the arguments.
						String[] arguments = action.getArguments();
						for (int i = 0; i < arguments.length; i++) {
							// If the action is variable, use the replacement
							if (arguments[i].charAt(0) == '?')
								arguments[i] = results.getSymbol(arguments[i]
										.substring(1));
						}

						// Check this is a valid action
						if (isValidAction(arguments, validActions
								.getSortedSet(action.getFactName()))) {
							activatedActions.putContains(action.getFactName(),
									arguments);

							// Use the found action set as a result.
							if (canAddRule(gr, actionsFound, actionsReturned))
								actionsCollection.add(action);
						}
					} while (results.next());

					if (actionsFound < actionsReturnedModified) {
						if ((actionsFound + actionsCollection.size()) > actionsReturnedModified) {
							Collections.shuffle(actionsCollection,
									PolicyGenerator.random_);
							actionsCollection = actionsCollection.subList(0,
									actionsReturnedModified - actionsFound);
						}

						// Turn on the actions
						if (canAddRule(gr, actionsFound, actionsReturned))
							actionSwitch.switchOn(new RuleAction(gr,
									actionsCollection, this));
					}
					actionsFound += actionsCollection.size();
				}
				results.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// If optimal, just exit
		if (optimal)
			return actionSwitch;

		// If the policy didn't generate enough rules, cover a set of new rules
		// for each action.
		if (!alreadyCovered) {
			if (actionsFound < actionsReturnedModified) {
				List<GuidedRule> coveredRules = PolicyGenerator.getInstance()
						.triggerRLGGCovering(state, validActions,
								goalReplacements, activatedActions, true);

				if (coveredRules != null) {
					// Add any new rules to the policy
					for (GuidedRule gr : coveredRules) {
						if (!policyRules_.contains(gr))
							policyRules_.add(gr);
					}
					evaluatePolicy(state, validActions, goalReplacements,
							actionSwitch, actionsReturned, optimal, true,
							noteTriggered);
				}
			} else {
				PolicyGenerator.getInstance()
						.triggerRLGGCovering(state, validActions,
								goalReplacements, activatedActions, false);
			}
		}

		return actionSwitch;
	}

	/**
	 * Checks if this action is in the valid actions.
	 * 
	 * @param actionArgs
	 *            The action args being checked.
	 * @param validArgs
	 *            The set of valid action args.
	 * @return True if the action is valid, false otherwise.
	 */
	private boolean isValidAction(String[] actionArgs,
			SortedSet<String[]> validArgs) {
		// If there are no chances for this action at all, return false.
		if (validArgs == null)
			return false;

		if (validArgs.contains(actionArgs))
			return true;
		return false;
	}

	/**
	 * Checks if a rule can be added to the actions switch, which involves
	 * whether it is an automatically added covered rule or not or if the number
	 * of actions returned is already full. If it is an covered rule, it will
	 * only be added if no other rules have been added (also, further covered
	 * rules will be added in this way too).
	 * 
	 * @param gr
	 *            The rule being possibly added.
	 * @param actionsFound
	 *            The number of actions found.
	 * @param actionsReturned
	 *            the number of actions returned.
	 * @return True if the rule can be added, false otherwise.
	 */
	private boolean canAddRule(GuidedRule gr, int actionsFound,
			int actionsReturned) {
		int actionsReturnedModified = (actionsReturned <= -1) ? Integer.MAX_VALUE
				: actionsReturned;
		// If we already have enough actions, don't add it.
		if (actionsFound >= actionsReturnedModified)
			return false;

		// If adding a covered rule to an infinite action list, don't add it if
		// other actions have been.
		if (isRLGGRule(gr) && (actionsReturned <= -1) && (actionsFound > 0)) {
			return false;
		}

		return true;
	}

	/**
	 * Adds a rule to the set of triggered rules. Some circumstances may forbid
	 * the rule being added.
	 * 
	 * @param rule
	 *            The rule to be added.
	 * @return True if the rule was successfully added, or is already present.
	 *         False if the rule was not allowed to be added.
	 */
	protected boolean addTriggeredRule(GuidedRule rule) {
		// If the rule is a loaded module rule, or the agent isn't noting
		// triggered rules, return false.
		if (rule.isLoadedModuleRule() || !noteTriggered_)
			return false;

		triggeredRules_.add(rule);
		return true;
	}
}