package rlPacMan;

import java.awt.Point;

public class GhostCentre extends PacPoint {
	public GhostCentre(Point centrePoint) {
		this.m_locX = centrePoint.x;
		this.m_locY = centrePoint.y;
	}

	@Override
	public String getObjectName() {
		return "ghostCentre";
	}
}
