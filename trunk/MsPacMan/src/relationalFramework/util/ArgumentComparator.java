package relationalFramework.util;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A class for comparing string array arguments against one another.
 * 
 * @author Sam Sarjant
 */
public class ArgumentComparator implements Comparator<String[]>, Serializable {
	private static final long serialVersionUID = -3892475329673001070L;
	private static ArgumentComparator instance_;
	
	private ArgumentComparator() {
		
	}
	
	@Override
	public int compare(String[] o1, String[] o2) {
		if (o1 == null) {
			if (o2 == null)
				return 0;
			return 1;
		} else if (o2 == null)
			return -1;
		
		// Compare the sizes
		int result = Double.compare(o1.length, o2.length);
		if (result != 0)
			return result;
		
		// Compare arguments one-by-one
		for (int i = 0; i < o1.length; i++) {
			result = o1[i].compareTo(o2[i]);
			if (result != 0)
				return result;
		}
		return 0;
	}

	public static Comparator<String[]> getInstance() {
		if (instance_ == null)
			instance_ = new ArgumentComparator();
		return instance_;
	}
}
