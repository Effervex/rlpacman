package jCloisterZone;

import java.lang.reflect.Method;

import com.jcloisterzone.Player;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.PlayerSlot;
import com.jcloisterzone.rmi.CallMessage;
import com.jcloisterzone.rmi.ServerIF;
import com.jcloisterzone.rmi.mina.ClientStub;

public class LocalCarcassonneClientStub extends ClientStub {
	public LocalCarcassonneClientStub() {
		game = new Game();
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		CallMessage msg = new CallMessage(method, args);
		msg.call(getServerProxy(), ServerIF.class);
		return null;
	}

	@Override
	public long getClientId() {
		return 1;
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
