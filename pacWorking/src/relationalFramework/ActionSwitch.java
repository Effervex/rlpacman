package relationalFramework;

import java.util.List;

import org.mandarax.kernel.Fact;

/**
 * This class represents an actions module. It controls which actions are switched on and off
 * @author Samuel J. Sarjant
 *
 */
public class ActionSwitch {
	/** The number of priorities. */
	public static final int NUM_PRIORITIES = 3;
	/** The prioritised list of switched actions. Max 3. */ 
	private List<Fact>[] activeActions_ = new List[NUM_PRIORITIES];
	
	/**
	 * Switches on a high action at the given priority.
	 * 
	 * @param clause The action being switched on.
	 * @param priority The priority level of the action.
	 */
	public void switchOn(List<Fact> clause, int priority) {
		activeActions_[priority] = clause;
	}

	public void switchOffAll() {
		for (int i = 0; i < NUM_PRIORITIES; i++) {
			activeActions_[i] = null;
		}
	}
	
	/**
	 * Gets the int version of the action list.
	 * 
	 * @return The int version of the action list.
	 */
	public List<Fact>[] getPrioritisedActions() {
		return activeActions_;
	}
	
	/**
	 * Gets the switch state of a particular action.
	 * 
	 * @param action The action to check.
	 * @return True if it is on, false otherwise
	 */
	public boolean isActionActive(Fact action) {
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
