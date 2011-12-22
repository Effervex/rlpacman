package jCloisterZone;

import java.util.List;

import jess.Rete;
import relationalFramework.PolicyActions;
import rrlFramework.RRLEnvironment;

import com.jcloisterzone.game.Game;

public class CarcassonneEnvironment extends RRLEnvironment {
	private Game environment_;
	private boolean guiMode_ = false;
	
	@Override
	protected void assertStateFacts(Rete rete, List<String> goalArgs)
			throws Exception {
		// TODO Auto-generated method stub
		
	}
	@Override
	protected double calculateReward(boolean isTerminal) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	protected List<String> getGoalArgs() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	protected Object groundActions(PolicyActions actions) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	protected boolean isReteDriven() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void cleanup() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void initialise(int runIndex, String[] extraArg) {
		// TODO Auto-generated method stub
		
	}
	@Override
	protected void startState() {
		// TODO Auto-generated method stub
		
	}
	@Override
	protected void stepState(Object action) {
		// TODO Auto-generated method stub
		
	}
}
