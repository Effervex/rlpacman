package blocksWorldMove;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import blocksWorld.BlocksState;

import cerrla.PolicyGenerator;
import jess.Fact;
import jess.Rete;
import relationalFramework.FiredAction;
import relationalFramework.ObjectObservations;
import relationalFramework.PolicyActions;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalWrapper;
import relationalFramework.StateSpec;
import util.Pair;

public class BlocksWorldRelationalWrapper extends RelationalWrapper {
	private Fact action_;

	@Override
	protected Rete assertStateFacts(Rete rete, Object... args) throws Exception {
		if (!isFirstStateInEpisode()) {
			// Apply the action
			rete.run();
			// Retract the action assertion
			rete.retract(action_);
			return rete;
		}

		BlocksState blocksState = (BlocksState) args[0];
		@SuppressWarnings("unchecked")
		List<String> goalArgs = (List<String>) args[1];

		// Assert the floor
		rete.assertString("(floor floor)");

		Integer[] intState = blocksState.getState();

		// Scanning through, making predicates (On, OnFloor, and Highest)
		int[] heightMap = new int[blocksState.length];
		int maxHeight = 0;
		List<Integer> highestBlocks = new ArrayList<Integer>();
		List<Integer> allBlocks = new ArrayList<Integer>();
		for (int i = 0; i < blocksState.length; i++) {
			// On the floor
			if (intState[i] == 0) {
				rete.assertString("(on " + (char) ('a' + i) + " floor)");
			} else {
				// On another block
				rete.assertString("(on " + (char) ('a' + i) + " "
						+ (char) ('a' + intState[i] - 1) + ")");
			}
			allBlocks.add(i);

			// Finding the heights
			int blockHeight = heightMap[i];
			if (blockHeight == 0) {
				blockHeight = recurseHeight(i, heightMap, intState);
			}
			if (blockHeight > maxHeight) {
				maxHeight = blockHeight;
				highestBlocks.clear();
			}
			if (blockHeight == maxHeight) {
				highestBlocks.add(i);
			}

			// Assert the blocks
			rete.assertString("(block " + (char) ('a' + i) + ")");
		}

		// Add the highest block/s
		for (Integer block : highestBlocks) {
			rete.assertString("(highest " + (char) ('a' + block) + ")");
		}

		// Asserting the highest goal
		if (goalArgs == null) {
			if (StateSpec.getInstance().getGoalName().equals("highestA")) {
				goalArgs = new ArrayList<String>(1);
				allBlocks.removeAll(highestBlocks);
				if (allBlocks.isEmpty())
					return null;
				goalArgs.add((char) ('a' + allBlocks
						.get(PolicyGenerator.random_.nextInt(allBlocks.size())))
						+ "");
			} else
				goalArgs = new ArrayList<String>();
		}

		// Add the goal
		StateSpec.getInstance().assertGoalPred(goalArgs, rete);
		return rete;
	}

	/**
	 * Finds the height of a block recursively by following a path. Stores the
	 * values.
	 * 
	 * @param start
	 *            The starting index to check.
	 * @param heightMap
	 *            The stored heightMap.
	 * @param worldState
	 *            The state of the world in block links.
	 * @return The maximum height of the block stack.
	 */
	private int recurseHeight(int start, int[] heightMap, Integer[] worldState) {
		if (worldState[start] == 0) {
			heightMap[start] = 1;
			return 1;
		}

		int below = worldState[start] - 1;
		recurseHeight(below, heightMap, worldState);
		heightMap[start] = heightMap[below] + 1;
		return heightMap[start];
	}

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
				action_ = StateSpec.getInstance().getRete().assertString(action.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		BlocksState blocksState = (BlocksState) args[0];

		if (action == null)
			return blocksState;

		Integer[] newState = new Integer[blocksState.length];

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
				newState[j] = stateArray[j];
				// If something is on that index/block (except the floor),
				// return the unchanged state
				if (indices[i] != -1 && stateArray[j] == indices[i] + 1)
					return blocksState;
			}
		}

		// Perform the action
		newState[indices[0]] = indices[1] + 1;

		return new Pair<BlocksState, RelationalPredicate>(new BlocksState(
				newState), action);
	}

	@Override
	public int isTerminal(Object... args) {
		if (ObjectObservations.getInstance().earlyExit
				|| super.isTerminal(args) == 1)
			return 1;
		return 0;
	}
}
