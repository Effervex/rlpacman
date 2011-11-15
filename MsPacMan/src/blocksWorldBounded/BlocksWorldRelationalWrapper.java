package blocksWorldBounded;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import blocksWorld.BlocksState;

import jess.Rete;

import cerrla.PolicyGenerator;
import relationalFramework.FiredAction;
import relationalFramework.PolicyActions;
import relationalFramework.RelationalPredicate;
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
		}

		ActiveBlocksState blocksState = ((ActiveBlocksState) args[0]).clone();

		if (action == null)
			return blocksState;

		Integer[] newState = blocksState.getState();
		boolean[] activeBlocks = blocksState.getActiveBlocks();

		if (action.getFactName().equals("move")) {
			// Finding the block objects
			int[] indices = new int[2];

			// Convert the blocks to indices
			Integer[] stateArray = blocksState.getState();
			for (int i = 0; i < indices.length; i++) {
				if (action.getArguments()[i].equals("floor")) {
					indices[i] = -1;
					// Cannot move the floor
					if (i == 0)
						return blocksState;
				} else
					indices[i] = (action.getArguments()[i].charAt(0)) - ('a');
				// In order to do either action, both blocks must be free
				for (int j = 0; j < stateArray.length; j++) {
					// If something is on that index/block (except the floor),
					// return the unchanged state
					if (indices[i] != -1 && stateArray[j] == indices[i] + 1)
						return blocksState;
				}
			}

			// Perform the action
			newState[indices[0]] = indices[1] + 1;
		} else if (action.getFactName().equals("toggle")) {
			int index = action.getArguments()[0].charAt(0) - 'a';
			activeBlocks[index] = !activeBlocks[index];
		}

		return new Pair<ActiveBlocksState, RelationalPredicate>(
				new ActiveBlocksState(newState, activeBlocks), action);
	}
	
	@Override
	protected Rete assertStateFacts(Rete rete, Object... args) throws Exception {
		rete = super.assertStateFacts(rete, args);
		BlocksState blocksState = (BlocksState) args[0];
		
		if (blocksState instanceof ActiveBlocksState) {
			ActiveBlocksState activeBlocksState = (ActiveBlocksState) blocksState;
			try {
				for (int i = 0; i < activeBlocksState.getActiveBlocks().length; i++) {
					if (activeBlocksState.getActiveBlocks()[i])
						rete.assertString("(active " + (char) ('a' + i) + ")");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return rete;
	}
}
