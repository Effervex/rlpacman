package relationalFramework;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.mandarax.kernel.InferenceEngine;
import org.mandarax.kernel.KnowledgeBase;
import org.mandarax.kernel.LogicFactory;
import org.mandarax.kernel.Predicate;
import org.mandarax.kernel.Prerequisite;
import org.mandarax.kernel.Query;
import org.mandarax.kernel.Term;
import org.mandarax.kernel.meta.JConstructor;

/**
 * A defined predicate contains the information needed to be able to generate a
 * random predicate using the finite set of terms within the predicate.
 * 
 * @author Sam Sarjant
 */
public class GuidedPredicate {
	/** The predicate defined. */
	private Predicate predicate_;

	/** The possible values the predicate can use. */
	private PredTerm[][] predValues_;

	/**
	 * A loose instantiation of the predicate, in that it can retain tied
	 * values.
	 */
	private PredTerm[] looseInstantiation_;

	/**
	 * A constructor for a GuidedPredicate. The PredTerms are the possible terms
	 * the predicate can take on.
	 * 
	 * @param predicate
	 *            The predicate being defined.
	 * @param predValues
	 *            The values that can be used within the predicate.
	 */
	public GuidedPredicate(Predicate predicate, PredTerm[][] predValues) {
		predicate_ = predicate;
		predValues_ = predValues;
		looseInstantiation_ = null;
	}

	/**
	 * A constructor for a loosely instantiated predicate. The only difference
	 * is that tied terms are yet to be tied to another term.
	 * 
	 * @param predicate
	 *            The predicate being defined.
	 * @param looseTerms
	 *            The guiding values for the predicate.
	 */
	public GuidedPredicate(Predicate predicate, PredTerm[] looseTerms) {
		predicate_ = predicate;
		predValues_ = null;
		looseInstantiation_ = looseTerms;
	}

	/**
	 * A constructor for the empty guided predicate.
	 */
	public GuidedPredicate() {
		predicate_ = null;
		predValues_ = null;
		looseInstantiation_ = null;
	}

	/**
	 * A method for creating all possible loose instantiations (tied free
	 * variables are not yet tied) of this defined predicate, taking into
	 * account the constants present in the goal and the background knowledge.
	 * 
	 * @param backgroundKnowledge
	 *            The background knowledge of the environment, for preventing
	 *            illegal or multiple instantiations.
	 * @param factory
	 *            The logic factory in use.
	 * @param ie
	 *            The inference engine in use.
	 * @return A collection of all possible loose instantiations.
	 */
	public Collection<GuidedPredicate> createAllLooseInstantiations(
			KnowledgeBase backgroundKnowledge, LogicFactory factory,
			InferenceEngine ie) {
		Collection<GuidedPredicate> looseInstantiations = new LinkedList<GuidedPredicate>();
		// For the empty pred, return itself
		if (predicate_ == null) {
			looseInstantiations.add(this);
			return looseInstantiations;
		}

		// For a pred without args, return the pred
		if (predValues_.length == 0) {
			looseInstantiations.add(new GuidedPredicate(predicate_,
					new PredTerm[0]));
			return looseInstantiations;
		}

		// Otherwise recurse through and find all instantiations.
		recurseInstantiations(0, 0, new PredTerm[predValues_.length],
				looseInstantiations, backgroundKnowledge, factory, ie);
		return looseInstantiations;
	}

	/**
	 * Recurses through a predicate's PredTerms, creating every (legal)
	 * instantiation possible. These can also include constants from the goal.
	 * The legality of an instantiation is given by the background knowledge.
	 * 
	 * @param argument
	 *            The current argument position.
	 * @param termNum
	 *            The current term number.
	 * @param predTerms
	 *            The current predicate values.
	 * @param looseInstantiations
	 *            The instantiation list to be filled.
	 * @param backgroundKnowledge
	 *            The background knowledge to prevent illegal and redundant
	 *            instantiations.
	 * @param factory
	 *            The logic factory in use.
	 * @param ie
	 *            The inference engine in use.
	 */
	private void recurseInstantiations(int argument, int termNum,
			PredTerm[] predTerms,
			Collection<GuidedPredicate> looseInstantiations,
			KnowledgeBase backgroundKnowledge, LogicFactory factory,
			InferenceEngine ie) {
		// Insert a term into the predVals.
		predTerms[argument] = predValues_[argument][termNum];

		// BASE CASE: Check if the predTerms are full.
		if (predTerms[predTerms.length - 1] != null) {
			PredTerm[] clone = new PredTerm[predTerms.length];
			for (int i = 0; i < predTerms.length; i++) {
				clone[i] = predTerms[i];
			}
			// If so, add to the collection and undo the addition
			GuidedPredicate loosePred = new GuidedPredicate(predicate_, clone);
			// Check that the predicate doesn't clash with the background
			// knowledge
			if (isPredicateValid(loosePred, backgroundKnowledge, factory, ie)) {
				looseInstantiations.add(loosePred);
			}
			predTerms[argument] = null;
		} else {
			// If not full, recurse deeper
			recurseInstantiations(argument + 1, 0, predTerms,
					looseInstantiations, backgroundKnowledge, factory, ie);
		}

		// Self-recurse along the termNums if there are still more to look at
		if (termNum < predValues_[argument].length - 1) {
			recurseInstantiations(argument, termNum + 1, predTerms,
					looseInstantiations, backgroundKnowledge, factory, ie);
		}
	}

