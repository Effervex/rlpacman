package relationalFramework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import jess.Fact;
import jess.QueryResult;
import jess.Rete;
import jess.ValueVector;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import relationalFramework.agentObservations.AgentObservations;

/**
 * An agent that chooses its decisions based on a fixed policy, fed in via agent
 * message.
 * 
 * @author Sam Sarjant
 */
public class PolicyActor implements AgentInterface {
	/**
	 * The internal reward the agent receives at every step when learning
	 * modularly.
	 */
	private static final double INTERNAL_STEPWISE_REWARD = -1;

	// ////////////// BASIC MEMBERS ////////////////////
	/** The current agent policy. */
	private Policy policy_;

	/** The reward received using this policy. */
	private int policySteps_;

	/** The last chosen actions. */
	private ActionChoice prevActions_;

	/** The previous state seen by the agent. */
	private Collection<Fact> prevState_;

	/** If this agent is the hand-coded agent. */
	private boolean handCoded_;

	// ////////////// MODULAR MEMBERS //////////////////
	/** An agent's internal goal to attempt to achieve. */
	private SortedSet<StringFact> goalPredicates_;

	/** The number of arguments the goal takes. */
	private int goalNumArgs_;

	/** The agent's internal goal to chase, if learning modules. */
	private String[] internalGoal_;

	/** The internal reward the agent perceives when performing module learning. */
	private double internalReward_;

	/** If the agent's internal goal has been met. */
	private boolean internalGoalMet_;

	/** A collection of possible goals for the agent to pursue. */
	private Collection<GoalArg> possibleGoals_;

	/** The goal arguments from the previous state. */
	private Collection<GoalArg> prevGoalArgs_;

	// /////////////// INTERNAL COUNTERS ////////////////////
	/** The total reward accrued by the agent. */
	private double totalReward_;

	/** The total number of steps the agent has taken. */
	private int totalSteps_;

	/** The total number of episodes recorded. */
	private int totalEpisodes_;

	// /////////////// RESAMPLING MEMBERS /////////////////////
	/** This episode's state-action pairs. */
	private Collection<StateAction> episodeStateActions_;

	/** The probability of the agent resampling a new policy. */
	private double resampleProb_;

	/** The best policy seen so far. */
	private Policy bestPolicy_;

	/** The best total reward the best policy received. */
	private int mostSteps_;

	// @Override
	public void agent_cleanup() {
	}

	// @Override
	public void agent_init(String arg0) {
		prevState_ = null;
		handCoded_ = false;
		possibleGoals_ = new ArrayList<GoalArg>();
		totalReward_ = 0;
		totalSteps_ = 0;
		totalEpisodes_ = -1;
		AgentObservations.loadAgentObservations();
	}

	// @Override
	@SuppressWarnings("unchecked")
	public String agent_message(String arg0) {
		// Receive a policy
		if (arg0.equals("GetPolicy")) {
			if (bestPolicy_ == null)
				bestPolicy_ = policy_;
			else {
				if (policySteps_ > mostSteps_)
					bestPolicy_ = policy_;
			}
			ObjectObservations.getInstance().objectArray = new Object[] { bestPolicy_ };
			policy_ = null;
			bestPolicy_ = null;
			mostSteps_ = Integer.MIN_VALUE;
		} else if (arg0.equals("SetPolicy")) {
			policy_ = (Policy) ObjectObservations.getInstance().objectArray[0];
			if ((goalPredicates_ != null) && (internalGoal_ != null)) {
				policy_.parameterArgs(internalGoal_);
			}
		} else if (arg0.equals("Optimal")) {
			handCoded_ = true;
		} else if (arg0.equals("internalReward")) {
			// Return the reward if the internal goal was met, else return a
			// large negative value.
			if (internalGoalMet_)
				return internalReward_ + "";
			else
				return -Math.abs(internalReward_ * 100) + "";
		} else if (arg0.equals(LearningController.INTERNAL_PREFIX)) {
			// Setting an internal goal
			SortedSet<StringFact> oldGoal = null;
			int oldNumArgs = -1;
			if (goalPredicates_ != null) {
				oldGoal = goalPredicates_;
				oldNumArgs = goalNumArgs_;
			}
			goalPredicates_ = (SortedSet<StringFact>) ObjectObservations
					.getInstance().objectArray[0];
			goalNumArgs_ = (Integer) ObjectObservations.getInstance().objectArray[1];

			internalGoal_ = null;
			possibleGoals_.clear();
			ObjectObservations.getInstance().objectArray[0] = oldGoal;
			ObjectObservations.getInstance().objectArray[1] = oldNumArgs;
		}
		return null;
	}

