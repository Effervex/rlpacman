/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/util/ProbabilityDistribution.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
	private static final long serialVersionUID = 297675144227949310L;
	public static final int MAX_RULES_STRING = 5;
	/** If probabilities are smaller than this, the probability is 0. */
	public static final double MIN_PROB = 1E-15;
	/** The instances in the distribution with associated weights. */
	private Map<T, Double> itemProbs_;

	/** A summed probability array for searching. */
	private transient double[] probArray_;
	/** If the prob array should be recalculated. */
	private transient boolean rebuildProbs_;
	/** The elements of the distribution in an array. */
	private transient T[] elementArray_;

	/** The KL size of this distribution. */
	private double klSize_;

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
	 * Builds the probability tree by summing probabilities and storing them in
	 * sorted order. It is assumed that the probabilities sum to one.
	 */
	private void buildProbTree() {
		if (probArray_ == null)
			probArray_ = new double[elementArray_.length];
		// Iterate through the items
		double sumProb = 0;
		for (int i = 0; i < elementArray_.length; i++) {
			if (sumProb >= 1)
				break;

			sumProb += itemProbs_.get(elementArray_[i]);
			probArray_[i] = sumProb;
		}
		rebuildProbs_ = false;
	}

	/**
	 * Calculates the KL divergence between two probability values.
	 * 
	 * @param newValue
	 *            The new value.
	 * @param oldValue
	 *            The old value.
	 * 
	 * @return The KL divergence between the values.
	 */
	private double klDivergence(double newValue, double oldValue) {
		if (newValue == 0)
			return 0;
		if (oldValue == 0)
			return Double.POSITIVE_INFINITY;

		double result = newValue * Math.log(newValue / oldValue);
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
	@Override
	public boolean add(T element) {
		itemProbs_.put(element, 1d);
		elementArray_ = null;
		klSize_ = 0;
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
		elementArray_ = null;
		klSize_ = 0;
		return true;
	}

	/**
	 * Adds all elements from a collection, with an initial probability of 1/K,
	 * where K is the size of the collection.
	 * 
	 * @param elements
	 *            The collection being added.
	 * @return True if the collection was modified.
	 */
	@Override
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
	 * Adds all arguments not already in the distribution.
	 * 
	 * @param arg0
	 *            The collection of elements being added.
	 * @param prob
	 *            The probability to set the slots.
	 */
	public boolean addContainsAll(Collection<? extends T> arg0, double prob) {
		boolean val = false;
		for (T t : arg0) {
			if (!contains(t))
				val |= add(t, prob);
		}
		return val;
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

	@Override
	public void clear() {
		itemProbs_.clear();
		elementArray_ = null;
		klSize_ = 0;
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
	public boolean contains(Object arg0) {
		return itemProbs_.containsKey(arg0);
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		return itemProbs_.keySet().containsAll(arg0);
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
	 * Gets the best element from this distribution. If two or more elements
	 * have equal probability, this will return one of them.
	 * 
	 * @return The best element (highest probability).
	 */
	public T getBestElement() {
		T bestElement = null;
		double bestProb = -1;
		for (T element : itemProbs_.keySet()) {
			double thisProb = itemProbs_.get(element);
			if (thisProb > bestProb) {
				bestProb = thisProb;
				bestElement = element;
			}
		}
		return bestElement;
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
	 * Gets the probability for an element if it is present.
	 * 
	 * @param element
	 *            The element with a probability.
	 * @return The probability of the rule, or -1 if it isn't present.
	 */
	public Double getProb(T element) {
		return itemProbs_.get(element);
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

	@Override
	public boolean isEmpty() {
		return itemProbs_.isEmpty();
	}

	@Override
	public Iterator<T> iterator() {
		return itemProbs_.keySet().iterator();
	}

	/**
	 * Returns the KL divergence of the distribution from a uniform distribution
	 * with the values representing the number of useful items in the
	 * distribution.
	 * 
	 * @return The KL size of the distribution. Max = size(), Min = 1 (or 0 if
	 *         empty).
	 */
	public double klSize() {
		if (klSize_ == 0) {
			double klSum = 0;
			int size = size();
			if (size <= 1)
				return 0;
			double uniform = 1.0 / size;
			for (Double prob : itemProbs_.values()) {
				klSum += klDivergence(prob, uniform);
			}
			double logBase = Math.log(size);
			klSize_ = Math.max(size * (1 - klSum / logBase), 1);
		}
		return klSize_;
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
		rebuildProbs_ = true;
		klSize_ = 0;
	}

	@Override
	public boolean remove(Object arg0) {
		Double val = itemProbs_.remove(arg0);
		if (val != null) {
			elementArray_ = null;
			klSize_ = 0;
			return true;
		}
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

	/**
	 * Resets the probabilities to an equal distribution.
	 */
	public void resetProbs() {
		// Set all probabilities to one.
		for (T element : itemProbs_.keySet()) {
			itemProbs_.put(element, 1.0 / itemProbs_.size());
		}
		rebuildProbs_ = true;
		klSize_ = 0;
	}

	/**
	 * Resets the probabilities to a given value.
	 */
	public void resetProbs(double prob) {
		// Set all probabilities to a given value.
		for (T element : itemProbs_.keySet()) {
			itemProbs_.put(element, prob);
		}
		rebuildProbs_ = true;
		klSize_ = 0;
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

	/**
	 * Samples a random weighted element from the distribution. This assumes all
	 * probabilities sum to 1.
	 * 
	 * @param useMostLikely
	 *            If we sample the most likely element.
	 * @return The element sampled, according to weight, or null.
	 */
	@SuppressWarnings("unchecked")
	public T sample(boolean useMostLikely) {
		if (itemProbs_.isEmpty())
			return null;

		// If using most likely, just get the highest prob one
		if (useMostLikely)
			return getOrderedElements().get(0);

		if (elementArray_ == null) {
			probArray_ = null;
			// Need to use flexible element array which ignores extremely low
			// probability items
			ArrayList<T> elementArray = new ArrayList<T>();
			for (T element : itemProbs_.keySet()) {
				if (itemProbs_.get(element) > MIN_PROB)
					elementArray.add(element);
			}
			elementArray_ = (T[]) new Object[elementArray.size()];
			elementArray_ = elementArray.toArray(elementArray_);
		}
		if (rebuildProbs_ || probArray_ == null)
			buildProbTree();

		double val = random_.nextDouble();
		int index = Arrays.binarySearch(probArray_, val);
		if (index < 0)
			index = Math.min(-index - 1, elementArray_.length - 1);

		return elementArray_[index];
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
			rebuildProbs_ = true;
			klSize_ = 0;
			return true;
		}
		return false;
	}

	@Override
	public int size() {
		return itemProbs_.size();
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
		int count = 0;
		double sum = 0;
		for (T element : ordered) {
			if (count >= MAX_RULES_STRING) {
				int remaining = ordered.size() - MAX_RULES_STRING;
				double remSum = (1 - sum);
				buffer.append(" + " + remaining + " OTHERS:~" + remSum
						/ remaining + " EACH");
				break;
			}
			if (!first)
				buffer.append(", ");
			double itemProb = itemProbs_.get(element);
			buffer.append("(" + element + ":" + itemProb + ")");
			sum += itemProb;
			first = false;
			count++;
		}
		buffer.append("}");
		return buffer.toString();
	}

	public String toString(boolean seperateElements) {
		if (!seperateElements)
			return toString();
		ArrayList<T> ordered = getOrderedElements();
		StringBuffer buffer = new StringBuffer("{");
		boolean first = true;
		for (T element : ordered) {
			if (!first)
				buffer.append(",\n");
			buffer.append("(" + element + ":" + itemProbs_.get(element) + ")");
			first = false;
		}
		buffer.append("}");
		return buffer.toString();
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
	public double updateDistribution(double numSamples, Map<T, Integer> counts,
			double stepSize) {
		if (numSamples != 0) {
			// For each of the rules within the distribution
			double absDiff = 0;
			for (T element : itemProbs_.keySet()) {
				// Update every element within the distribution
				Integer itemCount = counts.get(element);
				if (itemCount == null)
					itemCount = 0;
				absDiff += updateElement(element, numSamples, itemCount,
						stepSize);
			}

			// Normalise the probabilities
			normaliseProbs();
			absDiff /= (2 * stepSize);
			return absDiff;
		}

		return 0;
	}

	/**
	 * Updates the probability distribution using a constant step size.
	 * 
	 * @param observedDistribution
	 *            The observed distribution to step towards.
	 * @param stepSize
	 *            The constant step size.
	 * @return The absolute amount of difference in the probabilities for each
	 *         element.
	 */
	public double updateDistribution(
			ProbabilityDistribution<T> observedDistribution, double stepSize) {
		double absoluteChange = 0;
		for (T element : itemProbs_.keySet()) {
			// Update every element within the distribution
			Double ratio = observedDistribution.getProb(element);
			if (ratio == null)
				ratio = 0d;
			absoluteChange += updateElement(element, 1, ratio, stepSize);
		}

		// Normalise the probabilities
		normaliseProbs();

		return absoluteChange;
	}

	/**
	 * Updates the probability distribution using given step sizes.
	 * 
	 * @param observedDistribution
	 *            The observed distribution to step towards.
	 * @param stepSizes
	 *            The step sizes for each individual element.
	 * @return The absolute amount of difference in the probabilities for each
	 *         element.
	 */
	public double updateDistribution(
			ProbabilityDistribution<T> observedDistribution,
			Map<T, Double> stepSizes) {
		double absoluteChange = 0;
		for (T element : itemProbs_.keySet()) {
			// Update every element within the distribution
			Double ratio = observedDistribution.getProb(element);
			if (ratio == null)
				ratio = 0d;
			absoluteChange += updateElement(element, 1, ratio,
					stepSizes.get(element));
		}

		// Normalise the probabilities
		normaliseProbs();

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
	 * @return The absolute difference of the update.
	 */
	public double updateElement(T element, double numSamples, double count,
			double stepSize) {
		double oldValue = itemProbs_.get(element);
		// Calculate the new ratio.
		double observedProb = Math.min(count / numSamples, 1);
		// Update the value
		double newValue = stepSize * observedProb + (1 - stepSize) * oldValue;
		if (newValue <= MIN_PROB) {
			if (oldValue != 0)
				elementArray_ = null;
			newValue = 0;
		}
		if (newValue >= 1 - MIN_PROB)
			newValue = 1;
		// Set the new value.
		itemProbs_.put(element, newValue);
		rebuildProbs_ |= oldValue != newValue;
		klSize_ = 0;

		// TODO Note the '2' coefficient. The maximum (normalised) divergence
		// for a distribution approaches the limit of 2.
		return Math.abs(newValue - oldValue);
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
