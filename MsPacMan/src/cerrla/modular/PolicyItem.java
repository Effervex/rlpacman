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
 *    src/cerrla/modular/PolicyItem.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package cerrla.modular;

import org.apache.commons.collections.BidiMap;

/**
 * A simple interface to denote if an object is evaluatable as a relational
 * object.
 * 
 * @author Sam Sarjant
 */
public interface PolicyItem {

	/**
	 * The object needs to have goal parameters set to it.
	 * 
	 * @param goalArgs
	 */
	public void setParameters(BidiMap goalArgs);

	/**
	 * The object should have a method of looking like a nice string.
	 * 
	 * @return A nice String representation of the object.
	 */
	public String toNiceString();

	/**
	 * If the object should regenerate into a new modular policy.
	 * 
	 * @return True if the object should regenerate into a new modular policy.
	 */
	public boolean shouldRegenerate();

	/**
	 * Gets the size of this object.
	 * 
	 * @return 1 if this is a single object, more if it is a collection.
	 */
	public int size();
}
