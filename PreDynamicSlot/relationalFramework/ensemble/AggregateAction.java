package relationalFramework.ensemble;

import java.util.Collection;
import java.util.HashSet;

import relationalFramework.FiredAction;
import relationalFramework.RelationalPredicate;

/**
 * A structure for representing a number of actions together as one. It also
 * holds information for each action's rules and policies.
 * 
 * @author Sam Sarjant
 * 
 */
public class AggregateAction {
	/** The action being represented by this aggregate action. */
	private RelationalPredicate action_;
	
	/** The aggregated actions. */
	private Collection<FiredAction> aggregates_;

	/** The weight of this action. */
	private int weight_;

	/**
	 * Basic constructor.
	 * 
	 * @param action
	 *            The action to start the aggregate.
	 * @param weight
	 *            The weight of that action.
	 */
	public AggregateAction(FiredAction action, int weight) {
		action_ = action.getAction();
		aggregates_ = new HashSet<FiredAction>();
		weight_ = 0;
		addAction(action, weight);
	}

	/**
	 * Adds an action to this aggregate.
	 * 
	 * @param action
	 *            The action being added.
	 * @param weight
	 *            The weight of the action being added.
	 */
	public void addAction(FiredAction action, int weight) {
		aggregates_.add(action);
		weight_ += weight;
	}

	public int getWeight() {
		return weight_;
	}

	// TODO This may not be handling ensemble learning so well.
	// Need to get all actions to update all rules and policies.
	public FiredAction getAction() {
		return aggregates_.iterator().next();
	}
	
	@Override
	public String toString() {
		return "WEIGHT " + weight_ + ":" + action_.toString();
	}
}
