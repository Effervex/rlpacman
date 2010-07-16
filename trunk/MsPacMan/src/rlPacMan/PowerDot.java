package rlPacMan;

/**
 * A power dot in the PacMan world. Worth 40 points and causes ghosts to be edible for a time.
 * 
 * @author Sam Sarjant
 */
public class PowerDot extends Thing {
	/** The value of the powerdot. */ 
	protected int value_;
	
	/**
	 * A constructor.
	 * 
	 * @param gameModel The current game model.
	 * @param x The x location.
	 * @param y The y location.
	 * @param bMiddleX The middle param. Not needed.
	 */
	public PowerDot(GameModel gameModel, int x, int y) {
		super(gameModel, x, y, false);
		m_locX = x;
		m_locY = y;
		value_ = 40;
	}
	
	@Override
	public String getObjectName() {
		return "powerDot";
	}
	
	/**
	 * Gets the value of the dot.
	 * 
	 * @return The dot value.
	 */
	public int getValue() {
		return value_;
	}

	@Override
	protected void updatePixelVals(GameUI gameUI) {
	}
}
