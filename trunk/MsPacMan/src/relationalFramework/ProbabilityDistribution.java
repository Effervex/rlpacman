package relationalFramework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * A class representing a probability distribution of values. These values are
 * sampled randomly according to their probabilities.
 * 
 * @author Samuel J. Sarjant
 * 
 */
public class ProbabilityDistribution<T> implements Collection<T>, Serializable {
	private static final long serialVersionUID = -5600686052439795893L;
	/** The instances in the distribution with associated weights. */
	private Map<T, Double> itemProbs_;
	/** The random number generator. */
	protected Random random_;

	/**
	 * A constructor for the probability distribution.
	 */
	public ProbabilityDistribution() {
		random_ = new Random();
		itemProbs_ = new MutableKeyMap<T, Double>();
	}

	/**
	 * A constructor for the probability distribution.
	 * 
	 * @param random
	 *            A given random value.
	 */
	public ProbabilityDistribution(Random random) {
		random_ = random;
		itemProbs_ = new MutableKeyMap<T, Double>();
	}

	/**
	 * Samples a random weighted element from the distribution. This assumes all
	 * probabilities sum to 1.
	 * 
	 * @param useMostLikely
	 *            If we sample the most likely element.
	 * @return The element sampled, according to weight, or null.
	 */
	public T sample(boolean useMostLikely) {
		if (itemProbs_.size() == 0)
			return null;

		// If using most likely, just get the highest prob one
		if (useMostLikely)
			return getOrderedElements().get(0);

		double val = random_.nextDouble();
		double tally = 0;
		T lastItem = null;
		for (T item : itemProbs_.keySet()) {
			tally += itemProbs_.get(item);
			if (val < tally)
				return item;

			// Just in case
			lastItem = item;
		}
		return lastItem;
	}

	/**
	 * Samples a random weighted element from the distribution and removes it.
	 * 
	 * @param useMostLikely
	 *            If we sample the most likely element.
	 * @return An element sampled from the distribution with removal.
	 */
	public T sampleWithRemoval(boolean useMostLikely) {
		T result = sample(useMostLikely);
		remove(result);
		normaliseProbs();
		return result;
	}

	/**
	 * Adds an element with a probability of 1. It is recommended that the
	 * distribution is normalised or manually modified after this operation.
	 * 
	 * @param element
	 *            The element being added.
	 * @return True if the element was added.
	 */
	public boolean add(T element) {
		itemProbs_.put(element, 1d);
		return true;
	}

	/**
	 * Adds an element with a specified probability.
	 * 
	 * @param element
	 *            The element being added.
	 * @param prob
	 *            The initial probability of the element.
	 * @return True if the collection was modified.
	 */
	public boolean add(T element, double prob) {
		itemProbs_.put(element, prob);
		return true;
	}

	/**
	 * Gets the probability for an element if it is present.
	 * 
	 * @param element
	 *            The element with a probability.
	 * @return The probability of the rule, or -1 if it isn't present.
	 */
	public Double getProb(T element) {
		return itemProbs_.get(element);
	}

	/**
	 * Gets the element equal to the argument element.
	 * 
	 * @param equalElement
	 *            The element equal to another element in this distribution.
	 * @return The element or null.
	 */
	public T getElement(T equalElement) {
		Iterator<T> iter = iterator();
		while (iter.hasNext()) {
			T element = iter.next();
			if (element.equals(equalElement))
				return element;
		}
		return null;
	}

	/**
	 * Sets the probability of an element to a new probability. This may affect
	 * the 'sums-to-one' criteria.
	 * 
	 * @param element
	 *            The element being set.
	 * @param newProb
	 *            The new probability of the value.
	 * @return True if this contains the element, false otherwise.
	 */
	public boolean set(T element, double newProb) {
		if (itemProbs_.containsKey(element)) {
			itemProbs_.put(element, newProb);
			return true;
		}
		return false;
	}

	/**
	 * Adds all elements from a collection, with an initial probability of 1/K,
	 * where K is the size of the collection.
	 * 
	 * @param elements
	 *            The collection being added.
	 * @return True if the collection was modified.
	 */
	public boolean addAll(Collection<? extends T> elements) {
		if ((elements == null) || (elements.size() == 0))
			return false;
		boolean result = false;
		double prob = 1.0 / elements.size();
		for (T item : elements) {
			result = result | add(item, prob);
		}
		return result;
	}

	/**
	 * Adds all elements from a collection, with an initial probability of prob.
	 * 
	 * @param arg0
	 *            The collection being added.
	 * @param prob
	 *            The initial probability for all elements added.
	 * @return True if the collection was modified.
	 */
	public boolean addAll(Collection<? extends T> arg0, double prob) {
		if ((arg0 == null) || (arg0.size() == 0))
			return false;
		boolean result = false;
		for (T item : arg0) {
			result = result | add(item, prob);
		}
		return result;
	}

