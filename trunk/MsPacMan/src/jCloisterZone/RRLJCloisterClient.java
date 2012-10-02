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
 *    src/jCloisterZone/RRLJCloisterClient.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package jCloisterZone;

import com.jcloisterzone.game.Game;
import com.jcloisterzone.rmi.ServerIF;

public interface RRLJCloisterClient {
	public boolean closeGame(boolean force);

	public void createGame();

	public long getClientId();

	public Game getGame();

	public ServerIF getServer();

	public boolean isRunning();
}
