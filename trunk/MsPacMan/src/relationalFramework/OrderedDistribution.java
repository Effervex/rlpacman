package relationalFramework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * An ordered distribution is a distribution which attempts to optimise the
 * ordering of the elements contained within it.
 * 
 * @author Sam Sarjant
 */
public class OrderedDistribution<T> implements Collection<T> {
	/** The elements contained within the distribution. */
	private Collection<ItemProb<T>> elements_;

	/** The random number generator. */
	private Random random_;

	public OrderedDistribution(Random random) {
		random_ = random;
		elements_ = new ArrayList<ItemProb<T>>();
	}

	/**
	 * Creates a clone of this ordered distribution.
	 * 
	 * @return A clone of this distribution.
	 */
	@Override
	public OrderedDistribution<T> clone() {
		OrderedDistribution<T> clone = new OrderedDistribution<T>(random_);

		for (ItemProb<T> ip : elements_) {
			clone.add(ip.getItem(), ip.getProbability());
		}

		return clone;
	}

	/**
	 * Samples an element from the distribution.
	 * 
	 * @param order
	 *            The relative ordering from 0 to 1 that an element is being
	 *            sampled for.
	 * @param useMostLikely
	 *            Whether to use the most likely element or sample randomly.
	 * @return The element.
	 */
	public T sample(double order, boolean useMostLikely) {
		// Build a distribution, with modified weights based on element ordering
		// and the value asked for.
		ProbabilityDistribution<T> distribution = new ProbabilityDistribution<T>(
				random_);
		for (ItemProb<T> ip : elements_) {
			// Weight it such that 0 difference has full weight and a curve
			// drops from there to 0 weight at difference of 1.
			double difference = Math.abs(order - ip.getProbability());
			double weight = 1 - Math.sqrt(Math.sin(difference * Math.PI / 2));
			distribution.add(ip.getItem(), weight);
		}
		distribution.normaliseProbs();

		return distribution.sample(useMostLikely);
	}

	/**
	 * Samples an element from the distribution
	 * 
	 * @param index
	 *            The index the element is being sampled for.
	 * @param numElements
	 *            The number of elements being sampled for relative value
	 *            calculation.
	 * @param useMostLikely
	 *            Whether to simply use the most likely or just sample randomly.
	 * @return The element sampled.
	 */
	public T sample(int index, int numElements, boolean useMostLikely) {
		return sample(getRelativePosition(index, numElements), useMostLikely);
	}

	/**
	 * Samples an element from the distribution with removal.
	 * 
	 * @param index
	 *            The index the element is being sampled for.
	 * @param numElements
	 *            The number of elements being sampled for relative value
	 *            calculation.
	 * @param useMostLikely
	 *            Whether to simply use the most likely or just sample randomly.
	 * @return The element sampled.
	 */
	public T sampleWithRemoval(int index, int numElements, boolean useMostLikely) {
		T element = sample(index, numElements, useMostLikely);
		remove(element);
		return element;
	}

	/**
	 * Gets the elements contained within this distribution.
	 * 
	 * @return The elements contained within the distribution.
	 */
	public Collection<T> getElements() {
		Collection<T> elements = new ArrayList<T>();
		for (ItemProb<T> ip : elements_)
			elements.add(ip.getItem());
		return elements;
	}

	/**
	 * Gets all elements of this distribution in their most likely ordering.
	 * 
	 * @return A list of elements contains within this distribution ordered in
	 *         their most likely order.
	 */
	public List<T> getOrderedElements() {
		List<T> orderedElements = new ArrayList<T>();
		OrderedDistribution<T> cloneDist = clone();
		for (int i = 0; i < size(); i++) {
			orderedElements.add(cloneDist.sampleWithRemoval(i, size(), true));
		}
		return orderedElements;
	}

