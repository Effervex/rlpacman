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
 *    src/jCloisterZone/LocalCarcassonneServer.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package jCloisterZone;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Random;

import com.jcloisterzone.Expansion;
import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.figure.Follower;
import com.jcloisterzone.figure.Meeple;
import com.jcloisterzone.game.CustomRule;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.GameSettings;
import com.jcloisterzone.game.PlayerSlot;
import com.jcloisterzone.game.PlayerSlot.SlotType;
import com.jcloisterzone.game.phase.DrawPhase;
import com.jcloisterzone.game.phase.Phase;
import com.jcloisterzone.rmi.CallMessage;
import com.jcloisterzone.rmi.ClientIF;
import com.jcloisterzone.rmi.ServerIF;

public class LocalCarcassonneServer extends GameSettings implements ServerIF,
		InvocationHandler {
	private Random random = new Random();

	private Game game;

	protected final PlayerSlot[] slots;

	public LocalCarcassonneServer(Game game) {
		slots = new PlayerSlot[PlayerSlot.COUNT];
		this.game = game;
	}

	public ClientIF getStub() {
		return game.getPhase();
	}

	@Override
	public void updateExpansion(Expansion expansion, Boolean enabled) {
		getStub().updateExpansion(expansion, enabled);
	}

	@Override
	public void updateCustomRule(CustomRule rule, Boolean enabled) {
		getStub().updateCustomRule(rule, enabled);
	}

	@Override
	public void startGame() {
		// Initialise null slots
		for (int s = 0; s < slots.length; s++) {
			PlayerSlot slot = slots[s];
			if (slot == null)
				slot = new PlayerSlot(s);
			updateSlot(slot, null);
		}

		// Add BASIC expansion.
		getExpansions().add(Expansion.BASIC);
		getStub().updateExpansion(Expansion.BASIC, true);

		// Start the game
		getStub().startGame();

		// Modify the phases
		Phase drawPhase = game.getPhases().get(DrawPhase.class);
		ProxylessDrawPhase proxylessDrawPhase = new ProxylessDrawPhase(game,
				this);
		proxylessDrawPhase.setDefaultNext(drawPhase.getDefaultNext());
		game.getPhases().put(ProxylessDrawPhase.class, proxylessDrawPhase);
	}
	
	@Override
	public void stopGame() {
		// No action.
	}

	@Override
	public void placeNoFigure() {
		getStub().placeNoFigure();
	}

	@Override
	public void placeNoTile() {
		getStub().placeNoTile();
	}

	@Override
	public void placeTile(Rotation rotation, Position position) {
		getStub().placeTile(rotation, position);
	}

	@Override
	public void deployMeeple(Position p, Location d,
			Class<? extends Meeple> meepleType) {
		getStub().deployMeeple(p, d, meepleType);
	}

	@Override
	public void placeTowerPiece(Position p) {
		getStub().placeTowerPiece(p);
	}

	@Override
	public void escapeFromCity(Position p, Location d) {
		getStub().escapeFromCity(p, d);
	}

	@Override
	public void removeKnightWithPrincess(Position p, Location d) {
		getStub().removeKnightWithPrincess(p, d);
	}

	@Override
	public void captureFigure(Position p, Location d) {
		getStub().captureFigure(p, d);
	}

	@Override
	public void placeTunnelPiece(Position p, Location d, boolean isSecondPiece) {
		getStub().placeTunnelPiece(p, d, isSecondPiece);
	}

	@Override
	public void moveFairy(Position p) {
		getStub().moveFairy(p);
	}

	@Override
	public void moveDragon(Position p) {
		getStub().moveDragon(p);
	}

	@Override
	public void payRansom(Integer playerIndexToPay,
			Class<? extends Follower> meepleType) {
		getStub().payRansom(playerIndexToPay, meepleType);
	}

	@Override
	public void updateSlot(PlayerSlot slot,
			EnumSet<Expansion> supportedExpansions) {
		if (slot.getType() == SlotType.OPEN) { // new type
			slot.setNick(null);
			slot.setSerial(null);
		}
		if (slot.getType() != SlotType.AI) { // new type
			slot.setAiClassName(null);
		}
		slots[slot.getNumber()] = slot;
		getStub().updateSlot(slot);
	}

	@Override
	public void selectTile(Integer tiles) {
		// generate random tile
		getStub().nextTile(random.nextInt(tiles));
	}

	@Override
	public void setRandomGenerator(Random random) {
		this.random = random;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		CallMessage msg = new CallMessage(method, args);
		msg.call(this, ServerIF.class);
		return null;
	}

	@Override
	public void deployBridge(Position pos, Location loc) {
		// N/A
	}

	@Override
	public void deployCastle(Position pos, Location loc) {
		// N/A
	}

}
