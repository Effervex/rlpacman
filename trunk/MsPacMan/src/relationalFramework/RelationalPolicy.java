package relationalFramework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import cerrla.Module;
import cerrla.PolicyGenerator;

import relationalFramework.util.ArgumentComparator;
import relationalFramework.util.MultiMap;

import jess.QueryResult;
import jess.Rete;
import jess.ValueVector;

/**
 * This class represents a policy that the agent can use.
 * 
 * @author Samuel J. Sarjant
 */
public class RelationalPolicy implements Serializable {
	private static final long serialVersionUID = 1936194991813447116L;
	public static final char DELIMITER = '#';
	public static final String PREFIX = "Policy";
	/** Whether to add rules to the triggered rule set or not. */
	private boolean noteTriggered_;
	/** The rules of this policy, organised in a deterministic list format. */
	private List<RelationalRule> policyRules_;
	/** The number of rules added to this policy (excluding modular) */
	private int policySize_;
	/** The triggered rules in the policy */
	private Set<RelationalRule> triggeredRules_;

	/**
	 * A constructor for creating a new policy.
	 * 
	 * @param policySize
	 *            The maximum size of the policy.
	 */
	public RelationalPolicy() {
		policyRules_ = new ArrayList<RelationalRule>();
		triggeredRules_ = new HashSet<RelationalRule>();
		policySize_ = 0;
	}

	/**
	 * A constructor for a new policy using the same rules from an old policy.
	 * 
	 * @param policy
	 *            The old policy.
	 */
	public RelationalPolicy(RelationalPolicy policy) {
		this();
		policyRules_.addAll(policy.policyRules_);
		policySize_ = policy.policySize_;
	}

	/**
	 * Checks if a rule contains constant facts which can be achieved using
	 * modules. If so, the rules are loaded and internally added to the policy.
	 * 
	 * @param rule
	 *            The rule being checked.
	 */
	private void checkModular(RelationalRule rule) {
		Collection<GoalCondition> constantConditions = rule
				.getConstantConditions();
		if (constantConditions == null)
			return;
		// Load every necessary module
		for (GoalCondition constantCondition : constantConditions) {
			Module module = Module.loadModule(StateSpec.getInstance()
					.getEnvironmentName(), constantCondition.toString());
			// If the module exists
			if (module != null) {
				// Put the parameters into an arraylist
				ArrayList<String> parameters = new ArrayList<String>();
				RelationalPredicate cond = constantCondition.getFact();
				// Extract the parameters used in the constant
				// conditions
				for (String arg : cond.getArguments()) {
					// If modules are recursive, set up the parameters
					// accordingly.
					if (!rule.isLoadedModuleRule())
						parameters.add(arg);
					else {
						int index = rule.getQueryParameters().indexOf(arg);
						if (index != -1)
							parameters.add(rule.getModuleParameters()
									.get(index));
						else
							parameters.add(arg);
					}
				}

				// Add the module rules.
				for (RelationalRule gr : module.getModuleRules()) {
					gr.setModularParameters(parameters);
					checkModular(gr);
					policyRules_.add(gr);
				}
			}
		}
	}