	/**
	 * Updates the distribution to reflect the observed ordering of elements.
	 * 
	 * @param elementPositions
	 *            The relative positions of the elements.
	 * @param stepSize
	 *            The step size update parameter.
	 * @return The KL divergence between the old probabilities and the new
	 *         probabilities.
	 */
	public double updateDistribution(Map<T, Double> elementPositions,
			double stepSize) {
		double diff = 0;
		for (ItemProb<T> element : elements_) {
			if (elementPositions.containsKey(element.getItem())) {
				// Get the old value
				double oldValue = element.getProbability();

				// If the element doesn't exist, it has an order of 1 (last)
				double newOrder = elementPositions.get(element.getItem());
				// Generate the new value
				double newValue = stepSize * newOrder + (1 - stepSize)
						* oldValue;

				element.setProbability(newValue);

				diff += Math.abs(newValue - oldValue);
			}
		}
		return diff;
	}

	/**
	 * Gets the relative positioning of an element within a list of elements
	 * where 0 is the first element and 1 is the last.
	 * 
	 * @param index
	 *            The index of the element.
	 * @param numElements
	 *            The number of elements in the collection.
	 * @return A double value between 0 and 1 (inclusive) representing the
	 *         relative index.
	 */
	public static double getRelativePosition(int index, int numElements) {
		double val = 0.5;
		if (numElements > 1)
			val = 1.0 * index / (numElements - 1);
		return val;
	}

	/**
	 * Finds the element's itemprob in the distribution
	 * 
	 * @param element
	 *            The element to be found.
	 * @return The itemprob for the element or null if it isn't there.
	 */
	private ItemProb<T> find(T element) {
		for (ItemProb<T> ip : elements_) {
			if (ip.getItem().equals(element))
				return ip;
		}
		return null;
	}

	/**
	 * Adds an element to the distribution with a default order of in the
	 * middle.
	 * 
	 * @param e
	 *            The element being added.
	 * @return True.
	 */
	@Override
	public boolean add(T e) {
		elements_.add(new ItemProb<T>(e, 0.5));
		return true;
	}

	/**
	 * Adds an element to the distribution with a given ordering.
	 * 
	 * @param e
	 *            The element being added.
	 * @param order
	 *            The order of the element.
	 * @return True.
	 */
	public boolean add(T e, double order) {
		if (order < 0)
			order = 0;
		if (order > 1)
			order = 1;
		elements_.add(new ItemProb<T>(e, order));
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		boolean val = false;
		for (T t : c)
			val |= add(t);
		return val;
	}

	@Override
	public void clear() {
		elements_.clear();
	}

	@Override
	public boolean contains(Object o) {
		if (find((T) o) != null)
			return false;
		return true;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Iterator iter = c.iterator(); iter.hasNext();) {
			if (!contains(iter.next()))
				return false;
		}
		return true;
	}

	@Override
	public boolean isEmpty() {
		return elements_.isEmpty();
	}

	@Override
	public Iterator<T> iterator() {
		ArrayList<T> items = new ArrayList<T>(elements_.size());
		for (ItemProb<T> ip : elements_) {
			items.add(ip.getItem());
		}
		return items.iterator();
	}

	@Override
	public boolean remove(Object o) {
		ItemProb<T> element = find((T) o);
		if (element == null)
			return false;
		return elements_.remove(element);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for (Object o : c) {
			changed |= remove(o);
		}
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		if (c == null)
			throw new NullPointerException();

		int size = elements_.size();
		for (Iterator<ItemProb<T>> iter = elements_.iterator(); iter.hasNext();) {
			ItemProb<T> element = iter.next();
			if (!c.contains(element.getItem()))
				elements_.remove(element);
		}

		// If the sizes haven't changed, return false.
		if (size == elements_.size())
			return false;

		return true;
	}

	@Override
	public int size() {
		return elements_.size();
	}

	@Override
	public Object[] toArray() {
		return null;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return null;
	}

	/**
	 * Gets the ordering of the element.
	 * 
	 * @param element
	 *            The element to find the ordering for.
	 * @return An ordering between 0 and 1 inclusive or -1 if not present.
	 */
	public double getOrdering(T element) {
		ItemProb<T> ip = find(element);
		if (ip != null)
			return ip.getProbability();
		return -1;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer("[");
		Iterator<ItemProb<T>> iter = elements_.iterator();
		if (iter.hasNext())
			buffer.append(iter.next());
		while (iter.hasNext()) {
			buffer.append(", " + iter.next());
		}
		buffer.append("]");
		return buffer.toString();
	}
}
