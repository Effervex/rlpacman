package jCloisterZone;

import relationalFramework.PolicyActions;
import relationalFramework.RelationalWrapper;
import jess.Rete;

public class CarcassonneRelationalWrapper extends RelationalWrapper {

	@Override
	protected Rete assertStateFacts(Rete rete, Object... args) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean isReteDriven() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object groundActions(PolicyActions actions, Object... args) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
