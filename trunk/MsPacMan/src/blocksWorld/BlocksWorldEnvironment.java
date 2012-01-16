package blocksWorld;

import relationalFramework.FiredAction;
import relationalFramework.PolicyActions;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;
import rrlFramework.RRLExperiment;
import util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jess.Rete;

import blocksWorldMove.BlocksState;

/**
 * The environment for the blocks world interface.
 * 
 * @author Sam Sarjant
 */
public class BlocksWorldEnvironment extends
		blocksWorldMove.BlocksWorldEnvironment {
	@Override
	protected void assertStateFacts(Rete rete, List<String> goalArgs)
			throws Exception {
		if (steps_ != 0) {
			// Apply the action
			rete.run();
			return;
		}

		Integer[] intState = state_.getState();

		// Scanning through, making predicates (On, OnFloor)
		for (int i = 0; i < state_.length; i++) {
			// On the floor
			if (intState[i] == 0) {
				rete.assertString("(onFloor " + (char) ('a' + i) + ")");
			} else {
				// On another block
				rete.assertString("(on " + (char) ('a' + i) + " "
						+ (char) ('a' + intState[i] - 1) + ")");
			}

			// Assert the blocks
			rete.assertString("(block " + (char) ('a' + i) + ")");
		}
	}

	@Override
	protected Object groundActions(PolicyActions actions) {
		Collection<FiredAction> firedActions = actions.getFirstActionList();
		RelationalPredicate action = null;
		if (firedActions != null) {
			List<FiredAction> actionsList = new ArrayList<FiredAction>(
					firedActions);
			FiredAction selectedAction = actionsList
					.get(RRLExperiment.random_.nextInt(actionsList.size()));
			selectedAction.triggerRule();
			action = selectedAction.getAction();

			// Assert the action to the Rete object.
			try {
				StateSpec.getInstance().getRete()
						.assertString(action.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (action == null)
			return state_;

		BlocksState newState = state_.clone();

		// Finding the block objects
		if (action.getFactName().equals("move")) {
			int[] indices = new int[2];

			// Convert the blocks to indices
			for (int i = 0; i < indices.length; i++) {
				if (action.getArguments()[i].equals("floor"))
					indices[i] = -1;
				else
					indices[i] = (action.getArguments()[i].charAt(0)) - ('a');
			}

			// Perform the action
			newState.getState()[indices[0]] = indices[1] + 1;
		} else if (action.getFactName().equals("moveFloor")) {
			int index = action.getArguments()[0].charAt(0) - 'a';
			newState.getState()[index] = 0;
		}

		return new Pair<BlocksState, RelationalPredicate>(newState, action);
	}
}
