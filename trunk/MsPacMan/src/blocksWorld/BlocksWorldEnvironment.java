package blocksWorld;

import relationalFramework.PolicyActions;
import relationalFramework.RelationalPredicate;
import relationalFramework.StateSpec;
import rrlFramework.RRLExperiment;
import util.MultiMap;
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
		RelationalPredicate action = null;
		if (!actions.isEmpty())
			action = actions.getFirstRandomAction();
		return action;
	}

	protected Object randomAction() {
		Object action;
		// Select random action predicate
		MultiMap<String, String[]> validActions = observations_
				.getValidActions();
		Object[] moveArgs = new Object[0];
		Object[] moveFlArgs = new Object[0];
		try {
			if (validActions.containsKey("move"))
				moveArgs = validActions.getSortedSet("move").toArray();
			if (validActions.containsKey("moveFloor"))
				moveFlArgs = validActions.getSortedSet("moveFloor").toArray();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		int randomIndex = RRLExperiment.random_.nextInt(moveArgs.length
				+ moveFlArgs.length);
		if (randomIndex < moveArgs.length) {
			Object randomAction = moveArgs[randomIndex];
			action = new RelationalPredicate(StateSpec.getInstance()
					.getActions().get("move"), (String[]) randomAction);
		} else {
			Object randomAction = moveFlArgs[randomIndex - moveArgs.length];
			action = new RelationalPredicate(StateSpec.getInstance()
					.getActions().get("moveFloor"), (String[]) randomAction);
		}
		return action;
	}

	@Override
	protected void resolveAction(BlocksState newState,
			RelationalPredicate actionFact) {
		// Finding the block objects
		if (actionFact.getFactName().equals("move")) {
			int[] indices = new int[2];

			// Convert the blocks to indices
			for (int i = 0; i < indices.length; i++) {
				if (actionFact.getArguments()[i].equals("floor"))
					indices[i] = -1;
				else
					indices[i] = (actionFact.getArguments()[i].charAt(0)) - ('a');
			}

			// Perform the action
			newState.getState()[indices[0]] = indices[1] + 1;
		} else if (actionFact.getFactName().equals("moveFloor")) {
			int index = actionFact.getArguments()[0].charAt(0) - 'a';
			newState.getState()[index] = 0;
		}
	}
}