	// @Override
	public Action agent_start(Observation arg0) {
		// Initialise the policy
		if (policy_ == null) {
			generatePolicy(false);
		}

		prevState_ = null;
		totalEpisodes_++;

		// Initialising the actions
		prevActions_ = new ActionChoice();

		Action action = new Action(0, 0);
		action.charArray = ObjectObservations.OBSERVATION_ID.toCharArray();
		// Choosing an action
		Rete rete = ObjectObservations.getInstance().predicateKB;
		Collection<Fact> stateFacts = StateSpec.extractFacts(rete);
		setInternalGoal(rete);

		prevActions_ = chooseAction(
				ObjectObservations.getInstance().predicateKB, stateFacts, null);
		ObjectObservations.getInstance().objectArray = new ActionChoice[] { prevActions_ };

		return action;
	}

	// @Override
	public Action agent_step(double arg0, Observation arg1) {
		if (!handCoded_)
			noteInternalFigures(arg0);

		Action action = new Action(0, 0);
		action.charArray = ObjectObservations.OBSERVATION_ID.toCharArray();
		Rete rete = ObjectObservations.getInstance().predicateKB;
		Collection<Fact> stateFacts = StateSpec.extractFacts(rete);

		// Check the internal goal here
		if (!handCoded_ && (goalPredicates_ != null)) {
			processInternalGoal(rete, prevGoalArgs_);
		}

		// Randomly resample the policy if the agent has been performing the
		// same actions.
		if (resampleProb_ > 0
				&& PolicyGenerator.random_.nextDouble() < resampleProb_) {
			generatePolicy(true);
			resampleProb_ = 0;
		}

		policySteps_++;
		prevActions_ = chooseAction(rete, stateFacts, arg0);
		ObjectObservations.getInstance().objectArray = new ActionChoice[] { prevActions_ };
		if (goalPredicates_ != null && internalGoalMet_)
			ObjectObservations.getInstance().earlyExit = true;

		return action;
	}

	// @Override
	public void agent_end(double arg0) {
		if (!handCoded_)
			noteInternalFigures(arg0);

		// Save the pre-goal state and goal action
		if (goalPredicates_ != null) {
			if (!handCoded_) {
				Rete rete = ObjectObservations.getInstance().predicateKB;
				processInternalGoal(rete, prevGoalArgs_);
			}
		}

		policySteps_++;
		prevActions_ = null;
	}

	/**
	 * Generates a new policy to use.
	 * 
	 * @param isResampled
	 *            If this policy was a result of resampling.
	 */
	private void generatePolicy(boolean isResampled) {
		// Note the previous policy and its reward, if applicable
		if (policySteps_ > mostSteps_ && policy_ != null) {
			bestPolicy_ = policy_;
			mostSteps_ = policySteps_;
		}

		policy_ = PolicyGenerator.getInstance().generatePolicy(true);
		if (PolicyGenerator.debugMode_) {
			if (isResampled)
				System.out.println("RESAMPLED POLICY: " + policy_);
			else
				System.out.println(policy_);
		}
		if ((goalPredicates_ != null) && (internalGoal_ != null)) {
			policy_.parameterArgs(internalGoal_);
		}

		// Reset the state observations
		resampleProb_ = 0;
		int initialCapacity = 100;
		if (totalEpisodes_ > 0)
			initialCapacity = 2 * totalSteps_ / totalEpisodes_;
		episodeStateActions_ = new HashSet<StateAction>(initialCapacity);
		policySteps_ = 0;
	}

	/**
	 * Note internal reward figures. Only get a limited sample.
	 * 
	 * @param reward
	 *            The reward received.
	 */
	private void noteInternalFigures(double reward) {
		// Noting internal figures
		if (totalSteps_ < Integer.MAX_VALUE) {
			totalReward_ += reward;
			totalSteps_++;
		}
	}

