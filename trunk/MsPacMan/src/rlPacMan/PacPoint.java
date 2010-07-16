package rlPacMan;

/**
 * Every PacPoint has a location within the PacMan world that is not on a wall
 * and can be reached by PacMan (unless in the ghost cage).
 * 
 * @author Sam Sarjant
 */
public abstract class PacPoint {
	public int m_locX;
	public int m_locY;
	
	public abstract String getObjectName();

	@Override
	public String toString() {
		return getObjectName() + "_" + m_locX + "_" + m_locY;
	}
}
