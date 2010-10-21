package relationalFramework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * A class representing a probability distribution of values. These values are
 * sampled randomly according to their probabilities.
 * 
 * @author Samuel J. Sarjant
 * 
 */
public class ProbabilityDistribution<T> implements Collection<T> {
	/** The instances in the distribution with associated weights. */
	private ArrayList<ItemProb<T>> itemProbs_;
	/** The random number generator. */
	protected Random random_;

	/**
	 * A constructor for the probability distribution.
	 */
	public ProbabilityDistribution() {
		random_ = new Random();
		itemProbs_ = new ArrayList<ItemProb<T>>();
	}

	/**
	 * A constructor for the probability distribution.
	 * 
	 * @param random
	 *            A given random value.
	 */
	public ProbabilityDistribution(Random random) {
		random_ = random;
		itemProbs_ = new ArrayList<ItemProb<T>>();
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
		Iterator<ItemProb<T>> iter = itemProbs_.iterator();
		do {
			ItemProb<T> current = iter.next();
			tally += current.getProbability();
			if (val < tally)
				return current.getItem();
		} while ((tally < 1) && (iter.hasNext()));
		return itemProbs_.get(0).getItem();
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
		itemProbs_.add(new ItemProb<T>(element, 1));
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
		itemProbs_.add(new ItemProb<T>(element, prob));
		return true;
	}

	/**
	 * Gets the probability for the element at an index.
	 * 
	 * @param index
	 *            The index of the element.
	 * @return The probability for the element, or -1 if it doesn't exist.
	 */
	public double getProb(int index) {
		return itemProbs_.get(index).getProbability();
	}

	/**
	 * Gets the probability for an element if it is present.
	 * 
	 * @param element The element with a probability.
	 * @return The probability of the rule, or -1 if it isn't present.
	 */
	public double getProb(T element) {
		int index = indexOf(element);
		if (index == -1)
			return -1;
		return getProb(index);
	}

	/**
	 * Gets the element at an index.
	 * 
	 * @param index
	 *            The index of the element.
	 * @return The element or null if out-of-bounds.
	 */
	public T getElement(int index) {
		if (index >= itemProbs_.size())
			return null;
		return itemProbs_.get(index).getItem();
	}

