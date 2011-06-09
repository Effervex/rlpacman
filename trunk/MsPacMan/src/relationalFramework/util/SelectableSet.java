package relationalFramework.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class SelectableSet<T> implements Collection<T>, Serializable {
	private static final long serialVersionUID = -6025101660944005158L;
	private Map<T, T> slots_ = new HashMap<T, T>();

	/**
	 * Adds all the items only if they aren't already in the collection.
	 * 
	 * @param values The things to add.
	 */
	public boolean addContainsAll(Collection<T> values) {
		boolean changed = false;
		for (T element : values) {
			if (!contains(element))
				changed |= add(element);
		}
		return changed;
	}

	/**
	 * Searches for a matching element (i.e. one that satisfies equals) and
	 * returns it.
	 * 
	 * @param item
	 *            The item to search for.
	 * @return The matching item in the collection as defined by equals if
	 *         possible, otherwise null.
	 */
	public T findMatch(T item) {
		return slots_.get(item);
	}

	@Override
	public boolean add(T e) {
		T val = slots_.put(e, e);
		return (val == null) ? true : false;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		boolean result = false;
		for (T element : c)
			result |= add(element);
		return result;
	}

	@Override
	public void clear() {
		slots_.clear();
	}

	@Override
	public boolean contains(Object o) {
		return slots_.containsKey(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		boolean result = true;
		for (Object element : c)
			result &= contains(element);
		return result;
	}

	@Override
	public boolean isEmpty() {
		return slots_.isEmpty();
	}

	@Override
	public Iterator<T> iterator() {
		return slots_.keySet().iterator();
	}

	@Override
	public boolean remove(Object o) {
		T element = slots_.remove(o);
		return (element != null);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for (Object element : c)
			changed |= remove(element);
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		Collection<T> removables = new HashSet<T>(slots_.size());
		for (T element : slots_.keySet())
			if (!c.contains(element))
				removables.add(element);
		return removeAll(removables);
	}

	@Override
	public int size() {
		return slots_.size();
	}

	@Override
	public Object[] toArray() {
		return slots_.keySet().toArray();
	}

	@SuppressWarnings("hiding")
	@Override
	public <T> T[] toArray(T[] a) {
		return slots_.keySet().toArray(a);
	}
}
