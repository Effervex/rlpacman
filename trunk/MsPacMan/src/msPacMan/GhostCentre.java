/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/msPacMan/GhostCentre.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package msPacMan;

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
