package rlPacMan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
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
	private ArrayList<ItemProb> itemProbs_;
	/** The random number generator. */
	private Random random_;

	/**
	 * A constructor for the probability distribution.
	 * 
	 * @param popSize
	 *            The population size for the generator
	 */
	public ProbabilityDistribution() {
		random_ = new Random();
		itemProbs_ = new ArrayList<ItemProb>();
	}

	/**
	 * Samples a random weighted element from the distribution. This assumes all
	 * probabilities sum to 1.
	 * 
	 * @return The element sampled, according to weight, or null.
	 */
	public T sample() {
		if (itemProbs_.size() == 0)
			return null;
		double val = random_.nextDouble();
		double tally = 0;
		Iterator<ItemProb> iter = itemProbs_.iterator();
		do {
			ItemProb current = iter.next();
			tally += current.getProbability();
			if (val < tally)
				return current.getItem();
		} while ((tally < 1) && (iter.hasNext()));
		return null;
	}

	/**
	 * Samples an element from this distribution at an index with probability
	 * p_index. Otherwise returns null.
	 * 
	 * @param index
	 *            The index of the element being sampled.
	 * @return The element with probability p_index, else returns null.
	 */
	public T bernoulliSample(int index) {
		if (random_.nextDouble() < itemProbs_.get(index).getProbability())
			return itemProbs_.get(index).getItem();
		return null;
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
		itemProbs_.add(new ItemProb(element, 1));
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
		itemProbs_.add(new ItemProb(element, prob));
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
		for (ItemProb ip : itemProbs_) {
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
		for (ItemProb ip : itemProbs_) {
			sum += ip.getProbability();
		}

		if (sum == 0)
			return;

		// Normalise
		for (ItemProb ip : itemProbs_)
			ip.setProbability(ip.getProbability() / sum);
	}

	/**
	 * Gets the N best items from this distribution, as determined by their
	 * weight. The distribution must be no smaller than N (preferable 3N, in
	 * conjunction with the other method).
	 * 
	 * @param size
	 *            The number of items to get.
	 * @return A list of the best items in the distribution.
	 */
	public ArrayList<T> getNBest(int size) {
		// Sort the values
		ArrayList<ItemProb> ips = new ArrayList<ItemProb>(itemProbs_);
		Collections.sort(ips);

		ArrayList<T> elites = new ArrayList<T>();
		Iterator<ItemProb> iter = ips.iterator();
		// Skip through the first X-N items in the list.
		int i = 0;
		for (; i < ips.size() - size; i++) {
			iter.next();
		}
		// Getting the highest N values.
		for (; i < ips.size(); i++) {
			elites.add(iter.next().getItem());
		}
		return elites;
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
		ArrayList<ItemProb> ips = new ArrayList<ItemProb>(itemProbs_);
		Collections.sort(ips);

		ArrayList<ItemProb> removables = new ArrayList<ItemProb>();
		Iterator<ItemProb> iter = ips.iterator();
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
		for (ItemProb ip : removables) {
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
	 * @param offsetIndex
	 *            The starting index of the counts.
	 * @param stepSize
	 *            The step size for the update.
	 * @param valueModifier
	 *            The value modifier for the update.
	 */
	public void updateDistribution(double numSamples, int[] counts,
			int offsetIndex, double stepSize, double valueModifier) {
		if (numSamples != 0) {
			// For each of the rules within the distribution
			for (int i = 0; i < itemProbs_.size(); i++) {
				// Update every element within the distribution
				updateElement(numSamples, counts[i + offsetIndex], stepSize,
						valueModifier, i);
			}
			
			// Normalise the probabilities
			if (!sumsToOne())
				normaliseProbs();
		}
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
	 * @param valueModifier
	 *            The value modifier for the update.
	 * @param index
	 *            The index of the element to update.
	 */
	public void updateElement(double numSamples, int count, double stepSize,
			double valueModifier, int index) {
		// Calculate the new ratio.
		double ratio = count / numSamples;
		// Update the value
		double newValue = stepSize * ratio + (1 - stepSize) * getProb(index);
		// Set the new value multiplied by the modifier.
		set(index, newValue * valueModifier);
	}

	/**
	 * Clones this distribution. This is a shallow clone that does not clone the
	 * elements. Also, the random generator is not cloned.
	 * 
	 * @return A clone of this distribution (but not the elements contained
	 *         within).
	 */
	public ProbabilityDistribution<T> clone() {
		ProbabilityDistribution<T> clone = new ProbabilityDistribution<T>();
		for (ItemProb ip : itemProbs_) {
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
		Iterator<ItemProb> iter = itemProbs_.iterator();
		while (iter.hasNext()) {
			if (iter.next().element_.equals(element))
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
		for (ItemProb ip : itemProbs_) {
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
		for (ItemProb ip : itemProbs_) {
			ip.setProbability(prob);
		}
	}

	// @Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	// @Override
	public boolean contains(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	// @Override
	public boolean containsAll(Collection<?> arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	// @Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	// @Override
	public Iterator<T> iterator() {
		ArrayList<T> items = new ArrayList<T>(itemProbs_.size());
		for (ItemProb ip : itemProbs_) {
			items.add(ip.getItem());
		}
		return items.iterator();
	}

	// @Override
	public boolean remove(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	// @Override
	public boolean removeAll(Collection<?> arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	// @Override
	public boolean retainAll(Collection<?> arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	// @Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	// @Override
	@SuppressWarnings("hiding")
	public <T> T[] toArray(T[] arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Returns a string version of this distribution.
	 * 
	 * @return The string version of this probability distribution.
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		Iterator<ItemProb> iter = itemProbs_.iterator();
		if (iter.hasNext())
			buffer.append("[" + iter.next());
		while (iter.hasNext()) {
			buffer.append(", " + iter.next());
		}
		buffer.append("]");
		return buffer.toString();
	}

	/**
	 * A class for storing the items and associated probabilities.
	 */
	private class ItemProb implements Comparable<ItemProb> {
		/** The element stored. */
		private T element_;
		/** The probability, or weight. */
		private double prob_;

		/**
		 * A constructor for an item-probability pair.
		 * 
		 * @param element
		 *            The element.
		 * @param prob
		 *            The probability.
		 */
		public ItemProb(T element, double prob) {
			element_ = element;
			prob_ = prob;
		}

		/**
		 * Sets the probability of this ItemProb.
		 * 
		 * @param newProb
		 *            The new probability.
		 */
		public void setProbability(double newProb) {
			prob_ = newProb;
		}

		/**
		 * Gets the probability of this ItemProb.
		 * 
		 * @return The probability of the ItemProb.
		 */
		public double getProbability() {
			return prob_;
		}

		/**
		 * Gets the item of this ItemProb.
		 * 
		 * @return The item of the ItemProb.
		 */
		public T getItem() {
			return element_;
		}

		// @Override
		public int compareTo(ItemProb o) {
			if (prob_ < o.prob_) {
				return -1;
			} else if (prob_ > o.prob_) {
				return 1;
			} else {
				return Float
						.compare(element_.hashCode(), o.element_.hashCode());
			}
		}

		@Override
		public boolean equals(Object obj) {
			if ((obj == null)
					|| (!(obj instanceof ProbabilityDistribution.ItemProb)))
				return false;
			ItemProb ip = (ItemProb) obj;
			if (ip.element_.equals(element_)) {
				if (ip.prob_ == prob_) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int hashCode() {
			return (int) (element_.hashCode() * prob_);
		}

		@Override
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("(" + element_ + ": " + prob_ + ")");
			return buffer.toString();
		}
	}
}