	/**
	 * Chooses the action based on what higher actions are switched on.
	 * 
	 * @param state
	 *            The state of the system as given by predicates.
	 * @param reward
	 *            The reward received this step.
	 * @return A relational action.
	 */
	private ActionChoice chooseAction(Rete state, Collection<Fact> stateFacts,
			Double reward) {
		ActionChoice actions = new ActionChoice();

		boolean noteTriggered = true;
		if ((internalGoal_ != null) && (internalGoalMet_))
			noteTriggered = false;
		// Evaluate the policy for true rules and activates
		MultiMap<String, String[]> validActions = ObjectObservations
				.getInstance().validActions;

		actions = policy_.evaluatePolicy(state, validActions, actions,
				StateSpec.getInstance().getNumReturnedActions(), handCoded_,
				false, noteTriggered);

		// This allows action covering to catch variant action conditions.
		// This problem is caused by hierarchical RLGG rules, which cover
		// actions regarding new object type already
		if (!handCoded_)
			checkForUnseenPreds(state, validActions);

		// Save the previous state (if not an optimal agent).
		prevState_ = stateFacts;
		StateAction stateAction = new StateAction(stateFacts, actions);

		if (!handCoded_ && totalEpisodes_ > 0) {
			// Put the state action pair in the collection
			double resampleShiftAmount = 1 / (PolicyGenerator.RESAMPLE_POLICY_BOUND
					* totalSteps_ / totalEpisodes_);
			// If we've seen this state and less than average reward is being
			// received
			if (episodeStateActions_.contains(stateAction) && reward != null
					&& reward < (totalReward_ / totalSteps_)) {
				resampleProb_ += resampleShiftAmount;
			} else {
				episodeStateActions_.add(stateAction);
				resampleProb_ = Math
						.max(0, resampleProb_ - resampleShiftAmount);
			}
		}

		// Return the actions.
		return actions;
	}

