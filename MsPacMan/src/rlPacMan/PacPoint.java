package rlPacMan;

/**
 * Every PacPoint has a location within the PacMan world that is not on a wall
 * and can be reached by PacMan (unless in the ghost cage).
 * 
 * @author Sam Sarjant
 */
public abstract class PacPoint {
	int m_locX;
	int m_locY;

	@Override
	public String toString() {
		return "Point: " + m_locX + "," + m_locY;
	}
}
