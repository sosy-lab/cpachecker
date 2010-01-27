package cpa.observeranalysis;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import cfa.objectmodel.CFAEdge;

/** A transition in the observer automaton implements one of the {@link PATTERN_MATCHING_METHODS}.
 * This determines if the transition matches on a certain {@link CFAEdge}.
 * @author rhein
 */
class ObserverTransition {
  static enum PATTERN_MATCHING_METHODS {
    EXACT_MATCH,
    REGEX_MATCH,
    AST_COMPARISON
  }
  
  /** Pattern of this Transition, directly from the observer-definition file.  */
  private String pattern;
  private PATTERN_MATCHING_METHODS usedMethod;
  // The order of assertions and (more importantly) actions is preserved by the parser.
  private List<ObserverBoolExpr> assertions;
  private List<ObserverActionExpr> actions;
  /**
   * When the parser instances this class it can not assign a followstate because 
   * that state might not be created (forward-reference).
   * Only the name is known in the beginning and the followstate relation must be 
   * resolved by calling setFollowState() when all States are known. 
   */
  private String followStateName;
  private ObserverInternalState followState;

  public ObserverTransition(String pPattern, List<ObserverBoolExpr> pAssertions, List<ObserverActionExpr> pActions,
      String pFollowStateName, PATTERN_MATCHING_METHODS pUseMethod) {
    this.pattern = pPattern;
    this.assertions = pAssertions;
    this.actions = pActions;
    this.followStateName = pFollowStateName;
    this.usedMethod = pUseMethod;
  }

  /**
   * Resolves the follow-state relation for this transition.
   * @param pAllStates
   */
  public void setFollowState(List<ObserverInternalState> pAllStates) {
    for (ObserverInternalState s : pAllStates) {
      if (s.getName().equals(followStateName)) {
        this.followState = s;
        return;
      }
    }
    System.err.println("No Follow-State with name " + followStateName + " found. Calling this transition will result in an Exception.");
  }
  /** Writes a representation of this transition (as edge) in DOT file format to the argument {@link PrintStream}.
   * @param sourceStateId
   * @param out
   */
  void writeTransitionToDotFile(int sourceStateId, PrintStream out) {
    out.println(sourceStateId + " -> " + followState.getStateId() + " [label=\"" + pattern + "\"]");
  }

  /** Determines if this Transition matches on the argument {@link CFAEdge}.
   * The result of this method depends on which {@link PATTERN_MATCHING_METHODS} is used by this transition.
   * @param pCfaEdge
   * @return
   */
  public boolean match(CFAEdge pCfaEdge) {
    switch (usedMethod) {
    case EXACT_MATCH :
      return pCfaEdge.getRawStatement().equals(pattern);
    case REGEX_MATCH :
        return pCfaEdge.getRawStatement().matches(pattern);
      case AST_COMPARISON :
        boolean result = ObserverASTComparator.generateAndCompareASTs(pCfaEdge.getRawStatement(), pattern);
        /*if (result == true) {
          System.out.println("Triggered : " + pCfaEdge.getRawStatement() + " : " + this.pattern + " to " + followStateName);
        }*/
        return result;
      default:
        return false;
    }
  }

  /**
   * Checks if all assertions of this transition are fulfilled 
   * in the current configuration of the automaton this method is called.
   * @return
   */
  public boolean assertionsHold(Map<String, ObserverVariable> pVars) {
    for (ObserverBoolExpr assertion : assertions) {
      if (assertion.eval(pVars) == false) {
        return false; // Lazy Evaluation
      }
    }
    return true;
  }
  
  /**
   * Executes all actions of this transition in the order which is defined in the automaton definition file.
   * @param pVars 
   */
  public void executeActions(Map<String, ObserverVariable> pVars) {
    for (ObserverActionExpr action : actions) {
      action.execute(pVars);
    }
  }

  /** returns null if setFollowState() was not called or no followState with appropriate name was found.
   * @return
   */
  public ObserverInternalState getFollowState() {
    return followState;
  }
}
