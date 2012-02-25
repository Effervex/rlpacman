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

	private ClientIF getStub() {
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

}
