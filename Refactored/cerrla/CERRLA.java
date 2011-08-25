package cerrla;

import relationalFramework.GoalCondition;
import relationalFramework.ObjectObservations;
import relationalFramework.PolicyActions;
import relationalFramework.CoveringRelationalPolicy;
import relationalFramework.RelationalAgent;
import relationalFramework.RelationalObservation;
import relationalFramework.RelationalPolicy;
import relationalFramework.RelationalPredicate;
import relationalFramework.RelationalRule;
import relationalFramework.StateAction;
import relationalFramework.StateSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import jess.QueryResult;
import jess.Rete;
import jess.ValueVector;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import relationalFramework.agentObservations.AgentObservations;
import relationalFramework.agentObservations.GoalArg;
import relationalFramework.agentObservations.LocalAgentObservations;
import relationalFramework.ensemble.PolicyEnsemble;
import util.ArgumentComparator;
import util.MultiMap;
import util.Pair;

/**
 * An agent that chooses its decisions based on a fixed policy, fed in via agent
 * message.
 * 
 * @author Sam Sarjant
 */
public class CERRLA extends RelationalAgent {
	/**
	 * The internal reward the agent receives at every step when learning
	 * modularly.
	 */
	private static final double INTERNAL_STEPWISE_REWARD = -1;

	// ////////////// BASIC MEMBERS ////////////////////
	// TODO This is the parallelisable part (if cemHandler becomes singleton).
	/** The handler which determines which CEM is running. */
	private LearningHandler cemHandler_;

	/** The current agent policy. */
	private PolicyEnsemble policy_;

	/** The number episodes this policy has been evaluated against. */
	private int policyEpisodes_;

	/** The amount of reward this policy has accumulated. */
	private float policyReward_;

	/** Sets the ensemble size. */
	private int ensembleSize_ = 1;

	/** The number of steps evaluated by this policy. */
	private int policySteps_;

	/** The current goal arguments. (e.g. a -> ?G_0) */
	private BidiMap goalArgs_;

	// ////////////// MODULAR MEMBERS //////////////////
	/** The goal condition the agent is following. */
	private GoalCondition goalCondition_;

	/** The internal reward the agent perceives when performing module learning. */
	private float internalReward_;

	/** If the agent's internal goal has been met. */
	private boolean internalGoalMet_;

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
	private Pair<RelationalPolicy, Double> bestPolicy_;

	/** The best total reward the best policy received. */
	private int mostSteps_;

	@Override
	public void agentCleanup() {
	}

	@Override
	public void agentInit() {
		totalReward_ = 0;
		totalSteps_ = 0;
		totalEpisodes_ = -1;

		cemHandler_ = LearningHandler.getInstance();
	}

	// @Override
	public String agent_message(String arg0) {
		// Receive a policy
		if (arg0.equals("GetPolicy")) {
			if (bestPolicy_ == null) {
				if (policy_ == null)
					bestPolicy_ = null;
				else
					bestPolicy_ = policy_.getMajorPolicy();
			} else {
				if (policySteps_ > mostSteps_)
					bestPolicy_ = policy_.getMajorPolicy();
			}
			ObjectObservations.getInstance().objectArray = new Object[] { bestPolicy_ };
			policy_ = null;
			bestPolicy_ = null;
			mostSteps_ = Integer.MIN_VALUE;
		} else if (arg0.equals("SetPolicy")) {
			policy_ = new PolicyEnsemble(
					(RelationalPolicy) ObjectObservations.getInstance().objectArray[0]);
		} else if (arg0.equals("internalReward")) {
			// Return the reward if the internal goal was met, else return a
			// large negative value.
			if (internalGoalMet_)
				return internalReward_ + "";
			else
				return -Math.abs(internalReward_ * 100) + "";
		} else if (arg0.startsWith("ensemble")) {
			String[] split = arg0.split(" ");
			int oldEns = ensembleSize_;
			ensembleSize_ = Integer.parseInt(split[1]);
			return oldEns + "";
		}
		return null;
	}

	@Override
	public PolicyActions agentStep(RelationalObservation observation,
			Double reward) {
		super.agentStep(observation, reward);

		// If there is no policy, run the new policy method.
		if (policy_ == null) {
			newPolicy(observation);
		}

		// Record reward and other internal figures
		noteInternalFigures(reward);

		// Check the internal goal here
		if (goalCondition_ != null) {
			processInternalGoal(observation.getStateObservations());
			if (internalGoalMet_) {
				return null;
			}
		}

		// Randomly resample the policy if the agent has been performing the
		// same actions.
		if (resampleProb_ > 0
				&& PolicyGenerator.random_.nextDouble() < resampleProb_) {
			generatePolicy(true);
			resampleProb_ = 0;
		}

		return chooseAction(observation, reward);
	}

