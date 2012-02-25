package jCloisterZone;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.phase.Phase;
import com.jcloisterzone.rmi.CallMessage;
import com.jcloisterzone.rmi.ClientIF;
import com.jcloisterzone.rmi.ServerIF;

public class LocalCarcassonneServerStub implements InvocationHandler {
	private Game game;

	public LocalCarcassonneServerStub(Game game) {
		this.game = game;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		CallMessage msg = new CallMessage(method, args);
		msg.call(game.getPhase(), ClientIF.class);

		runPhases();
		return null;
	}

	/**
	 * Cycles through the phases whenever necessary.
	 */
	private void runPhases() {
		Phase phase = game.getPhase();
		while (phase != null && !phase.isEntered()) {
			phase.setEntered(true);
			phase.enter();
			phase = game.getPhase();
		}
	}
}