	/**
	 * Sets the probability of an element at an index to a new probability. This
	 * may affect the 'sums-to-one' criteria.
	 * 
	 * @param index
	 *            The index of the element being set.
	 * @param newProb
	 *            The new probability of the value.
	 * @return True if this contains the element, false otherwise.
	 */
	public boolean set(int index, double newProb) {
		itemProbs_.get(index).setProbability(newProb);
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
		for (ItemProb<T> ip : itemProbs_) {
			sum += ip.getProbability();
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
		int count = 0;
		for (ItemProb<T> ip : itemProbs_) {
			sum += ip.getProbability();
			count++;
		}

		// Normalise
		for (ItemProb<T> ip : itemProbs_) {
			// If the sum is 0, everything is equal.
			if (sum == 0)
				ip.setProbability(1.0 / count);
			else
				ip.setProbability(ip.getProbability() / sum);
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
		for (int i = 0; i < size(); i++) {
			if (getProb(i) < average) {
				clone.set(i, 0);
			} else if (binary) {
				clone.set(i, 1);
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
		ArrayList<ItemProb<T>> ips = new ArrayList<ItemProb<T>>(itemProbs_);
		Collections.sort(ips, Collections.reverseOrder());

		ArrayList<T> ordered = new ArrayList<T>();
		for (ItemProb<T> ip : ips) {
			ordered.add(ip.getItem());
		}
		return ordered;
	}

	/**
	 * Gets the elements of this distribution in an ordered list, from most
	 * likely to least, excluding zero-probability elements.
	 * 
	 * @return The non-zero elements of the distribution in order.
	 */
	public ArrayList<T> getNonZeroOrderedElements() {
		ArrayList<ItemProb<T>> ips = new ArrayList<ItemProb<T>>(itemProbs_);
		Collections.sort(ips, Collections.reverseOrder());

		ArrayList<T> ordered = new ArrayList<T>();
		for (ItemProb<T> ip : ips) {
			if (ip.getProbability() == 0)
				return ordered;
			ordered.add(ip.getItem());
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
		for (ItemProb<T> ip : itemProbs_) {
			if (ip.getProbability() > 0) {
				nonZeroes.add(ip.getItem());
			}
		}
		return nonZeroes;
	}

	/**
	 * Removes the N worst items from this distribution and returns the average
	 * weight of the top 2N items. Thus, the distribution must be no smaller
	 * than 3N.
	 * 
	 * @param size
	 *            The number of items to remove and half of the number of top
	 *            items to use in the average weight.
	 * @return The average value of the top 2N items.
	 */
	public double removeNWorst(int size) {
		if (itemProbs_.size() < (3 * size))
			return -1;
		// Sort the values
		ArrayList<ItemProb<T>> ips = new ArrayList<ItemProb<T>>(itemProbs_);
		Collections.sort(ips);

		ArrayList<ItemProb<T>> removables = new ArrayList<ItemProb<T>>();
		Iterator<ItemProb<T>> iter = ips.iterator();
		int i = 0;
		// Note the first N items - they will be removed
		for (; i < size; i++) {
			removables.add(iter.next());
		}
		// Skip to the last X-2N items
		for (; i < ips.size() - (2 * size); i++) {
			iter.next();
		}
		// Note the values of the last 2N items and average them
		double sum = 0;
		for (; i < ips.size(); i++) {
			sum += iter.next().getProbability();
		}
		sum /= (2 * size);

		// Remove the rules (one at a time to avoid removing duplicates)
		for (ItemProb<T> ip : removables) {
			itemProbs_.remove(ip);
		}
		return sum;
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
			for (ItemProb<T> ip : itemProbs_) {
				// Update every element within the distribution
				Double itemCount = counts.get(ip.getItem());
				if (itemCount == null)
					itemCount = 0d;
				absoluteChange += Math.abs(updateElement(ip, numSamples,
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
	 * @param numSamples
	 *            The number of samples used for the counts.
	 * @param count
	 *            The count for this element.
	 * @param stepSize
	 *            The step size for the update.
	 * @return The KL divergence of the update.
	 */
	public double updateElement(ItemProb<T> element, double numSamples,
			double count, double stepSize) {
		double oldValue = element.getProbability();
		// Calculate the new ratio.
		double ratio = count / numSamples;
		// Update the value
		double newValue = stepSize * ratio + (1 - stepSize) * oldValue;
		// Set the new value.
		element.setProbability(newValue);

		return klDivergence(oldValue, newValue);
	}

	/**
	 * Calculates the KL divergence between two probability values.
	 * 
	 * @param oldValue The old value.
	 * @param newValue The new value.
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
	 * Forms the probabilities of this generator into a string.
	 * 
	 * @param delimiter
	 *            The delimiter to place between elements.
	 * @return A string detailing the distribution probabilities.
	 */
	public String generatorString(String delimiter) {
		StringBuffer strBuffer = new StringBuffer();
		for (ItemProb<T> ip : itemProbs_) {
			strBuffer.append(ip.getProbability() + delimiter);
		}

		return strBuffer.toString();
	}

	/**
	 * Forms the probabilities of this generator into a string.
	 * 
	 * @param delimiter
	 *            The delimiter to place between elements.
	 * @return A string detailing the distribution probabilities.
	 */
	public void parseGeneratorString(String input, String delimiter) {
		String[] split = input.split(delimiter);
		Iterator<ItemProb<T>> iter = itemProbs_.iterator();
		for (int i = 0; i < split.length; i++) {
			iter.next().setProbability(Double.parseDouble(split[i]));
		}
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
		for (ItemProb<T> ip : itemProbs_) {
			clone.add(ip.getItem(), ip.getProbability());
		}
		return clone;
	}

	// @Override
	public int size() {
		return itemProbs_.size();
	}

	/**
	 * Gets the index of an element, or -1.
	 * 
	 * @param element
	 *            The element being searched for.
	 * @return The index of the element, or -1 if not present.
	 */
	public int indexOf(T element) {
		int i = 0;
		Iterator<ItemProb<T>> iter = itemProbs_.iterator();
		while (iter.hasNext()) {
			if (iter.next().getItem().equals(element))
				return i;
			i++;
		}
		return -1;
	}

	/**
	 * Resets the probabilities to an equal distribution.
	 */
	public void resetProbs() {
		// Set all probabilities to one.
		for (ItemProb<T> ip : itemProbs_) {
			ip.setProbability(1);
		}

		// Normalise the probs
		normaliseProbs();
	}

	/**
	 * Resets the probabilities to a given value.
	 */
	public void resetProbs(double prob) {
		// Set all probabilities to a given value.
		for (ItemProb<T> ip : itemProbs_) {
			ip.setProbability(prob);
		}
	}

	// @Override
	public void clear() {
		itemProbs_.clear();
	}

	// @Override
	public boolean contains(Object arg0) {
		if (arg0 == null)
			return false;
		T arg02 = (T) arg0;
		if (indexOf(arg02) == -1)
			return false;
		return true;
	}

	// @Override
	public boolean containsAll(Collection<?> arg0) {
		if (arg0 == null)
			return false;
		for (Object obj : arg0) {
			if (!contains(obj))
				return false;
		}
		return true;
	}

	// @Override
	public boolean isEmpty() {
		if (itemProbs_.isEmpty())
			return true;
		return false;
	}

	// @Override
	public Iterator<T> iterator() {
		ArrayList<T> items = new ArrayList<T>(itemProbs_.size());
		for (ItemProb<T> ip : itemProbs_) {
			items.add(ip.getItem());
		}
		return items.iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean remove(Object arg0) {
		if (arg0 != null) {
			int index = indexOf((T) arg0);
			if (index != -1) {
				itemProbs_.remove(index);
				return true;
			}
		}
		return false;
	}

	// @Override
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
		for (Iterator<ItemProb<T>> iter = itemProbs_.iterator(); iter.hasNext();) {
			ItemProb<T> element = iter.next();
			if (!collection.contains(element.getItem()))
				itemProbs_.remove(element);
		}

		// If the sizes haven't changed, return false.
		if (size == itemProbs_.size())
			return false;

		normaliseProbs();
		return true;
	}

	// @Override
	public Object[] toArray() {
		return null;
	}

	// @Override
	@SuppressWarnings("hiding")
	public <T> T[] toArray(T[] arg0) {
		return null;
	}

	@Override
	public String toString() {
		// TODO Output the string as a sorted list by probability
		StringBuffer buffer = new StringBuffer("[");
		Iterator<ItemProb<T>> iter = itemProbs_.iterator();
		if (iter.hasNext())
			buffer.append(iter.next());
		while (iter.hasNext()) {
			buffer.append(", " + iter.next());
		}
		buffer.append("]");
		return buffer.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((itemProbs_ == null) ? 0 : itemProbs_.hashCode());
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
		ProbabilityDistribution other = (ProbabilityDistribution) obj;
		if (itemProbs_ == null) {
			if (other.itemProbs_ != null)
				return false;
		} else if (!itemProbs_.equals(other.itemProbs_))
			return false;
		return true;
	}
}
