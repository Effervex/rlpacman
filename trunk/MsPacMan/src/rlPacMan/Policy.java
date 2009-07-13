package rlPacMan;

/**
 * This class represents a policy that the agent can use.
 * 
 * @author Samuel J. Sarjant
 */
public class Policy {
	public static final String PREFIX = "Policy";
	public static final char DELIMITER = ',';
	/** The rules of this policy, under their respective priorites. */
	private Rule[] priorityRules_;

	/**
	 * A constructor for creating a new policy.
	 * 
	 * @param policySize
	 *            The maximum size of the policy.
	 */
	@SuppressWarnings("unchecked")
	public Policy(int policySize) {
		priorityRules_ = new Rule[policySize];
	}

	/**
	 * Adds a rule to the policy at a given priority level.
	 * 
	 * @param index
	 *            The index (and priority level) the rule is added at.
	 * @param rule
	 *            The rule to be added.
	 */
	public void addRule(int index, Rule rule) {
		priorityRules_[index] = rule;
	}

	/**
	 * Gets the rules of the policy.
	 * 
	 * @return The rules of the policy.
	 */
	public Rule[] getRules() {
		return priorityRules_;
	}

	/**
	 * Returns a string version of the policy, parseable by an agent.
	 */
	public String toParseableString() {
		StringBuffer buffer = new StringBuffer(PREFIX);
		for (int i = 0; i < priorityRules_.length; i++) {
			buffer.append(DELIMITER);
			if (priorityRules_[i] != null)
				buffer
						.append(RuleBase.getInstance().indexOf(
								priorityRules_[i], i));
		}
		buffer.append(DELIMITER + "END");
		return buffer.toString();
	}

	@Override
	public String toString() {
		int priorityNumber = priorityRules_.length
				/ ActionSwitch.NUM_PRIORITIES;
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < priorityRules_.length; i++) {
			if (priorityRules_[i] != null) {
				buffer.append("[" + (i / priorityNumber + 1) + "]: ");
				buffer.append(priorityRules_[i] + "\n");
			}
		}
		return buffer.toString();
	}

	/**
	 * Parses a policy from a string representation of the policy.
	 * 
	 * @param strPolicy
	 *            The string version of the policy.
	 * @return The parsed policy or null if invalid.
	 */
	public static Policy parsePolicy(String strPolicy) {
		String[] split = strPolicy.split("" + DELIMITER);
		if (!split[0].equals(PREFIX))
			return null;

		// Parse the rules
		Policy policy = new Policy(split.length - 2);
		for (int i = 1; i < split.length - 1; i++) {
			// If there is a rule
			if (split[i].length() != 0) {
				policy.addRule(i - 1, RuleBase.getInstance().getRule(
						Integer.parseInt(split[i]), i - 1));
			}
		}
		return policy;
	}

	/**
	 * Evaluates the policy for applicable rules within each priority. If
	 * multiple rules are applicable, only apply the highest priority ones.
	 * 
	 * @param observations
	 *            The current observations.
	 * @param actionSwitch
	 *            The current actions.
	 */
	public void evaluatePolicy(double[] observations, ActionSwitch actionSwitch) {
		int priorityNumber = priorityRules_.length
				/ ActionSwitch.NUM_PRIORITIES + 1;
		// Check every slot, from top-to-bottom until one activates
		int firingPriority = -1;
		for (int i = 0; i < priorityRules_.length; i++) {
			// Check if the rule exists and if it does, if it applies.
			if ((priorityRules_[i] != null)
					&& (priorityRules_[i].evaluateCondition(observations,
							actionSwitch))) {
				// Check if this rule is at the same priority as other firing
				// rules
				//int thisPriority = i / priorityNumber;
				int thisPriority = firingPriority;
				if ((firingPriority == -1)
						|| (thisPriority == firingPriority)) {
					// Apply the rule and set the firing priority
					priorityRules_[i].applyAction(actionSwitch,
							(i / priorityNumber));
					firingPriority = thisPriority;
				} else {
					return;
				}
			}
		}
	}
}