package relationalFramework.util;

import java.io.Serializable;
import java.util.Comparator;

import relationalFramework.Slot;

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
