package rlPacMan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

/**
 * A class representing a probability distribution of values. These values are
 * sampled randomly according to their probabilities.
 * 
 * @author Samuel J. Sarjant
 * 
 */
@SuppressWarnings("serial")
public class ProbabilityDistribution<T> implements Collection<T> {

	/** Instance storage values. */
	private ArrayList<T> values_;
	private ArrayList<Double> probabilities_;
	private Random random_;

	/**
	 * A constructor for the probability distribution.
	 * 
	 * @param popSize
	 *            The population size for the generator
	 */
	public ProbabilityDistribution() {
		probabilities_ = new ArrayList<Double>();
		random_ = new Random();
		values_ = new ArrayList<T>();
		probabilities_ = new ArrayList<Double>();
	}

	/**
	 * Samples a random weighted element from the distribution. This assumes all
	 * probabilities sum to 1.
	 * 
	 * @return The element sampled, according to weight, or null.
	 */
	public T sample() {
		if (values_.size() == 0)
			return null;
		double val = random_.nextDouble();
		double tally = 0;
		Iterator<Double> iter = probabilities_.iterator();
		Iterator<T> elementIter = values_.iterator();
		do {
			tally += iter.next();
			T currElement = elementIter.next();
			if (val < tally)
				return currElement;
		} while ((tally < 1) && (iter.hasNext()));
		return null;
	}

	/**
	 * Samples an element from this distribution at an index with probability
	 * p_index. Otherwise returns null.
	 * 
	 * @param index
	 *            The index ofd the element being sampled.
	 * @return The element with probability p_index, else returns null.
	 */
	public T bernoulliSample(int index) {
		if (random_.nextDouble() < probabilities_.get(index))
			return values_.get(index);
		return null;
	}

	/**
	 * Adds an element with a specified probability.
	 * 
	 * @param arg0
	 *            The element being added.
	 * @param prob
	 *            The initial probability of the element.
	 * @return True if the collection was modified.
	 */
	public boolean add(T arg0, double prob) {
		if (!values_.contains(arg0)) {
			values_.add(arg0);
			probabilities_.add(prob);
			return true;
		}
		return false;
	}

	/**
	 * Gets the probability for this element.
	 * 
	 * @param element
	 *            The element.
	 * @return The probability for the element, or -1 if it doesn't exist.
	 */
	public double getProb(T element) {
		if (values_.contains(element))
			return probabilities_.get(values_.indexOf(element));
		return -1;
	}

	/**
	 * Gets the element at an index.
	 * 
	 * @param index
	 *            The index of the element.
	 * @return The element or null if out-of-bounds.
	 */
	public T getElement(int index) {
		if (index >= values_.size())
			return null;
		return values_.get(index);
	}

	/**
	 * Sets the probability of an existing element to a new probability. This
	 * may affect the 'sums-to-one' criteria.
	 * 
	 * @param element
	 *            The element being set.
	 * @param newProb
	 *            The new probability of the value.
	 * @return True if this contains the element, false otherwise.
	 */
	public boolean set(T element, double newProb) {
		if (!values_.contains(element))
			return false;

		probabilities_.set(values_.indexOf(element), newProb);
		return true;
	}

	/**
	 * Adds all elements from a collection, with an initial probability of 1/K,
	 * where K is the size of the collection.
	 * 
	 * @param arg0
	 *            The collection being added.
	 * @return True if the collection was modified.
	 */
	public boolean addAll(Collection<? extends T> arg0) {
		if ((arg0 == null) || (arg0.size() == 0))
			return false;
		boolean result = false;
		double prob = 1.0 / arg0.size();
		for (T item : arg0) {
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
		for (double prob : probabilities_) {
			sum += prob;
		}
		if (sum == 1)
			return true;
		return false;
	}

	/**
	 * Normalises the probabilities to sum to one.
	 */
	public void normaliseProbs() {
		// Get total
		double sum = 0;
		for (double prob : probabilities_) {
			sum += prob;
		}

		if (sum == 0)
			return;

		// Normalise
		for (int i = 0; i < probabilities_.size(); i++)
			probabilities_.set(i, probabilities_.get(i) / sum);
	}

	@Override
	public int size() {
		return values_.size();
	}

	@Override
	public boolean add(T arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean contains(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterator<T> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean remove(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T[] toArray(T[] arg0) {
		// TODO Auto-generated method stub
		return null;
	}
}
