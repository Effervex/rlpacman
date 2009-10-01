package rlPacMan;

/**
 * A power dot in the PacMan world. Worth 40 points and causes ghosts to be edible for a time.
 * 
 * @author Sam Sarjant
 */
public class PowerDot extends Dot {
	/**
	 * A constructor.
	 * 
	 * @param gameModel The current game model.
	 * @param x The x location.
	 * @param y The y location.
	 * @param bMiddleX The middle param. Not needed.
	 */
	public PowerDot(GameModel gameModel, int x, int y) {
		super(gameModel, x, y);
		value_ = 40;
	}
}
