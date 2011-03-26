package relationalFramework;

import java.util.ArrayList;
import java.util.Map;

/**
 * This class represents the chosen actions the agent returns when making a
 * decision.
 * 
 * @author Samuel J. Sarjant
 * 
 */
public class ActionChoice {
	/**
	 * The prioritised list of switched actions. Each rule may return a list of
	 * actions.
	 */
	private ArrayList<RuleAction> activeActions_;

	/** The action preds used in this action choice. */
	private ArrayList<String> actionPreds_;

	/**
	 * A constructor for a new ActionChoice, which initialises the action array.
	 */
	public ActionChoice() {
		activeActions_ = new ArrayList<RuleAction>();
		actionPreds_ = new ArrayList<String>();
	}

	/**
	 * Switches on actions.
	 * 
	 * @param actions
	 *            The actions being switched on.
	 */
	public void switchOn(RuleAction actions) {
		if ((actions != null) && (!actions.isEmpty())) {
			activeActions_.add(actions);
			String pred = actions.getRule().getActionPredicate();
			if (!actionPreds_.contains(pred))
				actionPreds_.add(pred);
		}
	}

	/**
	 * Switches off all active actions.
	 */
	public void switchOffAll() {
		activeActions_.clear();
	}

	/**
	 * Gets the active actions.
	 * 
	 * @return The list of active actions, of arbitrary length.
	 */
	public ArrayList<RuleAction> getActions() {
		return activeActions_;
	}

	/**
	 * Converts the action switch into a string version
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (RuleAction actions : activeActions_) {
			buffer.append(actions + "\n");
		}
		return buffer.toString();
	}

	/**
	 * Gets the action preds used in this ActionChoice.
	 * 
	 * @return A (String) list of action preds.
	 */
	public String getActionPreds() {
		if (actionPreds_.isEmpty())
			return "EMPTY";

		StringBuffer buffer = new StringBuffer(actionPreds_.get(0));
		for (int i = 1; i < actionPreds_.size(); i++) {
			buffer.append(", " + actionPreds_.get(i));
		}
		return buffer.toString();
	}

	public void replaceTerms(Map<String, String> replacements) {
		for (RuleAction ruleAction : activeActions_) {
			// Run through each action in the collection and replace
			// constants with modular variables.
			ruleAction.replaceTerms(replacements);
		}
	}

	/**
	 * Gets the first action list. This may be all the environment needs.
	 * 
	 * @return The first list of actions.
	 */
	public RuleAction getFirstActionList() {
		if (activeActions_.isEmpty())
			return null;
		return activeActions_.get(0);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((activeActions_ == null) ? 0 : activeActions_.hashCode());
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
		ActionChoice other = (ActionChoice) obj;
		if (activeActions_ == null) {
			if (other.activeActions_ != null)
				return false;
		} else if (!activeActions_.equals(other.activeActions_))
			return false;
		return true;
	}
}