	/**
	 * Checks if the probabilities sum to one.
	 * 
	 * @return True if the probabilities all sum to one.
	 */
	public boolean sumsToOne() {
		double sum = 0;
		for (Double d : itemProbs_.values()) {
			sum += d;
		}
		if ((sum >= 0.9999) && (sum <= 1.0001))
			return true;
		return false;
	}

	/**
	 * Normalises the probabilities to sum to one.
	 */
	public void normaliseProbs() {
		// Get total
		double sum = 0;
		for (Double d : itemProbs_.values()) {
			sum += d;
		}

		// If already at 1, just return.
		if ((sum >= 0.9999) && (sum <= 1.0001))
			return;

		// Normalise
		int count = itemProbs_.size();
		for (T element : itemProbs_.keySet()) {
			// If the sum is 0, everything is equal.
			if (sum == 0)
				itemProbs_.put(element, 1.0 / count);
			else
				itemProbs_.put(element, itemProbs_.get(element) / sum);
		}
	}

	/**
	 * Creates a clone distribution and restricts the values of the distribution
	 * to 0 or 1, in the case of binary. When binary, this is determined by
	 * whether the value is above or below 0.5. When not binary, all values
	 * below the mean value are set to 0 and those remaining are normalised.
	 * 
	 * @param binary
	 *            If the values are to be set to 0 and 1, split by 0.5.
	 * @return The cloned and bound distribution.
	 */
	public ProbabilityDistribution<T> bindProbs(boolean binary) {
		ProbabilityDistribution<T> clone = clone();

		// Calculate the average
		double average = 0.5;
		if (!binary) {
			average = 1.0 / size();
		}

		// Set those below the average to 0 and (if binary) above to 1
		for (T element : itemProbs_.keySet()) {
			if (getProb(element) < average) {
				clone.set(element, 0);
			} else if (binary) {
				clone.set(element, 1);
			}
		}

		// Normalise if not binary
		if (!binary)
			clone.normaliseProbs();
		return clone;
	}

	/**
	 * Gets the elements of this distribution in an ordered list, from most
	 * likely to least.
	 * 
	 * @return The elements of the distribution in order.
	 */
	public ArrayList<T> getOrderedElements() {
		ArrayList<T> ordered = new ArrayList<T>(itemProbs_.keySet());
		Collections.sort(ordered, new ProbabilityComparator<T>());

		return ordered;
	}

	/**
	 * Gets the elements of this distribution in an ordered list, from most
	 * likely to least, excluding zero-probability elements.
	 * 
	 * @return The non-zero elements of the distribution in order.
	 */
	public ArrayList<T> getNonZeroOrderedElements() {
		ArrayList<T> ordered = getOrderedElements();
		for (Iterator<T> iter = ordered.iterator(); iter.hasNext();) {
			T element = iter.next();
			if (itemProbs_.get(element) == 0)
				iter.remove();
		}
		return ordered;
	}

	/**
	 * Gets all non-zero elements in this distribution.
	 * 
	 * @return An arraylist of elements with non-zero probabilities.
	 */
	public ArrayList<T> getNonZero() {
		ArrayList<T> nonZeroes = new ArrayList<T>();
		for (T element : itemProbs_.keySet()) {
			if (itemProbs_.get(element) > 0) {
				nonZeroes.add(element);
			}
		}
		return nonZeroes;
	}

	/**
	 * Updates this distribution using the cross-entropy method.
	 * 
	 * @param numSamples
	 *            The number of samples used for the counts.
	 * @param counts
	 *            The counts of each of the elements.
	 * @param stepSize
	 *            The step size for the update.
	 * @return The absolute amount of difference in the probabilities for each
	 *         element.
	 */
	public double updateDistribution(double numSamples, Map<T, Double> counts,
			double stepSize) {
		double absoluteChange = 0;
		if (numSamples != 0) {
			// For each of the rules within the distribution
			for (T element : itemProbs_.keySet()) {
				// Update every element within the distribution
				Double itemCount = counts.get(element);
				if (itemCount == null)
					itemCount = 0d;
				absoluteChange += Math.abs(updateElement(element, numSamples,
						itemCount, stepSize));
			}

			// Normalise the probabilities
			normaliseProbs();
		}

		return absoluteChange;
	}