	/**
	 * The method to call when a new policy is required.
	 * 
	 * @param observation
	 *            The current state observation.
	 */
	private void newPolicy(RelationalObservation observation) {
		// Check for modular learning
		if (ProgramArgument.USE_MODULES.booleanValue()
				&& AgentObservations.getInstance().isSettled()
				&& !PolicyGenerator.getInstance().isModuleGenerator()) {
			// Check if the agent needs to drop into learning a module
			checkForModularLearning();
		}

		// Generate a new policy
		generatePolicy(false);
		policyEpisodes_ = 0;
		policyReward_ = 0;

		// If the policy generator is modular, set the internal goal up
		if (PolicyGenerator.getInstance().isModuleGenerator())
			goalCondition_ = PolicyGenerator.getInstance().getModuleGoal();

		setInternalGoal(observation.getStateObservations());

		// Sort out goal args
		goalArgs_ = new DualHashBidiMap(
				ObjectObservations.getInstance().goalReplacements);
		AgentObservations.getInstance().setNumGoalArgs(goalArgs_.size());
		policy_.parameterArgs(goalArgs_.inverseBidiMap());

		if (PolicyGenerator.debugMode_)
			System.out.println("Goal: " + goalArgs_);
	}

	/**
	 * Checks for modular learning - if the agent needs to learn a module as an
	 * internal goal.
	 */
	private void checkForModularLearning() {
		// Get the goal conditions from the local agent observations
		Collection<GoalCondition> goalConditions = AgentObservations
				.getInstance().getLocalSpecificGoalConditions();

		// Find out which modules already exist (remove any goal conditions that
		// already exist)
		Collection<GoalCondition> newGConds = new HashSet<GoalCondition>(
				goalConditions);
		for (GoalCondition gc : goalConditions) {
			// Be sure to check that the module being learned isn't the goal
			// being learned.
			if (Module.moduleExists(StateSpec.getInstance()
					.getEnvironmentName(), gc.toString())
					|| gc.toString().equals(
							PolicyGenerator.getInstance().getLocalGoal()))
				newGConds.remove(gc);
		}

		// Run a preliminary test on each to determine which module has the
		// least further goal conditions (relies on the least other modules to
		// be created).
		for (GoalCondition gc : newGConds) {
			if (!LocalAgentObservations.observationsExist(gc.toString())) {
				System.out.println("\n\n\n------PRELIMINARY MODULE RUN: "
						+ gc.toString() + "------\n\n\n");
				cemHandler_.add(new CrossEntropyMethod(gc), true);
			} else {
				cemHandler_.add(new CrossEntropyMethod(gc), false);
			}
		}
	}

	@Override
	public void agentEnd(double reward) {
		super.agentEnd(reward);
		policyEpisodes_++;
		if (PolicyGenerator.getInstance().isModuleGenerator())
			policyReward_ += internalReward_;
		else
			policyReward_ += getEpisodeReward();
		totalEpisodes_++;

		noteInternalFigures(reward);

		if (policyEpisodes_ >= 3) {
			// Note the policy and reward
			cemHandler_.getCurrentCEM().addSample(policy_,
					policyReward_ / policyEpisodes_);
			policy_ = null;
		}
	}

