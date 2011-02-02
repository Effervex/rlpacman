package relationalFramework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * An ordered distribution is a distribution which attempts to optimise the
 * ordering of the elements contained within it.
 * 
 * @author Sam Sarjant
 */
public class OrderedDistribution<T> implements Collection<T>, Serializable {
	private static final long serialVersionUID = 1157875397663702332L;

	/** The elements contained within the distribution. */
	private Map<T, Double> elements_;

	/**
	 * A map for accessing an element using an equal (but not necessarily the
	 * same) object.
	 */
	private Map<T, T> elementSelfMapping_;

	/** The random number generator. */
	private Random random_;

	public OrderedDistribution(Random random) {
		random_ = random;
		elements_ = new HashMap<T, Double>();
		elementSelfMapping_ = new HashMap<T, T>();
	}

	/**
	 * Creates a clone of this ordered distribution.
	 * 
	 * @return A clone of this distribution.
	 */
	@Override
	public OrderedDistribution<T> clone() {
		OrderedDistribution<T> clone = new OrderedDistribution<T>(random_);
		clone.elements_ = new HashMap<T, Double>(elements_);
		clone.elementSelfMapping_ = new HashMap<T, T>(elementSelfMapping_);

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
		for (T element : elements_.keySet()) {
			// Weight it such that 0 difference has full weight and a curve
			// drops from there to 0 weight at difference of 1.
			double difference = Math.abs(order - elements_.get(element));
			double weight = 1 - Math.sqrt(Math.sin(difference * Math.PI / 2));
			distribution.add(element, weight);
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
	 *            If the most likely element is to be chosen.
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
	 *            If the most likely element is to be chosen.
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
		return elements_.keySet();
	}

	/**
	 * Gets a particular element using an equal element as the key.
	 * 
	 * @param equalElement
	 *            The equal element.
	 * @return An element equal to the element but not necessarily the same.
	 */
	public T getElement(T equalElement) {
		return elementSelfMapping_.get(equalElement);
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
	 *            The sampled relative positions of the elements.
	 * @param stepSize
	 *            The step size update parameter.
	 * @return The difference between the old probabilities and the new
	 *         probabilities.
	 */
	public double updateDistribution(Map<T, Double> elementPositions,
			double stepSize) {
		double diff = 0;
		for (T element : elements_.keySet()) {
			diff += updateElement(elementPositions, stepSize, element);
		}
		return diff;
	}

	/**
	 * Updates the distribution to reflect the observed ordering of elements
	 * using individual step sizes.
	 * 
	 * @param elementPositions
	 *            The sampled relative positions of the elements.
	 * @param stepSizes
	 *            The individual step sizes for each element.
	 * @return The difference between the old probabilities and the new
	 *         probabilities.
	 */
	public double updateDistribution(Map<T, Double> elementPositions,
			Map<T, Double> stepSizes) {
		double diff = 0;
		for (T element : elements_.keySet()) {
			if (stepSizes.containsKey(element)) {
				diff += updateElement(elementPositions, stepSizes.get(element),
						element);
			}
		}
		return diff;
	}

	/**
	 * Updates a single element in the distribution.
	 * 
	 * @param elementPositions
	 *            The sampled relative positions of the elements.
	 * @param stepSize
	 *            The step size to update the element by.
	 * @param element
	 *            The element to update.
	 * @return The difference between the element's old value and the new one.
	 */
	private double updateElement(Map<T, Double> elementPositions,
			double stepSize, T element) {
		double diff = 0;
		if (elementPositions.containsKey(element)) {
			// Get the old value
			double oldValue = elements_.get(element);

			// If the element doesn't exist, it has an order of 1 (last)
			double newOrder = elementPositions.get(element);
			// Generate the new value
			double newValue = stepSize * newOrder + (1 - stepSize) * oldValue;

			elements_.put(element, newValue);

			diff = Math.abs(newValue - oldValue);
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
	 * Adds an element to the distribution with a default order of in the
	 * middle.
	 * 
	 * @param e
	 *            The element being added.
	 * @return True.
	 */
	@Override
	public boolean add(T e) {
		elements_.put(e, 0.5);
		elementSelfMapping_.put(e, e);
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
		elements_.put(e, order);
		elementSelfMapping_.put(e, e);
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		boolean val = false;
		for (T t : c)
			val |= add(t);
		return val;
	}

	public boolean addContainsAll(Collection<? extends T> c) {
		boolean val = false;
		for (T t : c) {
			if (!contains(t))
				val |= add(t);
		}
		return val;
	}

	@Override
	public void clear() {
		elements_.clear();
		elementSelfMapping_.clear();
	}

	@Override
	public boolean contains(Object o) {
		return elements_.containsKey(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return elements_.keySet().containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return elements_.isEmpty();
	}

	@Override
	public Iterator<T> iterator() {
		return elements_.keySet().iterator();
	}

	@Override
	public boolean remove(Object o) {
		Double val = elements_.remove(o);
		elementSelfMapping_.remove(o);
		if (val == null)
			return false;
		return true;
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
		Set<T> elementSet = new HashSet<T>(elements_.keySet());
		for (T element : elementSet) {
			if (!c.contains(element))
				remove(element);
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

	@SuppressWarnings("hiding")
	@Override
	public <T> T[] toArray(T[] a) {
		return null;
	}

	/**
	 * Gets the ordering of the element.
	 * 
	 * @param element
	 *            The element to find the ordering for.
	 * @return An ordering between 0 and 1 inclusive or null if not present.
	 */
	public Double getOrdering(T element) {
		return elements_.get(element);
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		boolean first = true;
		for (T element : elements_.keySet()) {
			if (!first)
				buffer.append("\n");
			buffer.append("(" + element + ":" + elements_.get(element) + ")");
			first = false;
		}
		return buffer.toString();
	}
}
