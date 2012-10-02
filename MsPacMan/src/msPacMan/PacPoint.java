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
 *    src/msPacMan/PacPoint.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package msPacMan;

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
