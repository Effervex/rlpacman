package jCloisterZone;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
import com.jcloisterzone.rmi.CallMessage;
import com.jcloisterzone.rmi.ClientIF;
import com.jcloisterzone.rmi.ServerIF;

public class LocalCarcassonneServer extends GameSettings implements ServerIF,
		InvocationHandler {
	private Random random = new Random();

	private ClientIF stub;

	protected final PlayerSlot[] slots;

	public LocalCarcassonneServer(Game game) {
		slots = new PlayerSlot[PlayerSlot.COUNT];
		LocalCarcassonneServerStub handler = new LocalCarcassonneServerStub(
				game);
		stub = (ClientIF) Proxy.newProxyInstance(
				ClientIF.class.getClassLoader(),
				new Class[] { ClientIF.class }, handler);
	}

	@Override
	public void updateExpansion(Expansion expansion, Boolean enabled) {
		stub.updateExpansion(expansion, enabled);
	}

	@Override
	public void updateCustomRule(CustomRule rule, Boolean enabled) {
		stub.updateCustomRule(rule, enabled);
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

		// TODO Add BASIC expansion.
		getExpansions().add(Expansion.BASIC);
		stub.updateExpansion(Expansion.BASIC, true);

		// Start the game
		stub.startGame();
	}

	@Override
	public void placeNoFigure() {
		stub.placeNoFigure();
	}

	@Override
	public void placeNoTile() {
		stub.placeNoTile();
	}

	@Override
	public void placeTile(Rotation rotation, Position position) {
		stub.placeTile(rotation, position);
	}

	@Override
	public void deployMeeple(Position p, Location d,
			Class<? extends Meeple> meepleType) {
		stub.deployMeeple(p, d, meepleType);
	}

	@Override
	public void placeTowerPiece(Position p) {
		stub.placeTowerPiece(p);
	}

	@Override
	public void escapeFromCity(Position p, Location d) {
		stub.escapeFromCity(p, d);
	}

	@Override
	public void removeKnightWithPrincess(Position p, Location d) {
		stub.removeKnightWithPrincess(p, d);
	}

	@Override
	public void captureFigure(Position p, Location d) {
		stub.captureFigure(p, d);
	}

	@Override
	public void placeTunnelPiece(Position p, Location d, boolean isSecondPiece) {
		stub.placeTunnelPiece(p, d, isSecondPiece);
	}

	@Override
	public void moveFairy(Position p) {
		stub.moveFairy(p);
	}

	@Override
	public void moveDragon(Position p) {
		stub.moveDragon(p);
	}

	@Override
	public void payRansom(Integer playerIndexToPay,
			Class<? extends Follower> meepleType) {
		stub.payRansom(playerIndexToPay, meepleType);
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
		stub.updateSlot(slot);
	}

	@Override
	public void selectTile(Integer tiles) {
		// generate random tile
		stub.nextTile(random.nextInt(tiles));
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

}
