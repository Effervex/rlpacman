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
 *    src/jCloisterZone/ProxylessDrawPhase.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package jCloisterZone;

import com.jcloisterzone.Player;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.PlayerSlot;
import com.jcloisterzone.game.phase.DrawPhase;
import com.jcloisterzone.rmi.ServerIF;

public class ProxylessDrawPhase extends DrawPhase {

	public ProxylessDrawPhase(Game game, ServerIF server) {
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
}
