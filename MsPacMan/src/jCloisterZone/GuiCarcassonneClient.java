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
 *    src/jCloisterZone/GuiCarcassonneClient.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package jCloisterZone;

import com.jcloisterzone.ui.Client;

public class GuiCarcassonneClient extends Client implements RRLJCloisterClient {
	private static final long serialVersionUID = -2770234875101534892L;
	private boolean running_;

	public GuiCarcassonneClient(String configFile, boolean maintainServer) {
		super(configFile, maintainServer);
	}
	
	@Override
	public void createGame() {
		super.createGame();
		running_ = true;
	}
	
	@Override
	public boolean closeGame(boolean force) {
		if (!running_)
			return true;
		running_ = false;
		return super.closeGame(force);
	}

	public boolean isRunning() {
		return running_;
	}
}