	/**
	 * Checks if a formed predicate is valid against the background knowledge.
	 * 
	 * @param loosePred
	 *            The yet-to-be formed predicate.
	 * @param backgroundKnowledge
	 *            The background knowledge for the environment.
	 * @param factory
	 *            The LogicFactory in use
	 * @param ie
	 *            The inference engine in use.
	 * @return True if the predicate is valid, false if it is illegal or
	 *         redundant.
	 */
	private boolean isPredicateValid(GuidedPredicate loosePred,
			KnowledgeBase backgroundKnowledge, LogicFactory factory,
			InferenceEngine ie) {
		// Create the fact
		List<Prerequisite> instant = loosePred.factify(factory, null, false);
		Query query = factory.createQuery(instant
				.toArray(new Prerequisite[instant.size()]), "test");
		// This may not work... I may need Resolution
		try {
			ie.query(query, backgroundKnowledge, InferenceEngine.ONE,
					InferenceEngine.BUBBLE_EXCEPTIONS);
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	/**
	 * Instantiates this guided predicate into a proper fact/prerequisite. For
	 * now, there is no negation. Tied terms can be given as a parameter and the
	 * same parameter is used for output of terms used in the predicate.
	 * 
	 * @param factory
	 *            The logic factory in use.
	 * @param existingTerms
	 *            The variable terms to fill in the tied terms and the output
	 *            parameter.
	 * @return A instantiated version of this guided predicate with type
	 *         predicates, if it can be. Also fills the existing terms array
	 *         with the terms used.
	 */
	public List<Prerequisite> factify(LogicFactory factory,
			Term[] existingTerms, boolean mustTie) {
		List<Prerequisite> result = new LinkedList<Prerequisite>();
		if (looseInstantiation_ != null) {
			int offset = 0;
			if (predicate_ instanceof JConstructor)
				offset = 1;

			// Create a term array
			Class[] predStructure = predicate_.getStructure();
			Term[] terms = new Term[predStructure.length];
			for (int i = 0; i < terms.length; i++) {
				// Check for the instantiated class in the predicate
				if (i < offset) {
					terms[i] = factory.createConstantTerm(StateSpec
							.getInstance());
				} else {
					// Instantiate/find the regular terms
					PredTerm argTerm = looseInstantiation_[i - offset];
					// If we have a tied term
					if (argTerm.getTermType() == PredTerm.TIED) {
						// If we have a replacement, use that
						if ((existingTerms != null)
								&& (existingTerms[i - offset] != null)
								&& (predStructure[i]
										.isAssignableFrom(existingTerms[i
												- offset].getType()))) {
							terms[i] = existingTerms[i - offset];
						} else {
							// If we must tie
							if (mustTie) {
								// Left untied, so results in an invalid fact
								return null;
							} else {
								// Treat this as a free variable
								terms[i] = factory.createVariableTerm(
										(String) (argTerm.getValue()),
										predStructure[i]);
							}
						}
					} else if (argTerm.getTermType() == PredTerm.FREE) {
						// Create a free variable
						terms[i] = factory.createVariableTerm((String) (argTerm
								.getValue()), predStructure[i]);
					} else if (argTerm.getTermType() == PredTerm.VALUE) {
						// Create a constant variable
						terms[i] = factory.createConstantTerm(argTerm
								.getValue(), predStructure[i]);
					}

					// Adding the type predicates, if necessary
					Predicate typePred = StateSpec.getInstance()
							.getTypePredicate(predStructure[i]);
					if (typePred != null) {
						Term[] typeTerm = { terms[i] };
						Prerequisite typePrereq = factory.createPrerequisite(
								typePred, typeTerm, false);
						if (!result.contains(typePrereq))
							result.add(typePrereq);
					}

					// Adding to the return array
					if (existingTerms != null) {
						existingTerms[i - offset] = terms[i];
					}
				}
			}

			// Create the prereq
			result.add(factory.createPrerequisite(predicate_, terms, false));
		}
		return result;
	}

	public Predicate getPredicate() {
		return predicate_;
	}

	public PredTerm[][] getPredValues() {
		return predValues_;
	}

	public PredTerm[] getLooseInstantiation() {
		return looseInstantiation_;
	}

	public String toString() {
		if (predicate_ == null)
			return "true";
		return predicate_.toString();
	}

	/**
	 * Creates the empty observation.
	 * 
	 * @return The empty guided predicate.
	 */
	public static GuidedPredicate emptyObservation() {
		return new GuidedPredicate();
	}
}
