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
 *    src/util/SlotOrderComparator.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package util;

import java.io.Serializable;
import java.util.Comparator;

import cerrla.Slot;


public class SlotOrderComparator implements Comparator<Slot>, Serializable {
	private static final long serialVersionUID = 3925398461164725398L;
	private static SlotOrderComparator instance_;

	private SlotOrderComparator() {
	}

	@Override
	public int compare(Slot o1, Slot o2) {
		int result = -Double.compare(o1.getSelectionProbability(),
				o2.getSelectionProbability());
		if (result != 0)
			return result;

		result = Double.compare(o1.getOrdering(), o2.getOrdering());
		if (result == 0) {
			result = Double.compare(o1.getOrderingSD(), o2.getOrderingSD());
			if (result == 0)
				return o1.compareTo(o2);
		}
		return result;
	}

	public static SlotOrderComparator getInstance() {
		if (instance_ == null)
			instance_ = new SlotOrderComparator();
		return instance_;
	}
}
