package relationalFramework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mandarax.kernel.Fact;
import org.mandarax.kernel.InferenceEngine;
import org.mandarax.kernel.KnowledgeBase;
import org.mandarax.kernel.Query;
import org.mandarax.kernel.Replacement;
import org.mandarax.kernel.ResultSet;
import org.mandarax.kernel.Rule;
import org.mandarax.kernel.Term;
import org.mandarax.kernel.VariableTerm;
import org.mandarax.util.LogicFactorySupport;

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
	/** The triggered rules in the policy */
	private Set<GuidedRule> triggeredRules_;

	/**
	 * A constructor for creating a new policy.
	 * 
	 * @param policySize
	 *            The maximum size of the policy.
	 */
	public Policy() {
		policyRules_ = new ArrayList<GuidedRule>();
		triggeredRules_ = new HashSet<GuidedRule>();
	}

	/**
	 * Adds a rule to the policy, with it's placement affected
	 * 
	 * @param rule
	 *            The rule to be added.
	 */
	public void addRule(GuidedRule rule) {
		if (!policyRules_.contains(rule))
			policyRules_.add(rule);
	}

	/**
	 * Gets the rules that fired from this policy.
	 * 
	 * @return The rules that fired in this policy
	 */
	public Collection<GuidedRule> getFiringRules() {
		return triggeredRules_;
	}

	@Override
	public String toString() {
		if (policyRules_.isEmpty())
			return "<EMPTY POLICY>";

		StringBuffer buffer = new StringBuffer("Policy:\n");
		for (GuidedRule rule : policyRules_) {
			buffer.append(StateSpec.encodeRule(rule.getRule()) + "\n");
		}
		return buffer.toString();
	}

	/**
	 * Evaluates a policy for a number of firing rules, and switches on the
	 * necessary rules.
	 * 
	 * @param state
	 *            The current state in predicates.
	 * @param actionSwitch
	 *            The current actions.
	 * @param actionsReturned
	 *            The number of actions to be returned.
	 */
	public void evaluatePolicy(KnowledgeBase state, ActionSwitch actionSwitch,
			int actionsReturned) {
		// Logic constructs
		LogicFactorySupport factorySupport = new LogicFactorySupport(
				PolicyGenerator.getInstance().getLogicFactory());
		InferenceEngine ie = PolicyGenerator.getInstance().getInferenceEngine();

		// A table for already completed requests
		Map<RuleCondition, ResultSet> resultTable = new HashMap<RuleCondition, ResultSet>();

		// Check every slot, from top-to-bottom until one activates
		int actionsFound = 0;
		Iterator<GuidedRule> iter = policyRules_.iterator();
		while ((actionsFound < actionsReturned) && (iter.hasNext())) {
			GuidedRule gr = iter.next();
			Rule rule = gr.getRule();

			// Setting up the necessary variables
			RuleCondition ruleConds = new RuleCondition(rule.getBody());
			ResultSet results = resultTable.get(ruleConds);

			// Find the result set
			// TODO Evaluate rules in a step-wise fashion, to infer how much of
			// a rule fires
			try {
				if (results == null) {
					Fact[] ruleConditions = ruleConds.getFactArray();

					// Forming the query
					Query query = factorySupport.query(ruleConditions, rule
							.toString());

					results = ie.query(query, state, InferenceEngine.ALL,
							InferenceEngine.BUBBLE_EXCEPTIONS);
					resultTable.put(ruleConds, results);
				}

				// If there is at least one result
				if (results.next()) {
					triggeredRules_.add(gr);
					List<Fact> actionResults = new ArrayList<Fact>();
					// For each possible replacement
					do {
						Map<Term, Term> replacementMap = results.getResults();
						Collection<Replacement> replacements = new ArrayList<Replacement>();
						// Find the replacements for the variable terms
						// in the action
						for (Term var : rule.getHead().getTerms()) {
							if (var instanceof VariableTerm) {
								replacements.add(new Replacement(var,
										replacementMap.get(var)));
							} else {
								replacements.add(new Replacement(var, var));
							}
						}

						// TODO Check that the action is within the valid action
						// set
						// Apply the replacements and add the fact to
						// the set
						Fact groundAction = rule.getHead().applyToFact(
								replacements);
						// If the action is ground
						if (!actionResults.contains(groundAction))
							actionResults.add(groundAction);
					} while (results.next());

					// Use the found action set as a result.
					actionSwitch.switchOn(actionResults, actionsFound);
					actionsFound++;
				}
				results.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// If the policy didn't generate enough rules, cover a set of new rules
		// for each action.
		if (actionsFound < actionsReturned) {
			List<GuidedRule> coveredRules = PolicyGenerator.getInstance()
					.triggerCovering(state);
			policyRules_.addAll(coveredRules);
			evaluatePolicy(state, actionSwitch, actionsReturned);
		}
	}
}