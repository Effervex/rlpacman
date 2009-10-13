package rlPacMan;

/**
 * A dot in the PacMan world. Worth 10 points.
 * 
 * @author Sam Sarjant
 */
public class Dot extends Thing {
	/** The value of the dot. */ 
	protected int value_;
	
	/**
	 * A constructor.
	 * 
	 * @param gameModel The current game model.
	 * @param x The x location.
	 * @param y The y location.
	 * @param bMiddleX The middle param. Not needed.
	 */
	public Dot(GameModel gameModel, int x, int y) {
		super(gameModel, x, y, false);
		m_locX = x;
		m_locY = y;
		value_ = 10;
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
		// TODO Auto-generated method stub
		
	}
	
	public String toString() {
		return "Dot: " + m_locX + "," + m_locY;
	}
}
