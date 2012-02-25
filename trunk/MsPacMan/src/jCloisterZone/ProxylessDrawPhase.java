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
