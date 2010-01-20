package relationalFramework;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.mandarax.kernel.InferenceEngine;
import org.mandarax.kernel.KnowledgeBase;
import org.mandarax.kernel.LogicFactory;
import org.mandarax.kernel.Predicate;
import org.mandarax.kernel.Prerequisite;
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
			looseInstantiations.add(loosePred);
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
	 * Instantiates this guided predicate into a proper fact/prerequisite. For
	 * now, there is no negation. Tied terms can be given as a parameter and the
	 * same parameter is used for output of terms used in the predicate.
	 * 
	 * @param factory
	 *            The logic factory in use.
	 * @param replacementTerms
	 *            The variable terms to fill in the tied terms and the output
	 *            parameter.
	 * @param negated
	 *            If the prerequisite is negated.
	 * @param allTerms
	 *            The list of all terms used so far.
	 * @return A instantiated version of this guided predicate with type
	 *         predicates, if it can be. Also fills the existing terms array
	 *         with the terms used.
	 */
	public List<Prerequisite> factify(LogicFactory factory,
			Term[] replacementTerms, boolean negated, boolean mustTie,
			Collection<Term> allTerms) {
		// If we don't have all terms, create one
		if (allTerms == null)
			allTerms = new HashSet<Term>();

		List<Prerequisite> result = new LinkedList<Prerequisite>();

		int offset = 0;
		if (predicate_ instanceof JConstructor)
			offset = 1;

		// Create a term array
		Class[] predStructure = predicate_.getStructure();
		Term[] terms = new Term[predStructure.length];

		// If we have existing terms and they are 1 too short, add the state
		// term to the start.
		if ((replacementTerms != null)
				&& (replacementTerms.length == (terms.length - offset - 1))) {
			Term[] newExistingTerms = new Term[replacementTerms.length + 1];
			newExistingTerms[0] = StateSpec.getStateTerm(factory);
			for (int i = 0; i < replacementTerms.length; i++)
				newExistingTerms[i + 1] = replacementTerms[i];
			replacementTerms = newExistingTerms;
		}

		// For each term
		for (int i = 0; i < terms.length; i++) {
			// Check for the instantiated class in the predicate
			if (i < offset) {
				terms[i] = StateSpec.getSpecTerm(factory);
			} else {
				// Instantiate/find the regular terms
				terms[i] = instantiateTerm(factory, replacementTerms, mustTie,
						offset, predStructure[i], i);
				if (terms[i] == null)
					return null;

				// Add the type predicates and the inequality predicates.
				addTypesAndInequals(factory, allTerms, result,
						predStructure[i], terms[i]);

				// Adding to the return array
				if (replacementTerms != null) {
					replacementTerms[i - offset] = terms[i];
				}
			}
		}

		// Create the prereq
		result.add(factory.createPrerequisite(predicate_, terms, negated));
		// }
		return result;
	}

	/**
	 * Adds the type predicates and the inequal predicates.
	 * 
	 * @param factory
	 *            The logic factory.
	 * @param allTerms
	 *            The existing terms to swap.
	 * @param result
	 *            The list of prereqs to add to.
	 * @param predStructure
	 *            The predicate structure.
	 * @param term
	 *            The current term of the predicate.
	 */
	private void addTypesAndInequals(LogicFactory factory,
			Collection<Term> allTerms, List<Prerequisite> result,
			Class termClass, Term term) {
		// If the term is already in the used terms, it has already been
		// defined.
		if (!allTerms.contains(term)) {
			// Adding the type predicates, if necessary
			Predicate typePred = StateSpec.getInstance().getTypePredicate(
					termClass);
			if (typePred != null) {
				Term[] typeTerm = { term };
				Prerequisite typePrereq = factory.createPrerequisite(typePred,
						typeTerm, false);
				if (!result.contains(typePrereq))
					result.add(typePrereq);
			}

			// Adding the inequality predicate/s
			Predicate inequal = StateSpec.getInequalityPredicate();
			// We need to define an inequality relation for each of these
			for (Term usedTerm : allTerms) {
				if (term.getType().equals(usedTerm.getType())) {
					// If at least one is a variable
					if ((term.isVariable()) || (usedTerm.isVariable())) {
						Term[] terms = new Term[3];
						terms[0] = StateSpec.getSpecTerm(factory);
						terms[1] = term;
						terms[2] = usedTerm;
						Prerequisite ineqPreq = factory.createPrerequisite(
								inequal, terms, false);
						result.add(ineqPreq);
					}
				}
			}

			allTerms.add(term);
		}
	}

	/**
	 * Instantiates a pred term into a proper term.
	 * 
	 * @param factory
	 *            The logic factory.
	 * @param existingTerms
	 *            The existing terms to swap.
	 * @param mustTie
	 *            If the existing terms must tie.
	 * @param offset
	 *            The offset for the existing terms.
	 * @param termClass
	 *            The class of the current term.
	 * @param index
	 *            The predicate term index.
	 * @return The created or existing term.
	 */
	private Term instantiateTerm(LogicFactory factory, Term[] existingTerms,
			boolean mustTie, int offset, Class termClass, int index) {
		PredTerm argTerm = null;
		if (looseInstantiation_ != null)
			argTerm = looseInstantiation_[index - offset];
		else
			argTerm = new PredTerm(predValues_[index - offset][0].getValue(),
					termClass, PredTerm.TIED);

		// If we have a tied term
		if (argTerm.getTermType() == PredTerm.TIED) {
			// If we have a replacement, use that
			if ((existingTerms != null)
					&& (existingTerms[index - offset] != null)
					&& (termClass
							.isAssignableFrom(existingTerms[index - offset]
									.getType()))) {
				return existingTerms[index - offset];
			} else {
				// If we must tie
				if (mustTie) {
					// Left untied, so results in an invalid fact
					return null;
				} else {
					// Treat this as a free variable
					return factory.createVariableTerm((String) (argTerm
							.getValue()), termClass);
				}
			}
		} else if (argTerm.getTermType() == PredTerm.FREE) {
			// Create a free variable
			return factory.createVariableTerm((String) (argTerm.getValue()),
					termClass);
		} else if (argTerm.getTermType() == PredTerm.VALUE) {
			// Create a constant variable
			return factory.createConstantTerm(argTerm.getValue(), termClass);
		}
		return null;
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

	@Override
	public String toString() {
		if (predicate_ == null)
			return "true";
		if (looseInstantiation_ != null) {
			StringBuffer buffer = new StringBuffer(predicate_.toString() + "(");
			boolean first = true;
			for (PredTerm pt : looseInstantiation_) {
				if (!first)
					buffer.append(",");
				buffer.append(pt.toString());
				first = false;
			}
			buffer.append(")");
			return buffer.toString();
		}
		return predicate_.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj != null) && (obj instanceof GuidedPredicate)) {
			GuidedPredicate gp = (GuidedPredicate) obj;
			// Predicate nullability
			if (predicate_ == null) {
				if (gp.predicate_ == null)
					return true;
				else
					return false;
			}

			if (predicate_.getName().equals(gp.predicate_.getName())) {
				boolean predValEquals = predValues_ == null ? gp.predValues_ == null
						: Arrays.deepEquals(predValues_, gp.predValues_);
				boolean looseInstantEquals = looseInstantiation_ == null ? gp.looseInstantiation_ == null
						: Arrays.equals(looseInstantiation_,
								gp.looseInstantiation_);
				return predValEquals && looseInstantEquals;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (predicate_ == null)
			return 7919;

		int predVal = 7919 * predicate_.hashCode();
		int predValsVal = predValues_ == null ? 2099 : 2099 * predValues_
				.hashCode();
		int looseVals = looseInstantiation_ == null ? 5059
				: 5059 * looseInstantiation_.hashCode();
		return predVal * predValsVal * looseVals;
	}
}
