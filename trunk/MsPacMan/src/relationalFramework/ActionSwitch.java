package relationalFramework;

import java.util.ArrayList;

/**
 * This class represents an actions module. It controls which actions are
 * switched on and off
 * 
 * @author Samuel J. Sarjant
 * 
 */
public class ActionSwitch {
	/**
	 * The prioritised list of switched actions. Each rule may return a list of
	 * actions.
	 */
	private ArrayList<ArrayList<String>> activeActions_;

	public ActionSwitch() {
		activeActions_ = new ArrayList<ArrayList<String>>();
	}

	/**
	 * Switches on an action.
	 * 
	 * @param action
	 *            The action being switched on.
	 */
	public void switchOn(ArrayList<String> action) {
		activeActions_.add(action);
	}

	/**
	 * Switches off all action slots.
	 */
	public void switchOffAll() {
		activeActions_.clear();
	}

	/**
	 * Gets the active actions.
	 * 
	 * @return The list of active actions, of arbitrary length.
	 */
	public ArrayList<ArrayList<String>> getPrioritisedActions() {
		return activeActions_;
	}

	/**
	 * Converts the action switch into a string version
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (ArrayList<String> actions : activeActions_) {
			buffer.append(actions + "\n");
		}
		return buffer.toString();
	}
}
