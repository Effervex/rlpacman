package relationalFramework;

import java.util.ArrayList;

/**
 * This class represents an actions module. It controls which actions are switched on and off
 * @author Samuel J. Sarjant
 *
 */
public class ActionSwitch {
	/** The prioritised list of switched actions. Max 3. */ 
	private ArrayList<String> activeActions_;
	
	public ActionSwitch() {
		activeActions_ = new ArrayList<String>();
	}
	
	/**
	 * Switches on an action.
	 * 
	 * @param action The action being switched on.
	 */
	public void switchOn(String action) {
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
	public ArrayList<String> getPrioritisedActions() {
		return activeActions_;
	}
	
	/**
	 * Checks if a particular action is active.
	 * 
	 * @param action The action to check.
	 * @return True if it is on, false otherwise
	 */
	public boolean isActionActive(String action) {
		return activeActions_.contains(action);
	}
	
	/**
	 * Converts the action switch into a string version
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (String action : activeActions_) {
			buffer.append(action + "\n");
		}
		return buffer.toString();
	}
}
