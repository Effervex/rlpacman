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
 *    src/jCloisterZone/ProxylessCreateGamePhase.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package jCloisterZone;

import com.jcloisterzone.Player;
import com.jcloisterzone.ai.AiPlayer;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.PlayerSlot;
import com.jcloisterzone.game.PlayerSlot.SlotType;
import com.jcloisterzone.game.phase.CreateGamePhase;
import com.jcloisterzone.rmi.ServerIF;

public class ProxylessCreateGamePhase extends CreateGamePhase {

	public ProxylessCreateGamePhase(Game game, ServerIF server) {
		super(game, server);
	}
	
	@Override
	public boolean isLocalPlayer(Player player) {
		if (player == null) return false;
		// Always local.
		return true;
	}
	
	@Override
	public boolean isLocalSlot(PlayerSlot slot) {
		if (slot == null) return false;
		// Always local.
		return true;
	}
	
	protected void prepareAiPlayers() {
		for(int i = 0; i < slots.length; i++) {
			PlayerSlot slot = slots[i];
			if (slot.getType() == SlotType.AI && isLocalSlot(slot)) {
				try {
					AiPlayer ai = (AiPlayer) Class.forName(slot.getAiClassName()).newInstance();
					ai.setGame(game);
					ai.setServer(getServer());
					for(Player player : game.getAllPlayers()) {
						if (player.getSlot().getNumber() == slot.getNumber()) {
							ai.setPlayer(player);
							break;
						}
					}
					game.addUserInterface(new ProxylessAiUserInterfaceAdapter(ai, game));
					logger.info("AI player created - " + slot.getAiClassName());
				} catch (Exception e) {
					logger.error("Unable to create AI player", e);
				}
			}
		}
	}
}
