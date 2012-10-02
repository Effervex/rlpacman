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
 *    src/relationalFramework/agentObservations/IntegerArray.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package relationalFramework.agentObservations;

import java.io.Serializable;
import java.util.Arrays;

public class IntegerArray implements Serializable {
	private static final long serialVersionUID = 3105015301957077580L;
	public int[] array_;
	
	public IntegerArray(int[] array) {
		array_ = array;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(array_);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntegerArray other = (IntegerArray) obj;
		if (!Arrays.equals(array_, other.array_))
			return false;
		return true;
	}
}