	/**
	 * Evaluates a single rule against the state to see which actions fire.
	 * 
	 * @param rule
	 *            The rule to be evaluated.
	 * @param state
	 *            The state to be evaluated against.
	 * @param validActions
	 *            The set of valid actions the agent can take at this state.
	 * @param activatedActions
	 *            The (possibly null) to-be-filled activated actions.
	 * @return A collection of actions which the rule creates.
	 */
	private Collection<FiredAction> evaluateRule(RelationalRule rule,
			Rete state, SortedSet<String[]> validActions,
			SortedSet<String[]> activatedActions) throws Exception {
		Collection<FiredAction> returnedActions = new TreeSet<FiredAction>();

		// Forming the query
		String query = StateSpec.getInstance().getRuleQuery(rule);
		// If there are parameters, temp or concrete, insert them here
		ValueVector vv = new ValueVector();
		if (rule.getQueryParameters() != null) {
			if (rule.getParameters() != null) {
				for (String param : rule.getParameters())
					vv.add(param);
			} else {
				// Use anonymous placeholder
				for (int i = 0; i < rule.getQueryParameters().size(); i++)
					vv.add(StateSpec.ANONYMOUS);
			}
		}
		QueryResult results = state.runQueryStar(query, vv);

		// If there is at least one result
		if (results.next()) {
			// For each possible replacement
			do {
				// Get the rule action, without brackets
				RelationalPredicate action = new RelationalPredicate(
						rule.getAction());

				// Find the arguments.
				String[] arguments = action.getArguments();
				for (int i = 0; i < arguments.length; i++) {
					// If the action is variable, use the replacement
					if (arguments[i].charAt(0) == '?')
						arguments[i] = results.getSymbol(arguments[i]
								.substring(1));
				}

				// Check this is a valid action
				if (isValidAction(arguments, validActions)) {
					if (activatedActions != null)
						activatedActions.add(arguments);

					// Use the found action set as a result.
					returnedActions.add(new FiredAction(action, rule, this));
				}
			} while (results.next());
		}
		results.close();

		return returnedActions;
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
	 * Adds a rule to the set of triggered rules. Some circumstances may forbid
	 * the rule being added.
	 * 
	 * @param rule
	 *            The rule to be added.
	 * @return True if the rule was successfully added, or is already present.
	 *         False if the rule was not allowed to be added.
	 */
	protected boolean addTriggeredRule(RelationalRule rule) {
		// If the rule is a loaded module rule, or the agent isn't noting
		// triggered rules, return false.
		if (rule.isLoadedModuleRule() || !noteTriggered_)
			return false;

		triggeredRules_.add(rule);
		return true;
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
	public void addRule(RelationalRule rule, boolean checkModular,
			boolean isLGGRule) {
		if (!policyRules_.contains(rule)) {
			// Check if the rule contains constant facts that could invoke
			// modular rules.
			if (checkModular && PolicyGenerator.getInstance().useModules_)
				checkModular(rule);
			policyRules_.add(rule);
			policySize_++;
		}
	}

	/**
	 * If the policy contains a rule.
	 * 
	 * @param rule
	 *            The rule being checked.
	 * @return True if the rule is within the policy, false otherwise.
	 */
	public boolean contains(RelationalRule rule) {
		return policyRules_.contains(rule);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RelationalPolicy other = (RelationalPolicy) obj;
		if (policyRules_ == null) {
			if (other.policyRules_ != null)
				return false;
		} else if (!policyRules_.equals(other.policyRules_))
			return false;
		if (policySize_ != other.policySize_)
			return false;
		return true;
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
	 * @param actionsReturned
	 *            The number of actions to be returned, or if -1, all actions.
	 * @param handCoded
	 *            If the policy is a hand-coded test one.
	 * @param noteTriggered
	 *            If this policy is noting the rules fired as triggered. Usually
	 *            deactivated after agent has found internal goal.
	 */
	public PolicyActions evaluatePolicy(Rete state,
			MultiMap<String, String[]> validActions,
			Map<String, String> goalReplacements, int actionsReturned,
			boolean handCoded, boolean noteTriggered) {
		noteTriggered_ = noteTriggered;
		PolicyActions actionSwitch = new PolicyActions();
		MultiMap<String, String[]> activatedActions = MultiMap
				.createSortedSetMultiMap(ArgumentComparator.getInstance());
		int actionsFound = 0;
		int actionsReturnedModified = (actionsReturned <= -1) ? Integer.MAX_VALUE
				: actionsReturned;

		try {
			// First evaluate the RLGG rules (if any). If the actions there
			// don't match up to the activated actions, covering will be
			// required.
			for (RelationalRule rlgg : PolicyGenerator.getInstance()
					.getRLGGRules()) {
				SortedSet<String[]> rlggActions = new TreeSet<String[]>(
						ArgumentComparator.getInstance());
				evaluateRule(rlgg, state,
						validActions.getSortedSet(rlgg.getActionPredicate()),
						rlggActions);
				activatedActions.putCollection(rlgg.getActionPredicate(),
						rlggActions);
			}

			// Next trigger covering, storing the rules in the policy if
			// required
			if (!handCoded) {
				List<RelationalRule> coveredRules = PolicyGenerator
						.getInstance().triggerRLGGCovering(state, validActions,
								goalReplacements, activatedActions);
				// If the policy is empty, store the rules in it.
				if (activatedActions.isKeysEmpty() && coveredRules != null) {
					Collections.shuffle(coveredRules, PolicyGenerator.random_);
					// Add any new rules to the policy
					for (RelationalRule gr : coveredRules) {
						if (!policyRules_.contains(gr))
							policyRules_.add(gr);
					}
				}
			}

			// Next, evaluate the rest of the policy until an adequate number of
			// rules are evaluated (usually 1 or all; may be the entire policy).
			Iterator<RelationalRule> iter = policyRules_.iterator();
			while (iter.hasNext() && actionsFound < actionsReturnedModified) {
				RelationalRule polRule = iter.next();
				Collection<FiredAction> firedActions = evaluateRule(
						polRule,
						state,
						validActions.getSortedSet(polRule.getActionPredicate()),
						null);
				actionSwitch.addFiredRule(firedActions);
				actionsFound += firedActions.size();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return actionSwitch;
	}

	/**
	 * Gets the rules that fired from this policy.
	 * 
	 * @return The rules that fired in this policy
	 */
	public Set<RelationalRule> getFiringRules() {
		return triggeredRules_;
	}

	/**
	 * Gets the rules that this policy is made up of.
	 * 
	 * @param excludeModular
	 *            If excluding the modular rules of the policy.
	 * @return The rule for the policy.
	 */
	public List<RelationalRule> getPolicyRules(boolean excludeModular) {
		if (excludeModular) {
			List<RelationalRule> rules = new ArrayList<RelationalRule>();
			for (RelationalRule gr : policyRules_) {
				if (!gr.isLoadedModuleRule())
					rules.add(gr);
			}
			return rules;
		}
		return policyRules_;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((policyRules_ == null) ? 0 : policyRules_.hashCode());
		result = prime * result + policySize_;
		return result;
	}

	/**
	 * Apply arguments to any parameterised rules contained within this policy.
	 * 
	 * @param goalState
	 *            The arguments to apply to the parameters.
	 */
	public void parameterArgs(Map<String, String> goalArgs) {
		List<String> params = null;
		if (goalArgs != null) {
			params = new ArrayList<String>();
			for (int i = 0; i < goalArgs.size(); i++)
				params.add(goalArgs.get(StateSpec.createGoalTerm(i)));
		}

		// Set the parameters for the policy rules.
		for (RelationalRule gr : policyRules_) {
			// Special case for loaded module rules - they have their parameter
			// args swapped.
			if (gr.isLoadedModuleRule()) {
				List<String> modParams = new ArrayList<String>();
				for (String modParam : gr.getModuleParameters())
					modParams.add(goalArgs.get(modParam));
				gr.setParameters(modParams);
			} else
				gr.setParameters(params);
		}
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
	 * A method for displaying only the rules used within the policy.
	 * 
	 * @return The policy in string format, minus the unused rules.
	 */
	public String toOnlyUsedString() {
		if (policyRules_.isEmpty())
			return "<EMPTY POLICY>";

		StringBuffer buffer = new StringBuffer("Policy:\n");
		for (RelationalRule rule : policyRules_) {
			// Don't display module rules or unused rules
			if (!rule.isLoadedModuleRule() && triggeredRules_.contains(rule)) {
				buffer.append(rule.toNiceString() + "\n");
			}
		}
		return buffer.toString();
	}

	@Override
	public String toString() {
		if (policyRules_.isEmpty())
			return "<EMPTY POLICY>";

		StringBuffer buffer = new StringBuffer("Policy");
		buffer.append(":\n");

		for (RelationalRule rule : policyRules_) {
			if (!rule.isLoadedModuleRule()) {
				buffer.append(rule.toNiceString() + "\n");
			} else {
				buffer.append("MODULAR: " + rule.toNiceString() + "\n");
			}
		}
		return buffer.toString();
	}
}