package relationalFramework.util;

public class Pair<A,B> {
	public A objA_;
	public B objB_;
	
	public Pair(A a, B b) {
		objA_ = a;
		objB_ = b;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((objA_ == null) ? 0 : objA_.hashCode());
		result = prime * result + ((objB_ == null) ? 0 : objB_.hashCode());
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
		Pair other = (Pair) obj;
		if (objA_ == null) {
			if (other.objA_ != null)
				return false;
		} else if (!objA_.equals(other.objA_))
			return false;
		if (objB_ == null) {
			if (other.objB_ != null)
				return false;
		} else if (!objB_.equals(other.objB_))
			return false;
		return true;
	}
	
	
}
