package relationalFramework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MultiMap<K, V> implements Serializable {
	private static final long serialVersionUID = -5907393112846626155L;
	/** An inner map containing the values. C is implicitly of type V. */
	private Map<K, List<V>> innerMap_;

	/**
	 * The constructor.
	 */
	public MultiMap() {
		innerMap_ = new HashMap<K, List<V>>();
	}
	
	public MultiMap(MultiMap<K, V> mm) {
		this();
		putAll(mm);
	}

	/**
	 * Initialises/gets the list under a key.
	 * 
	 * @param key
	 *            The key to get the list from.
	 * @return The newly created/pre-existing list.
	 */
	private List<V> initialiseGetList(K key) {
		// Initialise the list
		List<V> list = innerMap_.get(key);
		if (list == null) {
			list = new ArrayList<V>();
			innerMap_.put(key, list);
		}
		return list;
	}

	/**
	 * Clears the entire multimap.
	 */
	public void clear() {
		innerMap_.clear();
	}

	/**
	 * Clears the values from a key.
	 * 
	 * @param key
	 *            The key for clearing values under the key.
	 * @return True if the key is valid, false otherwise.
	 */
	public boolean clearValues(K key) {
		if (key == null)
			return false;
		innerMap_.put(key, null);
		return true;
	}

	/**
	 * Checks if the multimap contains the specified key.
	 * 
	 * @param key
	 *            The key being searched.
	 * @return True if the key is present.
	 */
	public boolean containsKey(Object key) {
		return innerMap_.containsKey(key);
	}

	/**
	 * Checks if the multimap contains the specified value (not collection).
	 * 
	 * @param The
	 *            value being searched for.
	 * @return True if the value is present within any of the collection values
	 *         in the multimap.
	 */
	public boolean containsValue(Object value) {
		for (K key : keySet()) {
			if (innerMap_.get(key).contains(value))
				return true;
		}
		return false;
	}

	/**
	 * Checks if the multimap contains a specified list as a mapped value.
	 * 
	 * @param list
	 *            The list being searched for.
	 * @return True if the list is a mapped value.
	 */
	public boolean containsList(List list) {
		return innerMap_.containsValue(list);
	}

	/**
	 * Gets the list of values under the multimap key.
	 * 
	 * @param key
	 *            The key to retrieve values from.
	 * @return The list under the key, or null.
	 */
	public List<V> get(Object key) {
		return innerMap_.get(key);
	}

	/**
	 * Gets the element at a specified index in the multimap value.
	 * 
	 * @param key
	 *            The key for the value.
	 * @param index
	 *            The index of the element at the key.
	 * @return The value at the index, or null if the value doesn't exist or the
	 *         index is out of range.
	 */
	public V getIndex(Object key, int index) {
		List<V> list = get(key);
		if ((list != null) && (index < list.size())) {
			return list.get(index);
		}
		return null;
	}

	/**
	 * Checks if the entire multimap is empty.
	 * 
	 * @return True if the multimap is empty.
	 */
	public boolean isKeysEmpty() {
		return innerMap_.isEmpty();
	}

	/**
	 * Returns true if the collection under this key is empty.
	 * 
	 * @param key
	 *            The key for the collection.
	 * @return True if the collection is empty.
	 */
	public boolean isValueEmpty(Object key) {
		List<V> values = innerMap_.get(key);
		if (values == null)
			return true;
		return values.isEmpty();
	}

	/**
	 * Checks if all values under every key are empty.
	 * 
	 * @return True if all values are empty.
	 */
	public boolean allValuesEmpty() {
		for (K key : keySet()) {
			if (!isValueEmpty(key))
				return false;
		}
		return true;
	}

	/**
	 * Gets the set of keys for this multimap.
	 * 
	 * @return The set of keys.
	 */
	public Set<K> keySet() {
		return innerMap_.keySet();
	}

	/**
	 * Adds a singular value to the multimap collection under a key.
	 * 
	 * @param key
	 *            The key to add the value to.
	 * @param value
	 *            The value to add to the collection.
	 * @return The resultant collection, containing the value.
	 */
	public List<V> put(K key, V value) {
		List<V> resultantCollection = initialiseGetList(key);

		// Adding the values
		if (value != null)
			resultantCollection.add(value);

		return resultantCollection;
	}

	/**
	 * Puts all values in a collection into the multimap.
	 * 
	 * @param key
	 *            The key to put the values under.
	 * @param collection
	 *            The collection containing the values to add.
	 * @return The resultant collection.
	 */
	public List<V> putCollection(K key, Collection<? extends V> collection) {
		List<V> resultantCollection = initialiseGetList(key);

		// Adding the values
		resultantCollection.addAll(collection);

		return resultantCollection;
	}

	/**
	 * Puts all values from a multi-map into this multi-map, but not
	 * overwriting.
	 * 
	 * @param mm
	 *            The multimap to add.
	 */
	public void putAll(MultiMap<? extends K, ? extends V> mm) {
		Set<? extends K> keySet = mm.keySet();
		for (K key : keySet) {
			putCollection(key, mm.get(key));
		}
	}

	/**
	 * Puts all values from a multi-map into this multi-map, but not
	 * overwriting.
	 * 
	 * @param mm
	 *            The multimap to add.
	 * @return True if the map was changed at all, false otherwise.
	 */
	public boolean putAllContains(MultiMap<? extends K, ? extends V> mm) {
		Set<? extends K> keySet = mm.keySet();
		boolean result = false;
		for (K key : keySet) {
			result |= putContains(key, mm.get(key));
		}

		return result;
	}

	/**
	 * Adds a singular value to the multimap collection under a key if it is not
	 * already there.
	 * 
	 * @param key
	 *            The key to add the value to.
	 * @param value
	 *            The value to add to the collection, unless it is already
	 *            there.
	 * @return True if the value was added.
	 */
	public boolean putContains(K key, V value) {
		List<V> resultantCollection = initialiseGetList(key);

		// Adding the values
		if (!resultantCollection.contains(value)) {
			resultantCollection.add(value);
			return true;
		}

		return false;
	}

	/**
	 * Puts all values in a collection into the multimap.
	 * 
	 * @param key
	 *            The key to put the values under.
	 * @param collection
	 *            The collection containing the values to add.
	 * @return True if the value was added.
	 */
	public boolean putContains(K key, Collection<? extends V> collection) {
		List<V> resultantCollection = initialiseGetList(key);

		// Adding the values
		boolean result = false;
		for (V value : collection) {
			if (!resultantCollection.contains(value)) {
				resultantCollection.add(value);
				result = true;
			}
		}

		return result;
	}

	/**
	 * Explicitly replaces a value if it is equal to the value being added.
	 * 
	 * @param key The key to place the value under.
	 * @param value The object that is guaranteed to be added to the collection.
	 */
	public void putReplace(K key, V value) {
		List<V> resultantCollection = initialiseGetList(key);
		
		if (resultantCollection.contains(value))
			resultantCollection.set(resultantCollection.indexOf(value), value);
		else
			resultantCollection.add(value);
	}

	/**
	 * Removes a key from the multimap and returns the list contained under the
	 * key.
	 * 
	 * @param key
	 *            The key to remove.
	 * @return The list contained under the key.
	 */
	public List<V> remove(Object key) {
		return innerMap_.remove(key);
	}

	/**
	 * Gets the size of this multimap, that is, the number of key-value
	 * mappings.
	 * 
	 * @return The number of keys in the map.
	 */
	public int size() {
		return innerMap_.size();
	}

	/**
	 * Gets the total summed size of this multimap, that is, the sum of sizes
	 * for each value in the mapping.
	 * 
	 * @return The total number of values in the multimap.
	 */
	public int sizeTotal() {
		return values().size();
	}

	/**
	 * Gets every value present in the multimap.
	 * 
	 * @return All the values present in the multimap.
	 */
	public Collection<V> values() {
		Collection<V> values = new ArrayList<V>();
		for (List<V> valueLists : valuesLists()) {
			values.addAll(valueLists);
		}
		return values;
	}

	/**
	 * Gets the lists of values in the middle level of the multimap.
	 * 
	 * @return All the lists containing the values.
	 */
	public Collection<List<V>> valuesLists() {
		return innerMap_.values();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((innerMap_ == null) ? 0 : innerMap_.hashCode());
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final MultiMap other = (MultiMap) obj;
		if (innerMap_ == null) {
			if (other.innerMap_ != null)
				return false;
		} else if (!innerMap_.equals(other.innerMap_))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return innerMap_.toString();
	}
}