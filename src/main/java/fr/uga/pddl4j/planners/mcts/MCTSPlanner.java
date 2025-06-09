package fr.uga.pddl4j.planners.mcts;

import fr.uga.pddl4j.heuristics.state.StateHeuristic;
import fr.uga.pddl4j.parser.DefaultParsedProblem;
import fr.uga.pddl4j.plan.Plan;
import fr.uga.pddl4j.plan.SequentialPlan;
import fr.uga.pddl4j.planners.AbstractPlanner;
import fr.uga.pddl4j.planners.Planner;
import fr.uga.pddl4j.planners.PlannerConfiguration;
import fr.uga.pddl4j.planners.PlanningProblem;
import fr.uga.pddl4j.planners.SearchStrategy;
import fr.uga.pddl4j.planners.statespace.search.StateSpaceSearch;
import fr.uga.pddl4j.problem.DefaultProblem;
import fr.uga.pddl4j.problem.Problem;
import fr.uga.pddl4j.problem.State;
import fr.uga.pddl4j.problem.operator.Action;
import fr.uga.pddl4j.problem.operator.ConditionalEffect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.util.*;

/**
 * Monte Carlo Tree Search Planner implementation for PDDL4J
 * 
 * @author StrongAmineMohamed
 * @version 1.0
 */
@CommandLine.Command(name = "MCTS", 
                    version = "MCTS 1.0",
                    description = "Monte Carlo Tree Search planner")
public class MCTSPlanner extends AbstractPlanner {

    private static final Logger LOGGER = LogManager.getLogger(MCTSPlanner.class.getName());
    
    @CommandLine.Option(names = {"-w", "--walks"}, 
                       defaultValue = "1000",
                       description = "Number of random walks (default: 1000)")
    private int numberOfWalks = 1000;
    
    @CommandLine.Option(names = {"-l", "--length"}, 
                       defaultValue = "100",
                       description = "Maximum length of random walks (default: 100)")
    private int maxWalkLength = 100;
    
    @CommandLine.Option(names = {"-d", "--deadlock"}, 
                       defaultValue = "false",
                       description = "Use Monte Carlo Deadlock Avoidance")
    private boolean useDeadlockAvoidance = false;
    
    @CommandLine.Option(names = {"-h", "--helpful"}, 
                       defaultValue = "false",
                       description = "Use Monte Carlo with Helpful Actions")
    private boolean useHelpfulActions = false;

    private Random random;

    public MCTSPlanner() {
        super();
        this.random = new Random();
    }

    public MCTSPlanner(PlannerConfiguration configuration) {
        super(configuration);
        this.random = new Random();
    }

    @Override
    public Plan solve(final Problem problem) {
        LOGGER.info("Starting MCTS planning...");
        
        final long startTime = System.currentTimeMillis();
        
        final State initialState = new State(problem.getInitialState());
        final State goalState = new State(problem.getGoal());
        
        Plan bestPlan = performMCTS(problem, initialState, goalState);
        
        final long endTime = System.currentTimeMillis();
        final double searchTime = (endTime - startTime) / 1000.0;
        
        LOGGER.info("MCTS completed in {} seconds", searchTime);
        
        return bestPlan;
    }

    private Plan performMCTS(Problem problem, State initialState, State goalState) {
        Plan bestPlan = null;
        int bestPlanLength = Integer.MAX_VALUE;
        
        for (int walk = 0; walk < numberOfWalks; walk++) {
            Plan walkPlan = performRandomWalk(problem, initialState, goalState);
            
            if (walkPlan != null && walkPlan.size() < bestPlanLength) {
                bestPlan = walkPlan;
                bestPlanLength = walkPlan.size();
                LOGGER.debug("Found better plan with length: {}", bestPlanLength);
            }
            
            if (bestPlanLength <= 10) {
                break;
            }
        }
        
        return bestPlan;
    }

