package rlPacMan;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A singleton implementation of the current rule base.
 * @author Samuel J. Sarjant
 *
 */
public class RuleBase {
	/** The instance. */
	private static RuleBase instance_;
	
	/** The rules in use. */
	private ArrayList<Rule> rules_;
	
	/**
	 * A private constructor.
	 */
	private RuleBase(boolean handCoded) {
		if (handCoded)
			loadHandCodedRules();
		else
			generateRandomRules();
	}

	/**
	 * Loads the hand-coded rules with default weights.
	 * 
	 * @param generator
	 *            The generator to receive the rules.
	 */
	private void loadHandCodedRules() {
		rules_ = new ArrayList<Rule>();

		// Go to dot
		rules_.add(new Rule(PacManObservations.CONSTANT, Rule.GREATER_THAN, 0,
				PacManHighAction.TO_DOT, true));
		// Go to centre of dots
		rules_.add(new Rule(PacManObservations.CONSTANT, Rule.GREATER_THAN, 0,
				PacManHighAction.TO_CENTRE_OF_DOTS, true));
		// From nearest ghost 1
		rules_.add(new Rule(PacManObservations.NEAREST_GHOST, Rule.LESS_THAN, 3,
				PacManHighAction.FROM_GHOST, true));
		// From nearest ghost 2
		rules_.add(new Rule(PacManObservations.NEAREST_GHOST, Rule.LESS_THAN, 4,
				PacManHighAction.FROM_GHOST, true));
		// From nearest ghost 3
		rules_.add(new Rule(PacManObservations.NEAREST_GHOST, Rule.LESS_THAN, 5,
				PacManHighAction.FROM_GHOST, true));
		// Stop running from nearest ghost 1
		rules_.add(new Rule(PacManObservations.NEAREST_GHOST, Rule.GREATER_THAN,
				5, PacManHighAction.FROM_GHOST, false));
		// Stop running from nearest ghost 2
		rules_.add(new Rule(PacManObservations.NEAREST_GHOST, Rule.GREATER_THAN,
				6, PacManHighAction.FROM_GHOST, false));
		// Stop running from nearest ghost 3
		rules_.add(new Rule(PacManObservations.NEAREST_GHOST, Rule.GREATER_THAN,
				7, PacManHighAction.FROM_GHOST, false));
		// Towards safe junction
		rules_.add(new Rule(PacManObservations.CONSTANT, Rule.GREATER_THAN, 0,
				PacManHighAction.TO_SAFE_JUNCTION, true));
		// Towards maximally safe junction 1
		rules_
				.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
						Rule.LESS_THAN, 3, PacManHighAction.TO_SAFE_JUNCTION,
						true));
		// Towards maximally safe junction 2
		rules_
				.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
						Rule.LESS_THAN, 2, PacManHighAction.TO_SAFE_JUNCTION,
						true));
		// Maximally safe junction off 1
		rules_.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
				Rule.GREATER_THAN, 3, PacManHighAction.TO_SAFE_JUNCTION, false));
		// Safe to stop running 1
		rules_.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
				Rule.GREATER_THAN, 3, PacManHighAction.FROM_GHOST, false));
		// Maximally safe junction off 2
		rules_.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
				Rule.GREATER_THAN, 5, PacManHighAction.TO_SAFE_JUNCTION, false));
		// Safe to stop running 2
		rules_.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
				Rule.GREATER_THAN, 5, PacManHighAction.FROM_GHOST, false));
		// Keep on moving
		rules_.add(new Rule(PacManObservations.CONSTANT, Rule.GREATER_THAN, 0,
				PacManHighAction.FROM_GHOST, true));
		// Eat edible ghosts
		rules_.add(new Rule(PacManObservations.CONSTANT, Rule.GREATER_THAN, 0,
				PacManHighAction.TO_ED_GHOST, true));
		// Ghost coming, chase powerdots
		rules_.add(new Rule(PacManObservations.NEAREST_GHOST, Rule.LESS_THAN, 4,
				PacManHighAction.TO_POWER_DOT, true));
		// If edible ghosts, don't chase power dots
		rules_.add(new Rule(PacManObservations.NEAREST_ED_GHOST, Rule.LESS_THAN,
				99, PacManHighAction.TO_POWER_DOT, false));
		// If edible ghosts and we're close to a powerdot, move away from it
		rules_.add(new Rule(PacManObservations.NEAREST_ED_GHOST, Rule.LESS_THAN,
				99, PacManObservations.NEAREST_POWER_DOT, Rule.LESS_THAN, 5,
				PacManHighAction.FROM_POWER_DOT, true));
		// If edible ghosts, move away from powerdots
		rules_.add(new Rule(PacManObservations.NEAREST_ED_GHOST, Rule.LESS_THAN,
				99, PacManHighAction.FROM_POWER_DOT, true));
		// If no edible ghosts, stop moving from powerdots
		rules_.add(new Rule(PacManObservations.NEAREST_ED_GHOST,
				Rule.GREATER_THAN, 99, PacManHighAction.FROM_POWER_DOT, false));
		// If no edible ghosts, chase powerdots
		rules_
				.add(new Rule(PacManObservations.NEAREST_ED_GHOST,
						Rule.GREATER_THAN, 99, PacManHighAction.TO_POWER_DOT,
						true));
		// If we're close to a ghost and powerdot, chase the powerdot 1
		rules_.add(new Rule(PacManObservations.NEAREST_POWER_DOT,
				Rule.LESS_THAN, 2, PacManObservations.NEAREST_GHOST,
				Rule.LESS_THAN, 5, PacManHighAction.TO_POWER_DOT, true));
		// If we're close to a ghost and powerdot, chase the powerdot 2
		rules_.add(new Rule(PacManObservations.NEAREST_POWER_DOT,
				Rule.LESS_THAN, 4, PacManObservations.NEAREST_GHOST,
				Rule.LESS_THAN, 5, PacManHighAction.TO_POWER_DOT, true));
		// In the clear
		rules_.add(new Rule(PacManObservations.NEAREST_GHOST, Rule.GREATER_THAN,
				7, PacManObservations.MAX_JUNCTION_SAFETY, Rule.GREATER_THAN,
				4, PacManHighAction.FROM_GHOST, false));
		// Ghosts are not close, eat a dot 1
		rules_.add(new Rule(PacManObservations.GHOST_DENSITY, Rule.LESS_THAN,
				1.5, PacManObservations.NEAREST_POWER_DOT, Rule.LESS_THAN, 5,
				PacManHighAction.FROM_POWER_DOT, true));
		// Far from power dot, stop running
		rules_.add(new Rule(PacManObservations.NEAREST_POWER_DOT,
				Rule.GREATER_THAN, 10, PacManHighAction.FROM_POWER_DOT, false));
		// Ghosts are too spread, move away from power dot
		rules_.add(new Rule(PacManObservations.TOTAL_DIST_TO_GHOSTS,
				Rule.GREATER_THAN, 30, PacManHighAction.FROM_POWER_DOT, true));
		// Unsafe junction, run from ghosts 1
		rules_.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
				Rule.LESS_THAN, 3, PacManHighAction.FROM_GHOST, true));
		// Unsafe junction, run from ghosts 2
		rules_.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
				Rule.LESS_THAN, 2, PacManHighAction.FROM_GHOST, true));
		// Unsafe junction, run from ghosts 3
		rules_.add(new Rule(PacManObservations.MAX_JUNCTION_SAFETY,
				Rule.LESS_THAN, 1, PacManHighAction.FROM_GHOST, true));
		// Move from ghost centre
		rules_.add(new Rule(PacManObservations.CONSTANT, Rule.GREATER_THAN, 0,
				PacManHighAction.FROM_GHOST_CENTRE, true));
		// Stop chasing edible ghosts if none
		rules_
				.add(new Rule(PacManObservations.NEAREST_ED_GHOST,
						Rule.GREATER_THAN, 99, PacManHighAction.TO_ED_GHOST,
						false));
		// Chasing edible ghosts
		rules_.add(new Rule(PacManObservations.NEAREST_ED_GHOST, Rule.LESS_THAN,
				99, PacManHighAction.TO_ED_GHOST, true));
		// If not running from powerdot, move towards it
		rules_.add(new Rule(PacManHighAction.FROM_POWER_DOT, false,
				PacManHighAction.TO_POWER_DOT, true));
	}
	
	/**
	 * Generates random rules.
	 */
	private void generateRandomRules() {
		rules_ = new ArrayList<Rule>();
	}
	
	/**
	 * Gets the rule at index.
	 * 
	 * @param index The index to get the rule from.
	 * @return The Rule or null.
	 */
	public Rule getRule(int index) {
		if (index >= rules_.size())
			return null;
		return rules_.get(index);
	}
	
	/**
	 * Gets all the rules in this instance of the rulebase.
	 * 
	 * @return The rules.
	 */
	public Collection<Rule> getRules() {
		return rules_;
	}
	
	/**
	 * Gets the index of this rule.
	 * 
	 * @param rule The rule to get the index for.
	 * @return The index or -1 if not present.
	 */
	public int indexOf(Rule rule) {
		return rules_.indexOf(rule);
	}
	
	/**
	 * Gets/Initialises the rule base instance.
	 * 
	 * @param handCoded If the rules are hand-coded or random.
	 * @return The rule base instance.
	 */
	public static RuleBase getInstance(boolean handCoded) {
		if (instance_ == null)
			instance_ = new RuleBase(handCoded);
		return instance_;
	}
	
	/**
	 * Gets the rule base instance.
	 * 
	 * @return The rule base instance or null if not yet initialised.
	 */
	public static RuleBase getInstance() {
		return instance_;
	}
}