	/**
	 * Updates a single element within the distribution.
	 * 
	 * @param element
	 *            The element being updated.
	 * @param numSamples
	 *            The number of samples used for the counts.
	 * @param count
	 *            The count for this element.
	 * @param stepSize
	 *            The step size for the update.
	 * @return The KL divergence of the update.
	 */
	public double updateElement(T element, double numSamples, double count,
			double stepSize) {
		double oldValue = itemProbs_.get(element);
		// Calculate the new ratio.
		double ratio = count / numSamples;
		// Update the value
		double newValue = stepSize * ratio + (1 - stepSize) * oldValue;
		// Set the new value.
		itemProbs_.put(element, newValue);

		return klDivergence(oldValue, newValue);
	}

	/**
	 * Calculates the KL divergence between two probability values.
	 * 
	 * @param oldValue
	 *            The old value.
	 * @param newValue
	 *            The new value.
	 * @return The KL divergence between the values.
	 */
	private double klDivergence(double oldValue, double newValue) {
		if (newValue == 0)
			return 0;
		if (oldValue == 0)
			return Double.POSITIVE_INFINITY;

		double result = newValue * Math.log(newValue / oldValue);
		return result;
	}

	/**
	 * Clones this distribution. This is a shallow clone that does not clone the
	 * elements. Also, the random generator is not cloned.
	 * 
	 * @return A clone of this distribution (but not the elements contained
	 *         within).
	 */
	@Override
	public ProbabilityDistribution<T> clone() {
		ProbabilityDistribution<T> clone = new ProbabilityDistribution<T>(
				random_);
		clone.itemProbs_ = new MutableKeyMap<T, Double>(itemProbs_);
		return clone;
	}

	@Override
	public int size() {
		return itemProbs_.size();
	}

	/**
	 * Resets the probabilities to an equal distribution.
	 */
	public void resetProbs() {
		// Set all probabilities to one.
		for (T element : itemProbs_.keySet()) {
			itemProbs_.put(element, 1.0 / itemProbs_.size());
		}
	}

	/**
	 * Resets the probabilities to a given value.
	 */
	public void resetProbs(double prob) {
		// Set all probabilities to a given value.
		for (T element : itemProbs_.keySet()) {
			itemProbs_.put(element, prob);
		}
	}

	@Override
	public void clear() {
		itemProbs_.clear();
	}

	@Override
	public boolean contains(Object arg0) {
		return itemProbs_.containsKey(arg0);
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		return itemProbs_.keySet().containsAll(arg0);
	}

	@Override
	public boolean isEmpty() {
		return itemProbs_.isEmpty();
	}

	@Override
	public Iterator<T> iterator() {
		return itemProbs_.keySet().iterator();
	}

	@Override
	public boolean remove(Object arg0) {
		Double val = itemProbs_.remove(arg0);
		if (val != null)
			return true;
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		boolean changed = false;
		for (Object obj : arg0) {
			changed |= remove(obj);
		}
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		if (collection == null)
			throw new NullPointerException();

		int size = itemProbs_.size();
		Set<T> elementSet = new HashSet<T>(itemProbs_.keySet());
		for (T element : elementSet) {
			if (!collection.contains(element))
				remove(element);
		}

		// If the sizes haven't changed, return false.
		if (size == itemProbs_.size())
			return false;

		normaliseProbs();
		return true;
	}

	@Override
	public Object[] toArray() {
		return null;
	}

	@Override
	@SuppressWarnings("hiding")
	public <T> T[] toArray(T[] arg0) {
		return null;
	}

	@Override
	public String toString() {
		ArrayList<T> ordered = getOrderedElements();
		StringBuffer buffer = new StringBuffer("{");
		boolean first = true;
		for (T element : ordered) {
			if (!first)
				buffer.append(", ");
			buffer.append("(" + element + ":" + itemProbs_.get(element) + ")");
			first = false;
		}
		buffer.append("}");
		return buffer.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 0;
		for (T element : itemProbs_.keySet())
			result += element.hashCode();
		result = prime * result + 1;
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
		ProbabilityDistribution<T> other = (ProbabilityDistribution<T>) obj;
		if (itemProbs_ == null) {
			if (other.itemProbs_ != null)
				return false;
		} else if (!itemProbs_.keySet().equals(other.itemProbs_.keySet()))
			return false;
		return true;
	}

	/**
	 * A class which compares 2 items based on their probability found within
	 * the distribution, where a larger probability comes first.
	 * 
	 * @author Sam Sarjant
	 * 
	 * @param <S>
	 *            The item type being compared.
	 */
	private class ProbabilityComparator<S extends T> implements Comparator<S> {
		@Override
		public int compare(S o1, S o2) {
			// Compares by element probability within the item probs
			if (getProb(o1) > getProb(o2))
				return -1;
			if (getProb(o1) < getProb(o2))
				return 1;
			return Double.compare(o1.hashCode(), o2.hashCode());
		}

	}
}
