package relationalFramework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collection;
import java.util.Iterator;

import jess.Rete;
import util.MultiMap;

/**
 * A basic policy that just evaluates its rules against the state and returns.
 * 
 * @author Sam Sarjant
 */
public class BasicRelationalPolicy extends RelationalPolicy {
	private static final long serialVersionUID = 3758878250620187453L;

	@Override
	public PolicyActions evaluatePolicy(RelationalObservation observation,
			int actionsReturned) {
		PolicyActions actionSwitch = new PolicyActions();
		int actionsFound = 0;
		int actionsReturnedModified = (actionsReturned <= -1) ? Integer.MAX_VALUE
				: actionsReturned;

		Rete state = observation.getStateObservations();
		MultiMap<String, String[]> validActions = observation.getValidActions();
		try {
			// Evaluate the policy rules.
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
	 * Loads a policy from file.
	 * 
	 * @param polFile
	 *            The policy file.
	 * @return The loaded policy.
	 */
	public static BasicRelationalPolicy loadPolicyFile(File polFile)
			throws Exception {
		BasicRelationalPolicy policy = new BasicRelationalPolicy();
		FileReader fr = new FileReader(polFile);
		BufferedReader br = new BufferedReader(fr);

		String input = null;
		while ((input = br.readLine()) != null)
			policy.addRule(new RelationalRule(input));

		br.close();
		fr.close();
		return policy;
	}
}