	/**
	 * Runs through the set of unseen predicates to check if the state contains
	 * them. This method is used to capture variant action conditions.
	 * 
	 * @param state
	 *            The state to scan.
	 * @param validActions
	 *            The state valid actions.
	 */
	private void checkForUnseenPreds(Rete state,
			MultiMap<String, String[]> validActions) {
		try {
			// Run through the unseen preds, triggering covering if necessary.
			boolean triggerCovering = false;
			Collection<StringFact> removables = new HashSet<StringFact>();
			for (StringFact unseenPred : AgentObservations.getInstance()
					.getUnseenPredicates()) {
				String query = StateSpec.getInstance().getRuleQuery(unseenPred);
				QueryResult results = state.runQueryStar(query,
						new ValueVector());
				if (results.next()) {
					// The unseen pred exists - trigger covering
					triggerCovering = true;
					removables.add(unseenPred);
				}
			}

			// If any unseen preds are seen, trigger covering.
			if (triggerCovering) {
				MultiMap<String, String[]> emptyActions = MultiMap
						.createSortedSetMultiMap(ArgumentComparator
								.getInstance());
				PolicyGenerator.getInstance().triggerRLGGCovering(state,
						validActions, emptyActions, false);
				AgentObservations.getInstance().removeUnseenPredicates(
						removables);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets an internal goal for the agent to pursue if the agent is a modular
	 * learning one. This method also modifies the agent policy towards the
	 * goal.
	 * 
	 * @param state
	 *            The current state.
	 */
	private void setInternalGoal(Rete state) {
		// If there is a modular goal
		if (goalPredicates_ != null) {
			// Scan the state for any met goals (noting the previous state goal
			// args).
			Collection<GoalArg> currentGoalArgs = extractPossibleGoals(state,
					null);
			// If there are possible unobtained goals
			if (possibleGoals_.size() > currentGoalArgs.size()) {
				internalReward_ = 0;
				internalGoalMet_ = false;

				List<GoalArg> untrueGoals = new ArrayList<GoalArg>(
						possibleGoals_);
				untrueGoals.removeAll(currentGoalArgs);
				if (!untrueGoals.isEmpty()) {
					// Select a random goal
					internalGoal_ = untrueGoals
							.get(
									PolicyGenerator.random_.nextInt(untrueGoals
											.size())).getGoalArgs();
					policy_.parameterArgs(internalGoal_);

					if (PolicyGenerator.debugMode_) {
						System.out.println("Internal goal: "
								+ Arrays.toString(internalGoal_));
						System.out.println(policy_);
					}
				}
			}
		}
	}

	/**
	 * Deals with operations concerning the internal goal such as forming
	 * pre-goal states, checking if the internal goal has been met (if one
	 * exists), and accumulating reward (unless goal has been met).
	 * 
	 * @param stateFacts
	 *            The current state facts.
	 * @param prevStateArgs
	 *            The goal args from the previous state.
	 */
	private void processInternalGoal(Rete rete,
			Collection<GoalArg> prevStateArgs) {
		// Check if the goal has been met
		Collection<GoalArg> goalArgs = extractPossibleGoals(rete, internalGoal_);

		// (Negative) reward per step if the goal is not achieved.
		if (!internalGoalMet_) {
			internalReward_ += INTERNAL_STEPWISE_REWARD;
		}
	}

	/**
	 * Extracts the possible goals from a state. Also checks if the internal
	 * goal is met.
	 * 
	 * @param state
	 *            The Rete state.
	 * @param goalParams
	 *            TODO Not being used yet. Will need to be to speed things up.
	 *            Insert as a ValueVector parameter.
	 * @return True if the goal is (or has been) met, false otherwise.
	 */
	private Collection<GoalArg> extractPossibleGoals(Rete state,
			String[] goalParams) {
		Collection<GoalArg> unseenGoals = new HashSet<GoalArg>();
		prevGoalArgs_ = new HashSet<GoalArg>();
		try {
			GuidedRule gr = new GuidedRule(new TreeSet<StringFact>(
					goalPredicates_), null, null);
			gr.expandConditions();
			String query = StateSpec.getInstance().getRuleQuery(gr);
			// Set the goal parameters
			ValueVector vv = new ValueVector();
			QueryResult results = state.runQueryStar(query, vv);
			Collection<StringFact> invariants = AgentObservations.getInstance()
					.getSpecificInvariants();

			// If there are results, then the goal COULD be met (can add
			// possible goals here)
			if (results.next()) {
				do {
					String[] arguments = new String[goalNumArgs_];
					for (int i = 0; i < goalNumArgs_; i++)
						arguments[i] = results.getSymbol(Module
								.createModuleParameter(i).substring(1));

					// Check the goal arg doesn't include an invariant
					GoalArg ga = new GoalArg(arguments);
					if (Arrays.equals(internalGoal_, arguments)) {
						internalGoalMet_ = true;
						// Add the actual goal to the achieved goals
						unseenGoals.add(ga);
					}
					if (!ga.includesInvariants(invariants)) {
						if (!possibleGoals_.contains(ga)) {
							possibleGoals_.add(ga);
							// If the goal has never been evaluated, note it
							// down
							unseenGoals.add(ga);
						}
						prevGoalArgs_.add(ga);
					}
				} while (results.next());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return unseenGoals;
	}

	/**
	 * A class for representing a possible goal state to use (i.e. one that has
	 * occurred).
	 * 
	 * @author Sam Sarjant
	 */
	private class GoalArg {
		/** The facts present in the state. */
		private String[] args_;

		/** The goal facts defined by the arguments. */
		private SortedSet<StringFact> goalFacts_;

		/**
		 * Constructor for a new GoalState.
		 * 
		 * @param facts
		 *            The facts of the goal state, sorted by predicate.
		 */
		public GoalArg(String[] args) {
			args_ = args;
			formGoalFacts(args);
		}

		/**
		 * Forms the goal facts from the given arguments.
		 * 
		 * @param args
		 *            The goal fact arguments.
		 */
		private void formGoalFacts(String[] args) {
			// Create the replacement map.
			Map<String, String> replacements = new HashMap<String, String>();
			for (int i = 0; i < goalNumArgs_; i++)
				replacements.put(Module.createModuleParameter(i), args[i]);

			// Replace the arguments and set the goal.
			goalFacts_ = new TreeSet<StringFact>();
			for (StringFact fact : goalPredicates_) {
				fact = new StringFact(fact);
				fact.replaceArguments(replacements, true);
				goalFacts_.add(fact);
			}
		}

		/**
		 * If this goal instantiation contains any of the parameterised
		 * invariants.
		 * 
		 * @param invariants
		 *            The invaiants to check against.
		 * @return True if the goal arguments contains the invariants.
		 */
		public boolean includesInvariants(Collection<StringFact> invariants) {
			if (invariants.isEmpty())
				return false;

			for (StringFact goalFact : goalFacts_) {
				// Don't check type preds, as they are generally always true.
				if (!StateSpec.getInstance().isTypePredicate(
						goalFact.getFactName())
						&& invariants.contains(goalFact))
					return true;
			}
			return false;
		}

		/**
		 * Gets the facts.
		 * 
		 * @return The arguments for the goal.
		 */
		public String[] getGoalArgs() {
			return args_;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + Arrays.hashCode(args_);
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
			GoalArg other = (GoalArg) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (!Arrays.equals(args_, other.args_))
				return false;
			return true;
		}

		private PolicyActor getOuterType() {
			return PolicyActor.this;
		}

		@Override
		public String toString() {
			return Arrays.toString(args_);
		}
	}
}