	/**
	 * Generates a new policy to use.
	 * 
	 * @param isResampled
	 *            If this policy was a result of resampling.
	 */
	@SuppressWarnings("unchecked")
	private void generatePolicy(boolean isResampled) {
		// Note the previous policy and its reward, if applicable
		if (policySteps_ > mostSteps_ && policy_ != null) {
			bestPolicy_ = policy_.getMajorPolicy();
			mostSteps_ = policySteps_;
		}

		Collection<RelationalPolicy> policies = new ArrayList<RelationalPolicy>(
				ensembleSize_);
		for (int i = 0; i < ensembleSize_; i++) {
			CoveringRelationalPolicy pol = cemHandler_.getCurrentCEM()
					.generatePolicy(false);

			// Apply the goal parameters to the goal conditions.
			if (goalArgs_ != null)
				pol.parameterArgs(goalArgs_.inverseBidiMap());
			policies.add(pol);
		}

		policy_ = new PolicyEnsemble(policies);
		if (ensembleSize_ > 1)
			System.out.println("Num policies: " + policy_.numPolicies());

		if (isResampled)
			System.out.println("RESAMPLED POLICY: " + policy_);
		else
			System.out.println(policy_);

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
	 * @param observation
	 *            The relational observation.
	 * @param reward
	 *            The reward received this step.
	 * @return A relational action.
	 */
	private PolicyActions chooseAction(RelationalObservation observation,
			Double reward) {
		PolicyActions actions = new PolicyActions();

		// Evaluate the policy for true rules and activates
		actions = policy_.evaluatePolicy(observation, StateSpec.getInstance()
				.getNumReturnedActions());

		// This allows action covering to catch variant action conditions.
		// This problem is caused by hierarchical RLGG rules, which cover
		// actions regarding new object type already
		Rete state = observation.getStateObservations();
		checkForUnseenPreds(observation);

		if (totalEpisodes_ > 0 && ProgramArgument.CHI.doubleValue() > 0
				&& episodeStateActions_ != null) {
			// Save the previous state (if not an optimal agent).
			StateAction stateAction = new StateAction(
					StateSpec.extractFacts(state), actions);

			// Put the state action pair in the collection
			double resampleShiftAmount = 1 / (ProgramArgument.CHI.doubleValue()
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
	 * @param observation
	 *            The relational observation.
	 */
	private void checkForUnseenPreds(RelationalObservation observation) {
		try {
			// Run through the unseen preds, triggering covering if necessary.
			boolean triggerCovering = false;
			Collection<RelationalPredicate> removables = new HashSet<RelationalPredicate>();
			for (RelationalPredicate unseenPred : AgentObservations
					.getInstance().getUnseenPredicates()) {
				String query = StateSpec.getInstance().getRuleQuery(unseenPred);
				QueryResult results = observation.getStateObservations().runQueryStar(query,
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
				PolicyGenerator.getInstance().triggerRLGGCovering(observation,
						emptyActions);
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
		if (goalCondition_ != null) {
			// Scan the state for any met goals (noting the previous state goal
			// args).
			Collection<GoalArg> currentGoalArgs = extractPossibleGoals(state,
					null);
			Collection<GoalArg> possibleGoals = AgentObservations.getInstance()
					.getPossibleGoalArgs();

			// If there are possible unobtained goals
			if (possibleGoals.size() > currentGoalArgs.size()) {
				internalReward_ = 0;
				internalGoalMet_ = false;

				List<GoalArg> untrueGoals = new ArrayList<GoalArg>(
						possibleGoals);
				untrueGoals.removeAll(currentGoalArgs);
				if (!untrueGoals.isEmpty()) {
					// Select a random goal
					goalArgs_ = new DualHashBidiMap(untrueGoals
							.get(PolicyGenerator.random_.nextInt(untrueGoals
									.size())).getGoalArgs());
					if (PolicyGenerator.debugMode_) {
						System.out.println("Internal goal: " + goalArgs_);
					}
					// Need to reverse it for the policy evaluation process
					goalArgs_ = goalArgs_.inverseBidiMap();
				}
			} else
				goalArgs_ = null;
		}
	}

	/**
	 * Deals with internal goal checking, as well as collecting possible
	 * internal goals.
	 * 
	 * @param stateFacts
	 *            The current state facts.
	 */
	private void processInternalGoal(Rete rete) {
		// Check if the goal has been met
		extractPossibleGoals(rete, goalArgs_);

		// (Negative) reward per step if the goal is not achieved.
		if (!internalGoalMet_) {
			internalReward_ += INTERNAL_STEPWISE_REWARD;
		}
	}

	/**
	 * Extracts the possible goals from a state. Also checks if the internal
	 * goal is met. Includes an inactivity meter which only checks the possible
	 * goals at exponentially increasing intervals (if inactive).
	 * 
	 * @param state
	 *            The Rete state.
	 * @param goalParams
	 *            The goal to check for.
	 * @return The possible goals that may have been achieved this state.
	 */
	@SuppressWarnings("unchecked")
	private Collection<GoalArg> extractPossibleGoals(Rete state,
			BidiMap goalParams) {
		Collection<GoalArg> stateGoals = new TreeSet<GoalArg>();
		prevGoalArgs_ = new HashSet<GoalArg>();

		// Check if searching all goals this step
		boolean searchAll = goalParams == null || goalParams.isEmpty()
				|| AgentObservations.getInstance().searchAllGoalArgs();

		try {
			Collection<RelationalPredicate> fact = new ArrayList<RelationalPredicate>(
					1);
			fact.add(goalCondition_.getFact());
			RelationalRule gr = new RelationalRule(
					new TreeSet<RelationalPredicate>(fact), null, null);
			gr.expandConditions();
			ValueVector vv = new ValueVector();
			// If not searching all, set up the query parameters
			if (!searchAll) {
				gr.setParameters(goalParams.inverseBidiMap());
				for (String param : gr.getParameters())
					vv.add(param);
			}

			String query = StateSpec.getInstance().getRuleQuery(gr);
			QueryResult results = state.runQueryStar(query, vv);
			Collection<RelationalPredicate> invariants = AgentObservations
					.getInstance().getSpecificInvariants();

			// If there are results, then the goal COULD be met (can add
			// possible goals here)
			if (results.next()) {
				do {
					Map<String, String> goalReplacements = new HashMap<String, String>();
					// Run through the arguments, checking if the goal has been
					// met.
					boolean isGoal = (goalArgs_ == null) ? false : true;
					for (int i = 0; i < goalCondition_.getNumArgs(); i++) {
						String goalTerm = StateSpec.createGoalTerm(i);
						String term = results.getSymbol(goalTerm.substring(1));
						goalReplacements.put(goalTerm, term);
						// Check if these are the goal args
						if (isGoal && !term.equals(goalArgs_.getKey(goalTerm)))
							isGoal = false;
					}

					if (isGoal)
						internalGoalMet_ = true;
					GoalArg ga = new GoalArg(goalReplacements, goalCondition_);
					// If the goal isn't using an invariant
					if (!ga.includesInvariants(invariants)) {
						stateGoals.add(ga);
						prevGoalArgs_.add(ga);
					}
				} while (results.next());
				AgentObservations.getInstance().noteGoalArgs(stateGoals);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return stateGoals;
	}

	@Override
	public boolean isConverged() {
		// TODO Auto-generated method stub
		return false;
	}
}