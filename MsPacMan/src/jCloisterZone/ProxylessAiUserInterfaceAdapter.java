/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/jCloisterZone/ProxylessAiUserInterfaceAdapter.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
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
