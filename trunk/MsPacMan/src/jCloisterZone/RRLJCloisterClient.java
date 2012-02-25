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
