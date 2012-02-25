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
		running_ = true;
		super.createGame();
	}
	
	@Override
	public boolean closeGame(boolean force) {
		running_ = false;
		return super.closeGame(force);
	}

	public boolean isRunning() {
		return running_;
	}
}
