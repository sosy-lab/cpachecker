package cpa.observeranalysis;

import java.io.PrintStream;
import java.util.List;
import java.util.logging.Level;

import cfa.objectmodel.CFAEdge;

import com.google.common.collect.ImmutableList;

import cpa.common.LogManager;
import cpa.observeranalysis.ObserverBoolExpr.MaybeBoolean;

/**
 * A transition in the observer automaton implements one of the {@link PATTERN_MATCHING_METHODS}.
 * This determines if the transition matches on a certain {@link CFAEdge}.
 * @author rhein
 */
class ObserverTransition {

  // The order of triggers, assertions and (more importantly) actions is preserved by the parser.
  private final List<ObserverBoolExpr> triggers;
  private final List<ObserverBoolExpr> assertions;
  private final List<ObserverActionExpr> actions;

  /**
   * When the parser instances this class it can not assign a followstate because 
   * that state might not be created (forward-reference).
   * Only the name is known in the beginning and the followstate relation must be 
   * resolved by calling setFollowState() when all States are known. 
   */
  private final String followStateName;
  private ObserverInternalState followState = null;

  public ObserverTransition(List<ObserverBoolExpr> pTriggers, List<ObserverBoolExpr> pAssertions, List<ObserverActionExpr> pActions,
      String pFollowStateName) {
    this.triggers = ImmutableList.copyOf(pTriggers);
    this.assertions = ImmutableList.copyOf(pAssertions);
    this.actions = ImmutableList.copyOf(pActions);
    this.followStateName = pFollowStateName;
  }

  public ObserverTransition(List<ObserverBoolExpr> pTriggers,
      List<ObserverBoolExpr> pAssertions, List<ObserverActionExpr> pActions,
      ObserverInternalState pFollowState) {
    this.triggers = pTriggers;
    this.assertions = pAssertions;
    this.actions = pActions;
    this.followState = pFollowState;
    this.followStateName = pFollowState.getName();
  }

  /**
   * Resolves the follow-state relation for this transition.
   */
  public void setFollowState(List<ObserverInternalState> pAllStates, LogManager pLogger) {
    if (this.followState == null) {
      for (ObserverInternalState s : pAllStates) {
        if (s.getName().equals(followStateName)) {
          this.followState = s;
          return;
        }
      }
      pLogger.log(Level.WARNING, "No Follow-State with name " + followStateName + " found. Calling this transition will result in an Exception.");
    }
  }
  
  /** Writes a representation of this transition (as edge) in DOT file format to the argument {@link PrintStream}.
   */
  void writeTransitionToDotFile(int sourceStateId, PrintStream out) {
    out.println(sourceStateId + " -> " + followState.getStateId() + " [label=\"" /*+ pattern */ + "\"]");
  }

  /** Determines if this Transition matches on the current State of the CPA.
   * This might return a <code>MaybeBoolean.MAYBE</code> value if the method cannot determine if the transition matches.
   * In this case more information (e.g. more AbstractElements of other CPAs) are needed.
   */
  public MaybeBoolean match(ObserverExpressionArguments pArgs) {
    for (ObserverBoolExpr trigger : triggers) {
      MaybeBoolean triggerValue = trigger.eval(pArgs);
      if (triggerValue != MaybeBoolean.TRUE) {
        return triggerValue;
      }
    }
    return MaybeBoolean.TRUE;
  }

  /**
   * Checks if all assertions of this transition are fulfilled 
   * in the current configuration of the automaton this method is called.
   */
  public boolean assertionsHold(ObserverExpressionArguments pArgs) {
    for (ObserverBoolExpr assertion : assertions) {
      if (assertion.eval(pArgs) != MaybeBoolean.TRUE) {
        return false; // Lazy Evaluation
      }
    }
    return true;
  }
  
  /**
   * Executes all actions of this transition in the order which is defined in the automaton definition file.
   */
  public void executeActions(ObserverExpressionArguments pArgs) {
    for (ObserverActionExpr action : actions) {
      action.execute(pArgs);
    }
    if (pArgs.getLogMessage() != null && pArgs.getLogMessage().length() > 0) {
      pArgs.getLogger().log(Level.INFO, pArgs.getLogMessage());
    }
  }

  /**
   * returns null if setFollowState() was not called or no followState with appropriate name was found.
   */
  public ObserverInternalState getFollowState() {
    return followState;
  }
}
