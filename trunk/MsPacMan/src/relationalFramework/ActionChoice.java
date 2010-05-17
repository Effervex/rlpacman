package relationalFramework;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
	private ArrayList<List<String>> activeActions_;

	/** The action preds used in this action choice. */
	private ArrayList<String> actionPreds_;

	/**
	 * A constructor for a new ActionChoice, which initialises the action array.
	 */
	public ActionChoice() {
		activeActions_ = new ArrayList<List<String>>();
		actionPreds_ = new ArrayList<String>();
	}

	/**
	 * Switches on actions.
	 * 
	 * @param actions
	 *            The actions being switched on.
	 */
	public void switchOn(List<String> actions) {
		if ((actions != null) && (!actions.isEmpty())) {
			activeActions_.add(actions);
			String pred = StateSpec.splitFact(actions.get(0))[0];
			if (!actionPreds_.contains(pred))
				actionPreds_.add(pred);
		}
	}

	/**
	 * Switches on an action.
	 * 
	 * @param action
	 *            The action being switched on.
	 */
	public void switchOn(String action) {
		ArrayList<String> al = new ArrayList<String>();
		al.add(action);
		activeActions_.add(al);
		String pred = StateSpec.splitFact(action)[0];
		if (!actionPreds_.contains(pred))
			actionPreds_.add(pred);
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
	public ArrayList<List<String>> getActions() {
		return activeActions_;
	}

	/**
	 * Converts the action switch into a string version
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (List<String> actions : activeActions_) {
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
		for (int i = 0; i < activeActions_.size(); i++) {
			ArrayList<String> modularActions = new ArrayList<String>();
			// Run through each action in the collection and replace
			// constants with modular variables.
			for (String action : activeActions_.get(i)) {
				for (String constant : replacements.keySet()) {
					action = action.replaceAll(" " + Pattern.quote(constant)
							+ "(?=( |\\)))", " " + replacements.get(constant));
				}
				modularActions.add(action);
			}
			activeActions_.set(i, modularActions);
		}
	}

	/**
	 * Gets the first action list. This may be all the environment needs.
	 * 
	 * @return The first list of actions.
	 */
	public List<String> getFirstActionList() {
		return activeActions_.get(0);
	}

	/**
	 * Gets the first action.
	 * 
	 * @return The very first action.
	 */
	public String getFirstAction() {
		return activeActions_.get(0).get(0);
	}
}
