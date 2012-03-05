package jCloisterZone;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jcloisterzone.action.CaptureAction;
import com.jcloisterzone.action.PlayerAction;
import com.jcloisterzone.ai.AiPlayer;
import com.jcloisterzone.ai.AiUserInterfaceAdapter;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.game.Game;

public class ProxylessAiUserInterfaceAdapter extends AiUserInterfaceAdapter {
	private Game game;

	public ProxylessAiUserInterfaceAdapter(AiPlayer aiPlayer, Game game) {
		super(aiPlayer);
		this.game = game;
	}

	/**
	 * Checks if the AiPlayer is active in a proxyless manner.
	 * 
	 * @return True if the ai player is active.
	 */
	private boolean isAiPlayerActive(AiPlayer aiPlayer) {
		if (game.getActivePlayer().getIndex() == aiPlayer.getPlayer()
				.getIndex())
			return true;
		return false;
	}

	@Override
	public void selectAbbeyPlacement(Set<Position> positions) {
		AiPlayer aiPlayer = getAiPlayer();
		if (isAiPlayerActive(aiPlayer))
			aiPlayer.selectAbbeyPlacement(positions);
	}

	@Override
	public void selectAction(List<PlayerAction> actions) {
		AiPlayer aiPlayer = getAiPlayer();
		if (isAiPlayerActive(aiPlayer))
			aiPlayer.selectAction(actions);
	}

	@Override
	public void selectTilePlacement(Map<Position, Set<Rotation>> positions) {
		AiPlayer aiPlayer = getAiPlayer();
		if (isAiPlayerActive(aiPlayer))
			aiPlayer.selectTilePlacement(positions);
	}

	@Override
	public void selectTowerCapture(CaptureAction action) {
		AiPlayer aiPlayer = getAiPlayer();
		if (isAiPlayerActive(aiPlayer))
			aiPlayer.selectTowerCapture(action);
	}

	@Override
	public void selectDragonMove(Set<Position> positions, int movesLeft) {
		AiPlayer aiPlayer = getAiPlayer();
		if (isAiPlayerActive(aiPlayer))
			aiPlayer.selectDragonMove(positions, movesLeft);
	}
}
