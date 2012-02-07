package jCloisterZone;

import java.util.List;

import jess.Rete;
import relationalFramework.PolicyActions;
import rrlFramework.RRLEnvironment;

import org.ini4j.Ini;

import com.jcloisterzone.Expansion;
import com.jcloisterzone.board.Board;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.PlayerSlot;
import com.jcloisterzone.game.phase.CreateGamePhase;
import com.jcloisterzone.game.phase.Phase;

public class CarcassonneEnvironment extends RRLEnvironment {
	private Phase gamePhase_;
	private Game environment_;
	private boolean guiMode_ = false;

	@Override
	protected void assertStateFacts(Rete rete, List<String> goalArgs)
			throws Exception {
		Board board = environment_.getBoard();
	}

	@Override
	protected double calculateReward(boolean isTerminal) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected List<String> getGoalArgs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Object groundActions(PolicyActions actions) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean isReteDriven() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void cleanup() {
		// TODO Auto-generated method stub

	}

	@Override
	public void initialise(int runIndex, String[] extraArg) {
		// TODO Auto-generated method stub
		environment_ = new Game();
		environment_.setConfig(new Ini());
		environment_.addGameListener(new CarcassonneEventListener());
		environment_.getExpansions().add(Expansion.BASIC);
		gamePhase_ = new CreateGamePhase(environment_, null);

		// Handle number of players playing
		for (int i = 0; i < PlayerSlot.COUNT; i++) {
			PlayerSlot slot = (i == 0) ? new PlayerSlot(i,
					PlayerSlot.SlotType.PLAYER, "CERRLA", 0l) : new PlayerSlot(
					i);
			gamePhase_.updateSlot(slot);
		}
	}

	@Override
	protected void startState() {
		gamePhase_.startGame();

	}

	@Override
	protected void stepState(Object action) {
		// TODO Auto-generated method stub

	}
}
