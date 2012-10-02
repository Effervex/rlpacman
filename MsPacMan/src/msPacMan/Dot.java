/*
 *    This file is part of the CERRLA algorithm, but was originally obtained
 *    from bennychow.com. Used with permission.
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
 *    src/msPacMan/Dot.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package msPacMan;


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
	public String getObjectName() {
		return "dot";
	}

	@Override
	protected void updatePixelVals(GameUI gameUI) {
	}
}