    private Plan performRandomWalk(Problem problem, State initialState, State goalState) {
        State currentState = new State(initialState);
        Plan plan = new SequentialPlan();
        Set<State> visitedStates = new HashSet<>();
        
        for (int step = 0; step < maxWalkLength; step++) {
            if (currentState.satisfy(goalState)) {
                return plan;
            }
            
            List<Action> applicableActions = getApplicableActions(problem, currentState);
            
            if (applicableActions.isEmpty()) {
                break;
            }
            
            if (useDeadlockAvoidance) {
                applicableActions = filterDeadlockActions(applicableActions, currentState, visitedStates);
            }
            
            if (useHelpfulActions) {
                applicableActions = filterHelpfulActions(applicableActions, currentState, goalState);
            }
            
            if (applicableActions.isEmpty()) {
                break;
            }
            
            Action selectedAction = applicableActions.get(random.nextInt(applicableActions.size()));
            
            visitedStates.add(new State(currentState));
            currentState = applyAction(currentState, selectedAction);
            plan.add(step, selectedAction);
        }
        
        return null;
    }

    private List<Action> getApplicableActions(Problem problem, State state) {
        List<Action> applicableActions = new ArrayList<>();
        
        for (Action action : problem.getActions()) {
            if (action.isApplicable(state)) {
                applicableActions.add(action);
            }
        }
        
        return applicableActions;
    }

    private List<Action> filterDeadlockActions(List<Action> actions, State currentState, Set<State> visitedStates) {
        List<Action> filteredActions = new ArrayList<>();
        
        for (Action action : actions) {
            State nextState = applyAction(currentState, action);
            if (!visitedStates.contains(nextState)) {
                filteredActions.add(action);
            }
        }
        
        return filteredActions.isEmpty() ? actions : filteredActions;
    }

    private List<Action> filterHelpfulActions(List<Action> actions, State currentState, State goalState) {
        List<Action> helpfulActions = new ArrayList<>();
        
        for (Action action : actions) {
            if (isHelpfulAction(action, currentState, goalState)) {
                helpfulActions.add(action);
            }
        }
        
        return helpfulActions.isEmpty() ? actions : helpfulActions;
    }

    private boolean isHelpfulAction(Action action, State currentState, State goalState) {
        for (ConditionalEffect effect : action.getConditionalEffects()) {
            if (effect.getCondition().isApplicable(currentState)) {
                if (goalState.satisfy(effect.getEffect().getPositiveFluents())) {
                    return true;
                }
            }
        }
        return false;
    }

    private State applyAction(State state, Action action) {
        State newState = new State(state);
        
        for (ConditionalEffect effect : action.getConditionalEffects()) {
            if (effect.getCondition().isApplicable(state)) {
                newState.apply(effect.getEffect());
            }
        }
        
        return newState;
    }

    @Override
    public boolean isSupported(Problem problem) {
        return problem.getRequirements().isEmpty() || 
               problem.getRequirements().contains(fr.uga.pddl4j.problem.RequireKey.STRIPS);
    }

    public static void main(String[] args) {
        try {
            final MCTSPlanner planner = new MCTSPlanner();
            CommandLine cmd = new CommandLine(planner);
            cmd.execute(args);
        } catch (IllegalArgumentException e) {
            LOGGER.fatal(e.getMessage());
        }
    }

    // Getters and setters
    public int getNumberOfWalks() { return numberOfWalks; }
    public void setNumberOfWalks(int numberOfWalks) { this.numberOfWalks = numberOfWalks; }
    
    public int getMaxWalkLength() { return maxWalkLength; }
    public void setMaxWalkLength(int maxWalkLength) { this.maxWalkLength = maxWalkLength; }
    
    public boolean isUseDeadlockAvoidance() { return useDeadlockAvoidance; }
    public void setUseDeadlockAvoidance(boolean useDeadlockAvoidance) { this.useDeadlockAvoidance = useDeadlockAvoidance; }
    
    public boolean isUseHelpfulActions() { return useHelpfulActions; }
    public void setUseHelpfulActions(boolean useHelpfulActions) { this.useHelpfulActions = useHelpfulActions; }
}
