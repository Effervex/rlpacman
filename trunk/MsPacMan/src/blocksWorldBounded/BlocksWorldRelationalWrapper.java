package blocksWorldBounded;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cerrla.PolicyGenerator;
import relationalFramework.FiredAction;
import relationalFramework.PolicyActions;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;
import util.Pair;

public class BlocksWorldRelationalWrapper extends
		blocksWorldMove.BlocksWorldRelationalWrapper {
	@Override
	public Object groundActions(PolicyActions actions, Object... args) {
		Collection<FiredAction> firedActions = actions.getFirstActionList();
		RelationalPredicate action = null;
		if (firedActions != null) {
			List<FiredAction> actionsList = new ArrayList<FiredAction>(
					firedActions);
			FiredAction selectedAction = actionsList
					.get(PolicyGenerator.random_.nextInt(actionsList.size()));
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
			return args[0];

		BoundedBlocksState newState = ((BoundedBlocksState) args[0]).clone();

		if (action.getFactName().equals("move")) {
			// Finding the block objects
			int[] indices = new int[2];

			// Convert the blocks to indices
			for (int i = 0; i < indices.length; i++) {
				if (action.getArguments()[i].equals("floor"))
					indices[i] = -1;
				else {
					indices[i] = (action.getArguments()[i].charAt(0)) - ('a');
					while (newState.getBoundBlocks()[indices[i]] != 0)
						indices[i] = newState.getBoundBlocks()[indices[i]];
				}
			}

			// Perform the action
			newState.getState()[indices[0]] = indices[1] + 1;
		} else if (action.getFactName().equals("bind")) {
			int indexA = action.getArguments()[0].charAt(0) - 'a';
			int indexB = action.getArguments()[1].charAt(0) - 'a';
			newState.getBoundBlocks()[indexA] = indexB;
		} else if (action.getFactName().equals("unbind")) {
			int indexA = action.getArguments()[0].charAt(0) - 'a';
			newState.getBoundBlocks()[indexA] = 0;
		}

		return new Pair<BoundedBlocksState, RelationalPredicate>(newState,
				action);
	}
}
