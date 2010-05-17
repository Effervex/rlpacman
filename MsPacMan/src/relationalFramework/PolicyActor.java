package relationalFramework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jess.Fact;
import jess.RU;
import jess.Rete;
import jess.Value;
import jess.ValueVector;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

/**
 * An agent that chooses its decisions based on a fixed policy, fed in via agent
 * message.
 * 
 * @author Sam Sarjant
 */
public class PolicyActor implements AgentInterface {
	/** The current agent policy. */
	private Policy policy_;

	/** The last chosen actions. */
	private ActionChoice prevActions_;

	/** The previous state seen by the agent. */
	private Collection<Fact> prevState_;

	/**
	 * If this agent is the optimal agent (defined by the environment, not how
	 * the agent sees itself).
	 */
	private boolean optimal_;

	/** An agent's internal goal to attempt to achieve. */
	private String goalPredicate_;

	/** The agent's internal goal to chase, if learning modules. */
	private String internalGoal_;

	/** The internal reward the agent perceives when performing module learning. */
	private double internalReward_;

	/** If the agent's internal goal has been met. */
	private boolean internalGoalMet_;

	/** A collection of possible goals for the agent to pursue. */
	private List<ValueVector> possibleGoals_;

	/** The total reward accrued by the agent. */
	private double totalReward_;

	/** The total number of steps the agent has taken. */
	private int totalSteps_;

	// @Override
	public void agent_cleanup() {
	}

	// @Override
	public void agent_init(String arg0) {
		prevState_ = null;
		optimal_ = false;
		possibleGoals_ = new ArrayList<ValueVector>();
		totalReward_ = 0;
		totalSteps_ = 0;
	}

	// @Override
	public String agent_message(String arg0) {
		// Receive a policy
		if (arg0.equals("Policy")) {
			policy_ = (Policy) ObjectObservations.getInstance().objectArray[0];
			if ((goalPredicate_ != null) && (!goalPredicate_.isEmpty())
					&& (!possibleGoals_.isEmpty())) {
				policy_.parameterArgs(possibleGoals_
						.get(PolicyGenerator.random_.nextInt(possibleGoals_
								.size())));
			}
		} else if (arg0.equals("Optimal")) {
			optimal_ = true;
		} else if (arg0.equals("internalReward")) {
			// Return the reward if the internal goal was met, else return a
			// large negative value.
			if (internalGoalMet_)
				return internalReward_ + "";
			else
				return -Math.abs(internalReward_ * 100) + "";
		} else if (arg0.equals("formPreGoal")) {
			// Only form pre-goal if the generator isn't a module.
			if (!PolicyGenerator.getInstance().isModuleGenerator())
				PolicyGenerator.getInstance().formPreGoalState(prevState_,
						prevActions_, StateSpec.getInstance().getConstants());
		} else if (arg0.indexOf(LearningController.INTERNAL_PREFIX) == 0) {
			// Setting an internal goal
			String oldGoal = goalPredicate_;
			goalPredicate_ = arg0.substring(LearningController.INTERNAL_PREFIX
					.length() + 1);
			if (goalPredicate_.isEmpty()) {
				goalPredicate_ = null;
			}

			internalGoal_ = null;
			possibleGoals_.clear();
			return oldGoal;
		}
		return null;
	}

	// @Override
	public Action agent_start(Observation arg0) {
		// Initialising the actions
		prevActions_ = new ActionChoice();

		Action action = new Action(0, 0);
		action.charArray = ObjectObservations.OBSERVATION_ID.toCharArray();
		// Choosing an action
		Collection<Fact> stateFacts = StateSpec.extractFacts(ObjectObservations
				.getInstance().predicateKB);
		setInternalGoal(stateFacts);

		prevActions_ = chooseAction(
				ObjectObservations.getInstance().predicateKB, stateFacts);
		ObjectObservations.getInstance().objectArray = new ActionChoice[] { prevActions_ };

		return action;
	}

	// @Override
	public Action agent_step(double arg0, Observation arg1) {
		noteInternalFigures(arg0);

		Action action = new Action(0, 0);
		action.charArray = ObjectObservations.OBSERVATION_ID.toCharArray();
		Collection<Fact> stateFacts = StateSpec.extractFacts(ObjectObservations
				.getInstance().predicateKB);

		// Check the internal goal here
		if (!optimal_ && (goalPredicate_ != null)) {
			processInternalGoal(stateFacts, prevState_);
		}

		prevActions_ = chooseAction(
				ObjectObservations.getInstance().predicateKB, stateFacts);
		ObjectObservations.getInstance().objectArray = new ActionChoice[] { prevActions_ };

		return action;
	}

	// @Override
	public void agent_end(double arg0) {
		noteInternalFigures(arg0);

		// Save the pre-goal state and goal action
		if (goalPredicate_ != null) {
			if (!optimal_) {
				Collection<Fact> stateFacts = StateSpec
						.extractFacts(ObjectObservations.getInstance().predicateKB);
				processInternalGoal(stateFacts, prevState_);
			}
		} else {
			if (!ObjectObservations.getInstance().objectArray[0]
					.equals(ObjectObservations.NO_PRE_GOAL)) {
				PolicyGenerator.getInstance().formPreGoalState(prevState_,
						prevActions_, StateSpec.getInstance().getConstants());
			}
		}

		prevActions_ = null;
	}

	/**
	 * Note internal reward figures. Only get a limited sample.
	 * 
	 * @param reward
	 *            The reward received.
	 */
	private void noteInternalFigures(double reward) {
		// Noting internal figures
		if (totalSteps_ < Short.MAX_VALUE) {
			totalReward_ += reward;
			totalSteps_++;
		}
	}

