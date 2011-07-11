package relationalFramework.ensemble;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import relationalFramework.FiredAction;
import relationalFramework.PolicyActions;
import relationalFramework.RelationalPredicate;

/**
 * The combined policy output from an ensemble of policies evaluated on a state.
 * 
 * @author Sam Sarjant
 */
public class ActionChoiceEnsemble {
	/** A prioritised list of actions, sorted by action. */
	private ArrayList<Map<RelationalPredicate, AggregateAction>> actionsEnsemble_;

	/**
	 * Basic constructor.
	 */
	public ActionChoiceEnsemble() {
		actionsEnsemble_ = new ArrayList<Map<RelationalPredicate, AggregateAction>>();
	}

	/**
	 * Adds an action choice to this ensemble.
	 * 
	 * @param policyActions
	 *            The actions to add to the ensemble.
	 * @param weight
	 *            The weighting to give to the actions.
	 */
	public void addActionChoice(PolicyActions policyActions, int weight) {
		int i = 0;
		for (Collection<FiredAction> firedActions : policyActions.getActions()) {
			// Initialise the sorted ensemble
			Map<RelationalPredicate, AggregateAction> aggregateActions = null;
			if (actionsEnsemble_.size() <= i) {
				aggregateActions = new HashMap<RelationalPredicate, AggregateAction>();
				actionsEnsemble_.add(aggregateActions);
			} else
				aggregateActions = actionsEnsemble_.get(i);

			// Add the actions
			for (FiredAction action : firedActions) {
				AggregateAction aa = aggregateActions.get(action.getAction());
				if (aa == null)
					aggregateActions.put(action.getAction(), new AggregateAction(action,
							weight));
				else
					aa.addAction(action, weight);
			}

			i++;
		}
	}

	/**
	 * Gets the voted action choice from the ensemble by using the highest voted
	 * actions.
	 * 
	 * @return The voted action choice, based on ensemble votes.
	 */
	public PolicyActions getVotedActionChoice() {
		PolicyActions votedActions = new PolicyActions();
		for (Map<RelationalPredicate, AggregateAction> actionLevel : actionsEnsemble_) {
			Collection<FiredAction> bestActions = new TreeSet<FiredAction>();
			int bestCount = 0;
			for (AggregateAction aa : actionLevel.values()) {
				int thisCount = aa.getWeight();
				if (thisCount > bestCount) {
					bestCount = thisCount;
					bestActions.clear();
				}
				if (thisCount == bestCount) {
					bestActions.add(aa.getAction());
				}
			}
			votedActions.addFiredRule(bestActions);
		}
		return votedActions;
	}
	
	@Override
	public String toString() {
		return actionsEnsemble_.toString();
	}
}
