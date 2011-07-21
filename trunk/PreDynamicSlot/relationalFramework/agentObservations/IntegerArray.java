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