	/**
	 * Chooses the action based on what higher actions are switched on.
	 * 
	 * @param state
	 *            The state of the system as given by predicates.
	 * @return A relational action.
	 */
	private ActionChoice chooseAction(Rete state, Collection<Fact> stateFacts) {
		ActionChoice actions = new ActionChoice();

		boolean noteTriggered = true;
		if ((internalGoal_ != null) && (internalGoalMet_))
			noteTriggered = false;
		// Evaluate the policy for true rules and activates
		actions = policy_.evaluatePolicy(state, actions, StateSpec
				.getInstance().getNumReturnedActions(), optimal_, false, noteTriggered);

		// Save the previous state (if not an optimal agent).
		prevState_ = stateFacts;

		// Return the actions.
		return actions;
	}

	/**
	 * Sets an internal goal for the agent to pursue if the agent is a modular
	 * learning one. This method also modifies the agent policy towards the
	 * goal.
	 * 
	 * @param stateFacts
	 *            The facts about the current state.
	 */
	private void setInternalGoal(Collection<Fact> stateFacts) {
		if ((goalPredicate_ != null) && (!possibleGoals_.isEmpty())) {
			internalReward_ = 0;
			internalGoalMet_ = false;

			// Examine the state for the goals that are true
			List<ValueVector> trueArgs = new ArrayList<ValueVector>();
			for (Fact fact : stateFacts) {
				String[] factSplit = StateSpec.splitFact(fact.toString());
				if (factSplit[0].equals(goalPredicate_)) {
					try {
						trueArgs.add(fact.getSlotValue("__data")
								.listValue(null));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			Collections.shuffle(possibleGoals_);
			ValueVector chosenGoal = null;
			for (int i = 0; i < possibleGoals_.size(); i++) {
				chosenGoal = possibleGoals_.get(i);

				// Check the goal hasn't already been achieved
				if (!trueArgs.contains(chosenGoal)) {
					break;
				}
			}

			// Modify the policy towards the chosen goal
			policy_.parameterArgs(chosenGoal);
			internalGoal_ = "(" + goalPredicate_ + " " + chosenGoal.toString()
					+ ")";

			if (PolicyGenerator.debugMode_) {
				System.out.println("Internal goal: " + internalGoal_);
				System.out.println(policy_);
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
	 * @param prevState
	 *            The facts from the previous state.
	 */
	private void processInternalGoal(Collection<Fact> stateFacts,
			Collection<Fact> prevState) {
		// Assigning reward
		if (!internalGoalMet_) {
			internalReward_ += totalReward_ / totalSteps_;
		}

		// Check each predicate in the state
		for (Fact fact : stateFacts) {
			String[] factSplit = StateSpec.splitFact(fact.toString());
			// If the fact is a goal fact and wasn't in the previous state, we
			// have an achieved goal.
			if (factSplit[0].equals(goalPredicate_)) {
				// Add the fact to the possible goals
				try {
					ValueVector vv = fact.getSlotValue("__data")
							.listValue(null);
					if (!possibleGoals_.contains(vv))
						possibleGoals_.add(vv);
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (!prevState.contains(fact)) {
					// Form the general pre-goal, replacing the 'goal' terms
					// with general placeholders
					formPlaceholderPreGoalState(factSplit);
				}

				// Checking if the internal goal is met
				if (internalGoal_ != null) {
					internalGoalMet_ |= StateSpec.reformFact(factSplit).equals(
							internalGoal_);
				}
			}
		}
	}

	/**
	 * Forms a pre-goal state using a new fact as the supposed goal. This new
	 * fact becomes the temporary goal, with its terms swapped for placeholder
	 * parameter terms.
	 * 
	 * @param temporaryGoal
	 *            The temporary goal.
	 */
	private void formPlaceholderPreGoalState(String[] temporaryGoal) {
		// The constants are the terms used in the fact
		List<String> placeholderConstants = new ArrayList<String>(
				temporaryGoal.length - 1);
		Map<String, String> replacements = new HashMap<String, String>();
		int paramIndex = 0;
		for (int i = 1; i < temporaryGoal.length; i++) {
			if (!replacements.containsKey(temporaryGoal[i])) {
				String param = Module.createModuleParameter(paramIndex);
				paramIndex++;
				replacements.put(temporaryGoal[i], param);
				placeholderConstants.add(param);
			}
		}

		// Replacing the facts in the pre-goal and action with the placeholder
		try {
			Collection<Fact> prevStateClone = new ArrayList<Fact>();
			for (Fact fact : prevState_) {
				String[] factSplit = StateSpec.splitFact(fact.toString());
				if (StateSpec.getInstance().isUsefulPredicate(factSplit[0])) {
					// Swap any 'goal' constants with placeholders
					boolean changed = false;
					for (int i = 1; i < factSplit.length; i++) {
						if (replacements.containsKey(factSplit[i])) {
							factSplit[i] = replacements.get(factSplit[i]);
							changed = true;
						}
					}

					// If changed, make a fact and use that
					if (changed) {
						Fact newFact = new Fact(factSplit[0], StateSpec
								.getInstance().getRete());
						ValueVector vv = new ValueVector(factSplit.length - 1);
						for (int i = 1; i < factSplit.length; i++)
							vv.add(factSplit[i]);
						newFact.setSlotValue("__data", new Value(vv, RU.LIST));
						prevStateClone.add(newFact);
					} else {
						prevStateClone.add(fact);
					}
				}
			}

			// Changing all the actions to modular format
			prevActions_.replaceTerms(replacements);

			// Forming the pre-goal with placeholder constants
			PolicyGenerator.getInstance().formPreGoalState(prevStateClone,
					prevActions_, placeholderConstants);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}