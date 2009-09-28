package crossEntropyFramework;

import crossEntropyFramework.Condition.ConditionObject;

/**
 * This class represents an actions module. It controls which actions are switched on and off
 * @author Samuel J. Sarjant
 *
 */
public class ActionSwitch {
	/** The number of priorities. */
	public static final int NUM_PRIORITIES = 3;
	/** The prioritised list of switched actions. Max 3. */ 
	private ConditionObject[] activeActions_ = new ConditionObject[NUM_PRIORITIES];
	
	/**
	 * Switches on a high action at the given priority.
	 * 
	 * @param action The action being switched on.
	 * @param priority The priority level of the action.
	 */
	public void switchOn(ConditionObject action, int priority) {
		activeActions_[priority] = action;
	}
	
	/**
	 * Switches off an action (or actions) regardless of the action priority.
	 * 
	 * @param action The action to be switched off.
	 */
	public void switchOff(ConditionObject action) {
		for (int i = 0; i < NUM_PRIORITIES; i++) {
			if ((activeActions_[i] != null) && (activeActions_[i].equals(action))) {
				activeActions_[i] = null;
			}
		}
	}
	
	/**
	 * Gets the int version of the action list.
	 * 
	 * @return The int version of the action list.
	 */
	public int[] getPrioritisedActions() {
		int[] prioritisedActions = new int[NUM_PRIORITIES];
		for (int i = 0; i < NUM_PRIORITIES; i++) {
			if (activeActions_[i] != null)
				prioritisedActions[i] = activeActions_[i].ordinal();
			else
				prioritisedActions[i] = -1;
		}
		
		return prioritisedActions;
	}
	
	/**
	 * Gets the switch state of a particular action.
	 * 
	 * @param action The action to check.
	 * @return True if it is on, false otherwise
	 */
	public boolean isActionActive(ConditionObject action) {
		for (int i = 0; i < NUM_PRIORITIES; i++) {
			if ((activeActions_[i] != null) && (activeActions_[i].equals(action)))
				return true;
		}
		return false;
	}
	
	/**
	 * Converts the action switch into a string version
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < NUM_PRIORITIES; i++) {
			buffer.append("[" + (i + 1) + "]: ");
			buffer.append(activeActions_[i] + "\n");
		}
		return buffer.toString();
	}
}
