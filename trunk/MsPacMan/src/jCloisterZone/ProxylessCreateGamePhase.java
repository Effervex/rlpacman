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
