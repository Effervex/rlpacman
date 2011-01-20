package relationalFramework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A mapping class which can have mutable keys (keys that change while within
 * the map). This is at the cost of speed, so large collections of objects
 * should not be used.
 * 
 * @author Sam Sarjant
 * 
 * @param <K> The mutable key.
 * @param <V> The value.
 */
public class MutableKeyMap<K, V> implements Map<K, V>, Serializable {
	private static final long serialVersionUID = -8073411368437257886L;
	private List<K> keys_;
	private List<V> values_;

	public MutableKeyMap() {
		keys_ = new ArrayList<K>();
		values_ = new ArrayList<V>();
	}

	public MutableKeyMap(Map<K, V> m) {
		this();
		putAll(m);
	}

	@Override
	public void clear() {
		keys_.clear();
		values_.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return keys_.contains(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return values_.contains(value);
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return null;
	}

	@Override
	public V get(Object key) {
		int index = keys_.indexOf(key);
		if (index != -1)
			return values_.get(index);
		return null;
	}

	@Override
	public boolean isEmpty() {
		return keys_.isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return new HashSet<K>(keys_);
	}

	@Override
	public V put(K key, V value) {
		int index = keys_.indexOf(key);
		if (index == -1) {
			keys_.add(key);
			values_.add(value);
			return null;
		} else {
			keys_.set(index, key);
			V oldVal = values_.get(index);
			values_.set(index, value);
			return oldVal;
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (K key : m.keySet()) {
			put(key, m.get(key));
		}
	}

	@Override
	public V remove(Object key) {
		int index = keys_.indexOf(key);
		if (index != -1) {
			keys_.remove(index);
			return values_.remove(index);
		}
		return null;
	}

	@Override
	public int size() {
		return keys_.size();
	}

	@Override
	public Collection<V> values() {
		return values_;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer("{");
		Iterator<K> keyIter = keys_.iterator();
		Iterator<V> valueIter = values_.iterator();
		boolean first = true;
		while (keyIter.hasNext()) {
			if (!first)
				buffer.append(", ");
			buffer.append(keyIter.next() + "=" + valueIter.next());
			first = false;
		}
		buffer.append("}");
		return buffer.toString();
	}
}
