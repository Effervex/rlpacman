package relationalFramework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jess.QueryResult;
import jess.Rete;
import jess.ValueVector;

/**
 * This class represents a policy that the agent can use.
 * 
 * @author Samuel J. Sarjant
 */
public class Policy {
	public static final String PREFIX = "Policy";
	public static final char DELIMITER = '#';
	/** The rules of this policy, organised in a deterministic list format. */
	private List<GuidedRule> policyRules_;
	/** The LGG rules added to the end of the policy. */
	private Set<GuidedRule> lggRules_;
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
		lggRules_ = new HashSet<GuidedRule>();
		policySize_ = 0;
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
				lggRules_.add(rule);
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
	 * Checks if a rule is an LGG rule added automatically to the end of
	 * policies. Not necessarily if the rule is an LGG rule, as those can be
	 * added manually too.
	 * 
	 * @param rule
	 *            The rule being checked.
	 * @return True if the rule was an automatically added LGG rule.
	 */
	public boolean isCoveredRule(GuidedRule rule) {
		return lggRules_.contains(rule);
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
	public void parameterArgs(List<ValueVector> goalState) {
		List<String> params = null;
		if (goalState != null) {
			params = new ArrayList<String>();
			try {
				for (ValueVector vv : goalState) {
					for (int i = 0; i < vv.size(); i++) {
						params.add(vv.get(i).stringValue(null));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
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
				+ ((lggRules_ == null) ? 0 : lggRules_.hashCode());
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
		if (lggRules_ == null) {
			if (other.lggRules_ != null)
				return false;
		} else if (!lggRules_.equals(other.lggRules_))
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
		return toString(true);
	}

	/**
	 * A method for displaying the policy with optional modular rules.
	 * 
	 * @param withModules
	 *            If displaying modules.
	 * @return The policy in string format.
	 */
	public String toString(boolean withModules) {
		if (policyRules_.isEmpty())
			return "<EMPTY POLICY>";

		StringBuffer buffer = new StringBuffer("Policy:\n");
		for (GuidedRule rule : policyRules_) {
			if (!rule.isLoadedModuleRule()) {
				if (!isCoveredRule(rule))
					buffer.append(StateSpec.getInstance().encodeRule(rule)
							+ "\n");
			} else if (withModules) {
				buffer.append("MODULAR: "
						+ StateSpec.getInstance().encodeRule(rule) + "\n");
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
	public ActionChoice evaluatePolicy(Rete state,
			MultiMap<String, String> validActions, ActionChoice actionSwitch,
			int actionsReturned, boolean optimal, boolean alreadyCovered,
			boolean noteTriggered) {
		noteTriggered_ = noteTriggered;
		actionSwitch.switchOffAll();
		MultiMap<String, String> activatedActions = new MultiMap<String, String>();

		int actionsReturnedModified = (actionsReturned <= -1) ? Integer.MAX_VALUE
				: actionsReturned;
		// Check every slot, from top-to-bottom until enough have activated (as
		// per actionsReturned)
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
				if (gr.getParameters() != null) {
					for (String param : gr.getParameters())
						vv.add(param);
				}
				QueryResult results = state.runQueryStar(query, vv);

				// If there is at least one result
				if (results.next()) {
					List<StringFact> actionsList = new ArrayList<StringFact>();

					// For each possible replacement
					do {
						// Get the rule action, without brackets
						StringFact action = new StringFact(gr.getAction());

						// Find the arguments.
						StringBuffer args = new StringBuffer();
						boolean first = true;
						String[] arguments = action.getArguments();
						for (int i = 0; i < arguments.length; i++) {
							if (arguments[i].charAt(0) == '?')
								arguments[i] = results.getSymbol(arguments[i]
										.substring(1));

							if (!first)
								args.append(" ");
							args.append(arguments[i]);
							first = false;
						}

						activatedActions.putContains(action.getFactName(), args
								.toString());

						// Use the found action set as a result.
						if (canAddRule(gr, actionsFound, actionsReturned))
							actionsList.add(action);
					} while (results.next());

					// Trim down the action list as it may contain too many
					// actions
					if (actionsFound < actionsReturnedModified) {
						if ((actionsFound + actionsList.size()) > actionsReturnedModified) {
							Collections.shuffle(actionsList,
									PolicyGenerator.random_);
							actionsList = actionsList.subList(0,
									actionsReturnedModified - actionsFound);
						}

						// Turn on the actions
						if (canAddRule(gr, actionsFound, actionsReturned))
							actionSwitch.switchOn(new RuleAction(gr,
									actionsList, this));
					}
					actionsFound += actionsList.size();
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
						.triggerCovering(state, validActions, activatedActions,
								true);

				if (coveredRules != null) {
					// Add any new rules to the policy
					for (GuidedRule gr : coveredRules) {
						if (!policyRules_.contains(gr))
							policyRules_.add(gr);
					}
					evaluatePolicy(state, validActions, actionSwitch,
							actionsReturned, optimal, true, noteTriggered);
				}
			} else {
				PolicyGenerator.getInstance().triggerCovering(state,
						validActions, activatedActions, false);
			}
		}

		return actionSwitch;
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
		if (isCoveredRule(gr) && (actionsReturned <= -1) && (actionsFound > 0)) {
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